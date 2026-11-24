package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.Decision
import ch.orellbuehler.acolyte.api.model.EpisodeLine
import ch.orellbuehler.acolyte.feature.downloads.DecisionFix
import ch.orellbuehler.acolyte.feature.downloads.DecisionHint
import ch.orellbuehler.acolyte.feature.downloads.DecisionRules
import ch.orellbuehler.acolyte.feature.downloads.DecisionStep
import ch.orellbuehler.acolyte.feature.downloads.NoMatchKind
import ch.orellbuehler.acolyte.feature.downloads.ReasonKind
import ch.orellbuehler.acolyte.feature.downloads.ReasonLineKind
import ch.orellbuehler.acolyte.feature.downloads.WhyKind
import ch.orellbuehler.acolyte.feature.downloads.groupDecisions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DecisionRulesTest {
    private fun dec(
        reason: String,
        kind: String = "movie",
        why: String = "conflict",
        downloadId: Long = 7,
        importFile: String = "",
        importFolder: String = "",
    ) = Decision(
        slotId = 1,
        kind = kind,
        why = why,
        downloadId = downloadId,
        reason = reason,
        importFile = importFile,
        importFolder = importFolder,
    )

    private val pair =
        "HolyArrs can't find the downloaded file where the Downloader says it is; check the path pair in Settings > Downloaders and " +
            "that the download folder is mounted, then import again"
    private val read =
        "HolyArrs may not read the downloaded file; give the user HolyArrs runs as (PUID and PGID) read access to the file " +
            "and the folder it is in, then import again"
    private val write =
        "HolyArrs may not write to the folder this file goes to in the Library; give the user HolyArrs runs as (PUID and PGID) " +
            "write access to it, then import again"
    private val save =
        "HolyArrs can't read this download's files: HOLYARRS_SAVE_PATH is not set and SABnzbd has no path pair. " +
            "Set HOLYARRS_SAVE_PATH if both apps see the same download folder, or add a path pair in Settings > Downloaders, then import again"
    private val slow = "reading the download folder took too long; try again"

    @Test
    fun leadsARefusedOutsideFileWithMatchByHand() {
        val reason = "HolyArrs refused Show.S01E01.mkv: the Downloader reported a file outside the download folder."
        assertEquals(DecisionFix.MATCH, DecisionRules.fix(dec(reason, kind = "show", why = "unmatched", importFolder = "/dl/Show")))
        assertEquals(ReasonKind.REFUSED, DecisionRules.reasonKind(dec(reason, kind = "show", why = "unmatched")))
        assertEquals(ReasonKind.REFUSED, DecisionRules.reasonKind(dec(reason.dropLast(1), kind = "show", why = "unmatched")))
    }

    @Test
    fun readsTheOtherReasonKindsWithTheFullStop() {
        val noMatch = dec("No file in the download matched an episode; match the files by hand.", kind = "show", why = "unmatched")
        assertEquals(ReasonKind.NO_MATCH, DecisionRules.reasonKind(noMatch))
        assertEquals(DecisionFix.VERSION, DecisionRules.fix(dec("The copy in place is protected; choose another Version for an upgrade.")))
        assertEquals(
            DecisionFix.EDITION,
            DecisionRules.fix(dec("Edition is unknown; confirm the edition or choose an unconstrained Version.")),
        )
    }

    @Test
    fun sendsAMissingSaveFolderToDownloaderSettings() {
        assertEquals(DecisionFix.FOLDER, DecisionRules.fix(dec(save)))
        assertEquals(DecisionFix.FOLDER, DecisionRules.fix(dec(save, kind = "show", why = "unreadable")))
        assertEquals(ReasonKind.PLAIN, DecisionRules.reasonKind(dec(save)))
        val plan = DecisionRules.plan(dec(save))
        assertEquals(DecisionStep.IMPORT_AGAIN, plan.lead)
        assertEquals(DecisionHint.PATH_PAIR, plan.hint)
    }

    @Test
    fun offersSetEditionOnlyForAnEditionRefusal() {
        val reasons =
            listOf(
                "edition is unknown; confirm the edition or choose an unconstrained Version",
                "edition is ambiguous; confirm it explicitly",
                "edition does not match the selected Version",
                "file and release name disagree about the edition; confirm it explicitly",
            )
        for (reason in reasons) {
            assertEquals(DecisionFix.EDITION, DecisionRules.fix(dec(reason)), reason)
            assertEquals(ReasonKind.EDITION, DecisionRules.reasonKind(dec(reason)), reason)
        }
        val other = dec("No file in the download matched this movie; match the files by hand.")
        assertEquals(DecisionFix.HAND, DecisionRules.fix(other))
        assertEquals(ReasonKind.NO_MATCH, DecisionRules.reasonKind(other))
        assertEquals(
            ReasonKind.PLAIN,
            DecisionRules.reasonKind(dec("nothing could be placed: not enough free space in the Library folder")),
        )
    }

    @Test
    fun pointsACopyInPlaceReasonToAnotherVersion() {
        val reasons =
            listOf(
                "the copy in place is protected; choose another Version for an upgrade",
                "the new edition differs from the copy in place; choose another Version",
                "file is 1080p web, which this Version's Pick rules don't allow, so it can't replace the copy in place; choose another Version",
            )
        for (reason in reasons) {
            assertEquals(DecisionFix.VERSION, DecisionRules.fix(dec(reason)))
            assertEquals(ReasonKind.COPY, DecisionRules.reasonKind(dec(reason)))
        }
    }

    @Test
    fun leadsWithImportThisFileWhenTheMovieHasOneVideo() {
        val reason = "No file in the download matched this movie; match the files by hand."
        assertEquals(DecisionFix.IMPORT, DecisionRules.fix(dec(reason, importFile = "grp-ean.mkv")))
        assertEquals(ReasonKind.NO_MATCH, DecisionRules.reasonKind(dec(reason, importFile = "grp-ean.mkv")))
        assertEquals(DecisionFix.EDITION, DecisionRules.fix(dec("edition is unknown", importFile = "x.mkv")))
        assertEquals(DecisionFix.HAND, DecisionRules.fix(dec(reason, kind = "show", importFile = "x.mkv")))
    }

    @Test
    fun leadsWithMatchByHandWhenNothingMatchedAndTheFolderIsKnown() {
        val reason = "No file in the download matched an episode; match the files by hand."
        assertEquals(DecisionFix.MATCH, DecisionRules.fix(dec(reason, kind = "show", importFolder = "/data/x")))
        assertEquals(DecisionFix.HAND, DecisionRules.fix(dec(reason, kind = "show")))
        assertEquals(DecisionFix.MATCH, DecisionRules.fix(dec("nothing could be placed; look at the import plan", importFolder = "/d")))
        val both = dec("No file in the download matched this movie; match the files by hand.", importFolder = "/d", importFile = "a.mkv")
        assertEquals(DecisionFix.IMPORT, DecisionRules.fix(both))
        val plan = DecisionRules.plan(dec(reason, kind = "show", why = "ran_out", importFolder = "/data/x"))
        assertEquals(DecisionStep.IMPORT_AGAIN, plan.lead)
        assertEquals(DecisionHint.MATCH_BY_HAND, plan.hint)
    }

    @Test
    fun namesWhatDidNotMatch() {
        assertEquals(NoMatchKind.MOVIE, DecisionRules.noMatchKind("No file in the download matched this movie; match the files by hand."))
        assertEquals(NoMatchKind.EPISODE, DecisionRules.noMatchKind("no file in the download matched an episode; match the files by hand"))
        assertEquals(NoMatchKind.TRACK, DecisionRules.noMatchKind("No file in the download matched a track; match the files by hand."))
        assertEquals(NoMatchKind.MOVIE, DecisionRules.noMatchKind("Nothing could be placed; look at the import plan."))
        assertEquals(
            NoMatchKind.EPISODE,
            DecisionRules.noMatchKind("nothing in the download could be matched to an episode; look at the import plan"),
        )
        assertEquals(
            NoMatchKind.TRACK,
            DecisionRules.noMatchKind("nothing in the download could be matched to a track; look at the import plan"),
        )
        assertNull(DecisionRules.noMatchKind("nothing could be placed: not enough free space in the Library folder"))
    }

    @Test
    fun keepsShowsAndMissingDownloadsOffTheMovieFixes() {
        assertEquals(DecisionFix.HAND, DecisionRules.fix(dec("edition is unknown", kind = "show")))
        assertEquals(DecisionFix.HAND, DecisionRules.fix(dec("edition is unknown", downloadId = 0)))
        assertEquals(
            DecisionFix.HAND,
            DecisionRules.fix(dec("the copy in place is protected; choose another Version for an upgrade", kind = "show")),
        )
        assertEquals(DecisionFix.SEARCH, DecisionRules.fix(dec("no other Release passed the Pick rules", why = "ran_out")))
        assertEquals(ReasonKind.TAKEN, DecisionRules.reasonKind(dec("x", why = "taken")))
    }

    @Test
    fun offersImportAgainForAReasonFixedOutsideHolyArrs() {
        for (kind in listOf("movie", "show", "artist")) {
            assertEquals(DecisionFix.AGAIN, DecisionRules.fix(dec(slow, kind = kind)))
            assertTrue(DecisionRules.canImportAgain(dec(slow, kind = kind)))
            for (reason in listOf(pair, save)) {
                assertEquals(DecisionFix.FOLDER, DecisionRules.fix(dec(reason, kind = kind)))
                assertTrue(DecisionRules.canImportAgain(dec(reason, kind = kind)))
            }
            for (reason in listOf(read, write)) {
                assertEquals(DecisionFix.AGAIN, DecisionRules.fix(dec(reason, kind = kind)))
                assertTrue(DecisionRules.canImportAgain(dec(reason, kind = kind)))
            }
        }
        for (kind in listOf("show", "artist")) {
            assertEquals(
                DecisionFix.AGAIN,
                DecisionRules.fix(dec("the file could not be placed: something went wrong; see the log", kind = kind)),
            )
            assertTrue(DecisionRules.canImportAgain(dec("The file could not be placed: something went wrong; see the log.", kind = kind)))
        }
        assertFalse(DecisionRules.canImportAgain(dec("the file could not be placed: open /dl/something went wrong; see the log")))
        assertEquals(DecisionFix.IMPORT, DecisionRules.fix(dec(read, importFile = "a.mkv")))
        assertTrue(DecisionRules.canImportAgain(dec(read, importFile = "a.mkv")))
        assertFalse(DecisionRules.canImportAgain(dec(read, downloadId = 0)))
        assertEquals(DecisionFix.HAND, DecisionRules.fix(dec(read, kind = "show", downloadId = 0)))
        val never =
            listOf(
                "no file in the download matched this movie; match the files by hand",
                "the file could not be placed: open /dl/HolyArrs may not read the downloaded file.mkv: input/output error",
                "no other Release passed the Pick rules",
            )
        for (reason in never) assertFalse(DecisionRules.canImportAgain(dec(reason)), reason)
    }

    @Test
    fun doesNotTakeAWordInAPathForAFix() {
        fun placed(tail: String) = dec("the file could not be placed: $tail", importFile = "a.mkv", importFolder = "/d")
        val path =
            placed(
                "hardlink failed (link /dl/Blade.Runner.1982.Special.Edition.1080p.mkv " +
                    "/movies/Blade Runner (1982)/Blade Runner (1982).mkv: operation not permitted)",
            )
        assertEquals(DecisionFix.IMPORT, DecisionRules.fix(path))
        assertEquals(ReasonKind.PLAIN, DecisionRules.reasonKind(path))
        assertEquals(DecisionFix.IMPORT, DecisionRules.fix(placed("mkdir /movies/My copy in place/x: read-only file system")))
        assertEquals(DecisionFix.IMPORT, DecisionRules.fix(placed("open /dl/HOLYARRS_SAVE_PATH/x.mkv: input/output error")))
        assertEquals(DecisionFix.VERSION, DecisionRules.fix(dec("the new edition differs from the copy in place; choose another Version")))
    }

    @Test
    fun importAgainShowsBesideAnotherLead() {
        assertTrue(DecisionRules.importAgainBeside(dec(pair), DecisionRules.fix(dec(pair))))
        val noMatch =
            dec(
                "no file in the download matched an episode; match the files by hand",
                kind = "show",
                why = "ran_out",
                importFolder = "/dl/x",
            )
        assertTrue(DecisionRules.mayImportAgain(noMatch))
        assertFalse(DecisionRules.canImportAgain(noMatch))
        assertTrue(DecisionRules.importAgainBeside(noMatch, DecisionRules.fix(noMatch)))
        val leads = dec("the file could not be placed: something went wrong; see the log")
        assertFalse(DecisionRules.importAgainBeside(leads, DecisionRules.fix(leads)))
        assertFalse(DecisionRules.importAgainBeside(dec(pair, downloadId = 0), DecisionRules.fix(dec(pair, downloadId = 0))))
        assertFalse(DecisionRules.mayImportAgain(noMatch.copy(reason = "nothing could be placed")))
    }

    @Test
    fun countsMinutesUntilTheRetry() {
        val now = Instant.parse("2026-11-22T12:00:00Z")
        assertNull(DecisionRules.retryMinutes("", now))
        assertNull(DecisionRules.retryMinutes("soon", now))
        assertEquals(5, DecisionRules.retryMinutes("2026-11-22T12:04:10Z", now))
        assertEquals(4, DecisionRules.retryMinutes("2026-11-22T12:04:00Z", now))
        assertEquals(1, DecisionRules.retryMinutes("2026-11-22T12:00:05Z", now))
        assertEquals(1, DecisionRules.retryMinutes("2026-11-22T11:58:00Z", now))
    }

    @Test
    fun leadsAPackWithImportAgainWhenEveryTrackWaitsOnIt() {
        fun tracks(
            reason: (Int) -> String,
            id: (Int) -> Long = { 7 },
        ) = (0 until 8).map { dec(reason(it), kind = "artist", why = "unreadable", downloadId = id(it)) }
        assertTrue(DecisionRules.packImportAgain(tracks({ write })))
        for (r in listOf(
            slow,
            "the file could not be placed: something went wrong; see the log",
            save,
        )) {
            assertTrue(
                DecisionRules.packImportAgain(
                    tracks({
                        r
                    }),
                ),
            )
        }
        assertFalse(DecisionRules.packImportAgain(tracks({ if (it == 3) "no file in the download matched a track" else write })))
        assertFalse(DecisionRules.packImportAgain(tracks({ "no file in the download matched a track" })))
        assertFalse(DecisionRules.packImportAgain(tracks({ write }, { if (it == 0) 7 else 8 })))
        assertFalse(DecisionRules.packImportAgain(emptyList()))
    }

    @Test
    fun saysTheReasonLine() {
        assertEquals(ReasonLineKind.READING, DecisionRules.reasonLine(dec(slow).copy(reading = true)).kind)
        assertEquals(ReasonLineKind.EDITION_UNKNOWN, DecisionRules.reasonLine(dec("edition is unknown")).kind)
        assertEquals(ReasonLineKind.COPY_IN_PLACE, DecisionRules.reasonLine(dec("the copy in place is protected")).kind)
        assertEquals(
            ReasonLineKind.NO_MATCH_EPISODE,
            DecisionRules.reasonLine(dec("No file in the download matched an episode.", kind = "show")).kind,
        )
        val link = DecisionRules.reasonLine(dec("not sent: the link leads to a local address", why = "link_refused"))
        assertEquals(ReasonLineKind.SERVER, link.kind)
        assertEquals("the link leads to a local address", link.text)
        val placed = DecisionRules.reasonLine(dec("The file could not be placed: mkdir /x: not a directory."))
        assertEquals("The file could not be placed.", placed.text)
        val why = DecisionRules.reasonLine(dec("", why = "not_sent"))
        assertEquals(ReasonLineKind.WHY, why.kind)
        assertEquals(WhyKind.NOT_SENT, why.why)
    }

    @Test
    fun groupsPackDecisionsOnOneCard() {
        val ep = { n: Int -> EpisodeLine(season = 1, episode = n, label = "S01E0$n") }
        val items =
            listOf(
                Decision(slotId = 3, kind = "show", downloadId = 9, packSlotIds = listOf(2, 3), episode = ep(3)),
                Decision(slotId = 5, kind = "movie", downloadId = 4),
                Decision(slotId = 2, kind = "show", downloadId = 9, packSlotIds = listOf(2, 3), episode = ep(2)),
            )
        val groups = groupDecisions(items)
        assertEquals(listOf("pack-9", "slot-5"), groups.map { it.key })
        assertEquals(listOf(2L, 3L), groups[0].items.map { it.slotId })
        assertTrue(groups[0].pack)
        assertFalse(groups[1].pack)
    }
}
