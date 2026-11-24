package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.library.ByHandBlocker
import ch.orellbuehler.acolyte.feature.library.ByHandConfidence
import ch.orellbuehler.acolyte.feature.library.FolderHealth
import ch.orellbuehler.acolyte.feature.library.HeldShow
import ch.orellbuehler.acolyte.feature.library.LibraryCopiesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryDeletedPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryFilesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryFoldersPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryImportPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryNoticeKind
import ch.orellbuehler.acolyte.feature.library.LibraryReleasesPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryRenamePresenter
import ch.orellbuehler.acolyte.feature.library.LibraryStatsPresenter
import ch.orellbuehler.acolyte.feature.library.LibrarySubtitlesPresenter
import ch.orellbuehler.acolyte.feature.library.ReleaseVerdict
import ch.orellbuehler.acolyte.feature.library.RenameFailure
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryToolsTest {
    private fun FakeServer.art() =
        json("GET", "/api/v1/titles", """{"titles":[{"id":5,"kind":"movie","name":"Heat","artwork":["/a.jpg"]}]}""")

    @Test
    fun statsHideVagueFactsAndPartsThatAreOff() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/library/stats",
                """{"kinds":[
                    {"kind":"movies","titles":2,"files":4,"bytes":10,"facts":{"resolution":[{"value":"1080p","files":3,"bytes":8},
                     {"value":"unchecked","files":1,"bytes":2}],"hdr":[{"value":"unknown","files":4,"bytes":10}],"container":[]}},
                    {"kind":"music","titles":1,"files":1,"bytes":1,"facts":{}}]}""",
            )
            server.json("GET", "/api/v1/library/stats/watched", """{"enabled":false,"people":[]}""")
            val presenter = LibraryStatsPresenter(server.api()) { libraryAccount(off = listOf("music")) }
            presenter.load()
            val state = presenter.state.value
            assertEquals(listOf("movies"), state.kinds.map { it.kind })
            val movies = state.current!!
            assertEquals(listOf("resolution"), movies.facts.map { it.name })
            assertEquals(1, movies.unchecked)
            assertEquals(
                0.75f,
                movies.facts
                    .first()
                    .buckets
                    .first()
                    .share,
            )
            assertTrue(state.canClean)
            assertNull(state.cleanup)
        }

    @Test
    fun statsCleanupPicksChosenPeople() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/library/stats", """{"kinds":[]}""")
            server.json(
                "GET",
                "/api/v1/library/stats/watched",
                """{"enabled":true,"read_at":"2026-11-24T10:00:00Z","people":[{"name":"Sam"}]}""",
            )
            server.json(
                "GET",
                "/api/v1/library/watched-cleanup",
                """{"enabled":true,"days":30,"people":[{"key":"1:a","name":"Sam","chosen":true},{"key":"1:b","name":"Kim","chosen":false}],
                   "found":[{"id":8,"kind":"movie","name":"Heat","bytes":5}]}""",
            )
            server.json("POST", "/api/v1/titles/8/delete", """{"title_id":8,"applied":true}""")
            val presenter = LibraryStatsPresenter(server.api()) { libraryAccount() }
            presenter.load()
            assertEquals(listOf("1:a"), presenter.state.value.cleanupPicked)
            presenter.toggleCleanupPerson("1:b")
            assertTrue(server.request("GET", "/api/v1/library/watched-cleanup").query.contains("days=30"))
            presenter.removeWatched(8, deleteFiles = true, neverAdd = false)
            assertEquals("""{"delete_files":true,"never_add":false,"apply":true}""", server.request("POST", "/api/v1/titles/8/delete").body)
            assertTrue(
                presenter.state.value.cleanup!!
                    .found
                    .isEmpty(),
            )
            assertEquals(
                LibraryNoticeKind.TITLE_DELETED,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun copiesPageAndReview() =
        runTest {
            val server = FakeServer()
            server.art()
            server.json(
                "GET",
                "/api/v1/library/copies",
                """{"copies":[{"file_id":1,"version_id":2,"title_id":5,"title":"Heat","codec":"h264","relative_path":"Heat/Heat.mkv"},
                   {"file_id":3,"version_id":4,"title_id":6,"title":"Alien","codec":"xvid","protected":true}],
                   "total":120,"limit":50,"offset":0,"codecs":{"h264":100,"xvid":20}}""",
            )
            server.json(
                "POST",
                "/api/v1/library/copies/search/preview",
                """{"copies":[{"title":"Heat"}],"slot_count":1,"review_token":"t"}""",
            )
            server.json("POST", "/api/v1/library/copies/search", """{"job_ids":[1,2]}""", 202)
            val presenter = LibraryCopiesPresenter(server.api())
            presenter.load()
            var state = presenter.state.value
            assertEquals("/a.jpg", state.rows.first().poster)
            assertEquals("Heat.mkv", state.rows.first().fileName)
            assertEquals(listOf("all", "h264", "xvid"), state.codecs.map { it.id })
            assertTrue(state.hasNext)
            presenter.toggle(3)
            assertEquals(0, presenter.state.value.selectedCount)
            presenter.toggle(1)
            presenter.review()
            presenter.start()
            state = presenter.state.value
            assertEquals(2, state.started)
            assertEquals(0, state.selectedCount)
            presenter.setCodec("xvid")
            assertTrue(server.request("GET", "/api/v1/library/copies").query.contains("codec=xvid"))
        }

    @Test
    fun deletedRestoreAsksWhenThePlaceIsTaken() =
        runTest {
            val server = FakeServer()
            server.art()
            server.json(
                "GET",
                "/api/v1/holding",
                """{"files":[{"id":1,"path":"Heat/Heat.mkv","title_id":5,"title":"Heat","size":3,"deleted_at":"2026-11-20T00:00:00Z",
                   "expires_at":"2099-01-01T00:00:00Z"},{"id":2,"path":"Heat/old.mkv","title_id":5,"replaced":true}]}""",
            )
            server.json("GET", "/api/v1/settings/files", """{"holding_days":7}""")
            server.on("POST", "/api/v1/holding/restore") { request ->
                val body = (request.body as TextContent).text
                if (body.contains("\"how\":\"both\"")) {
                    respondJson("""{"results":[{"id":1,"result":"restored"}]}""")
                } else {
                    respondJson("""{"results":[{"id":1,"result":"a file is already at x","taken":true,"can_swap":true}]}""")
                }
            }
            val presenter = LibraryDeletedPresenter(server.api(), { libraryAccount() }, 0)
            presenter.load()
            var state = presenter.state.value
            assertEquals(2, state.count)
            assertTrue(state.hasBoth)
            assertEquals(7, state.holdingDays)
            presenter.setShow(HeldShow.REPLACED)
            assertEquals(
                listOf(2L),
                presenter.state.value.rows
                    .map { it.file.id },
            )
            presenter.setShow(HeldShow.ALL)
            presenter.toggle(1)
            presenter.restore()
            state = presenter.state.value
            assertEquals("Heat.mkv", state.question?.name)
            assertTrue(state.question!!.canSwap)
            presenter.answer("both")
            assertNull(presenter.state.value.question)
            assertEquals(
                LibraryNoticeKind.RESTORED,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun heldListsTagsAndStepsBackAPage() =
        runTest {
            val server = FakeServer()
            server.art()
            server.json(
                "GET",
                "/api/v1/held",
                """{"releases":[{"id":9,"name":"Heat.1995.2160p","title_id":5,"title_name":"Heat","version":"Main","state":"held","outcome":"held",
                   "passed":true,"points":40,"rank":2,"reasons":[{"effect":"hold","text":"waits 2 h"},{"effect":"pass","text":"ok"}],
                   "read":{"resolution":2160}}],"total":26,"limit":25,"offset":25}""",
            )
            server.json("DELETE", "/api/v1/held/9", """{"removed":1}""")
            val presenter = LibraryReleasesPresenter(server.api(), turnedDown = false)
            presenter.nextPage()
            presenter.load()
            presenter.nextPage()
            val row =
                presenter.state.value.rows
                    .single()
            assertEquals(ReleaseVerdict.HELD, row.verdict)
            assertEquals("", row.version)
            assertEquals(listOf("4K"), row.tags)
            assertEquals(listOf("waits 2 h"), row.reasons)
            assertEquals("/a.jpg", row.poster)
            presenter.remove(9)
            assertEquals(0, presenter.state.value.offset)
        }

    @Test
    fun subtitlesSearchAllFollowsJobs() =
        runTest {
            val server = FakeServer()
            server.art()
            server.json("GET", "/api/v1/subtitles/languages", """{"languages":[{"code":"de","name":"German"}]}""")
            server.json(
                "GET",
                "/api/v1/subtitles/missing",
                """{"missing":[{"file_id":1,"title_id":5,"title":"Heat","kind":"movie","language":"de","tries":2}],"total":1,"titles":[]}""",
            )
            server.json("POST", "/api/v1/subtitles/missing/search", """{"queued":1,"job_ids":[4]}""", 202)
            server.json("GET", "/api/v1/jobs", """{"jobs":[{"ID":4,"State":"done"}]}""")
            val presenter = LibrarySubtitlesPresenter(server.api())
            presenter.load()
            assertEquals(
                "German",
                presenter.state.value.rows
                    .single()
                    .languageName,
            )
            presenter.setLanguage("de")
            presenter.searchAll()
            assertEquals("""{"language":"de"}""", server.request("POST", "/api/v1/subtitles/missing/search").body)
            assertFalse(presenter.state.value.searching)
            assertEquals(1, presenter.state.value.searchDone)
        }

    @Test
    fun renameFollowsTheJob() =
        runTest {
            val server = FakeServer()
            server.art()
            server.json(
                "GET",
                "/api/v1/rename",
                """{"plan_hash":"h","steps":[{"file_id":1,"title_id":5,"title_name":"Heat","from":"Heat/a.mkv","to":"Heat/Heat (1995).mkv"},
                   {"file_id":2,"title_id":5,"from":"Heat/b.mkv","skip":"found file"},{"file_id":3,"from":"x.mkv","to":"x.mkv"}]}""",
            )
            server.json("POST", "/api/v1/rename", """{"job_id":7}""", 202)
            server.json("GET", "/api/v1/jobs/7", """{"ID":7,"State":"done","Progress":1,"Result":{"renamed":1}}""")
            val presenter = LibraryRenamePresenter(server.api()) { libraryAccount() }
            presenter.load()
            val state = presenter.state.value
            assertEquals("a.mkv", state.changes.single().from)
            assertEquals("Heat (1995).mkv", state.changes.single().to)
            assertEquals(1, state.skipped.size)
            assertEquals(1, state.same)
            presenter.rename()
            assertEquals("""{"plan_hash":"h"}""", server.request("POST", "/api/v1/rename").body)
            assertEquals(
                LibraryNoticeKind.RENAMED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(
                1,
                presenter.state.value.notice
                    ?.count,
            )
            server.json("GET", "/api/v1/jobs/7", """{"ID":7,"State":"failed","Error":{"code":"plan_changed","message":"x"}}""")
            presenter.rename()
            assertEquals(RenameFailure.PLAN_CHANGED, presenter.state.value.failure)
        }

    @Test
    fun importScansAndAdoptsTickedFolders() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/library-folders",
                """{"library_folders":[{"id":3,"kind":"movie","path":"/movies","label":"Movies"}]}""",
            )
            server.json("POST", "/api/v1/library-folders/3/scan", """{"job_id":1,"dry_run":true}""", 202)
            server.json(
                "GET",
                "/api/v1/jobs/1",
                """{"ID":1,"State":"done","Result":{"plan":{"kind":"movie","library_folder_id":3,"path":"/movies","total":3,"folders":[
                    {"folder":"Heat (1995)","parsed_name":"Heat","parsed_year":1995,"status":"matched","match":{"tmdb_id":"949","name":"Heat","year":1995,"score":0.95},"others":[]},
                    {"folder":"Alien","parsed_name":"Alien","status":"unsure","match":{"tmdb_id":"348","name":"Alien","year":1979,"score":0.6},
                     "others":[{"tmdb_id":"8077","name":"Alien 3","year":1992,"score":0.5}]},
                    {"folder":"Docs","parsed_name":"Docs","status":"no_video","others":[]}],"counts":{}}}}""",
            )
            server.json("GET", "/api/v1/library-folders/3/tidy", """{"hash":"x","moves":[],"blocked":[],"undo_files":2}""")
            server.json("POST", "/api/v1/library-folders/3/adopt", """{"job_id":2}""", 202)
            server.json(
                "GET",
                "/api/v1/jobs/2",
                """{"ID":2,"State":"done","Result":{"results":[{"folder":"Heat (1995)","title_id":40},{"folder":"Alien","title_id":41}]}}""",
            )
            server.json("GET", "/api/v1/titles/lookup", """{"movies":[{"name":"Aliens","year":1986,"ids":{"tmdb":"679"}}]}""")
            val presenter = LibraryImportPresenter(server.api()) { true }
            presenter.load()
            assertEquals(3L, presenter.state.value.folderId)
            presenter.scan()
            var state = presenter.state.value
            assertEquals(listOf("Alien", "Heat (1995)", "Docs"), state.rows.map { it.folder })
            assertEquals(1, state.tickedCount)
            assertEquals(2, state.undoFiles)
            assertFalse(state.rows.last().canTick)
            presenter.openLookup("Alien")
            assertEquals(
                "Alien",
                presenter.state.value.lookup
                    ?.term,
            )
            presenter.lookup("Aliens 1986")
            assertTrue(server.request("GET", "/api/v1/titles/lookup").query.contains("year=1986"))
            presenter.useLookup(
                presenter.state.value.lookup!!
                    .results
                    .single(),
            )
            state = presenter.state.value
            assertEquals(
                "Aliens",
                state.rows
                    .first()
                    .pick
                    ?.name,
            )
            assertEquals(2, state.tickedCount)
            presenter.adopt()
            val body = server.request("POST", "/api/v1/library-folders/3/adopt").body.orEmpty()
            assertTrue(body.contains("\"tmdb_id\":\"679\""))
            assertTrue(body.contains("\"follow\":true"))
            assertEquals(
                2,
                presenter.state.value.outcome
                    ?.adopted,
            )
            assertEquals(0, presenter.state.value.tickedCount)
        }

    @Test
    fun filesTickSureGuessesAndImportInParts() =
        runTest {
            val server = FakeServer()
            server.json(
                "POST",
                "/api/v1/manual-import/scan",
                """{"path":"/dl/x","more":false,"files":[
                    {"path":"/dl/x/a.mkv","name":"a.mkv","size":5,"kind":"video","guess":{"title_id":5,"title_name":"Heat","title_kind":"movie","version_id":2,"confidence":0.9}},
                    {"path":"/dl/x/b.mkv","name":"b.mkv","size":5,"kind":"video","guess":{"title_id":6,"title_name":"Dark","title_kind":"show","confidence":0.5}},
                    {"path":"/dl/x/c.mkv","name":"c.mkv","size":5,"kind":"video","guess":{"title_id":5,"title_kind":"movie","has_file":true,"confidence":0.9}}]}""",
            )
            server.json(
                "POST",
                "/api/v1/manual-import",
                """{"placed":1,"results":[{"path":"/dl/x/a.mkv","outcome":"placed","placed_by":"hardlink"}]}""",
            )
            val presenter = LibraryFilesPresenter(server.api(), { libraryAccount() }, "/dl/x")
            presenter.load()
            var state = presenter.state.value
            assertTrue(state.allowed)
            assertTrue(state.canBrowse)
            assertEquals(listOf(true, false, false), state.rows.map { it.ticked })
            assertEquals(ByHandConfidence.LIKELY, state.rows[0].confidence)
            assertEquals(ByHandBlocker.NO_EPISODE, state.rows[1].blocker)
            assertEquals(ByHandBlocker.HAS_FILE, state.rows[2].blocker)
            presenter.setReplace("/dl/x/c.mkv", true)
            assertEquals(1, presenter.state.value.replaceCount)
            presenter.toggle("/dl/x/c.mkv")
            presenter.setMode("move")
            presenter.importFiles()
            assertEquals(
                """{"items":[{"path":"/dl/x/a.mkv","title_id":5,"version_id":2,"mode":"move"}]}""",
                server.request("POST", "/api/v1/manual-import").body,
            )
            state = presenter.state.value
            assertEquals("placed", state.rows[0].result?.outcome)
            assertFalse(state.rows[0].ticked)
            assertEquals(LibraryNoticeKind.IMPORTED, state.notice?.kind)
        }

    @Test
    fun foldersGroupAndAskForPasswordBeforeHandsOff() =
        runTest {
            val server = FakeServer()
            server.json(
                "GET",
                "/api/v1/library-folders",
                """{"library_folders":[{"id":1,"kind":"movie","path":"/data/media/movies","label":"Movies","health":"no_hardlink","copies_expected":true,
                    "default_pick_rules_id":4},{"id":2,"kind":"show","path":"/shows","health":"missing","default_pick_rules_id":null}]}""",
            )
            server.json("GET", "/api/v1/pick-rules", """{"pick_rules":[{"id":4,"name":"Remux","kinds":["movie"]}]}""")
            server.json("GET", "/api/v1/settings/import-folders", """{"extra_folders":["/in"],"watch_folders":[]}""")
            server.json("GET", "/api/v1/settings/side-files", """{"on":true,"types":["srt"]}""")
            server.json("GET", "/api/v1/tags", """{"tags":[{"label":"anime","titles":2}]}""")
            var proven = false
            server.on("PUT", "/api/v1/settings/hands-off") {
                if (proven) {
                    respondJson("""{"on":true}""")
                } else {
                    respondJson("""{"error":"confirm your local password again before changing hands-off mode","code":"reauth"}""", 403)
                }
            }
            var refreshed = false
            val presenter =
                LibraryFoldersPresenter(
                    server.api(),
                    { libraryAccount() },
                    { password, _ ->
                        proven = password == "secret"
                        if (proven) null else ApiError(ApiErrorCode.UNAUTHORIZED, 401)
                    },
                    { refreshed = true },
                )
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals(listOf("movie", "show"), state.groups.map { it.kind })
            val movies =
                state.groups
                    .first()
                    .folders
                    .single()
            assertEquals(FolderHealth.COPIES_EXPECTED, movies.health)
            assertEquals("Remux", movies.rulesName)
            assertEquals("…/media/movies", movies.shortPath)
            assertEquals(
                FolderHealth.MISSING,
                state.groups
                    .last()
                    .folders
                    .single()
                    .health,
            )
            assertEquals(listOf("anime"), state.tagSuggestions)
            assertEquals(1, presenter.rulesFor("movie").size)
            presenter.setHandsOff(true)
            assertTrue(presenter.state.value.needsPassword)
            presenter.confirmPassword("wrong", "")
            assertTrue(presenter.state.value.passwordError != null)
            presenter.confirmPassword("secret", "")
            assertFalse(presenter.state.value.needsPassword)
            assertTrue(presenter.state.value.handsOff)
            assertTrue(refreshed)
            assertEquals(
                LibraryNoticeKind.HANDS_OFF_ON,
                presenter.state.value.notice
                    ?.kind,
            )
        }

    @Test
    fun addFolderSendsOnlyChosenFields() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/library-folders", """{"library_folders":[]}""")
            server.json("POST", "/api/v1/library-folders", """{"id":5,"kind":"show","path":"/shows","health":"ok"}""", 201)
            val presenter = LibraryFoldersPresenter(server.api(), { libraryAccount() }, { _, _ -> null }, {})
            presenter.load()
            assertTrue(presenter.state.value.isEmpty)
            assertTrue(presenter.add("show", " /shows ", "", 0, listOf("Kids Only"), onlyTagged = true, lookOnly = false, flat = true))
            assertEquals(
                """{"path":"/shows","kind":"show","new_title_tags":["kids-only"],"only_tagged_sources":true}""",
                server.request("POST", "/api/v1/library-folders").body,
            )
            assertEquals(1, presenter.state.value.total)
        }
}
