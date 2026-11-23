package ch.orellbuehler.acolyte.feature.health

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.HealthEndpoints
import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.live.LiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HealthState(
    val status: LoadStatus = LoadStatus(),
    val checks: List<HealthCheck> = emptyList(),
    val running: Boolean = false,
    val actionError: ApiError? = null,
) {
    val overall: Tone
        get() =
            when {
                checks.any { it.tone == Tone.FAIL } -> Tone.FAIL
                checks.any { it.tone == Tone.WAIT } -> Tone.WAIT
                checks.isEmpty() -> Tone.IDLE
                else -> Tone.OK
            }

    val isEmpty: Boolean get() = status.phase == Phase.READY && checks.isEmpty()
}

val HealthCheck.tone: Tone
    get() =
        when (state) {
            "ok" -> Tone.OK
            "warning" -> Tone.WAIT
            "error" -> Tone.FAIL
            else -> Tone.IDLE
        }

class HealthPresenter internal constructor(
    private val api: Api,
) {
    private val mutableState = MutableStateFlow(HealthState())
    private val lock = Mutex()

    val state: StateFlow<HealthState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            api
                .result(HealthEndpoints.checks)
                .onSuccess { answer -> mutableState.update { it.copy(status = LoadStatus.ready, checks = answer.checks) } }
                .onFailure { error -> mutableState.update { it.copy(status = it.status.failed(error)) } }
        }
    }

    suspend fun runChecks() {
        lock.withLock {
            mutableState.update { it.copy(running = true, actionError = null) }
            api
                .result(HealthEndpoints.runChecks)
                .onSuccess { answer -> mutableState.update { it.copy(status = LoadStatus.ready, checks = answer.checks, running = false) } }
                .onFailure { error -> mutableState.update { it.copy(running = false, actionError = error) } }
        }
    }

    suspend fun watch() {
        api.live.collect { event ->
            if (event.topic == TOPIC || event.topic == LiveEvent.RESYNC) refresh()
        }
    }

    private companion object {
        const val TOPIC = "health"
    }
}
