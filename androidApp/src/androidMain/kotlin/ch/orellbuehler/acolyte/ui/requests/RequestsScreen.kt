package ch.orellbuehler.acolyte.ui.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.ClearKind
import ch.orellbuehler.acolyte.feature.requests.KindFilter
import ch.orellbuehler.acolyte.feature.requests.NOTE_MAX
import ch.orellbuehler.acolyte.feature.requests.PendingKind
import ch.orellbuehler.acolyte.feature.requests.RequestPage
import ch.orellbuehler.acolyte.feature.requests.RequestsNote
import ch.orellbuehler.acolyte.feature.requests.RequestsState
import ch.orellbuehler.acolyte.feature.requests.UNDO_MILLIS
import ch.orellbuehler.acolyte.feature.requests.askerNames
import ch.orellbuehler.acolyte.feature.requests.isFailed
import ch.orellbuehler.acolyte.feature.requests.isWaiting
import ch.orellbuehler.acolyte.feature.requests.life
import ch.orellbuehler.acolyte.feature.requests.requestPages
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.nav.SearchRoute
import ch.orellbuehler.acolyte.ui.nav.destination
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun RequestsScreen(
    account: Account,
    onBack: (() -> Unit)?,
    onNavigate: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { RequestsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    var review by rememberSaveable { mutableStateOf<Long?>(null) }
    var decline by rememberSaveable { mutableStateOf<Long?>(null) }
    var remove by rememberSaveable { mutableStateOf<Long?>(null) }
    var takeBack by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmAll by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var filters by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val pages = account.requestPages()

    NoteSnackbar(state.note, snackbar, onUndo = viewModel::undo, onShown = viewModel::dismissNote)

    ScreenScaffold(
        title = stringResource(Section.REQUESTS.destination.label),
        onBack = onBack,
        actions = {
            if (state.reportsOpen >= 0 || state.hasOwnReports) {
                IconButton(onClick = { onNavigate(Routes.reports) }) {
                    BadgedBox(badge = {
                        if (state.reportsOpen >
                            0
                        ) {
                            Badge { Text(if (state.reportsOpen > 99) "99+" else "${state.reportsOpen}") }
                        }
                    }) {
                        Icon(Icons.Outlined.ReportProblem, contentDescription = stringResource(R.string.requests_reports))
                    }
                }
            }
            if (state.approver) {
                IconButton(onClick = { filters = true }) {
                    BadgedBox(badge = { if (state.filtersOn > 0) Badge { Text("${state.filtersOn}") } }) {
                        Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.requests_filter))
                    }
                }
            }
            if (pages.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.requests_more))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        pages.forEach { page ->
                            DropdownMenuItem(
                                text = { Text(pageTitle(page)) },
                                onClick = {
                                    menu = false
                                    onNavigate(
                                        when (page) {
                                            RequestPage.RULES -> Routes.settingsRequests
                                            RequestPage.OUTSIDE_LISTS -> Routes.settingsOutsideLists
                                            RequestPage.NEVER_ADD -> Routes.settingsNeverAdd
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(
                status = state.status,
                isEmpty = state.isEmpty,
                onRefresh = viewModel::refresh,
                empty = {
                    Column {
                        EmptyView(
                            title = stringResource(CommonR.string.requests_empty_title),
                            message =
                                stringResource(
                                    if (state.canAsk) CommonR.string.requests_empty_message else CommonR.string.requests_empty_cannot_ask,
                                ),
                            icon = Icons.Rounded.Checklist,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.canAsk && !state.approver) {
                            Button(
                                onClick = { onNavigate(SearchRoute) },
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(Spacing.XLarge),
                            ) {
                                Text(stringResource(CommonR.string.requests_ask_something))
                            }
                        }
                    }
                },
            ) {
                RequestsList(
                    state = state,
                    viewModel = viewModel,
                    onOpen = { ask ->
                        if (state.approver) {
                            review = ask.id
                        } else if (ask.kind != "artist" && ask.tmdbId.isNotEmpty()) {
                            onNavigate(Routes.title(ask.kind, ask.tmdbId))
                        }
                    },
                    onDecline = { decline = it.id },
                    onTakeBack = { takeBack = it.id },
                    onApproveAll = { confirmAll = true },
                    onClear = { confirmClear = true },
                    onSearch = { onNavigate(SearchRoute) },
                    onReports = { onNavigate(Routes.reports) },
                )
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }

    review?.let { id ->
        AskReviewSheet(
            askId = id,
            onDismiss = { review = null },
            onApprove = { presenter ->
                review = null
                viewModel.approve(id, presenter)
            },
            onDecline = {
                review = null
                decline = id
            },
            onRemove = {
                review = null
                remove = id
            },
            onOpenTitle = { kind, tmdb ->
                review = null
                onNavigate(Routes.title(kind, tmdb))
            },
        )
    }
    decline?.let { id ->
        val ask = state.ask(id)
        if (ask == null) {
            decline = null
        } else {
            DeclineSheet(
                ask = ask,
                onDismiss = { decline = null },
                onDecline = { reason ->
                    decline = null
                    viewModel.decline(id, reason)
                },
            )
        }
    }
    remove?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.requests_remove_title),
            text = stringResource(R.string.requests_remove_text),
            confirm = stringResource(R.string.requests_remove),
            dismiss = stringResource(R.string.requests_keep),
            danger = true,
            onConfirm = {
                remove = null
                viewModel.remove(id)
            },
            onDismiss = { remove = null },
        )
    }
    takeBack?.let { id ->
        val ask = state.ask(id)
        ConfirmDialog(
            title = stringResource(R.string.requests_take_back_title, titleWithYear(ask)),
            text = stringResource(R.string.requests_take_back_text),
            confirm = stringResource(CommonR.string.requests_take_back),
            dismiss = stringResource(R.string.requests_keep),
            danger = true,
            onConfirm = {
                takeBack = null
                viewModel.takeBack(id)
            },
            onDismiss = { takeBack = null },
        )
    }
    if (confirmAll) {
        ConfirmDialog(
            title = stringResource(CommonR.string.requests_approve_all_title, state.waitingToApprove),
            text = if (state.isCut) stringResource(CommonR.string.requests_approve_all_cut) else null,
            confirm = stringResource(CommonR.string.requests_approve_n, state.waitingToApprove),
            dismiss = stringResource(R.string.common_cancel),
            onConfirm = {
                confirmAll = false
                viewModel.approveAll()
            },
            onDismiss = { confirmAll = false },
        )
    }
    if (confirmClear) {
        val which = state.clearable
        if (which == null) {
            confirmClear = false
        } else {
            val shared = state.shown.any { it.people.size > 1 }
            val body =
                stringResource(
                    if (which ==
                        ClearKind.FAILED
                    ) {
                        R.string.requests_clear_failed_text
                    } else {
                        R.string.requests_clear_finished_text
                    },
                ) +
                    if (shared) "\n\n" + stringResource(R.string.requests_clear_shared) else ""
            ConfirmDialog(
                title =
                    stringResource(
                        if (which == ClearKind.FAILED) R.string.requests_clear_failed_title else R.string.requests_clear_finished_title,
                        state.clearCount,
                    ),
                text = body,
                confirm = stringResource(R.string.requests_remove_all),
                dismiss = stringResource(R.string.requests_keep),
                danger = true,
                onConfirm = {
                    confirmClear = false
                    viewModel.clear(which)
                },
                onDismiss = { confirmClear = false },
            )
        }
    }
    if (filters) {
        FilterSheet(state, viewModel, onDismiss = { filters = false })
    }
}

@Composable
fun pageTitle(page: RequestPage): String =
    stringResource(
        when (page) {
            RequestPage.RULES -> R.string.requests_rules_title
            RequestPage.OUTSIDE_LISTS -> R.string.requests_outside_title
            RequestPage.NEVER_ADD -> R.string.requests_never_title
        },
    )

@Composable
private fun NoteSnackbar(
    note: RequestsNote?,
    host: SnackbarHostState,
    onUndo: (List<Long>) -> Unit,
    onShown: () -> Unit,
) {
    val text = note?.let { requestsNoteText(it) }
    val undo = stringResource(CommonR.string.requests_undo)
    LaunchedEffect(note?.id) {
        val current = note ?: return@LaunchedEffect
        val message = text ?: return@LaunchedEffect
        if (current.undoable) {
            val job =
                launch {
                    val result = host.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Indefinite)
                    if (result == SnackbarResult.ActionPerformed) onUndo(current.keys)
                }
            delay(UNDO_MILLIS)
            job.cancel()
        } else {
            host.showSnackbar(message, duration = SnackbarDuration.Long)
        }
        onShown()
    }
}

@Composable
private fun RequestsList(
    state: RequestsState,
    viewModel: RequestsViewModel,
    onOpen: (Ask) -> Unit,
    onDecline: (Ask) -> Unit,
    onTakeBack: (Ask) -> Unit,
    onApproveAll: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    onReports: () -> Unit,
) {
    val list = rememberLazyListState()
    LaunchedEffect(list, state.isCut) {
        snapshotFlow {
            list.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: 0
        }.collect { last ->
            if (state.isCut && last >= list.layoutInfo.totalItemsCount - 3) viewModel.loadMore()
        }
    }
    val shown = state.shown
    LazyColumn(state = list, contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        if (!state.approver) {
            item(key = "member") { MemberHeader(state, onSearch, onReports) }
        }
        item(key = "tabs") {
            if (state.approver) {
                ChoiceChips(
                    options = ApproverTab.entries,
                    selected = state.approverTab,
                    label = { tab ->
                        val n = state.approverCounts[tab] ?: 0
                        val failed = if (tab == ApproverTab.PROCESSING && state.failedCount > 0) " ⚠" else ""
                        if (n > 0) "${approverTabLabel(tab)} $n$failed" else approverTabLabel(tab)
                    },
                    onSelect = viewModel::setApproverTab,
                )
            } else {
                ChoiceChips(
                    options = state.memberTabs,
                    selected = state.memberTab,
                    label = { tab ->
                        val n = state.memberCounts[tab] ?: 0
                        if (n > 0) "${memberTabLabel(tab)} $n" else memberTabLabel(tab)
                    },
                    onSelect = viewModel::setMemberTab,
                )
            }
        }
        if (!state.approver && state.kinds.isNotEmpty()) {
            item(key = "kinds") { ChoiceChips(state.kinds, state.kind, { kindFilterLabel(it) }, viewModel::setKind) }
        }
        if (state.canApproveAll || state.clearable != null) {
            item(key = "bulk") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    if (state.canApproveAll) {
                        OutlinedButton(onClick = onApproveAll, enabled = !state.busy) {
                            Text(stringResource(CommonR.string.requests_approve_all, state.waitingToApprove))
                        }
                    }
                    when (state.clearable) {
                        ClearKind.FAILED ->
                            OutlinedButton(onClick = onClear, enabled = !state.busy) {
                                Text(stringResource(R.string.requests_clear_failed, state.clearCount))
                            }
                        ClearKind.FINISHED ->
                            OutlinedButton(onClick = onClear, enabled = !state.busy) {
                                Text(stringResource(R.string.requests_clear_finished, state.clearCount))
                            }
                        null -> Unit
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            item(key = "calm") { CalmTab(state, viewModel) }
        }
        items(shown, key = { it.id }) { ask ->
            RequestCard(
                ask = ask,
                state = state,
                onOpen = { onOpen(ask) },
                onApprove = { viewModel.approve(ask.id) },
                onDecline = { onDecline(ask) },
                onTakeBack = { onTakeBack(ask) },
                onUndo = { viewModel.undo(listOf(ask.id)) },
            )
        }
        if (state.isCut) {
            item(key = "cut") {
                Text(
                    stringResource(CommonR.string.requests_showing, state.asks.size, state.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
                )
            }
        }
    }
}

@Composable
private fun MemberHeader(
    state: RequestsState,
    onSearch: () -> Unit,
    onReports: () -> Unit,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        state.limits.forEach { line -> Hint(limitText(line)) }
        if (state.canAsk || state.hasOwnReports) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (state.canAsk) Button(onClick = onSearch) { Text(stringResource(CommonR.string.requests_ask_something)) }
                if (state.hasOwnReports) OutlinedButton(onClick = onReports) { Text(stringResource(R.string.requests_your_reports)) }
            }
        }
    }
}

@Composable
private fun CalmTab(
    state: RequestsState,
    viewModel: RequestsViewModel,
) {
    val filtered = state.approver && state.filtersOn > 0
    val (title, message) = calmWords(state)
    Column(Modifier.fillMaxWidth().padding(Spacing.XLarge), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (message != null) Hint(message, Modifier.padding(top = Spacing.XSmall))
        if (filtered) {
            TextButton(onClick = viewModel::clearFilters) { Text(stringResource(CommonR.string.requests_show_all)) }
        }
    }
}

@Composable
private fun RequestCard(
    ask: Ask,
    state: RequestsState,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onTakeBack: () -> Unit,
    onUndo: () -> Unit,
) {
    val life = ask.life
    val pending = state.pending[ask.id]
    var menu by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen).clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(Spacing.Medium), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            RequestPoster(ask.poster, ask.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ask.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (ask.id in state.newIds) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(stringResource(CommonR.string.requests_new)) }
                    }
                    if (!state.approver && ask.isWaiting && ask.mine && pending == null) {
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.requests_more))
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(CommonR.string.requests_take_back)) },
                                    onClick = {
                                        menu = false
                                        onTakeBack()
                                    },
                                )
                            }
                        }
                    }
                }
                Hint(factsLine(ask))
                StatusChip(if (state.approver) stageLabel(life.stage) else memberStatus(ask), life.tone)
                AskLines(ask, state.approver, state.myId)
                if (pending != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Hint(
                            stringResource(
                                if (pending ==
                                    PendingKind.APPROVE
                                ) {
                                    CommonR.string.requests_sending_approve
                                } else {
                                    CommonR.string.requests_sending_decline
                                },
                            ),
                            Modifier.weight(1f),
                        )
                        TextButton(onClick = onUndo) { Text(stringResource(CommonR.string.requests_undo)) }
                    }
                } else if (state.approver && ask.isWaiting) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        OutlinedButton(onClick = onDecline) { Text(stringResource(CommonR.string.requests_decline)) }
                        Button(
                            onClick = onApprove,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.acolyte.ok,
                                    contentColor = MaterialTheme.colorScheme.surface,
                                ),
                        ) { Text(stringResource(CommonR.string.requests_approve)) }
                    }
                } else if (state.approver && ask.isFailed) {
                    OutlinedButton(onClick = onDecline) { Text(stringResource(CommonR.string.requests_decline)) }
                }
                if (ask.state == "approved" && !ask.isFailed) {
                    Spacer(Modifier.height(Spacing.XSmall))
                    LinearProgressIndicator(
                        progress = { life.progress },
                        strokeCap = StrokeCap.Round,
                        color = MaterialTheme.acolyte.dot(life.tone),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AskLines(
    ask: Ask,
    approver: Boolean,
    myId: Long,
) {
    askLines(ask, approver, myId).forEach { line ->
        when (line.kind) {
            AskLineKind.MUTED -> Hint(line.text)
            AskLineKind.NOTE ->
                Text(
                    line.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            AskLineKind.FAIL -> FailText(line.text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    state: RequestsState,
    viewModel: RequestsViewModel,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge)) {
            if (state.stages.isNotEmpty()) {
                Text(stringResource(R.string.requests_filter_status), style = MaterialTheme.typography.titleSmall)
                RadioLine(stringResource(CommonR.string.requests_filter_all_statuses), state.stage == null) { viewModel.setStage(null) }
                state.stages.forEach { stage ->
                    RadioLine("${stageLabel(stage)} (${state.stageCounts[stage] ?: 0})", state.stage == stage) { viewModel.setStage(stage) }
                }
                HorizontalDivider(Modifier.padding(vertical = Spacing.Small))
            }
            Text(stringResource(R.string.requests_filter_type), style = MaterialTheme.typography.titleSmall)
            KindFilter.entries.forEach { kind -> RadioLine(kindFilterLabel(kind), state.kind == kind) { viewModel.setKind(kind) } }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = Spacing.Medium)) {
                Text(stringResource(R.string.requests_done))
            }
        }
    }
}

@Composable
fun RadioLine(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    hint: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = Spacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hint != null) Hint(hint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeclineSheet(
    ask: Ask,
    onDismiss: () -> Unit,
    onDecline: (String) -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    val quick =
        listOf(
            stringResource(R.string.requests_reason_have_it),
            stringResource(R.string.requests_reason_kids),
            stringResource(R.string.requests_reason_asked_before),
            stringResource(R.string.requests_reason_space),
        )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.requests_decline_title, titleWithYear(ask)), style = MaterialTheme.typography.titleLarge)
            Hint(stringResource(R.string.requests_decline_undo))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it.take(NOTE_MAX) },
                label = { Text(stringResource(R.string.requests_decline_reason, namesText(ask.askerNames))) },
                placeholder = { Text(stringResource(R.string.requests_decline_placeholder)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                quick.forEach { words ->
                    SuggestionChip(
                        onClick = {
                            reason =
                                if (reason.contains(words)) {
                                    reason.replace(words, "").replace(Regex("\\s*[.;,]\\s*$"), "").trim()
                                } else {
                                    listOf(reason.trim().trimEnd('.'), words).filter { it.isNotEmpty() }.joinToString(". ")
                                }.take(NOTE_MAX)
                        },
                        label = { Text(words) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Spacer(Modifier.width(Spacing.Small))
                Button(onClick = { onDecline(reason) }) { Text(stringResource(CommonR.string.requests_decline)) }
            }
        }
    }
}
