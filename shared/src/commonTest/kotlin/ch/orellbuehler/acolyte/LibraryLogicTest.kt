package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.FileQuality
import ch.orellbuehler.acolyte.api.model.GridAsk
import ch.orellbuehler.acolyte.api.model.GridSeason
import ch.orellbuehler.acolyte.api.model.GridShow
import ch.orellbuehler.acolyte.api.model.GridSlot
import ch.orellbuehler.acolyte.api.model.GridTitle
import ch.orellbuehler.acolyte.api.model.MyWatched
import ch.orellbuehler.acolyte.api.model.ReleaseRead
import ch.orellbuehler.acolyte.api.model.SlotDownload
import ch.orellbuehler.acolyte.api.model.SlotFile
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.library.FactKind
import ch.orellbuehler.acolyte.feature.library.LibStateKey
import ch.orellbuehler.acolyte.feature.library.LibraryLogic
import ch.orellbuehler.acolyte.feature.library.LibrarySort
import ch.orellbuehler.acolyte.feature.library.ReleaseText
import ch.orellbuehler.acolyte.feature.library.StatusFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryLogicTest {
    private val now = LibraryLogic.millis("2026-11-24T12:00:00Z")!!

    private fun movie(
        vararg slots: GridSlot,
        keep: Boolean = true,
        id: Long = 1,
        name: String = "Heat",
    ) = GridTitle(id = id, kind = "movie", name = name, keepUpToDate = keep, slots = slots.toList())

    private fun show(
        show: GridShow,
        keep: Boolean = true,
    ) = GridTitle(id = 2, kind = "show", name = "Dark", keepUpToDate = keep, show = show)

    @Test
    fun movieFailureBeatsDownload() {
        val state =
            LibraryLogic.state(
                movie(
                    GridSlot(state = "failed", reason = "no release passed"),
                    GridSlot(state = "downloading", download = SlotDownload(active = true, progress = 0.4)),
                ),
                now,
            )
        assertEquals(LibStateKey.FAILED, state?.key)
        assertEquals(listOf("no release passed"), state?.reasons)
        assertEquals(Tone.FAIL, state?.tone)
    }

    @Test
    fun missingReasonAndFileNotFoundAreFlagged() {
        val state =
            LibraryLogic.state(
                movie(
                    GridSlot(state = "needs_a_decision"),
                    GridSlot(
                        state = "needs_a_decision",
                        reason = "HolyArrs can't find the downloaded file where the Downloader says it is; x",
                    ),
                ),
                now,
            )
        assertEquals(LibStateKey.NEEDS_DECISION, state?.key)
        assertTrue(state!!.noReason)
        assertTrue(state.fileNotFound)
        assertTrue(state.reasons.isEmpty())
    }

    @Test
    fun downloadStates() {
        val p = LibraryLogic.state(movie(GridSlot(state = "downloading", download = SlotDownload(active = true, progress = 0.456))), now)
        assertEquals(LibStateKey.DOWNLOADING, p?.key)
        assertEquals(46, p?.percent)
        assertEquals(LibStateKey.QUEUED, LibraryLogic.state(movie(GridSlot(state = "chosen")), now)?.key)
        val importing = LibraryLogic.state(movie(GridSlot(state = "importing")), now)
        assertTrue(importing!!.importing)
        assertEquals(100, importing.percent)
        assertEquals(LibStateKey.STALLED, LibraryLogic.state(movie(GridSlot(state = "stalled")), now)?.key)
        val paused = LibraryLogic.state(movie(GridSlot(state = "downloading", download = SlotDownload(state = "paused"))), now)
        assertEquals(LibStateKey.PAUSED, paused?.key)
    }

    @Test
    fun ownedMovieShowsQuality() {
        val file = SlotFile(id = 3, state = "present", quality = FileQuality(resolution = 2160))
        val state = LibraryLogic.state(movie(GridSlot(state = "in_library", file = file)), now)
        assertEquals(LibStateKey.IN_LIBRARY, state?.key)
        assertEquals("4K", state?.quality)
        val notKept = LibraryLogic.state(movie(GridSlot(state = "in_library", file = file), keep = false), now)
        assertEquals(LibStateKey.NOT_KEPT, notKept?.key)
        assertEquals("4K", notKept?.quality)
        val gone = LibraryLogic.state(movie(GridSlot(state = "in_library", file = SlotFile(state = "missing"))), now)
        assertEquals(LibStateKey.MISSING_FILE, gone?.key)
    }

    @Test
    fun askAndUpcoming() {
        val asked = movie(GridSlot(state = "wanted")).copy(asks = listOf(GridAsk(titleId = 1, state = "asked")))
        assertEquals(LibStateKey.WAITING_APPROVAL, LibraryLogic.state(asked, now)?.key)
        val upcoming = movie(GridSlot(state = "not_out_yet")).copy(digitalDate = "2027-01-01T00:00:00Z")
        val state = LibraryLogic.state(upcoming, now)
        assertEquals(LibStateKey.NOT_OUT_YET, state?.key)
        assertEquals("2027-01-01T00:00:00Z", state?.date)
        assertEquals(LibStateKey.MISSING, LibraryLogic.state(movie(GridSlot(state = "wanted")), now)?.key)
    }

    @Test
    fun showStates() {
        val seasons =
            listOf(GridSeason(number = 1, keepUpToDate = true, aired = 10), GridSeason(number = 2, keepUpToDate = true, aired = 6))
        assertEquals(LibStateKey.MISSING, LibraryLogic.state(show(GridShow(seasons = seasons, inLibrary = 12, missing = 4)), now)?.key)
        assertEquals(4, LibraryLogic.state(show(GridShow(seasons = seasons, inLibrary = 12, missing = 4)), now)?.count)
        assertEquals(LibStateKey.ENDED, LibraryLogic.state(show(GridShow(seasons = seasons, inLibrary = 16, status = "ended")), now)?.key)
        assertEquals(LibStateKey.UP_TO_DATE, LibraryLogic.state(show(GridShow(seasons = seasons, inLibrary = 16)), now)?.key)
        assertEquals(LibStateKey.FAILED, LibraryLogic.state(show(GridShow(inLibrary = 3, failed = 2)), now)?.key)
        assertNull(LibraryLogic.state(show(GridShow()), now))
        assertEquals(LibStateKey.NOT_KEPT, LibraryLogic.state(show(GridShow(), keep = false), now)?.key)
        val specials = GridShow(seasons = listOf(GridSeason(number = 0, keepUpToDate = true, aired = 3)), inLibrary = 3)
        assertEquals(LibStateKey.SPECIALS_ONLY, LibraryLogic.state(show(specials), now)?.key)
        val upcoming = GridShow(notOutYet = 8, nextAirDate = "2027-02-01T00:00:00Z")
        assertEquals(LibStateKey.NOT_OUT_YET, LibraryLogic.state(show(upcoming), now)?.key)
    }

    @Test
    fun progressCountsAiredEpisodes() {
        val seasons =
            listOf(
                GridSeason(number = 0, keepUpToDate = false, aired = 4),
                GridSeason(number = 1, keepUpToDate = true, aired = 10),
                GridSeason(number = 2, keepUpToDate = false, aired = 6),
            )
        val p = LibraryLogic.progress(show(GridShow(seasons = seasons, inLibrary = 12)))
        assertEquals(12, p.have)
        assertEquals(16, p.total)
        assertEquals(8, LibraryLogic.progress(show(GridShow(episodes = 10, notOutYet = 2))).total)
    }

    @Test
    fun filtersMatchSlotStates() {
        val searching = movie(GridSlot(state = "searching"))
        assertTrue(LibraryLogic.matches(searching, StatusFilter.MISSING))
        assertTrue(LibraryLogic.matches(movie(GridSlot(state = "better_wanted")), StatusFilter.HAVE))
        assertTrue(LibraryLogic.matches(movie(GridSlot(state = "better_wanted")), StatusFilter.BETTER))
        assertTrue(!LibraryLogic.matches(show(GridShow(missing = 0)), StatusFilter.MISSING))
        assertTrue(!LibraryLogic.matches(GridTitle(kind = "show"), StatusFilter.HAVE))
    }

    @Test
    fun sortSinksMissingValuesAndSkipsArticles() {
        val a = movie(id = 1, name = "The Zoo").copy(year = 2001)
        val b = movie(id = 2, name = "Alien").copy(year = 0)
        val c = movie(id = 3, name = "Brazil").copy(year = 1985)
        val byYear = LibraryLogic.sort(listOf(a, b, c), LibrarySort.YEAR, descending = true, states = emptyMap())
        assertEquals(listOf(1L, 3L, 2L), byYear.map { it.id })
        val ascending = LibraryLogic.sort(listOf(a, b, c), LibrarySort.YEAR, descending = false, states = emptyMap())
        assertEquals(listOf(3L, 1L, 2L), ascending.map { it.id })
        val byName = LibraryLogic.sort(listOf(a, b, c), LibrarySort.NAME, descending = false, states = emptyMap())
        assertEquals(listOf(2L, 3L, 1L), byName.map { it.id })
        assertEquals("Z", LibraryLogic.letter("The Zoo"))
        assertEquals("E", LibraryLogic.letter("Élan"))
        assertEquals("#", LibraryLogic.letter("12 Monkeys"))
    }

    @Test
    fun scoreNeedsVotes() {
        assertNull(LibraryLogic.score(movie().copy(score = 8.0, votes = 10)))
        assertEquals(8.0, LibraryLogic.score(movie().copy(score = 8.0, votes = 30)))
    }

    @Test
    fun releaseFacts() {
        val past = movie().copy(digitalDate = "2020-01-01T00:00:00Z")
        assertEquals(FactKind.RELEASED, LibraryLogic.fact(past, LibrarySort.NAME, now)?.kind)
        val home = movie().copy(digitalDate = "2027-01-01T00:00:00Z")
        assertEquals(FactKind.HOME_RELEASE, LibraryLogic.fact(home, LibrarySort.NAME, now)?.kind)
        val cinema = movie().copy(cinemaDate = "2027-01-01T00:00:00Z")
        assertEquals(FactKind.CINEMAS, LibraryLogic.fact(cinema, LibrarySort.RELEASE, now)?.kind)
        assertEquals(FactKind.NOT_KNOWN, LibraryLogic.fact(movie(), LibrarySort.NAME, now)?.kind)
        val sized = movie().copy(sizeBytes = 1024)
        assertEquals(1024L, LibraryLogic.fact(sized, LibrarySort.SIZE, now)?.number)
        val ended = show(GridShow(status = "ended"))
        assertEquals(FactKind.ENDED, LibraryLogic.fact(ended, LibrarySort.NEXT, now)?.kind)
    }

    @Test
    fun searchMatchesWordsOrYear() {
        val t = movie(name = "The Dark Knight").copy(year = 2008)
        assertTrue(LibraryLogic.byName(t, "dark knight"))
        assertTrue(LibraryLogic.byName(t, "dark 2008"))
        assertTrue(!LibraryLogic.byName(t, "dark 2009"))
        assertEquals("a b", LibraryLogic.query("  a   b "))
    }

    @Test
    fun tmdbIdMustBeUsable() {
        assertEquals("949", LibraryLogic.tmdbOf(movie().copy(ids = mapOf("tmdb" to "949"))))
        assertEquals("", LibraryLogic.tmdbOf(movie().copy(ids = mapOf("tmdb" to "81189", "tvdb" to "81189"))))
        assertEquals("", LibraryLogic.tmdbOf(movie().copy(ids = mapOf("tmdb" to "abc"))))
        assertEquals("", LibraryLogic.tmdbOf(GridTitle(kind = "artist", ids = mapOf("tmdb" to "1"))))
    }

    @Test
    fun watchedCapsAtAiredSeasons() {
        val t = show(GridShow(seasons = listOf(GridSeason(number = 1, aired = 5), GridSeason(number = 0, aired = 2))))
        val w = MyWatched(enabled = true, linked = true, shows = mapOf("2" to mapOf("1" to 9, "0" to 2)))
        assertEquals(5 to 5, LibraryLogic.watched(t, w))
        assertEquals(0 to 0, LibraryLogic.watched(t, w.copy(linked = false)))
        assertEquals(1 to 1, LibraryLogic.watched(movie(), MyWatched(enabled = true, linked = true, movies = listOf(1))))
    }

    @Test
    fun textHelpers() {
        assertEquals("Sam" to true, LibraryLogic.personName("Sam\u206312"))
        assertEquals(listOf("it", "en"), LibraryLogic.audioCodes(" IT, en;"))
        assertEquals(listOf("anime", "kids-only"), LibraryLogic.cleanTags(listOf(" Anime ", "kids only", "anime", "")))
        assertEquals("Movie.mkv", LibraryLogic.fileName("/a/b/Movie.mkv"))
        assertEquals("…/b/c", LibraryLogic.shortPath("/a/b/c"))
    }

    @Test
    fun releaseTags() {
        val read =
            ReleaseRead(
                resolution = 2160,
                source = "web_dl",
                hdr = listOf("dv"),
                codec = "HEVC",
                channels = "5.1",
                languages = listOf("de"),
            )
        assertEquals(listOf("4K", "WEB-DL", "Dolby Vision", "x265", "5.1", "DE"), ReleaseText.tags(read))
    }
}
