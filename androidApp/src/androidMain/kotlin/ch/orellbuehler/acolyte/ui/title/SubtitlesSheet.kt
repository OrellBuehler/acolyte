package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.SubtitleFileView
import ch.orellbuehler.acolyte.api.model.SubtitleRowView
import ch.orellbuehler.acolyte.api.model.SubtitleWant
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.SubtitlesPresenter
import ch.orellbuehler.acolyte.feature.title.SubtitlesState
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

private sealed interface SubForm {
    data class Sync(
        val row: SubtitleRowView,
    ) : SubForm

    data class TurnDown(
        val row: SubtitleRowView,
    ) : SubForm

    data class Translate(
        val file: SubtitleFileView,
        val want: SubtitleRowView,
    ) : SubForm
}

@Composable
fun SubtitlesContent(
    presenter: SubtitlesPresenter,
    viewModel: TitleViewModel,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) {
        if (state.status.phase != Phase.READY) presenter.load()
        presenter.loadExtras()
    }
    LaunchedEffect(presenter) { presenter.watch() }
    FeedbackEffect(state.feedback)
    var form by remember { mutableStateOf<SubForm?>(null) }
    SheetTitle(stringResource(R.string.title_subtitles))
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        state.status.error?.let { ErrorBanner(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            OutlinedButton(onClick = { viewModel.launch { presenter.search() } }, enabled = state.busy != "search") {
                Text(stringResource(R.string.title_subtitles_search))
            }
            if (state.canCleanAll) {
                OutlinedButton(onClick = { viewModel.launch { presenter.cleanAll() } }, enabled = state.busy != "clean") {
                    Text(stringResource(R.string.title_subtitles_clean_all))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.title_subtitles_languages, wishLine(state.wish, state), fromText(state)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (!state.editing) TextButton(onClick = presenter::startEdit) { Text(stringResource(R.string.title_change)) }
        }
        if (state.editing) WishEditor(state, presenter, viewModel)
        if (state.status.phase == Phase.LOADING) CircularProgressIndicator()
        if (state.listMode) {
            Text(
                if (state.missingCount > 0) {
                    pluralStringResource(R.plurals.title_subtitles_missing_count, state.missingCount, state.missingCount)
                } else {
                    stringResource(R.string.title_subtitles_none_missing)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (state.files.isEmpty() && state.status.phase == Phase.READY) {
            Text(stringResource(R.string.title_subtitles_no_file), style = MaterialTheme.typography.bodyMedium)
        } else if (!state.listMode) {
            state.files.forEach { file -> FileSubtitles(file, state, presenter, viewModel) { form = it } }
        }
        state.choices?.let { choices -> ChoiceList(choices, state, presenter, viewModel) }
    }
    when (val f = form) {
        is SubForm.Sync ->
            SyncDialog(f.row, onDismiss = {
                form = null
            }) { offset, from, to -> viewModel.launch { presenter.sync(f.row.id, offset, from, to) } }
        is SubForm.TurnDown ->
            ReasonDialog(
                onDismiss = { form = null },
            ) { reason -> viewModel.launch { presenter.turnDown(f.row.id, reason) } }
        is SubForm.Translate ->
            TranslateDialog(f.file, f.want, state, onDismiss = {
                form = null
            }) { from -> viewModel.launch { presenter.translate(f.file.fileId, f.want, from) } }
        null -> Unit
    }
}

@Composable
private fun wishLine(
    wants: List<SubtitleWant>,
    state: SubtitlesState,
): String =
    if (wants.isEmpty()) {
        stringResource(R.string.title_subtitles_none)
    } else {
        wants.map { wantLabel(it, state) }.joinToString(" · ")
    }

@Composable
private fun wantLabel(
    want: SubtitleWant,
    state: SubtitlesState,
): String {
    val parts = mutableListOf(state.languageName(want.language).ifBlank { languageName(want.language, locale()) })
    if (want.hearingImpaired == "hi") parts += stringResource(R.string.title_subtitles_hi_only)
    if (want.hearingImpaired == "either") parts += stringResource(R.string.title_subtitles_either)
    if (want.forced) parts += stringResource(R.string.title_subtitles_forced_only)
    return parts.joinToString(", ")
}

@Composable
private fun fromText(state: SubtitlesState): String =
    stringResource(
        when (state.wishFrom) {
            "episode" -> R.string.title_subtitles_from_episode
            "version" -> R.string.title_subtitles_from_version
            "title" -> if (state.movie) R.string.title_subtitles_from_movie else R.string.title_subtitles_from_show
            else -> R.string.title_subtitles_from_default
        },
    )

@Composable
private fun WishEditor(
    state: SubtitlesState,
    presenter: SubtitlesPresenter,
    viewModel: TitleViewModel,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.title_subtitles_which), style = MaterialTheme.typography.titleSmall)
            SegmentRow(
                listOf(
                    false to
                        stringResource(
                            if (state.itemId >
                                0
                            ) {
                                R.string.title_subtitles_same_as_show
                            } else {
                                R.string.title_subtitles_default_languages
                            },
                        ),
                    true to stringResource(R.string.title_subtitles_own),
                ),
                state.editOwn,
            ) { presenter.setEditOwn(it) }
            Text(
                stringResource(if (state.editOwn) R.string.title_subtitles_own_hint else R.string.title_subtitles_default_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.editOwn) {
                state.editWants.forEachIndexed { index, want ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            ChoiceRow(
                                stringResource(R.string.title_language),
                                wantLabel(want, state),
                                state.languages.mapIndexed { i, l -> i.toLong() to l.name },
                                state.languages.indexOfFirst { it.code == want.language }.toLong(),
                            ) { i -> presenter.updateWant(index, want.copy(language = state.languages[i.toInt()].code)) }
                            ChoiceRow(
                                stringResource(R.string.title_subtitles_hearing),
                                hiText(want.hearingImpaired),
                                listOf(0L to hiText("normal"), 1L to hiText("hi"), 2L to hiText("either")),
                                listOf("normal", "hi", "either").indexOf(want.hearingImpaired).toLong(),
                            ) { i -> presenter.updateWant(index, want.copy(hearingImpaired = listOf("normal", "hi", "either")[i.toInt()])) }
                            if (state.translator) {
                                ChoiceRow(
                                    stringResource(R.string.title_subtitles_translate_from),
                                    want.translateFrom.ifBlank { stringResource(R.string.title_subtitles_no_translation) },
                                    listOf(-1L to stringResource(R.string.title_subtitles_no_translation)) +
                                        state.languages.mapIndexed { i, l -> i.toLong() to l.name },
                                    state.languages.indexOfFirst { it.code == want.translateFrom }.toLong(),
                                ) { i ->
                                    presenter.updateWant(
                                        index,
                                        want.copy(
                                            translateFrom =
                                                if (i <
                                                    0
                                                ) {
                                                    ""
                                                } else {
                                                    state.languages[i.toInt()].code
                                                },
                                        ),
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = want.forced, onCheckedChange = { presenter.updateWant(index, want.copy(forced = it)) })
                                Text(stringResource(R.string.title_subtitles_forced))
                            }
                        }
                        IconButton(onClick = { presenter.removeWant(index) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.title_remove))
                        }
                    }
                    HorizontalDivider()
                }
                if (state.editWants.size <
                    10
                ) {
                    TextButton(onClick = presenter::addWant) { Text(stringResource(R.string.title_subtitles_add_language)) }
                }
            }
            state.editProblem?.let {
                Text(
                    wishProblemText(it),
                    color = MaterialTheme.acolyte.failText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                TextButton(onClick = presenter::cancelEdit) { Text(stringResource(R.string.common_cancel)) }
                Button(
                    onClick = { viewModel.launch { presenter.saveEdit() } },
                    enabled = state.editProblem == null && state.busy != "wish",
                ) {
                    Text(stringResource(R.string.title_subtitles_save))
                }
            }
        }
    }
}

@Composable
private fun hiText(value: String): String =
    stringResource(
        when (value) {
            "hi" -> R.string.title_subtitles_hi
            "either" -> R.string.title_subtitles_hi_either
            else -> R.string.title_subtitles_hi_normal
        },
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileSubtitles(
    file: SubtitleFileView,
    state: SubtitlesState,
    presenter: SubtitlesPresenter,
    viewModel: TitleViewModel,
    openForm: (SubForm) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            val prefix =
                if (state.data.files
                        .map { it.versionId }
                        .distinct()
                        .size >
                    1
                ) {
                    file.versionLabel.ifBlank { stringResource(R.string.title_main_copy) } + " · "
                } else {
                    ""
                }
            Text(prefix + file.fileName, style = MaterialTheme.typography.titleSmall)
            val present = state.present(file)
            if (present.isEmpty()) Text(stringResource(R.string.title_subtitles_none_yet), style = MaterialTheme.typography.bodySmall)
            present.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall),
                        verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
                    ) {
                        Text(
                            state.languageName(row.language).ifBlank { languageName(row.language, locale()) },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        StatusChip(kindText(row), Tone.IDLE)
                        madeText(row.made)?.let { StatusChip(it, Tone.IDLE) }
                        if (row.picture) StatusChip(stringResource(R.string.title_subtitles_picture), Tone.IDLE)
                        row.score?.let { StatusChip("$it/100", Tone.IDLE) }
                    }
                    Text(
                        sourceText(row) +
                            row.savedAt
                                .takeIf { it.isNotBlank() }
                                ?.let { " · " + formatDate(it, locale()) }
                                .orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (TitleLogic.ourSubtitle(row)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                            TextButton(onClick = { openForm(SubForm.Sync(row)) }) { Text(stringResource(R.string.title_subtitles_sync)) }
                            if (row.relativePath.endsWith(".srt", true)) {
                                TextButton(
                                    onClick = { viewModel.launch { presenter.clean(row.id) } },
                                ) { Text(stringResource(R.string.title_subtitles_clean)) }
                            }
                            TextButton(onClick = { openForm(SubForm.TurnDown(row)) }) {
                                Text(stringResource(R.string.title_subtitles_turn_down), color = MaterialTheme.acolyte.failText)
                            }
                        }
                    }
                }
            }
            state.missing(file).forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.languageName(row.language).ifBlank { row.language } + ": " + missingText(row),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.translator && state.translatable(file, row.language).isNotEmpty()) {
                        TextButton(
                            onClick = { openForm(SubForm.Translate(file, row)) },
                        ) { Text(stringResource(R.string.title_subtitles_translate)) }
                    }
                }
            }
            OutlinedButton(onClick = { viewModel.launch { presenter.openChoices(file.fileId) } }) {
                Text(stringResource(R.string.title_subtitles_choose_one))
            }
        }
    }
}

@Composable
private fun kindText(row: SubtitleRowView): String =
    stringResource(
        when {
            row.forced -> R.string.title_subtitles_kind_forced
            row.hearingImpaired -> R.string.title_subtitles_kind_hi
            else -> R.string.title_subtitles_kind_normal
        },
    )

@Composable
private fun madeText(made: String): String? =
    when (made) {
        "translated" -> stringResource(R.string.title_subtitles_made_translated)
        "translated_partial" -> stringResource(R.string.title_subtitles_made_partial)
        "ai_translated" -> stringResource(R.string.title_subtitles_made_ai)
        "machine_translated" -> stringResource(R.string.title_subtitles_made_machine)
        else -> null
    }

@Composable
private fun sourceText(row: SubtitleRowView): String =
    when {
        row.place == "built_in" ->
            if (row.codec.isNotBlank()) {
                stringResource(
                    R.string.title_subtitles_inside_codec,
                    row.codec,
                )
            } else {
                stringResource(R.string.title_subtitles_inside)
            }
        row.origin == "found" -> stringResource(R.string.title_subtitles_found)
        row.origin == "by_hand" ->
            if (row.site.isNotBlank()) {
                stringResource(
                    R.string.title_subtitles_hand_from,
                    row.site,
                )
            } else {
                stringResource(R.string.title_subtitles_hand)
            }
        row.site.isNotBlank() -> stringResource(R.string.title_subtitles_from_site, row.site)
        else -> stringResource(R.string.title_subtitles_saved)
    }

@Composable
private fun missingText(row: SubtitleRowView): String =
    when {
        row.state == "none_good_enough" ->
            pluralStringResource(R.plurals.title_subtitles_none_good, row.tries, row.tries) +
                row.nextTryAt
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        " · " +
                            stringResource(R.string.title_subtitles_next_try, formatDate(it, locale()))
                    }.orEmpty()
        row.tries > 0 -> pluralStringResource(R.plurals.title_subtitles_not_found, row.tries, row.tries)
        else -> stringResource(R.string.title_subtitles_not_looked)
    }

@Composable
private fun ChoiceList(
    choices: ch.orellbuehler.acolyte.feature.title.SubtitleChoiceState,
    state: SubtitlesState,
    presenter: SubtitlesPresenter,
    viewModel: TitleViewModel,
) {
    HorizontalDivider()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.title_subtitles_choose_one),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = presenter::closeChoices,
        ) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.common_cancel)) }
    }
    val data = choices.choices
    if (choices.loading || data == null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            CircularProgressIndicator()
            Text(stringResource(R.string.title_subtitles_asking))
        }
        return
    }
    if (data.sitesAsked.isNotEmpty()) {
        Text(
            stringResource(R.string.title_subtitles_asked, data.sitesAsked.joinToString(", ")),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    data.outcomes.forEach { outcome ->
        Text(wantLabel(outcome.want, state), style = MaterialTheme.typography.titleSmall)
        when {
            outcome.covered.isNotEmpty() ->
                Text(
                    stringResource(
                        when {
                            outcome.covered.any { it.place == "beside" } -> R.string.title_subtitles_covered_beside
                            outcome.covered.any { it.picture } -> R.string.title_subtitles_covered_picture
                            else -> R.string.title_subtitles_covered_inside
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            outcome.result == "translating" ->
                Text(
                    stringResource(R.string.title_subtitles_translating_none),
                    style = MaterialTheme.typography.bodySmall,
                )
            outcome.choices.isEmpty() ->
                Text(
                    stringResource(R.string.title_subtitles_nothing_found),
                    style = MaterialTheme.typography.bodySmall,
                )
        }
        outcome.choices.forEach { candidate ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                    Text(candidate.shownName, style = MaterialTheme.typography.bodyMedium)
                    val facts = mutableListOf(candidate.site)
                    if (candidate.machineMade.isNotBlank()) facts += stringResource(R.string.title_subtitles_machine_made)
                    if (candidate.hearingImpaired) facts += stringResource(R.string.title_subtitles_kind_hi)
                    if (candidate.forced) facts += stringResource(R.string.title_subtitles_kind_forced)
                    if (candidate.trusted) facts += stringResource(R.string.title_subtitles_trusted)
                    if (candidate.downloads >
                        0
                    ) {
                        facts += pluralStringResource(R.plurals.title_subtitles_downloads, candidate.downloads, candidate.downloads)
                    }
                    Text(
                        facts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip("${candidate.score.total}/100", if (candidate.score.refused.isNotBlank()) Tone.FAIL else Tone.IDLE)
                        androidx.compose.foundation.layout
                            .Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { viewModel.launch { presenter.choose(choices.fileId, candidate.site, candidate.key) } },
                            enabled = state.busy != "choose-${candidate.key}",
                        ) { Text(stringResource(R.string.title_subtitles_choose)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncDialog(
    row: SubtitleRowView,
    onDismiss: () -> Unit,
    onSave: (Int, Double?, Double?) -> Unit,
) {
    var offset by remember { mutableStateOf("0") }
    var fps by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf("25") }
    var to by remember { mutableStateOf("23.976") }
    val shift = offset.toIntOrNull()?.coerceIn(-600000, 600000) ?: 0
    val valid = shift != 0 || fps
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_subtitles_sync)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(stringResource(R.string.title_subtitles_sync_intro), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                    OutlinedButton(
                        onClick = { offset = (shift - 500).toString() },
                    ) { Text(stringResource(R.string.title_subtitles_earlier, 500)) }
                    OutlinedButton(
                        onClick = { offset = (shift + 500).toString() },
                    ) { Text(stringResource(R.string.title_subtitles_later, 500)) }
                }
                OutlinedTextField(
                    value = offset,
                    onValueChange = { offset = it.filter { c -> c.isDigit() || c == '-' }.take(7) },
                    label = { Text(stringResource(R.string.title_subtitles_shift)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = fps, onCheckedChange = { fps = it })
                    Text(stringResource(R.string.title_subtitles_change_fps))
                }
                if (fps) {
                    OutlinedTextField(value = from, onValueChange = {
                        from = it
                    }, label = { Text(stringResource(R.string.title_subtitles_from_fps)) }, singleLine = true)
                    OutlinedTextField(value = to, onValueChange = {
                        to = it
                    }, label = { Text(stringResource(R.string.title_subtitles_to_fps)) }, singleLine = true)
                }
                if (!valid) {
                    Text(
                        stringResource(R.string.title_subtitles_sync_needed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.acolyte.failText,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onSave(
                        shift,
                        if (fps) from.replace(',', '.').toDoubleOrNull() else null,
                        if (fps) to.replace(',', '.').toDoubleOrNull() else null,
                    )
                },
                enabled = valid,
            ) { Text(stringResource(R.string.title_subtitles_fix)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun ReasonDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_subtitles_turn_down)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(stringResource(R.string.title_subtitles_turn_down_intro), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(200) },
                    label = { Text(stringResource(R.string.title_why_optional)) },
                    placeholder = { Text(stringResource(R.string.title_subtitles_why_placeholder)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onSave(reason)
            }) { Text(stringResource(R.string.title_subtitles_turn_it_down), color = MaterialTheme.acolyte.failText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun TranslateDialog(
    file: SubtitleFileView,
    want: SubtitleRowView,
    state: SubtitlesState,
    onDismiss: () -> Unit,
    onSave: (SubtitleRowView) -> Unit,
) {
    val sources = state.translatable(file, want.language)
    var picked by remember { mutableStateOf(sources.firstOrNull()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_subtitles_translate)) },
        text = {
            ChoiceRow(
                stringResource(R.string.title_subtitles_translate_from),
                picked?.let { "${state.languageName(it.language)} · ${sourceText(it)}" }.orEmpty(),
                sources.mapIndexed { i, s -> i.toLong() to "${state.languageName(s.language)} · ${sourceText(s)}" },
                sources.indexOf(picked).toLong(),
            ) { picked = sources[it.toInt()] }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                picked?.let(onSave)
            }, enabled = picked != null) { Text(stringResource(R.string.title_subtitles_translate)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
