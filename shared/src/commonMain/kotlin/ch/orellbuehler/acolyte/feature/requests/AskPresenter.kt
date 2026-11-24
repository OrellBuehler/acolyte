package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.model.AskAnswer
import ch.orellbuehler.acolyte.api.model.TitleAsk
import ch.orellbuehler.acolyte.api.model.AskEpisodes
import ch.orellbuehler.acolyte.api.model.LookupShow
import ch.orellbuehler.acolyte.api.model.NewAsk
import ch.orellbuehler.acolyte.api.model.QualityChoice
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ShowMode {
    ALL,
    CHOOSE,
    FUTURE,
}

enum class AskOutcome {
    REQUESTED,
    APPROVED,
    JOINED,
    ALREADY,
    TURNED_DOWN,
}

enum class EpisodesLoad {
    NONE,
    LOADING,
    FAILED,
    READY,
}

data class AskEpisodeTick(
    val number: Int,
    val name: String,
    val ticked: Boolean,
)

data class AskSeasonRow(
    val number: Int,
    val name: String,
    val kept: Boolean,
    val complete: Boolean,
    val ticked: Boolean,
    val expanded: Boolean,
    val load: EpisodesLoad,
    val episodes: List<AskEpisodeTick>,
    val picked: Int,
) {
    val available: Boolean get() = !kept && !complete
}

data class AskSheetState(
    val status: LoadStatus = LoadStatus(),
    val kind: String = "movie",
    val tmdb: String = "",
    val name: String = "",
    val year: Int = 0,
    val poster: String = "",
    val titleId: Long = 0,
    val neverAdd: Boolean = false,
    val briefs: Map<String, TitleAsk> = emptyMap(),
    val pills: List<QualityPill> = emptyList(),
    val selected: String = "",
    val also: Set<String> = emptySet(),
    val pickMany: Boolean = false,
    val follow: Boolean = true,
    val mode: ShowMode = ShowMode.ALL,
    val seasons: List<AskSeasonRow> = emptyList(),
    val busy: Boolean = false,
    val error: ApiError? = null,
    val outcome: AskOutcome? = null,
    val outcomeFourK: Boolean = false,
) {
    val isShow: Boolean get() = kind == "show"

    val showsQuality: Boolean get() = pills.size > 1

    val alsoPills: List<QualityPill> get() = alsoPills(pills, selected, pickMany)

    val failure: AskFailure? get() = error?.let { askFailure(it) }

    val chooseAllowed: Boolean get() = seasons.any { it.available }

    val pickedSeasons: List<Int> get() = seasons.filter { it.ticked }.map { it.number }

    val pickedEpisodes: Int get() = seasons.filter { !it.ticked }.sumOf { it.picked }

    val brief: TitleAsk? get() = if (isShow) briefs[""] else briefs[selected.ifEmpty { STANDARD_LINE }]

    val joining: Boolean get() = brief?.mine == false

    val canSubmit: Boolean
        get() =
            status.phase == Phase.READY &&
                !busy &&
                outcome == null &&
                (!showsQuality || selected.isNotEmpty()) &&
                (!isShow || mode != ShowMode.CHOOSE || pickedSeasons.isNotEmpty() || pickedEpisodes > 0)
}

class AskPresenter internal constructor(
    private val api: Api,
    private val inbox: RequestsInbox,
    kind: String,
    tmdb: String,
    private val fourKOnly: Boolean = false,
) {
    private var show: LookupShow? = null
    private var sent: Set<String> = emptySet()
    private val mutableState = MutableStateFlow(AskSheetState(kind = if (kind == "show") "show" else "movie", tmdb = tmdb))
    private val lock = Mutex()

    val state: StateFlow<AskSheetState> = mutableState.asStateFlow()

    suspend fun load() {
        lock.withLock {
            if (mutableState.value.status.phase == Phase.READY) return
            mutableState.update { it.copy(status = it.status.started()) }
            val current = mutableState.value
            coroutineScope {
                val limit = async { api.result(RequestsEndpoints.limit).valueOrNull() }
                if (current.isShow) loadShow(current.tmdb) else loadMovie(current.tmdb)
                val answer = limit.await()
                mutableState.update { s ->
                    if (s.status.phase != Phase.READY) return@update s
                    val choices: List<QualityChoice> =
                        if (s.isShow) {
                            answer?.qualities?.show.orEmpty()
                        } else {
                            answer
                                ?.qualities
                                ?.movie
                                .orEmpty()
                        }
                    val row =
                        if (s.isShow) {
                            memberRow(choices)
                        } else {
                            memberRow(
                                choices,
                                inLibrary = s.titleId > 0,
                                has4k = movieHas4k,
                                mine = movieBriefs.filterValues { it.mine }.keys,
                                open = movieBriefs.filterValues { !it.mine }.keys,
                                prefer = if (fourKOnly) FOUR_K_LINE else null,
                            )
                        }
                    s.copy(pills = row.pills, selected = row.selected, pickMany = answer?.pickMany == true)
                }
            }
        }
    }

    private var movieBriefs: Map<String, TitleAsk> = emptyMap()
    private var movieHas4k = false

    private suspend fun loadMovie(tmdb: String) {
        when (val result = api.result(LibraryEndpoints.lookupMovies, "term" to "tmdb:$tmdb")) {
            is ApiResult.Ok -> {
                val movie = result.value.movies.firstOrNull()
                if (movie == null) {
                    mutableState.update { it.copy(status = it.status.failed(ApiError(ApiErrorCode.NOT_FOUND, 404))) }
                    return
                }
                movieBriefs = listOfNotNull(movie.ask?.let { STANDARD_LINE to it }, movie.ask4k?.let { FOUR_K_LINE to it }).toMap()
                movieHas4k = movie.has4k
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        name = movie.name,
                        year = movie.year,
                        poster = movie.poster,
                        titleId = movie.titleId,
                        neverAdd = movie.neverAdd,
                        briefs = movieBriefs,
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }

    private suspend fun loadShow(tmdb: String) {
        when (val result = api.result(LibraryEndpoints.lookupShows, "term" to "tmdb:$tmdb")) {
            is ApiResult.Ok -> {
                val found = result.value.shows.firstOrNull()
                if (found == null) {
                    mutableState.update { it.copy(status = it.status.failed(ApiError(ApiErrorCode.NOT_FOUND, 404))) }
                    return
                }
                show = found
                val library =
                    if (found.titleId >
                        0
                    ) {
                        api
                            .result(RequestsEndpoints.titleSeasons, "id" to found.titleId)
                            .valueOrNull()
                            ?.seasons
                            .orEmpty()
                    } else {
                        emptyList()
                    }
                val kept = library.filter { it.keepUpToDate }.map { it.number }.toSet()
                val complete = library.filter { it.number > 0 && it.aired > 0 && it.inLibrary >= it.aired }.map { it.number }.toSet()
                val rows =
                    found.seasons.sortedBy { it.number }.map { s ->
                        AskSeasonRow(
                            s.number,
                            s.name,
                            s.number in kept,
                            s.number in complete,
                            false,
                            false,
                            EpisodesLoad.NONE,
                            emptyList(),
                            0,
                        )
                    }
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        name = found.name,
                        year = found.year,
                        poster = found.poster,
                        titleId = found.titleId,
                        neverAdd = found.neverAdd,
                        briefs = listOfNotNull(found.ask?.let { "" to it }).toMap(),
                        seasons = rows,
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }

    fun select(key: String) =
        mutableState.update { s ->
            if (s.pills.none { it.key == key && !it.disabled }) s else s.copy(selected = key, also = s.also - key, error = null)
        }

    fun toggleAlso(key: String) = mutableState.update { s -> s.copy(also = if (key in s.also) s.also - key else s.also + key) }

    fun setFollow(on: Boolean) = mutableState.update { it.copy(follow = on) }

    fun setMode(mode: ShowMode) =
        mutableState.update { s ->
            if (mode == ShowMode.CHOOSE &&
                !s.chooseAllowed
            ) {
                s
            } else {
                s.copy(mode = mode, error = null)
            }
        }

    fun toggleSeason(number: Int) =
        mutableState.update { s ->
            s.copy(
                seasons =
                    s.seasons.map { row ->
                        if (row.number != number || !row.available) {
                            row
                        } else {
                            val on = !row.ticked
                            row.copy(ticked = on, picked = 0, episodes = row.episodes.map { it.copy(ticked = false) })
                        }
                    },
                error = null,
            )
        }

    fun toggleEpisode(
        number: Int,
        episode: Int,
    ) = mutableState.update { s ->
        s.copy(
            seasons =
                s.seasons.map { row ->
                    if (row.number != number || row.ticked) {
                        row
                    } else {
                        val eps = row.episodes.map { if (it.number == episode) it.copy(ticked = !it.ticked) else it }
                        row.copy(episodes = eps, picked = eps.count { it.ticked })
                    }
                },
            error = null,
        )
    }

    suspend fun expandSeason(number: Int) {
        val row = mutableState.value.seasons.firstOrNull { it.number == number } ?: return
        val open = !row.expanded
        updateRow(number) { it.copy(expanded = open) }
        if (!open || row.load == EpisodesLoad.READY || row.load == EpisodesLoad.LOADING) return
        val tmdb =
            show
                ?.ids
                ?.get("tmdb")
                .orEmpty()
                .ifEmpty { mutableState.value.tmdb }
        updateRow(number) { it.copy(load = EpisodesLoad.LOADING) }
        when (val result = api.result(RequestsEndpoints.seasonEpisodes, "tmdb_id" to tmdb, "number" to number)) {
            is ApiResult.Ok -> {
                val eps = result.value.episodes.map { AskEpisodeTick(it.number, it.name, false) }
                updateRow(number) { it.copy(load = EpisodesLoad.READY, episodes = eps, picked = 0) }
            }
            is ApiResult.Failed -> updateRow(number) { it.copy(load = EpisodesLoad.FAILED) }
        }
    }

    suspend fun submit() {
        val start = mutableState.value
        if (!start.canSubmit) return
        mutableState.update { it.copy(busy = true, error = null) }
        val lines = (listOf(start.selected) + start.alsoPills.filter { it.key in start.also }.map { it.key }).filter { it !in sent }
        var last: AskAnswer? = null
        for (line in lines.ifEmpty { listOf("") }) {
            when (val result = api.result(RequestsEndpoints.make, body(start, line))) {
                is ApiResult.Ok -> {
                    sent = sent + line
                    last = result.value
                }
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(busy = false, error = result.error, also = it.also - sent) }
                    return
                }
            }
        }
        inbox.refresh()
        val answer = last
        val outcome =
            when {
                answer == null -> AskOutcome.REQUESTED
                answer.ask.state == APPROVED -> AskOutcome.APPROVED
                answer.ask.state == TURNED_DOWN -> AskOutcome.TURNED_DOWN
                answer.joined -> AskOutcome.JOINED
                answer.already -> AskOutcome.ALREADY
                else -> AskOutcome.REQUESTED
            }
        mutableState.update { it.copy(busy = false, outcome = outcome, outcomeFourK = start.selected == FOUR_K_LINE) }
    }

    private fun body(
        s: AskSheetState,
        line: String,
    ): NewAsk {
        val quality = line.ifEmpty { null }
        if (!s.isShow) return NewAsk(id = "tmdb:${s.tmdb}", kind = "movie", follow = s.follow, quality = quality)
        val id = showId(show, s.tmdb)
        return when (s.mode) {
            ShowMode.FUTURE -> NewAsk(id = id, kind = "show", futureOnly = true, follow = s.follow, quality = quality)
            ShowMode.ALL -> NewAsk(id = id, kind = "show", seasons = emptyList(), follow = s.follow, quality = quality)
            ShowMode.CHOOSE -> {
                val episodes =
                    s.seasons
                        .filter { !it.ticked && it.picked > 0 }
                        .map { row ->
                            AskEpisodes(
                                row.number,
                                row.episodes
                                    .filter { it.ticked }
                                    .map { it.number }
                                    .sorted(),
                            )
                        }
                NewAsk(
                    id = id,
                    kind = "show",
                    seasons = s.pickedSeasons,
                    episodes = episodes.ifEmpty { null },
                    follow = s.follow,
                    quality = quality,
                )
            }
        }
    }

    private fun updateRow(
        number: Int,
        transform: (AskSeasonRow) -> AskSeasonRow,
    ) = mutableState.update { s -> s.copy(seasons = s.seasons.map { if (it.number == number) transform(it) else it }) }
}

internal fun showId(
    show: LookupShow?,
    tmdb: String,
): String {
    val ids = show?.ids.orEmpty()
    for (key in listOf(show?.source.orEmpty(), "tmdb", "tvdb", "tvmaze", "imdb")) {
        val value = ids[key]
        if (key.isNotEmpty() && !value.isNullOrEmpty()) return "$key:$value"
    }
    return "tmdb:$tmdb"
}
