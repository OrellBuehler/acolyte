package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.model.CalendarRow
import ch.orellbuehler.acolyte.api.model.TitleBrief
import ch.orellbuehler.acolyte.feature.Tone

enum class CalendarType {
    ALL,
    MOVIES,
    SHOWS,
    MUSIC,
}

enum class CalendarFollows {
    MINE,
    EVERYONE,
    LIBRARY,
}

enum class CalendarView {
    AGENDA,
    MONTH,
}

enum class EntryDot(
    val tone: Tone,
) {
    HAVE(Tone.OK),
    GETTING(Tone.INFO),
    PROBLEM(Tone.FAIL),
    MISSING(Tone.WAIT),
    UPCOMING(Tone.IDLE),
}

enum class EntryTag {
    SHOW_PREMIERE,
    SEASON_PREMIERE,
    NEW_EPISODE,
    NEW_ALBUM,
    NEW_SINGLE,
    NEW_EP,
    IN_CINEMAS,
    NEW_MOVIE,
    EPISODES,
    ALBUMS,
    MOVIE_DATES,
}

enum class EntryWhen {
    TIME,
    ALL_DAY,
    ALBUM,
    SINGLE,
    EP,
    IN_THEATERS,
    STREAMING,
}

enum class EpisodeSpan {
    ONE,
    SAME_SEASON,
    ACROSS,
}

enum class MovieDateKind {
    CINEMA,
    DIGITAL,
    DISC,
}

data class CalendarGroup(
    val key: String,
    val day: Day,
    val lead: CalendarRow,
    val entries: List<CalendarRow>,
    val kind: String,
    val title: String,
    val tag: EntryTag,
    val count: Int,
    val movieKinds: List<MovieDateKind>,
    val whenKind: EntryWhen,
    val minutes: Int,
    val span: EpisodeSpan,
    val season: Int,
    val episode: Int,
    val season2: Int,
    val episode2: Int,
    val episodeName: String,
    val network: String,
    val caption: String,
    val album: String,
    val dot: EntryDot,
    val art: String?,
    val poster: String?,
    val following: Boolean,
    val followers: List<String>,
    val canSearch: Boolean,
)

data class MonthCell(
    val day: Day,
    val inMonth: Boolean,
    val today: Boolean,
    val count: Int,
    val tone: Tone?,
)

object CalendarRules {
    private val GETTING = setOf("searching", "chosen", "downloading", "importing", "held")
    private val PROBLEM = setOf("failed", "stalled", "needs_a_decision", "rejected")
    private val HOME = setOf("digital", "physical")
    private val STREAMING =
        setOf(
            "netflix",
            "disney+",
            "apple tv+",
            "apple tv",
            "prime video",
            "amazon",
            "amazon prime video",
            "max",
            "hbo max",
            "hulu",
            "paramount+",
            "paramount+ with showtime",
            "peacock",
            "youtube",
            "youtube premium",
            "crunchyroll",
            "starz",
            "mgm+",
            "amc+",
            "bet+",
            "britbox",
            "shudder",
            "tubi",
            "roku channel",
            "the roku channel",
        )

    fun entryDay(
        e: CalendarRow,
        offset: (Long) -> Int = ::localOffsetSeconds,
    ): Day? {
        if (e.kind == "episode") {
            if (e.timed) return Days.localDayOf(e.date, offset)
            return Day.parse(e.airDay) ?: Day.parse(e.date)
        }
        return Day.parse(e.date)
    }

    fun shows(
        e: CalendarRow,
        type: CalendarType,
    ): Boolean =
        when (type) {
            CalendarType.ALL -> true
            CalendarType.SHOWS -> e.kind == "episode"
            CalendarType.MUSIC -> e.kind == "album"
            CalendarType.MOVIES -> e.kind != "episode" && e.kind != "album"
        }

    fun followsOk(
        e: CalendarRow,
        follows: CalendarFollows,
    ): Boolean =
        when (follows) {
            CalendarFollows.LIBRARY -> true
            CalendarFollows.MINE -> e.following
            CalendarFollows.EVERYONE -> e.following || e.followers.isNotEmpty()
        }

    fun startFollows(
        myFollows: Int,
        people: Int,
        remembered: CalendarFollows?,
    ): CalendarFollows {
        var f = remembered ?: if (myFollows > 0) CalendarFollows.MINE else CalendarFollows.LIBRARY
        if (f == CalendarFollows.EVERYONE && people < 2) f = if (myFollows > 0) CalendarFollows.MINE else CalendarFollows.LIBRARY
        return f
    }

    fun entryKey(e: CalendarRow): String =
        when (e.kind) {
            "album" -> "a${e.partId}"
            "episode" -> "e${e.itemId}"
            else -> "${e.titleId}${e.kind}"
        }

    fun dot(
        e: CalendarRow,
        today: Day,
        offset: (Long) -> Int = ::localOffsetSeconds,
    ): EntryDot {
        fun inLib(s: String) = s == "in_library" || s == "better_wanted"
        val have = if (e.kind == "album") e.tracks > 0 && e.have >= e.tracks else inLib(e.state) || e.versions.any { inLib(it.state) }
        if (have) return EntryDot.HAVE
        val states = (listOf(e.state) + e.versions.map { it.state }).filter { it.isNotEmpty() }
        if (states.any { it in GETTING }) return EntryDot.GETTING
        if (states.any { it in PROBLEM }) return EntryDot.PROBLEM
        val day = entryDay(e, offset) ?: return EntryDot.UPCOMING
        return if (day < today) EntryDot.MISSING else EntryDot.UPCOMING
    }

    fun oneMovieDate(
        entries: List<CalendarRow>,
        titles: Map<Long, TitleBrief>,
    ): List<CalendarRow> {
        val home = mutableMapOf<Long, String>()
        for (e in entries) {
            if (e.kind !in HOME) continue
            val d = e.date.take(10)
            val was = home[e.titleId]
            if (was == null || d < was) home[e.titleId] = d
        }
        val kept = mutableSetOf<Long>()
        val out = mutableListOf<CalendarRow>()
        for (e in entries) {
            val movie = e.kind == "cinema" || e.kind in HOME
            if (!movie) {
                out += e
                continue
            }
            if (e.titleId in kept) continue
            val brief = titles[e.titleId]
            val homeDay =
                if (brief != null && brief.kind == "movie") {
                    listOf(brief.digitalDate, brief.physicalDate).filter { it.isNotEmpty() }.map { it.take(10) }.minOrNull()
                } else {
                    home[e.titleId]
                }
            val keep = if (homeDay != null) e.kind in HOME && e.date.take(10) == homeDay else e.kind == "cinema"
            if (!keep) continue
            kept += e.titleId
            val have = brief?.slots?.any { it.state == "in_library" || it.state == "better_wanted" } == true
            out += if (have) e.copy(state = "in_library") else e
        }
        return out
    }

    fun fakeEpisodeName(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty()) return true
        val simple =
            listOf(
                Regex("^(episode|ep\\.?|chapter|part)\\s*#?\\d+(\\s*/\\s*\\d+)?$", RegexOption.IGNORE_CASE),
                Regex("^\\d+\\s*/\\s*\\d+$"),
                Regex("^(tba|tbd|untitled)$", RegexOption.IGNORE_CASE),
                Regex("^#?\\d+$"),
                Regex("^\\d{1,4}[/.-]\\d{1,2}[/.-]\\d{1,4}$"),
            )
        if (simple.any { it.matches(n) }) return true
        val months = "jan(uary)?|feb(ruary)?|mar(ch)?|apr(il)?|may|june?|july?|aug(ust)?|sep(t(ember)?)?|oct(ober)?|nov(ember)?|dec(ember)?"
        val day = "(mon|tue|wed|thu|fri|sat|sun)[a-z]*,?\\s+"
        val date =
            Regex(
                "^($day)?(\\d{1,2}(st|nd|rd|th)?\\s+($months)\\.?,?(\\s+\\d{4})?|($months)\\.?\\s+\\d{1,2}(st|nd|rd|th)?,?(\\s+\\d{4})?)$",
                RegexOption.IGNORE_CASE,
            )
        return date.matches(n)
    }

    fun episodeName(name: String): String {
        val n = name.trim()
        val m = Regex("^(.+?)\\s+[-–—:|]\\s+(.+)$").find(n)
        if (m != null && fakeEpisodeName(m.groupValues[1])) return m.groupValues[2].trim()
        return if (fakeEpisodeName(n)) "" else n
    }

    fun channelShown(network: String): Boolean = network.isNotBlank() && network.trim().lowercase() in STREAMING

    fun tag(e: CalendarRow): EntryTag =
        when (e.kind) {
            "episode" ->
                when {
                    e.episode == 1 && e.season == 1 -> EntryTag.SHOW_PREMIERE
                    e.episode == 1 -> EntryTag.SEASON_PREMIERE
                    else -> EntryTag.NEW_EPISODE
                }
            "album" ->
                when (e.albumType.lowercase()) {
                    "single" -> EntryTag.NEW_SINGLE
                    "ep" -> EntryTag.NEW_EP
                    else -> EntryTag.NEW_ALBUM
                }
            "cinema" -> EntryTag.IN_CINEMAS
            else -> EntryTag.NEW_MOVIE
        }

    private fun kindRank(kind: String): Int =
        when (kind) {
            "cinema" -> 0
            "digital", "physical" -> 1
            "episode" -> 2
            "album" -> 3
            else -> 9
        }

    fun dayOrder(
        a: CalendarRow,
        b: CalendarRow,
    ): Int {
        val k = kindRank(a.kind) - kindRank(b.kind)
        if (k != 0) return k
        if (a.kind == "episode") {
            val at = if (a.timed) Days.parseInstant(a.date)?.epochSeconds ?: Long.MIN_VALUE else Long.MIN_VALUE
            val bt = if (b.timed) Days.parseInstant(b.date)?.epochSeconds ?: Long.MIN_VALUE else Long.MIN_VALUE
            if (at != bt) return at.compareTo(bt)
        }
        return compareValuesBy(a, b, { it.name.lowercase() }, { it.season }, { it.episode })
    }

    fun groups(
        entries: List<CalendarRow>,
        today: Day,
        manager: Boolean,
        offset: (Long) -> Int = ::localOffsetSeconds,
    ): List<CalendarGroup> {
        val byKey = linkedMapOf<String, Pair<Day, MutableList<CalendarRow>>>()
        val sorted =
            entries
                .mapNotNull { e -> entryDay(e, offset)?.let { it to e } }
                .sortedWith { x, y -> if (x.first == y.first) dayOrder(x.second, y.second) else x.first.compareTo(y.first) }
        for ((day, e) in sorted) {
            val who =
                when (e.kind) {
                    "album" -> "al${if (e.partId > 0) e.partId.toString() else e.album.ifEmpty { e.name }}"
                    "episode" -> "ep${e.titleId}"
                    else -> "mv${e.titleId}"
                }
            val key = "${day.key}:$who"
            byKey.getOrPut(key) { day to mutableListOf() }.second += e
        }
        return byKey.map { (key, value) -> group(key, value.first, value.second, today, manager, offset) }
    }

    private fun group(
        key: String,
        day: Day,
        entries: List<CalendarRow>,
        today: Day,
        manager: Boolean,
        offset: (Long) -> Int,
    ): CalendarGroup {
        val lead = entries.first()
        val count = entries.size
        val movieKinds =
            entries
                .map {
                    when (it.kind) {
                        "cinema" -> MovieDateKind.CINEMA
                        "digital" -> MovieDateKind.DIGITAL
                        else -> MovieDateKind.DISC
                    }
                }.distinct()
        val tag =
            when {
                count == 1 -> tag(lead)
                lead.kind == "episode" -> EntryTag.EPISODES
                lead.kind == "album" -> EntryTag.ALBUMS
                movieKinds.size > 1 -> EntryTag.MOVIE_DATES
                else -> tag(lead)
            }
        val timedAt = if (lead.kind == "episode" && lead.timed) Days.parseInstant(lead.date) else null
        val whenKind =
            when (lead.kind) {
                "episode" -> if (timedAt != null) EntryWhen.TIME else EntryWhen.ALL_DAY
                "album" ->
                    when (lead.albumType.lowercase()) {
                        "single" -> EntryWhen.SINGLE
                        "ep" -> EntryWhen.EP
                        else -> EntryWhen.ALBUM
                    }
                "cinema" -> EntryWhen.IN_THEATERS
                else -> EntryWhen.STREAMING
            }
        val eps = entries.sortedWith(compareBy({ it.season }, { it.episode }))
        val first = eps.first()
        val last = eps.last()
        val span =
            when {
                eps.size == 1 -> EpisodeSpan.ONE
                first.season == last.season -> EpisodeSpan.SAME_SEASON
                else -> EpisodeSpan.ACROSS
            }
        val caption =
            when (lead.kind) {
                "episode", "album" -> ""
                else -> listOf(lead.genres.firstOrNull().orEmpty(), lead.studio).filter { it.isNotBlank() }.joinToString(" · ")
            }
        val dots = entries.map { dot(it, today, offset) }
        val dot = dots.minByOrNull { severity(it) } ?: EntryDot.UPCOMING
        val art =
            lead.still
                .ifEmpty { lead.backdrop }
                .ifEmpty { lead.poster }
                .ifEmpty { null }
        return CalendarGroup(
            key = key,
            day = day,
            lead = lead,
            entries = entries,
            kind = lead.kind,
            title = if (lead.kind == "album") lead.album.ifEmpty { lead.name } else lead.name,
            tag = tag,
            count = count,
            movieKinds = movieKinds,
            whenKind = whenKind,
            minutes = timedAt?.let { Days.localMinutes(it, offset) } ?: -1,
            span = span,
            season = first.season,
            episode = first.episode,
            season2 = last.season,
            episode2 = last.episode,
            episodeName = if (count == 1) episodeName(lead.episodeName) else "",
            network = if (channelShown(lead.network)) lead.network else "",
            caption = caption,
            album = lead.album,
            dot = dot,
            art = art,
            poster = lead.poster.ifEmpty { null },
            following = lead.following,
            followers = lead.followers.filter { !it.me }.map { it.name },
            canSearch = manager && dots.any { it != EntryDot.HAVE },
        )
    }

    private fun severity(dot: EntryDot): Int =
        when (dot) {
            EntryDot.PROBLEM -> 0
            EntryDot.MISSING -> 1
            EntryDot.GETTING -> 2
            EntryDot.UPCOMING -> 3
            EntryDot.HAVE -> 4
        }

    fun monthCells(
        month: Month,
        groups: List<CalendarGroup>,
        today: Day,
    ): List<MonthCell> {
        val first = month.first
        val start = first.plusDays(-(first.weekday - 1))
        val weeks = (first.weekday - 1 + month.length + 6) / 7
        val byDay = groups.groupBy { it.day }
        return (0 until weeks * 7).map { i ->
            val day = start.plusDays(i)
            val list = byDay[day].orEmpty()
            val tone = list.minByOrNull { severity(it.dot) }?.dot?.tone
            MonthCell(day, day.month == month.month && day.year == month.year, day == today, list.size, tone)
        }
    }

    fun range(month: Month): Pair<Day, Day> {
        val first = month.first
        val start = first.plusDays(-(first.weekday - 1) - 1)
        val weeks = (first.weekday - 1 + month.length + 6) / 7
        val end = first.plusDays(-(first.weekday - 1) + weeks * 7 + 1)
        return start to end
    }

    fun searchBody(e: CalendarRow): ch.orellbuehler.acolyte.api.model.SearchBody =
        when {
            e.kind == "episode" && e.itemId > 0 ->
                ch.orellbuehler.acolyte.api.model
                    .SearchBody(itemId = e.itemId)
            e.kind == "album" && e.partId > 0 ->
                ch.orellbuehler.acolyte.api.model
                    .SearchBody(albumId = e.partId)
            else ->
                ch.orellbuehler.acolyte.api.model
                    .SearchBody()
        }

    fun missing(
        groups: List<CalendarGroup>,
        today: Day,
        offset: (Long) -> Int = ::localOffsetSeconds,
    ): List<CalendarRow> {
        val seen = mutableSetOf<String>()
        return groups.flatMap { it.entries }.filter { e ->
            if (dot(e, today, offset) != EntryDot.MISSING) return@filter false
            val k =
                when (e.kind) {
                    "episode" -> "e${e.itemId}"
                    "album" -> "a${e.partId}"
                    else -> "m${e.titleId}"
                }
            seen.add(k)
        }
    }
}
