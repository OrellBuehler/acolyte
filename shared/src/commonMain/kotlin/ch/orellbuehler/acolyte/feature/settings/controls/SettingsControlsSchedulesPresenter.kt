package ch.orellbuehler.acolyte.feature.settings.controls

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsControlsEndpoints
import ch.orellbuehler.acolyte.api.SettingsSharedEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsScheduleChange
import ch.orellbuehler.acolyte.api.model.SettingsScheduleRow
import ch.orellbuehler.acolyte.api.model.SettingsStageState
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

enum class SettingsControlsScheduleNoticeKind {
    STARTED,
    QUEUED,
    START_FAILED,
    FINISHED,
    NOTHING,
    JOB_FAILED,
    SAVED,
    AUTO_ON,
    AUTO_OFF,
    SAVE_FAILED,
}

data class SettingsControlsScheduleNotice(
    val id: Int,
    val kind: SettingsControlsScheduleNoticeKind,
    val schedule: String,
    val label: String,
    val detail: String = "",
    val reason: SettingsControlsReason = SettingsControlsReason.NONE,
    val every: SettingsControlsEvery = SettingsControlsEvery(SettingsControlsEveryUnit.ALWAYS, 0),
    val error: ApiError? = null,
)

data class SettingsControlsRunningRow(
    val kind: String,
    val label: String,
    val running: Boolean,
)

data class SettingsControlsScheduleRow(
    val kind: String,
    val label: String,
    val about: String,
    val dot: SettingsControlsDot,
    val every: SettingsControlsEvery,
    val lastAt: String,
    val took: String,
    val next: SettingsControlsNext,
    val failed: Boolean,
    val failedError: String,
    val reason: SettingsControlsReason,
    val reasonLinks: Boolean,
    val pageKey: String,
    val canRun: Boolean,
    val canChange: Boolean,
    val spinning: Boolean,
    val enabled: Boolean,
    val toggleable: Boolean,
    val stale: SettingsControlsStale,
    val editing: Boolean,
)

data class SettingsControlsScheduleGroupRows(
    val group: SettingsControlsScheduleGroup,
    val rows: List<SettingsControlsScheduleRow>,
)

data class SettingsControlsLoopRow(
    val key: String,
    val name: String,
    val reason: String,
    val run: SettingsControlsRun,
    val tone: Tone,
    val dot: SettingsControlsDot,
    val every: SettingsControlsEvery,
    val lastAt: String,
    val next: SettingsControlsNext,
)

data class SettingsControlsEditor(
    val kind: String,
    val label: String,
    val about: String,
    val unit: SettingsControlsUnit,
    val amount: String,
    val min: Long,
    val max: Long,
    val step: Long,
    val seconds: Long,
    val valid: Boolean,
    val toggleable: Boolean,
    val enabled: Boolean,
    val pageKey: String,
    val saving: Boolean,
    val busy: Boolean,
    val error: ApiError?,
) {
    val minAmount: Long get() = min / SettingsControlsLogic.unitSize(unit)

    val maxAmount: Long get() = max / SettingsControlsLogic.unitSize(unit)
}

data class SettingsControlsSchedulesState(
    val status: LoadStatus = LoadStatus(),
    val safeMode: Boolean = false,
    val canRun: Boolean = false,
    val canChange: Boolean = false,
    val running: List<SettingsControlsRunningRow> = emptyList(),
    val groups: List<SettingsControlsScheduleGroupRows> = emptyList(),
    val loops: List<SettingsControlsLoopRow> = emptyList(),
    val editor: SettingsControlsEditor? = null,
    val busyKind: String = "",
    val notice: SettingsControlsScheduleNotice? = null,
)

private class SettingsControlsDraft(
    val kind: String,
    val amount: String,
    val saving: Boolean = false,
    val error: ApiError? = null,
)

class SettingsControlsSchedulesPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val clock: Clock = Clock.System,
) {
    private val mutableState = MutableStateFlow(SettingsControlsSchedulesState())
    private val lock = Mutex()
    private var rows: List<SettingsScheduleRow> = emptyList()
    private var loops: List<SettingsStageState> = emptyList()
    private var safeMode = false
    private var now: Instant = clock.now()
    private var draft: SettingsControlsDraft? = null
    private val watching = mutableMapOf<String, Long>()
    private var notices = 0

    val state: StateFlow<SettingsControlsSchedulesState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    canRun = me?.can(Need.MANAGE_LIBRARY) == true,
                    canChange = me?.can(Need.SETTINGS) == true,
                )
            }
            when (val result = api.result(SettingsSharedEndpoints.schedules)) {
                is ApiResult.Ok -> {
                    rows = result.value.schedules
                    loops = result.value.loops
                    safeMode = result.value.safeMode
                    now = clock.now()
                    settle()
                    mutableState.update { it.copy(status = LoadStatus.ready) }
                    rebuild()
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    suspend fun watch() {
        coroutineScope {
            launch {
                while (true) {
                    delay(if (watching.isEmpty()) POLL_MILLIS else WATCH_MILLIS)
                    refresh()
                }
            }
            api.live.collect { event ->
                if (event.topic == TOPIC || event.topic == LiveEvent.RESYNC) refresh()
            }
        }
    }

    suspend fun runNow(kind: String) {
        val current = mutableState.value
        val row = rows.firstOrNull { it.kind == kind } ?: return
        if (!current.canRun || !SettingsControlsLogic.mayRun(row) || current.busyKind.isNotEmpty()) return
        mutableState.update { it.copy(busyKind = kind) }
        val label = effective(row).label
        when (val result = api.result(SettingsSharedEndpoints.runSchedule, "kind" to kind)) {
            is ApiResult.Ok -> {
                watching[kind] = result.value.jobId
                val noticeKind =
                    if (result.value.already) SettingsControlsScheduleNoticeKind.QUEUED else SettingsControlsScheduleNoticeKind.STARTED
                mutableState.update { it.copy(busyKind = "", notice = notice(noticeKind, kind, label)) }
                refresh()
            }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(
                        busyKind = "",
                        notice = notice(SettingsControlsScheduleNoticeKind.START_FAILED, kind, label, error = result.error),
                    )
                }
        }
    }

    fun edit(kind: String) {
        val row = rows.firstOrNull { it.kind == kind } ?: return
        if (!mutableState.value.canChange) return
        val info = effective(row)
        val size = SettingsControlsLogic.unitSize(SettingsControlsLogic.intervalUnit(info.step))
        val seconds = row.everySeconds.takeIf { it > 0 } ?: info.min
        draft = SettingsControlsDraft(kind, (seconds / size).toString())
        rebuild()
    }

    fun closeEditor() {
        draft = null
        rebuild()
    }

    fun setAmount(text: String) {
        val current = draft ?: return
        draft = SettingsControlsDraft(current.kind, text.filter { it.isDigit() }.take(AMOUNT_DIGITS), current.saving)
        rebuild()
    }

    fun stepAmount(up: Boolean) {
        val editor = mutableState.value.editor ?: return
        val size = SettingsControlsLogic.unitSize(editor.unit)
        val stepIn = (editor.step / size).coerceAtLeast(1)
        val minIn = editor.min / size
        val maxIn = editor.max / size
        val current = editor.amount.toLongOrNull() ?: minIn
        val next = (if (up) current + stepIn else current - stepIn).coerceIn(minIn, maxIn)
        draft = SettingsControlsDraft(editor.kind, next.toString(), draft?.saving ?: false)
        rebuild()
    }

    suspend fun saveEditor() {
        val editor = mutableState.value.editor ?: return
        val current = draft ?: return
        if (!editor.valid || current.saving) return
        draft = SettingsControlsDraft(current.kind, current.amount, saving = true)
        rebuild()
        val seconds = editor.seconds
        when (
            val result =
                api.result(
                    SettingsControlsEndpoints.changeSchedule,
                    SettingsScheduleChange(everySeconds = seconds),
                    "kind" to editor.kind,
                )
        ) {
            is ApiResult.Ok -> {
                draft = null
                val notice =
                    notice(
                        SettingsControlsScheduleNoticeKind.SAVED,
                        editor.kind,
                        editor.label,
                        every = SettingsControlsLogic.every(seconds),
                    )
                mutableState.update { it.copy(notice = notice) }
                refresh()
            }
            is ApiResult.Failed -> {
                draft = SettingsControlsDraft(current.kind, current.amount, error = result.error)
                rebuild()
            }
        }
    }

    suspend fun setEnabled(
        kind: String,
        enabled: Boolean,
    ) {
        val row = rows.firstOrNull { it.kind == kind } ?: return
        if (!mutableState.value.canChange || !SettingsControlsLogic.toggleable(kind) || mutableState.value.busyKind.isNotEmpty()) return
        val label = effective(row).label
        mutableState.update { it.copy(busyKind = kind) }
        rebuild()
        when (
            val result =
                api.result(
                    SettingsControlsEndpoints.changeSchedule,
                    SettingsScheduleChange(enabled = enabled),
                    "kind" to kind,
                )
        ) {
            is ApiResult.Ok -> {
                val noticeKind =
                    if (enabled) SettingsControlsScheduleNoticeKind.AUTO_ON else SettingsControlsScheduleNoticeKind.AUTO_OFF
                mutableState.update { it.copy(busyKind = "", notice = notice(noticeKind, kind, label)) }
                refresh()
            }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(
                        busyKind = "",
                        notice = notice(SettingsControlsScheduleNoticeKind.SAVE_FAILED, kind, label, error = result.error),
                    )
                }
        }
        rebuild()
    }

    private class Effective(
        val label: String,
        val about: String,
        val min: Long,
        val max: Long,
        val step: Long,
    )

    private fun effective(row: SettingsScheduleRow): Effective {
        val info = SettingsControlsLogic.kinds[row.kind]
        return Effective(
            label = row.label.ifEmpty { info?.label ?: row.kind.replace('_', ' ') },
            about = row.about.ifEmpty { info?.about.orEmpty() },
            min = row.minSeconds.takeIf { it > 0 } ?: info?.min ?: DEFAULT_MIN,
            max = row.maxSeconds.takeIf { it > 0 } ?: info?.max ?: DEFAULT_MAX,
            step = row.stepSeconds.takeIf { it > 0 } ?: info?.step ?: DEFAULT_MIN,
        )
    }

    private fun notice(
        kind: SettingsControlsScheduleNoticeKind,
        schedule: String,
        label: String,
        detail: String = "",
        reason: SettingsControlsReason = SettingsControlsReason.NONE,
        every: SettingsControlsEvery = SettingsControlsEvery(SettingsControlsEveryUnit.ALWAYS, 0),
        error: ApiError? = null,
    ): SettingsControlsScheduleNotice = SettingsControlsScheduleNotice(++notices, kind, schedule, label, detail, reason, every, error)

    private suspend fun settle() {
        for ((kind, jobId) in watching.toMap()) {
            val row = rows.firstOrNull { it.kind == kind }
            if (row == null) {
                watching.remove(kind)
                continue
            }
            if (SettingsControlsLogic.inLine(row)) continue
            val job = api.result(LibraryEndpoints.job, "id" to jobId).valueOrNull()
            if (job != null && !job.finished) continue
            watching.remove(kind)
            val label = effective(row).label
            val failed = (job?.state ?: row.lastJob) == "failed"
            val told =
                when {
                    failed -> {
                        val detail = job?.error?.message?.takeIf { it.isNotEmpty() } ?: row.lastJobError
                        notice(SettingsControlsScheduleNoticeKind.JOB_FAILED, kind, label, detail = detail)
                    }
                    row.lastJobNote.isNotEmpty() ->
                        notice(
                            SettingsControlsScheduleNoticeKind.NOTHING,
                            kind,
                            label,
                            reason = SettingsControlsLogic.reason(kind, row.lastJobNote),
                        )
                    else -> notice(SettingsControlsScheduleNoticeKind.FINISHED, kind, label)
                }
            mutableState.update { it.copy(notice = told) }
        }
    }

    private fun rebuild() {
        val current = mutableState.value
        val shown =
            rows.map { row ->
                val info = effective(row)
                val seen = SettingsControlsLogic.lastSeen(row)
                val failed = row.lastJob == "failed"
                val code = if (failed) "" else seen.why
                SettingsControlsScheduleRow(
                    kind = row.kind,
                    label = info.label,
                    about = info.about,
                    dot = SettingsControlsLogic.dot(row),
                    every = SettingsControlsLogic.every(row.everySeconds),
                    lastAt = seen.at,
                    took = seen.tookSeconds?.let { SettingsControlsLogic.duration(it) }.orEmpty(),
                    next = SettingsControlsLogic.next(row, now),
                    failed = failed,
                    failedError = if (failed) row.lastJobError else "",
                    reason = SettingsControlsLogic.reason(row.kind, code),
                    reasonLinks = code.isNotEmpty() && SettingsControlsLogic.reasonLinks(code) && SettingsControlsLogic.page(row) != null,
                    pageKey = SettingsControlsLogic.page(row)?.key.orEmpty(),
                    canRun = current.canRun && SettingsControlsLogic.mayRun(row),
                    canChange = current.canChange,
                    spinning =
                        (current.busyKind == row.kind && draft?.kind != row.kind) ||
                            (row.kind in watching && SettingsControlsLogic.inLine(row)),
                    enabled = row.enabled,
                    toggleable = SettingsControlsLogic.toggleable(row.kind),
                    stale = SettingsControlsLogic.stale(row),
                    editing = draft?.kind == row.kind,
                )
            }
        val groups =
            SettingsControlsScheduleGroup.entries
                .map { group -> SettingsControlsScheduleGroupRows(group, shown.filter { SettingsControlsLogic.group(it.kind) == group }) }
                .filter { it.rows.isNotEmpty() }
        val running =
            rows
                .filter { SettingsControlsLogic.inLine(it) }
                .map { SettingsControlsRunningRow(it.kind, effective(it).label, it.state == "running") }
        val loopRows =
            loops.map { loop ->
                val run = SettingsControlsLogic.run(loop.state, loop.stuck)
                val tone = SettingsControlsLogic.tone(run)
                SettingsControlsLoopRow(
                    key = loop.key,
                    name = loop.name,
                    reason = loop.reason,
                    run = run,
                    tone = tone,
                    dot =
                        when (tone) {
                            Tone.OK -> SettingsControlsDot.OK
                            Tone.FAIL -> SettingsControlsDot.FAIL
                            Tone.WAIT -> SettingsControlsDot.BUSY
                            else -> SettingsControlsDot.OFF
                        },
                    every = SettingsControlsLogic.every(loop.everySeconds),
                    lastAt = loop.lastPass,
                    next = SettingsControlsLogic.until(loop.nextPass, now),
                )
            }
        mutableState.update {
            it.copy(safeMode = safeMode, running = running, groups = groups, loops = loopRows, editor = editor(current.busyKind))
        }
    }

    private fun editor(busyKind: String): SettingsControlsEditor? {
        val current = draft ?: return null
        val row = rows.firstOrNull { it.kind == current.kind } ?: return null
        val info = effective(row)
        val unit = SettingsControlsLogic.intervalUnit(info.step)
        val size = SettingsControlsLogic.unitSize(unit)
        val seconds = (current.amount.toLongOrNull() ?: -1L) * size
        return SettingsControlsEditor(
            kind = row.kind,
            label = info.label,
            about = info.about,
            unit = unit,
            amount = current.amount,
            min = info.min,
            max = info.max,
            step = info.step,
            seconds = seconds,
            valid = current.amount.toLongOrNull() != null && SettingsControlsLogic.intervalValid(seconds, info.min, info.max, info.step),
            toggleable = SettingsControlsLogic.toggleable(row.kind),
            enabled = row.enabled,
            pageKey = SettingsControlsLogic.page(row)?.key.orEmpty(),
            saving = current.saving,
            busy = busyKind == row.kind,
            error = current.error,
        )
    }

    private companion object {
        const val TOPIC = "job"
        const val POLL_MILLIS = 30_000L
        const val WATCH_MILLIS = 3_000L
        const val AMOUNT_DIGITS = 6
        const val DEFAULT_MIN = 60L
        const val DEFAULT_MAX = 7 * 86_400L
    }
}
