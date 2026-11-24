package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.feature.Tone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

enum class MusicMark {
    NONE,
    COMPLETE,
    PARTIAL,
    DOWNLOADING,
    MISSING,
    REQUESTED,
    APPROVED,
}

val MusicMark.tone: Tone
    get() =
        when (this) {
            MusicMark.NONE -> Tone.IDLE
            MusicMark.COMPLETE -> Tone.OK
            MusicMark.PARTIAL -> Tone.OK
            MusicMark.DOWNLOADING -> Tone.INFO
            MusicMark.MISSING -> Tone.WAIT
            MusicMark.REQUESTED -> Tone.IDLE
            MusicMark.APPROVED -> Tone.OK
        }

enum class MusicAlbumKind {
    ALBUM,
    EP,
    SINGLE,
    BROADCAST,
    COMPILATION,
    LIVE,
    SOUNDTRACK,
    REMIX,
    OTHER,
}

enum class MusicShelfKind {
    ALBUMS,
    SINGLES,
    COMPILATIONS,
    LIVE,
    OTHER,
}

enum class MusicShape {
    SQUARE,
    ROUND,
    TILE,
    SONG,
    GRID,
    WIDE,
}

enum class MusicCardKind {
    ALBUM,
    ARTIST,
    SONG,
    GENRE,
}

enum class MusicLineKind {
    NONE,
    TEXT,
    OUT_ON,
    OUT_NOW,
    RELEASED_ON,
    LISTENS,
    ARRIVED_ON,
    ARTIST_YEAR,
    ARTIST_TEXT,
    ASKED,
    APPROVED,
    TURNED_DOWN,
    ARRIVED,
}

data class MusicLine(
    val kind: MusicLineKind = MusicLineKind.NONE,
    val artist: String = "",
    val text: String = "",
    val date: String = "",
    val number: Long = 0,
)

enum class MusicOpenKind {
    ALBUM,
    ARTIST,
    GENRE,
    PAGE,
    LIBRARY_ALBUM,
    LIBRARY_ARTIST,
    SEARCH,
    NONE,
}

data class MusicTarget(
    val kind: MusicOpenKind,
    val mbid: String = "",
    val artist: String = "",
    val titleId: Long = 0,
    val partId: Long = 0,
    val page: String = "",
    val slug: String = "",
    val query: String = "",
)

data class MusicOption(
    val id: String,
    val key: String,
)

object MusicOptions {
    val albumChoices =
        listOf("all", "future", "missing", "latest", "first", "none").map { MusicOption(it, it) }
    val newAlbums =
        listOf(MusicOption("all", "all"), MusicOption("new", "new"), MusicOption("none", "none"))
    val albumTypes = listOf("album", "ep", "single", "broadcast", "other").map { MusicOption(it, it) }
    val secondaryTypes =
        listOf(
            MusicOption("studio", "studio"),
            MusicOption("live", "live"),
            MusicOption("compilation", "compilation"),
            MusicOption("soundtrack", "soundtrack"),
            MusicOption("remix", "remix"),
            MusicOption("spokenword", "spokenword"),
            MusicOption("interview", "interview"),
            MusicOption("audiobook", "audiobook"),
            MusicOption("audio drama", "audio_drama"),
            MusicOption("dj-mix", "dj_mix"),
            MusicOption("mixtape/street", "mixtape"),
            MusicOption("demo", "demo"),
            MusicOption("field recording", "field_recording"),
        )
    val statuses =
        listOf(
            MusicOption("official", "official"),
            MusicOption("promotion", "promotion"),
            MusicOption("bootleg", "bootleg"),
            MusicOption("pseudo-release", "pseudo_release"),
        )
    val pressingRules = listOf("common", "most_tracks", "earliest").map { MusicOption(it, it) }

    val defaultAlbumTypes = listOf("album", "ep")
    val defaultSecondary = listOf("studio")
    val defaultStatuses = listOf("official")

    fun toggle(
        current: List<String>,
        id: String,
        on: Boolean,
        all: List<MusicOption>,
    ): List<String> {
        val set = if (on) current + id else current - id
        return all.map { it.id }.filter { it in set }
    }
}

object MusicRules {
    fun shortCount(n: Long): String {
        if (n < 1_000) return n.toString()
        if (n < 1_000_000) return trim(n / 1_000.0) + "K"
        if (n < 1_000_000_000) return trim(n / 1_000_000.0) + "M"
        return trim(n / 1_000_000_000.0) + "B"
    }

    private fun trim(v: Double): String {
        val one = (v * 10).roundToInt() / 10.0
        return if (v >= 100 || one == one.toLong().toDouble()) one.toLong().toString() else one.toString()
    }

    fun lengthText(ms: Long): String {
        if (ms <= 0) return ""
        val total = (ms + 500) / 1000
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }

    fun minutes(ms: Long): Int = ((ms + 30_000) / 60_000).toInt()

    fun year(date: String): String = date.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty()

    fun dayOf(date: String): String = if (date.length >= 10 && date[4] == '-' && date[7] == '-') date.take(10) else ""

    fun isFuture(
        date: String,
        today: String,
    ): Boolean = dayOf(date).let { it.isNotEmpty() && it > today }

    fun releaseLine(
        date: String,
        today: String,
    ): MusicLine {
        val day = dayOf(date)
        if (day.isEmpty()) return MusicLine(MusicLineKind.NONE)
        if (day > today) return MusicLine(MusicLineKind.OUT_ON, date = day)
        return if (daysBetween(day, today) in 0..14) MusicLine(MusicLineKind.OUT_NOW) else MusicLine(MusicLineKind.RELEASED_ON, date = day)
    }

    fun daysBetween(
        from: String,
        to: String,
    ): Int {
        val a = epoch(from) ?: return Int.MAX_VALUE
        val b = epoch(to) ?: return Int.MAX_VALUE
        return b - a
    }

    private fun epoch(text: String): Int? {
        val parts = text.take(10).split('-')
        if (parts.size != 3) return null
        val y0 = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        val y = if (m <= 2) y0 - 1 else y0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun albumKind(
        type: String,
        secondary: List<String>,
    ): MusicAlbumKind {
        val sec = secondary.map { it.lowercase() }.filter { it != "studio" }
        return when {
            "live" in sec -> MusicAlbumKind.LIVE
            "compilation" in sec -> MusicAlbumKind.COMPILATION
            "soundtrack" in sec -> MusicAlbumKind.SOUNDTRACK
            "remix" in sec || "dj-mix" in sec || "mixtape/street" in sec -> MusicAlbumKind.REMIX
            sec.isNotEmpty() -> MusicAlbumKind.OTHER
            else ->
                when (type.lowercase()) {
                    "album" -> MusicAlbumKind.ALBUM
                    "ep" -> MusicAlbumKind.EP
                    "single" -> MusicAlbumKind.SINGLE
                    "broadcast" -> MusicAlbumKind.BROADCAST
                    else -> MusicAlbumKind.OTHER
                }
        }
    }

    fun shelfKind(shelf: String): MusicShelfKind =
        when (shelf) {
            "albums" -> MusicShelfKind.ALBUMS
            "singles" -> MusicShelfKind.SINGLES
            "compilations" -> MusicShelfKind.COMPILATIONS
            "live" -> MusicShelfKind.LIVE
            else -> MusicShelfKind.OTHER
        }

    fun shape(wire: String): MusicShape =
        when (wire) {
            "round" -> MusicShape.ROUND
            "tile" -> MusicShape.TILE
            "song" -> MusicShape.SONG
            "grid" -> MusicShape.GRID
            "wide" -> MusicShape.WIDE
            else -> MusicShape.SQUARE
        }

    fun cardKind(wire: String): MusicCardKind =
        when (wire) {
            "artist" -> MusicCardKind.ARTIST
            "song" -> MusicCardKind.SONG
            "genre" -> MusicCardKind.GENRE
            else -> MusicCardKind.ALBUM
        }

    fun pill(
        state: String,
        asked: Boolean,
    ): MusicMark =
        when (state) {
            "complete" -> MusicMark.COMPLETE
            "partial" -> MusicMark.PARTIAL
            "downloading" -> MusicMark.DOWNLOADING
            "missing" -> MusicMark.MISSING
            else -> if (asked) MusicMark.REQUESTED else MusicMark.NONE
        }

    fun cardMark(
        state: String,
        rowType: String,
    ): MusicMark =
        if (rowType == "your_asks") {
            MusicMark.NONE
        } else {
            when (state) {
                "have" -> MusicMark.COMPLETE
                "asked" -> MusicMark.REQUESTED
                "approved" -> MusicMark.APPROVED
                else -> MusicMark.NONE
            }
        }

    fun unmatchedReason(reason: String): String =
        when (reason) {
            "music_no_album",
            "music_two_album",
            "music_album_off",
            "music_no_track",
            "music_unsure",
            "music_has_file",
            "music_duplicate",
            "music_other_artist",
            "music_not_listed",
            "unmatched",
            "replaced_by_hand",
            -> reason
            else -> "music_no_album"
        }

    fun cardLine(
        rowType: String,
        card: ch.orellbuehler.acolyte.api.model.MusicCard,
        today: String,
    ): MusicLine {
        if (card.note.isNotEmpty()) return MusicLine(MusicLineKind.TEXT, text = card.note, artist = card.artist)
        val artist = card.artist
        return when (rowType) {
            "fresh", "new_from", "coming_soon" -> releaseLine(card.date, today).copy(artist = artist)
            "rising" -> MusicLine(MusicLineKind.LISTENS, number = card.listenCount, artist = artist)
            "classic", "decade", "missing" ->
                year(card.date).let { y ->
                    if (y.isEmpty()) {
                        MusicLine(
                            MusicLineKind.ARTIST_TEXT,
                            artist = artist,
                        )
                    } else {
                        MusicLine(MusicLineKind.ARTIST_YEAR, artist = artist, text = y)
                    }
                }
            "just_arrived" -> MusicLine(MusicLineKind.ARRIVED_ON, artist = artist, date = dayOf(card.date))
            "your_asks" ->
                when (card.state) {
                    "asked" -> MusicLine(MusicLineKind.ASKED)
                    "approved" -> MusicLine(MusicLineKind.APPROVED)
                    "turned_down" -> MusicLine(MusicLineKind.TURNED_DOWN)
                    "available" -> MusicLine(MusicLineKind.ARRIVED)
                    else -> MusicLine(MusicLineKind.ARTIST_TEXT, artist = artist)
                }
            "top_albums", "trending_artists", "popular_genre", "popular_country" ->
                if (card.listenCount >
                    0
                ) {
                    MusicLine(MusicLineKind.LISTENS, number = card.listenCount, artist = artist)
                } else {
                    MusicLine(MusicLineKind.ARTIST_TEXT, artist = artist)
                }
            else -> if (card.kind == "artist") MusicLine(MusicLineKind.NONE) else MusicLine(MusicLineKind.ARTIST_TEXT, artist = artist)
        }
    }

    fun newLine(
        rowId: String,
        card: ch.orellbuehler.acolyte.api.model.MusicCard,
        today: String,
    ): MusicLine {
        if (card.note.isNotEmpty() &&
            rowId == "np_music_climbing"
        ) {
            return MusicLine(MusicLineKind.TEXT, text = card.note, artist = card.artist)
        }
        if (rowId ==
            "np_music_coming"
        ) {
            return releaseLine(card.date, today).let {
                it.copy(
                    kind =
                        if (it.kind ==
                            MusicLineKind.NONE
                        ) {
                            MusicLineKind.ARTIST_TEXT
                        } else {
                            it.kind
                        },
                    artist = card.artist,
                )
            }
        }
        val day = dayOf(card.date)
        if (day.isEmpty()) return MusicLine(MusicLineKind.ARTIST_TEXT, artist = card.artist)
        return MusicLine(MusicLineKind.ARRIVED_ON, artist = card.artist, date = day, number = daysBetween(day, today).toLong())
    }

    fun target(card: ch.orellbuehler.acolyte.api.model.MusicCard): MusicTarget =
        when {
            card.kind == "genre" -> MusicTarget(MusicOpenKind.GENRE, slug = card.mbid)
            card.href.isNotEmpty() -> pageTarget(card.href)
            card.kind == "song" -> MusicTarget(MusicOpenKind.ARTIST, mbid = card.artistMbid)
            card.kind == "artist" -> MusicTarget(MusicOpenKind.ARTIST, mbid = card.mbid.ifEmpty { card.artistMbid })
            card.kind == "album" && card.artistMbid.isNotEmpty() ->
                MusicTarget(
                    MusicOpenKind.ALBUM,
                    mbid = card.mbid,
                    artist = card.artistMbid,
                )
            card.titleId > 0 && card.partId > 0 -> MusicTarget(MusicOpenKind.LIBRARY_ALBUM, titleId = card.titleId, partId = card.partId)
            card.titleId > 0 -> MusicTarget(MusicOpenKind.LIBRARY_ARTIST, titleId = card.titleId)
            card.mbid.isNotEmpty() && card.kind == "album" -> MusicTarget(MusicOpenKind.ALBUM, mbid = card.mbid)
            else -> MusicTarget(MusicOpenKind.SEARCH, query = card.artist.ifEmpty { card.name })
        }

    fun pageTarget(href: String): MusicTarget {
        val parts = href.trim('/').split('/')
        if (parts.size != 3 || parts[0] != "music") return MusicTarget(MusicOpenKind.NONE)
        return when (parts[1]) {
            "genre" -> MusicTarget(MusicOpenKind.GENRE, slug = parts[2])
            "artist" -> MusicTarget(MusicOpenKind.ARTIST, mbid = parts[2])
            "album" -> MusicTarget(MusicOpenKind.ALBUM, mbid = parts[2])
            else -> MusicTarget(MusicOpenKind.PAGE, page = parts[1], slug = parts[2])
        }
    }

    fun tileTarget(tile: ch.orellbuehler.acolyte.api.model.MusicBrowseTile): MusicTarget =
        tile.href.takeIf { it.isNotEmpty() }?.let { pageTarget(it) } ?: tile.key.substringAfter(':', "").let {
            val kind = tile.key.substringBefore(':')
            if (kind == "genre") MusicTarget(MusicOpenKind.GENRE, slug = it) else MusicTarget(MusicOpenKind.PAGE, page = kind, slug = it)
        }

    fun bandHue(hue: Int): Int = ((hue % 24) + 24) % 24 * 15

    fun tileKind(key: String): String = key.substringBefore(':', "genre")

    private val palette =
        listOf(
            "#d62839",
            "#c4520a",
            "#9c6c00",
            "#23802f",
            "#0b7a55",
            "#0a7b8a",
            "#3b6ea5",
            "#2a5fe0",
            "#2b3b99",
            "#4a3bcb",
            "#a02fc0",
            "#7b5ed0",
            "#c4208c",
            "#d3336e",
            "#a8452b",
            "#4e6075",
        )

    private val fixed =
        mapOf(
            "pop" to 13,
            "hip-hop" to 1,
            "rock" to 0,
            "latin" to 2,
            "rnb" to 10,
            "country" to 14,
            "dance" to 7,
            "indie" to 4,
            "alternative" to 5,
            "metal" to 15,
            "kpop" to 12,
            "afro" to 3,
            "jazz" to 8,
            "classical" to 6,
            "soul-funk" to 11,
            "folk" to 9,
            "reggae" to 2,
            "blues" to 10,
            "punk" to 0,
            "christian" to 13,
            "soundtracks" to 1,
            "kids" to 14,
            "chill" to 5,
            "party" to 9,
            "love" to 12,
            "sad" to 8,
            "happy" to 3,
            "focus" to 6,
            "workout" to 1,
            "sleep" to 15,
            "romance" to 13,
            "energy" to 0,
            "throwback" to 2,
            "feel-good" to 4,
            "halloween" to 11,
            "christmas" to 3,
            "new-year" to 6,
            "valentines" to 13,
            "summer" to 2,
            "dia-de-muertos" to 12,
            "lunar-new-year" to 0,
            "pride" to 9,
            "thanksgiving" to 14,
            "back-to-school" to 7,
            "fall" to 1,
            "spring" to 4,
            "1960s" to 14,
            "1970s" to 2,
            "1980s" to 12,
            "1990s" to 3,
            "2000s" to 6,
            "2010s" to 5,
            "2020s" to 7,
            "driving" to 8,
            "cooking" to 0,
            "gaming" to 10,
            "studying" to 6,
            "gym" to 1,
            "running" to 3,
            "home" to 14,
        )

    fun tileColor(key: String): String {
        val slug = key.substringAfter(':', key).lowercase()
        val index = fixed[slug] ?: fixed[decade(slug)] ?: (fnv(slug) % palette.size)
        return palette[index]
    }

    private fun decade(slug: String): String =
        when (slug) {
            "60s" -> "1960s"
            "70s" -> "1970s"
            "80s" -> "1980s"
            "90s" -> "1990s"
            "00s" -> "2000s"
            "10s" -> "2010s"
            "20s" -> "2020s"
            else -> slug
        }

    private fun fnv(text: String): Int {
        var hash = 0x811c9dc5L
        for (c in text) {
            hash = hash xor c.code.toLong()
            hash = (hash * 0x01000193L) and 0xffffffffL
        }
        return (hash % 1_000_000L).toInt()
    }

    fun tint(hex: String): Long? {
        val rgb = parseHex(hex) ?: return null
        val (h, s, l) = toHsl(rgb)
        var sat: Double
        var light: Double
        if (s < 0.1 || l < 0.1) {
            sat = if (s < 0.1) 0.0 else s
            light = 0.12
        } else {
            sat = (s * 0.7).coerceIn(0.22, 0.55)
            light = (l * 0.6).coerceIn(0.14, 0.30)
        }
        var guard = 0
        while (guard < 40 && light > 0.06 && contrast(fromHsl(h, sat, light)) < 7.0) {
            light -= 0.01
            guard++
        }
        val (r, g, b) = fromHsl(h, sat, light)
        return (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    private fun parseHex(hex: String): Triple<Int, Int, Int>? {
        val t = hex.trim().removePrefix("#")
        if (t.length != 6) return null
        val v = t.toLongOrNull(16) ?: return null
        return Triple(((v shr 16) and 0xff).toInt(), ((v shr 8) and 0xff).toInt(), (v and 0xff).toInt())
    }

    private fun toHsl(c: Triple<Int, Int, Int>): Triple<Double, Double, Double> {
        val r = c.first / 255.0
        val g = c.second / 255.0
        val b = c.third / 255.0
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2
        if (mx == mn) return Triple(0.0, 0.0, l)
        val d = mx - mn
        val s = if (l > 0.5) d / (2 - mx - mn) else d / (mx + mn)
        val h =
            when (mx) {
                r -> ((g - b) / d + (if (g < b) 6 else 0)) / 6
                g -> ((b - r) / d + 2) / 6
                else -> ((r - g) / d + 4) / 6
            }
        return Triple(h, s, l)
    }

    private fun fromHsl(
        h: Double,
        s: Double,
        l: Double,
    ): Triple<Int, Int, Int> {
        if (s == 0.0) {
            val v = (l * 255).roundToInt()
            return Triple(v, v, v)
        }
        val q = if (l < 0.5) l * (1 + s) else l + s - l * s
        val p = 2 * l - q

        fun hue(t0: Double): Double {
            var t = t0
            if (t < 0) t += 1
            if (t > 1) t -= 1
            return when {
                t < 1.0 / 6 -> p + (q - p) * 6 * t
                t < 0.5 -> q
                t < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - t) * 6
                else -> p
            }
        }
        return Triple((hue(h + 1.0 / 3) * 255).roundToInt(), (hue(h) * 255).roundToInt(), (hue(h - 1.0 / 3) * 255).roundToInt())
    }

    private fun contrast(c: Triple<Int, Int, Int>): Double {
        fun lin(v: Int): Double {
            val x = v / 255.0
            return if (x <= 0.03928) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        val lum = 0.2126 * lin(c.first) + 0.7152 * lin(c.second) + 0.0722 * lin(c.third)
        return 1.05 / (lum + 0.05)
    }

    fun image(
        path: String,
        size: Int = 500,
    ): String {
        if (path.isEmpty() || !path.startsWith("/")) return path
        val sizable = path.contains("/music-trending/art/") || path.endsWith("/cover")
        if (!sizable || "size=" in path) return path
        return path + (if ('?' in path) "&" else "?") + "size=$size"
    }

    fun sizeIsGb(bytes: Long): Boolean = bytes >= GIB

    fun sizeValue(bytes: Long): String =
        if (bytes >= GIB) {
            val tenths = (bytes * 10 / GIB).toInt()
            "${tenths / 10}.${tenths % 10}"
        } else {
            (bytes / MIB).toString()
        }

    private const val GIB = 1024L * 1024 * 1024
    private const val MIB = 1024L * 1024
}
