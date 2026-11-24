package ch.orellbuehler.acolyte.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.settings.SettingsHomeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsHomeViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsHome()

    val state: StateFlow<SettingsHomeState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    suspend fun watch() = presenter.watch()
}
