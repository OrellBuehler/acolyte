package ch.orellbuehler.acolyte.feature.settings.controls

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsControlsEndpoints
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SettingsControlsWait {
    STOPPING,
    WAITING,
    BACK,
}

data class SettingsControlsStopped(
    val kind: SettingsControlsKind,
    val text: String,
    val wait: SettingsControlsWait,
)

data class SettingsControlsDangerState(
    val allowed: Boolean = true,
    val busyKind: SettingsControlsKind? = null,
    val ask: SettingsControlsAsk? = null,
    val notice: SettingsControlsNotice? = null,
    val stopped: SettingsControlsStopped? = null,
) {
    val busy: Boolean get() = busyKind != null
}

class SettingsControlsDangerPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
    private val refreshSession: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(SettingsControlsDangerState(allowed = account()?.can(Need.USERS) == true))
    private var notices = 0

    private val flow =
        SettingsControlFlow(
            gate = reauth,
            onBusy = { call -> mutableState.update { it.copy(busyKind = call?.kind) } },
            onAsk = { call, ask ->
                mutableState.update { it.copy(ask = SettingsControlsAsk(call.kind, call.name, ask.text, ask.expiresInSeconds)) }
            },
            onDone = { call, done ->
                mutableState.update {
                    it.copy(ask = null, stopped = SettingsControlsStopped(call.kind, done.text, SettingsControlsWait.STOPPING))
                }
            },
            onFail = { call, error ->
                mutableState.update {
                    it.copy(ask = null, notice = SettingsControlsNotice(++notices, call.kind, call.name, ok = false, error = error))
                }
            },
        )

    val state: StateFlow<SettingsControlsDangerState> = mutableState.asStateFlow()

    fun load() {
        mutableState.update { it.copy(allowed = account()?.can(Need.USERS) == true) }
    }

    suspend fun request(kind: SettingsControlsKind) {
        val endpoint =
            when (kind) {
                SettingsControlsKind.APP_RESTART -> SettingsControlsEndpoints.appRestart
                SettingsControlsKind.APP_SAFE_MODE -> SettingsControlsEndpoints.appSafeMode
                SettingsControlsKind.APP_SHUTDOWN -> SettingsControlsEndpoints.appShutdown
                else -> return
            }
        flow.start(SettingsControlCall(kind, kind.name, "") { confirm -> SettingsControlSteps.send(api, endpoint, confirm) })
    }

    suspend fun confirm() = flow.confirm()

    fun cancel() {
        flow.cancel()
        mutableState.update { it.copy(ask = null) }
    }

    fun dismissStopped() {
        mutableState.update { it.copy(stopped = null) }
    }

    suspend fun waitForServer() {
        val stopped = mutableState.value.stopped ?: return
        if (stopped.wait == SettingsControlsWait.BACK) return
        var waited = 0L
        while (mutableState.value.stopped?.wait == SettingsControlsWait.STOPPING) {
            delay(DOWN_POLL_MILLIS)
            waited += DOWN_POLL_MILLIS
            when {
                !answers() -> setWait(SettingsControlsWait.WAITING)
                waited >= DOWN_GRACE_MILLIS -> {
                    returned()
                    return
                }
            }
        }
        while (mutableState.value.stopped?.wait == SettingsControlsWait.WAITING) {
            delay(UP_POLL_MILLIS)
            if (answers()) {
                returned()
                return
            }
        }
    }

    private suspend fun returned() {
        setWait(SettingsControlsWait.BACK)
        refreshSession()
    }

    private fun setWait(wait: SettingsControlsWait) {
        mutableState.update { state -> state.copy(stopped = state.stopped?.copy(wait = wait)) }
    }

    private suspend fun answers(): Boolean =
        when (val result = api.result(AuthEndpoints.me)) {
            is ApiResult.Ok -> true
            is ApiResult.Failed -> result.error.status in 1 until SERVER_ERROR
        }

    private companion object {
        const val DOWN_POLL_MILLIS = 1_000L
        const val DOWN_GRACE_MILLIS = 12_000L
        const val UP_POLL_MILLIS = 2_000L
        const val SERVER_ERROR = 500
    }
}
