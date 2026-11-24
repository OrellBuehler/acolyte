package ch.orellbuehler.acolyte.feature.settings.server

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsServerEndpoints
import ch.orellbuehler.acolyte.api.clearTransfers
import ch.orellbuehler.acolyte.api.model.SettingsServerBackup
import ch.orellbuehler.acolyte.api.model.SettingsServerBackups
import ch.orellbuehler.acolyte.api.model.SettingsServerPreset
import ch.orellbuehler.acolyte.api.transferPath
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthGate
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthState
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SettingsServerBackupPreset(
    val wire: String,
) {
    BALANCED("balanced"),
    LIGHT("light"),
    CAREFUL("careful"),
    OFF("off"),
    ;

    internal companion object {
        fun of(wire: String): SettingsServerBackupPreset = entries.firstOrNull { it.wire == wire } ?: BALANCED
    }
}

enum class SettingsServerRestoreStage {
    IDLE,
    UPLOADING,
    RESTARTING,
}

enum class SettingsServerBackupsNoticeKind {
    MADE,
    MAKE_FAILED,
    DELETED,
    DELETE_FAILED,
    PLAN_SAVED,
    PLAN_FAILED,
    DOWNLOAD_FAILED,
    KIT_READY,
    KIT_FAILED,
    RESTORE_STAGED,
    RESTORE_FAILED,
    RESTARTED,
    RESTART_SLOW,
}

data class SettingsServerBackupsNotice(
    val seq: Int,
    val kind: SettingsServerBackupsNoticeKind,
    val name: String = "",
    val error: ApiError? = null,
)

data class SettingsServerBackupRow(
    val name: String,
    val kind: String,
    val createdAt: String,
    val size: Long,
    val detail: String,
    val busy: Boolean,
)

data class SettingsServerKeyKitInfo(
    val needed: Boolean,
    val fingerprint: String,
    val warn: Boolean,
)

data class SettingsServerBackupsState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val off: Boolean = false,
    val backups: List<SettingsServerBackupRow> = emptyList(),
    val preset: SettingsServerBackupPreset = SettingsServerBackupPreset.BALANCED,
    val presetSaving: Boolean = false,
    val restoreWaiting: Boolean = false,
    val keyNote: String = "",
    val keyKit: SettingsServerKeyKitInfo? = null,
    val canChange: Boolean = false,
    val isOwner: Boolean = false,
    val keyKitNeedsSession: Boolean = false,
    val making: Boolean = false,
    val keyKitBusy: Boolean = false,
    val restore: SettingsServerRestoreStage = SettingsServerRestoreStage.IDLE,
    val restoreName: String = "",
    val file: SettingsServerFile? = null,
    val notice: SettingsServerBackupsNotice? = null,
    val reauth: SettingsReauthState = SettingsReauthState(),
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && !off && backups.isEmpty()

    val canDownloadKit: Boolean get() = isOwner && !keyKitNeedsSession && keyKit?.fingerprint.orEmpty().isNotEmpty()

    val showKeyNote: Boolean get() = keyNote.isNotEmpty() && keyKit?.fingerprint.orEmpty().isEmpty()

    val restoring: Boolean get() = restore != SettingsServerRestoreStage.IDLE
}

class SettingsServerBackupsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    reauth: suspend (String, String?) -> ApiError?,
) {
    private val mutableState = MutableStateFlow(SettingsServerBackupsState())
    private val lock = Mutex()
    private var raw: List<SettingsServerBackup> = emptyList()
    private var busy: Set<String> = emptySet()
    private var seq = 0
    private var following = false
    private var restorePath = ""
    private val gate = SettingsReauthGate(reauth) { r -> mutableState.update { it.copy(reauth = r) } }

    val state: StateFlow<SettingsServerBackupsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.can(Need.SETTINGS)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    allowed = true,
                    canChange = true,
                    isOwner = me.can(Need.OWNER),
                    keyKitNeedsSession = me.method == SignInMethod.API_KEY,
                )
            }
            gate.guard(onFailure = { error -> mutableState.update { it.copy(status = it.status.failed(error)) } }) { fetch() }
        }
    }

    private suspend fun fetch(): ApiError? =
        when (val result = api.result(SettingsServerEndpoints.backups)) {
            is ApiResult.Ok -> {
                apply(result.value)
                null
            }
            is ApiResult.Failed ->
                when (result.error.code) {
                    ApiErrorCode.NOT_FOUND -> {
                        mutableState.update { it.copy(status = LoadStatus.ready, off = true) }
                        null
                    }
                    ApiErrorCode.REAUTH -> {
                        mutableState.update { it.copy(status = it.status.failed(result.error)) }
                        result.error
                    }
                    else -> result.error
                }
        }

    private fun apply(answer: SettingsServerBackups) {
        raw = answer.backups
        val kit = answer.keyKit
        mutableState.update {
            it.copy(
                status = LoadStatus.ready,
                off = false,
                preset = SettingsServerBackupPreset.of(answer.preset),
                restoreWaiting = answer.restoreWaiting,
                keyNote = answer.keyNote,
                keyKit = kit?.let { k -> SettingsServerKeyKitInfo(k.needed, k.fingerprint, k.level == "warn") },
            )
        }
        rebuild()
    }

    private fun rebuild() {
        val rows = raw.map { SettingsServerBackupRow(it.name, it.kind, it.createdAt, it.size, it.detail, it.name in busy) }
        mutableState.update { it.copy(backups = rows) }
    }

    private fun notice(
        kind: SettingsServerBackupsNoticeKind,
        name: String = "",
        error: ApiError? = null,
    ): SettingsServerBackupsNotice = SettingsServerBackupsNotice(++seq, kind, name, error)

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    suspend fun makeNow() {
        lock.withLock {
            if (mutableState.value.making) return
            mutableState.update { it.copy(making = true) }
            when (val result = api.result(SettingsServerEndpoints.makeBackup)) {
                is ApiResult.Ok -> {
                    raw = listOf(result.value) + raw.filter { it.name != result.value.name }
                    rebuild()
                    mutableState.update { it.copy(making = false, notice = notice(SettingsServerBackupsNoticeKind.MADE)) }
                }
                is ApiResult.Failed ->
                    mutableState.update {
                        it.copy(making = false, notice = notice(SettingsServerBackupsNoticeKind.MAKE_FAILED, error = result.error))
                    }
            }
        }
    }

    suspend fun setPreset(next: SettingsServerBackupPreset) {
        lock.withLock {
            val current = mutableState.value
            if (!current.canChange || current.presetSaving || current.preset == next) return
            val before = current.preset
            mutableState.update { it.copy(preset = next, presetSaving = true) }
            when (val result = api.result(SettingsServerEndpoints.savePreset, SettingsServerPreset(next.wire))) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(presetSaving = false, notice = notice(SettingsServerBackupsNoticeKind.PLAN_SAVED, next.wire))
                    }
                is ApiResult.Failed ->
                    mutableState.update {
                        it.copy(
                            preset = before,
                            presetSaving = false,
                            notice = notice(SettingsServerBackupsNoticeKind.PLAN_FAILED, error = result.error),
                        )
                    }
            }
        }
    }

    suspend fun download(name: String) {
        lock.withLock {
            if (!mutableState.value.isOwner || name in busy) return
            clearTransfers()
            mutableState.update { it.copy(file = null) }
            busy = busy + name
            rebuild()
            gate.guard(onFailure = { error -> downloadFailed(name, error) }) {
                when (val result = api.download(SettingsServerEndpoints.downloadBackup, name, "name" to name)) {
                    is ApiResult.Ok -> {
                        busy = busy - name
                        rebuild()
                        val file = result.value
                        mutableState.update {
                            it.copy(file = SettingsServerFile(++seq, SettingsServerFileKind.BACKUP, file.path, file.name, file.bytes))
                        }
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    private fun downloadFailed(
        name: String,
        error: ApiError,
    ) {
        busy = busy - name
        rebuild()
        mutableState.update { it.copy(notice = notice(SettingsServerBackupsNoticeKind.DOWNLOAD_FAILED, error = error)) }
    }

    suspend fun delete(name: String) {
        lock.withLock {
            if (!mutableState.value.isOwner || name in busy) return
            busy = busy + name
            rebuild()
            gate.guard(onFailure = { error -> deleteFailed(name, error) }) {
                when (val result = api.result(SettingsServerEndpoints.deleteBackup, "name" to name)) {
                    is ApiResult.Ok -> {
                        busy = busy - name
                        raw = raw.filter { it.name != name }
                        rebuild()
                        mutableState.update { it.copy(notice = notice(SettingsServerBackupsNoticeKind.DELETED)) }
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    private fun deleteFailed(
        name: String,
        error: ApiError,
    ) {
        busy = busy - name
        rebuild()
        mutableState.update { it.copy(notice = notice(SettingsServerBackupsNoticeKind.DELETE_FAILED, error = error)) }
    }

    suspend fun downloadKeyKit() {
        lock.withLock {
            val current = mutableState.value
            if (!current.canDownloadKit || current.keyKitBusy) return
            clearTransfers()
            mutableState.update { it.copy(file = null, keyKitBusy = true) }
            gate.guard(
                onFailure = { error ->
                    mutableState.update {
                        it.copy(
                            keyKitBusy = false,
                            notice = notice(SettingsServerBackupsNoticeKind.KIT_FAILED, error = error),
                        )
                    }
                },
            ) {
                when (val result = api.download(SettingsServerEndpoints.keyKit, "holyarrs-key-kit.key")) {
                    is ApiResult.Ok -> {
                        val file = result.value
                        mutableState.update {
                            it.copy(
                                keyKitBusy = false,
                                keyKit = it.keyKit?.copy(needed = false, warn = false),
                                file = SettingsServerFile(++seq, SettingsServerFileKind.KEY_KIT, file.path, file.name, file.bytes),
                                notice = notice(SettingsServerBackupsNoticeKind.KIT_READY),
                            )
                        }
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    fun stagingPath(name: String): String = transferPath("restore-$name")

    suspend fun restore(
        path: String,
        name: String,
    ) {
        lock.withLock {
            if (!mutableState.value.isOwner || mutableState.value.restoring) return
            restorePath = path
            mutableState.update { it.copy(restore = SettingsServerRestoreStage.UPLOADING, restoreName = name) }
            gate.guard(onFailure = { error -> restoreFailed(path, error) }) {
                when (val result = api.upload(SettingsServerEndpoints.restoreBackup, path)) {
                    is ApiResult.Ok -> {
                        deleteTransfer(path)
                        val stage =
                            if (result.value.restarting) SettingsServerRestoreStage.RESTARTING else SettingsServerRestoreStage.IDLE
                        mutableState.update {
                            it.copy(
                                restore = stage,
                                restoreWaiting = true,
                                notice = notice(SettingsServerBackupsNoticeKind.RESTORE_STAGED),
                            )
                        }
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
        followRestart()
    }

    private suspend fun followRestart() {
        if (mutableState.value.restore != SettingsServerRestoreStage.RESTARTING || following) return
        following = true
        try {
            waitForServer()
        } finally {
            following = false
        }
    }

    private fun restoreFailed(
        path: String,
        error: ApiError,
    ) {
        deleteTransfer(path)
        mutableState.update {
            it.copy(
                restore = SettingsServerRestoreStage.IDLE,
                restoreName = "",
                notice = notice(SettingsServerBackupsNoticeKind.RESTORE_FAILED, error = error),
            )
        }
    }

    private suspend fun waitForServer() {
        delay(RESTART_GRACE_MILLIS)
        var tries = 0
        var back = false
        while (tries < RESTART_TRIES && !back) {
            back = applied()
            if (!back) {
                delay(RESTART_POLL_MILLIS)
                tries++
            }
        }
        mutableState.update {
            it.copy(
                restore = SettingsServerRestoreStage.IDLE,
                restoreName = "",
                notice = notice(if (back) SettingsServerBackupsNoticeKind.RESTARTED else SettingsServerBackupsNoticeKind.RESTART_SLOW),
            )
        }
    }

    private suspend fun applied(): Boolean =
        when (val result = api.result(SettingsServerEndpoints.backups)) {
            is ApiResult.Ok -> {
                if (!result.value.restoreWaiting) apply(result.value)
                !result.value.restoreWaiting
            }
            is ApiResult.Failed -> result.error.status in HTTP_ANSWERS
        }

    fun fileDelivered() {
        mutableState.update { it.copy(file = null) }
    }

    fun release() {
        mutableState.update { it.copy(file = null) }
        clearTransfers()
    }

    suspend fun confirmReauth(
        password: String,
        code: String,
    ) {
        lock.withLock { gate.confirm(password, code) }
        followRestart()
    }

    fun cancelReauth() {
        gate.cancel()
        if (mutableState.value.restore == SettingsServerRestoreStage.UPLOADING && restorePath.isNotEmpty()) deleteTransfer(restorePath)
        busy = emptySet()
        rebuild()
        mutableState.update {
            val status = if (it.status.phase == Phase.LOADING) it.status.failed(ApiError(ApiErrorCode.REAUTH, 403)) else it.status
            it.copy(
                status = status,
                keyKitBusy = false,
                restore = SettingsServerRestoreStage.IDLE,
                restoreName = "",
            )
        }
    }

    private companion object {
        const val RESTART_GRACE_MILLIS = 3_000L
        const val RESTART_POLL_MILLIS = 2_000L
        const val RESTART_TRIES = 90
        val HTTP_ANSWERS = 100..499
    }
}
