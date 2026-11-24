package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.AddOutcome
import ch.orellbuehler.acolyte.api.model.ClientHealth
import ch.orellbuehler.acolyte.api.model.ClientView
import ch.orellbuehler.acolyte.api.model.Decision
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.DownloaderInfo
import ch.orellbuehler.acolyte.api.model.GridTitle
import ch.orellbuehler.acolyte.api.model.ImportRecord
import ch.orellbuehler.acolyte.api.model.MatchView
import ch.orellbuehler.acolyte.api.model.RestOf
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone

data class DownloadItem(
    val key: String,
    val row: DownloadRow,
    val stage: QueueStage,
    val name: String,
    val title: String,
    val subLine: SubLine,
    val word: StatusWord,
    val note: RowNote?,
    val problem: RowProblem?,
    val detail: RowDetail?,
    val bar: BarTone,
    val tone: Tone,
    val progress: Double,
    val moving: Boolean,
    val history: Boolean,
    val poster: String?,
    val square: Boolean,
    val quality: String,
    val protocol: String,
    val actions: List<DownloadAction>,
    val decisions: List<Decision>,
    val selectable: Boolean,
)

data class DownloadsFilter(
    val tab: DownloadsTab = DownloadsTab.ALL,
    val sub: DownloadsSub? = null,
    val query: String = "",
    val kind: DownloadKindFilter = DownloadKindFilter.ALL,
    val network: NetworkFilter = NetworkFilter.ANY,
    val tracker: String = "",
    val sort: SortOrder? = null,
) {
    val filtersOn: Int get() = listOf(kind != DownloadKindFilter.ALL, network != NetworkFilter.ANY, tracker.isNotEmpty()).count { it }
}

enum class DownloadsEmptyKind {
    NO_MATCHES,
    NO_KIND,
    NO_NETWORK,
    LOADING_MORE,
    TAB,
    SUB,
    NOTHING,
}

data class DownloadsEmpty(
    val kind: DownloadsEmptyKind,
    val count: Int = 0,
)

enum class ClientNoteKind {
    WAITING,
    STALE,
    OFFLINE,
}

data class ClientNote(
    val name: String,
    val kind: ClientNoteKind,
)

enum class NoticeKind {
    PAUSED,
    STARTED,
    RETRIED,
    MOVED_TOP,
    MOVED_BOTTOM,
    MOVED_UP,
    MOVED_DOWN,
    PRIORITY_SET,
    CHECKING,
    ANNOUNCED,
    FORCE_STARTED,
    IMPORT_QUEUED,
    REMOVED,
    REMOVED_KEPT,
    MARKED_FAILED,
    ALL_STARTED,
    ALL_PAUSED,
    ALL_FAILED,
    SPEED_SAVED,
    SEEDING_SET,
    MOVING,
    SPEEDS_SET,
    ADDED,
    SEARCHING,
    TURNED_DOWN,
    STOPPED,
    IMPORTED,
    EDITION_SET,
    MATCHING,
    MATCHED,
    DOWNLOAD_FAILED,
    BULK_DONE,
    NOT_HERE,
}

data class DownloadsNotice(
    val id: Long,
    val kind: NoticeKind,
    val count: Int = 0,
    val skipped: Int = 0,
    val text: String = "",
    val action: DownloadAction? = null,
    val outcome: AddOutcome? = null,
    val tone: Tone = Tone.OK,
)

data class DownloadTitleLink(
    val kind: String,
    val tmdb: String,
    val libraryId: Long = 0,
    val openLibrary: Boolean = false,
)

data class MatchesInfo(
    val matches: List<MatchView> = emptyList(),
    val pending: Int = 0,
    val loaded: Boolean = false,
)

data class StatusLine(
    val speed: Long,
    val upSpeed: Long,
    val free: Long,
    val left: Long,
    val review: Int,
    val limited: Boolean,
)

data class DownloadsState(
    val status: LoadStatus = LoadStatus(),
    val manager: Boolean = false,
    val canSettings: Boolean = false,
    val filter: DownloadsFilter = DownloadsFilter(),
    val items: List<DownloadItem> = emptyList(),
    val total: Int = 0,
    val tabs: List<TabCount> = emptyList(),
    val subs: List<SubCount> = emptyList(),
    val trackers: List<TrackerOption> = emptyList(),
    val trackerIssues: Int = 0,
    val empty: DownloadsEmpty? = null,
    val clients: List<ClientHealth> = emptyList(),
    val clientViews: List<ClientView> = emptyList(),
    val notes: List<ClientNote> = emptyList(),
    val statusLine: StatusLine = StatusLine(0, 0, 0, 0, 0, false),
    val decisions: List<Decision> = emptyList(),
    val matches: MatchesInfo = MatchesInfo(),
    val addTargets: List<DownloaderInfo> = emptyList(),
    val moreOutside: Int = 0,
    val loadingMore: Boolean = false,
    val selection: Set<String> = emptySet(),
    val busy: Set<String> = emptySet(),
    val batchBusy: Boolean = false,
    val notice: DownloadsNotice? = null,
    val actionError: ApiError? = null,
    val pushOn: Boolean = false,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && items.isEmpty() && total == 0 && filter == DownloadsFilter()

    val selecting: Boolean get() = selection.isNotEmpty()

    val reviewCount: Int get() = decisions.size

    fun item(key: String): DownloadItem? = items.firstOrNull { it.key == key }
}

internal data class DownloadsRaw(
    val rows: List<DownloadRow> = emptyList(),
    val imports: Map<Long, ImportRecord> = emptyMap(),
    val decisions: List<Decision> = emptyList(),
    val titles: Map<Long, GridTitle> = emptyMap(),
    val clients: List<ClientHealth> = emptyList(),
    val clientViews: List<ClientView> = emptyList(),
    val outsideRest: Map<String, RestOf> = emptyMap(),
    val outsideTotal: Int = 0,
    val nextCursor: String = "",
    val downloaders: List<DownloaderInfo> = emptyList(),
)

internal object DownloadsDerive {
    fun item(
        r: DownloadRow,
        raw: DownloadsRaw,
        manager: Boolean,
        canSettings: Boolean,
        decisionsByRow: Map<Long, List<Decision>>,
    ): DownloadItem {
        val stage = DownloadRules.stageOf(r)
        val im = if (r.id > 0) raw.imports[r.id] else null
        val detail = DownloadRules.statusDetail(r, im)
        val history = DownloadRules.inHistory(r)
        val problem = DownloadRules.problemOf(r)
        var word = DownloadRules.statusWord(r)
        var note: RowNote? = DownloadRules.queueNote(r) ?: DownloadRules.stepNote(r, detail)
        var bar = DownloadRules.barTone(r)
        if (history) {
            when {
                r.clientItem -> {
                    if (stage == QueueStage.FAILED &&
                        r.text.isNotBlank()
                    ) {
                        note = RowNote(DownloadNoteKind.CLIENT_TEXT, text = r.text.trim())
                    }
                }
                stage == QueueStage.FAILED -> Unit
                DownloadRules.filesGone(im) -> {
                    word = StatusWord.TAKEN
                    bar = BarTone.HELD
                }
                r.clearWaiting != null -> {
                    word = StatusWord.IMPORTED
                    note = RowNote(DownloadNoteKind.STILL_IN, text = r.downloader)
                }
            }
            if (bar == BarTone.GO) bar = BarTone.DONE
        }
        val tone =
            when {
                problem?.tone == Tone.FAIL || r.phase == "failed" || bar == BarTone.BAD -> Tone.FAIL
                bar == BarTone.HELD -> Tone.WAIT
                bar == BarTone.QUEUED -> Tone.INFO
                bar == BarTone.DONE -> Tone.OK
                else -> Tone.INFO
            }
        val brief = raw.titles[r.titleId]
        val decisions = if (r.id < 0) raw.decisions.filter { it.slotId == -r.id } else decisionsByRow[r.id].orEmpty()
        return DownloadItem(
            key = DownloadRules.keyOf(r),
            row = r,
            stage = stage,
            name = DownloadRules.rowName(r),
            title = DownloadRules.phoneTitle(r),
            subLine = DownloadRules.subLine(r),
            word = word,
            note = note,
            problem = problem,
            detail = detail,
            bar = bar,
            tone = tone,
            progress = if (history && stage != QueueStage.FAILED) 1.0 else r.progress.coerceIn(0.0, 1.0),
            moving = DownloadRules.moving(r),
            history = history,
            poster = brief?.artwork?.firstOrNull(),
            square = r.kind == "artist",
            quality = DownloadRules.quality(r.name),
            protocol = DownloadRules.protocolOf(r),
            actions = if (manager) DownloadRules.actions(r, canSettings) else emptyList(),
            decisions = decisions,
            selectable = manager && r.id >= 0 && (r.canAct || DownloadRules.clientOk(r)),
        )
    }

    fun state(
        base: DownloadsState,
        raw: DownloadsRaw,
    ): DownloadsState {
        val filter = base.filter
        val all = DownloadRules.withDecisions(raw.rows, if (base.manager) raw.decisions else emptyList())
        val ids =
            raw.rows
                .filter { it.id > 0 }
                .map { it.id }
                .toSet()
        val decisionsByRow = raw.decisions.filter { it.downloadId in ids }.groupBy { it.downloadId }
        val searched = all.filter { DownloadRules.matches(it, filter.query) }
        val kinded = searched.filter { filter.kind == DownloadKindFilter.ALL || DownloadRules.kindOf(it) == filter.kind.wire }
        val networked = kinded.filter { filter.network == NetworkFilter.ANY || DownloadRules.protocolOf(it) == filter.network.wire }
        val tracked = networked.filter { DownloadRules.byTracker(it, filter.tracker) }
        val partial = filter.query.isNotBlank() || filter.kind != DownloadKindFilter.ALL || filter.tracker.isNotEmpty()
        val left = DownloadRules.restSum(raw.outsideRest, filter.network.wire)
        val extra = if (partial) emptyMap() else left
        val picked = DownloadRules.pick(tracked, filter.tab, filter.sub)
        val sorted = DownloadRules.sortRows(picked, filter.sort, filter.tab)
        val items = sorted.map { item(it, raw, base.manager, base.canSettings, decisionsByRow) }
        val unloaded = (filter.sub?.stages ?: filter.tab.stages).sumOf { extra[it] ?: 0 }
        val empty =
            when {
                items.isNotEmpty() -> null
                filter.query.isNotBlank() -> DownloadsEmpty(DownloadsEmptyKind.NO_MATCHES)
                filter.kind != DownloadKindFilter.ALL -> DownloadsEmpty(DownloadsEmptyKind.NO_KIND)
                filter.network != NetworkFilter.ANY -> DownloadsEmpty(DownloadsEmptyKind.NO_NETWORK)
                unloaded > 0 -> DownloadsEmpty(DownloadsEmptyKind.LOADING_MORE, unloaded)
                all.isEmpty() -> DownloadsEmpty(DownloadsEmptyKind.NOTHING)
                filter.sub != null -> DownloadsEmpty(DownloadsEmptyKind.SUB)
                else -> DownloadsEmpty(DownloadsEmptyKind.TAB)
            }
        val totals = DownloadRules.queueTotals(raw.rows)
        val views = raw.clientViews
        val free = views.sumOf { it.status?.freeSpace ?: 0 }
        val clientLeft = views.maxOfOrNull { it.status?.timeLeft ?: 0 } ?: 0
        val limited = views.any { (it.status?.speedLimit ?: 0) > 0 }
        val notes =
            raw.clients.filter { it.health != "ok" && it.health.isNotEmpty() }.map {
                ClientNote(
                    it.name,
                    when {
                        it.health == "waiting" -> ClientNoteKind.WAITING
                        it.stale -> ClientNoteKind.STALE
                        else -> ClientNoteKind.OFFLINE
                    },
                )
            }
        val selection = base.selection.filter { key -> all.any { DownloadRules.keyOf(it) == key } }.toSet()
        return base.copy(
            items = items,
            total = all.size,
            tabs = DownloadRules.tabCounts(tracked, extra),
            subs = DownloadRules.subCounts(tracked, filter.tab, extra),
            trackers = DownloadRules.trackerOptions(all),
            trackerIssues = DownloadRules.trackerIssues(all),
            empty = empty,
            clients = raw.clients,
            clientViews = views,
            notes = notes,
            statusLine =
                StatusLine(
                    totals.speed,
                    totals.upSpeed,
                    free,
                    if (totals.eta >
                        0
                    ) {
                        totals.eta
                    } else {
                        clientLeft
                    },
                    raw.decisions.size,
                    limited,
                ),
            decisions = raw.decisions,
            addTargets = raw.downloaders.filter { it.protocol == "torrent" && it.kind == "qbittorrent" && it.writable },
            moreOutside = left.values.sum(),
            selection = selection,
        )
    }
}
