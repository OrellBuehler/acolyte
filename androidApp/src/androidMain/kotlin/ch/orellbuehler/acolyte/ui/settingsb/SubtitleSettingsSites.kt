package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.acquisition.SETTINGS_SUBTITLE_ACCOUNT_FIELDS
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleSiteFlag
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleSiteForm
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleSiteNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleSiteRow
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleState
import ch.orellbuehler.acolyte.feature.acquisition.covers
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
internal fun SubtitleSitesSection(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
    onRemove: (Long) -> Unit,
) {
    val form = state.form
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.acq_sub_sites_title), style = MaterialTheme.typography.titleLarge)
                Hint(stringResource(R.string.acq_sub_sites_sub))
            }
            if (form == null && state.sites.isNotEmpty() && state.offeredKinds.isNotEmpty()) {
                OutlinedButton(onClick = viewModel::startAdd) { Text(stringResource(R.string.acq_sub_add_site)) }
            }
        }
        if (form != null) SubtitleSiteForm(state, form, viewModel)
        val rows = state.siteRows.filter { form == null || form.isNew || it.id != form.id }
        if (state.sites.isEmpty() && form == null) {
            SettingsCard(stringResource(R.string.acq_sub_none_title), subtitle = stringResource(R.string.acq_sub_none_text)) {
                if (state.offeredKinds.isNotEmpty()) {
                    OutlinedButton(onClick = viewModel::startAdd) { Text(stringResource(R.string.acq_sub_add_site)) }
                }
            }
        }
        rows.forEach { row -> SubtitleSiteCard(row, viewModel, onRemove) }
    }
}

@Composable
private fun SubtitleSiteForm(
    state: SettingsSubtitleState,
    form: SettingsSubtitleSiteForm,
    viewModel: SubtitleSettingsViewModel,
) {
    var limits by rememberSaveable { mutableStateOf(false) }
    val kind = state.formKind
    val title = if (form.isNew) stringResource(R.string.acq_sub_add_site) else stringResource(R.string.acq_sub_change_title, form.name)
    SettingsCard(title) {
        if (form.isNew) {
            Text(stringResource(R.string.acq_sub_which_site), style = MaterialTheme.typography.titleSmall)
            state.offeredKinds.forEach { option ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    RadioButton(selected = option.kind == form.kind, onClick = { viewModel.pickKind(option.kind) })
                    Column(Modifier.weight(1f).padding(top = 12.dp)) {
                        Text("${option.name} (${subtitleCoversText(option.covers())})", style = MaterialTheme.typography.bodyLarge)
                        if (option.needsAccount) StatusChip(stringResource(R.string.acq_sub_needs_yours), Tone.FAIL)
                        if (option.note.isNotBlank()) Hint(option.note)
                    }
                }
            }
        }
        SubtitleTextField(stringResource(R.string.acq_sub_name), form.name, viewModel::setFormName)
        SiteNumber(
            R.string.acq_sub_priority,
            SettingsSubtitleSiteNumber.PRIORITY,
            form,
            viewModel,
            stringResource(R.string.acq_sub_priority_hint),
        )
        SubtitleTextField(stringResource(R.string.acq_sub_address), form.url, viewModel::setFormUrl, keyboardType = KeyboardType.Uri)
        if (kind?.needsAccount == true) {
            Text(stringResource(R.string.acq_sub_your_account), style = MaterialTheme.typography.titleSmall)
            Hint(
                stringResource(R.string.acq_sub_account_note) +
                    if (!form.isNew && form.hasAccount) " " + stringResource(R.string.acq_sub_account_keep) else "",
            )
            kind.account.filter { it in SETTINGS_SUBTITLE_ACCOUNT_FIELDS }.forEach { field ->
                SubtitleTextField(
                    label = subtitleAccountLabel(field),
                    value = form.account(field),
                    onChange = { viewModel.setFormAccount(field, it) },
                    secret = field != "username",
                    isError = field in form.missing,
                )
            }
            if (form.missing.isNotEmpty()) {
                val fields = form.missing.map { subtitleAccountLabel(it).lowercase() }
                FailText(stringResource(R.string.acq_sub_needs_fill, fields.joinToString(", ")))
            }
        }
        TextButton(onClick = { limits = !limits }) { Text(stringResource(R.string.acq_sub_limits)) }
        if (limits) {
            SiteNumber(R.string.acq_sub_gap, SettingsSubtitleSiteNumber.GAP_MS, form, viewModel)
            SiteNumber(R.string.acq_sub_per_window, SettingsSubtitleSiteNumber.PER_WINDOW, form, viewModel)
            SiteNumber(R.string.acq_sub_window, SettingsSubtitleSiteNumber.WINDOW_SECONDS, form, viewModel)
            SiteNumber(R.string.acq_sub_per_day, SettingsSubtitleSiteNumber.DOWNLOADS_PER_DAY, form, viewModel)
        }
        SwitchRow(
            stringResource(R.string.acq_sub_local),
            form.flag(SettingsSubtitleSiteFlag.ALLOW_LOCAL_NETWORK.key),
            { viewModel.setFormFlag(SettingsSubtitleSiteFlag.ALLOW_LOCAL_NETWORK.key, it) },
            hint = stringResource(R.string.acq_sub_local_hint),
        )
        SwitchRow(
            stringResource(R.string.acq_sub_machine_made),
            form.flag(SettingsSubtitleSiteFlag.MACHINE_MADE.key),
            { viewModel.setFormFlag(SettingsSubtitleSiteFlag.MACHINE_MADE.key, it) },
            hint = stringResource(R.string.acq_sub_machine_made_hint),
        )
        SwitchRow(
            stringResource(R.string.acq_sub_on),
            form.flag(SettingsSubtitleSiteFlag.ENABLED.key),
            { viewModel.setFormFlag(SettingsSubtitleSiteFlag.ENABLED.key, it) },
        )
        form.error?.let { FailText(subtitleErrorText(it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Button(onClick = viewModel::submitForm, enabled = form.canSubmit) { Text(stringResource(R.string.acq_sub_save)) }
            OutlinedButton(onClick = viewModel::cancelForm) { Text(stringResource(R.string.acq_sub_cancel)) }
        }
    }
}

@Composable
private fun SiteNumber(
    label: Int,
    field: SettingsSubtitleSiteNumber,
    form: SettingsSubtitleSiteForm,
    viewModel: SubtitleSettingsViewModel,
    hint: String? = null,
) {
    val valid = form.numberValid(field.key)
    SubtitleTextField(
        label = stringResource(label),
        value = form.number(field.key),
        onChange = { viewModel.setFormNumber(field.key, it) },
        hint = if (valid) hint else stringResource(R.string.acq_sub_range, field.min, field.max),
        isError = !valid,
        keyboardType = KeyboardType.Number,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubtitleSiteCard(
    row: SettingsSubtitleSiteRow,
    viewModel: SubtitleSettingsViewModel,
    onRemove: (Long) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Column(Modifier.weight(1f)) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        row.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(subtitleStateLabel(row.state), row.state.tone)
            }
            Text(subtitleStateText(row), style = MaterialTheme.typography.bodyMedium)
            Hint(siteLine(row))
            SiteTest(row)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = { viewModel.testSite(row.id) }, enabled = !row.testing) {
                    Text(stringResource(R.string.acq_sub_site_test))
                }
                if (row.offered) {
                    OutlinedButton(onClick = { viewModel.togglePaused(row.id) }) {
                        Text(stringResource(if (row.turnedOff) R.string.acq_sub_turn_on else R.string.acq_sub_pause))
                    }
                    OutlinedButton(onClick = { viewModel.startChange(row.id) }) { Text(stringResource(R.string.acq_sub_change)) }
                }
                OutlinedButton(onClick = { onRemove(row.id) }) { Text(stringResource(R.string.acq_sub_remove)) }
            }
        }
    }
}

@Composable
private fun siteLine(row: SettingsSubtitleSiteRow): String {
    val parts = mutableListOf(row.kindName, subtitleCoversText(row.covers), stringResource(R.string.acq_sub_priority_n, row.priority))
    if (row.needsAccount) parts += stringResource(if (row.hasAccount) R.string.acq_sub_account_saved else R.string.acq_sub_account_not_set)
    if (row.allowLocalNetwork) parts += stringResource(R.string.acq_sub_may_reach)
    if (row.downloadsMax > 0) parts += stringResource(R.string.acq_sub_downloads_today, row.downloadsToday, row.downloadsMax)
    return parts.joinToString(" · ")
}

@Composable
private fun SiteTest(row: SettingsSubtitleSiteRow) {
    val ok = row.testOk ?: return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        if (ok) {
            val label = listOf(row.kindName, row.testExtra).filter { it.isNotEmpty() }.joinToString(" · ")
            StatusChip(label, Tone.OK)
        } else {
            StatusChip(stringResource(R.string.acq_sub_test_failed), Tone.FAIL)
            val error = row.testError
            FailText(if (error != null) subtitleErrorText(error) else row.testMessage, Modifier.weight(1f))
        }
    }
}
