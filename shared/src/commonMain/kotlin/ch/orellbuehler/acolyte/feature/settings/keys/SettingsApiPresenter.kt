package ch.orellbuehler.acolyte.feature.settings.keys

import ch.orellbuehler.acolyte.api.AccountKeysEndpoints
import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsKeysAddresses
import ch.orellbuehler.acolyte.api.SettingsKeysEndpoints
import ch.orellbuehler.acolyte.api.model.ApiKeyInfo
import ch.orellbuehler.acolyte.api.model.NewApiKey
import ch.orellbuehler.acolyte.api.model.SettingsKeysOldApp
import ch.orellbuehler.acolyte.api.model.SettingsKeysOldAppChange
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthGate
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthState
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsApiState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val baseAddress: String = "",
    val openApiAddress: String = "",
    val keys: List<ApiKeyInfo> = emptyList(),
    val keysError: ApiError? = null,
    val fresh: SettingsKeysFresh? = null,
    val making: Boolean = false,
    val revokingId: Long = 0,
    val seerr: SettingsSeerrCard? = null,
    val seerrError: ApiError? = null,
    val seerrGone: Boolean = false,
    val seerrSaving: Boolean = false,
    val seerrFresh: SettingsSeerrFresh? = null,
    val notice: SettingsKeysNotice? = null,
    val reauth: SettingsReauthState = SettingsReauthState(),
) {
    val keysNeedPassword: Boolean get() = keysError?.code == ApiErrorCode.FORBIDDEN

    val labelMax: Int get() = SettingsKeysLogic.LABEL_MAX
}

class SettingsApiPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    reauth: suspend (String, String?) -> ApiError?,
) {
    private val mutableState = MutableStateFlow(SettingsApiState())
    private val lock = Mutex()
    private val notices = SettingsKeysNotices()
    private val gate = SettingsReauthGate(reauth) { value -> mutableState.update { it.copy(reauth = value) } }
    private var answer: SettingsKeysOldApp? = null
    private var busySide = ""

    val state: StateFlow<SettingsApiState> = mutableState.asStateFlow()

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
                    baseAddress = SettingsKeysAddresses.api(me.serverUrl),
                    openApiAddress = SettingsKeysAddresses.openApi(me.serverUrl),
                )
            }
            coroutineScope {
                val oldApp = async { api.result(SettingsKeysEndpoints.oldApp) }
                val keys = api.result(AccountKeysEndpoints.keys)
                val seerr = oldApp.await()
                val keysHard = (keys as? ApiResult.Failed)?.error?.takeIf { it.code != ApiErrorCode.FORBIDDEN }
                val seerrHard = (seerr as? ApiResult.Failed)?.error?.takeIf { it.code != ApiErrorCode.NOT_FOUND }
                when (keys) {
                    is ApiResult.Ok -> mutableState.update { it.copy(keys = keys.value.keys, keysError = null) }
                    is ApiResult.Failed -> mutableState.update { it.copy(keysError = keys.error) }
                }
                applySeerr(seerr)
                mutableState.update {
                    it.copy(status = if (keysHard != null && seerrHard != null) it.status.failed(keysHard) else LoadStatus.ready)
                }
            }
        }
    }

    suspend fun reloadSeerr() {
        lock.withLock {
            applySeerr(api.result(SettingsKeysEndpoints.oldApp))
        }
    }

    private fun applySeerr(result: ApiResult<SettingsKeysOldApp>) {
        when (result) {
            is ApiResult.Ok -> {
                answer = result.value
                mutableState.update { it.copy(seerr = card(), seerrError = null, seerrGone = false) }
            }
            is ApiResult.Failed ->
                if (result.error.code == ApiErrorCode.NOT_FOUND) {
                    answer = null
                    mutableState.update { it.copy(seerr = null, seerrError = null, seerrGone = true) }
                } else {
                    mutableState.update { it.copy(seerrError = result.error) }
                }
        }
    }

    private fun card(): SettingsSeerrCard? =
        answer?.let { value ->
            SettingsSeerrCard(
                enabled = value.enabled,
                deleteFiles = value.deleteFiles,
                sides =
                    value.sides.map { side ->
                        val key = side.key
                        SettingsSeerrSide(
                            side = side.side,
                            path = side.path,
                            hasKey = key != null,
                            hint = SettingsKeysLogic.keyHint(key?.hint.orEmpty()),
                            owner = key?.owner.orEmpty(),
                            ownerActive = key?.ownerActive ?: true,
                            lastUsedAt = key?.lastUsedAt,
                            busy = side.side == busySide,
                        )
                    },
                sent = SettingsKeysLogic.pushedRows(value.pushed),
            )
        }

    private suspend fun reloadKeys() {
        val result = api.result(AccountKeysEndpoints.keys)
        if (result is ApiResult.Ok) mutableState.update { it.copy(keys = result.value.keys, keysError = null) }
    }

    suspend fun makeKey(label: String) {
        val name = SettingsKeysLogic.cleanLabel(label) ?: return
        if (mutableState.value.making) return
        mutableState.update { it.copy(making = true) }
        gate.guard(onFailure = { error -> fail(SettingsKeysNoticeKind.KEY_MAKE_FAILED, error = error) }) {
            lock.withLock {
                when (val result = api.result(AccountKeysEndpoints.makeKey, NewApiKey(name))) {
                    is ApiResult.Ok -> {
                        val made = result.value
                        mutableState.update {
                            it.copy(making = false, fresh = SettingsKeysFresh(made.id, made.label.ifEmpty { name }, made.key))
                        }
                        reloadKeys()
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    suspend fun revokeKey(id: Long) {
        val key = mutableState.value.keys.firstOrNull { it.id == id } ?: return
        if (mutableState.value.revokingId != 0L) return
        mutableState.update { it.copy(revokingId = id) }
        gate.guard(onFailure = { error -> fail(SettingsKeysNoticeKind.KEY_REVOKE_FAILED, key.label, error) }) {
            lock.withLock {
                when (val result = api.result(AccountKeysEndpoints.revokeKey, "id" to id)) {
                    is ApiResult.Ok -> {
                        mutableState.update {
                            it.copy(
                                keys = it.keys.filter { row -> row.id != id },
                                fresh = it.fresh?.takeIf { fresh -> fresh.id != id },
                                revokingId = 0,
                                notice = notices.make(SettingsKeysNoticeKind.KEY_REVOKED, key.label),
                            )
                        }
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    fun dismissFresh() = mutableState.update { it.copy(fresh = null) }

    suspend fun setSeerrEnabled(on: Boolean) = changeSeerr(SettingsKeysOldAppChange(enabled = on)) { it.copy(enabled = on) }

    suspend fun setSeerrDeleteFiles(on: Boolean) = changeSeerr(SettingsKeysOldAppChange(deleteFiles = on)) { it.copy(deleteFiles = on) }

    private suspend fun changeSeerr(
        body: SettingsKeysOldAppChange,
        optimistic: (SettingsKeysOldApp) -> SettingsKeysOldApp,
    ) {
        val before = answer ?: return
        if (mutableState.value.seerrSaving) return
        answer = optimistic(before)
        mutableState.update { it.copy(seerrSaving = true, seerr = card()) }
        lock.withLock {
            when (val result = api.result(SettingsKeysEndpoints.changeOldApp, body)) {
                is ApiResult.Ok -> {
                    answer = result.value
                    mutableState.update { it.copy(seerrSaving = false, seerr = card()) }
                }
                is ApiResult.Failed -> {
                    answer = before
                    mutableState.update {
                        it.copy(
                            seerrSaving = false,
                            seerr = card(),
                            notice = notices.make(SettingsKeysNoticeKind.SEERR_SAVE_FAILED, error = result.error),
                        )
                    }
                }
            }
        }
    }

    suspend fun makeSeerrKey(side: String) {
        if (busySide.isNotEmpty() || answer?.sides?.any { it.side == side } != true) return
        busy(side)
        gate.guard(onFailure = { error -> failSeerr(SettingsKeysNoticeKind.SEERR_KEY_MAKE_FAILED, side, error) }) {
            lock.withLock {
                when (val result = api.result(SettingsKeysEndpoints.makeArrKey, "side" to side)) {
                    is ApiResult.Ok -> {
                        busy("")
                        mutableState.update { it.copy(seerrFresh = SettingsSeerrFresh(side, result.value.key)) }
                        applySeerr(api.result(SettingsKeysEndpoints.oldApp))
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    suspend fun removeSeerrKey(side: String) {
        if (busySide.isNotEmpty() || answer?.sides?.any { it.side == side } != true) return
        busy(side)
        gate.guard(onFailure = { error -> failSeerr(SettingsKeysNoticeKind.SEERR_KEY_REMOVE_FAILED, side, error) }) {
            lock.withLock {
                when (val result = api.result(SettingsKeysEndpoints.removeArrKey, "side" to side)) {
                    is ApiResult.Ok -> {
                        busy("")
                        mutableState.update {
                            it.copy(
                                seerrFresh = it.seerrFresh?.takeIf { fresh -> fresh.side != side },
                                notice = notices.make(SettingsKeysNoticeKind.SEERR_KEY_REMOVED, side),
                            )
                        }
                        applySeerr(api.result(SettingsKeysEndpoints.oldApp))
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    fun dismissSeerrFresh() = mutableState.update { it.copy(seerrFresh = null) }

    suspend fun confirmReauth(
        password: String,
        code: String,
    ) = gate.confirm(password, code)

    fun cancelReauth() {
        gate.cancel()
        busySide = ""
        mutableState.update { it.copy(making = false, revokingId = 0, seerr = card()) }
    }

    fun dismissNotice(seq: Int) = mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }

    private fun busy(side: String) {
        busySide = side
        mutableState.update { it.copy(seerr = card()) }
    }

    private fun fail(
        kind: SettingsKeysNoticeKind,
        name: String = "",
        error: ApiError,
    ) = mutableState.update {
        it.copy(making = false, revokingId = 0, notice = notices.make(kind, name, error))
    }

    private fun failSeerr(
        kind: SettingsKeysNoticeKind,
        side: String,
        error: ApiError,
    ) {
        busySide = ""
        mutableState.update { it.copy(seerr = card(), notice = notices.make(kind, side, error)) }
    }
}
