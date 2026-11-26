package ch.orellbuehler.acolyte.tv.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.feature.discover.CardAction
import ch.orellbuehler.acolyte.feature.discover.CardFresh
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardMark
import ch.orellbuehler.acolyte.feature.discover.CardNote
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.FreshKind
import ch.orellbuehler.acolyte.feature.discover.MarkLabel
import ch.orellbuehler.acolyte.feature.discover.NoteKind
import ch.orellbuehler.acolyte.feature.discover.genreSlug
import ch.orellbuehler.acolyte.tv.R

@Composable
fun tvMarkText(mark: CardMark): String =
    when (mark.label) {
        MarkLabel.IN_LIBRARY -> stringResource(R.string.tv_discover_mark_in_library)
        MarkLabel.PARTLY_AVAILABLE -> stringResource(R.string.tv_discover_mark_partly)
        MarkLabel.DOWNLOADING ->
            if (mark.percent >= 0) {
                stringResource(R.string.tv_discover_mark_downloading_percent, mark.percent)
            } else {
                stringResource(R.string.tv_discover_mark_downloading)
            }
        MarkLabel.REQUESTED -> stringResource(R.string.tv_discover_mark_requested)
        MarkLabel.APPROVED_WAITING -> stringResource(R.string.tv_discover_mark_approved_waiting)
        MarkLabel.WAITING_FOR_COPY -> stringResource(R.string.tv_discover_mark_waiting)
        MarkLabel.DECLINED -> stringResource(R.string.tv_discover_mark_declined)
        MarkLabel.HIDDEN -> stringResource(R.string.tv_discover_mark_hidden)
    }

@Composable
fun tvFreshText(fresh: CardFresh?): String? {
    fresh ?: return null
    return when {
        fresh.kind == FreshKind.NEW_EPISODE && fresh.text == "New Episode" -> stringResource(R.string.tv_discover_fresh_new_episode)
        fresh.kind == FreshKind.NEW_SEASON && fresh.text == "New Season" -> stringResource(R.string.tv_discover_fresh_new_season)
        fresh.kind == FreshKind.RECENTLY_ADDED && fresh.text == "Recently Added" ->
            stringResource(
                R.string.tv_discover_fresh_recently_added,
            )
        fresh.kind == FreshKind.NEW_RELEASE && fresh.text == "New Release" -> stringResource(R.string.tv_discover_fresh_new_release)
        fresh.text == "New series" -> stringResource(R.string.tv_discover_fresh_new_series)
        fresh.text == "New episodes" -> stringResource(R.string.tv_discover_fresh_new_episodes)
        else -> fresh.text
    }
}

@Composable
fun tvKindText(kind: CardKind): String =
    stringResource(
        when (kind) {
            CardKind.MOVIE -> R.string.tv_discover_kind_movie
            CardKind.SHOW -> R.string.tv_discover_kind_show
            CardKind.ARTIST -> R.string.tv_discover_kind_artist
        },
    )

@Composable
private fun noteText(note: CardNote): String =
    when (note.kind) {
        NoteKind.DOWNLOADING -> stringResource(R.string.tv_discover_note_downloading)
        NoteKind.MISSING_EPISODES -> pluralStringResource(R.plurals.tv_discover_note_missing_episodes, note.count, note.count)
        NoteKind.SEASONS_UP_TO_DATE -> pluralStringResource(R.plurals.tv_discover_note_seasons_up_to_date, note.count, note.count)
        NoteKind.MISSING_TRACKS -> pluralStringResource(R.plurals.tv_discover_note_missing_tracks, note.count, note.count)
        NoteKind.ALBUMS_UP_TO_DATE -> pluralStringResource(R.plurals.tv_discover_note_albums_up_to_date, note.count, note.count)
        NoteKind.DIRECTOR -> stringResource(R.string.tv_discover_role_director)
    }

@Composable
fun tvCardLine(
    card: DiscoverCard,
    typed: Boolean = false,
): String {
    val note = card.note?.let { noteText(it) }
    val detail =
        when {
            note != null && card.note?.kind == NoteKind.DIRECTOR && card.year > 0 -> "$note · ${card.year}"
            note != null -> note
            card.subtitle.isNotEmpty() && card.rowType == "film" && card.year > 0 -> "${card.subtitle} · ${card.year}"
            card.subtitle.isNotEmpty() -> tvServerWords(card.subtitle)
            card.year > 0 -> card.year.toString()
            else -> ""
        }
    if (!typed) return detail
    val type = tvKindText(card.kind)
    return if (detail.isEmpty() || detail == type) type else "$type · $detail"
}

@Composable
fun tvActionText(card: DiscoverCard): String? =
    when (card.action) {
        CardAction.ADD -> stringResource(R.string.tv_discover_action_add, card.name)
        CardAction.ASK -> stringResource(R.string.tv_discover_action_ask, card.name)
        null -> null
    }

@Composable
fun tvServerWords(text: String): String =
    when (text) {
        "Movie" -> stringResource(R.string.tv_discover_kind_movie)
        "Show" -> stringResource(R.string.tv_discover_kind_show)
        "Music" -> stringResource(R.string.tv_discover_kind_music)
        "New movie" -> stringResource(R.string.tv_discover_line_new_movie)
        "New show" -> stringResource(R.string.tv_discover_line_new_show)
        "New music" -> stringResource(R.string.tv_discover_line_new_music)
        else -> text
    }

@Composable
fun tvGenreText(name: String): String =
    when (genreSlug(name)) {
        "action" -> stringResource(R.string.tv_discover_genre_action)
        "adventure" -> stringResource(R.string.tv_discover_genre_adventure)
        "animation" -> stringResource(R.string.tv_discover_genre_animation)
        "comedy" -> stringResource(R.string.tv_discover_genre_comedy)
        "crime" -> stringResource(R.string.tv_discover_genre_crime)
        "documentary" -> stringResource(R.string.tv_discover_genre_documentary)
        "drama" -> stringResource(R.string.tv_discover_genre_drama)
        "family" -> stringResource(R.string.tv_discover_genre_family)
        "fantasy" -> stringResource(R.string.tv_discover_genre_fantasy)
        "history" -> stringResource(R.string.tv_discover_genre_history)
        "horror" -> stringResource(R.string.tv_discover_genre_horror)
        "music" -> stringResource(R.string.tv_discover_genre_music)
        "mystery" -> stringResource(R.string.tv_discover_genre_mystery)
        "romance" -> stringResource(R.string.tv_discover_genre_romance)
        "science_fiction" -> stringResource(R.string.tv_discover_genre_science_fiction)
        "thriller" -> stringResource(R.string.tv_discover_genre_thriller)
        "war" -> stringResource(R.string.tv_discover_genre_war)
        "western" -> stringResource(R.string.tv_discover_genre_western)
        "tv_movie" -> stringResource(R.string.tv_discover_genre_tv_movie)
        "action_adventure" -> stringResource(R.string.tv_discover_genre_action_adventure)
        "kids" -> stringResource(R.string.tv_discover_genre_kids)
        "reality" -> stringResource(R.string.tv_discover_genre_reality)
        "sci_fi_fantasy" -> stringResource(R.string.tv_discover_genre_sci_fi_fantasy)
        "war_politics" -> stringResource(R.string.tv_discover_genre_war_politics)
        "news" -> stringResource(R.string.tv_discover_genre_news)
        "soap" -> stringResource(R.string.tv_discover_genre_soap)
        "talk" -> stringResource(R.string.tv_discover_genre_talk)
        else -> name
    }
