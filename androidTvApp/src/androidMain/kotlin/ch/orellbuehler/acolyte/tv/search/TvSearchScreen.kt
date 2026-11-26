package ch.orellbuehler.acolyte.tv.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.discover.BrowseGridState
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.PeopleBrowseState
import ch.orellbuehler.acolyte.feature.discover.PeopleFilter
import ch.orellbuehler.acolyte.feature.discover.PersonCard
import ch.orellbuehler.acolyte.feature.discover.SearchResults
import ch.orellbuehler.acolyte.feature.discover.SearchScope
import ch.orellbuehler.acolyte.feature.discover.SearchState
import ch.orellbuehler.acolyte.feature.discover.SuggestKind
import ch.orellbuehler.acolyte.feature.discover.Suggestion
import ch.orellbuehler.acolyte.feature.discover.browseLink
import ch.orellbuehler.acolyte.feature.discover.tileHue
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvAmbientPage
import ch.orellbuehler.acolyte.tv.components.TvBrowseTile
import ch.orellbuehler.acolyte.tv.components.TvCardRow
import ch.orellbuehler.acolyte.tv.components.TvChip
import ch.orellbuehler.acolyte.tv.components.TvChipRow
import ch.orellbuehler.acolyte.tv.components.TvDiscoverCard
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvInlineLoading
import ch.orellbuehler.acolyte.tv.components.TvNoticeBanner
import ch.orellbuehler.acolyte.tv.components.TvPersonCard
import ch.orellbuehler.acolyte.tv.components.TvShelf
import ch.orellbuehler.acolyte.tv.components.TvTextField
import ch.orellbuehler.acolyte.tv.components.tvBrowseSectionTitle
import ch.orellbuehler.acolyte.tv.components.tvGenreText
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.components.tvPosterGrid
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.errorText
import kotlinx.coroutines.delay

private const val SUGGEST_DELAY = 150L
private const val SEARCH_DELAY = 250L
private const val TILE_COLUMNS = 4
private const val PEOPLE_COLUMNS = 7

class TvSearchActions(
    val type: (String) -> Unit,
    val submit: (String) -> Unit,
    val selectScope: (SearchScope) -> Unit,
    val setOnlyLibrary: (Boolean) -> Unit,
    val clearRecents: () -> Unit,
    val loadSection: (String) -> Unit,
    val selectPeople: (PeopleFilter) -> Unit,
    val morePeople: () -> Unit,
)

@Composable
fun TvSearchScreen(account: Account) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { TvSearchViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val browse by viewModel.browseState.collectAsStateWithLifecycle()
    val people by viewModel.peopleState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshOwned()
            viewModel.watch()
        }
    }
    LaunchedEffect(state.query) {
        delay(SUGGEST_DELAY)
        viewModel.suggest(state.query)
    }
    LaunchedEffect(state.query, state.scope) {
        delay(SEARCH_DELAY)
        viewModel.search(state.query)
    }
    val actions =
        remember(viewModel) {
            TvSearchActions(
                type = viewModel::type,
                submit = viewModel::submit,
                selectScope = viewModel::selectScope,
                setOnlyLibrary = viewModel::setOnlyLibrary,
                clearRecents = viewModel::clearRecents,
                loadSection = viewModel::loadSection,
                selectPeople = viewModel::selectPeople,
                morePeople = viewModel::morePeople,
            )
        }
    TvSearchContent(state, browse, people, actions)
}

@Composable
fun TvSearchContent(
    state: SearchState,
    browse: BrowseGridState,
    people: PeopleBrowseState,
    actions: TvSearchActions,
) {
    val navigator = LocalTvNavigator.current
    var text by remember { mutableStateOf(state.query) }
    LaunchedEffect(state.query) { if (state.query != text) text = state.query }
    val query = state.query.trim()
    TvAmbientPage { onFocusImage ->
        item(key = "field") {
            Column(Modifier.tvPageInset()) {
                TvTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        actions.type(it)
                    },
                    hint = stringResource(R.string.tv_discover_search_placeholder),
                    imeAction = ImeAction.Search,
                    onAction = { actions.submit(text) },
                )
            }
        }
        scopeChips(state, query, actions)
        if (query.isNotEmpty()) {
            suggestionChips(
                state,
                actions,
            ) { suggestion -> openSuggestion(navigator::openPerson, navigator::openTitle, actions, suggestion) }
        }
        if (query.isEmpty()) {
            idle(state, browse, people, actions) { navigator.openPerson(it) }
        } else {
            results(state, query, actions, { navigator.openPerson(it) }, onFocusImage)
        }
    }
}

private fun openSuggestion(
    openPerson: (String) -> Unit,
    openTitle: (String, String, Long) -> Unit,
    actions: TvSearchActions,
    suggestion: Suggestion,
) {
    when {
        suggestion.kind == SuggestKind.PERSON -> openPerson(suggestion.personId)
        suggestion.tmdb.isEmpty() -> actions.submit(suggestion.name)
        else -> openTitle(if (suggestion.kind == SuggestKind.SHOW) "show" else "movie", suggestion.tmdb, 0)
    }
}

private fun LazyListScope.scopeChips(
    state: SearchState,
    query: String,
    actions: TvSearchActions,
) {
    val shown = state.scopes.filter { it != SearchScope.MUSIC }
    if (shown.size > 1) {
        item(key = "scopes") {
            TvChipRow {
                items(
                    shown,
                    key = { it.name },
                ) { scope -> TvChip(tvScopeText(scope), scope == state.scope, { actions.selectScope(scope) }) }
            }
        }
    }
    if (query.isNotEmpty()) {
        item(key = "where") {
            TvChipRow {
                item(key = "everywhere") {
                    TvChip(stringResource(R.string.tv_discover_search_everywhere), !state.onlyLibrary, { actions.setOnlyLibrary(false) })
                }
                item(key = "library") {
                    TvChip(stringResource(R.string.tv_discover_search_in_library), state.onlyLibrary, { actions.setOnlyLibrary(true) })
                }
            }
        }
    }
}

private fun LazyListScope.suggestionChips(
    state: SearchState,
    actions: TvSearchActions,
    open: (Suggestion) -> Unit,
) {
    if (state.completions.isEmpty() && state.suggestions.isEmpty()) return
    item(key = "suggestions") {
        TvChipRow {
            items(state.completions, key = { "c-$it" }) { completion -> TvChip(completion, false, { actions.submit(completion) }) }
            items(state.suggestions, key = { "s-${it.kind}-${it.tmdb}-${it.personId}-${it.name}" }) { suggestion ->
                TvChip(listOf(suggestion.name, suggestion.sub).filter { it.isNotEmpty() }.joinToString(" · "), false, { open(suggestion) })
            }
        }
    }
}

private fun LazyListScope.idle(
    state: SearchState,
    browse: BrowseGridState,
    people: PeopleBrowseState,
    actions: TvSearchActions,
    openPerson: (String) -> Unit,
) {
    if (state.recents.isNotEmpty()) {
        item(key = "recents") {
            Column {
                Text(
                    stringResource(R.string.tv_discover_recent),
                    style = TvType.headline,
                    color = TvColors.text,
                    modifier = Modifier.tvPageInset(),
                )
                TvChipRow {
                    items(state.recents, key = { "r-$it" }) { recent -> TvChip(recent, false, { actions.submit(recent) }) }
                    item(key = "clear") { TvChip(stringResource(R.string.tv_discover_recent_clear), false, actions.clearRecents) }
                }
            }
        }
    }
    if (state.scope == SearchScope.PEOPLE) peopleBrowse(people, actions, openPerson) else tileBrowse(browse, actions)
}

private fun LazyListScope.tileBrowse(
    browse: BrowseGridState,
    actions: TvSearchActions,
) {
    browse.status.error?.let { error -> item(key = "browse-error") { TvNoticeBanner(errorText(error), Modifier.tvPageInset()) } }
    if (browse.status.phase == Phase.LOADING && browse.tiles.isEmpty()) item(key = "browse-loading") { TvInlineLoading() }
    if (browse.tiles.isNotEmpty()) {
        tileGrid("tiles", { stringResource(R.string.tv_discover_browse_genres) }, browse.tiles, browse.tab, null)
    }
    browse.sections.forEach { section ->
        if (section.tiles.isEmpty()) {
            item(key = "section-${section.id}") {
                LaunchedEffect(section.id) { actions.loadSection(section.id) }
                if (!section.loaded || section.building) TvInlineLoading()
            }
        } else {
            tileGrid("section-${section.id}", { tvBrowseSectionTitle(section.id, section.title) }, section.tiles, browse.tab) {
                actions.loadSection(section.id)
            }
        }
    }
}

private fun LazyListScope.tileGrid(
    key: String,
    title: @Composable () -> String,
    tiles: List<BrowseTile>,
    tab: ExploreType,
    onShown: (() -> Unit)?,
) {
    item(key = "$key-title") {
        if (onShown != null) LaunchedEffect(Unit) { onShown() }
        Text(title(), style = TvType.headline, color = TvColors.text, modifier = Modifier.tvPageInset())
    }
    items(tiles.chunked(TILE_COLUMNS), key = {
        "$key-${it.first().kind}-${it.first().category}-${it.first().mood}-${it.first().name}"
    }) { line ->
        val navigator = LocalTvNavigator.current
        Row(
            Modifier.padding(horizontal = TvMetrics.marginX),
            horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
        ) {
            line.forEach { tile ->
                TvBrowseTile(
                    name = if (tile.category.isEmpty()) tvGenreText(tile.name) else tile.name,
                    image = tile.backdrop,
                    hue = tileHue(tile.name),
                    onClick = { navigator.openBrowse(browseLink(tile, tab)) },
                )
            }
        }
    }
}

private fun LazyListScope.peopleBrowse(
    people: PeopleBrowseState,
    actions: TvSearchActions,
    openPerson: (String) -> Unit,
) {
    people.status.error?.let { error -> item(key = "people-error") { TvNoticeBanner(errorText(error), Modifier.tvPageInset()) } }
    if (people.filters.size > 1) {
        item(key = "people-filters") {
            TvChipRow {
                items(people.filters, key = { it.name }) { filter ->
                    TvChip(tvPeopleFilterText(filter), filter == people.filter, { actions.selectPeople(filter) })
                }
            }
        }
    }
    items(people.shelves, key = { "shelf-${it.id}" }) { shelf ->
        val title =
            stringResource(if (shelf.id == "library") R.string.tv_discover_people_library else R.string.tv_discover_people_hot)
        PersonShelf(title, shelf.people, openPerson)
    }
    if (people.popular.isNotEmpty()) {
        item(key = "popular-title") {
            Text(
                stringResource(R.string.tv_discover_people_popular),
                style = TvType.headline,
                color = TvColors.text,
                modifier = Modifier.tvPageInset(),
            )
        }
        personGrid("popular", people.popular, openPerson)
        if (people.more) {
            item(key = "people-more") {
                LaunchedEffect(people.popular.size) { actions.morePeople() }
                TvInlineLoading()
            }
        }
    } else if (people.status.phase == Phase.READY && people.shelves.isEmpty()) {
        item(key = "people-empty") {
            TvEmptyView(title = stringResource(R.string.tv_discover_people_empty), icon = Icons.Rounded.Group)
        }
    }
    if (people.attribution.isNotEmpty()) {
        item(key = "people-attribution") {
            Text(people.attribution, style = TvType.caption, color = TvColors.textFaint, modifier = Modifier.tvPageInset())
        }
    }
}

@Composable
private fun PersonShelf(
    title: String,
    people: List<PersonCard>,
    openPerson: (String) -> Unit,
    seeAll: (() -> Unit)? = null,
) {
    TvShelf(title, seeAll = seeAll) {
        items(people, key = { it.id }) { person ->
            TvPersonCard(person.name, person.photo, role = person.note, onClick = { openPerson(person.id) })
        }
    }
}

private fun LazyListScope.personGrid(
    key: String,
    people: List<PersonCard>,
    openPerson: (String) -> Unit,
) {
    items(people.distinctBy { it.id }.chunked(PEOPLE_COLUMNS), key = { "$key-${it.first().id}" }) { line ->
        Row(
            Modifier.padding(horizontal = TvMetrics.marginX),
            horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
        ) {
            line.forEach { person ->
                TvPersonCard(person.name, person.photo, role = person.note, onClick = { openPerson(person.id) })
            }
        }
    }
}

private fun LazyListScope.results(
    state: SearchState,
    query: String,
    actions: TvSearchActions,
    openPerson: (String) -> Unit,
    onFocusImage: (String?) -> Unit,
) {
    val found = state.shown
    if (found == null || (state.searching && found.searched != query)) {
        item(key = "searching") { TvInlineLoading() }
        return
    }
    foundItems(state, found, actions, openPerson, onFocusImage)
}

private fun LazyListScope.foundItems(
    state: SearchState,
    found: SearchResults,
    actions: TvSearchActions,
    openPerson: (String) -> Unit,
    onFocusImage: (String?) -> Unit,
) {
    val single = state.scope != SearchScope.ALL
    val focus: (DiscoverCard) -> Unit = { onFocusImage(it.wideImage) }
    if (found.metadataDown) {
        item(
            key = "down",
        ) { TvNoticeBanner(stringResource(R.string.tv_discover_metadata_down), Modifier.tvPageInset()) }
    }
    found.error?.let { error -> item(key = "found-error") { TvNoticeBanner(errorText(error), Modifier.tvPageInset()) } }
    item(key = "count") {
        val count = found.library.size + found.movies.size + found.shows.size + found.people.size + found.more.size
        Text(
            pluralStringResource(R.plurals.tv_discover_results, count, count),
            style = TvType.footnote,
            color = TvColors.textMuted,
            modifier = Modifier.tvPageInset(),
        )
    }
    found.understood?.let { understood ->
        item(key = "understood") {
            Column(Modifier.tvPageInset()) {
                Text(stringResource(R.string.tv_discover_plain_title), style = TvType.headline, color = TvColors.text)
                Text(
                    stringResource(R.string.tv_discover_plain_understood, tvUnderstoodText(understood)),
                    style = TvType.callout,
                    color = TvColors.textMuted,
                )
            }
        }
    }
    val plain = found.understood != null && (found.plainLibrary.isNotEmpty() || found.more.isNotEmpty())
    if (plain || found.library.isNotEmpty()) {
        item(key = "rows") {
            val rows = tvSearchRows(found)
            Column(verticalArrangement = Arrangement.spacedBy(TvMetrics.sectionGap)) {
                if (found.understood != null) {
                    rows["plainLibrary"]?.let { TvCardRow(it, onFocusCard = focus) }
                    rows["more"]?.let { TvCardRow(it, typed = true, onFocusCard = focus) }
                }
                rows["library"]?.let { TvCardRow(it, typed = !single, onFocusCard = focus) }
            }
        }
    }
    if (!single && found.top.isNotEmpty()) {
        item(key = "top") {
            TvShelf(stringResource(R.string.tv_discover_top_matches)) {
                items(found.top.withIndex().toList(), key = { it.index }) { (_, hit) ->
                    val card = hit.card
                    val person = hit.person
                    if (card != null) {
                        TvDiscoverCard(card, wide = true, typed = true, onFocus = { focus(card) })
                    } else if (person != null) {
                        TvPersonCard(person.name, person.photo, role = person.note, onClick = { openPerson(person.id) })
                    }
                }
            }
        }
    }
    if (single) {
        when (state.scope) {
            SearchScope.MOVIE -> tvPosterGrid(found.movies, "movies", onFocusCard = focus)
            SearchScope.SHOW -> tvPosterGrid(found.shows, "shows", onFocusCard = focus)
            SearchScope.PEOPLE -> personGrid("found-people", found.people, openPerson)
            else -> Unit
        }
    } else {
        if (found.people.isNotEmpty()) {
            item(key = "found-people") {
                PersonShelf(
                    stringResource(R.string.tv_discover_scope_people),
                    found.people,
                    openPerson,
                ) { actions.selectScope(SearchScope.PEOPLE) }
            }
        }
        if (found.movies.isNotEmpty() || found.shows.isNotEmpty()) {
            item(key = "found-titles") {
                val rows = tvSearchRows(found)
                Column(verticalArrangement = Arrangement.spacedBy(TvMetrics.sectionGap)) {
                    rows["movies"]?.let { TvCardRow(it, seeAll = { actions.selectScope(SearchScope.MOVIE) }, onFocusCard = focus) }
                    rows["shows"]?.let { TvCardRow(it, seeAll = { actions.selectScope(SearchScope.SHOW) }, onFocusCard = focus) }
                }
            }
        }
    }
    if (found.isEmpty) {
        item(key = "nothing") {
            TvEmptyView(
                title = stringResource(R.string.tv_discover_nothing_found, found.searched),
                message =
                    listOf(
                        stringResource(R.string.tv_discover_tip_spelling),
                        stringResource(R.string.tv_discover_tip_year),
                        stringResource(R.string.tv_discover_tip_describe),
                    ).joinToString("\n"),
                icon = Icons.Rounded.Search,
                actions = {
                    if (state.scope != SearchScope.ALL) {
                        TvActionButton(stringResource(R.string.tv_discover_look_everywhere), { actions.selectScope(SearchScope.ALL) })
                    }
                },
            )
        }
    }
}
