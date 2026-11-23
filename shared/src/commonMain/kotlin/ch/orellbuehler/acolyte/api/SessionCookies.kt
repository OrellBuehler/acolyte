package ch.orellbuehler.acolyte.api

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.concurrent.Volatile
import kotlin.time.Clock

@Serializable
internal data class StoredCookie(
    val name: String,
    val value: String,
    val path: String = "/",
    val expiresAt: Long? = null,
)

internal class SessionCookies(
    initial: List<StoredCookie> = emptyList(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : CookiesStorage {
    @Volatile
    private var cookies: Map<String, StoredCookie> = initial.associateBy { it.name }

    private val writes = Mutex()

    @Volatile
    var onChange: (List<StoredCookie>) -> Unit = {}

    fun snapshot(): List<StoredCookie> {
        val time = now()
        return cookies.values.filter { it.expiresAt == null || it.expiresAt > time }
    }

    fun hasSession(): Boolean = snapshot().any { it.name.startsWith(SESSION_COOKIE) }

    fun headerValue(): String? = snapshot().takeIf { it.isNotEmpty() }?.joinToString("; ") { "${it.name}=${it.value}" }

    fun clear() {
        cookies = emptyMap()
        onChange(emptyList())
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val path = requestUrl.encodedPath.ifEmpty { "/" }
        return snapshot()
            .filter { path.startsWith(it.path.trimEnd('/')) }
            .map { Cookie(name = it.name, value = it.value, encoding = CookieEncoding.RAW, path = it.path) }
    }

    override suspend fun addCookie(
        requestUrl: Url,
        cookie: Cookie,
    ) {
        val maxAge = cookie.maxAge
        val expiresAt = if (maxAge != null) now() + maxAge * 1000L else cookie.expires?.timestamp
        val changed =
            writes.withLock {
                val next = cookies.toMutableMap()
                if (cookie.value.isEmpty() || (expiresAt != null && expiresAt <= now())) {
                    next.remove(cookie.name)
                } else {
                    next[cookie.name] = StoredCookie(cookie.name, cookie.value, cookie.path ?: "/", expiresAt)
                }
                cookies = next
                next.values.toList()
            }
        onChange(changed)
    }

    override fun close() = Unit

    companion object {
        const val SESSION_COOKIE = "holyarrs_session"
    }
}
