package ch.orellbuehler.acolyte.api

enum class ApiErrorCode {
    OFFLINE,
    TIMEOUT,
    TLS,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    BAD_REQUEST,
    CONFLICT,
    PART_OFF,
    HANDS_OFF,
    REAUTH,
    RATE_LIMITED,
    SERVER,
    INVALID_RESPONSE,
    UNKNOWN,
}

data class ApiError(
    val code: ApiErrorCode,
    val status: Int = 0,
    val message: String = "",
    val retryAfterSeconds: Int? = null,
    val serverCode: String = "",
    val part: String = "",
    val text: String = "",
)

internal class ApiException(
    val error: ApiError,
    cause: Throwable? = null,
) : Exception("${error.code} ${error.status} ${error.message}".trim(), cause)

internal sealed interface ApiResult<out T> {
    data class Ok<T>(
        val value: T,
    ) : ApiResult<T>

    data class Failed(
        val error: ApiError,
    ) : ApiResult<Nothing>
}

internal inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Ok) block(value)
    return this
}

internal inline fun <T> ApiResult<T>.onFailure(block: (ApiError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failed) block(error)
    return this
}

internal fun <T> ApiResult<T>.valueOrNull(): T? = (this as? ApiResult.Ok)?.value

internal fun errorForStatus(
    status: Int,
    message: String,
    retryAfter: Int?,
    serverCode: String = "",
    part: String = "",
    text: String = "",
): ApiError {
    val code =
        when {
            status == 400 || status == 413 || status == 415 || status == 422 -> ApiErrorCode.BAD_REQUEST
            status == 401 -> ApiErrorCode.UNAUTHORIZED
            status == 403 && serverCode == REAUTH_CODE -> ApiErrorCode.REAUTH
            status == 403 -> ApiErrorCode.FORBIDDEN
            status == 404 || status == 410 -> ApiErrorCode.NOT_FOUND
            status == 409 && message == PART_OFF_MESSAGE -> ApiErrorCode.PART_OFF
            status == 409 && message == HANDS_OFF_MESSAGE -> ApiErrorCode.HANDS_OFF
            status == 409 -> ApiErrorCode.CONFLICT
            status == 429 -> ApiErrorCode.RATE_LIMITED
            status >= 500 -> ApiErrorCode.SERVER
            else -> ApiErrorCode.UNKNOWN
        }
    return ApiError(code, status, message, retryAfter, serverCode, part, text)
}

internal fun errorForThrowable(e: Throwable): ApiError {
    if (e is ApiException) return e.error
    val name = e::class.simpleName.orEmpty()
    val text = (e.message.orEmpty() + " " + (e.cause?.message.orEmpty())).lowercase()
    val code =
        when {
            name.contains("Serialization") || e is IllegalArgumentException -> ApiErrorCode.INVALID_RESPONSE
            name.contains("Timeout") || text.contains("timed out") || text.contains("timeout") -> ApiErrorCode.TIMEOUT
            TLS_HINTS.any { text.contains(it) } -> ApiErrorCode.TLS
            else -> ApiErrorCode.OFFLINE
        }
    return ApiError(code, 0, e.message.orEmpty())
}

private const val PART_OFF_MESSAGE = "this part is off"
private const val HANDS_OFF_MESSAGE = "hands-off mode is on"
private const val REAUTH_CODE = "reauth"

private val TLS_HINTS = listOf("certificate", "ssl", "tls", "trust anchor", "handshake", "-1202", "-1200")
