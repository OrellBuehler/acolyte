package ch.orellbuehler.acolyte.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.account.AccountScreen
import ch.orellbuehler.acolyte.ui.account.AiConnectScreen
import ch.orellbuehler.acolyte.ui.account.AlertsScreen
import ch.orellbuehler.acolyte.ui.account.DevicesScreen
import ch.orellbuehler.acolyte.ui.account.ProfileScreen
import ch.orellbuehler.acolyte.ui.account.SecurityScreen
import ch.orellbuehler.acolyte.ui.discover.ExploreScreen
import ch.orellbuehler.acolyte.ui.discover.PopularScreen
import ch.orellbuehler.acolyte.ui.discover.SearchScreen
import ch.orellbuehler.acolyte.ui.discover.discoverRoutes
import ch.orellbuehler.acolyte.ui.discover.rememberDiscoverNav
import ch.orellbuehler.acolyte.ui.downloads.CalendarScreen
import ch.orellbuehler.acolyte.ui.downloads.DownloadsScreen
import ch.orellbuehler.acolyte.ui.downloads.HistoryScreen
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
import ch.orellbuehler.acolyte.ui.people.PeopleDetailScreen
import ch.orellbuehler.acolyte.ui.people.PeopleScreen
import ch.orellbuehler.acolyte.ui.requests.AskMusicSheet
import ch.orellbuehler.acolyte.ui.requests.AskSheet
import ch.orellbuehler.acolyte.ui.requests.NeverAddScreen
import ch.orellbuehler.acolyte.ui.requests.OutsideListsScreen
import ch.orellbuehler.acolyte.ui.requests.ReportProblemSheet
import ch.orellbuehler.acolyte.ui.requests.ReportsScreen
import ch.orellbuehler.acolyte.ui.requests.RequestRulesScreen
import ch.orellbuehler.acolyte.ui.requests.RequestsScreen
import ch.orellbuehler.acolyte.ui.settingsb.AcquisitionPagesScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsDownloadersScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsMoveScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsNamingScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsPickRuleEditScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsPickRulesScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsSearchScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsSourceHealthScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsSourcesScreen
import ch.orellbuehler.acolyte.ui.settingsb.SettingsSubtitlesScreen
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
        composable<DownloadsRoute> {
            DownloadsScreen(
                show = "",
                open = "",
                onBack = back(Section.DOWNLOADS),
                onOpenTitle = { nav.navigate(Routes.downloadTitle(it)) },
                onOpenRoute = open,
            )
        }
        composable<CalendarRoute> {
            CalendarScreen(onBack = back(Section.CALENDAR), onOpenTitle = { nav.navigate(Routes.downloadTitle(it)) })
        }
        composable<RequestsRoute> { RequestsScreen(account, onBack = back(Section.REQUESTS), onNavigate = { nav.navigate(it) }) }
        composable<HistoryRoute> {
            HistoryScreen(
                titleId = 0,
                episodeId = 0,
                onBack = back(Section.HISTORY),
                onOpenTitle = { nav.navigate(Routes.downloadTitle(it)) },
                onOpenDownload = { nav.navigate(Routes.downloads(open = it)) },
                onOpenRoute = open,
            )
        }
        composable<DownloadsFocusRoute> { entry ->
            val route = entry.toRoute<DownloadsFocusRoute>()
            DownloadsScreen(
                show = route.show,
                open = route.open,
                onBack = { nav.popBackStack() },
                onOpenTitle = { nav.navigate(Routes.downloadTitle(it)) },
                onOpenRoute = open,
            )
        }
        composable<HistoryScopeRoute> { entry ->
            val route = entry.toRoute<HistoryScopeRoute>()
            HistoryScreen(
                titleId = route.titleId,
                episodeId = route.episodeId,
                onBack = { nav.popBackStack() },
                onOpenTitle = { nav.navigate(Routes.downloadTitle(it)) },
                onOpenDownload = { nav.navigate(Routes.downloads(open = it)) },
                onOpenRoute = open,
            )
        }
        composable<HealthRoute> { HealthScreen(onBack = back(Section.HEALTH)) }
        composable<PeopleRoute> { PeopleScreen(account, onOpen = open, onBack = back(Section.PEOPLE)) }
        composable<PeopleDetailRoute> { PeopleDetailScreen(it.toRoute<PeopleDetailRoute>().id, account, open, pop) }
        composable<PeopleDevicesRoute> {
            val route = it.toRoute<PeopleDevicesRoute>()
            DevicesScreen(route.id, route.name, pop)
        }
        composable<AccountProfileRoute> { ProfileScreen(pop) }
        composable<AccountSecurityRoute> { SecurityScreen(pop) }
        composable<AccountAlertsRoute> { AlertsScreen(pop) }
        composable<AccountDevicesRoute> { DevicesScreen(null, stringResource(R.string.myaccount_devices), pop) }
        composable<AiConnectRoute> { AiConnectScreen(pop) }
        composable<SettingsRoute> { AcquisitionPagesScreen(account, back(Section.SETTINGS), open) }
        composable<AccountRoute> { AccountScreen(account, onSignOut = onSignOut, onBack = back(Section.ACCOUNT), onOpen = open) }
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
        composable<MoreRoute> {
            MoreScreen(
                account,
                onOpen = { nav.navigate(it.destination.route) },
                onSignOut = onSignOut,
                onOpenDecisions = { nav.navigate(Routes.downloads(show = "problems.decide")) },
            )
        }
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
        composable<SettingsSourcesRoute> { SettingsSourcesScreen(account, pop, open) }
        composable<SettingsSourceHealthRoute> { SettingsSourceHealthScreen(it.toRoute<SettingsSourceHealthRoute>().id, account, pop) }
        composable<SettingsDownloadersRoute> { SettingsDownloadersScreen(account, pop, open) }
        composable<SettingsPickRulesRoute> { SettingsPickRulesScreen(account, pop, open) }
        composable<SettingsPickRuleEditRoute> {
            val route = it.toRoute<SettingsPickRuleEditRoute>()
            SettingsPickRuleEditScreen(route.id, route.from, account, pop, open)
        }
        composable<SettingsNamingRoute> { SettingsNamingScreen(account, pop) }
        composable<SettingsSubtitlesRoute> { SettingsSubtitlesScreen(account, pop) }
        composable<SettingsSearchRoute> { SettingsSearchScreen(account, pop) }
        composable<SettingsMoveRoute> { SettingsMoveScreen(account, pop, open) }
        discoverRoutes(nav, account, discover)
    }
}
