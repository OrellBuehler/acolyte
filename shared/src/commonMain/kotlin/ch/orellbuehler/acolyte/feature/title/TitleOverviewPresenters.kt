package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.TitleAnime
import ch.orellbuehler.acolyte.api.model.TitleReview
import ch.orellbuehler.acolyte.api.model.TitleReviews
import ch.orellbuehler.acolyte.api.model.TitleSeasonBrief
import ch.orellbuehler.acolyte.api.model.TitleSeasonEpisode
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.feature.LoadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SeasonBrowserState(
    val seasons: List<TitleSeasonBrief> = emptyList(),
    val selected: Int = 0,
    val episodes: Map<Int, List<TitleSeasonEpisode>> = emptyMap(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val anime: TitleAnime? = null,
    val newSeason: Int? = null,
) {
    val ordered: List<TitleSeasonBrief> get() =
        TitleLogic.seasonOrder(seasons.map { it.number }).mapNotNull { n ->
            seasons.firstOrNull {
                it.number ==
                    n
            }
        }

    val current: TitleSeasonBrief? get() = seasons.firstOrNull { it.number == selected }

    val shown: List<TitleSeasonEpisode> get() = episodes[selected].orEmpty()

    fun absolute(episode: Int): Int {
        val from =
            anime
                ?.takeIf { it.anime }
                ?.seasons
                ?.firstOrNull { it.number == selected }
                ?.absoluteFrom ?: 0
        return if (from > 0) from + episode - 1 else 0
    }
}

class SeasonBrowserPresenter internal constructor(
    private val api: Api,
    private val tmdb: String,
    seasons: List<TitleSeasonBrief>,
    thisYear: Int,
) {
    private val mutableState =
        MutableStateFlow(
            run {
                val briefs = seasons.map { SeasonBrief(number = it.number, episodes = it.episodeCount, year = it.year) }
                val newest = TitleLogic.newestSeason(briefs) ?: 0
                val fresh = seasons.firstOrNull { it.number == newest }?.takeIf { it.number > 0 && it.year >= thisYear - 1 }?.number
                SeasonBrowserState(seasons = seasons, selected = newest, newSeason = fresh)
            },
        )

    val state: StateFlow<SeasonBrowserState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.anime == null) {
            api.result(TitleEndpoints.anime, "kind" to "show", "tmdb_id" to tmdb).onSuccess { v ->
                mutableState.update { it.copy(anime = v) }
            }
        }
        select(mutableState.value.selected)
    }

    suspend fun select(season: Int) {
        mutableState.update { it.copy(selected = season, failed = false) }
        if (mutableState.value.episodes.containsKey(season) || mutableState.value.seasons.isEmpty()) return
        mutableState.update { it.copy(loading = true) }
        when (val result = api.result(TitleEndpoints.season, "tmdb_id" to tmdb, "number" to season)) {
            is ApiResult.Ok -> mutableState.update { it.copy(loading = false, episodes = it.episodes + (season to result.value.episodes)) }
            is ApiResult.Failed ->
                mutableState.update {
                    if (it.selected ==
                        season
                    ) {
                        it.copy(loading = false, failed = true)
                    } else {
                        it.copy(loading = false)
                    }
                }
        }
    }
}

data class ReviewsState(
    val status: LoadStatus = LoadStatus(),
    val data: TitleReviews = TitleReviews(),
    val reviews: List<TitleReview> = emptyList(),
    val source: String = "tmdb",
    val order: String = "newest",
    val loadingMore: Boolean = false,
    val everHad: Boolean = false,
) {
    val visible: Boolean get() = reviews.isNotEmpty() || (everHad && data.sources.size > 1)

    val canHelpful: Boolean get() =
        data.sources
            .firstOrNull { it.id == source }
            ?.orders
            ?.contains("helpful") == true

    val hasMore: Boolean get() = data.page < data.pages

    val spread: List<Int> get() = if (data.distribution.size == 10) data.distribution.reversed() else emptyList()
}

class ReviewsPresenter internal constructor(
    private val api: Api,
    private val kind: String,
    private val tmdb: String,
) {
    private val mutableState = MutableStateFlow(ReviewsState())

    val state: StateFlow<ReviewsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase == ch.orellbuehler.acolyte.feature.Phase.READY) return
        fetch(1, append = false)
    }

    suspend fun setSource(source: String) {
        mutableState.update { it.copy(source = source, order = "newest") }
        fetch(1, append = false)
    }

    suspend fun setOrder(order: String) {
        mutableState.update { it.copy(order = order) }
        fetch(1, append = false)
    }

    suspend fun more() {
        val s = mutableState.value
        if (!s.hasMore || s.loadingMore) return
        mutableState.update { it.copy(loadingMore = true) }
        fetch(s.data.page + 1, append = true)
    }

    private suspend fun fetch(
        page: Int,
        append: Boolean,
    ) {
        val s = mutableState.value
        if (!append) mutableState.update { it.copy(status = it.status.started()) }
        val result =
            api.result(
                TitleEndpoints.reviews,
                "kind" to kind,
                "tmdb_id" to tmdb,
                "source" to s.source,
                "page" to page,
                "order" to s.order.takeIf { it != "newest" },
            )
        when (result) {
            is ApiResult.Ok -> {
                val v = result.value
                mutableState.update {
                    val list = if (append) it.reviews + v.reviews else v.reviews
                    it.copy(
                        status = LoadStatus.ready,
                        data = v,
                        reviews = list,
                        loadingMore = false,
                        everHad =
                            it.everHad || list.isNotEmpty(),
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error), loadingMore = false) }
        }
    }
}
