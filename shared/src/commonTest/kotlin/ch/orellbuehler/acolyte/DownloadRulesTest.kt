package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.Decision
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.EpisodeLine
import ch.orellbuehler.acolyte.api.model.ImportRecord
import ch.orellbuehler.acolyte.api.model.ImportResult
import ch.orellbuehler.acolyte.api.model.PackRange
import ch.orellbuehler.acolyte.api.model.RestOf
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.downloads.Day
import ch.orellbuehler.acolyte.feature.downloads.Days
import ch.orellbuehler.acolyte.feature.downloads.DetailKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadFormat
import ch.orellbuehler.acolyte.feature.downloads.DownloadRules
import ch.orellbuehler.acolyte.feature.downloads.DownloadsSub
import ch.orellbuehler.acolyte.feature.downloads.DownloadsTab
import ch.orellbuehler.acolyte.feature.downloads.Month
import ch.orellbuehler.acolyte.feature.downloads.NoteKind
import ch.orellbuehler.acolyte.feature.downloads.ProblemKind
import ch.orellbuehler.acolyte.feature.downloads.SortKey
import ch.orellbuehler.acolyte.feature.downloads.SortOrder
import ch.orellbuehler.acolyte.feature.downloads.Stage
import ch.orellbuehler.acolyte.feature.downloads.StatusWord
import ch.orellbuehler.acolyte.feature.downloads.SubLineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadRulesTest {
    private fun row(
        phase: String,
        state: String = "",
        progress: Double = 0.0,
        hint: String = "",
    ) = DownloadRow(id = 1, phase = phase, state = state, progress = progress, clientHint = hint)

    @Test
    fun placesRowsOnStages() {
        assertEquals(Stage.CHECKING, DownloadRules.stageOf(row("downloading", hint = "moving")))
        assertEquals(Stage.QUEUED, DownloadRules.stageOf(row("waiting")))
        assertEquals(Stage.PAUSED, DownloadRules.stageOf(row("paused")))
        assertEquals(Stage.DOWNLOADING, DownloadRules.stageOf(row("downloading", progress = 0.4)))
        assertEquals(Stage.IMPORTING, DownloadRules.stageOf(row("downloading", progress = 1.0)))
        assertEquals(Stage.STALLED, DownloadRules.stageOf(row("stalled")))
        assertEquals(Stage.IMPORTING, DownloadRules.stageOf(row("waiting_to_import")))
        assertEquals(Stage.DECIDE, DownloadRules.stageOf(row("needs_a_decision")))
        assertEquals(Stage.FAILED, DownloadRules.stageOf(row("vanished")))
        assertEquals(Stage.DONE, DownloadRules.stageOf(row("failed").copy(replaced = true)))
        assertEquals(Stage.FAILED, DownloadRules.stageOf(row("import_refused")))
        assertEquals(Stage.SEEDING, DownloadRules.stageOf(row("done", state = "seeding")))
        assertEquals(Stage.DONE, DownloadRules.stageOf(row("done", state = "finished")))
        assertEquals(Stage.DONE, DownloadRules.stageOf(row("something_new")))
        assertTrue(DownloadRules.inHistory(row("import_refused")))
        assertFalse(DownloadRules.inHistory(row("stalled")))
    }

    @Test
    fun wordsFollowTheStage() {
        assertEquals(StatusWord.FAILED, DownloadRules.statusWord(row("vanished")))
        assertEquals(StatusWord.FAILED, DownloadRules.statusWord(row("failed").copy(replaced = true)))
        assertEquals(StatusWord.MOVING, DownloadRules.statusWord(row("downloading", hint = "moving")))
        assertEquals(StatusWord.CHECKING, DownloadRules.statusWord(row("downloading", hint = "checking")))
        assertEquals(StatusWord.REVIEW, DownloadRules.statusWord(row("needs_a_decision")))
        assertEquals(StatusWord.DONE_SEEDING, DownloadRules.statusWord(row("done").copy(seeding = true)))
        val pack = row("done").copy(pack = PackRange(1, 3, 3, 10))
        assertEquals(StatusWord.PACK_WAITING, DownloadRules.statusWord(pack))
    }

    @Test
    fun namesProblems() {
        val usenet = { text: String, phase: String -> row(phase).copy(protocol = "usenet", text = text) }
        assertEquals(ProblemKind.MISSING_ARTICLES, DownloadRules.problemOf(usenet("Articles were missing", "failed"))?.kind)
        assertEquals(Tone.FAIL, DownloadRules.problemOf(usenet("Articles were missing", "failed"))?.tone)
        assertEquals(
            ProblemKind.REPAIR_NEEDED,
            DownloadRules.problemOf(usenet("Repair failed, not enough repair blocks", "downloading"))?.kind,
        )
        assertNull(DownloadRules.problemOf(usenet("Verifying", "downloading")))
        assertEquals(ProblemKind.IMPORT_BLOCKED, DownloadRules.problemOf(row("import_refused"))?.kind)
        assertEquals(ProblemKind.TRACKER_ERROR, DownloadRules.problemOf(row("downloading").copy(trackerProblem = "error"))?.kind)
        val stopped = row("stalled").copy(text = "Every download on this downloader stopped at once.", protocol = "torrent", seeds = 2)
        assertEquals(ProblemKind.DOWNLOADER_STOPPED, DownloadRules.problemOf(stopped)?.kind)
        val copy = row("stalled").copy(availability = 0.5, protocol = "torrent", seeds = 1)
        assertEquals(RowFixtures.problem(ProblemKind.NO_FULL_COPY, Tone.FAIL), DownloadRules.problemOf(copy))
        assertEquals(ProblemKind.NO_PEERS, DownloadRules.problemOf(row("stalled").copy(protocol = "torrent"))?.kind)
        assertNull(DownloadRules.problemOf(row("paused").copy(trackerProblem = "error")))
        assertNull(DownloadRules.problemOf(row("done").copy(trackerProblem = "error")))
    }

    @Test
    fun explainsImportsAndSteps() {
        val failed =
            ImportRecord(id = 3, downloadRef = 1, state = "failed", results = listOf(ImportResult(outcome = "failed", error = "disk full")))
        assertEquals(DetailKind.IMPORT_FAILED, DownloadRules.statusDetail(row("import_refused"), failed)?.kind)
        assertEquals("disk full", DownloadRules.statusDetail(row("import_refused"), failed)?.text)
        assertEquals(DetailKind.REFUSED, DownloadRules.statusDetail(row("import_refused").copy(refusedWhy = "wrong file"), null)?.kind)
        assertEquals(DetailKind.NO_MATCH, DownloadRules.statusDetail(row("import_refused"), null)?.kind)
        assertEquals(DetailKind.UP_NEXT, DownloadRules.statusDetail(row("waiting_to_import"), null)?.kind)
        assertEquals(DetailKind.UNPACKING, DownloadRules.statusDetail(row("downloading", progress = 1.0), null)?.kind)
        val gone = ImportRecord(id = 4, downloadRef = 1, state = "failed", results = listOf(ImportResult(outcome = "gone")))
        assertEquals(DetailKind.FILES_GONE, DownloadRules.statusDetail(row("done"), gone)?.kind)
        val seeding = DownloadRules.statusDetail(row("done", state = "seeding").copy(ratio = 1.234), null)
        assertEquals(DetailKind.RATIO, seeding?.kind)
        assertEquals("1.23", seeding?.text)
        val hold =
            DownloadRules.statusDetail(
                row("done").copy(cleanupWait = listOf("S01E01", "S01E02", "S01E03", "S01E04", "S01E05", "S01E06")),
                null,
            )
        assertEquals("S01E01, S01E02, S01E03, S01E04 +2", hold?.text)
        assertEquals(NoteKind.VERIFYING, DownloadRules.stepNote(row("downloading", hint = "verifying"), null)?.kind)
        assertEquals(
            NoteKind.IMPORT_FAILED,
            DownloadRules.stepNote(row("import_refused"), DownloadRules.statusDetail(row("import_refused"), failed))?.kind,
        )
        assertEquals(NoteKind.NEXT_IN_LINE, DownloadRules.queueNote(row("waiting").copy(queuePos = 1))?.kind)
        assertEquals(3, DownloadRules.queueNote(row("waiting").copy(queuePos = 3))?.number)
    }

    @Test
    fun keepsTheNewestImportPerDownload() {
        val imports =
            listOf(ImportRecord(id = 2, downloadRef = 9), ImportRecord(id = 5, downloadRef = 9), ImportRecord(id = 1, downloadRef = 0))
        assertEquals(5L, DownloadRules.newestImports(imports)[9]?.id)
        assertEquals(1, DownloadRules.newestImports(imports).size)
    }

    @Test
    fun countsAndPicksTabs() {
        val rows =
            listOf(row("waiting"), row("paused"), row("downloading"), row("stalled"), row("failed"), row("needs_a_decision"), row("done"))
        val counts = DownloadRules.tabCounts(rows, mapOf(Stage.SEEDING to 4)).associate { it.tab to it.count }
        assertEquals(11, counts[DownloadsTab.ALL])
        assertEquals(2, counts[DownloadsTab.WAITING])
        assertEquals(2, counts[DownloadsTab.PROGRESS])
        assertEquals(2, counts[DownloadsTab.PROBLEMS])
        assertEquals(5, counts[DownloadsTab.DONE])
        assertEquals(2, DownloadRules.pick(rows, DownloadsTab.PROGRESS, DownloadsSub.DOWNLOADING).size)
        assertEquals(1, DownloadRules.pick(rows, DownloadsTab.PROBLEMS, DownloadsSub.DECIDE).size)
        assertEquals(2, DownloadRules.pick(rows, DownloadsTab.PROBLEMS, DownloadsSub.QUEUED).size)
        val subs = DownloadRules.subCounts(rows, DownloadsTab.WAITING, emptyMap()).associate { it.sub to it.count }
        assertEquals(mapOf(DownloadsSub.QUEUED to 1, DownloadsSub.PAUSED to 1), subs)
        val rest = mapOf("3" to RestOf("torrent", mapOf("seeding" to 4, "queued" to 1)), "4" to RestOf("usenet", mapOf("queued" to 2)))
        assertEquals(mapOf(Stage.QUEUED to 3, Stage.SEEDING to 4), DownloadRules.restSum(rest, ""))
        assertEquals(mapOf(Stage.QUEUED to 2), DownloadRules.restSum(rest, "usenet"))
    }

    @Test
    fun readsShowValuesOldOnesToo() {
        assertEquals(DownloadsTab.PROBLEMS to DownloadsSub.DECIDE, DownloadRules.showOf("problems.decide"))
        assertEquals(DownloadsTab.PROBLEMS to DownloadsSub.DECIDE, DownloadRules.showOf("decide"))
        assertEquals(DownloadsTab.PROGRESS to DownloadsSub.DOWNLOADING, DownloadRules.showOf("stalled"))
        assertEquals(DownloadsTab.DONE to null, DownloadRules.showOf("done.seeding"))
        assertEquals(DownloadsTab.ALL to null, DownloadRules.showOf("nonsense"))
        assertEquals(DownloadsTab.WAITING to null, DownloadRules.showOf("waiting.unknown"))
    }

    @Test
    fun namesRowsAndLines() {
        val movie = DownloadRow(title = "Heat", year = 1995, name = "Heat.1995.2160p.DV.mkv")
        assertEquals("Heat (1995)", DownloadRules.rowName(movie))
        assertEquals("1995", DownloadRules.subLine(movie).text)
        assertEquals("2160p DV", DownloadRules.quality(movie.name))
        assertEquals("1080p HDR", DownloadRules.quality("Show.S01E01.1080i.HDR10.x265"))
        assertEquals("", DownloadRules.quality("Some.Album.FLAC"))
        val album = DownloadRow(title = "Muse", kind = "artist", album = "Origin", year = 2001)
        assertEquals("Muse – Origin (2001)", DownloadRules.rowName(album))
        val one =
            DownloadRow(title = "Lost", kind = "show", episodes = listOf(EpisodeLine(season = 4, episode = 1, name = "The Beginning")))
        assertEquals("S04E01 · The Beginning", DownloadRules.subLine(one).text)
        val pack = one.copy(episodes = listOf(EpisodeLine(season = 1, episode = 1), EpisodeLine(season = 3, episode = 2)))
        assertEquals(SubLineKind.SEASONS, DownloadRules.subLine(pack).kind)
        assertEquals(3, DownloadRules.subLine(pack).to)
        assertEquals("Nameless.Release", DownloadRules.rowName(DownloadRow(name = "Nameless.Release")))
    }

    @Test
    fun searchesAndSorts() {
        val a = DownloadRow(id = 1, title = "Alpha", name = "Alpha.1080p", size = 10, progress = 0.2, startedAt = "2026-11-20T10:00:00Z")
        val b = DownloadRow(id = 2, title = "beta", name = "Beta.2160p", size = 0, progress = 0.8, startedAt = "2026-11-21T10:00:00Z")
        val c = DownloadRow(id = 3, title = "Alpha 10", name = "x", size = 30, progress = 0.5)
        assertTrue(DownloadRules.matches(b, "2160P"))
        assertFalse(DownloadRules.matches(a, "beta"))
        val rows = listOf(a, b, c)
        assertEquals(listOf(1L, 3L, 2L), DownloadRules.sortRows(rows, SortOrder(SortKey.TITLE, false), DownloadsTab.ALL).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), DownloadRules.sortRows(rows, SortOrder(SortKey.SIZE, true), DownloadsTab.ALL).map { it.id })
        assertEquals(listOf(1L, 3L, 2L), DownloadRules.sortRows(rows, SortOrder(SortKey.SIZE, false), DownloadsTab.ALL).map { it.id })
        assertEquals(listOf(2L, 3L, 1L), DownloadRules.sortRows(rows, SortOrder(SortKey.PROGRESS, true), DownloadsTab.ALL).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), DownloadRules.sortRows(rows, null, DownloadsTab.ALL).map { it.id })
        assertTrue(DownloadRules.firstDescending(SortKey.ADDED))
        assertFalse(DownloadRules.firstDescending(SortKey.TITLE))
        val done = listOf(a.copy(finishedAt = "2026-11-01T00:00:00Z"), b.copy(updatedAt = "2026-11-05T00:00:00Z"))
        assertEquals(listOf(2L, 1L), DownloadRules.sortRows(done, null, DownloadsTab.DONE).map { it.id })
    }

    @Test
    fun offersTheRightActions() {
        val qbit =
            DownloadRow(
                id = 5,
                phase = "downloading",
                state = "downloading",
                canAct = true,
                protocol = "torrent",
                downloaderKind = "qbittorrent",
                downloaderId = 2,
                hash = "abc",
                titleId = 9,
                kind = "movie",
            )
        val actions = DownloadRules.actions(qbit, canSettings = false)
        assertEquals(
            listOf(
                DownloadAction.PAUSE,
                DownloadAction.MOVE_UP,
                DownloadAction.MOVE_DOWN,
                DownloadAction.MOVE_TOP,
                DownloadAction.MOVE_BOTTOM,
                DownloadAction.OPEN_TITLE,
                DownloadAction.REMOVE,
                DownloadAction.RECHECK,
                DownloadAction.REANNOUNCE,
                DownloadAction.FORCE_START,
                DownloadAction.SEEDING_RULES,
                DownloadAction.SET_LOCATION,
                DownloadAction.SPEED_LIMITS,
                DownloadAction.MARK_FAILED,
            ),
            actions,
        )
        val outside = qbit.copy(id = 0, clientItem = true, outside = true, titleId = 0)
        assertFalse(DownloadAction.SEEDING_RULES in DownloadRules.actions(outside, canSettings = false))
        assertTrue(DownloadAction.SEEDING_RULES in DownloadRules.actions(outside, canSettings = true))
        assertFalse(DownloadAction.MARK_FAILED in DownloadRules.actions(outside, canSettings = true))
        val locked = qbit.copy(clientLocked = true)
        assertEquals(
            listOf(DownloadAction.OPEN_TITLE, DownloadAction.REMOVE, DownloadAction.MARK_FAILED),
            DownloadRules.actions(locked, false),
        )
        val failedJob =
            DownloadRow(
                phase = "failed",
                canAct = true,
                protocol = "usenet",
                downloaderKind = "sabnzbd",
                downloaderId = 3,
                hash = "SABnzbd_nzo_1",
                clientItem = true,
                outside = true,
            )
        assertEquals(listOf(DownloadAction.REMOVE), DownloadRules.actions(failedJob, canSettings = false))
        assertEquals(listOf(DownloadAction.RETRY, DownloadAction.REMOVE), DownloadRules.actions(failedJob, canSettings = true))
        val queuedJob = failedJob.copy(phase = "waiting", clientItem = false, outside = false, id = 8)
        assertTrue(DownloadAction.PRIORITY in DownloadRules.actions(queuedJob, false))
        assertTrue(DownloadAction.PAUSE in DownloadRules.actions(queuedJob, false))
    }

    @Test
    fun foldsDecisionsIntoRows() {
        val rows = listOf(DownloadRow(id = 4, phase = "done", active = false), DownloadRow(id = 5, phase = "downloading", active = true))
        val decisions =
            listOf(
                Decision(slotId = 10, downloadId = 4, title = "Heat", year = 1995),
                Decision(slotId = 11, downloadId = 5),
                Decision(slotId = 12, title = "Lost", kind = "show", episode = EpisodeLine(label = "S01E02", name = "Pilot")),
            )
        val merged = DownloadRules.withDecisions(rows, decisions)
        assertEquals("needs_a_decision", merged[0].phase)
        assertEquals("downloading", merged[1].phase)
        assertEquals(-12L, merged[2].id)
        assertEquals("S01E02 Pilot", merged[2].covers)
        assertEquals("Lost S01E02 Pilot", merged[2].name)
        assertEquals("s12", DownloadRules.keyOf(merged[2]))
        assertEquals("d4", DownloadRules.keyOf(merged[0]))
        assertEquals("c3:abc", DownloadRules.keyOf(DownloadRow(clientItem = true, downloaderId = 3, hash = "ABC")))
    }

    @Test
    fun summarisesActiveDownloads() {
        val rows =
            listOf(
                row("downloading", progress = 0.5),
                row("waiting", progress = 0.0),
                row("done", progress = 1.0),
                row("importing", progress = 1.0),
            )
        assertEquals(3 to 50, DownloadRules.summarise(rows))
        assertEquals(0 to 0, DownloadRules.summarise(listOf(row("done"))))
    }

    @Test
    fun readsLinksAndTrackers() {
        val (links, bad) = DownloadRules.splitLinks("magnet:?xt=urn:btih:abc\n\nhttps://x.example/a.torrent\nnot a link\n${"a".repeat(40)}")
        assertEquals(3, links.size)
        assertEquals(listOf("not a link"), bad)
        val (urls, wrong) = DownloadRules.splitTrackers("udp://tracker:80/announce\nftp://nope")
        assertEquals(listOf("udp://tracker:80/announce"), urls)
        assertEquals(listOf("ftp://nope"), wrong)
    }

    @Test
    fun formatsSizesLikeTheWeb() {
        assertEquals("0 B", DownloadFormat.size(0))
        assertEquals("553 MB", DownloadFormat.size(553_000_000))
        assertEquals("1.7 GB", DownloadFormat.size(1_700_000_000))
        assertEquals("1 GB", DownloadFormat.size(999_960_000))
        assertEquals("18 GB", DownloadFormat.size(18_200_000_000))
        assertEquals("14.2 TB", DownloadFormat.freeSize((14.2 * 1024 * 1024 * 1024 * 1024).toLong()))
        assertEquals("10k", DownloadFormat.swarmNumber(10_400))
        assertEquals("0.51", DownloadRules.shortRatio(0.512))
        assertEquals("12.3", DownloadRules.shortRatio(12.34))
    }

    @Test
    fun countsCivilDays() {
        assertEquals(0L, Day(1970, 1, 1).epochDay)
        assertEquals(Day(2026, 11, 24), Days.fromEpochDay(Day(2026, 11, 24).epochDay))
        assertEquals(2, Day(2026, 11, 24).weekday)
        assertEquals(Day(2027, 3, 1), Day(2027, 2, 28).plusDays(1))
        assertEquals(Day(2024, 2, 29), Day(2024, 2, 28).plusDays(1))
        assertEquals(Month(2027, 1), Month(2026, 12).plus(1))
        assertEquals(Month(2025, 12), Month(2026, 1).plus(-1))
        assertEquals(Day(2026, 11, 23), Days.weekStart(Day(2026, 11, 29)))
        assertNull(Day.parse("2026-13-01"))
        assertEquals(Day(2026, 11, 24), Day.parse("2026-11-24T23:59:00Z"))
    }
}

internal object RowFixtures {
    fun problem(
        kind: ProblemKind,
        tone: Tone,
    ) = ch.orellbuehler.acolyte.feature.downloads
        .RowProblem(kind, tone)
}
