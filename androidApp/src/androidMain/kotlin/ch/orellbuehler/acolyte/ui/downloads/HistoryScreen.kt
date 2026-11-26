package ch.orellbuehler.acolyte.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.downloads.Days
import ch.orellbuehler.acolyte.feature.downloads.DownloadStepKey
import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import ch.orellbuehler.acolyte.feature.downloads.HistoryFactKind
import ch.orellbuehler.acolyte.feature.downloads.HistoryFilter
import ch.orellbuehler.acolyte.feature.downloads.HistoryItem
import ch.orellbuehler.acolyte.feature.downloads.HistoryLine
import ch.orellbuehler.acolyte.feature.downloads.HistoryLineKind
import ch.orellbuehler.acolyte.feature.downloads.HistoryNotice
import ch.orellbuehler.acolyte.feature.downloads.HistoryRedo
import ch.orellbuehler.acolyte.feature.downloads.HistoryState
import ch.orellbuehler.acolyte.feature.downloads.HistoryStep
import ch.orellbuehler.acolyte.feature.downloads.HistoryType
import ch.orellbuehler.acolyte.feature.downloads.HistoryVerb
import ch.orellbuehler.acolyte.feature.downloads.HistoryWhen
import ch.orellbuehler.acolyte.feature.downloads.HistoryWho
import ch.orellbuehler.acolyte.feature.downloads.HistoryWhoFilter
import ch.orellbuehler.acolyte.feature.downloads.LibraryLink
import ch.orellbuehler.acolyte.feature.downloads.StepState
import ch.orellbuehler.acolyte.feature.downloads.WhoKind
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.LibraryDeletedRoute
import ch.orellbuehler.acolyte.ui.nav.LibraryReleasesRoute
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Clock
import ch.orellbuehler.acolyte.common.R as CommonR

class HistoryViewModel(
    sdk: AcolyteSdk,
    titleId: Long,
    episodeId: Long,
) : ViewModel() {
    val presenter = sdk.history(titleId, episodeId)

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun filter(filter: HistoryFilter) {
        viewModelScope.launch { presenter.setFilter(filter) }
    }

    fun clearScope() {
        viewModelScope.launch { presenter.clearScope() }
    }

    fun showMore() {
        viewModelScope.launch { presenter.showMore() }
    }

    fun findAnother(key: String) {
        viewModelScope.launch { presenter.findAnother(key) }
    }

    fun openTitle(
        titleId: Long,
        open: (DownloadTitleLink) -> Unit,
    ) {
        viewModelScope.launch { presenter.titleLink(titleId)?.let(open) }
    }

    suspend fun watch() = presenter.watch()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    titleId: Long,
    episodeId: Long,
    onBack: (() -> Unit)?,
    onOpenTitle: (DownloadTitleLink) -> Unit,
    onOpenDownload: (String) -> Unit,
    onOpenRoute: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "history-$titleId-$episodeId") { HistoryViewModel(sdk, titleId, episodeId) }
    val state by viewModel.presenter.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    var filtering by remember { mutableStateOf(false) }
    var openKey by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val notice =
        state.notice?.let {
            stringResource(
                if (it ==
                    HistoryNotice.REPLACING
                ) {
                    R.string.downloads_history_replacing
                } else {
                    R.string.downloads_history_no_release
                },
            )
        }
    LaunchedEffect(state.noticeId) {
        if (notice != null) {
            snackbar.showSnackbar(notice)
            viewModel.presenter.consumeNotice()
        }
    }
    val error = state.actionError?.let { errorText(it) + " " + it.message }
    LaunchedEffect(error) { if (error != null) snackbar.showSnackbar(error) }
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            title = stringResource(R.string.section_history),
            onBack = onBack,
            actions = {
                IconButton(onClick = { searching = !searching }) {
                    Icon(
                        if (searching) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.downloads_search),
                    )
                }
                val count = state.filter.count
                IconButton(onClick = { filtering = true }) {
                    BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
                        Icon(
                            Icons.Rounded.FilterList,
                            contentDescription =
                                if (count >
                                    0
                                ) {
                                    stringResource(R.string.downloads_history_filter_on, count)
                                } else {
                                    stringResource(R.string.downloads_history_filter)
                                },
                        )
                    }
                }
            },
        ) { padding ->
            StatusContent(
                status = state.status,
                isEmpty = state.isEmpty && !state.filtered && state.titleId == 0L && state.episodeId == 0L,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
                empty = { EmptyView(title = stringResource(R.string.downloads_history_empty), icon = Icons.Rounded.History) },
            ) {
                HistoryList(state, searching, viewModel) { openKey = it.key }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (filtering) HistoryFilterSheet(state, viewModel) { filtering = false }
    openKey?.let { key ->
        val item = state.item(key)
        if (item == null) {
            LaunchedEffect(key) { openKey = null }
        } else {
            HistorySheet(item, state, viewModel, onOpenTitle, onOpenDownload, onOpenRoute) { openKey = null }
        }
    }
}

@Composable
private fun HistoryList(
    state: HistoryState,
    searching: Boolean,
    viewModel: HistoryViewModel,
    onOpen: (HistoryItem) -> Unit,
) {
    val presenter = viewModel.presenter
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        if (searching || state.query.isNotEmpty()) {
            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = presenter::setQuery,
                    placeholder = { Text(stringResource(R.string.downloads_history_search_hint)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                )
            }
        }
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        if (state.titleId > 0 || state.episodeId > 0) {
            item(key = "scope") {
                InputChip(
                    selected = true,
                    onClick = viewModel::clearScope,
                    label = {
                        Text(
                            if (state.scopeName.isNotEmpty()) {
                                stringResource(R.string.downloads_history_scope, state.scopeName)
                            } else {
                                stringResource(R.string.downloads_history_scope_title)
                            },
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.downloads_history_clear),
                            Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.padding(horizontal = Spacing.Screen),
                )
            }
        }
        if (state.days.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.downloads_history_empty_filtered),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.Screen),
                )
            }
        }
        val today = Days.today(Clock.System.now())
        state.days.forEach { day ->
            item(key = "day-${day.day.key}") {
                val heading =
                    when (day.day) {
                        today -> stringResource(CommonR.string.downloads_day_today)
                        today.plusDays(-1) -> stringResource(R.string.downloads_day_yesterday)
                        else -> dayText(day.day)
                    }
                val tally =
                    listOfNotNull(
                        stringResource(R.string.downloads_history_tally, day.items.size),
                        if (day.failed > 0) stringResource(R.string.downloads_history_tally_failed, day.failed) else null,
                        if (day.needsYou > 0) stringResource(R.string.downloads_history_tally_needs, day.needsYou) else null,
                    )
                Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen).padding(top = Spacing.Large, bottom = Spacing.XSmall)) {
                    Text(heading, style = MaterialTheme.typography.titleMedium)
                    Text(
                        tally.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(day.items, key = { it.key }) { item -> HistoryRow(item) { onOpen(item) } }
        }
        if (state.more) {
            item(key = "more") {
                Column(Modifier.fillMaxWidth().padding(Spacing.Screen), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.downloads_history_count, state.loaded, state.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = viewModel::showMore, enabled = !state.loadingMore) {
                        Text(stringResource(R.string.downloads_history_show_more))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        ServerImage(
            source = item.poster,
            contentDescription = null,
            placeholderName = item.title.ifEmpty { "?" },
            modifier = (if (item.square) Modifier.size(40.dp) else Modifier.size(36.dp, 54.dp)).clip(RoundedCornerShape(Radius.Poster / 2)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(stringResource(item.verb.label()), item.verb.tone)
                Text(clockText(item.head.at).orEmpty(), style = MaterialTheme.typography.bodySmall, color = muted)
            }
            if (item.title.isNotEmpty()) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val bits = listOf(item.episode, item.version).filter { it.isNotEmpty() }
            if (bits.isNotEmpty()) Text(bits.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1)
            val line = lineText(item.line)
            if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val foot = listOf(item.extra, whoText(item.who)).filter { it.isNotEmpty() }
            if (foot.isNotEmpty()) Text(foot.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1)
        }
    }
}

@Composable
private fun whoText(who: HistoryWho): String =
    when (who.kind) {
        WhoKind.PERSON -> who.name
        WhoKind.A_PERSON -> stringResource(R.string.downloads_who_a_person)
        WhoKind.AI -> {
            val app = who.name.ifEmpty { stringResource(R.string.downloads_who_ai_app) }
            if (who.forName.isNotEmpty()) stringResource(R.string.downloads_who_ai, app, who.forName) else app
        }
        WhoKind.JOB -> stringResource(R.string.downloads_who_job)
        WhoKind.HOLYARRS -> stringResource(R.string.downloads_history_holyarrs)
    }

@Composable
private fun lineText(line: HistoryLine): String {
    val phrase =
        when (line.kind) {
            HistoryLineKind.SUMMARY, HistoryLineKind.REASON -> return line.text
            HistoryLineKind.ADDED_LIST ->
                if (line.text.isNotEmpty()) {
                    return stringResource(
                        R.string.downloads_line_added_list_named,
                        line.text,
                    )
                } else {
                    R.string.downloads_line_added_list
                }
            HistoryLineKind.ADDED_IMPORT -> R.string.downloads_line_added_import
            HistoryLineKind.ADDED_BY -> return stringResource(R.string.downloads_line_added_by, line.text)
            HistoryLineKind.ADDED_ASK -> return stringResource(R.string.downloads_line_added_ask, line.text)
            HistoryLineKind.REMOVED_KEPT -> R.string.downloads_line_removed_kept
            HistoryLineKind.REMOVED_DELETED -> R.string.downloads_line_removed_deleted
            HistoryLineKind.RENAMED -> R.string.downloads_line_renamed
            HistoryLineKind.RENAME_READY -> R.string.downloads_line_rename_ready
            HistoryLineKind.FEEDS -> R.string.downloads_line_feeds
            HistoryLineKind.LOOKED -> R.string.downloads_line_looked
            HistoryLineKind.SEARCH_FAILED -> R.string.downloads_line_search_failed
            HistoryLineKind.TURNED_DOWN -> R.string.downloads_line_turned_down
            HistoryLineKind.UNBLOCKED -> R.string.downloads_line_unblocked
            HistoryLineKind.HELD_BACK -> R.string.downloads_line_held_back
            HistoryLineKind.HELD_GONE -> R.string.downloads_line_held_gone
            HistoryLineKind.WAITING -> R.string.downloads_line_waiting
            HistoryLineKind.DOWNLOADING -> R.string.downloads_line_downloading
            HistoryLineKind.DOWNLOADING_PCT -> return stringResource(R.string.downloads_line_downloading_pct, line.number)
            HistoryLineKind.STALLED -> R.string.downloads_line_stalled
            HistoryLineKind.PAUSED -> R.string.downloads_line_paused
            HistoryLineKind.PAUSED_AT -> return stringResource(R.string.downloads_line_paused_at, line.number)
            HistoryLineKind.SHARING -> R.string.downloads_line_sharing
            HistoryLineKind.MOVED_OUT -> R.string.downloads_line_moved_out
            HistoryLineKind.REMOVED_OUTSIDE -> R.string.downloads_line_removed_outside
            HistoryLineKind.REMOVED_DL -> R.string.downloads_line_removed_dl
            HistoryLineKind.REMOVED_DL_FILES -> R.string.downloads_line_removed_dl_files
            HistoryLineKind.DL_FAILED -> R.string.downloads_line_dl_failed
            HistoryLineKind.NOT_REMOVED -> R.string.downloads_line_not_removed
            HistoryLineKind.NOT_MOVED -> R.string.downloads_line_not_moved
            HistoryLineKind.PARTLY -> R.string.downloads_line_partly
            HistoryLineKind.NOTHING_PLACED -> R.string.downloads_line_nothing_placed
            HistoryLineKind.BETTER -> R.string.downloads_line_better
            HistoryLineKind.FILE_BACK -> R.string.downloads_line_file_back
            HistoryLineKind.FILE_HELD -> R.string.downloads_line_file_held
            HistoryLineKind.HOLD_FAILED -> R.string.downloads_line_hold_failed
            HistoryLineKind.HOLDING_CLEANED -> R.string.downloads_line_holding_cleaned
            HistoryLineKind.LOW_SPACE -> R.string.downloads_line_low_space
            HistoryLineKind.STOPPED_LOOKING -> R.string.downloads_line_stopped_looking
            HistoryLineKind.WORKING_AGAIN -> R.string.downloads_line_working_again
            HistoryLineKind.MEDIA_TOLD -> R.string.downloads_line_media_told
            HistoryLineKind.MEDIA_FAILED -> R.string.downloads_line_media_failed
            HistoryLineKind.ALERT_FAILED -> R.string.downloads_line_alert_failed
            HistoryLineKind.OUT_NOW -> R.string.downloads_line_out_now
            HistoryLineKind.REPORT -> R.string.downloads_line_report
            HistoryLineKind.CHECKED_DETAILS -> R.string.downloads_line_checked_details
        }
    val text = stringResource(phrase)
    return if (line.text.isNotEmpty()) "$text · ${line.text}" else text
}

internal fun HistoryVerb.label(): Int =
    when (this) {
        HistoryVerb.IMPORTED -> R.string.downloads_verb_imported
        HistoryVerb.PARTLY_IMPORTED -> R.string.downloads_verb_partly_imported
        HistoryVerb.UPGRADED -> R.string.downloads_verb_upgraded
        HistoryVerb.LOW_ON_SPACE -> R.string.downloads_verb_low_on_space
        HistoryVerb.NEEDS_DECISION -> R.string.downloads_verb_needs_decision
        HistoryVerb.NEEDS_YOU -> R.string.downloads_verb_needs_you
        HistoryVerb.STOPPED_LOOKING -> R.string.downloads_verb_stopped_looking
        HistoryVerb.FIXED -> R.string.downloads_verb_fixed
        HistoryVerb.ASKED -> R.string.downloads_verb_asked
        HistoryVerb.APPROVED -> R.string.downloads_verb_approved
        HistoryVerb.TURNED_DOWN -> R.string.downloads_verb_turned_down
        HistoryVerb.FAILED -> R.string.downloads_verb_failed
        HistoryVerb.REPORTED -> R.string.downloads_verb_reported
        HistoryVerb.REOPENED -> R.string.downloads_verb_reopened
        HistoryVerb.DOWNLOADED -> R.string.downloads_verb_downloaded
        HistoryVerb.STALLED -> R.string.downloads_verb_stalled
        HistoryVerb.PAUSED -> R.string.downloads_verb_paused
        HistoryVerb.QUEUED -> R.string.downloads_verb_queued
        HistoryVerb.FILE_MISSING -> R.string.downloads_verb_file_missing
        HistoryVerb.FILE_BACK -> R.string.downloads_verb_file_back
        HistoryVerb.REFRESHED -> R.string.downloads_verb_refreshed
        HistoryVerb.ALBUMS_CHANGED -> R.string.downloads_verb_albums_changed
        HistoryVerb.OUT_NOW -> R.string.downloads_verb_out_now
        HistoryVerb.CHANGED -> R.string.downloads_verb_changed
        HistoryVerb.SETTINGS_IMPORTED -> R.string.downloads_verb_settings_imported
        HistoryVerb.ADOPTED -> R.string.downloads_verb_adopted
        HistoryVerb.ADDED -> R.string.downloads_verb_added
        HistoryVerb.SEARCHED -> R.string.downloads_verb_searched
        HistoryVerb.SEARCH_FAILED -> R.string.downloads_verb_search_failed
        HistoryVerb.HELD -> R.string.downloads_verb_held
        HistoryVerb.CHOSEN -> R.string.downloads_verb_chosen
        HistoryVerb.SENT -> R.string.downloads_verb_sent
        HistoryVerb.DOWNLOADING -> R.string.downloads_verb_downloading
        HistoryVerb.RENAMED -> R.string.downloads_verb_renamed
        HistoryVerb.DELETED -> R.string.downloads_verb_deleted
        HistoryVerb.SUBTITLES, HistoryVerb.SUBTITLES_WAIT -> R.string.downloads_verb_subtitles
        HistoryVerb.SCANNED -> R.string.downloads_verb_scanned
        HistoryVerb.HEALTH -> R.string.downloads_verb_health
        HistoryVerb.REQUESTS -> R.string.downloads_verb_requests
        HistoryVerb.BY_AI -> R.string.downloads_verb_by_ai
        HistoryVerb.NOT_DONE -> R.string.downloads_verb_not_done
        HistoryVerb.ALERT_FAILED -> R.string.downloads_verb_alert_failed
        HistoryVerb.MEDIA_SERVER, HistoryVerb.MEDIA_SERVER_FAILED -> R.string.downloads_verb_media_server
        HistoryVerb.SYSTEM -> R.string.downloads_verb_system
        HistoryVerb.OTHER -> R.string.downloads_verb_other
    }

internal fun HistoryStep.label(): Int =
    when (this) {
        HistoryStep.SENT -> R.string.downloads_history_step_sent
        HistoryStep.DOWNLOADED -> R.string.downloads_history_step_downloaded
        HistoryStep.IMPORTED -> R.string.downloads_history_step_imported
        HistoryStep.UPGRADED -> R.string.downloads_history_step_upgraded
        HistoryStep.RENAMED -> R.string.downloads_history_step_renamed
        HistoryStep.DELETED -> R.string.downloads_history_step_deleted
        HistoryStep.DECLINED -> R.string.downloads_history_step_declined
        HistoryStep.FAILED -> R.string.downloads_history_step_failed
        HistoryStep.ASKED -> R.string.downloads_history_step_asked
        HistoryStep.APPROVED -> R.string.downloads_history_step_approved
        HistoryStep.ADDED -> R.string.downloads_history_step_added
        HistoryStep.SUBTITLES -> R.string.downloads_history_step_subtitles
        HistoryStep.REPORTED -> R.string.downloads_history_step_reported
        HistoryStep.DECIDE -> R.string.downloads_history_step_decide
        HistoryStep.SYSTEM -> R.string.downloads_history_step_system
    }

private fun HistoryFactKind.label(): Int =
    when (this) {
        HistoryFactKind.QUALITY -> R.string.downloads_history_fact_quality
        HistoryFactKind.SIZE -> R.string.downloads_history_fact_size
        HistoryFactKind.SOURCE -> R.string.downloads_history_fact_source
        HistoryFactKind.DOWNLOADER -> R.string.downloads_history_fact_downloader
        HistoryFactKind.RELEASE -> R.string.downloads_history_fact_release
        HistoryFactKind.FILE -> R.string.downloads_history_fact_file
        HistoryFactKind.FROM -> R.string.downloads_history_fact_from
        HistoryFactKind.FOLDER -> R.string.downloads_history_fact_folder
        HistoryFactKind.WHY -> R.string.downloads_history_fact_why
        HistoryFactKind.ALBUM -> R.string.downloads_history_fact_album
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HistorySheet(
    item: HistoryItem,
    state: HistoryState,
    viewModel: HistoryViewModel,
    onOpenTitle: (DownloadTitleLink) -> Unit,
    onOpenDownload: (String) -> Unit,
    onOpenRoute: (Any) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                ServerImage(
                    source = item.poster,
                    contentDescription = null,
                    placeholderName = item.title.ifEmpty { "?" },
                    modifier =
                        (if (item.square) Modifier.size(64.dp) else Modifier.size(56.dp, 84.dp)).clip(
                            RoundedCornerShape(
                                Radius.Poster / 2,
                            ),
                        ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                    StatusChip(stringResource(item.verb.label()), item.verb.tone)
                    if (item.title.isNotEmpty()) Text(item.title, style = MaterialTheme.typography.titleMedium)
                    val bits = listOf(item.episode, item.version).filter { it.isNotEmpty() }
                    if (bits.isNotEmpty()) Text(bits.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (item.steps.isNotEmpty()) {
                Text(stringResource(R.string.downloads_history_steps), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    item.steps.forEach { step ->
                        val label =
                            when (step.key) {
                                DownloadStepKey.SENT -> R.string.downloads_history_step_sent
                                DownloadStepKey.DOWNLOADED -> R.string.downloads_history_step_downloaded
                                DownloadStepKey.IMPORTED -> R.string.downloads_history_step_imported
                            }
                        val (word, tone) =
                            when (step.state) {
                                StepState.DONE -> stringResource(label) to ch.orellbuehler.acolyte.feature.Tone.OK
                                StepState.FAILED ->
                                    stringResource(R.string.downloads_history_step_failed) to
                                        ch.orellbuehler.acolyte.feature.Tone.FAIL
                                StepState.NEEDS ->
                                    stringResource(R.string.downloads_history_step_decide) to
                                        ch.orellbuehler.acolyte.feature.Tone.WAIT
                                StepState.WAITING -> stringResource(label) to ch.orellbuehler.acolyte.feature.Tone.IDLE
                            }
                        StatusChip(word, tone)
                    }
                }
            }
            val line = lineText(item.line)
            if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodyMedium)
            Fact(
                stringResource(R.string.downloads_history_fact_when),
                listOfNotNull(dateTimeText(item.head.at), relativeTime(item.head.at)).joinToString(" · "),
            )
            Fact(stringResource(R.string.downloads_history_fact_by), whoText(item.who))
            item.facts.forEach { f ->
                Fact(stringResource(f.kind.label()), f.text) {
                    if (f.kind == HistoryFactKind.FOLDER || f.kind == HistoryFactKind.FILE || f.kind == HistoryFactKind.RELEASE) {
                        TextButton(
                            onClick = { clipboard.setText(AnnotatedString(f.text)) },
                        ) { Text(stringResource(R.string.downloads_history_copy)) }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                item.redo?.let { redo ->
                    Button(onClick = { confirm = true }, enabled = item.key !in state.busy) {
                        Text(
                            stringResource(
                                if (redo ==
                                    HistoryRedo.FIND_ANOTHER
                                ) {
                                    R.string.downloads_history_find_another
                                } else {
                                    R.string.downloads_history_replace
                                },
                            ),
                        )
                    }
                }
                if (item.head.titleId > 0) {
                    OutlinedButton(
                        onClick = { viewModel.openTitle(item.head.titleId, onOpenTitle) },
                    ) { Text(stringResource(CommonR.string.downloads_action_open)) }
                }
                if (item.downloadId > 0) {
                    OutlinedButton(
                        onClick = { onOpenDownload("d${item.downloadId}") },
                    ) { Text(stringResource(R.string.downloads_history_open_downloads)) }
                }
                when (item.libraryLink) {
                    LibraryLink.TURNED_DOWN ->
                        OutlinedButton(onClick = { onOpenRoute(LibraryReleasesRoute(turnedDown = true)) }) {
                            Text(stringResource(R.string.downloads_history_open_turned_down))
                        }
                    LibraryLink.HELD ->
                        OutlinedButton(onClick = { onOpenRoute(LibraryReleasesRoute(turnedDown = false)) }) {
                            Text(stringResource(R.string.downloads_history_open_held))
                        }
                    LibraryLink.DELETED ->
                        OutlinedButton(onClick = { onOpenRoute(LibraryDeletedRoute(item.head.titleId)) }) {
                            Text(stringResource(R.string.downloads_history_open_deleted))
                        }
                    LibraryLink.NONE -> Unit
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            text = { Text(stringResource(R.string.downloads_history_redo_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    viewModel.findAnother(item.key)
                }) { Text(stringResource(R.string.downloads_history_redo_button)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun Fact(
    label: String,
    value: String,
    trailing: @Composable () -> Unit = {},
) {
    if (value.isBlank()) return
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f),
        )
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.7f))
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilterSheet(
    state: HistoryState,
    viewModel: HistoryViewModel,
    onDismiss: () -> Unit,
) {
    var f by remember { mutableStateOf(state.filter) }
    var picking by remember { mutableStateOf<Boolean?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.downloads_history_filter), style = MaterialTheme.typography.titleLarge)
            ChipGroup(stringResource(R.string.downloads_history_event)) {
                HistoryStep.entries.forEach { step ->
                    FilterChip(
                        selected = step in f.steps,
                        onClick = { f = f.copy(steps = if (step in f.steps) f.steps - step else f.steps + step) },
                        label = { Text(stringResource(step.label())) },
                    )
                }
            }
            ChipGroup(stringResource(R.string.downloads_history_type)) {
                listOf(
                    HistoryType.ALL to R.string.downloads_history_all,
                    HistoryType.MOVIES to R.string.downloads_history_movies,
                    HistoryType.SHOWS to R.string.downloads_history_shows,
                    HistoryType.MUSIC to R.string.downloads_history_music,
                ).forEach { (t, label) -> FilterChip(f.type == t, { f = f.copy(type = t) }, label = { Text(stringResource(label)) }) }
            }
            ChipGroup(stringResource(R.string.downloads_history_who)) {
                listOf(
                    HistoryWhoFilter.ALL to R.string.downloads_history_all,
                    HistoryWhoFilter.ME to R.string.downloads_history_you,
                    HistoryWhoFilter.PEOPLE to R.string.downloads_history_members,
                    HistoryWhoFilter.AI to R.string.downloads_history_ai,
                    HistoryWhoFilter.HOLYARRS to R.string.downloads_history_holyarrs,
                ).forEach { (w, label) -> FilterChip(f.who == w, { f = f.copy(who = w) }, label = { Text(stringResource(label)) }) }
            }
            ChipGroup(stringResource(R.string.downloads_history_when)) {
                listOf(
                    HistoryWhen.ALL to R.string.downloads_history_all_time,
                    HistoryWhen.DAY to R.string.downloads_history_day,
                    HistoryWhen.WEEK to R.string.downloads_history_week,
                    HistoryWhen.MONTH to R.string.downloads_history_month,
                    HistoryWhen.CUSTOM to R.string.downloads_history_custom,
                ).forEach { (w, label) -> FilterChip(f.period == w, { f = f.copy(period = w) }, label = { Text(stringResource(label)) }) }
            }
            if (f.period == HistoryWhen.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    OutlinedButton(onClick = { picking = true }) {
                        Text(stringResource(R.string.downloads_history_from) + ": " + f.from.ifEmpty { "–" })
                    }
                    OutlinedButton(onClick = { picking = false }) {
                        Text(stringResource(R.string.downloads_history_to) + ": " + f.to.ifEmpty { "–" })
                    }
                }
                if (f.badRange) Text(stringResource(R.string.downloads_history_bad_range), color = MaterialTheme.acolyte.failText)
            }
            if (state.canSources && state.sources.isNotEmpty()) {
                val names = listOf(stringResource(R.string.downloads_history_all_sources)) + state.sources.map { it.name }
                Picker(
                    stringResource(R.string.downloads_history_source),
                    state.sources.firstOrNull { it.id == f.source }?.name ?: names[0],
                    names,
                ) { i ->
                    f = f.copy(source = if (i == 0) 0 else state.sources[i - 1].id)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = { f = HistoryFilter() }) { Text(stringResource(R.string.downloads_history_clear)) }
                Button(enabled = !f.badRange, onClick = {
                    viewModel.filter(f)
                    onDismiss()
                }) { Text(stringResource(R.string.downloads_history_done)) }
            }
        }
    }
    picking?.let { from ->
        val current = if (from) f.from else f.to
        val initial =
            runCatching {
                LocalDate
                    .parse(current)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    val day =
                        picker.selectedDateMillis
                            ?.let {
                                Instant
                                    .ofEpochMilli(
                                        it,
                                    ).atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString()
                            }.orEmpty()
                    f = if (from) f.copy(from = day) else f.copy(to = day)
                    picking = null
                }) { Text(stringResource(R.string.downloads_history_done)) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(picker, title = { Text(stringResource(R.string.downloads_history_pick_date), Modifier.padding(Spacing.Large)) }) }
    }
}
