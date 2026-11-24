package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.title.AskOffer
import ch.orellbuehler.acolyte.feature.title.FeedbackKind
import ch.orellbuehler.acolyte.feature.title.HeroPill
import ch.orellbuehler.acolyte.feature.title.LibraryKind
import ch.orellbuehler.acolyte.feature.title.ReleaseEmpty
import ch.orellbuehler.acolyte.feature.title.ReleaseScope
import ch.orellbuehler.acolyte.feature.title.ReleaseScopeKind
import ch.orellbuehler.acolyte.feature.title.RemoveState
import ch.orellbuehler.acolyte.feature.title.TitleAccess
import ch.orellbuehler.acolyte.feature.title.TitleAction
import ch.orellbuehler.acolyte.feature.title.TitlePresenter
import ch.orellbuehler.acolyte.feature.title.TitleProblem
import ch.orellbuehler.acolyte.feature.title.TitleTab
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TitlePresenterTest {
    private fun account(
        role: Role = Role.OWNER,
        perms: List<String> = emptyList(),
        kid: Boolean = false,
    ) = Account(
        serverUrl = BASE,
        serverName = "media",
        method = SignInMethod.PASSWORD,
        userId = 1,
        userName = "admin",
        displayName = "Admin",
        role = role,
        permissions = perms,
        parts = emptyList(),
        serverVersion = "1.3.0",
        avatarPath = null,
        mustSetUpTwoFactor = false,
        isKid = kid,
        safeMode = false,
    )

    private val details =
        """{"tmdb_id":"603","kind":"movie","name":"The Matrix","year":1999,"genres":["Action"],"runtime_minutes":136,"rating":"R",
           "score":8.2,"votes":20000,"people":[{"name":"Lana Wachowski","role":"director","id":"9340"},
           {"name":"Keanu Reeves","role":"actor","character":"Neo","id":"6384"}],"similar":[{"tmdb_id":"604","kind":"movie","name":"Reloaded",
           "genres":[],"in_library":false,"ask":null}],"videos":[{"key":"vKQi3bBA1y8","type":"Trailer","official":true,"language":"en"}],
           "dates":{"theaters":"1999-03-31"},"in_library":true,"title_id":7,"ask":null,"never_add":false,"streaming":[],"moods":[]}"""

    private val lookup =
        """{"movies":[{"name":"The Matrix","year":1999,"ids":{"tmdb":"603","imdb":"tt0133093"},"title_id":7,"never_add":false,"has_4k":false}]}"""

    private val owned =
        """{"id":7,"kind":"movie","name":"The Matrix","ids":{"tmdb":"603"},"keep_up_to_date":true,"artwork":[],"genres":[],"people":[],
           "tags":[],"versions":[{"id":1,"label":"Main","main":true}],"asks":[],
           "slots":[{"id":11,"version":"Main","version_id":1,"state":"wanted","state_label":"Missing"}]}"""

    private fun FakeServer.page() {
        json("GET", "/api/v1/metadata/movie/603", details)
        json("GET", "/api/v1/titles/lookup", lookup)
        json("GET", "/api/v1/titles/7", owned)
        json(
            "GET",
            "/api/v1/events",
            """{"events":[{"ID":1,"Kind":"search_done","Summary":"x"},{"ID":2,"Kind":"release_chosen","Summary":"Chose it"}]}""",
        )
        json("GET", "/api/v1/titles/7/follow", """{"title_id":7,"following":true}""")
        json("GET", "/api/v1/extras/movie/603", """{"scores":[{"source":"imdb","value":8.7,"max":10,"votes":2000000}]}""")
    }

    @Test
    fun loadsAnOwnedMovie() =
        runTest {
            val server = FakeServer()
            server.page()
            val presenter = TitlePresenter(server.api(), "film", "603", 0, false, account(), "en")
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals("movie", state.kind)
            assertEquals("The Matrix", state.name)
            assertEquals(7L, state.owned?.id)
            assertEquals(LibraryKind.MISSING, state.libraryStatus?.kind)
            assertTrue(state.primaryRuns)
            assertTrue(state.following)
            assertEquals(listOf("Chose it"), state.history.map { it.event.summary })
            assertEquals(listOf("imdb"), state.factScores.map { it.source })
            assertEquals(8.2, state.tmdbScore)
            assertEquals("vKQi3bBA1y8", state.trailer?.key)
            assertEquals(listOf("Lana Wachowski"), state.directors.map { it.name })
            assertEquals(TitleAction.SEARCH, state.actionGroups.first().first())
            assertEquals("term=tmdb%3A603", server.request("GET", "/api/v1/titles/lookup").query)
            assertTrue(state.links.any { it.url == "https://www.imdb.com/title/tt0133093/" })
        }

    @Test
    fun memberSeesAskButtonsOnly() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/metadata/movie/603",
                details.replace("\"title_id\":7", "\"title_id\":0").replace("\"in_library\":true", "\"in_library\":false"),
            )
            server.json("GET", "/api/v1/titles/lookup", lookup.replace("\"title_id\":7,", ""))
            server.json("GET", "/api/v1/metadata/movie/603/follow", """{"tmdb_id":"603","following":false}""")
            val presenter = TitlePresenter(server.api(), "movie", "603", 0, false, account(Role.MEMBER, listOf("ask")), "en")
            presenter.load()
            val state = presenter.state.value
            assertNull(state.owned)
            assertFalse(state.canAdd)
            assertEquals(AskOffer.ASK, state.askOffer)
            assertTrue(state.actionGroups.isEmpty())
            assertEquals(HeroPill.NONE, state.pill)
        }

    @Test
    fun kidAboveLimitIsBlocked() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/metadata/movie/603", """{"error":"above the limit"}""", 403)
            val presenter = TitlePresenter(server.api(), "movie", "603", 0, false, account(Role.MEMBER, kid = true), "en")
            presenter.load()
            assertEquals(TitleProblem.BLOCKED, presenter.state.value.problem)
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
        }

    @Test
    fun libraryIdWithoutTmdbLoadsTheLegacyPage() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/titles/9", """{"id":9,"kind":"movie","name":"Home video","ids":{},"slots":[],"versions":[]}""")
            server.json("GET", "/api/v1/titles/9/people", """{"people":[]}""")
            val presenter = TitlePresenter(server.api(), "", "", 9, true, account(), "en")
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertTrue(state.legacy)
            assertEquals(TitleTab.LIBRARY, state.tab)
            assertEquals("Home video", state.name)
        }

    @Test
    fun searchNowSaysNothingQueuedForAnOffShow() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/titles/5",
                """{"id":5,"kind":"show","name":"Off","ids":{},"keep_up_to_date":false,"show":{"seasons":[]}}""",
            )
            server.json("POST", "/api/v1/titles/5/search", """{"job_ids":[]}""", 202)
            val presenter =
                TitlePresenter(server.api(server.client(listOf(FakeServer.SESSION), csrf = "c")), "", "", 5, false, account(), "en")
            presenter.load()
            presenter.searchNow()
            assertEquals(
                FeedbackKind.TURN_SHOW_ON,
                presenter.state.value.feedback
                    ?.kind,
            )
            assertEquals("{}", server.request("POST", "/api/v1/titles/5/search").body)
        }

    @Test
    fun keepToggleSendsOnlyTheChange() =
        runTest {
            val server = FakeServer()
            server.page()
            server.json(
                "PUT",
                "/api/v1/titles/7",
                """{"title":${owned.replace("\"keep_up_to_date\":true", "\"keep_up_to_date\":false")}}""",
            )
            val presenter = TitlePresenter(server.api(), "movie", "603", 0, false, account(), "en")
            presenter.load()
            presenter.toggleKeep()
            assertEquals("""{"keep_up_to_date":false}""", server.request("PUT", "/api/v1/titles/7").body)
            assertEquals(
                TitleAction.KEEP,
                presenter.state.value.actionGroups[2]
                    .first(),
            )
        }

    @Test
    fun followToggleUsesTheLibraryRoute() =
        runTest {
            val server = FakeServer()
            server.page()
            server.json("DELETE", "/api/v1/titles/7/follow", """{"title_id":7,"following":false}""")
            val presenter = TitlePresenter(server.api(), "movie", "603", 0, false, account(), "en")
            presenter.load()
            presenter.toggleFollow()
            assertFalse(presenter.state.value.following)
            assertFalse(presenter.state.value.followBusy)
        }

    @Test
    fun releasePickNeedsASecondTapWhenForced() =
        runTest {
            val server = FakeServer()
            server.page()
            server.json(
                "GET",
                "/api/v1/titles/7/releases",
                """{"releases":[{"id":40,"name":"Matrix.1999.2160p","passed":false,"outcome":"refused","needs_force":true,"reasons":[]}],
                   "total":1,"limit":100,"offset":0}""",
            )
            server.json("POST", "/api/v1/releases/40/choose", """{"release_id":40,"slot_id":11,"forced":true,"warnings":[]}""")
            val title = TitlePresenter(server.api(), "movie", "603", 0, false, account(), "en")
            title.load()
            val releases = title.releases(ReleaseScope(ReleaseScopeKind.WHOLE))
            releases.load()
            assertEquals(ReleaseEmpty.NO_GOOD, releases.state.value.empty)
            releases.setAllTab(true)
            val release: TitleRelease =
                releases.state.value.shown
                    .single()
            releases.pick(release)
            assertTrue(40L in releases.state.value.armed)
            assertTrue(server.requests.none { it.path == "/api/v1/releases/40/choose" })
            releases.pick(release)
            assertEquals("""{"force":true}""", server.request("POST", "/api/v1/releases/40/choose").body)
            assertEquals(
                FeedbackKind.SENT,
                releases.state.value.feedback
                    ?.kind,
            )
            assertTrue(releases.state.value.done)
            assertEquals("limit=100&offset=0", server.request("GET", "/api/v1/titles/7/releases").query)
        }

    @Test
    fun seasonReleasesSendTheSeason() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/titles/5",
                """{"id":5,"kind":"show","name":"Show","ids":{},"keep_up_to_date":true,"show":{"seasons":[]}}""",
            )
            server.json("GET", "/api/v1/titles/5/releases", """{"releases":[],"total":0,"limit":100,"offset":0}""")
            server.json("POST", "/api/v1/titles/5/search", """{"job_ids":[3]}""", 202)
            val title = TitlePresenter(server.api(), "", "", 5, false, account(), "en")
            title.load()
            val releases = title.releases(ReleaseScope(ReleaseScopeKind.SEASON, season = 2))
            releases.load()
            assertEquals(ReleaseEmpty.NO_SEARCH, releases.state.value.empty)
            releases.search()
            assertEquals("""{"season":2,"choose":false}""", server.request("POST", "/api/v1/titles/5/search").body)
            assertEquals("limit=100&offset=0&season=2", server.request("GET", "/api/v1/titles/5/releases").query)
        }

    @Test
    fun deleteRunsADryRunFirst() =
        runTest {
            val server = FakeServer()
            server.page()
            server.on("POST", "/api/v1/titles/7/delete") { request ->
                val text = (request.body as io.ktor.http.content.TextContent).text
                val applied = text.contains("\"apply\":true")
                respondJson(
                    """{"name":"The Matrix","scope":"title","applied":$applied,"held":[{"file_id":5,"path":"a.mkv"}],"kept":[],"holding_days":7}""",
                )
            }
            val title = TitlePresenter(server.api(), "movie", "603", 0, false, account(), "en")
            title.load()
            val remove = title.remove(-1, emptyList())
            remove.setDeleteFiles(true)
            assertEquals(
                """{"delete_files":true,"never_add":false,"apply":false}""",
                server.request("POST", "/api/v1/titles/7/delete").body,
            )
            assertTrue(remove.state.value.ready)
            remove.apply()
            assertTrue(remove.state.value.done)
            assertEquals(listOf(5L), remove.state.value.heldIds)
            assertEquals(
                FeedbackKind.DELETED,
                remove.state.value.feedback
                    ?.kind,
            )
            assertEquals(
                7,
                remove.state.value.feedback
                    ?.count,
            )
        }

    @Test
    fun theRemoveSheetNamesWhatItDeletes() {
        val state = RemoveState("Abbey Road", show = false, season = -1, episodeIds = emptyList(), handsOff = false)
        assertFalse(state.album)
        assertFalse(state.artist)
        assertTrue(state.copy(music = true).artist)
        val album = state.copy(albumId = 4, music = true)
        assertTrue(album.album)
        assertFalse(album.artist)
        assertFalse(album.whole)
    }

    @Test
    fun addMovieSendsTheChoices() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/metadata/movie/603", details.replace("\"title_id\":7", "\"title_id\":0"))
            server.json("GET", "/api/v1/titles/lookup", lookup.replace("\"title_id\":7,", ""))
            server.json(
                "GET",
                "/api/v1/library-folders",
                """{"library_folders":[{"id":3,"path":"/m","kind":"movie","label":"Movies","health":"ok"}]}""",
            )
            server.json("GET", "/api/v1/pick-rules", """{"pick_rules":[{"id":4,"name":"Standard","kinds":[],"preset":true}]}""")
            server.json(
                "GET",
                "/api/v1/settings/title-defaults",
                """{"movie":{"library_folder_id":3,"pick_rules_id":4,"keep_up_to_date":true,"search_on_add":true}}""",
            )
            server.json("GET", "/api/v1/settings/release-gate", """{"release_gate":"in_cinemas"}""")
            server.json("POST", "/api/v1/titles", """{"title":{"id":12,"kind":"movie","name":"The Matrix"},"job_id":1}""", 201)
            val title = TitlePresenter(server.api(), "movie", "603", 0, false, account(), "en")
            title.load()
            assertTrue(title.state.value.canAdd)
            assertTrue(title.state.value.canCheckReleases)
            val add = title.add(false, 0, null)
            add.load()
            assertEquals(3L, add.state.value.folderId)
            add.setFollow(false)
            add.submit()
            assertEquals(
                """{"id":"tmdb:603","library_folder_id":3,"pick_rules_id":4,"keep_up_to_date":true,"release_gate":"in_cinemas",""" +
                    """"search_on_add":true,"more_versions":[],"follow":false}""",
                server.request("POST", "/api/v1/titles").body,
            )
            assertEquals(12L, add.state.value.addedId)
            assertEquals(12L, title.state.value.libraryId)
        }

    @Test
    fun accessFollowsTheAccount() {
        val member = TitleAccess.of(account(Role.MEMBER, listOf("ask")), "movie")
        assertTrue(member.mayAsk)
        assertFalse(member.manage)
        assertFalse(member.mayHide)
        val manager = TitleAccess.of(account(Role.ADMIN, listOf("ask", "manage_library")), "show")
        assertTrue(manager.manage)
        assertFalse(manager.mayAsk)
        assertTrue(manager.mayHide)
    }
}
