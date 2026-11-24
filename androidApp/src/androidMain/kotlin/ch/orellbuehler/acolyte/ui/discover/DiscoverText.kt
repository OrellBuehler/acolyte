package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.discover.CardAction
import ch.orellbuehler.acolyte.feature.discover.CardFresh
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardMark
import ch.orellbuehler.acolyte.feature.discover.CardNote
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.ExplorePill
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.FreshKind
import ch.orellbuehler.acolyte.feature.discover.MarkLabel
import ch.orellbuehler.acolyte.feature.discover.NoteKind
import ch.orellbuehler.acolyte.feature.discover.TitleStatus
import ch.orellbuehler.acolyte.feature.discover.genreSlug

@Composable
fun markText(mark: CardMark): String =
    when (mark.label) {
        MarkLabel.IN_LIBRARY -> stringResource(R.string.discover_mark_in_library)
        MarkLabel.PARTLY_AVAILABLE -> stringResource(R.string.discover_mark_partly)
        MarkLabel.DOWNLOADING ->
            if (mark.percent >= 0) {
                stringResource(R.string.discover_mark_downloading_percent, mark.percent)
            } else {
                stringResource(R.string.discover_mark_downloading)
            }
        MarkLabel.REQUESTED -> stringResource(R.string.discover_mark_requested)
        MarkLabel.APPROVED_WAITING -> stringResource(R.string.discover_mark_approved_waiting)
        MarkLabel.WAITING_FOR_COPY -> stringResource(R.string.discover_mark_waiting)
        MarkLabel.DECLINED -> stringResource(R.string.discover_mark_declined)
        MarkLabel.HIDDEN -> stringResource(R.string.discover_mark_hidden)
    }

@Composable
fun freshText(fresh: CardFresh?): String? {
    fresh ?: return null
    return when {
        fresh.kind == FreshKind.NEW_EPISODE && fresh.text == "New Episode" -> stringResource(R.string.discover_fresh_new_episode)
        fresh.kind == FreshKind.NEW_SEASON && fresh.text == "New Season" -> stringResource(R.string.discover_fresh_new_season)
        fresh.kind == FreshKind.RECENTLY_ADDED && fresh.text == "Recently Added" -> stringResource(R.string.discover_fresh_recently_added)
        fresh.kind == FreshKind.NEW_RELEASE && fresh.text == "New Release" -> stringResource(R.string.discover_fresh_new_release)
        fresh.text == "New series" -> stringResource(R.string.discover_fresh_new_series)
        fresh.text == "New episodes" -> stringResource(R.string.discover_fresh_new_episodes)
        else -> fresh.text
    }
}

@Composable
fun kindText(kind: CardKind): String =
    stringResource(
        when (kind) {
            CardKind.MOVIE -> R.string.discover_kind_movie
            CardKind.SHOW -> R.string.discover_kind_show
            CardKind.ARTIST -> R.string.discover_kind_artist
        },
    )

@Composable
fun noteText(note: CardNote): String =
    when (note.kind) {
        NoteKind.DOWNLOADING -> stringResource(R.string.discover_note_downloading)
        NoteKind.MISSING_EPISODES -> pluralStringResource(R.plurals.discover_note_missing_episodes, note.count, note.count)
        NoteKind.SEASONS_UP_TO_DATE -> pluralStringResource(R.plurals.discover_note_seasons_up_to_date, note.count, note.count)
        NoteKind.MISSING_TRACKS -> pluralStringResource(R.plurals.discover_note_missing_tracks, note.count, note.count)
        NoteKind.ALBUMS_UP_TO_DATE -> pluralStringResource(R.plurals.discover_note_albums_up_to_date, note.count, note.count)
        NoteKind.DIRECTOR -> stringResource(R.string.discover_role_director)
    }

@Composable
fun cardLine(
    card: DiscoverCard,
    typed: Boolean = false,
): String {
    val note = card.note?.let { noteText(it) }
    val detail =
        when {
            note != null && card.note?.kind == NoteKind.DIRECTOR && card.year > 0 -> "$note · ${card.year}"
            note != null -> note
            card.subtitle.isNotEmpty() && card.rowType == "film" && card.year > 0 -> "${card.subtitle} · ${card.year}"
            card.subtitle.isNotEmpty() -> serverWords(card.subtitle)
            card.year > 0 -> card.year.toString()
            else -> ""
        }
    if (!typed) return detail
    val type = kindText(card.kind)
    return if (detail.isEmpty() || detail == type) type else "$type · $detail"
}

@Composable
fun actionText(card: DiscoverCard): String? =
    when (card.action) {
        CardAction.ADD -> stringResource(R.string.discover_action_add, card.name)
        CardAction.ASK -> stringResource(R.string.discover_action_ask, card.name)
        null -> null
    }

@Composable
fun typeText(type: ExploreType): String =
    stringResource(
        when (type) {
            ExploreType.ALL -> R.string.discover_type_all
            ExploreType.MOVIES -> R.string.discover_type_movies
            ExploreType.SHOWS -> R.string.discover_type_shows
        },
    )

@Composable
fun heroPillText(
    pill: ExplorePill,
    rank: Int = 0,
    service: String = "",
): String =
    when (pill) {
        ExplorePill.NEW_SEASON -> stringResource(R.string.discover_pill_new_season)
        ExplorePill.PICKED_FOR_YOU -> stringResource(R.string.discover_pill_picked)
        ExplorePill.NEW_THIS_WEEK -> stringResource(R.string.discover_pill_new_week)
        ExplorePill.RANK -> stringResource(R.string.discover_pill_rank, rank, service)
    }

@Composable
fun statusText(
    status: TitleStatus,
    staff: Boolean,
): String =
    stringResource(
        when (status) {
            TitleStatus.IN_LIBRARY -> if (staff) R.string.discover_status_in_library else R.string.discover_status_ready
            TitleStatus.DOWNLOADING -> R.string.discover_status_downloading
            TitleStatus.MISSING -> if (staff) R.string.discover_status_missing else R.string.discover_status_get_it
            TitleStatus.REQUESTED -> if (staff) R.string.discover_status_waiting else R.string.discover_status_requested
            TitleStatus.APPROVED -> R.string.discover_status_approved
            TitleStatus.TURNED_DOWN -> R.string.discover_status_declined
            TitleStatus.FAILED -> R.string.discover_status_failed
            TitleStatus.COMING_SOON -> R.string.discover_status_coming_soon
        },
    )

@Composable
fun serverWords(text: String): String =
    when (text) {
        "Movie" -> stringResource(R.string.discover_kind_movie)
        "Show" -> stringResource(R.string.discover_kind_show)
        "Music" -> stringResource(R.string.discover_kind_music)
        "New movie" -> stringResource(R.string.discover_line_new_movie)
        "New show" -> stringResource(R.string.discover_line_new_show)
        "New music" -> stringResource(R.string.discover_line_new_music)
        else -> text
    }

@Composable
fun genreText(name: String): String =
    when (genreSlug(name)) {
        "action" -> stringResource(R.string.discover_genre_action)
        "adventure" -> stringResource(R.string.discover_genre_adventure)
        "animation" -> stringResource(R.string.discover_genre_animation)
        "comedy" -> stringResource(R.string.discover_genre_comedy)
        "crime" -> stringResource(R.string.discover_genre_crime)
        "documentary" -> stringResource(R.string.discover_genre_documentary)
        "drama" -> stringResource(R.string.discover_genre_drama)
        "family" -> stringResource(R.string.discover_genre_family)
        "fantasy" -> stringResource(R.string.discover_genre_fantasy)
        "history" -> stringResource(R.string.discover_genre_history)
        "horror" -> stringResource(R.string.discover_genre_horror)
        "music" -> stringResource(R.string.discover_genre_music)
        "mystery" -> stringResource(R.string.discover_genre_mystery)
        "romance" -> stringResource(R.string.discover_genre_romance)
        "science_fiction" -> stringResource(R.string.discover_genre_science_fiction)
        "thriller" -> stringResource(R.string.discover_genre_thriller)
        "war" -> stringResource(R.string.discover_genre_war)
        "western" -> stringResource(R.string.discover_genre_western)
        "tv_movie" -> stringResource(R.string.discover_genre_tv_movie)
        "action_adventure" -> stringResource(R.string.discover_genre_action_adventure)
        "kids" -> stringResource(R.string.discover_genre_kids)
        "reality" -> stringResource(R.string.discover_genre_reality)
        "sci_fi_fantasy" -> stringResource(R.string.discover_genre_sci_fi_fantasy)
        "war_politics" -> stringResource(R.string.discover_genre_war_politics)
        "news" -> stringResource(R.string.discover_genre_news)
        "soap" -> stringResource(R.string.discover_genre_soap)
        "talk" -> stringResource(R.string.discover_genre_talk)
        else -> name
    }

@Composable
fun sectionLabel(section: String): String? =
    when (section) {
        "genres" -> stringResource(R.string.discover_strip_genre)
        "moods" -> stringResource(R.string.discover_strip_mood)
        "stories" -> stringResource(R.string.discover_strip_story)
        "worlds" -> stringResource(R.string.discover_strip_world)
        "eras" -> stringResource(R.string.discover_strip_era)
        "world" -> stringResource(R.string.discover_strip_around)
        "collections" -> stringResource(R.string.discover_strip_collection)
        else -> null
    }

@Composable
fun browseSectionTitle(
    id: String,
    fallback: String,
): String =
    when (id) {
        "genres" -> stringResource(R.string.discover_browse_genres)
        "moods" -> stringResource(R.string.discover_browse_moods)
        "stories" -> stringResource(R.string.discover_browse_stories)
        "worlds" -> stringResource(R.string.discover_browse_worlds)
        "eras" -> stringResource(R.string.discover_browse_eras)
        "world" -> stringResource(R.string.discover_browse_world)
        "collections" -> stringResource(R.string.discover_browse_collections)
        else -> fallback
    }
