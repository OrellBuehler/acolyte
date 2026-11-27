package ch.orellbuehler.acolyte.tv.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.feature.discover.CardAction
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType

private const val DIM_MILLIS = 220

fun Modifier.tvPageInset(): Modifier = padding(horizontal = TvMetrics.marginX)

@Composable
fun TvShelf(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    seeAll: (() -> Unit)? = null,
    belowTitle: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val rows = LocalTvRowFocus.current
    val row = remember { Any() }
    val alpha by animateFloatAsState(rows?.alpha(row) ?: 1f, tween(DIM_MILLIS), label = "row-dim")
    Column(
        modifier
            .fillMaxWidth()
            .tvRowBounds()
            .onFocusChanged { rows?.update(row, it.hasFocus) }
            .graphicsLayer { this.alpha = alpha },
    ) {
        Column(Modifier.tvPageInset()) {
            Text(title, style = TvType.headline, color = TvColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, style = TvType.footnote, color = TvColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        belowTitle()
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = TvMetrics.marginX, vertical = TvMetrics.focusPad),
            horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
            verticalAlignment = Alignment.Top,
        ) {
            content()
            if (seeAll != null) {
                item(key = "see-all") {
                    TvActionButton(stringResource(R.string.tv_common_see_all), seeAll, icon = Icons.Rounded.ChevronRight)
                }
            }
        }
    }
}

@Composable
fun TvChipRow(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyRow(
        modifier = modifier.tvRowBounds().focusRestorer(),
        contentPadding = PaddingValues(horizontal = TvMetrics.marginX, vertical = TvMetrics.focusPad),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun TvDiscoverCard(
    card: DiscoverCard,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    typed: Boolean = false,
    subtitle: String? = null,
    badge: String? = null,
    showsText: Boolean = true,
    onFocus: (() -> Unit)? = null,
) {
    val navigator = LocalTvNavigator.current
    val line = subtitle ?: tvCardLine(card, typed)
    val fresh = tvFreshText(card.fresh)
    val action = tvActionText(card)
    val menu =
        if (card.action == null || action == null) {
            emptyList()
        } else {
            listOf(
                TvMenuAction("action", action, Icons.Rounded.Add) {
                    when (card.action) {
                        CardAction.ADD -> navigator.openTitle(card.kind.wire, card.tmdb)
                        else -> navigator.openAsk(card.kind.wire, card.tmdb)
                    }
                },
            )
        }
    if (wide) {
        TvBackdropCard(
            title = card.name,
            subtitle = line,
            image = card.wideImage,
            logo = card.logo,
            mark = card.mark,
            fresh = fresh,
            badge = badge,
            menu = menu,
            showsText = showsText,
            onFocus = onFocus,
            modifier = modifier,
            onClick = { navigator.openCard(card) },
        )
    } else {
        TvPosterCard(
            title = card.name,
            subtitle = line,
            image = card.posterImage,
            mark = card.mark,
            fresh = fresh,
            badge = badge,
            menu = menu,
            onFocus = onFocus,
            modifier = modifier,
            onClick = { navigator.openCard(card) },
        )
    }
}

@Composable
fun TvCardRow(
    row: CardRow,
    modifier: Modifier = Modifier,
    title: String? = null,
    typed: Boolean = false,
    seeAll: (() -> Unit)? = null,
    onFocusCard: ((DiscoverCard) -> Unit)? = null,
) {
    TvShelf(title ?: row.title, modifier, subtitle = row.subtitle.ifEmpty { null }, seeAll = seeAll) {
        items(row.cards, key = { it.key }) { card ->
            TvDiscoverCard(card, wide = row.wide, typed = typed, onFocus = onFocusCard?.let { focus -> { focus(card) } })
        }
    }
}

@Composable
fun TvTitleCard(
    card: TitleCard,
    manage: Boolean,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalTvNavigator.current
    val subtitle =
        when {
            card.inLibrary || card.titleId > 0 ->
                stringResource(if (manage) R.string.tv_title_card_in_library else R.string.tv_title_card_ready)
            card.ask != null -> stringResource(R.string.tv_title_card_requested)
            card.year > 0 -> card.year.toString()
            else -> null
        }
    TvPosterCard(
        title = card.name,
        subtitle = subtitle,
        image = card.poster,
        modifier = modifier,
        onClick = { navigator.openTitle(card.kind, card.tmdbId, card.titleId) },
    )
}
