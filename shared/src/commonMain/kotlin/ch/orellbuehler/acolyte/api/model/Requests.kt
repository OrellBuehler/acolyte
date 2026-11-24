package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AskWho(
    val id: Long = 0,
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
)

@Serializable
data class AskAsker(
    val id: Long = 0,
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("asked_at") val askedAt: String = "",
    @SerialName("from_watchlist") val fromWatchlist: Boolean = false,
)

@Serializable
data class AskAlbum(
    val mbid: String = "",
    val name: String = "",
    val year: Int = 0,
    val edition: String = "",
    val songs: List<String> = emptyList(),
    @SerialName("songs_done") val songsDone: Boolean = false,
)

@Serializable
data class AskEpisodes(
    val season: Int = 0,
    val episodes: List<Int> = emptyList(),
)

@Serializable
data class AskStep(
    val kind: String = "",
    val text: String = "",
    @SerialName("searched_at") val searchedAt: String = "",
    @SerialName("next_search") val nextSearch: String = "",
)

@Serializable
data class Ask(
    val id: Long = 0,
    val kind: String = "",
    @SerialName("tmdb_id") val tmdbId: String = "",
    val ref: String = "",
    val seasons: List<Int> = emptyList(),
    @SerialName("season_words") val seasonWords: String = "",
    val episodes: List<AskEpisodes> = emptyList(),
    @SerialName("future_only") val futureOnly: Boolean = false,
    @SerialName("four_k") val fourK: Boolean = false,
    val quality: String = "",
    @SerialName("quality_name") val qualityName: String = "",
    val albums: List<AskAlbum> = emptyList(),
    @SerialName("artist_id") val artistId: String = "",
    @SerialName("album_words") val albumWords: String = "",
    val name: String = "",
    val year: Int = 0,
    val poster: String = "",
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("asked_by") val askedBy: AskWho = AskWho(),
    @SerialName("asked_at") val askedAt: String = "",
    val people: List<AskAsker> = emptyList(),
    @SerialName("decided_by") val decidedBy: AskWho? = null,
    @SerialName("decided_at") val decidedAt: String = "",
    @SerialName("also_declined") val alsoDeclined: List<AskWho> = emptyList(),
    val note: String = "",
    @SerialName("auto_approved") val autoApproved: Boolean = false,
    @SerialName("available_at") val availableAt: String = "",
    val step: AskStep? = null,
    val problem: String = "",
    val adding: Boolean = false,
    val mine: Boolean = false,
    @SerialName("via_plugin") val viaPlugin: String = "",
)

@Serializable
data class AskCounts(
    val pending: Int = 0,
    val processing: Int = 0,
    val done: Int = 0,
    val failed: Int = 0,
)

@Serializable
data class AskList(
    val asks: List<Ask> = emptyList(),
    val counts: AskCounts = AskCounts(),
    val total: Int = 0,
)

@Serializable
data class AskBrief(
    val id: Long = 0,
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    val mine: Boolean = false,
    val people: Int = 0,
    val seasons: List<Int> = emptyList(),
    val albums: List<AskAlbum> = emptyList(),
)

@Serializable
data class AskAnswer(
    val ask: Ask = Ask(),
    val joined: Boolean = false,
    val already: Boolean = false,
)

@Serializable
data class AskDecision(
    val ask: Ask = Ask(),
    val notice: String = "",
)

@Serializable
internal data class NewAsk(
    val id: String,
    val kind: String = "movie",
    val seasons: List<Int>? = null,
    val episodes: List<AskEpisodes>? = null,
    @SerialName("future_only") val futureOnly: Boolean? = null,
    val albums: List<String>? = null,
    val editions: Map<String, String>? = null,
    val follow: Boolean = true,
    val quality: String? = null,
)

@Serializable
internal data class AlbumSongs(
    val album: String = "",
    val recordings: List<String> = emptyList(),
)

@Serializable
internal data class ApproveBody(
    val note: String = "",
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    val quality: String? = null,
    val seasons: List<Int>? = null,
    val episodes: List<AskEpisodes>? = null,
    val albums: List<String>? = null,
    val songs: List<AlbumSongs>? = null,
)

@Serializable
internal data class TurnDownBody(
    val note: String = "",
)

@Serializable
internal data class RemoveAsks(
    val ids: List<Long>? = null,
    val filter: String? = null,
    @SerialName("failed_downloads") val failedDownloads: List<Long>? = null,
    val only: String? = null,
)

@Serializable
data class RemovedAsks(
    val removed: Int = 0,
)

@Serializable
data class PartEpisode(
    val number: Int = 0,
    val name: String = "",
    @SerialName("air_day") val airDay: String = "",
)

@Serializable
data class PartSong(
    val disc: Int = 0,
    val number: Int = 0,
    val name: String = "",
    @SerialName("length_ms") val lengthMs: Long = 0,
    @SerialName("recording_id") val recordingId: String = "",
) {
    val key: String get() = recordingId.ifEmpty { "$disc-$number" }
}

@Serializable
data class StoredParts(
    val seasons: Map<String, List<PartEpisode>> = emptyMap(),
    val albums: Map<String, List<PartSong>> = emptyMap(),
)

@Serializable
data class KindLimit(
    @SerialName("per_week") val perWeek: Int = 0,
    val used: Int = 0,
    val left: Int = 0,
    @SerialName("retry_at") val retryAt: String = "",
    @SerialName("no_limit") val noLimit: Boolean = false,
)

@Serializable
data class QualityChoice(
    val id: String = "",
    val name: String = "",
    val extra: Boolean = false,
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
)

@Serializable
data class QualityChoices(
    val movie: List<QualityChoice> = emptyList(),
    val show: List<QualityChoice> = emptyList(),
)

@Serializable
data class FourKOffer(
    val movie: Boolean = false,
    val show: Boolean = false,
)

@Serializable
data class AskLimit(
    @SerialName("no_limit") val noLimit: Boolean = false,
    val movies: KindLimit = KindLimit(),
    val shows: KindLimit = KindLimit(),
    val music: KindLimit = KindLimit(),
    @SerialName("pick_many") val pickMany: Boolean = false,
    @SerialName("four_k") val fourK: FourKOffer = FourKOffer(),
    val qualities: QualityChoices = QualityChoices(),
)

@Serializable
data class InboxMessage(
    val id: Long = 0,
    @SerialName("created_at") val createdAt: String = "",
    val kind: String = "",
    val text: String = "",
    @SerialName("ask_id") val askId: Long = 0,
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("read_at") val readAt: String = "",
    @SerialName("report_id") val reportId: Long = 0,
)

@Serializable
data class InboxMessages(
    val messages: List<InboxMessage> = emptyList(),
    val unread: Int = 0,
    val waiting: Int = 0,
    @SerialName("reports_open") val reportsOpen: Int = 0,
)

@Serializable
data class AskedByPerson(
    val id: Long = 0,
    val name: String = "",
    @SerialName("asked_at") val askedAt: String = "",
    val me: Boolean = false,
)

@Serializable
data class AskedByTitle(
    val askers: List<AskedByPerson> = emptyList(),
)

@Serializable
data class AskedBy(
    val titles: Map<String, AskedByTitle> = emptyMap(),
)

@Serializable
data class AskMovie(
    val ids: Map<String, String> = emptyMap(),
    val name: String = "",
    val year: Int = 0,
    val poster: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("never_add") val neverAdd: Boolean = false,
    val ask: AskBrief? = null,
    @SerialName("ask_4k") val ask4k: AskBrief? = null,
    @SerialName("has_4k") val has4k: Boolean = false,
)

@Serializable
data class AskMovieLookup(
    val movies: List<AskMovie> = emptyList(),
)

@Serializable
data class AskShowSeason(
    val number: Int = 0,
    val name: String = "",
)

@Serializable
data class AskShow(
    val ids: Map<String, String> = emptyMap(),
    val name: String = "",
    val year: Int = 0,
    val poster: String = "",
    val source: String = "",
    val seasons: List<AskShowSeason> = emptyList(),
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("never_add") val neverAdd: Boolean = false,
    val ask: AskBrief? = null,
)

@Serializable
data class AskShowLookup(
    val shows: List<AskShow> = emptyList(),
    val source: String = "",
)

@Serializable
data class AskableAlbum(
    val mbid: String = "",
    val name: String = "",
    val type: String = "",
    @SerialName("secondary_types") val secondaryTypes: List<String> = emptyList(),
    @SerialName("release_date") val releaseDate: String = "",
    val kept: Boolean = false,
    @SerialName("left_out") val leftOut: Boolean = false,
)

@Serializable
data class AskArtist(
    val mbid: String = "",
    val name: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("never_add") val neverAdd: Boolean = false,
    val ask: AskBrief? = null,
    @SerialName("ask_albums") val askAlbums: List<AskableAlbum> = emptyList(),
)

@Serializable
data class AskArtistLookup(
    val artists: List<AskArtist> = emptyList(),
)

@Serializable
data class AskSeasonEpisode(
    val number: Int = 0,
    val name: String = "",
)

@Serializable
data class AskSeasonEpisodes(
    val episodes: List<AskSeasonEpisode> = emptyList(),
)

@Serializable
data class AskTitleSeason(
    val number: Int = 0,
    val aired: Int = 0,
    @SerialName("in_library") val inLibrary: Int = 0,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
)

@Serializable
data class AskTitleSeasons(
    val seasons: List<AskTitleSeason> = emptyList(),
)
