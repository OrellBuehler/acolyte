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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import ch.orellbuehler.acolyte.feature.library.CopyRow
import ch.orellbuehler.acolyte.feature.library.LibraryCopiesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryCopiesState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.util.Locale

private typealias CopiesModel = PresenterViewModel<LibraryCopiesPresenter>

@Composable
fun LibraryCopiesScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val model: CopiesModel = viewModel(key = "library-copies") { PresenterViewModel(sdk.libraryCopies()) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    ScreenScaffold(title = stringResource(R.string.library_codecs_title), onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it.take(256)
                        model.latest("search") { search(query) }
                    },
                    placeholder = { Text(stringResource(R.string.library_search_movie)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                model.latest("search") { search("") }
                            }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.library_search_clear)) }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CodecMenu(state, model)
                    SortMenu(state, model)
                    Text(
                        pluralStringResource(R.plurals.library_codecs_count, state.total, state.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                }
                if (state.started >= 0) {
                    Text(
                        pluralStringResource(R.plurals.library_codecs_started, state.started, state.started),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
                    )
                }
                StatusContent(
                    status = state.status,
                    isEmpty = state.isEmpty,
                    onRefresh = { model.run { refresh() } },
                    modifier = Modifier.weight(1f),
                    empty = { Centered { MutedText(stringResource(R.string.library_codecs_empty)) } },
                ) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                        items(state.rows, key = { it.item.fileId }) { row -> CopyRowItem(row) { model.presenter.toggle(row.item.fileId) } }
                        item {
                            Pager(
                                state.from,
                                state.to,
                                state.total,
                                state.hasPrevious,
                                state.hasNext,
                                { model.run { previousPage() } },
                                { model.run { nextPage() } },
                            )
                        }
                    }
                }
                if (state.selectedCount > 0) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                        Row(
                            Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                pluralStringResource(R.plurals.library_selected, state.selectedCount, state.selectedCount),
                                modifier = Modifier.weight(1f).padding(start = Spacing.Small),
                            )
                            Button(
                                onClick = { model.run { review() } },
                                enabled = !state.busy,
                            ) { Text(stringResource(R.string.library_codecs_review)) }
                        }
                    }
                }
            }
            NoticeHost(state.notice) { model.presenter.dismissNotice(it) }
        }
    }
    state.review?.let { review ->
        AlertDialog(
            onDismissRequest = { model.presenter.cancelReview() },
            title = { Text(pluralStringResource(R.plurals.library_codecs_review_title, review.items.size, review.items.size)) },
            text = {
                LazyColumn(Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    item { MutedText(pluralStringResource(R.plurals.library_codecs_review_note, review.slotCount, review.slotCount)) }
                    items(review.items) { item ->
                        Column {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            MutedText(stringResource(R.string.library_copy_line, item.version, codecLabel(item.codec)))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { model.run { start() } }, enabled = !state.busy) { Text(stringResource(R.string.library_start)) }
            },
            dismissButton = { TextButton(onClick = { model.presenter.cancelReview() }) { Text(stringResource(R.string.library_cancel)) } },
        )
    }
}

@Composable
private fun CodecMenu(
    state: LibraryCopiesState,
    model: CopiesModel,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text(
                if (state.codec == LibraryCopiesState.ALL) stringResource(R.string.library_codecs_all) else codecLabel(state.codec),
                maxLines = 1,
            )
            Icon(Icons.Rounded.ExpandMore, contentDescription = stringResource(R.string.library_codecs_codec))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.codecs.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (option.id == LibraryCopiesState.ALL) {
                                stringResource(R.string.library_codecs_all)
                            } else {
                                stringResource(R.string.library_codecs_option, codecLabel(option.id), option.count)
                            },
                        )
                    },
                    onClick = {
                        open = false
                        model.run { setCodec(option.id) }
                    },
                )
            }
        }
    }
}

@Composable
private fun SortMenu(
    state: LibraryCopiesState,
    model: CopiesModel,
) {
    var open by remember { mutableStateOf(false) }
    val labels =
        mapOf(
            "title" to stringResource(R.string.library_codecs_sort_title),
            "codec" to stringResource(R.string.library_codecs_sort_codec),
            "added" to stringResource(R.string.library_codecs_sort_added),
        )
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text(labels[state.sort] ?: state.sort, maxLines = 1)
            Icon(Icons.Rounded.ExpandMore, contentDescription = stringResource(R.string.library_codecs_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LibraryCopiesState.SORTS.forEach { sort ->
                DropdownMenuItem(text = { Text(labels[sort] ?: sort) }, onClick = {
                    open = false
                    model.run { setSort(sort) }
                })
            }
        }
    }
}

@Composable
private fun CopyRowItem(
    row: CopyRow,
    onToggle: () -> Unit,
) {
    val copy = row.item
    val name = if (copy.year > 0) stringResource(R.string.library_stats_name_year, copy.title, copy.year) else copy.title
    ListItem(
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = row.selected, onCheckedChange = { onToggle() }, enabled = !copy.isProtected)
                PosterThumb(row.poster, copy.title)
            }
        },
        headlineContent = { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                MutedText(listOf(copy.version, copy.edition).filter { it.isNotEmpty() }.joinToString(" · "))
                val codec =
                    if (copy.codec.isEmpty()) {
                        stringResource(R.string.library_codecs_not_read)
                    } else {
                        listOf(
                            codecLabel(copy.codec),
                            copy.container.uppercase(Locale.ROOT),
                            copy.inspectionStatus
                                .takeIf {
                                    it !=
                                        "complete"
                                }.orEmpty(),
                        ).filter { it.isNotEmpty() }
                            .joinToString(" · ")
                    }
                Text(codec, style = MaterialTheme.typography.bodyMedium)
                val audio =
                    listOf(
                        copy.audioLanguages.joinToString(", ") { languageName(it) }.ifEmpty { "—" },
                        pluralStringResource(R.plurals.library_codecs_tracks, copy.audioTracks, copy.audioTracks),
                        if (copy.audioUnknown) stringResource(R.string.library_codecs_some_unknown) else "",
                    ).filter { it.isNotEmpty() }.joinToString(" · ")
                MutedText(audio)
                MutedText(row.fileName)
                if (copy.isProtected) StatusChip(stringResource(R.string.library_codecs_protected), Tone.IDLE)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(enabled = !copy.isProtected, onClick = onToggle),
    )
}

fun languageName(code: String): String {
    val name = Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault())
    return if (name.isBlank() || name == code) code.uppercase(Locale.ROOT) else name
}
