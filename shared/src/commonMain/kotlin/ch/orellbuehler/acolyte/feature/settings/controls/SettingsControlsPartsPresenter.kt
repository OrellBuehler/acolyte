package ch.orellbuehler.acolyte.feature.settings.controls

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AppEndpoints
import ch.orellbuehler.acolyte.api.Endpoint
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsControlsEndpoints
import ch.orellbuehler.acolyte.api.SettingsSharedEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.EventRecord
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.api.model.SettingsControlAnswer
import ch.orellbuehler.acolyte.api.model.SettingsControlConfirm
import ch.orellbuehler.acolyte.api.model.SettingsStageState
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
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

data class SettingsControlsAsk(
    val kind: SettingsControlsKind,
    val name: String,
    val text: String,
    val expiresInSeconds: Int,
)

data class SettingsControlsNotice(
    val id: Int,
    val kind: SettingsControlsKind,
    val name: String,
    val ok: Boolean,
    val detail: String = "",
    val error: ApiError? = null,
)

data class SettingsControlsPartRow(
    val key: String,
    val part: String,
    val name: String,
    val on: Boolean,
    val run: SettingsControlsRun,
    val tone: Tone,
    val reason: String,
)

data class SettingsControlsStageRow(
    val key: String,
    val stage: String,
    val name: String,
    val run: SettingsControlsRun,
    val tone: Tone,
    val reason: String,
    val pausable: Boolean,
    val paused: Boolean,
)

data class SettingsControlsLinkRow(
    val key: String,
    val kind: SettingsControlsLinkKind,
    val name: String,
    val run: SettingsControlsRun,
    val tone: Tone,
    val reason: String,
    val canTest: Boolean,
)

data class SettingsControlsEventRow(
    val id: Long,
    val summary: String,
    val at: String,
)

data class SettingsControlsPartsState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val safeMode: Boolean = false,
    val parts: List<SettingsControlsPartRow> = emptyList(),
    val stages: List<SettingsControlsStageRow> = emptyList(),
    val links: List<SettingsControlsLinkRow> = emptyList(),
    val events: List<SettingsControlsEventRow> = emptyList(),
    val busyKey: String = "",
    val ask: SettingsControlsAsk? = null,
    val notice: SettingsControlsNotice? = null,
) {
    val busy: Boolean get() = busyKey.isNotEmpty()
}

enum class SettingsControlsStageAction {
    PAUSE,
    RESUME,
    RESTART,
}

class SettingsControlsPartsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
    private val refreshSession: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(SettingsControlsPartsState())
    private val lock = Mutex()
    private var answer = Parts()
    private var states: List<SettingsStageState> = emptyList()
    private var events: List<EventRecord> = emptyList()
    private var notices = 0

    private val flow =
        SettingsControlFlow(
            gate = reauth,
            onBusy = { call -> mutableState.update { it.copy(busyKey = call?.key.orEmpty()) } },
            onAsk = { call, ask ->
                mutableState.update { it.copy(ask = SettingsControlsAsk(call.kind, call.name, ask.text, ask.expiresInSeconds)) }
            },
            onDone = { call, done -> finish(call, done) },
            onFail = { call, error ->
                mutableState.update { it.copy(ask = null, notice = notice(call, ok = false, error = error)) }
            },
        )

    val state: StateFlow<SettingsControlsPartsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.can(Need.USERS)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update { it.copy(status = it.status.started(), allowed = true) }
            coroutineScope {
                val parts = async { api.result(AppEndpoints.parts) }
                val health = async { api.result(SettingsSharedEndpoints.healthStates) }
                val recent =
                    async {
                        api.result(TitleSupportEndpoints.events, "kind" to EVENT_KIND, "limit" to EVENT_LIMIT).valueOrNull()
                    }
                val partsResult = parts.await()
                val healthResult = health.await()
                val eventsResult = recent.await()
                val loaded = partsResult.valueOrNull()
                val reading = healthResult.valueOrNull()
                if (loaded == null || reading == null) {
                    val error = (partsResult as? ApiResult.Failed)?.error ?: (healthResult as ApiResult.Failed).error
                    mutableState.update { it.copy(status = it.status.failed(error)) }
                } else {
                    answer = loaded
                    states = reading.states
                    events = eventsResult?.events ?: events
                    mutableState.update { it.copy(status = LoadStatus.ready) }
                    rebuild()
                }
            }
        }
    }

    suspend fun watch() {
        coroutineScope {
            launch {
                while (true) {
                    delay(POLL_MILLIS)
                    refresh()
                }
            }
            api.live.collect { event ->
                if (event.topic == TOPIC || event.topic == LiveEvent.RESYNC) refresh()
            }
        }
    }

    suspend fun turnOn(part: String) {
        val name = partName(part)
        flow.start(
            SettingsControlCall(SettingsControlsKind.PART_ON, "part:$part", name) {
                SettingsControlSteps.plain(api, SettingsControlsEndpoints.partOn, "part" to part)
            },
        )
    }

    suspend fun turnOff(part: String) = confirmable(SettingsControlsKind.PART_OFF, part, SettingsControlsEndpoints.partOff)

    suspend fun restart(part: String) = confirmable(SettingsControlsKind.PART_RESTART, part, SettingsControlsEndpoints.partRestart)

    suspend fun stage(
        stage: String,
        action: SettingsControlsStageAction,
    ) {
        val name = states.firstOrNull { it.key == "stage:$stage" }?.name ?: stage
        val (kind, endpoint) =
            when (action) {
                SettingsControlsStageAction.PAUSE -> SettingsControlsKind.STAGE_PAUSE to SettingsControlsEndpoints.stagePause
                SettingsControlsStageAction.RESUME -> SettingsControlsKind.STAGE_RESUME to SettingsControlsEndpoints.stageResume
                SettingsControlsStageAction.RESTART -> SettingsControlsKind.STAGE_RESTART to SettingsControlsEndpoints.stageRestart
            }
        flow.start(
            SettingsControlCall(kind, "stage:$stage", name) {
                SettingsControlSteps.plain(api, endpoint, "name" to stage)
            },
        )
    }

    suspend fun reconnect(key: String) {
        val link = SettingsControlsLogic.link(key) ?: return
        val row = states.firstOrNull { it.key == key }
        val kind = if (link.kind == "downloader") SettingsControlsKind.LINK_RECONNECT else SettingsControlsKind.LINK_TEST
        flow.start(
            SettingsControlCall(kind, key, row?.name ?: link.id) {
                SettingsControlSteps.test(api, SettingsControlsEndpoints.linkRestart, "kind" to link.kind, "id" to link.id)
            },
        )
    }

    suspend fun confirm() = flow.confirm()

    fun cancel() {
        flow.cancel()
        mutableState.update { it.copy(ask = null) }
    }

    private suspend fun confirmable(
        kind: SettingsControlsKind,
        part: String,
        endpoint: Endpoint<SettingsControlConfirm, SettingsControlAnswer>,
    ) {
        flow.start(
            SettingsControlCall(kind, "part:$part", partName(part)) { confirm ->
                SettingsControlSteps.send(api, endpoint, confirm, "part" to part)
            },
        )
    }

    private suspend fun finish(
        call: SettingsControlCall,
        done: SettingsControlOutcome.Done,
    ) {
        val ok = !done.rejected
        val detail =
            when (call.kind) {
                SettingsControlsKind.LINK_RECONNECT, SettingsControlsKind.LINK_TEST ->
                    if (ok) SettingsControlsLogic.testLabel(done.text) else done.text
                SettingsControlsKind.PART_RESTART -> if (done.stillRunning > 0) done.text else ""
                else -> ""
            }
        mutableState.update { it.copy(ask = null, notice = notice(call, ok, detail = detail)) }
        refresh()
        if (call.kind == SettingsControlsKind.PART_ON || call.kind == SettingsControlsKind.PART_OFF) refreshSession()
    }

    private fun notice(
        call: SettingsControlCall,
        ok: Boolean,
        detail: String = "",
        error: ApiError? = null,
    ): SettingsControlsNotice = SettingsControlsNotice(++notices, call.kind, call.name, ok, detail, error)

    private fun partName(part: String): String = answer.parts.firstOrNull { it.part == part }?.name ?: part

    private fun rebuild() {
        val pausable = answer.stages.toSet()
        val parts =
            answer.parts.map { view ->
                val run = SettingsControlsLogic.run(view.state, stuck = false)
                SettingsControlsPartRow(
                    key = "part:${view.part}",
                    part = view.part,
                    name = view.name.ifEmpty { view.part },
                    on = view.on,
                    run = run,
                    tone = SettingsControlsLogic.tone(run),
                    reason = view.reason,
                )
            }
        val stages =
            states.filter { it.kind == "stage" }.map { stage ->
                val name = stage.key.removePrefix("stage:")
                val run = SettingsControlsLogic.run(stage.state, stage.stuck)
                SettingsControlsStageRow(
                    key = stage.key,
                    stage = name,
                    name = stage.name.ifEmpty { name },
                    run = run,
                    tone = SettingsControlsLogic.tone(run),
                    reason = stage.reason,
                    pausable = name in pausable,
                    paused = stage.state == "off",
                )
            }
        val links =
            states.filter { it.kind != "stage" && it.kind != "part" }.map { link ->
                val run = SettingsControlsLogic.run(link.state, link.stuck)
                SettingsControlsLinkRow(
                    key = link.key,
                    kind = SettingsControlsLogic.linkKind(link.kind),
                    name = link.name,
                    run = run,
                    tone = SettingsControlsLogic.tone(run),
                    reason = link.reason,
                    canTest = SettingsControlsLogic.link(link.key) != null,
                )
            }
        val recent = events.map { SettingsControlsEventRow(it.id, it.summary, it.at) }
        val safe = answer.safeMode || account()?.safeMode == true
        mutableState.update { it.copy(safeMode = safe, parts = parts, stages = stages, links = links, events = recent) }
    }

    private companion object {
        const val TOPIC = "health"
        const val POLL_MILLIS = 15_000L
        const val EVENT_KIND = "control_used"
        const val EVENT_LIMIT = 15
    }
}
