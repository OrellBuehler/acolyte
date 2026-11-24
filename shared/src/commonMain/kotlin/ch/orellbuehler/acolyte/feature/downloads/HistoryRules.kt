package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.model.EventRecord
import ch.orellbuehler.acolyte.feature.Tone
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

enum class HistoryStep(
    internal val kinds: List<String>,
) {
    SENT(listOf("release_chosen", "release_chosen_by_hand", "download_sent", "download_queued")),
    DOWNLOADED(listOf("download_finished", "download_seeding")),
    IMPORTED(listOf("import_done", "import_partly_done", "adopted", "media_server_told")),
    UPGRADED(listOf("file_replaced")),
    RENAMED(listOf("renamed", "rename_planned")),
    DELETED(listOf("title_removed", "episodes_removed", "file_held", "holding_cleaned")),
    DECLINED(listOf("ask_turned_down", "release_turned_down", "ai_ask_turned_down", "subtitle_turned_down")),
    FAILED(
        listOf(
            "download_failed",
            "download_vanished",
            "download_not_removed",
            "download_not_moved",
            "import_failed",
            "import_rejected",
            "file_hold_failed",
            "free_space_low",
            "health_broken",
            "library_folder_*",
            "file_missing",
            "search_failed",
            "alert_failed",
            "media_server_failed",
            "subtitle_site_paused",
            "subtitle_failed",
            "subtitle_translate_failed",
            "ai_action_failed",
        ),
    ),
    ASKED(listOf("ask_made", "ai_ask_made")),
    APPROVED(listOf("ask_approved", "ai_ask_approved")),
    ADDED(listOf("title_added", "list_synced", "version_changed", "gate_opened")),
    SUBTITLES(listOf("subtitle_saved", "subtitle_upgraded", "subtitle_synced", "subtitle_translated", "subtitle_cleaned")),
    REPORTED(listOf("report_*")),
    DECIDE(listOf("needs_decision", "stopped_looking")),
    SYSTEM(listOf("sign*", "api_key_*", "settings_changed", "secrets_imported", "account_changed", "backup_used", "health_recovered")),
}

enum class HistoryType {
    ALL,
    MOVIES,
    SHOWS,
    MUSIC,
}

enum class HistoryWhoFilter {
    ALL,
    ME,
    PEOPLE,
    AI,
    HOLYARRS,
}

enum class HistoryWhen {
    ALL,
    DAY,
    WEEK,
    MONTH,
    CUSTOM,
}

enum class HistoryVerb(
    val tone: Tone,
) {
    IMPORTED(Tone.OK),
    PARTLY_IMPORTED(Tone.WAIT),
    UPGRADED(Tone.INFO),
    LOW_ON_SPACE(Tone.FAIL),
    NEEDS_DECISION(Tone.WAIT),
    NEEDS_YOU(Tone.WAIT),
    STOPPED_LOOKING(Tone.IDLE),
    FIXED(Tone.OK),
    ASKED(Tone.INFO),
    APPROVED(Tone.OK),
    TURNED_DOWN(Tone.IDLE),
    FAILED(Tone.FAIL),
    REPORTED(Tone.WAIT),
    REOPENED(Tone.WAIT),
    DOWNLOADED(Tone.OK),
    STALLED(Tone.WAIT),
    PAUSED(Tone.IDLE),
    QUEUED(Tone.IDLE),
    FILE_MISSING(Tone.FAIL),
    FILE_BACK(Tone.OK),
    REFRESHED(Tone.IDLE),
    ALBUMS_CHANGED(Tone.IDLE),
    OUT_NOW(Tone.INFO),
    CHANGED(Tone.IDLE),
    SETTINGS_IMPORTED(Tone.IDLE),
    ADOPTED(Tone.OK),
    ADDED(Tone.OK),
    SEARCHED(Tone.IDLE),
    SEARCH_FAILED(Tone.FAIL),
    HELD(Tone.WAIT),
    CHOSEN(Tone.INFO),
    SENT(Tone.INFO),
    DOWNLOADING(Tone.INFO),
    RENAMED(Tone.IDLE),
    DELETED(Tone.IDLE),
    SUBTITLES(Tone.OK),
    SUBTITLES_WAIT(Tone.WAIT),
    SCANNED(Tone.IDLE),
    HEALTH(Tone.FAIL),
    REQUESTS(Tone.INFO),
    BY_AI(Tone.INFO),
    NOT_DONE(Tone.FAIL),
    ALERT_FAILED(Tone.FAIL),
    MEDIA_SERVER(Tone.IDLE),
    MEDIA_SERVER_FAILED(Tone.FAIL),
    SYSTEM(Tone.IDLE),
    OTHER(Tone.IDLE),
}

enum class StepState {
    DONE,
    FAILED,
    WAITING,
    NEEDS,
}

enum class DownloadStepKey {
    SENT,
    DOWNLOADED,
    IMPORTED,
}

data class DownloadStep(
    val key: DownloadStepKey,
    val state: StepState,
)

enum class LineKind {
    SUMMARY,
    REASON,
    ADDED_LIST,
    ADDED_IMPORT,
    ADDED_BY,
    ADDED_ASK,
    REMOVED_KEPT,
    REMOVED_DELETED,
    RENAMED,
    RENAME_READY,
    FEEDS,
    LOOKED,
    SEARCH_FAILED,
    TURNED_DOWN,
    UNBLOCKED,
    HELD_BACK,
    HELD_GONE,
    WAITING,
    DOWNLOADING,
    DOWNLOADING_PCT,
    STALLED,
    PAUSED,
    PAUSED_AT,
    SHARING,
    MOVED_OUT,
    REMOVED_OUTSIDE,
    REMOVED_DL,
    REMOVED_DL_FILES,
    DL_FAILED,
    NOT_REMOVED,
    NOT_MOVED,
    PARTLY,
    NOTHING_PLACED,
    BETTER,
    FILE_BACK,
    FILE_HELD,
    HOLD_FAILED,
    HOLDING_CLEANED,
    LOW_SPACE,
    STOPPED_LOOKING,
    WORKING_AGAIN,
    MEDIA_TOLD,
    MEDIA_FAILED,
    ALERT_FAILED,
    OUT_NOW,
    REPORT,
    CHECKED_DETAILS,
}

data class HistoryLine(
    val kind: LineKind,
    val text: String = "",
    val number: Int = 0,
)

enum class WhoKind {
    PERSON,
    A_PERSON,
    AI,
    JOB,
    HOLYARRS,
}

data class HistoryWho(
    val kind: WhoKind,
    val name: String = "",
    val forName: String = "",
)

enum class HistoryFactKind {
    QUALITY,
    SIZE,
    SOURCE,
    DOWNLOADER,
    RELEASE,
    FILE,
    FROM,
    FOLDER,
    WHY,
    ALBUM,
}

data class HistoryFact(
    val kind: HistoryFactKind,
    val text: String,
)

enum class HistoryRedo {
    FIND_ANOTHER,
    REPLACE,
}

data class HistoryItem(
    val key: String,
    val head: EventRecord,
    val events: List<EventRecord>,
    val steps: List<DownloadStep>,
    val verb: HistoryVerb,
    val title: String,
    val episode: String,
    val version: String,
    val line: HistoryLine,
    val extra: String,
    val who: HistoryWho,
    val poster: String?,
    val square: Boolean,
    val facts: List<HistoryFact>,
    val redo: HistoryRedo?,
    val releaseId: Long,
    val downloadId: Long,
    val failed: Boolean,
    val needsYou: Boolean,
)

data class HistoryDay(
    val day: Day,
    val items: List<HistoryItem>,
    val failed: Int,
    val needsYou: Int,
)

object HistoryRules {
    private val NON_ROUTINE =
        listOf(
            "import_done",
            "import_partly_done",
            "file_replaced",
            "adopted",
            "download_failed",
            "download_vanished",
            "download_not_removed",
            "download_not_moved",
            "import_failed",
            "import_rejected",
            "file_hold_failed",
            "free_space_low",
            "needs_decision",
            "stopped_looking",
            "health_broken",
            "library_folder_*",
            "file_missing",
            "search_failed",
            "alert_failed",
            "media_server_failed",
            "subtitle_site_paused",
            "subtitle_failed",
            "subtitle_translate_failed",
            "ai_action_failed",
            "title_added",
            "list_synced",
            "version_changed",
            "gate_opened",
            "release_chosen",
            "release_chosen_by_hand",
            "release_turned_down",
            "turned_down_removed",
            "release_held",
            "held_removed",
            "download_sent",
            "download_queued",
            "download_downloading",
            "download_stalled",
            "download_paused",
            "download_finished",
            "download_seeding",
            "download_removed",
            "download_changed",
            "renamed",
            "rename_planned",
            "title_removed",
            "episodes_removed",
            "file_held",
            "holding_cleaned",
            "subtitle_*",
            "ask_*",
            "ai_ask_*",
            "report_*",
            "sign*",
            "api_key_*",
            "settings_changed",
            "secrets_imported",
            "account_changed",
            "backup_used",
            "health_recovered",
            "search_done:noted",
            "feed_checked:noted",
        )

    private val GROUPS: List<Pair<String, List<String>>> =
        listOf(
            "add" to listOf("title_added", "adopted", "list_synced", "version_changed"),
            "search" to listOf("search_done", "search_failed", "feed_checked"),
            "hold" to listOf("release_held", "held_removed"),
            "choose" to listOf("release_chosen", "release_chosen_by_hand", "release_turned_down", "turned_down_removed"),
            "grab" to listOf("download_sent"),
            "download" to
                listOf(
                    "download_downloading",
                    "download_queued",
                    "download_stalled",
                    "download_paused",
                    "download_finished",
                    "download_seeding",
                    "download_changed",
                ),
            "fail" to listOf("download_failed", "download_vanished", "import_failed", "import_rejected", "file_hold_failed"),
            "import" to listOf("import_done", "import_partly_done", "file_replaced", "free_space_low"),
            "rename" to listOf("renamed", "rename_planned"),
            "delete" to
                listOf("title_removed", "episodes_removed", "file_held", "holding_cleaned", "download_removed", "download_not_removed"),
            "subtitles" to listOf("subtitle_*"),
            "scan" to
                listOf(
                    "library_scanned",
                    "library_rescanned",
                    "folder_scanned",
                    "file_missing",
                    "file_back",
                    "library_folder_*",
                    "title_refreshed",
                    "episodes_changed",
                    "details_source_changed",
                ),
            "decide" to listOf("needs_decision", "stopped_looking"),
            "health" to listOf("health_*"),
            "wishlist" to listOf("ask_*"),
            "reports" to listOf("report_*"),
            "links" to listOf("alert_failed", "media_server_*"),
            "ai" to listOf("ai_*"),
            "system" to listOf("sign*", "api_key_*", "settings_changed"),
        )

    private val DOWNLOAD_GROUPS = setOf("grab", "download", "fail", "import")
    private val REDO = Regex("^(release_chosen|download_|import_|file_replaced)")

    fun kinds(steps: Set<HistoryStep>): List<String> =
        if (steps.isEmpty()) {
            NON_ROUTINE
        } else {
            HistoryStep.entries
                .filter {
                    it in steps
                }.flatMap { it.kinds }
        }

    fun groupOf(kind: String): String {
        for ((key, kinds) in GROUPS) {
            for (k in kinds) if (if (k.endsWith("*")) kind.startsWith(k.dropLast(1)) else k == kind) return key
        }
        return ""
    }

    private fun exactGroup(kind: String): String = GROUPS.firstOrNull { (_, kinds) -> kind in kinds }?.first.orEmpty()

    fun verbOf(kind: String): HistoryVerb =
        when (kind) {
            "import_done" -> HistoryVerb.IMPORTED
            "import_partly_done" -> HistoryVerb.PARTLY_IMPORTED
            "file_replaced" -> HistoryVerb.UPGRADED
            "free_space_low" -> HistoryVerb.LOW_ON_SPACE
            "needs_decision" -> HistoryVerb.NEEDS_DECISION
            "stopped_looking" -> HistoryVerb.STOPPED_LOOKING
            "health_recovered", "report_fixed" -> HistoryVerb.FIXED
            "ask_made" -> HistoryVerb.ASKED
            "ask_approved" -> HistoryVerb.APPROVED
            "ask_turned_down", "release_turned_down" -> HistoryVerb.TURNED_DOWN
            "ask_not_added", "download_not_removed" -> HistoryVerb.FAILED
            "report_made" -> HistoryVerb.REPORTED
            "report_reopened" -> HistoryVerb.REOPENED
            "download_finished" -> HistoryVerb.DOWNLOADED
            "download_stalled" -> HistoryVerb.STALLED
            "download_paused" -> HistoryVerb.PAUSED
            "download_queued" -> HistoryVerb.QUEUED
            "file_missing" -> HistoryVerb.FILE_MISSING
            "file_back" -> HistoryVerb.FILE_BACK
            "title_refreshed" -> HistoryVerb.REFRESHED
            "albums_changed", "album_changed" -> HistoryVerb.ALBUMS_CHANGED
            "gate_opened" -> HistoryVerb.OUT_NOW
            "title_edited", "season_changed" -> HistoryVerb.CHANGED
            "secrets_imported" -> HistoryVerb.SETTINGS_IMPORTED
            "adopted" -> HistoryVerb.ADOPTED
            else ->
                when (groupOf(kind)) {
                    "add" -> HistoryVerb.ADDED
                    "search" -> if (kind == "search_failed") HistoryVerb.SEARCH_FAILED else HistoryVerb.SEARCHED
                    "hold" -> HistoryVerb.HELD
                    "choose" -> HistoryVerb.CHOSEN
                    "grab" -> HistoryVerb.SENT
                    "download" -> HistoryVerb.DOWNLOADING
                    "fail" -> HistoryVerb.FAILED
                    "import" -> HistoryVerb.IMPORTED
                    "rename" -> HistoryVerb.RENAMED
                    "delete" -> HistoryVerb.DELETED
                    "subtitles" -> if (Regex("failed|none").containsMatchIn(kind)) HistoryVerb.SUBTITLES_WAIT else HistoryVerb.SUBTITLES
                    "scan" -> HistoryVerb.SCANNED
                    "decide" -> HistoryVerb.NEEDS_DECISION
                    "health" -> HistoryVerb.HEALTH
                    "wishlist" -> HistoryVerb.REQUESTS
                    "ai" -> if (kind == "ai_action_failed") HistoryVerb.NOT_DONE else HistoryVerb.BY_AI
                    "links" ->
                        when {
                            kind == "alert_failed" -> HistoryVerb.ALERT_FAILED
                            Regex("failed|unreachable").containsMatchIn(kind) -> HistoryVerb.MEDIA_SERVER_FAILED
                            else -> HistoryVerb.MEDIA_SERVER
                        }
                    "system" -> HistoryVerb.SYSTEM
                    else -> HistoryVerb.OTHER
                }
        }

    fun isDownloadStep(e: EventRecord): Boolean = e.downloadId > 0 && exactGroup(e.kind) in DOWNLOAD_GROUPS

    fun steps(events: List<EventRecord>): List<DownloadStep> {
        val kinds = events.map { it.kind }.toSet()

        fun has(vararg k: String) = k.any { it in kinds }
        val downloadFailed = has("download_failed", "download_vanished")
        val importFailed = has("import_failed", "import_rejected", "file_hold_failed", "free_space_low")
        val imported = has("import_done", "import_partly_done", "file_replaced")
        val downloaded = imported || importFailed || has("download_finished", "download_seeding")
        val sent = downloaded || downloadFailed || events.any { exactGroup(it.kind) == "grab" || exactGroup(it.kind) == "download" }
        val needs = has("needs_decision") && !imported

        fun state(
            done: Boolean,
            failed: Boolean,
        ) = if (failed) {
            StepState.FAILED
        } else if (done) {
            StepState.DONE
        } else {
            StepState.WAITING
        }
        val importState = if (needs) StepState.NEEDS else state(imported, importFailed && !imported)
        return listOf(
            DownloadStep(DownloadStepKey.SENT, state(sent, false)),
            DownloadStep(DownloadStepKey.DOWNLOADED, state(downloaded, downloadFailed && !downloaded)),
            DownloadStep(DownloadStepKey.IMPORTED, importState),
        )
    }

    fun group(events: List<EventRecord>): List<Pair<String, List<EventRecord>>> {
        val byRelease = mutableMapOf<Long, Long>()
        val stuckSlot = mutableMapOf<Long, Long>()
        for (e in events) {
            if (!isDownloadStep(e)) continue
            val rel = num(e.data, "release_id").toLong()
            if (rel > 0 && rel !in byRelease) byRelease[rel] = e.downloadId
            if (e.slotId > 0 &&
                Regex("^import_(failed|rejected)$").matches(e.kind) &&
                e.slotId !in stuckSlot
            ) {
                stuckSlot[e.slotId] = e.downloadId
            }
        }
        val known = byRelease.values.toMutableSet()
        for (e in events) if (isDownloadStep(e)) known += e.downloadId

        fun downloadOf(e: EventRecord): Long {
            if (isDownloadStep(e)) return e.downloadId
            if (e.kind == "release_chosen" || e.kind == "release_chosen_by_hand") return byRelease[num(e.data, "release_id").toLong()] ?: 0
            if (e.kind == "needs_decision" || e.kind == "stopped_looking") {
                val id = e.downloadId.takeIf { it > 0 } ?: num(e.data, "download_id").toLong()
                if (id > 0 && id in known) return id
                return if (e.kind == "needs_decision" && e.slotId > 0) stuckSlot[e.slotId] ?: 0 else 0
            }
            return 0
        }
        val rows = mutableListOf<Pair<String, MutableList<EventRecord>>>()
        val byDownload = mutableMapOf<Long, MutableList<EventRecord>>()
        for (e in events) {
            val id = downloadOf(e)
            if (id <= 0) {
                rows += "e${e.id}" to mutableListOf(e)
                continue
            }
            val existing = byDownload[id]
            if (existing != null) {
                existing += e
            } else {
                val fresh = mutableListOf(e)
                byDownload[id] = fresh
                rows += "d$id" to fresh
            }
        }
        return rows
    }

    fun dropDuplicateAsks(events: List<EventRecord>): List<EventRecord> {
        val aiAsks =
            events
                .filter { it.kind.startsWith("ai_ask_") }
                .map { num(it.data, "ask_id").toLong() }
                .filter { it > 0 }
                .toSet()
        if (aiAsks.isEmpty()) return events
        return events.filter { e ->
            !(e.kind in setOf("ask_made", "ask_approved", "ask_turned_down") && num(e.data, "ask_id").toLong() in aiAsks)
        }
    }

    fun rowVerb(
        events: List<EventRecord>,
        steps: List<DownloadStep>,
    ): HistoryVerb {
        if (steps.isEmpty()) return verbOf(events.first().kind)
        if (steps.any { it.state == StepState.FAILED }) return HistoryVerb.FAILED
        if (steps.any { it.state == StepState.NEEDS }) return HistoryVerb.NEEDS_YOU
        val kinds = events.map { it.kind }.toSet()
        return when {
            "file_replaced" in kinds -> HistoryVerb.UPGRADED
            steps[2].state == StepState.DONE ->
                if ("import_partly_done" in kinds &&
                    "import_done" !in kinds
                ) {
                    HistoryVerb.PARTLY_IMPORTED
                } else {
                    HistoryVerb.IMPORTED
                }
            steps[1].state == StepState.DONE -> HistoryVerb.DOWNLOADED
            "download_stalled" in kinds -> HistoryVerb.STALLED
            "download_paused" in kinds -> HistoryVerb.PAUSED
            events.any { exactGroup(it.kind) == "download" } -> HistoryVerb.DOWNLOADING
            steps[0].state == StepState.DONE -> HistoryVerb.SENT
            else -> verbOf(events.first().kind)
        }
    }

    fun lineOf(e: EventRecord): HistoryLine {
        val d = e.data
        val reason = plainWhy(str(d, "reason"))
        val pct = (num(d, "progress") * 100).let { kotlin.math.round(it).toInt() }
        return when (e.kind) {
            "gate_opened" -> HistoryLine(LineKind.OUT_NOW)
            "title_added" ->
                when (str(d, "why_added")) {
                    "list" ->
                        if (str(
                                d,
                                "list_name",
                            ).isNotEmpty()
                        ) {
                            HistoryLine(LineKind.ADDED_LIST, str(d, "list_name"))
                        } else {
                            HistoryLine(LineKind.ADDED_LIST)
                        }
                    "import" -> HistoryLine(LineKind.ADDED_IMPORT)
                    "ask" -> HistoryLine(LineKind.ADDED_ASK, e.actorName)
                    else -> if (e.actorName.isNotEmpty()) HistoryLine(LineKind.ADDED_BY, e.actorName) else summary(e)
                }
            "list_synced" -> HistoryLine(LineKind.ADDED_LIST)
            "title_removed" -> HistoryLine(if (bool(d, "delete_files")) LineKind.REMOVED_DELETED else LineKind.REMOVED_KEPT)
            "title_refreshed" -> HistoryLine(LineKind.CHECKED_DETAILS)
            "renamed" -> HistoryLine(LineKind.RENAMED)
            "rename_planned" -> HistoryLine(LineKind.RENAME_READY)
            "search_done" -> HistoryLine(LineKind.LOOKED)
            "search_failed" -> HistoryLine(LineKind.SEARCH_FAILED, plainWhy(str(d, "reason").ifEmpty { str(d, "error") }))
            "feed_checked" -> HistoryLine(LineKind.FEEDS)
            "release_turned_down" -> HistoryLine(LineKind.TURNED_DOWN, reason)
            "turned_down_removed" -> HistoryLine(LineKind.UNBLOCKED)
            "release_held" -> HistoryLine(LineKind.HELD_BACK, reason)
            "held_removed" -> HistoryLine(LineKind.HELD_GONE)
            "download_queued" -> HistoryLine(LineKind.WAITING)
            "download_downloading" ->
                if (pct >
                    0
                ) {
                    HistoryLine(LineKind.DOWNLOADING_PCT, number = pct)
                } else {
                    HistoryLine(LineKind.DOWNLOADING)
                }
            "download_stalled" -> HistoryLine(LineKind.STALLED, reason)
            "download_paused" -> if (pct > 0) HistoryLine(LineKind.PAUSED_AT, number = pct) else HistoryLine(LineKind.PAUSED)
            "download_seeding" -> HistoryLine(LineKind.SHARING)
            "download_removed" ->
                when {
                    bool(d, "moved_out") -> HistoryLine(LineKind.MOVED_OUT)
                    bool(d, "outside") -> HistoryLine(LineKind.REMOVED_OUTSIDE)
                    bool(d, "delete_files") -> HistoryLine(LineKind.REMOVED_DL_FILES)
                    else -> HistoryLine(LineKind.REMOVED_DL)
                }
            "download_failed" -> if (reason.isNotEmpty()) HistoryLine(LineKind.REASON, reason) else HistoryLine(LineKind.DL_FAILED)
            "download_vanished" -> HistoryLine(LineKind.REMOVED_OUTSIDE)
            "download_not_removed" -> HistoryLine(LineKind.NOT_REMOVED)
            "download_not_moved" -> HistoryLine(LineKind.NOT_MOVED)
            "import_partly_done" -> HistoryLine(LineKind.PARTLY)
            "import_failed", "import_rejected" ->
                if (reason.isNotEmpty()) {
                    HistoryLine(
                        LineKind.REASON,
                        reason,
                    )
                } else {
                    HistoryLine(LineKind.NOTHING_PLACED)
                }
            "file_replaced" -> HistoryLine(LineKind.BETTER)
            "file_back" -> HistoryLine(LineKind.FILE_BACK)
            "file_held" -> HistoryLine(LineKind.FILE_HELD)
            "file_hold_failed" -> HistoryLine(LineKind.HOLD_FAILED)
            "holding_cleaned" -> HistoryLine(LineKind.HOLDING_CLEANED)
            "free_space_low" -> HistoryLine(LineKind.LOW_SPACE, plainWhy(str(d, "reason").ifEmpty { str(d, "message") }))
            "needs_decision" -> if (reason.isNotEmpty()) HistoryLine(LineKind.REASON, reason) else summary(e)
            "stopped_looking" -> HistoryLine(LineKind.STOPPED_LOOKING, reason)
            "health_recovered" -> HistoryLine(LineKind.WORKING_AGAIN, str(d, "subject"))
            "media_server_told" -> HistoryLine(LineKind.MEDIA_TOLD)
            "media_server_failed" -> HistoryLine(LineKind.MEDIA_FAILED, plainWhy(str(d, "reason").ifEmpty { str(d, "error") }))
            "alert_failed" -> HistoryLine(LineKind.ALERT_FAILED, plainWhy(str(d, "reason").ifEmpty { str(d, "error") }))
            "report_made", "report_reopened" -> HistoryLine(LineKind.REPORT, str(d, "note"))
            "report_fixed" -> HistoryLine(LineKind.REPORT, str(d, "reply"))
            else -> summary(e)
        }
    }

    private fun summary(e: EventRecord): HistoryLine = HistoryLine(LineKind.SUMMARY, withoutTitle(e.summary, e.titleName))

    fun withoutTitle(
        summary: String,
        title: String,
    ): String {
        val s = summary.trim()
        if (title.isEmpty()) return s
        return when {
            s.startsWith("$title: ") -> s.removePrefix("$title: ")
            s.startsWith("$title ") -> s
            else -> s
        }
    }

    fun plainWhy(reason: String): String {
        var s =
            reason
                .trim()
                .removeSuffix(".")
                .removeSuffix(";")
                .trim()
        if (s.isEmpty()) return ""
        s = s.replace(Regex("[;,]?\\s*(look at the import plan|match the files by hand)$", RegexOption.IGNORE_CASE), "")
        return s.replaceFirstChar { it.uppercase() }
    }

    fun resolutionOf(release: String): String {
        val m = Regex("\\b(2160p|4k|uhd|1080p|1080i|720p|576p|480p)\\b", RegexOption.IGNORE_CASE).find(release) ?: return ""
        val r = m.groupValues[1].lowercase()
        return if (r == "2160p" || r == "4k" || r == "uhd") "4K" else r
    }

    fun extraOf(
        events: List<EventRecord>,
        verb: HistoryVerb,
    ): String {
        val head = events.first()
        val d = head.data
        val release = events.firstNotNullOfOrNull { str(it.data, "release").ifEmpty { null } }.orEmpty()
        val quality = events.firstNotNullOfOrNull { str(it.data, "quality").ifEmpty { null } } ?: resolutionOf(release)
        val bytes =
            events.firstNotNullOfOrNull { e ->
                listOf("size_bytes", "size", "bytes").map { num(e.data, it) }.firstOrNull { it > 0 }
            }
        return when (verb) {
            HistoryVerb.UPGRADED -> {
                val old = str(d, "old_quality")
                val new = str(d, "new_quality")
                if (old.isNotEmpty() && new.isNotEmpty()) "$old → $new" else quality
            }
            HistoryVerb.IMPORTED, HistoryVerb.PARTLY_IMPORTED, HistoryVerb.DOWNLOADED ->
                listOfNotNull(quality.ifEmpty { null }, bytes?.let { DownloadFormat.size(it.toLong()) }).joinToString(" · ")
            HistoryVerb.CHOSEN, HistoryVerb.SENT, HistoryVerb.DOWNLOADING, HistoryVerb.FAILED, HistoryVerb.NEEDS_YOU -> quality
            HistoryVerb.ASKED -> str(d, "albums").ifEmpty { str(d, "seasons") }
            HistoryVerb.SUBTITLES, HistoryVerb.SUBTITLES_WAIT -> str(d, "language")
            else -> ""
        }
    }

    fun whoOf(e: EventRecord): HistoryWho {
        val d = e.data
        return when (e.actor.kind) {
            "user" -> {
                val name =
                    listOf("approved_by", "turned_down_by", "asked_by").map { firstText(d, it) }.firstOrNull { it.isNotEmpty() }
                        ?: e.actorName
                if (name.isNotEmpty()) HistoryWho(WhoKind.PERSON, name) else HistoryWho(WhoKind.A_PERSON)
            }
            "ai" -> HistoryWho(WhoKind.AI, str(d, "client"), e.actorName)
            "job" -> HistoryWho(WhoKind.JOB)
            else -> HistoryWho(WhoKind.HOLYARRS)
        }
    }

    fun facts(events: List<EventRecord>): List<HistoryFact> {
        val out = mutableListOf<HistoryFact>()
        val kinds = events.map { it.kind }
        val downloadish = kinds.any { it.startsWith("release_chosen") || it.startsWith("download_") } || events.size > 1

        fun first(vararg keys: String): String =
            events
                .firstNotNullOfOrNull { e ->
                    keys
                        .map {
                            str(e.data, it)
                        }.firstOrNull { it.isNotEmpty() }
                }.orEmpty()
        if (downloadish) {
            val release = first("release")
            val quality = first("quality").ifEmpty { resolutionOf(release) }
            if (quality.isNotEmpty()) out += HistoryFact(HistoryFactKind.QUALITY, quality)
            val bytes =
                events.firstNotNullOfOrNull { e ->
                    listOf("size_bytes", "size", "bytes").map { num(e.data, it) }.firstOrNull {
                        it >
                            0
                    }
                }
            if (bytes != null) out += HistoryFact(HistoryFactKind.SIZE, DownloadFormat.size(bytes.toLong()))
            first("source_name", "indexer", "source").takeIf { it.isNotEmpty() }?.let { out += HistoryFact(HistoryFactKind.SOURCE, it) }
            first("downloader", "client").takeIf { it.isNotEmpty() }?.let { out += HistoryFact(HistoryFactKind.DOWNLOADER, it) }
            if (release.isNotEmpty()) out += HistoryFact(HistoryFactKind.RELEASE, release)
        } else if (kinds.any { it == "needs_decision" || it == "stopped_looking" }) {
            first("release").takeIf { it.isNotEmpty() }?.let { out += HistoryFact(HistoryFactKind.RELEASE, it) }
        }
        first("file").takeIf { it.isNotEmpty() }?.let { out += HistoryFact(HistoryFactKind.FILE, it) }
        first("from").takeIf { it.isNotEmpty() }?.let { out += HistoryFact(HistoryFactKind.FROM, it) }
        first("folder", "save_path", "path", "to").takeIf { it.isNotEmpty() }?.let { out += HistoryFact(HistoryFactKind.FOLDER, it) }
        val why = first("reason", "error", "message")
        if (why.isNotEmpty()) out += HistoryFact(HistoryFactKind.WHY, why)
        first("album").takeIf { it.isNotEmpty() }?.let { out += HistoryFact(HistoryFactKind.ALBUM, it) }
        return out
    }

    fun redo(
        events: List<EventRecord>,
        verb: HistoryVerb,
    ): HistoryRedo? {
        val kinds = events.map { it.kind }
        if (kinds.none { REDO.containsMatchIn(it) }) return null
        if (kinds.all { it == "download_not_removed" }) return null
        if (events.first().titleId <= 0) return null
        return if (verb == HistoryVerb.FAILED ||
            verb == HistoryVerb.NEEDS_YOU ||
            verb == HistoryVerb.STALLED
        ) {
            HistoryRedo.FIND_ANOTHER
        } else {
            HistoryRedo.REPLACE
        }
    }

    fun releaseOf(events: List<EventRecord>): Long =
        events.firstNotNullOfOrNull { e -> num(e.data, "release_id").toLong().takeIf { it > 0 } } ?: 0

    fun textMatches(
        e: EventRecord,
        query: String,
        users: Map<Long, String>,
    ): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        val person = (if (e.actor.kind == "user") users[e.actor.id] else null) ?: e.actorName
        val fields =
            listOf(e.titleName, e.summary, e.episode, person) +
                listOf("source_name", "indexer", "source", "release", "client", "list_name").map { str(e.data, it) }
        return fields.any { it.lowercase().contains(q) }
    }

    fun whoMatches(
        e: EventRecord,
        who: HistoryWhoFilter,
        me: Long,
    ): Boolean =
        when (who) {
            HistoryWhoFilter.ALL -> true
            HistoryWhoFilter.ME -> e.actor.kind == "user" && e.actor.id == me
            HistoryWhoFilter.PEOPLE -> e.actor.kind == "user"
            HistoryWhoFilter.AI -> e.actor.kind == "ai"
            HistoryWhoFilter.HOLYARRS -> e.actor.kind != "user" && e.actor.kind != "ai"
        }

    fun typeMatches(
        kind: String?,
        type: HistoryType,
    ): Boolean =
        when (type) {
            HistoryType.ALL -> true
            HistoryType.MOVIES -> kind == "movie"
            HistoryType.SHOWS -> kind == "show"
            HistoryType.MUSIC -> kind == "artist" || kind == "album" || kind == "music"
        }

    fun dropHealthyFolders(events: List<EventRecord>): List<EventRecord> =
        events.filter {
            !(
                it.kind == "library_folder_health" &&
                    str(it.data, "health") == "ok"
            )
        }

    internal fun str(
        d: JsonObject?,
        key: String,
    ): String = (d?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull.orEmpty()

    internal fun num(
        d: JsonObject?,
        key: String,
    ): Double = (d?.get(key) as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull ?: 0.0

    internal fun bool(
        d: JsonObject?,
        key: String,
    ): Boolean = (d?.get(key) as? JsonPrimitive)?.booleanOrNull == true

    private fun firstText(
        d: JsonObject?,
        key: String,
    ): String {
        val v = d?.get(key)
        if (v is JsonPrimitive && v.isString) return v.content
        if (v is JsonArray) return (v.firstOrNull() as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
        return ""
    }
}
