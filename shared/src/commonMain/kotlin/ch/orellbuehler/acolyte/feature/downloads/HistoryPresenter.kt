package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DownloadsEndpoints
import ch.orellbuehler.acolyte.api.DownloadsLinkEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.EventRecord
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.SourceRef
import ch.orellbuehler.acolyte.api.model.TitleBrief
import ch.orellbuehler.acolyte.api.model.TurnDownBody
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveEvent
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

data class HistoryFilter(
    val steps: Set<HistoryStep> = emptySet(),
    val type: HistoryType = HistoryType.ALL,
    val who: HistoryWhoFilter = HistoryWhoFilter.ALL,
    val period: HistoryWhen = HistoryWhen.ALL,
    val from: String = "",
    val to: String = "",
    val source: Long = 0,
) {
    val count: Int
        get() =
            steps.size +
                listOf(type != HistoryType.ALL, who != HistoryWhoFilter.ALL, period != HistoryWhen.ALL, source > 0).count { it }

    val badRange: Boolean get() = period == HistoryWhen.CUSTOM && from.isNotEmpty() && to.isNotEmpty() && from > to
}

enum class HistoryNotice {
    REPLACING,
    NO_RELEASE,
}

data class HistoryState(
    val status: LoadStatus = LoadStatus(),
    val titleId: Long = 0,
    val episodeId: Long = 0,
    val scopeName: String = "",
    val filter: HistoryFilter = HistoryFilter(),
    val query: String = "",
    val days: List<HistoryDay> = emptyList(),
    val shown: Int = 0,
    val total: Int = 0,
    val loaded: Int = 0,
    val more: Boolean = false,
    val loadingMore: Boolean = false,
    val sources: List<SourceRef> = emptyList(),
    val canSources: Boolean = false,
    val busy: Set<String> = emptySet(),
    val notice: HistoryNotice? = null,
    val noticeId: Long = 0,
    val actionError: ApiError? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && days.isEmpty()

    val filtered: Boolean get() = filter.count > 0 || query.isNotBlank()

    fun item(key: String): HistoryItem? = days.firstNotNullOfOrNull { d -> d.items.firstOrNull { it.key == key } }
}

class HistoryPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    titleId: Long,
    episodeId: Long,
    private val clock: Clock = Clock.System,
    private val offset: (Long) -> Int = ::localOffsetSeconds,
) {
    private val links = TitleLinks(api)
    private val mutableState = MutableStateFlow(HistoryState(titleId = titleId, episodeId = episodeId))
    private val lock = Mutex()
    private var events: List<EventRecord> = emptyList()
    private var titles: Map<Long, TitleBrief> = emptyMap()
    private var users: Map<Long, String> = emptyMap()
    private var sideLoaded = false
    private var pages = 1
    private var pressedMore = false
    private var noticeId = 0L

    val state: StateFlow<HistoryState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() = reload(quiet = false)

    fun setQuery(query: String) {
        mutableState.update { it.copy(query = query) }
        publish()
    }

    suspend fun setFilter(filter: HistoryFilter) {
        mutableState.update { it.copy(filter = filter) }
        pages = 1
        pressedMore = false
        if (!filter.badRange) reload(quiet = false) else publish()
    }

    suspend fun clearScope() {
        mutableState.update { it.copy(titleId = 0, episodeId = 0, scopeName = "") }
        pages = 1
        reload(quiet = false)
    }

    suspend fun showMore() {
        val s = mutableState.value
        if (!s.more || s.loadingMore) return
        pressedMore = true
        mutableState.update { it.copy(loadingMore = true) }
        lock.withLock {
            fetch(offset = events.size).onSuccess { page ->
                events = events + page.events
                pages++
                mutableState.update { it.copy(total = page.total) }
            }
        }
        mutableState.update { it.copy(loadingMore = false) }
        publish()
    }

    suspend fun titleLink(titleId: Long): TitleLink? = links.resolve(titleId)

    fun consumeNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    suspend fun findAnother(key: String) {
        val item = mutableState.value.item(key) ?: return
        val release = item.releaseId
        if (release <= 0) {
            noticeId++
            mutableState.update { it.copy(notice = HistoryNotice.NO_RELEASE, noticeId = noticeId) }
            return
        }
        val slot = item.events.firstOrNull { it.slotId > 0 }?.slotId
        mutableState.update { it.copy(busy = it.busy + key, actionError = null) }
        val turned = api.result(DownloadsLinkEndpoints.turnDown, TurnDownBody(slot), "id" to release)
        val result =
            if (turned is ApiResult.Ok) {
                api.result(DownloadsLinkEndpoints.search, SearchBody(slotId = slot), "id" to item.head.titleId)
            } else {
                turned
            }
        mutableState.update { it.copy(busy = it.busy - key) }
        result
            .onSuccess {
                noticeId++
                mutableState.update { it.copy(notice = HistoryNotice.REPLACING, noticeId = noticeId) }
            }.onFailure { error -> mutableState.update { it.copy(actionError = error) } }
    }

    suspend fun watch() {
        coroutineScope {
            val poke = Channel<Unit>(Channel.CONFLATED)
            launch {
                api.live.collect { event ->
                    if (event.topic == "event" || event.topic == LiveEvent.RESYNC) poke.trySend(Unit)
                }
            }
            launch {
                for (x in poke) {
                    delay(2.seconds)
                    if (!pressedMore) reload(quiet = true)
                }
            }
        }
    }

    private suspend fun fetch(offset: Int): ApiResult<ch.orellbuehler.acolyte.api.model.EventsPage> {
        val s = mutableState.value
        val f = s.filter
        val now = clock.now()
        val today = Days.today(now, offset = this.offset)
        val from =
            when (f.period) {
                HistoryWhen.ALL -> null
                HistoryWhen.DAY -> (now - 24.hours).toString()
                HistoryWhen.WEEK -> (now - 7.days).toString()
                HistoryWhen.MONTH -> today.plusDays(-30).key
                HistoryWhen.CUSTOM -> f.from.ifEmpty { null }
            }
        val to = if (f.period == HistoryWhen.CUSTOM) f.to.ifEmpty { null } else null
        val titleFromQuery =
            if (s.titleId == 0L && s.episodeId == 0L && s.query.isNotBlank()) {
                titles.values
                    .filter { it.name.equals(s.query.trim(), ignoreCase = true) }
                    .singleOrNull()
                    ?.id
            } else {
                null
            }
        return api.result(
            DownloadsEndpoints.events,
            "episode" to s.episodeId.takeIf { it > 0 },
            "title" to (s.titleId.takeIf { it > 0 && s.episodeId == 0L } ?: titleFromQuery),
            "kind" to HistoryRules.kinds(f.steps).joinToString(","),
            "from" to from,
            "to" to to,
            "source" to f.source.takeIf { it > 0 },
            "feed" to 0,
            "limit" to PAGE,
            "offset" to offset.takeIf { it > 0 },
        )
    }

    private suspend fun reload(quiet: Boolean) {
        if (!sideLoaded) loadSide()
        lock.withLock {
            mutableState.update { if (quiet && it.status.phase == Phase.READY) it else it.copy(status = it.status.started()) }
            val collected = mutableListOf<EventRecord>()
            var total = 0
            var failed: ApiError? = null
            var episodeName = ""
            for (page in 0 until pages) {
                when (val answer = fetch(collected.size)) {
                    is ApiResult.Ok -> {
                        collected += answer.value.events
                        total = answer.value.total
                        episodeName = answer.value.episode
                        if (answer.value.events.size < PAGE) break
                    }
                    is ApiResult.Failed -> {
                        failed = answer.error
                        break
                    }
                }
            }
            val error = failed
            if (error != null) {
                mutableState.update { it.copy(status = it.status.failed(error)) }
            } else {
                events = collected
                val scope =
                    when {
                        mutableState.value.episodeId > 0 -> episodeName
                        mutableState.value.titleId > 0 ->
                            titles[mutableState.value.titleId]?.name
                                ?: collected.firstOrNull()?.titleName.orEmpty()
                        else -> ""
                    }
                mutableState.update { it.copy(status = LoadStatus.ready, total = total, scopeName = scope) }
            }
        }
        publish()
    }

    private suspend fun loadSide() {
        sideLoaded = true
        val who = account()
        if (who?.isOn(Part.MEDIA) == true) links.briefs()?.let { titles = it }
        if (who?.can(Need.USERS) == true) {
            api.result(DownloadsLinkEndpoints.users).valueOrNull()?.let { list -> users = list.users.associate { it.id to it.name } }
        }
        if (who?.can(Need.SETTINGS) == true) {
            api.result(DownloadsLinkEndpoints.sources).valueOrNull()?.let { list ->
                mutableState.update { it.copy(sources = list.sources, canSources = true) }
            }
        }
    }

    private fun publish() {
        val s = mutableState.value
        val me = account()?.userId ?: 0
        val visible =
            HistoryRules
                .dropHealthyFolders(HistoryRules.dropDuplicateAsks(events))
                .filter { HistoryRules.textMatches(it, s.query, users) }
                .filter { HistoryRules.whoMatches(it, s.filter.who, me) }
                .filter { s.filter.type == HistoryType.ALL || HistoryRules.typeMatches(titles[it.titleId]?.kind, s.filter.type) }
        val items =
            HistoryRules.group(visible).map { (key, list) ->
                val steps = if (key.startsWith("d")) HistoryRules.steps(list) else emptyList()
                val head = list.first()
                val verb = HistoryRules.rowVerb(list, steps)
                val failingStep = steps.any { it.state == StepState.FAILED }
                val brief = titles[head.titleId]
                val line =
                    if (steps.isNotEmpty()) {
                        val worst =
                            list.firstOrNull { HistoryRules.verbOf(it.kind).tone == ch.orellbuehler.acolyte.feature.Tone.FAIL }
                                ?: list.firstOrNull { it.kind == "needs_decision" } ?: head
                        HistoryRules.lineOf(worst)
                    } else {
                        HistoryRules.lineOf(head)
                    }
                HistoryItem(
                    key = key,
                    head = head,
                    events = list,
                    steps = steps,
                    verb = verb,
                    title = head.titleName.ifEmpty { brief?.name.orEmpty() },
                    episode = list.firstNotNullOfOrNull { it.episode.ifEmpty { null } }.orEmpty(),
                    version = head.version,
                    line = line,
                    extra = HistoryRules.extraOf(list, verb),
                    who = HistoryRules.whoOf(head),
                    poster = brief?.artwork?.firstOrNull(),
                    square = brief?.kind == "artist",
                    facts = HistoryRules.facts(list),
                    redo = HistoryRules.redo(list, verb),
                    releaseId = HistoryRules.releaseOf(list),
                    downloadId = list.firstNotNullOfOrNull { it.downloadId.takeIf { id -> id > 0 } } ?: 0,
                    failed = failingStep || verb.tone == ch.orellbuehler.acolyte.feature.Tone.FAIL,
                    needsYou = verb == HistoryVerb.NEEDS_YOU || verb == HistoryVerb.NEEDS_DECISION,
                )
            }
        val days =
            items
                .groupBy { Days.localDayOf(it.head.at, offset) ?: Day(1970, 1, 1) }
                .map { (day, list) -> HistoryDay(day, list, list.count { it.failed }, list.count { it.needsYou }) }
        mutableState.update {
            it.copy(
                days = days,
                shown = items.size,
                loaded = events.size,
                more = events.size < it.total,
            )
        }
    }

    private companion object {
        const val PAGE = 100
    }
}
