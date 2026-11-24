package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.api.model.OutsideList
import ch.orellbuehler.acolyte.api.model.SettingsScheduleRow
import ch.orellbuehler.acolyte.api.model.SettingsStageState
import ch.orellbuehler.acolyte.api.model.SettingsStatusActiveJob
import ch.orellbuehler.acolyte.api.model.SettingsStatusAlert
import ch.orellbuehler.acolyte.api.model.SettingsStatusInstall
import ch.orellbuehler.acolyte.api.model.SettingsStatusProgress
import ch.orellbuehler.acolyte.api.model.SettingsUpdateInfo
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.status.HistoryRun
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusCheck
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusCheckKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDiskKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDiskLevel
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDue
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDueKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusFix
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusGroup
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusHeadlineKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusInput
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMarker
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusPhase
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusReason
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRow
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRules
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusStanding
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusSummaryKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusTitle
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusUnit
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusUpdates
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusWord
import ch.orellbuehler.acolyte.feature.settings.status.settingsStatusUptime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-11-24T12:00:00Z").toEpochMilliseconds()

private fun state(
    key: String,
    kind: String,
    name: String,
    state: String = "running",
    reason: String = "",
    stuck: Boolean = false,
    since: String = "2026-11-20T10:00:00Z",
) = SettingsStageState(key = key, kind = kind, name = name, state = state, reason = reason, stuck = stuck, since = since)

private fun check(
    key: String,
    kind: String,
    subject: String,
    state: String = "ok",
    message: String = "",
) = HealthCheck(id = 1, key = key, kind = kind, subject = subject, state = state, message = message, checkedAt = "2026-11-24T11:00:00Z")

private fun schedule(
    kind: String = "job",
    enabled: Boolean = true,
    state: String = "waiting",
    lastRun: String = "",
    nextRun: String = "",
    lastJob: String = "",
    lastJobEnded: String = "",
    lastJobError: String = "",
    lastChecked: String = "",
    lastSkip: String = "",
) = SettingsScheduleRow(
    kind = kind,
    label = "Job $kind",
    enabled = enabled,
    state = state,
    lastRun = lastRun,
    nextRun = nextRun,
    lastJob = lastJob,
    lastJobEnded = lastJobEnded,
    lastJobError = lastJobError,
    lastChecked = lastChecked,
    lastSkip = lastSkip,
)

class SettingsStatusRulesTest {
    @Test
    fun linkedAppStatesMapToTonesAndWords() {
        assertEquals(Tone.FAIL to SettingsStatusWord.STUCK, SettingsStatusRules.linkTone("running", true))
        assertEquals(Tone.OK to SettingsStatusWord.WORKING, SettingsStatusRules.linkTone("running", false))
        assertEquals(Tone.IDLE to SettingsStatusWord.STARTING, SettingsStatusRules.linkTone("starting", false))
        assertEquals(Tone.WAIT to SettingsStatusWord.RETRYING, SettingsStatusRules.linkTone("retrying", false))
        assertEquals(Tone.FAIL to SettingsStatusWord.BROKEN, SettingsStatusRules.linkTone("failed", false))
        assertEquals(Tone.IDLE to SettingsStatusWord.OFF, SettingsStatusRules.linkTone("off", false))
        assertEquals(Tone.IDLE to SettingsStatusWord.OFF, SettingsStatusRules.linkTone("anything", false))
    }

    @Test
    fun linkIdsAndNoTime() {
        assertEquals(3L, SettingsStatusRules.linkId("link:source:3"))
        assertNull(SettingsStatusRules.linkId("stage:alerts"))
        assertNull(SettingsStatusRules.linkId("link:source:0"))
        assertEquals("", SettingsStatusRules.realTime("0001-01-01T00:00:00Z"))
        assertEquals("2026-11-24T00:00:00Z", SettingsStatusRules.realTime("2026-11-24T00:00:00Z"))
    }

    @Test
    fun linkedRowsKeepKindsAndOfferChecksOnlyForSourcesAndDownloaders() {
        val rows =
            SettingsStatusRules.build(
                SettingsStatusInput(
                    states =
                        listOf(
                            state("stage:alerts", "stage", "Alerts"),
                            state("part:movies", "part", "Movies"),
                            state("link:source:1", "source", "Indexer"),
                            state("link:downloader:2", "downloader", "qBit", "failed", "Connection refused"),
                            state("link:media_server:4", "media_server", "Plex", "retrying"),
                            state("link:subtitle_site:5", "subtitle_site", "Subs", "starting", since = "0001-01-01T00:00:00Z"),
                        ),
                ),
            )
        assertEquals(listOf("link:source:1", "link:downloader:2", "link:media_server:4", "link:subtitle_site:5"), rows.map { it.key })
        val source = rows[0]
        assertEquals(SettingsStatusGroup.SOURCES, source.group)
        assertEquals(SettingsStatusFix.SOURCES, source.fix)
        assertEquals(SettingsStatusCheckKind.SOURCE, source.check?.kind)
        assertEquals(1L, source.check?.id)
        val downloader = rows[1]
        assertEquals(Tone.FAIL, downloader.tone)
        assertEquals(SettingsStatusReason.TEXT, downloader.reasonKind)
        assertEquals("Connection refused", downloader.reason)
        assertEquals(SettingsStatusCheckKind.DOWNLOADER, downloader.check?.kind)
        assertNull(rows[2].check)
        assertEquals(SettingsStatusFix.MEDIA_SERVERS, rows[2].fix)
        assertEquals("", rows[3].at)
        assertEquals(SettingsStatusFix.SUBTITLES, rows[3].fix)
    }

    @Test
    fun aRunningDownloaderWithAWarningCheckWarns() {
        val rows =
            SettingsStatusRules.build(
                SettingsStatusInput(
                    states = listOf(state("link:downloader:2", "downloader", "qBit"), state("link:downloader:3", "downloader", "Sab")),
                    checks = listOf(check("downloader:2", "downloader", "qBit", "warning", "Every download stopped")),
                ),
            )
        val warned = rows.first { it.key == "link:downloader:2" }
        assertEquals(Tone.WAIT, warned.tone)
        assertEquals(SettingsStatusWord.WARNING, warned.word)
        assertEquals("Every download stopped", warned.reason)
        assertEquals(Tone.OK, rows.first { it.key == "link:downloader:3" }.tone)
        assertTrue(rows.none { it.key == "downloader:2" })
    }

    @Test
    fun healthChecksBecomeStorageUpdateAndDownloaderRows() {
        val rows =
            SettingsStatusRules.build(
                SettingsStatusInput(
                    checks =
                        listOf(
                            check("library_folder:1", "library_folder", "/media/movies", "error", "Folder is missing"),
                            check("disk_space:1", "disk_space", "/media/movies", "warning", "Only 500 MB free"),
                            check("backups", "backups", "Backups"),
                            check("backups:key", "backups", "Backups", "ok", "Recommended: download your key kit"),
                            check("update", "update", "HolyArrs 1.3.0"),
                            check("config_folder", "config_folder", "Config folder", "error", "mapped wrongly"),
                            check("waiting_downloads", "waiting_downloads", "Downloads", "warning", "3 wait"),
                            check("save_folder:2", "save_folder", "qBit", "error", "missing"),
                            check("no_downloader:usenet", "no_downloader", "usenet", "warning", "none"),
                            check("same_downloader:3", "same_downloader", "Sab", "warning", "twice"),
                            check("same_downloader:4", "same_downloader", "Sab", "ok"),
                            check("trailers", "trailers", "Trailer reader"),
                            check("source:1", "source", "Indexer"),
                        ),
                ),
            )
        assertEquals(
            listOf(
                "library_folder:1",
                "disk_space:1",
                "backups",
                "backups:key",
                "update",
                "config_folder",
                "waiting_downloads",
                "save_folder:2",
                "no_downloader:usenet",
                "same_downloader:3",
            ),
            rows.map { it.key },
        )
        val folder = rows[0]
        assertEquals(SettingsStatusGroup.LIBRARY, folder.group)
        assertEquals(SettingsStatusTitle.FOLDER, folder.title)
        assertEquals("/media/movies", folder.name)
        assertEquals(Tone.FAIL, folder.tone)
        assertEquals(SettingsStatusWord.BROKEN, folder.word)
        assertEquals(SettingsStatusFix.LIBRARY_FOLDERS, folder.fix)
        assertEquals(SettingsStatusCheckKind.CHECKS, folder.check?.kind)
        assertEquals("2026-11-24T11:00:00Z", folder.at)
        assertEquals(Tone.WAIT, rows[1].tone)
        assertEquals(SettingsStatusWord.WARNING, rows[1].word)
        assertEquals(SettingsStatusTitle.BACKUPS, rows[2].title)
        assertEquals("", rows[2].name)
        assertEquals(SettingsStatusReason.KEY_KIT, rows[3].reasonKind)
        assertEquals(Tone.OK, rows[3].tone)
        val update = rows[4]
        assertEquals(SettingsStatusGroup.APP, update.group)
        assertEquals(SettingsStatusTitle.PLAIN, update.title)
        assertEquals("HolyArrs 1.3.0", update.name)
        assertEquals(SettingsStatusFix.THIS_APP, update.fix)
        assertEquals(SettingsStatusReason.CONFIG_STEPS, rows[5].reasonKind)
        assertEquals(SettingsStatusFix.BACKUPS, rows[5].fix)
        assertEquals(SettingsStatusTitle.CONFIG_FOLDER, rows[5].title)
        assertEquals("", rows[5].name)
        assertEquals(SettingsStatusTitle.WAITING_DOWNLOADS, rows[6].title)
        assertEquals(SettingsStatusFix.DOWNLOADS_WAITING, rows[6].fix)
        assertEquals(SettingsStatusGroup.DOWNLOADERS, rows[7].group)
        assertEquals(SettingsStatusReason.SAVE_FOLDER, rows[7].reasonKind)
        assertEquals("qBit", rows[7].reason)
        assertEquals(SettingsStatusTitle.USENET_DOWNLOADER, rows[8].title)
        assertEquals(SettingsStatusReason.NO_USENET_DOWNLOADER, rows[8].reasonKind)
        assertEquals(SettingsStatusTitle.SAME_DOWNLOADER, rows[9].title)
        assertEquals("Sab", rows[9].name)
        assertEquals(SettingsStatusReason.SAME_DOWNLOADER, rows[9].reasonKind)
    }

    @Test
    fun keyKitNoteFollowsTheOwnerWording() {
        val rows =
            SettingsStatusRules.build(
                SettingsStatusInput(
                    checks = listOf(check("backups:key", "backups", "Backups", "ok", "Recommended: ask the owner for the key kit")),
                ),
            )
        assertEquals(SettingsStatusReason.KEY_KIT_OWNER, rows.single().reasonKind)
    }

    @Test
    fun outsideListsAndAlertsReadTheirOwnWords() {
        val rows =
            SettingsStatusRules.build(
                SettingsStatusInput(
                    lists =
                        listOf(
                            OutsideList(id = 1, name = "off", enabled = false),
                            OutsideList(id = 2, name = "bad", enabled = true, lastError = "token expired"),
                            OutsideList(id = 3, name = "new", enabled = true),
                            OutsideList(id = 4, name = "ok", enabled = true, lastSyncedAt = "2026-11-24T10:00:00Z", lastResult = "Added 3"),
                        ),
                    alerts =
                        listOf(
                            SettingsStatusAlert(id = 1, name = "off"),
                            SettingsStatusAlert(id = 2, name = "bad", enabled = true, lastError = "403"),
                            SettingsStatusAlert(id = 3, name = "new", enabled = true),
                            SettingsStatusAlert(id = 4, name = "ok", enabled = true, lastSentAt = "2026-11-24T10:00:00Z"),
                        ),
                ),
            )
        val lists = rows.filter { it.group == SettingsStatusGroup.LISTS }
        assertEquals(
            listOf(SettingsStatusWord.OFF, SettingsStatusWord.BROKEN, SettingsStatusWord.NOT_SYNCED, SettingsStatusWord.WORKING),
            lists.map { it.word },
        )
        assertEquals(listOf(Tone.IDLE, Tone.FAIL, Tone.IDLE, Tone.OK), lists.map { it.tone })
        assertEquals("Added 3", lists.last().reason)
        assertEquals(SettingsStatusFix.OUTSIDE_LISTS, lists.first().fix)
        val alerts = rows.filter { it.group == SettingsStatusGroup.ALERTS }
        assertEquals(
            listOf(SettingsStatusWord.OFF, SettingsStatusWord.BROKEN, SettingsStatusWord.NOT_SENT, SettingsStatusWord.WORKING),
            alerts.map { it.word },
        )
        assertEquals("403", alerts[1].reason)
        assertEquals(SettingsStatusFix.ALERTS, alerts.first().fix)
    }

    @Test
    fun metadataRowOnlyExistsOnceKnown() {
        assertTrue(SettingsStatusRules.build(SettingsStatusInput()).isEmpty())
        val up = SettingsStatusRules.build(SettingsStatusInput(metadataUp = true, metadataAt = "2026-11-24T11:00:00Z")).single()
        assertEquals(Tone.OK, up.tone)
        assertEquals(SettingsStatusTitle.METADATA, up.title)
        val down = SettingsStatusRules.build(SettingsStatusInput(metadataUp = false)).single()
        assertEquals(Tone.FAIL, down.tone)
        assertEquals(SettingsStatusReason.METADATA_DOWN, down.reasonKind)
    }

    @Test
    fun jobTonesFollowTheRules() {
        fun tone(s: SettingsScheduleRow) = SettingsStatusRules.jobTone(s, NOW)
        assertEquals(SettingsStatusWord.TURNED_OFF, tone(schedule(enabled = false, state = "off")).word)
        assertEquals(SettingsStatusWord.PART_OFF, tone(schedule(state = "part_off")).word)
        assertEquals(SettingsStatusWord.SAFE_MODE, tone(schedule(state = "safe_mode")).word)
        assertEquals(Tone.OK to SettingsStatusWord.RUNNING, tone(schedule(state = "running")).let { it.tone to it.word })
        val failed = tone(schedule(lastJob = "failed", lastJobError = "boom"))
        assertEquals(Tone.FAIL, failed.tone)
        assertEquals(SettingsStatusReason.TEXT, failed.reasonKind)
        assertEquals("boom", failed.reason)
        assertEquals(SettingsStatusReason.JOB_FAILED, tone(schedule(lastJob = "failed")).reasonKind)
        val late = tone(schedule(nextRun = "2026-11-24T11:40:00Z", lastRun = "2026-11-24T10:00:00Z"))
        assertEquals(Tone.WAIT to SettingsStatusWord.LATE, late.tone to late.word)
        assertEquals(SettingsStatusReason.JOB_LATE, late.reasonKind)
        assertEquals(SettingsStatusWord.OK, tone(schedule(nextRun = "2026-11-24T11:55:00Z", lastRun = "2026-11-24T10:00:00Z")).word)
        assertEquals(SettingsStatusWord.NOT_RUN, tone(schedule(nextRun = "2026-11-24T13:00:00Z")).word)
        assertEquals(
            SettingsStatusWord.CANCELLED,
            tone(schedule(lastJob = "cancelled", lastJobEnded = "2026-11-24T10:00:00Z", nextRun = "2026-11-24T13:00:00Z")).word,
        )
        assertEquals(
            SettingsStatusWord.OK,
            tone(schedule(lastJob = "cancelled", lastChecked = "2026-11-24T11:00:00Z", nextRun = "2026-11-24T13:00:00Z")).word,
        )
        assertEquals(
            SettingsStatusWord.OK,
            tone(schedule(state = "queued", lastRun = "2026-11-24T10:00:00Z", nextRun = "2026-11-24T09:00:00Z")).word,
        )
    }

    @Test
    fun lastSeenPicksTheNewestOfRunHandRunAndCheck() {
        val ran = schedule(lastRun = "2026-11-24T08:00:00Z", lastJobEnded = "2026-11-24T09:00:00Z")
        assertEquals("2026-11-24T09:00:00Z", SettingsStatusRules.lastRan(ran))
        assertEquals("2026-11-24T08:00:00Z", SettingsStatusRules.lastRan(schedule(lastRun = "2026-11-24T08:00:00Z")))
        assertEquals("2026-11-24T09:00:00Z", SettingsStatusRules.lastRan(schedule(lastJobEnded = "2026-11-24T09:00:00Z")))
        val checked =
            SettingsStatusRules.lastSeen(
                schedule(lastRun = "2026-11-24T08:00:00Z", lastChecked = "2026-11-24T10:00:00Z", lastSkip = "nobody"),
            )
        assertTrue(checked.checked)
        assertEquals("nobody", checked.why)
        assertEquals("2026-11-24T10:00:00Z", checked.at)
        val older = SettingsStatusRules.lastSeen(schedule(lastRun = "2026-11-24T11:00:00Z", lastChecked = "2026-11-24T10:00:00Z"))
        assertFalse(older.checked)
        assertEquals("2026-11-24T11:00:00Z", older.at)
        assertEquals("", SettingsStatusRules.lastSeen(schedule()).at)
        val onlyCheck = SettingsStatusRules.lastSeen(schedule(lastChecked = "2026-11-24T10:00:00Z"))
        assertEquals("nothing", onlyCheck.why)
    }

    @Test
    fun jobRowsCarryRunTimesAndActions() {
        val row =
            SettingsStatusRules
                .build(
                    SettingsStatusInput(
                        schedules =
                            listOf(
                                schedule(
                                    "health_check",
                                    lastRun = "2026-11-24T08:00:00Z",
                                    lastJobEnded = "2026-11-24T08:00:30Z",
                                    nextRun = "2026-11-24T14:00:00Z",
                                    lastJob = "done",
                                ),
                            ),
                        nowMillis = NOW,
                    ),
                ).single()
        assertEquals("job:health_check", row.key)
        assertEquals(SettingsStatusGroup.JOBS, row.group)
        assertEquals("Job health_check", row.name)
        assertEquals("2026-11-24T08:00:30Z", row.lastRun)
        assertEquals("2026-11-24T14:00:00Z", row.nextRun)
        assertEquals("done", row.lastResult)
        assertEquals(SettingsStatusCheckKind.JOB, row.check?.kind)
        assertEquals("health_check", row.check?.job)
        assertEquals(SettingsStatusFix.SCHEDULES, row.fix)
    }

    @Test
    fun attentionHeadlineCountsChecksAndJobsSeparately() {
        val rows =
            listOf(
                SettingsStatusRow(SettingsStatusGroup.SOURCES, "a", tone = Tone.WAIT),
                SettingsStatusRow(SettingsStatusGroup.LIBRARY, "b", tone = Tone.FAIL),
                SettingsStatusRow(SettingsStatusGroup.JOBS, "c", tone = Tone.FAIL),
                SettingsStatusRow(SettingsStatusGroup.DOWNLOADERS, "d", tone = Tone.OK),
            )
        assertEquals(listOf("b", "c", "a"), SettingsStatusRules.needsAttention(rows).map { it.key })
        val headline = SettingsStatusRules.headline(rows, true)
        assertEquals(SettingsStatusHeadlineKind.ATTENTION, headline.kind)
        assertEquals(2, headline.checks)
        assertEquals(1, headline.jobs)
    }

    @Test
    fun calmHeadlines() {
        val fine = listOf(SettingsStatusRow(SettingsStatusGroup.SOURCES, "a", tone = Tone.OK))
        assertEquals(SettingsStatusHeadlineKind.FINE, SettingsStatusRules.headline(fine, true).kind)
        assertEquals(SettingsStatusHeadlineKind.UNKNOWN, SettingsStatusRules.headline(fine, false).kind)
        val none = listOf(SettingsStatusRow(SettingsStatusGroup.LIBRARY, "a", tone = Tone.OK))
        assertEquals(SettingsStatusHeadlineKind.NOTHING_LINKED, SettingsStatusRules.headline(none, true).kind)
        assertEquals(SettingsStatusHeadlineKind.NOTHING_LINKED, SettingsStatusRules.headline(emptyList(), true).kind)
    }

    @Test
    fun linkedKindsAlwaysListAllSevenAndSummarise() {
        val rows =
            listOf(
                SettingsStatusRow(SettingsStatusGroup.SOURCES, "s1", tone = Tone.OK, word = SettingsStatusWord.WORKING),
                SettingsStatusRow(SettingsStatusGroup.SOURCES, "s2", tone = Tone.FAIL, word = SettingsStatusWord.BROKEN),
                SettingsStatusRow(SettingsStatusGroup.DOWNLOADERS, "d1", tone = Tone.OK, word = SettingsStatusWord.WORKING),
                SettingsStatusRow(SettingsStatusGroup.DOWNLOADERS, "d2", tone = Tone.OK, word = SettingsStatusWord.WORKING),
                SettingsStatusRow(SettingsStatusGroup.MEDIA, "m", tone = Tone.OK, word = SettingsStatusWord.WORKING),
                SettingsStatusRow(SettingsStatusGroup.LISTS, "l1", tone = Tone.IDLE, word = SettingsStatusWord.OFF),
                SettingsStatusRow(SettingsStatusGroup.LISTS, "l2", tone = Tone.IDLE, word = SettingsStatusWord.OFF),
                SettingsStatusRow(SettingsStatusGroup.ALERTS, "a1", tone = Tone.IDLE, word = SettingsStatusWord.NOT_SENT),
                SettingsStatusRow(SettingsStatusGroup.ALERTS, "a2", tone = Tone.IDLE, word = SettingsStatusWord.NOT_SENT),
                SettingsStatusRow(SettingsStatusGroup.LIBRARY, "x", tone = Tone.FAIL),
            )
        val kinds = SettingsStatusRules.kinds(rows, emptySet())
        assertEquals(
            listOf(
                SettingsStatusGroup.SOURCES,
                SettingsStatusGroup.DOWNLOADERS,
                SettingsStatusGroup.MEDIA,
                SettingsStatusGroup.SUBTITLES,
                SettingsStatusGroup.LISTS,
                SettingsStatusGroup.ALERTS,
                SettingsStatusGroup.METADATA,
            ),
            kinds.map { it.group },
        )
        val sources = kinds[0]
        assertEquals(listOf("s2", "s1"), sources.rows.map { it.key })
        assertEquals(1, sources.okCount)
        assertEquals(2, sources.onCount)
        assertEquals(SettingsStatusMarker.FAIL, sources.marker)
        assertEquals(SettingsStatusSummaryKind.NEEDS, sources.summary)
        assertEquals(1, sources.summaryCount)
        assertEquals(SettingsStatusSummaryKind.WORKING, kinds[1].summary)
        assertEquals(2, kinds[1].summaryCount)
        assertEquals(SettingsStatusSummaryKind.WORD, kinds[2].summary)
        assertEquals(SettingsStatusWord.WORKING, kinds[2].summaryWord)
        assertTrue(kinds[3].isEmpty)
        assertEquals(SettingsStatusSummaryKind.NOTHING, kinds[3].summary)
        assertEquals(SettingsStatusMarker.NONE, kinds[3].marker)
        assertEquals(SettingsStatusSummaryKind.OFF, kinds[4].summary)
        assertEquals(2, kinds[4].summaryCount)
        assertEquals(0, kinds[4].onCount)
        assertEquals(SettingsStatusMarker.IDLE, kinds[4].marker)
        assertEquals(SettingsStatusSummaryKind.WAITING, kinds[5].summary)
        assertEquals(2, kinds[5].summaryCount)
        assertEquals(SettingsStatusFix.OUTSIDE_LISTS, kinds[4].fix)
        assertEquals(SettingsStatusFix.THIS_APP, kinds[6].fix)
        assertEquals(SettingsStatusGroup.SOURCES, SettingsStatusRules.defaultKind(kinds))
        val calm =
            SettingsStatusRules.kinds(
                rows.filter { it.tone != Tone.FAIL },
                setOf(SettingsStatusGroup.LISTS, SettingsStatusGroup.ALERTS),
            )
        assertEquals(5, calm.size)
        assertEquals(SettingsStatusGroup.SOURCES, SettingsStatusRules.defaultKind(calm))
        val warn = listOf(SettingsStatusRow(SettingsStatusGroup.MEDIA, "m", tone = Tone.WAIT))
        assertEquals(SettingsStatusGroup.MEDIA, SettingsStatusRules.defaultKind(SettingsStatusRules.kinds(warn, emptySet())))
    }

    @Test
    fun diskBarsTurnAmberAndRed() {
        assertEquals(50, SettingsStatusRules.usedPercent(500, 1000))
        assertEquals(-1, SettingsStatusRules.usedPercent(-1, 1000))
        assertEquals(-1, SettingsStatusRules.usedPercent(10, -1))
        assertEquals(SettingsStatusDiskLevel.NORMAL, SettingsStatusRules.diskLevel(84))
        assertEquals(SettingsStatusDiskLevel.WARN, SettingsStatusRules.diskLevel(85))
        assertEquals(SettingsStatusDiskLevel.WARN, SettingsStatusRules.diskLevel(94))
        assertEquals(SettingsStatusDiskLevel.FULL, SettingsStatusRules.diskLevel(95))
        assertEquals(SettingsStatusDiskLevel.NORMAL, SettingsStatusRules.diskLevel(-1))
        assertEquals(SettingsStatusDiskKind.MOVIES, SettingsStatusRules.diskKind("movie"))
        assertEquals(SettingsStatusDiskKind.OTHER, SettingsStatusRules.diskKind("x"))
    }

    @Test
    fun nextRunWordsRoundLikeTheWebClient() {
        fun until(offsetSeconds: Long) =
            SettingsStatusRules.until(Instant.fromEpochMilliseconds(NOW + offsetSeconds * 1000).toString(), NOW)

        fun next(offsetSeconds: Long) =
            SettingsStatusRules.nextRun(Instant.fromEpochMilliseconds(NOW + offsetSeconds * 1000).toString(), NOW)
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.NOW), until(30))
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.IN, SettingsStatusUnit.MINUTES, 1), until(31 + 29))
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.IN, SettingsStatusUnit.MINUTES, 12), until(12 * 60))
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.IN, SettingsStatusUnit.HOURS, 2), until(2 * 3600))
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.IN, SettingsStatusUnit.DAYS, 3), until(3 * 86400))
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.NOW), next(-60))
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.OVERDUE, SettingsStatusUnit.MINUTES, 12), next(-12 * 60))
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.OVERDUE, SettingsStatusUnit.HOURS, 5), next(-5 * 3600))
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.OVERDUE, SettingsStatusUnit.DAYS, 2), next(-2 * 86400))
        assertEquals(SettingsStatusDue(), SettingsStatusRules.until("", NOW))
        assertEquals(SettingsStatusDue(), SettingsStatusRules.nextRun("never", NOW))
    }

    @Test
    fun nextWordIsNotPlannedForStoppedJobs() {
        val stopped =
            SettingsStatusRow(SettingsStatusGroup.JOBS, "j", word = SettingsStatusWord.TURNED_OFF, nextRun = "2026-11-24T13:00:00Z")
        assertEquals(SettingsStatusDue(), SettingsStatusRules.nextWord(stopped, NOW))
        val planned = SettingsStatusRow(SettingsStatusGroup.JOBS, "j", word = SettingsStatusWord.OK, nextRun = "2026-11-24T13:00:00Z")
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.IN, SettingsStatusUnit.HOURS, 1), SettingsStatusRules.nextWord(planned, NOW))
        val none = SettingsStatusRow(SettingsStatusGroup.JOBS, "j", word = SettingsStatusWord.OK)
        assertEquals(SettingsStatusDue(), SettingsStatusRules.nextWord(none, NOW))
        val running = SettingsStatusRow(SettingsStatusGroup.JOBS, "j", word = SettingsStatusWord.RUNNING, nextRun = "2026-11-24T11:30:00Z")
        assertEquals(SettingsStatusDueKind.NOW, SettingsStatusRules.nextWord(running, NOW).kind)
    }

    @Test
    fun upcomingSkipsStoppedAndRunningJobsAndKeepsFive() {
        val jobs =
            (1..8)
                .map { i ->
                    SettingsStatusRow(
                        SettingsStatusGroup.JOBS,
                        "job:k$i",
                        name = "K$i",
                        word = SettingsStatusWord.OK,
                        nextRun = "2026-11-24T${(12 + i).toString().padStart(2, '0')}:00:00Z",
                        check = SettingsStatusCheck(SettingsStatusCheckKind.JOB, job = "k$i"),
                    )
                }.toMutableList()
        jobs[1] = jobs[1].copy(word = SettingsStatusWord.TURNED_OFF)
        jobs += SettingsStatusRow(SettingsStatusGroup.JOBS, "job:none", name = "None")
        val running = SettingsStatusRules.running(listOf(SettingsStatusActiveJob(id = 1, kind = "k1", state = "running", progress = 0.5)))
        val next = SettingsStatusRules.upcoming(jobs, running, NOW)
        assertEquals(listOf("job:k3", "job:k4", "job:k5", "job:k6", "job:k7"), next.map { it.key })
        assertEquals(SettingsStatusDue(SettingsStatusDueKind.IN, SettingsStatusUnit.HOURS, 3), next.first().due)
    }

    @Test
    fun runningJobsListRunningBeforeQueued() {
        val list =
            SettingsStatusRules.running(
                listOf(
                    SettingsStatusActiveJob(id = 2, kind = "search_missing", state = "queued", label = "Search missing"),
                    SettingsStatusActiveJob(id = 1, kind = "rescan_library", state = "running", progress = 0.426, note = "Scanning"),
                ),
            )
        assertEquals(listOf(1L, 2L), list.map { it.id })
        assertEquals("rescan library", list[0].name)
        assertEquals(42, list[0].percent)
        assertTrue(list[0].hasProgress)
        assertTrue(list[1].queued)
        assertFalse(list[1].hasProgress)
    }

    @Test
    fun finishedRunsNewestFirstWithLearnedNames() {
        val history =
            listOf(
                HistoryRun(1, "health_check", "done", "2026-11-24T08:00:00Z"),
                HistoryRun(2, "clean_history", "failed", "2026-11-24T10:00:00Z"),
                HistoryRun(3, "mystery_job", "cancelled", "2026-11-24T09:00:00Z"),
                HistoryRun(4, "health_check", "running", ""),
                HistoryRun(5, "health_check", "queued", ""),
                HistoryRun(6, "health_check", "done", ""),
            )
        val list = SettingsStatusRules.finished(history, mapOf("health_check" to "Check health", "clean_history" to "Clean history"))
        assertEquals(listOf(2L, 3L, 1L), list.map { it.id })
        assertEquals(listOf("Clean history", "mystery job", "Check health"), list.map { it.name })
        assertEquals(listOf(Tone.FAIL, Tone.IDLE, Tone.OK), list.map { it.tone })
        assertEquals(
            listOf(SettingsStatusWord.FAILED, SettingsStatusWord.CANCELLED, SettingsStatusWord.OK),
            list.map { it.word },
        )
    }

    @Test
    fun shortPathsKeepTheLastTwoParts() {
        assertEquals("…/movies/4k", SettingsStatusRules.shortPath("/mnt/media/movies/4k"))
        assertEquals("/media/movies", SettingsStatusRules.shortPath("/media/movies"))
        assertEquals("Backups", SettingsStatusRules.shortPath("Backups"))
        assertEquals("…/b/c", SettingsStatusRules.shortPath("C:\\a\\b\\c"))
    }

    @Test
    fun uptimeReadsLikeAClock() {
        assertEquals("0d 00:27:14", settingsStatusUptime(27 * 60 + 14))
        assertEquals("2d 03:04:05", settingsStatusUptime(2 * 86400 + 3 * 3600 + 4 * 60 + 5))
        assertEquals("0d 00:00:00", settingsStatusUptime(-5))
    }
}

class SettingsStatusUpdatesTest {
    @Test
    fun standingFollowsTheUpdateAnswer() {
        fun info(
            on: Boolean = true,
            newer: Boolean = false,
            problem: String = "",
            noRelease: Boolean = false,
            checked: String = "",
        ) = SettingsUpdateInfo(on = on, newer = newer, problem = problem, noRelease = noRelease, checkedAt = checked)
        assertEquals(SettingsStatusStanding.LOADING, SettingsStatusUpdates.standing(null))
        assertEquals(SettingsStatusStanding.OFF, SettingsStatusUpdates.standing(info(on = false, newer = true)))
        assertEquals(SettingsStatusStanding.BEHIND, SettingsStatusUpdates.standing(info(newer = true, problem = "x")))
        assertEquals(SettingsStatusStanding.PROBLEM, SettingsStatusUpdates.standing(info(problem = "x")))
        assertEquals(SettingsStatusStanding.NONE, SettingsStatusUpdates.standing(info(noRelease = true, checked = "2026-11-24T10:00:00Z")))
        assertEquals(SettingsStatusStanding.CURRENT, SettingsStatusUpdates.standing(info(checked = "2026-11-24T10:00:00Z")))
        assertEquals(SettingsStatusStanding.UNCHECKED, SettingsStatusUpdates.standing(info()))
    }

    @Test
    fun releaseLinksMustBeProjectReleasePages() {
        val base = "https://github.com/HolyArrs/HolyArrs/releases/"
        assertEquals(base + "tag/v1.4.0", SettingsStatusUpdates.releaseLink(base + "tag/v1.4.0"))
        assertEquals("", SettingsStatusUpdates.releaseLink("https://github.com/Other/Other/releases/tag/v1"))
        assertEquals("", SettingsStatusUpdates.releaseLink(base + "tag/v1?x=1"))
        assertEquals("", SettingsStatusUpdates.releaseLink(base + "tag/v1#top"))
        assertEquals("", SettingsStatusUpdates.releaseLink(base + "../../evil"))
        assertEquals("", SettingsStatusUpdates.releaseLink("https://github.com/HolyArrs/HolyArrs/releases@evil.com/x"))
        assertEquals("", SettingsStatusUpdates.releaseLink("http://github.com/HolyArrs/HolyArrs/releases/tag/v1"))
        assertEquals("", SettingsStatusUpdates.releaseLink(""))
    }

    @Test
    fun notesAndVersions() {
        val behind = SettingsUpdateInfo(on = true, newer = true, notes = "One\r\n\nTwo\n")
        assertEquals(listOf("One", "Two"), SettingsStatusUpdates.noteLines(behind))
        assertEquals(emptyList(), SettingsStatusUpdates.noteLines(behind.copy(newer = false)))
        assertEquals(emptyList(), SettingsStatusUpdates.noteLines(null))
        assertEquals("1.4.0", SettingsStatusUpdates.plainVersion(" v1.4.0 "))
        assertEquals("1.4.0", SettingsStatusUpdates.plainVersion("1.4.0"))
    }

    @Test
    fun installPhasesAndMovingStates() {
        fun view(phase: String) = SettingsStatusInstall(progress = SettingsStatusProgress(phase = phase))
        assertEquals(SettingsStatusPhase.IDLE, SettingsStatusUpdates.phase(null))
        assertEquals(SettingsStatusPhase.DOWNLOADING, SettingsStatusUpdates.phase(view("downloading")))
        assertEquals(SettingsStatusPhase.READY, SettingsStatusUpdates.phase(view("ready")))
        assertEquals(SettingsStatusPhase.IDLE, SettingsStatusUpdates.phase(view("whatever")))
        for (moving in listOf("downloading", "checking", "waiting", "copying", "restarting")) {
            assertTrue(SettingsStatusUpdates.moving(view(moving)), moving)
        }
        for (still in listOf("idle", "ready", "failed")) assertFalse(SettingsStatusUpdates.moving(view(still)), still)
        assertEquals("ask", SettingsStatusUpdates.mode(SettingsStatusInstall(mode = "ask")).wire)
        assertEquals("off", SettingsStatusUpdates.mode(SettingsStatusInstall(mode = "weird")).wire)
    }

    @Test
    fun testLabelsNameTheAppAndVersion() {
        assertEquals("qBittorrent v5.1.2", SettingsStatusUpdates.testLabel("qBittorrent 5.1.2.build3 answered"))
        assertEquals("Sonarr v4.0.1", SettingsStatusUpdates.testLabel("Connected to Sonarr v4.0.1 at http://10.0.0.5:8989"))
        assertEquals("aria2 v1.37", SettingsStatusUpdates.testLabel("aria2 1.37 ready"))
        assertEquals("v1.2", SettingsStatusUpdates.testLabel("Server 1.2 ok"))
        assertEquals("", SettingsStatusUpdates.testLabel("Connected"))
        assertEquals("", SettingsStatusUpdates.testVersion("Reached http://10.0.0.5:8989"))
        assertEquals("v10.0.0", SettingsStatusUpdates.testVersion("host 10.0.0.5:8989"))
        assertEquals("Plex", SettingsStatusUpdates.testLabel("plex answered"))
    }
}
