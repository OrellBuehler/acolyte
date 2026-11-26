package ch.orellbuehler.acolyte.tv.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.home.HomeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone

class TvHomeViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.home(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000)

    val state: StateFlow<HomeState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    suspend fun watch() = presenter.watch()
}
