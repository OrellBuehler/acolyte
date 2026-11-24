package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TitleReleasePage(
    val releases: List<TitleRelease> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    @SerialName("running_job_ids") val runningJobIds: List<Long> = emptyList(),
    @SerialName("rules_changed") val rulesChanged: Boolean = false,
    @SerialName("last_search") val lastSearch: TitleLastSearch? = null,
)

@Serializable
data class TitleLastSearch(
    val at: String = "",
    val failed: Boolean = false,
    val found: Int = 0,
    val dropped: Int = 0,
    @SerialName("dropped_sources") val droppedSources: List<String> = emptyList(),
)

@Serializable
data class TitleRelease(
    val id: Long = 0,
    val name: String = "",
    val size: Long = 0,
    val seeders: Int? = null,
    @SerialName("protocol") val protocolName: String = "",
    val state: String = "",
    @SerialName("source_name") val sourceName: String = "",
    @SerialName("info_url") val infoUrl: String = "",
    @SerialName("slot_ids") val slotIds: List<Long> = emptyList(),
    @SerialName("published_at") val publishedAt: String = "",
    val outcome: String = "",
    val passed: Boolean = false,
    val rank: Int = 0,
    val points: Int = 0,
    @SerialName("needs_force") val needsForce: Boolean = false,
    @SerialName("gate_skipped") val gateSkipped: Boolean = false,
    val reasons: List<TitleReleaseReason> = emptyList(),
    val read: TitleReleaseRead? = null,
    val episode: String = "",
)

@Serializable
data class TitleReleaseReason(
    val code: String = "",
    val effect: String = "",
    val text: String = "",
)

@Serializable
data class TitleReleaseRead(
    val resolution: Int = 0,
    val source: String = "",
    val codec: String = "",
    val hdr: List<String> = emptyList(),
    val channels: String = "",
    val atmos: Boolean = false,
    val languages: List<String> = emptyList(),
    val group: String = "",
    val proper: Boolean = false,
    val format: String = "",
    val bitrate: Int = 0,
    @SerialName("bit_depth") val bitDepth: Int = 0,
    val lossless: Boolean = false,
)

@Serializable
internal data class ChooseBody(
    val force: Boolean? = null,
    @SerialName("slot_id") val slotId: Long? = null,
)

@Serializable
data class TitleChoice(
    @SerialName("release_id") val releaseId: Long = 0,
    @SerialName("slot_ids") val slotIds: List<Long> = emptyList(),
    val forced: Boolean = false,
    val warnings: List<String> = emptyList(),
    @SerialName("not_sent") val notSent: String = "",
    val sending: Boolean = false,
)

@Serializable
internal data class TurnDownBody(
    @SerialName("slot_id") val slotId: Long? = null,
    @SerialName("block_group") val blockGroup: Boolean? = null,
)

@Serializable
internal data class ReleaseCheckBody(
    val tmdb: Long,
    val name: String,
    @SerialName("pick_rules_id") val pickRulesId: Long,
    val imdb: String = "",
    @SerialName("original_language") val originalLanguage: String = "",
    val year: Int = 0,
)

@Serializable
data class TitleJobIds(
    @SerialName("job_ids") val jobIds: List<Long> = emptyList(),
)

@Serializable
internal data class CheckPickBody(
    @SerialName("release_id") val releaseId: Long,
    @SerialName("title_id") val titleId: Long,
    val force: Boolean,
)

@Serializable
internal data class AddMovieBody(
    val id: String,
    @SerialName("library_folder_id") val libraryFolderId: Long,
    @SerialName("pick_rules_id") val pickRulesId: Long,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean,
    @SerialName("release_gate") val releaseGate: String,
    @SerialName("search_on_add") val searchOnAdd: Boolean,
    @SerialName("more_versions") val moreVersions: List<VersionInput>,
    val follow: Boolean,
)

@Serializable
internal data class AddShowBody(
    val id: String,
    @SerialName("library_folder_id") val libraryFolderId: Long,
    @SerialName("pick_rules_id") val pickRulesId: Long,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean,
    @SerialName("search_on_add") val searchOnAdd: Boolean,
    val seasons: String,
    @SerialName("new_seasons") val newSeasons: String,
    @SerialName("episode_order") val episodeOrder: String,
    @SerialName("details_source") val detailsSource: String,
    @SerialName("more_versions") val moreVersions: List<VersionInput>,
    val follow: Boolean,
)

@Serializable
data class TitleAdded(
    val title: OwnedTitle = OwnedTitle(),
)

@Serializable
data class TitleAddDefaults(
    val movie: TitleKindDefaults = TitleKindDefaults(searchOnAdd = true),
    val show: TitleKindDefaults = TitleKindDefaults(),
)

@Serializable
data class TitleKindDefaults(
    @SerialName("library_folder_id") val libraryFolderId: Long = 0,
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    @SerialName("keep_up_to_date") val keepUpToDate: Boolean = true,
    @SerialName("search_on_add") val searchOnAdd: Boolean = false,
)

@Serializable
data class TitleGateDefaults(
    @SerialName("release_gate") val releaseGate: String = "",
    @SerialName("gate_delay_days") val gateDelayDays: Int = 0,
    @SerialName("bulk_search_respects_gate") val bulkSearchRespectsGate: Boolean = true,
)

@Serializable
data class TitleShowDefaults(
    @SerialName("season_choice") val seasonChoice: String = "",
    @SerialName("new_seasons") val newSeasons: String = "",
    @SerialName("episode_order") val episodeOrder: String = "",
    @SerialName("details_source") val detailsSource: String = "",
    @SerialName("keep_specials") val keepSpecials: Boolean = false,
    @SerialName("episode_wait_minutes") val episodeWaitMinutes: Int = 0,
)

@Serializable
data class TitleLeftovers(
    val leftovers: List<TitleLeftover> = emptyList(),
    val pressings: List<MusicPressingHint> = emptyList(),
)

@Serializable
data class TitleLeftover(
    @SerialName("file_id") val fileId: Long = 0,
    @SerialName("relative_path") val relativePath: String = "",
    val size: Long = 0,
    val resolution: Int = 0,
    val reason: String = "",
    @SerialName("dropped_season") val droppedSeason: Int = 0,
    val guess: TitleLeftoverGuess? = null,
    val music: MusicLeftoverMusic? = null,
    val dismissed: Boolean = false,
) {
    val fileName: String get() = relativePath.split('/', '\\').lastOrNull { it.isNotEmpty() }.orEmpty()
}

@Serializable
data class TitleLeftoverGuess(
    val season: Int = 0,
    val episodes: List<Int> = emptyList(),
)

@Serializable
internal data class MatchBody(
    val items: List<MatchItem>,
    val replace: Boolean,
)

@Serializable
internal data class MatchItem(
    @SerialName("file_id") val fileId: Long,
    @SerialName("episode_ids") val episodeIds: List<Long>,
)

@Serializable
data class TitleMatched(
    val results: List<TitleMatchResult> = emptyList(),
)

@Serializable
data class TitleMatchResult(
    @SerialName("file_id") val fileId: Long = 0,
    val linked: Boolean = false,
    val refused: String = "",
)

@Serializable
internal data class UnmatchBody(
    @SerialName("file_ids") val fileIds: List<Long>,
)

@Serializable
data class TitleUnmatched(
    val unmatched: Int = 0,
)

@Serializable
internal data class DismissBody(
    val dismissed: Boolean,
)

@Serializable
data class TitleDismissed(
    @SerialName("file_id") val fileId: Long = 0,
    val dismissed: Boolean = false,
)

@Serializable
data class TitleJob(
    @SerialName("job_id") val jobId: Long = 0,
)

@Serializable
data class TitleRenamePlan(
    @SerialName("plan_hash") val planHash: String = "",
    val steps: List<TitleRenameStep> = emptyList(),
)

@Serializable
data class TitleRenameStep(
    @SerialName("file_id") val fileId: Long = 0,
    val from: String = "",
    val to: String = "",
    val skip: String = "",
    val origin: String = "",
    val subtitles: List<TitleRenameSubtitle> = emptyList(),
) {
    val changes: Boolean get() = to.isNotEmpty() && to != from && skip.isEmpty()
}

@Serializable
data class TitleRenameSubtitle(
    val from: String = "",
    val to: String = "",
    val skip: String = "",
)

@Serializable
internal data class RenameBody(
    @SerialName("plan_hash") val planHash: String,
    @SerialName("file_ids") val fileIds: List<Long>,
)

@Serializable
data class TitleSubtitles(
    val wishes: List<SubtitleWishRow> = emptyList(),
    val files: List<SubtitleFileView> = emptyList(),
)

@Serializable
data class SubtitleWishRow(
    @SerialName("ID") val id: Long = 0,
    @SerialName("version_id") val versionId: Long = 0,
    @SerialName("item_id") val itemId: Long = 0,
    val languages: List<SubtitleWant> = emptyList(),
)

@Serializable
data class SubtitleFileView(
    @SerialName("file_id") val fileId: Long = 0,
    @SerialName("item_id") val itemId: Long = 0,
    @SerialName("version_id") val versionId: Long = 0,
    @SerialName("version_label") val versionLabel: String = "",
    @SerialName("relative_path") val relativePath: String = "",
    val wish: SubtitleResolved = SubtitleResolved(),
    val subtitles: List<SubtitleRowView> = emptyList(),
) {
    val fileName: String get() = relativePath.split('/', '\\').lastOrNull { it.isNotEmpty() }.orEmpty()
}

@Serializable
data class SubtitleResolved(
    val languages: List<SubtitleWant> = emptyList(),
    val from: String = "",
)

@Serializable
data class SubtitleWant(
    val language: String = "",
    @SerialName("hearing_impaired") val hearingImpaired: String = "normal",
    val forced: Boolean = false,
    @SerialName("translate_from") val translateFrom: String = "",
)

@Serializable
data class SubtitleRowView(
    @SerialName("ID") val id: Long = 0,
    @SerialName("file_id") val fileId: Long = 0,
    val language: String = "",
    val forced: Boolean = false,
    @SerialName("hearing_impaired") val hearingImpaired: Boolean = false,
    val place: String = "",
    val codec: String = "",
    @SerialName("relative_path") val relativePath: String = "",
    val site: String = "",
    val score: Int? = null,
    val origin: String = "",
    val state: String = "",
    @SerialName("saved_at") val savedAt: String = "",
    val tries: Int = 0,
    @SerialName("next_try_at") val nextTryAt: String = "",
    val made: String = "",
    val picture: Boolean = false,
    @SerialName("track_index") val trackIndex: Int? = null,
)

@Serializable
internal data class SubtitleWishBody(
    val languages: List<SubtitleWantBody>? = null,
    val inherit: Boolean? = null,
    @SerialName("item_id") val itemId: Long? = null,
)

@Serializable
internal data class SubtitleWantBody(
    val language: String,
    @SerialName("hearing_impaired") val hearingImpaired: String,
    val forced: Boolean,
    @SerialName("translate_from") val translateFrom: String? = null,
)

@Serializable
data class SubtitleWishes(
    val wishes: List<SubtitleWishRow> = emptyList(),
)

@Serializable
data class SubtitleChoices(
    @SerialName("file_id") val fileId: Long = 0,
    val outcomes: List<SubtitleOutcome> = emptyList(),
    @SerialName("sites_asked") val sitesAsked: List<String> = emptyList(),
)

@Serializable
data class SubtitleOutcome(
    val want: SubtitleWant = SubtitleWant(),
    val result: String = "",
    val reason: String = "",
    val choices: List<SubtitleCandidate> = emptyList(),
    val covered: List<SubtitleCovered> = emptyList(),
)

@Serializable
data class SubtitleCovered(
    val place: String = "",
    val picture: Boolean = false,
)

@Serializable
data class SubtitleCandidate(
    val site: String = "",
    val key: String = "",
    val language: String = "",
    @SerialName("hearing_impaired") val hearingImpaired: Boolean = false,
    val forced: Boolean = false,
    @SerialName("release_names") val releaseNames: List<String> = emptyList(),
    val title: String = "",
    val trusted: Boolean = false,
    val downloads: Int = 0,
    @SerialName("machine_made") val machineMade: String = "",
    val score: SubtitleScore = SubtitleScore(),
) {
    val shownName: String get() = releaseNames.firstOrNull().orEmpty().ifEmpty { title.ifEmpty { key } }
}

@Serializable
data class SubtitleScore(
    val total: Int = 0,
    val parts: List<SubtitleScorePart> = emptyList(),
    val refused: String = "",
)

@Serializable
data class SubtitleScorePart(
    val name: String = "",
    val points: Int = 0,
    val matched: Boolean = false,
)

@Serializable
internal data class SubtitleChooseBody(
    val site: String,
    val key: String,
)

@Serializable
data class SubtitleSaved(
    val path: String = "",
)

@Serializable
internal data class SubtitleTranslateBody(
    val language: String,
    val from: String? = null,
    val forced: Boolean? = null,
    val source: String? = null,
    val path: String? = null,
    val track: Int? = null,
)

@Serializable
data class SubtitleTranslating(
    val queued: Boolean = false,
)

@Serializable
internal data class SubtitleSyncBody(
    @SerialName("offset_ms") val offsetMs: Int? = null,
    @SerialName("from_fps") val fromFps: Double? = null,
    @SerialName("to_fps") val toFps: Double? = null,
)

@Serializable
data class SubtitleCleaned(
    val changed: Boolean = false,
)

@Serializable
data class SubtitlesCleaned(
    val changed: Int = 0,
)

@Serializable
internal data class SubtitleReasonBody(
    val reason: String? = null,
)

@Serializable
data class SubtitleTranslator(
    val kind: String = "",
    val url: String = "",
)
