package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QualityLine(
    val id: String = "",
    val name: String = "",
    val label: String = "",
    val hidden: Boolean = false,
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    val extra: Boolean = false,
    val ready: Boolean = false,
) {
    val isOwn: Boolean get() = id.startsWith("own-")
}

@Serializable
data class QualityLines(
    val movie: List<QualityLine> = emptyList(),
    val show: List<QualityLine> = emptyList(),
)

@Serializable
data class AskSettings(
    @SerialName("movies_per_week") val moviesPerWeek: Int = 0,
    @SerialName("shows_per_week") val showsPerWeek: Int = 0,
    @SerialName("music_per_week") val musicPerWeek: Int = 0,
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    @SerialName("search_on_approve") val searchOnApprove: Boolean = false,
    @SerialName("show_library_folder_id") val showLibraryFolderId: Long = 0,
    @SerialName("show_pick_rules_id") val showPickRulesId: Long = 0,
    @SerialName("music_library_folder_id") val musicLibraryFolderId: Long = 0,
    @SerialName("music_pick_rules_id") val musicPickRulesId: Long = 0,
    @SerialName("four_k_library_folder_id") val fourKLibraryFolderId: Long = 0,
    @SerialName("four_k_pick_rules_id") val fourKPickRulesId: Long = 0,
    @SerialName("show_four_k_library_folder_id") val showFourKLibraryFolderId: Long = 0,
    @SerialName("show_four_k_pick_rules_id") val showFourKPickRulesId: Long = 0,
    @SerialName("plex_watchlists") val plexWatchlists: Boolean = false,
    @SerialName("plex_watchlists_everyone") val plexWatchlistsEveryone: Boolean = false,
    @SerialName("plex_friends") val plexFriends: Boolean = false,
    @SerialName("owner_auto_approve") val ownerAutoApprove: Boolean = false,
    val qualities: QualityLines = QualityLines(),
    @SerialName("pick_many") val pickMany: Boolean = false,
    @SerialName("streaming_region") val streamingRegion: String = "",
    @SerialName("streaming_services") val streamingServices: List<Long> = emptyList(),
    @SerialName("skip_on_services") val skipOnServices: Boolean = false,
)

@Serializable
data class PlexFriend(
    @SerialName("user_id") val userId: Long = 0,
    val name: String = "",
    @SerialName("plex_name") val plexName: String = "",
    val kid: Boolean = false,
    @SerialName("checked_at") val checkedAt: String = "",
    val error: String = "",
    val asked: Int = 0,
)

@Serializable
data class PlexFriends(
    val on: Boolean = false,
    @SerialName("owner_linked") val ownerLinked: Boolean = false,
    val people: List<PlexFriend> = emptyList(),
)

@Serializable
data class OnServicesTitle(
    val id: Long = 0,
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    @SerialName("tmdb_id") val tmdbId: String = "",
    val bytes: Long = 0,
    val files: Int = 0,
    val service: String = "",
)

@Serializable
data class CleanupCheck(
    val running: Boolean = false,
    val checked: Int = 0,
    val total: Int = 0,
    val failed: Int = 0,
    val region: String = "",
    val found: List<OnServicesTitle> = emptyList(),
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("finished_at") val finishedAt: String = "",
)

@Serializable
data class AskFolder(
    val id: Long = 0,
    val kind: String = "",
    val label: String = "",
    val path: String = "",
)

@Serializable
data class AskFolders(
    @SerialName("library_folders") val libraryFolders: List<AskFolder> = emptyList(),
)

@Serializable
data class AskPickRules(
    val id: Long = 0,
    val name: String = "",
    val kinds: List<String> = emptyList(),
)

@Serializable
data class AskPickRulesList(
    @SerialName("pick_rules") val pickRules: List<AskPickRules> = emptyList(),
)

@Serializable
data class AskWatchService(
    val id: Long = 0,
    val name: String = "",
    val logo: String = "",
)

@Serializable
data class AskWatchServices(
    val region: String = "",
    val services: List<AskWatchService> = emptyList(),
)
