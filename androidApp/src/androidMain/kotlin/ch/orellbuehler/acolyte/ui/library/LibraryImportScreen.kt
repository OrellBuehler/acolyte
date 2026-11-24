package ch.orellbuehler.acolyte.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.library.ImportFolderOption
import ch.orellbuehler.acolyte.feature.library.ImportRow
import ch.orellbuehler.acolyte.feature.library.LibraryImportPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryImportState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.LibraryImportFilesRoute
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlin.math.roundToInt

private typealias ImportModel = PresenterViewModel<LibraryImportPresenter>

@Composable
fun LibraryImportScreen(
    onBack: () -> Unit,
    open: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val model: ImportModel = viewModel(key = "library-import") { PresenterViewModel(sdk.libraryImport()) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    var confirmRest by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.library_import_title), onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                StatusContent(
                    status = state.status,
                    isEmpty = state.isEmpty,
                    onRefresh = { model.run { refresh() } },
                    modifier = Modifier.weight(1f),
                    empty = { NoFolders(state, open) },
                ) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                        item { PickFolder(state, model, open) }
                        plan(state, model, onScanRest = { if (state.tickedCount > 0) confirmRest = true else model.run { scanRest() } })
                    }
                }
                if (state.plan != null && state.outcome == null) AdoptBar(state, model)
            }
            NoticeHost(state.notice) { model.presenter.dismissNotice(it) }
        }
    }
    if (confirmRest) {
        ConfirmDialog(
            title = stringResource(R.string.library_import_scan_rest),
            text = pluralStringResource(R.plurals.library_import_scan_rest_ask_folders, state.tickedCount, state.tickedCount),
            confirm = stringResource(R.string.library_import_scan_rest),
            onConfirm = { model.run { scanRest() } },
            onDismiss = { confirmRest = false },
        )
    }
    state.lookup?.let { LookupDialog(state, model) }
    state.tidy?.let { TidyDialog(state, model) }
}

@Composable
private fun NoFolders(
    state: LibraryImportState,
    open: (Any) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyView(
            title = stringResource(R.string.library_import_empty_title),
            message = stringResource(R.string.library_import_empty_message),
            icon = Icons.Rounded.Folder,
            modifier = Modifier.height(240.dp),
        )
        if (state.canOpenFolders) {
            Button(onClick = { open(Routes.settingsLibraryFolders) }) { Text(stringResource(R.string.library_empty_folders)) }
        }
    }
}

@Composable
private fun folderLabel(folder: ImportFolderOption): String {
    val base =
        if (folder.label.isNotEmpty() && folder.label != folder.path) {
            stringResource(R.string.library_import_folder_label, folder.label, folder.path)
        } else {
            folder.path
        }
    return when (folder.kind) {
        "show" -> stringResource(R.string.library_import_folder_show, base)
        "music" -> stringResource(R.string.library_import_folder_music, base)
        else -> base
    }
}

@Composable
private fun PickFolder(
    state: LibraryImportState,
    model: ImportModel,
    open: (Any) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.library_import_stay), color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.library_import_step_pick), style = MaterialTheme.typography.titleMedium)
        val current = state.folders.firstOrNull { it.id == state.folderId }
        Box {
            OutlinedButton(onClick = { menu = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(
                    current?.let { folderLabel(it) } ?: stringResource(R.string.library_import_folder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Rounded.ExpandMore, contentDescription = stringResource(R.string.library_import_folder))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                state.folders.forEach { folder ->
                    DropdownMenuItem(text = { Text(folderLabel(folder)) }, onClick = {
                        menu = false
                        model.presenter.selectFolder(folder.id)
                    })
                }
            }
        }
        Button(onClick = {
            model.run { scan() }
        }, enabled = !state.busy && state.folderId != 0L) { Text(stringResource(R.string.library_import_scan)) }
        TextButton(onClick = { open(LibraryImportFilesRoute()) }) { Text(stringResource(R.string.library_import_by_hand)) }
        if (state.busy) Progress(state)
        state.error?.let { ErrorBanner(it) }
    }
}

@Composable
private fun Progress(state: LibraryImportState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        LinearProgressIndicator(
            progress = {
                if (state.adopting &&
                    state.adoptTotal > 0
                ) {
                    state.adoptDone.toFloat() / state.adoptTotal
                } else {
                    state.progress.progress.toFloat()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        MutedText(
            when {
                state.adopting -> stringResource(R.string.library_import_adopting, state.adoptDone, state.adoptTotal)
                state.progressDone >= 0 && state.progressTotal > 0 ->
                    stringResource(R.string.library_import_progress_folders, state.progressDone, state.progressTotal)
                state.progress.note.isNotEmpty() -> state.progress.note
                else -> stringResource(R.string.library_import_progress_reading)
            },
        )
    }
}

private fun LazyListScope.plan(
    state: LibraryImportState,
    model: ImportModel,
    onScanRest: () -> Unit,
) {
    val plan = state.plan ?: return
    item {
        Column(Modifier.padding(Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            HorizontalDivider()
            Text(stringResource(R.string.library_import_step_check), style = MaterialTheme.typography.titleMedium)
            MutedText(
                if (plan.more || plan.offset > 0) {
                    stringResource(R.string.library_import_plan_part, plan.total, plan.path, plan.offset + 1, plan.offset + plan.shown)
                } else {
                    stringResource(R.string.library_import_plan, plan.shown, plan.path)
                },
            )
            if (plan.warning.isNotEmpty()) NoticeBanner(plan.warning)
            if (plan.kind == "movie") TidyNotice(state, model)
            if (plan.more && plan.next.isNotEmpty()) {
                MutedText(stringResource(R.string.library_import_more, plan.next))
                OutlinedButton(onClick = onScanRest, enabled = !state.busy) { Text(stringResource(R.string.library_import_scan_rest)) }
            }
            state.outcome?.let { Outcome(state) }
        }
    }
    item {
        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.Screen), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            items(state.statusOptions, key = { it.id }) { option ->
                FilterChip(
                    selected = option.selected,
                    onClick = { model.presenter.setFilter(option.id) },
                    label = { Text("${importStatusLabel(option.id, plan.kind)}  ${option.count}", maxLines = 1) },
                )
            }
        }
    }
    item {
        var query by rememberSaveable { mutableStateOf(state.query) }
        Row(Modifier.padding(horizontal = Spacing.Screen), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    model.presenter.setQuery(it)
                },
                placeholder = { Text(stringResource(R.string.library_import_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
    item {
        Row(Modifier.padding(horizontal = Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                model.presenter.selectAll()
            }, enabled = state.shownTickable > 0) { Text(stringResource(R.string.library_select_all)) }
            TextButton(onClick = {
                model.presenter.selectNone()
            }, enabled = state.tickedCount > 0) { Text(stringResource(R.string.library_select_none)) }
        }
    }
    items(state.rows, key = { it.folder }) { row -> ImportRowItem(row, plan.kind, state.busy, model) }
}

@Composable
private fun TidyNotice(
    state: LibraryImportState,
    model: ImportModel,
) {
    val plan = state.plan ?: return
    if (plan.looseMovies == 0 && plan.looseTop == 0 && !plan.suggestFlat && state.undoFiles == 0) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            if (plan.looseMovies > 0) Text(stringResource(R.string.library_import_tidy_loose, plan.looseMovies))
            if (plan.looseTop > 0) Text(stringResource(R.string.library_import_tidy_loose_top, plan.looseTop))
            if (plan.suggestFlat) Text(stringResource(R.string.library_import_tidy_suggest_flat))
            if (plan.looseTop > 0 || plan.suggestFlat) MutedText(stringResource(R.string.library_import_tidy_optional))
            if (state.undoFiles > 0) MutedText(stringResource(R.string.library_import_tidy_undo_note, state.undoFiles))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (!plan.flat && (plan.suggestFlat || plan.looseTop > 0)) {
                    TextButton(
                        onClick = { model.run { keepAsFiles() } },
                        enabled = !state.busy,
                    ) { Text(stringResource(R.string.library_import_tidy_keep_flat)) }
                }
                if (plan.looseMovies > 0 || plan.looseTop > 0 || plan.suggestFlat) {
                    TextButton(
                        onClick = { model.run { openTidy() } },
                        enabled = !state.busy,
                    ) { Text(stringResource(R.string.library_import_tidy_open)) }
                }
                if (state.undoFiles > 0) {
                    TextButton(
                        onClick = { model.run { undoTidy() } },
                        enabled = !state.busy,
                    ) { Text(stringResource(R.string.library_import_tidy_undo)) }
                }
            }
        }
    }
}

@Composable
private fun Outcome(state: LibraryImportState) {
    val outcome = state.outcome ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(stringResource(R.string.library_import_done), style = MaterialTheme.typography.titleSmall)
            if (outcome.adopted > 0) Text(pluralStringResource(R.plurals.library_import_done_adopted, outcome.adopted, outcome.adopted))
            if (outcome.failed > 0) Text(pluralStringResource(R.plurals.library_import_done_failed, outcome.failed, outcome.failed))
            if (outcome.unknown > 0) Text(pluralStringResource(R.plurals.library_import_done_unknown, outcome.unknown, outcome.unknown))
            outcome.folders.filter { it.error.isNotEmpty() }.forEach { failed ->
                MutedText(failed.folder + " · " + stringResource(R.string.library_import_not_adopted_why, failed.error))
            }
        }
    }
}

@Composable
fun importStatusLabel(
    status: String,
    kind: String,
): String =
    stringResource(
        when (status) {
            "matched" -> R.string.library_import_status_matched
            "unsure" -> R.string.library_import_status_unsure
            "unmatched" -> R.string.library_import_status_unmatched
            "not_looked_up" -> R.string.library_import_status_not_looked_up
            "in_library" -> R.string.library_import_status_in_library
            "never_add" -> R.string.library_import_status_never_add
            "no_video" -> if (kind == "music") R.string.library_import_status_no_audio else R.string.library_import_status_no_video
            else -> R.string.library_import_status_all
        },
    )

private fun importTone(status: String): Tone =
    when (status) {
        "matched", "in_library" -> Tone.OK
        "unsure" -> Tone.WAIT
        "unmatched" -> Tone.FAIL
        else -> Tone.IDLE
    }

@Composable
private fun ImportRowItem(
    row: ImportRow,
    kind: String,
    busy: Boolean,
    model: ImportModel,
) {
    var menu by remember { mutableStateOf(false) }
    val pick = row.pick
    val label = importStatusLabel(row.status, kind)
    val score = pick?.score ?: -1.0
    val pill =
        if ((row.status == "matched" || row.status == "unsure") && score in 0.0..0.899) {
            stringResource(R.string.library_import_status_score, label, (score * 100).roundToInt())
        } else {
            label
        }
    ListItem(
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = row.ticked,
                    onCheckedChange = { model.presenter.toggle(row.folder) },
                    enabled = row.canTick && pick != null && !busy,
                )
                PosterThumb(pick?.poster?.ifEmpty { null }, row.shownName)
            }
        },
        headlineContent = {
            Text(
                if (row.shownYear > 0) stringResource(R.string.library_stats_name_year, row.shownName, row.shownYear) else row.shownName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                MutedText(row.folder)
                StatusChip(pill, importTone(row.status))
                if (row.why.isNotEmpty()) MutedText(row.why)
                if (row.files > 0) {
                    MutedText(stringResource(R.string.library_import_episodes, row.episodes, row.files - row.unknown))
                    if (row.unknown > 0) MutedText(stringResource(R.string.library_import_left_alone, row.unknown))
                }
                if (row.albums > 0) MutedText(stringResource(R.string.library_import_albums, row.albums, row.tracks))
                if (row.canTick) {
                    Box {
                        TextButton(onClick = { menu = true }, enabled = !busy) { Text(stringResource(R.string.library_import_change)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            row.choices.forEach { choice ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (choice.year >
                                                0
                                            ) {
                                                stringResource(R.string.library_stats_name_year, choice.name, choice.year)
                                            } else {
                                                choice.name
                                            },
                                        )
                                    },
                                    onClick = {
                                        menu = false
                                        model.presenter.pick(row.folder, choice.key)
                                    },
                                )
                            }
                            DropdownMenuItem(text = { Text(stringResource(R.string.library_import_look_up)) }, onClick = {
                                menu = false
                                model.presenter.openLookup(row.folder)
                            })
                        }
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(enabled = row.canTick && pick != null && !busy) { model.presenter.toggle(row.folder) },
    )
}

@Composable
private fun AdoptBar(
    state: LibraryImportState,
    model: ImportModel,
) {
    val kind = state.plan?.kind ?: "movie"
    val count = state.tickedCount
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.library_import_follow),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = state.follow, onCheckedChange = { model.presenter.setFollow(it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pluralStringResource(R.plurals.library_import_ticked, count, count), modifier = Modifier.weight(1f))
                Button(onClick = { model.run { adopt() } }, enabled = count > 0 && !state.busy) {
                    Text(
                        when (kind) {
                            "show" -> pluralStringResource(R.plurals.library_import_adopt_show, count, count)
                            "music" -> pluralStringResource(R.plurals.library_import_adopt_artist, count, count)
                            else -> pluralStringResource(R.plurals.library_import_adopt_movie, count, count)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LookupDialog(
    state: LibraryImportState,
    model: ImportModel,
) {
    val lookup = state.lookup ?: return
    val kind = state.plan?.kind ?: "movie"
    var term by rememberSaveable(lookup.folder) { mutableStateOf(lookup.term) }
    AlertDialog(
        onDismissRequest = { model.presenter.closeLookup() },
        title = { Text(stringResource(R.string.library_import_lookup_title, lookup.folder)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it.take(200) },
                    placeholder = {
                        Text(
                            stringResource(
                                when (kind) {
                                    "show" -> R.string.library_import_lookup_show
                                    "music" -> R.string.library_import_lookup_music
                                    else -> R.string.library_import_lookup_movie
                                },
                            ),
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { model.run { lookup(term) } }, enabled = term.isNotBlank() && !lookup.busy) {
                    Text(stringResource(R.string.library_import_lookup_button))
                }
                if (lookup.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                lookup.error?.let { MutedText(libraryErrorText(it)) }
                if (lookup.nothing) {
                    val id =
                        when (kind) {
                            "show" -> "tvdb:<id>"
                            "music" -> "mbid:<id>"
                            else -> "tmdb:<id>"
                        }
                    MutedText(stringResource(R.string.library_import_lookup_nothing, id))
                }
                LazyColumn(Modifier.height(260.dp)) {
                    items(lookup.results, key = { it.key }) { result ->
                        ListItem(
                            leadingContent = { PosterThumb(result.poster.ifEmpty { null }, result.name) },
                            headlineContent = {
                                Text(
                                    if (result.year >
                                        0
                                    ) {
                                        stringResource(R.string.library_stats_name_year, result.name, result.year)
                                    } else {
                                        result.name
                                    },
                                )
                            },
                            supportingContent = {
                                if (result.inLibrary) {
                                    MutedText(
                                        stringResource(R.string.library_import_status_in_library),
                                    )
                                }
                            },
                            trailingContent = {
                                TextButton(onClick = { model.presenter.useLookup(result) }, enabled = !result.inLibrary) {
                                    Text(stringResource(R.string.library_import_lookup_use))
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { model.presenter.closeLookup() }) { Text(stringResource(R.string.library_close)) } },
    )
}

@Composable
private fun TidyDialog(
    state: LibraryImportState,
    model: ImportModel,
) {
    val tidy = state.tidy ?: return
    AlertDialog(
        onDismissRequest = { model.run { closeTidy() } },
        title = { Text(stringResource(R.string.library_import_tidy_open)) },
        text = {
            LazyColumn(Modifier.height(380.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (tidy.changed) item { NoticeBanner(stringResource(R.string.library_import_tidy_changed)) }
                val result = tidy.result
                if (result != null) {
                    item { Text(stringResource(R.string.library_import_tidy_moved, result.moved)) }
                    if (result.failed.isNotEmpty()) {
                        item { Text(stringResource(R.string.library_import_tidy_failed, result.failed.size)) }
                        items(result.failed) { MutedText(it.from + " · " + it.why) }
                    }
                } else {
                    item { MutedText(stringResource(R.string.library_import_tidy_intro)) }
                    if (tidy.moves.isEmpty() && tidy.blocked.isEmpty()) item { Text(stringResource(R.string.library_import_tidy_nothing)) }
                    if (tidy.moves.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.library_import_tidy_ready, tidy.moves.size),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                    items(tidy.moves) { move ->
                        Column {
                            Text(
                                stringResource(R.string.library_import_arrow, move.from, move.to),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (move.files.isNotEmpty()) {
                                MutedText(
                                    stringResource(
                                        R.string.library_import_tidy_also,
                                        move.files.joinToString(", ") { LibraryFormat.fileNameOf(it.from) },
                                    ),
                                )
                            }
                        }
                    }
                    if (tidy.blocked.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.library_import_tidy_blocked, tidy.blocked.size),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                    items(tidy.blocked) { move ->
                        Column {
                            Text(move.from, style = MaterialTheme.typography.bodySmall)
                            MutedText(
                                when (move.why) {
                                    "exists" -> stringResource(R.string.library_import_tidy_why_exists, move.path)
                                    "two" -> stringResource(R.string.library_import_tidy_why_two, move.path)
                                    "has_movie" -> stringResource(R.string.library_import_tidy_why_has_movie, move.path)
                                    else -> move.problem
                                },
                            )
                        }
                    }
                    if (tidy.unmatched > 0) item { MutedText(stringResource(R.string.library_import_tidy_unmatched, tidy.unmatched)) }
                }
            }
        },
        confirmButton = {
            if (tidy.result == null && tidy.moves.isNotEmpty()) {
                TextButton(
                    onClick = { model.run { tidy() } },
                    enabled = !tidy.busy,
                ) { Text(stringResource(R.string.library_import_tidy_move)) }
            }
        },
        dismissButton = {
            TextButton(onClick = { model.run { closeTidy() } }) {
                Text(stringResource(if (tidy.result != null) R.string.library_close else R.string.library_cancel))
            }
        },
    )
}
