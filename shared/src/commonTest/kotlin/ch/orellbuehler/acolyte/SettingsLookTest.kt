package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.discover.ExploreStatsPresenter
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookAppearancePresenter
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookAppearanceSaved
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookDefaultsPresenter
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookOption
import ch.orellbuehler.acolyte.feature.settings.look.settingsLookGateDays
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsLookTest {
    private val ui =
        """{"autoplay_trailers":true,"signin_library_art":false,"artwork_cache_gb":5,"history_routine_days":30,"history_other_days":0,
            "count_install":true}"""

    private fun appearanceServer(language: String = "de"): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/my/profile", """{"display_name":"Ana","username":"ana","language":"$language"}""")
        server.json("GET", "/api/v1/settings/ui", ui)
        return server
    }

    @Test
    fun appearanceLoadsTheLanguageAndTheEveryoneCardForSettingsPeople() =
        runTest {
            val server = appearanceServer()
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "owner", perms = emptyList()) }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals("de", s.language)
            assertTrue(s.showEveryone)
            assertTrue(s.autoplayTrailers)
            assertFalse(s.signInArt)
            assertTrue(s.canSeeStats)
            assertEquals(10, s.mostUsedLanguages.size)
            assertEquals(44, s.mostUsedLanguages.size + s.otherLanguages.size)
            assertEquals(44, (s.mostUsedLanguages + s.otherLanguages).toSet().size)
        }

    @Test
    fun appearanceHidesWhatAPersonCannotUse() =
        runTest {
            val server = appearanceServer()
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "member", perms = listOf("ask")) }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertFalse(s.showEveryone)
            assertFalse(s.canSeeStats)
            assertTrue(server.requests.none { it.path == "/api/v1/settings/ui" })
            presenter.setAutoplayTrailers(false)
            assertTrue(server.requests.none { it.method == "PUT" })
        }

    @Test
    fun appearanceAdminsSeeStatsWithoutTheSettingsPermission() =
        runTest {
            val server = appearanceServer()
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "admin", perms = listOf("ask")) }
            presenter.load()
            assertTrue(presenter.state.value.canSeeStats)
            assertFalse(presenter.state.value.showEveryone)
            val mediaOff =
                SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "admin", off = listOf("movies", "shows", "music")) }
            mediaOff.load()
            assertFalse(mediaOff.state.value.canSeeStats)
        }

    @Test
    fun appearanceHidesTheEveryoneCardWhenTheSettingsReadFails() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/my/profile", """{"display_name":"Ana","username":"ana","language":""}""")
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "owner", perms = emptyList()) }
            presenter.load()
            assertEquals(Phase.READY, presenter.state.value.status.phase)
            assertFalse(presenter.state.value.showEveryone)
            assertEquals("", presenter.state.value.language)
        }

    @Test
    fun appearanceFailsWhenTheProfileCannotBeRead() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/my/profile", """{"error":"nope"}""", 500)
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "owner", perms = emptyList()) }
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.SERVER,
                presenter.state.value.status.error
                    ?.code,
            )
        }

    @Test
    fun appearanceSavesTheLanguage() =
        runTest {
            val server = appearanceServer()
            server.on("PUT", "/api/v1/my/language") { respondJson("""{"display_name":"Ana","username":"ana","language":"fr"}""") }
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "owner", perms = emptyList()) }
            presenter.load()
            presenter.setLanguage("xx")
            presenter.setLanguage("de")
            assertTrue(server.requests.none { it.method == "PUT" })
            presenter.setLanguage("FR")
            var s = presenter.state.value
            assertEquals("fr", s.language)
            assertEquals(1, s.savedCount)
            assertEquals(SettingsLookAppearanceSaved.LANGUAGE, s.saved)
            assertEquals("""{"language":"fr"}""", server.request("PUT", "/api/v1/my/language").body)
            server.on("PUT", "/api/v1/my/language") { respondJson("""{"display_name":"Ana","username":"ana","language":""}""") }
            presenter.setLanguage("")
            s = presenter.state.value
            assertEquals("", s.language)
            assertEquals("""{"language":""}""", server.request("PUT", "/api/v1/my/language").body)
        }

    @Test
    fun appearanceKeepsTheLanguageWhenTheSaveFails() =
        runTest {
            val server = appearanceServer()
            server.json("PUT", "/api/v1/my/language", """{"error":"pick one of the app's languages"}""", 400)
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "owner", perms = emptyList()) }
            presenter.load()
            presenter.setLanguage("fr")
            val s = presenter.state.value
            assertEquals("de", s.language)
            assertEquals(ApiErrorCode.BAD_REQUEST, s.languageError?.code)
            assertFalse(s.languageSaving)
            assertEquals(0, s.savedCount)
        }

    @Test
    fun appearanceSavesOneEveryoneSettingAtATime() =
        runTest {
            val server = appearanceServer()
            server.on("PUT", "/api/v1/settings/ui") { respondJson("""{"autoplay_trailers":false,"signin_library_art":false}""") }
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "owner", perms = emptyList()) }
            presenter.load()
            presenter.setAutoplayTrailers(false)
            var s = presenter.state.value
            assertFalse(s.autoplayTrailers)
            assertEquals(SettingsLookAppearanceSaved.AUTOPLAY_OFF, s.saved)
            assertEquals("""{"autoplay_trailers":false}""", server.request("PUT", "/api/v1/settings/ui").body)
            server.on("PUT", "/api/v1/settings/ui") { respondJson("""{"autoplay_trailers":false,"signin_library_art":true}""") }
            presenter.setSignInArt(true)
            s = presenter.state.value
            assertTrue(s.signInArt)
            assertEquals(SettingsLookAppearanceSaved.ART_LIBRARY, s.saved)
            assertEquals(2, s.savedCount)
            assertEquals("""{"signin_library_art":true}""", server.request("PUT", "/api/v1/settings/ui").body)
        }

    @Test
    fun appearanceRevertsAnEveryoneSettingThatDidNotSave() =
        runTest {
            val server = appearanceServer()
            server.json("PUT", "/api/v1/settings/ui", """{"error":"artwork_cache_gb is 1, 2, 5, 10 or 20"}""", 400)
            val presenter = SettingsLookAppearancePresenter(server.api()) { discoverAccount(role = "owner", perms = emptyList()) }
            presenter.load()
            presenter.setAutoplayTrailers(false)
            val s = presenter.state.value
            assertTrue(s.autoplayTrailers)
            assertEquals(ApiErrorCode.BAD_REQUEST, s.everyoneError?.code)
            assertEquals("artwork_cache_gb is 1, 2, 5, 10 or 20", s.everyoneError?.message)
            assertFalse(s.everyoneSaving)
        }

    @Test
    fun statsLeaveOutTitleLayersWhileTheFeatureIsHeld() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/explore/row-stats",
                """{"days":30,"rows":[{"row_type":"best","title":"Best","shown":4,"opens":1}]}""",
            )
            server.json("GET", "/api/v1/explore/title-stats", """{"layers":[{"layer":"plain","shown":10,"opens":2,"asks":1}]}""")
            val owner = discoverAccount(role = "owner", perms = emptyList())
            val open = ExploreStatsPresenter(server.api()) { owner }
            open.load()
            assertEquals(1, open.state.value.layers.size)
            val held = ExploreStatsPresenter(server.api()) { owner.copy(held = listOf("row_title_stats")) }
            held.load()
            assertEquals(1, held.state.value.rows.size)
            assertTrue(
                held.state.value.layers
                    .isEmpty(),
            )
        }

    private val folders =
        """{"library_folders":[{"id":1,"kind":"movie","label":"Films","path":"/m"},{"id":2,"kind":"movie","path":"/m2"},
            {"id":3,"kind":"show","path":"/s"},{"id":4,"kind":"music","path":"/mu"}]}"""
    private val rules =
        """{"pick_rules":[{"id":10,"name":"Any","kinds":[]},{"id":11,"name":"Movies only","kinds":["movie"]},
            {"id":12,"name":"Shows only","kinds":["show"]},{"id":13,"name":"Lossless","kinds":["music"]}]}"""
    private val savedDefaults =
        """{"movie":{"library_folder_id":1,"pick_rules_id":0,"keep_up_to_date":true,"search_on_add":true},
            "show":{"library_folder_id":0,"pick_rules_id":12,"keep_up_to_date":true,"search_on_add":false}}"""
    private val savedGate = """{"release_gate":"in_cinemas","gate_delay_days":3,"bulk_search_respects_gate":true}"""
    private val savedShows =
        """{"season_choice":"all","new_seasons":"all","episode_order":"aired","details_source":"tmdb","keep_specials":true,
            "episode_wait_minutes":90}"""

    private fun defaultsServer(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/settings/title-defaults", savedDefaults)
        server.json("GET", "/api/v1/settings/release-gate", savedGate)
        server.json("GET", "/api/v1/settings/shows", savedShows)
        server.json("GET", "/api/v1/library-folders", folders)
        server.json("GET", "/api/v1/pick-rules", rules)
        return server
    }

    private fun owner() = discoverAccount(role = "owner", perms = emptyList())

    private fun body(
        server: FakeServer,
        path: String,
    ): String = server.request("PUT", path).body.orEmpty()

    @Test
    fun defaultsLoadAllThreeReadsAndTheChoices() =
        runTest {
            val server = defaultsServer()
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertTrue(s.showMovies)
            assertTrue(s.showShows)
            assertTrue(s.showGate)
            assertTrue(s.showShowSettings)
            assertFalse(s.dirty)
            assertEquals(listOf(SettingsLookOption(1, "Films (/m)"), SettingsLookOption(2, "/m2")), s.folderOptions(false))
            assertEquals(listOf(SettingsLookOption(3, "/s")), s.folderOptions(true))
            assertEquals(listOf(10L, 11L), s.ruleOptions(false).map { it.id })
            assertEquals(listOf(10L, 12L), s.ruleOptions(true).map { it.id })
            assertEquals(1, s.folderId(false))
            assertEquals(12, s.rulesId(true))
            assertTrue(s.searchOnAdd(false))
            assertFalse(s.searchOnAdd(true))
            assertEquals("3", s.gateDaysText)
            assertEquals(listOf("announced", "in_cinemas", "released"), s.gateChoices)
            assertEquals(listOf("tmdb", "tvmaze", "tvdb"), s.sourceChoices)
            assertTrue(s.canEdit)
            assertTrue(s.canOpenFolders)
            assertTrue(s.canOpenRequests)
        }

    @Test
    fun defaultsSaveOnlyTheChangedPartsInOrder() =
        runTest {
            val server = defaultsServer()
            server.on("PUT", "/api/v1/settings/title-defaults") { respondJson((it.body as TextContent).text) }
            server.on("PUT", "/api/v1/settings/release-gate") { respondJson((it.body as TextContent).text) }
            server.on("PUT", "/api/v1/settings/shows") { respondJson((it.body as TextContent).text) }
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setSearchOnAdd(show = false, on = false)
            presenter.setFolder(show = true, id = 3)
            presenter.setReleaseGate("released")
            presenter.setGateDays("12")
            presenter.setSeasonChoice("latest")
            presenter.setDetailsSource("tvdb")
            assertTrue(presenter.state.value.dirty)
            assertTrue(presenter.state.value.canSave)
            presenter.save()
            val s = presenter.state.value
            assertFalse(s.dirty)
            assertEquals(1, s.savedCount)
            assertNull(s.saveError)
            assertEquals(
                listOf("/api/v1/settings/title-defaults", "/api/v1/settings/release-gate", "/api/v1/settings/shows"),
                server.requests.filter { it.method == "PUT" }.map { it.path },
            )
            val defaults = body(server, "/api/v1/settings/title-defaults")
            assertTrue(defaults.contains(""""search_on_add":false"""))
            assertTrue(defaults.contains(""""library_folder_id":3"""))
            assertTrue(defaults.contains(""""movie":{"""))
            assertTrue(defaults.contains(""""show":{"""))
            val gate = body(server, "/api/v1/settings/release-gate")
            assertEquals("""{"release_gate":"released","gate_delay_days":12,"bulk_search_respects_gate":true}""", gate)
            val shows = body(server, "/api/v1/settings/shows")
            assertTrue(shows.contains(""""season_choice":"latest""""))
            assertTrue(shows.contains(""""details_source":"tvdb""""))
            assertTrue(shows.contains(""""keep_specials":true"""))
            assertTrue(shows.contains(""""episode_wait_minutes":90"""))
        }

    @Test
    fun defaultsSendNothingForUntouchedParts() =
        runTest {
            val server = defaultsServer()
            server.on("PUT", "/api/v1/settings/release-gate") { respondJson((it.body as TextContent).text) }
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setGateDays("")
            assertEquals("", presenter.state.value.gateDaysText)
            assertTrue(presenter.state.value.canSave)
            presenter.save()
            assertEquals(listOf("/api/v1/settings/release-gate"), server.requests.filter { it.method == "PUT" }.map { it.path })
            assertTrue(body(server, "/api/v1/settings/release-gate").contains(""""gate_delay_days":0"""))
            assertEquals("0", presenter.state.value.gateDaysText)
        }

    @Test
    fun defaultsKeepWhatSavedWhenALaterPartFails() =
        runTest {
            val server = defaultsServer()
            server.on("PUT", "/api/v1/settings/title-defaults") { respondJson((it.body as TextContent).text) }
            server.json("PUT", "/api/v1/settings/release-gate", """{"error":"gate_delay_days must be between 0 and 365"}""", 400)
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setKeepUpToDate(show = false, on = false)
            presenter.setReleaseGate("announced")
            presenter.setNewSeasons("none")
            presenter.save()
            val s = presenter.state.value
            assertEquals(ApiErrorCode.BAD_REQUEST, s.saveError?.code)
            assertEquals("gate_delay_days must be between 0 and 365", s.saveError?.message)
            assertEquals(0, s.savedCount)
            assertFalse(s.saving)
            assertTrue(s.dirty)
            assertEquals(false, s.savedDefaults?.movie?.keepUpToDate)
            assertEquals("in_cinemas", s.savedGate?.releaseGate)
            assertEquals("none", s.shows.newSeasons)
            assertTrue(server.requests.none { it.method == "PUT" && it.path == "/api/v1/settings/shows" })
            presenter.discard()
            assertFalse(presenter.state.value.dirty)
            assertEquals("in_cinemas", presenter.state.value.gate.releaseGate)
            assertEquals("all", presenter.state.value.shows.newSeasons)
        }

    @Test
    fun defaultsValidateTheExtraDays() =
        runTest {
            val server = defaultsServer()
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            presenter.setGateDays("4a0x0")
            assertEquals("400", presenter.state.value.gateDaysText)
            assertFalse(presenter.state.value.gateDaysValid)
            assertFalse(presenter.state.value.canSave)
            assertEquals(3, presenter.state.value.gate.gateDelayDays)
            presenter.setGateDays("365")
            assertTrue(presenter.state.value.gateDaysValid)
            assertTrue(presenter.state.value.canSave)
            assertEquals(365, presenter.state.value.gate.gateDelayDays)
            assertEquals(0, settingsLookGateDays(""))
            assertNull(settingsLookGateDays("366"))
        }

    @Test
    fun defaultsHideTheCardOfAPartThatIsOff() =
        runTest {
            val server = defaultsServer()
            server.json("GET", "/api/v1/settings/release-gate", """{"error":"this part is off","part":"movies"}""", 409)
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertFalse(s.showMovies)
            assertTrue(s.showShows)
            assertTrue(s.showShowSettings)
            assertNull(s.readError)
            assertFalse(s.isEmpty)
        }

    @Test
    fun defaultsSkipReadsOfPartsTheAccountHasOff() =
        runTest {
            val server = defaultsServer()
            val presenter = SettingsLookDefaultsPresenter(server.api()) { discoverAccount(role = "owner", off = listOf("shows", "music")) }
            presenter.load()
            val s = presenter.state.value
            assertTrue(s.showMovies)
            assertFalse(s.showShows)
            assertTrue(server.requests.none { it.path == "/api/v1/settings/shows" })
            val none =
                SettingsLookDefaultsPresenter(server.api()) { discoverAccount(role = "owner", off = listOf("movies", "shows", "music")) }
            none.load()
            assertTrue(none.state.value.isEmpty)
        }

    @Test
    fun defaultsShowAReadErrorBesideWhatLoaded() =
        runTest {
            val server = defaultsServer()
            server.json("GET", "/api/v1/settings/shows", """{"error":"boom"}""", 500)
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals(ApiErrorCode.SERVER, s.readError?.code)
            assertTrue(s.showMovies)
            assertFalse(s.showShowSettings)
        }

    @Test
    fun defaultsFailWhenNothingLoads() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/settings/title-defaults", """{"error":"boom"}""", 500)
            server.json("GET", "/api/v1/settings/release-gate", """{"error":"boom"}""", 500)
            server.json("GET", "/api/v1/settings/shows", """{"error":"boom"}""", 500)
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
            assertEquals(
                ApiErrorCode.SERVER,
                presenter.state.value.status.error
                    ?.code,
            )
        }

    @Test
    fun defaultsAreReadOnlyWithoutTheSettingsPermission() =
        runTest {
            val server = defaultsServer()
            val presenter =
                SettingsLookDefaultsPresenter(server.api()) { discoverAccount(role = "member", perms = listOf("manage_library")) }
            presenter.load()
            presenter.setSearchOnAdd(show = false, on = false)
            presenter.setReleaseGate("released")
            val s = presenter.state.value
            assertFalse(s.canEdit)
            assertFalse(s.dirty)
            assertFalse(s.canSave)
            assertFalse(s.canOpenFolders)
            assertTrue(s.searchOnAdd(false))
            presenter.save()
            assertTrue(server.requests.none { it.method == "PUT" })
        }

    @Test
    fun defaultsOfferAnUnknownSavedValueToo() =
        runTest {
            val server = defaultsServer()
            server.json("GET", "/api/v1/settings/release-gate", """{"release_gate":"later","gate_delay_days":0}""")
            val presenter = SettingsLookDefaultsPresenter(server.api()) { owner() }
            presenter.load()
            assertEquals(listOf("announced", "in_cinemas", "released", "later"), presenter.state.value.gateChoices)
            assertNotNull(presenter.state.value.savedGate)
        }
}
