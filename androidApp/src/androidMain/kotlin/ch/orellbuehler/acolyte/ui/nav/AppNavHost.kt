package ch.orellbuehler.acolyte.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.account.AccountScreen
import ch.orellbuehler.acolyte.ui.health.HealthScreen
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

    NavHost(navController = nav, startDestination = start, modifier = modifier) {
        composable<HomeRoute> { PlaceholderScreen(Section.HOME, back(Section.HOME)) }
        composable<ExploreRoute> { PlaceholderScreen(Section.EXPLORE, back(Section.EXPLORE)) }
        composable<LibraryRoute> { PlaceholderScreen(Section.LIBRARY, back(Section.LIBRARY)) }
        composable<SearchRoute> { PlaceholderScreen(Section.SEARCH, back(Section.SEARCH)) }
        composable<PopularRoute> { PlaceholderScreen(Section.POPULAR, back(Section.POPULAR)) }
        composable<MoviesRoute> { PlaceholderScreen(Section.MOVIES, back(Section.MOVIES)) }
        composable<ShowsRoute> { PlaceholderScreen(Section.SHOWS, back(Section.SHOWS)) }
        composable<MusicRoute> { PlaceholderScreen(Section.MUSIC, back(Section.MUSIC)) }
        composable<StatsRoute> { PlaceholderScreen(Section.STATS, back(Section.STATS)) }
        composable<DownloadsRoute> { PlaceholderScreen(Section.DOWNLOADS, back(Section.DOWNLOADS)) }
        composable<CalendarRoute> { PlaceholderScreen(Section.CALENDAR, back(Section.CALENDAR)) }
        composable<RequestsRoute> { PlaceholderScreen(Section.REQUESTS, back(Section.REQUESTS)) }
        composable<HistoryRoute> { PlaceholderScreen(Section.HISTORY, back(Section.HISTORY)) }
        composable<HealthRoute> { HealthScreen(onBack = back(Section.HEALTH)) }
        composable<PeopleRoute> { PlaceholderScreen(Section.PEOPLE, back(Section.PEOPLE)) }
        composable<SettingsRoute> { PlaceholderScreen(Section.SETTINGS, back(Section.SETTINGS)) }
        composable<AccountRoute> { AccountScreen(account, onSignOut = onSignOut, onBack = back(Section.ACCOUNT)) }
        composable<MoreRoute> { MoreScreen(account, onOpen = { nav.navigate(it.destination.route) }, onSignOut = onSignOut) }
    }
}
