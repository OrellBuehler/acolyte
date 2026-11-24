package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.session.InviteKind
import ch.orellbuehler.acolyte.session.InviteLink
import ch.orellbuehler.acolyte.session.InvitePhase
import ch.orellbuehler.acolyte.session.InviteTarget
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

class AccountSignInTest {
    private val cookie = SET_COOKIE to "holyarrs_session=s1; Path=/holyarrs/; Max-Age=600"

    private fun setupJson(ways: String = """["password"]""") =
        """{"needs_setup":true,"ways":$ways,"setup_ways":["password","plex"],"min_password_length":10,"config_unmapped":true}"""

    @Test
    fun firstRunCreatesTheOwner() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", setupJson())
            server.json("POST", "/api/v1/auth/setup", """{"name":"owner","csrf":"c"}""", 201, cookie)
            server.bootstrapRoutes(meJson().replace("\"views\"", "\"setup_open\":true,\"views\""))
            val sdk = server.sdk(backgroundScope)
            val flow = sdk.signIn()
            flow.connect(BASE)
            assertEquals(SignInStep.SETUP, flow.state.value.step)
            assertEquals(10, flow.state.value.minPasswordLength)
            assertTrue(flow.state.value.configUnmapped)

            flow.createOwner("bad name", "", "longenough1")
            assertEquals(
                SignInErrorCode.INVALID_NAME,
                flow.state.value.error
                    ?.code,
            )
            flow.createOwner("owner", "", "short")
            assertEquals(
                SignInErrorCode.PASSWORD_SHORT,
                flow.state.value.error
                    ?.code,
            )
            assertTrue(server.requests.none { it.path == "/api/v1/auth/setup" })

            flow.createOwner("owner", "The Owner", "longenough1")
            assertEquals(SignInStep.DONE, flow.state.value.step)
            assertEquals(
                """{"name":"owner","password":"longenough1","display_name":"The Owner"}""",
                server.request("POST", "/api/v1/auth/setup").body,
            )
            val account = assertIs<SessionState.SignedIn>(sdk.session.state.value).account
            assertTrue(account.setupOpen)
            assertEquals(SignInMethod.PASSWORD, account.method)
        }

    @Test
    fun existingOwnerIsReported() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", setupJson())
            server.json("POST", "/api/v1/auth/setup", """{"error":"the owner is already set up; sign in instead"}""", 409)
            val flow = server.sdk(backgroundScope).signIn()
            flow.connect(BASE)
            flow.createOwner("owner", "", "longenough1")
            assertEquals(
                SignInErrorCode.OWNER_EXISTS,
                flow.state.value.error
                    ?.code,
            )
        }

    @Test
    fun plexSignInPollsUntilTheBrowserStepIsDone() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":false,"ways":["password","plex","oidc"],"min_password_length":8}""")
            server.json("POST", "/api/v1/auth/plex/start", """{"id":7,"auth_url":"https://app.plex.tv/auth#?code=x"}""")
            var polls = 0
            server.on("POST", "/api/v1/auth/plex/finish") {
                polls++
                if (polls < 3) respondJson("""{"waiting":true}""", 202) else respondJson("""{"name":"anna","csrf":"c"}""", 200, cookie)
            }
            server.bootstrapRoutes()
            val sdk = server.sdk(backgroundScope)
            val flow = sdk.signIn()
            flow.connect(BASE)
            assertEquals(listOf(SignInWay.PASSWORD, SignInWay.PLEX), flow.state.value.ways)
            assertEquals(listOf(SignInWay.OIDC), flow.state.value.otherWays)
            flow.signInWithPlex()
            assertEquals(3, polls)
            assertEquals("""{"id":7}""", server.request("POST", "/api/v1/auth/plex/finish").body)
            assertEquals(SignInStep.DONE, flow.state.value.step)
            assertNull(flow.state.value.plexUrl)
            val account = assertIs<SessionState.SignedIn>(sdk.session.state.value).account
            assertEquals(SignInMethod.MEDIA_SERVER, account.method)
        }

    @Test
    fun plexThatEndedShowsTheError() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":false,"ways":["password","plex"]}""")
            server.json("POST", "/api/v1/auth/plex/start", """{"id":7,"auth_url":"https://app.plex.tv/auth#?code=x"}""")
            server.json("POST", "/api/v1/auth/plex/finish", """{"error":"this Plex sign-in has ended; start again"}""", 404)
            val flow = server.sdk(backgroundScope).signIn()
            flow.connect(BASE)
            flow.signInWithPlex()
            assertEquals(
                SignInErrorCode.SIGN_IN_ENDED,
                flow.state.value.error
                    ?.code,
            )
            assertEquals(SignInStep.CREDENTIALS, flow.state.value.step)
        }

    @Test
    fun ownerResetSignsTheOwnerIn() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/auth/state",
                """{"needs_setup":false,"ways":["password"],"owner_reset":true,"min_password_length":8}""",
            )
            server.json("POST", "/api/v1/auth/owner-reset/code", """{"ok":true}""", 202)
            server.json("POST", "/api/v1/auth/owner-reset/check", """{"name":"boss"}""")
            server.json("POST", "/api/v1/auth/owner-reset/finish", """{"ok":true,"name":"boss"}""")
            server.json("POST", "/api/v1/auth/login", """{"name":"boss","csrf":"c"}""", 200, cookie)
            server.bootstrapRoutes()
            val sdk = server.sdk(backgroundScope)
            val flow = sdk.signIn()
            flow.connect(BASE)
            assertTrue(flow.state.value.ownerReset)
            flow.startReset()
            assertEquals(SignInStep.RESET_CODE, flow.state.value.step)
            flow.makeResetCode()
            assertTrue(flow.state.value.resetCodeMade)
            flow.checkResetCode(" AAAA-BBBB ")
            assertEquals(SignInStep.RESET_PASSWORD, flow.state.value.step)
            assertEquals("boss", flow.state.value.resetName)
            assertEquals("""{"code":"AAAA-BBBB"}""", server.request("POST", "/api/v1/auth/owner-reset/check").body)
            flow.finishReset("AAAA-BBBB", "boss", "newpassword")
            assertEquals(SignInStep.DONE, flow.state.value.step)
            assertEquals(
                """{"code":"AAAA-BBBB","name":"boss","password":"newpassword"}""",
                server.request("POST", "/api/v1/auth/owner-reset/finish").body,
            )
        }

    @Test
    fun wrongResetCodeIsNamed() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":false,"ways":["password"],"owner_reset":true}""")
            server.json("POST", "/api/v1/auth/owner-reset/check", """{"error":"that code is wrong or has run out; make a new one"}""", 401)
            val flow = server.sdk(backgroundScope).signIn()
            flow.connect(BASE)
            flow.startReset()
            flow.checkResetCode("nope")
            assertEquals(
                SignInErrorCode.RESET_CODE_WRONG,
                flow.state.value.error
                    ?.code,
            )
            assertEquals(SignInStep.RESET_CODE, flow.state.value.step)
            flow.back()
            assertEquals(SignInStep.CREDENTIALS, flow.state.value.step)
        }

    @Test
    fun inviteLinksAreParsed() {
        assertEquals(InviteTarget(BASE, "abc_123-X"), InviteLink.parse("  $BASE/invite/abc_123-X  "))
        assertEquals(InviteTarget("https://nas.local:6262", "t1"), InviteLink.parse("https://nas.local:6262/invite/t1?x=1"))
        assertNull(InviteLink.parse("https://media.example.com/login"))
        assertNull(InviteLink.parse("/invite/abc"))
    }

    @Test
    fun invitePasswordJoinSignsIn() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/auth/state",
                """{"needs_setup":false,"ways":["password","plex","oidc","emby"],"min_password_length":8}""",
            )
            server.json("GET", "/api/v1/auth/invites/tok", """{"role":"member","note":"Mum","max_rating":"PG","rating_country":"US"}""")
            server.json("POST", "/api/v1/auth/invites/tok", """{"name":"mum","csrf":"c"}""", 201, cookie)
            server.bootstrapRoutes(meJson(role = "member", perms = "\"ask\""))
            val sdk = server.sdk(backgroundScope)
            val flow = sdk.invite(InviteTarget(BASE, "tok"))
            flow.load()
            val open = flow.state.value
            assertEquals(InvitePhase.OPEN, open.phase)
            assertEquals(InviteKind.KID, open.kind)
            assertEquals("PG", open.kidLimit)
            assertEquals(listOf(SignInWay.PLEX, SignInWay.EMBY), open.ways)

            flow.join("mum", "short")
            assertEquals(
                SignInErrorCode.PASSWORD_SHORT,
                flow.state.value.error
                    ?.code,
            )
            flow.join("mum", "longenough")
            assertEquals(InvitePhase.DONE, flow.state.value.phase)
            assertEquals("""{"name":"mum","password":"longenough"}""", server.request("POST", "/api/v1/auth/invites/tok").body)
            assertIs<SessionState.SignedIn>(sdk.session.state.value)
        }

    @Test
    fun goneInviteAndLinkedAccount() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":false,"ways":["password","plex"]}""")
            server.json(
                "GET",
                "/api/v1/auth/invites/old",
                """{"error":"this invite link is used up, ended or wrong; ask for a new one"}""",
                404,
            )
            val gone = server.sdk(backgroundScope).invite(InviteTarget(BASE, "old"))
            gone.load()
            assertEquals(InvitePhase.GONE, gone.state.value.phase)

            server.json("GET", "/api/v1/auth/invites/new", """{"role":"admin"}""")
            server.json("POST", "/api/v1/auth/invites/new/plex/start", """{"id":3,"auth_url":"https://app.plex.tv/auth#?code=y"}""")
            server.json(
                "POST",
                "/api/v1/auth/invites/new/plex/finish",
                """{"error":"This account already has a HolyArrs account. Sign in instead.","code":"linked"}""",
                409,
            )
            val linked = server.sdk(backgroundScope).invite(InviteTarget(BASE, "new"))
            linked.load()
            assertEquals(InviteKind.ADMIN, linked.state.value.kind)
            linked.joinPlex()
            assertEquals(
                SignInErrorCode.LINKED,
                linked.state.value.error
                    ?.code,
            )
            assertNotNull(linked.state.value)
        }
}
