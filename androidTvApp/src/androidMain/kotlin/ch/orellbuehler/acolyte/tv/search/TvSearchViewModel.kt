package ch.orellbuehler.acolyte.tv.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.discover.BrowseGridState
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.PeopleBrowseState
import ch.orellbuehler.acolyte.feature.discover.PeopleFilter
import ch.orellbuehler.acolyte.feature.discover.SearchScope
import ch.orellbuehler.acolyte.feature.discover.SearchState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TvSearchViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val search = sdk.search()
    private val browse = sdk.browse()
    private val people = sdk.peopleBrowse()

    val state: StateFlow<SearchState> = search.state
    val browseState: StateFlow<BrowseGridState> = browse.state
    val peopleState: StateFlow<PeopleBrowseState> = people.state

    init {
        search.load()
        loadBrowse()
    }

    fun type(text: String) = search.type(text)

    fun selectScope(scope: SearchScope) {
        search.selectScope(scope)
        loadBrowse()
    }

    fun setOnlyLibrary(on: Boolean) = search.setOnlyLibrary(on)

    fun clearRecents() = search.clearRecents()

    suspend fun suggest(text: String) = search.suggest(text)

    suspend fun search(text: String) = search.search(text)

    fun submit(text: String) {
        search.type(text)
        search.remember(text)
        viewModelScope.launch { search.search(text) }
    }

    fun loadSection(id: String) {
        viewModelScope.launch { browse.loadSection(id) }
    }

    fun refreshBrowse() {
        viewModelScope.launch {
            if (search.state.value.scope == SearchScope.PEOPLE) people.refresh() else browse.refresh()
        }
    }

    fun selectPeople(filter: PeopleFilter) {
        viewModelScope.launch { people.select(filter) }
    }

    fun morePeople() {
        viewModelScope.launch { people.loadMore() }
    }

    suspend fun refreshOwned() = search.refreshOwned()

    suspend fun watch() = browse.watch()

    private fun loadBrowse() {
        viewModelScope.launch {
            when (search.state.value.scope) {
                SearchScope.PEOPLE -> people.load()
                SearchScope.MOVIE -> browse.load(ExploreType.MOVIES)
                SearchScope.SHOW -> browse.load(ExploreType.SHOWS)
                else -> browse.load(ExploreType.ALL)
            }
        }
    }
}
