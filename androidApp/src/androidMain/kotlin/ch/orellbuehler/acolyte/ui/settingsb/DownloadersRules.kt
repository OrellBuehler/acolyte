package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderNumber
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderRulesState
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderSwitch
import ch.orellbuehler.acolyte.ui.components.ErrorView
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun DownloaderRulesSection(
    state: SettingsDownloaderRulesState,
    viewModel: DownloaderRulesViewModel,
) {
    val error = state.status.error
    when {
        !state.allowed -> Unit
        state.status.phase == Phase.LOADING ->
            Column(Modifier.fillMaxWidth().padding(Spacing.Large), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
        state.status.phase == Phase.FAILED && error != null ->
            ErrorView(
                error,
                Modifier.padding(Spacing.Large),
                onRetry = viewModel::refresh,
            )
        else -> DownloaderRulesCard(state, viewModel)
    }
}

@Composable
private fun RuleSwitch(
    state: SettingsDownloaderRulesState,
    viewModel: DownloaderRulesViewModel,
    which: SettingsDownloaderSwitch,
    title: Int,
    hint: Int? = null,
) {
    SwitchRow(
        stringResource(title),
        state.flag(which),
        { viewModel.setSwitch(which, it) },
        hint = hint?.let { stringResource(it) },
        enabled = state.canEdit,
    )
}

@Composable
private fun NumberField(
    state: SettingsDownloaderRulesState,
    viewModel: DownloaderRulesViewModel,
    which: SettingsDownloaderNumber,
    label: Int,
    hint: Int? = null,
) {
    OutlinedTextField(
        value = state.number(which),
        onValueChange = { viewModel.setNumber(which, it) },
        label = { Text(stringResource(label)) },
        supportingText = hint?.let { { Text(stringResource(it)) } },
        singleLine = true,
        enabled = state.canEdit,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DownloaderRulesCard(
    state: SettingsDownloaderRulesState,
    viewModel: DownloaderRulesViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_dl_wrong)) {
        RuleSwitch(
            state,
            viewModel,
            SettingsDownloaderSwitch.REMOVE_FAILED,
            R.string.acq_dl_remove_failed_dl,
            R.string.acq_dl_remove_failed_hint,
        )
        RuleSwitch(state, viewModel, SettingsDownloaderSwitch.AUTO_ADD, R.string.acq_dl_auto_add, R.string.acq_dl_auto_add_hint)
        RuleSwitch(state, viewModel, SettingsDownloaderSwitch.SAB_OUTSIDE, R.string.acq_dl_sab_outside, R.string.acq_dl_sab_outside_hint)
        Text(stringResource(R.string.acq_dl_when_remove), style = MaterialTheme.typography.titleSmall)
        RuleSwitch(state, viewModel, SettingsDownloaderSwitch.DELETE_FILES, R.string.acq_dl_delete_files)
        RuleSwitch(state, viewModel, SettingsDownloaderSwitch.TURN_DOWN, R.string.acq_dl_turn_down, R.string.acq_dl_turn_down_hint)
        RuleSwitch(state, viewModel, SettingsDownloaderSwitch.SEARCH_AGAIN, R.string.acq_dl_search_again, R.string.acq_dl_search_again_hint)
        Row(
            Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.acq_dl_advanced), style = MaterialTheme.typography.titleSmall)
            Icon(if (advanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
        }
        if (advanced) {
            NumberField(
                state,
                viewModel,
                SettingsDownloaderNumber.STALLED_AFTER,
                R.string.acq_dl_stalled_after,
                R.string.acq_dl_stalled_after_hint,
            )
            NumberField(
                state,
                viewModel,
                SettingsDownloaderNumber.STALLED_ACT,
                R.string.acq_dl_try_next_release,
                R.string.acq_dl_try_next_release_hint,
            )
            SwitchRow(
                stringResource(R.string.acq_dl_dead_rule),
                state.deadOn,
                viewModel::setDeadRule,
                hint = stringResource(R.string.acq_dl_dead_rule_hint),
                enabled = state.canEdit,
            )
            if (state.deadOn) {
                NumberField(
                    state,
                    viewModel,
                    SettingsDownloaderNumber.DEAD_MINUTES,
                    R.string.acq_dl_dead_minutes,
                    R.string.acq_dl_dead_minutes_hint,
                )
                NumberField(state, viewModel, SettingsDownloaderNumber.CRAWL_KBPS, R.string.acq_dl_crawl_kbps)
                NumberField(state, viewModel, SettingsDownloaderNumber.CRAWL_HOURS, R.string.acq_dl_crawl_hours, R.string.acq_dl_crawl_hint)
            }
            RuleSwitch(
                state,
                viewModel,
                SettingsDownloaderSwitch.FAIL_DANGEROUS,
                R.string.acq_dl_fail_programs,
                R.string.acq_dl_fail_programs_hint,
            )
            RuleSwitch(state, viewModel, SettingsDownloaderSwitch.REPLACE_VANISHED, R.string.acq_dl_replace, R.string.acq_dl_replace_hint)
            RuleSwitch(state, viewModel, SettingsDownloaderSwitch.TRY_NEXT, R.string.acq_dl_try_next_dl, R.string.acq_dl_try_next_dl_hint)
            NumberField(state, viewModel, SettingsDownloaderNumber.SPACING, R.string.acq_dl_spacing)
        }
        if (!state.canEdit) Hint(stringResource(R.string.requests_not_allowed_text))
    }
}

@Composable
fun DownloaderRulesSaveBar(
    state: SettingsDownloaderRulesState,
    viewModel: DownloaderRulesViewModel,
    modifier: Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding()) {
            state.saveError?.let { FailText(downloaderErrorText(it)) }
            state.issue?.let { FailText(downloaderRulesIssueText(it)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.acq_dl_unsaved), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::discard) { Text(stringResource(R.string.acq_dl_discard)) }
                Button(onClick = viewModel::save, enabled = state.canSave) { Text(stringResource(R.string.acq_dl_save)) }
            }
        }
    }
}
