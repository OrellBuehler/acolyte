package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TitleGrid(
    val titles: List<GridTitle> = emptyList(),
)

@Serializable
data class GridTitle(
    val id: Long = 0,
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    val overview: String = "",
    @SerialName("other_names") val otherNames: List<String> = emptyList(),
    val ids: Map<String, String> = emptyMap(),
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
    @SerialName("cinema_date") val cinemaDate: String = "",
    @SerialName("digital_date") val digitalDate: String = "",
    @SerialName("physical_date") val physicalDate: String = "",
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    @SerialName("folder_path") val folderPath: String = "",
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    @SerialName("pick_rules_name") val pickRulesName: String = "",
    val artwork: List<String> = emptyList(),
    val backdrop: String = "",
    val genres: List<String> = emptyList(),
    @SerialName("runtime_minutes") val runtimeMinutes: Int = 0,
    val rating: String = "",
    val score: Double = 0.0,
    val votes: Int = 0,
    @SerialName("added_at") val addedAt: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    @SerialName("copy") val mainCopy: TitleCopy? = null,
    @SerialName("last_activity") val lastActivity: String = "",
    val versions: List<GridVersion> = emptyList(),
    val slots: List<GridSlot> = emptyList(),
    val asks: List<GridAsk> = emptyList(),
    val show: GridShow? = null,
    val artist: GridArtist? = null,
)

@Serializable
data class TitleCopy(
    @SerialName("file_id") val fileId: Long = 0,
    @SerialName("version_id") val versionId: Long = 0,
    val codec: String = "",
    val container: String = "",
    @SerialName("audio_languages") val audioLanguages: List<String> = emptyList(),
    @SerialName("protected") val isProtected: Boolean = false,
)

@Serializable
data class GridVersion(
    val id: Long = 0,
    val label: String = "",
    val main: Boolean = false,
)

@Serializable
data class GridSlot(
    val id: Long = 0,
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    val reason: String = "",
    val download: SlotDownload? = null,
    val file: SlotFile? = null,
)

@Serializable
data class SlotDownload(
    val id: Long = 0,
    val active: Boolean = false,
    val progress: Double = 0.0,
    val state: String = "",
    val label: String = "",
)

@Serializable
data class SlotFile(
    @SerialName("ID") val id: Long = 0,
    @SerialName("State") val state: String = "",
    @SerialName("Quality") val quality: FileQuality? = null,
)

@Serializable
data class FileQuality(
    val resolution: Int = 0,
    val source: String = "",
    val codec: String = "",
    val hdr: List<String> = emptyList(),
    val audio: String = "",
)

@Serializable
data class GridAsk(
    @SerialName("title_id") val titleId: Long? = null,
    val state: String = "",
    @SerialName("asked_by") val askedBy: AskPerson? = null,
    val people: List<AskPerson> = emptyList(),
)

@Serializable
data class AskPerson(
    val id: Long = 0,
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
)

@Serializable
data class GridShow(
    val issues: List<ShowIssue> = emptyList(),
    val status: String = "",
    val network: String = "",
    val seasons: List<GridSeason> = emptyList(),
    val episodes: Int = 0,
    val missing: Int = 0,
    @SerialName("not_out_yet") val notOutYet: Int = 0,
    @SerialName("in_library") val inLibrary: Int = 0,
    @SerialName("better_wanted") val betterWanted: Int = 0,
    val downloading: Int = 0,
    val failed: Int = 0,
    @SerialName("next_air_date") val nextAirDate: String = "",
    @SerialName("season_count") val seasonCount: Int = 0,
    @SerialName("last_air_date") val lastAirDate: String = "",
)

@Serializable
data class ShowIssue(
    val state: String = "",
    val reason: String = "",
)

@Serializable
data class GridSeason(
    val number: Int = 0,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
    val aired: Int = 0,
    val dropped: Boolean = false,
)

@Serializable
data class GridArtist(
    val mbid: String = "",
    val status: String = "",
    val albums: Int = 0,
    val included: Int = 0,
    val tracks: Int = 0,
    @SerialName("in_library") val inLibrary: Int = 0,
    val missing: Int = 0,
    @SerialName("not_out_yet") val notOutYet: Int = 0,
    @SerialName("better_wanted") val betterWanted: Int = 0,
    val downloading: Int = 0,
    val failed: Int = 0,
    @SerialName("next_release") val nextRelease: String = "",
    @SerialName("last_release") val lastRelease: String = "",
    val bio: String = "",
)

@Serializable
data class MyWatched(
    val enabled: Boolean = false,
    val linked: Boolean = false,
    val movies: List<Long> = emptyList(),
    val shows: Map<String, Map<String, Int>> = emptyMap(),
)

@Serializable
data class AnyoneWatched(
    val enabled: Boolean = false,
    val titles: List<Long> = emptyList(),
)

@Serializable
data class MovieCollections(
    val collections: List<MovieCollection> = emptyList(),
)

@Serializable
data class MovieCollection(
    val id: Long = 0,
    val name: String = "",
    @SerialName("title_ids") val titleIds: List<Long> = emptyList(),
)

@Serializable
data class PickRuleList(
    @SerialName("pick_rules") val pickRules: List<PickRule> = emptyList(),
)

@Serializable
data class PickRule(
    val id: Long = 0,
    val name: String = "",
    val kinds: List<String> = emptyList(),
    val preset: Boolean = false,
)

@Serializable
internal data class TitleChange(
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean? = null,
    @SerialName("pick_rules_id") val pickRulesId: Long? = null,
    @SerialName("release_gate") val releaseGate: String? = null,
    @SerialName("gate_delay_days") val gateDelayDays: Int? = null,
    @SerialName("episode_order") val episodeOrder: String? = null,
    @SerialName("episode_group_id") val episodeGroupId: String? = null,
    @SerialName("new_seasons") val newSeasons: String? = null,
    @SerialName("pack_window") val packWindow: Int? = null,
)

@Serializable
internal data class VersionAudioChange(
    @SerialName("audio_languages") val audioLanguages: List<String>? = null,
    val label: String? = null,
    @SerialName("pick_rules_id") val pickRulesId: Long? = null,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean? = null,
)

@Serializable
internal data class TitleMove(
    @SerialName("library_folder_id") val libraryFolderId: Long,
    val apply: Boolean = true,
)

@Serializable
internal data class TitleRemove(
    @SerialName("delete_files") val deleteFiles: Boolean,
    @SerialName("never_add") val neverAdd: Boolean?,
    val apply: Boolean,
    val season: Int? = null,
    @SerialName("episode_ids") val episodeIds: List<Long>? = null,
)

@Serializable
data class RemovePlan(
    @SerialName("title_id") val titleId: Long = 0,
    val name: String = "",
    val scope: String = "",
    val season: Int = 0,
    val episodes: List<String> = emptyList(),
    val applied: Boolean = false,
    val held: List<RemoveFile> = emptyList(),
    val kept: List<RemoveFile> = emptyList(),
    @SerialName("holding_days") val holdingDays: Int = 0,
    @SerialName("active_downloads") val activeDownloads: Int = 0,
)

@Serializable
data class RemoveFile(
    @SerialName("file_id") val fileId: Long = 0,
    val path: String = "",
    val why: String = "",
)

@Serializable
internal data class TitleRefresh(
    @SerialName("title_ids") val titleIds: List<Long>,
)

@Serializable
internal data class MissingSearch(
    val kind: String? = null,
)

@Serializable
data class JobIds(
    @SerialName("job_ids") val jobIds: List<Long> = emptyList(),
)

@Serializable
data class StartedJob(
    @SerialName("job_id") val jobId: Long = 0,
)

@Serializable
data class TitleChanged(
    val title: OwnedTitle = OwnedTitle(),
    @SerialName("rename_suggested") val renameSuggested: Boolean = false,
)

@Serializable
data class MovedTitle(
    @SerialName("job_id") val jobId: Long = 0,
)
