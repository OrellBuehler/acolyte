package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DownloadsEndpoints
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.CalendarRow
import ch.orellbuehler.acolyte.api.model.GridTitle
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
import kotlin.time.Duration.Companion.seconds

data class CalendarDay(
    val day: Day,
    val groups: List<CalendarGroup>,
)

enum class CalendarNoticeKind {
    FOLLOWED,
    UNFOLLOWED,
    SEARCHING,
    NOTHING_TO_SEARCH,
    SEARCHING_MISSING,
}

data class CalendarNotice(
    val id: Long,
    val kind: CalendarNoticeKind,
    val name: String = "",
    val count: Int = 0,
)

data class CalendarState(
    val status: LoadStatus = LoadStatus(),
    val view: CalendarLayout = CalendarLayout.AGENDA,
    val month: Month = Month(1970, 1),
    val today: Day = Day(1970, 1, 1),
    val selected: Day? = null,
    val type: CalendarType = CalendarType.ALL,
    val follows: CalendarFollows = CalendarFollows.LIBRARY,
    val myFollows: Int = 0,
    val people: Int = 0,
    val days: List<CalendarDay> = emptyList(),
    val cells: List<MonthCell> = emptyList(),
    val hidden: Int = 0,
    val missing: Int = 0,
    val canSearch: Boolean = false,
    val musicOn: Boolean = true,
    val canPrevious: Boolean = true,
    val canNext: Boolean = true,
    val busy: Set<String> = emptySet(),
    val notice: CalendarNotice? = null,
    val actionError: ApiError? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && days.isEmpty()

    val selectedGroups: List<CalendarGroup> get() = selected?.let { s -> days.firstOrNull { it.day == s }?.groups }.orEmpty()

    fun group(key: String): CalendarGroup? = days.firstNotNullOfOrNull { d -> d.groups.firstOrNull { it.key == key } }
}

class CalendarPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val clock: Clock = Clock.System,
    private val offset: (Long) -> Int = ::localOffsetSeconds,
) {
    private val links = TitleLinks(api)
    private val lock = Mutex()
    private val loaded = mutableMapOf<String, List<CalendarRow>>()
    private var titles: Map<Long, GridTitle> = emptyMap()
    private var titlesTried = false
    private var followsPicked = false
    private val texts = mutableMapOf<String, String>()
    private var noticeId = 0L

    private val today: Day get() = Days.today(clock.now(), offset)

    private val mutableState =
        MutableStateFlow(
            today.let { t -> CalendarState(month = Month(t.year, t.month), today = t, selected = t) },
        )

    val state: StateFlow<CalendarState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        loaded.clear()
        loadMonth(mutableState.value.month, quiet = false)
    }

    fun setView(view: CalendarLayout) {
        mutableState.update { it.copy(view = view) }
    }

    fun setType(type: CalendarType) {
        mutableState.update { it.copy(type = type) }
        publish()
    }

    fun setFollows(follows: CalendarFollows) {
        followsPicked = true
        mutableState.update { it.copy(follows = follows) }
        publish()
    }

    fun select(day: Day) {
        mutableState.update { it.copy(selected = day) }
    }

    suspend fun showMonth(month: Month) {
        val t = today
        val min = Month(t.year, t.month).plus(-12)
        val max = Month(t.year, t.month).plus(12)
        val target =
            if (month.year * 12 + month.month <
                min.year * 12 + min.month
            ) {
                min
            } else if (month.year * 12 + month.month > max.year * 12 + max.month) {
                max
            } else {
                month
            }
        mutableState.update {
            it.copy(
                month = target,
                selected = if (t.year == target.year && t.month == target.month) t else target.first,
                canPrevious = target != min,
                canNext = target != max,
            )
        }
        publish()
        loadMonth(target, quiet = true)
    }

    suspend fun next() = showMonth(mutableState.value.month.plus(1))

    suspend fun previous() = showMonth(mutableState.value.month.plus(-1))

    suspend fun goToday() {
        val t = today
        showMonth(Month(t.year, t.month))
        mutableState.update { it.copy(selected = t) }
    }

    fun consumeNotice(id: Long) {
        mutableState.update { if (it.notice?.id == id) it.copy(notice = null) else it }
    }

    fun clearActionError() {
        mutableState.update { it.copy(actionError = null) }
    }

    suspend fun titleLink(titleId: Long): DownloadTitleLink? = links.resolve(titleId)

    fun link(group: CalendarGroup): DownloadTitleLink? {
        val e = group.lead
        if (e.tmdb.isEmpty() || e.tmdb == "0") return null
        return when (e.kind) {
            "episode" -> DownloadTitleLink("show", e.tmdb)
            "album" -> null
            else -> DownloadTitleLink("movie", e.tmdb)
        }
    }

    private suspend fun loadMonth(
        month: Month,
        quiet: Boolean,
    ) {
        lock.withLock {
            val who = account()
            mutableState.update {
                it.copy(
                    today = today,
                    canSearch = who?.can(Need.MANAGE_LIBRARY) == true && !who.isKid,
                    musicOn = who?.isOn(Part.MUSIC) == true && !who.isKid,
                    status = if (quiet && it.status.phase == Phase.READY) it.status else it.status.started(),
                )
            }
            if (!titlesTried) {
                titlesTried = true
                links.briefs()?.let { titles = it }
            }
            if (month.key !in loaded) {
                val (from, to) = CalendarRules.range(month)
                when (val answer = api.result(DownloadsEndpoints.calendar, "from" to from.key, "to" to to.key)) {
                    is ApiResult.Ok -> {
                        loaded[month.key] = answer.value.dates
                        mutableState.update { s ->
                            val follows =
                                if (followsPicked) {
                                    s.follows
                                } else {
                                    CalendarRules.startFollows(
                                        answer.value.myFollows,
                                        answer.value.people,
                                        null,
                                    )
                                }
                            s.copy(
                                status = LoadStatus.ready,
                                myFollows = answer.value.myFollows,
                                people = answer.value.people,
                                follows = follows,
                            )
                        }
                    }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(answer.error)) }
                }
            } else {
                mutableState.update { it.copy(status = LoadStatus.ready) }
            }
        }
        publish()
    }

    private fun publish() {
        val s = mutableState.value
        val rows = loaded[s.month.key].orEmpty()
        val unique = rows.distinctBy { CalendarRules.entryKey(it) }
        val single = CalendarRules.oneMovieDate(unique, titles)
        val typed = single.filter { CalendarRules.shows(it, s.type) && (s.musicOn || it.kind != "album") }
        val shown = typed.filter { CalendarRules.followsOk(it, s.follows) }
        val t = today
        val groups =
            CalendarRules
                .groups(shown, t, s.canSearch, offset)
                .filter { it.day.year == s.month.year && it.day.month == s.month.month }
        val days = groups.groupBy { it.day }.map { (day, list) -> CalendarDay(day, list) }
        mutableState.update {
            it.copy(
                today = t,
                days = days,
                cells = CalendarRules.monthCells(it.month, groups, t),
                hidden = typed.size - shown.size,
                missing = if (it.canSearch) CalendarRules.missing(groups, t, offset).size else 0,
            )
        }
    }

    suspend fun follow(
        group: CalendarGroup,
        on: Boolean,
    ) {
        val id = group.lead.titleId
        val key = "follow-$id"
        mutableState.update { it.copy(busy = it.busy + key, actionError = null) }
        val result =
            if (on) {
                api.result(TitleEndpoints.followTitleOn, "id" to id)
            } else {
                api.result(TitleEndpoints.followTitleOff, "id" to id)
            }
        mutableState.update { it.copy(busy = it.busy - key) }
        result
            .onSuccess { answer ->
                for ((month, rows) in loaded.toMap()) {
                    loaded[month] = rows.map { if (it.titleId == id) it.copy(following = answer.following) else it }
                }
                noticeId++
                mutableState.update {
                    it.copy(
                        myFollows = (it.myFollows + if (answer.following) 1 else -1).coerceAtLeast(0),
                        notice =
                            CalendarNotice(
                                noticeId,
                                if (answer.following) CalendarNoticeKind.FOLLOWED else CalendarNoticeKind.UNFOLLOWED,
                                group.lead.name,
                            ),
                    )
                }
                publish()
            }.onFailure { error -> mutableState.update { it.copy(actionError = error) } }
    }

    suspend fun search(group: CalendarGroup) {
        val t = today
        val entries = group.entries.filter { CalendarRules.dot(it, t, offset) != EntryDot.HAVE }
        searchEntries(group.key, entries, CalendarNoticeKind.SEARCHING, group.title)
    }

    suspend fun searchMissing() {
        val s = mutableState.value
        val entries = CalendarRules.missing(s.days.flatMap { it.groups }, today, offset)
        searchEntries("missing", entries, CalendarNoticeKind.SEARCHING_MISSING, "")
    }

    private suspend fun searchEntries(
        key: String,
        entries: List<CalendarRow>,
        kind: CalendarNoticeKind,
        name: String,
    ) {
        if (!mutableState.value.canSearch) return
        mutableState.update { it.copy(busy = it.busy + key, actionError = null) }
        var jobs = 0
        var error: ApiError? = null
        for (e in entries) {
            when (val result = api.result(LibraryEndpoints.search, CalendarRules.searchBody(e), "id" to e.titleId)) {
                is ApiResult.Ok -> jobs += result.value.jobIds.size
                is ApiResult.Failed -> if (error == null) error = result.error
            }
        }
        noticeId++
        mutableState.update {
            it.copy(
                busy = it.busy - key,
                actionError = error,
                notice =
                    if (error == null) {
                        CalendarNotice(
                            noticeId,
                            if (jobs == 0 &&
                                entries.size <= 1
                            ) {
                                CalendarNoticeKind.NOTHING_TO_SEARCH
                            } else {
                                kind
                            },
                            name,
                            entries.size,
                        )
                    } else {
                        null
                    },
            )
        }
    }

    suspend fun describe(group: CalendarGroup): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for (e in group.entries) {
            val key = CalendarRules.entryKey(e)
            texts[key]?.let {
                out[key] = it
                continue
            }
            if (e.tmdb.isEmpty()) continue
            val text =
                when (e.kind) {
                    "episode" ->
                        api
                            .result(TitleEndpoints.season, "tmdb_id" to e.tmdb, "number" to e.season)
                            .valueOrNull()
                            ?.episodes
                            ?.firstOrNull { it.number == e.episode }
                            ?.overview
                    "album" -> null
                    else -> api.result(TitleEndpoints.details, "kind" to "movie", "tmdb_id" to e.tmdb).valueOrNull()?.overview
                }
            if (!text.isNullOrBlank()) {
                texts[key] = text
                out[key] = text
            }
        }
        return out
    }

    suspend fun watch() {
        coroutineScope {
            val poke = Channel<Unit>(Channel.CONFLATED)
            launch {
                api.live.collect { event ->
                    if (event.topic == "title" || event.topic == LiveEvent.RESYNC) poke.trySend(Unit)
                }
            }
            launch {
                for (x in poke) {
                    delay(1.seconds)
                    loaded.clear()
                    loadMonth(mutableState.value.month, quiet = true)
                }
            }
        }
    }
}
