package ch.orellbuehler.acolyte.feature.settings.mediaservers

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.SettingsMediaServersEndpoints
import ch.orellbuehler.acolyte.api.deviceLanguage
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersItem
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersLabels
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersPlexItem
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersTested
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersTracks
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersWatch
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsMediaServersPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
    private val language: () -> String = { deviceLanguage().substringBefore('-') },
) {
    private val mutableState = MutableStateFlow(SettingsMediaServersState())
    private val lock = Mutex()
    private var items: List<SettingsMediaServersItem> = emptyList()
    private var plexItems: List<SettingsMediaServersPlexItem> = emptyList()
    private var results: Map<Long, SettingsMediaServersTag> = emptyMap()
    private var testing: Set<Long> = emptySet()
    private var removing: Set<Long> = emptySet()
    private var noticeSeq = 0L

    val state: StateFlow<SettingsMediaServersState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.allows(Need.SETTINGS, Part.CORE)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            val owner = me.can(Need.OWNER)
            val media = me.isOn(Part.MEDIA)
            val subtitles = me.isOn(Part.SUBTITLES)
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    allowed = true,
                    isOwner = owner,
                    mediaOn = media,
                    subtitlesOn = subtitles,
                    canPickPlex = owner && me.method != SignInMethod.API_KEY,
                )
            }
            coroutineScope {
                val ownerCards = owner && media
                val watch = async { if (ownerCards) api.result(SettingsMediaServersEndpoints.watchHistory).valueOrNull() else null }
                val labels = async { if (ownerCards) api.result(SettingsMediaServersEndpoints.labels).valueOrNull() else null }
                val tracks =
                    async { if (ownerCards && subtitles) api.result(SettingsMediaServersEndpoints.tracks).valueOrNull() else null }
                val languages =
                    async {
                        if (ownerCards && subtitles) api.result(LibraryEndpoints.subtitleLanguages).valueOrNull()?.languages else null
                    }
                val failure = loadServers()
                mutableState.update { it.copy(status = failure?.let { error -> it.status.failed(error) } ?: LoadStatus.ready) }
                val savedTracks = if (languages.await() == null) null else tracks.await()
                val savedLabels = labels.await()
                val savedWatch = watch.await()
                mutableState.update {
                    it.copy(
                        watchLoaded = savedWatch != null,
                        watchOn = savedWatch?.enabled == true,
                        labels = savedLabels,
                        labelDraft = savedLabels ?: SettingsMediaServersLabels(),
                        cardsRevision = it.cardsRevision + 1,
                        tracks = savedTracks,
                        trackDraft = savedTracks ?: SettingsMediaServersTracks(),
                        languages = languages.await().orEmpty(),
                    )
                }
            }
        }
    }

    private suspend fun loadServers(): ApiError? =
        when (val result = api.result(SettingsMediaServersEndpoints.list)) {
            is ApiResult.Ok -> {
                items = result.value.mediaServers
                rebuild()
                null
            }
            is ApiResult.Failed -> result.error
        }

    private suspend fun reloadServers() {
        loadServers()?.let { error -> mutableState.update { it.copy(status = it.status.failed(error)) } }
    }

    private fun rebuild() {
        val rows =
            items.map { item ->
                SettingsMediaServersRow(
                    id = item.id,
                    name = item.name,
                    kind = item.kind,
                    kindName = kindName(item.kind),
                    url = item.url,
                    enabled = item.enabled,
                    onImport = item.onImport,
                    onRename = item.onRename,
                    onDelete = item.onDelete,
                    pathFrom = item.pathFrom,
                    pathTo = item.pathTo,
                    lastOkAt = item.lastOkAt,
                    lastError = item.lastError,
                    tag = results[item.id],
                    testing = item.id in testing,
                    busy = item.id in removing,
                )
            }
        mutableState.update { it.copy(servers = rows) }
    }

    private fun kindName(wire: String): String =
        SettingsMediaServersKind.of(wire)?.displayName ?: wire.replaceFirstChar { it.uppercaseChar() }

    fun startAdd() {
        plexItems = emptyList()
        mutableState.update {
            it.copy(
                form = SettingsMediaServersForm(),
                formRevision = it.formRevision + 1,
                formError = null,
                saving = false,
                plex = SettingsMediaServersPlexPick(),
            )
        }
    }

    fun startChange(id: Long) {
        val item = items.firstOrNull { it.id == id } ?: return
        plexItems = emptyList()
        val form =
            SettingsMediaServersForm(
                id = item.id,
                kind = item.kind,
                name = item.name,
                originalName = item.name,
                url = item.url,
                tokenStored = item.token.isNotEmpty(),
                webUrl = item.webUrl,
                plainPassword = item.plainPassword,
                onImport = item.onImport,
                onRename = item.onRename,
                onDelete = item.onDelete,
                pathFrom = item.pathFrom,
                pathTo = item.pathTo,
                allowLocalNetwork = item.allowLocalNetwork,
                enabled = item.enabled,
            )
        mutableState.update {
            it.copy(
                form = form,
                formRevision = it.formRevision + 1,
                formError = null,
                saving = false,
                plex = SettingsMediaServersPlexPick(),
            )
        }
    }

    fun cancelForm() {
        plexItems = emptyList()
        mutableState.update { it.copy(form = null, formError = null, saving = false, plex = SettingsMediaServersPlexPick()) }
    }

    private fun edit(transform: (SettingsMediaServersForm) -> SettingsMediaServersForm) =
        mutableState.update { s -> s.form?.let { s.copy(form = transform(it), formError = null) } ?: s }

    fun setName(text: String) = edit { it.copy(name = text.take(MAX_NAME)) }

    fun setKind(wire: String) {
        if (SettingsMediaServersKind.of(wire) == null) return
        mutableState.update { s ->
            val form = s.form
            if (form == null || !form.isNew) {
                s
            } else {
                s.copy(
                    form = form.copy(kind = wire, usePlexSignIn = form.usePlexSignIn && wire == SettingsMediaServersKind.PLEX.wire),
                    formError = null,
                    plex = if (wire == SettingsMediaServersKind.PLEX.wire) s.plex else SettingsMediaServersPlexPick(),
                )
            }
        }
    }

    fun setUrl(text: String) = edit { it.copy(url = text) }

    fun setUsername(text: String) = edit { it.copy(username = text) }

    fun setToken(text: String) = edit { it.copy(token = text) }

    fun setWebUrl(text: String) = edit { it.copy(webUrl = text) }

    fun setPlainPassword(on: Boolean) = edit { it.copy(plainPassword = on) }

    fun setOnImport(on: Boolean) = edit { it.copy(onImport = on) }

    fun setOnRename(on: Boolean) = edit { it.copy(onRename = on) }

    fun setOnDelete(on: Boolean) = edit { it.copy(onDelete = on) }

    fun setPathFrom(text: String) = edit { it.copy(pathFrom = text) }

    fun setPathTo(text: String) = edit { it.copy(pathTo = text) }

    fun setAllowLocalNetwork(on: Boolean) = edit { it.copy(allowLocalNetwork = on) }

    fun setEnabled(on: Boolean) = edit { it.copy(enabled = on) }

    suspend fun findPlexServers() {
        val s = mutableState.value
        if (!s.showsPlexPicker || s.plex.finding) return
        mutableState.update { it.copy(plex = it.plex.copy(finding = true, notLinked = false, error = null)) }
        when (val result = api.result(SettingsMediaServersEndpoints.plexServers)) {
            is ApiResult.Ok -> {
                plexItems = result.value.servers
                val rows =
                    plexItems.map { item ->
                        SettingsMediaServersPlexRow(
                            id = item.id,
                            name = item.name,
                            address = item.address,
                            place =
                                if (item.relay) {
                                    "relay"
                                } else if (item.local) {
                                    "local"
                                } else {
                                    "remote"
                                },
                            urls = SettingsMediaServersLogic.plexAddresses(item.local, item.url, item.secureUrl),
                        )
                    }
                mutableState.update { it.copy(plex = SettingsMediaServersPlexPick(searched = true, rows = rows)) }
                pickPlex(firstPlexRow())
            }
            is ApiResult.Failed -> {
                val notLinked = result.error.code == ApiErrorCode.CONFLICT
                mutableState.update {
                    it.copy(plex = it.plex.copy(finding = false, notLinked = notLinked, error = if (notLinked) null else result.error))
                }
            }
        }
    }

    private fun firstPlexRow(): Int {
        var best = -1
        plexItems.forEachIndexed { index, item ->
            val rank = SettingsMediaServersLogic.plexRank(item.local, item.relay)
            val current = plexItems.getOrNull(best)
            if (current == null || rank < SettingsMediaServersLogic.plexRank(current.local, current.relay)) best = index
        }
        return best
    }

    fun pickPlex(index: Int) {
        mutableState.update { s ->
            val form = s.form
            val row = s.plex.rows.getOrNull(index)
            if (form == null || !form.isNew) {
                s
            } else if (row == null) {
                s.copy(plex = s.plex.copy(picked = -1), form = form.copy(usePlexSignIn = false), formError = null)
            } else {
                s.copy(
                    formRevision = s.formRevision + 1,
                    plex = s.plex.copy(picked = index),
                    form =
                        form.copy(
                            usePlexSignIn = true,
                            url = row.urls.firstOrNull().orEmpty(),
                            name = form.name.ifBlank { row.name.take(MAX_NAME) },
                            token = "",
                        ),
                    formError = null,
                )
            }
        }
    }

    fun pickPlexAddress(url: String) {
        mutableState.update { s ->
            val form = s.form
            if (form == null || url !in s.plex.addresses) s else s.copy(form = form.copy(url = url), formRevision = s.formRevision + 1)
        }
    }

    suspend fun save() {
        val form = mutableState.value.form ?: return
        if (!form.canSave || mutableState.value.saving) return
        reauth.guardError(onFailure = { error -> mutableState.update { it.copy(formError = error) } }) {
            lock.withLock {
                mutableState.update { it.copy(saving = true, formError = null) }
                val result =
                    if (form.isNew) {
                        api.result(SettingsMediaServersEndpoints.add, form.input())
                    } else {
                        api.result(SettingsMediaServersEndpoints.change, form.input(), "id" to form.id)
                    }
                when (result) {
                    is ApiResult.Ok -> {
                        results = results - form.id - result.value.id
                        plexItems = emptyList()
                        mutableState.update {
                            it.copy(
                                saving = false,
                                form = null,
                                plex = SettingsMediaServersPlexPick(),
                                notice = makeNotice(SettingsMediaServersNoticeKind.SAVED, form.name.trim()),
                            )
                        }
                        reloadServers()
                        null
                    }
                    is ApiResult.Failed -> {
                        mutableState.update { it.copy(saving = false) }
                        result.error
                    }
                }
            }
        }
    }

    suspend fun test(id: Long) {
        val item = items.firstOrNull { it.id == id } ?: return
        if (id in testing) return
        reauth.guardError {
            testing = testing + id
            rebuild()
            val result = api.result(SettingsMediaServersEndpoints.test, "id" to id)
            testing = testing - id
            if (result is ApiResult.Failed && result.error.code == ApiErrorCode.REAUTH) {
                rebuild()
                return@guardError result.error
            }
            results = results + (id to tagOf(item, result))
            lock.withLock { loadServers() }
            null
        }
    }

    private fun tagOf(
        item: SettingsMediaServersItem,
        result: ApiResult<SettingsMediaServersTested>,
    ): SettingsMediaServersTag {
        val app = kindName(item.kind)
        return when (result) {
            is ApiResult.Ok -> {
                val found = result.value.found
                val libraries =
                    if (found.startsWith("Libraries:", ignoreCase = true)) {
                        found.drop(LIBRARIES_PREFIX).split(',').count { it.isNotBlank() }
                    } else {
                        0
                    }
                SettingsMediaServersTag(
                    ok = true,
                    text =
                        if (libraries > 0) {
                            ""
                        } else {
                            found
                                .trim()
                                .split(Regex("\\s+"))
                                .lastOrNull()
                                .orEmpty()
                        },
                    app = app,
                    libraries = libraries,
                    hover = found,
                    failure = "",
                    error = null,
                    helpUrl = "",
                )
            }
            is ApiResult.Failed -> {
                val failure = SettingsMediaServersLogic.serverText(result.error)
                SettingsMediaServersTag(
                    ok = false,
                    text = failure,
                    app = app,
                    libraries = 0,
                    hover = failure,
                    failure = failure,
                    error = result.error,
                    helpUrl = SettingsMediaServersLogic.connectHelpUrl(failure),
                )
            }
        }
    }

    suspend fun remove(id: Long) {
        if (items.none { it.id == id } || id in removing) return
        reauth.guardError(onFailure = { error -> notify(SettingsMediaServersNoticeKind.REMOVE_FAILED, error = error) }) {
            lock.withLock {
                removing = removing + id
                rebuild()
                val result = api.result(SettingsMediaServersEndpoints.remove, "id" to id)
                removing = removing - id
                when (result) {
                    is ApiResult.Ok -> {
                        results = results - id
                        reloadServers()
                        null
                    }
                    is ApiResult.Failed -> {
                        rebuild()
                        result.error
                    }
                }
            }
        }
    }

    suspend fun setWatchHistory(on: Boolean) {
        if (mutableState.value.watchBusy) return
        reauth.guardError(onFailure = { error -> notify(SettingsMediaServersNoticeKind.WATCH_FAILED, error = error) }) {
            lock.withLock {
                mutableState.update { it.copy(watchBusy = true) }
                when (val result = api.result(SettingsMediaServersEndpoints.saveWatchHistory, SettingsMediaServersWatch(on))) {
                    is ApiResult.Ok -> {
                        mutableState.update {
                            it.copy(
                                watchBusy = false,
                                watchOn = on,
                                notice =
                                    makeNotice(
                                        if (on) SettingsMediaServersNoticeKind.WATCH_ON else SettingsMediaServersNoticeKind.WATCH_OFF,
                                    ),
                            )
                        }
                        null
                    }
                    is ApiResult.Failed -> {
                        mutableState.update { it.copy(watchBusy = false) }
                        result.error
                    }
                }
            }
        }
    }

    private fun editLabels(transform: (SettingsMediaServersLabels) -> SettingsMediaServersLabels) =
        mutableState.update { it.copy(labelDraft = transform(it.labelDraft)) }

    fun setLabelsEnabled(on: Boolean) = editLabels { it.copy(enabled = on) }

    fun setLabelText(text: String) = editLabels { it.copy(label = text.take(MAX_LABEL)) }

    fun setLabelAskers(on: Boolean) = editLabels { it.copy(askers = on) }

    suspend fun saveLabels() {
        val s = mutableState.value
        if (!s.labelsCanSave) return
        val body = s.labelDraft.copy(label = s.labelDraft.label.trim())
        reauth.guardError(onFailure = { error -> notify(SettingsMediaServersNoticeKind.LABELS_FAILED, error = error) }) {
            lock.withLock {
                mutableState.update { it.copy(labelBusy = true) }
                when (val result = api.result(SettingsMediaServersEndpoints.saveLabels, body)) {
                    is ApiResult.Ok -> {
                        mutableState.update {
                            it.copy(
                                labelBusy = false,
                                labels = result.value,
                                labelDraft = result.value,
                                cardsRevision = it.cardsRevision + 1,
                                notice = makeNotice(SettingsMediaServersNoticeKind.LABELS_SAVED),
                            )
                        }
                        null
                    }
                    is ApiResult.Failed -> {
                        mutableState.update { it.copy(labelBusy = false) }
                        result.error
                    }
                }
            }
        }
    }

    suspend fun runLabels() {
        if (!mutableState.value.labelsCanRun) return
        reauth.guardError(onFailure = { error -> notify(SettingsMediaServersNoticeKind.LABELS_START_FAILED, error = error) }) {
            lock.withLock {
                mutableState.update { it.copy(labelRunBusy = true) }
                when (val result = api.result(SettingsMediaServersEndpoints.runLabels)) {
                    is ApiResult.Ok -> {
                        mutableState.update {
                            it.copy(labelRunBusy = false, notice = makeNotice(SettingsMediaServersNoticeKind.LABELS_STARTED))
                        }
                        null
                    }
                    is ApiResult.Failed -> {
                        mutableState.update { it.copy(labelRunBusy = false) }
                        result.error
                    }
                }
            }
        }
    }

    private fun editTracks(transform: (SettingsMediaServersTracks) -> SettingsMediaServersTracks) =
        mutableState.update { it.copy(trackDraft = transform(it.trackDraft)) }

    private fun editList(
        subtitles: Boolean,
        transform: (List<String>) -> List<String>,
    ) = editTracks { if (subtitles) it.copy(subtitles = transform(it.subtitles)) else it.copy(audio = transform(it.audio)) }

    fun setTracksEnabled(on: Boolean) = editTracks { it.copy(enabled = on) }

    fun addTrackLanguage(subtitles: Boolean) {
        val s = mutableState.value
        if (if (subtitles) !s.canAddSubtitle else !s.canAddAudio) return
        val list = if (subtitles) s.trackDraft.subtitles else s.trackDraft.audio
        val next = SettingsMediaServersLogic.nextLanguage(list, s.languages.map { it.code }, language()) ?: return
        editList(subtitles) { it + next }
    }

    fun removeTrackLanguage(
        subtitles: Boolean,
        index: Int,
    ) = editList(subtitles) { list -> list.filterIndexed { i, _ -> i != index } }

    fun moveTrackLanguage(
        subtitles: Boolean,
        index: Int,
        up: Boolean,
    ) = editList(subtitles) { SettingsMediaServersLogic.moveLanguage(it, index, up) }

    fun setTrackLanguage(
        subtitles: Boolean,
        index: Int,
        code: String,
    ) {
        val s = mutableState.value
        val known = code == SettingsMediaServersLogic.OFF && subtitles || s.languages.any { it.code == code }
        val list = if (subtitles) s.trackDraft.subtitles else s.trackDraft.audio
        if (!known || (code in list && list.getOrNull(index) != code)) return
        editList(subtitles) { SettingsMediaServersLogic.replaceLanguage(it, index, code) }
    }

    suspend fun saveTracks() {
        val s = mutableState.value
        if (!s.tracksCanSave) return
        val body = s.trackDraft
        reauth.guardError(onFailure = { error -> notify(SettingsMediaServersNoticeKind.TRACKS_FAILED, error = error) }) {
            lock.withLock {
                mutableState.update { it.copy(trackBusy = true) }
                when (val result = api.result(SettingsMediaServersEndpoints.saveTracks, body)) {
                    is ApiResult.Ok -> {
                        mutableState.update {
                            it.copy(
                                trackBusy = false,
                                tracks = result.value,
                                trackDraft = result.value,
                                notice =
                                    makeNotice(
                                        if (result.value.enabled) {
                                            SettingsMediaServersNoticeKind.TRACKS_SAVED_ON
                                        } else {
                                            SettingsMediaServersNoticeKind.TRACKS_SAVED
                                        },
                                    ),
                            )
                        }
                        null
                    }
                    is ApiResult.Failed -> {
                        mutableState.update { it.copy(trackBusy = false) }
                        result.error
                    }
                }
            }
        }
    }

    fun dismissNotice(id: Long) {
        mutableState.update { if (it.notice?.id == id) it.copy(notice = null) else it }
    }

    private fun makeNotice(
        kind: SettingsMediaServersNoticeKind,
        name: String = "",
        error: ApiError? = null,
    ): SettingsMediaServersNotice {
        noticeSeq += 1
        return SettingsMediaServersNotice(noticeSeq, kind, name, error)
    }

    private fun notify(
        kind: SettingsMediaServersNoticeKind,
        name: String = "",
        error: ApiError? = null,
    ) = mutableState.update { it.copy(notice = makeNotice(kind, name, error)) }

    private companion object {
        const val MAX_NAME = 80
        const val MAX_LABEL = 64
        const val LIBRARIES_PREFIX = 10
    }
}
