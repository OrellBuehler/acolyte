package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.AskAsker
import ch.orellbuehler.acolyte.api.model.AskLimit
import ch.orellbuehler.acolyte.api.model.AskStep
import ch.orellbuehler.acolyte.api.model.AskWho
import ch.orellbuehler.acolyte.api.model.KindLimit
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.QualityChoice
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.AskFailure
import ch.orellbuehler.acolyte.feature.requests.LimitKind
import ch.orellbuehler.acolyte.feature.requests.MemberTab
import ch.orellbuehler.acolyte.feature.requests.PillNote
import ch.orellbuehler.acolyte.feature.requests.Stage
import ch.orellbuehler.acolyte.feature.requests.WaitTime
import ch.orellbuehler.acolyte.feature.requests.alsoPills
import ch.orellbuehler.acolyte.feature.requests.approverSort
import ch.orellbuehler.acolyte.feature.requests.approverTab
import ch.orellbuehler.acolyte.feature.requests.askFailure
import ch.orellbuehler.acolyte.feature.requests.askedLine
import ch.orellbuehler.acolyte.feature.requests.askerNames
import ch.orellbuehler.acolyte.feature.requests.firstMemberTab
import ch.orellbuehler.acolyte.feature.requests.isFailed
import ch.orellbuehler.acolyte.feature.requests.life
import ch.orellbuehler.acolyte.feature.requests.limitLines
import ch.orellbuehler.acolyte.feature.requests.memberRow
import ch.orellbuehler.acolyte.feature.requests.memberTab
import ch.orellbuehler.acolyte.feature.requests.ownerRow
import ch.orellbuehler.acolyte.feature.requests.qualityTagLine
import ch.orellbuehler.acolyte.feature.requests.shownName
import ch.orellbuehler.acolyte.feature.requests.waitTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestsRulesTest {
    private fun ask(
        state: String,
        titleId: Long = 0,
        adding: Boolean = false,
        problem: String = "",
        step: String? = null,
    ) = Ask(
        id = 1,
        kind = "movie",
        name = "Dune",
        state = state,
        titleId = titleId,
        adding = adding,
        problem = problem,
        step =
            step?.let {
                AskStep(it)
            },
    )

    @Test
    fun tabsFollowTheWebRules() {
        assertEquals(MemberTab.WAITING, ask("asked").memberTab)
        assertEquals(MemberTab.DECLINED, ask("turned_down").memberTab)
        assertEquals(MemberTab.READY, ask("available").memberTab)
        assertEquals(MemberTab.APPROVED, ask("approved", titleId = 4).memberTab)
        assertEquals(MemberTab.APPROVED, ask("approved", adding = true).memberTab)
        assertEquals(MemberTab.FAILED, ask("approved").memberTab)
        assertEquals(MemberTab.FAILED, ask("approved", titleId = 4, problem = "It was deleted").memberTab)
        assertEquals(ApproverTab.WAITING, ask("asked").approverTab)
        assertEquals(ApproverTab.PROCESSING, ask("approved").approverTab)
        assertEquals(ApproverTab.DONE, ask("turned_down").approverTab)
        assertEquals(ApproverTab.DONE, ask("available").approverTab)
        assertTrue(ask("approved").isFailed)
        assertFalse(ask("asked").isFailed)
    }

    @Test
    fun firstTabIsTheFirstWithAsks() {
        assertEquals(MemberTab.WAITING, firstMemberTab(emptyList()))
        assertEquals(MemberTab.APPROVED, firstMemberTab(listOf(ask("available"), ask("approved", titleId = 2))))
    }

    @Test
    fun lifecycleStages() {
        assertEquals(Stage.WAITING, ask("asked").life.stage)
        assertEquals(Tone.WAIT, ask("asked").life.tone)
        assertEquals(Stage.DECLINED, ask("turned_down").life.stage)
        assertEquals(Stage.READY, ask("available").life.stage)
        assertEquals(1f, ask("available").life.progress)
        assertEquals(Stage.FAILED, ask("approved").life.stage)
        assertEquals(Tone.FAIL, ask("approved").life.tone)
        assertEquals(Stage.SEARCHING, ask("approved", 3, step = "searching").life.stage)
        assertEquals(Stage.DOWNLOADING, ask("approved", 3, step = "downloading").life.stage)
        assertEquals(Stage.IMPORTING, ask("approved", 3, step = "importing").life.stage)
        assertEquals(Stage.NEEDS_DECISION, ask("approved", 3, step = "waiting_on_admin").life.stage)
        assertEquals(Stage.LOOKING, ask("approved", 3, step = "waiting").life.stage)
        assertEquals(Stage.UPCOMING, ask("approved", 3, step = "not_out").life.stage)
        assertEquals(Stage.PROCESSING, ask("approved", 3).life.stage)
        assertEquals(Stage.PROCESSING, ask("approved", 3, step = "not_out").copy(futureOnly = true).life.stage)
    }

    @Test
    fun approverSortPutsFailedFirst() {
        val failed = ask("approved").copy(id = 1, decidedAt = "2026-11-01T10:00:00Z")
        val newer = ask("approved", 3).copy(id = 2, decidedAt = "2026-11-20T10:00:00Z")
        val older = ask("approved", 3).copy(id = 3, decidedAt = "2026-11-10T10:00:00Z")
        assertEquals(listOf(1L, 2L, 3L), approverSort(ApproverTab.PROCESSING, listOf(older, newer, failed)).map { it.id })
        val a = ask("asked").copy(id = 5, askedAt = "2026-11-01T10:00:00Z")
        val b = ask("asked").copy(id = 6, askedAt = "2026-11-02T10:00:00Z")
        assertEquals(listOf(6L, 5L), approverSort(ApproverTab.WAITING, listOf(a, b)).map { it.id })
    }

    @Test
    fun waitWordsRoundUp() {
        assertEquals(WaitTime(0, 0, 1), waitTime(30_000))
        assertEquals(WaitTime(0, 0, 5), waitTime(4 * 60_000 + 1))
        assertEquals(WaitTime(0, 2, 30), waitTime(150 * 60_000L))
        assertEquals(WaitTime(2, 3, 0), waitTime(2 * 86_400_000L + 2 * 3_600_000L + 60_000))
    }

    @Test
    fun limitLinesSkipNoLimit() {
        val now = 1_000_000_000_000L
        val limit =
            AskLimit(
                movies = KindLimit(perWeek = 10, used = 3, left = 7),
                shows = KindLimit(perWeek = 2, used = 2, left = 0, retryAt = "2001-09-09T01:46:40Z"),
                music = KindLimit(noLimit = true),
            )
        val lines = limitLines(limit, now = now - 3_600_000)
        assertEquals(listOf(LimitKind.MOVIES, LimitKind.SHOWS), lines.map { it.kind })
        assertEquals(7, lines[0].left)
        assertNull(lines[0].wait)
        assertEquals(WaitTime(0, 1, 0), lines[1].wait)
        assertTrue(limitLines(AskLimit(noLimit = true)).isEmpty())
    }

    @Test
    fun memberRowDisablesOwnedAndAskedLines() {
        val lines =
            listOf(QualityChoice("standard", "", false, 0), QualityChoice("own-1", "HD", false, 7), QualityChoice("4k", "", true, 2))
        val row = memberRow(lines, inLibrary = true)
        assertEquals(listOf(true, true, false), row.pills.map { it.disabled })
        assertEquals(PillNote.IN_LIBRARY, row.pills[0].note)
        assertEquals("4k", row.selected)
        val asked = memberRow(lines, mine = setOf("standard"))
        assertEquals(listOf(PillNote.REQUESTED, PillNote.REQUESTED, PillNote.NONE), asked.pills.map { it.note })
        val joined = memberRow(lines, open = setOf("own-1"))
        assertEquals("own-1", joined.selected)
        assertEquals("4k", memberRow(lines, prefer = "4k").selected)
        assertTrue(row.shown)
        assertFalse(memberRow(lines.take(1)).shown)
        assertEquals(listOf("4k"), alsoPills(memberRow(lines).pills, "standard", pickMany = true).map { it.key })
        assertTrue(alsoPills(memberRow(lines).pills, "standard", pickMany = false).isEmpty())
    }

    @Test
    fun ownerRowKeepsTheAskedLineAndAddsRules() {
        val lines = listOf(QualityChoice("standard", "", false, 1), QualityChoice("4k", "", true, 2))
        val rules =
            listOf(
                PickRule(1, "Standard", listOf("movie")),
                PickRule(6, "Lite", listOf("movie", "show")),
                PickRule(3, "Music", listOf("music")),
            )
        val ask = Ask(kind = "movie", quality = "own-9", qualityName = "Remux")
        val pills = ownerRow(lines, rules, ask) { if (it == "4k") "4K" else "Standard" }
        assertEquals(listOf("standard", "own-9", "rules-6", "4k"), pills.map { it.key })
        assertEquals(6L, pills[2].rulesId)
        assertEquals("standard", pills[2].line)
    }

    @Test
    fun askedLineFallsBackToFourK() {
        assertEquals("4k", Ask(fourK = true).askedLine)
        assertEquals("standard", Ask().askedLine)
        assertNull(Ask(kind = "movie").qualityTagLine)
        assertEquals("4k", Ask(kind = "movie", fourK = true).qualityTagLine)
        assertNull(Ask(kind = "artist", quality = "4k").qualityTagLine)
    }

    @Test
    fun failuresAreRecognised() {
        assertEquals(AskFailure.WEEKLY_LIMIT, askFailure(ApiError(ApiErrorCode.RATE_LIMITED, 429, "you have asked for 10 movies")))
        assertEquals(AskFailure.NEVER_ADD, askFailure(ApiError(ApiErrorCode.CONFLICT, 409, "this movie is on the Never-add list")))
        assertEquals(
            AskFailure.IN_LIBRARY,
            askFailure(ApiError(ApiErrorCode.CONFLICT, 409, "this movie is already in the Library, ready to watch")),
        )
        assertEquals(AskFailure.ALREADY_DECIDED, askFailure(ApiError(ApiErrorCode.CONFLICT, 409, "this ask was already decided")))
        assertEquals(
            AskFailure.NO_MUSIC,
            askFailure(ApiError(ApiErrorCode.FORBIDDEN, 403, "This account cannot ask for music, because music has no age ratings.")),
        )
        assertEquals(
            AskFailure.AGE_LIMIT,
            askFailure(ApiError(ApiErrorCode.FORBIDDEN, 403, "This account can ask only for titles rated up to PG.")),
        )
        assertEquals(AskFailure.OTHER, askFailure(ApiError(ApiErrorCode.SERVER, 500, "boom")))
    }

    @Test
    fun namesDropTheDeletedMark() {
        assertEquals("ana", shownName("ana\u206312"))
        val a = Ask(askedBy = AskWho(1, "ana"), people = listOf(AskAsker(1, "ana"), AskAsker(2, "bo\u20632")))
        assertEquals(listOf("ana", "bo"), a.askerNames)
        assertEquals(listOf("cy"), Ask(askedBy = AskWho(3, "cy")).askerNames)
    }
}
