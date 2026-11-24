package ch.orellbuehler.acolyte.ui.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.requests.AlbumPickRow
import ch.orellbuehler.acolyte.feature.requests.AskReviewPresenter
import ch.orellbuehler.acolyte.feature.requests.AskReviewState
import ch.orellbuehler.acolyte.feature.requests.SeasonPickRow
import ch.orellbuehler.acolyte.feature.requests.Tick
import ch.orellbuehler.acolyte.feature.requests.askerNames
import ch.orellbuehler.acolyte.feature.requests.isFailed
import ch.orellbuehler.acolyte.feature.requests.isWaiting
import ch.orellbuehler.acolyte.feature.requests.life
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorView
import ch.orellbuehler.acolyte.ui.components.LoadingView
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskReviewSheet(
    askId: Long,
    onDismiss: () -> Unit,
    onApprove: (AskReviewPresenter) -> Unit,
    onDecline: () -> Unit,
    onRemove: () -> Unit,
    onOpenTitle: (String, String) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "review-$askId") { AskReviewViewModel(sdk, askId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val ask = state.ask
        when {
            state.status.phase == Phase.LOADING -> LoadingView(Modifier.padding(Spacing.XLarge))
            ask == null -> state.status.error?.let { ErrorView(it) }
            else ->
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        RequestPoster(ask.poster, ask.name, width = 84.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                            Text(titleWithYear(ask), style = MaterialTheme.typography.titleLarge)
                            Hint(factsLine(ask))
                            val ago = relativeTime(ask.askedAt).orEmpty()
                            Hint(stringResource(R.string.requests_asker_line, namesText(ask.askerNames), ago))
                            StatusChip(stageLabel(ask.life.stage), ask.life.tone)
                        }
                    }
                    if (!ask.isWaiting) AskLines(ask, approver = true, myId = 0)
                    if (ask.tmdbId.isNotEmpty() && ask.kind != "artist") {
                        TextButton(onClick = { onOpenTitle(ask.kind, ask.tmdbId) }) { Text(stringResource(R.string.requests_open_title)) }
                    }
                    if (ask.isWaiting) {
                        Text(stringResource(R.string.requests_what_to_add), style = MaterialTheme.typography.titleMedium)
                        if (ask.kind == "movie") Hint(stringResource(R.string.requests_the_movie))
                    } else if (state.seasons.isNotEmpty() || state.albums.isNotEmpty()) {
                        Text(stringResource(R.string.requests_what_asked), style = MaterialTheme.typography.titleMedium)
                    }
                    PartsHead(state)
                    state.seasons.forEach { row -> SeasonRowView(row, ask.isWaiting, viewModel) }
                    state.albums.forEach { row -> AlbumRowView(row, ask.isWaiting, viewModel) }
                    if (ask.isWaiting && state.showsQuality) QualityRow(state, viewModel)
                    if (ask.isWaiting) {
                        Hint(
                            when {
                                state.empty -> stringResource(R.string.requests_tick_one)
                                state.changed -> stringResource(R.string.requests_adds_ticked)
                                ask.kind == "movie" -> stringResource(R.string.requests_adds_movie)
                                else -> stringResource(R.string.requests_adds_what, askedWhat(ask))
                            },
                        )
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        IconButton(onClick = onRemove) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.requests_remove),
                                tint = MaterialTheme.acolyte.failText,
                            )
                        }
                        Box(Modifier.weight(1f))
                        if (ask.isWaiting || ask.isFailed) {
                            OutlinedButton(onClick = onDecline) { Text(stringResource(R.string.requests_decline)) }
                        }
                        if (ask.isWaiting) {
                            Button(
                                onClick = { onApprove(viewModel.presenter) },
                                enabled = state.canApprove,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.acolyte.ok,
                                        contentColor = MaterialTheme.colorScheme.surface,
                                    ),
                            ) { Text(stringResource(R.string.requests_approve)) }
                        }
                    }
                }
        }
    }
}

@Composable
private fun PartsHead(state: AskReviewState) {
    if (state.seasons.isNotEmpty()) {
        val seasons = pluralStringResource(R.plurals.requests_seasons, state.seasons.size, state.seasons.size)
        val episodes = pluralStringResource(R.plurals.requests_episodes, state.keptEpisodes, state.keptEpisodes)
        val head =
            if (state.keptSeasons == state.seasons.size) {
                stringResource(R.string.requests_parts_all, seasons)
            } else {
                stringResource(R.string.requests_parts_some, state.keptSeasons, seasons)
            }
        Hint(if (state.keptEpisodes > 0) "$head · $episodes" else head)
    }
    if (state.albums.isNotEmpty()) {
        val counts = state.albumCounts.map { (kind, n) -> albumKindText(kind, n) }.joinToString(" · ")
        val songs =
            when {
                state.allSongs == 0 -> ""
                state.keptSongs == state.allSongs -> pluralStringResource(R.plurals.requests_songs, state.allSongs, state.allSongs)
                else -> stringResource(R.string.requests_some_songs, state.keptSongs, state.allSongs)
            }
        Hint(listOf(counts, songs).filter { it.isNotEmpty() }.joinToString(" · "))
    }
}

private fun Tick.toggleable(): ToggleableState =
    when (this) {
        Tick.ALL -> ToggleableState.On
        Tick.SOME -> ToggleableState.Indeterminate
        Tick.NONE -> ToggleableState.Off
    }

@Composable
private fun SeasonRowView(
    row: SeasonPickRow,
    editable: Boolean,
    viewModel: AskReviewViewModel,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TriStateCheckbox(state = row.tick.toggleable(), onClick = { viewModel.toggleSeason(row.number) }, enabled = editable)
            Column(Modifier.weight(1f).clickable { viewModel.expandSeason(row.number) }) {
                Text(seasonName(row.number), style = MaterialTheme.typography.bodyLarge)
                Hint(
                    when {
                        row.all == 0 -> stringResource(R.string.requests_episodes_not_listed)
                        row.asked >= row.all -> stringResource(R.string.requests_all_episodes, row.all)
                        else -> stringResource(R.string.requests_some_episodes, row.asked, row.all)
                    },
                )
            }
            IconButton(onClick = { viewModel.expandSeason(row.number) }) {
                Icon(
                    if (row.expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(R.string.requests_episodes),
                )
            }
        }
        if (row.expanded) {
            when {
                row.loading -> CircularProgressIndicator(Modifier.padding(start = Spacing.XLarge))
                row.failed -> FailText(stringResource(R.string.requests_episodes_failed), Modifier.padding(start = Spacing.XLarge))
                row.episodes.isEmpty() -> Hint(stringResource(R.string.requests_episodes_none), Modifier.padding(start = Spacing.XLarge))
                else ->
                    row.episodes.forEach { ep ->
                        Row(Modifier.padding(start = Spacing.XLarge), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ep.ticked,
                                onCheckedChange = { viewModel.toggleEpisode(row.number, ep.number) },
                                enabled = editable,
                            )
                            Text(
                                if (ep.name.isNotBlank()) {
                                    "${ep.number}. ${ep.name}"
                                } else {
                                    stringResource(
                                        R.string.requests_episode,
                                        ep.number,
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun AlbumRowView(
    row: AlbumPickRow,
    editable: Boolean,
    viewModel: AskReviewViewModel,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TriStateCheckbox(state = row.tick.toggleable(), onClick = { viewModel.toggleAlbum(row.mbid) }, enabled = editable)
            Column(Modifier.weight(1f).clickable { viewModel.expandAlbum(row.mbid) }) {
                Text(row.name, style = MaterialTheme.typography.bodyLarge)
                val facts =
                    listOfNotNull(
                        row.year.takeIf { it > 0 }?.toString(),
                        albumKindWord(row.kind),
                        row.editions.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.requests_editions, it, it) },
                        row.songs.size
                            .takeIf { it > 0 }
                            ?.let { pluralStringResource(R.plurals.requests_songs, it, it) },
                    )
                Hint(facts.joinToString(" · "))
                if (!editable && row.tick == Tick.NONE) Hint(stringResource(R.string.requests_left_out))
            }
            if (row.songs.isNotEmpty()) {
                IconButton(onClick = { viewModel.expandAlbum(row.mbid) }) {
                    Icon(
                        if (row.expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(R.string.requests_songs),
                    )
                }
            }
        }
        if (row.expanded) {
            row.songs.forEach { song ->
                Row(Modifier.padding(start = Spacing.XLarge), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = song.ticked, onCheckedChange = { viewModel.toggleSong(row.mbid, song.key) }, enabled = editable)
                    Text(
                        "${if (song.disc > 1) "${song.disc}-" else ""}${song.number}. ${song.name}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityRow(
    state: AskReviewState,
    viewModel: AskReviewViewModel,
) {
    val ask = state.ask ?: return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Text(stringResource(R.string.requests_quality), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            state.pills.forEach { pill ->
                FilterChip(
                    selected = pill.key == state.selected,
                    onClick = { viewModel.select(pill.key) },
                    label = { Text(if (pill.rulesId > 0) pill.name else qualityName(pill.line, pill.name)) },
                )
            }
        }
        val asked = state.pills.firstOrNull { it.line == ask.quality.ifEmpty { if (ask.fourK) "4k" else "standard" } && it.rulesId == 0L }
        val askedName = asked?.let { qualityName(it.line, it.name) } ?: qualityName(ask.quality, ask.qualityName)
        Hint(stringResource(R.string.requests_asked_for_quality, namesText(ask.askerNames), askedName))
        if (state.extraOnly) {
            val pill = state.selectedPill
            if (pill != null) Hint(stringResource(R.string.requests_adds_only_copy, qualityName(pill.line, pill.name)))
        }
    }
}
