package ch.orellbuehler.acolyte.ui.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicAlbum
import ch.orellbuehler.acolyte.feature.music.MusicAlbumKind
import ch.orellbuehler.acolyte.feature.music.MusicArtistState
import ch.orellbuehler.acolyte.feature.music.MusicLibraryArtistState
import ch.orellbuehler.acolyte.feature.music.MusicMain
import ch.orellbuehler.acolyte.feature.music.MusicMark
import ch.orellbuehler.acolyte.feature.music.MusicRules
import ch.orellbuehler.acolyte.feature.music.MusicShelfKind
import ch.orellbuehler.acolyte.feature.music.MusicSong
import ch.orellbuehler.acolyte.feature.music.MusicStoreItem
import ch.orellbuehler.acolyte.feature.music.MusicVideo
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

class MusicArtistActions(
    val nav: MusicNav,
    val onStillReading: () -> Unit,
    val onAbout: () -> Unit,
    val onFollow: () -> Unit,
    val onAlbumFollow: (String) -> Unit,
    val onAskAll: () -> Unit,
)

class MusicArtistEmbed(
    val library: MusicLibraryArtistState,
    val onOpenLibrary: () -> Unit,
)

fun openArtistUrl(
    context: Context,
    url: String,
) {
    if (url.isEmpty()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun Long.artistCount(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

fun LazyListScope.musicArtistBody(
    state: MusicArtistState,
    actions: MusicArtistActions,
    embed: MusicArtistEmbed? = null,
) {
    item(key = "artist-hero") { ArtistHero(state) }
    state.status.error?.let { error -> item(key = "artist-error") { ErrorBanner(error) } }
    item(key = "artist-header") { ArtistHeader(state, actions, embed) }
    if (embed != null) item(key = "artist-embed") { ArtistEmbedInfo(embed.library) }
    if (state.top.isNotEmpty()) {
        item(key = "artist-top-title") { SectionHeader(stringResource(R.string.music_artist_top_songs)) }
        itemsIndexed(state.top, key = { index, song -> "artist-top-$index-${song.title}" }) { index, song ->
            SongRow(index + 1, song, state.mbid, actions.nav)
        }
    }
    if (state.essential.isNotEmpty()) {
        item(
            key = "artist-essential",
        ) { AlbumShelf(stringResource(R.string.music_artist_essential), state.essential, state, actions, false) }
    }
    state.shelves.forEach { shelf ->
        item(key = "artist-shelf-${shelf.kind}") {
            AlbumShelf(shelfTitle(shelf.kind), shelf.albums, state, actions, shelf.kind != MusicShelfKind.ALBUMS)
        }
    }
    if (state.shelves.isEmpty() && state.coming.isEmpty() && state.storeAlbums.isNotEmpty()) {
        item(key = "artist-store") {
            CardShelf(title = stringResource(R.string.music_artist_discography)) {
                itemsIndexed(state.storeAlbums) { _, album -> StoreCard(album) }
            }
        }
    }
    if (state.gap.isNotEmpty() && state.shelves.isEmpty()) {
        item(key = "artist-gap") {
            MusicArtistInfoNote(if (state.gap == "gap") stringResource(R.string.music_artist_gap_fallback) else state.gap)
        }
    }
    if (state.coming.isNotEmpty()) {
        item(key = "artist-coming-title") { SectionHeader(stringResource(R.string.music_artist_coming)) }
        items(state.coming, key = { "artist-coming-" + it.mbid }) { album -> ComingRow(album, state.mbid, actions) }
    }
    if (state.videos.isNotEmpty()) {
        item(key = "artist-videos") {
            CardShelf(title = stringResource(R.string.music_artist_videos)) {
                items(state.videos, key = { it.id }) { video -> VideoCard(video) }
            }
        }
    }
    if (state.listeners > 0 || state.rank > 0 || state.askAll != null) item(key = "artist-stats") { StatsBand(state, actions) }
    if (state.appearsOn.isNotEmpty()) {
        item(key = "artist-appears") {
            CardShelf(title = stringResource(R.string.music_artist_appears_on)) {
                items(state.appearsOn, key = { "appears-" + it.mbid }) { album ->
                    MusicAlbumCard(
                        album,
                        onClick = { actions.nav.openAlbum(album.mbid, album.artistMbid.ifEmpty { state.mbid }) },
                        subtitle = album.artistName.ifEmpty { album.year },
                    )
                }
            }
        }
    }
    if (state.related.isNotEmpty()) {
        item(key = "artist-related") {
            CardShelf(title = stringResource(R.string.music_artist_related)) {
                items(state.related, key = { "related-" + it.mbid }) { card ->
                    MusicArtistCircle(card, onClick = { actions.nav.openArtist(card.mbid) })
                }
            }
        }
    }
    if (state.similar.isNotEmpty() || state.similarLoading) {
        item(key = "artist-similar") {
            CardShelf(title = stringResource(R.string.music_artist_similar)) {
                if (state.similar.isEmpty()) {
                    items(PLACEHOLDERS) { PlaceholderCircle() }
                } else {
                    items(state.similar, key = { "similar-" + it.mbid }) { card ->
                        MusicArtistCircle(card, onClick = { actions.nav.openArtist(card.mbid) })
                    }
                }
            }
        }
    }
    if (state.bio != null || state.genres.isNotEmpty()) {
        item(key = "artist-about-card") { MusicArtistAboutCard(state, actions.onAbout) }
    }
}

private const val PLACEHOLDERS = 4

@Composable
private fun ArtistHero(state: MusicArtistState) {
    val tint = tintColor(state.tint)
    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(tint ?: MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (state.image.isNotEmpty()) {
            ServerImage(MusicRules.image(state.image, 250), null, Modifier.fillMaxSize())
        }
        ServerImage(MusicRules.image(state.wide, 1000), null, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, background))),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistHeader(
    state: MusicArtistState,
    actions: MusicArtistActions,
    embed: MusicArtistEmbed?,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
            MusicArt(state.image.ifEmpty { state.wide }, state.name, Modifier.size(104.dp), round = true, size = 250)
            Column(Modifier.weight(1f)) {
                Text(
                    state.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val line =
                    when {
                        state.listeners > 0 ->
                            pluralStringResource(
                                R.plurals.music_artist_listeners,
                                state.listeners.artistCount(),
                                MusicRules.shortCount(state.listeners),
                            )
                        state.plays > 0 -> stringResource(R.string.music_artist_plays, MusicRules.shortCount(state.plays))
                        else -> ""
                    }
                if (line.isNotEmpty()) {
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            MainButton(state, actions, embed)
            OutlinedButton(onClick = actions.onAbout) { Text(stringResource(R.string.music_artist_about), maxLines = 1) }
            if (state.followKnown) {
                if (state.following) {
                    FilledTonalButton(onClick = actions.onFollow) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.music_artist_following),
                            maxLines = 1,
                            modifier = Modifier.padding(start = Spacing.Small),
                        )
                    }
                } else {
                    OutlinedButton(onClick = actions.onFollow) { Text(stringResource(R.string.music_artist_follow), maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun MainButton(
    state: MusicArtistState,
    actions: MusicArtistActions,
    embed: MusicArtistEmbed?,
) {
    val nav = actions.nav
    if (embed != null) {
        when {
            embed.library.canManage ->
                Button(onClick = embed.onOpenLibrary) {
                    Text(stringResource(R.string.music_artist_main_in_library), maxLines = 1)
                }
            embed.library.canAsk ->
                Button(onClick = { nav.ask(state.mbid, state.name, "") }) {
                    Text(stringResource(R.string.music_artist_main_ask_albums), maxLines = 1)
                }
        }
        return
    }
    when (state.main) {
        MusicMain.NONE -> Unit
        MusicMain.OPEN_LIBRARY ->
            Button(onClick = { nav.openLibraryArtist(state.titleId, "") }) {
                Text(stringResource(R.string.music_artist_main_open), maxLines = 1)
            }
        MusicMain.ADD ->
            Button(onClick = { if (state.mainReady) nav.add(state.mbid, state.name) else actions.onStillReading() }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.music_artist_main_add), maxLines = 1, modifier = Modifier.padding(start = Spacing.Small))
            }
        MusicMain.ASK ->
            Button(onClick = { if (state.mainReady) nav.ask(state.mbid, state.name, "") else actions.onStillReading() }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.music_artist_main_ask), maxLines = 1, modifier = Modifier.padding(start = Spacing.Small))
            }
        MusicMain.ASKED ->
            Button(onClick = {}, enabled = false) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.music_artist_main_asked), maxLines = 1, modifier = Modifier.padding(start = Spacing.Small))
            }
    }
}

@Composable
private fun ArtistEmbedInfo(library: MusicLibraryArtistState) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        if (library.disambiguation.isNotEmpty()) {
            Text(library.disambiguation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (library.canManage) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                LinearProgressIndicator(progress = { library.progress }, modifier = Modifier.width(120.dp))
                Text(libraryProgressText(library), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
        if (library.nextRelease.isNotEmpty()) {
            Text(
                stringResource(R.string.music_artist_next_album, shortDay(library.nextRelease)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun libraryProgressText(library: MusicLibraryArtistState): String {
    val parts = mutableListOf<String>()
    if (library.waitingForTracks) {
        parts += stringResource(R.string.music_artist_progress_albums, library.includedCount, library.albumCount)
    } else {
        parts += pluralStringResource(R.plurals.music_artist_progress_tracks, library.totalTracks, library.haveTracks, library.totalTracks)
        parts +=
            if (library.missingTracks > 0) {
                stringResource(R.string.music_artist_progress_missing, library.missingTracks)
            } else {
                stringResource(R.string.music_artist_progress_complete)
            }
        if (library.notOutYet > 0) parts += stringResource(R.string.music_artist_progress_not_out, library.notOutYet)
    }
    return parts.joinToString(" · ")
}

@Composable
private fun shelfTitle(kind: MusicShelfKind): String =
    stringResource(
        when (kind) {
            MusicShelfKind.ALBUMS -> R.string.music_artist_shelf_albums
            MusicShelfKind.SINGLES -> R.string.music_artist_shelf_singles
            MusicShelfKind.COMPILATIONS -> R.string.music_artist_shelf_compilations
            MusicShelfKind.LIVE -> R.string.music_artist_shelf_live
            MusicShelfKind.OTHER -> R.string.music_artist_shelf_other
        },
    )

@Composable
private fun SongRow(
    number: Int,
    song: MusicSong,
    artistMbid: String,
    nav: MusicNav,
) {
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    val open = song.albumMbid.isNotEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = open) { nav.openAlbum(song.albumMbid, artistMbid) }
            .padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        MusicArt(song.image, song.title, Modifier.size(48.dp), size = 250)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (song.explicit) MusicExplicitBadge()
            }
            val sub = listOf(song.album, song.year).filter { it.isNotEmpty() }.joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (song.length.isNotEmpty()) {
            Text(song.length, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (open) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.music_artist_more_for, song.title))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.music_artist_open_album)) },
                        onClick = {
                            menu = false
                            nav.openAlbum(song.albumMbid, artistMbid)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.music_artist_copy_song)) },
                        onClick = {
                            menu = false
                            clipboard.setText(AnnotatedString(song.title))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumShelf(
    title: String,
    albums: List<MusicAlbum>,
    state: MusicArtistState,
    actions: MusicArtistActions,
    withKind: Boolean,
) {
    CardShelf(title = title) {
        items(albums, key = { it.mbid }) { album ->
            val plus = state.titleId <= 0 && state.mainReady && !album.asked && album.mark == MusicMark.NONE
            val canAdd = plus && state.main == MusicMain.ADD
            val canAsk = plus && state.main == MusicMain.ASK
            val subtitle =
                if (withKind) listOf(album.year, kindWord(album.kind)).filter { it.isNotEmpty() }.joinToString(" · ") else null
            MusicAlbumCard(
                album,
                onClick = { actions.nav.openAlbum(album.mbid, state.mbid) },
                subtitle = subtitle,
                action =
                    if (canAdd || canAsk) {
                        { AlbumPlus(album, canAdd, state, actions.nav) }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun AlbumPlus(
    album: MusicAlbum,
    add: Boolean,
    state: MusicArtistState,
    nav: MusicNav,
) {
    FilledTonalIconButton(
        onClick = { if (add) nav.add(state.mbid, state.name) else nav.ask(state.mbid, state.name, album.mbid) },
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription =
                if (add) {
                    stringResource(R.string.music_action_add_artist, state.name)
                } else {
                    stringResource(R.string.music_action_ask_artist, album.name)
                },
        )
    }
}

@Composable
private fun StoreCard(album: MusicStoreItem) {
    Column(Modifier.width(MusicCardWidth)) {
        MusicArt(album.image, album.title, Modifier.fillMaxWidth().aspectRatio(1f), size = 250)
        Row(
            Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                album.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (album.explicit) MusicExplicitBadge()
        }
        val parts = mutableListOf<String>()
        if (album.year.isNotEmpty()) parts += album.year
        if (album.kind != MusicAlbumKind.ALBUM) parts += kindWord(album.kind)
        if (album.tracks > 0) parts += pluralStringResource(R.plurals.music_artist_tracks, album.tracks, album.tracks)
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ComingRow(
    album: MusicAlbum,
    artistMbid: String,
    actions: MusicArtistActions,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { actions.nav.openAlbum(album.mbid, artistMbid) }
            .padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        MusicArt(album.image, album.name, Modifier.size(72.dp), size = 250)
        Column(Modifier.weight(1f)) {
            Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val out = if (album.date.isNotEmpty()) stringResource(R.string.music_line_out_on, shortDay(album.date)) else ""
            Text(
                listOf(kindWord(album.kind), out).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MusicBellButton(album.following, album.name) { actions.onAlbumFollow(album.mbid) }
    }
}

@Composable
private fun VideoCard(video: MusicVideo) {
    val context = LocalContext.current
    Column(
        Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(Radius.Card))
            .clickable { openArtistUrl(context, video.url) },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(Radius.Card))) {
            ServerImage(video.thumb, null, Modifier.fillMaxSize(), placeholderName = video.title)
            if (video.length.isNotEmpty()) {
                Text(
                    video.length,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.acolyte.onArt,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(MaterialTheme.acolyte.artScrim, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            video.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            stringResource(R.string.music_artist_video_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsBand(
    state: MusicArtistState,
    actions: MusicArtistActions,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        if (state.listeners > 0 || state.rank > 0) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
            ) {
                if (state.listeners > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.music_artist_listeners,
                            state.listeners.artistCount(),
                            MusicRules.shortCount(state.listeners),
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                if (state.rank > 0) {
                    Text(stringResource(R.string.music_artist_stats_rank, state.rank), style = MaterialTheme.typography.bodyMedium)
                }
                if (state.change != 0L) {
                    val up = state.change > 0
                    Text(
                        stringResource(
                            if (up) R.string.music_artist_stats_up else R.string.music_artist_stats_down,
                            MusicRules.shortCount(kotlin.math.abs(state.change)),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (up) MaterialTheme.acolyte.okText else MaterialTheme.acolyte.failText,
                    )
                }
            }
        }
        state.askAll?.let { all ->
            if (!confirm) {
                OutlinedButton(onClick = { confirm = true }, enabled = !state.asking) {
                    Text(stringResource(R.string.music_artist_ask_all, all.count))
                }
            } else {
                Text(
                    pluralStringResource(
                        if (all.gb) R.plurals.music_artist_ask_all_gb else R.plurals.music_artist_ask_all_mb,
                        all.count,
                        all.count,
                        all.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Button(
                        onClick = {
                            confirm = false
                            actions.onAskAll()
                        },
                        enabled = !state.asking,
                    ) { Text(stringResource(R.string.music_artist_ask_confirm)) }
                    TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderCircle() {
    Box(
        Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
fun MusicArtistInfoNote(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(Spacing.Medium))
    }
}
