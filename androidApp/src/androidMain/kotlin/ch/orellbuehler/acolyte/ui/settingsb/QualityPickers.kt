package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleProblem
import ch.orellbuehler.acolyte.feature.quality.SettingsTraitChoice
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing

sealed interface QualityItem {
    data class Header(
        val text: String,
    ) : QualityItem

    data class Option(
        val id: String,
        val text: String,
    ) : QualityItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityDropdown(
    label: String,
    text: String,
    items: List<QualityItem>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
    problem: SettingsPickRuleProblem? = null,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(expanded = open && enabled, onExpandedChange = { if (enabled) open = it }) {
            OutlinedTextField(
                value = text,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                isError = problem != null,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
            )
            ExposedDropdownMenu(expanded = open && enabled, onDismissRequest = { open = false }) {
                items.forEach { item ->
                    when (item) {
                        is QualityItem.Header ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        item.text,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                onClick = {},
                                enabled = false,
                            )
                        is QualityItem.Option ->
                            DropdownMenuItem(
                                text = { Text(item.text) },
                                onClick = {
                                    open = false
                                    onSelect(item.id)
                                },
                            )
                    }
                }
            }
        }
        if (problem != null) {
            FailText(qualityProblemText(problem), Modifier.padding(top = Spacing.XSmall, start = Spacing.Large))
        } else if (hint != null) {
            Hint(hint, Modifier.padding(top = Spacing.XSmall, start = Spacing.Large))
        }
    }
}

@Composable
fun qualityTraitItems(choices: List<SettingsTraitChoice>): List<QualityItem> {
    val builtIn = choices.filter { !it.own }.map { QualityItem.Option(it.id, qualityTraitLabel(it)) }
    val own = choices.filter { it.own }.map { QualityItem.Option(it.id, qualityTraitLabel(it)) }
    return if (own.isEmpty()) builtIn else builtIn + QualityItem.Header(stringResource(R.string.acq_q_e_your_traits)) + own
}

@Composable
fun QualityField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Number,
    placeholder: String? = null,
    hint: String? = null,
    problem: SettingsPickRuleProblem? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = problem != null,
        supportingText =
            when {
                problem != null -> ({ Text(qualityProblemText(problem)) })
                hint != null -> ({ Text(hint) })
                else -> null
            },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun QualityProblemLine(problem: SettingsPickRuleProblem?) {
    if (problem != null) FailText(qualityProblemText(problem))
}

@Composable
fun QualityRemovableChip(
    text: String,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    InputChip(
        selected = false,
        onClick = onRemove,
        enabled = enabled,
        label = { Text(text) },
        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.padding(0.dp)) },
    )
}

@Composable
fun QualityDisclosure(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
    }
}
