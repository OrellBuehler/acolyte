package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthState
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsReauthDialog(
    state: SettingsReauthState,
    onConfirm: (String, String) -> Unit,
    onCancel: () -> Unit,
) {
    if (!state.needed) return
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.settings_reauth_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(stringResource(R.string.settings_reauth_text))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.settings_reauth_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.settings_reauth_code)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let { FailText(errorText(it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password, code) }, enabled = password.isNotEmpty() && !state.busy) {
                Text(stringResource(R.string.settings_reauth_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.settings_reauth_cancel)) } },
    )
}
