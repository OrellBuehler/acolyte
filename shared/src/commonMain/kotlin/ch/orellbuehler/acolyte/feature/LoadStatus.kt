package ch.orellbuehler.acolyte.feature

import ch.orellbuehler.acolyte.api.ApiError

enum class Phase {
    LOADING,
    READY,
    FAILED,
}

data class LoadStatus(
    val phase: Phase = Phase.LOADING,
    val refreshing: Boolean = false,
    val error: ApiError? = null,
) {
    internal fun started(): LoadStatus = if (phase == Phase.READY) copy(refreshing = true, error = null) else LoadStatus(Phase.LOADING)

    internal fun failed(error: ApiError): LoadStatus =
        if (phase == Phase.READY) copy(refreshing = false, error = error) else LoadStatus(Phase.FAILED, error = error)

    internal companion object {
        val ready = LoadStatus(Phase.READY)
    }
}

enum class Tone {
    OK,
    WAIT,
    FAIL,
    INFO,
    IDLE,
}
