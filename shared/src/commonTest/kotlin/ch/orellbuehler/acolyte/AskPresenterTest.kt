package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.requests.AskFailure
import ch.orellbuehler.acolyte.feature.requests.AskMusicPresenter
import ch.orellbuehler.acolyte.feature.requests.AskOutcome
import ch.orellbuehler.acolyte.feature.requests.AskPresenter
import ch.orellbuehler.acolyte.feature.requests.AskReviewPresenter
import ch.orellbuehler.acolyte.feature.requests.EpisodesLoad
import ch.orellbuehler.acolyte.feature.requests.MusicMode
import ch.orellbuehler.acolyte.feature.requests.RequestsInbox
import ch.orellbuehler.acolyte.feature.requests.ShowMode
import ch.orellbuehler.acolyte.feature.requests.Tick
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AskPresenterTest {
    private val limit =
        """{"no_limit":false,"pick_many":true,"four_k":{"movie":true,"show":true},"qualities":{
            "movie":[{"id":"standard","name":"","extra":false,"pick_rules_id":0},{"id":"4k","name":"","extra":true,"pick_rules_id":2}],
            "show":[{"id":"standard","name":"","extra":false,"pick_rules_id":0}]}}"""

    private fun FakeServer.member() {
        json("GET", "/api/v1/asks/limit", limit)
        json("GET", "/api/v1/messages", """{"messages":[],"unread":0}""")
    }

    private fun inbox(server: FakeServer) = RequestsInbox(server.api()) { testAccount(Role.MEMBER, listOf("ask")) }

    @Test
    fun movieAsksEachPickedLine() =
        runTest {
            val server = FakeServer()
            server.member()
            server.json(
                "GET",
                "/api/v1/titles/lookup",
                """{"movies":[{"ids":{"tmdb":"603"},"name":"The Matrix","year":1999,"ask":{"id":4,"state":"asked","state_label":"Waiting for approval","mine":false,"people":1}}]}""",
            )
            server.json("POST", "/api/v1/asks", """{"ask":${askJson(4)},"joined":true,"already":false}""")
            val presenter = AskPresenter(server.api(), inbox(server), "movie", "603")
            presenter.load()
            var s = presenter.state.value
            assertEquals(Phase.READY, s.status.phase)
            assertEquals("The Matrix", s.name)
            assertTrue(s.showsQuality)
            assertEquals("standard", s.selected)
            assertTrue(s.joining)
            assertEquals(listOf("4k"), s.alsoPills.map { it.key })
            assertEquals("term=tmdb%3A603", server.request("GET", "/api/v1/titles/lookup").query)
            presenter.toggleAlso("4k")
            presenter.setFollow(false)
            presenter.submit()
            s = presenter.state.value
            assertEquals(AskOutcome.JOINED, s.outcome)
            val bodies = server.requests.filter { it.method == "POST" && it.path == "/api/v1/asks" }.map { it.body }
            assertEquals(
                listOf(
                    """{"id":"tmdb:603","kind":"movie","follow":false,"quality":"standard"}""",
                    """{"id":"tmdb:603","kind":"movie","follow":false,"quality":"4k"}""",
                ),
                bodies,
            )
        }

    @Test
    fun weeklyLimitIsSaid() =
        runTest {
            val server = FakeServer()
            server.member()
            server.json("GET", "/api/v1/titles/lookup", """{"movies":[{"ids":{"tmdb":"1"},"name":"A"}]}""")
            server.json(
                "POST",
                "/api/v1/asks",
                """{"error":"you have asked for 10 movies","kind":"movies","per_week":10}""",
                429,
                "Retry-After" to "7200",
            )
            val presenter = AskPresenter(server.api(), inbox(server), "movie", "1")
            presenter.load()
            presenter.submit()
            val s = presenter.state.value
            assertEquals(AskFailure.WEEKLY_LIMIT, s.failure)
            assertEquals(7200, s.error?.retryAfterSeconds)
            assertNull(s.outcome)
            assertFalse(s.busy)
        }

    @Test
    fun showAsksChosenSeasonsAndEpisodes() =
        runTest {
            val server = FakeServer()
            server.member()
            server.json(
                "GET",
                "/api/v1/shows/lookup",
                """{"shows":[{"ids":{"tmdb":"1399","tvdb":"121361"},"source":"tvdb","name":"GoT","title_id":8,
                    "seasons":[{"number":2},{"number":1},{"number":0}]}]}""",
            )
            server.json(
                "GET",
                "/api/v1/titles/8/seasons",
                """{"seasons":[{"number":1,"aired":10,"in_library":10,"keep_up_to_date":false},{"number":2,"aired":10,"in_library":2}]}""",
            )
            server.json("GET", "/api/v1/metadata/show/1399/seasons/0", """{"episodes":[{"number":1,"name":"Pilot"},{"number":2}]}""")
            server.json("POST", "/api/v1/asks", """{"ask":${askJson(3, kind = "show")},"joined":false,"already":false}""")
            val presenter = AskPresenter(server.api(), inbox(server), "show", "1399")
            presenter.load()
            var s = presenter.state.value
            assertEquals(listOf(0, 1, 2), s.seasons.map { it.number })
            assertTrue(s.seasons[1].complete)
            assertFalse(s.showsQuality)
            presenter.setMode(ShowMode.CHOOSE)
            assertFalse(presenter.state.value.canSubmit)
            presenter.toggleSeason(2)
            presenter.toggleSeason(1)
            presenter.expandSeason(0)
            assertEquals(
                EpisodesLoad.READY,
                presenter.state.value.seasons[0]
                    .load,
            )
            presenter.toggleEpisode(0, 2)
            s = presenter.state.value
            assertEquals(listOf(2), s.pickedSeasons)
            assertEquals(1, s.pickedEpisodes)
            presenter.submit()
            assertEquals(AskOutcome.REQUESTED, presenter.state.value.outcome)
            assertEquals(
                """{"id":"tvdb:121361","kind":"show","seasons":[2],"episodes":[{"season":0,"episodes":[2]}],"follow":true,"quality":"standard"}""",
                server.request("POST", "/api/v1/asks").body,
            )
        }

    @Test
    fun futureSeasonsOnly() =
        runTest {
            val server = FakeServer()
            server.member()
            server.json("GET", "/api/v1/shows/lookup", """{"shows":[{"ids":{"tmdb":"5"},"name":"S","seasons":[{"number":1}]}]}""")
            server.json(
                "POST",
                "/api/v1/asks",
                """{"ask":${askJson(3, "approved", kind = "show", titleId = 2)},"joined":false,"already":false}""",
                201,
            )
            val presenter = AskPresenter(server.api(), inbox(server), "show", "5")
            presenter.load()
            presenter.setMode(ShowMode.FUTURE)
            presenter.submit()
            assertEquals(AskOutcome.APPROVED, presenter.state.value.outcome)
            assertEquals(
                """{"id":"tmdb:5","kind":"show","future_only":true,"follow":true,"quality":"standard"}""",
                server.request("POST", "/api/v1/asks").body,
            )
        }

    @Test
    fun musicAsksSomeAlbums() =
        runTest {
            val server = FakeServer()
            server.member()
            server.json(
                "GET",
                "/api/v1/artists/lookup",
                """{"artists":[{"mbid":"m1","name":"Band","ask_albums":[
                    {"mbid":"a1","name":"One","type":"album","release_date":"2001-01-01","kept":true},
                    {"mbid":"a2","name":"Two","type":"album","release_date":"2003-01-01"},
                    {"mbid":"a3","name":"Live","type":"album","secondary_types":["live"],"left_out":true}]}]}""",
            )
            server.json("POST", "/api/v1/asks", """{"ask":${askJson(3, kind = "artist")},"joined":false,"already":false}""")
            val presenter = AskMusicPresenter(server.api(), inbox(server), "m1", "", "a2", "r2")
            presenter.load()
            val s = presenter.state.value
            assertEquals("Band", s.name)
            assertEquals(MusicMode.SOME, s.mode)
            assertEquals(listOf("a2"), s.picked.map { it.mbid })
            presenter.toggle("a1")
            presenter.submit()
            assertEquals(
                """{"id":"mbid:m1","kind":"artist","albums":["a2"],"editions":{"a2":"r2"},"follow":true}""",
                server.request("POST", "/api/v1/asks").body,
            )
            assertEquals("term=mbid%3Am1&albums=true", server.request("GET", "/api/v1/artists/lookup").query)
        }

    @Test
    fun reviewNarrowsAShowAsk() =
        runTest {
            val server = FakeServer()
            server.json("GET", "/api/v1/asks/3", askJson(3, kind = "show", seasons = "[1,2]"))
            server.json(
                "GET",
                "/api/v1/asks/3/parts",
                """{"seasons":{"1":[{"number":1},{"number":2}],"2":[{"number":1}],"3":[{"number":1}]},"albums":{}}""",
            )
            server.json("GET", "/api/v1/asks/limit", limit)
            server.json("GET", "/api/v1/pick-rules", """{"pick_rules":[{"id":6,"name":"Lite","kinds":["show"]}]}""")
            val review = AskReviewPresenter(server.api(), { testAccount() }, 3)
            review.load()
            var s = review.state.value
            assertEquals(listOf(1, 2), s.seasons.map { it.number })
            assertEquals(listOf(Tick.ALL, Tick.ALL), s.seasons.map { it.tick })
            assertFalse(s.changed)
            assertEquals(listOf("standard", "rules-6"), s.pills.map { it.key })
            review.toggleEpisode(1, 2)
            review.toggleSeason(2)
            review.select("rules-6")
            s = review.state.value
            assertTrue(s.changed)
            assertEquals(Tick.SOME, s.seasons[0].tick)
            assertEquals(Tick.NONE, s.seasons[1].tick)
            val body = review.approveBody
            assertEquals(emptyList(), body.seasons)
            assertEquals(listOf(1), body.episodes?.single()?.episodes)
            assertEquals(6L, body.pickRulesId)
            assertNull(body.quality)
            review.toggleEpisode(1, 1)
            assertTrue(review.state.value.empty)
            assertFalse(review.state.value.canApprove)
        }
}
