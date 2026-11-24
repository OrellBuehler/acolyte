package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.MusicBrowseTile
import ch.orellbuehler.acolyte.api.model.MusicCard
import ch.orellbuehler.acolyte.feature.music.MusicAlbumKind
import ch.orellbuehler.acolyte.feature.music.MusicKindsPreset
import ch.orellbuehler.acolyte.feature.music.MusicLineKind
import ch.orellbuehler.acolyte.feature.music.MusicMark
import ch.orellbuehler.acolyte.feature.music.MusicOpenKind
import ch.orellbuehler.acolyte.feature.music.MusicOptions
import ch.orellbuehler.acolyte.feature.music.MusicProblem
import ch.orellbuehler.acolyte.feature.music.MusicRules
import ch.orellbuehler.acolyte.feature.music.MusicShape
import ch.orellbuehler.acolyte.feature.music.kindsPreset
import ch.orellbuehler.acolyte.feature.music.musicProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MusicRulesTest {
    @Test
    fun countsAndLengthsAreShort() {
        assertEquals("999", MusicRules.shortCount(999))
        assertEquals("12K", MusicRules.shortCount(12_000))
        assertEquals("1.2M", MusicRules.shortCount(1_234_567))
        assertEquals("387K", MusicRules.shortCount(387_000))
        assertEquals("3:05", MusicRules.lengthText(185_000))
        assertEquals("", MusicRules.lengthText(0))
        assertEquals(4, MusicRules.minutes(230_000))
    }

    @Test
    fun releaseWordsFollowTheDate() {
        assertEquals(MusicLineKind.OUT_ON, MusicRules.releaseLine("2026-12-05", "2026-11-24").kind)
        assertEquals(MusicLineKind.OUT_NOW, MusicRules.releaseLine("2026-11-20", "2026-11-24").kind)
        assertEquals(MusicLineKind.RELEASED_ON, MusicRules.releaseLine("2026-01-20", "2026-11-24").kind)
        assertEquals(MusicLineKind.NONE, MusicRules.releaseLine("2026", "2026-11-24").kind)
        assertTrue(MusicRules.isFuture("2027-01-01", "2026-11-24"))
    }

    @Test
    fun albumKindsReadSecondaryTypes() {
        assertEquals(MusicAlbumKind.ALBUM, MusicRules.albumKind("album", listOf("studio")))
        assertEquals(MusicAlbumKind.LIVE, MusicRules.albumKind("album", listOf("live")))
        assertEquals(MusicAlbumKind.COMPILATION, MusicRules.albumKind("album", listOf("compilation")))
        assertEquals(MusicAlbumKind.SINGLE, MusicRules.albumKind("single", emptyList()))
        assertEquals(MusicAlbumKind.REMIX, MusicRules.albumKind("album", listOf("remix")))
    }

    @Test
    fun pillsMapAlbumStates() {
        assertEquals(MusicMark.COMPLETE, MusicRules.pill("complete", false))
        assertEquals(MusicMark.PARTIAL, MusicRules.pill("partial", true))
        assertEquals(MusicMark.REQUESTED, MusicRules.pill("not_included", true))
        assertEquals(MusicMark.NONE, MusicRules.pill("", false))
        assertEquals(MusicMark.COMPLETE, MusicRules.cardMark("have", "fresh"))
        assertEquals(MusicMark.NONE, MusicRules.cardMark("asked", "your_asks"))
    }

    @Test
    fun cardLinesFollowTheRowType() {
        val card = MusicCard(name = "A", artist = "Ann", date = "2026-11-20", listenCount = 12_000)
        assertEquals(MusicLineKind.OUT_NOW, MusicRules.cardLine("fresh", card, "2026-11-24").kind)
        assertEquals(MusicLineKind.LISTENS, MusicRules.cardLine("top_albums", card, "2026-11-24").kind)
        assertEquals(MusicLineKind.ARTIST_YEAR, MusicRules.cardLine("classic", card, "2026-11-24").kind)
        assertEquals(MusicLineKind.ARTIST_TEXT, MusicRules.cardLine("mix", card, "2026-11-24").kind)
        assertEquals(MusicLineKind.TEXT, MusicRules.cardLine("mix", card.copy(note = "+1.2K"), "2026-11-24").kind)
        assertEquals(MusicLineKind.ASKED, MusicRules.cardLine("your_asks", card.copy(state = "asked"), "2026-11-24").kind)
    }

    @Test
    fun cardTargetsFollowTheWebRules() {
        val album = MusicCard(kind = "album", mbid = "a1", artistMbid = "r1")
        assertEquals(MusicTargetCheck(MusicOpenKind.ALBUM, "a1", "r1"), MusicTargetCheck.of(album))
        assertEquals(MusicTargetCheck(MusicOpenKind.ARTIST, "r1", ""), MusicTargetCheck.of(MusicCard(kind = "artist", mbid = "r1")))
        assertEquals(MusicOpenKind.GENRE, MusicRules.target(MusicCard(kind = "genre", mbid = "pop")).kind)
        assertEquals(MusicOpenKind.LIBRARY_ALBUM, MusicRules.target(MusicCard(kind = "album", titleId = 4, partId = 5)).kind)
        assertEquals(MusicOpenKind.SEARCH, MusicRules.target(MusicCard(kind = "album", artist = "Ann")).kind)
        val page = MusicRules.pageTarget("/music/decade/90s")
        assertEquals(MusicOpenKind.PAGE, page.kind)
        assertEquals("decade", page.page)
        assertEquals("90s", page.slug)
        assertEquals(MusicOpenKind.GENRE, MusicRules.pageTarget("/music/genre/pop").kind)
        assertEquals(MusicOpenKind.NONE, MusicRules.pageTarget("/elsewhere").kind)
        val tile = MusicRules.tileTarget(MusicBrowseTile(key = "mood:chill", href = "/music/mood/chill"))
        assertEquals("chill", tile.slug)
    }

    @Test
    fun tintDarkensTheCoverColour() {
        assertNull(MusicRules.tint("nope"))
        val tint = assertNotNull(MusicRules.tint("#d62839"))
        val r = (tint shr 16) and 0xff
        assertTrue(r < 0x80, "dark enough for white text")
    }

    @Test
    fun imagesGetASize() {
        assertEquals("/api/v1/music-trending/art/album/x?size=250", MusicRules.image("/api/v1/music-trending/art/album/x", 250))
        assertEquals("/api/v1/albums/3/cover?size=500", MusicRules.image("/api/v1/albums/3/cover"))
        assertEquals("/api/v1/music/genre-art/pop?v=2", MusicRules.image("/api/v1/music/genre-art/pop?v=2"))
        assertEquals("https://x.test/a.jpg", MusicRules.image("https://x.test/a.jpg"))
    }

    @Test
    fun tileColoursAreStable() {
        assertEquals("#d3336e", MusicRules.tileColor("genre:pop"))
        assertEquals("#3b6ea5", MusicRules.tileColor("decade:2000s"))
        assertEquals(MusicRules.tileColor("genre:unknown-x"), MusicRules.tileColor("mood:unknown-x"))
        assertEquals(30, MusicRules.bandHue(2))
        assertEquals(345, MusicRules.bandHue(-1))
    }

    @Test
    fun kindGroupsKeepTheirOrder() {
        assertEquals(listOf("album", "single"), MusicOptions.toggle(listOf("single"), "album", true, MusicOptions.albumTypes))
        assertEquals(listOf("album"), MusicOptions.toggle(listOf("album", "ep"), "ep", false, MusicOptions.albumTypes))
        assertEquals(MusicKindsPreset.ALBUMS_EPS, kindsPreset(listOf("album", "ep"), listOf("studio"), listOf("official")))
        assertEquals(MusicKindsPreset.CUSTOM, kindsPreset(listOf("album", "ep"), listOf("studio", "live"), listOf("official")))
    }

    @Test
    fun loadProblemsAreNamed() {
        assertEquals(MusicProblem.NOT_FOUND, musicProblem(ApiError(ApiErrorCode.NOT_FOUND, 404)))
        assertEquals(MusicProblem.FORBIDDEN, musicProblem(ApiError(ApiErrorCode.FORBIDDEN, 403)))
        assertEquals(MusicProblem.OFFLINE, musicProblem(ApiError(ApiErrorCode.OFFLINE)))
        assertEquals(MusicProblem.SLOW, musicProblem(ApiError(ApiErrorCode.SERVER, 500)))
        assertEquals(MusicShape.WIDE, MusicRules.shape("wide"))
    }
}

private data class MusicTargetCheck(
    val kind: MusicOpenKind,
    val mbid: String,
    val artist: String,
) {
    companion object {
        fun of(card: MusicCard): MusicTargetCheck = MusicRules.target(card).let { MusicTargetCheck(it.kind, it.mbid, it.artist) }
    }
}
