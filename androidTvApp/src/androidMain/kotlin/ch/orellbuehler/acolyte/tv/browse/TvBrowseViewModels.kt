package ch.orellbuehler.acolyte.tv.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowseViewState
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.FilmRole
import ch.orellbuehler.acolyte.feature.discover.FilmSort
import ch.orellbuehler.acolyte.feature.discover.GenreState
import ch.orellbuehler.acolyte.feature.discover.PersonState
import ch.orellbuehler.acolyte.feature.discover.SeriesState
import ch.orellbuehler.acolyte.tv.explore.tvRegion
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TvGenreViewModel(
    sdk: AcolyteSdk,
    type: ExploreType,
    genre: Int,
) : ViewModel() {
    private val presenter = sdk.exploreGenre(type, genre, tvRegion())

    val state: StateFlow<GenreState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun selectService(id: Long) {
        viewModelScope.launch { presenter.selectService(id) }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()

    suspend fun watch() = presenter.watch()
}

class TvBrowseListViewModel(
    sdk: AcolyteSdk,
    link: BrowseLink,
) : ViewModel() {
    private val presenter = sdk.browseView(link)

    val state: StateFlow<BrowseViewState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()
}

class TvPersonViewModel(
    sdk: AcolyteSdk,
    id: String,
) : ViewModel() {
    private val presenter = sdk.person(id)

    val state: StateFlow<PersonState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun select(
        role: FilmRole,
        sort: FilmSort,
    ) {
        viewModelScope.launch { presenter.select(role, sort) }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()
}

class TvCollectionViewModel(
    sdk: AcolyteSdk,
    id: String,
) : ViewModel() {
    private val presenter = sdk.movieSeries(id)

    val state: StateFlow<SeriesState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }
}
