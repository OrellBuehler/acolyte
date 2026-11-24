package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.ChooseBody
import ch.orellbuehler.acolyte.api.model.LookupMovie
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.ReleaseCheckBody
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.api.model.TitleReleasePage
import ch.orellbuehler.acolyte.api.model.TurnDownBody
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ReleaseScopeKind {
    WHOLE,
    SEASON,
    EPISODE,
    CHECK,
}

data class ReleaseScope(
    val kind: ReleaseScopeKind,
    val season: Int = 0,
    val episode: Int = 0,
    val itemId: Long = 0,
    val slotId: Long = 0,
)

enum class ReleaseEmpty {
    NONE,
    NO_SEARCH,
    NOTHING_FOUND,
    DROPPED,
    FAILED,
    NO_GOOD,
    NO_MATCH,
}

data class ReleasesState(
    val scope: ReleaseScope,
    val show: Boolean,
    val titleName: String,
    val status: LoadStatus = LoadStatus(),
    val page: TitleReleasePage? = null,
    val allTab: Boolean = false,
    val sort: ReleaseSort = ReleaseSort.BEST,
    val resolution: Int = 0,
    val source: String = "",
    val language: String = "",
    val query: String = "",
    val limit: Int = 100,
    val armed: Set<Long> = emptySet(),
    val sent: Set<Long> = emptySet(),
    val busy: Long = 0,
    val starting: Boolean = false,
    val rules: List<PickRule> = emptyList(),
    val rulesId: Long = 0,
    val checkId: Long = 0,
    val picked: TitleRelease? = null,
    val done: Boolean = false,
    val feedback: Feedback? = null,
) {
    val check: Boolean get() = scope.kind == ReleaseScopeKind.CHECK

    val wholeShow: Boolean get() = show && scope.kind == ReleaseScopeKind.WHOLE

    val running: Boolean get() = starting || page?.runningJobIds.orEmpty().isNotEmpty()

    val all: List<TitleRelease> get() = page?.releases.orEmpty()

    val total: Int get() = page?.total ?: 0

    fun find(releaseId: Long): TitleRelease? = all.firstOrNull { it.id == releaseId }

    val goodCount: Int get() = all.count { TitleLogic.goodMatch(it) }

    val resolutions: List<Int> get() = TitleLogic.resolutionsOf(all)

    val sources: List<String> get() = TitleLogic.sourcesOf(all)

    val languages: List<String> get() = TitleLogic.languagesOf(all)

    val shown: List<TitleRelease>
        get() {
            val base = if (allTab) all else all.filter { TitleLogic.goodMatch(it) }
            val filtered =
                base.filter { r ->
                    (resolution == 0 || r.read?.resolution == resolution) &&
                        (source.isEmpty() || r.sourceName == source) &&
                        (language.isEmpty() || language in r.read?.languages.orEmpty()) &&
                        (query.isBlank() || TitleLogic.nameMatches(r.name, query))
                }
            return TitleLogic.sortReleases(filtered, sort)
        }

    val filtering: Boolean get() = resolution != 0 || source.isNotEmpty() || language.isNotEmpty() || query.isNotBlank()

    val canShowMore: Boolean get() = total > all.size && limit < MAX_LIMIT

    val empty: ReleaseEmpty
        get() {
            val p = page ?: return ReleaseEmpty.NONE
            if (all.isEmpty()) {
                if (running) return ReleaseEmpty.NONE
                val last = p.lastSearch ?: return ReleaseEmpty.NO_SEARCH
                return when {
                    last.failed -> ReleaseEmpty.FAILED
                    last.dropped > 0 -> ReleaseEmpty.DROPPED
                    last.at.isEmpty() -> ReleaseEmpty.NO_SEARCH
                    else -> ReleaseEmpty.NOTHING_FOUND
                }
            }
            if (shown.isEmpty()) return if (!allTab && !filtering) ReleaseEmpty.NO_GOOD else ReleaseEmpty.NO_MATCH
            return ReleaseEmpty.NONE
        }

    internal companion object {
        const val MAX_LIMIT = 500
    }
}

class ReleasesPresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    scope: ReleaseScope,
    show: Boolean,
    titleName: String,
    private val movie: LookupMovie?,
    private val tmdb: String,
    private val onChanged: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(ReleasesState(scope, show, titleName, sort = lastSort))
    private val lock = Mutex()
    private val feedback = FeedbackCounter()

    val state: StateFlow<ReleasesState> = mutableState.asStateFlow()

    private val current: ReleasesState get() = mutableState.value

    suspend fun load() {
        if (current.check) {
            loadRules()
            if (current.checkId == 0L) {
                mutableState.update { it.copy(status = LoadStatus.ready, page = TitleReleasePage()) }
                return
            }
        }
        reload()
    }

    private suspend fun loadRules() {
        if (current.rules.isNotEmpty()) return
        val rules =
            TitleLogic.rulesFor(
                api
                    .result(LibraryEndpoints.pickRules)
                    .valueOrNull()
                    ?.pickRules
                    .orEmpty(),
                "movie",
            )
        val defaults = api.result(TitleSupportEndpoints.defaults).valueOrNull()?.movie
        val start = rules.firstOrNull { it.id == defaults?.pickRulesId }?.id ?: rules.firstOrNull()?.id ?: 0
        mutableState.update { it.copy(rules = rules, rulesId = if (it.rulesId == 0L) start else it.rulesId) }
    }

    suspend fun reload() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            val s = current
            val result =
                if (s.check) {
                    api.result(TitleEndpoints.checkResults, "id" to s.checkId)
                } else {
                    api.result(
                        TitleEndpoints.releases,
                        "id" to titleId,
                        "limit" to s.limit,
                        "offset" to 0,
                        "season" to s.scope.season.takeIf { s.scope.kind == ReleaseScopeKind.SEASON },
                        "item_id" to s.scope.itemId.takeIf { s.scope.kind == ReleaseScopeKind.EPISODE },
                    )
                }
            when (result) {
                is ApiResult.Ok -> mutableState.update { it.copy(status = LoadStatus.ready, page = result.value, starting = false) }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error), starting = false) }
            }
        }
    }

    suspend fun watch() {
        while (true) {
            delay(POLL_MILLIS)
            if (current.page
                    ?.runningJobIds
                    .orEmpty()
                    .isNotEmpty()
            ) {
                reload()
            }
        }
    }

    suspend fun search() {
        val s = current
        if (s.wholeShow || s.running) return
        mutableState.update { it.copy(starting = true, feedback = null) }
        if (s.check) {
            val m = movie
            val body =
                ReleaseCheckBody(
                    tmdb = tmdb.toLongOrNull() ?: 0,
                    name = m?.name.orEmpty().ifEmpty { s.titleName },
                    pickRulesId = s.rulesId,
                    imdb = m?.ids?.get("imdb").orEmpty(),
                    originalLanguage = m?.originalLanguage.orEmpty(),
                    year = m?.year ?: 0,
                )
            when (val result = api.result(TitleEndpoints.check, body)) {
                is ApiResult.Ok -> mutableState.update { it.copy(checkId = result.value.jobIds.firstOrNull() ?: 0) }
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(starting = false, feedback = feedback.failed(result.error)) }
                    return
                }
            }
        } else {
            val body =
                when (s.scope.kind) {
                    ReleaseScopeKind.SEASON -> SearchBody(season = s.scope.season, choose = false)
                    ReleaseScopeKind.EPISODE -> SearchBody(slotId = s.scope.slotId.takeIf { it > 0 }, choose = false)
                    else -> SearchBody(choose = false)
                }
            val result = api.result(LibraryEndpoints.search, body, "id" to titleId)
            if (result is ApiResult.Failed) {
                mutableState.update { it.copy(starting = false, feedback = feedback.failed(result.error)) }
                return
            }
        }
        reload()
    }

    suspend fun pick(release: TitleRelease) {
        val s = current
        if (release.needsForce && release.id !in s.armed) {
            mutableState.update { it.copy(armed = it.armed + release.id) }
            return
        }
        if (s.check) {
            mutableState.update { it.copy(picked = release, done = true) }
            return
        }
        mutableState.update { it.copy(busy = release.id) }
        val force = release.needsForce || release.id in s.armed
        val slot = s.scope.slotId.takeIf { s.scope.kind == ReleaseScopeKind.EPISODE && it > 0 }
        when (val result = api.result(TitleEndpoints.choose, ChooseBody(force = force, slotId = slot), "id" to release.id)) {
            is ApiResult.Ok -> {
                val c = result.value
                val note =
                    when {
                        c.notSent.isNotEmpty() -> feedback.next(FeedbackKind.NOT_SENT, c.notSent)
                        c.sending -> feedback.next(FeedbackKind.SENDING, c.warnings.firstOrNull().orEmpty(), c.warnings.size)
                        else -> feedback.next(FeedbackKind.SENT, c.warnings.firstOrNull().orEmpty(), c.warnings.size)
                    }
                mutableState.update { it.copy(busy = 0, sent = it.sent + release.id, feedback = note, done = c.notSent.isEmpty()) }
                onChanged()
            }
            is ApiResult.Failed -> {
                if (result.error.isNeedsForce() && !force) {
                    mutableState.update { it.copy(busy = 0, armed = it.armed + release.id) }
                } else {
                    mutableState.update { it.copy(busy = 0, feedback = feedback.failed(result.error)) }
                }
            }
        }
    }

    suspend fun turnDown(
        release: TitleRelease,
        blockGroup: Boolean,
    ) {
        val s = current
        mutableState.update { it.copy(busy = release.id) }
        val slot = s.scope.slotId.takeIf { s.scope.kind == ReleaseScopeKind.EPISODE && it > 0 }
        api
            .result(TitleEndpoints.turnDown, TurnDownBody(slotId = slot, blockGroup = blockGroup.takeIf { it }), "id" to release.id)
            .onSuccess {
                val group = release.read?.group.orEmpty()
                say(if (blockGroup) feedback.next(FeedbackKind.GROUP_BLOCKED, group) else feedback.next(FeedbackKind.TURNED_DOWN))
            }.onFailure { say(feedback.failed(it)) }
        mutableState.update { it.copy(busy = 0) }
        reload()
    }

    fun setAllTab(all: Boolean) = mutableState.update { it.copy(allTab = all) }

    fun setSort(sort: ReleaseSort) {
        lastSort = sort
        mutableState.update { it.copy(sort = sort) }
    }

    fun setResolution(resolution: Int) = mutableState.update { it.copy(resolution = resolution) }

    fun setSource(source: String) = mutableState.update { it.copy(source = source) }

    fun setLanguage(language: String) = mutableState.update { it.copy(language = language) }

    fun setQuery(query: String) = mutableState.update { it.copy(query = query) }

    fun clearFilters() = mutableState.update { it.copy(resolution = 0, source = "", language = "", query = "", allTab = true) }

    suspend fun showMore() {
        mutableState.update { it.copy(limit = minOf(ReleasesState.MAX_LIMIT, it.limit + 100)) }
        reload()
    }

    fun setRules(rulesId: Long) {
        if (rulesId == current.rulesId) return
        mutableState.update { it.copy(rulesId = rulesId, checkId = 0, page = TitleReleasePage(), armed = emptySet()) }
    }

    private fun say(next: Feedback) {
        mutableState.update { it.copy(feedback = next) }
    }

    internal companion object {
        const val POLL_MILLIS = 3_000L
        var lastSort = ReleaseSort.BEST
    }
}
