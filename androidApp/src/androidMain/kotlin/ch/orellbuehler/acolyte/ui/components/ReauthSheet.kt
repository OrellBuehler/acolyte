package ch.orellbuehler.acolyte.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.feature.account.ReauthMode
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch

internal const val REAUTH_CODE_MAX = 11

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReauthSheet(gate: ReauthGate) {
    val state by gate.state.collectAsStateWithLifecycle()
    if (!state.visible) return
    val scope = rememberCoroutineScope()
    val sdk = LocalSdk.current
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val canConfirm = password.isNotEmpty() && (!state.needsCode || code.isNotBlank()) && !state.busy
    val confirm = { if (canConfirm) scope.launch { gate.confirm(password, code) } }

    ModalBottomSheet(
        onDismissRequest = { if (!state.busy) gate.cancel() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.imePadding().padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.reauth_title), style = MaterialTheme.typography.titleLarge)
            when (state.mode) {
                ReauthMode.PASSWORD -> {
                    Text(
                        stringResource(R.string.reauth_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.reauth_password)) },
                        singleLine = true,
                        enabled = !state.busy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (state.needsCode) ImeAction.Next else ImeAction.Done,
                            ),
                        keyboardActions = KeyboardActions(onDone = { confirm() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.needsCode) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.take(REAUTH_CODE_MAX) },
                            label = { Text(stringResource(R.string.reauth_code)) },
                            singleLine = true,
                            enabled = !state.busy,
                            keyboardOptions =
                                KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                            keyboardActions = KeyboardActions(onDone = { confirm() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.error?.let { Text(reauthErrorText(it), color = MaterialTheme.acolyte.failText) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End)) {
                        TextButton(onClick = gate::cancel, enabled = !state.busy) { Text(stringResource(R.string.common_cancel)) }
                        Button(onClick = { confirm() }, enabled = canConfirm) {
                            if (state.busy) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(stringResource(R.string.reauth_confirm))
                            }
                        }
                    }
                }
                ReauthMode.SIGN_IN_AGAIN -> {
                    Text(stringResource(R.string.reauth_sign_in_again), style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End)) {
                        TextButton(onClick = gate::cancel) { Text(stringResource(R.string.common_cancel)) }
                        OutlinedButton(onClick = {
                            gate.cancel()
                            scope.launch { sdk.session.signOut() }
                        }) { Text(stringResource(R.string.account_sign_out)) }
                    }
                }
                ReauthMode.SET_PASSWORD -> {
                    Text(stringResource(R.string.reauth_set_password), style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = gate::cancel) { Text(stringResource(R.string.reauth_ok)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun reauthErrorText(error: ApiError): String =
    when (error.code) {
        ApiErrorCode.UNAUTHORIZED, ApiErrorCode.FORBIDDEN, ApiErrorCode.BAD_REQUEST -> stringResource(R.string.reauth_wrong)
        else -> errorText(error)
    }
