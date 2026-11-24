package ch.orellbuehler.acolyte.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.library.LibraryReleasesPresenter
import ch.orellbuehler.acolyte.feature.library.ReleaseRow
import ch.orellbuehler.acolyte.feature.library.ReleaseVerdict
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.LibraryReleasesRoute
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun LibraryReleasesScreen(
    turnedDown: Boolean,
    onBack: () -> Unit,
    open: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val model: PresenterViewModel<LibraryReleasesPresenter> =
        viewModel(key = "library-releases-$turnedDown") { PresenterViewModel(sdk.libraryReleases(turnedDown)) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    WhileVisible { model.presenter.watch() }
    var confirm by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(if (turnedDown) R.string.library_releases_turned_down else R.string.library_releases_held),
        onBack = onBack,
        actions = {
            if (state.total >
                0
            ) {
                TextButton(
                    onClick = { confirm = true },
                    enabled = !state.busy,
                ) { Text(stringResource(R.string.library_releases_clear_all)) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
                    listOf(false, true).forEachIndexed { index, other ->
                        SegmentedButton(
                            selected = turnedDown == other,
                            onClick = {
                                if (turnedDown != other) {
                                    onBack()
                                    open(LibraryReleasesRoute(other))
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                        ) {
                            Text(
                                stringResource(if (other) R.string.library_releases_turned_down else R.string.library_releases_held),
                                maxLines = 1,
                            )
                        }
                    }
                }
                StatusContent(
                    status = state.status,
                    isEmpty = state.isEmpty && state.offset == 0,
                    onRefresh = { model.run { refresh() } },
                    modifier = Modifier.weight(1f),
                    empty = {
                        val title = if (turnedDown) R.string.library_releases_none_turned else R.string.library_releases_none_held
                        val hint =
                            if (turnedDown) R.string.library_releases_none_turned_hint else R.string.library_releases_none_held_hint
                        EmptyView(
                            title = stringResource(title),
                            message = stringResource(hint),
                            icon = if (turnedDown) Icons.Rounded.Block else Icons.Rounded.HourglassTop,
                        )
                    },
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = Spacing.Screen, end = Spacing.Screen, bottom = Spacing.XLarge),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        item { MutedText(pluralStringResource(R.plurals.library_releases_count, state.total, state.total)) }
                        items(state.rows, key = { it.id }) { row ->
                            ReleaseCard(row, turnedDown, enabled = !state.busy) { model.run { remove(row.id) } }
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
    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.library_releases_clear_all),
            text =
                if (turnedDown) {
                    pluralStringResource(R.plurals.library_releases_clear_turned, state.total, state.total)
                } else {
                    pluralStringResource(R.plurals.library_releases_clear_held, state.total, state.total)
                },
            confirm = stringResource(R.string.library_releases_clear_all),
            danger = true,
            onConfirm = { model.run { clearAll() } },
            onDismiss = { confirm = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleaseCard(
    row: ReleaseRow,
    turnedDown: Boolean,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.Medium), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            PosterThumb(row.poster, row.title.ifEmpty { row.name })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                val heading = listOf(row.title, row.version).filter { it.isNotEmpty() }.joinToString(" · ")
                if (heading.isNotEmpty()) Text(heading, style = MaterialTheme.typography.titleSmall)
                Text(row.name, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall),
                    verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
                ) {
                    StatusChip(verdictLabel(row.verdict), row.verdict.tone)
                    row.tags.forEach { tag ->
                        SuggestionChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                MutedText(facts(row, turnedDown))
                row.reasons.forEach { MutedText("• $it") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onRemove, enabled = enabled) {
                        Text(stringResource(if (turnedDown) R.string.library_releases_take_off else R.string.library_releases_stop_holding))
                    }
                }
            }
        }
    }
}

@Composable
private fun facts(
    row: ReleaseRow,
    turnedDown: Boolean,
): String {
    val parts = mutableListOf(row.sourceName, LibraryFormat.bytes(row.size))
    if (row.seeders >= 0) parts += pluralStringResource(R.plurals.library_releases_seeders, row.seeders, row.seeders)
    parts += row.transport
    if (row.points >= 0) parts += stringResource(R.string.library_releases_points, row.points)
    if (row.rank > 0) parts += stringResource(R.string.library_releases_ranked, row.rank)
    if (!turnedDown &&
        row.holdUntil.isNotEmpty()
    ) {
        parts += stringResource(R.string.library_releases_sent, LibraryFormat.dateTime(row.holdUntil))
    }
    if (turnedDown && row.since.isNotEmpty()) parts += stringResource(R.string.library_releases_since, LibraryFormat.day(row.since))
    return parts.filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
private fun verdictLabel(verdict: ReleaseVerdict): String =
    stringResource(
        when (verdict) {
            ReleaseVerdict.TURNED_DOWN -> R.string.library_releases_verdict_turned_down
            ReleaseVerdict.CHOSEN -> R.string.library_releases_verdict_chosen
            ReleaseVerdict.HELD -> R.string.library_releases_verdict_held
            ReleaseVerdict.WOULD_CHOOSE -> R.string.library_releases_verdict_would_choose
            ReleaseVerdict.WOULD_HOLD -> R.string.library_releases_verdict_would_hold
            ReleaseVerdict.REFUSED -> R.string.library_releases_verdict_refused
            ReleaseVerdict.PASSED_OVER -> R.string.library_releases_verdict_passed_over
            ReleaseVerdict.JUDGED -> R.string.library_releases_verdict_judged
        },
    )
