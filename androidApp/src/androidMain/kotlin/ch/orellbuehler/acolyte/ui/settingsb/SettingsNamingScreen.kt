package ch.orellbuehler.acolyte.ui.settingsb

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingCleanNote
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsNamingScreen(
    account: Account,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val vm = viewModel { SettingsNamingViewModel(sdk) }
    val state by vm.state.collectAsStateWithLifecycle()
    val editor = remember { NamingEditor() }
    val snackbar = remember { SnackbarHostState() }
    var leave by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(vm) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.followExample() } }
    val savedText = stringResource(R.string.acq_nm_saved)
    LaunchedEffect(state.savedCount) { if (state.savedCount > 0) snackbar.showSnackbar(savedText) }
    val removing = stringResource(R.string.acq_nm_clean_removing)
    val older = stringResource(R.string.acq_nm_clean_older, state.cleanDays)
    val nothing = stringResource(R.string.acq_nm_clean_nothing)
    val kept = stringResource(R.string.acq_nm_clean_kept)
    LaunchedEffect(state.cleanCount) {
        when (state.cleanNote) {
            SettingsNamingCleanNote.REMOVING -> snackbar.showSnackbar("$removing. $older")
            SettingsNamingCleanNote.NOTHING -> snackbar.showSnackbar("$nothing. $kept")
            null -> Unit
        }
    }
    val guard = { if (state.dirty) leave = true else onBack() }
    BackHandler(enabled = state.dirty) { leave = true }
    ScreenScaffold(title = stringResource(R.string.acq_page_naming), onBack = guard) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (!state.allowed) {
                SettingsNotAllowed(stringResource(R.string.acq_nm_not_allowed), stringResource(R.string.acq_nm_not_allowed_text))
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = vm::refresh) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        item(key = "where") { NamingWhereCard(state, vm) }
                        item(key = "movies") { NamingMovieCard(state, vm, editor) }
                        item(key = "shows") { NamingShowCard(state, vm, editor) }
                        item(key = "music") { NamingMusicCard(state, vm, editor) }
                        item(key = "importing") { NamingImportingCard(state, vm) }
                        item(key = "holding") { NamingHoldingCard(state, vm) }
                    }
                }
                Column(Modifier.align(Alignment.BottomCenter)) {
                    SnackbarHost(snackbar)
                    val focused = editor.focused
                    if (focused != null && editor.sheet == null) {
                        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                            NamingExampleBox(state, state.exampleFor(focused), Modifier.padding(Spacing.Small))
                        }
                    }
                    if (state.dirty) NamingSaveBar(state, vm::discard, vm::save)
                }
            }
        }
    }
    NamingWordsSheet(state, editor, vm::insertWord)
    if (leave) {
        ConfirmDialog(
            title = stringResource(R.string.acq_nm_leave_title),
            text = stringResource(R.string.acq_nm_leave_text),
            confirm = stringResource(R.string.acq_nm_discard),
            dismiss = stringResource(R.string.acq_nm_leave_stay),
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
