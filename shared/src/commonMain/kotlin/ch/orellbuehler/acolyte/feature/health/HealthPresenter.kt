package ch.orellbuehler.acolyte.feature.health

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AppEndpoints
import ch.orellbuehler.acolyte.api.DownloadersEndpoints
import ch.orellbuehler.acolyte.api.HealthEndpoints
import ch.orellbuehler.acolyte.api.HomeEndpoints
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.OutsideListsEndpoints
import ch.orellbuehler.acolyte.api.SettingsLookEndpoints
import ch.orellbuehler.acolyte.api.SettingsServerEndpoints
import ch.orellbuehler.acolyte.api.SettingsSharedEndpoints
import ch.orellbuehler.acolyte.api.SettingsStatusEndpoints
import ch.orellbuehler.acolyte.api.SourcesEndpoints
import ch.orellbuehler.acolyte.api.TransferredFile
import ch.orellbuehler.acolyte.api.model.About
import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.api.model.HealthChecks
import ch.orellbuehler.acolyte.api.model.HomeHealth
import ch.orellbuehler.acolyte.api.model.JobList
import ch.orellbuehler.acolyte.api.model.OutsideList
import ch.orellbuehler.acolyte.api.model.OutsideLists
import ch.orellbuehler.acolyte.api.model.SettingsHealthStates
import ch.orellbuehler.acolyte.api.model.SettingsScheduleRow
import ch.orellbuehler.acolyte.api.model.SettingsSchedules
import ch.orellbuehler.acolyte.api.model.SettingsStageState
import ch.orellbuehler.acolyte.api.model.SettingsStatusActiveJob
import ch.orellbuehler.acolyte.api.model.SettingsStatusActiveJobs
import ch.orellbuehler.acolyte.api.model.SettingsStatusAlert
import ch.orellbuehler.acolyte.api.model.SettingsStatusAlerts
import ch.orellbuehler.acolyte.api.model.SettingsStatusDisk
import ch.orellbuehler.acolyte.api.model.SettingsStatusDisks
import ch.orellbuehler.acolyte.api.model.SettingsStatusInstall
import ch.orellbuehler.acolyte.api.model.SettingsStatusMetadata
import ch.orellbuehler.acolyte.api.model.SettingsStatusModeChange
import ch.orellbuehler.acolyte.api.model.SettingsStatusTest
import ch.orellbuehler.acolyte.api.model.SettingsUpdateInfo
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.home.systemMillis
import ch.orellbuehler.acolyte.feature.settings.status.HistoryRun
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusAbout
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusAboutLoad
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusCheckKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDiskView
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusFinished
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusGroup
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusHeadline
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusInput
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusInstallView
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusJobLine
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMarker
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMode
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusNotice
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusNoticeKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusPhase
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRead
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusReason
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRow
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRules
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRunning
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusStanding
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusTitle
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusUpcoming
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusUpdates
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant

data class HealthState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canAct: Boolean = false,
    val canManageUpdates: Boolean = false,
    val canBundle: Boolean = false,
    val nowMillis: Long = 0,
    val headline: SettingsStatusHeadline = SettingsStatusHeadline(),
    val failedReads: List<SettingsStatusRead> = emptyList(),
    val problems: List<SettingsStatusRow> = emptyList(),
    val nothingLinked: Boolean = false,
    val linkedUnreadable: Boolean = false,
    val kinds: List<SettingsStatusKind> = emptyList(),
    val defaultKind: SettingsStatusGroup = SettingsStatusGroup.SOURCES,
    val disks: List<SettingsStatusDiskView> = emptyList(),
    val disksLoaded: Boolean = false,
    val backups: SettingsStatusRow? = null,
    val keyKit: SettingsStatusRow? = null,
    val jobLines: List<SettingsStatusJobLine> = emptyList(),
    val jobsBadge: SettingsStatusMarker = SettingsStatusMarker.NONE,
    val running: List<SettingsStatusRunning> = emptyList(),
    val nextUp: List<SettingsStatusUpcoming> = emptyList(),
    val finished: List<SettingsStatusFinished> = emptyList(),
    val updateOn: Boolean = false,
    val updateVersion: String = "",
    val updateLatest: String = "",
    val updateCheckedAt: String = "",
    val updateProblem: String = "",
    val updateFailed: Boolean = false,
    val standing: SettingsStatusStanding = SettingsStatusStanding.LOADING,
    val updateNotes: List<String> = emptyList(),
    val updateLink: String = "",
    val updateAvailable: Boolean = false,
    val checkingUpdate: Boolean = false,
    val install: SettingsStatusInstallView? = null,
    val installPhase: SettingsStatusPhase = SettingsStatusPhase.IDLE,
    val installMode: SettingsStatusMode = SettingsStatusMode.OFF,
    val installMoving: Boolean = false,
    val installBusy: Boolean = false,
    val about: SettingsStatusAbout = SettingsStatusAbout(),
    val aboutLoad: SettingsStatusAboutLoad = SettingsStatusAboutLoad.LOADING,
    val bundleBusy: Boolean = false,
    val bundle: TransferredFile? = null,
    val busy: List<String> = emptyList(),
    val notice: SettingsStatusNotice? = null,
    val actionError: ApiError? = null,
) {
    val isEmpty: Boolean get() = false

    val installLocked: Boolean get() = install?.modeLocked == true

    val installReady: Boolean get() = install?.ready.orEmpty().isNotEmpty() && !installMoving

    val installCanGoBack: Boolean get() = install?.previous.orEmpty().isNotEmpty() && !installMoving

    fun isBusy(key: String): Boolean = key in busy
}

private data class Raw(
    val states: List<SettingsStageState>? = null,
    val checks: List<HealthCheck>? = null,
    val schedules: List<SettingsScheduleRow>? = null,
    val lists: List<OutsideList>? = null,
    val alerts: List<SettingsStatusAlert>? = null,
    val metadataUp: Boolean? = null,
    val metadataAt: String = "",
    val active: List<SettingsStatusActiveJob> = emptyList(),
    val history: List<HistoryRun> = emptyList(),
    val disks: List<SettingsStatusDisk>? = null,
    val disksAt: Long = 0,
    val update: SettingsUpdateInfo? = null,
    val updateFailed: Boolean = false,
    val install: SettingsStatusInstall? = null,
    val about: About? = null,
    val aboutAt: Long = 0,
    val aboutLoad: SettingsStatusAboutLoad = SettingsStatusAboutLoad.LOADING,
    val server: HomeHealth? = null,
    val failed: List<SettingsStatusRead> = emptyList(),
    val hidden: List<SettingsStatusRead> = emptyList(),
    val checkingUpdate: Boolean = false,
    val installBusy: Boolean = false,
    val bundleBusy: Boolean = false,
    val bundle: TransferredFile? = null,
    val notice: SettingsStatusNotice? = null,
    val actionError: ApiError? = null,
)

private class Reads(
    val states: ApiResult<SettingsHealthStates>,
    val checks: ApiResult<HealthChecks>,
    val schedules: ApiResult<SettingsSchedules>,
    val lists: ApiResult<OutsideLists>?,
    val alerts: ApiResult<SettingsStatusAlerts>?,
    val metadata: ApiResult<SettingsStatusMetadata>,
    val active: ApiResult<SettingsStatusActiveJobs>,
    val history: ApiResult<JobList>,
    val disks: ApiResult<SettingsStatusDisks>?,
    val update: ApiResult<SettingsUpdateInfo>?,
    val install: ApiResult<SettingsStatusInstall>?,
    val about: ApiResult<About>?,
    val server: ApiResult<HomeHealth>?,
)

class HealthPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val clock: () -> Long = ::systemMillis,
) {
    private val mutableState = MutableStateFlow(HealthState())
    private val lock = Mutex()
    private var raw = Raw()
    private var rows: List<SettingsStatusRow> = emptyList()
    private val busy = mutableListOf<String>()
    private var noticeId = 0
    private var fullPending = false

    val state: StateFlow<HealthState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() = reload(full = true, indicator = true, disks = true)

    suspend fun runChecks() = checkAll()

    suspend fun watch() {
        coroutineScope {
            val poke = Channel<Unit>(Channel.CONFLATED)
            if (mutableState.value.status.phase == Phase.READY) poke.trySend(Unit)
            launch {
                api.live.collect { event ->
                    if (event.topic == HEALTH_TOPIC || event.topic == JOB_TOPIC || event.topic == LiveEvent.RESYNC) poke.trySend(Unit)
                }
            }
            launch {
                for (ignored in poke) {
                    delay(LIVE_DELAY)
                    val full = fullPending
                    fullPending = false
                    reload(full = full, indicator = false, disks = false)
                }
            }
            launch {
                while (true) {
                    delay(POLL_MILLIS)
                    reload(full = false, indicator = false, disks = false)
                }
            }
            launch {
                mutableState
                    .map { it.installMoving }
                    .distinctUntilChanged()
                    .collectLatest { moving -> if (moving) followInstall(poke) }
            }
        }
    }

    private suspend fun followInstall(poke: Channel<Unit>) {
        val was =
            mutableState.value.install
                ?.running
                .orEmpty()
        while (true) {
            delay(INSTALL_POLL_MILLIS)
            val result = api.result(SettingsStatusEndpoints.install)
            if (result is ApiResult.Ok) {
                if (result.value.running != was) {
                    fullPending = true
                    poke.trySend(Unit)
                }
                commit { it.copy(install = result.value) }
            }
        }
    }

    private suspend fun reload(
        full: Boolean,
        indicator: Boolean,
        disks: Boolean,
    ) {
        lock.withLock {
            val me = account()
            if (me == null || !me.can(Need.MANAGE_LIBRARY)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            if (indicator) mutableState.update { it.copy(status = it.status.started()) }
            val act = me.can(Need.SETTINGS)
            val now = clock()
            val loadDisks = disks || raw.disks == null || now - raw.disksAt >= DISK_MILLIS
            val reads =
                coroutineScope {
                    val states = async { api.result(SettingsSharedEndpoints.healthStates) }
                    val checks = async { api.result(HealthEndpoints.checks) }
                    val schedules = async { api.result(SettingsSharedEndpoints.schedules) }
                    val lists = async { if (act) api.result(OutsideListsEndpoints.lists) else null }
                    val alerts = async { if (act) api.result(SettingsServerEndpoints.alerts) else null }
                    val metadata = async { api.result(SettingsLookEndpoints.metadataHealth) }
                    val active = async { api.result(SettingsStatusEndpoints.activeJobs) }
                    val history = async { api.result(LibraryEndpoints.jobs, "limit" to HISTORY_LIMIT) }
                    val diskRead = async { if (loadDisks) api.result(SettingsStatusEndpoints.disks) else null }
                    val update = async { if (full) api.result(SettingsSharedEndpoints.updateInfo) else null }
                    val install = async { if (act) api.result(SettingsStatusEndpoints.install) else null }
                    val about = async { if (full && act) api.result(AppEndpoints.about) else null }
                    val server = async { if (full) api.result(HomeEndpoints.health) else null }
                    Reads(
                        states.await(),
                        checks.await(),
                        schedules.await(),
                        lists.await(),
                        alerts.await(),
                        metadata.await(),
                        active.await(),
                        history.await(),
                        diskRead.await(),
                        update.await(),
                        install.await(),
                        about.await(),
                        server.await(),
                    )
                }
            merge(reads, now, act)
        }
    }

    private fun merge(
        r: Reads,
        now: Long,
        act: Boolean,
    ) {
        val states = r.states
        val schedules = r.schedules
        if (states is ApiResult.Failed && schedules is ApiResult.Failed) {
            mutableState.update { it.copy(status = it.status.failed(states.error), allowed = true) }
            return
        }
        val failed = mutableListOf<SettingsStatusRead>()
        val hidden = mutableListOf<SettingsStatusRead>()
        if (states is ApiResult.Failed) failed += SettingsStatusRead.PARTS
        if (r.checks is ApiResult.Failed) failed += SettingsStatusRead.CHECKS
        if (schedules is ApiResult.Failed) failed += SettingsStatusRead.JOBS
        sortRead(r.lists, SettingsStatusRead.LISTS, act, failed, hidden)
        sortRead(r.alerts, SettingsStatusRead.ALERTS, act, failed, hidden)
        commit(ready = true) { old ->
            var next = old.copy(failed = failed, hidden = hidden)
            next = next.copy(states = (states as? ApiResult.Ok)?.value?.states)
            next = next.copy(checks = (r.checks as? ApiResult.Ok)?.value?.checks)
            next = next.copy(schedules = (schedules as? ApiResult.Ok)?.value?.schedules)
            next = next.copy(lists = (r.lists as? ApiResult.Ok)?.value?.outsideLists)
            next = next.copy(alerts = (r.alerts as? ApiResult.Ok)?.value?.alerts)
            next =
                next.copy(
                    metadataUp = (r.metadata as? ApiResult.Ok)?.value?.up ?: false,
                    metadataAt = Instant.fromEpochMilliseconds(now).toString(),
                )
            if (r.active is ApiResult.Ok) next = next.copy(active = r.active.value.jobs)
            if (r.history is ApiResult.Ok) {
                next =
                    next.copy(
                        history =
                            r.history.value.jobs
                                .map { HistoryRun(it.id, it.kind, it.state, it.endedAt.orEmpty()) },
                    )
            }
            if (r.disks is ApiResult.Ok) next = next.copy(disks = r.disks.value.disks, disksAt = now)
            if (r.disks is ApiResult.Failed && next.disks == null) next = next.copy(disks = emptyList(), disksAt = now)
            if (r.update is ApiResult.Ok) next = next.copy(update = r.update.value, updateFailed = false)
            if (r.update is ApiResult.Failed) next = next.copy(updateFailed = next.update == null)
            if (!act) next = next.copy(install = null)
            if (r.install is ApiResult.Ok) next = next.copy(install = r.install.value)
            if (r.install is ApiResult.Failed &&
                r.install.error.code
                    .isDenied()
            ) {
                next = next.copy(install = null)
            }
            if (r.server is ApiResult.Ok) next = next.copy(server = r.server.value)
            aboutRead(next, r.about, act, now)
        }
    }

    private fun aboutRead(
        old: Raw,
        read: ApiResult<About>?,
        act: Boolean,
        now: Long,
    ): Raw =
        when {
            !act -> old.copy(aboutLoad = SettingsStatusAboutLoad.DENIED)
            read is ApiResult.Ok -> old.copy(about = read.value, aboutAt = now, aboutLoad = SettingsStatusAboutLoad.READY)
            read is ApiResult.Failed && read.error.code.isDenied() -> old.copy(aboutLoad = SettingsStatusAboutLoad.DENIED)
            read is ApiResult.Failed && old.about == null -> old.copy(aboutLoad = SettingsStatusAboutLoad.FAILED)
            else -> old
        }

    private fun <T> sortRead(
        read: ApiResult<T>?,
        which: SettingsStatusRead,
        act: Boolean,
        failed: MutableList<SettingsStatusRead>,
        hidden: MutableList<SettingsStatusRead>,
    ) {
        if (!act) {
            hidden += which
            return
        }
        if (read !is ApiResult.Failed) return
        if (read.error.code.isDenied() || read.error.code == ApiErrorCode.PART_OFF) hidden += which else failed += which
    }

    suspend fun checkAll() {
        if (!canAct() || !begin(ALL_KEY)) return
        try {
            when (val result = api.result(HealthEndpoints.runChecks)) {
                is ApiResult.Ok -> {
                    commit { it.copy(actionError = null) }
                    reload(full = false, indicator = false, disks = true)
                    notice(SettingsStatusNoticeKind.CHECKS_RAN)
                }
                is ApiResult.Failed -> {
                    commit { it.copy(actionError = result.error) }
                    notice(SettingsStatusNoticeKind.CHECKS_FAILED, error = result.error)
                }
            }
        } finally {
            end(ALL_KEY)
        }
    }

    suspend fun check(key: String) {
        val row = rows.firstOrNull { it.key == key } ?: return
        val check = row.check ?: return
        if (!canAct() || !begin(key)) return
        try {
            when (check.kind) {
                SettingsStatusCheckKind.JOB -> runJob(row, check.job)
                SettingsStatusCheckKind.CHECKS -> runRowChecks(row)
                SettingsStatusCheckKind.SOURCE ->
                    testLinked(
                        row,
                        api.result(SourcesEndpoints.test, "id" to check.id).mapTest {
                            it.ok to
                                it.text
                        },
                    )
                SettingsStatusCheckKind.DOWNLOADER ->
                    testLinked(
                        row,
                        api.result(DownloadersEndpoints.test, null, "id" to check.id).mapTest {
                            it.ok to
                                it.text
                        },
                    )
            }
        } finally {
            end(key)
        }
    }

    private suspend fun runJob(
        row: SettingsStatusRow,
        job: String,
    ) {
        when (val result = api.result(SettingsSharedEndpoints.runSchedule, "kind" to job)) {
            is ApiResult.Ok -> {
                notice(if (result.value.already) SettingsStatusNoticeKind.JOB_ALREADY else SettingsStatusNoticeKind.JOB_STARTED, row)
                reload(full = false, indicator = false, disks = false)
            }
            is ApiResult.Failed -> notice(SettingsStatusNoticeKind.ROW_CANT_CHECK, row, error = result.error)
        }
    }

    private suspend fun runRowChecks(row: SettingsStatusRow) {
        when (val result = api.result(HealthEndpoints.runChecks)) {
            is ApiResult.Ok -> {
                reload(full = false, indicator = false, disks = true)
                val now = rows.firstOrNull { it.key == row.key }
                when {
                    now == null -> notice(SettingsStatusNoticeKind.CHECKS_RAN)
                    now.tone == Tone.OK -> notice(SettingsStatusNoticeKind.ROW_WORKING, now)
                    else -> notice(SettingsStatusNoticeKind.ROW_WORD, now)
                }
            }
            is ApiResult.Failed -> notice(SettingsStatusNoticeKind.ROW_CANT_CHECK, row, error = result.error)
        }
    }

    private fun <T> ApiResult<T>.mapTest(read: (T) -> Pair<Boolean, String>): ApiResult<SettingsStatusTest> =
        when (this) {
            is ApiResult.Ok -> read(value).let { ApiResult.Ok(SettingsStatusTest(it.first, it.second)) }
            is ApiResult.Failed -> this
        }

    private suspend fun testLinked(
        row: SettingsStatusRow,
        result: ApiResult<SettingsStatusTest>,
    ) {
        when (result) {
            is ApiResult.Ok -> {
                if (result.value.ok) {
                    notice(SettingsStatusNoticeKind.ROW_CONNECTED, row, SettingsStatusUpdates.testLabel(result.value.text))
                } else {
                    notice(SettingsStatusNoticeKind.ROW_CANT_CONNECT, row, result.value.text.trim())
                }
                reload(full = false, indicator = false, disks = false)
            }
            is ApiResult.Failed -> notice(SettingsStatusNoticeKind.ROW_CANT_CHECK, row, error = result.error)
        }
    }

    suspend fun checkUpdates() {
        if (!canAct() || raw.checkingUpdate) return
        commit { it.copy(checkingUpdate = true) }
        when (val result = api.result(SettingsStatusEndpoints.checkUpdate)) {
            is ApiResult.Ok -> commit { it.copy(checkingUpdate = false, update = result.value, updateFailed = false) }
            is ApiResult.Failed -> {
                commit { it.copy(checkingUpdate = false) }
                notice(SettingsStatusNoticeKind.UPDATE_CHECK_FAILED, error = result.error)
            }
        }
    }

    suspend fun setCheckOn(on: Boolean) {
        if (!canManageUpdates() || raw.install == null || raw.installBusy) return
        if (changeMode(SettingsStatusModeChange(checkOn = on))) {
            val read = api.result(SettingsSharedEndpoints.updateInfo)
            if (read is ApiResult.Ok) commit { it.copy(update = read.value) }
        }
    }

    suspend fun setMode(mode: SettingsStatusMode) {
        val install = raw.install ?: return
        if (!canManageUpdates() || raw.installBusy || install.modeLocked || !install.available) return
        changeMode(SettingsStatusModeChange(mode = mode.wire))
    }

    private suspend fun changeMode(change: SettingsStatusModeChange): Boolean {
        commit { it.copy(installBusy = true) }
        return when (val result = api.result(SettingsStatusEndpoints.changeMode, change)) {
            is ApiResult.Ok -> {
                commit { it.copy(installBusy = false, install = result.value) }
                notice(SettingsStatusNoticeKind.UPDATE_SAVED)
                true
            }
            is ApiResult.Failed -> {
                commit { it.copy(installBusy = false) }
                notice(SettingsStatusNoticeKind.UPDATE_SAVE_FAILED, error = result.error)
                false
            }
        }
    }

    suspend fun installNow() {
        if (!canManageUpdates() || raw.installBusy) return
        commit { it.copy(installBusy = true) }
        when (val result = api.result(SettingsStatusEndpoints.installNow)) {
            is ApiResult.Ok -> commit { it.copy(installBusy = false, install = result.value) }
            is ApiResult.Failed -> {
                commit { it.copy(installBusy = false) }
                notice(SettingsStatusNoticeKind.UPDATE_INSTALL_FAILED, error = result.error)
            }
        }
    }

    suspend fun goBack() {
        if (!canManageUpdates() || raw.installBusy) return
        commit { it.copy(installBusy = true) }
        when (val result = api.result(SettingsStatusEndpoints.goBack)) {
            is ApiResult.Ok -> commit { it.copy(installBusy = false, install = result.value) }
            is ApiResult.Failed -> {
                commit { it.copy(installBusy = false) }
                notice(SettingsStatusNoticeKind.UPDATE_GO_BACK_FAILED, error = result.error)
            }
        }
    }

    suspend fun downloadBundle() {
        if (!canManageUpdates() || raw.bundleBusy) return
        commit { it.copy(bundleBusy = true) }
        when (val result = api.download(SettingsStatusEndpoints.supportBundle, BUNDLE_NAME)) {
            is ApiResult.Ok -> commit { it.copy(bundleBusy = false, bundle = result.value) }
            is ApiResult.Failed -> {
                commit { it.copy(bundleBusy = false) }
                notice(SettingsStatusNoticeKind.BUNDLE_FAILED, error = result.error)
            }
        }
    }

    fun bundleHandled() = commit { it.copy(bundle = null) }

    fun dismissNotice() = commit { it.copy(notice = null) }

    private fun canAct(): Boolean = account()?.can(Need.SETTINGS) == true

    private fun canManageUpdates(): Boolean = account()?.can(Need.USERS) == true

    private fun begin(key: String): Boolean {
        if (key in busy) return false
        busy += key
        commit { it }
        return true
    }

    private fun end(key: String) {
        busy -= key
        commit { it }
    }

    private fun notice(
        kind: SettingsStatusNoticeKind,
        row: SettingsStatusRow? = null,
        detail: String = "",
        error: ApiError? = null,
    ) = commit { it.copy(notice = SettingsStatusNotice(++noticeId, kind, row, detail, error)) }

    private fun commit(
        ready: Boolean = false,
        change: (Raw) -> Raw,
    ) {
        raw = change(raw)
        val derived = derive()
        mutableState.update { old -> derived.copy(status = if (ready) LoadStatus.ready else old.status) }
    }

    private fun derive(): HealthState {
        val me = account()
        val now = clock()
        val input =
            SettingsStatusInput(
                raw.states.orEmpty(),
                raw.checks.orEmpty(),
                raw.schedules.orEmpty(),
                raw.lists.orEmpty(),
                raw.alerts.orEmpty(),
                raw.metadataUp,
                raw.metadataAt,
                now,
            )
        val all = SettingsStatusRules.build(input)
        rows = all
        val hiddenGroups =
            raw.hidden
                .mapNotNull {
                    when (it) {
                        SettingsStatusRead.LISTS -> SettingsStatusGroup.LISTS
                        SettingsStatusRead.ALERTS -> SettingsStatusGroup.ALERTS
                        else -> null
                    }
                }.toSet()
        val kinds = SettingsStatusRules.kinds(all, hiddenGroups)
        val library = all.filter { it.group == SettingsStatusGroup.LIBRARY }
        val jobs = all.filter { it.group == SettingsStatusGroup.JOBS }
        val linkedKnown = SettingsStatusRead.PARTS !in raw.failed
        val running = SettingsStatusRules.running(raw.active)
        val names = raw.schedules.orEmpty().associate { it.kind to it.label } + raw.active.associate { it.kind to it.label }
        val install = raw.install
        val users = me?.can(Need.USERS) == true
        return HealthState(
            allowed = true,
            canAct = me?.can(Need.SETTINGS) == true,
            canManageUpdates = users,
            canBundle = users,
            nowMillis = now,
            headline = SettingsStatusRules.headline(all, linkedKnown),
            failedReads = raw.failed,
            problems = SettingsStatusRules.needsAttention(all),
            nothingLinked = linkedKnown && SettingsStatusRules.nothingLinked(all),
            linkedUnreadable = !linkedKnown,
            kinds = kinds,
            defaultKind = SettingsStatusRules.defaultKind(kinds),
            disks = disksOf(library),
            disksLoaded = raw.disks != null,
            backups = library.firstOrNull { it.key.startsWith("backups") },
            keyKit =
                library.firstOrNull {
                    it.key == "backups:key" &&
                        it.tone == Tone.OK &&
                        (it.reasonKind == SettingsStatusReason.KEY_KIT || it.reasonKind == SettingsStatusReason.KEY_KIT_OWNER)
                },
            jobLines = jobs.map { SettingsStatusJobLine(it, SettingsStatusRules.nextWord(it, now)) },
            jobsBadge =
                when {
                    jobs.any { it.tone == Tone.FAIL } -> SettingsStatusMarker.FAIL
                    jobs.any { it.tone == Tone.WAIT } -> SettingsStatusMarker.WAIT
                    else -> SettingsStatusMarker.NONE
                },
            running = running,
            nextUp = SettingsStatusRules.upcoming(jobs, running, now),
            finished = SettingsStatusRules.finished(raw.history, names),
            updateOn = raw.update?.on == true,
            updateVersion = SettingsStatusUpdates.plainVersion(raw.update?.version.orEmpty()),
            updateLatest = SettingsStatusUpdates.plainVersion(raw.update?.latest.orEmpty()),
            updateCheckedAt = raw.update?.checkedAt.orEmpty(),
            updateProblem = raw.update?.problem.orEmpty(),
            updateFailed = raw.updateFailed,
            standing = SettingsStatusUpdates.standing(raw.update),
            updateNotes = SettingsStatusUpdates.noteLines(raw.update),
            updateLink = SettingsStatusUpdates.releaseLink(raw.update?.link.orEmpty()),
            updateAvailable = raw.update?.newer == true,
            checkingUpdate = raw.checkingUpdate,
            install = SettingsStatusUpdates.view(install),
            installPhase = SettingsStatusUpdates.phase(install),
            installMode = SettingsStatusUpdates.mode(install),
            installMoving = SettingsStatusUpdates.moving(install),
            installBusy = raw.installBusy,
            about = aboutOf(),
            aboutLoad = raw.aboutLoad,
            bundleBusy = raw.bundleBusy,
            bundle = raw.bundle,
            busy = busy.toList(),
            notice = raw.notice,
            actionError = raw.actionError,
        )
    }

    private fun disksOf(library: List<SettingsStatusRow>): List<SettingsStatusDiskView> =
        raw.disks.orEmpty().map { disk ->
            val own =
                library.filter {
                    (it.title == SettingsStatusTitle.FOLDER || it.title == SettingsStatusTitle.FREE_SPACE) &&
                        it.name == disk.path
                }
            val percent = SettingsStatusRules.usedPercent(disk.freeBytes, disk.totalBytes)
            SettingsStatusDiskView(
                disk.path,
                disk.label,
                SettingsStatusRules.diskKind(disk.kind),
                disk.freeBytes,
                disk.totalBytes,
                percent,
                SettingsStatusRules.diskLevel(percent),
                own.firstOrNull { it.tone == Tone.FAIL } ?: own.firstOrNull { it.tone == Tone.WAIT },
            )
        }

    private fun aboutOf(): SettingsStatusAbout {
        val about = raw.about
        val server = raw.server
        return SettingsStatusAbout(
            version = about?.version ?: raw.update?.version.orEmpty(),
            go = about?.go.orEmpty().removePrefix("go"),
            os = about?.os.orEmpty(),
            container = about?.docker == true,
            sqlite = about?.sqlite.orEmpty(),
            migration = about?.migration ?: 0,
            configDir = about?.configDir.orEmpty(),
            startDir = about?.startDir.orEmpty(),
            uptimeSeconds = about?.uptimeSeconds ?: 0,
            startedAt = about?.startedAt.orEmpty(),
            hasDetails = about != null,
            hasServer = server != null,
            writes = server?.writes ?: 0,
            writeWaitP95Ms = server?.writeWaitP95Ms ?: 0.0,
            urlBase = server?.urlBase.orEmpty(),
            liveClients = server?.liveClients ?: 0,
            loadedAt = raw.aboutAt,
        )
    }

    private companion object {
        const val HEALTH_TOPIC = "health"
        const val JOB_TOPIC = "job"
        const val LIVE_DELAY = 400L
        const val POLL_MILLIS = 60_000L
        const val INSTALL_POLL_MILLIS = 2_000L
        const val DISK_MILLIS = 60_000L
        const val HISTORY_LIMIT = 80
        const val ALL_KEY = "all"
        const val BUNDLE_NAME = "holyarrs-support.zip"
    }
}

private fun ApiErrorCode.isDenied(): Boolean = this == ApiErrorCode.FORBIDDEN || this == ApiErrorCode.REAUTH
