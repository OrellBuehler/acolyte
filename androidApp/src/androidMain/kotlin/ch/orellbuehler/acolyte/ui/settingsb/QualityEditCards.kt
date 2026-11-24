package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleChoices
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleEditState
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun QualityNameCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val presenter = viewModel.presenter
    val form = state.form
    SettingsCard(stringResource(R.string.acq_q_e_name_kinds)) {
        if (state.preset) {
            Text(
                form.name + " · " + qualityKindsLine(form.kinds),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Hint(stringResource(R.string.acq_q_e_fixed))
        } else {
            val problem = state.problem("name")
            OutlinedTextField(
                value = form.name,
                onValueChange = presenter::setName,
                enabled = state.mayEdit,
                label = { Text(stringResource(R.string.acq_q_e_name)) },
                isError = problem != null,
                supportingText = problem?.let { { Text(qualityProblemText(it)) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.acq_q_e_for),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsPickRuleChoices.kinds.forEach { kind ->
                val on = kind in form.kinds
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = on, enabled = state.mayEdit, role = Role.Checkbox) { presenter.setKind(kind, it) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    Checkbox(checked = on, onCheckedChange = null, enabled = state.mayEdit)
                    Text(qualityKindLabel(kind), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Hint(stringResource(R.string.acq_q_e_switching))
            QualityProblemLine(state.problem("kinds"))
        }
    }
}

@Composable
fun QualityStepsCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val presenter = viewModel.presenter
    val form = state.form
    val best = form.stepsBestFirst
    SettingsCard(stringResource(R.string.acq_q_e_steps), subtitle = stringResource(R.string.acq_q_e_steps_desc)) {
        best.forEachIndexed { index, step ->
            val label = qualityStepLabel(step.quality)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .weight(1f)
                        .toggleable(value = step.allowed, enabled = state.mayEdit, role = Role.Switch) {
                            presenter.setStepAllowed(step.quality, it)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    Switch(checked = step.allowed, onCheckedChange = null, enabled = state.mayEdit)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        if (step.allowed && step.quality == form.goodEnoughAt) {
                            StatusChip(stringResource(R.string.acq_q_e_good_enough), Tone.OK)
                        }
                    }
                }
                if (state.mayEdit) {
                    IconButton(onClick = { presenter.moveStep(step.quality, true) }, enabled = index > 0) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.acq_q_e_move_up))
                    }
                    IconButton(onClick = { presenter.moveStep(step.quality, false) }, enabled = index < best.lastIndex) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.acq_q_e_move_down))
                    }
                }
            }
        }
        QualityProblemLine(state.problem("steps"))
        val allowed = form.allowedBestFirst
        QualityDropdown(
            label = stringResource(R.string.acq_q_e_good_enough_at),
            text = if (allowed.any { it.quality == form.goodEnoughAt }) qualityStepLabel(form.goodEnoughAt) else "",
            items = allowed.map { QualityItem.Option(it.quality, qualityStepLabel(it.quality)) },
            onSelect = presenter::setGoodEnoughAt,
            enabled = state.mayEdit,
            hint = stringResource(R.string.acq_q_e_good_enough_hint),
            problem = state.problem("good_enough_at"),
        )
        QualityField(
            label = stringResource(R.string.acq_q_e_good_enough_points),
            value = form.goodEnoughPoints,
            onChange = presenter::setGoodEnoughPoints,
            enabled = state.mayEdit,
            hint = stringResource(R.string.acq_q_e_good_enough_points_hint),
            problem = state.problem("good_enough_points"),
        )
    }
}

@Composable
fun QualityTraitsEditCard(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
    onOpen: (Any) -> Unit,
) {
    val presenter = viewModel.presenter
    val form = state.form
    SettingsCard(stringResource(R.string.acq_q_e_traits), subtitle = stringResource(R.string.acq_q_e_traits_desc)) {
        if (form.traits.isEmpty()) Hint(stringResource(R.string.acq_q_e_no_traits))
        form.traits.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider()
            val label = qualityTraitLabel(state.choice(row.trait))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                QualityDropdown(
                    label = stringResource(R.string.acq_q_e_trait_n, index + 1),
                    text = label,
                    items = qualityTraitItems(state.freeTraitChoices(index)),
                    onSelect = { presenter.setTraitKey(index, it) },
                    enabled = state.mayEdit,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    QualityField(
                        label = stringResource(R.string.acq_q_e_points_for, label),
                        value = row.points,
                        onChange = { presenter.setTraitPoints(index, it) },
                        modifier = Modifier.weight(1f),
                        enabled = state.mayEdit,
                    )
                    if (state.mayEdit) {
                        IconButton(onClick = { presenter.removeTrait(index) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.acq_q_e_remove_trait, label))
                        }
                    }
                }
            }
        }
        QualityProblemLine(state.problem("traits"))
        if (state.mayEdit) {
            OutlinedButton(onClick = presenter::addTrait, enabled = state.canAddTrait) { Text(stringResource(R.string.acq_q_e_add_trait)) }
            OutlinedButton(onClick = { onOpen(Routes.settingsPickRules) }) { Text(stringResource(R.string.acq_q_e_own_trait)) }
        }
        QualityAdvanced(state, viewModel)
    }
}

@Composable
private fun QualityAdvanced(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
) {
    val presenter = viewModel.presenter
    val form = state.form
    var expanded by rememberSaveable { mutableStateOf(false) }
    val fields = listOf("order", "upgrade_min_points", "min_points", "proper_policy", "max_age_days")
    val open = expanded || fields.any { state.problem(it) != null }
    HorizontalDivider()
    QualityDisclosure(stringResource(R.string.acq_q_e_advanced), open) { expanded = !open }
    if (!open) return
    QualityDropdown(
        label = stringResource(R.string.acq_q_e_which_wins),
        text = qualityLabel(qualityOrderRes(form.order), form.order),
        items = SettingsPickRuleChoices.orders.map { QualityItem.Option(it, qualityLabel(qualityOrderRes(it), it)) },
        onSelect = presenter::setOrder,
        enabled = state.mayEdit,
        hint = qualityOrderDescRes(form.order)?.let { stringResource(it) },
        problem = state.problem("order"),
    )
    QualityField(
        label = stringResource(R.string.acq_q_e_upgrade),
        value = form.upgradeMinPoints,
        onChange = presenter::setUpgradeMinPoints,
        enabled = state.mayEdit,
        hint = stringResource(R.string.acq_q_e_upgrade_hint),
        problem = state.problem("upgrade_min_points"),
    )
    QualityField(
        label = stringResource(R.string.acq_q_e_min_points),
        value = form.minPoints,
        onChange = presenter::setMinPoints,
        enabled = state.mayEdit,
        problem = state.problem("min_points"),
    )
    QualityDropdown(
        label = stringResource(R.string.acq_q_e_fixed_copies),
        text = qualityLabel(qualityProperRes(form.properMode), form.properMode),
        items = SettingsPickRuleChoices.properModes.map { QualityItem.Option(it, qualityLabel(qualityProperRes(it), it)) },
        onSelect = presenter::setProperMode,
        enabled = state.mayEdit,
        hint = qualityProperDescRes(form.properMode)?.let { stringResource(it) },
        problem = state.problem("proper_policy"),
    )
    if (form.properMode == "take_any" || form.properMode == "same_group_only") {
        QualityField(
            label = stringResource(R.string.acq_q_e_proper_age),
            value = form.properAge,
            onChange = presenter::setProperAge,
            enabled = state.mayEdit,
            hint = stringResource(R.string.acq_q_e_proper_age_hint),
            problem = state.problem("max_age_days"),
        )
    }
}
