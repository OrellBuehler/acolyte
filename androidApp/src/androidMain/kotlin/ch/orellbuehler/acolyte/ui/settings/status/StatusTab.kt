package ch.orellbuehler.acolyte.ui.settings.status

import android.text.format.Formatter
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.health.HealthState
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDiskLevel
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDiskView
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusFix
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusGroup
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMarker
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRow
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusSummaryKind
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.health.HealthViewModel
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

private const val SHOWN_ROWS = 6

fun LazyListScope.statusTab(
    state: HealthState,
    viewModel: HealthViewModel,
    onOpen: (Any) -> Unit,
) {
    item(key = "plate-health") { HealthPlate(state, viewModel, onOpen) }
    item(key = "plate-linked") { LinkedAppsPlate(state, viewModel, onOpen) }
    item(key = "plate-storage") { StoragePlate(state, viewModel, onOpen) }
}

@Composable
private fun HealthPlate(
    state: HealthState,
    viewModel: HealthViewModel,
    onOpen: (Any) -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_status_health)) {
        when {
            state.problems.isNotEmpty() ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                    state.problems.forEach { row -> ProblemRow(row, state, viewModel, onOpen) }
                }
            state.nothingLinked -> {
                CalmLine(Tone.IDLE, stringResource(R.string.settings_status_nothing_to_check))
                if (state.canAct) {
                    OutlinedButton(onClick = { onOpen(statusFixRoute(SettingsStatusFix.SOURCES)) }) {
                        Text(stringResource(R.string.settings_status_add_source))
                    }
                }
            }
            state.linkedUnreadable -> CalmLine(Tone.IDLE, stringResource(R.string.settings_status_no_problems_found))
            else -> CalmLine(Tone.OK, stringResource(R.string.settings_status_all_good))
        }
    }
}

@Composable
private fun CalmLine(
    tone: Tone,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
        ToneDot(tone, size = 10.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProblemRow(
    row: SettingsStatusRow,
    state: HealthState,
    viewModel: HealthViewModel,
    onOpen: (Any) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            ToneBang(row.tone)
            Column(Modifier.weight(1f)) {
                Text(statusRowName(row), style = MaterialTheme.typography.titleSmall)
                Text(
                    statusGroupLabel(row.group),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(statusWord(row.word), row.tone)
        }
        RowDetails(row)
        RowActions(row, state, viewModel, onOpen)
    }
}

@Composable
private fun RowDetails(row: SettingsStatusRow) {
    val reason = statusRowReason(row)
    if (reason.isNotBlank()) Text(reason, style = MaterialTheme.typography.bodyMedium)
    statusSince(row)?.let { Hint(it) }
}

@Composable
private fun RowActions(
    row: SettingsStatusRow,
    state: HealthState,
    viewModel: HealthViewModel,
    onOpen: (Any) -> Unit,
) {
    val fix = row.fix
    val check = row.check
    if (!state.canAct || (fix == null && check == null)) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        if (check != null) {
            val label =
                stringResource(
                    if (row.group == SettingsStatusGroup.JOBS) {
                        R.string.settings_status_run_now
                    } else {
                        R.string.settings_status_check
                    },
                )
            ActionButton(label, state.isBusy(row.key), { viewModel.check(row.key) })
        }
        if (fix != null) {
            OutlinedButton(onClick = { onOpen(statusFixRoute(fix)) }) { Text(stringResource(R.string.settings_status_fix)) }
        }
    }
}

@Composable
private fun LinkedAppsPlate(
    state: HealthState,
    viewModel: HealthViewModel,
    onOpen: (Any) -> Unit,
) {
    var picked by rememberSaveable { mutableStateOf("") }
    val current = state.kinds.firstOrNull { it.group.name == picked } ?: state.kinds.firstOrNull { it.group == state.defaultKind }
    val ok = state.kinds.sumOf { it.okCount }
    val on = state.kinds.sumOf { it.onCount }
    SettingsCard(stringResource(R.string.settings_status_linked), subtitle = "$ok/$on") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            state.kinds.forEach { kind ->
                FilterChip(
                    selected = kind.group == current?.group,
                    onClick = { picked = kind.group.name },
                    label = { Text(statusGroupLabel(kind.group), maxLines = 1) },
                    leadingIcon = { KindMarker(kind.marker) },
                )
            }
        }
        if (current != null) KindPane(current, state, viewModel, onOpen)
    }
}

@Composable
private fun KindMarker(marker: SettingsStatusMarker) {
    when (marker) {
        SettingsStatusMarker.FAIL -> ToneBang(Tone.FAIL)
        SettingsStatusMarker.WAIT -> ToneBang(Tone.WAIT)
        SettingsStatusMarker.OK -> ToneDot(Tone.OK)
        SettingsStatusMarker.IDLE, SettingsStatusMarker.NONE -> ToneDot(Tone.IDLE)
    }
}

@Composable
private fun kindSummary(kind: SettingsStatusKind): String =
    when (kind.summary) {
        SettingsStatusSummaryKind.NOTHING -> stringResource(R.string.settings_status_summary_nothing)
        SettingsStatusSummaryKind.NEEDS ->
            pluralStringResource(
                R.plurals.settings_status_summary_needs,
                kind.summaryCount,
                kind.summaryCount,
            )
        SettingsStatusSummaryKind.WORD -> kind.summaryWord?.let { statusWord(it) }.orEmpty()
        SettingsStatusSummaryKind.OFF -> stringResource(R.string.settings_status_summary_off, kind.summaryCount)
        SettingsStatusSummaryKind.WORKING ->
            pluralStringResource(
                R.plurals.settings_status_summary_working,
                kind.summaryCount,
                kind.summaryCount,
            )
        SettingsStatusSummaryKind.WAITING ->
            pluralStringResource(
                R.plurals.settings_status_summary_waiting,
                kind.summaryCount,
                kind.summaryCount,
            )
    }

@Composable
private fun KindPane(
    kind: SettingsStatusKind,
    state: HealthState,
    viewModel: HealthViewModel,
    onOpen: (Any) -> Unit,
) {
    var all by rememberSaveable(kind.group.name) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(statusGroupLabel(kind.group), style = MaterialTheme.typography.titleSmall)
                Text(kindSummary(kind), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.canAct && !kind.isEmpty) {
                TextButton(onClick = { onOpen(statusFixRoute(kind.fix)) }) { Text(stringResource(R.string.settings_status_manage)) }
            }
        }
        if (kind.isEmpty) {
            Text(
                statusGroupNone(kind.group),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.canAct) {
                OutlinedButton(onClick = { onOpen(statusFixRoute(kind.fix)) }) { Text(stringResource(R.string.settings_status_add)) }
            }
        } else {
            val shown = if (all) kind.rows else kind.rows.take(SHOWN_ROWS)
            shown.forEach { row ->
                KindRow(row, state, viewModel, onOpen)
                HorizontalDivider(color = MaterialTheme.acolyte.line)
            }
            if (kind.rows.size > SHOWN_ROWS) {
                TextButton(onClick = { all = !all }) {
                    Text(
                        if (all) {
                            stringResource(
                                R.string.settings_status_show_fewer,
                            )
                        } else {
                            stringResource(R.string.settings_status_show_all, kind.rows.size)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KindRow(
    row: SettingsStatusRow,
    state: HealthState,
    viewModel: HealthViewModel,
    onOpen: (Any) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            Text(
                statusRowName(row),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusChip(statusWord(row.word), row.tone)
        }
        if (row.needsAttention) {
            RowDetails(row)
            RowActions(row, state, viewModel, onOpen)
        }
    }
}

@Composable
private fun StoragePlate(
    state: HealthState,
    viewModel: HealthViewModel,
    onOpen: (Any) -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_status_group_library)) {
        when {
            !state.disksLoaded -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            state.disks.isEmpty() -> Text(stringResource(R.string.settings_status_no_folders), style = MaterialTheme.typography.bodyMedium)
            else ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                    state.disks.forEach { disk -> DiskCard(disk, state, onOpen) }
                }
        }
        state.backups?.let { BackupsLine(it, state.keyKit) }
    }
}

@Composable
private fun DiskCard(
    disk: SettingsStatusDiskView,
    state: HealthState,
    onOpen: (Any) -> Unit,
) {
    val context = LocalContext.current
    val problem = disk.problem
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            Text(statusDiskName(disk.label, disk.kind), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (problem != null) {
                StatusChip(statusWord(problem.word), problem.tone)
            } else {
                StatusChip(stringResource(R.string.settings_status_word_working), Tone.OK)
            }
        }
        if (disk.freeBytes >= 0) {
            val free = Formatter.formatFileSize(context, disk.freeBytes)
            if (disk.totalBytes > 0) {
                Text(
                    stringResource(R.string.settings_status_free_of, free, Formatter.formatFileSize(context, disk.totalBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(stringResource(R.string.settings_status_free, free), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (disk.usedPercent >= 0) {
            LinearProgressIndicator(
                progress = { disk.usedPercent / PERCENT_SCALE },
                color =
                    when (disk.level) {
                        SettingsStatusDiskLevel.NORMAL -> MaterialTheme.colorScheme.primary
                        SettingsStatusDiskLevel.WARN -> MaterialTheme.acolyte.wait
                        SettingsStatusDiskLevel.FULL -> MaterialTheme.acolyte.fail
                    },
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(
                disk.path,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (disk.usedPercent >= 0) Hint(stringResource(R.string.settings_status_pct_used, disk.usedPercent))
        }
        if (problem != null) {
            val reason = statusRowReason(problem)
            if (reason.isNotBlank()) Text(reason, style = MaterialTheme.typography.bodyMedium)
            val fix = problem.fix
            if (state.canAct && fix != null) {
                OutlinedButton(onClick = { onOpen(statusFixRoute(fix)) }) { Text(stringResource(R.string.settings_status_fix)) }
            }
        }
    }
}

private const val PERCENT_SCALE = 100f

@Composable
private fun BackupsLine(
    backups: SettingsStatusRow,
    keyKit: SettingsStatusRow?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        HorizontalDivider(color = MaterialTheme.acolyte.line)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.settings_status_backups),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            StatusChip(statusWord(backups.word), backups.tone)
        }
        relativeTime(backups.at)?.let { Hint(stringResource(R.string.settings_status_last_checked, it)) }
        if (backups.tone != Tone.OK) {
            val reason = statusRowReason(backups)
            if (reason.isNotBlank()) Text(reason, style = MaterialTheme.typography.bodyMedium)
        }
        if (keyKit != null) Hint(statusRowReason(keyKit))
    }
}
