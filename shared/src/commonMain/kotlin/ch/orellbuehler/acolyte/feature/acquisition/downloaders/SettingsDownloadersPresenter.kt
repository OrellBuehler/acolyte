package ch.orellbuehler.acolyte.feature.acquisition.downloaders

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DownloadersEndpoints
import ch.orellbuehler.acolyte.api.DownloadsLinkEndpoints
import ch.orellbuehler.acolyte.api.model.DownloaderInfo
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderAttempt
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderCategoryBody
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderPath
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderTestAnswer
import ch.orellbuehler.acolyte.api.model.SettingsKeyLookupBody
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.mayEditAcquisition
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SettingsDownloaderKeyNote {
    FETCHED,
    NEEDS_LOGIN,
    WRONG_LOGIN,
    NOT_FOUND,
}

enum class SettingsDownloaderMessageKind {
    SAVED,
    PAUSE_FAILED,
    RESUME_FAILED,
    REMOVE_FAILED,
    CATEGORY_MADE,
    CATEGORY_FAILED,
    ABANDONED,
    ABANDON_FAILED,
}

data class SettingsDownloaderMessage(
    val kind: SettingsDownloaderMessageKind,
    val name: String = "",
    val app: String = "",
    val error: ApiError? = null,
)

data class SettingsDownloaderTestView(
    val ok: Boolean = false,
    val text: String = "",
    val tag: String = "",
    val category: String = "",
    val canMake: Boolean = false,
    val makeOff: Boolean = false,
    val error: ApiError? = null,
)

data class SettingsDownloaderCategoryState(
    val loading: Boolean = false,
    val listed: Boolean? = null,
    val categories: List<String> = emptyList(),
) {
    fun options(current: String): List<String> =
        if (current.isNotEmpty() &&
            current !in categories
        ) {
            listOf(current) + categories
        } else {
            categories
        }
}

data class SettingsDownloaderKeyState(
    val fetching: Boolean = false,
    val loginShown: Boolean = false,
    val user: String = "",
    val pass: String = "",
    val note: SettingsDownloaderKeyNote? = null,
)

data class SettingsDownloaderRow(
    val info: DownloaderInfo,
    val kind: SettingsDownloaderKind?,
    val health: SettingsDownloaderHealthState,
    val testing: Boolean,
    val busy: Boolean,
    val test: SettingsDownloaderTestView?,
    val pending: List<SettingsDownloaderAttempt>,
    val attemptError: ApiError?,
    val abandoning: Long,
) {
    val id: Long get() = info.id

    val appName: String get() = kind?.appName ?: info.kind

    val usenet: Boolean get() = kind?.usenet ?: (info.protocol == "usenet")

    val tone: Tone get() = health.tone

    val healthWord: String get() = info.health.label

    val showHealthWord: Boolean get() = health != SettingsDownloaderHealthState.WORKING

    val paused: Boolean get() = health == SettingsDownloaderHealthState.PAUSED

    val canChange: Boolean get() = kind != null

    val failedTest: Boolean get() = test != null && !test.ok

    val showHealthText: Boolean get() = info.health.text.isNotBlank() && !failedTest

    val healthTextFails: Boolean get() = health != SettingsDownloaderHealthState.WORKING && health != SettingsDownloaderHealthState.PAUSED

    val locked: Boolean get() = info.writableLocked

    val handsOff: Boolean get() = !info.writable && !info.writableLocked
}

data class SettingsDownloadersState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canEdit: Boolean = false,
    val owner: Boolean = false,
    val downloaders: List<DownloaderInfo> = emptyList(),
    val attempts: Map<Long, List<SettingsDownloaderAttempt>> = emptyMap(),
    val attemptErrors: Map<Long, ApiError> = emptyMap(),
    val results: Map<Long, SettingsDownloaderTestView> = emptyMap(),
    val testing: Set<Long> = emptySet(),
    val busy: Set<Long> = emptySet(),
    val abandoning: Long = 0,
    val form: SettingsDownloaderForm? = null,
    val formError: ApiError? = null,
    val saving: Boolean = false,
    val formTesting: Boolean = false,
    val categories: SettingsDownloaderCategoryState = SettingsDownloaderCategoryState(),
    val key: SettingsDownloaderKeyState = SettingsDownloaderKeyState(),
    val message: SettingsDownloaderMessage? = null,
    val messageCount: Int = 0,
) {
    val isEmpty: Boolean get() = downloaders.isEmpty()

    val rows: List<SettingsDownloaderRow>
        get() =
            downloaders.map { info ->
                SettingsDownloaderRow(
                    info = info,
                    kind = SettingsDownloaderKind.from(info.kind),
                    health = SettingsDownloaderHealthState.from(info.health.state),
                    testing = info.id in testing,
                    busy = info.id in busy,
                    test = results[info.id],
                    pending = attempts[info.id].orEmpty().filter { it.state == "pending" },
                    attemptError = attemptErrors[info.id],
                    abandoning = abandoning,
                )
            }

    val formResult: SettingsDownloaderTestView? get() = form?.let { results[it.id] }

    val formApp: String get() = form?.appName.orEmpty()

    val canFetchKey: Boolean get() = owner && form?.canFetchKey == true
}

class SettingsDownloadersPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsDownloadersState())
    private val lock = Mutex()

    val state: StateFlow<SettingsDownloadersState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        val me = account()
        if (me == null || !me.mayOpen(AcquisitionPage.DOWNLOADERS)) {
            mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
            return
        }
        mutableState.update {
            it.copy(status = it.status.started(), allowed = true, canEdit = me.mayEditAcquisition, owner = me.role == Role.OWNER)
        }
        reload()
    }

    suspend fun watch() {
        api.live.collect { event ->
            if (event.topic == TOPIC || event.topic == LiveEvent.RESYNC) reload()
        }
    }

    private suspend fun reload() {
        lock.withLock {
            when (val result = api.result(DownloadsLinkEndpoints.downloaders)) {
                is ApiResult.Ok -> {
                    val list = result.value.downloaders
                    val ids = list.map { it.id }.toSet()
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            downloaders = list,
                            results = it.results.filterKeys { id -> id in ids },
                            attempts = it.attempts.filterKeys { id -> id in ids },
                            attemptErrors = it.attemptErrors.filterKeys { id -> id in ids },
                        )
                    }
                    loadAttempts(list)
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    private suspend fun loadAttempts(list: List<DownloaderInfo>) {
        coroutineScope {
            list
                .map { info ->
                    async { info.id to api.result(DownloadersEndpoints.attempts, "id" to info.id) }
                }.awaitAll()
        }.forEach { (id, result) ->
            when (result) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            attempts = it.attempts + (id to result.value.attempts),
                            attemptErrors =
                                it.attemptErrors - id,
                        )
                    }
                is ApiResult.Failed ->
                    mutableState.update { it.copy(attempts = it.attempts - id, attemptErrors = it.attemptErrors + (id to result.error)) }
            }
        }
    }

    fun startAdd() {
        if (!mayEdit()) return
        mutableState.update {
            it.copy(
                form = SettingsDownloaderForm.blank(),
                formError = null,
                categories = SettingsDownloaderCategoryState(),
                key = SettingsDownloaderKeyState(),
            )
        }
    }

    suspend fun startChange(id: Long) {
        val s = mutableState.value
        val info = s.downloaders.firstOrNull { it.id == id } ?: return
        if (!mayEdit() || SettingsDownloaderKind.from(info.kind) == null) return
        mutableState.update {
            it.copy(
                form = SettingsDownloaderForm.fromInfo(info),
                formError = null,
                categories = SettingsDownloaderCategoryState(loading = true),
                key = SettingsDownloaderKeyState(),
            )
        }
        loadCategories(id)
    }

    fun cancelForm() = mutableState.update { it.copy(form = null, formError = null, saving = false, formTesting = false) }

    fun pickKind(wire: String) {
        val kind = SettingsDownloaderKind.from(wire) ?: return
        edit { SettingsDownloaderForm.picking(it, kind) }
        mutableState.update { it.copy(key = SettingsDownloaderKeyState()) }
    }

    fun setName(value: String) = edit { it.copy(name = value.take(NAME_LIMIT)) }

    fun setUrl(value: String) = edit { it.copy(url = value) }

    fun setUsername(value: String) = edit { it.copy(username = value) }

    fun setSecret(value: String) = edit { it.copy(secret = value) }

    fun setLogin(on: Boolean) =
        edit { if (it.login == on) it else it.copy(login = on, secret = "", username = if (on) it.username else "") }

    fun setCategory(value: String) = edit { it.copy(category = value.take(CATEGORY_LIMIT)) }

    fun setPriority(value: String) = edit { it.copy(priority = value.filter { c -> c.isDigit() }.take(2)) }

    fun setKindCategory(
        field: SettingsDownloaderKindField,
        value: String,
    ) = edit {
        val text = value.take(CATEGORY_LIMIT)
        when (field) {
            SettingsDownloaderKindField.MOVIES -> it.copy(categoryMovies = text)
            SettingsDownloaderKindField.SHOWS -> it.copy(categoryShows = text)
            SettingsDownloaderKindField.MUSIC -> it.copy(categoryMusic = text)
        }
    }

    fun addTag(raw: String) =
        edit {
            val tag =
                raw
                    .trim()
                    .lowercase()
                    .replace(Regex("\\s+"), "-")
                    .take(TAG_LIMIT)
            if (tag.isEmpty() || tag in it.tags || it.tags.size >= TAG_COUNT) it else it.copy(tags = it.tags + tag)
        }

    fun removeTag(tag: String) = edit { it.copy(tags = it.tags - tag) }

    fun setCategoryAfterImport(value: String) = edit { it.copy(categoryAfterImport = value.take(CATEGORY_LIMIT)) }

    fun setOwnOnly(on: Boolean) = edit { it.copy(ownOnly = on) }

    fun addPathPair() = edit { it.copy(pathMap = it.pathMap + SettingsDownloaderPath()) }

    fun setPathTheirs(
        index: Int,
        value: String,
    ) = editPair(index) { it.copy(theirs = value) }

    fun setPathOurs(
        index: Int,
        value: String,
    ) = editPair(index) { it.copy(ours = value) }

    fun setPathLink(
        index: Int,
        on: Boolean,
    ) = editPair(index) { it.copy(link = on) }

    fun removePathPair(index: Int) = edit { it.copy(pathMap = it.pathMap.filterIndexed { i, _ -> i != index }) }

    fun setAllowLocalNetwork(on: Boolean) = edit { it.copy(allowLocalNetwork = on) }

    fun setWritable(on: Boolean) = edit { if (it.writableLocked) it else it.copy(writable = on) }

    fun setSeedRemove(on: Boolean) = edit { it.copy(seedRemove = on) }

    fun setSeedRatio(value: String) = edit { it.copy(seedRatio = number(value)) }

    fun setSeedHours(value: String) = edit { it.copy(seedHours = number(value)) }

    fun setSeedDeleteFiles(on: Boolean) = edit { it.copy(seedDeleteFiles = on) }

    fun setClearFinished(on: Boolean) = edit { it.copy(clearFinished = on) }

    fun setSabUser(value: String) = mutableState.update { it.copy(key = it.key.copy(user = value)) }

    fun setSabPass(value: String) = mutableState.update { it.copy(key = it.key.copy(pass = value)) }

    suspend fun save() {
        val form = mutableState.value.form ?: return
        if (!form.canSave || mutableState.value.saving || !mayEdit()) return
        mutableState.update { it.copy(saving = true, formError = null) }
        val body = form.body()
        val result =
            if (form.isNew) {
                api.result(DownloadersEndpoints.add, body)
            } else {
                api.result(DownloadersEndpoints.change, body, "id" to form.id)
            }
        when (result) {
            is ApiResult.Ok -> {
                val saved = result.value
                mutableState.update {
                    it.copy(
                        form = null,
                        saving = false,
                        message = SettingsDownloaderMessage(SettingsDownloaderMessageKind.SAVED, name = saved.name),
                        messageCount = it.messageCount + 1,
                    )
                }
                reload()
                test(saved.id)
            }
            is ApiResult.Failed -> mutableState.update { it.copy(saving = false, formError = result.error) }
        }
    }

    suspend fun test(id: Long) {
        val s = mutableState.value
        val info = s.downloaders.firstOrNull { it.id == id } ?: return
        if (id in s.testing) return
        mutableState.update { it.copy(testing = it.testing + id) }
        val app = SettingsDownloaderKind.from(info.kind)?.appName ?: info.kind
        val view =
            when (val result = api.result(DownloadersEndpoints.test, null, "id" to id)) {
                is ApiResult.Ok -> testView(result.value, app)
                is ApiResult.Failed -> failedView(result.error)
            }
        mutableState.update { it.copy(testing = it.testing - id, results = it.results + (id to view)) }
        reload()
    }

    suspend fun testForm() {
        val form = mutableState.value.form ?: return
        if (form.isNew || mutableState.value.formTesting) return
        mutableState.update { it.copy(formTesting = true) }
        val view =
            when (val result = api.result(DownloadersEndpoints.test, form.testBody(), "id" to form.id)) {
                is ApiResult.Ok -> testView(result.value, form.appName)
                is ApiResult.Failed -> failedView(result.error)
            }
        mutableState.update { it.copy(formTesting = false, results = it.results + (form.id to view)) }
        reload()
    }

    suspend fun pause(id: Long) = setPaused(id, true)

    suspend fun resume(id: Long) = setPaused(id, false)

    private suspend fun setPaused(
        id: Long,
        paused: Boolean,
    ) {
        val info = mutableState.value.downloaders.firstOrNull { it.id == id } ?: return
        if (id in mutableState.value.busy) return
        mutableState.update { it.copy(busy = it.busy + id) }
        val result = api.result(if (paused) DownloadersEndpoints.pause else DownloadersEndpoints.resume, "id" to id)
        mutableState.update { it.copy(busy = it.busy - id) }
        result
            .onSuccess { reload() }
            .onFailure {
                post(
                    SettingsDownloaderMessage(
                        if (paused) SettingsDownloaderMessageKind.PAUSE_FAILED else SettingsDownloaderMessageKind.RESUME_FAILED,
                        name = info.name,
                        error = it,
                    ),
                )
            }
    }

    suspend fun remove(id: Long) {
        val info = mutableState.value.downloaders.firstOrNull { it.id == id } ?: return
        if (id in mutableState.value.busy) return
        mutableState.update { it.copy(busy = it.busy + id) }
        val result = api.result(DownloadersEndpoints.remove, "id" to id)
        mutableState.update { it.copy(busy = it.busy - id) }
        result
            .onSuccess { reload() }
            .onFailure { post(SettingsDownloaderMessage(SettingsDownloaderMessageKind.REMOVE_FAILED, name = info.name, error = it)) }
    }

    suspend fun makeCategory(
        id: Long,
        name: String,
    ) {
        val info = mutableState.value.downloaders.firstOrNull { it.id == id } ?: return
        if (id in mutableState.value.busy) return
        mutableState.update { it.copy(busy = it.busy + id) }
        val result = api.result(DownloadersEndpoints.makeCategory, SettingsDownloaderCategoryBody(name), "id" to id)
        mutableState.update { it.copy(busy = it.busy - id) }
        val app = SettingsDownloaderKind.from(info.kind)?.appName ?: info.kind
        when (result) {
            is ApiResult.Ok -> {
                post(SettingsDownloaderMessage(SettingsDownloaderMessageKind.CATEGORY_MADE, name = name, app = app))
                test(id)
            }
            is ApiResult.Failed ->
                post(
                    SettingsDownloaderMessage(SettingsDownloaderMessageKind.CATEGORY_FAILED, name = name, error = result.error),
                )
        }
    }

    suspend fun abandon(
        id: Long,
        attemptId: Long,
    ) {
        val info = mutableState.value.downloaders.firstOrNull { it.id == id } ?: return
        if (mutableState.value.abandoning != 0L) return
        mutableState.update { it.copy(abandoning = attemptId) }
        val result = api.result(DownloadersEndpoints.abandon, "id" to id, "attemptID" to attemptId)
        mutableState.update { it.copy(abandoning = 0) }
        when (result) {
            is ApiResult.Ok -> {
                post(SettingsDownloaderMessage(SettingsDownloaderMessageKind.ABANDONED, name = info.name))
                loadAttempts(listOf(info))
            }
            is ApiResult.Failed ->
                post(
                    SettingsDownloaderMessage(SettingsDownloaderMessageKind.ABANDON_FAILED, name = info.name, error = result.error),
                )
        }
    }

    private suspend fun loadCategories(id: Long) {
        val result = api.result(DownloadersEndpoints.categories, "id" to id)
        if (mutableState.value.form?.id != id) return
        when (result) {
            is ApiResult.Ok -> {
                val answer = result.value
                mutableState.update { s ->
                    val form = s.form
                    val picked =
                        if (answer.listed && form != null && form.hasPicks()) {
                            form.copy(
                                categoryMovies = answer.picks.movie,
                                categoryShows = answer.picks.show,
                                categoryMusic = answer.picks.artist,
                            )
                        } else {
                            form
                        }
                    s.copy(form = picked, categories = SettingsDownloaderCategoryState(false, answer.listed, answer.categories))
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(categories = SettingsDownloaderCategoryState(false, false)) }
        }
    }

    suspend fun fetchKey() {
        val s = mutableState.value
        val form = s.form ?: return
        if (!s.canFetchKey || s.key.fetching) return
        val login = s.key.loginShown
        mutableState.update { it.copy(key = it.key.copy(fetching = true)) }
        val body =
            SettingsKeyLookupBody(
                kind = SettingsDownloaderKind.SABNZBD.wire,
                url = form.url.trim(),
                allowLocalNetwork = form.allowLocalNetwork,
                username = if (login) s.key.user else "",
                password = if (login) s.key.pass else "",
            )
        val answer = (api.result(DownloadersEndpoints.keyLookup, body) as? ApiResult.Ok)?.value
        mutableState.update { current ->
            val key = current.key
            when {
                answer != null && answer.key.isNotEmpty() ->
                    current.copy(
                        form = current.form?.copy(secret = answer.key),
                        key = SettingsDownloaderKeyState(note = SettingsDownloaderKeyNote.FETCHED),
                    )
                answer != null && answer.needLogin ->
                    current.copy(key = key.copy(fetching = false, loginShown = true, note = SettingsDownloaderKeyNote.NEEDS_LOGIN))
                answer != null && (answer.problem == WRONG_LOGIN || (login && s.key.user.isNotEmpty())) ->
                    current.copy(key = key.copy(fetching = false, loginShown = login, note = SettingsDownloaderKeyNote.WRONG_LOGIN))
                else -> current.copy(key = key.copy(fetching = false, loginShown = login, note = SettingsDownloaderKeyNote.NOT_FOUND))
            }
        }
    }

    private fun mayEdit(): Boolean = account()?.mayEditAcquisition == true

    private fun post(message: SettingsDownloaderMessage) =
        mutableState.update { it.copy(message = message, messageCount = it.messageCount + 1) }

    private fun edit(transform: (SettingsDownloaderForm) -> SettingsDownloaderForm) =
        mutableState.update { s -> s.form?.let { s.copy(form = transform(it), formError = null) } ?: s }

    private fun editPair(
        index: Int,
        transform: (SettingsDownloaderPath) -> SettingsDownloaderPath,
    ) = edit { it.copy(pathMap = it.pathMap.mapIndexed { i, pair -> if (i == index) transform(pair) else pair }) }

    private fun number(value: String): String = value.filter { it.isDigit() || it == '.' }.take(NUMBER_LIMIT)

    private fun testView(
        answer: SettingsDownloaderTestAnswer,
        app: String,
    ): SettingsDownloaderTestView =
        SettingsDownloaderTestView(
            ok = answer.ok,
            text = answer.text,
            tag = if (answer.ok) testTag(app, answer.version) else "",
            category = answer.category,
            canMake = answer.canMake,
            makeOff = answer.makeOff,
        )

    private fun failedView(error: ApiError): SettingsDownloaderTestView =
        SettingsDownloaderTestView(ok = false, text = if (error.status in SERVER_SENTENCE) error.message else "", error = error)

    private companion object {
        const val TOPIC = "downloader"
        const val WRONG_LOGIN = "wrong_login"
        const val NAME_LIMIT = 80
        const val CATEGORY_LIMIT = 64
        const val TAG_LIMIT = 40
        const val TAG_COUNT = 20
        const val NUMBER_LIMIT = 9
        val SERVER_SENTENCE = 400..409
    }
}

fun testTag(
    app: String,
    version: String,
): String {
    val parts =
        Regex("(\\d+(?:\\.\\d+)+)")
            .find(version)
            ?.value
            ?.split('.')
            ?.take(VERSION_PARTS)
            ?.joinToString(".")
    return listOfNotNull(app.takeIf { it.isNotEmpty() }, parts?.let { "v$it" }).joinToString(" ")
}

private const val VERSION_PARTS = 3
