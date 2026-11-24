package ch.orellbuehler.acolyte.ui.settings.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.health.HealthState
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusInstallView
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMode
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusPhase
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusStanding
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.health.HealthViewModel
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

fun LazyListScope.updatesTab(
    state: HealthState,
    viewModel: HealthViewModel,
) {
    if (state.updateFailed && state.standing == SettingsStatusStanding.LOADING) {
        item(key = "update-failed") {
            SettingsCard(stringResource(R.string.settings_status_group_app)) {
                FailText(stringResource(R.string.settings_status_update_failed))
                OutlinedButton(onClick = viewModel::refresh) { Text(stringResource(R.string.settings_status_try_again)) }
            }
        }
        return
    }
    item(key = "update-band") { VersionBand(state, viewModel) }
    if (state.updateNotes.isNotEmpty()) item(key = "update-notes") { NotesCard(state) }
    val install = state.install
    if (state.canAct && install != null) item(key = "update-inapp") { InAppCard(state, install, viewModel) }
}

@Composable
private fun standingText(state: HealthState): String =
    when (state.standing) {
        SettingsStatusStanding.LOADING -> "…"
        SettingsStatusStanding.OFF -> stringResource(R.string.settings_status_standing_off)
        SettingsStatusStanding.BEHIND -> stringResource(R.string.settings_status_standing_behind)
        SettingsStatusStanding.PROBLEM -> stringResource(R.string.settings_status_standing_problem)
        SettingsStatusStanding.NONE -> stringResource(R.string.settings_status_standing_none)
        SettingsStatusStanding.CURRENT -> stringResource(R.string.settings_status_standing_current)
        SettingsStatusStanding.UNCHECKED -> stringResource(R.string.settings_status_standing_unchecked)
    }

@Composable
private fun VersionBand(
    state: HealthState,
    viewModel: HealthViewModel,
) {
    val uri = LocalUriHandler.current
    SettingsCard(stringResource(R.string.settings_status_your_version)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.settings_status_this_version), style = MaterialTheme.typography.labelMedium)
                Text(state.updateVersion.ifBlank { "…" }, style = MaterialTheme.typography.headlineSmall)
            }
            if (state.standing == SettingsStatusStanding.BEHIND && state.updateLatest.isNotBlank()) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    Text(stringResource(R.string.settings_status_newest), style = MaterialTheme.typography.labelMedium)
                    Text(state.updateLatest, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.acolyte.waitText)
                }
            } else if (state.standing == SettingsStatusStanding.CURRENT) {
                StatusChip(stringResource(R.string.settings_status_up_to_date), Tone.OK)
            }
        }
        Text(standingText(state), style = MaterialTheme.typography.bodyMedium)
        if (state.standing == SettingsStatusStanding.PROBLEM && state.updateProblem.isNotBlank()) Hint(state.updateProblem)
        relativeTime(state.updateCheckedAt)?.let { Hint(stringResource(R.string.settings_status_update_checked, it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (state.updateLink.isNotBlank()) {
                OutlinedButton(onClick = { uri.openUri(state.updateLink) }) { Text(stringResource(R.string.settings_status_what_changed)) }
            }
            if (state.canAct && state.updateOn) {
                val again = state.updateCheckedAt.isNotBlank()
                val label = if (again) R.string.settings_status_check_again else R.string.settings_status_check_now
                ActionButton(stringResource(label), state.checkingUpdate, viewModel::checkUpdates)
            }
        }
    }
}

@Composable
private fun NotesCard(state: HealthState) {
    SettingsCard(stringResource(R.string.settings_status_new_since)) {
        state.updateNotes.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text("•", style = MaterialTheme.typography.bodyMedium)
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InAppCard(
    state: HealthState,
    install: SettingsStatusInstallView,
    viewModel: HealthViewModel,
) {
    val manage = state.canManageUpdates
    SettingsCard(stringResource(R.string.settings_status_inapp_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.settings_status_inapp_check_on),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = install.checkOn, onCheckedChange = viewModel::setCheckOn, enabled = manage && !state.installBusy)
        }
        if (!manage) Hint(stringResource(R.string.settings_status_inapp_users_only))
        if (install.available) {
            ModeRow(state, install, viewModel)
            if (install.modeLocked) Hint(stringResource(R.string.settings_status_inapp_locked))
            if (!install.checkOn) Hint(stringResource(R.string.settings_status_inapp_check_off_hint))
            if (install.checkOn || install.mode != SettingsStatusMode.OFF) {
                Hint(
                    stringResource(
                        when (install.mode) {
                            SettingsStatusMode.AUTO -> R.string.settings_status_inapp_auto_hint
                            SettingsStatusMode.ASK -> R.string.settings_status_inapp_ask_hint
                            SettingsStatusMode.OFF -> R.string.settings_status_inapp_off_hint
                        },
                    ),
                )
            }
            Hint(
                stringResource(
                    if (install.fromUpdate) R.string.settings_status_inapp_from_update else R.string.settings_status_inapp_from_image,
                ),
            )
            InstallNotes(install)
            InstallProgress(install)
            if (state.installReady && manage) {
                Text(stringResource(R.string.settings_status_inapp_ready, install.ready), style = MaterialTheme.typography.bodyMedium)
                if (install.oneWay) Text(stringResource(R.string.settings_status_inapp_one_way), color = MaterialTheme.acolyte.waitText)
                Button(
                    onClick = viewModel::installNow,
                    enabled = !state.installBusy,
                ) { Text(stringResource(R.string.settings_status_inapp_update_now)) }
            }
            if (state.installCanGoBack && manage) {
                TextButton(onClick = viewModel::goBack, enabled = !state.installBusy) {
                    Text(stringResource(R.string.settings_status_inapp_go_back, install.previous))
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    state: HealthState,
    install: SettingsStatusInstallView,
    viewModel: HealthViewModel,
) {
    val modes = SettingsStatusMode.entries
    val enabled = state.canManageUpdates && !install.modeLocked && !state.installBusy
    Text(stringResource(R.string.settings_status_inapp_mode), style = MaterialTheme.typography.labelMedium)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = install.mode == mode,
                onClick = { viewModel.setMode(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                enabled = enabled && (mode == SettingsStatusMode.OFF || install.checkOn),
            ) {
                Text(
                    stringResource(
                        when (mode) {
                            SettingsStatusMode.OFF -> R.string.settings_status_inapp_off
                            SettingsStatusMode.ASK -> R.string.settings_status_inapp_ask
                            SettingsStatusMode.AUTO -> R.string.settings_status_inapp_auto
                        },
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun InstallNotes(install: SettingsStatusInstallView) {
    if (install.wentBack) {
        Hint(stringResource(R.string.settings_status_inapp_went_back, install.wentBackTo, install.wentBackLeft))
    } else if (install.rollback.isNotBlank()) {
        Text(stringResource(R.string.settings_status_inapp_rolled_back, install.rollback), color = MaterialTheme.acolyte.waitText)
    }
    if (install.execError.isNotBlank()) {
        Text(stringResource(R.string.settings_status_inapp_exec_error, install.execError), color = MaterialTheme.acolyte.waitText)
    }
}

@Composable
private fun InstallProgress(install: SettingsStatusInstallView) {
    val line =
        when (install.phase) {
            SettingsStatusPhase.DOWNLOADING -> stringResource(R.string.settings_status_inapp_downloading, install.progressVersion)
            SettingsStatusPhase.CHECKING -> stringResource(R.string.settings_status_inapp_checking, install.progressVersion)
            SettingsStatusPhase.WAITING -> stringResource(R.string.settings_status_inapp_waiting, install.progressMessage)
            SettingsStatusPhase.COPYING -> stringResource(R.string.settings_status_inapp_copying)
            SettingsStatusPhase.RESTARTING -> stringResource(R.string.settings_status_inapp_restarting)
            SettingsStatusPhase.FAILED -> stringResource(R.string.settings_status_inapp_failed, install.progressMessage)
            SettingsStatusPhase.IDLE, SettingsStatusPhase.READY -> ""
        }
    if (line.isNotEmpty()) {
        Text(
            line,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (install.phase ==
                    SettingsStatusPhase.FAILED
                ) {
                    MaterialTheme.acolyte.waitText
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
    }
}
