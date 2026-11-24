package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.music.MusicAlbumStateKey
import ch.orellbuehler.acolyte.feature.music.MusicLibAlbum
import ch.orellbuehler.acolyte.feature.music.MusicLibFilter
import ch.orellbuehler.acolyte.feature.music.MusicLibGroup
import ch.orellbuehler.acolyte.feature.music.MusicLibraryArtistPresenter
import ch.orellbuehler.acolyte.feature.music.MusicLibraryArtistState
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.library.LibraryFormat
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

class MusicLibraryActions(
    val nav: MusicNav,
    val presenter: MusicLibraryArtistPresenter,
    val act: (suspend MusicLibraryArtistPresenter.() -> Unit) -> Unit,
    val openSheet: (String) -> Unit,
    val askMove: (Long) -> Unit,
)

private val Groups =
    listOf(
        MusicLibGroup.ALBUMS,
        MusicLibGroup.EPS,
        MusicLibGroup.SINGLES,
        MusicLibGroup.LIVE,
        MusicLibGroup.COMPILATIONS,
        MusicLibGroup.OTHER,
    )

@Composable
fun MusicLibraryTab(
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        state.status.error?.let { error -> item(key = "lib-error") { ErrorBanner(error) } }
        item(key = "lib-status") { LibraryStatusCard(state, actions) }
        if (state.unmatched > 0) item(key = "lib-unmatched") { UnmatchedRow(state.unmatched) { actions.openSheet("unmatched") } }
        item(key = "lib-releases") { ReleasesHeader(state, actions) }
        if (state.shown.isEmpty()) {
            item(key = "lib-empty") {
                Text(
                    stringResource(
                        if (state.includedCount == 0 &&
                            state.filter == MusicLibFilter.INCLUDED
                        ) {
                            R.string.music_lib_empty_none
                        } else {
                            R.string.music_lib_empty
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.Screen),
                )
            }
        }
        Groups.forEach { group ->
            val albums = state.shown.filter { it.group == group }
            if (albums.isNotEmpty()) {
                item(key = "lib-group-$group") {
                    Text(
                        stringResource(R.string.music_lib_group_count, groupName(group), albums.size),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                    )
                }
                items(albums, key = { "lib-album-" + it.id }) { album -> LibraryAlbumRow(album, state, actions) }
            }
        }
        item(key = "lib-settings") { MusicLibrarySettings(state, actions) }
        musicLibraryAsks(state)
    }
}

@Composable
private fun groupName(group: MusicLibGroup): String =
    stringResource(
        when (group) {
            MusicLibGroup.ALBUMS -> R.string.music_lib_group_albums
            MusicLibGroup.EPS -> R.string.music_lib_group_eps
            MusicLibGroup.SINGLES -> R.string.music_lib_group_singles
            MusicLibGroup.LIVE -> R.string.music_lib_group_live
            MusicLibGroup.COMPILATIONS -> R.string.music_lib_group_compilations
            MusicLibGroup.OTHER -> R.string.music_lib_group_other
        },
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryStatusCard(
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                StatusChip(
                    stringResource(R.string.music_lib_status_tracks, state.haveTracks, state.totalTracks),
                    if (state.missingTracks == 0 && state.totalTracks > 0) Tone.OK else Tone.WAIT,
                )
                if (state.fullReleases > 0) {
                    FactChip(pluralStringResource(R.plurals.music_lib_full_releases, state.fullReleases, state.fullReleases))
                }
            }
            if (state.waitingForTracks) {
                Text(
                    stringResource(R.string.music_lib_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.gateReason.isNotEmpty()) Text(state.gateReason, style = MaterialTheme.typography.bodySmall)
            if (state.noDownloader.isNotEmpty()) {
                Text(
                    stringResource(R.string.music_lib_no_downloader, state.noDownloader),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.acolyte.failText,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                if (state.folderPath.isNotEmpty()) FactChip(state.folderPath)
                if (state.sizeBytes > 0) FactChip(LibraryFormat.bytes(state.sizeBytes))
                if (state.rulesName.isNotEmpty()) FactChip(state.rulesName)
                FactChip(stringResource(if (state.keepUpToDate) R.string.music_lib_fact_kept else R.string.music_lib_fact_not_kept))
                val total = state.albums.count { it.included }
                if (total > 0) FactChip(stringResource(R.string.music_lib_fact_kept_releases, state.keptReleases, total))
            }
            if (state.missingTracks > 0) {
                Button(onClick = { actions.act { searchAll() } }) { Text(stringResource(R.string.music_lib_get_missing)) }
            }
        }
    }
}

@Composable
private fun FactChip(text: String) {
    Surface(color = MaterialTheme.acolyte.chip, shape = CircleShape) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun UnmatchedRow(
    count: Int,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Row(Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.music_lib_unmatched_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    pluralStringResource(R.plurals.music_lib_unmatched_files, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleasesHeader(
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    Column {
        SectionHeader(stringResource(R.string.music_lib_releases))
        FlowRow(
            Modifier.padding(horizontal = Spacing.Screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.filter == MusicLibFilter.INCLUDED,
                onClick = { actions.presenter.setFilter(MusicLibFilter.INCLUDED) },
                label = { Text(stringResource(R.string.music_lib_filter_included)) },
            )
            if (state.leftOut > 0) {
                FilterChip(
                    selected = state.filter == MusicLibFilter.LEFT_OUT,
                    onClick = { actions.presenter.setFilter(MusicLibFilter.LEFT_OUT) },
                    label = { Text(stringResource(R.string.music_lib_filter_left_out, state.leftOut)) },
                )
                FilterChip(
                    selected = state.filter == MusicLibFilter.ALL,
                    onClick = { actions.presenter.setFilter(MusicLibFilter.ALL) },
                    label = { Text(stringResource(R.string.music_lib_filter_all, state.albums.size)) },
                )
            }
            if (state.expanded.isEmpty()) {
                TextButton(onClick = { actions.act { expandAll() } }) { Text(stringResource(R.string.music_lib_expand_all)) }
            } else {
                TextButton(onClick = { actions.presenter.collapseAll() }) { Text(stringResource(R.string.music_lib_collapse_all)) }
            }
        }
    }
}

@Composable
private fun albumStateText(key: MusicAlbumStateKey): String =
    stringResource(
        when (key) {
            MusicAlbumStateKey.NOT_INCLUDED -> R.string.music_lib_state_not_included
            MusicAlbumStateKey.NO_TRACKS -> R.string.music_lib_state_no_tracks
            MusicAlbumStateKey.COMPLETE -> R.string.music_lib_state_complete
            MusicAlbumStateKey.NOT_OUT_YET -> R.string.music_lib_not_out_yet
            MusicAlbumStateKey.NOT_KEPT -> R.string.music_lib_state_not_kept
            MusicAlbumStateKey.PARTIAL -> R.string.music_lib_state_partial
            MusicAlbumStateKey.MISSING -> R.string.music_lib_state_missing
        },
    )

@Composable
private fun LibraryAlbumRow(
    album: MusicLibAlbum,
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    val open = album.id in state.expanded
    val busy = album.id in state.busy
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { actions.act { expand(album.id) } }
                .padding(horizontal = Spacing.Small, vertical = Spacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            IconButton(onClick = { actions.act { toggleKeepAlbum(album.id) } }, enabled = album.included && !busy) {
                Icon(
                    if (album.keep) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription =
                        stringResource(
                            when {
                                !album.included -> R.string.music_lib_keep_disabled
                                album.keep -> R.string.music_lib_keep_on
                                else -> R.string.music_lib_keep_off
                            },
                        ),
                    tint = if (album.keep) MaterialTheme.acolyte.okText else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MusicArt(album.cover, album.name, Modifier.size(46.dp), size = 250)
            Column(Modifier.weight(1f)) {
                Text(album.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val parts = mutableListOf<String>()
                if (album.year.isNotEmpty()) parts += album.year
                if (album.tracks > 0) parts += pluralStringResource(R.plurals.music_lib_row_tracks, album.tracks, album.tracks)
                if (album.future) parts += stringResource(R.string.music_lib_not_out_yet)
                if (album.dropped) parts += stringResource(R.string.music_lib_dropped)
                parts += albumStateText(album.state)
                Text(
                    parts.distinct().joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (album.tracks > 0) CountPill(album)
            IconButton(onClick = { actions.act { searchAlbum(album.id) } }, enabled = album.missing > 0 && album.included) {
                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.music_lib_search_album))
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = stringResource(if (open) R.string.music_lib_collapse else R.string.music_lib_expand),
                modifier = Modifier.rotate(if (open) 180f else 0f),
            )
        }
        if (open) LibraryTracks(album, state, actions)
    }
}

@Composable
private fun CountPill(album: MusicLibAlbum) {
    val tone =
        when {
            album.inLibrary >= album.tracks -> Tone.OK
            album.inLibrary > 0 -> Tone.WAIT
            else -> Tone.IDLE
        }
    Surface(color = MaterialTheme.acolyte.chip, shape = CircleShape) {
        Text(
            "${album.inLibrary} / ${album.tracks}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.acolyte.text(tone) ?: MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun LibraryTracks(
    album: MusicLibAlbum,
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    val tracks = state.tracksOf(album.id)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = Spacing.Screen)) {
        if (album.id in state.loadingTracks) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = Spacing.Small))
        tracks.forEach { track ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (track.multiDisc) "${track.disc}-${track.number}" else track.number.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                )
                Text(
                    track.name.ifEmpty { stringResource(R.string.music_lib_track_n, track.number) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (track.length.isNotEmpty()) {
                    Text(
                        track.length,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.Small),
                    )
                }
                if (track.stateLabel.isNotEmpty()) StatusChip(track.stateLabel, track.tone)
                if (track.slotId > 0) {
                    IconButton(onClick = { actions.act { searchTrack(album.id, track.slotId, track.name) } }) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.music_lib_search_track))
                    }
                }
            }
        }
        TextButton(onClick = { actions.nav.openLibraryAlbum(state.titleId, album.id) }) {
            Text(stringResource(R.string.music_lib_open_album))
        }
    }
}

fun LazyListScope.musicLibraryAsks(state: MusicLibraryArtistState) {
    if (state.asks.isEmpty()) return
    item(key = "lib-asks-title") { SectionHeader(stringResource(R.string.music_lib_requests)) }
    items(state.asks.size, key = { "lib-ask-$it" }) { index ->
        val ask = state.asks[index]
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall)) {
            Text(
                stringResource(R.string.music_lib_ask_by, ask.askedBy, shortDay(ask.askedAt)),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (ask.albums.isNotEmpty()) {
                Text(
                    stringResource(R.string.music_lib_ask_for, ask.albums),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (ask.decidedBy.isNotEmpty()) {
                Text(
                    stringResource(R.string.music_lib_ask_decided, ask.decidedBy, shortDay(ask.decidedAt)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (ask.stateLabel.isNotEmpty()) {
                Text(
                    stringResource(R.string.music_lib_ask_now, ask.stateLabel),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (ask.note.isNotEmpty()) {
                Text(
                    stringResource(R.string.music_lib_ask_note, ask.note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
