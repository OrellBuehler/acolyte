package ch.orellbuehler.acolyte.tv.popular

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.PopularState
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvBrowsePage
import ch.orellbuehler.acolyte.tv.components.TvChipShelf
import ch.orellbuehler.acolyte.tv.components.TvDiscoverCard
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvInlineLoading
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvShelf
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.TvTypeChipRow
import ch.orellbuehler.acolyte.tv.components.tvHeroPillText
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText

class TvPopularActions(
    val refresh: () -> Unit,
    val select: (ExploreType) -> Unit,
    val loadMore: () -> Unit,
    val selectService: (String, Long) -> Unit,
)

@Composable
fun TvPopularScreen(account: Account) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { TvPopularViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshOwned()
            viewModel.watch()
        }
    }
    val actions =
        remember(viewModel) {
            TvPopularActions(viewModel::refresh, viewModel::select, viewModel::loadMore, viewModel::selectService)
        }
    TvPopularContent(state, actions)
}

@Composable
fun TvPopularContent(
    state: PopularState,
    actions: TvPopularActions,
) {
    TvStatusContent(
        status = state.status,
        isEmpty = state.isEmpty,
        reload = actions.refresh,
        empty = {
            TvEmptyView(
                title = stringResource(R.string.tv_discover_popular_building_title),
                message = stringResource(R.string.tv_discover_explore_building_message),
                icon = Icons.Rounded.AutoAwesome,
            )
        },
    ) {
        TvBrowsePage(
            hero = state.hero,
            pills = state.heroPills.map { tvHeroPillText(it, state.heroRank, state.heroService) },
            header = {
                Column {
                    TvTypeChipRow(state.type, actions.select)
                    state.status.error?.let { TvNoticeBanner(errorText(it), Modifier.tvPageInset()) }
                    if (state.sourceOff) TvNoticeBanner(stringResource(R.string.tv_discover_source_off), Modifier.tvPageInset())
                }
            },
        ) { onFocusCard ->
            if (state.days.isNotEmpty()) {
                item(key = "week") {
                    Text(
                        stringResource(R.string.tv_discover_this_week),
                        style = TvType.title3,
                        color = TvColors.text,
                        modifier = Modifier.tvPageInset(),
                    )
                }
                items(state.days, key = { "day-" + it.day }) { day ->
                    TvShelf(title = tvDayTitle(day)) {
                        items(day.entries, key = { it.card.key }) { entry ->
                            TvDiscoverCard(
                                card = entry.card,
                                wide = true,
                                subtitle = tvRailWhat(entry),
                                onFocus = { onFocusCard(entry.card) },
                            )
                        }
                    }
                }
            }
            items(state.rows, key = { "row-" + it.row.id }) { item ->
                TvChipShelf(
                    title = item.row.title,
                    row = item.row,
                    subtitle = item.row.subtitle.ifEmpty { null },
                    chips = item.chips,
                    selected = item.selected,
                    showAll = true,
                    onPick = { actions.selectService(item.row.id, it) },
                    ranked = item.row.ranked,
                    wide = !item.row.ranked,
                    loading = item.loading,
                    onFocusCard = onFocusCard,
                )
            }
            if (state.more) {
                item(key = "more") {
                    LaunchedEffect(state.rows.size) { actions.loadMore() }
                    TvInlineLoading()
                }
            }
        }
    }
}
