package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.music.MusicCardAction
import ch.orellbuehler.acolyte.feature.music.MusicItem
import ch.orellbuehler.acolyte.feature.music.MusicRowsState
import ch.orellbuehler.acolyte.feature.music.MusicRules
import ch.orellbuehler.acolyte.feature.music.MusicShape
import ch.orellbuehler.acolyte.feature.music.MusicShelfRow
import ch.orellbuehler.acolyte.feature.music.MusicTile
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.discover.LoadMoreEffect
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.util.Locale
import java.util.TimeZone

internal fun musicUtcOffset(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000

@Composable
internal fun RefreshOnResume(refresh: () -> Unit) {
    var first by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (first) first = false else refresh()
    }
}

@Composable
fun MusicRowsContent(
    state: MusicRowsState,
    nav: MusicNav,
    onRefresh: () -> Unit,
    onRegion: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.status.phase == Phase.FAILED) {
        MessageView(
            icon = Icons.Rounded.LibraryMusic,
            title = stringResource(R.string.music_rows_error_title),
            modifier = modifier,
            message = stringResource(R.string.music_rows_error_message),
            action = stringResource(R.string.common_retry),
            onAction = onRefresh,
        )
        return
    }
    StatusContent(
        status = state.status,
        isEmpty = state.isEmpty || state.kid,
        onRefresh = onRefresh,
        modifier = modifier,
        empty = { MusicRowsEmpty(state) },
    ) {
        val list = rememberLazyListState()
        LoadMoreEffect(list, state.more && !state.loadingMore, onLoadMore)
        LazyColumn(state = list, contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
            musicRowsItems(state, nav, onRegion)
            if (state.loadingMore) {
                item(key = "more") {
                    Row(Modifier.fillMaxWidth().padding(Spacing.Large), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicRowsEmpty(state: MusicRowsState) {
    if (state.kid) {
        MessageView(
            icon = Icons.Rounded.LibraryMusic,
            title = stringResource(R.string.music_rows_kid_title),
            message = stringResource(R.string.music_rows_kid_message),
        )
        return
    }
    val title =
        when {
            !state.isPage -> stringResource(R.string.music_rows_empty_explore)
            state.title.isNotEmpty() -> stringResource(R.string.music_rows_empty_page, state.title)
            else -> stringResource(R.string.music_rows_empty_page_plain)
        }
    MessageView(
        icon = Icons.Rounded.LibraryMusic,
        title = title,
        message = stringResource(R.string.music_rows_empty_message),
    )
}

fun LazyListScope.musicRowsItems(
    state: MusicRowsState,
    nav: MusicNav,
    onRegion: (String) -> Unit,
) {
    state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
    if (state.isPage) item(key = "band") { MusicBand(state) }
    val spotlight = listOfNotNull(state.hero) + state.spotlight
    if (spotlight.isNotEmpty()) {
        item(key = "spotlight") {
            CardShelf(title = stringResource(R.string.music_rows_spotlight)) {
                items(spotlight) { MusicSpotlightCard(it, nav) }
            }
        }
    }
    if (state.strip.isNotEmpty()) {
        item(key = "strip") {
            CardShelf(title = stringResource(R.string.discover_genres)) {
                items(state.strip) { MusicStripTile(it, nav) }
            }
        }
    }
    val grid = state.grid.flatMap { it.items }
    if (grid.isNotEmpty()) {
        item(key = "browse") {
            SectionHeader(
                stringResource(R.string.music_rows_browse),
                actionLabel = stringResource(R.string.music_rows_browse_all),
                onAction = { nav.openSection(Section.SEARCH) },
            )
        }
        musicTileRows(grid.map { it.toTile() }, nav, "grid")
    }
    if (state.thin) item(key = "thin") { MusicThin(state, nav) }
    val regionRow = state.rows.firstOrNull { it.type == "popular_country" } ?: state.rows.firstOrNull { it.type == "top_albums" }
    state.rows.forEachIndexed { index, row ->
        if (row.items.isEmpty()) return@forEachIndexed
        val select = state.regions.size > 1 && row === regionRow
        musicShelf(
            row = row,
            nav = nav,
            key = "row-$index-${row.id}",
            header = { if (select) MusicRegionSelect(state.region, state.regions, onRegion) },
        )
    }
}

fun LazyListScope.musicShelf(
    row: MusicShelfRow,
    nav: MusicNav,
    key: String,
    header: @Composable () -> Unit = {},
    onSeeAll: (() -> Unit)? = null,
    card: @Composable (MusicItem) -> Unit = { MusicItemCard(it, onClick = { nav.open(it) }, onAct = { nav.act(it) }) },
) {
    when (row.shape) {
        MusicShape.SONG -> item(key = key) { MusicSongList(row, nav, onSeeAll) }
        MusicShape.WIDE ->
            item(key = key) {
                CardShelf(
                    title = row.title,
                    subtitle = row.subtitle,
                    seeAllLabel = onSeeAll?.let { stringResource(R.string.common_see_all) },
                    onSeeAll = onSeeAll,
                    header = header,
                ) {
                    items(row.items) { MusicWideCard(it, nav) }
                }
            }
        else ->
            item(key = key) {
                CardShelf(
                    title = row.title,
                    subtitle = row.subtitle,
                    seeAllLabel = onSeeAll?.let { stringResource(R.string.common_see_all) },
                    onSeeAll = onSeeAll,
                    header = header,
                ) {
                    items(row.items) { card(it) }
                }
            }
    }
}

fun LazyListScope.musicTileRows(
    tiles: List<MusicTile>,
    nav: MusicNav,
    key: String,
) {
    val pairs = tiles.chunked(2)
    items(pairs.size, key = { "$key-$it" }) { index ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            pairs[index].forEach { tile -> MusicTileCard(tile, onClick = { nav.open(tile.target) }, modifier = Modifier.weight(1f)) }
            if (pairs[index].size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

internal fun MusicItem.toTile(): MusicTile =
    MusicTile(
        key,
        name,
        target,
        color.ifEmpty { MusicRules.tileColor(key) },
        hue,
        images.ifEmpty { listOf(image).filter { it.isNotEmpty() } },
    )

@Composable
fun MusicBand(state: MusicRowsState) {
    val hue = state.bandHue.toFloat()
    val brush =
        remember(hue) {
            Brush.linearGradient(listOf(Color.hsl(hue, 0.52f, 0.36f), Color.hsl((hue + 40f) % 360f, 0.60f, 0.18f)))
        }
    val kind =
        stringResource(
            when (state.tileKind) {
                "mood" -> R.string.music_rows_kind_mood
                "season" -> R.string.music_rows_kind_season
                "decade" -> R.string.music_rows_kind_decade
                "activity" -> R.string.music_rows_kind_activity
                else -> R.string.music_rows_kind_genre
            },
        )
    Box(Modifier.fillMaxWidth().background(brush).padding(Spacing.Large)) {
        Column(Modifier.fillMaxWidth(0.62f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(kind, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            Text(state.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                pluralStringResource(R.plurals.music_rows_count, state.rows.size, state.rows.size),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            if (state.leads.isNotEmpty()) {
                Text(
                    stringResource(R.string.music_rows_lead, state.leads.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(Modifier.align(Alignment.CenterEnd).size(width = 132.dp, height = 92.dp)) {
            state.collage.forEachIndexed { index, path ->
                MusicArt(
                    path,
                    "",
                    Modifier
                        .size(64.dp)
                        .offset(x = (index * 16).dp, y = (if (index % 2 == 0) 0 else 24).dp)
                        .graphicsLayer { rotationZ = (index - 2) * 6f },
                    size = 250,
                )
            }
        }
    }
}

@Composable
fun MusicSpotlightCard(
    item: MusicItem,
    nav: MusicNav,
    modifier: Modifier = Modifier,
) {
    val kicker = item.kicker.ifEmpty { item.badge }.ifEmpty { stringResource(R.string.music_rows_popular_now) }
    Box(
        modifier
            .width(280.dp)
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(Radius.Card))
            .clickable { nav.open(item) },
    ) {
        MusicArt(item.cover.ifEmpty { item.image }, item.name, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))),
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(0.8f).padding(Spacing.Medium)) {
            Text(kicker, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.explicit) MusicExplicitBadge()
            }
            if (item.artist.isNotEmpty() && item.artist != item.name) {
                Text(
                    item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MusicCornerMark(item.mark, Modifier.align(Alignment.TopEnd).padding(6.dp))
        MusicActButton(item, nav, Modifier.align(Alignment.BottomEnd).padding(6.dp))
    }
}

@Composable
fun MusicActButton(
    item: MusicItem,
    nav: MusicNav,
    modifier: Modifier = Modifier,
) {
    if (item.action == MusicCardAction.NONE) return
    FilledTonalIconButton(onClick = { nav.act(item) }, modifier = modifier.size(36.dp)) {
        Icon(
            Icons.Rounded.Add,
            contentDescription =
                if (item.action == MusicCardAction.ADD) {
                    stringResource(R.string.music_action_add_artist, item.artist.ifEmpty { item.name })
                } else {
                    stringResource(R.string.music_action_ask_artist, item.artist.ifEmpty { item.name })
                },
        )
    }
}

@Composable
fun MusicStripTile(
    item: MusicItem,
    nav: MusicNav,
) {
    val color = remember(item.color) { parseColor(item.color) }
    Box(
        Modifier
            .width(184.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(Radius.Card))
            .background(color)
            .clickable { nav.open(item) },
    ) {
        item.images.firstOrNull()?.let { path ->
            ServerImage(
                source = MusicRules.image(path, 250),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.55f),
            )
        }
        Text(
            item.name,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).padding(Spacing.Medium).fillMaxWidth(0.8f),
        )
    }
}

@Composable
fun MusicWideCard(
    item: MusicItem,
    nav: MusicNav,
) {
    Column(Modifier.width(240.dp).clip(RoundedCornerShape(Radius.Card)).clickable { nav.open(item) }) {
        MusicArt(item.image.ifEmpty { item.cover }, item.name, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Text(
            item.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        val line = MusicLineText(item.line)
        if (line.isNotEmpty()) {
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MusicSongList(
    row: MusicShelfRow,
    nav: MusicNav,
    onSeeAll: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.Small)) {
        SectionHeader(
            row.title,
            actionLabel = onSeeAll?.let { stringResource(R.string.common_see_all) },
            onAction = onSeeAll,
        )
        if (row.subtitle.isNotEmpty()) {
            Text(
                row.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.Small),
            )
        }
        row.items.forEach { MusicSongLine(it, nav, compact) }
    }
}

@Composable
fun MusicSongLine(
    item: MusicItem,
    nav: MusicNav,
    compact: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().clickable { nav.open(item) }.padding(horizontal = Spacing.Screen, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicArt(item.image, item.name, Modifier.size(44.dp), size = 250)
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.track.ifEmpty { item.name },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.explicit) MusicExplicitBadge()
            }
            Text(
                MusicLineText(item.line).ifEmpty { item.artist },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (compact) {
            MusicActButton(item, nav)
        } else {
            TextButton(onClick = { nav.open(item) }) { Text(stringResource(R.string.music_rows_ask_album), maxLines = 1) }
        }
    }
}

@Composable
private fun MusicThin(
    state: MusicRowsState,
    nav: MusicNav,
) {
    Column(Modifier.fillMaxWidth().padding(Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.music_rows_thin_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.music_rows_thin_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.similar.forEach { similar -> TextButton(onClick = { nav.open(similar.target) }) { Text(similar.name) } }
    }
}

@Composable
private fun MusicRegionSelect(
    region: String,
    regions: List<String>,
    onRegion: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = Spacing.Screen)) {
        OutlinedButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = Spacing.Medium)) {
            Text(regionName(region))
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = stringResource(R.string.music_rows_region))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            regions.forEach { code ->
                DropdownMenuItem(
                    text = { Text(regionName(code)) },
                    onClick = {
                        open = false
                        onRegion(code)
                    },
                )
            }
        }
    }
}

@Composable
private fun regionName(code: String): String =
    if (code.isEmpty() || code == "global") {
        stringResource(R.string.music_rows_region_global)
    } else {
        runCatching {
            Locale
                .Builder()
                .setRegion(code)
                .build()
                .displayCountry
        }.getOrDefault("").ifEmpty { code }
    }
