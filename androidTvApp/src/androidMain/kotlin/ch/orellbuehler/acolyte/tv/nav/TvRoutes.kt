package ch.orellbuehler.acolyte.tv.nav

import kotlinx.serialization.Serializable

@Serializable
data object TabsRoute

@Serializable
data class TitleRoute(
    val kind: String,
    val tmdb: String,
    val libraryId: Long = 0,
)

@Serializable
data class PersonRoute(
    val id: String,
)

@Serializable
data class CollectionRoute(
    val id: String,
)

@Serializable
data class GenreRoute(
    val type: String,
    val genre: Int,
)

@Serializable
data class BrowseRoute(
    val target: String,
    val type: String,
    val genre: Int = 0,
    val category: String = "",
    val mood: String = "",
)

@Serializable
data class SectionRoute(
    val section: String,
)

@Serializable
data class AskRoute(
    val kind: String,
    val tmdb: String,
    val fourK: Boolean = false,
)

@Serializable
data class PhoneRoute(
    val section: String = "",
    val topic: String = "",
)

enum class PhoneTopic {
    PERSON,
    COLLECTION,
    REPORTS,
    OTHER,
}
