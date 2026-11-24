package ch.orellbuehler.acolyte.ui.settings.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.health.HealthState
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusJobLine
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRead
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.health.HealthViewModel
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

fun LazyListScope.jobsTab(
    state: HealthState,
    viewModel: HealthViewModel,
) {
    item(key = "jobs-running") { RunningCard(state) }
    item(key = "jobs-next") { NextUpCard(state) }
    item(key = "jobs-finished") { FinishedCard(state) }
    item(key = "jobs-all") { AllJobsCard(state, viewModel) }
}

@Composable
private fun RunningCard(state: HealthState) {
    SettingsCard(stringResource(R.string.settings_status_running_now)) {
        if (state.running.isEmpty()) {
            Text(stringResource(R.string.settings_status_nothing_running), style = MaterialTheme.typography.bodyMedium)
        }
        state.running.forEach { job ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                    Text(job.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (job.queued) {
                        StatusChip(stringResource(R.string.settings_status_queued), Tone.IDLE)
                    } else if (job.hasProgress) {
                        Text("${job.percent}%", style = MaterialTheme.typography.labelLarge)
                    } else {
                        StatusChip(stringResource(R.string.settings_status_word_running), Tone.OK)
                    }
                }
                if (job.hasProgress) {
                    LinearProgressIndicator(progress = { job.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
                if (job.note.isNotBlank()) Hint(job.note)
            }
        }
    }
}

@Composable
private fun NextUpCard(state: HealthState) {
    SettingsCard(stringResource(R.string.settings_status_next_up)) {
        if (state.nextUp.isEmpty()) {
            Text(stringResource(R.string.settings_status_no_runs_planned), style = MaterialTheme.typography.bodyMedium)
        }
        state.nextUp.forEach { next ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    statusDue(next.due),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(DUE_WIDTH),
                )
                ToneDot(next.tone)
                Text(next.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (next.tone == Tone.FAIL || next.tone == Tone.WAIT) StatusChip(statusWord(next.word), next.tone)
            }
        }
    }
}

private val DUE_WIDTH = 96.dp

@Composable
private fun FinishedCard(state: HealthState) {
    SettingsCard(stringResource(R.string.settings_status_recently_finished)) {
        if (state.finished.isEmpty()) {
            Text(stringResource(R.string.settings_status_nothing_finished), style = MaterialTheme.typography.bodyMedium)
        }
        state.finished.forEach { run ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    relativeTime(run.endedAt).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(DUE_WIDTH),
                )
                ToneDot(run.tone)
                Text(run.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (run.tone != Tone.OK) StatusChip(statusWord(run.word), run.tone)
            }
        }
    }
}

@Composable
private fun AllJobsCard(
    state: HealthState,
    viewModel: HealthViewModel,
) {
    SettingsCard(stringResource(R.string.settings_status_all_jobs)) {
        if (state.jobLines.isEmpty()) {
            val failed = SettingsStatusRead.JOBS in state.failedReads
            Text(
                stringResource(if (failed) R.string.settings_status_jobs_failed else R.string.settings_status_no_jobs),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.jobLines.forEachIndexed { index, line ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.acolyte.line)
            JobLine(line, state, viewModel)
        }
    }
}

@Composable
private fun JobLine(
    line: SettingsStatusJobLine,
    state: HealthState,
    viewModel: HealthViewModel,
) {
    val row = line.row
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (row.tone != Tone.OK) StatusChip(statusWord(row.word), row.tone)
        }
        if (row.tone == Tone.FAIL) {
            val reason = statusRowReason(row)
            if (reason.isNotBlank()) Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.acolyte.failText)
        }
        val last = relativeTime(row.lastRun) ?: stringResource(R.string.settings_status_not_yet)
        Hint(stringResource(R.string.settings_status_last_run, last))
        Hint(stringResource(R.string.settings_status_next_run, statusDue(line.due)))
        if (state.canAct) {
            ActionButton(stringResource(R.string.settings_status_run_now), state.isBusy(row.key), { viewModel.check(row.key) })
        }
    }
}
