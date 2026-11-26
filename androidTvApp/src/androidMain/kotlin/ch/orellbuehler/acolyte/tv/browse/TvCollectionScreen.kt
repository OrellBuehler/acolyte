package ch.orellbuehler.acolyte.tv.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.discover.SeriesState
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvCardSurface
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText

@Composable
fun TvCollectionScreen(id: String) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "collection-$id") { TvCollectionViewModel(sdk, id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvCollectionContent(state, viewModel::refresh)
}

@Composable
fun TvCollectionContent(
    state: SeriesState,
    onRefresh: () -> Unit,
) {
    TvStatusContent(
        status = state.status,
        isEmpty = state.isEmpty,
        reload = onRefresh,
        empty = { TvEmptyView(icon = Icons.Rounded.Movie) },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = TvMetrics.marginY),
            verticalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
        ) {
            item(key = "header") {
                Column(Modifier.tvPageInset(), verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap)) {
                    Text(
                        state.name.ifEmpty { stringResource(R.string.tv_discover_series_title) },
                        style = TvType.title3,
                        color = TvColors.text,
                        maxLines = 2,
                    )
                    state.status.error?.let { TvNoticeBanner(errorText(it)) }
                }
            }
            items(state.movies, key = { it.id }) { movie ->
                TvCardSurface(
                    onClick = {},
                    modifier = Modifier.padding(horizontal = TvMetrics.marginX).fillMaxWidth().heightIn(min = TvMetrics.thumbHeight),
                ) {
                    Column(Modifier.padding(TvMetrics.gutter), verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap / 2)) {
                        Text(
                            if (movie.year > 0) "${movie.name} (${movie.year})" else movie.name,
                            style = TvType.calloutBold,
                            color = TvColors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            pluralStringResource(R.plurals.tv_discover_series_versions, movie.versions, movie.versions) +
                                " · " +
                                pluralStringResource(R.plurals.tv_discover_series_files, movie.files, movie.files),
                            style = TvType.footnote,
                            color = TvColors.textMuted,
                        )
                    }
                }
            }
        }
    }
}
