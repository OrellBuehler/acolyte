package ch.orellbuehler.acolyte.feature.account

import ch.orellbuehler.acolyte.api.AccountEndpoints
import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.model.AccountCode
import ch.orellbuehler.acolyte.api.model.AccountEmpty
import ch.orellbuehler.acolyte.api.model.AccountMediaLinkBody
import ch.orellbuehler.acolyte.api.model.AccountMediaLinks
import ch.orellbuehler.acolyte.api.model.AccountPasswordChange
import ch.orellbuehler.acolyte.api.model.AccountPlexFinish
import ch.orellbuehler.acolyte.api.model.AccountPlexLink
import ch.orellbuehler.acolyte.api.model.AccountSsoLink
import ch.orellbuehler.acolyte.api.model.AccountTwoFactorSetup
import ch.orellbuehler.acolyte.api.model.PeopleSsoUnlink
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountRules
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class AccountSecurityNotice {
    PASSWORD_CHANGED,
    PASSWORD_SET,
    TWO_FACTOR_OFF,
    CODES_RENEWED,
    PLEX_LINKED,
    PLEX_UNLINKED,
    MEDIA_LINKED,
    MEDIA_UNLINKED,
    SSO_UNLINKED,
}

enum class AccountSecurityProblem {
    PASSWORD_SHORT,
    PASSWORDS_DIFFER,
    CODE_INVALID,
    PLEX_TIMEOUT,
}

data class AccountSecurityState(
    val status: LoadStatus = LoadStatus(),
    val setup: AccountTwoFactorSetup? = null,
    val backupCodes: List<String> = emptyList(),
    val plex: AccountPlexLink = AccountPlexLink(),
    val plexUrl: String? = null,
    val media: AccountMediaLinks = AccountMediaLinks(),
    val sso: List<AccountSsoLink> = emptyList(),
    val busy: Boolean = false,
    val notice: AccountSecurityNotice? = null,
    val noticeText: String = "",
    val problem: AccountSecurityProblem? = null,
    val error: ApiError? = null,
    val minPasswordLength: Int = 8,
) {
    val plexWaiting: Boolean get() = plexUrl != null
}

class AccountSecurityPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
    private val refreshAccount: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(AccountSecurityState())
    private val lock = Mutex()

    val state: StateFlow<AccountSecurityState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            var minimum: Int? = null
            val (plex, media, sso) =
                coroutineScope {
                    val p = async { api.result(AccountEndpoints.plexLink) }
                    val m = async { api.result(AccountEndpoints.mediaLinks) }
                    val s = async { api.result(AccountEndpoints.ssoLinks) }
                    val n = async { api.result(AuthEndpoints.state).valueOrNull()?.minPasswordLength }
                    minimum = n.await()
                    Triple(p.await(), m.await(), s.await())
                }
            val failed = listOf(plex, media, sso).filterIsInstance<ApiResult.Failed>().firstOrNull()
            if (failed != null && plex is ApiResult.Failed && media is ApiResult.Failed && sso is ApiResult.Failed) {
                mutableState.update { it.copy(status = it.status.failed(failed.error)) }
                return
            }
            mutableState.update {
                it.copy(
                    status = LoadStatus.ready,
                    plex = plex.valueOrNull() ?: it.plex,
                    media = media.valueOrNull() ?: it.media,
                    sso = sso.valueOrNull()?.links ?: it.sso,
                    minPasswordLength = minimum ?: it.minPasswordLength,
                )
            }
        }
    }

    fun consume() {
        mutableState.update { it.copy(notice = null, noticeText = "", problem = null, error = null) }
    }

    fun dismissBackupCodes() {
        mutableState.update { it.copy(backupCodes = emptyList()) }
    }

    suspend fun changePassword(
        current: String,
        new: String,
        confirm: String,
    ) {
        val hasPassword = account()?.hasPassword != false
        when {
            !AccountRules.validPassword(new, mutableState.value.minPasswordLength) -> return problem(AccountSecurityProblem.PASSWORD_SHORT)
            new != confirm -> return problem(AccountSecurityProblem.PASSWORDS_DIFFER)
        }
        val body = AccountPasswordChange(if (hasPassword) current else null, new)
        act(if (hasPassword) AccountSecurityNotice.PASSWORD_CHANGED else AccountSecurityNotice.PASSWORD_SET) {
            reauth.guard { api.result(AccountEndpoints.password, body) }
        }
        refreshAccount()
    }

    suspend fun startTwoFactor() {
        if (!begin()) return
        when (val result = reauth.guard { api.result(AccountEndpoints.twoFactorSetup) }) {
            is ApiResult.Ok -> mutableState.update { it.copy(busy = false, setup = result.value) }
            is ApiResult.Failed -> fail(result.error)
        }
    }

    fun cancelTwoFactor() {
        mutableState.update { it.copy(setup = null, problem = null, error = null) }
    }

    suspend fun enableTwoFactor(code: String) {
        val cleaned = code.filter { it.isDigit() }
        if (cleaned.length != 6) return problem(AccountSecurityProblem.CODE_INVALID)
        if (!begin()) return
        when (val result = reauth.guard { api.result(AccountEndpoints.twoFactorEnable, AccountCode(cleaned)) }) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(busy = false, setup = null, backupCodes = result.value.backupCodes) }
                refreshAccount()
            }
            is ApiResult.Failed -> fail(result.error)
        }
    }

    suspend fun disableTwoFactor(code: String) {
        act(
            AccountSecurityNotice.TWO_FACTOR_OFF,
        ) { reauth.guard { api.result(AccountEndpoints.twoFactorDisable, AccountCode(code.trim())) } }
        refreshAccount()
    }

    suspend fun renewBackupCodes(code: String) {
        if (!begin()) return
        when (val result = reauth.guard { api.result(AccountEndpoints.backupCodes, AccountCode(code.trim())) }) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(busy = false, backupCodes = result.value.backupCodes, notice = AccountSecurityNotice.CODES_RENEWED)
                }
            is ApiResult.Failed -> fail(result.error)
        }
    }

    suspend fun linkPlex() {
        if (!begin()) return
        val started =
            when (val result = reauth.guard { api.result(AccountEndpoints.plexLinkStart, AccountEmpty()) }) {
                is ApiResult.Ok -> result.value
                is ApiResult.Failed -> return fail(result.error)
            }
        mutableState.update { it.copy(plexUrl = started.authUrl) }
        try {
            repeat(PLEX_POLLS) {
                delay(PLEX_POLL_MILLIS)
                when (val result = reauth.guard { api.result(AccountEndpoints.plexLinkFinish, AccountPlexFinish(started.id)) }) {
                    is ApiResult.Ok ->
                        if (!result.value.waiting) {
                            mutableState.update {
                                it.copy(
                                    busy = false,
                                    plex = result.value,
                                    notice = AccountSecurityNotice.PLEX_LINKED,
                                    noticeText = result.value.name,
                                )
                            }
                            return
                        }
                    is ApiResult.Failed ->
                        if (result.error.code != ApiErrorCode.RATE_LIMITED) return fail(result.error)
                }
            }
            mutableState.update { it.copy(busy = false, problem = AccountSecurityProblem.PLEX_TIMEOUT) }
        } finally {
            mutableState.update { it.copy(plexUrl = null, busy = false) }
        }
    }

    suspend fun unlinkPlex() {
        act(AccountSecurityNotice.PLEX_UNLINKED) { reauth.guard { api.result(AccountEndpoints.plexUnlink) } }
        refresh()
    }

    suspend fun linkMedia(
        serverId: Long,
        name: String,
        password: String,
    ) {
        val body = AccountMediaLinkBody(name.trim(), password)
        act(AccountSecurityNotice.MEDIA_LINKED) { reauth.guard { api.result(AccountEndpoints.mediaLink, body, "server" to serverId) } }
        refresh()
    }

    suspend fun unlinkMedia(kind: String) {
        act(AccountSecurityNotice.MEDIA_UNLINKED) { reauth.guard { api.result(AccountEndpoints.mediaUnlink, "kind" to kind) } }
        refresh()
    }

    suspend fun unlinkSso(link: AccountSsoLink) {
        val body = PeopleSsoUnlink(link.kind, link.issuer)
        act(AccountSecurityNotice.SSO_UNLINKED) { reauth.guard { api.result(AccountEndpoints.ssoUnlink, body) } }
        refresh()
    }

    private fun problem(problem: AccountSecurityProblem) {
        mutableState.update { it.copy(problem = problem) }
    }

    private fun begin(): Boolean {
        if (mutableState.value.busy) return false
        mutableState.update { it.copy(busy = true, error = null, problem = null, notice = null) }
        return true
    }

    private fun fail(error: ApiError) {
        mutableState.update { it.copy(busy = false, error = error) }
    }

    private suspend fun <T> act(
        notice: AccountSecurityNotice,
        block: suspend () -> ApiResult<T>,
    ) {
        if (!begin()) return
        when (val result = block()) {
            is ApiResult.Ok -> mutableState.update { it.copy(busy = false, notice = notice) }
            is ApiResult.Failed -> fail(result.error)
        }
    }

    private companion object {
        const val PLEX_POLL_MILLIS = 2_000L
        const val PLEX_POLLS = 300
    }
}
