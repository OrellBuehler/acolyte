package ch.orellbuehler.acolyte.tv.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.tv.material3.Icon
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.TvScreenshotLaunch
import ch.orellbuehler.acolyte.tv.ask.TvAskScreen
import ch.orellbuehler.acolyte.tv.browse.TvBrowseScreen
import ch.orellbuehler.acolyte.tv.browse.TvCollectionScreen
import ch.orellbuehler.acolyte.tv.browse.TvGenreScreen
import ch.orellbuehler.acolyte.tv.browse.TvPersonScreen
import ch.orellbuehler.acolyte.tv.calendar.TvCalendarScreen
import ch.orellbuehler.acolyte.tv.components.TvOpenOnPhoneView
import ch.orellbuehler.acolyte.tv.components.requestFocusSafely
import ch.orellbuehler.acolyte.tv.downloads.TvDownloadsScreen
import ch.orellbuehler.acolyte.tv.explore.TvExploreScreen
import ch.orellbuehler.acolyte.tv.home.TvHomeScreen
import ch.orellbuehler.acolyte.tv.popular.TvPopularScreen
import ch.orellbuehler.acolyte.tv.requests.TvRequestsScreen
import ch.orellbuehler.acolyte.tv.search.TvSearchScreen
import ch.orellbuehler.acolyte.tv.settings.TvSettingsScreen
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.tv.title.TvTitleScreen
import kotlinx.coroutines.launch

val LocalTvSignOut = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun TvMain(
    account: Account,
    onSignOut: () -> Unit,
) {
    val nav = rememberNavController()
    val tabs = TvSections.tabs(account)
    val selection = remember(tabs) { TvTabSelection(tabs) }
    val navigator = remember(account, nav, selection) { TvNavigator(account, nav, selection) }
    CompositionLocalProvider(LocalTvNavigator provides navigator, LocalTvSignOut provides onSignOut) {
        TvScreenshotLaunch.Navigate()
        NavHost(nav, startDestination = TabsRoute) {
            composable<TabsRoute> { TvMainTabs(account, selection) }
            composable<TitleRoute> { entry ->
                val route = entry.toRoute<TitleRoute>()
                TvTitleScreen(route.kind, route.tmdb, route.libraryId)
            }
            composable<AskRoute> { entry ->
                val route = entry.toRoute<AskRoute>()
                TvAskScreen(route.kind, route.tmdb, route.fourK)
            }
            composable<PersonRoute> { entry -> TvPersonScreen(entry.toRoute<PersonRoute>().id) }
            composable<CollectionRoute> { entry -> TvCollectionScreen(entry.toRoute<CollectionRoute>().id) }
            composable<GenreRoute> { entry ->
                val route = entry.toRoute<GenreRoute>()
                TvGenreScreen(route.type, route.genre)
            }
            composable<BrowseRoute> { entry -> TvBrowseScreen(entry.toRoute<BrowseRoute>()) }
            composable<SectionRoute> { entry ->
                val section = Section.entries.firstOrNull { it.name == entry.toRoute<SectionRoute>().section }
                if (section != null) TvSectionScreen(section, account)
            }
            composable<PhoneRoute> { entry ->
                val route = entry.toRoute<PhoneRoute>()
                val section = Section.entries.firstOrNull { it.name == route.section }
                val topic = PhoneTopic.entries.firstOrNull { it.name == route.topic } ?: PhoneTopic.OTHER
                TvOpenOnPhoneView(stringResource(section?.titleRes ?: topic.titleRes))
            }
        }
    }
}

@Composable
fun TvSectionScreen(
    section: Section,
    account: Account,
) {
    when (section) {
        Section.HOME -> TvHomeScreen()
        Section.EXPLORE -> TvExploreScreen(account)
        Section.SEARCH -> TvSearchScreen(account)
        Section.REQUESTS -> TvRequestsScreen(account)
        Section.CALENDAR -> TvCalendarScreen(account)
        Section.DOWNLOADS -> TvDownloadsScreen(account)
        Section.POPULAR -> TvPopularScreen(account)
        Section.SETTINGS -> TvSettingsScreen(account)
        else -> TvOpenOnPhoneView(stringResource(section.titleRes))
    }
}

@Composable
fun TvMainTabs(
    account: Account,
    selection: TvTabSelection,
    content: @Composable (Section) -> Unit = { TvSectionScreen(it, account) },
) {
    val tabFocus = remember { FocusRequester() }
    var tabsFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { tabFocus.requestFocusSafely() }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = !tabsFocused || selection.index != 0) {
        if (tabsFocused) {
            selection.index = 0
            scope.launch {
                withFrameNanos { }
                tabFocus.requestFocusSafely()
            }
        } else {
            tabFocus.requestFocusSafely()
        }
    }
    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selection.index,
            containerColor = Color.Transparent,
            modifier =
                Modifier
                    .padding(start = TvMetrics.marginX, end = TvMetrics.marginX, top = TvMetrics.marginY)
                    .onFocusChanged { tabsFocused = it.hasFocus },
        ) {
            selection.tabs.forEachIndexed { index, section ->
                Tab(
                    selected = index == selection.index,
                    onFocus = { selection.index = index },
                    onClick = { selection.index = index },
                    modifier = if (index == selection.index) Modifier.focusRequester(tabFocus) else Modifier,
                    colors =
                        TabDefaults.pillIndicatorTabColors(
                            contentColor = TvColors.textMuted,
                            selectedContentColor = TvColors.text,
                            focusedContentColor = TvColors.background,
                            focusedSelectedContentColor = TvColors.background,
                        ),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(section.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(section.titleRes), style = TvType.footnoteBold, maxLines = 1)
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxSize()) {
            content(selection.current)
        }
    }
}
