package ch.orellbuehler.acolyte.tv.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.feature.discover.ExplorePill
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.TopServices
import ch.orellbuehler.acolyte.feature.discover.TopSource
import ch.orellbuehler.acolyte.tv.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun tvTypeText(type: ExploreType): String =
    stringResource(
        when (type) {
            ExploreType.ALL -> R.string.tv_discover_type_all
            ExploreType.MOVIES -> R.string.tv_discover_type_movies
            ExploreType.SHOWS -> R.string.tv_discover_type_shows
        },
    )

@Composable
fun tvHeroPillText(
    pill: ExplorePill,
    rank: Int = 0,
    service: String = "",
): String =
    when (pill) {
        ExplorePill.NEW_SEASON -> stringResource(R.string.tv_discover_pill_new_season)
        ExplorePill.PICKED_FOR_YOU -> stringResource(R.string.tv_discover_pill_picked)
        ExplorePill.NEW_THIS_WEEK -> stringResource(R.string.tv_discover_pill_new_week)
        ExplorePill.RANK -> stringResource(R.string.tv_discover_pill_rank, rank, service)
    }

@Composable
fun tvTopTitle(
    type: ExploreType,
    genre: String?,
): String =
    when {
        genre != null -> stringResource(R.string.tv_discover_top_genre, genre)
        type == ExploreType.MOVIES -> stringResource(R.string.tv_discover_top_movies)
        type == ExploreType.SHOWS -> stringResource(R.string.tv_discover_top_shows)
        else -> stringResource(R.string.tv_discover_top_all)
    }

@Composable
fun tvTopSource(top: TopServices): String =
    when (top.source) {
        TopSource.TODAY -> stringResource(R.string.tv_discover_top_today, top.serviceName)
        TopSource.AS_OF -> stringResource(R.string.tv_discover_top_as_of, top.serviceName, top.fetchedAt.take(DATE_LENGTH))
        TopSource.POPULAR -> stringResource(R.string.tv_discover_top_popular, top.serviceName)
    }

@Composable
fun tvStripLabel(section: String): String? =
    when (section) {
        "genres" -> stringResource(R.string.tv_discover_strip_genre)
        "moods" -> stringResource(R.string.tv_discover_strip_mood)
        "stories" -> stringResource(R.string.tv_discover_strip_story)
        "worlds" -> stringResource(R.string.tv_discover_strip_world)
        "eras" -> stringResource(R.string.tv_discover_strip_era)
        "world" -> stringResource(R.string.tv_discover_strip_around)
        "collections" -> stringResource(R.string.tv_discover_strip_collection)
        else -> null
    }

@Composable
fun tvBrowseSectionTitle(
    id: String,
    fallback: String,
): String =
    when (id) {
        "genres" -> stringResource(R.string.tv_discover_browse_genres)
        "moods" -> stringResource(R.string.tv_discover_browse_moods)
        "stories" -> stringResource(R.string.tv_discover_browse_stories)
        "worlds" -> stringResource(R.string.tv_discover_browse_worlds)
        "eras" -> stringResource(R.string.tv_discover_browse_eras)
        "world" -> stringResource(R.string.tv_discover_browse_world)
        "collections" -> stringResource(R.string.tv_discover_browse_collections)
        else -> fallback
    }

fun tvShortDay(day: String): String = dayFormat(day, "EEEMMMd")

fun tvMonthDay(day: String): String = dayFormat(day, "MMMd")

fun tvWeekday(day: String): String = dayFormat(day, "EEEE")

private fun dayFormat(
    day: String,
    skeleton: String,
): String {
    val locale = Locale.getDefault()
    return runCatching {
        LocalDate
            .parse(
                day.take(DATE_LENGTH),
            ).format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
    }.getOrDefault(day)
}

fun tvLongDate(day: String): String? =
    runCatching {
        val locale = Locale.getDefault()
        LocalDate
            .parse(
                day.take(DATE_LENGTH),
            ).format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "yMMMMd"), locale))
    }.getOrNull()

private const val DATE_LENGTH = 10
