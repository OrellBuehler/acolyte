package ch.orellbuehler.acolyte.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.Decision
import ch.orellbuehler.acolyte.api.model.TrackerInfo
import ch.orellbuehler.acolyte.feature.downloads.DecideAction
import ch.orellbuehler.acolyte.feature.downloads.DecisionGroup
import ch.orellbuehler.acolyte.feature.downloads.DecisionHint
import ch.orellbuehler.acolyte.feature.downloads.DecisionRules
import ch.orellbuehler.acolyte.feature.downloads.DecisionStep
import ch.orellbuehler.acolyte.feature.downloads.DetailNotice
import ch.orellbuehler.acolyte.feature.downloads.DetailTab
import ch.orellbuehler.acolyte.feature.downloads.DownloadAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadDetailPresenter
import ch.orellbuehler.acolyte.feature.downloads.DownloadDetailState
import ch.orellbuehler.acolyte.feature.downloads.DownloadFormat
import ch.orellbuehler.acolyte.feature.downloads.DownloadItem
import ch.orellbuehler.acolyte.feature.downloads.DownloadRules
import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import ch.orellbuehler.acolyte.feature.downloads.DownloadsState
import ch.orellbuehler.acolyte.feature.downloads.FactKind
import ch.orellbuehler.acolyte.feature.downloads.InfoFact
import ch.orellbuehler.acolyte.feature.downloads.InfoGroupKind
import ch.orellbuehler.acolyte.feature.downloads.ReasonLine
import ch.orellbuehler.acolyte.feature.downloads.ReasonLineKind
import ch.orellbuehler.acolyte.feature.downloads.WhyKind
import ch.orellbuehler.acolyte.feature.downloads.groupDecisions
import ch.orellbuehler.acolyte.nav.LibraryTool
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.LibraryImportFilesRoute
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch
import kotlin.time.Clock

private val BUTTONS =
    setOf(DownloadAction.PAUSE, DownloadAction.RESUME, DownloadAction.RETRY, DownloadAction.OPEN_TITLE, DownloadAction.REMOVE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadDetailSheet(
    item: DownloadItem,
    list: DownloadsState,
    viewModel: DownloadsViewModel,
    onDismiss: () -> Unit,
    onAction: (DownloadAction) -> Unit,
    onOpenTitle: (DownloadTitleLink) -> Unit,
    onOpenRoute: (Any) -> Unit,
) {
    val presenter = remember(item.key) { viewModel.detail(item.row) }
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(item.row) { presenter.update(item.row) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(presenter, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { presenter.watch() } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                ServerImage(
                    source = item.poster,
                    contentDescription = null,
                    placeholderName = item.title,
                    modifier =
                        (if (item.square) Modifier.size(64.dp) else Modifier.size(56.dp, 84.dp)).clip(
                            RoundedCornerShape(
                                Radius.Poster / 2,
                            ),
                        ),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    val sub = subLineText(item.subLine)
                    if (sub.isNotEmpty()) {
                        Text(
                            sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val health = mutableListOf(stringResource(item.word.label()))
                    if (!item.history) health += "${DownloadFormat.percent(item.progress)}%"
                    if (item.moving) health += speedText(item.row.speed)
                    if (item.moving && item.row.eta > 0) health += stringResource(R.string.downloads_time_left, durationText(item.row.eta))
                    Text(health.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = toneColor(item.tone))
                    item.problem?.let {
                        Text(
                            stringResource(it.kind.label()),
                            style = MaterialTheme.typography.labelMedium,
                            color = toneColor(it.tone),
                        )
                    }
                    item.detail?.let {
                        Text(
                            detailText(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!item.history) {
                        LinearProgressIndicator(
                            progress = { item.progress.toFloat() },
                            color = barColor(item.bar),
                            trackColor = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
            }
            if (item.actions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                    FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        item.actions.filter { it in BUTTONS }.forEach { action ->
                            OutlinedButton(onClick = { onAction(action) }, enabled = item.key !in list.busy) {
                                Text(
                                    stringResource(action.label()),
                                    color = if (action == DownloadAction.REMOVE) MaterialTheme.acolyte.failText else Color.Unspecified,
                                )
                            }
                        }
                    }
                    val rest = item.actions.filter { it !in BUTTONS }
                    if (rest.isNotEmpty()) {
                        var open by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { open = true },
                            ) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.downloads_more_actions)) }
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                rest.forEach { action ->
                                    DropdownMenuItem(text = { Text(stringResource(action.label())) }, onClick = {
                                        open = false
                                        onAction(action)
                                    })
                                }
                            }
                        }
                    }
                }
            }
            if (item.decisions.isNotEmpty()) DecisionPanel(item.decisions, list, viewModel, onOpenTitle, onOpenRoute)
            DetailTabs(state, presenter) { password, code ->
                val ok = viewModel.reauth(password, code)
                if (ok) presenter.clearActionError()
                ok
            }
        }
    }
}

@Composable
private fun DetailTabs(
    state: DownloadDetailState,
    presenter: DownloadDetailPresenter,
    onReauth: suspend (String, String) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    val tabs = state.tabs
    val index = tabs.indexOf(state.tab).coerceAtLeast(0)
    PrimaryScrollableTabRow(selectedTabIndex = index, edgePadding = 0.dp, containerColor = Color.Transparent) {
        tabs.forEachIndexed { i, tab ->
            Tab(selected = i == index, onClick = {
                presenter.setTab(tab)
                scope.launch { presenter.refresh() }
            }, text = { Text(stringResource(tab.label())) })
        }
    }
    if (state.readFailed) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.downloads_detail_read_failed),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.acolyte.waitText,
            )
            TextButton(onClick = { scope.launch { presenter.refresh() } }) { Text(stringResource(R.string.common_retry)) }
        }
    }
    if (!state.live && state.tab != DetailTab.INFO) {
        Text(stringResource(R.string.downloads_detail_watch), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    if (state.reading && state.tab != DetailTab.INFO) {
        Text(stringResource(R.string.downloads_detail_reading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    when (state.tab) {
        DetailTab.INFO -> InfoTab(state)
        DetailTab.FILES -> FilesTab(state, presenter, onReauth)
        DetailTab.PEERS -> PeersTab(state)
        DetailTab.TRACKERS -> TrackersTab(state, presenter)
        DetailTab.SERVERS -> ServersTab(state)
        DetailTab.LOG -> LogTab(state)
    }
    val notice = state.notice
    if (notice != null) {
        val text =
            when (notice) {
                DetailNotice.FILES_SET -> stringResource(R.string.downloads_files_set)
                DetailNotice.TRACKER_SAVED -> stringResource(R.string.downloads_tracker_saved)
                DetailNotice.TRACKER_REMOVED -> stringResource(R.string.downloads_tracker_removed)
                DetailNotice.TRACKERS_ADDED -> stringResource(R.string.downloads_trackers_added)
            }
        Text(text, color = MaterialTheme.acolyte.okText, style = MaterialTheme.typography.bodySmall)
        LaunchedEffect(notice) {
            kotlinx.coroutines.delay(4000)
            presenter.consumeNotice()
        }
    }
    val error = state.actionError
    if (error != null && !state.needsReauth) {
        Text(
            ch.orellbuehler.acolyte.ui.components
                .errorText(error) + " " + error.message,
            color = MaterialTheme.acolyte.failText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun DetailTab.label(): Int =
    when (this) {
        DetailTab.INFO -> R.string.downloads_detail_tab_info
        DetailTab.FILES -> R.string.downloads_detail_tab_files
        DetailTab.PEERS -> R.string.downloads_detail_tab_peers
        DetailTab.TRACKERS -> R.string.downloads_detail_tab_trackers
        DetailTab.SERVERS -> R.string.downloads_detail_tab_servers
        DetailTab.LOG -> R.string.downloads_detail_tab_log
    }

private fun InfoGroupKind.label(): Int =
    when (this) {
        InfoGroupKind.TRANSFER -> R.string.downloads_group_transfer
        InfoGroupKind.SWARM -> R.string.downloads_group_swarm
        InfoGroupKind.DATES -> R.string.downloads_group_dates
        InfoGroupKind.RULES -> R.string.downloads_group_rules
        InfoGroupKind.WHERE -> R.string.downloads_group_where
    }

private fun FactKind.label(): Int =
    when (this) {
        FactKind.STATUS -> R.string.downloads_fact_status
        FactKind.DOWNLOADED -> R.string.downloads_fact_downloaded
        FactKind.SIZE -> R.string.downloads_fact_size
        FactKind.DOWN -> R.string.downloads_fact_down
        FactKind.UP -> R.string.downloads_fact_up
        FactKind.TIME_LEFT -> R.string.downloads_fact_time_left
        FactKind.UPLOADED -> R.string.downloads_fact_uploaded
        FactKind.RATIO -> R.string.downloads_fact_ratio
        FactKind.LAST_ACTIVITY -> R.string.downloads_fact_last_activity
        FactKind.WASTED -> R.string.downloads_fact_wasted
        FactKind.SEEDING_TIME -> R.string.downloads_fact_seeding_time
        FactKind.SEEDS -> R.string.downloads_fact_seeds
        FactKind.PEERS -> R.string.downloads_fact_peers
        FactKind.PRIVATE -> R.string.downloads_fact_private
        FactKind.ADDED -> R.string.downloads_fact_added
        FactKind.POSTED -> R.string.downloads_fact_posted
        FactKind.DONE_AT -> R.string.downloads_fact_done
        FactKind.TIME_ACTIVE -> R.string.downloads_fact_time_active
        FactKind.RELEASE -> R.string.downloads_fact_release
        FactKind.SAVE_PATH -> R.string.downloads_fact_save_path
        FactKind.DOWNLOADER -> R.string.downloads_fact_downloader
        FactKind.CATEGORY -> R.string.downloads_fact_category
        FactKind.FOLDER -> R.string.downloads_fact_folder
        FactKind.REQUESTED_BY -> R.string.downloads_fact_requested_by
        FactKind.SPEED_LIMITS -> R.string.downloads_fact_speed_limits
        FactKind.SEEDING_RULES -> R.string.downloads_fact_seeding_rules
    }

@Composable
private fun factValue(f: InfoFact): String =
    when {
        f.kind == FactKind.DOWNLOADED -> stringResource(R.string.downloads_value_of, DownloadFormat.size(f.a), DownloadFormat.size(f.b))
        f.kind == FactKind.SEEDS || f.kind == FactKind.PEERS ->
            if (f.b > 0) stringResource(R.string.downloads_value_of, f.a.toString(), f.b.toString()) else f.a.toString()
        f.kind == FactKind.PRIVATE -> stringResource(if (f.a > 0) R.string.downloads_value_yes else R.string.downloads_value_no)
        f.kind == FactKind.SPEED_LIMITS ->
            stringResource(
                R.string.downloads_value_caps,
                if (f.a > 0) speedText(f.a) else stringResource(R.string.downloads_value_no_cap),
                if (f.b > 0) speedText(f.b) else stringResource(R.string.downloads_value_no_cap),
            )
        f.limits != null -> limitsText(f.limits ?: return "")
        f.at.isNotEmpty() -> listOfNotNull(dateTimeText(f.at), relativeTime(f.at)).joinToString(" · ")
        f.seconds > 0 -> durationText(f.seconds)
        else -> f.text
    }

@Composable
private fun InfoTab(state: DownloadDetailState) {
    val groups = state.info
    if (groups.isEmpty()) {
        Text(stringResource(R.string.downloads_detail_nothing), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    if (state.detail?.torrentError?.isNotEmpty() == true) {
        Text(
            stringResource(R.string.downloads_detail_read_failed),
            color = MaterialTheme.acolyte.waitText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    groups.forEach { group ->
        Text(
            stringResource(group.kind.label()),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = Spacing.Small),
        )
        group.facts.forEach { f ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(
                    stringResource(f.kind.label()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.4f),
                )
                Text(factValue(f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.6f))
            }
        }
    }
}

private val PRIORITIES =
    listOf(
        0 to R.string.downloads_file_skip,
        1 to R.string.downloads_file_normal,
        6 to R.string.downloads_file_high,
        7 to R.string.downloads_file_max,
    )

@Composable
private fun FilesTab(
    state: DownloadDetailState,
    presenter: DownloadDetailPresenter,
    onReauth: suspend (String, String) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Pair<List<Int>, Int>?>(null) }
    if (state.needsReauth) {
        var password by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(stringResource(R.string.downloads_reauth_files))
                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text(stringResource(R.string.downloads_reauth_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(code, {
                    code = it
                }, label = { Text(stringResource(R.string.downloads_reauth_code)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        if (onReauth(password, code)) pending?.let { (indexes, priority) -> presenter.setFilePriority(indexes, priority) }
                    }
                }) { Text(stringResource(R.string.downloads_reauth_confirm)) }
            }
        }
    }
    val files = state.files
    if (files == null || files.isEmpty()) {
        Text(stringResource(R.string.downloads_files_empty_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.downloads_files_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    if (!state.singleFile) {
        OutlinedTextField(
            state.fileFilter,
            presenter::setFileFilter,
            label = { Text(stringResource(R.string.downloads_files_filter)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val tree = state.fileTree
    if (tree.isEmpty()) Text(stringResource(R.string.downloads_files_none_match), color = MaterialTheme.colorScheme.onSurfaceVariant)
    tree.forEach { node ->
        Row(Modifier.fillMaxWidth().padding(start = (node.depth * 12).dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (node.folder) "${node.name}/" else node.name,
                    style = if (node.folder) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${DownloadFormat.size(node.size)} · ${DownloadFormat.percent(node.progress)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            var open by remember { mutableStateOf(false) }
            Box {
                val current = PRIORITIES.firstOrNull { it.first == node.priority }?.second ?: R.string.downloads_file_mixed
                TextButton(onClick = { open = true }, enabled = state.canEdit && !state.busy) { Text(stringResource(current)) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    PRIORITIES.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                            open = false
                            pending = node.indexes to value
                            scope.launch { presenter.setFilePriority(node.indexes, value) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun PeersTab(state: DownloadDetailState) {
    val peers = state.peers
    if (peers.isEmpty()) {
        Text(stringResource(R.string.downloads_peers_empty_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.downloads_peers_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    peers.forEach { p ->
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(listOf(p.address, p.client).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            Text(
                "${DownloadFormat.percent(p.progress)}% · " +
                    stringResource(R.string.downloads_speed_down, speedText(p.speed)) + " · " +
                    stringResource(R.string.downloads_speed_up, speedText(p.upSpeed)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun trackerStatus(status: String): Int? =
    when (status) {
        "working" -> R.string.downloads_tracker_working
        "not_contacted" -> R.string.downloads_tracker_not_contacted
        "not_working" -> R.string.downloads_tracker_not_working
        "error" -> R.string.downloads_tracker_error
        "unreachable" -> R.string.downloads_tracker_unreachable
        "disabled" -> R.string.downloads_tracker_disabled
        else -> null
    }

@Composable
private fun TrackersTab(
    state: DownloadDetailState,
    presenter: DownloadDetailPresenter,
) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<TrackerInfo?>(null) }
    var adding by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf<List<String>>(emptyList()) }
    val trackers = state.trackers
    if (trackers.isEmpty()) {
        Text(stringResource(R.string.downloads_trackers_empty_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.downloads_trackers_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    trackers.forEach { t ->
        val editable = state.canEdit && !t.fixed && t.ref.isNotEmpty()
        Column(
            Modifier.fillMaxWidth().let { if (editable) it.clickable { editing = t } else it }.padding(vertical = 4.dp),
        ) {
            Text(
                t.url,
                style = MaterialTheme.typography.bodySmall,
                color = if (t.fixed) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            )
            val words =
                listOfNotNull(
                    if (t.fixed) {
                        stringResource(R.string.downloads_tracker_peer_source)
                    } else {
                        trackerStatus(t.status)?.let { stringResource(it) }
                            ?: t.status
                    },
                    if (!t.fixed) stringResource(R.string.downloads_tracker_tier, t.tier) else null,
                    stringResource(R.string.downloads_tracker_counts, t.seeds, t.peers),
                    t.message.ifEmpty { null },
                )
            Text(words.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (state.canEdit) {
        HorizontalDivider()
        OutlinedTextField(
            adding,
            {
                adding = it
                bad = emptyList()
            },
            label = { Text(stringResource(R.string.downloads_trackers_add_label)) },
            placeholder = { Text(stringResource(R.string.downloads_trackers_add_hint)) },
            minLines = 2,
            isError = bad.isNotEmpty(),
            supportingText =
                bad.firstOrNull { it.isNotEmpty() }?.let { line ->
                    { Text(stringResource(R.string.downloads_tracker_bad, line)) }
                },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(enabled = !state.busy && adding.isNotBlank(), onClick = {
            scope.launch {
                val left = presenter.addTrackers(adding)
                if (left.isEmpty()) adding = "" else bad = left
            }
        }) { Text(stringResource(R.string.downloads_trackers_add)) }
    }
    editing?.let { t ->
        var url by remember(t) { mutableStateOf("") }
        var tier by remember(t) { mutableStateOf(t.tier.toString()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.downloads_tracker_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(t.url, style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(R.string.downloads_tracker_hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        url,
                        { url = it },
                        label = { Text(stringResource(R.string.downloads_tracker_new_address)) },
                        singleLine = true,
                    )
                    NumberField(stringResource(R.string.downloads_tracker_tier_label), tier) { tier = it.filter(Char::isDigit) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newTier = tier.toIntOrNull()?.takeIf { it != t.tier }
                    scope.launch { presenter.editTracker(t.ref, url, newTier) }
                    editing = null
                }) { Text(stringResource(R.string.downloads_save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch { presenter.removeTracker(t.ref) }
                    editing = null
                }) { Text(stringResource(R.string.downloads_action_remove), color = MaterialTheme.acolyte.failText) }
            },
        )
    }
}

@Composable
private fun ServersTab(state: DownloadDetailState) {
    val rows = state.servers
    if (rows.isEmpty()) {
        Text(stringResource(R.string.downloads_servers_empty_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.downloads_servers_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    rows.forEach { r ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(r.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(
                listOfNotNull(
                    r.job.ifEmpty {
                        null
                    },
                    r.found.ifEmpty { null }?.let { stringResource(R.string.downloads_server_found, it) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun repairLabel(stage: String): Int =
    when (stage) {
        "download" -> R.string.downloads_repair_download
        "repair" -> R.string.downloads_repair_repair
        "join" -> R.string.downloads_repair_join
        "unpack" -> R.string.downloads_repair_unpack
        "rename" -> R.string.downloads_repair_rename
        "move" -> R.string.downloads_repair_move
        else -> R.string.downloads_repair_script
    }

@Composable
private fun LogTab(state: DownloadDetailState) {
    val repair = state.repair
    val log = state.log
    if (repair.isEmpty() && log.isEmpty()) {
        Text(stringResource(R.string.downloads_log_empty_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.downloads_log_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    repair.forEach { step ->
        Text(stringResource(repairLabel(step.stage)), style = MaterialTheme.typography.titleSmall)
        step.lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
    val today =
        ch.orellbuehler.acolyte.feature.downloads.Days
            .today(Clock.System.now())
    log.groupBy { it.day }.forEach { (day, lines) ->
        val heading =
            when {
                day == null -> ""
                day == today -> stringResource(R.string.downloads_day_today)
                day == today.plusDays(-1) -> stringResource(R.string.downloads_day_yesterday)
                else -> dayText(day)
            }
        if (heading.isNotEmpty()) {
            Text(
                heading,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = Spacing.Small),
            )
        }
        lines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(
                    clockText(line.at).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    (if (line.warn) "⚠ " else "") + line.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (line.warn) MaterialTheme.acolyte.waitText else Color.Unspecified,
                )
            }
        }
    }
}

@Composable
internal fun reasonText(line: ReasonLine): String =
    when (line.kind) {
        ReasonLineKind.READING -> stringResource(R.string.downloads_reason_reading)
        ReasonLineKind.EDITION_UNKNOWN -> stringResource(R.string.downloads_reason_edition)
        ReasonLineKind.COPY_IN_PLACE -> stringResource(R.string.downloads_reason_copy)
        ReasonLineKind.NO_MATCH_MOVIE -> stringResource(R.string.downloads_reason_no_match_movie)
        ReasonLineKind.NO_MATCH_EPISODE -> stringResource(R.string.downloads_reason_no_match_episode)
        ReasonLineKind.NO_MATCH_TRACK -> stringResource(R.string.downloads_reason_no_match_track)
        ReasonLineKind.SERVER -> line.text
        ReasonLineKind.WHY -> whyText(line.why)
    }

@Composable
internal fun whyText(why: WhyKind): String =
    stringResource(
        when (why) {
            WhyKind.RAN_OUT -> R.string.downloads_why_ran_out
            WhyKind.CONFLICT -> R.string.downloads_why_conflict
            WhyKind.NOT_REPLACED -> R.string.downloads_why_not_replaced
            WhyKind.NOT_SENT -> R.string.downloads_why_not_sent
            WhyKind.LINK_REFUSED -> R.string.downloads_why_link_refused
            WhyKind.TAKEN -> R.string.downloads_why_taken
        },
    )

private fun DecisionStep.label(): Int =
    when (this) {
        DecisionStep.SEARCH -> R.string.downloads_decide_search
        DecisionStep.IMPORT_AGAIN -> R.string.downloads_decide_import_again
        DecisionStep.IMPORT_FILE -> R.string.downloads_decide_import_file
        DecisionStep.SET_EDITION -> R.string.downloads_decide_set_edition
        DecisionStep.OPEN_VERSIONS -> R.string.downloads_decide_versions
        DecisionStep.OPEN_RELEASES -> R.string.downloads_decide_releases
    }

private sealed interface DecisionConfirm {
    data class ImportFile(
        val d: Decision,
    ) : DecisionConfirm

    data class TurnDown(
        val d: Decision,
        val pack: Boolean,
    ) : DecisionConfirm

    data class Stop(
        val d: Decision,
        val pack: Boolean,
    ) : DecisionConfirm
}

@Composable
private fun DecisionPanel(
    decisions: List<Decision>,
    list: DownloadsState,
    viewModel: DownloadsViewModel,
    onOpenTitle: (DownloadTitleLink) -> Unit,
    onOpenRoute: (Any) -> Unit,
) {
    var confirm by remember { mutableStateOf<DecisionConfirm?>(null) }
    val groups = remember(decisions) { groupDecisions(decisions) }
    Text(
        stringResource(R.string.downloads_decide_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.acolyte.failText,
    )
    groups.forEach { group -> DecisionCard(group, list, viewModel, onOpenTitle, onOpenRoute) { confirm = it } }
    when (val c = confirm) {
        null -> Unit
        is DecisionConfirm.ImportFile -> {
            val name = DownloadRules.decisionName(c.d)
            ConfirmDialog(
                text =
                    if (c.d.hasCopy) {
                        stringResource(R.string.downloads_decide_import_replace, c.d.importFile, name)
                    } else {
                        stringResource(R.string.downloads_decide_import_confirm, c.d.importFile, name)
                    },
                button = stringResource(R.string.downloads_decide_import_button),
                onDismiss = { confirm = null },
            ) { viewModel.decide(c.d, DecideAction.IMPORT_FILE, false) }
        }
        is DecisionConfirm.TurnDown ->
            ConfirmDialog(
                text =
                    if (c.pack) {
                        stringResource(R.string.downloads_decide_turn_down_pack)
                    } else {
                        stringResource(
                            R.string.downloads_decide_turn_down_confirm,
                            c.d.releaseName.ifEmpty { DownloadRules.decisionName(c.d) },
                        )
                    },
                button = stringResource(R.string.downloads_decide_turn_down),
                onDismiss = { confirm = null },
            ) { viewModel.decide(c.d, DecideAction.TURN_DOWN, c.pack) }
        is DecisionConfirm.Stop ->
            ConfirmDialog(
                text =
                    stringResource(
                        R.string.downloads_decide_stop_confirm,
                        if (c.pack) stringResource(R.string.downloads_decide_these) else DownloadRules.decisionName(c.d),
                    ),
                button = stringResource(R.string.downloads_decide_stop_button),
                destructive = true,
                onDismiss = { confirm = null },
            ) { viewModel.decide(c.d, DecideAction.STOP, c.pack) }
    }
}

@Composable
private fun ConfirmDialog(
    text: String,
    button: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(button, color = if (destructive) MaterialTheme.acolyte.failText else Color.Unspecified) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun DecisionCard(
    group: DecisionGroup,
    list: DownloadsState,
    viewModel: DownloadsViewModel,
    onOpenTitle: (DownloadTitleLink) -> Unit,
    onOpenRoute: (Any) -> Unit,
    onConfirm: (DecisionConfirm) -> Unit,
) {
    val d = group.lead
    val plan = remember(d) { DecisionRules.plan(d) }
    val busy = group.key in list.busy || "slot-${d.slotId}" in list.busy
    var why by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var edition by remember { mutableStateOf("") }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val turnDownLabel = if (group.pack) R.string.downloads_decide_pack_turn_down else R.string.downloads_decide_turn_down

    fun run(step: DecisionStep) {
        when (step) {
            DecisionStep.SEARCH -> viewModel.decide(d, DecideAction.SEARCH, group.pack)
            DecisionStep.IMPORT_AGAIN -> viewModel.importAgain(d)
            DecisionStep.IMPORT_FILE -> onConfirm(DecisionConfirm.ImportFile(d))
            DecisionStep.SET_EDITION -> editing = true
            DecisionStep.OPEN_VERSIONS, DecisionStep.OPEN_RELEASES ->
                viewModel.openTitle(d.titleId) { onOpenTitle(it.copy(openLibrary = true)) }
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (group.pack) {
                val total = d.pack.size.coerceAtLeast(group.items.size)
                Text(
                    stringResource(
                        if (d.kind == "artist") R.string.downloads_decide_pack_tracks else R.string.downloads_decide_pack_episodes,
                        group.items.size,
                        total,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                group.items.forEach { item ->
                    Text(
                        "${item.episode?.label.orEmpty()} ${item.episode?.name.orEmpty()} · ${reasonText(DecisionRules.reasonLine(item))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
            } else {
                Text(DownloadRules.decisionName(d), style = MaterialTheme.typography.titleSmall)
                Text(reasonText(DecisionRules.reasonLine(d)), style = MaterialTheme.typography.bodyMedium)
            }
            val minutes = DecisionRules.retryMinutes(d.retryAt, Clock.System.now())
            val meta =
                listOfNotNull(
                    d.version.ifEmpty { null },
                    relativeTime(d.since),
                    minutes?.let { stringResource(R.string.downloads_decide_retry_in, it) },
                )
            if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = muted)
            when (plan.hint) {
                DecisionHint.PATH_PAIR ->
                    Text(
                        stringResource(R.string.downloads_decide_hint_path),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.acolyte.waitText,
                    )
                DecisionHint.MATCH_BY_HAND -> {
                    Text(
                        stringResource(R.string.downloads_decide_hint_match),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.acolyte.waitText,
                    )
                    if (LibraryTool.IMPORT_FILES in LocalSdk.current.libraryTools()) {
                        OutlinedButton(onClick = { onOpenRoute(LibraryImportFilesRoute()) }) {
                            Text(stringResource(R.string.downloads_decide_import_files))
                        }
                    }
                }
                DecisionHint.NONE -> Unit
            }
            if (editing) {
                OutlinedTextField(
                    edition,
                    { edition = it.take(80) },
                    label = { Text(stringResource(R.string.downloads_decide_edition_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(enabled = edition.isNotBlank() && !busy, onClick = {
                    viewModel.setEdition(d, edition) { ok -> if (ok) editing = false }
                }) { Text(stringResource(R.string.downloads_decide_edition_save)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                val lead = if (group.pack) (if (group.packImportAgain) DecisionStep.IMPORT_AGAIN else DecisionStep.SEARCH) else plan.lead
                Button(onClick = { run(lead) }, enabled = !busy && !(lead == DecisionStep.IMPORT_AGAIN && d.reading)) {
                    Text(
                        if (group.pack &&
                            lead == DecisionStep.SEARCH
                        ) {
                            stringResource(R.string.downloads_decide_pack_search)
                        } else {
                            stringResource(lead.label())
                        },
                    )
                }
                if (!group.pack) {
                    plan.beside?.let { step ->
                        OutlinedButton(onClick = { run(step) }, enabled = !busy && !d.reading) { Text(stringResource(step.label())) }
                    }
                }
                var open by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { open = true }, enabled = !busy) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.downloads_more_actions))
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        val steps =
                            if (group.pack) {
                                (
                                    if (lead ==
                                        DecisionStep.IMPORT_AGAIN
                                    ) {
                                        listOf(DecisionStep.SEARCH)
                                    } else {
                                        emptyList()
                                    }
                                )
                            } else {
                                plan.more
                            }
                        steps.forEach { step ->
                            DropdownMenuItem(text = { Text(stringResource(step.label())) }, onClick = {
                                open = false
                                run(step)
                            })
                        }
                        if (plan.turnDown) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(turnDownLabel))
                                },
                                onClick = {
                                    open = false
                                    onConfirm(DecisionConfirm.TurnDown(d, group.pack))
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(if (group.pack) R.string.downloads_decide_pack_stop else R.string.downloads_decide_stop),
                                    color = MaterialTheme.acolyte.failText,
                                )
                            },
                            onClick = {
                                open = false
                                onConfirm(DecisionConfirm.Stop(d, group.pack))
                            },
                        )
                        if (d.kind != "artist") {
                            DropdownMenuItem(text = { Text(stringResource(R.string.downloads_action_open)) }, onClick = {
                                open = false
                                viewModel.openTitle(d.titleId, onOpenTitle)
                            })
                        }
                    }
                }
                TextButton(onClick = { why = !why }) { Text(stringResource(R.string.downloads_decide_why)) }
            }
            if (why) {
                Text(whyText(DecisionRules.whyOf(d)), style = MaterialTheme.typography.bodySmall)
                if (d.reason.isNotBlank()) Text(d.reason, style = MaterialTheme.typography.bodySmall, color = muted)
                if (d.hasCopy) Text(stringResource(R.string.downloads_decide_has_copy), style = MaterialTheme.typography.bodySmall)
                if (list.canSettings && d.releaseName.isNotEmpty()) {
                    Text(
                        stringResource(R.string.downloads_decide_release, d.releaseName),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
            }
        }
    }
}

@Composable
internal fun dayText(day: ch.orellbuehler.acolyte.feature.downloads.Day): String {
    val date = java.time.LocalDate.of(day.year, day.month, day.day)
    val now = java.time.LocalDate.now()
    val pattern = if (day.year == now.year) "EEE, d MMM" else "EEE, d MMM yyyy"
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return java.time.format.DateTimeFormatter
        .ofPattern(pattern, locale)
        .format(date)
}
