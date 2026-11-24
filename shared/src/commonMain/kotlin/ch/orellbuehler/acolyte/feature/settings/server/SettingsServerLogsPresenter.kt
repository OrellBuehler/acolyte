package ch.orellbuehler.acolyte.feature.settings.server

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsServerEndpoints
import ch.orellbuehler.acolyte.api.clearTransfers
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val SETTINGS_LOGS_LIMIT = 100
const val SETTINGS_LOGS_QUERY_MAX = 200

data class SettingsServerLogsFilter(
    val level: String = "",
    val query: String = "",
    val from: String = "",
    val to: String = "",
) {
    val active: Boolean get() = level.isNotEmpty() || query.isNotEmpty() || from.isNotEmpty() || to.isNotEmpty()
}

data class SettingsServerLogLineRow(
    val at: String,
    val level: String,
    val text: String,
    val tone: Tone,
)

data class SettingsServerLogsState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val filter: SettingsServerLogsFilter = SettingsServerLogsFilter(),
    val lines: List<SettingsServerLogLineRow> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = SETTINGS_LOGS_LIMIT,
    val busy: Boolean = false,
    val noLogFile: Boolean = false,
    val downloading: Boolean = false,
    val downloadError: ApiError? = null,
    val file: SettingsServerFile? = null,
) {
    val rangeFrom: Int get() = offset + 1

    val rangeTo: Int get() = offset + lines.size

    val hasNewer: Boolean get() = offset > 0

    val hasOlder: Boolean get() = offset + lines.size < total

    val noMatch: Boolean get() = status.phase == Phase.READY && total == 0
}

class SettingsServerLogsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
) {
    private val mutableState = MutableStateFlow(SettingsServerLogsState())
    private val lock = Mutex()
    private var files = 0
    private var seq = 0

    val state: StateFlow<SettingsServerLogsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        val me = account()
        if (me == null || !me.can(Need.USERS)) {
            mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
            return
        }
        val current = mutableState.value
        fetch(current.filter, current.offset)
    }

    suspend fun applyFilter(
        level: String,
        query: String,
        from: String,
        to: String,
    ) {
        val filter = SettingsServerLogsFilter(normalLevel(level), query.trim().take(SETTINGS_LOGS_QUERY_MAX), from.trim(), to.trim())
        fetch(filter, 0)
    }

    suspend fun clear() {
        fetch(SettingsServerLogsFilter(), 0)
    }

    suspend fun older() {
        val s = mutableState.value
        if (s.hasOlder && !s.busy) fetch(s.filter, s.offset + s.limit)
    }

    suspend fun newer() {
        val s = mutableState.value
        if (s.hasNewer && !s.busy) fetch(s.filter, maxOf(0, s.offset - s.limit))
    }

    private fun normalLevel(level: String): String = if (level == "warning" || level == "error" || level == "info") level else ""

    private suspend fun fetch(
        filter: SettingsServerLogsFilter,
        offset: Int,
    ) {
        val mine = ++seq
        mutableState.update { it.copy(status = it.status.started(), filter = filter, busy = true, noLogFile = false) }
        reauth.guardError(onFailure = { error -> failed(mine, error) }, onCancel = ::cancelled) {
            val result =
                api.result(
                    SettingsServerEndpoints.logs,
                    "level" to filter.level.ifEmpty { null },
                    "q" to filter.query.ifEmpty { null },
                    "from" to filter.from.ifEmpty { null },
                    "to" to filter.to.ifEmpty { null },
                    "offset" to offset.takeIf { it > 0 },
                    "limit" to SETTINGS_LOGS_LIMIT,
                )
            when {
                mine != seq -> null
                result is ApiResult.Ok -> {
                    val page = result.value
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            busy = false,
                            lines = page.lines.map { l -> SettingsServerLogLineRow(l.at, l.level, l.text, toneOf(l.level)) },
                            total = page.total,
                            offset = page.offset,
                            limit = page.limit.takeIf { l -> l > 0 } ?: SETTINGS_LOGS_LIMIT,
                        )
                    }
                    null
                }
                result is ApiResult.Failed -> result.error
                else -> null
            }
        }
    }

    private fun failed(
        mine: Int,
        error: ApiError,
    ) {
        if (mine != seq) return
        mutableState.update {
            it.copy(status = it.status.failed(error), busy = false, noLogFile = error.status == NO_LOG_FILE)
        }
    }

    private fun toneOf(level: String): Tone =
        when (level) {
            "error" -> Tone.FAIL
            "warning" -> Tone.WAIT
            else -> Tone.IDLE
        }

    suspend fun download() {
        lock.withLock {
            if (mutableState.value.downloading) return
            clearTransfers()
            mutableState.update { it.copy(downloading = true, downloadError = null, file = null) }
            reauth.guardError(
                onFailure = { error -> mutableState.update { it.copy(downloading = false, downloadError = error) } },
                onCancel = ::cancelled,
            ) {
                when (val result = api.download(SettingsServerEndpoints.downloadLogs, "holyarrs-log.txt")) {
                    is ApiResult.Ok -> {
                        val file = result.value
                        mutableState.update {
                            it.copy(
                                downloading = false,
                                file = SettingsServerFile(++files, SettingsServerFileKind.LOG, file.path, file.name, file.bytes),
                            )
                        }
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    fun fileDelivered() {
        mutableState.update { it.copy(file = null) }
    }

    fun release() {
        mutableState.update { it.copy(file = null) }
        clearTransfers()
    }

    fun dismissDownloadError() {
        mutableState.update { it.copy(downloadError = null) }
    }

    private fun cancelled() {
        mutableState.update {
            val status = if (it.status.phase == Phase.LOADING) it.status.failed(ApiError(ApiErrorCode.REAUTH, 403)) else it.status
            it.copy(status = status, busy = false, downloading = false)
        }
    }

    private companion object {
        const val NO_LOG_FILE = 503
    }
}
