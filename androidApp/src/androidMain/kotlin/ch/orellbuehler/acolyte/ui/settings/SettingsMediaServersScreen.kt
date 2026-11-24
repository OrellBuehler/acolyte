package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersRow
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.SettingsSignInRoute
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.settings.mediaservers.DefaultTracksCard
import ch.orellbuehler.acolyte.ui.settings.mediaservers.MediaServerFormSheet
import ch.orellbuehler.acolyte.ui.settings.mediaservers.PlexLabelsCard
import ch.orellbuehler.acolyte.ui.settings.mediaservers.SettingsMediaServersViewModel
import ch.orellbuehler.acolyte.ui.settings.mediaservers.WatchHistoryCard
import ch.orellbuehler.acolyte.ui.settings.mediaservers.mediaServerTagText
import ch.orellbuehler.acolyte.ui.settings.mediaservers.mediaServerToldText
import ch.orellbuehler.acolyte.ui.settings.mediaservers.mediaServersNoticeText
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsMediaServersScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsMediaServersViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var removeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var askWatchOff by rememberSaveable { mutableStateOf(false) }
    val note = state.notice
    val noteText = note?.let { mediaServersNoticeText(it) }
    LaunchedEffect(note?.id) {
        if (note != null && noteText != null) {
            snackbar.showSnackbar(noteText)
            viewModel.dismissNotice(note.id)
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.settings_page_media_servers),
        onBack = onBack,
        actions = {
            if (state.allowed && state.servers.isNotEmpty()) {
                IconButton(onClick = viewModel::startAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.settings_mediaservers_add))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                        if (state.isEmpty) {
                            item(key = "empty") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    EmptyView(
                                        title = stringResource(R.string.settings_mediaservers_empty_title),
                                        message = stringResource(R.string.settings_mediaservers_empty_text),
                                        icon = Icons.Rounded.Tv,
                                    )
                                    Button(onClick = viewModel::startAdd) { Text(stringResource(R.string.settings_mediaservers_add)) }
                                }
                            }
                        }
                        items(state.servers, key = { it.id }) { row ->
                            ServerCard(
                                row,
                                onTest = { viewModel.test(row.id) },
                                onChange = { viewModel.startChange(row.id) },
                                onRemove = { removeId = row.id },
                            )
                        }
                        if (state.showWatch) {
                            item(key = "watch") {
                                WatchHistoryCard(state) { on -> if (on) viewModel.setWatchHistory(true) else askWatchOff = true }
                            }
                        }
                        if (state.showLabels) item(key = "labels") { PlexLabelsCard(state, viewModel) }
                        if (state.showTracks) item(key = "tracks") { DefaultTracksCard(state, viewModel) }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (state.formOpen) {
        MediaServerFormSheet(state, viewModel, onOpenSignIn = {
            viewModel.cancelForm()
            onOpen(SettingsSignInRoute)
        })
    }
    removeId?.let { id ->
        val row = state.servers.firstOrNull { it.id == id }
        ConfirmDialog(
            title = stringResource(R.string.settings_mediaservers_remove_title, row?.name.orEmpty()),
            text = stringResource(R.string.settings_mediaservers_remove_text),
            confirm = stringResource(R.string.settings_mediaservers_remove),
            dismiss = stringResource(R.string.common_cancel),
            danger = true,
            onConfirm = {
                removeId = null
                viewModel.remove(id)
            },
            onDismiss = { removeId = null },
        )
    }
    if (askWatchOff) {
        ConfirmDialog(
            title = stringResource(R.string.settings_mediaservers_watch_off_title),
            text = stringResource(R.string.settings_mediaservers_watch_off_text),
            confirm = stringResource(R.string.settings_mediaservers_watch_off_confirm),
            dismiss = stringResource(R.string.common_cancel),
            danger = true,
            onConfirm = {
                askWatchOff = false
                viewModel.setWatchHistory(false)
            },
            onDismiss = { askWatchOff = false },
        )
    }
    SettingsReauthDialog(state.reauth, viewModel::confirmReauth, viewModel::cancelReauth)
}

@Composable
private fun ServerCard(
    row: SettingsMediaServersRow,
    onTest: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    val uri = LocalUriHandler.current
    val tag = row.tag
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
            ) {
                AssistChip(onClick = {}, label = { Text(row.kindName) })
                if (!row.enabled) AssistChip(onClick = {}, label = { Text(stringResource(R.string.settings_mediaservers_off)) })
                if (tag != null) {
                    StatusChip(
                        text = mediaServerTagText(tag),
                        tone = if (tag.ok) Tone.OK else Tone.FAIL,
                        modifier = Modifier.padding(top = Spacing.XSmall),
                    )
                }
            }
            Text(
                row.url,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Hint(mediaServerToldText(row))
            if (tag != null && row.showsFailure) {
                FailText(tag.failure.ifEmpty { tag.error?.let { errorText(it) }.orEmpty() })
                if (tag.helpUrl.isNotEmpty()) {
                    TextButton(onClick = { runCatching { uri.openUri(tag.helpUrl) } }) {
                        Text(stringResource(R.string.settings_mediaservers_help))
                    }
                }
            } else if (row.showsLastError) {
                FailText(stringResource(R.string.settings_mediaservers_last_failed, row.lastError))
            } else if (row.showsLastChecked) {
                Hint(stringResource(R.string.settings_mediaservers_last_checked, relativeTime(row.lastOkAt) ?: row.lastOkAt))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onTest, enabled = !row.testing && !row.busy) {
                    if (row.testing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(Spacing.Small))
                    }
                    Text(stringResource(R.string.settings_mediaservers_test))
                }
                OutlinedButton(onClick = onChange, enabled = !row.busy) { Text(stringResource(R.string.settings_mediaservers_change)) }
                TextButton(onClick = onRemove, enabled = !row.busy) { Text(stringResource(R.string.settings_mediaservers_remove)) }
            }
        }
    }
}
