package ch.orellbuehler.acolyte.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.music.MusicCardKind
import ch.orellbuehler.acolyte.feature.music.MusicItem
import ch.orellbuehler.acolyte.feature.music.MusicNewState
import ch.orellbuehler.acolyte.feature.music.MusicShape
import ch.orellbuehler.acolyte.feature.music.MusicShelfRow
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val TRACKS = "np_music_tracks"
private const val TRACKS_SHOWN = 5

@Composable
fun MusicNewContent(
    nav: MusicNav,
    typeBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sdk = LocalSdk.current
    val model = viewModel(key = "music-new") { PresenterViewModel(sdk.musicNew("global", musicUtcOffset())) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var seeAll by rememberSaveable { mutableStateOf<String?>(null) }
    MusicNoteEffect(state.note, snackbar)
    RefreshOnResume { model.run { refresh() } }
    BackHandler(enabled = seeAll != null) { seeAll = null }
    val onFollow: (String) -> Unit = { key -> model.run { toggleFollow(key) } }
    val row = seeAll?.let { id -> state.rows.firstOrNull { it.id == id } }
    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            typeBar()
            if (row != null) {
                MusicSeeAll(row, nav, onBack = { seeAll = null }, onFollow = onFollow)
            } else {
                MusicNewBody(
                    state = state,
                    nav = nav,
                    onRefresh = { model.run { refresh() } },
                    onFollow = onFollow,
                    onSeeAll = { seeAll = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MusicNewBody(
    state: MusicNewState,
    nav: MusicNav,
    onRefresh: () -> Unit,
    onFollow: (String) -> Unit,
    onSeeAll: (String) -> Unit,
    modifier: Modifier,
) {
    if (state.status.phase == Phase.FAILED) {
        MessageView(
            icon = Icons.Rounded.LibraryMusic,
            title = stringResource(R.string.music_new_error_title),
            modifier = modifier,
            message = stringResource(R.string.music_new_error_message),
            action = stringResource(R.string.common_retry),
            onAction = onRefresh,
        )
        return
    }
    val bare = state.hero.isEmpty() && state.rows.isEmpty()
    StatusContent(
        status = state.status,
        isEmpty = state.kid || state.isEmpty || (state.building && bare),
        onRefresh = onRefresh,
        modifier = modifier,
        empty = { MusicNewEmpty(state) },
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
            state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
            if (state.building) item(key = "building") { NoticeBanner(stringResource(R.string.music_new_building_message)) }
            if (state.hero.isNotEmpty()) item(key = "hero") { MusicNewHero(state.hero, nav, onFollow) }
            state.rows.forEachIndexed { index, row ->
                if (row.items.isEmpty()) return@forEachIndexed
                val key = "row-$index-${row.id}"
                if (row.id == TRACKS) {
                    item(
                        key = key,
                    ) { MusicSongList(row.copy(items = row.items.take(TRACKS_SHOWN)), nav, { onSeeAll(row.id) }, compact = true) }
                } else {
                    musicShelf(row, nav, key, onSeeAll = { onSeeAll(row.id) }, card = { MusicNewCard(it, nav, onFollow) })
                }
            }
            if (state.tiles.isNotEmpty()) {
                item(key = "explore-more") {
                    SectionHeader(
                        stringResource(R.string.music_new_explore_more),
                        actionLabel = stringResource(R.string.music_rows_browse_all),
                        onAction = { nav.openSection(Section.SEARCH) },
                    )
                }
                musicTileRows(state.tiles.take(4), nav, "tiles")
            }
        }
    }
}

@Composable
private fun MusicNewEmpty(state: MusicNewState) {
    when {
        state.kid ->
            MessageView(
                icon = Icons.Rounded.LibraryMusic,
                title = stringResource(R.string.music_rows_kid_title),
                message = stringResource(R.string.music_rows_kid_message),
            )
        state.building ->
            MessageView(
                icon = Icons.Rounded.LibraryMusic,
                title = stringResource(R.string.music_new_building_title),
                message = stringResource(R.string.music_new_building_message),
            )
        else ->
            MessageView(
                icon = Icons.Rounded.LibraryMusic,
                title = stringResource(R.string.music_new_empty_title),
                message = stringResource(R.string.music_new_empty_message),
            )
    }
}

@Composable
private fun MusicSeeAll(
    row: MusicShelfRow,
    nav: MusicNav,
    onBack: () -> Unit,
    onFollow: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        item(key = "back") {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.music_new_title))
                }
                Text(row.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (row.id == TRACKS || row.shape == MusicShape.SONG) {
            items(row.items) { MusicSongLine(it, nav, compact = true) }
        } else {
            val pairs = row.items.chunked(2)
            items(pairs.size, key = { "pair-$it" }) { index ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    pairs[index].forEach { MusicNewCard(it, nav, onFollow, Modifier.weight(1f)) }
                    if (pairs[index].size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MusicNewCard(
    item: MusicItem,
    nav: MusicNav,
    onFollow: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = MusicCardWidth,
) {
    Box(modifier) {
        MusicItemCard(item, onClick = { nav.open(item) }, onAct = { nav.act(item) }, modifier = Modifier.fillMaxWidth(), width = width)
        if (item.upcoming && item.kind == MusicCardKind.ALBUM) {
            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                MusicBellButton(item.following, item.name) { onFollow(item.key) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicNewHero(
    hero: List<MusicItem>,
    nav: MusicNav,
    onFollow: (String) -> Unit,
) {
    val list = rememberLazyListState()
    LazyRow(
        state = list,
        flingBehavior = rememberSnapFlingBehavior(list),
        contentPadding = PaddingValues(horizontal = Spacing.Screen, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        items(hero) { MusicHeroCard(it, nav, onFollow) }
    }
}

@Composable
private fun MusicHeroCard(
    item: MusicItem,
    nav: MusicNav,
    onFollow: (String) -> Unit,
) {
    Column(Modifier.width(300.dp).clip(RoundedCornerShape(Radius.Card))) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(Radius.Card))) {
            MusicArt(item.cover.ifEmpty { item.image }, item.name, Modifier.fillMaxSize().clickable { nav.open(item) })
            MusicCornerMark(item.mark, Modifier.align(Alignment.TopEnd).padding(6.dp))
            if (item.upcoming && item.kind == MusicCardKind.ALBUM) {
                Box(Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
                    MusicBellButton(item.following, item.name) { onFollow(item.key) }
                }
            } else {
                MusicActButton(item, nav, Modifier.align(Alignment.BottomEnd).padding(6.dp))
            }
        }
        Text(
            heroEyebrow(item),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (item.explicit) MusicExplicitBadge()
        }
        val line = MusicLineText(item.line)
        if (line.isNotEmpty()) {
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun heroEyebrow(item: MusicItem): String {
    val day = runCatching { LocalDate.parse(item.date.take(10)) }.getOrNull()
    val until = day?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    return when {
        day != null && until != null && item.upcoming && until in 0..6 ->
            stringResource(R.string.music_new_out_day, day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()))
        item.badge.isNotEmpty() -> item.badge
        item.kicker.isNotEmpty() && !item.kicker.startsWith("New") -> item.kicker
        else -> stringResource(R.string.music_rows_popular_now)
    }
}
