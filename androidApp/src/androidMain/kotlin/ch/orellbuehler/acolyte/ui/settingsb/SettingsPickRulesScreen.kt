package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.quality.SettingsQualityNotice
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsPickRulesScreen(
    account: Account,
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { QualityListViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val notice = state.notice
    val noticeText = if (notice != null) listNoticeText(notice) else ""
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(notice?.seq) {
        if (notice != null) {
            snackbar.showSnackbar(noticeText)
            viewModel.presenter.dismissNotice(notice.seq)
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.acq_page_quality),
        onBack = onBack,
        actions = {
            if (state.allowed && state.mayEdit) {
                IconButton(onClick = { onOpen(Routes.settingsPickRuleEdit(0, 0)) }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.acq_q_l_add))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        item(key = "sets") { QualitySetsCard(state, viewModel, onOpen) }
                        item(key = "traits") { QualityTraitsCard(state, viewModel) }
                        item(key = "check") { QualityNameCheckCard(state, viewModel) }
                        item(key = "where") { QualityWhereCard(state, onOpen) }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    QualityRemoveDialog(state.remove, viewModel)
    QualityTraitSheet(state.traitSheet, viewModel)
    QualityTraitRemoveDialog(state.traitRemove, viewModel)
}

@Composable
private fun listNoticeText(notice: SettingsQualityNotice): String =
    when (notice.kind) {
        "removed" -> stringResource(R.string.acq_q_l_removed, notice.name)
        "trait_made" -> stringResource(R.string.acq_q_ot_made, notice.name) + " · " + stringResource(R.string.acq_q_ot_ready)
        "trait_saved" -> stringResource(R.string.acq_q_ot_saved, notice.name)
        "trait_removed" -> {
            val removed = stringResource(R.string.acq_q_ot_removed, notice.name)
            if (notice.count > 0) removed + " · " + pluralStringResource(R.plurals.acq_q_ot_out_of, notice.count, notice.count) else removed
        }
        else -> ""
    }
