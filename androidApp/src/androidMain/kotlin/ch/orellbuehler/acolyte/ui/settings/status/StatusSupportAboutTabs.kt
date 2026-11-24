package ch.orellbuehler.acolyte.ui.settings.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.health.HealthState
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusAboutLoad
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusLinks
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusStanding
import ch.orellbuehler.acolyte.feature.settings.status.settingsStatusUptime
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.health.HealthViewModel
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.delay

fun LazyListScope.supportTab() {
    item(key = "support") { SupportCard() }
}

@Composable
private fun SupportCard() {
    val uri = LocalUriHandler.current
    SettingsCard(stringResource(R.string.settings_status_support_heading)) {
        Text(stringResource(R.string.settings_status_support_p1), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.settings_status_support_p2), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.settings_status_support_does_heading), style = MaterialTheme.typography.titleSmall)
        listOf(
            R.string.settings_status_support_does1,
            R.string.settings_status_support_does2,
            R.string.settings_status_support_does3,
        ).forEach {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text("•", style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            stringResource(R.string.settings_status_support_p3_lead) + " " + stringResource(R.string.settings_status_support_p3),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(stringResource(R.string.settings_status_support_p4), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { uri.openUri(SettingsStatusLinks.SPONSOR) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.padding(end = Spacing.Small))
            Text(stringResource(R.string.settings_status_support_sponsor))
        }
        OutlinedButton(onClick = { uri.openUri(SettingsStatusLinks.COFFEE) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_status_support_coffee))
        }
        OutlinedButton(onClick = { uri.openUri(SettingsStatusLinks.STAR) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_status_support_star))
        }
        OutlinedButton(onClick = { uri.openUri(SettingsStatusLinks.NOTE) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_status_support_note))
        }
    }
}

fun LazyListScope.aboutTab(
    state: HealthState,
    viewModel: HealthViewModel,
    onSeeUpdate: () -> Unit,
) {
    item(key = "about") { AboutCard(state, viewModel, onSeeUpdate) }
    if (state.canBundle) item(key = "about-help") { HelpCard(state, viewModel) }
}

@Composable
private fun AboutCard(
    state: HealthState,
    viewModel: HealthViewModel,
    onSeeUpdate: () -> Unit,
) {
    val about = state.about
    SettingsCard(stringResource(R.string.settings_status_tab_about)) {
        if (state.aboutLoad == SettingsStatusAboutLoad.FAILED && !about.hasDetails) {
            FailText(stringResource(R.string.settings_status_about_failed))
            OutlinedButton(onClick = viewModel::refresh) { Text(stringResource(R.string.settings_status_try_again)) }
        }
        KeyValue(stringResource(R.string.settings_status_about_version)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                val denied = state.aboutLoad == SettingsStatusAboutLoad.FAILED || state.aboutLoad == SettingsStatusAboutLoad.DENIED
                Text(about.version.ifBlank { if (denied) "-" else "…" }, style = MaterialTheme.typography.bodyMedium)
                if (state.updateAvailable) {
                    StatusChip(stringResource(R.string.settings_status_update_available), Tone.WAIT)
                } else if (state.standing == SettingsStatusStanding.CURRENT) {
                    StatusChip(stringResource(R.string.settings_status_up_to_date), Tone.OK)
                }
            }
            if (state.updateAvailable) {
                OutlinedButton(onClick = onSeeUpdate) { Text(stringResource(R.string.settings_status_see_update)) }
            }
        }
        if (about.hasDetails) {
            KeyValueText(stringResource(R.string.settings_status_about_go), about.go)
            KeyValueText(stringResource(R.string.settings_status_about_system), about.os)
            KeyValueText(
                stringResource(R.string.settings_status_about_container),
                stringResource(if (about.container) R.string.settings_status_yes else R.string.settings_status_no),
            )
            KeyValueText(
                stringResource(R.string.settings_status_about_database),
                stringResource(R.string.settings_status_about_sqlite, about.sqlite),
            )
            KeyValueText(stringResource(R.string.settings_status_about_migration), about.migration.toString())
            KeyValueText(stringResource(R.string.settings_status_about_config_folder), about.configDir, mono = true)
            KeyValueText(stringResource(R.string.settings_status_about_start_folder), about.startDir, mono = true)
            KeyValue(stringResource(R.string.settings_status_about_uptime)) { Uptime(about.uptimeSeconds, about.loadedAt) }
        }
        if (about.hasServer) {
            KeyValueText(
                stringResource(R.string.settings_status_about_writes),
                stringResource(R.string.settings_status_about_writes_value, about.writes, about.writeWaitP95Ms.toString()),
            )
            KeyValueText(
                stringResource(R.string.settings_status_about_url_base),
                about.urlBase.ifBlank { stringResource(R.string.settings_status_about_url_none) },
            )
            KeyValueText(stringResource(R.string.settings_status_about_live_tabs), about.liveClients.toString())
        }
    }
}

@Composable
private fun Uptime(
    seconds: Long,
    loadedAt: Long,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(UPTIME_TICK)
            now = System.currentTimeMillis()
        }
    }
    val elapsed = if (loadedAt > 0) ((now - loadedAt) / MILLIS).coerceAtLeast(0) else 0
    Text(settingsStatusUptime(seconds + elapsed), style = MaterialTheme.typography.bodyMedium)
}

private const val UPTIME_TICK = 1_000L
private const val MILLIS = 1_000L

@Composable
private fun HelpCard(
    state: HealthState,
    viewModel: HealthViewModel,
) {
    SettingsCard(stringResource(R.string.settings_status_help_title)) {
        Text(stringResource(R.string.settings_status_help_text), style = MaterialTheme.typography.bodyMedium)
        Hint(stringResource(R.string.settings_status_help_slow))
        ActionButton(stringResource(R.string.settings_status_bundle), state.bundleBusy, viewModel::downloadBundle, Modifier.fillMaxWidth())
    }
}
