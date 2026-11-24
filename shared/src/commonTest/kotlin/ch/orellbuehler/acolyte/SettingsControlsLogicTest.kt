package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.model.SettingsControlAnswer
import ch.orellbuehler.acolyte.api.model.SettingsLinkTest
import ch.orellbuehler.acolyte.api.model.SettingsScheduleRow
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.SettingsPage
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlOutcome
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlSteps
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsDot
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsEvery
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsEveryUnit
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsLinkKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsLogic
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsNext
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsNextKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsReason
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsRun
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsScheduleGroup
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsStale
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SettingsControlsLogicTest {
    private val now = Instant.parse("2026-11-24T10:00:00Z")

    private fun row(
        kind: String = "feed_check",
        state: String = "waiting",
        enabled: Boolean = true,
        extra: SettingsScheduleRow.() -> SettingsScheduleRow = { this },
    ): SettingsScheduleRow = SettingsScheduleRow(kind = kind, state = state, enabled = enabled).extra()

    @Test
    fun questionDropsTheServersTail() {
        val text =
            "To turn Movies off (its screens hide, its scheduled work stops), send the same request again within 60 seconds " +
                "with {\"confirm\": \"abc\"}."
        assertEquals("To turn Movies off (its screens hide, its scheduled work stops).", SettingsControlsLogic.question(text))
        assertEquals("Plain text.", SettingsControlsLogic.question("Plain text."))
    }

    @Test
    fun anAnswerWithAConfirmFieldIsTheFirstStep() {
        val ask =
            SettingsControlSteps.outcome(
                ApiResult.Ok(SettingsControlAnswer("id-1", 45, "To restart HolyArrs, send the same request again within 45 seconds.")),
            )
        assertTrue(ask is SettingsControlOutcome.Asking)
        assertEquals("id-1", ask.confirm)
        assertEquals(45, ask.expiresInSeconds)
        assertEquals("To restart HolyArrs.", ask.text)
        val defaulted = SettingsControlSteps.outcome(ApiResult.Ok(SettingsControlAnswer("id-2", 0, "x")))
        assertEquals(60, (defaulted as SettingsControlOutcome.Asking).expiresInSeconds)
        val done = SettingsControlSteps.outcome(ApiResult.Ok(SettingsControlAnswer(text = "Movies restarted.", stillRunning = 2)))
        assertEquals(SettingsControlOutcome.Done("Movies restarted.", 2), done)
    }

    @Test
    fun linksAreReadFromStateKeys() {
        val link = SettingsControlsLogic.link("link:downloader:3")
        assertEquals("downloader", link?.kind)
        assertEquals("3", link?.id)
        assertEquals("media_server", SettingsControlsLogic.link("link:media_server:12")?.kind)
        assertNull(SettingsControlsLogic.link("link:plugin:3"))
        assertNull(SettingsControlsLogic.link("stage:backups"))
        assertEquals(SettingsControlsLinkKind.SUBTITLE_SITE, SettingsControlsLogic.linkKind("subtitle_site"))
        assertEquals(SettingsControlsLinkKind.OTHER, SettingsControlsLogic.linkKind("x"))
    }

    @Test
    fun runStatesAndTones() {
        assertEquals(SettingsControlsRun.STUCK, SettingsControlsLogic.run("running", stuck = true))
        assertEquals(SettingsControlsRun.OFF, SettingsControlsLogic.run("off", stuck = false))
        assertEquals(SettingsControlsRun.RETRYING, SettingsControlsLogic.run("retrying", stuck = false))
        assertEquals(Tone.OK, SettingsControlsLogic.tone(SettingsControlsRun.RUNNING))
        assertEquals(Tone.FAIL, SettingsControlsLogic.tone(SettingsControlsRun.FAILED))
        assertEquals(Tone.IDLE, SettingsControlsLogic.tone(SettingsControlsRun.OFF))
        assertEquals(Tone.WAIT, SettingsControlsLogic.tone(SettingsControlsRun.STARTING))
        assertEquals(Tone.WAIT, SettingsControlsLogic.tone(SettingsControlsRun.STUCK))
    }

    @Test
    fun testLabelNamesTheAppAndItsVersion() {
        assertEquals("qBittorrent v5.1.2", SettingsControlsLogic.testLabel("qBittorrent v5.1.2.14 is ready"))
        assertEquals("Sonarr v4.0.1", SettingsControlsLogic.testLabel("Sonarr 4.0.1.929"))
        assertEquals("Plex v1.40.1", SettingsControlsLogic.testLabel("KingStreamingPC Plex 1.40.1.8227"))
        assertEquals("", SettingsControlsLogic.testLabel("connected to http://10.0.0.5:8080/api"))
        assertEquals("v2.1", SettingsControlsLogic.testLabel("version 2.1"))
        assertEquals("", SettingsControlsLogic.testLabel("host-1.2.3.4"))
        assertEquals("aria2", SettingsControlsLogic.testApp("aria 1"))
    }

    @Test
    fun linkAnswersPickTheFirstPresentLine() {
        assertEquals("a", SettingsControlsLogic.linkSaid(SettingsLinkTest(text = "a", message = "b")))
        assertEquals("b", SettingsControlsLogic.linkSaid(SettingsLinkTest(message = "b", found = "c")))
        assertEquals("c", SettingsControlsLogic.linkSaid(SettingsLinkTest(found = "c")))
        assertEquals("", SettingsControlsLogic.linkSaid(SettingsLinkTest()))
    }

    @Test
    fun sizesAndDurations() {
        assertEquals("0 B", SettingsControlsLogic.bytes(0))
        assertEquals("512 B", SettingsControlsLogic.bytes(512))
        assertEquals("1.5 KB", SettingsControlsLogic.bytes(1536))
        assertEquals("12 MB", SettingsControlsLogic.bytes(12L * 1024 * 1024))
        assertEquals("2.0 GB", SettingsControlsLogic.bytes(2L * 1024 * 1024 * 1024))
        assertEquals("5s", SettingsControlsLogic.duration(4.6))
        assertEquals("3m", SettingsControlsLogic.duration(200.0))
        assertEquals("1h 5m", SettingsControlsLogic.duration(3900.0))
        assertEquals("2d 3h", SettingsControlsLogic.duration((51 * 3600).toDouble()))
        assertEquals("", SettingsControlsLogic.duration(-1.0))
    }

    @Test
    fun howOftenUsesTheLargestWholeUnit() {
        assertEquals(SettingsControlsEvery(SettingsControlsEveryUnit.ALWAYS, 0), SettingsControlsLogic.every(0))
        assertEquals(SettingsControlsEvery(SettingsControlsEveryUnit.DAYS, 1), SettingsControlsLogic.every(86400))
        assertEquals(SettingsControlsEvery(SettingsControlsEveryUnit.HOURS, 36), SettingsControlsLogic.every(36 * 3600L))
        assertEquals(SettingsControlsEvery(SettingsControlsEveryUnit.MINUTES, 90), SettingsControlsLogic.every(5400))
        assertEquals(SettingsControlsEvery(SettingsControlsEveryUnit.SECONDS, 45), SettingsControlsLogic.every(45))
    }

    @Test
    fun intervalsAreEditedInHoursOrMinutes() {
        assertEquals(SettingsControlsUnit.HOURS, SettingsControlsLogic.intervalUnit(3600))
        assertEquals(SettingsControlsUnit.MINUTES, SettingsControlsLogic.intervalUnit(60))
        assertEquals(SettingsControlsUnit.MINUTES, SettingsControlsLogic.intervalUnit(300))
        assertEquals(3600L, SettingsControlsLogic.unitSize(SettingsControlsUnit.HOURS))
        assertEquals(60L, SettingsControlsLogic.unitSize(SettingsControlsUnit.MINUTES))
    }

    @Test
    fun intervalValidationFollowsMinMaxAndStep() {
        val min = 6 * 3600L
        val max = 7 * 86400L
        val step = 3600L
        assertTrue(SettingsControlsLogic.intervalValid(6 * 3600L, min, max, step))
        assertTrue(SettingsControlsLogic.intervalValid(max, min, max, step))
        assertFalse(SettingsControlsLogic.intervalValid(5 * 3600L, min, max, step))
        assertFalse(SettingsControlsLogic.intervalValid(max + step, min, max, step))
        assertFalse(SettingsControlsLogic.intervalValid(7 * 3600L + 60, min, max, step))
        assertTrue(SettingsControlsLogic.intervalValid(15 * 60L, 15 * 60L, 86400, 300))
        assertFalse(SettingsControlsLogic.intervalValid(17 * 60L, 15 * 60L, 86400, 300))
    }

    @Test
    fun everyKindIsGroupedAndKnowsItsPage() {
        assertEquals(18, SettingsControlsLogic.kinds.size)
        assertEquals(SettingsControlsScheduleGroup.LIBRARY, SettingsControlsLogic.group("rescan_library"))
        assertEquals(SettingsControlsScheduleGroup.COPIES, SettingsControlsLogic.group("subtitle_better"))
        assertEquals(SettingsControlsScheduleGroup.METADATA, SettingsControlsLogic.group("refresh_titles"))
        assertEquals(SettingsControlsScheduleGroup.UPKEEP, SettingsControlsLogic.group("check_bridges"))
        assertEquals(SettingsControlsScheduleGroup.UPKEEP, SettingsControlsLogic.group("something_new"))
        val pages =
            mapOf(
                "check_bridges" to SettingsPage.SOURCES,
                "sync_plex_watchlists" to SettingsPage.REQUESTS,
                "feed_check" to SettingsPage.SEARCH,
                "search_missing" to SettingsPage.SEARCH,
                "health_check" to SettingsPage.THIS_APP,
                "clean_history" to SettingsPage.THIS_APP,
                "subtitle_missing" to SettingsPage.SUBTITLES,
                "subtitle_better" to SettingsPage.SUBTITLES,
            )
        for ((kind, page) in pages) assertEquals(page, SettingsControlsLogic.kinds.getValue(kind).page, kind)
        assertEquals(pages.size, SettingsControlsLogic.kinds.values.count { it.page != null })
        for ((kind, info) in SettingsControlsLogic.kinds) {
            assertTrue(info.min in 1..info.max, kind)
            assertEquals(0L, info.min % info.step, kind)
            assertEquals(0L, info.max % info.step, kind)
        }
    }

    @Test
    fun theServersPageWinsOverTheTable() {
        assertEquals(SettingsPage.SEARCH, SettingsControlsLogic.page(row("feed_check")))
        assertEquals(SettingsPage.SUBTITLES, SettingsControlsLogic.page(row("feed_check") { copy(settings = "/settings/subtitles") }))
        assertNull(SettingsControlsLogic.page(row("grab_held")))
        assertEquals("this_app", SettingsControlsLogic.page(row("health_check"))?.key)
    }

    @Test
    fun nextRunReadsLikeTheWeb() {
        fun at(seconds: Long) = Instant.fromEpochSeconds(now.epochSeconds + seconds).toString()
        assertEquals(SettingsControlsNext(SettingsControlsNextKind.NONE), SettingsControlsLogic.until("", now))
        assertEquals(SettingsControlsNext(SettingsControlsNextKind.NOW), SettingsControlsLogic.until(at(20), now))
        assertEquals(SettingsControlsNext(SettingsControlsNextKind.NOW), SettingsControlsLogic.until(at(-500), now))
        assertEquals(SettingsControlsNext(SettingsControlsNextKind.MINUTES, 5), SettingsControlsLogic.until(at(300), now))
        assertEquals(SettingsControlsNext(SettingsControlsNextKind.HOURS, 2), SettingsControlsLogic.until(at(7200), now))
        assertEquals(SettingsControlsNext(SettingsControlsNextKind.DAYS, 3), SettingsControlsLogic.until(at(3 * 86400L), now))
        assertEquals(SettingsControlsNextKind.TURNED_OFF, SettingsControlsLogic.next(row(enabled = false), now).kind)
        assertEquals(SettingsControlsNextKind.PART_OFF, SettingsControlsLogic.next(row(state = "part_off"), now).kind)
        assertEquals(SettingsControlsNextKind.SAFE_MODE, SettingsControlsLogic.next(row(state = "safe_mode"), now).kind)
        assertEquals(SettingsControlsNextKind.RUNNING, SettingsControlsLogic.next(row(state = "running"), now).kind)
        assertEquals(SettingsControlsNextKind.QUEUED, SettingsControlsLogic.next(row(state = "queued"), now).kind)
        assertEquals(
            SettingsControlsNext(SettingsControlsNextKind.MINUTES, 10),
            SettingsControlsLogic.next(row { copy(nextRun = at(600)) }, now),
        )
    }

    @Test
    fun lastSeenPrefersTheNewestThing() {
        val ran = row { copy(lastRun = "2026-11-24T09:00:00Z") }
        assertEquals("2026-11-24T09:00:00Z", SettingsControlsLogic.lastSeen(ran).at)
        val byHand = row { copy(lastRun = "2026-11-24T09:00:00Z", lastJobEnded = "2026-11-24T09:30:00Z", lastJobSeconds = 12.0) }
        val seen = SettingsControlsLogic.lastSeen(byHand)
        assertEquals("2026-11-24T09:30:00Z", seen.at)
        assertEquals(12.0, seen.tookSeconds)
        assertFalse(seen.checked)
        val scheduledAfter = row { copy(lastRun = "2026-11-24T09:45:00Z", lastJobEnded = "2026-11-24T09:30:00Z", lastJobSeconds = 12.0) }
        assertNull(SettingsControlsLogic.lastSeen(scheduledAfter).tookSeconds)
        val checked = row { copy(lastRun = "2026-11-24T09:00:00Z", lastChecked = "2026-11-24T09:50:00Z", lastSkip = "setting_off") }
        val check = SettingsControlsLogic.lastSeen(checked)
        assertTrue(check.checked)
        assertEquals("setting_off", check.why)
        assertEquals("nothing", SettingsControlsLogic.lastSeen(row { copy(lastChecked = "2026-11-24T09:50:00Z") }).why)
        assertEquals("", SettingsControlsLogic.lastSeen(row()).at)
        val noted = row { copy(lastRun = "2026-11-24T09:00:00Z", lastJobEnded = "2026-11-24T09:00:30Z", lastJobNote = "nobody") }
        assertEquals("nobody", SettingsControlsLogic.lastSeen(noted).why)
    }

    @Test
    fun skipReasonsNameThePlexWatchlistsWhenTheyApply() {
        assertEquals(SettingsControlsReason.NONE, SettingsControlsLogic.reason("feed_check", ""))
        assertEquals(SettingsControlsReason.SETTING_OFF, SettingsControlsLogic.reason("feed_check", "setting_off"))
        assertEquals(SettingsControlsReason.PLEX_OFF, SettingsControlsLogic.reason("sync_plex_watchlists", "setting_off"))
        assertEquals(SettingsControlsReason.PLEX_NOBODY, SettingsControlsLogic.reason("sync_plex_watchlists", "nobody"))
        assertEquals(SettingsControlsReason.NOTHING, SettingsControlsLogic.reason("feed_check", "nobody"))
        assertEquals(SettingsControlsReason.STARTING, SettingsControlsLogic.reason("feed_check", "starting"))
        assertEquals(SettingsControlsReason.NOTHING, SettingsControlsLogic.reason("feed_check", "whatever"))
        assertTrue(SettingsControlsLogic.reasonLinks("setting_off"))
        assertTrue(SettingsControlsLogic.reasonLinks("nobody"))
        assertFalse(SettingsControlsLogic.reasonLinks("starting"))
    }

    @Test
    fun dotsStatesAndNotes() {
        assertEquals(SettingsControlsDot.BUSY, SettingsControlsLogic.dot(row(state = "running")))
        assertEquals(SettingsControlsDot.BUSY, SettingsControlsLogic.dot(row(state = "queued")))
        assertEquals(SettingsControlsDot.FAIL, SettingsControlsLogic.dot(row { copy(lastJob = "failed") }))
        assertEquals(SettingsControlsDot.OK, SettingsControlsLogic.dot(row()))
        assertEquals(SettingsControlsDot.OFF, SettingsControlsLogic.dot(row(state = "off")))
        assertEquals(SettingsControlsDot.OFF, SettingsControlsLogic.dot(row(state = "part_off")))
        assertFalse(SettingsControlsLogic.mayRun(row(state = "part_off")))
        assertFalse(SettingsControlsLogic.mayRun(row(state = "safe_mode")))
        assertTrue(SettingsControlsLogic.mayRun(row(state = "off")))
        assertEquals(SettingsControlsStale.REFRESH_OFF, SettingsControlsLogic.stale(row("refresh_titles", enabled = false)))
        assertEquals(SettingsControlsStale.RESCAN_OFF, SettingsControlsLogic.stale(row("rescan_library", enabled = false)))
        assertEquals(SettingsControlsStale.NONE, SettingsControlsLogic.stale(row("rescan_library")))
        assertEquals(SettingsControlsStale.NONE, SettingsControlsLogic.stale(row("feed_check", enabled = false)))
        assertTrue(SettingsControlsLogic.toggleable("refresh_titles"))
        assertFalse(SettingsControlsLogic.toggleable("refresh_shows"))
    }

    @Test
    fun historyChoicesKeepAnOddCurrentValue() {
        assertEquals(listOf(0, 7, 14, 30, 90, 180, 365), SettingsControlsLogic.historyChoices(30))
        assertEquals(listOf(0, 7, 14, 30, 90, 180, 365, 45), SettingsControlsLogic.historyChoices(45))
        assertEquals(listOf(1, 2, 5, 10, 20), SettingsControlsLogic.CACHE_GB)
    }
}
