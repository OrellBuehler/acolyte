package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.api.model.DiscoverItem
import ch.orellbuehler.acolyte.api.model.DiscoverRow
import ch.orellbuehler.acolyte.api.model.ExploreCounts
import ch.orellbuehler.acolyte.api.model.ExploreStripCounts
import ch.orellbuehler.acolyte.api.model.GenreCard
import ch.orellbuehler.acolyte.api.model.ServiceTop
import ch.orellbuehler.acolyte.api.model.ServiceTopAnswer
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ExplorePill {
    NEW_SEASON,
    PICKED_FOR_YOU,
    NEW_THIS_WEEK,
    RANK,
}

data class ServiceChip(
    val id: Long,
    val name: String,
)

enum class TopSource {
    TODAY,
    AS_OF,
    POPULAR,
}

data class TopServices(
    val chips: List<ServiceChip> = emptyList(),
    val selected: Long = 0,
    val cards: List<DiscoverCard> = emptyList(),
    val ranked: Boolean = false,
    val source: TopSource = TopSource.POPULAR,
    val serviceName: String = "",
    val fetchedAt: String = "",
    val attribution: String = "",
)

data class GenreBanner(
    val genre: Genre,
    val both: Boolean,
    val have: Int,
    val picks: List<DiscoverCard>,
)

data class ExploreState(
    val status: LoadStatus = LoadStatus(),
    val type: ExploreType = ExploreType.ALL,
    val hero: List<DiscoverCard> = emptyList(),
    val heroPills: List<ExplorePill> = emptyList(),
    val banner: GenreBanner? = null,
    val strip: List<BrowseTile> = emptyList(),
    val rows: List<CardRow> = emptyList(),
    val top: TopServices? = null,
    val genres: List<GenreCard> = emptyList(),
    val building: Boolean = false,
    val more: Boolean = false,
    val loadingMore: Boolean = false,
    val sourceOff: Boolean = false,
    val canHide: Boolean = false,
    val canSeeStats: Boolean = false,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty() && hero.isEmpty()
}

internal fun ServiceTopAnswer.topServices(
    selected: Long,
    rules: CardRules,
    owned: OwnedIndex,
    type: ExploreType,
): TopServices? {
    val lists = services.filter { it.items.isNotEmpty() }
    if (lists.isEmpty()) return null
    val pick = lists.firstOrNull { it.service == selected } ?: lists.first()
    return TopServices(
        chips = lists.map { ServiceChip(it.service, it.name) },
        selected = pick.service,
        cards = pick.items.map { it.card("top_services", rules, owned) }.filter { type.keeps(it.kind) },
        ranked = pick.ranked,
        source = pick.sourceOf(),
        serviceName = pick.name,
        fetchedAt = pick.fetchedAt,
        attribution = attribution,
    )
}

private fun ServiceTop.sourceOf(): TopSource =
    when {
        !ranked -> TopSource.POPULAR
        ageSeconds > DAY_SECONDS -> TopSource.AS_OF
        else -> TopSource.TODAY
    }

private const val DAY_SECONDS = 86_400L

internal fun favouriteGenre(
    owned: OwnedIndex,
    type: ExploreType,
): Pair<Genre, Int>? {
    val counts = mutableMapOf<String, MutableMap<CardKind, Int>>()
    for (item in owned.items) {
        val kind =
            when (item.kind) {
                "movie" -> CardKind.MOVIE
                "show" -> CardKind.SHOW
                else -> continue
            }
        if (!type.keeps(kind)) continue
        for (name in item.genres.distinct()) {
            val sides = counts.getOrPut(name.lowercase()) { mutableMapOf() }
            sides[kind] = (sides[kind] ?: 0) + 1
        }
    }
    val best = counts.maxByOrNull { (_, sides) -> sides.values.sum() } ?: return null
    val total = best.value.values.sum()
    if (total < 2) return null
    val kind = if ((best.value[CardKind.SHOW] ?: 0) > (best.value[CardKind.MOVIE] ?: 0)) CardKind.SHOW else CardKind.MOVIE
    val list = if (kind == CardKind.SHOW) Genres.shows else Genres.movies
    val genre = list.firstOrNull { it.name.lowercase() == best.key } ?: return null
    return genre to total
}

class ExplorePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val owned: OwnedLibrary,
    private val prefs: DiscoverPrefs,
    private val region: String,
    private val timeZone: String,
) {
    private val mutableState = MutableStateFlow(ExploreState(type = prefs.exploreType))
    private val lock = Mutex()
    private var head: List<DiscoverRow> = emptyList()
    private var tail: List<DiscoverRow> = emptyList()
    private var heroItems: List<DiscoverItem> = emptyList()
    private var taste: List<String> = emptyList()
    private var tops: ServiceTopAnswer? = null
    private var newServices: DiscoverRow? = null
    private var page = 0
    private var buildingTries = 0
    private var stripShown = false

    val state: StateFlow<ExploreState> = mutableState.asStateFlow()

    private val rules: CardRules get() = CardRules.of(account())

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun select(type: ExploreType) {
        if (type == mutableState.value.type) return
        prefs.exploreType = type
        mutableState.value = ExploreState(type = type)
        refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val type = mutableState.value.type
            mutableState.update { it.copy(status = it.status.started()) }
            val index = owned.get()
            when (val result = api.result(DiscoverEndpoints.head, *query(type))) {
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(status = it.status.failed(result.error)) }
                    return
                }
                is ApiResult.Ok -> {
                    val answer = result.value
                    head = answer.head.orEmpty()
                    tail = emptyList()
                    page = 0
                    heroItems = answer.hero.orEmpty()
                    taste = answer.taste.orEmpty()
                    mutableState.update {
                        it.copy(building = answer.building, more = answer.tailRows > 0, sourceOff = answer.sourceOff)
                    }
                }
            }
            coroutineScope {
                val top = async { api.result(DiscoverEndpoints.serviceTop, "type" to type.wire, "region" to region).valueOrNull() }
                val fresh = async { api.result(DiscoverEndpoints.newServices, "type" to type.wire).valueOrNull()?.row }
                val genres =
                    async {
                        api
                            .result(DiscoverEndpoints.genreRow, "type" to type.wire)
                            .valueOrNull()
                            ?.genres
                            .orEmpty()
                    }
                val strip =
                    async {
                        val titles = head.map { it.title.trim().take(MAX_ROW_TITLE) }.filter { it.isNotEmpty() }.take(MAX_ROW_TITLES)
                        api
                            .result(DiscoverEndpoints.strip, "type" to type.wire, "row" to titles, "tz" to timeZone)
                            .valueOrNull()
                            ?.tiles
                            .orEmpty()
                    }
                tops = top.await()
                newServices = fresh.await()
                val genreCards = genres.await()
                val tiles = strip.await()
                mutableState.update {
                    it.copy(genres = if (genreCards.size >= GENRE_ROW_MIN) genreCards.take(GENRE_ROW_MAX) else emptyList(), strip = tiles)
                }
            }
            publish(index)
            mutableState.update { it.copy(status = LoadStatus.ready) }
            loadBanner(type, index)
        }
    }

    suspend fun loadMore() {
        lock.withLock {
            val current = mutableState.value
            if (!current.more || current.loadingMore || current.status.phase != Phase.READY) return
            mutableState.update { it.copy(loadingMore = true) }
            when (val result = api.result(DiscoverEndpoints.tail, *query(current.type), "page" to page)) {
                is ApiResult.Ok -> {
                    val known = (head + tail).map { it.id }.toSet()
                    tail = tail + result.value.rows.filter { it.id !in known }
                    page++
                    mutableState.update { it.copy(more = result.value.more && page <= MAX_TAIL_PAGE, loadingMore = false) }
                    publish(owned.index.value)
                }
                is ApiResult.Failed -> mutableState.update { it.copy(loadingMore = false, more = false) }
            }
        }
    }

    suspend fun selectService(id: Long) {
        val top = mutableState.value.top ?: return
        if (top.selected == id) return
        mutableState.update { it.copy(top = top.copy(selected = id)) }
        publish(owned.index.value)
    }

    suspend fun refreshOwned() {
        if (mutableState.value.status.phase != Phase.READY) return
        publish(owned.refresh())
    }

    suspend fun stripShown() {
        if (stripShown) return
        val keys =
            mutableState.value.strip
                .map(::stripKey)
                .filter { it.isNotEmpty() }
                .distinct()
                .take(MAX_ROW_TITLES)
        if (keys.isEmpty()) return
        stripShown = true
        api.result(DiscoverEndpoints.stripCounts, ExploreStripCounts("shown", keys))
    }

    suspend fun stripOpened(tile: BrowseTile) = stripOpenedKey(stripKey(tile))

    suspend fun stripOpenedKey(key: String) {
        if (key.isEmpty()) return
        api.result(DiscoverEndpoints.stripCounts, ExploreStripCounts("open", listOf(key)))
    }

    suspend fun opened(card: DiscoverCard) = count(card.rowType, "open")

    suspend fun asking(card: DiscoverCard) = count(card.rowType, "ask")

    suspend fun openedFrom(rowType: String) = count(rowType, "open")

    suspend fun askingFrom(rowType: String) = count(rowType, "ask")

    suspend fun resetRecommendations(): ApiError? =
        when (val result = api.result(DiscoverEndpoints.resetStripCounts)) {
            is ApiResult.Failed -> result.error
            is ApiResult.Ok -> {
                stripShown = false
                null
            }
        }

    suspend fun watch() {
        while (true) {
            val current = mutableState.value
            val waiting = current.building || (current.status.phase == Phase.READY && current.hero.isEmpty() && current.rows.isNotEmpty())
            if (!waiting || buildingTries >= BUILDING_TRIES) return
            delay(if (current.building) BUILDING_DELAY else HERO_DELAY)
            buildingTries++
            refresh()
        }
    }

    private suspend fun count(
        type: String,
        event: String,
    ) {
        if (type !in COUNTED) return
        val layer =
            mutableState.value.rows
                .firstOrNull { it.type == type }
                ?.layer
                ?.takeIf { it in LAYERS }
        api.result(DiscoverEndpoints.counts, ExploreCounts(type, event, layer))
    }

    private fun query(type: ExploreType): Array<Pair<String, Any?>> =
        arrayOf("type" to type.wire, "tz" to timeZone, "services" to "", "region" to region)

    private fun publish(index: OwnedIndex) {
        val current = mutableState.value
        val type = current.type
        val rules = rules
        val hero =
            heroItems
                .map { it.card("hero", rules, index) }
                .filter { type.keeps(it.kind) && it.wideImage != null }
        val top = tops?.topServices(current.top?.selected ?: 0, rules, index, type)
        val heroKeys = hero.map(::identity).toSet()
        val topKeys =
            top
                ?.cards
                ?.map(::identity)
                ?.toSet()
                .orEmpty()
        val rows =
            (head + tail)
                .map { it.cardRow(rules, index) }
                .map { row ->
                    if (row.type ==
                        "top_services"
                    ) {
                        row.copy(cards = top?.cards.orEmpty(), ranked = top?.ranked == true)
                    } else {
                        row
                    }
                }.map { row -> if (row.type == "top_services") row else row.copy(cards = row.cards.filter { type.keeps(it.kind) }) }
                .filter { it.cards.isNotEmpty() }
        val (topRows, others) = rows.partition { it.type == "top_services" }
        val deduped = enoughCards(dedupeTop(others, heroKeys + topKeys))
        val ordered = mutableListOf<CardRow>()
        ordered += deduped
        val topAt = rows.indexOfFirst { it.type == "top_services" }.coerceAtLeast(0).coerceAtMost(ordered.size)
        topRows.firstOrNull()?.let { ordered.add(topAt, it.copy(seeAll = true)) }
        newServices?.let { row ->
            val cards =
                row.items
                    .map { it.card("np_services", rules, index, row.title) }
                    .filter { type.keeps(it.kind) && identity(it) !in heroKeys && identity(it) !in topKeys }
            if (cards.size >= NEW_ROW_MIN) {
                val at = ordered.indexOfFirst { it.type == "top_services" }.let { if (it < 0) minOf(1, ordered.size) else it + 1 }
                ordered.add(at, CardRow(row.id.ifEmpty { row.type }, "np_services", row.title, cards = cards, seeAll = true))
            }
        }
        if (current.genres.isNotEmpty()) {
            var at = minOf(GENRE_ROW_AT, ordered.size)
            while (at < ordered.size &&
                (ordered.getOrNull(at - 1)?.type?.contains("genre") == true || ordered[at].type.contains("genre"))
            ) {
                at++
            }
            ordered.add(at, CardRow("genre_browse", "genre_browse", ""))
        }
        val first = hero.firstOrNull()
        val pills = mutableListOf<ExplorePill>()
        if (first != null) {
            if (first.kind == CardKind.SHOW && first.fresh?.kind == FreshKind.NEW_SEASON) pills += ExplorePill.NEW_SEASON
            val need = minOf(2, taste.size)
            if (need > 0 &&
                first.genres.count { g -> taste.any { it.equals(g, ignoreCase = true) } } >= need
            ) {
                pills += ExplorePill.PICKED_FOR_YOU
            }
        }
        val account = account()
        mutableState.update {
            it.copy(
                hero = hero,
                heroPills = pills.take(2),
                rows = ordered,
                top = top,
                canHide = rules.canHide,
                canSeeStats = account?.can(Need.USERS) == true,
            )
        }
    }

    private suspend fun loadBanner(
        type: ExploreType,
        index: OwnedIndex,
    ) {
        val (genre, have) =
            favouriteGenre(index, type) ?: run {
                mutableState.update { it.copy(banner = null) }
                return
            }
        val kindType = if (genre.kind == CardKind.SHOW) ExploreType.SHOWS else ExploreType.MOVIES
        val answer = api.result(DiscoverEndpoints.genre, "type" to kindType.wire, "genre" to genre.id).valueOrNull() ?: return
        val rules = rules
        val seen = mutableSetOf<String>()
        val picks =
            (answer.hero + answer.rows.flatMap { it.items })
                .map { it.card("genre", rules, index) }
                .filter { it.kind != CardKind.ARTIST && it.poster != null && it.mark == null && it.titleId == 0L }
                .filter { seen.add(identity(it)) }
                .take(BANNER_PICKS)
        mutableState.update {
            it.copy(banner = if (picks.size >= BANNER_MIN) GenreBanner(genre, genre.id in Genres.mergedUnderAll, have, picks) else null)
        }
    }

    internal companion object {
        const val MAX_ROW_TITLE = 80
        const val MAX_ROW_TITLES = 40
        const val GENRE_ROW_MIN = 8
        const val GENRE_ROW_MAX = 16
        const val GENRE_ROW_AT = 6
        const val NEW_ROW_MIN = 6
        const val BANNER_MIN = 5
        const val BANNER_PICKS = 7
        const val BUILDING_TRIES = 24
        const val BUILDING_DELAY = 5_000L
        const val HERO_DELAY = 3_000L
        const val MAX_TAIL_PAGE = 100

        val COUNTED =
            setOf(
                "top_picks",
                "trending",
                "because_have",
                "because_asked",
                "person",
                "genre",
                "popular_genre",
                "popular_decade",
                "decade",
                "new_services",
                "best",
                "household",
                "hidden",
                "top_services",
                "collection",
                "cinemas",
                "new_seasons",
                "returning",
                "mood",
                "classics",
                "hero",
            )
        val LAYERS = setOf("occasion", "time", "genre", "taste", "reason", "plain", "fixed")

        fun stripKey(tile: BrowseTile): String = tile.category.ifEmpty { tile.name.lowercase() }
    }
}
