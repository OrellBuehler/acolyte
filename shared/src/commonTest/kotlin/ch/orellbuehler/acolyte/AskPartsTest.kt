package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.AlbumSongs
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.AskAlbum
import ch.orellbuehler.acolyte.api.model.AskEpisodes
import ch.orellbuehler.acolyte.api.model.AskableAlbum
import ch.orellbuehler.acolyte.feature.requests.AlbumKind
import ch.orellbuehler.acolyte.feature.requests.AlbumPick
import ch.orellbuehler.acolyte.feature.requests.Tick
import ch.orellbuehler.acolyte.feature.requests.albumCounts
import ch.orellbuehler.acolyte.feature.requests.albumGroups
import ch.orellbuehler.acolyte.feature.requests.albumKind
import ch.orellbuehler.acolyte.feature.requests.albumsChoice
import ch.orellbuehler.acolyte.feature.requests.askAlbums
import ch.orellbuehler.acolyte.feature.requests.askSeasonRows
import ch.orellbuehler.acolyte.feature.requests.baseName
import ch.orellbuehler.acolyte.feature.requests.keptByDefault
import ch.orellbuehler.acolyte.feature.requests.seasonPick
import ch.orellbuehler.acolyte.feature.requests.seasonsChoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AskPartsTest {
    @Test
    fun baseNameDropsEditionTags() {
        assertEquals("album", baseName("Album (Deluxe Edition)"))
        assertEquals("album", baseName("Album - 2011 Remaster"))
        assertEquals("album", baseName("Album [20th Anniversary] (Remastered)"))
        assertEquals("live at leeds (part two)", baseName("Live at Leeds (Part Two)"))
        assertEquals("night", baseName("  Night  "))
    }

    @Test
    fun albumKindsFollowTypesAndNames() {
        assertEquals(AlbumKind.ALBUMS, albumKind("album", emptyList()))
        assertEquals(AlbumKind.ALBUMS, albumKind("album", listOf("studio")))
        assertEquals(AlbumKind.SINGLES, albumKind("ep", emptyList()))
        assertEquals(AlbumKind.SINGLES, albumKind("single", emptyList()))
        assertEquals(AlbumKind.LIVE, albumKind("album", listOf("live")))
        assertEquals(AlbumKind.LIVE, albumKind("album", emptyList(), "Live at Lowlands"))
        assertEquals(AlbumKind.COMPILATIONS, albumKind("album", listOf("compilation")))
        assertEquals(AlbumKind.OTHER, albumKind("album", emptyList(), "The Demos"))
        assertEquals(AlbumKind.OTHER, albumKind("broadcast", emptyList()))
        assertTrue(keptByDefault(AskableAlbum(type = "album")))
        assertTrue(keptByDefault(AskableAlbum(type = "ep", secondaryTypes = listOf("studio"))))
        assertFalse(keptByDefault(AskableAlbum(type = "album", secondaryTypes = listOf("live"))))
        assertFalse(keptByDefault(AskableAlbum(type = "single")))
    }

    @Test
    fun editionsFoldUnderTheOldestPlainOne() {
        val albums =
            listOf(
                AskableAlbum("a2", "Album (Deluxe)", "album", releaseDate = "2012-01-01"),
                AskableAlbum("a1", "Album", "album", releaseDate = "2010-05-01"),
                AskableAlbum("s1", "Song", "single", releaseDate = "2009-01-01"),
                AskableAlbum("l1", "Album", "album", listOf("live"), "2011-01-01"),
                AskableAlbum("b1", "Second", "album", releaseDate = "2014-01-01"),
            )
        val groups = albumGroups(albums)
        assertEquals(listOf("a1", "b1", "s1", "l1"), groups.map { it.mbid })
        assertEquals(listOf("a1", "a2"), groups[0].ids)
        assertEquals(2010, groups[0].year)
        assertEquals(mapOf(AlbumKind.ALBUMS to 2, AlbumKind.SINGLES to 1, AlbumKind.LIVE to 1), albumCounts(groups))
    }

    @Test
    fun askAlbumsUsesTheAskOrTheDefaults() {
        val known = listOf(AskableAlbum("x", "X", "album"), AskableAlbum("y", "Y", "single"))
        val named = askAlbums(Ask(albums = listOf(AskAlbum("y", "Y"), AskAlbum("z", "Zed", 2001))), known)
        assertEquals(listOf("y", "z"), named.map { it.mbid })
        assertEquals("2001", named[1].releaseDate)
        assertEquals(listOf("x"), askAlbums(Ask(), known).map { it.mbid })
        assertEquals(listOf("x", "y"), askAlbums(Ask(), known, stored = listOf("x", "y")).map { it.mbid })
    }

    @Test
    fun seasonRowsCoverAskedSeasonsAndSingleEpisodes() {
        val known = mapOf(0 to listOf(1), 1 to listOf(1, 2, 3), 2 to listOf(1, 2), 3 to listOf(1))
        val whole = askSeasonRows(Ask(kind = "show"), known)
        assertEquals(listOf(1, 2, 3), whole.map { it.number })
        assertTrue(whole.all { it.asked == null })
        val some = askSeasonRows(Ask(kind = "show", seasons = listOf(1), episodes = listOf(AskEpisodes(3, listOf(1)))), known)
        assertEquals(listOf(1, 3), some.map { it.number })
        assertNull(some[0].asked)
        assertEquals(listOf(1), some[1].asked)
        assertTrue(askSeasonRows(Ask(kind = "show", futureOnly = true), known).isEmpty())
    }

    @Test
    fun seasonPicksBuildTheApproveBody() {
        val known = mapOf(1 to listOf(1, 2, 3), 2 to listOf(1, 2))
        val rows = askSeasonRows(Ask(kind = "show"), known)
        val asAsked = rows.map { seasonPick(it, null) }
        assertEquals(listOf(Tick.ALL, Tick.ALL), asAsked.map { it.tick })
        val unchanged = seasonsChoice(asAsked)
        assertFalse(unchanged.changed)
        assertNull(unchanged.seasons)
        val narrowed = seasonsChoice(listOf(seasonPick(rows[0], listOf(1, 3)), seasonPick(rows[1], null)))
        assertTrue(narrowed.changed)
        assertEquals(listOf(2), narrowed.seasons)
        assertEquals(listOf(AskEpisodes(1, listOf(1, 3))), narrowed.episodes)
        val onlyEpisodes = seasonsChoice(listOf(seasonPick(rows[0], listOf(2)), seasonPick(rows[1], emptyList())))
        assertEquals(emptyList(), onlyEpisodes.seasons)
        assertEquals(listOf(AskEpisodes(1, listOf(2))), onlyEpisodes.episodes)
        val nothing = seasonsChoice(listOf(seasonPick(rows[0], emptyList()), seasonPick(rows[1], emptyList())))
        assertTrue(nothing.empty)
    }

    @Test
    fun unknownSeasonListsAreWholeUnlessUnticked() {
        val rows = askSeasonRows(Ask(kind = "show", seasons = listOf(4)), emptyMap())
        assertEquals(Tick.ALL, seasonPick(rows[0], null).tick)
        assertEquals(Tick.NONE, seasonPick(rows[0], emptyList()).tick)
    }

    @Test
    fun albumPicksSendOnlyWhenNarrowed() {
        val groups =
            albumGroups(
                listOf(
                    AskableAlbum("a1", "Album", "album"),
                    AskableAlbum("a2", "Album (Deluxe)", "album"),
                    AskableAlbum("b1", "B", "album"),
                ),
            )
        val all = groups.map { AlbumPick(it, listOf("r1", "r2"), true, listOf("r1", "r2")) }
        assertFalse(albumsChoice(all).changed)
        val partly = listOf(all[0].copy(keptSongs = listOf("r2")), all[1].copy(ticked = false))
        val choice = albumsChoice(partly)
        assertEquals(listOf("a1", "a2"), choice.albums)
        assertEquals(listOf(AlbumSongs("a1", listOf("r2"))), choice.songs)
        assertTrue(albumsChoice(all.map { it.copy(ticked = false) }).empty)
    }
}
