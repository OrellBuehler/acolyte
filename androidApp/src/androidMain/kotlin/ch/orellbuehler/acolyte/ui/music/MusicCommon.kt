package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.discover.CardMark
import ch.orellbuehler.acolyte.feature.discover.MarkLabel
import ch.orellbuehler.acolyte.feature.discover.MarkState
import ch.orellbuehler.acolyte.feature.music.MusicAlbum
import ch.orellbuehler.acolyte.feature.music.MusicAlbumKind
import ch.orellbuehler.acolyte.feature.music.MusicArtistCard
import ch.orellbuehler.acolyte.feature.music.MusicCardAction
import ch.orellbuehler.acolyte.feature.music.MusicCardKind
import ch.orellbuehler.acolyte.feature.music.MusicItem
import ch.orellbuehler.acolyte.feature.music.MusicLine
import ch.orellbuehler.acolyte.feature.music.MusicLineKind
import ch.orellbuehler.acolyte.feature.music.MusicMark
import ch.orellbuehler.acolyte.feature.music.MusicNote
import ch.orellbuehler.acolyte.feature.music.MusicNoteKind
import ch.orellbuehler.acolyte.feature.music.MusicProblem
import ch.orellbuehler.acolyte.feature.music.MusicRules
import ch.orellbuehler.acolyte.feature.music.MusicTile
import ch.orellbuehler.acolyte.feature.music.tone
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.OwnedMark
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.discover.shortDate
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.time.LocalDate

val MusicCardWidth: Dp = 148.dp

@Composable
fun MusicArt(
    path: String?,
    name: String,
    modifier: Modifier = Modifier,
    round: Boolean = false,
    size: Int = 500,
) {
    val shape = if (round) CircleShape else RoundedCornerShape(Radius.Card)
    ServerImage(
        source = path?.takeIf { it.isNotEmpty() }?.let { MusicRules.image(it, size) },
        contentDescription = null,
        placeholderName = name,
        modifier = modifier.clip(shape),
    )
}

@Composable
fun MusicMarkChip(
    mark: MusicMark,
    modifier: Modifier = Modifier,
) {
    if (mark == MusicMark.NONE) return
    StatusChip(markText(mark), mark.tone, modifier)
}

@Composable
fun markText(mark: MusicMark): String =
    stringResource(
        when (mark) {
            MusicMark.NONE -> R.string.music_mark_none
            MusicMark.COMPLETE -> R.string.music_mark_complete
            MusicMark.PARTIAL -> R.string.music_mark_partial
            MusicMark.DOWNLOADING -> R.string.music_mark_downloading
            MusicMark.MISSING -> R.string.music_mark_missing
            MusicMark.REQUESTED -> R.string.music_mark_requested
            MusicMark.APPROVED -> R.string.music_mark_approved
        },
    )

private fun MusicMark.toCardMark(): CardMark? =
    when (this) {
        MusicMark.NONE, MusicMark.MISSING -> null
        MusicMark.COMPLETE -> CardMark(MarkState.AVAILABLE, MarkLabel.IN_LIBRARY)
        MusicMark.PARTIAL -> CardMark(MarkState.PARTIAL, MarkLabel.PARTLY_AVAILABLE)
        MusicMark.DOWNLOADING -> CardMark(MarkState.DOWNLOADING, MarkLabel.DOWNLOADING)
        MusicMark.REQUESTED -> CardMark(MarkState.REQUESTED, MarkLabel.REQUESTED)
        MusicMark.APPROVED -> CardMark(MarkState.REQUESTED, MarkLabel.APPROVED_WAITING)
    }

@Composable
fun MusicCornerMark(
    mark: MusicMark,
    modifier: Modifier = Modifier,
) {
    val card = mark.toCardMark() ?: return
    OwnedMark(card, markText(mark), modifier)
}

@Composable
fun MusicLineText(line: MusicLine): String {
    val artist = line.artist
    val tail =
        when (line.kind) {
            MusicLineKind.NONE -> ""
            MusicLineKind.TEXT -> line.text
            MusicLineKind.OUT_ON -> stringResource(R.string.music_line_out_on, shortDay(line.date))
            MusicLineKind.OUT_NOW -> stringResource(R.string.music_line_out_now)
            MusicLineKind.RELEASED_ON -> stringResource(R.string.music_line_released_on, shortDay(line.date))
            MusicLineKind.LISTENS ->
                pluralStringResource(
                    R.plurals.music_line_listens,
                    line.number.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                    MusicRules.shortCount(line.number),
                )
            MusicLineKind.ARRIVED_ON -> stringResource(R.string.music_line_arrived_on, shortDay(line.date))
            MusicLineKind.ARTIST_YEAR -> line.text
            MusicLineKind.ARTIST_TEXT -> ""
            MusicLineKind.ASKED -> stringResource(R.string.music_line_asked)
            MusicLineKind.APPROVED -> stringResource(R.string.music_line_approved)
            MusicLineKind.TURNED_DOWN -> stringResource(R.string.music_line_turned_down)
            MusicLineKind.ARRIVED -> stringResource(R.string.music_line_arrived)
        }
    return listOf(artist, tail).filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
fun shortDay(date: String): String = runCatching { shortDate(LocalDate.parse(date.take(10))) }.getOrDefault(date)

@Composable
fun kindWord(kind: MusicAlbumKind): String =
    stringResource(
        when (kind) {
            MusicAlbumKind.ALBUM -> R.string.music_kind_album
            MusicAlbumKind.EP -> R.string.music_kind_ep
            MusicAlbumKind.SINGLE -> R.string.music_kind_single
            MusicAlbumKind.BROADCAST -> R.string.music_kind_broadcast
            MusicAlbumKind.COMPILATION -> R.string.music_kind_compilation
            MusicAlbumKind.LIVE -> R.string.music_kind_live
            MusicAlbumKind.SOUNDTRACK -> R.string.music_kind_soundtrack
            MusicAlbumKind.REMIX -> R.string.music_kind_remix
            MusicAlbumKind.OTHER -> R.string.music_kind_other
        },
    )

@Composable
fun MusicExplicitBadge() {
    Text(
        stringResource(R.string.music_explicit_badge),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp)
                .semantics { contentDescription = "explicit" },
    )
}

@Composable
fun MusicAlbumCard(
    album: MusicAlbum,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = MusicCardWidth,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier.width(width).clip(RoundedCornerShape(Radius.Card)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            MusicArt(album.image, album.name, Modifier.fillMaxWidth().aspectRatio(1f), size = if (width > 160.dp) 500 else 250)
            MusicCornerMark(album.mark, Modifier.align(Alignment.TopEnd).padding(6.dp))
            if (action != null) Box(Modifier.align(Alignment.BottomEnd).padding(6.dp)) { action() }
        }
        Row(
            Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                album.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (album.explicit) MusicExplicitBadge()
        }
        val sub = subtitle ?: album.year
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
}

@Composable
fun MusicItemCard(
    item: MusicItem,
    onClick: () -> Unit,
    onAct: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = MusicCardWidth,
    round: Boolean = item.kind == MusicCardKind.ARTIST,
) {
    Column(modifier.width(width).clip(RoundedCornerShape(Radius.Card)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            MusicArt(
                item.image.ifEmpty {
                    item.images.firstOrNull().orEmpty()
                },
                item.name,
                Modifier.fillMaxWidth().aspectRatio(1f),
                round = round,
                size = 500,
            )
            MusicCornerMark(item.mark, Modifier.align(Alignment.TopEnd).padding(6.dp))
            if (item.action != MusicCardAction.NONE) {
                FilledTonalIconButton(onClick = onAct, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(36.dp)) {
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
        }
        Row(
            Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleSmall,
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.fresh.isNotEmpty()) {
            Text(
                item.fresh,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MusicBellButton(
    following: Boolean,
    name: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            if (following) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
            contentDescription =
                if (following) {
                    stringResource(
                        R.string.music_follow_album_on,
                        name,
                    )
                } else {
                    stringResource(R.string.music_follow_album_off, name)
                },
        )
    }
}

@Composable
fun MusicArtistCircle(
    artist: MusicArtistCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    Column(
        modifier.width(size).clip(RoundedCornerShape(Radius.Card)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MusicArt(artist.image, artist.name, Modifier.size(size), round = true, size = 250)
        Text(
            artist.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (artist.note.isNotEmpty()) {
            Text(
                artist.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MusicTileCard(
    tile: MusicTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = remember(tile.color) { parseColor(tile.color) }
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(Radius.Card))
            .background(color)
            .clickable(onClick = onClick),
    ) {
        Text(
            tile.name,
            style = MaterialTheme.typography.titleMedium,
            color = androidx.compose.ui.graphics.Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).padding(Spacing.Medium).fillMaxWidth(0.7f),
        )
        tile.covers.firstOrNull()?.let { cover ->
            MusicArt(cover, tile.name, Modifier.align(Alignment.BottomEnd).padding(Spacing.Small).size(64.dp), size = 250)
        }
    }
}

fun parseColor(hex: String): androidx.compose.ui.graphics.Color =
    runCatching {
        androidx.compose.ui.graphics
            .Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(
        androidx.compose.ui.graphics
            .Color(0xFF4E6075),
    )

fun tintColor(tint: Long): androidx.compose.ui.graphics.Color? =
    if (tint ==
        0L
    ) {
        null
    } else {
        androidx.compose.ui.graphics
            .Color(0xFF000000L or tint)
    }

@Composable
fun MusicProblemView(
    problem: MusicProblem,
    album: Boolean,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MessageView(
        icon = Icons.Rounded.Notifications,
        title = musicProblemText(problem, album),
        modifier = modifier,
        action = if (problem == MusicProblem.OFFLINE || problem == MusicProblem.SLOW) stringResource(R.string.common_retry) else null,
        onAction = onRetry,
    )
}

@Composable
fun musicProblemText(
    problem: MusicProblem,
    album: Boolean,
): String =
    stringResource(
        when (problem) {
            MusicProblem.NONE, MusicProblem.SLOW -> if (album) R.string.music_problem_slow_album else R.string.music_problem_slow_artist
            MusicProblem.OFFLINE -> R.string.music_problem_offline
            MusicProblem.NOT_FOUND -> if (album) R.string.music_problem_not_found_album else R.string.music_problem_not_found_artist
            MusicProblem.BAD_LINK -> if (album) R.string.music_problem_bad_album else R.string.music_problem_bad_artist
            MusicProblem.FORBIDDEN -> if (album) R.string.music_problem_forbidden_album else R.string.music_problem_forbidden_artist
        },
    )

@Composable
fun musicNoteText(note: MusicNote): String =
    when (note.kind) {
        MusicNoteKind.FAILED -> note.error?.let { errorText(it) }.orEmpty()
        MusicNoteKind.STILL_READING -> stringResource(R.string.music_note_still_reading)
        MusicNoteKind.FOLLOW_FAILED -> stringResource(R.string.music_note_follow_failed)
        MusicNoteKind.ASKED_REQUESTED -> stringResource(R.string.music_note_asked_requested, note.text)
        MusicNoteKind.ASKED_APPROVED -> stringResource(R.string.music_note_asked_approved, note.text)
        MusicNoteKind.ASKED_JOINED -> stringResource(R.string.music_note_asked_joined, note.text)
        MusicNoteKind.ASKED_ALREADY -> stringResource(R.string.music_note_asked_already, note.text)
        MusicNoteKind.ADDED -> stringResource(R.string.music_note_added, note.text)
        MusicNoteKind.ALREADY_IN_LIBRARY -> stringResource(R.string.music_note_already_in_library, note.text)
        MusicNoteKind.NOT_FOUND -> stringResource(R.string.music_note_not_found, note.text)
        MusicNoteKind.KEEP_ALBUM_ON, MusicNoteKind.KEEP_ARTIST_ON -> stringResource(R.string.music_note_keep_on, note.text)
        MusicNoteKind.KEEP_ALBUM_OFF, MusicNoteKind.KEEP_ARTIST_OFF -> stringResource(R.string.music_note_keep_off, note.text)
        MusicNoteKind.SEARCHING -> stringResource(R.string.music_note_searching, note.text)
        MusicNoteKind.SEARCHING_ALL -> stringResource(R.string.music_note_searching_all)
        MusicNoteKind.KINDS_SAVED -> stringResource(R.string.music_note_kinds_saved)
        MusicNoteKind.REFRESHING -> stringResource(R.string.music_note_refreshing, note.text)
        MusicNoteKind.MOVING -> stringResource(R.string.music_note_moving, note.text)
        MusicNoteKind.PRESSING_CHANGED -> stringResource(R.string.music_note_pressing_changed)
        MusicNoteKind.PRESSING_FAILED -> stringResource(R.string.music_note_pressing_failed)
        MusicNoteKind.SETTINGS_SAVED -> stringResource(R.string.music_note_settings_saved)
        MusicNoteKind.MATCHED -> pluralStringResource(R.plurals.music_note_matched, note.count, note.count)
        MusicNoteKind.NOT_MATCHED -> pluralStringResource(R.plurals.music_note_not_matched, note.count, note.count)
        MusicNoteKind.UNMATCHED -> stringResource(R.string.music_note_unmatched)
    }

@Composable
fun MusicNoteEffect(
    note: MusicNote?,
    snackbar: SnackbarHostState,
) {
    val text = note?.let { musicNoteText(it) }.orEmpty()
    LaunchedEffect(note?.id) {
        if (note != null && text.isNotEmpty()) snackbar.showSnackbar(text)
    }
}
