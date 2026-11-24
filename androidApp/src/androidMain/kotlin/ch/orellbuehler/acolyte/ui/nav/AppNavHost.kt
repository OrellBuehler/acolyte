package ch.orellbuehler.acolyte.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.account.AccountScreen
import ch.orellbuehler.acolyte.ui.discover.ExploreScreen
import ch.orellbuehler.acolyte.ui.discover.PopularScreen
import ch.orellbuehler.acolyte.ui.discover.SearchScreen
import ch.orellbuehler.acolyte.ui.discover.discoverRoutes
import ch.orellbuehler.acolyte.ui.discover.rememberDiscoverNav
import ch.orellbuehler.acolyte.ui.health.HealthScreen
import ch.orellbuehler.acolyte.ui.home.HomeScreen
import ch.orellbuehler.acolyte.ui.library.LibraryCopiesScreen
import ch.orellbuehler.acolyte.ui.library.LibraryDeletedScreen
import ch.orellbuehler.acolyte.ui.library.LibraryFilesScreen
import ch.orellbuehler.acolyte.ui.library.LibraryFoldersScreen
import ch.orellbuehler.acolyte.ui.library.LibraryImportScreen
import ch.orellbuehler.acolyte.ui.library.LibraryReleasesScreen
import ch.orellbuehler.acolyte.ui.library.LibraryRenameScreen
import ch.orellbuehler.acolyte.ui.library.LibraryScreen
import ch.orellbuehler.acolyte.ui.library.LibraryStatsScreen
import ch.orellbuehler.acolyte.ui.library.LibrarySubtitlesScreen
import ch.orellbuehler.acolyte.ui.library.LibraryToolsScreen
import ch.orellbuehler.acolyte.ui.requests.AskMusicSheet
import ch.orellbuehler.acolyte.ui.requests.AskSheet
import ch.orellbuehler.acolyte.ui.requests.NeverAddScreen
import ch.orellbuehler.acolyte.ui.requests.OutsideListsScreen
import ch.orellbuehler.acolyte.ui.requests.ReportProblemSheet
import ch.orellbuehler.acolyte.ui.requests.ReportsScreen
import ch.orellbuehler.acolyte.ui.requests.RequestRulesScreen
import ch.orellbuehler.acolyte.ui.requests.RequestsScreen
import ch.orellbuehler.acolyte.ui.shell.MoreScreen
import ch.orellbuehler.acolyte.ui.shell.PlaceholderScreen
import ch.orellbuehler.acolyte.ui.title.TitleScreen

@Composable
fun AppNavHost(
    nav: NavHostController,
    account: Account,
    start: Any,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = account.tabs

    fun back(section: Section): (() -> Unit)? = if (section in tabs) null else ({ nav.popBackStack() })

    val open: (Any) -> Unit = { nav.navigate(it) }
    val pop: () -> Unit = { nav.popBackStack() }
    val discover = rememberDiscoverNav(nav, account)

    NavHost(navController = nav, startDestination = start, modifier = modifier) {
        composable<HomeRoute> { HomeScreen(discover, back(Section.HOME)) }
        composable<ExploreRoute> { ExploreScreen(discover, back(Section.EXPLORE)) }
        composable<LibraryRoute> { LibraryScreen(null, back(Section.LIBRARY), open) }
        composable<SearchRoute> { SearchScreen(discover, back(Section.SEARCH)) }
        composable<PopularRoute> { PopularScreen(discover, back(Section.POPULAR)) }
        composable<MoviesRoute> { LibraryScreen(LibraryKind.MOVIE, back(Section.MOVIES), open) }
        composable<ShowsRoute> { LibraryScreen(LibraryKind.SHOW, back(Section.SHOWS), open) }
        composable<MusicRoute> { PlaceholderScreen(Section.MUSIC, back(Section.MUSIC)) }
        composable<StatsRoute> { LibraryStatsScreen(back(Section.STATS)) }
        composable<DownloadsRoute> { PlaceholderScreen(Section.DOWNLOADS, back(Section.DOWNLOADS)) }
        composable<CalendarRoute> { PlaceholderScreen(Section.CALENDAR, back(Section.CALENDAR)) }
        composable<RequestsRoute> { RequestsScreen(account, onBack = back(Section.REQUESTS), onNavigate = { nav.navigate(it) }) }
        composable<HistoryRoute> { PlaceholderScreen(Section.HISTORY, back(Section.HISTORY)) }
        composable<HealthRoute> { HealthScreen(onBack = back(Section.HEALTH)) }
        composable<PeopleRoute> { PlaceholderScreen(Section.PEOPLE, back(Section.PEOPLE)) }
        composable<SettingsRoute> { PlaceholderScreen(Section.SETTINGS, back(Section.SETTINGS)) }
        composable<AccountRoute> { AccountScreen(account, onSignOut = onSignOut, onBack = back(Section.ACCOUNT)) }
        dialog<AskRoute> { entry ->
            val route = entry.toRoute<AskRoute>()
            AskSheet(route.kind, route.tmdb, route.fourK, onClose = { nav.popBackStack() })
        }
        dialog<AskMusicRoute> { entry ->
            val route = entry.toRoute<AskMusicRoute>()
            AskMusicSheet(route.artist, route.name, route.album, route.edition, onClose = { nav.popBackStack() })
        }
        composable<ReportsRoute> { ReportsScreen(onBack = { nav.popBackStack() }) }
        dialog<ReportProblemRoute> { entry ->
            val route = entry.toRoute<ReportProblemRoute>()
            ReportProblemSheet(route, onClose = { nav.popBackStack() }, onReports = {
                nav.popBackStack()
                nav.navigate(ReportsRoute)
            })
        }
        composable<RequestRulesRoute> { RequestRulesScreen(onBack = { nav.popBackStack() }, onNavigate = { nav.navigate(it) }) }
        composable<OutsideListsRoute> { OutsideListsScreen(onBack = { nav.popBackStack() }) }
        composable<NeverAddRoute> { NeverAddScreen(onBack = { nav.popBackStack() }) }
        composable<MoreRoute> { MoreScreen(account, onOpen = { nav.navigate(it.destination.route) }, onSignOut = onSignOut) }
        composable<TitleRoute> { entry -> TitleScreen(entry.toRoute(), nav, onBack = { nav.popBackStack() }) }
        composable<LibraryToolsRoute> { LibraryToolsScreen(pop, open) }
        composable<LibraryDeletedRoute> { LibraryDeletedScreen(it.toRoute<LibraryDeletedRoute>().titleId, pop) }
        composable<LibraryCopiesRoute> { LibraryCopiesScreen(pop) }
        composable<LibraryReleasesRoute> { LibraryReleasesScreen(it.toRoute<LibraryReleasesRoute>().turnedDown, pop, open) }
        composable<LibrarySubtitlesRoute> { LibrarySubtitlesScreen(pop) }
        composable<LibraryRenameRoute> { LibraryRenameScreen(pop) }
        composable<LibraryImportRoute> { LibraryImportScreen(pop, open) }
        composable<LibraryImportFilesRoute> { LibraryFilesScreen(it.toRoute<LibraryImportFilesRoute>().path, pop) }
        composable<SettingsLibraryFoldersRoute> { LibraryFoldersScreen(pop) }
        discoverRoutes(nav, account, discover)
    }
}
