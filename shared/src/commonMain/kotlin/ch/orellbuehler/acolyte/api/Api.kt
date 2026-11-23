package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.live.LiveEvent
import kotlinx.coroutines.flow.Flow

internal class Api(
    private val client: () -> ApiClient?,
    val live: Flow<LiveEvent>,
) {
    suspend fun <R> result(
        endpoint: Endpoint<Unit, R>,
        vararg params: Pair<String, Any?>,
    ): ApiResult<R> = client()?.result(endpoint, *params) ?: signedOut

    suspend fun <B, R> result(
        endpoint: Endpoint<B, R>,
        body: B,
        vararg params: Pair<String, Any?>,
    ): ApiResult<R> = client()?.result(endpoint, body, *params) ?: signedOut

    private val signedOut = ApiResult.Failed(ApiError(ApiErrorCode.UNAUTHORIZED, 401))
}
