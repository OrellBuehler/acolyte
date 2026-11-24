package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.CalendarFollower
import ch.orellbuehler.acolyte.api.model.CalendarRow
import ch.orellbuehler.acolyte.api.model.CalendarVersion
import ch.orellbuehler.acolyte.api.model.GridSlot
import ch.orellbuehler.acolyte.api.model.GridTitle
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.downloads.CalendarFollows
import ch.orellbuehler.acolyte.feature.downloads.CalendarNoticeKind
import ch.orellbuehler.acolyte.feature.downloads.CalendarPresenter
import ch.orellbuehler.acolyte.feature.downloads.CalendarRules
import ch.orellbuehler.acolyte.feature.downloads.CalendarType
import ch.orellbuehler.acolyte.feature.downloads.Day
import ch.orellbuehler.acolyte.feature.downloads.EntryDot
import ch.orellbuehler.acolyte.feature.downloads.EntryTag
import ch.orellbuehler.acolyte.feature.downloads.EntryWhen
import ch.orellbuehler.acolyte.feature.downloads.EpisodeSpan
import ch.orellbuehler.acolyte.feature.downloads.Month
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class CalendarTest {
    private val today = Day(2026, 11, 24)
    private val zurich = { _: Long -> 3600 }

    private fun episode(
        season: Int,
        number: Int,
        date: String = "2026-11-25T00:00:00Z",
        timed: Boolean = false,
        airDay: String = "",
        state: String = "",
    ) = CalendarRow(
        titleId = 5,
        name = "Lost",
        kind = "episode",
        date = date,
        itemId = (season * 100 + number).toLong(),
        season = season,
        episode = number,
        timed = timed,
        airDay = airDay,
        state = state,
        tmdb = "4607",
    )

    @Test
    fun placesEntriesOnTheirDay() {
        assertEquals(Day(2026, 11, 26), CalendarRules.entryDay(episode(1, 1, "2026-11-25T23:30:00Z", timed = true), zurich))
        assertEquals(Day(2026, 11, 25), CalendarRules.entryDay(episode(1, 1, "2026-11-25T22:30:00Z", timed = true), zurich))
        assertEquals(Day(2026, 11, 24), CalendarRules.entryDay(episode(1, 1, "2026-11-25T00:00:00Z", airDay = "2026-11-24"), zurich))
        assertEquals(Day(2026, 11, 25), CalendarRules.entryDay(episode(1, 1, "2026-11-25T00:00:00Z"), zurich))
        assertEquals(Day(2026, 12, 1), CalendarRules.entryDay(CalendarRow(kind = "digital", date = "2026-12-01T23:00:00Z"), zurich))
    }

    @Test
    fun dotsFollowTheStates() {
        assertEquals(EntryDot.HAVE, CalendarRules.dot(episode(1, 1, state = "in_library"), today, zurich))
        assertEquals(
            EntryDot.HAVE,
            CalendarRules.dot(episode(1, 1).copy(versions = listOf(CalendarVersion("4K", "better_wanted"))), today, zurich),
        )
        assertEquals(EntryDot.GETTING, CalendarRules.dot(episode(1, 1, state = "downloading"), today, zurich))
        assertEquals(EntryDot.PROBLEM, CalendarRules.dot(episode(1, 1, state = "stalled"), today, zurich))
        assertEquals(EntryDot.MISSING, CalendarRules.dot(episode(1, 1, date = "2026-11-20T00:00:00Z", state = "wanted"), today, zurich))
        assertEquals(EntryDot.UPCOMING, CalendarRules.dot(episode(1, 1, state = "not_out_yet"), today, zurich))
        val album = CalendarRow(kind = "album", date = "2026-11-01", tracks = 10, have = 10)
        assertEquals(EntryDot.HAVE, CalendarRules.dot(album, today, zurich))
    }

    @Test
    fun keepsOneDatePerMovie() {
        val cinema = CalendarRow(titleId = 9, name = "Dune", kind = "cinema", date = "2026-10-01T00:00:00Z")
        val digital = CalendarRow(titleId = 9, name = "Dune", kind = "digital", date = "2026-11-28T00:00:00Z")
        val disc = CalendarRow(titleId = 9, name = "Dune", kind = "physical", date = "2026-12-10T00:00:00Z")
        assertEquals(listOf("digital"), CalendarRules.oneMovieDate(listOf(cinema, digital, disc), emptyMap()).map { it.kind })
        assertEquals(listOf("cinema"), CalendarRules.oneMovieDate(listOf(cinema), emptyMap()).map { it.kind })
        val brief = GridTitle(id = 9, kind = "movie", physicalDate = "2026-12-10T00:00:00Z", slots = listOf(GridSlot(state = "in_library")))
        val kept = CalendarRules.oneMovieDate(listOf(cinema, digital, disc), mapOf(9L to brief))
        assertEquals(listOf("physical"), kept.map { it.kind })
        assertEquals("in_library", kept[0].state)
    }

    @Test
    fun namesTagsAndEpisodes() {
        assertEquals(EntryTag.SHOW_PREMIERE, CalendarRules.tag(episode(1, 1)))
        assertEquals(EntryTag.SEASON_PREMIERE, CalendarRules.tag(episode(3, 1)))
        assertEquals(EntryTag.NEW_EPISODE, CalendarRules.tag(episode(3, 4)))
        assertEquals(EntryTag.NEW_SINGLE, CalendarRules.tag(CalendarRow(kind = "album", albumType = "Single")))
        assertEquals(EntryTag.IN_CINEMAS, CalendarRules.tag(CalendarRow(kind = "cinema")))
        assertTrue(CalendarRules.fakeEpisodeName("Episode 8827"))
        assertTrue(CalendarRules.fakeEpisodeName("TBA"))
        assertTrue(CalendarRules.fakeEpisodeName("November 24, 2026"))
        assertFalse(CalendarRules.fakeEpisodeName("The Constant"))
        assertEquals("The Constant", CalendarRules.episodeName("Episode 5 - The Constant"))
        assertEquals("", CalendarRules.episodeName("Untitled"))
        assertTrue(CalendarRules.channelShown("Netflix"))
        assertFalse(CalendarRules.channelShown("ABC"))
    }

    @Test
    fun groupsADaysEntries() {
        val entries =
            listOf(
                episode(1, 3, "2026-11-25T20:00:00Z", timed = true),
                episode(1, 2, "2026-11-25T19:00:00Z", timed = true),
                CalendarRow(titleId = 9, name = "Dune", kind = "digital", date = "2026-11-25T00:00:00Z"),
            )
        val groups = CalendarRules.groups(entries, today, manager = true, offset = zurich)
        assertEquals(2, groups.size)
        assertEquals("digital", groups[0].kind)
        assertEquals(EntryWhen.STREAMING, groups[0].whenKind)
        val lost = groups[1]
        assertEquals(EntryTag.EPISODES, lost.tag)
        assertEquals(EpisodeSpan.SAME_SEASON, lost.span)
        assertEquals(2, lost.episode)
        assertEquals(3, lost.episode2)
        assertEquals(EntryWhen.TIME, lost.whenKind)
        assertEquals(20 * 60, lost.minutes)
        assertTrue(lost.canSearch)
    }

    @Test
    fun buildsTheMonthGrid() {
        val month = Month(2026, 11)
        val (from, to) = CalendarRules.range(month)
        assertEquals(Day(2026, 10, 25), from)
        assertEquals(Day(2026, 12, 8), to)
        val groups = CalendarRules.groups(listOf(episode(1, 2, state = "stalled")), today, manager = false, offset = zurich)
        val cells = CalendarRules.monthCells(month, groups, today)
        assertEquals(42, cells.size)
        assertEquals(Day(2026, 10, 26), cells[0].day)
        assertFalse(cells[0].inMonth)
        val marked = cells.first { it.day == Day(2026, 11, 25) }
        assertEquals(1, marked.count)
        assertEquals(Tone.FAIL, marked.tone)
        assertTrue(cells.first { it.day == today }.today)
        assertNull(cells.first { it.day == Day(2026, 11, 1) }.tone)
    }

    @Test
    fun filtersByTypeAndFollows() {
        val followed = episode(1, 1).copy(following = true)
        val others = episode(1, 2).copy(followers = listOf(CalendarFollower(id = 2, name = "Kim")))
        assertTrue(CalendarRules.followsOk(followed, CalendarFollows.MINE))
        assertFalse(CalendarRules.followsOk(others, CalendarFollows.MINE))
        assertTrue(CalendarRules.followsOk(others, CalendarFollows.EVERYONE))
        assertTrue(CalendarRules.shows(CalendarRow(kind = "physical"), CalendarType.MOVIES))
        assertFalse(CalendarRules.shows(CalendarRow(kind = "album"), CalendarType.MOVIES))
        assertEquals(CalendarFollows.MINE, CalendarRules.startFollows(2, 1, null))
        assertEquals(CalendarFollows.LIBRARY, CalendarRules.startFollows(0, 4, null))
        assertEquals(CalendarFollows.MINE, CalendarRules.startFollows(1, 1, CalendarFollows.EVERYONE))
    }

    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-11-24T12:00:00Z")
        }

    private val answer =
        """{"from":"2026-10-25","to":"2026-12-07","titles":"all","my_follows":1,"people":2,"dates":[
            {"title_id":5,"name":"Lost","kind":"episode","date":"2026-11-20T00:00:00Z","item_id":101,"season":1,"episode":1,"tmdb":"4607",
             "state":"wanted","timed":false,"following":true},
            {"title_id":5,"name":"Lost","kind":"episode","date":"2026-11-20T00:00:00Z","item_id":101,"season":1,"episode":1,"tmdb":"4607",
             "state":"wanted","timed":false,"following":true},
            {"title_id":9,"name":"Dune","kind":"digital","date":"2026-11-28T00:00:00Z","tmdb":"438631","timed":false}
        ]}"""

    @Test
    fun loadsAMonth() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/calendar", answer)
            server.json("GET", "/api/v1/titles", """{"titles":[]}""")
            val presenter = CalendarPresenter(server.api(), { testAccount() }, clock, zurich)
            presenter.load()
            val state = presenter.state.value
            assertEquals(Phase.READY, state.status.phase)
            assertEquals("from=2026-10-25&to=2026-12-08", server.request("GET", "/api/v1/calendar").query)
            assertEquals(Month(2026, 11), state.month)
            assertEquals(CalendarFollows.MINE, state.follows)
            assertEquals(1, state.days.size)
            assertEquals(1, state.hidden)
            assertEquals(1, state.missing)
            presenter.setFollows(CalendarFollows.LIBRARY)
            assertEquals(2, presenter.state.value.days.size)
            presenter.setType(CalendarType.MOVIES)
            assertEquals(
                listOf("Dune"),
                presenter.state.value.days
                    .flatMap { d -> d.groups.map { it.title } },
            )
            assertEquals(
                "movie",
                presenter
                    .link(
                        presenter.state.value.days[0]
                            .groups[0],
                    )?.kind,
            )
        }

    @Test
    fun followsAndSearches() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/calendar", answer)
            server.json("PUT", "/api/v1/titles/5/follow", """{"title_id":5,"following":false}""")
            server.json("DELETE", "/api/v1/titles/5/follow", """{"title_id":5,"following":false}""")
            server.json("POST", "/api/v1/titles/5/search", """{"job_ids":[8]}""", 202)
            val presenter = CalendarPresenter(server.api(), { testAccount() }, clock, zurich)
            presenter.load()
            val lost =
                presenter.state.value.days[0]
                    .groups[0]
            presenter.search(lost)
            assertEquals("""{"item_id":101}""", server.request("POST", "/api/v1/titles/5/search").body)
            assertEquals(
                CalendarNoticeKind.SEARCHING,
                presenter.state.value.notice
                    ?.kind,
            )
            presenter.follow(lost, on = false)
            assertEquals(
                CalendarNoticeKind.UNFOLLOWED,
                presenter.state.value.notice
                    ?.kind,
            )
            assertEquals(0, presenter.state.value.myFollows)
            assertTrue(
                presenter.state.value.days
                    .isEmpty(),
            )
            presenter.next()
            assertEquals(Month(2026, 12), presenter.state.value.month)
        }
}
