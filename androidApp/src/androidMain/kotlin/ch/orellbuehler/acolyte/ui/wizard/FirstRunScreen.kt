package ch.orellbuehler.acolyte.ui.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.account.AccountSecurityProblem
import ch.orellbuehler.acolyte.feature.account.AccountWizardStep
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.OpenInBrowserOnce
import ch.orellbuehler.acolyte.ui.components.PasswordField
import ch.orellbuehler.acolyte.ui.components.PlexWaitingPanel
import ch.orellbuehler.acolyte.ui.components.ReauthSheet
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun FirstRunScreen() {
    val sdk = LocalSdk.current
    val viewModel = viewModel { FirstRunViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val security by viewModel.securityState.collectAsStateWithLifecycle()
    OpenInBrowserOnce(security.plexUrl)
    BackHandler(enabled = state.step == AccountWizardStep.DONE) { viewModel.back() }
    ReauthSheet(viewModel.wizardReauth)
    ReauthSheet(viewModel.securityReauth)

    val onServers = state.step == AccountWizardStep.SERVERS
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.XLarge)
            .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.wizard_step, if (onServers) 1 else 2, 2),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (onServers) {
                TextButton(onClick = viewModel::finish, enabled = !state.busy) { Text(stringResource(R.string.wizard_skip_setup)) }
            }
        }
        if (onServers) {
            ServersStep(viewModel)
        } else {
            DoneStep(viewModel)
        }
        state.error?.let { error ->
            if (onServers) NoticeBanner(wizardErrorText(error)) else ErrorBanner(error)
        }
    }
}

@Composable
private fun ServersStep(viewModel: FirstRunViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val security by viewModel.securityState.collectAsStateWithLifecycle()
    var formKind by rememberSaveable { mutableStateOf<String?>(null) }
    val secReady = security.status.phase == Phase.READY

    fun linkedName(kind: String): String? {
        val fromSecurity =
            security.media.links
                .firstOrNull { it.provider.equals(kind, ignoreCase = true) }
                ?.name
        val fromWizard = state.linked.firstOrNull { it.provider.equals(kind, ignoreCase = true) }?.name
        return if (secReady) fromSecurity else fromSecurity ?: fromWizard
    }
    val plexName = security.plex.takeIf { it.linked }?.name
    val anyLinked = plexName != null || linkedName("jellyfin") != null || linkedName("emby") != null
    val busy = state.busy || security.busy

    Text(stringResource(R.string.wizard_servers_title), style = MaterialTheme.typography.headlineMedium)
    Text(
        stringResource(R.string.wizard_servers_lead),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val kind = formKind
    if (kind != null) {
        ServerForm(kind, viewModel, onClose = { formKind = null })
        return
    }
    if (security.plexWaiting) {
        PlexWaitingPanel(onCancel = viewModel::cancelPlex)
    } else {
        ServerRow(
            title = stringResource(R.string.way_plex),
            linked = plexName,
            busy = busy,
            onConnect = viewModel::linkPlex,
            onUnlink = viewModel::unlinkPlex,
        )
    }
    ServerRow(
        title = stringResource(R.string.invite_provider_jellyfin),
        linked = linkedName("jellyfin"),
        busy = busy,
        onConnect = { formKind = "jellyfin" },
        onUnlink = { viewModel.unlinkMedia("jellyfin") },
    )
    ServerRow(
        title = stringResource(R.string.invite_provider_emby),
        linked = linkedName("emby"),
        busy = busy,
        onConnect = { formKind = "emby" },
        onUnlink = { viewModel.unlinkMedia("emby") },
    )
    if (security.problem == AccountSecurityProblem.PLEX_TIMEOUT) NoticeBanner(stringResource(CommonR.string.signin_error_plex_timeout))
    security.error?.let { ErrorBanner(it) }
    Button(onClick = viewModel::next, enabled = !busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(stringResource(if (anyLinked) R.string.wizard_continue else R.string.wizard_skip_servers))
    }
}

@Composable
private fun ServerRow(
    title: String,
    linked: String?,
    busy: Boolean,
    onConnect: () -> Unit,
    onUnlink: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (linked !=
                        null
                    ) {
                        stringResource(R.string.wizard_linked_as, linked)
                    } else {
                        stringResource(R.string.wizard_connect_hint, title)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (linked != null) {
                OutlinedButton(onClick = onUnlink, enabled = !busy) { Text(stringResource(R.string.wizard_unlink)) }
            } else {
                Button(onClick = onConnect, enabled = !busy) { Text(stringResource(R.string.wizard_connect)) }
            }
        }
    }
}

@Composable
private fun ServerForm(
    kind: String,
    viewModel: FirstRunViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var address by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(kind) { viewModel.discover() }
    val title = stringResource(if (kind == "emby") R.string.invite_provider_emby else R.string.invite_provider_jellyfin)
    val ready = address.isNotBlank() && name.isNotBlank() && password.isNotEmpty()
    val submit = { if (ready && !state.busy) viewModel.linkServer(kind, address, name, password, onClose) }

    Text(stringResource(R.string.wizard_form_lead, title), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        label = { Text(stringResource(R.string.wizard_address)) },
        placeholder = { Text(stringResource(R.string.wizard_address_hint), maxLines = 1) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
    val found = state.found.filter { it.kind.equals(kind, ignoreCase = true) }
    if (state.discovering) {
        Text(
            stringResource(R.string.wizard_discovering),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (found.isNotEmpty()) {
        Text(
            stringResource(R.string.wizard_found),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        found.forEach { server ->
            AssistChip(
                onClick = { address = server.address },
                label = { Text(server.name.ifBlank { server.address } + " · " + server.address, maxLines = 1) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(128) },
        label = { Text(stringResource(R.string.signin_name)) },
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
    Button(onClick = { submit() }, enabled = ready && !state.busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(stringResource(R.string.wizard_sign_in_connect))
    }
    TextButton(onClick = onClose, enabled = !state.busy) { Text(stringResource(R.string.common_back)) }
}

@Composable
private fun DoneStep(viewModel: FirstRunViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Text(stringResource(R.string.wizard_done_title), style = MaterialTheme.typography.headlineMedium)
    Text(
        stringResource(R.string.wizard_done_lead),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = viewModel::finish, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(stringResource(R.string.wizard_open_home))
    }
    OutlinedButton(onClick = viewModel::finish, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(stringResource(R.string.wizard_skip_setup))
    }
    TextButton(onClick = viewModel::back, enabled = !state.busy) { Text(stringResource(R.string.common_back)) }
}
