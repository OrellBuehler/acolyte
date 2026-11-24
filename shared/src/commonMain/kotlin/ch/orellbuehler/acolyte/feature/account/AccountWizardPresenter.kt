package ch.orellbuehler.acolyte.feature.account

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.SignInExtraEndpoints
import ch.orellbuehler.acolyte.api.model.AccountFoundServer
import ch.orellbuehler.acolyte.api.model.AccountSetupLink
import ch.orellbuehler.acolyte.api.model.AccountSetupLinked
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AccountWizardStep {
    SERVERS,
    DONE,
}

data class AccountWizardState(
    val step: AccountWizardStep = AccountWizardStep.SERVERS,
    val found: List<AccountFoundServer> = emptyList(),
    val discovering: Boolean = false,
    val linked: List<AccountSetupLinked> = emptyList(),
    val busy: Boolean = false,
    val finished: Boolean = false,
    val error: ApiError? = null,
)

class AccountWizardPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
    private val refreshAccount: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(AccountWizardState())

    val state: StateFlow<AccountWizardState> = mutableState.asStateFlow()

    val mustFinish: Boolean get() = account()?.setupOpen == true

    suspend fun discover() {
        if (mutableState.value.discovering) return
        mutableState.update { it.copy(discovering = true) }
        val servers = (api.result(SignInExtraEndpoints.setupDiscover) as? ApiResult.Ok)?.value?.servers.orEmpty()
        mutableState.update { it.copy(discovering = false, found = servers) }
    }

    suspend fun linkServer(
        kind: String,
        address: String,
        name: String,
        password: String,
    ) {
        if (mutableState.value.busy) return
        val url = address.trim().let { if (it.contains("://")) it else "http://$it" }
        mutableState.update { it.copy(busy = true, error = null) }
        when (val result = api.result(SignInExtraEndpoints.setupLink, AccountSetupLink(url, name.trim(), password), "kind" to kind)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        busy = false,
                        linked =
                            it.linked.filter { l -> l.provider != kind } + result.value,
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }

    fun next() {
        mutableState.update { it.copy(step = AccountWizardStep.DONE, error = null) }
    }

    fun back() {
        mutableState.update { it.copy(step = AccountWizardStep.SERVERS, error = null) }
    }

    suspend fun finish(skipped: Boolean) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        val result =
            reauth.guard {
                if (skipped) api.result(SignInExtraEndpoints.setupDone, "skipped" to "1") else api.result(SignInExtraEndpoints.setupDone)
            }
        when (result) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(busy = false, finished = true) }
                refreshAccount()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }
}
