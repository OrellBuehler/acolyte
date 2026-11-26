package ch.orellbuehler.acolyte.tv

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.navigation.compose.rememberNavController
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.AskAsker
import ch.orellbuehler.acolyte.api.model.AskCounts
import ch.orellbuehler.acolyte.api.model.CalendarRow
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.downloads.BarTone
import ch.orellbuehler.acolyte.feature.downloads.CalendarDay
import ch.orellbuehler.acolyte.feature.downloads.CalendarFollows
import ch.orellbuehler.acolyte.feature.downloads.CalendarGroup
import ch.orellbuehler.acolyte.feature.downloads.CalendarState
import ch.orellbuehler.acolyte.feature.downloads.CalendarType
import ch.orellbuehler.acolyte.feature.downloads.ClientNote
import ch.orellbuehler.acolyte.feature.downloads.ClientNoteKind
import ch.orellbuehler.acolyte.feature.downloads.Day
import ch.orellbuehler.acolyte.feature.downloads.DownloadItem
import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import ch.orellbuehler.acolyte.feature.downloads.DownloadsState
import ch.orellbuehler.acolyte.feature.downloads.DownloadsSub
import ch.orellbuehler.acolyte.feature.downloads.DownloadsTab
import ch.orellbuehler.acolyte.feature.downloads.EntryDot
import ch.orellbuehler.acolyte.feature.downloads.EntryTag
import ch.orellbuehler.acolyte.feature.downloads.EntryWhen
import ch.orellbuehler.acolyte.feature.downloads.EpisodeSpan
import ch.orellbuehler.acolyte.feature.downloads.Month
import ch.orellbuehler.acolyte.feature.downloads.QueueStage
import ch.orellbuehler.acolyte.feature.downloads.StatusLine
import ch.orellbuehler.acolyte.feature.downloads.StatusWord
import ch.orellbuehler.acolyte.feature.downloads.SubCount
import ch.orellbuehler.acolyte.feature.downloads.SubLine
import ch.orellbuehler.acolyte.feature.downloads.SubLineKind
import ch.orellbuehler.acolyte.feature.downloads.TabCount
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.KindFilter
import ch.orellbuehler.acolyte.feature.requests.LimitKind
import ch.orellbuehler.acolyte.feature.requests.LimitLine
import ch.orellbuehler.acolyte.feature.requests.MemberTab
import ch.orellbuehler.acolyte.feature.requests.RequestsNote
import ch.orellbuehler.acolyte.feature.requests.RequestsNoteKind
import ch.orellbuehler.acolyte.feature.requests.RequestsState
import ch.orellbuehler.acolyte.feature.requests.Stage
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import ch.orellbuehler.acolyte.tv.calendar.TvCalendarActions
import ch.orellbuehler.acolyte.tv.calendar.TvCalendarContent
import ch.orellbuehler.acolyte.tv.downloads.TvDownloadsActions
import ch.orellbuehler.acolyte.tv.downloads.TvDownloadsContent
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.nav.LocalTvSignOut
import ch.orellbuehler.acolyte.tv.nav.TvNavigator
import ch.orellbuehler.acolyte.tv.nav.TvSections
import ch.orellbuehler.acolyte.tv.nav.TvTabSelection
import ch.orellbuehler.acolyte.tv.requests.TvRequestsActions
import ch.orellbuehler.acolyte.tv.requests.TvRequestsContent
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvTheme
import ch.orellbuehler.acolyte.ui.LocalSdk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class TvActivityScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val sdk = AcolyteSdk(MemoryStore(), "test")

    private val account =
        Account(
            serverUrl = "http://holyarrs.local:6262",
            serverName = "holyarrs.local",
            method = SignInMethod.PASSWORD,
            userId = 1,
            userName = "anna",
            displayName = "Anna",
            role = Role.OWNER,
            permissions = emptyList(),
            parts = emptyList<PartView>(),
            serverVersion = null,
            avatarPath = null,
            mustSetUpTwoFactor = false,
            isKid = false,
            safeMode = false,
        )

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            val nav = rememberNavController()
            val selection = remember { TvTabSelection(TvSections.tabs(account)) }
            val navigator = remember { TvNavigator(account, nav, selection) }
            TvTheme {
                CompositionLocalProvider(LocalSdk provides sdk, LocalTvNavigator provides navigator, LocalTvSignOut provides {}) {
                    Box(Modifier.fillMaxSize().background(TvColors.background)) { content() }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun shoot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File("build/tv-screens/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    private fun press(text: String) {
        val node = compose.onAllNodesWithText(text).onFirst()
        node.requestFocus()
        compose.waitForIdle()
        node.assertIsFocused()
        node.performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private class RequestsLog : TvRequestsActions {
        val calls = mutableListOf<String>()

        override fun refresh() {
            calls += "refresh"
        }

        override fun loadMore() {
            calls += "more"
        }

        override fun setMemberTab(tab: MemberTab) {
            calls += "member:$tab"
        }

        override fun setApproverTab(tab: ApproverTab) {
            calls += "approver:$tab"
        }

        override fun setKind(kind: KindFilter) {
            calls += "kind:$kind"
        }

        override fun setStage(stage: Stage?) {
            calls += "stage:$stage"
        }

        override fun clearFilters() {
            calls += "clear"
        }

        override fun approve(id: Long) {
            calls += "approve:$id"
        }

        override fun decline(id: Long) {
            calls += "decline:$id"
        }

        override fun approveAll() {
            calls += "approveAll"
        }

        override fun undo(keys: List<Long>) {
            calls += "undo:$keys"
        }

        override fun takeBack(id: Long) {
            calls += "takeBack:$id"
        }

        override fun dismissNote() {
            calls += "dismiss"
        }
    }

    private fun ask(
        id: Long,
        name: String,
        state: String,
        kind: String = "movie",
        mine: Boolean = true,
        problem: String = "",
        titleId: Long = 0,
    ) = Ask(
        id = id,
        kind = kind,
        tmdbId = "t$id",
        name = name,
        year = 2020 + id.toInt(),
        state = state,
        mine = mine,
        titleId = titleId,
        problem = problem,
        askedAt = "2026-11-20T10:00:00Z",
        people = listOf(AskAsker(1, "anna", "Anna", "2026-11-20T10:00:00Z")),
    )

    private val memberAsks =
        listOf(
            ask(1, "The Quiet Harbour", "asked"),
            ask(2, "Salt and Iron", "asked", kind = "show"),
            ask(3, "Northern Lights", "asked", mine = false),
        )

    private fun memberState(
        asks: List<Ask> = memberAsks,
        note: RequestsNote? = null,
    ) = RequestsState(
        status = LoadStatus(Phase.READY),
        canAsk = true,
        myId = 1,
        asks = asks,
        total = asks.size,
        limits = listOf(LimitLine(LimitKind.MOVIES, 3, 5, null)),
        note = note,
    )

    @Test
    fun requestsListTabsAndTakeBackMenu() {
        val log = RequestsLog()
        show { TvRequestsContent(memberState(), log, canSearch = true) }
        compose.onNodeWithText("The Quiet Harbour").assertIsDisplayed()
        compose.onNodeWithText("Ask for something").assertIsDisplayed()
        shoot("requests-member")
        press("Approved")
        assertEquals(listOf("member:APPROVED"), log.calls)
        compose.onNodeWithText("The Quiet Harbour").requestFocus()
        compose.waitForIdle()
        shoot("requests-focused")
        compose.onNodeWithText("The Quiet Harbour").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        compose.onNodeWithText("Take back").assertIsDisplayed()
        shoot("requests-menu")
        press("Take back")
        assertEquals("takeBack:1", log.calls.last())
    }

    @Test
    fun requestsApproverApprovesAllAfterConfirming() {
        val log = RequestsLog()
        val asks = (1..4).map { ask(it.toLong(), "Waiting $it", "asked", mine = false) }
        val state =
            RequestsState(
                status = LoadStatus(Phase.READY),
                approver = true,
                canAsk = true,
                asks = asks,
                total = 6,
                counts = AskCounts(pending = 4),
                listed = AskCounts(pending = 4),
            )
        show { TvRequestsContent(state, log, canSearch = true) }
        compose.onNodeWithText("Approve all (4)").assertIsDisplayed()
        shoot("requests-approver")
        press("Approve all (4)")
        compose.onNodeWithText("Approve all waiting requests (4)?").assertIsDisplayed()
        shoot("requests-confirm")
        press("Approve 4")
        assertEquals("approveAll", log.calls.last())
    }

    @Test
    fun requestsNoteBannerUndoes() {
        val log = RequestsLog()
        val note = RequestsNote(id = 1, kind = RequestsNoteKind.APPROVED, name = "Salt and Iron", keys = listOf(2))
        show { TvRequestsContent(memberState(note = note), log, canSearch = false) }
        compose.onNodeWithText("Undo").assertIsDisplayed()
        shoot("requests-note")
        press("Undo")
        assertEquals("undo:[2]", log.calls.last())
    }

    @Test
    fun requestsEmptyOffersToAsk() {
        val state = RequestsState(status = LoadStatus(Phase.READY), canAsk = true)
        show { TvRequestsContent(state, RequestsLog(), canSearch = true) }
        compose.onNodeWithText("Nothing on the Requests page yet").assertIsDisplayed()
        compose.onNodeWithText("Ask for something").assertIsDisplayed()
        shoot("requests-empty")
    }

    @Test
    fun requestsGerman() {
        RuntimeEnvironment.setQualifiers("+de")
        show { TvRequestsContent(memberState(), RequestsLog(), canSearch = true) }
        compose.onNodeWithText("The Quiet Harbour").assertIsDisplayed()
        shoot("requests-de")
    }

    @Test
    fun requestsFailedLoadOffersRetry() {
        val log = RequestsLog()
        val state = RequestsState(status = LoadStatus(Phase.FAILED, error = ApiError(ApiErrorCode.OFFLINE, 0)))
        show { TvRequestsContent(state, log, canSearch = false) }
        press("Try again")
        assertEquals(listOf("refresh"), log.calls)
    }

    private class CalendarLog : TvCalendarActions {
        val calls = mutableListOf<String>()

        override fun refresh() {
            calls += "refresh"
        }

        override fun previous() {
            calls += "previous"
        }

        override fun next() {
            calls += "next"
        }

        override fun today() {
            calls += "today"
        }

        override fun setType(type: CalendarType) {
            calls += "type:$type"
        }

        override fun setFollows(follows: CalendarFollows) {
            calls += "follows:$follows"
        }

        override fun link(group: CalendarGroup): DownloadTitleLink? {
            calls += "link:${group.key}"
            return null
        }
    }

    private fun group(
        key: String,
        title: String,
        day: Day,
        dot: EntryDot,
    ) = CalendarGroup(
        key = key,
        day = day,
        lead = CalendarRow(titleId = 1, name = title, kind = "episode"),
        entries = emptyList(),
        kind = "episode",
        title = title,
        tag = EntryTag.NEW_EPISODE,
        count = 1,
        movieKinds = emptyList(),
        whenKind = EntryWhen.ALL_DAY,
        minutes = 0,
        span = EpisodeSpan.ONE,
        season = 2,
        episode = 4,
        season2 = 0,
        episode2 = 0,
        episodeName = "The Long Tide",
        network = "Coastline",
        caption = "",
        album = "",
        dot = dot,
        dots = listOf(dot),
        art = null,
        poster = null,
        following = true,
        followers = emptyList(),
        canSearch = false,
    )

    private fun calendarState(hidden: Int = 3): CalendarState {
        val today = Day(2026, 11, 26)
        val next = Day(2026, 11, 27)
        return CalendarState(
            status = LoadStatus(Phase.READY),
            month = Month(2026, 11),
            today = today,
            people = 2,
            hidden = hidden,
            days =
                listOf(
                    CalendarDay(
                        today,
                        listOf(
                            group("a", "Tidewater", today, EntryDot.HAVE),
                            group("b", "Salt and Iron", today, EntryDot.GETTING),
                            group("c", "Northern Lights", today, EntryDot.UPCOMING),
                        ),
                    ),
                    CalendarDay(next, listOf(group("d", "Lighthouse Keepers", next, EntryDot.MISSING))),
                ),
        )
    }

    @Test
    fun calendarShowsMonthFiltersAndDays() {
        val log = CalendarLog()
        show { TvCalendarContent(calendarState(), log) }
        compose.onNodeWithText("November 2026").assertIsDisplayed()
        compose.onNodeWithText("Tidewater").assertIsDisplayed()
        shoot("calendar")
        press("Month after")
        press("Month before")
        press("Today")
        press("TV shows")
        press("Everyone\u0027s follows")
        press("Show the Whole Library")
        compose.onNodeWithText("T").requestFocus()
        compose.waitForIdle()
        shoot("calendar-focused")
        compose.onNodeWithText("T").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        assertEquals(
            listOf("next", "previous", "today", "type:SHOWS", "follows:EVERYONE", "follows:LIBRARY", "link:a"),
            log.calls,
        )
    }

    @Test
    fun calendarDisablesBoundsAndShowsEmptyMonth() {
        val state = CalendarState(status = LoadStatus(Phase.READY), month = Month(2026, 11), today = Day(2026, 11, 26), canNext = false)
        show { TvCalendarContent(state, CalendarLog()) }
        compose.onNodeWithText("Nothing coming up").assertIsDisplayed()
        compose.onNodeWithText("Month after").assertIsNotEnabled()
        shoot("calendar-empty")
    }

    @Test
    fun calendarGerman() {
        RuntimeEnvironment.setQualifiers("+de")
        show { TvCalendarContent(calendarState(), CalendarLog()) }
        compose.onNodeWithText("Tidewater").assertIsDisplayed()
        shoot("calendar-de")
    }

    private class DownloadsLog : TvDownloadsActions {
        val calls = mutableListOf<String>()

        override fun refresh() {
            calls += "refresh"
        }

        override fun setTab(tab: DownloadsTab) {
            calls += "tab:$tab"
        }

        override fun setSub(sub: DownloadsSub) {
            calls += "sub:$sub"
        }

        override fun showMore() {
            calls += "more"
        }

        override fun openTitle(
            titleId: Long,
            open: (DownloadTitleLink) -> Unit,
        ) {
            calls += "open:$titleId"
        }
    }

    private fun download(
        key: String,
        title: String,
        word: StatusWord,
        tone: Tone,
        progress: Double,
        bar: BarTone,
    ) = DownloadItem(
        key = key,
        row = DownloadRow(id = key.hashCode().toLong(), titleId = 7, size = 4_500_000_000, speed = 12_000_000, eta = 540, active = true),
        stage = QueueStage.DOWNLOADING,
        name = title,
        title = title,
        subLine = SubLine(SubLineKind.SEASON, from = 2),
        word = word,
        note = null,
        problem = null,
        detail = null,
        bar = bar,
        tone = tone,
        progress = progress,
        moving = true,
        history = false,
        poster = null,
        square = false,
        quality = "1080p",
        protocol = "torrent",
        actions = emptyList(),
        decisions = emptyList(),
        selectable = false,
    )

    private fun downloadsState(manager: Boolean = true) =
        DownloadsState(
            status = LoadStatus(Phase.READY),
            manager = manager,
            total = 3,
            tabs =
                listOf(
                    TabCount(DownloadsTab.ALL, 3),
                    TabCount(DownloadsTab.WAITING, 1),
                    TabCount(DownloadsTab.PROGRESS, 2),
                    TabCount(DownloadsTab.PROBLEMS, 0),
                    TabCount(DownloadsTab.DONE, 0),
                ),
            subs = listOf(SubCount(DownloadsSub.DOWNLOADING, 2), SubCount(DownloadsSub.CHECKING, 0)),
            items =
                listOf(
                    download("a", "Salt and Iron", StatusWord.DOWNLOADING, Tone.INFO, 0.42, BarTone.GO),
                    download("b", "The Quiet Harbour", StatusWord.QUEUED, Tone.WAIT, 0.0, BarTone.QUEUED),
                    download("c", "Northern Lights", StatusWord.DOWNLOADING, Tone.INFO, 0.9, BarTone.GO),
                ),
            notes = listOf(ClientNote("qBittorrent", ClientNoteKind.OFFLINE)),
            statusLine = StatusLine(speed = 12_000_000, upSpeed = 0, free = 120_000_000_000, left = 540, review = 2, limited = false),
            moreOutside = 4,
        )

    @Test
    fun downloadsShowsTabsCardsAndOpensTitle() {
        val log = DownloadsLog()
        show { TvDownloadsContent(downloadsState(), log) }
        compose.onNodeWithText("Salt and Iron").assertIsDisplayed()
        compose.onNodeWithText("2 waiting for a decision. Decide on your phone.").assertIsDisplayed()
        shoot("downloads")
        press("In progress  2")
        press("Downloading  2")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Show more torrents (4 not loaded)"))
        press("Show more torrents (4 not loaded)")
        compose.onNodeWithText("Salt and Iron").requestFocus()
        compose.waitForIdle()
        shoot("downloads-focused")
        compose.onNodeWithText("Salt and Iron").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        assertEquals(listOf("tab:PROGRESS", "sub:DOWNLOADING", "more"), log.calls.take(3))
        assertTrue(log.calls.last().startsWith("open:"))
    }

    @Test
    fun downloadsEmptyStates() {
        val empty = DownloadsState(status = LoadStatus(Phase.READY), manager = false)
        show { TvDownloadsContent(empty, DownloadsLog()) }
        compose.onNodeWithText("Nothing on its way").assertIsDisplayed()
        shoot("downloads-empty")
    }

    @Test
    fun downloadsGerman() {
        RuntimeEnvironment.setQualifiers("+de")
        show { TvDownloadsContent(downloadsState(manager = false), DownloadsLog()) }
        compose.onNodeWithText("Salt and Iron").assertIsDisplayed()
        shoot("downloads-de")
    }
}
