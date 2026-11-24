package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.session.SecureStore
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

enum class ExploreType(
    val wire: String,
) {
    ALL("all"),
    MOVIES("movies"),
    SHOWS("shows"),
    ;

    internal fun keeps(kind: CardKind): Boolean =
        when (this) {
            ALL -> true
            MOVIES -> kind == CardKind.MOVIE
            SHOWS -> kind == CardKind.SHOW
        }

    companion object {
        fun of(wire: String): ExploreType = entries.firstOrNull { it.wire == wire } ?: ALL
    }
}

data class Genre(
    val id: Int,
    val name: String,
    val kind: CardKind,
) {
    val slug: String get() = genreSlug(name)
}

fun genreSlug(name: String): String = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

object Genres {
    val movies: List<Genre> =
        listOf(
            28 to "Action",
            12 to "Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            14 to "Fantasy",
            36 to "History",
            27 to "Horror",
            10402 to "Music",
            9648 to "Mystery",
            10749 to "Romance",
            878 to "Science Fiction",
            53 to "Thriller",
            10752 to "War",
            37 to "Western",
        ).map { Genre(it.first, it.second, CardKind.MOVIE) }

    val shows: List<Genre> =
        listOf(
            10759 to "Action & Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            10762 to "Kids",
            9648 to "Mystery",
            10764 to "Reality",
            10765 to "Sci-Fi & Fantasy",
            10768 to "War & Politics",
            37 to "Western",
        ).map { Genre(it.first, it.second, CardKind.SHOW) }

    val hideMovies: List<String> = movies.map { it.name } + "TV Movie"

    val hideShows: List<String> = shows.map { it.name } + listOf("News", "Soap", "Talk")

    internal val mergedUnderAll: Set<Int> = setOf(878, 14, 28, 12, 10752)

    fun of(type: ExploreType): List<Genre> =
        when (type) {
            ExploreType.MOVIES -> movies
            ExploreType.SHOWS -> shows
            ExploreType.ALL -> movies.filter { it.id in mergedUnderAll }
        }

    fun named(
        id: Int,
        type: ExploreType,
    ): Genre? =
        when (type) {
            ExploreType.SHOWS -> shows.firstOrNull { it.id == id }
            else -> movies.firstOrNull { it.id == id } ?: shows.firstOrNull { it.id == id }
        }

    internal fun idOf(
        name: String,
        kind: CardKind,
    ): Int? = (if (kind == CardKind.SHOW) shows else movies).firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
}

enum class FilmRole(
    val wire: String,
) {
    ALL("all"),
    ACTOR("actor"),
    DIRECTOR("director"),
}

enum class FilmSort(
    val wire: String,
) {
    OLDEST("oldest"),
    NEWEST("newest"),
    POPULAR("popular"),
}

internal class DiscoverPrefs(
    private val store: SecureStore?,
    private val who: () -> String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun read(key: String): String? =
        try {
            store?.read("discover.${who()}.$key")
        } catch (e: Exception) {
            null
        }

    private fun write(
        key: String,
        value: String,
    ) {
        try {
            store?.write("discover.${who()}.$key", value)
        } catch (e: Exception) {
            return
        }
    }

    var exploreType: ExploreType
        get() = ExploreType.of(read("explore.type").orEmpty())
        set(value) = write("explore.type", value.wire)

    var onlyLibrary: Boolean
        get() = read("search.only_library") == "1"
        set(value) = write("search.only_library", if (value) "1" else "0")

    var filmSort: FilmSort
        get() = FilmSort.entries.firstOrNull { it.wire == read("people.sort") } ?: FilmSort.OLDEST
        set(value) = write("people.sort", value.wire)

    var heroVisit: Int
        get() = read("popular.hero").orEmpty().toIntOrNull() ?: 0
        set(value) = write("popular.hero", value.toString())

    var recents: List<String>
        get() =
            try {
                read("search.recent")?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }.orEmpty()
            } catch (e: IllegalArgumentException) {
                emptyList()
            }
        set(value) = write("search.recent", json.encodeToString(ListSerializer(String.serializer()), value))
}
