package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.clearTransfers
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.health.HealthPresenter
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusAboutLoad
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDiskKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDiskLevel
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDueKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusGroup
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusHeadlineKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMarker
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusMode
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusNoticeKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusPhase
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRead
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusStanding
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusWord
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val STATUS_NOW = Instant.parse("2026-11-24T12:00:00Z").toEpochMilliseconds()

private const val STATES =
    """{"safe_mode":false,"states":[
        {"key":"stage:alerts","kind":"stage","name":"Alerts","state":"running"},
        {"key":"link:source:1","kind":"source","name":"Indexer","state":"running","since":"2026-11-20T10:00:00Z"},
        {"key":"link:downloader:2","kind":"downloader","name":"qBit","state":"running","since":"2026-11-20T10:00:00Z"},
        {"key":"link:downloader:3","kind":"downloader","name":"Sab","state":"failed","reason":"Connection refused","since":"2026-11-21T10:00:00Z"},
        {"key":"link:media_server:4","kind":"media_server","name":"Plex","state":"retrying","reason":"Retrying soon","since":"2026-11-21T10:00:00Z"},
        {"key":"link:subtitle_site:5","kind":"subtitle_site","name":"OpenSubs","state":"starting"},
        {"key":"part:movies","kind":"part","name":"Movies","state":"running"}]}"""

private const val CHECKS =
    """{"checks":[
        {"id":1,"key":"library_folder:1","kind":"library_folder","subject":"/media/movies","state":"error","message":"Folder is missing",
         "since":"2026-11-22T10:00:00Z","checked_at":"2026-11-23T10:00:00Z"},
        {"id":2,"key":"disk_space:1","kind":"disk_space","subject":"/media/movies","state":"warning","message":"Only 500 MB free",
         "since":"2026-11-22T10:00:00Z","checked_at":"2026-11-23T10:00:00Z"},
        {"id":3,"key":"downloader:2","kind":"downloader","subject":"qBit","state":"warning","message":"Every download stopped"},
        {"id":4,"key":"backups","kind":"backups","subject":"Backups","state":"ok","message":"","checked_at":"2026-11-23T10:00:00Z"},
        {"id":5,"key":"backups:key","kind":"backups","subject":"Backups","state":"ok","message":"Recommended: download your key kit"},
        {"id":6,"key":"update","kind":"update","subject":"HolyArrs 1.3.0","state":"ok"},
        {"id":7,"key":"trailers","kind":"trailers","subject":"Trailer reader","state":"ok","new_field":1}
    ],"worst":"error","sync":null}"""

private const val SCHEDULES =
    """{"schedules":[
        {"kind":"refresh_titles","label":"Refresh movie details","enabled":true,"state":"waiting","last_run":"2026-11-24T08:00:00Z",
         "next_run":"2026-11-24T20:00:00Z","last_job":"done","last_job_ended":"2026-11-24T08:00:10Z","part":"movies"},
        {"kind":"health_check","label":"Check health","enabled":true,"state":"waiting","last_job":"failed","last_job_error":"boom",
         "last_job_ended":"2026-11-24T09:00:00Z","next_run":"2026-11-24T12:30:00Z"},
        {"kind":"clean_history","label":"Clean history","enabled":true,"state":"waiting","last_run":"2026-11-24T07:00:00Z",
         "next_run":"2026-11-24T11:00:00Z"},
        {"kind":"sync_outside_lists","label":"Read Outside lists","enabled":false,"state":"off"},
        {"kind":"rescan_library","label":"Rescan","enabled":true,"state":"running","last_run":"2026-11-24T11:00:00Z",
         "next_run":"2026-11-24T23:00:00Z"}
    ],"loops":[],"safe_mode":false}"""

private const val LISTS =
    """{"outside_lists":[
        {"id":1,"name":"Trakt watchlist","enabled":true,"last_synced_at":"2026-11-24T10:00:00Z","last_result":"Added 3"},
        {"id":2,"name":"Broken list","enabled":true,"last_error":"token expired"}]}"""

private const val ALERTS =
    """{"alerts":[{"id":1,"name":"Discord","enabled":true,"last_sent_at":"2026-11-24T10:00:00Z","kind":"discord"}],"moments":["health"]}"""

private const val ACTIVE =
    """{"jobs":[
        {"id":9,"kind":"rescan_library","label":"Rescan","state":"running","progress":0.4,"note":"Scanning","queued_at":"2026-11-24T11:00:00Z",
         "started_at":"2026-11-24T11:00:05Z","by_person":false},
        {"id":10,"kind":"search_missing","label":"Search missing","state":"queued","progress":0,"queued_at":"2026-11-24T11:30:00Z","by_person":true}]}"""

private const val HISTORY =
    """{"jobs":[
        {"ID":3,"Kind":"health_check","State":"failed","StartedAt":"2026-11-24T08:59:00Z","EndedAt":"2026-11-24T09:00:00Z","Progress":1,
         "ProgressNote":null,"Result":null,"Error":{"code":"x","message":"boom"}},
        {"ID":2,"Kind":"refresh_titles","State":"done","StartedAt":"2026-11-24T08:00:00Z","EndedAt":"2026-11-24T08:00:10Z","Progress":1},
        {"ID":1,"Kind":"rescan_library","State":"running","StartedAt":"2026-11-24T11:00:05Z","EndedAt":null,"Progress":0.4}]}"""

private const val DISKS =
    """{"disks":[
        {"path":"/media/movies","kind":"movie","label":"","free_bytes":50000000,"total_bytes":1000000000},
        {"path":"/media/shows","kind":"show","label":"TV","free_bytes":-1,"total_bytes":-1}]}"""

private const val UPDATE =
    """{"on":true,"feed":"https://api.github.com/x","checked_at":"2026-11-24T10:00:00Z","latest":"1.4.0","newer":true,
        "link":"https://github.com/HolyArrs/HolyArrs/releases/tag/v1.4.0","notes":"One\nTwo\n","version":"1.3.0"}"""

private const val INSTALL =
    """{"available":true,"mode":"ask","mode_locked":false,"check_on":true,"source":"image","previous":"1.2.0","launch":{"source":"image"},
        "progress":{"phase":"idle","at":"0001-01-01T00:00:00Z"},"ready":"1.4.0","one_way":true,"running":"1.3.0"}"""

private const val ABOUT =
    """{"version":"1.3.0","go":"go1.27.4","os":"linux/amd64","docker":true,"sqlite":"3.53.4","migration":159,"config_dir":"/config",
        "start_dir":"/","uptime_s":1634,"started_at":"2026-11-24T11:32:46Z"}"""

private const val SERVER =
    """{"ok":true,"version":"1.3.0","schema":159,"uptime_s":8,"write_wait_p95_ms":0.6,"write_wait_samples":75,"writes":83,
        "live_clients":2,"url_base":"/holyarrs","sources":null,"downloaders":null}"""

internal fun FakeServer.statusRoutes() {
    json("GET", "/api/v1/health/states", STATES)
    json("GET", "/api/v1/health/checks", CHECKS)
    json("POST", "/api/v1/health/checks/run", CHECKS)
    json("GET", "/api/v1/schedules", SCHEDULES)
    json("GET", "/api/v1/outside-lists", LISTS)
    json("GET", "/api/v1/alerts", ALERTS)
    json("GET", "/api/v1/metadata/health", """{"up":true}""")
    json("GET", "/api/v1/jobs/active", ACTIVE)
    json("GET", "/api/v1/jobs", HISTORY)
    json("GET", "/api/v1/health/disks", DISKS)
    json("GET", "/api/v1/app/update", UPDATE)
    json("GET", "/api/v1/app/update/install", INSTALL)
    json("GET", "/api/v1/app/about", ABOUT)
    json("GET", "/api/v1/health", SERVER)
}

private fun FakeServer.presenter(
    account: Account = downloadsAccount(),
    api: Api = api(client(listOf(FakeServer.SESSION), csrf = "token")),
): HealthPresenter = HealthPresenter(api, { account }, { STATUS_NOW })

private fun settingsOnly(): Account = downloadsAccount(role = "user", perms = listOf("manage_library", "settings"))

class HealthPresenterTest {
    @Test
    fun loadsEverythingTheStatusPageShows() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            assertEquals(Phase.LOADING, presenter.state.value.status.phase)
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertTrue(state.canAct)
            assertTrue(state.canManageUpdates)
            assertEquals(emptyList(), state.failedReads)
            assertEquals(SettingsStatusHeadlineKind.ATTENTION, state.headline.kind)
            assertEquals(6, state.headline.checks)
            assertEquals(2, state.headline.jobs)
            assertEquals(
                listOf(
                    "link:downloader:3",
                    "library_folder:1",
                    "list:2",
                    "job:health_check",
                    "link:downloader:2",
                    "link:media_server:4",
                    "disk_space:1",
                    "job:clean_history",
                ),
                state.problems.map { it.key },
            )
            assertEquals(
                listOf(
                    SettingsStatusGroup.SOURCES,
                    SettingsStatusGroup.DOWNLOADERS,
                    SettingsStatusGroup.MEDIA,
                    SettingsStatusGroup.SUBTITLES,
                    SettingsStatusGroup.LISTS,
                    SettingsStatusGroup.ALERTS,
                    SettingsStatusGroup.METADATA,
                ),
                state.kinds.map { it.group },
            )
            assertEquals(SettingsStatusGroup.DOWNLOADERS, state.defaultKind)
            assertEquals(listOf("link:downloader:3", "link:downloader:2"), state.kinds[1].rows.map { it.key })
            assertEquals(
                listOf(
                    SettingsStatusMarker.OK,
                    SettingsStatusMarker.FAIL,
                    SettingsStatusMarker.WAIT,
                    SettingsStatusMarker.IDLE,
                    SettingsStatusMarker.FAIL,
                    SettingsStatusMarker.OK,
                    SettingsStatusMarker.OK,
                ),
                state.kinds.map { it.marker },
            )
        }

    @Test
    fun storageShowsDisksWithTheirWorstCheckAndTheBackupsLine() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            presenter.load()
            val state = presenter.state.value
            assertTrue(state.disksLoaded)
            val movies = state.disks[0]
            assertEquals(SettingsStatusDiskKind.MOVIES, movies.kind)
            assertEquals(95, movies.usedPercent)
            assertEquals(SettingsStatusDiskLevel.FULL, movies.level)
            assertEquals("library_folder:1", movies.problem?.key)
            val shows = state.disks[1]
            assertEquals("TV", shows.label)
            assertEquals(-1, shows.usedPercent)
            assertNull(shows.problem)
            assertEquals("backups", state.backups?.key)
            assertEquals("backups:key", state.keyKit?.key)
        }

    @Test
    fun jobsTabListsRunningNextAndFinished() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            presenter.load()
            val state = presenter.state.value
            assertEquals(listOf("Rescan", "Search missing"), state.running.map { it.name })
            assertEquals(40, state.running[0].percent)
            assertTrue(state.running[1].queued)
            assertEquals(listOf("job:clean_history", "job:health_check", "job:refresh_titles"), state.nextUp.map { it.key })
            assertEquals(SettingsStatusDueKind.OVERDUE, state.nextUp[0].due.kind)
            assertEquals(SettingsStatusDueKind.IN, state.nextUp[1].due.kind)
            assertEquals(30, state.nextUp[1].due.amount)
            assertEquals(listOf(3L, 2L), state.finished.map { it.id })
            assertEquals(listOf("Check health", "Refresh movie details"), state.finished.map { it.name })
            assertEquals(SettingsStatusMarker.FAIL, state.jobsBadge)
            val lines = state.jobLines.associateBy { it.row.key }
            assertEquals(SettingsStatusWord.FAILED, lines.getValue("job:health_check").row.word)
            assertEquals(SettingsStatusWord.LATE, lines.getValue("job:clean_history").row.word)
            assertEquals(SettingsStatusDueKind.OVERDUE, lines.getValue("job:clean_history").due.kind)
            assertEquals(SettingsStatusDueKind.NONE, lines.getValue("job:sync_outside_lists").due.kind)
            assertEquals("2026-11-24T08:00:10Z", lines.getValue("job:refresh_titles").row.lastRun)
        }

    @Test
    fun updatesAndAboutTabs() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            presenter.load()
            val state = presenter.state.value
            assertEquals(SettingsStatusStanding.BEHIND, state.standing)
            assertTrue(state.updateAvailable)
            assertEquals(listOf("One", "Two"), state.updateNotes)
            assertEquals("https://github.com/HolyArrs/HolyArrs/releases/tag/v1.4.0", state.updateLink)
            assertEquals(SettingsStatusMode.ASK, state.installMode)
            assertEquals(SettingsStatusPhase.IDLE, state.installPhase)
            assertTrue(state.installReady)
            assertTrue(state.installCanGoBack)
            assertFalse(state.installMoving)
            assertEquals(SettingsStatusAboutLoad.READY, state.aboutLoad)
            assertEquals("1.3.0", state.about.version)
            assertEquals("1.27.4", state.about.go)
            assertTrue(state.about.container)
            assertEquals(159, state.about.migration)
            assertEquals(1634L, state.about.uptimeSeconds)
            assertTrue(state.about.hasServer)
            assertEquals(83L, state.about.writes)
            assertEquals("/holyarrs", state.about.urlBase)
            assertEquals(2, state.about.liveClients)
            assertEquals(STATUS_NOW, state.about.loadedAt)
        }

    @Test
    fun peopleWithoutSettingsGetAReadOnlyPage() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter(downloadsAccount(role = "user", perms = listOf("manage_library")))
            presenter.load()
            val state = presenter.state.value
            assertFalse(state.canAct)
            assertFalse(state.canManageUpdates)
            assertFalse(state.canBundle)
            assertNull(state.install)
            assertEquals(SettingsStatusAboutLoad.DENIED, state.aboutLoad)
            assertEquals("1.3.0", state.about.version)
            assertEquals(5, state.kinds.size)
            assertTrue(state.kinds.none { it.group == SettingsStatusGroup.LISTS || it.group == SettingsStatusGroup.ALERTS })
            val paths = server.recorded().map { it.path }
            assertTrue("/api/v1/outside-lists" !in paths)
            assertTrue("/api/v1/alerts" !in paths)
            assertTrue("/api/v1/app/about" !in paths)
            assertTrue("/api/v1/app/update/install" !in paths)
            assertTrue(state.allowed)
        }

    @Test
    fun peopleWhoManageNothingAreTurnedAway() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter(downloadsAccount(role = "user", perms = listOf("ask")))
            presenter.load()
            assertFalse(presenter.state.value.allowed)
            assertEquals(Phase.READY, presenter.state.value.status.phase)
            assertTrue(server.recorded().isEmpty())
        }

    @Test
    fun forbiddenListsAreLeftOutAndOtherFailuresAreReported() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("GET", "/api/v1/outside-lists", """{"error":"your account may not do this"}""", 403)
            server.json("GET", "/api/v1/alerts", """{"error":"boom"}""", 500)
            server.json("GET", "/api/v1/health/checks", """{"error":"boom"}""", 500)
            val presenter = server.presenter()
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals(listOf(SettingsStatusRead.CHECKS, SettingsStatusRead.ALERTS), state.failedReads)
            assertTrue(state.kinds.none { it.group == SettingsStatusGroup.LISTS })
            assertTrue(state.kinds.any { it.group == SettingsStatusGroup.ALERTS })
        }

    @Test
    fun routesThatAskForAPasswordProofAreLeftOutInsteadOfFailing() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val reauth = """{"error":"confirm your local password again before using this route","code":"reauth"}"""
            server.json("GET", "/api/v1/alerts", reauth, 403)
            server.json("GET", "/api/v1/app/about", reauth, 403)
            server.json("GET", "/api/v1/app/update/install", reauth, 403)
            val presenter = server.presenter()
            presenter.load()
            val state = presenter.state.value
            assertTrue(state.failedReads.isEmpty())
            assertTrue(state.kinds.none { it.group == SettingsStatusGroup.ALERTS })
            assertNull(state.install)
            assertEquals(SettingsStatusAboutLoad.DENIED, state.aboutLoad)
        }

    @Test
    fun partOffListsAreLeftOutToo() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("GET", "/api/v1/outside-lists", """{"error":"this part is off","part":"media","text":"Movies is off."}""", 409)
            val presenter = server.presenter()
            presenter.load()
            assertTrue(
                presenter.state.value.failedReads
                    .isEmpty(),
            )
            assertTrue(
                presenter.state.value.kinds
                    .none { it.group == SettingsStatusGroup.LISTS },
            )
        }

    @Test
    fun firstFailureShowsTheErrorWhenPartsAndJobsBothFail() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("GET", "/api/v1/health/states", """{"error":"your account may not do this; ask the owner"}""", 403)
            server.json("GET", "/api/v1/schedules", """{"error":"boom"}""", 500)
            val presenter = server.presenter()
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.FAILED, state.status.phase)
            assertEquals(ApiErrorCode.FORBIDDEN, state.status.error?.code)
            assertEquals("your account may not do this; ask the owner", state.status.error?.message)
        }

    @Test
    fun oneFailedReadOnlyShowsABannerOnReadyData() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            presenter.load()
            server.json("GET", "/api/v1/health/states", """{"error":"boom"}""", 500)
            presenter.refresh()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertNull(state.status.error)
            assertEquals(listOf(SettingsStatusRead.PARTS), state.failedReads)
            assertTrue(state.linkedUnreadable)
            assertEquals(SettingsStatusHeadlineKind.ATTENTION, state.headline.kind)
        }

    @Test
    fun failedRefreshOfBothMainReadsKeepsData() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            presenter.load()
            server.json("GET", "/api/v1/health/states", """{"error":"boom"}""", 500)
            server.json("GET", "/api/v1/schedules", """{"error":"boom"}""", 500)
            presenter.refresh()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertFalse(state.status.refreshing)
            assertEquals(ApiErrorCode.SERVER, state.status.error?.code)
            assertEquals(7, state.kinds.size)
            assertTrue(state.problems.isNotEmpty())
        }

    @Test
    fun metadataFailureShowsTheServerDown() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.on("GET", "/api/v1/metadata/health") { throw IllegalStateException("Unable to resolve host") }
            val presenter = server.presenter()
            presenter.load()
            assertEquals(
                SettingsStatusMarker.FAIL,
                presenter.state.value.kinds
                    .last()
                    .marker,
            )
        }

    @Test
    fun checkAllRunsTheChecksReloadsDisksAndSaysSo() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            presenter.load()
            val disksBefore = server.recorded().count { it.path == "/api/v1/health/disks" }
            presenter.checkAll()
            val state = presenter.state.value
            assertEquals("token", server.request("POST", "/api/v1/health/checks/run").headers["X-CSRF-Token"])
            assertEquals(SettingsStatusNoticeKind.CHECKS_RAN, state.notice?.kind)
            assertEquals(disksBefore + 1, server.recorded().count { it.path == "/api/v1/health/disks" })
            assertTrue(state.busy.isEmpty())
            assertNull(state.actionError)
            presenter.dismissNotice()
            assertNull(presenter.state.value.notice)
        }

    @Test
    fun checkAllFailureIsReported() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json(
                "POST",
                "/api/v1/health/checks/run",
                """{"error":"confirm your local password again before using this route","code":"reauth"}""",
                403,
            )
            val presenter = server.presenter()
            presenter.runChecks()
            val state = presenter.state.value
            assertEquals(ApiErrorCode.REAUTH, state.actionError?.code)
            assertEquals(SettingsStatusNoticeKind.CHECKS_FAILED, state.notice?.kind)
            assertEquals(ApiErrorCode.REAUTH, state.notice?.error?.code)
            assertTrue(state.busy.isEmpty())
        }

    @Test
    fun staleCsrfIsRefreshedOnce() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            var calls = 0
            server.on("POST", "/api/v1/health/checks/run") { request ->
                calls++
                if (request.headers["X-CSRF-Token"] ==
                    "fresh"
                ) {
                    respondJson(CHECKS)
                } else {
                    respondJson("""{"error":"missing or wrong CSRF token"}""", 403)
                }
            }
            server.json("GET", "/api/v1/auth/me", meJson(csrf = "fresh"))
            val client = server.client(listOf(FakeServer.SESSION), csrf = "stale")
            val presenter = server.presenter(api = server.api(client))
            presenter.checkAll()
            assertEquals(2, calls)
            assertEquals("fresh", client.credentials.csrf)
            assertNull(presenter.state.value.actionError)
        }

    @Test
    fun checkingALinkedAppTestsItAndReportsTheAnswer() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("POST", "/api/v1/sources/1/test", """{"ok":true,"text":"Prowlarr v1.2.3 answered","items":4,"ms":20}""")
            val presenter = server.presenter()
            presenter.load()
            presenter.check("link:source:1")
            var notice = presenter.state.value.notice
            assertEquals(SettingsStatusNoticeKind.ROW_CONNECTED, notice?.kind)
            assertEquals("Indexer", notice?.row?.name)
            assertEquals("Prowlarr v1.2.3", notice?.detail)
            server.json("POST", "/api/v1/downloaders/3/test", """{"ok":false,"text":" Connection refused \n","ms":1}""")
            presenter.check("link:downloader:3")
            notice = presenter.state.value.notice
            assertEquals(SettingsStatusNoticeKind.ROW_CANT_CONNECT, notice?.kind)
            assertEquals("Connection refused", notice?.detail)
            server.json("POST", "/api/v1/downloaders/2/test", """{"error":"no such Downloader"}""", 404)
            presenter.check("link:downloader:2")
            notice = presenter.state.value.notice
            assertEquals(SettingsStatusNoticeKind.ROW_CANT_CHECK, notice?.kind)
            assertEquals(ApiErrorCode.NOT_FOUND, notice?.error?.code)
            assertTrue(
                presenter.state.value.busy
                    .isEmpty(),
            )
        }

    @Test
    fun runningAJobStartsItOrSaysItIsAlreadyRunning() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("POST", "/api/v1/schedules/health_check/run", """{"job_id":44}""")
            server.json("POST", "/api/v1/schedules/clean_history/run", """{"job_id":45,"already":true}""")
            val presenter = server.presenter()
            presenter.load()
            presenter.check("job:health_check")
            assertEquals(
                SettingsStatusNoticeKind.JOB_STARTED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                "Check health",
                presenter.state.value.notice
                    ?.row
                    ?.name,
            )
            presenter.check("job:clean_history")
            assertEquals(
                SettingsStatusNoticeKind.JOB_ALREADY,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun checkingAStorageRowRunsTheChecksAndReportsItsNewWord() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            presenter.load()
            presenter.check("library_folder:1")
            val notice = presenter.state.value.notice
            assertEquals(SettingsStatusNoticeKind.ROW_WORD, notice?.kind)
            assertEquals(SettingsStatusWord.BROKEN, notice?.row?.word)
            server.json("GET", "/api/v1/health/checks", """{"checks":[],"worst":"ok"}""")
            presenter.check("library_folder:1")
            assertEquals(
                SettingsStatusNoticeKind.CHECKS_RAN,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun checkingAFineStorageRowSaysItWorks() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter()
            presenter.load()
            presenter.check("backups")
            assertEquals(
                SettingsStatusNoticeKind.ROW_WORKING,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun actionsNeedSettingsAndAKnownRow() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val presenter = server.presenter(downloadsAccount(role = "user", perms = listOf("manage_library")))
            presenter.load()
            presenter.checkAll()
            presenter.check("link:source:1")
            presenter.check("link:downloader:2")
            presenter.checkUpdates()
            assertTrue(server.recorded().none { it.method == "POST" })
            assertNull(presenter.state.value.notice)
            val owner = server.presenter()
            owner.load()
            owner.check("no-such-row")
            assertTrue(server.recorded().none { it.method == "POST" })
        }

    @Test
    fun checkingForUpdatesStoresTheAnswerOrFails() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json(
                "POST",
                "/api/v1/app/update/check",
                """{"on":true,"newer":false,"checked_at":"2026-11-24T12:00:00Z","version":"1.4.0"}""",
            )
            val presenter = server.presenter()
            presenter.load()
            presenter.checkUpdates()
            assertEquals(SettingsStatusStanding.CURRENT, presenter.state.value.standing)
            assertFalse(presenter.state.value.updateAvailable)
            assertFalse(presenter.state.value.checkingUpdate)
            server.json("POST", "/api/v1/app/update/check", """{"error":"feed answered 500"}""", 502)
            presenter.checkUpdates()
            assertEquals(
                SettingsStatusNoticeKind.UPDATE_CHECK_FAILED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(SettingsStatusStanding.CURRENT, presenter.state.value.standing)
        }

    @Test
    fun inAppSettingsAreWrittenByOwnersAndAdminsOnly() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.on("PUT", "/api/v1/app/update/mode") {
                val body = (it.body as TextContent).text
                var view = INSTALL
                if ("\"mode\":\"auto\"" in body) view = view.replace("\"mode\":\"ask\"", "\"mode\":\"auto\"")
                if ("\"check_on\":false" in body) view = view.replace("\"check_on\":true", "\"check_on\":false")
                respondJson(view)
            }
            val presenter = server.presenter()
            presenter.load()
            presenter.setMode(SettingsStatusMode.AUTO)
            assertEquals("""{"mode":"auto"}""", server.request("PUT", "/api/v1/app/update/mode").body)
            assertEquals(SettingsStatusMode.AUTO, presenter.state.value.installMode)
            assertEquals(
                SettingsStatusNoticeKind.UPDATE_SAVED,
                presenter.state.value.notice
                    ?.kind,
            )
            val updateReads = server.recorded().count { it.path == "/api/v1/app/update" }
            presenter.setCheckOn(false)
            assertEquals("""{"check_on":false}""", server.request("PUT", "/api/v1/app/update/mode").body)
            assertEquals(
                false,
                presenter.state.value.install
                    ?.checkOn,
            )
            assertEquals(updateReads + 1, server.recorded().count { it.path == "/api/v1/app/update" })

            val limited = server.presenter(settingsOnly())
            limited.load()
            assertNotNull(limited.state.value.install)
            assertFalse(limited.state.value.canManageUpdates)
            val puts = server.recorded().count { it.method == "PUT" }
            limited.setMode(SettingsStatusMode.OFF)
            limited.setCheckOn(true)
            limited.installNow()
            limited.goBack()
            assertEquals(puts, server.recorded().count { it.method == "PUT" })
            assertTrue(server.recorded().none { it.path == "/api/v1/app/update/install" && it.method == "POST" })
        }

    @Test
    fun lockedModesAreNotWritten() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("GET", "/api/v1/app/update/install", INSTALL.replace("\"mode_locked\":false", "\"mode_locked\":true"))
            val presenter = server.presenter()
            presenter.load()
            assertTrue(presenter.state.value.installLocked)
            presenter.setMode(SettingsStatusMode.AUTO)
            assertTrue(server.recorded().none { it.method == "PUT" })
        }

    @Test
    fun saveFailureKeepsTheModeAndSaysSo() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("PUT", "/api/v1/app/update/mode", """{"error":"HOLYARRS_UPDATES sets the mode"}""", 409)
            val presenter = server.presenter()
            presenter.load()
            presenter.setMode(SettingsStatusMode.AUTO)
            assertEquals(SettingsStatusMode.ASK, presenter.state.value.installMode)
            assertEquals(
                SettingsStatusNoticeKind.UPDATE_SAVE_FAILED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                "HOLYARRS_UPDATES sets the mode",
                presenter.state.value.notice
                    ?.error
                    ?.message,
            )
            assertFalse(presenter.state.value.installBusy)
        }

    @Test
    fun updateNowAndGoBackReplaceTheInstallView() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val moving = INSTALL.replace("\"phase\":\"idle\"", "\"phase\":\"downloading\",\"version\":\"1.4.0\"")
            server.json("POST", "/api/v1/app/update/install", moving, 202)
            server.json("POST", "/api/v1/app/update/go-back", """{"error":"nothing to go back to"}""", 409)
            val presenter = server.presenter()
            presenter.load()
            presenter.goBack()
            assertEquals(
                SettingsStatusNoticeKind.UPDATE_GO_BACK_FAILED,
                presenter.state.value.notice
                    ?.kind,
            )
            presenter.installNow()
            val state = presenter.state.value
            assertEquals(SettingsStatusPhase.DOWNLOADING, state.installPhase)
            assertTrue(state.installMoving)
            assertFalse(state.installReady)
            assertFalse(state.installCanGoBack)
            assertFalse(state.installBusy)
        }

    @Test
    fun installFailureIsReported() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("POST", "/api/v1/app/update/install", """{"error":"no update is downloaded and checked yet"}""", 409)
            val presenter = server.presenter()
            presenter.load()
            presenter.installNow()
            assertEquals(
                SettingsStatusNoticeKind.UPDATE_INSTALL_FAILED,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun supportBundleNeedsUsersAndIsHandedOver() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.on("GET", "/api/v1/support/bundle") {
                respond(
                    content = "zipdata",
                    headers =
                        headersOf(
                            HttpHeaders.ContentType to listOf("application/zip"),
                            HttpHeaders.ContentDisposition to listOf("attachment; filename=\"holyarrs-support-20261124-120000.zip\""),
                        ),
                )
            }
            val limited = server.presenter(settingsOnly())
            limited.load()
            limited.downloadBundle()
            assertNull(limited.state.value.bundle)
            assertTrue(server.recorded().none { it.path == "/api/v1/support/bundle" })
            val presenter = server.presenter()
            presenter.load()
            assertTrue(presenter.state.value.canBundle)
            presenter.downloadBundle()
            val file = presenter.state.value.bundle
            assertEquals("holyarrs-support-20261124-120000.zip", file?.name)
            assertEquals(7L, file?.bytes)
            assertFalse(presenter.state.value.bundleBusy)
            presenter.bundleHandled()
            assertNull(presenter.state.value.bundle)
            clearTransfers()
        }

    @Test
    fun supportBundleFailureIsReported() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("GET", "/api/v1/support/bundle", """{"error":"boom"}""", 500)
            val presenter = server.presenter()
            presenter.load()
            presenter.downloadBundle()
            assertEquals(
                SettingsStatusNoticeKind.BUNDLE_FAILED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertNull(presenter.state.value.bundle)
        }

    @Test
    fun liveHealthAndJobEventsReloadAfterTheThrottle() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val live = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 16)
            val client = server.client(listOf(FakeServer.SESSION), csrf = "token")
            val presenter = server.presenter(api = Api({ client }, live))
            presenter.load()
            val job = backgroundScope.launch { presenter.watch() }
            runCurrent()
            advanceTimeBy(500)
            eventually { server.recorded().count { it.path == "/api/v1/health/states" } >= 2 }
            val before = server.recorded().count { it.path == "/api/v1/health/states" }
            live.emit(LiveEvent("job", JsonNull, null))
            runCurrent()
            advanceTimeBy(500)
            eventually { server.recorded().count { it.path == "/api/v1/health/states" } > before }
            val again = server.recorded().count { it.path == "/api/v1/health/states" }
            live.emit(LiveEvent("somethingelse", JsonNull, null))
            runCurrent()
            advanceTimeBy(500)
            assertEquals(again, server.recorded().count { it.path == "/api/v1/health/states" })
            live.emit(LiveEvent(LiveEvent.RESYNC, JsonNull, null))
            runCurrent()
            advanceTimeBy(500)
            eventually { server.recorded().count { it.path == "/api/v1/health/states" } > again }
            val third = server.recorded().count { it.path == "/api/v1/health/states" }
            advanceTimeBy(61_000)
            eventually { server.recorded().count { it.path == "/api/v1/health/states" } > third }
            job.cancel()
        }

    @Test
    fun aMovingInstallIsPolledEveryTwoSecondsAndReloadsWhenTheVersionChanges() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            server.json("GET", "/api/v1/app/update/install", INSTALL.replace("\"phase\":\"idle\"", "\"phase\":\"restarting\""))
            val presenter = server.presenter()
            presenter.load()
            assertTrue(presenter.state.value.installMoving)
            val job = backgroundScope.launch { presenter.watch() }
            runCurrent()
            advanceTimeBy(500)
            eventually { server.recorded().count { it.path == "/api/v1/health/states" } >= 2 }
            val before = server.recorded().count { it.path == "/api/v1/app/update/install" }
            server.on("GET", "/api/v1/app/update/install") { throw IllegalStateException("restarting") }
            advanceTimeBy(2_100)
            eventually { server.recorded().count { it.path == "/api/v1/app/update/install" } > before }
            assertTrue(presenter.state.value.installMoving)
            server.json("GET", "/api/v1/app/update/install", INSTALL.replace("\"running\":\"1.3.0\"", "\"running\":\"1.4.0\""))
            val reads = server.recorded().count { it.path == "/api/v1/app/update" }
            advanceTimeBy(2_100)
            eventually { !presenter.state.value.installMoving }
            advanceTimeBy(500)
            eventually { server.recorded().count { it.path == "/api/v1/app/update" } > reads }
            job.cancel()
        }
}

class ApiErrorTest {
    @Test
    fun readsServerErrorFields() =
        runTest {
            val server = FakeServer()
            server.statusRoutes()
            val partOff = """{"error":"this part is off","part":"media","text":"Movies is off."}"""
            server.json("GET", "/api/v1/health/states", partOff, 409)
            server.json("GET", "/api/v1/schedules", partOff, 409)
            val presenter = server.presenter()
            presenter.load()
            val error = presenter.state.value.status.error
            assertEquals(ApiErrorCode.PART_OFF, error?.code)
            assertEquals("media", error?.part)
            assertEquals("Movies is off.", error?.text)
        }

    @Test
    fun offlineIsReported() =
        runTest {
            val server = FakeServer()
            server.on("GET", "/api/v1/health/states") { throw IllegalStateException("Unable to resolve host") }
            server.on("GET", "/api/v1/schedules") { throw IllegalStateException("Unable to resolve host") }
            val presenter = server.presenter()
            presenter.load()
            assertEquals(
                ApiErrorCode.OFFLINE,
                presenter.state.value.status.error
                    ?.code,
            )
        }
}
