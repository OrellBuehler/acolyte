package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowseTarget
import ch.orellbuehler.acolyte.feature.discover.BrowseViewState
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.components.errorText

@Composable
fun TvBrowseListView(
    state: BrowseViewState,
    link: BrowseLink,
    onRefresh: () -> Unit,
    onOpenGenre: () -> Unit,
) {
    val genre = tvGenreText(state.genreName)
    TvStatusContent(
        status = state.status,
        isEmpty = state.isEmpty,
        reload = onRefresh,
        empty = {
            if (link.target == BrowseTarget.MOOD) {
                TvEmptyView(
                    title = stringResource(R.string.tv_empty_title),
                    message = stringResource(R.string.tv_discover_mood_empty),
                    icon = Icons.Rounded.GridView,
                    actions = { TvActionButton(stringResource(R.string.tv_discover_open_genre, genre), onOpenGenre, prominent = true) },
                )
            } else {
                TvEmptyView(title = stringResource(R.string.tv_discover_category_empty), icon = Icons.Rounded.GridView)
            }
        },
    ) {
        TvAmbientPage(topPadding = TvMetrics.marginY + TvMetrics.focusPad) { onFocusImage ->
            item(key = "header") {
                Column(Modifier.tvPageInset(), verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap)) {
                    Text(state.title.ifEmpty { genre }, style = TvType.title3, color = TvColors.text, maxLines = 1)
                    if (link.target == BrowseTarget.MOOD && genre.isNotEmpty()) {
                        Text("$genre · ${tvTypeKinds(state.type)}", style = TvType.callout, color = TvColors.textMuted, maxLines = 1)
                    }
                    state.status.error?.let { TvNoticeBanner(errorText(it)) }
                }
            }
            tvPosterGrid(state.cards, "browse", typed = state.type == ExploreType.ALL, onFocusCard = { onFocusImage(it.wideImage) })
        }
    }
}

@Composable
private fun tvTypeKinds(type: ExploreType): String =
    when (type) {
        ExploreType.MOVIES -> stringResource(R.string.tv_discover_type_movies)
        ExploreType.SHOWS -> stringResource(R.string.tv_discover_type_shows)
        ExploreType.ALL -> stringResource(R.string.tv_discover_movies_and_shows)
    }
