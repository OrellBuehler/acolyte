package ch.orellbuehler.acolyte.tv.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.FilmRole
import ch.orellbuehler.acolyte.feature.discover.FilmSort
import ch.orellbuehler.acolyte.feature.discover.PersonState
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvAmbientPage
import ch.orellbuehler.acolyte.tv.components.TvCardRow
import ch.orellbuehler.acolyte.tv.components.TvChip
import ch.orellbuehler.acolyte.tv.components.TvChipRow
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvPersonCard
import ch.orellbuehler.acolyte.tv.components.TvShelf
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.requestFocusSafely
import ch.orellbuehler.acolyte.tv.components.tvLongDate
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ServerImage

@Composable
fun TvPersonScreen(id: String) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "person-$id") { TvPersonViewModel(sdk, id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.refreshOwned() } }
    TvPersonContent(state, viewModel::refresh, viewModel::select)
}

@Composable
fun TvPersonContent(
    state: PersonState,
    onRefresh: () -> Unit,
    onSelect: (FilmRole, FilmSort) -> Unit,
) {
    val navigator = LocalTvNavigator.current
    var about by remember { mutableStateOf(false) }
    val rows = personRows(state)
    TvStatusContent(status = state.status, isEmpty = false, reload = onRefresh) {
        TvAmbientPage(fallback = state.photo, topPadding = TvMetrics.marginY + TvMetrics.focusPad) { onFocusImage ->
            item(key = "header") { PersonHeader(state) { about = true } }
            state.limit?.let { limit ->
                item(key = "limit") {
                    TvNoticeBanner(stringResource(R.string.tv_discover_person_limit, limit.used, limit.left), Modifier.tvPageInset())
                }
            }
            items(rows, key = { it.id }) { row ->
                if (row.id == "filmography") {
                    Filmography(state, onSelect)
                } else if (row.cards.isNotEmpty()) {
                    TvCardRow(row, onFocusCard = { onFocusImage(it.wideImage) })
                }
            }
            if (state.decades.isEmpty() && !state.reloading) {
                item(key = "none") {
                    TvEmptyView(title = stringResource(R.string.tv_discover_person_no_movies), icon = Icons.Rounded.Movie)
                }
            }
            if (state.oftenWith.size >= OFTEN_MIN) {
                item(key = "often") {
                    TvShelf(stringResource(R.string.tv_discover_person_often)) {
                        items(state.oftenWith, key = { it.id }) { person ->
                            TvPersonCard(person.name, person.photo, role = person.note, onClick = { navigator.openPerson(person.id) })
                        }
                    }
                }
            }
            if (state.attribution.isNotEmpty()) {
                item(key = "attribution") {
                    Text(state.attribution, style = TvType.caption, color = TvColors.textFaint, modifier = Modifier.tvPageInset())
                }
            }
        }
    }
    if (about) TvPersonAbout(state) { about = false }
}

@Composable
private fun personRows(state: PersonState): List<CardRow> {
    val rows = mutableListOf<CardRow>()
    val groups =
        listOf(
            Triple("known", R.string.tv_discover_person_known_for, state.knownFor),
            Triple("coming", R.string.tv_discover_person_coming_soon, state.comingSoon),
            Triple("library", R.string.tv_discover_search_in_library, state.inLibrary),
            Triple("asked", R.string.tv_discover_person_asked, state.asked),
        )
    groups.filter { it.third.isNotEmpty() }.forEach { (id, title, cards) -> rows += CardRow(id, id, stringResource(title), cards = cards) }
    rows += CardRow("filmography", "filmography", "")
    state.decades.forEach { decade ->
        val title =
            if (decade.year == 0) {
                stringResource(R.string.tv_discover_person_coming_soon)
            } else {
                stringResource(R.string.tv_discover_person_decade, decade.year)
            }
        rows += CardRow("decade-${decade.year}", "decade", title, cards = decade.cards)
    }
    return rows
}

@Composable
private fun PersonHeader(
    state: PersonState,
    onAbout: () -> Unit,
) {
    Row(Modifier.tvPageInset(), horizontalArrangement = Arrangement.spacedBy(TvMetrics.marginX)) {
        ServerImage(
            source = state.photo,
            contentDescription = null,
            size = ImageSize.POSTER,
            placeholderName = state.name,
            modifier = Modifier.size(TvMetrics.personPhoto).clip(CircleShape),
        )
        Column(Modifier.widthIn(max = TvMetrics.readableWidth), verticalArrangement = Arrangement.spacedBy(TvMetrics.gutter)) {
            Text(state.name, style = TvType.title3, color = TvColors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val since = if (state.firstYear > 0) stringResource(R.string.tv_discover_person_since, state.firstYear) else ""
            val roles = state.roles.map { tvRoleWord(it) } + since
            Text(roles.filter { it.isNotEmpty() }.joinToString(" · "), style = TvType.callout, color = TvColors.textMuted, maxLines = 1)
            if (state.about.biography.isNotEmpty()) {
                Text(state.about.biography, style = TvType.footnote, color = TvColors.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (state.about.biography.isNotEmpty() || state.about.birthday.isNotEmpty()) {
                TvActionButton(stringResource(R.string.tv_discover_person_about), onAbout)
            }
        }
    }
}

@Composable
private fun Filmography(
    state: PersonState,
    onSelect: (FilmRole, FilmSort) -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.tv_discover_person_filmography),
            style = TvType.headline,
            color = TvColors.text,
            modifier = Modifier.tvPageInset(),
        )
        TvChipRow {
            items(
                FilmRole.entries,
                key = { it.name },
            ) { role -> TvChip(tvRoleText(role), role == state.role, { onSelect(role, state.sort) }) }
        }
        TvChipRow {
            items(
                FilmSort.entries,
                key = { it.name },
            ) { sort -> TvChip(tvSortText(sort), sort == state.sort, { onSelect(state.role, sort) }) }
        }
    }
}

@Composable
private fun TvPersonAbout(
    state: PersonState,
    onClose: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val about = state.about
    val born = tvDate(about.birthday)
    val died = tvDate(about.deathday)
    val facts = mutableListOf<Pair<String, String>>()
    if (born != null) {
        val years = tvYears(born, died)
        val age =
            if (died == null) {
                stringResource(R.string.tv_discover_person_age, years)
            } else {
                stringResource(R.string.tv_discover_person_aged, years)
            }
        facts += stringResource(R.string.tv_discover_person_born) to "${tvLongDate(about.birthday).orEmpty()} · $age"
    }
    if (about.birthplace.isNotEmpty()) facts += stringResource(R.string.tv_discover_person_from) to about.birthplace
    if (died != null) facts += stringResource(R.string.tv_discover_person_died) to tvLongDate(about.deathday).orEmpty()
    if (about.department.isNotEmpty()) facts += stringResource(R.string.tv_discover_person_known_for) to about.department
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) { focus.requestFocusSafely() }
        Column(
            Modifier.fillMaxSize().background(TvColors.background).padding(horizontal = TvMetrics.marginX, vertical = TvMetrics.marginY),
            verticalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
        ) {
            Text(stringResource(R.string.tv_discover_person_about_name, state.name), style = TvType.title3, color = TvColors.text)
            TvActionButton(stringResource(R.string.tv_discover_done), onClose, Modifier.focusRequester(focus), prominent = true)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(TvMetrics.gutter)) {
                items(facts, key = { it.first }) { (label, value) ->
                    Column(Modifier.widthIn(max = TvMetrics.readableWidth).tvReadable()) {
                        Text(label, style = TvType.footnote, color = TvColors.textMuted)
                        Text(value, style = TvType.callout, color = TvColors.text)
                    }
                }
                if (about.biography.isNotEmpty()) {
                    item(
                        key = "story",
                    ) { Text(stringResource(R.string.tv_discover_person_story), style = TvType.headline, color = TvColors.text) }
                    items(tvBioChunks(about.biography).withIndex().toList(), key = { it.index }) { chunk ->
                        Text(
                            chunk.value,
                            style = TvType.callout,
                            color = TvColors.text,
                            modifier = Modifier.widthIn(max = TvMetrics.readableWidth).tvReadable(),
                        )
                    }
                }
            }
        }
    }
}

private const val OFTEN_MIN = 4

@Composable
private fun Modifier.tvReadable(): Modifier {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvMetrics.cornerRadius)
    return onFocusChanged { focused = it.isFocused }
        .focusable()
        .background(if (focused) TvColors.surfaceRaised else Color.Transparent, shape)
        .padding(TvMetrics.textGap)
}
