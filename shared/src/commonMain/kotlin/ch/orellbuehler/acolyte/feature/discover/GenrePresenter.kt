package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.model.DiscoverItem
import ch.orellbuehler.acolyte.api.model.DiscoverRow
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

data class GenreState(
    val status: LoadStatus = LoadStatus(),
    val type: ExploreType = ExploreType.ALL,
    val genreId: Int = 0,
    val name: String = "",
    val hero: List<DiscoverCard> = emptyList(),
    val rows: List<CardRow> = emptyList(),
    val top: TopServices? = null,
    val partial: Boolean = false,
    val canHide: Boolean = false,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty() && hero.isEmpty()
}

class GenrePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val owned: OwnedLibrary,
    type: ExploreType,
    genre: Int,
    private val region: String,
) {
    private val mutableState =
        MutableStateFlow(GenreState(type = type, genreId = genre, name = Genres.named(genre, type)?.name.orEmpty()))
    private val lock = Mutex()
    private var rows: List<DiscoverRow> = emptyList()
    private var hero: List<DiscoverItem> = emptyList()
    private var chips: List<ServiceChip> = emptyList()
    private var topItems: List<DiscoverItem> = emptyList()
    private var tries = 0

    val state: StateFlow<GenreState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val current = mutableState.value
            mutableState.update { it.copy(status = it.status.started()) }
            val index = owned.get()
            when (val result = api.result(DiscoverEndpoints.genre, "type" to current.type.wire, "genre" to current.genreId)) {
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(status = it.status.failed(result.error)) }
                    return
                }
                is ApiResult.Ok -> {
                    rows = result.value.rows
                    hero = result.value.hero
                    mutableState.update {
                        it.copy(
                            name =
                                result.value.genre.name
                                    .ifEmpty { it.name },
                            partial = result.value.partial,
                        )
                    }
                }
            }
            if (chips.isEmpty()) {
                chips =
                    api
                        .result(DiscoverEndpoints.serviceTop, "type" to current.type.wire, "region" to region)
                        .valueOrNull()
                        ?.services
                        .orEmpty()
                        .map { ServiceChip(it.service, it.name) }
            }
            val chip = mutableState.value.top?.selected ?: chips.firstOrNull()?.id ?: 0
            if (chip > 0) loadTop(chip)
            publish(index)
            mutableState.update { it.copy(status = LoadStatus.ready) }
        }
    }

    suspend fun selectService(id: Long) {
        lock.withLock {
            loadTop(id)
            publish(owned.index.value)
        }
    }

    suspend fun refreshOwned() {
        if (mutableState.value.status.phase == Phase.READY) publish(owned.refresh())
    }

    suspend fun watch() {
        while (mutableState.value.partial && tries < PARTIAL_TRIES) {
            delay(PARTIAL_DELAY)
            tries++
            refresh()
        }
    }

    private suspend fun loadTop(chip: Long) {
        val current = mutableState.value
        val type = if (current.type == ExploreType.ALL) "all" else current.type.wire
        topItems =
            api
                .result(
                    DiscoverEndpoints.top,
                    "type" to type,
                    "chip" to chip,
                    "region" to region,
                    "genre" to current.genreId,
                ).valueOrNull()
                ?.items
                .orEmpty()
        val name = chips.firstOrNull { it.id == chip }?.name.orEmpty()
        mutableState.update {
            it.copy(top = TopServices(chips = chips, selected = chip, serviceName = name, source = TopSource.POPULAR))
        }
    }

    private fun publish(index: OwnedIndex) {
        val rules = CardRules.of(account())
        val heroCards = hero.map { it.card("hero", rules, index) }.filter { it.wideImage != null }
        val cards = rows.map { it.cardRow(rules, index) }.filter { it.cards.isNotEmpty() }
        val topCards = topItems.map { it.card("top_services", rules, index) }
        val top = mutableState.value.top?.copy(cards = topCards)
        val list = cards.toMutableList()
        if (top != null && topCards.isNotEmpty()) {
            list.add(minOf(1, list.size), CardRow("top_services", "top_services", "", cards = topCards, seeAll = true))
        }
        mutableState.update { it.copy(hero = heroCards, rows = list, top = top, canHide = rules.canHide) }
    }

    private companion object {
        const val PARTIAL_TRIES = 6
        const val PARTIAL_DELAY = 1_000L
    }
}
