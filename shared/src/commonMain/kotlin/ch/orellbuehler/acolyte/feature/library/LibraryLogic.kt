package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.model.GridSlot
import ch.orellbuehler.acolyte.api.model.GridTitle
import ch.orellbuehler.acolyte.api.model.MyWatched
import ch.orellbuehler.acolyte.api.model.ShowIssue
import ch.orellbuehler.acolyte.feature.Tone
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

enum class LibraryKind(
    val wire: String,
) {
    MOVIE("movie"),
    SHOW("show"),
    ARTIST("artist"),
    ;

    internal companion object {
        fun of(wire: String): LibraryKind? = entries.firstOrNull { it.wire == wire }
    }
}

enum class LibStateKey(
    val tone: Tone,
) {
    FAILED(Tone.FAIL),
    NEEDS_DECISION(Tone.FAIL),
    STALLED(Tone.FAIL),
    MISSING_FILE(Tone.FAIL),
    MISSING(Tone.WAIT),
    BETTER_WANTED(Tone.WAIT),
    PAUSED(Tone.IDLE),
    DOWNLOADING(Tone.INFO),
    QUEUED(Tone.INFO),
    APPROVED(Tone.OK),
    WAITING_APPROVAL(Tone.IDLE),
    NOT_OUT_YET(Tone.IDLE),
    NOT_KEPT(Tone.IDLE),
    SPECIALS_ONLY(Tone.OK),
    DECLINED(Tone.IDLE),
    IN_LIBRARY(Tone.OK),
    UP_TO_DATE(Tone.OK),
    ENDED(Tone.OK),
}

data class LibState(
    val key: LibStateKey,
    val count: Int = 0,
    val percent: Int = -1,
    val quality: String = "",
    val date: String = "",
    val reasons: List<String> = emptyList(),
    val noReason: Boolean = false,
    val fileNotFound: Boolean = false,
    val importing: Boolean = false,
) {
    val tone: Tone get() = key.tone
    val rank: Int get() = key.ordinal
    val isProblem: Boolean get() = rank <= LibStateKey.BETTER_WANTED.ordinal
}

enum class StatusFilter(
    val wire: String,
) {
    ALL("all"),
    MISSING("missing"),
    NOT_OUT_YET("not_out_yet"),
    BETTER("better"),
    HAVE("have"),
}

enum class KeptFilter {
    ANY,
    ON,
    OFF,
}

@Serializable
data class LibraryFilter(
    val status: StatusFilter = StatusFilter.ALL,
    val kept: KeptFilter = KeptFilter.ANY,
    val asker: String = ASKER_ANYONE,
    val series: Long = 0,
    val codec: String = "",
    val subs: Boolean = false,
    val watched: Boolean = false,
) {
    val activeCount: Int
        get() =
            listOf(
                status != StatusFilter.ALL,
                kept != KeptFilter.ANY,
                asker != ASKER_ANYONE,
                series != 0L,
                codec.isNotEmpty(),
                subs,
                watched,
            ).count { it }

    val isActive: Boolean get() = activeCount > 0

    companion object {
        const val ASKER_ANYONE = "anyone"
        const val ASKER_ME = "me"
    }
}

enum class LibrarySort(
    internal val ascending: Boolean,
) {
    STATUS(true),
    TITLE(true),
    YEAR(false),
    SCORE(false),
    OUT(false),
    ADDED(false),
    SIZE(false),
    RUNTIME(false),
    RULES(true),
    NEXT(true),
    LAST(false),
    PROGRESS(true),
    NETWORK(true),
}

enum class FactKind {
    RELEASED,
    HOME_RELEASE,
    IN_CINEMAS,
    CINEMAS,
    RELEASE_DATE,
    NOT_KNOWN,
    EPISODES,
    TRACKS,
    NEXT_EPISODE,
    NEXT_RELEASE,
    ENDED,
    LAST_AIRED,
    ADDED,
    SIZE,
    RUNTIME,
    SCORE,
    YEAR,
    NETWORK,
    QUALITY,
}

data class LibraryFact(
    val kind: FactKind,
    val date: String = "",
    val number: Long = 0,
    val total: Long = 0,
    val score: Double = 0.0,
    val text: String = "",
)

internal data class Progress(
    val have: Int,
    val total: Int,
) {
    val share: Float get() = if (total <= 0) 0f else (have.toFloat() / total).coerceIn(0f, 1f)
}

internal object LibraryLogic {
    private val busyStates =
        setOf("searching", "held", "chosen", "downloading", "stalled", "failed", "importing", "needs_a_decision", "rejected")
    private val haveStates = setOf("in_library", "better_wanted")
    private val downloadingStates = setOf("chosen", "downloading", "stalled", "importing")
    private const val FILE_NOT_FOUND = "HolyArrs can't find the downloaded file where the Downloader says it is;"
    private val articles = Regex("^(the|a|an)\\s+")
    private const val MIN_VOTES = 25

    val kindSorts: Map<LibraryKind, List<LibrarySort>> =
        mapOf(
            LibraryKind.MOVIE to
                listOf(
                    LibrarySort.STATUS,
                    LibrarySort.TITLE,
                    LibrarySort.YEAR,
                    LibrarySort.SCORE,
                    LibrarySort.OUT,
                    LibrarySort.ADDED,
                    LibrarySort.SIZE,
                    LibrarySort.RUNTIME,
                    LibrarySort.RULES,
                ),
            LibraryKind.SHOW to
                listOf(
                    LibrarySort.STATUS,
                    LibrarySort.TITLE,
                    LibrarySort.NEXT,
                    LibrarySort.LAST,
                    LibrarySort.PROGRESS,
                    LibrarySort.ADDED,
                    LibrarySort.YEAR,
                    LibrarySort.SCORE,
                    LibrarySort.SIZE,
                    LibrarySort.NETWORK,
                    LibrarySort.RULES,
                ),
            LibraryKind.ARTIST to
                listOf(LibrarySort.STATUS, LibrarySort.TITLE, LibrarySort.PROGRESS, LibrarySort.ADDED, LibrarySort.SIZE, LibrarySort.NEXT),
        )

    fun defaultSort(kind: LibraryKind): Pair<LibrarySort, Boolean> =
        when (kind) {
            LibraryKind.SHOW -> LibrarySort.NEXT to false
            else -> LibrarySort.ADDED to true
        }

    fun millis(iso: String): Long? =
        if (iso.isBlank()) {
            null
        } else {
            try {
                Instant.parse(iso).toEpochMilliseconds()
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    fun now(): Long = Clock.System.now().toEpochMilliseconds()

    fun quality(t: GridTitle): String {
        val best = t.slots.maxOfOrNull { it.file?.quality?.resolution ?: 0 } ?: 0
        return when {
            best >= 2000 -> "4K"
            best >= 1000 -> "1080p"
            best >= 700 -> "720p"
            best > 0 -> "${best}p"
            else -> ""
        }
    }

    private fun percent(p: Double): Int = (p.coerceIn(0.0, 1.0) * 100).roundToInt()

    private fun askOf(t: GridTitle): String? {
        val states = t.asks.filter { it.titleId == null || it.titleId == t.id }.map { it.state }
        return listOf("approved", "asked", "turned_down").firstOrNull { it in states }
    }

    private fun askState(ask: String?): LibState? =
        when (ask) {
            "approved" -> LibState(LibStateKey.APPROVED)
            "asked" -> LibState(LibStateKey.WAITING_APPROVAL)
            "turned_down" -> LibState(LibStateKey.DECLINED)
            else -> null
        }

    private fun issueState(issues: List<Pair<String, String>>): LibState? {
        val found = issues.filter { it.first == "failed" || it.first == "needs_a_decision" }
        if (found.isEmpty()) return null
        val failed = found.any { it.first == "failed" }
        val reasons = found.map { it.second.trim() }
        return LibState(
            key = if (failed) LibStateKey.FAILED else LibStateKey.NEEDS_DECISION,
            reasons = reasons.filter { it.isNotEmpty() && !it.startsWith(FILE_NOT_FOUND) }.distinct(),
            noReason = reasons.any { it.isEmpty() },
            fileNotFound = reasons.any { it.startsWith(FILE_NOT_FOUND) },
        )
    }

    private fun future(
        iso: String,
        now: Long,
    ): String = if ((millis(iso) ?: 0) > now) iso else ""

    fun state(
        t: GridTitle,
        now: Long = now(),
    ): LibState? = if (t.kind == "show" || t.kind == "artist") groupState(t, now) else movieState(t, now)

    private fun movieState(
        t: GridTitle,
        now: Long,
    ): LibState {
        val slots = t.slots

        fun has(state: String) = slots.any { it.state == state }
        issueState(slots.map { it.state to it.reason })?.let { return it }
        val dl = slots.firstOrNull { it.download?.active == true || it.state in downloadingStates }
        if (dl != null) return downloadState(dl)
        if (slots.any { it.file?.state == "missing" }) return LibState(LibStateKey.MISSING_FILE)
        val q = quality(t)
        if (has("better_wanted")) return LibState(LibStateKey.BETTER_WANTED, quality = q)
        val owned = slots.any { it.state == "in_library" || it.state == "no_hardlink" || it.file != null }
        if (!t.keepUpToDate) return LibState(LibStateKey.NOT_KEPT, quality = if (owned) q else "")
        if (owned) return LibState(LibStateKey.IN_LIBRARY, quality = q)
        askState(askOf(t))?.let { return it }
        if (has("not_out_yet") || has("waiting")) {
            val date = t.digitalDate.ifEmpty { t.physicalDate.ifEmpty { t.cinemaDate } }
            return LibState(LibStateKey.NOT_OUT_YET, date = future(date, now))
        }
        return LibState(LibStateKey.MISSING)
    }

    private fun downloadState(slot: GridSlot): LibState {
        val d = slot.download
        val p = percent(d?.progress ?: 0.0)
        return when {
            slot.state == "stalled" || d?.state == "stalled" -> LibState(LibStateKey.STALLED, percent = p)
            d?.state == "paused" -> LibState(LibStateKey.PAUSED, percent = p)
            d?.state == "queued" || d?.state == "sent" || slot.state == "chosen" -> LibState(LibStateKey.QUEUED)
            slot.state == "importing" -> LibState(LibStateKey.DOWNLOADING, percent = 100, importing = true)
            else -> LibState(LibStateKey.DOWNLOADING, percent = p)
        }
    }

    private data class Counts(
        val inLibrary: Int,
        val missing: Int,
        val notOutYet: Int,
        val betterWanted: Int,
        val downloading: Int,
        val failed: Int,
        val next: String,
    )

    private fun countsOf(t: GridTitle): Counts? =
        when (t.kind) {
            "show" ->
                t.show?.let {
                    Counts(
                        it.inLibrary,
                        it.missing,
                        it.notOutYet,
                        it.betterWanted,
                        it.downloading,
                        it.failed,
                        it.nextAirDate,
                    )
                }
            "artist" ->
                t.artist?.let { Counts(it.inLibrary, it.missing, it.notOutYet, it.betterWanted, it.downloading, it.failed, it.nextRelease) }
            else -> null
        }

    private fun groupState(
        t: GridTitle,
        now: Long,
    ): LibState? {
        val s = countsOf(t) ?: return askState(askOf(t))
        val issues: List<ShowIssue> = t.show?.issues.orEmpty()
        issueState(issues.map { it.state to it.reason })?.let { return it }
        if (s.failed > 0) return LibState(LibStateKey.FAILED, count = s.failed)
        if (s.downloading > 0) return LibState(LibStateKey.DOWNLOADING, count = s.downloading)
        val ask = askOf(t)
        if (s.inLibrary == 0 && s.missing == 0 && s.notOutYet == 0 && s.betterWanted == 0 && ask == null) {
            return if (t.keepUpToDate) null else LibState(LibStateKey.NOT_KEPT)
        }
        if (s.inLibrary == 0 && s.missing == 0) askState(ask)?.let { return it }
        if (!t.keepUpToDate && s.missing == 0) return LibState(LibStateKey.NOT_KEPT)
        val show = t.show
        if (t.kind == "show" && show != null) {
            val kept = show.seasons.filter { it.keepUpToDate && !it.dropped }
            if (kept.isNotEmpty() && kept.all { it.number == 0 }) return LibState(LibStateKey.SPECIALS_ONLY, count = show.inLibrary)
        }
        if (s.missing > 0) return LibState(LibStateKey.MISSING, count = s.missing)
        if (s.betterWanted > 0) return LibState(LibStateKey.BETTER_WANTED, count = s.betterWanted)
        if (s.inLibrary == 0) {
            if (s.notOutYet > 0 || s.next.isNotEmpty()) return LibState(LibStateKey.NOT_OUT_YET, date = future(s.next, now))
            askState(ask)?.let { return it }
            return LibState(LibStateKey.MISSING)
        }
        if (t.kind == "show" && show?.status == "ended") return LibState(LibStateKey.ENDED)
        return LibState(LibStateKey.UP_TO_DATE)
    }

    fun airedEpisodes(t: GridTitle): Int {
        val show = t.show ?: return 0
        if (show.seasons.isEmpty()) return maxOf(0, show.episodes - show.notOutYet)
        return show.seasons.sumOf { if (it.number > 0 || it.keepUpToDate) it.aired else 0 }
    }

    fun progress(t: GridTitle): Progress {
        val show = t.show
        if (t.kind == "show" && show != null) return Progress(show.inLibrary, airedEpisodes(t))
        val artist = t.artist
        if (t.kind == "artist" && artist != null) return Progress(artist.inLibrary, maxOf(0, artist.tracks - artist.notOutYet))
        return Progress(if (t.slots.any { it.state in haveStates }) 1 else 0, 1)
    }

    fun matches(
        t: GridTitle,
        filter: StatusFilter,
    ): Boolean {
        if (filter == StatusFilter.ALL) return true
        if (t.kind == "show" || t.kind == "artist") {
            val s = countsOf(t) ?: return false
            return when (filter) {
                StatusFilter.HAVE -> s.inLibrary > 0
                StatusFilter.BETTER -> s.betterWanted > 0
                StatusFilter.NOT_OUT_YET -> s.notOutYet > 0
                StatusFilter.MISSING -> s.missing > 0
                StatusFilter.ALL -> true
            }
        }
        val states = t.slots.map { it.state }
        return when (filter) {
            StatusFilter.HAVE -> states.any { it in haveStates }
            StatusFilter.BETTER -> "better_wanted" in states
            StatusFilter.NOT_OUT_YET -> "not_out_yet" in states
            StatusFilter.MISSING -> states.any { it == "wanted" || it in busyStates }
            StatusFilter.ALL -> true
        }
    }

    fun missing(t: GridTitle): Boolean = matches(t, StatusFilter.MISSING)

    fun query(text: String): String =
        text
            .take(256)
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(16)
            .joinToString(" ")

    fun byName(
        t: GridTitle,
        query: String,
    ): Boolean {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val name = t.name.lowercase()
        return words.all { name.contains(it) || (t.year > 0 && t.year.toString() == it) }
    }

    fun sortName(name: String): String = name.lowercase().replace(articles, "")

    fun letter(name: String): String {
        val first = sortName(name).firstOrNull()?.let { fold(it) } ?: return "#"
        return if (first in 'a'..'z') first.uppercaseChar().toString() else "#"
    }

    private fun fold(c: Char): Char =
        when (c) {
            in "àáâãäåā" -> 'a'
            in "çćč" -> 'c'
            in "èéêëēě" -> 'e'
            in "ìíîïī" -> 'i'
            in "ñń" -> 'n'
            in "òóôõöøō" -> 'o'
            in "ùúûüū" -> 'u'
            in "ýÿ" -> 'y'
            in "šś" -> 's'
            in "žźż" -> 'z'
            else -> c
        }

    fun score(t: GridTitle): Double? = if (t.votes >= MIN_VOTES && t.score > 0) t.score else null

    fun releaseDate(t: GridTitle): String = t.digitalDate.ifEmpty { t.physicalDate.ifEmpty { t.cinemaDate } }

    private sealed interface SortValue : Comparable<SortValue> {
        data class Number(
            val value: Double,
        ) : SortValue {
            override fun compareTo(other: SortValue): Int = value.compareTo((other as Number).value)
        }

        data class Text(
            val value: String,
        ) : SortValue {
            override fun compareTo(other: SortValue): Int = value.compareTo((other as Text).value)
        }
    }

    private fun sortValue(
        t: GridTitle,
        key: LibrarySort,
        state: LibState?,
    ): SortValue? {
        fun number(v: Number?) = v?.let { SortValue.Number(it.toDouble()) }

        fun date(iso: String) = number(millis(iso))

        fun text(v: String) = v.takeIf { it.isNotEmpty() }?.let { SortValue.Text(it.lowercase()) }
        return when (key) {
            LibrarySort.STATUS -> number(state?.rank)
            LibrarySort.TITLE -> SortValue.Text(sortName(t.name))
            LibrarySort.YEAR -> number(t.year.takeIf { it > 0 })
            LibrarySort.ADDED -> date(t.addedAt)
            LibrarySort.SIZE -> number(t.sizeBytes)
            LibrarySort.NEXT ->
                when (t.kind) {
                    "show" -> date(t.show?.nextAirDate.orEmpty())
                    "artist" -> date(t.artist?.nextRelease.orEmpty())
                    else -> null
                }
            LibrarySort.LAST -> date(t.show?.lastAirDate.orEmpty())
            LibrarySort.OUT -> date(releaseDate(t))
            LibrarySort.RUNTIME -> number(t.runtimeMinutes.takeIf { it > 0 })
            LibrarySort.NETWORK -> text(t.show?.network.orEmpty())
            LibrarySort.RULES -> text(t.pickRulesName)
            LibrarySort.SCORE -> number(score(t))
            LibrarySort.PROGRESS -> progress(t).takeIf { it.total > 0 }?.let { number(it.have.toDouble() / it.total) }
        }
    }

    fun sort(
        titles: List<GridTitle>,
        key: LibrarySort,
        descending: Boolean,
        states: Map<Long, LibState?>,
    ): List<GridTitle> {
        val sign = if (descending) -1 else 1
        return titles
            .map { Triple(it, sortValue(it, key, states[it.id]), sortName(it.name)) }
            .sortedWith { a, b ->
                val av = a.second
                val bv = b.second
                when {
                    av == null && bv != null -> 1
                    bv == null && av != null -> -1
                    av != null && bv != null && av.compareTo(bv) != 0 -> av.compareTo(bv) * sign
                    a.third != b.third -> a.third.compareTo(b.third)
                    else -> a.first.id.compareTo(b.first.id)
                }
            }.map { it.first }
    }

    fun fact(
        t: GridTitle,
        sort: LibrarySort,
        now: Long = now(),
    ): LibraryFact? =
        when (sort) {
            LibrarySort.SCORE -> score(t)?.let { LibraryFact(FactKind.SCORE, score = it) }
            LibrarySort.OUT -> releaseFact(t, now)
            LibrarySort.ADDED -> t.addedAt.takeIf { it.isNotEmpty() }?.let { LibraryFact(FactKind.ADDED, date = it) }
            LibrarySort.SIZE -> t.sizeBytes.takeIf { it > 0 }?.let { LibraryFact(FactKind.SIZE, number = it) }
            LibrarySort.RUNTIME -> t.runtimeMinutes.takeIf { it > 0 }?.let { LibraryFact(FactKind.RUNTIME, number = it.toLong()) }
            LibrarySort.RULES -> t.pickRulesName.takeIf { it.isNotEmpty() }?.let { LibraryFact(FactKind.QUALITY, text = it) }
            LibrarySort.NEXT -> nextFact(t)
            LibrarySort.LAST ->
                t.show
                    ?.lastAirDate
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { LibraryFact(FactKind.LAST_AIRED, date = it) }
            LibrarySort.NETWORK ->
                t.show
                    ?.network
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { LibraryFact(FactKind.NETWORK, text = it) }
            LibrarySort.PROGRESS -> progressFact(t)
            else -> defaultFact(t, now)
        }

    private fun defaultFact(
        t: GridTitle,
        now: Long,
    ): LibraryFact? =
        when (t.kind) {
            "movie" -> releaseFact(t, now)
            else -> progressFact(t)
        }

    private fun progressFact(t: GridTitle): LibraryFact? {
        val p = progress(t)
        if (p.total <= 0) return null
        return LibraryFact(
            if (t.kind ==
                "artist"
            ) {
                FactKind.TRACKS
            } else {
                FactKind.EPISODES
            },
            number = p.have.toLong(),
            total = p.total.toLong(),
        )
    }

    private fun nextFact(t: GridTitle): LibraryFact? {
        if (t.kind ==
            "artist"
        ) {
            return t.artist
                ?.nextRelease
                ?.takeIf { it.isNotEmpty() }
                ?.let { LibraryFact(FactKind.NEXT_RELEASE, date = it) }
        }
        val show = t.show ?: return null
        if (show.nextAirDate.isNotEmpty()) return LibraryFact(FactKind.NEXT_EPISODE, date = show.nextAirDate)
        return if (show.status == "ended") LibraryFact(FactKind.ENDED) else null
    }

    private fun releaseFact(
        t: GridTitle,
        now: Long,
    ): LibraryFact {
        if (t.digitalDate.isNotEmpty()) {
            val past = (millis(t.digitalDate) ?: 0) <= now
            return LibraryFact(if (past) FactKind.RELEASED else FactKind.HOME_RELEASE, date = t.digitalDate)
        }
        if (t.cinemaDate.isNotEmpty()) {
            val past = (millis(t.cinemaDate) ?: 0) <= now
            return LibraryFact(if (past) FactKind.IN_CINEMAS else FactKind.CINEMAS, date = t.cinemaDate)
        }
        val date = releaseDate(t)
        return if (date.isNotEmpty()) LibraryFact(FactKind.RELEASE_DATE, date = date) else LibraryFact(FactKind.NOT_KNOWN)
    }

    fun tmdbOf(t: GridTitle): String {
        if (t.kind != "movie" && t.kind != "show") return ""
        val tmdb = t.ids["tmdb"].orEmpty()
        if (tmdb.isEmpty() || !tmdb.all { it.isDigit() }) return ""
        if (tmdb == t.ids["tvdb"] || tmdb == t.ids["tvmaze"]) return ""
        return tmdb
    }

    fun watched(
        t: GridTitle,
        watched: MyWatched?,
    ): Pair<Int, Int> {
        if (watched == null || !watched.enabled || !watched.linked) return 0 to 0
        if (t.kind == "movie") return if (t.id in watched.movies) 1 to 1 else 0 to 0
        val show = t.show ?: return 0 to 0
        val seen = watched.shows[t.id.toString()] ?: return 0 to 0
        var count = 0
        var of = 0
        for (season in show.seasons.filter { it.number > 0 }) {
            val cap = season.aired
            of += cap
            count += minOf(seen[season.number.toString()] ?: 0, cap)
        }
        return count to of
    }

    fun personName(name: String): Pair<String, Boolean> {
        val mark = name.indexOf('\u2063')
        return if (mark >= 0) name.substring(0, mark) to true else name to false
    }

    fun audioCodes(text: String): List<String> =
        text
            .split(Regex("[\\s,;]+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    fun cleanTags(tags: List<String>): List<String> =
        tags
            .map { tag ->
                tag
                    .lowercase()
                    .trim()
                    .replace(Regex("\\s+"), "-")
                    .trim('-', ',')
                    .take(40)
            }.filter { it.isNotEmpty() }
            .distinct()
            .take(20)

    fun fileName(path: String): String {
        val i = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return if (i < 0) path else path.substring(i + 1)
    }

    fun shortPath(path: String): String {
        val parts = path.trimEnd('/').split('/').filter { it.isNotEmpty() }
        return if (parts.size <= 2) path else "…/" + parts.takeLast(2).joinToString("/")
    }
}
