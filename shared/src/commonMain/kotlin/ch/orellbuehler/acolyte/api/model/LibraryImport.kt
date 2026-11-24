package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdoptScanRequest(
    val after: String? = null,
)

@Serializable
data class ScanStarted(
    @SerialName("job_id") val jobId: Long = 0,
    @SerialName("dry_run") val dryRun: Boolean = false,
)

@Serializable
data class AdoptPlan(
    val kind: String = "",
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    val path: String = "",
    val folders: List<AdoptCandidate> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val limit: Int = 0,
    val more: Boolean = false,
    val next: String = "",
    val total: Int = 0,
    val offset: Int = 0,
    val warning: String = "",
    val partial: Boolean = false,
    @SerialName("loose_movies") val looseMovies: Int = 0,
    val flat: Boolean = false,
    @SerialName("loose_top") val looseTop: Int = 0,
    @SerialName("suggest_flat") val suggestFlat: Boolean = false,
)

@Serializable
data class AdoptCandidate(
    val folder: String = "",
    @SerialName("parsed_name") val parsedName: String = "",
    @SerialName("parsed_year") val parsedYear: Int = 0,
    val match: AdoptMatch? = null,
    val others: List<AdoptMatch> = emptyList(),
    val loose: Boolean = false,
    val status: String = "",
    val why: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val files: List<AdoptEpisodeFile> = emptyList(),
    val episodes: Int = 0,
    val unknown: Int = 0,
    val albums: List<AdoptAlbum> = emptyList(),
    val tracks: Int = 0,
)

@Serializable
data class AdoptMatch(
    val id: String = "",
    @SerialName("tmdb_id") val tmdbId: String = "",
    val name: String = "",
    val year: Int = 0,
    val score: Double = 0.0,
    val poster: String = "",
) {
    val key: String get() = id.ifEmpty { tmdbId }
}

@Serializable
data class AdoptEpisodeFile(
    val path: String = "",
    val size: Long = 0,
    val episodes: List<String> = emptyList(),
    val why: String = "",
)

@Serializable
data class AdoptAlbum(
    val folder: String = "",
    @SerialName("parsed_name") val parsedName: String = "",
    val match: AdoptMatch? = null,
    val matched: Int = 0,
    val unknown: Int = 0,
    val status: String = "",
)

@Serializable
internal data class AdoptRequest(
    val folders: List<AdoptChoice>,
    val follow: Boolean,
)

@Serializable
internal data class AdoptChoice(
    val folder: String,
    @SerialName("tmdb_id") val tmdbId: String? = null,
    val id: String? = null,
)

@Serializable
data class AdoptResult(
    val folder: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val file: String = "",
    val error: String = "",
    val episodes: Int = 0,
)

@Serializable
data class TidyPlan(
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    val moves: List<TidyMove> = emptyList(),
    val blocked: List<TidyMove> = emptyList(),
    val unmatched: List<String> = emptyList(),
    @SerialName("hash") val planHash: String = "",
    @SerialName("undo_files") val undoFiles: Int = 0,
)

@Serializable
data class TidyMove(
    val from: String = "",
    val to: String = "",
    val name: String = "",
    val year: Int = 0,
    val files: List<TidyFile> = emptyList(),
    val problem: String = "",
    val why: String = "",
    val path: String = "",
)

@Serializable
data class TidyFile(
    val from: String = "",
    val to: String = "",
)

@Serializable
internal data class TidyApply(
    val hash: String,
)

@Serializable
data class TidyResult(
    val moved: Int = 0,
    val failed: List<TidyFailure> = emptyList(),
)

@Serializable
data class TidyFailure(
    val from: String = "",
    val why: String = "",
)

@Serializable
data class MovieLookup(
    val movies: List<LookupMovie> = emptyList(),
)

@Serializable
data class LookupMovie(
    val name: String = "",
    val year: Int = 0,
    val ids: Map<String, String> = emptyMap(),
    val poster: String = "",
    @SerialName("title_id") val titleId: Long = 0,
)

@Serializable
data class ShowLookup(
    val shows: List<LookupShow> = emptyList(),
)

@Serializable
data class LookupShow(
    val source: String = "",
    val name: String = "",
    val year: Int = 0,
    val ids: Map<String, String> = emptyMap(),
    val poster: String = "",
    @SerialName("title_id") val titleId: Long = 0,
)

@Serializable
data class ArtistLookup(
    val artists: List<LookupArtist> = emptyList(),
    val pending: Boolean = false,
)

@Serializable
data class LookupArtist(
    val mbid: String = "",
    val name: String = "",
    val disambiguation: String = "",
    val image: String = "",
    @SerialName("title_id") val titleId: Long = 0,
)

@Serializable
internal data class ByHandScanRequest(
    val path: String,
)

@Serializable
data class ByHandScan(
    val path: String = "",
    val files: List<ByHandFile> = emptyList(),
    val more: Boolean = false,
)

@Serializable
data class ByHandFile(
    val path: String = "",
    val name: String = "",
    val size: Long = 0,
    val kind: String = "",
    val quality: String = "",
    val guess: ByHandGuess = ByHandGuess(),
    val note: String = "",
    @SerialName("in_library") val inLibrary: Boolean = false,
)

@Serializable
data class ByHandGuess(
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("title_name") val titleName: String = "",
    @SerialName("title_kind") val titleKind: String = "",
    val year: Int = 0,
    @SerialName("version_id") val versionId: Long = 0,
    val season: Int? = null,
    val episode: Int? = null,
    @SerialName("slot_id") val slotId: Long = 0,
    @SerialName("has_file") val hasFile: Boolean = false,
    val confidence: Double = 0.0,
    @SerialName("album_id") val albumId: Long = 0,
    @SerialName("album_name") val albumName: String = "",
    @SerialName("album_year") val albumYear: Int = 0,
    val disc: Int = 0,
    val track: Int = 0,
    @SerialName("track_name") val trackName: String = "",
    val reason: String = "",
)

@Serializable
internal data class ByHandImport(
    val items: List<ByHandItem>,
)

@Serializable
internal data class ByHandItem(
    val path: String,
    @SerialName("title_id") val titleId: Long? = null,
    @SerialName("slot_id") val slotId: Long? = null,
    @SerialName("version_id") val versionId: Long? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val mode: String,
    val replace: Boolean? = null,
)

@Serializable
data class ByHandAnswer(
    val placed: Int = 0,
    val results: List<ByHandResult> = emptyList(),
)

@Serializable
data class ByHandResult(
    val path: String = "",
    val outcome: String = "",
    @SerialName("placed_by") val placedBy: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val error: String = "",
)

@Serializable
internal data class FolderBrowseRequest(
    val path: String,
)

@Serializable
data class FolderBrowse(
    val path: String = "",
    val parent: String = "",
    val folders: List<BrowseFolder> = emptyList(),
    val files: Int = 0,
    val more: Boolean = false,
)

@Serializable
data class BrowseFolder(
    val name: String = "",
    val path: String = "",
)

@Serializable
data class LibrarySearch(
    val titles: List<SearchTitle> = emptyList(),
)

@Serializable
data class SearchTitle(
    val id: Long = 0,
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    val artwork: List<String> = emptyList(),
)

@Serializable
data class TitleVersions(
    val id: Long = 0,
    val kind: String = "",
    val versions: List<TitleVersion> = emptyList(),
    val slots: List<VersionSlot> = emptyList(),
)

@Serializable
data class VersionSlot(
    val id: Long = 0,
    @SerialName("version_id") val versionId: Long = 0,
    val file: SlotFile? = null,
)

@Serializable
data class TitleVersion(
    val id: Long = 0,
    val label: String = "",
    val main: Boolean = false,
)

@Serializable
data class EpisodeList(
    val episodes: List<EpisodeRow> = emptyList(),
)

@Serializable
data class EpisodeRow(
    val id: Long = 0,
    val season: Int = 0,
    val episode: Int = 0,
    val name: String = "",
    val slots: List<EpisodeSlot> = emptyList(),
)

@Serializable
data class EpisodeSlot(
    val id: Long = 0,
    @SerialName("version_id") val versionId: Long = 0,
    @SerialName("file_id") val fileId: Long = 0,
)
