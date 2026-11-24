package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.model.OutsideList
import ch.orellbuehler.acolyte.api.model.Report
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.TraktDeviceCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.requests.FolderField
import ch.orellbuehler.acolyte.feature.requests.LimitKind
import ch.orellbuehler.acolyte.feature.requests.LineKind
import ch.orellbuehler.acolyte.feature.requests.ListKind
import ch.orellbuehler.acolyte.feature.requests.ListSeasons
import ch.orellbuehler.acolyte.feature.requests.NeverAddPresenter
import ch.orellbuehler.acolyte.feature.requests.OutsideListsPresenter
import ch.orellbuehler.acolyte.feature.requests.OutsideNoteKind
import ch.orellbuehler.acolyte.feature.requests.ReportFilter
import ch.orellbuehler.acolyte.feature.requests.ReportKind
import ch.orellbuehler.acolyte.feature.requests.ReportProblemPresenter
import ch.orellbuehler.acolyte.feature.requests.ReportsNoteKind
import ch.orellbuehler.acolyte.feature.requests.ReportsPresenter
import ch.orellbuehler.acolyte.feature.requests.RequestPage
import ch.orellbuehler.acolyte.feature.requests.RequestSettingsPresenter
import ch.orellbuehler.acolyte.feature.requests.RequestsInbox
import ch.orellbuehler.acolyte.feature.requests.SettingsFlag
import ch.orellbuehler.acolyte.feature.requests.listLink
import ch.orellbuehler.acolyte.feature.requests.mayOpen
import ch.orellbuehler.acolyte.feature.requests.partWords
import ch.orellbuehler.acolyte.feature.requests.searchBody
import ch.orellbuehler.acolyte.feature.requests.verificationUrl
import ch.orellbuehler.acolyte.session.Role
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestSettingsTest {
    private val settings =
        """{"movies_per_week":10,"shows_per_week":10,"music_per_week":10,"search_on_approve":true,"owner_auto_approve":true,
            "qualities":{"movie":[{"id":"standard","name":"","label":"","hidden":false,"pick_rules_id":0,"extra":false,"ready":true},
            {"id":"4k","name":"","label":"","hidden":false,"pick_rules_id":2,"extra":true,"ready":true}],"show":[]},
            "streaming_region":"","streaming_services":[],"skip_on_services":false}"""

    @Test
    fun pagesFollowPermissionsAndParts() {
        assertTrue(testAccount(Role.OWNER, emptyList()).mayOpen(RequestPage.RULES))
        assertFalse(testAccount(Role.ADMIN, listOf("approve")).mayOpen(RequestPage.RULES))
        assertTrue(testAccount(Role.ADMIN, listOf("manage_library")).mayOpen(RequestPage.NEVER_ADD))
        assertFalse(testAccount(Role.ADMIN, listOf("manage_library")).mayOpen(RequestPage.OUTSIDE_LISTS))
    }

    @Test
    fun requestRulesEditAndSave() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/asks", settings)
            server.json(
                "GET",
                "/api/v1/library-folders",
                """{"library_folders":[{"id":1,"kind":"movie","label":"Films","path":"/m"},{"id":2,"kind":"show","path":"/s"}]}""",
            )
            server.json(
                "GET",
                "/api/v1/pick-rules",
                """{"pick_rules":[{"id":2,"name":"4K","kinds":["movie","show"]},{"id":3,"name":"Lossless","kinds":["music"]}]}""",
            )
            server.json("GET", "/api/v1/settings/asks/plex-friends", """{"on":false,"owner_linked":false,"people":[]}""")
            server.json("GET", "/api/v1/settings/asks/streaming-cleanup", """{"running":false,"found":[]}""")
            server.on("PUT", "/api/v1/settings/asks") { respondJson((it.body as TextContent).text) }
            val presenter = RequestSettingsPresenter(server.api()) { testAccount(Role.OWNER, emptyList()) }
            presenter.load()
            var s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertFalse(s.dirty)
            assertEquals(listOf(1L), s.foldersFor(FolderField.MOVIE).map { it.id })
            assertEquals(listOf(3L), s.rulesFor(FolderField.MUSIC).map { it.id })
            presenter.setLimit(LimitKind.MOVIES, "12x")
            presenter.setFlag(SettingsFlag.PLEX_WATCHLISTS, true)
            presenter.setFlag(SettingsFlag.PLEX_FRIENDS, true)
            presenter.addLine(LineKind.MOVIE)
            presenter.setLineName(LineKind.MOVIE, 2, "Remux")
            presenter.moveLine(LineKind.MOVIE, 2, up = true)
            s = presenter.state.value
            assertTrue(s.dirty)
            assertTrue(s.canSave)
            assertEquals("12", s.limit(LimitKind.MOVIES))
            assertEquals(listOf("standard", "own-1", "4k"), s.lines(LineKind.MOVIE).map { it.id })
            assertTrue(s.showFriends)
            presenter.setFlag(SettingsFlag.PLEX_WATCHLISTS, false)
            assertFalse(presenter.state.value.flag(SettingsFlag.PLEX_FRIENDS))
            presenter.setLimit(LimitKind.SHOWS, "1001")
            assertFalse(presenter.state.value.canSave)
            presenter.setLimit(LimitKind.SHOWS, "0")
            presenter.save()
            s = presenter.state.value
            assertFalse(s.dirty)
            assertEquals(1, s.savedCount)
            assertEquals(12, s.saved.moviesPerWeek)
            assertEquals(0, s.saved.showsPerWeek)
            assertTrue(
                server
                    .request("PUT", "/api/v1/settings/asks")
                    .body
                    .orEmpty()
                    .contains(""""id":"own-1","name":"Remux""""),
            )
        }

    @Test
    fun outsideListsAddAList() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/outside-lists", """{"outside_lists":[]}""")
            server.json(
                "GET",
                "/api/v1/library-folders",
                """{"library_folders":[{"id":1,"kind":"movie","path":"/m"},{"id":2,"kind":"show","path":"/s"}]}""",
            )
            server.json("GET", "/api/v1/pick-rules", """{"pick_rules":[]}""")
            server.json(
                "GET",
                "/api/v1/settings/trakt-signin",
                """{"client_id":false,"client_secret":false,"signed_in":false,"waiting":false}""",
            )
            server.json("GET", "/api/v1/settings/trakt-key", """{"key":""}""")
            server.json("GET", "/api/v1/settings/trakt-token", """{"token":""}""")
            server.json("GET", "/api/v1/settings/mdblist-key", """{"key":""}""")
            server.json("POST", "/api/v1/outside-lists", """{"id":4,"name":"Fav","kind":"trakt_list","enabled":true}""", 201)
            val presenter = OutsideListsPresenter(server.api()) { testAccount(Role.OWNER, emptyList()) }
            presenter.load()
            assertTrue(presenter.state.value.isEmpty)
            presenter.startNew()
            presenter.setName(" Fav ")
            presenter.setKind(ListKind.TRAKT)
            presenter.setTitleKind(show = true)
            presenter.setRef("ana/favourites")
            presenter.setSeasons(ListSeasons.FUTURE)
            presenter.setNewSeasons(false)
            val s = presenter.state.value
            assertTrue(s.needsTraktKey)
            assertEquals(2L, s.draft.libraryFolderId)
            assertTrue(s.canSave)
            presenter.saveList()
            assertNull(presenter.state.value.editing)
            assertEquals(
                OutsideNoteKind.SAVED,
                presenter.state.value.note
                    ?.kind,
            )
            assertEquals(
                """{"name":"Fav","kind":"trakt_list","title_kind":"show","ref":"ana/favourites",""" +
                    """"library_folder_id":2,"pick_rules_id":0,""" +
                    """"keep_up_to_date":true,"search_on_add":false,"seasons":"future","new_seasons":"all",""" +
                    """"enabled":true,"sync_every_hours":12}""",
                server.request("POST", "/api/v1/outside-lists").body,
            )
        }

    @Test
    fun traktSignInPollsUntilApproved() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/outside-lists", """{"outside_lists":[]}""")
            server.json("GET", "/api/v1/library-folders", """{"library_folders":[]}""")
            server.json("GET", "/api/v1/pick-rules", """{"pick_rules":[]}""")
            server.json(
                "GET",
                "/api/v1/settings/trakt-signin",
                """{"client_id":true,"client_secret":true,"signed_in":false,"waiting":false}""",
            )
            server.json("GET", "/api/v1/settings/trakt-key", """{"key":"abc…"}""")
            server.json(
                "POST",
                "/api/v1/settings/trakt-signin",
                """{"user_code":"AB12","verification_url":"https://trakt.tv/activate","expires_in":600,"interval":5}""",
            )
            var checks = 0
            server.on("POST", "/api/v1/settings/trakt-signin/check") {
                checks++
                if (checks < 2) {
                    respondJson("""{"state":"waiting","status":{"client_id":true,"client_secret":true,"waiting":true}}""")
                } else {
                    respondJson(
                        """{"state":"signed_in","status":{"client_id":true,"client_secret":true,"signed_in":true,"username":"ana"}}""",
                    )
                }
            }
            val presenter = OutsideListsPresenter(server.api()) { testAccount(Role.OWNER, emptyList()) }
            presenter.load()
            assertTrue(presenter.state.value.canSignIn)
            presenter.signInTrakt()
            assertEquals(2, checks)
            assertNull(presenter.state.value.device)
            assertEquals(
                "ana",
                presenter.state.value.trakt
                    ?.username,
            )
            assertEquals(
                OutsideNoteKind.TRAKT_SIGNED_IN,
                presenter.state.value.note
                    ?.kind,
            )
        }

    @Test
    fun linksAndCodes() {
        assertEquals("https://www.themoviedb.org/list/12", listLink(OutsideList(kind = "tmdb_list", ref = "12")))
        assertEquals("https://trakt.tv/users/ana/lists/fav", listLink(OutsideList(kind = "trakt_list", ref = "ana/fav")))
        assertEquals("https://trakt.tv/users/ana/watchlist", listLink(OutsideList(kind = "trakt_list", ref = "ana/watchlist")))
        assertEquals(
            "https://trakt.tv/shows/watched/weekly",
            listLink(OutsideList(kind = "trakt_list", ref = "chart:watched-weekly", titleKind = "show")),
        )
        assertEquals("https://mdblist.com/lists/ana/fav", listLink(OutsideList(kind = "mdblist_list", ref = "ana/fav")))
        assertEquals("https://trakt.tv/activate", verificationUrl(TraktDeviceCode(verificationUrl = "https://evil.example")))
    }

    @Test
    fun neverAddChecksTheInput() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/never-add", """{"never_add":[{"id":1,"kind":"movie","ids":{"tmdb":"5"},"name":"Old"}]}""")
            server.json("POST", "/api/v1/never-add", """{"id":2,"kind":"show","ids":{"tmdb":"1399"},"name":"GoT"}""", 201)
            server.on("DELETE", "/api/v1/never-add/1") { respondJson("", 204) }
            val presenter = NeverAddPresenter(server.api()) { testAccount() }
            presenter.load()
            assertFalse(presenter.add(false, "abc", "X", ""))
            assertTrue(presenter.state.value.missing)
            assertTrue(presenter.add(true, "tmdb:1399", " GoT ", ""))
            assertEquals("""{"kind":"show","tmdb_id":"1399","name":"GoT"}""", server.request("POST", "/api/v1/never-add").body)
            presenter.remove(1)
            assertEquals(
                listOf(2L),
                presenter.state.value.entries
                    .map { it.id },
            )
            assertEquals("Old", presenter.state.value.removed)
        }

    @Test
    fun reportsFilterAndFix() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/reports",
                """{"open":1,"reports":[{"id":1,"title_id":5,"title_name":"A","title_kind":"movie","kind":"sound","state":"fixed","created_at":"2026-11-01T10:00:00Z"},
                    {"id":2,"title_id":6,"title_name":"B","title_kind":"show","kind":"picture","state":"open","created_at":"2026-10-01T10:00:00Z",
                     "made_by":{"id":3,"name":"cy"},"mine":false,"season":2}]}""",
            )
            server.json(
                "POST",
                "/api/v1/reports/2/fix",
                """{"report":{"id":2,"title_id":6,"title_name":"B","kind":"picture","state":"fixed"}}""",
            )
            server.json("GET", "/api/v1/messages", """{"messages":[],"unread":0}""")
            val inbox = RequestsInbox(server.api()) { testAccount() }
            val presenter = ReportsPresenter(server.api(), { testAccount() }, inbox)
            presenter.load()
            var s = presenter.state.value
            assertEquals(ReportFilter.OPEN, s.filter)
            assertEquals(listOf(2L), s.shown.map { it.id })
            assertEquals(mapOf(ReportFilter.OPEN to 1, ReportFilter.FIXED to 1, ReportFilter.ALL to 2), s.counts)
            presenter.setFilter(ReportFilter.ALL)
            assertEquals(
                listOf(2L, 1L),
                presenter.state.value.shown
                    .map { it.id },
            )
            assertNull(presenter.fix(2, "  Got a sharper copy "))
            assertEquals("""{"reply":"Got a sharper copy"}""", server.request("POST", "/api/v1/reports/2/fix").body)
            s = presenter.state.value
            assertEquals(ReportsNoteKind.FIXED, s.note?.kind)
            assertEquals("cy", s.note?.reporter)
            assertEquals(0, s.counts[ReportFilter.OPEN])
        }

    @Test
    fun searchBodiesAndParts() {
        assertEquals(
            "{}",
            ApiJson.lenient.encodeToString(SearchBody.serializer(), searchBody(Report(titleKind = "movie", itemId = 3))),
        )
        assertEquals(2, searchBody(Report(titleKind = "show", season = 2)).season)
        assertEquals(9L, searchBody(Report(titleKind = "show", season = 2, itemId = 9)).itemId)
        assertNull(searchBody(Report(titleKind = "show", season = 2, itemId = 9)).season)
        assertEquals(4L, searchBody(Report(titleKind = "artist", albumId = 4)).albumId)
        assertEquals("S02E03 · The Mill", Report(where = "S02E03", itemName = "The Mill").partWords)
        assertEquals("Season 2", Report(where = "Season 2").partWords)
    }

    @Test
    fun reportSheetNeedsAKind() =
        runTest {
            val server = FakeServer()
            server.json("POST", "/api/v1/reports", """{"report":{"id":7,"title_id":5,"kind":"sound","state":"open"}}""", 201)
            val presenter = ReportProblemPresenter(server.api(), 5, -1, 0, 0, 0)
            presenter.send()
            assertTrue(presenter.state.value.missingKind)
            presenter.setKind(ReportKind.SOUND)
            presenter.setNote("  drifts  ")
            presenter.send()
            assertEquals(
                7L,
                presenter.state.value.sent
                    ?.id,
            )
            assertEquals("""{"title_id":5,"kind":"sound","note":"drifts"}""", server.request("POST", "/api/v1/reports").body)
        }
}
