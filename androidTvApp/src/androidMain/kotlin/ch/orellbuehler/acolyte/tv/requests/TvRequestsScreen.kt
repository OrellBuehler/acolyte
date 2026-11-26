package ch.orellbuehler.acolyte.tv.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.KindFilter
import ch.orellbuehler.acolyte.feature.requests.PendingKind
import ch.orellbuehler.acolyte.feature.requests.RequestsState
import ch.orellbuehler.acolyte.feature.requests.UNDO_MILLIS
import ch.orellbuehler.acolyte.feature.requests.isFailed
import ch.orellbuehler.acolyte.feature.requests.life
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvBackdropLayer
import ch.orellbuehler.acolyte.tv.components.TvBadgeLabel
import ch.orellbuehler.acolyte.tv.components.TvChip
import ch.orellbuehler.acolyte.tv.components.TvChipRow
import ch.orellbuehler.acolyte.tv.components.TvConfirmDialog
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvMenuAction
import ch.orellbuehler.acolyte.tv.components.TvMenuDialog
import ch.orellbuehler.acolyte.tv.components.TvNoteBanner
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvStatusChip
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.TvThumbCard
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.components.tvThumbRows
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.nav.TvSections
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.AskLineKind
import ch.orellbuehler.acolyte.ui.requests.askLines
import ch.orellbuehler.acolyte.ui.requests.calmWords
import ch.orellbuehler.acolyte.ui.requests.factsLine
import ch.orellbuehler.acolyte.ui.requests.failed
import ch.orellbuehler.acolyte.ui.requests.kindFilterLabel
import ch.orellbuehler.acolyte.ui.requests.limitText
import ch.orellbuehler.acolyte.ui.requests.memberStatus
import ch.orellbuehler.acolyte.ui.requests.requestsNoteText
import ch.orellbuehler.acolyte.ui.requests.stageLabel
import ch.orellbuehler.acolyte.ui.requests.titleWithYear
import kotlinx.coroutines.delay
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun TvRequestsScreen(account: Account) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { TvRequestsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    DisposableEffect(viewModel) { onDispose { viewModel.flush() } }
    TvRequestsContent(state, viewModel, canSearch = TvSections.visible(Section.SEARCH, account))
}

@Composable
fun TvRequestsContent(
    state: RequestsState,
    actions: TvRequestsActions,
    canSearch: Boolean,
) {
    val navigator = LocalTvNavigator.current
    var ambient by remember { mutableStateOf<String?>(null) }
    var acting by remember { mutableStateOf<Long?>(null) }
    var confirmAll by remember { mutableStateOf(false) }
    val note = state.note
    LaunchedEffect(note?.id) {
        if (note == null) return@LaunchedEffect
        delay(if (note.undoable) UNDO_MILLIS else NOTE_MILLIS)
        actions.dismissNote()
    }
    Box(Modifier.fillMaxSize()) {
        TvBackdropLayer(ambient, blurred = true)
        TvStatusContent(
            status = state.status,
            isEmpty = state.isEmpty,
            reload = actions::refresh,
            empty = { Empty(state, canSearch) },
        ) {
            RequestsList(
                state,
                actions,
                canSearch,
                onFocusPoster = { ambient = it },
                onTap = { ask ->
                    if (tvAskNeedsMenu(ask, state)) {
                        acting = ask.id
                    } else if (ask.canOpen()) {
                        navigator.openTitle(ask.kind, ask.tmdbId)
                    }
                },
                onApproveAll = { confirmAll = true },
            )
        }
        if (note != null) {
            TvNoteBanner(
                text = requestsNoteText(note),
                failed = note.failed,
                onUndo = if (note.undoable) ({ actions.undo(note.keys) }) else null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = TvMetrics.marginX, vertical = TvMetrics.marginY),
            )
        }
    }
    acting?.let { id ->
        val ask = state.ask(id)
        if (ask == null) {
            acting = null
        } else {
            TvMenuDialog(
                title = titleWithYear(ask),
                actions = askMenu(ask, state, actions) { navigator.openTitle(ask.kind, ask.tmdbId) },
                onDismiss = { acting = null },
            )
        }
    }
    if (confirmAll) {
        TvConfirmDialog(
            title = stringResource(CommonR.string.requests_approve_all_title, state.waitingToApprove),
            message = if (state.isCut) stringResource(CommonR.string.requests_approve_all_cut) else "",
            confirm = stringResource(CommonR.string.requests_approve_n, state.waitingToApprove),
            cancel = stringResource(R.string.tv_common_cancel),
            onConfirm = {
                confirmAll = false
                actions.approveAll()
            },
            onDismiss = { confirmAll = false },
        )
    }
}

private const val NOTE_MILLIS = 4000L

@Composable
private fun askMenu(
    ask: Ask,
    state: RequestsState,
    actions: TvRequestsActions,
    open: () -> Unit,
): List<TvMenuAction> {
    val labels =
        tvAskActions(ask, state).map { action ->
            when (action) {
                TvAskAction.OPEN -> TvMenuAction("open", stringResource(CommonR.string.requests_open_title), onClick = open)
                TvAskAction.APPROVE ->
                    TvMenuAction("approve", stringResource(CommonR.string.requests_approve)) { actions.approve(ask.id) }
                TvAskAction.DECLINE ->
                    TvMenuAction("decline", stringResource(CommonR.string.requests_decline)) { actions.decline(ask.id) }
                TvAskAction.TAKE_BACK ->
                    TvMenuAction("take-back", stringResource(CommonR.string.requests_take_back)) { actions.takeBack(ask.id) }
                TvAskAction.UNDO -> TvMenuAction("undo", stringResource(CommonR.string.requests_undo)) { actions.undo(listOf(ask.id)) }
            }
        }
    return labels
}

@Composable
private fun Empty(
    state: RequestsState,
    canSearch: Boolean,
) {
    val navigator = LocalTvNavigator.current
    TvEmptyView(
        title = stringResource(CommonR.string.requests_empty_title),
        message = stringResource(if (state.canAsk) CommonR.string.requests_empty_message else CommonR.string.requests_empty_cannot_ask),
        icon = Icons.Rounded.Checklist,
        modifier = Modifier.fillMaxWidth(),
        actions = {
            if (state.canAsk && !state.approver && canSearch) {
                TvActionButton(
                    stringResource(CommonR.string.requests_ask_something),
                    { navigator.openSection(Section.SEARCH) },
                    icon = Icons.Rounded.Search,
                    prominent = true,
                )
            }
        },
    )
}

@Composable
private fun RequestsList(
    state: RequestsState,
    actions: TvRequestsActions,
    canSearch: Boolean,
    onFocusPoster: (String?) -> Unit,
    onTap: (Ask) -> Unit,
    onApproveAll: () -> Unit,
) {
    val navigator = LocalTvNavigator.current
    LazyColumn(
        contentPadding = PaddingValues(top = TvMetrics.focusPad, bottom = TvMetrics.marginY * 4),
        verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap),
    ) {
        state.status.error?.let { error ->
            item(key = "error") { TvNoticeBanner(errorText(error), Modifier.tvPageInset()) }
        }
        if (!state.approver && (state.limits.isNotEmpty() || (state.canAsk && canSearch))) {
            item(key = "member") {
                Column(Modifier.tvPageInset(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.limits.forEach { line -> Text(limitText(line), style = TvType.footnote, color = TvColors.textMuted) }
                    if (state.canAsk && canSearch) {
                        TvActionButton(
                            stringResource(CommonR.string.requests_ask_something),
                            { navigator.openSection(Section.SEARCH) },
                            Modifier.padding(vertical = TvMetrics.focusPad),
                            icon = Icons.Rounded.Search,
                            prominent = true,
                        )
                    }
                }
            }
        }
        item(key = "tabs") {
            TvChipRow {
                if (state.approver) {
                    items(ApproverTab.entries, key = { it.name }) { tab ->
                        TvChip(tvApproverTabTitle(tab, state), tab == state.approverTab, { actions.setApproverTab(tab) })
                    }
                } else {
                    items(state.memberTabs, key = { it.name }) { tab ->
                        TvChip(tvMemberTabTitle(tab, state), tab == state.memberTab, { actions.setMemberTab(tab) })
                    }
                }
            }
        }
        val kinds = if (state.approver) KindFilter.entries else state.kinds
        if (kinds.isNotEmpty()) {
            item(key = "kinds") {
                TvChipRow {
                    items(kinds, key = { it.name }) { kind -> TvChip(kindFilterLabel(kind), kind == state.kind, { actions.setKind(kind) }) }
                }
            }
        }
        if (state.approver && state.stages.isNotEmpty()) {
            item(key = "stages") {
                TvChipRow {
                    item(key = "all") {
                        TvChip(stringResource(CommonR.string.requests_filter_all_statuses), state.stage == null, { actions.setStage(null) })
                    }
                    items(state.stages, key = { it.name }) { stage ->
                        TvChip(
                            "${stageLabel(stage)} (${state.stageCounts[stage] ?: 0})",
                            state.stage == stage,
                            { actions.setStage(stage) },
                        )
                    }
                }
            }
        }
        if (state.canApproveAll) {
            item(key = "bulk") {
                TvActionButton(
                    stringResource(CommonR.string.requests_approve_all, state.waitingToApprove),
                    onApproveAll,
                    Modifier.tvPageInset().padding(vertical = TvMetrics.focusPad),
                    icon = Icons.Rounded.CheckCircle,
                    enabled = !state.busy,
                )
            }
        }
        if (state.shown.isEmpty()) item(key = "calm") { Calm(state, actions) }
        tvThumbRows(state.shown, key = { it.id }) { ask, modifier -> RequestCard(ask, state, onFocusPoster, onTap, modifier) }
        if (state.isCut) {
            item(key = "cut") {
                Column(Modifier.tvPageInset(), verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap)) {
                    Text(
                        stringResource(CommonR.string.requests_showing, state.asks.size, state.total),
                        style = TvType.footnote,
                        color = TvColors.textMuted,
                    )
                    TvActionButton(
                        stringResource(R.string.tv_requests_more),
                        actions::loadMore,
                        Modifier.padding(vertical = TvMetrics.focusPad),
                        icon = Icons.Rounded.ArrowDownward,
                        enabled = !state.loadingMore,
                    )
                }
            }
        }
    }
}

@Composable
private fun Calm(
    state: RequestsState,
    actions: TvRequestsActions,
) {
    val words = calmWords(state)
    Column(
        Modifier.fillMaxWidth().padding(vertical = TvMetrics.sectionGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(words.title, style = TvType.headline, color = TvColors.text, textAlign = TextAlign.Center)
        words.message?.let { Text(it, style = TvType.footnote, color = TvColors.textMuted, textAlign = TextAlign.Center) }
        if (state.approver && state.filtersOn > 0) {
            TvActionButton(
                stringResource(CommonR.string.requests_show_all),
                actions::clearFilters,
                Modifier.padding(top = TvMetrics.focusPad),
                icon = Icons.Rounded.FilterList,
            )
        }
    }
}

@Composable
private fun RequestCard(
    ask: Ask,
    state: RequestsState,
    onFocusPoster: (String?) -> Unit,
    onTap: (Ask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val life = ask.life
    val pending = state.pending[ask.id]
    TvThumbCard(
        image = ask.poster.ifEmpty { null },
        name = ask.name,
        onClick = { onTap(ask) },
        modifier = modifier,
        onFocus = { onFocusPoster(ask.poster.ifEmpty { null }) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                ask.name,
                style = TvType.calloutBold,
                color = TvColors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (ask.id in state.newIds) TvBadgeLabel(stringResource(CommonR.string.requests_new))
        }
        Text(factsLine(ask), style = TvType.footnote, color = TvColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        TvStatusChip(if (state.approver) stageLabel(life.stage) else memberStatus(ask), life.tone)
        askLines(ask, state.approver, state.myId).forEach { line ->
            when (line.kind) {
                AskLineKind.MUTED ->
                    Text(
                        line.text,
                        style = TvType.footnote,
                        color = TvColors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                AskLineKind.NOTE ->
                    Text(
                        line.text,
                        style = TvType.footnote,
                        fontStyle = FontStyle.Italic,
                        color = TvColors.text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                AskLineKind.FAIL ->
                    Text(
                        line.text,
                        style = TvType.footnote,
                        color = TvColors.status.failText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
            }
        }
        if (pending != null) {
            Text(
                stringResource(
                    if (pending ==
                        PendingKind.APPROVE
                    ) {
                        CommonR.string.requests_sending_approve
                    } else {
                        CommonR.string.requests_sending_decline
                    },
                ),
                style = TvType.footnote,
                color = TvColors.textMuted,
            )
        } else if (ask.state == "approved" && !ask.isFailed) {
            LinearProgressIndicator(
                progress = { life.progress.coerceIn(0f, 1f) },
                color = TvColors.dot(life.tone),
                trackColor = TvColors.surfaceHigh,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().height(3.dp),
            )
        }
    }
}
