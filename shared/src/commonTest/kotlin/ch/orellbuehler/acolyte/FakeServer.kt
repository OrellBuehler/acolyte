package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.Credentials
import ch.orellbuehler.acolyte.api.SessionCookies
import ch.orellbuehler.acolyte.api.StoredCookie
import ch.orellbuehler.acolyte.session.MemoryStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val BASE = "https://media.example.com/holyarrs"

internal data class Recorded(
    val method: String,
    val path: String,
    val query: String,
    val headers: Map<String, String>,
    val body: String?,
)

internal class FakeServer {
    private val routes = mutableMapOf<String, suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>()

    val requests = mutableListOf<Recorded>()

    private val recording = Mutex()

    fun on(
        method: String,
        path: String,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) {
        routes["$method $path"] = handler
    }

    fun json(
        method: String,
        path: String,
        body: String,
        status: Int = 200,
        vararg headers: Pair<String, String>,
    ) = on(method, path) { respondJson(body, status, *headers) }

    fun engine(): MockEngine =
        MockEngine { request ->
            val path = request.url.encodedPath.removePrefix("/holyarrs")
            val recorded =
                Recorded(
                    request.method.value,
                    path,
                    request.url.encodedQuery,
                    request.headers.entries().associate { it.key to it.value.joinToString(",") },
                    (request.body as? TextContent)?.text,
                )
            recording.withLock { requests += recorded }
            val handler = routes["${request.method.value} $path"]
            handler?.invoke(this, request) ?: respondJson("""{"error":"not found"}""", 404)
        }

    fun request(
        method: String,
        path: String,
    ): Recorded = requests.last { it.method == method && it.path == path }

    fun client(
        cookies: List<StoredCookie> = emptyList(),
        csrf: String? = null,
        apiKey: String? = null,
    ): ApiClient = ApiClient(BASE, Credentials(apiKey, SessionCookies(cookies), csrf), engine(), language = { "en" })

    fun api(client: ApiClient = client(listOf(SESSION))): Api = Api({ client }, emptyFlow())

    fun sdk(
        scope: CoroutineScope,
        store: MemoryStore = MemoryStore(),
    ): AcolyteSdk = AcolyteSdk(store, "test", { engine() }, scope)

    companion object {
        val SESSION = StoredCookie("holyarrs_session", "token-1", "/holyarrs/")
    }
}

internal fun MockRequestHandleScope.respondJson(
    body: String,
    status: Int = 200,
    vararg headers: Pair<String, String>,
): HttpResponseData =
    respond(
        content = body,
        status = HttpStatusCode.fromValue(status),
        headers =
            headersOf(
                *(listOf(HttpHeaders.ContentType to "application/json") + headers)
                    .map {
                        it.first to listOf(it.second)
                    }.toTypedArray(),
            ),
    )

internal fun meJson(
    role: String = "owner",
    perms: String = "\"ask\",\"approve\",\"manage_library\",\"settings\"",
    csrf: String = "csrf-me",
    maxRating: String = "",
    mustTwoFactor: Boolean = false,
): String =
    """{"id":1,"name":"admin","via":"session","csrf":"$csrf","role":"$role","perms":[$perms],"max_rating":"$maxRating",""" +
        """"must_two_factor":$mustTwoFactor,"avatar":null,"views":{},"extra_field":42}"""

internal const val PARTS_JSON =
    """{"parts":[{"name":"Movies","on":true,"part":"movies","state":"running"},""" +
        """{"name":"TV shows","on":true,"part":"shows","state":"running"},""" +
        """{"name":"Music","on":false,"part":"music","state":"off"},""" +
        """{"name":"Requests","on":true,"part":"wishlist","state":"running"}],"safe_mode":false,"screens":{"/":"core"}}"""

internal fun FakeServer.bootstrapRoutes(me: String = meJson()) {
    json("GET", "/api/v1/auth/me", me)
    json("GET", "/api/v1/parts", PARTS_JSON)
    json("GET", "/api/v1/my/profile", """{"display_name":"The Admin","username":"admin"}""")
    json("GET", "/api/v1/app/about", """{"version":"1.3.0","go":"go1.26"}""")
}
