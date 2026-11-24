package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.Credentials
import ch.orellbuehler.acolyte.api.SessionCookies
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.feature.sources.SettingsBridgeDraft
import ch.orellbuehler.acolyte.feature.sources.SettingsBridgePresenter
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCause
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCheckPresenter
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCheckWord
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceHealthPresenter
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceOp
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceWord
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesNoticeKind
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesPresenter
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSourcesPresenterTest {
    private val owner = testAccount(Role.OWNER, emptyList())

    private fun source(
        id: Int,
        name: String,
        state: String = "enabled",
        health: String = """{"state":"working","text":"All good."}""",
        extra: String = "",
    ) = """{"id":$id,"name":"$name","protocol":"torrent","format":"torznab","url":"https://j.example/api","api_key":"••••••••",
        "use_for":{"feed":true,"auto_search":true,"hand_search":false},"kinds":["movie","tv"],"categories":null,"min_seeders":1,
        "limits":{"searches_per_window":0,"window_seconds":3600,"gap_ms":0,"timeout_ms":30000},"priority":25,"state":"$state",
        "health":$health,"allow_local_network":false,"proxy":"","proxy_password":"","proxy_allow_local_network":false,
        "summary":{"last_success":"2026-01-01T10:00:00Z","last_failure":null,"calls_24h":4,"failures_24h":1,"avg_ms_24h":840},
        "tags":[],"multi_languages":[],"no_language_languages":[],"downloader_id":0$extra}"""

    private fun listJson(vararg rows: String) = """{"sources":[${rows.joinToString(",")}]}"""

    private fun FakeServer.sourcesRoutes(vararg rows: String) {
        json("GET", "/api/v1/sources", listJson(*rows))
        json(
            "GET",
            "/api/v1/downloaders",
            """{"downloaders":[{"id":1,"name":"qBit","protocol":"torrent"},{"id":2,"name":"SAB","protocol":"usenet"}]}""",
        )
    }

    @Test
    fun listsRowsWithStatesAndNotes() =
        runTest {
            val server = FakeServer()
            server.sourcesRoutes(
                source(1, "Knaben"),
                source(
                    2,
                    "Down one",
                    state = "paused",
                    health = """{"state":"unreachable","text":"It did not answer.","until":"2026-01-01T12:00:00Z","reason":"down"}""",
                ),
                source(3, "Mine", state = "disabled", health = """{"state":"disabled","text":"Paused by you."}"""),
                source(
                    4,
                    "Blocked",
                    health = """{"state":"unreachable","text":"Blocked by the site's bot check.","block":"bot_check","cause":"refused"}""",
                ),
            )
            val presenter = SettingsSourcesPresenter(server.api()) { owner }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.canEdit)
            assertFalse(s.partOff)
            assertEquals(2, s.downloaders?.size)
            val (working, paused, byMe, blocked) = s.rows
            assertEquals(SettingsSourceWord.WORKING, working.word)
            assertFalse(working.showDot)
            assertFalse(working.showNote)
            assertTrue(working.showAvg)
            assertEquals(SettingsSourceWord.DOWN, paused.word)
            assertEquals(Tone.FAIL, paused.tone)
            assertTrue(paused.canTryNow)
            assertTrue(paused.showUntil)
            assertFalse(paused.showNote)
            assertEquals(SettingsSourceWord.PAUSED, byMe.word)
            assertTrue(byMe.isDisabled)
            assertFalse(byMe.canTryNow)
            assertTrue(blocked.showNote)
            assertTrue(blocked.noteBad)
            assertTrue(blocked.botCheckHint)
            assertEquals(SettingsSourceCause.REFUSED, blocked.cause)
            assertEquals("Torznab", working.formatLabel)
        }

    @Test
    fun partOffAndPermissions() =
        runTest {
            val server = FakeServer()
            server.sourcesRoutes(source(1, "Knaben"))
            val off = owner.copy(parts = owner.parts + PartView("sources", "Sources", false))
            val presenter = SettingsSourcesPresenter(server.api()) { off }
            presenter.load()
            assertTrue(presenter.state.value.partOff)
            assertFalse(presenter.state.value.showBridgeAndCheck)
            val denied = SettingsSourcesPresenter(server.api()) { testAccount(Role.ADMIN, listOf("manage_library")) }
            denied.load()
            assertFalse(denied.state.value.allowed)
            assertTrue(
                denied.state.value.rows
                    .isEmpty(),
            )
        }

    @Test
    fun loadErrorKeepsRetry() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/sources", """{"error":"boom"}""", 500)
            val presenter = SettingsSourcesPresenter(server.api()) { owner }
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            server.sourcesRoutes(source(1, "Knaben"))
            presenter.refresh()
            assertEquals(Phase.READY, presenter.state.value.status.phase)
            assertEquals(1, presenter.state.value.rows.size)
        }

    @Test
    fun addSavesThenTests() =
        runTest {
            val server = FakeServer()
            server.sourcesRoutes(source(7, "New"))
            server.json("POST", "/api/v1/sources", source(7, "New"), 201)
            server.json(
                "POST",
                "/api/v1/sources/7/test",
                """{"ok":true,"ms":120,"text":"The Source answered.","health":{"state":"working","text":""},"items":12}""",
            )
            val presenter = SettingsSourcesPresenter(server.api()) { owner }
            presenter.load()
            presenter.startAdd()
            val form = presenter.state.value.editing
            assertNotNull(form)
            assertFalse(presenter.save(form.copy(name = "", url = "")))
            assertTrue(presenter.save(form.copy(name = "New", url = "https://j.example/api", apiKey = "k")))
            val body = server.request("POST", "/api/v1/sources").body.orEmpty()
            assertTrue(body.contains(""""name":"New""""))
            assertTrue(body.contains(""""api_key":"k""""))
            assertTrue(body.contains(""""window_seconds":3600"""))
            assertFalse(body.contains("site"))
            val s = presenter.state.value
            assertNull(s.editing)
            assertEquals(SettingsSourcesNoticeKind.SAVED, s.notice?.kind)
            assertEquals("New", s.notice?.name)
            val test = s.rows.single().test
            assertNotNull(test)
            assertTrue(test.ok)
            assertEquals(12, test.items)
            assertEquals(120, test.ms)
        }

    @Test
    fun changeSendsAPutAndKeepsServerTextOnFailure() =
        runTest {
            val server = FakeServer()
            server.sourcesRoutes(source(3, "Old"))
            server.json("PUT", "/api/v1/sources/3", """{"error":"another Source has that name"}""", 409)
            val presenter = SettingsSourcesPresenter(server.api()) { owner }
            presenter.load()
            presenter.startChange(3)
            val form = presenter.state.value.editing
            assertNotNull(form)
            assertEquals(3, form.id)
            assertEquals("Old", form.name)
            assertFalse(presenter.save(form.copy(name = "Taken")))
            val s = presenter.state.value
            assertNotNull(s.editing)
            assertEquals(ApiErrorCode.CONFLICT, s.saveError?.code)
            assertEquals("another Source has that name", s.saveError?.message)
            assertFalse(s.saving)
            presenter.cancelEdit()
            assertNull(presenter.state.value.editing)
        }

    @Test
    fun failedTestShowsItsTextAndTheBotCheckHint() =
        runTest {
            val server = FakeServer()
            server.sourcesRoutes(source(1, "Site"))
            server.json(
                "POST",
                "/api/v1/sources/1/test",
                """{"ok":false,"ms":900,"text":"Blocked by the site's bot check.","cause":"auth","health":{"state":"unreachable","text":"x"}}""",
            )
            val presenter = SettingsSourcesPresenter(server.api()) { owner }
            presenter.load()
            presenter.test(1)
            val row =
                presenter.state.value.rows
                    .single()
            assertFalse(row.test?.ok ?: true)
            assertTrue(row.botCheckHint)
            assertEquals(SettingsSourceCause.AUTH, row.test?.cause)
            assertFalse(row.testing)
        }

    @Test
    fun pauseResumeTryAndRemove() =
        runTest {
            val server = FakeServer()
            server.sourcesRoutes(source(1, "A"), source(2, "B"))
            server.json("POST", "/api/v1/sources/1/pause", source(1, "A", state = "disabled"))
            server.json("POST", "/api/v1/sources/1/resume", source(1, "A"))
            server.json(
                "POST",
                "/api/v1/sources/2/try",
                """{"ok":true,"ms":5,"text":"The Source answered.","health":{"state":"working"}}""",
            )
            server.on("DELETE", "/api/v1/sources/2") { respondJson("", 204) }
            val presenter = SettingsSourcesPresenter(server.api()) { owner }
            presenter.load()
            presenter.setPaused(1, true)
            assertEquals(1, server.requests.count { it.method == "POST" && it.path == "/api/v1/sources/1/pause" })
            presenter.setPaused(1, false)
            assertEquals(1, server.requests.count { it.method == "POST" && it.path == "/api/v1/sources/1/resume" })
            presenter.tryNow(2)
            assertTrue(
                presenter.state.value.rows
                    .first { it.id == 2L }
                    .test
                    ?.ok == true,
            )
            server.sourcesRoutes(source(1, "A"))
            presenter.remove(2)
            assertEquals(
                listOf(1L),
                presenter.state.value.rows
                    .map { it.id },
            )
            server.json("POST", "/api/v1/sources/1/pause", """{"error":"nope"}""", 500)
            presenter.setPaused(1, true)
            assertEquals(
                SettingsSourcesNoticeKind.PAUSE_FAILED,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun liveSourceEventsRefreshTheRows() =
        runTest {
            val server = FakeServer()
            server.sourcesRoutes(source(1, "A"))
            val live = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 4)
            val client =
                ApiClient(BASE, Credentials(null, SessionCookies(listOf(FakeServer.SESSION)), null), server.engine(), language = { "en" })
            val presenter = SettingsSourcesPresenter(Api({ client }, live)) { owner }
            presenter.load()
            val before = server.requests.count { it.path == "/api/v1/sources" }
            val job = launch { presenter.watch() }
            advanceUntilIdle()
            live.emit(LiveEvent("source", buildJsonObject { put("id", 1) }, "1"))
            live.emit(LiveEvent("other", JsonNull, "2"))
            advanceUntilIdle()
            assertEquals(before + 1, server.requests.count { it.path == "/api/v1/sources" })
            job.cancel()
        }

    @Test
    fun testSearchSummarisesPerSource() =
        runTest {
            val server = FakeServer()
            server.json(
                "POST",
                "/api/v1/sources/check",
                """{"query":"The Matrix","kind":"movie","sources":[
                {"id":1,"name":"A","answered":true,"paused":false,"count":10,"ms":300,"health":{"state":"working"}},
                {"id":2,"name":"B","answered":false,"paused":false,"count":0,"ms":20000,"error":"timed out","health":{"state":"unreachable"}},
                {"id":3,"name":"C","answered":false,"paused":true,"count":0,"ms":0,"health":{"state":"unreachable","text":"Waiting."}}]}""",
            )
            val presenter = SettingsSourceCheckPresenter(server.api())
            presenter.run("movie", "  ")
            val result = presenter.state.value.result
            assertNotNull(result)
            assertEquals(3, result.total)
            assertEquals(1, result.connected)
            assertEquals(1, result.failed)
            assertEquals(1, result.paused)
            assertEquals(
                listOf(SettingsSourceCheckWord.CONNECTED, SettingsSourceCheckWord.NOT_CONNECTED, SettingsSourceCheckWord.PAUSED),
                result.lines.map { it.word },
            )
            assertEquals("Waiting.", result.lines[2].detail)
            assertEquals("timed out", result.lines[1].detail)
            assertTrue(result.lines[1].link)
            assertTrue(
                server
                    .request("POST", "/api/v1/sources/check")
                    .body
                    .orEmpty()
                    .contains(""""kind":"movie""""),
            )
        }

    @Test
    fun healthShowsStatsTotalsAndTriesPausedSources() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/sources/9/stats",
                """{"source":${source(
                    9,
                    "Slow",
                    state = "paused",
                    health = """{"state":"unreachable","text":"Slow.","reason":"slowed"}""",
                )},
                "days":[{"day":"2026-01-01","calls":4,"failures":1,"grabs":2,"avg_ms":300},{"day":"2026-01-02","calls":6,"failures":0,"grabs":0,"avg_ms":500}],
                "recent":[{"at":"2026-01-02T10:00:00Z","op":"search","ok":false,"ms":900,"error_kind":"rate_limited"}]}""",
            )
            server.json(
                "POST",
                "/api/v1/sources/9/try",
                """{"ok":true,"ms":5,"text":"The Source answered.","health":{"state":"working"}}""",
            )
            val presenter = SettingsSourceHealthPresenter(server.api(), { owner }, 9)
            presenter.load()
            var s = presenter.state.value
            assertEquals(SettingsSourceWord.SLOWED, s.row?.word)
            assertEquals(10, s.totalCalls)
            assertEquals(1, s.totalFailures)
            assertEquals(2, s.totalGrabs)
            assertEquals(2, s.bars.size)
            assertEquals(SettingsSourceOp.SEARCH, s.recent.single().op)
            assertEquals("rate limited", s.recent.single().rawError)
            assertEquals("days=14", server.request("GET", "/api/v1/sources/9/stats").query)
            presenter.setDays(30)
            assertEquals("days=30", server.request("GET", "/api/v1/sources/9/stats").query)
            presenter.setDays(5)
            assertEquals(30, presenter.state.value.days)
            presenter.tryNow()
            s = presenter.state.value
            assertEquals(true, s.row?.test?.ok)
            assertTrue(server.requests.any { it.path == "/api/v1/sources/9/try" })
        }

    private fun bridgeJson(
        id: Int,
        kind: String,
        extra: String = "",
    ) = """{"id":$id,"kind":"$kind","url":"http://j:9117","sites":3,"sources":3$extra}"""

    private fun bridgePresenter(
        server: FakeServer,
        account: Account = owner,
        reauth: suspend (String, String?) -> ch.orellbuehler.acolyte.api.ApiError? = { _, _ -> null },
    ) = SettingsBridgePresenter(server.api(), { account }, ReauthGate(reauth, { account }))

    @Test
    fun bridgeTestAndLinkJackett() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/bridges", """{"bridges":[],"home_ip":"192.168.1.9"}""")
            server.json("POST", "/api/v1/bridges/test", """{"ok":true,"text":"Jackett answered with 2 sites.","sites":["A","B"]}""")
            server.json("POST", "/api/v1/bridges", bridgeJson(5, "jackett"), 201)
            val presenter = bridgePresenter(server)
            presenter.load()
            assertEquals("192.168.1.9", presenter.state.value.homeIp)
            presenter.startConnect("jackett")
            val draft = SettingsBridgeDraft(kind = "jackett", url = " http://j:9117 ", key = " k ")
            presenter.test(draft)
            val result =
                presenter.state.value.form
                    ?.result
            assertNotNull(result)
            assertEquals(2, result.siteCount)
            assertEquals(listOf("A", "B"), result.sites)
            server.json("GET", "/api/v1/bridges", """{"bridges":[${bridgeJson(5, "jackett")}]}""")
            assertTrue(presenter.link(draft))
            val s = presenter.state.value
            assertNull(s.form)
            assertEquals(1, s.bridges.size)
            assertEquals(1, s.changed)
            assertEquals(SettingsSourcesNoticeKind.BRIDGE_CONNECTED, s.notice?.kind)
            assertEquals("Jackett", s.notice?.name)
            assertEquals(3, s.notice?.count)
            val body = server.request("POST", "/api/v1/bridges").body.orEmpty()
            assertTrue(body.contains(""""url":"http://j:9117""""))
            assertTrue(body.contains(""""key":"k""""))
            assertTrue(body.contains(""""callback_url":"""""))
        }

    @Test
    fun prowlarrAsksForExistingKeysThenReauthAndRetries() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/bridges", """{"bridges":[]}""")
            var asked = 0
            server.on("POST", "/api/v1/bridges") {
                asked++
                when (asked) {
                    1 -> respondJson("""{"error":"HolyArrs already has an arr key","need":"keys","sides":["sonarr","radarr"]}""", 409)
                    2 -> respondJson("""{"error":"confirm your local password again","code":"reauth"}""", 403)
                    else -> respondJson(bridgeJson(8, "prowlarr", ""","keys":{"sonarr":"new-key"}"""), 201)
                }
            }
            var reauthed = ""
            val presenter = bridgePresenter(server) { password, _ -> null.also { reauthed = password } }
            presenter.load()
            presenter.startConnect("prowlarr")
            val draft = SettingsBridgeDraft(kind = "prowlarr", url = "http://p:9696", key = "k", callbackUrl = "http://holy:6262")
            assertFalse(presenter.link(draft))
            val form = presenter.state.value.form
            assertEquals(listOf("sonarr", "radarr"), form?.keysNeeded)
            assertTrue(form?.needsSonarrKey == true)
            assertEquals(ApiErrorCode.CONFLICT, form?.error?.code)
            val again = draft.copy(newKeys = true)
            val job = async { presenter.link(again) }
            presenter.reauth.state.first { it.visible }
            presenter.reauth.confirm("secret", "")
            assertTrue(job.await())
            assertEquals("secret", reauthed)
            val s = presenter.state.value
            assertNull(s.form)
            assertEquals(mapOf("sonarr" to "new-key"), s.madeKeys)
            assertEquals(3, asked)
            presenter.dismissKeys()
            assertTrue(
                presenter.state.value.madeKeys
                    .isEmpty(),
            )
            val sent =
                server.requests
                    .last { it.path == "/api/v1/bridges" && it.method == "POST" }
                    .body
                    .orEmpty()
            assertTrue(sent.contains(""""new_keys":true"""))
        }

    @Test
    fun bridgeFieldErrorsAndRateLimit() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/bridges", """{"bridges":[]}""")
            server.json("POST", "/api/v1/bridges/test", """{"error":"Enter a full address.","field":"url"}""", 400)
            val presenter = bridgePresenter(server)
            presenter.load()
            presenter.startConnect("jackett")
            val draft = SettingsBridgeDraft(url = "x", key = "k")
            presenter.test(draft)
            assertEquals(
                "Enter a full address.",
                presenter.state.value.form
                    ?.urlError,
            )
            server.json("POST", "/api/v1/bridges/test", """{"error":"Too many tries."}""", 429, "Retry-After" to "60")
            presenter.test(draft)
            val form = presenter.state.value.form
            assertEquals("", form?.urlError)
            assertEquals(ApiErrorCode.RATE_LIMITED, form?.error?.code)
        }

    @Test
    fun changingTheAddressRemovesTheOldConnection() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/bridges", """{"bridges":[${bridgeJson(5, "jackett")}]}""")
            server.json("POST", "/api/v1/bridges", bridgeJson(6, "jackett"), 201)
            server.json("DELETE", "/api/v1/bridges/5", """{"sources":3,"kept":false}""")
            val presenter = bridgePresenter(server)
            presenter.load()
            presenter.startChange(5)
            val initial =
                presenter.state.value.form
                    ?.initial
            assertNotNull(initial)
            assertEquals(5, initial.editingId)
            assertEquals("http://j:9117", initial.url)
            assertTrue(presenter.link(initial.copy(url = "http://other:9117", key = "k")))
            assertTrue(server.requests.any { it.method == "DELETE" && it.path == "/api/v1/bridges/5" })
        }

    @Test
    fun syncAndUnlink() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/bridges", """{"bridges":[${bridgeJson(5, "jackett")},${bridgeJson(6, "prowlarr")}]}""")
            server.json(
                "POST",
                "/api/v1/bridges/5/check",
                """{"result":{"sites":3,"added":2,"updated":1,"removed":0,"skipped":0},"bridge":${bridgeJson(5, "jackett")}}""",
            )
            server.json("POST", "/api/v1/bridges/6/check", """{"bridge":${bridgeJson(6, "prowlarr")}}""")
            server.json("DELETE", "/api/v1/bridges/5", """{"sources":3,"kept":true}""")
            val presenter = bridgePresenter(server)
            presenter.load()
            presenter.sync(5)
            var notice = presenter.state.value.notice
            assertEquals(SettingsSourcesNoticeKind.BRIDGE_SYNCED, notice?.kind)
            assertEquals(2, notice?.added)
            assertEquals(1, notice?.changed)
            presenter.sync(6)
            notice = presenter.state.value.notice
            assertEquals(SettingsSourcesNoticeKind.BRIDGE_SYNCING_AGAIN, notice?.kind)
            assertEquals(2, presenter.state.value.changed)
            assertTrue(presenter.unlink(5, keepSources = true))
            assertEquals("keep_sources=true", server.request("DELETE", "/api/v1/bridges/5").query)
            assertEquals(
                SettingsSourcesNoticeKind.BRIDGE_DISCONNECTED,
                presenter.state.value.notice
                    ?.kind,
            )
            server.json("DELETE", "/api/v1/bridges/6", """{"error":"Prowlarr did not answer."}""", 502)
            assertFalse(presenter.unlink(6, keepSources = false))
            assertEquals(
                502,
                presenter.state.value.removeError
                    ?.status,
            )
            assertEquals("", server.request("DELETE", "/api/v1/bridges/6").query)
        }
}
