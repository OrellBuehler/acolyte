package ch.orellbuehler.acolyte.ui.downloads

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.api.model.ShareLimits
import ch.orellbuehler.acolyte.common.R
import ch.orellbuehler.acolyte.feature.downloads.ClientNote
import ch.orellbuehler.acolyte.feature.downloads.ClientNoteKind
import ch.orellbuehler.acolyte.feature.downloads.DetailKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadFormat
import ch.orellbuehler.acolyte.feature.downloads.DownloadItem
import ch.orellbuehler.acolyte.feature.downloads.DownloadKindFilter
import ch.orellbuehler.acolyte.feature.downloads.DownloadNoteKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadRules
import ch.orellbuehler.acolyte.feature.downloads.DownloadsEmpty
import ch.orellbuehler.acolyte.feature.downloads.DownloadsEmptyKind
import ch.orellbuehler.acolyte.feature.downloads.DownloadsNotice
import ch.orellbuehler.acolyte.feature.downloads.DownloadsState
import ch.orellbuehler.acolyte.feature.downloads.DownloadsSub
import ch.orellbuehler.acolyte.feature.downloads.DownloadsTab
import ch.orellbuehler.acolyte.feature.downloads.NetworkFilter
import ch.orellbuehler.acolyte.feature.downloads.NoticeKind
import ch.orellbuehler.acolyte.feature.downloads.ProblemKind
import ch.orellbuehler.acolyte.feature.downloads.RowDetail
import ch.orellbuehler.acolyte.feature.downloads.RowNote
import ch.orellbuehler.acolyte.feature.downloads.SortKey
import ch.orellbuehler.acolyte.feature.downloads.StatusLine
import ch.orellbuehler.acolyte.feature.downloads.StatusWord
import ch.orellbuehler.acolyte.feature.downloads.SubLine
import ch.orellbuehler.acolyte.feature.downloads.SubLineKind
import ch.orellbuehler.acolyte.feature.downloads.UsenetPriority
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@StringRes
fun DownloadsTab.label(): Int =
    when (this) {
        DownloadsTab.ALL -> R.string.downloads_tab_all
        DownloadsTab.WAITING -> R.string.downloads_tab_waiting
        DownloadsTab.PROGRESS -> R.string.downloads_tab_progress
        DownloadsTab.PROBLEMS -> R.string.downloads_tab_problems
        DownloadsTab.DONE -> R.string.downloads_tab_done
    }

@StringRes
fun DownloadsSub.label(): Int =
    when (this) {
        DownloadsSub.QUEUED -> R.string.downloads_sub_queued
        DownloadsSub.PAUSED -> R.string.downloads_sub_paused
        DownloadsSub.DOWNLOADING -> R.string.downloads_sub_downloading
        DownloadsSub.CHECKING -> R.string.downloads_sub_checking
        DownloadsSub.IMPORTING -> R.string.downloads_sub_importing
        DownloadsSub.FAILED -> R.string.downloads_sub_failed
        DownloadsSub.DECIDE -> R.string.downloads_sub_decide
    }

@StringRes
fun StatusWord.label(): Int =
    when (this) {
        StatusWord.FAILED -> R.string.downloads_word_failed
        StatusWord.PACK_WAITING -> R.string.downloads_word_pack_waiting
        StatusWord.MOVING -> R.string.downloads_word_moving
        StatusWord.CHECKING -> R.string.downloads_word_checking
        StatusWord.QUEUED -> R.string.downloads_word_queued
        StatusWord.PAUSED -> R.string.downloads_word_paused
        StatusWord.DOWNLOADING -> R.string.downloads_word_downloading
        StatusWord.STALLED -> R.string.downloads_word_stalled
        StatusWord.IMPORTING -> R.string.downloads_word_importing
        StatusWord.SEEDING -> R.string.downloads_word_seeding
        StatusWord.REVIEW -> R.string.downloads_word_review
        StatusWord.DONE_SEEDING -> R.string.downloads_word_done_seeding
        StatusWord.DONE -> R.string.downloads_word_done
        StatusWord.IMPORTED -> R.string.downloads_word_imported
        StatusWord.TAKEN -> R.string.downloads_word_taken
    }

@StringRes
fun ProblemKind.label(): Int =
    when (this) {
        ProblemKind.MISSING_ARTICLES -> R.string.downloads_problem_missing_articles
        ProblemKind.REPAIR_NEEDED -> R.string.downloads_problem_repair_needed
        ProblemKind.REPAIRING -> R.string.downloads_problem_repairing
        ProblemKind.IMPORT_BLOCKED -> R.string.downloads_problem_import_blocked
        ProblemKind.TRACKER_ERROR -> R.string.downloads_problem_tracker_error
        ProblemKind.NO_TRACKER_REPLY -> R.string.downloads_problem_no_tracker_reply
        ProblemKind.DOWNLOADER_STOPPED -> R.string.downloads_problem_downloader_stopped
        ProblemKind.NO_FULL_COPY -> R.string.downloads_problem_no_full_copy
        ProblemKind.NO_PEERS -> R.string.downloads_problem_no_peers
        ProblemKind.TRACKER_WARNING -> R.string.downloads_problem_tracker_warning
    }

@Composable
fun detailText(detail: RowDetail): String =
    when (detail.kind) {
        DetailKind.FILES_GONE -> stringResource(R.string.downloads_detail_files_gone)
        DetailKind.UNPACKING -> stringResource(R.string.downloads_detail_unpacking)
        DetailKind.UP_NEXT -> stringResource(R.string.downloads_detail_up_next)
        DetailKind.PLACING -> stringResource(R.string.downloads_detail_placing)
        DetailKind.IMPORT_FAILED -> stringResource(R.string.downloads_detail_import_failed, detail.text)
        DetailKind.NO_FILE -> stringResource(R.string.downloads_detail_no_file)
        DetailKind.REMOVED -> stringResource(R.string.downloads_detail_removed)
        DetailKind.REFUSED -> detail.text
        DetailKind.NO_MATCH -> stringResource(R.string.downloads_detail_no_match)
        DetailKind.CLEANUP_HOLD -> stringResource(R.string.downloads_detail_cleanup, detail.text)
        DetailKind.RATIO -> stringResource(R.string.downloads_detail_ratio, detail.text)
    }

@Composable
fun noteText(note: RowNote): String =
    when (note.kind) {
        DownloadNoteKind.NEXT_IN_LINE -> stringResource(R.string.downloads_note_next)
        DownloadNoteKind.IN_LINE -> stringResource(R.string.downloads_note_in_line, note.number)
        DownloadNoteKind.VERIFYING -> stringResource(R.string.downloads_note_verifying)
        DownloadNoteKind.REPAIRING -> stringResource(R.string.downloads_note_repairing)
        DownloadNoteKind.UNPACKING -> stringResource(R.string.downloads_note_unpacking)
        DownloadNoteKind.FINISHING -> stringResource(R.string.downloads_note_finishing)
        DownloadNoteKind.SCRIPT -> stringResource(R.string.downloads_note_script)
        DownloadNoteKind.FILES_MISSING -> stringResource(R.string.downloads_note_files_missing)
        DownloadNoteKind.DOWNLOADER_ERROR -> stringResource(R.string.downloads_note_downloader_error)
        DownloadNoteKind.IMPORT_FAILED -> stringResource(R.string.downloads_note_import_failed)
        DownloadNoteKind.REMOVED_OUTSIDE -> stringResource(R.string.downloads_note_removed_outside)
        DownloadNoteKind.NO_MATCH -> stringResource(R.string.downloads_note_no_match)
        DownloadNoteKind.FILES_GONE -> stringResource(R.string.downloads_word_taken)
        DownloadNoteKind.STILL_IN -> stringResource(R.string.downloads_note_still_in, note.text)
        DownloadNoteKind.CLIENT_TEXT -> note.text
        DownloadNoteKind.DETAIL -> note.detail?.let { detailText(it).replaceFirstChar { c -> c.uppercase() } }.orEmpty()
    }

@Composable
fun subLineText(line: SubLine): String =
    when (line.kind) {
        SubLineKind.TEXT -> line.text
        SubLineKind.SEASON -> stringResource(R.string.downloads_season, line.from)
        SubLineKind.SEASONS -> stringResource(R.string.downloads_seasons, line.from, line.to)
    }

@StringRes
fun DownloadAction.label(): Int =
    when (this) {
        DownloadAction.PAUSE -> R.string.downloads_action_pause
        DownloadAction.RESUME -> R.string.downloads_action_resume
        DownloadAction.RETRY -> R.string.downloads_action_retry
        DownloadAction.MOVE_UP -> R.string.downloads_action_up
        DownloadAction.MOVE_DOWN -> R.string.downloads_action_down
        DownloadAction.MOVE_TOP -> R.string.downloads_action_top
        DownloadAction.MOVE_BOTTOM -> R.string.downloads_action_bottom
        DownloadAction.PRIORITY -> R.string.downloads_action_priority
        DownloadAction.IMPORT_AGAIN -> R.string.downloads_action_import
        DownloadAction.OPEN_TITLE -> R.string.downloads_action_open
        DownloadAction.REMOVE -> R.string.downloads_action_remove
        DownloadAction.RECHECK -> R.string.downloads_action_recheck
        DownloadAction.REANNOUNCE -> R.string.downloads_action_reannounce
        DownloadAction.FORCE_START -> R.string.downloads_action_force
        DownloadAction.SEEDING_RULES -> R.string.downloads_action_seeding
        DownloadAction.SET_LOCATION -> R.string.downloads_action_location
        DownloadAction.SPEED_LIMITS -> R.string.downloads_action_speeds
        DownloadAction.MARK_FAILED -> R.string.downloads_action_mark_failed
    }

@StringRes
fun UsenetPriority.label(): Int =
    when (this) {
        UsenetPriority.LOW -> R.string.downloads_priority_low
        UsenetPriority.NORMAL -> R.string.downloads_priority_normal
        UsenetPriority.HIGH -> R.string.downloads_priority_high
        UsenetPriority.FORCE -> R.string.downloads_priority_force
    }

@StringRes
fun SortKey.label(): Int =
    when (this) {
        SortKey.TITLE -> R.string.downloads_sort_title
        SortKey.PROGRESS -> R.string.downloads_sort_progress
        SortKey.STATUS -> R.string.downloads_sort_status
        SortKey.SIZE -> R.string.downloads_sort_size
        SortKey.QUALITY -> R.string.downloads_sort_quality
        SortKey.ADDED -> R.string.downloads_sort_added
        SortKey.SEEDS -> R.string.downloads_sort_seeds
        SortKey.PEERS -> R.string.downloads_sort_peers
        SortKey.RATIO -> R.string.downloads_sort_ratio
        SortKey.TRACKER -> R.string.downloads_sort_tracker
        SortKey.REQUESTED -> R.string.downloads_sort_requested
        SortKey.DOWNLOADER -> R.string.downloads_sort_downloader
    }

@Composable
fun durationText(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return when {
        s < 60 -> stringResource(R.string.downloads_duration_s, s.toInt())
        s < 3600 -> stringResource(R.string.downloads_duration_m, (s / 60).toInt())
        s < 48 * 3600 -> stringResource(R.string.downloads_duration_hm, (s / 3600).toInt(), ((s % 3600) / 60).toInt())
        else -> stringResource(R.string.downloads_duration_dh, (s / 86400).toInt(), ((s % 86400) / 3600).toInt())
    }
}

@Composable
fun limitsText(l: ShareLimits): String {
    fun minutes(m: Long) = m * 60
    val parts = mutableListOf<String>()
    when {
        l.ratio == -2.0 -> Unit
        l.ratio < 0 -> parts += stringResource(R.string.downloads_rules_no_limit)
        else -> parts += stringResource(R.string.downloads_rules_ratio, DownloadRules.shortRatio(l.ratio))
    }
    if (l.seedingMinutes >= 0) parts += stringResource(R.string.downloads_rules_seeding, durationText(minutes(l.seedingMinutes)))
    if (l.inactiveMinutes >= 0) parts += stringResource(R.string.downloads_rules_idle, durationText(minutes(l.inactiveMinutes)))
    val base = if (parts.isEmpty()) stringResource(R.string.downloads_rules_downloader) else parts.joinToString(" · ")
    val then =
        when (l.action) {
            "stop" -> stringResource(R.string.downloads_then_stop)
            "remove" -> stringResource(R.string.downloads_then_remove)
            "remove_files" -> stringResource(R.string.downloads_then_remove_files)
            "super_seed" -> stringResource(R.string.downloads_then_super)
            else -> ""
        }
    return if (then.isEmpty()) base else stringResource(R.string.downloads_rules_then, base, then.lowercase())
}

fun speedText(bytes: Long): String = DownloadFormat.speed(bytes)

fun dateTimeText(iso: String): String? =
    try {
        DateTimeFormatter
            .ofLocalizedDateTime(
                FormatStyle.MEDIUM,
                FormatStyle.SHORT,
            ).withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        null
    }

fun clockText(iso: String): String? =
    try {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
    } catch (e: Exception) {
        null
    }

@Composable
fun noticeText(notice: DownloadsNotice): String {
    val o = notice.outcome
    return when (notice.kind) {
        NoticeKind.PAUSED -> stringResource(R.string.downloads_notice_paused)
        NoticeKind.STARTED -> stringResource(R.string.downloads_notice_started)
        NoticeKind.RETRIED -> stringResource(R.string.downloads_notice_retried)
        NoticeKind.MOVED_TOP -> stringResource(R.string.downloads_notice_moved_top)
        NoticeKind.MOVED_BOTTOM -> stringResource(R.string.downloads_notice_moved_bottom)
        NoticeKind.MOVED_UP -> stringResource(R.string.downloads_notice_moved_up)
        NoticeKind.MOVED_DOWN -> stringResource(R.string.downloads_notice_moved_down)
        NoticeKind.PRIORITY_SET -> stringResource(R.string.downloads_notice_priority)
        NoticeKind.CHECKING -> stringResource(R.string.downloads_notice_checking)
        NoticeKind.ANNOUNCED -> stringResource(R.string.downloads_notice_announced)
        NoticeKind.FORCE_STARTED -> stringResource(R.string.downloads_notice_force)
        NoticeKind.IMPORT_QUEUED -> stringResource(R.string.downloads_notice_import_queued)
        NoticeKind.REMOVED -> stringResource(R.string.downloads_notice_removed, notice.count)
        NoticeKind.REMOVED_KEPT -> stringResource(R.string.downloads_notice_removed_kept, notice.text)
        NoticeKind.MARKED_FAILED -> stringResource(R.string.downloads_notice_marked_failed)
        NoticeKind.ALL_STARTED -> stringResource(R.string.downloads_notice_all_started)
        NoticeKind.ALL_PAUSED -> stringResource(R.string.downloads_notice_all_paused)
        NoticeKind.ALL_FAILED -> stringResource(R.string.downloads_notice_all_failed, notice.text)
        NoticeKind.SPEED_SAVED -> stringResource(R.string.downloads_notice_speed_saved)
        NoticeKind.SEEDING_SET -> stringResource(R.string.downloads_notice_seeding_set)
        NoticeKind.MOVING -> stringResource(R.string.downloads_notice_moving)
        NoticeKind.SPEEDS_SET -> stringResource(R.string.downloads_notice_speeds_set)
        NoticeKind.SEARCHING -> stringResource(R.string.downloads_notice_searching, notice.text)
        NoticeKind.TURNED_DOWN -> stringResource(R.string.downloads_notice_turned_down, notice.text)
        NoticeKind.STOPPED -> stringResource(R.string.downloads_notice_stopped, notice.text)
        NoticeKind.IMPORTED -> stringResource(R.string.downloads_notice_imported, notice.text)
        NoticeKind.EDITION_SET -> stringResource(R.string.downloads_notice_edition_set)
        NoticeKind.MATCHING -> stringResource(R.string.downloads_notice_matching)
        NoticeKind.MATCHED -> stringResource(R.string.downloads_notice_matched)
        NoticeKind.DOWNLOAD_FAILED ->
            if (notice.text.isBlank()) {
                stringResource(R.string.downloads_notice_download_failed_plain)
            } else {
                stringResource(R.string.downloads_notice_download_failed, notice.text)
            }
        NoticeKind.BULK_DONE ->
            if (notice.skipped > 0) {
                stringResource(R.string.downloads_notice_bulk_skipped, notice.count, notice.skipped)
            } else {
                stringResource(R.string.downloads_notice_bulk, notice.count)
            }
        NoticeKind.NOT_HERE -> stringResource(R.string.downloads_notice_not_here)
        NoticeKind.ADDED ->
            if (o == null) {
                stringResource(R.string.downloads_add_nothing)
            } else {
                val parts = mutableListOf<String>()
                if (o.added > 0) parts += stringResource(R.string.downloads_add_added, o.added)
                if (o.pending > 0) parts += stringResource(R.string.downloads_add_pending, o.pending)
                if (o.duplicates > 0) parts += stringResource(R.string.downloads_add_duplicates, o.duplicates)
                if (o.failed > 0) parts += stringResource(R.string.downloads_add_failed, o.failed)
                if (o.bad.isNotEmpty()) parts += stringResource(R.string.downloads_add_bad, o.bad.joinToString(", "))
                if (o.dropped > 0) parts += stringResource(R.string.downloads_add_dropped, o.dropped)
                if (parts.isEmpty()) stringResource(R.string.downloads_add_nothing) else parts.joinToString(" · ")
            }
    }
}

fun subEmpty(sub: DownloadsSub?): Int =
    when (sub) {
        DownloadsSub.QUEUED -> R.string.downloads_empty_sub_queued
        DownloadsSub.PAUSED -> R.string.downloads_empty_sub_paused
        DownloadsSub.DOWNLOADING -> R.string.downloads_empty_sub_downloading
        DownloadsSub.CHECKING -> R.string.downloads_empty_sub_checking
        DownloadsSub.IMPORTING -> R.string.downloads_empty_sub_importing
        DownloadsSub.FAILED -> R.string.downloads_empty_sub_failed
        DownloadsSub.DECIDE, null -> R.string.downloads_empty_sub_decide
    }

fun tabEmpty(tab: DownloadsTab): Pair<Int, Int> =
    when (tab) {
        DownloadsTab.ALL -> R.string.downloads_empty_all_title to R.string.downloads_empty_all
        DownloadsTab.WAITING -> R.string.downloads_empty_waiting_title to R.string.downloads_empty_waiting
        DownloadsTab.PROGRESS -> R.string.downloads_empty_progress_title to R.string.downloads_empty_progress
        DownloadsTab.PROBLEMS -> R.string.downloads_empty_problems_title to R.string.downloads_empty_problems
        DownloadsTab.DONE -> R.string.downloads_empty_done_title to R.string.downloads_empty_done
    }

@Composable
fun clientNoteText(note: ClientNote): String =
    stringResource(
        when (note.kind) {
            ClientNoteKind.WAITING -> R.string.downloads_note_client_waiting
            ClientNoteKind.STALE -> R.string.downloads_note_client_stale
            ClientNoteKind.OFFLINE -> R.string.downloads_note_client_offline
        },
        note.name,
    )

data class EmptyWords(
    val title: String,
    val message: String?,
)

@Composable
fun downloadsEmptyWords(
    state: DownloadsState,
    empty: DownloadsEmpty,
): EmptyWords {
    val f = state.filter
    return when (empty.kind) {
        DownloadsEmptyKind.NO_MATCHES -> EmptyWords(stringResource(R.string.downloads_empty_matches, f.query), null)
        DownloadsEmptyKind.NO_KIND ->
            EmptyWords(
                stringResource(
                    when (f.kind) {
                        DownloadKindFilter.SHOW -> R.string.downloads_empty_kind_show
                        DownloadKindFilter.MUSIC -> R.string.downloads_empty_kind_music
                        else -> R.string.downloads_empty_kind_movie
                    },
                ),
                null,
            )
        DownloadsEmptyKind.NO_NETWORK ->
            EmptyWords(
                stringResource(
                    if (f.network ==
                        NetworkFilter.USENET
                    ) {
                        R.string.downloads_empty_network_usenet
                    } else {
                        R.string.downloads_empty_network_torrent
                    },
                ),
                null,
            )
        DownloadsEmptyKind.LOADING_MORE ->
            EmptyWords(
                stringResource(R.string.downloads_empty_loading_title),
                stringResource(R.string.downloads_empty_loading, empty.count),
            )
        DownloadsEmptyKind.SUB -> EmptyWords(stringResource(subEmpty(f.sub)), null)
        DownloadsEmptyKind.NOTHING, DownloadsEmptyKind.TAB -> {
            val (title, message) = tabEmpty(f.tab)
            EmptyWords(stringResource(title), stringResource(message))
        }
    }
}

@Composable
fun downloadSecondLine(item: DownloadItem): String {
    val problem = item.problem
    val note = item.note
    return when {
        problem != null -> stringResource(problem.kind.label())
        note != null -> noteText(note)
        else -> ""
    }
}

@Composable
fun downloadFactsLine(item: DownloadItem): String {
    val facts = mutableListOf<String>()
    if (item.row.size > 0) facts += DownloadFormat.size(item.row.size)
    if (!item.history) facts += "${DownloadFormat.percent(item.progress)}%"
    if (item.moving && item.row.speed >= 1000) {
        facts += stringResource(R.string.downloads_speed_down, speedText(item.row.speed))
        if (item.row.upSpeed > 0) facts += stringResource(R.string.downloads_speed_up, speedText(item.row.upSpeed))
        if (item.row.eta > 0) facts += stringResource(R.string.downloads_time_left, durationText(item.row.eta))
    }
    return facts.joinToString(" · ")
}

@Composable
fun statusLineText(line: StatusLine): String {
    val parts = mutableListOf(stringResource(R.string.downloads_speed_down, speedText(line.speed)))
    if (line.free > 0) parts += stringResource(R.string.downloads_status_free, DownloadFormat.freeSize(line.free))
    if (line.left > 0) parts += stringResource(R.string.downloads_time_left, durationText(line.left))
    return parts.joinToString(" · ") + if (line.limited) " · " + stringResource(R.string.downloads_status_limited) else ""
}

@Composable
fun dayText(day: ch.orellbuehler.acolyte.feature.downloads.Day): String {
    val date = java.time.LocalDate.of(day.year, day.month, day.day)
    val now = java.time.LocalDate.now()
    val pattern = if (day.year == now.year) "EEE, d MMM" else "EEE, d MMM yyyy"
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return java.time.format.DateTimeFormatter
        .ofPattern(pattern, locale)
        .format(date)
}
