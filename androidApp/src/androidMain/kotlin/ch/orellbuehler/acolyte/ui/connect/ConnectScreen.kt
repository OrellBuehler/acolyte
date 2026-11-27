package ch.orellbuehler.acolyte.ui.connect

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.session.SignInState
import ch.orellbuehler.acolyte.session.SignInStep
import ch.orellbuehler.acolyte.session.SignInWay
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.OpenInBrowserOnce
import ch.orellbuehler.acolyte.ui.components.PlexWaitingPanel
import ch.orellbuehler.acolyte.ui.components.signInErrorText
import ch.orellbuehler.acolyte.ui.invite.InviteScreen
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun ConnectScreen(expired: Boolean) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { ConnectViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val invite by viewModel.invite.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.restartIfDone() }
    OpenInBrowserOnce(state.plexUrl)

    val target = invite
    if (target != null) {
        InviteScreen(target, onClose = viewModel::closeInvite)
        return
    }
    BackHandler(enabled = state.step != SignInStep.SERVER && state.step != SignInStep.DONE) { viewModel.back() }

    var resetCode by rememberSaveable { mutableStateOf("") }
    AuthFrame {
        when (state.step) {
            SignInStep.SERVER, SignInStep.DONE -> ServerStep(state, expired, viewModel)
            SignInStep.CREDENTIALS -> CredentialsStep(state, viewModel)
            SignInStep.TWO_FACTOR -> TwoFactorStep(state, viewModel)
            SignInStep.SETUP -> SetupStep(state, viewModel)
            SignInStep.RESET_CODE -> ResetCodeStep(state, viewModel, resetCode) { resetCode = it }
            SignInStep.RESET_PASSWORD -> ResetPasswordStep(state, viewModel, resetCode)
        }
        state.error?.let { ErrorLine(signInErrorText(it)) }
    }
}

@Composable
internal fun ErrorLine(text: String) {
    Text(text, color = MaterialTheme.acolyte.failText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
}

@Composable
internal fun AuthFrame(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(CommonR.drawable.ic_brand_mark),
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
            )
            content()
        }
    }
}

@Composable
internal fun Heading(
    title: String,
    subtitle: String?,
) {
    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    if (subtitle != null) {
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ServerStep(
    state: SignInState,
    expired: Boolean,
    viewModel: ConnectViewModel,
) {
    var address by rememberSaveable { mutableStateOf(state.serverInput) }
    Heading(stringResource(R.string.connect_title), stringResource(if (expired) R.string.connect_expired else R.string.connect_subtitle))
    OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        label = { Text(stringResource(R.string.connect_address)) },
        placeholder = { Text(stringResource(R.string.connect_address_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { viewModel.connect(address) }),
        modifier = Modifier.fillMaxWidth(),
    )
    BusyButton(stringResource(R.string.connect_continue), state.busy, enabled = address.isNotBlank()) { viewModel.connect(address) }
    InviteEntry(viewModel)
    PrivacyLink()
}

@Composable
private fun PrivacyLink() {
    val uri = LocalUriHandler.current
    val url = stringResource(R.string.privacy_policy_url)
    TextButton(onClick = { runCatching { uri.openUri(url) } }) { Text(stringResource(R.string.privacy_policy)) }
}

@Composable
private fun InviteEntry(viewModel: ConnectViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    var link by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    if (!open) {
        TextButton(onClick = { open = true }) { Text(stringResource(R.string.invite_entry)) }
        return
    }
    val submit = { invalid = !viewModel.openInvite(link) }
    OutlinedTextField(
        value = link,
        onValueChange = {
            link = it
            invalid = false
        },
        label = { Text(stringResource(R.string.invite_link)) },
        placeholder = { Text(stringResource(R.string.invite_link_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        isError = invalid,
        supportingText = { Text(stringResource(if (invalid) R.string.invite_link_invalid else R.string.invite_link_help)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { submit() }),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = submit, enabled = link.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(stringResource(R.string.invite_open))
    }
}

@Composable
private fun CredentialsStep(
    state: SignInState,
    viewModel: ConnectViewModel,
) {
    var useKey by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(state.userName) }
    var password by rememberSaveable { mutableStateOf("") }
    var key by rememberSaveable { mutableStateOf("") }
    var chosen by rememberSaveable { mutableStateOf(SignInWay.PASSWORD) }
    val formWays = state.ways.filter { it != SignInWay.PLEX }
    val way = if (chosen in formWays) chosen else formWays.firstOrNull() ?: SignInWay.PASSWORD
    val plexWaiting = state.plexUrl != null

    Heading(stringResource(R.string.signin_title), null)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            state.serverName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        TextButton(onClick = viewModel::back, enabled = !state.busy) { Text(stringResource(R.string.connect_change_server)) }
    }
    if (plexWaiting) {
        PlexWaitingPanel(onCancel = viewModel::cancelPlex)
        return
    }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(selected = !useKey, onClick = { useKey = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
            Text(stringResource(R.string.signin_with_password), maxLines = 1)
        }
        SegmentedButton(selected = useKey, onClick = { useKey = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
            Text(stringResource(R.string.signin_with_key), maxLines = 1)
        }
    }
    if (useKey) {
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text(stringResource(R.string.signin_api_key)) },
            supportingText = { Text(stringResource(R.string.signin_api_key_hint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { viewModel.useApiKey(key) }),
            modifier = Modifier.fillMaxWidth(),
        )
        BusyButton(stringResource(R.string.signin_submit), state.busy, enabled = key.isNotBlank()) { viewModel.useApiKey(key) }
    } else {
        if (SignInWay.PLEX in state.ways) {
            BusyButton(stringResource(R.string.signin_plex_continue), state.busy, enabled = true, onClick = viewModel::startPlex)
            if (formWays.isNotEmpty()) PlexDivider()
        }
        if (formWays.isNotEmpty()) {
            PasswordForm(state, viewModel, name, password, way, formWays, { name = it }, { password = it }) {
                chosen =
                    it
            }
        }
    }
    state.hostRefused?.let { NoticeBanner(stringResource(R.string.signin_host_refused, it)) }
    if (state.ownerReset && !useKey) {
        TextButton(onClick = viewModel::startReset, enabled = !state.busy) { Text(stringResource(R.string.signin_reset_link)) }
    }
    OtherWays(state)
}

@Composable
private fun PlexDivider() {
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

@Composable
private fun PasswordForm(
    state: SignInState,
    viewModel: ConnectViewModel,
    name: String,
    password: String,
    way: SignInWay,
    formWays: List<SignInWay>,
    onName: (String) -> Unit,
    onPassword: (String) -> Unit,
    onWay: (SignInWay) -> Unit,
) {
    if (formWays.size > 1) {
        var open by rememberSaveable { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(way.label()), maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = stringResource(R.string.signin_account))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                formWays.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.label())) },
                        onClick = {
                            onWay(option)
                            open = false
                        },
                    )
                }
            }
        }
    }
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        label = { Text(stringResource(R.string.signin_name)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPassword,
        label = { Text(stringResource(R.string.signin_password)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { viewModel.signIn(name, password, way) }),
        modifier = Modifier.fillMaxWidth(),
    )
    BusyButton(stringResource(R.string.signin_submit), state.busy, enabled = name.isNotBlank() && password.isNotEmpty()) {
        viewModel.signIn(name, password, way)
    }
}

@Composable
private fun OtherWays(state: SignInState) {
    if (state.otherWays.isEmpty()) return
    val names = state.otherWays.map { stringResource(it.label()) }.joinToString()
    Text(
        stringResource(R.string.signin_other_ways, names),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun TwoFactorStep(
    state: SignInState,
    viewModel: ConnectViewModel,
) {
    var code by rememberSaveable { mutableStateOf("") }
    Heading(stringResource(R.string.twofactor_title), stringResource(R.string.twofactor_message))
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.take(16) },
        label = { Text(stringResource(R.string.twofactor_code)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { viewModel.verify(code) }),
        modifier = Modifier.fillMaxWidth(),
    )
    BusyButton(stringResource(R.string.twofactor_submit), state.busy, enabled = code.isNotBlank()) { viewModel.verify(code) }
    TextButton(onClick = viewModel::back, enabled = !state.busy) { Text(stringResource(R.string.common_back)) }
}

@Composable
internal fun BusyButton(
    text: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(text, maxLines = 1)
        }
    }
}

private fun SignInWay.label(): Int =
    when (this) {
        SignInWay.PASSWORD -> R.string.signin_way_password
        SignInWay.JELLYFIN -> R.string.signin_way_jellyfin
        SignInWay.EMBY -> R.string.signin_way_emby
        SignInWay.PLEX -> R.string.way_plex
        SignInWay.OIDC -> R.string.way_oidc
        SignInWay.FORWARD_AUTH -> R.string.way_forward_auth
    }
