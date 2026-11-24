package ch.orellbuehler.acolyte.ui.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.requests.AskOutcome
import ch.orellbuehler.acolyte.feature.requests.AskSeasonRow
import ch.orellbuehler.acolyte.feature.requests.AskSheetState
import ch.orellbuehler.acolyte.feature.requests.EpisodesLoad
import ch.orellbuehler.acolyte.feature.requests.MusicMode
import ch.orellbuehler.acolyte.feature.requests.PillNote
import ch.orellbuehler.acolyte.feature.requests.QualityPill
import ch.orellbuehler.acolyte.feature.requests.ShowMode
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorView
import ch.orellbuehler.acolyte.ui.components.LoadingView
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSheet(
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0f) }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            content = content,
        )
    }
}

@Composable
fun SheetFooter(
    primary: String,
    enabled: Boolean,
    busy: Boolean,
    onCancel: () -> Unit,
    onPrimary: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
        Spacer(Modifier.width(Spacing.Small))
        Button(onClick = onPrimary, enabled = enabled && !busy) {
            if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text(primary)
        }
    }
}

@Composable
fun DoneView(
    title: String,
    message: String,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.Large), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.acolyte.ok, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = Spacing.Medium))
        Hint(message, Modifier.padding(top = Spacing.XSmall))
        Button(onClick = onClose, modifier = Modifier.padding(top = Spacing.Large)) { Text(stringResource(R.string.requests_done)) }
    }
}

@Composable
fun outcomeTitle(
    outcome: AskOutcome,
    name: String,
    fourK: Boolean,
): String =
    when (outcome) {
        AskOutcome.APPROVED -> stringResource(R.string.requests_outcome_approved, name)
        AskOutcome.JOINED, AskOutcome.ALREADY -> stringResource(R.string.requests_outcome_already, name)
        AskOutcome.TURNED_DOWN -> stringResource(R.string.requests_outcome_turned_down, name)
        AskOutcome.REQUESTED ->
            stringResource(
                if (fourK) R.string.requests_outcome_requested_four_k else R.string.requests_outcome_requested,
                name,
            )
    }

@Composable
fun outcomeMessage(outcome: AskOutcome): String =
    stringResource(
        when (outcome) {
            AskOutcome.APPROVED -> R.string.requests_outcome_searching
            AskOutcome.JOINED, AskOutcome.ALREADY -> R.string.requests_outcome_on_it
            AskOutcome.TURNED_DOWN -> R.string.requests_outcome_on_services
            AskOutcome.REQUESTED -> R.string.requests_outcome_waiting
        },
    )

@Composable
private fun AskError(error: ApiError?) {
    if (error != null) FailText(askErrorText(error))
}

@Composable
private fun FollowRow(
    follow: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!follow) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = follow, onCheckedChange = onChange)
        Column {
            Text(stringResource(R.string.requests_follow), style = MaterialTheme.typography.bodyLarge)
            Hint(stringResource(R.string.requests_follow_hint))
        }
    }
}

@Composable
private fun QualityPills(
    pills: List<QualityPill>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text(stringResource(R.string.requests_quality), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        pills.forEach { pill ->
            val note =
                when (pill.note) {
                    PillNote.IN_LIBRARY -> stringResource(R.string.requests_pill_in_library)
                    PillNote.REQUESTED -> stringResource(R.string.requests_pill_requested)
                    PillNote.NONE -> null
                }
            FilterChip(
                selected = pill.key == selected,
                onClick = { onSelect(pill.key) },
                enabled = !pill.disabled,
                label = { Text(listOfNotNull(qualityName(pill.line, pill.name), note).joinToString(" · ")) },
            )
        }
    }
}

@Composable
fun AskSheet(
    kind: String,
    tmdb: String,
    fourK: Boolean,
    onClose: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "ask-$kind-$tmdb-$fourK") { AskViewModel(sdk, kind, tmdb, fourK) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    RouteSheet(onClose) {
        val outcome = state.outcome
        when {
            state.status.phase == Phase.LOADING -> LoadingView(Modifier.padding(Spacing.XLarge))
            state.status.phase == Phase.FAILED -> state.status.error?.let { ErrorView(it, onRetry = viewModel::retry) }
            outcome != null -> {
                val what = if (state.isShow && state.mode != ShowMode.ALL) "${state.name} (${showModeWords(state)})" else state.name
                DoneView(outcomeTitle(outcome, what, state.outcomeFourK), outcomeMessage(outcome), onClose)
            }
            else -> {
                Text(
                    stringResource(R.string.requests_ask_title, if (state.year > 0) "${state.name} (${state.year})" else state.name),
                    style = MaterialTheme.typography.titleLarge,
                )
                val brief = state.brief
                Hint(
                    when {
                        brief != null && state.isShow ->
                            stringResource(R.string.requests_ask_already_show, seasonsText(brief.seasons), brief.stateLabel)
                        brief != null && state.joining -> stringResource(R.string.requests_ask_already, brief.stateLabel)
                        else -> stringResource(R.string.requests_ask_hear)
                    },
                )
                if (state.neverAdd) FailText(stringResource(R.string.requests_error_never_add))
                if (state.isShow) ShowChoices(state, viewModel)
                if (state.showsQuality) QualityPills(state.pills, state.selected, viewModel::select)
                state.alsoPills.forEach { pill ->
                    Row(
                        Modifier.fillMaxWidth().clickable { viewModel.toggleAlso(pill.key) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = pill.key in state.also, onCheckedChange = { viewModel.toggleAlso(pill.key) })
                        Text(stringResource(R.string.requests_also_ask, qualityName(pill.line, pill.name)))
                    }
                }
                FollowRow(state.follow, viewModel::setFollow)
                AskError(state.error)
                SheetFooter(
                    primary = askLabel(state),
                    enabled = state.canSubmit,
                    busy = state.busy,
                    onCancel = onClose,
                    onPrimary = viewModel::submit,
                )
            }
        }
    }
}

@Composable
private fun showModeWords(state: AskSheetState): String =
    when (state.mode) {
        ShowMode.FUTURE -> stringResource(R.string.requests_seasons_future)
        ShowMode.ALL -> stringResource(R.string.requests_seasons_whole)
        ShowMode.CHOOSE -> chooseSummary(state)
    }

@Composable
private fun chooseSummary(state: AskSheetState): String {
    val parts = mutableListOf<String>()
    if (state.pickedSeasons.isNotEmpty()) parts += seasonsText(state.pickedSeasons)
    if (state.pickedEpisodes > 0) {
        parts += pluralStringResource(R.plurals.requests_episodes_count, state.pickedEpisodes, state.pickedEpisodes)
    }
    return parts.joinToString(" + ")
}

@Composable
private fun askLabel(state: AskSheetState): String =
    when {
        !state.isShow -> stringResource(R.string.requests_ask_for, state.name)
        state.mode == ShowMode.ALL -> stringResource(R.string.requests_ask_all_seasons)
        state.mode == ShowMode.FUTURE -> stringResource(R.string.requests_ask_future)
        state.pickedSeasons.isEmpty() && state.pickedEpisodes == 0 -> stringResource(R.string.requests_pick_season)
        else -> stringResource(R.string.requests_ask_for, chooseSummary(state))
    }

@Composable
private fun ShowChoices(
    state: AskSheetState,
    viewModel: AskViewModel,
) {
    Text(stringResource(R.string.requests_ask_what), style = MaterialTheme.typography.titleSmall)
    RadioLine(
        stringResource(R.string.requests_mode_all),
        state.mode == ShowMode.ALL,
        hint = stringResource(R.string.requests_mode_all_hint),
    ) {
        viewModel.setMode(ShowMode.ALL)
    }
    RadioLine(
        stringResource(R.string.requests_mode_choose),
        state.mode == ShowMode.CHOOSE,
        enabled = state.chooseAllowed,
        hint = stringResource(R.string.requests_mode_choose_hint),
    ) { viewModel.setMode(ShowMode.CHOOSE) }
    RadioLine(
        stringResource(R.string.requests_mode_future),
        state.mode == ShowMode.FUTURE,
        hint = stringResource(R.string.requests_mode_future_hint),
    ) {
        viewModel.setMode(ShowMode.FUTURE)
    }
    if (state.mode == ShowMode.CHOOSE) {
        state.seasons.forEach { row -> AskSeasonView(row, viewModel) }
    }
}

@Composable
private fun AskSeasonView(
    row: AskSeasonRow,
    viewModel: AskViewModel,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = row.ticked || !row.available,
                onCheckedChange = { viewModel.toggleSeason(row.number) },
                enabled = row.available,
            )
            Column(Modifier.weight(1f)) {
                Text(row.name.ifBlank { seasonName(row.number) }, style = MaterialTheme.typography.bodyLarge)
                val note =
                    when {
                        row.kept -> stringResource(R.string.requests_season_kept)
                        row.complete -> stringResource(R.string.requests_season_in_library)
                        row.picked > 0 -> stringResource(R.string.requests_season_picked, row.picked)
                        else -> null
                    }
                if (note != null) Hint(note)
            }
            IconButton(onClick = { viewModel.expandSeason(row.number) }, enabled = row.available && !row.ticked) {
                Icon(
                    if (row.expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(R.string.requests_episodes),
                )
            }
        }
        if (row.expanded && row.available && !row.ticked) {
            when (row.load) {
                EpisodesLoad.LOADING -> Hint(stringResource(R.string.requests_episodes_reading), Modifier.padding(start = Spacing.XLarge))
                EpisodesLoad.FAILED -> FailText(stringResource(R.string.requests_episodes_failed), Modifier.padding(start = Spacing.XLarge))
                EpisodesLoad.READY, EpisodesLoad.NONE ->
                    if (row.episodes.isEmpty()) {
                        Hint(stringResource(R.string.requests_episodes_none), Modifier.padding(start = Spacing.XLarge))
                    } else {
                        row.episodes.forEach { ep ->
                            Row(
                                Modifier.padding(start = Spacing.XLarge).clickable { viewModel.toggleEpisode(row.number, ep.number) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = ep.ticked, onCheckedChange = { viewModel.toggleEpisode(row.number, ep.number) })
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
}

@Composable
fun AskMusicSheet(
    artist: String,
    name: String,
    album: String,
    edition: String,
    onClose: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "ask-music-$artist-$album") { AskMusicViewModel(sdk, artist, name, album, edition) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    RouteSheet(onClose) {
        val outcome = state.outcome
        when {
            state.status.phase == Phase.LOADING -> LoadingView(Modifier.padding(Spacing.XLarge))
            state.status.phase == Phase.FAILED -> state.status.error?.let { ErrorView(it, onRetry = viewModel::retry) }
            outcome != null -> DoneView(outcomeTitle(outcome, state.name, false), outcomeMessage(outcome), onClose)
            else -> {
                Text(stringResource(R.string.requests_ask_title, state.name), style = MaterialTheme.typography.titleLarge)
                val brief = state.brief
                if (brief != null) {
                    val what =
                        if (brief.albums.isEmpty()) {
                            stringResource(
                                R.string.requests_albums_every,
                            )
                        } else {
                            namesText(brief.albums.map { it.name })
                        }
                    Hint(stringResource(R.string.requests_ask_already_music, what, brief.stateLabel))
                } else {
                    Hint(stringResource(R.string.requests_ask_hear))
                }
                Text(stringResource(R.string.requests_ask_what), style = MaterialTheme.typography.titleSmall)
                RadioLine(
                    stringResource(R.string.requests_mode_every_album),
                    state.mode == MusicMode.EVERY,
                    hint = stringResource(R.string.requests_mode_every_album_hint),
                ) {
                    viewModel.setMode(MusicMode.EVERY)
                }
                RadioLine(
                    stringResource(R.string.requests_mode_some_albums),
                    state.mode == MusicMode.SOME,
                    enabled = state.someAllowed,
                    hint = stringResource(R.string.requests_mode_some_albums_hint),
                ) { viewModel.setMode(MusicMode.SOME) }
                if (state.mode == MusicMode.SOME) {
                    state.albums.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = row.available) { viewModel.toggle(row.mbid) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = row.ticked || row.kept,
                                onCheckedChange = { viewModel.toggle(row.mbid) },
                                enabled = row.available,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(row.name, style = MaterialTheme.typography.bodyLarge)
                                val facts =
                                    listOfNotNull(
                                        row.year.takeIf { it > 0 }?.toString(),
                                        albumKindWord(row.kind),
                                        if (row.kept) stringResource(R.string.requests_album_in_library) else null,
                                        if (row.leftOut) stringResource(R.string.requests_album_left_out) else null,
                                    )
                                Hint(facts.joinToString(" · "))
                            }
                        }
                    }
                }
                FollowRow(state.follow, viewModel::setFollow)
                AskError(state.error)
                SheetFooter(
                    primary =
                        when {
                            state.mode == MusicMode.EVERY -> stringResource(R.string.requests_ask_every_album)
                            state.picked.isEmpty() -> stringResource(R.string.requests_pick_album)
                            else -> stringResource(R.string.requests_ask_for, namesText(state.picked.map { it.name }))
                        },
                    enabled = state.canSubmit,
                    busy = state.busy,
                    onCancel = onClose,
                    onPrimary = viewModel::submit,
                )
            }
        }
    }
}
