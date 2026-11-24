package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.move.MoveBadge
import ch.orellbuehler.acolyte.feature.move.MoveChooseWhy
import ch.orellbuehler.acolyte.feature.move.MoveItemRow
import ch.orellbuehler.acolyte.feature.move.MoveListKind
import ch.orellbuehler.acolyte.feature.move.MovePersonChoice
import ch.orellbuehler.acolyte.feature.move.MoveVocabulary
import ch.orellbuehler.acolyte.feature.move.SettingsMoveState
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing

private const val PAGE = 100

@Composable
private fun ItemRow(row: MoveItemRow) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(row.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (row.n > 1) Text("×${row.n}", style = MaterialTheme.typography.bodyMedium)
            val word = row.word
            if (word != null) {
                StatusChip(moveWordText(word, row.raw), moveTone(row.tone))
            } else if (row.look) {
                StatusChip(stringResource(R.string.acq_mv_tag_looks), Tone.WAIT)
            }
            when (row.badge) {
                MoveBadge.NONE -> Unit
                MoveBadge.YOU -> StatusChip(stringResource(R.string.acq_mv_badge_you), Tone.OK)
                MoveBadge.PLEX -> StatusChip(stringResource(R.string.acq_mv_badge_plex), Tone.OK)
                MoveBadge.JELLYFIN -> StatusChip(stringResource(R.string.acq_mv_badge_jellyfin), Tone.OK)
                MoveBadge.EMBY -> StatusChip(stringResource(R.string.acq_mv_badge_emby), Tone.OK)
                MoveBadge.INVITE -> StatusChip(stringResource(R.string.acq_mv_badge_invite), Tone.IDLE)
            }
        }
        if (row.hint.isNotEmpty()) Hint(row.hint)
        if (row.group != ch.orellbuehler.acolyte.feature.move.MoveGroup.UNKNOWN) Hint(moveGroupName(row.group, ""))
        if (row.traits.isNotEmpty()) Hint(stringResource(R.string.acq_mv_traits, row.traits))
        if (row.skipped.isNotEmpty()) Hint(stringResource(R.string.acq_mv_left_out_list, row.skipped))
        if (row.needs.isNotEmpty()) Hint(stringResource(R.string.acq_mv_needs_what, row.needs))
        if (row.section.isNotEmpty()) Hint(row.section)
    }
}

@Composable
fun MoveItemsDialog(
    request: MoveListRequest,
    viewModel: MoveViewModel,
    onClose: () -> Unit,
) {
    val list = remember(request) { viewModel.items(request.kind, request.id) }
    var query by remember(request) { mutableStateOf("") }
    var onlyLook by remember(request) { mutableStateOf(list.showFilter) }
    var limit by remember(request) { mutableIntStateOf(PAGE) }
    val page = remember(list, query, onlyLook, limit) { list.page(query, onlyLook, limit) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.acq_mv_close))
                    }
                    Text(request.title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        limit = PAGE
                    },
                    placeholder = { Text(stringResource(R.string.acq_mv_list_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
                )
                if (list.showFilter) {
                    Row(
                        Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.acq_mv_list_which), style = MaterialTheme.typography.labelMedium)
                        FilterChip(
                            selected = onlyLook,
                            onClick = {
                                onlyLook = true
                                limit = PAGE
                            },
                            label = { Text(stringResource(R.string.acq_mv_tag_looks)) },
                        )
                        FilterChip(
                            selected = !onlyLook,
                            onClick = {
                                onlyLook = false
                                limit = PAGE
                            },
                            label = { Text(stringResource(R.string.acq_mv_list_all)) },
                        )
                    }
                }
                if (page.total > 0) {
                    Hint(
                        stringResource(R.string.acq_mv_list_showing, page.rows.size, page.total),
                        Modifier.padding(horizontal = Spacing.Screen),
                    )
                }
                if (page.rows.isEmpty()) {
                    Hint(
                        if (query.isNotBlank()) {
                            stringResource(
                                R.string.acq_mv_list_no_match,
                            )
                        } else {
                            stringResource(R.string.acq_mv_list_empty)
                        },
                        Modifier.padding(Spacing.Screen),
                    )
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(page.rows, key = { it.key }) { row ->
                        ItemRow(row)
                        HorizontalDivider()
                    }
                    if (page.rows.size < page.total) {
                        item(key = "more") {
                            TextButton(onClick = { limit += PAGE }, modifier = Modifier.padding(horizontal = Spacing.Small)) {
                                Text(stringResource(R.string.acq_mv_list_more))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveKeysSheet(
    kind: MoveListKind,
    viewModel: MoveViewModel,
    onClose: () -> Unit,
) {
    val rows = remember(kind) { viewModel.items(kind, "").rows }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(
                if (kind ==
                    MoveListKind.DOWNLOADER_KEYS
                ) {
                    stringResource(R.string.acq_mv_keys_downloaders)
                } else {
                    stringResource(R.string.acq_mv_keys_sources)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            listOf("usenet", "torrent", "downloaders").forEach { section ->
                val inSection = rows.filter { it.section == section }
                if (inSection.isNotEmpty()) {
                    val title =
                        when (section) {
                            "usenet" -> stringResource(R.string.acq_mv_sec_usenet)
                            "torrent" -> stringResource(R.string.acq_mv_sec_torrent)
                            else -> stringResource(R.string.acq_mv_sec_downloaders)
                        }
                    Text("$title · ${inSection.size}", style = MaterialTheme.typography.titleSmall)
                    inSection.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            Text(row.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            when (row.needs) {
                                "key" -> StatusChip(stringResource(R.string.acq_mv_need_key), Tone.IDLE)
                                "password" -> StatusChip(stringResource(R.string.acq_mv_need_password), Tone.IDLE)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveNotMovedSheet(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    onClose: () -> Unit,
) {
    var app by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            val selected = app
            if (selected == null) {
                Text(stringResource(R.string.acq_mv_not_imported), style = MaterialTheme.typography.titleLarge)
                Hint(stringResource(R.string.acq_mv_not_imported_lead))
                state.choose.notMovedApps.forEach { entry ->
                    TextButton(onClick = { app = entry.app }, modifier = Modifier.fillMaxWidth()) {
                        Text(entry.name.ifEmpty { entry.app }, Modifier.weight(1f))
                        Text("${entry.n}")
                    }
                }
            } else {
                Text(
                    stringResource(R.string.acq_mv_not_imported_app, MoveVocabulary.appName(selected)),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = { app = null }) { Text(stringResource(R.string.acq_mv_all_apps)) }
                val rows = remember(selected) { viewModel.items(MoveListKind.NOT_MOVED_APP, selected).rows }
                rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider()
                    ItemRow(row)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveBeforeSheet(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    onShow: (MoveListRequest) -> Unit,
) {
    val choose = state.choose
    ModalBottomSheet(onDismissRequest = viewModel::closeDialog, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.acq_mv_before_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.acq_mv_before_text, choose.total), style = MaterialTheme.typography.bodyMedium)
            if (choose.tryNotice) Hint(stringResource(R.string.acq_mv_checked_again))
            if (choose.stillChecking > 0) Hint(stringResource(R.string.acq_mv_still_checking, choose.stillChecking))
            choose.outcome?.let { MoveOutcomeBlock(it, after = false) }
            choose.before.forEach { row ->
                val label = moveGroupName(row.group, row.id.substringBefore(':'))
                val word =
                    ch.orellbuehler.acolyte.feature.move
                        .MoveSummaryPart(row.n, row.word, "")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.acq_mv_before_row, row.n, label, moveSummaryWord(row.word, "")),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = { onShow(MoveListRequest(MoveListKind.BEFORE_GROUP, row.id, label)) }) {
                        Text(stringResource(R.string.acq_mv_show))
                    }
                }
            }
            if (choose.choices.isNotEmpty()) {
                Text(stringResource(R.string.acq_mv_needs_choice), style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(R.string.acq_mv_until_choose))
                choose.choices.forEach { row ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Text(row.name, style = MaterialTheme.typography.bodyLarge)
                        Hint(
                            when (row.why) {
                                MoveChooseWhy.SAME_NAME -> stringResource(R.string.acq_mv_why_same_name)
                                MoveChooseWhy.NUMERIC_ID -> stringResource(R.string.acq_mv_why_numeric)
                                MoveChooseWhy.MERGE_GONE -> stringResource(R.string.acq_mv_why_gone)
                            },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            FilterChip(
                                selected = row.choice == MovePersonChoice.MERGE,
                                onClick = { viewModel.chooseOnce(row.key, MovePersonChoice.MERGE) },
                                label = { Text(stringResource(R.string.acq_mv_same_person)) },
                            )
                            FilterChip(
                                selected = row.choice == MovePersonChoice.SKIP,
                                onClick = { viewModel.chooseOnce(row.key, MovePersonChoice.SKIP) },
                                label = { Text(stringResource(R.string.acq_mv_leave_out)) },
                            )
                        }
                    }
                }
            }
            choose.tryProblem?.let { FailText(moveProblemText(it)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End)) {
                OutlinedButton(onClick = viewModel::closeDialog) { Text(stringResource(R.string.acq_mv_go_back)) }
                Button(onClick = viewModel::start, enabled = !choose.trying && !choose.stale && !state.starting) {
                    Text(stringResource(R.string.acq_mv_import_anyway))
                }
            }
        }
    }
}

@Composable
fun MoveConfirmDialog(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
) {
    val choose = state.choose
    AlertDialog(
        onDismissRequest = viewModel::closeDialog,
        title = { Text(pluralStringResource(R.plurals.acq_mv_confirm_title, choose.total, choose.total)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (choose.stillChecking > 0) Text(stringResource(R.string.acq_mv_still_checking_import, choose.stillChecking))
                if (choose.toCheck > 0) Text(pluralStringResource(R.plurals.acq_mv_note_check, choose.toCheck, choose.toCheck))
                if (choose.apps.isNotEmpty()) Text(stringResource(R.string.acq_mv_confirm_from, moveAndList(choose.apps)))
                Text(stringResource(R.string.acq_mv_confirm_undo_later))
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::start,
                enabled = !state.starting,
            ) { Text(stringResource(R.string.acq_mv_import)) }
        },
        dismissButton = { TextButton(onClick = viewModel::closeDialog) { Text(stringResource(R.string.acq_mv_cancel)) } },
    )
}

@Composable
fun MoveUndoDialog(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
) {
    AlertDialog(
        onDismissRequest = viewModel::closeDialog,
        title = { Text(stringResource(R.string.acq_mv_undo_title)) },
        text = { Text(stringResource(R.string.acq_mv_undo_text)) },
        confirmButton = {
            TextButton(
                onClick = viewModel::undo,
            ) { Text(stringResource(R.string.acq_mv_undo_confirm), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = viewModel::closeDialog) { Text(stringResource(R.string.acq_mv_cancel)) } },
    )
}
