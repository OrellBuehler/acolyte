package ch.orellbuehler.acolyte.ui.settings.mediaservers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
internal fun MediaServerField(
    value: String,
    revision: Int,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String? = null,
    secret: Boolean = false,
    mono: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int = Int.MAX_VALUE,
) {
    var text by remember(revision) { mutableStateOf(value) }
    val style = MaterialTheme.typography.bodyLarge
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.take(maxLength)
            onChange(text)
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = hint?.let { { Text(it) } },
        textStyle = if (mono) style.copy(fontFamily = FontFamily.Monospace) else style,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (secret) KeyboardType.Password else keyboardType,
                capitalization = if (mono || secret) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
                autoCorrectEnabled = !(mono || secret),
            ),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun MediaServerCheckRow(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Row(
        modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.padding(vertical = Spacing.XSmall)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Hint(hint)
        }
    }
}
