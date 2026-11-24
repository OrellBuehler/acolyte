package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.ReportsEndpoints
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.model.ApproveBody
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.AskCounts
import ch.orellbuehler.acolyte.api.model.AskDecision
import ch.orellbuehler.acolyte.api.model.AskTurnDownBody
import ch.orellbuehler.acolyte.api.model.RemoveAsks
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

enum class PendingKind {
    APPROVE,
    DECLINE,
}

enum class RequestsNoteKind {
    APPROVED,
    DECLINED,
    APPROVED_MANY,
    APPROVE_FAILED,
    DECLINE_FAILED,
    ALREADY_DECIDED,
    NOTICE,
    REMOVED,
    NOTHING_TO_REMOVE,
    REMOVE_FAILED,
    TAKEN_BACK,
    TAKE_BACK_FAILED,
}

enum class ClearKind {
    FAILED,
    FINISHED,
}

data class RequestsNote(
    val id: Long,
    val kind: RequestsNoteKind,
    val name: String = "",
    val count: Int = 0,
    val by: String = "",
    val state: String = "",
    val text: String = "",
    val error: ApiError? = null,
    val keys: List<Long> = emptyList(),
) {
    val undoable: Boolean get() = keys.isNotEmpty()
}

data class RequestsState(
    val status: LoadStatus = LoadStatus(),
    val approver: Boolean = false,
    val canAsk: Boolean = false,
    val manager: Boolean = false,
    val myId: Long = 0,
    val pages: List<RequestPage> = emptyList(),
    val asks: List<Ask> = emptyList(),
    val counts: AskCounts = AskCounts(),
    val listed: AskCounts = AskCounts(),
    val total: Int = 0,
    val chosenMemberTab: MemberTab? = null,
    val approverTab: ApproverTab = ApproverTab.WAITING,
    val kind: KindFilter = KindFilter.ALL,
    val stage: Stage? = null,
    val pending: Map<Long, PendingKind> = emptyMap(),
    val limits: List<LimitLine> = emptyList(),
    val newIds: Set<Long> = emptySet(),
    val reportsOpen: Int = -1,
    val hasOwnReports: Boolean = false,
    val loadingMore: Boolean = false,
    val busy: Boolean = false,
    val note: RequestsNote? = null,
) {
    val effective: List<Ask>
        get() =
            asks.map { ask ->
                when (pending[ask.id]) {
                    PendingKind.APPROVE -> ask.copy(state = APPROVED, adding = true, problem = "")
                    PendingKind.DECLINE -> ask.copy(state = TURNED_DOWN)
                    null -> ask
                }
            }

    val kinds: List<KindFilter>
        get() {
            val present = asks.map { it.kind }.toSet()
            return KindFilter.entries.filter { it == KindFilter.ALL || it.wire in present }.takeIf { it.size > 2 } ?: emptyList()
        }

    private val byKind: List<Ask> get() = effective.filter { kind == KindFilter.ALL || it.kind == kind.wire }

    val memberCounts: Map<MemberTab, Int> get() = MemberTab.entries.associateWith { tab -> byKind.count { it.memberTab == tab } }

    val memberTab: MemberTab get() = chosenMemberTab ?: firstMemberTab(byKind)

    val memberTabs: List<MemberTab>
        get() = MemberTab.entries.filter { it != MemberTab.FAILED || (memberCounts[MemberTab.FAILED] ?: 0) > 0 }

    val approverCounts: Map<ApproverTab, Int>
        get() {
            val viewed = ApproverTab.entries.associateWith { tab -> byKind.count { it.approverTab == tab } }
            if (kind != KindFilter.ALL) return viewed
            val listed =
                mapOf(
                    ApproverTab.WAITING to listed.pending,
                    ApproverTab.PROCESSING to listed.processing,
                    ApproverTab.DONE to listed.done,
                )
            val server =
                mapOf(
                    ApproverTab.WAITING to counts.pending,
                    ApproverTab.PROCESSING to counts.processing,
                    ApproverTab.DONE to counts.done,
                )
            return ApproverTab.entries.associateWith { tab ->
                maxOf(0, (server[tab] ?: 0) + (viewed[tab] ?: 0) - (listed[tab] ?: 0))
            }
        }

    val failedCount: Int
        get() {
            val viewed = byKind.count { it.isFailed }
            if (kind != KindFilter.ALL) return viewed
            return maxOf(0, counts.failed + viewed - listed.failed)
        }

    private val tabAsks: List<Ask>
        get() = if (approver) byKind.filter { it.approverTab == approverTab } else byKind.filter { it.memberTab == memberTab }

    val stages: List<Stage> get() = if (approver) stagesIn(tabAsks).takeIf { it.size > 1 } ?: emptyList() else emptyList()

    val stageCounts: Map<Stage, Int> get() = tabAsks.groupingBy { it.life.stage }.eachCount()

    val shown: List<Ask>
        get() =
            if (approver) {
                approverSort(approverTab, tabAsks.filter { stage == null || it.life.stage == stage })
            } else {
                tabAsks
            }

    val filtersOn: Int get() = (if (stage != null) 1 else 0) + (if (kind != KindFilter.ALL) 1 else 0)

    val isCut: Boolean get() = asks.size < total

    val waitingToApprove: Int
        get() {
            val loaded = shown.count { it.isWaiting && it.id !in pending }
            if (kind != KindFilter.ALL || stage != null) return loaded
            return maxOf(loaded, counts.pending - listed.pending + effective.count { it.isWaiting && it.id !in pending })
        }

    val canApproveAll: Boolean get() = approver && approverTab == ApproverTab.WAITING && waitingToApprove > 1

    val clearable: ClearKind?
        get() =
            when {
                !approver -> null
                approverTab == ApproverTab.PROCESSING && failedCount > 0 -> ClearKind.FAILED
                approverTab == ApproverTab.DONE && (approverCounts[ApproverTab.DONE] ?: 0) > 0 -> ClearKind.FINISHED
                else -> null
            }

    val clearCount: Int
        get() =
            when (clearable) {
                ClearKind.FAILED -> if (stage == null) failedCount else shown.count { it.isFailed }
                ClearKind.FINISHED -> if (stage == null) approverCounts[ApproverTab.DONE] ?: 0 else shown.size
                null -> 0
            }

    val isEmpty: Boolean get() = status.phase == Phase.READY && asks.isEmpty()

    fun ask(id: Long): Ask? = effective.firstOrNull { it.id == id }

    fun isPending(id: Long): Boolean = id in pending
}

class RequestsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val inbox: RequestsInbox,
    scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(RequestsState())
    private val lock = Mutex()
    private val undo = UndoQueue<Long>(scope)
    private var noteId = 0L

    val state: StateFlow<RequestsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() = read(quiet = false)

    private suspend fun read(quiet: Boolean) {
        lock.withLock {
            val me = account()
            val approver = me?.can(Need.APPROVE) == true
            val canAsk = me?.can(Need.ASK) == true
            val manager = me?.can(Need.MANAGE_LIBRARY) == true
            val first = mutableState.value.status.phase != Phase.READY
            if (!quiet) mutableState.update { it.copy(status = it.status.started()) }
            coroutineScope {
                val have = mutableState.value.asks.size
                val limitAnswer = async { if (canAsk && !approver) api.result(RequestsEndpoints.limit).valueOrNull() else null }
                val reports = async { api.result(ReportsEndpoints.list, "limit" to 1).valueOrNull() }
                val newIds = if (first) inbox.markRead() else emptySet()
                val count = maxOf(PAGE, minOf(have, MAX_RELOAD))
                when (val result = api.result(RequestsEndpoints.asks, "limit" to count)) {
                    is ApiResult.Ok ->
                        mutableState.update {
                            it.copy(
                                status = LoadStatus.ready,
                                approver = approver,
                                canAsk = canAsk,
                                manager = manager,
                                myId = me?.userId ?: 0,
                                pages = me?.requestPages().orEmpty(),
                                asks = result.value.asks,
                                counts = result.value.counts,
                                listed = countsOf(result.value.asks),
                                total = result.value.total,
                                newIds = it.newIds + newIds,
                            )
                        }
                    is ApiResult.Failed ->
                        if (!quiet || first) mutableState.update { it.copy(status = it.status.failed(result.error)) }
                }
                val limit = limitAnswer.await()
                val reportList = reports.await()
                mutableState.update {
                    it.copy(
                        limits = limitLines(limit, enabledKinds(me)),
                        reportsOpen = if (manager) reportList?.open ?: 0 else -1,
                        hasOwnReports = reportList?.reports?.isNotEmpty() == true,
                    )
                }
            }
        }
    }

    suspend fun loadMore() {
        val current = mutableState.value
        if (!current.isCut || current.loadingMore) return
        mutableState.update { it.copy(loadingMore = true) }
        api
            .result(RequestsEndpoints.asks, "limit" to PAGE, "offset" to current.asks.size)
            .onSuccess { page ->
                mutableState.update { s ->
                    val known = s.asks.map { it.id }.toSet()
                    val fresh = page.asks.filter { it.id !in known }
                    s.copy(
                        asks = s.asks + fresh,
                        listed = s.listed + countsOf(fresh),
                        counts = page.counts,
                        total = page.total,
                        loadingMore = false,
                    )
                }
            }.onFailure { mutableState.update { it.copy(loadingMore = false) } }
    }

    fun setMemberTab(tab: MemberTab) = mutableState.update { it.copy(chosenMemberTab = tab) }

    fun setApproverTab(tab: ApproverTab) = mutableState.update { it.copy(approverTab = tab, stage = null) }

    fun setKind(kind: KindFilter) = mutableState.update { it.copy(kind = kind, stage = null, chosenMemberTab = null) }

    fun setStage(stage: Stage?) = mutableState.update { it.copy(stage = stage) }

    fun clearStage() = mutableState.update { it.copy(stage = null) }

    fun clearFilters() = mutableState.update { it.copy(stage = null, kind = KindFilter.ALL) }

    fun dismissNote() = mutableState.update { it.copy(note = null) }

    fun approve(
        id: Long,
        review: AskReviewPresenter? = null,
    ) {
        val ask = mutableState.value.ask(id) ?: return
        val body = review?.approveBody ?: ApproveBody()
        hold(listOf(ask), PendingKind.APPROVE)
        note(RequestsNoteKind.APPROVED, name = ask.name, keys = listOf(id))
        undo.hold(id, onUndone = { release(id) }) { sendApprove(ask, body) }
    }

    fun decline(
        id: Long,
        reason: String,
    ) {
        val ask = mutableState.value.ask(id) ?: return
        val body = AskTurnDownBody(reason.trim().take(NOTE_MAX))
        hold(listOf(ask), PendingKind.DECLINE)
        note(RequestsNoteKind.DECLINED, name = ask.name, keys = listOf(id))
        undo.hold(id, onUndone = { release(id) }) { sendDecline(ask, body) }
    }

    suspend fun approveAll() {
        val current = mutableState.value
        if (!current.canApproveAll) return
        mutableState.update { it.copy(busy = true) }
        var targets = current.shown.filter { it.isWaiting && it.id !in current.pending }
        if (current.isCut && current.kind == KindFilter.ALL && current.stage == null) {
            val all = mutableListOf<Ask>()
            var offset = 0
            while (true) {
                val page = api.result(RequestsEndpoints.asks, "state" to ASKED, "limit" to BULK, "offset" to offset).valueOrNull() ?: break
                all += page.asks
                offset += page.asks.size
                if (page.asks.size < BULK) break
            }
            if (all.isNotEmpty()) {
                mutableState.update { s ->
                    val known = s.asks.map { it.id }.toSet()
                    val fresh = all.filter { it.id !in known }
                    s.copy(asks = s.asks + fresh, listed = s.listed + countsOf(fresh))
                }
                targets = all.filter { it.id !in current.pending }
            }
        }
        mutableState.update { it.copy(busy = false) }
        if (targets.isEmpty()) return
        hold(targets, PendingKind.APPROVE)
        note(RequestsNoteKind.APPROVED_MANY, count = targets.size, keys = targets.map { it.id })
        targets.forEach { ask -> undo.hold(ask.id, onUndone = { release(ask.id) }) { sendApprove(ask, ApproveBody(), quietNotice = true) } }
    }

    fun undo(keys: List<Long>) {
        keys.forEach { undo.undo(it) }
        mutableState.update { it.copy(note = null) }
    }

    fun undoOne(id: Long) = undo(listOf(id))

    fun flush() = undo.flush()

    internal suspend fun idle() = undo.idle()

    suspend fun remove(id: Long) {
        val ask = mutableState.value.ask(id) ?: return
        mutableState.update { it.copy(busy = true) }
        val result =
            if (ask.isWaiting) {
                api.result(RequestsEndpoints.remove, RemoveAsks(ids = listOf(id)))
            } else {
                api.result(RequestsEndpoints.takeBack, "id" to id)
            }
        when (result) {
            is ApiResult.Ok -> {
                mutableState.update { s -> s.copy(asks = s.asks.filter { it.id != id }, busy = false) }
                note(RequestsNoteKind.REMOVED, count = 1)
                read(quiet = true)
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(busy = false) }
                note(RequestsNoteKind.REMOVE_FAILED, name = ask.name, error = result.error)
            }
        }
    }

    suspend fun clear(which: ClearKind) {
        val current = mutableState.value
        val filter = if (which == ClearKind.FAILED) "failed" else "finished"
        val body =
            if (current.stage == null && current.kind == KindFilter.ALL) {
                RemoveAsks(filter = filter)
            } else {
                val ids =
                    current.shown
                        .filter {
                            if (which ==
                                ClearKind.FAILED
                            ) {
                                it.isFailed
                            } else {
                                it.approverTab == ApproverTab.DONE
                            }
                        }.map { it.id }
                if (ids.isEmpty()) {
                    note(RequestsNoteKind.NOTHING_TO_REMOVE)
                    return
                }
                RemoveAsks(ids = ids, only = filter)
            }
        mutableState.update { it.copy(busy = true) }
        api
            .result(RequestsEndpoints.remove, body)
            .onSuccess { answer ->
                mutableState.update { it.copy(busy = false) }
                note(if (answer.removed > 0) RequestsNoteKind.REMOVED else RequestsNoteKind.NOTHING_TO_REMOVE, count = answer.removed)
                read(quiet = true)
            }.onFailure { error ->
                mutableState.update { it.copy(busy = false) }
                note(RequestsNoteKind.REMOVE_FAILED, error = error)
            }
    }

    suspend fun takeBack(id: Long): ApiError? {
        val ask = mutableState.value.ask(id) ?: return null
        mutableState.update { it.copy(busy = true) }
        return when (val result = api.result(RequestsEndpoints.takeBack, "id" to id)) {
            is ApiResult.Ok -> {
                mutableState.update { s -> s.copy(asks = s.asks.filter { it.id != id }, busy = false) }
                note(RequestsNoteKind.TAKEN_BACK, name = ask.name)
                read(quiet = true)
                null
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(busy = false) }
                result.error
            }
        }
    }

    suspend fun watch() {
        coroutineScope {
            launch {
                while (true) {
                    delay(POLL_MILLIS)
                    val s = mutableState.value
                    if (s.status.phase == Phase.READY && s.asks.any { it.inFlight }) read(quiet = true)
                }
            }
            api.live.collect { event ->
                when (event.topic) {
                    ASK_TOPIC -> patch(event)
                    ASK_REMOVED_TOPIC -> {
                        val id = (event.data as? JsonObject)?.get("id")?.jsonPrimitive?.longOrNull ?: return@collect
                        mutableState.update { s -> s.copy(asks = s.asks.filter { it.id != id }) }
                    }
                    LiveEvent.RESYNC -> read(quiet = true)
                }
            }
        }
    }

    private fun patch(event: LiveEvent) {
        val ask =
            try {
                ApiJson.lenient.decodeFromJsonElement(Ask.serializer(), event.data)
            } catch (e: IllegalArgumentException) {
                return
            }
        if (ask.id == 0L) return
        mutableState.update { s ->
            if (!s.approver && !ask.mine) return@update s
            val index = s.asks.indexOfFirst { it.id == ask.id }
            if (index >= 0) {
                s.copy(asks = s.asks.toMutableList().also { it[index] = ask })
            } else {
                s.copy(asks = listOf(ask) + s.asks, total = s.total + 1)
            }
        }
    }

    private fun hold(
        asks: List<Ask>,
        kind: PendingKind,
    ) = mutableState.update { s -> s.copy(pending = s.pending + asks.associate { it.id to kind }) }

    private fun release(id: Long) = mutableState.update { s -> s.copy(pending = s.pending - id) }

    private suspend fun sendApprove(
        ask: Ask,
        body: ApproveBody,
        quietNotice: Boolean = false,
    ) {
        settle(ask, api.result(RequestsEndpoints.approve, body, "id" to ask.id), RequestsNoteKind.APPROVE_FAILED, quietNotice)
    }

    private suspend fun sendDecline(
        ask: Ask,
        body: AskTurnDownBody,
    ) {
        settle(ask, api.result(RequestsEndpoints.turnDown, body, "id" to ask.id), RequestsNoteKind.DECLINE_FAILED, quietNotice = true)
    }

    private suspend fun settle(
        ask: Ask,
        result: ApiResult<AskDecision>,
        failed: RequestsNoteKind,
        quietNotice: Boolean,
    ) {
        when (result) {
            is ApiResult.Ok -> {
                replace(result.value.ask)
                release(ask.id)
                if (result.value.notice.isNotBlank() &&
                    !quietNotice
                ) {
                    note(RequestsNoteKind.NOTICE, name = ask.name, text = result.value.notice)
                }
            }
            is ApiResult.Failed -> {
                release(ask.id)
                if (askFailure(result.error) == AskFailure.ALREADY_DECIDED) {
                    val fresh = api.result(RequestsEndpoints.ask, "id" to ask.id).valueOrNull()
                    if (fresh != null) replace(fresh)
                    note(
                        RequestsNoteKind.ALREADY_DECIDED,
                        name = ask.name,
                        by = fresh?.deciderName.orEmpty(),
                        state = fresh?.state.orEmpty(),
                    )
                } else {
                    note(failed, name = ask.name, error = result.error)
                }
            }
        }
        inbox.refresh()
    }

    private fun replace(ask: Ask) {
        if (ask.id == 0L) return
        mutableState.update { s -> s.copy(asks = s.asks.map { if (it.id == ask.id) ask else it }) }
    }

    private fun note(
        kind: RequestsNoteKind,
        name: String = "",
        count: Int = 0,
        by: String = "",
        state: String = "",
        text: String = "",
        error: ApiError? = null,
        keys: List<Long> = emptyList(),
    ) {
        noteId++
        val next = RequestsNote(noteId, kind, name, count, by, state, text, error, keys)
        mutableState.update { it.copy(note = next) }
    }

    private fun enabledKinds(me: Account?): List<LimitKind> {
        me ?: return emptyList()
        return listOfNotNull(
            LimitKind.MOVIES.takeIf { me.isOn(Part.MOVIES) },
            LimitKind.SHOWS.takeIf { me.isOn(Part.SHOWS) },
            LimitKind.MUSIC.takeIf { me.isOn(Part.MUSIC) && !me.isKid },
        )
    }

    private companion object {
        const val PAGE = 200
        const val BULK = 500
        const val MAX_RELOAD = 700
        const val POLL_MILLIS = 15_000L
        const val ASK_TOPIC = "ask"
        const val ASK_REMOVED_TOPIC = "ask_removed"
    }
}

internal fun countsOf(asks: List<Ask>): AskCounts =
    AskCounts(
        pending = asks.count { it.approverTab == ApproverTab.WAITING },
        processing = asks.count { it.approverTab == ApproverTab.PROCESSING },
        done = asks.count { it.approverTab == ApproverTab.DONE },
        failed = asks.count { it.isFailed },
    )

internal operator fun AskCounts.plus(other: AskCounts): AskCounts =
    AskCounts(pending + other.pending, processing + other.processing, done + other.done, failed + other.failed)
