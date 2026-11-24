package ch.orellbuehler.acolyte.ui.settingsb

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleState
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsSubtitlesScreen(
    account: Account,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SubtitleSettingsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    val note = state.note
    val noteText = note?.let { subtitleNoteText(it) }
    LaunchedEffect(note?.id) { if (noteText != null) snackbar.showSnackbar(noteText) }
    var leaving by rememberSaveable { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = state.dirty) { leaving = true }
    ScreenScaffold(
        title = stringResource(R.string.acq_page_subtitles),
        onBack = { if (state.dirty) leaving = true else onBack() },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        state.status.error?.let { error ->
                            item(key = "error") {
                                Column {
                                    ErrorBanner(error)
                                    TextButton(onClick = viewModel::refresh, modifier = Modifier.padding(horizontal = Spacing.Medium)) {
                                        Text(stringResource(R.string.acq_sub_retry))
                                    }
                                }
                            }
                        }
                        item(key = "languages") { SubtitleLanguagesCard(state, viewModel) }
                        item(key = "translator") { SubtitleTranslatorCard(state, viewModel) }
                        item(key = "sites") { SubtitleSitesSection(state, viewModel, onRemove = { removing = it }) }
                        item(key = "when") { SubtitleWhenCard(state, viewModel) }
                        item(key = "more") { SubtitleMoreOptions(state, viewModel) }
                    }
                }
                if (state.dirty) SubtitleSaveBar(state, viewModel, Modifier.align(Alignment.BottomCenter))
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (state.dirty) 112.dp else 0.dp))
        }
    }
    if (leaving) {
        ConfirmDialog(
            title = stringResource(R.string.acq_sub_leave_title),
            text = stringResource(R.string.acq_sub_leave_text),
            confirm = stringResource(R.string.acq_sub_leave),
            dismiss = stringResource(R.string.acq_sub_stay),
            onConfirm = {
                leaving = false
                viewModel.discard()
                onBack()
            },
            onDismiss = { leaving = false },
            danger = true,
        )
    }
    val site = state.sites.firstOrNull { it.id == removing }
    if (site != null) {
        ConfirmDialog(
            title = stringResource(R.string.acq_sub_confirm_remove, site.name),
            text = null,
            confirm = stringResource(R.string.acq_sub_remove),
            dismiss = stringResource(R.string.acq_sub_cancel),
            onConfirm = {
                removing = null
                viewModel.removeSite(site.id)
            },
            onDismiss = { removing = null },
            danger = true,
        )
    }
}

@Composable
private fun SubtitleSaveBar(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
    modifier: Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding()) {
            state.saveError?.let { FailText(subtitleErrorText(it)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.acq_sub_unsaved),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = viewModel::discard, modifier = Modifier.padding(end = Spacing.Small)) {
                    Text(stringResource(R.string.acq_sub_discard))
                }
                Button(onClick = viewModel::save, enabled = state.canSave) { Text(stringResource(R.string.acq_sub_save)) }
            }
        }
    }
}
