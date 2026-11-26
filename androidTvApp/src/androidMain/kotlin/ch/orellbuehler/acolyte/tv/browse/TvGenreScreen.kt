package ch.orellbuehler.acolyte.tv.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.GenreState
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvBrowsePage
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.tvDiscoverRows
import ch.orellbuehler.acolyte.tv.components.tvGenreText
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText

@Composable
fun TvGenreScreen(
    type: String,
    genre: Int,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "genre-$type-$genre") { TvGenreViewModel(sdk, ExploreType.of(type), genre) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshOwned()
            viewModel.watch()
        }
    }
    TvGenreContent(state, viewModel::refresh, viewModel::selectService)
}

@Composable
fun TvGenreContent(
    state: GenreState,
    onRefresh: () -> Unit,
    onPickService: (Long) -> Unit,
) {
    val name = tvGenreText(state.name)
    TvStatusContent(
        status = state.status,
        isEmpty = state.isEmpty,
        reload = onRefresh,
        empty = { TvEmptyView(title = stringResource(R.string.tv_discover_genre_empty), icon = Icons.Rounded.GridView) },
    ) {
        TvBrowsePage(
            hero = state.hero.firstOrNull(),
            header = {
                Column(Modifier.tvPageInset().padding(top = TvMetrics.marginY)) {
                    Text(name, style = TvType.title3, color = TvColors.text, maxLines = 1)
                    state.status.error?.let { TvNoticeBanner(errorText(it)) }
                }
            },
        ) { onFocusCard ->
            tvDiscoverRows(
                rows = state.rows,
                top = state.top,
                type = state.type,
                onPickService = onPickService,
                topGenre = name,
                onFocusCard = onFocusCard,
            )
        }
    }
}
