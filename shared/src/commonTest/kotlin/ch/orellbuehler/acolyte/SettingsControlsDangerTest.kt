package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsDangerPresenter
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsWait
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsControlsDangerTest {
    private class Setup(
        val server: FakeServer,
        val presenter: SettingsControlsDangerPresenter,
        val refreshed: IntArray,
    )

    private fun setup(account: Account = testAccount(Role.ADMIN, listOf("manage_library", "settings"))): Setup {
        val server = FakeServer()
        val client = server.client(listOf(FakeServer.SESSION), csrf = "token")
        val refreshed = intArrayOf(0)
        val presenter =
            SettingsControlsDangerPresenter(Api({ client }, emptyFlow()), { account }, testGate(account), { refreshed[0]++ })
        return Setup(server, presenter, refreshed)
    }

    private fun FakeServer.control(
        path: String,
        what: String,
        done: String,
    ) = on("POST", path) { request ->
        if ((request.body as TextContent).text.contains("\"confirm\":\"k1\"")) {
            respondJson("""{"text":"$done"}""", 202)
        } else {
            respondJson(
                """{"confirm":"k1","expires_in_s":60,"text":"To $what, send the same request again within 60 seconds with {\"confirm\": \"k1\"}."}""",
                202,
            )
        }
    }

    @Test
    fun onlyOwnersAndAdminsMayOpenIt() {
        assertTrue(
            setup()
                .presenter.state.value.allowed,
        )
        assertTrue(
            setup(testAccount(Role.OWNER, emptyList()))
                .presenter.state.value.allowed,
        )
        assertFalse(
            setup(testAccount(Role.MEMBER, listOf("settings")))
                .presenter.state.value.allowed,
        )
    }

    @Test
    fun everyControlAsksFirstAndStopsTheAppOnConfirm() =
        runTest {
            val controls =
                listOf(
                    Triple(SettingsControlsKind.APP_RESTART, "/api/v1/app/restart", "HolyArrs stops now and Docker starts it again."),
                    Triple(SettingsControlsKind.APP_SAFE_MODE, "/api/v1/app/safe-mode", "Safe mode next."),
                    Triple(SettingsControlsKind.APP_SHUTDOWN, "/api/v1/app/shutdown", "HolyArrs shuts down now."),
                )
            for ((kind, path, text) in controls) {
                val s = setup()
                s.server.control(path, "do it", text)
                s.presenter.request(kind)
                val ask = assertNotNull(s.presenter.state.value.ask)
                assertEquals(kind, ask.kind)
                assertEquals("To do it.", ask.text)
                assertNull(s.presenter.state.value.stopped)
                s.presenter.confirm()
                val stopped = assertNotNull(s.presenter.state.value.stopped)
                assertEquals(kind, stopped.kind)
                assertEquals(text, stopped.text)
                assertEquals(SettingsControlsWait.STOPPING, stopped.wait)
                assertNull(s.presenter.state.value.ask)
            }
        }

    @Test
    fun theFirstStepSendsAnEmptyBodyAndTheSecondTheId() =
        runTest {
            val s = setup()
            s.server.control("/api/v1/app/restart", "restart HolyArrs", "bye")
            s.presenter.request(SettingsControlsKind.APP_RESTART)
            assertEquals("{}", s.server.request("POST", "/api/v1/app/restart").body)
            assertEquals("token", s.server.request("POST", "/api/v1/app/restart").headers["X-CSRF-Token"])
            s.presenter.confirm()
            assertEquals("""{"confirm":"k1"}""", s.server.request("POST", "/api/v1/app/restart").body)
        }

    @Test
    fun cancellingAsksNothingMore() =
        runTest {
            val s = setup()
            s.server.control("/api/v1/app/shutdown", "shut HolyArrs down", "bye")
            s.presenter.request(SettingsControlsKind.APP_SHUTDOWN)
            s.presenter.cancel()
            s.presenter.confirm()
            assertNull(s.presenter.state.value.ask)
            assertNull(s.presenter.state.value.stopped)
            assertEquals(1, s.server.recorded().size)
        }

    @Test
    fun otherKindsAreIgnored() =
        runTest {
            val s = setup()
            s.presenter.request(SettingsControlsKind.PART_OFF)
            assertEquals(0, s.server.recorded().size)
        }

    @Test
    fun errorsAreShownAndNothingStops() =
        runTest {
            val s = setup()
            s.server.json("POST", "/api/v1/app/safe-mode", """{"error":"could not arrange safe mode; nothing was restarted"}""", 500)
            s.presenter.request(SettingsControlsKind.APP_SAFE_MODE)
            var state = s.presenter.state.value
            assertNull(state.stopped)
            var notice = assertNotNull(state.notice)
            assertEquals(SettingsControlsKind.APP_SAFE_MODE, notice.kind)
            assertFalse(notice.ok)
            assertEquals(ApiErrorCode.SERVER, notice.error?.code)
            s.server.json("POST", "/api/v1/app/restart", """{"error":"this server cannot restart itself"}""", 503)
            s.presenter.request(SettingsControlsKind.APP_RESTART)
            state = s.presenter.state.value
            notice = assertNotNull(state.notice)
            assertEquals(2, notice.id)
            assertEquals("this server cannot restart itself", notice.error?.message)
            assertFalse(state.busy)
        }

    @Test
    fun anExpiredIdFailsAndTheAskCloses() =
        runTest {
            val s = setup()
            var step = 0
            s.server.on("POST", "/api/v1/app/restart") {
                if (step++ == 0) {
                    respondJson("""{"confirm":"k1","expires_in_s":60,"text":"To restart HolyArrs, send the same request again."}""", 202)
                } else {
                    respondJson("""{"error":"that confirmation id is unknown, used, expired or for another control; ask again"}""", 409)
                }
            }
            s.presenter.request(SettingsControlsKind.APP_RESTART)
            s.presenter.confirm()
            val state = s.presenter.state.value
            assertNull(state.ask)
            assertNull(state.stopped)
            assertEquals(ApiErrorCode.CONFLICT, state.notice?.error?.code)
        }

    @Test
    fun waitsForTheServerToGoAndComeBack() =
        runTest {
            val s = setup()
            s.server.control("/api/v1/app/restart", "restart HolyArrs", "stopping")
            var calls = 0
            s.server.on("GET", "/api/v1/auth/me") {
                when (calls++) {
                    0 -> respondJson(meJson())
                    1, 2 -> error("connection refused")
                    3 -> respondJson("""{"error":"bad gateway"}""", 502)
                    else -> respondJson(meJson())
                }
            }
            s.presenter.request(SettingsControlsKind.APP_RESTART)
            s.presenter.confirm()
            s.presenter.waitForServer()
            val stopped = assertNotNull(s.presenter.state.value.stopped)
            assertEquals(SettingsControlsWait.BACK, stopped.wait)
            assertEquals(5, calls)
            assertEquals(1, s.refreshed[0])
            s.presenter.dismissStopped()
            assertNull(s.presenter.state.value.stopped)
        }

    @Test
    fun aServerThatNeverGoesDownCountsAsBackAfterAWhile() =
        runTest {
            val s = setup()
            s.server.control("/api/v1/app/restart", "restart HolyArrs", "stopping")
            s.server.json("GET", "/api/v1/auth/me", meJson())
            s.presenter.request(SettingsControlsKind.APP_RESTART)
            s.presenter.confirm()
            s.presenter.waitForServer()
            assertEquals(
                SettingsControlsWait.BACK,
                s.presenter.state.value.stopped
                    ?.wait,
            )
            assertEquals(12, s.server.recorded().count { it.path == "/api/v1/auth/me" })
        }
}
