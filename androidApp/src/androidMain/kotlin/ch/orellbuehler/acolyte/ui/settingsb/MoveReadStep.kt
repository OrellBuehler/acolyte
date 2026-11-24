package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.move.MoveReadForm
import ch.orellbuehler.acolyte.feature.move.MoveReadRow
import ch.orellbuehler.acolyte.feature.move.MoveVocabulary
import ch.orellbuehler.acolyte.feature.move.SettingsMoveState
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

private const val EARLIER_SHOWN = 5

@Composable
fun MoveReadStep(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    onAdd: () -> Unit,
    onEdit: (MoveReadRow) -> Unit,
    onForget: (MoveReadRow) -> Unit,
    onOpenDownloaders: () -> Unit,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        item(key = "head") {
            SettingsCard(
                title = stringResource(R.string.acq_mv_step1_title),
                subtitle = stringResource(R.string.acq_mv_subtitle),
            ) {
                if (state.reads.isEmpty() && state.readingKind.isEmpty()) {
                    Text(stringResource(R.string.acq_mv_empty_title), style = MaterialTheme.typography.titleMedium)
                    Hint(stringResource(R.string.acq_mv_empty_text))
                }
                state.reads.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider()
                    MoveReadRowView(row, viewModel, onEdit, onForget)
                }
                if (state.readingKind.isNotEmpty()) {
                    if (state.reads.isNotEmpty()) HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            Text(MoveVocabulary.appName(state.readingKind), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            StatusChip(stringResource(R.string.acq_mv_reading_chip), Tone.INFO)
                        }
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Hint(stringResource(R.string.acq_mv_reading_now))
                    }
                }
                OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(stringResource(R.string.acq_mv_add_app), Modifier.padding(start = Spacing.Small))
                }
            }
        }
        if (state.brought.isNotEmpty()) {
            item(key = "brought") {
                SettingsCard(
                    title = stringResource(R.string.acq_mv_brought_title),
                    subtitle = stringResource(R.string.acq_mv_brought_text),
                ) {
                    state.brought.forEach { b ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            Column(Modifier.weight(1f)) {
                                Text(b.name, style = MaterialTheme.typography.bodyLarge)
                                Hint("${b.app} · ${b.kind}")
                            }
                            when (b.needs) {
                                "key" -> StatusChip(stringResource(R.string.acq_mv_needs_key), Tone.WAIT)
                                "password" -> StatusChip(stringResource(R.string.acq_mv_needs_password), Tone.WAIT)
                                "token" -> StatusChip(stringResource(R.string.acq_mv_needs_token), Tone.WAIT)
                                else -> StatusChip(stringResource(R.string.acq_mv_brought_ready), Tone.OK)
                            }
                        }
                    }
                    if (state.brought.any { it.needs.isNotEmpty() }) {
                        OutlinedButton(onClick = onOpenDownloaders) { Text(stringResource(R.string.acq_mv_open_downloaders)) }
                    }
                }
            }
        }
        if (state.moves.isNotEmpty() || state.movesProblem != null) {
            item(key = "earlier") {
                SettingsCard(title = stringResource(R.string.acq_mv_earlier)) {
                    if (state.movesProblem != null) FailText(stringResource(R.string.acq_mv_earlier_failed))
                    val rows = if (showAll) state.moves else state.moves.take(EARLIER_SHOWN)
                    rows.forEachIndexed { index, m ->
                        if (index > 0) HorizontalDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                            ) {
                                Text(
                                    stringResource(R.string.acq_mv_earlier_row, m.id, moveShortDate(m.createdAt)),
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                StatusChip(moveStateText(m.word), m.tone)
                            }
                            Hint(if (m.counts.isEmpty()) stringResource(R.string.acq_mv_nothing_counted) else moveSummaryText(m.counts))
                            Row {
                                TextButton(onClick = { viewModel.openMove(m.id) }) { Text(stringResource(R.string.acq_mv_open)) }
                                if (m.canRemove) {
                                    TextButton(onClick = { viewModel.removeMove(m.id) }) { Text(stringResource(R.string.acq_mv_remove)) }
                                }
                            }
                        }
                    }
                    if (!showAll && state.moves.size > EARLIER_SHOWN) {
                        TextButton(onClick = { showAll = true }) { Text(stringResource(R.string.acq_mv_show_all, state.moves.size)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveReadRowView(
    row: MoveReadRow,
    viewModel: MoveViewModel,
    onEdit: (MoveReadRow) -> Unit,
    onForget: (MoveReadRow) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Switch(checked = row.picked, onCheckedChange = { viewModel.togglePick(row.id, it) })
            Column(Modifier.weight(1f)) {
                Text(
                    if (row.showHost) stringResource(R.string.acq_mv_app_at, row.name, row.host) else row.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                val used = if (row.usedBy > 0) stringResource(R.string.acq_mv_used_in, row.usedBy) else ""
                val stats = if (row.stats.isEmpty()) stringResource(R.string.acq_mv_nothing_found) else moveCountsText(row.stats)
                Hint(listOf(used, stats).filter { it.isNotEmpty() }.joinToString(" · "))
            }
            if (row.version.isNotEmpty()) {
                StatusChip(stringResource(R.string.acq_mv_version_tag, row.version), Tone.OK)
            } else {
                StatusChip(stringResource(R.string.acq_mv_read_tag), Tone.IDLE)
            }
        }
        Hint(stringResource(R.string.acq_mv_read_on, moveShortDate(row.createdAt)))
        Row {
            TextButton(onClick = { onEdit(row) }) { Text(stringResource(R.string.acq_mv_edit)) }
            TextButton(
                onClick = { onForget(row) },
            ) { Text(stringResource(R.string.acq_mv_remove), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun MoveForgetDialog(
    row: MoveReadRow,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.acq_mv_forget_title, row.name)) },
        text = { Text(stringResource(R.string.acq_mv_forget_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.acq_mv_remove), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.acq_mv_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveReadSheet(
    request: MoveReadRequest,
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    serverHost: String,
    onClose: () -> Unit,
) {
    val editing = request.editing
    var kind by rememberSaveable { mutableStateOf(request.kind) }
    var address by rememberSaveable { mutableStateOf(request.address.removePrefix("http://")) }
    var key by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var useSaved by rememberSaveable { mutableStateOf(true) }
    var missing by remember { mutableStateOf(false) }
    var showOthers by remember { mutableStateOf(false) }
    val saved = state.savedKey(kind, if (editing) request.address else "", editing)
    val withSaved = saved != null && useSaved
    val sheet = state.sheet
    val app = MoveVocabulary.apps.firstOrNull { it.kind == kind }
    val form =
        MoveReadForm(
            kind = kind,
            address = if (withSaved) saved?.address.orEmpty() else address.trim(),
            key = if (withSaved) "" else key.trim(),
            username = if (withSaved) "" else username,
            password = if (withSaved) "" else password,
            useSavedKey = withSaved,
        )

    fun changed() {
        missing = false
        viewModel.clearProbe()
    }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(
                if (editing) {
                    stringResource(
                        R.string.acq_mv_sheet_edit,
                        MoveVocabulary.appName(kind),
                    )
                } else {
                    stringResource(R.string.acq_mv_sheet_add)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            PickerField(
                label = stringResource(R.string.acq_mv_app),
                options = MoveVocabulary.apps.map { it.kind to it.name },
                selected = kind,
                onSelect = {
                    kind = it
                    useSaved = true
                    viewModel.clearFound()
                    changed()
                },
                enabled = !editing && !sheet.reading,
            )
            if (saved != null) {
                SwitchRow(
                    title = if (editing) stringResource(R.string.acq_mv_saved_key) else stringResource(R.string.acq_mv_use_saved),
                    checked = useSaved,
                    onChange = {
                        useSaved = it
                        changed()
                    },
                    hint = stringResource(R.string.acq_mv_sent_only_to, saved.address),
                )
            }
            if (!withSaved) {
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        if (editing) useSaved = false
                        changed()
                    },
                    label = { Text(stringResource(R.string.acq_mv_address)) },
                    placeholder = { Text(MoveVocabulary.placeholder(kind).removePrefix("http://")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = missing,
                    supportingText = if (missing) ({ Text(stringResource(R.string.acq_mv_address_missing)) }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.owner && !editing) {
                    FindOnNetwork(state, viewModel, form, serverHost, kind, showOthers, { showOthers = it }) { found ->
                        kind = found.kind
                        address = found.address
                        useSaved = false
                        viewModel.clearFound()
                        changed()
                    }
                }
                if (kind == "qbittorrent") {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            changed()
                        },
                        label = { Text(stringResource(R.string.acq_mv_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            changed()
                        },
                        label = { Text(stringResource(R.string.acq_mv_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it
                        changed()
                    },
                    label = { Text(stringResource(R.string.acq_mv_api_key)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Hint(
                    if (kind == "qbittorrent") {
                        stringResource(R.string.acq_mv_api_key_qb)
                    } else {
                        stringResource(R.string.acq_mv_api_key_where, app?.name ?: kind)
                    },
                )
            }
            val probe = sheet.probe
            if (probe != null) {
                if (probe.ok) {
                    StatusChip("${probe.app} ${probe.version}".trim(), Tone.OK)
                } else {
                    probe.problem?.let { FailText(moveProblemText(it)) }
                }
            }
            if (sheet.linked.isNotEmpty()) {
                MoveLinkedPanel(state, viewModel, form) { row ->
                    kind = row.kind
                    address = row.address
                    useSaved = false
                    changed()
                }
            }
            sheet.readProblem?.let { FailText(moveProblemText(it)) }
            if (sheet.stopped && !sheet.reading) Hint(stringResource(R.string.acq_mv_stopped))
            if (sheet.reading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Hint(
                    "${stringResource(
                        R.string.acq_mv_adding,
                        MoveVocabulary.appName(kind),
                    )} · ${stringResource(R.string.acq_mv_takes_a_while)}",
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End)) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.acq_mv_cancel)) }
                OutlinedButton(
                    onClick = {
                        if (!withSaved && address.isBlank()) missing = true else viewModel.probe(form)
                    },
                    enabled = !sheet.reading && !sheet.probing,
                ) { Text(if (sheet.probing) stringResource(R.string.acq_mv_testing) else stringResource(R.string.acq_mv_test)) }
                if (sheet.reading) {
                    Button(onClick = viewModel::stopRead) { Text(stringResource(R.string.acq_mv_stop)) }
                } else {
                    Button(onClick = {
                        if (!withSaved && address.isBlank()) {
                            missing = true
                        } else {
                            viewModel.read(form) { ok -> if (ok) onClose() }
                        }
                    }) {
                        Text(
                            if (editing) {
                                stringResource(
                                    R.string.acq_mv_save,
                                )
                            } else {
                                stringResource(R.string.acq_mv_add_button, MoveVocabulary.appName(kind))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FindOnNetwork(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    form: MoveReadForm,
    serverHost: String,
    kind: String,
    showOthers: Boolean,
    setShowOthers: (Boolean) -> Unit,
    onUse: (ch.orellbuehler.acolyte.feature.move.MoveFoundRow) -> Unit,
) {
    val sheet = state.sheet
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.acq_mv_find), style = MaterialTheme.typography.bodyLarge)
                Hint(stringResource(R.string.acq_mv_find_hint))
            }
            OutlinedButton(onClick = { viewModel.find(serverHost, form) }, enabled = !sheet.finding && !sheet.reading) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Text(stringResource(R.string.acq_mv_find_search), Modifier.padding(start = Spacing.Small))
            }
        }
        if (sheet.finding) LinearProgressIndicator(Modifier.fillMaxWidth())
        sheet.findProblem?.let { FailText(moveProblemText(it)) }
        if (sheet.findRan) {
            val mine = sheet.found.filter { it.kind == kind }
            val others = sheet.found.filter { it.kind != kind }
            mine.forEach { FoundRow(it, onUse) }
            if (mine.isEmpty()) {
                Hint(
                    if (sheet.found.isEmpty()) {
                        stringResource(R.string.acq_mv_find_none)
                    } else {
                        stringResource(R.string.acq_mv_find_none_kind, MoveVocabulary.appName(kind))
                    },
                )
            }
            if (others.isNotEmpty()) {
                TextButton(onClick = { setShowOthers(!showOthers) }) { Text(stringResource(R.string.acq_mv_find_others, others.size)) }
                if (showOthers) others.forEach { FoundRow(it, onUse) }
            }
            if (sheet.found.isEmpty() && sheet.searched.isNotEmpty()) {
                Hint(stringResource(R.string.acq_mv_find_searched, sheet.searched.joinToString(", ")))
            }
        }
    }
}

@Composable
private fun FoundRow(
    row: ch.orellbuehler.acolyte.feature.move.MoveFoundRow,
    onUse: (ch.orellbuehler.acolyte.feature.move.MoveFoundRow) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Column(Modifier.weight(1f)) {
            Text("${row.name} ${row.version}".trim(), style = MaterialTheme.typography.bodyLarge)
            Hint(row.address)
        }
        TextButton(onClick = { onUse(row) }) { Text(stringResource(R.string.acq_mv_find_use)) }
    }
}

@Composable
private fun MoveLinkedPanel(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    form: MoveReadForm,
    onUse: (ch.orellbuehler.acolyte.feature.move.MoveLinkedRow) -> Unit,
) {
    val sheet = state.sheet
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.acq_mv_linked_title), style = MaterialTheme.typography.titleMedium)
        sheet.linked.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        listOf(
                            row.name.ifEmpty { MoveVocabulary.appName(row.kind) },
                            row.version,
                        ).filter { it.isNotEmpty() }.joinToString(" "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Hint(row.address)
                    if (row.hasKey && !row.reachable) FailText(stringResource(R.string.acq_mv_not_reached))
                }
                if (!row.hasKey && row.offered) {
                    TextButton(onClick = { onUse(row) }) { Text(stringResource(R.string.acq_mv_find_use)) }
                }
            }
        }
        val addable = sheet.addable
        if (addable.isNotEmpty()) {
            val names = moveAndList(addable.map { it.name.ifEmpty { MoveVocabulary.appName(it.kind) } }.distinct())
            Button(onClick = { viewModel.addLinked(form) }, enabled = !sheet.addingLinked && !sheet.reading) {
                Text(stringResource(R.string.acq_mv_also_add, names, MoveVocabulary.appName(sheet.linkedFrom)))
            }
            Hint(stringResource(R.string.acq_mv_linked_keys, MoveVocabulary.appName(sheet.linkedFrom)))
        }
    }
}
