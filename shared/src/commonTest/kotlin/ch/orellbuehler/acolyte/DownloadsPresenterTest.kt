package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.api.model.TorrentUpload
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.downloads.DecideAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadBadges
import ch.orellbuehler.acolyte.feature.downloads.DownloadKindFilter
import ch.orellbuehler.acolyte.feature.downloads.DownloadsBadgeKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadsEmptyKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadsPresenter
import ch.orellbuehler.acolyte.feature.downloads.DownloadsSub
import ch.orellbuehler.acolyte.feature.downloads.DownloadsTab
import ch.orellbuehler.acolyte.feature.downloads.NoticeKind
import ch.orellbuehler.acolyte.feature.downloads.StatusWord
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.live.LiveStatus
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountSnapshot
import ch.orellbuehler.acolyte.session.SignInMethod
import io.ktor.client.engine.mock.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal fun FakeServer.recorded(): List<Recorded> {
    repeat(20) {
        try {
            return requests.toList()
        } catch (_: ConcurrentModificationException) {
        }
    }
    return emptyList()
}

internal suspend fun TestScope.eventually(check: () -> Boolean) {
    repeat(600) {
        runCurrent()
        if (check()) return
        withContext(Dispatchers.Default) { delay(10) }
    }
    assertTrue(check())
}

internal fun downloadsAccount(
    role: String = "owner",
    perms: List<String> = listOf("ask", "approve", "manage_library", "settings"),
    id: Long = 1,
    maxRating: String = "",
): Account {
    val parts = listOf("movies", "shows", "music", "subtitles", "wishlist", "sources").map { PartView(part = it, on = true) }
    val me = Me(id = id, name = "sam", role = role, perms = perms, maxRating = maxRating)
    return Account.from(BASE, SignInMethod.PASSWORD, AccountSnapshot(me, Parts(parts)))
}

class DownloadsPresenterTest {
    private val view =
        """{"downloads":[
            {"id":12,"state":"downloading","phase":"downloading","progress":0.5,"speed":2000000,"active":true,"name":"Heat.1995.1080p",
             "title":"Heat","year":1995,"title_id":40,"kind":"movie","can_act":true,"protocol":"torrent","downloader_kind":"qbittorrent",
             "downloader_id":2,"hash":"aaa","live_key":"2:aaa","size":1000,"updated_at":"2026-11-20T10:00:00Z"},
            {"id":11,"state":"finished","phase":"done","progress":1,"name":"Lost.S01","title":"Lost","kind":"show","can_act":true,
             "downloader_id":3,"protocol":"usenet","downloader_kind":"sabnzbd","updated_at":"2026-11-19T10:00:00Z"},
            {"id":10,"state":"failed","phase":"import_refused","name":"Bad","title":"Bad","kind":"movie","can_act":true,"refused_why":"wrong file"},
            {"id":0,"client_item":true,"outside":true,"name":"Linux.iso","phase":"waiting","state":"queued","downloader_id":2,"hash":"bbb",
             "live_key":"2:bbb","protocol":"torrent","can_act":true,"queue_pos":1,"extra":"ignored"}
        ],"totals":{"speed":2000000,"up_speed":0},"clients":[{"id":2,"name":"qBit","health":"stale","stale":true}],
        "outside_total":5,"next_cursor":"","outside_rest":{"2":{"protocol":"torrent","stages":{"seeding":4}}},"revs":{"2":5},"boot":"b1"}"""

    private val decisions =
        """{"items":[{"slot_id":70,"title_id":40,"title":"Heat","year":1995,"version":"Main","versions":1,"why":"conflict",
            "reason":"Edition is unknown; confirm the edition.","has_copy":false,"since":"2026-11-20T10:00:00Z","kind":"movie","download_id":12},
            {"slot_id":71,"title_id":41,"title":"Lost","why":"ran_out","reason":"No other Release passed.","kind":"show",
             "episode":{"slot_id":71,"season":1,"episode":2,"label":"S01E02","name":"Pilot"}}],"count":2}"""

    private fun FakeServer.downloadRoutes() {
        json("GET", "/api/v1/downloads/view", view)
        json(
            "GET",
            "/api/v1/imports",
            """{"imports":[{"ID":3,"DownloadID":10,"State":"failed","Result":[{"outcome":"failed","error":"disk full"}]}]}""",
        )
        json("GET", "/api/v1/needs-decision", decisions)
        json("GET", "/api/v1/titles", """{"titles":[{"id":40,"kind":"movie","name":"Heat","artwork":["/api/v1/artwork/heat.jpg"]}]}""")
        json(
            "GET",
            "/api/v1/downloaders",
            """{"downloaders":[{"id":2,"name":"qBit","protocol":"torrent","kind":"qbittorrent","writable":true}]}""",
        )
        json("GET", "/api/v1/settings/downloaders", """{"remove_delete_files":false,"remove_turn_down":true,"remove_search_again":true}""")
        json(
            "GET",
            "/api/v1/downloads/clients",
            """{"clients":[{"id":2,"name":"qBit","controls":true,"status":{"free_space":5000,"speed_limit":0}}]}""",
        )
        json("GET", "/api/v1/downloads/matches", """{"matches":[],"counts":{},"pending":0}""")
    }

    private class Setup(
        val server: FakeServer,
        val presenter: DownloadsPresenter,
        val live: MutableSharedFlow<LiveEvent>,
        val badges: DownloadBadges,
        val status: MutableStateFlow<LiveStatus>,
    )

    private fun setup(account: Account = downloadsAccount()): Setup {
        val server = FakeServer()
        server.downloadRoutes()
        val live = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 16)
        val client = server.client(listOf(FakeServer.SESSION), csrf = "token")
        val api = Api({ client }, live)
        val badges = DownloadBadges(api, { account })
        val status = MutableStateFlow(LiveStatus.LIVE)
        return Setup(server, DownloadsPresenter(api, status, badges, { account }), live, badges, status)
    }

    @Test
    fun loadsTheManagerView() =
        runTest {
            val s = setup()
            s.presenter.load()
            val state = s.presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertTrue(state.manager)
            assertEquals(5, state.items.size)
            assertEquals(listOf("d12", "d11", "d10", "c2:bbb", "s71"), state.items.map { it.key })
            val heat = state.items[0]
            assertEquals(StatusWord.DOWNLOADING, heat.word)
            assertEquals(StatusWord.REVIEW, state.items[4].word)
            assertEquals("/api/v1/artwork/heat.jpg", heat.poster)
            assertEquals(1, heat.decisions.size)
            assertEquals("Heat (1995)", heat.name)
            assertTrue(DownloadAction.PAUSE in heat.actions)
            val refused = state.items[2]
            assertEquals("disk full", refused.detail?.text)
            assertEquals(2, state.decisions.size)
            assertEquals(1, state.addTargets.size)
            assertEquals(1, state.notes.size)
            assertEquals(5000L, state.statusLine.free)
            assertEquals(4, state.moreOutside)
            val all = state.tabs.first { it.tab == DownloadsTab.ALL }.count
            assertEquals(9, all)
            assertEquals("limit=100", s.server.request("GET", "/api/v1/downloads/view").query)
            assertEquals("brief=1", s.server.request("GET", "/api/v1/titles").query)
            assertEquals(2, s.badges.state.value.active)
            assertEquals(25, s.badges.state.value.percent)
            assertEquals(2, s.badges.state.value.decide)
            assertEquals(DownloadsBadgeKind.ACTIVE, s.badges.state.value.kind)
        }

    @Test
    fun filtersTabsAndSearch() =
        runTest {
            val s = setup()
            s.presenter.load()
            s.presenter.show("problems.decide")
            assertEquals(DownloadsTab.PROBLEMS, s.presenter.state.value.filter.tab)
            assertEquals(DownloadsSub.DECIDE, s.presenter.state.value.filter.sub)
            assertEquals(
                listOf("s71"),
                s.presenter.state.value.items
                    .map { it.key },
            )
            s.presenter.setTab(DownloadsTab.ALL)
            s.presenter.setQuery("linux")
            assertEquals(
                listOf("c2:bbb"),
                s.presenter.state.value.items
                    .map { it.key },
            )
            s.presenter.setQuery("nothing here")
            assertEquals(
                DownloadsEmptyKind.NO_MATCHES,
                s.presenter.state.value.empty
                    ?.kind,
            )
            s.presenter.setQuery("")
            s.presenter.setKind(DownloadKindFilter.SHOW)
            assertEquals(
                listOf("d11", "s71"),
                s.presenter.state.value.items
                    .map { it.key },
            )
            s.presenter.setTab(DownloadsTab.WAITING)
            assertEquals(
                DownloadsEmptyKind.NO_KIND,
                s.presenter.state.value.empty
                    ?.kind,
            )
        }

    @Test
    fun watchersSeeNoActions() =
        runTest {
            val s = setup(downloadsAccount(role = "member", perms = listOf("ask")))
            s.presenter.load()
            val state = s.presenter.state.value
            assertFalse(state.manager)
            assertTrue(state.items.all { it.actions.isEmpty() && !it.selectable })
            assertTrue(s.server.requests.none { it.path == "/api/v1/imports" || it.path == "/api/v1/needs-decision" })
            assertEquals(4, state.items.size)
        }

    @Test
    fun actsOnOwnRowsAndClientItems() =
        runTest {
            val s = setup()
            s.server.json("POST", "/api/v1/downloads/12/pause", """{"ok":true}""")
            s.server.json("POST", "/api/v1/downloaders/2/bulk", """{"ok":true,"count":1}""")
            s.presenter.load()
            s.presenter.act("d12", DownloadAction.PAUSE)
            assertEquals("token", s.server.request("POST", "/api/v1/downloads/12/pause").headers["X-CSRF-Token"])
            assertEquals(
                NoticeKind.PAUSED,
                s.presenter.state.value.notice
                    ?.kind,
            )
            s.presenter.act("c2:bbb", DownloadAction.MOVE_TOP)
            assertEquals("""{"action":"top","hashes":["bbb"]}""", s.server.request("POST", "/api/v1/downloaders/2/bulk").body)
            assertEquals(
                NoticeKind.MOVED_TOP,
                s.presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun reportsFailedActions() =
        runTest {
            val s = setup()
            s.server.json("POST", "/api/v1/downloads/12/pause", """{"error":"this Downloader is read only here"}""", 409)
            s.presenter.load()
            s.presenter.act("d12", DownloadAction.PAUSE)
            assertEquals(
                "this Downloader is read only here",
                s.presenter.state.value.actionError
                    ?.message,
            )
            assertTrue(
                s.presenter.state.value.busy
                    .isEmpty(),
            )
        }

    @Test
    fun removesWithTheDefaults() =
        runTest {
            val s = setup()
            s.server.json("POST", "/api/v1/downloads/12/remove", """{"id":12,"state":"cleaned"}""")
            s.server.json("POST", "/api/v1/downloaders/2/bulk", """{"ok":true,"count":1}""")
            s.presenter.load()
            val plan = assertNotNull(s.presenter.removePlan(listOf("d12")))
            assertTrue(plan.showTurnDown)
            assertFalse(plan.deleteFiles)
            assertTrue(plan.turnDown)
            val failed = s.presenter.remove(plan, deleteFiles = true, turnDown = true, searchAgain = true)
            assertTrue(failed.isEmpty())
            assertEquals(
                """{"delete_files":true,"turn_down":true,"search_again":true}""",
                s.server.request("POST", "/api/v1/downloads/12/remove").body,
            )
            val many = assertNotNull(s.presenter.removePlan(listOf("d12", "c2:bbb")))
            assertFalse(many.showTurnDown)
            assertEquals(2, many.keys.size)
            s.presenter.remove(many, deleteFiles = false, turnDown = false, searchAgain = false)
            assertEquals(
                """{"action":"remove","hashes":["bbb"],"delete_files":false}""",
                s.server.request("POST", "/api/v1/downloaders/2/bulk").body,
            )
            assertEquals(
                NoticeKind.REMOVED,
                s.presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun keepsOutsideTorrentsWithoutSettings() =
        runTest {
            val s = setup(downloadsAccount(role = "admin", perms = listOf("manage_library")))
            s.presenter.load()
            val plan = assertNotNull(s.presenter.removePlan(listOf("d12", "c2:bbb")))
            assertEquals(listOf("d12"), plan.keys)
            assertEquals(1, plan.blocked)
        }

    @Test
    fun decidesAndSetsEditions() =
        runTest {
            val s = setup()
            s.server.json("POST", "/api/v1/needs-decision/71/decide", """{"job_id":5,"job_ids":[5],"action":"search"}""", 202)
            s.server.json(
                "PUT",
                "/api/v1/downloads/12/edition",
                """{"download_id":12,"edition":"Theatrical","confirmed":true,"job_id":9}""",
            )
            s.presenter.load()
            val lost = s.presenter.state.value.decisions[1]
            s.presenter.decide(lost, DecideAction.SEARCH, pack = false)
            assertEquals(
                """{"action":"search","pack":false,"replace":false}""",
                s.server.request("POST", "/api/v1/needs-decision/71/decide").body,
            )
            assertEquals(
                NoticeKind.SEARCHING,
                s.presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                "Lost S01E02 Pilot",
                s.presenter.state.value.notice
                    ?.text,
            )
            val heat = s.presenter.state.value.decisions[0]
            assertTrue(s.presenter.setEdition(heat, "  Theatrical "))
            assertEquals(
                """{"edition":"Theatrical","confirm":true,"retry":true}""",
                s.server.request("PUT", "/api/v1/downloads/12/edition").body,
            )
        }

    @Test
    fun appliesLivePatchesAndRereadsOnEvents() =
        runTest {
            val s = setup()
            s.presenter.load()
            val job = backgroundScope.launch { s.presenter.watch() }
            runCurrent()
            val patch = """{"kind":"patch","boot":"b1","client":2,"fromRev":5,"toRev":6,"upsert":[{"key":"2:aaa","progress":0.75}]}"""
            s.live.emit(LiveEvent("dl", ApiJson.lenient.parseToJsonElement(patch), "b1-1"))
            runCurrent()
            assertEquals(
                0.75,
                s.presenter.state.value.items
                    .first { it.key == "d12" }
                    .row.progress,
            )
            val before = s.server.recorded().count { it.path == "/api/v1/downloads/view" }
            s.live.emit(
                LiveEvent("download", ApiJson.lenient.parseToJsonElement("""{"ID":12,"State":"failed","StateReason":"no seeds"}"""), null),
            )
            runCurrent()
            assertEquals(
                NoticeKind.DOWNLOAD_FAILED,
                s.presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                "no seeds",
                s.presenter.state.value.notice
                    ?.text,
            )
            advanceTimeBy(500)
            eventually { s.server.recorded().count { it.path == "/api/v1/downloads/view" } > before }
            val gap = """{"kind":"patch","boot":"b1","client":2,"fromRev":9,"toRev":10}"""
            s.live.emit(LiveEvent("dl", ApiJson.lenient.parseToJsonElement(gap), null))
            eventually { s.server.recorded().any { it.path == "/api/v1/downloads/view" && it.query == "limit=100&client=2" } }
            job.cancel()
        }

    @Test
    fun pollsWhenTheStreamIsDown() =
        runTest {
            val s = setup()
            s.status.value = LiveStatus.RECONNECTING
            s.presenter.load()
            val job = backgroundScope.launch { s.presenter.watch() }
            runCurrent()
            val before = s.server.recorded().count { it.path == "/api/v1/downloads/view" }
            advanceTimeBy(10_500)
            eventually { s.server.recorded().count { it.path == "/api/v1/downloads/view" } > before }
            assertTrue(s.server.recorded().none { it.path == "/api/v1/live/interest" })
            s.status.value = LiveStatus.LIVE
            advanceTimeBy(30_500)
            eventually { s.server.recorded().any { it.path == "/api/v1/live/interest" } }
            job.cancel()
        }

    @Test
    fun selectsAndActsInBulk() =
        runTest {
            val s = setup()
            s.server.json("POST", "/api/v1/downloaders/2/bulk", """{"ok":true,"count":2}""")
            s.presenter.load()
            s.presenter.toggleSelect("d12")
            s.presenter.toggleSelect("c2:bbb")
            s.presenter.toggleSelect("s71")
            assertEquals(setOf("d12", "c2:bbb"), s.presenter.state.value.selection)
            assertTrue(DownloadAction.RECHECK in s.presenter.selectionActions())
            s.presenter.bulk(DownloadAction.RECHECK)
            assertEquals("""{"action":"recheck","hashes":["aaa","bbb"]}""", s.server.request("POST", "/api/v1/downloaders/2/bulk").body)
            assertEquals(
                NoticeKind.BULK_DONE,
                s.presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                2,
                s.presenter.state.value.notice
                    ?.count,
            )
            assertTrue(
                s.presenter.state.value.selection
                    .isEmpty(),
            )
        }

    @Test
    fun addsTorrentLinksAndFiles() =
        runTest {
            val s = setup()
            var type = ""
            var sent = ""
            s.server.on("POST", "/api/v1/downloaders/2/torrents") { request ->
                type =
                    request.body.contentType
                        ?.withoutParameters()
                        ?.toString()
                        .orEmpty()
                sent = request.body.toByteArray().decodeToString()
                respondJson("""{"added":1,"failed":0,"pending":0,"duplicates":0}""")
            }
            s.presenter.load()
            assertEquals(listOf("nope"), s.presenter.addTorrents(2, "nope", "", "", false))
            assertTrue(s.presenter.addTorrents(2, "magnet:?xt=urn:btih:abc", "tv", "", false).isEmpty())
            assertEquals("application/json", type)
            assertTrue(sent.contains("\"links\":[\"magnet:?xt=urn:btih:abc\"]"), sent)
            val file = TorrentUpload("show.torrent", "d8:announce".encodeToByteArray())
            assertTrue(s.presenter.addTorrentFiles(2, "", listOf(file), "tv", "new", true).isEmpty())
            assertEquals("multipart/form-data", type)
            assertTrue(sent.contains("name=\"torrents\"; filename=\"show.torrent\"") && sent.contains("d8:announce"), sent)
            assertTrue(sent.contains("name=\"paused\"") && sent.contains("true"), sent)
            assertEquals(
                NoticeKind.ADDED,
                s.presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun resolvesTitleLinks() =
        runTest {
            val s = setup()
            s.server.json("GET", "/api/v1/titles/40", """{"id":40,"kind":"movie","name":"Heat","ids":{"tmdb":"949","imdb":"tt0113277"}}""")
            assertEquals("949", s.presenter.titleLink(40)?.tmdb)
            assertEquals("movie", s.presenter.titleLink(40)?.kind)
            assertEquals(1, s.server.requests.count { it.path == "/api/v1/titles/40" })
        }
}
