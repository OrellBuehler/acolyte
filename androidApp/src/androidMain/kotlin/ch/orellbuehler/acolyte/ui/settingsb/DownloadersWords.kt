package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderHealthState
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderIssue
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderKeyNote
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderKind
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderMessage
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderMessageKind
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderRow
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderRulesIssue
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderTestView
import ch.orellbuehler.acolyte.ui.components.errorText

@Composable
fun downloaderErrorText(error: ApiError): String =
    if (error.status in SERVER_SENTENCES && error.message.isNotBlank()) error.message else errorText(error)

private val SERVER_SENTENCES = 400..409

@Composable
fun downloaderHealthWord(row: SettingsDownloaderRow): String =
    when (row.health) {
        SettingsDownloaderHealthState.WORKING -> stringResource(R.string.acq_dl_state_working)
        SettingsDownloaderHealthState.PAUSED -> stringResource(R.string.acq_dl_state_paused)
        SettingsDownloaderHealthState.RESTING -> stringResource(R.string.acq_dl_state_resting)
        SettingsDownloaderHealthState.NOT_ANSWERING -> stringResource(R.string.acq_dl_state_not_answering)
        SettingsDownloaderHealthState.SIGN_IN_REFUSED -> stringResource(R.string.acq_dl_state_sign_in_refused)
        SettingsDownloaderHealthState.UNKNOWN -> row.healthWord.ifBlank { row.info.health.state }
    }

@Composable
fun downloaderTestText(view: SettingsDownloaderTestView): String =
    view.text.ifBlank { view.error?.let { downloaderErrorText(it) }.orEmpty() }

@Composable
fun downloaderIssueText(issue: SettingsDownloaderIssue): String =
    stringResource(
        when (issue) {
            SettingsDownloaderIssue.NAME -> R.string.acq_dl_issue_name
            SettingsDownloaderIssue.URL -> R.string.acq_dl_issue_url
            SettingsDownloaderIssue.CATEGORY, SettingsDownloaderIssue.KIND_CATEGORY, SettingsDownloaderIssue.AFTER_IMPORT ->
                R.string.acq_dl_issue_category
            SettingsDownloaderIssue.CATEGORY_LABEL -> R.string.acq_dl_issue_label
            SettingsDownloaderIssue.CATEGORY_FOLDER -> R.string.acq_dl_issue_folder
            SettingsDownloaderIssue.AFTER_IMPORT_SAME -> R.string.acq_dl_issue_after_same
            SettingsDownloaderIssue.PRIORITY -> R.string.acq_dl_issue_priority
            SettingsDownloaderIssue.PATH -> R.string.acq_dl_issue_path
            SettingsDownloaderIssue.RATIO -> R.string.acq_dl_issue_ratio
            SettingsDownloaderIssue.HOURS -> R.string.acq_dl_issue_hours
            SettingsDownloaderIssue.SECRET -> R.string.acq_dl_issue_secret
            SettingsDownloaderIssue.USERNAME -> R.string.acq_dl_issue_username
        },
    )

@Composable
fun downloaderRulesIssueText(issue: SettingsDownloaderRulesIssue): String =
    stringResource(
        when (issue) {
            SettingsDownloaderRulesIssue.NOT_A_NUMBER -> R.string.acq_dl_rules_issue_number
            SettingsDownloaderRulesIssue.STALLED_AFTER_RANGE -> R.string.acq_dl_rules_issue_after
            SettingsDownloaderRulesIssue.ACT_SOONER -> R.string.acq_dl_rules_issue_sooner
            SettingsDownloaderRulesIssue.ACT_RANGE -> R.string.acq_dl_rules_issue_act
            SettingsDownloaderRulesIssue.SPACING_RANGE -> R.string.acq_dl_rules_issue_spacing
            SettingsDownloaderRulesIssue.DEAD_MINUTES_RANGE -> R.string.acq_dl_rules_issue_dead
            SettingsDownloaderRulesIssue.KBPS_RANGE -> R.string.acq_dl_rules_issue_kbps
            SettingsDownloaderRulesIssue.HOURS_RANGE -> R.string.acq_dl_rules_issue_hours
        },
    )

@Composable
fun downloaderKeyNoteText(note: SettingsDownloaderKeyNote): String =
    stringResource(
        when (note) {
            SettingsDownloaderKeyNote.FETCHED -> R.string.acq_dl_key_fetched
            SettingsDownloaderKeyNote.NEEDS_LOGIN -> R.string.acq_dl_key_needs_login
            SettingsDownloaderKeyNote.WRONG_LOGIN -> R.string.acq_dl_key_wrong_login
            SettingsDownloaderKeyNote.NOT_FOUND -> R.string.acq_dl_key_not_found
        },
    )

@Composable
fun downloaderCategoryHint(kind: SettingsDownloaderKind?): String =
    stringResource(
        when (kind) {
            SettingsDownloaderKind.SABNZBD -> R.string.acq_dl_cat_sab
            SettingsDownloaderKind.TRANSMISSION -> R.string.acq_dl_cat_transmission
            SettingsDownloaderKind.DELUGE -> R.string.acq_dl_cat_deluge
            SettingsDownloaderKind.RTORRENT -> R.string.acq_dl_cat_rtorrent
            SettingsDownloaderKind.ARIA2 -> R.string.acq_dl_cat_aria2
            SettingsDownloaderKind.NZBGET -> R.string.acq_dl_cat_nzbget
            else -> R.string.acq_dl_cat_qbit
        },
    )

@Composable
fun downloaderMessageText(message: SettingsDownloaderMessage): String {
    val reason = message.error?.let { downloaderErrorText(it) }
    val head =
        when (message.kind) {
            SettingsDownloaderMessageKind.SAVED -> stringResource(R.string.acq_dl_saved, message.name)
            SettingsDownloaderMessageKind.PAUSE_FAILED -> stringResource(R.string.acq_dl_pause_failed)
            SettingsDownloaderMessageKind.RESUME_FAILED -> stringResource(R.string.acq_dl_resume_failed)
            SettingsDownloaderMessageKind.REMOVE_FAILED -> stringResource(R.string.acq_dl_remove_failed)
            SettingsDownloaderMessageKind.CATEGORY_MADE -> stringResource(R.string.acq_dl_category_made, message.name, message.app)
            SettingsDownloaderMessageKind.CATEGORY_FAILED -> stringResource(R.string.acq_dl_category_failed, message.name)
            SettingsDownloaderMessageKind.ABANDONED -> stringResource(R.string.acq_dl_abandoned)
            SettingsDownloaderMessageKind.ABANDON_FAILED -> stringResource(R.string.acq_dl_abandon_failed)
        }
    return if (reason == null) head else stringResource(R.string.acq_dl_with_reason, head, reason)
}
