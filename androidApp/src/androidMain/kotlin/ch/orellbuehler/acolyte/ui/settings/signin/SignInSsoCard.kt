package ch.orellbuehler.acolyte.ui.settings.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSso
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSsoError
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSsoPhase
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInState
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
internal fun SsoCard(
    state: SettingsSignInState,
    model: SignInModel,
    snackbar: SnackbarHostState,
) {
    val sso = state.sso
    if (sso.phase == SettingsSignInSsoPhase.HIDDEN) return
    SettingsCard(stringResource(R.string.settings_signin_sso_title), subtitle = stringResource(R.string.settings_signin_sso_intro)) {
        when (sso.phase) {
            SettingsSignInSsoPhase.HIDDEN -> Unit
            SettingsSignInSsoPhase.NEEDS_SESSION -> InlineNotice(stringResource(R.string.settings_signin_sso_needs_session), warn = false)
            SettingsSignInSsoPhase.LOCKED -> {
                Hint(stringResource(R.string.settings_signin_sso_locked))
                Button(onClick = { model.run { loadSso() } }) { Text(stringResource(R.string.settings_signin_sso_unlock)) }
            }
            SettingsSignInSsoPhase.LOADING ->
                SmallSpinner()
            SettingsSignInSsoPhase.FAILED -> {
                sso.loadError?.let { FailText(failureText(it)) }
                Button(onClick = { model.run { loadSso() } }) { Text(stringResource(R.string.settings_signin_try_again)) }
            }
            SettingsSignInSsoPhase.READY -> {
                if (sso.environment) SsoFacts(sso) else SsoForm(sso, model, snackbar)
                val forward = sso.view.forward
                val proxy =
                    if (forward.on) {
                        stringResource(R.string.settings_signin_sso_proxy_on, forward.issuer)
                    } else {
                        stringResource(R.string.settings_signin_sso_proxy_off)
                    }
                Hint(stringResource(R.string.settings_signin_sso_proxy, proxy))
            }
        }
    }
}

@Composable
private fun Fact(
    label: String,
    value: String,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SsoFacts(sso: SettingsSignInSso) {
    val view = sso.view
    InlineNotice(stringResource(R.string.settings_signin_sso_env_note), warn = false)
    Fact(
        stringResource(R.string.settings_signin_sso_title),
        stringResource(if (view.on) R.string.settings_signin_on else R.string.settings_signin_off),
    )
    if (view.on) {
        Fact(stringResource(R.string.settings_signin_sso_issuer), view.issuer)
        Fact(stringResource(R.string.settings_signin_sso_client_id), view.clientId)
        Fact(stringResource(R.string.settings_signin_sso_callback), view.redirectUrl)
        Fact(
            stringResource(R.string.settings_signin_sso_new_people),
            stringResource(if (view.allowJoin) R.string.settings_signin_sso_may_join else R.string.settings_signin_join_nobody),
        )
    }
}

@Composable
private fun SsoForm(
    sso: SettingsSignInSso,
    model: SignInModel,
    snackbar: SnackbarHostState,
) {
    val presenter = model.presenter
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.settings_signin_sso_copied)
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val busy = sso.saving
    SwitchRow(
        title = stringResource(R.string.settings_signin_sso_show_button),
        checked = sso.on,
        onChange = presenter::setSsoOn,
        enabled = !busy,
    )
    SignInField(
        label = stringResource(R.string.settings_signin_sso_issuer),
        value = sso.issuer,
        onValueChange = presenter::setSsoIssuer,
        enabled = !busy,
        hint = stringResource(R.string.settings_signin_sso_issuer_hint),
        placeholder = "https://auth.example.com/application/o/holyarrs/",
        mono = true,
        keyboard = KeyboardType.Uri,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedButton(onClick = { model.run { testSso() } }, enabled = !busy && sso.issuer.isNotBlank() && !sso.testing) {
            Text(stringResource(R.string.settings_signin_test))
        }
        if (sso.testing) SmallSpinner()
    }
    sso.test?.let { TestResult(it, okLabel = stringResource(R.string.settings_signin_sso_openid)) }
    SignInField(
        label = stringResource(R.string.settings_signin_sso_client_id),
        value = sso.clientId,
        onValueChange = presenter::setSsoClientId,
        enabled = !busy,
        mono = true,
    )
    SignInField(
        label = stringResource(R.string.settings_signin_sso_secret),
        value = sso.secret,
        onValueChange = presenter::setSsoSecret,
        enabled = !busy && !sso.publicClient,
        hint = stringResource(if (sso.secretKept) R.string.settings_signin_sso_secret_saved else R.string.settings_signin_sso_secret_new),
        password = true,
    )
    SwitchRow(
        title = stringResource(R.string.settings_signin_sso_public_client),
        checked = sso.publicClient,
        onChange = presenter::setSsoPublicClient,
        enabled = !busy,
    )
    OutlinedTextField(
        value = sso.callback,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.settings_signin_sso_callback_address)) },
        supportingText = { Text(stringResource(R.string.settings_signin_sso_callback_hint)) },
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(
        onClick = {
            clipboard.setText(AnnotatedString(sso.callback))
            scope.launch { snackbar.showSnackbar(copied) }
        },
    ) { Text(stringResource(R.string.settings_signin_sso_copy)) }
    SwitchRow(
        title = stringResource(R.string.settings_signin_sso_allow_join),
        checked = sso.allowJoin,
        onChange = presenter::setSsoAllowJoin,
        enabled = !busy,
    )
    SignInField(
        label = stringResource(R.string.settings_signin_sso_groups_claim),
        value = sso.groupsClaim,
        onValueChange = presenter::setSsoGroupsClaim,
        enabled = !busy,
        hint = stringResource(R.string.settings_signin_sso_groups_claim_hint),
        placeholder = "groups",
        mono = true,
    )
    SignInField(
        label = stringResource(R.string.settings_signin_sso_group_roles),
        value = sso.groupRoles,
        onValueChange = presenter::setSsoGroupRoles,
        enabled = !busy && sso.groupsClaim.isNotBlank(),
        hint = stringResource(R.string.settings_signin_sso_group_roles_hint),
        placeholder = "admins=admin, family=member",
        mono = true,
    )
    if (sso.opening) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sso.confirmEveryone, onCheckedChange = presenter::setSsoConfirmEveryone, enabled = !busy)
            Text(stringResource(R.string.settings_signin_sso_confirm_everyone), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (sso.moving && sso.view.links > 0) {
        InlineNotice(pluralStringResource(R.plurals.settings_signin_sso_moving, sso.view.links, sso.view.links))
    }
    if (sso.callback.startsWith("http://")) Hint(stringResource(R.string.settings_signin_sso_plain_http))
    SignInField(
        label = stringResource(R.string.settings_signin_sso_password),
        value = password,
        onValueChange = { password = it },
        enabled = !busy,
        hint = stringResource(R.string.settings_signin_sso_password_hint),
        password = true,
    )
    if (sso.showCode) {
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.take(11) },
            label = { Text(stringResource(R.string.settings_signin_sso_code)) },
            supportingText = if (sso.needsCode) null else ({ Text(stringResource(R.string.settings_signin_sso_code_hint)) }),
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    SsoError(sso)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Button(
            onClick = {
                val typedPassword = password
                val typedCode = code
                model.run { saveSso(typedPassword, typedCode) }
            },
            enabled = !busy && password.isNotEmpty() && (!sso.needsCode || code.isNotBlank()),
        ) { Text(stringResource(R.string.settings_signin_sso_save)) }
        if (busy) SmallSpinner()
    }
}

@Composable
private fun SsoError(sso: SettingsSignInSso) {
    val error = sso.saveError ?: return
    FailText(
        when (error) {
            SettingsSignInSsoError.RETYPE -> stringResource(R.string.settings_signin_sso_retype)
            SettingsSignInSsoError.CONFIRM_BOX -> stringResource(R.string.settings_signin_sso_confirm_box)
            SettingsSignInSsoError.WRONG_PROOF -> stringResource(R.string.settings_signin_sso_wrong_proof)
            SettingsSignInSsoError.GROUP_ROLES -> stringResource(R.string.settings_signin_sso_group_roles_hint)
            SettingsSignInSsoError.OTHER -> sso.error?.let { failureText(it) }.orEmpty()
        },
    )
}
