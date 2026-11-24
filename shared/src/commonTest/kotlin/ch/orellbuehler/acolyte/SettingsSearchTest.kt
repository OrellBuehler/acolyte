package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchChoice
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchNumberError
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchPresenter
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchRun
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchSwitch
import ch.orellbuehler.acolyte.feature.acquisition.settingsSearchNumberError
import ch.orellbuehler.acolyte.session.Role
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSearchTest {
    private val settings =
        """{"feed_check_minutes":15,"missing_search_count":20,"missing_search_better":false,"try_next_downloader":true,
        "missing_search_shows":true,"season_pack_upgrade":"all","season_pack_prefer":"rank","multi_season_packs":false,
        "missing_search_music":true,"music_discography_packs":false,"album_track_tolerance":0,"album_extra_files":2,
        "artist_search_cap":200,"refuse_early_releases":true,"early_release_grace_hours":24,"refuse_tiny_releases":true,
        "import_min_video_mb":0,"import_min_track_kb":0,"import_size_floors":true,"hold_unknown_releases":false,
        "episode_default_minutes":20}"""

    private fun server(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/settings/search", settings)
        server.on("PUT", "/api/v1/settings/search") { respondJson((it.body as TextContent).text) }
        server.json("POST", "/api/v1/search/feed-check", """{"job_ids":[3]}""", 202)
        server.json("POST", "/api/v1/search/better", """{"job_ids":[4,5]}""", 202)
        return server
    }

    private fun presenter(
        server: FakeServer,
        role: Role = Role.OWNER,
        perms: List<String> = emptyList(),
    ) = SettingsSearchPresenter(server.api()) { testAccount(role, perms) }

    @Test
    fun loadsTheSettings() =
        runTest {
            val p = presenter(server())
            p.load()
            val s = p.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertFalse(s.dirty)
            assertEquals("20", s.number(SettingsSearchNumber.MISSING_COUNT))
            assertTrue(s.isOn(SettingsSearchSwitch.REFUSE_EARLY))
            assertEquals("rank", s.choice(SettingsSearchChoice.PACK_PREFER))
            assertEquals("2", s.number("album_extra_files"))
            assertTrue(s.canRun)
        }

    @Test
    fun theFeedMinutesAreZeroOrTenToOneTwenty() {
        val feed = SettingsSearchNumber.FEED_MINUTES
        assertNull(settingsSearchNumberError(feed, "0"))
        assertNull(settingsSearchNumberError(feed, "10"))
        assertNull(settingsSearchNumberError(feed, "120"))
        assertEquals(SettingsSearchNumberError.FEED, settingsSearchNumberError(feed, "9"))
        assertEquals(SettingsSearchNumberError.FEED, settingsSearchNumberError(feed, "121"))
        assertEquals(SettingsSearchNumberError.FEED, settingsSearchNumberError(feed, ""))
        assertNull(settingsSearchNumberError(SettingsSearchNumber.TRACK_TOLERANCE, "-1"))
        assertEquals(SettingsSearchNumberError.RANGE, settingsSearchNumberError(SettingsSearchNumber.TRACK_TOLERANCE, "-2"))
        assertEquals(SettingsSearchNumberError.RANGE, settingsSearchNumberError(SettingsSearchNumber.EPISODE_MINUTES, "0"))
        assertEquals(SettingsSearchNumberError.RANGE, settingsSearchNumberError(SettingsSearchNumber.MISSING_COUNT, "501"))
    }

    @Test
    fun editsTrackDirtyAndInvalidNumbersBlockSave() =
        runTest {
            val p = presenter(server())
            p.load()
            p.setNumber(SettingsSearchNumber.FEED_MINUTES, "5")
            assertEquals("feed", p.state.value.numberErrorKey("feed_check_minutes"))
            assertTrue(p.state.value.dirty)
            assertFalse(p.state.value.canSave)
            p.setNumber("feed_check_minutes", "15")
            assertFalse(p.state.value.dirty)
            p.setNumber("album_track_tolerance", "-1")
            assertEquals("-1", p.state.value.number("album_track_tolerance"))
            assertTrue(p.state.value.canSave)
            p.setNumber("missing_search_count", "-3")
            assertEquals("3", p.state.value.number("missing_search_count"))
            p.setSwitch("missing_search_better", true)
            p.setChoice(SettingsSearchChoice.PACK_UPGRADE, "any")
            assertTrue(p.state.value.isOn("missing_search_better"))
            p.discard()
            val s = p.state.value
            assertFalse(s.dirty)
            assertEquals("all", s.choice("season_pack_upgrade"))
            assertEquals("20", s.number("missing_search_count"))
        }

    @Test
    fun saveSendsTheWholeObjectIncludingTheServerOnlyFields() =
        runTest {
            val server = server()
            val p = presenter(server)
            p.load()
            p.setNumber("missing_search_count", "0")
            p.setSwitch(SettingsSearchSwitch.MULTI_SEASON_PACKS, true)
            p.save()
            val body = server.request("PUT", "/api/v1/settings/search").body.orEmpty()
            assertTrue(body.contains(""""missing_search_count":0"""))
            assertTrue(body.contains(""""multi_season_packs":true"""))
            assertTrue(body.contains(""""try_next_downloader":true"""))
            assertTrue(body.contains(""""album_extra_files":2"""))
            assertTrue(body.contains(""""artist_search_cap":200"""))
            val s = p.state.value
            assertEquals(1, s.savedCount)
            assertFalse(s.dirty)
        }

    @Test
    fun aServerRefusalShowsUnderTheForm() =
        runTest {
            val server = server()
            server.json("PUT", "/api/v1/settings/search", """{"error":"feed_check_minutes is 0 (off) or 10 to 120"}""", 400)
            val p = presenter(server)
            p.load()
            p.setSwitch("hold_unknown_releases", true)
            p.save()
            val s = p.state.value
            assertEquals(ApiErrorCode.BAD_REQUEST, s.saveError?.code)
            assertEquals("feed_check_minutes is 0 (off) or 10 to 120", s.saveError?.message)
            assertTrue(s.dirty)
            assertEquals(0, s.savedCount)
        }

    @Test
    fun theTwoRunNowActionsQueueJobsAndReportBack() =
        runTest {
            val server = server()
            val p = presenter(server)
            p.load()
            p.checkFeeds()
            var s = p.state.value
            assertEquals(SettingsSearchRun.FEEDS, s.note)
            assertEquals(1, s.noteCount)
            assertNull(s.running)
            p.findBetter()
            s = p.state.value
            assertEquals(SettingsSearchRun.BETTER, s.note)
            assertEquals(2, s.noteCount)
            assertEquals(1, server.requests.count { it.path == "/api/v1/search/feed-check" && it.method == "POST" })
            assertEquals(1, server.requests.count { it.path == "/api/v1/search/better" && it.method == "POST" })
        }

    @Test
    fun aFailedRunShowsTheServerText() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/search/feed-check", """{"error":"no sources"}""", 409)
            val p = presenter(server)
            p.load()
            p.checkFeeds()
            val s = p.state.value
            assertEquals("no sources", s.runError?.message)
            assertEquals(1, s.runErrorCount)
            assertEquals(0, s.noteCount)
            assertNull(s.running)
        }

    @Test
    fun runningNeedsManageLibraryAndTheSettingsPage() =
        runTest {
            val p = presenter(server(), Role.ADMIN, listOf("settings"))
            p.load()
            assertTrue(p.state.value.allowed)
            assertFalse(p.state.value.canRun)
            p.checkFeeds()
            assertEquals(0, p.state.value.noteCount)
            val other = presenter(server(), Role.ADMIN, listOf("manage_library"))
            other.load()
            assertFalse(other.state.value.allowed)
        }
}
