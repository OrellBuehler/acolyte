package ch.orellbuehler.acolyte.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import ch.orellbuehler.acolyte.feature.downloads.CalendarFollows
import ch.orellbuehler.acolyte.feature.downloads.CalendarGroup
import ch.orellbuehler.acolyte.feature.downloads.CalendarLayout
import ch.orellbuehler.acolyte.feature.downloads.CalendarRules
import ch.orellbuehler.acolyte.feature.downloads.CalendarState
import ch.orellbuehler.acolyte.feature.downloads.CalendarType
import ch.orellbuehler.acolyte.feature.downloads.Day
import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import ch.orellbuehler.acolyte.common.R as CommonR

class CalendarViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    val presenter = sdk.calendar()

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun next() {
        viewModelScope.launch { presenter.next() }
    }

    fun previous() {
        viewModelScope.launch { presenter.previous() }
    }

    fun today() {
        viewModelScope.launch { presenter.goToday() }
    }

    fun follow(
        group: CalendarGroup,
        on: Boolean,
    ) {
        viewModelScope.launch { presenter.follow(group, on) }
    }

    fun search(group: CalendarGroup) {
        viewModelScope.launch { presenter.search(group) }
    }

    fun searchMissing() {
        viewModelScope.launch { presenter.searchMissing() }
    }

    fun describe(
        group: CalendarGroup,
        done: (Map<String, String>) -> Unit,
    ) {
        viewModelScope.launch { done(presenter.describe(group)) }
    }

    suspend fun watch() = presenter.watch()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: (() -> Unit)?,
    onOpenTitle: (DownloadTitleLink) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { CalendarViewModel(sdk) }
    val state by viewModel.presenter.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    var filtering by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val notice = state.notice
    val noticeLine = notice?.let { calendarNoticeText(it) }
    LaunchedEffect(notice?.id) {
        if (notice != null && noticeLine != null) {
            snackbar.showSnackbar(noticeLine)
            viewModel.presenter.consumeNotice(notice.id)
        }
    }
    val error = state.actionError?.let { errorText(it) + " " + it.message }
    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(error)
            viewModel.presenter.clearActionError()
        }
    }
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            title = stringResource(R.string.section_calendar),
            onBack = onBack,
            actions = {
                val month = state.view == CalendarLayout.MONTH
                IconButton(onClick = { viewModel.presenter.setView(if (month) CalendarLayout.AGENDA else CalendarLayout.MONTH) }) {
                    Icon(
                        if (month) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.CalendarMonth,
                        contentDescription =
                            stringResource(
                                if (month) R.string.downloads_calendar_agenda else R.string.downloads_calendar_month,
                            ),
                    )
                }
                IconButton(
                    onClick = viewModel::today,
                ) { Icon(Icons.Rounded.Today, contentDescription = stringResource(CommonR.string.downloads_calendar_today)) }
                IconButton(onClick = { filtering = true }) {
                    Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.downloads_calendar_filter))
                }
            },
        ) { padding ->
            StatusContent(
                status = state.status,
                isEmpty = false,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            ) {
                CalendarBody(state, viewModel) { open = it.key }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (filtering) CalendarFilterSheet(state, viewModel) { filtering = false }
    open?.let { key ->
        val group = state.group(key)
        if (group == null) {
            LaunchedEffect(key) { open = null }
        } else {
            EntrySheet(group, state, viewModel, onOpenTitle) { open = null }
        }
    }
}

@Composable
private fun CalendarBody(
    state: CalendarState,
    viewModel: CalendarViewModel,
    onOpen: (CalendarGroup) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        item(key = "month") {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previous, enabled = state.canPrevious) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = stringResource(CommonR.string.downloads_calendar_previous),
                    )
                }
                Text(
                    calendarMonthTitle(state.month),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::next, enabled = state.canNext) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(CommonR.string.downloads_calendar_next),
                    )
                }
            }
        }
        if (state.hidden > 0) {
            item(key = "hidden") {
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(CommonR.string.downloads_calendar_hidden, state.hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.presenter.setFollows(CalendarFollows.LIBRARY) }) {
                        Text(stringResource(CommonR.string.downloads_calendar_show_library))
                    }
                }
            }
        }
        if (state.view == CalendarLayout.MONTH) {
            item(key = "grid") { MonthGrid(state, viewModel.presenter::select) }
            val selected = state.selected
            if (selected != null) {
                item(key = "selected") { DayHeading(selected, state.today) }
                val groups = state.selectedGroups
                if (groups.isEmpty()) {
                    item(key = "none") {
                        Text(
                            stringResource(R.string.downloads_calendar_day_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.Screen),
                        )
                    }
                }
                items(groups, key = { it.key }) { g -> EntryRow(g, state, viewModel) { onOpen(g) } }
            }
        } else {
            if (state.days.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.XLarge)) {
                        EmptyView(
                            title = stringResource(CommonR.string.downloads_calendar_empty_title),
                            message = stringResource(CommonR.string.downloads_calendar_empty),
                            icon = Icons.Rounded.CalendarMonth,
                        )
                    }
                }
            }
            state.days.forEach { day ->
                item(key = "day-${day.day.key}") { DayHeading(day.day, state.today) }
                items(day.groups, key = { it.key }) { g -> EntryRow(g, state, viewModel) { onOpen(g) } }
            }
        }
    }
}

@Composable
private fun DayHeading(
    day: Day,
    today: Day,
) {
    val text = calendarDayHeading(day, today)
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = if (day == today) MaterialTheme.acolyte.accentText else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = Spacing.Screen).padding(top = Spacing.Large, bottom = Spacing.XSmall),
    )
}

@Composable
private fun MonthGrid(
    state: CalendarState,
    onSelect: (Day) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(Modifier.padding(horizontal = Spacing.Screen)) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        state.cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    val selected = cell.day == state.selected
                    Column(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(Radius.Button))
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .let {
                                if (cell.today &&
                                    !selected
                                ) {
                                    it.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(Radius.Button))
                                } else {
                                    it
                                }
                            }.clickable(enabled = cell.inMonth) { onSelect(cell.day) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            cell.day.day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (cell.count > 0) FontWeight.SemiBold else FontWeight.Normal,
                            color =
                                when {
                                    selected -> MaterialTheme.colorScheme.onPrimary
                                    !cell.inMonth -> MaterialTheme.acolyte.textFaint
                                    else -> MaterialTheme.colorScheme.onBackground
                                },
                        )
                        val tone = cell.tone
                        Box(
                            Modifier
                                .padding(top = 2.dp)
                                .size(6.dp)
                                .background(
                                    if (tone != null &&
                                        cell.inMonth
                                    ) {
                                        (if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.acolyte.dot(tone))
                                    } else {
                                        Color.Transparent
                                    },
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    g: CalendarGroup,
    state: CalendarState,
    viewModel: CalendarViewModel,
    onOpen: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServerImage(
            source = g.art,
            contentDescription = null,
            placeholderName = g.title,
            modifier = Modifier.width(96.dp).height(54.dp).clip(RoundedCornerShape(Radius.Poster / 2)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${whenText(g)} · ${tagText(g)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.acolyte.accentText)
            Text(g.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            captionLines(g).forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusChip(dotText(g.dot), g.dot.tone)
        }
        val busy = "follow-${g.lead.titleId}" in state.busy
        IconButton(onClick = { viewModel.follow(g, !g.following) }, enabled = !busy) {
            Icon(
                if (g.following) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                contentDescription =
                    stringResource(
                        if (g.following) R.string.downloads_calendar_unfollow else R.string.downloads_calendar_follow,
                        g.lead.name,
                    ),
                tint = if (g.following) MaterialTheme.acolyte.accentText else muted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntrySheet(
    g: CalendarGroup,
    state: CalendarState,
    viewModel: CalendarViewModel,
    onOpenTitle: (DownloadTitleLink) -> Unit,
    onDismiss: () -> Unit,
) {
    var texts by remember(g.key) { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(g.key) { viewModel.describe(g) { texts = it } }
    val link = viewModel.presenter.link(g)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            ServerImage(
                source = g.art,
                contentDescription = null,
                placeholderName = g.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(Radius.Card)),
            )
            Text("${whenText(g)} · ${tagText(g)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.acolyte.accentText)
            Text(g.title, style = MaterialTheme.typography.titleLarge)
            if (g.kind == "episode") {
                val today = state.today
                g.entries.forEach { e ->
                    if (g.entries.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(CommonR.string.downloads_calendar_season_episode, e.season, e.episode),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            val dot = CalendarRules.dot(e, today)
                            StatusChip(dotText(dot), dot.tone)
                        }
                    } else {
                        Text(
                            stringResource(CommonR.string.downloads_calendar_season_episode, e.season, e.episode),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    val name = CalendarRules.episodeName(e.episodeName)
                    if (name.isNotEmpty()) Text(name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        texts[CalendarRules.entryKey(e)] ?: stringResource(R.string.downloads_calendar_no_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                captionLines(
                    g,
                ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (g.kind != "album") {
                    Text(
                        texts[CalendarRules.entryKey(g.lead)] ?: stringResource(R.string.downloads_calendar_no_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (g.followers.isNotEmpty()) {
                Text(
                    stringResource(R.string.downloads_calendar_followed_by, g.followers.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(dotText(g.dot), g.dot.tone)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (state.canSearch && g.canSearch) {
                    Button(
                        onClick = { viewModel.search(g) },
                        enabled = g.key !in state.busy,
                    ) { Text(stringResource(R.string.downloads_calendar_search_now)) }
                }
                if (link != null) {
                    OutlinedButton(onClick = {
                        onDismiss()
                        onOpenTitle(link)
                    }) {
                        Text(
                            stringResource(
                                if (link.kind ==
                                    "show"
                                ) {
                                    R.string.downloads_calendar_go_show
                                } else {
                                    R.string.downloads_calendar_go_movie
                                },
                            ),
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.downloads_close)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarFilterSheet(
    state: CalendarState,
    viewModel: CalendarViewModel,
    onDismiss: () -> Unit,
) {
    val p = viewModel.presenter
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            ChipGroup(stringResource(R.string.downloads_calendar_type)) {
                listOfNotNull(
                    CalendarType.ALL to CommonR.string.downloads_calendar_all,
                    CalendarType.MOVIES to CommonR.string.downloads_calendar_movies,
                    CalendarType.SHOWS to CommonR.string.downloads_calendar_shows,
                    if (state.musicOn) CalendarType.MUSIC to CommonR.string.downloads_calendar_music else null,
                ).forEach { (t, label) -> FilterChip(state.type == t, { p.setType(t) }, label = { Text(stringResource(label)) }) }
            }
            Text(
                stringResource(R.string.downloads_calendar_which),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOfNotNull(
                Triple(CalendarFollows.MINE, CommonR.string.downloads_calendar_mine, R.string.downloads_calendar_mine_hint),
                if (state.people >
                    1
                ) {
                    Triple(CalendarFollows.EVERYONE, CommonR.string.downloads_calendar_everyone, R.string.downloads_calendar_everyone_hint)
                } else {
                    null
                },
                Triple(CalendarFollows.LIBRARY, CommonR.string.downloads_calendar_library, R.string.downloads_calendar_library_hint),
            ).forEach { (f, label, hint) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            p.setFollows(f)
                        }.padding(vertical = Spacing.XSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(selected = state.follows == f, onClick = { p.setFollows(f) })
                    Column {
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.canSearch && state.missing > 0) {
                OutlinedButton(onClick = {
                    viewModel.searchMissing()
                    onDismiss()
                }, enabled = "missing" !in state.busy) { Text(stringResource(R.string.downloads_calendar_search_missing, state.missing)) }
            }
            Button(onClick = onDismiss) { Text(stringResource(R.string.downloads_history_done)) }
        }
    }
}
