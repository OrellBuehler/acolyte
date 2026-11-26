package ch.orellbuehler.acolyte.ui.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.AccountMediaServer
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.account.AccountSecurityNotice
import ch.orellbuehler.acolyte.feature.account.AccountSecurityProblem
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.OpenInBrowserOnce
import ch.orellbuehler.acolyte.ui.components.PasswordField
import ch.orellbuehler.acolyte.ui.components.PlexWaitingPanel
import ch.orellbuehler.acolyte.ui.components.ReauthSheet
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.common.R as CommonR

fun qrCells(rows: List<String>): List<List<Boolean>> = rows.map { row -> row.map { it == '1' } }

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val vm = viewModel { AccountSecurityViewModel(sdk) }
    val state by vm.state.collectAsStateWithLifecycle()
    val account = currentAccount() ?: return
    val notice =
        when (state.notice) {
            AccountSecurityNotice.PASSWORD_CHANGED -> stringResource(R.string.myaccount_pw_changed)
            AccountSecurityNotice.PASSWORD_SET -> stringResource(R.string.myaccount_pw_set)
            AccountSecurityNotice.TWO_FACTOR_OFF -> stringResource(R.string.myaccount_tf_off)
            AccountSecurityNotice.CODES_RENEWED -> stringResource(R.string.myaccount_tf_renewed)
            AccountSecurityNotice.PLEX_LINKED -> stringResource(R.string.myaccount_plex_linked, state.noticeText)
            AccountSecurityNotice.PLEX_UNLINKED -> stringResource(R.string.myaccount_plex_unlinked)
            AccountSecurityNotice.MEDIA_LINKED -> stringResource(R.string.myaccount_media_linked)
            AccountSecurityNotice.MEDIA_UNLINKED -> stringResource(R.string.myaccount_media_unlinked)
            AccountSecurityNotice.SSO_UNLINKED -> stringResource(R.string.myaccount_sso_unlinked)
            null -> null
        }
    ToastOnce(notice) { vm.consume() }
    state.plexUrl?.let { OpenInBrowserOnce(it) }

    ScreenScaffold(title = stringResource(R.string.myaccount_security), onBack = onBack) { padding ->
        StatusContent(state.status, isEmpty = false, onRefresh = { vm.refresh() }, modifier = Modifier.padding(padding)) {
            FormColumn(PaddingValuesZero) {
                if (account.mustSetUpTwoFactor) NoticeBanner(stringResource(R.string.myaccount_tf_required))
                state.error?.let { ErrorBanner(it) }
                state.problem?.let {
                    val text =
                        when (it) {
                            AccountSecurityProblem.PASSWORD_SHORT -> stringResource(R.string.myaccount_pw_short, state.minPasswordLength)
                            AccountSecurityProblem.PASSWORDS_DIFFER -> stringResource(R.string.myaccount_pw_differ)
                            AccountSecurityProblem.CODE_INVALID -> stringResource(R.string.myaccount_tf_code_invalid)
                            AccountSecurityProblem.PLEX_TIMEOUT -> stringResource(CommonR.string.signin_error_plex_timeout)
                        }
                    ErrorBannerText(text)
                }
                PasswordCard(account, vm)
                TwoFactorCard(account, vm)
                LinkedCard(account, vm)
            }
        }
    }
    ReauthSheet(vm.presenter.reauth)
}

@Composable
private fun PasswordCard(
    account: Account,
    vm: AccountSecurityViewModel,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val minimum = state.minPasswordLength
    FormCard(stringResource(R.string.myaccount_password)) {
        if (!open) {
            Text(
                stringResource(if (account.hasPassword) R.string.myaccount_pw_hint else R.string.myaccount_pw_none),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = { open = true }) {
                Text(stringResource(if (account.hasPassword) R.string.myaccount_pw_change else R.string.myaccount_pw_set_button))
            }
        } else {
            if (account.hasPassword) {
                PasswordField(current, { current = it }, stringResource(R.string.myaccount_pw_current))
            }
            PasswordField(
                new,
                { new = it },
                stringResource(R.string.myaccount_pw_new),
                placeholder = stringResource(R.string.myaccount_pw_placeholder, minimum),
                error = if (passwordTooShort(new, minimum)) stringResource(R.string.myaccount_pw_short, minimum) else null,
            )
            PasswordField(
                confirm,
                { confirm = it },
                stringResource(R.string.myaccount_pw_confirm),
                error = if (passwordMismatch(new, confirm)) stringResource(R.string.myaccount_pw_differ) else null,
                imeAction = ImeAction.Done,
            )
            Text(stringResource(R.string.myaccount_pw_signs_out), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Button(
                    enabled = !state.busy && passwordReady(new, confirm, minimum) && (current.isNotEmpty() || !account.hasPassword),
                    onClick = {
                        vm.changePassword(current, new, confirm)
                        open = false
                        current = ""
                        new = ""
                        confirm = ""
                    },
                ) { Text(stringResource(if (account.hasPassword) R.string.myaccount_pw_change else R.string.myaccount_pw_set_button)) }
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        }
    }
}

@Composable
private fun TwoFactorCard(
    account: Account,
    vm: AccountSecurityViewModel,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var code by rememberSaveable { mutableStateOf("") }
    var managing by rememberSaveable { mutableStateOf(false) }
    FormCard(stringResource(R.string.myaccount_tf)) {
        LabeledRow(stringResource(R.string.myaccount_tf_hint)) {
            StatusChip(
                stringResource(if (account.twoFactor) R.string.myaccount_on else R.string.myaccount_off),
                if (account.twoFactor) Tone.OK else Tone.IDLE,
            )
        }
        val setup = state.setup
        when {
            state.backupCodes.isNotEmpty() -> {
                Text(stringResource(R.string.myaccount_tf_codes), style = MaterialTheme.typography.bodyMedium)
                Text(state.backupCodes.joinToString("\n"), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(state.backupCodes.joinToString("\n"))) }) {
                        Text(stringResource(R.string.myaccount_copy))
                    }
                    OutlinedButton(onClick = {
                        val send =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, state.backupCodes.joinToString("\n"))
                            }
                        context.startActivity(Intent.createChooser(send, null))
                    }) { Text(stringResource(R.string.myaccount_share)) }
                    TextButton(onClick = { vm.dismissCodes() }) { Text(stringResource(R.string.myaccount_close)) }
                }
            }
            setup != null -> {
                Text(stringResource(R.string.myaccount_tf_scan), style = MaterialTheme.typography.bodyMedium)
                if (setup.qr.isNotEmpty()) QrCode(setup.qr)
                Text(stringResource(R.string.myaccount_tf_or_type), style = MaterialTheme.typography.bodySmall)
                Text(setup.secret.chunked(4).joinToString(" "), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(setup.secret)) },
                    ) { Text(stringResource(R.string.myaccount_copy)) }
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(setup.uri))) }
                    }) { Text(stringResource(R.string.myaccount_tf_open_app)) }
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    label = { Text(stringResource(R.string.myaccount_tf_six)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Button(enabled = code.length == 6 && !state.busy, onClick = {
                        vm.enableTwoFactor(code)
                        code = ""
                    }) { Text(stringResource(R.string.myaccount_tf_turn_on)) }
                    TextButton(onClick = { vm.cancelTwoFactor() }) { Text(stringResource(R.string.common_cancel)) }
                }
            }
            !account.twoFactor ->
                OutlinedButton(enabled = !state.busy, onClick = { vm.startTwoFactor() }) {
                    Text(stringResource(R.string.myaccount_tf_set_up))
                }
            !managing -> OutlinedButton(onClick = { managing = true }) { Text(stringResource(R.string.myaccount_tf_manage)) }
            else -> {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(11) },
                    label = { Text(stringResource(R.string.myaccount_tf_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    OutlinedButton(enabled = code.isNotBlank() && !state.busy, onClick = {
                        vm.renewCodes(code)
                        code = ""
                    }) { Text(stringResource(R.string.myaccount_tf_renew)) }
                    OutlinedButton(enabled = code.isNotBlank() && !state.busy, onClick = {
                        vm.disableTwoFactor(code)
                        code = ""
                        managing = false
                    }) { Text(stringResource(R.string.myaccount_tf_turn_off), color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { managing = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            }
        }
    }
}

@Composable
private fun QrCode(rows: List<String>) {
    val cells = qrCells(rows)
    val size = cells.size + 8
    Canvas(Modifier.size(220.dp).background(Color.White)) {
        val cell = this.size.width / size
        cells.forEachIndexed { y, line ->
            line.forEachIndexed { x, dark ->
                if (dark) drawRect(Color.Black, Offset((x + 4) * cell, (y + 4) * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

@Composable
private fun LinkedCard(
    account: Account,
    vm: AccountSecurityViewModel,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var form by rememberSaveable { mutableStateOf<Long?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    FormCard(stringResource(R.string.myaccount_linked)) {
        Text(stringResource(R.string.myaccount_linked_hint), style = MaterialTheme.typography.bodySmall)
        val plex = state.plex
        if (state.plexWaiting) {
            PlexWaitingPanel(onCancel = { vm.cancelPlex() })
        } else if (plex.linked) {
            LabeledRow(stringResource(R.string.myaccount_plex_as, plex.name)) {
                if (account.hasPassword) {
                    TextButton(onClick = { vm.unlinkPlex() }) { Text(stringResource(R.string.myaccount_unlink)) }
                }
            }
            if (!account.hasPassword) {
                Text(
                    stringResource(R.string.myaccount_unlink_needs_password),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else if (account.hasPassword && account.usesCookies) {
            OutlinedButton(enabled = !state.busy, onClick = { vm.linkPlex() }) { Text(stringResource(R.string.myaccount_plex_link)) }
        }
        state.sso.forEach { link ->
            LabeledRow(stringResource(R.string.myaccount_sso_as, link.issuer, link.name)) {
                TextButton(onClick = { vm.unlinkSso(link) }) { Text(stringResource(R.string.myaccount_unlink)) }
            }
        }
        Text(stringResource(R.string.myaccount_sso_web), style = MaterialTheme.typography.bodySmall)
        if (state.media.servers.isNotEmpty() || state.media.links.isNotEmpty()) {
            HorizontalDivider()
            Text(stringResource(R.string.myaccount_watched), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.myaccount_watched_hint), style = MaterialTheme.typography.bodySmall)
            state.media.links.forEach { link ->
                LabeledRow(stringResource(R.string.myaccount_media_as, link.provider.replaceFirstChar { it.uppercase() }, link.name)) {
                    TextButton(onClick = { vm.unlinkMedia(link.provider) }) { Text(stringResource(R.string.myaccount_unlink)) }
                }
            }
            state.media.servers.forEach { server: AccountMediaServer ->
                val linked = state.media.links.any { it.provider == server.kind }
                if (form == server.id) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.myaccount_media_name, server.name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PasswordField(password, { password = it }, stringResource(R.string.myaccount_pw_label))
                    Text(stringResource(R.string.myaccount_media_note, server.name), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Button(enabled = name.isNotBlank() && password.isNotEmpty() && !state.busy, onClick = {
                            vm.linkMedia(server.id, name, password)
                            password = ""
                            form = null
                        }) { Text(stringResource(R.string.myaccount_media_link)) }
                        TextButton(onClick = { form = null }) { Text(stringResource(R.string.common_cancel)) }
                    }
                } else {
                    OutlinedButton(onClick = {
                        form = server.id
                        name = ""
                        password = ""
                    }) {
                        Text(stringResource(if (linked) R.string.myaccount_media_change else R.string.myaccount_media_link_to, server.name))
                    }
                }
            }
        }
    }
}
