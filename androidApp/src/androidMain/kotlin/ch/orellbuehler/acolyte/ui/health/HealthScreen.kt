package ch.orellbuehler.acolyte.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.health.HealthState
import ch.orellbuehler.acolyte.feature.health.tone
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun HealthScreen(onBack: (() -> Unit)?) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { HealthViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }

    ScreenScaffold(
        title = stringResource(R.string.section_health),
        onBack = onBack,
        actions = {
            if (state.running) {
                CircularProgressIndicator(Modifier.padding(12.dp).padding(end = 4.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = viewModel::runChecks) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.health_run))
                }
            }
        },
    ) { padding ->
        StatusContent(
            status = state.status,
            isEmpty = state.isEmpty,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            empty = {
                EmptyView(
                    title = stringResource(R.string.health_empty_title),
                    message = stringResource(R.string.health_empty_message),
                    icon = Icons.Rounded.MonitorHeart,
                )
            },
        ) {
            HealthList(state)
        }
    }
}

@Composable
private fun HealthList(state: HealthState) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        state.status.error?.let { item { ErrorBanner(it) } }
        state.actionError?.let { item { ErrorBanner(it) } }
        item {
            Row(Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
                StatusChip(overallText(state.overall), state.overall)
            }
        }
        items(state.checks, key = { it.id }) { check -> CheckRow(check) }
    }
}

@Composable
private fun CheckRow(check: HealthCheck) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Row(
            Modifier.padding(Spacing.Large),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(check.subject, style = MaterialTheme.typography.titleSmall)
                if (check.message.isNotBlank()) {
                    Text(check.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                relativeTime(check.since)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            StatusChip(stateText(check.tone), check.tone)
        }
    }
}

@Composable
private fun stateText(tone: Tone): String =
    stringResource(
        when (tone) {
            Tone.OK -> R.string.health_state_ok
            Tone.WAIT -> R.string.health_state_warning
            Tone.FAIL -> R.string.health_state_error
            Tone.INFO, Tone.IDLE -> R.string.health_state_unknown
        },
    )

@Composable
private fun overallText(tone: Tone): String =
    stringResource(
        when (tone) {
            Tone.FAIL -> R.string.health_overall_error
            Tone.WAIT -> R.string.health_overall_warning
            else -> R.string.health_overall_ok
        },
    )
