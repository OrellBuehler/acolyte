package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInFixKind
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInImportPhase
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInJoin
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInLines
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInLinkFailure
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInLinkPhase
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInNoticeKind
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInPresenter
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInRole
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSeen
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSkipOff
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSsoError
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInSsoPhase
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInWayKind
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSignInTest {
    private val settingsPath = "/api/v1/auth/settings"

    private val settingsJson =
        """{"skip_login_local_network":false,"local_networks":["192.168.1.0/24"],"local_person":0,"local_skip_off":"",
            "media_sign_in":["plex"],"home_ranges":["10.0.0.0/8","172.16.0.0/12","192.168.0.0/16","fc00::/7"],
            "cgnat_range":"100.64.0.0/10","trusted_proxies":[],"trusted_proxies_fixed":["172.18.0.2"],
            "require_two_factor_admins":false,"owner_reset_link":true,"min_password_length":8,
            "plex":{"on":true,"server_id":"srv1","join":"list","allow":"ana bo"},
            "jellyfin":{"on":false,"url":"http://10.0.0.5:8096","join":"nobody","allow":""},
            "emby":{"on":false,"join":"nobody","allow":""},
            "plex_owner":{"linked":true,"name":"ana","can_import":true},"allowed_hosts":["tower.local"],
            "seen_address":"192.168.1.40","gateway_networks":[],"proxy_untrusted":false,"seen_gateway":false}"""

    private val usersJson =
        """{"users":[{"id":1,"name":"Ana","role":"owner","state":"active","avatar":null,"max_rating":""},
            {"id":2,"name":"Bo","role":"member","state":"active","avatar":"/api/v1/users/2/avatar","max_rating":"PG"},
            {"id":3,"name":"Cy","role":"admin","state":"off","max_rating":""}]}"""

    private val ssoJson =
        """{"source":"settings","on":true,"running":true,"issuer":"https://auth.example.com/","client_id":"holyarrs","secret_set":true,
            "redirect_url":"","suggested_redirect_url":"https://media.example.com/holyarrs/api/v1/auth/oidc/callback","allow_join":false,
            "groups_claim":"groups","group_roles":"admins=admin","links":2,"local_network_allowed":false,
            "forward":{"on":false,"allow_join":false,"links":0}}"""

    private val reauthJson = """{"error":"confirm your local password again before using this route","code":"reauth"}"""

    private fun settings(vararg edits: Pair<String, String>): String =
        edits.fold(settingsJson) { json, (from, to) -> json.replace(from, to) }

    private fun server(
        settings: String = settingsJson,
        sso: String? = ssoJson,
    ): FakeServer {
        val server = FakeServer()
        server.json("GET", settingsPath, settings)
        server.json("GET", "/api/v1/users", usersJson)
        if (sso != null) server.json("GET", "/api/v1/auth/sso", sso)
        server.json("GET", "/api/v1/auth/me", """{"id":1,"name":"ana","role":"owner","two_factor":true}""")
        return server
    }

    private fun presenter(
        server: FakeServer,
        role: Role = Role.OWNER,
        method: SignInMethod = SignInMethod.PASSWORD,
        reauth: suspend (String, String?) -> ApiError? = { _, _ -> null },
    ): SettingsSignInPresenter =
        SettingsSignInPresenter(server.api(), { testAccount(role, listOf("settings")).copy(method = method) }, reauth)

    private fun body(
        server: FakeServer,
        method: String,
        path: String,
    ): JsonObject = Json.parseToJsonElement(server.request(method, path).body.orEmpty()).jsonObject

    private fun calls(
        server: FakeServer,
        method: String,
        path: String,
    ): Int = server.requests.toList().count { it.method == method && it.path == path }

    @Test
    fun lineChecksMatchTheWeb() {
        fun fix(
            text: String,
            proxy: Boolean = false,
            gateways: List<String> = emptyList(),
        ) = SettingsSignInLines.check(text, proxy, gateways)
        for (good in listOf("192.168.1.0/24", "10.0.0.0/8", "192.168.1.5", "172.16.0.0/12", "fd00::/8", "100.64.0.0/10")) {
            assertNull(fix(good), good)
        }
        assertEquals(SettingsSignInFixKind.PART, fix("192.168.1.300")?.kind)
        val missing = fix("10.0.0/8")
        assertEquals(SettingsSignInFixKind.MISSING, missing?.kind)
        assertEquals("10.0.0.0/8", missing?.fix)
        val letter = fix("192.168.l.0/24")
        assertEquals(SettingsSignInFixKind.LETTER, letter?.kind)
        assertEquals("l", letter?.letter)
        assertEquals("192.168.1.0/24", letter?.fix)
        assertEquals("10.0.0.0", fix("lO.0.0.0")?.fix)
        assertEquals(SettingsSignInFixKind.EXTRA, fix("1.2.3.4.5")?.kind)
        assertEquals(SettingsSignInFixKind.BITS, fix("10.0.0.0/40")?.kind)
        assertEquals(SettingsSignInFixKind.BITS, fix("10.0.0.0/x")?.kind)
        assertEquals(SettingsSignInFixKind.WIDE, fix("0.0.0.0/0")?.kind)
        assertEquals(SettingsSignInFixKind.WIDE, fix("10.0.0.0/7")?.kind)
        assertEquals(SettingsSignInFixKind.NOT_ADDRESS, fix("home")?.kind)
        assertEquals(SettingsSignInFixKind.NOT_ADDRESS, fix("10.0.0.0/8/8")?.kind)
        assertEquals(SettingsSignInFixKind.NOT_ADDRESS, fix("1..2.3.4.5")?.kind)
    }

    @Test
    fun bridgeIsRefusedForNetworksButNotProxies() {
        assertEquals(SettingsSignInFixKind.BRIDGE, SettingsSignInLines.check("172.17.0.0/16", false)?.kind)
        assertEquals(SettingsSignInFixKind.BRIDGE, SettingsSignInLines.check("172.17.0.1", false)?.kind)
        assertEquals(SettingsSignInFixKind.BRIDGE, SettingsSignInLines.check("172.20.0.1", false, listOf("172.20.0.1"))?.kind)
        assertNull(SettingsSignInLines.check("172.17.0.2", true))
        assertNull(SettingsSignInLines.check("172.20.0.1", false))
        assertNull(SettingsSignInLines.check("172.17.0.0/12", false))
    }

    @Test
    fun ipv6LinesOnlyGetThePrefixAndCharacterCheck() {
        assertNull(SettingsSignInLines.check("fc00::/7", false))
        assertNull(SettingsSignInLines.check("::1", true))
        assertEquals(SettingsSignInFixKind.WIDE, SettingsSignInLines.check("fc00::/6", false)?.kind)
        assertEquals(SettingsSignInFixKind.NOT_ADDRESS, SettingsSignInLines.check("fc00::/200", false)?.kind)
        assertEquals(SettingsSignInFixKind.NOT_ADDRESS, SettingsSignInLines.check("fc0g::/8", false)?.kind)
        assertEquals(SettingsSignInFixKind.NOT_ADDRESS, SettingsSignInLines.check("fc00::/", false)?.kind)
    }

    @Test
    fun problemsAreNumberedByLineAndSkipBlanks() {
        val problems = SettingsSignInLines.problems("192.168.1.0/24\n\n192.168.1.300\n  home  ", false)
        assertEquals(listOf(3, 4), problems.map { it.line })
        assertEquals("home", problems[1].text)
        assertTrue(SettingsSignInLines.problems("", false).isEmpty())
    }

    @Test
    fun linesSplitOnSpacesCommasAndNewlines() {
        assertEquals(listOf("a", "b", "c", "d"), SettingsSignInLines.lines("a, b\nc  d,,\n"))
    }

    @Test
    fun coverageAndGatewayGuesses() {
        assertEquals(true, SettingsSignInLines.covers("192.168.1.40", listOf("192.168.1.0/24")))
        assertEquals(false, SettingsSignInLines.covers("10.1.1.1", listOf("192.168.1.0/24")))
        assertEquals(true, SettingsSignInLines.covers("10.1.1.1", listOf("10.1.1.1")))
        assertNull(SettingsSignInLines.covers("fd00::1", listOf("fd00::/8")))
        assertTrue(SettingsSignInLines.gatewayLike("172.17.0.1"))
        assertTrue(SettingsSignInLines.gatewayLike("172.31.5.1"))
        assertTrue(SettingsSignInLines.gatewayLike("10.88.0.1"))
        assertFalse(SettingsSignInLines.gatewayLike("192.168.1.1"))
        assertFalse(SettingsSignInLines.gatewayLike("172.32.0.1"))
    }

    @Test
    fun ownerLoadsEverything() =
        runTest {
            val server = server()
            val presenter = presenter(server)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.isOwner)
            assertFalse(s.apiKey)
            assertEquals("8", s.minLengthText)
            assertTrue(s.forgotLink)
            val plex = s.way(SettingsSignInWayKind.PLEX)
            assertTrue(plex.on)
            assertEquals(SettingsSignInJoin.LIST, plex.join)
            assertEquals("srv1", plex.serverId)
            assertEquals("ana bo", plex.allow)
            assertEquals("http://10.0.0.5:8096", s.way(SettingsSignInWayKind.JELLYFIN).url)
            assertTrue(s.plex.linked)
            assertEquals("ana", s.plex.name)
            assertTrue(s.canImport(SettingsSignInWayKind.PLEX))
            assertTrue(s.canImport(SettingsSignInWayKind.JELLYFIN))
            assertFalse(s.canImport(SettingsSignInWayKind.EMBY))
            assertEquals(listOf("Plex"), s.local.mediaApps)
            assertFalse(s.showLocal)
            assertEquals("192.168.1.0/24", s.local.networks)
            assertEquals(listOf("172.18.0.2"), s.local.fixedProxies)
            assertEquals("tower.local", s.hosts.text)
            assertEquals(listOf("Ana", "Bo"), s.local.people?.map { it.name })
            assertEquals(
                SettingsSignInRole.OWNER,
                s.local.people
                    ?.first()
                    ?.role,
            )
            assertEquals(
                "PG",
                s.local.people
                    ?.last()
                    ?.kidLimit,
            )
            assertEquals(1L, s.local.pickedId)
            assertEquals(SettingsSignInSeen.COVERED, s.local.seen)
            assertEquals(SettingsSignInSsoPhase.READY, s.sso.phase)
            assertEquals(true, s.sso.twoFactor)
            assertTrue(s.sso.secretKept)
            assertFalse(s.sso.publicClient)
            assertEquals("https://media.example.com/holyarrs/api/v1/auth/oidc/callback", s.sso.callback)
        }

    @Test
    fun ownerOnlyControlsAreOffForAdmins() =
        runTest {
            val server = server()
            val presenter = presenter(server, role = Role.ADMIN)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertFalse(s.isOwner)
            assertEquals(SettingsSignInSsoPhase.HIDDEN, s.sso.phase)
            assertNull(s.local.people)
            assertEquals(0, calls(server, "GET", "/api/v1/users"))
            assertEquals(0, calls(server, "GET", "/api/v1/auth/sso"))
            presenter.setRequireTwoFactor(true)
            presenter.saveHosts()
            assertEquals(0, calls(server, "PUT", settingsPath))
        }

    @Test
    fun apiKeyAccountsDoNotTouchSessionOnlyRoutes() =
        runTest {
            val server = server()
            val presenter = presenter(server, method = SignInMethod.API_KEY)
            presenter.load()
            assertTrue(presenter.state.value.apiKey)
            assertEquals(SettingsSignInSsoPhase.NEEDS_SESSION, presenter.state.value.sso.phase)
            assertEquals(0, calls(server, "GET", "/api/v1/auth/sso"))
            presenter.loadSso()
            presenter.saveHosts()
            assertEquals(0, calls(server, "GET", "/api/v1/auth/sso"))
            assertEquals(0, calls(server, "PUT", settingsPath))
            server.json("PUT", settingsPath, settingsJson)
            presenter.setLocalProxies("10.0.0.5")
            presenter.setLocalNetworks("192.168.2.0/24")
            presenter.saveLocal()
            assertFalse(body(server, "PUT", settingsPath).containsKey("trusted_proxies"))
            assertEquals(
                "192.168.2.0/24",
                body(server, "PUT", settingsPath)["local_networks"]
                    ?.jsonArray
                    ?.single()
                    ?.jsonPrimitive
                    ?.content,
            )
        }

    @Test
    fun loadFailureKeepsTheErrorAndRetryWorks() =
        runTest {
            val server = FakeServer()
            server.json("GET", settingsPath, """{"error":"nope"}""", 500)
            server.json("GET", "/api/v1/users", usersJson)
            server.json("GET", "/api/v1/auth/sso", ssoJson)
            val presenter = presenter(server)
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.SERVER,
                presenter.state.value.status.error
                    ?.code,
            )
            server.json("GET", settingsPath, settingsJson)
            presenter.refresh()
            assertEquals(Phase.READY, presenter.state.value.status.phase)
        }

    @Test
    fun loadAsksForThePasswordWhenTheServerWantsProof() =
        runTest {
            val server = server()
            server.json("GET", settingsPath, reauthJson, 403)
            val asked = mutableListOf<Pair<String, String?>>()
            val presenter =
                presenter(server) { password, code ->
                    asked += password to code
                    null
                }
            presenter.load()
            assertTrue(presenter.state.value.reauth.needed)
            assertEquals(Phase.LOADING, presenter.state.value.status.phase)
            server.json("GET", settingsPath, settingsJson)
            presenter.confirmReauth("secret", " 123456 ")
            assertEquals(listOf<Pair<String, String?>>("secret" to "123456"), asked)
            assertFalse(presenter.state.value.reauth.needed)
            assertEquals(Phase.READY, presenter.state.value.status.phase)
        }

    @Test
    fun cancellingTheProofOnLoadShowsAnError() =
        runTest {
            val server = server()
            server.json("GET", settingsPath, reauthJson, 403)
            val presenter = presenter(server)
            presenter.load()
            presenter.cancelReauth()
            assertFalse(presenter.state.value.reauth.needed)
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.REAUTH,
                presenter.state.value.status.error
                    ?.code,
            )
        }

    @Test
    fun shortestPasswordWarnsAndSavesJustThatField() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) { respondJson(settings("\"min_password_length\":8" to "\"min_password_length\":6")) }
            val presenter = presenter(server)
            presenter.load()
            presenter.setMinLength("6x")
            var s = presenter.state.value
            assertEquals("6", s.minLengthText)
            assertTrue(s.shortPasswordWarning)
            assertTrue(s.canSaveMinLength)
            presenter.setMinLength("3")
            assertFalse(presenter.state.value.canSaveMinLength)
            presenter.setMinLength("65")
            assertFalse(presenter.state.value.canSaveMinLength)
            presenter.setMinLength("6")
            presenter.saveMinLength()
            s = presenter.state.value
            assertEquals(
                mapOf("min_password_length" to "6"),
                body(server, "PUT", settingsPath).mapValues { it.value.jsonPrimitive.content },
            )
            assertEquals("6", s.minLengthText)
            assertFalse(s.minLengthSaving)
            assertEquals(SettingsSignInNoticeKind.MIN_SAVED, s.notice?.kind)
            assertEquals(6, s.notice?.count)
            presenter.setMinLength("12")
            assertFalse(presenter.state.value.shortPasswordWarning)
        }

    @Test
    fun switchesSaveInstantlyAndRollBackOnFailure() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) {
                respondJson(
                    settings(
                        "\"require_two_factor_admins\":false" to "\"require_two_factor_admins\":true",
                    ),
                )
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.setRequireTwoFactor(true)
            var s = presenter.state.value
            assertTrue(s.requireTwoFactor)
            assertEquals(SettingsSignInNoticeKind.TWO_FACTOR_REQUIRED, s.notice?.kind)
            assertEquals(setOf("require_two_factor_admins"), body(server, "PUT", settingsPath).keys)
            server.json("PUT", settingsPath, """{"error":"sign in with your password for this"}""", 403)
            presenter.setForgotLink(false)
            s = presenter.state.value
            assertTrue(s.forgotLink)
            assertFalse(s.forgotLinkSaving)
            assertEquals(SettingsSignInNoticeKind.FAILED, s.notice?.kind)
            assertEquals(ApiErrorCode.FORBIDDEN, s.notice?.error?.code)
            assertEquals(false, body(server, "PUT", settingsPath)["owner_reset_link"]?.jsonPrimitive?.boolean)
        }

    @Test
    fun opensJoiningToEveryoneOnlyAfterConfirming() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) { respondJson(settings("\"join\":\"list\"" to "\"join\":\"everyone\"")) }
            val presenter = presenter(server)
            presenter.load()
            presenter.setWayJoin(SettingsSignInWayKind.PLEX, SettingsSignInJoin.EVERYONE)
            presenter.saveWay(SettingsSignInWayKind.PLEX)
            var s = presenter.state.value
            assertEquals(0, calls(server, "PUT", settingsPath))
            assertEquals(SettingsSignInWayKind.PLEX, s.everyoneAsk?.kind)
            assertEquals(ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInEveryoneWho.PICKED_SERVER, s.everyoneAsk?.who)
            presenter.dismissEveryone()
            assertNull(presenter.state.value.everyoneAsk)
            presenter.saveWay(SettingsSignInWayKind.PLEX, confirmed = true)
            s = presenter.state.value
            val sent = body(server, "PUT", settingsPath)
            assertEquals(setOf("plex", "confirm_everyone"), sent.keys)
            val plex = sent.getValue("plex").jsonObject
            assertEquals(setOf("on", "server_id", "join", "allow"), plex.keys)
            assertEquals("everyone", plex.getValue("join").jsonPrimitive.content)
            assertEquals(SettingsSignInJoin.EVERYONE, s.way(SettingsSignInWayKind.PLEX).savedJoin)
            assertEquals(SettingsSignInNoticeKind.WAY_SAVED, s.notice?.kind)
            assertEquals("Plex", s.notice?.name)
            presenter.saveWay(SettingsSignInWayKind.PLEX)
            assertEquals(2, calls(server, "PUT", settingsPath))
            assertNull(presenter.state.value.everyoneAsk)
        }

    @Test
    fun jellyfinWaysSendTheAddressAndTestItAfterSaving() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) {
                respondJson(
                    settings(
                        "\"on\":false,\"url\":\"http://10.0.0.5:8096\"" to "\"on\":true,\"url\":\"http://10.0.0.5:8096\"",
                    ),
                )
            }
            server.json("POST", "/api/v1/auth/media-server/jellyfin/test", """{"ok":false,"message":"connection refused"}""")
            val presenter = presenter(server)
            presenter.load()
            val kind = SettingsSignInWayKind.JELLYFIN
            assertTrue(
                presenter.state.value
                    .way(kind)
                    .untested,
            )
            presenter.setWayOn(kind, true)
            presenter.saveWay(kind)
            val jellyfin = body(server, "PUT", settingsPath).getValue("jellyfin").jsonObject
            assertEquals(setOf("on", "url", "join", "allow"), jellyfin.keys)
            assertEquals("http://10.0.0.5:8096", jellyfin.getValue("url").jsonPrimitive.content)
            var s = presenter.state.value
            assertEquals(1, calls(server, "POST", "/api/v1/auth/media-server/jellyfin/test"))
            assertEquals(SettingsSignInNoticeKind.WAY_UNREACHABLE, s.notice?.kind)
            assertEquals("connection refused", s.way(kind).test?.text)
            assertFalse(s.way(kind).untested)
            server.json(
                "POST",
                "/api/v1/auth/media-server/jellyfin/test",
                """{"ok":true,"name":"Home","version":"10.9.1","product":"Jellyfin Server"}""",
            )
            presenter.saveWay(kind)
            s = presenter.state.value
            assertEquals(SettingsSignInNoticeKind.WAY_SAVED, s.notice?.kind)
            assertEquals("Jellyfin Server 10.9.1 (Home)", s.way(kind).test?.text)
            presenter.saveWay(kind)
            assertEquals(2, calls(server, "POST", "/api/v1/auth/media-server/jellyfin/test"))
            presenter.setWayUrl(kind, "http://other:8096")
            assertNull(
                presenter.state.value
                    .way(kind)
                    .test,
            )
            assertTrue(
                presenter.state.value
                    .way(kind)
                    .untested,
            )
        }

    @Test
    fun testingAnAddressReportsFailuresFromTheServer() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/media-server/emby/test", """{"error":"not an address"}""", 400)
            val presenter = presenter(server)
            presenter.load()
            presenter.testWay(SettingsSignInWayKind.EMBY)
            assertEquals(0, calls(server, "POST", "/api/v1/auth/media-server/emby/test"))
            presenter.setWayUrl(SettingsSignInWayKind.EMBY, "nope")
            presenter.testWay(SettingsSignInWayKind.EMBY)
            val test =
                presenter.state.value
                    .way(SettingsSignInWayKind.EMBY)
                    .test
            assertEquals(false, test?.ok)
            assertEquals(ApiErrorCode.BAD_REQUEST, test?.error?.code)
        }

    @Test
    fun plexLinkPollsUntilTheOwnerConfirms() =
        runTest {
            val server = server()
            val client = server.client(listOf(FakeServer.SESSION), csrf = "csrf")
            server.json(
                "POST",
                "/api/v1/auth/plex/link/start",
                """{"id":7,"auth_url":"https://app.plex.tv/auth#?code=abc"}""",
                200,
                "Set-Cookie" to "holyarrs_plex_7=secret; Path=/; Max-Age=600; HttpOnly",
            )
            var checks = 0
            server.on("POST", "/api/v1/auth/plex/link/finish") {
                checks++
                when (checks) {
                    1 -> respondJson("""{"waiting":true}""", 202)
                    2 -> respondJson("""{"error":"checking too often; wait a second"}""", 429, "Retry-After" to "1")
                    else -> respondJson("""{"name":"ana","servers":[{"id":"srv9","name":"Home"}]}""")
                }
            }
            val presenter =
                SettingsSignInPresenter(server.api(client), { testAccount(Role.OWNER, emptyList()) }, { _, _ -> null })
            presenter.load()
            presenter.setWayServerId(SettingsSignInWayKind.PLEX, "")
            presenter.linkPlex()
            val s = presenter.state.value
            assertEquals(SettingsSignInLinkPhase.LINKED_PICK, s.plex.phase)
            assertEquals(1, s.plex.openCount)
            assertEquals("https://app.plex.tv/auth#?code=abc", s.plex.authUrl)
            assertEquals(listOf("srv9" to "Home"), s.plex.servers.map { it.id to it.name })
            assertEquals("srv9", s.way(SettingsSignInWayKind.PLEX).serverId)
            assertEquals(SettingsSignInNoticeKind.PLEX_LINKED, s.notice?.kind)
            assertEquals(3, calls(server, "POST", "/api/v1/auth/plex/link/finish"))
            assertEquals("""{"id":7}""", server.request("POST", "/api/v1/auth/plex/link/finish").body)
            val cookie =
                server
                    .request(
                        "POST",
                        "/api/v1/auth/plex/link/finish",
                    ).headers.entries
                    .firstOrNull { it.key.equals("Cookie", true) }
            assertTrue(cookie?.value.orEmpty().contains("holyarrs_plex_7=secret"), cookie.toString())
            assertTrue(
                client.credentials.cookies
                    .snapshot()
                    .any { it.name == "holyarrs_plex_7" && it.value == "secret" },
            )
            assertTrue(cookie?.value.orEmpty().contains("holyarrs_session=token-1"))
        }

    @Test
    fun plexLinkWithoutServersSaysSo() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/plex/link/start", """{"id":8,"auth_url":"https://app.plex.tv/auth#?code=x"}""")
            server.json("POST", "/api/v1/auth/plex/link/finish", """{"name":"ana","servers":[]}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.linkPlex()
            assertEquals(SettingsSignInLinkPhase.LINKED_NONE, presenter.state.value.plex.phase)
            assertEquals(
                "srv1",
                presenter.state.value
                    .way(SettingsSignInWayKind.PLEX)
                    .serverId,
            )
        }

    @Test
    fun plexLinkStopsOnTheServersAnswers() =
        runTest {
            val cases =
                listOf(
                    404 to SettingsSignInLinkFailure.ENDED,
                    409 to SettingsSignInLinkFailure.TAKEN,
                    502 to SettingsSignInLinkFailure.OTHER,
                    403 to SettingsSignInLinkFailure.NEEDS_PASSWORD,
                )
            for ((status, failure) in cases) {
                val server = server()
                server.json("POST", "/api/v1/auth/plex/link/start", """{"id":8,"auth_url":"https://app.plex.tv/auth#?code=x"}""")
                server.json("POST", "/api/v1/auth/plex/link/finish", """{"error":"nope"}""", status)
                val presenter = presenter(server)
                presenter.load()
                presenter.linkPlex()
                val plex = presenter.state.value.plex
                assertEquals(SettingsSignInLinkPhase.FAILED, plex.phase, "status $status")
                assertEquals(failure, plex.failure, "status $status")
                assertEquals(1, calls(server, "POST", "/api/v1/auth/plex/link/finish"))
            }
        }

    @Test
    fun plexLinkFailsAtOnceWhenStartIsRefused() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/plex/link/start", """{"error":"too many Plex sign-ins started; wait a few minutes"}""", 429)
            val presenter = presenter(server)
            presenter.load()
            presenter.linkPlex()
            val plex = presenter.state.value.plex
            assertEquals(SettingsSignInLinkPhase.FAILED, plex.phase)
            assertEquals(SettingsSignInLinkFailure.TOO_MANY, plex.failure)
            assertEquals(0, calls(server, "POST", "/api/v1/auth/plex/link/finish"))
        }

    @Test
    fun plexLinkKeepsWaitingThroughNetworkHiccups() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/plex/link/start", """{"id":9,"auth_url":"https://app.plex.tv/auth#?code=x"}""")
            var checks = 0
            server.on("POST", "/api/v1/auth/plex/link/finish") {
                checks++
                if (checks < 3) throw IllegalStateException("Network is unreachable") else respondJson("""{"name":"ana","servers":[]}""")
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.linkPlex()
            assertEquals(SettingsSignInLinkPhase.LINKED_NONE, presenter.state.value.plex.phase)
            assertEquals(3, checks)
        }

    @Test
    fun plexLinkGivesUpAfterTenMinutes() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/plex/link/start", """{"id":9,"auth_url":"https://app.plex.tv/auth#?code=x"}""")
            server.json("POST", "/api/v1/auth/plex/link/finish", """{"waiting":true}""", 202)
            val presenter = presenter(server)
            presenter.load()
            presenter.linkPlex()
            assertEquals(SettingsSignInLinkPhase.TIMED_OUT, presenter.state.value.plex.phase)
            assertEquals(300, calls(server, "POST", "/api/v1/auth/plex/link/finish"))
        }

    @Test
    fun plexLinkAsksForThePasswordThenContinues() =
        runTest {
            val server = server()
            var started = 0
            server.on("POST", "/api/v1/auth/plex/link/start") {
                started++
                if (started ==
                    1
                ) {
                    respondJson(reauthJson, 403)
                } else {
                    respondJson("""{"id":4,"auth_url":"https://app.plex.tv/auth#?code=y"}""")
                }
            }
            server.json(
                "POST",
                "/api/v1/auth/plex/link/finish",
                """{"name":"ana","servers":[{"id":"a","name":"A"},{"id":"b","name":"B"}]}""",
            )
            val asked = mutableListOf<String>()
            val presenter =
                presenter(server) { password, _ ->
                    asked += password
                    null
                }
            presenter.load()
            presenter.linkPlex()
            assertTrue(presenter.state.value.reauth.needed)
            assertEquals(SettingsSignInLinkPhase.STARTING, presenter.state.value.plex.phase)
            assertEquals(0, calls(server, "POST", "/api/v1/auth/plex/link/finish"))
            presenter.confirmReauth("pw", "")
            val s = presenter.state.value
            assertEquals(listOf("pw"), asked)
            assertFalse(s.reauth.needed)
            assertEquals(SettingsSignInLinkPhase.LINKED_PICK, s.plex.phase)
            assertEquals("srv1", s.way(SettingsSignInWayKind.PLEX).serverId)
            assertEquals(2, s.plex.servers.size)
        }

    @Test
    fun cancellingTheProofStopsTheLink() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/plex/link/start", reauthJson, 403)
            val presenter = presenter(server)
            presenter.load()
            presenter.linkPlex()
            presenter.cancelReauth()
            assertEquals(SettingsSignInLinkPhase.IDLE, presenter.state.value.plex.phase)
            assertFalse(presenter.state.value.reauth.needed)
        }

    @Test
    fun plexLinkAsksAgainWhenTheProofRunsOutAndKeepsWaiting() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/plex/link/start", """{"id":9,"auth_url":"https://app.plex.tv/auth#?code=x"}""")
            var checks = 0
            server.on("POST", "/api/v1/auth/plex/link/finish") {
                checks++
                when {
                    checks <= 2 -> respondJson(reauthJson, 403)
                    checks == 3 -> respondJson("""{"waiting":true}""", 202)
                    else -> respondJson("""{"name":"ana","servers":[]}""")
                }
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.linkPlex()
            assertTrue(presenter.state.value.reauth.needed)
            assertEquals(SettingsSignInLinkPhase.WAITING, presenter.state.value.plex.phase)
            presenter.confirmReauth("pw", "")
            assertFalse(presenter.state.value.reauth.needed)
            assertEquals(SettingsSignInLinkPhase.LINKED_NONE, presenter.state.value.plex.phase)
            assertEquals(4, checks)
        }

    @Test
    fun cancellingTheLinkEndsTheWait() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/plex/link/start", """{"id":9,"auth_url":"https://app.plex.tv/auth#?code=x"}""")
            lateinit var presenter: SettingsSignInPresenter
            var checks = 0
            server.on("POST", "/api/v1/auth/plex/link/finish") {
                checks++
                presenter.cancelLink()
                respondJson("""{"waiting":true}""", 202)
            }
            presenter = presenter(server)
            presenter.load()
            presenter.linkPlex()
            assertEquals(SettingsSignInLinkPhase.IDLE, presenter.state.value.plex.phase)
            assertEquals(1, checks)
        }

    @Test
    fun cancellingTheProofRollsTheSwitchBack() =
        runTest {
            val server = server()
            server.json("PUT", settingsPath, reauthJson, 403)
            val presenter = presenter(server)
            presenter.load()
            presenter.setRequireTwoFactor(true)
            assertTrue(presenter.state.value.reauth.needed)
            assertTrue(presenter.state.value.requireTwoFactor)
            presenter.cancelReauth()
            assertFalse(presenter.state.value.requireTwoFactor)
            assertFalse(presenter.state.value.requireTwoFactorSaving)
            assertFalse(presenter.state.value.reauth.needed)
        }

    @Test
    fun unlinkingPlexClearsTheServersAfterAskingForProof() =
        runTest {
            val server = server()
            var deletes = 0
            server.on("DELETE", "/api/v1/auth/plex/link") {
                deletes++
                if (deletes == 1) respondJson(reauthJson, 403) else respondJson("""{"linked":false,"name":"","can_import":false}""")
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.unlinkPlex()
            assertTrue(presenter.state.value.reauth.needed)
            presenter.confirmReauth("pw", "")
            val s = presenter.state.value
            assertFalse(s.plex.linked)
            assertFalse(s.plex.canImport)
            assertFalse(s.canImport(SettingsSignInWayKind.PLEX))
            assertEquals(SettingsSignInNoticeKind.PLEX_UNLINKED, s.notice?.kind)
            assertEquals(2, deletes)
        }

    @Test
    fun plexImportPreviewsAndMakesThePickedPeople() =
        runTest {
            val server = server()
            server.json(
                "POST",
                "/api/v1/auth/import/plex/preview",
                """{"people":[{"id":"1","name":"Ada","email":"ada@example.com","kid":false,"server_admin":false},
                    {"id":"2","name":"Kim","kid":true,"server_admin":false},
                    {"id":"3","name":"Sam","kid":false,"server_admin":true,"linked_to":"sam"}]}""",
            )
            server.json("POST", "/api/v1/auth/import/plex", """{"made":["Ada","Kim"],"skipped":0}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.openImport(SettingsSignInWayKind.PLEX)
            val sheet = assertNotNull(presenter.state.value.importSheet)
            assertEquals(SettingsSignInImportPhase.READY, sheet.phase)
            assertEquals(2, sheet.pickedCount)
            assertTrue(sheet.people.last().locked)
            assertEquals(
                3,
                presenter.state.value
                    .way(SettingsSignInWayKind.PLEX)
                    .lastCount,
            )
            assertEquals("{}", server.request("POST", "/api/v1/auth/import/plex/preview").body)
            presenter.togglePerson("3")
            presenter.toggleKid("3")
            assertEquals(2, assertNotNull(presenter.state.value.importSheet).pickedCount)
            presenter.togglePerson("1")
            assertEquals(1, assertNotNull(presenter.state.value.importSheet).pickedCount)
            presenter.toggleKid("1")
            presenter.togglePerson("1")
            presenter.runImport()
            assertNull(presenter.state.value.importSheet)
            val sent = body(server, "POST", "/api/v1/auth/import/plex").getValue("people").jsonArray.map { it.jsonObject }
            assertEquals(listOf("1", "2"), sent.map { it.getValue("id").jsonPrimitive.content })
            assertEquals(listOf(false, true), sent.map { it.getValue("kid").jsonPrimitive.boolean })
            assertTrue(sent.none { it.containsKey("confirm_not_kid") })
            assertEquals(
                SettingsSignInNoticeKind.IMPORTED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                2,
                presenter.state.value.notice
                    ?.count,
            )
            assertEquals(
                "Ada, Kim",
                presenter.state.value.notice
                    ?.name,
            )
        }

    @Test
    fun importingARestrictedAccountAsAnAdultNeedsConfirming() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/import/plex/preview", """{"people":[{"id":"2","name":"Kim","kid":true}]}""")
            server.json("POST", "/api/v1/auth/import/plex", """{"made":["Kim"],"skipped":0}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.openImport(SettingsSignInWayKind.PLEX)
            presenter.toggleKid("2")
            presenter.runImport()
            assertEquals(listOf("Kim"), assertNotNull(presenter.state.value.importSheet).confirmAdults)
            assertEquals(0, calls(server, "POST", "/api/v1/auth/import/plex"))
            presenter.dismissConfirmAdults()
            assertTrue(assertNotNull(presenter.state.value.importSheet).confirmAdults.isEmpty())
            presenter.runImport(confirmAdults = true)
            val sent =
                body(server, "POST", "/api/v1/auth/import/plex")
                    .getValue("people")
                    .jsonArray
                    .single()
                    .jsonObject
            assertEquals(false, sent.getValue("kid").jsonPrimitive.boolean)
            assertEquals(true, sent.getValue("confirm_not_kid").jsonPrimitive.boolean)
        }

    @Test
    fun jellyfinImportAsksForAnAdminFirstAndSurvivesAMistake() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/import/jellyfin/preview", """{"error":"wrong name or password"}""", 401)
            val presenter = presenter(server)
            presenter.load()
            presenter.openImport(SettingsSignInWayKind.JELLYFIN)
            assertEquals(SettingsSignInImportPhase.ASK, assertNotNull(presenter.state.value.importSheet).phase)
            assertEquals(0, calls(server, "POST", "/api/v1/auth/import/jellyfin/preview"))
            presenter.previewImport(" root ", "pw")
            var sheet = assertNotNull(presenter.state.value.importSheet)
            assertEquals(SettingsSignInImportPhase.ASK, sheet.phase)
            assertEquals(ApiErrorCode.UNAUTHORIZED, sheet.error?.code)
            assertEquals(
                mapOf(
                    "name" to "root",
                    "password" to "pw",
                ),
                body(server, "POST", "/api/v1/auth/import/jellyfin/preview").mapValues {
                    it.value.jsonPrimitive.content
                },
            )
            server.json("POST", "/api/v1/auth/import/jellyfin/preview", """{"people":[]}""")
            presenter.previewImport("root", "pw")
            sheet = assertNotNull(presenter.state.value.importSheet)
            assertEquals(SettingsSignInImportPhase.READY, sheet.phase)
            assertTrue(sheet.people.isEmpty())
            assertNull(sheet.error)
            presenter.closeImport()
            assertNull(presenter.state.value.importSheet)
            presenter.openImport(SettingsSignInWayKind.EMBY)
            assertNull(presenter.state.value.importSheet)
        }

    @Test
    fun importAsksForTheProofAndKeepsTheSheetOpen() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/import/plex/preview", """{"people":[{"id":"1","name":"Ada"}]}""")
            var made = 0
            server.on("POST", "/api/v1/auth/import/plex") {
                made++
                if (made == 1) respondJson(reauthJson, 403) else respondJson("""{"made":[],"skipped":1}""")
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.openImport(SettingsSignInWayKind.PLEX)
            presenter.runImport()
            assertTrue(presenter.state.value.reauth.needed)
            assertTrue(assertNotNull(presenter.state.value.importSheet).busy)
            presenter.confirmReauth("pw", "")
            assertNull(presenter.state.value.importSheet)
            assertEquals(
                SettingsSignInNoticeKind.IMPORT_NONE,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun ssoUnlocksAfterAPasswordWhenItNeedsProof() =
        runTest {
            val server = server(sso = null)
            server.json("GET", "/api/v1/auth/sso", reauthJson, 403)
            val presenter = presenter(server)
            presenter.load()
            assertEquals(SettingsSignInSsoPhase.LOCKED, presenter.state.value.sso.phase)
            assertEquals(Phase.READY, presenter.state.value.status.phase)
            presenter.loadSso()
            assertTrue(presenter.state.value.reauth.needed)
            presenter.cancelReauth()
            assertEquals(SettingsSignInSsoPhase.LOCKED, presenter.state.value.sso.phase)
            server.json("GET", "/api/v1/auth/sso", ssoJson)
            presenter.loadSso()
            assertEquals(SettingsSignInSsoPhase.READY, presenter.state.value.sso.phase)
            assertFalse(presenter.state.value.reauth.needed)
        }

    @Test
    fun ssoLoadFailuresCanBeRetried() =
        runTest {
            val server = server(sso = null)
            server.json("GET", "/api/v1/auth/sso", """{"error":"only the owner sets up single sign-on"}""", 403)
            val presenter = presenter(server)
            presenter.load()
            assertEquals(SettingsSignInSsoPhase.FAILED, presenter.state.value.sso.phase)
            assertEquals(
                ApiErrorCode.FORBIDDEN,
                presenter.state.value.sso.loadError
                    ?.code,
            )
            server.json("GET", "/api/v1/auth/sso", ssoJson)
            presenter.loadSso()
            assertEquals(SettingsSignInSsoPhase.READY, presenter.state.value.sso.phase)
        }

    @Test
    fun ssoEnvironmentIsReadOnly() =
        runTest {
            val server = server(sso = ssoJson.replace("\"source\":\"settings\"", "\"source\":\"environment\""))
            val presenter = presenter(server)
            presenter.load()
            val sso = presenter.state.value.sso
            assertTrue(sso.environment)
            assertNull(sso.twoFactor)
            presenter.saveSso("pw", "")
            assertEquals(0, calls(server, "PUT", "/api/v1/auth/sso"))
            assertEquals(0, calls(server, "GET", "/api/v1/auth/me"))
        }

    @Test
    fun ssoSaveEnforcesTheRulesTheWebDoes() =
        runTest {
            val server = server()
            val saved = ssoJson.replace("\"client_id\":\"holyarrs\"", "\"client_id\":\"new-client\"").replace("\"links\":2", "\"links\":0")
            server.json("PUT", "/api/v1/auth/sso", """{"sso":$saved,"links_removed":2,"sessions_ended":3}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.setSsoClientId("new-client")
            var sso = presenter.state.value.sso
            assertTrue(sso.moving)
            assertTrue(sso.mustRetype)
            presenter.saveSso("pw", "")
            assertEquals(SettingsSignInSsoError.RETYPE, presenter.state.value.sso.saveError)
            assertEquals(0, calls(server, "PUT", "/api/v1/auth/sso"))
            presenter.setSsoPublicClient(true)
            assertFalse(presenter.state.value.sso.mustRetype)
            presenter.setSsoPublicClient(false)
            presenter.setSsoSecret("s3cret")
            presenter.setSsoAllowJoin(true)
            assertTrue(presenter.state.value.sso.opening)
            presenter.saveSso("pw", "")
            assertEquals(SettingsSignInSsoError.CONFIRM_BOX, presenter.state.value.sso.saveError)
            presenter.setSsoConfirmEveryone(true)
            presenter.saveSso(" pw ", " 123456 ")
            sso = presenter.state.value.sso
            val sent = body(server, "PUT", "/api/v1/auth/sso")
            assertEquals(" pw ", sent.getValue("password").jsonPrimitive.content)
            assertEquals("123456", sent.getValue("code").jsonPrimitive.content)
            assertEquals("new-client", sent.getValue("client_id").jsonPrimitive.content)
            assertEquals("s3cret", sent.getValue("client_secret").jsonPrimitive.content)
            assertEquals("https://auth.example.com/", sent.getValue("issuer").jsonPrimitive.content)
            assertEquals(true, sent.getValue("confirm_everyone").jsonPrimitive.boolean)
            assertEquals(true, sent.getValue("allow_join").jsonPrimitive.boolean)
            assertEquals(false, sent.getValue("public_client").jsonPrimitive.boolean)
            assertEquals(
                "https://media.example.com/holyarrs/api/v1/auth/oidc/callback",
                sent.getValue("redirect_url").jsonPrimitive.content,
            )
            assertEquals("groups", sent.getValue("groups_claim").jsonPrimitive.content)
            assertEquals("admins=admin", sent.getValue("group_roles").jsonPrimitive.content)
            assertNull(sso.saveError)
            assertEquals("", sso.secret)
            assertFalse(sso.confirmEveryone)
            assertFalse(sso.saving)
            assertEquals("new-client", sso.view.clientId)
            assertEquals(
                SettingsSignInNoticeKind.SSO_SAVED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                2,
                presenter.state.value.notice
                    ?.count,
            )
            assertEquals(
                3,
                presenter.state.value.notice
                    ?.count2,
            )
        }

    @Test
    fun ssoSaveSeparatesAWrongProofFromOtherAnswers() =
        runTest {
            val server = server()
            val presenter = presenter(server)
            presenter.load()
            server.json("PUT", "/api/v1/auth/sso", """{"error":"wrong password or code","code":"wrong_proof"}""", 403)
            presenter.saveSso("bad", "")
            var sso = presenter.state.value.sso
            assertEquals(SettingsSignInSsoError.WRONG_PROOF, sso.saveError)
            assertFalse(presenter.state.value.reauth.needed)
            assertFalse(sso.saving)
            server.json("PUT", "/api/v1/auth/sso", """{"error":"check the groups claim","code":"group_roles"}""", 400)
            presenter.saveSso("pw", "")
            assertEquals(SettingsSignInSsoError.GROUP_ROLES, presenter.state.value.sso.saveError)
            server.json(
                "PUT",
                "/api/v1/auth/sso",
                """{"error":"too many wrong passwords; wait a little and try again"}""",
                429,
                "Retry-After" to "30",
            )
            presenter.saveSso("pw", "")
            sso = presenter.state.value.sso
            assertEquals(SettingsSignInSsoError.OTHER, sso.saveError)
            assertEquals(ApiErrorCode.RATE_LIMITED, sso.error?.code)
            assertEquals(30, sso.error?.retryAfterSeconds)
        }

    @Test
    fun ssoSaveAsksForProofWhenItExpiredAndRetries() =
        runTest {
            val server = server()
            var puts = 0
            server.on("PUT", "/api/v1/auth/sso") {
                puts++
                if (puts == 1) respondJson(reauthJson, 403) else respondJson("""{"sso":$ssoJson,"links_removed":0,"sessions_ended":0}""")
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.setSsoGroupRoles("admins=admin, family=member")
            presenter.saveSso("pw", "")
            assertTrue(presenter.state.value.reauth.needed)
            assertTrue(presenter.state.value.sso.saving)
            presenter.confirmReauth("pw", "")
            assertEquals(2, puts)
            assertFalse(presenter.state.value.sso.saving)
            assertEquals(
                SettingsSignInNoticeKind.SSO_SAVED,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun ssoTestReportsTheIssuer() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/auth/sso/test", """{"ok":true}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.setSsoIssuer(" https://auth.example.org/ ")
            presenter.testSso()
            var test = presenter.state.value.sso.test
            assertEquals(true, test?.ok)
            assertEquals("https://auth.example.org/", test?.text)
            assertEquals("https://auth.example.org/", body(server, "POST", "/api/v1/auth/sso/test")["issuer"]?.jsonPrimitive?.content)
            server.json("POST", "/api/v1/auth/sso/test", """{"error":"no OpenID Connect provider answered at this issuer"}""", 502)
            presenter.testSso()
            test = presenter.state.value.sso.test
            assertEquals(false, test?.ok)
            assertEquals(ApiErrorCode.SERVER, test?.error?.code)
            assertFalse(presenter.state.value.sso.testing)
            presenter.setSsoIssuer("https://other/")
            assertNull(presenter.state.value.sso.test)
        }

    @Test
    fun skipSwitchSendsNetworksAndPersonWhenTurnedOn() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) {
                respondJson(
                    settings(
                        "\"skip_login_local_network\":false" to "\"skip_login_local_network\":true",
                    ),
                )
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.pickPerson(2)
            presenter.setSkip(true)
            val on = body(server, "PUT", settingsPath)
            assertEquals(setOf("skip_login_local_network", "local_networks", "local_person"), on.keys)
            assertEquals(
                2L,
                on
                    .getValue("local_person")
                    .jsonPrimitive.content
                    .toLong(),
            )
            assertTrue(presenter.state.value.local.skip)
            assertTrue(presenter.state.value.showLocal)
            assertEquals(
                SettingsSignInNoticeKind.LOCAL_ON,
                presenter.state.value.notice
                    ?.kind,
            )
            server.on("PUT", settingsPath) { respondJson(settingsJson) }
            presenter.setSkip(false)
            assertEquals(setOf("skip_login_local_network"), body(server, "PUT", settingsPath).keys)
            assertFalse(presenter.state.value.local.skip)
            assertEquals(
                SettingsSignInNoticeKind.LOCAL_OFF,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun skipSwitchRollsBackWhenTheServerRefuses() =
        runTest {
            val server = server()
            server.json("PUT", settingsPath, """{"error":"List at least one local network first"}""", 400)
            val presenter = presenter(server)
            presenter.load()
            presenter.setSkip(true)
            val s = presenter.state.value
            assertFalse(s.local.skip)
            assertFalse(s.local.skipSaving)
            assertEquals(ApiErrorCode.BAD_REQUEST, s.notice?.error?.code)
        }

    @Test
    fun localNetworkSavesNetworksPersonAndOnlyChangedProxies() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) { respondJson(settingsJson) }
            val presenter = presenter(server)
            presenter.load()
            assertFalse(presenter.state.value.local.dirty)
            presenter.setLocalNetworks("192.168.1.0/24\n192.168.l.0/24\n10.0.0/8")
            var local = presenter.state.value.local
            assertEquals(listOf(2, 3), local.netProblems.map { it.line })
            assertEquals(2, local.problemCount)
            assertFalse(local.canSave)
            presenter.saveLocal()
            assertEquals(0, calls(server, "PUT", settingsPath))
            presenter.setLocalNetworks("192.168.1.0/24\n10.0.0.0/8")
            presenter.pickPerson(2)
            local = presenter.state.value.local
            assertTrue(local.canSave)
            assertEquals(2L, local.pickedId)
            presenter.saveLocal()
            var sent = body(server, "PUT", settingsPath)
            assertEquals(setOf("local_networks", "local_person"), sent.keys)
            assertEquals(listOf("192.168.1.0/24", "10.0.0.0/8"), sent.getValue("local_networks").jsonArray.map { it.jsonPrimitive.content })
            assertEquals(
                SettingsSignInNoticeKind.LOCAL_SAVED,
                presenter.state.value.notice
                    ?.kind,
            )
            presenter.setLocalProxies("10.0.0.5\n10.0.0.6")
            presenter.setLocalNetworks("192.168.1.0/24")
            presenter.saveLocal()
            sent = body(server, "PUT", settingsPath)
            assertEquals(listOf("10.0.0.5", "10.0.0.6"), sent.getValue("trusted_proxies").jsonArray.map { it.jsonPrimitive.content })
            presenter.setLocalProxies("172.17.0.2")
            assertEquals(0, presenter.state.value.local.proxyProblems.size)
            presenter.setLocalProxies("172.17.0.x")
            assertEquals(1, presenter.state.value.local.proxyProblems.size)
        }

    @Test
    fun localNetworkFailureStaysInsideTheCard() =
        runTest {
            val server = server()
            server.json("PUT", settingsPath, """{"error":"you reach HolyArrs through the proxy at 10.0.0.5; keep it in the list"}""", 409)
            val presenter = presenter(server)
            presenter.load()
            presenter.setLocalProxies("10.0.0.9")
            presenter.saveLocal()
            val s = presenter.state.value
            assertEquals(ApiErrorCode.CONFLICT, s.local.saveError?.code)
            assertNull(s.notice)
            assertFalse(s.local.saving)
            assertEquals("10.0.0.9", s.local.proxies)
        }

    @Test
    fun homeRangesAndTailscaleEditTheTextBox() =
        runTest {
            val server = server()
            val presenter = presenter(server)
            presenter.load()
            presenter.addHomeRanges()
            assertEquals(
                listOf("192.168.1.0/24", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "fc00::/7"),
                SettingsSignInLines.lines(presenter.state.value.local.networks),
            )
            presenter.addHomeRanges()
            assertEquals(5, SettingsSignInLines.lines(presenter.state.value.local.networks).size)
            assertFalse(presenter.state.value.local.hasTailscale)
            presenter.setTailscale(true)
            assertTrue(presenter.state.value.local.hasTailscale)
            assertEquals("100.64.0.0/10", SettingsSignInLines.lines(presenter.state.value.local.networks).last())
            presenter.setTailscale(false)
            assertFalse(presenter.state.value.local.hasTailscale)
            assertEquals(5, SettingsSignInLines.lines(presenter.state.value.local.networks).size)
        }

    @Test
    fun theSeenAddressIsClassified() =
        runTest {
            val bridge = server(settings("\"seen_address\":\"192.168.1.40\"" to "\"seen_address\":\"172.17.0.1\""))
            val onBridge = presenter(bridge)
            onBridge.load()
            assertEquals(SettingsSignInSeen.BRIDGE, onBridge.state.value.local.seen)
            val own =
                server(
                    settings(
                        "\"seen_address\":\"192.168.1.40\"" to "\"seen_address\":\"172.17.0.1\"",
                        "\"seen_gateway\":false" to "\"seen_gateway\":true",
                    ),
                )
            val ownGateway = presenter(own)
            ownGateway.load()
            assertEquals(SettingsSignInSeen.OWN_GATEWAY, ownGateway.state.value.local.seen)
            val away = server(settings("\"seen_address\":\"192.168.1.40\"" to "\"seen_address\":\"10.9.9.9\""))
            val outside = presenter(away)
            outside.load()
            assertEquals(SettingsSignInSeen.NOT_COVERED, outside.state.value.local.seen)
            val none = server(settings("\"seen_address\":\"192.168.1.40\"" to "\"seen_address\":\"\""))
            val unknown = presenter(none)
            unknown.load()
            assertEquals(SettingsSignInSeen.NONE, unknown.state.value.local.seen)
        }

    @Test
    fun skipOffReasonsAreMapped() =
        runTest {
            val gone = presenter(server(settings("\"local_skip_off\":\"\"" to "\"local_skip_off\":\"person_gone\"")))
            gone.load()
            assertEquals(SettingsSignInSkipOff.PERSON_GONE, gone.state.value.local.skipOff)
            val kid = presenter(server(settings("\"local_skip_off\":\"\"" to "\"local_skip_off\":\"kid\"")))
            kid.load()
            assertEquals(SettingsSignInSkipOff.KID, kid.state.value.local.skipOff)
        }

    @Test
    fun hostNamesSaveAsAList() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) {
                respondJson(
                    settings(
                        "\"allowed_hosts\":[\"tower.local\"]" to "\"allowed_hosts\":[\"tower.local\",\"media.example.com\"]",
                    ),
                )
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.setHosts("tower.local, media.example.com\n")
            presenter.saveHosts()
            val sent = body(server, "PUT", settingsPath)
            assertEquals(setOf("allowed_hosts"), sent.keys)
            assertEquals(
                listOf("tower.local", "media.example.com"),
                sent.getValue("allowed_hosts").jsonArray.map { it.jsonPrimitive.content },
            )
            assertEquals("tower.local\nmedia.example.com", presenter.state.value.hosts.text)
            assertEquals(
                SettingsSignInNoticeKind.HOSTS_SAVED,
                presenter.state.value.notice
                    ?.kind,
            )
            server.json("PUT", settingsPath, """{"error":"you opened HolyArrs as tower.local; keep that name in the list"}""", 409)
            presenter.setHosts("media.example.com")
            presenter.saveHosts()
            assertEquals(
                SettingsSignInNoticeKind.FAILED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals("media.example.com", presenter.state.value.hosts.text)
            assertFalse(presenter.state.value.hosts.saving)
        }

    @Test
    fun savingOneCardKeepsWhatWasTypedInAnother() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) {
                respondJson(
                    settings(
                        "\"require_two_factor_admins\":false" to "\"require_two_factor_admins\":true",
                    ),
                )
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.setHosts("typed.example.com")
            presenter.setLocalNetworks("10.0.0.0/8")
            presenter.setWayAllow(SettingsSignInWayKind.PLEX, "someone")
            presenter.setRequireTwoFactor(true)
            val s = presenter.state.value
            assertEquals("typed.example.com", s.hosts.text)
            assertEquals("10.0.0.0/8", s.local.networks)
            assertEquals("someone", s.way(SettingsSignInWayKind.PLEX).allow)
            assertTrue(s.requireTwoFactor)
        }

    @Test
    fun aWriteThatNeedsProofIsRetriedAfterTheOwnerConfirms() =
        runTest {
            val server = server()
            var puts = 0
            server.on("PUT", settingsPath) {
                puts++
                if (puts ==
                    1
                ) {
                    respondJson(reauthJson, 403)
                } else {
                    respondJson(settings("\"owner_reset_link\":true" to "\"owner_reset_link\":false"))
                }
            }
            val failures = mutableListOf<ApiError>()
            val presenter =
                presenter(server) { _, _ ->
                    ApiError(ApiErrorCode.FORBIDDEN, 403, "wrong").also { failures += it }
                }
            presenter.load()
            presenter.setForgotLink(false)
            assertTrue(presenter.state.value.reauth.needed)
            assertTrue(presenter.state.value.forgotLinkSaving)
            presenter.confirmReauth("bad", "")
            assertTrue(presenter.state.value.reauth.needed)
            assertEquals(
                ApiErrorCode.FORBIDDEN,
                presenter.state.value.reauth.error
                    ?.code,
            )
            assertEquals(1, puts)
            presenter.cancelReauth()
            assertTrue(presenter.state.value.forgotLink)
            assertFalse(presenter.state.value.forgotLinkSaving)
            assertFalse(presenter.state.value.reauth.needed)
        }

    @Test
    fun noticesAreNumberedAndDismissable() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) { respondJson(settingsJson) }
            val presenter = presenter(server)
            presenter.load()
            presenter.setForgotLink(true)
            val first = assertNotNull(presenter.state.value.notice)
            presenter.setForgotLink(true)
            val second = assertNotNull(presenter.state.value.notice)
            assertTrue(second.seq > first.seq)
            presenter.dismissNotice(first.seq)
            assertNotNull(presenter.state.value.notice)
            presenter.dismissNotice(second.seq)
            assertNull(presenter.state.value.notice)
        }

    @Test
    fun theRequestBodyIsTheJsonTheServerExpects() =
        runTest {
            val server = server()
            server.on("PUT", settingsPath) { respondJson(settingsJson) }
            val presenter = presenter(server)
            presenter.load()
            presenter.setMinLength("9")
            presenter.saveMinLength()
            assertEquals("""{"min_password_length":9}""", server.request("PUT", settingsPath).body)
        }
}
