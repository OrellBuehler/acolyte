package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.SessionState
import ch.orellbuehler.acolyte.session.SignInErrorCode
import ch.orellbuehler.acolyte.session.SignInMethod
import ch.orellbuehler.acolyte.session.SignInStep
import ch.orellbuehler.acolyte.session.SignInWay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignInFlowTest {
    private val stateJson =
        """{"needs_setup":false,"ways":["password","plex","jellyfin"],""" +
            """"min_password_length":8,"host_refused":"nas.lan"}"""

    @Test
    fun passwordWithTwoFactorSignsIn() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", stateJson)
            server.json("POST", "/api/v1/auth/login", """{"two_factor":true,"ticket":"t-1"}""")
            server.json(
                "POST",
                "/api/v1/auth/login/two-factor",
                """{"name":"admin","csrf":"csrf-login"}""",
                200,
                SET_COOKIE to "holyarrs_session_6262=abc; Path=/holyarrs/; Max-Age=2592000; HttpOnly; SameSite=Lax",
            )
            server.bootstrapRoutes()
            val store = MemoryStore()
            val sdk = server.sdk(backgroundScope, store)
            sdk.session.restore()
            assertIs<SessionState.SignedOut>(sdk.session.state.value)

            val flow = sdk.signIn()
            flow.connect("https://media.example.com/holyarrs/login")
            assertEquals(SignInStep.CREDENTIALS, flow.state.value.step)
            assertEquals(BASE, flow.state.value.serverUrl)
            assertEquals(listOf(SignInWay.PASSWORD, SignInWay.PLEX, SignInWay.JELLYFIN), flow.state.value.ways)
            assertEquals(emptyList(), flow.state.value.otherWays)
            assertEquals("nas.lan", flow.state.value.hostRefused)

            flow.signIn(" admin ", "secret")
            assertEquals(SignInStep.TWO_FACTOR, flow.state.value.step)
            assertEquals("""{"name":"admin","password":"secret"}""", server.request("POST", "/api/v1/auth/login").body)

            flow.verify("123 456")
            assertEquals(SignInStep.DONE, flow.state.value.step)
            assertEquals("""{"ticket":"t-1","code":"123456"}""", server.request("POST", "/api/v1/auth/login/two-factor").body)

            val signedIn = assertIs<SessionState.SignedIn>(sdk.session.state.value)
            val account = signedIn.account
            assertEquals("The Admin", account.displayName)
            assertEquals("1.3.0", account.serverVersion)
            assertEquals(SignInMethod.PASSWORD, account.method)
            assertTrue(Section.HEALTH in account.sections)
            assertTrue(Section.MUSIC !in account.sections)
            assertTrue(Section.MOVIES in account.sections)
            assertEquals("holyarrs_session_6262=abc", server.request("GET", "/api/v1/auth/me").headers["Cookie"])
            assertTrue(store.read("connection")!!.contains("csrf-me"))
        }

    @Test
    fun wrongPasswordShowsError() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", stateJson)
            server.json("POST", "/api/v1/auth/login", """{"error":"wrong name or password"}""", 401)
            val sdk = server.sdk(backgroundScope)
            val flow = sdk.signIn()
            flow.connect("media.example.com/holyarrs")
            flow.signIn("admin", "nope")
            assertEquals(
                SignInErrorCode.WRONG_CREDENTIALS,
                flow.state.value.error
                    ?.code,
            )
            assertEquals(SignInStep.CREDENTIALS, flow.state.value.step)
            assertNull(sdk.session.account)
        }

    @Test
    fun rateLimitCarriesRetryAfter() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", stateJson)
            server.json("POST", "/api/v1/auth/login", """{"error":"too many failed sign-ins"}""", 429, "Retry-After" to "30")
            val flow = server.sdk(backgroundScope).signIn()
            flow.connect(BASE)
            flow.signIn("admin", "nope")
            assertEquals(
                SignInErrorCode.RATE_LIMITED,
                flow.state.value.error
                    ?.code,
            )
            assertEquals(
                30,
                flow.state.value.error
                    ?.retryAfterSeconds,
            )
        }

    @Test
    fun serverThatNeedsSetupOpensTheOwnerStep() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":true,"ways":["password"]}""")
            val flow = server.sdk(backgroundScope).signIn()
            flow.connect(BASE)
            assertEquals(SignInStep.SETUP, flow.state.value.step)
            assertEquals(null, flow.state.value.error)
        }

    @Test
    fun otherServerIsNotHolyArrs() =
        runTest {
            val server = FakeServer()
            server.on("GET", "/api/v1/auth/state") { respondJson("<html></html>") }
            val flow = server.sdk(backgroundScope).signIn()
            flow.connect(BASE)
            assertEquals(
                SignInErrorCode.NOT_HOLYARRS,
                flow.state.value.error
                    ?.code,
            )
        }

    @Test
    fun invalidAddressIsRejected() =
        runTest {
            val flow = FakeServer().sdk(backgroundScope).signIn()
            flow.connect("ftp://nas")
            assertEquals(
                SignInErrorCode.INVALID_ADDRESS,
                flow.state.value.error
                    ?.code,
            )
        }

    @Test
    fun apiKeySignsInWithoutCookies() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", stateJson)
            server.bootstrapRoutes(meJson(role = "member", perms = "\"ask\"", csrf = ""))
            val sdk = server.sdk(backgroundScope)
            val flow = sdk.signIn()
            flow.connect(BASE)
            flow.useApiKey(" hak_key ")
            assertEquals(SignInStep.DONE, flow.state.value.step)
            val account = assertIs<SessionState.SignedIn>(sdk.session.state.value).account
            assertEquals(SignInMethod.API_KEY, account.method)
            assertNull(account.serverVersion)
            assertTrue(Section.HEALTH !in account.sections)
            assertEquals("hak_key", server.request("GET", "/api/v1/auth/me").headers["X-Api-Key"])
            assertTrue(server.requests.none { it.path == "/api/v1/app/about" })
        }

    @Test
    fun rejectedApiKeyStaysOnCredentials() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", stateJson)
            server.json("GET", "/api/v1/auth/me", """{"error":"API key not accepted"}""", 401)
            val sdk = server.sdk(backgroundScope)
            val flow = sdk.signIn()
            flow.connect(BASE)
            flow.useApiKey("hak_wrong")
            assertEquals(
                SignInErrorCode.KEY_REJECTED,
                flow.state.value.error
                    ?.code,
            )
            assertEquals(SignInStep.CREDENTIALS, flow.state.value.step)
        }

    @Test
    fun restoredSessionIsSignedOutWhenExpired() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", stateJson)
            server.json(
                "POST",
                "/api/v1/auth/login",
                """{"name":"admin","csrf":"c"}""",
                200,
                SET_COOKIE to "holyarrs_session=s1; Path=/holyarrs/; Max-Age=600",
            )
            server.bootstrapRoutes()
            val store = MemoryStore()
            val first = server.sdk(backgroundScope, store)
            val flow = first.signIn()
            flow.connect(BASE)
            flow.signIn("admin", "pw")
            assertIs<SessionState.SignedIn>(first.session.state.value)

            server.json("GET", "/api/v1/auth/me", """{"error":"sign in first"}""", 401)
            val second = server.sdk(backgroundScope, store)
            second.session.restore()
            val out = assertIs<SessionState.SignedOut>(second.session.state.value)
            assertTrue(out.expired)
            assertEquals(BASE, out.lastServerUrl)
            assertEquals("admin", out.lastUserName)
            assertNotNull(store.read("connection"))
            assertTrue(!store.read("connection")!!.contains("s1"))
        }

    @Test
    fun signOutCallsLogoutWithCsrf() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", stateJson)
            server.json(
                "POST",
                "/api/v1/auth/login",
                """{"name":"admin","csrf":"c"}""",
                200,
                SET_COOKIE to "holyarrs_session=s1; Path=/holyarrs/; Max-Age=600",
            )
            server.on("POST", "/api/v1/auth/logout") { respondJson("", 204) }
            server.bootstrapRoutes()
            val sdk = server.sdk(backgroundScope)
            val flow = sdk.signIn()
            flow.connect(BASE)
            flow.signIn("admin", "pw")
            sdk.session.signOut()
            assertEquals("csrf-me", server.request("POST", "/api/v1/auth/logout").headers["X-CSRF-Token"])
            assertIs<SessionState.SignedOut>(sdk.session.state.value)
        }
}

internal const val SET_COOKIE = "Set-Cookie"
