package ch.orellbuehler.acolyte.ui.settingsb

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchRun
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsSearchScreen(
    account: Account,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val vm = viewModel { SettingsSearchViewModel(sdk) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var leave by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val savedText = stringResource(R.string.acq_se_saved)
    LaunchedEffect(state.savedCount) { if (state.savedCount > 0) snackbar.showSnackbar(savedText) }
    val checking = stringResource(R.string.acq_se_checking)
    val looking = stringResource(R.string.acq_se_looking)
    LaunchedEffect(state.noteCount) {
        when (state.note) {
            SettingsSearchRun.FEEDS -> snackbar.showSnackbar(checking)
            SettingsSearchRun.BETTER -> snackbar.showSnackbar(looking)
            null -> Unit
        }
    }
    val couldnt = stringResource(R.string.acq_se_couldnt)
    val runError = state.runError?.let { settingsErrorText(it) }.orEmpty()
    LaunchedEffect(state.runErrorCount) { if (state.runErrorCount > 0) snackbar.showSnackbar("$couldnt. $runError") }
    val guard = { if (state.dirty) leave = true else onBack() }
    BackHandler(enabled = state.dirty) { leave = true }
    ScreenScaffold(
        title = stringResource(R.string.acq_page_search),
        onBack = guard,
        actions = {
            if (state.allowed && state.status.phase == Phase.READY && state.canRun) {
                if (state.running != null) {
                    CircularProgressIndicator(Modifier.padding(end = Spacing.Small).size(20.dp), strokeWidth = 2.dp)
                }
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.acq_se_more))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.acq_se_check_feeds)) },
                        enabled = state.running == null,
                        onClick = {
                            menu = false
                            vm.checkFeeds()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.acq_se_find_better)) },
                        enabled = state.running == null,
                        onClick = {
                            menu = false
                            vm.findBetter()
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (!state.allowed) {
                SettingsNotAllowed(stringResource(R.string.acq_se_not_allowed), stringResource(R.string.acq_se_not_allowed_text))
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = vm::refresh) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        item(key = "nightly") { SearchNightlyCard(state, vm) }
                        item(key = "early") { SearchEarlyCard(state, vm) }
                        item(key = "packs") { SearchPacksCard(state, vm) }
                        item(key = "music") { SearchMusicCard(state, vm) }
                    }
                }
                Column(Modifier.align(Alignment.BottomCenter)) {
                    SnackbarHost(snackbar)
                    if (state.dirty) SearchSaveBar(state, vm::discard, vm::save)
                }
            }
        }
    }
    if (leave) {
        ConfirmDialog(
            title = stringResource(R.string.acq_se_leave_title),
            text = stringResource(R.string.acq_se_leave_text),
            confirm = stringResource(R.string.acq_se_discard),
            dismiss = stringResource(R.string.acq_se_leave_stay),
            danger = true,
            onConfirm = {
                leave = false
                vm.discard()
                onBack()
            },
            onDismiss = { leave = false },
        )
    }
}
