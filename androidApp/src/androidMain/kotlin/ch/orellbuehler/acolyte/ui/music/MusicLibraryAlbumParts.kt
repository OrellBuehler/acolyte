package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.music.MusicAlbum
import ch.orellbuehler.acolyte.feature.music.MusicAlbumKind
import ch.orellbuehler.acolyte.feature.music.MusicAlbumStateKey
import ch.orellbuehler.acolyte.feature.music.MusicLibTrack
import ch.orellbuehler.acolyte.feature.music.MusicLibraryAlbumState
import ch.orellbuehler.acolyte.feature.music.MusicMark
import ch.orellbuehler.acolyte.feature.music.MusicPressingRow
import ch.orellbuehler.acolyte.feature.music.MusicRules
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

class LibraryAlbumActions(
    val search: () -> Unit,
    val toggleKeep: () -> Unit,
    val rename: () -> Unit,
    val delete: () -> Unit,
    val report: () -> Unit,
    val history: () -> Unit,
    val pressing: (Long) -> Unit,
)

@Composable
private fun albumStateText(key: MusicAlbumStateKey): String =
    stringResource(
        when (key) {
            MusicAlbumStateKey.NOT_INCLUDED -> R.string.music_album_state_not_included
            MusicAlbumStateKey.NO_TRACKS -> R.string.music_album_state_no_tracks
            MusicAlbumStateKey.COMPLETE -> R.string.music_album_state_complete
            MusicAlbumStateKey.NOT_OUT_YET -> R.string.music_album_state_not_out_yet
            MusicAlbumStateKey.NOT_KEPT -> R.string.music_album_state_not_kept
            MusicAlbumStateKey.PARTIAL -> R.string.music_album_state_partial
            MusicAlbumStateKey.MISSING -> R.string.music_album_state_missing
        },
    )

private fun albumStateTone(key: MusicAlbumStateKey): Tone =
    when (key) {
        MusicAlbumStateKey.COMPLETE -> Tone.OK
        MusicAlbumStateKey.PARTIAL, MusicAlbumStateKey.MISSING, MusicAlbumStateKey.NOT_OUT_YET -> Tone.WAIT
        else -> Tone.IDLE
    }

@Composable
internal fun RowScope.LibraryAlbumMenu(
    state: MusicLibraryAlbumState,
    actions: LibraryAlbumActions,
) {
    if (!state.canManage && !state.hasFile) return
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.music_album_lib_more_actions))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (state.canManage) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_album_lib_search_now)) },
                    onClick = {
                        open = false
                        actions.search()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_album_lib_keep)) },
                    enabled = state.included && !state.busy,
                    trailingIcon = { if (state.keep) Icon(Icons.Rounded.Check, contentDescription = null) },
                    onClick = {
                        open = false
                        actions.toggleKeep()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_album_lib_rename)) },
                    onClick = {
                        open = false
                        actions.rename()
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_album_lib_delete), color = MaterialTheme.acolyte.failText) },
                    onClick = {
                        open = false
                        actions.delete()
                    },
                )
            }
            if (state.hasFile) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_album_lib_report)) },
                    onClick = {
                        open = false
                        actions.report()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibraryAlbumHero(
    state: MusicLibraryAlbumState,
    nav: MusicNav,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.background)),
            ).padding(horizontal = Spacing.Screen, vertical = Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Box(Modifier.size(220.dp).clip(RoundedCornerShape(Radius.Card))) {
            MusicArt(state.artistImage, state.name, Modifier.fillMaxSize())
            ServerImage(MusicRules.image(state.cover, 500), null, Modifier.fillMaxSize())
        }
        if (state.artistName.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.section_music),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    state.artistName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, false).clickable { nav.openLibraryArtist(state.titleId, "") },
                )
            }
        }
        Text(state.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        val tracks =
            if (state.trackCount >
                0
            ) {
                pluralStringResource(R.plurals.music_album_lib_tracks, state.trackCount, state.trackCount)
            } else {
                ""
            }
        val duration =
            when {
                state.minutes <= 0 -> ""
                state.minutes >= 60 -> stringResource(R.string.music_album_lib_hours, state.minutes / 60, state.minutes % 60)
                else -> stringResource(R.string.music_album_lib_min, state.minutes)
            }
        val meta = listOf(state.year, kindWord(state.kind), tracks, duration).filter { it.isNotEmpty() }.joinToString(" · ")
        Text(
            meta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state.disambiguation.isNotEmpty()) {
            Text(
                state.disambiguation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.canManage) {
                StatusChip(albumStateText(state.stateKey), albumStateTone(state.stateKey))
            } else {
                StatusChip(
                    stringResource(if (state.complete) R.string.music_album_lib_in_yours else R.string.music_album_lib_not_here),
                    if (state.complete) Tone.OK else Tone.IDLE,
                )
            }
            if (state.dropped) StatusChip(stringResource(R.string.music_album_lib_not_listed), Tone.WAIT)
        }
    }
}

@Composable
internal fun LibraryAlbumEmpty(state: MusicLibraryAlbumState) {
    when {
        !state.included -> Hint(stringResource(R.string.music_album_lib_left_out), Modifier.padding(horizontal = Spacing.Screen))
        state.tracksWaiting -> repeat(6) { LibrarySkeletonRow() }
        else ->
            Text(
                stringResource(R.string.music_album_no_songs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
            )
    }
}

@Composable
private fun LibrarySkeletonRow() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(
                    width = 24.dp,
                    height = 12.dp,
                ).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(Spacing.Large))
        Box(Modifier.weight(1f).height(14.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(Spacing.XLarge * 2))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibraryTrackRow(
    track: MusicLibTrack,
    staff: Boolean,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Text(
            if (track.multiDisc) "${track.disc}-${track.number}" else track.number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                track.name.ifEmpty { stringResource(R.string.music_album_lib_name_unknown) },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (staff) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (track.slots.isEmpty()) {
                        StatusChip(track.stateLabel, track.tone)
                    } else {
                        track.slots.forEach { slot ->
                            val text =
                                if (track.slots.size > 1 &&
                                    slot.version.isNotEmpty()
                                ) {
                                    "${slot.version}: ${slot.label}"
                                } else {
                                    slot.label
                                }
                            StatusChip(text, slot.tone)
                        }
                    }
                }
            } else {
                val have = track.slots.any { it.tone == Tone.OK } || (track.slots.isEmpty() && track.tone == Tone.OK)
                StatusChip(
                    stringResource(if (have) R.string.music_album_lib_in_yours else R.string.music_album_lib_not_here),
                    if (have) Tone.OK else Tone.IDLE,
                )
            }
        }
        if (track.length.isNotEmpty()) {
            Text(
                track.length,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.Small),
            )
        }
    }
}

@Composable
internal fun LibraryAlbumCard(
    state: MusicLibraryAlbumState,
    actions: LibraryAlbumActions,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Large),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Text(stringResource(R.string.music_album_lib_library), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                StatusChip(albumStateText(state.stateKey), albumStateTone(state.stateKey))
                Text(
                    stringResource(R.string.music_album_lib_of, state.inLibrary, state.trackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = actions.search) { Text(stringResource(R.string.music_album_lib_search_now)) }
                TextButton(onClick = actions.history) { Text(stringResource(R.string.section_history)) }
            }
            HorizontalDivider()
            Text(stringResource(R.string.music_album_lib_settings), style = MaterialTheme.typography.titleSmall)
            SwitchRow(
                stringResource(R.string.music_album_lib_keep),
                state.keep,
                { actions.toggleKeep() },
                hint =
                    stringResource(
                        if (state.included) R.string.music_album_lib_keep_hint else R.string.music_album_lib_keep_left_out,
                    ),
                enabled = state.included && !state.busy,
            )
            if (state.pressings.size > 1) EditionRow(state, actions)
        }
    }
}

@Composable
private fun EditionRow(
    state: MusicLibraryAlbumState,
    actions: LibraryAlbumActions,
) {
    var open by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<MusicPressingRow?>(null) }
    val current = state.pressings.firstOrNull { it.chosen }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = Spacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.music_album_lib_edition), style = MaterialTheme.typography.bodyLarge)
                val how = stringResource(if (state.byRule) R.string.music_album_lib_by_rule else R.string.music_album_lib_by_hand)
                Hint(if (current != null) "$how · ${pressingLabel(current)}" else how)
            }
            Icon(Icons.Rounded.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.pressings.forEach { pressing ->
                DropdownMenuItem(
                    text = { Text(pressingLabel(pressing)) },
                    trailingIcon = { if (pressing.chosen) Icon(Icons.Rounded.Check, contentDescription = null) },
                    onClick = {
                        open = false
                        if (!pressing.chosen) pending = pressing
                    },
                )
            }
        }
    }
    pending?.let { pressing ->
        ConfirmDialog(
            title = stringResource(R.string.music_album_lib_follow_edition),
            text = stringResource(R.string.music_album_lib_follow_edition_body),
            confirm = stringResource(R.string.music_album_lib_use_edition),
            dismiss = stringResource(R.string.common_cancel),
            onConfirm = {
                pending = null
                actions.pressing(pressing.id)
            },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun pressingLabel(pressing: MusicPressingRow): String {
    val name = if (pressing.disambiguation.isNotEmpty()) "${pressing.name} (${pressing.disambiguation})" else pressing.name
    val tracks = pluralStringResource(R.plurals.music_album_lib_tracks, pressing.trackCount, pressing.trackCount)
    return listOf(name, pressing.year, pressing.country, pressing.formats, tracks).filter { it.isNotEmpty() }.joinToString(" · ")
}

internal fun LazyListScope.libraryAlbumMore(
    state: MusicLibraryAlbumState,
    nav: MusicNav,
) {
    if (state.moreBy.isEmpty()) return
    item(key = "more-by") {
        CardShelf(title = stringResource(R.string.music_album_more_by, state.artistName)) {
            items(state.moreBy) { entry ->
                val full = entry.tracks > 0 && entry.inLibrary >= entry.tracks
                val album =
                    MusicAlbum(
                        mbid = entry.mbid,
                        name = entry.name,
                        kind = MusicAlbumKind.ALBUM,
                        year = entry.year,
                        date = entry.date,
                        image = entry.cover,
                        explicit = false,
                        upcoming = entry.future,
                        mark = if (full) MusicMark.COMPLETE else MusicMark.NONE,
                        following = false,
                        partId = entry.id,
                        artistMbid = state.artistMbid,
                        artistName = state.artistName,
                        asked = false,
                    )
                val subtitle =
                    if (full) {
                        stringResource(R.string.music_album_lib_in_yours)
                    } else {
                        stringResource(R.string.music_album_lib_tracks_here, entry.inLibrary, entry.tracks)
                    }
                MusicAlbumCard(album, onClick = { nav.openLibraryAlbum(state.titleId, entry.id) }, subtitle = subtitle)
            }
        }
    }
}
