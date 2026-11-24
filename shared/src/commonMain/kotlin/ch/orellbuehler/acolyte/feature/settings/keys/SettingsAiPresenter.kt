package ch.orellbuehler.acolyte.feature.settings.keys

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.PeopleEndpoints
import ch.orellbuehler.acolyte.api.SettingsKeysAddresses
import ch.orellbuehler.acolyte.api.SettingsKeysEndpoints
import ch.orellbuehler.acolyte.api.model.AccountKey
import ch.orellbuehler.acolyte.api.model.AccountKeyChange
import ch.orellbuehler.acolyte.api.model.AccountKeyMake
import ch.orellbuehler.acolyte.api.model.SettingsKeysAiChange
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthGate
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthState
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsAiState(
    val status: LoadStatus = LoadStatus(),
    val available: Boolean = true,
    val enabled: Boolean = false,
    val isOwner: Boolean = false,
    val canManageKeys: Boolean = false,
    val canChange: Boolean = false,
    val changeNeedsSession: Boolean = false,
    val address: String = "",
    val saving: Boolean = false,
    val keys: List<SettingsKeysAiKey> = emptyList(),
    val keysError: ApiError? = null,
    val fresh: SettingsKeysFresh? = null,
    val making: Boolean = false,
    val revokingId: Long = 0,
    val changing: Boolean = false,
    val changeError: ApiError? = null,
    val changedCount: Int = 0,
    val notice: SettingsKeysNotice? = null,
    val reauth: SettingsReauthState = SettingsReauthState(),
) {
    val keysNeedPassword: Boolean get() = keysError?.code == ApiErrorCode.FORBIDDEN

    val changeDetail: String get() = changeError?.let { SettingsKeysLogic.detail(it) }.orEmpty()

    val offeredGroups: List<String> get() = SettingsKeysLogic.offeredGroups

    val defaultGroups: List<String> get() = SettingsKeysLogic.defaultGroups

    val labelMax: Int get() = SettingsKeysLogic.LABEL_MAX
}

class SettingsAiPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    reauth: suspend (String, String?) -> ApiError?,
) {
    private val mutableState = MutableStateFlow(SettingsAiState())
    private val lock = Mutex()
    private val notices = SettingsKeysNotices()
    private val gate = SettingsReauthGate(reauth) { value -> mutableState.update { it.copy(reauth = value) } }

    val state: StateFlow<SettingsAiState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || me.isHeld(SettingsKeysLogic.AI_FEATURE)) {
                mutableState.update { it.copy(status = LoadStatus.ready, available = false) }
                return
            }
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    available = true,
                    isOwner = me.can(Need.OWNER),
                    canManageKeys = me.can(Need.SETTINGS),
                    canChange = me.can(Need.USERS),
                    changeNeedsSession = me.method == SignInMethod.API_KEY,
                    address = SettingsKeysAddresses.mcp(me.serverUrl),
                )
            }
            coroutineScope {
                val keys = async { if (me.can(Need.SETTINGS)) api.result(PeopleEndpoints.keys) else null }
                when (val result = api.result(SettingsKeysEndpoints.ai)) {
                    is ApiResult.Ok -> mutableState.update { it.copy(status = LoadStatus.ready, enabled = result.value.enabled) }
                    is ApiResult.Failed ->
                        if (result.error.code == ApiErrorCode.NOT_FOUND) {
                            mutableState.update { it.copy(status = LoadStatus.ready, available = false) }
                        } else {
                            mutableState.update { it.copy(status = it.status.failed(result.error)) }
                        }
                }
                when (val result = keys.await()) {
                    is ApiResult.Ok -> mutableState.update { it.copy(keys = aiKeys(result.value.keys), keysError = null) }
                    is ApiResult.Failed -> mutableState.update { it.copy(keysError = result.error) }
                    null -> Unit
                }
            }
        }
    }

    private fun aiKeys(all: List<AccountKey>): List<SettingsKeysAiKey> =
        all
            .filter { SettingsKeysLogic.AI_SCOPE in it.scopes }
            .map { SettingsKeysAiKey(it.id, it.label, it.hint, it.createdAt, it.lastUsedAt, SettingsKeysLogic.knownGroups(it.mcpGroups)) }

    private suspend fun reloadKeys() {
        val result = api.result(PeopleEndpoints.keys)
        if (result is ApiResult.Ok) mutableState.update { it.copy(keys = aiKeys(result.value.keys), keysError = null) }
    }

    suspend fun setEnabled(on: Boolean) {
        val before = mutableState.value
        if (!before.isOwner || before.saving || before.enabled == on) return
        mutableState.update { it.copy(enabled = on, saving = true) }
        lock.withLock {
            when (val result = api.result(SettingsKeysEndpoints.changeAi, SettingsKeysAiChange(on))) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            enabled = result.value.enabled,
                            saving = false,
                            notice =
                                notices.make(
                                    if (result.value.enabled) SettingsKeysNoticeKind.AI_ON else SettingsKeysNoticeKind.AI_OFF,
                                ),
                        )
                    }
                is ApiResult.Failed ->
                    mutableState.update {
                        it.copy(
                            enabled = before.enabled,
                            saving = false,
                            notice = notices.make(SettingsKeysNoticeKind.AI_SAVE_FAILED, error = result.error),
                        )
                    }
            }
        }
    }

    suspend fun makeKey(
        label: String,
        groups: List<String>,
    ) {
        val name = SettingsKeysLogic.cleanLabel(label) ?: return
        if (mutableState.value.making || !mutableState.value.canManageKeys) return
        val body = AccountKeyMake(name, listOf(SettingsKeysLogic.AI_SCOPE), SettingsKeysLogic.orderedGroups(groups))
        mutableState.update { it.copy(making = true) }
        gate.guard(onFailure = { error -> fail(SettingsKeysNoticeKind.KEY_MAKE_FAILED, error = error) }) {
            lock.withLock {
                when (val result = api.result(PeopleEndpoints.makeKey, body)) {
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

    suspend fun changeGroups(
        id: Long,
        groups: List<String>,
    ) {
        val key = mutableState.value.keys.firstOrNull { it.id == id } ?: return
        if (mutableState.value.changing || !mutableState.value.canChange) return
        val body = AccountKeyChange(SettingsKeysLogic.orderedGroups(groups, keepPlugins = SettingsKeysLogic.PLUGINS in key.groups))
        mutableState.update { it.copy(changing = true, changeError = null) }
        gate.guard(onFailure = { error -> mutableState.update { it.copy(changing = false, changeError = error) } }) {
            lock.withLock {
                when (val result = api.result(PeopleEndpoints.changeKey, body, "id" to id)) {
                    is ApiResult.Ok -> {
                        val saved = SettingsKeysLogic.knownGroups(result.value.mcpGroups)
                        mutableState.update {
                            it.copy(
                                keys = it.keys.map { row -> if (row.id == id) row.copy(groups = saved) else row },
                                changing = false,
                                changeError = null,
                                changedCount = it.changedCount + 1,
                                notice = notices.make(SettingsKeysNoticeKind.GROUPS_SAVED, key.label),
                            )
                        }
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    fun clearChangeError() = mutableState.update { it.copy(changeError = null) }

    suspend fun revokeKey(id: Long) {
        val key = mutableState.value.keys.firstOrNull { it.id == id } ?: return
        if (mutableState.value.revokingId != 0L) return
        mutableState.update { it.copy(revokingId = id) }
        gate.guard(onFailure = { error -> fail(SettingsKeysNoticeKind.KEY_REVOKE_FAILED, key.label, error) }) {
            lock.withLock {
                when (val result = api.result(PeopleEndpoints.revokeKey, "id" to id)) {
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

    suspend fun confirmReauth(
        password: String,
        code: String,
    ) = gate.confirm(password, code)

    fun cancelReauth() {
        gate.cancel()
        mutableState.update { it.copy(making = false, revokingId = 0, changing = false) }
    }

    fun dismissNotice(seq: Int) = mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }

    private fun fail(
        kind: SettingsKeysNoticeKind,
        name: String = "",
        error: ApiError,
    ) = mutableState.update {
        it.copy(making = false, revokingId = 0, notice = notices.make(kind, name, error))
    }
}
