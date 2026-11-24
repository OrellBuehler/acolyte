package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.clearTransfers
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerFileKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerLogsPresenter
import ch.orellbuehler.acolyte.session.Role
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsServerLogsTest {
    @AfterTest
    fun clean() = clearTransfers()

    private fun page(
        count: Int,
        total: Int,
        offset: Int = 0,
    ): String {
        val lines =
            (0 until count).joinToString(",") { i ->
                val level = listOf("info", "warning", "error")[i % 3]
                """{"at":"2026-11-24T10:00:0${i % 10}Z","level":"$level","text":"line ${offset + i}"}"""
            }
        return """{"lines":[$lines],"total":$total,"offset":$offset,"limit":100}"""
    }

    private fun presenter(
        server: FakeServer,
        role: Role = Role.ADMIN,
    ) = SettingsServerLogsPresenter(server.api(), { testAccount(role, emptyList()) }, testGate(testAccount(role, emptyList())))

    @Test
    fun loadsTheNewestLinesWithTheirTones() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/logs", page(3, 250))
            val presenter = presenter(server)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals(listOf(Tone.IDLE, Tone.WAIT, Tone.FAIL), s.lines.map { it.tone })
            assertEquals(listOf("info", "warning", "error"), s.lines.map { it.level })
            assertEquals(250, s.total)
            assertEquals(1, s.rangeFrom)
            assertEquals(3, s.rangeTo)
            assertFalse(s.hasNewer)
            assertTrue(s.hasOlder)
            assertFalse(s.noMatch)
            assertEquals("limit=100", server.request("GET", "/api/v1/logs").query)
        }

    @Test
    fun peopleWhoAreNotAdminsGetNothing() =
        runTest {
            val server = FakeServer()
            val presenter =
                SettingsServerLogsPresenter(server.api(), {
                    testAccount(Role.MEMBER, listOf("settings"))
                }, testGate(testAccount(Role.MEMBER, listOf("settings"))))
            presenter.load()
            assertFalse(presenter.state.value.allowed)
            assertEquals(0, server.recorded().size)
        }

    @Test
    fun anEmptyAnswerMatchesNothing() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/logs", """{"lines":[],"total":0,"offset":0,"limit":100}""")
            val presenter = presenter(server)
            presenter.load()
            assertTrue(presenter.state.value.noMatch)
            assertFalse(presenter.state.value.hasOlder)
        }

    @Test
    fun theFilterLeavesEmptyFieldsOutAndStartsAtTheTop() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/logs", page(2, 2))
            val presenter = presenter(server)
            presenter.load()
            presenter.applyFilter("warning", "  qBittorrent  ", "2026-10-01", "")
            var q = server.recorded().last().query
            assertEquals("level=warning&q=qBittorrent&from=2026-10-01&limit=100", q)
            assertTrue(presenter.state.value.filter.active)
            assertEquals("qBittorrent", presenter.state.value.filter.query)
            presenter.applyFilter("bogus", "", "", "2026-10-31")
            q = server.recorded().last().query
            assertEquals("to=2026-10-31&limit=100", q)
            presenter.clear()
            assertEquals("limit=100", server.recorded().last().query)
            assertFalse(presenter.state.value.filter.active)
        }

    @Test
    fun theQueryIsLimitedToTwoHundredCharacters() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/logs", page(1, 1))
            val presenter = presenter(server)
            presenter.applyFilter("", "x".repeat(300), "", "")
            assertEquals(200, presenter.state.value.filter.query.length)
        }

    @Test
    fun pagesBackAndForthByTheLimit() =
        runTest {
            val server = FakeServer()
            server.on("GET", "/api/v1/logs") {
                val offset = it.url.parameters["offset"]?.toInt() ?: 0
                respondJson(page(if (offset >= 200) 50 else 100, 250, offset))
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.older()
            assertEquals(100, presenter.state.value.offset)
            assertEquals("offset=100&limit=100", server.recorded().last().query)
            assertTrue(presenter.state.value.hasNewer)
            assertEquals(101, presenter.state.value.rangeFrom)
            assertEquals(200, presenter.state.value.rangeTo)
            presenter.older()
            assertEquals(200, presenter.state.value.offset)
            assertEquals(250, presenter.state.value.rangeTo)
            assertFalse(presenter.state.value.hasOlder)
            presenter.older()
            assertEquals(200, presenter.state.value.offset)
            presenter.newer()
            assertEquals(100, presenter.state.value.offset)
            presenter.newer()
            assertEquals("limit=100", server.recorded().last().query)
            presenter.newer()
            assertEquals(0, presenter.state.value.offset)
        }

    @Test
    fun applyingAFilterResetsTheOffset() =
        runTest {
            val server = FakeServer()
            server.on("GET", "/api/v1/logs") { respondJson(page(100, 250, it.url.parameters["offset"]?.toInt() ?: 0)) }
            val presenter = presenter(server)
            presenter.load()
            presenter.older()
            presenter.applyFilter("error", "", "", "")
            assertEquals("level=error&limit=100", server.recorded().last().query)
            assertEquals(0, presenter.state.value.offset)
        }

    @Test
    fun aBadFilterShowsTheErrorAndKeepsTheLines() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/logs", page(3, 3))
            val presenter = presenter(server)
            presenter.load()
            server.json("GET", "/api/v1/logs", """{"error":"from must be a date like 2026-10-01 or a full time"}""", 400)
            presenter.applyFilter("", "", "yesterday", "")
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals(ApiErrorCode.BAD_REQUEST, s.status.error?.code)
            assertEquals(3, s.lines.size)
            assertFalse(s.busy)
        }

    @Test
    fun aServerWithoutALogFileSaysSo() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/logs",
                """{"error":"this HolyArrs keeps no log file; read the program's output (docker logs)"}""",
                503,
            )
            val presenter = presenter(server)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.FAILED, s.status.phase)
            assertTrue(s.noLogFile)
            assertTrue(
                s.status.error
                    ?.message
                    .orEmpty()
                    .contains("docker logs"),
            )
        }

    @Test
    fun downloadsTheLogToATempFile() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/logs", page(1, 1))
            server.on("GET", "/api/v1/logs/download") {
                respond(
                    content = "line one\nline two\n".encodeToByteArray(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentDisposition, """attachment; filename="holyarrs-log-20261124-100000.txt""""),
                )
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.download()
            val s = presenter.state.value
            val file = assertNotNull(s.file)
            assertEquals(SettingsServerFileKind.LOG, file.kind)
            assertEquals("holyarrs-log-20261124-100000.txt", file.name)
            assertEquals(18L, file.bytes)
            assertFalse(s.downloading)
            presenter.release()
            assertNull(presenter.state.value.file)
        }

    @Test
    fun aFailedDownloadShowsTheError() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/logs", page(1, 1))
            server.json("GET", "/api/v1/logs/download", """{"error":"this HolyArrs keeps no log file"}""", 503)
            val presenter = presenter(server)
            presenter.load()
            presenter.download()
            val s = presenter.state.value
            assertNull(s.file)
            assertFalse(s.downloading)
            assertEquals(503, s.downloadError?.status)
            presenter.dismissDownloadError()
            assertNull(presenter.state.value.downloadError)
        }

    @Test
    fun aMissingPasswordProofParksTheLoadUntilConfirmed() =
        runTest {
            val server = FakeServer()
            var attempts = 0
            server.on("GET", "/api/v1/logs") {
                attempts++
                if (attempts == 1) {
                    respondJson("""{"error":"confirm your local password again","code":"reauth"}""", 403)
                } else {
                    respondJson(page(2, 2))
                }
            }
            val presenter = presenter(server)
            val job = async { presenter.load() }
            presenter.reauth.state.first { it.visible }
            assertEquals(Phase.LOADING, presenter.state.value.status.phase)
            presenter.reauth.confirm("pw", "")
            job.await()
            assertFalse(presenter.reauth.state.value.visible)
            assertEquals(Phase.READY, presenter.state.value.status.phase)
            assertEquals(2, presenter.state.value.lines.size)
        }
}
