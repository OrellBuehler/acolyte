package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.BrowseGridState
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.PeopleBrowseState
import ch.orellbuehler.acolyte.feature.discover.PeopleFilter
import ch.orellbuehler.acolyte.feature.discover.PersonCard
import ch.orellbuehler.acolyte.feature.discover.SearchResults
import ch.orellbuehler.acolyte.feature.discover.SearchScope
import ch.orellbuehler.acolyte.feature.discover.SearchState
import ch.orellbuehler.acolyte.feature.discover.SuggestKind
import ch.orellbuehler.acolyte.feature.discover.SuggestState
import ch.orellbuehler.acolyte.feature.discover.Suggestion
import ch.orellbuehler.acolyte.feature.discover.Understood
import ch.orellbuehler.acolyte.feature.discover.browseLink
import ch.orellbuehler.acolyte.feature.discover.tileHue
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.BrowseTileCard
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.PersonCircle
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val search = sdk.search()
    private val browse = sdk.browse()
    private val people = sdk.peopleBrowse()

    val state: StateFlow<SearchState> = search.state
    val browseState: StateFlow<BrowseGridState> = browse.state
    val peopleState: StateFlow<PeopleBrowseState> = people.state

    init {
        search.load()
        loadBrowse()
    }

    fun type(text: String) = search.type(text)

    fun selectScope(scope: SearchScope) {
        search.selectScope(scope)
        loadBrowse()
    }

    fun setOnlyLibrary(on: Boolean) = search.setOnlyLibrary(on)

    fun clearRecents() = search.clearRecents()

    suspend fun suggest(text: String) = search.suggest(text)

    suspend fun search(text: String) = search.search(text)

    fun submit(text: String) {
        search.type(text)
        search.remember(text)
        viewModelScope.launch { search.search(text) }
    }

    fun loadSection(id: String) {
        viewModelScope.launch { browse.loadSection(id) }
    }

    fun refreshBrowse() {
        viewModelScope.launch {
            if (search.state.value.scope == SearchScope.PEOPLE) people.refresh() else browse.refresh()
        }
    }

    fun selectPeople(filter: PeopleFilter) {
        viewModelScope.launch { people.select(filter) }
    }

    fun morePeople() {
        viewModelScope.launch { people.loadMore() }
    }

    suspend fun refreshOwned() = search.refreshOwned()

    suspend fun watch() = browse.watch()

    private fun loadBrowse() {
        viewModelScope.launch {
            when (search.state.value.scope) {
                SearchScope.PEOPLE -> people.load()
                SearchScope.MOVIE -> browse.load(ExploreType.MOVIES)
                SearchScope.SHOW -> browse.load(ExploreType.SHOWS)
                SearchScope.ALL -> browse.load(ExploreType.ALL)
            }
        }
    }
}

@Composable
fun SearchScreen(
    nav: DiscoverNav,
    onBack: (() -> Unit)?,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SearchViewModel(sdk) }
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
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    ScreenScaffold(title = stringResource(R.string.section_search), onBack = onBack) { padding ->
        CardHost(nav, state.canHide, { viewModel.submit(state.query) }, Modifier.padding(padding)) { events ->
            val list = rememberLazyListState()
            LoadMoreEffect(list, state.query.isBlank() && state.scope == SearchScope.PEOPLE && people.more && !people.loadingMore) {
                viewModel.morePeople()
            }
            LazyColumn(state = list, contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                item(key = "field") {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::type,
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.discover_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.type("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.discover_search_clear))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions =
                            KeyboardActions(onSearch = {
                                viewModel.submit(state.query)
                                focus.clearFocus()
                            }),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.Screen, vertical = Spacing.Small)
                                .onFocusChanged { focused = it.isFocused },
                    )
                }
                if (state.scopes.size > 2) {
                    item(key = "scopes") { ScopeBar(state.scopes, state.scope, viewModel::selectScope) }
                }
                if (state.query.isNotBlank()) {
                    item(key = "only") {
                        Row(
                            Modifier.padding(horizontal = Spacing.Screen),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.discover_search_from), style = MaterialTheme.typography.bodySmall)
                            FilterChip(
                                selected = !state.onlyLibrary,
                                onClick = { viewModel.setOnlyLibrary(false) },
                                label = { Text(stringResource(R.string.discover_search_everywhere)) },
                            )
                            FilterChip(
                                selected = state.onlyLibrary,
                                onClick = { viewModel.setOnlyLibrary(true) },
                                label = { Text(stringResource(R.string.discover_search_in_library)) },
                            )
                        }
                    }
                    if (focused && (state.suggestions.isNotEmpty() || state.completions.isNotEmpty())) {
                        item(key = "suggest") {
                            SuggestionList(state.completions, state.suggestions, viewModel::submit) { suggestion ->
                                focus.clearFocus()
                                when (suggestion.kind) {
                                    SuggestKind.PERSON -> nav.openPerson(suggestion.personId)
                                    SuggestKind.MOVIE, SuggestKind.SHOW ->
                                        if (suggestion.tmdb.isNotEmpty()) {
                                            nav.openTitle(if (suggestion.kind == SuggestKind.SHOW) "show" else "movie", suggestion.tmdb)
                                        } else {
                                            viewModel.submit(suggestion.name)
                                        }
                                }
                            }
                        }
                    }
                    results(state, events, nav, viewModel)
                } else {
                    if (state.recents.isNotEmpty()) {
                        item(
                            key = "recents",
                        ) { Recents(state.recents, viewModel::submit, viewModel::clearRecents) }
                    }
                    if (state.scope == SearchScope.PEOPLE) {
                        peopleBrowse(people, nav, viewModel)
                    } else {
                        browseGrid(browse, nav, viewModel)
                    }
                }
            }
        }
    }
}

private const val SUGGEST_DELAY = 150L
private const val SEARCH_DELAY = 250L

@Composable
private fun scopeText(scope: SearchScope): String =
    stringResource(
        when (scope) {
            SearchScope.ALL -> R.string.discover_type_all
            SearchScope.PEOPLE -> R.string.discover_scope_people
            SearchScope.MOVIE -> R.string.discover_type_movies
            SearchScope.SHOW -> R.string.discover_type_shows
        },
    )

@Composable
private fun ScopeBar(
    scopes: List<SearchScope>,
    selected: SearchScope,
    onSelect: (SearchScope) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
        scopes.forEachIndexed { index, scope ->
            SegmentedButton(
                selected = scope == selected,
                onClick = { onSelect(scope) },
                shape = SegmentedButtonDefaults.itemShape(index, scopes.size),
                label = { Text(scopeText(scope), maxLines = 1) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionList(
    completions: List<String>,
    suggestions: List<Suggestion>,
    onComplete: (String) -> Unit,
    onPick: (Suggestion) -> Unit,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
        if (completions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                completions.forEach { AssistChip(onClick = { onComplete(it) }, label = { Text(it) }) }
            }
        }
        suggestions.forEach { suggestion ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPick(suggestion) }
                    .padding(vertical = Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                ServerImage(
                    source = suggestion.art,
                    contentDescription = null,
                    size = ImageSize.THUMB,
                    placeholderName = suggestion.name,
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(if (suggestion.kind == SuggestKind.PERSON) CircleShape else RoundedCornerShape(8.dp)),
                )
                Column(Modifier.weight(1f)) {
                    Text(suggestion.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (suggestion.sub.isNotEmpty()) {
                        Text(
                            suggestion.sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                when (suggestion.state) {
                    SuggestState.HAVE -> StatusChip(stringResource(R.string.discover_suggest_have), Tone.OK)
                    SuggestState.MISSING -> StatusChip(stringResource(R.string.discover_suggest_missing), Tone.WAIT)
                    SuggestState.ASKED -> StatusChip(stringResource(R.string.discover_suggest_asked), Tone.IDLE)
                    SuggestState.NONE -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Recents(
    recents: List<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.padding(vertical = Spacing.Small)) {
        SectionHeader(
            stringResource(R.string.discover_recent),
            actionLabel = stringResource(R.string.discover_recent_clear),
            onAction = onClear,
        )
        FlowRow(Modifier.padding(horizontal = Spacing.Screen), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            recents.forEach { text ->
                AssistChip(
                    onClick = { onPick(text) },
                    label = { Text(text) },
                    leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}

private fun LazyListScope.results(
    state: SearchState,
    events: CardEvents,
    nav: DiscoverNav,
    viewModel: SearchViewModel,
) {
    val results = state.shown
    if (results == null || (state.searching && results.searched != state.query.trim())) {
        item(key = "searching") {
            Box(Modifier.fillMaxWidth().padding(Spacing.XLarge), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }
    if (results.metadataDown) {
        item(key = "down") { NoticeBanner(stringResource(R.string.discover_metadata_down)) }
    }
    results.error?.let { item(key = "error") { ErrorBanner(it) } }
    val single = state.scope != SearchScope.ALL
    item(key = "count") {
        val count = results.library.size + results.movies.size + results.shows.size + results.people.size + results.more.size
        Text(
            pluralStringResource(R.plurals.discover_results, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        )
    }
    results.understood?.let { understood ->
        item(key = "plain") { PlainBlock(understood, results, events) }
    }
    if (results.library.isNotEmpty()) {
        item(key = "library") {
            CardShelf(title = stringResource(R.string.discover_search_in_library)) {
                items(results.library, key = { it.key }) { card -> TitleWideCard(card, events, typed = !single) }
            }
        }
    }
    if (results.top.isNotEmpty() && !single) {
        item(key = "top") {
            CardShelf(title = stringResource(R.string.discover_top_matches)) {
                items(results.top, key = { it.card?.key ?: ("p" + it.person?.id) }) { hit ->
                    hit.card?.let { TitleWideCard(it, events, typed = true) }
                    hit.person?.let { PersonCircle(it, onClick = { nav.openPerson(it.id) }) }
                }
            }
        }
    }
    if (single) {
        val cards =
            when (state.scope) {
                SearchScope.MOVIE -> results.movies
                SearchScope.SHOW -> results.shows
                else -> emptyList()
            }
        if (cards.isNotEmpty()) item(key = "grid") { CardGrid(cards, events) { viewModel.submit(it) } }
        if (state.scope == SearchScope.PEOPLE && results.people.isNotEmpty()) {
            item(key = "people") { PeopleGrid(results.people, nav) }
        }
    } else {
        if (results.people.isNotEmpty()) {
            item(key = "people") {
                CardShelf(
                    title = stringResource(R.string.discover_scope_people),
                    onSeeAll = { viewModel.selectScope(SearchScope.PEOPLE) },
                ) {
                    items(results.people, key = { it.id }) { person -> PersonCircle(person, onClick = { nav.openPerson(person.id) }) }
                }
            }
        }
        if (results.movies.isNotEmpty()) {
            item(key = "movies") {
                CardShelf(title = stringResource(R.string.discover_type_movies), onSeeAll = { viewModel.selectScope(SearchScope.MOVIE) }) {
                    items(results.movies, key = { it.key }) { card -> TitleWideCard(card, events) }
                }
            }
        }
        if (results.shows.isNotEmpty()) {
            item(key = "shows") {
                CardShelf(title = stringResource(R.string.discover_type_shows), onSeeAll = { viewModel.selectScope(SearchScope.SHOW) }) {
                    items(results.shows, key = { it.key }) { card -> TitleWideCard(card, events) }
                }
            }
        }
    }
    if (results.isEmpty) {
        item(key = "nothing") { NothingFound(results, state, nav, viewModel) }
    }
}

@Composable
private fun CardGrid(
    cards: List<DiscoverCard>,
    events: CardEvents,
    onSearch: (String) -> Unit,
) {
    val wrapped =
        CardEvents(
            open = { card -> if (card.opensTitle) events.open(card) else onSearch(card.name) },
            longPress = events.longPress,
            act = events.act,
        )
    CardGrid(cards, wrapped)
}

@Composable
private fun PlainBlock(
    understood: Understood,
    results: SearchResults,
    events: CardEvents,
) {
    Column(Modifier.padding(vertical = Spacing.Small)) {
        SectionHeader(stringResource(R.string.discover_plain_title))
        Text(
            stringResource(R.string.discover_plain_understood, understoodText(understood)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.Screen),
        )
        if (results.plainLibrary.isNotEmpty()) {
            CardShelf(title = stringResource(R.string.discover_search_in_library)) {
                items(results.plainLibrary, key = { it.key }) { card -> TitleWideCard(card, events) }
            }
        }
        if (results.more.isNotEmpty()) {
            CardShelf(title = stringResource(R.string.discover_plain_more)) {
                items(results.more, key = { it.key }) { card -> TitleWideCard(card, events, typed = true) }
            }
        }
    }
}

@Composable
private fun understoodText(understood: Understood): String {
    val kind =
        when {
            understood.music -> stringResource(R.string.discover_kind_music)
            understood.kind == CardKind.MOVIE -> stringResource(R.string.discover_type_movies)
            understood.kind == CardKind.SHOW -> stringResource(R.string.discover_type_shows)
            else -> stringResource(R.string.discover_plain_titles)
        }
    val or = stringResource(R.string.discover_plain_or)
    val head = (listOf(understood.genres.map { genreText(it) }.joinToString(or)) + kind).filter { it.isNotEmpty() }.joinToString(" ")
    val years =
        when {
            understood.yearFrom > 0 && understood.yearFrom == understood.yearTo -> understood.yearFrom.toString()
            understood.yearFrom > 0 && understood.yearTo > 0 -> "${understood.yearFrom}–${understood.yearTo}"
            understood.yearFrom > 0 -> stringResource(R.string.discover_plain_from, understood.yearFrom)
            understood.yearTo > 0 -> stringResource(R.string.discover_plain_to, understood.yearTo)
            else -> ""
        }
    val parts =
        listOf(
            if (understood.name.isNotEmpty()) "“${understood.name}”" else "",
            head,
            years,
            if (understood.people.isNotEmpty()) stringResource(R.string.discover_plain_with, understood.people.joinToString(", ")) else "",
            if (understood.words.isNotEmpty()) stringResource(R.string.discover_plain_about, understood.words.joinToString(", ")) else "",
        )
    return parts.filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
private fun NothingFound(
    results: SearchResults,
    state: SearchState,
    nav: DiscoverNav,
    viewModel: SearchViewModel,
) {
    Column(
        Modifier.fillMaxWidth().padding(Spacing.XLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Icon(
            Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(stringResource(R.string.discover_nothing_found, results.searched), style = MaterialTheme.typography.titleMedium)
        listOf(
            R.string.discover_tip_spelling,
            R.string.discover_tip_year,
            R.string.discover_tip_id,
            R.string.discover_tip_describe,
        ).forEach {
            Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.scope != SearchScope.ALL) {
            OutlinedButton(onClick = { viewModel.selectScope(SearchScope.ALL) }) { Text(stringResource(R.string.discover_look_everywhere)) }
        }
        if (state.canManage) {
            TextButton(onClick = { nav.openSection(Section.SETTINGS) }) { Text(stringResource(R.string.discover_check_sources)) }
        }
    }
}

@Composable
private fun PeopleGrid(
    people: List<PersonCard>,
    nav: DiscoverNav,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        people.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { person -> PersonCircle(person, onClick = { nav.openPerson(person.id) }) }
                repeat(3 - row.size) { Box(Modifier.size(104.dp)) }
            }
        }
    }
}

private fun LazyListScope.browseGrid(
    browse: BrowseGridState,
    nav: DiscoverNav,
    viewModel: SearchViewModel,
) {
    browse.status.error?.let { error -> item(key = "browse-error") { ErrorBanner(error) } }
    if (browse.tiles.isNotEmpty()) {
        item(key = "genres") {
            SectionHeader(stringResource(R.string.discover_browse_genres))
            TileGrid(browse.tiles, browse.tab, nav)
        }
    }
    items(browse.sections, key = { "section-" + it.id }) { section ->
        LaunchedEffect(section.id) { viewModel.loadSection(section.id) }
        Column {
            SectionHeader(browseSectionTitle(section.id, section.title))
            if (section.tiles.isEmpty() && (!section.loaded || section.building)) {
                Box(Modifier.fillMaxWidth().padding(Spacing.Large), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                TileGrid(section.tiles, browse.tab, nav)
            }
        }
    }
}

@Composable
private fun TileGrid(
    tiles: List<BrowseTile>,
    tab: ExploreType,
    nav: DiscoverNav,
) {
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                pair.forEach { tile ->
                    BrowseTileCard(
                        name = if (tile.category.isEmpty() && tile.mood.isEmpty()) genreText(tile.name) else tile.name,
                        image = tile.backdrop,
                        hue = tileHue(tile.name),
                        modifier = Modifier.weight(1f).aspectRatio(3f / 4f),
                        onClick = { nav.openBrowseOrGenre(browseLink(tile, tab)) },
                    )
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

private fun LazyListScope.peopleBrowse(
    people: PeopleBrowseState,
    nav: DiscoverNav,
    viewModel: SearchViewModel,
) {
    people.status.error?.let { error -> item(key = "people-error") { ErrorBanner(error) } }
    if (people.filters.size > 1) {
        item(key = "filters") {
            Row(Modifier.padding(horizontal = Spacing.Screen), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                people.filters.forEach { filter ->
                    FilterChip(
                        selected = filter == people.filter,
                        onClick = { viewModel.selectPeople(filter) },
                        label = { Text(peopleFilterText(filter)) },
                    )
                }
            }
        }
    }
    items(people.shelves, key = { "shelf-" + it.id }) { shelf ->
        CardShelf(
            title =
                if (shelf.id ==
                    "library"
                ) {
                    stringResource(R.string.discover_people_library)
                } else {
                    stringResource(R.string.discover_people_hot)
                },
        ) {
            items(shelf.people, key = { it.id }) { person -> PersonCircle(person, onClick = { nav.openPerson(person.id) }) }
        }
    }
    if (people.popular.isNotEmpty()) {
        item(key = "popular") {
            SectionHeader(stringResource(R.string.discover_people_popular))
            PeopleGrid(people.popular, nav)
        }
    } else if (people.status.phase == Phase.READY && people.shelves.isEmpty()) {
        item(key = "people-empty") {
            MessageView(icon = Icons.Rounded.Search, title = stringResource(R.string.discover_people_empty))
        }
    }
    if (people.loadingMore) {
        item(key = "people-more") {
            Box(Modifier.fillMaxWidth().padding(Spacing.Large), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
    if (people.attribution.isNotEmpty()) {
        item(key = "attribution") {
            Text(
                people.attribution,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.Screen),
            )
        }
    }
}

@Composable
private fun peopleFilterText(filter: PeopleFilter): String =
    stringResource(
        when (filter) {
            PeopleFilter.POPULAR -> R.string.discover_people_popular
            PeopleFilter.ACTORS -> R.string.discover_people_actors
            PeopleFilter.DIRECTORS -> R.string.discover_people_directors
            PeopleFilter.COMPOSERS -> R.string.discover_people_composers
        },
    )
