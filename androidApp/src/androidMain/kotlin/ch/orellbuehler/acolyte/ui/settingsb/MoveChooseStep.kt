package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.move.MoveFolderRow
import ch.orellbuehler.acolyte.feature.move.MoveGroup
import ch.orellbuehler.acolyte.feature.move.MoveGroupNote
import ch.orellbuehler.acolyte.feature.move.MoveListKind
import ch.orellbuehler.acolyte.feature.move.MoveNamesChoice
import ch.orellbuehler.acolyte.feature.move.MovePersonChoice
import ch.orellbuehler.acolyte.feature.move.MovePickKind
import ch.orellbuehler.acolyte.feature.move.MoveRuleRow
import ch.orellbuehler.acolyte.feature.move.MoveVocabulary
import ch.orellbuehler.acolyte.feature.move.SettingsMoveState
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun MoveChooseStep(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    onShow: (MoveListRequest) -> Unit,
    onKeys: (String) -> Unit,
    onNotMoved: () -> Unit,
) {
    val choose = state.choose
    var open by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        item(key = "summary") {
            SettingsCard(title = stringResource(R.string.acq_mv_step2_title)) {
                when {
                    !choose.planned -> {
                        choose.planProblem?.let { FailText(moveProblemText(it)) } ?: Hint(stringResource(R.string.acq_mv_gate_updating))
                    }
                    choose.empty -> {
                        Text(stringResource(R.string.acq_mv_choose_empty), style = MaterialTheme.typography.titleMedium)
                        Hint(stringResource(R.string.acq_mv_choose_empty_text))
                    }
                    else -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                            choose.tiles.forEach { tile ->
                                Column(Modifier.weight(1f)) {
                                    Text("${tile.n}", style = MaterialTheme.typography.headlineMedium)
                                    Text(
                                        tileWord(tile.group, tile.n, tile.label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (choose.alsoGroups.isNotEmpty()) {
                            Hint(stringResource(R.string.acq_mv_also, moveAndList(choose.alsoGroups.map { moveAlsoName(it) })))
                        }
                        Hint(stringResource(R.string.acq_mv_files_stay))
                    }
                }
                if (choose.stillChecking > 0) Hint(stringResource(R.string.acq_mv_still_checking, choose.stillChecking))
                if (choose.tryNotice) Hint(stringResource(R.string.acq_mv_checked_again))
                choose.tryProblem?.let { FailText(moveProblemText(it)) }
            }
        }
        if (choose.needFolders.isNotEmpty()) {
            item(key = "need") {
                SettingsCard(title = stringResource(R.string.acq_mv_folders), subtitle = stringResource(R.string.acq_mv_pick_where)) {
                    choose.needFolders.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider()
                        MoveFolderEditor(row, viewModel)
                    }
                }
            }
        }
        if (choose.people.isNotEmpty()) {
            item(key = "people") {
                SettingsCard(title = stringResource(R.string.acq_mv_account_title)) {
                    choose.people.forEach { person ->
                        val app = MoveVocabulary.appName(person.app)
                        SwitchRow(
                            title =
                                person.name + " · " +
                                    if (person.owner) {
                                        stringResource(
                                            R.string.acq_mv_this_is_me,
                                        )
                                    } else {
                                        stringResource(R.string.acq_mv_keep_admin)
                                    },
                            checked = person.on,
                            onChange = { on ->
                                viewModel.setPerson(
                                    person.key,
                                    if (!on) {
                                        null
                                    } else if (person.owner) {
                                        MovePersonChoice.OWNER
                                    } else {
                                        MovePersonChoice.ADMIN
                                    },
                                )
                            },
                            hint =
                                if (person.owner) {
                                    stringResource(
                                        R.string.acq_mv_owner_in,
                                        app,
                                    )
                                } else {
                                    stringResource(R.string.acq_mv_admin_in, app)
                                },
                        )
                    }
                }
            }
        }
        if (choose.namesAsk) {
            item(key = "names") {
                SettingsCard(title = stringResource(R.string.acq_mv_names_title)) {
                    NamesOption(
                        MoveNamesChoice.KEEP,
                        choose.names,
                        R.string.acq_mv_names_keep,
                        R.string.acq_mv_names_keep_hint,
                        viewModel,
                    )
                    NamesOption(
                        MoveNamesChoice.RENAME,
                        choose.names,
                        R.string.acq_mv_names_rename,
                        R.string.acq_mv_names_rename_hint,
                        viewModel,
                    )
                }
            }
        }
        if (choose.sourceKeys > 0 || choose.downloaderKeys > 0 || choose.invitees > 0) {
            item(key = "after") {
                SettingsCard(title = stringResource(R.string.acq_mv_after_title)) {
                    if (choose.sourceKeys > 0) {
                        AfterRow(
                            pluralStringResource(R.plurals.acq_mv_reconnect_sources, choose.sourceKeys, choose.sourceKeys),
                            stringResource(R.string.acq_mv_paused_keys),
                        ) { onKeys("sources") }
                    }
                    if (choose.downloaderKeys > 0) {
                        AfterRow(
                            pluralStringResource(R.plurals.acq_mv_reconnect_downloaders, choose.downloaderKeys, choose.downloaderKeys),
                            stringResource(R.string.acq_mv_paused_passwords),
                        ) { onKeys("downloaders") }
                    }
                    if (choose.invitees > 0) {
                        val title = stringResource(R.string.acq_mv_list_people)
                        AfterRow(
                            pluralStringResource(R.plurals.acq_mv_invite_users, choose.invitees, choose.invitees),
                            stringResource(R.string.acq_mv_invite_hint),
                        ) { onShow(MoveListRequest(MoveListKind.INVITEES, "people", title)) }
                    }
                }
            }
        }
        if (choose.planned) {
            item(key = "change") {
                SettingsCard(title = stringResource(R.string.acq_mv_change_title)) {
                    TextButton(onClick = { open = !open }) {
                        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
                        Text(
                            if (open) stringResource(R.string.acq_mv_close) else stringResource(R.string.acq_mv_show),
                            Modifier.padding(start = Spacing.Small),
                        )
                    }
                    if (open) MoveChangeBody(state, viewModel, onShow, onNotMoved)
                }
            }
        }
    }
}

@Composable
private fun tileWord(
    group: MoveGroup,
    n: Int,
    label: String,
): String =
    when (group) {
        MoveGroup.TITLES -> pluralStringResource(R.plurals.acq_mv_tile_titles, n)
        MoveGroup.WISHLIST -> pluralStringResource(R.plurals.acq_mv_tile_wishlist, n)
        MoveGroup.PEOPLE -> pluralStringResource(R.plurals.acq_mv_tile_people, n)
        else -> moveGroupName(group, label)
    }

@Composable
private fun NamesOption(
    value: MoveNamesChoice,
    current: MoveNamesChoice,
    title: Int,
    hint: Int,
    viewModel: MoveViewModel,
) {
    Row(
        Modifier.fillMaxWidth().clickable { viewModel.setNames(value) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        RadioButton(selected = current == value, onClick = { viewModel.setNames(value) })
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Hint(stringResource(hint))
        }
    }
}

@Composable
private fun AfterRow(
    title: String,
    hint: String,
    onShow: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Hint(hint)
        }
        TextButton(onClick = onShow) { Text(stringResource(R.string.acq_mv_show)) }
    }
}

@Composable
private fun MoveChangeBody(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
    onShow: (MoveListRequest) -> Unit,
    onNotMoved: () -> Unit,
) {
    val choose = state.choose
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        choose.groups.forEach { g ->
            val label = moveGroupName(g.group, g.label)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Column(Modifier.weight(1f)) {
                        Text("$label · ${g.count}", style = MaterialTheme.typography.bodyLarge)
                        val note = groupNoteText(g.note, g.noteCount)
                        if (note.isNotEmpty()) Hint(note)
                    }
                    TextButton(onClick = { onShow(MoveListRequest(MoveListKind.PLAN_GROUP, g.id, label)) }, enabled = g.count > 0) {
                        Text(stringResource(R.string.acq_mv_show))
                    }
                    Switch(checked = g.on, onCheckedChange = { viewModel.setGroup(g.id, it) }, enabled = g.canSkip)
                }
                if (g.showFormats) {
                    SwitchRow(
                        stringResource(R.string.acq_mv_formats),
                        !choose.noFormats,
                        { viewModel.setNoFormats(!it) },
                        hint = stringResource(R.string.acq_mv_formats_hint),
                    )
                }
            }
        }
        if (choose.folders.isNotEmpty()) {
            Text(stringResource(R.string.acq_mv_folders), style = MaterialTheme.typography.titleMedium)
            choose.folders.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                MoveFolderEditor(row, viewModel)
            }
        }
        if (choose.folders.isNotEmpty() || choose.showRules) {
            Text(
                if (choose.folders.isNotEmpty()) stringResource(R.string.acq_mv_advanced) else stringResource(R.string.acq_mv_path_rules),
                style = MaterialTheme.typography.titleMedium,
            )
            Hint(stringResource(R.string.acq_mv_path_rules_hint))
            choose.rules.forEachIndexed { index, rule -> MoveRuleEditor(index, rule, choose.rules.size, viewModel) }
            OutlinedButton(onClick = { viewModel.addRule("") }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.acq_mv_add_rule), Modifier.padding(start = Spacing.Small))
            }
        }
        if (choose.notMovedCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.acq_mv_not_imported), style = MaterialTheme.typography.bodyLarge)
                    Hint(pluralStringResource(R.plurals.acq_mv_not_imported_n, choose.notMovedCount, choose.notMovedCount))
                }
                TextButton(onClick = onNotMoved) { Text(stringResource(R.string.acq_mv_show)) }
            }
        }
        if (choose.emptyGroups.isNotEmpty()) Hint(stringResource(R.string.acq_mv_nothing_for, choose.emptyGroups.joinToString(", ")))
    }
}

@Composable
private fun groupNoteText(
    note: MoveGroupNote,
    n: Int,
): String =
    when (note) {
        MoveGroupNote.NONE -> ""
        MoveGroupNote.LEFT_OUT -> stringResource(R.string.acq_mv_note_left_out)
        MoveGroupNote.INVITE -> stringResource(R.string.acq_mv_note_invite)
        MoveGroupNote.KEYS -> stringResource(R.string.acq_mv_note_keys)
        MoveGroupNote.KEYS_OR_PASSWORDS -> stringResource(R.string.acq_mv_note_keys_pw)
        MoveGroupNote.CHECK_SHOWS -> pluralStringResource(R.plurals.acq_mv_note_check, n, n)
    }

private fun pickKey(row: MoveFolderRow): String =
    when (row.pick) {
        MovePickKind.NONE -> "none"
        MovePickKind.LEAVE -> "leave"
        MovePickKind.OTHER -> "other"
        MovePickKind.PATH -> "p:" + row.path
    }

@Composable
private fun MoveFolderEditor(
    row: MoveFolderRow,
    viewModel: MoveViewModel,
) {
    val noneLabel = if (row.unseen) stringResource(R.string.acq_mv_bring_without) else stringResource(R.string.acq_mv_dont_bring)
    val options = mutableListOf("none" to noneLabel)
    if (row.unseen) options += "leave" to stringResource(R.string.acq_mv_dont_bring)
    val paths = if (row.pick == MovePickKind.PATH && row.path !in row.options) row.options + row.path else row.options
    paths.forEach { options += ("p:$it") to it }
    options += "other" to stringResource(R.string.acq_mv_other_path)
    var other by remember(row.oldPath) { mutableStateOf(row.other) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small), modifier = Modifier.padding(vertical = Spacing.Small)) {
        Text(row.oldPath, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
        PickerField(
            label = stringResource(R.string.acq_mv_rule_to),
            options = options,
            selected = pickKey(row),
            onSelect = { key ->
                when {
                    key == "none" -> viewModel.pickFolder(row.oldPath, MovePickKind.NONE, "")
                    key == "leave" -> viewModel.pickFolder(row.oldPath, MovePickKind.LEAVE, "")
                    key == "other" -> viewModel.pickFolder(row.oldPath, MovePickKind.OTHER, "")
                    else -> viewModel.pickFolder(row.oldPath, MovePickKind.PATH, key.removePrefix("p:"))
                }
            },
            hint = if (row.pick == MovePickKind.PATH && row.path == row.suggested) stringResource(R.string.acq_mv_suggested) else null,
        )
        if (row.pick == MovePickKind.OTHER) {
            OutlinedTextField(
                value = other,
                onValueChange = {
                    other = it
                    viewModel.typeOther(row.oldPath, it)
                },
                label = { Text(stringResource(R.string.acq_mv_path_here_for, row.oldPath)) },
                placeholder = { Text("/data/movies") },
                singleLine = true,
                isError = row.problem != ch.orellbuehler.acolyte.feature.move.MovePathProblem.NONE,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.commitFolders() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) viewModel.commitFolders() },
            )
        }
        if (row.problem != ch.orellbuehler.acolyte.feature.move.MovePathProblem.NONE) FailText(movePathProblemText(row.problem))
        if (row.gone.isNotEmpty()) {
            FailText(stringResource(R.string.acq_mv_gone_note, row.gone))
            if (row.canAddRule) {
                OutlinedButton(onClick = { viewModel.addRule(row.oldPath) }) { Text(stringResource(R.string.acq_mv_add_rule)) }
            }
        }
        if (row.twins.isNotEmpty()) Hint(stringResource(R.string.acq_mv_twins, row.twins.joinToString(", ")))
    }
}

@Composable
private fun MoveRuleEditor(
    index: Int,
    rule: MoveRuleRow,
    size: Int,
    viewModel: MoveViewModel,
) {
    var from by remember(index, size) { mutableStateOf(rule.from) }
    var to by remember(index, size) { mutableStateOf(rule.to) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedTextField(
                    value = from,
                    onValueChange = {
                        from = it
                        viewModel.editRule(index, it, to)
                    },
                    label = { Text(stringResource(R.string.acq_mv_rule_from)) },
                    placeholder = { Text("/movies") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) viewModel.commitFolders() },
                )
                OutlinedTextField(
                    value = to,
                    onValueChange = {
                        to = it
                        viewModel.editRule(index, from, it)
                    },
                    label = { Text(stringResource(R.string.acq_mv_rule_to)) },
                    placeholder = { Text("/data/movies") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) viewModel.commitFolders() },
                )
            }
            IconButton(onClick = { viewModel.removeRule(index) }) {
                Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.acq_mv_rule_remove))
            }
        }
        if (rule.problem != ch.orellbuehler.acolyte.feature.move.MovePathProblem.NONE) FailText(movePathProblemText(rule.problem))
    }
}
