package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.model.DiscoverFresh
import ch.orellbuehler.acolyte.api.model.DiscoverItem
import ch.orellbuehler.acolyte.api.model.OwnedItem
import ch.orellbuehler.acolyte.api.model.TitleAsk
import ch.orellbuehler.acolyte.feature.discover.CardAction
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.CardRules
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.FreshKind
import ch.orellbuehler.acolyte.feature.discover.MarkLabel
import ch.orellbuehler.acolyte.feature.discover.MarkState
import ch.orellbuehler.acolyte.feature.discover.OwnedIndex
import ch.orellbuehler.acolyte.feature.discover.card
import ch.orellbuehler.acolyte.feature.discover.dedupeTop
import ch.orellbuehler.acolyte.feature.discover.enoughCards
import ch.orellbuehler.acolyte.feature.discover.qualityBadge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiscoverCardsTest {
    private val member = CardRules(staff = false, canManage = false, canAsk = true)
    private val staff = CardRules(staff = true, canManage = true, canAsk = true)

    private fun owned(vararg items: OwnedItem) = OwnedIndex(items.toList())

    @Test
    fun ownedStatesMapToMarks() {
        val index =
            owned(
                OwnedItem(kind = "movie", tmdb = "1", id = 11, state = "owned"),
                OwnedItem(kind = "show", tmdb = "2", id = 12, state = "partial"),
                OwnedItem(kind = "movie", tmdb = "3", id = 13, state = "downloading", percent = 42.4),
                OwnedItem(kind = "movie", tmdb = "4", id = 14, state = "requested"),
                OwnedItem(kind = "movie", tmdb = "5", id = 15, state = "wanted"),
            )
        val cards =
            listOf("1" to "movie", "2" to "show", "3" to "movie", "4" to "movie", "5" to "movie").map { (tmdb, kind) ->
                DiscoverItem(tmdbId = tmdb, kind = kind, name = "x").card("trending", member, index)
            }
        assertEquals(MarkLabel.IN_LIBRARY, cards[0].mark?.label)
        assertEquals(11, cards[0].titleId)
        assertEquals(MarkState.PARTIAL, cards[1].mark?.state)
        assertEquals(MarkState.DOWNLOADING, cards[2].mark?.state)
        assertEquals(42, cards[2].mark?.percent)
        assertEquals(MarkLabel.REQUESTED, cards[3].mark?.label)
        assertEquals(MarkLabel.WAITING_FOR_COPY, cards[4].mark?.label)
        assertTrue(cards.all { it.action == null })
    }

    @Test
    fun askStatesWithoutLibraryMatch() {
        val index = OwnedIndex()

        fun card(
            state: String,
            mine: Boolean = false,
            rules: CardRules = member,
        ) = DiscoverItem(tmdbId = "9", kind = "movie", ask = TitleAsk(state = state, mine = mine)).card("best", rules, index)
        assertEquals(MarkLabel.REQUESTED, card("asked").mark?.label)
        assertEquals(MarkLabel.APPROVED_WAITING, card("approved").mark?.label)
        assertNull(card("turned_down").mark)
        assertEquals(MarkState.DECLINED, card("turned_down", rules = staff).mark?.state)
        assertNull(card("asked", mine = true).action)
        assertEquals(CardAction.ASK, card("asked", mine = false).action)
        assertEquals(CardAction.ADD, card("asked", rules = staff).action)
    }

    @Test
    fun noActionWithoutRightsOrInLibrary() {
        val index = OwnedIndex()
        val nobody = CardRules()
        assertNull(DiscoverItem(tmdbId = "9", kind = "movie").card("best", nobody, index).action)
        val inLibrary = DiscoverItem(tmdbId = "9", kind = "movie", inLibrary = true, titleId = 4).card("best", staff, index)
        assertNull(inLibrary.action)
        assertEquals(MarkLabel.IN_LIBRARY, inLibrary.mark?.label)
        assertNull(DiscoverItem(tmdbId = "", kind = "movie").card("best", staff, index).action)
    }

    @Test
    fun freshStripsAreSuppressedByRowType() {
        val index = OwnedIndex()
        val fresh = DiscoverFresh(kind = "new_release", text = "New Release")
        assertNull(DiscoverItem(tmdbId = "1", kind = "movie", fresh = fresh).card("best", member, index).fresh)
        assertEquals(
            FreshKind.NEW_RELEASE,
            DiscoverItem(tmdbId = "1", kind = "movie", fresh = fresh)
                .card("trending", member, index)
                .fresh
                ?.kind,
        )
        assertNull(DiscoverItem(tmdbId = "1", kind = "movie", fresh = DiscoverFresh("odd", "x")).card("trending", member, index).fresh)
        assertNull(DiscoverItem(tmdbId = "1", kind = "movie", fresh = DiscoverFresh("coming", "")).card("trending", member, index).fresh)
        assertNull(
            DiscoverItem(
                tmdbId = "1",
                kind = "show",
                fresh = DiscoverFresh("new_season", "New Season"),
            ).card("new_seasons", member, index).fresh,
        )
    }

    @Test
    fun widePictureSkipsArtWithTitle() {
        val index = OwnedIndex()
        val art = DiscoverItem(tmdbId = "1", kind = "movie", poster = "p", cardArt = "a", artHasTitle = true).card("x", member, index)
        assertEquals("p", art.wideImage)
        val plain = DiscoverItem(tmdbId = "1", kind = "movie", poster = "p", cardArt = "a").card("x", member, index)
        assertEquals("a", plain.wideImage)
    }

    private fun cards(vararg ids: Int): List<DiscoverCard> = ids.map { DiscoverCard("k$it", CardKind.MOVIE, "$it", 0, "n$it", 0) }

    @Test
    fun dedupeKeepsRowsAboveSix() {
        val rows =
            listOf(
                CardRow("a", "trending", "A", cards = cards(1, 2, 3, 4, 5, 6, 7, 8)),
                CardRow("b", "best", "B", cards = cards(1, 2, 3, 9, 10, 11, 12, 13, 14)),
                CardRow("c", "best", "C", cards = cards(1, 2, 3, 4, 5, 6, 7)),
            )
        val out = dedupeTop(rows, setOf("movie:9"))
        assertEquals(8, out[0].cards.size)
        assertEquals(listOf("10", "11", "12", "13", "14"), out[1].cards.map { it.tmdb }.filter { it.toInt() > 9 })
        assertEquals(6, out[1].cards.size)
        assertEquals(6, out[2].cards.size)
        val kept = enoughCards(out)
        assertEquals(listOf("a"), kept.map { it.id })
        assertTrue(enoughCards(listOf(CardRow("h", "household", "", cards = cards(1, 2, 3, 4)))).isNotEmpty())
        assertFalse(enoughCards(listOf(CardRow("t", "trending", "", cards = cards(1, 2, 3, 4)))).isNotEmpty())
    }

    @Test
    fun qualityBadges() {
        assertEquals("4K", qualityBadge(2160))
        assertEquals("1080p", qualityBadge(1080))
        assertEquals("720p", qualityBadge(720))
        assertEquals("480p", qualityBadge(480))
    }
}
