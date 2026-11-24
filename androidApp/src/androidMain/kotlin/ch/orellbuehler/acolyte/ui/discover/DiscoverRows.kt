package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.GenreCard
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.CardAction
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.ServiceChip
import ch.orellbuehler.acolyte.feature.discover.TopServices
import ch.orellbuehler.acolyte.feature.discover.TopSource
import ch.orellbuehler.acolyte.feature.discover.tileHue
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.components.BrowseTileCard
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.components.PosterCard
import ch.orellbuehler.acolyte.ui.components.RankedPoster
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.WideCard
import ch.orellbuehler.acolyte.ui.components.WideCardWidth
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun TitleWideCard(
    card: DiscoverCard,
    events: CardEvents,
    modifier: Modifier = Modifier,
    typed: Boolean = false,
    subtitle: String? = null,
    fill: Boolean = false,
) {
    val mark = card.mark
    WideCard(
        title = card.name,
        image = card.wideImage,
        subtitle = subtitle ?: cardLine(card, typed),
        logo = card.logo,
        mark = mark,
        markLabel = mark?.let { markText(it) }.orEmpty(),
        fresh = freshText(card.fresh),
        actionLabel = actionText(card),
        onAction = if (card.action != null) ({ events.act(card) }) else null,
        onLongClick = { events.longPress(card) },
        onClick = { events.open(card) },
        modifier = if (fill) modifier.fillMaxWidth() else modifier,
        width = if (fill) Dp.Unspecified else WideCardWidth,
    )
}

@Composable
fun TitlePosterCard(
    card: DiscoverCard,
    events: CardEvents,
    modifier: Modifier = Modifier,
) {
    val mark = card.mark
    PosterCard(
        title = card.name,
        image = card.posterImage,
        subtitle = cardLine(card),
        mark = mark,
        markLabel = mark?.let { markText(it) }.orEmpty(),
        fresh = freshText(card.fresh),
        actionLabel = actionText(card),
        onAction = if (card.action != null) ({ events.act(card) }) else null,
        onLongClick = { events.longPress(card) },
        onClick = { events.open(card) },
        modifier = modifier,
    )
}

@Composable
fun CardRowView(
    row: CardRow,
    events: CardEvents,
    modifier: Modifier = Modifier,
    title: String = row.title,
    typed: Boolean = false,
    onSeeAll: (() -> Unit)? = null,
) {
    CardShelf(title = title, subtitle = row.subtitle, onSeeAll = onSeeAll, modifier = modifier) {
        if (row.ranked) {
            itemsIndexed(row.cards, key = { _, card -> card.key }) { index, card ->
                RankedPoster(if (card.rank > 0) card.rank else index + 1) { TitlePosterCard(card, events) }
            }
        } else if (row.wide) {
            items(row.cards, key = { it.key }) { card -> TitleWideCard(card, events, typed = typed) }
        } else {
            items(row.cards, key = { it.key }) { card -> TitlePosterCard(card, events) }
        }
    }
}

@Composable
fun TopServicesView(
    top: TopServices,
    row: CardRow,
    title: String,
    events: CardEvents,
    onChip: (Long) -> Unit,
    onSeeAll: (() -> Unit)?,
) {
    CardShelf(
        title = title,
        onSeeAll = onSeeAll,
        header = {
            if (top.chips.size > 1) ServiceChips(top.chips, top.selected, onChip, showAll = false)
            val source =
                when (top.source) {
                    TopSource.TODAY -> stringResource(R.string.discover_top_today, top.serviceName)
                    TopSource.AS_OF -> stringResource(R.string.discover_top_as_of, top.serviceName, top.fetchedAt.take(10))
                    TopSource.POPULAR -> stringResource(R.string.discover_top_popular, top.serviceName)
                }
            if (top.serviceName.isNotEmpty()) {
                Text(
                    source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.Small),
                )
            }
        },
        footer = {
            if (top.attribution.isNotEmpty()) {
                Text(
                    top.attribution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.acolyte.textFaint,
                    modifier = Modifier.padding(horizontal = Spacing.Screen).padding(top = Spacing.XSmall),
                )
            }
        },
    ) {
        itemsIndexed(row.cards, key = { _, card -> card.key }) { index, card ->
            if (top.ranked) {
                RankedPoster(if (card.rank > 0) card.rank else index + 1) { TitlePosterCard(card, events) }
            } else {
                TitlePosterCard(card, events)
            }
        }
    }
}

@Composable
fun ServiceChips(
    chips: List<ServiceChip>,
    selected: Long,
    onChip: (Long) -> Unit,
    showAll: Boolean,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = Modifier.padding(bottom = Spacing.Small),
    ) {
        if (showAll) {
            item {
                FilterChip(selected = selected == 0L, onClick = { onChip(0) }, label = { Text(stringResource(R.string.discover_type_all)) })
            }
        }
        items(chips, key = { it.id }) { chip ->
            FilterChip(selected = chip.id == selected, onClick = { onChip(chip.id) }, label = { Text(chip.name) })
        }
    }
}

@Composable
fun GenreBrowseRow(
    genres: List<GenreCard>,
    onOpen: (GenreCard) -> Unit,
) {
    CardShelf(title = stringResource(R.string.discover_browse_by_genre)) {
        items(genres, key = { "${it.kind}-${it.id}" }) { genre ->
            BrowseTileCard(
                name = genreText(genre.name),
                image = genre.backdrop,
                hue = tileHue(genre.name),
                modifier = Modifier.width(200.dp).aspectRatio(16f / 9f),
                onClick = { onOpen(genre) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroCard(
    card: DiscoverCard,
    pills: List<String>,
    events: CardEvents,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.acolyte
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Screen)
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(Radius.Panel))
            .background(colors.placeholder),
    ) {
        ServerImage(
            source = card.wideImage ?: card.poster,
            contentDescription = null,
            size = ImageSize.HERO,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to colors.artScrimStrong)),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            if (card.logo != null) {
                ServerImage(
                    source = card.logo,
                    contentDescription = card.name,
                    size = ImageSize.POSTER_LARGE,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.7f).height(72.dp),
                )
            } else {
                Text(
                    card.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onArt,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val genres = card.genres.take(3).map { genreText(it) }
            val meta = listOf(if (card.year > 0) card.year.toString() else "") + genres.ifEmpty { listOf(kindText(card.kind)) }
            Text(
                meta
                    .filter {
                        it.isNotEmpty()
                    }.joinToString(" · "),
                color = colors.onArt.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (pills.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    pills.forEach { StatusChip(it, Tone.INFO, onArt = true) }
                }
            }
            if (card.overview.isNotEmpty()) {
                Text(
                    card.overview.take(220),
                    color = colors.onArt.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Button(onClick = { events.open(card) }, enabled = card.opensTitle) {
                    Text(stringResource(R.string.discover_more_info))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = Spacing.XSmall),
                    )
                }
                actionText(card)?.let { label ->
                    FilledTonalButton(onClick = { events.act(card) }) {
                        Icon(Icons.Rounded.Add, contentDescription = label)
                        Text(
                            stringResource(
                                if (card.action ==
                                    CardAction.ADD
                                ) {
                                    R.string.discover_action_add_short
                                } else {
                                    R.string.discover_action_ask_short
                                },
                            ),
                            modifier = Modifier.padding(start = Spacing.XSmall),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardGrid(
    cards: List<DiscoverCard>,
    events: CardEvents,
    typed: Boolean = false,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        cards.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                pair.forEach { card -> TitleWideCard(card, events, Modifier.weight(1f), typed = typed, fill = true) }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}
