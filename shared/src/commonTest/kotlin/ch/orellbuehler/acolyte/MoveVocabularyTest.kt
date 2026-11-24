package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.MoveLine
import ch.orellbuehler.acolyte.api.model.MovePathRule
import ch.orellbuehler.acolyte.api.model.MoveRecord
import ch.orellbuehler.acolyte.api.model.MoveSnapshot
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.move.FolderPick
import ch.orellbuehler.acolyte.feature.move.GateInput
import ch.orellbuehler.acolyte.feature.move.MoveCountKind
import ch.orellbuehler.acolyte.feature.move.MoveGateReason
import ch.orellbuehler.acolyte.feature.move.MovePathProblem
import ch.orellbuehler.acolyte.feature.move.MovePickKind
import ch.orellbuehler.acolyte.feature.move.MoveReachFailure
import ch.orellbuehler.acolyte.feature.move.MoveRules
import ch.orellbuehler.acolyte.feature.move.MoveStateWord
import ch.orellbuehler.acolyte.feature.move.MoveTone
import ch.orellbuehler.acolyte.feature.move.MoveVocabulary
import ch.orellbuehler.acolyte.feature.move.MoveWord
import ch.orellbuehler.acolyte.feature.move.SettingsMoveStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveVocabularyTest {
    @Test
    fun resultWordsFollowTheWebVocabulary() {
        assertEquals(MoveWord.WILL_BE_ADDED, MoveVocabulary.resultWord("create"))
        assertEquals(MoveWord.ADDED, MoveVocabulary.resultWord("created"))
        assertEquals(MoveWord.ADDED, MoveVocabulary.resultWord("create", after = true))
        assertEquals(MoveWord.WONT_WORK, MoveVocabulary.resultWord("failed"))
        assertEquals(MoveWord.FAILED, MoveVocabulary.resultWord("failed", after = true))
        assertEquals(MoveWord.NOT_REMOVED, MoveVocabulary.resultWord("failed", after = true, undo = true))
        assertEquals(MoveWord.REMOVED, MoveVocabulary.resultWord("removed"))
        assertEquals(MoveWord.KEPT, MoveVocabulary.resultWord("kept"))
        assertEquals(MoveWord.OTHER, MoveVocabulary.resultWord("odd_thing"))
        assertEquals("odd thing", MoveVocabulary.rawWord("odd_thing"))
        assertEquals(MoveWord.NO_FOLDER_HERE, MoveVocabulary.resultWord("not_found"))
    }

    @Test
    fun tonesAndLooks() {
        assertEquals(MoveTone.OK, MoveVocabulary.resultTone("adopted"))
        assertEquals(MoveTone.WARN, MoveVocabulary.resultTone("kept"))
        assertEquals(MoveTone.WARN, MoveVocabulary.resultTone("not_found"))
        assertEquals(MoveTone.BAD, MoveVocabulary.resultTone("failed"))
        assertEquals(MoveTone.QUIET, MoveVocabulary.resultTone("already"))
        assertTrue(MoveVocabulary.needsLook("skipped"))
        assertTrue(MoveVocabulary.needsLook("created", "seasons"))
        assertFalse(MoveVocabulary.needsLook("created"))
    }

    @Test
    fun summaryIsOrderedAndSkipsZeros() {
        val parts = MoveVocabulary.summary(mapOf("failed" to 1, "created" to 12, "already" to 3, "skipped" to 0), after = true)
        assertEquals(listOf(12, 3, 1), parts.map { it.n })
        assertEquals(listOf(MoveWord.ADDED, MoveWord.ALREADY_HERE, MoveWord.FAILED), parts.map { it.word })
        assertEquals(7, MoveVocabulary.moveTotal(mapOf("created" to 4, "adopted" to 2, "matched" to 1, "failed" to 9)))
    }

    @Test
    fun statsPerApp() {
        val counts = mapOf("titles" to 120, "indexers" to 5, "download_clients" to 2, "users" to 0)
        assertEquals(
            listOf(MoveCountKind.MOVIE to 120, MoveCountKind.SOURCE to 5, MoveCountKind.DOWNLOADER to 2),
            MoveVocabulary.bestStats("radarr", counts).map { it.kind to it.n },
        )
        assertEquals(MoveCountKind.SHOW, MoveVocabulary.bestStats("sonarr", counts).first().kind)
        assertEquals(MoveCountKind.ARTIST, MoveVocabulary.bestStats("lidarr", counts).first().kind)
        assertTrue(MoveVocabulary.bestStats("seerr", counts).isEmpty())
        assertEquals(MoveCountKind.PERSON, MoveVocabulary.bestStats("seerr", mapOf("users" to 4, "requests" to 9)).first().kind)
        val parts = MoveVocabulary.countParts("radarr", counts, 1)
        assertEquals(listOf(MoveCountKind.MOVIE), parts.map { it.kind })
    }

    @Test
    fun progressNotes() {
        val plain = MoveVocabulary.progressNote("3 of 10 imported.")
        assertEquals(3, plain.done)
        assertEquals(10, plain.total)
        assertEquals(0, plain.waitLeft)
        val undo = MoveVocabulary.progressNote("1 of 4 undone.")
        assertEquals(4, undo.total)
        val busy = MoveVocabulary.progressNote("3 of 10 imported. wait 130 4")
        assertEquals(4, busy.waitLeft)
        assertEquals(3, busy.waitMinutes)
        assertEquals(1, MoveVocabulary.progressNote("1 of 2 imported. wait 5 1").waitMinutes)
        assertFalse(MoveVocabulary.progressNote("starting").isCount)
    }

    @Test
    fun moveStates() {
        assertEquals(MoveStateWord.WAITING to Tone.WAIT, MoveVocabulary.stateWord("queued", ""))
        assertEquals(MoveStateWord.IMPORTING to Tone.INFO, MoveVocabulary.stateWord("running", ""))
        assertEquals(MoveStateWord.IMPORTED to Tone.OK, MoveVocabulary.stateWord("done", ""))
        assertEquals(MoveStateWord.STOPPED to Tone.FAIL, MoveVocabulary.stateWord("failed", ""))
        assertEquals(MoveStateWord.UNDONE to Tone.IDLE, MoveVocabulary.stateWord("undone", ""))
        assertEquals(MoveStateWord.UNDOING to Tone.INFO, MoveVocabulary.stateWord("done", "running"))
        assertEquals(MoveStateWord.UNDO_FAILED to Tone.FAIL, MoveVocabulary.stateWord("done", "failed"))
    }

    @Test
    fun addressesGetAScheme() {
        assertEquals("http://192.168.1.10:7878", MoveVocabulary.normalizeAddress(" 192.168.1.10:7878 "))
        assertEquals("https://radarr.example/x", MoveVocabulary.normalizeAddress("https://radarr.example/x"))
        assertEquals("", MoveVocabulary.normalizeAddress("  "))
        assertEquals("radarr:7878", MoveVocabulary.hostOf("http://radarr:7878/"))
        assertEquals("http://192.168.1.10:5055", MoveVocabulary.placeholder("seerr"))
        assertEquals("Radarr", MoveVocabulary.appName("radarr"))
        assertEquals(9, MoveVocabulary.apps.size)
    }

    @Test
    fun failuresInPlainWords() {
        assertEquals(MoveReachFailure.CANT_REACH, MoveVocabulary.reachFailure(502, "the app does not answer"))
        assertEquals(MoveReachFailure.TRY_HTTP, MoveVocabulary.reachFailure(502, "server gave HTTP response to HTTPS client"))
        assertEquals(MoveReachFailure.TIMEOUT, MoveVocabulary.reachFailure(504, "the read took too long"))
        assertEquals(MoveReachFailure.REFUSED_KEY, MoveVocabulary.reachFailure(422, "the app refused the key or password"))
        assertEquals(MoveReachFailure.TOO_BIG, MoveVocabulary.reachFailure(413, "too big"))
        assertEquals(MoveReachFailure.OTHER, MoveVocabulary.reachFailure(400, "bad body"))
    }

    @Test
    fun pathRulesAreChecked() {
        assertEquals(MovePathProblem.FILL_BOTH, MoveRules.ruleProblem("/a", ""))
        assertEquals(MovePathProblem.FULL_PATH, MoveRules.ruleProblem("a", "/b"))
        assertEquals(MovePathProblem.DOTS, MoveRules.ruleProblem("/a/../b", "/c"))
        assertEquals(MovePathProblem.NONE, MoveRules.ruleProblem("C:\\media", "/data"))
        assertEquals(MovePathProblem.NONE, MoveRules.ruleProblem("/movies", "/data/movies"))
        val rules = listOf(MovePathRule("", ""), MovePathRule("/a", ""), MovePathRule("/a", "/b"))
        assertEquals(listOf(MovePathProblem.NONE, MovePathProblem.FILL_BOTH, MovePathProblem.NONE), MoveRules.extraProblems(rules))
        assertEquals(listOf(MovePathRule("/a", ""), MovePathRule("/a", "/b")), MoveRules.cleanRules(rules))
        assertEquals("/data/movies", MoveRules.cleanPath(" //data\\movies/ "))
        assertEquals("/", MoveRules.cleanPath("/"))
    }

    @Test
    fun folderPicksBecomeRules() {
        val picks =
            mapOf(
                "/movies" to FolderPick(MovePickKind.PATH, "/data/movies"),
                "/tv" to FolderPick(MovePickKind.OTHER),
                "/4k" to FolderPick(MovePickKind.LEAVE),
                "/music" to FolderPick(MovePickKind.NONE),
            )
        val others = mapOf("/tv" to " /data/tv ")
        assertEquals(MovePathProblem.NONE, MoveRules.folderProblem("/tv", picks["/tv"], others["/tv"]))
        assertEquals(MovePathProblem.TYPE_PATH, MoveRules.folderProblem("/tv", picks["/tv"], ""))
        assertEquals(MovePathProblem.NONE, MoveRules.folderProblem("/4k", picks["/4k"], null))
        val rules = MoveRules.rulesFrom(picks, others, listOf(MovePathRule("/movies", "/skipped"), MovePathRule("/x", "/y")))
        assertEquals(listOf(MovePathRule("/movies", "/data/movies"), MovePathRule("/tv", "/data/tv"), MovePathRule("/x", "/y")), rules)
        val twins = MoveRules.sharedTargets(mapOf("/a" to FolderPick(MovePickKind.PATH, "/d/m/"), "/b" to FolderPick(MovePickKind.PATH, "/d/m")), emptyMap())
        assertEquals(listOf("/b"), twins["/a"])
        assertEquals(listOf("/a"), twins["/b"])
    }

    @Test
    fun gatesGiveTheirReasons() {
        fun gate(input: GateInput) = MoveRules.stepGate(input)
        assertEquals(MoveGateReason.TURN_ON_READ, gate(GateInput(SettingsMoveStep.READ, 0)).reason)
        assertTrue(gate(GateInput(SettingsMoveStep.READ, 2)).ok)
        assertEquals(MoveGateReason.UPDATING, gate(GateInput(SettingsMoveStep.CHOOSE, 1)).reason)
        assertEquals(MoveGateReason.PROGRESS, gate(GateInput(SettingsMoveStep.CHOOSE, 1, planning = true, hasProgress = true)).reason)
        val ready = GateInput(SettingsMoveStep.CHOOSE, 1, planned = true, groupsOn = 2, groupsWithItems = 3)
        assertTrue(gate(ready).ok)
        assertEquals(MoveGateReason.FIX_FOLDER, gate(ready.copy(folderErrors = 1)).reason)
        assertEquals(MoveGateReason.FIX_RULE, gate(ready.copy(ruleErrors = 1)).reason)
        assertEquals(MoveGateReason.NOTHING_NEW, gate(ready.copy(groupsWithItems = 0)).reason)
        assertEquals(MoveGateReason.TURN_ON_GROUP, gate(ready.copy(groupsOn = 0)).reason)
        assertEquals(MoveGateReason.SAFE_MODE, gate(ready.copy(safeMode = true)).reason)
        val tried = GateInput(SettingsMoveStep.IMPORT, 1, tried = true)
        assertTrue(gate(tried).ok)
        assertEquals(MoveGateReason.TRYING, gate(GateInput(SettingsMoveStep.IMPORT, 1, trying = true)).reason)
        assertEquals(MoveGateReason.UPDATING, gate(tried.copy(trying = true)).reason)
        assertEquals(MoveGateReason.TEST_FAILED, gate(tried.copy(failed = true)).reason)
        assertEquals(MoveGateReason.CHANGED_SINCE_TEST, gate(tried.copy(stale = true)).reason)
        assertEquals(MoveGateReason.ONE_NEEDS_CHOICE, gate(tried.copy(openChoices = 1)).reason)
        assertEquals(2, gate(tried.copy(openChoices = 2)).n)
        assertFalse(gate(tried.copy(moving = true)).ok)
    }

    @Test
    fun readsAndMovesRelate() {
        val snaps =
            listOf(
                MoveSnapshot(id = 4, kind = "radarr", address = "http://r:1", createdAt = "2026-03-01T10:00:00Z"),
                MoveSnapshot(id = 3, kind = "radarr", address = "http://r:1", createdAt = "2026-02-01T10:00:00Z"),
                MoveSnapshot(id = 2, kind = "radarr", address = "http://r:2", createdAt = "2026-02-01T10:00:00Z"),
                MoveSnapshot(id = 1, kind = "sonarr", address = "http://s:1", createdAt = "2026-01-01T10:00:00Z"),
            )
        assertEquals(listOf(4L, 2L, 1L), MoveRules.latestPerInstance(snaps).map { it.id })
        val moves =
            listOf(
                MoveRecord(id = 9, createdAt = "2026-02-15T10:00:00Z", snapshots = listOf(4, 3, 1), state = "done"),
                MoveRecord(id = 8, createdAt = "2026-02-10T10:00:00Z", snapshots = listOf(2), state = "failed"),
            )
        val used = MoveRules.usedByMoves(moves, snaps)
        assertEquals(mapOf(3L to 9L, 1L to 9L), used)
    }

    @Test
    fun folderOutcomeCountsThreeWays() {
        val lines =
            listOf(
                MoveLine(group = "titles", key = "a", result = "created", folder = "/m"),
                MoveLine(group = "titles", key = "b", result = "adopted", folder = "/m"),
                MoveLine(group = "titles", key = "c", result = "created", attention = "unreachable", folder = "/old"),
                MoveLine(group = "titles", key = "d", result = "skipped", attention = "unchecked", folder = "/leave"),
                MoveLine(group = "folders", key = "folder:/leave", result = "skipped", attention = "unchecked", folder = "/leave"),
                MoveLine(group = "folders", key = "folder:/old", result = "not_found"),
                MoveLine(group = "titles", key = "e", result = "not_found"),
            )
        val outcome = MoveRules.folderOutcome(lines)
        assertEquals(2, outcome.came)
        assertEquals(1, outcome.attention)
        assertEquals(listOf("/old"), outcome.attentionFolders)
        assertEquals(1, outcome.unchecked)
        assertEquals(listOf("/leave"), outcome.uncheckedFolders)
        assertEquals(1, MoveRules.noFolderCount(lines))
        assertEquals(
            "Show (Old Name)",
            MoveRules.seasonName(MoveLine(name = "Old Name (2020)", here = "Show")),
        )
    }
}
