package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.AddMovieBody
import ch.orellbuehler.acolyte.api.model.AddShowBody
import ch.orellbuehler.acolyte.api.model.CheckPickBody
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.LookupShow
import ch.orellbuehler.acolyte.api.model.NewAsk
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.SearchBody
import ch.orellbuehler.acolyte.api.model.TitleAddDefaults
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.api.model.TitleRelease
import ch.orellbuehler.acolyte.api.model.VersionInput
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

enum class BatchLine {
    WAITING,
    DONE,
    ALREADY,
    FAILED,
}

data class BatchItem(
    val card: TitleCard,
    val line: BatchLine = BatchLine.WAITING,
    val error: ApiError? = null,
)

data class AddTitleState(
    val kind: String,
    val name: String,
    val manage: Boolean,
    val handsOff: Boolean,
    val choose: Boolean = false,
    val status: LoadStatus = LoadStatus(),
    val defaultsLate: Boolean = false,
    val folders: List<LibraryFolder> = emptyList(),
    val rules: List<PickRule> = emptyList(),
    val folderId: Long = 0,
    val rulesId: Long = 0,
    val gate: String = "released",
    val keepUpToDate: Boolean = true,
    val searchOnAdd: Boolean = true,
    val secondOn: Boolean = false,
    val secondLabel: String = "4K",
    val secondRulesId: Long = 0,
    val secondFolderId: Long = 0,
    val follow: Boolean = true,
    val seasons: String = "all",
    val newSeasons: String = "all",
    val order: String = "aired",
    val source: String = "tmdb",
    val submitting: Boolean = false,
    val error: ApiError? = null,
    val addedId: Long = 0,
    val openPick: Boolean = false,
    val batch: List<BatchItem> = emptyList(),
    val batchDone: Boolean = false,
    val feedback: Feedback? = null,
) {
    val isShow: Boolean get() = kind == "show"

    val isBatch: Boolean get() = batch.isNotEmpty()

    val noFolder: Boolean get() = folders.isEmpty() && !handsOff

    val secondProblem: LabelProblem? get() = if (secondOn) TitleLogic.labelProblem(secondLabel, emptyList()) else null

    val canSubmit: Boolean
        get() =
            !submitting &&
                status.phase == ch.orellbuehler.acolyte.feature.Phase.READY &&
                (!manage || !noFolder) &&
                secondProblem == null

    val sameFolder: Boolean get() = secondOn && secondFolderId == folderId

    val newSeasonsLocked: Boolean get() = seasons == "future"
}

class AddTitlePresenter internal constructor(
    private val api: Api,
    kind: String,
    name: String,
    private val tmdb: String,
    private val show: LookupShow?,
    manage: Boolean,
    handsOff: Boolean,
    choose: Boolean,
    batch: List<TitleCard>,
    private val checkId: Long,
    private val release: TitleRelease?,
    private val onAdded: suspend (Long) -> Unit,
) {
    private val mutableState =
        MutableStateFlow(
            AddTitleState(
                kind = kind,
                name = name,
                manage = manage,
                handsOff = handsOff,
                choose = choose,
                searchOnAdd = kind != "show",
                batch = batch.map { BatchItem(it) },
                status = if (manage) LoadStatus() else LoadStatus.ready,
            ),
        )
    private val feedback = FeedbackCounter()
    private var defaults: TitleAddDefaults? = null

    val state: StateFlow<AddTitleState> = mutableState.asStateFlow()

    private val current: AddTitleState get() = mutableState.value

    suspend fun load() {
        if (!current.manage) return
        mutableState.update { it.copy(status = it.status.started()) }
        coroutineScope {
            val folders = async { api.result(LibraryEndpoints.folders) }
            val rules = async { api.result(LibraryEndpoints.pickRules) }
            val saved = async { withTimeoutOrNull(DEFAULTS_WAIT_MILLIS) { api.result(TitleSupportEndpoints.defaults) } }
            val gate = async { if (current.isShow) null else api.result(TitleSupportEndpoints.gate).valueOrNull() }
            val showDefaults = async { if (current.isShow) api.result(TitleSupportEndpoints.showDefaults).valueOrNull() else null }
            val f = folders.await()
            val r = rules.await()
            val error = (f as? ApiResult.Failed)?.error ?: (r as? ApiResult.Failed)?.error
            if (error != null) {
                mutableState.update { it.copy(status = it.status.failed(error)) }
                return@coroutineScope
            }
            val savedResult = saved.await()
            defaults = savedResult?.valueOrNull()
            val kind = current.kind
            val kindFolders = TitleLogic.foldersFor(f.valueOrNull()?.libraryFolders.orEmpty(), kind)
            val kindRules = TitleLogic.rulesFor(r.valueOrNull()?.pickRules.orEmpty(), kind)
            val kindDefaults = if (current.isShow) defaults?.show else defaults?.movie
            val start = TitleLogic.startChoices(kindDefaults, kindFolders, kindRules, kind, builtInSearch = !current.isShow)
            val second = TitleLogic.secondVersion(emptyList(), kindRules, kindFolders, start.folderId)
            val sd = showDefaults.await()
            mutableState.update {
                it.copy(
                    status = LoadStatus.ready,
                    defaultsLate = savedResult == null,
                    folders = kindFolders,
                    rules = kindRules,
                    folderId = start.folderId,
                    rulesId = start.rulesId,
                    keepUpToDate = start.keepUpToDate,
                    searchOnAdd = if (it.choose) false else start.searchOnAdd,
                    gate = rememberedGate ?: gate.await()?.releaseGate?.ifBlank { null } ?: "released",
                    secondLabel = second.label.ifEmpty { "4K" },
                    secondRulesId = second.rulesId,
                    secondFolderId = second.folderId,
                    seasons = sd?.seasonChoice?.ifBlank { null } ?: "all",
                    newSeasons = sd?.newSeasons?.ifBlank { null } ?: "all",
                    order = sd?.episodeOrder?.ifBlank { null } ?: "aired",
                    source = show?.source?.ifBlank { null } ?: sd?.detailsSource?.ifBlank { null } ?: "tmdb",
                )
            }
        }
    }

    fun setFolder(id: Long) {
        val folder = current.folders.firstOrNull { it.id == id }
        val kindDefaults = if (current.isShow) defaults?.show else defaults?.movie
        mutableState.update { it.copy(folderId = id, rulesId = TitleLogic.rulesForFolder(folder, kindDefaults, it.rules)) }
    }

    fun setRules(id: Long) = mutableState.update { it.copy(rulesId = id) }

    fun setGate(gate: String) = mutableState.update { it.copy(gate = gate) }

    fun setKeep(on: Boolean) = mutableState.update { it.copy(keepUpToDate = on) }

    fun setSearch(on: Boolean) = mutableState.update { it.copy(searchOnAdd = on) }

    fun setFollow(on: Boolean) = mutableState.update { it.copy(follow = on) }

    fun setSecond(on: Boolean) = mutableState.update { it.copy(secondOn = on) }

    fun setSecondLabel(label: String) = mutableState.update { it.copy(secondLabel = label) }

    fun setSecondRules(id: Long) = mutableState.update { it.copy(secondRulesId = id) }

    fun setSecondFolder(id: Long) = mutableState.update { it.copy(secondFolderId = id) }

    fun setSeasons(choice: String) =
        mutableState.update { it.copy(seasons = choice, newSeasons = if (choice == "future") "all" else it.newSeasons) }

    fun setNewSeasons(choice: String) = mutableState.update { it.copy(newSeasons = choice) }

    fun setOrder(order: String) = mutableState.update { it.copy(order = order) }

    fun setSource(source: String) = mutableState.update { it.copy(source = source) }

    suspend fun submit() {
        val s = current
        if (!s.canSubmit) return
        mutableState.update { it.copy(submitting = true, error = null) }
        if (s.isBatch) {
            runBatch()
            return
        }
        if (!s.isShow) rememberedGate = s.gate
        val result = addOne(if (s.isShow) showId() else "tmdb:$tmdb", s)
        when (result) {
            is ApiResult.Ok -> {
                val id = result.value
                var openPick = false
                if (s.choose) {
                    val picked = release
                    if (picked != null && checkId > 0) {
                        val pick = api.result(TitleEndpoints.checkPick, CheckPickBody(picked.id, id, picked.needsForce), "id" to checkId)
                        if (pick is ApiResult.Failed) {
                            openPick = true
                            api.result(LibraryEndpoints.search, SearchBody(choose = false), "id" to id)
                            mutableState.update { it.copy(feedback = feedback.next(FeedbackKind.NOT_SENT, pick.error.message)) }
                        } else {
                            val c = (pick as ApiResult.Ok).value
                            val note =
                                when {
                                    c.notSent.isNotEmpty() -> feedback.next(FeedbackKind.NOT_SENT, c.notSent)
                                    c.sending -> feedback.next(FeedbackKind.SENDING, c.warnings.firstOrNull().orEmpty(), c.warnings.size)
                                    else -> feedback.next(FeedbackKind.SENT, c.warnings.firstOrNull().orEmpty(), c.warnings.size)
                                }
                            mutableState.update { it.copy(feedback = note) }
                        }
                    } else {
                        openPick = true
                        api.result(LibraryEndpoints.search, SearchBody(choose = false), "id" to id)
                    }
                }
                mutableState.update {
                    it.copy(
                        submitting = false,
                        addedId = id,
                        openPick = openPick,
                        feedback = it.feedback ?: feedback.next(FeedbackKind.ADDED, s.name),
                    )
                }
                onAdded(id)
            }
            is ApiResult.Failed -> mutableState.update { it.copy(submitting = false, error = result.error) }
        }
    }

    private suspend fun addOne(
        id: String,
        s: AddTitleState,
    ): ApiResult<Long> {
        val more =
            if (s.secondOn) {
                listOf(
                    VersionInput(
                        label = s.secondLabel.trim(),
                        pickRulesId = s.secondRulesId.takeIf { it > 0 },
                        libraryFolderId =
                            s.secondFolderId.takeIf {
                                it >
                                    0
                            },
                    ),
                )
            } else {
                emptyList()
            }
        val result =
            if (s.isShow) {
                api.result(
                    TitleEndpoints.addShow,
                    AddShowBody(
                        id = id,
                        libraryFolderId = s.folderId,
                        pickRulesId = s.rulesId,
                        keepUpToDate = s.keepUpToDate,
                        searchOnAdd = s.searchOnAdd,
                        seasons = s.seasons,
                        newSeasons = if (s.seasons == "future") "all" else s.newSeasons,
                        episodeOrder = s.order,
                        detailsSource = s.source,
                        moreVersions = more,
                        follow = s.follow,
                    ),
                )
            } else {
                api.result(
                    TitleEndpoints.addMovie,
                    AddMovieBody(
                        id = id,
                        libraryFolderId = s.folderId,
                        pickRulesId = s.rulesId,
                        keepUpToDate = s.keepUpToDate,
                        releaseGate = s.gate,
                        searchOnAdd = if (s.choose) false else s.searchOnAdd,
                        moreVersions = more,
                        follow = s.follow,
                    ),
                )
            }
        return when (result) {
            is ApiResult.Ok -> ApiResult.Ok(result.value.title.id)
            is ApiResult.Failed -> result
        }
    }

    private suspend fun runBatch() {
        val s = current
        for ((index, item) in s.batch.withIndex()) {
            if (item.line == BatchLine.DONE || item.line == BatchLine.ALREADY) continue
            val id = "tmdb:${item.card.tmdbId}"
            val line: Pair<BatchLine, ApiError?> =
                if (s.manage) {
                    when (val result = addOne(id, s.copy(secondOn = false))) {
                        is ApiResult.Ok -> BatchLine.DONE to null
                        is ApiResult.Failed ->
                            if (result.error.code ==
                                ApiErrorCode.CONFLICT
                            ) {
                                BatchLine.ALREADY to result.error
                            } else {
                                BatchLine.FAILED to result.error
                            }
                    }
                } else {
                    when (val result = api.result(RequestsEndpoints.make, NewAsk(id = id, kind = "movie"))) {
                        is ApiResult.Ok -> (if (result.value.already) BatchLine.ALREADY else BatchLine.DONE) to null
                        is ApiResult.Failed -> BatchLine.FAILED to result.error
                    }
                }
            mutableState.update { st ->
                st.copy(batch = st.batch.mapIndexed { i, b -> if (i == index) b.copy(line = line.first, error = line.second) else b })
            }
        }
        val done = current.batch.count { it.line == BatchLine.DONE }
        mutableState.update {
            it.copy(submitting = false, batchDone = true, feedback = feedback.next(FeedbackKind.ADDED, s.name, done))
        }
        onAdded(0)
    }

    private fun showId(): String {
        val ids = show?.ids.orEmpty()
        val source = show?.source.orEmpty()
        ids[source]?.takeIf { it.isNotBlank() }?.let { return "$source:$it" }
        for (key in listOf("tmdb", "tvdb", "tvmaze", "imdb")) {
            ids[key]?.takeIf { it.isNotBlank() }?.let { return "$key:$it" }
        }
        return "tmdb:$tmdb"
    }

    internal companion object {
        const val DEFAULTS_WAIT_MILLIS = 3_000L
        var rememberedGate: String? = null
    }
}
