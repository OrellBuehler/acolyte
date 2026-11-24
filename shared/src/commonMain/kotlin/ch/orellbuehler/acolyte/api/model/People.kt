package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FilmographyMovie(
    val name: String = "",
    val year: Int = 0,
    val overview: String = "",
    val ids: SearchIds = SearchIds(),
    val poster: String? = null,
    val backdrop: String? = null,
    val genres: List<String> = emptyList(),
    @SerialName("cinema_date") val cinemaDate: String = "",
    @SerialName("digital_date") val digitalDate: String = "",
    val roles: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
    val popularity: Double = 0.0,
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("never_add") val neverAdd: Boolean = false,
    val ask: TitleAsk? = null,
)

@Serializable
internal data class FilmographyPerson(
    val id: String = "",
    val name: String = "",
    val department: String = "",
    val biography: String = "",
    val birthday: String = "",
    val deathday: String = "",
    val birthplace: String = "",
    val profile: String? = null,
)

@Serializable
internal data class FilmographyStats(
    @SerialName("first_year") val firstYear: Int = 0,
    @SerialName("last_year") val lastYear: Int = 0,
    val active: Boolean = false,
    val films: Int = 0,
    val shows: Int = 0,
    val titles: Int = 0,
    val rating: Double = 0.0,
    val rated: Int = 0,
)

@Serializable
internal data class FilmographyPage(
    val person: FilmographyPerson = FilmographyPerson(),
    val movies: List<FilmographyMovie> = emptyList(),
    val page: Int = 0,
    @SerialName("page_size") val pageSize: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
    val complete: Boolean = false,
    val library: List<FilmographyMovie> = emptyList(),
    val asked: List<FilmographyMovie> = emptyList(),
    @SerialName("known_for") val knownFor: List<FilmographyMovie> = emptyList(),
    @SerialName("coming_soon") val comingSoon: List<FilmographyMovie> = emptyList(),
    val stats: FilmographyStats = FilmographyStats(),
    @SerialName("often_with") val oftenWith: List<PeopleHit> = emptyList(),
    val attribution: String = "",
)

@Serializable
internal data class SeriesFile(
    val id: Long = 0,
    @SerialName("version_id") val versionId: Long = 0,
    val path: String = "",
    val origin: String = "",
    val state: String = "",
    val protected: Boolean = false,
)

@Serializable
internal data class SeriesTitle(
    val id: Long = 0,
    val name: String = "",
    val year: Int = 0,
    val state: String = "",
    @SerialName("version_ids") val versionIds: List<Long> = emptyList(),
    val files: List<SeriesFile> = emptyList(),
    val protected: Boolean = false,
    val selected: Boolean = false,
    val skip: String = "",
)

@Serializable
internal data class SeriesInfo(
    val id: Long = 0,
    @SerialName("source_id") val sourceId: String = "",
    val name: String = "",
    @SerialName("owned_titles") val ownedTitles: Int = 0,
)

@Serializable
internal data class SeriesDetail(
    val id: Long = 0,
    @SerialName("source_id") val sourceId: String = "",
    val name: String = "",
    @SerialName("owned_titles") val ownedTitles: Int = 0,
    val titles: List<SeriesTitle> = emptyList(),
)

@Serializable
internal data class SeriesRemoveRequest(
    @SerialName("title_ids") val titleIds: List<Long>,
    @SerialName("delete_files") val deleteFiles: Boolean = false,
    @SerialName("never_add") val neverAdd: Boolean = false,
    @SerialName("acknowledge_protection") val acknowledgeProtection: Boolean = false,
    @SerialName("review_token") val reviewToken: String? = null,
)

@Serializable
internal data class SeriesChoice(
    @SerialName("delete_files") val deleteFiles: Boolean = false,
    @SerialName("never_add") val neverAdd: Boolean = false,
    @SerialName("acknowledge_protection") val acknowledgeProtection: Boolean = false,
)

@Serializable
internal data class SeriesResult(
    @SerialName("title_id") val titleId: Long = 0,
    val name: String = "",
    val status: String = "",
    val error: String = "",
)

@Serializable
internal data class SeriesReview(
    val collection: SeriesInfo = SeriesInfo(),
    val titles: List<SeriesTitle> = emptyList(),
    val choice: SeriesChoice = SeriesChoice(),
    @SerialName("review_token") val reviewToken: String = "",
    val applied: Boolean = false,
    val results: List<SeriesResult> = emptyList(),
)

@Serializable
internal data class HiddenList(
    val hidden: List<TitleHide> = emptyList(),
)
