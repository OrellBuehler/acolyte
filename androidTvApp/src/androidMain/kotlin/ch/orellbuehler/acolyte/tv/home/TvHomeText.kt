package ch.orellbuehler.acolyte.tv.home

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.feature.home.ArrivedTitle
import ch.orellbuehler.acolyte.feature.home.ComingUp
import ch.orellbuehler.acolyte.feature.home.HomePicture
import ch.orellbuehler.acolyte.feature.home.PictureLabel
import ch.orellbuehler.acolyte.feature.home.UpWhen
import ch.orellbuehler.acolyte.tv.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun tvArrivedTitle(title: ArrivedTitle): String =
    stringResource(
        when (title) {
            ArrivedTitle.NEW_TODAY -> R.string.tv_home_new_today
            ArrivedTitle.NEW_THIS_WEEK -> R.string.tv_home_new_this_week
            ArrivedTitle.JUST_ARRIVED -> R.string.tv_home_just_arrived
            ArrivedTitle.RECENTLY_ADDED -> R.string.tv_home_recently_added
        },
    )

@Composable
fun tvPictureLabel(picture: HomePicture): String =
    when (picture.label) {
        PictureLabel.NEW_SEASON -> stringResource(R.string.tv_home_pill_new_season)
        PictureLabel.COMING_SOON -> stringResource(R.string.tv_home_pill_coming_soon)
        PictureLabel.FOLLOWING -> stringResource(R.string.tv_home_pill_following)
        PictureLabel.ARRIVED -> tvArrivedTitle(picture.arrived)
    }

@Composable
fun tvUpWhen(up: ComingUp): String =
    when (up.whenKind) {
        UpWhen.OUT_NOW -> stringResource(R.string.tv_home_out_now)
        UpWhen.TODAY -> stringResource(R.string.tv_discover_day_today)
        UpWhen.DATE -> runCatching { shortDate(LocalDate.parse(up.date)) }.getOrDefault(up.date)
        UpWhen.TEXT -> up.text
    }

fun shortDate(date: LocalDate): String {
    val locale = Locale.getDefault()
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale))
}
