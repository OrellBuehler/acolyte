package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TitleDetails(
    @SerialName("tmdb_id") val tmdbId: String = "",
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    val poster: String = "",
    val backdrop: String = "",
    val overview: String = "",
    val genres: List<String> = emptyList(),
    val logo: String = "",
    val logos: Map<String, String> = emptyMap(),
    @SerialName("runtime_minutes") val runtimeMinutes: Int = 0,
    val rating: String = "",
    val score: Double = 0.0,
    val votes: Int = 0,
    val people: List<TitlePerson> = emptyList(),
    val keywords: List<String> = emptyList(),
    @SerialName("content_notes") val contentNotes: List<String> = emptyList(),
    val status: String = "",
    val tagline: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val budget: Long = 0,
    val revenue: Long = 0,
    val studios: List<String> = emptyList(),
    @SerialName("episode_runtime") val episodeRuntime: Int = 0,
    @SerialName("spoken_languages") val spokenLanguages: List<String> = emptyList(),
    val dates: TitleDates? = null,
    @SerialName("next_episode") val nextEpisode: TitleNextEpisode? = null,
    val seasons: List<TitleSeasonBrief> = emptyList(),
    @SerialName("origin_country") val originCountry: List<String> = emptyList(),
    @SerialName("original_language") val originalLanguage: String = "",
    val similar: List<TitleCard> = emptyList(),
    val backdrops: List<String> = emptyList(),
    val videos: List<TitleVideo> = emptyList(),
    val collection: TitleCollection? = null,
    @SerialName("in_library") val inLibrary: Boolean = false,
    @SerialName("title_id") val titleId: Long = 0,
    val ask: TitleAsk? = null,
    @SerialName("never_add") val neverAdd: Boolean = false,
    val quality: String = "",
    val hidden: Boolean = false,
    val hides: List<TitleHide> = emptyList(),
)

@Serializable
data class TitlePerson(
    val name: String = "",
    val role: String = "",
    val character: String = "",
    val profile: String = "",
    val id: String = "",
)

@Serializable
data class TitleVideo(
    val key: String = "",
    val name: String = "",
    val type: String = "",
    val official: Boolean = false,
    val language: String = "",
)

@Serializable
data class TitleDates(
    val theaters: String = "",
    val digital: String = "",
    val physical: String = "",
    @SerialName("first_air") val firstAir: String = "",
    @SerialName("last_air") val lastAir: String = "",
)

@Serializable
data class TitleNextEpisode(
    val season: Int = 0,
    val episode: Int = 0,
    val name: String = "",
    @SerialName("air_date") val airDate: String = "",
)

@Serializable
data class TitleSeasonBrief(
    val number: Int = 0,
    @SerialName("episode_count") val episodeCount: Int = 0,
    val year: Int = 0,
)

@Serializable
data class TitleCollection(
    val id: String = "",
    val name: String = "",
    val parts: List<TitleCard> = emptyList(),
)

@Serializable
data class TitleCard(
    @SerialName("tmdb_id") val tmdbId: String = "",
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    val poster: String = "",
    val backdrop: String = "",
    val overview: String = "",
    @SerialName("in_library") val inLibrary: Boolean = false,
    @SerialName("title_id") val titleId: Long = 0,
    val ask: TitleAsk? = null,
)

@Serializable
data class TitleAsk(
    val id: Long = 0,
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    val mine: Boolean = false,
    val people: Int = 0,
    val seasons: List<Int> = emptyList(),
    val albums: List<AskAlbum> = emptyList(),
)

@Serializable
data class TitleHide(
    val id: Long = 0,
    val kind: String = "",
    @SerialName("tmdb_id") val tmdbId: String = "",
    val genre: String = "",
    val name: String = "",
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class TitleExtras(
    val logo: String = "",
    val scores: List<TitleScore> = emptyList(),
)

@Serializable
data class TitleScore(
    val source: String = "",
    val value: Double = 0.0,
    val max: Double = 0.0,
    val votes: Long = 0,
)

@Serializable
data class TitleWiki(
    val badges: List<String> = emptyList(),
    val awards: List<TitleAward> = emptyList(),
)

@Serializable
data class TitleAward(
    val name: String = "",
    val family: String = "",
    val won: Boolean = false,
    val year: Int = 0,
)

@Serializable
data class TitleMoreFrom(
    val person: String = "",
    val role: String = "",
    val items: List<TitleCard> = emptyList(),
)

@Serializable
data class TitleReviews(
    val source: String = "",
    val sources: List<TitleReviewSource> = emptyList(),
    val order: String = "",
    val score: Double = 0.0,
    val votes: Int = 0,
    val distribution: List<Int> = emptyList(),
    val watching: Int = 0,
    val reviews: List<TitleReview> = emptyList(),
    val page: Int = 1,
    val pages: Int = 0,
    @SerialName("reviews_hidden") val reviewsHidden: Boolean = false,
)

@Serializable
data class TitleReviewSource(
    val id: String = "",
    val name: String = "",
    val orders: List<String> = emptyList(),
)

@Serializable
data class TitleReview(
    val author: String = "",
    val date: String = "",
    val rating: Double = 0.0,
    val text: String = "",
    val likes: Int = 0,
    val spoiler: Boolean = false,
)

@Serializable
data class TitleSeasonEpisodes(
    val season: Int = 0,
    val episodes: List<TitleSeasonEpisode> = emptyList(),
)

@Serializable
data class TitleSeasonEpisode(
    val number: Int = 0,
    val name: String = "",
    val overview: String = "",
    val runtime: Int = 0,
    val still: String = "",
    @SerialName("air_day") val airDay: String = "",
)

@Serializable
data class TitleAnime(
    val anime: Boolean = false,
    val seasons: List<TitleAnimeSeason> = emptyList(),
)

@Serializable
data class TitleAnimeSeason(
    val number: Int = 0,
    @SerialName("absolute_from") val absoluteFrom: Int = 0,
)

@Serializable
data class TitleFollow(
    @SerialName("title_id") val titleId: Long = 0,
    val following: Boolean = false,
)

@Serializable
data class TitleMetaFollow(
    @SerialName("tmdb_id") val tmdbId: String = "",
    val following: Boolean = false,
    @SerialName("title_id") val titleId: Long = 0,
)

@Serializable
data class TitleTrailerStream(
    val type: String = "",
    val url: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("mp4_url") val mp4Url: String = "",
    val audio: Boolean = false,
    val key: String = "",
    @SerialName("proxy_url") val proxyUrl: String = "",
)

@Serializable
data class TitleWatchLinks(
    val links: List<TitleWatchLink> = emptyList(),
)

@Serializable
data class TitleWatchLink(
    val server: String = "",
    val kind: String = "",
    val url: String = "",
)

@Serializable
data class TitleSearchOutcome(
    val empty: Boolean = false,
    val at: String = "",
    val sources: List<TitleOutcomeSource> = emptyList(),
    val note: String = "",
    val refused: Boolean = false,
)

@Serializable
data class TitleOutcomeSource(
    val id: Long = 0,
    val name: String = "",
    val why: String = "",
)

@Serializable
data class TitleUsers(
    val users: List<TitleUser> = emptyList(),
)

@Serializable
data class TitleUser(
    val id: Long = 0,
    val name: String = "",
)

@Serializable
internal data class HideBody(
    val kind: String,
    @SerialName("tmdb_id") val tmdbId: String? = null,
    val name: String? = null,
    val genre: String? = null,
    @SerialName("user_id") val userId: Long? = null,
)
