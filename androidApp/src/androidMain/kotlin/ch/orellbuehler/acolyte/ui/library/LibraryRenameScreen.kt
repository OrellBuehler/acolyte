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
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.library.LibraryRenamePresenter
import ch.orellbuehler.acolyte.feature.library.LibraryRenameState
import ch.orellbuehler.acolyte.feature.library.RenameFailure
import ch.orellbuehler.acolyte.feature.library.RenameRow
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun LibraryRenameScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val model: PresenterViewModel<LibraryRenamePresenter> =
        viewModel(key = "library-rename") { PresenterViewModel(sdk.libraryRename()) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.library_rename_title), onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                StatusContent(
                    status = state.status,
                    isEmpty = state.isEmpty,
                    onRefresh = { model.run { refresh() } },
                    modifier = Modifier.weight(1f),
                    empty = {
                        EmptyView(
                            title = stringResource(R.string.library_rename_nothing),
                            icon = Icons.Rounded.DriveFileRenameOutline,
                        )
                    },
                ) { RenameList(state) }
                if (state.changes.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                        Row(
                            Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Small),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(onClick = { confirm = true }, enabled = state.canRename && !state.running) {
                                Text(pluralStringResource(R.plurals.library_rename_button, state.changes.size, state.changes.size))
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
            title = pluralStringResource(R.plurals.library_rename_confirm, state.changes.size, state.changes.size),
            text = stringResource(R.string.library_rename_confirm_text),
            confirm = pluralStringResource(R.plurals.library_rename_button, state.changes.size, state.changes.size),
            onConfirm = { model.run { rename() } },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun RenameList(state: LibraryRenameState) {
    var skippedOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.Screen, end = Spacing.Screen, bottom = Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                MutedText(pluralStringResource(R.plurals.library_rename_count, state.changes.size, state.changes.size))
                if (!state.canRename) NoticeBanner(stringResource(R.string.library_rename_hands_off))
                if (state.changes.isNotEmpty()) MutedText(stringResource(R.string.library_rename_note))
                when (state.failure) {
                    RenameFailure.PLAN_CHANGED -> NoticeBanner(stringResource(R.string.library_rename_changed))
                    RenameFailure.FAILED -> NoticeBanner(state.failureText.ifEmpty { stringResource(R.string.library_rename_failed) })
                    RenameFailure.NONE -> Unit
                }
                if (state.running) {
                    LinearProgressIndicator(progress = { state.job.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    MutedText(
                        state.job.note.ifEmpty {
                            stringResource(if (state.job.progress > 0) R.string.library_rename_running else R.string.library_rename_waiting)
                        },
                    )
                }
            }
        }
        if (state.changes.isEmpty() && state.skipped.isNotEmpty()) item { MutedText(stringResource(R.string.library_rename_nothing)) }
        items(state.changes, key = { "c${it.fileId}" }) { RenameCard(it) }
        if (state.skipped.isNotEmpty()) {
            item {
                val open = skippedOpen || state.changes.isEmpty()
                ListItem(
                    headlineContent = {
                        Text(
                            pluralStringResource(R.plurals.library_rename_skipped, state.skipped.size, state.skipped.size),
                        )
                    },
                    trailingContent = { Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickable { skippedOpen = !skippedOpen },
                )
            }
            if (skippedOpen || state.changes.isEmpty()) {
                items(state.skipped, key = { "s${it.fileId}" }) { row ->
                    Column(Modifier.padding(vertical = Spacing.XSmall)) {
                        Text(row.title, style = MaterialTheme.typography.titleSmall)
                        Text(row.from, style = MaterialTheme.typography.bodySmall)
                        MutedText(row.skip)
                    }
                }
            }
        }
        if (state.same > 0) item { MutedText(pluralStringResource(R.plurals.library_rename_same, state.same, state.same)) }
    }
}

@Composable
private fun RenameCard(row: RenameRow) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.Medium), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            PosterThumb(row.poster, row.title)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(row.title, style = MaterialTheme.typography.titleSmall)
                MutedText(stringResource(R.string.library_rename_now))
                Text(row.from, style = MaterialTheme.typography.bodySmall)
                MutedText(stringResource(R.string.library_rename_new))
                Text(row.to, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                row.subtitles.forEach { sub ->
                    MutedText(
                        if (sub.skip.isNotEmpty()) {
                            stringResource(R.string.library_rename_subtitle_stays, sub.from, sub.skip)
                        } else {
                            stringResource(R.string.library_rename_subtitle_to, sub.from, sub.to)
                        },
                    )
                }
            }
        }
    }
}
