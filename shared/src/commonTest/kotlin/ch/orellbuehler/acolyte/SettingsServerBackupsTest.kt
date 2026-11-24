package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.clearTransfers
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupPreset
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupsNoticeKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupsPresenter
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerFileKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerRestoreStage
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsServerBackupsTest {
    @AfterTest
    fun clean() = clearTransfers()

    private val listJson =
        """{"backups":[
            {"name":"holyarrs-backup-manual-20261120T101500.000Z.zip","kind":"manual","created_at":"2026-11-20T10:15:00Z","size":5242880},
            {"name":"holyarrs-backup-pre-update-20261101T080000.000Z.zip","kind":"pre-update","created_at":"2026-11-01T08:00:00Z",
              "size":1048576,"detail":"before updating from 0.3.0 to 0.4.0","from_version":"0.3.0","to_version":"0.4.0"}
            ],"preset":"careful","restore_waiting":false,"key_note":"Backups do not include the master key.",
            "key_kit":{"needed":true,"fingerprint":"ab12cd34","level":"warn"}}"""

    private val newest = "holyarrs-backup-manual-20261120T101500.000Z.zip"

    private fun server(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/backups", listJson)
        return server
    }

    private fun presenter(
        server: FakeServer,
        role: Role = Role.OWNER,
        method: SignInMethod = SignInMethod.PASSWORD,
        reauth: suspend (String, String?) -> ApiError? = { _, _ -> null },
    ): SettingsServerBackupsPresenter {
        val account = testAccount(role, listOf("settings")).copy(method = method)
        return SettingsServerBackupsPresenter(server.api(), { account }, testGate(account, reauth))
    }

    private fun MockRequestHandleScope.file(
        name: String,
        bytes: ByteArray,
    ): HttpResponseData =
        respond(
            content = bytes,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentDisposition, """attachment; filename="$name""""),
        )

    private fun read(path: String): ByteArray = SystemFileSystem.source(Path(path)).buffered().use { it.readByteArray() }

    private val reauthBody = """{"error":"confirm your local password again before using this route","code":"reauth"}"""

    @Test
    fun loadsTheBackupsTheNewestFirstAndTheKeyCard() =
        runTest {
            val presenter = presenter(server())
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertFalse(s.off)
            assertEquals(listOf(newest, "holyarrs-backup-pre-update-20261101T080000.000Z.zip"), s.backups.map { it.name })
            assertEquals(5242880L, s.backups[0].size)
            assertEquals("before updating from 0.3.0 to 0.4.0", s.backups[1].detail)
            assertEquals("pre-update", s.backups[1].kind)
            assertEquals(SettingsServerBackupPreset.CAREFUL, s.preset)
            assertFalse(s.restoreWaiting)
            assertEquals("ab12cd34", s.keyKit?.fingerprint)
            assertTrue(s.keyKit?.needed == true)
            assertTrue(s.keyKit.warn)
            assertTrue(s.isOwner)
            assertTrue(s.canChange)
            assertTrue(s.canDownloadKit)
            assertFalse(s.showKeyNote)
            assertFalse(s.isEmpty)
        }

    @Test
    fun theKeyNoteShowsOnlyWithoutAFingerprint() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/backups",
                """{"backups":[],"preset":"balanced","restore_waiting":true,"key_note":"Keep the key file safe.","key_kit":{"needed":false,"fingerprint":"","level":""}}""",
            )
            val presenter = presenter(server)
            presenter.load()
            val s = presenter.state.value
            assertTrue(s.showKeyNote)
            assertFalse(s.canDownloadKit)
            assertTrue(s.restoreWaiting)
            assertTrue(s.isEmpty)
        }

    @Test
    fun anApiKeyCannotDownloadTheKeyKitAndAdminsCannotDownloadAnything() =
        runTest {
            val withKey = presenter(server(), method = SignInMethod.API_KEY)
            withKey.load()
            assertTrue(withKey.state.value.keyKitNeedsSession)
            assertFalse(withKey.state.value.canDownloadKit)
            val server = server()
            val admin =
                SettingsServerBackupsPresenter(server.api(), {
                    testAccount(Role.ADMIN, listOf("settings"))
                }, testGate(testAccount(Role.ADMIN, listOf("settings"))))
            admin.load()
            assertFalse(admin.state.value.isOwner)
            assertTrue(admin.state.value.canChange)
            admin.download(newest)
            assertTrue(server.recorded().none { it.path.startsWith("/api/v1/backups/holyarrs") })
        }

    @Test
    fun peopleWithoutTheSettingsPermissionGetNothing() =
        runTest {
            val server = server()
            val presenter =
                SettingsServerBackupsPresenter(server.api(), {
                    testAccount(Role.ADMIN, listOf("approve"))
                }, testGate(testAccount(Role.ADMIN, listOf("approve"))))
            presenter.load()
            assertFalse(presenter.state.value.allowed)
            assertEquals(0, server.recorded().size)
        }

    @Test
    fun aBuildWithoutTheBackupManagerShowsTheOffState() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/backups", """{"error":"not found"}""", 404)
            val presenter = presenter(server)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.off)
            assertFalse(s.isEmpty)
        }

    @Test
    fun otherLoadFailuresAreErrors() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/backups", """{"error":"could not list backups"}""", 500)
            val presenter = presenter(server)
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.SERVER,
                presenter.state.value.status.error
                    ?.code,
            )
            assertFalse(presenter.state.value.off)
        }

    @Test
    fun makesABackupAndPutsItFirst() =
        runTest {
            val server = server()
            server.json(
                "POST",
                "/api/v1/backups",
                """{"name":"holyarrs-backup-manual-20261124T090000.000Z.zip","kind":"manual","created_at":"2026-11-24T09:00:00Z","size":123}""",
                201,
            )
            val presenter = presenter(server)
            presenter.load()
            presenter.makeNow()
            val s = presenter.state.value
            assertEquals(3, s.backups.size)
            assertEquals("holyarrs-backup-manual-20261124T090000.000Z.zip", s.backups.first().name)
            assertEquals(SettingsServerBackupsNoticeKind.MADE, s.notice?.kind)
            assertFalse(s.making)
        }

    @Test
    fun aBackupThatFailsForSpaceShowsTheServersSentence() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/backups", """{"error":"not enough free space, 120 MB needed and try again."}""", 507)
            val presenter = presenter(server)
            presenter.load()
            presenter.makeNow()
            val s = presenter.state.value
            assertEquals(2, s.backups.size)
            assertEquals(SettingsServerBackupsNoticeKind.MAKE_FAILED, s.notice?.kind)
            assertEquals(507, s.notice?.error?.status)
            assertTrue(
                s.notice
                    ?.error
                    ?.message
                    .orEmpty()
                    .contains("free space"),
            )
            assertFalse(s.making)
        }

    @Test
    fun theBackupPlanIsOptimisticAndRollsBack() =
        runTest {
            val server = server()
            server.on("PUT", "/api/v1/backups/settings") { respondJson((it.body as TextContent).text) }
            val presenter = presenter(server)
            presenter.load()
            presenter.setPreset(SettingsServerBackupPreset.CAREFUL)
            assertTrue(server.recorded().none { it.method == "PUT" })
            presenter.setPreset(SettingsServerBackupPreset.LIGHT)
            var s = presenter.state.value
            assertEquals(SettingsServerBackupPreset.LIGHT, s.preset)
            assertFalse(s.presetSaving)
            assertEquals(SettingsServerBackupsNoticeKind.PLAN_SAVED, s.notice?.kind)
            assertEquals("light", s.notice?.name)
            assertEquals("""{"preset":"light"}""", server.request("PUT", "/api/v1/backups/settings").body)
            server.json("PUT", "/api/v1/backups/settings", """{"error":"unknown preset"}""", 400)
            presenter.setPreset(SettingsServerBackupPreset.OFF)
            s = presenter.state.value
            assertEquals(SettingsServerBackupPreset.LIGHT, s.preset)
            assertEquals(SettingsServerBackupsNoticeKind.PLAN_FAILED, s.notice?.kind)
        }

    @Test
    fun downloadsABackupToATempFileAndHandsItOver() =
        runTest {
            val server = server()
            server.on("GET", "/api/v1/backups/$newest") { file(newest, byteArrayOf(1, 2, 3, 4)) }
            val presenter = presenter(server)
            presenter.load()
            presenter.download(newest)
            val s = presenter.state.value
            val file = assertNotNull(s.file)
            assertEquals(SettingsServerFileKind.BACKUP, file.kind)
            assertEquals(newest, file.name)
            assertEquals(4L, file.bytes)
            assertEquals(listOf<Byte>(1, 2, 3, 4), read(file.path).toList())
            assertTrue(s.backups.none { it.busy })
            presenter.fileDelivered()
            assertNull(presenter.state.value.file)
            assertTrue(SystemFileSystem.exists(Path(file.path)))
            presenter.release()
            assertFalse(SystemFileSystem.exists(Path(file.path)))
        }

    @Test
    fun aDownloadWithoutAFreshPasswordAsksForItAndRetries() =
        runTest {
            val server = server()
            var attempts = 0
            server.on("GET", "/api/v1/backups/$newest") {
                attempts++
                if (attempts == 1) respondJson(reauthBody, 403) else file(newest, byteArrayOf(9))
            }
            var asked = ""
            val presenter =
                presenter(server, reauth = { password, code ->
                    asked = "$password/$code"
                    null
                })
            presenter.load()
            val job = async { presenter.download(newest) }
            presenter.reauth.state.first { it.visible }
            var s = presenter.state.value
            assertNull(s.file)
            assertTrue(s.backups.first().busy)
            presenter.reauth.confirm("secret", "123456")
            job.await()
            s = presenter.state.value
            assertEquals("secret/123456", asked)
            assertFalse(presenter.reauth.state.value.visible)
            assertNotNull(s.file)
            assertEquals(2, attempts)
            assertFalse(s.backups.first().busy)
        }

    @Test
    fun cancellingThePasswordPromptFreesTheRow() =
        runTest {
            val server = server()
            server.json("DELETE", "/api/v1/backups/$newest", reauthBody, 403)
            val presenter = presenter(server)
            presenter.load()
            val job = async { presenter.delete(newest) }
            presenter.reauth.state.first { it.visible }
            presenter.reauth.cancel()
            job.await()
            val s = presenter.state.value
            assertFalse(presenter.reauth.state.value.visible)
            assertTrue(s.backups.none { it.busy })
            assertEquals(2, s.backups.size)
        }

    @Test
    fun aWrongPasswordKeepsThePromptOpenWithTheError() =
        runTest {
            val server = server()
            server.json("DELETE", "/api/v1/backups/$newest", reauthBody, 403)
            val presenter = presenter(server, reauth = { _, _ -> ApiError(ApiErrorCode.FORBIDDEN, 403) })
            presenter.load()
            val job = async { presenter.delete(newest) }
            presenter.reauth.state.first { it.visible }
            presenter.reauth.confirm("nope", "")
            val prompt = presenter.reauth.state.value
            assertTrue(prompt.visible)
            assertEquals(ApiErrorCode.FORBIDDEN, prompt.error?.code)
            presenter.reauth.cancel()
            job.await()
        }

    @Test
    fun deletesABackup() =
        runTest {
            val server = server()
            server.on("DELETE", "/api/v1/backups/$newest") { respondJson("", 204) }
            val presenter = presenter(server)
            presenter.load()
            presenter.delete(newest)
            val s = presenter.state.value
            assertEquals(1, s.backups.size)
            assertEquals(SettingsServerBackupsNoticeKind.DELETED, s.notice?.kind)
        }

    @Test
    fun aFailedDeleteKeepsTheRow() =
        runTest {
            val server = server()
            server.json("DELETE", "/api/v1/backups/$newest", """{"error":"no such backup"}""", 404)
            val presenter = presenter(server)
            presenter.load()
            presenter.delete(newest)
            val s = presenter.state.value
            assertEquals(2, s.backups.size)
            assertEquals(SettingsServerBackupsNoticeKind.DELETE_FAILED, s.notice?.kind)
            assertEquals(ApiErrorCode.NOT_FOUND, s.notice?.error?.code)
            assertTrue(s.backups.none { it.busy })
        }

    @Test
    fun downloadsTheKeyKitAndClearsTheReminder() =
        runTest {
            val server = server()
            server.on("POST", "/api/v1/system/key-kit") { file("holyarrs-key-kit-ab12cd34.key", "# key\nAAAA\n".encodeToByteArray()) }
            val presenter = presenter(server)
            presenter.load()
            presenter.downloadKeyKit()
            val s = presenter.state.value
            val file = assertNotNull(s.file)
            assertEquals(SettingsServerFileKind.KEY_KIT, file.kind)
            assertEquals("holyarrs-key-kit-ab12cd34.key", file.name)
            assertFalse(s.keyKit?.needed == true)
            assertFalse(s.keyKitBusy)
            assertEquals(SettingsServerBackupsNoticeKind.KIT_READY, s.notice?.kind)
        }

    @Test
    fun theKeyKitFailureShowsTheServersWords() =
        runTest {
            val server = server()
            server.json(
                "POST",
                "/api/v1/system/key-kit",
                """{"error":"sign in with your password in the browser to download the key kit"}""",
                403,
            )
            val presenter = presenter(server)
            presenter.load()
            presenter.downloadKeyKit()
            val s = presenter.state.value
            assertEquals(SettingsServerBackupsNoticeKind.KIT_FAILED, s.notice?.kind)
            assertEquals(ApiErrorCode.FORBIDDEN, s.notice?.error?.code)
            assertFalse(s.keyKitBusy)
            assertNull(s.file)
        }

    @Test
    fun aRestoreUploadsTheFileAndWaitsForTheServerToComeBack() =
        runTest {
            val server = server()
            val bytes = ByteArray(2048) { (it % 251).toByte() }
            var received = ByteArray(0)
            var contentType = ""
            server.on("POST", "/api/v1/backups/restore") {
                received = it.body.toByteArray()
                contentType =
                    it.body.contentType
                        ?.toString()
                        .orEmpty()
                respondJson("""{"staged":true,"restarting":true,"note":"The backup is checked and staged."}""", 202)
            }
            var polls = 0
            server.on("GET", "/api/v1/backups") {
                polls++
                when {
                    polls == 1 -> respondJson(listJson)
                    polls == 2 -> respondJson("""{"error":"starting"}""", 503)
                    polls == 3 -> respondJson(listJson.replace(""""restore_waiting":false""", """"restore_waiting":true"""))
                    else -> respondJson(listJson)
                }
            }
            val presenter = presenter(server)
            presenter.load()
            val path = presenter.stagingPath("my backup.zip")
            SystemFileSystem.sink(Path(path)).buffered().use { it.write(bytes) }
            presenter.restore(path, "my backup.zip")
            val s = presenter.state.value
            assertEquals(bytes.toList(), received.toList())
            assertEquals("application/octet-stream", contentType)
            assertEquals(SettingsServerRestoreStage.IDLE, s.restore)
            assertEquals(SettingsServerBackupsNoticeKind.RESTARTED, s.notice?.kind)
            assertFalse(s.restoreWaiting)
            assertEquals(4, polls)
            assertFalse(SystemFileSystem.exists(Path(path)))
        }

    @Test
    fun aRestoreThatCannotRestartItselfOnlyWaits() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/backups/restore", """{"staged":true,"restarting":false,"note":"staged"}""", 202)
            val presenter = presenter(server)
            presenter.load()
            val path = presenter.stagingPath("b.zip")
            SystemFileSystem.sink(Path(path)).buffered().use { it.write(byteArrayOf(1)) }
            presenter.restore(path, "b.zip")
            val s = presenter.state.value
            assertEquals(SettingsServerRestoreStage.IDLE, s.restore)
            assertTrue(s.restoreWaiting)
            assertEquals(SettingsServerBackupsNoticeKind.RESTORE_STAGED, s.notice?.kind)
            assertEquals(1, server.recorded().count { it.path == "/api/v1/backups" })
        }

    @Test
    fun aRefusedRestoreShowsTheReason() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/backups/restore", """{"error":"this backup is from a newer HolyArrs"}""", 400)
            val presenter = presenter(server)
            presenter.load()
            val path = presenter.stagingPath("b.zip")
            SystemFileSystem.sink(Path(path)).buffered().use { it.write(byteArrayOf(1)) }
            presenter.restore(path, "b.zip")
            val s = presenter.state.value
            assertEquals(SettingsServerRestoreStage.IDLE, s.restore)
            assertEquals(SettingsServerBackupsNoticeKind.RESTORE_FAILED, s.notice?.kind)
            assertEquals(400, s.notice?.error?.status)
            assertFalse(s.restoreWaiting)
            assertFalse(SystemFileSystem.exists(Path(path)))
        }

    @Test
    fun aRestoreAsksForThePasswordFirstAndKeepsTheFile() =
        runTest {
            val server = server()
            var attempts = 0
            server.on("POST", "/api/v1/backups/restore") {
                attempts++
                if (attempts == 1) {
                    respondJson(reauthBody, 403)
                } else {
                    respondJson("""{"staged":true,"restarting":false,"note":"staged"}""", 202)
                }
            }
            val presenter = presenter(server)
            presenter.load()
            val path = presenter.stagingPath("b.zip")
            SystemFileSystem.sink(Path(path)).buffered().use { it.write(byteArrayOf(1, 2)) }
            val job = async { presenter.restore(path, "b.zip") }
            presenter.reauth.state.first { it.visible }
            assertEquals(SettingsServerRestoreStage.UPLOADING, presenter.state.value.restore)
            assertTrue(SystemFileSystem.exists(Path(path)))
            presenter.reauth.confirm("pw", "")
            job.await()
            assertEquals(SettingsServerRestoreStage.IDLE, presenter.state.value.restore)
            assertTrue(presenter.state.value.restoreWaiting)
            assertEquals(2, attempts)
        }
}
