package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowseTarget
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.nav.CollectionRoute
import ch.orellbuehler.acolyte.ui.nav.DiscoverBrowseRoute
import ch.orellbuehler.acolyte.ui.nav.DiscoverGenreRoute
import ch.orellbuehler.acolyte.ui.nav.DiscoverHiddenRoute
import ch.orellbuehler.acolyte.ui.nav.DiscoverStatsRoute
import ch.orellbuehler.acolyte.ui.nav.PersonRoute
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.nav.destination

@Composable
fun rememberDiscoverNav(
    nav: NavHostController,
    account: Account,
): DiscoverNav =
    remember(nav, account) {
        val tabs = account.tabs
        DiscoverNav(
            openTitle = { kind, tmdb -> nav.navigate(Routes.title(kind, tmdb)) },
            ask = { kind, tmdb -> nav.navigate(Routes.ask(kind, tmdb)) },
            openPerson = { nav.navigate(Routes.person(it)) },
            openGenre = { type, genre -> nav.navigate(DiscoverGenreRoute(type.wire, genre)) },
            openBrowse = { link ->
                nav.navigate(DiscoverBrowseRoute(link.target.name, link.type.wire, link.genre, link.category, link.mood))
            },
            openSection = { section ->
                when {
                    section in tabs ->
                        nav.navigate(section.destination.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    account.shows(section) -> nav.navigate(section.destination.route)
                }
            },
            openHidden = { nav.navigate(DiscoverHiddenRoute) },
            openStats = { nav.navigate(DiscoverStatsRoute) },
        )
    }

fun NavGraphBuilder.discoverRoutes(
    nav: NavHostController,
    account: Account,
    discover: DiscoverNav,
) {
    val canHide = account.can(Need.USERS)
    val back: () -> Unit = { nav.popBackStack() }
    composable<PersonRoute> { entry -> PersonScreen(entry.toRoute<PersonRoute>().id, discover, canHide, back) }
    composable<CollectionRoute> { entry -> SeriesScreen(entry.toRoute<CollectionRoute>().id, back) }
    composable<DiscoverGenreRoute> { entry ->
        val route = entry.toRoute<DiscoverGenreRoute>()
        GenreScreen(ExploreType.of(route.type), route.genre, discover, canHide, back)
    }
    composable<DiscoverBrowseRoute> { entry ->
        val route = entry.toRoute<DiscoverBrowseRoute>()
        val link =
            BrowseLink(
                target = BrowseTarget.entries.firstOrNull { it.name == route.target } ?: BrowseTarget.CATEGORY,
                type = ExploreType.of(route.type),
                genre = route.genre,
                category = route.category,
                mood = route.mood,
            )
        BrowseViewScreen(link, discover, back)
    }
    composable<DiscoverHiddenRoute> { HiddenScreen(back) }
    composable<DiscoverStatsRoute> { ExploreStatsScreen(back) }
}
