package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsDangerState
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsStopped
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsWait
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.settings.controls.SettingsDangerViewModel
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsLabel
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsNotice
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsYes
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsDangerScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsDangerViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stopped = state.stopped
    LaunchedEffect(stopped != null) { if (stopped != null) viewModel.waitForServer() }
    val snackbar = remember { SnackbarHostState() }
    val noticeText = state.notice?.let { settingsControlsNotice(it) }
    LaunchedEffect(state.notice?.id) { noticeText?.let { snackbar.showSnackbar(it) } }
    ScreenScaffold(title = stringResource(R.string.settings_page_danger), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.allowed -> MessageView(icon = Icons.Rounded.Lock, title = stringResource(R.string.settings_controls_owner_only))
                stopped != null -> StoppedView(stopped, viewModel)
                else -> DangerCard(state, viewModel)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    state.ask?.let { ask ->
        ConfirmDialog(
            title = stringResource(R.string.settings_controls_ask_title, settingsControlsLabel(ask.kind, ask.name)),
            text = ask.text + "\n\n" + stringResource(R.string.settings_controls_expires, ask.expiresInSeconds),
            confirm = settingsControlsYes(ask.kind, ask.name),
            dismiss = stringResource(R.string.common_cancel),
            onConfirm = viewModel::confirm,
            onDismiss = viewModel::cancel,
            danger = true,
        )
    }
    SettingsReauthDialog(state.reauth, viewModel::confirmReauth, viewModel::cancelReauth)
}

@Composable
private fun DangerCard(
    state: SettingsControlsDangerState,
    viewModel: SettingsDangerViewModel,
) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = Spacing.XLarge)) {
        SettingsCard(stringResource(R.string.settings_controls_whole_app)) {
            Text(
                stringResource(R.string.settings_controls_danger_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DangerButton(SettingsControlsKind.APP_RESTART, state, viewModel, destructive = false)
            DangerButton(SettingsControlsKind.APP_SAFE_MODE, state, viewModel, destructive = false)
            DangerButton(SettingsControlsKind.APP_SHUTDOWN, state, viewModel, destructive = true)
            Hint(stringResource(R.string.settings_controls_danger_safe_note))
        }
    }
}

@Composable
private fun DangerButton(
    kind: SettingsControlsKind,
    state: SettingsControlsDangerState,
    viewModel: SettingsDangerViewModel,
    destructive: Boolean,
) {
    val label = settingsControlsLabel(kind, "")
    val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            if (state.busyKind == kind) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(label)
        }
    }
    if (destructive) {
        Button(
            onClick = { viewModel.request(kind) },
            enabled = !state.busy,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) { content() }
    } else {
        OutlinedButton(onClick = { viewModel.request(kind) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun StoppedView(
    stopped: SettingsControlsStopped,
    viewModel: SettingsDangerViewModel,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stopped.text.ifBlank { stringResource(R.string.settings_controls_done_stopping) },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (stopped.wait != SettingsControlsWait.BACK) CircularProgressIndicator()
        Text(
            stringResource(
                when (stopped.wait) {
                    SettingsControlsWait.STOPPING -> R.string.settings_controls_wait_stopping
                    SettingsControlsWait.WAITING -> R.string.settings_controls_wait_waiting
                    SettingsControlsWait.BACK -> R.string.settings_controls_wait_back
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (stopped.kind == SettingsControlsKind.APP_SHUTDOWN && stopped.wait != SettingsControlsWait.BACK) {
            Hint(stringResource(R.string.settings_controls_wait_shutdown), Modifier.fillMaxWidth())
        }
        if (stopped.wait == SettingsControlsWait.BACK) {
            Button(onClick = viewModel::dismissStopped) { Text(stringResource(R.string.settings_controls_done)) }
        } else {
            TextButton(onClick = viewModel::dismissStopped) { Text(stringResource(R.string.settings_controls_stop_waiting)) }
        }
    }
}
