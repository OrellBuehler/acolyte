package ch.orellbuehler.acolyte.ui.settings.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.server.SETTINGS_LOGS_QUERY_MAX
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerLogLineRow
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerLogsFilter
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerLogsState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.settings.SettingsReauthDialog
import ch.orellbuehler.acolyte.ui.settings.shareTransferredFile
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun SettingsLogsContent(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val context = LocalContext.current
    val viewModel = viewModel { SettingsLogsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    val file = state.file
    LaunchedEffect(file?.seq) {
        if (file != null) {
            shareTransferredFile(context, file.toTransferred(), "text/plain")
            viewModel.fileDelivered()
        }
    }
    val downloadError = state.downloadError
    val downloadText = downloadError?.let { noticeWithError(stringResource(R.string.settings_server_logs_download_failed), it) }
    LaunchedEffect(downloadError) {
        if (downloadText != null) {
            snackbar.showSnackbar(downloadText)
            viewModel.dismissDownloadError()
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.settings_page_logs),
        onBack = onBack,
        actions = {
            if (state.allowed && !state.noLogFile) {
                IconButton(onClick = { filterOpen = true }) {
                    Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.settings_server_logs_filter_button))
                }
                IconButton(onClick = viewModel::download, enabled = !state.downloading) {
                    if (state.downloading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.settings_server_logs_download))
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.allowed -> NotAllowed()
                state.noLogFile ->
                    EmptyView(
                        title = stringResource(R.string.settings_server_logs_no_file),
                        message = stringResource(R.string.settings_server_logs_no_file_text),
                        icon = Icons.Rounded.Description,
                    )
                else -> StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) { LogsList(state, viewModel) }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (filterOpen) {
        FilterSheet(state.filter, onApply = { level, query, from, to ->
            filterOpen = false
            viewModel.applyFilter(level, query, from, to)
        }, onClear = {
            filterOpen = false
            viewModel.clear()
        }, onClose = { filterOpen = false })
    }
    SettingsReauthDialog(state.reauth, onConfirm = viewModel::confirmReauth, onCancel = viewModel::cancelReauth)
}

@Composable
private fun levelLabel(level: String): String =
    stringResource(
        when (level) {
            "error" -> R.string.settings_server_logs_error
            "warning" -> R.string.settings_server_logs_warning
            else -> R.string.settings_server_logs_info
        },
    )

@Composable
private fun LogsList(
    state: SettingsServerLogsState,
    viewModel: SettingsLogsViewModel,
) {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        if (state.busy) item(key = "busy") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.status.error?.let { error -> item(key = "error") { NoticeBanner(serverErrorLine(error)) } }
        if (state.filter.active) item(key = "filter") { ActiveFilter(state.filter, onClear = viewModel::clear) }
        item(key = "range") {
            Hint(
                if (state.noMatch) {
                    stringResource(R.string.settings_server_logs_no_match)
                } else {
                    stringResource(R.string.settings_server_logs_range, state.rangeFrom, state.rangeTo, state.total)
                },
                Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
            )
        }
        itemsIndexed(state.lines, key = { index, _ -> state.offset + index }) { _, line ->
            LogLine(line, dateTimeText(context, line.at))
        }
        if (state.lines.isNotEmpty()) {
            item(key = "pager") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    OutlinedButton(onClick = viewModel::newer, enabled = state.hasNewer && !state.busy) {
                        Text(stringResource(R.string.settings_server_logs_newer))
                    }
                    OutlinedButton(onClick = viewModel::older, enabled = state.hasOlder && !state.busy) {
                        Text(stringResource(R.string.settings_server_logs_older))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilter(
    filter: SettingsServerLogsFilter,
    onClear: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (filter.level.isNotEmpty()) {
                AssistChip(onClick = {}, label = { Text(levelFilterLabel(filter.level)) })
            }
            if (filter.query.isNotEmpty()) AssistChip(onClick = {}, label = { Text(filter.query) })
            if (filter.from.isNotEmpty()) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.settings_server_logs_from) + " " + filter.from) })
            }
            if (filter.to.isNotEmpty()) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.settings_server_logs_to) + " " + filter.to) })
            }
        }
        TextButton(onClick = onClear) { Text(stringResource(R.string.settings_server_logs_clear)) }
    }
}

@Composable
private fun levelFilterLabel(level: String): String =
    stringResource(
        when (level) {
            "error" -> R.string.settings_server_logs_errors
            "warning" -> R.string.settings_server_logs_warnings
            else -> R.string.settings_server_logs_every
        },
    )

@Composable
private fun LogLine(
    line: SettingsServerLogLineRow,
    time: String,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            StatusChip(levelLabel(line.level), line.tone)
            if (line.at.isNotEmpty()) Hint(time)
        }
        SelectionContainer {
            Text(line.text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
        }
        HorizontalDivider(Modifier.padding(top = Spacing.XSmall))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    filter: SettingsServerLogsFilter,
    onApply: (String, String, String, String) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    var level by rememberSaveable { mutableStateOf(filter.level) }
    var query by rememberSaveable { mutableStateOf(filter.query) }
    var from by rememberSaveable { mutableStateOf(filter.from) }
    var to by rememberSaveable { mutableStateOf(filter.to) }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(
                    rememberScrollState(),
                ).imePadding()
                .padding(horizontal = Spacing.Screen)
                .padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.settings_server_logs_filter), style = MaterialTheme.typography.titleLarge)
            PickerField(
                stringResource(R.string.settings_server_logs_level),
                listOf(
                    "" to stringResource(R.string.settings_server_logs_every),
                    "warning" to stringResource(R.string.settings_server_logs_warnings),
                    "error" to stringResource(R.string.settings_server_logs_errors),
                ),
                level,
                onSelect = { level = it },
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(SETTINGS_LOGS_QUERY_MAX) },
                label = { Text(stringResource(R.string.settings_server_logs_text)) },
                placeholder = { Text(stringResource(R.string.settings_server_logs_example)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onSearch = { onApply(level, query, from, to) }),
                modifier = Modifier.fillMaxWidth(),
            )
            DateRow(stringResource(R.string.settings_server_logs_from), from) { from = it }
            DateRow(stringResource(R.string.settings_server_logs_to), to) { to = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.settings_server_logs_clear)) }
                Button(onClick = { onApply(level, query, from, to) }) { Text(stringResource(R.string.settings_server_logs_apply)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Text(label + ": " + value.ifEmpty { stringResource(R.string.settings_server_logs_any_date) })
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.settings_server_logs_clear_date))
            }
        }
    }
    if (open) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = dayToMillis(value))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onChange(millisToDay(it)) }
                    open = false
                }) { Text(stringResource(R.string.settings_server_logs_apply)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) {
            DatePicker(state = picker)
        }
    }
}

private fun dayToMillis(day: String): Long? =
    runCatching {
        LocalDate
            .parse(day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

private fun millisToDay(millis: Long): String =
    Instant
        .ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()
