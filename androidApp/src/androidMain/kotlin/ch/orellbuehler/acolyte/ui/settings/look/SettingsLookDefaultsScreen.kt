package ch.orellbuehler.acolyte.ui.settings.look

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookDefaultsState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsLookDefaultsScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsLookDefaultsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.settings_look_defaults_saved)
    LaunchedEffect(state.savedCount) { if (state.savedCount > 0) snackbar.showSnackbar(savedText) }
    val editing = state.canEdit && state.dirty
    LookLeaveGuard(editing, onBack) { back ->
        ScreenScaffold(title = stringResource(R.string.settings_page_title_defaults), onBack = back) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                StatusContent(
                    status = state.status,
                    isEmpty = state.isEmpty,
                    onRefresh = viewModel::refresh,
                    empty = {
                        EmptyView(
                            title = stringResource(R.string.settings_look_defaults_empty),
                            message = stringResource(R.string.settings_look_defaults_empty_text),
                        )
                    },
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        if (!state.canEdit) {
                            item(key = "read-only") {
                                Hint(
                                    stringResource(R.string.settings_look_defaults_read_only),
                                    Modifier.padding(horizontal = Spacing.Screen),
                                )
                            }
                        }
                        state.readError?.let { item(key = "read-error") { ErrorBanner(it) } }
                        if (state.showMovies) item(key = "movies") { KindCard(state, viewModel, show = false) }
                        if (state.showShows) item(key = "shows") { KindCard(state, viewModel, show = true) }
                        item(key = "footer") { Footer(state, onOpen) }
                    }
                }
                if (editing) {
                    LookSaveBar(
                        canSave = state.canSave,
                        saving = state.saving,
                        onDiscard = viewModel::discard,
                        onSave = viewModel::save,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        error = state.saveError,
                        problem = if (state.gateDaysValid) null else stringResource(R.string.settings_look_defaults_gate_days_invalid),
                    )
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (editing) 96.dp else 0.dp))
            }
        }
    }
}

@Composable
private fun KindCard(
    state: SettingsLookDefaultsState,
    viewModel: SettingsLookDefaultsViewModel,
    show: Boolean,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val first = stringResource(if (show) R.string.settings_look_defaults_first_show else R.string.settings_look_defaults_first_movie)
    val folders = listOf(0L to first) + state.folderOptions(show).map { it.id to it.name }
    val ownQuality = stringResource(R.string.settings_look_defaults_folder_quality)
    val rules = listOf(0L to ownQuality) + state.ruleOptions(show).map { it.id to it.name }
    SettingsCard(stringResource(if (show) R.string.settings_look_defaults_shows else R.string.settings_look_defaults_movies)) {
        PickerField(
            stringResource(R.string.settings_look_defaults_folder),
            folders,
            state.folderId(show),
            { viewModel.setFolder(show, it) },
            enabled = state.canEdit,
        )
        PickerField(
            stringResource(R.string.settings_look_defaults_quality),
            rules,
            state.rulesId(show),
            { viewModel.setRules(show, it) },
            enabled = state.canEdit,
        )
        SwitchRow(
            stringResource(R.string.settings_look_defaults_keep),
            state.keepUpToDate(show),
            { viewModel.setKeepUpToDate(show, it) },
            hint = if (show) null else stringResource(R.string.settings_look_defaults_keep_hint),
            enabled = state.canEdit,
        )
        SwitchRow(
            stringResource(R.string.settings_look_defaults_search),
            state.searchOnAdd(show),
            { viewModel.setSearchOnAdd(show, it) },
            hint = if (show) stringResource(R.string.settings_look_defaults_search_hint) else null,
            enabled = state.canEdit,
        )
        if ((show && state.showShowSettings) || (!show && state.showGate)) {
            TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.settings_look_advanced)) }
            if (advanced) {
                if (show) ShowAdvanced(state, viewModel) else GateAdvanced(state, viewModel)
            }
        }
    }
}

@Composable
private fun GateAdvanced(
    state: SettingsLookDefaultsState,
    viewModel: SettingsLookDefaultsViewModel,
) {
    PickerField(
        stringResource(R.string.settings_look_defaults_gate),
        state.gateChoices.map { it to choiceLabel(gateLabels, it) },
        state.gate.releaseGate,
        viewModel::setReleaseGate,
        enabled = state.canEdit,
        hint = choiceHint(gateHints, state.gate.releaseGate),
    )
    OutlinedTextField(
        value = state.gateDaysText,
        onValueChange = viewModel::setGateDays,
        label = { Text(stringResource(R.string.settings_look_defaults_gate_days)) },
        supportingText = { Text(stringResource(R.string.settings_look_defaults_gate_days_hint)) },
        isError = !state.gateDaysValid,
        enabled = state.canEdit,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ShowAdvanced(
    state: SettingsLookDefaultsState,
    viewModel: SettingsLookDefaultsViewModel,
) {
    PickerField(
        stringResource(R.string.settings_look_defaults_seasons),
        state.seasonChoices.map { it to choiceLabel(seasonLabels, it) },
        state.shows.seasonChoice,
        viewModel::setSeasonChoice,
        enabled = state.canEdit,
        hint = choiceHint(seasonHints, state.shows.seasonChoice),
    )
    PickerField(
        stringResource(R.string.settings_look_defaults_new_seasons),
        state.laterSeasonChoices.map { it to choiceLabel(newSeasonLabels, it) },
        state.shows.newSeasons,
        viewModel::setNewSeasons,
        enabled = state.canEdit,
    )
    PickerField(
        stringResource(R.string.settings_look_defaults_order),
        state.orderChoices.map { it to choiceLabel(orderLabels, it) },
        state.shows.episodeOrder,
        viewModel::setEpisodeOrder,
        enabled = state.canEdit,
        hint = choiceHint(orderHints, state.shows.episodeOrder),
    )
    PickerField(
        stringResource(R.string.settings_look_defaults_details),
        state.sourceChoices.map { it to choiceLabel(sourceLabels, it) },
        state.shows.detailsSource,
        viewModel::setDetailsSource,
        enabled = state.canEdit,
    )
}

@Composable
private fun Footer(
    state: SettingsLookDefaultsState,
    onOpen: (Any) -> Unit,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen)) {
        Hint(stringResource(R.string.settings_look_defaults_footer))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (state.canOpenFolders) {
                TextButton(onClick = { onOpen(Routes.settingsLibraryFolders) }) {
                    Text(stringResource(R.string.settings_page_library_folders))
                }
            }
            if (state.canOpenRequests) {
                TextButton(onClick = { onOpen(Routes.settingsRequests) }) { Text(stringResource(R.string.settings_page_requests)) }
            }
            if (state.canOpenOutsideLists) {
                TextButton(onClick = { onOpen(Routes.settingsOutsideLists) }) { Text(stringResource(R.string.settings_page_outside_lists)) }
            }
        }
    }
}

private val gateLabels =
    mapOf(
        "announced" to R.string.settings_look_defaults_gate_announced,
        "in_cinemas" to R.string.settings_look_defaults_gate_in_cinemas,
        "released" to R.string.settings_look_defaults_gate_released,
    )

private val gateHints =
    mapOf(
        "announced" to R.string.settings_look_defaults_gate_announced_hint,
        "in_cinemas" to R.string.settings_look_defaults_gate_in_cinemas_hint,
        "released" to R.string.settings_look_defaults_gate_released_hint,
    )

private val seasonLabels =
    mapOf(
        "all" to R.string.settings_look_defaults_seasons_all,
        "future" to R.string.settings_look_defaults_seasons_future,
        "latest" to R.string.settings_look_defaults_seasons_latest,
        "none" to R.string.settings_look_defaults_seasons_none,
    )

private val seasonHints =
    mapOf(
        "all" to R.string.settings_look_defaults_seasons_all_hint,
        "future" to R.string.settings_look_defaults_seasons_future_hint,
        "latest" to R.string.settings_look_defaults_seasons_latest_hint,
        "none" to R.string.settings_look_defaults_seasons_none_hint,
    )

private val newSeasonLabels =
    mapOf(
        "all" to R.string.settings_look_defaults_new_seasons_all,
        "none" to R.string.settings_look_defaults_new_seasons_none,
    )

private val orderLabels =
    mapOf(
        "aired" to R.string.settings_look_defaults_order_aired,
        "daily" to R.string.settings_look_defaults_order_daily,
        "absolute" to R.string.settings_look_defaults_order_absolute,
    )

private val orderHints =
    mapOf(
        "aired" to R.string.settings_look_defaults_order_aired_hint,
        "daily" to R.string.settings_look_defaults_order_daily_hint,
        "absolute" to R.string.settings_look_defaults_order_absolute_hint,
    )

private val sourceLabels =
    mapOf(
        "tmdb" to R.string.settings_look_defaults_source_tmdb,
        "tvmaze" to R.string.settings_look_defaults_source_tvmaze,
        "tvdb" to R.string.settings_look_defaults_source_tvdb,
    )

@Composable
private fun choiceLabel(
    labels: Map<String, Int>,
    value: String,
): String = labels[value]?.let { stringResource(it) } ?: value

@Composable
private fun choiceHint(
    hints: Map<String, Int>,
    value: String,
): String? = hints[value]?.let { stringResource(it) }
