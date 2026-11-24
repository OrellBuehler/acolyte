package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.ExploreRowStat
import ch.orellbuehler.acolyte.api.model.HideBody
import ch.orellbuehler.acolyte.api.model.TitleHide
import ch.orellbuehler.acolyte.api.model.TitleUser
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class HideWhat {
    MOVIE,
    SHOW,
    MOVIE_GENRE,
    SHOW_GENRE,
}

enum class HideFor {
    EVERYONE,
    PERSON,
    REMOVED_PERSON,
}

data class HiddenRow(
    val id: Long,
    val name: String,
    val what: HideWhat,
    val who: HideFor,
    val person: String,
    val genre: Boolean,
)

data class HidePerson(
    val id: Long,
    val name: String,
)

data class HiddenState(
    val status: LoadStatus = LoadStatus(),
    val rows: List<HiddenRow> = emptyList(),
    val actionError: ApiError? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty()
}

internal fun TitleHide.row(users: List<TitleUser>): HiddenRow {
    val isGenre = genre.isNotEmpty()
    val what =
        when {
            kind == "show" && isGenre -> HideWhat.SHOW_GENRE
            kind == "show" -> HideWhat.SHOW
            isGenre -> HideWhat.MOVIE_GENRE
            else -> HideWhat.MOVIE
        }
    val person = users.firstOrNull { it.id == userId }
    return HiddenRow(
        id = id,
        name = genre.ifEmpty { name.ifEmpty { "TMDB $tmdbId" } },
        what = what,
        who =
            when {
                userId == 0L -> HideFor.EVERYONE
                person != null -> HideFor.PERSON
                else -> HideFor.REMOVED_PERSON
            },
        person = person?.name.orEmpty(),
        genre = isGenre,
    )
}

class HiddenPresenter internal constructor(
    private val api: Api,
    private val userId: Long?,
) {
    private val mutableState = MutableStateFlow(HiddenState())
    private val lock = Mutex()
    private var users: List<TitleUser> = emptyList()
    private var hides: List<TitleHide> = emptyList()

    val state: StateFlow<HiddenState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            if (users.isEmpty()) {
                users =
                    api
                        .result(TitleSupportEndpoints.users)
                        .valueOrNull()
                        ?.users
                        .orEmpty()
            }
            when (val result = api.result(DiscoverEndpoints.hidden, "user_id" to userId)) {
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                is ApiResult.Ok -> {
                    hides = result.value.hidden
                    mutableState.update { it.copy(status = LoadStatus.ready, rows = hides.map { h -> h.row(users) }) }
                }
            }
        }
    }

    suspend fun showAgain(id: Long): Boolean {
        mutableState.update { it.copy(actionError = null) }
        return when (val result = api.result(TitleSupportEndpoints.unhide, "id" to id)) {
            is ApiResult.Failed -> {
                mutableState.update { it.copy(actionError = result.error) }
                false
            }
            is ApiResult.Ok -> {
                hides = hides.filter { it.id != id }
                mutableState.update { it.copy(rows = hides.map { h -> h.row(users) }) }
                true
            }
        }
    }
}

data class HideSheetState(
    val status: LoadStatus = LoadStatus(),
    val name: String = "",
    val kind: CardKind = CardKind.MOVIE,
    val genres: List<String> = emptyList(),
    val hidden: List<HiddenRow> = emptyList(),
    val people: List<HidePerson> = emptyList(),
    val genre: String = "",
    val everyone: Boolean = true,
    val personId: Long = 0,
    val saving: Boolean = false,
    val needsPerson: Boolean = false,
    val error: ApiError? = null,
)

data class HideDone(
    val id: Long,
    val name: String,
    val everyone: Boolean,
    val person: String,
)

class HideSheetPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val tmdb: String,
    kind: CardKind,
    name: String,
    genres: List<String>,
) {
    private val known = if (kind == CardKind.SHOW) Genres.hideShows else Genres.hideMovies
    private val mutableState =
        MutableStateFlow(
            HideSheetState(
                name = name,
                kind = kind,
                genres = genres.filter { g -> known.any { it.equals(g, ignoreCase = true) } },
            ),
        )
    private val lock = Mutex()
    private var users: List<TitleUser> = emptyList()

    val state: StateFlow<HideSheetState> = mutableState.asStateFlow()

    suspend fun load() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            users =
                api
                    .result(TitleSupportEndpoints.users)
                    .valueOrNull()
                    ?.users
                    .orEmpty()
            val me = account()?.userId ?: 0
            when (val result = api.result(DiscoverEndpoints.hidden)) {
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                is ApiResult.Ok -> {
                    val current = mutableState.value
                    val mine =
                        result.value.hidden.filter { hide ->
                            hide.kind == current.kind.wire &&
                                (hide.tmdbId == tmdb || (hide.genre.isNotEmpty() && current.genres.any { it.equals(hide.genre, true) }))
                        }
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            hidden = mine.map { h -> h.row(users) },
                            people = users.filter { u -> u.id != me }.map { u -> HidePerson(u.id, u.name) },
                        )
                    }
                }
            }
        }
    }

    fun pickGenre(genre: String) {
        mutableState.update { it.copy(genre = genre) }
    }

    fun pickEveryone(everyone: Boolean) {
        mutableState.update { it.copy(everyone = everyone, needsPerson = false) }
    }

    fun pickPerson(id: Long) {
        mutableState.update { it.copy(personId = id, needsPerson = false) }
    }

    suspend fun hide(): HideDone? {
        val current = mutableState.value
        if (!current.everyone && current.personId == 0L) {
            mutableState.update { it.copy(needsPerson = true) }
            return null
        }
        mutableState.update { it.copy(saving = true, error = null) }
        val userId = if (current.everyone) null else current.personId
        val request =
            if (current.genre.isEmpty()) {
                HideBody(kind = current.kind.wire, tmdbId = tmdb, name = current.name.take(NAME_MAX), userId = userId)
            } else {
                HideBody(kind = current.kind.wire, genre = current.genre, userId = userId)
            }
        return when (val result = api.result(TitleSupportEndpoints.hide, request)) {
            is ApiResult.Failed -> {
                mutableState.update { it.copy(saving = false, error = result.error) }
                null
            }
            is ApiResult.Ok -> {
                mutableState.update { it.copy(saving = false) }
                HideDone(
                    id = result.value.id,
                    name = current.genre.ifEmpty { current.name },
                    everyone = current.everyone,
                    person = users.firstOrNull { it.id == current.personId }?.name.orEmpty(),
                )
            }
        }
    }

    suspend fun showAgain(id: Long): ApiError? =
        when (val result = api.result(TitleSupportEndpoints.unhide, "id" to id)) {
            is ApiResult.Failed -> result.error
            is ApiResult.Ok -> {
                mutableState.update { state -> state.copy(hidden = state.hidden.filter { it.id != id }) }
                null
            }
        }

    private companion object {
        const val NAME_MAX = 300
    }
}

data class RowStatLine(
    val rowType: String,
    val title: String,
    val shown: Int,
    val opens: Int,
    val asks: Int,
)

data class LayerStatLine(
    val layer: String,
    val shown: Int,
    val opens: Int,
    val asks: Int,
) {
    val rate: Double get() = if (shown > 0) (opens + asks).toDouble() / shown else -1.0
}

data class ExploreStatsState(
    val status: LoadStatus = LoadStatus(),
    val days: Int = 30,
    val rows: List<RowStatLine> = emptyList(),
    val layers: List<LayerStatLine> = emptyList(),
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty() && layers.isEmpty()
}

class ExploreStatsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(ExploreStatsState())

    val state: StateFlow<ExploreStatsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(DiscoverEndpoints.rowStats)) {
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            is ApiResult.Ok -> {
                val layers =
                    if (account()?.role == Role.OWNER) {
                        api
                            .result(DiscoverEndpoints.titleStats)
                            .valueOrNull()
                            ?.layers
                            .orEmpty()
                            .map { LayerStatLine(it.layer, it.shown, it.opens, it.asks) }
                    } else {
                        emptyList()
                    }
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        days = result.value.days,
                        rows = result.value.rows.map(ExploreRowStat::line),
                        layers = layers,
                    )
                }
            }
        }
    }
}

private fun ExploreRowStat.line() = RowStatLine(rowType, title, shown, opens, asks)
