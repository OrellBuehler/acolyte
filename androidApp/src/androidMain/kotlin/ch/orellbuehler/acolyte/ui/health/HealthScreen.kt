package ch.orellbuehler.acolyte.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.health.HealthState
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusHeadlineKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMarker
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRead
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.settings.shareTransferredFile
import ch.orellbuehler.acolyte.ui.settings.status.ToneDot
import ch.orellbuehler.acolyte.ui.settings.status.aboutTab
import ch.orellbuehler.acolyte.ui.settings.status.jobsTab
import ch.orellbuehler.acolyte.ui.settings.status.statusNoticeText
import ch.orellbuehler.acolyte.ui.settings.status.statusTab
import ch.orellbuehler.acolyte.ui.settings.status.supportTab
import ch.orellbuehler.acolyte.ui.settings.status.updatesTab
import ch.orellbuehler.acolyte.ui.theme.Spacing

private enum class StatusTabId {
    STATUS,
    JOBS,
    UPDATES,
    SUPPORT,
    ABOUT,
}

@Composable
fun HealthScreen(
    onBack: (() -> Unit)?,
    onOpen: (Any) -> Unit = {},
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { HealthViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    var tab by rememberSaveable { mutableStateOf(StatusTabId.STATUS) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val notice = state.notice
    val noticeText = notice?.let { statusNoticeText(it) }
    LaunchedEffect(notice?.id) {
        if (noticeText != null) {
            snackbar.showSnackbar(noticeText)
            viewModel.dismissNotice()
        }
    }
    LaunchedEffect(state.bundle) {
        state.bundle?.let {
            shareTransferredFile(context, it, "application/zip")
            viewModel.bundleHandled()
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.section_health),
        onBack = onBack,
        actions = {
            if (state.allowed && state.canAct && tab != StatusTabId.UPDATES) {
                if (state.isBusy("all")) {
                    CircularProgressIndicator(Modifier.padding(12.dp).padding(end = 4.dp).size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = viewModel::checkAll) {
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
                    StatusBody(state, viewModel, tab, { tab = it }, onOpen)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun StatusBody(
    state: HealthState,
    viewModel: HealthViewModel,
    tab: StatusTabId,
    onTab: (StatusTabId) -> Unit,
    onOpen: (Any) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        item(key = "headline") {
            Text(
                headlineText(state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Screen),
            )
        }
        state.status.error?.let { error -> item(key = "error") { ErrorBanner(error) } }
        if (state.failedReads.isNotEmpty()) item(key = "failed") { FailedReads(state.failedReads, viewModel) }
        item(key = "tabs") { TabStrip(state, tab, onTab) }
        when (tab) {
            StatusTabId.STATUS -> statusTab(state, viewModel, onOpen)
            StatusTabId.JOBS -> jobsTab(state, viewModel)
            StatusTabId.UPDATES -> updatesTab(state, viewModel)
            StatusTabId.SUPPORT -> supportTab()
            StatusTabId.ABOUT -> aboutTab(state, viewModel) { onTab(StatusTabId.UPDATES) }
        }
    }
}

@Composable
private fun headlineText(state: HealthState): String {
    val headline = state.headline
    return when (headline.kind) {
        SettingsStatusHeadlineKind.FINE -> stringResource(R.string.settings_status_headline_fine)
        SettingsStatusHeadlineKind.UNKNOWN -> stringResource(R.string.settings_status_headline_none)
        SettingsStatusHeadlineKind.NOTHING_LINKED -> stringResource(R.string.settings_status_headline_nothing_linked)
        SettingsStatusHeadlineKind.ATTENTION ->
            listOfNotNull(
                headline.checks.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.settings_status_headline_checks, it, it) },
                headline.jobs.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.settings_status_headline_jobs, it, it) },
            ).joinToString(", ")
    }
}

@Composable
private fun FailedReads(
    reads: List<SettingsStatusRead>,
    viewModel: HealthViewModel,
) {
    val what =
        reads
            .map {
                stringResource(
                    when (it) {
                        SettingsStatusRead.PARTS -> R.string.settings_status_what_parts
                        SettingsStatusRead.CHECKS -> R.string.settings_status_what_checks
                        SettingsStatusRead.JOBS -> R.string.settings_status_what_jobs
                        SettingsStatusRead.LISTS -> R.string.settings_status_what_lists
                        SettingsStatusRead.ALERTS -> R.string.settings_status_what_alerts
                    },
                )
            }.joinToString(", ")
    Row(
        Modifier.padding(horizontal = Spacing.Screen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.settings_status_could_not_load, what),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.common_retry)) }
    }
}

@Composable
private fun TabStrip(
    state: HealthState,
    tab: StatusTabId,
    onTab: (StatusTabId) -> Unit,
) {
    val tabs = StatusTabId.entries
    PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = Spacing.Screen) {
        tabs.forEach { id ->
            Tab(
                selected = id == tab,
                onClick = { onTab(id) },
                text = {
                    BadgedBox(
                        badge = {
                            when {
                                id == StatusTabId.JOBS && state.jobsBadge == SettingsStatusMarker.FAIL -> ToneDot(Tone.FAIL)
                                id == StatusTabId.JOBS && state.jobsBadge == SettingsStatusMarker.WAIT -> ToneDot(Tone.WAIT)
                                id == StatusTabId.UPDATES && state.updateAvailable -> ToneDot(Tone.WAIT)
                            }
                        },
                    ) {
                        Text(stringResource(tabLabel(id)), maxLines = 1)
                    }
                },
            )
        }
    }
}

private fun tabLabel(id: StatusTabId): Int =
    when (id) {
        StatusTabId.STATUS -> R.string.settings_status_tab_status
        StatusTabId.JOBS -> R.string.settings_status_tab_jobs
        StatusTabId.UPDATES -> R.string.settings_status_tab_updates
        StatusTabId.SUPPORT -> R.string.settings_status_tab_support
        StatusTabId.ABOUT -> R.string.settings_status_tab_about
    }
