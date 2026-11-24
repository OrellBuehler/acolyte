package ch.orellbuehler.acolyte.ui.settings.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertField
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertForm
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerMomentGroup
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SheetFooter
import ch.orellbuehler.acolyte.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertFormSheet(
    form: SettingsServerAlertForm,
    viewModel: SettingsAlertsViewModel,
) {
    ModalBottomSheet(onDismissRequest = viewModel::cancelForm, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(
                    rememberScrollState(),
                ).imePadding()
                .padding(horizontal = Spacing.Screen)
                .padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(
                if (form.isNew) {
                    stringResource(R.string.settings_server_form_add_title)
                } else {
                    stringResource(R.string.settings_server_form_change_title, form.name)
                },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextBox(
                stringResource(R.string.settings_server_form_name),
                form.name,
                { viewModel.setText(SettingsServerAlertField.ALERT_NAME, it) },
                capitalization = KeyboardCapitalization.Sentences,
            )
            if (form.isNew) {
                PickerField(
                    stringResource(R.string.settings_server_form_kind),
                    SettingsServerAlertKind.entries.map { it to pickerKindLabel(it) },
                    form.kind,
                    viewModel::setKind,
                )
            }
            KindFields(form, viewModel)
            form.groups.forEach { group -> MomentGroup(group, viewModel) }
            CheckLine(
                stringResource(R.string.settings_server_local),
                form.allowLocalNetwork,
                viewModel::setAllowLocalNetwork,
                stringResource(R.string.settings_server_local_hint),
            )
            CheckLine(stringResource(R.string.settings_server_send), form.enabled, viewModel::setEnabled, null)
            form.error?.let { ServerErrorText(it) }
            SheetFooter(
                stringResource(R.string.settings_server_save),
                enabled = form.canSave,
                busy = form.saving,
                onCancel = viewModel::cancelForm,
                onPrimary = viewModel::save,
            )
        }
    }
}

@Composable
private fun KindFields(
    form: SettingsServerAlertForm,
    viewModel: SettingsAlertsViewModel,
) {
    val set: (SettingsServerAlertField, String) -> Unit = viewModel::setText
    val stored = stringResource(R.string.settings_server_stored)
    when (form.kind) {
        SettingsServerAlertKind.DISCORD ->
            TextBox(
                stringResource(R.string.settings_server_discord_link),
                form.url,
                { set(SettingsServerAlertField.URL, it) },
                hint = stringResource(R.string.settings_server_discord_link_hint),
                placeholder = if (form.urlStored) stored else "https://discord.com/api/webhooks/…",
                keyboard = KeyboardType.Uri,
                mono = true,
            )
        SettingsServerAlertKind.TELEGRAM -> {
            TextBox(
                stringResource(R.string.settings_server_bot_token),
                form.botToken,
                { set(SettingsServerAlertField.BOT_TOKEN, it) },
                hint = stringResource(if (form.isNew) R.string.settings_server_bot_token_new else R.string.settings_server_kept_keep),
                placeholder =
                    when {
                        form.isNew -> "123456789:AAH…"
                        form.botTokenStored -> stored
                        else -> stringResource(R.string.settings_server_paste_bot)
                    },
                secret = true,
            )
            TextBox(
                stringResource(R.string.settings_server_chat_id),
                form.chatId,
                { set(SettingsServerAlertField.CHAT_ID, it) },
                hint = stringResource(R.string.settings_server_chat_id_hint),
                placeholder = "-1001234567890, @mychannel",
                mono = true,
            )
        }
        SettingsServerAlertKind.NTFY -> {
            TextBox(
                stringResource(R.string.settings_server_server_address),
                form.serverUrl,
                { set(SettingsServerAlertField.SERVER_URL, it) },
                hint = stringResource(R.string.settings_server_ntfy_server_hint),
                placeholder = "https://ntfy.sh",
                keyboard = KeyboardType.Uri,
                mono = true,
            )
            TextBox(
                stringResource(R.string.settings_server_topic),
                form.topic,
                { set(SettingsServerAlertField.TOPIC, it) },
                hint = stringResource(R.string.settings_server_topic_hint),
                placeholder = "holyarrs-alerts",
                mono = true,
            )
            TextBox(
                stringResource(R.string.settings_server_access_token),
                form.appToken,
                { set(SettingsServerAlertField.APP_TOKEN, it) },
                hint =
                    stringResource(
                        if (form.canClearToken) R.string.settings_server_kept_keep_server else R.string.settings_server_access_token_new,
                    ),
                placeholder = if (form.canClearToken) stored else "tk_…",
                secret = true,
                enabled = !form.clearAppToken,
            )
            if (form.canClearToken) {
                CheckLine(
                    stringResource(R.string.settings_server_remove_token),
                    form.clearAppToken,
                    viewModel::setClearToken,
                    stringResource(R.string.settings_server_remove_token_hint),
                )
            }
        }
        SettingsServerAlertKind.GOTIFY -> {
            TextBox(
                stringResource(R.string.settings_server_server_address),
                form.serverUrl,
                { set(SettingsServerAlertField.SERVER_URL, it) },
                hint = stringResource(R.string.settings_server_gotify_server_hint),
                placeholder = "https://gotify.example.com",
                keyboard = KeyboardType.Uri,
                mono = true,
            )
            TextBox(
                stringResource(R.string.settings_server_app_token),
                form.appToken,
                { set(SettingsServerAlertField.APP_TOKEN, it) },
                hint =
                    stringResource(
                        if (form.isNew) R.string.settings_server_gotify_token_new else R.string.settings_server_kept_keep_server,
                    ),
                placeholder = appTokenPlaceholder(form, stored),
                secret = true,
            )
        }
        SettingsServerAlertKind.PUSHOVER -> {
            TextBox(
                stringResource(R.string.settings_server_user_key),
                form.userKey,
                { set(SettingsServerAlertField.USER_KEY, it) },
                hint = stringResource(R.string.settings_server_user_key_hint),
                mono = true,
            )
            TextBox(
                stringResource(R.string.settings_server_app_token),
                form.appToken,
                { set(SettingsServerAlertField.APP_TOKEN, it) },
                hint = stringResource(if (form.isNew) R.string.settings_server_pushover_token_new else R.string.settings_server_kept_keep),
                placeholder = appTokenPlaceholder(form, stored),
                secret = true,
            )
        }
        SettingsServerAlertKind.EMAIL -> EmailFields(form, set, stored)
        SettingsServerAlertKind.WEBHOOK -> {
            TextBox(
                stringResource(R.string.settings_server_hook_link),
                form.url,
                { set(SettingsServerAlertField.URL, it) },
                hint = stringResource(R.string.settings_server_hook_link_hint),
                placeholder = if (form.urlStored) stored else "https://example.com/holyarrs",
                keyboard = KeyboardType.Uri,
                mono = true,
            )
            TextBox(
                stringResource(R.string.settings_server_secret),
                form.secret,
                { set(SettingsServerAlertField.SECRET, it) },
                hint = stringResource(R.string.settings_server_secret_hint),
                placeholder = if (form.secretStored) stored else null,
                secret = true,
            )
            PickerField(
                stringResource(R.string.settings_server_secret_does),
                listOf(
                    "sign" to stringResource(R.string.settings_server_auth_sign),
                    "bearer" to stringResource(R.string.settings_server_auth_bearer),
                    "none" to stringResource(R.string.settings_server_auth_none),
                ),
                form.webhookAuth,
                onSelect = { set(SettingsServerAlertField.WEBHOOK_AUTH, it) },
            )
        }
    }
}

@Composable
private fun appTokenPlaceholder(
    form: SettingsServerAlertForm,
    stored: String,
): String? =
    when {
        form.isNew -> null
        form.appTokenStored -> stored
        else -> stringResource(R.string.settings_server_paste_app)
    }

@Composable
private fun EmailFields(
    form: SettingsServerAlertForm,
    set: (SettingsServerAlertField, String) -> Unit,
    stored: String,
) {
    TextBox(
        stringResource(R.string.settings_server_mail_server),
        form.smtpHost,
        { set(SettingsServerAlertField.SMTP_HOST, it) },
        placeholder = "smtp.example.com",
        keyboard = KeyboardType.Uri,
    )
    TextBox(
        stringResource(R.string.settings_server_port),
        form.smtpPort,
        { set(SettingsServerAlertField.SMTP_PORT, it) },
        keyboard = KeyboardType.Number,
        error = if (form.portValid) null else stringResource(R.string.settings_server_port_invalid),
    )
    PickerField(
        stringResource(R.string.settings_server_security),
        listOf(
            "starttls" to stringResource(R.string.settings_server_sec_starttls),
            "tls" to stringResource(R.string.settings_server_sec_tls),
            "none" to stringResource(R.string.settings_server_sec_none),
        ),
        form.smtpSecurity,
        onSelect = { set(SettingsServerAlertField.SMTP_SECURITY, it) },
    )
    TextBox(
        stringResource(R.string.settings_server_from_address),
        form.smtpFrom,
        { set(SettingsServerAlertField.SMTP_FROM, it) },
        keyboard = KeyboardType.Email,
    )
    TextBox(stringResource(R.string.settings_server_mail_user), form.smtpUser, { set(SettingsServerAlertField.SMTP_USER, it) })
    TextBox(
        stringResource(R.string.settings_server_mail_pass),
        form.smtpPass,
        { set(SettingsServerAlertField.SMTP_PASS, it) },
        placeholder = if (form.passStored) stored else null,
        secret = true,
    )
    TextBox(
        stringResource(R.string.settings_server_staff),
        form.staffTo,
        { set(SettingsServerAlertField.STAFF_TO, it) },
        hint = stringResource(R.string.settings_server_staff_hint),
        placeholder = "you@example.com, other@example.com",
        keyboard = KeyboardType.Email,
    )
}

@Composable
private fun MomentGroup(
    group: SettingsServerMomentGroup,
    viewModel: SettingsAlertsViewModel,
) {
    val name =
        stringResource(
            if (group.key ==
                "requests"
            ) {
                R.string.settings_server_group_requests
            } else {
                R.string.settings_server_group_library
            },
        )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Text(stringResource(R.string.settings_server_when_to_send, name), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(
                if (group.key == "requests") R.string.settings_server_group_requests_hint else R.string.settings_server_group_library_hint,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { viewModel.setGroup(group.key, !group.allOn) }) {
            Text(stringResource(if (group.allOn) R.string.settings_server_moments_none else R.string.settings_server_moments_all))
        }
        group.moments.forEach { moment ->
            CheckLine(momentTitle(moment.id), moment.on, { viewModel.setMoment(moment.id, it) }, momentHint(moment.id).ifEmpty { null })
        }
    }
}

@Composable
private fun CheckLine(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    hint: String?,
) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.weight(1f).padding(top = Spacing.XSmall)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TextBox(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String? = null,
    placeholder: String? = null,
    secret: Boolean = false,
    mono: Boolean = false,
    enabled: Boolean = true,
    error: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        supportingText = (error ?: hint)?.let { { Text(it) } },
        isError = error != null,
        enabled = enabled,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions =
            KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = false,
                keyboardType = if (secret) KeyboardType.Password else keyboard,
            ),
        textStyle =
            if (mono) {
                MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                )
            } else {
                MaterialTheme.typography.bodyLarge
            },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
