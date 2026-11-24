package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.AddTitlePresenter
import ch.orellbuehler.acolyte.feature.title.BatchLine
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun AddTitleContent(
    presenter: AddTitlePresenter,
    viewModel: TitleViewModel,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    FeedbackEffect(state.feedback)
    LaunchedEffect(state.addedId, state.openPick) {
        if (state.addedId > 0) {
            viewModel.close()
            if (state.openPick) {
                viewModel.openReleases(
                    ch.orellbuehler.acolyte.feature.title
                        .ReleaseScope(ch.orellbuehler.acolyte.feature.title.ReleaseScopeKind.WHOLE),
                )
            }
        }
    }
    val title =
        when {
            state.isBatch && state.manage -> stringResource(R.string.title_add_all_of, state.name)
            state.isBatch -> stringResource(R.string.title_request_all_of, state.name)
            else -> stringResource(R.string.title_add_name, state.name)
        }
    SheetTitle(title)
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        if (state.manage) {
            when (state.status.phase) {
                Phase.LOADING -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.title_add_loading))
                    }
                    return@Column
                }
                Phase.FAILED -> {
                    Text(stringResource(R.string.title_add_folders_failed), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { viewModel.launch { presenter.load() } }) { Text(stringResource(R.string.common_retry)) }
                    return@Column
                }
                Phase.READY -> Unit
            }
            if (state.defaultsLate) {
                Text(
                    stringResource(R.string.title_add_defaults_late),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.acolyte.waitText,
                )
            }
            if (state.noFolder) {
                Text(
                    stringResource(if (state.isShow) R.string.title_add_no_folder_show else R.string.title_add_no_folder_movie),
                    color = MaterialTheme.acolyte.failText,
                )
            } else if (state.folders.isEmpty() && state.handsOff) {
                Text(stringResource(R.string.title_add_hands_off), style = MaterialTheme.typography.bodySmall)
            } else {
                ChoiceRow(
                    stringResource(R.string.title_folder),
                    state.folders
                        .firstOrNull { it.id == state.folderId }
                        ?.let { "${it.shown} (${it.path})" }
                        .orEmpty(),
                    state.folders.map { it.id to "${it.shown} (${it.path})" },
                    state.folderId,
                ) { presenter.setFolder(it) }
            }
            ChoiceRow(
                stringResource(R.string.title_pick_rules),
                state.rules
                    .firstOrNull { it.id == state.rulesId }
                    ?.name
                    .orEmpty(),
                state.rules.map { it.id to it.name },
                state.rulesId,
                hint = stringResource(R.string.title_pick_rules_hint),
            ) { presenter.setRules(it) }
            if (!state.isShow) {
                Text(stringResource(R.string.title_release_gate), style = MaterialTheme.typography.titleSmall)
                GateChoices(state.gate, presenter::setGate)
            } else if (!state.isBatch) {
                ShowChoices(state, presenter)
            }
            SettingSwitch(
                stringResource(R.string.title_keep_up_to_date),
                stringResource(if (state.isShow) R.string.title_add_keep_hint_show else R.string.title_add_keep_hint),
                state.keepUpToDate,
            ) { presenter.setKeep(it) }
            if (!state.choose) {
                SettingSwitch(
                    stringResource(R.string.title_add_search),
                    stringResource(if (state.isShow) R.string.title_add_search_hint_show else R.string.title_add_search_hint),
                    state.searchOnAdd,
                ) { presenter.setSearch(it) }
            }
            if (!state.isBatch) {
                SettingSwitch(stringResource(R.string.title_add_second), stringResource(R.string.title_add_second_hint), state.secondOn) {
                    presenter.setSecond(it)
                }
                if (state.secondOn) {
                    OutlinedTextField(
                        value = state.secondLabel,
                        onValueChange = { presenter.setSecondLabel(it.take(40)) },
                        label = { Text(stringResource(R.string.title_version_name)) },
                        isError = state.secondProblem != null,
                        supportingText = { state.secondProblem?.let { Text(labelProblemText(it)) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ChoiceRow(
                        stringResource(R.string.title_version_rules_label),
                        state.rules
                            .firstOrNull { it.id == state.secondRulesId }
                            ?.name
                            .orEmpty(),
                        state.rules.map { it.id to it.name },
                        state.secondRulesId,
                    ) { presenter.setSecondRules(it) }
                    ChoiceRow(
                        stringResource(R.string.title_version_folder),
                        state.folders
                            .firstOrNull { it.id == state.secondFolderId }
                            ?.shown
                            .orEmpty(),
                        state.folders.map { it.id to "${it.shown} (${it.path})" },
                        state.secondFolderId,
                        hint = if (state.sameFolder) stringResource(R.string.title_add_same_folder, state.secondLabel) else null,
                    ) { presenter.setSecondFolder(it) }
                }
            }
        }
        if (!state.isBatch) {
            SettingSwitch(stringResource(R.string.title_follow), stringResource(R.string.title_add_follow_hint), state.follow) {
                presenter.setFollow(it)
            }
        }
        if (state.isBatch) {
            HorizontalDivider()
            state.batch.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.card.name + if (item.card.year > 0) " (${item.card.year})" else "", modifier = Modifier.weight(1f))
                    val (text, tone) =
                        when (item.line) {
                            BatchLine.WAITING -> stringResource(R.string.title_batch_waiting) to Tone.IDLE
                            BatchLine.DONE ->
                                stringResource(if (state.manage) R.string.title_batch_added else R.string.title_batch_requested) to
                                    Tone.OK
                            BatchLine.ALREADY -> stringResource(R.string.title_batch_already) to Tone.WAIT
                            BatchLine.FAILED -> (item.error?.let { errorText(it) } ?: stringResource(R.string.error_unknown)) to Tone.FAIL
                        }
                    StatusChip(text, tone)
                }
            }
        }
        state.error?.let { ErrorBanner(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            TextButton(
                onClick = viewModel::close,
            ) { Text(stringResource(if (state.batchDone) R.string.title_done else R.string.common_cancel)) }
            if (!state.batchDone) {
                Button(onClick = { viewModel.launch { presenter.submit() } }, enabled = state.canSubmit) {
                    Text(
                        stringResource(
                            when {
                                state.isBatch && state.manage -> R.string.title_add_all
                                state.isBatch -> R.string.title_request_all
                                state.choose -> R.string.title_add_and_choose
                                else -> R.string.title_add_to_library
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowChoices(
    state: ch.orellbuehler.acolyte.feature.title.AddTitleState,
    presenter: AddTitlePresenter,
) {
    Text(stringResource(R.string.title_add_seasons), style = MaterialTheme.typography.titleSmall)
    RadioList(
        listOf(
            Triple("all", R.string.title_add_seasons_all, R.string.title_add_seasons_all_hint),
            Triple("future", R.string.title_add_seasons_future, R.string.title_add_seasons_future_hint),
            Triple("latest", R.string.title_add_seasons_latest, R.string.title_add_seasons_latest_hint),
            Triple("none", R.string.title_add_seasons_none, R.string.title_add_seasons_none_hint),
        ),
        state.seasons,
        presenter::setSeasons,
    )
    Text(stringResource(R.string.title_add_new_seasons), style = MaterialTheme.typography.titleSmall)
    SegmentRow(
        listOf("all" to stringResource(R.string.title_add_new_seasons_all), "none" to stringResource(R.string.title_add_new_seasons_none)),
        state.newSeasons,
    ) { if (!state.newSeasonsLocked) presenter.setNewSeasons(it) }
    Text(
        stringResource(if (state.newSeasonsLocked) R.string.title_add_new_seasons_locked else R.string.title_add_new_seasons_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(stringResource(R.string.title_episode_order), style = MaterialTheme.typography.titleSmall)
    RadioList(
        listOf(
            Triple("aired", R.string.title_add_order_aired, R.string.title_add_order_aired_hint),
            Triple("daily", R.string.title_add_order_daily, R.string.title_add_order_daily_hint),
            Triple("absolute", R.string.title_add_order_absolute, R.string.title_add_order_absolute_hint),
        ),
        state.order,
        presenter::setOrder,
    )
    Text(stringResource(R.string.title_add_details_from), style = MaterialTheme.typography.titleSmall)
    SegmentRow(listOf("tmdb" to "TMDB", "tvmaze" to "TVMaze", "tvdb" to "TheTVDB"), state.source) { presenter.setSource(it) }
}

@Composable
private fun RadioList(
    options: List<Triple<String, Int, Int>>,
    selected: String,
    onPick: (String) -> Unit,
) {
    Column {
        options.forEach { (value, label, hint) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == value, onClick = { onPick(value) })
                Column {
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
