package ch.orellbuehler.acolyte.feature.settings.controls

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Endpoint
import ch.orellbuehler.acolyte.api.model.SettingsControlAnswer
import ch.orellbuehler.acolyte.api.model.SettingsControlConfirm
import ch.orellbuehler.acolyte.api.model.SettingsLinkTest
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthGate

internal sealed interface SettingsControlOutcome {
    data class Done(
        val text: String = "",
        val stillRunning: Int = 0,
        val rejected: Boolean = false,
    ) : SettingsControlOutcome

    data class Asking(
        val confirm: String,
        val text: String,
        val expiresInSeconds: Int,
    ) : SettingsControlOutcome

    data class Failed(
        val error: ApiError,
    ) : SettingsControlOutcome
}

internal object SettingsControlSteps {
    const val EXPIRES_SECONDS = 60

    fun outcome(result: ApiResult<SettingsControlAnswer>): SettingsControlOutcome =
        when (result) {
            is ApiResult.Failed -> SettingsControlOutcome.Failed(result.error)
            is ApiResult.Ok -> {
                val answer = result.value
                if (answer.confirm.isNotEmpty()) {
                    SettingsControlOutcome.Asking(
                        answer.confirm,
                        SettingsControlsLogic.question(answer.text),
                        answer.expiresInSeconds.takeIf { it > 0 } ?: EXPIRES_SECONDS,
                    )
                } else {
                    SettingsControlOutcome.Done(answer.text, answer.stillRunning)
                }
            }
        }

    suspend fun send(
        api: Api,
        endpoint: Endpoint<SettingsControlConfirm, SettingsControlAnswer>,
        confirm: String?,
        vararg params: Pair<String, Any?>,
    ): SettingsControlOutcome = outcome(api.result(endpoint, SettingsControlConfirm(confirm), *params))

    suspend fun plain(
        api: Api,
        endpoint: Endpoint<Unit, SettingsControlAnswer>,
        vararg params: Pair<String, Any?>,
    ): SettingsControlOutcome = outcome(api.result(endpoint, *params))

    suspend fun test(
        api: Api,
        endpoint: Endpoint<Unit, SettingsLinkTest>,
        vararg params: Pair<String, Any?>,
    ): SettingsControlOutcome =
        when (val result = api.result(endpoint, *params)) {
            is ApiResult.Failed -> SettingsControlOutcome.Failed(result.error)
            is ApiResult.Ok -> SettingsControlOutcome.Done(SettingsControlsLogic.linkSaid(result.value), rejected = !result.value.ok)
        }
}

internal class SettingsControlCall(
    val kind: SettingsControlsKind,
    val key: String,
    val name: String,
    val send: suspend (String?) -> SettingsControlOutcome,
)

internal class SettingsControlFlow(
    private val gate: SettingsReauthGate,
    private val onBusy: (SettingsControlCall?) -> Unit,
    private val onAsk: (SettingsControlCall, SettingsControlOutcome.Asking) -> Unit,
    private val onDone: suspend (SettingsControlCall, SettingsControlOutcome.Done) -> Unit,
    private val onFail: (SettingsControlCall, ApiError) -> Unit,
) {
    private var pending: Pair<SettingsControlCall, String>? = null
    private var running = false

    val isAsking: Boolean get() = pending != null

    suspend fun start(call: SettingsControlCall) {
        if (running) return
        pending = null
        run(call, null)
    }

    suspend fun confirm() {
        val ask = pending ?: return
        if (running) return
        run(ask.first, ask.second)
    }

    fun cancel() {
        pending = null
    }

    suspend fun confirmReauth(
        password: String,
        code: String,
    ) = gate.confirm(password, code)

    fun cancelReauth() {
        pending = null
        gate.cancel()
    }

    private suspend fun run(
        call: SettingsControlCall,
        confirm: String?,
    ) {
        running = true
        try {
            gate.guard(onFailure = { error -> fail(call, error) }) {
                onBusy(call)
                when (val outcome = call.send(confirm)) {
                    is SettingsControlOutcome.Asking -> {
                        pending = call to outcome.confirm
                        onBusy(null)
                        onAsk(call, outcome)
                        null
                    }
                    is SettingsControlOutcome.Done -> {
                        pending = null
                        onBusy(null)
                        onDone(call, outcome)
                        null
                    }
                    is SettingsControlOutcome.Failed -> {
                        onBusy(null)
                        outcome.error
                    }
                }
            }
        } finally {
            running = false
        }
    }

    private fun fail(
        call: SettingsControlCall,
        error: ApiError,
    ) {
        pending = null
        onFail(call, error)
    }
}
