package ch.orellbuehler.acolyte.ui.title

import android.icu.text.CompactDecimalFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.title.AirWord
import ch.orellbuehler.acolyte.feature.title.Feedback
import ch.orellbuehler.acolyte.feature.title.FeedbackKind
import ch.orellbuehler.acolyte.feature.title.HeroPill
import ch.orellbuehler.acolyte.feature.title.LabelProblem
import ch.orellbuehler.acolyte.feature.title.LibraryLabel
import ch.orellbuehler.acolyte.feature.title.LibraryStatus
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.feature.title.VerdictKind
import ch.orellbuehler.acolyte.feature.title.WishProblem
import ch.orellbuehler.acolyte.ui.components.errorText
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun libraryLabel(status: LibraryStatus): String =
    when (status.label) {
        LibraryLabel.IN_LIBRARY -> stringResource(R.string.title_state_in_library)
        LibraryLabel.DOWNLOADED -> stringResource(R.string.title_state_downloaded)
        LibraryLabel.DOWNLOADING -> stringResource(R.string.title_state_downloading)
        LibraryLabel.DOWNLOADING_PERCENT -> stringResource(R.string.title_state_downloading_percent, status.count)
        LibraryLabel.DOWNLOADING_EPISODES -> pluralStringResource(R.plurals.title_state_downloading_episodes, status.count, status.count)
        LibraryLabel.FAILED_EPISODES -> pluralStringResource(R.plurals.title_state_failed_episodes, status.count, status.count)
        LibraryLabel.EPISODES_OF -> stringResource(R.string.title_state_episodes_of, status.count, status.total)
        LibraryLabel.DOWNLOAD_FAILED -> stringResource(R.string.title_state_download_failed)
        LibraryLabel.NOT_DOWNLOADED -> stringResource(R.string.title_state_not_downloaded)
    }

@Composable
fun pillText(
    pill: HeroPill,
    mine: Boolean,
): String? {
    val base =
        when (pill) {
            HeroPill.NONE -> return null
            HeroPill.HIDDEN -> return stringResource(R.string.title_pill_hidden)
            HeroPill.NEVER_ADD -> return stringResource(R.string.title_pill_never_add)
            HeroPill.ASKED -> stringResource(R.string.title_pill_asked)
            HeroPill.APPROVED -> stringResource(R.string.title_pill_approved)
            HeroPill.TURNED_DOWN -> stringResource(R.string.title_pill_turned_down)
            HeroPill.AVAILABLE -> stringResource(R.string.title_pill_available)
        }
    return if (mine) stringResource(R.string.title_pill_you_asked, base) else base
}

@Composable
fun feedbackText(feedback: Feedback): String {
    val f = feedback
    return when (f.kind) {
        FeedbackKind.FAILED -> f.error?.let { errorText(it) } ?: f.text.ifBlank { stringResource(CommonR.string.error_unknown) }
        FeedbackKind.SEARCHING -> stringResource(R.string.title_note_searching, f.text)
        FeedbackKind.SEARCHING_SEASON -> stringResource(R.string.title_note_searching_season, f.count)
        FeedbackKind.SEARCHING_EPISODE -> stringResource(R.string.title_note_searching_episode, f.text)
        FeedbackKind.SEARCHING_MISSING -> stringResource(R.string.title_note_searching_missing)
        FeedbackKind.NOTHING_TO_SEARCH -> stringResource(R.string.title_note_nothing_to_search)
        FeedbackKind.TURN_SHOW_ON -> stringResource(R.string.title_note_turn_show_on)
        FeedbackKind.REFRESHING -> stringResource(R.string.title_note_refreshing, f.text)
        FeedbackKind.SAVED -> stringResource(R.string.title_note_saved, f.text)
        FeedbackKind.ORDER_CHANGED -> stringResource(R.string.title_note_order_changed)
        FeedbackKind.MOVING -> stringResource(R.string.title_note_moving, f.text)
        FeedbackKind.MARKED_FAILED -> stringResource(R.string.title_note_marked_failed)
        FeedbackKind.VERSION_ADDED, FeedbackKind.VERSION_SAVED, FeedbackKind.FILE_SAVED -> stringResource(R.string.title_note_saved, f.text)
        FeedbackKind.VERSION_REMOVED -> stringResource(R.string.title_note_version_removed, f.text)
        FeedbackKind.FILE_ADOPTED -> stringResource(R.string.title_note_file_adopted, f.text)
        FeedbackKind.PACK_NEXT -> stringResource(R.string.title_note_pack_next)
        FeedbackKind.HIDDEN ->
            if (f.text.isBlank()) {
                stringResource(
                    R.string.title_note_hidden_everyone,
                )
            } else {
                stringResource(R.string.title_note_hidden_for, f.text)
            }
        FeedbackKind.SHOWN_AGAIN -> stringResource(R.string.title_note_shown_again, f.text)
        FeedbackKind.RESTORED -> stringResource(R.string.title_note_restored, f.text)
        FeedbackKind.SENT -> sentText(R.string.title_note_sent, f)
        FeedbackKind.SENDING -> sentText(R.string.title_note_sending, f)
        FeedbackKind.NOT_SENT -> stringResource(R.string.title_note_not_sent, f.text)
        FeedbackKind.TURNED_DOWN -> stringResource(R.string.title_note_turned_down)
        FeedbackKind.GROUP_BLOCKED -> stringResource(R.string.title_note_group_blocked, f.text)
        FeedbackKind.SUBTITLES_SEARCHING -> stringResource(R.string.title_note_subtitles_searching)
        FeedbackKind.SUBTITLES_NO_FILE -> stringResource(R.string.title_note_subtitles_no_file)
        FeedbackKind.SUBTITLES_SAVED -> stringResource(R.string.title_note_subtitles_saved)
        FeedbackKind.SUBTITLES_DEFAULT -> stringResource(R.string.title_note_subtitles_default)
        FeedbackKind.SUBTITLE_SAVED -> stringResource(R.string.title_note_subtitle_saved, f.text)
        FeedbackKind.TRANSLATING -> stringResource(R.string.title_note_translating, f.text)
        FeedbackKind.TIMING_FIXED -> stringResource(R.string.title_note_timing_fixed)
        FeedbackKind.CLEANED -> pluralStringResource(R.plurals.title_note_cleaned, f.count, f.count)
        FeedbackKind.NOTHING_TO_CLEAN -> stringResource(R.string.title_note_nothing_to_clean)
        FeedbackKind.SUBTITLE_TURNED_DOWN -> stringResource(R.string.title_note_subtitle_turned_down)
        FeedbackKind.MATCHED -> pluralStringResource(R.plurals.title_note_matched, f.count, f.count)
        FeedbackKind.UNMATCHED -> pluralStringResource(R.plurals.title_note_unmatched, f.count, f.count)
        FeedbackKind.RESCANNING -> stringResource(R.string.title_note_rescanning)
        FeedbackKind.RESCAN_DONE -> pluralStringResource(R.plurals.title_note_rescan_done, f.count, f.count)
        FeedbackKind.RENAMED -> pluralStringResource(R.plurals.title_note_renamed, f.count, f.count)
        FeedbackKind.RENAME_CHANGED -> stringResource(R.string.title_note_rename_changed)
        FeedbackKind.RENAME_FAILED -> stringResource(R.string.title_note_rename_failed)
        FeedbackKind.DELETED ->
            if (f.count >
                0
            ) {
                stringResource(R.string.title_note_deleted_kept, f.text, f.count)
            } else {
                stringResource(R.string.title_note_deleted, f.text)
            }
        FeedbackKind.ADDED -> stringResource(R.string.title_note_added, f.text)
    }
}

@Composable
private fun sentText(
    res: Int,
    f: Feedback,
): String {
    val head = stringResource(res)
    return when {
        f.count > 1 -> head + " · " + pluralStringResource(R.plurals.title_note_warnings, f.count, f.count)
        f.text.isNotBlank() -> "$head · ${f.text}"
        else -> head
    }
}

@Composable
fun verdictText(kind: VerdictKind): String =
    stringResource(
        when (kind) {
            VerdictKind.TURNED_DOWN -> R.string.title_verdict_turned_down
            VerdictKind.CHOSEN -> R.string.title_verdict_chosen
            VerdictKind.HELD -> R.string.title_verdict_held
            VerdictKind.WOULD_CHOOSE -> R.string.title_verdict_would_choose
            VerdictKind.WOULD_HOLD -> R.string.title_verdict_would_hold
            VerdictKind.REFUSED -> R.string.title_verdict_refused
            VerdictKind.PASSED_OVER -> R.string.title_verdict_passed_over
            VerdictKind.JUDGED -> R.string.title_verdict_judged
        },
    )

@Composable
fun labelProblemText(problem: LabelProblem): String =
    stringResource(
        when (problem) {
            LabelProblem.EMPTY -> R.string.title_label_empty
            LabelProblem.TOO_LONG -> R.string.title_label_too_long
            LabelProblem.BAD_CHARACTER -> R.string.title_label_bad_character
            LabelProblem.TRAILING_DOT -> R.string.title_label_trailing_dot
            LabelProblem.DEVICE_NAME -> R.string.title_label_device
            LabelProblem.TAKEN -> R.string.title_label_taken
        },
    )

@Composable
fun wishProblemText(problem: WishProblem): String =
    stringResource(
        when (problem) {
            WishProblem.TOO_MANY -> R.string.title_wish_too_many
            WishProblem.PICK -> R.string.title_wish_pick
            WishProblem.TWICE -> R.string.title_wish_twice
        },
    )

@Composable
fun runtimeText(minutes: Int): String =
    when {
        minutes <= 0 -> ""
        minutes < 60 -> stringResource(R.string.title_runtime_minutes, minutes)
        minutes % 60 == 0 -> stringResource(R.string.title_runtime_hours, minutes / 60)
        else -> stringResource(R.string.title_runtime_hours_minutes, minutes / 60, minutes % 60)
    }

@Composable
fun seasonName(number: Int): String =
    if (number == 0) stringResource(R.string.title_specials) else stringResource(R.string.title_season_number, number)

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

fun formatBytes(
    bytes: Long,
    locale: Locale,
): String {
    if (bytes <= 0) return ""
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val format = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = if (value < 10 && unit > 0) 1 else 0 }
    return "${format.format(value)} ${units[unit]}"
}

fun formatMoney(
    amount: Long,
    locale: Locale,
): String {
    val format = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
    format.maximumFractionDigits = 1
    return "$" + format.format(amount)
}

fun localDate(iso: String): LocalDate? =
    try {
        if (iso.length > 10) OffsetDateTime.parse(iso).toLocalDate() else LocalDate.parse(iso.take(10))
    } catch (e: Exception) {
        null
    }

fun formatDate(
    iso: String,
    locale: Locale,
): String = localDate(iso)?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) ?: iso

fun today(): String = LocalDate.now().toString()

@Composable
fun airText(day: String): String {
    val loc = locale()
    val date = localDate(day) ?: return ""
    return when (TitleLogic.airWord(day, today())) {
        AirWord.TODAY -> stringResource(R.string.title_air_today)
        AirWord.YESTERDAY -> stringResource(R.string.title_air_yesterday)
        AirWord.TOMORROW -> stringResource(R.string.title_air_tomorrow)
        AirWord.WEEKDAY -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM", loc))
        AirWord.DATE -> date.format(DateTimeFormatter.ofPattern("d MMM", loc))
        AirWord.DATE_WITH_YEAR -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(loc))
        AirWord.NONE -> ""
    }
}

fun languageName(
    code: String,
    locale: Locale,
): String = if (code.isBlank()) "" else Locale.forLanguageTag(code).getDisplayLanguage(locale).ifBlank { code }

fun countryName(
    code: String,
    locale: Locale,
): String =
    Locale
        .Builder()
        .setRegion(code)
        .build()
        .getDisplayCountry(locale)
        .ifBlank { code }
