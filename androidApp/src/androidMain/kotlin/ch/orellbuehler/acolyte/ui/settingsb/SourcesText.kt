package ch.orellbuehler.acolyte.ui.settingsb

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.sources.SettingsCheckWord
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCause
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceErrorKind
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceOp
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceOutcome
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceProblem
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceRow
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceWord
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesLogic
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesNotice
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesNoticeKind
import ch.orellbuehler.acolyte.ui.components.errorText
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun sourceErrorText(error: ApiError): String = if (SettingsSourcesLogic.showsServerText(error)) error.message else errorText(error)

fun sourceTimeText(iso: String): String? =
    try {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
    } catch (e: java.time.format.DateTimeParseException) {
        null
    }

fun sourceDurationText(ms: Int): String = SettingsSourcesLogic.durationText(ms, DecimalFormatSymbols.getInstance().decimalSeparator)

@Composable
fun sourceWordText(row: SettingsSourceRow): String =
    when (row.word) {
        SettingsSourceWord.SLOWED -> stringResource(R.string.acq_src_word_slowed)
        SettingsSourceWord.DOWN -> stringResource(R.string.acq_src_word_down)
        SettingsSourceWord.BLOCKED -> stringResource(R.string.acq_src_word_blocked)
        SettingsSourceWord.KEY_REFUSED -> stringResource(R.string.acq_src_word_key_refused)
        SettingsSourceWord.WORKING -> stringResource(R.string.acq_src_word_working)
        SettingsSourceWord.UNREACHABLE -> stringResource(R.string.acq_src_word_unreachable)
        SettingsSourceWord.KEY_TURNED_DOWN -> stringResource(R.string.acq_src_word_key_turned_down)
        SettingsSourceWord.PAUSED -> stringResource(R.string.acq_src_word_paused)
        SettingsSourceWord.OTHER -> row.rawWord
    }

@Composable
fun sourceProtocolText(protocol: String): String =
    when (protocol) {
        "torrent" -> stringResource(R.string.acq_src_proto_torrent)
        "usenet" -> stringResource(R.string.acq_src_proto_usenet)
        else -> protocol.replaceFirstChar { it.uppercase() }
    }

@Composable
fun sourceUsesText(
    feed: Boolean,
    auto: Boolean,
    hand: Boolean,
): String {
    val parts = mutableListOf<String>()
    if (feed) parts += stringResource(R.string.acq_src_use_feed)
    if (auto) parts += stringResource(R.string.acq_src_use_auto)
    if (hand) parts += stringResource(R.string.acq_src_use_hand)
    return if (parts.isEmpty()) stringResource(R.string.acq_src_use_none) else parts.joinToString(" · ")
}

@Composable
fun sourceProblemText(problem: SettingsSourceProblem): String =
    stringResource(
        when (problem) {
            SettingsSourceProblem.NAME -> R.string.acq_src_problem_name
            SettingsSourceProblem.URL -> R.string.acq_src_problem_url
            SettingsSourceProblem.URL_QUERY -> R.string.acq_src_problem_url_query
            SettingsSourceProblem.PRIORITY -> R.string.acq_src_problem_priority
            SettingsSourceProblem.SEEDERS -> R.string.acq_src_problem_seeders
            SettingsSourceProblem.SEARCHES -> R.string.acq_src_problem_searches
            SettingsSourceProblem.WINDOW -> R.string.acq_src_problem_window
            SettingsSourceProblem.GAP -> R.string.acq_src_problem_gap
            SettingsSourceProblem.TIMEOUT -> R.string.acq_src_problem_timeout
            SettingsSourceProblem.PROXY -> R.string.acq_src_problem_proxy
            SettingsSourceProblem.SITE_USER -> R.string.acq_src_problem_site_user
            SettingsSourceProblem.SITE_AGENT -> R.string.acq_src_problem_site_agent
            SettingsSourceProblem.LANGUAGES -> R.string.acq_src_problem_languages
        },
    )

@Composable
fun sourceCauseText(cause: SettingsSourceCause): String =
    stringResource(
        when (cause) {
            SettingsSourceCause.NAME_NOT_FOUND -> R.string.acq_src_cause_name_not_found
            SettingsSourceCause.REFUSED -> R.string.acq_src_cause_refused
            SettingsSourceCause.TIMEOUT -> R.string.acq_src_cause_timeout
            SettingsSourceCause.CERTIFICATE -> R.string.acq_src_cause_certificate
            SettingsSourceCause.USE_HTTP -> R.string.acq_src_cause_use_http
            SettingsSourceCause.USE_HTTPS -> R.string.acq_src_cause_use_https
            SettingsSourceCause.AUTH -> R.string.acq_src_cause_auth
            SettingsSourceCause.WRONG_PATH -> R.string.acq_src_cause_wrong_path
            SettingsSourceCause.WRONG_APP -> R.string.acq_src_cause_wrong_app
            SettingsSourceCause.CATEGORY_MISSING -> R.string.acq_src_cause_category_missing
            SettingsSourceCause.LOCAL_NETWORK -> R.string.acq_src_cause_local_network
        },
    )

@Composable
fun sourceOpText(
    op: SettingsSourceOp,
    raw: String,
): String =
    when (op) {
        SettingsSourceOp.CAPS -> stringResource(R.string.acq_src_op_caps)
        SettingsSourceOp.SEARCH -> stringResource(R.string.acq_src_op_search)
        SettingsSourceOp.FEED -> stringResource(R.string.acq_src_op_feed)
        SettingsSourceOp.TEST -> stringResource(R.string.acq_src_op_test)
        SettingsSourceOp.OTHER -> raw
    }

@Composable
fun sourceErrorKindText(
    kind: SettingsSourceErrorKind,
    raw: String,
): String =
    when (kind) {
        SettingsSourceErrorKind.DOWN -> stringResource(R.string.acq_src_err_down)
        SettingsSourceErrorKind.RATE_LIMITED -> stringResource(R.string.acq_src_err_rate_limited)
        SettingsSourceErrorKind.LOGIN_REFUSED -> stringResource(R.string.acq_src_err_login_refused)
        SettingsSourceErrorKind.BROKEN_DEFINITION -> stringResource(R.string.acq_src_err_broken_definition)
        SettingsSourceErrorKind.BLOCKED -> stringResource(R.string.acq_src_err_blocked)
        SettingsSourceErrorKind.OTHER -> raw.ifEmpty { stringResource(R.string.acq_src_err_other) }
    }

@Composable
fun sourceCheckWordText(word: SettingsCheckWord): String =
    stringResource(
        when (word) {
            SettingsCheckWord.CONNECTED -> R.string.acq_src_check_connected
            SettingsCheckWord.NOT_CONNECTED -> R.string.acq_src_check_not_connected
            SettingsCheckWord.PAUSED -> R.string.acq_src_check_paused
        },
    )

@Composable
fun sourceOutcomeText(outcome: SettingsSourceOutcome): String {
    if (!outcome.ok) return outcome.error?.let { sourceErrorText(it) } ?: outcome.text
    val time = sourceDurationText(outcome.ms)
    return if (outcome.items > 0) {
        pluralStringResource(R.plurals.acq_src_test_items, outcome.items, outcome.items) + " · " + time
    } else {
        stringResource(R.string.acq_src_test_answered, time)
    }
}

@Composable
fun sourceNoticeText(notice: SettingsSourcesNotice): String =
    when (notice.kind) {
        SettingsSourcesNoticeKind.SAVED -> stringResource(R.string.acq_src_saved, notice.name)
        SettingsSourcesNoticeKind.PAUSE_FAILED -> failed(R.string.acq_src_pause_failed, notice.error)
        SettingsSourcesNoticeKind.RESUME_FAILED -> failed(R.string.acq_src_resume_failed, notice.error)
        SettingsSourcesNoticeKind.REMOVE_FAILED -> failed(R.string.acq_src_remove_failed, notice.error)
        SettingsSourcesNoticeKind.BRIDGE_CONNECTED -> {
            val body =
                if (notice.detail == "jackett") {
                    pluralStringResource(R.plurals.acq_src_bridge_added, notice.count, notice.count)
                } else {
                    stringResource(R.string.acq_src_bridge_arrive)
                }
            stringResource(R.string.acq_src_bridge_connected, notice.name) + ". " + body
        }
        SettingsSourcesNoticeKind.BRIDGE_DISCONNECTED -> stringResource(R.string.acq_src_bridge_disconnected, notice.name)
        SettingsSourcesNoticeKind.BRIDGE_SYNCED ->
            stringResource(R.string.acq_src_bridge_synced) + ". " +
                stringResource(R.string.acq_src_bridge_sync_body, notice.added, notice.removed, notice.changed)
        SettingsSourcesNoticeKind.BRIDGE_UP_TO_DATE -> stringResource(R.string.acq_src_bridge_up_to_date)
        SettingsSourcesNoticeKind.BRIDGE_SYNCING_AGAIN -> stringResource(R.string.acq_src_bridge_syncing_again, notice.name)
        SettingsSourcesNoticeKind.BRIDGE_SYNC_FAILED -> failed(R.string.acq_src_bridge_sync_failed, notice.error)
    }

@Composable
private fun failed(
    @StringRes res: Int,
    error: ApiError?,
): String = stringResource(res) + (error?.let { ". " + sourceErrorText(it) } ?: "")
