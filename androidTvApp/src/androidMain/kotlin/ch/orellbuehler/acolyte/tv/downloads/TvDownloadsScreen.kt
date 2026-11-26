package ch.orellbuehler.acolyte.tv.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.downloads.DownloadItem
import ch.orellbuehler.acolyte.feature.downloads.DownloadsEmpty
import ch.orellbuehler.acolyte.feature.downloads.DownloadsEmptyKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadsState
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvBackdropLayer
import ch.orellbuehler.acolyte.tv.components.TvChip
import ch.orellbuehler.acolyte.tv.components.TvChipRow
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.TvThumbCard
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.components.tvThumbRows
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.downloads.clientNoteText
import ch.orellbuehler.acolyte.ui.downloads.downloadFactsLine
import ch.orellbuehler.acolyte.ui.downloads.downloadSecondLine
import ch.orellbuehler.acolyte.ui.downloads.downloadsEmptyWords
import ch.orellbuehler.acolyte.ui.downloads.label
import ch.orellbuehler.acolyte.ui.downloads.statusLineText
import ch.orellbuehler.acolyte.ui.downloads.subLineText
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun TvDownloadsScreen(account: Account) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { TvDownloadsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    TvDownloadsContent(state, viewModel)
}

@Composable
fun TvDownloadsContent(
    state: DownloadsState,
    actions: TvDownloadsActions,
) {
    val navigator = LocalTvNavigator.current
    var ambient by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        TvBackdropLayer(ambient, blurred = true)
        TvStatusContent(
            status = state.status,
            isEmpty = state.isEmpty,
            reload = actions::refresh,
            empty = {
                TvEmptyView(
                    title =
                        stringResource(
                            if (state.manager) CommonR.string.downloads_empty_all_title else CommonR.string.downloads_empty_watch_title,
                        ),
                    message =
                        stringResource(if (state.manager) CommonR.string.downloads_empty_manager else CommonR.string.downloads_empty_watch),
                    icon = Icons.Rounded.Download,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = TvMetrics.focusPad, bottom = TvMetrics.marginY),
                verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap),
            ) {
                state.status.error?.let { error ->
                    item(key = "error") { TvNoticeBanner(errorText(error), Modifier.tvPageInset()) }
                }
                items(state.notes, key = { "note-${it.name}" }) { note -> TvNoticeBanner(clientNoteText(note), Modifier.tvPageInset()) }
                if (state.statusLine.review > 0) {
                    item(key = "review") {
                        TvNoticeBanner(stringResource(R.string.tv_downloads_review, state.statusLine.review), Modifier.tvPageInset())
                    }
                }
                item(key = "tabs") {
                    TvChipRow {
                        items(state.tabs, key = { it.tab.name }) { count ->
                            TvChip(
                                stringResource(count.tab.label()) + "  " + count.count,
                                state.filter.tab == count.tab,
                                { actions.setTab(count.tab) },
                            )
                        }
                    }
                }
                if (state.subs.isNotEmpty()) {
                    item(key = "subs") {
                        TvChipRow {
                            items(state.subs, key = { it.sub.name }) { count ->
                                TvChip(
                                    stringResource(count.sub.label()) + "  " + count.count,
                                    state.filter.sub == count.sub,
                                    { actions.setSub(count.sub) },
                                )
                            }
                        }
                    }
                }
                if (state.manager) {
                    item(key = "status") {
                        Text(
                            statusLineText(state.statusLine),
                            style = TvType.footnote,
                            color = TvColors.textMuted,
                            modifier = Modifier.tvPageInset(),
                        )
                    }
                }
                if (state.items.isEmpty()) item(key = "empty") { EmptyFiltered(state) }
                tvThumbRows(state.items, key = { it.key }) { item, modifier ->
                    DownloadCard(
                        item,
                        modifier,
                        onFocus = { ambient = item.poster },
                        onClick = {
                            if (item.row.titleId > 0) {
                                actions.openTitle(item.row.titleId) { navigator.openTitle(it.kind, it.tmdb, it.libraryId) }
                            }
                        },
                    )
                }
                if (state.moreOutside > 0) {
                    item(key = "more") {
                        TvActionButton(
                            stringResource(CommonR.string.downloads_show_more, state.moreOutside),
                            actions::showMore,
                            Modifier.tvPageInset().padding(vertical = TvMetrics.focusPad),
                            icon = Icons.Rounded.ArrowDownward,
                            enabled = !state.loadingMore,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFiltered(state: DownloadsState) {
    val words = downloadsEmptyWords(state, state.empty ?: DownloadsEmpty(DownloadsEmptyKind.TAB))
    TvEmptyView(
        title = words.title,
        message = words.message,
        icon = Icons.Rounded.Download,
        modifier = Modifier.fillMaxWidth().padding(vertical = TvMetrics.sectionGap),
    )
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    modifier: Modifier,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    TvThumbCard(image = item.poster, name = item.title, onClick = onClick, modifier = modifier, square = item.square, onFocus = onFocus) {
        Text(item.title, style = TvType.calloutBold, color = TvColors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val sub = subLineText(item.subLine)
        if (sub.isNotEmpty()) Text(sub, style = TvType.footnote, color = TvColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(item.word.label()),
                style = TvType.footnoteBold,
                color = TvColors.textFor(item.tone),
                maxLines = 1,
            )
            val second = downloadSecondLine(item)
            if (second.isNotEmpty()) {
                Text(
                    "· $second",
                    style = TvType.footnote,
                    color = item.problem?.let { TvColors.textFor(it.tone) } ?: TvColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LinearProgressIndicator(
            progress = { item.progress.toFloat().coerceIn(0f, 1f) },
            color = tvBarColor(item.bar),
            trackColor = TvColors.surfaceHigh,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(3.dp),
        )
        val facts = downloadFactsLine(item)
        if (facts.isNotEmpty()) Text(facts, style = TvType.caption, color = TvColors.textMuted, maxLines = 1)
    }
}
