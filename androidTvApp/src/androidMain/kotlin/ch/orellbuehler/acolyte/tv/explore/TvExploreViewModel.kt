package ch.orellbuehler.acolyte.tv.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.feature.discover.ExploreState
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone

fun tvRegion(): String = Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"

class TvExploreViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.explore(tvRegion(), TimeZone.getDefault().id)

    val state: StateFlow<ExploreState> = presenter.state

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

    fun selectService(id: Long) {
        viewModelScope.launch { presenter.selectService(id) }
    }

    fun stripShown() {
        viewModelScope.launch { presenter.stripShown() }
    }

    fun stripOpened(tile: BrowseTile) {
        viewModelScope.launch { presenter.stripOpened(tile) }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()

    suspend fun watch() = presenter.watch()
}
