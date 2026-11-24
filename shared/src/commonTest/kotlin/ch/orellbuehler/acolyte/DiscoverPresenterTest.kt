package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.discover.CardAction
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.ExplorePresenter
import ch.orellbuehler.acolyte.feature.discover.ExploreStatsPresenter
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.FilmSort
import ch.orellbuehler.acolyte.feature.discover.HiddenPresenter
import ch.orellbuehler.acolyte.feature.discover.HideFor
import ch.orellbuehler.acolyte.feature.discover.HideSheetPresenter
import ch.orellbuehler.acolyte.feature.discover.HideWhat
import ch.orellbuehler.acolyte.feature.discover.MarkLabel
import ch.orellbuehler.acolyte.feature.discover.NoteKind
import ch.orellbuehler.acolyte.feature.discover.PersonPresenter
import ch.orellbuehler.acolyte.feature.discover.PopularPresenter
import ch.orellbuehler.acolyte.feature.discover.SearchPresenter
import ch.orellbuehler.acolyte.feature.discover.SearchScope
import ch.orellbuehler.acolyte.feature.discover.SeriesOutcome
import ch.orellbuehler.acolyte.feature.discover.SeriesPresenter
import ch.orellbuehler.acolyte.feature.discover.TopSource
import ch.orellbuehler.acolyte.feature.discover.WhatKind
import ch.orellbuehler.acolyte.feature.discover.describes
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiscoverPresenterTest {
    private fun items(
        from: Int,
        count: Int,
        kind: String = "movie",
    ) = (from until from + count).map { item("$it", kind) }

    @Test
    fun exploreBuildsRowsInOrder() =
        runTest {
            val server = FakeServer()
            val head =
                """{"type":"all","building":false,"tail_rows":1,"taste":["Drama"],"hero":[${item(
                    "900",
                    "show",
                    extra = ""","fresh":{"kind":"new_season","text":"New Season"}""",
                )}],"head":[""" +
                    row("top_services", "top_services", emptyList(), "Trending on streaming") + "," +
                    row("trending", "trending", items(1, 10)) + "," +
                    row("best", "best", items(1, 9) + items(30, 2)) + "," +
                    row("small", "household", items(40, 3)) + "]}"
            server.json("GET", "/api/v1/explore/head", head)
            server.json(
                "GET",
                "/api/v1/explore/service-top",
                """{"region":"CH","attribution":"Data","services":[{"service":8,"name":"Netflix","ranked":true,"age_seconds":10,""" +
                    """"items":[${items(
                        100,
                        3,
                    ).joinToString(",")}]},{"service":9,"name":"Other","ranked":false,"items":[${item("200")}]}]}""",
            )
            server.json("GET", "/api/v1/explore/new/services", """{"row":{"id":"np","type":"np_services","title":"New","items":[]}}""")
            server.json("GET", "/api/v1/explore/genre-row", """{"genres":[]}""")
            server.json("GET", "/api/v1/explore/strip", """{"tiles":[{"kind":"movie","genre":18,"name":"Drama","section":"genres"}]}""")
            server.json(
                "GET",
                "/api/v1/titles/owned",
                """{"items":[{"kind":"movie","tmdb":"1","id":7,"state":"owned","genres":["Drama"]}]}""",
            )
            server.json("GET", "/api/v1/explore/tail", """{"page":0,"more":false,"rows":[${row("tail1", "classics", items(50, 8))}]}""")
            val api = server.api()
            val presenter =
                ExplorePresenter(api, { discoverAccount(perms = listOf("ask")) }, owned(api), prefs(), "CH", "Europe/Zurich")
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals(listOf("top_services", "trending"), state.rows.map { it.type })
            assertEquals(3, state.rows[0].cards.size)
            assertTrue(state.rows[0].ranked)
            assertEquals(TopSource.TODAY, state.top?.source)
            assertEquals(listOf("Netflix", "Other"), state.top?.chips?.map { it.name })
            assertEquals(
                MarkLabel.IN_LIBRARY,
                state.rows[1]
                    .cards[0]
                    .mark
                    ?.label,
            )
            assertEquals(CardAction.ASK, state.rows[1].cards[1].action)
            assertEquals("900", state.hero.single().tmdb)
            assertEquals(2, state.heroPills.size)
            assertEquals(1, state.strip.size)
            assertTrue(state.more)
            val query = server.request("GET", "/api/v1/explore/head").query
            assertTrue(query.contains("type=all") && query.contains("region=CH") && query.contains("tz=Europe"), query)
            assertTrue(server.request("GET", "/api/v1/explore/strip").query.contains("row=Row"))

            presenter.selectService(9)
            assertEquals(
                listOf("200"),
                presenter.state.value.rows[0]
                    .cards
                    .map { it.tmdb },
            )
            assertEquals(
                TopSource.POPULAR,
                presenter.state.value.top
                    ?.source,
            )

            presenter.loadMore()
            assertEquals(
                "classics",
                presenter.state.value.rows
                    .last()
                    .type,
            )
            assertFalse(presenter.state.value.more)

            presenter.stripShown()
            assertEquals("""{"event":"shown","categories":["drama"]}""", server.request("POST", "/api/v1/explore/strip/counts").body)
            presenter.opened(
                presenter.state.value.rows[1]
                    .cards[0],
            )
            assertTrue(server.request("POST", "/api/v1/explore/counts").body!!.contains("\"row_type\":\"trending\""))
        }

    @Test
    fun exploreTabFiltersKinds() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/explore/head",
                """{"hero":[],"head":[${row("mix", "trending", items(1, 8) + items(20, 8, "show"))}]}""",
            )
            val api = server.api()
            val store =
                ch.orellbuehler.acolyte.session
                    .MemoryStore()
            val presenter = ExplorePresenter(api, { discoverAccount() }, owned(api), prefs(store), "US", "UTC")
            presenter.select(ExploreType.SHOWS)
            assertTrue(
                presenter.state.value.rows
                    .single()
                    .cards
                    .all { it.kind == CardKind.SHOW },
            )
            assertEquals(ExploreType.SHOWS, prefs(store).exploreType)
        }

    @Test
    fun popularReadsRailAndServiceChips() =
        runTest {
            val server = FakeServer()
            val services = ""","streaming":{"service":8,"name":"Netflix"}"""
            val other = ""","streaming":{"service":9,"name":"Disney"}"""
            server.json(
                "GET",
                "/api/v1/explore/new",
                """{"type":"all","page":0,"more":false,"hero":[${item("1", extra = services)}],""" +
                    """"rail":[{"day":"2026-11-25","items":[${item(
                        "2",
                        inLibrary = true,
                        extra = ""","what":"On Netflix","upcoming":true""",
                    )}]}],""" +
                    """"rows":[${row("np_services", "np_services", listOf(item("3", extra = services), item("4", extra = other)))}]}""",
            )
            server.json(
                "GET",
                "/api/v1/explore/new/service",
                """{"row":{"id":"np_services","type":"np_services","items":[${item("5", extra = services)}]}}""",
            )
            val api = server.api()
            val presenter = PopularPresenter(api, { discoverAccount() }, owned(api), prefs()) { "2026-11-24" }
            presenter.load()
            val state = presenter.state.value
            assertEquals("1", state.hero?.tmdb)
            val day = state.days.single()
            assertEquals(1, day.offset)
            assertEquals(WhatKind.ON_SERVICE, day.entries.single().what)
            assertEquals("Netflix", day.entries.single().text)
            assertTrue(day.entries.single().alsoComing)
            assertEquals(
                listOf("Netflix", "Disney"),
                state.rows
                    .single()
                    .chips
                    .map { it.name },
            )
            presenter.selectService("np_services", 8)
            assertEquals(
                listOf("5"),
                presenter.state.value.rows
                    .single()
                    .row.cards
                    .map { it.tmdb },
            )
            assertTrue(server.request("GET", "/api/v1/explore/new/service").query.contains("row=services"))
            presenter.selectService("np_services", 0)
            assertEquals(
                2,
                presenter.state.value.rows
                    .single()
                    .row.cards.size,
            )
        }

    @Test
    fun describesPlainWords() {
        assertTrue(describes("space movie with a robot from 2014"))
        assertTrue(describes("dramas from the 90s"))
        assertTrue(describes("films about whales"))
        assertFalse(describes("Dune 2021"))
        assertFalse(describes("tmdb:693134"))
        assertFalse(describes("Interstellar"))
    }

    @Test
    fun searchRunsEveryGroup() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/library/search",
                """{"titles":[{"id":4,"kind":"show","name":"Dark","ids":{"tmdb":"70523"},""" +
                    """"show":{"missing":2,"seasons":[{"number":1}]}}],""" +
                    """"people":[{"name":"Louis Hofmann","titles":[{"id":4,"name":"Dark","kind":"show"}]}]}""",
            )
            server.json(
                "GET",
                "/api/v1/titles/lookup",
                """{"movies":[{"name":"Dark City","year":1998,"ids":{"tmdb":"2666"},"hidden":true},""" +
                    """{"name":"Dark","year":2005,"ids":{"tmdb":"9"},"ask":{"id":1,"state":"asked","mine":true}}]}""",
            )
            server.json("GET", "/api/v1/shows/lookup", """{"shows":[{"name":"Dark","ids":{"tmdb":"70523"},"title_id":4}]}""")
            server.json("GET", "/api/v1/people/search", """{"people":[{"id":"1","name":"Dark Person","department":"Acting"}]}""")
            server.json("GET", "/api/v1/titles/owned", """{"items":[{"kind":"show","tmdb":"70523","id":4,"state":"partial"}]}""")
            val api = server.api()
            val store =
                ch.orellbuehler.acolyte.session
                    .MemoryStore()
            val presenter = SearchPresenter(api, { discoverAccount(role = "admin", perms = MANAGER_PERMS) }, owned(api), prefs(store))
            presenter.load()
            presenter.search("Dark")
            val results = presenter.state.value.results!!
            assertEquals(listOf("Dark", "Louis Hofmann"), results.library.map { it.name })
            assertEquals(NoteKind.MISSING_EPISODES, results.library[0].note?.kind)
            assertEquals(2, results.library[0].note?.count)
            assertEquals(MarkLabel.HIDDEN, results.movies[0].mark?.label)
            assertEquals(MarkLabel.REQUESTED, results.movies[1].mark?.label)
            assertNull(results.shows[0].action)
            assertEquals(CardAction.ADD, results.movies[0].action)
            assertEquals("Dark", results.top.first().name)
            assertEquals(1, results.people.size)
            assertFalse(results.isEmpty)

            presenter.remember("Dark")
            presenter.remember("dune")
            presenter.remember("DARK")
            assertEquals(listOf("DARK", "dune"), presenter.state.value.recents)
            presenter.setOnlyLibrary(true)
            assertTrue(
                presenter.state.value.shown!!
                    .movies
                    .isEmpty(),
            )
            assertEquals(
                2,
                presenter.state.value.shown!!
                    .library.size,
            )

            presenter.selectScope(SearchScope.PEOPLE)
            presenter.search("Dark")
            assertTrue(server.request("GET", "/api/v1/library/search").query.contains("kind=people"))
        }

    @Test
    fun personLoadsEveryPage() =
        runTest {
            val server = FakeServer()
            var calls = 0
            server.on("GET", "/api/v1/people/7") { request ->
                calls++
                val page = request.url.parameters["page"]
                val movies =
                    if (page == "1") {
                        """[{"name":"A","year":1994,"ids":{"tmdb":"1"},"poster":"p","roles":["Actor"],"characters":["Hero"]},""" +
                            """{"name":"B","year":2003,"ids":{"tmdb":"2"},"poster":"p","roles":["Director"]}]"""
                    } else {
                        """[{"name":"C","year":0,"ids":{"tmdb":"3"},"poster":"p","roles":["Actor"]},""" +
                            """{"name":"D","year":1999,"ids":{"tmdb":"4"},"roles":["Actor"]}]"""
                    }
                respondJson(
                    """{"person":{"id":"7","name":"Jo","department":"Acting"},"movies":$movies,"page":$page,"total_pages":2,""" +
                        """"stats":{"first_year":1994,"films":4},"known_for":[{"name":"A","ids":{"tmdb":"1"},"poster":"p"}]}""",
                )
            }
            val api = server.api()
            val presenter = PersonPresenter(api, { discoverAccount() }, owned(api), prefs(), "7")
            presenter.load()
            val state = presenter.state.value
            assertEquals(2, calls)
            assertEquals("Jo", state.name)
            assertEquals(listOf("Actor", "Director"), state.roles)
            assertEquals(listOf(0, 1990, 2000), state.decades.map { it.year })
            assertEquals(
                NoteKind.DIRECTOR,
                state.decades[2]
                    .cards[0]
                    .note
                    ?.kind,
            )
            assertEquals("Hero", state.decades[1].cards[0].subtitle)
            assertEquals(1, state.knownFor.size)
            presenter.select(sort = FilmSort.POPULAR)
            assertEquals(
                listOf(2000, 1990, 0),
                presenter.state.value.decades
                    .map { it.year },
            )
            assertTrue(server.request("GET", "/api/v1/people/7").query.contains("sort=popular"))
        }

    @Test
    fun seriesReviewsThenRemoves() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/collections/3",
                """{"id":3,"name":"Dune","titles":[{"id":1,"name":"Dune","year":2021,"version_ids":[5],""" +
                    """"files":[{"id":9,"protected":true}]},""" +
                    """{"id":2,"name":"Dune Two","year":2024,"version_ids":[6],"files":[]}]}""",
            )
            server.json(
                "POST",
                "/api/v1/collections/3/remove/preview",
                """{"review_token":"t1","applied":false,"titles":[{"id":2,"name":"Dune Two","selected":true}]}""",
            )
            server.json(
                "POST",
                "/api/v1/collections/3/remove",
                """{"applied":true,"results":[{"title_id":2,"name":"Dune Two","status":"removed"}]}""",
            )
            val presenter = SeriesPresenter(server.api(), { discoverAccount(role = "admin", perms = MANAGER_PERMS) }, "3")
            presenter.load()
            assertEquals(setOf(1L, 2L), presenter.state.value.selected)
            assertTrue(
                presenter.state.value.movies[0]
                    .hasProtected,
            )
            presenter.setDeleteFiles(true)
            assertTrue(presenter.state.value.needsAcknowledge)
            presenter.toggle(1)
            assertFalse(presenter.state.value.needsAcknowledge)
            presenter.review()
            assertEquals(
                """{"title_ids":[2],"delete_files":true,"never_add":false,"acknowledge_protection":false}""",
                server.request("POST", "/api/v1/collections/3/remove/preview").body,
            )
            assertEquals(
                1,
                presenter.state.value.review
                    ?.size,
            )
            presenter.apply()
            assertTrue(server.request("POST", "/api/v1/collections/3/remove").body!!.contains("\"review_token\":\"t1\""))
            assertEquals(
                SeriesOutcome.REMOVED,
                presenter.state.value.results
                    ?.single()
                    ?.outcome,
            )
        }

    @Test
    fun hidesAndShowsAgain() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/users", """{"users":[{"id":2,"name":"sam"},{"id":5,"name":"kim"}]}""")
            server.json(
                "GET",
                "/api/v1/hidden",
                """{"hidden":[{"id":1,"kind":"movie","tmdb_id":"10","name":"Heat","user_id":0},""" +
                    """{"id":2,"kind":"movie","genre":"Horror","user_id":5},{"id":3,"kind":"show","tmdb_id":"4","user_id":8}]}""",
            )
            server.json("POST", "/api/v1/hidden", """{"id":9,"kind":"movie","genre":"Horror","user_id":5}""", 201)
            server.on("DELETE", "/api/v1/hidden/1") { respondJson("", 204) }
            val api = server.api()
            val list = HiddenPresenter(api, null)
            list.load()
            val rows = list.state.value.rows
            assertEquals(listOf(HideWhat.MOVIE, HideWhat.MOVIE_GENRE, HideWhat.SHOW), rows.map { it.what })
            assertEquals(listOf(HideFor.EVERYONE, HideFor.PERSON, HideFor.REMOVED_PERSON), rows.map { it.who })
            assertEquals("Horror", rows[1].name)
            assertEquals("kim", rows[1].person)
            assertEquals("TMDB 4", rows[2].name)
            assertTrue(list.showAgain(1))
            assertEquals(2, list.state.value.rows.size)

            val sheet = HideSheetPresenter(api, { discoverAccount() }, "10", CardKind.MOVIE, "Heat", listOf("Horror", "Made up"))
            sheet.load()
            assertEquals(listOf("Horror"), sheet.state.value.genres)
            assertEquals(2, sheet.state.value.hidden.size)
            assertEquals(
                listOf("kim"),
                sheet.state.value.people
                    .map { it.name },
            )
            sheet.pickEveryone(false)
            assertNull(sheet.hide())
            assertTrue(sheet.state.value.needsPerson)
            sheet.pickPerson(5)
            sheet.pickGenre("Horror")
            val done = assertNotNull(sheet.hide())
            assertEquals(9, done.id)
            assertEquals("kim", done.person)
            assertEquals("""{"kind":"movie","genre":"Horror","user_id":5}""", server.request("POST", "/api/v1/hidden").body)
        }

    @Test
    fun statsSkipTitleLayersForAdmins() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/explore/row-stats",
                """{"days":30,"rows":[{"row_type":"best","title":"Best","shown":4,"opens":1}]}""",
            )
            val presenter = ExploreStatsPresenter(server.api()) { discoverAccount(role = "admin", perms = MANAGER_PERMS) }
            presenter.load()
            assertEquals(1, presenter.state.value.rows.size)
            assertTrue(server.requests.none { it.path == "/api/v1/explore/title-stats" })
        }
}
