package ch.orellbuehler.acolyte.feature.sources

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DownloadsLinkEndpoints
import ch.orellbuehler.acolyte.api.Endpoint
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.SourcesEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsSourceTest
import ch.orellbuehler.acolyte.api.model.SourceRef
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.mayEditAcquisition
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

data class SettingsSourcesState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canEdit: Boolean = false,
    val partOff: Boolean = false,
    val rows: List<SettingsSourceRow> = emptyList(),
    val downloaders: List<SettingsSourceDownloader>? = null,
    val editing: SettingsSourceForm? = null,
    val editToken: Int = 0,
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val notice: SettingsSourcesNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty() && editing == null

    val hasRows: Boolean get() = rows.isNotEmpty()

    val showOff: Boolean get() = partOff

    val showBridgeAndCheck: Boolean get() = !partOff

    val canAdd: Boolean get() = canEdit && editing == null
}

class SettingsSourcesPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsSourcesState())
    private val lock = Mutex()
    private var sources: List<SourceRef> = emptyList()
    private var outcomes: Map<Long, SettingsSourceOutcome> = emptyMap()
    private var testing: Set<Long> = emptySet()
    private var busy: Set<Long> = emptySet()
    private var seq = 0

    val state: StateFlow<SettingsSourcesState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() = reload(quiet = false)

    suspend fun reloadQuietly() = reload(quiet = true)

    private suspend fun reload(quiet: Boolean) {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(AcquisitionPage.SOURCES)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update {
                it.copy(
                    status = if (quiet) it.status else it.status.started(),
                    allowed = true,
                    canEdit = me.mayEditAcquisition,
                    partOff = !me.isOn(Part.SOURCES),
                )
            }
            coroutineScope {
                val downloaders =
                    async {
                        api.result(DownloadsLinkEndpoints.downloaders).valueOrNull()?.downloaders?.map {
                            SettingsSourceDownloader(it.id, it.name, it.protocol)
                        }
                    }
                when (val result = api.result(DownloadsLinkEndpoints.sources)) {
                    is ApiResult.Ok -> {
                        sources = result.value.sources
                        mutableState.update { it.copy(status = LoadStatus.ready, downloaders = downloaders.await()) }
                    }
                    is ApiResult.Failed -> {
                        downloaders.cancel()
                        if (!quiet) mutableState.update { it.copy(status = it.status.failed(result.error)) }
                    }
                }
            }
            rebuild()
        }
    }

    private fun rebuild() {
        mutableState.update { s ->
            s.copy(rows = sources.map { settingsSourceRow(it, outcomes[it.id], it.id in testing, it.id in busy) })
        }
    }

    private fun notice(
        kind: SettingsSourcesNoticeKind,
        name: String = "",
        error: ApiError? = null,
    ) {
        seq++
        mutableState.update { it.copy(notice = SettingsSourcesNotice(seq, kind, name = name, error = error)) }
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    fun settled(form: SettingsSourceForm): SettingsSourceForm = form.settled(mutableState.value.downloaders)

    fun startAdd() {
        if (!mutableState.value.canEdit) return
        open(SettingsSourceForm.blank())
    }

    fun startChange(id: Long) {
        if (!mutableState.value.canEdit) return
        val source = sources.firstOrNull { it.id == id } ?: return
        open(SettingsSourceForm.from(source))
    }

    private fun open(form: SettingsSourceForm) {
        mutableState.update {
            it.copy(editing = form.settled(it.downloaders), editToken = it.editToken + 1, saveError = null, saving = false)
        }
    }

    fun cancelEdit() {
        mutableState.update { it.copy(editing = null, saveError = null, saving = false) }
    }

    suspend fun save(form: SettingsSourceForm): Boolean {
        val s = mutableState.value
        if (!s.canEdit || s.saving || !form.canSave) return false
        mutableState.update { it.copy(saving = true, saveError = null) }
        val body = form.body()
        val result =
            if (form.isNew) api.result(SourcesEndpoints.add, body) else api.result(SourcesEndpoints.change, body, "id" to form.id)
        return when (result) {
            is ApiResult.Ok -> {
                val saved = result.value
                mutableState.update { it.copy(saving = false, editing = null) }
                notice(SettingsSourcesNoticeKind.SAVED, saved.name)
                reload(quiet = true)
                test(saved.id)
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(saving = false, saveError = result.error) }
                false
            }
        }
    }

    suspend fun test(id: Long) = runTest(id, SourcesEndpoints.test)

    suspend fun tryNow(id: Long) = runTest(id, SourcesEndpoints.tryNow)

    private suspend fun runTest(
        id: Long,
        endpoint: Endpoint<Unit, SettingsSourceTest>,
    ) {
        if (id in testing) return
        testing = testing + id
        rebuild()
        val outcome =
            when (val result = api.result(endpoint, "id" to id)) {
                is ApiResult.Ok -> SettingsSourcesLogic.outcome(result.value)
                is ApiResult.Failed -> SettingsSourcesLogic.failedOutcome(result.error)
            }
        outcomes = outcomes + (id to outcome)
        testing = testing - id
        reload(quiet = true)
    }

    suspend fun setPaused(
        id: Long,
        paused: Boolean,
    ) {
        busy = busy + id
        rebuild()
        val result = api.result(if (paused) SourcesEndpoints.pause else SourcesEndpoints.resume, "id" to id)
        busy = busy - id
        if (result is ApiResult.Failed) {
            notice(if (paused) SettingsSourcesNoticeKind.PAUSE_FAILED else SettingsSourcesNoticeKind.RESUME_FAILED, error = result.error)
        }
        reload(quiet = true)
    }

    suspend fun remove(id: Long) {
        busy = busy + id
        rebuild()
        val result = api.result(SourcesEndpoints.remove, "id" to id)
        busy = busy - id
        when (result) {
            is ApiResult.Ok -> {
                sources = sources.filter { it.id != id }
                outcomes = outcomes - id
            }
            is ApiResult.Failed -> notice(SettingsSourcesNoticeKind.REMOVE_FAILED, error = result.error)
        }
        rebuild()
        reload(quiet = true)
    }

    fun nameOf(id: Long): String = sources.firstOrNull { it.id == id }?.name.orEmpty()

    suspend fun watch() {
        coroutineScope {
            val poke = Channel<Unit>(Channel.CONFLATED)
            launch {
                api.live.collect { event ->
                    if (event.topic == TOPIC || event.topic == LiveEvent.RESYNC) poke.trySend(Unit)
                }
            }
            launch {
                for (ignored in poke) {
                    delay(1.seconds)
                    if (mutableState.value.status.phase == Phase.READY) reload(quiet = true)
                }
            }
        }
    }

    internal companion object {
        const val TOPIC = "source"
    }
}
