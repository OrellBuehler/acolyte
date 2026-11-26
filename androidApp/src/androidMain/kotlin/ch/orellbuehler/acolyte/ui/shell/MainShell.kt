package ch.orellbuehler.acolyte.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.SessionViewModel
import ch.orellbuehler.acolyte.ui.nav.AppNavHost
import ch.orellbuehler.acolyte.ui.nav.Destination
import ch.orellbuehler.acolyte.ui.nav.RequestsRoute
import ch.orellbuehler.acolyte.ui.nav.ScreenshotLaunch
import ch.orellbuehler.acolyte.ui.nav.destination

@Composable
fun MainShell(
    account: Account,
    session: SessionViewModel,
) {
    val tabs = account.tabs.map { it.destination } + account.moreTab.destination
    val sdk = LocalSdk.current
    val inbox by sdk.inbox.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(sdk, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { sdk.inbox.poll() } }
    val requestsTab = Section.REQUESTS in account.tabs
    key(tabs.map { it.route }) {
        val nav = rememberNavController()
        ScreenshotLaunch.Navigate(nav)
        val entry by nav.currentBackStackEntryAsState()
        var selected by rememberSaveable { mutableIntStateOf(0) }
        val current = tabs.indexOfFirst { tab -> entry?.destination?.hasRoute(tab.route::class) == true }
        LaunchedEffect(current) { if (current >= 0) selected = current }
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = index == selected
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                nav.openTab(tab, isSelected)
                                selected = index
                            },
                            icon = {
                                val badge = if (tab.route == RequestsRoute) inbox.requestsBadge else 0
                                val dot = index == tabs.lastIndex && !requestsTab && inbox.hasAttention
                                BadgedBox(badge = {
                                    if (badge > 0) {
                                        Badge { Text(badge.toString()) }
                                    } else if (dot) {
                                        Badge()
                                    }
                                }) {
                                    Icon(if (isSelected) tab.selectedIcon else tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(tab.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AppNavHost(nav, account, start = tabs.first().route, onSignOut = session::signOut)
            }
        }
    }
}

private fun NavHostController.openTab(
    tab: Destination,
    reselected: Boolean,
) {
    if (reselected) {
        popBackStack(tab.route, inclusive = false)
        return
    }
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
