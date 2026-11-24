package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.TitleLeftover
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.title.LeftoversPresenter
import ch.orellbuehler.acolyte.feature.title.LeftoversState
import ch.orellbuehler.acolyte.feature.title.RemovePresenter
import ch.orellbuehler.acolyte.feature.title.RenamePresenter
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun LeftoversContent(
    presenter: LeftoversPresenter,
    viewModel: TitleViewModel,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    FeedbackEffect(state.feedback)
    val loc = locale()
    var leftAlone by rememberSaveable { mutableStateOf(false) }
    SheetTitle(stringResource(R.string.title_leftovers), stringResource(R.string.title_leftovers_intro))
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        state.status.error?.let { ErrorBanner(it) }
        if (!state.handsOff) {
            OutlinedButton(onClick = { viewModel.launch { presenter.rescan() } }, enabled = !state.rescanning) {
                Text(stringResource(if (state.rescanning) R.string.title_rescanning else R.string.title_rescan))
            }
        }
        if (state.renameOffered) {
            NoticeBanner(stringResource(R.string.title_leftovers_rename_note))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Button(onClick = {
                    viewModel.close()
                    viewModel.openRename()
                }) { Text(stringResource(R.string.title_rename_to_match)) }
                TextButton(onClick = { viewModel.launch { presenter.undoMatch() } }) { Text(stringResource(R.string.title_undo)) }
            }
        }
        state.shift?.let { guess ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        stringResource(R.string.title_shift_title, guess.fromSeason, guess.toSeason, guess.shift),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        pluralStringResource(R.plurals.title_shift_body, guess.moves.size, guess.moves.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    guess.moves.forEach { m ->
                        Text(
                            TitleLogic.episodeCode(m.fromSeason, m.fromEpisode) + " → " + TitleLogic.episodeCode(m.toSeason, m.toEpisode),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = { viewModel.launch { presenter.matchShift() } }, enabled = !state.busy) {
                        Text(stringResource(R.string.title_match_all, guess.moves.size))
                    }
                }
            }
        }
        if (state.status.phase == Phase.LOADING) CircularProgressIndicator()
        if (state.status.phase == Phase.READY && state.open.isEmpty()) Text(stringResource(R.string.title_leftovers_none))
        state.open.forEach { leftover -> LeftoverRow(leftover, state, presenter, viewModel, loc) }
        if (state.leftAlone.isNotEmpty()) {
            TextButton(onClick = { leftAlone = !leftAlone }) { Text(stringResource(R.string.title_left_alone, state.leftAlone.size)) }
            if (leftAlone) {
                state.leftAlone.forEach { leftover ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            leftover.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = { viewModel.launch { presenter.dismiss(leftover, false) } },
                        ) { Text(stringResource(R.string.title_list_again)) }
                    }
                }
            }
        }
        state.match?.let { MatchEditor(state, presenter, viewModel) }
    }
}

@Composable
private fun LeftoverRow(
    leftover: TitleLeftover,
    state: LeftoversState,
    presenter: LeftoversPresenter,
    viewModel: TitleViewModel,
    loc: java.util.Locale,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(leftover.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(
                    formatBytes(leftover.size, loc),
                    if (leftover.resolution >
                        0
                    ) {
                        "${leftover.resolution}p"
                    } else {
                        ""
                    },
                    reasonText(leftover),
                ).filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                TextButton(
                    onClick = { viewModel.launch { presenter.dismiss(leftover, true) } },
                ) { Text(stringResource(R.string.title_leave_alone)) }
                Button(
                    onClick = { if (state.show) presenter.openMatch(leftover) else viewModel.launch { presenter.matchMovie(leftover) } },
                    enabled = !state.busy && (!state.show || state.episodesLoaded),
                ) { Text(stringResource(R.string.title_match)) }
            }
        }
    }
}

@Composable
private fun reasonText(leftover: TitleLeftover): String =
    when (leftover.reason) {
        "no_episode_numbers" -> stringResource(R.string.title_reason_no_numbers)
        "whole_season" -> stringResource(R.string.title_reason_whole_season)
        "episode_not_listed" ->
            if (leftover.droppedSeason > 0) {
                stringResource(R.string.title_reason_season_not_listed, leftover.droppedSeason)
            } else {
                stringResource(R.string.title_reason_not_listed)
            }
        "bigger_file_same_episode" -> stringResource(R.string.title_reason_bigger)
        "episode_has_file" -> stringResource(R.string.title_reason_has_file)
        "movie_more_than_one" -> stringResource(R.string.title_reason_more_than_one)
        "movie_copy_choice" -> stringResource(R.string.title_reason_copy_choice)
        "movie_has_file" -> stringResource(R.string.title_reason_movie_has_file)
        "replaced_by_hand" -> stringResource(R.string.title_reason_replaced)
        "unmatched" -> stringResource(R.string.title_reason_unmatched)
        "seasons_differ" -> stringResource(R.string.title_reason_seasons_differ)
        else -> stringResource(R.string.title_reason_not_listed)
    }

@Composable
private fun MatchEditor(
    state: LeftoversState,
    presenter: LeftoversPresenter,
    viewModel: TitleViewModel,
) {
    val match = state.match ?: return
    HorizontalDivider()
    Text(stringResource(R.string.title_match_file), style = MaterialTheme.typography.titleMedium)
    Text(match.leftover.fileName, style = MaterialTheme.typography.bodySmall)
    ChipChoices(state.seasonNumbers.map { it to seasonName(it) }, match.season) { presenter.matchSeason(it) }
    Text(stringResource(R.string.title_match_pick), style = MaterialTheme.typography.bodySmall)
    state.episodesIn(match.season).forEach { e ->
        Row(Modifier.fillMaxWidth().clickable { presenter.togglePick(e.id) }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = e.id in match.picked, onCheckedChange = { presenter.togglePick(e.id) })
            Text(
                TitleLogic.episodeCode(e.season, e.episode) + if (e.name.isNotBlank()) " · ${e.name}" else "",
                modifier = Modifier.weight(1f),
            )
            if (e.hasFile) Text(stringResource(R.string.title_has_file), style = MaterialTheme.typography.labelSmall)
        }
    }
    if (state.pickedHasFile) {
        Text(
            stringResource(R.string.title_match_replace_warning),
            color = MaterialTheme.acolyte.waitText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        TextButton(onClick = presenter::closeMatch) { Text(stringResource(R.string.common_cancel)) }
        Button(
            onClick = { viewModel.launch { presenter.confirmMatch() } },
            enabled = match.picked.isNotEmpty() && !state.busy,
            colors =
                if (state.pickedHasFile) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.acolyte.fail,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
        ) { Text(stringResource(if (state.pickedHasFile) R.string.title_match_replace else R.string.title_match)) }
    }
}

@Composable
fun RenameContent(
    presenter: RenamePresenter,
    name: String,
    viewModel: TitleViewModel,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    FeedbackEffect(state.feedback)
    if (state.season < 0) SheetTitle(stringResource(R.string.title_rename_files), name)
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        state.status.error?.let { ErrorBanner(it) }
        if (state.status.phase == Phase.LOADING) {
            CircularProgressIndicator()
            return@Column
        }
        val changes = state.changes
        Text(
            if (changes.isEmpty()) {
                stringResource(
                    R.string.title_rename_nothing,
                )
            } else {
                pluralStringResource(R.plurals.title_rename_count, changes.size, changes.size)
            },
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(R.string.title_rename_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        changes.forEach { step ->
            Column(Modifier.padding(vertical = Spacing.XSmall)) {
                Text(
                    stringResource(R.string.title_rename_now, step.from),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.title_rename_new, step.to), style = MaterialTheme.typography.bodyMedium)
                step.subtitles.forEach { sub ->
                    Text(
                        if (sub.to.isNotBlank()) {
                            stringResource(R.string.title_rename_subtitle, sub.from, sub.to)
                        } else {
                            stringResource(R.string.title_rename_subtitle_stays, sub.from, sub.skip)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            HorizontalDivider()
        }
        if (state.skipped.isNotEmpty()) {
            Text(
                pluralStringResource(R.plurals.title_rename_skipped, state.skipped.size, state.skipped.size),
                style = MaterialTheme.typography.titleSmall,
            )
            state.skipped.forEach { Text("${it.from} · ${it.skip}", style = MaterialTheme.typography.bodySmall) }
        }
        if (state.alreadyRight >
            0
        ) {
            Text(stringResource(R.string.title_rename_right, state.alreadyRight), style = MaterialTheme.typography.bodySmall)
        }
        if (state.running) {
            LinearProgressIndicator(progress = { state.progress.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text(state.note.ifBlank { stringResource(R.string.title_renaming) }, style = MaterialTheme.typography.bodySmall)
        }
        if (changes.isNotEmpty()) {
            Button(onClick = { viewModel.launch { presenter.apply() } }, enabled = !state.running) {
                Text(pluralStringResource(R.plurals.title_rename_button, changes.size, changes.size))
            }
        }
    }
}

@Composable
fun RemoveContent(
    presenter: RemovePresenter,
    viewModel: TitleViewModel,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    FeedbackEffect(state.feedback)
    val target =
        when {
            state.episodeIds.isNotEmpty() ->
                pluralStringResource(
                    R.plurals.title_episodes_count,
                    state.episodeIds.size,
                    state.episodeIds.size,
                )
            state.season >= 0 -> seasonName(state.season)
            else -> state.name
        }
    SheetTitle(stringResource(R.string.title_delete_question, target))
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        if (state.done) {
            Text(stringResource(R.string.title_deleted_done, state.name), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (state.heldIds.isNotEmpty() && state.whole) {
                    OutlinedButton(onClick = {
                        viewModel.launch { presenter.undo() }
                        viewModel.close()
                    }) { Text(stringResource(R.string.title_undo)) }
                }
                Button(onClick = viewModel::close) { Text(stringResource(R.string.title_done)) }
            }
            return@Column
        }
        if (state.handsOff) {
            Text(stringResource(R.string.title_delete_hands_off), style = MaterialTheme.typography.bodySmall)
        } else {
            SettingSwitch(
                stringResource(if (state.whole) R.string.title_delete_files else R.string.title_delete_their_files),
                stringResource(R.string.title_delete_files_hint),
                state.deleteFiles,
            ) { viewModel.launch { presenter.setDeleteFiles(it) } }
        }
        if (state.whole) {
            SettingSwitch(stringResource(R.string.title_never_add), stringResource(R.string.title_never_add_hint), state.neverAdd) {
                viewModel.launch { presenter.setNeverAdd(it) }
            }
        }
        Text(stringResource(R.string.title_what_happens), style = MaterialTheme.typography.titleSmall)
        val plan = state.plan
        state.planError?.let { ErrorBanner(it) }
        if (plan == null && state.planError == null) CircularProgressIndicator()
        if (plan != null) {
            val lines = mutableListOf<String>()
            if (state.whole) lines += stringResource(R.string.title_plan_leaves)
            if (plan.season > 0 || plan.scope == "season") lines += stringResource(R.string.title_plan_season, plan.season)
            if (plan.episodes.isNotEmpty()) lines += stringResource(R.string.title_plan_episodes, plan.episodes.joinToString(", "))
            if (plan.held.isNotEmpty()) {
                lines +=
                    if (plan.holdingDays > 0) {
                        pluralStringResource(R.plurals.title_plan_held_days, plan.held.size, plan.held.size, plan.holdingDays)
                    } else {
                        pluralStringResource(R.plurals.title_plan_held, plan.held.size, plan.held.size)
                    }
            }
            if (plan.kept.isNotEmpty()) lines += pluralStringResource(R.plurals.title_plan_kept, plan.kept.size, plan.kept.size)
            if (plan.held.isEmpty() && plan.kept.isEmpty()) lines += stringResource(R.string.title_plan_no_files)
            if (state.neverAdd && state.whole) lines += stringResource(R.string.title_plan_never_add)
            if (plan.activeDownloads >
                0
            ) {
                lines += pluralStringResource(R.plurals.title_plan_downloads, plan.activeDownloads, plan.activeDownloads)
            }
            lines.forEach { Text("· $it", style = MaterialTheme.typography.bodyMedium) }
            (plan.held + plan.kept).take(20).forEach { f ->
                Text(
                    f.path + if (f.why.isNotBlank()) " · ${f.why}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            TextButton(onClick = viewModel::close) { Text(stringResource(R.string.common_cancel)) }
            Button(
                onClick = { viewModel.launch { presenter.apply() } },
                enabled = state.ready,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.acolyte.fail),
            ) {
                Text(
                    when {
                        state.season >= 0 -> stringResource(R.string.title_delete_season, seasonName(state.season))
                        state.episodeIds.isNotEmpty() -> stringResource(R.string.title_delete_episode)
                        state.show -> stringResource(R.string.title_delete_show)
                        else -> stringResource(R.string.title_delete_movie)
                    },
                )
            }
        }
    }
}
