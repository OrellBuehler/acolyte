package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.ClearKind
import ch.orellbuehler.acolyte.feature.requests.KindFilter
import ch.orellbuehler.acolyte.feature.requests.MemberTab
import ch.orellbuehler.acolyte.feature.requests.PendingKind
import ch.orellbuehler.acolyte.feature.requests.RequestsInbox
import ch.orellbuehler.acolyte.feature.requests.RequestsNoteKind
import ch.orellbuehler.acolyte.feature.requests.RequestsPresenter
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun testAccount(
    role: Role = Role.ADMIN,
    perms: List<String> = listOf("ask", "approve", "manage_library"),
    kid: Boolean = false,
): Account =
    Account(
        serverUrl = BASE,
        serverName = "media.example.com",
        method = SignInMethod.PASSWORD,
        userId = 1,
        userName = "ana",
        displayName = "Ana",
        role = role,
        permissions = perms,
        parts =
            listOf(
                PartView("movies", "Movies", true),
                PartView("shows", "TV shows", true),
                PartView("music", "Music", true),
                PartView("wishlist", "Requests", true),
            ),
        serverVersion = null,
        avatarPath = null,
        mustSetUpTwoFactor = false,
        isKid = kid,
        safeMode = false,
    )

internal fun askJson(
    id: Long,
    state: String = "asked",
    kind: String = "movie",
    name: String = "Movie $id",
    titleId: Long? = null,
    mine: Boolean = true,
    extra: String = "",
    seasons: String = "[]",
): String =
    """{"id":$id,"kind":"$kind","tmdb_id":"${100 + id}","ref":"tmdb:${100 + id}","seasons":$seasons,""" +
        """"episodes":[],"future_only":false,"four_k":false,""" +
        """"quality":"standard","quality_name":"","albums":[],"name":"$name","year":2024,"state":"$state","state_label":"x",""" +
        (titleId?.let { "\"title_id\":$it," } ?: "") +
        """"asked_by":{"id":2,"name":"bo"},"asked_at":"2026-11-2${id % 10}T10:00:00Z",""" +
        """"people":[{"id":2,"name":"bo","asked_at":"2026-11-20T10:00:00Z"}],""" +
        """"auto_approved":false,"mine":$mine$extra}"""

class RequestsPresenterTest {
    private fun TestScope.presenter(
        server: FakeServer,
        account: Account = testAccount(),
        api: Api = server.api(),
    ): Pair<RequestsPresenter, RequestsInbox> {
        val inbox = RequestsInbox(api) { account }
        return RequestsPresenter(api, { account }, inbox, backgroundScope) to inbox
    }

    private fun FakeServer.asks(
        vararg asks: String,
        counts: String = """{"pending":1,"processing":1,"done":1,"failed":0}""",
        total: Int = asks.size,
    ) = json("GET", "/api/v1/asks", """{"asks":[${asks.joinToString(",")}],"counts":$counts,"total":$total}""")

    private fun FakeServer.common() {
        json("GET", "/api/v1/reports", """{"open":2,"reports":[]}""")
        json("GET", "/api/v1/asks/limit", """{"no_limit":false,"movies":{"per_week":5,"used":1,"left":4}}""")
        json("GET", "/api/v1/messages", """{"messages":[{"id":9,"kind":"approved","text":"x","ask_id":7}],"unread":1,"waiting":3}""")
        on("POST", "/api/v1/messages/read") { respondJson("", 204) }
    }

    @Test
    fun approverSeesTabsAndCounts() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1), askJson(2, "approved", titleId = 5), askJson(3, "turned_down"))
            val (presenter, inbox) = presenter(server)
            inbox.refresh()
            assertEquals(3, inbox.state.value.requestsBadge)
            assertEquals(3, inbox.badge(Section.REQUESTS))
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.approver)
            assertEquals(2, s.reportsOpen)
            assertEquals(mapOf(ApproverTab.WAITING to 1, ApproverTab.PROCESSING to 1, ApproverTab.DONE to 1), s.approverCounts)
            assertEquals(listOf(1L), s.shown.map { it.id })
            assertEquals(setOf(7L), s.newIds)
            assertEquals(0, inbox.state.value.unread)
            server.request("POST", "/api/v1/messages/read")
            assertEquals("limit=200", server.request("GET", "/api/v1/asks").query)
            presenter.setApproverTab(ApproverTab.DONE)
            assertEquals(
                listOf(3L),
                presenter.state.value.shown
                    .map { it.id },
            )
        }

    @Test
    fun approveWaitsForTheUndoWindow() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1), askJson(2))
            server.json("POST", "/api/v1/asks/1/approve", """{"ask":${askJson(1, "approved", titleId = 9)},"notice":""}""")
            val (presenter, _) = presenter(server)
            presenter.load()
            presenter.approve(1)
            var s = presenter.state.value
            assertEquals(PendingKind.APPROVE, s.pending[1])
            assertEquals(RequestsNoteKind.APPROVED, s.note?.kind)
            assertTrue(s.note?.undoable == true)
            assertEquals(listOf(2L), s.shown.map { it.id })
            assertTrue(server.requests.none { it.path == "/api/v1/asks/1/approve" })
            presenter.idle()
            s = presenter.state.value
            assertTrue(s.pending.isEmpty())
            assertEquals("approved", s.asks.first { it.id == 1L }.state)
            assertEquals(9L, s.asks.first { it.id == 1L }.titleId)
            val body = Json.parseToJsonElement(server.request("POST", "/api/v1/asks/1/approve").body.orEmpty()) as JsonObject
            assertEquals(setOf("note", "library_folder_id", "pick_rules_id"), body.keys)
        }

    @Test
    fun undoSendsNothing() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1))
            val (presenter, _) = presenter(server)
            presenter.load()
            presenter.decline(1, "no")
            advanceTimeBy(3_000)
            presenter.undo(
                presenter.state.value.note!!
                    .keys,
            )
            presenter.idle()
            assertTrue(server.requests.none { it.path.endsWith("/turn-down") })
            assertTrue(
                presenter.state.value.pending
                    .isEmpty(),
            )
            assertNull(presenter.state.value.note)
            assertEquals(
                "asked",
                presenter.state.value.asks
                    .single()
                    .state,
            )
        }

    @Test
    fun declineSendsTheTrimmedReason() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1))
            server.json("POST", "/api/v1/asks/1/turn-down", """{"ask":${askJson(1, "turned_down")}}""")
            val (presenter, _) = presenter(server)
            presenter.load()
            presenter.decline(1, "  Already have it  ")
            assertEquals(PendingKind.DECLINE, presenter.state.value.pending[1])
            presenter.idle()
            assertEquals("""{"note":"Already have it"}""", server.request("POST", "/api/v1/asks/1/turn-down").body)
            assertEquals(
                "turned_down",
                presenter.state.value.asks
                    .single()
                    .state,
            )
        }

    @Test
    fun anotherApproverWasFirst() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1))
            server.json("POST", "/api/v1/asks/1/approve", """{"error":"this ask was already decided"}""", 409)
            server.json(
                "GET",
                "/api/v1/asks/1",
                askJson(1, "turned_down", extra = ""","decided_by":{"id":3,"name":"cy","display_name":"Cyrus"}"""),
            )
            val (presenter, _) = presenter(server)
            presenter.load()
            presenter.approve(1)
            presenter.flush()
            presenter.idle()
            val s = presenter.state.value
            assertEquals(RequestsNoteKind.ALREADY_DECIDED, s.note?.kind)
            assertEquals("Cyrus", s.note?.by)
            assertEquals("turned_down", s.asks.single().state)
        }

    @Test
    fun failedApproveReturnsTheCard() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1))
            server.json("POST", "/api/v1/asks/1/approve", """{"error":"boom"}""", 500)
            val (presenter, _) = presenter(server)
            presenter.load()
            presenter.approve(1)
            presenter.idle()
            val s = presenter.state.value
            assertEquals(RequestsNoteKind.APPROVE_FAILED, s.note?.kind)
            assertEquals(listOf(1L), s.shown.map { it.id })
        }

    @Test
    fun approveAllHoldsEveryWaitingAsk() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(
                askJson(1),
                askJson(2),
                askJson(3, "approved", titleId = 3),
                counts = """{"pending":2,"processing":1,"done":0,"failed":0}""",
            )
            server.json("POST", "/api/v1/asks/1/approve", """{"ask":${askJson(1, "approved", titleId = 1)}}""")
            server.json("POST", "/api/v1/asks/2/approve", """{"ask":${askJson(2, "approved", titleId = 2)}}""")
            val (presenter, _) = presenter(server)
            presenter.load()
            assertTrue(presenter.state.value.canApproveAll)
            presenter.approveAll()
            assertEquals(
                RequestsNoteKind.APPROVED_MANY,
                presenter.state.value.note
                    ?.kind,
            )
            assertEquals(
                2,
                presenter.state.value.note
                    ?.count,
            )
            presenter.idle()
            assertEquals(2, server.requests.count { it.path.endsWith("/approve") })
            assertEquals(0, presenter.state.value.approverCounts[ApproverTab.WAITING])
        }

    @Test
    fun clearFailedUsesTheFilter() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1, "approved"), counts = """{"pending":0,"processing":1,"done":0,"failed":1}""")
            server.json("POST", "/api/v1/asks/remove", """{"removed":1}""")
            val (presenter, _) = presenter(server)
            presenter.load()
            presenter.setApproverTab(ApproverTab.PROCESSING)
            assertEquals(ClearKind.FAILED, presenter.state.value.clearable)
            assertEquals(1, presenter.state.value.clearCount)
            presenter.clear(ClearKind.FAILED)
            assertEquals("""{"filter":"failed"}""", server.request("POST", "/api/v1/asks/remove").body)
            assertEquals(
                RequestsNoteKind.REMOVED,
                presenter.state.value.note
                    ?.kind,
            )
        }

    @Test
    fun membersSeeTheirTabsAndLimits() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1, "available", titleId = 1), askJson(2, "approved"), askJson(3, "approved", titleId = 3, kind = "show"))
            val (presenter, _) = presenter(server, testAccount(Role.MEMBER, listOf("ask")))
            presenter.load()
            val s = presenter.state.value
            assertFalse(s.approver)
            assertEquals(-1, s.reportsOpen)
            assertEquals(MemberTab.APPROVED, s.memberTab)
            assertEquals(listOf(MemberTab.WAITING, MemberTab.APPROVED, MemberTab.DECLINED, MemberTab.READY, MemberTab.FAILED), s.memberTabs)
            assertEquals(1, s.memberCounts[MemberTab.FAILED])
            assertEquals(listOf(KindFilter.ALL, KindFilter.MOVIE, KindFilter.SHOW), s.kinds)
            assertEquals(4, s.limits.single().left)
            presenter.setKind(KindFilter.SHOW)
            assertEquals(
                listOf(3L),
                presenter.state.value.shown
                    .map { it.id },
            )
        }

    @Test
    fun takeBackDeletesTheAsk() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1))
            server.on("DELETE", "/api/v1/asks/1") { respondJson("", 204) }
            val (presenter, _) = presenter(server, testAccount(Role.MEMBER, listOf("ask")))
            presenter.load()
            assertNull(presenter.takeBack(1))
            assertEquals(
                RequestsNoteKind.TAKEN_BACK,
                presenter.state.value.note
                    ?.kind,
            )
            server.request("DELETE", "/api/v1/asks/1")
        }

    @Test
    fun liveAsksPatchTheList() =
        runTest {
            val server = FakeServer()
            server.common()
            server.asks(askJson(1), askJson(2))
            val events =
                flowOf(
                    LiveEvent("ask", Json.parseToJsonElement(askJson(1, "approved", titleId = 4)), "b-1"),
                    LiveEvent("ask", Json.parseToJsonElement(askJson(5)), "b-2"),
                    LiveEvent("ask_removed", Json.parseToJsonElement("""{"id":2}"""), "b-3"),
                )
            val client = server.client(listOf(FakeServer.SESSION))
            val (presenter, _) = presenter(server, api = Api({ client }, events))
            presenter.load()
            backgroundScope.launch { presenter.watch() }
            runCurrent()
            val asks = presenter.state.value.asks
            assertEquals(listOf(5L, 1L), asks.map { it.id })
            assertEquals("approved", asks[1].state)
        }
}
