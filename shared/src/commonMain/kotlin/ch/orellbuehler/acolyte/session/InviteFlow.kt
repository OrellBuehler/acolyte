package ch.orellbuehler.acolyte.session

import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.Credentials
import ch.orellbuehler.acolyte.api.SignInExtraEndpoints
import ch.orellbuehler.acolyte.api.model.AccountPlexFinish
import ch.orellbuehler.acolyte.api.model.InviteJoinBody
import ch.orellbuehler.acolyte.api.model.PasswordSignIn
import ch.orellbuehler.acolyte.api.model.SignInAnswer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

data class InviteTarget(
    val baseUrl: String,
    val token: String,
)

object InviteLink {
    private val TOKEN = Regex("/invite/([A-Za-z0-9_~.-]+)")

    fun parse(text: String): InviteTarget? {
        val trimmed = text.trim()
        val match = TOKEN.find(trimmed) ?: return null
        val prefix = trimmed.substring(0, match.range.first)
        val base = ServerAddress.candidates(prefix).firstOrNull() ?: return null
        return InviteTarget(base, match.groupValues[1])
    }
}

enum class InvitePhase {
    LOADING,
    OPEN,
    GONE,
    DONE,
}

enum class InviteKind {
    ADMIN,
    KID,
    MEMBER,
}

data class InviteState(
    val phase: InvitePhase = InvitePhase.LOADING,
    val serverName: String = "",
    val kind: InviteKind = InviteKind.MEMBER,
    val kidLimit: String = "",
    val ways: List<SignInWay> = emptyList(),
    val minPasswordLength: Int = 8,
    val signedInAs: String? = null,
    val plexUrl: String? = null,
    val busy: Boolean = false,
    val error: SignInError? = null,
)

class InviteFlow internal constructor(
    private val sessions: SessionManager,
    private val target: InviteTarget,
) {
    private val lock = Mutex()
    private var client: ApiClient? = null

    private val mutableState = MutableStateFlow(InviteState(serverName = ServerAddress.displayName(target.baseUrl)))

    val state: StateFlow<InviteState> = mutableState.asStateFlow()

    suspend fun load() {
        attempt {
            replaceClient(null)
            val probe = sessions.newClient(target.baseUrl, Credentials())
            val auth =
                when (val result = probe.result(AuthEndpoints.state)) {
                    is ApiResult.Ok -> result.value
                    is ApiResult.Failed -> {
                        probe.close()
                        return@attempt fail(error(result.error))
                    }
                }
            when (val result = probe.result(SignInExtraEndpoints.inviteLook, "token" to target.token)) {
                is ApiResult.Ok -> {
                    client = probe
                    val look = result.value
                    mutableState.update {
                        it.copy(
                            phase = InvitePhase.OPEN,
                            kind =
                                when {
                                    look.role == "admin" -> InviteKind.ADMIN
                                    look.maxRating.isNotEmpty() -> InviteKind.KID
                                    else -> InviteKind.MEMBER
                                },
                            kidLimit = look.maxRating,
                            ways =
                                auth.ways.mapNotNull { wire -> SignInWay.entries.firstOrNull { way -> way.wire == wire } }.filter {
                                    it == SignInWay.PLEX || it == SignInWay.JELLYFIN || it == SignInWay.EMBY
                                },
                            minPasswordLength = auth.minPasswordLength,
                            signedInAs = sessions.account?.takeIf { account -> account.serverUrl == target.baseUrl }?.userName,
                        )
                    }
                }
                is ApiResult.Failed -> {
                    probe.close()
                    if (result.error.code == ApiErrorCode.NOT_FOUND) {
                        mutableState.update { it.copy(phase = InvitePhase.GONE) }
                    } else {
                        fail(error(result.error))
                    }
                }
            }
        }
    }

    suspend fun signOutAndJoin() {
        sessions.signOut()
        mutableState.update { it.copy(signedInAs = null) }
    }

    suspend fun join(
        name: String,
        password: String,
    ) {
        attempt {
            val current = client ?: return@attempt
            val trimmed = name.trim()
            when {
                !AccountRules.validName(trimmed) -> return@attempt fail(SignInError(SignInErrorCode.INVALID_NAME))
                !AccountRules.validPassword(password, mutableState.value.minPasswordLength) ->
                    return@attempt fail(SignInError(SignInErrorCode.PASSWORD_SHORT))
            }
            val result = current.result(SignInExtraEndpoints.inviteJoin, InviteJoinBody(trimmed, password), "token" to target.token)
            finish(current, result, trimmed, SignInMethod.PASSWORD)
        }
    }

    suspend fun joinMediaServer(
        way: SignInWay,
        name: String,
        password: String,
    ) {
        attempt {
            val current = client ?: return@attempt
            val result =
                current.result(
                    SignInExtraEndpoints.inviteMediaServer,
                    PasswordSignIn(name.trim(), password),
                    "token" to target.token,
                    "kind" to way.wire,
                )
            finish(current, result, name.trim(), SignInMethod.MEDIA_SERVER)
        }
    }

    suspend fun joinPlex() {
        attempt {
            val current = client ?: return@attempt
            val started =
                when (val result = current.result(SignInExtraEndpoints.invitePlexStart, "token" to target.token)) {
                    is ApiResult.Ok -> result.value
                    is ApiResult.Failed -> return@attempt fail(error(result.error))
                }
            mutableState.update { it.copy(plexUrl = started.authUrl) }
            try {
                repeat(PLEX_POLLS) {
                    delay(PLEX_POLL_MILLIS)
                    val result =
                        current.result(
                            SignInExtraEndpoints.invitePlexFinish,
                            AccountPlexFinish(started.id),
                            "token" to target.token,
                        )
                    val waiting = result is ApiResult.Ok && result.value.waiting
                    val limited = result is ApiResult.Failed && result.error.code == ApiErrorCode.RATE_LIMITED
                    if (!waiting && !limited) {
                        finish(current, result, "", SignInMethod.MEDIA_SERVER)
                        return@attempt
                    }
                }
                fail(SignInError(SignInErrorCode.PLEX_TIMEOUT))
            } finally {
                mutableState.update { it.copy(plexUrl = null) }
            }
        }
    }

    fun close() {
        replaceClient(null)
    }

    private suspend fun finish(
        current: ApiClient,
        result: ApiResult<SignInAnswer>,
        typedName: String,
        method: SignInMethod,
    ) {
        when (result) {
            is ApiResult.Ok -> {
                val answer = result.value
                current.credentials.csrf = answer.csrf
                val connection =
                    StoredConnection(baseUrl = current.baseUrl, userName = answer.name.ifEmpty { typedName }, method = method)
                val problem = sessions.establish(current, connection)
                if (problem == null) {
                    client = null
                    mutableState.update { it.copy(phase = InvitePhase.DONE, error = null) }
                } else {
                    fail(error(problem))
                }
            }
            is ApiResult.Failed -> {
                val problem = result.error
                when {
                    problem.code == ApiErrorCode.NOT_FOUND && problem.message.contains("invite") ->
                        mutableState.update { it.copy(phase = InvitePhase.GONE) }
                    problem.code == ApiErrorCode.NOT_FOUND && problem.message.contains("link") ->
                        mutableState.update { it.copy(phase = InvitePhase.GONE) }
                    problem.code == ApiErrorCode.NOT_FOUND && problem.message.contains("ended") ->
                        fail(SignInError(SignInErrorCode.SIGN_IN_ENDED, problem.message))
                    problem.code == ApiErrorCode.CONFLICT && problem.serverCode == "linked" ->
                        fail(SignInError(SignInErrorCode.LINKED, problem.message))
                    problem.code == ApiErrorCode.CONFLICT -> fail(SignInError(SignInErrorCode.NAME_TAKEN, problem.message))
                    problem.code == ApiErrorCode.UNAUTHORIZED -> fail(SignInError(SignInErrorCode.WRONG_CREDENTIALS, problem.message))
                    problem.code == ApiErrorCode.NOT_FOUND -> fail(SignInError(SignInErrorCode.WAY_OFF, problem.message))
                    else -> fail(error(problem))
                }
            }
        }
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

    private fun fail(error: SignInError) {
        mutableState.update { it.copy(error = error) }
    }

    private fun replaceClient(next: ApiClient?) {
        val previous = client
        client = next
        if (previous != null && previous !== next) previous.close()
    }

    private fun error(error: ApiError): SignInError {
        val code =
            when (error.code) {
                ApiErrorCode.TLS -> SignInErrorCode.TLS
                ApiErrorCode.OFFLINE, ApiErrorCode.TIMEOUT -> SignInErrorCode.UNREACHABLE
                ApiErrorCode.RATE_LIMITED -> SignInErrorCode.RATE_LIMITED
                ApiErrorCode.FORBIDDEN ->
                    if (error.message.contains("host", ignoreCase = true)) SignInErrorCode.HOST_REFUSED else SignInErrorCode.NOT_ALLOWED
                ApiErrorCode.NOT_FOUND, ApiErrorCode.INVALID_RESPONSE -> SignInErrorCode.NOT_HOLYARRS
                else -> SignInErrorCode.SERVER
            }
        return SignInError(code, error.message, error.retryAfterSeconds)
    }

    private companion object {
        const val PLEX_POLL_MILLIS = 2_000L
        const val PLEX_POLLS = 270
    }
}
