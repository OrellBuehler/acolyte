package ch.orellbuehler.acolyte.ui.downloads

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.downloads.BarTone
import ch.orellbuehler.acolyte.feature.downloads.ClientNoteKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadFormat
import ch.orellbuehler.acolyte.feature.downloads.DownloadItem
import ch.orellbuehler.acolyte.feature.downloads.DownloadRules
import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import ch.orellbuehler.acolyte.feature.downloads.DownloadsEmptyKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadsNotice
import ch.orellbuehler.acolyte.feature.downloads.DownloadsState
import ch.orellbuehler.acolyte.feature.downloads.DownloadsSub
import ch.orellbuehler.acolyte.feature.downloads.DownloadsTab
import ch.orellbuehler.acolyte.feature.downloads.KindFilter
import ch.orellbuehler.acolyte.feature.downloads.NetworkFilter
import ch.orellbuehler.acolyte.feature.downloads.NoticeKind
import ch.orellbuehler.acolyte.feature.downloads.RemovePlan
import ch.orellbuehler.acolyte.feature.downloads.SortKey
import ch.orellbuehler.acolyte.feature.downloads.SortOrder
import ch.orellbuehler.acolyte.feature.downloads.TrackerFilterKind
import ch.orellbuehler.acolyte.feature.downloads.UsenetPriority
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

internal sealed interface DownloadsSheet {
    data object Filters : DownloadsSheet

    data object Speed : DownloadsSheet

    data object Add : DownloadsSheet

    data object Matches : DownloadsSheet

    data class Detail(
        val key: String,
    ) : DownloadsSheet

    data class Remove(
        val plan: RemovePlan,
    ) : DownloadsSheet

    data class MarkFailed(
        val key: String,
        val name: String,
    ) : DownloadsSheet

    data class Priority(
        val key: String,
    ) : DownloadsSheet

    data class Steer(
        val mode: DownloadAction,
        val keys: List<String>,
    ) : DownloadsSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    show: String,
    open: String,
    onBack: (() -> Unit)?,
    onOpenTitle: (DownloadTitleLink) -> Unit,
    onOpenRoute: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "downloads-$show") { DownloadsViewModel(sdk, show) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    var sheet by remember { mutableStateOf<DownloadsSheet?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.items, open) {
        if (!opened && open.isNotEmpty() && state.item(open) != null) {
            opened = true
            sheet = DownloadsSheet.Detail(open)
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val notice = state.notice
    val noticeLine = notice?.let { noticeText(it) }
    LaunchedEffect(notice?.id) {
        if (notice != null && noticeLine != null) {
            snackbar.showSnackbar(noticeLine)
            viewModel.presenter.consumeNotice(notice.id)
        }
    }
    val actionError = state.actionError
    val errorLine =
        actionError?.let {
            errorText(it) +
                it.message
                    .takeIf { m -> m.isNotBlank() }
                    ?.let { m -> " $m" }
                    .orEmpty()
        }
    LaunchedEffect(actionError) {
        if (errorLine != null) {
            snackbar.showSnackbar(errorLine)
            viewModel.presenter.clearActionError()
        }
    }

    fun handle(
        item: DownloadItem,
        action: DownloadAction,
    ) {
        when (action) {
            DownloadAction.OPEN_TITLE -> viewModel.openTitle(item.row.titleId, onOpenTitle)
            DownloadAction.REMOVE -> viewModel.presenter.removePlan(listOf(item.key))?.let { sheet = DownloadsSheet.Remove(it) }
            DownloadAction.MARK_FAILED -> sheet = DownloadsSheet.MarkFailed(item.key, item.name)
            DownloadAction.PRIORITY -> sheet = DownloadsSheet.Priority(item.key)
            DownloadAction.SEEDING_RULES, DownloadAction.SET_LOCATION, DownloadAction.SPEED_LIMITS ->
                sheet = DownloadsSheet.Steer(action, listOf(item.key))
            else -> viewModel.act(item.key, action)
        }
    }
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            title =
                if (state.selecting) {
                    stringResource(
                        R.string.downloads_selected,
                        state.selection.size,
                    )
                } else {
                    stringResource(R.string.section_downloads)
                },
            onBack = onBack,
            actions = {
                if (state.selecting) {
                    IconButton(onClick = viewModel.presenter::selectAll) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = stringResource(R.string.downloads_select_all))
                    }
                    IconButton(onClick = viewModel.presenter::clearSelection) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.downloads_clear_selection))
                    }
                } else {
                    TopActions(state, searching, { searching = !searching }, { sheet = it }, viewModel)
                }
            },
        ) { padding ->
            StatusContent(
                status = state.status,
                isEmpty = state.isEmpty,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
                empty = {
                    if (state.manager) {
                        EmptyView(
                            title = stringResource(R.string.downloads_empty_all_title),
                            message = stringResource(R.string.downloads_empty_manager),
                            icon = Icons.Rounded.Download,
                        )
                    } else {
                        EmptyView(
                            title = stringResource(R.string.downloads_empty_watch_title),
                            message = stringResource(R.string.downloads_empty_watch),
                            icon = Icons.Rounded.Download,
                        )
                    }
                },
            ) {
                DownloadsList(
                    state = state,
                    searching = searching,
                    viewModel = viewModel,
                    onOpen = { sheet = DownloadsSheet.Detail(it.key) },
                    onAction = ::handle,
                    onSheet = { sheet = it },
                )
            }
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            SnackbarHost(snackbar)
            if (state.selecting) SelectionBar(state, viewModel) { sheet = it }
        }
    }
    when (val current = sheet) {
        null -> Unit
        is DownloadsSheet.Detail -> {
            val item = state.item(current.key)
            if (item == null) {
                LaunchedEffect(current) { sheet = null }
            } else {
                DownloadDetailSheet(
                    item,
                    state,
                    viewModel,
                    onDismiss = { sheet = null },
                    onAction = { handle(item, it) },
                    onOpenTitle = onOpenTitle,
                    onOpenRoute = onOpenRoute,
                )
            }
        }
        is DownloadsSheet.Remove -> RemoveDialog(current.plan, state.batchBusy, viewModel) { sheet = null }
        is DownloadsSheet.MarkFailed ->
            AlertDialog(
                onDismissRequest = { sheet = null },
                title = { Text(stringResource(R.string.downloads_mark_failed_title, current.name)) },
                text = { Text(stringResource(R.string.downloads_mark_failed_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.act(current.key, DownloadAction.MARK_FAILED)
                        sheet = null
                    }) { Text(stringResource(R.string.downloads_action_mark_failed), color = MaterialTheme.acolyte.failText) }
                },
                dismissButton = { TextButton(onClick = { sheet = null }) { Text(stringResource(R.string.common_cancel)) } },
            )
        is DownloadsSheet.Priority ->
            AlertDialog(
                onDismissRequest = { sheet = null },
                title = { Text(stringResource(R.string.downloads_action_priority)) },
                text = {
                    Column {
                        UsenetPriority.entries.forEach { p ->
                            TextButton(onClick = {
                                viewModel.priority(current.key, p)
                                sheet = null
                            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(p.label())) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { sheet = null }) { Text(stringResource(R.string.common_cancel)) } },
            )
        DownloadsSheet.Filters -> FiltersSheet(state, viewModel) { sheet = null }
        DownloadsSheet.Speed -> SpeedSheet(viewModel) { sheet = null }
        DownloadsSheet.Add -> AddTorrentsSheet(state, viewModel) { sheet = null }
        DownloadsSheet.Matches -> MatchesSheet(state, viewModel) { sheet = null }
        is DownloadsSheet.Steer -> SteerSheet(current.mode, current.keys, viewModel) { sheet = null }
    }
}

@Composable
private fun TopActions(
    state: DownloadsState,
    searching: Boolean,
    onSearch: () -> Unit,
    onSheet: (DownloadsSheet) -> Unit,
    viewModel: DownloadsViewModel,
) {
    IconButton(onClick = onSearch) {
        Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = stringResource(R.string.downloads_search))
    }
    val on = state.filter.filtersOn
    IconButton(onClick = { onSheet(DownloadsSheet.Filters) }) {
        BadgedBox(badge = { if (on > 0) Badge { Text(on.toString()) } }) {
            Icon(
                Icons.Rounded.FilterList,
                contentDescription =
                    if (on > 0) stringResource(R.string.downloads_filters_on, on) else stringResource(R.string.downloads_filters),
            )
        }
    }
    if (state.manager && state.addTargets.isNotEmpty()) {
        IconButton(onClick = { onSheet(DownloadsSheet.Add) }) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.downloads_add))
        }
    }
    if (state.manager) {
        var open by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.downloads_all_menu))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.downloads_select)) }, onClick = {
                    open = false
                    state.items.firstOrNull { it.selectable }?.let { viewModel.presenter.toggleSelect(it.key) }
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.downloads_start_all)) }, onClick = {
                    open = false
                    viewModel.startAll()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.downloads_pause_all)) }, onClick = {
                    open = false
                    viewModel.pauseAll()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.downloads_speed_limit_menu)) }, onClick = {
                    open = false
                    onSheet(DownloadsSheet.Speed)
                })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadsList(
    state: DownloadsState,
    searching: Boolean,
    viewModel: DownloadsViewModel,
    onOpen: (DownloadItem) -> Unit,
    onAction: (DownloadItem, DownloadAction) -> Unit,
    onSheet: (DownloadsSheet) -> Unit,
) {
    val presenter = viewModel.presenter
    LazyColumn(contentPadding = PaddingValues(bottom = if (state.selecting) 120.dp else Spacing.XLarge)) {
        if (searching || state.filter.query.isNotEmpty()) {
            item(key = "search") {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = presenter::setQuery,
                    placeholder = { Text(stringResource(R.string.downloads_search_hint)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.filter.query.isNotEmpty()) {
                            IconButton(onClick = { presenter.setQuery("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.downloads_clear_search))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                )
            }
        }
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        items(state.notes, key = { "note-${it.name}" }) { note ->
            NoticeBanner(
                stringResource(
                    when (note.kind) {
                        ClientNoteKind.WAITING -> R.string.downloads_note_client_waiting
                        ClientNoteKind.STALE -> R.string.downloads_note_client_stale
                        ClientNoteKind.OFFLINE -> R.string.downloads_note_client_offline
                    },
                    note.name,
                ),
            )
        }
        if (state.manager && state.matches.matches.isNotEmpty()) {
            item(key = "matches") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                ) {
                    Row(Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.downloads_matches_banner, state.matches.matches.size),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onSheet(DownloadsSheet.Matches) }) { Text(stringResource(R.string.downloads_matches_open)) }
                    }
                }
            }
        }
        item(key = "tabs") {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                state.tabs.forEach { count ->
                    FilterChip(
                        selected = state.filter.tab == count.tab,
                        onClick = { presenter.setTab(count.tab) },
                        label = { Text("${stringResource(count.tab.label())}  ${count.count}") },
                    )
                }
            }
        }
        if (state.subs.isNotEmpty()) {
            item(key = "subs") {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    state.subs.forEach { count ->
                        FilterChip(
                            selected = state.filter.sub == count.sub,
                            onClick = { presenter.setSub(count.sub) },
                            label = { Text("${stringResource(count.sub.label())}  ${count.count}") },
                        )
                    }
                }
            }
        }
        if (state.manager) item(key = "status") { StatusLineRow(state, onSheet, presenter::show, presenter::setTracker) }
        val empty = state.empty
        if (empty != null) {
            item(key = "empty") { InlineEmpty(state, empty.kind, empty.count, presenter::setQuery, presenter::clearFilters) }
        }
        items(state.items, key = { it.key }) { item ->
            DownloadRowCard(
                item = item,
                selecting = state.selecting,
                selected = item.key in state.selection,
                busy = item.key in state.busy,
                onClick = { if (state.selecting && item.selectable) presenter.toggleSelect(item.key) else onOpen(item) },
                onLongClick = { if (item.selectable) presenter.toggleSelect(item.key) },
                onAction = { onAction(item, it) },
            )
        }
        if (state.moreOutside > 0 && state.empty?.kind != DownloadsEmptyKind.NO_MATCHES) {
            item(key = "more") {
                OutlinedButton(
                    onClick = viewModel::showMore,
                    enabled = !state.loadingMore,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Screen),
                ) { Text(stringResource(R.string.downloads_show_more, state.moreOutside)) }
            }
        }
    }
}

@Composable
private fun StatusLineRow(
    state: DownloadsState,
    onSheet: (DownloadsSheet) -> Unit,
    onShow: (String) -> Unit,
    onTracker: (String) -> Unit,
) {
    val line = state.statusLine
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val parts = mutableListOf(stringResource(R.string.downloads_speed_down, speedText(line.speed)))
        if (line.free > 0) parts += stringResource(R.string.downloads_status_free, DownloadFormat.freeSize(line.free))
        if (line.left > 0) parts += stringResource(R.string.downloads_time_left, durationText(line.left))
        TextButton(onClick = { onSheet(DownloadsSheet.Speed) }) {
            Text(
                parts.joinToString(" · ") + if (line.limited) " · " + stringResource(R.string.downloads_status_limited) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (line.review > 0) {
            AssistChip(
                onClick = { onShow("problems.decide") },
                label = { Text(stringResource(R.string.downloads_status_review, line.review), color = MaterialTheme.acolyte.failText) },
            )
        }
        if (state.trackerIssues > 0) {
            FilterChip(
                selected = state.filter.tracker == "p:any",
                onClick = { onTracker(if (state.filter.tracker == "p:any") "" else "p:any") },
                label = { Text(stringResource(R.string.downloads_tracker_issues, state.trackerIssues)) },
            )
        }
    }
}

@Composable
private fun InlineEmpty(
    state: DownloadsState,
    kind: DownloadsEmptyKind,
    count: Int,
    onQuery: (String) -> Unit,
    onClear: () -> Unit,
) {
    val f = state.filter
    val (title, message) =
        when (kind) {
            DownloadsEmptyKind.NO_MATCHES -> stringResource(R.string.downloads_empty_matches, f.query) to null
            DownloadsEmptyKind.NO_KIND ->
                stringResource(
                    when (f.kind) {
                        KindFilter.SHOW -> R.string.downloads_empty_kind_show
                        KindFilter.MUSIC -> R.string.downloads_empty_kind_music
                        else -> R.string.downloads_empty_kind_movie
                    },
                ) to null
            DownloadsEmptyKind.NO_NETWORK ->
                stringResource(
                    if (f.network ==
                        NetworkFilter.USENET
                    ) {
                        R.string.downloads_empty_network_usenet
                    } else {
                        R.string.downloads_empty_network_torrent
                    },
                ) to null
            DownloadsEmptyKind.LOADING_MORE ->
                stringResource(R.string.downloads_empty_loading_title) to stringResource(R.string.downloads_empty_loading, count)
            DownloadsEmptyKind.SUB -> stringResource(subEmpty(f.sub)) to null
            DownloadsEmptyKind.NOTHING, DownloadsEmptyKind.TAB -> {
                val (t, m) = tabEmpty(f.tab)
                stringResource(t) to stringResource(m)
            }
        }
    val action =
        when (kind) {
            DownloadsEmptyKind.NO_MATCHES -> stringResource(R.string.downloads_clear_search) to { onQuery("") }
            DownloadsEmptyKind.NO_KIND, DownloadsEmptyKind.NO_NETWORK -> stringResource(R.string.downloads_empty_show_all) to onClear
            else -> null
        }
    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.XLarge)) {
        MessageView(
            icon = Icons.Rounded.Download,
            title = title,
            message = message,
            action = action?.first,
            onAction = action?.second,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun subEmpty(sub: DownloadsSub?): Int =
    when (sub) {
        DownloadsSub.QUEUED -> R.string.downloads_empty_sub_queued
        DownloadsSub.PAUSED -> R.string.downloads_empty_sub_paused
        DownloadsSub.DOWNLOADING -> R.string.downloads_empty_sub_downloading
        DownloadsSub.CHECKING -> R.string.downloads_empty_sub_checking
        DownloadsSub.IMPORTING -> R.string.downloads_empty_sub_importing
        DownloadsSub.FAILED -> R.string.downloads_empty_sub_failed
        DownloadsSub.DECIDE, null -> R.string.downloads_empty_sub_decide
    }

private fun tabEmpty(tab: DownloadsTab): Pair<Int, Int> =
    when (tab) {
        DownloadsTab.ALL -> R.string.downloads_empty_all_title to R.string.downloads_empty_all
        DownloadsTab.WAITING -> R.string.downloads_empty_waiting_title to R.string.downloads_empty_waiting
        DownloadsTab.PROGRESS -> R.string.downloads_empty_progress_title to R.string.downloads_empty_progress
        DownloadsTab.PROBLEMS -> R.string.downloads_empty_problems_title to R.string.downloads_empty_problems
        DownloadsTab.DONE -> R.string.downloads_empty_done_title to R.string.downloads_empty_done
    }

@Composable
internal fun barColor(tone: BarTone): Color {
    val c = MaterialTheme.acolyte
    return when (tone) {
        BarTone.GO, BarTone.DONE -> c.ok
        BarTone.QUEUED -> c.info
        BarTone.HELD -> c.wait
        BarTone.BAD -> c.idle
    }
}

@Composable
internal fun toneColor(tone: Tone): Color = MaterialTheme.acolyte.text(tone) ?: MaterialTheme.acolyte.dot(tone)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRowCard(
    item: DownloadItem,
    selecting: Boolean,
    selected: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (DownloadAction) -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) Checkbox(checked = selected, onCheckedChange = { onClick() }, enabled = item.selectable)
        val shape = RoundedCornerShape(Radius.Poster / 2)
        ServerImage(
            source = item.poster,
            contentDescription = null,
            placeholderName = item.title,
            modifier = (if (item.square) Modifier.size(48.dp) else Modifier.size(40.dp, 60.dp)).clip(shape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = subLineText(item.subLine)
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(stringResource(item.word.label()), style = MaterialTheme.typography.labelMedium, color = toneColor(item.tone))
                val problem = item.problem
                val note = item.note
                val second =
                    when {
                        problem != null -> stringResource(problem.kind.label())
                        note != null -> noteText(note)
                        else -> ""
                    }
                if (second.isNotEmpty()) {
                    Text(
                        "· $second",
                        style = MaterialTheme.typography.labelMedium,
                        color = problem?.let { toneColor(it.tone) } ?: muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { item.progress.toFloat() },
                color = barColor(item.bar),
                trackColor = MaterialTheme.colorScheme.outline,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            )
            val facts = mutableListOf<String>()
            if (item.row.size > 0) facts += DownloadFormat.size(item.row.size)
            if (!item.history) facts += "${DownloadFormat.percent(item.progress)}%"
            if (item.moving && item.row.speed >= 1000) {
                facts += stringResource(R.string.downloads_speed_down, speedText(item.row.speed))
                if (item.row.upSpeed > 0) facts += stringResource(R.string.downloads_speed_up, speedText(item.row.upSpeed))
                if (item.row.eta > 0) facts += stringResource(R.string.downloads_time_left, durationText(item.row.eta))
            }
            if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1)
        }
        if (item.actions.isNotEmpty() && !selecting) RowMenu(item, busy, onAction)
    }
}

@Composable
private fun RowMenu(
    item: DownloadItem,
    busy: Boolean,
    onAction: (DownloadAction) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, enabled = !busy) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.downloads_more_actions))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            item.actions.forEach { action ->
                if (action == DownloadAction.RECHECK || action == DownloadAction.MARK_FAILED) HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(action.label()),
                            color =
                                if (action == DownloadAction.REMOVE ||
                                    action == DownloadAction.MARK_FAILED
                                ) {
                                    MaterialTheme.acolyte.failText
                                } else {
                                    Color.Unspecified
                                },
                        )
                    },
                    onClick = {
                        open = false
                        onAction(action)
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectionBar(
    state: DownloadsState,
    viewModel: DownloadsViewModel,
    onSheet: (DownloadsSheet) -> Unit,
) {
    val actions = remember(state.selection, state.items) { viewModel.presenter.selectionActions() }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).navigationBarsPadding().padding(Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            actions.forEach { action ->
                val enabled = !state.batchBusy
                val onClick = {
                    when (action) {
                        DownloadAction.REMOVE ->
                            viewModel.presenter
                                .removePlan(
                                    state.selection.toList(),
                                )?.let { onSheet(DownloadsSheet.Remove(it)) }
                        DownloadAction.SEEDING_RULES, DownloadAction.SET_LOCATION, DownloadAction.SPEED_LIMITS ->
                            onSheet(DownloadsSheet.Steer(action, state.selection.toList()))
                        else -> viewModel.bulk(action)
                    }
                    Unit
                }
                if (action == DownloadAction.REMOVE) {
                    Button(onClick = onClick, enabled = enabled) { Text(stringResource(action.label())) }
                } else {
                    OutlinedButton(onClick = onClick, enabled = enabled) { Text(stringResource(action.label())) }
                }
            }
        }
    }
}

@Composable
internal fun noticeText(notice: DownloadsNotice): String {
    val o = notice.outcome
    return when (notice.kind) {
        NoticeKind.PAUSED -> stringResource(R.string.downloads_notice_paused)
        NoticeKind.STARTED -> stringResource(R.string.downloads_notice_started)
        NoticeKind.RETRIED -> stringResource(R.string.downloads_notice_retried)
        NoticeKind.MOVED_TOP -> stringResource(R.string.downloads_notice_moved_top)
        NoticeKind.MOVED_BOTTOM -> stringResource(R.string.downloads_notice_moved_bottom)
        NoticeKind.MOVED_UP -> stringResource(R.string.downloads_notice_moved_up)
        NoticeKind.MOVED_DOWN -> stringResource(R.string.downloads_notice_moved_down)
        NoticeKind.PRIORITY_SET -> stringResource(R.string.downloads_notice_priority)
        NoticeKind.CHECKING -> stringResource(R.string.downloads_notice_checking)
        NoticeKind.ANNOUNCED -> stringResource(R.string.downloads_notice_announced)
        NoticeKind.FORCE_STARTED -> stringResource(R.string.downloads_notice_force)
        NoticeKind.IMPORT_QUEUED -> stringResource(R.string.downloads_notice_import_queued)
        NoticeKind.REMOVED -> stringResource(R.string.downloads_notice_removed, notice.count)
        NoticeKind.REMOVED_KEPT -> stringResource(R.string.downloads_notice_removed_kept, notice.text)
        NoticeKind.MARKED_FAILED -> stringResource(R.string.downloads_notice_marked_failed)
        NoticeKind.ALL_STARTED -> stringResource(R.string.downloads_notice_all_started)
        NoticeKind.ALL_PAUSED -> stringResource(R.string.downloads_notice_all_paused)
        NoticeKind.ALL_FAILED -> stringResource(R.string.downloads_notice_all_failed, notice.text)
        NoticeKind.SPEED_SAVED -> stringResource(R.string.downloads_notice_speed_saved)
        NoticeKind.SEEDING_SET -> stringResource(R.string.downloads_notice_seeding_set)
        NoticeKind.MOVING -> stringResource(R.string.downloads_notice_moving)
        NoticeKind.SPEEDS_SET -> stringResource(R.string.downloads_notice_speeds_set)
        NoticeKind.SEARCHING -> stringResource(R.string.downloads_notice_searching, notice.text)
        NoticeKind.TURNED_DOWN -> stringResource(R.string.downloads_notice_turned_down, notice.text)
        NoticeKind.STOPPED -> stringResource(R.string.downloads_notice_stopped, notice.text)
        NoticeKind.IMPORTED -> stringResource(R.string.downloads_notice_imported, notice.text)
        NoticeKind.EDITION_SET -> stringResource(R.string.downloads_notice_edition_set)
        NoticeKind.MATCHING -> stringResource(R.string.downloads_notice_matching)
        NoticeKind.MATCHED -> stringResource(R.string.downloads_notice_matched)
        NoticeKind.DOWNLOAD_FAILED ->
            if (notice.text.isBlank()) {
                stringResource(R.string.downloads_notice_download_failed_plain)
            } else {
                stringResource(R.string.downloads_notice_download_failed, notice.text)
            }
        NoticeKind.BULK_DONE ->
            if (notice.skipped > 0) {
                stringResource(R.string.downloads_notice_bulk_skipped, notice.count, notice.skipped)
            } else {
                stringResource(R.string.downloads_notice_bulk, notice.count)
            }
        NoticeKind.NOT_HERE -> stringResource(R.string.downloads_notice_not_here)
        NoticeKind.ADDED ->
            if (o == null) {
                stringResource(R.string.downloads_add_nothing)
            } else {
                val parts = mutableListOf<String>()
                if (o.added > 0) parts += stringResource(R.string.downloads_add_added, o.added)
                if (o.pending > 0) parts += stringResource(R.string.downloads_add_pending, o.pending)
                if (o.duplicates > 0) parts += stringResource(R.string.downloads_add_duplicates, o.duplicates)
                if (o.failed > 0) parts += stringResource(R.string.downloads_add_failed, o.failed)
                if (o.bad.isNotEmpty()) parts += stringResource(R.string.downloads_add_bad, o.bad.joinToString(", "))
                if (o.dropped > 0) parts += stringResource(R.string.downloads_add_dropped, o.dropped)
                if (parts.isEmpty()) stringResource(R.string.downloads_add_nothing) else parts.joinToString(" · ")
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(
    state: DownloadsState,
    viewModel: DownloadsViewModel,
    onDismiss: () -> Unit,
) {
    val p = viewModel.presenter
    val f = state.filter
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(stringResource(R.string.downloads_filters), style = MaterialTheme.typography.titleLarge)
            ChipGroup(stringResource(R.string.downloads_filter_library)) {
                listOf(
                    KindFilter.ALL to R.string.downloads_filter_everything,
                    KindFilter.MOVIE to R.string.downloads_filter_movies,
                    KindFilter.SHOW to R.string.downloads_filter_shows,
                    KindFilter.MUSIC to R.string.downloads_filter_music,
                ).forEach { (k, label) ->
                    FilterChip(selected = f.kind == k, onClick = { p.setKind(k) }, label = { Text(stringResource(label)) })
                }
            }
            ChipGroup(stringResource(R.string.downloads_filter_network)) {
                listOf(
                    NetworkFilter.ANY to R.string.downloads_filter_any_network,
                    NetworkFilter.USENET to R.string.downloads_filter_usenet,
                    NetworkFilter.TORRENT to R.string.downloads_filter_torrent,
                ).forEach { (k, label) ->
                    FilterChip(selected = f.network == k, onClick = { p.setNetwork(k) }, label = { Text(stringResource(label)) })
                }
            }
            if (state.trackers.isNotEmpty()) {
                ChipGroup(stringResource(R.string.downloads_filter_trackers)) {
                    FilterChip(selected = f.tracker.isEmpty(), onClick = {
                        p.setTracker("")
                    }, label = { Text(stringResource(R.string.downloads_filter_all_trackers)) })
                    FilterChip(
                        selected = f.tracker == "p:any",
                        onClick = { p.setTracker("p:any") },
                        label = { Text(stringResource(R.string.downloads_filter_tracker_problems)) },
                    )
                    state.trackers.forEach { t ->
                        val label =
                            when (t.kind) {
                                TrackerFilterKind.HOST -> t.label
                                TrackerFilterKind.ERROR -> stringResource(R.string.downloads_problem_tracker_error)
                                TrackerFilterKind.WARNING -> stringResource(R.string.downloads_problem_tracker_warning)
                                TrackerFilterKind.ANNOUNCE -> stringResource(R.string.downloads_problem_no_tracker_reply)
                            }
                        FilterChip(selected = f.tracker == t.key, onClick = { p.setTracker(t.key) }, label = { Text("$label  ${t.count}") })
                    }
                }
            }
            ChipGroup(stringResource(R.string.downloads_sort)) {
                FilterChip(
                    selected = f.sort == null,
                    onClick = { p.setSort(null) },
                    label = { Text(stringResource(R.string.downloads_sort_default)) },
                )
                val sort = f.sort
                SortKey.entries.forEach { key ->
                    val current = sort?.takeIf { it.key == key }
                    FilterChip(
                        selected = current != null,
                        onClick = { p.setSort(SortOrder(key, current?.descending?.not() ?: DownloadRules.firstDescending(key))) },
                        label = {
                            Text(
                                stringResource(key.label()) +
                                    if (current == null) {
                                        ""
                                    } else if (current.descending) {
                                        " ↓"
                                    } else {
                                        " ↑"
                                    },
                            )
                        },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = p::clearFilters) { Text(stringResource(R.string.downloads_filter_clear)) }
                Button(onClick = onDismiss) { Text(stringResource(R.string.downloads_filter_show, state.items.size)) }
            }
        }
    }
}

@Composable
internal fun ChipGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) { content() }
    }
}
