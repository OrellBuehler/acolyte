package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DownloadsEndpoints
import ch.orellbuehler.acolyte.api.model.DownloadDetail
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.FilePriorityBody
import ch.orellbuehler.acolyte.api.model.PeerInfo
import ch.orellbuehler.acolyte.api.model.ShareLimits
import ch.orellbuehler.acolyte.api.model.Swarm
import ch.orellbuehler.acolyte.api.model.TorrentFile
import ch.orellbuehler.acolyte.api.model.TrackerAddBody
import ch.orellbuehler.acolyte.api.model.TrackerEditBody
import ch.orellbuehler.acolyte.api.model.TrackerInfo
import ch.orellbuehler.acolyte.api.model.TrackerRemoveBody
import ch.orellbuehler.acolyte.api.model.UsenetView
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

enum class DetailTab {
    INFO,
    FILES,
    PEERS,
    TRACKERS,
    SERVERS,
    LOG,
}

enum class InfoGroupKind {
    TRANSFER,
    SWARM,
    DATES,
    RULES,
    WHERE,
}

enum class DownloadFactKind {
    STATUS,
    DOWNLOADED,
    SIZE,
    DOWN,
    UP,
    TIME_LEFT,
    UPLOADED,
    RATIO,
    LAST_ACTIVITY,
    WASTED,
    SEEDING_TIME,
    SEEDS,
    PEERS,
    PRIVATE,
    ADDED,
    POSTED,
    DONE_AT,
    TIME_ACTIVE,
    RELEASE,
    SAVE_PATH,
    DOWNLOADER,
    CATEGORY,
    FOLDER,
    REQUESTED_BY,
    SPEED_LIMITS,
    SEEDING_RULES,
}

data class InfoFact(
    val kind: DownloadFactKind,
    val text: String = "",
    val at: String = "",
    val seconds: Long = 0,
    val a: Long = 0,
    val b: Long = 0,
    val limits: ShareLimits? = null,
)

data class InfoGroup(
    val kind: InfoGroupKind,
    val facts: List<InfoFact>,
)

data class FileNode(
    val path: String,
    val name: String,
    val depth: Int,
    val folder: Boolean,
    val size: Long,
    val progress: Double,
    val priority: Int,
    val indexes: List<Int>,
)

data class ServerRow(
    val name: String,
    val job: String,
    val found: String,
)

data class LogEntry(
    val at: String,
    val day: Day?,
    val summary: String,
    val warn: Boolean,
)

data class RepairStep(
    val stage: String,
    val lines: List<String>,
)

enum class DetailNotice {
    FILES_SET,
    TRACKER_SAVED,
    TRACKER_REMOVED,
    TRACKERS_ADDED,
}

data class DownloadDetailState(
    val row: DownloadRow,
    val tabs: List<DetailTab>,
    val tab: DetailTab = DetailTab.INFO,
    val live: Boolean = false,
    val detail: DownloadDetail? = null,
    val files: List<TorrentFile>? = null,
    val fileFilter: String = "",
    val swarm: Swarm? = null,
    val usenet: UsenetView? = null,
    val reading: Boolean = true,
    val readFailed: Boolean = false,
    val canEdit: Boolean = false,
    val busy: Boolean = false,
    val actionError: ApiError? = null,
    val needsReauth: Boolean = false,
    val notice: DetailNotice? = null,
    val noticeCount: Int = 0,
) {
    val info: List<InfoGroup> get() = DetailRules.info(row, detail)

    val fileTree: List<FileNode> get() = DetailRules.tree(DetailRules.filterFiles(files.orEmpty(), fileFilter))

    val peers: List<PeerInfo> get() = swarm?.peers.orEmpty().sortedBy { it.address }

    val trackers: List<TrackerInfo> get() = swarm?.trackers.orEmpty()

    val servers: List<ServerRow> get() = DetailRules.serverRows(usenet)

    val repair: List<RepairStep> get() = DetailRules.repairSteps(usenet)

    val log: List<LogEntry> get() = DetailRules.log(detail)

    val singleFile: Boolean get() = (files?.size ?: 0) <= 1
}

object DetailRules {
    private val WARN = Regex("fail|error|stall|vanish|missing|errored", RegexOption.IGNORE_CASE)
    private val REPAIR = listOf("download", "repair", "join", "unpack", "rename", "move", "script")

    fun readsLive(r: DownloadRow): Boolean =
        (r.id > 0 || (r.clientItem && r.hash.isNotEmpty())) && !(r.phase == "vanished" && !r.clientItem)

    fun tabs(r: DownloadRow): List<DetailTab> =
        if (DownloadRules.protocolOf(r) == "usenet") {
            listOf(DetailTab.INFO, DetailTab.SERVERS, DetailTab.LOG)
        } else {
            listOf(DetailTab.INFO, DetailTab.FILES, DetailTab.PEERS, DetailTab.TRACKERS, DetailTab.LOG)
        }

    fun info(
        r: DownloadRow,
        d: DownloadDetail?,
    ): List<InfoGroup> {
        val failed = DownloadRules.stageOf(r) == Stage.FAILED
        val status = if (failed && r.text.isNotBlank()) InfoFact(DownloadFactKind.STATUS, r.text.trim()) else null
        val requested = (d?.requestedBy ?: "").ifEmpty { r.requestedBy }
        val downloader = (d?.downloader ?: "").ifEmpty { r.downloader }
        if (DownloadRules.protocolOf(r) == "usenet") {
            val transfer =
                listOfNotNull(
                    status,
                    if (r.progress >=
                        1
                    ) {
                        fact(DownloadFactKind.SIZE, size(r.size))
                    } else {
                        factPair(DownloadFactKind.DOWNLOADED, (r.size * r.progress).toLong(), r.size)
                    },
                    if (r.speed > 0) fact(DownloadFactKind.DOWN, speed(r.speed)) else null,
                    if (r.eta > 0) InfoFact(DownloadFactKind.TIME_LEFT, seconds = r.eta) else null,
                )
            val dates =
                listOfNotNull(
                    at(DownloadFactKind.POSTED, r.postedAt),
                    at(DownloadFactKind.ADDED, r.startedAt),
                    at(DownloadFactKind.DONE_AT, r.finishedAt),
                )
            val where =
                listOfNotNull(
                    fact(DownloadFactKind.RELEASE, r.name),
                    fact(DownloadFactKind.DOWNLOADER, downloader),
                    fact(DownloadFactKind.CATEGORY, r.category),
                    fact(DownloadFactKind.FOLDER, r.importFolder.ifEmpty { r.path }),
                    fact(DownloadFactKind.REQUESTED_BY, requested),
                )
            return listOf(
                InfoGroup(InfoGroupKind.TRANSFER, transfer),
                InfoGroup(InfoGroupKind.DATES, dates),
                InfoGroup(InfoGroupKind.WHERE, where),
            ).filter { it.facts.isNotEmpty() }
        }
        val t = d?.torrent
        val seeding = t != null && (t.downloaded >= t.size && t.size > 0) || r.progress >= 1
        val ratio = t?.ratio ?: r.ratio
        val transfer =
            if (seeding) {
                listOfNotNull(
                    status,
                    ratio?.let { fact(DownloadFactKind.RATIO, DownloadRules.ratioText(it, 2)) },
                    t?.uploaded?.takeIf { it > 0 }?.let { fact(DownloadFactKind.UPLOADED, size(it)) },
                    t?.seedingTime?.takeIf { it > 0 }?.let { InfoFact(DownloadFactKind.SEEDING_TIME, seconds = it) },
                    t?.let { at(DownloadFactKind.LAST_ACTIVITY, it.lastActivity) },
                    fact(DownloadFactKind.SIZE, size(t?.size ?: r.size)),
                    (t?.upSpeed ?: r.upSpeed).takeIf { it > 0 }?.let { fact(DownloadFactKind.UP, speed(it)) },
                    (t?.speed ?: r.speed).takeIf { it > 0 }?.let { fact(DownloadFactKind.DOWN, speed(it)) },
                    t?.wasted?.takeIf { it > 0 }?.let { fact(DownloadFactKind.WASTED, size(it)) },
                )
            } else {
                listOfNotNull(
                    status,
                    factPair(DownloadFactKind.DOWNLOADED, t?.downloaded ?: (r.size * r.progress).toLong(), t?.size ?: r.size),
                    (t?.speed ?: r.speed).takeIf { it > 0 }?.let { fact(DownloadFactKind.DOWN, speed(it)) },
                    (t?.upSpeed ?: r.upSpeed).takeIf { it > 0 }?.let { fact(DownloadFactKind.UP, speed(it)) },
                    (t?.eta ?: r.eta).takeIf { it > 0 }?.let { InfoFact(DownloadFactKind.TIME_LEFT, seconds = it) },
                    t?.uploaded?.takeIf { it > 0 }?.let { fact(DownloadFactKind.UPLOADED, size(it)) },
                    ratio?.let { fact(DownloadFactKind.RATIO, DownloadRules.ratioText(it, 2)) },
                    t?.let { at(DownloadFactKind.LAST_ACTIVITY, it.lastActivity) },
                    t?.wasted?.takeIf { it > 0 }?.let { fact(DownloadFactKind.WASTED, size(it)) },
                )
            }
        val seeds = t?.seeds ?: r.seeds
        val seedsTotal = t?.seedsTotal ?: r.seedsTotal
        val peers = t?.peers ?: r.peers
        val peersTotal = t?.peersTotal ?: r.peersTotal
        val swarm =
            listOfNotNull(
                InfoFact(
                    DownloadFactKind.SEEDS,
                    a = seeds.toLong(),
                    b =
                        if (seedsTotal >= seeds &&
                            seedsTotal > 0
                        ) {
                            seedsTotal.toLong()
                        } else {
                            0
                        },
                ),
                InfoFact(
                    DownloadFactKind.PEERS,
                    a = peers.toLong(),
                    b =
                        if (peersTotal >= peers &&
                            peersTotal > 0
                        ) {
                            peersTotal.toLong()
                        } else {
                            0
                        },
                ),
                t?.let { InfoFact(DownloadFactKind.PRIVATE, a = if (it.private) 1 else 0) },
            )
        val dates =
            listOfNotNull(
                at(DownloadFactKind.ADDED, t?.addedAt?.ifEmpty { null } ?: r.startedAt),
                at(DownloadFactKind.DONE_AT, t?.doneAt?.ifEmpty { null } ?: r.finishedAt),
                t?.timeActive?.takeIf { it > 0 }?.let { InfoFact(DownloadFactKind.TIME_ACTIVE, seconds = it) },
                if (!seeding) t?.seedingTime?.takeIf { it > 0 }?.let { InfoFact(DownloadFactKind.SEEDING_TIME, seconds = it) } else null,
                fact(DownloadFactKind.RELEASE, r.name),
                fact(DownloadFactKind.SAVE_PATH, t?.savePath?.ifEmpty { null } ?: r.path),
                fact(DownloadFactKind.DOWNLOADER, downloader),
                fact(DownloadFactKind.REQUESTED_BY, requested),
            )
        val rules =
            listOfNotNull(
                if (r.limitDown > 0 || r.limitUp > 0) InfoFact(DownloadFactKind.SPEED_LIMITS, a = r.limitDown, b = r.limitUp) else null,
                r.limits?.let { InfoFact(DownloadFactKind.SEEDING_RULES, limits = it) },
            )
        return listOf(
            InfoGroup(InfoGroupKind.TRANSFER, transfer),
            InfoGroup(InfoGroupKind.SWARM, if (t != null || seeds > 0 || peers > 0) swarm else emptyList()),
            InfoGroup(InfoGroupKind.DATES, dates),
            InfoGroup(InfoGroupKind.RULES, rules),
        ).filter { it.facts.isNotEmpty() }
    }

    private fun fact(
        kind: DownloadFactKind,
        text: String,
    ): InfoFact? = text.takeIf { it.isNotBlank() }?.let { InfoFact(kind, it) }

    private fun at(
        kind: DownloadFactKind,
        iso: String?,
    ): InfoFact? = iso?.takeIf { it.isNotBlank() && Days.parseInstant(it) != null }?.let { InfoFact(kind, at = it) }

    private fun factPair(
        kind: DownloadFactKind,
        a: Long,
        b: Long,
    ): InfoFact? = if (b > 0) InfoFact(kind, a = a, b = b) else null

    fun size(bytes: Long): String = DownloadFormat.size(bytes)

    fun speed(bytes: Long): String = DownloadFormat.size(bytes) + "/s"

    fun filterFiles(
        files: List<TorrentFile>,
        query: String,
    ): List<TorrentFile> {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return files
        return files.filter { f -> words.all { f.name.lowercase().contains(it) } }
    }

    fun tree(files: List<TorrentFile>): List<FileNode> {
        val sorted = files.sortedWith { a, b -> DownloadRules.naturalCompare(a.name, b.name) }
        val out = mutableListOf<FileNode>()
        val folders = mutableMapOf<String, Int>()
        for (f in sorted) {
            val parts = f.name.split('/').filter { it.isNotEmpty() }
            for (depth in 0 until parts.size - 1) {
                val path = parts.take(depth + 1).joinToString("/")
                if (path !in folders) {
                    folders[path] = out.size
                    out += FileNode(path, parts[depth], depth, true, 0, 0.0, f.priority, emptyList())
                }
                val i = folders.getValue(path)
                val node = out[i]
                val total = node.size + f.size
                val done = node.progress * node.size + f.progress * f.size
                out[i] =
                    node.copy(
                        size = total,
                        progress = if (total > 0) done / total else 0.0,
                        priority = if (node.indexes.isEmpty() || node.priority == f.priority) f.priority else -1,
                        indexes = node.indexes + f.index,
                    )
            }
            out +=
                FileNode(
                    f.name,
                    parts.lastOrNull() ?: f.name,
                    (parts.size - 1).coerceAtLeast(0),
                    false,
                    f.size,
                    f.progress,
                    f.priority,
                    listOf(f.index),
                )
        }
        return out
    }

    fun serverRows(v: UsenetView?): List<ServerRow> {
        if (v == null) return emptyList()
        val stats =
            v.servers
                ?.servers
                .orEmpty()
                .associateBy { it.name }
                .toMutableMap()
        val rows =
            v.jobServers.map { j ->
                val s = stats.remove(j.name)
                ServerRow(j.name, j.amount, s?.let { found(it.articlesFound, it.articlesTried) }.orEmpty())
            }
        return rows + stats.values.map { ServerRow(it.name, "", found(it.articlesFound, it.articlesTried)) }
    }

    private fun found(
        hit: Long,
        tried: Long,
    ): String = if (tried <= 0) "" else "${kotlin.math.round(hit * 100.0 / tried).toLong()}%"

    fun repairSteps(v: UsenetView?): List<RepairStep> {
        if (v == null) return emptyList()
        return REPAIR.mapNotNull { key ->
            val lines = v.stages.filter { it.stage == key }.flatMap { it.lines }
            if (lines.isEmpty()) null else RepairStep(key, lines)
        }
    }

    fun log(d: DownloadDetail?): List<LogEntry> =
        d?.log.orEmpty().map { LogEntry(it.at, Days.localDayOf(it.at), it.summary, WARN.containsMatchIn(it.kind)) }
}

class DownloadDetailPresenter internal constructor(
    private val api: Api,
    row: DownloadRow,
    private val manager: Boolean,
    canSettings: Boolean,
) {
    private val mutableState =
        MutableStateFlow(
            DownloadDetailState(
                row = row,
                tabs = DetailRules.tabs(row),
                live = manager && DetailRules.readsLive(row),
                canEdit = manager && DownloadRules.mayEdit(row, canSettings) && DownloadRules.qbitTorrent(row),
            ),
        )
    private val lock = Mutex()

    val state: StateFlow<DownloadDetailState> = mutableState.asStateFlow()

    private val row: DownloadRow get() = mutableState.value.row

    private val client: Boolean get() = row.clientItem || row.id <= 0

    fun update(row: DownloadRow) {
        mutableState.update { it.copy(row = row) }
    }

    fun setTab(tab: DetailTab) {
        mutableState.update { it.copy(tab = tab) }
    }

    fun setFileFilter(text: String) {
        mutableState.update { it.copy(fileFilter = text) }
    }

    fun consumeNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    fun clearActionError() {
        mutableState.update { it.copy(actionError = null, needsReauth = false) }
    }

    private fun params(): Array<Pair<String, Any?>> =
        if (client) {
            arrayOf("id" to row.downloaderId, "hash" to row.hash)
        } else {
            arrayOf(
                "id" to row.id,
            )
        }

    suspend fun refresh() {
        if (!mutableState.value.live) {
            mutableState.update { it.copy(reading = false) }
            return
        }
        lock.withLock {
            val p = params()
            val detail = api.result(if (client) DownloadsEndpoints.itemDetail else DownloadsEndpoints.detail, *p)
            val tab = mutableState.value.tab
            val usenet = DownloadRules.protocolOf(row) == "usenet"
            val files =
                if (tab == DetailTab.FILES &&
                    !usenet
                ) {
                    api.result(if (client) DownloadsEndpoints.itemFiles else DownloadsEndpoints.files, *p)
                } else {
                    null
                }
            val swarm =
                if ((tab == DetailTab.PEERS || tab == DetailTab.TRACKERS) && !usenet) {
                    api.result(if (client) DownloadsEndpoints.itemSwarm else DownloadsEndpoints.swarm, *p)
                } else {
                    null
                }
            val news =
                if ((tab == DetailTab.SERVERS || tab == DetailTab.LOG) && usenet) {
                    api.result(if (client) DownloadsEndpoints.itemUsenet else DownloadsEndpoints.usenet, *p)
                } else {
                    null
                }
            mutableState.update { s ->
                s.copy(
                    reading = false,
                    detail = detail.valueOrNull() ?: s.detail,
                    files = files?.valueOrNull()?.files ?: s.files,
                    swarm = swarm?.valueOrNull() ?: s.swarm,
                    usenet = news?.valueOrNull() ?: s.usenet,
                    readFailed = (detail is ApiResult.Failed && s.detail == null) || (files is ApiResult.Failed && s.files == null),
                )
            }
        }
    }

    suspend fun watch() {
        while (true) {
            refresh()
            delay(3.seconds)
        }
    }

    suspend fun setFilePriority(
        indexes: List<Int>,
        priority: Int,
    ) {
        if (indexes.isEmpty() || !mutableState.value.canEdit) return
        val body = FilePriorityBody(indexes.distinct().sorted(), priority)
        mutableState.update { it.copy(busy = true, actionError = null, needsReauth = false) }
        val result =
            if (client) {
                api.result(DownloadsEndpoints.itemSetFiles, body, "id" to row.downloaderId, "hash" to row.hash)
            } else {
                api.result(DownloadsEndpoints.setFiles, body, "id" to row.id)
            }
        mutableState.update { it.copy(busy = false) }
        result
            .onSuccess {
                mutableState.update { it.copy(notice = DetailNotice.FILES_SET) }
                refresh()
            }.onFailure { error -> fail(error) }
    }

    suspend fun addTrackers(text: String): List<String> {
        val (urls, bad) = DownloadRules.splitTrackers(text)
        if (bad.isNotEmpty() || urls.isEmpty()) return bad.ifEmpty { listOf("") }
        tracker(DetailNotice.TRACKERS_ADDED, urls.size) {
            api.result(
                DownloadsEndpoints.addTrackers,
                TrackerAddBody(urls),
                "id" to row.downloaderId,
                "hash" to row.hash,
            )
        }
        return emptyList()
    }

    suspend fun editTracker(
        ref: String,
        url: String,
        tier: Int?,
    ) {
        val body = TrackerEditBody(ref, url.trim().ifEmpty { null }, tier?.coerceIn(0, 255))
        if (body.url == null && body.tier == null) return
        tracker(DetailNotice.TRACKER_SAVED, 1) {
            api.result(
                DownloadsEndpoints.editTracker,
                body,
                "id" to row.downloaderId,
                "hash" to row.hash,
            )
        }
    }

    suspend fun removeTracker(ref: String) {
        tracker(DetailNotice.TRACKER_REMOVED, 1) {
            api.result(DownloadsEndpoints.removeTrackers, TrackerRemoveBody(listOf(ref)), "id" to row.downloaderId, "hash" to row.hash)
        }
    }

    private suspend fun tracker(
        notice: DetailNotice,
        count: Int,
        call: suspend () -> ApiResult<*>,
    ) {
        if (!mutableState.value.canEdit || row.hash.isEmpty()) return
        mutableState.update { it.copy(busy = true, actionError = null) }
        val result = call()
        mutableState.update { it.copy(busy = false) }
        when (result) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(notice = notice, noticeCount = count) }
                refresh()
            }
            is ApiResult.Failed -> fail(result.error)
        }
    }

    private fun fail(error: ApiError) {
        val reauth = error.code == ApiErrorCode.REAUTH || error.message.contains("confirm your local password", ignoreCase = true)
        mutableState.update { it.copy(actionError = error, needsReauth = reauth) }
    }
}
