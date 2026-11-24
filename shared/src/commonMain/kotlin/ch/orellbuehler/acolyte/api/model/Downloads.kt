package ch.orellbuehler.acolyte.api.model

import ch.orellbuehler.acolyte.api.FormBody
import ch.orellbuehler.acolyte.api.FormFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

@Serializable
data class DownloadRow(
    val id: Long = 0,
    val state: String = "",
    val label: String = "",
    val text: String = "",
    val progress: Double = 0.0,
    val speed: Long = 0,
    @SerialName("up_speed") val upSpeed: Long = 0,
    val seeds: Int = 0,
    val peers: Int = 0,
    @SerialName("seeds_total") val seedsTotal: Int = 0,
    @SerialName("peers_total") val peersTotal: Int = 0,
    val eta: Long = 0,
    val active: Boolean = false,
    val seeding: Boolean = false,
    val pack: PackRange? = null,
    val phase: String = "",
    val name: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val title: String = "",
    val year: Int = 0,
    val version: String = "",
    val downloader: String = "",
    val path: String = "",
    @SerialName("downloader_kind") val downloaderKind: String = "",
    val protocol: String = "",
    val size: Long = 0,
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("finished_at") val finishedAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("slot_state") val slotState: String = "",
    @SerialName("can_act") val canAct: Boolean = false,
    @SerialName("client_locked") val clientLocked: Boolean = false,
    @SerialName("downloader_gone") val downloaderGone: Boolean = false,
    @SerialName("can_import") val canImport: Boolean = false,
    @SerialName("refused_why") val refusedWhy: String = "",
    @SerialName("import_folder") val importFolder: String = "",
    @SerialName("cleanup_wait") val cleanupWait: List<String> = emptyList(),
    val replaced: Boolean = false,
    val kind: String = "",
    val episodes: List<EpisodeLine> = emptyList(),
    val covers: String = "",
    val album: String = "",
    @SerialName("client_item") val clientItem: Boolean = false,
    @SerialName("files_stay") val filesStay: Boolean = false,
    val outside: Boolean = false,
    val linked: Boolean = false,
    @SerialName("downloader_id") val downloaderId: Long = 0,
    val hash: String = "",
    val category: String = "",
    @SerialName("requested_by") val requestedBy: String = "",
    @SerialName("source_name") val sourceName: String = "",
    @SerialName("source_id") val sourceId: Long = 0,
    @SerialName("posted_at") val postedAt: String = "",
    val ratio: Double? = null,
    val limits: ShareLimits? = null,
    @SerialName("client_hint") val clientHint: String = "",
    @SerialName("queue_pos") val queuePos: Int = 0,
    val tracker: String = "",
    @SerialName("clear_waiting") val clearWaiting: ClearWait? = null,
    @SerialName("tracker_problem") val trackerProblem: String = "",
    val availability: Double? = null,
    @SerialName("live_key") val liveKey: String = "",
    @SerialName("limit_down") val limitDown: Long = 0,
    @SerialName("limit_up") val limitUp: Long = 0,
)

@Serializable
data class PackRange(
    val from: Int = 0,
    val to: Int = 0,
    val taken: Int = 0,
    val total: Int = 0,
)

@Serializable
data class EpisodeLine(
    @SerialName("slot_id") val slotId: Long = 0,
    val season: Int = 0,
    val episode: Int = 0,
    val label: String = "",
    val name: String = "",
    val state: String = "",
    val track: Boolean = false,
)

@Serializable
data class ClearWait(
    @SerialName("next_try") val nextTry: String = "",
    val tries: Int = 0,
    val reason: String = "",
)

@Serializable
data class ShareLimits(
    val ratio: Double = -2.0,
    @SerialName("seeding_minutes") val seedingMinutes: Long = -2,
    @SerialName("inactive_minutes") val inactiveMinutes: Long = -2,
    val action: String = "",
)

@Serializable
data class ClientHealth(
    val id: Long = 0,
    val name: String = "",
    val health: String = "",
    val stale: Boolean = false,
    @SerialName("seen_at") val seenAt: String = "",
)

@Serializable
data class RestOf(
    val protocol: String = "",
    val stages: Map<String, Int> = emptyMap(),
)

@Serializable
data class SpeedTotals(
    val speed: Long = 0,
    @SerialName("up_speed") val upSpeed: Long = 0,
)

@Serializable
data class DownloadsView(
    val downloads: List<DownloadRow> = emptyList(),
    val totals: SpeedTotals = SpeedTotals(),
    val clients: List<ClientHealth> = emptyList(),
    @SerialName("outside_total") val outsideTotal: Int = 0,
    @SerialName("next_cursor") val nextCursor: String = "",
    @SerialName("outside_rest") val outsideRest: Map<String, RestOf> = emptyMap(),
    val revs: Map<String, Long>? = null,
    val boot: String = "",
)

@Serializable
data class ClientStatus(
    val paused: Boolean = false,
    val speed: Long = 0,
    @SerialName("up_speed") val upSpeed: Long = 0,
    @SerialName("speed_limit") val speedLimit: Long = 0,
    @SerialName("up_limit") val upLimit: Long = 0,
    @SerialName("free_space") val freeSpace: Long = 0,
    val connection: String = "",
    @SerialName("slow_mode") val slowMode: Boolean? = null,
    @SerialName("time_left") val timeLeft: Long = 0,
)

@Serializable
data class ClientView(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    val protocol: String = "",
    val controls: Boolean = false,
    @SerialName("torrent_controls") val torrentControls: Boolean = false,
    val status: ClientStatus? = null,
    val error: String = "",
)

@Serializable
data class ClientViews(
    val clients: List<ClientView> = emptyList(),
)

@Serializable
data class ImportRecord(
    @SerialName("ID") val id: Long = 0,
    @SerialName("DownloadID") val downloadRef: Long? = null,
    @SerialName("Kind") val kind: String = "",
    @SerialName("State") val state: String = "",
    @SerialName("Plan") val planSteps: List<ImportPlanStep>? = null,
    @SerialName("Result") val results: List<ImportResult>? = null,
) {
    val downloadId: Long get() = downloadRef ?: 0

    val plan: List<ImportPlanStep> get() = planSteps.orEmpty()

    val result: List<ImportResult> get() = results.orEmpty()
}

@Serializable
data class ImportPlanStep(
    @SerialName("source_file") val sourceFile: String = "",
    @SerialName("target_path") val targetPath: String = "",
)

@Serializable
data class ImportResult(
    @SerialName("source_file") val sourceFile: String = "",
    val outcome: String = "",
    @SerialName("placed_by") val placedBy: String = "",
    val error: String = "",
)

@Serializable
data class ImportList(
    val imports: List<ImportRecord> = emptyList(),
)

@Serializable
data class Decision(
    @SerialName("slot_id") val slotId: Long = 0,
    @SerialName("title_id") val titleId: Long = 0,
    val title: String = "",
    val year: Int = 0,
    val version: String = "",
    val versions: Int = 0,
    val why: String = "",
    val reason: String = "",
    @SerialName("release_id") val releaseId: Long = 0,
    @SerialName("release_name") val releaseName: String = "",
    @SerialName("has_copy") val hasCopy: Boolean = false,
    val since: String = "",
    @SerialName("retry_at") val retryAt: String = "",
    val kind: String = "",
    val episode: EpisodeLine? = null,
    val album: String = "",
    @SerialName("download_id") val downloadId: Long = 0,
    @SerialName("pack_slot_ids") val packSlotIds: List<Long> = emptyList(),
    val pack: List<EpisodeLine> = emptyList(),
    @SerialName("import_file") val importFile: String = "",
    @SerialName("import_folder") val importFolder: String = "",
    val reading: Boolean = false,
)

@Serializable
data class DecisionList(
    val items: List<Decision> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class DecideBody(
    val action: String,
    val pack: Boolean = false,
    val replace: Boolean = false,
)

@Serializable
data class DecideAnswer(
    val action: String = "",
    @SerialName("job_ids") val jobIds: List<Long> = emptyList(),
    val stopped: Int = 0,
)

@Serializable
data class EditionBody(
    val edition: String,
    val confirm: Boolean = true,
    val retry: Boolean = true,
)

@Serializable
data class EditionAnswer(
    @SerialName("download_id") val downloadId: Long = 0,
    val edition: String = "",
    val confirmed: Boolean = false,
)

@Serializable
data class RemoveBody(
    @SerialName("delete_files") val deleteFiles: Boolean = false,
    @SerialName("turn_down") val turnDown: Boolean = false,
    @SerialName("search_again") val searchAgain: Boolean = false,
)

@Serializable
data class DeleteFilesBody(
    @SerialName("delete_files") val deleteFiles: Boolean = false,
)

@Serializable
data class RemoveAnswer(
    val id: Long = 0,
    val state: String = "",
    @SerialName("client_note") val clientNote: String = "",
)

@Serializable
data class DlOk(
    val ok: Boolean = false,
    val count: Int = 0,
    val id: String = "",
    val folder: String = "",
)

@Serializable
data class PriorityBody(
    val priority: String,
)

@Serializable
data class BulkBody(
    val action: String,
    val hashes: List<String>,
    @SerialName("delete_files") val deleteFiles: Boolean? = null,
)

@Serializable
data class ShareLimitsBody(
    val hashes: List<String>,
    val ratio: Double,
    @SerialName("seeding_minutes") val seedingMinutes: Long,
    @SerialName("inactive_minutes") val inactiveMinutes: Long,
    val action: String,
)

@Serializable
data class LocationBody(
    val hashes: List<String>,
    val folder: String,
    val category: String? = null,
)

@Serializable
data class TorrentSpeedBody(
    val hashes: List<String>,
    val down: Long,
    val up: Long,
)

@Serializable
data class SpeedLimitBody(
    val down: Long,
    val up: Long,
)

@Serializable
data class SlowModeBody(
    val on: Boolean,
)

@Serializable
data class SlowModeAnswer(
    val ok: Boolean = false,
    @SerialName("slow_mode") val slowMode: Boolean = false,
)

@Serializable
internal data class AddTorrentsBody(
    val links: List<String>,
    val category: String,
    @SerialName("save_path") val savePath: String,
    val paused: Boolean,
    @Transient val files: List<TorrentUpload> = emptyList(),
) : FormBody {
    override val formFields: List<Pair<String, String>>
        get() = listOf("links" to links.joinToString("\n"), "category" to category, "save_path" to savePath, "paused" to paused.toString())

    override val formFiles: List<FormFile>
        get() = files.map { FormFile("torrents", it.name, "application/x-bittorrent", it.bytes) }
}

class TorrentUpload(
    val name: String,
    val bytes: ByteArray,
)

@Serializable
data class AddOutcome(
    val added: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0,
    val duplicates: Int = 0,
    val bad: List<String> = emptyList(),
    val dropped: Int = 0,
)

@Serializable
data class FilePriorityBody(
    val indexes: List<Int>,
    val priority: Int,
)

@Serializable
data class TrackerAddBody(
    val urls: List<String>,
)

@Serializable
data class TrackerEditBody(
    val ref: String,
    val url: String? = null,
    val tier: Int? = null,
)

@Serializable
data class TrackerRemoveBody(
    val refs: List<String>,
)

@Serializable
data class DownloadDetail(
    val downloader: String = "",
    @SerialName("requested_by") val requestedBy: String = "",
    val torrent: TorrentDetail? = null,
    @SerialName("torrent_error") val torrentError: String = "",
    val log: List<LogLine> = emptyList(),
)

@Serializable
data class TorrentDetail(
    val size: Long = 0,
    val downloaded: Long = 0,
    val uploaded: Long = 0,
    val wasted: Long = 0,
    val speed: Long = 0,
    @SerialName("up_speed") val upSpeed: Long = 0,
    val eta: Long = 0,
    val ratio: Double = 0.0,
    val seeds: Int = 0,
    @SerialName("seeds_total") val seedsTotal: Int = 0,
    val peers: Int = 0,
    @SerialName("peers_total") val peersTotal: Int = 0,
    @SerialName("added_at") val addedAt: String = "",
    @SerialName("done_at") val doneAt: String = "",
    @SerialName("save_path") val savePath: String = "",
    val private: Boolean = false,
    @SerialName("seeding_time") val seedingTime: Long = 0,
    @SerialName("time_active") val timeActive: Long = 0,
    @SerialName("last_activity") val lastActivity: String = "",
)

@Serializable
data class LogLine(
    val at: String = "",
    val kind: String = "",
    val summary: String = "",
)

@Serializable
data class TorrentFile(
    val index: Int = 0,
    val name: String = "",
    val size: Long = 0,
    val progress: Double = 0.0,
    val priority: Int = 1,
)

@Serializable
data class TorrentFiles(
    val files: List<TorrentFile> = emptyList(),
)

@Serializable
data class TrackerInfo(
    val url: String = "",
    val tier: Int = 0,
    val status: String = "",
    val message: String = "",
    val seeds: Int = 0,
    val peers: Int = 0,
    @SerialName("next_check") val nextCheck: Long = 0,
    val fixed: Boolean = false,
    val ref: String = "",
)

@Serializable
data class PeerInfo(
    val address: String = "",
    val client: String = "",
    val progress: Double = 0.0,
    val speed: Long = 0,
    @SerialName("up_speed") val upSpeed: Long = 0,
    val country: String = "",
)

@Serializable
data class Swarm(
    val trackers: List<TrackerInfo> = emptyList(),
    val peers: List<PeerInfo> = emptyList(),
)

@Serializable
data class UsenetStage(
    val stage: String = "",
    val lines: List<String> = emptyList(),
)

@Serializable
data class JobServer(
    val name: String = "",
    val amount: String = "",
)

@Serializable
data class ServerStat(
    val name: String = "",
    val total: Long = 0,
    @SerialName("articles_tried") val articlesTried: Long = 0,
    @SerialName("articles_found") val articlesFound: Long = 0,
)

@Serializable
data class ServerStats(
    val servers: List<ServerStat> = emptyList(),
)

@Serializable
data class UsenetView(
    val stages: List<UsenetStage> = emptyList(),
    @SerialName("job_servers") val jobServers: List<JobServer> = emptyList(),
    val servers: ServerStats? = null,
)

@Serializable
data class MatchCandidate(
    val id: String = "",
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    val poster: String = "",
    val score: Int = 0,
)

@Serializable
data class MatchView(
    val id: Long = 0,
    @SerialName("downloader_id") val downloaderId: Long = 0,
    val hash: String = "",
    val name: String = "",
    val state: String = "",
    val candidates: List<MatchCandidate> = emptyList(),
)

@Serializable
data class MatchList(
    val matches: List<MatchView> = emptyList(),
    val pending: Int = 0,
)

@Serializable
data class MatchPickBody(
    val candidate: String? = null,
    @SerialName("not_media") val notMedia: Boolean? = null,
    val skip: Boolean? = null,
)

@Serializable
data class MatchRunAnswer(
    val ok: Boolean = false,
    val checked: Int = 0,
)

@Serializable
data class DownloaderInfo(
    val id: Long = 0,
    val name: String = "",
    val protocol: String = "",
    val kind: String = "",
    val writable: Boolean = false,
    val category: String = "",
)

@Serializable
data class DownloaderList(
    val downloaders: List<DownloaderInfo> = emptyList(),
)

@Serializable
data class DownloaderSettings(
    @SerialName("remove_delete_files") val removeDeleteFiles: Boolean = true,
    @SerialName("remove_turn_down") val removeTurnDown: Boolean = false,
    @SerialName("remove_search_again") val removeSearchAgain: Boolean = true,
)

@Serializable
data class LiveInterestBody(
    val view: String = "downloads",
)

@Serializable
data class LiveInterestAnswer(
    @SerialName("ttl_seconds") val ttlSeconds: Int = 0,
)

@Serializable
data class TitleBrief(
    val id: Long = 0,
    val kind: String = "",
    val name: String = "",
    val artwork: List<String> = emptyList(),
    @SerialName("cinema_date") val cinemaDate: String = "",
    @SerialName("digital_date") val digitalDate: String = "",
    @SerialName("physical_date") val physicalDate: String = "",
    val slots: List<TitleBriefSlot> = emptyList(),
)

@Serializable
data class TitleBriefSlot(
    val state: String = "",
)

@Serializable
data class TitleBriefList(
    val titles: List<TitleBrief> = emptyList(),
)

@Serializable
data class TitleRef(
    val id: Long = 0,
    val kind: String = "",
    val name: String = "",
    val ids: JsonObject? = null,
)
