package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.DownloaderInfo
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderPath
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderForm
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderHealthState
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderIssue
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderKeyNote
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderKind
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderKindField
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderMessageKind
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderNumber
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderRulesIssue
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderRulesPresenter
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderSwitch
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloadersPresenter
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.isAbsolutePath
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.samePathPair
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.testTag
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.validAddress
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsDownloadersTest {
    private val qbit =
        """{"id":1,"name":"qBit","kind":"qbittorrent","protocol":"torrent","url":"http://nas:8090","username":"","secret":"••••••••",
            "category":"holyarrs","category_movies":"","category_shows":"","category_music":"","priority":1,"path_map":[],
            "allow_local_network":true,"writable":true,"writable_locked":false,"clear_finished":true,
            "after_seeding":{"remove":true,"ratio":2.0,"minutes":90,"delete_files":false},"category_after_import":"","own_only":false,
            "tags":["hd"],"health":{"state":"working","label":"Working","text":""},"active_downloads":2}"""

    private val sab =
        """{"id":2,"name":"SAB","kind":"sabnzbd","protocol":"usenet","url":"http://nas:8080","secret":"••••••••","category":"movies",
            "priority":2,"writable":true,"clear_finished":true,"health":{"state":"resting","label":"Resting","text":"Waits until 12:00 UTC."}}"""

    private val plugin =
        """{"id":3,"name":"Mystery","kind":"plugin-torrent","protocol":"torrent","url":"plugin:x","health":{"state":"strange","label":"Odd","text":""}}"""

    private fun info(json: String): DownloaderInfo = Json { ignoreUnknownKeys = true }.decodeFromString(DownloaderInfo.serializer(), json)

    private fun presenter(
        server: FakeServer,
        role: Role = Role.OWNER,
        perms: List<String> = emptyList(),
    ) = SettingsDownloadersPresenter(server.api()) { testAccount(role, perms) }

    private fun FakeServer.list(vararg rows: String) = json("GET", "/api/v1/downloaders", """{"downloaders":[${rows.joinToString(",")}]}""")

    private fun bodyOf(request: Recorded): JsonObject = Json.parseToJsonElement(request.body.orEmpty()).jsonObject

    @Test
    fun pageNeedsSettings() {
        assertTrue(testAccount(Role.ADMIN, listOf("settings")).mayOpen(AcquisitionPage.DOWNLOADERS))
        assertFalse(testAccount(Role.ADMIN, listOf("manage_library")).mayOpen(AcquisitionPage.DOWNLOADERS))
    }

    @Test
    fun newFormDefaultsFollowTheWebForm() {
        val sabForm = SettingsDownloaderForm.starting(SettingsDownloaderKind.SABNZBD)
        assertEquals("SABnzbd", sabForm.name)
        assertEquals("movies", sabForm.category)
        assertEquals("http://", sabForm.url)
        val torrent = SettingsDownloaderForm.starting(SettingsDownloaderKind.DELUGE)
        assertEquals("holyarrs", torrent.category)
        assertTrue(torrent.allowLocalNetwork && torrent.writable && torrent.clearFinished)
        assertFalse(torrent.ownOnly || torrent.seedRemove)
        assertEquals("1", torrent.priority)
    }

    @Test
    fun pickingAnotherAppKeepsAddressLocalNetworkAndWritable() {
        val typed =
            SettingsDownloaderForm
                .starting(
                    SettingsDownloaderKind.QBITTORRENT,
                ).copy(url = "http://nas:1", allowLocalNetwork = false, writable = false, name = "x")
        val next = SettingsDownloaderForm.picking(typed, SettingsDownloaderKind.TRANSMISSION)
        assertEquals("http://nas:1", next.url)
        assertFalse(next.allowLocalNetwork)
        assertFalse(next.writable)
        assertEquals("Transmission", next.name)
        val untouched = SettingsDownloaderForm.picking(SettingsDownloaderForm.blank(), SettingsDownloaderKind.NZBGET)
        assertEquals("http://", untouched.url)
    }

    @Test
    fun bodyFollowsTheKind() {
        val qb =
            SettingsDownloaderForm
                .starting(
                    SettingsDownloaderKind.QBITTORRENT,
                ).copy(name = " qB ", url = " http://a:1 ", secret = "key", username = "ana")
        var body = qb.body()
        assertEquals("qB", body.name)
        assertEquals("http://a:1", body.url)
        assertEquals("", body.username)
        assertEquals("", body.categoryAfterImport)
        assertNull(body.clearFinished)
        assertEquals(false, body.ownOnly)
        assertNotNull(body.afterSeeding)
        body = qb.copy(login = true).body()
        assertEquals("ana", body.username)
        val trans = SettingsDownloaderForm.starting(SettingsDownloaderKind.TRANSMISSION).copy(username = " u ").body()
        assertEquals("u", trans.username)
        assertNull(trans.categoryAfterImport)
        val deluge = SettingsDownloaderForm.starting(SettingsDownloaderKind.DELUGE).copy(username = "ignored").body()
        assertEquals("", deluge.username)
        val sabBody = SettingsDownloaderForm.starting(SettingsDownloaderKind.SABNZBD).copy(clearFinished = false).body()
        assertEquals(false, sabBody.clearFinished)
        assertNull(sabBody.ownOnly)
        assertNull(sabBody.afterSeeding)
        val nzb = SettingsDownloaderForm.starting(SettingsDownloaderKind.NZBGET).body()
        assertNull(nzb.afterSeeding)
        assertEquals(1, nzb.priority)
    }

    @Test
    fun seedingGoalsOnlyForQbittorrentAndRtorrent() {
        val base =
            SettingsDownloaderForm
                .starting(
                    SettingsDownloaderKind.QBITTORRENT,
                ).copy(seedRemove = true, seedRatio = "1.5", seedHours = "0.5", seedDeleteFiles = true)
        val seeding = base.body().afterSeeding
        assertNotNull(seeding)
        assertEquals(1.5, seeding.ratio)
        assertEquals(30, seeding.minutes)
        assertTrue(seeding.deleteFiles)
        assertEquals(
            1,
            base
                .copy(seedHours = "0.001")
                .body()
                .afterSeeding
                ?.minutes,
        )
        assertEquals(
            30,
            base
                .copy(kind = "rtorrent")
                .body()
                .afterSeeding
                ?.minutes,
        )
        val trans = base.copy(kind = "transmission").body().afterSeeding
        assertNull(trans?.ratio)
        assertNull(trans?.minutes)
        assertTrue(trans?.remove == true)
        assertFalse(
            base
                .copy(seedRemove = false)
                .body()
                .afterSeeding
                ?.deleteFiles == true,
        )
        assertNull(
            base
                .copy(seedRatio = "0", seedHours = "")
                .body()
                .afterSeeding
                ?.ratio,
        )
    }

    @Test
    fun emptyPathPairsAreDroppedAndLinkIsKept() {
        val form =
            SettingsDownloaderForm.starting(SettingsDownloaderKind.DELUGE).copy(
                pathMap =
                    listOf(
                        SettingsDownloaderPath("", ""),
                        SettingsDownloaderPath(" /data ", "/dl ", true),
                    ),
            )
        assertEquals(listOf(SettingsDownloaderPath("/data", "/dl", true)), form.body().pathMap)
        assertTrue(form.copy(pathMap = form.pathMap + SettingsDownloaderPath("/only", "")).problem(SettingsDownloaderIssue.PATH))
        assertFalse(form.problem(SettingsDownloaderIssue.PATH))
    }

    @Test
    fun savedDownloaderFillsTheFormAndChangesAreDetected() {
        val form = SettingsDownloaderForm.fromInfo(info(qbit))
        assertEquals("2", form.seedRatio)
        assertEquals("1.5", form.seedHours)
        assertEquals(listOf("hd"), form.tags)
        assertFalse(form.login)
        assertFalse(form.changed)
        assertNull(form.testBody())
        val typed = form.copy(url = "http://other:1", secret = "new")
        assertTrue(typed.changed)
        assertEquals("new", typed.testBody()?.secret)
        assertEquals("••••••••", form.body().secret)
    }

    @Test
    fun validationMirrorsTheServer() {
        val form = SettingsDownloaderForm.starting(SettingsDownloaderKind.SABNZBD).copy(url = "http://nas:8080", secret = "k")
        assertTrue(form.canSave)
        assertTrue(form.copy(name = "  ").issues.contains(SettingsDownloaderIssue.NAME))
        assertTrue(form.copy(name = "x".repeat(81)).issues.contains(SettingsDownloaderIssue.NAME))
        assertTrue(form.copy(url = "nas:8080").issues.contains(SettingsDownloaderIssue.URL))
        assertTrue(form.copy(category = "a/b").issues.contains(SettingsDownloaderIssue.CATEGORY))
        assertTrue(form.copy(category = "").issues.contains(SettingsDownloaderIssue.CATEGORY))
        assertTrue(form.copy(priority = "51").issues.contains(SettingsDownloaderIssue.PRIORITY))
        assertTrue(form.copy(priority = "0").issues.contains(SettingsDownloaderIssue.PRIORITY))
        assertTrue(form.copy(secret = "").issues.contains(SettingsDownloaderIssue.SECRET))
        assertTrue(form.copy(categoryShows = "a\\b").issues.contains(SettingsDownloaderIssue.KIND_CATEGORY))
        assertFalse(form.copy(id = 4, secret = "").issues.contains(SettingsDownloaderIssue.SECRET))
    }

    @Test
    fun categoryRulesPerKind() {
        val deluge = SettingsDownloaderForm.starting(SettingsDownloaderKind.DELUGE).copy(url = "http://nas:8112")
        assertTrue(deluge.copy(category = "Movies").issues.contains(SettingsDownloaderIssue.CATEGORY_LABEL))
        assertFalse(deluge.copy(category = "movies_1.x-y").issues.contains(SettingsDownloaderIssue.CATEGORY_LABEL))
        val aria = SettingsDownloaderForm.starting(SettingsDownloaderKind.ARIA2).copy(url = "http://nas:6800/jsonrpc")
        assertTrue(aria.copy(category = "..").issues.contains(SettingsDownloaderIssue.CATEGORY_FOLDER))
        assertTrue(aria.copy(category = "a:b").issues.contains(SettingsDownloaderIssue.CATEGORY_FOLDER))
        assertTrue(aria.copy(category = "a|b").issues.contains(SettingsDownloaderIssue.CATEGORY_FOLDER))
        assertTrue(aria.copy(category = "movies").issues.isEmpty())
    }

    @Test
    fun qbittorrentSignInRules() {
        val form = SettingsDownloaderForm.starting(SettingsDownloaderKind.QBITTORRENT).copy(url = "http://nas:8090")
        assertTrue(form.issues.contains(SettingsDownloaderIssue.SECRET))
        assertTrue(form.copy(login = true, secret = "pw").issues.contains(SettingsDownloaderIssue.USERNAME))
        assertTrue(form.copy(login = true, secret = "pw", username = "ana").issues.isEmpty())
        assertTrue(form.copy(secret = "key", categoryAfterImport = "holyarrs").issues.contains(SettingsDownloaderIssue.AFTER_IMPORT_SAME))
        assertTrue(form.copy(secret = "key", categoryAfterImport = "done").issues.isEmpty())
        val saved = SettingsDownloaderForm.fromInfo(info(qbit))
        assertTrue(saved.issues.isEmpty())
        val switched = saved.copy(login = true, username = "ana", secret = "")
        assertTrue(switched.issues.contains(SettingsDownloaderIssue.SECRET))
    }

    @Test
    fun seedingGoalValidation() {
        val form = SettingsDownloaderForm.fromInfo(info(qbit))
        assertTrue(form.copy(seedRatio = "1001").issues.contains(SettingsDownloaderIssue.RATIO))
        assertTrue(form.copy(seedRatio = "0").issues.contains(SettingsDownloaderIssue.RATIO))
        assertTrue(form.copy(seedHours = "9000").issues.contains(SettingsDownloaderIssue.HOURS))
        assertTrue(form.copy(seedHours = "").issues.isEmpty())
        assertFalse(form.copy(kind = "transmission", seedRatio = "5000").issues.contains(SettingsDownloaderIssue.RATIO))
    }

    @Test
    fun addressPathAndPairHelpers() {
        assertTrue(validAddress("http://192.168.1.10:8090"))
        assertTrue(validAddress("HTTPS://host/path/"))
        assertTrue(validAddress("http://[::1]:8080"))
        assertFalse(validAddress("http://"))
        assertFalse(validAddress("http://:8080"))
        assertFalse(validAddress("http://ana:pw@host"))
        assertFalse(validAddress("http://host/?a=1"))
        assertFalse(validAddress("ftp://host"))
        assertTrue(isAbsolutePath("/data"))
        assertTrue(isAbsolutePath("D:\\dl"))
        assertTrue(isAbsolutePath("c:/dl"))
        assertFalse(isAbsolutePath("data"))
        assertFalse(isAbsolutePath(""))
        assertTrue(samePathPair(SettingsDownloaderPath("/data/", "/data")))
        assertTrue(samePathPair(SettingsDownloaderPath("D:\\Dl", "d:/dl/")))
        assertFalse(samePathPair(SettingsDownloaderPath("/a", "/b")))
        assertFalse(samePathPair(SettingsDownloaderPath("", "")))
        assertEquals("qBittorrent v5.0.4", testTag("qBittorrent", "5.0.4.1"))
        assertEquals("SABnzbd", testTag("SABnzbd", ""))
    }

    @Test
    fun listLoadsRowsAndKeepsThePageWhenAttemptsAreRefused() =
        runTest {
            val server = FakeServer()
            server.list(qbit, sab, plugin)
            server.json(
                "GET",
                "/api/v1/downloaders/1/attempts",
                """{"attempts":[{"id":5,"downloader_id":1,"release_id":9,"slot_id":4,"state":"pending","created_at":"2026-01-01T00:00:00Z"},
                    {"id":6,"downloader_id":1,"release_id":9,"slot_id":4,"state":"abandoned"}]}""",
            )
            server.json("GET", "/api/v1/downloaders/2/attempts", """{"error":"needs the admin scope"}""", 403)
            server.json("GET", "/api/v1/downloaders/3/attempts", """{"attempts":[]}""")
            val presenter = presenter(server)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.allowed && s.canEdit && s.owner)
            assertEquals(3, s.rows.size)
            val first = s.rows[0]
            assertEquals("qBittorrent", first.appName)
            assertEquals(listOf(5L), first.pending.map { it.id })
            assertEquals(SettingsDownloaderHealthState.WORKING, first.health)
            assertFalse(first.showHealthWord)
            assertEquals(Tone.OK, first.tone)
            val second = s.rows[1]
            assertEquals(SettingsDownloaderHealthState.RESTING, second.health)
            assertEquals(Tone.WAIT, second.tone)
            assertEquals(ApiErrorCode.FORBIDDEN, second.attemptError?.code)
            assertTrue(second.healthTextFails)
            val third = s.rows[2]
            assertFalse(third.canChange)
            assertEquals("plugin-torrent", third.appName)
            assertEquals(SettingsDownloaderHealthState.UNKNOWN, third.health)
            assertEquals("Odd", third.healthWord)
        }

    @Test
    fun adminWithoutSettingsIsTurnedAway() =
        runTest {
            val presenter = presenter(FakeServer(), Role.ADMIN, listOf("manage_library"))
            presenter.load()
            assertFalse(presenter.state.value.allowed)
        }

    @Test
    fun addingSavesThenTestsTheNewOne() =
        runTest {
            val server = FakeServer()
            server.list(qbit)
            server.json("GET", "/api/v1/downloaders/1/attempts", """{"attempts":[]}""")
            server.json("POST", "/api/v1/downloaders", sab, 201)
            server.json("GET", "/api/v1/downloaders/2/attempts", """{"attempts":[]}""")
            server.json(
                "POST",
                "/api/v1/downloaders/2/test",
                """{"ok":true,"ms":12,"text":"It answered: sabnzbd 4.3.1.","version":"4.3.1","health":{"state":"working","label":"Working","text":""}}""",
            )
            val presenter = presenter(server)
            presenter.load()
            server.list(qbit, sab)
            presenter.startAdd()
            presenter.pickKind("sabnzbd")
            presenter.setUrl("http://nas:8080")
            assertFalse(
                presenter.state.value.form!!
                    .canSave,
            )
            presenter.setSecret("apikey")
            presenter.setKindCategory(SettingsDownloaderKindField.SHOWS, "tv")
            presenter.addTag(" HD Only ")
            presenter.save()
            val sent = bodyOf(server.request("POST", "/api/v1/downloaders"))
            assertEquals("SABnzbd", sent["name"]?.jsonPrimitive?.content)
            assertEquals("sabnzbd", sent["kind"]?.jsonPrimitive?.content)
            assertEquals("apikey", sent["secret"]?.jsonPrimitive?.content)
            assertEquals("tv", sent["category_shows"]?.jsonPrimitive?.content)
            assertTrue(sent["clear_finished"]!!.jsonPrimitive.boolean)
            assertFalse(sent.containsKey("own_only"))
            assertFalse(sent.containsKey("after_seeding"))
            assertContains(sent["tags"].toString(), "hd-only")
            val s = presenter.state.value
            assertNull(s.form)
            assertEquals(SettingsDownloaderMessageKind.SAVED, s.message?.kind)
            assertEquals("SAB", s.message?.name)
            assertTrue(server.requests.any { it.method == "POST" && it.path == "/api/v1/downloaders/2/test" })
            assertNull(server.request("POST", "/api/v1/downloaders/2/test").body)
        }

    @Test
    fun aRefusedSaveShowsTheServerSentenceAndKeepsTheForm() =
        runTest {
            val server = FakeServer()
            server.list()
            server.json("POST", "/api/v1/downloaders", """{"error":"another Downloader has that name"}""", 409)
            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            presenter.pickKind("deluge")
            presenter.setUrl("http://nas:8112")
            presenter.save()
            val s = presenter.state.value
            assertNotNull(s.form)
            assertEquals(ApiErrorCode.CONFLICT, s.formError?.code)
            assertEquals("another Downloader has that name", s.formError?.message)
            assertFalse(s.saving)
        }

    @Test
    fun changingLoadsCategoriesAndFillsPicks() =
        runTest {
            val server = FakeServer()
            server.list(qbit)
            server.json("GET", "/api/v1/downloaders/1/attempts", """{"attempts":[]}""")
            server.json(
                "GET",
                "/api/v1/downloaders/1/categories",
                """{"listed":true,"categories":["movies","tv","music"],"picks":{"movie":"movies","show":"tv","artist":"music"}}""",
            )
            server.on("PUT", "/api/v1/downloaders/1") { respondJson(qbit) }
            server.json("POST", "/api/v1/downloaders/1/test", """{"ok":true,"version":"5.0.0","text":"It answered: qbittorrent 5.0.0."}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.startChange(1)
            var s = presenter.state.value
            assertEquals(true, s.categories.listed)
            assertEquals("movies", s.form?.categoryMovies)
            assertEquals("tv", s.form?.categoryShows)
            assertEquals("music", s.form?.categoryMusic)
            assertEquals(listOf("movies", "tv", "music"), s.categories.options(""))
            assertEquals(listOf("old", "movies", "tv", "music"), s.categories.options("old"))
            presenter.setPriority("7x")
            presenter.save()
            val sent = bodyOf(server.request("PUT", "/api/v1/downloaders/1"))
            assertEquals(7, sent["priority"]?.jsonPrimitive?.content?.toInt())
            assertEquals("••••••••", sent["secret"]?.jsonPrimitive?.content)
            assertEquals("", sent["category_after_import"]?.jsonPrimitive?.content)
            s = presenter.state.value
            assertNull(s.form)
        }

    @Test
    fun unreadableAppFallsBackToTextBoxes() =
        runTest {
            val server = FakeServer()
            server.list(sab)
            server.json("GET", "/api/v1/downloaders/2/attempts", """{"attempts":[]}""")
            server.json("GET", "/api/v1/downloaders/2/categories", """{"listed":false,"categories":[],"picks":{}}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.startChange(2)
            val s = presenter.state.value
            assertEquals(false, s.categories.listed)
            assertEquals("", s.form?.categoryMovies)
        }

    @Test
    fun aRefusedCategoryListAlsoFallsBack() =
        runTest {
            val server = FakeServer()
            server.list(sab)
            server.json("GET", "/api/v1/downloaders/2/attempts", """{"attempts":[]}""")
            server.json("GET", "/api/v1/downloaders/2/categories", """{"error":"needs the admin scope"}""", 403)
            val presenter = presenter(server)
            presenter.load()
            presenter.startChange(2)
            assertEquals(false, presenter.state.value.categories.listed)
        }

    @Test
    fun pluginKindsCannotBeChanged() =
        runTest {
            val server = FakeServer()
            server.list(plugin)
            server.json("GET", "/api/v1/downloaders/3/attempts", """{"attempts":[]}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.startChange(3)
            assertNull(presenter.state.value.form)
        }

    @Test
    fun typedTestSendsTheFormOnlyWhenItDiffers() =
        runTest {
            val server = FakeServer()
            server.list(sab)
            server.json("GET", "/api/v1/downloaders/2/attempts", """{"attempts":[]}""")
            server.json("GET", "/api/v1/downloaders/2/categories", """{"listed":false,"categories":[],"picks":{}}""")
            server.json(
                "POST",
                "/api/v1/downloaders/2/test",
                """{"ok":false,"text":"It did not answer.","category":"movies","can_make":true}""",
            )
            val presenter = presenter(server)
            presenter.load()
            presenter.startChange(2)
            presenter.testForm()
            assertNull(server.request("POST", "/api/v1/downloaders/2/test").body)
            presenter.setUrl("http://other:8080")
            presenter.testForm()
            val body = bodyOf(server.request("POST", "/api/v1/downloaders/2/test"))
            assertEquals("http://other:8080", body["url"]?.jsonPrimitive?.content)
            val result = presenter.state.value.formResult
            assertNotNull(result)
            assertFalse(result.ok)
            assertEquals("movies", result.category)
            assertTrue(result.canMake)
            assertNotNull(presenter.state.value.form)
        }

    @Test
    fun rowTestShowsTheVersionTagAndTheMissingCategory() =
        runTest {
            val server = FakeServer()
            server.list(qbit, sab)
            server.json("GET", "/api/v1/downloaders/1/attempts", """{"attempts":[]}""")
            server.json("GET", "/api/v1/downloaders/2/attempts", """{"attempts":[]}""")
            server.json("POST", "/api/v1/downloaders/1/test", """{"ok":true,"version":"5.0.4","text":"It answered."}""")
            server.json(
                "POST",
                "/api/v1/downloaders/2/test",
                """{"ok":false,"text":"No category movies.","category":"movies","can_make":true}""",
            )
            server.on("POST", "/api/v1/downloaders/2/category") { respondJson("", 204) }
            val presenter = presenter(server)
            presenter.load()
            presenter.test(1)
            presenter.test(2)
            var s = presenter.state.value
            assertEquals("qBittorrent v5.0.4", s.rows[0].test?.tag)
            assertTrue(s.rows[1].failedTest)
            assertFalse(s.rows[1].showHealthText)
            assertEquals("movies", s.rows[1].test?.category)
            presenter.makeCategory(2, "movies")
            assertEquals("movies", bodyOf(server.request("POST", "/api/v1/downloaders/2/category"))["name"]?.jsonPrimitive?.content)
            s = presenter.state.value
            assertEquals(SettingsDownloaderMessageKind.CATEGORY_MADE, s.message?.kind)
            assertEquals("SABnzbd", s.message?.app)
        }

    @Test
    fun failedTestWithoutSentenceKeepsTheError() =
        runTest {
            val server = FakeServer()
            server.list(qbit)
            server.json("GET", "/api/v1/downloaders/1/attempts", """{"attempts":[]}""")
            server.json("POST", "/api/v1/downloaders/1/test", """{"error":"sign in with your password for this"}""", 403)
            val presenter = presenter(server)
            presenter.load()
            presenter.test(1)
            val test =
                presenter.state.value.rows[0]
                    .test
            assertNotNull(test)
            assertFalse(test.ok)
            assertEquals(ApiErrorCode.FORBIDDEN, test.error?.code)
        }

    @Test
    fun pauseResumeAndRemoveFailuresBecomeMessages() =
        runTest {
            val server = FakeServer()
            server.list(qbit)
            server.json("GET", "/api/v1/downloaders/1/attempts", """{"attempts":[]}""")
            server.json("POST", "/api/v1/downloaders/1/pause", qbit.replace("working", "paused"))
            server.json("POST", "/api/v1/downloaders/1/resume", """{"error":"boom"}""", 500)
            server.json("DELETE", "/api/v1/downloaders/1", """{"error":"this Downloader still has active downloads; pause it"}""", 409)
            val presenter = presenter(server)
            presenter.load()
            presenter.pause(1)
            assertNull(presenter.state.value.message)
            presenter.resume(1)
            assertEquals(
                SettingsDownloaderMessageKind.RESUME_FAILED,
                presenter.state.value.message
                    ?.kind,
            )
            presenter.remove(1)
            val s = presenter.state.value
            assertEquals(SettingsDownloaderMessageKind.REMOVE_FAILED, s.message?.kind)
            assertEquals("qBit", s.message?.name)
            assertEquals(ApiErrorCode.CONFLICT, s.message?.error?.code)
            assertEquals(2, s.messageCount)
            assertTrue(s.busy.isEmpty())
        }

    @Test
    fun removeReloadsTheList() =
        runTest {
            val server = FakeServer()
            server.list(qbit)
            server.json("GET", "/api/v1/downloaders/1/attempts", """{"attempts":[]}""")
            server.on("DELETE", "/api/v1/downloaders/1") { respondJson("", 204) }
            val presenter = presenter(server)
            presenter.load()
            server.list()
            presenter.remove(1)
            assertTrue(presenter.state.value.isEmpty)
        }

    @Test
    fun givingUpOnASendReloadsItsAttempts() =
        runTest {
            val server = FakeServer()
            server.list(qbit)
            server.json(
                "GET",
                "/api/v1/downloaders/1/attempts",
                """{"attempts":[{"id":5,"downloader_id":1,"release_id":9,"slot_id":4,"state":"pending"}]}""",
            )
            server.on("POST", "/api/v1/downloaders/1/attempts/5/abandon") { respondJson("", 204) }
            val presenter = presenter(server)
            presenter.load()
            assertEquals(
                1,
                presenter.state.value.rows[0]
                    .pending.size,
            )
            server.json("GET", "/api/v1/downloaders/1/attempts", """{"attempts":[{"id":5,"downloader_id":1,"state":"abandoned"}]}""")
            presenter.abandon(1, 5)
            val s = presenter.state.value
            assertEquals(0, s.rows[0].pending.size)
            assertEquals(SettingsDownloaderMessageKind.ABANDONED, s.message?.kind)
            assertEquals(0L, s.abandoning)
        }

    @Test
    fun tagsAreCleanedLikeTheServerDoes() =
        runTest {
            val presenter = presenter(FakeServer())
            presenter.startAdd()
            presenter.pickKind("deluge")
            presenter.addTag("  Kids Movies ")
            presenter.addTag("kids-movies")
            presenter.addTag("")
            presenter.addTag("x".repeat(60))
            assertEquals(
                listOf("kids-movies", "x".repeat(40)),
                presenter.state.value.form
                    ?.tags,
            )
            presenter.removeTag("kids-movies")
            assertEquals(
                1,
                presenter.state.value.form
                    ?.tags
                    ?.size,
            )
        }

    @Test
    fun qbittorrentWayChangeClearsTheSecret() =
        runTest {
            val presenter = presenter(FakeServer())
            presenter.startAdd()
            presenter.pickKind("qbittorrent")
            presenter.setSecret("typed")
            presenter.setLogin(true)
            presenter.setUsername("ana")
            presenter.setSecret("pw")
            presenter.setLogin(false)
            val form = presenter.state.value.form
            assertEquals("", form?.secret)
            assertEquals("", form?.username)
            assertFalse(form?.hasUsername == true)
        }

    @Test
    fun lockedWritableCannotBeSwitched() =
        runTest {
            val server = FakeServer()
            server.list(qbit.replace("\"writable_locked\":false", "\"writable_locked\":true"))
            server.json("GET", "/api/v1/downloaders/1/attempts", """{"attempts":[]}""")
            server.json("GET", "/api/v1/downloaders/1/categories", """{"listed":false}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.startChange(1)
            presenter.setWritable(false)
            assertEquals(
                true,
                presenter.state.value.form
                    ?.writable,
            )
        }

    @Test
    fun pathPairEditing() =
        runTest {
            val presenter = presenter(FakeServer())
            presenter.startAdd()
            presenter.pickKind("transmission")
            presenter.addPathPair()
            presenter.setPathTheirs(0, "/data")
            presenter.setPathOurs(0, "/downloads")
            presenter.setPathLink(0, true)
            presenter.addPathPair()
            presenter.removePathPair(1)
            assertEquals(
                listOf(SettingsDownloaderPath("/data", "/downloads", true)),
                presenter.state.value.form
                    ?.pathMap,
            )
        }

    @Test
    fun fetchKeyOnlyForOwnersAndReportsEachOutcome() =
        runTest {
            val server = FakeServer()
            server.list()
            val admin = presenter(server, Role.ADMIN, listOf("settings"))
            admin.load()
            admin.startAdd()
            admin.pickKind("sabnzbd")
            admin.setUrl("http://nas:8080")
            assertFalse(admin.state.value.canFetchKey)
            admin.fetchKey()
            assertTrue(server.requests.none { it.path == "/api/v1/migrate/key-lookup" })

            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            presenter.pickKind("sabnzbd")
            presenter.setUrl("http://nas:8080")
            assertTrue(presenter.state.value.canFetchKey)
            server.json("POST", "/api/v1/migrate/key-lookup", """{"key":"","need_login":true}""")
            presenter.fetchKey()
            assertEquals(SettingsDownloaderKeyNote.NEEDS_LOGIN, presenter.state.value.key.note)
            assertTrue(presenter.state.value.key.loginShown)
            presenter.setSabUser("ana")
            presenter.setSabPass("pw")
            server.json("POST", "/api/v1/migrate/key-lookup", """{"key":"","problem":"wrong_login"}""")
            presenter.fetchKey()
            assertEquals(SettingsDownloaderKeyNote.WRONG_LOGIN, presenter.state.value.key.note)
            val sent = bodyOf(server.request("POST", "/api/v1/migrate/key-lookup"))
            assertEquals("ana", sent["username"]?.jsonPrimitive?.content)
            assertEquals("pw", sent["password"]?.jsonPrimitive?.content)
            assertEquals("sabnzbd", sent["kind"]?.jsonPrimitive?.content)
            server.json("POST", "/api/v1/migrate/key-lookup", """{"key":"abc123"}""")
            presenter.fetchKey()
            val done = presenter.state.value
            assertEquals("abc123", done.form?.secret)
            assertEquals(SettingsDownloaderKeyNote.FETCHED, done.key.note)
            assertFalse(done.key.loginShown)
            server.json("POST", "/api/v1/migrate/key-lookup", """{"error":"owner only"}""", 403)
            presenter.fetchKey()
            assertEquals(SettingsDownloaderKeyNote.NOT_FOUND, presenter.state.value.key.note)
            assertEquals(
                "http://nas:8080/config/general/",
                presenter.state.value.form
                    ?.keyPageUrl,
            )
        }

    @Test
    fun cancelClosesTheForm() =
        runTest {
            val presenter = presenter(FakeServer())
            presenter.startAdd()
            assertNotNull(presenter.state.value.form)
            presenter.cancelForm()
            assertNull(presenter.state.value.form)
        }

    private val settingsJson =
        """{"stalled_after_hours":6,"stalled_act_hours":24,"remove_failed":true,"fail_dangerous":true,"replace_vanished":true,
            "grab_spacing_seconds":2,"try_next_downloader":true,"remove_delete_files":true,"remove_turn_down":false,
            "remove_search_again":true,"downloads_sync":"on","downloads_sab_outside":true,"downloads_auto_add":false,
            "dead_metadata_minutes":0,"crawl_kbps":0,"crawl_hours":0}"""

    private fun rules(server: FakeServer) = SettingsDownloaderRulesPresenter(server.api()) { testAccount(Role.OWNER, emptyList()) }

    @Test
    fun rulesCardLoadsEditsAndSaves() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/downloaders", settingsJson)
            server.on("PUT", "/api/v1/settings/downloaders") { respondJson((it.body as io.ktor.http.content.TextContent).text) }
            val presenter = rules(server)
            presenter.load()
            var s = presenter.state.value
            assertTrue(s.loaded && s.canEdit)
            assertFalse(s.dirty)
            assertEquals("6", s.number(SettingsDownloaderNumber.STALLED_AFTER))
            presenter.setSwitch(SettingsDownloaderSwitch.TURN_DOWN, true)
            presenter.setNumber(SettingsDownloaderNumber.SPACING, "1.5")
            presenter.setNumber(SettingsDownloaderNumber.STALLED_AFTER, "12abc")
            s = presenter.state.value
            assertTrue(s.dirty && s.canSave)
            assertEquals(12.0, s.draft.stalledAfterHours)
            assertTrue(s.flag(SettingsDownloaderSwitch.TURN_DOWN))
            presenter.save()
            s = presenter.state.value
            assertFalse(s.dirty)
            assertEquals(1, s.savedCount)
            val sent = bodyOf(server.request("PUT", "/api/v1/settings/downloaders"))
            assertEquals(1.5, sent["grab_spacing_seconds"]?.jsonPrimitive?.content?.toDouble())
            assertTrue(sent["remove_turn_down"]!!.jsonPrimitive.boolean)
            assertEquals(0.0, sent["dead_metadata_minutes"]?.jsonPrimitive?.content?.toDouble())
            assertFalse(sent.containsKey("downloads_sync"))
        }

    @Test
    fun rulesCardRefusesWhatTheServerWouldRefuse() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/downloaders", settingsJson)
            val presenter = rules(server)
            presenter.load()
            presenter.setNumber(SettingsDownloaderNumber.STALLED_AFTER, "30")
            assertEquals(SettingsDownloaderRulesIssue.ACT_SOONER, presenter.state.value.issue)
            assertFalse(presenter.state.value.canSave)
            presenter.setNumber(SettingsDownloaderNumber.STALLED_ACT, "800")
            assertEquals(SettingsDownloaderRulesIssue.ACT_RANGE, presenter.state.value.issue)
            presenter.setNumber(SettingsDownloaderNumber.STALLED_ACT, "40")
            assertNull(presenter.state.value.issue)
            presenter.setNumber(SettingsDownloaderNumber.STALLED_AFTER, "200")
            assertEquals(SettingsDownloaderRulesIssue.STALLED_AFTER_RANGE, presenter.state.value.issue)
            presenter.discard()
            presenter.setNumber(SettingsDownloaderNumber.SPACING, "61")
            assertEquals(SettingsDownloaderRulesIssue.SPACING_RANGE, presenter.state.value.issue)
            presenter.setNumber(SettingsDownloaderNumber.SPACING, "")
            assertEquals(SettingsDownloaderRulesIssue.NOT_A_NUMBER, presenter.state.value.issue)
            presenter.discard()
            assertFalse(presenter.state.value.dirty)
        }

    @Test
    fun deadRuleSwitchWritesTheDefaultsAndChecksTheRanges() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/downloaders", settingsJson)
            val presenter = rules(server)
            presenter.load()
            assertFalse(presenter.state.value.deadOn)
            presenter.setDeadRule(true)
            var s = presenter.state.value
            assertTrue(s.deadOn)
            assertEquals(30.0, s.draft.deadMetadataMinutes)
            assertEquals(10.0, s.draft.crawlKbps)
            assertEquals(6.0, s.draft.crawlHours)
            presenter.setNumber(SettingsDownloaderNumber.DEAD_MINUTES, "0")
            s = presenter.state.value
            assertTrue(s.deadOn)
            assertNull(s.issue)
            presenter.setNumber(SettingsDownloaderNumber.DEAD_MINUTES, "3")
            assertEquals(SettingsDownloaderRulesIssue.DEAD_MINUTES_RANGE, presenter.state.value.issue)
            presenter.setNumber(SettingsDownloaderNumber.DEAD_MINUTES, "30")
            presenter.setNumber(SettingsDownloaderNumber.CRAWL_HOURS, "200")
            assertEquals(SettingsDownloaderRulesIssue.HOURS_RANGE, presenter.state.value.issue)
            presenter.setNumber(SettingsDownloaderNumber.CRAWL_HOURS, "6")
            presenter.setNumber(SettingsDownloaderNumber.CRAWL_KBPS, "100001")
            assertEquals(SettingsDownloaderRulesIssue.KBPS_RANGE, presenter.state.value.issue)
            presenter.setDeadRule(false)
            s = presenter.state.value
            assertFalse(s.deadOn)
            assertEquals(0.0, s.draft.crawlKbps)
            assertFalse(s.dirty)
        }

    @Test
    fun rulesSaveErrorStaysOnTheCard() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/downloaders", settingsJson)
            server.json("PUT", "/api/v1/settings/downloaders", """{"error":"grab_spacing_seconds is 0 to 60"}""", 400)
            val presenter = rules(server)
            presenter.load()
            presenter.setSwitch(SettingsDownloaderSwitch.AUTO_ADD, true)
            presenter.save()
            val s = presenter.state.value
            assertEquals("grab_spacing_seconds is 0 to 60", s.saveError?.message)
            assertTrue(s.dirty)
            assertFalse(s.saving)
        }

    @Test
    fun downloadsWaveStillReadsTheRemoveDefaults() {
        val parsed =
            Json {
                ignoreUnknownKeys = true
            }.decodeFromString(
                ch.orellbuehler.acolyte.api.model.DownloaderSettings
                    .serializer(),
                settingsJson,
            )
        assertTrue(parsed.removeDeleteFiles)
        assertFalse(parsed.removeTurnDown)
        assertTrue(parsed.removeSearchAgain)
    }
}
