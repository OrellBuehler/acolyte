package ch.orellbuehler.acolyte.ui.settingsb

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleCovers
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleNote
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleNoteKind
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleSiteRow
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleSiteState
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleWishProblem
import ch.orellbuehler.acolyte.ui.components.errorText
import java.time.Instant

@Composable
internal fun subtitleErrorText(error: ApiError): String =
    if ((error.code == ApiErrorCode.BAD_REQUEST || error.code == ApiErrorCode.CONFLICT) && error.message.isNotBlank()) {
        error.message
    } else {
        errorText(error)
    }

@Composable
internal fun subtitleTypeText(type: String): String =
    stringResource(
        when (type) {
            "hi" -> R.string.acq_sub_type_hi
            "either" -> R.string.acq_sub_type_either
            else -> R.string.acq_sub_type_normal
        },
    )

@Composable
internal fun subtitleProblemText(problem: SettingsSubtitleWishProblem): String =
    stringResource(
        when (problem) {
            SettingsSubtitleWishProblem.TOO_MANY -> R.string.acq_sub_problem_too_many
            SettingsSubtitleWishProblem.PICK -> R.string.acq_sub_problem_pick
            SettingsSubtitleWishProblem.TWICE -> R.string.acq_sub_problem_twice
        },
    )

@Composable
internal fun subtitleCoversText(covers: SettingsSubtitleCovers): String =
    stringResource(
        when (covers) {
            SettingsSubtitleCovers.BOTH -> R.string.acq_sub_covers_both
            SettingsSubtitleCovers.MOVIES -> R.string.acq_sub_covers_movies
            SettingsSubtitleCovers.SHOWS -> R.string.acq_sub_covers_shows
        },
    )

@Composable
internal fun subtitleAccountLabel(field: String): String =
    stringResource(
        when (field) {
            "username" -> R.string.acq_sub_field_username
            "password" -> R.string.acq_sub_field_password
            else -> R.string.acq_sub_field_api_key
        },
    )

@Composable
private fun pauseWhy(reason: String): String =
    stringResource(
        when (reason) {
            "down" -> R.string.acq_sub_pause_down
            "rate_limited" -> R.string.acq_sub_pause_rate_limited
            "download_limit" -> R.string.acq_sub_pause_download_limit
            "login_refused" -> R.string.acq_sub_pause_login_refused
            "blocked" -> R.string.acq_sub_pause_blocked
            "unreadable" -> R.string.acq_sub_pause_unreadable
            else -> R.string.acq_sub_pause_failed
        },
    )

@Composable
internal fun subtitleStateLabel(state: SettingsSubtitleSiteState): String =
    stringResource(
        when (state) {
            SettingsSubtitleSiteState.NEEDS_ACCOUNT -> R.string.acq_sub_state_needs
            SettingsSubtitleSiteState.PAUSED -> R.string.acq_sub_state_paused
            SettingsSubtitleSiteState.RESTING -> R.string.acq_sub_state_resting
            SettingsSubtitleSiteState.ON -> R.string.acq_sub_state_on
        },
    )

@Composable
internal fun subtitleStateText(row: SettingsSubtitleSiteRow): String =
    when (row.state) {
        SettingsSubtitleSiteState.NEEDS_ACCOUNT -> stringResource(R.string.acq_sub_state_needs_text)
        SettingsSubtitleSiteState.PAUSED -> stringResource(R.string.acq_sub_state_paused_text)
        SettingsSubtitleSiteState.ON -> stringResource(R.string.acq_sub_state_on_text)
        SettingsSubtitleSiteState.RESTING -> {
            val why = pauseWhy(row.pauseReason)
            val until = whenText(LocalContext.current, row.pausedUntil)
            if (until.isEmpty()) {
                stringResource(R.string.acq_sub_state_resting_text, why)
            } else {
                stringResource(R.string.acq_sub_state_resting_until, until, why)
            }
        }
    }

private fun whenText(
    context: Context,
    iso: String,
): String {
    if (iso.isEmpty()) return ""
    return try {
        val millis = Instant.parse(iso).toEpochMilli()
        val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
        DateUtils.formatDateTime(context, millis, flags)
    } catch (e: Exception) {
        iso
    }
}

@Composable
internal fun subtitleNoteText(note: SettingsSubtitleNote): String =
    when (note.kind) {
        SettingsSubtitleNoteKind.SETTINGS_SAVED -> stringResource(R.string.acq_sub_note_settings_saved)
        SettingsSubtitleNoteKind.TRANSLATOR_SAVED -> stringResource(R.string.acq_sub_note_translator_saved)
        SettingsSubtitleNoteKind.SITE_SAVED -> stringResource(R.string.acq_sub_note_site_saved, note.name)
        SettingsSubtitleNoteKind.SITE_PAUSED -> stringResource(R.string.acq_sub_note_site_paused, note.name)
        SettingsSubtitleNoteKind.SITE_RESUMED -> stringResource(R.string.acq_sub_note_site_resumed, note.name)
        SettingsSubtitleNoteKind.PAUSE_FAILED -> noteWithError(R.string.acq_sub_note_pause_failed, note.error)
        SettingsSubtitleNoteKind.RESUME_FAILED -> noteWithError(R.string.acq_sub_note_resume_failed, note.error)
        SettingsSubtitleNoteKind.REMOVE_FAILED -> noteWithError(R.string.acq_sub_note_remove_failed, note.error)
        SettingsSubtitleNoteKind.CLEAN_QUEUED -> stringResource(R.string.acq_sub_note_clean_queued)
        SettingsSubtitleNoteKind.CLEAN_FAILED -> noteWithError(R.string.acq_sub_note_clean_failed, note.error)
    }

@Composable
private fun noteWithError(
    res: Int,
    error: ApiError?,
): String {
    val head = stringResource(res)
    return if (error == null) head else "$head: ${subtitleErrorText(error)}"
}
