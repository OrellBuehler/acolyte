package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HeldFiles(
    val files: List<HeldFile> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class HeldFile(
    val id: Long = 0,
    val path: String = "",
    @SerialName("held_path") val heldPath: String = "",
    val folder: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val title: String = "",
    val kind: String = "",
    @SerialName("title_deleted") val titleDeleted: Boolean = false,
    val size: Long = 0,
    @SerialName("deleted_at") val deletedAt: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    val replaced: Boolean = false,
    @SerialName("replaced_by") val replacedBy: String = "",
)

@Serializable
internal data class RestoreRequest(
    val ids: List<Long>,
    val how: String = "",
)

@Serializable
data class RestoreAnswer(
    val results: List<RestoreResult> = emptyList(),
    @SerialName("titles_back") val titlesBack: List<Long> = emptyList(),
)

@Serializable
data class RestoreResult(
    val id: Long = 0,
    val path: String = "",
    val result: String = "",
    val taken: Boolean = false,
    @SerialName("can_swap") val canSwap: Boolean = false,
)

@Serializable
data class FileSettings(
    @SerialName("holding_days") val holdingDays: Int = 0,
    @SerialName("min_free_mb") val minFreeMb: Int = 0,
    @SerialName("skip_free_space_check") val skipFreeSpaceCheck: Boolean = false,
    @SerialName("unkeep_deleted_movies") val unkeepDeletedMovies: Boolean = false,
    @SerialName("unkeep_deleted_episodes") val unkeepDeletedEpisodes: Boolean = false,
)

@Serializable
data class ReleasePage(
    val releases: List<ReleaseView> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

@Serializable
data class ReleaseView(
    val id: Long = 0,
    val name: String = "",
    val size: Long = 0,
    val seeders: Int? = null,
    val protocol: String = "",
    val state: String = "",
    @SerialName("source_name") val sourceName: String = "",
    val version: String = "",
    @SerialName("title_id") val titleId: Long? = null,
    @SerialName("title_name") val titleName: String = "",
    val episode: String = "",
    val album: String = "",
    @SerialName("turned_down_for") val turnedDownFor: String = "",
    @SerialName("hold_until") val holdUntil: String = "",
    val outcome: String = "",
    val passed: Boolean = false,
    val rank: Int = 0,
    val points: Int = 0,
    val reasons: List<ReleaseReason> = emptyList(),
    val read: ReleaseRead? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class ReleaseReason(
    val code: String = "",
    val effect: String = "",
    val text: String = "",
)

@Serializable
data class ReleaseRead(
    val resolution: Int = 0,
    val source: String = "",
    val codec: String = "",
    val hdr: List<String> = emptyList(),
    val channels: String = "",
    val atmos: Boolean = false,
    val languages: List<String> = emptyList(),
    val proper: Boolean = false,
    val format: String = "",
    val bitrate: Int = 0,
    @SerialName("bit_depth") val bitDepth: Int = 0,
)

@Serializable
data class Removed(
    val removed: Int = 0,
)

@Serializable
data class LibraryCopyPage(
    val copies: List<LibraryCopy> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val codecs: Map<String, Int> = emptyMap(),
)

@Serializable
data class LibraryCopy(
    @SerialName("file_id") val fileId: Long = 0,
    @SerialName("version_id") val versionId: Long = 0,
    @SerialName("title_id") val titleId: Long = 0,
    val title: String = "",
    val year: Int = 0,
    val version: String = "",
    val edition: String = "",
    @SerialName("relative_path") val relativePath: String = "",
    @SerialName("protected") val isProtected: Boolean = false,
    val codec: String = "",
    @SerialName("inspection_status") val inspectionStatus: String = "",
    val container: String = "",
    @SerialName("audio_languages") val audioLanguages: List<String> = emptyList(),
    @SerialName("audio_tracks") val audioTracks: Int = 0,
    @SerialName("audio_unknown") val audioUnknown: Boolean = false,
    @SerialName("added_at") val addedAt: String = "",
)

@Serializable
data class CopySelection(
    @SerialName("file_id") val fileId: Long = 0,
    @SerialName("version_id") val versionId: Long = 0,
)

@Serializable
internal data class CopySearchRequest(
    val selections: List<CopySelection>,
    @SerialName("review_token") val reviewToken: String? = null,
)

@Serializable
data class CopySearchPreview(
    val copies: List<LibraryCopy> = emptyList(),
    @SerialName("slot_count") val slotCount: Int = 0,
    @SerialName("review_token") val reviewToken: String = "",
)

@Serializable
data class MissingSubtitles(
    val missing: List<MissingSubtitle> = emptyList(),
    val total: Int = 0,
    val titles: List<MissingTitle> = emptyList(),
)

@Serializable
data class MissingSubtitle(
    @SerialName("file_id") val fileId: Long = 0,
    @SerialName("item_id") val itemId: Long = 0,
    @SerialName("title_id") val titleId: Long = 0,
    val title: String = "",
    val kind: String = "",
    val season: Int = 0,
    val episode: Int = 0,
    val language: String = "",
    val forced: Boolean = false,
    @SerialName("hearing_impaired") val hearingImpaired: Boolean = false,
    val state: String = "",
    val tries: Int = 0,
    @SerialName("next_try_at") val nextTryAt: String = "",
)

@Serializable
data class MissingTitle(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    val count: Int = 0,
)

@Serializable
internal data class MissingSubtitleSearch(
    val kind: String? = null,
    val language: String? = null,
    @SerialName("title_id") val titleId: Long? = null,
)

@Serializable
internal data class FileSubtitleSearch(
    @SerialName("file_id") val fileId: Long? = null,
    @SerialName("title_id") val titleId: Long? = null,
    @SerialName("item_id") val itemId: Long? = null,
)

@Serializable
data class SubtitleSearchStarted(
    val queued: Int = 0,
    @SerialName("job_ids") val jobIds: List<Long> = emptyList(),
)

@Serializable
data class SubtitleLanguages(
    val languages: List<SubtitleLanguage> = emptyList(),
)

@Serializable
data class SubtitleLanguage(
    val code: String = "",
    val name: String = "",
)

@Serializable
data class RenamePlan(
    val steps: List<RenameStep> = emptyList(),
    @SerialName("plan_hash") val planHash: String = "",
)

@Serializable
data class RenameStep(
    @SerialName("file_id") val fileId: Long = 0,
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("title_name") val titleName: String = "",
    val from: String = "",
    val to: String = "",
    val skip: String = "",
    val subtitles: List<RenameSubtitle> = emptyList(),
)

@Serializable
data class RenameSubtitle(
    val from: String = "",
    val to: String = "",
    val skip: String = "",
)

@Serializable
internal data class RenameApply(
    @SerialName("plan_hash") val planHash: String,
)

@Serializable
data class JobView(
    @SerialName("ID") val id: Long = 0,
    @SerialName("State") val state: String = "",
    @SerialName("Progress") val progress: Double = 0.0,
    @SerialName("ProgressNote") val progressNote: String? = null,
    @SerialName("Result") val result: JsonObject? = null,
    @SerialName("Error") val error: JobError? = null,
    @SerialName("Kind") val kind: String = "",
    @SerialName("StartedAt") val startedAt: String? = null,
    @SerialName("EndedAt") val endedAt: String? = null,
) {
    val finished: Boolean get() = state == "done" || state == "failed" || state == "cancelled"
}

@Serializable
data class JobList(
    val jobs: List<JobView> = emptyList(),
)

@Serializable
data class JobError(
    val code: String = "",
    val message: String = "",
)

@Serializable
data class LibraryStats(
    val kinds: List<KindStats> = emptyList(),
)

@Serializable
data class KindStats(
    val kind: String = "",
    val titles: Int = 0,
    val files: Int = 0,
    val bytes: Long = 0,
    val facts: Map<String, List<StatsBucket>> = emptyMap(),
)

@Serializable
data class StatsBucket(
    val value: String = "",
    val titles: Int = 0,
    val files: Int = 0,
    val bytes: Long = 0,
)

@Serializable
data class WatchedSummary(
    val enabled: Boolean = false,
    @SerialName("read_at") val readAt: String = "",
    val people: List<WatchedPerson> = emptyList(),
)

@Serializable
data class WatchedPerson(
    val server: String = "",
    @SerialName("server_kind") val serverKind: String = "",
    val name: String = "",
    val linked: Boolean = false,
    @SerialName("user_name") val userName: String = "",
    val movies: Int = 0,
    val shows: Int = 0,
    val episodes: Int = 0,
    val plays: Int = 0,
    @SerialName("last_watched_at") val lastWatchedAt: String = "",
)

@Serializable
data class WatchedCleanup(
    val enabled: Boolean = false,
    val days: Int = 0,
    val people: List<CleanupPerson> = emptyList(),
    val found: List<WatchedTitle> = emptyList(),
)

@Serializable
data class CleanupPerson(
    val key: String = "",
    val name: String = "",
    val server: String = "",
    val chosen: Boolean = false,
)

@Serializable
data class WatchedTitle(
    val id: Long = 0,
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    val bytes: Long = 0,
    @SerialName("last_watched_at") val lastWatchedAt: String = "",
)
