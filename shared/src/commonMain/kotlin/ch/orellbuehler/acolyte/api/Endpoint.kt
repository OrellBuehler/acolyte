package ch.orellbuehler.acolyte.api

import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

enum class Need(
    val wire: String,
) {
    PUBLIC("public"),
    SIGNED_IN("signed_in"),
    SELF("self"),
    USERS("users"),
    OWNER("owner"),
    ASK("ask"),
    AUTO_APPROVE("auto_approve"),
    AUTO_APPROVE_FOUR_K("auto_approve_four_k"),
    APPROVE("approve"),
    MANAGE_LIBRARY("manage_library"),
    SETTINGS("settings"),
}

enum class Part(
    val wire: String,
) {
    CORE("core"),
    MEDIA("media"),
    TITLE("title"),
    MOVIES("movies"),
    SHOWS("shows"),
    MUSIC("music"),
    SUBTITLES("subtitles"),
    WISHLIST("wishlist"),
    SOURCES("sources"),
}

internal val NoContent: KSerializer<Unit> = Unit.serializer()

internal class Endpoint<B, R>(
    val method: HttpMethod,
    val path: String,
    val need: Need,
    val part: Part,
    val query: List<String>,
    val body: KSerializer<B>?,
    val response: KSerializer<R>,
) {
    val key: String get() = "${method.value} $path"

    val pathParams: List<String> = PARAM.findAll(path).map { it.groupValues[1] }.toList()

    val isUnsafe: Boolean get() = method != HttpMethod.Get && method != HttpMethod.Head

    fun url(
        baseUrl: String,
        params: Map<String, Any?>,
    ): String {
        val unknown = params.keys - pathParams.toSet() - query.toSet()
        require(unknown.isEmpty()) { "$key has no parameter ${unknown.joinToString()}; declare query names on the endpoint" }
        val missing = pathParams.filter { params[it] == null }
        require(missing.isEmpty()) { "$key needs ${missing.joinToString()}" }
        val segments =
            path.trimStart('/').split('/').map { segment ->
                PARAM.replace(segment) { params.getValue(it.groupValues[1]).toString() }
            }
        val builder = URLBuilder(baseUrl)
        builder.appendPathSegments(segments, encodeSlash = true)
        for (name in query) {
            when (val value = params[name]) {
                null -> Unit
                is Iterable<*> -> value.filterNotNull().forEach { builder.parameters.append(name, it.toString()) }
                else -> builder.parameters.append(name, value.toString())
            }
        }
        return builder.buildString()
    }

    private companion object {
        val PARAM = Regex("\\{([^}]+)\\}")
    }
}

internal sealed class EndpointGroup {
    private val registered = mutableListOf<Endpoint<*, *>>()

    val endpoints: List<Endpoint<*, *>> get() = registered.toList()

    private fun <B, R> add(endpoint: Endpoint<B, R>): Endpoint<B, R> = endpoint.also { registered += it }

    protected fun <R> get(
        path: String,
        response: KSerializer<R>,
        need: Need,
        part: Part = Part.CORE,
        query: List<String> = emptyList(),
    ): Endpoint<Unit, R> = add(Endpoint(HttpMethod.Get, path, need, part, query, null, response))

    protected fun <R> post(
        path: String,
        response: KSerializer<R>,
        need: Need,
        part: Part = Part.CORE,
        query: List<String> = emptyList(),
    ): Endpoint<Unit, R> = add(Endpoint(HttpMethod.Post, path, need, part, query, null, response))

    protected fun <B, R> post(
        path: String,
        body: KSerializer<B>,
        response: KSerializer<R>,
        need: Need,
        part: Part = Part.CORE,
        query: List<String> = emptyList(),
    ): Endpoint<B, R> = add(Endpoint(HttpMethod.Post, path, need, part, query, body, response))

    protected fun <R> put(
        path: String,
        response: KSerializer<R>,
        need: Need,
        part: Part = Part.CORE,
        query: List<String> = emptyList(),
    ): Endpoint<Unit, R> = add(Endpoint(HttpMethod.Put, path, need, part, query, null, response))

    protected fun <B, R> put(
        path: String,
        body: KSerializer<B>,
        response: KSerializer<R>,
        need: Need,
        part: Part = Part.CORE,
        query: List<String> = emptyList(),
    ): Endpoint<B, R> = add(Endpoint(HttpMethod.Put, path, need, part, query, body, response))

    protected fun <B, R> patch(
        path: String,
        body: KSerializer<B>,
        response: KSerializer<R>,
        need: Need,
        part: Part = Part.CORE,
        query: List<String> = emptyList(),
    ): Endpoint<B, R> = add(Endpoint(HttpMethod.Patch, path, need, part, query, body, response))

    protected fun <R> delete(
        path: String,
        response: KSerializer<R>,
        need: Need,
        part: Part = Part.CORE,
        query: List<String> = emptyList(),
    ): Endpoint<Unit, R> = add(Endpoint(HttpMethod.Delete, path, need, part, query, null, response))
}
