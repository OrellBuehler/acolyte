package ch.orellbuehler.acolyte.ui.settings.status

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksField
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksNoticeKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksRow
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthChecksViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsHealthChecks()

    val state: StateFlow<SettingsHealthChecksState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun runChecks() {
        viewModelScope.launch { presenter.runChecks() }
    }

    fun setNumber(
        field: SettingsHealthChecksField,
        text: String,
    ) = presenter.setNumber(field, text)

    fun setDays(days: Int) = presenter.setDays(days)

    fun setFeed(url: String) = presenter.setFeed(url)

    fun setLocal(on: Boolean) = presenter.setLocal(on)

    fun setPrerelease(on: Boolean) = presenter.setPrerelease(on)

    fun discard() = presenter.discard()

    fun save() {
        viewModelScope.launch { presenter.save() }
    }

    fun dismissNotice() = presenter.dismissNotice()

    suspend fun watch() = presenter.watch()
}

@Composable
fun HealthChecksPage(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { HealthChecksViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    var leaving by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state.dirty) { leaving = true }
    if (leaving) {
        ConfirmDialog(
            title = stringResource(R.string.settings_status_hc_leave_title),
            text = stringResource(R.string.settings_status_hc_leave_text),
            confirm = stringResource(R.string.settings_status_hc_leave_confirm),
            dismiss = stringResource(R.string.settings_status_hc_leave_stay),
            onConfirm = {
                leaving = false
                viewModel.discard()
                onBack()
            },
            onDismiss = { leaving = false },
            danger = true,
        )
    }
    val notice = state.notice
    val noticeText =
        when (notice?.kind) {
            SettingsHealthChecksNoticeKind.SAVED -> stringResource(R.string.settings_status_hc_saved)
            SettingsHealthChecksNoticeKind.RUN_FAILED -> stringResource(R.string.settings_status_hc_run_failed)
            null -> null
        }
    LaunchedEffect(notice?.id) {
        if (noticeText != null) {
            snackbar.showSnackbar(noticeText)
            viewModel.dismissNotice()
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.settings_page_health),
        onBack = { if (state.dirty) leaving = true else onBack() },
        actions = {
            if (state.allowed && state.canSeeChecks) {
                if (state.running) {
                    CircularProgressIndicator(Modifier.padding(12.dp).padding(end = 4.dp).size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = viewModel::runChecks) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.settings_status_check_again))
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                MessageView(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.settings_status_not_allowed),
                    message = stringResource(R.string.settings_status_not_allowed_text),
                )
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        if (state.canSeeChecks) item(key = "results") { ResultsCard(state) }
                        if (state.canEdit && state.hasSettings) item(key = "settings") { SettingsForm(state, viewModel) }
                    }
                }
                if (state.dirty) SaveBar(state, viewModel, Modifier.align(Alignment.BottomCenter))
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (state.dirty) 96.dp else 0.dp))
        }
    }
}

@Composable
private fun SaveBar(
    state: SettingsHealthChecksState,
    viewModel: HealthChecksViewModel,
    modifier: Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding()) {
            state.saveError?.let { FailText(statusErrorDetail(it)) }
            if (!state.valid) FailText(stringResource(R.string.settings_status_hc_invalid))
            Text(stringResource(R.string.settings_status_hc_unsaved), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = viewModel::discard) { Text(stringResource(R.string.settings_status_hc_discard)) }
                Button(onClick = viewModel::save, enabled = state.canSave) { Text(stringResource(R.string.settings_status_hc_save)) }
            }
        }
    }
}

@Composable
private fun ResultsCard(state: SettingsHealthChecksState) {
    SettingsCard(stringResource(R.string.settings_status_hc_results), subtitle = stringResource(R.string.settings_status_hc_results_sub)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_status_hc_worst), style = MaterialTheme.typography.bodyMedium)
            StatusChip(
                stringResource(
                    when (state.worst) {
                        Tone.OK -> R.string.settings_status_hc_fine
                        Tone.WAIT -> R.string.settings_status_word_warning
                        Tone.FAIL -> R.string.settings_status_word_broken
                        else -> R.string.settings_status_hc_unknown
                    },
                ),
                state.worst,
            )
        }
        state.sync.forEach { sync ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                HorizontalDivider(color = MaterialTheme.acolyte.line)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.settings_status_hc_tracking, sync.name),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    val word = if (sync.degraded) R.string.settings_status_hc_sync_degraded else R.string.settings_status_hc_checking
                    StatusChip(stringResource(word), if (sync.degraded) Tone.FAIL else Tone.WAIT)
                }
                Hint(stringResource(if (sync.degraded) R.string.settings_status_hc_reader_down else R.string.settings_status_hc_read_stale))
                if (sync.seenAt.isNotBlank()) {
                    relativeTime(sync.seenAt)?.let { Hint(stringResource(R.string.settings_status_hc_last_read, it)) }
                }
            }
        }
        if (state.checks.isEmpty()) {
            Text(stringResource(R.string.settings_status_hc_none), style = MaterialTheme.typography.bodyMedium)
        }
        state.checks.forEach { CheckRow(it) }
    }
}

@Composable
private fun kindLabel(kind: SettingsHealthChecksKind): String =
    stringResource(
        when (kind) {
            SettingsHealthChecksKind.LIBRARY_FOLDER -> R.string.settings_status_hc_kind_library_folder
            SettingsHealthChecksKind.DISK_SPACE -> R.string.settings_status_title_free_space
            SettingsHealthChecksKind.SOURCE -> R.string.settings_status_hc_kind_source
            SettingsHealthChecksKind.DOWNLOADER -> R.string.settings_status_hc_kind_downloader
            SettingsHealthChecksKind.BACKUPS -> R.string.settings_status_title_backups
            SettingsHealthChecksKind.MASTER_KEY -> R.string.settings_status_hc_kind_master_key
            SettingsHealthChecksKind.UPDATE -> R.string.settings_status_title_update
            SettingsHealthChecksKind.CONFIG_DISK -> R.string.settings_status_title_config_disk
            SettingsHealthChecksKind.CONFIG_FOLDER -> R.string.settings_status_hc_kind_config_folder
            SettingsHealthChecksKind.WAITING_DOWNLOADS -> R.string.settings_status_title_waiting_downloads
            SettingsHealthChecksKind.CLEAR_WAITING -> R.string.settings_status_title_clear_waiting
            SettingsHealthChecksKind.SAVE_FOLDER -> R.string.settings_status_hc_kind_save_folder
            SettingsHealthChecksKind.SAME_DOWNLOADER -> R.string.settings_status_hc_kind_same_downloader
            SettingsHealthChecksKind.TORRENT_DOWNLOADER -> R.string.settings_status_title_torrent_downloader
            SettingsHealthChecksKind.USENET_DOWNLOADER -> R.string.settings_status_title_usenet_downloader
            SettingsHealthChecksKind.TRAILERS -> R.string.settings_status_hc_kind_trailers
            SettingsHealthChecksKind.PEOPLE_CHECK -> R.string.settings_status_title_people_check
            SettingsHealthChecksKind.OTHER -> R.string.settings_status_hc_kind_other
        },
    )

@Composable
private fun CheckRow(row: SettingsHealthChecksRow) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        HorizontalDivider(color = MaterialTheme.acolyte.line)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val label = row.label.ifEmpty { kindLabel(row.kind) }
                Text(label, style = MaterialTheme.typography.titleSmall)
                if (row.subject.isNotBlank()) {
                    Text(row.subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (row.message.isNotBlank()) Text(row.message, style = MaterialTheme.typography.bodyMedium)
            }
            if (row.tone != Tone.OK) {
                Column(horizontalAlignment = Alignment.End) {
                    val word = if (row.tone == Tone.WAIT) R.string.settings_status_word_warning else R.string.settings_status_word_broken
                    StatusChip(stringResource(word), row.tone)
                    relativeTime(row.since)?.let { Hint(it) }
                }
            }
        }
    }
}

@Composable
private fun dayLabel(days: Int): String =
    if (days == 1) stringResource(R.string.settings_status_hc_day) else stringResource(R.string.settings_status_hc_days, days)

@Composable
private fun SettingsForm(
    state: SettingsHealthChecksState,
    viewModel: HealthChecksViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.settings_status_hc_settings)) {
        Hint(stringResource(R.string.settings_status_hc_only_checks))
        Hint(stringResource(R.string.settings_status_hc_privacy, state.version.ifBlank { "version" }))
        PickerField(
            label = stringResource(R.string.settings_status_hc_every_days),
            options = state.days.map { days -> days to dayLabel(days) },
            selected = state.draft.updateCheckEveryDays,
            onSelect = viewModel::setDays,
        )
        OutlinedTextField(
            value = state.lowText,
            onValueChange = { viewModel.setNumber(SettingsHealthChecksField.LOW_SPACE, it) },
            label = { Text(stringResource(R.string.settings_status_hc_low_space)) },
            supportingText = { Text(stringResource(R.string.settings_status_hc_low_space_hint)) },
            isError = !state.lowValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { advanced = !advanced }) {
            Text(stringResource(if (advanced) R.string.settings_status_hc_hide_advanced else R.string.settings_status_hc_advanced))
        }
        if (advanced) {
            OutlinedTextField(
                value = state.everyText,
                onValueChange = { viewModel.setNumber(SettingsHealthChecksField.EVERY_MINUTES, it) },
                label = { Text(stringResource(R.string.settings_status_hc_minutes)) },
                isError = !state.everyValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.guardText,
                onValueChange = { viewModel.setNumber(SettingsHealthChecksField.FLAP_GUARD, it) },
                label = { Text(stringResource(R.string.settings_status_hc_guard)) },
                supportingText = { Text(stringResource(R.string.settings_status_hc_guard_hint)) },
                isError = !state.guardValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.draft.updateCheckUrl,
                onValueChange = viewModel::setFeed,
                label = { Text(stringResource(R.string.settings_status_hc_feed)) },
                placeholder = { Text(state.saved?.updateCheckDefault.orEmpty(), fontFamily = FontFamily.Monospace) },
                supportingText = { Text(stringResource(R.string.settings_status_hc_feed_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow(stringResource(R.string.settings_status_hc_feed_local), state.draft.updateCheckLocal, viewModel::setLocal)
            SwitchRow(stringResource(R.string.settings_status_hc_prerelease), state.draft.updatePrerelease, viewModel::setPrerelease)
        }
    }
}
