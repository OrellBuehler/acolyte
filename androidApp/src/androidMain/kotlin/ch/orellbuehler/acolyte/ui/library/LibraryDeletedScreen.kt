package ch.orellbuehler.acolyte.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import ch.orellbuehler.acolyte.feature.library.HeldRow
import ch.orellbuehler.acolyte.feature.library.HeldShow
import ch.orellbuehler.acolyte.feature.library.LibraryDeletedPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryDeletedState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing

private typealias DeletedModel = PresenterViewModel<LibraryDeletedPresenter>

@Composable
fun LibraryDeletedScreen(
    titleId: Long,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val model: DeletedModel = viewModel(key = "library-deleted") { PresenterViewModel(sdk.libraryDeleted(titleId)) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.library_deleted_title), onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                StatusContent(
                    status = state.status,
                    isEmpty = state.isEmpty && state.titleId == 0L,
                    onRefresh = { model.run { refresh() } },
                    modifier = Modifier.weight(1f),
                    empty = { EmptyView(title = stringResource(R.string.library_deleted_none), icon = Icons.Rounded.DeleteOutline) },
                ) { DeletedList(state, model) }
                if (state.rows.isNotEmpty()) RestoreBar(state, model)
            }
            NoticeHost(state.notice) { model.presenter.dismissNotice(it) }
        }
    }
    state.question?.let { question ->
        AlertDialog(
            onDismissRequest = { model.run { answer("") } },
            title = { Text(stringResource(R.string.library_deleted_taken_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(stringResource(R.string.library_deleted_taken_body, question.name))
                    if (question.canSwap) MutedText(stringResource(R.string.library_deleted_swap_hint))
                    MutedText(stringResource(R.string.library_deleted_keep_both_hint))
                }
            },
            confirmButton = {
                Row {
                    if (question.canSwap) {
                        TextButton(onClick = { model.run { answer("swap") } }) { Text(stringResource(R.string.library_deleted_swap)) }
                    }
                    TextButton(onClick = { model.run { answer("both") } }) { Text(stringResource(R.string.library_deleted_keep_both)) }
                }
            },
            dismissButton = { TextButton(onClick = { model.run { answer("") } }) { Text(stringResource(R.string.library_cancel)) } },
        )
    }
}

@Composable
private fun DeletedList(
    state: LibraryDeletedState,
    model: DeletedModel,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
        item {
            Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                MutedText(pluralStringResource(R.plurals.library_deleted_files, state.count, state.count))
                MutedText(
                    when {
                        state.holdingDays < 0 -> stringResource(R.string.library_deleted_note_unknown)
                        state.holdingDays == 0 -> stringResource(R.string.library_deleted_note_forever)
                        else -> pluralStringResource(R.plurals.library_deleted_days, state.holdingDays, state.holdingDays)
                    },
                )
                if (!state.canRestore) NoticeBanner(stringResource(R.string.library_deleted_hands_off))
                if (state.titleId != 0L) {
                    InputChip(
                        selected = true,
                        onClick = { model.run { clearTitle() } },
                        label = {
                            Text(
                                if (state.titleName.isNotEmpty()) {
                                    stringResource(R.string.library_deleted_only_title, state.titleName)
                                } else {
                                    stringResource(R.string.library_deleted_one_title)
                                },
                            )
                        },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                    )
                }
                if (state.hasBoth) {
                    val shows = listOf(HeldShow.ALL, HeldShow.DELETED, HeldShow.REPLACED)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        shows.forEachIndexed { index, show ->
                            SegmentedButton(
                                selected = state.show == show,
                                onClick = { model.presenter.setShow(show) },
                                shape = SegmentedButtonDefaults.itemShape(index, shows.size),
                            ) {
                                Text(
                                    stringResource(
                                        when (show) {
                                            HeldShow.ALL -> R.string.library_deleted_show_all
                                            HeldShow.DELETED -> R.string.library_deleted_tag_deleted
                                            HeldShow.REPLACED -> R.string.library_deleted_tag_replaced
                                        },
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (state.rows.isEmpty()) {
            item { Centered { MutedText(stringResource(R.string.library_deleted_none)) } }
        } else {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.library_deleted_select_all)) },
                    leadingContent = { Checkbox(checked = state.allSelected, onCheckedChange = { model.presenter.toggleAll() }) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickable { model.presenter.toggleAll() },
                )
            }
        }
        items(state.rows, key = { it.file.id }) { row -> HeldRowItem(row) { model.presenter.toggle(row.file.id) } }
    }
}

@Composable
private fun HeldRowItem(
    row: HeldRow,
    onToggle: () -> Unit,
) {
    val file = row.file
    ListItem(
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = row.selected, onCheckedChange = { onToggle() })
                PosterThumb(row.poster, file.title)
            }
        },
        headlineContent = { Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                StatusChip(
                    stringResource(if (file.replaced) R.string.library_deleted_tag_replaced else R.string.library_deleted_tag_deleted),
                    if (file.replaced) Tone.INFO else Tone.IDLE,
                )
                Text(row.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val day = LibraryFormat.day(file.deletedAt)
                val action =
                    when {
                        file.replaced && file.replacedBy.isNotEmpty() ->
                            stringResource(R.string.library_deleted_replaced_by, day, LibraryFormat.fileNameOf(file.replacedBy))
                        file.replaced -> stringResource(R.string.library_deleted_replaced_on, day)
                        else -> stringResource(R.string.library_deleted_on, day)
                    }
                val withTitle = if (file.titleDeleted) " " + stringResource(R.string.library_deleted_with_title) else ""
                MutedText(LibraryFormat.bytes(file.size) + " · " + action + withTitle)
            }
        },
        trailingContent = {
            val left =
                when {
                    row.daysLeft < 0 -> stringResource(R.string.library_deleted_kept)
                    row.daysLeft == 0 -> stringResource(R.string.library_deleted_today)
                    else -> pluralStringResource(R.plurals.library_deleted_left, row.daysLeft, row.daysLeft)
                }
            Text(
                left,
                style = MaterialTheme.typography.labelSmall,
                color = if (row.daysLeft in 0..1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onToggle),
    )
}

@Composable
private fun RestoreBar(
    state: LibraryDeletedState,
    model: DeletedModel,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Small), horizontalArrangement = Arrangement.End) {
            Button(onClick = { model.run { restore() } }, enabled = state.canRestore && state.selectedCount > 0 && !state.busy) {
                Text(
                    if (state.selectedCount > 0) {
                        stringResource(R.string.library_deleted_restore_n, state.selectedCount)
                    } else {
                        stringResource(R.string.library_deleted_restore)
                    },
                )
            }
        }
    }
}
