package ch.orellbuehler.acolyte.ui.settings.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertRow
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertsNotice
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertsNoticeKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertsState
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerMissing
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerPackState
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerTestKind
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsAlertsContent(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsAlertsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var removing by rememberSaveable { mutableStateOf<Long?>(null) }
    val notice = state.notice
    val noticeText = notice?.let { alertsNoticeText(it) }
    LaunchedEffect(notice?.seq) {
        if (notice != null && noticeText != null) {
            snackbar.showSnackbar(noticeText)
            viewModel.dismissNotice(notice.seq)
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.settings_page_alerts),
        onBack = onBack,
        actions = {
            if (state.allowed && state.alerts.isNotEmpty() && state.form == null) {
                IconButton(onClick = viewModel::startAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.settings_server_alerts_add))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    AlertsList(state, viewModel, onRemove = { removing = it })
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    state.form?.let { AlertFormSheet(it, viewModel) }
    removing?.let { id ->
        val alert = state.alerts.firstOrNull { it.id == id }
        ConfirmDialog(
            title = stringResource(R.string.settings_server_alert_remove_title, alert?.name.orEmpty()),
            text = null,
            confirm = stringResource(R.string.settings_server_alert_remove),
            dismiss = stringResource(R.string.common_cancel),
            danger = true,
            onConfirm = {
                removing = null
                viewModel.remove(id)
            },
            onDismiss = { removing = null },
        )
    }
}

@Composable
private fun alertsNoticeText(notice: SettingsServerAlertsNotice): String =
    when (notice.kind) {
        SettingsServerAlertsNoticeKind.SAVED -> stringResource(R.string.settings_server_alert_saved, notice.name)
        SettingsServerAlertsNoticeKind.REMOVE_FAILED ->
            noticeWithError(stringResource(R.string.settings_server_alert_remove_failed), notice.error)
        SettingsServerAlertsNoticeKind.PACK_SAVED ->
            stringResource(
                R.string.settings_server_pack_saved,
                stringResource(if (notice.name == "one") R.string.settings_server_pack_per_pack else R.string.settings_server_pack_each),
            )
        SettingsServerAlertsNoticeKind.PACK_FAILED -> noticeWithError(stringResource(R.string.settings_server_pack_failed), notice.error)
    }

@Composable
private fun AlertsList(
    state: SettingsServerAlertsState,
    viewModel: SettingsAlertsViewModel,
    onRemove: (Long) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        item(key = "lead") { Hint(stringResource(R.string.settings_server_alerts_lead), Modifier.padding(horizontal = Spacing.Screen)) }
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        if (state.alerts.isEmpty()) {
            item(key = "empty") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    EmptyView(
                        title = stringResource(R.string.settings_server_alerts_empty),
                        message = stringResource(R.string.settings_server_alerts_empty_text),
                        icon = Icons.Rounded.NotificationsNone,
                        modifier = Modifier.padding(vertical = Spacing.Large),
                    )
                    Button(onClick = viewModel::startAdd) { Text(stringResource(R.string.settings_server_alerts_add)) }
                }
            }
        }
        items(state.alerts, key = { it.id }) { alert ->
            AlertCard(
                alert,
                onTest = { viewModel.test(alert.id) },
                onChange = { viewModel.startChange(alert.id) },
                onRemove = { onRemove(alert.id) },
            )
        }
        if (state.showPack) {
            item(key = "pack") { PackCard(state.pack, onPick = viewModel::setPack, onRetry = viewModel::retryPack) }
        }
    }
}

@Composable
private fun AlertCard(
    alert: SettingsServerAlertRow,
    onTest: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(alert.name, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
            ) {
                AssistChip(onClick = {}, label = { Text(kindLabel(alert.kind)) })
                if (!alert.enabled) AssistChip(onClick = {}, label = { Text(stringResource(R.string.settings_server_alert_off)) })
                if (alert.test?.kind == SettingsServerTestKind.SENT) {
                    StatusChip(stringResource(R.string.settings_server_test_sent, alert.name), Tone.OK)
                }
            }
            if (alert.missing != SettingsServerMissing.NOTHING) {
                Text(
                    stringResource(R.string.settings_server_alert_needs, missingWords(alert.missing)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Hint(momentsLine(alert.moments))
            AlertResult(alert)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onTest, enabled = !alert.testing) {
                    if (alert.testing) {
                        CircularProgressIndicator(Modifier.padding(end = Spacing.Small).size(16.dp), strokeWidth = 2.dp)
                    }
                    Text(stringResource(R.string.settings_server_alert_test))
                }
                OutlinedButton(onClick = onChange, enabled = alert.editable && !alert.testing) {
                    Text(stringResource(R.string.settings_server_alert_change))
                }
                val removeLabel = stringResource(R.string.settings_server_alert_remove_name, alert.name)
                TextButton(
                    onClick = onRemove,
                    enabled = !alert.testing,
                    modifier = Modifier.semantics { contentDescription = removeLabel },
                ) { Text(stringResource(R.string.settings_server_alert_remove)) }
            }
        }
    }
}

@Composable
private fun AlertResult(alert: SettingsServerAlertRow) {
    val test = alert.test
    val testError = test?.error
    when {
        test?.kind == SettingsServerTestKind.TYPE_FIRST -> FailText(stringResource(R.string.settings_server_test_type_first))
        test?.kind == SettingsServerTestKind.FAILED && testError != null -> ServerErrorText(testError)
        alert.lastError.isNotEmpty() -> FailText(stringResource(R.string.settings_server_alert_last_failed, alert.lastError))
        alert.lastSentAt.isNotEmpty() -> {
            val ago = relativeText(alert.lastSentAt)
            if (ago != null) Hint(stringResource(R.string.settings_server_alert_last_sent, ago))
        }
    }
}

@Composable
private fun PackCard(
    pack: SettingsServerPackState,
    onPick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_server_packs), subtitle = stringResource(R.string.settings_server_packs_note)) {
        when {
            pack.loading -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            pack.error != null -> {
                ServerErrorText(pack.error!!)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.settings_server_pack_retry)) }
            }
            else ->
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    PackChoice(
                        "one",
                        pack,
                        stringResource(R.string.settings_server_pack_one),
                        stringResource(R.string.settings_server_pack_one_hint),
                        onPick,
                    )
                    PackChoice("each", pack, stringResource(R.string.settings_server_pack_each), null, onPick)
                }
        }
    }
}

@Composable
private fun PackChoice(
    style: String,
    pack: SettingsServerPackState,
    title: String,
    hint: String?,
    onPick: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = pack.style == style, enabled = !pack.saving, role = Role.RadioButton, onClick = { onPick(style) }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        RadioButton(selected = pack.style == style, onClick = null, enabled = !pack.saving)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Hint(hint)
        }
    }
}
