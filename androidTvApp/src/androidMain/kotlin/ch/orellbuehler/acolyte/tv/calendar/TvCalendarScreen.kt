package ch.orellbuehler.acolyte.tv.calendar

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.downloads.CalendarFollows
import ch.orellbuehler.acolyte.feature.downloads.CalendarState
import ch.orellbuehler.acolyte.feature.downloads.CalendarType
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvBackdropCard
import ch.orellbuehler.acolyte.tv.components.TvBackdropLayer
import ch.orellbuehler.acolyte.tv.components.TvChip
import ch.orellbuehler.acolyte.tv.components.TvChipInfo
import ch.orellbuehler.acolyte.tv.components.TvChipRow
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvShelf
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.downloads.calendarDayHeading
import ch.orellbuehler.acolyte.ui.downloads.calendarFollowsLabel
import ch.orellbuehler.acolyte.ui.downloads.calendarHeadline
import ch.orellbuehler.acolyte.ui.downloads.calendarMonthTitle
import ch.orellbuehler.acolyte.ui.downloads.calendarTypeLabel
import ch.orellbuehler.acolyte.ui.downloads.captionLines
import ch.orellbuehler.acolyte.ui.downloads.dotText
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun TvCalendarScreen(account: Account) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { TvCalendarViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    TvCalendarContent(state, viewModel)
}

@Composable
fun TvCalendarContent(
    state: CalendarState,
    actions: TvCalendarActions,
) {
    val navigator = LocalTvNavigator.current
    var ambient by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        TvBackdropLayer(ambient, blurred = true)
        TvStatusContent(status = state.status, isEmpty = false, reload = actions::refresh) {
            LazyColumn(
                contentPadding = PaddingValues(top = TvMetrics.focusPad, bottom = TvMetrics.marginY),
                verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap),
            ) {
                state.status.error?.let { error ->
                    item(key = "error") { TvNoticeBanner(errorText(error), Modifier.tvPageInset()) }
                }
                item(key = "month") { MonthBar(state, actions) }
                item(key = "type") {
                    TvChipRow {
                        items(CalendarType.entries.filter { it != CalendarType.MUSIC || state.musicOn }, key = { it.name }) { type ->
                            TvChip(calendarTypeLabel(type), type == state.type, { actions.setType(type) })
                        }
                    }
                }
                item(key = "follows") {
                    TvChipRow {
                        items(
                            CalendarFollows.entries.filter { it != CalendarFollows.EVERYONE || state.people > 1 },
                            key = { it.name },
                        ) { follows ->
                            TvChip(calendarFollowsLabel(follows), follows == state.follows, { actions.setFollows(follows) })
                        }
                    }
                }
                if (state.hidden > 0) {
                    item(key = "hidden") {
                        Row(
                            Modifier.tvPageInset().focusGroup(),
                            horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(CommonR.string.downloads_calendar_hidden, state.hidden),
                                style = TvType.footnote,
                                color = TvColors.textMuted,
                            )
                            TvActionButton(
                                stringResource(CommonR.string.downloads_calendar_show_library),
                                { actions.setFollows(CalendarFollows.LIBRARY) },
                                icon = Icons.Rounded.VideoLibrary,
                            )
                        }
                    }
                }
                if (state.days.isEmpty()) {
                    item(key = "empty") {
                        TvEmptyView(
                            title = stringResource(CommonR.string.downloads_calendar_empty_title),
                            message = stringResource(CommonR.string.downloads_calendar_empty),
                            icon = Icons.Rounded.CalendarMonth,
                            modifier = Modifier.fillMaxWidth().padding(vertical = TvMetrics.sectionGap),
                        )
                    }
                }
                items(state.days, key = { it.day.key }) { day ->
                    TvShelf(calendarDayHeading(day.day, state.today)) {
                        items(day.groups, key = { it.key }) { group ->
                            TvBackdropCard(
                                title = group.title,
                                subtitle = calendarHeadline(group),
                                detail = captionLines(group).joinToString(" · "),
                                chip = TvChipInfo(dotText(group.dot), group.dot.tone),
                                image = group.art,
                                onFocus = { ambient = group.art },
                                onClick = {
                                    actions.link(group)?.let { navigator.openTitle(it.kind, it.tmdb, it.libraryId) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthBar(
    state: CalendarState,
    actions: TvCalendarActions,
) {
    Row(
        Modifier.tvPageInset().padding(vertical = TvMetrics.focusPad).focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvActionButton(
            stringResource(CommonR.string.downloads_calendar_previous),
            actions::previous,
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            enabled = state.canPrevious,
        )
        Text(calendarMonthTitle(state.month), style = TvType.title3, color = TvColors.text)
        TvActionButton(
            stringResource(CommonR.string.downloads_calendar_next),
            actions::next,
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            enabled = state.canNext,
        )
        TvActionButton(stringResource(CommonR.string.downloads_calendar_today), actions::today, icon = Icons.Rounded.Today)
    }
}
