package ch.orellbuehler.acolyte.tv.title

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.feature.title.AirWord
import ch.orellbuehler.acolyte.feature.title.HeroPill
import ch.orellbuehler.acolyte.feature.title.LibraryLabel
import ch.orellbuehler.acolyte.feature.title.LibraryStatus
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.tv.R
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun tvLibraryLabel(status: LibraryStatus): String =
    when (status.label) {
        LibraryLabel.IN_LIBRARY -> stringResource(R.string.tv_title_state_in_library)
        LibraryLabel.DOWNLOADED -> stringResource(R.string.tv_title_state_downloaded)
        LibraryLabel.DOWNLOADING -> stringResource(R.string.tv_title_state_downloading)
        LibraryLabel.DOWNLOADING_PERCENT -> stringResource(R.string.tv_title_state_downloading_percent, status.count)
        LibraryLabel.DOWNLOADING_EPISODES ->
            pluralStringResource(R.plurals.tv_title_state_downloading_episodes, status.count, status.count)
        LibraryLabel.FAILED_EPISODES -> pluralStringResource(R.plurals.tv_title_state_failed_episodes, status.count, status.count)
        LibraryLabel.EPISODES_OF -> stringResource(R.string.tv_title_state_episodes_of, status.count, status.total)
        LibraryLabel.DOWNLOAD_FAILED -> stringResource(R.string.tv_title_state_download_failed)
        LibraryLabel.NOT_DOWNLOADED -> stringResource(R.string.tv_title_state_not_downloaded)
    }

@Composable
fun tvPillText(
    pill: HeroPill,
    mine: Boolean,
): String? {
    val base =
        when (pill) {
            HeroPill.NONE -> return null
            HeroPill.HIDDEN -> return stringResource(R.string.tv_title_pill_hidden)
            HeroPill.NEVER_ADD -> return stringResource(R.string.tv_title_pill_never_add)
            HeroPill.ASKED -> stringResource(R.string.tv_title_pill_asked)
            HeroPill.APPROVED -> stringResource(R.string.tv_title_pill_approved)
            HeroPill.TURNED_DOWN -> stringResource(R.string.tv_title_pill_turned_down)
            HeroPill.AVAILABLE -> stringResource(R.string.tv_title_pill_available)
        }
    return if (mine) stringResource(R.string.tv_title_pill_you_asked, base) else base
}

@Composable
fun tvRuntimeText(minutes: Int): String =
    when {
        minutes <= 0 -> ""
        minutes < 60 -> stringResource(R.string.tv_title_runtime_minutes, minutes)
        minutes % 60 == 0 -> stringResource(R.string.tv_title_runtime_hours, minutes / 60)
        else -> stringResource(R.string.tv_title_runtime_hours_minutes, minutes / 60, minutes % 60)
    }

@Composable
fun tvSeasonName(number: Int): String =
    if (number == 0) stringResource(R.string.tv_title_specials) else stringResource(R.string.tv_title_season_number, number)

@Composable
fun tvAirText(day: String): String {
    val locale = LocalLocale.current.platformLocale
    val date = tvLocalDate(day) ?: return ""
    return when (TitleLogic.airWord(day, LocalDate.now().toString())) {
        AirWord.TODAY -> stringResource(R.string.tv_title_air_today)
        AirWord.YESTERDAY -> stringResource(R.string.tv_title_air_yesterday)
        AirWord.TOMORROW -> stringResource(R.string.tv_title_air_tomorrow)
        AirWord.WEEKDAY -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
        AirWord.DATE -> date.format(DateTimeFormatter.ofPattern("d MMM", locale))
        AirWord.DATE_WITH_YEAR -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        AirWord.NONE -> ""
    }
}

fun tvLocalDate(iso: String): LocalDate? =
    try {
        if (iso.length > 10) OffsetDateTime.parse(iso).toLocalDate() else LocalDate.parse(iso.take(10))
    } catch (e: Exception) {
        null
    }
