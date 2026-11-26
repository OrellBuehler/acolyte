package ch.orellbuehler.acolyte.tv.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.feature.discover.FilmRole
import ch.orellbuehler.acolyte.feature.discover.FilmSort
import ch.orellbuehler.acolyte.tv.R
import java.time.LocalDate
import java.time.Period

@Composable
fun tvRoleWord(role: String): String =
    when (role) {
        "Actor" -> stringResource(R.string.tv_discover_role_actor)
        "Director" -> stringResource(R.string.tv_discover_role_director)
        else -> role
    }

@Composable
fun tvRoleText(role: FilmRole): String =
    stringResource(
        when (role) {
            FilmRole.ALL -> R.string.tv_discover_type_all
            FilmRole.ACTOR -> R.string.tv_discover_role_actor
            FilmRole.DIRECTOR -> R.string.tv_discover_role_director
        },
    )

@Composable
fun tvSortText(sort: FilmSort): String =
    stringResource(
        when (sort) {
            FilmSort.OLDEST -> R.string.tv_discover_sort_oldest
            FilmSort.NEWEST -> R.string.tv_discover_sort_newest
            FilmSort.POPULAR -> R.string.tv_discover_sort_popular
        },
    )

fun tvDate(text: String): LocalDate? = runCatching { LocalDate.parse(text.take(DATE_LENGTH)) }.getOrNull()

fun tvYears(
    born: LocalDate,
    until: LocalDate?,
): Int = Period.between(born, until ?: LocalDate.now()).years

fun tvBioChunks(text: String): List<String> {
    val chunks = mutableListOf<String>()
    text.split(Regex("\\n\\s*\\n|\\n")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { paragraph ->
        var current = ""
        paragraph.split(Regex("(?<=[.!?])\\s+")).forEach { sentence ->
            if (current.isNotEmpty() && current.length + sentence.length > CHUNK) {
                chunks += current
                current = sentence
            } else {
                current = if (current.isEmpty()) sentence else "$current $sentence"
            }
        }
        if (current.isNotEmpty()) chunks += current
    }
    return chunks
}

private const val CHUNK = 420
private const val DATE_LENGTH = 10
