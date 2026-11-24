package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.model.EventActor
import ch.orellbuehler.acolyte.api.model.EventRecord
import ch.orellbuehler.acolyte.api.model.EventsPage
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.downloads.Day
import ch.orellbuehler.acolyte.feature.downloads.DownloadStepKey
import ch.orellbuehler.acolyte.feature.downloads.HistoryFactKind
import ch.orellbuehler.acolyte.feature.downloads.HistoryFilter
import ch.orellbuehler.acolyte.feature.downloads.HistoryLineKind
import ch.orellbuehler.acolyte.feature.downloads.HistoryNotice
import ch.orellbuehler.acolyte.feature.downloads.HistoryPresenter
import ch.orellbuehler.acolyte.feature.downloads.HistoryRedo
import ch.orellbuehler.acolyte.feature.downloads.HistoryRules
import ch.orellbuehler.acolyte.feature.downloads.HistoryStep
import ch.orellbuehler.acolyte.feature.downloads.HistoryType
import ch.orellbuehler.acolyte.feature.downloads.HistoryVerb
import ch.orellbuehler.acolyte.feature.downloads.HistoryWhen
import ch.orellbuehler.acolyte.feature.downloads.HistoryWhoFilter
import ch.orellbuehler.acolyte.feature.downloads.StepState
import ch.orellbuehler.acolyte.feature.downloads.WhoKind
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class HistoryTest {
    private fun data(json: String): JsonObject = ApiJson.lenient.parseToJsonElement(json).jsonObject

    private fun event(
        id: Long,
        kind: String,
        download: Long = 0,
        at: String = "2026-11-24T10:00:00Z",
        data: String = "{}",
        slot: Long = 0,
        actor: EventActor = EventActor("system"),
    ) = EventRecord(
        id = id,
        kind = kind,
        downloadRef = download,
        at = at,
        data = data(data),
        slotRef = slot,
        titleRef = 40,
        titleName = "Heat",
        actor = actor,
    )

    @Test
    fun foldsADownloadsEventsIntoOneRow() {
        val events =
            listOf(
                event(5, "import_done", download = 9, data = """{"size_bytes":2000000000}"""),
                event(4, "download_finished", download = 9),
                event(3, "download_sent", download = 9, data = """{"release_id":77,"release":"Heat.1995.2160p.mkv"}"""),
                event(2, "release_chosen", data = """{"release_id":77,"quality":"2160p"}"""),
                event(1, "title_added"),
            )
        val rows = HistoryRules.group(events)
        assertEquals(listOf("d9", "e1"), rows.map { it.first })
        assertEquals(4, rows[0].second.size)
        val steps = HistoryRules.steps(rows[0].second)
        assertEquals(listOf(StepState.DONE, StepState.DONE, StepState.DONE), steps.map { it.state })
        assertEquals(HistoryVerb.IMPORTED, HistoryRules.rowVerb(rows[0].second, steps))
        assertEquals("2160p · 2 GB", HistoryRules.extraOf(rows[0].second, HistoryVerb.IMPORTED))
        assertEquals(77L, HistoryRules.releaseOf(rows[0].second))
        assertEquals(HistoryRedo.REPLACE, HistoryRules.redo(rows[0].second, HistoryVerb.IMPORTED))
        val facts = HistoryRules.facts(rows[0].second).map { it.kind }
        assertTrue(HistoryFactKind.QUALITY in facts && HistoryFactKind.RELEASE in facts && HistoryFactKind.SIZE in facts)
    }

    @Test
    fun marksFailedAndWaitingSteps() {
        val failed =
            listOf(
                event(2, "download_failed", download = 3, data = """{"reason":"the downloader says error."}"""),
                event(1, "download_sent", download = 3),
            )
        val steps = HistoryRules.steps(failed)
        assertEquals(StepState.FAILED, steps.first { it.key == DownloadStepKey.DOWNLOADED }.state)
        assertEquals(HistoryVerb.FAILED, HistoryRules.rowVerb(failed, steps))
        assertEquals(HistoryRedo.FIND_ANOTHER, HistoryRules.redo(failed, HistoryVerb.FAILED))
        val needs =
            listOf(
                event(3, "needs_decision", data = """{"download_id":4,"reason":"edition is unknown"}"""),
                event(2, "download_finished", download = 4),
            )
        val folded = HistoryRules.group(needs)
        assertEquals(1, folded.size)
        val needSteps = HistoryRules.steps(folded[0].second)
        assertEquals(StepState.NEEDS, needSteps[2].state)
        assertEquals(HistoryVerb.NEEDS_YOU, HistoryRules.rowVerb(folded[0].second, needSteps))
        val stuck = listOf(event(3, "needs_decision", slot = 8), event(2, "import_failed", download = 5, slot = 8))
        assertEquals(1, HistoryRules.group(stuck).size)
    }

    @Test
    fun picksVerbsAndLines() {
        assertEquals(HistoryVerb.UPGRADED, HistoryRules.verbOf("file_replaced"))
        assertEquals(HistoryVerb.TURNED_DOWN, HistoryRules.verbOf("release_turned_down"))
        assertEquals(HistoryVerb.SUBTITLES_WAIT, HistoryRules.verbOf("subtitle_none_good_enough"))
        assertEquals(HistoryVerb.SYSTEM, HistoryRules.verbOf("signed_in"))
        assertEquals(HistoryVerb.MEDIA_SERVER_FAILED, HistoryRules.verbOf("media_server_unreachable"))
        assertEquals(HistoryVerb.OTHER, HistoryRules.verbOf("brand_new_kind"))
        assertEquals(HistoryLineKind.PAUSED_AT, HistoryRules.lineOf(event(1, "download_paused", data = """{"progress":0.42}""")).kind)
        assertEquals(42, HistoryRules.lineOf(event(1, "download_paused", data = """{"progress":0.42}""")).number)
        assertEquals(
            HistoryLineKind.REMOVED_DELETED,
            HistoryRules.lineOf(event(1, "title_removed", data = """{"delete_files":true}""")).kind,
        )
        val reason = HistoryRules.lineOf(event(1, "import_failed", data = """{"reason":"nothing fits; match the files by hand."}"""))
        assertEquals(HistoryLineKind.REASON, reason.kind)
        assertEquals("Nothing fits", reason.text)
        val summary = HistoryRules.lineOf(event(1, "settings_changed").copy(summary = "Heat: changed"))
        assertEquals(HistoryLineKind.SUMMARY, summary.kind)
        assertEquals("changed", summary.text)
        assertEquals("4K", HistoryRules.resolutionOf("Movie.UHD.BluRay"))
    }

    @Test
    fun saysWhoDidIt() {
        assertEquals(WhoKind.HOLYARRS, HistoryRules.whoOf(event(1, "x")).kind)
        val person = HistoryRules.whoOf(event(1, "ask_approved", data = """{"approved_by":"Kim"}""", actor = EventActor("user", 3)))
        assertEquals("Kim", person.name)
        val ai =
            HistoryRules.whoOf(
                event(1, "ai_ask_made", data = """{"client":"Claude"}""", actor = EventActor("ai", 3)).copy(actorName = "Sam"),
            )
        assertEquals(WhoKind.AI, ai.kind)
        assertEquals("Sam", ai.forName)
        assertEquals(WhoKind.JOB, HistoryRules.whoOf(event(1, "x", actor = EventActor("job"))).kind)
    }

    @Test
    fun buildsTheKindListAndFilters() {
        val all = HistoryRules.kinds(emptySet())
        assertTrue("search_done:noted" in all && "library_folder_*" in all)
        assertTrue(all.size <= 64)
        assertEquals(
            listOf("download_finished", "download_seeding", "file_replaced"),
            HistoryRules.kinds(setOf(HistoryStep.UPGRADED, HistoryStep.DOWNLOADED)),
        )
        val mine = event(1, "x", actor = EventActor("user", 2))
        assertTrue(HistoryRules.whoMatches(mine, HistoryWhoFilter.ME, 2))
        assertFalse(HistoryRules.whoMatches(mine, HistoryWhoFilter.ME, 3))
        assertTrue(HistoryRules.whoMatches(event(1, "x"), HistoryWhoFilter.HOLYARRS, 2))
        assertTrue(HistoryRules.typeMatches("album", HistoryType.MUSIC))
        assertTrue(HistoryRules.textMatches(event(1, "x", data = """{"source_name":"NZBgeek"}"""), "geek", emptyMap()))
        val dupes =
            listOf(
                event(2, "ask_made", data = """{"ask_id":5}"""),
                event(1, "ai_ask_made", data = """{"ask_id":5}"""),
                event(0, "ask_made", data = """{"ask_id":6}"""),
            )
        assertEquals(listOf(1L, 0L), HistoryRules.dropDuplicateAsks(dupes).map { it.id })
    }

    private val page =
        """{"events":[
            {"ID":3,"At":"2026-11-24T08:00:00Z","Actor":{"kind":"system"},"Kind":"import_done","TitleID":40,"SlotID":null,"DownloadID":9,
             "Summary":"Imported","Data":{"release_id":77},"GroupKey":"","title_name":"Heat"},
            {"ID":2,"At":"2026-11-24T07:00:00Z","Actor":{"kind":"system"},"Kind":"download_sent","TitleID":40,"DownloadID":9,"Summary":"Sent",
             "Data":{"release_id":77},"title_name":"Heat"},
            {"ID":1,"At":"2026-11-22T21:00:00Z","Actor":{"kind":"user","id":1},"Kind":"title_added","TitleID":41,"DownloadID":null,
             "Summary":"Added","Data":null,"title_name":"Lost","actor_name":"sam"}
        ],"total":3,"limit":100,"offset":0}"""

    private fun presenter(
        server: FakeServer,
        titleId: Long = 0,
    ) = HistoryPresenter(
        server.api(),
        { downloadsAccount() },
        titleId,
        0,
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-11-24T12:00:00Z")
        },
        { 3600 },
    )

    @Test
    fun loadsDaysOfHistory() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/events", page)
            server.json("GET", "/api/v1/titles", """{"titles":[{"id":40,"kind":"movie","name":"Heat","artwork":["/a.jpg"]}]}""")
            server.json("GET", "/api/v1/users", """{"users":[{"id":1,"name":"Sam"}]}""")
            server.json("GET", "/api/v1/sources", """{"sources":[{"id":4,"name":"NZBgeek"}]}""")
            val presenter = presenter(server)
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals(listOf(Day(2026, 11, 24), Day(2026, 11, 22)), state.days.map { it.day })
            val heat = state.days[0].items.single()
            assertEquals("d9", heat.key)
            assertEquals(HistoryVerb.IMPORTED, heat.verb)
            assertEquals("/a.jpg", heat.poster)
            assertEquals(1, state.sources.size)
            val query = server.request("GET", "/api/v1/events").query
            assertTrue(query.contains("feed=0") && query.contains("limit=100") && query.contains("kind="))
            assertFalse(state.more)
            assertFalse(state.filtered)
        }

    @Test
    fun sendsFiltersAndScopes() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/events", page)
            val presenter = presenter(server, titleId = 40)
            presenter.load()
            assertTrue(server.request("GET", "/api/v1/events").query.contains("title=40"))
            presenter.setFilter(
                HistoryFilter(steps = setOf(HistoryStep.UPGRADED), period = HistoryWhen.MONTH, who = HistoryWhoFilter.PEOPLE),
            )
            val query = server.request("GET", "/api/v1/events").query
            assertTrue(query.contains("kind=file_replaced"))
            assertTrue(query.contains("from=2026-10-25"))
            assertEquals(
                listOf("e1"),
                presenter.state.value.days
                    .flatMap { d -> d.items.map { it.key } },
            )
            presenter.setFilter(HistoryFilter(period = HistoryWhen.CUSTOM, from = "2026-11-20", to = "2026-11-01"))
            assertTrue(presenter.state.value.filter.badRange)
        }

    @Test
    fun findsAnotherRelease() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/events", page)
            server.json("POST", "/api/v1/releases/77/turn-down", """{"id":77}""")
            server.json("POST", "/api/v1/titles/40/search", """{"job_ids":[3]}""", 202)
            val presenter = presenter(server)
            presenter.load()
            presenter.findAnother("d9")
            assertEquals("{}", server.request("POST", "/api/v1/releases/77/turn-down").body)
            assertEquals(HistoryNotice.REPLACING, presenter.state.value.notice)
            presenter.findAnother("e1")
            assertEquals(HistoryNotice.NO_RELEASE, presenter.state.value.notice)
        }

    @Test
    fun decodesGoCasedEvents() {
        val decoded = ApiJson.strict.decodeFromString(EventsPage.serializer(), page)
        assertEquals(0L, decoded.events[0].slotId)
        assertEquals(1L, decoded.events[2].actor.id)
    }
}
