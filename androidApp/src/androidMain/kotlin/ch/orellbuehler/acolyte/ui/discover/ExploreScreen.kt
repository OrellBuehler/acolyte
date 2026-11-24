package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowseTarget
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.ExploreState
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.GenreBanner
import ch.orellbuehler.acolyte.feature.discover.Genres
import ch.orellbuehler.acolyte.feature.discover.browseLink
import ch.orellbuehler.acolyte.feature.discover.tileHue
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.BrowseTileCard
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone

class ExploreViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.explore(deviceRegion(), TimeZone.getDefault().id)

    val state: StateFlow<ExploreState> = presenter.state

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

    fun selectService(id: Long) {
        viewModelScope.launch { presenter.selectService(id) }
    }

    fun stripShown() {
        viewModelScope.launch { presenter.stripShown() }
    }

    fun stripOpened(tile: BrowseTile) {
        viewModelScope.launch { presenter.stripOpened(tile) }
    }

    fun opened(card: DiscoverCard) {
        viewModelScope.launch { presenter.opened(card) }
    }

    fun asking(card: DiscoverCard) {
        viewModelScope.launch { presenter.asking(card) }
    }

    fun reset(done: (Boolean) -> Unit) {
        viewModelScope.launch { done(presenter.resetRecommendations() == null) }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()

    suspend fun watch() = presenter.watch()
}

fun deviceRegion(): String = Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"

@Composable
fun ExploreScreen(
    nav: DiscoverNav,
    onBack: (() -> Unit)?,
) {
    val sdk = LocalSdk.current
    val resources = LocalResources.current
    val viewModel = viewModel { ExploreViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshOwned()
            viewModel.watch()
        }
    }
    var menu by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = stringResource(R.string.section_explore),
        onBack = onBack,
        actions = {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.discover_more_actions))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (state.canHide) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.discover_hidden_title)) },
                        onClick = {
                            menu = false
                            nav.openHidden()
                        },
                    )
                }
                if (state.canSeeStats) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.discover_stats_title)) },
                        onClick = {
                            menu = false
                            nav.openStats()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.discover_reset)) },
                    onClick = {
                        menu = false
                        viewModel.reset { ok ->
                            resetMessage = resources.getString(if (ok) R.string.discover_reset_done else R.string.discover_reset_failed)
                        }
                    },
                )
            }
        },
    ) { padding ->
        CardHost(
            nav = nav,
            canHide = state.canHide,
            onChanged = viewModel::refresh,
            onOpen = viewModel::opened,
            onAct = viewModel::asking,
            modifier = Modifier.padding(padding),
        ) { events ->
            StatusContent(
                status = state.status,
                isEmpty = state.isEmpty,
                onRefresh = viewModel::refresh,
                empty = {
                    MessageView(
                        icon = Icons.Rounded.Explore,
                        title = stringResource(R.string.discover_explore_building_title),
                        message = stringResource(R.string.discover_explore_building_message),
                        action = stringResource(R.string.section_search),
                        onAction = { nav.openSection(Section.SEARCH) },
                    )
                },
            ) {
                ExploreList(state, events, nav, viewModel, resetMessage)
            }
        }
    }
}

@Composable
private fun ExploreList(
    state: ExploreState,
    events: CardEvents,
    nav: DiscoverNav,
    viewModel: ExploreViewModel,
    notice: String?,
) {
    val list = rememberLazyListState()
    LoadMoreEffect(list, state.more && !state.loadingMore) { viewModel.loadMore() }
    LazyColumn(state = list, contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        item(key = "types") { TypeBar(state.type, viewModel::select, onGenre = { nav.openGenre(state.type, it) }) }
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        notice?.let { item(key = "notice") { NoticeBanner(it) } }
        if (state.sourceOff) item(key = "off") { NoticeBanner(stringResource(R.string.discover_source_off)) }
        state.hero.firstOrNull()?.let { hero ->
            item(key = "hero") {
                HeroCard(hero, state.heroPills.map { heroPillText(it) }, events, Modifier.padding(vertical = Spacing.Small))
            }
        }
        state.banner?.let { banner -> item(key = "banner") { BannerView(banner, events, nav) } }
        if (state.strip.isNotEmpty()) {
            item(key = "strip") {
                LaunchedEffect(Unit) { viewModel.stripShown() }
                StripView(state.strip, state.type, nav, viewModel::stripOpened)
            }
        }
        items(state.rows, key = { "row-" + it.id }) { row ->
            when (row.type) {
                "top_services" ->
                    state.top?.let { top ->
                        TopServicesView(
                            top = top,
                            row = row,
                            title = topTitle(state.type, null),
                            events = events,
                            onChip = viewModel::selectService,
                            onSeeAll = { nav.openSection(Section.POPULAR) },
                        )
                    }
                "genre_browse" ->
                    GenreBrowseRow(state.genres) { genre ->
                        val type =
                            if (genre.both) {
                                ExploreType.ALL
                            } else if (genre.kind == "show") {
                                ExploreType.SHOWS
                            } else {
                                ExploreType.MOVIES
                            }
                        nav.openGenre(type, genre.id)
                    }
                "np_services" ->
                    CardRowView(
                        row,
                        events,
                        title = stringResource(R.string.discover_new_on_services),
                        onSeeAll = { nav.openSection(Section.POPULAR) },
                    )
                else -> CardRowView(row, events, typed = state.type == ExploreType.ALL)
            }
        }
        if (state.loadingMore) {
            item(key = "more") {
                Row(Modifier.fillMaxWidth().padding(Spacing.Large), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun LoadMoreEffect(
    list: LazyListState,
    enabled: Boolean,
    onLoad: () -> Unit,
) {
    val near by remember {
        derivedStateOf {
            val info = list.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(list, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow { near }.collect { if (it) onLoad() }
    }
}

@Composable
fun topTitle(
    type: ExploreType,
    genre: String?,
): String =
    when {
        genre != null -> stringResource(R.string.discover_top_genre, genre)
        type == ExploreType.MOVIES -> stringResource(R.string.discover_top_movies)
        type == ExploreType.SHOWS -> stringResource(R.string.discover_top_shows)
        else -> stringResource(R.string.discover_top_all)
    }

@Composable
fun TypeBar(
    type: ExploreType,
    onSelect: (ExploreType) -> Unit,
    onGenre: ((Int) -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            ExploreType.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = entry == type,
                    onClick = { onSelect(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index, ExploreType.entries.size),
                    label = { Text(typeText(entry), maxLines = 1) },
                )
            }
        }
        if (onGenre != null && type != ExploreType.ALL) {
            Column {
                OutlinedButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = Spacing.Medium)) {
                    Text(stringResource(R.string.discover_genres))
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    Genres.of(type).forEach { genre ->
                        DropdownMenuItem(
                            text = { Text(genreText(genre.name)) },
                            onClick = {
                                open = false
                                onGenre(genre.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerView(
    banner: GenreBanner,
    events: CardEvents,
    nav: DiscoverNav,
) {
    val name = genreText(banner.genre.name)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = {
            val type = if (banner.genre.kind == CardKind.SHOW) ExploreType.SHOWS else ExploreType.MOVIES
            nav.openGenre(if (banner.both) ExploreType.ALL else type, banner.genre.id)
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Column(Modifier.padding(vertical = Spacing.Large)) {
            Column(Modifier.padding(horizontal = Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(
                    stringResource(R.string.discover_banner_eyebrow),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.discover_banner_title, name), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.discover_banner_line, banner.have, banner.picks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CardShelf(title = "") {
                items(banner.picks, key = { it.key }) { card -> TitlePosterCard(card, events) }
            }
        }
    }
}

@Composable
private fun StripView(
    tiles: List<BrowseTile>,
    type: ExploreType,
    nav: DiscoverNav,
    onOpened: (BrowseTile) -> Unit,
) {
    CardShelf(title = stringResource(R.string.discover_categories)) {
        items(tiles, key = { "${it.kind}-${it.genre}-${it.category}-${it.mood}-${it.name}" }) { tile ->
            BrowseTileCard(
                name = if (tile.category.isEmpty()) genreText(tile.name) else tile.name,
                image = tile.backdrop,
                hue = tileHue(tile.name),
                label = sectionLabel(tile.section),
                plain = true,
                modifier = Modifier.width(184.dp).aspectRatio(4f / 3f),
                onClick = {
                    onOpened(tile)
                    val link = browseLink(tile, type)
                    nav.openBrowseOrGenre(link)
                },
            )
        }
        item(key = "all") {
            BrowseTileCard(
                name = stringResource(R.string.discover_all_categories),
                image = tiles.firstOrNull()?.backdrop,
                hue = 0,
                label = stringResource(R.string.discover_strip_browse),
                modifier = Modifier.width(184.dp).aspectRatio(4f / 3f),
                onClick = { nav.openSection(Section.SEARCH) },
            )
        }
    }
}

fun DiscoverNav.openBrowseOrGenre(link: BrowseLink) {
    if (link.target == BrowseTarget.GENRE) openGenre(link.type, link.genre) else openBrowse(link)
}
