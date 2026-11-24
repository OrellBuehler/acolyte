package ch.orellbuehler.acolyte.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.utils.io.readLine
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.concurrent.Volatile

internal class Credentials(
    val apiKey: String? = null,
    val cookies: SessionCookies = SessionCookies(),
    csrf: String? = null,
) {
    @Volatile
    var csrf: String? = csrf
        set(value) {
            if (field == value) return
            field = value
            onChange()
        }

    @Volatile
    var onChange: () -> Unit = {}

    init {
        cookies.onChange = { onChange() }
    }

    val isSignedIn: Boolean get() = apiKey != null || cookies.hasSession()
}

internal class ApiClient(
    val baseUrl: String,
    val credentials: Credentials,
    private val engine: HttpClientEngine,
    appVersion: String = "dev",
    private val json: Json = ApiJson.lenient,
    private val language: () -> String = ::deviceLanguage,
) {
    @Volatile
    var onUnauthorized: () -> Unit = {}

    private val http =
        HttpClient(engine) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 20_000
            }
            install(HttpCookies) {
                storage = credentials.cookies
            }
            install(UserAgent) {
                agent = "Acolyte/$appVersion ($platformName)"
            }
        }

    suspend fun <R> call(
        endpoint: Endpoint<Unit, R>,
        vararg params: Pair<String, Any?>,
    ): R = execute(endpoint, null, params.toMap(), retried = false)

    suspend fun <B, R> call(
        endpoint: Endpoint<B, R>,
        body: B,
        vararg params: Pair<String, Any?>,
    ): R = execute(endpoint, body, params.toMap(), retried = false)

    suspend fun <R> result(
        endpoint: Endpoint<Unit, R>,
        vararg params: Pair<String, Any?>,
    ): ApiResult<R> = capture { execute(endpoint, null, params.toMap(), retried = false) }

    suspend fun <B, R> result(
        endpoint: Endpoint<B, R>,
        body: B,
        vararg params: Pair<String, Any?>,
    ): ApiResult<R> = capture { execute(endpoint, body, params.toMap(), retried = false) }

    fun imageHeaders(): Map<String, String> {
        credentials.apiKey?.let { return mapOf(API_KEY_HEADER to it) }
        return credentials.cookies.headerValue()?.let { mapOf(HttpHeaders.Cookie to it) } ?: emptyMap()
    }

    suspend fun live(
        lastEventId: String?,
        onOpen: () -> Unit,
        onLine: suspend (String) -> Unit,
    ) {
        try {
            http
                .prepareGet(baseUrl.trimEnd('/') + LIVE_PATH) {
                    timeout {
                        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        socketTimeoutMillis = LIVE_IDLE_MILLIS
                    }
                    accept(ContentType.Text.EventStream)
                    authenticate(unsafe = false)
                    lastEventId?.let { header(LAST_EVENT_ID_HEADER, it) }
                }.execute { response ->
                    if (response.status.value !in 200..299) throw failure(response, response.bodyAsText(), LIVE_PATH)
                    onOpen()
                    val channel = response.bodyAsChannel()
                    while (true) {
                        val line = channel.readLine() ?: break
                        onLine(line)
                    }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: Throwable) {
            throw ApiException(errorForThrowable(e), e)
        }
    }

    fun close() {
        http.close()
        engine.close()
    }

    private suspend fun <T> capture(block: suspend () -> T): ApiResult<T> =
        try {
            ApiResult.Ok(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            ApiResult.Failed(e.error)
        }

    private suspend fun <B, R> execute(
        endpoint: Endpoint<B, R>,
        body: B?,
        params: Map<String, Any?>,
        retried: Boolean,
    ): R {
        val url = endpoint.url(baseUrl, params)
        val bodySerializer = endpoint.body
        val response: HttpResponse
        val text: String
        try {
            response =
                http.request(url) {
                    method = endpoint.method
                    accept(ContentType.Application.Json)
                    authenticate(endpoint.isUnsafe)
                    val form = body as? FormBody
                    if (form != null && form.formFiles.isNotEmpty()) {
                        setBody(multipart(form))
                    } else if (bodySerializer != null && body != null) {
                        setBody(TextContent(json.encodeToString(bodySerializer, body), ContentType.Application.Json))
                    }
                }
            text = response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw ApiException(errorForThrowable(e), e)
        }
        val status = response.status.value
        if (status in 200..299) return decode(endpoint, text, status)
        val error = failure(response, text, endpoint.path)
        val usesSession = endpoint.isUnsafe && credentials.apiKey == null
        if (status == 403 && !retried && usesSession && error.error.message.contains("csrf", ignoreCase = true)) {
            refreshCsrf()
            return execute(endpoint, body, params, retried = true)
        }
        throw error
    }

    private fun multipart(form: FormBody): MultiPartFormDataContent =
        MultiPartFormDataContent(
            formData {
                form.formFields.forEach { (name, value) -> append(name, value) }
                form.formFiles.forEach { file ->
                    append(
                        file.field,
                        file.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, file.type)
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name.replace("\"", "")}\"")
                        },
                    )
                }
            },
        )

    private suspend fun refreshCsrf() {
        val me = execute(AuthEndpoints.me, null, emptyMap(), retried = true)
        if (me.csrf.isNotEmpty()) credentials.csrf = me.csrf
    }

    private fun HttpRequestBuilder.authenticate(unsafe: Boolean) {
        header(LANGUAGE_HEADER, language())
        val key = credentials.apiKey
        if (key != null) {
            header(API_KEY_HEADER, key)
        } else if (unsafe) {
            credentials.csrf?.takeIf { it.isNotEmpty() }?.let { header(CSRF_HEADER, it) }
        }
    }

    private fun failure(
        response: HttpResponse,
        text: String,
        path: String,
    ): ApiException {
        val status = response.status.value
        val body =
            try {
                json.parseToJsonElement(text) as? JsonObject
            } catch (e: IllegalArgumentException) {
                null
            }

        fun field(name: String): String = (body?.get(name) as? JsonPrimitive)?.contentOrNull.orEmpty()
        val message = field("error").ifEmpty { text.take(200) }
        if (status == 401 && credentials.isSignedIn && !isQuiet401(path)) onUnauthorized()
        val retryAfter = response.headers[HttpHeaders.RetryAfter]?.trim()?.toIntOrNull()
        return ApiException(errorForStatus(status, message, retryAfter, field("code"), field("part"), field("text"), text.take(BODY_LIMIT)))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R> decode(
        endpoint: Endpoint<*, R>,
        text: String,
        status: Int,
    ): R {
        if (endpoint.response === NoContent) return Unit as R
        return try {
            json.decodeFromString(endpoint.response, text.ifBlank { "{}" })
        } catch (e: IllegalArgumentException) {
            throw ApiException(ApiError(ApiErrorCode.INVALID_RESPONSE, status, e.message.orEmpty()), e)
        }
    }

    companion object {
        const val API_KEY_HEADER = "X-Api-Key"
        const val CSRF_HEADER = "X-CSRF-Token"
        const val LANGUAGE_HEADER = "X-HolyArrs-Language"
        const val LAST_EVENT_ID_HEADER = "Last-Event-ID"
        const val LIVE_PATH = "/live"
        const val LIVE_IDLE_MILLIS = 45_000L
        private const val BODY_LIMIT = 4_000

        private val QUIET_401 =
            listOf("login", "state", "setup", "media-server", "plex", "invites", "two-factor", "password", "oidc", "forward", "reauth")
                .map { "/api/v1/auth/$it" }

        fun isQuiet401(path: String): Boolean = QUIET_401.any { path.startsWith(it) }
    }
}
