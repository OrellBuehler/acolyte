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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import ch.orellbuehler.acolyte.feature.library.ByHandBlocker
import ch.orellbuehler.acolyte.feature.library.ByHandConfidence
import ch.orellbuehler.acolyte.feature.library.ByHandPick
import ch.orellbuehler.acolyte.feature.library.ByHandRow
import ch.orellbuehler.acolyte.feature.library.LibraryFilesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryFilesState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.theme.Spacing

private typealias FilesModel = PresenterViewModel<LibraryFilesPresenter>

@Composable
fun LibraryFilesScreen(
    path: String,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val model: FilesModel = viewModel(key = "library-files") { PresenterViewModel(sdk.libraryFiles(path)) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.library_files_title), onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (!state.allowed) {
                EmptyView(title = stringResource(R.string.library_files_owner_only), icon = Icons.Rounded.Lock)
            } else {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                        item { PathPicker(state, model) }
                        if (state.scanned) {
                            item {
                                Column(Modifier.padding(Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                                    Text(stringResource(R.string.library_files_check), style = MaterialTheme.typography.titleMedium)
                                    MutedText(
                                        pluralStringResource(
                                            R.plurals.library_files_found,
                                            state.rows.size,
                                            state.rows.size,
                                            state.scanPath,
                                        ),
                                    )
                                    if (state.more) MutedText(stringResource(R.string.library_files_more))
                                }
                            }
                            items(state.rows, key = { it.path }) { row -> FileRowItem(row, state.importing, model) }
                            item { ModePicker(state, model) }
                        } else {
                            item { MutedText(stringResource(R.string.library_files_nothing_moves), Modifier.padding(Spacing.Screen)) }
                        }
                    }
                    if (state.scanned && state.rows.isNotEmpty()) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                            Row(
                                Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    pluralStringResource(R.plurals.library_import_ticked, state.tickedCount, state.tickedCount),
                                    modifier = Modifier.weight(1f).padding(start = Spacing.Small),
                                )
                                Button(
                                    onClick = { if (state.replaceCount > 0) confirm = true else model.run { importFiles() } },
                                    enabled = state.tickedCount > 0 && !state.importing,
                                ) { Text(pluralStringResource(R.plurals.library_files_import, state.tickedCount, state.tickedCount)) }
                            }
                        }
                    }
                }
            }
            NoticeHost(state.notice) { model.presenter.dismissNotice(it) }
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.library_files_import_button),
            text = pluralStringResource(R.plurals.library_files_replace_confirm, state.replaceCount, state.replaceCount),
            confirm = stringResource(R.string.library_files_import_button),
            onConfirm = { model.run { importFiles() } },
            onDismiss = { confirm = false },
        )
    }
    state.browser?.let { BrowserDialog(state, model) }
    state.editor?.let { EditorDialog(state, model) }
}

@Composable
private fun PathPicker(
    state: LibraryFilesState,
    model: FilesModel,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.library_files_pick), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.path,
            onValueChange = { model.presenter.setPath(it) },
            label = { Text(stringResource(R.string.library_files_path)) },
            placeholder = { Text(stringResource(R.string.library_files_path_example)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        MutedText(stringResource(R.string.library_files_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (state.canBrowse) {
                OutlinedButton(
                    onClick = { model.run { openBrowser() } },
                    enabled = !state.scanning,
                ) { Text(stringResource(R.string.library_files_browse)) }
            }
            Button(onClick = { model.run { scan() } }, enabled = state.path.isNotBlank() && !state.scanning && !state.importing) {
                Text(stringResource(R.string.library_files_scan))
            }
        }
        if (state.scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { ErrorBanner(it) }
    }
}

@Composable
private fun pickLine(pick: ByHandPick?): String {
    pick ?: return stringResource(R.string.library_files_no_match)
    val name = if (pick.year > 0) stringResource(R.string.library_stats_name_year, pick.titleName, pick.year) else pick.titleName
    return when {
        pick.trackName.isNotEmpty() || pick.albumName.isNotEmpty() -> {
            val album =
                if (pick.albumYear >
                    0
                ) {
                    stringResource(R.string.library_stats_name_year, pick.albumName, pick.albumYear)
                } else {
                    pick.albumName
                }
            stringResource(R.string.library_files_track_pick, pick.titleName, album, pick.track, pick.trackName)
        }
        pick.titleKind == "show" && pick.season >= 0 && pick.episode >= 0 ->
            stringResource(R.string.library_files_episode_pick, name, pick.season, pick.episode)
        else -> name
    }
}

@Composable
private fun blockerText(blocker: ByHandBlocker): String? =
    when (blocker) {
        ByHandBlocker.NONE -> null
        ByHandBlocker.IN_LIBRARY -> stringResource(R.string.library_files_blocker_in_library)
        ByHandBlocker.NO_PICK -> stringResource(R.string.library_files_blocker_no_pick)
        ByHandBlocker.NO_EPISODE -> stringResource(R.string.library_files_blocker_no_episode)
        ByHandBlocker.HAS_FILE -> stringResource(R.string.library_files_blocker_has_file)
        ByHandBlocker.NO_TRACK -> stringResource(R.string.library_files_blocker_no_track)
    }

@Composable
private fun FileRowItem(
    row: ByHandRow,
    importing: Boolean,
    model: FilesModel,
) {
    ListItem(
        leadingContent = { Checkbox(checked = row.ticked, onCheckedChange = { model.presenter.toggle(row.path) }, enabled = row.canTick) },
        headlineContent = { Text(row.name, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                MutedText(listOf(LibraryFormat.bytes(row.size), row.quality).filter { it.isNotEmpty() }.joinToString(" · "))
                Text(pickLine(row.pick), style = MaterialTheme.typography.bodyMedium)
                when (row.confidence) {
                    ByHandConfidence.LIKELY -> StatusChip(stringResource(R.string.library_files_likely), Tone.OK)
                    ByHandConfidence.GUESS -> StatusChip(stringResource(R.string.library_files_guess), Tone.WAIT)
                    ByHandConfidence.NONE -> Unit
                }
                val note =
                    blockerText(row.blocker)?.takeIf { row.blocker != ByHandBlocker.HAS_FILE || !row.replace }
                        ?: row.note.ifEmpty { null }
                        ?: if (row.duplicate) stringResource(R.string.library_files_duplicate) else null
                note?.let { MutedText(it) }
                if (row.pick?.hasFile == true && row.result == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.library_files_replace),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Switch(checked = row.replace, onCheckedChange = { model.presenter.setReplace(row.path, it) }, enabled = !importing)
                    }
                }
                row.result?.let { result ->
                    Text(
                        when {
                            result.outcome == "placed" && result.placedBy == "move" -> stringResource(R.string.library_files_moved)
                            result.outcome == "placed" && result.placedBy == "copy" -> stringResource(R.string.library_files_copied)
                            result.outcome == "placed" && result.placedBy.isNotEmpty() -> stringResource(R.string.library_files_linked)
                            result.outcome == "placed" -> stringResource(R.string.library_files_recorded)
                            result.outcome == "refused" -> stringResource(R.string.library_files_not_imported, result.error)
                            else -> stringResource(R.string.library_files_failed, result.error)
                        },
                        color = if (result.outcome == "placed") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (row.canChange && row.result == null) {
                    TextButton(onClick = { model.presenter.openEditor(row.path) }, enabled = !importing) {
                        Text(stringResource(R.string.library_files_change))
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(enabled = row.canTick) { model.presenter.toggle(row.path) },
    )
}

@Composable
private fun ModePicker(
    state: LibraryFilesState,
    model: FilesModel,
) {
    Column(Modifier.padding(Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.library_files_mode), style = MaterialTheme.typography.titleSmall)
        val modes = listOf(LibraryFilesState.MODE_COPY, LibraryFilesState.MODE_MOVE)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { model.presenter.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                ) {
                    Text(
                        stringResource(
                            if (mode ==
                                LibraryFilesState.MODE_MOVE
                            ) {
                                R.string.library_files_mode_move
                            } else {
                                R.string.library_files_mode_copy
                            },
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
        MutedText(
            stringResource(
                if (state.mode ==
                    LibraryFilesState.MODE_MOVE
                ) {
                    R.string.library_files_mode_move_hint
                } else {
                    R.string.library_files_mode_copy_hint
                },
            ),
        )
    }
}

@Composable
private fun BrowserDialog(
    state: LibraryFilesState,
    model: FilesModel,
) {
    val browser = state.browser ?: return
    AlertDialog(
        onDismissRequest = { model.presenter.closeBrowser() },
        title = { Text(stringResource(R.string.library_files_browser_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(
                    browser.path.ifEmpty { stringResource(R.string.library_files_browser_root) },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!browser.atRoot) {
                    TextButton(onClick = { model.run { browseUp() } }, enabled = !browser.loading) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = null)
                        Text(stringResource(R.string.library_files_browser_up))
                    }
                }
                if (browser.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                browser.error?.let { MutedText(libraryErrorText(it)) }
                if (browser.files > 0) MutedText(pluralStringResource(R.plurals.library_files_browser_files, browser.files, browser.files))
                if (!browser.loading && browser.folders.isEmpty() && browser.error == null) {
                    MutedText(stringResource(R.string.library_files_browser_none))
                }
                LazyColumn(Modifier.height(280.dp)) {
                    items(browser.folders, key = { it.path }) { folder ->
                        ListItem(
                            leadingContent = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                            headlineContent = { Text(folder.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier.clickable(enabled = !browser.loading) { model.run { browse(folder.path) } },
                        )
                    }
                }
                if (browser.more) MutedText(stringResource(R.string.library_files_browser_more))
            }
        },
        confirmButton = {
            TextButton(onClick = { model.run { useBrowsed() } }, enabled = !browser.atRoot && !browser.loading) {
                Text(stringResource(R.string.library_files_browser_use))
            }
        },
        dismissButton = { TextButton(onClick = { model.presenter.closeBrowser() }) { Text(stringResource(R.string.library_cancel)) } },
    )
}

@Composable
private fun EditorDialog(
    state: LibraryFilesState,
    model: FilesModel,
) {
    val editor = state.editor ?: return
    var query by rememberSaveable(editor.path) { mutableStateOf(editor.query) }
    AlertDialog(
        onDismissRequest = { model.presenter.closeEditor() },
        title = { Text(stringResource(R.string.library_files_editor_title)) },
        text = {
            LazyColumn(Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(256) },
                        label = { Text(stringResource(R.string.library_files_editor_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Button(onClick = { model.run { editorSearch(query) } }, enabled = query.isNotBlank() && !editor.searching) {
                        Text(stringResource(R.string.library_files_editor_lookup))
                    }
                }
                if (editor.searching || editor.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                editor.error?.let { item { MutedText(libraryErrorText(it)) } }
                if (editor.nothing) item { MutedText(stringResource(R.string.library_files_editor_nothing)) }
                items(editor.results, key = { it.id }) { title ->
                    val picked = editor.titleId == title.id
                    ListItem(
                        leadingContent = { PosterThumb(title.artwork.firstOrNull(), title.name) },
                        headlineContent = {
                            Text(
                                if (title.year >
                                    0
                                ) {
                                    stringResource(R.string.library_stats_name_year, title.name, title.year)
                                } else {
                                    title.name
                                },
                            )
                        },
                        supportingContent = { MutedText(kindWord(title.kind)) },
                        trailingContent = {
                            TextButton(onClick = { model.run { editorChoose(title.id) } }, enabled = !picked && !editor.loading) {
                                Text(
                                    stringResource(
                                        if (picked) R.string.library_files_editor_picked else R.string.library_files_editor_pick,
                                    ),
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
                if (editor.titleId > 0 && !editor.loading) {
                    if (editor.versions.size > 1) {
                        item {
                            Choice(
                                stringResource(R.string.library_files_editor_version),
                                editor.versions
                                    .firstOrNull { it.id == editor.versionId }
                                    ?.label
                                    .orEmpty(),
                                editor.versions.map { it.id.toString() to it.label },
                            ) { model.presenter.editorVersion(it.toLong()) }
                        }
                    }
                    if (editor.titleKind == "show") {
                        item {
                            val seasonName: @Composable (Int) -> String = { n ->
                                if (n ==
                                    0
                                ) {
                                    stringResource(R.string.library_files_editor_specials)
                                } else {
                                    stringResource(R.string.library_files_editor_season_n, n)
                                }
                            }
                            Choice(
                                stringResource(R.string.library_files_editor_season),
                                if (editor.season >=
                                    0
                                ) {
                                    seasonName(editor.season)
                                } else {
                                    stringResource(R.string.library_files_editor_pick_one)
                                },
                                editor.seasons.map { it.toString() to seasonName(it) },
                            ) { model.presenter.editorSeason(it.toInt()) }
                        }
                        if (editor.season >= 0) {
                            item {
                                val line: @Composable (Int, Int, String) -> String = { s, e, n ->
                                    stringResource(R.string.library_files_editor_episode_line, s, e, n)
                                }
                                val current = editor.episodes.firstOrNull { it.episode == editor.episode }
                                Choice(
                                    stringResource(R.string.library_files_editor_episode),
                                    current?.let { line(it.season, it.episode, it.name) }
                                        ?: stringResource(R.string.library_files_editor_pick_one),
                                    editor.episodes.map { it.episode.toString() to line(it.season, it.episode, it.name) },
                                ) { model.presenter.editorEpisode(it.toInt()) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { model.presenter.editorUse() },
                enabled = editor.canUse,
            ) { Text(stringResource(R.string.library_files_editor_use)) }
        },
        dismissButton = { TextButton(onClick = { model.presenter.closeEditor() }) { Text(stringResource(R.string.library_cancel)) } },
    )
}

@Composable
private fun kindWord(kind: String): String =
    stringResource(
        when (kind) {
            "show" -> R.string.library_kind_show
            "artist" -> R.string.library_kind_music
            else -> R.string.library_kind_movie
        },
    )

@Composable
private fun Choice(
    label: String,
    current: String,
    options: List<Pair<String, String>>,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        MutedText(label)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(current, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore, contentDescription = label)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (id, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = {
                        open = false
                        onPick(id)
                    })
                }
            }
        }
    }
}
