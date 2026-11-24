package ch.orellbuehler.acolyte.feature.settings.controls

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.HomeEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsControlsEndpoints
import ch.orellbuehler.acolyte.api.SettingsSharedEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsPathInfo
import ch.orellbuehler.acolyte.api.model.SettingsProxy
import ch.orellbuehler.acolyte.api.model.SettingsStorage
import ch.orellbuehler.acolyte.api.model.SettingsUiChange
import ch.orellbuehler.acolyte.api.model.SettingsUiSettings
import ch.orellbuehler.acolyte.api.model.SettingsUrlBase
import ch.orellbuehler.acolyte.api.model.SettingsUrlBaseChange
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsControlsPathRow(
    val name: String,
    val path: String,
    val size: String,
    val free: String,
    val kind: String,
) {
    val freeKnown: Boolean get() = free.isNotEmpty()
}

data class SettingsControlsStorage(
    val app: List<SettingsControlsPathRow>,
    val folders: List<SettingsControlsPathRow>,
    val savePath: String,
)

data class SettingsControlsUrlBase(
    val saved: String,
    val inUse: String,
    val restartNeeded: Boolean,
)

data class SettingsControlsProxyForm(
    val type: String = "",
    val host: String = "",
    val port: String = "8080",
    val username: String = "",
    val password: String = "",
    val bypass: String = "",
    val bypassLocal: Boolean = true,
)

enum class SettingsControlsSavedKind {
    HISTORY,
    CACHE,
    COUNT_ON,
    COUNT_OFF,
    URL_BASE,
    PROXY_ON,
    PROXY_OFF,
    FAILED,
}

data class SettingsControlsSaved(
    val id: Int,
    val kind: SettingsControlsSavedKind,
    val value: Int = 0,
    val restartNeeded: Boolean = false,
    val error: ApiError? = null,
)

private const val DEFAULT_PROXY_PORT = 8080

data class SettingsControlsThisAppState(
    val status: LoadStatus = LoadStatus(),
    val canSettings: Boolean = false,
    val isOwner: Boolean = false,
    val version: String = "",
    val uptime: String = "",
    val schema: Int = 0,
    val liveClients: Int = 0,
    val uiLoaded: Boolean = false,
    val historyRoutineDays: Int = 30,
    val historyOtherDays: Int = 0,
    val cacheGb: Int = 5,
    val countInstall: Boolean = true,
    val uiBusy: Boolean = false,
    val storage: SettingsControlsStorage? = null,
    val storageProof: Boolean = false,
    val storageError: ApiError? = null,
    val urlBaseAvailable: Boolean = true,
    val urlBase: SettingsControlsUrlBase? = null,
    val urlBaseDraft: String = "",
    val urlBaseSaving: Boolean = false,
    val urlBaseError: ApiError? = null,
    val proxyAvailable: Boolean = true,
    val proxyLoaded: Boolean = false,
    val proxy: SettingsControlsProxyForm = SettingsControlsProxyForm(),
    val proxySaved: SettingsControlsProxyForm = SettingsControlsProxyForm(),
    val proxyPasswordSaved: Boolean = false,
    val proxySaving: Boolean = false,
    val proxyError: ApiError? = null,
    val saved: SettingsControlsSaved? = null,
) {
    val canChangeHistory: Boolean get() = isOwner

    val routineChoices: List<Int> get() = SettingsControlsLogic.historyChoices(historyRoutineDays)

    val otherChoices: List<Int> get() = SettingsControlsLogic.historyChoices(historyOtherDays)

    val cacheChoices: List<Int> get() = SettingsControlsLogic.CACHE_GB

    val canSaveUrlBase: Boolean
        get() = urlBase != null && urlBaseDraft.trim() != urlBase.saved && !urlBaseSaving

    val proxyOn: Boolean get() = proxy.type.isNotEmpty()

    val proxyChanged: Boolean get() = proxy != proxySaved

    val proxyValid: Boolean
        get() = !proxyOn || (proxy.host.isNotBlank() && proxy.port.toIntOrNull() in 1..MAX_PORT)

    val canSaveProxy: Boolean get() = proxyLoaded && proxyChanged && proxyValid && !proxySaving

    val proxyPasswordHint: Boolean get() = proxyPasswordSaved

    private companion object {
        const val MAX_PORT = 65535
    }
}

class SettingsControlsThisAppPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
) {
    private val mutableState = MutableStateFlow(SettingsControlsThisAppState())
    private val lock = Mutex()
    private var notices = 0

    val state: StateFlow<SettingsControlsThisAppState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            val settings = me?.can(Need.SETTINGS) == true
            val owner = me?.can(Need.OWNER) == true
            mutableState.update { it.copy(status = it.status.started(), canSettings = settings, isOwner = owner) }
            coroutineScope {
                val health = async { api.result(HomeEndpoints.health) }
                val ui = async { if (settings) api.result(SettingsSharedEndpoints.uiSettings) else null }
                val storage = async { if (settings) api.result(SettingsControlsEndpoints.storage) else null }
                val urlBase = async { if (settings) api.result(SettingsControlsEndpoints.urlBase) else null }
                val proxy = async { if (owner) api.result(SettingsControlsEndpoints.proxy) else null }
                val healthResult = health.await()
                val reading = healthResult.valueOrNull()
                if (reading == null) {
                    val error = (healthResult as ApiResult.Failed).error
                    mutableState.update { it.copy(status = it.status.failed(error)) }
                    return@coroutineScope
                }
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        version = reading.version,
                        uptime = SettingsControlsLogic.duration(reading.uptimeSeconds.toDouble()),
                        schema = reading.schema,
                        liveClients = reading.liveClients,
                    )
                }
                applyUi(ui.await())
                applyStorage(storage.await())
                applyUrlBase(urlBase.await())
                applyProxy(proxy.await())
            }
        }
    }

    suspend fun poll() {
        if (mutableState.value.status.phase != Phase.READY) {
            refresh()
            return
        }
        lock.withLock {
            val settings = mutableState.value.canSettings
            coroutineScope {
                val health = async { api.result(HomeEndpoints.health) }
                val storage =
                    async {
                        if (settings &&
                            !mutableState.value.storageProof
                        ) {
                            api.result(SettingsControlsEndpoints.storage)
                        } else {
                            null
                        }
                    }
                when (val result = health.await()) {
                    is ApiResult.Ok ->
                        mutableState.update {
                            it.copy(
                                status = LoadStatus.ready,
                                version = result.value.version,
                                uptime = SettingsControlsLogic.duration(result.value.uptimeSeconds.toDouble()),
                                schema = result.value.schema,
                                liveClients = result.value.liveClients,
                            )
                        }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                }
                applyStorage(storage.await())
            }
        }
    }

    suspend fun watch() {
        coroutineScope {
            launch {
                while (true) {
                    delay(POLL_MILLIS)
                    poll()
                }
            }
            api.live.collect { event ->
                if (event.topic == TOPIC || event.topic == LiveEvent.RESYNC) poll()
            }
        }
    }

    suspend fun setHistoryRoutine(days: Int) =
        saveUi(SettingsUiChange(historyRoutineDays = days), { it.copy(historyRoutineDays = days) }) {
            SettingsControlsSavedKind.HISTORY to 0
        }

    suspend fun setHistoryOther(days: Int) =
        saveUi(SettingsUiChange(historyOtherDays = days), { it.copy(historyOtherDays = days) }) {
            SettingsControlsSavedKind.HISTORY to 0
        }

    suspend fun setCacheGb(gb: Int) =
        saveUi(SettingsUiChange(artworkCacheGb = gb), { it.copy(cacheGb = gb) }) {
            SettingsControlsSavedKind.CACHE to it.artworkCacheGb
        }

    suspend fun setCountInstall(on: Boolean) =
        saveUi(SettingsUiChange(countInstall = on), { it.copy(countInstall = on) }) {
            (if (it.countInstall) SettingsControlsSavedKind.COUNT_ON else SettingsControlsSavedKind.COUNT_OFF) to 0
        }

    suspend fun proveStorage() {
        reauth.guardError(onFailure = { error -> mutableState.update { it.copy(storageError = error) } }) {
            val result = api.result(SettingsControlsEndpoints.storage)
            applyStorage(result)
            (result as? ApiResult.Failed)?.error
        }
    }

    fun setUrlBase(text: String) {
        mutableState.update { it.copy(urlBaseDraft = text.take(URL_BASE_MAX), urlBaseError = null) }
    }

    suspend fun saveUrlBase() {
        val current = mutableState.value
        if (!current.canSaveUrlBase) return
        mutableState.update { it.copy(urlBaseSaving = true, urlBaseError = null) }
        when (val result = api.result(SettingsControlsEndpoints.changeUrlBase, SettingsUrlBaseChange(current.urlBaseDraft.trim()))) {
            is ApiResult.Ok -> {
                val value = result.value
                mutableState.update {
                    it.copy(
                        urlBase = SettingsControlsUrlBase(value.saved, value.inUse, value.restartNeeded),
                        urlBaseDraft = value.saved,
                        urlBaseSaving = false,
                        saved = SettingsControlsSaved(++notices, SettingsControlsSavedKind.URL_BASE, restartNeeded = value.restartNeeded),
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(urlBaseSaving = false, urlBaseError = result.error) }
        }
    }

    fun setProxyType(type: String) = editProxy { it.copy(type = type) }

    fun setProxyHost(text: String) = editProxy { it.copy(host = text.take(HOST_MAX)) }

    fun setProxyPort(text: String) = editProxy { it.copy(port = text.filter { char -> char.isDigit() }.take(PORT_DIGITS)) }

    fun setProxyUsername(text: String) = editProxy { it.copy(username = text.take(SECRET_MAX)) }

    fun setProxyPassword(text: String) = editProxy { it.copy(password = text.take(SECRET_MAX)) }

    fun setProxyBypass(text: String) = editProxy { it.copy(bypass = text.take(BYPASS_MAX)) }

    fun setProxyBypassLocal(on: Boolean) = editProxy { it.copy(bypassLocal = on) }

    suspend fun saveProxy() {
        val current = mutableState.value
        if (!current.canSaveProxy) return
        val form = current.proxy
        val named = form.type.isNotEmpty() && form.username.isNotEmpty()
        val keep = form.password.isEmpty() && current.proxyPasswordSaved && named
        val password =
            when {
                keep -> MASK
                named -> form.password
                else -> ""
            }
        val body =
            SettingsProxy(form.type, form.host.trim(), form.port.toIntOrNull() ?: 0, form.username, password, form.bypass, form.bypassLocal)
        mutableState.update { it.copy(proxySaving = true, proxyError = null) }
        when (val result = api.result(SettingsControlsEndpoints.changeProxy, body)) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(proxySaving = false) }
                takeProxy(result.value)
                val kind = if (result.value.type.isNotEmpty()) SettingsControlsSavedKind.PROXY_ON else SettingsControlsSavedKind.PROXY_OFF
                mutableState.update { it.copy(saved = SettingsControlsSaved(++notices, kind)) }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(proxySaving = false, proxyError = result.error) }
        }
    }

    private fun editProxy(transform: (SettingsControlsProxyForm) -> SettingsControlsProxyForm) {
        mutableState.update { it.copy(proxy = transform(it.proxy), proxyError = null) }
    }

    private suspend fun saveUi(
        change: SettingsUiChange,
        optimistic: (SettingsControlsThisAppState) -> SettingsControlsThisAppState,
        done: (SettingsUiSettings) -> Pair<SettingsControlsSavedKind, Int>,
    ) {
        val before = mutableState.value
        if (before.uiBusy || !before.uiLoaded) return
        if ((change.historyRoutineDays != null || change.historyOtherDays != null) && !before.canChangeHistory) return
        mutableState.update { optimistic(it).copy(uiBusy = true) }
        when (val result = api.result(SettingsSharedEndpoints.changeUiSettings, change)) {
            is ApiResult.Ok -> {
                val (kind, value) = done(result.value)
                mutableState.update { it.copy(saved = SettingsControlsSaved(++notices, kind, value)) }
                applyUi(result)
            }
            is ApiResult.Failed -> {
                mutableState.update {
                    it.copy(
                        historyRoutineDays = before.historyRoutineDays,
                        historyOtherDays = before.historyOtherDays,
                        cacheGb = before.cacheGb,
                        countInstall = before.countInstall,
                        uiBusy = false,
                        saved = SettingsControlsSaved(++notices, SettingsControlsSavedKind.FAILED, error = result.error),
                    )
                }
                applyUi(api.result(SettingsSharedEndpoints.uiSettings))
            }
        }
        mutableState.update { it.copy(uiBusy = false) }
    }

    private fun applyUi(result: ApiResult<SettingsUiSettings>?) {
        when (result) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        uiLoaded = true,
                        historyRoutineDays = result.value.historyRoutineDays,
                        historyOtherDays = result.value.historyOtherDays,
                        cacheGb = result.value.artworkCacheGb,
                        countInstall = result.value.countInstall,
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(uiLoaded = false) }
            null -> Unit
        }
    }

    private fun applyStorage(result: ApiResult<SettingsStorage>?) {
        when (result) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        storage =
                            SettingsControlsStorage(
                                result.value.app.map(::pathRow),
                                result.value.libraryFolders.map(::pathRow),
                                result.value.savePath,
                            ),
                        storageProof = false,
                        storageError = null,
                    )
                }
            is ApiResult.Failed ->
                mutableState.update {
                    if (result.error.code == ApiErrorCode.REAUTH) {
                        it.copy(storage = null, storageProof = true, storageError = null)
                    } else {
                        it.copy(storage = null, storageProof = false, storageError = result.error)
                    }
                }
            null -> Unit
        }
    }

    private fun applyUrlBase(result: ApiResult<SettingsUrlBase>?) {
        when (result) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        urlBaseAvailable = true,
                        urlBase = SettingsControlsUrlBase(result.value.saved, result.value.inUse, result.value.restartNeeded),
                        urlBaseDraft = result.value.saved,
                        urlBaseError = null,
                    )
                }
            is ApiResult.Failed ->
                mutableState.update {
                    if (result.error.code == ApiErrorCode.NOT_FOUND) {
                        it.copy(urlBaseAvailable = false, urlBase = null)
                    } else {
                        it.copy(urlBaseError = result.error)
                    }
                }
            null -> Unit
        }
    }

    private fun applyProxy(result: ApiResult<SettingsProxy>?) {
        when (result) {
            is ApiResult.Ok -> takeProxy(result.value)
            is ApiResult.Failed ->
                mutableState.update {
                    if (result.error.code == ApiErrorCode.NOT_FOUND) it.copy(proxyAvailable = false) else it.copy(proxyError = result.error)
                }
            null -> Unit
        }
    }

    private fun takeProxy(value: SettingsProxy) {
        val form =
            SettingsControlsProxyForm(
                type = value.type,
                host = value.host,
                port = (value.port.takeIf { it > 0 } ?: DEFAULT_PROXY_PORT).toString(),
                username = value.username,
                password = "",
                bypass = value.bypass,
                bypassLocal = value.bypassLocal,
            )
        mutableState.update {
            it.copy(
                proxyAvailable = true,
                proxyLoaded = true,
                proxy = form,
                proxySaved = form,
                proxyPasswordSaved = value.password.isNotEmpty(),
                proxyError = null,
            )
        }
    }

    private fun pathRow(info: SettingsPathInfo): SettingsControlsPathRow =
        SettingsControlsPathRow(
            name = info.name,
            path = info.path,
            size = if (info.bytes > 0) SettingsControlsLogic.bytes(info.bytes) else "",
            free = if (info.freeBytes >= 0) SettingsControlsLogic.bytes(info.freeBytes) else "",
            kind = info.kind,
        )

    private companion object {
        const val TOPIC = "health"
        const val POLL_MILLIS = 30_000L
        const val URL_BASE_MAX = 200
        const val HOST_MAX = 253
        const val SECRET_MAX = 255
        const val BYPASS_MAX = 2000
        const val PORT_DIGITS = 5
        const val MASK = "••••••••"
    }
}
