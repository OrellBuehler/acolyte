package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SuggestRow(
    val kind: String = "",
    val name: String = "",
    val sub: String = "",
    val art: String? = null,
    val state: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val tmdb: String = "",
    @SerialName("person_id") val personId: String = "",
    val mbid: String = "",
    @SerialName("artist_mbid") val artistMbid: String = "",
)

@Serializable
internal data class SuggestAnswer(
    val q: String = "",
    val completions: List<String> = emptyList(),
    val items: List<SuggestRow> = emptyList(),
)

@Serializable
data class SearchSlotDownload(
    val active: Boolean = false,
)

@Serializable
data class SearchSlot(
    val state: String = "",
    val download: SearchSlotDownload? = null,
)

@Serializable
data class SearchSeason(
    val number: Int = 0,
)

@Serializable
data class SearchShowInfo(
    val missing: Int = 0,
    val seasons: List<SearchSeason> = emptyList(),
)

@Serializable
data class SearchArtistInfo(
    val albums: Int = 0,
    val missing: Int = 0,
)

@Serializable
data class SearchAlbum(
    val id: Long = 0,
    @SerialName("title_id") val titleId: Long = 0,
    val name: String = "",
    val artist: String = "",
    val year: Int = 0,
    val image: String? = null,
)

@Serializable
data class SearchPersonTitle(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
)

@Serializable
data class SearchPerson(
    val name: String = "",
    val titles: List<SearchPersonTitle> = emptyList(),
)

@Serializable
internal data class PlainQuery(
    val text: String = "",
    val plain: Boolean = true,
    val name: String = "",
    val kind: String = "",
    @SerialName("year_from") val yearFrom: Int = 0,
    @SerialName("year_to") val yearTo: Int = 0,
    val genres: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val words: List<String> = emptyList(),
)

@Serializable
internal data class PlainAnswer(
    val query: PlainQuery = PlainQuery(),
    val titles: List<SearchTitle> = emptyList(),
    val more: List<DiscoverItem> = emptyList(),
    val note: String = "",
)

@Serializable
internal data class SearchIds(
    val tmdb: String = "",
)

@Serializable
internal data class PeopleCredit(
    val name: String = "",
    val year: Int = 0,
    val ids: SearchIds = SearchIds(),
    val poster: String? = null,
    val backdrop: String? = null,
)

@Serializable
internal data class PeopleHit(
    val id: String = "",
    val name: String = "",
    val department: String = "",
    val profile: String? = null,
    @SerialName("known_for") val knownFor: List<PeopleCredit> = emptyList(),
)

@Serializable
internal data class PeoplePage(
    val people: List<PeopleHit> = emptyList(),
    val page: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
    val attribution: String = "",
    @SerialName("source_off") val sourceOff: Boolean = false,
)

@Serializable
internal data class PeopleDepartments(
    @SerialName("Acting") val acting: Int = 0,
    @SerialName("Directing") val directing: Int = 0,
    @SerialName("Sound") val sound: Int = 0,
)

@Serializable
internal data class PeopleShelfMeta(
    val id: String = "",
    val title: String = "",
    val count: Int = 0,
    val departments: PeopleDepartments = PeopleDepartments(),
)

@Serializable
internal data class PeopleShelves(
    val shelves: List<PeopleShelfMeta> = emptyList(),
    val attribution: String = "",
)

@Serializable
internal data class PeopleShelfPerson(
    val id: String = "",
    val name: String = "",
    val department: String = "",
    val profile: String? = null,
    val note: String = "",
)

@Serializable
internal data class PeopleShelf(
    val id: String = "",
    val title: String = "",
    val people: List<PeopleShelfPerson> = emptyList(),
    val attribution: String = "",
)
