package ch.orellbuehler.acolyte.tv.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
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
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.ExploreState
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.browseLink
import ch.orellbuehler.acolyte.feature.discover.tileHue
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvBrowsePage
import ch.orellbuehler.acolyte.tv.components.TvBrowseTile
import ch.orellbuehler.acolyte.tv.components.TvCardRow
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvInlineLoading
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvShelf
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.TvTypeChipRow
import ch.orellbuehler.acolyte.tv.components.tvDiscoverRows
import ch.orellbuehler.acolyte.tv.components.tvGenreText
import ch.orellbuehler.acolyte.tv.components.tvHeroPillText
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.components.tvStripLabel
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.nav.TvSections
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText

class TvExploreActions(
    val refresh: () -> Unit,
    val select: (ExploreType) -> Unit,
    val loadMore: () -> Unit,
    val selectService: (Long) -> Unit,
    val stripShown: () -> Unit,
    val stripOpened: (BrowseTile) -> Unit,
)

@Composable
fun TvExploreScreen(account: Account) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { TvExploreViewModel(sdk) }
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
            TvExploreActions(
                refresh = viewModel::refresh,
                select = viewModel::select,
                loadMore = viewModel::loadMore,
                selectService = viewModel::selectService,
                stripShown = viewModel::stripShown,
                stripOpened = viewModel::stripOpened,
            )
        }
    TvExploreContent(state, TvSections.visible(Section.POPULAR, account), actions)
}

@Composable
fun TvExploreContent(
    state: ExploreState,
    popularVisible: Boolean,
    actions: TvExploreActions,
) {
    val navigator = LocalTvNavigator.current
    val banner = state.banner
    val bannerRow =
        banner?.takeIf { it.picks.isNotEmpty() }?.let {
            CardRow(
                id = "banner",
                type = "banner",
                title = stringResource(R.string.tv_discover_banner_title, tvGenreText(it.genre.name)),
                subtitle = stringResource(R.string.tv_discover_banner_line, it.have, it.picks.size),
                cards = it.picks,
            )
        }
    TvStatusContent(
        status = state.status,
        isEmpty = state.isEmpty,
        reload = actions.refresh,
        empty = {
            TvEmptyView(
                title = stringResource(R.string.tv_discover_explore_building_title),
                message = stringResource(R.string.tv_discover_explore_building_message),
                icon = Icons.Rounded.Explore,
            )
        },
    ) {
        TvBrowsePage(
            hero = state.hero.firstOrNull(),
            pills = state.heroPills.map { tvHeroPillText(it) },
            header = {
                Column {
                    TvTypeChipRow(state.type, actions.select)
                    state.status.error?.let { TvNoticeBanner(errorText(it), Modifier.tvPageInset()) }
                    if (state.sourceOff) TvNoticeBanner(stringResource(R.string.tv_discover_source_off), Modifier.tvPageInset())
                }
            },
        ) { onFocusCard ->
            if (bannerRow != null && banner != null) {
                item(key = "banner") {
                    TvCardRow(
                        bannerRow,
                        seeAll = {
                            val type =
                                when {
                                    banner.both -> ExploreType.ALL
                                    banner.genre.kind == CardKind.SHOW -> ExploreType.SHOWS
                                    else -> ExploreType.MOVIES
                                }
                            navigator.openGenre(type.wire, banner.genre.id)
                        },
                        onFocusCard = onFocusCard,
                    )
                }
            }
            if (state.strip.isNotEmpty()) {
                item(key = "strip") {
                    LaunchedEffect(Unit) { actions.stripShown() }
                    TvShelf(stringResource(R.string.tv_discover_categories)) {
                        items(state.strip, key = { "${it.kind}-${it.genre}-${it.category}-${it.mood}-${it.name}" }) { tile ->
                            TvBrowseTile(
                                name = if (tile.category.isEmpty()) tvGenreText(tile.name) else tile.name,
                                image = tile.backdrop,
                                hue = tileHue(tile.name),
                                label = tvStripLabel(tile.section),
                                onClick = {
                                    actions.stripOpened(tile)
                                    navigator.openBrowse(browseLink(tile, state.type))
                                },
                            )
                        }
                        item(key = "all") {
                            TvBrowseTile(
                                name = stringResource(R.string.tv_discover_all_categories),
                                image = state.strip.firstOrNull()?.backdrop,
                                label = stringResource(R.string.tv_discover_strip_browse),
                                onClick = { navigator.openSection(Section.SEARCH) },
                            )
                        }
                    }
                }
            }
            tvDiscoverRows(
                rows = state.rows,
                top = state.top,
                type = state.type,
                onPickService = actions.selectService,
                genres = state.genres,
                onSeeAllNew = if (popularVisible) ({ navigator.openSection(Section.POPULAR) }) else null,
                onFocusCard = onFocusCard,
            )
            if (state.more) {
                item(key = "more") {
                    LaunchedEffect(state.rows.size) { actions.loadMore() }
                    TvInlineLoading()
                }
            }
        }
    }
}
