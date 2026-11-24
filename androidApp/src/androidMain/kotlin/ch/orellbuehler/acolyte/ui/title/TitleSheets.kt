package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.OwnedVersion
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleSheets(
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    val sheet = viewModel.sheet ?: return
    val presenter = viewModel.presenter
    val sheetLinks = remember(links) { links.closing(viewModel::close) }
    when (sheet) {
        is TitleSheet.MarkFailed ->
            ConfirmDialog(
                stringResource(R.string.title_mark_failed),
                stringResource(R.string.title_mark_failed_confirm),
                stringResource(R.string.title_mark_failed),
                onDismiss = viewModel::close,
            ) { viewModel.launch { presenter.markFailed(sheet.downloadId) } }
        is TitleSheet.Unprotect ->
            ConfirmDialog(
                stringResource(R.string.title_unprotect),
                stringResource(R.string.title_unprotect_confirm),
                stringResource(R.string.title_unprotect),
                onDismiss = viewModel::close,
            ) { viewModel.launch { presenter.protect(sheet.fileId, false) } }
        is TitleSheet.RemoveVersion ->
            ConfirmDialog(
                stringResource(R.string.title_remove_version, sheet.version.label),
                stringResource(
                    if (state.isShow) R.string.title_remove_version_show else R.string.title_remove_version_movie,
                    sheet.version.label,
                ),
                stringResource(R.string.title_stop),
                onDismiss = viewModel::close,
            ) { viewModel.launch { presenter.removeVersion(sheet.version.id) } }
        is TitleSheet.Move ->
            ConfirmDialog(
                stringResource(R.string.title_move_to, sheet.folder.shown),
                stringResource(R.string.title_move_body) + "\n" + sheet.folder.path,
                stringResource(R.string.title_move_files),
                onDismiss = viewModel::close,
            ) { viewModel.launch { presenter.moveTo(sheet.folder.id) } }
        is TitleSheet.Trailer -> TrailerPlayer(sheet.source, viewModel, sheetLinks, onDismiss = viewModel::close)
        else -> {
            val full =
                sheet is TitleSheet.Releases || sheet is TitleSheet.Subtitles || sheet is TitleSheet.Add || sheet is TitleSheet.Leftovers
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = full)
            val host = remember(sheet) { SnackbarHostState() }
            ModalBottomSheet(onDismissRequest = viewModel::close, sheetState = sheetState) {
                CompositionLocalProvider(LocalTitleSnackbar provides host) {
                    Box {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(
                                    rememberScrollState(),
                                ).padding(bottom = Spacing.XLarge)
                                .navigationBarsPadding(),
                        ) {
                            SheetBody(sheet, state, viewModel, sheetLinks)
                        }
                        SnackbarHost(host, Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetBody(
    sheet: TitleSheet,
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    when (sheet) {
        TitleSheet.Cast -> CastList(state, links)
        is TitleSheet.Cards -> CardsGrid(sheet.title, sheet.cards, state, links)
        TitleSheet.History -> AllHistory(state, links)
        TitleSheet.Hide -> HideContent(state, viewModel)
        TitleSheet.Gate -> GateContent(state, viewModel)
        is TitleSheet.Releases -> ReleasesContent(sheet.presenter, viewModel)
        is TitleSheet.Add -> AddTitleContent(sheet.presenter, viewModel)
        is TitleSheet.Episode -> EpisodeContent(sheet.presenter, viewModel)
        is TitleSheet.SeasonTools -> SeasonToolsContent(sheet.season, state, viewModel)
        is TitleSheet.Subtitles -> SubtitlesContent(sheet.presenter, viewModel)
        is TitleSheet.Leftovers -> LeftoversContent(sheet.presenter, viewModel)
        is TitleSheet.Rename -> RenameContent(sheet.presenter, state.name, viewModel)
        is TitleSheet.Remove -> RemoveContent(sheet.presenter, viewModel)
        is TitleSheet.VersionForm -> VersionForm(sheet.version, sheet.keepFileId, state, viewModel)
        is TitleSheet.Edition -> EditionForm(sheet.fileId, sheet.current, viewModel)
        is TitleSheet.Adopt -> AdoptForm(sheet.versionId, viewModel)
        is TitleSheet.Review -> ReviewContent(sheet.review)
        else -> Unit
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirm, color = MaterialTheme.acolyte.failText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
fun SheetTitle(
    title: String,
    subtitle: String? = null,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CastList(
    state: TitleState,
    links: TitleNav,
) {
    SheetTitle(stringResource(R.string.title_cast))
    FlowRow(
        Modifier.padding(horizontal = Spacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        state.cast.forEach { person -> PersonCircle(person) { links.person(person.id) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardsGrid(
    title: String,
    cards: List<ch.orellbuehler.acolyte.api.model.TitleCard>,
    state: TitleState,
    links: TitleNav,
) {
    SheetTitle(title)
    FlowRow(
        Modifier.padding(horizontal = Spacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        cards.forEach { card -> CardItem(card, state, links) }
    }
}

@Composable
private fun AllHistory(
    state: TitleState,
    links: TitleNav,
) {
    SheetTitle(stringResource(R.string.title_history), state.name)
    if (state.history.isEmpty()) {
        Text(stringResource(R.string.title_history_empty), modifier = Modifier.padding(Spacing.Screen))
    }
    state.history.forEach { HistoryLine(it.event, it.tone) }
    TextButton(onClick = links::history, modifier = Modifier.padding(horizontal = Spacing.Small)) {
        Text(stringResource(R.string.title_open_history))
    }
}

@Composable
private fun ReviewContent(review: ch.orellbuehler.acolyte.api.model.TitleReview) {
    var shown by rememberSaveable { mutableStateOf(!review.spoiler) }
    SheetTitle(stringResource(R.string.title_review_by, review.author.ifBlank { stringResource(R.string.title_review_someone) }))
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        if (review.rating > 0) Text("★ " + TitleLogic.oneDecimal(review.rating), style = MaterialTheme.typography.titleSmall)
        if (shown) {
            Text(review.text, style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.title_review_spoiler), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { shown = true }) { Text(stringResource(R.string.title_review_show)) }
        }
    }
}

@Composable
private fun HideContent(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    var genre by rememberSaveable { mutableStateOf("") }
    var person by rememberSaveable { mutableLongStateOf(0L) }
    var onePerson by rememberSaveable { mutableStateOf(false) }
    val others = state.users
    SheetTitle(stringResource(R.string.title_hide_title, state.name), stringResource(R.string.title_hide_body))
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        if (state.hides.isNotEmpty()) {
            Text(stringResource(R.string.title_hidden_now), style = MaterialTheme.typography.titleSmall)
            state.hides.forEach { hide ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val who =
                        if (hide.userId == 0L) {
                            stringResource(R.string.title_hide_everyone)
                        } else {
                            others.firstOrNull { it.id == hide.userId }?.name ?: stringResource(R.string.title_hide_removed_person)
                        }
                    Text(
                        (if (hide.genre.isNotBlank()) hide.genre else hide.name.ifBlank { state.name }) + " · " + who,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { viewModel.launch { viewModel.presenter.unhide(hide.id) } },
                    ) { Text(stringResource(R.string.title_show_again)) }
                }
            }
        }
        if (state.genres.isNotEmpty()) {
            ChoiceRow(
                stringResource(R.string.title_hide_what),
                if (genre.isEmpty()) stringResource(R.string.title_hide_only, state.name) else hideGenre(genre, state.isShow),
                listOf(-1L to stringResource(R.string.title_hide_only, state.name)) +
                    state.genres.mapIndexed { i, g -> i.toLong() to hideGenre(g, state.isShow) },
                if (genre.isEmpty()) -1L else state.genres.indexOf(genre).toLong(),
            ) { key -> genre = if (key < 0) "" else state.genres[key.toInt()] }
        }
        if (others.size > 1) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !onePerson, onClick = { onePerson = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text(stringResource(R.string.title_hide_everyone))
                }
                SegmentedButton(selected = onePerson, onClick = { onePerson = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text(stringResource(R.string.title_hide_one_person))
                }
            }
            if (onePerson) {
                ChoiceRow(
                    stringResource(R.string.title_hide_person),
                    others.firstOrNull { it.id == person }?.name ?: stringResource(R.string.title_hide_pick_person),
                    others.map { it.id to it.name },
                    person,
                ) { person = it }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            TextButton(onClick = viewModel::close) { Text(stringResource(R.string.common_cancel)) }
            androidx.compose.material3.Button(
                onClick = {
                    val who = if (onePerson) others.firstOrNull { it.id == person }?.name.orEmpty() else ""
                    viewModel.launch { if (viewModel.presenter.hide(genre, if (onePerson) person else 0, who)) viewModel.close() }
                },
                enabled = state.busy != "hide" && (!onePerson || person > 0),
            ) { Text(stringResource(R.string.title_hide_button)) }
        }
    }
}

@Composable
private fun hideGenre(
    genre: String,
    show: Boolean,
): String = stringResource(if (show) R.string.title_hide_genre_shows else R.string.title_hide_genre_movies, genre)

@Composable
private fun GateContent(
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val owned = state.owned ?: return
    var gate by rememberSaveable { mutableStateOf(owned.releaseGate.ifBlank { "released" }) }
    var days by rememberSaveable { mutableStateOf(owned.gateDelayDays.toString()) }
    SheetTitle(stringResource(R.string.title_release_gate), state.name)
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        GateChoices(gate) { gate = it }
        OutlinedTextField(
            value = days,
            onValueChange = { days = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text(stringResource(R.string.title_gate_wait_days)) },
            supportingText = { Text(stringResource(R.string.title_gate_wait_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            TextButton(onClick = viewModel::close) { Text(stringResource(R.string.common_cancel)) }
            androidx.compose.material3.Button(onClick = {
                viewModel.launch { viewModel.presenter.setGate(gate, days.toIntOrNull() ?: 0) }
                viewModel.close()
            }) { Text(stringResource(R.string.title_save)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GateChoices(
    gate: String,
    onPick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        listOf(
            Triple("announced", R.string.title_gate_announced, R.string.title_gate_announced_hint),
            Triple("in_cinemas", R.string.title_gate_in_cinemas, R.string.title_gate_in_cinemas_hint),
            Triple("released", R.string.title_gate_released, R.string.title_gate_released_hint),
        ).forEach { (value, label, hint) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.RadioButton(selected = gate == value, onClick = { onPick(value) })
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

@Composable
private fun VersionForm(
    version: OwnedVersion?,
    keepFileId: Long,
    state: TitleState,
    viewModel: TitleViewModel,
) {
    val owned = state.owned ?: return
    val taken = owned.versions.filter { it.id != version?.id }.map { it.label }
    val second =
        remember(state.choicesLoaded) { TitleLogic.secondVersion(taken, state.kindRules, state.kindFolders, owned.libraryFolderId) }
    var label by rememberSaveable { mutableStateOf(version?.label ?: second.label) }
    var rules by rememberSaveable(state.choicesLoaded) { mutableLongStateOf(version?.pickRulesId ?: second.rulesId) }
    var folder by rememberSaveable(state.choicesLoaded) { mutableLongStateOf(version?.libraryFolderId ?: second.folderId) }
    var keep by rememberSaveable { mutableStateOf(version?.keepUpToDate ?: true) }
    var edition by rememberSaveable { mutableStateOf(version?.edition.orEmpty()) }
    var audio by rememberSaveable { mutableStateOf(version?.audioLanguages.orEmpty().joinToString(", ")) }
    var folderName by rememberSaveable { mutableStateOf("") }
    val problem = TitleLogic.labelProblem(label, taken)
    val editionProblem = TitleLogic.editionProblem(edition)
    SheetTitle(
        if (version !=
            null
        ) {
            stringResource(R.string.title_version_change, version.label)
        } else {
            stringResource(R.string.title_version_add, state.name)
        },
        if (version ==
            null
        ) {
            stringResource(if (state.isShow) R.string.title_version_intro_show else R.string.title_version_intro_movie)
        } else {
            null
        },
    )
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it.take(40) },
            label = { Text(stringResource(R.string.title_version_name)) },
            isError = problem != null && label.isNotEmpty(),
            supportingText = { problem?.takeIf { label.isNotEmpty() }?.let { Text(labelProblemText(it)) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!state.isShow && version == null) {
            OutlinedTextField(
                value = edition,
                onValueChange = { edition = it.take(40) },
                label = { Text(stringResource(R.string.title_edition)) },
                isError = editionProblem != null,
                supportingText = { editionProblem?.let { Text(labelProblemText(it)) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = audio,
            onValueChange = { audio = it },
            label = { Text(stringResource(R.string.title_audio_languages)) },
            supportingText = { Text(stringResource(R.string.title_audio_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ChoiceRow(
            stringResource(R.string.title_version_rules_label),
            state.kindRules
                .firstOrNull { it.id == rules }
                ?.name
                .orEmpty(),
            state.kindRules.map { it.id to it.name },
            rules,
            enabled = state.choicesLoaded,
        ) { rules = it }
        if (version == null) {
            ChoiceRow(
                stringResource(R.string.title_version_folder),
                state.kindFolders
                    .firstOrNull { it.id == folder }
                    ?.shown
                    .orEmpty(),
                state.kindFolders.map { it.id to "${it.shown} (${it.path})" },
                folder,
                hint = if (folder == owned.libraryFolderId) stringResource(R.string.title_version_same_folder) else null,
                enabled = state.choicesLoaded,
            ) { folder = it }
            if (!state.isShow) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it.take(200) },
                    label = { Text(stringResource(R.string.title_version_existing_folder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SettingSwitch(stringResource(R.string.title_keep_up_to_date), stringResource(R.string.title_version_keep_hint), keep) { keep = it }
        state.choicesError?.let {
            Text(
                ch.orellbuehler.acolyte.ui.components
                    .errorText(it),
                color = MaterialTheme.acolyte.failText,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            TextButton(onClick = viewModel::close) { Text(stringResource(R.string.common_cancel)) }
            androidx.compose.material3.Button(
                onClick = {
                    viewModel.launch {
                        val presenter = viewModel.presenter
                        val ok =
                            when {
                                version != null -> presenter.changeVersion(version.id, label, rules, keep, audio)
                                keepFileId > 0 -> presenter.keepAndAdd(keepFileId, label, rules, folder, keep, edition, audio, folderName)
                                else -> presenter.addVersion(label, rules, folder, keep, edition, audio, folderName)
                            }
                        if (ok) viewModel.close()
                    }
                },
                enabled = problem == null && editionProblem == null && state.busy != "version",
            ) { Text(stringResource(if (version != null) R.string.title_save else R.string.title_add_version_button)) }
        }
    }
}

@Composable
private fun EditionForm(
    fileId: Long,
    current: String,
    viewModel: TitleViewModel,
) {
    var edition by rememberSaveable { mutableStateOf(current) }
    val problem = TitleLogic.editionProblem(edition)
    SheetTitle(stringResource(R.string.title_set_edition))
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = edition,
            onValueChange = { edition = it.take(40) },
            label = { Text(stringResource(R.string.title_edition)) },
            isError = problem != null,
            supportingText = { problem?.let { Text(labelProblemText(it)) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            TextButton(onClick = viewModel::close) { Text(stringResource(R.string.common_cancel)) }
            androidx.compose.material3.Button(
                onClick = { viewModel.launch { if (viewModel.presenter.setEdition(fileId, edition)) viewModel.close() } },
                enabled = problem == null,
            ) { Text(stringResource(R.string.title_save)) }
        }
    }
}

@Composable
private fun AdoptForm(
    versionId: Long,
    viewModel: TitleViewModel,
) {
    var path by rememberSaveable { mutableStateOf("") }
    SheetTitle(stringResource(R.string.title_adopt), stringResource(R.string.title_adopt_hint))
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            label = { Text(stringResource(R.string.title_adopt_path)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            TextButton(onClick = viewModel::close) { Text(stringResource(R.string.common_cancel)) }
            androidx.compose.material3.Button(
                onClick = { viewModel.launch { if (viewModel.presenter.adopt(versionId, path)) viewModel.close() } },
                enabled = path.isNotBlank(),
            ) { Text(stringResource(R.string.title_adopt_button)) }
        }
    }
}

@Composable
fun <T> SegmentRow(
    options: List<Pair<T, String>>,
    selected: T,
    onPick: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onPick(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(label, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipChoices(
    options: List<Pair<T, String>>,
    selected: T,
    onPick: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        options.forEach { (value, label) -> FilterChip(selected = value == selected, onClick = { onPick(value) }, label = { Text(label) }) }
    }
}
