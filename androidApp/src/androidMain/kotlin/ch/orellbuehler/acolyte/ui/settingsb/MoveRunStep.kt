package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.move.MoveDoneLook
import ch.orellbuehler.acolyte.feature.move.MoveFolderOutcome
import ch.orellbuehler.acolyte.feature.move.MoveListKind
import ch.orellbuehler.acolyte.feature.move.MoveRunState
import ch.orellbuehler.acolyte.feature.move.MoveStateWord
import ch.orellbuehler.acolyte.feature.move.SettingsMoveState
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.nav.LibraryImportRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryRenameRoute
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
private fun RunCapsule(run: MoveRunState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        if (run.running && run.lost) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                StatusChip(stringResource(R.string.acq_mv_cant_reach_word), Tone.WAIT)
                Text(stringResource(R.string.acq_mv_trying_again), style = MaterialTheme.typography.bodyMedium)
            }
            Hint(stringResource(R.string.acq_mv_cant_reach))
        } else if (run.running) {
            StatusChip(moveStateText(if (run.undoing) MoveStateWord.UNDOING else MoveStateWord.IMPORTING), Tone.INFO)
            LinearProgressIndicator(progress = { run.progress }, modifier = Modifier.fillMaxWidth())
            val note = run.note
            if (note.isCount) {
                Text(stringResource(R.string.acq_mv_progress_of, note.done, note.total), style = MaterialTheme.typography.bodyMedium)
                if (note.waitLeft > 0) Hint(stringResource(R.string.acq_mv_busy_wait, note.waitLeft, note.waitMinutes))
            } else if (note.raw.isNotEmpty()) {
                Hint(note.raw)
            }
        } else {
            val tone =
                when {
                    run.undoFailed || run.failed -> Tone.FAIL
                    run.undone -> Tone.IDLE
                    run.look == MoveDoneLook.FAILED -> Tone.FAIL
                    run.look == MoveDoneLook.PENDING -> Tone.WAIT
                    else -> Tone.OK
                }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                StatusChip(moveStateText(run.word), tone)
                val detail =
                    when {
                        run.undoFailed -> pluralStringResource(R.plurals.acq_mv_items, run.failedCount, run.failedCount)
                        run.needsYou && !run.undone && !run.failed && run.look != MoveDoneLook.READY ->
                            stringResource(
                                R.string.acq_mv_needs_you,
                            )
                        else -> moveSummaryText(run.counts)
                    }
                if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Hint(stringResource(R.string.acq_mv_done_when, run.id, moveDate(run.createdAt)))
    }
}

@Composable
fun MoveImportStep(state: SettingsMoveState) {
    val run = state.run
    LazyColumn(contentPadding = PaddingValues(vertical = Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        item(key = "import") {
            SettingsCard(title = stringResource(R.string.acq_mv_step3_title)) {
                if (run != null) RunCapsule(run)
                Hint(stringResource(R.string.acq_mv_leave_page))
            }
        }
    }
}

@Composable
fun MoveOutcomeBlock(
    outcome: MoveFolderOutcome,
    after: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.acq_mv_outcome_title), style = MaterialTheme.typography.titleSmall)
        Text(
            if (after) stringResource(R.string.acq_mv_came_over, outcome.came) else stringResource(R.string.acq_mv_will_come, outcome.came),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (outcome.attention > 0) {
            Text(stringResource(R.string.acq_mv_need_attention, outcome.attention), style = MaterialTheme.typography.bodyLarge)
            Hint(stringResource(R.string.acq_mv_need_attention_why, outcome.attentionFolders.joinToString(", ")))
        }
        if (outcome.unchecked > 0) {
            Text(stringResource(R.string.acq_mv_left_out_unchecked, outcome.unchecked), style = MaterialTheme.typography.bodyLarge)
            outcome.uncheckedFolders.forEach { Hint(stringResource(R.string.acq_mv_left_out_unchecked_why, it)) }
        }
    }
}

@Composable
private fun TodoCard(
    title: String,
    hint: String,
    action: String?,
    onAction: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint.isNotEmpty()) Hint(hint)
        }
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun MoveDoneStep(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    onShow: (MoveListRequest) -> Unit,
    onOpen: (Any) -> Unit,
) {
    val run = state.run ?: return
    LazyColumn(contentPadding = PaddingValues(vertical = Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        item(key = "done") {
            SettingsCard(title = stringResource(R.string.acq_mv_step4_title)) { RunCapsule(run) }
        }
        if (run.hasReport && !run.running) {
            if (run.needsYou) {
                item(key = "todo") {
                    SettingsCard(title = stringResource(R.string.acq_mv_needs_you)) {
                        if (run.plainNext) {
                            run.next.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        } else {
                            if (run.noFolder > 0) {
                                TodoCard(
                                    pluralStringResource(R.plurals.acq_mv_no_folder_n, run.noFolder, run.noFolder),
                                    stringResource(R.string.acq_mv_add_folder_then),
                                    stringResource(R.string.acq_mv_folders),
                                ) { onOpen(Routes.settingsLibraryFolders) }
                            }
                            if (run.renameNext) {
                                TodoCard(
                                    stringResource(R.string.acq_mv_rename_next),
                                    stringResource(R.string.acq_mv_rename_next_hint),
                                    stringResource(R.string.acq_mv_check_renames),
                                ) { onOpen(LibraryRenameRoute) }
                            }
                            if (run.openChoices > 0) {
                                TodoCard(
                                    pluralStringResource(R.plurals.acq_mv_choose_people, run.openChoices, run.openChoices),
                                    stringResource(R.string.acq_mv_they_stay),
                                    stringResource(R.string.acq_mv_choose),
                                    viewModel::chooseAgain,
                                )
                            }
                            if (run.seasons.isNotEmpty()) {
                                TodoCard(
                                    pluralStringResource(R.plurals.acq_mv_note_check, run.seasons.size, run.seasons.size),
                                    stringResource(R.string.acq_mv_seasons_why, run.seasons.joinToString(", ") { it.name }),
                                    null,
                                ) {}
                                run.seasons.filter { it.titleId > 0 }.forEach { s ->
                                    TodoCard(s.name, "", stringResource(R.string.acq_mv_open)) { onOpen(Routes.libraryTitle(s.titleId)) }
                                }
                            }
                            if (run.failedCount > 0) {
                                TodoCard(
                                    pluralStringResource(R.plurals.acq_mv_failed_n, run.failedCount, run.failedCount),
                                    stringResource(R.string.acq_mv_failed_hint),
                                    stringResource(R.string.acq_mv_match_by_hand),
                                ) { onOpen(LibraryImportRoute) }
                            }
                        }
                    }
                }
            }
            if (run.groups.isNotEmpty() || run.notMoved > 0 || run.keysAgain > 0 || run.notes > 0) {
                item(key = "results") {
                    SettingsCard(title = stringResource(R.string.acq_mv_results)) {
                        run.outcome?.let { MoveOutcomeBlock(it, after = true) }
                        run.groups.forEachIndexed { index, g ->
                            if (index > 0) HorizontalDivider()
                            val label = moveGroupName(g.group, g.id)
                            TodoCard(label, moveSummaryText(g.summary), stringResource(R.string.acq_mv_show)) {
                                onShow(MoveListRequest(MoveListKind.REPORT_GROUP, g.id, label))
                            }
                        }
                        if (run.notMoved > 0) {
                            HorizontalDivider()
                            val title = stringResource(R.string.acq_mv_not_imported)
                            TodoCard(
                                title,
                                pluralStringResource(R.plurals.acq_mv_not_imported_n, run.notMoved, run.notMoved),
                                stringResource(R.string.acq_mv_show),
                            ) { onShow(MoveListRequest(MoveListKind.NOT_MOVED, "", title)) }
                        }
                        if (run.keysAgain > 0) {
                            HorizontalDivider()
                            val title = stringResource(R.string.acq_mv_keys_again)
                            TodoCard(title, "${run.keysAgain}", stringResource(R.string.acq_mv_show)) {
                                onShow(MoveListRequest(MoveListKind.KEYS_AGAIN, "", title))
                            }
                        }
                        if (run.notes > 0) {
                            HorizontalDivider()
                            val title = stringResource(R.string.acq_mv_notes)
                            TodoCard(title, "${run.notes}", stringResource(R.string.acq_mv_show)) {
                                onShow(MoveListRequest(MoveListKind.NOTES, "", title))
                            }
                        }
                    }
                }
            }
        }
    }
}
