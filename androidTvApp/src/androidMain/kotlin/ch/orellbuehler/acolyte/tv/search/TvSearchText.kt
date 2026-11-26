package ch.orellbuehler.acolyte.tv.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.PeopleFilter
import ch.orellbuehler.acolyte.feature.discover.SearchResults
import ch.orellbuehler.acolyte.feature.discover.SearchScope
import ch.orellbuehler.acolyte.feature.discover.Understood
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.tvGenreText

@Composable
fun tvScopeText(scope: SearchScope): String =
    stringResource(
        when (scope) {
            SearchScope.ALL -> R.string.tv_discover_type_all
            SearchScope.PEOPLE -> R.string.tv_discover_scope_people
            SearchScope.MOVIE -> R.string.tv_discover_type_movies
            SearchScope.SHOW -> R.string.tv_discover_type_shows
            SearchScope.MUSIC -> R.string.tv_section_music
        },
    )

@Composable
fun tvPeopleFilterText(filter: PeopleFilter): String =
    stringResource(
        when (filter) {
            PeopleFilter.POPULAR -> R.string.tv_discover_people_popular
            PeopleFilter.ACTORS -> R.string.tv_discover_people_actors
            PeopleFilter.DIRECTORS -> R.string.tv_discover_people_directors
            PeopleFilter.COMPOSERS -> R.string.tv_discover_people_composers
        },
    )

@Composable
fun tvUnderstoodText(understood: Understood): String {
    val kind =
        when {
            understood.music -> stringResource(R.string.tv_discover_kind_music)
            understood.kind == CardKind.MOVIE -> stringResource(R.string.tv_discover_type_movies)
            understood.kind == CardKind.SHOW -> stringResource(R.string.tv_discover_type_shows)
            else -> stringResource(R.string.tv_discover_plain_titles)
        }
    val or = stringResource(R.string.tv_discover_plain_or)
    val genres = understood.genres.map { tvGenreText(it) }.joinToString(or)
    val head = listOf(genres, kind).filter { it.isNotEmpty() }.joinToString(" ")
    val years =
        when {
            understood.yearFrom > 0 && understood.yearFrom == understood.yearTo -> understood.yearFrom.toString()
            understood.yearFrom > 0 && understood.yearTo > 0 -> "${understood.yearFrom}–${understood.yearTo}"
            understood.yearFrom > 0 -> stringResource(R.string.tv_discover_plain_from, understood.yearFrom)
            understood.yearTo > 0 -> stringResource(R.string.tv_discover_plain_to, understood.yearTo)
            else -> ""
        }
    val parts =
        listOf(
            if (understood.name.isNotEmpty()) "“${understood.name}”" else "",
            head,
            years,
            if (understood.people.isNotEmpty()) {
                stringResource(
                    R.string.tv_discover_plain_with,
                    understood.people.joinToString(", "),
                )
            } else {
                ""
            },
            if (understood.words.isNotEmpty()) {
                stringResource(
                    R.string.tv_discover_plain_about,
                    understood.words.joinToString(", "),
                )
            } else {
                ""
            },
        )
    return parts.filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
fun tvSearchRows(results: SearchResults): Map<String, CardRow> {
    val library = stringResource(R.string.tv_discover_search_in_library)
    val groups =
        listOf(
            Triple("library", library, results.library),
            Triple("movies", stringResource(R.string.tv_discover_type_movies), results.movies),
            Triple("shows", stringResource(R.string.tv_discover_type_shows), results.shows),
            Triple("plainLibrary", library, results.plainLibrary),
            Triple("more", stringResource(R.string.tv_discover_plain_more), results.more),
        )
    return groups.filter { it.third.isNotEmpty() }.associate { (id, title, cards) -> id to CardRow(id, id, title, cards = cards) }
}
