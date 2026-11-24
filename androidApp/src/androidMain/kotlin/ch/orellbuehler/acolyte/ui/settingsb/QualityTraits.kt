package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRulesState
import ch.orellbuehler.acolyte.feature.quality.SettingsTraitRemove
import ch.orellbuehler.acolyte.feature.quality.SettingsTraitRow
import ch.orellbuehler.acolyte.feature.quality.SettingsTraitSheet
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

private val MATCHES = listOf("name", "group", "size")

@Composable
fun QualityTraitsCard(
    state: SettingsPickRulesState,
    viewModel: QualityListViewModel,
) {
    SettingsCard(stringResource(R.string.acq_q_ot_title), subtitle = stringResource(R.string.acq_q_ot_desc)) {
        val error = state.traitsError
        if (error != null && !state.traitsLoaded) {
            FailText(errorText(error))
            OutlinedButton(onClick = viewModel::refresh) { Text(stringResource(R.string.common_retry)) }
        }
        if (state.traitsEmpty) {
            Text(stringResource(R.string.acq_q_ot_none), style = MaterialTheme.typography.titleSmall)
            Hint(stringResource(R.string.acq_q_ot_none_text))
        }
        state.handMade.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider()
            HandMadeRow(row, state.mayEdit, viewModel)
        }
        if (state.mayEdit && state.traitsLoaded) {
            Button(onClick = { viewModel.presenter.openTrait(0) }) { Text(stringResource(R.string.acq_q_ot_make)) }
        }
        if (state.trashTraits.isNotEmpty()) {
            ImportedGroup(
                title = stringResource(R.string.acq_q_ot_from_trash, state.trashTraits.size),
                note = stringResource(R.string.acq_q_ot_sync_again),
                rows = state.trashTraits,
                mayEdit = state.mayEdit,
                viewModel = viewModel,
            )
        }
        if (state.importedTraits.isNotEmpty()) {
            ImportedGroup(
                title = stringResource(R.string.acq_q_ot_from_old, state.importedTraits.size),
                note = stringResource(R.string.acq_q_ot_import_again),
                rows = state.importedTraits,
                mayEdit = state.mayEdit,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun traitSummary(row: SettingsTraitRow): String =
    when (row.match) {
        "name" -> stringResource(R.string.acq_q_ot_sum_name, row.terms.joinToString(", "))
        "group" -> stringResource(R.string.acq_q_ot_sum_group, row.terms.joinToString(", "))
        "size" -> {
            val parts = mutableListOf<String>()
            if (row.over.isNotEmpty()) parts += stringResource(R.string.acq_q_ot_size_over, row.over)
            if (row.upTo.isNotEmpty()) parts += stringResource(R.string.acq_q_ot_size_up_to, row.upTo)
            stringResource(R.string.acq_q_ot_sum_size, parts.joinToString(", "))
        }
        else -> row.terms.joinToString(", ")
    }

@Composable
private fun HandMadeRow(
    row: SettingsTraitRow,
    mayEdit: Boolean,
    viewModel: QualityListViewModel,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(traitSummary(row), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (mayEdit) {
            TextButton(onClick = { viewModel.presenter.openTrait(row.id) }) { Text(stringResource(R.string.acq_q_l_change)) }
            Column {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.acq_q_ot_more_for, row.name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.acq_q_ot_remove), color = MaterialTheme.acolyte.failText) },
                        onClick = {
                            menu = false
                            viewModel.presenter.askRemoveTrait(row.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportedGroup(
    title: String,
    note: String,
    rows: List<SettingsTraitRow>,
    mayEdit: Boolean,
    viewModel: QualityListViewModel,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = Spacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
    }
    if (open) {
        Hint(note)
        rows.forEach { row -> ImportedRow(row, mayEdit, viewModel) }
    }
}

@Composable
private fun ImportedRow(
    row: SettingsTraitRow,
    mayEdit: Boolean,
    viewModel: QualityListViewModel,
) {
    var menu by remember { mutableStateOf(false) }
    val specs = row.specs.map { spec -> specText(spec.name, spec.kind, spec.negate) }.joinToString(", ")
    val from =
        if (row.app == "sonarr" ||
            row.app == "radarr"
        ) {
            stringResource(R.string.acq_q_ot_from_app, row.app.replaceFirstChar { it.uppercase() })
        } else {
            ""
        }
    val line = listOf(from, specs).filter { it.isNotEmpty() }.joinToString(" · ")
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (line.isNotEmpty()) {
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (mayEdit) {
            Column {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.acq_q_ot_more_for, row.name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.acq_q_ot_remove), color = MaterialTheme.acolyte.failText) },
                        onClick = {
                            menu = false
                            viewModel.presenter.askRemoveTrait(row.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun specText(
    name: String,
    kind: String,
    negate: Boolean,
): String {
    val word = qualityLabel(qualitySpecKindRes(kind), kind)
    return stringResource(if (negate) R.string.acq_q_ot_spec_not else R.string.acq_q_ot_spec, name, word)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityTraitSheet(
    sheet: SettingsTraitSheet?,
    viewModel: QualityListViewModel,
) {
    if (sheet == null) return
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { if (!sheet.busy) viewModel.presenter.closeTrait() }, sheetState = state) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(
                stringResource(if (sheet.id == 0L) R.string.acq_q_ot_make_title else R.string.acq_q_ot_change_title),
                style = MaterialTheme.typography.titleLarge,
            )
            TraitSheetFields(sheet, viewModel)
            sheet.formError?.let { FailText(errorText(it)) }
            Row(Modifier.fillMaxWidth().padding(bottom = Spacing.Large), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { viewModel.presenter.closeTrait() }, enabled = !sheet.busy) {
                    Text(stringResource(R.string.acq_q_ot_cancel))
                }
                Button(onClick = { viewModel.saveTrait() }, enabled = !sheet.busy) {
                    if (sheet.busy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(if (sheet.id == 0L) R.string.acq_q_ot_make_button else R.string.acq_q_ot_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun TraitSheetFields(
    sheet: SettingsTraitSheet,
    viewModel: QualityListViewModel,
) {
    val presenter = viewModel.presenter
    val matchLabels = MATCHES.map { it to qualityLabel(qualityMatchRes(it), it) }
    OutlinedTextField(
        value = sheet.name,
        onValueChange = presenter::setTraitName,
        label = { Text(stringResource(R.string.acq_q_ot_name)) },
        isError = sheet.problem("name") != null,
        supportingText = sheet.problem("name")?.let { { Text(qualityProblemText(it)) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    PickerField(
        label = stringResource(R.string.acq_q_ot_look_at),
        options = matchLabels,
        selected = sheet.match,
        onSelect = presenter::setTraitMatch,
        hint = qualityMatchHintRes(sheet.match)?.let { stringResource(it) },
    )
    val terms = sheet.problem("terms")
    if (sheet.match == "size") {
        OutlinedTextField(
            value = sheet.over,
            onValueChange = presenter::setTraitOver,
            label = { Text(stringResource(R.string.acq_q_ot_over)) },
            isError = terms != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = sheet.upTo,
            onValueChange = presenter::setTraitUpTo,
            label = { Text(stringResource(R.string.acq_q_ot_up_to)) },
            isError = terms != null,
            supportingText = terms?.let { { Text(qualityProblemText(it)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        OutlinedTextField(
            value = sheet.terms,
            onValueChange = presenter::setTraitTerms,
            label = { Text(stringResource(if (sheet.match == "group") R.string.acq_q_ot_groups else R.string.acq_q_ot_words)) },
            isError = terms != null,
            supportingText = { Text(terms?.let { qualityProblemText(it) } ?: stringResource(R.string.acq_q_ot_terms_hint)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun QualityTraitRemoveDialog(
    remove: SettingsTraitRemove?,
    viewModel: QualityListViewModel,
) {
    if (remove == null) return
    AlertDialog(
        onDismissRequest = { if (!remove.busy) viewModel.presenter.cancelRemoveTrait() },
        title = { Text(stringResource(R.string.acq_q_ot_remove_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(remove.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.acq_q_ot_remove_note), style = MaterialTheme.typography.bodyMedium)
                remove.error?.let { FailText(errorText(it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.confirmRemoveTrait() }, enabled = !remove.busy) {
                if (remove.busy) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.acq_q_ot_remove), color = MaterialTheme.acolyte.failText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.presenter.cancelRemoveTrait() }, enabled = !remove.busy) {
                Text(stringResource(R.string.acq_q_ot_cancel))
            }
        },
    )
}
