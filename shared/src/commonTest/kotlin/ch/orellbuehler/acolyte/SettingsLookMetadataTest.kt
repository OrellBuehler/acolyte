package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookMetadataPresenter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsLookMetadataTest {
    private fun answer(
        mode: String = "full",
        locked: Boolean = false,
        saved: List<String> = emptyList(),
        docker: Boolean = false,
    ): String {
        val keys =
            listOf("tmdb_token", "tvdb_key", "tvdb_pin", "mdblist_key", "fanart_key", "omdb_key")
                .joinToString(",") { """"$it":${it in saved}""" }
        val lockedNote = if (locked) ""","locked_note":"Set by your Docker settings"""" else ""
        return """{"mode":"$mode","locked":$locked$lockedNote,"keys":{$keys},"tmdb_key_from_docker":$docker,
            "sources":[{"id":"tmdb","name":"TMDB","state":"${if (mode == "off") "off" else "on"}"},
            {"id":"omdb","name":"OMDb","state":"needs_key","key":"omdb_key"},{"id":"charts","name":"Charts","state":"off"}],
            "hosts":["api.example.org","img.example.org"],"own_keys_note":"note"}"""
    }

    private fun server(
        mode: String = "full",
        saved: List<String> = emptyList(),
        up: Boolean = true,
    ): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/settings/metadata", answer(mode, saved = saved))
        server.json("GET", "/api/v1/metadata/health", """{"up":$up}""")
        return server
    }

    private fun owner() = discoverAccount(role = "owner", perms = emptyList())

    private fun body(server: FakeServer): String = server.request("PUT", "/api/v1/settings/metadata").body.orEmpty()

    @Test
    fun loadReadsTheModeKeysSourcesAndHealth() =
        runTest {
            val server = server(saved = listOf("omdb_key"))
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals("full", s.mode)
            assertEquals(listOf("api.example.org", "img.example.org"), s.hosts)
            assertEquals(6, s.keys.size)
            assertTrue(s.keys.first { it.name == "omdb_key" }.stored)
            assertFalse(s.keys.first { it.name == "tmdb_token" }.saved)
            assertFalse(s.dirty)
            assertEquals(true, s.health?.up)
            assertNull(s.healthError)
            assertTrue(s.canChooseMode)
            assertTrue(s.offSources.isEmpty())
        }

    @Test
    fun keyRowsFollowTheMode() =
        runTest {
            val full = SettingsLookMetadataPresenter(server().api()) { owner() }
            full.load()
            assertTrue(full.state.value.tvdbClosed)
            assertEquals(
                listOf("omdb_key"),
                full.state.value.shownKeys
                    .map { it.name },
            )
            full.openTvdb()
            assertFalse(full.state.value.tvdbClosed)
            assertEquals(
                listOf("tvdb_key", "tvdb_pin", "omdb_key"),
                full.state.value.shownKeys
                    .map { it.name },
            )
            val saved = SettingsLookMetadataPresenter(server(saved = listOf("tvdb_key")).api()) { owner() }
            saved.load()
            assertFalse(saved.state.value.tvdbClosed)
            assertEquals(
                listOf("tvdb_key", "tvdb_pin", "omdb_key"),
                saved.state.value.shownKeys
                    .map { it.name },
            )
            val own = SettingsLookMetadataPresenter(server(mode = "own").api()) { owner() }
            own.load()
            assertEquals(
                listOf("tmdb_token", "tvdb_key", "tvdb_pin", "mdblist_key", "fanart_key", "omdb_key"),
                own.state.value.shownKeys
                    .map { it.name },
            )
            assertEquals(
                listOf("omdb", "charts"),
                own.state.value.offSources
                    .map { it.id },
            )
            val off = SettingsLookMetadataPresenter(server(mode = "off").api()) { owner() }
            off.load()
            assertTrue(
                off.state.value.shownKeys
                    .isEmpty(),
            )
            assertEquals(3, off.state.value.offSources.size)
        }

    @Test
    fun savingSendsEverySixKeyLikeTheWeb() =
        runTest {
            val server = server(mode = "own", saved = listOf("tmdb_token", "mdblist_key"))
            server.json("PUT", "/api/v1/settings/metadata", answer(mode = "own", saved = listOf("tmdb_token", "omdb_key")))
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setKey("omdb_key", "  abc123 \n")
            presenter.clearKey("mdblist_key")
            var s = presenter.state.value
            assertTrue(s.dirty)
            assertTrue(s.canSave)
            assertTrue(s.keys.first { it.name == "mdblist_key" }.removing)
            assertEquals("  abc123 ", s.keys.first { it.name == "omdb_key" }.text)
            presenter.save()
            assertEquals(
                """{"keys":{"tmdb_token":"••••••••","tvdb_key":"","tvdb_pin":"","mdblist_key":"","fanart_key":"","omdb_key":"abc123"}}""",
                body(server),
            )
            s = presenter.state.value
            assertFalse(s.dirty)
            assertEquals(1, s.keysSavedCount)
            assertTrue(s.keys.first { it.name == "omdb_key" }.stored)
            assertFalse(s.keys.first { it.name == "mdblist_key" }.saved)
        }

    @Test
    fun restoreAndDiscardPutTheMasksBack() =
        runTest {
            val server = server(mode = "own", saved = listOf("tmdb_token"))
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            presenter.clearKey("tmdb_token")
            assertTrue(presenter.state.value.dirty)
            presenter.restoreKey("tmdb_token")
            assertFalse(presenter.state.value.dirty)
            presenter.setKey("fanart_key", "x")
            presenter.setKey("tmdb_token", "y")
            presenter.discard()
            assertFalse(presenter.state.value.dirty)
            assertEquals(
                "",
                presenter.state.value.keys
                    .first { it.name == "fanart_key" }
                    .text,
            )
            assertTrue(
                presenter.state.value.keys
                    .first { it.name == "tmdb_token" }
                    .stored,
            )
        }

    @Test
    fun aKeySetByDockerCannotBeEdited() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/metadata", answer(mode = "own", saved = listOf("tmdb_token"), docker = true))
            server.json("GET", "/api/v1/metadata/health", """{"up":true}""")
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            assertTrue(
                presenter.state.value.keys
                    .first { it.name == "tmdb_token" }
                    .fromDocker,
            )
            presenter.clearKey("tmdb_token")
            presenter.setKey("tmdb_token", "x")
            presenter.testKey("tmdb_token")
            assertFalse(presenter.state.value.dirty)
            assertTrue(server.requests.none { it.method == "POST" })
        }

    @Test
    fun savingFailureKeepsTheEdits() =
        runTest {
            val server = server(mode = "own")
            server.json("PUT", "/api/v1/settings/metadata", """{"error":"send {\"mode\": ...}"}""", 400)
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setKey("omdb_key", "abc")
            presenter.save()
            val s = presenter.state.value
            assertEquals(ApiErrorCode.BAD_REQUEST, s.saveError?.code)
            assertTrue(s.dirty)
            assertEquals(0, s.keysSavedCount)
            assertFalse(s.saving)
        }

    @Test
    fun choosingAModeSendsOnlyTheModeAndKeepsKeyEdits() =
        runTest {
            val server = server(mode = "full")
            server.json("PUT", "/api/v1/settings/metadata", answer(mode = "own"))
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setKey("omdb_key", "abc")
            presenter.chooseMode("own")
            assertEquals("""{"mode":"own"}""", body(server))
            val s = presenter.state.value
            assertEquals("own", s.mode)
            assertEquals(1, s.modeSavedCount)
            assertTrue(s.dirty)
            assertEquals("abc", s.keys.first { it.name == "omdb_key" }.text)
            assertEquals(2, server.requests.count { it.method == "GET" && it.path == "/api/v1/metadata/health" })
        }

    @Test
    fun choosingOffDropsUnsavedKeyEdits() =
        runTest {
            val server = server(mode = "own")
            server.json("PUT", "/api/v1/settings/metadata", answer(mode = "off"))
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setKey("omdb_key", "abc")
            presenter.chooseMode("off")
            assertEquals("off", presenter.state.value.mode)
            assertFalse(presenter.state.value.dirty)
        }

    @Test
    fun aLockedModeCannotBeChanged() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/metadata", answer(mode = "full", locked = true))
            server.json("GET", "/api/v1/metadata/health", """{"up":true}""")
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            assertTrue(presenter.state.value.locked)
            assertFalse(presenter.state.value.canChooseMode)
            presenter.chooseMode("own")
            assertTrue(server.requests.none { it.method == "PUT" })
            assertEquals("full", presenter.state.value.mode)
        }

    @Test
    fun aFailedModeChangeGoesBack() =
        runTest {
            val server = server(mode = "full")
            server.json("PUT", "/api/v1/settings/metadata", """{"error":"Set by your Docker settings"}""", 409)
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            presenter.chooseMode("off")
            val s = presenter.state.value
            assertEquals("full", s.mode)
            assertEquals(ApiErrorCode.CONFLICT, s.modeError?.code)
            assertEquals("Set by your Docker settings", s.modeError?.message)
            assertFalse(s.modeSaving)
            assertEquals(0, s.modeSavedCount)
        }

    @Test
    fun testingAKeySendsTheKeyAndThePinOnlyForTheTvdbKey() =
        runTest {
            val server = server(mode = "own", saved = listOf("omdb_key"))
            server.json(
                "POST",
                "/api/v1/settings/metadata/test",
                """{"ok":false,"code":"tvdb_refused","message":"Key refused; a PIN may be needed"}""",
            )
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setKey("tvdb_key", " v4key ")
            presenter.setKey("tvdb_pin", " 1234 ")
            presenter.testKey("tvdb_key")
            assertEquals(
                """{"key":"tvdb_key","value":"v4key","pin":"1234"}""",
                server.request("POST", "/api/v1/settings/metadata/test").body,
            )
            val result =
                presenter.state.value.keys
                    .first { it.name == "tvdb_key" }
                    .result
            assertEquals(false, result?.ok)
            assertEquals("tvdb_refused", result?.code)
            server.json("POST", "/api/v1/settings/metadata/test", """{"ok":true,"code":"works","message":"Key works"}""")
            presenter.testKey("omdb_key")
            assertEquals(
                """{"key":"omdb_key","value":"••••••••","pin":""}""",
                server.request("POST", "/api/v1/settings/metadata/test").body,
            )
            assertEquals(
                true,
                presenter.state.value.keys
                    .first { it.name == "omdb_key" }
                    .result
                    ?.ok,
            )
            assertFalse(
                presenter.state.value.keys
                    .first { it.name == "omdb_key" }
                    .testing,
            )
            presenter.testKey("tvdb_pin")
            assertEquals(2, server.requests.count { it.method == "POST" })
        }

    @Test
    fun aKeyTestThatCannotRunShowsTheError() =
        runTest {
            val server = server(mode = "own")
            server.json("POST", "/api/v1/settings/metadata/test", """{"error":"boom"}""", 500)
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            presenter.testKey("tmdb_token")
            val result =
                presenter.state.value.keys
                    .first { it.name == "tmdb_token" }
                    .result
            assertEquals(ApiErrorCode.SERVER, result?.error?.code)
            presenter.setKey("tmdb_token", "x")
            assertNull(
                presenter.state.value.keys
                    .first { it.name == "tmdb_token" }
                    .result,
            )
        }

    @Test
    fun healthShowsAFakeAddressHostAndChecksAgain() =
        runTest {
            val server = server(up = false)
            server.json("GET", "/api/v1/metadata/health", """{"up":false,"message":"x","fakeIP":"api.example.org"}""")
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            assertEquals(
                false,
                presenter.state.value.health
                    ?.up,
            )
            assertEquals(
                "api.example.org",
                presenter.state.value.health
                    ?.fakeHost,
            )
            server.json("GET", "/api/v1/metadata/health", """{"up":true,"fakeIP":"bad host!"}""")
            presenter.checkAgain()
            assertEquals(
                true,
                presenter.state.value.health
                    ?.up,
            )
            assertEquals(
                "",
                presenter.state.value.health
                    ?.fakeHost,
            )
            assertFalse(presenter.state.value.checking)
            server.json("GET", "/api/v1/metadata/health", """{"up":false,"fakeIP":"bad host!"}""")
            presenter.checkAgain()
            assertEquals(
                "",
                presenter.state.value.health
                    ?.fakeHost,
            )
            server.json("GET", "/api/v1/metadata/health", """{"error":"nope"}""", 500)
            presenter.checkAgain()
            assertEquals(
                ApiErrorCode.SERVER,
                presenter.state.value.healthError
                    ?.code,
            )
            assertFalse(presenter.state.value.checking)
        }

    @Test
    fun theLoadFailsWhenTheSettingsCannotBeRead() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/metadata", """{"error":"boom"}""", 500)
            server.json("GET", "/api/v1/metadata/health", """{"up":true}""")
            val presenter = SettingsLookMetadataPresenter(server.api()) { owner() }
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
        }

    @Test
    fun peopleWithoutTheSettingsPermissionAreTurnedAway() =
        runTest {
            val server = server()
            val presenter =
                SettingsLookMetadataPresenter(server.api()) { discoverAccount(role = "member", perms = listOf("manage_library")) }
            presenter.load()
            assertFalse(presenter.state.value.allowed)
            assertFalse(presenter.state.value.canSave)
            assertTrue(server.requests.isEmpty())
        }
}
