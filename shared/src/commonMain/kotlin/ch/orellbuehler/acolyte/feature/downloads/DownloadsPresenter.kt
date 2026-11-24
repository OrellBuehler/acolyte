package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DownloadsEndpoints
import ch.orellbuehler.acolyte.api.DownloadsLinkEndpoints
import ch.orellbuehler.acolyte.api.Endpoint
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.AddTorrentsBody
import ch.orellbuehler.acolyte.api.model.BulkBody
import ch.orellbuehler.acolyte.api.model.DecideBody
import ch.orellbuehler.acolyte.api.model.Decision
import ch.orellbuehler.acolyte.api.model.DeleteFilesBody
import ch.orellbuehler.acolyte.api.model.DlOk
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.DownloaderSettings
import ch.orellbuehler.acolyte.api.model.EditionBody
import ch.orellbuehler.acolyte.api.model.LiveInterestBody
import ch.orellbuehler.acolyte.api.model.LocationBody
import ch.orellbuehler.acolyte.api.model.MatchPickBody
import ch.orellbuehler.acolyte.api.model.PriorityBody
import ch.orellbuehler.acolyte.api.model.RemoveBody
import ch.orellbuehler.acolyte.api.model.ShareLimitsBody
import ch.orellbuehler.acolyte.api.model.SlowModeBody
import ch.orellbuehler.acolyte.api.model.SpeedLimitBody
import ch.orellbuehler.acolyte.api.model.TorrentSpeedBody
import ch.orellbuehler.acolyte.api.model.TorrentUpload
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.live.LiveStatus
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class DecideAction(
    val wire: String,
) {
    SEARCH("search"),
    TURN_DOWN("turn_down"),
    STOP("stop"),
    IMPORT_FILE("import"),
}

enum class RemoveDoneLine {
    DONE_SEEDING,
    DONE_IDLE,
    PART_SEEDING,
    PART_IDLE,
}

enum class RemoveFilesLine {
    PATH,
    ONE,
    MANY,
}

data class DownloadRemovePlan(
    val keys: List<String>,
    val names: List<String>,
    val more: Int,
    val single: String,
    val releaseName: String,
    val doneLine: RemoveDoneLine?,
    val downloader: String,
    val showDeleteFiles: Boolean,
    val showTurnDown: Boolean,
    val deleteFiles: Boolean,
    val turnDown: Boolean,
    val searchAgain: Boolean,
    val blocked: Int,
    val filesLine: RemoveFilesLine,
    val filesPath: String,
    val filesClients: String,
    val keepsSome: Boolean,
    val keepsPath: String,
)

data class SpeedTarget(
    val id: Long,
    val name: String,
    val torrent: Boolean,
    val downMb: Double,
    val upMb: Double,
    val slowMode: Boolean?,
)

class DownloadsPresenter internal constructor(
    private val api: Api,
    private val liveStatus: StateFlow<LiveStatus>,
    private val badges: DownloadBadges,
    private val account: () -> Account?,
) {
    private val links = TitleLinks(api)
    private val raw = MutableStateFlow(DownloadsRaw())
    private val mutableState = MutableStateFlow(DownloadsState())
    private val lock = Mutex()
    private var sync: DlSyncState? = null
    private var outsideLimit = OUTSIDE_PAGE
    private var noticeId = 0L
    private var removeDefaults: DownloaderSettings? = null
    private var postersTried = false

    val state: StateFlow<DownloadsState> = mutableState.asStateFlow()

    private val manager: Boolean get() = account()?.can(Need.MANAGE_LIBRARY) == true

    private val canSettings: Boolean get() = account()?.can(Need.SETTINGS) == true

    private val mediaOn: Boolean get() = account()?.isOn(Part.MEDIA) == true

    private val pushHealthy: Boolean get() = sync != null && liveStatus.value == LiveStatus.LIVE

    init {
        mutableState.update { it.copy(manager = manager, canSettings = canSettings) }
    }

    fun show(value: String) {
        val (tab, sub) = DownloadRules.showOf(value)
        setFilter { it.copy(tab = tab, sub = sub) }
    }

    fun setTab(tab: DownloadsTab) = setFilter { it.copy(tab = tab, sub = null) }

    fun setSub(sub: DownloadsSub?) = setFilter { it.copy(sub = if (it.sub == sub) null else sub) }

    fun setQuery(query: String) = setFilter { it.copy(query = query) }

    fun setKind(kind: KindFilter) = setFilter { it.copy(kind = kind) }

    fun setNetwork(network: NetworkFilter) =
        setFilter {
            it.copy(
                network = network,
                tracker =
                    if (network ==
                        NetworkFilter.USENET
                    ) {
                        ""
                    } else {
                        it.tracker
                    },
            )
        }

    fun setTracker(key: String) = setFilter { it.copy(tracker = key) }

    fun setSort(sort: SortOrder?) = setFilter { it.copy(sort = sort) }

    fun setSortName(
        key: String,
        descending: Boolean,
    ) = setSort(
        SortKey.entries
            .firstOrNull {
                it.name.replace("_", "").equals(key.replace("_", ""), ignoreCase = true)
            }?.let { SortOrder(it, descending) },
    )

    fun clearFilters() = setFilter { it.copy(kind = KindFilter.ALL, network = NetworkFilter.ANY, tracker = "") }

    fun toggleSelect(key: String) {
        val item = mutableState.value.item(key) ?: return
        if (!item.selectable) return
        mutableState.update { s -> s.copy(selection = if (key in s.selection) s.selection - key else s.selection + key) }
    }

    fun selectAll() {
        mutableState.update { s ->
            s.copy(
                selection =
                    s.items
                        .filter { it.selectable }
                        .map { it.key }
                        .toSet(),
            )
        }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selection = emptySet()) }
    }

    fun consumeNotice(id: Long) {
        mutableState.update { if (it.notice?.id == id) it.copy(notice = null) else it }
    }

    fun clearActionError() {
        mutableState.update { it.copy(actionError = null) }
    }

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        reload(quiet = false)
    }

    suspend fun showMore() {
        if (outsideLimit >= OUTSIDE_MAX) return
        outsideLimit = (outsideLimit * 2).coerceAtMost(OUTSIDE_MAX)
        mutableState.update { it.copy(loadingMore = true) }
        reload(quiet = true)
        mutableState.update { it.copy(loadingMore = false) }
    }

    suspend fun titleLink(titleId: Long): DownloadTitleLink? = links.resolve(titleId)

    private suspend fun reload(quiet: Boolean) {
        lock.withLock {
            val isManager = manager
            mutableState.update {
                it.copy(
                    manager = isManager,
                    canSettings = canSettings,
                    status = if (quiet && it.status.phase == Phase.READY) it.status else it.status.started(),
                )
            }
            when (
                val answer =
                    api.result(
                        DownloadsEndpoints.view,
                        "limit" to 100,
                        "outside_limit" to outsideLimit.takeIf { it != OUTSIDE_PAGE },
                    )
            ) {
                is ApiResult.Ok -> {
                    val view = answer.value
                    sync = DlSync.fromAnswer(view)
                    raw.update {
                        it.copy(
                            rows = view.downloads,
                            clients = view.clients,
                            outsideRest = view.outsideRest,
                            outsideTotal = view.outsideTotal,
                            nextCursor = view.nextCursor,
                        )
                    }
                    if (isManager) {
                        badges.rows(view.downloads)
                        api.result(DownloadsEndpoints.imports, "limit" to 100).onSuccess { list ->
                            raw.update { it.copy(imports = DownloadRules.newestImports(list.imports)) }
                        }
                    }
                    mutableState.update { it.copy(status = LoadStatus.ready, pushOn = sync != null) }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(answer.error)) }
            }
            publish()
        }
        if (!postersTried) {
            postersTried = true
            loadSide()
        }
        if (manager) loadMatches()
    }

    private suspend fun loadSide() {
        if (mediaOn) links.briefs()?.let { titles -> raw.update { it.copy(titles = titles) } }
        if (manager) {
            if (mediaOn) loadDecisions()
            api.result(DownloadsLinkEndpoints.downloaders).onSuccess { list -> raw.update { it.copy(downloaders = list.downloaders) } }
            if (canSettings) removeDefaults = api.result(DownloadsLinkEndpoints.downloaderSettings).valueOrNull()
            loadClients()
        }
        publish()
    }

    suspend fun loadDecisions() {
        if (!manager || !mediaOn) return
        api.result(DownloadsEndpoints.decisions).onSuccess { list ->
            raw.update { it.copy(decisions = list.items) }
            badges.decisions(list.items.size)
            publish()
        }
    }

    suspend fun loadClients() {
        if (!manager) return
        api.result(DownloadsEndpoints.clients).onSuccess { list ->
            raw.update { it.copy(clientViews = list.clients) }
            publish()
        }
    }

    suspend fun loadMatches() {
        if (!manager) return
        api.result(DownloadsEndpoints.matches).onSuccess { list ->
            mutableState.update { it.copy(matches = MatchesInfo(list.matches, list.pending, true)) }
        }
    }

    suspend fun watch() {
        coroutineScope {
            val poke = Channel<Unit>(Channel.CONFLATED)
            if (mutableState.value.status.phase == Phase.READY) poke.trySend(Unit)
            launch {
                api.live.collect { event -> onLive(event, poke) }
            }
            launch {
                for (x in poke) {
                    delay(400.milliseconds)
                    reload(quiet = true)
                }
            }
            launch {
                while (true) {
                    delay(if (pushHealthy) 30.seconds else 10.seconds)
                    reload(quiet = true)
                }
            }
            if (manager) {
                launch {
                    while (true) {
                        delay(5.seconds)
                        loadClients()
                    }
                }
                launch {
                    while (true) {
                        delay(60.seconds)
                        loadDecisions()
                    }
                }
            }
            launch {
                while (true) {
                    if (pushHealthy) api.result(DownloadsEndpoints.liveInterest, LiveInterestBody())
                    delay(30.seconds)
                }
            }
        }
    }

    private suspend fun onLive(
        event: LiveEvent,
        poke: Channel<Unit>,
    ) {
        when (event.topic) {
            "dl" -> onPatch(event)
            "download" -> {
                DlSync.liveDownload(event.data)?.let { live ->
                    raw.update { it.copy(rows = DlSync.applyDownload(it.rows, live)) }
                    publish()
                    if (manager &&
                        DlSync.failureNotice(live)
                    ) {
                        notice(NoticeKind.DOWNLOAD_FAILED, text = live.reason.take(160), tone = Tone.FAIL)
                    }
                }
                poke.trySend(Unit)
            }
            "import" -> {
                DlSync.liveImport(event.data)?.let { record ->
                    raw.update { it.copy(imports = DlSync.withImport(it.imports, record)) }
                    publish()
                }
                poke.trySend(Unit)
            }
            "slot" -> {
                poke.trySend(Unit)
                loadDecisions()
            }
            "releases" -> loadDecisions()
            LiveEvent.RESYNC -> {
                poke.trySend(Unit)
                loadDecisions()
            }
        }
    }

    private suspend fun onPatch(event: LiveEvent) {
        val held = sync ?: return
        when (val message = DlSync.parse(event.data)) {
            is DlPatch -> {
                val reread =
                    lock.withLock {
                        when (val outcome = DlSync.apply(raw.value.rows, held, message)) {
                            is DlOutcome.Applied -> {
                                sync = outcome.sync
                                raw.update { it.copy(rows = outcome.rows) }
                                publish()
                                null
                            }
                            DlOutcome.Dropped -> null
                            is DlOutcome.Reread -> outcome
                        }
                    }
                when {
                    reread == null -> Unit
                    reread.client == null -> reload(quiet = true)
                    else -> rereadClient(reread.client)
                }
            }
            is DlHealth -> {
                val before = raw.value.clients.firstOrNull { it.id == message.client }
                raw.update { r ->
                    r.copy(
                        clients =
                            r.clients.map {
                                if (it.id == message.client) {
                                    it.copy(health = message.health, stale = message.stale, seenAt = message.seenAt)
                                } else {
                                    it
                                }
                            },
                    )
                }
                publish()
                if (DlSync.wentStale(before?.stale ?: false, message)) reload(quiet = true)
            }
        }
    }

    private suspend fun rereadClient(client: Long) {
        val page = api.result(DownloadsEndpoints.view, "limit" to 100, "client" to client).valueOrNull() ?: return
        lock.withLock {
            val held = sync ?: return
            val (rows, next) = DlSync.mergeClient(raw.value.rows, held, client, page.downloads, page.revs)
            sync = next
            raw.update { it.copy(rows = rows) }
            publish()
        }
    }

    private fun setFilter(change: (DownloadsFilter) -> DownloadsFilter) {
        mutableState.update { it.copy(filter = change(it.filter)) }
        publish()
    }

    private fun publish() {
        mutableState.update { DownloadsDerive.state(it, raw.value) }
    }

    private fun notice(
        kind: NoticeKind,
        count: Int = 0,
        skipped: Int = 0,
        text: String = "",
        action: DownloadAction? = null,
        tone: Tone = Tone.OK,
        outcome: ch.orellbuehler.acolyte.api.model.AddOutcome? = null,
    ) {
        noticeId++
        mutableState.update { it.copy(notice = DownloadsNotice(noticeId, kind, count, skipped, text, action, outcome, tone)) }
    }

    private fun rowsOf(keys: Collection<String>): List<DownloadRow> {
        val all = DownloadRules.withDecisions(raw.value.rows, raw.value.decisions)
        return keys.mapNotNull { key -> all.firstOrNull { DownloadRules.keyOf(it) == key } }
    }

    private suspend fun <T> busy(
        key: String,
        block: suspend () -> ApiResult<T>,
    ): ApiResult<T> {
        mutableState.update { it.copy(busy = it.busy + key, actionError = null) }
        val result = block()
        mutableState.update { it.copy(busy = it.busy - key) }
        result.onFailure { error -> mutableState.update { it.copy(actionError = error) } }
        return result
    }

    private suspend fun own(
        endpoint: Endpoint<Unit, DlOk>,
        item: Endpoint<Unit, DlOk>,
        r: DownloadRow,
    ): ApiResult<DlOk> =
        if (r.clientItem || r.id <= 0) {
            api.result(item, "id" to r.downloaderId, "hash" to r.hash)
        } else {
            api.result(endpoint, "id" to r.id)
        }

    suspend fun act(
        key: String,
        action: DownloadAction,
    ) {
        val r = rowsOf(listOf(key)).firstOrNull() ?: return
        if (action !in SINGLE) return
        val result =
            busy(key) {
                when (action) {
                    DownloadAction.PAUSE -> own(DownloadsEndpoints.pause, DownloadsEndpoints.itemPause, r)
                    DownloadAction.RESUME -> own(DownloadsEndpoints.resume, DownloadsEndpoints.itemResume, r)
                    DownloadAction.RECHECK -> own(DownloadsEndpoints.recheck, DownloadsEndpoints.itemRecheck, r)
                    DownloadAction.REANNOUNCE -> own(DownloadsEndpoints.reannounce, DownloadsEndpoints.itemReannounce, r)
                    DownloadAction.RETRY -> api.result(DownloadsEndpoints.itemRetry, "id" to r.downloaderId, "hash" to r.hash)
                    DownloadAction.IMPORT_AGAIN -> api.result(DownloadsEndpoints.importAgain, "id" to r.id)
                    DownloadAction.MARK_FAILED ->
                        when (val answer = api.result(TitleSupportEndpoints.markFailed, "id" to r.id)) {
                            is ApiResult.Ok -> ApiResult.Ok(DlOk(ok = true))
                            is ApiResult.Failed -> answer
                        }
                    DownloadAction.MOVE_TOP, DownloadAction.MOVE_BOTTOM, DownloadAction.MOVE_UP, DownloadAction.MOVE_DOWN -> move(r, action)
                    else -> api.result(DownloadsEndpoints.bulk, BulkBody("force", listOf(r.hash)), "id" to r.downloaderId)
                }
            }
        result.onSuccess {
            when (action) {
                DownloadAction.PAUSE -> notice(NoticeKind.PAUSED)
                DownloadAction.RESUME -> notice(NoticeKind.STARTED)
                DownloadAction.RECHECK -> notice(NoticeKind.CHECKING)
                DownloadAction.REANNOUNCE -> notice(NoticeKind.ANNOUNCED)
                DownloadAction.RETRY -> notice(NoticeKind.RETRIED, count = 1)
                DownloadAction.IMPORT_AGAIN -> notice(NoticeKind.IMPORT_QUEUED)
                DownloadAction.MARK_FAILED -> notice(NoticeKind.MARKED_FAILED)
                DownloadAction.MOVE_TOP -> notice(NoticeKind.MOVED_TOP)
                DownloadAction.MOVE_BOTTOM -> notice(NoticeKind.MOVED_BOTTOM)
                DownloadAction.MOVE_UP -> notice(NoticeKind.MOVED_UP)
                DownloadAction.MOVE_DOWN -> notice(NoticeKind.MOVED_DOWN)
                DownloadAction.FORCE_START -> notice(NoticeKind.FORCE_STARTED, count = 1)
                else -> Unit
            }
            reload(quiet = true)
        }
    }

    private suspend fun move(
        r: DownloadRow,
        action: DownloadAction,
    ): ApiResult<DlOk> {
        val word =
            when (action) {
                DownloadAction.MOVE_TOP -> "top"
                DownloadAction.MOVE_BOTTOM -> "bottom"
                DownloadAction.MOVE_UP -> "up"
                else -> "down"
            }
        if (DownloadRules.qbitTorrent(r)) return api.result(DownloadsEndpoints.bulk, BulkBody(word, listOf(r.hash)), "id" to r.downloaderId)
        val endpoint =
            when (action) {
                DownloadAction.MOVE_TOP -> DownloadsEndpoints.itemTop
                DownloadAction.MOVE_BOTTOM -> DownloadsEndpoints.itemBottom
                DownloadAction.MOVE_UP -> DownloadsEndpoints.itemUp
                else -> DownloadsEndpoints.itemDown
            }
        return api.result(endpoint, "id" to r.downloaderId, "hash" to r.hash)
    }

    suspend fun setPriority(
        key: String,
        priority: UsenetPriority,
    ) {
        val r = rowsOf(listOf(key)).firstOrNull() ?: return
        busy(key) { api.result(DownloadsEndpoints.itemPriority, PriorityBody(priority.wire), "id" to r.downloaderId, "hash" to r.hash) }
            .onSuccess {
                notice(NoticeKind.PRIORITY_SET, text = priority.wire)
                reload(quiet = true)
            }
    }

    fun selectionActions(): List<DownloadAction> {
        val rows = rowsOf(mutableState.value.selection)
        if (rows.isEmpty()) return emptyList()
        val settings = canSettings
        val out = mutableListOf<DownloadAction>()
        val client = rows.filter { DownloadRules.clientOk(it) }
        if (client.any { DownloadRules.pausable(it) && it.state != "paused" }) out += DownloadAction.PAUSE
        if (client.any { DownloadRules.pausable(it) && it.state == "paused" }) out += DownloadAction.RESUME
        val torrents = client.filter { DownloadRules.protocolOf(it) == "torrent" && it.hash.isNotEmpty() && it.downloaderId > 0 }
        if (torrents.any { DownloadRules.controllable(it.downloaderKind) }) out += listOf(DownloadAction.RECHECK, DownloadAction.REANNOUNCE)
        val qbit = torrents.filter { DownloadRules.qbitTorrent(it) }
        if (qbit.isNotEmpty()) {
            out +=
                listOf(DownloadAction.MOVE_TOP, DownloadAction.MOVE_UP, DownloadAction.MOVE_DOWN, DownloadAction.MOVE_BOTTOM)
        }
        val steer = qbit.filter { DownloadRules.mayEdit(it, settings) }
        if (steer.isNotEmpty()) {
            out +=
                listOf(DownloadAction.FORCE_START, DownloadAction.SEEDING_RULES, DownloadAction.SET_LOCATION, DownloadAction.SPEED_LIMITS)
        }
        if (rows.any { DownloadRules.retryable(it) && DownloadRules.mayEdit(it, settings) }) out += DownloadAction.RETRY
        if (rows.any { it.canAct }) out += DownloadAction.REMOVE
        return out
    }

    suspend fun bulk(action: DownloadAction) {
        val rows = rowsOf(mutableState.value.selection)
        if (rows.isEmpty() || mutableState.value.batchBusy) return
        mutableState.update { it.copy(batchBusy = true, actionError = null) }
        var done = 0
        var error: ApiError? = null
        val client = rows.filter { DownloadRules.clientOk(it) }

        suspend fun count(result: ApiResult<DlOk>) {
            when (result) {
                is ApiResult.Ok -> done += if (result.value.count > 0) result.value.count else 1
                is ApiResult.Failed -> if (error == null) error = result.error
            }
        }
        val targets: List<DownloadRow> =
            when (action) {
                DownloadAction.PAUSE, DownloadAction.RESUME -> {
                    val want = action == DownloadAction.RESUME
                    val list = client.filter { DownloadRules.pausable(it) && (it.state == "paused") == want }
                    val (torrents, others) =
                        list.partition {
                            DownloadRules.protocolOf(it) == "torrent" &&
                                it.hash.isNotEmpty() &&
                                it.downloaderId > 0
                        }
                    for ((id, group) in torrents.groupBy { it.downloaderId }) {
                        count(
                            api.result(
                                DownloadsEndpoints.bulk,
                                BulkBody(if (want) "start" else "pause", group.map { it.hash }),
                                "id" to id,
                            ),
                        )
                    }
                    for (r in others) {
                        count(
                            if (want) {
                                own(DownloadsEndpoints.resume, DownloadsEndpoints.itemResume, r)
                            } else {
                                own(DownloadsEndpoints.pause, DownloadsEndpoints.itemPause, r)
                            },
                        )
                    }
                    list
                }
                DownloadAction.RETRY -> {
                    val list = rows.filter { DownloadRules.retryable(it) && DownloadRules.mayEdit(it, canSettings) }
                    for (r in list) count(api.result(DownloadsEndpoints.itemRetry, "id" to r.downloaderId, "hash" to r.hash))
                    list
                }
                else -> {
                    val word =
                        when (action) {
                            DownloadAction.RECHECK -> "recheck"
                            DownloadAction.REANNOUNCE -> "reannounce"
                            DownloadAction.MOVE_TOP -> "top"
                            DownloadAction.MOVE_BOTTOM -> "bottom"
                            DownloadAction.MOVE_UP -> "up"
                            DownloadAction.MOVE_DOWN -> "down"
                            DownloadAction.FORCE_START -> "force"
                            else -> {
                                mutableState.update { it.copy(batchBusy = false) }
                                return
                            }
                        }
                    val list =
                        client.filter {
                            DownloadRules.protocolOf(it) == "torrent" &&
                                it.hash.isNotEmpty() &&
                                it.downloaderId > 0 &&
                                when (action) {
                                    DownloadAction.RECHECK, DownloadAction.REANNOUNCE -> DownloadRules.controllable(it.downloaderKind)
                                    DownloadAction.FORCE_START -> DownloadRules.qbitTorrent(it) && DownloadRules.mayEdit(it, canSettings)
                                    else -> DownloadRules.qbitTorrent(it)
                                }
                        }
                    for ((id, group) in list.groupBy { it.downloaderId }) {
                        count(
                            api.result(
                                DownloadsEndpoints.bulk,
                                BulkBody(word, group.map { it.hash }),
                                "id" to id,
                            ),
                        )
                    }
                    list
                }
            }
        mutableState.update { it.copy(batchBusy = false, actionError = error) }
        if (targets.isEmpty()) {
            notice(NoticeKind.NOT_HERE, tone = Tone.WAIT)
        } else if (done > 0) {
            notice(NoticeKind.BULK_DONE, count = done, skipped = rows.size - targets.size, action = action)
            clearSelection()
        }
        reload(quiet = true)
    }

    suspend fun startAll() = everything(DownloadsEndpoints.resumeAll, NoticeKind.ALL_STARTED)

    suspend fun pauseAll() = everything(DownloadsEndpoints.pauseAll, NoticeKind.ALL_PAUSED)

    private suspend fun everything(
        endpoint: Endpoint<Unit, ch.orellbuehler.acolyte.api.model.ClientViews>,
        kind: NoticeKind,
    ) {
        busy(BATCH) { api.result(endpoint) }.onSuccess { answer ->
            val failed = answer.clients.filter { it.controls && it.error.isNotEmpty() }
            if (failed.isEmpty()) {
                notice(kind)
            } else {
                notice(
                    NoticeKind.ALL_FAILED,
                    count = failed.size,
                    text = failed.first().name,
                    action =
                        if (kind ==
                            NoticeKind.ALL_STARTED
                        ) {
                            DownloadAction.RESUME
                        } else {
                            DownloadAction.PAUSE
                        },
                    tone = Tone.FAIL,
                )
            }
            reload(quiet = true)
            loadClients()
        }
    }

    fun removePlan(keys: List<String>): DownloadRemovePlan? {
        val all = rowsOf(keys).filter { it.canAct }
        if (all.isEmpty()) return null
        val settings = canSettings
        val rows = if (settings) all else all.filter { !it.outside }
        val single = all.size == 1
        val first = all.first()
        val singleOwn = single && !first.clientItem && first.id > 0
        val finishedOwn = { r: DownloadRow -> !r.clientItem && (r.state == "finished" || r.state == "seeding") }
        val doneInLibrary = singleOwn && finishedOwn(first) && first.phase == "done"
        val doneLine =
            if (doneInLibrary) {
                val seeding = first.protocol != "usenet" && (first.seeding || first.state == "seeding")
                val part = first.episodes.size > 1 && first.episodes.any { it.state != "in_library" && it.state != "better_wanted" }
                when {
                    part && seeding -> RemoveDoneLine.PART_SEEDING
                    part -> RemoveDoneLine.PART_IDLE
                    seeding -> RemoveDoneLine.DONE_SEEDING
                    else -> RemoveDoneLine.DONE_IDLE
                }
            } else {
                null
            }
        val defaults =
            when {
                !singleOwn -> Triple(false, false, false)
                settings ->
                    removeDefaults?.let { Triple(it.removeDeleteFiles, it.removeTurnDown, it.removeSearchAgain) }
                        ?: Triple(true, false, true)
                else -> Triple(true, false, true)
            }
        val allGone = rows.isNotEmpty() && rows.all { it.downloaderGone }
        val deleteFiles = !allGone && !(singleOwn && finishedOwn(first)) && defaults.first
        val showTurnDown = singleOwn && !finishedOwn(first) && first.phase != "done"
        val deleting = rows.filter { !it.filesStay }
        val stay = rows.size - deleting.size
        val clients =
            deleting
                .map { it.downloader }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
        val filesLine =
            when {
                deleting.size == 1 && deleting[0].path.isNotEmpty() -> RemoveFilesLine.PATH
                deleting.size <= 1 -> RemoveFilesLine.ONE
                else -> RemoveFilesLine.MANY
            }
        return DownloadRemovePlan(
            keys = rows.map { DownloadRules.keyOf(it) },
            names = rows.take(6).map { DownloadRules.rowName(it) },
            more = (rows.size - 6).coerceAtLeast(0),
            single = if (single) DownloadRules.rowName(first) else "",
            releaseName = if (single) first.name else "",
            doneLine = doneLine,
            downloader = first.downloader,
            showDeleteFiles = !allGone,
            showTurnDown = showTurnDown,
            deleteFiles = deleteFiles,
            turnDown = showTurnDown && defaults.second,
            searchAgain = defaults.third,
            blocked = all.size - rows.size,
            filesLine = filesLine,
            filesPath = deleting.firstOrNull()?.path.orEmpty(),
            filesClients = clients,
            keepsSome = stay > 0 && !(stay == 1 && rows.size == 1),
            keepsPath = if (stay == 1 && rows.size == 1) rows[0].path else "",
        )
    }

    suspend fun remove(
        plan: DownloadRemovePlan,
        deleteFiles: Boolean,
        turnDown: Boolean,
        searchAgain: Boolean,
    ): List<String> {
        val rows = rowsOf(plan.keys)
        if (rows.isEmpty()) return emptyList()
        mutableState.update { it.copy(batchBusy = true, actionError = null) }
        val failed = mutableListOf<String>()
        var error: ApiError? = null
        var note = ""
        var removed = 0
        val single = rows.size == 1
        for (r in rows.filter { !it.clientItem && it.id > 0 }) {
            val body =
                if (single && plan.showTurnDown) {
                    RemoveBody(deleteFiles, turnDown, turnDown && searchAgain)
                } else {
                    RemoveBody(deleteFiles && !r.downloaderGone, false, false)
                }
            when (val result = api.result(DownloadsEndpoints.remove, body, "id" to r.id)) {
                is ApiResult.Ok -> {
                    removed++
                    if (result.value.clientNote.isNotEmpty()) note = result.value.clientNote
                }
                is ApiResult.Failed -> {
                    failed += DownloadRules.keyOf(r)
                    if (error == null) error = result.error
                }
            }
        }
        val clientItems = rows.filter { it.clientItem || it.id <= 0 }
        val (torrents, usenet) = clientItems.partition { DownloadRules.protocolOf(it) != "usenet" }
        for ((id, group) in torrents.groupBy { it.downloaderId }) {
            when (val result = api.result(DownloadsEndpoints.bulk, BulkBody("remove", group.map { it.hash }, deleteFiles), "id" to id)) {
                is ApiResult.Ok -> removed += group.size
                is ApiResult.Failed -> {
                    failed += group.map { DownloadRules.keyOf(it) }
                    if (error == null) error = result.error
                }
            }
        }
        for (r in usenet) {
            when (
                val result =
                    api.result(
                        DownloadsEndpoints.itemRemove,
                        DeleteFilesBody(deleteFiles),
                        "id" to r.downloaderId,
                        "hash" to r.hash,
                    )
            ) {
                is ApiResult.Ok -> removed++
                is ApiResult.Failed -> {
                    failed += DownloadRules.keyOf(r)
                    if (error == null) error = result.error
                }
            }
        }
        mutableState.update { it.copy(batchBusy = false, actionError = error, selection = it.selection - plan.keys.toSet()) }
        if (removed > 0) {
            if (note.isNotEmpty()) {
                notice(
                    NoticeKind.REMOVED_KEPT,
                    text = note,
                    tone = Tone.WAIT,
                )
            } else {
                notice(NoticeKind.REMOVED, count = removed)
            }
        }
        reload(quiet = true)
        return failed
    }

    fun speedTargets(): List<SpeedTarget> =
        raw.value.clientViews.filter { it.controls && it.status != null }.map {
            val s = it.status!!
            SpeedTarget(it.id, it.name, it.protocol == "torrent", s.speedLimit / 1_000_000.0, s.upLimit / 1_000_000.0, s.slowMode)
        }

    suspend fun saveSpeedLimits(targets: List<SpeedTarget>) {
        val before = speedTargets().associateBy { it.id }
        var error: ApiError? = null
        mutableState.update { it.copy(batchBusy = true, actionError = null) }
        for (t in targets) {
            val was = before[t.id] ?: continue
            if (was.downMb == t.downMb && was.upMb == t.upMb) continue
            val body =
                SpeedLimitBody(
                    (t.downMb.coerceAtLeast(0.0) * 1_000_000).roundToLong(),
                    (t.upMb.coerceAtLeast(0.0) * 1_000_000).roundToLong(),
                )
            api.result(DownloadsEndpoints.speedLimit, body, "id" to t.id).onFailure { if (error == null) error = it }
        }
        mutableState.update { it.copy(batchBusy = false, actionError = error) }
        if (error == null) notice(NoticeKind.SPEED_SAVED)
        loadClients()
    }

    suspend fun setSlowMode(
        clientId: Long,
        on: Boolean,
    ) {
        busy("slow-$clientId") { api.result(DownloadsEndpoints.slowMode, SlowModeBody(on), "id" to clientId) }
        loadClients()
    }

    private fun steerRows(keys: Collection<String>): List<DownloadRow> =
        rowsOf(keys).filter { DownloadRules.qbitTorrent(it) && DownloadRules.mayEdit(it, canSettings) }

    suspend fun setShareLimits(
        keys: List<String>,
        ratio: Double,
        seedingMinutes: Long,
        inactiveMinutes: Long,
        action: String,
    ): Boolean {
        val rows = steerRows(keys)
        if (DownloadRules.limitsProblem(ratio, seedingMinutes, inactiveMinutes)) return false
        return steer(rows, NoticeKind.SEEDING_SET) { id, hashes ->
            api.result(DownloadsEndpoints.shareLimits, ShareLimitsBody(hashes, ratio, seedingMinutes, inactiveMinutes, action), "id" to id)
        }
    }

    suspend fun setLocation(
        keys: List<String>,
        category: String,
        folder: String,
    ): Boolean {
        if (folder.isBlank()) return false
        return steer(steerRows(keys), NoticeKind.MOVING) { id, hashes ->
            api.result(DownloadsEndpoints.location, LocationBody(hashes, folder.trim(), category.trim().ifEmpty { null }), "id" to id)
        }
    }

    suspend fun setTorrentSpeeds(
        keys: List<String>,
        downKb: Double,
        upKb: Double,
    ): Boolean =
        steer(steerRows(keys), NoticeKind.SPEEDS_SET) { id, hashes ->
            api.result(
                DownloadsEndpoints.torrentSpeeds,
                TorrentSpeedBody(hashes, (downKb.coerceAtLeast(0.0) * 1000).roundToLong(), (upKb.coerceAtLeast(0.0) * 1000).roundToLong()),
                "id" to id,
            )
        }

    private suspend fun steer(
        rows: List<DownloadRow>,
        kind: NoticeKind,
        call: suspend (Long, List<String>) -> ApiResult<DlOk>,
    ): Boolean {
        if (rows.isEmpty()) return false
        mutableState.update { it.copy(batchBusy = true, actionError = null) }
        var error: ApiError? = null
        for ((id, group) in rows.groupBy { it.downloaderId }) call(id, group.map { it.hash }).onFailure { if (error == null) error = it }
        mutableState.update { it.copy(batchBusy = false, actionError = error) }
        if (error == null) notice(kind, count = rows.size)
        reload(quiet = true)
        return error == null
    }

    fun steerStart(keys: List<String>): ch.orellbuehler.acolyte.api.model.ShareLimits =
        steerRows(keys).firstNotNullOfOrNull { it.limits } ?: ch.orellbuehler.acolyte.api.model
            .ShareLimits()

    suspend fun addTorrents(
        downloaderId: Long,
        text: String,
        category: String,
        savePath: String,
        paused: Boolean,
    ): List<String> = addTorrentFiles(downloaderId, text, emptyList(), category, savePath, paused)

    suspend fun addTorrentFiles(
        downloaderId: Long,
        text: String,
        files: List<TorrentUpload>,
        category: String,
        savePath: String,
        paused: Boolean,
    ): List<String> {
        val (links, bad) = DownloadRules.splitLinks(text)
        if (bad.isNotEmpty() || (links.isEmpty() && files.isEmpty())) return bad.ifEmpty { listOf("") }
        val torrents = files.filter { it.name.endsWith(".torrent", ignoreCase = true) && it.bytes.isNotEmpty() }.take(MAX_FILES)
        busy(BATCH) {
            api.result(
                DownloadsEndpoints.addTorrents,
                AddTorrentsBody(links, category.trim(), savePath.trim(), paused, torrents),
                "id" to downloaderId,
            )
        }.onSuccess { outcome ->
            val tone =
                when {
                    outcome.failed > 0 || outcome.bad.isNotEmpty() ->
                        if (outcome.added > 0 ||
                            outcome.pending > 0
                        ) {
                            Tone.INFO
                        } else {
                            Tone.FAIL
                        }
                    outcome.added > 0 || outcome.pending > 0 -> Tone.OK
                    else -> Tone.INFO
                }
            notice(NoticeKind.ADDED, outcome = outcome, tone = tone)
            reload(quiet = true)
        }
        return emptyList()
    }

    suspend fun decide(
        decision: Decision,
        action: DecideAction,
        pack: Boolean,
    ) {
        val key = if (pack) "pack-${decision.downloadId}" else "slot-${decision.slotId}"
        val body = DecideBody(action.wire, pack, action == DecideAction.IMPORT_FILE && decision.hasCopy)
        busy(key) { api.result(DownloadsEndpoints.decide, body, "id" to decision.slotId) }.onSuccess {
            when (action) {
                DecideAction.SEARCH -> notice(NoticeKind.SEARCHING, text = DownloadRules.decisionName(decision))
                DecideAction.TURN_DOWN -> notice(NoticeKind.TURNED_DOWN, text = DownloadRules.decisionName(decision))
                DecideAction.STOP -> notice(NoticeKind.STOPPED, text = DownloadRules.decisionName(decision))
                DecideAction.IMPORT_FILE -> notice(NoticeKind.IMPORTED, text = DownloadRules.decisionName(decision))
            }
            loadDecisions()
            reload(quiet = true)
        }
    }

    suspend fun importAgain(decision: Decision) {
        if (decision.downloadId <= 0) return
        busy("slot-${decision.slotId}") { api.result(DownloadsEndpoints.importAgain, "id" to decision.downloadId) }.onSuccess {
            notice(NoticeKind.IMPORT_QUEUED)
            loadDecisions()
            reload(quiet = true)
        }
    }

    suspend fun setEdition(
        decision: Decision,
        edition: String,
    ): Boolean {
        val value = edition.trim().take(80)
        if (value.isEmpty() || decision.downloadId <= 0) return false
        val result =
            busy("slot-${decision.slotId}") {
                api.result(
                    DownloadsEndpoints.edition,
                    EditionBody(value),
                    "id" to decision.downloadId,
                )
            }
        result.onSuccess {
            notice(NoticeKind.EDITION_SET)
            loadDecisions()
        }
        return result is ApiResult.Ok
    }

    suspend fun pickMatch(
        matchId: Long,
        candidate: String?,
        notMedia: Boolean,
    ) {
        val body =
            when {
                candidate != null -> MatchPickBody(candidate = candidate)
                notMedia -> MatchPickBody(notMedia = true)
                else -> MatchPickBody(skip = true)
            }
        busy("match-$matchId") { api.result(DownloadsEndpoints.pickMatch, body, "id" to matchId) }.onSuccess {
            if (candidate != null) notice(NoticeKind.MATCHED)
            loadMatches()
            reload(quiet = true)
        }
    }

    suspend fun runMatches() {
        busy(BATCH) { api.result(DownloadsEndpoints.runMatches) }.onSuccess {
            notice(NoticeKind.MATCHING, tone = Tone.INFO)
            delay(4.seconds)
            loadMatches()
        }
    }

    private companion object {
        const val OUTSIDE_PAGE = 200
        const val OUTSIDE_MAX = 5000
        const val BATCH = "batch"
        const val MAX_FILES = 500
        val SINGLE =
            setOf(
                DownloadAction.PAUSE,
                DownloadAction.RESUME,
                DownloadAction.RECHECK,
                DownloadAction.REANNOUNCE,
                DownloadAction.RETRY,
                DownloadAction.IMPORT_AGAIN,
                DownloadAction.MARK_FAILED,
                DownloadAction.MOVE_TOP,
                DownloadAction.MOVE_BOTTOM,
                DownloadAction.MOVE_UP,
                DownloadAction.MOVE_DOWN,
                DownloadAction.FORCE_START,
            )
    }
}
