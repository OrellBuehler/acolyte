package ch.orellbuehler.acolyte.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.health.HealthState
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

    fun runChecks() {
        viewModelScope.launch { presenter.runChecks() }
    }

    suspend fun watch() = presenter.watch()
}
