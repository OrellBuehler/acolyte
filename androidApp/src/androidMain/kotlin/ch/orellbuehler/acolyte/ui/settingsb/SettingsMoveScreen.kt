package ch.orellbuehler.acolyte.ui.settingsb

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.move.MoveDialog
import ch.orellbuehler.acolyte.feature.move.MoveListKind
import ch.orellbuehler.acolyte.feature.move.MoveNotice
import ch.orellbuehler.acolyte.feature.move.MoveNoticeKind
import ch.orellbuehler.acolyte.feature.move.MoveReadRow
import ch.orellbuehler.acolyte.feature.move.SettingsMoveState
import ch.orellbuehler.acolyte.feature.move.SettingsMoveStep
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing

data class MoveListRequest(
    val kind: MoveListKind,
    val id: String,
    val title: String,
)

data class MoveReadRequest(
    val kind: String,
    val address: String,
    val editing: Boolean,
)

@Composable
fun SettingsMoveScreen(
    account: Account,
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { MoveViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    var listing by remember { mutableStateOf<MoveListRequest?>(null) }
    var readRequest by remember { mutableStateOf<MoveReadRequest?>(null) }
    var forgetting by remember { mutableStateOf<MoveReadRow?>(null) }
    var nmOpen by rememberSaveable { mutableStateOf(false) }
    var keysOf by rememberSaveable { mutableStateOf("") }
    val running = state.run?.running == true && state.step == SettingsMoveStep.RUN
    val serverHost =
        remember(account.serverUrl) {
            runCatching {
                java.net
                    .URI(account.serverUrl)
                    .host
                    .orEmpty()
            }.getOrDefault("")
        }

    BackHandler(enabled = state.step != SettingsMoveStep.READ && !running) { viewModel.enter(SettingsMoveStep.READ) }

    val notice = state.notices.firstOrNull()
    if (notice != null) {
        val text = noticeText(notice)
        LaunchedEffect(notice.seq) {
            snackbar.showSnackbar(text)
            viewModel.dismissNotice(notice.seq)
        }
    }

    ScreenScaffold(title = stringResource(R.string.acq_page_move), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                MessageView(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.acq_mv_not_allowed),
                    message = stringResource(R.string.acq_mv_not_allowed_text),
                )
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    Column(Modifier.fillMaxSize()) {
                        MoveRail(state, running, viewModel::enter)
                        state.problem?.let { problem ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) { NoticeBanner(moveProblemText(problem)) }
                                TextButton(onClick = viewModel::dismissProblem) { Text(stringResource(R.string.acq_mv_close)) }
                            }
                        }
                        if (state.safeMode) NoticeBanner(stringResource(R.string.acq_mv_safe_mode))
                        Box(Modifier.weight(1f)) {
                            if (state.gone) {
                                MessageView(
                                    icon = Icons.Rounded.WarningAmber,
                                    title = stringResource(R.string.acq_mv_gone),
                                    action = stringResource(R.string.acq_mv_gone_back),
                                    onAction = viewModel::goBackToReads,
                                )
                            } else {
                                when (state.step) {
                                    SettingsMoveStep.READ ->
                                        MoveReadStep(
                                            state,
                                            viewModel,
                                            onAdd = { readRequest = MoveReadRequest("radarr", "", false) },
                                            onEdit = { readRequest = MoveReadRequest(it.kind, it.address, true) },
                                            onForget = { forgetting = it },
                                            onOpenDownloaders = { onOpen(Routes.settingsDownloaders) },
                                        )
                                    SettingsMoveStep.CHOOSE ->
                                        MoveChooseStep(
                                            state,
                                            viewModel,
                                            onShow = { listing = it },
                                            onKeys = { keysOf = it },
                                            onNotMoved = { nmOpen = true },
                                        )
                                    SettingsMoveStep.RUN -> MoveImportStep(state)
                                    SettingsMoveStep.DONE -> MoveDoneStep(state, viewModel, onShow = { listing = it }, onOpen = onOpen)
                                }
                            }
                        }
                        MoveFooter(state, viewModel)
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
        }
    }

    readRequest?.let { request ->
        MoveReadSheet(request, state, viewModel, serverHost, onClose = {
            readRequest = null
            if (!state.sheet.reading) viewModel.resetSheet()
        })
    }
    forgetting?.let { row ->
        MoveForgetDialog(row, onConfirm = {
            viewModel.forget(row.id)
            forgetting = null
        }, onDismiss = { forgetting = null })
    }
    listing?.let { request -> MoveItemsDialog(request, viewModel, onClose = { listing = null }) }
    if (nmOpen) MoveNotMovedSheet(state, viewModel, onClose = { nmOpen = false })
    if (keysOf.isNotEmpty()) {
        MoveKeysSheet(
            kind = if (keysOf == "downloaders") MoveListKind.DOWNLOADER_KEYS else MoveListKind.SOURCE_KEYS,
            viewModel = viewModel,
            onClose = { keysOf = "" },
        )
    }
    when (state.dialog) {
        MoveDialog.BEFORE -> MoveBeforeSheet(state, viewModel, onShow = { listing = it })
        MoveDialog.CONFIRM -> MoveConfirmDialog(state, viewModel)
        MoveDialog.UNDO -> MoveUndoDialog(state, viewModel)
        MoveDialog.NONE -> Unit
    }
    if (state.needsPassword) MovePasswordDialog(state, viewModel)
}

@Composable
fun noticeText(notice: MoveNotice): String =
    when (notice.kind) {
        MoveNoticeKind.READ_DONE ->
            buildString {
                append(stringResource(R.string.acq_mv_read_done, notice.name))
                if (notice.n > 0) {
                    append(" · ")
                    append(pluralStringResource(R.plurals.acq_mv_read_keys, notice.n, notice.n))
                } else if (notice.counts.isNotEmpty()) {
                    append(" · ")
                    append(moveCountsText(notice.counts))
                }
            }
        MoveNoticeKind.READ_FAILED, MoveNoticeKind.LINKED_FAILED ->
            stringResource(R.string.acq_mv_read_failed, notice.name) + (notice.problem?.let { " · " + moveProblemText(it) } ?: "")
        MoveNoticeKind.READ_STOPPED -> stringResource(R.string.acq_mv_stopped)
        MoveNoticeKind.IMPORT_FINISHED -> stringResource(R.string.acq_mv_n_import_finished, notice.id)
        MoveNoticeKind.IMPORT_FAILED ->
            stringResource(R.string.acq_mv_n_import_failed, notice.id) + " · " + stringResource(R.string.acq_mv_n_see_report)
        MoveNoticeKind.IMPORT_UNDONE -> stringResource(R.string.acq_mv_n_import_undone, notice.id)
        MoveNoticeKind.UNDO_FAILED -> stringResource(R.string.acq_mv_n_undo_failed)
    }

@Composable
private fun MoveRail(
    state: SettingsMoveState,
    locked: Boolean,
    onPick: (SettingsMoveStep) -> Unit,
) {
    val steps =
        listOf(
            SettingsMoveStep.READ to R.string.acq_mv_rail_read,
            SettingsMoveStep.CHOOSE to R.string.acq_mv_rail_choose,
            SettingsMoveStep.RUN to R.string.acq_mv_rail_import,
            SettingsMoveStep.DONE to R.string.acq_mv_rail_done,
        )
    val current = steps.indexOfFirst { it.first == state.step }
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), modifier = Modifier.fillMaxWidth()) {
            steps.forEachIndexed { index, (step, label) ->
                val selected = index == current
                val enabled = index < current && !locked
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).then(if (enabled) Modifier.clickable { onPick(step) } else Modifier),
                ) {
                    Text(
                        "${index + 1} ${stringResource(label)}",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.Small).fillMaxWidth(),
                    )
                }
            }
        }
        val busy = state.step == SettingsMoveStep.CHOOSE && (state.choose.planning || state.choose.trying || state.choose.checkingNow)
        if (busy) {
            val note = state.choose.note
            Hint(if (note != null) moveCheckText(note) else stringResource(R.string.acq_mv_checking))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoveFooter(
    state: SettingsMoveState,
    viewModel: MoveViewModel,
) {
    if (state.step == SettingsMoveStep.RUN) return
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.acq_mv_report_title)
    val run = state.run
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            val gateText = moveGateText(state.gate, state.choose.note)
            if (state.step != SettingsMoveStep.DONE && gateText.isNotEmpty()) {
                if (state.gate.ok) Hint(gateText) else FailText(gateText)
            }
            when (state.step) {
                SettingsMoveStep.READ ->
                    Button(
                        onClick = { viewModel.enter(SettingsMoveStep.CHOOSE) },
                        enabled = state.gate.ok,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.acq_mv_continue)) }
                SettingsMoveStep.CHOOSE ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        OutlinedButton(onClick = { viewModel.enter(SettingsMoveStep.READ) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.acq_mv_back))
                        }
                        Button(
                            onClick = viewModel::importNow,
                            enabled = state.gate.ok && !state.choose.checkingNow && !state.starting,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.acq_mv_import)) }
                    }
                SettingsMoveStep.DONE ->
                    if (run != null) {
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                        ) {
                            if (!run.running && run.hasReport) {
                                OutlinedButton(onClick = {
                                    viewModel.report { name, text -> context.startActivity(shareIntent(shareTitle, name, text)) }
                                }) { Text(stringResource(R.string.acq_mv_report)) }
                            }
                            if (run.undoFailed) {
                                OutlinedButton(onClick = viewModel::askUndo) { Text(stringResource(R.string.acq_mv_undo_again)) }
                            } else if (run.canUndo) {
                                TextButton(onClick = viewModel::askUndo) {
                                    Text(stringResource(R.string.acq_mv_undo_this), color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Button(onClick = viewModel::done, enabled = !run.running) { Text(stringResource(R.string.acq_mv_done)) }
                        }
                    }
                SettingsMoveStep.RUN -> Unit
            }
        }
    }
}

private fun shareIntent(
    title: String,
    name: String,
    text: String,
): Intent {
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, name)
            putExtra(Intent.EXTRA_TITLE, name)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    return Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
