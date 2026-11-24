package ch.orellbuehler.acolyte.feature.settings.status

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.HealthEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsSharedEndpoints
import ch.orellbuehler.acolyte.api.SettingsStatusEndpoints
import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.api.model.HealthChecks
import ch.orellbuehler.acolyte.api.model.SettingsHealthChecksChange
import ch.orellbuehler.acolyte.api.model.SettingsHealthChecksSettings
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SettingsHealthChecksKind {
    LIBRARY_FOLDER,
    DISK_SPACE,
    SOURCE,
    DOWNLOADER,
    BACKUPS,
    MASTER_KEY,
    UPDATE,
    CONFIG_DISK,
    CONFIG_FOLDER,
    WAITING_DOWNLOADS,
    CLEAR_WAITING,
    SAVE_FOLDER,
    SAME_DOWNLOADER,
    TORRENT_DOWNLOADER,
    USENET_DOWNLOADER,
    TRAILERS,
    PEOPLE_CHECK,
    OTHER,
}

enum class SettingsHealthChecksField {
    EVERY_MINUTES,
    FLAP_GUARD,
    LOW_SPACE,
}

enum class SettingsHealthChecksNoticeKind {
    SAVED,
    RUN_FAILED,
}

data class SettingsHealthChecksNotice(
    val id: Int,
    val kind: SettingsHealthChecksNoticeKind,
    val error: ApiError? = null,
)

data class SettingsHealthChecksRow(
    val key: String,
    val kind: SettingsHealthChecksKind,
    val label: String,
    val subject: String,
    val fullSubject: String,
    val message: String,
    val tone: Tone,
    val since: String,
)

data class SettingsHealthChecksSync(
    val id: Long,
    val name: String,
    val degraded: Boolean,
    val seenAt: String,
)

data class SettingsHealthChecksState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canSeeChecks: Boolean = false,
    val canEdit: Boolean = false,
    val worst: Tone = Tone.IDLE,
    val checks: List<SettingsHealthChecksRow> = emptyList(),
    val sync: List<SettingsHealthChecksSync> = emptyList(),
    val saved: SettingsHealthChecksSettings? = null,
    val draft: SettingsHealthChecksSettings = SettingsHealthChecksSettings(),
    val everyText: String = "",
    val guardText: String = "",
    val lowText: String = "",
    val version: String = "",
    val running: Boolean = false,
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val notice: SettingsHealthChecksNotice? = null,
) {
    val hasSettings: Boolean get() = saved != null

    val everyValid: Boolean get() = parseHealthChecksNumber(everyText, EVERY_MIN, EVERY_MAX) != null

    val guardValid: Boolean get() = parseHealthChecksNumber(guardText, GUARD_MIN, GUARD_MAX) != null

    val lowValid: Boolean get() = parseHealthChecksNumber(lowText, 0, LOW_MAX) != null

    val valid: Boolean get() = everyValid && guardValid && lowValid

    val dirty: Boolean
        get() {
            val base = saved ?: return false
            return draft != base ||
                everyText != base.everyMinutes.toString() ||
                guardText != base.flapGuard.toString() ||
                lowText != base.lowSpaceMb.toString()
        }

    val canSave: Boolean get() = canEdit && dirty && valid && !saving

    val days: List<Int> get() = UPDATE_DAYS
}

internal val UPDATE_DAYS: List<Int> = listOf(1, 3, 7, 14, 30)

internal const val EVERY_MIN = 1
internal const val EVERY_MAX = 1440
internal const val GUARD_MIN = 1
internal const val GUARD_MAX = 20
internal const val LOW_MAX = 16_777_216

internal fun parseHealthChecksNumber(
    text: String,
    min: Int,
    max: Int,
): Int? = text.trim().toIntOrNull()?.takeIf { it in min..max }

internal object SettingsHealthChecksRules {
    private val labels =
        mapOf(
            "library_folder" to (SettingsHealthChecksKind.LIBRARY_FOLDER to "Library folder"),
            "disk_space" to (SettingsHealthChecksKind.DISK_SPACE to "Free space"),
            "source" to (SettingsHealthChecksKind.SOURCE to "Source"),
            "downloader" to (SettingsHealthChecksKind.DOWNLOADER to "Downloader"),
            "backups" to (SettingsHealthChecksKind.BACKUPS to "Backups"),
            "update" to (SettingsHealthChecksKind.UPDATE to "Update"),
            "config_disk" to (SettingsHealthChecksKind.CONFIG_DISK to "Config folder space"),
            "config_folder" to (SettingsHealthChecksKind.CONFIG_FOLDER to "Config folder"),
            "waiting_downloads" to (SettingsHealthChecksKind.WAITING_DOWNLOADS to "Downloads waiting for files"),
            "clear_waiting" to (SettingsHealthChecksKind.CLEAR_WAITING to "Imported downloads still in a Downloader"),
            "save_folder" to (SettingsHealthChecksKind.SAVE_FOLDER to "Downloader save folder"),
            "same_downloader" to (SettingsHealthChecksKind.SAME_DOWNLOADER to "Downloader added twice"),
            "trailers" to (SettingsHealthChecksKind.TRAILERS to "Trailer reader"),
            "people_check" to (SettingsHealthChecksKind.PEOPLE_CHECK to "People check"),
        )

    fun row(check: HealthCheck): SettingsHealthChecksRow {
        val tone =
            when (check.state) {
                "ok" -> Tone.OK
                "warning" -> Tone.WAIT
                else -> Tone.FAIL
            }
        val subject = check.subject.trim()
        if (check.key == "backups:key") {
            return SettingsHealthChecksRow(check.key, SettingsHealthChecksKind.MASTER_KEY, "", "", "", check.message, tone, check.since)
        }
        if (check.kind == "no_downloader") {
            val kind = if (subject == "usenet") SettingsHealthChecksKind.USENET_DOWNLOADER else SettingsHealthChecksKind.TORRENT_DOWNLOADER
            return SettingsHealthChecksRow(check.key, kind, "", "", "", check.message, tone, check.since)
        }
        val (kind, label) = labels[check.kind] ?: (SettingsHealthChecksKind.OTHER to check.kind)
        val (shown, rest) =
            when {
                subject.isNotEmpty() && subject.lowercase().startsWith(label.lowercase()) -> subject to ""
                subject.isNotEmpty() && label.lowercase().startsWith(subject.lowercase()) -> "" to ""
                else -> "" to subject
            }
        val own = shown.ifEmpty { if (kind == SettingsHealthChecksKind.OTHER) check.kind else "" }
        return SettingsHealthChecksRow(
            check.key,
            kind,
            own,
            SettingsStatusRules.shortPath(rest),
            rest,
            check.message,
            tone,
            check.since,
        )
    }

    fun worst(answer: HealthChecks): Tone =
        when (answer.worst) {
            "ok" -> Tone.OK
            "warning" -> Tone.WAIT
            "error" -> Tone.FAIL
            else -> Tone.IDLE
        }
}

class SettingsHealthChecksPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsHealthChecksState())
    private val lock = Mutex()
    private var noticeId = 0

    val state: StateFlow<SettingsHealthChecksState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() = reload(indicator = true)

    private suspend fun reload(indicator: Boolean) {
        lock.withLock {
            val me = account()
            val checks = me?.can(Need.MANAGE_LIBRARY) == true
            val edit = me?.can(Need.SETTINGS) == true
            if (me == null || !(checks || edit)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            if (indicator) mutableState.update { it.copy(status = it.status.started()) }
            val firstSettings = mutableState.value.saved == null
            val reads =
                coroutineScope {
                    val a = async { if (checks) api.result(HealthEndpoints.checks) else null }
                    val s = async { if (edit && firstSettings) api.result(SettingsStatusEndpoints.healthSettings) else null }
                    val i = async { if (checks) api.result(SettingsSharedEndpoints.updateInfo) else null }
                    Triple(a.await(), s.await(), i.await())
                }
            val (answer, settings, info) = reads
            val failed = (answer as? ApiResult.Failed)?.error ?: (settings as? ApiResult.Failed)?.error
            mutableState.update { current ->
                var next = current.copy(allowed = true, canSeeChecks = checks, canEdit = edit)
                if (answer is ApiResult.Ok) next = next.withChecks(answer.value)
                if (settings is ApiResult.Ok) next = next.withSaved(settings.value)
                if (info is ApiResult.Ok) next = next.copy(version = info.value.version)
                next.copy(status = if (failed == null) LoadStatus.ready else current.status.failed(failed))
            }
        }
    }

    suspend fun runChecks() {
        if (!mutableState.value.canSeeChecks || mutableState.value.running) return
        mutableState.update { it.copy(running = true) }
        when (val result = api.result(HealthEndpoints.runChecks)) {
            is ApiResult.Ok -> mutableState.update { it.withChecks(result.value).copy(running = false) }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(
                        running = false,
                        notice = SettingsHealthChecksNotice(++noticeId, SettingsHealthChecksNoticeKind.RUN_FAILED, result.error),
                    )
                }
        }
    }

    fun setNumber(
        field: SettingsHealthChecksField,
        text: String,
    ) {
        val digits = text.filter { it.isDigit() }.take(NUMBER_DIGITS)
        mutableState.update { s ->
            val value = digits.toIntOrNull()
            val draft =
                when (field) {
                    SettingsHealthChecksField.EVERY_MINUTES -> if (value != null) s.draft.copy(everyMinutes = value) else s.draft
                    SettingsHealthChecksField.FLAP_GUARD -> if (value != null) s.draft.copy(flapGuard = value) else s.draft
                    SettingsHealthChecksField.LOW_SPACE -> if (value != null) s.draft.copy(lowSpaceMb = value) else s.draft
                }
            when (field) {
                SettingsHealthChecksField.EVERY_MINUTES -> s.copy(draft = draft, everyText = digits, saveError = null)
                SettingsHealthChecksField.FLAP_GUARD -> s.copy(draft = draft, guardText = digits, saveError = null)
                SettingsHealthChecksField.LOW_SPACE -> s.copy(draft = draft, lowText = digits, saveError = null)
            }
        }
    }

    fun setDays(days: Int) = edit { it.copy(updateCheckEveryDays = days) }

    fun setFeed(url: String) = edit { it.copy(updateCheckUrl = url) }

    fun setLocal(on: Boolean) = edit { it.copy(updateCheckLocal = on) }

    fun setPrerelease(on: Boolean) = edit { it.copy(updatePrerelease = on) }

    fun discard() = mutableState.update { s -> s.saved?.let { s.withSaved(it) } ?: s }

    suspend fun save() {
        val s = mutableState.value
        if (!s.canSave) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        val body =
            SettingsHealthChecksChange(
                everyMinutes = parseHealthChecksNumber(s.everyText, EVERY_MIN, EVERY_MAX) ?: s.draft.everyMinutes,
                flapGuard = parseHealthChecksNumber(s.guardText, GUARD_MIN, GUARD_MAX) ?: s.draft.flapGuard,
                lowSpaceMb = parseHealthChecksNumber(s.lowText, 0, LOW_MAX) ?: s.draft.lowSpaceMb,
                updateCheckUrl = s.draft.updateCheckUrl.trim(),
                updateCheckLocal = s.draft.updateCheckLocal,
                updatePrerelease = s.draft.updatePrerelease,
                updateCheckEveryDays = s.draft.updateCheckEveryDays,
            )
        when (val result = api.result(SettingsStatusEndpoints.saveHealthSettings, body)) {
            is ApiResult.Ok -> {
                val info = api.result(SettingsSharedEndpoints.updateInfo).valueOrNull()
                mutableState.update {
                    it
                        .withSaved(result.value)
                        .copy(
                            saving = false,
                            version = info?.version ?: it.version,
                            notice = SettingsHealthChecksNotice(++noticeId, SettingsHealthChecksNoticeKind.SAVED),
                        )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(saving = false, saveError = result.error) }
        }
    }

    fun dismissNotice() = mutableState.update { it.copy(notice = null) }

    suspend fun watch() {
        coroutineScope {
            if (mutableState.value.status.phase == Phase.READY) launch { reload(indicator = false) }
            launch {
                api.live.collect { event ->
                    if (event.topic == TOPIC || event.topic == LiveEvent.RESYNC) reload(indicator = false)
                }
            }
            launch {
                while (true) {
                    delay(POLL_MILLIS)
                    reload(indicator = false)
                }
            }
        }
    }

    private fun edit(transform: (SettingsHealthChecksSettings) -> SettingsHealthChecksSettings) =
        mutableState.update { it.copy(draft = transform(it.draft), saveError = null) }

    private companion object {
        const val TOPIC = "health"
        const val POLL_MILLIS = 30_000L
        const val NUMBER_DIGITS = 8
    }
}

private fun SettingsHealthChecksState.withChecks(value: HealthChecks): SettingsHealthChecksState =
    copy(
        worst = SettingsHealthChecksRules.worst(value),
        checks = value.checks.map { SettingsHealthChecksRules.row(it) },
        sync = value.sync.orEmpty().map { SettingsHealthChecksSync(it.id, it.name, it.health == "degraded", it.seenAt) },
    )

private fun SettingsHealthChecksState.withSaved(value: SettingsHealthChecksSettings): SettingsHealthChecksState =
    copy(
        saved = value,
        draft = value,
        everyText = value.everyMinutes.toString(),
        guardText = value.flapGuard.toString(),
        lowText = value.lowSpaceMb.toString(),
        saveError = null,
    )
