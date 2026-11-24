package ch.orellbuehler.acolyte.ui.discover

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.PopularRow
import ch.orellbuehler.acolyte.feature.discover.PopularState
import ch.orellbuehler.acolyte.feature.discover.RailDayEntry
import ch.orellbuehler.acolyte.feature.discover.RailEntry
import ch.orellbuehler.acolyte.feature.discover.WhatKind
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.RankedPoster
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.TimeZone

class PopularViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.popular(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000)

    val state: StateFlow<PopularState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun select(type: ExploreType) {
        viewModelScope.launch { presenter.select(type) }
    }

    fun loadMore() {
        viewModelScope.launch { presenter.loadMore() }
    }

    fun selectService(
        row: String,
        id: Long,
    ) {
        viewModelScope.launch { presenter.selectService(row, id) }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()

    suspend fun watch() = presenter.watch()
}

@Composable
fun PopularScreen(
    nav: DiscoverNav,
    onBack: (() -> Unit)?,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { PopularViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshOwned()
            viewModel.watch()
        }
    }
    ScreenScaffold(title = stringResource(R.string.section_popular), onBack = onBack) { padding ->
        CardHost(nav, state.canHide, viewModel::refresh, Modifier.padding(padding)) { events ->
            StatusContent(
                status = state.status,
                isEmpty = state.isEmpty,
                onRefresh = viewModel::refresh,
                empty = {
                    MessageView(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.discover_popular_building_title),
                        message = stringResource(R.string.discover_explore_building_message),
                        action = stringResource(R.string.section_explore),
                        onAction = { nav.openSection(Section.EXPLORE) },
                    )
                },
            ) {
                val list = rememberLazyListState()
                LoadMoreEffect(list, state.more && !state.loadingMore) { viewModel.loadMore() }
                LazyColumn(state = list, contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                    item(key = "types") { TypeBar(state.type, viewModel::select, onGenre = null) }
                    state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                    if (state.sourceOff) item(key = "off") { NoticeBanner(stringResource(R.string.discover_source_off)) }
                    state.hero?.let { hero ->
                        item(key = "hero") {
                            val pills = state.heroPills.map { heroPillText(it, state.heroRank, state.heroService) }
                            HeroCard(hero, pills, events, Modifier.padding(vertical = Spacing.Small))
                        }
                    }
                    if (state.days.isNotEmpty()) {
                        item(key = "week") {
                            Text(
                                stringResource(R.string.discover_this_week),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small).semantics { heading() },
                            )
                        }
                        items(state.days, key = { "day-" + it.day }) { day -> DayView(day, events) }
                    }
                    items(state.rows, key = { "row-" + it.row.id }) { row -> PopularRowView(row, events, viewModel::selectService) }
                    if (state.loadingMore) {
                        item(key = "more") {
                            Row(Modifier.fillMaxWidth().padding(Spacing.Large), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayView(
    day: RailDayEntry,
    events: CardEvents,
) {
    val date = runCatching { LocalDate.parse(day.day) }.getOrNull()
    val word =
        when (day.offset) {
            0 -> stringResource(R.string.discover_day_today)
            -1 -> stringResource(R.string.discover_day_yesterday)
            1 -> stringResource(R.string.discover_day_tomorrow)
            else -> date?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: day.day
        }
    val small = date?.let { shortDate(it) }.orEmpty()
    CardShelf(title = "$word · $small") {
        items(day.entries, key = { day.day + it.card.key }) { entry -> TitleWideCard(entry.card, events, subtitle = whatText(entry)) }
    }
}

@Composable
private fun whatText(entry: RailEntry): String {
    val base =
        when (entry.what) {
            WhatKind.ON_SERVICE -> stringResource(R.string.discover_on_service, entry.text)
            WhatKind.OUT -> stringResource(R.string.discover_what_out)
            WhatKind.COMING -> stringResource(R.string.discover_what_coming)
            WhatKind.OTHER -> entry.text
        }
    return if (entry.alsoComing) "$base · ${stringResource(R.string.discover_what_coming)}" else base
}

@Composable
private fun PopularRowView(
    row: PopularRow,
    events: CardEvents,
    onService: (String, Long) -> Unit,
) {
    CardShelf(
        title = row.row.title,
        subtitle = row.row.subtitle,
        header = { if (row.chips.isNotEmpty()) ServiceChips(row.chips, row.selected, { onService(row.row.id, it) }, showAll = true) },
    ) {
        if (row.loading) {
            item { CircularProgressIndicator(Modifier.padding(Spacing.Large)) }
        } else if (row.row.ranked) {
            itemsIndexed(row.row.cards, key = { _, card -> card.key }) { index, card ->
                RankedPoster(if (card.rank > 0) card.rank else index + 1) { TitlePosterCard(card, events) }
            }
        } else {
            items(row.row.cards, key = { it.key }) { card ->
                TitleWideCard(
                    card,
                    events,
                    subtitle = if (card.streaming.isNotEmpty()) stringResource(R.string.discover_on_service, card.streaming) else null,
                )
            }
        }
    }
}

fun shortDate(date: LocalDate): String {
    val locale = Locale.getDefault()
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale))
}
