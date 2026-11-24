package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsLinkKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsPartsPresenter
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsRun
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsStageAction
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Role
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsControlsPartsTest {
    private val parts =
        """{"parts":[{"part":"movies","name":"Movies","on":true,"state":"running"},
            {"part":"shows","name":"TV shows","on":true,"state":"running"},
            {"part":"music","name":"Music","on":false,"state":"off","reason":"turned off by Ana"},
            {"part":"subtitles","name":"Subtitles","on":true,"state":"retrying","reason":"waiting for the server"}],
            "safe_mode":false,"safe_screens":[],"stages":["backups","downloads"],"screens":{"/":"core"}}"""

    private val states =
        """{"safe_mode":false,"states":[
            {"key":"stage:backups","kind":"stage","name":"backups","state":"running","reason":"","stuck":false,"restarts":0},
            {"key":"stage:downloads","kind":"stage","name":"downloads","state":"off","reason":"paused by Ana","stuck":false,"restarts":1},
            {"key":"stage:workers","kind":"stage","name":"workers","state":"running","stuck":true},
            {"key":"link:downloader:3","kind":"downloader","name":"qBittorrent","state":"failed","reason":"connection refused"},
            {"key":"link:source:7","kind":"source","name":"Jackett","state":"running"},
            {"key":"part:movies","kind":"part","name":"Movies","state":"running"}]}"""

    private val events =
        """{"events":[{"ID":9,"At":"2026-11-24T08:00:00Z","Kind":"control_used","Summary":"The owner turned Music off"}],"total":1,
            "limit":15,"offset":0}"""

    private fun admin() = testAccount(Role.ADMIN, listOf("manage_library", "settings"))

    private class Setup(
        val server: FakeServer,
        val presenter: SettingsControlsPartsPresenter,
        val live: MutableSharedFlow<LiveEvent>,
        val reauths: MutableList<Pair<String, String?>>,
    )

    private fun setup(
        account: ch.orellbuehler.acolyte.session.Account = admin(),
        reauthError: ApiError? = null,
    ): Setup {
        val server = FakeServer()
        server.json("GET", "/api/v1/parts", parts)
        server.json("GET", "/api/v1/health/states", states)
        server.json("GET", "/api/v1/events", events)
        val live = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 16)
        val client = server.client(listOf(FakeServer.SESSION), csrf = "token")
        val reauths = mutableListOf<Pair<String, String?>>()
        val presenter =
            SettingsControlsPartsPresenter(
                Api({ client }, live),
                { account },
                { password, code ->
                    reauths += password to code
                    reauthError
                },
                { },
            )
        return Setup(server, presenter, live, reauths)
    }

    private fun askJson(
        id: String,
        what: String,
    ) =
        """{"confirm":"$id","expires_in_s":60,"text":"To $what, send the same request again within 60 seconds with {\"confirm\": \"$id\"}."}"""

    @Test
    fun loadsPartsStagesLinksAndEvents() =
        runTest {
            val s = setup()
            s.presenter.load()
            val state = s.presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertTrue(state.allowed)
            assertFalse(state.safeMode)
            assertEquals(listOf("movies", "shows", "music", "subtitles"), state.parts.map { it.part })
            assertEquals(listOf("Movies", "TV shows", "Music", "Subtitles"), state.parts.map { it.name })
            assertEquals(SettingsControlsRun.OFF, state.parts[2].run)
            assertEquals(Tone.IDLE, state.parts[2].tone)
            assertEquals("turned off by Ana", state.parts[2].reason)
            assertEquals(SettingsControlsRun.RETRYING, state.parts[3].run)
            assertEquals(Tone.WAIT, state.parts[3].tone)
            assertEquals(listOf("backups", "downloads", "workers"), state.stages.map { it.stage })
            assertEquals(listOf(true, true, false), state.stages.map { it.pausable })
            assertEquals(listOf(false, true, false), state.stages.map { it.paused })
            assertEquals(SettingsControlsRun.STUCK, state.stages[2].run)
            assertEquals(listOf("link:downloader:3", "link:source:7"), state.links.map { it.key })
            assertEquals(SettingsControlsLinkKind.DOWNLOADER, state.links[0].kind)
            assertEquals(SettingsControlsRun.FAILED, state.links[0].run)
            assertTrue(state.links.all { it.canTest })
            assertEquals(listOf("The owner turned Music off"), state.events.map { it.summary })
            val query = s.server.request("GET", "/api/v1/events").query
            assertTrue(query.contains("kind=control_used") && query.contains("limit=15"), query)
        }

    @Test
    fun peopleWithoutAdminRightsGetNothing() =
        runTest {
            val s = setup(testAccount(Role.MEMBER, listOf("manage_library", "settings")))
            s.presenter.load()
            assertFalse(s.presenter.state.value.allowed)
            assertEquals(Phase.READY, s.presenter.state.value.status.phase)
            assertEquals(0, s.server.recorded().size)
        }

    @Test
    fun theFirstFailureShowsAnErrorAndLaterOnesKeepTheData() =
        runTest {
            val s = setup()
            s.server.json("GET", "/api/v1/health/states", """{"error":"boom"}""", 500)
            s.presenter.load()
            assertEquals(Phase.FAILED, s.presenter.state.value.status.phase)
            s.server.json("GET", "/api/v1/health/states", states)
            s.presenter.refresh()
            assertEquals(Phase.READY, s.presenter.state.value.status.phase)
            s.server.json("GET", "/api/v1/parts", """{"error":"boom"}""", 500)
            s.presenter.refresh()
            assertEquals(Phase.READY, s.presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.SERVER,
                s.presenter.state.value.status.error
                    ?.code,
            )
            assertEquals(4, s.presenter.state.value.parts.size)
        }

    @Test
    fun safeModeComesFromTheAnswer() =
        runTest {
            val s = setup()
            s.server.json("GET", "/api/v1/parts", """{"parts":[],"safe_mode":true,"safe_screens":["/library"],"stages":[]}""")
            s.presenter.load()
            assertTrue(s.presenter.state.value.safeMode)
        }

    @Test
    fun turningAPartOnNeedsNoConfirm() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/parts/music/on", parts)
            s.presenter.turnOn("music")
            val state = s.presenter.state.value
            assertNull(state.ask)
            val notice = assertNotNull(state.notice)
            assertEquals(SettingsControlsKind.PART_ON, notice.kind)
            assertEquals("Music", notice.name)
            assertTrue(notice.ok)
            assertEquals("", state.busyKey)
            val sent = s.server.request("POST", "/api/v1/parts/music/on")
            assertEquals("token", sent.headers["X-CSRF-Token"])
            assertNull(sent.body)
            assertEquals(2, s.server.recorded().count { it.path == "/api/v1/parts" })
        }

    @Test
    fun turningAPartOffAsksFirstAndSendsTheIdBack() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.on("POST", "/api/v1/parts/movies/off") { request ->
                val body = (request.body as TextContent).text
                if (body.contains("\"confirm\":\"abc\"")) {
                    respondJson(parts)
                } else {
                    respondJson(askJson("abc", "turn Movies off (its screens hide)"), 202)
                }
            }
            s.presenter.turnOff("movies")
            val ask = assertNotNull(s.presenter.state.value.ask)
            assertEquals(SettingsControlsKind.PART_OFF, ask.kind)
            assertEquals("Movies", ask.name)
            assertEquals("To turn Movies off (its screens hide).", ask.text)
            assertEquals(60, ask.expiresInSeconds)
            assertNull(s.presenter.state.value.notice)
            assertEquals("{}", s.server.request("POST", "/api/v1/parts/movies/off").body)
            s.presenter.confirm()
            val state = s.presenter.state.value
            assertNull(state.ask)
            assertEquals(SettingsControlsKind.PART_OFF, state.notice?.kind)
            assertTrue(state.notice?.ok == true)
            assertEquals("""{"confirm":"abc"}""", s.server.request("POST", "/api/v1/parts/movies/off").body)
        }

    @Test
    fun cancellingTheAskSendsNothingMore() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/parts/shows/restart", askJson("x1", "restart TV shows"), 202)
            s.presenter.restart("shows")
            assertNotNull(s.presenter.state.value.ask)
            s.presenter.cancel()
            assertNull(s.presenter.state.value.ask)
            s.presenter.confirm()
            assertEquals(1, s.server.recorded().count { it.path == "/api/v1/parts/shows/restart" })
        }

    @Test
    fun restartTellsWhenJobsWereStillRunning() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.on("POST", "/api/v1/parts/movies/restart") { request ->
                if ((request.body as TextContent).text.contains("go-1")) {
                    respondJson("""{"text":"Movies restarted. Some Jobs were still running after the wait.","still_running":2}""")
                } else {
                    respondJson(askJson("go-1", "restart Movies"), 202)
                }
            }
            s.presenter.restart("movies")
            s.presenter.confirm()
            val notice = assertNotNull(s.presenter.state.value.notice)
            assertEquals(SettingsControlsKind.PART_RESTART, notice.kind)
            assertEquals("Movies restarted. Some Jobs were still running after the wait.", notice.detail)
            assertTrue(notice.ok)
        }

    @Test
    fun aLateConfirmIdFailsWithTheServersReason() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.on("POST", "/api/v1/parts/movies/off") { request ->
                if ((request.body as TextContent).text.contains("old")) {
                    respondJson("""{"error":"that confirmation id is unknown, used, expired or for another control; ask again"}""", 409)
                } else {
                    respondJson(askJson("old", "turn Movies off"), 202)
                }
            }
            s.presenter.turnOff("movies")
            s.presenter.confirm()
            val state = s.presenter.state.value
            assertNull(state.ask)
            val notice = assertNotNull(state.notice)
            assertFalse(notice.ok)
            assertEquals(ApiErrorCode.CONFLICT, notice.error?.code)
            assertTrue(
                notice.error
                    ?.message
                    .orEmpty()
                    .contains("expired"),
            )
            assertEquals("", state.busyKey)
        }

    @Test
    fun aRefusedPartSwitchIsReportedWithItsError() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/parts/subtitles/on", """{"error":"Subtitles need Movies or TV shows"}""", 409)
            s.presenter.turnOn("subtitles")
            val notice = assertNotNull(s.presenter.state.value.notice)
            assertFalse(notice.ok)
            assertEquals("Subtitles", notice.name)
            assertEquals("Subtitles need Movies or TV shows", notice.error?.message)
        }

    @Test
    fun stagesPauseResumeAndRestartWithoutConfirm() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/stages/downloads/pause", """{"text":"The stage downloads was paused."}""")
            s.server.json("POST", "/api/v1/stages/downloads/resume", """{"text":"The stage downloads was resumed."}""")
            s.server.json("POST", "/api/v1/stages/downloads/restart", """{"text":"The stage downloads was restarted."}""")
            s.presenter.stage("downloads", SettingsControlsStageAction.PAUSE)
            assertEquals(
                SettingsControlsKind.STAGE_PAUSE,
                s.presenter.state.value.notice
                    ?.kind,
            )
            s.presenter.stage("downloads", SettingsControlsStageAction.RESUME)
            assertEquals(
                SettingsControlsKind.STAGE_RESUME,
                s.presenter.state.value.notice
                    ?.kind,
            )
            s.presenter.stage("downloads", SettingsControlsStageAction.RESTART)
            val notice = assertNotNull(s.presenter.state.value.notice)
            assertEquals(SettingsControlsKind.STAGE_RESTART, notice.kind)
            assertEquals("downloads", notice.name)
            assertEquals(3, notice.id)
            assertEquals(
                listOf("pause", "resume", "restart"),
                s.server
                    .recorded()
                    .filter { it.path.startsWith("/api/v1/stages/") }
                    .map { it.path.substringAfterLast('/') },
            )
        }

    @Test
    fun aStageThatTheServerRefusesShowsTheError() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/stages/downloads/pause", """{"error":"another control is still running"}""", 409)
            s.presenter.stage("downloads", SettingsControlsStageAction.PAUSE)
            val notice = assertNotNull(s.presenter.state.value.notice)
            assertFalse(notice.ok)
            assertEquals("another control is still running", notice.error?.message)
        }

    @Test
    fun linkedAppsReconnectAndReportWhatTheTestSaid() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/links/source/7/restart", """{"ok":true,"text":"Jackett 0.22.1.5 answered"}""")
            s.presenter.reconnect("link:source:7")
            var notice = assertNotNull(s.presenter.state.value.notice)
            assertEquals(SettingsControlsKind.LINK_TEST, notice.kind)
            assertEquals("Jackett", notice.name)
            assertTrue(notice.ok)
            assertEquals("Jackett v0.22.1", notice.detail)
            s.server.json("POST", "/api/v1/links/downloader/3/restart", """{"ok":false,"text":"connection refused"}""")
            s.presenter.reconnect("link:downloader:3")
            notice = assertNotNull(s.presenter.state.value.notice)
            assertEquals(SettingsControlsKind.LINK_RECONNECT, notice.kind)
            assertFalse(notice.ok)
            assertEquals("connection refused", notice.detail)
            assertNull(notice.error)
            s.server.json("POST", "/api/v1/links/downloader/3/restart", """{"error":"media server did not answer"}""", 502)
            s.presenter.reconnect("link:downloader:3")
            notice = assertNotNull(s.presenter.state.value.notice)
            assertFalse(notice.ok)
            assertEquals(ApiErrorCode.SERVER, notice.error?.code)
            s.presenter.reconnect("stage:backups")
            assertEquals(3, s.server.recorded().count { it.path.startsWith("/api/v1/links/") })
        }

    @Test
    fun aMediaServerAnswerReadsItsFoundLine() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/links/media_server/2/restart", """{"ok":true,"found":"KingStreamingPC 1.40.1.8227"}""")
            s.presenter.reconnect("link:media_server:2")
            assertEquals(
                "v1.40.1",
                s.presenter.state.value.notice
                    ?.detail,
            )
        }

    @Test
    fun aFreshPasswordIsAskedForAndTheControlIsRetried() =
        runTest {
            val s = setup()
            s.presenter.load()
            var allowed = false
            s.server.on("POST", "/api/v1/parts/music/on") {
                if (allowed) {
                    respondJson(parts)
                } else {
                    respondJson("""{"error":"confirm your local password again","code":"reauth"}""", 403)
                }
            }
            s.presenter.turnOn("music")
            assertTrue(s.presenter.state.value.reauth.needed)
            assertNull(s.presenter.state.value.notice)
            assertEquals("", s.presenter.state.value.busyKey)
            allowed = true
            s.presenter.confirmReauth("secret", " 123456 ")
            assertEquals<List<Pair<String, String?>>>(listOf("secret" to "123456"), s.reauths)
            assertFalse(s.presenter.state.value.reauth.needed)
            assertEquals(
                SettingsControlsKind.PART_ON,
                s.presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(2, s.server.recorded().count { it.path == "/api/v1/parts/music/on" })
        }

    @Test
    fun aWrongPasswordKeepsThePromptOpen() =
        runTest {
            val s = setup(reauthError = ApiError(ApiErrorCode.FORBIDDEN, 403, "wrong password"))
            s.presenter.load()
            s.server.json("POST", "/api/v1/parts/music/on", """{"error":"confirm","code":"reauth"}""", 403)
            s.presenter.turnOn("music")
            s.presenter.confirmReauth("nope", "")
            val reauth = s.presenter.state.value.reauth
            assertTrue(reauth.needed)
            assertEquals(ApiErrorCode.FORBIDDEN, reauth.error?.code)
            s.presenter.cancelReauth()
            assertFalse(s.presenter.state.value.reauth.needed)
        }

    @Test
    fun theHealthTopicAndAResyncReloadThePage() =
        runTest {
            val s = setup()
            s.presenter.load()
            val job = backgroundScope.launch { s.presenter.watch() }
            runCurrent()

            fun count() = s.server.recorded().count { it.path == "/api/v1/health/states" }
            var seen = count()
            s.live.emit(LiveEvent("title", JsonNull, null))
            runCurrent()
            assertEquals(seen, count())
            s.live.emit(LiveEvent("health", JsonNull, null))
            eventually { count() > seen }
            seen = count()
            s.live.emit(LiveEvent(LiveEvent.RESYNC, JsonNull, null))
            eventually { count() > seen }
            job.cancel()
        }

    @Test
    fun theTimerReloadsEveryFifteenSeconds() =
        runTest {
            val s = setup()
            s.presenter.load()
            val job = backgroundScope.launch { s.presenter.watch() }
            runCurrent()
            val before = s.server.recorded().count { it.path == "/api/v1/parts" }
            advanceTimeBy(15_500)
            eventually { s.server.recorded().count { it.path == "/api/v1/parts" } > before }
            job.cancel()
        }
}
