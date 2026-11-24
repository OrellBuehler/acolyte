package ch.orellbuehler.acolyte.feature.settings.status

import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.api.model.OutsideList
import ch.orellbuehler.acolyte.api.model.SettingsScheduleRow
import ch.orellbuehler.acolyte.api.model.SettingsStageState
import ch.orellbuehler.acolyte.api.model.SettingsStatusActiveJob
import ch.orellbuehler.acolyte.api.model.SettingsStatusAlert
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.home.epochMillisOf
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class SettingsStatusGroup {
    SOURCES,
    DOWNLOADERS,
    LIBRARY,
    MEDIA,
    SUBTITLES,
    LISTS,
    ALERTS,
    METADATA,
    APP,
    JOBS,
}

enum class SettingsStatusWord {
    WORKING,
    WARNING,
    BROKEN,
    STUCK,
    STARTING,
    RETRYING,
    OFF,
    NOT_SYNCED,
    NOT_SENT,
    TURNED_OFF,
    PART_OFF,
    SAFE_MODE,
    RUNNING,
    FAILED,
    LATE,
    NOT_RUN,
    CANCELLED,
    OK,
}

enum class SettingsStatusTitle {
    PLAIN,
    FOLDER,
    FREE_SPACE,
    BACKUPS,
    UPDATE,
    CONFIG_DISK,
    CONFIG_FOLDER,
    WAITING_DOWNLOADS,
    CLEAR_WAITING,
    SAVE_FOLDER,
    TORRENT_DOWNLOADER,
    USENET_DOWNLOADER,
    SAME_DOWNLOADER,
    PEOPLE_CHECK,
    METADATA,
}

enum class SettingsStatusReason {
    NONE,
    TEXT,
    JOB_LATE,
    JOB_FAILED,
    CONFIG_STEPS,
    SAVE_FOLDER,
    NO_TORRENT_DOWNLOADER,
    NO_USENET_DOWNLOADER,
    SAME_DOWNLOADER,
    KEY_KIT,
    KEY_KIT_OWNER,
    METADATA_DOWN,
}

enum class SettingsStatusFix {
    SOURCES,
    DOWNLOADERS,
    MEDIA_SERVERS,
    SUBTITLES,
    LIBRARY_FOLDERS,
    BACKUPS,
    THIS_APP,
    OUTSIDE_LISTS,
    ALERTS,
    SCHEDULES,
    DOWNLOADS_WAITING,
    DOWNLOADS,
}

enum class SettingsStatusCheckKind {
    SOURCE,
    DOWNLOADER,
    JOB,
    CHECKS,
}

data class SettingsStatusCheck(
    val kind: SettingsStatusCheckKind,
    val id: Long = 0,
    val job: String = "",
)

data class SettingsStatusRow(
    val group: SettingsStatusGroup,
    val key: String,
    val title: SettingsStatusTitle = SettingsStatusTitle.PLAIN,
    val name: String = "",
    val tone: Tone = Tone.IDLE,
    val word: SettingsStatusWord = SettingsStatusWord.OFF,
    val reasonKind: SettingsStatusReason = SettingsStatusReason.NONE,
    val reason: String = "",
    val at: String = "",
    val fix: SettingsStatusFix? = null,
    val check: SettingsStatusCheck? = null,
    val lastRun: String = "",
    val nextRun: String = "",
    val lastResult: String = "",
) {
    val needsAttention: Boolean get() = tone == Tone.FAIL || tone == Tone.WAIT

    val stopped: Boolean get() = word in STOPPED_WORDS

    val isOff: Boolean get() = word == SettingsStatusWord.OFF
}

enum class SettingsStatusHeadlineKind {
    FINE,
    UNKNOWN,
    NOTHING_LINKED,
    ATTENTION,
}

data class SettingsStatusHeadline(
    val kind: SettingsStatusHeadlineKind = SettingsStatusHeadlineKind.UNKNOWN,
    val checks: Int = 0,
    val jobs: Int = 0,
)

enum class SettingsStatusMarker {
    NONE,
    IDLE,
    OK,
    WAIT,
    FAIL,
}

enum class SettingsStatusSummaryKind {
    NOTHING,
    NEEDS,
    WORD,
    OFF,
    WORKING,
    WAITING,
}

data class SettingsStatusKind(
    val group: SettingsStatusGroup,
    val rows: List<SettingsStatusRow> = emptyList(),
    val okCount: Int = 0,
    val onCount: Int = 0,
    val marker: SettingsStatusMarker = SettingsStatusMarker.NONE,
    val summary: SettingsStatusSummaryKind = SettingsStatusSummaryKind.NOTHING,
    val summaryCount: Int = 0,
    val summaryWord: SettingsStatusWord? = null,
    val fix: SettingsStatusFix = SettingsStatusFix.SOURCES,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}

enum class SettingsStatusDiskKind {
    MOVIES,
    SHOWS,
    MUSIC,
    OTHER,
}

enum class SettingsStatusDiskLevel {
    NORMAL,
    WARN,
    FULL,
}

data class SettingsStatusDiskView(
    val path: String,
    val label: String = "",
    val kind: SettingsStatusDiskKind = SettingsStatusDiskKind.OTHER,
    val freeBytes: Long = -1,
    val totalBytes: Long = -1,
    val usedPercent: Int = -1,
    val level: SettingsStatusDiskLevel = SettingsStatusDiskLevel.NORMAL,
    val problem: SettingsStatusRow? = null,
)

enum class SettingsStatusDueKind {
    NONE,
    NOW,
    IN,
    OVERDUE,
}

enum class SettingsStatusUnit {
    MINUTES,
    HOURS,
    DAYS,
}

data class SettingsStatusDue(
    val kind: SettingsStatusDueKind = SettingsStatusDueKind.NONE,
    val unit: SettingsStatusUnit = SettingsStatusUnit.MINUTES,
    val amount: Int = 0,
)

data class SettingsStatusRunning(
    val id: Long,
    val kind: String,
    val name: String,
    val queued: Boolean = false,
    val progress: Double = 0.0,
    val note: String = "",
    val startedAt: String = "",
) {
    val percent: Int get() = (progress * PERCENT).toInt().coerceIn(0, PERCENT)

    val hasProgress: Boolean get() = !queued && progress > 0.0
}

data class SettingsStatusUpcoming(
    val key: String,
    val name: String,
    val tone: Tone,
    val word: SettingsStatusWord,
    val due: SettingsStatusDue,
)

data class SettingsStatusFinished(
    val id: Long,
    val name: String,
    val tone: Tone,
    val word: SettingsStatusWord,
    val endedAt: String,
)

private const val PERCENT = 100

private val STOPPED_WORDS = setOf(SettingsStatusWord.TURNED_OFF, SettingsStatusWord.PART_OFF, SettingsStatusWord.SAFE_MODE)

internal data class SettingsStatusInput(
    val states: List<SettingsStageState> = emptyList(),
    val checks: List<HealthCheck> = emptyList(),
    val schedules: List<SettingsScheduleRow> = emptyList(),
    val lists: List<OutsideList> = emptyList(),
    val alerts: List<SettingsStatusAlert> = emptyList(),
    val metadataUp: Boolean? = null,
    val metadataAt: String = "",
    val nowMillis: Long = 0,
)

internal data class SettingsStatusJobTone(
    val tone: Tone,
    val word: SettingsStatusWord,
    val reasonKind: SettingsStatusReason = SettingsStatusReason.NONE,
    val reason: String = "",
)

internal data class SettingsStatusSeen(
    val at: String = "",
    val checked: Boolean = false,
    val why: String = "",
)

internal object SettingsStatusRules {
    private const val LATE_MILLIS = 10 * 60_000L
    private const val SECOND = 1000L
    private const val MINUTE_SECONDS = 60L
    private const val HOUR_SECONDS = 3600L
    private const val DAY_SECONDS = 86400L
    private const val IMMINENT_SECONDS = 30L
    private const val LATE_GRACE_SECONDS = 60L
    private const val NEXT_UP = 5
    private const val RECENT = 10

    val kindOrder: List<SettingsStatusGroup> =
        listOf(
            SettingsStatusGroup.SOURCES,
            SettingsStatusGroup.DOWNLOADERS,
            SettingsStatusGroup.MEDIA,
            SettingsStatusGroup.SUBTITLES,
            SettingsStatusGroup.LISTS,
            SettingsStatusGroup.ALERTS,
            SettingsStatusGroup.METADATA,
        )

    private val kindFix =
        mapOf(
            SettingsStatusGroup.SOURCES to SettingsStatusFix.SOURCES,
            SettingsStatusGroup.DOWNLOADERS to SettingsStatusFix.DOWNLOADERS,
            SettingsStatusGroup.MEDIA to SettingsStatusFix.MEDIA_SERVERS,
            SettingsStatusGroup.SUBTITLES to SettingsStatusFix.SUBTITLES,
            SettingsStatusGroup.LISTS to SettingsStatusFix.OUTSIDE_LISTS,
            SettingsStatusGroup.ALERTS to SettingsStatusFix.ALERTS,
            SettingsStatusGroup.METADATA to SettingsStatusFix.THIS_APP,
        )

    private val linkGroup =
        mapOf(
            "source" to (SettingsStatusGroup.SOURCES to SettingsStatusFix.SOURCES),
            "downloader" to (SettingsStatusGroup.DOWNLOADERS to SettingsStatusFix.DOWNLOADERS),
            "media_server" to (SettingsStatusGroup.MEDIA to SettingsStatusFix.MEDIA_SERVERS),
            "subtitle_site" to (SettingsStatusGroup.SUBTITLES to SettingsStatusFix.SUBTITLES),
        )

    private val checkFix =
        mapOf(
            "library_folder" to SettingsStatusFix.LIBRARY_FOLDERS,
            "disk_space" to SettingsStatusFix.LIBRARY_FOLDERS,
            "backups" to SettingsStatusFix.BACKUPS,
            "update" to SettingsStatusFix.THIS_APP,
            "config_disk" to SettingsStatusFix.THIS_APP,
            "config_folder" to SettingsStatusFix.BACKUPS,
            "waiting_downloads" to SettingsStatusFix.DOWNLOADS_WAITING,
            "clear_waiting" to SettingsStatusFix.DOWNLOADS,
            "save_folder" to SettingsStatusFix.DOWNLOADERS,
            "no_downloader" to SettingsStatusFix.DOWNLOADERS,
            "same_downloader" to SettingsStatusFix.DOWNLOADERS,
            "people_check" to SettingsStatusFix.MEDIA_SERVERS,
        )

    private val checkTitle =
        mapOf(
            "library_folder" to (SettingsStatusTitle.FOLDER to "Folder"),
            "disk_space" to (SettingsStatusTitle.FREE_SPACE to "Free space"),
            "backups" to (SettingsStatusTitle.BACKUPS to "Backups"),
            "update" to (SettingsStatusTitle.UPDATE to "Update"),
            "config_disk" to (SettingsStatusTitle.CONFIG_DISK to "Config folder space"),
            "config_folder" to (SettingsStatusTitle.CONFIG_FOLDER to "Config folder mapping"),
            "waiting_downloads" to (SettingsStatusTitle.WAITING_DOWNLOADS to "Downloads waiting for files"),
            "clear_waiting" to (SettingsStatusTitle.CLEAR_WAITING to "Imported downloads still in a Downloader"),
            "save_folder" to (SettingsStatusTitle.SAVE_FOLDER to "Save folder for"),
            "same_downloader" to (SettingsStatusTitle.SAME_DOWNLOADER to "Added twice:"),
            "people_check" to (SettingsStatusTitle.PEOPLE_CHECK to "People check"),
        )

    private val downloaderChecks = setOf("save_folder", "no_downloader", "same_downloader")

    fun realTime(text: String): String = if (text.isBlank() || text.startsWith("0001-")) "" else text

    fun linkId(key: String): Long? = key.substringAfterLast(':').toLongOrNull()?.takeIf { it > 0 }

    fun linkTone(
        state: String,
        stuck: Boolean,
    ): Pair<Tone, SettingsStatusWord> =
        when {
            stuck -> Tone.FAIL to SettingsStatusWord.STUCK
            state == "running" -> Tone.OK to SettingsStatusWord.WORKING
            state == "starting" -> Tone.IDLE to SettingsStatusWord.STARTING
            state == "retrying" -> Tone.WAIT to SettingsStatusWord.RETRYING
            state == "failed" -> Tone.FAIL to SettingsStatusWord.BROKEN
            else -> Tone.IDLE to SettingsStatusWord.OFF
        }

    fun lastRan(s: SettingsScheduleRow): String {
        val a = realTime(s.lastRun)
        val b = realTime(s.lastJobEnded)
        if (a.isEmpty() || b.isEmpty()) return a.ifEmpty { b }
        val ma = epochMillisOf(a)
        val mb = epochMillisOf(b)
        return if (ma != null && mb != null && mb > ma) b else a
    }

    fun lastSeen(s: SettingsScheduleRow): SettingsStatusSeen {
        val ran = lastRan(s)
        val checked = realTime(s.lastChecked)
        if (checked.isNotEmpty()) {
            val mc = epochMillisOf(checked)
            val mr = epochMillisOf(ran)
            if (ran.isEmpty() || (mc != null && mr != null && mc > mr)) {
                return SettingsStatusSeen(checked, true, s.lastSkip.ifEmpty { "nothing" })
            }
        }
        if (ran.isEmpty()) return SettingsStatusSeen()
        val byJob = ran == realTime(s.lastJobEnded)
        return SettingsStatusSeen(ran, false, if (byJob) s.lastJobNote else "")
    }

    fun jobTone(
        s: SettingsScheduleRow,
        nowMillis: Long,
    ): SettingsStatusJobTone {
        if (!s.enabled) return SettingsStatusJobTone(Tone.IDLE, SettingsStatusWord.TURNED_OFF)
        if (s.state == "part_off") return SettingsStatusJobTone(Tone.IDLE, SettingsStatusWord.PART_OFF)
        if (s.state == "safe_mode") return SettingsStatusJobTone(Tone.IDLE, SettingsStatusWord.SAFE_MODE)
        if (s.state == "running") return SettingsStatusJobTone(Tone.OK, SettingsStatusWord.RUNNING)
        if (s.lastJob == "failed") {
            val text = s.lastJobError.isNotBlank()
            return SettingsStatusJobTone(
                Tone.FAIL,
                SettingsStatusWord.FAILED,
                if (text) SettingsStatusReason.TEXT else SettingsStatusReason.JOB_FAILED,
                s.lastJobError,
            )
        }
        val next = epochMillisOf(s.nextRun)
        if (s.state == "waiting" && next != null && nowMillis - next > LATE_MILLIS) {
            return SettingsStatusJobTone(Tone.WAIT, SettingsStatusWord.LATE, SettingsStatusReason.JOB_LATE)
        }
        val seen = lastSeen(s)
        if (seen.at.isEmpty()) return SettingsStatusJobTone(Tone.IDLE, SettingsStatusWord.NOT_RUN)
        if (s.lastJob == "cancelled" && !seen.checked) return SettingsStatusJobTone(Tone.IDLE, SettingsStatusWord.CANCELLED)
        return SettingsStatusJobTone(Tone.OK, SettingsStatusWord.OK)
    }

    private fun titleOf(c: HealthCheck): Pair<SettingsStatusTitle, String> {
        if (c.kind == "no_downloader") {
            return (if (c.subject == "usenet") SettingsStatusTitle.USENET_DOWNLOADER else SettingsStatusTitle.TORRENT_DOWNLOADER) to ""
        }
        val (title, label) = checkTitle.getValue(c.kind)
        val bare =
            c.subject.isBlank() ||
                c.subject == label ||
                c.kind == "waiting_downloads" ||
                c.kind == "clear_waiting" ||
                c.kind.startsWith("config_")
        return when {
            bare -> title to ""
            c.kind == "update" -> SettingsStatusTitle.PLAIN to c.subject
            else -> title to c.subject
        }
    }

    private fun checkReason(c: HealthCheck): Pair<SettingsStatusReason, String> {
        val bad = c.state != "ok"
        return when {
            c.kind == "config_folder" && bad -> SettingsStatusReason.CONFIG_STEPS to ""
            c.kind == "save_folder" && bad -> SettingsStatusReason.SAVE_FOLDER to c.subject
            c.kind == "no_downloader" && bad ->
                (if (c.subject == "usenet") SettingsStatusReason.NO_USENET_DOWNLOADER else SettingsStatusReason.NO_TORRENT_DOWNLOADER) to ""
            c.kind == "same_downloader" && bad -> SettingsStatusReason.SAME_DOWNLOADER to c.subject
            c.key == "backups:key" && !bad && c.message.isNotEmpty() ->
                (
                    if (c.message.contains(
                            "owner",
                            ignoreCase = true,
                        )
                    ) {
                        SettingsStatusReason.KEY_KIT_OWNER
                    } else {
                        SettingsStatusReason.KEY_KIT
                    }
                ) to
                    ""
            c.message.isNotEmpty() -> SettingsStatusReason.TEXT to c.message
            else -> SettingsStatusReason.NONE to ""
        }
    }

    fun build(input: SettingsStatusInput): List<SettingsStatusRow> {
        val rows = mutableListOf<SettingsStatusRow>()
        for (s in input.states) {
            val (group, fix) = linkGroup[s.kind] ?: continue
            val id = linkId(s.key)
            val check =
                if (id != null && (s.kind == "source" || s.kind == "downloader")) {
                    SettingsStatusCheck(if (s.kind == "source") SettingsStatusCheckKind.SOURCE else SettingsStatusCheckKind.DOWNLOADER, id)
                } else {
                    null
                }
            var (tone, word) = linkTone(s.state, s.stuck)
            var reasonKind = if (s.reason.isEmpty()) SettingsStatusReason.NONE else SettingsStatusReason.TEXT
            var reason = s.reason
            if (s.kind == "downloader" && tone == Tone.OK && id != null) {
                val warn = input.checks.firstOrNull { it.kind == "downloader" && it.state != "ok" && linkId(it.key) == id }
                if (warn != null) {
                    tone = Tone.WAIT
                    word = SettingsStatusWord.WARNING
                    reason = warn.message
                    reasonKind = if (reason.isEmpty()) SettingsStatusReason.NONE else SettingsStatusReason.TEXT
                }
            }
            rows +=
                SettingsStatusRow(
                    group,
                    s.key,
                    SettingsStatusTitle.PLAIN,
                    s.name,
                    tone,
                    word,
                    reasonKind,
                    reason,
                    realTime(s.since),
                    fix,
                    check,
                )
        }
        for (c in input.checks) {
            val fix = checkFix[c.kind] ?: continue
            if (c.state == "ok" && c.kind in downloaderChecks) continue
            val tone =
                when (c.state) {
                    "ok" -> Tone.OK
                    "warning" -> Tone.WAIT
                    else -> Tone.FAIL
                }
            val word =
                when (tone) {
                    Tone.OK -> SettingsStatusWord.WORKING
                    Tone.WAIT -> SettingsStatusWord.WARNING
                    else -> SettingsStatusWord.BROKEN
                }
            val (title, name) = titleOf(c)
            val (reasonKind, reason) = checkReason(c)
            val group =
                when {
                    c.kind == "update" -> SettingsStatusGroup.APP
                    c.kind in downloaderChecks -> SettingsStatusGroup.DOWNLOADERS
                    else -> SettingsStatusGroup.LIBRARY
                }
            rows +=
                SettingsStatusRow(
                    group,
                    c.key,
                    title,
                    name,
                    tone,
                    word,
                    reasonKind,
                    reason,
                    realTime(c.checkedAt).ifEmpty { realTime(c.since) },
                    fix,
                    SettingsStatusCheck(SettingsStatusCheckKind.CHECKS),
                )
        }
        for (l in input.lists) {
            val key = "list:${l.id}"
            val at = realTime(l.lastSyncedAt)
            val fix = SettingsStatusFix.OUTSIDE_LISTS
            rows +=
                when {
                    !l.enabled ->
                        SettingsStatusRow(
                            SettingsStatusGroup.LISTS,
                            key,
                            name = l.name,
                            at = at,
                            fix = fix,
                            word = SettingsStatusWord.OFF,
                        )
                    l.lastError.isNotEmpty() ->
                        SettingsStatusRow(
                            SettingsStatusGroup.LISTS,
                            key,
                            name = l.name,
                            tone = Tone.FAIL,
                            word = SettingsStatusWord.BROKEN,
                            reasonKind = SettingsStatusReason.TEXT,
                            reason = l.lastError,
                            at = at,
                            fix = fix,
                        )
                    at.isEmpty() ->
                        SettingsStatusRow(SettingsStatusGroup.LISTS, key, name = l.name, word = SettingsStatusWord.NOT_SYNCED, fix = fix)
                    else ->
                        SettingsStatusRow(
                            SettingsStatusGroup.LISTS,
                            key,
                            name = l.name,
                            tone = Tone.OK,
                            word = SettingsStatusWord.WORKING,
                            reasonKind = if (l.lastResult.isEmpty()) SettingsStatusReason.NONE else SettingsStatusReason.TEXT,
                            reason = l.lastResult,
                            at = at,
                            fix = fix,
                        )
                }
        }
        for (a in input.alerts) {
            val key = "alert:${a.id}"
            val at = realTime(a.lastSentAt)
            val fix = SettingsStatusFix.ALERTS
            rows +=
                when {
                    !a.enabled ->
                        SettingsStatusRow(
                            SettingsStatusGroup.ALERTS,
                            key,
                            name = a.name,
                            at = at,
                            fix = fix,
                            word = SettingsStatusWord.OFF,
                        )
                    a.lastError.isNotEmpty() ->
                        SettingsStatusRow(
                            SettingsStatusGroup.ALERTS,
                            key,
                            name = a.name,
                            tone = Tone.FAIL,
                            word = SettingsStatusWord.BROKEN,
                            reasonKind = SettingsStatusReason.TEXT,
                            reason = a.lastError,
                            at = at,
                            fix = fix,
                        )
                    at.isEmpty() ->
                        SettingsStatusRow(SettingsStatusGroup.ALERTS, key, name = a.name, word = SettingsStatusWord.NOT_SENT, fix = fix)
                    else ->
                        SettingsStatusRow(
                            SettingsStatusGroup.ALERTS,
                            key,
                            name = a.name,
                            tone = Tone.OK,
                            word = SettingsStatusWord.WORKING,
                            at = at,
                            fix = fix,
                        )
                }
        }
        input.metadataUp?.let { up ->
            rows +=
                SettingsStatusRow(
                    SettingsStatusGroup.METADATA,
                    "metadata",
                    SettingsStatusTitle.METADATA,
                    tone = if (up) Tone.OK else Tone.FAIL,
                    word = if (up) SettingsStatusWord.WORKING else SettingsStatusWord.BROKEN,
                    reasonKind = if (up) SettingsStatusReason.NONE else SettingsStatusReason.METADATA_DOWN,
                    at = input.metadataAt,
                )
        }
        for (s in input.schedules) {
            val job = jobTone(s, input.nowMillis)
            rows +=
                SettingsStatusRow(
                    SettingsStatusGroup.JOBS,
                    "job:${s.kind}",
                    SettingsStatusTitle.PLAIN,
                    s.label,
                    job.tone,
                    job.word,
                    job.reasonKind,
                    job.reason,
                    realTime(s.lastJobEnded).ifEmpty { realTime(s.lastRun) },
                    SettingsStatusFix.SCHEDULES,
                    SettingsStatusCheck(SettingsStatusCheckKind.JOB, job = s.kind),
                    lastSeen(s).at,
                    realTime(s.nextRun),
                    s.lastJob,
                )
        }
        return rows
    }

    fun needsAttention(rows: List<SettingsStatusRow>): List<SettingsStatusRow> =
        rows.filter { it.tone == Tone.FAIL } + rows.filter { it.tone == Tone.WAIT }

    fun nothingLinked(rows: List<SettingsStatusRow>): Boolean =
        rows.none { it.group == SettingsStatusGroup.SOURCES || it.group == SettingsStatusGroup.DOWNLOADERS }

    fun headline(
        rows: List<SettingsStatusRow>,
        linkedKnown: Boolean,
    ): SettingsStatusHeadline {
        val bad = needsAttention(rows)
        if (bad.isEmpty()) {
            val kind =
                when {
                    !linkedKnown -> SettingsStatusHeadlineKind.UNKNOWN
                    nothingLinked(rows) -> SettingsStatusHeadlineKind.NOTHING_LINKED
                    else -> SettingsStatusHeadlineKind.FINE
                }
            return SettingsStatusHeadline(kind)
        }
        val checks = bad.count { it.group != SettingsStatusGroup.JOBS }
        return SettingsStatusHeadline(SettingsStatusHeadlineKind.ATTENTION, checks, bad.size - checks)
    }

    private fun rank(row: SettingsStatusRow): Int =
        when (row.tone) {
            Tone.FAIL -> 0
            Tone.WAIT -> 1
            else -> 2
        }

    fun kind(
        group: SettingsStatusGroup,
        rows: List<SettingsStatusRow>,
    ): SettingsStatusKind {
        val ordered = rows.sortedBy { rank(it) }
        val ok = rows.count { it.tone == Tone.OK }
        val on = rows.count { !it.isOff }
        val marker =
            when {
                rows.any { it.tone == Tone.FAIL } -> SettingsStatusMarker.FAIL
                rows.any { it.tone == Tone.WAIT } -> SettingsStatusMarker.WAIT
                rows.isEmpty() -> SettingsStatusMarker.NONE
                rows.all { it.tone == Tone.IDLE } -> SettingsStatusMarker.IDLE
                else -> SettingsStatusMarker.OK
            }
        val bad = rows.count { it.needsAttention }
        var summary = SettingsStatusSummaryKind.NOTHING
        var count = 0
        var word: SettingsStatusWord? = null
        when {
            rows.isEmpty() -> Unit
            bad > 0 -> {
                summary = SettingsStatusSummaryKind.NEEDS
                count = bad
            }
            rows.size == 1 -> {
                summary = SettingsStatusSummaryKind.WORD
                word = rows.first().word
            }
            rows.all { it.isOff } -> {
                summary = SettingsStatusSummaryKind.OFF
                count = rows.size
            }
            ok > 0 -> {
                summary = SettingsStatusSummaryKind.WORKING
                count = ok
            }
            else -> {
                summary = SettingsStatusSummaryKind.WAITING
                count = on
            }
        }
        return SettingsStatusKind(group, ordered, ok, on, marker, summary, count, word, kindFix.getValue(group))
    }

    fun kinds(
        rows: List<SettingsStatusRow>,
        hidden: Set<SettingsStatusGroup>,
    ): List<SettingsStatusKind> = kindOrder.filter { it !in hidden }.map { group -> kind(group, rows.filter { it.group == group }) }

    fun defaultKind(kinds: List<SettingsStatusKind>): SettingsStatusGroup =
        (
            kinds.firstOrNull { it.marker == SettingsStatusMarker.FAIL }
                ?: kinds.firstOrNull { it.marker == SettingsStatusMarker.WAIT }
                ?: kinds.firstOrNull()
        )?.group ?: SettingsStatusGroup.SOURCES

    fun diskKind(kind: String): SettingsStatusDiskKind =
        when (kind) {
            "movie" -> SettingsStatusDiskKind.MOVIES
            "show" -> SettingsStatusDiskKind.SHOWS
            "music" -> SettingsStatusDiskKind.MUSIC
            else -> SettingsStatusDiskKind.OTHER
        }

    fun usedPercent(
        free: Long,
        total: Long,
    ): Int = if (total > 0 && free >= 0) (((total - free).toDouble() / total) * PERCENT).roundToInt() else -1

    fun diskLevel(percent: Int): SettingsStatusDiskLevel =
        when {
            percent >= FULL_PERCENT -> SettingsStatusDiskLevel.FULL
            percent >= WARN_PERCENT -> SettingsStatusDiskLevel.WARN
            else -> SettingsStatusDiskLevel.NORMAL
        }

    private const val WARN_PERCENT = 85
    private const val FULL_PERCENT = 95

    fun until(
        iso: String,
        nowMillis: Long,
    ): SettingsStatusDue {
        val at = epochMillisOf(iso) ?: return SettingsStatusDue()
        val seconds = ((at - nowMillis).toDouble() / SECOND).roundToLong()
        return when {
            seconds <= IMMINENT_SECONDS -> SettingsStatusDue(SettingsStatusDueKind.NOW)
            seconds < HOUR_SECONDS -> due(SettingsStatusDueKind.IN, SettingsStatusUnit.MINUTES, seconds, MINUTE_SECONDS)
            seconds < DAY_SECONDS -> due(SettingsStatusDueKind.IN, SettingsStatusUnit.HOURS, seconds, HOUR_SECONDS)
            else -> due(SettingsStatusDueKind.IN, SettingsStatusUnit.DAYS, seconds, DAY_SECONDS)
        }
    }

    fun nextRun(
        iso: String,
        nowMillis: Long,
    ): SettingsStatusDue {
        val at = epochMillisOf(iso) ?: return SettingsStatusDue()
        val late = ((nowMillis - at).toDouble() / SECOND).roundToLong()
        return when {
            late <= LATE_GRACE_SECONDS -> until(iso, nowMillis)
            late < HOUR_SECONDS -> due(SettingsStatusDueKind.OVERDUE, SettingsStatusUnit.MINUTES, late, MINUTE_SECONDS)
            late < DAY_SECONDS -> due(SettingsStatusDueKind.OVERDUE, SettingsStatusUnit.HOURS, late, HOUR_SECONDS)
            else -> due(SettingsStatusDueKind.OVERDUE, SettingsStatusUnit.DAYS, late, DAY_SECONDS)
        }
    }

    private fun due(
        kind: SettingsStatusDueKind,
        unit: SettingsStatusUnit,
        seconds: Long,
        size: Long,
    ): SettingsStatusDue = SettingsStatusDue(kind, unit, (seconds.toDouble() / size).roundToInt())

    fun nextWord(
        row: SettingsStatusRow,
        nowMillis: Long,
    ): SettingsStatusDue {
        if (row.nextRun.isEmpty() || row.stopped || epochMillisOf(row.nextRun) == null) return SettingsStatusDue()
        return if (row.word == SettingsStatusWord.RUNNING) until(row.nextRun, nowMillis) else nextRun(row.nextRun, nowMillis)
    }

    fun upcoming(
        jobs: List<SettingsStatusRow>,
        running: List<SettingsStatusRunning>,
        nowMillis: Long,
    ): List<SettingsStatusUpcoming> {
        val busy = running.filterNot { it.queued }.map { it.kind }.toSet()
        return jobs
            .filter { it.nextRun.isNotEmpty() && !it.stopped && epochMillisOf(it.nextRun) != null }
            .sortedBy { epochMillisOf(it.nextRun) ?: Long.MAX_VALUE }
            .filter { it.check?.job !in busy }
            .take(NEXT_UP)
            .map { row ->
                val due =
                    if (row.word ==
                        SettingsStatusWord.RUNNING
                    ) {
                        SettingsStatusDue(SettingsStatusDueKind.NOW)
                    } else {
                        nextRun(row.nextRun, nowMillis)
                    }
                SettingsStatusUpcoming(row.key, row.name, row.tone, row.word, due)
            }
    }

    fun jobName(
        kind: String,
        names: Map<String, String>,
    ): String = names[kind] ?: kind.replace('_', ' ')

    fun finished(
        history: List<HistoryRun>,
        names: Map<String, String>,
    ): List<SettingsStatusFinished> =
        history
            .filter { it.endedAt.isNotEmpty() && it.state != "running" && it.state != "queued" && epochMillisOf(it.endedAt) != null }
            .sortedByDescending { epochMillisOf(it.endedAt) ?: 0L }
            .take(RECENT)
            .map { run ->
                val (tone, word) =
                    when (run.state) {
                        "done" -> Tone.OK to SettingsStatusWord.OK
                        "failed" -> Tone.FAIL to SettingsStatusWord.FAILED
                        else -> Tone.IDLE to SettingsStatusWord.CANCELLED
                    }
                SettingsStatusFinished(run.id, jobName(run.kind, names), tone, word, run.endedAt)
            }

    fun running(active: List<SettingsStatusActiveJob>): List<SettingsStatusRunning> =
        active
            .map {
                SettingsStatusRunning(
                    it.id,
                    it.kind,
                    it.label.ifEmpty { it.kind.replace('_', ' ') },
                    it.state == "queued",
                    it.progress,
                    it.note,
                    realTime(it.startedAt),
                )
            }.sortedBy { if (it.queued) 1 else 0 }

    fun shortPath(text: String): String {
        if (!text.contains('/') && !text.contains('\\')) return text
        val parts = text.split('/', '\\').filter { it.isNotEmpty() }
        return if (parts.size > 2) "…/" + parts.takeLast(2).joinToString("/") else text
    }
}

internal data class HistoryRun(
    val id: Long,
    val kind: String,
    val state: String,
    val endedAt: String,
)

fun settingsStatusUptime(seconds: Long): String {
    val total = seconds.coerceAtLeast(0)
    val days = total / 86400
    val hours = (total / 3600) % 24
    val minutes = (total / 60) % 60
    val rest = total % 60
    return "${days}d ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${rest.toString().padStart(2, '0')}"
}
