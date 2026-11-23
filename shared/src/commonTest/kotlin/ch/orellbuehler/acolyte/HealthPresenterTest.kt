package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.health.HealthPresenter
import ch.orellbuehler.acolyte.feature.health.tone
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthPresenterTest {
    private val checks =
        """{"checks":[
            {"id":4,"key":"backups","kind":"backups","subject":"Backups","state":"warning","confirmed":"warning",
             "message":"No backup yet","since":"2026-11-23T22:27:12Z","checked_at":"2026-11-23T22:27:12Z","new_field":1},
            {"id":5,"key":"disk_space:1","kind":"disk_space","subject":"Disk","state":"ok","message":null}
        ]}"""

    @Test
    fun loadsChecks() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/health/checks", checks)
            val presenter = HealthPresenter(server.api())
            assertEquals(Phase.LOADING, presenter.state.value.status.phase)
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals(listOf("Backups", "Disk"), state.checks.map { it.subject })
            assertEquals("", state.checks[1].message)
            assertEquals(Tone.WAIT, state.checks[0].tone)
            assertEquals(Tone.WAIT, state.overall)
            assertFalse(state.isEmpty)
            assertEquals("en", server.request("GET", "/api/v1/health/checks").headers["X-HolyArrs-Language"])
        }

    @Test
    fun emptyListIsReady() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/health/checks", """{"checks":null}""")
            val presenter = HealthPresenter(server.api())
            presenter.load()
            assertTrue(presenter.state.value.isEmpty)
            assertEquals(Tone.IDLE, presenter.state.value.overall)
        }

    @Test
    fun firstFailureShowsError() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/health/checks", """{"error":"your account may not do this; ask the owner"}""", 403)
            val presenter = HealthPresenter(server.api())
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.FAILED, state.status.phase)
            assertEquals(ApiErrorCode.FORBIDDEN, state.status.error?.code)
            assertEquals("your account may not do this; ask the owner", state.status.error?.message)
        }

    @Test
    fun failedRefreshKeepsData() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/health/checks", checks)
            val presenter = HealthPresenter(server.api())
            presenter.load()
            server.json("GET", "/api/v1/health/checks", """{"error":"boom"}""", 500)
            presenter.refresh()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertFalse(state.status.refreshing)
            assertEquals(ApiErrorCode.SERVER, state.status.error?.code)
            assertEquals(2, state.checks.size)
        }

    @Test
    fun runChecksSendsCsrf() =
        runTest {
            val server = FakeServer()
            server.json("POST", "/api/v1/health/checks/run", checks)
            val presenter = HealthPresenter(server.api(server.client(listOf(FakeServer.SESSION), csrf = "token")))
            presenter.runChecks()
            assertEquals("token", server.request("POST", "/api/v1/health/checks/run").headers["X-CSRF-Token"])
            assertEquals(2, presenter.state.value.checks.size)
            assertNull(presenter.state.value.actionError)
        }

    @Test
    fun staleCsrfIsRefreshedOnce() =
        runTest {
            val server = FakeServer()
            var calls = 0
            server.on("POST", "/api/v1/health/checks/run") { request ->
                calls++
                if (request.headers["X-CSRF-Token"] ==
                    "fresh"
                ) {
                    respondJson(checks)
                } else {
                    respondJson("""{"error":"missing or wrong CSRF token"}""", 403)
                }
            }
            server.json("GET", "/api/v1/auth/me", meJson(csrf = "fresh"))
            val client = server.client(listOf(FakeServer.SESSION), csrf = "stale")
            val presenter = HealthPresenter(server.api(client))
            presenter.runChecks()
            assertEquals(2, calls)
            assertEquals("fresh", client.credentials.csrf)
            assertNull(presenter.state.value.actionError)
        }

    @Test
    fun offlineIsReported() =
        runTest {
            val server = FakeServer()
            server.on("GET", "/api/v1/health/checks") { throw IllegalStateException("Unable to resolve host") }
            val presenter = HealthPresenter(server.api())
            presenter.load()
            assertEquals(
                ApiErrorCode.OFFLINE,
                presenter.state.value.status.error
                    ?.code,
            )
        }
}

class ApiErrorTest {
    @Test
    fun readsServerErrorFields() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/health/checks", """{"error":"this part is off","part":"media","text":"Movies is off."}""", 409)
            val presenter = HealthPresenter(server.api())
            presenter.load()
            val error = presenter.state.value.status.error
            assertEquals(ApiErrorCode.PART_OFF, error?.code)
            assertEquals("media", error?.part)
            assertEquals("Movies is off.", error?.text)
        }

    @Test
    fun reauthIsItsOwnCode() =
        runTest {
            val server = FakeServer()
            server.json(
                "POST",
                "/api/v1/health/checks/run",
                """{"error":"confirm your local password again before using this route","code":"reauth"}""",
                403,
            )
            val presenter = HealthPresenter(server.api())
            presenter.runChecks()
            assertEquals(
                ApiErrorCode.REAUTH,
                presenter.state.value.actionError
                    ?.code,
            )
        }
}
