package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class OwnedTitle(
    val id: Long = 0,
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    val overview: String = "",
    @SerialName("original_language") val originalLanguage: String = "",
    @SerialName("other_names") val otherNames: List<String> = emptyList(),
    val ids: Map<String, String> = emptyMap(),
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
    @SerialName("release_gate") val releaseGate: String = "",
    @SerialName("gate_delay_days") val gateDelayDays: Int = 0,
    @SerialName("gate_opens_at") val gateOpensAt: String = "",
    @SerialName("refreshed_at") val refreshedAt: String = "",
    val stage: String = "",
    @SerialName("gate_open") val gateOpen: Boolean = false,
    @SerialName("gate_reason") val gateReason: String = "",
    @SerialName("no_downloader") val noDownloader: String = "",
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    @SerialName("folder_path") val folderPath: String = "",
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    @SerialName("pick_rules_name") val pickRulesName: String = "",
    val artwork: List<String> = emptyList(),
    val backdrop: String = "",
    val logo: String = "",
    val genres: List<String> = emptyList(),
    @SerialName("runtime_minutes") val runtimeMinutes: Int = 0,
    val rating: String = "",
    @SerialName("added_at") val addedAt: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    val versions: List<OwnedVersion> = emptyList(),
    val slots: List<OwnedSlot> = emptyList(),
    val asks: List<OwnedAsk> = emptyList(),
    val show: OwnedShow? = null,
) {
    val tmdbId: String get() = ids["tmdb"].orEmpty()
}

@Serializable
data class OwnedVersion(
    val id: Long = 0,
    val label: String = "",
    val main: Boolean = false,
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    @SerialName("pick_rules_name") val pickRulesName: String = "",
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    @SerialName("folder_path") val folderPath: String = "",
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
    val edition: String = "",
    @SerialName("audio_languages") val audioLanguages: List<String> = emptyList(),
)

@Serializable
data class OwnedSlot(
    val id: Long = 0,
    val version: String = "",
    @SerialName("version_id") val versionId: Long = 0,
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    val reason: String = "",
    val file: OwnedFile? = null,
    @SerialName("active_download_id") val activeDownloadId: Long = 0,
    val download: OwnedDownload? = null,
)

@Serializable
data class OwnedDownload(
    val id: Long = 0,
    val state: String = "",
    val label: String = "",
    val text: String = "",
    val progress: Double = 0.0,
    val speed: Long = 0,
    val eta: Long = 0,
    val active: Boolean = false,
)

@Serializable
data class OwnedQuality(
    val resolution: Int = 0,
    val source: String = "",
    val codec: String = "",
    val hdr: List<String> = emptyList(),
    val audio: String = "",
)

@Serializable
data class OwnedFile(
    @SerialName("ID") val id: Long = 0,
    @SerialName("Protected") val isProtected: Boolean = false,
    @SerialName("VersionID") val versionId: Long = 0,
    @SerialName("RelativePath") val relativePath: String = "",
    @SerialName("Size") val size: Long = 0,
    @SerialName("AddedAt") val addedAt: String = "",
    @SerialName("Quality") val quality: OwnedQuality = OwnedQuality(),
    @SerialName("Languages") val languages: List<String> = emptyList(),
    @SerialName("MediaInfo") val mediaInfo: JsonObject = JsonObject(emptyMap()),
    @SerialName("ReleaseName") val releaseName: String = "",
    @SerialName("Group") val group: String = "",
    @SerialName("Origin") val origin: String = "",
    @SerialName("State") val state: String = "",
) {
    val fileName: String get() = relativePath.split('/', '\\').lastOrNull { it.isNotEmpty() }.orEmpty()

    val edition: String get() = info("edition")

    val videoCodec: String get() = info("video_codec")

    val audioCodec: String get() = info("audio_codec")

    val resolutionLabel: String
        get() = info("resolution").ifEmpty { if (quality.resolution > 0) "${quality.resolution}p" else "" }

    val audioLanguages: List<String> get() = languageList("audio_languages")

    val subtitleLanguages: List<String> get() = languageList("subtitle_languages").ifEmpty { languageList("subtitles") }

    internal fun info(key: String): String = (mediaInfo[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun languageList(key: String): List<String> {
        val raw =
            when (val value = mediaInfo[key]) {
                is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                is JsonPrimitive -> value.contentOrNull?.split('/', ',').orEmpty()
                else -> emptyList()
            }
        return raw.map { it.trim().uppercase() }.filter { it.isNotEmpty() }
    }
}

@Serializable
data class OwnedAsk(
    val id: Long = 0,
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    @SerialName("asked_by") val askedBy: OwnedWho = OwnedWho(),
    @SerialName("asked_at") val askedAt: String = "",
    val people: List<OwnedWho> = emptyList(),
    @SerialName("decided_by") val decidedBy: OwnedWho? = null,
    @SerialName("decided_at") val decidedAt: String = "",
    val note: String = "",
    @SerialName("four_k") val fourK: Boolean = false,
    @SerialName("season_words") val seasonWords: String = "",
)

@Serializable
data class OwnedWho(
    val id: Long = 0,
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
) {
    val shown: String get() = displayName.ifBlank { name }
}

@Serializable
data class OwnedShow(
    @SerialName("details_source") val detailsSource: String = "",
    @SerialName("episode_order") val episodeOrder: String = "aired",
    @SerialName("new_seasons") val newSeasons: String = "all",
    @SerialName("pack_window") val packWindow: Int = 0,
    val status: String = "",
    val network: String = "",
    val seasons: List<OwnedSeason> = emptyList(),
    val episodes: Int = 0,
    val missing: Int = 0,
    @SerialName("not_out_yet") val notOutYet: Int = 0,
    @SerialName("in_library") val inLibrary: Int = 0,
    val downloading: Int = 0,
    val failed: Int = 0,
    @SerialName("next_air_date") val nextAirDate: String = "",
    @SerialName("episode_group") val episodeGroup: OwnedEpisodeGroup? = null,
)

@Serializable
data class OwnedEpisodeGroup(
    val id: String = "",
    val name: String = "",
    val type: Int = 0,
)

@Serializable
data class OwnedSeason(
    val number: Int = 0,
    val name: String = "",
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = false,
    val episodes: Int = 0,
    val aired: Int = 0,
    @SerialName("in_library") val inLibrary: Int = 0,
    val missing: Int = 0,
    @SerialName("not_out_yet") val notOutYet: Int = 0,
    val downloading: Int = 0,
    val failed: Int = 0,
    @SerialName("next_air_date") val nextAirDate: String = "",
    @SerialName("next_air_day") val nextAirDay: String = "",
)

@Serializable
data class OwnedEpisodes(
    @SerialName("episode_order") val episodeOrder: String = "",
    val episodes: List<OwnedEpisode> = emptyList(),
)

@Serializable
data class OwnedEpisode(
    val id: Long = 0,
    val season: Int = 0,
    val episode: Int = 0,
    val absolute: Int = 0,
    val name: String = "",
    @SerialName("air_date") val airDate: String = "",
    @SerialName("air_day") val airDay: String = "",
    val runtime: Int = 0,
    @SerialName("runtime_minutes") val runtimeMinutes: Int = 0,
    val still: String = "",
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = true,
    val slots: List<OwnedEpisodeSlot> = emptyList(),
    @SerialName("outside_order") val outsideOrder: Boolean = false,
    @SerialName("tmdb_season") val tmdbSeason: Int = 0,
    @SerialName("tmdb_episode") val tmdbEpisode: Int = 0,
) {
    val mainSlot: OwnedEpisodeSlot? get() = slots.firstOrNull()

    val minutes: Int get() = if (runtimeMinutes > 0) runtimeMinutes else runtime
}

@Serializable
data class OwnedEpisodeSlot(
    val id: Long = 0,
    val version: String = "",
    @SerialName("version_id") val versionId: Long = 0,
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    val reason: String = "",
    @SerialName("file_id") val fileId: Long = 0,
    @SerialName("good_enough") val goodEnough: Boolean = true,
    val points: Int? = null,
    @SerialName("good_enough_points") val goodEnoughPoints: Int? = null,
    val file: OwnedFile? = null,
)

@Serializable
data class OwnedEpisodeGroups(
    val chosen: String? = null,
    val groups: List<OwnedEpisodeGroupChoice> = emptyList(),
)

@Serializable
data class OwnedEpisodeGroupChoice(
    val id: String = "",
    val name: String = "",
    val type: Int = 0,
    val seasons: Int = 0,
    val episodes: Int = 0,
)

@Serializable
internal data class KeepBody(
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean,
)

@Serializable
internal data class SearchBody(
    @SerialName("slot_id") val slotId: Long? = null,
    @SerialName("item_id") val itemId: Long? = null,
    val season: Int? = null,
    @SerialName("version_id") val versionId: Long? = null,
    val choose: Boolean? = null,
)

@Serializable
data class PackNext(
    @SerialName("download_id") val downloadId: Long = 0,
    val added: Int = 0,
)

@Serializable
data class FilePoints(
    val step: String = "",
    @SerialName("good_enough_at") val goodEnoughAt: String = "",
    @SerialName("step_reached") val stepReached: Boolean = false,
    val points: Int = 0,
    @SerialName("good_enough_points") val goodEnoughPoints: Int? = null,
    @SerialName("good_enough") val goodEnough: Boolean = false,
    @SerialName("meets_version") val meetsVersion: Boolean = true,
    val traits: List<FileTrait> = emptyList(),
    val names: Map<String, String> = emptyMap(),
)

@Serializable
data class FileTrait(
    val trait: String = "",
    val points: Int = 0,
)

@Serializable
internal data class FileChoice(
    @SerialName("protected") val isProtected: Boolean? = null,
    val edition: String? = null,
    @SerialName("acknowledge_protection") val acknowledgeProtection: Boolean? = null,
)

@Serializable
internal data class VersionInput(
    val label: String,
    @SerialName("pick_rules_id") val pickRulesId: Long? = null,
    @SerialName("library_folder_id") val libraryFolderId: Long? = null,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean? = null,
    val edition: String? = null,
    @SerialName("audio_languages") val audioLanguages: List<String>? = null,
    @SerialName("folder_name") val folderName: String? = null,
)

@Serializable
internal data class AdoptBody(
    @SerialName("relative_path") val relativePath: String,
)

@Serializable
data class TitlePeople(
    val people: List<TitleCredit> = emptyList(),
)

@Serializable
data class TitleCredit(
    val person: TitleCreditPerson = TitleCreditPerson(),
    val roles: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
)

@Serializable
data class TitleCreditPerson(
    val id: String = "",
    val name: String = "",
    val profile: String = "",
)

@Serializable
data class TitleEvents(
    val events: List<TitleEvent> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class TitleEvent(
    @SerialName("ID") val id: Long = 0,
    @SerialName("Kind") val kind: String = "",
    @SerialName("Summary") val summary: String = "",
    @SerialName("At") val at: String = "",
    @SerialName("DownloadID") val downloadId: Long? = null,
    @SerialName("SlotID") val slotId: Long? = null,
    @SerialName("GroupKey") val groupKey: String = "",
    @SerialName("TitleID") val titleId: Long? = null,
    val episode: String = "",
    val version: String = "",
    @SerialName("actor_name") val actorName: String = "",
)
