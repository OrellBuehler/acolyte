package ch.orellbuehler.acolyte.ui.invite

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.session.AccountRules
import ch.orellbuehler.acolyte.session.InviteKind
import ch.orellbuehler.acolyte.session.InvitePhase
import ch.orellbuehler.acolyte.session.InviteState
import ch.orellbuehler.acolyte.session.InviteTarget
import ch.orellbuehler.acolyte.session.SignInErrorCode
import ch.orellbuehler.acolyte.session.SignInWay
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.account.passwordMismatch
import ch.orellbuehler.acolyte.ui.account.passwordReady
import ch.orellbuehler.acolyte.ui.account.passwordTooShort
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.OpenInBrowserOnce
import ch.orellbuehler.acolyte.ui.components.PasswordField
import ch.orellbuehler.acolyte.ui.components.PlexWaitingPanel
import ch.orellbuehler.acolyte.ui.components.signInErrorText
import ch.orellbuehler.acolyte.ui.connect.AuthFrame
import ch.orellbuehler.acolyte.ui.connect.BusyButton
import ch.orellbuehler.acolyte.ui.connect.ErrorLine
import ch.orellbuehler.acolyte.ui.connect.Heading
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun InviteScreen(
    target: InviteTarget,
    onClose: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "invite-${target.token}") { InviteViewModel(sdk, target) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    OpenInBrowserOnce(state.plexUrl)
    BackHandler {
        if (state.plexUrl != null) viewModel.cancelPlex() else onClose()
    }
    AuthFrame {
        Heading(stringResource(R.string.invite_title), state.serverName)
        val error = state.error
        when {
            state.phase == InvitePhase.GONE -> Gone(onClose)
            state.phase == InvitePhase.LOADING && error != null -> {
                ErrorLine(stringResource(R.string.invite_open_failed))
                BusyButton(stringResource(R.string.common_retry), state.busy, enabled = true, onClick = viewModel::retry)
                TextButton(onClick = onClose) { Text(stringResource(R.string.invite_to_sign_in)) }
            }
            state.phase == InvitePhase.LOADING || state.phase == InvitePhase.DONE -> CircularProgressIndicator()
            state.signedInAs != null -> SignedInNotice(state.signedInAs.orEmpty(), state.busy, viewModel, onClose)
            state.plexUrl != null -> PlexWaitingPanel(onCancel = viewModel::cancelPlex)
            error?.code == SignInErrorCode.LINKED -> {
                ErrorLine(stringResource(CommonR.string.signin_error_linked))
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.invite_to_sign_in)) }
            }
            else -> {
                OpenInvite(state, viewModel, onClose)
                error?.let { ErrorLine(signInErrorText(it)) }
            }
        }
    }
}

@Composable
private fun Gone(onClose: () -> Unit) {
    ErrorLine(stringResource(R.string.invite_gone))
    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.invite_to_sign_in)) }
}

@Composable
private fun SignedInNotice(
    name: String,
    busy: Boolean,
    viewModel: InviteViewModel,
    onClose: () -> Unit,
) {
    NoticeBanner(stringResource(R.string.invite_signed_in_as, name))
    BusyButton(stringResource(R.string.invite_sign_out_join), busy, enabled = true, onClick = viewModel::signOutAndJoin)
    TextButton(onClick = onClose) { Text(stringResource(R.string.invite_go_home)) }
}

@Composable
private fun OpenInvite(
    state: InviteState,
    viewModel: InviteViewModel,
    onClose: () -> Unit,
) {
    var mediaWay by rememberSaveable { mutableStateOf<SignInWay?>(null) }
    Text(
        when (state.kind) {
            InviteKind.ADMIN -> stringResource(R.string.invite_as_admin)
            InviteKind.KID -> stringResource(R.string.invite_as_kid, state.kidLimit)
            InviteKind.MEMBER -> stringResource(R.string.invite_as_member)
        },
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    val way = mediaWay
    if (way != null) {
        MediaServerForm(way, state, viewModel) { mediaWay = null }
        return
    }
    state.ways.forEach { option ->
        val label = stringResource(option.tile(state.ways.size == 1))
        if (option == SignInWay.PLEX) {
            BusyButton(label, state.busy, enabled = true, onClick = viewModel::startPlex)
        } else {
            OutlinedButton(onClick = { mediaWay = option }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
    }
    if (state.ways.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                stringResource(R.string.signin_or),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.weight(1f))
        }
    }
    JoinForm(state, viewModel)
    TextButton(onClick = onClose, enabled = !state.busy) { Text(stringResource(R.string.invite_to_sign_in)) }
}

@Composable
private fun JoinForm(
    state: InviteState,
    viewModel: InviteViewModel,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val minimum = state.minPasswordLength
    val ready = AccountRules.validName(name) && passwordReady(password, confirm, minimum)
    val submit = { if (ready && !state.busy) viewModel.join(name, password) }
    OutlinedTextField(
        value = name,
        onValueChange = { name = AccountRules.cleanName(it) },
        label = { Text(stringResource(R.string.signin_name)) },
        supportingText = { Text(stringResource(R.string.signin_setup_username_hint)) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.signin_password),
        placeholder = stringResource(R.string.signin_setup_password_hint, minimum),
        error = if (passwordTooShort(password, minimum)) stringResource(R.string.signin_setup_password_short, minimum) else null,
        enabled = !state.busy,
    )
    PasswordField(
        value = confirm,
        onValueChange = { confirm = it },
        label = stringResource(R.string.signin_setup_confirm),
        error = if (passwordMismatch(password, confirm)) stringResource(R.string.signin_setup_mismatch) else null,
        enabled = !state.busy,
        imeAction = ImeAction.Done,
        onDone = submit,
    )
    BusyButton(stringResource(R.string.invite_join), state.busy, enabled = ready) { submit() }
}

@Composable
private fun MediaServerForm(
    way: SignInWay,
    state: InviteState,
    viewModel: InviteViewModel,
    onOther: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val provider = stringResource(if (way == SignInWay.EMBY) R.string.invite_provider_emby else R.string.invite_provider_jellyfin)
    val ready = name.isNotBlank() && password.isNotEmpty()
    val submit = { if (ready && !state.busy) viewModel.joinMediaServer(way, name, password) }
    Text(
        stringResource(R.string.invite_media_lead, provider),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(128) },
        label = { Text(stringResource(R.string.invite_media_name, provider)) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.signin_password),
        enabled = !state.busy,
        imeAction = ImeAction.Done,
        onDone = submit,
    )
    BusyButton(stringResource(R.string.invite_media_join, provider), state.busy, enabled = ready) { submit() }
    TextButton(onClick = onOther, enabled = !state.busy) { Text(stringResource(R.string.invite_other_ways)) }
}

private fun SignInWay.tile(single: Boolean): Int =
    when (this) {
        SignInWay.PLEX -> if (single) R.string.signin_plex_continue else R.string.way_plex
        SignInWay.JELLYFIN -> if (single) R.string.invite_continue_jellyfin else R.string.invite_provider_jellyfin
        SignInWay.EMBY -> if (single) R.string.invite_continue_emby else R.string.invite_provider_emby
        else -> R.string.way_plex
    }
