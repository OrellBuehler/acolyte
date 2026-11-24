package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceBar
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCallRow
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceHealthState
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceRow
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsSourceHealthScreen(
    id: Long,
    account: Account,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val model = viewModel(key = "source-health-$id") { SourceHealthViewModel(sdk, id) }
    val state by model.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(model, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { model.watch() } }
    val row = state.row
    ScreenScaffold(title = stringResource(R.string.acq_src_health_title, row?.name.orEmpty()), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = model::refresh) {
                    if (row != null) HealthBody(row, state, model)
                }
            }
        }
    }
}

@Composable
private fun HealthBody(
    row: SettingsSourceRow,
    state: SettingsSourceHealthState,
    model: SourceHealthViewModel,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        modifier = Modifier.fillMaxSize(),
    ) {
        state.status.error?.let { error -> item { FailText(sourceErrorText(error), Modifier.padding(horizontal = Spacing.Screen)) } }
        item { HealthHeader(row, state, model) }
        item { HealthTiles(row, state) }
        item { OverTimeCard(state, model) }
        item { RecentCard(state.recent) }
    }
}

@Composable
private fun HealthHeader(
    row: SettingsSourceRow,
    state: SettingsSourceHealthState,
    model: SourceHealthViewModel,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        val uses = sourceUsesText(row.feed, row.autoSearch, row.handSearch)
        Text(
            stringResource(
                R.string.acq_src_health_subtitle,
                row.formatLabel,
                sourceProtocolText(row.protocolName),
                row.priority,
                uses,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            StatusChip(sourceWordText(row), row.tone)
            if (row.canTryNow && state.canEdit) {
                Button(onClick = model::tryNow, enabled = !state.trying) {
                    Text(stringResource(if (state.trying) R.string.acq_src_testing else R.string.acq_src_try_now))
                }
            }
        }
    }
}

@Composable
private fun Tile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: @Composable () -> Unit = {},
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = modifier) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
            detail()
        }
    }
}

@Composable
private fun StateDetail(row: SettingsSourceRow) {
    val until = sourceTimeText(row.until)?.let { stringResource(R.string.acq_src_paused_until, it) + "." }
    val text = listOfNotNull(row.note.ifBlank { null }, until).joinToString(" ")
    if (text.isNotEmpty()) Hint(text)
    row.test?.let { test ->
        if (test.ok) {
            Text(sourceOutcomeText(test), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.acolyte.okText)
        } else {
            FailText(sourceOutcomeText(test))
            test.cause?.let { Hint(sourceCauseText(it)) }
        }
    }
}

@Composable
private fun HealthTiles(
    row: SettingsSourceRow,
    state: SettingsSourceHealthState,
) {
    val never = stringResource(R.string.acq_src_health_never)
    val lastAnswer = row.lastSuccess.takeIf { it.isNotEmpty() }?.let { relativeTime(it) } ?: never
    val lastFailure = row.lastFailure.takeIf { it.isNotEmpty() }?.let { relativeTime(it) }
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Tile(stringResource(R.string.acq_src_health_state), sourceWordText(row), Modifier.weight(1f)) { StateDetail(row) }
            Tile(stringResource(R.string.acq_src_health_last_answer), lastAnswer, Modifier.weight(1f)) {
                Hint(
                    if (lastFailure != null) {
                        stringResource(R.string.acq_src_health_last_failure, lastFailure)
                    } else {
                        stringResource(R.string.acq_src_health_no_failures)
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Tile(
                stringResource(R.string.acq_src_health_today),
                if (row.avgMs > 0) sourceDurationText(row.avgMs) else stringResource(R.string.acq_src_health_no_times),
                Modifier.weight(1f),
            ) {
                if (row.avgMs > 0) Hint(stringResource(R.string.acq_src_health_average))
            }
            Tile(
                stringResource(R.string.acq_src_health_last_days, state.days),
                stringResource(R.string.acq_src_health_calls, state.totalCalls),
                Modifier.weight(1f),
            ) {
                Hint(stringResource(R.string.acq_src_health_failed_picked, state.totalFailures, state.totalGrabs))
            }
        }
    }
}

private fun dayLabel(day: String): String =
    try {
        LocalDate.parse(day).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    } catch (e: java.time.format.DateTimeParseException) {
        day
    }

@Composable
private fun OverTimeCard(
    state: SettingsSourceHealthState,
    model: SourceHealthViewModel,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Text(stringResource(R.string.acq_src_health_over_time), style = MaterialTheme.typography.titleMedium)
            Hint(stringResource(R.string.acq_src_health_days_shown))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                state.choices.forEach { n ->
                    FilterChip(
                        selected = state.days == n,
                        onClick = { model.setDays(n) },
                        label = { Text(stringResource(R.string.acq_src_health_days, n)) },
                    )
                }
            }
            if (!state.hasCalls) {
                Hint(stringResource(R.string.acq_src_health_no_calls))
            } else {
                Legend()
                CallBars(state.bars)
                DayAxis(state.bars)
            }
            Text(stringResource(R.string.acq_src_health_response_time), style = MaterialTheme.typography.titleSmall)
            if (!state.hasTimes) {
                Hint(stringResource(R.string.acq_src_health_no_times))
            } else {
                TimeBars(state.bars)
                Hint(sourceDurationText(state.bars.maxOf { it.avgMs }))
            }
        }
    }
}

@Composable
private fun Legend() {
    val colors = MaterialTheme.acolyte
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(colors.ok, stringResource(R.string.acq_src_health_answered))
        LegendDot(colors.fail, stringResource(R.string.acq_src_health_failed))
        LegendDot(MaterialTheme.colorScheme.primary, stringResource(R.string.acq_src_health_picked))
    }
    Hint(stringResource(R.string.acq_src_health_utc))
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CallBars(bars: List<SettingsSourceBar>) {
    val colors = MaterialTheme.acolyte
    val picked = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.acq_src_health_chart_label)
    Canvas(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = description }) {
        val slot = size.width / bars.size
        val width = slot * 0.7f
        val corner = CornerRadius(2f)
        bars.forEachIndexed { i, bar ->
            val x = i * slot + (slot - width) / 2
            val total = size.height * bar.callsPercent / 100f
            val failed = size.height * bar.failedPercent / 100f
            val answered = (total - failed).coerceAtLeast(0f)
            if (answered > 0f) drawRoundRect(colors.ok, Offset(x, size.height - answered), Size(width, answered), corner)
            if (failed > 0f) drawRoundRect(colors.fail, Offset(x, size.height - answered - failed), Size(width, failed), corner)
            if (bar.grabs > 0) {
                val center = Offset(x + width / 2, size.height - size.height * bar.grabsPercent / 100f)
                drawCircle(picked, radius = width.coerceAtMost(8f) / 2, center = center)
            }
        }
    }
}

@Composable
private fun TimeBars(bars: List<SettingsSourceBar>) {
    val color = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.acq_src_health_time_label)
    Canvas(Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = description }) {
        val slot = size.width / bars.size
        val width = slot * 0.7f
        bars.forEachIndexed { i, bar ->
            val h = size.height * bar.timePercent / 100f
            if (h > 0f) drawRoundRect(color, Offset(i * slot + (slot - width) / 2, size.height - h), Size(width, h), CornerRadius(2f))
        }
    }
}

@Composable
private fun DayAxis(bars: List<SettingsSourceBar>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Hint(dayLabel(bars.first().day))
        Hint(dayLabel(bars.last().day))
    }
}

@Composable
private fun RecentCard(recent: List<SettingsSourceCallRow>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.acq_src_health_recent), style = MaterialTheme.typography.titleMedium)
            if (recent.isEmpty()) Hint(stringResource(R.string.acq_src_health_no_recent))
            recent.forEachIndexed { index, call ->
                if (index > 0) HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(sourceOpText(call.op, call.rawOp), style = MaterialTheme.typography.bodyMedium)
                        Hint(relativeTime(call.at).orEmpty())
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (call.ok) {
                            Text(
                                stringResource(R.string.acq_src_health_answered_cell),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.acolyte.okText,
                            )
                        } else {
                            FailText(sourceErrorKindText(call.errorKind, call.rawError))
                        }
                        Hint(sourceDurationText(call.ms))
                    }
                }
            }
        }
    }
}
