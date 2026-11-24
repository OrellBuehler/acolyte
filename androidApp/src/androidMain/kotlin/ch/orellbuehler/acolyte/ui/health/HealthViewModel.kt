package ch.orellbuehler.acolyte.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.health.HealthState
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.health()

    val state: StateFlow<HealthState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun checkAll() {
        viewModelScope.launch { presenter.checkAll() }
    }

    fun check(key: String) {
        viewModelScope.launch { presenter.check(key) }
    }

    fun checkUpdates() {
        viewModelScope.launch { presenter.checkUpdates() }
    }

    fun setCheckOn(on: Boolean) {
        viewModelScope.launch { presenter.setCheckOn(on) }
    }

    fun setMode(mode: SettingsStatusMode) {
        viewModelScope.launch { presenter.setMode(mode) }
    }

    fun installNow() {
        viewModelScope.launch { presenter.installNow() }
    }

    fun goBack() {
        viewModelScope.launch { presenter.goBack() }
    }

    fun downloadBundle() {
        viewModelScope.launch { presenter.downloadBundle() }
    }

    fun bundleHandled() = presenter.bundleHandled()

    fun dismissNotice() = presenter.dismissNotice()

    suspend fun watch() = presenter.watch()
}
