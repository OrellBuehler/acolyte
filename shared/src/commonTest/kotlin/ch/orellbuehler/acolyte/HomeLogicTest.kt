package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.api.model.HomeAnswer
import ch.orellbuehler.acolyte.api.model.HomeAskProgress
import ch.orellbuehler.acolyte.api.model.HomeCard
import ch.orellbuehler.acolyte.api.model.HomeDecision
import ch.orellbuehler.acolyte.api.model.HomeDownload
import ch.orellbuehler.acolyte.api.model.HomeHealthPart
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.FreshKind
import ch.orellbuehler.acolyte.feature.discover.epochDay
import ch.orellbuehler.acolyte.feature.home.ArrivedTitle
import ch.orellbuehler.acolyte.feature.home.AttentionKind
import ch.orellbuehler.acolyte.feature.home.AttentionTarget
import ch.orellbuehler.acolyte.feature.home.DownloadStage
import ch.orellbuehler.acolyte.feature.home.GreetingTime
import ch.orellbuehler.acolyte.feature.home.LocalTime
import ch.orellbuehler.acolyte.feature.home.NewsKind
import ch.orellbuehler.acolyte.feature.home.UpWhen
import ch.orellbuehler.acolyte.feature.home.arrivedTitle
import ch.orellbuehler.acolyte.feature.home.attention
import ch.orellbuehler.acolyte.feature.home.downloadingNow
import ch.orellbuehler.acolyte.feature.home.epochMillisOf
import ch.orellbuehler.acolyte.feature.home.freeSpace
import ch.orellbuehler.acolyte.feature.home.greetingName
import ch.orellbuehler.acolyte.feature.home.greetingTime
import ch.orellbuehler.acolyte.feature.home.homeNews
import ch.orellbuehler.acolyte.feature.home.homeStrip
import ch.orellbuehler.acolyte.feature.home.isoOf
import ch.orellbuehler.acolyte.feature.home.partCount
import ch.orellbuehler.acolyte.feature.home.stageOf
import ch.orellbuehler.acolyte.feature.home.upLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeLogicTest {
    private val noon = epochMillisOf("2026-11-24T12:00:00Z")!!
    private val now = LocalTime(noon, 3600)

    @Test
    fun greetsByName() {
        assertEquals("Maya", greetingName("Maya Stern", "maya"))
        assertEquals("Agent Check", greetingName("", "agent-check"))
        assertEquals("Sam", greetingName("", "sam@example.com"))
        assertEquals("", greetingName("", "0f3a9c2b1d4e5f60"))
        assertEquals("", greetingName("", "test_user"))
        assertEquals(GreetingTime.MORNING, greetingTime(6))
        assertEquals(GreetingTime.AFTERNOON, greetingTime(12))
        assertEquals(GreetingTime.EVENING, greetingTime(22))
        assertEquals(GreetingTime.EVENING, greetingTime(3))
    }

    @Test
    fun readsTimes() {
        assertEquals(0L, epochMillisOf("1970-01-01T00:00:00Z"))
        assertEquals(3_600_000L, epochMillisOf("1970-01-01T02:00:00+01:00"))
        assertEquals(86_400_000L, epochMillisOf("1970-01-02"))
        assertEquals(0L, epochMillisOf("1970-01-01T00:00:00.123Z"))
        assertNull(epochMillisOf("soon"))
        assertEquals("2026-11-24", isoOf(epochDay("2026-11-24")!!))
        assertEquals("2024-02-29", isoOf(epochDay("2024-02-29")!!))
        assertEquals("2026-11-24", now.isoDay())
        assertEquals(13, now.hour)
        assertEquals(2, now.weekday)
    }

    @Test
    fun newsPicksTheMostPersonalFact() {
        val arrivedAsk = Ask(name = "Dune", state = "available", mine = true, availableAt = "2026-11-23T10:00:00Z")
        val news = homeNews(null, listOf(arrivedAsk), emptyList(), approver = false, manager = false, kid = false, now = now, seed = "sam")
        assertTrue(news?.kind in setOf(NewsKind.ASKED_ARRIVED_ANY, NewsKind.ASKED_ARRIVED_ONE))
        assertEquals("Dune", news?.title)

        val waiting = homeNews(null, listOf(Ask(state = "asked")), emptyList(), true, true, false, now, "sam")
        assertTrue(waiting?.kind in setOf(NewsKind.WAITING, NewsKind.WAITING_PAGE))
        assertEquals(1, waiting?.count)
        assertNull(homeNews(null, listOf(Ask(state = "asked")), emptyList(), true, true, true, now, "sam"))

        val home =
            HomeAnswer(
                arrived = listOf(HomeCard(titleId = 3, name = "Arrival", at = "2026-11-24T08:00:00Z")),
                askProgress = mapOf("4" to HomeAskProgress(progress = 0.9, state = "downloading")),
            )
        val fresh = homeNews(home, emptyList(), emptyList(), false, false, false, now, "sam")
        assertTrue(fresh?.kind in setOf(NewsKind.NEW_TODAY_ANY, NewsKind.NEW_TODAY_ONE))
        val finishing = homeNews(home.copy(arrived = emptyList()), emptyList(), emptyList(), false, false, false, now, "sam")
        assertEquals(NewsKind.MINE_FINISHING, finishing?.kind)
        assertNull(homeNews(HomeAnswer(), emptyList(), emptyList(), false, false, false, now, "sam"))
    }

    @Test
    fun downloadStages() {
        assertEquals(DownloadStage.CHECKING, stageOf(HomeDownload(clientHint = "checking", phase = "downloading")))
        assertEquals(DownloadStage.IMPORTING, stageOf(HomeDownload(phase = "downloading", progress = 1.0)))
        assertEquals(DownloadStage.DONE, stageOf(HomeDownload(phase = "failed", replaced = true)))
        assertEquals(DownloadStage.FAILED, stageOf(HomeDownload(phase = "import_refused")))
        assertEquals(DownloadStage.SEEDING, stageOf(HomeDownload(phase = "done", state = "seeding")))
        val list =
            listOf(
                HomeDownload(id = 1, phase = "downloading", progress = 0.2, active = true),
                HomeDownload(id = 2, phase = "downloading", progress = 0.7, active = true),
                HomeDownload(id = 3, phase = "downloading", progress = 0.9, active = true, clientItem = true, outside = true),
                HomeDownload(id = 4, phase = "done"),
            )
        assertEquals(listOf(2L, 1L), downloadingNow(list).map { it.id })
    }

    @Test
    fun attentionGroupsByCause() {
        val checks =
            listOf(
                HealthCheck(kind = "source", key = "source:1", subject = "Indexer", state = "error"),
                HealthCheck(kind = "backups", key = "backups", subject = "Backups", state = "ok", confirmed = "warning"),
                HealthCheck(kind = "disk_space", key = "disk:1", subject = "/data", state = "ok"),
            )
        val failed = listOf(HomeDownload(id = 1, title = "Dune", phase = "failed"), HomeDownload(id = 2, title = "Heat", phase = "failed"))
        val rows = attention(failed, listOf(HomeDecision(slotId = 5, title = "Show")), checks, admin = true)
        assertEquals(
            listOf(AttentionKind.HEALTH, AttentionKind.DOWNLOAD_FAILED, AttentionKind.DECISION, AttentionKind.HEALTH),
            rows.map {
                it.kind
            },
        )
        assertEquals(Tone.FAIL, rows[0].tone)
        assertEquals(listOf("Indexer"), rows[0].names)
        assertEquals(2, rows[1].count)
        assertEquals(AttentionTarget.DOWNLOADS, rows[1].target)
        assertEquals("backups", rows[3].checkKind)
        assertEquals(AttentionTarget.HEALTH, attention(emptyList(), emptyList(), checks, admin = false)[0].target)
    }

    @Test
    fun partsAndDisks() {
        val parts =
            listOf(
                HomeHealthPart("A", "ok"),
                HomeHealthPart("B", "disabled"),
                HomeHealthPart("C", "paused"),
                HomeHealthPart("Old (removed 3)", "ok"),
            )
        val count = partCount(parts, listOf("A"))
        assertEquals(3, count.total)
        assertEquals(1, count.off)
        assertEquals(0, count.working)
        assertEquals(listOf("C", "A"), count.troubled)
        val gb = 1024L * 1024 * 1024
        assertEquals(100 * gb, freeSpace(listOf(100 * gb, 101 * gb), false)?.free)
        assertEquals(false, freeSpace(listOf(100 * gb, 101 * gb), false)?.many)
        assertEquals(true, freeSpace(listOf(10 * gb, 500 * gb), false)?.many)
        assertNull(freeSpace(listOf(0), false))
        assertEquals(0L, freeSpace(listOf(0), true)?.free)
    }

    @Test
    fun cardLines() {
        assertEquals(FreshKind.NEW_SEASON, homeStrip(HomeCard(kind = "show", line = "Season 4 added"))?.kind)
        assertEquals(FreshKind.NEW_EPISODE, homeStrip(HomeCard(kind = "show", line = "S4 · 3 new episodes"))?.kind)
        assertNull(homeStrip(HomeCard(kind = "show", line = "Show")))
        assertEquals("Band · 2 new albums", homeStrip(HomeCard(kind = "artist", name = "Band", line = "2 new albums"))?.text)
        assertEquals(ArrivedTitle.NEW_TODAY, arrivedTitle(noon - 3_600_000, noon))
        assertEquals(ArrivedTitle.RECENTLY_ADDED, arrivedTitle(noon - 20 * 86_400_000L, noon))
        val out = upLine(HomeCard(line = "Season 5 · out now", at = "2026-11-20"), "2026-11-24")
        assertEquals(UpWhen.OUT_NOW, out.second)
        assertEquals("Season 5", out.first)
        val movie = upLine(HomeCard(line = "Out Mar 3", at = "2027-03-03"), "2026-11-24")
        assertEquals("", movie.first)
        assertEquals(UpWhen.DATE, movie.second)
        assertEquals("2027-03-03", movie.third)
        assertEquals(UpWhen.TODAY, upLine(HomeCard(line = "S2E5 · Nov 24", at = "2026-11-24T20:00:00Z"), "2026-11-24").second)
    }
}
