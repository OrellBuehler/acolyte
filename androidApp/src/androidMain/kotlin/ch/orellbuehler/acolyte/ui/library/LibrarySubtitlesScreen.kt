package ch.orellbuehler.acolyte.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.library.LibrarySubtitlesPresenter
import ch.orellbuehler.acolyte.feature.library.LibrarySubtitlesState
import ch.orellbuehler.acolyte.feature.library.SubtitleRow
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing

private typealias SubtitlesModel = PresenterViewModel<LibrarySubtitlesPresenter>

@Composable
fun LibrarySubtitlesScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val model: SubtitlesModel = viewModel(key = "library-subtitles") { PresenterViewModel(sdk.librarySubtitles()) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    WhileVisible { model.presenter.watch() }

    ScreenScaffold(
        title = stringResource(R.string.library_subs_title),
        onBack = onBack,
        actions = {
            TextButton(onClick = { model.run { searchAll() } }, enabled = state.total > 0 && !state.searching && !state.queuing) {
                Text(stringResource(R.string.library_subs_search_all))
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Filters(state, model)
                if (state.searching) {
                    Column(Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
                        LinearProgressIndicator(
                            progress = { if (state.searchTotal > 0) state.searchDone.toFloat() / state.searchTotal else 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        MutedText(stringResource(R.string.library_subs_searched, state.searchDone, state.searchTotal))
                    }
                }
                StatusContent(
                    status = state.status,
                    isEmpty = state.isEmpty,
                    onRefresh = { model.run { refresh() } },
                    modifier = Modifier.weight(1f),
                    empty = {
                        EmptyView(
                            title =
                                stringResource(
                                    if (state.filtered) R.string.library_subs_nothing_matches else R.string.library_subs_none,
                                ),
                            icon = Icons.Rounded.Subtitles,
                        )
                    },
                ) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                        item {
                            MutedText(
                                pluralStringResource(R.plurals.library_subs_count, state.total, state.total),
                                Modifier.padding(horizontal = Spacing.Screen),
                            )
                        }
                        items(
                            state.rows,
                            key = { "${it.row.fileId}-${it.row.language}-${it.row.forced}-${it.row.hearingImpaired}" },
                        ) { row ->
                            SubtitleRowItem(row) { model.run { searchRow(row.row.fileId) } }
                        }
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
            }
            NoticeHost(state.notice) { model.presenter.dismissNotice(it) }
        }
    }
}

@Composable
private fun Filters(
    state: LibrarySubtitlesState,
    model: SubtitlesModel,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen, vertical = Spacing.XSmall),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Choice(
            label = stringResource(R.string.library_subs_language),
            current =
                state.languages.firstOrNull { it.code == state.language }?.name ?: stringResource(R.string.library_subs_every),
            options = listOf("" to stringResource(R.string.library_subs_every)) + state.languages.map { it.code to it.name },
        ) { model.run { setLanguage(it) } }
        val kinds =
            listOf(
                "" to stringResource(R.string.library_subs_kind_both),
                "movie" to stringResource(R.string.library_subs_kind_movie),
                "episode" to stringResource(R.string.library_subs_kind_episode),
            )
        Choice(
            stringResource(R.string.library_subs_kind),
            kinds.first { it.first == state.kind }.second,
            kinds,
        ) { model.run { setKind(it) } }
        val titles =
            listOf("0" to stringResource(R.string.library_subs_all_titles)) +
                state.titles.map { it.id.toString() to stringResource(R.string.library_subs_title_option, it.name, it.count) }
        Choice(
            stringResource(R.string.library_subs_movie_or_show),
            titles.firstOrNull { it.first == state.titleId.toString() }?.second ?: titles.first().second,
            titles,
        ) { model.run { setTitle(it.toLongOrNull() ?: 0) } }
    }
}

@Composable
private fun Choice(
    label: String,
    current: String,
    options: List<Pair<String, String>>,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text(current, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.ExpandMore, contentDescription = label, modifier = Modifier.size(18.dp))
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

@Composable
private fun SubtitleRowItem(
    row: SubtitleRow,
    onSearch: () -> Unit,
) {
    val m = row.row
    val name = if (m.kind == "episode") stringResource(R.string.library_subs_episode, m.title, m.season, m.episode) else m.title
    val language =
        when {
            m.hearingImpaired -> stringResource(R.string.library_subs_hearing, row.languageName)
            m.forced -> stringResource(R.string.library_subs_forced, row.languageName)
            else -> row.languageName
        }
    val status =
        when {
            m.state == "none_good_enough" -> {
                val base = pluralStringResource(R.plurals.library_subs_none_good, m.tries, m.tries)
                if (m.nextTryAt.isNotEmpty()) {
                    stringResource(
                        R.string.library_subs_next_try,
                        base,
                        LibraryFormat.dateTime(m.nextTryAt),
                    )
                } else {
                    base
                }
            }
            m.tries > 0 -> pluralStringResource(R.plurals.library_subs_not_found, m.tries, m.tries)
            else -> stringResource(R.string.library_subs_not_looked)
        }
    ListItem(
        leadingContent = { PosterThumb(row.poster, m.title) },
        headlineContent = { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Text(language, style = MaterialTheme.typography.bodyMedium)
                MutedText(status)
            }
        },
        trailingContent = {
            if (row.busy) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onSearch) { Text(stringResource(R.string.library_subs_search)) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}
