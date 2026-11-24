package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksField
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksNoticeKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksPresenter
import ch.orellbuehler.acolyte.feature.settings.status.SettingsHealthChecksRules
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val HC_CHECKS =
    """{"checks":[
        {"id":1,"key":"library_folder:1","kind":"library_folder","subject":"/mnt/media/movies/4k","state":"error","message":"Folder is missing",
         "since":"2026-11-22T10:00:00Z","checked_at":"2026-11-23T10:00:00Z","confirmed":"error"},
        {"id":2,"key":"backups","kind":"backups","subject":"Backups","state":"ok","message":"","since":"2026-11-22T10:00:00Z"},
        {"id":3,"key":"backups:key","kind":"backups","subject":"Backups","state":"warning","message":"Download your key kit","since":"2026-11-22T10:00:00Z"}
    ],"worst":"error","sync":[
        {"id":3,"name":"qBit","health":"degraded","seen_at":"2026-11-24T10:00:00Z"},
        {"id":4,"name":"Sab","health":"checking","stale":true}]}"""

private const val HC_SETTINGS =
    """{"every_minutes":5,"flap_guard":2,"low_space_mb":1024,"update_check_url":"","update_check_local":false,"update_prerelease":false,
        "update_check_every_days":1,"update_check_default":"https://api.github.com/repos/HolyArrs/HolyArrs/releases/latest"}"""

private const val HC_UPDATE = """{"on":true,"feed":"","newer":false,"version":"1.3.0"}"""

private fun FakeServer.healthChecksRoutes() {
    json("GET", "/api/v1/health/checks", HC_CHECKS)
    json("POST", "/api/v1/health/checks/run", HC_CHECKS)
    json("GET", "/api/v1/settings/health", HC_SETTINGS)
    json("GET", "/api/v1/app/update", HC_UPDATE)
}

private fun FakeServer.healthChecks(
    account: Account = downloadsAccount(),
    api: Api = api(client(listOf(FakeServer.SESSION), csrf = "token")),
) = SettingsHealthChecksPresenter(api) { account }

class SettingsHealthChecksTest {
    @Test
    fun loadsResultsAndSettings() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            val presenter = server.healthChecks()
            assertEquals(Phase.LOADING, presenter.state.value.status.phase)
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertTrue(state.canSeeChecks)
            assertTrue(state.canEdit)
            assertEquals(Tone.FAIL, state.worst)
            assertEquals(listOf("library_folder:1", "backups", "backups:key"), state.checks.map { it.key })
            val folder = state.checks[0]
            assertEquals(SettingsHealthChecksKind.LIBRARY_FOLDER, folder.kind)
            assertEquals("…/movies/4k", folder.subject)
            assertEquals("/mnt/media/movies/4k", folder.fullSubject)
            assertEquals("", folder.label)
            assertEquals("Folder is missing", folder.message)
            assertEquals(Tone.FAIL, folder.tone)
            assertEquals(SettingsHealthChecksKind.BACKUPS, state.checks[1].kind)
            assertEquals("", state.checks[1].subject)
            assertEquals(SettingsHealthChecksKind.MASTER_KEY, state.checks[2].kind)
            assertEquals(Tone.WAIT, state.checks[2].tone)
            assertEquals(listOf(true, false), state.sync.map { it.degraded })
            assertEquals(listOf("qBit", "Sab"), state.sync.map { it.name })
            assertEquals("1.3.0", state.version)
            assertTrue(state.hasSettings)
            assertEquals("5", state.everyText)
            assertEquals("2", state.guardText)
            assertEquals("1024", state.lowText)
            assertFalse(state.dirty)
            assertFalse(state.canSave)
            assertEquals(listOf(1, 3, 7, 14, 30), state.days)
        }

    @Test
    fun checksNeedManageLibraryAndSettingsNeedSettings() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            val manager = server.healthChecks(downloadsAccount(role = "user", perms = listOf("manage_library")))
            manager.load()
            assertTrue(manager.state.value.canSeeChecks)
            assertFalse(manager.state.value.canEdit)
            assertFalse(manager.state.value.hasSettings)
            assertTrue(server.recorded().none { it.path == "/api/v1/settings/health" })
            val admin = server.healthChecks(downloadsAccount(role = "user", perms = listOf("settings")))
            admin.load()
            assertFalse(admin.state.value.canSeeChecks)
            assertTrue(admin.state.value.canEdit)
            assertTrue(admin.state.value.hasSettings)
            assertEquals(1, server.recorded().count { it.path == "/api/v1/health/checks" })
            val nobody = server.healthChecks(downloadsAccount(role = "user", perms = listOf("ask")))
            nobody.load()
            assertFalse(nobody.state.value.allowed)
        }

    @Test
    fun editsValidateAndSaveOnlyWhatChanged() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            server.on(
                "PUT",
                "/api/v1/settings/health",
            ) { respondJson((it.body as TextContent).text.replace("}", ""","update_check_default":"d"}""")) }
            val presenter = server.healthChecks()
            presenter.load()
            presenter.setNumber(SettingsHealthChecksField.EVERY_MINUTES, "15x")
            presenter.setNumber(SettingsHealthChecksField.FLAP_GUARD, "3")
            presenter.setNumber(SettingsHealthChecksField.LOW_SPACE, "2048")
            presenter.setDays(7)
            presenter.setFeed("  https://example.com/releases  ")
            presenter.setLocal(true)
            presenter.setPrerelease(true)
            var state = presenter.state.value
            assertEquals("15", state.everyText)
            assertTrue(state.dirty)
            assertTrue(state.valid)
            assertTrue(state.canSave)
            presenter.setNumber(SettingsHealthChecksField.EVERY_MINUTES, "2000")
            assertFalse(presenter.state.value.everyValid)
            assertFalse(presenter.state.value.canSave)
            presenter.setNumber(SettingsHealthChecksField.EVERY_MINUTES, "")
            assertFalse(presenter.state.value.everyValid)
            presenter.setNumber(SettingsHealthChecksField.FLAP_GUARD, "21")
            assertFalse(presenter.state.value.guardValid)
            presenter.setNumber(SettingsHealthChecksField.FLAP_GUARD, "3")
            presenter.setNumber(SettingsHealthChecksField.EVERY_MINUTES, "15")
            presenter.save()
            state = presenter.state.value
            val body = server.request("PUT", "/api/v1/settings/health").body.orEmpty()
            assertEquals("token", server.request("PUT", "/api/v1/settings/health").headers["X-CSRF-Token"])
            assertTrue("\"every_minutes\":15" in body, body)
            assertTrue("\"flap_guard\":3" in body, body)
            assertTrue("\"low_space_mb\":2048" in body, body)
            assertTrue("\"update_check_url\":\"https://example.com/releases\"" in body, body)
            assertTrue("\"update_check_local\":true" in body, body)
            assertTrue("\"update_prerelease\":true" in body, body)
            assertTrue("\"update_check_every_days\":7" in body, body)
            assertFalse("update_check_default" in body.substringBefore("\"update_check_default\":\"d\""), body)
            assertFalse(state.saving)
            assertFalse(state.dirty)
            assertEquals(SettingsHealthChecksNoticeKind.SAVED, state.notice?.kind)
            assertEquals(2, server.recorded().count { it.path == "/api/v1/app/update" })
            presenter.dismissNotice()
            assertNull(presenter.state.value.notice)
        }

    @Test
    fun saveFailureShowsTheErrorAndKeepsTheEdits() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            server.json("PUT", "/api/v1/settings/health", """{"error":"every_minutes is 1 to 1440"}""", 400)
            val presenter = server.healthChecks()
            presenter.load()
            presenter.setNumber(SettingsHealthChecksField.LOW_SPACE, "5")
            presenter.save()
            val state = presenter.state.value
            assertEquals(ApiErrorCode.BAD_REQUEST, state.saveError?.code)
            assertEquals("every_minutes is 1 to 1440", state.saveError?.message)
            assertTrue(state.dirty)
            assertFalse(state.saving)
            presenter.setNumber(SettingsHealthChecksField.LOW_SPACE, "6")
            assertNull(presenter.state.value.saveError)
        }

    @Test
    fun discardGoesBackToTheSavedSettings() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            val presenter = server.healthChecks()
            presenter.load()
            presenter.setNumber(SettingsHealthChecksField.EVERY_MINUTES, "9")
            presenter.setFeed("https://x.example")
            presenter.setDays(30)
            presenter.discard()
            val state = presenter.state.value
            assertFalse(state.dirty)
            assertEquals("5", state.everyText)
            assertEquals("", state.draft.updateCheckUrl)
            assertEquals(1, state.draft.updateCheckEveryDays)
            presenter.save()
            assertTrue(server.recorded().none { it.method == "PUT" })
        }

    @Test
    fun checkAgainReplacesTheList() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            val presenter = server.healthChecks()
            presenter.load()
            server.json("POST", "/api/v1/health/checks/run", """{"checks":[],"worst":"ok","sync":null}""")
            presenter.runChecks()
            val state = presenter.state.value
            assertEquals("token", server.request("POST", "/api/v1/health/checks/run").headers["X-CSRF-Token"])
            assertEquals(Tone.OK, state.worst)
            assertTrue(state.checks.isEmpty())
            assertTrue(state.sync.isEmpty())
            assertFalse(state.running)
        }

    @Test
    fun checkAgainFailureIsReported() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            server.json("POST", "/api/v1/health/checks/run", """{"error":"boom"}""", 500)
            val presenter = server.healthChecks()
            presenter.load()
            presenter.runChecks()
            val state = presenter.state.value
            assertEquals(SettingsHealthChecksNoticeKind.RUN_FAILED, state.notice?.kind)
            assertEquals(ApiErrorCode.SERVER, state.notice?.error?.code)
            assertEquals(3, state.checks.size)
        }

    @Test
    fun firstFailureIsAnErrorAndLaterOnesKeepData() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            server.json("GET", "/api/v1/health/checks", """{"error":"boom"}""", 500)
            val presenter = server.healthChecks()
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            server.json("GET", "/api/v1/health/checks", HC_CHECKS)
            presenter.refresh()
            assertEquals(Phase.READY, presenter.state.value.status.phase)
            server.json("GET", "/api/v1/health/checks", """{"error":"boom"}""", 500)
            presenter.refresh()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals(ApiErrorCode.SERVER, state.status.error?.code)
            assertEquals(3, state.checks.size)
        }

    @Test
    fun liveHealthEventsAndTheTimerReloadTheChecks() =
        runTest {
            val server = FakeServer()
            server.healthChecksRoutes()
            val live = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 16)
            val client = server.client(listOf(FakeServer.SESSION), csrf = "token")
            val presenter = server.healthChecks(api = Api({ client }, live))
            presenter.load()
            val job = backgroundScope.launch { presenter.watch() }
            runCurrent()
            eventually { server.recorded().count { it.path == "/api/v1/health/checks" } >= 2 }
            val before = server.recorded().count { it.path == "/api/v1/health/checks" }
            live.emit(LiveEvent("health", JsonNull, null))
            runCurrent()
            eventually { server.recorded().count { it.path == "/api/v1/health/checks" } > before }
            val again = server.recorded().count { it.path == "/api/v1/health/checks" }
            live.emit(LiveEvent("other", JsonNull, null))
            runCurrent()
            assertEquals(again, server.recorded().count { it.path == "/api/v1/health/checks" })
            advanceTimeBy(30_500)
            eventually { server.recorded().count { it.path == "/api/v1/health/checks" } > again }
            assertEquals(1, server.recorded().count { it.path == "/api/v1/settings/health" })
            job.cancel()
        }
}

class SettingsHealthChecksRulesTest {
    private fun row(
        kind: String,
        subject: String,
        key: String = kind,
        state: String = "ok",
    ) = SettingsHealthChecksRules.row(HealthCheck(key = key, kind = kind, subject = subject, state = state))

    @Test
    fun aSubjectThatRepeatsTheLabelReplacesIt() {
        val repeated = row("backups", "Backups keys")
        assertEquals("Backups keys", repeated.label)
        assertEquals("", repeated.subject)
        val short = row("update", "Up")
        assertEquals("", short.label)
        assertEquals("", short.subject)
        val plain = row("update", "HolyArrs 1.3.0")
        assertEquals("", plain.label)
        assertEquals("HolyArrs 1.3.0", plain.subject)
        assertEquals(SettingsHealthChecksKind.UPDATE, plain.kind)
    }

    @Test
    fun unknownKindsShowTheirName() {
        val unknown = row("new_thing", "Something")
        assertEquals(SettingsHealthChecksKind.OTHER, unknown.kind)
        assertEquals("new_thing", unknown.label)
        assertEquals("Something", unknown.subject)
    }

    @Test
    fun specialKeysAndDownloaderGaps() {
        assertEquals(SettingsHealthChecksKind.MASTER_KEY, row("backups", "Backups", key = "backups:key").kind)
        assertEquals(SettingsHealthChecksKind.USENET_DOWNLOADER, row("no_downloader", "usenet").kind)
        assertEquals(SettingsHealthChecksKind.TORRENT_DOWNLOADER, row("no_downloader", "torrent").kind)
        assertEquals(SettingsHealthChecksKind.SAME_DOWNLOADER, row("same_downloader", "Sab").kind)
        assertEquals("Sab", row("same_downloader", "Sab").subject)
    }

    @Test
    fun statesMapToTones() {
        assertEquals(Tone.OK, row("backups", "", state = "ok").tone)
        assertEquals(Tone.WAIT, row("backups", "", state = "warning").tone)
        assertEquals(Tone.FAIL, row("backups", "", state = "error").tone)
    }
}
