package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.quality.SettingsNameCheckCopy
import ch.orellbuehler.acolyte.feature.quality.SettingsNameCheckResult
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRulesState
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun QualityNameCheckCard(
    state: SettingsPickRulesState,
    viewModel: QualityListViewModel,
) {
    val check = state.check
    val presenter = viewModel.presenter
    val copies = stringResource(R.string.acq_q_nc_copies)
    val options = listOf(0L to copies) + state.rules.map { it.id to it.name }
    SettingsCard(stringResource(R.string.acq_q_nc_title), subtitle = stringResource(R.string.acq_q_nc_desc)) {
        OutlinedTextField(
            value = check.name,
            onValueChange = presenter::setCheckName,
            label = { Text(stringResource(R.string.acq_q_nc_name)) },
            placeholder = { Text(stringResource(R.string.acq_q_nc_placeholder)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        PickerField(
            label = stringResource(R.string.acq_q_nc_rules),
            options = options,
            selected = check.rulesId,
            onSelect = presenter::setCheckRules,
        )
        OutlinedTextField(
            value = check.size,
            onValueChange = presenter::setCheckSize,
            label = { Text(stringResource(R.string.acq_q_nc_size)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = viewModel::runCheck, enabled = check.canCheck) {
            if (check.busy) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.acq_q_nc_check))
            }
        }
        check.error?.let { FailText(stringResource(R.string.acq_q_nc_failed) + ": " + errorText(it)) }
        check.result?.let { NameCheckResult(it) }
    }
}

@Composable
private fun NameCheckResult(result: SettingsNameCheckResult) {
    HorizontalDivider()
    Text(
        stringResource(R.string.acq_q_nc_read_as),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val title = result.title.ifEmpty { "—" } + if (result.year > 0) " (${result.year})" else ""
    Text(title, style = MaterialTheme.typography.titleMedium)
    if (result.tags.isNotEmpty()) QualityTagRow(result.tags)
    if (result.matchName.isNotEmpty()) {
        Text(stringResource(R.string.acq_q_nc_matches, result.matchName), style = MaterialTheme.typography.bodyMedium)
        if (result.matchDetail.isNotEmpty()) Hint(result.matchDetail)
    }
    val hint =
        when (result.hint) {
            "no_title" -> stringResource(R.string.acq_q_nc_no_title)
            "no_album" -> stringResource(R.string.acq_q_nc_no_album, result.matchName)
            "no_copies" -> stringResource(R.string.acq_q_nc_no_copies)
            else -> null
        }
    if (hint != null) Hint(hint)
    result.copies.forEach { CopyBlock(it) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualityTagRow(tags: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        tags.forEach { QualityTagChip(it) }
    }
}

@Composable
fun QualityTagChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.background(MaterialTheme.acolyte.chip, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun reasonColor(effect: String): Color =
    when (effect) {
        "fail" -> MaterialTheme.acolyte.failText
        "hold", "warn" -> MaterialTheme.acolyte.waitText
        else -> MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun CopyBlock(copy: SettingsNameCheckCopy) {
    HorizontalDivider()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(copy.header, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(qualityLabel(qualityVerdictRes(copy.verdict), copy.verdict), copy.tone)
            if (copy.passed) Text(stringResource(R.string.acq_q_nc_points, copy.points), style = MaterialTheme.typography.bodyMedium)
            if (copy.hasFile) QualityTagChip(stringResource(R.string.acq_q_nc_has_file))
        }
        if (copy.showRules) Hint(copy.rulesName)
        copy.reasons.forEach { reason ->
            Text(reason.text, style = MaterialTheme.typography.bodyMedium, color = reasonColor(reason.effect))
        }
    }
}
