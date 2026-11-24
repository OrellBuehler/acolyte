package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleKind
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSite
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleTidy
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleWant
import ch.orellbuehler.acolyte.api.model.SubtitleLanguage
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleNoteKind
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitlePresenter
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleSiteState
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleWishProblem
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.feature.acquisition.settingsSubtitleAccountMissing
import ch.orellbuehler.acolyte.feature.acquisition.settingsSubtitleCleanRows
import ch.orellbuehler.acolyte.feature.acquisition.settingsSubtitleFps
import ch.orellbuehler.acolyte.feature.acquisition.settingsSubtitleNewLanguage
import ch.orellbuehler.acolyte.feature.acquisition.settingsSubtitleSiteState
import ch.orellbuehler.acolyte.feature.acquisition.settingsSubtitleTestCount
import ch.orellbuehler.acolyte.feature.acquisition.settingsSubtitleWishProblem
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSubtitleTest {
    private fun owner(subtitles: Boolean = true): Account =
        testAccount(Role.OWNER, emptyList()).let { it.copy(parts = it.parts + PartView("subtitles", "Subtitles", subtitles)) }

    private val settings =
        """{"languages":[{"language":"en","hearing_impaired":"normal","forced":false}],"pass_mark_movie":70,"pass_mark_episode":90,
            "use_built_in":true,"ignore_picture_tracks":false,"hi_suffix":"hi","search_hours":6,"better":true,"better_hours":12,
            "better_days":7,"batch":100,"adaptive":true,"sync_from_fps":25,"sync_to_fps":23.976,"sync_step_ms":500,
            "clean":{"*":{"ads":true,"hi":false,"tags":false,"emoji":false,"ocr":false,"caps":false,"spacing":false,"overlap":false}}}"""

    private val kinds =
        """{"kinds":[{"kind":"opensubtitles","name":"OpenSubtitles.com","default_url":"https://api.opensubtitles.com/api/v1","movies":true,
            "shows":true,"needs_account":true,"account":["api_key","username","password"],"note":"Needs your account."},
            {"kind":"gestdown","name":"Gestdown","default_url":"https://api.gestdown.info","movies":false,"shows":true,"needs_account":false},
            {"kind":"podnapisi","name":"Podnapisi","default_url":"https://www.podnapisi.net","movies":true,"shows":true,"default_off":true}]}"""

    private val sites =
        """{"sites":[{"ID":3,"name":"Gestdown","kind":"gestdown","url":"https://api.gestdown.info","state":"enabled","pause_reason":null,
            "paused_until":null,"failures":0,"limits":{"gap_ms":1000,"per_window":60,"window_seconds":3600,"downloads_per_day":20},
            "downloads_today":4,"allow_local_network":false,"priority":25,"machine_made":false,"has_account":false,"CreatedAt":"x"}]}"""

    private fun server(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/settings/subtitles", settings)
        server.json("GET", "/api/v1/subtitles/languages", """{"languages":[{"code":"en","name":"English"},{"code":"de","name":"German"}]}""")
        server.json(
            "GET",
            "/api/v1/settings/subtitles/translator",
            """{"kind":"","url":"","model":"","has_key":false,"built_in_auto":false}""",
        )
        server.json("GET", "/api/v1/subtitle-sites/kinds", kinds)
        server.json("GET", "/api/v1/subtitle-sites", sites)
        return server
    }

    @Test
    fun pageNeedsSettingsAndTheSubtitlesPart() {
        assertTrue(owner().mayOpen(AcquisitionPage.SUBTITLES))
        assertFalse(owner(subtitles = false).mayOpen(AcquisitionPage.SUBTITLES))
        assertFalse(testAccount(Role.ADMIN, listOf("manage_library")).mayOpen(AcquisitionPage.SUBTITLES))
    }

    @Test
    fun wishProblems() {
        fun want(
            language: String,
            forced: Boolean = false,
        ) = SettingsSubtitleWant(language = language, forced = forced)
        assertNull(settingsSubtitleWishProblem(listOf(want("en"), want("en", forced = true))))
        assertEquals(SettingsSubtitleWishProblem.TWICE, settingsSubtitleWishProblem(listOf(want("en"), want("en"))))
        assertEquals(SettingsSubtitleWishProblem.PICK, settingsSubtitleWishProblem(listOf(want("en"), want(""))))
        assertEquals(SettingsSubtitleWishProblem.TOO_MANY, settingsSubtitleWishProblem(List(11) { want("l$it") }))
    }

    @Test
    fun newLanguageIsTheFirstUnused() {
        val languages = listOf(SubtitleLanguage("it", "Italian"))
        assertEquals("en", settingsSubtitleNewLanguage(emptyList(), languages))
        val taken = listOf("en", "es", "fr", "de").map { SettingsSubtitleWant(language = it) }
        assertEquals("it", settingsSubtitleNewLanguage(taken, languages))
    }

    @Test
    fun numberRanges() {
        assertEquals(100.0, SettingsSubtitleNumber.PASS_MOVIE.parse("100"))
        assertNull(SettingsSubtitleNumber.PASS_MOVIE.parse("101"))
        assertNull(SettingsSubtitleNumber.BETTER_HOURS.parse("5"))
        assertEquals(6.0, SettingsSubtitleNumber.BETTER_HOURS.parse("6"))
        assertNull(SettingsSubtitleNumber.SYNC_STEP_MS.parse("9"))
        assertEquals(23.976, SettingsSubtitleNumber.SYNC_TO_FPS.parse(" 23.976 "))
        assertNull(SettingsSubtitleNumber.SYNC_TO_FPS.parse("0.5"))
        assertNull(SettingsSubtitleNumber.BATCH.parse("1.5"))
        assertNull(SettingsSubtitleNumber.BATCH.parse(""))
        assertEquals("25", settingsSubtitleFps(25.0))
        assertEquals("23.976", settingsSubtitleFps(23.976))
    }

    @Test
    fun cleanRowsPutEveryLanguageFirst() {
        val tidy = SettingsSubtitleTidy()
        assertEquals(listOf("*", "de", "fr"), settingsSubtitleCleanRows(mapOf("fr" to tidy, "de" to tidy, "*" to tidy)))
        assertEquals(listOf("*"), settingsSubtitleCleanRows(emptyMap()))
    }

    @Test
    fun testCountReadsTheNumber() {
        assertEquals("12 subtitles", settingsSubtitleTestCount("The site answered: 12 subtitles for a test title."))
        assertEquals("", settingsSubtitleTestCount("The site refused the account details."))
    }

    @Test
    fun siteStateFollowsTheRules() {
        val needs = SettingsSubtitleKind(kind = "opensubtitles", needsAccount = true)
        val plain = SettingsSubtitleKind(kind = "gestdown")
        val enabled = SettingsSubtitleSite(id = 1, kind = "gestdown", state = "enabled")
        assertEquals(SettingsSubtitleSiteState.NEEDS_ACCOUNT, settingsSubtitleSiteState(enabled.copy(kind = "opensubtitles"), needs))
        assertEquals(SettingsSubtitleSiteState.ON, settingsSubtitleSiteState(enabled.copy(hasAccount = true), needs))
        assertEquals(SettingsSubtitleSiteState.PAUSED, settingsSubtitleSiteState(enabled.copy(state = "disabled"), plain))
        assertEquals(SettingsSubtitleSiteState.RESTING, settingsSubtitleSiteState(enabled.copy(state = "paused"), plain))
        assertEquals(SettingsSubtitleSiteState.ON, settingsSubtitleSiteState(enabled, null))
    }

    @Test
    fun loadsEverythingAndEditsDefaults() =
        runTest {
            val server = server()
            server.on("PUT", "/api/v1/settings/subtitles") { respondJson((it.body as io.ktor.http.content.TextContent).text) }
            val presenter = SettingsSubtitlePresenter(server.api()) { owner() }
            presenter.load()
            var s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertNull(s.status.error)
            assertEquals(2, s.languages.size)
            assertEquals(3, s.kinds.size)
            assertEquals(1, s.sites.size)
            assertFalse(s.dirty)
            assertFalse(s.translatorReady)
            presenter.addLanguage()
            presenter.setWantType(1, "hi")
            presenter.setWantLanguage(1, "de")
            presenter.setNumber("pass_mark_movie", "85")
            presenter.setNumber("sync_to_fps", "24")
            presenter.setFlag("better", false)
            presenter.pickCleanLanguage("de")
            presenter.setStep("hi", true)
            s = presenter.state.value
            assertTrue(s.canSave)
            assertEquals(listOf("en", "de"), s.draft.languages.map { it.language })
            assertEquals("hi", s.draft.languages[1].hearingImpaired)
            assertEquals(listOf("*", "de"), s.cleanRows)
            assertTrue(s.step("hi"))
            assertTrue(s.step("ads"))
            presenter.setNumber("pass_mark_movie", "101")
            assertFalse(presenter.state.value.canSave)
            assertFalse(presenter.state.value.numberValid("pass_mark_movie"))
            presenter.setNumber("pass_mark_movie", "85")
            presenter.save()
            s = presenter.state.value
            assertFalse(s.dirty)
            assertEquals(SettingsSubtitleNoteKind.SETTINGS_SAVED, s.note?.kind)
            assertEquals(85, s.saved.passMarkMovie)
            assertEquals(24.0, s.saved.syncToFps)
            val body = server.request("PUT", "/api/v1/settings/subtitles").body.orEmpty()
            assertTrue(body.contains(""""pass_mark_movie":85"""))
            assertTrue(body.contains(""""hearing_impaired":"hi""""))
            assertTrue(body.contains(""""de":{"ads":true,"hi":true"""))
        }

    @Test
    fun duplicateLanguageBlocksSave() =
        runTest {
            val presenter = SettingsSubtitlePresenter(server().api()) { owner() }
            presenter.load()
            presenter.addLanguage()
            presenter.setWantLanguage(1, "en")
            assertEquals(SettingsSubtitleWishProblem.TWICE, presenter.state.value.wishProblem)
            assertFalse(presenter.state.value.canSave)
            presenter.setWantForced(1, true)
            assertTrue(presenter.state.value.canSave)
            presenter.removeLanguage(1)
            assertFalse(presenter.state.value.dirty)
        }

    @Test
    fun removingACleanLanguageDropsItFromTheBody() =
        runTest {
            val presenter = SettingsSubtitlePresenter(server().api()) { owner() }
            presenter.load()
            presenter.pickCleanLanguage("fr")
            assertEquals(setOf("*", "fr"), presenter.state.value.draft.clean.keys)
            presenter.removeCleanLanguage()
            assertEquals("*", presenter.state.value.cleanLanguage)
            assertFalse(presenter.state.value.dirty)
        }

    @Test
    fun partialFailureKeepsTheScreenAndShowsTheError() =
        runTest {
            val server = server()
            server.json("GET", "/api/v1/subtitle-sites", """{"error":"nope"}""", 500)
            val presenter = SettingsSubtitlePresenter(server.api()) { owner() }
            presenter.load()
            val s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals(ApiErrorCode.SERVER, s.status.error?.code)
            assertEquals(1, s.draft.languages.size)
        }

    @Test
    fun settingsFailureIsAFullFailure() =
        runTest {
            val server = server()
            server.json("GET", "/api/v1/settings/subtitles", """{"error":"nope"}""", 500)
            val presenter = SettingsSubtitlePresenter(server.api()) { owner() }
            presenter.load()
            assertEquals(Phase.FAILED, presenter.state.value.status.phase)
        }

    @Test
    fun translatorSavesAndTests() =
        runTest {
            val server = server()
            server.json(
                "PUT",
                "/api/v1/settings/subtitles/translator",
                """{"kind":"openai","url":"http://x/v1","model":"m1","has_key":true,"built_in_auto":true}""",
            )
            server.json(
                "POST",
                "/api/v1/settings/subtitles/translator/test",
                """{"ok":true,"message":"Translated two lines.","models":["m1","m2"]}""",
            )
            val presenter = SettingsSubtitlePresenter(server.api()) { owner() }
            presenter.load()
            assertFalse(presenter.state.value.canSaveTranslator)
            presenter.setTranslatorKind("openai")
            assertFalse(presenter.state.value.canSaveTranslator)
            presenter.setTranslatorUrl(" http://x/v1 ")
            presenter.setTranslatorModel("m1")
            presenter.setTranslatorKey("secret")
            presenter.setTranslatorBuiltInAuto(true)
            assertTrue(presenter.state.value.canSaveTranslator)
            assertFalse(presenter.state.value.canTestTranslator)
            presenter.saveTranslator()
            val s = presenter.state.value
            assertTrue(s.translatorReady)
            assertFalse(s.translatorDirty)
            assertEquals("", s.translator.key)
            assertTrue(s.translator.hasKey)
            assertEquals(listOf("m1", "m2"), s.modelSuggestions)
            assertEquals(true, s.translatorTest?.ok)
            val body = server.request("PUT", "/api/v1/settings/subtitles/translator").body.orEmpty()
            assertTrue(body.contains(""""url":"http://x/v1""""))
            assertTrue(body.contains(""""key":"secret""""))
            assertFalse(body.contains("clear_key"))
            presenter.setTranslatorKey("")
            presenter.setTranslatorClearKey(true)
            presenter.saveTranslator()
            assertTrue(server.request("PUT", "/api/v1/settings/subtitles/translator").body.orEmpty().contains(""""clear_key":true"""))
        }

    @Test
    fun addingASiteNeedsItsAccountThenSavesAndTests() =
        runTest {
            val server = server()
            server.json(
                "POST",
                "/api/v1/subtitle-sites",
                """{"ID":9,"name":"OpenSubtitles.com","kind":"opensubtitles","url":"https://api.opensubtitles.com/api/v1","state":"enabled",
                    "limits":{"gap_ms":1000,"per_window":60,"window_seconds":3600,"downloads_per_day":0},"priority":25,"has_account":true}""",
                201,
            )
            server.json("POST", "/api/v1/subtitle-sites/9/test", """{"ok":true,"message":"The site answered: 12 subtitles for a test title.","found":12}""")
            val presenter = SettingsSubtitlePresenter(server.api()) { owner() }
            presenter.load()
            presenter.startAdd()
            assertEquals("gestdown", presenter.state.value.form?.kind)
            presenter.pickKind("opensubtitles")
            var form = presenter.state.value.form!!
            assertEquals("OpenSubtitles.com", form.name)
            assertEquals("https://api.opensubtitles.com/api/v1", form.url)
            assertTrue(form.enabled)
            presenter.setFormAccount("username", "ana")
            presenter.submitForm()
            form = presenter.state.value.form!!
            assertEquals(listOf("api_key", "password"), form.missing)
            assertTrue(server.requests.none { it.method == "POST" && it.path == "/api/v1/subtitle-sites" })
            presenter.setFormAccount("api_key", "k")
            presenter.setFormAccount("password", "p")
            presenter.setFormNumber("priority", "30")
            presenter.submitForm()
            val s = presenter.state.value
            assertNull(s.form)
            assertEquals(SettingsSubtitleNoteKind.SITE_SAVED, s.note?.kind)
            assertEquals("OpenSubtitles.com", s.note?.name)
            val row = s.siteRows.first()
            assertEquals(3L, row.id)
            val body = server.request("POST", "/api/v1/subtitle-sites").body.orEmpty()
            assertTrue(body.contains(""""priority":30"""))
            assertTrue(body.contains(""""api_key":"k""""))
            assertTrue(body.contains(""""limits":{"gap_ms":1000,"per_window":60,"window_seconds":3600,"downloads_per_day":0}"""))
            assertEquals(12, s.siteTests[9L]?.found)
        }

    @Test
    fun changeKeepsAccountWhenNothingIsTyped() {
        val kind = SettingsSubtitleKind(kind = "opensubtitles", needsAccount = true, account = listOf("api_key", "username", "password"))
        val form =
            ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleSiteForm(kind = "opensubtitles", isNew = false, hasAccount = true)
        assertEquals(emptyList(), settingsSubtitleAccountMissing(kind, form))
        assertEquals(listOf("password"), settingsSubtitleAccountMissing(kind, form.copy(username = "a", apiKey = "b")))
        assertEquals(listOf("api_key", "username", "password"), settingsSubtitleAccountMissing(kind, form.copy(hasAccount = false)))
    }

    @Test
    fun pausingSendsTheFullBodyWithEnabledFlipped() =
        runTest {
            val server = server()
            server.json(
                "PUT",
                "/api/v1/subtitle-sites/3",
                """{"ID":3,"name":"Gestdown","kind":"gestdown","url":"https://api.gestdown.info","state":"disabled"}""",
            )
            val presenter = SettingsSubtitlePresenter(server.api()) { owner() }
            presenter.load()
            presenter.togglePaused(3)
            val body = server.request("PUT", "/api/v1/subtitle-sites/3").body.orEmpty()
            assertTrue(body.contains(""""enabled":false"""))
            assertTrue(body.contains(""""downloads_per_day":20"""))
            assertTrue(body.contains(""""name":"Gestdown""""))
            assertEquals(SettingsSubtitleNoteKind.SITE_PAUSED, presenter.state.value.note?.kind)
        }

    @Test
    fun removeAndCleanReportFailures() =
        runTest {
            val server = server()
            server.json("DELETE", "/api/v1/subtitle-sites/3", """{"error":"gone"}""", 404)
            server.json("POST", "/api/v1/subtitles/clean", """{"queued":true}""", 202)
            val presenter = SettingsSubtitlePresenter(server.api()) { owner() }
            presenter.load()
            presenter.removeSite(3)
            assertEquals(SettingsSubtitleNoteKind.REMOVE_FAILED, presenter.state.value.note?.kind)
            assertEquals("Gestdown", presenter.state.value.note?.name)
            presenter.cleanAll()
            assertEquals(SettingsSubtitleNoteKind.CLEAN_QUEUED, presenter.state.value.note?.kind)
        }

    @Test
    fun siteRowsCarryTheDownloadsAndState() =
        runTest {
            val presenter = SettingsSubtitlePresenter(server().api()) { owner() }
            presenter.load()
            val row = presenter.state.value.siteRows.single()
            assertEquals("Gestdown", row.kindName)
            assertEquals(4, row.downloadsToday)
            assertEquals(20, row.downloadsMax)
            assertEquals(SettingsSubtitleSiteState.ON, row.state)
            assertTrue(row.offered)
        }
}
