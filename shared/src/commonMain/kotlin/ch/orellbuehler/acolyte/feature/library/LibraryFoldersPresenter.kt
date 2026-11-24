package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.FolderChange
import ch.orellbuehler.acolyte.api.model.FolderInput
import ch.orellbuehler.acolyte.api.model.FolderPath
import ch.orellbuehler.acolyte.api.model.HandsOff
import ch.orellbuehler.acolyte.api.model.ImportFolders
import ch.orellbuehler.acolyte.api.model.ImportFoldersChange
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.SideFiles
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class FolderHealth {
    OK,
    MISSING,
    READ_ONLY,
    NO_HARDLINK,
    COPIES_EXPECTED,
    LOOK_ONLY,
    UNKNOWN,
}

data class FolderRow(
    val folder: LibraryFolder,
    val health: FolderHealth,
    val tone: Tone,
    val busy: Boolean,
    val rulesName: String,
    val rulesIsDefault: Boolean,
    val shortPath: String,
)

data class FolderGroup(
    val kind: String,
    val folders: List<FolderRow>,
)

data class LibraryFoldersState(
    val status: LoadStatus = LoadStatus(),
    val groups: List<FolderGroup> = emptyList(),
    val rules: List<PickRule>? = null,
    val canEdit: Boolean = false,
    val isOwner: Boolean = false,
    val handsOff: Boolean = false,
    val handsOffBusy: Boolean = false,
    val needsPassword: Boolean = false,
    val passwordError: ApiError? = null,
    val canSaveImportFolders: Boolean = false,
    val importFolders: ImportFolders? = null,
    val sideFiles: SideFiles? = null,
    val tagSuggestions: List<String> = emptyList(),
    val adding: Boolean = false,
    val notice: LibraryNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && groups.isEmpty()
    val total: Int get() = groups.sumOf { it.folders.size }
}

class LibraryFoldersPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val reauth: suspend (String, String?) -> ApiError?,
    private val refreshSession: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(LibraryFoldersState())
    private val lock = Mutex()
    private val notices = Notices()
    private var folders: List<LibraryFolder> = emptyList()
    private var busy: Set<Long> = emptySet()
    private var pendingHandsOff: Boolean? = null

    val state: StateFlow<LibraryFoldersState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    canEdit = me?.can(Need.SETTINGS) == true,
                    isOwner = me?.can(Need.OWNER) == true,
                    handsOff = me?.handsOff == true,
                    canSaveImportFolders = me?.can(Need.USERS) == true,
                )
            }
            coroutineScope {
                val rulesCall = async { api.result(LibraryEndpoints.pickRules).valueOrNull()?.pickRules }
                val importCall = async { api.result(LibraryEndpoints.importFolders).valueOrNull() }
                val sideCall = async { api.result(LibraryEndpoints.sideFiles).valueOrNull() }
                val tagsCall =
                    async {
                        api
                            .result(LibraryEndpoints.tags)
                            .valueOrNull()
                            ?.tags
                            ?.map { it.label }
                            .orEmpty()
                    }
                when (val result = api.result(LibraryEndpoints.folders)) {
                    is ApiResult.Ok -> {
                        folders = result.value.libraryFolders
                        mutableState.update { it.copy(status = LoadStatus.ready) }
                    }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                }
                val rules = rulesCall.await()
                val imports = importCall.await()
                val side = sideCall.await()
                val tags = tagsCall.await()
                mutableState.update { it.copy(rules = rules, importFolders = imports, sideFiles = side, tagSuggestions = tags) }
            }
            rebuild()
        }
    }

    private fun health(f: LibraryFolder): Pair<FolderHealth, Tone> =
        when {
            f.lookOnly && f.health == "missing" -> FolderHealth.MISSING to Tone.FAIL
            f.lookOnly -> FolderHealth.LOOK_ONLY to Tone.OK
            f.health == "ok" -> FolderHealth.OK to Tone.OK
            f.health == "missing" -> FolderHealth.MISSING to Tone.FAIL
            f.health == "read_only" -> FolderHealth.READ_ONLY to Tone.FAIL
            f.health == "no_hardlink" && f.copiesExpected -> FolderHealth.COPIES_EXPECTED to Tone.OK
            f.health == "no_hardlink" -> FolderHealth.NO_HARDLINK to Tone.WAIT
            else -> FolderHealth.UNKNOWN to Tone.WAIT
        }

    private fun rebuild() {
        val rules = mutableState.value.rules.orEmpty()
        val groups =
            listOf("movie", "show", "music").mapNotNull { kind ->
                val list = folders.filter { it.kind == kind }
                if (list.isEmpty()) return@mapNotNull null
                FolderGroup(
                    kind,
                    list.map { f ->
                        val (health, tone) = health(f)
                        val own = f.defaultPickRulesId?.takeIf { it > 0 }?.let { id -> rules.firstOrNull { it.id == id }?.name }
                        FolderRow(f, health, tone, f.id in busy, own.orEmpty(), own == null, LibraryLogic.shortPath(f.path))
                    },
                )
            }
        mutableState.update { it.copy(groups = groups) }
    }

    fun rulesFor(kind: String): List<PickRule> {
        val wanted = if (kind == "music") setOf("music", "artist") else setOf(kind)
        return mutableState.value.rules
            .orEmpty()
            .filter { r -> r.kinds.any { it in wanted } }
    }

    private fun fail(error: ApiError): LibraryNotice =
        if (error.code == ApiErrorCode.BAD_REQUEST && error.message.contains("does not exist")) {
            notices.make(LibraryNoticeKind.FAILED, name = "missing", error = error)
        } else {
            notices.failed(error)
        }

    suspend fun add(
        kind: String,
        path: String,
        label: String,
        rulesId: Long,
        tags: List<String>,
        onlyTagged: Boolean,
        lookOnly: Boolean,
        flat: Boolean,
    ): Boolean {
        val clean = path.trim()
        if (clean.isEmpty()) return false
        mutableState.update { it.copy(adding = true) }
        val cleanTags = LibraryLogic.cleanTags(tags)
        val input =
            FolderInput(
                path = clean,
                kind = kind,
                label = label.trim().take(MAX_LABEL).ifEmpty { null },
                defaultPickRulesId = rulesId.takeIf { it > 0 },
                titleTags = cleanTags.ifEmpty { null },
                onlyTaggedSources = if (cleanTags.isNotEmpty() && onlyTagged) true else null,
                lookOnly = if (lookOnly) true else null,
                flat = if (flat && kind == "movie") true else null,
            )
        return when (val result = api.result(LibraryEndpoints.addFolder, input)) {
            is ApiResult.Ok -> {
                folders = folders + result.value
                mutableState.update {
                    it.copy(adding = false, notice = notices.make(LibraryNoticeKind.FOLDER_ADDED, name = result.value.health))
                }
                rebuild()
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(adding = false, notice = fail(result.error)) }
                false
            }
        }
    }

    private suspend fun change(
        id: Long,
        body: FolderChange,
        notice: LibraryNoticeKind? = null,
    ): Boolean {
        busy = busy + id
        rebuild()
        val result = api.result(LibraryEndpoints.changeFolder, body, "id" to id)
        busy = busy - id
        return when (result) {
            is ApiResult.Ok -> {
                replace(result.value)
                notice?.let { n -> mutableState.update { it.copy(notice = notices.make(n)) } }
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(notice = fail(result.error)) }
                api.result(LibraryEndpoints.folders).valueOrNull()?.let { folders = it.libraryFolders }
                rebuild()
                false
            }
        }
    }

    private fun replace(folder: LibraryFolder) {
        folders = folders.map { if (it.id == folder.id) folder else it }
        rebuild()
    }

    suspend fun check(id: Long) {
        busy = busy + id
        rebuild()
        val result = api.result(LibraryEndpoints.checkFolder, "id" to id)
        busy = busy - id
        when (result) {
            is ApiResult.Ok -> {
                replace(result.value)
                mutableState.update { it.copy(notice = notices.make(LibraryNoticeKind.FOLDER_CHECKED, name = result.value.health)) }
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(notice = fail(result.error)) }
                rebuild()
            }
        }
    }

    suspend fun rename(
        id: Long,
        label: String,
    ) {
        val clean = label.trim().take(MAX_LABEL)
        if (clean.isEmpty()) return
        change(id, FolderChange(label = clean))
    }

    suspend fun setRules(
        id: Long,
        rulesId: Long,
    ) {
        folders = folders.map { if (it.id == id) it.copy(defaultPickRulesId = rulesId) else it }
        change(id, FolderChange(defaultPickRulesId = rulesId))
    }

    suspend fun setTags(
        id: Long,
        tags: List<String>,
        onlyTagged: Boolean,
    ) {
        val clean = LibraryLogic.cleanTags(tags)
        change(id, FolderChange(titleTags = clean, onlyTaggedSources = onlyTagged && clean.isNotEmpty()))
    }

    suspend fun setAudio(
        id: Long,
        text: String,
    ) {
        change(id, FolderChange(titleAudio = LibraryLogic.audioCodes(text)))
    }

    suspend fun setCopiesExpected(
        id: Long,
        on: Boolean,
    ) {
        if (change(id, FolderChange(copiesExpected = on))) check(id)
    }

    suspend fun setLookOnly(
        id: Long,
        on: Boolean,
    ) {
        if (change(id, FolderChange(lookOnly = on))) check(id)
    }

    suspend fun setFlat(
        id: Long,
        on: Boolean,
    ) {
        change(id, FolderChange(flat = on))
    }

    suspend fun changePath(
        id: Long,
        path: String,
    ): Boolean {
        val clean = path.trim()
        if (clean.isEmpty()) return false
        busy = busy + id
        rebuild()
        val result = api.result(LibraryEndpoints.moveFolder, FolderPath(clean), "id" to id)
        busy = busy - id
        return when (result) {
            is ApiResult.Ok -> {
                replace(result.value)
                mutableState.update { it.copy(notice = notices.make(LibraryNoticeKind.PATH_CHANGED)) }
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(notice = fail(result.error)) }
                rebuild()
                false
            }
        }
    }

    suspend fun remove(id: Long) {
        busy = busy + id
        rebuild()
        val result = api.result(LibraryEndpoints.removeFolder, "id" to id)
        busy = busy - id
        when (result) {
            is ApiResult.Ok -> {
                folders = folders.filter { it.id != id }
                mutableState.update { it.copy(notice = notices.make(LibraryNoticeKind.FOLDER_REMOVED)) }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(notice = fail(result.error)) }
        }
        rebuild()
    }

    suspend fun saveImportFolders(
        extra: String,
        watch: String,
    ) {
        fun lines(text: String) =
            text
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        when (val result = api.result(LibraryEndpoints.saveImportFolders, ImportFoldersChange(lines(extra), lines(watch)))) {
            is ApiResult.Ok ->
                mutableState.update { it.copy(importFolders = result.value, notice = notices.make(LibraryNoticeKind.IMPORT_FOLDERS_SAVED)) }
            is ApiResult.Failed -> mutableState.update { it.copy(notice = fail(result.error)) }
        }
    }

    suspend fun saveSideFiles(
        on: Boolean,
        types: String,
    ) {
        val list =
            types
                .split(Regex("[\\s,]+"))
                .map { it.trim().trimStart('.').lowercase() }
                .filter { it.isNotEmpty() }
                .distinct()
        when (val result = api.result(LibraryEndpoints.saveSideFiles, SideFiles(on, list))) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        sideFiles = result.value,
                        notice = notices.make(LibraryNoticeKind.SIDE_FILES_SAVED),
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(notice = fail(result.error)) }
        }
    }

    suspend fun setHandsOff(on: Boolean) {
        mutableState.update { it.copy(handsOffBusy = true) }
        when (val result = api.result(LibraryEndpoints.handsOff, HandsOff(on))) {
            is ApiResult.Ok -> {
                pendingHandsOff = null
                refreshSession()
                mutableState.update {
                    it.copy(
                        handsOffBusy = false,
                        handsOff = result.value.on,
                        needsPassword = false,
                        notice = notices.make(if (result.value.on) LibraryNoticeKind.HANDS_OFF_ON else LibraryNoticeKind.HANDS_OFF_OFF),
                    )
                }
            }
            is ApiResult.Failed ->
                if (result.error.code == ApiErrorCode.REAUTH) {
                    pendingHandsOff = on
                    mutableState.update { it.copy(handsOffBusy = false, needsPassword = true, passwordError = null) }
                } else {
                    mutableState.update { it.copy(handsOffBusy = false, notice = fail(result.error)) }
                }
        }
    }

    suspend fun confirmPassword(
        password: String,
        code: String,
    ) {
        val pending = pendingHandsOff ?: return
        mutableState.update { it.copy(handsOffBusy = true, passwordError = null) }
        val error = reauth(password, code.trim().ifEmpty { null })
        if (error != null) {
            mutableState.update { it.copy(handsOffBusy = false, passwordError = error) }
            return
        }
        mutableState.update { it.copy(needsPassword = false) }
        setHandsOff(pending)
    }

    fun cancelPassword() {
        pendingHandsOff = null
        mutableState.update { it.copy(needsPassword = false, passwordError = null) }
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    private companion object {
        const val MAX_LABEL = 64
    }
}
