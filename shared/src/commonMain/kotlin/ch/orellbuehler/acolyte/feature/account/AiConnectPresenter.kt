package ch.orellbuehler.acolyte.feature.account

import ch.orellbuehler.acolyte.api.AccountEndpoints
import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.AccountAiAllow
import ch.orellbuehler.acolyte.api.model.AccountAiInfo
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import io.ktor.http.parseQueryString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AiConnectState(
    val status: LoadStatus = LoadStatus(),
    val valid: Boolean = true,
    val info: AccountAiInfo = AccountAiInfo(),
    val mayAllow: Boolean = false,
    val busy: Boolean = false,
    val redirect: String? = null,
    val error: ApiError? = null,
) {
    val canAllow: Boolean get() = mayAllow && info.enabled && !busy
}

class AiConnectPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
    link: String,
) {
    private val query: String = link.trim().substringAfter('?', "").substringBefore('#')

    private val mutableState = MutableStateFlow(AiConnectState(valid = query.isNotEmpty()))
    private val lock = Mutex()

    val state: StateFlow<AiConnectState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase == Phase.READY || query.isEmpty()) return
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            val params = parseQueryString(query).entries().associate { it.key to it.value.firstOrNull() }
            val pairs = AI_PARAMS.map { it to params[it] }.toTypedArray()
            when (val result = api.result(AccountEndpoints.aiInfo, *pairs)) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            info = result.value,
                            mayAllow = account()?.can(Need.SETTINGS) == true && account()?.usesCookies == true,
                        )
                    }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    suspend fun allow() {
        if (!mutableState.value.canAllow) return
        mutableState.update { it.copy(busy = true, error = null) }
        when (val result = reauth.guard { api.result(AccountEndpoints.aiAllow, AccountAiAllow(query)) }) {
            is ApiResult.Ok -> mutableState.update { it.copy(busy = false, redirect = result.value.redirect) }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }

    private companion object {
        val AI_PARAMS =
            listOf("response_type", "client_id", "redirect_uri", "code_challenge", "code_challenge_method", "state", "resource", "scope")
    }
}
