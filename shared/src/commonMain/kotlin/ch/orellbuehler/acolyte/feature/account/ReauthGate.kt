package ch.orellbuehler.acolyte.feature.account

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ReauthMode {
    PASSWORD,
    SIGN_IN_AGAIN,
    SET_PASSWORD,
}

data class ReauthState(
    val visible: Boolean = false,
    val mode: ReauthMode = ReauthMode.PASSWORD,
    val needsCode: Boolean = false,
    val busy: Boolean = false,
    val error: ApiError? = null,
)

class ReauthGate internal constructor(
    private val reauth: suspend (String, String?) -> ApiError?,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(ReauthState())
    private val prompting = Mutex()
    private var waiting: CompletableDeferred<Boolean>? = null

    val state: StateFlow<ReauthState> = mutableState.asStateFlow()

    internal suspend fun <T> guard(block: suspend () -> ApiResult<T>): ApiResult<T> {
        val first = block()
        if (first !is ApiResult.Failed || first.error.code != ApiErrorCode.REAUTH) return first
        val proven =
            prompting.withLock {
                val me = account()
                val deferred = CompletableDeferred<Boolean>()
                waiting = deferred
                mutableState.value =
                    ReauthState(
                        visible = true,
                        mode =
                            when {
                                me?.hasPassword == false && OUTSIDE.any { me.authMethod.startsWith(it) } -> ReauthMode.SIGN_IN_AGAIN
                                me?.hasPassword == false -> ReauthMode.SET_PASSWORD
                                else -> ReauthMode.PASSWORD
                            },
                        needsCode = me?.twoFactor == true,
                    )
                try {
                    deferred.await()
                } finally {
                    waiting = null
                    mutableState.value = ReauthState()
                }
            }
        return if (proven) block() else first
    }

    suspend fun confirm(
        password: String,
        code: String,
    ) {
        val pending = waiting ?: return
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        val error = reauth(password, code.trim().ifEmpty { null })
        if (error == null) {
            pending.complete(true)
        } else {
            mutableState.update { it.copy(busy = false, error = error) }
        }
    }

    fun cancel() {
        waiting?.complete(false)
    }

    private companion object {
        val OUTSIDE = setOf("plex", "jellyfin", "emby", "oidc", "forward_auth")
    }
}
