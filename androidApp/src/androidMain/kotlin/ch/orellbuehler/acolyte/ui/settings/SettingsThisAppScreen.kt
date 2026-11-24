package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsPathRow
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsThisAppState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.settings.controls.SettingsThisAppViewModel
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsError
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsKeep
import ch.orellbuehler.acolyte.ui.settings.controls.settingsControlsSaved
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch

@Composable
fun SettingsThisAppScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsThisAppViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    val savedText = state.saved?.let { settingsControlsSaved(it) }
    LaunchedEffect(state.saved?.id) { savedText?.let { snackbar.showSnackbar(it) } }
    ScreenScaffold(title = stringResource(R.string.settings_page_this_app), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                ThisAppList(state, viewModel, snackbar)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    SettingsReauthDialog(state.reauth, viewModel::confirmReauth, viewModel::cancelReauth)
}

@Composable
private fun ThisAppList(
    state: SettingsControlsThisAppState,
    viewModel: SettingsThisAppViewModel,
    snackbar: SnackbarHostState,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        item(key = "facts") { FactsCard(state) }
        if (state.canSettings && state.uiLoaded) item(key = "history") { HistoryCard(state, viewModel) }
        if (state.canSettings) {
            item(key = "advanced") {
                TextButton(onClick = { advanced = !advanced }, modifier = Modifier.padding(horizontal = Spacing.Small)) {
                    Text(stringResource(if (advanced) R.string.settings_controls_advanced_hide else R.string.settings_controls_advanced))
                }
            }
        }
        if (state.canSettings && advanced) {
            if (state.uiLoaded) item(key = "server") { ServerCard(state, viewModel) }
            item(key = "storage") { StorageCard(state, viewModel, snackbar) }
            if (state.urlBaseAvailable) item(key = "urlbase") { UrlBaseCard(state, viewModel) }
            if (state.isOwner && state.proxyAvailable && state.proxyLoaded) item(key = "proxy") { ProxyCard(state, viewModel) }
        }
        item(key = "licence") {
            Hint(stringResource(R.string.settings_controls_licence), Modifier.padding(horizontal = Spacing.Screen))
        }
    }
}

@Composable
private fun FactRow(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FactsCard(state: SettingsControlsThisAppState) {
    SettingsCard(stringResource(R.string.settings_controls_facts_title)) {
        FactRow(stringResource(R.string.settings_controls_facts_version), state.version.ifEmpty { "-" })
        FactRow(stringResource(R.string.settings_controls_facts_uptime), state.uptime.ifEmpty { "-" })
        if (state.schema > 0) FactRow(stringResource(R.string.settings_controls_facts_schema), state.schema.toString())
        FactRow(stringResource(R.string.settings_controls_facts_live), state.liveClients.toString())
    }
}

@Composable
private fun HistoryCard(
    state: SettingsControlsThisAppState,
    viewModel: SettingsThisAppViewModel,
) {
    val editable = state.canChangeHistory && !state.uiBusy
    SettingsCard(stringResource(R.string.settings_controls_history_title)) {
        PickerField(
            label = stringResource(R.string.settings_controls_keep_routine),
            options = state.routineChoices.map { it to settingsControlsKeep(it) },
            selected = state.historyRoutineDays,
            onSelect = viewModel::setHistoryRoutine,
            enabled = editable,
            hint = stringResource(R.string.settings_controls_keep_routine_hint),
        )
        PickerField(
            label = stringResource(R.string.settings_controls_keep_other),
            options = state.otherChoices.map { it to settingsControlsKeep(it) },
            selected = state.historyOtherDays,
            onSelect = viewModel::setHistoryOther,
            enabled = editable,
            hint = stringResource(R.string.settings_controls_keep_other_hint),
        )
        if (!state.canChangeHistory) Hint(stringResource(R.string.settings_controls_history_owner))
    }
}

@Composable
private fun ServerCard(
    state: SettingsControlsThisAppState,
    viewModel: SettingsThisAppViewModel,
) {
    SettingsCard(stringResource(R.string.settings_controls_server_title)) {
        PickerField(
            label = stringResource(R.string.settings_controls_pictures),
            options = state.cacheChoices.map { it to stringResource(R.string.settings_controls_gb, it) },
            selected = state.cacheGb,
            onSelect = viewModel::setCacheGb,
            enabled = !state.uiBusy,
            hint = stringResource(R.string.settings_controls_pictures_hint),
        )
        SwitchRow(
            stringResource(R.string.settings_controls_count),
            state.countInstall,
            viewModel::setCountInstall,
            hint = stringResource(R.string.settings_controls_count_hint),
            enabled = !state.uiBusy,
        )
    }
}

@Composable
private fun StorageCard(
    state: SettingsControlsThisAppState,
    viewModel: SettingsThisAppViewModel,
    snackbar: SnackbarHostState,
) {
    val storage = state.storage
    SettingsCard(
        stringResource(R.string.settings_controls_storage_title),
        subtitle = stringResource(R.string.settings_controls_storage_desc),
    ) {
        when {
            storage != null -> {
                val rows = storage.app.map { it to false } + storage.folders.map { it to true }
                rows.forEachIndexed { index, (row, folder) ->
                    if (index > 0) HorizontalDivider()
                    PathRow(row, folder, snackbar)
                }
                if (storage.savePath.isNotEmpty()) {
                    if (rows.isNotEmpty()) HorizontalDivider()
                    SaveFolderRow(storage.savePath, snackbar)
                }
            }
            state.storageProof -> {
                Text(stringResource(R.string.settings_controls_storage_proof), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = viewModel::proveStorage) { Text(stringResource(R.string.settings_controls_storage_confirm)) }
            }
            else -> state.storageError?.let { FailText(settingsControlsError(it)) }
        }
    }
}

@Composable
private fun CopyPath(
    path: String,
    snackbar: SnackbarHostState,
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.settings_controls_copied)
    Row(verticalAlignment = Alignment.CenterVertically) {
        SelectionContainer(Modifier.weight(1f)) {
            Text(path, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
        IconButton(
            onClick = {
                clipboard.setText(AnnotatedString(path))
                scope.launch { snackbar.showSnackbar(copied) }
            },
        ) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.settings_controls_copy_path))
        }
    }
}

@Composable
private fun PathRow(
    row: SettingsControlsPathRow,
    folder: Boolean,
    snackbar: SnackbarHostState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Text(
            if (row.size.isEmpty()) row.name else row.name + " · " + row.size,
            style = MaterialTheme.typography.titleSmall,
        )
        if (folder) Hint(stringResource(R.string.settings_controls_library_folder))
        CopyPath(row.path, snackbar)
        Hint(
            if (row.freeKnown) {
                stringResource(
                    R.string.settings_controls_free,
                    row.free,
                )
            } else {
                stringResource(R.string.settings_controls_free_unknown)
            },
        )
    }
}

@Composable
private fun SaveFolderRow(
    path: String,
    snackbar: SnackbarHostState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Text(stringResource(R.string.settings_controls_save_folder), style = MaterialTheme.typography.titleSmall)
        CopyPath(path, snackbar)
    }
}

@Composable
private fun UrlBaseCard(
    state: SettingsControlsThisAppState,
    viewModel: SettingsThisAppViewModel,
) {
    val urlBase = state.urlBase
    SettingsCard(stringResource(R.string.settings_controls_url_base), subtitle = stringResource(R.string.settings_controls_url_base_desc)) {
        OutlinedTextField(
            value = state.urlBaseDraft,
            onValueChange = viewModel::setUrlBase,
            label = { Text(stringResource(R.string.settings_controls_url_base)) },
            placeholder = { Text("/holyarrs") },
            supportingText = {
                if (urlBase != null) {
                    Text(
                        stringResource(
                            R.string.settings_controls_in_use,
                            urlBase.inUse.ifEmpty { stringResource(R.string.settings_controls_none) },
                        ),
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            enabled = urlBase != null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (urlBase?.restartNeeded == true) {
            Text(
                stringResource(R.string.settings_controls_needs_restart),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.acolyte.waitText,
            )
        }
        state.urlBaseError?.let { FailText(settingsControlsError(it)) }
        Button(onClick = viewModel::saveUrlBase, enabled = state.canSaveUrlBase) { Text(stringResource(R.string.settings_controls_save)) }
    }
}

@Composable
private fun ProxyCard(
    state: SettingsControlsThisAppState,
    viewModel: SettingsThisAppViewModel,
) {
    val form = state.proxy
    SettingsCard(stringResource(R.string.settings_controls_proxy), subtitle = stringResource(R.string.settings_controls_proxy_desc)) {
        PickerField(
            label = stringResource(R.string.settings_controls_proxy_type),
            options =
                listOf(
                    "" to stringResource(R.string.settings_controls_proxy_off),
                    "http" to "HTTP",
                    "https" to "HTTPS",
                    "socks5" to "SOCKS5",
                ),
            selected = form.type,
            onSelect = viewModel::setProxyType,
            enabled = !state.proxySaving,
        )
        if (state.proxyOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedTextField(
                    value = form.host,
                    onValueChange = viewModel::setProxyHost,
                    label = { Text(stringResource(R.string.settings_controls_proxy_host)) },
                    placeholder = { Text("proxy.lan") },
                    isError = form.host.isBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = form.port,
                    onValueChange = viewModel::setProxyPort,
                    label = { Text(stringResource(R.string.settings_controls_proxy_port)) },
                    isError = form.port.toIntOrNull() !in 1..65535,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = form.username,
                onValueChange = viewModel::setProxyUsername,
                label = { Text(stringResource(R.string.settings_controls_proxy_user)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = viewModel::setProxyPassword,
                label = { Text(stringResource(R.string.settings_controls_proxy_password)) },
                supportingText = {
                    if (state.proxyPasswordHint) Text(stringResource(R.string.settings_controls_proxy_password_kept))
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.bypass,
                onValueChange = viewModel::setProxyBypass,
                label = { Text(stringResource(R.string.settings_controls_proxy_bypass)) },
                placeholder = { Text("*.example.com, 10.0.0.0/8") },
                supportingText = { Text(stringResource(R.string.settings_controls_proxy_bypass_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow(
                stringResource(R.string.settings_controls_proxy_bypass_local),
                form.bypassLocal,
                viewModel::setProxyBypassLocal,
                hint = stringResource(R.string.settings_controls_proxy_bypass_local_hint),
            )
        }
        state.proxyError?.let { FailText(settingsControlsError(it)) }
        Button(onClick = viewModel::saveProxy, enabled = state.canSaveProxy) { Text(stringResource(R.string.settings_controls_save)) }
    }
}
