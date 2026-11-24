package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.OutsideListsEndpoints
import ch.orellbuehler.acolyte.api.RequestSettingsEndpoints
import ch.orellbuehler.acolyte.api.model.AskFolder
import ch.orellbuehler.acolyte.api.model.AskPickRules
import ch.orellbuehler.acolyte.api.model.KeyBody
import ch.orellbuehler.acolyte.api.model.NeverAddEntry
import ch.orellbuehler.acolyte.api.model.NewNeverAdd
import ch.orellbuehler.acolyte.api.model.OutsideList
import ch.orellbuehler.acolyte.api.model.OutsideListDraft
import ch.orellbuehler.acolyte.api.model.SecretBody
import ch.orellbuehler.acolyte.api.model.TokenBody
import ch.orellbuehler.acolyte.api.model.TraktDeviceCode
import ch.orellbuehler.acolyte.api.model.TraktStatus
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ListKind(
    val wire: String,
) {
    TMDB("tmdb_list"),
    TRAKT("trakt_list"),
    MDBLIST("mdblist_list"),
    ;

    companion object {
        fun of(wire: String): ListKind = entries.firstOrNull { it.wire == wire } ?: TMDB
    }
}

enum class ListSeasons(
    val wire: String,
) {
    ALL("all"),
    FUTURE("future"),
    LATEST("latest"),
    FIRST("first"),
    NONE("none"),
    ;

    companion object {
        fun of(wire: String): ListSeasons = entries.firstOrNull { it.wire == wire } ?: ALL
    }
}

enum class OutsideNoteKind {
    SAVED,
    SAVED_OFF,
    READING,
    READ_FAILED,
    REMOVED,
    REMOVE_FAILED,
    TRAKT_SAVED,
    TRAKT_KEY_SAVED,
    TRAKT_SIGNED_IN,
    TRAKT_DENIED,
    TRAKT_EXPIRED,
    TRAKT_SIGNED_OUT,
    MDBLIST_SAVED,
    MDBLIST_REMOVED,
}

data class OutsideNote(
    val id: Long,
    val kind: OutsideNoteKind,
    val name: String = "",
)

data class OutsideListsState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val owner: Boolean = false,
    val lists: List<OutsideList> = emptyList(),
    val folders: List<AskFolder> = emptyList(),
    val rules: List<AskPickRules> = emptyList(),
    val editing: Long? = null,
    val draft: OutsideListDraft = OutsideListDraft(),
    val formError: ApiError? = null,
    val saving: Boolean = false,
    val busy: Set<Long> = emptySet(),
    val trakt: TraktStatus? = null,
    val traktKey: String = "",
    val traktToken: String = "",
    val mdblistKey: String = "",
    val keyBusy: Boolean = false,
    val keyError: ApiError? = null,
    val device: TraktDeviceCode? = null,
    val note: OutsideNote? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && lists.isEmpty() && editing == null

    val formOpen: Boolean get() = editing != null

    val isShowList: Boolean get() = draft.titleKind == "show"

    val draftKind: ListKind get() = ListKind.of(draft.kind)

    val draftSeasons: ListSeasons get() = ListSeasons.of(draft.seasons)

    val draftFolders: List<AskFolder> get() = folders.filter { it.kind == draft.titleKind }

    val draftRules: List<AskPickRules> get() = rules.filter { it.kinds.isEmpty() || draft.titleKind in it.kinds }

    val needsTraktKey: Boolean get() = draftKind == ListKind.TRAKT && trakt?.clientId != true && traktKey.isEmpty()

    val canSave: Boolean
        get() = !saving && draft.name.isNotBlank() && draft.ref.isNotBlank() && draftFolders.isNotEmpty() && draft.syncEveryHours in 6..168

    val hasTraktList: Boolean get() = lists.any { it.kind == ListKind.TRAKT.wire }

    val traktKeySet: Boolean get() = traktKey.isNotEmpty() || trakt?.clientId == true

    val canSignIn: Boolean get() = owner && traktKeySet && trakt?.clientSecret == true && device == null && !keyBusy
}

fun listLink(list: OutsideList): String {
    val ref = list.ref.trim()
    return when (ListKind.of(list.kind)) {
        ListKind.TMDB -> "https://www.themoviedb.org/list/$ref"
        ListKind.TRAKT ->
            when {
                ref.startsWith("chart:") -> {
                    val name = ref.removePrefix("chart:").replace('-', '/')
                    "https://trakt.tv/${if (list.titleKind == "show") "shows" else "movies"}/$name"
                }
                ref.endsWith("/watchlist") -> "https://trakt.tv/users/${ref.substringBefore('/')}/watchlist"
                ref.contains('/') -> "https://trakt.tv/users/${ref.substringBefore('/')}/lists/${ref.substringAfter('/')}"
                else -> "https://trakt.tv/users/$ref"
            }
        ListKind.MDBLIST -> "https://mdblist.com/lists/$ref"
    }
}

val TraktDeviceCode.url: String get() = verificationUrl(this)

val OutsideList.link: String get() = listLink(this)

fun verificationUrl(code: TraktDeviceCode): String =
    code.verificationUrl.takeIf { it.startsWith("https://trakt.tv") } ?: "https://trakt.tv/activate"

class OutsideListsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(OutsideListsState())
    private val lock = Mutex()
    private var noteId = 0L
    private var signInRun = 0

    val state: StateFlow<OutsideListsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(RequestPage.OUTSIDE_LISTS)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            val owner = me.role == Role.OWNER
            mutableState.update { it.copy(status = it.status.started(), owner = owner) }
            coroutineScope {
                val folders = async { api.result(RequestSettingsEndpoints.folders) }
                val rules = async { api.result(RequestSettingsEndpoints.pickRules) }
                val trakt = async { api.result(OutsideListsEndpoints.traktSignIn).valueOrNull() }
                val key = async { api.result(OutsideListsEndpoints.traktKey).valueOrNull() }
                val token = async { if (owner) api.result(OutsideListsEndpoints.traktToken).valueOrNull() else null }
                val mdblist = async { api.result(OutsideListsEndpoints.mdblistKey).valueOrNull() }
                val lists = api.result(OutsideListsEndpoints.lists)
                val folderResult = folders.await()
                val rulesResult = rules.await()
                val failure = listOf(lists, folderResult, rulesResult).filterIsInstance<ApiResult.Failed>().firstOrNull()
                if (failure != null) {
                    mutableState.update { it.copy(status = it.status.failed(failure.error)) }
                    return@coroutineScope
                }
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        lists = lists.valueOrNull()?.outsideLists.orEmpty(),
                        folders = folderResult.valueOrNull()?.libraryFolders.orEmpty(),
                        rules = rulesResult.valueOrNull()?.pickRules.orEmpty(),
                        trakt = trakt.await(),
                        traktKey = key.await()?.key.orEmpty(),
                        traktToken = token.await()?.token.orEmpty(),
                        mdblistKey = mdblist.await()?.key.orEmpty(),
                    )
                }
            }
        }
    }

    private suspend fun reloadLists() {
        api.result(OutsideListsEndpoints.lists).onSuccess { answer -> mutableState.update { it.copy(lists = answer.outsideLists) } }
    }

    fun startNew() =
        mutableState.update { s ->
            val folder = s.folders.firstOrNull { it.kind == "movie" }?.id ?: 0
            s.copy(editing = 0, draft = OutsideListDraft(libraryFolderId = folder), formError = null)
        }

    fun edit(id: Long) =
        mutableState.update { s ->
            val list = s.lists.firstOrNull { it.id == id } ?: return@update s
            s.copy(
                editing = id,
                formError = null,
                draft =
                    OutsideListDraft(
                        name = list.name,
                        kind = list.kind,
                        titleKind = list.titleKind.ifEmpty { "movie" },
                        ref = list.ref,
                        libraryFolderId = list.libraryFolderId,
                        pickRulesId = list.pickRulesId ?: 0,
                        keepUpToDate = list.keepUpToDate,
                        searchOnAdd = list.searchOnAdd,
                        seasons = list.seasons.ifEmpty { "all" },
                        newSeasons = list.newSeasons.ifEmpty { "all" },
                        enabled = list.enabled,
                        syncEveryHours = list.syncEveryHours.takeIf { it > 0 } ?: 12,
                    ),
            )
        }

    fun cancelEdit() = mutableState.update { it.copy(editing = null, formError = null) }

    fun setName(name: String) = draft { it.copy(name = name.take(80)) }

    fun setKind(kind: ListKind) = draft { it.copy(kind = kind.wire) }

    fun setTitleKind(show: Boolean) =
        mutableState.update { s ->
            val kind = if (show) "show" else "movie"
            if (s.draft.titleKind == kind) return@update s
            val folder = s.folders.firstOrNull { it.kind == kind }?.id ?: 0
            s.copy(draft = s.draft.copy(titleKind = kind, libraryFolderId = folder, pickRulesId = 0), formError = null)
        }

    fun setRef(ref: String) = draft { it.copy(ref = ref) }

    fun setFolder(id: Long) = draft { it.copy(libraryFolderId = id) }

    fun setPickRules(id: Long) = draft { it.copy(pickRulesId = id) }

    fun setSeasons(seasons: ListSeasons) =
        draft { it.copy(seasons = seasons.wire, newSeasons = if (seasons == ListSeasons.FUTURE) "all" else it.newSeasons) }

    fun setNewSeasons(on: Boolean) =
        draft {
            if (it.seasons ==
                ListSeasons.FUTURE.wire
            ) {
                it
            } else {
                it.copy(newSeasons = if (on) "all" else "none")
            }
        }

    fun setEvery(text: String) = draft { it.copy(syncEveryHours = text.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0) }

    fun setKeepUpToDate(on: Boolean) = draft { it.copy(keepUpToDate = on) }

    fun setSearchOnAdd(on: Boolean) = draft { it.copy(searchOnAdd = on) }

    fun setEnabled(on: Boolean) = draft { it.copy(enabled = on) }

    suspend fun saveList() {
        val s = mutableState.value
        val id = s.editing ?: return
        if (!s.canSave) return
        mutableState.update { it.copy(saving = true, formError = null) }
        val body = s.draft.copy(name = s.draft.name.trim(), ref = s.draft.ref.trim())
        val result =
            if (id ==
                0L
            ) {
                api.result(OutsideListsEndpoints.add, body)
            } else {
                api.result(OutsideListsEndpoints.change, body, "id" to id)
            }
        when (result) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(saving = false, editing = null) }
                note(if (result.value.enabled) OutsideNoteKind.SAVED else OutsideNoteKind.SAVED_OFF, result.value.name)
                reloadLists()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(saving = false, formError = result.error) }
        }
    }

    suspend fun sync(id: Long) {
        val list = mutableState.value.lists.firstOrNull { it.id == id } ?: return
        mutableState.update { it.copy(busy = it.busy + id) }
        api
            .result(OutsideListsEndpoints.sync, "id" to id)
            .onSuccess { note(OutsideNoteKind.READING, list.name) }
            .onFailure { note(OutsideNoteKind.READ_FAILED, list.name) }
        mutableState.update { it.copy(busy = it.busy - id) }
    }

    suspend fun remove(id: Long) {
        val list = mutableState.value.lists.firstOrNull { it.id == id } ?: return
        mutableState.update { it.copy(busy = it.busy + id) }
        api
            .result(OutsideListsEndpoints.remove, "id" to id)
            .onSuccess {
                mutableState.update { s -> s.copy(lists = s.lists.filter { it.id != id }) }
                note(OutsideNoteKind.REMOVED, list.name)
            }.onFailure { note(OutsideNoteKind.REMOVE_FAILED, list.name) }
        mutableState.update { it.copy(busy = it.busy - id) }
    }

    suspend fun saveTrakt(
        key: String,
        token: String,
        secret: String,
    ): Boolean {
        val s = mutableState.value
        val k = key.trim()
        val t = token.trim().takeIf { s.owner }.orEmpty()
        val c = secret.trim().takeIf { s.owner }.orEmpty()
        if (k.isEmpty() && t.isEmpty() && c.isEmpty()) return false
        mutableState.update { it.copy(keyBusy = true, keyError = null) }
        var error: ApiError? = null
        if (k.isNotEmpty()) {
            api
                .result(OutsideListsEndpoints.saveTraktKey, KeyBody(k))
                .onSuccess { m ->
                    mutableState.update { it.copy(traktKey = m.key) }
                }.onFailure {
                    error =
                        it
                }
        }
        if (error == null && t.isNotEmpty()) {
            api
                .result(OutsideListsEndpoints.saveTraktToken, TokenBody(t))
                .onSuccess { m ->
                    mutableState.update { it.copy(traktToken = m.token) }
                }.onFailure {
                    error = it
                }
        }
        if (error == null && c.isNotEmpty()) {
            api
                .result(OutsideListsEndpoints.saveTraktSecret, SecretBody(c))
                .onSuccess { st ->
                    mutableState.update { it.copy(trakt = st) }
                }.onFailure {
                    error =
                        it
                }
        }
        val status = api.result(OutsideListsEndpoints.traktSignIn).valueOrNull()
        mutableState.update { it.copy(keyBusy = false, keyError = error, trakt = status ?: it.trakt) }
        if (error == null) note(if (t.isNotEmpty() || c.isNotEmpty()) OutsideNoteKind.TRAKT_SAVED else OutsideNoteKind.TRAKT_KEY_SAVED)
        return error == null
    }

    suspend fun signInTrakt() {
        if (!mutableState.value.canSignIn) return
        val run = ++signInRun
        mutableState.update { it.copy(keyBusy = true, keyError = null) }
        val code =
            when (val result = api.result(OutsideListsEndpoints.startTraktSignIn)) {
                is ApiResult.Ok -> result.value
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(keyBusy = false, keyError = result.error) }
                    return
                }
            }
        mutableState.update { it.copy(keyBusy = false, device = code) }
        val every = maxOf(code.interval, 1) * 1000L
        while (run == signInRun && mutableState.value.device != null) {
            delay(every)
            if (run != signInRun) return
            when (val result = api.result(OutsideListsEndpoints.checkTraktSignIn)) {
                is ApiResult.Ok -> {
                    val check = result.value
                    when (check.state) {
                        "waiting" -> mutableState.update { it.copy(trakt = check.status) }
                        "signed_in" -> {
                            mutableState.update { it.copy(device = null, trakt = check.status) }
                            note(OutsideNoteKind.TRAKT_SIGNED_IN)
                        }
                        "denied" -> {
                            mutableState.update { it.copy(device = null, trakt = check.status) }
                            note(OutsideNoteKind.TRAKT_DENIED)
                        }
                        else -> {
                            mutableState.update { it.copy(device = null, trakt = check.status) }
                            note(OutsideNoteKind.TRAKT_EXPIRED)
                        }
                    }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(device = null, keyError = result.error) }
            }
        }
    }

    fun cancelSignIn() {
        signInRun++
        mutableState.update { it.copy(device = null) }
    }

    suspend fun signOutTrakt() {
        mutableState.update { it.copy(keyBusy = true, keyError = null) }
        api
            .result(OutsideListsEndpoints.endTraktSignIn)
            .onSuccess { st ->
                mutableState.update { it.copy(keyBusy = false, trakt = st) }
                note(OutsideNoteKind.TRAKT_SIGNED_OUT)
            }.onFailure { error -> mutableState.update { it.copy(keyBusy = false, keyError = error) } }
    }

    suspend fun saveMdblist(key: String): Boolean {
        val k = key.trim()
        if (k.isEmpty() && mutableState.value.mdblistKey.isEmpty()) return false
        mutableState.update { it.copy(keyBusy = true, keyError = null) }
        return when (val result = api.result(OutsideListsEndpoints.saveMdblistKey, KeyBody(k))) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(keyBusy = false, mdblistKey = result.value.key) }
                note(if (k.isEmpty()) OutsideNoteKind.MDBLIST_REMOVED else OutsideNoteKind.MDBLIST_SAVED)
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(keyBusy = false, keyError = result.error) }
                false
            }
        }
    }

    fun dismissNote() = mutableState.update { it.copy(note = null) }

    suspend fun watch() {
        api.live.collect { event ->
            when (event.topic) {
                OUTSIDE_TOPIC -> reloadLists()
                LiveEvent.RESYNC -> refresh()
            }
        }
    }

    private fun draft(transform: (OutsideListDraft) -> OutsideListDraft) =
        mutableState.update {
            it.copy(draft = transform(it.draft), formError = null)
        }

    private fun note(
        kind: OutsideNoteKind,
        name: String = "",
    ) {
        noteId++
        mutableState.update { it.copy(note = OutsideNote(noteId, kind, name)) }
    }

    private companion object {
        const val OUTSIDE_TOPIC = "outside_list"
    }
}

data class NeverAddState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val entries: List<NeverAddEntry> = emptyList(),
    val busy: Boolean = false,
    val removing: Set<Long> = emptySet(),
    val error: ApiError? = null,
    val missing: Boolean = false,
    val added: Int = 0,
    val removed: String = "",
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && entries.isEmpty()
}

val NeverAddEntry.tmdb: String get() = ids["tmdb"].orEmpty()

class NeverAddPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(NeverAddState())
    private val lock = Mutex()

    val state: StateFlow<NeverAddState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(RequestPage.NEVER_ADD)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update { it.copy(status = it.status.started()) }
            api
                .result(OutsideListsEndpoints.neverAdd)
                .onSuccess { answer -> mutableState.update { it.copy(status = LoadStatus.ready, entries = answer.neverAdd) } }
                .onFailure { error -> mutableState.update { it.copy(status = it.status.failed(error)) } }
        }
    }

    suspend fun add(
        show: Boolean,
        tmdb: String,
        name: String,
        reason: String,
    ): Boolean {
        val id = tmdb.trim().removePrefix("tmdb:").trim()
        if (id.isEmpty() || id.any { !it.isDigit() } || name.isBlank()) {
            mutableState.update { it.copy(missing = true, error = null) }
            return false
        }
        mutableState.update { it.copy(busy = true, missing = false, error = null) }
        val body = NewNeverAdd(if (show) "show" else "movie", id, name.trim().take(200), reason.trim().take(200).ifEmpty { null })
        return when (val result = api.result(OutsideListsEndpoints.addNeverAdd, body)) {
            is ApiResult.Ok -> {
                mutableState.update { s ->
                    s.copy(
                        busy = false,
                        entries =
                            listOf(result.value) + s.entries.filter { it.id != result.value.id },
                        added = s.added + 1,
                    )
                }
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(busy = false, error = result.error) }
                false
            }
        }
    }

    suspend fun remove(id: Long) {
        val entry = mutableState.value.entries.firstOrNull { it.id == id } ?: return
        mutableState.update { it.copy(removing = it.removing + id, error = null) }
        when (val result = api.result(OutsideListsEndpoints.removeNeverAdd, "id" to id)) {
            is ApiResult.Ok ->
                mutableState.update { s ->
                    s.copy(entries = s.entries.filter { it.id != id }, removing = s.removing - id, removed = entry.name)
                }
            is ApiResult.Failed -> mutableState.update { it.copy(removing = it.removing - id, error = result.error) }
        }
    }
}
