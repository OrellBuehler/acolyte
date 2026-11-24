package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.move.MoveDialog
import ch.orellbuehler.acolyte.feature.move.MoveNoticeKind
import ch.orellbuehler.acolyte.feature.move.MovePersonChoice
import ch.orellbuehler.acolyte.feature.move.MovePickKind
import ch.orellbuehler.acolyte.feature.move.MoveReadForm
import ch.orellbuehler.acolyte.feature.move.MoveStateWord
import ch.orellbuehler.acolyte.feature.move.SettingsMovePresenter
import ch.orellbuehler.acolyte.feature.move.SettingsMoveStep
import ch.orellbuehler.acolyte.session.Role
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MovePresenterTest {
    private val snapshots =
        """{"snapshots":[
            {"id":3,"created_at":"2026-01-02T10:00:00Z","kind":"radarr","via":"api","address":"http://radarr:7878","app_version":"5.1.0",
             "counts":{"titles":120,"indexers":5,"download_clients":2}},
            {"id":2,"created_at":"2026-01-01T10:00:00Z","kind":"radarr","via":"api","address":"http://radarr:7878","counts":{"titles":100}},
            {"id":1,"created_at":"2026-01-01T09:00:00Z","kind":"sonarr","via":"api","address":"http://sonarr:8989","counts":{"titles":40}}
        ]}"""

    private val libraryFolders = """{"library_folders":[{"id":1,"path":"/data/movies","kind":"movie","label":"Movies"}]}"""

    private val plan =
        """{"snapshots":[3,1],"apps":["radarr","sonarr"],"old_paths":["/movies","/tv"],
            "groups":[
              {"name":"folders","label":"Library folders","items":[
                 {"key":"folder:/movies","name":"/movies","from":"radarr folders[0]","folder":{"old_path":"/movies","path":"","kind":"movie","unseen":true}},
                 {"key":"folder:/tv","name":"/tv","from":"sonarr folders[0]","folder":{"old_path":"/tv","path":"","kind":"show","unseen":true}}]},
              {"name":"titles","label":"Titles","items":[
                 {"key":"title:1","name":"Heat (1995)","from":"radarr titles[0]"},{"key":"title:2","name":"Alien (1979)","from":"radarr titles[1]"}]},
              {"name":"people","label":"People","items":[
                 {"key":"person:seerr:1","name":"Ana","from":"seerr users[0]","person":{"app":"seerr","role":"admin","was_owner":true}}]},
              {"name":"sources","label":"Sources","items":[{"key":"src:1","name":"Tracker","from":"radarr indexers[0]","needs":"key"}]}
            ],
            "not_moved":[{"group":"other","from":"radarr settings","what":"UI language","why":"handled by HolyArrs"}],"needs_input":[]}"""

    private fun planDone(hash: String = "h1") =
        """{"check_id":"c1","kind":"plan","state":"done","plan":$plan,"suggested_path_rules":[{"from":"/movies","to":"/data/movies"}],
            "missing_paths":{},"seen_folders":["/data/movies"],"here_folders":["/data/movies"],"plan_hash":"$hash"}"""

    private fun dryDone(
        hash: String,
        checking: Int = 0,
    ) = """{"check_id":"c2","kind":"dry_run","state":"done","plan_hash":"$hash","report":{"dry":true,"apps":["radarr"],"counts":{"create":2},
        "checking":$checking,"lines":[{"group":"titles","key":"title:1","name":"Heat (1995)","result":"create"},
        {"group":"titles","key":"title:2","name":"Alien (1979)","result":"not_found"}]}}"""

    private fun runningCheck(kind: String) = """{"check_id":"c1","kind":"$kind","state":"running","step":"reading"}"""

    private fun presenter(
        server: FakeServer,
        role: Role = Role.OWNER,
        reauth: suspend (String, String?) -> ch.orellbuehler.acolyte.api.ApiError? = { _, _ -> null },
    ) = SettingsMovePresenter(server.api(), { testAccount(role, emptyList()) }, reauth)

    private fun baseServer(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/migrate/snapshots", snapshots)
        server.json("GET", "/api/v1/migrate/moves", """{"moves":[]}""")
        server.json("GET", "/api/v1/migrate/saved-keys", """{"saved":[{"kind":"sonarr","address":"http://sonarr:8989","allow_local_network":true}]}""")
        server.json("GET", "/api/v1/library-folders", libraryFolders)
        return server
    }

    @Test
    fun loadPicksTheNewestReadOfEachCopy() =
        runTest {
            val presenter = presenter(baseServer())
            presenter.load()
            val state = presenter.state.value
            assertTrue(state.allowed)
            assertTrue(state.owner)
            assertEquals(listOf(3L, 2L, 1L), state.reads.map { it.id })
            assertEquals(listOf(true, false, true), state.reads.map { it.picked })
            assertEquals(2, state.pickedCount)
            assertEquals(1, state.savedKeys.size)
            assertEquals("5.1.0", state.reads.first().version)
            assertTrue(state.gate.ok)
        }

    @Test
    fun anAdminDoesNotAskForOwnerOnlyCalls() =
        runTest {
            val server = baseServer()
            val presenter = presenter(server, Role.ADMIN)
            presenter.load()
            assertFalse(presenter.state.value.owner)
            assertTrue(presenter.state.value.savedKeys.isEmpty())
            assertTrue(server.requests.none { it.path.endsWith("saved-keys") })
            presenter.find("radarr.example", MoveReadForm("radarr"))
            assertTrue(server.requests.none { it.path.endsWith("/find") })
        }

    @Test
    fun aMemberIsNotAllowed() =
        runTest {
            val presenter = presenter(baseServer(), Role.MEMBER)
            presenter.load()
            assertFalse(presenter.state.value.allowed)
        }

    @Test
    fun aFailedLoadKeepsTheError() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/migrate/snapshots", """{"error":"boom"}""", 500)
            val presenter = presenter(server)
            presenter.load()
            assertNotNull(presenter.state.value.status.error)
        }

    @Test
    fun readingAnAppPicksItAndBringsItsDownloaders() =
        runTest {
            val server = baseServer()
            server.json(
                "POST",
                "/api/v1/migrate/read",
                """{"snapshot":{"id":9,"kind":"sonarr","address":"http://sonarr:8989","counts":{"titles":10}},"replaced":[1],
                    "needs_reentry":["a","b"],"counts":{"titles":10}}""",
                201,
            )
            server.json(
                "POST",
                "/api/v1/migrate/downloaders",
                """{"downloaders":[{"id":5,"app":"sonarr","name":"qb","kind":"qbittorrent","needs":"password"},{"id":0,"app":"sonarr","name":"x"}]}""",
            )
            val presenter = presenter(server)
            presenter.load()
            assertTrue(presenter.read(MoveReadForm("sonarr", "sonarr:8989", key = "abc")))
            val state = presenter.state.value
            assertEquals(listOf(9L, 3L, 2L), state.reads.map { it.id })
            assertTrue(state.reads.first { it.id == 9L }.picked)
            assertFalse(state.reads.any { it.id == 1L })
            assertEquals(listOf("qb"), state.brought.map { it.name })
            assertEquals("password", state.brought.first().needs)
            val notice = state.notices.single()
            assertEquals(MoveNoticeKind.READ_DONE, notice.kind)
            assertEquals(2, notice.n)
            val sent = server.request("POST", "/api/v1/migrate/read").body.orEmpty()
            assertTrue(sent.contains("\"url\":\"http://sonarr:8989\""))
            assertTrue(sent.contains("\"key\":\"abc\""))
            assertTrue(server.request("POST", "/api/v1/migrate/downloaders").body.orEmpty().contains("[9]"))
        }

    @Test
    fun aFailedProbeSaysWhyInPlainWords() =
        runTest {
            val server = baseServer()
            server.json("POST", "/api/v1/migrate/probe", """{"error":"the app does not answer"}""", 502)
            val presenter = presenter(server)
            presenter.load()
            presenter.probe(MoveReadForm("radarr", "radarr:7878", key = "k"))
            val probe = presenter.state.value.sheet.probe
            assertNotNull(probe)
            assertFalse(probe.ok)
            assertEquals(ch.orellbuehler.acolyte.feature.move.MoveReachFailure.CANT_REACH, probe.problem?.reach)
        }

    @Test
    fun aSeerrProbeListsItsLinkedApps() =
        runTest {
            val server = baseServer()
            server.json("POST", "/api/v1/migrate/probe", """{"app":{"kind":"overseerr","version":"1.33"}}""")
            server.json(
                "POST",
                "/api/v1/migrate/linked",
                """{"apps":[{"kind":"radarr","address":"http://radarr:7878","has_key":true,"reachable":true},
                    {"kind":"sonarr","address":"http://new:8989","has_key":true,"reachable":true,"version":"4"},
                    {"kind":"radarr","address":"http://far","has_key":true,"reachable":false}]}""",
            )
            val presenter = presenter(server)
            presenter.load()
            presenter.probe(MoveReadForm("seerr", "seerr:5055", key = "k"))
            val sheet = presenter.state.value.sheet
            assertEquals("overseerr", sheet.detected)
            assertEquals("Overseerr", sheet.probe?.app)
            assertEquals(listOf("http://new:8989", "http://far"), sheet.linked.map { it.address })
            assertEquals(listOf("http://new:8989"), sheet.addable.map { it.address })
        }

    @Test
    fun anAddedLinkedAppIsReadAndReportedWhenItFails() =
        runTest {
            val server = baseServer()
            server.json("POST", "/api/v1/migrate/probe", """{"app":{"kind":"seerr"}}""")
            var calls = 0
            server.on("POST", "/api/v1/migrate/linked") {
                calls++
                if (calls == 1) {
                    respondJson("""{"apps":[{"kind":"sonarr","address":"http://new:8989","has_key":true,"reachable":true}]}""")
                } else {
                    respondJson(
                        """{"apps":[],"read":[{"kind":"sonarr","snapshot":{"id":11,"kind":"sonarr","address":"http://new:8989"}}],
                            "failed":[{"kind":"radarr","address":"http://x","error":"the app does not answer"}]}""",
                    )
                }
            }
            server.json("POST", "/api/v1/migrate/downloaders", """{"downloaders":[]}""")
            val presenter = presenter(server)
            presenter.load()
            val form = MoveReadForm("seerr", "seerr:5055", key = "k")
            presenter.probe(form)
            presenter.addLinked(form)
            val state = presenter.state.value
            assertTrue(state.reads.any { it.id == 11L && it.picked })
            assertEquals(listOf(MoveNoticeKind.READ_DONE, MoveNoticeKind.LINKED_FAILED), state.notices.map { it.kind })
            assertTrue(state.sheet.linked.isEmpty())
            assertTrue(server.request("POST", "/api/v1/migrate/linked").body.orEmpty().contains("\"add\":[\"http://new:8989\"]"))
        }

    @Test
    fun findingOnTheNetworkRetriesOnceAndKeepsKnownApps() =
        runTest {
            val server = baseServer()
            var calls = 0
            server.on("POST", "/api/v1/migrate/find") {
                calls++
                respondJson(
                    if (calls == 1) {
                        """{"found":[],"searched":["192.168.1.0/24"]}"""
                    } else {
                        """{"found":[{"kind":"radarr","address":"http://192.168.1.10:7878","version":"5"},{"kind":"jackett","address":"http://j"}],"searched":["192.168.1.0/24"]}"""
                    },
                )
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.find("media.example.com", MoveReadForm("radarr"))
            val sheet = presenter.state.value.sheet
            assertEquals(2, calls)
            assertEquals(listOf("192.168.1.10:7878"), sheet.found.map { it.address })
            assertTrue(sheet.findRan)
            assertTrue(server.request("POST", "/api/v1/migrate/find").body.orEmpty().contains("media.example.com"))
        }

    @Test
    fun forgettingAReadUnpicksIt() =
        runTest {
            val server = baseServer()
            server.on("DELETE", "/api/v1/migrate/snapshots/3") { respond("", HttpStatusCode.NoContent) }
            val presenter = presenter(server)
            presenter.load()
            assertTrue(presenter.forget(3))
            assertEquals(listOf(2L, 1L), presenter.state.value.reads.map { it.id })
            assertEquals(1, presenter.state.value.pickedCount)
        }

    private fun chooseServer(
        dryHashes: List<String> = listOf("h1"),
        checking: List<Int> = listOf(0),
    ): FakeServer {
        val server = baseServer()
        server.json("POST", "/api/v1/migrate/plan", runningCheck("plan"), 202)
        var polls = 0
        server.on("GET", "/api/v1/migrate/checks/c1") {
            polls++
            if (polls == 1) respondJson("""{"check_id":"c1","state":"running","step":"titles","done":2,"total":10,"now":"Heat","counts":{"not_found":1}}""") else respondJson(planDone())
        }
        var dry = 0
        server.on("POST", "/api/v1/migrate/dry-run") {
            val n = dry++
            respondJson(dryDone(dryHashes[minOf(n, dryHashes.size - 1)], checking[minOf(n, checking.size - 1)]), 202)
        }
        return server
    }

    @Test
    fun theFolderPlanFillsSuggestionsAndShowsProgress() =
        runTest {
            val server = chooseServer()
            val presenter = presenter(server)
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            val state = presenter.state.value
            assertEquals(SettingsMoveStep.CHOOSE, state.step)
            assertTrue(state.choose.planned)
            assertFalse(state.choose.planning)
            val posts = server.requests.count { it.method == "POST" && it.path.endsWith("/plan") }
            assertEquals(2, posts)
            assertEquals(listOf("/tv"), state.choose.needFolders.map { it.oldPath })
            val movies = state.choose.folders.single()
            assertEquals("/movies", movies.oldPath)
            assertEquals(MovePickKind.PATH, movies.pick)
            assertEquals("/data/movies", movies.path)
            assertEquals(listOf("/data/movies"), movies.options)
            assertEquals(listOf("Titles", "People"), state.choose.tiles.map { it.label })
            assertEquals(2, state.choose.tiles.first { it.id == "titles" }.n)
            assertEquals(1, state.choose.people.size)
            assertTrue(state.choose.people.single().on)
            assertEquals(1, state.choose.sourceKeys)
            assertEquals(1, state.choose.notMovedCount)
            val sent = server.request("POST", "/api/v1/migrate/plan").body.orEmpty()
            assertTrue(sent.contains("\"snapshots\":[3,1]"))
            assertTrue(sent.contains("\"people\":{\"person:seerr:1\":\"owner\"}"))
            assertTrue(sent.contains("\"from\":\"/movies\",\"to\":\"/data/movies\""))
        }

    @Test
    fun aForgottenCheckRestartsUpToThreeTimes() =
        runTest {
            val server = baseServer()
            server.json("POST", "/api/v1/migrate/plan", runningCheck("plan"), 202)
            server.json("GET", "/api/v1/migrate/checks/c1", """{"error":"this check is no longer kept; start it again"}""", 410)
            val presenter = presenter(server)
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            assertEquals(4, server.requests.count { it.method == "POST" && it.path.endsWith("/plan") })
            assertNotNull(presenter.state.value.choose.planProblem)
            assertFalse(presenter.state.value.choose.planning)
        }

    @Test
    fun theDryRunRunsAgainWhileShowsAreStillChecking() =
        runTest {
            val server = chooseServer(listOf("h1", "h2"), listOf(2, 0))
            val presenter = presenter(server)
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            val before = testScheduler.currentTime
            presenter.runDry()
            assertEquals(2, server.requests.count { it.path.endsWith("/dry-run") })
            assertTrue(testScheduler.currentTime - before >= 5_000)
            val choose = presenter.state.value.choose
            assertTrue(choose.tried)
            assertFalse(choose.stale)
            assertEquals(1, choose.problems)
            assertEquals(2, choose.total)
            assertEquals(1, choose.before.size)
        }

    @Test
    fun aChangeMakesTheTestRunStale() =
        runTest {
            val server = chooseServer()
            val presenter = presenter(server)
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            presenter.runDry()
            assertFalse(presenter.state.value.choose.stale)
            presenter.setGroup("sources", false)
            val choose = presenter.state.value.choose
            assertTrue(choose.stale)
            assertTrue(choose.groups.first { it.id == "sources" }.on.not())
            assertFalse(presenter.state.value.canStart)
        }

    @Test
    fun importAsksToConfirmThenStartsWithThePlanHash() =
        runTest {
            val server = chooseServer()
            server.json(
                "POST",
                "/api/v1/migrate/moves",
                """{"move":{"id":7,"state":"queued","snapshots":[3,1],"created_at":"2026-01-03T10:00:00Z"},"job_id":4}""",
                202,
            )
            val presenter = presenter(server)
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            presenter.pickFolder("/tv", MovePickKind.PATH, "/data/tv")
            presenter.runPlan()
            presenter.importNow()
            assertEquals(MoveDialog.BEFORE, presenter.state.value.dialog)
            presenter.start()
            val state = presenter.state.value
            assertEquals(SettingsMoveStep.IMPORT, state.step)
            assertEquals(MoveDialog.NONE, state.dialog)
            assertEquals(7L, state.run?.id)
            assertTrue(state.run?.running == true)
            assertEquals(MoveStateWord.WAITING, state.run?.word)
            assertTrue(server.request("POST", "/api/v1/migrate/moves").body.orEmpty().contains("\"plan_hash\":\"h1\""))
        }

    @Test
    fun aChangedPlanChecksAgainAndAsksAgain() =
        runTest {
            val server = chooseServer(listOf("h1", "h2"))
            var starts = 0
            server.on("POST", "/api/v1/migrate/moves") {
                starts++
                respondJson("""{"error":"the plan changed since its dry run; dry run again"}""", 409)
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            presenter.importNow()
            presenter.start()
            val state = presenter.state.value
            assertEquals(1, starts)
            assertTrue(state.choose.tryNotice)
            assertEquals(2, server.requests.count { it.path.endsWith("/dry-run") })
            assertEquals(MoveDialog.BEFORE, state.dialog)
            assertEquals(SettingsMoveStep.CHOOSE, state.step)
        }

    @Test
    fun aReauthAnswerAsksForThePasswordThenStartsAgain() =
        runTest {
            val server = chooseServer()
            var starts = 0
            server.on("POST", "/api/v1/migrate/moves") {
                starts++
                if (starts == 1) {
                    respondJson("""{"error":"confirm your password","code":"reauth"}""", 403)
                } else {
                    respondJson("""{"move":{"id":8,"state":"running"},"job_id":1}""", 202)
                }
            }
            var asked = ""
            val presenter = presenter(server, reauth = { password, code ->
                asked = "$password/$code"
                null
            })
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            presenter.importNow()
            presenter.start()
            assertTrue(presenter.state.value.needsPassword)
            assertNull(presenter.state.value.problem)
            presenter.confirmPassword("secret", "")
            assertEquals("secret/null", asked)
            assertFalse(presenter.state.value.needsPassword)
            assertEquals(8L, presenter.state.value.run?.id)
            assertEquals(2, starts)
        }

    @Test
    fun aSafeModeRefusalIsRemembered() =
        runTest {
            val server = chooseServer()
            server.json(
                "POST",
                "/api/v1/migrate/moves",
                """{"error":"HolyArrs started in safe mode; restart it normally to import or undo"}""",
                409,
            )
            val presenter = presenter(server)
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            presenter.importNow()
            presenter.start()
            assertTrue(presenter.state.value.safeMode)
            assertEquals(ApiErrorCode.CONFLICT, presenter.state.value.problem?.error?.code)
        }

    @Test
    fun theFollowerWaitsTwoFourThenTenSeconds() =
        runTest {
            val server = baseServer()
            var polls = 0
            server.on("GET", "/api/v1/migrate/moves/7") {
                polls++
                when {
                    polls == 1 ->
                        respondJson("""{"move":{"id":7,"state":"running","snapshots":[3]},"progress":0.1,"progress_note":"1 of 10 imported."}""")
                    polls <= 5 -> respondJson("""{"error":"down"}""", 502)
                    polls == 6 ->
                        respondJson("""{"move":{"id":7,"state":"running","snapshots":[3]},"progress":0.5,"progress_note":"5 of 10 imported."}""")
                    else -> respondJson("""{"move":{"id":7,"state":"done","snapshots":[3]},"progress":1,"progress_note":""}""")
                }
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.openMove(7)
            assertTrue(presenter.state.value.run?.running == true)
            val before = testScheduler.currentTime
            presenter.follow(7)
            assertEquals(7, polls)
            assertEquals(27_000L, testScheduler.currentTime - before)
            val run = presenter.state.value.run
            assertEquals(MoveStateWord.IMPORTED, run?.word)
            assertEquals(SettingsMoveStep.DONE, presenter.state.value.step)
            assertEquals(MoveNoticeKind.IMPORT_FINISHED, presenter.state.value.notices.last().kind)
            assertFalse(run?.lost == true)
        }

    @Test
    fun undoingFollowsLikeAMoveAndTheReportIsText() =
        runTest {
            val server = baseServer()
            server.json(
                "GET",
                "/api/v1/migrate/moves/7",
                """{"move":{"id":7,"state":"done","snapshots":[3],"report":{"counts":{"created":2},"lines":[],"next":["Add folders"]}},"progress":1}""",
            )
            server.json(
                "POST",
                "/api/v1/migrate/moves/7/undo",
                """{"move":{"id":7,"state":"done","undo_state":"queued","snapshots":[3]},"undo_job_id":2}""",
                202,
            )
            server.on("GET", "/api/v1/migrate/moves/7/report") {
                respond("# Report", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/markdown; charset=utf-8"))
            }
            val presenter = presenter(server)
            presenter.load()
            presenter.openMove(7)
            val done = presenter.state.value.run
            assertEquals(SettingsMoveStep.DONE, presenter.state.value.step)
            assertTrue(done?.canUndo == true)
            assertTrue(done?.needsYou == true)
            assertEquals("# Report", presenter.reportText())
            assertEquals("holyarrs-move-7.md", presenter.reportName())
            presenter.askUndo()
            assertEquals(MoveDialog.UNDO, presenter.state.value.dialog)
            presenter.undo()
            val state = presenter.state.value
            assertEquals(MoveDialog.NONE, state.dialog)
            assertTrue(state.run?.undoing == true)
            assertEquals(MoveStateWord.UNDOING, state.run?.word)
        }

    @Test
    fun peopleChoicesAreKeptPerPerson() =
        runTest {
            val server = chooseServer()
            val presenter = presenter(server)
            presenter.load()
            presenter.enter(SettingsMoveStep.CHOOSE)
            presenter.runPlan()
            presenter.setPerson("person:seerr:1", null)
            assertFalse(presenter.state.value.choose.people.single().on)
            presenter.setPerson("person:seerr:2", MovePersonChoice.MERGE)
            presenter.runPlan()
            val sent = server.request("POST", "/api/v1/migrate/plan").body.orEmpty()
            assertTrue(sent.contains("\"person:seerr:2\":\"merge\""))
        }
}
