package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.library.ActiveFilter
import ch.orellbuehler.acolyte.feature.library.LibStateKey
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.feature.library.LibraryLayout
import ch.orellbuehler.acolyte.feature.library.LibraryNoticeKind
import ch.orellbuehler.acolyte.feature.library.LibraryPrefs
import ch.orellbuehler.acolyte.feature.library.LibraryPresenter
import ch.orellbuehler.acolyte.feature.library.LibrarySort
import ch.orellbuehler.acolyte.nav.LibraryTool
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountSnapshot
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun libraryAccount(
    role: String = "owner",
    perms: List<String> = listOf("ask", "approve", "manage_library", "settings"),
    off: List<String> = emptyList(),
    handsOff: Boolean = false,
    views: Map<String, String> = emptyMap(),
): Account {
    val parts = listOf("movies", "shows", "music", "subtitles", "wishlist", "sources").map { PartView(part = it, on = it !in off) }
    val me =
        Me(
            id = 1,
            name = "admin",
            role = role,
            perms = perms,
            handsOff = handsOff,
            views = JsonObject(views.mapValues { JsonPrimitive(it.value) }),
        )
    return Account.from(BASE, SignInMethod.PASSWORD, AccountSnapshot(me, Parts(parts)))
}

class LibraryPresenterTest {
    private val grid =
        """{"titles":[
            {"id":1,"kind":"movie","name":"Heat","year":1995,"keep_up_to_date":true,"ids":{"tmdb":"949"},
             "artwork":["/api/v1/artwork/a.jpg"],"added_at":"2026-01-01T00:00:00Z","size_bytes":100,
             "copy":{"file_id":11,"version_id":21,"codec":"h264","protected":false},
             "slots":[{"id":5,"state":"in_library","file":{"ID":11,"State":"present","Quality":{"resolution":1080}}}],
             "asks":[{"title_id":1,"state":"available","asked_by":{"id":7,"name":"Sam"},"people":[]}],"versions":[{"id":21,"label":"Main","main":true}]},
            {"id":2,"kind":"movie","name":"Alien","year":1979,"keep_up_to_date":true,"ids":{"tmdb":"348"},
             "added_at":"2026-02-01T00:00:00Z","copy":{"file_id":12,"version_id":22,"codec":"hevc","protected":true},
             "slots":[{"id":6,"state":"wanted"}],"asks":null},
            {"id":3,"kind":"movie","name":"The Thing","year":1982,"keep_up_to_date":false,"slots":[{"id":7,"state":"wanted"}]},
            {"id":4,"kind":"show","name":"Dark","keep_up_to_date":true,"slots":[],
             "show":{"status":"ended","episodes":26,"missing":0,"in_library":26,"seasons":[{"number":1,"keep_up_to_date":true,"aired":26}]}}
        ]}"""

    private fun FakeServer.library() {
        json("GET", "/api/v1/titles", grid)
        json("GET", "/api/v1/my/watched", """{"enabled":true,"linked":true,"movies":[1],"shows":{}}""")
        json("GET", "/api/v1/library/watched-by-anyone", """{"enabled":true,"titles":[2]}""")
        json("GET", "/api/v1/subtitles/missing", """{"missing":[],"total":0,"titles":[{"id":1,"name":"Heat","kind":"movie","count":1}]}""")
        json("GET", "/api/v1/collections", """{"collections":[{"id":9,"name":"Alien Collection","title_ids":[2]}]}""")
        json("GET", "/api/v1/holding", """{"count":3}""")
    }

    private fun presenter(
        server: FakeServer,
        account: Account = libraryAccount(),
        kind: LibraryKind? = LibraryKind.MOVIE,
        store: MemoryStore = MemoryStore(),
    ) = LibraryPresenter(server.api(), { account }, LibraryPrefs(store), kind)

    @Test
    fun loadsMoviesWithStateAndExtras() =
        runTest {
            val server = FakeServer()
            server.library()
            val presenter = presenter(server)
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals(3, state.total)
            assertEquals(listOf(2L, 1L, 3L), state.items.map { it.id })
            val heat = state.items.first { it.id == 1L }
            assertEquals(LibStateKey.IN_LIBRARY, heat.state?.key)
            assertEquals("1080p", heat.state?.quality)
            assertEquals("949", heat.tmdb)
            assertEquals(1, heat.watchedSeen)
            assertEquals(3, state.deletedCount)
            assertTrue(state.canManage)
            assertTrue(LibraryTool.COPIES in state.tools)
            assertEquals("grid=1", server.request("GET", "/api/v1/titles").query)
            assertEquals(listOf(LibraryKind.MOVIE, LibraryKind.SHOW), state.kinds)
            assertTrue(state.subtitlesOption != null)
            assertTrue(state.watchedOption != null)
            assertEquals(2, state.seriesOptions.size)
            assertEquals(
                listOf("hevc", "h264"),
                state.codecOptions
                    .map { it.id }
                    .sorted()
                    .reversed(),
            )
        }

    @Test
    fun filtersCountWithTheirOwnDimensionLeftOut() =
        runTest {
            val server = FakeServer()
            server.library()
            val presenter = presenter(server)
            presenter.load()
            presenter.setStatus("missing")
            var state = presenter.state.value
            assertEquals(listOf(2L, 3L), state.items.map { it.id }.sorted())
            assertEquals(3, state.statusOptions.first { it.id == "all" }.count)
            assertEquals(2, state.statusOptions.first { it.id == "missing" }.count)
            assertEquals(2, state.missingCount)
            presenter.toggleKept("off")
            state = presenter.state.value
            assertEquals(listOf(3L), state.items.map { it.id })
            assertEquals(listOf(ActiveFilter.STATUS, ActiveFilter.KEPT), state.chips.map { it.filter })
            presenter.removeChip(ActiveFilter.STATUS)
            assertEquals(
                listOf(3L),
                presenter.state.value.items
                    .map { it.id },
            )
            presenter.setAsker("7")
            assertTrue(presenter.state.value.nothingMatches)
            presenter.clearFilters()
            assertEquals(3, presenter.state.value.items.size)
            presenter.toggleWatched()
            assertEquals(
                listOf(2L),
                presenter.state.value.items
                    .map { it.id },
            )
            presenter.clearFilters()
            presenter.setSeries(9)
            assertEquals(
                listOf(2L),
                presenter.state.value.items
                    .map { it.id },
            )
        }

    @Test
    fun sortFlipsAndUsesStartDirection() =
        runTest {
            val server = FakeServer()
            server.library()
            val presenter = presenter(server)
            presenter.load()
            presenter.setSort(LibrarySort.NAME)
            assertFalse(presenter.state.value.descending)
            assertEquals(
                listOf(2L, 1L, 3L),
                presenter.state.value.items
                    .map { it.id },
            )
            presenter.setSort(LibrarySort.NAME)
            assertTrue(presenter.state.value.descending)
            assertEquals(
                listOf(3L, 1L, 2L),
                presenter.state.value.items
                    .map { it.id },
            )
        }

    @Test
    fun searchKeepsServerMatches() =
        runTest {
            val server = FakeServer()
            server.library()
            val presenter = presenter(server)
            presenter.load()
            server.on("GET", "/api/v1/titles") { request ->
                if (request.url.parameters.contains(
                        "brief",
                    )
                ) {
                    respondJson("""{"titles":[{"id":3,"kind":"movie","name":"The Thing"}]}""")
                } else {
                    respondJson(grid)
                }
            }
            presenter.search("Das Ding")
            assertEquals(
                listOf(3L),
                presenter.state.value.items
                    .map { it.id },
            )
            assertTrue(server.request("GET", "/api/v1/titles").query.contains("q=Das"))
            presenter.search("heat")
            assertEquals(
                listOf(1L, 3L),
                presenter.state.value.items
                    .map { it.id },
            )
            presenter.search("")
            assertEquals(3, presenter.state.value.items.size)
        }

    @Test
    fun kindSwitchResetsAndIsRemembered() =
        runTest {
            val server = FakeServer()
            server.library()
            val store = MemoryStore()
            val presenter = presenter(server, store = store)
            presenter.load()
            presenter.setStatus("missing")
            presenter.setKind(LibraryKind.SHOW)
            val state = presenter.state.value
            assertEquals(LibraryKind.SHOW, state.kind)
            assertEquals(LibrarySort.NEXT, state.sort)
            assertFalse(state.filter.isActive)
            assertEquals(
                LibStateKey.ENDED,
                state.items
                    .single()
                    .state
                    ?.key,
            )
            assertEquals(LibraryKind.SHOW, presenter(server, kind = null, store = store).state.value.kind)
        }

    @Test
    fun layoutIsSavedOnTheAccount() =
        runTest {
            val server = FakeServer()
            server.library()
            server.json("PUT", "/api/v1/my/views", "{}")
            val presenter = presenter(server, account = libraryAccount(views = mapOf("library.view.movie" to "posters")))
            assertEquals(LibraryLayout.GRID, presenter.state.value.layout)
            presenter.setLayout(LibraryLayout.LIST)
            val body = server.request("PUT", "/api/v1/my/views").body.orEmpty()
            assertTrue(body.contains("\"library.view.movie\":\"rows\""))
            assertTrue(body.contains("\"library.view.show\":\"rows\""))
        }

    @Test
    fun savedFiltersStayOnTheDevice() =
        runTest {
            val server = FakeServer()
            server.library()
            val store = MemoryStore()
            val presenter = presenter(server, store = store)
            presenter.load()
            presenter.setStatus("missing")
            presenter.saveFilter("  To get  ")
            assertEquals(
                LibraryNoticeKind.FILTER_SAVED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                listOf("To get"),
                presenter.state.value.savedFilters
                    .map { it.name },
            )
            assertTrue(
                presenter.state.value.savedFilters
                    .single()
                    .active,
            )
            presenter.clearFilters()
            presenter.applySavedFilter("To get")
            assertEquals(2, presenter.state.value.items.size)
            val again = presenter(server, store = store)
            assertEquals(
                listOf("To get"),
                again.state.value.savedFilters
                    .map { it.name },
            )
            again.deleteSavedFilter("To get")
            assertTrue(
                again.state.value.savedFilters
                    .isEmpty(),
            )
        }

    @Test
    fun bulkKeepCountsFailures() =
        runTest {
            val server = FakeServer()
            server.library()
            server.on("PUT", "/api/v1/titles/1") { respondJson("""{"title":{}}""") }
            server.on("PUT", "/api/v1/titles/2") { respondJson("""{"error":"nope"}""", 400) }
            val presenter = presenter(server)
            presenter.load()
            presenter.startSelecting()
            presenter.selectAll()
            presenter.toggle(3)
            assertEquals(2, presenter.state.value.selectedCount)
            presenter.keepSelected(false)
            val notice = presenter.state.value.notice!!
            assertEquals(LibraryNoticeKind.UNKEPT, notice.kind)
            assertEquals(1, notice.count)
            assertEquals(2, notice.total)
            assertTrue(notice.partly)
            assertEquals("{\"keep_up_to_date\":false}", server.request("PUT", "/api/v1/titles/1").body)
        }

    @Test
    fun findAnotherCopySkipsProtectedFiles() =
        runTest {
            val server = FakeServer()
            server.library()
            server.json(
                "POST",
                "/api/v1/library/copies/search/preview",
                """{"copies":[{"file_id":11,"version_id":21,"title":"Heat","version":"Main","codec":"h264"}],"slot_count":1,"review_token":"tok"}""",
            )
            server.json("POST", "/api/v1/library/copies/search", """{"items":[],"job_ids":[4]}""", 202)
            val presenter = presenter(server)
            presenter.load()
            presenter.startSelecting()
            presenter.toggle(1)
            presenter.toggle(2)
            assertTrue(presenter.state.value.canFindCopies)
            presenter.reviewCopies()
            assertEquals(
                """{"selections":[{"file_id":11,"version_id":21}]}""",
                server.request("POST", "/api/v1/library/copies/search/preview").body,
            )
            assertEquals(
                1,
                presenter.state.value.copiesReview
                    ?.items
                    ?.size,
            )
            presenter.startCopies()
            assertTrue(server.request("POST", "/api/v1/library/copies/search").body!!.contains("\"review_token\":\"tok\""))
            assertNull(presenter.state.value.copiesReview)
            assertEquals(
                LibraryNoticeKind.COPIES_SEARCHING,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun membersSeeNoManageTools() =
        runTest {
            val server = FakeServer()
            server.library()
            val presenter = presenter(server, account = libraryAccount(role = "member", perms = listOf("ask")))
            presenter.load()
            val state = presenter.state.value
            assertFalse(state.canManage)
            assertTrue(state.tools.isEmpty())
            assertNull(state.subtitlesOption)
            assertTrue(server.requests.none { it.path == "/api/v1/holding" })
            presenter.startSelecting()
            assertFalse(presenter.state.value.selecting)
        }

    @Test
    fun failedLoadReportsError() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/titles", """{"error":"this part is off","part":"media"}""", 409)
            val presenter = presenter(server)
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.PART_OFF,
                presenter.state.value.status.error
                    ?.code,
            )
        }

    @Test
    fun toolsFollowPartsAndHandsOff() {
        val owner = libraryAccount(handsOff = true, off = listOf("subtitles"))
        assertFalse(LibraryTool.MISSING_SUBTITLES in owner.libraryTools)
        assertFalse(LibraryTool.IMPORT in owner.libraryTools)
        assertTrue(LibraryTool.HELD in owner.libraryTools)
        val manager = libraryAccount(role = "admin", perms = listOf("manage_library"))
        assertFalse(LibraryTool.LIBRARY_FOLDERS in manager.libraryTools)
        assertFalse(LibraryTool.IMPORT_FILES in manager.libraryTools)
        assertTrue(LibraryTool.IMPORT in manager.libraryTools)
        assertTrue(libraryAccount(role = "member", perms = listOf("ask")).libraryTools.isEmpty())
        assertFalse(LibraryTool.COPIES in libraryAccount(off = listOf("movies")).libraryTools)
    }
}
