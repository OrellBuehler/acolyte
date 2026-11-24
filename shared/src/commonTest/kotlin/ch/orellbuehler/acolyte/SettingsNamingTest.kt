package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingChoice
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingCleanNote
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingNumberError
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingPresenter
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingSwitch
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingText
import ch.orellbuehler.acolyte.feature.acquisition.settingsNamingInsert
import ch.orellbuehler.acolyte.feature.acquisition.settingsNamingNumberError
import ch.orellbuehler.acolyte.session.Role
import io.ktor.http.content.TextContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsNamingTest {
    private val naming =
        """{"naming":{"file_pattern":"{title} ({year}) {quality}","folder_pattern":"{title} ({year})","colon":"smart","illegal":"replace",
        "rename_on_import":true,"group_by_genre":false,"group_by_letter":false,"show_group_by_letter":false,
        "show_rename_on_import":true,"show_colon":"smart","show_illegal":"replace","music_rename_on_import":true,
        "music_colon":"smart","music_illegal":"replace","show_folder_pattern":"{title} ({year})","season_folders":true,
        "season_folder_pattern":"Season {season}","specials_folder":"Specials",
        "episode_pattern":"{title} - {episode} - {episode_title} {quality}","daily_pattern":"{title} - {air_date}",
        "anime_pattern":"{title} - {absolute}","multi_episode_style":"range","air_date_style":"dash","absolute_digits":3,
        "adopt_scan_limit":5000,"music_adopt_scan_limit":50,"artist_folder_pattern":"{artist}",
        "album_folder_pattern":"{album} ({year})","track_pattern":"{track} - {track_title}",
        "multi_disc_track_pattern":"{disc}-{track} - {track_title}","disc_folders":false,"disc_folder_pattern":"Disc {disc}",
        "music_read_tags":true,"title_language":""},
        "words":{"title":"the title","year":"the year","quality":"the quality","season":"season","episode":"episode",
        "artist":"the artist","track":"track number","edition":"edition","tmdb":"tmdb id"},
        "example":"Dune - Part Two (2024) Bluray-1080p","example_folder":"Dune - Part Two (2024)",
        "example_episodes":{"show_folder":"Copper Valley (2020)","episode":"Season 1/Copper Valley - S01E02","multi":"","special":"Specials/x",
        "daily":"d","anime":"a"},
        "example_music":{"artist_folder":"The Lab Lanterns","album_folder":"Small Hours (2021)","track":"03 - Glow","multi_disc":"2-03 - Glow",
        "various":"v"}}"""
    private val files =
        """{"holding_days":7,"min_free_mb":100,"skip_free_space_check":false,"unkeep_deleted_movies":false,"unkeep_deleted_episodes":true}"""
    private val example = """{"example":"E1","example_folder":"F1","example_episodes":{"episode":"ep"},"example_music":{"track":"tr"}}"""

    private fun server(): FakeServer {
        val server = FakeServer()
        server.json("GET", "/api/v1/settings/naming", naming)
        server.json("GET", "/api/v1/settings/files", files)
        server.on("PUT", "/api/v1/settings/naming") { respondJson(naming) }
        server.on("PUT", "/api/v1/settings/files") { respondJson((it.body as TextContent).text) }
        server.json("POST", "/api/v1/settings/naming/example", example)
        return server
    }

    private fun presenter(
        server: FakeServer,
        role: Role = Role.OWNER,
        perms: List<String> = emptyList(),
    ) = SettingsNamingPresenter(server.api()) { testAccount(role, perms) }

    @Test
    fun loadsBothAnswersAndStartsClean() =
        runTest {
            val p = presenter(server())
            p.load()
            val s = p.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertFalse(s.dirty)
            assertEquals("{title} ({year}) {quality}", s.text(SettingsNamingText.FILE_PATTERN))
            assertEquals("7", s.number(SettingsNamingNumber.HOLDING_DAYS))
            assertTrue(s.isOn(SettingsNamingSwitch.UNKEEP_EPISODES))
            assertEquals("3", s.choice(SettingsNamingChoice.ABSOLUTE_DIGITS))
            assertEquals("Dune - Part Two (2024) Bluray-1080p", s.example)
            assertEquals(listOf("show_folder", "episode", "special", "daily", "anime"), s.episodeRows.map { it.key })
            assertEquals(listOf("artist_folder", "album_folder", "track", "multi_disc", "various"), s.musicRows.map { it.key })
            assertTrue(s.canClean)
            assertEquals("Dune - Part Two (2024)/Dune - Part Two (2024) Bluray-1080p", s.exampleFor(SettingsNamingText.FILE_PATTERN))
            assertEquals("Copper Valley (2020)", s.exampleFor("show_folder_pattern"))
            assertEquals("2-03 - Glow", s.exampleFor("disc_folder_pattern"))
            assertEquals("Specials/x", s.exampleFor("specials_folder"))
        }

    @Test
    fun aPersonWithoutThePageGetsNotAllowed() =
        runTest {
            val p = presenter(server(), Role.ADMIN, listOf("manage_library"))
            p.load()
            assertFalse(p.state.value.allowed)
        }

    @Test
    fun genreAndLetterGroupingExcludeEachOther() =
        runTest {
            val p = presenter(server())
            p.load()
            p.setSwitch(SettingsNamingSwitch.GROUP_BY_GENRE, true)
            assertTrue(p.state.value.isOn("group_by_genre"))
            p.setSwitch(SettingsNamingSwitch.GROUP_BY_LETTER, true)
            assertTrue(p.state.value.isOn("group_by_letter"))
            assertFalse(p.state.value.isOn("group_by_genre"))
            p.setSwitch("group_by_genre", true)
            assertFalse(p.state.value.isOn("group_by_letter"))
            assertTrue(p.state.value.dirty)
        }

    @Test
    fun serverStylesFillTheFourPatterns() =
        runTest {
            val p = presenter(server())
            p.load()
            p.applyStyle("jellyfin")
            val s = p.state.value
            assertEquals("{title} ({year}) [tmdbid-{tmdb}]", s.text(SettingsNamingText.FOLDER_PATTERN))
            assertEquals(
                "{title} ({year}) [tmdbid-{tmdb}] - {{edition}} [{quality_full}][{audio} {audio_channels}][{hdr_type}][{video_codec}]-{group}",
                s.text(SettingsNamingText.FILE_PATTERN),
            )
            assertEquals("{title} ({year}) [tvdbid-{tvdb}]", s.text(SettingsNamingText.SHOW_FOLDER_PATTERN))
            assertEquals(
                "{title} ({year}) - {episode} - {episode_title} [{quality_full}][{audio} {audio_channels}][{hdr_type}][{video_codec}]-{group}",
                s.text(SettingsNamingText.EPISODE_PATTERN),
            )
            p.applyStyle("plex")
            assertEquals("{title} ({year}) {tmdb-{tmdb}}", p.state.value.text("folder_pattern"))
            p.applyStyle("emby")
            assertEquals("{title} ({year}) [tvdb-{tvdb}]", p.state.value.text("show_folder_pattern"))
        }

    @Test
    fun numbersAreCheckedAsTheWebPageDoes() =
        runTest {
            val p = presenter(server())
            p.load()
            p.setNumber(SettingsNamingNumber.HOLDING_DAYS, "")
            assertEquals(SettingsNamingNumberError.DAYS, p.state.value.numberError(SettingsNamingNumber.HOLDING_DAYS))
            assertFalse(p.state.value.canSave)
            p.setNumber(SettingsNamingNumber.HOLDING_DAYS, "9999")
            assertEquals("daysmax", p.state.value.numberErrorKey("holding_days"))
            p.setNumber(SettingsNamingNumber.HOLDING_DAYS, "0")
            assertNull(p.state.value.numberError(SettingsNamingNumber.HOLDING_DAYS))
            p.setNumber(SettingsNamingNumber.MIN_FREE_MB, "99")
            assertEquals(SettingsNamingNumberError.FREE, p.state.value.numberError(SettingsNamingNumber.MIN_FREE_MB))
            p.setNumber(SettingsNamingNumber.MIN_FREE_MB, "100")
            assertNull(p.state.value.numberError(SettingsNamingNumber.MIN_FREE_MB))
            p.setNumber("adopt_scan_limit", "9")
            assertEquals("range", p.state.value.numberErrorKey("adopt_scan_limit"))
            assertFalse(p.state.value.canSave)
            p.setNumber("adopt_scan_limit", "100001")
            assertEquals(SettingsNamingNumberError.RANGE, p.state.value.numberError(SettingsNamingNumber.ADOPT_SCAN_LIMIT))
            p.setNumber("adopt_scan_limit", "12ab")
            assertEquals("12", p.state.value.number("adopt_scan_limit"))
            assertEquals(SettingsNamingNumberError.RANGE, settingsNamingNumberError(SettingsNamingNumber.MUSIC_ADOPT_SCAN_LIMIT, "5"))
            p.setNumber("adopt_scan_limit", "5000")
            p.setNumber("music_adopt_scan_limit", "50")
            assertTrue(p.state.value.dirty)
            p.setNumber(SettingsNamingNumber.HOLDING_DAYS, "7")
            p.setNumber(SettingsNamingNumber.MIN_FREE_MB, "100")
            assertFalse(p.state.value.dirty)
        }

    @Test
    fun wordsGoInAtTheCursorOrTheEnd() {
        val base = "{title} ({year})"
        assertEquals("{title} ({year}) {quality}", settingsNamingInsert(base, base.length, base.length, "quality").text)
        val middle = settingsNamingInsert("{title} X", 8, 9, "year")
        assertEquals("{title} {year}", middle.text)
        assertEquals(14, middle.cursor)
        assertEquals("{title}", settingsNamingInsert("", 0, 0, "title").text)
        assertEquals("a{b}c", settingsNamingInsert("ac", 1, 1, "b").text)
        assertEquals("{x} {b}", settingsNamingInsert("{x}", 99, 99, "b").text)
    }

    @Test
    fun wordsAreOfferedPerKindOfPatternAndSearchable() =
        runTest {
            val p = presenter(server())
            p.load()
            val s = p.state.value
            assertEquals(listOf("edition", "quality", "title", "tmdb", "year"), s.words(SettingsNamingText.FILE_PATTERN, "").map { it.key })
            assertEquals(
                listOf("episode", "season"),
                s.words("daily_pattern", "").map { it.key }.filter { it in listOf("episode", "season") },
            )
            assertEquals(listOf("season"), s.words(SettingsNamingText.SEASON_FOLDER_PATTERN, "").map { it.key })
            assertEquals(listOf("artist", "edition", "track", "year"), s.words(SettingsNamingText.TRACK_PATTERN, "").map { it.key })
            assertEquals(listOf("tmdb"), s.words(SettingsNamingText.FILE_PATTERN, "TMDB").map { it.key })
            assertEquals(listOf("year"), s.words(SettingsNamingText.FILE_PATTERN, "the year").map { it.key })
            assertTrue(s.words(SettingsNamingText.SPECIALS_FOLDER, "").isEmpty())
            val cursor = p.insertWord("file_pattern", "edition", 0, 0)
            assertEquals(9, cursor)
            assertTrue(
                p.state.value
                    .text("file_pattern")
                    .startsWith("{edition}{title}"),
            )
            assertEquals(0, p.insertWord("specials_folder", "x", 0, 0))
        }

    @Test
    fun theLanguageListHasNoEnglish() {
        val s = presenter(server()).state.value
        assertFalse(s.languages.any { it.code == "en" })
        assertEquals("Deutsch", s.languages.first { it.code == "de" }.name)
        assertEquals("", SettingsNamingChoice.TITLE_LANGUAGE.options.first())
        assertEquals(s.languages.size + 1, SettingsNamingChoice.TITLE_LANGUAGE.options.size)
    }

    @Test
    fun saveSendsNamingThenFilesAndGoesClean() =
        runTest {
            val server = server()
            val p = presenter(server)
            p.load()
            p.setText(SettingsNamingText.FILE_PATTERN, "{title}")
            p.setChoice("colon", "dash")
            p.setNumber("holding_days", "30")
            p.setSwitch("unkeep_deleted_movies", true)
            assertTrue(p.state.value.canSave)
            p.save()
            val sent = server.requests.filter { it.method == "PUT" }
            assertEquals(listOf("/api/v1/settings/naming", "/api/v1/settings/files"), sent.map { it.path })
            assertTrue(sent[0].body.orEmpty().contains(""""file_pattern":"{title}""""))
            assertTrue(sent[0].body.orEmpty().contains(""""colon":"dash""""))
            assertTrue(sent[0].body.orEmpty().contains(""""adopt_scan_limit":5000"""))
            assertTrue(sent[1].body.orEmpty().contains(""""holding_days":30"""))
            assertTrue(sent[1].body.orEmpty().contains(""""unkeep_deleted_movies":true"""))
            val s = p.state.value
            assertEquals(1, s.savedCount)
            assertFalse(s.dirty)
            assertEquals(30, s.savedFiles.holdingDays)
        }

    @Test
    fun aServerRefusalOfTheNamingKeepsTheDraftAndSkipsTheFiles() =
        runTest {
            val server = server()
            server.json("PUT", "/api/v1/settings/naming", """{"error":"the file name pattern is empty"}""", 400)
            val p = presenter(server)
            p.load()
            p.setText(SettingsNamingText.FILE_PATTERN, "")
            p.setNumber("holding_days", "1")
            p.save()
            val s = p.state.value
            assertEquals(ApiErrorCode.BAD_REQUEST, s.saveError?.code)
            assertEquals("the file name pattern is empty", s.saveError?.message)
            assertTrue(s.dirty)
            assertEquals(0, server.requests.count { it.path == "/api/v1/settings/files" && it.method == "PUT" })
        }

    @Test
    fun discardGoesBackToWhatWasLoaded() =
        runTest {
            val p = presenter(server())
            p.load()
            p.setText(SettingsNamingText.TRACK_PATTERN, "x")
            p.setNumber("holding_days", "")
            p.setSwitch("skip_free_space_check", true)
            assertTrue(p.state.value.dirty)
            p.discard()
            val s = p.state.value
            assertFalse(s.dirty)
            assertEquals("7", s.number("holding_days"))
            assertFalse(s.isOn("skip_free_space_check"))
        }

    @Test
    fun theLiveExampleWaitsForTheLastEditAndDropsStaleAnswers() =
        runTest {
            val server = server()
            val p = presenter(server)
            p.load()
            val job = backgroundScope.launch { p.followExample() }
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(0, server.requests.count { it.path.endsWith("/example") })
            p.setText(SettingsNamingText.FILE_PATTERN, "{title} a")
            advanceTimeBy(200)
            p.setText(SettingsNamingText.FILE_PATTERN, "{title} ab")
            advanceTimeBy(200)
            runCurrent()
            assertEquals(0, server.requests.count { it.path.endsWith("/example") })
            advanceTimeBy(60)
            eventually { p.state.value.example == "E1" }
            val posted = server.requests.filter { it.path.endsWith("/example") }
            assertEquals(1, posted.size)
            assertTrue(posted[0].body.orEmpty().contains(""""file_pattern":"{title} ab""""))
            assertFalse(posted[0].body.orEmpty().contains("adopt_scan_limit"))
            assertEquals("F1", p.state.value.exampleFolder)
            assertEquals(
                listOf("episode"),
                p.state.value.episodeRows
                    .map { it.key },
            )
            job.cancel()
        }

    @Test
    fun anExampleErrorShowsUnderTheExampleAndBlocksSave() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/settings/naming/example", """{"error":"the file name pattern needs {title}"}""", 400)
            val p = presenter(server)
            p.load()
            val job = backgroundScope.launch { p.followExample() }
            runCurrent()
            p.setText(SettingsNamingText.FILE_PATTERN, "{year}")
            advanceTimeBy(300)
            eventually { p.state.value.exampleError != null }
            assertEquals(
                "the file name pattern needs {title}",
                p.state.value.exampleError
                    ?.message,
            )
            assertTrue(p.state.value.dirty)
            assertFalse(p.state.value.canSave)
            server.json("POST", "/api/v1/settings/naming/example", example)
            p.setText(SettingsNamingText.FILE_PATTERN, "{title}")
            advanceTimeBy(300)
            eventually { p.state.value.exampleError == null && p.state.value.example == "E1" }
            assertTrue(p.state.value.canSave)
            job.cancel()
        }

    @Test
    fun clearingTheHoldingFolderTellsWhatWillHappen() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/holding/clean", """{"job_id":4}""", 202)
            val p = presenter(server)
            p.load()
            p.cleanHolding()
            var s = p.state.value
            assertEquals(SettingsNamingCleanNote.REMOVING, s.cleanNote)
            assertEquals(7, s.cleanDays)
            assertEquals(1, s.cleanCount)
            p.setNumber("holding_days", "0")
            p.save()
            p.cleanHolding()
            s = p.state.value
            assertEquals(SettingsNamingCleanNote.NOTHING, s.cleanNote)
            assertEquals(2, s.cleanCount)
        }

    @Test
    fun handsOffRefusesTheCleanAsAnError() =
        runTest {
            val server = server()
            server.json("POST", "/api/v1/holding/clean", """{"error":"hands-off mode is on"}""", 409)
            val p = presenter(server)
            p.load()
            p.cleanHolding()
            val s = p.state.value
            assertEquals(ApiErrorCode.HANDS_OFF, s.cleanError?.code)
            assertNull(s.cleanNote)
            assertFalse(s.cleaning)
        }

    @Test
    fun aLoadFailureKeepsTheScreenInTheFailedState() =
        runTest {
            val server = server()
            server.json("GET", "/api/v1/settings/files", """{"error":"boom"}""", 500)
            val p = presenter(server)
            p.load()
            assertEquals(Phase.FAILED, p.state.value.status.phase)
        }
}
