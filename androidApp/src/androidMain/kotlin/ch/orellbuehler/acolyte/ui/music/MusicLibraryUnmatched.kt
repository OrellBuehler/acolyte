package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.music.MusicLeftoverRow
import ch.orellbuehler.acolyte.feature.music.MusicLeftoversPresenter
import ch.orellbuehler.acolyte.feature.music.MusicMatchState
import ch.orellbuehler.acolyte.feature.music.MusicNoteKind
import ch.orellbuehler.acolyte.feature.music.MusicPressingHintRow
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.library.LibraryFormat
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicUnmatchedSheet(
    presenter: MusicLeftoversPresenter,
    onClose: () -> Unit,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showAlone by rememberSaveable { mutableStateOf(false) }
    val undo = stringResource(R.string.music_lib_unm_undo)
    val note = state.note
    val noteText = note?.let { musicNoteText(it) }.orEmpty()
    LaunchedEffect(presenter) { presenter.load() }
    LaunchedEffect(note?.id) {
        if (note != null && noteText.isNotEmpty()) {
            val result = snackbar.showSnackbar(noteText, actionLabel = if (note.kind == MusicNoteKind.MATCHED) undo else null)
            if (result == SnackbarResult.ActionPerformed) presenter.undoMatch()
        }
    }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Box(Modifier.fillMaxWidth()) {
            LazyColumn(
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(bottom = Spacing.XLarge),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                item(key = "unm-head") {
                    Row(Modifier.padding(horizontal = Spacing.Screen), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.music_lib_unmatched_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { scope.launch { presenter.load() } }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.music_lib_unm_refresh))
                        }
                    }
                    Text(
                        stringResource(R.string.music_lib_unm_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                    )
                }
                state.status.error?.let { error -> item(key = "unm-error") { ErrorBanner(error) } }
                if (state.status.phase == Phase.LOADING) {
                    item(key = "unm-loading") {
                        Box(
                            Modifier.fillMaxWidth().padding(Spacing.Large),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                }
                items(state.pressings, key = { "unm-hint-${it.albumId}-${it.pressingId}" }) { hint ->
                    HintCard(hint, state.rescanning) { scope.launch { presenter.usePressing(hint) } }
                }
                items(state.open, key = { "unm-file-" + it.fileId }) { row ->
                    LeftoverCard(
                        row,
                        alone = false,
                        onLeave = { scope.launch { presenter.dismiss(row.fileId, true) } },
                        onMatch = { scope.launch { presenter.openMatch(row.fileId) } },
                    )
                }
                if (state.status.phase == Phase.READY && state.open.isEmpty() && state.alone.isEmpty()) {
                    item(key = "unm-nothing") {
                        Text(
                            stringResource(R.string.music_lib_unm_nothing),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = Spacing.Screen),
                        )
                    }
                }
                if (state.alone.isNotEmpty()) {
                    item(key = "unm-alone-head") {
                        Row(
                            Modifier.fillMaxWidth().clickable { showAlone = !showAlone }.padding(
                                horizontal = Spacing.Screen,
                                vertical = Spacing.Medium,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.music_lib_unm_alone, state.alone.size),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.rotate(if (showAlone) 180f else 0f),
                            )
                        }
                    }
                    if (showAlone) {
                        items(state.alone, key = { "unm-alone-" + it.fileId }) { row ->
                            LeftoverCard(
                                row,
                                alone = true,
                                onLeave = { scope.launch { presenter.dismiss(row.fileId, false) } },
                                onMatch = { scope.launch { presenter.openMatch(row.fileId) } },
                            )
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    state.match?.let { match -> MatchSheet(match, state.busy, presenter) }
}

@Composable
private fun HintCard(
    hint: MusicPressingHintRow,
    busy: Boolean,
    onUse: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.music_lib_unm_hint_title, hint.albumName), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.music_lib_unm_hint_body, hint.fits, hint.files, hint.pressingName, hint.trackCount, hint.chosen),
                style = MaterialTheme.typography.bodySmall,
            )
            FilledTonalButton(onClick = onUse, enabled = !busy) { Text(stringResource(R.string.music_lib_unm_use_pressing)) }
        }
    }
}

@Composable
private fun reasonText(row: MusicLeftoverRow): String =
    when (row.reason) {
        "music_two_album" -> stringResource(R.string.music_lib_reason_two_album)
        "music_album_off" -> stringResource(R.string.music_lib_reason_album_off)
        "music_no_track" -> stringResource(R.string.music_lib_reason_no_track)
        "music_unsure" -> stringResource(R.string.music_lib_reason_unsure)
        "music_has_file" -> stringResource(R.string.music_lib_reason_has_file)
        "music_duplicate" -> stringResource(R.string.music_lib_reason_duplicate)
        "music_other_artist" -> stringResource(R.string.music_lib_reason_other_artist, row.owner)
        "music_not_listed" -> stringResource(R.string.music_lib_reason_not_listed)
        "unmatched", "replaced_by_hand" -> stringResource(R.string.music_lib_reason_by_hand)
        else -> stringResource(R.string.music_lib_reason_no_album)
    }

@Composable
private fun LeftoverCard(
    row: MusicLeftoverRow,
    alone: Boolean,
    onLeave: () -> Unit,
    onMatch: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(row.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                LibraryFormat.bytes(row.size) + " · " + reasonText(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.closest.isNotEmpty()) {
                Text(
                    stringResource(R.string.music_lib_unm_closest, row.closest),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = onLeave) {
                    Text(stringResource(if (alone) R.string.music_lib_unm_list_again else R.string.music_lib_unm_leave))
                }
                FilledTonalButton(onClick = onMatch) { Text(stringResource(R.string.music_lib_unm_match)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchSheet(
    match: MusicMatchState,
    busy: Boolean,
    presenter: MusicLeftoversPresenter,
) {
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { presenter.closeMatch() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(bottom = Spacing.XLarge),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            item(key = "match-head") {
                Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(stringResource(R.string.music_lib_unm_match_title), style = MaterialTheme.typography.titleLarge)
                    Text(match.file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        LibraryFormat.bytes(match.file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.music_lib_unm_album), style = MaterialTheme.typography.titleSmall)
                    Box {
                        OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                match.albums
                                    .firstOrNull { it.id == match.albumId }
                                    ?.name
                                    .orEmpty(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            match.albums.forEach { album ->
                                DropdownMenuItem(
                                    text = { Text(album.name) },
                                    onClick = {
                                        menu = false
                                        scope.launch { presenter.pickAlbum(album.id) }
                                    },
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(
                            if (match.tracks.isEmpty() &&
                                !match.loading
                            ) {
                                R.string.music_lib_unm_no_tracks
                            } else {
                                R.string.music_lib_unm_pick_track
                            },
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (match.loading) CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            items(match.tracks, key = { "match-track-" + it.id }) { track ->
                Row(
                    Modifier.fillMaxWidth().clickable { presenter.pickTrack(track.id) }.padding(horizontal = Spacing.Screen),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = match.picked == track.id, onClick = { presenter.pickTrack(track.id) })
                    Text(
                        "${track.number}. ${track.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (track.hasFile) {
                        Text(
                            stringResource(R.string.music_lib_unm_has_file),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.acolyte.waitText,
                        )
                    }
                }
            }
            item(key = "match-actions") {
                Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    if (match.replaces) NoticeBanner(stringResource(R.string.music_lib_unm_replace_note))
                    Button(
                        onClick = { scope.launch { presenter.confirmMatch() } },
                        enabled = match.picked != 0L && !busy,
                        colors =
                            if (match.replaces) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.acolyte.fail)
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                    ) { Text(stringResource(if (match.replaces) R.string.music_lib_unm_replace else R.string.music_lib_unm_match)) }
                }
            }
        }
    }
}
