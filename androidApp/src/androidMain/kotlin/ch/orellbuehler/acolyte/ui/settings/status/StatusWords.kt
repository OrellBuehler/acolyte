package ch.orellbuehler.acolyte.ui.settings.status

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDiskKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDue
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusDueKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusFix
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusGroup
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusNotice
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusNoticeKind
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusReason
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusRow
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusTitle
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusUnit
import ch.orellbuehler.acolyte.feature.settings.status.SettingsStatusWord
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.nav.SettingsAlertsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsBackupsRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsDownloadersRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsLibraryFoldersRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsMediaServersRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSchedulesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSourcesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsSubtitlesRoute
import ch.orellbuehler.acolyte.ui.nav.SettingsThisAppRoute

@StringRes
private fun wordRes(word: SettingsStatusWord): Int =
    when (word) {
        SettingsStatusWord.WORKING -> R.string.settings_status_word_working
        SettingsStatusWord.WARNING -> R.string.settings_status_word_warning
        SettingsStatusWord.BROKEN -> R.string.settings_status_word_broken
        SettingsStatusWord.STUCK -> R.string.settings_status_word_stuck
        SettingsStatusWord.STARTING -> R.string.settings_status_word_starting
        SettingsStatusWord.RETRYING -> R.string.settings_status_word_retrying
        SettingsStatusWord.OFF -> R.string.settings_status_word_off
        SettingsStatusWord.NOT_SYNCED -> R.string.settings_status_word_not_synced
        SettingsStatusWord.NOT_SENT -> R.string.settings_status_word_not_sent
        SettingsStatusWord.TURNED_OFF -> R.string.settings_status_word_turned_off
        SettingsStatusWord.PART_OFF -> R.string.settings_status_word_part_off
        SettingsStatusWord.SAFE_MODE -> R.string.settings_status_word_safe_mode
        SettingsStatusWord.RUNNING -> R.string.settings_status_word_running
        SettingsStatusWord.FAILED -> R.string.settings_status_word_failed
        SettingsStatusWord.LATE -> R.string.settings_status_word_late
        SettingsStatusWord.NOT_RUN -> R.string.settings_status_word_not_run
        SettingsStatusWord.CANCELLED -> R.string.settings_status_word_cancelled
        SettingsStatusWord.OK -> R.string.settings_status_word_ok
    }

@Composable
fun statusWord(word: SettingsStatusWord): String = stringResource(wordRes(word))

private fun titleRes(title: SettingsStatusTitle): Int? =
    when (title) {
        SettingsStatusTitle.PLAIN -> null
        SettingsStatusTitle.FOLDER -> R.string.settings_status_title_folder
        SettingsStatusTitle.FREE_SPACE -> R.string.settings_status_title_free_space
        SettingsStatusTitle.BACKUPS -> R.string.settings_status_title_backups
        SettingsStatusTitle.UPDATE -> R.string.settings_status_title_update
        SettingsStatusTitle.CONFIG_DISK -> R.string.settings_status_title_config_disk
        SettingsStatusTitle.CONFIG_FOLDER -> R.string.settings_status_title_config_folder
        SettingsStatusTitle.WAITING_DOWNLOADS -> R.string.settings_status_title_waiting_downloads
        SettingsStatusTitle.CLEAR_WAITING -> R.string.settings_status_title_clear_waiting
        SettingsStatusTitle.SAVE_FOLDER -> R.string.settings_status_title_save_folder
        SettingsStatusTitle.TORRENT_DOWNLOADER -> R.string.settings_status_title_torrent_downloader
        SettingsStatusTitle.USENET_DOWNLOADER -> R.string.settings_status_title_usenet_downloader
        SettingsStatusTitle.SAME_DOWNLOADER -> R.string.settings_status_title_same_downloader
        SettingsStatusTitle.PEOPLE_CHECK -> R.string.settings_status_title_people_check
        SettingsStatusTitle.METADATA -> R.string.settings_status_title_metadata
    }

@Composable
fun statusRowName(row: SettingsStatusRow): String {
    val res = titleRes(row.title) ?: return row.name
    val label = stringResource(res)
    return if (row.name.isBlank()) label else "$label ${row.name}"
}

@Composable
fun statusRowReason(row: SettingsStatusRow): String =
    when (row.reasonKind) {
        SettingsStatusReason.NONE -> ""
        SettingsStatusReason.TEXT -> row.reason
        SettingsStatusReason.JOB_LATE -> stringResource(R.string.settings_status_reason_late)
        SettingsStatusReason.JOB_FAILED -> stringResource(R.string.settings_status_reason_failed)
        SettingsStatusReason.CONFIG_STEPS -> stringResource(R.string.settings_status_reason_config_steps)
        SettingsStatusReason.SAVE_FOLDER -> stringResource(R.string.settings_status_reason_save_folder, row.reason)
        SettingsStatusReason.NO_TORRENT_DOWNLOADER -> stringResource(R.string.settings_status_reason_no_torrent)
        SettingsStatusReason.NO_USENET_DOWNLOADER -> stringResource(R.string.settings_status_reason_no_usenet)
        SettingsStatusReason.SAME_DOWNLOADER -> stringResource(R.string.settings_status_reason_same_downloader, row.reason)
        SettingsStatusReason.KEY_KIT -> stringResource(R.string.settings_status_reason_key_kit)
        SettingsStatusReason.KEY_KIT_OWNER -> stringResource(R.string.settings_status_reason_key_kit_owner)
        SettingsStatusReason.METADATA_DOWN -> stringResource(R.string.settings_status_reason_metadata_down)
    }

@Composable
fun statusGroupLabel(group: SettingsStatusGroup): String =
    stringResource(
        when (group) {
            SettingsStatusGroup.SOURCES -> R.string.settings_status_group_sources
            SettingsStatusGroup.DOWNLOADERS -> R.string.settings_status_group_downloaders
            SettingsStatusGroup.LIBRARY -> R.string.settings_status_group_library
            SettingsStatusGroup.MEDIA -> R.string.settings_status_group_media
            SettingsStatusGroup.SUBTITLES -> R.string.settings_status_group_subtitles
            SettingsStatusGroup.LISTS -> R.string.settings_status_group_lists
            SettingsStatusGroup.ALERTS -> R.string.settings_status_group_alerts
            SettingsStatusGroup.METADATA -> R.string.settings_status_group_metadata
            SettingsStatusGroup.APP -> R.string.settings_status_group_app
            SettingsStatusGroup.JOBS -> R.string.settings_status_group_jobs
        },
    )

@Composable
fun statusGroupNone(group: SettingsStatusGroup): String =
    stringResource(
        when (group) {
            SettingsStatusGroup.SOURCES -> R.string.settings_status_none_sources
            SettingsStatusGroup.DOWNLOADERS -> R.string.settings_status_none_downloaders
            SettingsStatusGroup.MEDIA -> R.string.settings_status_none_media
            SettingsStatusGroup.SUBTITLES -> R.string.settings_status_none_subtitles
            SettingsStatusGroup.LISTS -> R.string.settings_status_none_lists
            SettingsStatusGroup.ALERTS -> R.string.settings_status_none_alerts
            else -> R.string.settings_status_none_metadata
        },
    )

@Composable
fun statusDiskName(
    label: String,
    kind: SettingsStatusDiskKind,
): String =
    label.ifBlank {
        stringResource(
            when (kind) {
                SettingsStatusDiskKind.MOVIES -> R.string.settings_status_disk_movies
                SettingsStatusDiskKind.SHOWS -> R.string.settings_status_disk_shows
                SettingsStatusDiskKind.MUSIC -> R.string.settings_status_disk_music
                SettingsStatusDiskKind.OTHER -> R.string.settings_status_disk_library
            },
        )
    }

@Composable
fun statusSince(row: SettingsStatusRow): String? {
    val ago = relativeTime(row.at) ?: return null
    val res =
        when (row.group) {
            SettingsStatusGroup.SOURCES, SettingsStatusGroup.DOWNLOADERS, SettingsStatusGroup.MEDIA, SettingsStatusGroup.SUBTITLES ->
                R.string.settings_status_since
            SettingsStatusGroup.LIBRARY, SettingsStatusGroup.APP, SettingsStatusGroup.METADATA -> R.string.settings_status_checked
            SettingsStatusGroup.LISTS -> R.string.settings_status_synced
            SettingsStatusGroup.ALERTS -> R.string.settings_status_sent
            SettingsStatusGroup.JOBS -> R.string.settings_status_ran
        }
    return stringResource(res, ago)
}

@Composable
fun statusDue(due: SettingsStatusDue): String =
    when (due.kind) {
        SettingsStatusDueKind.NONE -> stringResource(R.string.settings_status_due_none)
        SettingsStatusDueKind.NOW -> stringResource(R.string.settings_status_due_now)
        SettingsStatusDueKind.IN ->
            stringResource(
                when (due.unit) {
                    SettingsStatusUnit.MINUTES -> R.string.settings_status_due_in_min
                    SettingsStatusUnit.HOURS -> R.string.settings_status_due_in_hour
                    SettingsStatusUnit.DAYS -> R.string.settings_status_due_in_day
                },
                due.amount,
            )
        SettingsStatusDueKind.OVERDUE ->
            stringResource(
                when (due.unit) {
                    SettingsStatusUnit.MINUTES -> R.string.settings_status_due_late_min
                    SettingsStatusUnit.HOURS -> R.string.settings_status_due_late_hour
                    SettingsStatusUnit.DAYS -> R.string.settings_status_due_late_day
                },
                due.amount,
            )
    }

fun statusFixRoute(fix: SettingsStatusFix): Any =
    when (fix) {
        SettingsStatusFix.SOURCES -> SettingsSourcesRoute
        SettingsStatusFix.DOWNLOADERS -> SettingsDownloadersRoute
        SettingsStatusFix.MEDIA_SERVERS -> SettingsMediaServersRoute
        SettingsStatusFix.SUBTITLES -> SettingsSubtitlesRoute
        SettingsStatusFix.LIBRARY_FOLDERS -> SettingsLibraryFoldersRoute
        SettingsStatusFix.BACKUPS -> SettingsBackupsRoute
        SettingsStatusFix.THIS_APP -> SettingsThisAppRoute
        SettingsStatusFix.OUTSIDE_LISTS -> Routes.settingsOutsideLists
        SettingsStatusFix.ALERTS -> SettingsAlertsRoute
        SettingsStatusFix.SCHEDULES -> SettingsSchedulesRoute
        SettingsStatusFix.DOWNLOADS_WAITING -> Routes.downloads(show = "waiting.paused")
        SettingsStatusFix.DOWNLOADS -> Routes.downloads()
    }

private val SERVER_SENTENCE_STATUSES = setOf(400, 409, 422, 502, 503)

@Composable
fun statusErrorDetail(error: ApiError): String {
    val line = errorText(error)
    val sentence = error.message.takeIf { it.isNotBlank() && error.status in SERVER_SENTENCE_STATUSES }
    return if (sentence != null) "$line\n$sentence" else line
}

@Composable
fun statusNoticeText(notice: SettingsStatusNotice): String {
    val row = notice.row
    val name = if (row != null) statusRowName(row) else ""
    val word = if (row != null) statusWord(row.word) else ""
    val head =
        when (notice.kind) {
            SettingsStatusNoticeKind.CHECKS_RAN -> stringResource(R.string.settings_status_notice_checks_ran)
            SettingsStatusNoticeKind.CHECKS_FAILED -> stringResource(R.string.settings_status_notice_checks_failed)
            SettingsStatusNoticeKind.ROW_WORKING -> stringResource(R.string.settings_status_notice_row_working, name)
            SettingsStatusNoticeKind.ROW_WORD -> stringResource(R.string.settings_status_notice_row_word, name, word)
            SettingsStatusNoticeKind.ROW_CONNECTED -> stringResource(R.string.settings_status_notice_connected, name)
            SettingsStatusNoticeKind.ROW_CANT_CONNECT -> stringResource(R.string.settings_status_notice_cant_connect, name)
            SettingsStatusNoticeKind.ROW_CANT_CHECK -> stringResource(R.string.settings_status_notice_cant_check, name)
            SettingsStatusNoticeKind.JOB_STARTED -> stringResource(R.string.settings_status_notice_job_started, name)
            SettingsStatusNoticeKind.JOB_ALREADY -> stringResource(R.string.settings_status_notice_job_already, name)
            SettingsStatusNoticeKind.UPDATE_CHECK_FAILED -> stringResource(R.string.settings_status_notice_update_check_failed)
            SettingsStatusNoticeKind.UPDATE_SAVED -> stringResource(R.string.settings_status_notice_saved)
            SettingsStatusNoticeKind.UPDATE_SAVE_FAILED -> stringResource(R.string.settings_status_notice_save_failed)
            SettingsStatusNoticeKind.UPDATE_INSTALL_FAILED -> stringResource(R.string.settings_status_notice_install_failed)
            SettingsStatusNoticeKind.UPDATE_GO_BACK_FAILED -> stringResource(R.string.settings_status_notice_go_back_failed)
            SettingsStatusNoticeKind.BUNDLE_FAILED -> stringResource(R.string.settings_status_notice_bundle_failed)
        }
    val error = notice.error
    val detail =
        when {
            notice.detail.isNotBlank() -> notice.detail
            error != null -> statusErrorDetail(error)
            else -> ""
        }
    return if (detail.isBlank()) head else "$head\n$detail"
}
