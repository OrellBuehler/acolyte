package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.api.model.DiscoverItem
import ch.orellbuehler.acolyte.api.model.PeopleShelfPerson
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class BrowseTarget {
    CATEGORY,
    MOOD,
    GENRE,
}

data class BrowseLink(
    val target: BrowseTarget,
    val type: ExploreType,
    val genre: Int,
    val category: String,
    val mood: String,
)

fun browseLink(
    tile: BrowseTile,
    tab: ExploreType,
): BrowseLink {
    val own = if (tile.kind == "show") ExploreType.SHOWS else ExploreType.MOVIES
    val type =
        if (tile.both) {
            ExploreType.ALL
        } else if (tab == ExploreType.ALL) {
            own
        } else {
            tab
        }
    return when {
        tile.category.isNotEmpty() -> BrowseLink(BrowseTarget.CATEGORY, tab, tile.genre, tile.category, "")
        tile.mood.isNotEmpty() -> BrowseLink(BrowseTarget.MOOD, type, tile.genre, "", tile.mood)
        else -> BrowseLink(BrowseTarget.GENRE, type, tile.genre, "", "")
    }
}

data class BrowseSectionState(
    val id: String,
    val title: String,
    val tiles: List<BrowseTile> = emptyList(),
    val loaded: Boolean = false,
    val building: Boolean = false,
)

data class BrowseGridState(
    val status: LoadStatus = LoadStatus(),
    val tab: ExploreType = ExploreType.ALL,
    val tiles: List<BrowseTile> = emptyList(),
    val sections: List<BrowseSectionState> = emptyList(),
)

class BrowsePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(BrowseGridState())
    private val lock = Mutex()

    val state: StateFlow<BrowseGridState> = mutableState.asStateFlow()

    suspend fun load(tab: ExploreType) {
        if (mutableState.value.tab == tab && mutableState.value.status.phase == Phase.READY) return
        lock.withLock {
            mutableState.value = BrowseGridState(tab = tab)
            refreshLocked()
        }
    }

    suspend fun refresh() {
        lock.withLock { refreshLocked() }
    }

    private suspend fun refreshLocked() {
        val tab = mutableState.value.tab
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(DiscoverEndpoints.browse, "type" to tab.wire)) {
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            is ApiResult.Ok -> {
                val account = account()
                val tiles =
                    result.value.tiles.filter { tile ->
                        val kind = if (tile.kind == "show") CardKind.SHOW else CardKind.MOVIE
                        val partOn = account?.isOn(if (kind == CardKind.SHOW) Part.SHOWS else Part.MOVIES) != false
                        partOn && (tile.both || tab.keeps(kind))
                    }
                val sections =
                    result.value.sections
                        .filter { it.id != "genres" }
                        .map { BrowseSectionState(it.id, it.title) }
                mutableState.update { it.copy(status = LoadStatus.ready, tiles = tiles, sections = sections) }
            }
        }
    }

    suspend fun loadSection(id: String) {
        val section = mutableState.value.sections.firstOrNull { it.id == id } ?: return
        if (section.loaded && !section.building) return
        val tab = mutableState.value.tab
        val answer = api.result(DiscoverEndpoints.browseSection, "type" to tab.wire, "section" to id).valueOrNull() ?: return
        mutableState.update { state ->
            state.copy(
                sections =
                    state.sections.map {
                        if (it.id == id) it.copy(tiles = answer.tiles, loaded = true, building = answer.building) else it
                    },
            )
        }
    }

    suspend fun watch() {
        var tries = 0
        while (tries < BUILDING_TRIES) {
            val building = mutableState.value.sections.filter { it.building }
            if (building.isEmpty()) {
                delay(IDLE_DELAY)
                continue
            }
            delay(BUILDING_DELAY)
            tries++
            building.forEach { loadSection(it.id) }
        }
    }

    private companion object {
        const val BUILDING_TRIES = 48
        const val BUILDING_DELAY = 2_500L
        const val IDLE_DELAY = 1_000L
    }
}

data class BrowseViewState(
    val status: LoadStatus = LoadStatus(),
    val title: String = "",
    val genreName: String = "",
    val type: ExploreType = ExploreType.ALL,
    val cards: List<DiscoverCard> = emptyList(),
    val canHide: Boolean = false,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && cards.isEmpty()
}

class BrowseViewPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val owned: OwnedLibrary,
    private val link: BrowseLink,
) {
    private val mutableState =
        MutableStateFlow(
            BrowseViewState(
                title = link.mood,
                genreName = Genres.named(link.genre, link.type)?.name.orEmpty(),
                type = link.type,
            ),
        )
    private var items: List<DiscoverItem> = emptyList()

    val state: StateFlow<BrowseViewState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started()) }
        val index = owned.get()
        if (link.target == BrowseTarget.CATEGORY) {
            when (val result = api.result(DiscoverEndpoints.browseCategory, "type" to link.type.wire, "id" to link.category)) {
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                is ApiResult.Ok -> {
                    items = result.value.items
                    mutableState.update { it.copy(title = result.value.name) }
                    publish(index)
                }
            }
        } else {
            when (val result = api.result(DiscoverEndpoints.genre, "type" to link.type.wire, "genre" to link.genre)) {
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                is ApiResult.Ok -> {
                    items =
                        result.value.rows
                            .firstOrNull { it.type == "mood" && it.title.equals(link.mood, ignoreCase = true) }
                            ?.items
                            .orEmpty()
                    mutableState.update {
                        it.copy(
                            genreName =
                                result.value.genre.name
                                    .ifEmpty { it.genreName },
                        )
                    }
                    publish(index)
                }
            }
        }
    }

    suspend fun refreshOwned() {
        if (mutableState.value.status.phase == Phase.READY) publish(owned.refresh())
    }

    private fun publish(index: OwnedIndex) {
        val rules = CardRules.of(account())
        val row = if (link.target == BrowseTarget.CATEGORY) "category" else "mood"
        mutableState.update {
            it.copy(status = LoadStatus.ready, cards = items.map { item -> item.card(row, rules, index) }, canHide = rules.canHide)
        }
    }
}

enum class PeopleFilter(
    internal val department: String,
) {
    POPULAR(""),
    ACTORS("Acting"),
    DIRECTORS("Directing"),
    COMPOSERS("Sound"),
}

data class PeopleShelfState(
    val id: String,
    val title: String,
    val people: List<PersonCard> = emptyList(),
)

data class PeopleBrowseState(
    val status: LoadStatus = LoadStatus(),
    val filter: PeopleFilter = PeopleFilter.POPULAR,
    val filters: List<PeopleFilter> = listOf(PeopleFilter.POPULAR),
    val shelves: List<PeopleShelfState> = emptyList(),
    val popular: List<PersonCard> = emptyList(),
    val more: Boolean = false,
    val loadingMore: Boolean = false,
    val attribution: String = "",
)

class PeopleBrowsePresenter internal constructor(
    private val api: Api,
) {
    private val mutableState = MutableStateFlow(PeopleBrowseState())
    private val lock = Mutex()
    private var shelfPeople: Map<String, List<PeopleShelfPerson>> = emptyMap()
    private var shelfTitles: List<Pair<String, String>> = emptyList()
    private var popular: List<Pair<PersonCard, String>> = emptyList()
    private var page = 0
    private var pages = 1

    val state: StateFlow<PeopleBrowseState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            page = 0
            pages = 1
            popular = emptyList()
            val shelves = api.result(DiscoverEndpoints.peopleShelves)
            if (shelves is ApiResult.Failed) {
                mutableState.update { it.copy(status = it.status.failed(shelves.error)) }
                return
            }
            val meta = (shelves as ApiResult.Ok).value
            shelfTitles = meta.shelves.filter { it.count >= SHELF_MIN }.map { it.id to it.title }
            shelfPeople =
                shelfTitles.associate { (id, _) ->
                    id to
                        api
                            .result(DiscoverEndpoints.peopleShelf, "id" to id)
                            .valueOrNull()
                            ?.people
                            .orEmpty()
                }
            mutableState.update { it.copy(attribution = meta.attribution) }
            loadPage()
            publish()
            mutableState.update { it.copy(status = LoadStatus.ready) }
        }
    }

    suspend fun select(filter: PeopleFilter) {
        mutableState.update { it.copy(filter = filter) }
        publish()
        lock.withLock {
            while (filter != PeopleFilter.POPULAR && popular.count { it.second == filter.department } < FILTER_TARGET && page < pages) {
                loadPage()
            }
        }
        publish()
    }

    suspend fun loadMore() {
        lock.withLock {
            if (page >= pages || page >= MAX_PAGES) return
            mutableState.update { it.copy(loadingMore = true) }
            loadPage()
        }
        publish()
    }

    private suspend fun loadPage() {
        if (page >= MAX_PAGES || page >= pages) return
        val answer = api.result(DiscoverEndpoints.peoplePopular, "page" to page + 1).valueOrNull()
        if (answer == null) {
            pages = page
            return
        }
        page++
        pages = answer.totalPages.coerceAtMost(MAX_PAGES)
        popular = (popular + answer.people.map { it.card() to it.department }).distinctBy { it.first.id }.take(MAX_PEOPLE)
    }

    private fun publish() {
        val filter = mutableState.value.filter

        fun keep(department: String) = filter == PeopleFilter.POPULAR || department == filter.department
        val shelves =
            shelfTitles
                .map { (id, title) ->
                    PeopleShelfState(
                        id,
                        title,
                        shelfPeople[id].orEmpty().filter { keep(it.department) }.map { PersonCard(it.id, it.name, it.note, it.profile) },
                    )
                }.filter { it.people.size >= SHELF_MIN }
        val everyone = shelfPeople.values.flatten().map { it.department } + popular.map { it.second }
        val filters =
            PeopleFilter.entries.filter { f -> f == PeopleFilter.POPULAR || everyone.count { it == f.department } >= SHELF_MIN }
        mutableState.update {
            it.copy(
                shelves = shelves,
                popular = popular.filter { p -> keep(p.second) }.map { p -> p.first },
                filters = filters,
                more = page < pages && page < MAX_PAGES,
                loadingMore = false,
            )
        }
    }

    private companion object {
        const val SHELF_MIN = 8
        const val MAX_PAGES = 8
        const val MAX_PEOPLE = 150
        const val FILTER_TARGET = 30
    }
}
