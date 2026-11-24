package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowseTarget
import ch.orellbuehler.acolyte.feature.discover.BrowseViewState
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.GenreState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GenreViewModel(
    sdk: AcolyteSdk,
    type: ExploreType,
    genre: Int,
) : ViewModel() {
    private val presenter = sdk.exploreGenre(type, genre, deviceRegion())

    val state: StateFlow<GenreState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun selectService(id: Long) {
        viewModelScope.launch { presenter.selectService(id) }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()

    suspend fun watch() = presenter.watch()
}

@Composable
fun GenreScreen(
    type: ExploreType,
    genre: Int,
    nav: DiscoverNav,
    canHide: Boolean,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "genre-${type.wire}-$genre") { GenreViewModel(sdk, type, genre) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshOwned()
            viewModel.watch()
        }
    }
    val name = genreText(state.name)
    ScreenScaffold(title = name, onBack = onBack) { padding ->
        CardHost(nav, canHide, viewModel::refresh, Modifier.padding(padding)) { events ->
            StatusContent(
                status = state.status,
                isEmpty = state.isEmpty,
                onRefresh = viewModel::refresh,
                empty = {
                    MessageView(
                        icon = Icons.Rounded.Category,
                        title = stringResource(R.string.discover_genre_empty),
                        message = null,
                    )
                },
            ) {
                LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                    state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                    state.hero.firstOrNull()?.let { hero ->
                        item(key = "hero") { HeroCard(hero, emptyList(), events, Modifier.padding(vertical = Spacing.Small)) }
                    }
                    items(state.rows, key = { "row-" + it.id }) { row ->
                        val top = state.top
                        if (row.type == "top_services" && top != null) {
                            TopServicesView(top, row, topTitle(type, name), events, viewModel::selectService, null)
                        } else {
                            CardRowView(row, events)
                        }
                    }
                }
            }
        }
    }
}

class BrowseViewViewModel(
    sdk: AcolyteSdk,
    link: BrowseLink,
) : ViewModel() {
    private val presenter = sdk.browseView(link)

    val state: StateFlow<BrowseViewState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()
}

@Composable
fun BrowseViewScreen(
    link: BrowseLink,
    nav: DiscoverNav,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val key = "browse-${link.target}-${link.type}-${link.genre}-${link.category}-${link.mood}"
    val viewModel = viewModel(key = key) { BrowseViewViewModel(sdk, link) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.refreshOwned() } }
    val mood = link.target == BrowseTarget.MOOD
    val genre = genreText(state.genreName)
    val kinds =
        when (state.type) {
            ExploreType.MOVIES -> stringResource(R.string.discover_type_movies)
            ExploreType.SHOWS -> stringResource(R.string.discover_type_shows)
            ExploreType.ALL -> stringResource(R.string.discover_movies_and_shows)
        }
    ScreenScaffold(title = state.title.ifEmpty { genre }, onBack = onBack) { padding ->
        CardHost(nav, state.canHide, viewModel::refresh, Modifier.padding(padding)) { events ->
            StatusContent(
                status = state.status,
                isEmpty = state.isEmpty,
                onRefresh = viewModel::refresh,
                empty = {
                    if (mood) {
                        MessageView(
                            icon = Icons.Rounded.Category,
                            title = stringResource(R.string.empty_title),
                            message = stringResource(R.string.discover_mood_empty),
                            action = stringResource(R.string.discover_open_genre, genre),
                            onAction = { nav.openGenre(link.type, link.genre) },
                        )
                    } else {
                        MessageView(icon = Icons.Rounded.Category, title = stringResource(R.string.discover_category_empty))
                    }
                },
            ) {
                LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                    state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                    if (mood && genre.isNotEmpty()) {
                        item(key = "sub") {
                            Text(
                                "$genre · $kinds",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                            )
                        }
                    }
                    item(key = "grid") { CardGrid(state.cards, events, typed = state.type == ExploreType.ALL) }
                }
            }
        }
    }
}
