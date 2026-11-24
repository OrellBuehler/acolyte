package ch.orellbuehler.acolyte.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.account.AccountScreen
import ch.orellbuehler.acolyte.ui.health.HealthScreen
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
import ch.orellbuehler.acolyte.ui.library.TitlePlaceholderScreen
import ch.orellbuehler.acolyte.ui.shell.MoreScreen
import ch.orellbuehler.acolyte.ui.shell.PlaceholderScreen

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

    NavHost(navController = nav, startDestination = start, modifier = modifier) {
        composable<HomeRoute> { PlaceholderScreen(Section.HOME, back(Section.HOME)) }
        composable<ExploreRoute> { PlaceholderScreen(Section.EXPLORE, back(Section.EXPLORE)) }
        composable<LibraryRoute> { LibraryScreen(null, back(Section.LIBRARY), open) }
        composable<SearchRoute> { PlaceholderScreen(Section.SEARCH, back(Section.SEARCH)) }
        composable<PopularRoute> { PlaceholderScreen(Section.POPULAR, back(Section.POPULAR)) }
        composable<MoviesRoute> { LibraryScreen(LibraryKind.MOVIE, back(Section.MOVIES), open) }
        composable<ShowsRoute> { LibraryScreen(LibraryKind.SHOW, back(Section.SHOWS), open) }
        composable<MusicRoute> { PlaceholderScreen(Section.MUSIC, back(Section.MUSIC)) }
        composable<StatsRoute> { LibraryStatsScreen(back(Section.STATS)) }
        composable<DownloadsRoute> { PlaceholderScreen(Section.DOWNLOADS, back(Section.DOWNLOADS)) }
        composable<CalendarRoute> { PlaceholderScreen(Section.CALENDAR, back(Section.CALENDAR)) }
        composable<RequestsRoute> { PlaceholderScreen(Section.REQUESTS, back(Section.REQUESTS)) }
        composable<HistoryRoute> { PlaceholderScreen(Section.HISTORY, back(Section.HISTORY)) }
        composable<HealthRoute> { HealthScreen(onBack = back(Section.HEALTH)) }
        composable<PeopleRoute> { PlaceholderScreen(Section.PEOPLE, back(Section.PEOPLE)) }
        composable<SettingsRoute> { PlaceholderScreen(Section.SETTINGS, back(Section.SETTINGS)) }
        composable<AccountRoute> { AccountScreen(account, onSignOut = onSignOut, onBack = back(Section.ACCOUNT)) }
        composable<MoreRoute> { MoreScreen(account, onOpen = { nav.navigate(it.destination.route) }, onSignOut = onSignOut) }
        composable<TitleRoute> { TitlePlaceholderScreen(pop) }
        composable<LibraryToolsRoute> { LibraryToolsScreen(pop, open) }
        composable<LibraryDeletedRoute> { LibraryDeletedScreen(it.toRoute<LibraryDeletedRoute>().titleId, pop) }
        composable<LibraryCopiesRoute> { LibraryCopiesScreen(pop) }
        composable<LibraryReleasesRoute> { LibraryReleasesScreen(it.toRoute<LibraryReleasesRoute>().turnedDown, pop, open) }
        composable<LibrarySubtitlesRoute> { LibrarySubtitlesScreen(pop) }
        composable<LibraryRenameRoute> { LibraryRenameScreen(pop) }
        composable<LibraryImportRoute> { LibraryImportScreen(pop, open) }
        composable<LibraryImportFilesRoute> { LibraryFilesScreen(it.toRoute<LibraryImportFilesRoute>().path, pop) }
        composable<SettingsLibraryFoldersRoute> { LibraryFoldersScreen(pop) }
    }
}
