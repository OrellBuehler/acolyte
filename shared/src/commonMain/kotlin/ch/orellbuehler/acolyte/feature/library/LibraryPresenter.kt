package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.AnyoneWatched
import ch.orellbuehler.acolyte.api.model.CopySearchRequest
import ch.orellbuehler.acolyte.api.model.CopySelection
import ch.orellbuehler.acolyte.api.model.GridTitle
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.MissingSearch
import ch.orellbuehler.acolyte.api.model.MissingSubtitleSearch
import ch.orellbuehler.acolyte.api.model.MovieCollection
import ch.orellbuehler.acolyte.api.model.MyWatched
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.TitleChange
import ch.orellbuehler.acolyte.api.model.TitleMove
import ch.orellbuehler.acolyte.api.model.TitleRefresh
import ch.orellbuehler.acolyte.api.model.TitleRemove
import ch.orellbuehler.acolyte.api.model.VersionAudioChange
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.nav.LibraryTool
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class LibraryLayout(
    internal val wire: String,
) {
    LIST("rows"),
    GRID("posters"),
}

enum class ActiveFilter {
    STATUS,
    KEPT,
    ASKER,
    SERIES,
    CODEC,
    SUBS,
    WATCHED,
    SEARCH,
    GROUPED,
}

data class LibraryItem(
    val id: Long,
    val kind: LibraryKind,
    val name: String,
    val year: Int,
    val poster: String?,
    val tmdb: String,
    val state: LibState?,
    val have: Int,
    val total: Int,
    val kept: Boolean,
    val watchedSeen: Int,
    val watchedOf: Int,
    val fact: LibraryFact?,
    val overview: String,
) {
    val canOpen: Boolean get() = tmdb.isNotEmpty()
    val share: Float get() = Progress(have, total).share
}

data class FilterOption(
    val id: String,
    val label: String = "",
    val count: Int = -1,
    val selected: Boolean = false,
)

data class ActiveChip(
    val filter: ActiveFilter,
    val id: String,
    val label: String = "",
)

data class LibraryGroup(
    val id: Long,
    val name: String,
    val items: List<LibraryItem>,
)

data class MenuOption(
    val id: Long,
    val label: String,
)

data class SavedFilterOption(
    val name: String,
    val active: Boolean,
)

data class CopyReviewItem(
    val title: String,
    val version: String,
    val codec: String,
)

data class CopyReview(
    val items: List<CopyReviewItem>,
    val slotCount: Int,
    internal val token: String,
    internal val selections: List<CopySelection>,
)

data class LibraryState(
    val status: LoadStatus = LoadStatus(),
    val kind: LibraryKind = LibraryKind.MOVIE,
    val kinds: List<LibraryKind> = emptyList(),
    val musicEntry: Boolean = false,
    val canManage: Boolean = false,
    val showsPosterState: Boolean = false,
    val tools: List<LibraryTool> = emptyList(),
    val addKinds: List<LibraryKind> = emptyList(),
    val canOpenFolders: Boolean = false,
    val layout: LibraryLayout = LibraryLayout.LIST,
    val items: List<LibraryItem> = emptyList(),
    val groups: List<LibraryGroup> = emptyList(),
    val total: Int = 0,
    val query: String = "",
    val searchFailed: Boolean = false,
    val sort: LibrarySort = LibrarySort.ADDED,
    val descending: Boolean = true,
    val sorts: List<LibrarySort> = emptyList(),
    val filter: LibraryFilter = LibraryFilter(),
    val statusOptions: List<FilterOption> = emptyList(),
    val keptOptions: List<FilterOption> = emptyList(),
    val askerOptions: List<FilterOption> = emptyList(),
    val codecOptions: List<FilterOption> = emptyList(),
    val seriesOptions: List<FilterOption> = emptyList(),
    val subtitlesOption: FilterOption? = null,
    val watchedOption: FilterOption? = null,
    val chips: List<ActiveChip> = emptyList(),
    val grouped: Boolean = false,
    val canGroup: Boolean = false,
    val savedFilters: List<SavedFilterOption> = emptyList(),
    val selecting: Boolean = false,
    val selected: List<Long> = emptyList(),
    val moveOptions: List<MenuOption> = emptyList(),
    val ruleOptions: List<MenuOption> = emptyList(),
    val canFindCopies: Boolean = false,
    val canSetAudio: Boolean = false,
    val missingCount: Int = 0,
    val canSearchSubtitles: Boolean = false,
    val deletedCount: Int = 0,
    val working: Boolean = false,
    val notice: LibraryNotice? = null,
    val copiesReview: CopyReview? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && total == 0
    val nothingMatches: Boolean get() = status.phase == Phase.READY && total > 0 && items.isEmpty()
    val selectedCount: Int get() = selected.size
    val canSaveFilter: Boolean get() = filter.isActive
    val activeFilterCount: Int get() = filter.activeCount
}

class LibraryPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val prefs: LibraryPrefs,
    initialKind: LibraryKind?,
) {
    private val mutableState = MutableStateFlow(LibraryState())
    private val lock = Mutex()
    private val notices = Notices()

    private var titles: List<GridTitle> = emptyList()
    private var states: Map<Long, LibState?> = emptyMap()
    private var watched: MyWatched? = null
    private var anyone: AnyoneWatched? = null
    private var subtitleIds: Set<Long>? = null
    private var collections: List<MovieCollection> = emptyList()
    private var folders: List<LibraryFolder>? = null
    private var rules: List<PickRule>? = null
    private var serverIds: Set<Long>? = null
    private var picked: Set<Long> = emptySet()
    private var people: Map<Long, List<Pair<Long, String>>> = emptyMap()
    private var seriesByTitle: Map<Long, List<MovieCollection>> = emptyMap()

    val state: StateFlow<LibraryState> = mutableState.asStateFlow()

    init {
        val me = account()
        val kinds = kindsFor(me)
        val owner = owner(me)
        val kind =
            initialKind?.takeIf { it in kinds } ?: prefs.lastKind(owner)?.takeIf { it in kinds } ?: kinds.firstOrNull() ?: LibraryKind.MOVIE
        val (sort, descending) = LibraryLogic.defaultSort(kind)
        mutableState.value = LibraryState(kind = kind, sort = sort, descending = descending, layout = layoutFor(me, kind))
        rebuild()
    }

    private fun owner(me: Account?): String = "${me?.userId ?: 0}@${me?.serverName.orEmpty()}"

    private fun kindsFor(me: Account?): List<LibraryKind> {
        me ?: return listOf(LibraryKind.MOVIE, LibraryKind.SHOW)
        return buildList {
            if (me.shows(Section.MOVIES)) add(LibraryKind.MOVIE)
            if (me.shows(Section.SHOWS)) add(LibraryKind.SHOW)
        }
    }

    private fun layoutFor(
        me: Account?,
        kind: LibraryKind,
    ): LibraryLayout =
        if (me?.savedViews?.get("library.view.${kind.wire}") ==
            LibraryLayout.GRID.wire
        ) {
            LibraryLayout.GRID
        } else {
            LibraryLayout.LIST
        }

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            val me = account()
            when (val result = api.result(LibraryEndpoints.titles, "grid" to 1)) {
                is ApiResult.Ok -> {
                    titles = result.value.titles
                    val now = LibraryLogic.now()
                    states = titles.associate { it.id to LibraryLogic.state(it, now) }
                    people = titles.associate { it.id to linkedPeople(it) }
                    loadExtras(me)
                    indexSeries()
                    mutableState.update { it.copy(status = LoadStatus.ready) }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
            rebuild()
        }
    }

    private suspend fun loadExtras(me: Account?) {
        coroutineScope {
            val manage = me?.managesLibrary == true
            val watchedCall = async { api.result(LibraryEndpoints.watched).valueOrNull() }
            val anyoneCall =
                async {
                    if (me != null && me.can(Need.USERS) && !me.isKid) api.result(LibraryEndpoints.watchedByAnyone).valueOrNull() else null
                }
            val subsCall =
                async {
                    if (manage && me.isOn(Part.SUBTITLES) && !me.handsOff) {
                        api.result(LibraryEndpoints.missingSubtitles, "limit" to 1).valueOrNull()
                    } else {
                        null
                    }
                }
            val seriesCall = async { if (me?.isOn(Part.MOVIES) != false) api.result(LibraryEndpoints.collections).valueOrNull() else null }
            val deletedCall = async { if (manage) api.result(LibraryEndpoints.holding, "count" to 1).valueOrNull() else null }
            watchedCall.await()?.let { watched = it }
            anyone = anyoneCall.await() ?: anyone
            subsCall.await()?.let { answer -> subtitleIds = answer.titles.map { it.id }.toSet() }
            seriesCall.await()?.let { collections = it.collections }
            deletedCall.await()?.let { answer -> mutableState.update { it.copy(deletedCount = answer.count) } }
        }
    }

    fun setKind(kind: LibraryKind) {
        val current = mutableState.value
        if (current.kind == kind || kind !in current.kinds) return
        val me = account()
        prefs.saveLastKind(owner(me), kind)
        val (sort, descending) = LibraryLogic.defaultSort(kind)
        picked = emptySet()
        serverIds = null
        mutableState.update {
            it.copy(
                kind = kind,
                sort = sort,
                descending = descending,
                filter = LibraryFilter(),
                query = "",
                searchFailed = false,
                grouped = false,
                selecting = false,
                layout = layoutFor(me, kind),
            )
        }
        rebuild()
    }

    suspend fun search(text: String) {
        val query = text.take(MAX_QUERY)
        mutableState.update { it.copy(query = query, searchFailed = false) }
        val cleaned = LibraryLogic.query(query)
        if (cleaned.isEmpty()) {
            serverIds = null
            rebuild()
            return
        }
        rebuild()
        delay(SEARCH_DELAY)
        val result = api.result(LibraryEndpoints.titles, "brief" to 1, "q" to cleaned)
        if (mutableState.value.query != query) return
        when (result) {
            is ApiResult.Ok ->
                serverIds =
                    result.value.titles
                        .map { it.id }
                        .toSet()
            is ApiResult.Failed -> mutableState.update { it.copy(searchFailed = true) }
        }
        rebuild()
    }

    fun setSort(sort: LibrarySort) {
        mutableState.update {
            if (it.sort == sort) it.copy(descending = !it.descending) else it.copy(sort = sort, descending = !sort.ascending)
        }
        rebuild()
    }

    fun setDescending(descending: Boolean) {
        mutableState.update { it.copy(descending = descending) }
        rebuild()
    }

    fun setStatus(id: String) =
        updateFilter { it.copy(status = StatusFilter.entries.firstOrNull { s -> s.wire == id } ?: StatusFilter.ALL) }

    fun toggleKept(id: String) =
        updateFilter {
            val next = if (id == "on") KeptFilter.ON else KeptFilter.OFF
            it.copy(kept = if (it.kept == next) KeptFilter.ANY else next)
        }

    fun setAsker(id: String) = updateFilter { it.copy(asker = id) }

    fun setSeries(id: Long) = updateFilter { it.copy(series = id) }

    fun setCodec(id: String) = updateFilter { it.copy(codec = if (it.codec == id) "" else id) }

    fun toggleSubtitles() = updateFilter { it.copy(subs = !it.subs) }

    fun toggleWatched() = updateFilter { it.copy(watched = !it.watched) }

    fun setGrouped(grouped: Boolean) {
        mutableState.update { it.copy(grouped = grouped && it.canGroup) }
        rebuild()
    }

    fun removeChip(filter: ActiveFilter) {
        when (filter) {
            ActiveFilter.STATUS -> updateFilter { it.copy(status = StatusFilter.ALL) }
            ActiveFilter.KEPT -> updateFilter { it.copy(kept = KeptFilter.ANY) }
            ActiveFilter.ASKER -> updateFilter { it.copy(asker = LibraryFilter.ASKER_ANYONE) }
            ActiveFilter.SERIES -> updateFilter { it.copy(series = 0) }
            ActiveFilter.CODEC -> updateFilter { it.copy(codec = "") }
            ActiveFilter.SUBS -> updateFilter { it.copy(subs = false) }
            ActiveFilter.WATCHED -> updateFilter { it.copy(watched = false) }
            ActiveFilter.SEARCH -> {
                serverIds = null
                mutableState.update { it.copy(query = "", searchFailed = false) }
                rebuild()
            }
            ActiveFilter.GROUPED -> setGrouped(false)
        }
    }

    fun clearFilters() {
        serverIds = null
        mutableState.update { it.copy(filter = LibraryFilter(), query = "", searchFailed = false, grouped = false) }
        rebuild()
    }

    private fun updateFilter(change: (LibraryFilter) -> LibraryFilter) {
        mutableState.update { it.copy(filter = change(it.filter)) }
        rebuild()
    }

    fun saveFilter(name: String) {
        val clean = name.trim().take(LibraryPrefs.MAX_NAME)
        val current = mutableState.value
        if (clean.isEmpty() || !current.filter.isActive) return
        val owner = owner(account())
        val list = prefs.filters(owner, current.kind).filter { it.name != clean } + SavedFilter(clean, current.filter)
        prefs.saveFilters(owner, current.kind, list)
        mutableState.update { it.copy(notice = notices.make(LibraryNoticeKind.FILTER_SAVED, name = clean)) }
        rebuild()
    }

    fun applySavedFilter(name: String) {
        val current = mutableState.value
        val saved = prefs.filters(owner(account()), current.kind).firstOrNull { it.name == name } ?: return
        updateFilter { saved.filter }
    }

    fun deleteSavedFilter(name: String) {
        val current = mutableState.value
        val owner = owner(account())
        prefs.saveFilters(owner, current.kind, prefs.filters(owner, current.kind).filter { it.name != name })
        rebuild()
    }

    suspend fun setLayout(layout: LibraryLayout) {
        val current = mutableState.value
        if (current.layout == layout) return
        mutableState.update { it.copy(layout = layout) }
        val names = if (current.kind == LibraryKind.ARTIST) listOf("artist") else listOf("all", "movie", "show")
        api.result(LibraryEndpoints.views, JsonObject(names.associate { "library.view.$it" to JsonPrimitive(layout.wire) }))
    }

    suspend fun startSelecting() {
        mutableState.update { it.copy(selecting = true) }
        rebuild()
        if (folders == null || rules == null) {
            coroutineScope {
                val folderCall = async { api.result(LibraryEndpoints.folders).valueOrNull() }
                val rulesCall = async { api.result(LibraryEndpoints.pickRules).valueOrNull() }
                folders = folderCall.await()?.libraryFolders ?: folders
                rules = rulesCall.await()?.pickRules ?: rules
            }
            rebuild()
        }
    }

    fun stopSelecting() {
        picked = emptySet()
        mutableState.update { it.copy(selecting = false, copiesReview = null) }
        rebuild()
    }

    fun toggle(id: Long) {
        picked = if (id in picked) picked - id else picked + id
        mutableState.update { it.copy(copiesReview = null) }
        rebuild()
    }

    fun selectAll() {
        picked =
            mutableState.value.items
                .map { it.id }
                .toSet()
        rebuild()
    }

    fun selectNone() {
        picked = emptySet()
        rebuild()
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    private fun pickedTitles(): List<GridTitle> {
        val shown =
            mutableState.value.items
                .map { it.id }
                .toSet()
        return titles.filter { it.id in picked && it.id in shown }
    }

    private suspend fun bulk(
        only: (GridTitle) -> Boolean,
        call: suspend (GridTitle) -> ApiResult<*>,
    ): Triple<Int, Int, ApiError?> {
        val targets = pickedTitles().filter(only)
        var ok = 0
        var error: ApiError? = null
        for (title in targets) {
            when (val result = call(title)) {
                is ApiResult.Ok -> ok++
                is ApiResult.Failed -> if (error == null) error = result.error
            }
        }
        return Triple(ok, targets.size, error)
    }

    private suspend fun runBulk(
        notice: LibraryNoticeKind,
        name: String = "",
        endSelecting: Boolean = false,
        only: (GridTitle) -> Boolean = { true },
        call: suspend (GridTitle) -> ApiResult<*>,
    ) {
        lock.withLock {
            mutableState.update { it.copy(working = true) }
            val (ok, total, error) = bulk(only, call)
            mutableState.update {
                it.copy(working = false, notice = notices.make(notice, count = ok, total = total, name = name, error = error))
            }
            if (endSelecting) picked = emptySet()
            if (endSelecting) mutableState.update { it.copy(selecting = false) }
        }
        refresh()
    }

    suspend fun keepSelected(on: Boolean) =
        runBulk(if (on) LibraryNoticeKind.KEPT else LibraryNoticeKind.UNKEPT) {
            api.result(LibraryEndpoints.change, TitleChange(keepUpToDate = on), "id" to it.id)
        }

    suspend fun setRules(id: Long) {
        val name = rules?.firstOrNull { it.id == id }?.name.orEmpty()
        runBulk(
            LibraryNoticeKind.RULES_SET,
            name = name,
        ) { api.result(LibraryEndpoints.change, TitleChange(pickRulesId = id), "id" to it.id) }
    }

    suspend fun setAudio(text: String) {
        val codes = LibraryLogic.audioCodes(text)
        runBulk(LibraryNoticeKind.AUDIO_SET, only = { it.kind != "artist" }) {
            api.result(LibraryEndpoints.audio, VersionAudioChange(codes), "id" to it.id, "vid" to "main")
        }
    }

    suspend fun searchSelected() =
        runBulk(LibraryNoticeKind.SEARCHING) {
            api.result(LibraryEndpoints.search, SearchBody(), "id" to it.id)
        }

    suspend fun refreshSelected() {
        val ids = pickedTitles().map { it.id }
        if (ids.isEmpty()) return
        mutableState.update { it.copy(working = true) }
        val result = api.result(LibraryEndpoints.refresh, TitleRefresh(ids))
        mutableState.update {
            it.copy(
                working = false,
                notice =
                    when (result) {
                        is ApiResult.Ok -> notices.make(LibraryNoticeKind.REFRESHING, count = ids.size, total = ids.size)
                        is ApiResult.Failed -> notices.failed(result.error)
                    },
            )
        }
    }

    suspend fun moveSelected(folderId: Long) {
        val folder = folders?.firstOrNull { it.id == folderId }
        val name = folder?.label?.ifEmpty { folder.path }.orEmpty()
        runBulk(LibraryNoticeKind.MOVING, name = name, endSelecting = true) {
            api.result(LibraryEndpoints.move, TitleMove(folderId), "id" to it.id)
        }
    }

    suspend fun removeSelected() =
        runBulk(LibraryNoticeKind.REMOVED, endSelecting = true) {
            api.result(LibraryEndpoints.remove, TitleRemove(deleteFiles = false, neverAdd = false, apply = true), "id" to it.id)
        }

    suspend fun reviewCopies() {
        val selections =
            pickedTitles()
                .mapNotNull { it.mainCopy }
                .filter { !it.isProtected && it.fileId > 0 }
                .map { CopySelection(it.fileId, it.versionId) }
                .distinct()
                .take(MAX_COPIES)
        if (selections.isEmpty()) return
        mutableState.update { it.copy(working = true) }
        api
            .result(LibraryEndpoints.copiesPreview, CopySearchRequest(selections))
            .onSuccess { preview ->
                val review =
                    CopyReview(
                        items = preview.copies.map { CopyReviewItem(it.title, it.version, it.codec) },
                        slotCount = preview.slotCount,
                        token = preview.reviewToken,
                        selections = selections,
                    )
                mutableState.update { it.copy(working = false, copiesReview = review) }
            }.onFailure { error -> mutableState.update { it.copy(working = false, notice = notices.failed(error)) } }
    }

    fun cancelCopies() {
        mutableState.update { it.copy(copiesReview = null) }
    }

    suspend fun startCopies() {
        val review = mutableState.value.copiesReview ?: return
        mutableState.update { it.copy(working = true) }
        api
            .result(LibraryEndpoints.copiesSearch, CopySearchRequest(review.selections, review.token))
            .onSuccess { answer ->
                mutableState.update {
                    it.copy(
                        working = false,
                        copiesReview = null,
                        notice = notices.make(LibraryNoticeKind.COPIES_SEARCHING, count = answer.jobIds.size),
                    )
                }
            }.onFailure { error -> mutableState.update { it.copy(working = false, copiesReview = null, notice = notices.failed(error)) } }
    }

    suspend fun searchAllMissing() {
        val current = mutableState.value
        mutableState.update { it.copy(working = true) }
        val result = api.result(LibraryEndpoints.searchMissing, MissingSearch(current.kind.wire))
        mutableState.update {
            it.copy(
                working = false,
                notice =
                    when (result) {
                        is ApiResult.Ok -> notices.make(LibraryNoticeKind.MISSING_SEARCHING, count = current.missingCount)
                        is ApiResult.Failed -> notices.failed(result.error)
                    },
            )
        }
    }

    suspend fun searchSubtitles() {
        val kind = if (mutableState.value.kind == LibraryKind.SHOW) "episode" else "movie"
        mutableState.update { it.copy(working = true) }
        val result = api.result(LibraryEndpoints.searchMissingSubtitles, MissingSubtitleSearch(kind = kind))
        mutableState.update {
            it.copy(
                working = false,
                notice =
                    when (result) {
                        is ApiResult.Ok -> notices.make(LibraryNoticeKind.SUBTITLES_SEARCHING, count = result.value.queued)
                        is ApiResult.Failed -> notices.failed(result.error)
                    },
            )
        }
    }

    suspend fun watch() {
        api.live
            .filter { it.topic == "title" || it.topic == "slot" || it.topic == LiveEvent.RESYNC }
            .conflate()
            .collect { refresh() }
    }

    private fun linkedPeople(t: GridTitle): List<Pair<Long, String>> =
        t.asks
            .filter { it.titleId == null || it.titleId == t.id }
            .flatMap { ask -> listOfNotNull(ask.askedBy) + ask.people }
            .filter { it.name.isNotBlank() }
            .map { it.id to LibraryLogic.personName(it.name).first }

    private fun indexSeries() {
        val index = mutableMapOf<Long, MutableList<MovieCollection>>()
        for (c in collections) for (id in c.titleIds) index.getOrPut(id) { mutableListOf() } += c
        seriesByTitle = index
    }

    private fun seriesOf(id: Long): List<MovieCollection> = seriesByTitle[id].orEmpty()

    private fun peopleOf(id: Long): List<Pair<Long, String>> = people[id].orEmpty()

    private fun passes(
        t: GridTitle,
        f: LibraryFilter,
        query: String,
        skip: ActiveFilter?,
        me: Account?,
    ): Boolean {
        if (skip != ActiveFilter.SEARCH && query.isNotEmpty() && !LibraryLogic.byName(t, query) && serverIds?.contains(t.id) != true) {
            return false
        }
        if (skip != ActiveFilter.STATUS && !LibraryLogic.matches(t, f.status)) return false
        if (skip != ActiveFilter.KEPT && f.kept == KeptFilter.ON && !t.keepUpToDate) return false
        if (skip != ActiveFilter.KEPT && f.kept == KeptFilter.OFF && t.keepUpToDate) return false
        if (skip != ActiveFilter.ASKER && f.asker != LibraryFilter.ASKER_ANYONE) {
            val wanted = if (f.asker == LibraryFilter.ASKER_ME) me?.userId else f.asker.toLongOrNull()
            if (peopleOf(t.id).none { it.first == wanted }) return false
        }
        if (skip != ActiveFilter.SERIES && f.series != 0L && seriesOf(t.id).none { it.id == f.series }) return false
        if (skip != ActiveFilter.CODEC && f.codec.isNotEmpty() && t.mainCopy?.codec != f.codec) return false
        if (skip != ActiveFilter.SUBS && f.subs && subtitleIds?.contains(t.id) != true) return false
        val known = anyone
        if (skip != ActiveFilter.WATCHED && f.watched && known != null && known.enabled && t.id !in known.titles) return false
        return true
    }

    private fun item(
        t: GridTitle,
        sort: LibrarySort,
        now: Long,
    ): LibraryItem {
        val progress = LibraryLogic.progress(t)
        val (seen, of) = LibraryLogic.watched(t, watched)
        return LibraryItem(
            id = t.id,
            kind = LibraryKind.of(t.kind) ?: LibraryKind.MOVIE,
            name = t.name,
            year = t.year,
            poster = t.artwork.firstOrNull(),
            tmdb = LibraryLogic.tmdbOf(t),
            state = states[t.id],
            have = progress.have,
            total = progress.total,
            kept = t.keepUpToDate,
            watchedSeen = seen,
            watchedOf = of,
            fact = LibraryLogic.fact(t, sort, now),
            overview = t.overview.ifEmpty { t.artist?.bio.orEmpty() },
        )
    }

    private fun rebuild() {
        val me = account()
        val current = mutableState.value
        val kind = current.kind
        val kindTitles = titles.filter { it.kind == kind.wire }
        val query = LibraryLogic.query(current.query)
        val f = current.filter
        val now = LibraryLogic.now()

        val bases = mutableMapOf<ActiveFilter, List<GridTitle>>()

        fun count(
            skip: ActiveFilter,
            test: (GridTitle) -> Boolean,
        ): Int = bases.getOrPut(skip) { kindTitles.filter { passes(it, f, query, skip, me) } }.count(test)

        val shownTitles = LibraryLogic.sort(kindTitles.filter { passes(it, f, query, null, me) }, current.sort, current.descending, states)
        val items = shownTitles.map { item(it, current.sort, now) }
        val manage = me?.managesLibrary == true
        val canGroup = kind == LibraryKind.MOVIE && collections.isNotEmpty()
        val grouped = current.grouped && canGroup
        val askers =
            kindTitles
                .flatMap { peopleOf(it.id) }
                .distinctBy { it.first }
                .filter { it.first != me?.userId }
                .sortedBy { it.second.lowercase() }
        val codecs =
            if (kind == LibraryKind.MOVIE) {
                kindTitles
                    .mapNotNull { it.mainCopy?.codec?.takeIf { c -> c.isNotEmpty() } }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
            } else {
                emptyList()
            }
        val showCodecs = f.codec.isNotEmpty() || codecs.count { it != "unknown" } > 1
        val subsCount = if (subtitleIds != null) count(ActiveFilter.SUBS) { subtitleIds?.contains(it.id) == true } else 0
        val subsAvailable = manage && kind != LibraryKind.ARTIST && subtitleIds != null && (subsCount > 0 || f.subs)
        val anyoneNow = anyone
        val watchedCount = if (anyoneNow != null && anyoneNow.enabled) count(ActiveFilter.WATCHED) { it.id in anyoneNow.titles } else 0
        val watchedAvailable = kind != LibraryKind.ARTIST && anyoneNow?.enabled == true && (watchedCount > 0 || f.watched)
        val visible = items.map { it.id }.toSet()
        val selected = picked.filter { it in visible }
        val pickedNow = titles.filter { it.id in selected }
        val pickedKinds = pickedNow.map { it.kind }.distinct()
        val folderKind = pickedKinds.singleOrNull()?.let { if (it == "artist") "music" else it }
        val kindFolders = folders.orEmpty().filter { it.kind == folderKind }
        val neededKinds = (pickedKinds.ifEmpty { listOf(kind.wire) }).map { if (it == "artist") "music" else it }.toSet()
        val owner = owner(me)
        val saved = prefs.filters(owner, kind)
        val chips =
            buildList {
                if (f.status != StatusFilter.ALL) add(ActiveChip(ActiveFilter.STATUS, f.status.wire))
                if (f.kept != KeptFilter.ANY) add(ActiveChip(ActiveFilter.KEPT, if (f.kept == KeptFilter.ON) "on" else "off"))
                if (f.asker != LibraryFilter.ASKER_ANYONE) {
                    val name = askers.firstOrNull { it.first.toString() == f.asker }?.second.orEmpty()
                    add(ActiveChip(ActiveFilter.ASKER, f.asker, name))
                }
                if (f.series != 0L) {
                    add(ActiveChip(ActiveFilter.SERIES, f.series.toString(), collections.firstOrNull { it.id == f.series }?.name.orEmpty()))
                }
                if (f.codec.isNotEmpty()) add(ActiveChip(ActiveFilter.CODEC, f.codec))
                if (f.subs) add(ActiveChip(ActiveFilter.SUBS, "missing"))
                if (f.watched) add(ActiveChip(ActiveFilter.WATCHED, "anyone"))
                if (query.isNotEmpty()) add(ActiveChip(ActiveFilter.SEARCH, "", query))
                if (grouped) add(ActiveChip(ActiveFilter.GROUPED, ""))
            }
        val groups =
            if (grouped) {
                val bySeries = mutableMapOf<Long, MutableList<LibraryItem>>()
                val loose = mutableListOf<LibraryItem>()
                for (it in items) {
                    val series = seriesOf(it.id).minByOrNull { s -> s.name.lowercase() }
                    if (series == null) loose += it else bySeries.getOrPut(series.id) { mutableListOf() } += it
                }
                bySeries.entries
                    .map { (id, list) -> LibraryGroup(id, collections.first { it.id == id }.name, list) }
                    .sortedBy { it.name.lowercase() } + listOfNotNull(loose.takeIf { it.isNotEmpty() }?.let { LibraryGroup(0, "", it) })
            } else {
                emptyList()
            }
        val tools = me?.libraryTools.orEmpty()
        mutableState.update {
            it.copy(
                kinds = kindsFor(me),
                musicEntry = me?.shows(Section.MUSIC) == true,
                canManage = manage,
                showsPosterState = me != null && (me.can(Need.MANAGE_LIBRARY) || me.can(Need.APPROVE)),
                tools = tools,
                addKinds = if (manage) kindsFor(me) else emptyList(),
                canOpenFolders = LibraryTool.LIBRARY_FOLDERS in tools,
                items = items,
                groups = groups,
                total = kindTitles.size,
                sorts = LibraryLogic.kindSorts.getValue(kind),
                statusOptions =
                    StatusFilter.entries.map { s ->
                        FilterOption(
                            s.wire,
                            count = count(ActiveFilter.STATUS) { t -> LibraryLogic.matches(t, s) },
                            selected =
                                f.status == s,
                        )
                    },
                keptOptions =
                    listOf(
                        FilterOption("on", count = count(ActiveFilter.KEPT) { t -> t.keepUpToDate }, selected = f.kept == KeptFilter.ON),
                        FilterOption("off", count = count(ActiveFilter.KEPT) { t -> !t.keepUpToDate }, selected = f.kept == KeptFilter.OFF),
                    ),
                askerOptions =
                    listOf(
                        FilterOption(LibraryFilter.ASKER_ANYONE, selected = f.asker == LibraryFilter.ASKER_ANYONE),
                        FilterOption(
                            LibraryFilter.ASKER_ME,
                            count = count(ActiveFilter.ASKER) { t -> peopleOf(t.id).any { p -> p.first == me?.userId } },
                            selected = f.asker == LibraryFilter.ASKER_ME,
                        ),
                    ) +
                        askers.map { (id, name) ->
                            FilterOption(
                                id.toString(),
                                name,
                                count(ActiveFilter.ASKER) { t -> peopleOf(t.id).any { p -> p.first == id } },
                                f.asker == id.toString(),
                            )
                        },
                codecOptions =
                    if (showCodecs) {
                        codecs.map { c ->
                            FilterOption(
                                c,
                                count = count(ActiveFilter.CODEC) { t -> t.mainCopy?.codec == c },
                                selected =
                                    f.codec == c,
                            )
                        }
                    } else {
                        emptyList()
                    },
                seriesOptions =
                    if (kind == LibraryKind.MOVIE && collections.isNotEmpty()) {
                        listOf(FilterOption("0", selected = f.series == 0L)) +
                            collections.sortedBy { c -> c.name.lowercase() }.map { c ->
                                FilterOption(
                                    c.id.toString(),
                                    c.name,
                                    count(ActiveFilter.SERIES) { t ->
                                        seriesOf(t.id).any { x ->
                                            x.id ==
                                                c.id
                                        }
                                    },
                                    f.series == c.id,
                                )
                            }
                    } else {
                        emptyList()
                    },
                subtitlesOption = if (subsAvailable) FilterOption("missing", count = subsCount, selected = f.subs) else null,
                watchedOption = if (watchedAvailable) FilterOption("anyone", count = watchedCount, selected = f.watched) else null,
                chips = chips,
                grouped = grouped,
                canGroup = canGroup,
                savedFilters = saved.reversed().map { s -> SavedFilterOption(s.name, s.filter == f) },
                selecting = it.selecting && manage,
                selected = selected,
                moveOptions =
                    if (manage &&
                        kindFolders.size > 1 &&
                        !me.handsOff
                    ) {
                        kindFolders.map { MenuOption(it.id, it.label.ifEmpty { it.path }) }
                    } else {
                        emptyList()
                    },
                ruleOptions =
                    rules
                        .orEmpty()
                        .filter { r -> r.kinds.isEmpty() || neededKinds.all { k -> k in r.kinds || (k == "music" && "artist" in r.kinds) } }
                        .map { r -> MenuOption(r.id, r.name) },
                canFindCopies =
                    kind == LibraryKind.MOVIE && pickedNow.any { t -> t.mainCopy?.let { c -> !c.isProtected && c.fileId > 0 } == true },
                canSetAudio = kind != LibraryKind.ARTIST,
                missingCount = kindTitles.count { t -> LibraryLogic.missing(t) },
                canSearchSubtitles = manage && f.subs && items.isNotEmpty(),
            )
        }
    }

    private companion object {
        const val MAX_QUERY = 256
        const val SEARCH_DELAY = 200L
        const val MAX_COPIES = 100
    }
}
