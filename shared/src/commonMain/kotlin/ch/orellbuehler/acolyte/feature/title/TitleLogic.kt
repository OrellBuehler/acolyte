package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.OwnedEpisode
import ch.orellbuehler.acolyte.api.model.OwnedFile
import ch.orellbuehler.acolyte.api.model.OwnedSeason
import ch.orellbuehler.acolyte.api.model.OwnedShow
import ch.orellbuehler.acolyte.api.model.OwnedSlot
import ch.orellbuehler.acolyte.api.model.OwnedTitle
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.SubtitleRowView
import ch.orellbuehler.acolyte.api.model.SubtitleWant
import ch.orellbuehler.acolyte.api.model.TitleKindDefaults
import ch.orellbuehler.acolyte.api.model.TitleLeftover
import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.api.model.TitleReleaseReason
import ch.orellbuehler.acolyte.api.model.TitleScore
import ch.orellbuehler.acolyte.api.model.TitleVideo
import ch.orellbuehler.acolyte.feature.Tone
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

enum class LibraryKind {
    HAVE,
    DOWNLOADING,
    FAILED,
    MISSING,
}

enum class LibraryLabel {
    IN_LIBRARY,
    DOWNLOADED,
    DOWNLOADING,
    DOWNLOADING_PERCENT,
    DOWNLOADING_EPISODES,
    FAILED_EPISODES,
    EPISODES_OF,
    DOWNLOAD_FAILED,
    NOT_DOWNLOADED,
}

data class LibraryStatus(
    val kind: LibraryKind,
    val label: LibraryLabel,
    val count: Int = 0,
    val total: Int = 0,
) {
    val tone: Tone
        get() =
            when (kind) {
                LibraryKind.HAVE -> Tone.OK
                LibraryKind.DOWNLOADING -> Tone.INFO
                LibraryKind.FAILED -> Tone.FAIL
                LibraryKind.MISSING -> Tone.IDLE
            }
}

enum class EpisodeTone {
    HAVE,
    DOWNLOADING,
    FAILED,
    MISSING,
}

enum class AirWord {
    TODAY,
    YESTERDAY,
    TOMORROW,
    WEEKDAY,
    DATE,
    DATE_WITH_YEAR,
    NONE,
}

enum class SeasonPick {
    ALL,
    LATEST,
    OFF,
    CUSTOM,
}

enum class VerdictKind {
    TURNED_DOWN,
    CHOSEN,
    HELD,
    WOULD_CHOOSE,
    WOULD_HOLD,
    REFUSED,
    PASSED_OVER,
    JUDGED,
}

data class Verdict(
    val kind: VerdictKind,
    val tone: Tone,
)

enum class ReleaseSort {
    BEST,
    POINTS,
    QUALITY,
    SIZE,
    SEEDERS,
    AGE,
}

enum class LabelProblem {
    EMPTY,
    TOO_LONG,
    BAD_CHARACTER,
    TRAILING_DOT,
    DEVICE_NAME,
    TAKEN,
}

data class StartChoices(
    val folderId: Long,
    val rulesId: Long,
    val keepUpToDate: Boolean,
    val searchOnAdd: Boolean,
)

data class SecondVersion(
    val label: String,
    val rulesId: Long,
    val folderId: Long,
)

object TitleLogic {
    const val MIN_TMDB_VOTES = 25
    private val scoreOrder = listOf("imdb", "rt_critics", "rt_audience", "metacritic", "letterboxd", "trakt")
    val defaultRatingPicks = listOf("imdb", "rt_critics", "rt_audience")
    private val youTubeKey = Regex("^[A-Za-z0-9_-]{6,20}$")

    fun eventTone(kind: String): Tone =
        when {
            kind.contains("failed") || kind.contains("rejected") || kind.contains("stalled") || kind.contains("vanished") -> Tone.FAIL
            kind.startsWith("import") || kind.startsWith("file_") -> Tone.OK
            kind.startsWith("download") || kind.startsWith("release_chosen") || kind.startsWith("grab") -> Tone.INFO
            kind.contains("held") || kind.contains("decision") -> Tone.WAIT
            else -> Tone.IDLE
        }

    fun libraryStatus(owned: OwnedTitle): LibraryStatus {
        val show = owned.show
        if (show != null) {
            val aired = airedEpisodes(show)
            return when {
                show.downloading > 0 -> LibraryStatus(LibraryKind.DOWNLOADING, LibraryLabel.DOWNLOADING_EPISODES, show.downloading)
                show.failed > 0 -> LibraryStatus(LibraryKind.FAILED, LibraryLabel.FAILED_EPISODES, show.failed)
                show.episodes > 0 && show.inLibrary >= aired -> LibraryStatus(LibraryKind.HAVE, LibraryLabel.IN_LIBRARY)
                else ->
                    LibraryStatus(
                        if (show.inLibrary > 0) LibraryKind.HAVE else LibraryKind.MISSING,
                        LibraryLabel.EPISODES_OF,
                        show.inLibrary,
                        aired,
                    )
            }
        }
        val active = activeSlot(owned)
        if (active != null) {
            val download = active.download
            return if (download != null) {
                val percent = floor(download.progress.coerceIn(0.0, 1.0) * 100).toInt()
                LibraryStatus(LibraryKind.DOWNLOADING, LibraryLabel.DOWNLOADING_PERCENT, percent)
            } else {
                LibraryStatus(LibraryKind.DOWNLOADING, LibraryLabel.DOWNLOADING)
            }
        }
        if (owned.slots.any { it.file != null }) return LibraryStatus(LibraryKind.HAVE, LibraryLabel.IN_LIBRARY)
        if (owned.slots.any { leftInDownloader(it) }) return LibraryStatus(LibraryKind.HAVE, LibraryLabel.DOWNLOADED)
        if (owned.slots.any { it.state == "failed" }) return LibraryStatus(LibraryKind.FAILED, LibraryLabel.DOWNLOAD_FAILED)
        return LibraryStatus(LibraryKind.MISSING, LibraryLabel.NOT_DOWNLOADED)
    }

    fun activeSlot(owned: OwnedTitle): OwnedSlot? =
        owned.slots.firstOrNull { it.download?.active == true || it.state == "downloading" || it.state == "importing" }

    fun leftInDownloader(slot: OwnedSlot): Boolean = slot.state == "in_library" && slot.file == null

    fun standInFolder(path: String): Boolean = path.startsWith("hands-off:")

    fun airedEpisodes(show: OwnedShow): Int {
        if (show.seasons.isEmpty()) return maxOf(0, show.episodes - show.notOutYet)
        return show.seasons.sumOf { if (it.number > 0 || it.keepUpToDate) it.aired else 0 }
    }

    fun fullSeasons(show: OwnedShow): Int = show.seasons.count { it.number > 0 && it.episodes > 0 && it.inLibrary >= it.episodes }

    fun qualityBadges(files: List<OwnedFile>): List<String> {
        val out = mutableSetOf<String>()
        for (f in files) {
            val res = f.quality.resolution.takeIf { it > 0 } ?: f.info("resolution").filter { it.isDigit() }.toIntOrNull() ?: 0
            if (res >= 2160) {
                out += "4K"
            } else if (res >= 1080) {
                out += "HD"
            }
            val hdr =
                (
                    f.quality.hdr +
                        listOf(
                            f.info("hdr"),
                            f.info("video_dynamic_range"),
                            f.info("hdr_format"),
                        )
                ).joinToString(" ").lowercase()
            if (Regex("dolby ?vision|\\bdv\\b|dovi").containsMatchIn(hdr)) out += "Dolby Vision"
            if (Regex("hdr|hlg|pq").containsMatchIn(hdr.replace(Regex("dolby ?vision|dovi"), ""))) out += "HDR"
            val audio = listOf(f.quality.audio, f.info("audio_codec"), f.info("audio_format")).joinToString(" ").lowercase()
            if ("atmos" in audio) out += "Atmos"
            if (f.subtitleLanguages.isNotEmpty()) out += "CC"
        }
        val list = listOf("4K", "HD", "Dolby Vision", "HDR", "Atmos", "CC").filter { it in out }
        return if ("4K" in list) list - "HD" else list
    }

    fun pickTrailer(
        videos: List<TitleVideo>,
        language: String,
    ): TitleVideo? {
        val want = language.lowercase().substringBefore('-')
        var best: TitleVideo? = null
        var bestScore = -1
        for (v in videos) {
            if (!youTubeKey.matches(v.key)) continue
            var score =
                when (v.type) {
                    "Trailer" -> 100
                    "Teaser" -> 0
                    else -> continue
                }
            if (v.official) score += 10
            val lang = v.language.lowercase()
            if (want.isNotEmpty() && lang == want) {
                score += 5
            } else if (lang == "en" || lang.isEmpty()) {
                score += 2
            }
            if (score > bestScore) {
                best = v
                bestScore = score
            }
        }
        return best
    }

    fun trailerRow(
        videos: List<TitleVideo>,
        language: String,
    ): List<TitleVideo> {
        val playable = videos.filter { youTubeKey.matches(it.key) && (it.type == "Trailer" || it.type == "Teaser") }
        val main = pickTrailer(playable, language) ?: return playable
        return listOf(main) + playable.filter { it !== main }
    }

    fun cleanScores(scores: List<TitleScore>): List<TitleScore> =
        scores
            .filter { it.source in scoreOrder && it.value > 0 && it.max > 0 && it.value <= it.max }
            .sortedBy { scoreOrder.indexOf(it.source) }

    fun scoreText(score: TitleScore): String {
        if (score.max == 100.0) return if (score.source == "metacritic") "${score.value.roundToInt()}" else "${score.value.roundToInt()}%"
        return oneDecimal(score.value)
    }

    fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToInt()
        return "${tenths / 10}.${abs(tenths % 10)}"
    }

    fun shownScores(
        scores: List<TitleScore>,
        tmdb: Double,
        picks: List<String> = defaultRatingPicks,
    ): Pair<List<TitleScore>, Double?> {
        val out = scores.filter { it.source in picks }
        val tmdbShown = if (tmdb > 0 && "tmdb" in picks) tmdb else null
        if (out.isNotEmpty() || tmdbShown != null) return out to tmdbShown
        scores.firstOrNull()?.let { return listOf(it) to null }
        return emptyList<TitleScore>() to (if (tmdb > 0) tmdb else null)
    }

    fun seasonsToAsk(
        all: List<Int>,
        owned: OwnedTitle,
    ): List<Int> {
        val kept =
            if (owned.keepUpToDate) {
                owned.show
                    ?.seasons
                    .orEmpty()
                    .filter { it.keepUpToDate }
                    .map { it.number }
            } else {
                emptyList()
            }
        val complete =
            owned.show
                ?.seasons
                .orEmpty()
                .filter { it.number > 0 && it.aired > 0 && it.inLibrary >= it.aired }
                .map { it.number }
        return all.filter { it > 0 && it !in kept && it !in complete }.sorted()
    }

    fun newestSeason(seasons: List<SeasonBrief>): Int? {
        val regular = seasons.filter { it.number > 0 }.sortedByDescending { it.number }
        val live = regular.firstOrNull { it.aired > 0 || it.episodes > 0 || it.notOutYet > 0 || it.hasNext }
        return (live ?: regular.firstOrNull() ?: seasons.firstOrNull())?.number
    }

    fun seasonOrder(numbers: List<Int>): List<Int> = numbers.filter { it > 0 }.sortedDescending() + numbers.filter { it <= 0 }

    fun episodeTone(
        state: String,
        hasFile: Boolean,
    ): EpisodeTone =
        when {
            state == "have" || state == "imported" || state == "in_library" || state == "kept" -> EpisodeTone.HAVE
            hasFile && state != "downloading" && state != "importing" && state != "searching" -> EpisodeTone.HAVE
            state == "downloading" || state == "importing" || state == "searching" -> EpisodeTone.DOWNLOADING
            state == "failed" -> EpisodeTone.FAILED
            else -> EpisodeTone.MISSING
        }

    fun episodeTone(episode: OwnedEpisode): EpisodeTone {
        val slot = episode.mainSlot
        return episodeTone(slot?.state.orEmpty(), slot?.file != null || (slot?.fileId ?: 0) > 0)
    }

    fun epQuality(file: OwnedFile?): String {
        if (file == null) return ""
        val q = file.quality
        val source =
            when (q.source) {
                "web_dl", "webrip", "web_rip" -> "WEB"
                "bluray" -> "BluRay"
                "remux" -> "Remux"
                "hdtv", "tv" -> "TV"
                "dvd" -> "DVD"
                else -> ""
            }
        return listOf(if (q.resolution > 0) "${q.resolution}p" else "", source).filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun showOffHint(
        showKept: Boolean,
        seasons: List<OwnedSeason>,
    ): Boolean = !showKept && seasons.any { it.keepUpToDate }

    fun getAllMissingShown(
        showKept: Boolean,
        seasons: List<OwnedSeason>,
    ): Boolean = showKept && seasons.any { it.keepUpToDate && it.inLibrary < it.episodes - it.notOutYet }

    fun seasonPick(
        seasons: List<OwnedSeason>,
        newSeasons: String,
    ): SeasonPick {
        val real = seasons.filter { it.number > 0 }
        val kept = real.filter { it.keepUpToDate }
        val latest = real.maxOfOrNull { it.number } ?: 0
        val newOn = newSeasons != "none"
        return when {
            kept.size == real.size && newOn -> SeasonPick.ALL
            kept.isEmpty() && !newOn -> SeasonPick.OFF
            kept.size == 1 && kept[0].number == latest && newOn -> SeasonPick.LATEST
            else -> SeasonPick.CUSTOM
        }
    }

    fun seasonKeepPlan(
        seasons: List<OwnedSeason>,
        pick: SeasonPick,
    ): List<Pair<Int, Boolean>> {
        val real = seasons.filter { it.number > 0 }
        val latest = real.maxOfOrNull { it.number } ?: 0
        return real.mapNotNull { s ->
            val want = pick == SeasonPick.ALL || (pick == SeasonPick.LATEST && s.number == latest)
            if (want != s.keepUpToDate) s.number to want else null
        }
    }

    fun epochDay(iso: String): Long? {
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(iso) ?: return null
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        if (mo !in 1..12 || d !in 1..31) return null
        val yy = if (mo <= 2) y - 1 else y
        val era = (if (yy >= 0) yy else yy - 399) / 400
        val yoe = yy - era * 400
        val doy = (153 * (mo + (if (mo > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }

    fun airWord(
        day: String,
        today: String,
    ): AirWord {
        val target = epochDay(day) ?: return AirWord.NONE
        val now = epochDay(today) ?: return AirWord.DATE_WITH_YEAR
        val diff = target - now
        return when {
            diff == 0L -> AirWord.TODAY
            diff == -1L -> AirWord.YESTERDAY
            diff == 1L -> AirWord.TOMORROW
            day.take(4) != today.take(4) -> AirWord.DATE_WITH_YEAR
            abs(diff) < 7 -> AirWord.WEEKDAY
            else -> AirWord.DATE
        }
    }

    fun notAired(
        episode: OwnedEpisode,
        today: String,
    ): Boolean {
        val day = epochDay(episode.airDay.ifEmpty { episode.airDate }) ?: return false
        val now = epochDay(today) ?: return false
        return day > now
    }

    fun verdict(release: TitleRelease): Verdict {
        val kind =
            when {
                release.state == "turned_down" -> VerdictKind.TURNED_DOWN
                release.state == "chosen" -> VerdictKind.CHOSEN
                release.state == "held" -> VerdictKind.HELD
                release.outcome == "chosen" -> VerdictKind.WOULD_CHOOSE
                release.outcome == "held" -> VerdictKind.WOULD_HOLD
                release.outcome == "refused" -> VerdictKind.REFUSED
                release.outcome == "passed_over" -> VerdictKind.PASSED_OVER
                release.outcome == "turned_down" -> VerdictKind.TURNED_DOWN
                else -> VerdictKind.JUDGED
            }
        val tone =
            when {
                release.state == "chosen" -> Tone.INFO
                release.state == "turned_down" || release.outcome == "refused" -> Tone.FAIL
                release.outcome == "chosen" -> Tone.OK
                release.outcome == "held" || release.state == "held" -> Tone.WAIT
                else -> Tone.IDLE
            }
        return Verdict(kind, tone)
    }

    fun keyReasons(reasons: List<TitleReleaseReason>): List<TitleReleaseReason> {
        val order = listOf("fail", "hold", "rank", "warn")
        return reasons.filter { it.effect in order }.sortedBy { order.indexOf(it.effect) }
    }

    fun goodMatch(release: TitleRelease): Boolean = release.passed && release.state != "turned_down"

    fun recommended(release: TitleRelease): Boolean = release.outcome == "chosen" && release.state != "chosen"

    fun resolutionWord(n: Int): String = if (n >= 2160) "4K" else "${n}p"

    fun readTags(release: TitleRelease): List<String> {
        val x = release.read ?: return emptyList()
        val out = mutableListOf<String>()
        if (x.format.isNotEmpty()) {
            out += x.format.uppercase()
            if (x.bitDepth > 0) {
                out += "${x.bitDepth}-bit"
            } else if (x.bitrate > 0) {
                out += "${x.bitrate} kbps"
            }
        }
        if (x.resolution > 0) out += resolutionWord(x.resolution)
        sourceWord[x.source]?.let { out += it }
        x.hdr.forEach { h -> hdrWord[h]?.let { out += it } }
        if (x.codec.isNotEmpty()) out += codecWord[x.codec.lowercase()] ?: x.codec
        if (x.atmos) {
            out += "Atmos"
        } else if (x.channels.isNotEmpty()) {
            out += x.channels
        }
        x.languages.forEach { out += it.uppercase() }
        if (x.proper) out += "PROPER"
        return out
    }

    private val sourceWord =
        mapOf(
            "remux" to "Remux",
            "bluray" to "BluRay",
            "web_dl" to "WEB-DL",
            "web_rip" to "WEBRip",
            "tv" to "HDTV",
            "dvd" to "DVD",
            "disc_image" to "Disc",
            "cam" to "CAM",
            "telesync" to "TS",
        )
    private val hdrWord = mapOf("dv" to "Dolby Vision", "hdr10plus" to "HDR10+", "hdr10" to "HDR10", "hlg" to "HLG")
    private val codecWord =
        mapOf("x265" to "x265", "h265" to "x265", "hevc" to "x265", "x264" to "x264", "h264" to "x264", "av1" to "AV1")
    private val sourceRank =
        mapOf(
            "remux" to 9,
            "bluray" to 8,
            "disc_image" to 8,
            "web_dl" to 7,
            "web_rip" to 6,
            "tv" to 5,
            "dvd" to 4,
            "telesync" to 1,
            "cam" to 0,
        )

    fun qualityRank(release: TitleRelease): Int {
        val r = release.read
        return (r?.resolution ?: 0) * 10 + (sourceRank[r?.source.orEmpty()] ?: 3) + (r?.bitDepth ?: 0) + if (r?.lossless == true) 5 else 0
    }

    private val byBest =
        Comparator<TitleRelease> { a, b ->
            compareValuesBy(b, a) { if (it.outcome == "chosen") 1 else 0 }.takeIf { it != 0 }
                ?: compareValuesBy(b, a) { if (it.passed) 1 else 0 }.takeIf { it != 0 }
                ?: compareValuesBy(a, b) { if (it.rank == 0) 999 else it.rank }.takeIf { it != 0 }
                ?: (b.points - a.points)
        }

    fun sortReleases(
        list: List<TitleRelease>,
        by: ReleaseSort,
    ): List<TitleRelease> {
        val first: Comparator<TitleRelease>? =
            when (by) {
                ReleaseSort.BEST -> null
                ReleaseSort.POINTS -> compareByDescending { it.points }
                ReleaseSort.QUALITY -> compareByDescending { qualityRank(it) }
                ReleaseSort.SIZE -> compareByDescending { it.size }
                ReleaseSort.SEEDERS -> compareByDescending { it.seeders ?: -1 }
                ReleaseSort.AGE -> compareByDescending { it.publishedAt }
            }
        return list.sortedWith(first?.then(byBest) ?: byBest)
    }

    fun resolutionsOf(list: List<TitleRelease>): List<Int> =
        list
            .mapNotNull { it.read?.resolution?.takeIf { r -> r > 0 } }
            .distinct()
            .sortedDescending()

    fun sourcesOf(list: List<TitleRelease>): List<String> =
        list
            .map { it.sourceName }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

    fun languagesOf(list: List<TitleRelease>): List<String> {
        val counts = mutableMapOf<String, Int>()
        for (r in list) {
            for (l in r.read
                ?.languages
                .orEmpty()
                .toSet()) {
                counts[l] = (counts[l] ?: 0) + 1
            }
        }
        return counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).map { it.key }
    }

    fun nameMatches(
        name: String,
        query: String,
    ): Boolean {
        val separators = Regex("[._\\-]+")
        val n = name.lowercase().replace(separators, " ")
        return query
            .lowercase()
            .replace(separators, " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .all { it in n }
    }

    fun foldersFor(
        folders: List<LibraryFolder>,
        kind: String,
    ): List<LibraryFolder> = folders.filter { it.kind == kind }

    fun rulesFor(
        rules: List<PickRule>,
        kind: String,
    ): List<PickRule> = rules.filter { it.kinds.isEmpty() || kind in it.kinds }

    fun startChoices(
        defaults: TitleKindDefaults?,
        folders: List<LibraryFolder>,
        rules: List<PickRule>,
        kind: String,
        builtInSearch: Boolean,
    ): StartChoices {
        val kindFolders = foldersFor(folders, kind)
        val kindRules = rulesFor(rules, kind)
        val folder =
            kindFolders.firstOrNull { it.id == defaults?.libraryFolderId }
                ?: kindFolders.firstOrNull { it.health == "ok" }
                ?: kindFolders.firstOrNull()
        val rulesId =
            kindRules.firstOrNull { it.id == defaults?.pickRulesId }?.id
                ?: kindRules.firstOrNull { it.id == folder?.defaultPickRulesId }?.id
                ?: kindRules.firstOrNull()?.id
                ?: 0
        return StartChoices(
            folderId = folder?.id ?: 0,
            rulesId = rulesId,
            keepUpToDate = defaults?.keepUpToDate ?: true,
            searchOnAdd = defaults?.searchOnAdd ?: builtInSearch,
        )
    }

    fun rulesForFolder(
        folder: LibraryFolder?,
        defaults: TitleKindDefaults?,
        rules: List<PickRule>,
    ): Long =
        rules.firstOrNull { it.id == defaults?.pickRulesId }?.id
            ?: rules.firstOrNull { it.id == folder?.defaultPickRulesId }?.id
            ?: rules.firstOrNull()?.id
            ?: 0

    fun secondVersion(
        taken: List<String>,
        rules: List<PickRule>,
        folders: List<LibraryFolder>,
        mainFolderId: Long,
    ): SecondVersion {
        val fourK = Regex("4k|2160|uhd", RegexOption.IGNORE_CASE)
        val label = if (taken.any { it.equals("4K", ignoreCase = true) }) "" else "4K"
        val rulesId =
            (rules.firstOrNull { it.name.equals("4K", ignoreCase = true) } ?: rules.firstOrNull { fourK.containsMatchIn(it.name) })?.id
                ?: rules.firstOrNull()?.id
                ?: 0
        val folderId =
            folders.firstOrNull { it.id != mainFolderId && (fourK.containsMatchIn(it.label) || fourK.containsMatchIn(it.path)) }?.id
                ?: mainFolderId.takeIf { id -> folders.any { it.id == id } }
                ?: folders.firstOrNull()?.id
                ?: 0
        return SecondVersion(label, rulesId, folderId)
    }

    private val badLabelChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '[', ']')
    private val badEditionChars = badLabelChars + setOf('{', '}')
    private val deviceNames =
        setOf("CON", "PRN", "AUX", "NUL") + (1..9).flatMap { listOf("COM$it", "LPT$it") }

    fun labelProblem(
        label: String,
        taken: List<String>,
    ): LabelProblem? {
        val text = label.trim()
        return when {
            text.isEmpty() -> LabelProblem.EMPTY
            text.encodeToByteArray().size > 40 -> LabelProblem.TOO_LONG
            text.any { it in badLabelChars || it.isISOControl() || it in invisible } -> LabelProblem.BAD_CHARACTER
            text.endsWith(".") -> LabelProblem.TRAILING_DOT
            text.substringBefore('.').uppercase() in deviceNames -> LabelProblem.DEVICE_NAME
            taken.any { it.equals(text, ignoreCase = true) } -> LabelProblem.TAKEN
            else -> null
        }
    }

    fun editionProblem(edition: String): LabelProblem? {
        val text = edition.trim()
        return when {
            text.isEmpty() -> null
            text.encodeToByteArray().size > 40 -> LabelProblem.TOO_LONG
            text.any { it in badEditionChars || it.isISOControl() || it in invisible } -> LabelProblem.BAD_CHARACTER
            text.endsWith(".") -> LabelProblem.TRAILING_DOT
            text.substringBefore('.').uppercase() in deviceNames -> LabelProblem.DEVICE_NAME
            else -> null
        }
    }

    private val invisible =
        setOf('\u200B', '\u200C', '\u200D', '\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E', '\uFEFF')

    fun languageCodes(text: String): List<String> =
        text
            .split(Regex("[,\\s]+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun wishProblem(wants: List<SubtitleWant>): WishProblem? {
        if (wants.size > 10) return WishProblem.TOO_MANY
        val seen = mutableSetOf<String>()
        for (w in wants) {
            if (w.language.isEmpty()) return WishProblem.PICK
            if (!seen.add("${w.language}|${w.forced}")) return WishProblem.TWICE
        }
        return null
    }

    fun ourSubtitle(row: SubtitleRowView): Boolean =
        row.place == "beside" && row.state == "present" && (row.origin == "fetched" || row.origin == "by_hand")

    fun presentSubtitles(rows: List<SubtitleRowView>): List<SubtitleRowView> {
        val order = mapOf("built_in" to 0, "beside" to 1, "wish" to 2)
        return rows.filter { it.state == "present" }.sortedWith(compareBy<SubtitleRowView> { order[it.place] ?: 3 }.thenBy { it.language })
    }

    fun missingSubtitles(rows: List<SubtitleRowView>): List<SubtitleRowView> =
        rows.filter { it.place == "wish" && (it.state == "wanted" || it.state == "none_good_enough") }

    fun nextLanguage(wants: List<SubtitleWant>): String =
        listOf("en", "es", "fr", "de").firstOrNull { c -> wants.none { it.language == c } } ?: ""

    fun episodeCode(
        season: Int,
        episode: Int,
    ): String = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"

    fun episodeCodeOf(name: String): String? = Regex("S\\d{1,3}E\\d{1,4}", RegexOption.IGNORE_CASE).find(name)?.value?.uppercase()

    fun namePick(
        leftover: TitleLeftover,
        episodes: List<MatchEpisode>,
    ): List<Long> {
        val guess = leftover.guess
        if (leftover.reason != "seasons_differ" || guess == null) return emptyList()
        return episodes.filter { it.season == guess.season && it.episode in guess.episodes }.map { it.id }
    }

    fun shiftGuess(
        leftovers: List<TitleLeftover>,
        episodes: List<MatchEpisode>,
    ): ShiftGuess? {
        val bySeason = linkedMapOf<Int, MutableList<Pair<Long, Int>>>()
        val listed = episodes.map { it.season to it.episode }.toSet()
        for (l in leftovers) {
            val guess = l.guess ?: continue
            if (l.dismissed || guess.episodes.size != 1) continue
            val off = l.reason == "episode_not_listed" || (l.reason == "unmatched" && (guess.season to guess.episodes[0]) !in listed)
            if (!off) continue
            bySeason.getOrPut(guess.season) { mutableListOf() } += l.fileId to guess.episodes[0]
        }
        var best: ShiftGuess? = null
        var fits = 0
        val seasons = episodes.map { it.season }.distinct()
        for ((from, files) in bySeason) {
            if (files.size < 2) continue
            val sorted = files.sortedBy { it.second }
            for (to in seasons) {
                if (to == from) continue
                val empty = episodes.filter { it.season == to && !it.hasFile }.associateBy { it.episode }
                if (empty.isEmpty()) continue
                val shift = empty.keys.min() - sorted.first().second
                val moves = mutableListOf<ShiftMove>()
                for ((fileId, e) in sorted) {
                    val ep = empty[e + shift] ?: break
                    moves += ShiftMove(fileId, from, e, to, e + shift, ep.id)
                }
                if (moves.size != sorted.size) continue
                fits++
                best = ShiftGuess(from, to, shift, moves)
            }
        }
        return if (fits == 1) best else null
    }
}

enum class WishProblem {
    TOO_MANY,
    PICK,
    TWICE,
}

data class ShiftMove(
    val fileId: Long,
    val fromSeason: Int,
    val fromEpisode: Int,
    val toSeason: Int,
    val toEpisode: Int,
    val episodeId: Long,
)

data class ShiftGuess(
    val fromSeason: Int,
    val toSeason: Int,
    val shift: Int,
    val moves: List<ShiftMove>,
)

data class MatchEpisode(
    val id: Long,
    val season: Int,
    val episode: Int,
    val name: String = "",
    val hasFile: Boolean = false,
)

data class SeasonBrief(
    val number: Int,
    val episodes: Int = 0,
    val aired: Int = 0,
    val notOutYet: Int = 0,
    val hasNext: Boolean = false,
    val year: Int = 0,
)
