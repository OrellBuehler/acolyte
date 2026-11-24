package ch.orellbuehler.acolyte.ui.settings.server

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerMissing
import ch.orellbuehler.acolyte.feature.settings.server.showsServerSentence
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.time.OffsetDateTime

@Composable
fun ServerErrorText(
    error: ApiError,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        FailText(errorText(error))
        if (error.showsServerSentence) Hint(error.message)
    }
}

@Composable
fun serverErrorLine(error: ApiError): String = if (error.showsServerSentence) "${errorText(error)} ${error.message}" else errorText(error)

@Composable
fun noticeWithError(
    title: String,
    error: ApiError?,
): String = if (error == null) title else "$title: ${serverErrorLine(error)}"

@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    padded: Boolean = true,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth().padding(horizontal = if (padded) Spacing.Screen else 0.dp),
    ) {
        Row(Modifier.padding(Spacing.Medium), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@StringRes
fun kindLabelRes(wire: String): Int? =
    when (wire) {
        "discord" -> R.string.settings_server_kind_discord
        "telegram" -> R.string.settings_server_kind_telegram
        "ntfy" -> R.string.settings_server_kind_ntfy
        "gotify" -> R.string.settings_server_kind_gotify
        "pushover" -> R.string.settings_server_kind_pushover
        "email" -> R.string.settings_server_kind_email
        "webhook" -> R.string.settings_server_kind_webhook
        else -> null
    }

@Composable
fun kindLabel(wire: String): String = kindLabelRes(wire)?.let { stringResource(it) } ?: wire

@Composable
fun pickerKindLabel(kind: SettingsServerAlertKind): String =
    stringResource(
        when (kind) {
            SettingsServerAlertKind.DISCORD -> R.string.settings_server_kind_discord
            SettingsServerAlertKind.TELEGRAM -> R.string.settings_server_kind_telegram
            SettingsServerAlertKind.NTFY -> R.string.settings_server_kind_ntfy
            SettingsServerAlertKind.GOTIFY -> R.string.settings_server_kind_gotify
            SettingsServerAlertKind.PUSHOVER -> R.string.settings_server_kind_pushover
            SettingsServerAlertKind.EMAIL -> R.string.settings_server_kind_email
            SettingsServerAlertKind.WEBHOOK -> R.string.settings_server_kind_webhook_json
        },
    )

@Composable
fun missingWords(missing: SettingsServerMissing): String =
    if (missing == SettingsServerMissing.NOTHING) {
        ""
    } else {
        stringResource(missingRes(missing))
    }

@StringRes
private fun missingRes(missing: SettingsServerMissing): Int =
    when (missing) {
        SettingsServerMissing.NOTHING, SettingsServerMissing.ADDRESS -> R.string.settings_server_need_address
        SettingsServerMissing.BOT -> R.string.settings_server_need_bot
        SettingsServerMissing.BOT_AND_CHAT -> R.string.settings_server_need_bot_and_chat
        SettingsServerMissing.CHAT -> R.string.settings_server_need_chat
        SettingsServerMissing.TOPIC -> R.string.settings_server_need_topic
        SettingsServerMissing.APP -> R.string.settings_server_need_app
        SettingsServerMissing.SERVER -> R.string.settings_server_need_server
        SettingsServerMissing.SERVER_AND_APP -> R.string.settings_server_need_server_and_app
        SettingsServerMissing.USER_AND_APP -> R.string.settings_server_need_user_and_app
        SettingsServerMissing.USER -> R.string.settings_server_need_user
        SettingsServerMissing.LINK -> R.string.settings_server_need_link
    }

@StringRes
private fun momentTitleRes(id: String): Int? =
    when (id) {
        "ask_waiting" -> R.string.settings_server_moment_ask_waiting_title
        "ask_approved" -> R.string.settings_server_moment_ask_approved_title
        "ask_turned_down" -> R.string.settings_server_moment_ask_turned_down_title
        "ask_ready" -> R.string.settings_server_moment_ask_ready_title
        "new_episodes" -> R.string.settings_server_moment_new_episodes_title
        "new_albums" -> R.string.settings_server_moment_new_albums_title
        "report_made" -> R.string.settings_server_moment_report_made_title
        "report_fixed" -> R.string.settings_server_moment_report_fixed_title
        "grab" -> R.string.settings_server_moment_grab_title
        "downloaded" -> R.string.settings_server_moment_downloaded_title
        "download_failed" -> R.string.settings_server_moment_download_failed_title
        "import" -> R.string.settings_server_moment_import_title
        "rename" -> R.string.settings_server_moment_rename_title
        "add" -> R.string.settings_server_moment_add_title
        "delete" -> R.string.settings_server_moment_delete_title
        "health" -> R.string.settings_server_moment_health_title
        "needs_decision" -> R.string.settings_server_moment_needs_decision_title
        "plugin_message" -> R.string.settings_server_moment_plugin_message_title
        else -> null
    }

@StringRes
private fun momentHintRes(id: String): Int? =
    when (id) {
        "ask_waiting" -> R.string.settings_server_moment_ask_waiting_hint
        "ask_approved" -> R.string.settings_server_moment_ask_approved_hint
        "ask_turned_down" -> R.string.settings_server_moment_ask_turned_down_hint
        "ask_ready" -> R.string.settings_server_moment_ask_ready_hint
        "new_episodes" -> R.string.settings_server_moment_new_episodes_hint
        "new_albums" -> R.string.settings_server_moment_new_albums_hint
        "report_made" -> R.string.settings_server_moment_report_made_hint
        "report_fixed" -> R.string.settings_server_moment_report_fixed_hint
        "grab" -> R.string.settings_server_moment_grab_hint
        "downloaded" -> R.string.settings_server_moment_downloaded_hint
        "download_failed" -> R.string.settings_server_moment_download_failed_hint
        "import" -> R.string.settings_server_moment_import_hint
        "rename" -> R.string.settings_server_moment_rename_hint
        "add" -> R.string.settings_server_moment_add_hint
        "delete" -> R.string.settings_server_moment_delete_hint
        "health" -> R.string.settings_server_moment_health_hint
        "needs_decision" -> R.string.settings_server_moment_needs_decision_hint
        "plugin_message" -> R.string.settings_server_moment_plugin_message_hint
        else -> null
    }

@Composable
fun momentTitle(id: String): String = momentTitleRes(id)?.let { stringResource(it) } ?: id

@Composable
fun momentHint(id: String): String = momentHintRes(id)?.let { stringResource(it) } ?: ""

@Composable
fun momentsLine(ids: List<String>): String =
    if (ids.isEmpty()) stringResource(R.string.settings_server_alert_no_moments) else ids.map { momentTitle(it) }.joinToString(" · ")

fun parseMillis(iso: String): Long? =
    try {
        OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }

fun relativeText(iso: String): String? =
    parseMillis(iso)?.let { DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString() }

fun dateTimeText(
    context: Context,
    iso: String,
): String {
    val millis = parseMillis(iso) ?: return iso
    return DateUtils.formatDateTime(
        context,
        millis,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )
}

fun sizeText(
    context: Context,
    bytes: Long,
): String = Formatter.formatShortFileSize(context, bytes)
