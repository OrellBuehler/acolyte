package ch.orellbuehler.acolyte.ui.settingsb

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceRow
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesState
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch

@Composable
fun SettingsSourcesScreen(
    account: Account,
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val model = viewModel { SourcesViewModel(sdk) }
    val bridge = viewModel { BridgeViewModel(sdk) }
    val check = viewModel { SourceCheckViewModel(sdk) }
    val state by model.state.collectAsStateWithLifecycle()
    val bridgeState by bridge.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(model, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { model.watch() } }
    LaunchedEffect(bridgeState.changed) { if (bridgeState.changed > 0) model.reloadQuietly() }
    val notice = state.notice ?: bridgeState.notice
    val noticeText = notice?.let { sourceNoticeText(it) }
    LaunchedEffect(notice?.seq) {
        if (notice != null && noticeText != null) {
            snackbar.showSnackbar(noticeText)
            model.dismissNotice(notice.seq)
            bridge.dismissNotice(notice.seq)
        }
    }
    ScreenScaffold(title = stringResource(R.string.acq_page_sources), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(
                    status = state.status,
                    isEmpty = false,
                    onRefresh = {
                        model.refresh()
                        bridge.refresh()
                    },
                ) {
                    SourcesList(
                        state = state,
                        model = model,
                        bridge = bridge,
                        check = check,
                        listState = listState,
                        onHealth = { onOpen(Routes.settingsSourceHealth(it)) },
                        onBridgeLink = { scope.launch { listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1) } },
                    )
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun EmptyBlock(
    title: String,
    text: String,
    action: String?,
    onAction: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.XLarge, vertical = Spacing.XLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun SourcesList(
    state: SettingsSourcesState,
    model: SourcesViewModel,
    bridge: BridgeViewModel,
    check: SourceCheckViewModel,
    listState: LazyListState,
    onHealth: (Long) -> Unit,
    onBridgeLink: () -> Unit,
) {
    var removing by remember { mutableStateOf<SettingsSourceRow?>(null) }
    val editingId = state.editing?.takeIf { !it.isNew }?.id
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "lead") {
            Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Hint(stringResource(R.string.acq_src_lead))
                Hint(stringResource(R.string.acq_src_built_in))
                if (state.canAdd && state.hasRows) {
                    Button(onClick = model::startAdd) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = Spacing.Small))
                        Text(stringResource(R.string.acq_src_add))
                    }
                }
            }
        }
        state.status.error?.let { error -> item(key = "error") { ErrorBanner(error) } }
        state.editing?.let { form ->
            item(key = "form-${state.editToken}") { SourceFormCard(form, state, model) }
        }
        if (state.partOff) {
            item(key = "off") {
                if (state.hasRows) {
                    NoticeBanner(stringResource(R.string.acq_src_off_text))
                } else {
                    EmptyBlock(stringResource(R.string.acq_src_off_title), stringResource(R.string.acq_src_off_text), null) {}
                }
            }
        } else if (!state.hasRows && state.editing == null) {
            item(key = "none") {
                EmptyBlock(
                    stringResource(R.string.acq_src_none_title),
                    stringResource(R.string.acq_src_none_text),
                    if (state.canEdit) stringResource(R.string.acq_src_add) else null,
                    model::startAdd,
                )
            }
        }
        items(state.rows.filter { it.id != editingId }, key = { "row-${it.id}" }) { row ->
            SourceRowCard(
                row = row,
                canEdit = state.canEdit,
                onTest = { model.test(row.id) },
                onTry = { model.tryNow(row.id) },
                onPause = { model.setPaused(row.id, !row.isDisabled) },
                onHealth = { onHealth(row.id) },
                onChange = { model.startChange(row.id) },
                onRemove = { removing = row },
                onBridge = onBridgeLink,
            )
        }
        if (state.showBridgeAndCheck) {
            item(key = "bridge") { BridgeCard(bridge) }
            if (state.hasRows && state.canEdit) item(key = "check") { SourceCheckCard(check) }
        }
    }
    removing?.let { row ->
        ConfirmDialog(
            title = stringResource(R.string.acq_src_remove_title, row.name),
            text = stringResource(R.string.acq_src_remove_text),
            confirm = stringResource(R.string.acq_src_remove),
            dismiss = stringResource(R.string.acq_src_cancel),
            onConfirm = {
                model.remove(row.id)
                removing = null
            },
            onDismiss = { removing = null },
            danger = true,
        )
    }
}

@Composable
private fun SourceRowCard(
    row: SettingsSourceRow,
    canEdit: Boolean,
    onTest: () -> Unit,
    onTry: () -> Unit,
    onPause: () -> Unit,
    onHealth: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    onBridge: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).clickable(onClick = onHealth)) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        row.formatLabel + " · " + sourceProtocolText(row.protocolName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (row.showAvg) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(sourceDurationText(row.avgMs), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.acq_src_avg),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            StatusChip(sourceWordText(row), row.tone)
            SourceNotes(row, onBridge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                if (canEdit) {
                    FilledTonalButton(onClick = onTest, enabled = !row.testing && !row.busy) {
                        if (row.testing) {
                            CircularProgressIndicator(Modifier.padding(end = Spacing.Small).size(18.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.acq_src_testing))
                        } else {
                            Text(stringResource(R.string.acq_src_test))
                        }
                    }
                    if (row.canTryNow) {
                        val idle = !row.testing && !row.busy
                        OutlinedButton(onClick = onTry, enabled = idle) { Text(stringResource(R.string.acq_src_try_now)) }
                    }
                } else {
                    OutlinedButton(onClick = onHealth) { Text(stringResource(R.string.acq_src_health)) }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (canEdit) RowMenu(row, onPause, onHealth, onChange, onRemove)
                }
            }
        }
    }
}

@Composable
private fun SourceNotes(
    row: SettingsSourceRow,
    onBridge: () -> Unit,
) {
    row.test?.let { test ->
        if (test.ok) {
            Text(sourceOutcomeText(test), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.acolyte.okText)
        } else {
            FailText(sourceOutcomeText(test))
            test.cause?.let { Hint(sourceCauseText(it)) }
        }
    }
    if (row.showNote) {
        val until = sourceTimeText(row.until)?.let { " " + stringResource(R.string.acq_src_paused_until, it) }.orEmpty()
        if (row.noteBad) FailText(row.note + until) else Hint(row.note + until)
        row.cause?.takeIf { row.test == null }?.let { Hint(sourceCauseText(it)) }
    }
    if (row.showUntil) {
        sourceTimeText(row.until)?.let { Hint(stringResource(R.string.acq_src_paused_until, it)) }
    }
    if (row.isDisabled) Hint(stringResource(R.string.acq_src_paused_by_you))
    if (row.botCheckHint) {
        TextButton(onClick = onBridge) { Text(stringResource(R.string.acq_src_bot_check_link)) }
    }
}

@Composable
private fun RowMenu(
    row: SettingsSourceRow,
    onPause: () -> Unit,
    onHealth: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.acq_src_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (row.isDisabled) R.string.acq_src_resume else R.string.acq_src_pause)) },
                enabled = !row.busy,
                onClick = {
                    open = false
                    onPause()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.acq_src_health)) },
                onClick = {
                    open = false
                    onHealth()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.acq_src_change)) },
                onClick = {
                    open = false
                    onChange()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.acq_src_remove), color = MaterialTheme.acolyte.failText) },
                enabled = !row.busy,
                onClick = {
                    open = false
                    onRemove()
                },
            )
        }
    }
}
