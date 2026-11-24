package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.home.HomePresenter
import ch.orellbuehler.acolyte.feature.home.HomeTileKind
import ch.orellbuehler.acolyte.feature.home.PictureLabel
import ch.orellbuehler.acolyte.feature.home.SetupStepKind
import ch.orellbuehler.acolyte.feature.home.epochMillisOf
import ch.orellbuehler.acolyte.nav.Section
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomePresenterTest {
    private val now = epochMillisOf("2026-11-24T12:00:00Z")!!

    private val home =
        """{"rows":["arrived","asks","new_season","soon","discover"],"owner":false,"kid":false,
            "arrived":[{"key":"a1","kind":"movie","title_id":1,"name":"Dune","line":"New movie","backdrop":"b1","at":"2026-11-24T08:00:00Z"},
                       {"key":"a2","kind":"show","title_id":2,"name":"Dark","line":"S2 · 3 new episodes","backdrop":"b2","at":"2026-11-20T08:00:00Z"}],
            "new_seasons":[{"key":"s3","kind":"show","title_id":3,"name":"Severance","line":"Season 2 · out now","poster":"p3","at":"2026-11-22"}],
            "soon":[{"key":"c4","kind":"movie","title_id":4,"name":"Soon","line":"Out Nov 26","backdrop":"b4","at":"2026-11-26"}],
            "following":[],"ask_progress":{},
            "library":{"movie":{"titles":3,"parts":0,"items":3},"show":{"titles":2,"parts":4,"items":40}}}"""

    @Test
    fun memberHome() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/home", home)
            server.json("GET", "/api/v1/auth/me", meJson(role = "member", perms = "\"ask\""))
            server.json(
                "GET",
                "/api/v1/asks",
                """{"asks":[{"id":1,"state":"asked","mine":true},{"id":2,"state":"turned_down","mine":true}]}""",
            )
            server.json("GET", "/api/v1/asks/limit", """{"no_limit":false,"movies":{"per_week":3,"left":2},"shows":{"no_limit":true}}""")
            server.json("GET", "/api/v1/titles/owned", """{"items":[{"kind":"movie","tmdb":"438631","id":1,"state":"owned"}]}""")
            val api = server.api()
            val presenter =
                HomePresenter(api, { discoverAccount(displayName = "Maya Stern") }, owned(api), 3600) { now }
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals("Maya", state.name)
            assertFalse(state.system)
            assertEquals(
                listOf(
                    HomeTileKind.ASKS,
                    HomeTileKind.LIBRARY,
                    HomeTileKind.NEW,
                    HomeTileKind.EXPLORE,
                    HomeTileKind.POPULAR,
                    HomeTileKind.SEARCH,
                ),
                state.tiles.map { it.kind },
            )
            assertEquals(Section.MOVIES, state.tiles[1].target)
            assertEquals(1, state.asks?.open)
            assertEquals(1, state.asks?.declined)
            assertEquals(listOf(PictureLabel.NEW_SEASON, PictureLabel.ARRIVED, PictureLabel.COMING_SOON), state.pictures.map { it.label })
            assertEquals("438631", state.pictures[1].card.tmdb)
            assertEquals(listOf("Dark"), state.arrived?.cards?.map { it.name })
            assertTrue(state.comingUp.isEmpty())
            assertEquals(5, state.libraryTotal)
            val joined = assertNotNull(state.joined)
            assertTrue(joined.canAsk)
            assertEquals(2, joined.limits.first().left)
            assertNull(state.attention)

            server.on("PUT", "/api/v1/my/views") { respondJson("""{"welcome.seen":"1"}""") }
            presenter.dismissWelcome()
            assertNull(presenter.state.value.joined)
            assertEquals("""{"welcome.seen":"1"}""", server.request("PUT", "/api/v1/my/views").body)
        }

    @Test
    fun managerSystem() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/home", home.replace("\"owner\":false", "\"owner\":true"))
            server.json("GET", "/api/v1/auth/me", meJson(role = "owner"))
            server.json("GET", "/api/v1/asks", """{"asks":[{"id":1,"state":"asked","poster":"p"}]}""")
            server.json("GET", "/api/v1/library-folders", """{"library_folders":[{"id":1,"free_space":1000}]}""")
            server.json(
                "GET",
                "/api/v1/downloads/view",
                """{"downloads":[{"id":5,"phase":"downloading","progress":0.5,"active":true,"speed":1000,"title":"Dune"},""" +
                    """{"id":6,"phase":"failed","title":"Heat"}]}""",
            )
            server.json(
                "GET",
                "/api/v1/health",
                """{"version":"1.3.0","uptime_s":60,"sources":[{"name":"S","state":"ok"}],"downloaders":[]}""",
            )
            server.json("GET", "/api/v1/health/checks", """{"checks":[{"id":1,"kind":"backups","key":"backups","state":"warning"}]}""")
            server.json("GET", "/api/v1/needs-decision", """{"items":[],"count":0}""")
            val api = server.api()
            val presenter = HomePresenter(api, { discoverAccount(role = "owner", perms = MANAGER_PERMS) }, owned(api), 0) { now }
            presenter.load()
            val state = presenter.state.value
            assertTrue(state.system)
            assertEquals(HomeTileKind.ATTENTION, state.tiles.first().kind)
            assertEquals(Section.DOWNLOADS, state.tiles.first().target)
            assertEquals(Tone.FAIL, state.attention?.worst)
            assertEquals(2, state.attention?.rows?.size)
            assertTrue(state.tiles.any { it.kind == HomeTileKind.DOWNLOADS })
            assertTrue(state.tiles.any { it.kind == HomeTileKind.DISK })
            assertEquals(1, state.asks?.waiting)
            val system = assertNotNull(state.systemBoard)
            assertEquals(1, system.downloading)
            assertEquals(1000.0, system.speed)
            assertEquals(0, system.downloaders?.total)
            assertEquals(1, system.sources?.working)
            assertEquals(
                listOf(SetupStepKind.FOLDER, SetupStepKind.SOURCE, SetupStepKind.DOWNLOADER, SetupStepKind.TITLE),
                state.steps.map { it.kind },
            )
            assertEquals(SetupStepKind.DOWNLOADER, state.nextStep?.kind)
        }
}
