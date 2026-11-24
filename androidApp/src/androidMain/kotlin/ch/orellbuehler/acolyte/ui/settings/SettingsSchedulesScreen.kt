package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsDot
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsEditor
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsLoopRow
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsNextKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsReason
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsRun
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsScheduleRow
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsSchedulesState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.nav.SettingsPartsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSearchRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSourcesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSubtitlesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsThisAppRoute
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.settings.controls.SettingsSchedulesViewModel
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsError
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsEvery
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsGroup
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsNext
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsReason
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsRun
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsScheduleNotice
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsStale
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsUnit
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun SettingsSchedulesScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsSchedulesViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    val noticeText = state.notice?.let { settingsControlsScheduleNotice(it) }
    LaunchedEffect(state.notice?.id) { noticeText?.let { snackbar.showSnackbar(it) } }
    ScreenScaffold(title = stringResource(R.string.settings_page_schedules), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                SchedulesList(state, viewModel, onOpen)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun pageRoute(key: String): Any? =
    when (key) {
        "sources" -> SettingsSourcesRoute
        "requests" -> Routes.settingsRequests
        "search" -> SettingsSearchRoute
        "subtitles" -> SettingsSubtitlesRoute
        "this_app" -> SettingsThisAppRoute
        else -> null
    }

@Composable
private fun SchedulesList(
    state: SettingsControlsSchedulesState,
    viewModel: SettingsSchedulesViewModel,
    onOpen: (Any) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        if (state.safeMode) item(key = "safe") { NoticeBanner(stringResource(R.string.settings_controls_sched_safe_mode)) }
        if (state.running.isNotEmpty()) {
            item(key = "running") {
                SettingsCard(stringResource(R.string.settings_controls_sched_running_now)) {
                    state.running.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            Dot(SettingsControlsDot.BUSY)
                            Text(row.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(
                                    if (row.running) R.string.settings_controls_next_running else R.string.settings_controls_next_queued,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.acolyte.waitText,
                            )
                        }
                    }
                }
            }
        }
        state.groups.forEach { group ->
            item(key = "group-${group.group.name}") {
                SettingsCard(settingsControlsGroup(group.group)) {
                    group.rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider()
                        ScheduleRow(row, state, viewModel, onOpen)
                    }
                }
            }
        }
        if (state.loops.isNotEmpty()) {
            item(key = "loops") {
                SettingsCard(stringResource(R.string.settings_controls_sched_stages)) {
                    Text(
                        stringResource(R.string.settings_controls_sched_stages_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { onOpen(SettingsPartsRoute) },
                    ) { Text(stringResource(R.string.settings_controls_sched_open_parts)) }
                    state.loops.forEachIndexed { index, loop ->
                        if (index > 0) HorizontalDivider()
                        LoopRow(loop)
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(dot: SettingsControlsDot) {
    val tone =
        when (dot) {
            SettingsControlsDot.OK -> Tone.OK
            SettingsControlsDot.BUSY -> Tone.WAIT
            SettingsControlsDot.FAIL -> Tone.FAIL
            SettingsControlsDot.OFF -> Tone.IDLE
        }
    Box(Modifier.size(10.dp).background(MaterialTheme.acolyte.dot(tone), CircleShape))
}

@Composable
private fun ScheduleRow(
    row: SettingsControlsScheduleRow,
    state: SettingsControlsSchedulesState,
    viewModel: SettingsSchedulesViewModel,
    onOpen: (Any) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Box(Modifier.padding(top = 6.dp)) { Dot(row.dot) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(row.label, style = MaterialTheme.typography.titleSmall)
                if (row.about.isNotBlank()) Hint(row.about)
            }
        }
        Column(Modifier.padding(start = 22.dp), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Hint(stringResource(R.string.settings_controls_sched_how_often) + ": " + settingsControlsEvery(row.every))
            val ago = relativeTime(row.lastAt)
            val last =
                when {
                    ago == null -> stringResource(R.string.settings_controls_sched_not_yet)
                    row.took.isNotEmpty() -> ago + " · " + stringResource(R.string.settings_controls_sched_took, row.took)
                    else -> ago
                }
            Hint(stringResource(R.string.settings_controls_sched_last_run) + ": " + last)
            Hint(stringResource(R.string.settings_controls_sched_next_run) + ": " + settingsControlsNext(row.next))
            if (row.failed) {
                FailText(
                    if (row.failedError.isNotEmpty()) {
                        stringResource(R.string.settings_controls_sched_failed_with, row.failedError)
                    } else {
                        stringResource(R.string.settings_controls_sched_failed)
                    },
                )
            }
            if (!row.failed && row.reason != SettingsControlsReason.NONE) ReasonLine(row, onOpen)
            settingsControlsStale(row.stale)?.let { Hint(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                if (row.canRun) {
                    Button(onClick = { viewModel.runNow(row.kind) }, enabled = !row.spinning && state.busyKind.isEmpty()) {
                        if (row.spinning) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            stringResource(R.string.settings_controls_sched_run_now),
                            modifier = Modifier.padding(start = if (row.spinning) 8.dp else 0.dp),
                        )
                    }
                }
                if (row.canChange && !row.editing) {
                    OutlinedButton(onClick = { viewModel.edit(row.kind) }) { Text(stringResource(R.string.settings_controls_sched_change)) }
                }
            }
            val editor = state.editor
            if (row.editing && editor != null && editor.kind == row.kind) EditorCard(editor, viewModel, onOpen)
        }
    }
}

@Composable
private fun ReasonLine(
    row: SettingsControlsScheduleRow,
    onOpen: (Any) -> Unit,
) {
    val route = if (row.reasonLinks) pageRoute(row.pageKey) else null
    Text(
        settingsControlsReason(row.reason),
        style = MaterialTheme.typography.bodySmall,
        color = if (route != null) MaterialTheme.acolyte.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = if (route != null) Modifier.clickable { onOpen(route) } else Modifier,
    )
}

@Composable
private fun EditorCard(
    editor: SettingsControlsEditor,
    viewModel: SettingsSchedulesViewModel,
    onOpen: (Any) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            val unit = settingsControlsUnit(editor.unit)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                IconButton(onClick = { viewModel.stepAmount(false) }) {
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.settings_controls_sched_less))
                }
                OutlinedTextField(
                    value = editor.amount,
                    onValueChange = viewModel::setAmount,
                    label = { Text(stringResource(R.string.settings_controls_sched_every_field, unit)) },
                    isError = !editor.valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.stepAmount(true) }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.settings_controls_sched_more))
                }
            }
            Hint(stringResource(R.string.settings_controls_sched_range, editor.minAmount, editor.maxAmount, unit))
            if (editor.toggleable) AutoRow(editor, viewModel)
            editor.error?.let { FailText(settingsControlsError(it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                pageRoute(editor.pageKey)?.let { route ->
                    TextButton(onClick = { onOpen(route) }) { Text(stringResource(R.string.settings_controls_sched_settings)) }
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::closeEditor) { Text(stringResource(R.string.common_cancel)) }
                    Button(onClick = viewModel::saveEditor, enabled = editor.valid && !editor.saving) {
                        Text(stringResource(R.string.settings_controls_sched_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoRow(
    editor: SettingsControlsEditor,
    viewModel: SettingsSchedulesViewModel,
) {
    val refresh = editor.kind == "refresh_titles"
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(
            stringResource(if (refresh) R.string.settings_controls_sched_auto_refresh else R.string.settings_controls_sched_auto_rescan),
            style = MaterialTheme.typography.titleSmall,
        )
        Hint(
            stringResource(
                when {
                    refresh && editor.enabled -> R.string.settings_controls_sched_refresh_on
                    refresh -> R.string.settings_controls_sched_refresh_off
                    editor.enabled -> R.string.settings_controls_sched_rescan_on
                    else -> R.string.settings_controls_sched_rescan_off
                },
            ),
        )
        val label =
            stringResource(
                when {
                    refresh && editor.enabled -> R.string.settings_controls_sched_refresh_turn_off
                    refresh -> R.string.settings_controls_sched_refresh_turn_on
                    editor.enabled -> R.string.settings_controls_sched_rescan_turn_off
                    else -> R.string.settings_controls_sched_rescan_turn_on
                },
            )
        if (editor.enabled) {
            Button(
                onClick = { viewModel.setEnabled(editor.kind, false) },
                enabled = !editor.busy,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) { Text(label) }
        } else {
            OutlinedButton(onClick = { viewModel.setEnabled(editor.kind, true) }, enabled = !editor.busy) { Text(label) }
        }
    }
}

@Composable
private fun LoopRow(loop: SettingsControlsLoopRow) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Dot(loop.dot)
            Text(loop.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
            if (loop.run != SettingsControlsRun.RUNNING) StatusChip(settingsControlsRun(loop.run), loop.tone)
        }
        if (loop.reason.isNotBlank()) Hint(loop.reason)
        Hint(stringResource(R.string.settings_controls_sched_how_often) + ": " + settingsControlsEvery(loop.every))
        relativeTime(loop.lastAt)?.let { Hint(stringResource(R.string.settings_controls_sched_last_run) + ": " + it) }
        if (loop.next.kind != SettingsControlsNextKind.NONE) {
            Hint(stringResource(R.string.settings_controls_sched_next_run) + ": " + settingsControlsNext(loop.next))
        }
    }
}
