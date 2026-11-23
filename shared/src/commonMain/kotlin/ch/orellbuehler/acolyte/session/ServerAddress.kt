package ch.orellbuehler.acolyte.session

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodedPath

internal object ServerAddress {
    private val TRAILING = listOf("/api/v1", "/login", "/welcome", "/live")

    fun candidates(input: String): List<String> {
        val text = input.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return emptyList()
        val hasScheme = text.contains("://")
        if (hasScheme && !text.startsWith("http://", ignoreCase = true) && !text.startsWith("https://", ignoreCase = true)) {
            return emptyList()
        }
        val raw = if (hasScheme) listOf(text) else listOf("https://$text", "http://$text")
        return raw.mapNotNull { normalize(it) }.distinct()
    }

    fun normalize(text: String): String? {
        val url =
            try {
                Url(text)
            } catch (e: Exception) {
                return null
            }
        if (url.host.isEmpty() || (url.protocol != URLProtocol.HTTP && url.protocol != URLProtocol.HTTPS)) return null
        var path = url.encodedPath.trimEnd('/')
        var trimmed = true
        while (trimmed) {
            trimmed = false
            for (suffix in TRAILING) {
                val at = path.indexOf(suffix)
                if (at >= 0 && (at + suffix.length == path.length || path[at + suffix.length] == '/')) {
                    path = path.substring(0, at).trimEnd('/')
                    trimmed = true
                }
            }
        }
        val builder = URLBuilder(protocol = url.protocol, host = url.host, port = url.specifiedPort)
        builder.encodedPath = path
        return builder.buildString().trimEnd('/')
    }

    fun displayName(baseUrl: String): String =
        try {
            val url = Url(baseUrl)
            val port = if (url.specifiedPort != 0 && url.specifiedPort != url.protocol.defaultPort) ":${url.specifiedPort}" else ""
            url.host + port + url.encodedPath.trimEnd('/')
        } catch (e: Exception) {
            baseUrl
        }
}
