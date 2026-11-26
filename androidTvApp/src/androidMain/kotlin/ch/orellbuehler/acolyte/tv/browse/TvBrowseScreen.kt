package ch.orellbuehler.acolyte.tv.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowseTarget
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.tv.components.TvBrowseListView
import ch.orellbuehler.acolyte.tv.nav.BrowseRoute
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.ui.LocalSdk

@Composable
fun TvBrowseScreen(route: BrowseRoute) {
    val sdk = LocalSdk.current
    val navigator = LocalTvNavigator.current
    val link =
        remember(route) {
            BrowseLink(
                target = BrowseTarget.entries.firstOrNull { it.name == route.target } ?: BrowseTarget.CATEGORY,
                type = ExploreType.of(route.type),
                genre = route.genre,
                category = route.category,
                mood = route.mood,
            )
        }
    val viewModel = viewModel(key = "browse-$route") { TvBrowseListViewModel(sdk, link) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.refreshOwned() }
    }
    TvBrowseListView(state, link, viewModel::refresh) { navigator.openGenre(link.type.wire, link.genre) }
}
