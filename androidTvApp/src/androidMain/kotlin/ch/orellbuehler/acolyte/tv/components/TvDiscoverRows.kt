package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.api.model.GenreCard
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.ServiceChip
import ch.orellbuehler.acolyte.feature.discover.TopServices
import ch.orellbuehler.acolyte.feature.discover.tileHue
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType

const val TV_POSTER_COLUMNS = 6

@Composable
fun TvInlineLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(TvMetrics.backdropHeight), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TvColors.accent, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun TvTypeChipRow(
    type: ExploreType,
    onSelect: (ExploreType) -> Unit,
) {
    TvChipRow {
        items(ExploreType.entries, key = { it.name }) { entry ->
            TvChip(tvTypeText(entry), entry == type, { onSelect(entry) })
        }
    }
}

@Composable
fun TvServiceChips(
    chips: List<ServiceChip>,
    selected: Long,
    onPick: (Long) -> Unit,
    showAll: Boolean = false,
) {
    TvChipRow {
        if (showAll) {
            item(key = "all") { TvChip(stringResource(R.string.tv_discover_type_all), selected == 0L, { onPick(0) }) }
        }
        items(chips, key = { it.id }) { chip -> TvChip(chip.name, chip.id == selected, { onPick(chip.id) }) }
    }
}

@Composable
fun TvChipShelf(
    title: String,
    row: CardRow,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    note: String? = null,
    chips: List<ServiceChip> = emptyList(),
    selected: Long = 0,
    showAll: Boolean = false,
    onPick: ((Long) -> Unit)? = null,
    ranked: Boolean = false,
    wide: Boolean = false,
    loading: Boolean = false,
    onFocusCard: ((DiscoverCard) -> Unit)? = null,
) {
    Column(modifier) {
        TvShelf(
            title = title,
            subtitle = subtitle,
            belowTitle = { if (chips.size > 1 && onPick != null) TvServiceChips(chips, selected, onPick, showAll) },
        ) {
            if (loading) {
                item(key = "loading") { TvInlineLoading(Modifier.padding(horizontal = TvMetrics.marginX)) }
            } else {
                items(row.cards.withIndex().toList(), key = { it.value.key }) { (index, card) ->
                    TvDiscoverCard(
                        card = card,
                        wide = wide,
                        badge = if (ranked) "#${card.rank.takeIf { it > 0 } ?: (index + 1)}" else null,
                        onFocus = onFocusCard?.let { focus -> { focus(card) } },
                    )
                }
            }
        }
        if (!note.isNullOrEmpty()) {
            Text(note, style = TvType.caption, color = TvColors.textFaint, modifier = Modifier.tvPageInset())
        }
    }
}

fun genreCardType(genre: GenreCard): ExploreType =
    when {
        genre.both -> ExploreType.ALL
        genre.kind == "show" -> ExploreType.SHOWS
        else -> ExploreType.MOVIES
    }

fun LazyListScope.tvDiscoverRows(
    rows: List<CardRow>,
    top: TopServices?,
    type: ExploreType,
    onPickService: (Long) -> Unit,
    genres: List<GenreCard> = emptyList(),
    topGenre: String? = null,
    onSeeAllNew: (() -> Unit)? = null,
    onFocusCard: ((DiscoverCard) -> Unit)? = null,
) {
    items(rows, key = { "row-" + it.id }) { row ->
        when (row.type) {
            "top_services" ->
                if (top != null) {
                    TvChipShelf(
                        title = tvTopTitle(type, topGenre),
                        row = row,
                        subtitle = if (top.serviceName.isEmpty()) null else tvTopSource(top),
                        note = top.attribution,
                        chips = top.chips,
                        selected = top.selected,
                        onPick = onPickService,
                        ranked = top.ranked,
                        onFocusCard = onFocusCard,
                    )
                }
            "genre_browse" -> if (genres.isNotEmpty()) TvGenreTiles(genres)
            "np_services" ->
                TvCardRow(
                    row,
                    title = stringResource(R.string.tv_discover_new_on_services),
                    seeAll = onSeeAllNew,
                    onFocusCard = onFocusCard,
                )
            else -> TvCardRow(row, typed = type == ExploreType.ALL, onFocusCard = onFocusCard)
        }
    }
}

@Composable
private fun TvGenreTiles(genres: List<GenreCard>) {
    val navigator = LocalTvNavigator.current
    TvShelf(stringResource(R.string.tv_discover_browse_by_genre)) {
        items(genres, key = { "${it.kind}-${it.id}" }) { genre ->
            TvBrowseTile(
                name = tvGenreText(genre.name),
                image = genre.backdrop,
                hue = tileHue(genre.name),
                onClick = { navigator.openGenre(genreCardType(genre).wire, genre.id) },
            )
        }
    }
}

fun LazyListScope.tvPosterGrid(
    cards: List<DiscoverCard>,
    rowId: String,
    typed: Boolean = false,
    onFocusCard: ((DiscoverCard) -> Unit)? = null,
) {
    items(cards.distinctBy { it.key }.chunked(TV_POSTER_COLUMNS), key = { "$rowId-${it.first().key}" }) { line ->
        Row(
            Modifier.padding(horizontal = TvMetrics.marginX),
            horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
        ) {
            line.forEach { card ->
                TvDiscoverCard(card, typed = typed, onFocus = onFocusCard?.let { focus -> { focus(card) } })
            }
        }
    }
}
