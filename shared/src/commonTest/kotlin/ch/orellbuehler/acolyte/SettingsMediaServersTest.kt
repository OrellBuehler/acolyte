package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersNoticeKind
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersPresenter
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsMediaServersTest {
    private val servers =
        """{"media_servers":[
            {"id":1,"name":"Plex Home","kind":"plex","url":"http://plex:32400","token":"••••••••","enabled":true,"on_import":true,
             "on_rename":true,"on_delete":false,"path_from":"/data/movies","path_to":"/movies","allow_local_network":true,"web_url":"",
             "plain_password":false,"last_ok_at":"2026-05-01T10:00:00Z"},
            {"id":2,"name":"Jelly","kind":"jellyfin","url":"http://jelly:8096","token":"••••••••","enabled":true,"on_import":true,
             "on_rename":false,"on_delete":false,"path_from":"","path_to":"","allow_local_network":true,"web_url":"https://jelly.example",
             "plain_password":false,"last_error":"connection refused"},
            {"id":3,"name":"Music","kind":"subsonic","url":"http://nas:4533","token":"••••••••","enabled":false,"on_import":false,
             "on_rename":false,"on_delete":false,"path_from":"","path_to":"","allow_local_network":false,"web_url":"",
             "plain_password":true}]}"""

    private val languages =
        """{"languages":[{"code":"en","name":"English"},{"code":"de","name":"German"},{"code":"fr","name":"French"},
            {"code":"es","name":"Spanish"},{"code":"it","name":"Italian"},{"code":"nl","name":"Dutch"},{"code":"sv","name":"Swedish"}]}"""

    private fun owner(): Account = testAccount(Role.OWNER, emptyList())

    private fun body(
        server: FakeServer,
        method: String,
        path: String,
    ): JsonObject = Json.parseToJsonElement(server.request(method, path).body.orEmpty()).jsonObject

    private fun count(
        server: FakeServer,
        method: String,
        path: String,
    ): Int = server.requests.toList().count { it.method == method && it.path == path }

    private fun fullServer(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/media-servers", servers)
        server.json("GET", "/api/v1/settings/watch-history", """{"enabled":true}""")
        server.json("GET", "/api/v1/settings/plex-labels", """{"enabled":false,"label":"holyarrs","askers":false}""")
        server.json("GET", "/api/v1/settings/default-tracks", """{"enabled":false,"audio":[],"subtitles":[]}""")
        server.json("GET", "/api/v1/subtitles/languages", languages)
        return server
    }

    private fun presenter(
        server: FakeServer,
        account: Account? = owner(),
        reauth: suspend (String, String?) -> ApiError? = { _, _ -> null },
    ) = SettingsMediaServersPresenter(server.api(), { account }, testGate(account, reauth), { "de" })

    @Test
    fun loadsTheServersAndTheOwnerCards() =
        runTest {
            val server = fullServer()
            val presenter = presenter(server)
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.allowed)
            assertFalse(s.isEmpty)
            assertEquals(listOf("Plex Home", "Jelly", "Music"), s.servers.map { it.name })
            assertEquals(listOf("Plex", "Jellyfin", "Navidrome / Subsonic"), s.servers.map { it.kindName })
            assertEquals(listOf(true, true, false), s.servers.map { it.enabled })
            assertTrue(s.servers[0].hasPathMap)
            assertTrue(s.servers[0].showsLastChecked)
            assertTrue(s.servers[1].showsLastError)
            assertTrue(s.servers[2].toldNothing)
            assertTrue(s.hasPlex)
            assertTrue(s.hasTrackServer)
            assertTrue(s.showWatch)
            assertTrue(s.watchOn)
            assertTrue(s.showLabels)
            assertTrue(s.showTracks)
            assertEquals(7, s.languages.size)
            assertTrue(s.canPickPlex)
            assertEquals(5, s.kinds.size)
            assertEquals("Navidrome / Subsonic", s.kinds[3].name)
        }

    @Test
    fun anAdminWithSettingsSeesTheListButNotTheOwnerCards() =
        runTest {
            val server = fullServer()
            val presenter = presenter(server, testAccount(Role.ADMIN, listOf("settings")))
            presenter.load()
            val s = presenter.state.value
            assertEquals(3, s.servers.size)
            assertFalse(s.isOwner)
            assertFalse(s.showWatch)
            assertFalse(s.showLabels)
            assertFalse(s.showTracks)
            assertFalse(s.canPickPlex)
            assertEquals(0, count(server, "GET", "/api/v1/settings/watch-history"))
            assertEquals(0, count(server, "GET", "/api/v1/settings/default-tracks"))
        }

    @Test
    fun withoutTheSettingsPermissionThePageIsNotAllowed() =
        runTest {
            val server = fullServer()
            val presenter = presenter(server, testAccount(Role.ADMIN, listOf("manage_library")))
            presenter.load()
            assertFalse(presenter.state.value.allowed)
            assertEquals(0, server.requests.toList().size)
        }

    @Test
    fun anEmptyListIsAnEmptyState() =
        runTest {
            val server = fullServer()
            server.json("GET", "/api/v1/media-servers", """{"media_servers":[]}""")
            val presenter = presenter(server)
            presenter.load()
            assertTrue(presenter.state.value.isEmpty)
            assertFalse(presenter.state.value.showLabels)
            assertFalse(presenter.state.value.showTracks)
        }

    @Test
    fun aFailedListKeepsWhatWasLoadedAndShowsTheError() =
        runTest {
            val server = fullServer()
            val presenter = presenter(server)
            presenter.load()
            server.json("GET", "/api/v1/media-servers", """{"error":"boom"}""", 500)
            presenter.refresh()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals(ApiErrorCode.SERVER, s.status.error?.code)
            assertEquals(3, s.servers.size)
        }

    @Test
    fun aPartThatIsOffHidesOnlyItsCard() =
        runTest {
            val noSubtitles =
                owner().copy(parts = listOf(PartView("subtitles", "Subtitles", false)))
            val server = fullServer()
            val presenter = presenter(server, noSubtitles)
            presenter.load()
            var s = presenter.state.value
            assertTrue(s.showWatch)
            assertTrue(s.showLabels)
            assertFalse(s.showTracks)
            assertEquals(0, count(server, "GET", "/api/v1/settings/default-tracks"))

            val broken = fullServer()
            broken.json(
                "GET",
                "/api/v1/settings/plex-labels",
                """{"error":"this part is off","part":"media","text":"Media is off."}""",
                409,
            )
            val other = presenter(broken)
            other.load()
            s = other.state.value
            assertTrue(s.showWatch)
            assertFalse(s.showLabels)
            assertTrue(s.showTracks)

            val noMedia =
                owner().copy(
                    parts = listOf(PartView("movies", "Movies", false), PartView("shows", "TV", false), PartView("music", "Music", false)),
                )
            val off = presenter(fullServer(), noMedia)
            off.load()
            s = off.state.value
            assertFalse(s.showWatch)
            assertFalse(s.showLabels)
            assertFalse(s.showTracks)
            assertEquals(3, s.servers.size)
        }

    @Test
    fun addingASubsonicServerSendsTheFormAndReloads() =
        runTest {
            val server = fullServer()
            server.json("POST", "/api/v1/media-servers", """{"id":9,"name":"Nas","kind":"subsonic","url":"http://nas:4533"}""", 201)
            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            assertTrue(presenter.state.value.formOpen)
            assertFalse(presenter.state.value.canSave)
            presenter.setKind("subsonic")
            presenter.setName(" Nas ")
            presenter.setUrl("http://nas:4533 ")
            presenter.setUsername("ana")
            presenter.setToken("pw")
            presenter.setPlainPassword(true)
            presenter.setOnDelete(false)
            presenter.setAllowLocalNetwork(false)
            presenter.setPathFrom("/ignored")
            assertTrue(presenter.state.value.canSave)
            presenter.save()
            val sent = body(server, "POST", "/api/v1/media-servers")
            assertEquals("Nas", sent["name"]?.jsonPrimitive?.content)
            assertEquals("subsonic", sent["kind"]?.jsonPrimitive?.content)
            assertEquals("http://nas:4533", sent["url"]?.jsonPrimitive?.content)
            assertEquals("ana", sent["username"]?.jsonPrimitive?.content)
            assertEquals("pw", sent["token"]?.jsonPrimitive?.content)
            assertTrue(sent["plain_password"]?.jsonPrimitive?.boolean == true)
            assertFalse(sent["on_delete"]?.jsonPrimitive?.boolean == true)
            assertFalse(sent["allow_local_network"]?.jsonPrimitive?.boolean == true)
            assertEquals("", sent["path_from"]?.jsonPrimitive?.content)
            assertNull(sent["use_plex_sign_in"])
            val s = presenter.state.value
            assertFalse(s.formOpen)
            assertFalse(s.saving)
            assertEquals(SettingsMediaServersNoticeKind.SAVED, s.notice?.kind)
            assertEquals("Nas", s.notice?.name)
            assertEquals(2, count(server, "GET", "/api/v1/media-servers"))
        }

    @Test
    fun changingAServerStartsEmptyAndKeepsTheStoredToken() =
        runTest {
            val server = fullServer()
            server.on(
                "PUT",
                "/api/v1/media-servers/2",
            ) { respondJson("""{"id":2,"name":"Jelly 2","kind":"jellyfin","url":"http://jelly:8096"}""") }
            val presenter = presenter(server)
            presenter.load()
            presenter.startChange(2)
            var form = presenter.state.value.form
            assertNotNull(form)
            assertEquals("Jelly", form.originalName)
            assertEquals("http://jelly:8096", form.url)
            assertEquals("", form.token)
            assertTrue(form.tokenStored)
            assertEquals("https://jelly.example", form.webUrl)
            assertFalse(form.isNew)
            presenter.setKind("plex")
            assertEquals(
                "jellyfin",
                presenter.state.value.form
                    ?.kind,
            )
            presenter.setName("Jelly 2")
            presenter.setWebUrl("")
            presenter.save()
            val sent = body(server, "PUT", "/api/v1/media-servers/2")
            assertEquals("Jelly 2", sent["name"]?.jsonPrimitive?.content)
            assertNull(sent["kind"])
            assertNull(sent["token"])
            assertNull(sent["username"])
            assertEquals("http://jelly:8096", sent["url"]?.jsonPrimitive?.content)
            assertEquals("", sent["web_url"]?.jsonPrimitive?.content)
            assertTrue(sent["on_import"]?.jsonPrimitive?.boolean == true)
            assertFalse(presenter.state.value.formOpen)
            assertEquals(
                SettingsMediaServersNoticeKind.SAVED,
                presenter.state.value.notice
                    ?.kind,
            )
            form = presenter.state.value.form
            assertNull(form)
        }

    @Test
    fun aRefusedSaveKeepsTheFormOpenWithTheServersSentence() =
        runTest {
            val server = fullServer()
            server.json("POST", "/api/v1/media-servers", """{"error":"another media server has that name"}""", 409)
            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            presenter.setName("Jelly")
            presenter.setUrl("http://j:8096")
            presenter.setToken("key")
            presenter.save()
            val s = presenter.state.value
            assertTrue(s.formOpen)
            assertFalse(s.saving)
            assertEquals(ApiErrorCode.CONFLICT, s.formError?.code)
            assertEquals("Another media server has that name.", s.formDetail)
            assertNull(s.notice)
            presenter.setName("Jelly two")
            assertNull(presenter.state.value.formError)
            presenter.cancelForm()
            assertFalse(presenter.state.value.formOpen)
        }

    @Test
    fun aTestOfAPlexServerCountsItsLibrariesAndAJellyfinOneReadsTheVersion() =
        runTest {
            val server = fullServer()
            server.json("POST", "/api/v1/media-servers/1/test", """{"ok":true,"found":"Libraries: Movies, TV shows, Music"}""")
            server.json("POST", "/api/v1/media-servers/2/test", """{"ok":true,"found":"Jelly Server 10.9.1"}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.test(1)
            var tag =
                presenter.state.value.servers[0]
                    .tag
            assertNotNull(tag)
            assertTrue(tag.ok)
            assertEquals(3, tag.libraries)
            assertEquals("Libraries: Movies, TV shows, Music", tag.hover)
            assertEquals("Plex · 3 libraries", tag.label("Connected", "3 libraries"))
            assertFalse(
                presenter.state.value.servers[0]
                    .testing,
            )
            presenter.test(2)
            tag =
                presenter.state.value.servers[1]
                    .tag
            assertNotNull(tag)
            assertEquals("Jellyfin v10.9.1", tag.label("Connected", ""))
            assertEquals("Jelly Server 10.9.1", tag.hover)
            assertEquals(3, count(server, "GET", "/api/v1/media-servers"))
            assertFalse(
                presenter.state.value.servers[1]
                    .showsFailure,
            )
        }

    @Test
    fun aFailedTestShowsTheServersReasonAndTheHelpLink() =
        runTest {
            val server = fullServer()
            server.json("POST", "/api/v1/media-servers/2/test", """{"error":"jelly → not found."}""", 502)
            server.json("POST", "/api/v1/media-servers/1/test", """{"error":"connection refused"}""", 502)
            val presenter = presenter(server)
            presenter.load()
            presenter.test(2)
            var row = presenter.state.value.servers[1]
            var tag = row.tag
            assertNotNull(tag)
            assertFalse(tag.ok)
            assertEquals("Jelly → not found.", tag.failure)
            assertTrue(tag.helpUrl.endsWith("text=A%20container%20name%20like"))
            assertTrue(row.showsFailure)
            assertFalse(row.showsLastError)
            presenter.test(1)
            row = presenter.state.value.servers[0]
            tag = row.tag
            assertNotNull(tag)
            assertEquals("Connection refused.", tag.failure)
            assertEquals("", tag.helpUrl)
            assertEquals(ApiErrorCode.SERVER, tag.error?.code)
        }

    @Test
    fun savingAChangedServerForgetsItsLastTest() =
        runTest {
            val server = fullServer()
            server.json("POST", "/api/v1/media-servers/2/test", """{"ok":true,"found":"Jelly 10.9.1"}""")
            server.json("PUT", "/api/v1/media-servers/2", """{"id":2,"name":"Jelly","kind":"jellyfin"}""")
            val presenter = presenter(server)
            presenter.load()
            presenter.test(2)
            assertNotNull(
                presenter.state.value.servers[1]
                    .tag,
            )
            presenter.startChange(2)
            presenter.save()
            assertNull(
                presenter.state.value.servers[1]
                    .tag,
            )
        }

    @Test
    fun removingAServerDeletesItAndReloads() =
        runTest {
            val server = fullServer()
            server.json("DELETE", "/api/v1/media-servers/3", "", 204)
            val presenter = presenter(server)
            presenter.load()
            presenter.remove(3)
            assertEquals(1, count(server, "DELETE", "/api/v1/media-servers/3"))
            assertEquals(2, count(server, "GET", "/api/v1/media-servers"))
            assertNull(presenter.state.value.notice)
            assertFalse(
                presenter.state.value.servers
                    .any { it.busy },
            )
        }

    @Test
    fun aFailedRemoveShowsANotice() =
        runTest {
            val server = fullServer()
            server.json("DELETE", "/api/v1/media-servers/3", """{"error":"no such media server"}""", 404)
            val presenter = presenter(server)
            presenter.load()
            presenter.remove(3)
            val notice = presenter.state.value.notice
            assertEquals(SettingsMediaServersNoticeKind.REMOVE_FAILED, notice?.kind)
            assertEquals(ApiErrorCode.NOT_FOUND, notice?.error?.code)
            assertEquals(3, presenter.state.value.servers.size)
            presenter.dismissNotice(notice?.id ?: 0)
            assertNull(presenter.state.value.notice)
        }

    @Test
    fun thePlexPickerFillsTheFormAndUsesThePlexSignIn() =
        runTest {
            val server = fullServer()
            server.json(
                "GET",
                "/api/v1/auth/plex/servers",
                """{"servers":[
                    {"id":"a","name":"Far","address":"1.2.3.4","port":32400,"local":false,"url":"https://far.plex.direct:32400"},
                    {"id":"b","name":"Relay","address":"relay.plex.tv","port":443,"local":false,"relay":true,"url":"https://relay",
                     "secure_url":"https://relay"},
                    {"id":"c","name":"Den","address":"192.168.1.5","port":32400,"local":true,"url":"http://192.168.1.5:32400",
                     "secure_url":"https://den.plex.direct:32400"}]}""",
            )
            server.json("POST", "/api/v1/media-servers", """{"id":9,"name":"Den","kind":"plex"}""", 201)
            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            assertFalse(presenter.state.value.showsPlexPicker)
            presenter.setKind("plex")
            assertTrue(presenter.state.value.showsPlexPicker)
            presenter.findPlexServers()
            var s = presenter.state.value
            assertTrue(s.plex.searched)
            assertEquals(listOf("remote", "relay", "local"), s.plex.rows.map { it.place })
            assertEquals(2, s.plex.picked)
            assertEquals(listOf("https://den.plex.direct:32400", "http://192.168.1.5:32400"), s.plex.addresses)
            var form = s.form
            assertNotNull(form)
            assertEquals("Den", form.name)
            assertEquals("https://den.plex.direct:32400", form.url)
            assertTrue(form.usePlexSignIn)
            assertFalse(form.showsToken)
            val revision = presenter.state.value.formRevision
            presenter.pickPlexAddress("http://elsewhere:32400")
            assertEquals(
                "https://den.plex.direct:32400",
                presenter.state.value.form
                    ?.url,
            )
            presenter.pickPlexAddress("http://192.168.1.5:32400")
            assertEquals(
                "http://192.168.1.5:32400",
                presenter.state.value.form
                    ?.url,
            )
            assertEquals(revision + 1, presenter.state.value.formRevision)
            presenter.setToken("typed")
            presenter.save()
            val sent = body(server, "POST", "/api/v1/media-servers")
            assertTrue(sent["use_plex_sign_in"]?.jsonPrimitive?.boolean == true)
            assertNull(sent["token"])
            assertEquals("http://192.168.1.5:32400", sent["url"]?.jsonPrimitive?.content)
            assertEquals("plex", sent["kind"]?.jsonPrimitive?.content)

            presenter.startAdd()
            presenter.setKind("plex")
            presenter.findPlexServers()
            presenter.setName("Mine")
            presenter.pickPlex(0)
            s = presenter.state.value
            form = s.form
            assertNotNull(form)
            assertEquals("Mine", form.name)
            assertEquals("https://far.plex.direct:32400", form.url)
            presenter.pickPlex(-1)
            assertFalse(
                presenter.state.value.form
                    ?.usePlexSignIn ?: true,
            )
            assertEquals(-1, presenter.state.value.plex.picked)
            presenter.setKind("jellyfin")
            assertFalse(presenter.state.value.plex.searched)
        }

    @Test
    fun withoutALinkedPlexAccountThePickerSaysSo() =
        runTest {
            val server = fullServer()
            server.json("GET", "/api/v1/auth/plex/servers", """{"error":"no Plex account is linked"}""", 409)
            val presenter = presenter(server)
            presenter.load()
            presenter.startAdd()
            presenter.setKind("plex")
            presenter.findPlexServers()
            val plex = presenter.state.value.plex
            assertTrue(plex.notLinked)
            assertNull(plex.error)
            assertFalse(plex.finding)
            server.json("GET", "/api/v1/auth/plex/servers", """{"error":"Plex did not list your servers; try again"}""", 502)
            presenter.findPlexServers()
            assertFalse(presenter.state.value.plex.notLinked)
            assertEquals(
                ApiErrorCode.SERVER,
                presenter.state.value.plex.error
                    ?.code,
            )
        }

    @Test
    fun aKeySessionCannotUseThePlexPicker() =
        runTest {
            val server = fullServer()
            val presenter = presenter(server, owner().copy(method = SignInMethod.API_KEY))
            presenter.load()
            presenter.startAdd()
            presenter.setKind("plex")
            assertFalse(presenter.state.value.canPickPlex)
            assertFalse(presenter.state.value.showsPlexPicker)
            presenter.findPlexServers()
            assertEquals(0, count(server, "GET", "/api/v1/auth/plex/servers"))
        }

    @Test
    fun aSaveThatNeedsAPasswordWaitsForItAndThenGoesThrough() =
        runTest {
            val server = fullServer()
            var proven = false
            server.on("POST", "/api/v1/media-servers") {
                if (proven) {
                    respondJson("""{"id":9,"name":"Jelly","kind":"jellyfin"}""", 201)
                } else {
                    respondJson("""{"error":"confirm your local password again before using this route","code":"reauth"}""", 403)
                }
            }
            val presenter =
                presenter(server, reauth = { password, _ ->
                    proven = password == "secret"
                    if (proven) null else ApiError(ApiErrorCode.UNAUTHORIZED, 401)
                })
            presenter.load()
            presenter.startAdd()
            presenter.setName("Jelly")
            presenter.setUrl("http://j:8096")
            presenter.setToken("key")
            val job = async { presenter.save() }
            presenter.reauth.state.first { it.visible }
            var s = presenter.state.value
            assertTrue(s.formOpen)
            assertFalse(s.saving)
            assertNull(s.formError)
            presenter.reauth.confirm("wrong", "")
            assertTrue(presenter.reauth.state.value.visible)
            assertEquals(
                ApiErrorCode.UNAUTHORIZED,
                presenter.reauth.state.value.error
                    ?.code,
            )
            presenter.reauth.confirm("secret", "123456")
            job.await()
            s = presenter.state.value
            assertFalse(presenter.reauth.state.value.visible)
            assertFalse(s.formOpen)
            assertEquals(SettingsMediaServersNoticeKind.SAVED, s.notice?.kind)
            assertEquals(2, count(server, "POST", "/api/v1/media-servers"))
        }

    @Test
    fun cancellingThePasswordPromptDropsTheParkedWrite() =
        runTest {
            val server = fullServer()
            server.json("DELETE", "/api/v1/media-servers/3", """{"error":"confirm your password","code":"reauth"}""", 403)
            val presenter = presenter(server)
            presenter.load()
            val job = async { presenter.remove(3) }
            presenter.reauth.state.first { it.visible }
            assertNull(presenter.state.value.notice)
            assertFalse(
                presenter.state.value.servers
                    .any { it.busy },
            )
            presenter.reauth.cancel()
            job.await()
            assertFalse(presenter.reauth.state.value.visible)
            assertEquals(1, count(server, "DELETE", "/api/v1/media-servers/3"))
        }

    @Test
    fun theWatchHistorySwitchWritesAndKeepsItsValueOnFailure() =
        runTest {
            val server = fullServer()
            server.on("PUT", "/api/v1/settings/watch-history") { respondJson("""{"enabled":false}""") }
            val presenter = presenter(server)
            presenter.load()
            presenter.setWatchHistory(false)
            assertFalse(presenter.state.value.watchOn)
            assertEquals(false, body(server, "PUT", "/api/v1/settings/watch-history")["enabled"]?.jsonPrimitive?.boolean)
            assertEquals(
                SettingsMediaServersNoticeKind.WATCH_OFF,
                presenter.state.value.notice
                    ?.kind,
            )
            server.json("PUT", "/api/v1/settings/watch-history", """{"error":"something went wrong; see the log"}""", 500)
            presenter.setWatchHistory(true)
            val s = presenter.state.value
            assertFalse(s.watchOn)
            assertFalse(s.watchBusy)
            assertEquals(SettingsMediaServersNoticeKind.WATCH_FAILED, s.notice?.kind)
            assertEquals(ApiErrorCode.SERVER, s.notice?.error?.code)
            assertEquals("", s.notice?.detail)
            server.json("PUT", "/api/v1/settings/watch-history", """{"enabled":true}""")
            presenter.setWatchHistory(true)
            assertTrue(presenter.state.value.watchOn)
            assertEquals(
                SettingsMediaServersNoticeKind.WATCH_ON,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun plexLabelsSaveOnlyWhenChangedAndValid() =
        runTest {
            val server = fullServer()
            server.on("PUT", "/api/v1/settings/plex-labels") { respondJson("""{"enabled":true,"label":"requested","askers":true}""") }
            server.json("POST", "/api/v1/settings/plex-labels/run", """{"started":true}""", 202)
            val presenter = presenter(server)
            presenter.load()
            assertFalse(presenter.state.value.labelsChanged)
            assertFalse(presenter.state.value.labelsCanSave)
            assertFalse(presenter.state.value.labelsCanRun)
            presenter.runLabels()
            assertEquals(0, count(server, "POST", "/api/v1/settings/plex-labels/run"))
            presenter.setLabelsEnabled(true)
            assertTrue(presenter.state.value.labelsCanSave)
            presenter.setLabelText("   ")
            assertFalse(presenter.state.value.labelsCanSave)
            presenter.setLabelText(" requested ")
            presenter.setLabelAskers(true)
            presenter.saveLabels()
            val sent = body(server, "PUT", "/api/v1/settings/plex-labels")
            assertEquals("requested", sent["label"]?.jsonPrimitive?.content)
            assertTrue(sent["enabled"]?.jsonPrimitive?.boolean == true)
            assertTrue(sent["askers"]?.jsonPrimitive?.boolean == true)
            var s = presenter.state.value
            assertFalse(s.labelsChanged)
            assertEquals("requested", s.labels?.label)
            assertEquals(SettingsMediaServersNoticeKind.LABELS_SAVED, s.notice?.kind)
            assertTrue(s.labelsCanRun)
            presenter.runLabels()
            assertEquals(1, count(server, "POST", "/api/v1/settings/plex-labels/run"))
            assertEquals(
                SettingsMediaServersNoticeKind.LABELS_STARTED,
                presenter.state.value.notice
                    ?.kind,
            )
            server.json("POST", "/api/v1/settings/plex-labels/run", """{"error":"turn on Label requested titles first"}""", 409)
            presenter.runLabels()
            s = presenter.state.value
            assertEquals(SettingsMediaServersNoticeKind.LABELS_START_FAILED, s.notice?.kind)
            assertEquals("Turn on Label requested titles first.", s.notice?.detail)
            assertFalse(s.labelRunBusy)
            presenter.setLabelText("x".repeat(100))
            assertEquals(64, presenter.state.value.labelDraft.label.length)
        }

    @Test
    fun defaultTracksAreEditedAsOrderedListsOfAtMostFive() =
        runTest {
            val server = fullServer()
            server.on("PUT", "/api/v1/settings/default-tracks") {
                respondJson("""{"enabled":true,"audio":["de","en"],"subtitles":["de","off"]}""")
            }
            val presenter = presenter(server)
            presenter.load()
            assertFalse(presenter.state.value.tracksChanged)
            presenter.setTracksEnabled(true)
            assertTrue(presenter.state.value.tracksChanged)
            assertFalse(presenter.state.value.tracksCanSave)
            presenter.addTrackLanguage(subtitles = false)
            presenter.addTrackLanguage(subtitles = false)
            assertEquals(listOf("de", "en"), presenter.state.value.trackDraft.audio)
            presenter.moveTrackLanguage(subtitles = false, index = 1, up = true)
            assertEquals(listOf("en", "de"), presenter.state.value.trackDraft.audio)
            presenter.setTrackLanguage(subtitles = false, index = 0, code = "fr")
            assertEquals(listOf("fr", "de"), presenter.state.value.trackDraft.audio)
            presenter.setTrackLanguage(subtitles = false, index = 0, code = "de")
            assertEquals(listOf("fr", "de"), presenter.state.value.trackDraft.audio)
            presenter.setTrackLanguage(subtitles = false, index = 0, code = "off")
            assertEquals(listOf("fr", "de"), presenter.state.value.trackDraft.audio)
            presenter.moveTrackLanguage(subtitles = false, index = 0, up = false)
            assertEquals(listOf("de", "fr"), presenter.state.value.trackDraft.audio)
            presenter.removeTrackLanguage(subtitles = false, index = 1)
            presenter.addTrackLanguage(subtitles = false)
            assertEquals(listOf("de", "en"), presenter.state.value.trackDraft.audio)

            presenter.addTrackLanguage(subtitles = true)
            presenter.addTrackLanguage(subtitles = true)
            presenter.setTrackLanguage(subtitles = true, index = 1, code = "off")
            var s = presenter.state.value
            assertEquals(listOf("de", "off"), s.trackDraft.subtitles)
            assertFalse(s.canAddSubtitle)
            val rows = s.subtitleRows
            assertEquals(listOf("de", "off"), rows.map { it.code })
            assertEquals("German", rows[0].name)
            assertEquals("off", rows[0].options.first().code)
            assertTrue(rows[0].options.any { it.code == "de" })
            assertFalse(rows[1].canUp)
            assertFalse(rows[0].canDown)
            assertFalse(rows[1].canDown)
            assertTrue(s.audioRows[0].canDown)
            assertFalse(s.audioRows[0].canUp)
            assertFalse(s.audioRows[0].options.any { it.code == "en" })
            presenter.addTrackLanguage(subtitles = true)
            assertEquals(listOf("de", "off"), presenter.state.value.trackDraft.subtitles)

            presenter.saveTracks()
            val sent = body(server, "PUT", "/api/v1/settings/default-tracks")
            assertTrue(sent["enabled"]?.jsonPrimitive?.boolean == true)
            assertEquals(listOf("de", "en"), sent["audio"]?.jsonArray?.map { it.jsonPrimitive.content })
            assertEquals(listOf("de", "off"), sent["subtitles"]?.jsonArray?.map { it.jsonPrimitive.content })
            s = presenter.state.value
            assertFalse(s.tracksChanged)
            assertFalse(s.trackBusy)
            assertEquals(SettingsMediaServersNoticeKind.TRACKS_SAVED_ON, s.notice?.kind)
        }

    @Test
    fun theLanguageListsStopAtFive() =
        runTest {
            val server = fullServer()
            val presenter = presenter(server)
            presenter.load()
            presenter.setTracksEnabled(true)
            repeat(7) { presenter.addTrackLanguage(subtitles = false) }
            assertEquals(5, presenter.state.value.trackDraft.audio.size)
            assertEquals(
                5,
                presenter.state.value.trackDraft.audio
                    .toSet()
                    .size,
            )
            assertFalse(presenter.state.value.canAddAudio)
            presenter.removeTrackLanguage(subtitles = false, index = 0)
            assertTrue(presenter.state.value.canAddAudio)
        }

    @Test
    fun aFailedTracksSaveKeepsTheEditsAndShowsTheReason() =
        runTest {
            val server = fullServer()
            server.json("PUT", "/api/v1/settings/default-tracks", """{"error":"pick up to 5 languages"}""", 400)
            val presenter = presenter(server)
            presenter.load()
            presenter.setTracksEnabled(true)
            presenter.addTrackLanguage(subtitles = true)
            presenter.saveTracks()
            val s = presenter.state.value
            assertTrue(s.tracksChanged)
            assertEquals(listOf("de"), s.trackDraft.subtitles)
            assertEquals(SettingsMediaServersNoticeKind.TRACKS_FAILED, s.notice?.kind)
            assertEquals("Pick up to 5 languages.", s.notice?.detail)
            assertFalse(s.trackBusy)
        }

    @Test
    fun theTracksCardSavesWithoutTheOnNoticeWhenTurnedOff() =
        runTest {
            val server = fullServer()
            server.json("GET", "/api/v1/settings/default-tracks", """{"enabled":true,"audio":["en"],"subtitles":[]}""")
            server.json("PUT", "/api/v1/settings/default-tracks", """{"enabled":false,"audio":["en"],"subtitles":[]}""")
            val presenter = presenter(server)
            presenter.load()
            assertTrue(presenter.state.value.trackDraft.enabled)
            presenter.setTracksEnabled(false)
            assertTrue(presenter.state.value.tracksCanSave)
            presenter.saveTracks()
            assertEquals(
                SettingsMediaServersNoticeKind.TRACKS_SAVED,
                presenter.state.value.notice
                    ?.kind,
            )
        }
}
