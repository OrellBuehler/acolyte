package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.BrowseFolder
import ch.orellbuehler.acolyte.api.model.ByHandFile
import ch.orellbuehler.acolyte.api.model.ByHandImport
import ch.orellbuehler.acolyte.api.model.ByHandItem
import ch.orellbuehler.acolyte.api.model.ByHandResult
import ch.orellbuehler.acolyte.api.model.ByHandScanRequest
import ch.orellbuehler.acolyte.api.model.EpisodeRow
import ch.orellbuehler.acolyte.api.model.FolderBrowseRequest
import ch.orellbuehler.acolyte.api.model.SearchTitle
import ch.orellbuehler.acolyte.api.model.TitleVersion
import ch.orellbuehler.acolyte.api.model.TitleVersions
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ByHandBlocker {
    NONE,
    IN_LIBRARY,
    NO_PICK,
    NO_EPISODE,
    HAS_FILE,
    NO_TRACK,
}

enum class ByHandConfidence {
    NONE,
    LIKELY,
    GUESS,
}

data class ByHandPick(
    val titleId: Long,
    val titleName: String,
    val titleKind: String,
    val year: Int,
    val versionId: Long,
    val season: Int,
    val episode: Int,
    val slotId: Long,
    val hasFile: Boolean,
    val albumId: Long,
    val albumName: String,
    val albumYear: Int,
    val disc: Int,
    val track: Int,
    val trackName: String,
)

data class ByHandRow(
    val path: String,
    val name: String,
    val size: Long,
    val quality: String,
    val kind: String,
    val pick: ByHandPick?,
    val confidence: ByHandConfidence,
    val blocker: ByHandBlocker,
    val note: String,
    val duplicate: Boolean,
    val ticked: Boolean,
    val canTick: Boolean,
    val canChange: Boolean,
    val replace: Boolean,
    val result: ByHandResult?,
)

data class BrowseState(
    val path: String = "",
    val parent: String = "",
    val folders: List<BrowseFolder> = emptyList(),
    val files: Int = 0,
    val more: Boolean = false,
    val loading: Boolean = true,
    val error: ApiError? = null,
) {
    val atRoot: Boolean get() = path.isEmpty()
}

data class EditorState(
    val path: String,
    val query: String,
    val searching: Boolean = false,
    val results: List<SearchTitle> = emptyList(),
    val nothing: Boolean = false,
    val error: ApiError? = null,
    val titleId: Long = 0,
    val titleName: String = "",
    val titleKind: String = "",
    val year: Int = 0,
    val loading: Boolean = false,
    val versions: List<TitleVersion> = emptyList(),
    val versionId: Long = 0,
    val seasons: List<Int> = emptyList(),
    val season: Int = -1,
    val episodes: List<EpisodeRow> = emptyList(),
    val episode: Int = -1,
) {
    val canUse: Boolean get() = titleId > 0 && !loading && (titleKind != "show" || (season >= 0 && episode >= 0))
}

data class LibraryFilesState(
    val allowed: Boolean = false,
    val canBrowse: Boolean = false,
    val path: String = "",
    val scanning: Boolean = false,
    val scanned: Boolean = false,
    val scanPath: String = "",
    val more: Boolean = false,
    val rows: List<ByHandRow> = emptyList(),
    val mode: String = MODE_COPY,
    val tickedCount: Int = 0,
    val replaceCount: Int = 0,
    val importing: Boolean = false,
    val browser: BrowseState? = null,
    val editor: EditorState? = null,
    val error: ApiError? = null,
    val notice: LibraryNotice? = null,
) {
    companion object {
        const val MODE_COPY = "copy"
        const val MODE_MOVE = "move"
    }
}

class LibraryFilesPresenter internal constructor(
    private val api: Api,
    account: () -> Account?,
    path: String,
) {
    private val mutableState = MutableStateFlow(LibraryFilesState(path = path))
    private val notices = Notices()
    private var files: List<ByHandFile> = emptyList()
    private var picks: Map<String, ByHandPick?> = emptyMap()
    private var changed: Set<String> = emptySet()
    private var ticked: Set<String> = emptySet()
    private var replace: Set<String> = emptySet()
    private var results: Map<String, ByHandResult> = emptyMap()
    private var editorTitle: TitleVersions? = null
    private var allEpisodes: List<EpisodeRow> = emptyList()

    val state: StateFlow<LibraryFilesState> = mutableState.asStateFlow()

    init {
        val me = account()
        mutableState.update { it.copy(allowed = me?.can(Need.SETTINGS) == true, canBrowse = me?.can(Need.USERS) == true) }
    }

    suspend fun load() {
        val current = mutableState.value
        if (!current.scanned && !current.scanning && current.path.isNotBlank()) scan()
    }

    fun setPath(path: String) {
        mutableState.update { it.copy(path = path) }
    }

    fun setMode(mode: String) {
        mutableState.update { it.copy(mode = if (mode == LibraryFilesState.MODE_MOVE) mode else LibraryFilesState.MODE_COPY) }
    }

    suspend fun scan() {
        val path = mutableState.value.path.trim()
        if (path.isEmpty() || mutableState.value.scanning) return
        mutableState.update { it.copy(scanning = true, error = null) }
        when (val result = api.result(LibraryEndpoints.byHandScan, ByHandScanRequest(path))) {
            is ApiResult.Ok -> {
                files = result.value.files
                picks = files.associate { it.path to guessPick(it) }
                changed = emptySet()
                replace = emptySet()
                results = emptyMap()
                ticked = startTicks()
                mutableState.update { it.copy(scanning = false, scanned = true, scanPath = result.value.path, more = result.value.more) }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(scanning = false, error = result.error) }
        }
        rebuild()
    }

    private fun guessPick(f: ByHandFile): ByHandPick? {
        val g = f.guess
        if (g.titleId == 0L && g.slotId == 0L) return null
        return ByHandPick(
            titleId = g.titleId,
            titleName = g.titleName,
            titleKind = g.titleKind,
            year = g.year,
            versionId = g.versionId,
            season = g.season ?: -1,
            episode = g.episode ?: -1,
            slotId = g.slotId,
            hasFile = g.hasFile,
            albumId = g.albumId,
            albumName = g.albumName,
            albumYear = g.albumYear,
            disc = g.disc,
            track = g.track,
            trackName = g.trackName,
        )
    }

    private fun blocker(f: ByHandFile): ByHandBlocker {
        if (f.inLibrary) return ByHandBlocker.IN_LIBRARY
        val pick = picks[f.path] ?: return ByHandBlocker.NO_PICK
        if (f.kind == "audio" || pick.titleKind == "artist") {
            if (pick.slotId == 0L) return ByHandBlocker.NO_TRACK
        } else if (pick.titleKind == "show" && (pick.season < 0 || pick.episode < 0)) {
            return ByHandBlocker.NO_EPISODE
        }
        if (pick.hasFile && f.path !in replace) return ByHandBlocker.HAS_FILE
        return ByHandBlocker.NONE
    }

    private fun startTicks(): Set<String> {
        val sure = files.filter { it.guess.confidence >= SURE && blocker(it) == ByHandBlocker.NONE && it.note.isEmpty() }
        val music = sure.filter { it.kind == "audio" && it.guess.slotId > 0 }
        val best =
            music
                .groupBy { it.guess.slotId }
                .values
                .map { group ->
                    group.sortedWith(compareByDescending<ByHandFile> { it.guess.confidence }.thenByDescending { it.size }).first().path
                }.toSet()
        return sure.filter { it.kind != "audio" || it.guess.slotId == 0L || it.path in best }.map { it.path }.toSet()
    }

    private fun rebuild() {
        val tickedFiles = files.filter { it.path in ticked && blocker(it) == ByHandBlocker.NONE }
        val slotsTicked = tickedFiles.filter { it.kind == "audio" }.mapNotNull { picks[it.path]?.slotId?.takeIf { s -> s > 0 } }
        val rows =
            files.map { f ->
                val pick = picks[f.path]
                val block = blocker(f)
                val isGuess = f.path !in changed
                val c = f.guess.confidence
                val slot = pick?.slotId ?: 0
                ByHandRow(
                    path = f.path,
                    name = f.name,
                    size = f.size,
                    quality = f.quality,
                    kind = f.kind,
                    pick = pick,
                    confidence =
                        when {
                            !isGuess || pick == null -> ByHandConfidence.NONE
                            c >= SURE -> ByHandConfidence.LIKELY
                            c > 0 && c < 1 -> ByHandConfidence.GUESS
                            else -> ByHandConfidence.NONE
                        },
                    blocker = block,
                    note = f.note,
                    duplicate = f.kind == "audio" && slot > 0 && f.path !in ticked && slot in slotsTicked,
                    ticked = f.path in ticked && block == ByHandBlocker.NONE,
                    canTick = block == ByHandBlocker.NONE && !mutableState.value.importing && results[f.path]?.outcome != "placed",
                    canChange = !f.inLibrary && f.kind != "audio",
                    replace = f.path in replace,
                    result = results[f.path],
                )
            }
        mutableState.update {
            it.copy(
                rows = rows,
                tickedCount = tickedFiles.size,
                replaceCount = tickedFiles.count { f -> f.path in replace },
            )
        }
    }

    fun toggle(path: String) {
        ticked = if (path in ticked) ticked - path else ticked + path
        rebuild()
    }

    fun setReplace(
        path: String,
        on: Boolean,
    ) {
        replace = if (on) replace + path else replace - path
        if (on) ticked = ticked + path
        rebuild()
    }

    suspend fun importFiles() {
        val current = mutableState.value
        if (current.importing) return
        val chosen = files.filter { it.path in ticked && blocker(it) == ByHandBlocker.NONE }
        val items =
            chosen.mapNotNull { f ->
                val pick = picks[f.path] ?: return@mapNotNull null
                val swap = if (f.path in replace) true else null
                if (f.kind == "audio" || pick.titleKind == "artist") {
                    ByHandItem(f.path, slotId = pick.slotId, mode = current.mode, replace = swap)
                } else {
                    ByHandItem(
                        f.path,
                        titleId = pick.titleId,
                        versionId = pick.versionId.takeIf { it > 0 },
                        season = pick.season.takeIf { pick.titleKind == "show" && it >= 0 },
                        episode = pick.episode.takeIf { pick.titleKind == "show" && it >= 0 },
                        mode = current.mode,
                        replace = swap,
                    )
                }
            }
        if (items.isEmpty()) return
        mutableState.update { it.copy(importing = true) }
        rebuild()
        var placed = 0
        var error: ApiError? = null
        for (part in parts(items)) {
            when (val result = api.result(LibraryEndpoints.byHandImport, ByHandImport(part))) {
                is ApiResult.Ok -> {
                    placed += result.value.placed
                    results = results + result.value.results.associateBy { it.path }
                }
                is ApiResult.Failed -> {
                    error = result.error
                    break
                }
            }
        }
        ticked = ticked - results.filterValues { it.outcome == "placed" }.keys
        mutableState.update {
            it.copy(
                importing = false,
                notice = error?.let { e -> notices.failed(e) } ?: notices.make(LibraryNoticeKind.IMPORTED, count = placed),
            )
        }
        rebuild()
    }

    private fun parts(items: List<ByHandItem>): List<List<ByHandItem>> {
        val out = mutableListOf<List<ByHandItem>>()
        var part = mutableListOf<ByHandItem>()
        var size = 0
        for (item in items) {
            val bytes = ApiJson.lenient.encodeToString(ByHandItem.serializer(), item).length + 1
            if (part.isNotEmpty() && (part.size >= PART_ITEMS || size + bytes > PART_BYTES)) {
                out += part
                part = mutableListOf()
                size = 0
            }
            part += item
            size += bytes
        }
        if (part.isNotEmpty()) out += part
        return out
    }

    suspend fun openBrowser() {
        mutableState.update { it.copy(browser = BrowseState()) }
        browse("")
    }

    suspend fun browse(path: String) {
        mutableState.update { it.copy(browser = (it.browser ?: BrowseState()).copy(loading = true, error = null)) }
        when (val result = api.result(LibraryEndpoints.browse, FolderBrowseRequest(path))) {
            is ApiResult.Ok -> {
                val b = result.value
                mutableState.update {
                    it.copy(browser = BrowseState(b.path, b.parent, b.folders, b.files, b.more, loading = false))
                }
            }
            is ApiResult.Failed ->
                mutableState.update { it.copy(browser = (it.browser ?: BrowseState()).copy(loading = false, error = result.error)) }
        }
    }

    suspend fun browseUp() {
        val browser = mutableState.value.browser ?: return
        browse(browser.parent)
    }

    suspend fun useBrowsed() {
        val browser = mutableState.value.browser ?: return
        if (browser.path.isEmpty()) return
        mutableState.update { it.copy(browser = null, path = browser.path) }
        scan()
    }

    fun closeBrowser() {
        mutableState.update { it.copy(browser = null) }
    }

    fun openEditor(path: String) {
        val f = files.firstOrNull { it.path == path } ?: return
        val pick = picks[path]
        editorTitle = null
        allEpisodes = emptyList()
        mutableState.update { it.copy(editor = EditorState(path = path, query = pick?.titleName ?: f.guess.titleName)) }
    }

    fun closeEditor() {
        mutableState.update { it.copy(editor = null) }
    }

    suspend fun editorSearch(query: String) {
        val editor = mutableState.value.editor ?: return
        val text = LibraryLogic.query(query)
        if (text.isEmpty()) return
        mutableState.update { it.copy(editor = editor.copy(query = query, searching = true, error = null, nothing = false)) }
        when (val result = api.result(LibraryEndpoints.librarySearch, "q" to text, "kind" to "all")) {
            is ApiResult.Ok -> {
                val titles = result.value.titles.filter { it.kind == "movie" || it.kind == "show" }
                updateEditor(editor.path) { it.copy(searching = false, results = titles, nothing = titles.isEmpty()) }
            }
            is ApiResult.Failed -> updateEditor(editor.path) { it.copy(searching = false, error = result.error) }
        }
    }

    suspend fun editorChoose(titleId: Long) {
        val editor = mutableState.value.editor ?: return
        val title = editor.results.firstOrNull { it.id == titleId } ?: return
        updateEditor(editor.path) {
            it.copy(
                titleId = title.id,
                titleName = title.name,
                titleKind = title.kind,
                year = title.year,
                loading = true,
                season = -1,
                episode = -1,
            )
        }
        val details = api.result(LibraryEndpoints.titleVersions, "id" to title.id).valueOrNull()
        editorTitle = details
        allEpisodes =
            if (title.kind ==
                "show"
            ) {
                api
                    .result(LibraryEndpoints.episodes, "id" to title.id)
                    .valueOrNull()
                    ?.episodes
                    .orEmpty()
            } else {
                emptyList()
            }
        val versions = details?.versions.orEmpty().sortedByDescending { it.main }
        val seasons = allEpisodes.map { it.season }.distinct().sorted()
        updateEditor(editor.path) {
            it.copy(
                loading = false,
                versions = versions,
                versionId = versions.firstOrNull()?.id ?: 0,
                seasons = seasons,
                season = if (seasons.size == 1) seasons.first() else -1,
                episodes = if (seasons.size == 1) allEpisodes.filter { e -> e.season == seasons.first() } else emptyList(),
                error = if (details == null) ApiError(ApiErrorCode.UNKNOWN) else null,
            )
        }
    }

    fun editorVersion(id: Long) {
        val editor = mutableState.value.editor ?: return
        updateEditor(editor.path) { it.copy(versionId = id) }
    }

    fun editorSeason(season: Int) {
        val editor = mutableState.value.editor ?: return
        updateEditor(editor.path) { it.copy(season = season, episode = -1, episodes = allEpisodes.filter { e -> e.season == season }) }
    }

    fun editorEpisode(episode: Int) {
        val editor = mutableState.value.editor ?: return
        updateEditor(editor.path) { it.copy(episode = episode) }
    }

    fun editorUse() {
        val editor = mutableState.value.editor ?: return
        if (!editor.canUse) return
        val hasFile =
            if (editor.titleKind == "show") {
                allEpisodes
                    .firstOrNull { it.season == editor.season && it.episode == editor.episode }
                    ?.slots
                    ?.any { (editor.versionId == 0L || it.versionId == editor.versionId) && it.fileId > 0 } == true
            } else {
                editorTitle?.slots?.any { (editor.versionId == 0L || it.versionId == editor.versionId) && it.file != null } == true
            }
        val pick =
            ByHandPick(
                titleId = editor.titleId,
                titleName = editor.titleName,
                titleKind = editor.titleKind,
                year = editor.year,
                versionId = editor.versionId,
                season = if (editor.titleKind == "show") editor.season else -1,
                episode = if (editor.titleKind == "show") editor.episode else -1,
                slotId = 0,
                hasFile = hasFile,
                albumId = 0,
                albumName = "",
                albumYear = 0,
                disc = 0,
                track = 0,
                trackName = "",
            )
        picks = picks + (editor.path to pick)
        changed = changed + editor.path
        ticked = ticked + editor.path
        mutableState.update { it.copy(editor = null) }
        rebuild()
    }

    private fun updateEditor(
        path: String,
        change: (EditorState) -> EditorState,
    ) {
        mutableState.update { s -> s.editor?.takeIf { it.path == path }?.let { s.copy(editor = change(it)) } ?: s }
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    private companion object {
        const val SURE = 0.7
        const val PART_ITEMS = 250
        const val PART_BYTES = 32 * 1024
    }
}
