package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.model.FilmographyMovie
import ch.orellbuehler.acolyte.api.model.FilmographyPage
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class Decade(
    val year: Int,
    val cards: List<DiscoverCard>,
)

data class PersonAbout(
    val biography: String = "",
    val birthday: String = "",
    val deathday: String = "",
    val birthplace: String = "",
    val department: String = "",
)

data class MovieLimit(
    val used: Int,
    val left: Int,
)

data class PersonState(
    val status: LoadStatus = LoadStatus(),
    val id: String = "",
    val name: String = "",
    val photo: String? = null,
    val roles: List<String> = emptyList(),
    val firstYear: Int = 0,
    val films: Int = 0,
    val about: PersonAbout = PersonAbout(),
    val knownFor: List<DiscoverCard> = emptyList(),
    val comingSoon: List<DiscoverCard> = emptyList(),
    val inLibrary: List<DiscoverCard> = emptyList(),
    val asked: List<DiscoverCard> = emptyList(),
    val decades: List<Decade> = emptyList(),
    val oftenWith: List<PersonCard> = emptyList(),
    val role: FilmRole = FilmRole.ALL,
    val sort: FilmSort = FilmSort.OLDEST,
    val limit: MovieLimit? = null,
    val reloading: Boolean = false,
    val attribution: String = "",
    val canHide: Boolean = false,
)

internal fun FilmographyMovie.card(
    rules: CardRules,
    owned: OwnedIndex,
    row: String,
): DiscoverCard? {
    val tmdb = ids.tmdb
    if (poster == null && backdrop == null) return null
    val ownedItem = owned.find("movie", tmdb)
    val id = if (titleId > 0) titleId else ownedItem?.id ?: 0
    val visibleAsk = ask?.takeUnless { it.state == "turned_down" && !rules.staff }
    val mark = ownedItem?.let { ownedMark(it, ask, rules.staff) } ?: plainMark(titleId > 0, visibleAsk, false, rules.staff)
    val directed = characters.isEmpty() && "Director" in roles
    return DiscoverCard(
        key = "$row-$tmdb",
        kind = CardKind.MOVIE,
        tmdb = tmdb,
        titleId = id,
        name = name,
        year = year,
        subtitle = characters.joinToString(" / "),
        poster = poster,
        backdrop = backdrop ?: poster,
        overview = overview,
        genres = genres,
        mark = mark,
        action = cardAction(CardKind.MOVIE, tmdb, id, mark, ask != null, neverAdd, rules),
        mine = visibleAsk?.mine == true,
        rowType = row,
        note = if (directed) CardNote(NoteKind.DIRECTOR) else null,
    )
}

internal fun decades(
    cards: List<DiscoverCard>,
    sort: FilmSort,
): List<Decade> {
    val dated = cards.filter { it.year > 0 }.groupBy { it.year / DECADE * DECADE }
    val keys =
        when (sort) {
            FilmSort.OLDEST -> dated.keys.sorted()
            FilmSort.NEWEST, FilmSort.POPULAR -> dated.keys.sortedDescending()
        }
    val undated = cards.filter { it.year == 0 }
    val list = keys.map { Decade(it, dated.getValue(it)) }
    if (undated.isEmpty()) return list
    return if (sort == FilmSort.POPULAR) list + Decade(0, undated) else listOf(Decade(0, undated)) + list
}

private const val DECADE = 10

class PersonPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val owned: OwnedLibrary,
    private val prefs: DiscoverPrefs,
    private val id: String,
) {
    private val mutableState = MutableStateFlow(PersonState(id = id, sort = prefs.filmSort))
    private val lock = Mutex()
    private var first: FilmographyPage? = null
    private var movies: List<FilmographyMovie> = emptyList()

    val state: StateFlow<PersonState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            val error = fetch()
            if (error != null) {
                mutableState.update { it.copy(status = it.status.failed(error)) }
                return
            }
            loadLimit()
            publish(owned.get())
            mutableState.update { it.copy(status = LoadStatus.ready) }
        }
    }

    suspend fun select(
        role: FilmRole = mutableState.value.role,
        sort: FilmSort = mutableState.value.sort,
    ) {
        lock.withLock {
            prefs.filmSort = sort
            mutableState.update { it.copy(role = role, sort = sort, reloading = true) }
            val error = fetch()
            mutableState.update { it.copy(reloading = false, status = if (error != null) it.status.failed(error) else it.status) }
            publish(owned.index.value)
        }
    }

    suspend fun refreshOwned() {
        if (mutableState.value.status.phase != Phase.READY) return
        lock.withLock {
            fetch()
            loadLimit()
            publish(owned.refresh())
        }
    }

    private suspend fun fetch(): ApiError? {
        val current = mutableState.value
        var page = 1
        var all = mutableListOf<FilmographyMovie>()
        while (true) {
            val result =
                api.result(
                    DiscoverEndpoints.person,
                    "id" to id.removePrefix("tmdb:"),
                    "role" to current.role.wire,
                    "sort" to current.sort.wire,
                    "page" to page,
                    "page_size" to PAGE_SIZE,
                )
            when (result) {
                is ApiResult.Failed -> return if (page == 1) result.error else null
                is ApiResult.Ok -> {
                    if (page == 1) {
                        first = result.value
                        all = result.value.movies.toMutableList()
                    } else {
                        all += result.value.movies
                    }
                    movies = all
                    if (page >= result.value.totalPages || page >= MAX_PAGES || result.value.movies.isEmpty()) return null
                    page++
                }
            }
        }
    }

    private suspend fun loadLimit() {
        val account = account() ?: return
        if (!account.allows(Need.ASK, Part.WISHLIST) || account.can(Need.MANAGE_LIBRARY)) return
        val limit = api.result(RequestsEndpoints.limit).valueOrNull() ?: return
        mutableState.update {
            it.copy(limit = if (limit.noLimit || limit.movies.noLimit) null else MovieLimit(limit.movies.used, limit.movies.left))
        }
    }

    private fun publish(index: OwnedIndex) {
        val page = first ?: return
        val rules = CardRules.of(account())
        val credits = movies.mapNotNull { it.card(rules, index, "film") }.distinctBy { it.tmdb }
        val roles =
            buildList {
                if (movies.any { "Actor" in it.roles }) add("Actor")
                if (movies.any { "Director" in it.roles }) add("Director")
                if (isEmpty() && page.person.department.isNotEmpty()) add(page.person.department)
            }
        mutableState.update {
            it.copy(
                name = page.person.name,
                photo = page.person.profile,
                roles = roles,
                firstYear = page.stats.firstYear,
                films = page.stats.films,
                about =
                    PersonAbout(
                        biography = page.person.biography,
                        birthday = page.person.birthday,
                        deathday = page.person.deathday,
                        birthplace = page.person.birthplace,
                        department = page.person.department,
                    ),
                knownFor = page.knownFor.mapNotNull { m -> m.card(rules, index, "known") }.take(KNOWN_MAX),
                comingSoon = page.comingSoon.mapNotNull { m -> m.card(rules, index, "coming") },
                inLibrary = page.library.mapNotNull { m -> m.card(rules, index, "library") },
                asked = page.asked.filter { m -> m.ask?.mine == true }.mapNotNull { m -> m.card(rules, index, "asked") },
                decades = decades(credits, it.sort),
                oftenWith = page.oftenWith.map { p -> PersonCard(p.id, p.name, p.department, p.profile) }.take(OFTEN_MAX),
                attribution = page.attribution,
                canHide = rules.canHide,
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 20
        const val KNOWN_MAX = 10
        const val OFTEN_MAX = 8
    }
}
