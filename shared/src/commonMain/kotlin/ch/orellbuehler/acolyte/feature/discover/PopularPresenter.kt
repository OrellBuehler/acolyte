package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.model.DiscoverItem
import ch.orellbuehler.acolyte.api.model.DiscoverRow
import ch.orellbuehler.acolyte.api.model.PopularDay
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

enum class WhatKind {
    ON_SERVICE,
    OUT,
    COMING,
    OTHER,
}

data class RailEntry(
    val card: DiscoverCard,
    val what: WhatKind,
    val text: String,
    val alsoComing: Boolean,
)

data class RailDayEntry(
    val day: String,
    val offset: Int,
    val entries: List<RailEntry>,
)

data class PopularRow(
    val row: CardRow,
    val chips: List<ServiceChip> = emptyList(),
    val selected: Long = 0,
    val loading: Boolean = false,
)

data class PopularState(
    val status: LoadStatus = LoadStatus(),
    val type: ExploreType = ExploreType.ALL,
    val hero: DiscoverCard? = null,
    val heroPills: List<ExplorePill> = emptyList(),
    val heroRank: Int = 0,
    val heroService: String = "",
    val days: List<RailDayEntry> = emptyList(),
    val rows: List<PopularRow> = emptyList(),
    val building: Boolean = false,
    val more: Boolean = false,
    val loadingMore: Boolean = false,
    val sourceOff: Boolean = false,
    val canHide: Boolean = false,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty() && hero == null && days.isEmpty()
}

internal fun whatOf(item: DiscoverItem): Pair<WhatKind, String> {
    val what = item.what.trim()
    return when {
        what.startsWith("On ") -> WhatKind.ON_SERVICE to what.removePrefix("On ").trim()
        what == "Out" -> WhatKind.OUT to ""
        what == "Coming" -> WhatKind.COMING to ""
        else -> WhatKind.OTHER to what
    }
}

internal fun dayOffset(
    day: String,
    today: String,
): Int = (epochDay(day) ?: 0) - (epochDay(today) ?: 0)

internal fun epochDay(text: String): Int? {
    val parts = text.take(DATE_LENGTH).split('-')
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

private const val DATE_LENGTH = 10

class PopularPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val owned: OwnedLibrary,
    private val prefs: DiscoverPrefs,
    private val today: () -> String,
) {
    private val mutableState = MutableStateFlow(PopularState())
    private val lock = Mutex()
    private var rows: List<DiscoverRow> = emptyList()
    private var picked: Map<String, List<DiscoverItem>> = emptyMap()
    private var selected: Map<String, Long> = emptyMap()
    private var heroItems: List<DiscoverItem> = emptyList()
    private var rail: List<PopularDay> = emptyList()
    private var page = 0
    private var tries = 0
    private var heroAt = -1

    val state: StateFlow<PopularState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun select(type: ExploreType) {
        if (type == mutableState.value.type) return
        selected = emptyMap()
        picked = emptyMap()
        mutableState.value = PopularState(type = type)
        refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val type = mutableState.value.type
            mutableState.update { it.copy(status = it.status.started()) }
            val index = owned.get()
            when (val result = api.result(DiscoverEndpoints.popular, "type" to type.wire, "page" to 0)) {
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                is ApiResult.Ok -> {
                    val answer = result.value
                    rows = answer.rows
                    page = 1
                    heroItems = answer.hero
                    rail = answer.rail
                    if (heroAt < 0 && heroItems.isNotEmpty()) {
                        val visit = prefs.heroVisit
                        heroAt = visit % heroItems.size
                        prefs.heroVisit = visit + 1
                    }
                    mutableState.update {
                        it.copy(building = answer.building, more = answer.more, sourceOff = answer.sourceOff)
                    }
                    publish(index)
                    mutableState.update { it.copy(status = LoadStatus.ready) }
                }
            }
        }
    }

    suspend fun loadMore() {
        lock.withLock {
            val current = mutableState.value
            if (!current.more || current.loadingMore || current.status.phase != Phase.READY) return
            mutableState.update { it.copy(loadingMore = true) }
            when (val result = api.result(DiscoverEndpoints.popular, "type" to current.type.wire, "page" to page)) {
                is ApiResult.Ok -> {
                    val known = rows.map { it.id }.toSet()
                    rows = rows + result.value.rows.filter { it.id !in known }
                    page++
                    mutableState.update { it.copy(more = result.value.more && page <= MAX_PAGE, loadingMore = false) }
                    publish(owned.index.value)
                }
                is ApiResult.Failed -> mutableState.update { it.copy(loadingMore = false, more = false) }
            }
        }
    }

    suspend fun selectService(
        rowId: String,
        service: Long,
    ) {
        val row = rows.firstOrNull { it.id == rowId } ?: return
        selected = selected + (rowId to service)
        if (service == 0L) {
            picked = picked - rowId
            publish(owned.index.value)
            return
        }
        mutableState.update { state -> state.copy(rows = state.rows.map { if (it.row.id == rowId) it.copy(loading = true) else it }) }
        val kind =
            when (row.type) {
                "np_top_shows" -> "hot_shows"
                "np_top_movies" -> "hot_movies"
                else -> "services"
            }
        val answer =
            api
                .result(DiscoverEndpoints.popularService, "id" to service, "type" to mutableState.value.type.wire, "row" to kind)
                .valueOrNull()
        val items =
            answer
                ?.row
                ?.items
                ?.takeIf { it.isNotEmpty() }
                ?: row.items.filter { it.streaming?.service == service }
        picked = picked + (rowId to if (row.type.startsWith("np_top")) items.take(TOP_SIZE) else items)
        publish(owned.index.value)
    }

    suspend fun refreshOwned() {
        if (mutableState.value.status.phase == Phase.READY) publish(owned.refresh())
    }

    suspend fun watch() {
        while (true) {
            val current = mutableState.value
            val heroWait = current.status.phase == Phase.READY && current.hero == null && current.rows.isNotEmpty()
            val limit = if (current.building) BUILDING_TRIES else HERO_TRIES
            if (!(current.building || heroWait) || tries >= limit) return
            delay(POLL_DELAY)
            tries++
            refresh()
        }
    }

    private fun publish(index: OwnedIndex) {
        val rules = CardRules.of(account())
        val type = mutableState.value.type
        val heroItem = heroItems.getOrNull(heroAt.coerceAtLeast(0))
        val hero = heroItem?.card("hero", rules, index)?.takeIf { it.wideImage != null && type.keeps(it.kind) }
        val pills = mutableListOf<ExplorePill>()
        if (hero != null) {
            if (hero.fresh?.kind == FreshKind.NEW_SEASON) pills += ExplorePill.NEW_SEASON
            val rank = heroItem.streaming?.rank ?: 0
            if (rank in 1..TOP_SIZE) pills += ExplorePill.RANK
            if (heroItem.fresh != null &&
                heroItem.fresh.kind != "coming" &&
                ExplorePill.NEW_SEASON !in pills
            ) {
                pills += ExplorePill.NEW_THIS_WEEK
            }
        }
        val now = today()
        val days =
            rail
                .map { day ->
                    RailDayEntry(
                        day = day.day,
                        offset = dayOffset(day.day, now),
                        entries =
                            day.items
                                .map { item ->
                                    val (what, text) = whatOf(item)
                                    RailEntry(item.card("rail", rules, index), what, text, item.upcoming && item.inLibrary)
                                }.filter { type.keeps(it.card.kind) },
                    )
                }.filter { it.entries.isNotEmpty() }
        val list =
            rows
                .map { row ->
                    val base = row.cardRow(rules, index)
                    val chips =
                        if (row.type in CHIP_ROWS) {
                            row.items
                                .mapNotNull { it.streaming }
                                .filter { it.service > 0 }
                                .distinctBy { it.service }
                                .map { ServiceChip(it.service, it.name) }
                        } else {
                            emptyList()
                        }
                    val cards =
                        picked[row.id]?.map { it.card(row.type, rules, index, row.title) } ?: base.cards
                    PopularRow(
                        row = base.copy(cards = cards.filter { type.keeps(it.kind) }),
                        chips = if (chips.size > 1) chips else emptyList(),
                        selected = selected[row.id] ?: 0,
                    )
                }.filter { it.row.cards.isNotEmpty() || it.selected != 0L }
        mutableState.update {
            it.copy(
                hero = hero,
                heroPills = pills.take(2),
                heroRank = heroItem?.streaming?.rank ?: 0,
                heroService = heroItem?.streaming?.name.orEmpty(),
                days = days,
                rows = list,
                canHide = rules.canHide,
            )
        }
    }

    private companion object {
        const val MAX_PAGE = 20
        const val TOP_SIZE = 10
        const val BUILDING_TRIES = 20
        const val HERO_TRIES = 5
        const val POLL_DELAY = 3_000L
        val CHIP_ROWS = setOf("np_services", "np_top_shows", "np_top_movies")
    }
}
