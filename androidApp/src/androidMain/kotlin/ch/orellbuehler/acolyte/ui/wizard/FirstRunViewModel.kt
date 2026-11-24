package ch.orellbuehler.acolyte.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.account.AccountSecurityState
import ch.orellbuehler.acolyte.feature.account.AccountWizardState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FirstRunViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val wizard = sdk.accountWizard()
    private val security = sdk.accountSecurity()
    private var plexJob: Job? = null

    val state: StateFlow<AccountWizardState> = wizard.state
    val securityState: StateFlow<AccountSecurityState> = security.state
    val wizardReauth = wizard.reauth
    val securityReauth = security.reauth

    init {
        viewModelScope.launch { security.load() }
    }

    fun discover() {
        viewModelScope.launch { wizard.discover() }
    }

    fun linkServer(
        kind: String,
        address: String,
        name: String,
        password: String,
        onLinked: () -> Unit,
    ) {
        viewModelScope.launch {
            wizard.linkServer(kind, address, name, password)
            if (wizard.state.value.error == null &&
                wizard.state.value.linked
                    .any { it.provider == kind }
            ) {
                security.refresh()
                onLinked()
            }
        }
    }

    fun linkPlex() {
        if (plexJob?.isActive == true) return
        plexJob = viewModelScope.launch { security.linkPlex() }
    }

    fun cancelPlex() {
        plexJob?.cancel()
        plexJob = null
    }

    fun unlinkPlex() {
        viewModelScope.launch { security.unlinkPlex() }
    }

    fun unlinkMedia(kind: String) {
        viewModelScope.launch { security.unlinkMedia(kind) }
    }

    fun next() = wizard.next()

    fun back() = wizard.back()

    fun finish() {
        viewModelScope.launch { wizard.finish(skipped = true) }
    }

    override fun onCleared() {
        plexJob?.cancel()
    }
}
