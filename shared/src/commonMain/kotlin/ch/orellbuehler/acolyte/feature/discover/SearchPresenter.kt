package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.LookupMovie
import ch.orellbuehler.acolyte.api.model.LookupShow
import ch.orellbuehler.acolyte.api.model.PeopleHit
import ch.orellbuehler.acolyte.api.model.PlainQuery
import ch.orellbuehler.acolyte.api.model.SearchTitle
import ch.orellbuehler.acolyte.api.model.TitleAsk
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SearchScope(
    val wire: String,
) {
    ALL("all"),
    PEOPLE("people"),
    MOVIE("movie"),
    SHOW("show"),
}

enum class SuggestKind {
    MOVIE,
    SHOW,
    PERSON,
}

enum class SuggestState {
    NONE,
    HAVE,
    MISSING,
    ASKED,
}

data class Suggestion(
    val kind: SuggestKind,
    val name: String,
    val sub: String,
    val art: String?,
    val state: SuggestState,
    val tmdb: String,
    val personId: String,
)

data class SearchHit(
    val card: DiscoverCard? = null,
    val person: PersonCard? = null,
) {
    val name: String get() = card?.name ?: person?.name.orEmpty()
}

data class Understood(
    val name: String,
    val genres: List<String>,
    val kind: CardKind?,
    val music: Boolean,
    val yearFrom: Int,
    val yearTo: Int,
    val people: List<String>,
    val words: List<String>,
)

data class SearchResults(
    val searched: String = "",
    val library: List<DiscoverCard> = emptyList(),
    val top: List<SearchHit> = emptyList(),
    val movies: List<DiscoverCard> = emptyList(),
    val shows: List<DiscoverCard> = emptyList(),
    val people: List<PersonCard> = emptyList(),
    val understood: Understood? = null,
    val plainLibrary: List<DiscoverCard> = emptyList(),
    val more: List<DiscoverCard> = emptyList(),
    val metadataDown: Boolean = false,
    val error: ApiError? = null,
) {
    val isEmpty: Boolean
        get() =
            library.isEmpty() &&
                movies.isEmpty() &&
                shows.isEmpty() &&
                people.isEmpty() &&
                plainLibrary.isEmpty() &&
                more.isEmpty()
}

data class SearchState(
    val query: String = "",
    val scope: SearchScope = SearchScope.ALL,
    val scopes: List<SearchScope> = emptyList(),
    val onlyLibrary: Boolean = false,
    val recents: List<String> = emptyList(),
    val completions: List<String> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val searching: Boolean = false,
    val results: SearchResults? = null,
    val canManage: Boolean = false,
    val canHide: Boolean = false,
) {
    val shown: SearchResults?
        get() {
            val r = results ?: return null
            if (!onlyLibrary) return r
            return r.copy(top = emptyList(), movies = emptyList(), shows = emptyList(), people = emptyList(), more = emptyList())
        }
}

private val KIND_WORDS = Regex("\\b(movies?|films?|shows?|series|tv|sitcom)\\b", RegexOption.IGNORE_CASE)
private val YEAR_WORDS =
    Regex(
        "\\b(from|in|after|before|since|around)\\s+(18[7-9]\\d|19\\d\\d|20\\d\\d)\\b|\\b'?\\d0'?s\\b|\\b(18|19|20)\\d0s\\b|" +
            "\\b(twenties|thirties|forties|fifties|sixties|seventies|eighties|nineties)\\b",
        RegexOption.IGNORE_CASE,
    )
private val ABOUT_WORDS = Regex("\\b(starring|featuring|about|with)\\b", RegexOption.IGNORE_CASE)
private val ID_LIKE = Regex("^[a-z]+:\\S+$", RegexOption.IGNORE_CASE)
private val SHOW_ME = Regex("\\bshow me\\b", RegexOption.IGNORE_CASE)
private val SPACES = Regex("\\s+")

fun describes(text: String): Boolean {
    val t = text.trim()
    val words = t.split(SPACES).filter { it.isNotEmpty() }.size
    if (t.length > PLAIN_MAX || words < 2 || ID_LIKE.matches(t)) return false
    return KIND_WORDS.containsMatchIn(t.replace(SHOW_ME, "")) ||
        YEAR_WORDS.containsMatchIn(t) ||
        (ABOUT_WORDS.containsMatchIn(t) && words >= 3)
}

private const val PLAIN_MAX = 300

internal fun <T> rankTop(
    groups: List<List<T>>,
    term: String,
    name: (T) -> String,
    max: Int = 6,
): List<T> {
    val want = term.trim().lowercase()
    val exact = groups.mapNotNull { group -> group.firstOrNull { want.isNotEmpty() && name(it).trim().lowercase() == want } }
    val rest = groups.map { group -> group.filter { it !in exact } }
    val out = exact.toMutableList()
    var round = 0
    while (out.size < max && rest.any { round < it.size }) {
        for (group in rest) {
            if (out.size >= max) break
            group.getOrNull(round)?.let { out += it }
        }
        round++
    }
    return out.take(max)
}

internal fun libraryNote(hit: SearchTitle): CardNote? {
    if (hit.slots.any { it.download?.active == true || it.state == "downloading" || it.state == "importing" }) {
        return CardNote(NoteKind.DOWNLOADING)
    }
    val show = hit.show
    if (hit.kind == "show" && show != null) {
        if (show.missing > 0) return CardNote(NoteKind.MISSING_EPISODES, show.missing)
        return CardNote(NoteKind.SEASONS_UP_TO_DATE, show.seasons.count { it.number > 0 })
    }
    val artist = hit.artist
    if (hit.kind == "artist" && artist != null) {
        if (artist.missing > 0) return CardNote(NoteKind.MISSING_TRACKS, artist.missing)
        return CardNote(NoteKind.ALBUMS_UP_TO_DATE, artist.albums)
    }
    return null
}

internal fun SearchTitle.card(
    rules: CardRules,
    owned: OwnedIndex,
    row: String,
): DiscoverCard {
    val ownedItem = owned.byTitleId(id)
    return DiscoverCard(
        key = "$row-$id",
        kind = CardKind.of(kind),
        tmdb = tmdb.ifEmpty { ownedItem?.tmdb.orEmpty() },
        titleId = id,
        name = name,
        year = year,
        poster = artwork.firstOrNull(),
        backdrop = backdrop ?: artwork.firstOrNull(),
        logo = logo,
        genres = genres,
        mark = ownedItem?.let { ownedMark(it, null, rules.staff) } ?: CardMark(MarkState.AVAILABLE, MarkLabel.IN_LIBRARY),
        rowType = row,
        note = libraryNote(this),
    )
}

internal fun lookupCard(
    kind: CardKind,
    name: String,
    year: Int,
    ids: Map<String, String>,
    poster: String,
    backdrop: String?,
    titleId: Long,
    overview: String,
    genres: List<String>,
    neverAdd: Boolean,
    hidden: Boolean,
    ask: TitleAsk?,
    rules: CardRules,
    owned: OwnedIndex,
): DiscoverCard {
    val tmdb = ids["tmdb"].orEmpty()
    val ownedItem = owned.find(kind.wire, tmdb)
    val id = if (titleId > 0) titleId else ownedItem?.id ?: 0
    val mark = ownedItem?.let { ownedMark(it, ask, rules.staff) } ?: plainMark(titleId > 0, ask, hidden, rules.staff)
    val mine = ask?.mine == true
    val picture = poster.ifEmpty { null }
    return DiscoverCard(
        key = "${kind.wire}-$tmdb-$name",
        kind = kind,
        tmdb = tmdb,
        titleId = id,
        name = name,
        year = year,
        poster = picture,
        backdrop = backdrop ?: picture,
        overview = overview,
        genres = genres,
        mark = mark,
        action = cardAction(kind, tmdb, id, mark, mine, neverAdd, rules),
        mine = mine,
        rowType = "search",
    )
}

internal fun LookupMovie.card(
    rules: CardRules,
    owned: OwnedIndex,
): DiscoverCard =
    lookupCard(CardKind.MOVIE, name, year, ids, poster, backdrop, titleId, overview, genres, neverAdd, hidden, ask, rules, owned)

internal fun LookupShow.card(
    rules: CardRules,
    owned: OwnedIndex,
): DiscoverCard =
    lookupCard(CardKind.SHOW, name, year, ids, poster, backdrop, titleId, overview, genres, neverAdd, hidden, ask, rules, owned)

internal fun PeopleHit.card(): PersonCard = PersonCard(id, name, department, profile)

internal fun PlainQuery.understood(): Understood? {
    if (plain) return null
    return Understood(
        name = name,
        genres = genres,
        kind =
            when (kind) {
                "movie" -> CardKind.MOVIE
                "show" -> CardKind.SHOW
                else -> null
            },
        music = kind == "music",
        yearFrom = yearFrom,
        yearTo = yearTo,
        people = people,
        words = words,
    )
}

class SearchPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val owned: OwnedLibrary,
    private val prefs: DiscoverPrefs,
) {
    private val mutableState = MutableStateFlow(SearchState())
    private var suggestRun = 0
    private var searchRun = 0
    private var suggestOff = false

    val state: StateFlow<SearchState> = mutableState.asStateFlow()

    fun load() {
        val account = account()
        val scopes =
            buildList {
                add(SearchScope.ALL)
                if (account?.isOn(Part.MOVIES) != false) add(SearchScope.PEOPLE)
                if (account?.isOn(Part.MOVIES) != false) add(SearchScope.MOVIE)
                if (account?.isOn(Part.SHOWS) != false) add(SearchScope.SHOW)
            }
        mutableState.update {
            it.copy(
                scopes = scopes,
                recents = prefs.recents,
                onlyLibrary = prefs.onlyLibrary,
                canManage = account?.can(Need.MANAGE_LIBRARY) == true,
                canHide = account?.can(Need.USERS) == true,
            )
        }
    }

    fun type(text: String) {
        mutableState.update { it.copy(query = text) }
        if (text.isBlank()) {
            searchRun++
            suggestRun++
            mutableState.update { it.copy(results = null, searching = false, suggestions = emptyList(), completions = emptyList()) }
        }
    }

    fun selectScope(scope: SearchScope) {
        mutableState.update { it.copy(scope = scope) }
    }

    fun setOnlyLibrary(on: Boolean) {
        prefs.onlyLibrary = on
        mutableState.update { it.copy(onlyLibrary = on) }
    }

    fun clearRecents() {
        prefs.recents = emptyList()
        mutableState.update { it.copy(recents = emptyList()) }
    }

    fun remember(text: String) {
        val words = text.trim()
        if (words.isEmpty()) return
        val list = (listOf(words) + prefs.recents.filter { !it.equals(words, ignoreCase = true) }).take(RECENT_MAX)
        prefs.recents = list
        mutableState.update { it.copy(recents = list) }
    }

    suspend fun suggest(text: String) {
        val words = text.trim()
        val run = ++suggestRun
        if (suggestOff || words.length < SUGGEST_MIN || words.length > SUGGEST_MAX) {
            mutableState.update { it.copy(suggestions = emptyList(), completions = emptyList()) }
            return
        }
        val scope = mutableState.value.scope
        val kind = if (scope == SearchScope.ALL) null else scope.wire
        when (val result = api.result(DiscoverEndpoints.suggest, "q" to words, "kind" to kind)) {
            is ApiResult.Failed -> {
                if (result.error.code == ApiErrorCode.NOT_FOUND) suggestOff = true
                if (run == suggestRun) mutableState.update { it.copy(suggestions = emptyList(), completions = emptyList()) }
            }
            is ApiResult.Ok -> {
                if (run != suggestRun) return
                val items =
                    result.value.items.mapNotNull { row ->
                        val kind =
                            when (row.kind) {
                                "movie" -> SuggestKind.MOVIE
                                "show" -> SuggestKind.SHOW
                                "person" -> SuggestKind.PERSON
                                else -> return@mapNotNull null
                            }
                        val state =
                            when (row.state) {
                                "have" -> SuggestState.HAVE
                                "missing" -> SuggestState.MISSING
                                "asked" -> SuggestState.ASKED
                                else -> SuggestState.NONE
                            }
                        Suggestion(kind, row.name, row.sub, row.art, state, row.tmdb, row.personId)
                    }
                mutableState.update { it.copy(suggestions = items, completions = result.value.completions.take(COMPLETIONS)) }
            }
        }
    }

    suspend fun search(text: String) {
        val words = text.trim()
        val run = ++searchRun
        if (words.isEmpty()) {
            mutableState.update { it.copy(results = null, searching = false) }
            return
        }
        mutableState.update { it.copy(searching = true) }
        val account = account()
        val rules = CardRules.of(account)
        val scope = mutableState.value.scope
        val moviesOn = account?.isOn(Part.MOVIES) != false
        val showsOn = account?.isOn(Part.SHOWS) != false
        val wantMovies = moviesOn && (scope == SearchScope.ALL || scope == SearchScope.MOVIE)
        val wantShows = showsOn && (scope == SearchScope.ALL || scope == SearchScope.SHOW)
        val wantPeople = moviesOn && (scope == SearchScope.ALL || scope == SearchScope.PEOPLE)
        val plain = scope == SearchScope.ALL && describes(words)
        val index = owned.get()
        val results =
            coroutineScope {
                val library = async { api.result(LibraryEndpoints.librarySearch, "q" to words.take(LIBRARY_MAX), "kind" to scope.wire) }
                val movies = async { if (wantMovies) api.result(LibraryEndpoints.lookupMovies, "term" to words) else null }
                val shows = async { if (wantShows) api.result(LibraryEndpoints.lookupShows, "term" to words) else null }
                val people = async { if (wantPeople) api.result(DiscoverEndpoints.peopleSearch, "q" to words.take(PEOPLE_MAX)) else null }
                val described = async { if (plain) api.result(DiscoverEndpoints.plainSearch, "q" to words) else null }
                val outcomes = listOf(library.await(), movies.await(), shows.await(), people.await(), described.await())
                val failures = outcomes.filterIsInstance<ApiResult.Failed>().map { it.error }
                val down = failures.any { it.status == SERVICE_UNAVAILABLE }
                val shown = failures.firstOrNull { it.code != ApiErrorCode.FORBIDDEN && it.status != SERVICE_UNAVAILABLE }
                val lib = library.await().valueOrNull()
                val libraryCards =
                    lib
                        ?.titles
                        .orEmpty()
                        .filter { it.kind != "artist" }
                        .map { it.card(rules, index, "library") } +
                        lib?.people.orEmpty().mapNotNull { person ->
                            val first = person.titles.firstOrNull() ?: return@mapNotNull null
                            val ownedItem = index.byTitleId(first.id)
                            DiscoverCard(
                                key = "person-${person.name}",
                                kind = CardKind.of(first.kind),
                                tmdb = ownedItem?.tmdb.orEmpty(),
                                titleId = first.id,
                                name = person.name,
                                year = 0,
                                subtitle = person.titles.take(2).joinToString(", ") { it.name },
                                rowType = "library_person",
                            )
                        }
                val movieCards =
                    movies
                        .await()
                        ?.valueOrNull()
                        ?.movies
                        .orEmpty()
                        .map { it.card(rules, index) }
                val showCards =
                    shows
                        .await()
                        ?.valueOrNull()
                        ?.shows
                        .orEmpty()
                        .map { it.card(rules, index) }
                val peopleAnswer = people.await()?.valueOrNull()
                val personCards = if (peopleAnswer?.sourceOff == true) emptyList() else peopleAnswer?.people.orEmpty().map { it.card() }
                val plainAnswer = described.await()?.valueOrNull()
                val top =
                    if (scope == SearchScope.ALL) {
                        rankTop(
                            listOf(
                                libraryCards.map { SearchHit(card = it) },
                                movieCards.map { SearchHit(card = it) },
                                showCards.map { SearchHit(card = it) },
                                personCards.map { SearchHit(person = it) },
                            ),
                            words,
                            { it.name },
                        )
                    } else {
                        emptyList()
                    }
                SearchResults(
                    searched = words,
                    library = libraryCards.distinctBy { it.key },
                    top = top,
                    movies = movieCards.distinctBy { it.key },
                    shows = showCards.distinctBy { it.key },
                    people = personCards.distinctBy { it.id },
                    understood = plainAnswer?.query?.understood(),
                    plainLibrary = plainAnswer?.titles.orEmpty().map { it.card(rules, index, "plain") },
                    more = plainAnswer?.more.orEmpty().map { it.card("plain", rules, index) },
                    metadataDown = down,
                    error = shown,
                )
            }
        if (run != searchRun) return
        mutableState.update { it.copy(results = results, searching = false) }
    }

    suspend fun refreshOwned() {
        owned.refresh()
        val words = mutableState.value.results?.searched ?: return
        search(words)
    }

    private companion object {
        const val SUGGEST_MIN = 2
        const val SUGGEST_MAX = 100
        const val LIBRARY_MAX = 256
        const val PEOPLE_MAX = 200
        const val COMPLETIONS = 3
        const val RECENT_MAX = 8
        const val SERVICE_UNAVAILABLE = 503
    }
}
