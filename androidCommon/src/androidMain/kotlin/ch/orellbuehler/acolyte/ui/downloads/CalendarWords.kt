package ch.orellbuehler.acolyte.ui.downloads

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.common.R
import ch.orellbuehler.acolyte.feature.downloads.CalendarFollows
import ch.orellbuehler.acolyte.feature.downloads.CalendarGroup
import ch.orellbuehler.acolyte.feature.downloads.CalendarNotice
import ch.orellbuehler.acolyte.feature.downloads.CalendarNoticeKind
import ch.orellbuehler.acolyte.feature.downloads.CalendarType
import ch.orellbuehler.acolyte.feature.downloads.Day
import ch.orellbuehler.acolyte.feature.downloads.EntryDot
import ch.orellbuehler.acolyte.feature.downloads.EntryTag
import ch.orellbuehler.acolyte.feature.downloads.EntryWhen
import ch.orellbuehler.acolyte.feature.downloads.EpisodeSpan
import ch.orellbuehler.acolyte.feature.downloads.Month
import ch.orellbuehler.acolyte.feature.downloads.MovieDateKind
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun tagText(g: CalendarGroup): String =
    when (g.tag) {
        EntryTag.SHOW_PREMIERE -> stringResource(R.string.downloads_calendar_tag_show_premiere)
        EntryTag.SEASON_PREMIERE -> stringResource(R.string.downloads_calendar_tag_season_premiere)
        EntryTag.NEW_EPISODE -> stringResource(R.string.downloads_calendar_tag_new_episode)
        EntryTag.NEW_ALBUM -> stringResource(R.string.downloads_calendar_tag_new_album)
        EntryTag.NEW_SINGLE -> stringResource(R.string.downloads_calendar_tag_new_single)
        EntryTag.NEW_EP -> stringResource(R.string.downloads_calendar_tag_new_ep)
        EntryTag.IN_CINEMAS -> stringResource(R.string.downloads_calendar_tag_in_cinemas)
        EntryTag.NEW_MOVIE -> stringResource(R.string.downloads_calendar_tag_new_movie)
        EntryTag.EPISODES -> stringResource(R.string.downloads_calendar_tag_episodes, g.count)
        EntryTag.ALBUMS -> stringResource(R.string.downloads_calendar_tag_albums, g.count)
        EntryTag.MOVIE_DATES -> {
            val words =
                g.movieKinds.map {
                    stringResource(
                        when (it) {
                            MovieDateKind.CINEMA -> R.string.downloads_calendar_kind_cinema
                            MovieDateKind.DIGITAL -> R.string.downloads_calendar_kind_digital
                            MovieDateKind.DISC -> R.string.downloads_calendar_kind_disc
                        },
                    )
                }
            if (words.size >
                1
            ) {
                stringResource(R.string.downloads_calendar_and, words.dropLast(1).joinToString(", "), words.last().lowercase())
            } else {
                words.firstOrNull().orEmpty()
            }
        }
    }

@Composable
fun whenText(g: CalendarGroup): String =
    when (g.whenKind) {
        EntryWhen.TIME -> LocalTime.of(g.minutes / 60, g.minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        EntryWhen.ALL_DAY -> stringResource(R.string.downloads_calendar_all_day)
        EntryWhen.ALBUM -> stringResource(R.string.downloads_calendar_album)
        EntryWhen.SINGLE -> stringResource(R.string.downloads_calendar_single)
        EntryWhen.EP -> stringResource(R.string.downloads_calendar_ep)
        EntryWhen.IN_THEATERS -> stringResource(R.string.downloads_calendar_in_theaters)
        EntryWhen.STREAMING -> stringResource(R.string.downloads_calendar_streaming)
    }

@Composable
fun episodesText(g: CalendarGroup): String =
    when (g.span) {
        EpisodeSpan.ONE -> stringResource(R.string.downloads_calendar_season_episode, g.season, g.episode)
        EpisodeSpan.SAME_SEASON -> stringResource(R.string.downloads_calendar_season_episodes, g.season, g.episode, g.episode2)
        EpisodeSpan.ACROSS -> stringResource(R.string.downloads_calendar_season_span, g.season, g.episode, g.season2, g.episode2)
    }

@Composable
fun dotText(dot: EntryDot): String =
    stringResource(
        when (dot) {
            EntryDot.HAVE -> R.string.downloads_calendar_dot_have
            EntryDot.GETTING -> R.string.downloads_calendar_dot_getting
            EntryDot.PROBLEM -> R.string.downloads_calendar_dot_problem
            EntryDot.MISSING -> R.string.downloads_calendar_dot_missing
            EntryDot.UPCOMING -> R.string.downloads_calendar_dot_upcoming
        },
    )

@Composable
fun captionLines(g: CalendarGroup): List<String> =
    when (g.kind) {
        "episode" -> listOf(episodesText(g), g.episodeName, g.network)
        "album" -> listOf(g.lead.name, g.album.takeIf { it != g.title }.orEmpty())
        else -> listOf(g.caption)
    }.filter { it.isNotBlank() }

@Composable
fun calendarNoticeText(notice: CalendarNotice): String =
    when (notice.kind) {
        CalendarNoticeKind.FOLLOWED -> stringResource(R.string.downloads_calendar_followed, notice.name)
        CalendarNoticeKind.UNFOLLOWED -> stringResource(R.string.downloads_calendar_unfollowed, notice.name)
        CalendarNoticeKind.SEARCHING -> stringResource(R.string.downloads_calendar_searching, notice.name)
        CalendarNoticeKind.NOTHING_TO_SEARCH -> stringResource(R.string.downloads_calendar_nothing_to_search)
        CalendarNoticeKind.SEARCHING_MISSING -> stringResource(R.string.downloads_calendar_searching_missing, notice.count)
    }

@Composable
fun calendarMonthTitle(month: Month): String {
    val locale = LocalConfiguration.current.locales[0]
    return YearMonth
        .of(month.year, month.month)
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar { it.uppercase() }
}

@Composable
fun calendarDayHeading(
    day: Day,
    today: Day,
): String = if (day == today) stringResource(R.string.downloads_day_today) else dayText(day)

@Composable
fun calendarTypeLabel(type: CalendarType): String =
    stringResource(
        when (type) {
            CalendarType.ALL -> R.string.downloads_calendar_all
            CalendarType.MOVIES -> R.string.downloads_calendar_movies
            CalendarType.SHOWS -> R.string.downloads_calendar_shows
            CalendarType.MUSIC -> R.string.downloads_calendar_music
        },
    )

@Composable
fun calendarFollowsLabel(follows: CalendarFollows): String =
    stringResource(
        when (follows) {
            CalendarFollows.MINE -> R.string.downloads_calendar_mine
            CalendarFollows.EVERYONE -> R.string.downloads_calendar_everyone
            CalendarFollows.LIBRARY -> R.string.downloads_calendar_library
        },
    )
