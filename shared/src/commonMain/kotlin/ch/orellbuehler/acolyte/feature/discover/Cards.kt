package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.BrowseTile
import ch.orellbuehler.acolyte.api.model.DiscoverFresh
import ch.orellbuehler.acolyte.api.model.DiscoverItem
import ch.orellbuehler.acolyte.api.model.DiscoverRow
import ch.orellbuehler.acolyte.api.model.OwnedItem
import ch.orellbuehler.acolyte.api.model.TitleAsk
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.session.Account
import kotlin.math.roundToInt

enum class CardKind(
    val wire: String,
) {
    MOVIE("movie"),
    SHOW("show"),
    ARTIST("artist"),
    ;

    internal companion object {
        fun of(wire: String): CardKind = entries.firstOrNull { it.wire == wire } ?: MOVIE
    }
}

enum class MarkState {
    AVAILABLE,
    PARTIAL,
    DOWNLOADING,
    REQUESTED,
    DECLINED,
}

enum class MarkLabel {
    IN_LIBRARY,
    PARTLY_AVAILABLE,
    DOWNLOADING,
    REQUESTED,
    APPROVED_WAITING,
    WAITING_FOR_COPY,
    DECLINED,
    HIDDEN,
}

data class CardMark(
    val state: MarkState,
    val label: MarkLabel,
    val percent: Int = -1,
)

enum class FreshKind(
    internal val wire: String,
) {
    NEW_EPISODE("new_episode"),
    NEW_SEASON("new_season"),
    RECENTLY_ADDED("recently_added"),
    COMING("coming"),
    SEASON_COMING("season_coming"),
    NEW_RELEASE("new_release"),
    ;

    internal companion object {
        fun of(wire: String): FreshKind? = entries.firstOrNull { it.wire == wire }
    }
}

data class CardFresh(
    val kind: FreshKind,
    val text: String,
)

enum class CardAction {
    ADD,
    ASK,
}

data class DiscoverCard(
    val key: String,
    val kind: CardKind,
    val tmdb: String,
    val titleId: Long,
    val name: String,
    val year: Int,
    val subtitle: String = "",
    val poster: String? = null,
    val backdrop: String? = null,
    val logo: String? = null,
    val overview: String = "",
    val genres: List<String> = emptyList(),
    val mark: CardMark? = null,
    val fresh: CardFresh? = null,
    val action: CardAction? = null,
    val streaming: String = "",
    val rank: Int = 0,
    val mine: Boolean = false,
    val rowType: String = "",
    val rowTitle: String = "",
    val note: CardNote? = null,
) {
    val opensTitle: Boolean get() = tmdb.isNotEmpty() && kind != CardKind.ARTIST

    val wideImage: String? get() = backdrop ?: poster

    val posterImage: String? get() = poster ?: backdrop
}

enum class NoteKind {
    DOWNLOADING,
    MISSING_EPISODES,
    SEASONS_UP_TO_DATE,
    MISSING_TRACKS,
    ALBUMS_UP_TO_DATE,
    DIRECTOR,
}

data class CardNote(
    val kind: NoteKind,
    val count: Int = 0,
)

data class CardRow(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String = "",
    val cards: List<DiscoverCard> = emptyList(),
    val wide: Boolean = true,
    val ranked: Boolean = false,
    val seeAll: Boolean = false,
    val layer: String = "",
)

data class PersonCard(
    val id: String,
    val name: String,
    val note: String = "",
    val photo: String? = null,
)

enum class TitleStatus(
    val tone: Tone,
) {
    IN_LIBRARY(Tone.OK),
    DOWNLOADING(Tone.INFO),
    MISSING(Tone.WAIT),
    REQUESTED(Tone.IDLE),
    APPROVED(Tone.OK),
    TURNED_DOWN(Tone.IDLE),
    FAILED(Tone.FAIL),
    COMING_SOON(Tone.IDLE),
}

internal data class CardRules(
    val staff: Boolean = false,
    val canManage: Boolean = false,
    val canAsk: Boolean = false,
    val canHide: Boolean = false,
) {
    companion object {
        fun of(account: Account?): CardRules {
            account ?: return CardRules()
            return CardRules(
                staff = account.can(Need.MANAGE_LIBRARY) || account.can(Need.APPROVE),
                canManage = account.can(Need.MANAGE_LIBRARY),
                canAsk = account.allows(Need.ASK, Part.WISHLIST),
                canHide = account.can(Need.USERS),
            )
        }
    }
}

internal class OwnedIndex(
    internal val items: List<OwnedItem> = emptyList(),
) {
    private val byKey: Map<String, OwnedItem> =
        items.filter { it.tmdb.isNotEmpty() || it.mbid.isNotEmpty() }.associateBy { "${it.kind}:${it.tmdb.ifEmpty { it.mbid }}" }

    private val byId: Map<Long, OwnedItem> = items.filter { it.kind != "album" }.associateBy { it.id }

    internal fun find(
        kind: String,
        id: String,
    ): OwnedItem? = if (id.isEmpty()) null else byKey["$kind:$id"]

    internal fun byTitleId(id: Long): OwnedItem? = if (id <= 0) null else byId[id]

    val isEmpty: Boolean get() = items.isEmpty()

    internal companion object {
        val empty = OwnedIndex()
    }
}

internal val HAVE_STATES = setOf("owned", "available", "in_library")

internal fun ownedMark(
    owned: OwnedItem,
    ask: TitleAsk?,
    staff: Boolean,
): CardMark =
    when (owned.state) {
        "downloading" -> CardMark(MarkState.DOWNLOADING, MarkLabel.DOWNLOADING, if (owned.percent > 0) owned.percent.roundToInt() else -1)
        in HAVE_STATES -> CardMark(MarkState.AVAILABLE, MarkLabel.IN_LIBRARY)
        "partial" -> CardMark(MarkState.PARTIAL, MarkLabel.PARTLY_AVAILABLE)
        else ->
            when {
                ask?.state == "turned_down" && staff -> CardMark(MarkState.DECLINED, MarkLabel.DECLINED)
                ask?.state == "approved" || owned.state == "approved" -> CardMark(MarkState.REQUESTED, MarkLabel.APPROVED_WAITING)
                owned.state == "requested" || owned.state == "asked" || ask?.state == "asked" ->
                    CardMark(MarkState.REQUESTED, MarkLabel.REQUESTED)
                else -> CardMark(MarkState.REQUESTED, MarkLabel.WAITING_FOR_COPY)
            }
    }

internal fun plainMark(
    inLibrary: Boolean,
    ask: TitleAsk?,
    hidden: Boolean,
    staff: Boolean,
): CardMark? =
    when {
        inLibrary -> CardMark(MarkState.AVAILABLE, MarkLabel.IN_LIBRARY)
        ask?.state == "asked" -> CardMark(MarkState.REQUESTED, MarkLabel.REQUESTED)
        ask?.state == "approved" -> CardMark(MarkState.REQUESTED, MarkLabel.APPROVED_WAITING)
        ask?.state == "available" -> CardMark(MarkState.AVAILABLE, MarkLabel.IN_LIBRARY)
        ask?.state == "turned_down" && staff -> CardMark(MarkState.DECLINED, MarkLabel.DECLINED)
        hidden && staff -> CardMark(MarkState.DECLINED, MarkLabel.HIDDEN)
        else -> null
    }

internal fun cardAction(
    kind: CardKind,
    tmdb: String,
    titleId: Long,
    mark: CardMark?,
    mine: Boolean,
    neverAdd: Boolean,
    rules: CardRules,
): CardAction? {
    if (kind == CardKind.ARTIST || tmdb.isEmpty() || titleId > 0 || neverAdd) return null
    if (mark?.state == MarkState.AVAILABLE || mark?.state == MarkState.PARTIAL || mark?.state == MarkState.DOWNLOADING) return null
    return when {
        rules.canManage -> CardAction.ADD
        rules.canAsk && !mine -> CardAction.ASK
        else -> null
    }
}

private val SUPPRESS: Map<String, Set<FreshKind>> =
    mapOf(
        "best" to setOf(FreshKind.NEW_RELEASE),
        "new_services" to setOf(FreshKind.NEW_RELEASE),
        "np_services" to setOf(FreshKind.NEW_RELEASE),
        "fresh" to setOf(FreshKind.NEW_RELEASE),
        "new_from" to setOf(FreshKind.NEW_RELEASE),
        "just_arrived" to setOf(FreshKind.RECENTLY_ADDED),
        "recent" to setOf(FreshKind.RECENTLY_ADDED),
        "coming_soon" to setOf(FreshKind.COMING, FreshKind.SEASON_COMING),
        "upcoming" to setOf(FreshKind.COMING, FreshKind.SEASON_COMING),
        "new_episodes" to setOf(FreshKind.NEW_EPISODE),
        "new_seasons" to setOf(FreshKind.NEW_SEASON),
    )

internal fun freshFor(
    fresh: DiscoverFresh?,
    rowType: String,
    kind: CardKind,
): CardFresh? {
    fresh ?: return null
    val freshKind = FreshKind.of(fresh.kind) ?: return null
    if (fresh.text.isBlank()) return null
    if (kind == CardKind.ARTIST && freshKind != FreshKind.RECENTLY_ADDED && freshKind != FreshKind.NEW_RELEASE) return null
    if (SUPPRESS[rowType]?.contains(freshKind) == true) return null
    return CardFresh(freshKind, fresh.text)
}

internal fun DiscoverItem.card(
    rowType: String,
    rules: CardRules,
    owned: OwnedIndex,
    rowTitle: String = "",
): DiscoverCard {
    val cardKind = CardKind.of(kind)
    val ownedItem = owned.find(kind, tmdbId)
    val visibleAsk = ask?.takeUnless { it.state == "turned_down" && !rules.staff }
    val id = if (inLibrary && titleId > 0) titleId else ownedItem?.id ?: 0
    val mark = if (ownedItem != null) ownedMark(ownedItem, ask, rules.staff) else plainMark(inLibrary, ask, false, rules.staff)
    val mine = visibleAsk?.mine == true
    val picture = backdrop ?: cardArt?.takeUnless { artHasTitle }
    return DiscoverCard(
        key = "$rowType-$kind-$tmdbId",
        kind = cardKind,
        tmdb = tmdbId,
        titleId = id,
        name = name,
        year = year,
        subtitle = what,
        poster = poster,
        backdrop = picture,
        logo = cardLogo,
        overview = overview,
        genres = genres,
        mark = mark,
        fresh = freshFor(fresh, rowType, cardKind),
        action = cardAction(cardKind, tmdbId, id, mark, mine, false, rules),
        streaming = streaming?.name.orEmpty(),
        rank = if (rank > 0) rank else streaming?.rank ?: 0,
        mine = mine,
        rowType = rowType,
        rowTitle = rowTitle,
    )
}

internal fun DiscoverRow.cardRow(
    rules: CardRules,
    owned: OwnedIndex,
): CardRow =
    CardRow(
        id = id.ifEmpty { type },
        type = type,
        title = title,
        subtitle = subtitle,
        cards = items.map { it.card(type, rules, owned, title) }.distinctBy { it.key },
        wide = wide || items.any { it.kind == "movie" || it.kind == "show" },
        ranked = type == "np_top_shows" || type == "np_top_movies",
        layer = titleLayer,
    )

internal fun identity(card: DiscoverCard): String = "${card.kind.wire}:${card.tmdb}"

internal fun dedupeTop(
    rows: List<CardRow>,
    taken: Set<String> = emptySet(),
    first: Int = 8,
    min: Int = 6,
): List<CardRow> {
    val seen = taken.toMutableSet()
    var at = 0
    return rows.map { row ->
        if (at >= first || row.cards.isEmpty()) return@map row
        at++
        val fresh = row.cards.filter { identity(it) !in seen }
        val floor = minOf(min, row.cards.size)
        val cards =
            if (fresh.size < floor) {
                val back =
                    row.cards
                        .filter { identity(it) in seen }
                        .take(floor - fresh.size)
                        .toSet()
                row.cards.filter { identity(it) !in seen || it in back }
            } else {
                fresh
            }
        cards.forEach { seen += identity(it) }
        if (cards.size == row.cards.size) row else row.copy(cards = cards)
    }
}

internal val ROW_MIN: Map<String, Int> =
    mapOf(
        "because_have" to 6,
        "because_asked" to 6,
        "person" to 6,
        "new_services" to 6,
        "household" to 4,
        "hidden" to 6,
        "top_services" to 1,
        "collection" to 2,
        "cinemas" to 6,
        "new_seasons" to 4,
        "returning" to 4,
    )

internal fun enoughCards(rows: List<CardRow>): List<CardRow> = rows.filter { it.cards.size >= (ROW_MIN[it.type] ?: 8) }

fun qualityBadge(height: Int): String =
    when {
        height >= 2000 -> "4K"
        height >= 1000 -> "1080p"
        height >= 700 -> "720p"
        else -> "${height}p"
    }

fun tileHue(name: String): Int {
    var hash = 0x811c9dc5L
    for (char in name.lowercase()) {
        hash = hash xor char.code.toLong()
        hash = (hash * 0x01000193L) and 0xffffffffL
    }
    return (hash % TILE_HUES).toInt()
}

private const val TILE_HUES = 12

object DiscoverKit {
    fun hue(name: String): Int = tileHue(name)

    fun slug(name: String): String = genreSlug(name)

    fun link(
        tile: BrowseTile,
        tab: ExploreType,
    ): BrowseLink = browseLink(tile, tab)

    fun genres(type: ExploreType): List<Genre> = Genres.of(type)

    fun stripKey(tile: BrowseTile): String = ExplorePresenter.stripKey(tile)

    fun genreName(
        id: Int,
        type: ExploreType,
    ): String = Genres.named(id, type)?.name.orEmpty()
}
