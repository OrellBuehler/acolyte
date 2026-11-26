package ch.orellbuehler.acolyte.tv.popular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.PopularState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone

class TvPopularViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.popular(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000)

    val state: StateFlow<PopularState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun select(type: ExploreType) {
        viewModelScope.launch { presenter.select(type) }
    }

    fun loadMore() {
        viewModelScope.launch { presenter.loadMore() }
    }

    fun selectService(
        row: String,
        service: Long,
    ) {
        viewModelScope.launch { presenter.selectService(row, service) }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()

    suspend fun watch() = presenter.watch()
}
