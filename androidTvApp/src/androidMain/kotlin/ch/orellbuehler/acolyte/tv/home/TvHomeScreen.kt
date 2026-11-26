package ch.orellbuehler.acolyte.tv.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.home.HomeState
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvBackdropCard
import ch.orellbuehler.acolyte.tv.components.TvBackdropLayer
import ch.orellbuehler.acolyte.tv.components.TvBillboard
import ch.orellbuehler.acolyte.tv.components.TvCardRow
import ch.orellbuehler.acolyte.tv.components.TvDiscoverCard
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvShelf
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.nav.TvSections
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText

private class HomeRow(
    val title: String,
    val row: CardRow,
)

@Composable
fun TvHomeScreen() {
    val sdk = LocalSdk.current
    val viewModel = viewModel { TvHomeViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    TvHomeContent(state, viewModel::refresh)
}

@Composable
fun TvHomeContent(
    state: HomeState,
    onRefresh: () -> Unit,
) {
    val navigator = LocalTvNavigator.current
    val featured = state.pictures
    val rows = homeRows(state)
    val first =
        featured.firstOrNull()?.card ?: rows
            .firstOrNull()
            ?.row
            ?.cards
            ?.firstOrNull()
    var billboard by remember { mutableStateOf<DiscoverCard?>(null) }
    var ambient by remember { mutableStateOf<DiscoverCard?>(null) }
    val shown = billboard ?: first
    val label = featured.firstOrNull { it.card.key == shown?.key }

    Box(Modifier.fillMaxSize()) {
        TvBackdropLayer((ambient ?: shown)?.wideImage)
        TvStatusContent(
            status = state.status,
            isEmpty = first == null && state.comingUp.isEmpty(),
            reload = onRefresh,
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = TvMetrics.marginY),
                verticalArrangement = Arrangement.spacedBy(TvMetrics.sectionGap),
            ) {
                item(key = "billboard") {
                    Column(Modifier.fillParentMaxHeight(TvMetrics.HOME_HERO_FRACTION), verticalArrangement = Arrangement.Bottom) {
                        state.status.error?.let { TvNoticeBanner(errorText(it), Modifier.tvPageInset()) }
                        Spacer(Modifier.weight(1f))
                        if (shown != null) {
                            TvBillboard(
                                shown,
                                label = label?.let { tvPictureLabel(it) },
                                modifier = Modifier.tvPageInset().padding(bottom = 8.dp),
                            )
                        }
                        if (featured.isNotEmpty()) {
                            TvShelf(stringResource(R.string.tv_home_featured)) {
                                items(featured, key = { it.card.key }) { picture ->
                                    TvDiscoverCard(
                                        card = picture.card,
                                        wide = true,
                                        badge = tvPictureLabel(picture),
                                        showsText = false,
                                        onFocus = {
                                            billboard = picture.card
                                            ambient = picture.card
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                items(rows, key = { it.row.id }) { entry ->
                    TvCardRow(
                        row = entry.row,
                        title = entry.title,
                        seeAll = seeAll(entry.row, navigator.account) { navigator.openSection(Section.EXPLORE) },
                        onFocusCard = { ambient = it },
                    )
                }
                if (state.comingUp.isNotEmpty()) {
                    item(key = "coming-up") {
                        TvShelf(
                            title = stringResource(R.string.tv_home_coming_up),
                            seeAll =
                                if (TvSections.visible(
                                        Section.CALENDAR,
                                        navigator.account,
                                    )
                                ) {
                                    ({ navigator.openSection(Section.CALENDAR) })
                                } else {
                                    null
                                },
                        ) {
                            items(state.comingUp, key = { it.key }) { up ->
                                TvBackdropCard(
                                    title = up.name,
                                    subtitle = listOf(up.sub, tvUpWhen(up)).filter { it.isNotEmpty() }.joinToString(" · "),
                                    image = up.image,
                                    onClick = { if (up.tmdb.isNotEmpty()) navigator.openTitle(up.kind, up.tmdb) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun homeRows(state: HomeState): List<HomeRow> {
    val result = mutableListOf<HomeRow>()
    state.arrived?.let { result += HomeRow(tvArrivedTitle(state.arrivedTitle), it) }
    state.borrowed.forEach { result += HomeRow(it.title, it) }
    return result.filter { it.row.cards.isNotEmpty() }
}

private fun seeAll(
    row: CardRow,
    account: Account,
    open: () -> Unit,
): (() -> Unit)? = if (row.seeAll && TvSections.visible(Section.EXPLORE, account)) open else null
