package ch.orellbuehler.acolyte.ui.title

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.title.Feedback
import ch.orellbuehler.acolyte.feature.title.TitleProblem
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.feature.title.TitleTab
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorView
import ch.orellbuehler.acolyte.ui.components.LoadingView
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.nav.HistoryRoute
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.nav.TitleRoute
import ch.orellbuehler.acolyte.ui.theme.Spacing

class TitleNav(
    private val nav: NavHostController,
    private val before: () -> Unit = {},
    private val open: (String) -> Unit,
) {
    fun closing(close: () -> Unit): TitleNav = TitleNav(nav, close, open)

    fun card(card: TitleCard) {
        before()
        if (card.titleId > 0) {
            nav.navigate(Routes.libraryTitle(card.titleId).copy(kind = card.kind, tmdb = card.tmdbId))
        } else {
            nav.navigate(Routes.title(card.kind, card.tmdbId))
        }
    }

    fun person(id: String) {
        if (id.isBlank()) return
        before()
        nav.navigate(Routes.person(id))
    }

    fun collection(id: String) {
        if (id.isBlank()) return
        before()
        nav.navigate(Routes.collection(id))
    }

    fun ask(
        kind: String,
        tmdb: String,
        fourK: Boolean = false,
    ) {
        before()
        nav.navigate(Routes.ask(kind, tmdb, fourK))
    }

    fun report(
        titleId: Long,
        name: String,
    ) {
        before()
        nav.navigate(Routes.reportProblem(titleId, name))
    }

    fun history() {
        before()
        nav.navigate(HistoryRoute)
    }

    fun url(url: String) = open(url)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    route: TitleRoute,
    nav: NavHostController,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "title-${route.kind}-${route.tmdb}-${route.libraryId}") { TitleViewModel(sdk, route) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.presenter.watch() } }
    val context = LocalContext.current
    val links =
        remember(nav) {
            TitleNav(nav) { url ->
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }
        }
    val snackbar = remember { SnackbarHostState() }
    val list = rememberLazyListState()
    val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 } }
    LaunchedEffect(state.tab, state.ownedId) {
        if (state.tab == TitleTab.LIBRARY) viewModel.presenter.loadChoices()
    }

    CompositionLocalProvider(LocalTitleSnackbar provides snackbar) {
        FeedbackEffect(state.feedback)
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        if (scrolled) Text(state.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    },
                    actions = { if (state.ready) TitleTopActions(state, viewModel, links) },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = if (scrolled) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                        ),
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                when {
                    state.problem == TitleProblem.BLOCKED ->
                        MessageView(
                            icon = Icons.Rounded.Block,
                            title = stringResource(R.string.title_blocked),
                            action = stringResource(R.string.common_back),
                            onAction = onBack,
                            modifier = Modifier.padding(padding),
                        )
                    state.problem == TitleProblem.NOT_FOUND ->
                        MessageView(
                            icon = Icons.Rounded.SearchOff,
                            title = stringResource(R.string.title_not_found),
                            message = stringResource(R.string.title_not_found_hint),
                            action = stringResource(R.string.common_back),
                            onAction = onBack,
                            modifier = Modifier.padding(padding),
                        )
                    state.problem == TitleProblem.NOT_IN_LIBRARY ->
                        MessageView(
                            icon = Icons.Rounded.SearchOff,
                            title = stringResource(R.string.title_not_in_library),
                            message = stringResource(R.string.title_not_in_library_hint),
                            action = stringResource(R.string.common_back),
                            onAction = onBack,
                            modifier = Modifier.padding(padding),
                        )
                    state.status.phase == Phase.LOADING -> LoadingView(Modifier.padding(padding))
                    state.status.phase == Phase.FAILED ->
                        state.status.error?.let { ErrorView(it, Modifier.padding(padding), onRetry = viewModel::refresh) }
                    else ->
                        PullToRefreshBox(isRefreshing = state.status.refreshing, onRefresh = viewModel::refresh) {
                            LazyColumn(state = list, contentPadding = PaddingValues(bottom = Spacing.XLarge * 2)) {
                                titleContent(state, viewModel, links)
                            }
                        }
                }
            }
        }
        TitleSheets(state, viewModel, links)
    }
}

private fun LazyListScope.titleContent(
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    item(key = "hero") { TitleHero(state, viewModel, links) }
    if (state.isOwned) {
        stickyHeader(key = "tabs") { TitleTabs(state, viewModel) }
    }
    if (state.tab == TitleTab.LIBRARY && state.isOwned) {
        libraryContent(state, viewModel, links)
    } else {
        overviewContent(state, viewModel, links)
    }
}

@Composable
private fun TitleTabs(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    PrimaryTabRow(
        selectedTabIndex = if (state.tab == TitleTab.OVERVIEW) 0 else 1,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Tab(
            selected = state.tab == TitleTab.OVERVIEW,
            onClick = { viewModel.presenter.selectTab(TitleTab.OVERVIEW) },
            text = { Text(stringResource(R.string.title_tab_overview)) },
        )
        Tab(
            selected = state.tab == TitleTab.LIBRARY,
            onClick = { viewModel.presenter.selectTab(TitleTab.LIBRARY) },
            text = {
                Text(
                    if (state.libraryAlert) {
                        stringResource(
                            R.string.title_tab_library_alert,
                        )
                    } else {
                        stringResource(R.string.title_tab_library)
                    },
                )
            },
        )
    }
}

val LocalTitleSnackbar = compositionLocalOf<SnackbarHostState?> { null }

@Composable
fun FeedbackEffect(feedback: Feedback?) {
    val host = LocalTitleSnackbar.current ?: return
    if (feedback == null) return
    val text = feedbackText(feedback)
    LaunchedEffect(feedback) { host.showSnackbar(text) }
}
