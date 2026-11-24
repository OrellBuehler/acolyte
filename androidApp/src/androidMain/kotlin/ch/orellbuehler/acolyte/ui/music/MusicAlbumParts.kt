package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicAlbumCta
import ch.orellbuehler.acolyte.feature.music.MusicAlbumPresenter
import ch.orellbuehler.acolyte.feature.music.MusicAlbumState
import ch.orellbuehler.acolyte.feature.music.MusicHave
import ch.orellbuehler.acolyte.feature.music.MusicTrackRow
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlbumHero(
    state: MusicAlbumState,
    nav: MusicNav,
    vm: PresenterViewModel<MusicAlbumPresenter>,
) {
    val tint = tintColor(state.tint) ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val background = MaterialTheme.colorScheme.background
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.6f), background)))
            .padding(horizontal = Spacing.Screen, vertical = Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        MusicArt(state.image, state.name, Modifier.size(260.dp), size = 1200)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                state.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, false),
            )
            if (state.explicit) MusicExplicitBadge()
        }
        if (state.artistName.isNotEmpty()) {
            Text(
                state.artistName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = state.artistMbid.isNotEmpty()) { nav.openArtist(state.artistMbid) },
            )
        }
        val facts = listOf(kindWord(state.kind), state.year).filter { it.isNotEmpty() }.joinToString(" · ")
        Text(facts, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AlbumPrimaryButton(state, nav, vm)
            if (state.artistMbid.isNotEmpty() && state.artistName.isNotEmpty()) {
                OutlinedButton(onClick = { nav.openArtist(state.artistMbid) }) {
                    Text(stringResource(R.string.music_album_go_to, state.artistName), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (state.followKnown) {
                FilledTonalIconButton(onClick = { vm.run { toggleFollow() } }) {
                    Icon(
                        if (state.following) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                        contentDescription =
                            stringResource(
                                if (state.following) R.string.music_album_unfollow_artist else R.string.music_album_follow_artist,
                                state.artistName,
                            ),
                    )
                }
            }
        }
        if (state.mark != ch.orellbuehler.acolyte.feature.music.MusicMark.NONE || state.showCounts) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                MusicMarkChip(state.mark)
                if (state.showCounts) {
                    Text(
                        stringResource(R.string.music_album_lib_of, state.have, state.have + state.missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumPrimaryButton(
    state: MusicAlbumState,
    nav: MusicNav,
    vm: PresenterViewModel<MusicAlbumPresenter>,
) {
    val cta = state.cta
    if (cta == MusicAlbumCta.NONE) return
    val label =
        stringResource(
            when (cta) {
                MusicAlbumCta.OPEN_LIBRARY -> R.string.music_album_open_library
                MusicAlbumCta.MANAGE_LIBRARY -> R.string.music_album_manage_library
                MusicAlbumCta.ADD -> R.string.music_album_add
                MusicAlbumCta.ASK -> R.string.music_album_ask
                MusicAlbumCta.ASKED -> R.string.music_album_asked
                else -> R.string.music_album_open_artist
            },
        )
    Button(
        enabled = cta != MusicAlbumCta.ASKED,
        onClick = {
            if (!state.ctaReady) {
                vm.run { stillReading() }
            } else {
                when (cta) {
                    MusicAlbumCta.OPEN_LIBRARY, MusicAlbumCta.MANAGE_LIBRARY ->
                        if (state.partId >
                            0
                        ) {
                            nav.openLibraryAlbum(state.titleId, state.partId)
                        } else {
                            nav.openLibraryArtist(state.titleId, "")
                        }
                    MusicAlbumCta.ADD -> nav.add(state.artistMbid, state.artistName)
                    MusicAlbumCta.ASK ->
                        if (state.edition.isNotEmpty()) {
                            vm.run { askWithEdition() }
                        } else {
                            nav.ask(
                                state.artistMbid,
                                state.artistName,
                                state.mbid,
                            )
                        }
                    MusicAlbumCta.OPEN_ARTIST -> nav.openArtist(state.artistMbid)
                    else -> Unit
                }
            }
        },
    ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
internal fun AlbumGapNotice(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(
            if (text == "gap") stringResource(R.string.music_album_gap) else text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun AlbumCounts(state: MusicAlbumState) {
    if (!state.showCounts) return
    Text(
        stringResource(R.string.music_album_counts, state.have, state.missing),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
    )
}

@Composable
internal fun AlbumEmptyTracks(state: MusicAlbumState) {
    when {
        state.gap.isNotEmpty() -> Unit
        state.tracksWaiting -> repeat(6) { AlbumSkeletonRow() }
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
private fun AlbumSkeletonRow() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(
                    width = 20.dp,
                    height = 12.dp,
                ).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(Spacing.Large))
        Box(Modifier.weight(1f).height(14.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(Spacing.XLarge * 2))
    }
}

@Composable
internal fun AlbumTrackRow(
    row: MusicTrackRow,
    showDisc: Boolean,
    state: MusicAlbumState,
    nav: MusicNav,
    server: String,
    snackbar: SnackbarHostState,
) {
    if (showDisc) {
        Text(
            stringResource(R.string.music_album_disc, row.disc),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = Spacing.Screen).padding(top = Spacing.Medium, bottom = Spacing.XSmall),
        )
    }
    Row(
        Modifier.fillMaxWidth().padding(start = Spacing.Screen, end = Spacing.XSmall).height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(30.dp),
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                row.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, false),
            )
            if (row.explicit) MusicExplicitBadge()
        }
        HaveIcon(row.have)
        if (row.length.isNotEmpty()) {
            Text(
                row.length,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.Small),
            )
        }
        AlbumTrackMenu(row, state, nav, server, snackbar)
    }
}

@Composable
private fun HaveIcon(have: MusicHave) {
    val colors = MaterialTheme.acolyte
    when (have) {
        MusicHave.UNKNOWN -> Unit
        MusicHave.IN_LIBRARY ->
            Icon(
                Icons.Rounded.CheckCircle,
                stringResource(R.string.music_album_in_library),
                Modifier.padding(start = Spacing.Small).size(20.dp),
                colors.ok,
            )
        MusicHave.DOWNLOADING ->
            Icon(
                Icons.Rounded.Download,
                stringResource(R.string.music_album_downloading),
                Modifier.padding(start = Spacing.Small).size(20.dp),
                colors.info,
            )
        MusicHave.MISSING ->
            Icon(
                Icons.Rounded.Remove,
                stringResource(R.string.music_album_missing),
                Modifier.padding(start = Spacing.Small).size(20.dp),
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun AlbumTrackMenu(
    row: MusicTrackRow,
    state: MusicAlbumState,
    nav: MusicNav,
    server: String,
    snackbar: SnackbarHostState,
) {
    var open by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val songCopied = stringResource(R.string.music_album_song_copied)
    val linkCopied = stringResource(R.string.music_album_link_copied)
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.music_album_more_for, row.name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.music_album_copy_song)) },
                onClick = {
                    open = false
                    clipboard.setText(AnnotatedString(row.name))
                    scope.launch { snackbar.showSnackbar(songCopied) }
                },
            )
            if (server.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_album_copy_link)) },
                    onClick = {
                        open = false
                        clipboard.setText(AnnotatedString("$server/music/album/${state.mbid}"))
                        scope.launch { snackbar.showSnackbar(linkCopied) }
                    },
                )
            }
            if (state.titleId > 0) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_album_open_library)) },
                    onClick = {
                        open = false
                        if (state.partId >
                            0
                        ) {
                            nav.openLibraryAlbum(state.titleId, state.partId)
                        } else {
                            nav.openLibraryArtist(state.titleId, "")
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlbumFooter(
    state: MusicAlbumState,
    vm: PresenterViewModel<MusicAlbumPresenter>,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
    ) {
        val date = remember(state.date) { longDate(state.date) }
        if (date.isNotEmpty()) Hint(stringResource(R.string.music_album_released, date))
        if (state.songs > 0) {
            val songs = pluralStringResource(R.plurals.music_album_songs, state.songs, state.songs)
            val minutes = if (state.minutes > 0) pluralStringResource(R.plurals.music_album_minutes, state.minutes, state.minutes) else ""
            Hint(listOf(songs, minutes).filter { it.isNotEmpty() }.joinToString(", "))
        }
        if (state.label.isNotEmpty()) Hint(stringResource(R.string.music_album_label, state.year, state.label).trim())
        if (state.editions.size > 1) {
            var open by rememberSaveable { mutableStateOf(false) }
            val picked = state.editions.firstOrNull { it.mbid == state.edition }
            val title =
                listOfNotNull(pluralStringResource(R.plurals.music_album_editions, state.editions.size, state.editions.size), picked?.label)
                    .joinToString(" · ")
            TextButton(onClick = { open = !open }) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
            }
            if (open) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    FilterChip(
                        selected = state.edition.isEmpty(),
                        onClick = { if (state.edition.isNotEmpty()) vm.presenter.pickEdition(state.edition) },
                        label = { Text(stringResource(R.string.music_album_edition_usual)) },
                    )
                    state.editions.forEach { edition ->
                        FilterChip(
                            selected = edition.mbid == state.edition,
                            onClick = { vm.presenter.pickEdition(edition.mbid) },
                            label = {
                                Text(
                                    pluralStringResource(
                                        R.plurals.music_album_edition_tracks,
                                        edition.tracks,
                                        edition.label,
                                        edition.tracks,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            if (state.edition.isNotEmpty() && state.canAsk && !state.canAdd) Hint(stringResource(R.string.music_album_edition_asking))
        }
    }
}

private fun longDate(date: String): String =
    runCatching { LocalDate.parse(date.take(10)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) }.getOrDefault("")

internal fun LazyListScope.albumRails(
    state: MusicAlbumState,
    nav: MusicNav,
) {
    if (state.moreBy.isNotEmpty()) {
        item(key = "more-by") {
            CardShelf(title = stringResource(R.string.music_album_more_by, state.artistName)) {
                items(state.moreBy) { album ->
                    val subtitle = listOf(album.year, kindWord(album.kind)).filter { it.isNotEmpty() }.joinToString(" · ")
                    MusicAlbumCard(
                        album,
                        onClick = { nav.openAlbum(album.mbid, album.artistMbid.ifEmpty { state.artistMbid }) },
                        subtitle = subtitle,
                    )
                }
            }
        }
    }
    if (state.near.isNotEmpty()) {
        item(key = "near") {
            CardShelf(title = stringResource(R.string.music_album_fans)) {
                items(state.near) { album ->
                    MusicAlbumCard(
                        album,
                        onClick = { nav.openAlbum(album.mbid, album.artistMbid) },
                        subtitle = album.artistName.ifEmpty { album.year },
                    )
                }
            }
        }
    }
    if (state.similar.isNotEmpty()) {
        item(key = "similar") {
            CardShelf(title = stringResource(R.string.music_album_if_you_like, state.artistName)) {
                items(state.similar) { artist -> MusicArtistCircle(artist, onClick = { nav.openArtist(artist.mbid) }, size = 152.dp) }
            }
        }
    }
}
