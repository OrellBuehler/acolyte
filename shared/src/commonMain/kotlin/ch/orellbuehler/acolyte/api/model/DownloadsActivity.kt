package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EventActor(
    val kind: String = "",
    val id: Long = 0,
)

@Serializable
data class EventRecord(
    @SerialName("ID") val id: Long = 0,
    @SerialName("At") val at: String = "",
    @SerialName("Actor") val actor: EventActor = EventActor(),
    @SerialName("Kind") val kind: String = "",
    @SerialName("TitleID") val titleRef: Long? = null,
    @SerialName("SlotID") val slotRef: Long? = null,
    @SerialName("DownloadID") val downloadRef: Long? = null,
    @SerialName("Summary") val summary: String = "",
    @SerialName("Data") val data: JsonObject? = null,
    @SerialName("title_name") val titleName: String = "",
    val episode: String = "",
    val version: String = "",
    @SerialName("actor_name") val actorName: String = "",
) {
    val titleId: Long get() = titleRef ?: 0

    val slotId: Long get() = slotRef ?: 0

    val downloadId: Long get() = downloadRef ?: 0
}

@Serializable
data class EventsPage(
    val events: List<EventRecord> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val episode: String = "",
)

@Serializable
data class PersonRef(
    val id: Long = 0,
    val name: String = "",
)

@Serializable
data class PersonList(
    val users: List<PersonRef> = emptyList(),
)

@Serializable
data class SourceRef(
    val id: Long = 0,
    val name: String = "",
)

@Serializable
data class SourceList(
    val sources: List<SourceRef> = emptyList(),
)

@Serializable
data class TurnDownBody(
    @SerialName("slot_id") val slotId: Long? = null,
)

@Serializable
data class ReleaseRef(
    val id: Long = 0,
)

@Serializable
data class SearchBody(
    @SerialName("item_id") val itemId: Long? = null,
    @SerialName("album_id") val albumId: Long? = null,
    @SerialName("slot_id") val slotId: Long? = null,
)

@Serializable
data class SearchAnswer(
    @SerialName("job_ids") val jobIds: List<Long> = emptyList(),
)

@Serializable
data class FollowAnswer(
    @SerialName("title_id") val titleId: Long = 0,
    val following: Boolean = false,
)

@Serializable
data class CalendarFollower(
    val id: Long = 0,
    val name: String = "",
    val avatar: String = "",
    val me: Boolean = false,
)

@Serializable
data class CalendarVersion(
    val label: String = "",
    val state: String = "",
)

@Serializable
data class CalendarRow(
    @SerialName("title_id") val titleId: Long = 0,
    val name: String = "",
    val year: Int = 0,
    val kind: String = "",
    val date: String = "",
    @SerialName("item_id") val itemId: Long = 0,
    val season: Int = 0,
    val episode: Int = 0,
    @SerialName("episode_name") val episodeName: String = "",
    val tmdb: String = "",
    @SerialName("air_day") val airDay: String = "",
    val timed: Boolean = false,
    val runtime: Int = 0,
    val network: String = "",
    val state: String = "",
    val versions: List<CalendarVersion> = emptyList(),
    @SerialName("part_id") val partId: Long = 0,
    val album: String = "",
    @SerialName("album_type") val albumType: String = "",
    val tracks: Int = 0,
    val have: Int = 0,
    val poster: String = "",
    val backdrop: String = "",
    val still: String = "",
    val genres: List<String> = emptyList(),
    val studio: String = "",
    @SerialName("cinema_date") val cinemaDate: String = "",
    @SerialName("home_date") val homeDate: String = "",
    val nth: Int = 0,
    val following: Boolean = false,
    val followers: List<CalendarFollower> = emptyList(),
)

@Serializable
data class CalendarAnswer(
    val from: String = "",
    val to: String = "",
    val dates: List<CalendarRow> = emptyList(),
    @SerialName("my_follows") val myFollows: Int = 0,
    val people: Int = 0,
)

@Serializable
data class SeasonEpisode(
    val number: Int = 0,
    val name: String = "",
    val overview: String = "",
    val still: String = "",
)

@Serializable
data class SeasonEpisodes(
    val episodes: List<SeasonEpisode> = emptyList(),
)

@Serializable
data class MovieOverview(
    val overview: String = "",
)
