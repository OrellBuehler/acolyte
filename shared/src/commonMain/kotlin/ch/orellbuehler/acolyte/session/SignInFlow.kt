package ch.orellbuehler.acolyte.session

import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.Credentials
import ch.orellbuehler.acolyte.api.model.PasswordSignIn
import ch.orellbuehler.acolyte.api.model.SignInAnswer
import ch.orellbuehler.acolyte.api.model.TwoFactorSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

enum class SignInStep {
    SERVER,
    CREDENTIALS,
    TWO_FACTOR,
    DONE,
}

enum class SignInWay(
    internal val wire: String,
    val inApp: Boolean,
) {
    PASSWORD("password", true),
    JELLYFIN("jellyfin", true),
    EMBY("emby", true),
    PLEX("plex", false),
    OIDC("oidc", false),
    FORWARD_AUTH("forward_auth", false),
}

enum class SignInErrorCode {
    INVALID_ADDRESS,
    UNREACHABLE,
    TLS,
    NOT_HOLYARRS,
    NEEDS_SETUP,
    HOST_REFUSED,
    WRONG_CREDENTIALS,
    WRONG_CODE,
    SIGN_IN_ENDED,
    KEY_REJECTED,
    NOT_ALLOWED,
    WAY_OFF,
    RATE_LIMITED,
    SERVER,
}

data class SignInError(
    val code: SignInErrorCode,
    val message: String = "",
    val retryAfterSeconds: Int? = null,
)

data class SignInState(
    val step: SignInStep = SignInStep.SERVER,
    val serverInput: String = "",
    val serverUrl: String = "",
    val serverName: String = "",
    val ways: List<SignInWay> = emptyList(),
    val otherWays: List<SignInWay> = emptyList(),
    val userName: String = "",
    val hostRefused: String? = null,
    val busy: Boolean = false,
    val error: SignInError? = null,
)

class SignInFlow internal constructor(
    private val sessions: SessionManager,
) {
    private val lock = Mutex()
    private var client: ApiClient? = null
    private var ticket: String? = null
    private var pendingMethod = SignInMethod.PASSWORD

    private val mutableState: MutableStateFlow<SignInState>

    init {
        val (server, user) = sessions.lastConnection()
        mutableState =
            MutableStateFlow(
                SignInState(
                    serverInput = server?.let { ServerAddress.displayName(it) }.orEmpty(),
                    userName = user.orEmpty(),
                ),
            )
    }

    val state: StateFlow<SignInState> = mutableState.asStateFlow()

    suspend fun connect(address: String) {
        attempt {
            val candidates = ServerAddress.candidates(address)
            if (candidates.isEmpty()) {
                fail(SignInError(SignInErrorCode.INVALID_ADDRESS))
                return@attempt
            }
            mutableState.update { it.copy(serverInput = address.trim()) }
            var best: ApiError? = null
            for (url in candidates) {
                val probe = sessions.newClient(url, Credentials())
                when (val result = probe.result(AuthEndpoints.state)) {
                    is ApiResult.Ok -> {
                        val answer = result.value
                        when {
                            answer.ways.isEmpty() -> best = better(best, ApiError(ApiErrorCode.INVALID_RESPONSE, 200))
                            answer.needsSetup -> {
                                probe.close()
                                fail(SignInError(SignInErrorCode.NEEDS_SETUP))
                                return@attempt
                            }
                            else -> {
                                replaceClient(probe)
                                val ways = answer.ways.mapNotNull { wire -> SignInWay.entries.firstOrNull { it.wire == wire } }
                                mutableState.update {
                                    it.copy(
                                        step = SignInStep.CREDENTIALS,
                                        serverUrl = url,
                                        serverName = ServerAddress.displayName(url),
                                        ways = ways.filter { way -> way.inApp }.ifEmpty { listOf(SignInWay.PASSWORD) },
                                        otherWays = ways.filterNot { way -> way.inApp },
                                        hostRefused = answer.hostRefused?.takeIf { refused -> refused.isNotBlank() },
                                        busy = false,
                                        error = null,
                                    )
                                }
                                return@attempt
                            }
                        }
                    }
                    is ApiResult.Failed -> best = better(best, result.error)
                }
                probe.close()
            }
            fail(serverError(best))
        }
    }

    suspend fun signIn(
        name: String,
        password: String,
        way: SignInWay = SignInWay.PASSWORD,
    ) {
        attempt {
            val current = client ?: return@attempt
            val body = PasswordSignIn(name.trim(), password)
            mutableState.update { it.copy(userName = name.trim()) }
            val result =
                when (way) {
                    SignInWay.JELLYFIN, SignInWay.EMBY -> current.result(AuthEndpoints.mediaServerLogin, body, "kind" to way.wire)
                    else -> current.result(AuthEndpoints.login, body)
                }
            pendingMethod = if (way == SignInWay.PASSWORD) SignInMethod.PASSWORD else SignInMethod.MEDIA_SERVER
            handleAnswer(current, result)
        }
    }

    suspend fun verify(code: String) {
        attempt {
            val current = client ?: return@attempt
            val pending = ticket ?: return@attempt
            val cleaned = code.trim().replace(" ", "")
            handleAnswer(current, current.result(AuthEndpoints.twoFactor, TwoFactorSignIn(pending, cleaned)))
        }
    }

    suspend fun useApiKey(key: String) {
        attempt {
            val url = mutableState.value.serverUrl.ifEmpty { return@attempt }
            val trimmed = key.trim()
            if (trimmed.isEmpty()) {
                fail(SignInError(SignInErrorCode.KEY_REJECTED))
                return@attempt
            }
            val keyed = sessions.newClient(url, Credentials(apiKey = trimmed))
            val error = sessions.establish(keyed, StoredConnection(baseUrl = url, method = SignInMethod.API_KEY, apiKey = trimmed))
            if (error == null) {
                replaceClient(null)
                mutableState.update { it.copy(step = SignInStep.DONE, busy = false, error = null) }
            } else {
                keyed.close()
                fail(
                    when (error.code) {
                        ApiErrorCode.UNAUTHORIZED -> SignInError(SignInErrorCode.KEY_REJECTED, error.message)
                        ApiErrorCode.FORBIDDEN -> SignInError(SignInErrorCode.NOT_ALLOWED, error.message)
                        else -> serverError(error)
                    },
                )
            }
        }
    }

    fun back() {
        val step = mutableState.value.step
        if (mutableState.value.busy) return
        when (step) {
            SignInStep.TWO_FACTOR -> {
                ticket = null
                mutableState.update { it.copy(step = SignInStep.CREDENTIALS, error = null) }
            }
            SignInStep.CREDENTIALS -> {
                replaceClient(null)
                mutableState.update { it.copy(step = SignInStep.SERVER, error = null) }
            }
            else -> Unit
        }
    }

    fun clearError() {
        mutableState.update { it.copy(error = null) }
    }

    fun close() {
        replaceClient(null)
    }

    private suspend inline fun attempt(crossinline block: suspend () -> Unit) {
        if (!lock.tryLock()) return
        try {
            mutableState.update { it.copy(busy = true, error = null) }
            block()
        } finally {
            mutableState.update { it.copy(busy = false) }
            lock.unlock()
        }
    }

    private suspend fun handleAnswer(
        current: ApiClient,
        result: ApiResult<SignInAnswer>,
    ) {
        when (result) {
            is ApiResult.Ok -> {
                val answer = result.value
                if (answer.twoFactor && answer.ticket.isNotEmpty()) {
                    ticket = answer.ticket
                    mutableState.update { it.copy(step = SignInStep.TWO_FACTOR) }
                    return
                }
                current.credentials.csrf = answer.csrf
                val connection =
                    StoredConnection(
                        baseUrl = current.baseUrl,
                        userName = answer.name.ifEmpty { mutableState.value.userName },
                        method = pendingMethod,
                    )
                val error = sessions.establish(current, connection)
                if (error == null) {
                    client = null
                    ticket = null
                    mutableState.update { it.copy(step = SignInStep.DONE, error = null) }
                } else {
                    fail(serverError(error))
                }
            }
            is ApiResult.Failed -> {
                val error = result.error
                val inTwoFactor = mutableState.value.step == SignInStep.TWO_FACTOR
                val code =
                    when {
                        error.code == ApiErrorCode.RATE_LIMITED -> SignInErrorCode.RATE_LIMITED
                        error.code == ApiErrorCode.UNAUTHORIZED && error.message.contains("ended") -> SignInErrorCode.SIGN_IN_ENDED
                        error.code == ApiErrorCode.UNAUTHORIZED && inTwoFactor -> SignInErrorCode.WRONG_CODE
                        error.code == ApiErrorCode.UNAUTHORIZED -> SignInErrorCode.WRONG_CREDENTIALS
                        error.code == ApiErrorCode.FORBIDDEN && error.message.contains("host", ignoreCase = true) ->
                            SignInErrorCode.HOST_REFUSED
                        error.code == ApiErrorCode.FORBIDDEN -> SignInErrorCode.NOT_ALLOWED
                        error.code == ApiErrorCode.NOT_FOUND -> SignInErrorCode.WAY_OFF
                        else -> null
                    }
                if (code == SignInErrorCode.SIGN_IN_ENDED) {
                    ticket = null
                    mutableState.update { it.copy(step = SignInStep.CREDENTIALS) }
                }
                fail(code?.let { SignInError(it, error.message, error.retryAfterSeconds) } ?: serverError(error))
            }
        }
    }

    private fun fail(error: SignInError) {
        mutableState.update { it.copy(error = error) }
    }

    private fun replaceClient(next: ApiClient?) {
        val previous = client
        client = next
        if (previous != null && previous !== next) previous.close()
    }

    private fun better(
        current: ApiError?,
        next: ApiError,
    ): ApiError {
        if (current == null) return next

        fun rank(error: ApiError) =
            when {
                error.status > 0 -> 3
                error.code == ApiErrorCode.TLS -> 2
                error.code == ApiErrorCode.TIMEOUT -> 1
                else -> 0
            }
        return if (rank(next) > rank(current)) next else current
    }

    private fun serverError(error: ApiError?): SignInError {
        error ?: return SignInError(SignInErrorCode.UNREACHABLE)
        val code =
            when {
                error.code == ApiErrorCode.TLS -> SignInErrorCode.TLS
                error.code == ApiErrorCode.OFFLINE || error.code == ApiErrorCode.TIMEOUT -> SignInErrorCode.UNREACHABLE
                error.code == ApiErrorCode.FORBIDDEN && error.message.contains("host", ignoreCase = true) -> SignInErrorCode.HOST_REFUSED
                error.code == ApiErrorCode.FORBIDDEN -> SignInErrorCode.NOT_ALLOWED
                error.code == ApiErrorCode.RATE_LIMITED -> SignInErrorCode.RATE_LIMITED
                error.code == ApiErrorCode.NOT_FOUND ||
                    error.code == ApiErrorCode.INVALID_RESPONSE ||
                    error.code == ApiErrorCode.BAD_REQUEST ->
                    SignInErrorCode.NOT_HOLYARRS
                else -> SignInErrorCode.SERVER
            }
        return SignInError(code, error.message, error.retryAfterSeconds)
    }
}
