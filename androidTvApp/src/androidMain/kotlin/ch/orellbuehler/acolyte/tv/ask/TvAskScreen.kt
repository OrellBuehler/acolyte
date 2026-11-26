package ch.orellbuehler.acolyte.tv.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.requests.AskOutcome
import ch.orellbuehler.acolyte.feature.requests.AskSeasonRow
import ch.orellbuehler.acolyte.feature.requests.AskSheetState
import ch.orellbuehler.acolyte.feature.requests.PillNote
import ch.orellbuehler.acolyte.feature.requests.ShowMode
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvChip
import ch.orellbuehler.acolyte.tv.components.TvErrorView
import ch.orellbuehler.acolyte.tv.components.TvLoadingView
import ch.orellbuehler.acolyte.tv.components.TvModeTile
import ch.orellbuehler.acolyte.tv.components.TvToggleRow
import ch.orellbuehler.acolyte.tv.components.requestFocusSafely
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk

@Composable
fun TvAskScreen(
    kind: String,
    tmdb: String,
    fourK: Boolean,
) {
    val sdk = LocalSdk.current
    val navigator = LocalTvNavigator.current
    val viewModel = viewModel(key = "ask-$kind-$tmdb-$fourK") { TvAskViewModel(sdk, kind, tmdb, fourK) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvAskContent(state, viewModel, viewModel::retry, navigator::back)
}

@Composable
fun TvAskContent(
    state: AskSheetState,
    actions: TvAskActions,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val outcome = state.outcome
    Column(Modifier.fillMaxSize().background(TvColors.background)) {
        when {
            outcome != null -> Done(state, outcome, onClose)
            state.status.phase == Phase.LOADING -> TvLoadingView()
            state.status.phase == Phase.FAILED -> state.status.error?.let { TvErrorView(it, onRetry = onRetry) }
            else -> Form(state, actions, onClose)
        }
    }
}

@Composable
private fun Done(
    state: AskSheetState,
    outcome: AskOutcome,
    onDone: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val what = if (state.isShow && state.mode != ShowMode.ALL) "${state.name} (${modeWords(state)})" else state.name
    Column(
        Modifier.fillMaxSize().padding(TvMetrics.marginX),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = TvColors.status.ok, modifier = Modifier.size(48.dp))
        Text(tvOutcomeTitle(outcome, what, state.outcomeFourK), style = TvType.title3, color = TvColors.text, textAlign = TextAlign.Center)
        Text(
            tvOutcomeMessage(outcome),
            style = TvType.footnote,
            color = TvColors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
        )
        TvActionButton(stringResource(R.string.tv_requests_done), onDone, Modifier.focusRequester(focus), prominent = true)
    }
    LaunchedEffect(Unit) { focus.requestFocusSafely() }
}

@Composable
private fun Form(
    state: AskSheetState,
    viewModel: TvAskActions,
    onCancel: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TvMetrics.marginX, vertical = TvMetrics.marginY),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.tv_requests_ask_title, if (state.year > 0) "${state.name} (${state.year})" else state.name),
                style = TvType.title,
                color = TvColors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                hint(state),
                style = TvType.footnote,
                color = TvColors.textMuted,
                modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
            )
            if (state.neverAdd) {
                Text(stringResource(R.string.tv_requests_error_never_add), style = TvType.footnote, color = TvColors.status.failText)
            }
        }
        if (state.isShow) {
            Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter)) {
                TvModeTile(
                    Icons.Rounded.Layers,
                    stringResource(R.string.tv_requests_mode_all),
                    stringResource(R.string.tv_requests_mode_all_hint),
                    state.mode == ShowMode.ALL,
                    onClick = { viewModel.setMode(ShowMode.ALL) },
                )
                TvModeTile(
                    Icons.Rounded.Checklist,
                    stringResource(R.string.tv_requests_mode_choose),
                    stringResource(R.string.tv_requests_mode_choose_hint),
                    state.mode == ShowMode.CHOOSE,
                    onClick = { viewModel.setMode(ShowMode.CHOOSE) },
                    enabled = state.chooseAllowed,
                )
                TvModeTile(
                    Icons.Rounded.CalendarMonth,
                    stringResource(R.string.tv_requests_mode_future),
                    stringResource(R.string.tv_requests_mode_future_hint),
                    state.mode == ShowMode.FUTURE,
                    onClick = { viewModel.setMode(ShowMode.FUTURE) },
                )
            }
            if (state.mode == ShowMode.CHOOSE) {
                Column(Modifier.focusGroup().widthIn(max = TvMetrics.readableWidth)) {
                    state.seasons.forEach { season -> SeasonRow(season, viewModel) }
                }
            }
        }
        if (state.showsQuality) {
            Column(Modifier.focusGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.tv_requests_quality), style = TvType.headline, color = TvColors.text)
                LazyRow(
                    modifier = Modifier.focusRestorer(),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(vertical = TvMetrics.focusPad),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.pills, key = { it.key }) { pill ->
                        val note =
                            when (pill.note) {
                                PillNote.IN_LIBRARY -> stringResource(R.string.tv_requests_pill_in_library)
                                PillNote.REQUESTED -> stringResource(R.string.tv_requests_pill_requested)
                                PillNote.NONE -> null
                            }
                        TvChip(
                            title = listOfNotNull(tvQualityName(pill.line, pill.name), note).joinToString(" · "),
                            selected = pill.key == state.selected,
                            enabled = !pill.disabled,
                            onClick = { viewModel.select(pill.key) },
                        )
                    }
                }
                state.alsoPills.forEach { pill ->
                    TvToggleRow(
                        title = stringResource(R.string.tv_requests_also_ask, tvQualityName(pill.line, pill.name)),
                        checked = pill.key in state.also,
                        onChange = { viewModel.toggleAlso(pill.key) },
                        modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
                    )
                }
            }
        }
        TvToggleRow(
            title = stringResource(R.string.tv_requests_follow),
            hint = stringResource(R.string.tv_requests_follow_hint),
            checked = state.follow,
            onChange = viewModel::setFollow,
            modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
        )
        state.error?.let { Text(tvAskErrorText(it), style = TvType.callout, color = TvColors.status.failText) }
        Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvActionButton(stringResource(R.string.tv_common_cancel), onCancel)
            TvActionButton(
                title = submitLabel(state),
                onClick = viewModel::submit,
                icon = Icons.Rounded.Add,
                prominent = true,
                enabled = state.canSubmit,
            )
        }
    }
}

@Composable
private fun SeasonRow(
    season: AskSeasonRow,
    viewModel: TvAskActions,
) {
    val note =
        when {
            season.kept -> stringResource(R.string.tv_requests_season_kept)
            season.complete -> stringResource(R.string.tv_requests_season_in_library)
            season.picked > 0 -> stringResource(R.string.tv_requests_season_picked, season.picked)
            else -> null
        }
    TvToggleRow(
        title = season.name.ifEmpty { tvAskSeasonName(season.number) },
        hint = note,
        checked = season.ticked || !season.available,
        enabled = season.available,
        onChange = { viewModel.toggleSeason(season.number) },
    )
}

@Composable
private fun modeWords(state: AskSheetState): String =
    when (state.mode) {
        ShowMode.FUTURE -> stringResource(R.string.tv_requests_seasons_future)
        ShowMode.ALL -> stringResource(R.string.tv_requests_seasons_whole)
        ShowMode.CHOOSE -> chooseSummary(state)
    }

@Composable
private fun chooseSummary(state: AskSheetState): String {
    val parts = mutableListOf<String>()
    if (state.pickedSeasons.isNotEmpty()) parts += tvSeasonsText(state.pickedSeasons)
    if (state.pickedEpisodes > 0) {
        parts += pluralStringResource(R.plurals.tv_requests_episodes_count, state.pickedEpisodes, state.pickedEpisodes)
    }
    return parts.joinToString(" + ")
}

@Composable
private fun submitLabel(state: AskSheetState): String =
    when {
        !state.isShow -> stringResource(R.string.tv_requests_ask_for, state.name)
        state.mode == ShowMode.ALL -> stringResource(R.string.tv_requests_ask_all_seasons)
        state.mode == ShowMode.FUTURE -> stringResource(R.string.tv_requests_ask_future)
        state.pickedSeasons.isEmpty() && state.pickedEpisodes == 0 -> stringResource(R.string.tv_requests_pick_season)
        else -> stringResource(R.string.tv_requests_ask_for, chooseSummary(state))
    }

@Composable
private fun hint(state: AskSheetState): String {
    val brief = state.brief
    return when {
        brief != null && state.isShow ->
            stringResource(
                R.string.tv_requests_ask_already_show,
                tvSeasonsText(brief.seasons),
                brief.stateLabel,
            )
        brief != null && state.joining -> stringResource(R.string.tv_requests_ask_already, brief.stateLabel)
        else -> stringResource(R.string.tv_requests_ask_hear)
    }
}
