package ch.orellbuehler.acolyte.feature.settings

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode

data class SettingsReauthState(
    val needed: Boolean = false,
    val busy: Boolean = false,
    val error: ApiError? = null,
)

internal class SettingsReauthGate(
    private val reauth: suspend (String, String?) -> ApiError?,
    private val publish: (SettingsReauthState) -> Unit,
) {
    private var pending: (suspend () -> ApiError?)? = null
    private var pendingFailure: (ApiError) -> Unit = {}

    suspend fun guard(
        onFailure: (ApiError) -> Unit = {},
        action: suspend () -> ApiError?,
    ) {
        val failure = action()
        when {
            failure == null -> {
                pending = null
                publish(SettingsReauthState())
            }
            failure.code == ApiErrorCode.REAUTH -> {
                pending = action
                pendingFailure = onFailure
                publish(SettingsReauthState(needed = true))
            }
            else -> {
                pending = null
                publish(SettingsReauthState())
                onFailure(failure)
            }
        }
    }

    suspend fun confirm(
        password: String,
        code: String,
    ) {
        val action = pending ?: return
        val onFailure = pendingFailure
        publish(SettingsReauthState(needed = true, busy = true))
        val failure = reauth(password, code.trim().ifEmpty { null })
        if (failure != null) {
            publish(SettingsReauthState(needed = true, error = failure))
            return
        }
        pending = null
        guard(onFailure, action)
    }

    fun cancel() {
        pending = null
        publish(SettingsReauthState())
    }
}
