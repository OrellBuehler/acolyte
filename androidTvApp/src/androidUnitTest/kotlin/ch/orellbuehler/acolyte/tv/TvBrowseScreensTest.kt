package ch.orellbuehler.acolyte.tv

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.navigation.compose.rememberNavController
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.api.model.GenreCard
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.discover.BrowseGridState
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.BrowseSectionState
import ch.orellbuehler.acolyte.feature.discover.BrowseTarget
import ch.orellbuehler.acolyte.feature.discover.BrowseViewState
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardMark
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.Decade
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.ExplorePill
import ch.orellbuehler.acolyte.feature.discover.ExploreState
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.FilmRole
import ch.orellbuehler.acolyte.feature.discover.FilmSort
import ch.orellbuehler.acolyte.feature.discover.GenreBanner
import ch.orellbuehler.acolyte.feature.discover.GenreState
import ch.orellbuehler.acolyte.feature.discover.Genres
import ch.orellbuehler.acolyte.feature.discover.MarkLabel
import ch.orellbuehler.acolyte.feature.discover.MarkState
import ch.orellbuehler.acolyte.feature.discover.MovieLimit
import ch.orellbuehler.acolyte.feature.discover.PeopleBrowseState
import ch.orellbuehler.acolyte.feature.discover.PeopleFilter
import ch.orellbuehler.acolyte.feature.discover.PeopleShelfState
import ch.orellbuehler.acolyte.feature.discover.PersonAbout
import ch.orellbuehler.acolyte.feature.discover.PersonCard
import ch.orellbuehler.acolyte.feature.discover.PersonState
import ch.orellbuehler.acolyte.feature.discover.PopularRow
import ch.orellbuehler.acolyte.feature.discover.PopularState
import ch.orellbuehler.acolyte.feature.discover.RailDayEntry
import ch.orellbuehler.acolyte.feature.discover.RailEntry
import ch.orellbuehler.acolyte.feature.discover.SearchHit
import ch.orellbuehler.acolyte.feature.discover.SearchResults
import ch.orellbuehler.acolyte.feature.discover.SearchScope
import ch.orellbuehler.acolyte.feature.discover.SearchState
import ch.orellbuehler.acolyte.feature.discover.SeriesMovie
import ch.orellbuehler.acolyte.feature.discover.SeriesState
import ch.orellbuehler.acolyte.feature.discover.ServiceChip
import ch.orellbuehler.acolyte.feature.discover.TopServices
import ch.orellbuehler.acolyte.feature.discover.TopSource
import ch.orellbuehler.acolyte.feature.discover.Understood
import ch.orellbuehler.acolyte.feature.discover.WhatKind
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import ch.orellbuehler.acolyte.tv.browse.TvCollectionContent
import ch.orellbuehler.acolyte.tv.browse.TvGenreContent
import ch.orellbuehler.acolyte.tv.browse.TvPersonContent
import ch.orellbuehler.acolyte.tv.components.TvBrowseListView
import ch.orellbuehler.acolyte.tv.explore.TvExploreActions
import ch.orellbuehler.acolyte.tv.explore.TvExploreContent
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.nav.LocalTvSignOut
import ch.orellbuehler.acolyte.tv.nav.TvNavigator
import ch.orellbuehler.acolyte.tv.nav.TvSections
import ch.orellbuehler.acolyte.tv.nav.TvTabSelection
import ch.orellbuehler.acolyte.tv.popular.TvPopularActions
import ch.orellbuehler.acolyte.tv.popular.TvPopularContent
import ch.orellbuehler.acolyte.tv.search.TvSearchActions
import ch.orellbuehler.acolyte.tv.search.TvSearchContent
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvTheme
import ch.orellbuehler.acolyte.ui.LocalSdk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class TvBrowseScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val sdk = AcolyteSdk(MemoryStore(), "test")

    private val account =
        Account(
            serverUrl = "http://holyarrs.local:6262",
            serverName = "holyarrs.local",
            method = SignInMethod.PASSWORD,
            userId = 1,
            userName = "anna",
            displayName = "Anna",
            role = Role.OWNER,
            permissions = emptyList(),
            parts = emptyList<PartView>(),
            serverVersion = null,
            avatarPath = null,
            mustSetUpTwoFactor = false,
            isKid = false,
            safeMode = false,
        )

    private val calls = mutableListOf<String>()

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            val nav = rememberNavController()
            val selection = remember { TvTabSelection(TvSections.tabs(account)) }
            val navigator = remember { TvNavigator(account, nav, selection) }
            TvTheme {
                CompositionLocalProvider(LocalSdk provides sdk, LocalTvNavigator provides navigator, LocalTvSignOut provides {}) {
                    Box(Modifier.fillMaxSize().background(TvColors.background)) { content() }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun shoot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File("build/tv-screens/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        if (compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()) return
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(matcher)
        compose.waitForIdle()
    }

    private fun press(text: String) {
        val control = hasText(text) and hasClickAction()
        scrollTo(control)
        compose.onNode(control).requestFocus()
        compose.waitForIdle()
        compose.onNode(control).assertIsFocused()
        compose.onNode(control).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private fun key(vararg moves: String) {
        moves.forEach { move ->
            val pressed =
                when (move) {
                    "up" -> Key.DirectionUp
                    "down" -> Key.DirectionDown
                    "left" -> Key.DirectionLeft
                    else -> Key.DirectionRight
                }
            compose.onNode(isFocused()).performKeyInput { pressKey(pressed) }
            compose.waitForIdle()
        }
    }

    private fun focusFirstCard() {
        compose.onAllNodes(hasClickAction()).onFirst().requestFocus()
        compose.waitForIdle()
    }

    private fun card(
        key: String,
        name: String,
        kind: CardKind = CardKind.MOVIE,
        mark: CardMark? = null,
    ) = DiscoverCard(
        key = key,
        kind = kind,
        tmdb = key,
        titleId = 0,
        name = name,
        year = 2024,
        overview = "About $name",
        genres = listOf("Drama", "Thriller", "Mystery"),
        mark = mark,
    )

    private fun cards(
        prefix: String,
        count: Int,
        kind: CardKind = CardKind.MOVIE,
    ) = (1..count).map { card("$prefix$it", "$prefix title $it", kind) }

    private fun row(
        id: String,
        title: String,
        count: Int = 8,
        wide: Boolean = true,
        type: String = id,
    ) = CardRow(id, type, title, cards = cards(id, count), wide = wide)

    private val exploreActions =
        TvExploreActions(
            refresh = { calls += "refresh" },
            select = { calls += "select-${it.wire}" },
            loadMore = {},
            selectService = { calls += "service-$it" },
            stripShown = {},
            stripOpened = {},
        )

    private fun exploreState(): ExploreState {
        val top = cards("top", 8)
        return ExploreState(
            status = LoadStatus(Phase.READY),
            hero = listOf(card("hero", "The Quiet Harbour", mark = CardMark(MarkState.AVAILABLE, MarkLabel.IN_LIBRARY))),
            heroPills = listOf(ExplorePill.PICKED_FOR_YOU),
            banner =
                GenreBanner(
                    Genres.movies.first { it.name == "Drama" },
                    both = false,
                    have = 12,
                    picks = cards("pick", 6),
                ),
            strip =
                listOf(
                    BrowseTile(kind = "movie", genre = 18, name = "Drama", section = "genres"),
                    BrowseTile(kind = "movie", mood = "Cozy nights", name = "Cozy nights", section = "moods"),
                    BrowseTile(kind = "movie", category = "heist", name = "Heists", section = "stories"),
                    BrowseTile(kind = "movie", genre = 878, name = "Science Fiction", section = "genres"),
                ),
            rows =
                listOf(
                    CardRow("top", "top_services", "", cards = top, ranked = true, seeAll = true),
                    row("because", "Because you have Salt and Iron", type = "because_have"),
                    CardRow("genre_browse", "genre_browse", ""),
                    row("trending", "Trending", wide = false),
                ),
            top =
                TopServices(
                    chips = listOf(ServiceChip(1, "Netflix"), ServiceChip(2, "Disney+"), ServiceChip(3, "Prime Video")),
                    selected = 1,
                    cards = top,
                    ranked = true,
                    source = TopSource.TODAY,
                    serviceName = "Netflix",
                    attribution = "Source: public charts",
                ),
            genres = Genres.movies.take(10).map { GenreCard(kind = "movie", id = it.id, name = it.name) },
        )
    }

    @Test
    fun exploreShowsBillboardRowsAndFollowsFocus() {
        show { TvExploreContent(exploreState(), popularVisible = true, actions = exploreActions) }
        compose.onNodeWithText("About The Quiet Harbour").assertIsDisplayed()
        compose.onNodeWithText("Picked for you").assertIsDisplayed()
        shoot("explore")
        compose.onNodeWithText("Movies").requestFocus()
        key("down", "right")
        compose.onNodeWithText("About pick title 2").assertIsDisplayed()
        key("down", "down", "down")
        shoot("explore-focused")
        compose.onNode(isFocused()).assertIsDisplayed()
    }

    @Test
    fun exploreChipsAreReachableAndActOnCenter() {
        show { TvExploreContent(exploreState(), popularVisible = true, actions = exploreActions) }
        press("Movies")
        assertEquals(listOf("select-movies"), calls)
        key("right")
        compose.onNodeWithText("TV shows").assertIsFocused()
        key("left")
        compose.onNodeWithText("Movies").assertIsFocused()
        key("down")
        compose.onNodeWithText("About pick title 1").assertIsDisplayed()
        key("right", "right")
        compose.onNodeWithText("About pick title 3").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de-w960dp-h540dp-xhdpi")
    fun exploreInGerman() {
        show { TvExploreContent(exploreState(), popularVisible = true, actions = exploreActions) }
        compose.onNodeWithText("Filme").assertIsDisplayed()
        shoot("explore-de")
    }

    @Test
    fun exploreBuildingShowsReload() {
        show {
            TvExploreContent(ExploreState(status = LoadStatus(Phase.READY)), popularVisible = false, actions = exploreActions)
        }
        compose.onNodeWithText("Reload").assertIsFocused()
        shoot("explore-building")
    }

    private val popularActions =
        TvPopularActions(refresh = {}, select = { calls += "select-${it.wire}" }, loadMore = {}, selectService = { row, id ->
            calls +=
                "$row-$id"
        })

    private fun popularState(): PopularState {
        val list = cards("rank", 10)
        return PopularState(
            status = LoadStatus(Phase.READY),
            hero = card("hero", "The Quiet Harbour"),
            heroPills = listOf(ExplorePill.RANK, ExplorePill.NEW_THIS_WEEK),
            heroRank = 2,
            heroService = "Netflix",
            days =
                listOf(
                    RailDayEntry(
                        "2026-11-26",
                        0,
                        cards("day", 5).map { RailEntry(it, WhatKind.ON_SERVICE, "Netflix", alsoComing = false) },
                    ),
                    RailDayEntry(
                        "2026-11-27",
                        1,
                        cards("next", 4).map { RailEntry(it, WhatKind.COMING, "", alsoComing = false) },
                    ),
                ),
            rows =
                listOf(
                    PopularRow(
                        CardRow("np_top_shows", "np_top_shows", "Top shows", cards = list, ranked = true),
                        chips = listOf(ServiceChip(1, "Netflix"), ServiceChip(2, "Disney+")),
                        selected = 0,
                    ),
                    PopularRow(CardRow("np_services", "np_services", "New on services", cards = cards("new", 8))),
                ),
        )
    }

    @Test
    fun popularShowsWeekAndRankedRows() {
        show { TvPopularContent(popularState(), popularActions) }
        compose.onNodeWithText("This week").assertIsDisplayed()
        compose.onNodeWithText("#2 on Netflix").assertIsDisplayed()
        shoot("popular")
        press("Disney+")
        assertEquals(listOf("np_top_shows-2"), calls)
    }

    @Test
    @Config(qualifiers = "de-w960dp-h540dp-xhdpi")
    fun popularInGerman() {
        show { TvPopularContent(popularState(), popularActions) }
        compose.onNodeWithText("Diese Woche").assertIsDisplayed()
        shoot("popular-de")
    }

    private val searchActions =
        TvSearchActions(
            type = { calls += "type-$it" },
            submit = { calls += "submit-$it" },
            selectScope = { calls += "scope-${it.wire}" },
            setOnlyLibrary = { calls += "library-$it" },
            clearRecents = { calls += "clear" },
            loadSection = {},
            selectPeople = { calls += "people-${it.name}" },
            morePeople = {},
        )

    private val scopes = listOf(SearchScope.ALL, SearchScope.PEOPLE, SearchScope.MOVIE, SearchScope.SHOW)

    private fun genreTiles() = Genres.movies.take(8).map { BrowseTile(kind = "movie", genre = it.id, name = it.name, section = "genres") }

    private fun browseState() =
        BrowseGridState(
            status = LoadStatus(Phase.READY),
            tiles = genreTiles(),
            sections =
                listOf(
                    BrowseSectionState(
                        "moods",
                        "Moods",
                        tiles = listOf(BrowseTile(kind = "movie", mood = "Cozy nights", name = "Cozy nights")),
                        loaded = true,
                    ),
                ),
        )

    @Test
    fun searchStartsWithRecentsAndGenreTiles() {
        val state = SearchState(scopes = scopes, recents = listOf("dune", "salt and iron"))
        show { TvSearchContent(state, browseState(), PeopleBrowseState(), searchActions) }
        compose.onNodeWithText("Recent searches").assertIsDisplayed()
        compose.onNodeWithText("Genres").assertIsDisplayed()
        shoot("search")
        press("dune")
        press("Clear")
        press("People")
        assertEquals(listOf("submit-dune", "clear", "scope-people"), calls)
    }

    @Test
    fun searchTypingReportsText() {
        show { TvSearchContent(SearchState(scopes = scopes), browseState(), PeopleBrowseState(), searchActions) }
        compose.onNode(hasSetTextAction()).performTextInput("dune")
        compose.waitForIdle()
        assertEquals(listOf("type-dune"), calls)
    }

    @Test
    fun searchResultsGroupByKind() {
        val found =
            SearchResults(
                searched = "harbour",
                library = cards("lib", 5),
                top = listOf(SearchHit(card = card("t1", "Harbour Lights")), SearchHit(person = PersonCard("p1", "Mara Venn", "Actor"))),
                movies = cards("mov", 7),
                shows = cards("show", 6, CardKind.SHOW),
                people = listOf(PersonCard("p1", "Mara Venn", "Actor"), PersonCard("p2", "Joel Haas", "Director")),
            )
        val state =
            SearchState(query = "harbour", scopes = scopes, completions = listOf("harbour lights"), results = found)
        show { TvSearchContent(state, browseState(), PeopleBrowseState(), searchActions) }
        compose.onNodeWithText("harbour lights").assertIsDisplayed()
        shoot("search-results")
        press("In your Library")
        assertEquals(listOf("library-true"), calls)
        scrollTo(hasText("Top matches"))
        compose.onNodeWithText("Top matches").assertIsDisplayed()
        shoot("search-results-top")
    }

    @Test
    fun searchUnderstandsDescriptions() {
        val found =
            SearchResults(
                searched = "scary movie from 2014",
                understood = Understood("", listOf("Horror"), CardKind.MOVIE, false, 2014, 2014, emptyList(), emptyList()),
                plainLibrary = cards("plain", 4),
                more = cards("more", 6),
            )
        val state = SearchState(query = "scary movie from 2014", scopes = scopes, results = found)
        show { TvSearchContent(state, browseState(), PeopleBrowseState(), searchActions) }
        compose.onNodeWithText("Matches for your description").assertIsDisplayed()
        shoot("search-plain")
    }

    @Test
    fun searchFindingNothingOffersEverywhere() {
        val state =
            SearchState(
                query = "zzzz",
                scope = SearchScope.MOVIE,
                scopes = scopes,
                results = SearchResults(searched = "zzzz"),
            )
        show { TvSearchContent(state, browseState(), PeopleBrowseState(), searchActions) }
        compose.onNodeWithText("Nothing found for “zzzz”").assertIsDisplayed()
        press("Look everywhere")
        assertEquals(listOf("scope-all"), calls)
        shoot("search-nothing")
    }

    @Test
    fun searchSingleScopeShowsPosterGrid() {
        val found = SearchResults(searched = "harbour", movies = cards("mov", 14))
        val state = SearchState(query = "harbour", scope = SearchScope.MOVIE, scopes = scopes, results = found)
        show { TvSearchContent(state, browseState(), PeopleBrowseState(), searchActions) }
        scrollTo(hasText("mov title 1"))
        shoot("search-grid")
        compose.onAllNodes(hasClickAction() and hasText("MT")).onFirst().requestFocus()
        compose.waitForIdle()
        key("right", "down")
        shoot("search-grid-focused")
    }

    @Test
    fun searchPeopleScopeListsShelvesFiltersAndGrid() {
        val people = (1..14).map { PersonCard("p$it", "Person $it", "Actor") }
        val state = SearchState(scope = SearchScope.PEOPLE, scopes = scopes)
        val browse =
            PeopleBrowseState(
                status = LoadStatus(Phase.READY),
                filters = listOf(PeopleFilter.POPULAR, PeopleFilter.ACTORS, PeopleFilter.DIRECTORS),
                shelves = listOf(PeopleShelfState("library", "From your Library", people.take(6))),
                popular = people,
                attribution = "People data: public source",
            )
        show { TvSearchContent(state, browseState(), browse, searchActions) }
        compose.onNodeWithText("From your Library").assertIsDisplayed()
        shoot("search-people")
        press("Actors")
        assertEquals(listOf("people-ACTORS"), calls)
    }

    @Test
    @Config(qualifiers = "de-w960dp-h540dp-xhdpi")
    fun searchInGerman() {
        val found = SearchResults(searched = "harbour", library = cards("lib", 5), movies = cards("mov", 7))
        val state = SearchState(query = "harbour", scopes = scopes, results = found)
        show { TvSearchContent(state, browseState(), PeopleBrowseState(), searchActions) }
        compose.onAllNodesWithText("In deiner Bibliothek").onFirst().assertIsDisplayed()
        shoot("search-de")
    }

    private fun personState() =
        PersonState(
            status = LoadStatus(Phase.READY),
            id = "p1",
            name = "Mara Venn",
            roles = listOf("Actor", "Director"),
            firstYear = 1998,
            about =
                PersonAbout(
                    biography = "Mara Venn began on the stage in Rotterdam. " + "She moved to film in the late nineties. ".repeat(14),
                    birthday = "1974-03-02",
                    birthplace = "Rotterdam, Netherlands",
                    department = "Acting",
                ),
            knownFor = cards("known", 8),
            inLibrary = cards("owned", 4),
            decades = listOf(Decade(2020, cards("twenty", 6)), Decade(2010, cards("ten", 6))),
            oftenWith = (1..5).map { PersonCard("o$it", "Colleague $it", "Actor") },
            limit = MovieLimit(3, 7),
            role = FilmRole.ALL,
            sort = FilmSort.NEWEST,
            attribution = "Credits: public source",
        )

    @Test
    fun personShowsFilmographyAndOpensAbout() {
        show { TvPersonContent(personState(), onRefresh = {}, onSelect = { role, sort -> calls += "${role.wire}-${sort.wire}" }) }
        compose.onNodeWithText("Mara Venn").assertIsDisplayed()
        compose.onNodeWithText("Known for").assertIsDisplayed()
        shoot("person")
        press("Director")
        assertEquals(listOf("director-newest"), calls)
        press("About")
        compose.onNodeWithText("About Mara Venn").assertIsDisplayed()
        compose.onNodeWithText("Done").assertIsFocused()
        shoot("person-about")
    }

    @Test
    @Config(qualifiers = "de-w960dp-h540dp-xhdpi")
    fun personInGerman() {
        show { TvPersonContent(personState(), onRefresh = {}, onSelect = { _, _ -> }) }
        compose.onNodeWithText("Bekannt für").assertIsDisplayed()
        shoot("person-de")
    }

    @Test
    fun genreShowsBillboardAndRows() {
        val top = cards("top", 8)
        val state =
            GenreState(
                status = LoadStatus(Phase.READY),
                type = ExploreType.MOVIES,
                genreId = 18,
                name = "Drama",
                hero = listOf(card("hero", "The Quiet Harbour")),
                rows = listOf(CardRow("top_services", "top_services", "", cards = top, seeAll = true), row("best", "Best of drama")),
                top =
                    TopServices(
                        chips = listOf(ServiceChip(1, "Netflix"), ServiceChip(2, "Disney+")),
                        selected = 1,
                        cards = top,
                        serviceName = "Netflix",
                    ),
            )
        show { TvGenreContent(state, onRefresh = {}, onPickService = { calls += "service-$it" }) }
        compose.onNodeWithText("Drama").assertIsDisplayed()
        shoot("genre")
        press("Disney+")
        key("down", "right")
        compose.onNodeWithText("About top title 3").assertExists()
        key("down", "down")
        compose.onNode(isFocused()).assertExists()
        shoot("genre-focused")
        assertEquals(listOf("service-2"), calls)
    }

    @Test
    fun browseListShowsPosterGrid() {
        val state =
            BrowseViewState(
                status = LoadStatus(Phase.READY),
                title = "Cozy nights",
                genreName = "Drama",
                type = ExploreType.MOVIES,
                cards = cards("cozy", 13),
            )
        val link = BrowseLink(BrowseTarget.MOOD, ExploreType.MOVIES, 18, "", "Cozy nights")
        show { TvBrowseListView(state, link, onRefresh = {}, onOpenGenre = {}) }
        compose.onNodeWithText("Cozy nights").assertIsDisplayed()
        shoot("browse-list")
        focusFirstCard()
        key("right", "down", "down")
        compose.onNode(isFocused()).assertIsDisplayed()
        shoot("browse-list-focused")
    }

    @Test
    fun emptyMoodOffersTheGenre() {
        val state =
            BrowseViewState(status = LoadStatus(Phase.READY), title = "Cozy nights", genreName = "Drama", type = ExploreType.MOVIES)
        val link = BrowseLink(BrowseTarget.MOOD, ExploreType.MOVIES, 18, "", "Cozy nights")
        show { TvBrowseListView(state, link, onRefresh = {}, onOpenGenre = { calls += "genre" }) }
        press("Open Drama")
        assertEquals(listOf("genre"), calls)
        shoot("browse-empty")
    }

    @Test
    fun collectionListsMoviesAndTheyTakeFocus() {
        val state =
            SeriesState(
                status = LoadStatus(Phase.READY),
                id = "c1",
                name = "The Harbour Collection",
                movies =
                    listOf(
                        SeriesMovie(1, "The Quiet Harbour", 2020, 1, 2, false, listOf(1), listOf(1, 2)),
                        SeriesMovie(2, "The Loud Harbour", 2023, 2, 3, false, listOf(2, 3), listOf(3, 4, 5)),
                    ),
            )
        show { TvCollectionContent(state, onRefresh = {}) }
        compose.onNodeWithText("The Harbour Collection").assertIsDisplayed()
        compose.onNodeWithText("The Quiet Harbour (2020)").requestFocus()
        compose.waitForIdle()
        compose.onNodeWithText("The Quiet Harbour (2020)").assertIsFocused()
        shoot("collection")
    }
}
