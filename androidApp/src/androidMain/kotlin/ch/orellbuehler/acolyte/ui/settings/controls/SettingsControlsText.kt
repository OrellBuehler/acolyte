package ch.orellbuehler.acolyte.ui.settings.controls

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsEvery
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsEveryUnit
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsLinkKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsNext
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsNextKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsNotice
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsReason
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsRun
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsSaved
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsSavedKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsScheduleGroup
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsScheduleNotice
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsScheduleNoticeKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsStale
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsUnit
import ch.orellbuehler.acolyte.ui.components.errorText

private val DETAIL_STATUSES = setOf(400, 403, 409, 422, 502, 503)

@Composable
fun settingsControlsError(error: ApiError): String {
    val base = errorText(error)
    val detail = error.message.takeIf { it.isNotBlank() && error.status in DETAIL_STATUSES }
    return if (detail == null) base else "$base\n$detail"
}

@StringRes
private fun labelRes(kind: SettingsControlsKind): Int =
    when (kind) {
        SettingsControlsKind.PART_ON -> R.string.settings_controls_label_part_on
        SettingsControlsKind.PART_OFF -> R.string.settings_controls_label_part_off
        SettingsControlsKind.PART_RESTART, SettingsControlsKind.STAGE_RESTART -> R.string.settings_controls_label_restart
        SettingsControlsKind.STAGE_PAUSE -> R.string.settings_controls_label_pause
        SettingsControlsKind.STAGE_RESUME -> R.string.settings_controls_label_resume
        SettingsControlsKind.LINK_RECONNECT -> R.string.settings_controls_label_reconnect
        SettingsControlsKind.LINK_TEST -> R.string.settings_controls_label_test
        SettingsControlsKind.APP_RESTART -> R.string.settings_controls_label_app_restart
        SettingsControlsKind.APP_SAFE_MODE -> R.string.settings_controls_label_app_safe
        SettingsControlsKind.APP_SHUTDOWN -> R.string.settings_controls_label_app_shutdown
    }

@StringRes
private fun yesRes(kind: SettingsControlsKind): Int =
    when (kind) {
        SettingsControlsKind.PART_ON -> R.string.settings_controls_yes_part_on
        SettingsControlsKind.PART_OFF -> R.string.settings_controls_yes_part_off
        SettingsControlsKind.PART_RESTART, SettingsControlsKind.STAGE_RESTART -> R.string.settings_controls_yes_restart
        SettingsControlsKind.STAGE_PAUSE -> R.string.settings_controls_yes_pause
        SettingsControlsKind.STAGE_RESUME -> R.string.settings_controls_yes_resume
        SettingsControlsKind.LINK_RECONNECT -> R.string.settings_controls_yes_reconnect
        SettingsControlsKind.LINK_TEST -> R.string.settings_controls_yes_test
        SettingsControlsKind.APP_RESTART -> R.string.settings_controls_yes_app_restart
        SettingsControlsKind.APP_SAFE_MODE -> R.string.settings_controls_yes_app_safe
        SettingsControlsKind.APP_SHUTDOWN -> R.string.settings_controls_yes_app_shutdown
    }

@StringRes
private fun failedRes(kind: SettingsControlsKind): Int =
    when (kind) {
        SettingsControlsKind.PART_ON -> R.string.settings_controls_failed_part_on
        SettingsControlsKind.PART_OFF -> R.string.settings_controls_failed_part_off
        SettingsControlsKind.PART_RESTART, SettingsControlsKind.STAGE_RESTART -> R.string.settings_controls_failed_restart
        SettingsControlsKind.STAGE_PAUSE -> R.string.settings_controls_failed_pause
        SettingsControlsKind.STAGE_RESUME -> R.string.settings_controls_failed_resume
        SettingsControlsKind.LINK_RECONNECT, SettingsControlsKind.LINK_TEST -> R.string.settings_controls_failed_connect
        SettingsControlsKind.APP_RESTART -> R.string.settings_controls_failed_app_restart
        SettingsControlsKind.APP_SAFE_MODE -> R.string.settings_controls_failed_app_safe
        SettingsControlsKind.APP_SHUTDOWN -> R.string.settings_controls_failed_app_shutdown
    }

@StringRes
private fun doneRes(kind: SettingsControlsKind): Int =
    when (kind) {
        SettingsControlsKind.PART_ON -> R.string.settings_controls_done_part_on
        SettingsControlsKind.PART_OFF -> R.string.settings_controls_done_part_off
        SettingsControlsKind.PART_RESTART, SettingsControlsKind.STAGE_RESTART -> R.string.settings_controls_done_restarted
        SettingsControlsKind.STAGE_PAUSE -> R.string.settings_controls_done_paused
        SettingsControlsKind.STAGE_RESUME -> R.string.settings_controls_done_resumed
        SettingsControlsKind.LINK_RECONNECT, SettingsControlsKind.LINK_TEST -> R.string.settings_controls_done_connected
        SettingsControlsKind.APP_RESTART, SettingsControlsKind.APP_SAFE_MODE, SettingsControlsKind.APP_SHUTDOWN ->
            R.string.settings_controls_done_stopping
    }

@Composable
fun settingsControlsLabel(
    kind: SettingsControlsKind,
    name: String,
): String = stringResource(labelRes(kind), name)

@Composable
fun settingsControlsYes(
    kind: SettingsControlsKind,
    name: String,
): String = stringResource(yesRes(kind), name)

@Composable
fun settingsControlsNotice(notice: SettingsControlsNotice): String {
    val isLink = notice.kind == SettingsControlsKind.LINK_RECONNECT || notice.kind == SettingsControlsKind.LINK_TEST
    val title = stringResource(if (notice.ok) doneRes(notice.kind) else failedRes(notice.kind), notice.name)
    val error = notice.error
    val body =
        when {
            error != null -> settingsControlsError(error)
            isLink && notice.ok -> notice.detail.ifEmpty { stringResource(R.string.settings_controls_connected_plain) }
            else -> notice.detail
        }
    return if (body.isEmpty()) title else "$title\n$body"
}

@Composable
fun settingsControlsRun(run: SettingsControlsRun): String =
    stringResource(
        when (run) {
            SettingsControlsRun.OFF -> R.string.settings_controls_run_off
            SettingsControlsRun.STARTING -> R.string.settings_controls_run_starting
            SettingsControlsRun.RUNNING -> R.string.settings_controls_run_running
            SettingsControlsRun.RETRYING -> R.string.settings_controls_run_retrying
            SettingsControlsRun.FAILED -> R.string.settings_controls_run_failed
            SettingsControlsRun.STUCK -> R.string.settings_controls_run_stuck
        },
    )

@Composable
fun settingsControlsLinkKind(kind: SettingsControlsLinkKind): String =
    stringResource(
        when (kind) {
            SettingsControlsLinkKind.SOURCE -> R.string.settings_controls_link_source
            SettingsControlsLinkKind.DOWNLOADER -> R.string.settings_controls_link_downloader
            SettingsControlsLinkKind.MEDIA_SERVER -> R.string.settings_controls_link_media_server
            SettingsControlsLinkKind.SUBTITLE_SITE -> R.string.settings_controls_link_subtitle_site
            SettingsControlsLinkKind.OTHER -> R.string.settings_controls_link_other
        },
    )

@Composable
fun settingsControlsPartName(
    part: String,
    fallback: String,
): String =
    when (part) {
        "movies" -> stringResource(R.string.settings_controls_part_movies)
        "shows" -> stringResource(R.string.settings_controls_part_shows)
        "music" -> stringResource(R.string.settings_controls_part_music)
        "subtitles" -> stringResource(R.string.settings_controls_part_subtitles)
        "wishlist" -> stringResource(R.string.settings_controls_part_wishlist)
        "sources" -> stringResource(R.string.settings_controls_part_sources)
        else -> fallback
    }

@Composable
fun settingsControlsPartStops(part: String): String? =
    when (part) {
        "movies" -> stringResource(R.string.settings_controls_stops_movies)
        "shows" -> stringResource(R.string.settings_controls_stops_shows)
        "music" -> stringResource(R.string.settings_controls_stops_music)
        "subtitles" -> stringResource(R.string.settings_controls_stops_subtitles)
        "wishlist" -> stringResource(R.string.settings_controls_stops_wishlist)
        "sources" -> stringResource(R.string.settings_controls_stops_sources)
        else -> null
    }

@Composable
fun settingsControlsPartNeeds(part: String): String? =
    when (part) {
        "subtitles" -> stringResource(R.string.settings_controls_needs_subtitles)
        "wishlist" -> stringResource(R.string.settings_controls_needs_wishlist)
        else -> null
    }

@PluralsRes
private fun everyRes(unit: SettingsControlsEveryUnit): Int =
    when (unit) {
        SettingsControlsEveryUnit.DAYS -> R.plurals.settings_controls_every_days
        SettingsControlsEveryUnit.HOURS -> R.plurals.settings_controls_every_hours
        SettingsControlsEveryUnit.MINUTES -> R.plurals.settings_controls_every_minutes
        else -> R.plurals.settings_controls_every_seconds
    }

@PluralsRes
private fun runsRes(unit: SettingsControlsEveryUnit): Int =
    when (unit) {
        SettingsControlsEveryUnit.DAYS -> R.plurals.settings_controls_runs_days
        SettingsControlsEveryUnit.HOURS -> R.plurals.settings_controls_runs_hours
        SettingsControlsEveryUnit.MINUTES -> R.plurals.settings_controls_runs_minutes
        else -> R.plurals.settings_controls_runs_seconds
    }

@Composable
fun settingsControlsEvery(every: SettingsControlsEvery): String {
    if (every.unit == SettingsControlsEveryUnit.ALWAYS) return stringResource(R.string.settings_controls_every_always)
    val count = every.count.toInt()
    return pluralStringResource(everyRes(every.unit), count, count)
}

@Composable
fun settingsControlsRuns(every: SettingsControlsEvery): String {
    if (every.unit == SettingsControlsEveryUnit.ALWAYS) return stringResource(R.string.settings_controls_runs_always)
    val count = every.count.toInt()
    return pluralStringResource(runsRes(every.unit), count, count)
}

@Composable
fun settingsControlsNext(next: SettingsControlsNext): String =
    when (next.kind) {
        SettingsControlsNextKind.NONE -> stringResource(R.string.settings_controls_next_none)
        SettingsControlsNextKind.TURNED_OFF -> stringResource(R.string.settings_controls_next_off)
        SettingsControlsNextKind.PART_OFF -> stringResource(R.string.settings_controls_next_part_off)
        SettingsControlsNextKind.SAFE_MODE -> stringResource(R.string.settings_controls_next_safe_mode)
        SettingsControlsNextKind.RUNNING -> stringResource(R.string.settings_controls_next_running)
        SettingsControlsNextKind.QUEUED -> stringResource(R.string.settings_controls_next_queued)
        SettingsControlsNextKind.NOW -> stringResource(R.string.settings_controls_next_now)
        SettingsControlsNextKind.MINUTES -> stringResource(R.string.settings_controls_next_minutes, next.count)
        SettingsControlsNextKind.HOURS -> stringResource(R.string.settings_controls_next_hours, next.count)
        SettingsControlsNextKind.DAYS -> stringResource(R.string.settings_controls_next_days, next.count)
    }

@Composable
fun settingsControlsGroup(group: SettingsControlsScheduleGroup): String =
    stringResource(
        when (group) {
            SettingsControlsScheduleGroup.LIBRARY -> R.string.settings_controls_group_library
            SettingsControlsScheduleGroup.COPIES -> R.string.settings_controls_group_copies
            SettingsControlsScheduleGroup.METADATA -> R.string.settings_controls_group_metadata
            SettingsControlsScheduleGroup.UPKEEP -> R.string.settings_controls_group_upkeep
        },
    )

@Composable
fun settingsControlsReason(reason: SettingsControlsReason): String =
    stringResource(
        when (reason) {
            SettingsControlsReason.NONE, SettingsControlsReason.NOTHING -> R.string.settings_controls_skip_nothing
            SettingsControlsReason.SETTING_OFF -> R.string.settings_controls_skip_setting_off
            SettingsControlsReason.NOBODY -> R.string.settings_controls_skip_nothing
            SettingsControlsReason.STARTING -> R.string.settings_controls_skip_starting
            SettingsControlsReason.PLEX_OFF -> R.string.settings_controls_skip_plex_off
            SettingsControlsReason.PLEX_NOBODY -> R.string.settings_controls_skip_plex_nobody
        },
    )

@Composable
fun settingsControlsStale(stale: SettingsControlsStale): String? =
    when (stale) {
        SettingsControlsStale.NONE -> null
        SettingsControlsStale.REFRESH_OFF -> stringResource(R.string.settings_controls_stale_refresh)
        SettingsControlsStale.RESCAN_OFF -> stringResource(R.string.settings_controls_stale_rescan)
    }

@Composable
fun settingsControlsUnit(unit: SettingsControlsUnit): String =
    stringResource(
        if (unit ==
            SettingsControlsUnit.HOURS
        ) {
            R.string.settings_controls_unit_hours
        } else {
            R.string.settings_controls_unit_minutes
        },
    )

@Composable
fun settingsControlsScheduleNotice(notice: SettingsControlsScheduleNotice): String {
    val label = notice.label
    val error = notice.error
    val title =
        when (notice.kind) {
            SettingsControlsScheduleNoticeKind.STARTED -> stringResource(R.string.settings_controls_sched_started, label)
            SettingsControlsScheduleNoticeKind.QUEUED -> stringResource(R.string.settings_controls_sched_queued, label)
            SettingsControlsScheduleNoticeKind.START_FAILED -> stringResource(R.string.settings_controls_sched_start_failed, label)
            SettingsControlsScheduleNoticeKind.FINISHED -> stringResource(R.string.settings_controls_sched_finished, label)
            SettingsControlsScheduleNoticeKind.NOTHING -> stringResource(R.string.settings_controls_sched_nothing, label)
            SettingsControlsScheduleNoticeKind.JOB_FAILED -> stringResource(R.string.settings_controls_sched_job_failed, label)
            SettingsControlsScheduleNoticeKind.SAVED -> stringResource(R.string.settings_controls_sched_saved, label)
            SettingsControlsScheduleNoticeKind.AUTO_ON -> stringResource(R.string.settings_controls_sched_auto_on, label)
            SettingsControlsScheduleNoticeKind.AUTO_OFF -> stringResource(R.string.settings_controls_sched_auto_off, label)
            SettingsControlsScheduleNoticeKind.SAVE_FAILED -> stringResource(R.string.settings_controls_sched_save_failed)
        }
    val body =
        when {
            error != null -> settingsControlsError(error)
            notice.kind == SettingsControlsScheduleNoticeKind.NOTHING -> settingsControlsReason(notice.reason)
            notice.kind == SettingsControlsScheduleNoticeKind.SAVED -> settingsControlsRuns(notice.every)
            else -> notice.detail
        }
    return if (body.isEmpty()) title else "$title\n$body"
}

@Composable
fun settingsControlsKeep(days: Int): String =
    when (days) {
        0 -> stringResource(R.string.settings_controls_keep_forever)
        365 -> stringResource(R.string.settings_controls_keep_year)
        else -> pluralStringResource(R.plurals.settings_controls_keep_days, days, days)
    }

@Composable
fun settingsControlsSaved(saved: SettingsControlsSaved): String {
    val error = saved.error
    return when (saved.kind) {
        SettingsControlsSavedKind.HISTORY -> stringResource(R.string.settings_controls_saved_history)
        SettingsControlsSavedKind.CACHE ->
            stringResource(R.string.settings_controls_saved_cache) + "\n" +
                stringResource(R.string.settings_controls_saved_cache_up_to, saved.value)
        SettingsControlsSavedKind.COUNT_ON -> stringResource(R.string.settings_controls_saved_counted)
        SettingsControlsSavedKind.COUNT_OFF -> stringResource(R.string.settings_controls_saved_not_counted)
        SettingsControlsSavedKind.URL_BASE ->
            stringResource(R.string.settings_controls_saved_url_base) +
                if (saved.restartNeeded) "\n" + stringResource(R.string.settings_controls_saved_restart_to_use) else ""
        SettingsControlsSavedKind.PROXY_ON -> stringResource(R.string.settings_controls_saved_proxy)
        SettingsControlsSavedKind.PROXY_OFF -> stringResource(R.string.settings_controls_saved_proxy_off)
        SettingsControlsSavedKind.FAILED ->
            stringResource(R.string.settings_controls_saved_failed) + if (error != null) "\n" + settingsControlsError(error) else ""
    }
}
