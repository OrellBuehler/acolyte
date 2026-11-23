package ch.orellbuehler.acolyte.session

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiException
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AppEndpoints
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.Credentials
import ch.orellbuehler.acolyte.api.SessionCookies
import ch.orellbuehler.acolyte.api.StoredCookie
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.api.model.Reauth
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.live.LiveStatus
import ch.orellbuehler.acolyte.live.LiveUpdates
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

sealed interface SessionState {
    data object Restoring : SessionState

    data class SignedOut(
        val lastServerUrl: String?,
        val lastUserName: String?,
        val expired: Boolean,
    ) : SessionState

    data class SignedIn(
        val account: Account,
    ) : SessionState
}

@Serializable
internal data class StoredConnection(
    val baseUrl: String,
    val userName: String = "",
    val method: SignInMethod = SignInMethod.PASSWORD,
    val apiKey: String? = null,
    val cookies: List<StoredCookie> = emptyList(),
    val csrf: String? = null,
    val account: AccountSnapshot? = null,
) {
    val hasCredentials: Boolean get() = apiKey != null || cookies.any { it.name.startsWith(SessionCookies.SESSION_COOKIE) }

    fun signedOut(): StoredConnection = StoredConnection(baseUrl = baseUrl, userName = userName, method = method)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManager internal constructor(
    private val store: SecureStore,
    private val engineFactory: () -> HttpClientEngine,
    private val appVersion: String,
    private val scope: CoroutineScope,
) {
    private class Active(
        val client: ApiClient,
        connection: StoredConnection,
        val live: LiveUpdates,
    ) {
        var connection: StoredConnection = connection
    }

    private val mutableState = MutableStateFlow<SessionState>(SessionState.Restoring)
    private val active = MutableStateFlow<Active?>(null)
    private val lock = Mutex()

    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    val account: Account? get() = (mutableState.value as? SessionState.SignedIn)?.account

    val liveStatus: StateFlow<LiveStatus> =
        active
            .flatMapLatest { it?.live?.status ?: flowOf(LiveStatus.OFF) }
            .stateIn(scope, SharingStarted.Eagerly, LiveStatus.OFF)

    internal val liveEvents: Flow<LiveEvent> = active.flatMapLatest { it?.live?.events ?: emptyFlow() }

    internal val api: Api = Api({ active.value?.client }, liveEvents)

    internal val client: ApiClient? get() = active.value?.client

    suspend fun restore() {
        lock.withLock {
            if (mutableState.value !is SessionState.Restoring) return
            val stored = load()
            if (stored == null || !stored.hasCredentials) {
                mutableState.value = SessionState.SignedOut(stored?.baseUrl, stored?.userName, expired = false)
                return
            }
            activate(stored)
            stored.account?.let { mutableState.value = SessionState.SignedIn(Account.from(stored.baseUrl, stored.method, it)) }
        }
        val error = refresh()
        if (error != null && mutableState.value is SessionState.Restoring) {
            lock.withLock {
                val current = active.value
                end(current)
                mutableState.value = SessionState.SignedOut(current?.connection?.baseUrl, current?.connection?.userName, expired = false)
            }
        }
    }

    suspend fun refresh(): ApiError? {
        val current = active.value ?: return null
        return when (val result = bootstrap(current.client)) {
            is BootResult.Ok -> {
                lock.withLock {
                    if (active.value !== current) return null
                    if (result.snapshot.me.csrf
                            .isNotEmpty()
                    ) {
                        current.client.credentials.csrf = result.snapshot.me.csrf
                    }
                    current.connection = current.connection.copy(account = result.snapshot)
                    persist(current)
                    mutableState.value =
                        SessionState.SignedIn(Account.from(current.connection.baseUrl, current.connection.method, result.snapshot))
                }
                null
            }
            is BootResult.Failed -> {
                if (result.error.code == ApiErrorCode.UNAUTHORIZED) expire(current)
                result.error
            }
        }
    }

    suspend fun reauth(
        password: String,
        code: String? = null,
    ): ApiError? {
        val current = active.value ?: return ApiError(ApiErrorCode.UNAUTHORIZED, 401)
        return when (val result = current.client.result(AuthEndpoints.reauth, Reauth(password, code?.trim()?.ifEmpty { null }))) {
            is ApiResult.Ok -> {
                if (result.value.csrf.isNotEmpty()) current.client.credentials.csrf = result.value.csrf
                null
            }
            is ApiResult.Failed -> result.error
        }
    }

    suspend fun signOut() {
        val current = active.value
        if (current != null && current.connection.apiKey == null) {
            withContext(NonCancellable) { current.client.result(AuthEndpoints.logout) }
        }
        lock.withLock {
            val connection = current?.connection ?: load()
            end(current)
            connection?.let { save(it.signedOut()) }
            mutableState.value = SessionState.SignedOut(connection?.baseUrl, connection?.userName, expired = false)
        }
    }

    internal fun newClient(
        baseUrl: String,
        credentials: Credentials,
    ): ApiClient = ApiClient(baseUrl, credentials, engineFactory(), appVersion)

    internal suspend fun establish(
        client: ApiClient,
        connection: StoredConnection,
    ): ApiError? {
        val snapshot =
            when (val result = bootstrap(client)) {
                is BootResult.Ok -> result.snapshot
                is BootResult.Failed -> return result.error
            }
        lock.withLock {
            end(active.value)
            if (snapshot.me.csrf.isNotEmpty()) client.credentials.csrf = snapshot.me.csrf
            val stored =
                connection.copy(
                    cookies = client.credentials.cookies.snapshot(),
                    csrf = client.credentials.csrf,
                    account = snapshot,
                )
            val next = Active(client, stored, LiveUpdates(client, scope))
            wire(next)
            active.value = next
            save(stored)
            mutableState.value = SessionState.SignedIn(Account.from(stored.baseUrl, stored.method, snapshot))
        }
        return null
    }

    internal fun lastConnection(): Pair<String?, String?> {
        val stored = load()
        return stored?.baseUrl to stored?.userName
    }

    private fun activate(stored: StoredConnection) {
        val credentials = Credentials(stored.apiKey, SessionCookies(stored.cookies), stored.csrf)
        val client = newClient(stored.baseUrl, credentials)
        val next = Active(client, stored, LiveUpdates(client, scope))
        wire(next)
        active.value = next
    }

    private fun wire(target: Active) {
        target.client.credentials.onChange = { persist(target) }
        target.client.onUnauthorized = { scope.launch { expire(target) } }
    }

    private suspend fun expire(target: Active) {
        lock.withLock {
            if (active.value !== target) return
            end(target)
            save(target.connection.signedOut())
            mutableState.value = SessionState.SignedOut(target.connection.baseUrl, target.connection.userName, expired = true)
        }
    }

    private fun end(target: Active?) {
        target ?: return
        target.client.credentials.onChange = {}
        target.client.onUnauthorized = {}
        if (active.value === target) active.value = null
        target.client.close()
    }

    private fun persist(target: Active) {
        if (active.value !== target && mutableState.value !is SessionState.Restoring) return
        val credentials = target.client.credentials
        val next = target.connection.copy(cookies = credentials.cookies.snapshot(), csrf = credentials.csrf)
        target.connection = next
        save(next)
    }

    private fun load(): StoredConnection? =
        store.read(KEY)?.let {
            try {
                ApiJson.lenient.decodeFromString(StoredConnection.serializer(), it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    private fun save(connection: StoredConnection) {
        store.write(KEY, ApiJson.lenient.encodeToString(StoredConnection.serializer(), connection))
    }

    private sealed interface BootResult {
        data class Ok(
            val snapshot: AccountSnapshot,
        ) : BootResult

        data class Failed(
            val error: ApiError,
        ) : BootResult
    }

    private suspend fun bootstrap(client: ApiClient): BootResult =
        try {
            coroutineScope {
                val parts = async { client.result(AppEndpoints.parts).valueOrNull() ?: Parts() }
                val profile = async { client.result(AppEndpoints.profile).valueOrNull() }
                val me = client.call(AuthEndpoints.me)
                val version =
                    if (me.role == "owner" || "settings" in me.perms) client.result(AppEndpoints.about).valueOrNull()?.version else null
                BootResult.Ok(AccountSnapshot(me, parts.await(), profile.await(), version))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            BootResult.Failed(e.error)
        }

    private companion object {
        const val KEY = "connection"
    }
}
