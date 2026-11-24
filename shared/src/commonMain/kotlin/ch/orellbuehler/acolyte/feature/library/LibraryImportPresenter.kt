package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.model.AdoptCandidate
import ch.orellbuehler.acolyte.api.model.AdoptChoice
import ch.orellbuehler.acolyte.api.model.AdoptMatch
import ch.orellbuehler.acolyte.api.model.AdoptPlan
import ch.orellbuehler.acolyte.api.model.AdoptRequest
import ch.orellbuehler.acolyte.api.model.AdoptResult
import ch.orellbuehler.acolyte.api.model.AdoptScanRequest
import ch.orellbuehler.acolyte.api.model.FolderChange
import ch.orellbuehler.acolyte.api.model.TidyApply
import ch.orellbuehler.acolyte.api.model.TidyMove
import ch.orellbuehler.acolyte.api.model.TidyResult
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer

data class ImportFolderOption(
    val id: Long,
    val kind: String,
    val label: String,
    val path: String,
)

data class ImportPick(
    val key: String,
    val name: String,
    val year: Int,
    val poster: String,
    val score: Double,
)

data class ImportRow(
    val folder: String,
    val status: String,
    val why: String,
    val pick: ImportPick?,
    val choices: List<ImportPick>,
    val shownName: String,
    val shownYear: Int,
    val ticked: Boolean,
    val canTick: Boolean,
    val titleId: Long,
    val episodes: Int,
    val files: Int,
    val unknown: Int,
    val albums: Int,
    val tracks: Int,
)

data class LookupResult(
    val key: String,
    val name: String,
    val year: Int,
    val poster: String,
    val inLibrary: Boolean,
)

data class ImportLookup(
    val folder: String,
    val term: String,
    val busy: Boolean = false,
    val results: List<LookupResult> = emptyList(),
    val nothing: Boolean = false,
    val error: ApiError? = null,
)

data class ImportPlanInfo(
    val kind: String,
    val path: String,
    val total: Int,
    val offset: Int,
    val shown: Int,
    val more: Boolean,
    val next: String,
    val warning: String,
    val partial: Boolean,
    val looseMovies: Int,
    val looseTop: Int,
    val suggestFlat: Boolean,
    val flat: Boolean,
)

data class AdoptedFolder(
    val folder: String,
    val titleId: Long,
    val error: String,
)

data class ImportOutcome(
    val adopted: Int,
    val failed: Int,
    val unknown: Int,
    val folders: List<AdoptedFolder>,
)

data class TidyInfo(
    val moves: List<TidyMove>,
    val blocked: List<TidyMove>,
    val unmatched: Int,
    val busy: Boolean,
    val changed: Boolean,
    val result: TidyResult?,
)

data class LibraryImportState(
    val status: LoadStatus = LoadStatus(),
    val folders: List<ImportFolderOption> = emptyList(),
    val folderId: Long = 0,
    val canOpenFolders: Boolean = false,
    val scanning: Boolean = false,
    val adopting: Boolean = false,
    val progress: JobProgress = JobProgress(),
    val progressDone: Int = -1,
    val progressTotal: Int = -1,
    val adoptDone: Int = 0,
    val adoptTotal: Int = 0,
    val plan: ImportPlanInfo? = null,
    val rows: List<ImportRow> = emptyList(),
    val statusOptions: List<FilterOption> = emptyList(),
    val statusFilter: String = ALL,
    val query: String = "",
    val tickedCount: Int = 0,
    val shownTickable: Int = 0,
    val follow: Boolean = true,
    val lookup: ImportLookup? = null,
    val outcome: ImportOutcome? = null,
    val undoFiles: Int = 0,
    val tidy: TidyInfo? = null,
    val error: ApiError? = null,
    val notice: LibraryNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && folders.isEmpty()
    val busy: Boolean get() = scanning || adopting

    companion object {
        const val ALL = "all"
    }
}

class LibraryImportPresenter internal constructor(
    private val api: Api,
    private val canOpenFolders: () -> Boolean,
) {
    private val mutableState = MutableStateFlow(LibraryImportState())
    private val notices = Notices()
    private var plan: AdoptPlan? = null
    private var picks: Map<String, String> = emptyMap()
    private var named: Map<String, ImportPick> = emptyMap()
    private var ticked: Set<String> = emptySet()
    private var tidyHash = ""

    val state: StateFlow<LibraryImportState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started(), canOpenFolders = canOpenFolders()) }
        when (val result = api.result(LibraryEndpoints.folders)) {
            is ApiResult.Ok -> {
                val folders =
                    result.value.libraryFolders
                        .filter { it.kind in setOf("movie", "show", "music") }
                        .map { ImportFolderOption(it.id, it.kind, it.label, it.path) }
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        folders = folders,
                        folderId = it.folderId.takeIf { id -> folders.any { f -> f.id == id } } ?: folders.firstOrNull()?.id ?: 0,
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }

    fun selectFolder(id: Long) {
        if (mutableState.value.busy) return
        plan = null
        picks = emptyMap()
        ticked = emptySet()
        mutableState.update { it.copy(folderId = id, plan = null, outcome = null, tidy = null, undoFiles = 0, error = null) }
        rebuild()
    }

    suspend fun scan() = runScan(null)

    suspend fun scanRest() = runScan(plan?.next?.takeIf { it.isNotEmpty() })

    private suspend fun runScan(after: String?) {
        val current = mutableState.value
        if (current.busy || current.folderId == 0L) return
        mutableState.update {
            it.copy(
                scanning = true,
                error = null,
                outcome = null,
                progress = JobProgress(running = true),
                progressDone = -1,
                progressTotal = -1,
            )
        }
        when (val started = api.result(LibraryEndpoints.scan, AdoptScanRequest(after), "id" to current.folderId)) {
            is ApiResult.Ok -> {
                val outcome = LibraryJobs.follow(api, started.value.jobId) { job -> progress(job.progress, job.progressNote.orEmpty()) }
                val found = LibraryJobs.result(outcome.job, "plan", AdoptPlan.serializer())
                if (found != null) {
                    plan = found
                    picks = found.folders.filter { it.status == "matched" && it.match != null }.associate { it.folder to it.match!!.key }
                    ticked = picks.keys
                    named = emptyMap()
                    mutableState.update { it.copy(scanning = false, progress = JobProgress(), statusFilter = LibraryImportState.ALL) }
                    if (found.kind == "movie") loadUndo()
                } else {
                    val error =
                        outcome.error ?: ApiError(
                            ApiErrorCode.SERVER,
                            0,
                            outcome.job
                                ?.error
                                ?.message
                                .orEmpty(),
                        )
                    mutableState.update { it.copy(scanning = false, progress = JobProgress(), error = error) }
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(scanning = false, progress = JobProgress(), error = started.error) }
        }
        rebuild()
    }

    private fun progress(
        value: Double,
        note: String,
    ) {
        val numbers = PROGRESS.find(note)
        mutableState.update {
            it.copy(
                progress = JobProgress(true, value, note),
                progressDone = numbers?.groupValues?.get(1)?.toIntOrNull() ?: -1,
                progressTotal = numbers?.groupValues?.get(2)?.toIntOrNull() ?: -1,
            )
        }
    }

    private suspend fun loadUndo() {
        val id = mutableState.value.folderId
        api.result(LibraryEndpoints.tidyPlan, "id" to id).valueOrNull()?.let { tidy ->
            mutableState.update { it.copy(undoFiles = tidy.undoFiles) }
        }
    }

    fun setFilter(status: String) {
        mutableState.update { it.copy(statusFilter = status) }
        rebuild()
    }

    fun setQuery(text: String) {
        mutableState.update { it.copy(query = text.take(200)) }
        rebuild()
    }

    fun setFollow(on: Boolean) {
        mutableState.update { it.copy(follow = on) }
    }

    fun toggle(folder: String) {
        if (picks[folder].isNullOrEmpty()) return
        ticked = if (folder in ticked) ticked - folder else ticked + folder
        rebuild()
    }

    fun selectAll() {
        ticked = ticked +
            mutableState.value.rows
                .filter { it.canTick && it.pick != null }
                .map { it.folder }
        rebuild()
    }

    fun selectNone() {
        ticked = emptySet()
        rebuild()
    }

    fun pick(
        folder: String,
        key: String,
    ) {
        picks = picks + (folder to key)
        ticked = ticked + folder
        rebuild()
    }

    fun openLookup(folder: String) {
        val candidate = plan?.folders?.firstOrNull { it.folder == folder } ?: return
        val term = candidate.parsedName + if (candidate.parsedYear > 0) " ${candidate.parsedYear}" else ""
        mutableState.update { it.copy(lookup = ImportLookup(folder, term)) }
    }

    fun closeLookup() {
        mutableState.update { it.copy(lookup = null) }
    }

    suspend fun lookup(term: String) {
        val lookup = mutableState.value.lookup ?: return
        val text = term.trim()
        if (text.isEmpty()) return
        mutableState.update { it.copy(lookup = lookup.copy(term = text, busy = true, error = null, nothing = false)) }
        val found: ApiResult<List<LookupResult>> =
            when (plan?.kind) {
                "music" -> lookupArtists(text)
                "show" ->
                    when (val r = api.result(LibraryEndpoints.lookupShows, "term" to text)) {
                        is ApiResult.Ok ->
                            ApiResult.Ok(
                                r.value.shows.mapNotNull { s ->
                                    showId(s.source, s.ids).takeIf { it.isNotEmpty() }?.let {
                                        LookupResult(
                                            it,
                                            s.name,
                                            s.year,
                                            s.poster,
                                            s.titleId > 0,
                                        )
                                    }
                                },
                            )
                        is ApiResult.Failed -> r
                    }
                else -> {
                    val match = YEAR.matchEntire(text)
                    val r =
                        if (match != null) {
                            api.result(LibraryEndpoints.lookupMovies, "term" to match.groupValues[1], "year" to match.groupValues[2])
                        } else {
                            api.result(LibraryEndpoints.lookupMovies, "term" to text)
                        }
                    when (r) {
                        is ApiResult.Ok ->
                            ApiResult.Ok(
                                r.value.movies.mapNotNull { m ->
                                    m.ids["tmdb"]?.takeIf { it.isNotEmpty() }?.let {
                                        LookupResult(
                                            it,
                                            m.name,
                                            m.year,
                                            m.poster,
                                            m.titleId > 0,
                                        )
                                    }
                                },
                            )
                        is ApiResult.Failed -> r
                    }
                }
            }
        mutableState.update { s ->
            val open = s.lookup?.takeIf { it.folder == lookup.folder } ?: return@update s
            when (found) {
                is ApiResult.Ok -> s.copy(lookup = open.copy(busy = false, results = found.value, nothing = found.value.isEmpty()))
                is ApiResult.Failed -> s.copy(lookup = open.copy(busy = false, error = found.error))
            }
        }
    }

    private suspend fun lookupArtists(text: String): ApiResult<List<LookupResult>> {
        repeat(ARTIST_TRIES) {
            when (val r = api.result(LibraryEndpoints.lookupArtists, "term" to text)) {
                is ApiResult.Ok -> {
                    if (!r.value.pending || it == ARTIST_TRIES - 1) {
                        return ApiResult.Ok(
                            r.value.artists.map { a ->
                                val name = if (a.disambiguation.isNotEmpty()) "${a.name} (${a.disambiguation})" else a.name
                                LookupResult("mbid:${a.mbid}", name, 0, a.image, a.titleId > 0)
                            },
                        )
                    }
                }
                is ApiResult.Failed -> return r
            }
            delay(ARTIST_WAIT)
        }
        return ApiResult.Ok(emptyList())
    }

    private fun showId(
        source: String,
        ids: Map<String, String>,
    ): String {
        for (k in listOf(source, "tmdb", "tvdb", "tvmaze", "imdb")) ids[k]?.takeIf { it.isNotEmpty() }?.let { return "$k:$it" }
        return ""
    }

    fun useLookup(result: LookupResult) {
        val lookup = mutableState.value.lookup ?: return
        named = named + (result.key to ImportPick(result.key, result.name, result.year, result.poster, -1.0))
        picks = picks + (lookup.folder to result.key)
        ticked = ticked + lookup.folder
        mutableState.update { it.copy(lookup = null) }
        rebuild()
    }

    suspend fun adopt() {
        val current = mutableState.value
        val found = plan ?: return
        if (current.busy) return
        val choices =
            found.folders
                .filter { it.folder in ticked && canAdopt(it) }
                .mapNotNull { c ->
                    val key = picks[c.folder]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    if (found.kind == "movie") AdoptChoice(c.folder, tmdbId = key) else AdoptChoice(c.folder, id = key)
                }
        if (choices.isEmpty()) return
        mutableState.update {
            it.copy(
                adopting = true,
                adoptDone = 0,
                adoptTotal = choices.size,
                error = null,
                progress = JobProgress(true),
            )
        }
        val results = mutableListOf<AdoptResult>()
        var unknown = 0
        var error: ApiError? = null
        for (part in parts(choices)) {
            when (val started = api.result(LibraryEndpoints.adopt, AdoptRequest(part, current.follow), "id" to current.folderId)) {
                is ApiResult.Ok -> {
                    val outcome = LibraryJobs.follow(api, started.value.jobId) { job -> progress(job.progress, job.progressNote.orEmpty()) }
                    val got = LibraryJobs.result(outcome.job, "results", ListSerializer(AdoptResult.serializer()))
                    if (got != null) results += got else unknown += part.size
                    if (outcome.job?.state == "failed" &&
                        got == null
                    ) {
                        error =
                            ApiError(
                                ApiErrorCode.SERVER,
                                0,
                                outcome.job.error
                                    ?.message
                                    .orEmpty(),
                            )
                    }
                }
                is ApiResult.Failed -> {
                    error = started.error
                    break
                }
            }
            mutableState.update { it.copy(adoptDone = it.adoptDone + part.size) }
        }
        val adopted = results.count { it.error.isEmpty() }
        val outcome =
            ImportOutcome(
                adopted = adopted,
                failed = results.count { it.error.isNotEmpty() },
                unknown = unknown,
                folders = results.map { AdoptedFolder(it.folder, it.titleId, it.error) },
            )
        val done = results.filter { it.error.isEmpty() }.map { it.folder }.toSet()
        ticked = ticked - done
        mutableState.update {
            it.copy(
                adopting = false,
                progress = JobProgress(),
                outcome = outcome,
                error = error,
                notice = if (adopted > 0) notices.make(LibraryNoticeKind.ADOPTED, count = adopted, name = found.kind) else it.notice,
            )
        }
        rebuild()
    }

    private fun parts(choices: List<AdoptChoice>): List<List<AdoptChoice>> {
        val out = mutableListOf<List<AdoptChoice>>()
        var part = mutableListOf<AdoptChoice>()
        var size = 0
        for (choice in choices) {
            val bytes = ApiJson.lenient.encodeToString(AdoptChoice.serializer(), choice).length + 1
            if (part.isNotEmpty() && (part.size >= PART_ITEMS || size + bytes > PART_BYTES)) {
                out += part
                part = mutableListOf()
                size = 0
            }
            part += choice
            size += bytes
        }
        if (part.isNotEmpty()) out += part
        return out
    }

    suspend fun keepAsFiles() {
        val id = mutableState.value.folderId
        when (val result = api.result(LibraryEndpoints.changeFolder, FolderChange(flat = true), "id" to id)) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(notice = notices.make(LibraryNoticeKind.FLAT_ON)) }
                scan()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(notice = notices.failed(result.error)) }
        }
    }

    suspend fun openTidy() {
        val id = mutableState.value.folderId
        mutableState.update { it.copy(tidy = TidyInfo(emptyList(), emptyList(), 0, busy = true, changed = false, result = null)) }
        when (val result = api.result(LibraryEndpoints.tidyPlan, "id" to id)) {
            is ApiResult.Ok -> {
                tidyHash = result.value.planHash
                mutableState.update {
                    it.copy(
                        undoFiles = result.value.undoFiles,
                        tidy = TidyInfo(result.value.moves, result.value.blocked, result.value.unmatched.size, false, false, null),
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(tidy = null, notice = notices.failed(result.error)) }
        }
    }

    suspend fun tidy() {
        val id = mutableState.value.folderId
        val info = mutableState.value.tidy ?: return
        mutableState.update { it.copy(tidy = info.copy(busy = true)) }
        when (val result = api.result(LibraryEndpoints.tidy, TidyApply(tidyHash), "id" to id)) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(
                        tidy = info.copy(busy = false, result = result.value),
                        notice = notices.make(LibraryNoticeKind.TIDIED, count = result.value.moved),
                    )
                }
                loadUndo()
            }
            is ApiResult.Failed ->
                if (result.error.code == ApiErrorCode.CONFLICT) {
                    openTidy()
                    mutableState.update { it.copy(tidy = it.tidy?.copy(changed = true)) }
                } else {
                    mutableState.update { it.copy(tidy = info.copy(busy = false), notice = notices.failed(result.error)) }
                }
        }
    }

    suspend fun undoTidy() {
        val id = mutableState.value.folderId
        when (val result = api.result(LibraryEndpoints.untidy, "id" to id)) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(
                        undoFiles = 0,
                        notice = notices.make(LibraryNoticeKind.UNTIDIED, count = result.value.moved),
                    )
                }
                scan()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(notice = notices.failed(result.error)) }
        }
    }

    suspend fun closeTidy() {
        val moved = mutableState.value.tidy?.result != null
        mutableState.update { it.copy(tidy = null) }
        if (moved) scan()
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    private fun canAdopt(c: AdoptCandidate): Boolean = c.status == "matched" || c.status == "unsure" || c.status == "unmatched"

    private fun choice(m: AdoptMatch): ImportPick = ImportPick(m.key, m.name, m.year, m.poster, m.score)

    private fun rebuild() {
        val found = plan
        val current = mutableState.value
        if (found == null) {
            mutableState.update {
                it.copy(
                    plan = null,
                    rows = emptyList(),
                    statusOptions = emptyList(),
                    tickedCount = 0,
                    shownTickable = 0,
                )
            }
            return
        }
        val words =
            current.query
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
        val rows =
            sortFolders(found.folders)
                .filter { current.statusFilter == LibraryImportState.ALL || it.status == current.statusFilter }
                .filter { c ->
                    val text =
                        listOf(
                            c.folder,
                            c.parsedName,
                            c.parsedYear.toString(),
                            c.match?.name.orEmpty(),
                            c.match
                                ?.year
                                ?.toString()
                                .orEmpty(),
                        ).joinToString(" ")
                            .lowercase()
                    words.all { text.contains(it) }
                }.map { c ->
                    val choices = listOfNotNull(c.match).plus(c.others).distinctBy { it.key }.map { choice(it) }
                    val key = picks[c.folder]
                    val pick = key?.let { k -> choices.firstOrNull { it.key == k } ?: named[k] }
                    ImportRow(
                        folder = c.folder,
                        status = c.status,
                        why = c.why,
                        pick = pick,
                        choices = choices,
                        shownName = pick?.name ?: c.parsedName.ifEmpty { c.folder.substringAfterLast('/') },
                        shownYear = pick?.year ?: c.parsedYear,
                        ticked = c.folder in ticked && pick != null && canAdopt(c),
                        canTick = canAdopt(c),
                        titleId = c.titleId,
                        episodes = c.episodes,
                        files = c.files.size,
                        unknown = c.unknown,
                        albums = c.albums.size,
                        tracks = c.tracks,
                    )
                }
        val order = listOf("matched", "unsure", "unmatched", "not_looked_up", "in_library", "never_add", "no_video")
        val counts = found.folders.groupingBy { it.status }.eachCount()
        mutableState.update {
            it.copy(
                plan =
                    ImportPlanInfo(
                        kind = found.kind,
                        path = found.path,
                        total = found.total,
                        offset = found.offset,
                        shown = found.folders.size,
                        more = found.more,
                        next = found.next,
                        warning = found.warning,
                        partial = found.partial,
                        looseMovies = found.looseMovies,
                        looseTop = found.looseTop,
                        suggestFlat = found.suggestFlat,
                        flat = found.flat,
                    ),
                rows = rows,
                statusOptions =
                    listOf(
                        FilterOption(
                            LibraryImportState.ALL,
                            count = found.folders.size,
                            selected =
                                it.statusFilter == LibraryImportState.ALL,
                        ),
                    ) +
                        order.filter { s -> (counts[s] ?: 0) > 0 }.map { s ->
                            FilterOption(
                                s,
                                count = counts[s] ?: 0,
                                selected =
                                    it.statusFilter == s,
                            )
                        },
                tickedCount = found.folders.count { c -> c.folder in ticked && canAdopt(c) && !picks[c.folder].isNullOrEmpty() },
                shownTickable = rows.count { r -> r.canTick && r.pick != null },
            )
        }
    }

    private fun sortFolders(folders: List<AdoptCandidate>): List<AdoptCandidate> {
        val order = listOf("unsure", "unmatched", "not_looked_up", "matched", "in_library", "never_add", "no_video")

        fun rank(c: AdoptCandidate) = order.indexOf(c.status).let { if (it < 0) order.size else it }

        fun title(c: AdoptCandidate) = (c.match?.name ?: c.parsedName.ifEmpty { c.folder.substringAfterLast('/') }).lowercase()
        return folders.sortedWith(compareBy<AdoptCandidate>({ rank(it) }, { title(it) }, { it.folder }))
    }

    private companion object {
        val PROGRESS = Regex("(\\d+)\\D+?(\\d+)")
        val YEAR = Regex("^(.*?)\\s+\\(?((?:19|20)\\d\\d)\\)?$")
        const val PART_ITEMS = 250
        const val PART_BYTES = 32 * 1024
        const val ARTIST_TRIES = 6
        const val ARTIST_WAIT = 1_500L
    }
}
