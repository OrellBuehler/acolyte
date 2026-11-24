package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.AccountSession
import ch.orellbuehler.acolyte.api.model.AccountSsoLink
import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.account.AccountDevicesPresenter
import ch.orellbuehler.acolyte.feature.account.AccountImportPresenter
import ch.orellbuehler.acolyte.feature.account.AccountKeysPresenter
import ch.orellbuehler.acolyte.feature.account.AccountProblem
import ch.orellbuehler.acolyte.feature.account.AccountProfilePresenter
import ch.orellbuehler.acolyte.feature.account.AccountSaved
import ch.orellbuehler.acolyte.feature.account.AccountSecurityNotice
import ch.orellbuehler.acolyte.feature.account.AccountSecurityPresenter
import ch.orellbuehler.acolyte.feature.account.AccountSecurityProblem
import ch.orellbuehler.acolyte.feature.account.AccountWizardPresenter
import ch.orellbuehler.acolyte.feature.account.AiConnectPresenter
import ch.orellbuehler.acolyte.feature.account.PeopleInviteDraft
import ch.orellbuehler.acolyte.feature.account.PeopleInviteState
import ch.orellbuehler.acolyte.feature.account.PeopleNotice
import ch.orellbuehler.acolyte.feature.account.PeoplePresenter
import ch.orellbuehler.acolyte.feature.account.PeoplePreset
import ch.orellbuehler.acolyte.feature.account.PeopleProblem
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.feature.account.ReauthMode
import ch.orellbuehler.acolyte.feature.account.peoplePreset
import ch.orellbuehler.acolyte.feature.account.state
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountSnapshot
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountPresenterTest {
    private fun account(
        role: String = "owner",
        perms: List<String> = listOf("ask", "settings"),
        hasPassword: Boolean = true,
        authMethod: String = "local_password",
        off: List<String> = emptyList(),
    ): Account {
        val parts = listOf("movies", "wishlist").map { PartView(part = it, on = it !in off) }
        val me =
            Me(id = 1, name = "admin", role = role, perms = perms, hasPassword = hasPassword, authMethod = authMethod, twoFactor = true)
        return Account.from(BASE, SignInMethod.PASSWORD, AccountSnapshot(me, Parts(parts)))
    }

    private fun gate(
        account: Account = account(),
        reauth: suspend (String, String?) -> ch.orellbuehler.acolyte.api.ApiError? = { _, _ -> null },
    ) = ReauthGate(reauth, { account })

    @Test
    fun reauthRetriesTheRequestAfterTheProof() =
        runTest {
            val server = FakeServer()
            var calls = 0
            server.on("POST", "/api/v1/auth/two-factor/disable") {
                calls++
                if (calls == 1) {
                    respondJson("""{"error":"confirm your local password again before using this route","code":"reauth"}""", 403)
                } else {
                    respondJson("{}", 204)
                }
            }
            var proof: Pair<String, String?>? = null
            val gate =
                gate { password, code ->
                    proof = password to code
                    null
                }
            val security = AccountSecurityPresenter(server.api(), { account() }, gate) {}
            val job = async { security.disableTwoFactor("123456") }
            val prompt = gate.state.first { it.visible }
            assertEquals(ReauthMode.PASSWORD, prompt.mode)
            assertTrue(prompt.needsCode)
            gate.confirm("secret", " 123456 ")
            job.await()
            assertEquals("secret" to "123456", proof)
            assertEquals(2, calls)
            assertEquals(AccountSecurityNotice.TWO_FACTOR_OFF, security.state.value.notice)
            assertFalse(gate.state.value.visible)
        }

    @Test
    fun reauthCancelKeepsTheFailureAndPasswordlessPeopleSignInAgain() =
        runTest {
            val server = FakeServer()
            server.json("DELETE", "/api/v1/auth/keys/4", """{"error":"confirm","code":"reauth"}""", 403)
            val gate = gate(account(hasPassword = false, authMethod = "plex"))
            val keys = AccountKeysPresenter(server.api(), gate, false)
            val job =
                async {
                    keys.revoke(
                        ch.orellbuehler.acolyte.api.model
                            .AccountKey(id = 4, label = "x"),
                    )
                }
            val prompt = gate.state.first { it.visible }
            assertEquals(ReauthMode.SIGN_IN_AGAIN, prompt.mode)
            gate.cancel()
            job.await()
            assertEquals(
                ApiErrorCode.REAUTH,
                keys.state.value.error
                    ?.code,
            )
        }

    @Test
    fun wrongProofStaysOpen() =
        runTest {
            val server = FakeServer()
            server.json("POST", "/api/v1/auth/password", """{"error":"x","code":"reauth"}""", 403)
            val gate =
                gate {
                    _,
                    _,
                    ->
                    ch.orellbuehler.acolyte.api
                        .ApiError(ApiErrorCode.UNAUTHORIZED, 401, "the password or code is wrong")
                }
            val security = AccountSecurityPresenter(server.api(), { account() }, gate) {}
            val job = async { security.changePassword("old", "newpassword", "newpassword") }
            gate.state.first { it.visible }
            gate.confirm("bad", "")
            assertEquals(
                ApiErrorCode.UNAUTHORIZED,
                gate.state.value.error
                    ?.code,
            )
            assertTrue(gate.state.value.visible)
            gate.cancel()
            job.await()
        }

    @Test
    fun profileLoadsAndSaves() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/my/profile", """{"display_name":"Adm","username":"admin","language":"de"}""")
            server.json("GET", "/api/v1/my/alerts", """{"email":"a@b.c","discord_id":"","new_episodes":null}""")
            server.json(
                "GET",
                "/api/v1/my/follows",
                """{"follows":[{"title_id":12,"kind":"show","name":"Show","year":2020,"unfollow":"/titles/12/follow","x":1},""" +
                    """{"title_id":0,"kind":"movie","tmdb_id":"603","unfollow":"/metadata/movie/603/follow"}]}""",
            )
            server.json("PUT", "/api/v1/my/profile", """{"display_name":"New","username":"admin","language":"de"}""")
            server.json("PUT", "/api/v1/my/alerts", """{"email":"x@y.z","discord_id":"123","new_episodes":false}""")
            server.json("PUT", "/api/v1/my/language", """{"display_name":"New","username":"admin","language":""}""")
            server.json("DELETE", "/api/v1/titles/12/follow", """{"following":false}""")
            server.json("DELETE", "/api/v1/metadata/movie/603/follow", """{"following":false}""")
            var pictureType = ""
            server.on("PUT", "/api/v1/account/avatar") {
                pictureType = it.body.contentType.toString()
                respondJson("""{"avatar":"/api/v1/users/1/avatar?v=ab"}""")
            }
            server.json("DELETE", "/api/v1/account/avatar", "{}", 204)
            var refreshed = 0
            val presenter = AccountProfilePresenter(server.api(), { account() }) { refreshed++ }
            presenter.load()
            val loaded = presenter.state.value
            assertEquals(Phase.READY, loaded.status.phase)
            assertEquals("de", loaded.profile.language)
            assertTrue(loaded.newEpisodes)
            assertEquals(2, loaded.follows.size)

            presenter.saveName("x".repeat(65))
            assertEquals(AccountProblem.NAME_INVALID, presenter.state.value.problem)
            presenter.saveName(" New ")
            assertEquals("""{"display_name":"New"}""", server.request("PUT", "/api/v1/my/profile").body)
            assertEquals("New", presenter.state.value.profile.displayName)
            assertEquals(AccountSaved.NAME, presenter.state.value.saved)

            presenter.saveLanguage("")
            assertEquals("""{"language":""}""", server.request("PUT", "/api/v1/my/language").body)

            presenter.saveAlerts(" x@y.z ", "123", false)
            assertEquals("""{"email":"x@y.z","discord_id":"123","new_episodes":false}""", server.request("PUT", "/api/v1/my/alerts").body)
            assertFalse(presenter.state.value.newEpisodes)

            presenter.unfollow(loaded.follows[0])
            presenter.unfollow(loaded.follows[1])
            assertTrue(
                presenter.state.value.follows
                    .isEmpty(),
            )
            assertEquals(AccountSaved.UNFOLLOWED, presenter.state.value.saved)

            presenter.uploadPicture(ByteArray(6 * 1024 * 1024), "image/jpeg")
            assertEquals(AccountProblem.PICTURE_TOO_BIG, presenter.state.value.problem)
            presenter.uploadPicture(byteArrayOf(1, 2, 3), "image/jpeg")
            assertEquals("image/jpeg", pictureType)
            presenter.removePicture()
            assertEquals(AccountSaved.PICTURE_REMOVED, presenter.state.value.saved)
            assertTrue(refreshed >= 3)
        }

    @Test
    fun followsAreSkippedWhenRequestsAreOff() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/my/profile", """{"display_name":"Adm","username":"admin"}""")
            server.json("GET", "/api/v1/my/alerts", """{"email":"","discord_id":""}""")
            val presenter = AccountProfilePresenter(server.api(), { account(off = listOf("wishlist")) }) {}
            presenter.load()
            assertFalse(presenter.state.value.followsOn)
            assertTrue(server.requests.none { it.path == "/api/v1/my/follows" })
        }

    @Test
    fun securityPasswordAndTwoFactor() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":false,"ways":["password"],"min_password_length":10}""")
            server.json("GET", "/api/v1/auth/plex/my-link", """{"linked":true,"name":"anna","kept":true}""")
            server.json("GET", "/api/v1/auth/media-link", """{"servers":[{"id":2,"name":"Jelly","kind":"jellyfin"}],"links":[]}""")
            server.json(
                "GET",
                "/api/v1/auth/sso/links",
                """{"links":[{"kind":"oidc","issuer":"https://sso","name":"anna","created_at":"x"}]}""",
            )
            server.json("POST", "/api/v1/auth/password", "", 204)
            server.json("POST", "/api/v1/auth/two-factor/setup", """{"secret":"ABCD","uri":"otpauth://totp/x","qr":["101"]}""")
            server.json("POST", "/api/v1/auth/two-factor/enable", """{"backup_codes":["a-b","c-d"]}""")
            server.json("POST", "/api/v1/auth/media-link/2", """{"provider":"jellyfin","name":"anna"}""")
            server.json("POST", "/api/v1/auth/sso/unlink", "", 204)
            val security = AccountSecurityPresenter(server.api(), { account() }, gate()) {}
            security.load()
            val loaded = security.state.value
            assertEquals(10, loaded.minPasswordLength)
            assertTrue(loaded.plex.linked)
            assertEquals(1, loaded.sso.size)

            security.changePassword("old", "short", "short")
            assertEquals(AccountSecurityProblem.PASSWORD_SHORT, security.state.value.problem)
            security.changePassword("old", "longenough1", "different11")
            assertEquals(AccountSecurityProblem.PASSWORDS_DIFFER, security.state.value.problem)
            security.changePassword("old", "longenough1", "longenough1")
            assertEquals("""{"current":"old","new":"longenough1"}""", server.request("POST", "/api/v1/auth/password").body)
            assertEquals(AccountSecurityNotice.PASSWORD_CHANGED, security.state.value.notice)

            security.startTwoFactor()
            assertEquals(
                "ABCD",
                security.state.value.setup
                    ?.secret,
            )
            security.enableTwoFactor("12 34")
            assertEquals(AccountSecurityProblem.CODE_INVALID, security.state.value.problem)
            security.enableTwoFactor("123456")
            assertEquals(listOf("a-b", "c-d"), security.state.value.backupCodes)
            assertNull(security.state.value.setup)
            security.dismissBackupCodes()
            assertTrue(
                security.state.value.backupCodes
                    .isEmpty(),
            )

            security.linkMedia(2, " anna ", "pw")
            assertEquals("""{"name":"anna","password":"pw"}""", server.request("POST", "/api/v1/auth/media-link/2").body)
            security.unlinkSso(AccountSsoLink("oidc", "https://sso", "anna"))
            assertEquals("""{"kind":"oidc","issuer":"https://sso"}""", server.request("POST", "/api/v1/auth/sso/unlink").body)
        }

    @Test
    fun firstPasswordOmitsTheCurrentOne() =
        runTest {
            val server = FakeServer()
            server.json("POST", "/api/v1/auth/password", "", 204)
            val security = AccountSecurityPresenter(server.api(), { account(hasPassword = false, authMethod = "plex") }, gate()) {}
            security.changePassword("", "longenough1", "longenough1")
            assertEquals("""{"new":"longenough1"}""", server.request("POST", "/api/v1/auth/password").body)
            assertEquals(AccountSecurityNotice.PASSWORD_SET, security.state.value.notice)
        }

    @Test
    fun plexLinkPollsAndKeepsTheName() =
        runTest {
            val server = FakeServer()
            server.json("POST", "/api/v1/auth/plex/my-link/start", """{"id":9,"auth_url":"https://app.plex.tv/auth#?c=1"}""")
            var polls = 0
            server.on("POST", "/api/v1/auth/plex/my-link/finish") {
                polls++
                if (polls == 1) respondJson("""{"waiting":true}""", 202) else respondJson("""{"linked":true,"name":"anna","kept":true}""")
            }
            val security = AccountSecurityPresenter(server.api(), { account() }, gate()) {}
            security.linkPlex()
            assertEquals(2, polls)
            assertTrue(security.state.value.plex.linked)
            assertEquals(AccountSecurityNotice.PLEX_LINKED, security.state.value.notice)
            assertEquals("anna", security.state.value.noticeText)
            assertNull(security.state.value.plexUrl)
        }

    @Test
    fun devicesAreOrderedAndEnded() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/auth/sessions",
                """{"sessions":[{"id":"a","device":"Old","last_used_at":"2026-01-01T00:00:00Z"},""" +
                    """{"id":"b","device":"This","last_used_at":"2025-01-01T00:00:00Z","current":true},""" +
                    """{"id":"c","device":"New","last_used_at":"2026-06-01T00:00:00Z"}]}""",
            )
            server.json("DELETE", "/api/v1/auth/sessions/a", "", 204)
            server.json("DELETE", "/api/v1/auth/sessions", """{"ended":1}""")
            val devices = AccountDevicesPresenter(server.api(), null)
            devices.load()
            assertEquals(
                listOf("b", "c", "a"),
                devices.state.value.ordered
                    .map { it.id },
            )
            assertTrue(devices.state.value.hasOthers)
            devices.end(AccountSession(id = "a"))
            assertEquals(1, devices.state.value.ended)
            devices.endAll()
            assertEquals(1, devices.state.value.ended)
            assertTrue(server.requests.count { it.path == "/api/v1/auth/sessions" && it.method == "GET" } >= 2)
        }

    @Test
    fun otherPeopleDevicesUseTheUserRoutes() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/users/5/sessions", """{"sessions":[{"id":"z","device":"Phone"}]}""")
            server.json("POST", "/api/v1/users/5/sessions/end-all", """{"ended":2}""")
            server.json("GET", "/api/v1/users/5/sessions", """{"sessions":[]}""")
            val devices = AccountDevicesPresenter(server.api(), 5)
            devices.load()
            devices.endAll()
            assertEquals(2, devices.state.value.ended)
        }

    private val usersJson =
        """{"users":[
            {"id":1,"name":"admin","role":"owner","state":"active","perms":["ask","approve"],"two_factor":true,"password":true},
            {"id":2,"name":"ann","role":"admin","state":"active","perms":["ask","settings"],
             "sso_links":[{"kind":"oidc","issuer":"i","name":"ann"}]},
            {"id":3,"name":"kid","role":"member","state":"active","perms":["ask"],"max_rating":"PG",
             "links":[{"provider":"plex","name":"k"}]},
            {"id":4,"name":"bob","role":"member","state":"disabled","off_reason":"jellyfin",
             "perms":["ask","auto_approve","approve","manage_library"]}
        ],"perms":["ask","auto_approve","auto_approve_four_k","approve","manage_library","settings"]}"""

    @Test
    fun peopleRulesFollowTheRole() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":false,"ways":["password"],"min_password_length":8}""")
            server.json("GET", "/api/v1/users", usersJson)
            server.json(
                "GET",
                "/api/v1/users/invites",
                """{"invites":[{"id":"h1","role":"member","max_uses":1,"uses":0,"never_expires":true,"note":"Mum"},""" +
                    """{"id":"h2","role":"member","max_uses":1,"uses":1,"used_at":"2026-01-01T00:00:00Z","used_by":"mum"},""" +
                    """{"id":"h3","role":"member","max_uses":0,"uses":3,"expires_at":"2020-01-01T00:00:00Z"}]}""",
            )
            val owner = PeoplePresenter(server.api(), { account() }, gate())
            owner.load()
            val state = owner.state.value
            assertEquals(4, state.users.size)
            assertFalse(state.mayChange(state.users[0]))
            assertTrue(state.mayChange(state.users[1]))
            assertTrue(state.mayChange(state.users[2]))
            assertTrue(state.isKid(state.users[2]))
            assertEquals(PeoplePreset.KID, state.presetOf(state.users[2]))
            assertEquals(PeoplePreset.MANAGER, state.presetOf(state.users[3]))
            assertEquals(PeoplePreset.MANAGER, peoplePreset(listOf("ask", "auto_approve", "approve", "manage_library"), ""))
            assertEquals(
                listOf(PeopleInviteState.OPEN, PeopleInviteState.USED, PeopleInviteState.ENDED),
                state.invites.map { it.state },
            )
            assertEquals(6, state.grantable.size)
            assertEquals("jellyfin", state.users[3].offReason)

            val admin = PeoplePresenter(server.api(), { account(role = "admin", perms = listOf("ask", "settings")) }, gate())
            admin.load()
            val limited = admin.state.value
            assertEquals(listOf("ask", "settings"), limited.grantable)
            assertTrue(limited.mayChange(limited.users[3]))
            assertFalse(limited.mayChange(limited.users[1]))
            assertFalse(limited.canMakeAdmin)
        }

    @Test
    fun peopleActionsSendTheRightBodies() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":false,"ways":["password"],"min_password_length":8}""")
            server.json("GET", "/api/v1/users", usersJson)
            server.json("GET", "/api/v1/users/invites", """{"invites":[]}""")
            server.json(
                "POST",
                "/api/v1/users/invites",
                """{"id":"h9","path":"/invite/tok9","role":"member","max_uses":1,"never_expires":false}""",
                201,
            )
            server.json("PUT", "/api/v1/users/3", """{"id":3,"role":"member","state":"active","perms":["ask"]}""")
            server.json("POST", "/api/v1/users/3/password", "", 204)
            server.json("POST", "/api/v1/users/3/two-factor/reset", "", 204)
            server.json("DELETE", "/api/v1/users/3", "", 204)
            server.json("DELETE", "/api/v1/users/invites/h1", "", 204)
            val people = PeoplePresenter(server.api(), { account() }, gate())
            people.load()

            people.makeInvite(PeopleInviteDraft(days = 31))
            assertEquals(PeopleProblem.INVALID_DAYS, people.state.value.problem)
            people.makeInvite(PeopleInviteDraft(rating = "PG", note = "  Kid  "))
            assertEquals(
                """{"role":"member","perms":["ask"],"note":"Kid","days":7,"max_uses":1,"max_rating":"PG","rating_country":"US"}""",
                server.request("POST", "/api/v1/users/invites").body,
            )
            assertEquals(
                "$BASE/invite/tok9",
                people.state.value.fresh
                    ?.url,
            )
            assertTrue(
                people.state.value.fresh
                    ?.singleUse == true,
            )

            val kid = people.state.value.users[2]
            people.save(kid, admin = false, perms = listOf("ask", "auto_approve"), rating = "PG-13")
            assertEquals(
                """{"role":"member","perms":["ask","auto_approve"],"max_rating":"PG-13","rating_country":"US"}""",
                server.request("PUT", "/api/v1/users/3").body,
            )
            people.turnOff(kid)
            assertEquals("""{"state":"disabled"}""", server.request("PUT", "/api/v1/users/3").body)
            assertEquals(PeopleNotice.TURNED_OFF, people.state.value.notice)
            people.setPassword(kid, "short")
            assertEquals(PeopleProblem.PASSWORD_SHORT, people.state.value.problem)
            people.setPassword(kid, "longenough")
            assertEquals("""{"password":"longenough"}""", server.request("POST", "/api/v1/users/3/password").body)
            people.resetTwoFactor(kid)
            people.delete(kid)
            assertEquals(PeopleNotice.DELETED, people.state.value.notice)
            people.revokeInvite(
                ch.orellbuehler.acolyte.api.model
                    .PeopleInvite(id = "h1"),
            )
            assertNotNull(server.requests.firstOrNull { it.method == "DELETE" && it.path == "/api/v1/users/invites/h1" })
        }

    @Test
    fun unlinkingTheOnlyWayInAsksAgain() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/state", """{"needs_setup":false,"ways":["password"]}""")
            server.json("GET", "/api/v1/users", usersJson)
            server.json("GET", "/api/v1/users/invites", """{"invites":[]}""")
            var calls = 0
            server.on("POST", "/api/v1/users/2/sso/unlink") {
                calls++
                if (calls == 1) respondJson("""{"error":"only way","code":"no_way_in"}""", 409) else respondJson("", 204)
            }
            val people = PeoplePresenter(server.api(), { account() }, gate())
            people.load()
            val ann = people.state.value.users[1]
            people.unlinkSso(ann, ann.ssoLinks[0])
            assertNotNull(people.state.value.noWayIn)
            people.confirmNoWayIn()
            assertNull(people.state.value.noWayIn)
            assertEquals(
                """{"kind":"oidc","issuer":"i","confirm_no_way_in":true}""",
                server.request("POST", "/api/v1/users/2/sso/unlink").body,
            )
            assertEquals(PeopleNotice.SSO_UNLINKED, people.state.value.notice)
        }

    @Test
    fun apiKeysAndAiKeysAreSplitByScope() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/auth/keys",
                """{"keys":[{"id":1,"label":"Script","hint":"…abcd","scopes":["all"]},""" +
                    """{"id":2,"label":"Claude","hint":"…wxyz","scopes":["mcp"],"mcp_groups":["see","add"]}]}""",
            )
            server.json("POST", "/api/v1/auth/keys", """{"id":3,"label":"New","scopes":["mcp"],"key":"hak_secret","note":"once"}""", 201)
            server.json("PUT", "/api/v1/auth/keys/2", """{"id":2,"label":"Claude","mcp_groups":["see"]}""")
            val api = AccountKeysPresenter(server.api(), gate(), false)
            api.load()
            assertEquals(
                listOf("Script"),
                api.state.value.keys
                    .map { it.label },
            )
            api.make(" Home ")
            assertEquals("""{"label":"Home"}""", server.request("POST", "/api/v1/auth/keys").body)
            assertEquals(
                "hak_secret",
                api.state.value.made
                    ?.key,
            )
            api.dismissMade()
            assertNull(api.state.value.made)

            val ai = AccountKeysPresenter(server.api(), gate(), true)
            ai.load()
            assertEquals(
                listOf("Claude"),
                ai.state.value.keys
                    .map { it.label },
            )
            ai.make("Cursor", listOf("approve", "see", "bogus"))
            assertEquals(
                """{"label":"Cursor","scopes":["mcp"],"mcp_groups":["see","approve"]}""",
                server.request("POST", "/api/v1/auth/keys").body,
            )
            ai.setGroups(ai.state.value.keys[0], listOf("see"))
            assertEquals("""{"mcp_groups":["see"]}""", server.request("PUT", "/api/v1/auth/keys/2").body)
        }

    @Test
    fun importListsAndSendsThePickedPeople() =
        runTest {
            val server = FakeServer()
            server.json(
                "POST",
                "/api/v1/auth/import/jellyfin/preview",
                """{"people":[{"id":"u1","name":"Anna","kid":true},{"id":"u2","name":"Bob","server_admin":true},""" +
                    """{"id":"u3","name":"Cy","linked_to":"cy"}]}""",
            )
            server.json("POST", "/api/v1/auth/import/jellyfin", """{"made":["Anna"],"skipped":0}""")
            server.json("POST", "/api/v1/auth/media-server/emby/test", """{"ok":true,"name":"Home","version":"4.8","product":"Emby"}""")
            val import = AccountImportPresenter(server.api(), gate(), "jellyfin")
            import.preview(" admin ", "pw")
            assertEquals("""{"name":"admin","password":"pw"}""", server.request("POST", "/api/v1/auth/import/jellyfin/preview").body)
            assertEquals(setOf("u1"), import.state.value.picked)
            assertEquals(2, import.state.value.importable.size)
            import.toggleKid("u1")
            import.import()
            assertEquals(
                """{"people":[{"id":"u1","kid":false,"confirm_not_kid":true}]}""",
                server.request("POST", "/api/v1/auth/import/jellyfin").body,
            )
            assertEquals(listOf("Anna"), import.state.value.made)
            assertEquals("4.8", import.test("emby", " http://nas:8096 ")?.version)
            assertEquals("""{"url":"http://nas:8096"}""", server.request("POST", "/api/v1/auth/media-server/emby/test").body)
        }

    @Test
    fun wizardLinksServersAndFinishes() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/auth/setup/discover",
                """{"servers":[{"kind":"jellyfin","name":"Home","address":"http://192.168.1.2:8096"}]}""",
            )
            server.json("POST", "/api/v1/auth/setup/link/jellyfin", """{"provider":"jellyfin","name":"anna","url":"http://nas:8096"}""")
            server.json("POST", "/api/v1/auth/setup/done", """{"done":true}""")
            var refreshed = 0
            val wizard = AccountWizardPresenter(server.api(), { account() }, gate()) { refreshed++ }
            wizard.discover()
            assertEquals(1, wizard.state.value.found.size)
            wizard.linkServer("jellyfin", "nas:8096", " anna ", "pw")
            assertEquals(
                """{"url":"http://nas:8096","name":"anna","password":"pw"}""",
                server.request("POST", "/api/v1/auth/setup/link/jellyfin").body,
            )
            assertEquals(
                listOf("anna"),
                wizard.state.value.linked
                    .map { it.name },
            )
            wizard.finish(skipped = true)
            assertEquals("skipped=1", server.request("POST", "/api/v1/auth/setup/done").query)
            assertTrue(wizard.state.value.finished)
            assertEquals(1, refreshed)
        }

    @Test
    fun aiConnectReadsTheAuthorizeQuery() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/auth/ai-connect",
                """{"client_name":"Claude","returns_to":"claude.ai","local":false,"enabled":true,"deny_url":"https://x/?error=access_denied"}""",
            )
            server.json("POST", "/api/v1/auth/ai-connect", """{"redirect":"https://claude.ai/cb?code=1&state=s"}""")
            val link =
                "https://media.example.com/holyarrs/ai?response_type=code&client_id=hai_1&redirect_uri=https%3A%2F%2Fclaude.ai%2Fcb" +
                    "&state=s&scope=mcp"
            val ai = AiConnectPresenter(server.api(), { account() }, gate(), link)
            ai.load()
            assertTrue(ai.state.value.mayAllow)
            assertEquals("Claude", ai.state.value.info.clientName)
            val sent = server.request("GET", "/api/v1/auth/ai-connect").query
            assertTrue(sent.contains("client_id=hai_1"))
            assertTrue(sent.contains("redirect_uri=https%3A%2F%2Fclaude.ai%2Fcb"))
            ai.allow()
            assertEquals(
                """{"query":"response_type=code&client_id=hai_1&redirect_uri=https%3A%2F%2Fclaude.ai%2Fcb&state=s&scope=mcp"}""",
                server.request("POST", "/api/v1/auth/ai-connect").body,
            )
            assertEquals("https://claude.ai/cb?code=1&state=s", ai.state.value.redirect)

            val bad = AiConnectPresenter(server.api(), { account() }, gate(), "https://x/nothing")
            assertFalse(bad.state.value.valid)
        }

    @Test
    fun memberCannotAllowAi() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/ai-connect", """{"client_name":"Claude","enabled":true}""")
            val ai = AiConnectPresenter(server.api(), { account(role = "member", perms = listOf("ask")) }, gate(), "x?response_type=code")
            ai.load()
            assertFalse(ai.state.value.mayAllow)
            ai.allow()
            assertTrue(server.requests.none { it.method == "POST" })
        }
}
