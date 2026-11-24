package ch.orellbuehler.acolyte.ui.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.Report
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.requests.NOTE_MAX
import ch.orellbuehler.acolyte.feature.requests.ReportFilter
import ch.orellbuehler.acolyte.feature.requests.ReportKind
import ch.orellbuehler.acolyte.feature.requests.ReportsNote
import ch.orellbuehler.acolyte.feature.requests.ReportsNoteKind
import ch.orellbuehler.acolyte.feature.requests.ReportsState
import ch.orellbuehler.acolyte.feature.requests.fixerName
import ch.orellbuehler.acolyte.feature.requests.isOpen
import ch.orellbuehler.acolyte.feature.requests.partWords
import ch.orellbuehler.acolyte.feature.requests.reportKind
import ch.orellbuehler.acolyte.feature.requests.reporterName
import ch.orellbuehler.acolyte.feature.requests.versionWord
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.ReportProblemRoute
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { ReportsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    val note = state.note
    val noteText = note?.let { reportsNoteText(it) }
    LaunchedEffect(note?.id) {
        if (noteText != null) {
            snackbar.showSnackbar(noteText, duration = SnackbarDuration.Short)
            viewModel.dismissNote()
        }
    }
    ScreenScaffold(
        title = stringResource(if (state.manager) R.string.requests_reports else R.string.requests_your_reports),
        onBack = onBack,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                    item(key = "tabs") {
                        ChoiceChips(
                            options = ReportFilter.entries,
                            selected = state.filter,
                            label = { f ->
                                val n = state.counts[f] ?: 0
                                "${filterLabel(f)} $n"
                            },
                            onSelect = viewModel::setFilter,
                        )
                    }
                    if (state.shown.isEmpty()) {
                        item(key = "empty") { ReportsEmpty(state) }
                    }
                    items(state.shown, key = { it.id }) { report ->
                        ReportCard(report, state, onOpen = { open = report.id }, onFix = { viewModel.fix(report.id, "") {} }, onBetter = {
                            viewModel.betterCopy(report.id)
                        })
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    open?.let { id ->
        val report = state.report(id)
        if (report == null) {
            open = null
        } else {
            ReportWindow(
                report = report,
                state = state,
                onDismiss = { open = null },
                onFix = { reply -> viewModel.fix(id, reply) { open = null } },
                onReopen = { viewModel.reopen(id) { open = null } },
                onBetter = { viewModel.betterCopy(id) },
            )
        }
    }
}

@Composable
private fun filterLabel(filter: ReportFilter): String =
    stringResource(
        when (filter) {
            ReportFilter.OPEN -> R.string.requests_report_open
            ReportFilter.FIXED -> R.string.requests_report_fixed
            ReportFilter.ALL -> R.string.requests_report_all
        },
    )

@Composable
private fun reportsNoteText(note: ReportsNote): String {
    val error = note.error?.let { askErrorText(it) }.orEmpty()
    return when (note.kind) {
        ReportsNoteKind.FIXED ->
            if (note.reporter.isNotBlank()) {
                stringResource(R.string.requests_report_marked_fixed_tell, note.reporter)
            } else {
                stringResource(R.string.requests_report_marked_fixed)
            }
        ReportsNoteKind.FIX_FAILED -> stringResource(R.string.requests_report_fix_failed, error)
        ReportsNoteKind.REOPENED -> stringResource(R.string.requests_report_reopened)
        ReportsNoteKind.REOPEN_FAILED -> stringResource(R.string.requests_report_reopen_failed, error)
        ReportsNoteKind.SEARCH_STARTED -> stringResource(R.string.requests_report_search_started, note.name)
        ReportsNoteKind.SEARCH_FAILED -> stringResource(R.string.requests_report_search_failed, error)
    }
}

@Composable
private fun ReportsEmpty(state: ReportsState) {
    val (title, message) =
        when {
            state.filter == ReportFilter.FIXED ->
                stringResource(R.string.requests_report_empty_fixed) to
                    stringResource(R.string.requests_report_empty_fixed_text)
            state.manager && state.filter == ReportFilter.OPEN ->
                stringResource(R.string.requests_report_empty_open) to stringResource(R.string.requests_report_empty_manager_text)
            state.manager -> stringResource(R.string.requests_report_empty) to stringResource(R.string.requests_report_empty_manager_text)
            else -> stringResource(R.string.requests_report_empty) to stringResource(R.string.requests_report_empty_member_text)
        }
    EmptyView(title = title, message = message, icon = Icons.Outlined.ReportProblem, modifier = Modifier.padding(top = Spacing.XLarge))
}

@Composable
fun reportFacts(report: Report): String {
    val part =
        report.versionWord.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.requests_report_version, it) } ?: report.partWords
    return listOf(
        kindWord(report.titleKind),
        report.year
            .takeIf {
                it > 0
            }?.toString()
            .orEmpty(),
        part,
    ).filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
private fun ReportCard(
    report: Report,
    state: ReportsState,
    onOpen: () -> Unit,
    onFix: () -> Unit,
    onBetter: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen).clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(Spacing.Medium), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            RequestPoster(report.poster, report.titleName)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(report.titleName, style = MaterialTheme.typography.titleSmall)
                Hint(reportFacts(report))
                StatusChip(
                    if (report.isOpen) reportKindLabel(report.reportKind) else stringResource(R.string.requests_report_fixed),
                    if (report.isOpen) Tone.WAIT else Tone.OK,
                )
                val who = if (report.mine) stringResource(R.string.requests_you) else report.reporterName
                Hint(stringResource(R.string.requests_report_by, who, relativeTime(report.createdAt).orEmpty()))
                Text(
                    report.note.ifBlank { stringResource(R.string.requests_report_no_note) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                )
                if (!report.isOpen) {
                    val fixer = report.fixerName.ifBlank { null }
                    Hint(
                        stringResource(
                            R.string.requests_report_fixed_by,
                            fixer ?: stringResource(R.string.requests_someone),
                            relativeTime(report.closedAt).orEmpty(),
                        ),
                    )
                    if (report.reply.isNotBlank()) Text("“${report.reply}”", style = MaterialTheme.typography.bodySmall)
                }
                if (state.manager && report.isOpen) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        OutlinedButton(onClick = onBetter, enabled = report.id !in state.searching) {
                            Text(
                                stringResource(
                                    if (report.id in
                                        state.searching
                                    ) {
                                        R.string.requests_report_searching
                                    } else {
                                        R.string.requests_report_better
                                    },
                                ),
                            )
                        }
                        Button(
                            onClick = onFix,
                            enabled = report.id !in state.busy,
                        ) { Text(stringResource(R.string.requests_report_mark_fixed)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportWindow(
    report: Report,
    state: ReportsState,
    onDismiss: () -> Unit,
    onFix: (String) -> Unit,
    onReopen: () -> Unit,
    onBetter: () -> Unit,
) {
    var reply by rememberSaveable(report.id) { mutableStateOf("") }
    val kind = report.reportKind
    val quick =
        listOfNotNull(
            when (kind) {
                ReportKind.SUBTITLES -> stringResource(R.string.requests_reply_subtitles)
                ReportKind.SOUND -> stringResource(R.string.requests_reply_sound)
                ReportKind.PICTURE -> stringResource(R.string.requests_reply_picture)
                else -> null
            },
            stringResource(R.string.requests_reply_swapped),
            stringResource(R.string.requests_reply_try_again),
            stringResource(R.string.requests_reply_nothing_wrong),
        )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(
                if (report.year >
                    0
                ) {
                    "${report.titleName} (${report.year})"
                } else {
                    report.titleName
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Hint(reportFacts(report))
            StatusChip(
                if (report.isOpen) stringResource(R.string.requests_report_open) else stringResource(R.string.requests_report_fixed),
                if (report.isOpen) Tone.WAIT else Tone.OK,
            )
            Text(reportKindLabel(kind), style = MaterialTheme.typography.titleSmall)
            Hint(reportKindHint(kind))
            Text(report.note.ifBlank { stringResource(R.string.requests_report_no_note) }, style = MaterialTheme.typography.bodyLarge)
            if (!report.isOpen && report.reply.isNotBlank()) {
                Hint(
                    stringResource(
                        R.string.requests_report_fixed_by,
                        report.fixerName.ifBlank { stringResource(R.string.requests_someone) },
                        "",
                    ),
                )
                Text("“${report.reply}”", style = MaterialTheme.typography.bodyMedium)
            }
            if (state.manager && report.isOpen) {
                val who = if (report.mine) "" else report.reporterName
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it.take(NOTE_MAX) },
                    label = {
                        Text(
                            if (who.isNotBlank()) {
                                stringResource(R.string.requests_reply_to, who)
                            } else {
                                stringResource(R.string.requests_reply_to_reporter)
                            },
                        )
                    },
                    placeholder = { Text(stringResource(R.string.requests_reply_placeholder)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    quick.forEach { words -> SuggestionChip(onClick = { reply = words }, label = { Text(words) }) }
                }
                Hint(stringResource(if (reply.isBlank()) R.string.requests_reply_hint else R.string.requests_reply_hint_with))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.manager && report.isOpen) {
                    OutlinedButton(onClick = onBetter, enabled = report.id !in state.searching) {
                        Text(stringResource(R.string.requests_report_better))
                    }
                }
                Box(Modifier.weight(1f))
                if (state.manager) {
                    val busy = report.id in state.busy
                    if (report.isOpen) {
                        Button(onClick = { onFix(reply) }, enabled = !busy) {
                            if (busy) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(stringResource(R.string.requests_report_mark_fixed))
                            }
                        }
                    } else {
                        Button(onClick = onReopen, enabled = !busy) { Text(stringResource(R.string.requests_report_reopen)) }
                    }
                } else {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.requests_done)) }
                }
            }
        }
    }
}

@Composable
fun ReportProblemSheet(
    route: ReportProblemRoute,
    onClose: () -> Unit,
    onReports: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel =
        viewModel(key = "report-${route.titleId}-${route.season}-${route.itemId}-${route.albumId}") {
            ReportProblemViewModel(sdk, route.titleId, route.season, route.itemId, route.albumId, route.versionId)
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    RouteSheet(onClose) {
        val sent = state.sent
        if (sent != null) {
            DoneView(stringResource(R.string.requests_report_thanks), reportKindLabel(sent.reportKind) + " · " + reportFacts(sent), onClose)
            TextButton(onClick = onReports, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.requests_report_see_yours))
            }
        } else {
            Text(stringResource(R.string.requests_report_title), style = MaterialTheme.typography.titleLarge)
            if (route.name.isNotBlank()) Hint(route.name)
            Text(stringResource(R.string.requests_report_what), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                ReportKind.entries.forEach { kind ->
                    FilterChip(
                        selected = state.kind == kind,
                        onClick = { viewModel.setKind(kind) },
                        label = { Text(reportKindLabel(kind)) },
                    )
                }
            }
            val kind = state.kind
            Hint(if (kind != null) reportKindHint(kind) else stringResource(R.string.requests_report_pick))
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.requests_report_note)) },
                placeholder = { Text(stringResource(R.string.requests_report_note_placeholder)) },
                supportingText = { Text(stringResource(R.string.requests_report_left, state.left)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.missingKind) FailText(stringResource(R.string.requests_report_pick_first))
            state.error?.let { FailText(askErrorText(it)) }
            SheetFooter(
                stringResource(R.string.requests_report_send),
                enabled = true,
                busy = state.busy,
                onCancel = onClose,
                onPrimary = viewModel::send,
            )
        }
    }
}
