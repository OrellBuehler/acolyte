package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsDot
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsEvery
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsEveryUnit
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsNextKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsReason
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsRun
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsScheduleGroup
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsScheduleNoticeKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsSchedulesPresenter
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsStale
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsUnit
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
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
import kotlin.time.Clock
import kotlin.time.Instant

class SettingsControlsSchedulesTest {
    private fun row(
        kind: String,
        label: String,
        every: Long,
        limits: String = """"min_s":21600,"max_s":604800,"step_s":3600""",
        state: String = "waiting",
        enabled: Boolean = true,
        extra: String = "",
    ) = """{"kind":"$kind","label":"$label","about":"About $label.","every_s":$every,$limits,"enabled":$enabled,"state":"$state",
        "part":"core"$extra}"""

    private fun schedules(feedState: String = "waiting"): String {
        val rows =
            listOf(
                row(
                    "feed_check",
                    "Feed check",
                    900,
                    """"min_s":600,"max_s":7200,"step_s":60""",
                    feedState,
                    extra =
                        ""","last_run":"2026-11-24T09:50:00Z","next_run":"2026-11-24T10:05:00Z","last_job":"done",
                        "last_job_ended":"2026-11-24T09:50:20Z","last_job_s":20.4,"settings":"/settings/search"""",
                ),
                row("rescan_library", "Rescan the Library", 86400, state = "off", enabled = false),
                row(
                    "sync_plex_watchlists",
                    "Read Plex watchlists",
                    600,
                    """"min_s":180,"max_s":3600,"step_s":60""",
                    extra = ""","last_checked":"2026-11-24T09:58:00Z","last_skip":"setting_off","next_run":"2026-11-24T10:08:00Z",
                        "settings":"/settings/requests"""",
                ),
                row("refresh_titles", "Refresh movie details", 86400, state = "part_off"),
                row(
                    "subtitle_better",
                    "Look for better subtitles",
                    21600,
                    state = "running",
                    extra = ""","settings":"/settings/subtitles"""",
                ),
                row(
                    "clean_history",
                    "Clean up History",
                    86400,
                    extra = ""","last_job":"failed","last_job_error":"disk full","last_job_ended":"2026-11-24T09:00:00Z"""",
                ),
                row("mystery", "mystery", 3600, limits = """"min_s":0"""),
            )
        val loops =
            """[{"key":"stage:backups","kind":"stage","name":"backups","state":"running","every_s":3600,
                "last_pass":"2026-11-24T09:00:00Z","next_pass":"2026-11-24T10:30:00Z","part":"core"},
                {"key":"stage:sessions","kind":"stage","name":"sessions","state":"failed","reason":"database locked","part":"core"}]"""
        return """{"schedules":[${rows.joinToString(",")}],"loops":$loops,"safe_mode":false}"""
    }

    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-11-24T10:00:00Z")
        }

    private class Setup(
        val server: FakeServer,
        val presenter: SettingsControlsSchedulesPresenter,
        val live: MutableSharedFlow<LiveEvent>,
    )

    private fun setup(account: Account = testAccount(Role.ADMIN, listOf("manage_library", "settings"))): Setup {
        val server = FakeServer()
        server.json("GET", "/api/v1/schedules", schedules())
        val live = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 16)
        val client = server.client(listOf(FakeServer.SESSION), csrf = "token")
        return Setup(server, SettingsControlsSchedulesPresenter(Api({ client }, live), { account }, clock), live)
    }

    @Test
    fun loadsTheGroupsInThePagesOrder() =
        runTest {
            val s = setup()
            s.presenter.load()
            val state = s.presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertTrue(state.canRun)
            assertTrue(state.canChange)
            assertFalse(state.safeMode)
            assertEquals(
                listOf(
                    SettingsControlsScheduleGroup.LIBRARY,
                    SettingsControlsScheduleGroup.COPIES,
                    SettingsControlsScheduleGroup.METADATA,
                    SettingsControlsScheduleGroup.UPKEEP,
                ),
                state.groups.map { it.group },
            )
            assertEquals(
                listOf(
                    listOf("rescan_library"),
                    listOf("feed_check", "subtitle_better"),
                    listOf("refresh_titles"),
                    listOf("sync_plex_watchlists", "clean_history", "mystery"),
                ),
                state.groups.map { group -> group.rows.map { it.kind } },
            )
            assertEquals(listOf("Look for better subtitles"), state.running.map { it.label })
            assertTrue(state.running.single().running)
        }

    @Test
    fun aRowTellsWhenItRanAndWhenItRunsNext() =
        runTest {
            val s = setup()
            s.presenter.load()
            val rows =
                s.presenter.state.value.groups
                    .flatMap { it.rows }
                    .associateBy { it.kind }
            val feed = rows.getValue("feed_check")
            assertEquals("Feed check", feed.label)
            assertEquals("About Feed check.", feed.about)
            assertEquals(SettingsControlsEvery(SettingsControlsEveryUnit.MINUTES, 15), feed.every)
            assertEquals("2026-11-24T09:50:20Z", feed.lastAt)
            assertEquals("20s", feed.took)
            assertEquals(SettingsControlsNextKind.MINUTES, feed.next.kind)
            assertEquals(5, feed.next.count)
            assertEquals("search", feed.pageKey)
            assertEquals(SettingsControlsDot.OK, feed.dot)
            assertTrue(feed.canRun)
            assertFalse(feed.failed)
            assertEquals(SettingsControlsReason.NONE, feed.reason)
        }

    @Test
    fun offPartOffAndFailedRowsSayWhy() =
        runTest {
            val s = setup()
            s.presenter.load()
            val rows =
                s.presenter.state.value.groups
                    .flatMap { it.rows }
                    .associateBy { it.kind }
            val rescan = rows.getValue("rescan_library")
            assertEquals(SettingsControlsNextKind.TURNED_OFF, rescan.next.kind)
            assertEquals(SettingsControlsStale.RESCAN_OFF, rescan.stale)
            assertEquals(SettingsControlsDot.OFF, rescan.dot)
            assertTrue(rescan.toggleable)
            assertFalse(rescan.enabled)
            assertTrue(rescan.canRun)
            assertEquals("", rescan.lastAt)
            val refresh = rows.getValue("refresh_titles")
            assertEquals(SettingsControlsNextKind.PART_OFF, refresh.next.kind)
            assertFalse(refresh.canRun)
            assertTrue(refresh.canChange)
            val plex = rows.getValue("sync_plex_watchlists")
            assertEquals("2026-11-24T09:58:00Z", plex.lastAt)
            assertEquals(SettingsControlsReason.PLEX_OFF, plex.reason)
            assertTrue(plex.reasonLinks)
            assertEquals("requests", plex.pageKey)
            val history = rows.getValue("clean_history")
            assertTrue(history.failed)
            assertEquals("disk full", history.failedError)
            assertEquals(SettingsControlsDot.FAIL, history.dot)
            assertEquals(SettingsControlsReason.NONE, history.reason)
            assertEquals(SettingsControlsNextKind.NONE, history.next.kind)
            assertEquals("this_app", history.pageKey)
            val mystery = rows.getValue("mystery")
            assertEquals("mystery", mystery.label)
            assertEquals("", mystery.pageKey)
        }

    @Test
    fun stagesAreListedWithTheirStates() =
        runTest {
            val s = setup()
            s.presenter.load()
            val loops = s.presenter.state.value.loops
            assertEquals(listOf("backups", "sessions"), loops.map { it.name })
            assertEquals(SettingsControlsRun.RUNNING, loops[0].run)
            assertEquals(SettingsControlsDot.OK, loops[0].dot)
            assertEquals(SettingsControlsEvery(SettingsControlsEveryUnit.HOURS, 1), loops[0].every)
            assertEquals("2026-11-24T09:00:00Z", loops[0].lastAt)
            assertEquals(SettingsControlsNextKind.MINUTES, loops[0].next.kind)
            assertEquals(30, loops[0].next.count)
            assertEquals(Tone.FAIL, loops[1].tone)
            assertEquals(SettingsControlsDot.FAIL, loops[1].dot)
            assertEquals(SettingsControlsEveryUnit.ALWAYS, loops[1].every.unit)
            assertEquals(SettingsControlsNextKind.NONE, loops[1].next.kind)
            assertEquals("database locked", loops[1].reason)
        }

    @Test
    fun theFirstFailureIsAnErrorAndLaterOnesKeepTheRows() =
        runTest {
            val s = setup()
            s.server.json("GET", "/api/v1/schedules", """{"error":"boom"}""", 500)
            s.presenter.load()
            assertEquals(Phase.FAILED, s.presenter.state.value.status.phase)
            s.server.json("GET", "/api/v1/schedules", schedules())
            s.presenter.refresh()
            s.server.json("GET", "/api/v1/schedules", """{"error":"boom"}""", 500)
            s.presenter.refresh()
            assertEquals(Phase.READY, s.presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.SERVER,
                s.presenter.state.value.status.error
                    ?.code,
            )
            assertEquals(4, s.presenter.state.value.groups.size)
        }

    @Test
    fun runNowStartsTheJobAndTellsHowItEnded() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/schedules/feed_check/run", """{"job_id":5,"already":false}""", 202)
            s.server.json("GET", "/api/v1/schedules", schedules("running"))
            s.presenter.runNow("feed_check")
            var state = s.presenter.state.value
            assertEquals(SettingsControlsScheduleNoticeKind.STARTED, state.notice?.kind)
            assertEquals("Feed check", state.notice?.label)
            assertEquals("", state.busyKind)
            assertTrue(
                state.groups
                    .flatMap { it.rows }
                    .first { it.kind == "feed_check" }
                    .spinning,
            )
            s.server.json("GET", "/api/v1/schedules", schedules("waiting"))
            s.server.json("GET", "/api/v1/jobs/5", """{"ID":5,"State":"running"}""")
            s.presenter.refresh()
            assertEquals(
                SettingsControlsScheduleNoticeKind.STARTED,
                s.presenter.state.value.notice
                    ?.kind,
            )
            s.server.json("GET", "/api/v1/jobs/5", """{"ID":5,"State":"done"}""")
            s.presenter.refresh()
            state = s.presenter.state.value
            assertEquals(SettingsControlsScheduleNoticeKind.FINISHED, state.notice?.kind)
            assertFalse(
                state.groups
                    .flatMap { it.rows }
                    .first { it.kind == "feed_check" }
                    .spinning,
            )
            s.presenter.refresh()
            assertEquals(2, s.server.recorded().count { it.path == "/api/v1/jobs/5" })
        }

    @Test
    fun aFailedJobAndAJobWithNothingToDoAreTold() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/schedules/clean_history/run", """{"job_id":8,"already":false}""", 202)
            s.server.json("GET", "/api/v1/jobs/8", """{"ID":8,"State":"failed","Error":{"code":"internal","message":"disk full"}}""")
            s.presenter.runNow("clean_history")
            val failed = assertNotNull(s.presenter.state.value.notice)
            assertEquals(SettingsControlsScheduleNoticeKind.JOB_FAILED, failed.kind)
            assertEquals("disk full", failed.detail)
            s.server.json(
                "GET",
                "/api/v1/schedules",
                schedules().replace(
                    """"last_skip":"setting_off"""",
                    """"last_skip":"setting_off","last_job_note":"setting_off"""",
                ),
            )
            s.server.json("POST", "/api/v1/schedules/sync_plex_watchlists/run", """{"job_id":9,"already":false}""", 202)
            s.server.json("GET", "/api/v1/jobs/9", """{"ID":9,"State":"done"}""")
            s.presenter.runNow("sync_plex_watchlists")
            val nothing = assertNotNull(s.presenter.state.value.notice)
            assertEquals(SettingsControlsScheduleNoticeKind.NOTHING, nothing.kind)
            assertEquals(SettingsControlsReason.PLEX_OFF, nothing.reason)
        }

    @Test
    fun aJobAlreadyInLineIsReportedAsQueued() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("POST", "/api/v1/schedules/feed_check/run", """{"job_id":4,"already":true}""", 202)
            s.server.json("GET", "/api/v1/schedules", schedules("queued"))
            s.presenter.runNow("feed_check")
            assertEquals(
                SettingsControlsScheduleNoticeKind.QUEUED,
                s.presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun runNowIsRefusedForOffPartsAndForPeopleWhoMayNotRun() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.runNow("refresh_titles")
            s.presenter.runNow("nope")
            assertEquals(0, s.server.recorded().count { it.method == "POST" })
            s.server.json("POST", "/api/v1/schedules/feed_check/run", """{"error":"its part is off"}""", 409)
            s.presenter.runNow("feed_check")
            val notice = assertNotNull(s.presenter.state.value.notice)
            assertEquals(SettingsControlsScheduleNoticeKind.START_FAILED, notice.kind)
            assertEquals(ApiErrorCode.CONFLICT, notice.error?.code)
            val member = setup(testAccount(Role.MEMBER, listOf("settings")))
            member.presenter.load()
            assertFalse(member.presenter.state.value.canRun)
            member.presenter.runNow("feed_check")
            assertEquals(0, member.server.recorded().count { it.method == "POST" })
        }

    @Test
    fun theEditorUsesMinutesForFineStepsAndChecksTheLimits() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.edit("feed_check")
            var editor = assertNotNull(s.presenter.state.value.editor)
            assertEquals(SettingsControlsUnit.MINUTES, editor.unit)
            assertEquals("15", editor.amount)
            assertEquals(10, editor.minAmount)
            assertEquals(120, editor.maxAmount)
            assertTrue(editor.valid)
            assertEquals("search", editor.pageKey)
            assertFalse(editor.toggleable)
            s.presenter.setAmount("5x")
            editor = assertNotNull(s.presenter.state.value.editor)
            assertEquals("5", editor.amount)
            assertFalse(editor.valid)
            s.presenter.setAmount("")
            assertFalse(assertNotNull(s.presenter.state.value.editor).valid)
            s.presenter.setAmount("121")
            assertFalse(assertNotNull(s.presenter.state.value.editor).valid)
            s.presenter.setAmount("120")
            assertTrue(assertNotNull(s.presenter.state.value.editor).valid)
            s.presenter.stepAmount(up = true)
            assertEquals("120", assertNotNull(s.presenter.state.value.editor).amount)
            s.presenter.stepAmount(up = false)
            assertEquals("119", assertNotNull(s.presenter.state.value.editor).amount)
            assertTrue(
                s.presenter.state.value.groups
                    .flatMap { it.rows }
                    .first { it.kind == "feed_check" }
                    .editing,
            )
            s.presenter.closeEditor()
            assertNull(s.presenter.state.value.editor)
        }

    @Test
    fun theEditorUsesHoursForWholeHourSteps() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.edit("rescan_library")
            val editor = assertNotNull(s.presenter.state.value.editor)
            assertEquals(SettingsControlsUnit.HOURS, editor.unit)
            assertEquals("24", editor.amount)
            assertEquals(6, editor.minAmount)
            assertEquals(168, editor.maxAmount)
            assertTrue(editor.toggleable)
            assertFalse(editor.enabled)
            s.presenter.setAmount("6")
            s.presenter.stepAmount(up = false)
            assertEquals("6", assertNotNull(s.presenter.state.value.editor).amount)
            s.presenter.stepAmount(up = true)
            assertEquals("7", assertNotNull(s.presenter.state.value.editor).amount)
        }

    @Test
    fun savingTheIntervalSendsSecondsAndClosesTheEditor() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.edit("feed_check")
            s.presenter.setAmount("30")
            s.server.json("PUT", "/api/v1/schedules/feed_check", row("feed_check", "Feed check", 1800))
            s.presenter.saveEditor()
            assertEquals("""{"every_s":1800}""", s.server.request("PUT", "/api/v1/schedules/feed_check").body)
            val state = s.presenter.state.value
            assertNull(state.editor)
            val notice = assertNotNull(state.notice)
            assertEquals(SettingsControlsScheduleNoticeKind.SAVED, notice.kind)
            assertEquals(SettingsControlsEvery(SettingsControlsEveryUnit.MINUTES, 30), notice.every)
            assertEquals(2, s.server.recorded().count { it.path == "/api/v1/schedules" })
        }

    @Test
    fun anInvalidIntervalIsNotSentAndARefusedOneStaysInTheEditor() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.edit("feed_check")
            s.presenter.setAmount("5")
            s.presenter.saveEditor()
            assertEquals(0, s.server.recorded().count { it.method == "PUT" })
            s.presenter.setAmount("20")
            s.server.json(
                "PUT",
                "/api/v1/schedules/feed_check",
                """{"error":"every_s must be 10 minutes to 2 hours in steps of 1 minute"}""",
                400,
            )
            s.presenter.saveEditor()
            val editor = assertNotNull(s.presenter.state.value.editor)
            assertEquals(ApiErrorCode.BAD_REQUEST, editor.error?.code)
            assertFalse(editor.saving)
            s.presenter.setAmount("21")
            assertNull(assertNotNull(s.presenter.state.value.editor).error)
        }

    @Test
    fun peopleWhoMayNotChangeCannotOpenTheEditor() =
        runTest {
            val s = setup(testAccount(Role.MEMBER, listOf("manage_library")))
            s.presenter.load()
            assertFalse(s.presenter.state.value.canChange)
            s.presenter.edit("feed_check")
            assertNull(s.presenter.state.value.editor)
        }

    @Test
    fun theTableFillsInWhatTheServerLeavesOut() =
        runTest {
            val s = setup()
            s.server.json(
                "GET",
                "/api/v1/schedules",
                """{"schedules":[{"kind":"feed_check","every_s":900,"enabled":true,"state":"waiting"}],"loops":null}""",
            )
            s.presenter.load()
            val row =
                s.presenter.state.value.groups
                    .single()
                    .rows
                    .single()
            assertEquals("Feed check", row.label)
            assertEquals("Reads the newest Releases of every Source with a feed.", row.about)
            s.presenter.edit("feed_check")
            val editor = assertNotNull(s.presenter.state.value.editor)
            assertEquals(10, editor.minAmount)
            assertEquals(120, editor.maxAmount)
        }

    @Test
    fun automaticRescanAndRefreshCanBeTurnedOnAndOff() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("PUT", "/api/v1/schedules/rescan_library", row("rescan_library", "Rescan the Library", 86400))
            s.presenter.setEnabled("rescan_library", true)
            assertEquals("""{"enabled":true}""", s.server.request("PUT", "/api/v1/schedules/rescan_library").body)
            assertEquals(
                SettingsControlsScheduleNoticeKind.AUTO_ON,
                s.presenter.state.value.notice
                    ?.kind,
            )
            s.server.json("PUT", "/api/v1/schedules/refresh_titles", row("refresh_titles", "Refresh movie details", 86400))
            s.presenter.setEnabled("refresh_titles", false)
            assertEquals(
                SettingsControlsScheduleNoticeKind.AUTO_OFF,
                s.presenter.state.value.notice
                    ?.kind,
            )
            s.presenter.setEnabled("feed_check", false)
            assertEquals(2, s.server.recorded().count { it.method == "PUT" })
            s.server.json("PUT", "/api/v1/schedules/rescan_library", """{"error":"only scheduled movie details refresh"}""", 409)
            s.presenter.setEnabled("rescan_library", false)
            val notice = assertNotNull(s.presenter.state.value.notice)
            assertEquals(SettingsControlsScheduleNoticeKind.SAVE_FAILED, notice.kind)
            assertEquals("", s.presenter.state.value.busyKind)
        }

    @Test
    fun jobTopicsAndResyncReloadThePageAndTheTimerRuns() =
        runTest {
            val s = setup()
            s.presenter.load()
            val job = backgroundScope.launch { s.presenter.watch() }
            runCurrent()

            fun count() = s.server.recorded().count { it.path == "/api/v1/schedules" }
            var seen = count()
            s.live.emit(LiveEvent("health", JsonNull, null))
            runCurrent()
            assertEquals(seen, count())
            s.live.emit(LiveEvent("job", JsonNull, null))
            eventually { count() > seen }
            seen = count()
            s.live.emit(LiveEvent(LiveEvent.RESYNC, JsonNull, null))
            eventually { count() > seen }
            seen = count()
            advanceTimeBy(30_500)
            eventually { count() > seen }
            job.cancel()
        }
}
