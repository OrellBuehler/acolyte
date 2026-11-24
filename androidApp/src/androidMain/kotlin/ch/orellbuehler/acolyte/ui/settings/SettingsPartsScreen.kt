package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsEventRow
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsLinkKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsLinkRow
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsPartRow
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsPartsState
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsRun
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsStageAction
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsStageRow
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ReauthSheet
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.SettingsDangerRoute
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.settings.controls.SettingsPartsViewModel
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsLabel
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsLinkKind
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsNotice
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsPartName
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsPartNeeds
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsPartStops
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsRun
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsYes
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsPartsScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsPartsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    val noticeText = state.notice?.let { settingsControlsNotice(it) }
    LaunchedEffect(state.notice?.id) { noticeText?.let { snackbar.showSnackbar(it) } }
    ScreenScaffold(title = stringResource(R.string.settings_page_parts), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                MessageView(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.settings_controls_owner_only),
                )
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    PartsList(state, viewModel, onOpen)
                }
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
    ReauthSheet(viewModel.reauth)
}

@Composable
private fun PartsList(
    state: SettingsControlsPartsState,
    viewModel: SettingsPartsViewModel,
    onOpen: (Any) -> Unit,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        if (state.safeMode) item(key = "safe") { SafeModeCard { onOpen(SettingsDangerRoute) } }
        item(key = "parts") { PartsCard(state, viewModel) }
        item(key = "advanced") {
            TextButton(onClick = { advanced = !advanced }, modifier = Modifier.padding(horizontal = Spacing.Small)) {
                Text(stringResource(if (advanced) R.string.settings_controls_advanced_hide else R.string.settings_controls_advanced))
            }
        }
        if (advanced) {
            item(key = "stages") { StagesCard(state, viewModel) }
            item(key = "links") { LinksCard(state, viewModel) }
            item(key = "events") { EventsCard(state.events) }
        }
        item(key = "whole") { WholeAppCard { onOpen(SettingsDangerRoute) } }
    }
}

@Composable
private fun SafeModeCard(onOpen: () -> Unit) {
    SettingsCard(stringResource(R.string.settings_controls_safe_mode_title)) {
        Text(stringResource(R.string.settings_controls_safe_mode_text), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onOpen) { Text(stringResource(R.string.settings_controls_open_danger)) }
    }
}

@Composable
private fun PartsCard(
    state: SettingsControlsPartsState,
    viewModel: SettingsPartsViewModel,
) {
    SettingsCard(
        stringResource(R.string.settings_controls_parts_title),
        subtitle = stringResource(R.string.settings_controls_parts_desc),
    ) {
        state.parts.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider()
            PartRow(row, state.busy, state.busyKey == row.key, viewModel)
        }
    }
}

@Composable
private fun PartRow(
    row: SettingsControlsPartRow,
    busy: Boolean,
    working: Boolean,
    viewModel: SettingsPartsViewModel,
) {
    val name = settingsControlsPartName(row.part, row.name)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
            if (row.run != SettingsControlsRun.RUNNING) StatusChip(settingsControlsRun(row.run), row.tone)
        }
        settingsControlsPartStops(row.part)?.let { Hint(stringResource(R.string.settings_controls_when_off, it)) }
        settingsControlsPartNeeds(row.part)?.let { Hint(it) }
        if (row.reason.isNotBlank()) Hint(row.reason)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            if (row.on) {
                OutlinedButton(onClick = { viewModel.restart(row.part) }, enabled = !busy) {
                    Text(stringResource(R.string.settings_controls_restart))
                }
                OutlinedButton(onClick = { viewModel.turnOff(row.part) }, enabled = !busy) {
                    Text(stringResource(R.string.settings_controls_turn_off))
                }
            } else {
                Button(onClick = { viewModel.turnOn(row.part) }, enabled = !busy) {
                    Text(stringResource(R.string.settings_controls_turn_on))
                }
            }
            if (working) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun StagesCard(
    state: SettingsControlsPartsState,
    viewModel: SettingsPartsViewModel,
) {
    SettingsCard(
        stringResource(R.string.settings_controls_stages_title),
        subtitle = stringResource(R.string.settings_controls_stages_desc),
    ) {
        if (state.stages.isEmpty()) {
            Hint(stringResource(R.string.settings_controls_stages_empty))
        }
        state.stages.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider()
            StageRow(row, state.busy, state.busyKey == row.key, viewModel)
        }
    }
}

@Composable
private fun StageRow(
    row: SettingsControlsStageRow,
    busy: Boolean,
    working: Boolean,
    viewModel: SettingsPartsViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(row.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
            if (row.run != SettingsControlsRun.RUNNING) StatusChip(settingsControlsRun(row.run), row.tone)
        }
        if (row.reason.isNotBlank()) Hint(row.reason)
        if (row.pausable) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                if (row.paused) {
                    OutlinedButton(onClick = { viewModel.stage(row.stage, SettingsControlsStageAction.RESUME) }, enabled = !busy) {
                        Text(stringResource(R.string.settings_controls_resume))
                    }
                } else {
                    OutlinedButton(onClick = { viewModel.stage(row.stage, SettingsControlsStageAction.PAUSE) }, enabled = !busy) {
                        Text(stringResource(R.string.settings_controls_pause))
                    }
                }
                OutlinedButton(onClick = { viewModel.stage(row.stage, SettingsControlsStageAction.RESTART) }, enabled = !busy) {
                    Text(stringResource(R.string.settings_controls_restart))
                }
                if (working) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun LinksCard(
    state: SettingsControlsPartsState,
    viewModel: SettingsPartsViewModel,
) {
    SettingsCard(stringResource(R.string.settings_controls_links_title), subtitle = stringResource(R.string.settings_controls_links_desc)) {
        if (state.links.isEmpty()) {
            Hint(stringResource(R.string.settings_controls_links_empty))
        }
        state.links.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider()
            LinkRow(row, state.busy, state.busyKey == row.key, viewModel)
        }
    }
}

@Composable
private fun LinkRow(
    row: SettingsControlsLinkRow,
    busy: Boolean,
    working: Boolean,
    viewModel: SettingsPartsViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(row.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
            if (row.run != SettingsControlsRun.RUNNING) StatusChip(settingsControlsRun(row.run), row.tone)
        }
        Hint(settingsControlsLinkKind(row.kind))
        if (row.reason.isNotBlank()) Hint(row.reason)
        if (row.canTest) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { viewModel.reconnect(row.key) }, enabled = !busy) {
                    Text(
                        stringResource(
                            if (row.kind == SettingsControlsLinkKind.DOWNLOADER) {
                                R.string.settings_controls_reconnect
                            } else {
                                R.string.settings_controls_test_now
                            },
                        ),
                    )
                }
                if (working) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun EventsCard(events: List<SettingsControlsEventRow>) {
    SettingsCard(stringResource(R.string.settings_controls_events_title)) {
        if (events.isEmpty()) {
            Hint(stringResource(R.string.settings_controls_events_empty))
        }
        events.forEachIndexed { index, event ->
            if (index > 0) HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(event.summary, style = MaterialTheme.typography.bodyMedium)
                relativeTime(event.at)?.let { Hint(it) }
            }
        }
    }
}

@Composable
private fun WholeAppCard(onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen).clickable(onClick = onOpen),
    ) {
        Row(
            Modifier.padding(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(stringResource(R.string.settings_controls_whole_app), style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(R.string.settings_controls_whole_app_note))
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}
