package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.model.Decision
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.ImportRecord
import ch.orellbuehler.acolyte.api.model.RestOf
import ch.orellbuehler.acolyte.feature.Tone
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class QueueStage(
    val wire: String,
) {
    CHECKING("checking"),
    QUEUED("queued"),
    PAUSED("paused"),
    DOWNLOADING("downloading"),
    STALLED("stalled"),
    IMPORTING("importing"),
    DONE("done"),
    SEEDING("seeding"),
    FAILED("failed"),
    DECIDE("decide"),
}

enum class DownloadsSub(
    internal val stages: Set<QueueStage>,
) {
    QUEUED(setOf(QueueStage.QUEUED)),
    PAUSED(setOf(QueueStage.PAUSED)),
    DOWNLOADING(setOf(QueueStage.DOWNLOADING, QueueStage.STALLED)),
    CHECKING(setOf(QueueStage.CHECKING)),
    IMPORTING(setOf(QueueStage.IMPORTING)),
    FAILED(setOf(QueueStage.FAILED)),
    DECIDE(setOf(QueueStage.DECIDE)),
}

enum class DownloadsTab(
    internal val stages: Set<QueueStage>,
    val subs: List<DownloadsSub>,
) {
    ALL(QueueStage.entries.toSet(), emptyList()),
    WAITING(setOf(QueueStage.QUEUED, QueueStage.PAUSED), listOf(DownloadsSub.QUEUED, DownloadsSub.PAUSED)),
    PROGRESS(
        setOf(QueueStage.DOWNLOADING, QueueStage.STALLED, QueueStage.CHECKING, QueueStage.IMPORTING),
        listOf(DownloadsSub.DOWNLOADING, DownloadsSub.CHECKING, DownloadsSub.IMPORTING),
    ),
    PROBLEMS(setOf(QueueStage.FAILED, QueueStage.DECIDE), listOf(DownloadsSub.FAILED, DownloadsSub.DECIDE)),
    DONE(setOf(QueueStage.DONE, QueueStage.SEEDING), emptyList()),
}

enum class StatusWord {
    FAILED,
    PACK_WAITING,
    MOVING,
    CHECKING,
    QUEUED,
    PAUSED,
    DOWNLOADING,
    STALLED,
    IMPORTING,
    SEEDING,
    REVIEW,
    DONE_SEEDING,
    DONE,
    IMPORTED,
    TAKEN,
}

enum class ProblemKind {
    MISSING_ARTICLES,
    REPAIR_NEEDED,
    REPAIRING,
    IMPORT_BLOCKED,
    TRACKER_ERROR,
    NO_TRACKER_REPLY,
    DOWNLOADER_STOPPED,
    NO_FULL_COPY,
    NO_PEERS,
    TRACKER_WARNING,
}

data class RowProblem(
    val kind: ProblemKind,
    val tone: Tone,
)

enum class DetailKind {
    FILES_GONE,
    UNPACKING,
    UP_NEXT,
    PLACING,
    IMPORT_FAILED,
    NO_FILE,
    REMOVED,
    REFUSED,
    NO_MATCH,
    CLEANUP_HOLD,
    RATIO,
}

data class RowDetail(
    val kind: DetailKind,
    val text: String = "",
)

enum class DownloadNoteKind {
    NEXT_IN_LINE,
    IN_LINE,
    VERIFYING,
    REPAIRING,
    UNPACKING,
    FINISHING,
    SCRIPT,
    FILES_MISSING,
    DOWNLOADER_ERROR,
    IMPORT_FAILED,
    REMOVED_OUTSIDE,
    NO_MATCH,
    FILES_GONE,
    STILL_IN,
    CLIENT_TEXT,
    DETAIL,
}

data class RowNote(
    val kind: DownloadNoteKind,
    val number: Int = 0,
    val text: String = "",
    val detail: RowDetail? = null,
)

enum class BarTone {
    GO,
    QUEUED,
    HELD,
    BAD,
    DONE,
}

enum class DownloadKindFilter(
    val wire: String,
) {
    ALL(""),
    MOVIE("movie"),
    SHOW("show"),
    MUSIC("artist"),
}

enum class NetworkFilter(
    val wire: String,
) {
    ANY(""),
    USENET("usenet"),
    TORRENT("torrent"),
}

enum class SubLineKind {
    TEXT,
    SEASON,
    SEASONS,
}

data class SubLine(
    val kind: SubLineKind,
    val text: String = "",
    val from: Int = 0,
    val to: Int = 0,
)

enum class DownloadAction {
    PAUSE,
    RESUME,
    RETRY,
    MOVE_UP,
    MOVE_DOWN,
    MOVE_TOP,
    MOVE_BOTTOM,
    PRIORITY,
    IMPORT_AGAIN,
    OPEN_TITLE,
    REMOVE,
    RECHECK,
    REANNOUNCE,
    FORCE_START,
    SEEDING_RULES,
    SET_LOCATION,
    SPEED_LIMITS,
    MARK_FAILED,
}

enum class UsenetPriority(
    val wire: String,
) {
    LOW("low"),
    NORMAL("normal"),
    HIGH("high"),
    FORCE("force"),
}

enum class SortKey(
    internal val numeric: Boolean,
) {
    TITLE(false),
    PROGRESS(true),
    STATUS(false),
    SIZE(true),
    QUALITY(false),
    ADDED(true),
    SEEDS(true),
    PEERS(true),
    RATIO(true),
    TRACKER(false),
    REQUESTED(false),
    DOWNLOADER(false),
}

data class SortOrder(
    val key: SortKey,
    val descending: Boolean,
)

enum class TrackerFilterKind {
    HOST,
    ERROR,
    WARNING,
    ANNOUNCE,
}

data class TrackerOption(
    val key: String,
    val kind: TrackerFilterKind,
    val label: String,
    val count: Int,
)

data class TabCount(
    val tab: DownloadsTab,
    val count: Int,
)

data class SubCount(
    val sub: DownloadsSub,
    val count: Int,
)

object DownloadRules {
    private val NOW_PHASES = setOf("waiting", "downloading", "stalled", "paused", "waiting_to_import", "importing")
    private val USENET_QUEUED = setOf("waiting", "downloading", "paused", "stalled")
    private val TORRENT_KINDS = setOf("qbittorrent", "transmission", "deluge", "rtorrent", "aria2")
    private val NOT_FOUND = Regex("no such file or directory|cannot find the (file|path)", RegexOption.IGNORE_CASE)

    fun stageOf(r: DownloadRow): QueueStage {
        if (r.clientHint == "checking" || r.clientHint == "moving") return QueueStage.CHECKING
        return when (r.phase) {
            "waiting" -> QueueStage.QUEUED
            "paused" -> QueueStage.PAUSED
            "downloading" -> if (r.progress >= 1) QueueStage.IMPORTING else QueueStage.DOWNLOADING
            "stalled" -> QueueStage.STALLED
            "waiting_to_import", "importing" -> QueueStage.IMPORTING
            "needs_a_decision" -> QueueStage.DECIDE
            "failed", "vanished" -> if (r.replaced) QueueStage.DONE else QueueStage.FAILED
            "import_refused" -> QueueStage.FAILED
            "done" -> if (r.state == "seeding") QueueStage.SEEDING else QueueStage.DONE
            else -> QueueStage.DONE
        }
    }

    fun inHistory(r: DownloadRow): Boolean =
        stageOf(r).let {
            it == QueueStage.DONE ||
                it == QueueStage.SEEDING ||
                it == QueueStage.FAILED
        }

    fun isNow(r: DownloadRow): Boolean = r.phase in NOW_PHASES

    fun partialPack(r: DownloadRow): Boolean = r.pack?.let { it.taken < it.total } ?: false

    fun protocolOf(r: DownloadRow): String {
        val p = r.protocol.lowercase()
        if (p == "torrent" || p == "usenet") return p
        val k = r.downloaderKind.lowercase()
        if (k == "sabnzbd" || k == "nzbget") return "usenet"
        if (k in TORRENT_KINDS) return "torrent"
        return ""
    }

    fun kindOf(r: DownloadRow): String = r.kind.ifEmpty { if (r.clientItem) "" else "movie" }

    fun statusWord(r: DownloadRow): StatusWord {
        if (r.phase == "failed" || r.phase == "vanished") return StatusWord.FAILED
        val stage = stageOf(r)
        if ((stage == QueueStage.DONE || stage == QueueStage.SEEDING) && partialPack(r)) return StatusWord.PACK_WAITING
        return when (stage) {
            QueueStage.CHECKING -> if (r.clientHint == "moving") StatusWord.MOVING else StatusWord.CHECKING
            QueueStage.QUEUED -> StatusWord.QUEUED
            QueueStage.PAUSED -> StatusWord.PAUSED
            QueueStage.DOWNLOADING -> StatusWord.DOWNLOADING
            QueueStage.STALLED -> StatusWord.STALLED
            QueueStage.IMPORTING -> StatusWord.IMPORTING
            QueueStage.SEEDING -> StatusWord.SEEDING
            QueueStage.DECIDE -> StatusWord.REVIEW
            QueueStage.FAILED -> StatusWord.FAILED
            QueueStage.DONE -> if (r.seeding) StatusWord.DONE_SEEDING else StatusWord.DONE
        }
    }

    fun problemOf(r: DownloadRow): RowProblem? {
        val stage = stageOf(r)
        if (stage == QueueStage.DONE || stage == QueueStage.PAUSED) return null
        val failed = stage == QueueStage.FAILED
        val protocol = protocolOf(r)
        if (protocol == "usenet") {
            val t = r.text
            if (Regex("article|missing|incomplete", RegexOption.IGNORE_CASE).containsMatchIn(t)) {
                return RowProblem(ProblemKind.MISSING_ARTICLES, if (failed) Tone.FAIL else Tone.WAIT)
            }
            val failing = failed || Regex("fail|error|need|unable|cannot", RegexOption.IGNORE_CASE).containsMatchIn(t)
            if (failing && Regex("repair|par2?\\b|verif", RegexOption.IGNORE_CASE).containsMatchIn(t)) {
                return RowProblem(ProblemKind.REPAIR_NEEDED, if (failed) Tone.FAIL else Tone.WAIT)
            }
            if (r.clientHint == "repairing") return RowProblem(ProblemKind.REPAIRING, Tone.WAIT)
        }
        if (r.phase == "import_refused" || stage == QueueStage.DECIDE) return RowProblem(ProblemKind.IMPORT_BLOCKED, Tone.FAIL)
        if (r.trackerProblem == "error") return RowProblem(ProblemKind.TRACKER_ERROR, Tone.FAIL)
        if (r.trackerProblem == "announce") return RowProblem(ProblemKind.NO_TRACKER_REPLY, Tone.WAIT)
        if (stage == QueueStage.STALLED &&
            Regex("^every download on this downloader stopped", RegexOption.IGNORE_CASE).containsMatchIn(r.text)
        ) {
            return RowProblem(ProblemKind.DOWNLOADER_STOPPED, Tone.WAIT)
        }
        val getting = (stage == QueueStage.DOWNLOADING || stage == QueueStage.STALLED) && !partialPack(r)
        val availability = r.availability
        if (getting && availability != null && availability < 1) {
            return RowProblem(ProblemKind.NO_FULL_COPY, if (stage == QueueStage.STALLED) Tone.FAIL else Tone.WAIT)
        }
        if (stage == QueueStage.STALLED && !partialPack(r) && protocol == "torrent" && r.seeds == 0 && r.peers == 0) {
            return RowProblem(ProblemKind.NO_PEERS, Tone.WAIT)
        }
        if (r.trackerProblem == "warning") return RowProblem(ProblemKind.TRACKER_WARNING, Tone.WAIT)
        return null
    }

    fun newestImports(imports: List<ImportRecord>): Map<Long, ImportRecord> {
        val out = mutableMapOf<Long, ImportRecord>()
        for (im in imports) {
            if (im.downloadId <= 0) continue
            val had = out[im.downloadId]
            if (had == null || im.id > had.id) out[im.downloadId] = im
        }
        return out
    }

    fun filesGone(im: ImportRecord?): Boolean {
        val res = im?.result.orEmpty().filter { it.outcome != "skipped" }
        return res.isNotEmpty() && res.all { it.outcome == "gone" || (it.outcome == "failed" && NOT_FOUND.containsMatchIn(it.error)) }
    }

    fun failReason(im: ImportRecord?): RowDetail? {
        if (im == null || im.state != "failed") return null
        val err =
            im.result
                .firstOrNull { it.error.isNotBlank() }
                ?.error
                ?.trim()
        if (err != null) return RowDetail(DetailKind.IMPORT_FAILED, if (err.length > 80) err.take(77) + "…" else err)
        return RowDetail(DetailKind.NO_FILE)
    }

    fun statusDetail(
        r: DownloadRow,
        im: ImportRecord?,
    ): RowDetail? {
        val stage = stageOf(r)
        if ((stage == QueueStage.DONE || stage == QueueStage.SEEDING || stage == QueueStage.DECIDE) &&
            filesGone(im)
        ) {
            return RowDetail(DetailKind.FILES_GONE)
        }
        if (stage == QueueStage.IMPORTING) {
            return when (r.phase) {
                "downloading" -> RowDetail(DetailKind.UNPACKING)
                "waiting_to_import" -> RowDetail(DetailKind.UP_NEXT)
                else -> RowDetail(DetailKind.PLACING)
            }
        }
        if (stage == QueueStage.FAILED || stage == QueueStage.DECIDE) {
            failReason(im)?.let { return it }
            if (r.phase == "vanished") return RowDetail(DetailKind.REMOVED)
            if (r.phase == "import_refused") {
                return if (r.refusedWhy.isNotBlank()) RowDetail(DetailKind.REFUSED, r.refusedWhy) else RowDetail(DetailKind.NO_MATCH)
            }
        }
        if ((stage == QueueStage.DONE || stage == QueueStage.SEEDING) && r.cleanupWait.isNotEmpty()) {
            val eps = r.cleanupWait
            val shown = eps.take(4).joinToString(", ") + if (eps.size > 4) " +${eps.size - 4}" else ""
            return RowDetail(DetailKind.CLEANUP_HOLD, shown)
        }
        val ratio = r.ratio
        if (stage == QueueStage.SEEDING && ratio != null) return RowDetail(DetailKind.RATIO, ratioText(ratio, 2))
        return null
    }

    fun stepNote(
        r: DownloadRow,
        detail: RowDetail?,
    ): RowNote? {
        when (r.clientHint) {
            "verifying" -> return RowNote(DownloadNoteKind.VERIFYING)
            "repairing" -> return RowNote(DownloadNoteKind.REPAIRING)
            "unpacking" -> return RowNote(DownloadNoteKind.UNPACKING)
            "finishing" -> return RowNote(DownloadNoteKind.FINISHING)
            "script" -> return RowNote(DownloadNoteKind.SCRIPT)
            "missing" -> return RowNote(DownloadNoteKind.FILES_MISSING)
            "errored" -> return RowNote(DownloadNoteKind.DOWNLOADER_ERROR)
        }
        return when (detail?.kind) {
            null -> null
            DetailKind.IMPORT_FAILED, DetailKind.NO_FILE -> RowNote(DownloadNoteKind.IMPORT_FAILED)
            DetailKind.REMOVED -> RowNote(DownloadNoteKind.REMOVED_OUTSIDE)
            DetailKind.NO_MATCH -> RowNote(DownloadNoteKind.NO_MATCH)
            DetailKind.FILES_GONE -> RowNote(DownloadNoteKind.FILES_GONE)
            else -> RowNote(DownloadNoteKind.DETAIL, detail = detail)
        }
    }

    fun queueNote(r: DownloadRow): RowNote? {
        if (stageOf(r) != QueueStage.QUEUED || r.queuePos <= 0) return null
        return if (r.queuePos == 1) RowNote(DownloadNoteKind.NEXT_IN_LINE) else RowNote(DownloadNoteKind.IN_LINE, number = r.queuePos)
    }

    fun barTone(r: DownloadRow): BarTone =
        when (stageOf(r)) {
            QueueStage.QUEUED -> BarTone.QUEUED
            QueueStage.PAUSED, QueueStage.STALLED -> BarTone.HELD
            QueueStage.DECIDE, QueueStage.FAILED -> BarTone.BAD
            else -> BarTone.GO
        }

    fun moving(r: DownloadRow): Boolean = r.active && r.speed > 0 && stageOf(r) == QueueStage.DOWNLOADING

    fun rowName(r: DownloadRow): String {
        if (r.title.isEmpty()) return r.name
        val year = if (r.year > 0) " (${r.year})" else ""
        return when (r.kind) {
            "show" -> r.title
            "artist" -> if (r.album.isNotEmpty()) "${r.title} – ${r.album}$year" else r.title
            else -> "${r.title}$year"
        }
    }

    fun subLine(r: DownloadRow): SubLine {
        when (r.kind) {
            "show" -> {
                val eps = r.episodes
                if (eps.size == 1) {
                    val e = eps[0]
                    val code = "S${pad(e.season)}E${pad(e.episode)}"
                    return SubLine(SubLineKind.TEXT, if (e.name.isNotEmpty()) "$code · ${e.name}" else code)
                }
                if (eps.size > 1) {
                    val seasons = eps.map { it.season }.distinct().sorted()
                    return if (seasons.size == 1) {
                        SubLine(SubLineKind.SEASON, from = seasons[0])
                    } else {
                        SubLine(SubLineKind.SEASONS, from = seasons.first(), to = seasons.last())
                    }
                }
                return SubLine(SubLineKind.TEXT, r.covers)
            }
            "artist" -> return SubLine(SubLineKind.TEXT, r.album.ifEmpty { r.covers })
        }
        if (r.kind == "movie" || r.title.isNotEmpty()) return SubLine(SubLineKind.TEXT, if (r.year > 0) r.year.toString() else "")
        return SubLine(SubLineKind.TEXT)
    }

    fun phoneTitle(r: DownloadRow): String = r.title.ifEmpty { r.name }

    fun quality(name: String): String {
        val n = name.lowercase()
        if (n.isEmpty()) return ""
        val res =
            when {
                Regex("\\b(2160p|4k|uhd)\\b").containsMatchIn(n) -> "2160p"
                Regex("\\b1080[pi]\\b").containsMatchIn(n) -> "1080p"
                Regex("\\b720p\\b").containsMatchIn(n) -> "720p"
                Regex("\\b(480p|576p|sd)\\b").containsMatchIn(n) -> "480p"
                else -> ""
            }
        val tag =
            when {
                Regex("\\b(dv|dovi|dolby[ ._-]?vision)\\b").containsMatchIn(n) -> "DV"
                Regex("\\bhdr").containsMatchIn(n) -> "HDR"
                else -> ""
            }
        return listOf(res, tag).filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun matches(
        r: DownloadRow,
        query: String,
    ): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return listOf(
            r.title,
            r.name,
            r.covers,
            r.album,
            r.downloader,
            r.downloaderKind,
            quality(r.name),
        ).any { it.lowercase().contains(q) }
    }

    fun byTracker(
        r: DownloadRow,
        key: String,
    ): Boolean {
        if (key.isEmpty()) return true
        if (key == "p:any") return r.trackerProblem.isNotEmpty()
        if (key.startsWith("p:")) return r.trackerProblem == key.removePrefix("p:")
        return key.startsWith("host:") && r.tracker == key.removePrefix("host:")
    }

    fun trackerOptions(rows: List<DownloadRow>): List<TrackerOption> {
        val hosts = mutableMapOf<String, Int>()
        val problems = mutableMapOf<String, Int>()
        for (r in rows) {
            if (r.tracker.isNotEmpty()) hosts[r.tracker] = (hosts[r.tracker] ?: 0) + 1
            if (r.trackerProblem.isNotEmpty()) problems[r.trackerProblem] = (problems[r.trackerProblem] ?: 0) + 1
        }
        val kinds =
            listOf(
                "error" to TrackerFilterKind.ERROR,
                "warning" to TrackerFilterKind.WARNING,
                "announce" to TrackerFilterKind.ANNOUNCE,
            )
        return kinds.filter { it.first in problems }.map { TrackerOption("p:${it.first}", it.second, "", problems.getValue(it.first)) } +
            hosts.entries.sortedBy { it.key }.map { TrackerOption("host:${it.key}", TrackerFilterKind.HOST, it.key, it.value) }
    }

    fun trackerIssues(rows: List<DownloadRow>): Int = rows.count { it.trackerProblem.isNotEmpty() }

    fun tabCounts(
        rows: List<DownloadRow>,
        extra: Map<QueueStage, Int>,
    ): List<TabCount> {
        val stages = rows.map { stageOf(it) }
        return DownloadsTab.entries.map { tab ->
            TabCount(tab, stages.count { it in tab.stages } + tab.stages.sumOf { extra[it] ?: 0 })
        }
    }

    fun subCounts(
        rows: List<DownloadRow>,
        tab: DownloadsTab,
        extra: Map<QueueStage, Int>,
    ): List<SubCount> {
        val stages = rows.map { stageOf(it) }
        return tab.subs.map { sub -> SubCount(sub, stages.count { it in sub.stages } + sub.stages.sumOf { extra[it] ?: 0 }) }
    }

    fun pick(
        rows: List<DownloadRow>,
        tab: DownloadsTab,
        sub: DownloadsSub?,
    ): List<DownloadRow> {
        val stages = sub?.takeIf { it in tab.subs }?.stages ?: tab.stages
        return rows.filter { stageOf(it) in stages }
    }

    fun restSum(
        rest: Map<String, RestOf>,
        protocol: String,
    ): Map<QueueStage, Int> {
        val out = mutableMapOf<QueueStage, Int>()
        for (entry in rest.values) {
            if (protocol.isNotEmpty() && entry.protocol != protocol) continue
            for ((name, n) in entry.stages) {
                val stage = QueueStage.entries.firstOrNull { it.wire == name } ?: continue
                out[stage] = (out[stage] ?: 0) + n
            }
        }
        return out
    }

    fun showOf(value: String): Pair<DownloadsTab, DownloadsSub?> {
        val legacy =
            mapOf(
                "all" to "active",
                "stalled" to "progress.downloading",
                "progress.stalled" to "progress.downloading",
                "seeding" to "done",
                "done.seeding" to "done",
                "done.done" to "done",
                "failed" to "problems.failed",
                "done.failed" to "problems.failed",
                "decide" to "problems.decide",
            )
        val parts = (legacy[value] ?: value).split('.')
        val tab =
            when (parts.getOrNull(0)) {
                "waiting" -> DownloadsTab.WAITING
                "progress" -> DownloadsTab.PROGRESS
                "problems" -> DownloadsTab.PROBLEMS
                "done" -> DownloadsTab.DONE
                else -> return DownloadsTab.ALL to null
            }
        val sub = tab.subs.firstOrNull { it.name.lowercase() == parts.getOrNull(1) }
        return tab to sub
    }

    fun controllable(kind: String): Boolean = kind.isEmpty() || kind == "qbittorrent" || kind == "sabnzbd"

    fun pausable(r: DownloadRow): Boolean {
        val k = r.downloaderKind
        val kindOk = controllable(k) || k == "nzbget" || k == "rtorrent" || k == "aria2"
        return kindOk && !(r.protocol == "usenet" && (r.phase == "done" || r.phase == "failed"))
    }

    fun retryable(r: DownloadRow): Boolean =
        (controllable(r.downloaderKind) || r.downloaderKind == "nzbget") &&
            r.clientItem &&
            r.outside &&
            r.hash.isNotEmpty() &&
            r.downloaderId > 0 &&
            r.protocol == "usenet" &&
            r.phase == "failed"

    fun usenetQueued(r: DownloadRow): Boolean =
        r.protocol == "usenet" &&
            (r.downloaderKind == "sabnzbd" || r.downloaderKind == "nzbget") &&
            r.downloaderId > 0 &&
            r.hash.isNotEmpty() &&
            r.phase in USENET_QUEUED

    fun clientOk(r: DownloadRow): Boolean = r.canAct && !r.clientLocked

    fun mayEdit(
        r: DownloadRow,
        canSettings: Boolean,
    ): Boolean = clientOk(r) && (!r.outside || canSettings)

    fun qbitTorrent(r: DownloadRow): Boolean =
        r.downloaderId > 0 &&
            r.hash.isNotEmpty() &&
            r.protocol == "torrent" &&
            (r.downloaderKind.isEmpty() || r.downloaderKind == "qbittorrent")

    fun opensTitle(r: DownloadRow): Boolean = r.titleId > 0 && (r.linked || !r.outside) && kindOf(r) in setOf("movie", "show")

    fun actions(
        r: DownloadRow,
        canSettings: Boolean,
    ): List<DownloadAction> {
        val out = mutableListOf<DownloadAction>()
        val client = clientOk(r)
        if (client) {
            if (pausable(r)) out += if (r.state == "paused") DownloadAction.RESUME else DownloadAction.PAUSE
            if (retryable(r) && mayEdit(r, canSettings)) out += DownloadAction.RETRY
            if (qbitTorrent(r) || usenetQueued(r)) {
                out += listOf(DownloadAction.MOVE_UP, DownloadAction.MOVE_DOWN, DownloadAction.MOVE_TOP, DownloadAction.MOVE_BOTTOM)
            }
            if (usenetQueued(r)) out += DownloadAction.PRIORITY
        }
        if (r.canImport && r.id > 0) out += DownloadAction.IMPORT_AGAIN
        if (opensTitle(r)) out += DownloadAction.OPEN_TITLE
        if (r.canAct) out += DownloadAction.REMOVE
        if (!r.canAct) return out
        val torrent = protocolOf(r) == "torrent"
        if (client && torrent && controllable(r.downloaderKind)) out += listOf(DownloadAction.RECHECK, DownloadAction.REANNOUNCE)
        if (client && torrent && qbitTorrent(r) && mayEdit(r, canSettings)) {
            out +=
                listOf(DownloadAction.FORCE_START, DownloadAction.SEEDING_RULES, DownloadAction.SET_LOCATION, DownloadAction.SPEED_LIMITS)
        }
        if (!r.clientItem && r.id > 0 && !inHistory(r)) out += DownloadAction.MARK_FAILED
        return out
    }

    fun sortRows(
        rows: List<DownloadRow>,
        order: SortOrder?,
        tab: DownloadsTab,
    ): List<DownloadRow> {
        if (order == null) {
            if (tab != DownloadsTab.DONE) return rows
            return sortBy(rows, true) { it.finishedAt.ifEmpty { it.updatedAt } }
        }
        return sortBy(rows, order.descending) { sortValue(it, order.key) }
    }

    fun firstDescending(key: SortKey): Boolean = key.numeric

    private fun sortValue(
        r: DownloadRow,
        key: SortKey,
    ): Comparable<*>? =
        when (key) {
            SortKey.TITLE -> rowName(r)
            SortKey.PROGRESS -> r.progress
            SortKey.STATUS -> statusWord(r).ordinal
            SortKey.SIZE -> r.size.takeIf { it > 0 }
            SortKey.QUALITY -> quality(r.name)
            SortKey.ADDED -> r.startedAt
            SortKey.SEEDS -> if (protocolOf(r) == "torrent") r.seeds else null
            SortKey.PEERS -> if (protocolOf(r) == "torrent") r.peers else null
            SortKey.RATIO -> r.ratio
            SortKey.TRACKER -> r.tracker
            SortKey.REQUESTED -> if (r.outside) "" else r.requestedBy
            SortKey.DOWNLOADER -> r.downloader
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> sortBy(
        rows: List<T>,
        descending: Boolean,
        key: (T) -> Comparable<*>?,
    ): List<T> {
        fun empty(v: Any?) = v == null || v == "" || (v is Double && !v.isFinite())
        val keyed = rows.mapIndexed { i, r -> Triple(r, i, key(r)) }
        return keyed
            .sortedWith { a, b ->
                val ea = empty(a.third)
                val eb = empty(b.third)
                if (ea || eb) {
                    if (ea == eb) {
                        a.second - b.second
                    } else if (ea) {
                        1
                    } else {
                        -1
                    }
                } else {
                    val c =
                        when (val x = a.third) {
                            is String -> naturalCompare(x, b.third as String)
                            else -> (x as Comparable<Any>).compareTo(b.third as Any)
                        }
                    val d = if (descending) -c else c
                    if (d != 0) d else a.second - b.second
                }
            }.map { it.first }
    }

    internal fun naturalCompare(
        a: String,
        b: String,
    ): Int {
        val x = a.lowercase()
        val y = b.lowercase()
        var i = 0
        var j = 0
        while (i < x.length && j < y.length) {
            val cx = x[i]
            val cy = y[j]
            if (cx.isDigit() && cy.isDigit()) {
                val si = i
                val sj = j
                while (i < x.length && x[i].isDigit()) i++
                while (j < y.length && y[j].isDigit()) j++
                val nx = x.substring(si, i).trimStart('0')
                val ny = y.substring(sj, j).trimStart('0')
                if (nx.length != ny.length) return nx.length - ny.length
                val c = nx.compareTo(ny)
                if (c != 0) return c
            } else {
                if (cx != cy) return cx.compareTo(cy)
                i++
                j++
            }
        }
        return (x.length - i) - (y.length - j)
    }

    fun summarise(rows: List<DownloadRow>): Pair<Int, Int> {
        val now = rows.filter { isNow(it) }
        if (now.isEmpty()) return 0 to 0
        val sum = now.sumOf { min(max(it.progress, 0.0), 1.0) }
        return now.size to floor(sum / now.size * 100).toInt()
    }

    fun queueTotals(rows: List<DownloadRow>): QueueTotals {
        val queue = rows.filter { !inHistory(it) }
        val speed = queue.sumOf { if (moving(it)) it.speed else 0 }
        val up = queue.sumOf { if (moving(it)) it.upSpeed else 0 }
        val left = queue.sumOf { max(0.0, it.size * (1 - min(max(it.progress, 0.0), 1.0))) }
        val eta = if (speed > 0 && left > 0) (left / speed).roundToLong() else 0
        return QueueTotals(speed, up, left.toLong(), eta)
    }

    fun decisionName(d: Decision): String {
        val ep = d.episode
        if (ep != null &&
            d.album.isNotEmpty()
        ) {
            return "${d.title} – ${d.album} ${ep.label}${if (ep.name.isNotEmpty()) " ${ep.name}" else ""}"
        }
        if (ep != null) return "${d.title} ${ep.label}${if (ep.name.isNotEmpty()) " ${ep.name}" else ""}"
        return "${d.title}${if (d.year > 0) " (${d.year})" else ""}"
    }

    fun decisionRow(d: Decision): DownloadRow =
        DownloadRow(
            id = -d.slotId,
            phase = "needs_a_decision",
            name = d.releaseName.ifEmpty { decisionName(d) },
            title = d.title,
            titleId = d.titleId,
            year = d.year,
            version = d.version,
            updatedAt = d.since,
            canAct = false,
            kind = d.kind,
            album = d.album,
            covers = d.episode?.let { e -> e.label + if (e.name.isNotEmpty()) " ${e.name}" else "" }.orEmpty(),
        )

    fun withDecisions(
        rows: List<DownloadRow>,
        decisions: List<Decision>,
    ): List<DownloadRow> {
        val ids = rows.filter { it.id > 0 }.map { it.id }.toSet()
        val waiting = decisions.filter { it.downloadId > 0 && it.downloadId in ids }.map { it.downloadId }.toSet()
        val merged =
            rows.map { r ->
                if (r.id in waiting && !r.active && r.phase != "needs_a_decision") r.copy(phase = "needs_a_decision") else r
            }
        return merged + decisions.filter { !(it.downloadId > 0 && it.downloadId in ids) }.map { decisionRow(it) }
    }

    fun keyOf(r: DownloadRow): String =
        when {
            r.id < 0 -> "s${-r.id}"
            r.clientItem || r.id == 0L -> "c${r.downloaderId}:${r.hash.lowercase()}"
            else -> "d${r.id}"
        }

    fun ratioText(
        ratio: Double,
        decimals: Int,
    ): String {
        var factor = 1.0
        repeat(decimals) { factor *= 10 }
        val scaled = kotlin.math.round(ratio * factor).toLong()
        if (decimals == 0) return scaled.toString()
        val whole = scaled / factor.toLong()
        val frac = (scaled % factor.toLong()).toString().padStart(decimals, '0')
        return "$whole.$frac"
    }

    fun shortRatio(ratio: Double): String =
        if (ratio >=
            100
        ) {
            ratioText(ratio, 0)
        } else if (ratio >= 10) {
            ratioText(ratio, 1)
        } else {
            ratioText(ratio, 2)
        }

    fun splitLinks(text: String): Pair<List<String>, List<String>> {
        val links = mutableListOf<String>()
        val bad = mutableListOf<String>()
        for (raw in text.lines()) {
            val l = raw.trim()
            if (l.isEmpty()) continue
            val good =
                Regex("^magnet:\\?", RegexOption.IGNORE_CASE).containsMatchIn(l) ||
                    Regex("^https?://\\S+$", RegexOption.IGNORE_CASE).matches(l) ||
                    Regex("^[0-9a-f]{40}$", RegexOption.IGNORE_CASE).matches(l)
            if (good) links += l else bad += l
        }
        return links to bad
    }

    fun splitTrackers(text: String): Pair<List<String>, List<String>> {
        val urls = mutableListOf<String>()
        val bad = mutableListOf<String>()
        for (raw in text.lines()) {
            val l = raw.trim()
            if (l.isEmpty()) continue
            if (Regex("^(https?|udp)://[^\\s|]+$", RegexOption.IGNORE_CASE).matches(l)) urls += l else bad += l
        }
        return urls to bad
    }

    fun limitsProblem(
        ratio: Double,
        seedingMinutes: Long,
        inactiveMinutes: Long,
    ): Boolean {
        if (ratio >= 0 && ratio > 9998) return true
        return listOf(seedingMinutes, inactiveMinutes).any { it >= 0 && it > 525600 }
    }

    private fun pad(n: Int): String = n.toString().padStart(2, '0')
}

data class QueueTotals(
    val speed: Long = 0,
    val upSpeed: Long = 0,
    val left: Long = 0,
    val eta: Long = 0,
)
