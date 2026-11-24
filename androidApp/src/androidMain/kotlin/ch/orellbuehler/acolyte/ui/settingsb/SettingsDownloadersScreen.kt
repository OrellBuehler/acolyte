package ch.orellbuehler.acolyte.ui.settingsb

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsDownloadersScreen(
    account: Account,
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { DownloadersViewModel(sdk) }
    val rules = viewModel { DownloaderRulesViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ruleState by rules.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    val form = state.form
    val messageText = state.message?.let { downloaderMessageText(it) }
    val rulesSaved = stringResource(R.string.acq_dl_rules_saved)
    LaunchedEffect(state.messageCount) { if (state.messageCount > 0 && messageText != null) snackbar.showSnackbar(messageText) }
    LaunchedEffect(ruleState.savedCount) { if (ruleState.savedCount > 0) snackbar.showSnackbar(rulesSaved) }
    BackHandler(enabled = form != null) { viewModel.cancelForm() }
    val title =
        when {
            form == null -> stringResource(R.string.acq_page_downloaders)
            form.isNew -> stringResource(R.string.acq_dl_add)
            else -> stringResource(R.string.acq_dl_form_change, form.name)
        }
    ScreenScaffold(
        title = title,
        onBack = if (form != null) viewModel::cancelForm else onBack,
        actions = {
            if (form == null && state.canEdit && !state.isEmpty) {
                IconButton(onClick = viewModel::startAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.acq_dl_add))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.allowed -> NotAllowed()
                form != null -> DownloaderForm(state, form, viewModel)
                else -> {
                    StatusContent(
                        status = state.status,
                        isEmpty = false,
                        onRefresh = {
                            viewModel.refresh()
                            rules.refresh()
                        },
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = if (ruleState.dirty) 160.dp else Spacing.XLarge),
                            verticalArrangement =
                                androidx.compose.foundation.layout.Arrangement
                                    .spacedBy(Spacing.Medium),
                        ) {
                            state.status.error?.let { error -> item(key = "error") { ErrorBanner(error) } }
                            item(key = "lead") { Hint(stringResource(R.string.acq_dl_lead), Modifier.padding(horizontal = Spacing.Screen)) }
                            if (state.isEmpty) {
                                item(key = "empty") { EmptyDownloaders(state.canEdit, viewModel::startAdd) }
                            } else {
                                items(state.rows, key = { it.id }) { row -> DownloaderCard(row, state.canEdit, viewModel) }
                            }
                            item(key = "rules") { DownloaderRulesSection(ruleState, rules) }
                        }
                    }
                    if (ruleState.dirty) DownloaderRulesSaveBar(ruleState, rules, Modifier.align(Alignment.BottomCenter))
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).padding(
                    bottom =
                        if (ruleState.dirty &&
                            form == null
                        ) {
                            96.dp
                        } else {
                            0.dp
                        },
                ),
            )
        }
    }
}
