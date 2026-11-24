package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleRemove
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleRow
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRulesState
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun QualitySetsCard(
    state: SettingsPickRulesState,
    viewModel: QualityListViewModel,
    onOpen: (Any) -> Unit,
) {
    SettingsCard(stringResource(R.string.acq_q_l_sets), subtitle = stringResource(R.string.acq_q_l_sets_sub)) {
        if (state.rules.isEmpty()) {
            Text(stringResource(R.string.acq_q_l_none), style = MaterialTheme.typography.titleSmall)
            Hint(stringResource(R.string.acq_q_l_none_text))
            if (state.mayEdit) {
                Button(onClick = { onOpen(Routes.settingsPickRuleEdit(0, 0)) }) { Text(stringResource(R.string.acq_q_l_add)) }
            }
        } else {
            state.rules.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                QualityRuleRow(row, state.mayEdit, viewModel, onOpen)
            }
            if (state.mayEdit) Hint(stringResource(R.string.acq_q_l_hint))
        }
    }
}

@Composable
private fun QualityRuleRow(
    row: SettingsPickRuleRow,
    mayEdit: Boolean,
    viewModel: QualityListViewModel,
    onOpen: (Any) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val used = if (row.inUse == 0) stringResource(R.string.acq_q_not_used) else qualityUsedIn(row.inUse)
    val line = qualityKindsLine(row.kinds) + " · " + used
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (row.readyChip) StatusChip(stringResource(R.string.acq_q_l_ready_made), Tone.IDLE)
        }
        TextButton(onClick = { onOpen(Routes.settingsPickRuleEdit(row.id, 0)) }) {
            Text(stringResource(if (mayEdit) R.string.acq_q_l_change else R.string.acq_q_l_open))
        }
        if (mayEdit) {
            Column {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.acq_q_l_more_for, row.name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.acq_q_l_copy)) },
                        onClick = {
                            menu = false
                            onOpen(Routes.settingsPickRuleEdit(0, row.id))
                        },
                    )
                    if (!row.preset) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.acq_q_l_remove), color = MaterialTheme.acolyte.failText) },
                            onClick = {
                                menu = false
                                viewModel.presenter.askRemove(row.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QualityRemoveDialog(
    remove: SettingsPickRuleRemove?,
    viewModel: QualityListViewModel,
) {
    if (remove == null) return
    val use = remove.use
    AlertDialog(
        onDismissRequest = { if (!remove.busy) viewModel.presenter.cancelRemove() },
        title = { Text(stringResource(R.string.acq_q_l_remove_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(remove.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.acq_q_l_remove_note), style = MaterialTheme.typography.bodyMedium)
                if (use != null) {
                    FailText(stringResource(R.string.acq_q_l_remove_in_use))
                    Text(qualityUsedBy(use), style = MaterialTheme.typography.bodyMedium)
                } else {
                    if (remove.inUse > 0) {
                        Text(
                            stringResource(R.string.acq_q_l_remove_used, qualityUsedIn(remove.inUse)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    remove.error?.let { FailText(errorText(it)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.confirmRemove() }, enabled = !remove.busy) {
                if (remove.busy) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.acq_q_l_remove), color = MaterialTheme.acolyte.failText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.presenter.cancelRemove() }, enabled = !remove.busy) {
                Text(stringResource(R.string.acq_q_l_cancel))
            }
        },
    )
}

@Composable
fun QualityWhereCard(
    state: SettingsPickRulesState,
    onOpen: (Any) -> Unit,
) {
    SettingsCard(stringResource(R.string.acq_q_l_where)) {
        WhereRow(R.string.acq_q_l_where_folders, R.string.acq_q_l_where_folders_text) { onOpen(Routes.settingsLibraryFolders) }
        HorizontalDivider()
        WhereRow(R.string.acq_q_l_where_requests, R.string.acq_q_l_where_requests_text) { onOpen(Routes.settingsRequests) }
        HorizontalDivider()
        WhereRow(R.string.acq_q_l_where_lists, R.string.acq_q_l_where_lists_text) { onOpen(Routes.settingsOutsideLists) }
        HorizontalDivider()
        WhereRow(R.string.acq_q_l_where_defaults, R.string.acq_q_l_where_defaults_text, null)
        HorizontalDivider()
        WhereRow(R.string.acq_q_l_where_title, null, null)
        if (!state.mayEdit) Hint(stringResource(R.string.acq_q_l_read_only))
    }
}

@Composable
private fun WhereRow(
    title: Int,
    text: Int?,
    onClick: (() -> Unit)?,
) {
    val base = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        base.fillMaxWidth().padding(vertical = Spacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            if (text != null) Hint(stringResource(text))
        }
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
