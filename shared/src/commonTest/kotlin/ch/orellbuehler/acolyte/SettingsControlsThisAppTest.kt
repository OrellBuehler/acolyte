package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsSavedKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsThisAppPresenter
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsControlsThisAppTest {
    private val ui =
        """{"autoplay_trailers":true,"signin_library_art":false,"artwork_cache_gb":5,"history_routine_days":30,
            "history_other_days":0,"count_install":true}"""

    private val storage =
        """{"app":[{"name":"Config folder","path":"/config","free_bytes":1073741824},
            {"name":"Main database","path":"/config/holyarrs.db","bytes":12582912,"free_bytes":1073741824},
            {"name":"Log files","path":"/config/logs","free_bytes":-1}],
            "library_folders":[{"name":"Films","path":"/media/films","kind":"movie","free_bytes":5368709120}],
            "save_path":"/downloads"}"""

    private val proxy =
        """{"type":"http","host":"proxy.lan","port":3128,"username":"ana","password":"••••••••","bypass":"*.lan","bypass_local":true}"""

    private class Setup(
        val server: FakeServer,
        val presenter: SettingsControlsThisAppPresenter,
        val reauths: MutableList<Pair<String, String?>>,
    )

    private fun setup(account: Account = testAccount(Role.OWNER, emptyList())): Setup {
        val server = FakeServer()
        server.json("GET", "/api/v1/health", """{"ok":true,"version":"1.3.0","uptime_s":183600,"schema":42,"live_clients":2}""")
        server.json("GET", "/api/v1/settings/ui", ui)
        server.json("GET", "/api/v1/app/storage", storage)
        server.json("GET", "/api/v1/settings/url-base", """{"saved":"/holyarrs","in_use":"","restart_needed":true}""")
        server.json("GET", "/api/v1/settings/proxy", proxy)
        val client = server.client(listOf(FakeServer.SESSION), csrf = "token")
        val reauths = mutableListOf<Pair<String, String?>>()
        val presenter =
            SettingsControlsThisAppPresenter(
                Api({ client }, emptyFlow()),
                { account },
                testGate(account) { password, code ->
                    reauths += password to code
                    null
                },
            )
        return Setup(server, presenter, reauths)
    }

    private fun admin() = testAccount(Role.ADMIN, listOf("manage_library", "settings"))

    @Test
    fun theOwnerSeesEveryCard() =
        runTest {
            val s = setup()
            s.presenter.load()
            val state = s.presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertTrue(state.canSettings)
            assertTrue(state.isOwner)
            assertEquals("1.3.0", state.version)
            assertEquals("2d 3h", state.uptime)
            assertEquals(42, state.schema)
            assertEquals(2, state.liveClients)
            assertTrue(state.uiLoaded)
            assertEquals(30, state.historyRoutineDays)
            assertEquals(0, state.historyOtherDays)
            assertEquals(5, state.cacheGb)
            assertTrue(state.countInstall)
            val paths = assertNotNull(state.storage)
            assertEquals(listOf("Config folder", "Main database", "Log files"), paths.app.map { it.name })
            assertEquals("", paths.app[0].size)
            assertEquals("1.0 GB", paths.app[0].free)
            assertEquals("12 MB", paths.app[1].size)
            assertFalse(paths.app[2].freeKnown)
            assertEquals("5.0 GB", paths.folders[0].free)
            assertEquals("movie", paths.folders[0].kind)
            assertEquals("/downloads", paths.savePath)
            assertEquals("/holyarrs", state.urlBase?.saved)
            assertTrue(state.urlBase?.restartNeeded == true)
            assertEquals("/holyarrs", state.urlBaseDraft)
            assertTrue(state.proxyLoaded)
            assertEquals("http", state.proxy.type)
            assertEquals("3128", state.proxy.port)
            assertEquals("", state.proxy.password)
            assertTrue(state.proxyPasswordSaved)
            assertFalse(state.canSaveProxy)
            assertFalse(state.canSaveUrlBase)
        }

    @Test
    fun anAdminCannotChangeHistoryOrTheProxy() =
        runTest {
            val s = setup(admin())
            s.presenter.load()
            val state = s.presenter.state.value
            assertTrue(state.canSettings)
            assertFalse(state.isOwner)
            assertFalse(state.canChangeHistory)
            assertFalse(state.proxyLoaded)
            assertEquals(0, s.server.recorded().count { it.path == "/api/v1/settings/proxy" })
            s.presenter.setHistoryRoutine(7)
            assertEquals(0, s.server.recorded().count { it.method == "PUT" })
            s.presenter.setCacheGb(10)
            assertEquals(1, s.server.recorded().count { it.method == "PUT" })
        }

    @Test
    fun aHealthFailureOnTheFirstReadShowsAnError() =
        runTest {
            val s = setup()
            s.server.json("GET", "/api/v1/health", """{"error":"your account may not do this"}""", 403)
            s.presenter.load()
            assertEquals(Phase.FAILED, s.presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.FORBIDDEN,
                s.presenter.state.value.status.error
                    ?.code,
            )
        }

    @Test
    fun historyKeepTimesSaveOneFieldAtATime() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.on("PUT", "/api/v1/settings/ui") {
                respondJson(ui.replace("\"history_routine_days\":30", "\"history_routine_days\":7"))
            }
            s.presenter.setHistoryRoutine(7)
            val state = s.presenter.state.value
            assertEquals(7, state.historyRoutineDays)
            assertFalse(state.uiBusy)
            assertEquals(SettingsControlsSavedKind.HISTORY, state.saved?.kind)
            assertEquals("""{"history_routine_days":7}""", s.server.request("PUT", "/api/v1/settings/ui").body)
            assertEquals("token", s.server.request("PUT", "/api/v1/settings/ui").headers["X-CSRF-Token"])
        }

    @Test
    fun aRefusedChangeRollsTheControlBack() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("PUT", "/api/v1/settings/ui", """{"error":"only the owner may change how long History is kept"}""", 403)
            s.presenter.setHistoryOther(90)
            val state = s.presenter.state.value
            assertEquals(0, state.historyOtherDays)
            assertFalse(state.uiBusy)
            assertEquals(SettingsControlsSavedKind.FAILED, state.saved?.kind)
            assertEquals(ApiErrorCode.FORBIDDEN, state.saved?.error?.code)
            assertEquals(2, s.server.recorded().count { it.method == "GET" && it.path == "/api/v1/settings/ui" })
        }

    @Test
    fun pictureStorageAndInstallCountSave() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.server.json("PUT", "/api/v1/settings/ui", ui.replace("\"artwork_cache_gb\":5", "\"artwork_cache_gb\":10"))
            s.presenter.setCacheGb(10)
            assertEquals(10, s.presenter.state.value.cacheGb)
            assertEquals(
                SettingsControlsSavedKind.CACHE,
                s.presenter.state.value.saved
                    ?.kind,
            )
            assertEquals(
                10,
                s.presenter.state.value.saved
                    ?.value,
            )
            assertEquals("""{"artwork_cache_gb":10}""", s.server.request("PUT", "/api/v1/settings/ui").body)
            s.server.json("PUT", "/api/v1/settings/ui", ui.replace("\"count_install\":true", "\"count_install\":false"))
            s.presenter.setCountInstall(false)
            assertFalse(s.presenter.state.value.countInstall)
            assertEquals(
                SettingsControlsSavedKind.COUNT_OFF,
                s.presenter.state.value.saved
                    ?.kind,
            )
            assertEquals("""{"count_install":false}""", s.server.request("PUT", "/api/v1/settings/ui").body)
        }

    @Test
    fun pathsNeedAFreshPasswordAndLoadAfterIt() =
        runTest {
            val s = setup()
            var proven = false
            s.server.on("GET", "/api/v1/app/storage") {
                if (proven) respondJson(storage) else respondJson("""{"error":"confirm your password","code":"reauth"}""", 403)
            }
            s.presenter.load()
            var state = s.presenter.state.value
            assertNull(state.storage)
            assertTrue(state.storageProof)
            assertNull(state.storageError)
            assertFalse(s.presenter.reauth.state.value.visible)
            val job = async { s.presenter.proveStorage() }
            s.presenter.reauth.state
                .first { it.visible }
            proven = true
            s.presenter.reauth.confirm("secret", "")
            job.await()
            assertEquals<List<Pair<String, String?>>>(listOf("secret" to null), s.reauths)
            state = s.presenter.state.value
            assertFalse(s.presenter.reauth.state.value.visible)
            assertFalse(state.storageProof)
            assertEquals(3, state.storage?.app?.size)
        }

    @Test
    fun otherPathErrorsAreShownAndPollingSkipsAProofWait() =
        runTest {
            val s = setup()
            s.server.json("GET", "/api/v1/app/storage", """{"error":"boom"}""", 500)
            s.presenter.load()
            assertEquals(
                ApiErrorCode.SERVER,
                s.presenter.state.value.storageError
                    ?.code,
            )
            assertFalse(s.presenter.state.value.storageProof)
            s.server.json("GET", "/api/v1/app/storage", """{"error":"confirm","code":"reauth"}""", 403)
            s.presenter.poll()
            assertTrue(s.presenter.state.value.storageProof)
            val before = s.server.recorded().count { it.path == "/api/v1/app/storage" }
            s.presenter.poll()
            assertEquals(before, s.server.recorded().count { it.path == "/api/v1/app/storage" })
        }

    @Test
    fun pollingKeepsWhatIsBeingTyped() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.setUrlBase("/media")
            s.presenter.setProxyHost("other.lan")
            s.server.json("GET", "/api/v1/health", """{"ok":true,"version":"1.3.1","uptime_s":60,"schema":42}""")
            s.presenter.poll()
            val state = s.presenter.state.value
            assertEquals("1.3.1", state.version)
            assertEquals("1m", state.uptime)
            assertEquals("/media", state.urlBaseDraft)
            assertEquals("other.lan", state.proxy.host)
        }

    @Test
    fun theUrlBaseSavesTheTrimmedPathAndWarnsAboutARestart() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.setUrlBase("/holyarrs")
            assertFalse(s.presenter.state.value.canSaveUrlBase)
            s.presenter.setUrlBase("  /media  ")
            assertTrue(s.presenter.state.value.canSaveUrlBase)
            s.server.json("PUT", "/api/v1/settings/url-base", """{"saved":"/media","in_use":"/holyarrs","restart_needed":true}""")
            s.presenter.saveUrlBase()
            val state = s.presenter.state.value
            assertEquals("""{"url_base":"/media"}""", s.server.request("PUT", "/api/v1/settings/url-base").body)
            assertEquals("/media", state.urlBase?.saved)
            assertEquals("/media", state.urlBaseDraft)
            assertFalse(state.canSaveUrlBase)
            assertEquals(SettingsControlsSavedKind.URL_BASE, state.saved?.kind)
            assertTrue(state.saved?.restartNeeded == true)
        }

    @Test
    fun aBadUrlBaseKeepsTheDraftAndShowsTheError() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.setUrlBase("not a path")
            s.server.json("PUT", "/api/v1/settings/url-base", """{"error":"url_base must be a plain path"}""", 400)
            s.presenter.saveUrlBase()
            val state = s.presenter.state.value
            assertEquals(ApiErrorCode.BAD_REQUEST, state.urlBaseError?.code)
            assertEquals("not a path", state.urlBaseDraft)
            assertFalse(state.urlBaseSaving)
            s.presenter.setUrlBase("/ok")
            assertNull(s.presenter.state.value.urlBaseError)
        }

    @Test
    fun oldServersWithoutTheUrlBaseOrProxyHideThoseCards() =
        runTest {
            val s = setup()
            s.server.json("GET", "/api/v1/settings/url-base", """{"error":"not found"}""", 404)
            s.server.json("GET", "/api/v1/settings/proxy", """{"error":"not found"}""", 404)
            s.presenter.load()
            val state = s.presenter.state.value
            assertFalse(state.urlBaseAvailable)
            assertFalse(state.proxyAvailable)
            assertEquals(Phase.READY, state.status.phase)
        }

    @Test
    fun theProxyKeepsTheSavedPasswordWithTheMask() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.setProxyHost("proxy2.lan")
            s.presenter.setProxyPort("80x80")
            assertEquals("8080", s.presenter.state.value.proxy.port)
            assertTrue(s.presenter.state.value.canSaveProxy)
            s.server.json(
                "PUT",
                "/api/v1/settings/proxy",
                """{"type":"http","host":"proxy2.lan","port":8080,"username":"ana","password":"••••••••","bypass":"*.lan","bypass_local":true}""",
            )
            s.presenter.saveProxy()
            val body = assertNotNull(s.server.request("PUT", "/api/v1/settings/proxy").body)
            assertTrue(body.contains(""""password":"••••••••""""), body)
            assertTrue(body.contains(""""host":"proxy2.lan""""), body)
            assertTrue(body.contains(""""port":8080"""), body)
            val state = s.presenter.state.value
            assertEquals(SettingsControlsSavedKind.PROXY_ON, state.saved?.kind)
            assertFalse(state.canSaveProxy)
            assertEquals("", state.proxy.password)
            assertTrue(state.proxyPasswordSaved)
        }

    @Test
    fun aNewProxyPasswordReplacesTheOldOne() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.setProxyPassword("hunter2")
            assertTrue(s.presenter.state.value.canSaveProxy)
            s.server.json("PUT", "/api/v1/settings/proxy", proxy)
            s.presenter.saveProxy()
            val body = assertNotNull(s.server.request("PUT", "/api/v1/settings/proxy").body)
            assertTrue(body.contains(""""password":"hunter2""""), body)
        }

    @Test
    fun turningTheProxyOffClearsThePassword() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.setProxyType("")
            assertTrue(s.presenter.state.value.canSaveProxy)
            s.server.json(
                "PUT",
                "/api/v1/settings/proxy",
                """{"type":"","host":"","port":0,"username":"","password":"","bypass":"","bypass_local":true}""",
            )
            s.presenter.saveProxy()
            val body = assertNotNull(s.server.request("PUT", "/api/v1/settings/proxy").body)
            assertTrue(body.contains(""""type":"""""), body)
            assertTrue(body.contains(""""password":"""""), body)
            val state = s.presenter.state.value
            assertEquals(SettingsControlsSavedKind.PROXY_OFF, state.saved?.kind)
            assertFalse(state.proxyOn)
            assertEquals("8080", state.proxy.port)
            assertFalse(state.proxyPasswordSaved)
        }

    @Test
    fun theProxyNeedsAHostAndAPort() =
        runTest {
            val s = setup()
            s.server.json(
                "GET",
                "/api/v1/settings/proxy",
                """{"type":"","host":"","port":0,"username":"","password":"","bypass":"","bypass_local":true}""",
            )
            s.presenter.load()
            s.presenter.setProxyType("socks5")
            assertFalse(s.presenter.state.value.proxyValid)
            assertFalse(s.presenter.state.value.canSaveProxy)
            s.presenter.setProxyHost("  ")
            assertFalse(s.presenter.state.value.proxyValid)
            s.presenter.setProxyHost("proxy.lan")
            assertTrue(s.presenter.state.value.canSaveProxy)
            s.presenter.setProxyPort("0")
            assertFalse(s.presenter.state.value.proxyValid)
            s.presenter.setProxyPort("70000")
            assertFalse(s.presenter.state.value.proxyValid)
            s.presenter.setProxyPort("65535")
            assertTrue(s.presenter.state.value.proxyValid)
            s.server.json("PUT", "/api/v1/settings/proxy", """{"error":"host is not valid"}""", 400)
            s.presenter.saveProxy()
            assertEquals(
                ApiErrorCode.BAD_REQUEST,
                s.presenter.state.value.proxyError
                    ?.code,
            )
            assertFalse(s.presenter.state.value.proxySaving)
            val sent = (s.server.request("PUT", "/api/v1/settings/proxy").body).orEmpty()
            assertTrue(sent.contains(""""type":"socks5""""), sent)
            assertTrue(sent.contains(""""password":"""""), sent)
        }
}
