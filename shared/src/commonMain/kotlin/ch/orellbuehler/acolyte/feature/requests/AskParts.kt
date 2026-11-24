package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.model.AlbumSongs
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.AskEpisodes
import ch.orellbuehler.acolyte.api.model.AskableAlbum

enum class AlbumKind {
    ALBUMS,
    SINGLES,
    LIVE,
    COMPILATIONS,
    OTHER,
}

data class AlbumGroup(
    val mbid: String,
    val name: String,
    val year: Int,
    val kind: AlbumKind,
    val ids: List<String>,
    val editions: List<String>,
)

enum class Tick {
    ALL,
    SOME,
    NONE,
}

data class SeasonRow(
    val number: Int,
    val episodes: List<Int>,
    val asked: List<Int>?,
)

data class SeasonPick(
    val number: Int,
    val all: List<Int>,
    val asked: List<Int>,
    val ticked: List<Int>,
    val tick: Tick,
) {
    val changed: Boolean get() = ticked.size != asked.size || ticked.any { it !in asked }
}

data class AlbumPick(
    val group: AlbumGroup,
    val songs: List<String>,
    val ticked: Boolean,
    val keptSongs: List<String>,
) {
    val tick: Tick
        get() =
            when {
                !ticked || (songs.isNotEmpty() && keptSongs.isEmpty()) -> Tick.NONE
                songs.isNotEmpty() && keptSongs.size < songs.size -> Tick.SOME
                else -> Tick.ALL
            }
}

internal data class PartsChoice(
    val seasons: List<Int>? = null,
    val episodes: List<AskEpisodes>? = null,
    val albums: List<String>? = null,
    val songs: List<AlbumSongs>? = null,
    val changed: Boolean = false,
    val empty: Boolean = false,
)

private val editionWords =
    Regex(
        "\\b(deluxe|remaster(ed)?|anniversary|expanded|edition|version|explicit|clean|bonus|special|super|collector'?s?|reissue|mono|" +
            "stereo|japan(ese)?|uk|us|international|tour|platinum|complete|legacy|redux|\\d{1,2}(st|nd|rd|th))\\b",
        RegexOption.IGNORE_CASE,
    )
private val tagged = Regex("^(.*?)\\s*(?:[(\\[]([^()\\[\\]]*)[)\\]]|\\s[-–:]\\s+([^-–:]+))$")
private val liveName = Regex("^live\\s+(at|in|from|on)\\b|[(\\[]live[^)\\]]*[)\\]]", RegexOption.IGNORE_CASE)
private val notAlbumName =
    Regex("\\b(demos?|bootlegs?|outtakes?|unreleased|rehearsals?|b-sides|leaked|snippets?|remix(es)?|remixed)\\b", RegexOption.IGNORE_CASE)
private val spaces = Regex("\\s+")

fun baseName(name: String): String {
    var s = name.trim()
    while (true) {
        val m = tagged.find(s) ?: break
        val head = m.groupValues[1]
        val tag = m.groupValues[2].ifEmpty { m.groupValues[3] }
        if (head.isEmpty() || !editionWords.containsMatchIn(tag)) break
        s = head
    }
    return s.lowercase().replace(spaces, " ").trim()
}

fun albumKind(
    type: String,
    secondaryTypes: List<String>,
    name: String = "",
): AlbumKind {
    if (name.isNotEmpty() && liveName.containsMatchIn(name)) return AlbumKind.LIVE
    if (name.isNotEmpty() && notAlbumName.containsMatchIn(name)) return AlbumKind.OTHER
    val sec = secondaryTypes.map { it.lowercase() }.filter { it != "studio" }
    return when {
        "live" in sec -> AlbumKind.LIVE
        "compilation" in sec -> AlbumKind.COMPILATIONS
        sec.isNotEmpty() -> AlbumKind.OTHER
        type.lowercase() == "album" -> AlbumKind.ALBUMS
        type.lowercase() == "ep" || type.lowercase() == "single" -> AlbumKind.SINGLES
        else -> AlbumKind.OTHER
    }
}

fun keptByDefault(album: AskableAlbum): Boolean {
    if (album.type.lowercase() !in setOf("album", "ep")) return false
    return album.secondaryTypes.all { it.lowercase() == "studio" }
}

internal fun yearOf(date: String): Int = date.take(4).toIntOrNull() ?: 0

fun albumGroups(albums: List<AskableAlbum>): List<AlbumGroup> {
    val groups = albums.groupBy { "${albumKind(it.type, it.secondaryTypes, it.name)}|${baseName(it.name)}" }
    return groups.values
        .map { list ->
            val sorted = list.sortedWith(compareBy<AskableAlbum> { it.releaseDate.ifEmpty { "9999" } }.thenBy { it.name.length })
            val main =
                sorted.firstOrNull {
                    baseName(it.name) ==
                        it.name
                            .trim()
                            .lowercase()
                            .replace(spaces, " ")
                } ?: sorted.first()
            val rest = sorted.filter { it !== main }
            AlbumGroup(
                mbid = main.mbid,
                name = main.name,
                year = yearOf(main.releaseDate),
                kind = albumKind(main.type, main.secondaryTypes, main.name),
                ids = listOf(main.mbid) + rest.map { it.mbid },
                editions = rest.map { it.name },
            )
        }.sortedWith(
            compareBy<AlbumGroup> { it.kind.ordinal }.thenBy { if (it.year == 0) 9999 else it.year }.thenBy { it.name.lowercase() },
        )
}

fun albumCounts(groups: List<AlbumGroup>): Map<AlbumKind, Int> =
    AlbumKind.entries.associateWith { kind -> groups.count { it.kind == kind } }.filterValues { it > 0 }

fun askAlbums(
    ask: Ask,
    known: List<AskableAlbum>,
    stored: Collection<String> = emptyList(),
): List<AskableAlbum> {
    if (ask.albums.isNotEmpty()) {
        return ask.albums.map { a ->
            known.firstOrNull { it.mbid == a.mbid }
                ?: AskableAlbum(mbid = a.mbid, name = a.name, type = "album", releaseDate = if (a.year > 0) a.year.toString() else "")
        }
    }
    val defaults = known.filter { keptByDefault(it) }
    val extra =
        stored.filter { id -> defaults.none { it.mbid == id } }.map { id ->
            known.firstOrNull { it.mbid == id }
                ?: AskableAlbum(mbid = id, name = id)
        }
    return defaults + extra
}

fun askSeasonRows(
    ask: Ask,
    known: Map<Int, List<Int>>,
): List<SeasonRow> {
    if (ask.futureOnly) return emptyList()
    val regular = known.keys.filter { it > 0 }.sorted()
    val single = ask.episodes.filter { it.episodes.isNotEmpty() }
    val want =
        if (ask.seasons.isNotEmpty() || single.isNotEmpty()) {
            ask.seasons + single.map { it.season }
        } else {
            regular
        }
    return want.toSet().sorted().map { n ->
        val asked =
            if (n in ask.seasons) {
                null
            } else {
                single
                    .firstOrNull { it.season == n }
                    ?.episodes
                    ?.toSet()
                    ?.sorted()
            }
        SeasonRow(n, known[n].orEmpty(), asked)
    }
}

fun seasonPick(
    row: SeasonRow,
    ticked: List<Int>?,
): SeasonPick {
    val all = row.episodes
    val asked = row.asked ?: all
    val kept = (ticked ?: asked).filter { all.isEmpty() || it in all }.toSet().sorted()
    val tick =
        when {
            all.isEmpty() -> if (ticked != null && ticked.isEmpty()) Tick.NONE else Tick.ALL
            kept.isEmpty() -> Tick.NONE
            kept.size < all.size -> Tick.SOME
            else -> Tick.ALL
        }
    return SeasonPick(row.number, all, asked, kept, tick)
}

internal fun seasonsChoice(picks: List<SeasonPick>): PartsChoice {
    if (picks.isEmpty()) return PartsChoice()
    val empty = picks.all { it.tick == Tick.NONE }
    val changed = picks.any { it.changed || (it.all.isEmpty() && it.tick == Tick.NONE) }
    if (!changed || empty) return PartsChoice(changed = changed, empty = empty)
    val seasons = picks.filter { it.tick == Tick.ALL }.map { it.number }
    val episodes = picks.filter { it.tick == Tick.SOME }.map { AskEpisodes(it.number, it.ticked) }
    return PartsChoice(seasons = seasons, episodes = episodes.ifEmpty { null }, changed = true)
}

internal fun albumsChoice(picks: List<AlbumPick>): PartsChoice {
    if (picks.isEmpty()) return PartsChoice()
    val empty = picks.all { it.tick == Tick.NONE }
    val changed = picks.any { it.tick != Tick.ALL }
    if (!changed || empty) return PartsChoice(changed = changed, empty = empty)
    val kept = picks.filter { it.tick != Tick.NONE }
    val songs = kept.filter { it.tick == Tick.SOME }.map { AlbumSongs(it.group.mbid, it.keptSongs) }
    return PartsChoice(albums = kept.flatMap { it.group.ids }, songs = songs.ifEmpty { null }, changed = true)
}
