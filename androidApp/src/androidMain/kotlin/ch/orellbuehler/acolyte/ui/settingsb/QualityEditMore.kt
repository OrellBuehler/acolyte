package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleChoices
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleEditState
import ch.orellbuehler.acolyte.feature.quality.SettingsTrashResult
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityTrashCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val trash = state.trash
    val synced = state.form.synced
    SettingsCard(stringResource(R.string.acq_q_e_trash_title), subtitle = stringResource(R.string.acq_q_e_trash_desc)) {
        if (synced != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(synced.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Hint(
                        stringResource(
                            R.string.acq_q_e_trash_synced,
                            stringResource(qualityTrashAppRes(synced.app)),
                            qualityLocalTime(synced.at),
                        ),
                    )
                }
                if (state.mayEdit) {
                    OutlinedButton(onClick = viewModel::syncAgain, enabled = !state.dirty && !trash.busy) {
                        Text(stringResource(R.string.acq_q_e_trash_sync_again))
                    }
                }
            }
        }
        if (state.mayEdit) {
            if (synced != null) HorizontalDivider()
            Text(
                stringResource(R.string.acq_q_e_trash_for),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val apps = SettingsPickRuleChoices.trashApps
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                apps.forEachIndexed { index, app ->
                    SegmentedButton(
                        selected = trash.app == app,
                        onClick = { viewModel.setTrashApp(app) },
                        shape = SegmentedButtonDefaults.itemShape(index, apps.size),
                        enabled = !trash.busy,
                    ) {
                        Text(stringResource(qualityTrashAppRes(app)))
                    }
                }
            }
            if (!trash.profilesLoaded) {
                OutlinedButton(onClick = viewModel::loadTrash, enabled = !trash.loading) {
                    if (trash.loading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.acq_q_e_trash_show))
                    }
                }
            } else if (trash.profiles.isEmpty()) {
                Hint(stringResource(R.string.acq_q_e_trash_none))
            } else {
                QualityDropdown(
                    label = stringResource(R.string.acq_q_e_trash_profile),
                    text = trash.choice?.name.orEmpty(),
                    items = trash.profiles.map { QualityItem.Option(it.id, it.name) },
                    onSelect = viewModel.presenter::setTrashPick,
                    hint = trash.choice?.description?.takeIf { it.isNotEmpty() },
                )
                Hint(stringResource(R.string.acq_q_e_trash_replaces))
                Button(onClick = viewModel::applyTrash, enabled = !state.dirty && trash.pick.isNotEmpty() && !trash.busy) {
                    if (trash.busy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.acq_q_e_trash_apply))
                    }
                }
                if (state.dirty) Hint(stringResource(R.string.acq_q_e_trash_save_first))
            }
        }
        trash.error?.let { FailText(errorText(it)) }
        trash.result?.let { TrashResult(it) }
    }
}

@Composable
private fun TrashResult(result: SettingsTrashResult) {
    var open by rememberSaveable { mutableStateOf(false) }
    HorizontalDivider()
    Text(
        stringResource(R.string.acq_q_e_trash_result, result.scored, result.added, result.updated),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (result.skipped.isNotEmpty()) {
        QualityDisclosure(stringResource(R.string.acq_q_e_trash_left_out, result.skipped.size), open) { open = !open }
        if (open) {
            result.skipped.forEach { skipped ->
                val detail =
                    if (skipped.kinds.isEmpty()) {
                        skipped.reason
                    } else {
                        val words = skipped.kinds.map { qualityLabel(qualitySpecKindRes(it), it) }.joinToString(", ")
                        stringResource(R.string.acq_q_e_trash_tests, words)
                    }
                Text(
                    "${skipped.name}: $detail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QualityMustNeverCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val presenter = viewModel.presenter
    val form = state.form
    var group by remember { mutableStateOf("") }
    val addGroup = {
        presenter.addBlockedGroup(group)
        group = ""
    }
    SettingsCard(stringResource(R.string.acq_q_e_must_never)) {
        Text(stringResource(R.string.acq_q_e_must_have), style = MaterialTheme.typography.labelLarge)
        TraitChips(form.mustHave, state, state.mayEdit) { presenter.removeMustHave(it) }
        if (state.mayEdit) {
            QualityDropdown(
                label = stringResource(R.string.acq_q_e_add_must),
                text = "",
                items = qualityTraitItems(state.traitChoices.filter { it.id !in form.mustHave }),
                onSelect = presenter::addMustHave,
            )
        }
        QualityProblemLine(state.problem("must_have"))
        HorizontalDivider()
        Text(stringResource(R.string.acq_q_e_never), style = MaterialTheme.typography.labelLarge)
        TraitChips(form.never, state, state.mayEdit) { presenter.removeNever(it) }
        if (state.mayEdit) {
            QualityDropdown(
                label = stringResource(R.string.acq_q_e_add_never),
                text = "",
                items = qualityTraitItems(state.neverChoices.filter { it.id !in form.never }),
                onSelect = presenter::addNever,
            )
        }
        QualityProblemLine(state.problem("never"))
        HorizontalDivider()
        Text(stringResource(R.string.acq_q_e_blocked_groups), style = MaterialTheme.typography.labelLarge)
        if (form.blockedGroups.isEmpty()) {
            Hint(stringResource(R.string.acq_q_e_nothing_yet))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                form.blockedGroups.forEach { QualityRemovableChip(it, state.mayEdit) { presenter.removeBlockedGroup(it) } }
            }
        }
        if (state.mayEdit) {
            OutlinedTextField(
                value = group,
                onValueChange = { group = it },
                label = { Text(stringResource(R.string.acq_q_e_add_group)) },
                trailingIcon = {
                    IconButton(onClick = addGroup, enabled = group.isNotBlank()) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.acq_q_e_add_group))
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addGroup() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Hint(stringResource(R.string.acq_q_e_blocked_hint))
        if (!form.music) {
            HorizontalDivider()
            Text(stringResource(R.string.acq_q_e_devices), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SettingsPickRuleChoices.devices.forEach { device ->
                    FilterChip(
                        selected = device in form.devices,
                        onClick = { presenter.toggleDevice(device) },
                        enabled = state.mayEdit,
                        label = { Text(qualityLabel(qualityDeviceRes(device), device)) },
                    )
                }
            }
            Hint(stringResource(R.string.acq_q_e_devices_hint))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TraitChips(
    ids: List<String>,
    state: SettingsPickRuleEditState,
    enabled: Boolean,
    onRemove: (String) -> Unit,
) {
    if (ids.isEmpty()) {
        Hint(stringResource(R.string.acq_q_e_nothing_yet))
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        ids.forEach { id -> QualityRemovableChip(qualityTraitLabel(state.choice(id)), enabled) { onRemove(id) } }
    }
}

@Composable
fun QualitySizeCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val presenter = viewModel.presenter
    val form = state.form
    SettingsCard(
        stringResource(R.string.acq_q_e_size),
        subtitle = stringResource(if (form.music) R.string.acq_q_e_size_music else R.string.acq_q_e_size_video),
    ) {
        QualityField(
            label = stringResource(R.string.acq_q_e_smallest),
            value = form.sizeMin,
            onChange = presenter::setSizeMin,
            enabled = state.mayEdit,
            keyboard = KeyboardType.Decimal,
            placeholder = stringResource(R.string.acq_q_e_no_limit),
        )
        QualityField(
            label = stringResource(R.string.acq_q_e_largest),
            value = form.sizeMax,
            onChange = presenter::setSizeMax,
            enabled = state.mayEdit,
            keyboard = KeyboardType.Decimal,
            placeholder = stringResource(R.string.acq_q_e_no_limit),
        )
        Hint(stringResource(R.string.acq_q_e_size_note))
        if (form.stepSizes.isNotEmpty()) {
            HorizontalDivider()
            form.stepSizes.forEach { row ->
                val label = qualityStepLabel(row.step)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (state.mayEdit) {
                        IconButton(onClick = { presenter.removeStepSize(row.step) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.acq_q_e_remove_step_size, label))
                        }
                    }
                }
                QualityField(
                    label = stringResource(R.string.acq_q_e_smallest),
                    value = row.min,
                    onChange = { presenter.setStepSize(row.step, "min", it) },
                    enabled = state.mayEdit,
                    keyboard = KeyboardType.Decimal,
                )
                QualityField(
                    label = stringResource(R.string.acq_q_e_preferred),
                    value = row.preferred,
                    onChange = { presenter.setStepSize(row.step, "preferred", it) },
                    enabled = state.mayEdit,
                    keyboard = KeyboardType.Decimal,
                    placeholder = stringResource(R.string.acq_q_e_no_preferred),
                )
                QualityField(
                    label = stringResource(R.string.acq_q_e_largest),
                    value = row.max,
                    onChange = { presenter.setStepSize(row.step, "max", it) },
                    enabled = state.mayEdit,
                    keyboard = KeyboardType.Decimal,
                    placeholder = stringResource(R.string.acq_q_e_no_limit),
                )
            }
            Hint(stringResource(R.string.acq_q_e_step_sizes_note))
        }
        if (state.mayEdit && form.sizeChoices.isNotEmpty()) {
            QualityDropdown(
                label = stringResource(R.string.acq_q_e_add_step_size),
                text = "",
                items = form.sizeChoices.map { QualityItem.Option(it, qualityStepLabel(it)) },
                onSelect = presenter::addStepSize,
            )
        }
        QualityProblemLine(state.problem("size_limits"))
        SwitchRow(
            title = stringResource(R.string.acq_q_e_replace_small),
            checked = form.replaceUnderMin,
            onChange = presenter::setReplaceUnderMin,
            hint = stringResource(R.string.acq_q_e_replace_small_hint),
            enabled = state.mayEdit,
        )
    }
}

@Composable
fun QualityWaitingCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val presenter = viewModel.presenter
    val form = state.form
    SettingsCard(stringResource(R.string.acq_q_e_waiting), subtitle = stringResource(R.string.acq_q_e_waiting_desc)) {
        Text(
            stringResource(R.string.acq_q_e_prefer),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val prefers = SettingsPickRuleChoices.prefers
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            prefers.forEachIndexed { index, prefer ->
                SegmentedButton(
                    selected = form.prefer == prefer,
                    onClick = { presenter.setPrefer(prefer) },
                    shape = SegmentedButtonDefaults.itemShape(index, prefers.size),
                    enabled = state.mayEdit,
                ) {
                    Text(qualityLabel(qualityPreferRes(prefer), prefer))
                }
            }
        }
        Hint(stringResource(R.string.acq_q_e_wins_tie))
        QualityProblemLine(state.problem("prefer"))
        QualityField(
            label = stringResource(R.string.acq_q_e_wait_torrent),
            value = form.waitTorrent,
            onChange = presenter::setWaitTorrent,
            enabled = state.mayEdit,
            problem = state.problem("wait_minutes_torrent"),
        )
        QualityField(
            label = stringResource(R.string.acq_q_e_wait_usenet),
            value = form.waitUsenet,
            onChange = presenter::setWaitUsenet,
            enabled = state.mayEdit,
            problem = state.problem("wait_minutes_usenet"),
        )
        SwitchRow(
            title = stringResource(R.string.acq_q_e_skip_wait),
            checked = form.skipWait,
            onChange = presenter::setSkipWait,
            enabled = state.mayEdit,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QualitySourcesCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val presenter = viewModel.presenter
    val form = state.form
    var tag by remember { mutableStateOf("") }
    val commit = {
        presenter.addTag(tag)
        tag = ""
    }
    SettingsCard(stringResource(R.string.acq_q_e_sources), subtitle = stringResource(R.string.acq_q_e_sources_desc)) {
        Text(stringResource(R.string.acq_q_e_tags), style = MaterialTheme.typography.labelLarge)
        if (form.tags.isEmpty()) {
            Hint(stringResource(R.string.acq_q_e_nothing_yet))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                form.tags.forEach { QualityRemovableChip(it, state.mayEdit) { presenter.removeTag(it) } }
            }
        }
        if (state.mayEdit) {
            OutlinedTextField(
                value = tag,
                onValueChange = {
                    if (it.contains(',')) {
                        presenter.addTag(it)
                        tag = ""
                    } else {
                        tag = it
                    }
                },
                label = { Text(stringResource(R.string.acq_q_e_add_tag)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QualityLanguageCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val presenter = viewModel.presenter
    val form = state.form
    SettingsCard(stringResource(R.string.acq_q_e_language), subtitle = stringResource(R.string.acq_q_e_language_desc)) {
        Text(stringResource(R.string.acq_q_e_languages), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            SettingsPickRuleChoices.languages.forEach { code ->
                FilterChip(
                    selected = code in form.languages,
                    onClick = { presenter.toggleLanguage(code) },
                    enabled = state.mayEdit,
                    label = { Text(qualityLanguageLabel(code)) },
                )
            }
        }
        QualityProblemLine(state.problem("language"))
        if (form.frenchWanted) {
            HorizontalDivider()
            Text(stringResource(R.string.acq_q_e_french), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SettingsPickRuleChoices.frenchVersions.forEach { version ->
                    FilterChip(
                        selected = version in form.frenchVersions,
                        onClick = { presenter.toggleFrench(version) },
                        enabled = state.mayEdit,
                        label = { Text(version) },
                    )
                }
            }
            Hint(stringResource(R.string.acq_q_e_french_hint))
        }
        HorizontalDivider()
        SwitchRow(
            title = stringResource(R.string.acq_q_e_original),
            checked = form.originalOk,
            onChange = presenter::setOriginalOk,
            hint = stringResource(R.string.acq_q_e_original_hint),
            enabled = state.mayEdit,
        )
        if (form.originalOk && form.languages.isNotEmpty()) {
            QualityField(
                label = stringResource(R.string.acq_q_e_lang_wait),
                value = form.languageWait,
                onChange = presenter::setLanguageWait,
                enabled = state.mayEdit,
                hint = stringResource(R.string.acq_q_e_lang_wait_hint),
                problem = state.problem("wait_days"),
            )
        }
    }
}
