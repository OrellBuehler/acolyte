package ch.orellbuehler.acolyte.image

import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.ArtworkEndpoints
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader

enum class ImageSize(
    internal val tmdb: String,
) {
    THUMB("w185"),
    POSTER("w342"),
    POSTER_LARGE("w500"),
    BACKDROP("w780"),
    HERO("w1280"),
    ORIGINAL("original"),
}

class Images internal constructor(
    private val client: () -> ApiClient?,
) {
    fun url(
        source: String?,
        size: ImageSize = ImageSize.POSTER,
    ): String? {
        val text = source?.trim().orEmpty()
        if (text.isEmpty()) return null
        val base = client()?.baseUrl
        TMDB.matchEntire(text)?.let { match ->
            base ?: return text
            return ArtworkEndpoints.tmdb.url(base, mapOf("size" to size.tmdb, "file" to match.groupValues[1]))
        }
        if (text.startsWith("//")) return "https:$text"
        if (text.startsWith("/")) return base?.let { it.trimEnd('/') + text }
        if (text.startsWith("http://") || text.startsWith("https://")) return text
        return null
    }

    fun headers(url: String): Map<String, String> {
        val current = client() ?: return emptyMap()
        return if (onServer(current, url)) current.imageHeaders() else emptyMap()
    }

    suspend fun storeCookies(
        url: String,
        setCookieHeaders: List<String>,
    ) {
        val current = client() ?: return
        if (!onServer(current, url)) return
        val requestUrl = Url(url)
        for (header in setCookieHeaders) {
            current.credentials.cookies.addCookie(requestUrl, parseServerSetCookieHeader(header))
        }
    }

    private fun onServer(
        current: ApiClient,
        url: String,
    ): Boolean = url.startsWith(current.baseUrl.trimEnd('/') + "/")

    private companion object {
        val TMDB = Regex("https?://image\\.tmdb\\.org/t/p/[^/]+/([A-Za-z0-9_-]+\\.(?:jpg|jpeg|png|webp))")
    }
}
