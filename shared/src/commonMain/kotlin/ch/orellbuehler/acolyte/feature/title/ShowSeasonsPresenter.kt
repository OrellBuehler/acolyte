package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.KeepBody
import ch.orellbuehler.acolyte.api.model.OwnedEpisode
import ch.orellbuehler.acolyte.api.model.TitleEvent
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SeasonEpisodes(
    val season: Int,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val episodes: List<OwnedEpisode> = emptyList(),
)

data class ShowSeasonsState(
    val manage: Boolean = false,
    val open: Set<Int> = emptySet(),
    val seasons: Map<Int, SeasonEpisodes> = emptyMap(),
    val outside: List<OwnedEpisode> = emptyList(),
    val belowOnly: Boolean = false,
    val busy: String = "",
    val packBusy: Boolean = false,
    val history: List<TitleEvent> = emptyList(),
    val historyLoaded: Boolean = false,
    val feedback: Feedback? = null,
) {
    val loaded: List<SeasonEpisodes> get() = seasons.values.toList()

    val openSeasons: List<Int> get() = open.toList()

    fun find(episodeId: Long): OwnedEpisode? =
        seasons.values.firstNotNullOfOrNull { s -> s.episodes.firstOrNull { it.id == episodeId } }
            ?: outside.firstOrNull { it.id == episodeId }

    fun episodesOf(season: Int): List<OwnedEpisode> {
        val all = seasons[season]?.episodes.orEmpty()
        return if (belowOnly) all.filter { below(it) } else all
    }

    val belowCount: Int get() = seasons.values.sumOf { s -> s.episodes.count { below(it) } }

    private fun below(e: OwnedEpisode): Boolean = e.slots.any { (it.file != null || it.fileId > 0) && !it.goodEnough }

    fun seasonFiles(season: Int): List<OwnedEpisode> = seasons[season]?.episodes.orEmpty().filter { e -> e.slots.any { it.file != null } }

    fun seasonHistory(season: Int): List<TitleEvent> {
        val code = "S" + season.toString().padStart(2, '0') + "E"
        return history.filter { it.episode.startsWith(code, ignoreCase = true) }
    }

    fun seasonSize(season: Int): Long = seasonFiles(season).sumOf { e -> e.slots.sumOf { it.file?.size ?: 0 } }
}

class ShowSeasonsPresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    manage: Boolean,
    private val onChanged: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(ShowSeasonsState(manage = manage))
    private val feedback = FeedbackCounter()

    val state: StateFlow<ShowSeasonsState> = mutableState.asStateFlow()

    suspend fun toggle(season: Int) {
        val open = season in mutableState.value.open
        mutableState.update { it.copy(open = if (open) it.open - season else it.open + season) }
        if (!open) ensure(season)
    }

    suspend fun expandAll(seasons: List<Int>) {
        mutableState.update { it.copy(open = seasons.toSet()) }
        seasons.forEach { ensure(it) }
    }

    fun collapseAll() {
        mutableState.update { it.copy(open = emptySet()) }
    }

    fun toggleBelow() {
        mutableState.update { it.copy(belowOnly = !it.belowOnly) }
    }

    suspend fun ensure(season: Int) {
        val known = mutableState.value.seasons[season]
        if (known != null && !known.failed) return
        load(season)
    }

    suspend fun load(season: Int) {
        mutableState.update { it.copy(seasons = it.seasons + (season to SeasonEpisodes(season, loading = true))) }
        val points = if (mutableState.value.manage) 1 else null
        when (val result = api.result(LibraryEndpoints.episodes, "id" to titleId, "season" to season, "files" to 1, "points" to points)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        seasons =
                            it.seasons +
                                (season to SeasonEpisodes(season, episodes = result.value.episodes.filter { e -> !e.outsideOrder })),
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(seasons = it.seasons + (season to SeasonEpisodes(season, failed = true))) }
        }
    }

    suspend fun reset(grouped: Boolean) {
        val open = mutableState.value.open
        mutableState.update { it.copy(seasons = emptyMap(), outside = emptyList()) }
        open.forEach { load(it) }
        if (grouped) loadOutside()
    }

    suspend fun loadOutside() {
        api.result(LibraryEndpoints.episodes, "id" to titleId, "files" to 1).onSuccess { all ->
            mutableState.update { it.copy(outside = all.episodes.filter { e -> e.outsideOrder }) }
        }
    }

    suspend fun keepSeason(
        season: Int,
        keep: Boolean,
    ) {
        busy("season-$season") {
            api
                .result(TitleEndpoints.keepSeason, KeepBody(keep), "id" to titleId, "number" to season)
                .onFailure { say(feedback.failed(it)) }
        }
        if (season in mutableState.value.seasons) load(season)
        onChanged()
    }

    suspend fun keepEpisode(
        episode: OwnedEpisode,
        keep: Boolean,
    ) {
        busy("episode-${episode.id}") {
            api
                .result(TitleEndpoints.keepEpisode, KeepBody(keep), "id" to episode.id)
                .onSuccess { updated ->
                    mutableState.update { st ->
                        val s = st.seasons[episode.season] ?: return@update st
                        val list = s.episodes.map { if (it.id == episode.id) it.copy(keepUpToDate = updated.keepUpToDate) else it }
                        st.copy(seasons = st.seasons + (episode.season to s.copy(episodes = list)))
                    }
                }.onFailure { say(feedback.failed(it)) }
        }
    }

    suspend fun packNext(season: Int) {
        mutableState.update { it.copy(packBusy = true) }
        api
            .result(TitleEndpoints.packNext, "id" to titleId, "number" to season)
            .onSuccess { say(feedback.next(FeedbackKind.PACK_NEXT)) }
            .onFailure { say(feedback.failed(it)) }
        mutableState.update { it.copy(packBusy = false) }
        load(season)
    }

    suspend fun loadHistory() {
        val events =
            api
                .result(
                    TitleSupportEndpoints.events,
                    "title" to titleId,
                    "limit" to 50,
                ).valueOrNull()
                ?.events
                ?.map { it.toTitleEvent() }
        mutableState.update { it.copy(history = events.orEmpty().filter { e -> e.kind !in TitleState.routineKinds }, historyLoaded = true) }
    }

    private inline fun busy(
        key: String,
        block: () -> Unit,
    ) {
        mutableState.update { it.copy(busy = key) }
        try {
            block()
        } finally {
            mutableState.update { if (it.busy == key) it.copy(busy = "") else it }
        }
    }

    private fun say(next: Feedback) {
        mutableState.update { it.copy(feedback = next) }
    }
}
