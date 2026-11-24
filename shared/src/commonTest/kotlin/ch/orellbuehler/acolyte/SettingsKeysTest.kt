package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.SettingsKeysAddresses
import ch.orellbuehler.acolyte.api.model.SettingsKeysPushed
import ch.orellbuehler.acolyte.api.model.SettingsKeysPushedFormat
import ch.orellbuehler.acolyte.api.model.SettingsKeysPushedProfile
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsAiPresenter
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsApiPresenter
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsKeysLogic
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsKeysNoticeKind
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
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

class SettingsKeysTest {
    private val reauthJson = """{"error":"confirm your local password again before using this route","code":"reauth"}"""

    private val keysJson =
        """{"keys":[{"id":1,"label":"Home Assistant","hint":"…ab12","created_at":"2026-11-01T10:00:00.000000000Z",
            "last_used_at":"2026-11-20T08:00:00.000000000Z","scopes":["all"]},
            {"id":2,"label":"Claude Desktop","hint":"…cd34","created_at":"2026-11-02T10:00:00.000000000Z","scopes":["mcp"],
            "mcp_groups":["plugins","add","see"]}]}"""

    private val oldAppJson =
        """{"enabled":true,"delete_files":false,"sides":[
            {"side":"sonarr","path":"/holyarrs/sonarr","url":"https://media.example.com/holyarrs/sonarr",
            "key":{"side":"sonarr","hint":"…9f2c","owner":"ana","owner_active":false,"created_at":"2026-11-01T10:00:00.000000000Z",
            "last_used_at":"2026-11-10T10:00:00.000000000Z"}},
            {"side":"radarr","path":"/holyarrs/radarr","url":"https://media.example.com/holyarrs/radarr"}],
            "versions":{"sonarr":"4.0.0","radarr":"5.0.0"},
            "pushed":{"formats":[{"side":"sonarr","name":"HDR","trait_id":3,"updated_at":"2026-11-02T00:00:00.000000000Z"}],
            "profiles":[{"side":"radarr","name":"UHD","pick_rules_id":7,"notes":["a"],"updated_at":"2026-11-03T00:00:00.000000000Z"}]}}"""

    private fun owner(): Account = testAccount(Role.OWNER, emptyList())

    private fun api(
        server: FakeServer,
        account: Account = owner(),
        reauth: suspend (String, String?) -> ApiError? = { _, _ -> null },
    ) = SettingsApiPresenter(server.api(), { account }, testGate(account, reauth))

    private fun ai(
        server: FakeServer,
        account: Account = owner(),
        reauth: suspend (String, String?) -> ApiError? = { _, _ -> null },
    ) = SettingsAiPresenter(server.api(), { account }, testGate(account, reauth))

    private fun apiServer(): FakeServer =
        FakeServer().apply {
            json("GET", "/api/v1/auth/keys", keysJson)
            json("GET", "/api/v1/settings/old-app-api", oldAppJson)
        }

    private fun aiServer(enabled: Boolean = false): FakeServer =
        FakeServer().apply {
            json("GET", "/api/v1/settings/ai", """{"enabled":$enabled,"path":"/api/v1/mcp","versions":["2025-11-25"]}""")
            json("GET", "/api/v1/auth/keys", keysJson)
        }

    @Test
    fun apiPageLoadsKeysAndTheSeerrCard() =
        runTest {
            val presenter = api(apiServer())
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.allowed)
            assertEquals("https://media.example.com/holyarrs/api/v1", s.baseAddress)
            assertEquals("https://media.example.com/holyarrs/api/v1/openapi.json", s.openApiAddress)
            assertEquals(listOf("Home Assistant", "Claude Desktop"), s.keys.map { it.label })
            assertFalse(s.keysNeedPassword)
            assertFalse(s.seerrGone)
            val card = assertNotNull(s.seerr)
            assertTrue(card.enabled)
            assertFalse(card.deleteFiles)
            assertEquals(listOf("sonarr", "radarr"), card.sides.map { it.side })
            val sonarr = card.sides[0]
            assertTrue(sonarr.hasKey)
            assertEquals("9f2c", sonarr.hint)
            assertEquals("ana", sonarr.owner)
            assertFalse(sonarr.ownerActive)
            assertEquals("/holyarrs/sonarr", sonarr.path)
            assertFalse(card.sides[1].hasKey)
            assertEquals(listOf("UHD", "HDR"), card.sent.map { it.name })
            assertEquals(listOf("profile", "format"), card.sent.map { it.kind })
        }

    @Test
    fun apiPageNeedsTheSettingsPermission() =
        runTest {
            val server = apiServer()
            val presenter = api(server, testAccount(Role.MEMBER, listOf("ask")))
            presenter.load()
            assertFalse(presenter.state.value.allowed)
            assertEquals(Phase.READY, presenter.state.value.status.phase)
            assertTrue(server.requests.isEmpty())
        }

    @Test
    fun seerrCardIsHiddenWhenTheServerHasNoOldAppApi() =
        runTest {
            val server = apiServer()
            server.json("GET", "/api/v1/settings/old-app-api", """{"error":"not found"}""", 404)
            val presenter = api(server)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.seerrGone)
            assertNull(s.seerr)
            assertNull(s.seerrError)
            assertEquals(2, s.keys.size)
        }

    @Test
    fun keysAnswering403AskForThePasswordInTheNote() =
        runTest {
            val server = apiServer()
            server.json("GET", "/api/v1/auth/keys", """{"error":"sign in in the browser first"}""", 403)
            val presenter = api(server)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.keysNeedPassword)
            assertTrue(s.keys.isEmpty())
            assertNotNull(s.seerr)
        }

    @Test
    fun theWholePageFailsOnlyWhenBothCallsFail() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/auth/keys", """{"error":"boom"}""", 500)
            server.json("GET", "/api/v1/settings/old-app-api", """{"error":"boom"}""", 500)
            val presenter = api(server)
            presenter.load()
            val failed = presenter.state.value.status
            assertEquals(Phase.FAILED, failed.phase)
            assertEquals(ApiErrorCode.SERVER, failed.error?.code)
            server.json("GET", "/api/v1/settings/old-app-api", oldAppJson)
            server.json("GET", "/api/v1/auth/keys", """{"error":"boom"}""", 500)
            presenter.refresh()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals(ApiErrorCode.SERVER, s.keysError?.code)
            assertNotNull(s.seerr)
        }

    @Test
    fun makingAKeyShowsItOnceAndReloadsTheList() =
        runTest {
            val server = apiServer()
            var made = false
            server.on("GET", "/api/v1/auth/keys") {
                respondJson(
                    if (made) {
                        keysJson.replace(
                            """{"keys":[""",
                            """{"keys":[{"id":3,"label":"Pi","hint":"…ef56","scopes":["all"]},""",
                        )
                    } else {
                        keysJson
                    },
                )
            }
            server.on("POST", "/api/v1/auth/keys") {
                made = true
                respondJson("""{"id":3,"label":"Pi","scopes":["all"],"key":"hak_secret","note":"shown once"}""", 201)
            }
            val presenter = api(server)
            presenter.load()
            presenter.makeKey("   ")
            presenter.makeKey("x".repeat(65))
            assertTrue(server.requests.none { it.method == "POST" })
            presenter.makeKey("  Pi  ")
            val s = presenter.state.value
            assertEquals("""{"label":"Pi"}""", server.request("POST", "/api/v1/auth/keys").body)
            assertEquals("hak_secret", s.fresh?.key)
            assertEquals("Pi", s.fresh?.name)
            assertEquals(3L, s.fresh?.id)
            assertFalse(s.making)
            assertEquals("Pi", s.keys.first().label)
            presenter.dismissFresh()
            assertNull(presenter.state.value.fresh)
        }

    @Test
    fun makingAKeyAsksForThePasswordThenRetries() =
        runTest {
            val server = apiServer()
            var posts = 0
            server.on("POST", "/api/v1/auth/keys") {
                posts++
                if (posts == 1) {
                    respondJson(reauthJson, 403)
                } else {
                    respondJson("""{"id":3,"label":"Pi","scopes":["all"],"key":"hak_secret"}""", 201)
                }
            }
            val proofs = mutableListOf<Pair<String, String?>>()
            val presenter =
                api(server) { password, code ->
                    proofs += password to code
                    null
                }
            presenter.load()
            val job = async { presenter.makeKey("Pi") }
            presenter.reauth.state.first { it.visible }
            var s = presenter.state.value
            assertTrue(s.making)
            assertNull(s.fresh)
            presenter.reauth.confirm("secret", " ")
            job.await()
            s = presenter.state.value
            assertEquals(listOf<Pair<String, String?>>("secret" to null), proofs)
            assertEquals(2, posts)
            assertFalse(presenter.reauth.state.value.visible)
            assertFalse(s.making)
            assertEquals("hak_secret", s.fresh?.key)
        }

    @Test
    fun aWrongPasswordKeepsTheDialogAndCancelDropsTheAction() =
        runTest {
            val server = apiServer()
            server.json("DELETE", "/api/v1/auth/keys/1", reauthJson, 403)
            val wrong = ApiError(ApiErrorCode.UNAUTHORIZED, 401)
            val presenter = api(server) { _, _ -> wrong }
            presenter.load()
            val job = async { presenter.revokeKey(1) }
            presenter.reauth.state.first { it.visible }
            assertEquals(1L, presenter.state.value.revokingId)
            presenter.reauth.confirm("nope", "")
            assertTrue(presenter.reauth.state.value.visible)
            assertEquals(wrong, presenter.reauth.state.value.error)
            presenter.reauth.cancel()
            job.await()
            val s = presenter.state.value
            assertFalse(presenter.reauth.state.value.visible)
            assertEquals(0L, s.revokingId)
            assertEquals(2, s.keys.size)
            assertNull(s.notice)
        }

    @Test
    fun revokingAKeyRemovesItAndReportsFailures() =
        runTest {
            val server = apiServer()
            server.json("DELETE", "/api/v1/auth/keys/1", "", 204)
            server.json("DELETE", "/api/v1/auth/keys/2", """{"error":"no such key"}""", 404)
            val presenter = api(server)
            presenter.load()
            presenter.revokeKey(1)
            var s = presenter.state.value
            assertEquals(listOf(2L), s.keys.map { it.id })
            assertEquals(SettingsKeysNoticeKind.KEY_REVOKED, s.notice?.kind)
            assertEquals("Home Assistant", s.notice?.name)
            assertEquals(0L, s.revokingId)
            presenter.revokeKey(2)
            s = presenter.state.value
            assertEquals(listOf(2L), s.keys.map { it.id })
            assertEquals(SettingsKeysNoticeKind.KEY_REVOKE_FAILED, s.notice?.kind)
            assertEquals(ApiErrorCode.NOT_FOUND, s.notice?.error?.code)
            presenter.dismissNotice(s.notice!!.seq)
            assertNull(presenter.state.value.notice)
        }

    @Test
    fun seerrSwitchesFlipAtOnceAndRollBackOnFailure() =
        runTest {
            val server = apiServer()
            val presenter = api(server)
            var seenWhileSaving: Boolean? = null
            server.on("PUT", "/api/v1/settings/old-app-api") {
                val card = presenter.state.value.seerr
                seenWhileSaving = card?.enabled
                respondJson(oldAppJson.replace(""""enabled":true""", """"enabled":false"""))
            }
            presenter.load()
            presenter.setSeerrEnabled(false)
            assertEquals(false, seenWhileSaving)
            assertEquals("""{"enabled":false}""", server.request("PUT", "/api/v1/settings/old-app-api").body)
            val flipped = presenter.state.value
            assertEquals(false, flipped.seerr?.enabled)
            assertFalse(flipped.seerrSaving)
            server.json("PUT", "/api/v1/settings/old-app-api", """{"error":"nope"}""", 500)
            presenter.setSeerrDeleteFiles(true)
            val s = presenter.state.value
            assertEquals("""{"delete_files":true}""", server.request("PUT", "/api/v1/settings/old-app-api").body)
            assertEquals(false, s.seerr?.deleteFiles)
            assertEquals(SettingsKeysNoticeKind.SEERR_SAVE_FAILED, s.notice?.kind)
            assertFalse(s.seerrSaving)
        }

    @Test
    fun seerrKeysAreMadeShownOnceAndRemoved() =
        runTest {
            val server = apiServer()
            server.json(
                "POST",
                "/api/v1/settings/old-app-api/keys/radarr",
                """{"side":"radarr","key":"arr_secret","path":"/holyarrs/radarr"}""",
                201,
            )
            server.json("DELETE", "/api/v1/settings/old-app-api/keys/sonarr", "", 204)
            val presenter = api(server)
            presenter.load()
            presenter.makeSeerrKey("radarr")
            var s = presenter.state.value
            assertEquals("radarr", s.seerrFresh?.side)
            assertEquals("arr_secret", s.seerrFresh?.key)
            assertTrue(s.seerr!!.sides.none { it.busy })
            presenter.removeSeerrKey("sonarr")
            s = presenter.state.value
            assertEquals(SettingsKeysNoticeKind.SEERR_KEY_REMOVED, s.notice?.kind)
            assertEquals("sonarr", s.notice?.name)
            assertEquals("radarr", s.seerrFresh?.side)
            presenter.dismissSeerrFresh()
            assertNull(presenter.state.value.seerrFresh)
            presenter.makeSeerrKey("lidarr")
            assertEquals(1, server.requests.count { it.method == "POST" })
        }

    @Test
    fun seerrKeysAskForThePasswordAndShowTheServersReason() =
        runTest {
            val server = apiServer()
            var posts = 0
            server.on("POST", "/api/v1/settings/old-app-api/keys/sonarr") {
                posts++
                when (posts) {
                    1 -> respondJson(reauthJson, 403)
                    2 -> respondJson("""{"error":"the key acts as you, and your account may not manage the Library"}""", 403)
                    else -> respondJson("""{"side":"sonarr","key":"k","path":"/sonarr"}""", 201)
                }
            }
            val presenter = api(server)
            presenter.load()
            val job = async { presenter.makeSeerrKey("sonarr") }
            presenter.reauth.state.first { it.visible }
            val parked = presenter.state.value.seerr!!
            assertTrue(parked.sides[0].busy)
            presenter.reauth.confirm("pw", "")
            job.await()
            var s = presenter.state.value
            assertFalse(presenter.reauth.state.value.visible)
            assertTrue(s.seerr!!.sides.none { it.busy })
            assertEquals(SettingsKeysNoticeKind.SEERR_KEY_MAKE_FAILED, s.notice?.kind)
            assertEquals("the key acts as you, and your account may not manage the Library", s.notice?.detail)
            presenter.makeSeerrKey("sonarr")
            s = presenter.state.value
            assertEquals("k", s.seerrFresh?.key)
        }

    @Test
    fun aiPageForTheOwner() =
        runTest {
            val presenter = ai(aiServer(enabled = true))
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.available)
            assertTrue(s.enabled)
            assertTrue(s.isOwner)
            assertTrue(s.canManageKeys)
            assertTrue(s.canChange)
            assertFalse(s.changeNeedsSession)
            assertEquals("https://media.example.com/holyarrs/api/v1/mcp", s.address)
            assertEquals(listOf("Claude Desktop"), s.keys.map { it.label })
            assertEquals(listOf("see", "add", "plugins"), s.keys[0].groups)
            assertEquals(listOf("see", "add", "control", "settings", "delete", "admin", "approve"), s.offeredGroups)
            assertEquals(listOf("see", "add", "control", "settings"), s.defaultGroups)
        }

    @Test
    fun anAdminCanChangeKeysButNotTheSwitch() =
        runTest {
            val server = aiServer()
            val presenter = ai(server, testAccount(Role.ADMIN, listOf("settings")))
            presenter.load()
            assertFalse(presenter.state.value.isOwner)
            assertTrue(presenter.state.value.canChange)
            presenter.setEnabled(true)
            assertTrue(server.requests.none { it.method == "PUT" })
            assertFalse(presenter.state.value.enabled)
        }

    @Test
    fun aMemberWithoutSettingsSeesNoKeys() =
        runTest {
            val server = aiServer()
            val presenter = ai(server, testAccount(Role.MEMBER, listOf("ask")))
            presenter.load()
            val s = presenter.state.value
            assertFalse(s.canManageKeys)
            assertFalse(s.canChange)
            assertTrue(s.keys.isEmpty())
            assertTrue(server.requests.none { it.path == "/api/v1/auth/keys" })
        }

    @Test
    fun aKeySignedInAccountCannotChangeKeys() =
        runTest {
            val account = owner().copy(method = SignInMethod.API_KEY)
            val presenter = ai(aiServer(), account)
            presenter.load()
            assertTrue(presenter.state.value.changeNeedsSession)
        }

    @Test
    fun theAiPageIsUnavailableWhileHeldBackOrNotFound() =
        runTest {
            val held = FakeServer()
            val heldPresenter = ai(held, owner().copy(held = listOf("ai_apps")))
            heldPresenter.load()
            assertFalse(heldPresenter.state.value.available)
            assertEquals(Phase.READY, heldPresenter.state.value.status.phase)
            assertTrue(held.requests.isEmpty())
            val missing = FakeServer()
            missing.json("GET", "/api/v1/settings/ai", """{"error":"not found"}""", 404)
            missing.json("GET", "/api/v1/auth/keys", keysJson)
            val presenter = ai(missing)
            presenter.load()
            assertFalse(presenter.state.value.available)
            assertEquals(Phase.READY, presenter.state.value.status.phase)
        }

    @Test
    fun theAiSwitchFlipsAtOnceAndRollsBackOnFailure() =
        runTest {
            val server = aiServer()
            val presenter = ai(server)
            var seenWhileSaving: Boolean? = null
            server.on("PUT", "/api/v1/settings/ai") {
                seenWhileSaving = presenter.state.value.enabled
                respondJson("""{"enabled":true,"path":"/api/v1/mcp","versions":[]}""")
            }
            presenter.load()
            presenter.setEnabled(true)
            assertEquals(true, seenWhileSaving)
            assertEquals("""{"enabled":true}""", server.request("PUT", "/api/v1/settings/ai").body)
            val on = presenter.state.value
            assertTrue(on.enabled)
            assertEquals(SettingsKeysNoticeKind.AI_ON, on.notice?.kind)
            server.json("PUT", "/api/v1/settings/ai", """{"error":"nope"}""", 500)
            presenter.setEnabled(false)
            val s = presenter.state.value
            assertTrue(s.enabled)
            assertFalse(s.saving)
            assertEquals(SettingsKeysNoticeKind.AI_SAVE_FAILED, s.notice?.kind)
        }

    @Test
    fun anAiKeyIsMadeWithTheTickedGroupsInScreenOrder() =
        runTest {
            val server = aiServer()
            server.json(
                "POST",
                "/api/v1/auth/keys",
                """{"id":9,"label":"Cursor","scopes":["mcp"],"key":"hak_ai","mcp_groups":["see","add","admin"]}""",
                201,
            )
            val presenter = ai(server)
            presenter.load()
            presenter.makeKey("  ", listOf("see"))
            assertTrue(server.requests.none { it.method == "POST" })
            presenter.makeKey(" Cursor ", listOf("admin", "plugins", "bogus", "see", "add"))
            assertEquals(
                """{"label":"Cursor","scopes":["mcp"],"mcp_groups":["see","add","admin"]}""",
                server.request("POST", "/api/v1/auth/keys").body,
            )
            val s = presenter.state.value
            assertEquals("hak_ai", s.fresh?.key)
            assertEquals(9L, s.fresh?.id)
            assertFalse(s.making)
            presenter.dismissFresh()
            assertNull(presenter.state.value.fresh)
        }

    @Test
    fun changingAKeyKeepsItsPluginsBoxAndCountsTheSave() =
        runTest {
            val server = aiServer()
            server.json("PUT", "/api/v1/auth/keys/2", """{"id":2,"label":"Claude Desktop","mcp_groups":["see","control","plugins"]}""")
            val presenter = ai(server)
            presenter.load()
            presenter.changeGroups(2, listOf("control", "see"))
            assertEquals("""{"mcp_groups":["see","control","plugins"]}""", server.request("PUT", "/api/v1/auth/keys/2").body)
            val s = presenter.state.value
            assertEquals(listOf("see", "control", "plugins"), s.keys[0].groups)
            assertEquals(1, s.changedCount)
            assertFalse(s.changing)
            assertNull(s.changeError)
            assertEquals(SettingsKeysNoticeKind.GROUPS_SAVED, s.notice?.kind)
            assertEquals("Claude Desktop", s.notice?.name)
        }

    @Test
    fun changingAKeyShowsAForbiddenAnswerInline() =
        runTest {
            val server = aiServer()
            server.json("PUT", "/api/v1/auth/keys/2", """{"error":"only the owner and admins may change a key, signed in"}""", 403)
            val presenter = ai(server)
            presenter.load()
            presenter.changeGroups(2, listOf("see"))
            var s = presenter.state.value
            assertEquals(ApiErrorCode.FORBIDDEN, s.changeError?.code)
            assertEquals("only the owner and admins may change a key, signed in", s.changeDetail)
            assertEquals(0, s.changedCount)
            assertFalse(s.changing)
            assertEquals(listOf("see", "add", "plugins"), s.keys[0].groups)
            presenter.clearChangeError()
            s = presenter.state.value
            assertNull(s.changeError)
            presenter.changeGroups(99, listOf("see"))
            assertEquals(1, server.requests.count { it.method == "PUT" })
        }

    @Test
    fun aMemberCannotChangeKeys() =
        runTest {
            val server = aiServer()
            val presenter = ai(server, testAccount(Role.MEMBER, listOf("settings")))
            presenter.load()
            presenter.changeGroups(2, listOf("see"))
            assertTrue(server.requests.none { it.method == "PUT" })
        }

    @Test
    fun revokingAnAiKeyGoesThroughThePasswordPrompt() =
        runTest {
            val server = aiServer()
            var deletes = 0
            server.on("DELETE", "/api/v1/auth/keys/2") {
                deletes++
                if (deletes == 1) respondJson(reauthJson, 403) else respondJson("", 204)
            }
            val presenter = ai(server)
            presenter.load()
            val job = async { presenter.revokeKey(2) }
            presenter.reauth.state.first { it.visible }
            assertEquals(2L, presenter.state.value.revokingId)
            presenter.reauth.confirm("pw", "123456")
            job.await()
            val s = presenter.state.value
            assertTrue(s.keys.isEmpty())
            assertFalse(presenter.reauth.state.value.visible)
            assertEquals(SettingsKeysNoticeKind.KEY_REVOKED, s.notice?.kind)
            assertEquals("Claude Desktop", s.notice?.name)
        }

    @Test
    fun theLogicHelpersFollowTheWebClient() {
        assertEquals("9f2c", SettingsKeysLogic.keyHint("…9f2c"))
        assertEquals("9f2c", SettingsKeysLogic.keyHint("..9f2c"))
        assertEquals("9f2c", SettingsKeysLogic.keyHint("9f2c"))
        assertEquals("Pi", SettingsKeysLogic.cleanLabel("  Pi "))
        assertNull(SettingsKeysLogic.cleanLabel(" "))
        assertNull(SettingsKeysLogic.cleanLabel("x".repeat(65)))
        assertEquals(64, SettingsKeysLogic.cleanLabel("x".repeat(64))?.length)
        assertEquals(listOf("see", "admin"), SettingsKeysLogic.orderedGroups(setOf("admin", "see", "plugins", "x")))
        assertEquals(listOf("see", "plugins"), SettingsKeysLogic.orderedGroups(setOf("see"), keepPlugins = true))
        assertEquals(listOf("add", "plugins"), SettingsKeysLogic.knownGroups(listOf("plugins", "nope", "add")))
        assertEquals("said no", SettingsKeysLogic.detail(ApiError(ApiErrorCode.CONFLICT, 409, "said no")))
        assertEquals("said no", SettingsKeysLogic.detail(ApiError(ApiErrorCode.FORBIDDEN, 403, "said no")))
        assertEquals("", SettingsKeysLogic.detail(ApiError(ApiErrorCode.SERVER, 500, "internal detail")))
        assertEquals("", SettingsKeysLogic.detail(ApiError(ApiErrorCode.NOT_FOUND, 404, "gone")))
    }

    @Test
    fun sentByOtherAppsSortNewestFirstThenByName() {
        val pushed =
            SettingsKeysPushed(
                formats =
                    listOf(
                        SettingsKeysPushedFormat(side = "sonarr", name = "b", updatedAt = "2026-11-02T00:00:00.000000000Z"),
                        SettingsKeysPushedFormat(side = "radarr", name = "A", updatedAt = "2026-11-02T00:00:00.000000000Z"),
                        SettingsKeysPushedFormat(side = "radarr", name = "old", updatedAt = "2026-10-01T00:00:00.000000000Z"),
                    ),
                profiles =
                    listOf(
                        SettingsKeysPushedProfile(
                            side = "radarr",
                            name = "UHD",
                            pickRulesId = 7,
                            updatedAt = "2026-11-05T00:00:00.000000000Z",
                        ),
                    ),
            )
        val rows = SettingsKeysLogic.pushedRows(pushed)
        assertEquals(listOf("UHD", "A", "b", "old"), rows.map { it.name })
        assertEquals(listOf("p-7", "f-radarr-A", "f-sonarr-b", "f-radarr-old"), rows.map { it.key })
        assertTrue(SettingsKeysLogic.pushedRows(SettingsKeysPushed()).isEmpty())
    }

    @Test
    fun addressesGoUnderTheUrlBase() {
        assertEquals("https://media.example.com/holyarrs/api/v1", SettingsKeysAddresses.api(BASE))
        assertEquals("https://media.example.com/holyarrs/api/v1", SettingsKeysAddresses.api("$BASE/"))
        assertEquals("https://media.example.com/holyarrs/api/v1/mcp", SettingsKeysAddresses.mcp(BASE))
        assertEquals("https://media.example.com/holyarrs/settings/remote-access", SettingsKeysAddresses.remoteAccess(BASE))
        assertEquals("https://host:6262/settings/remote-access", SettingsKeysAddresses.remoteAccess("https://host:6262"))
    }
}
