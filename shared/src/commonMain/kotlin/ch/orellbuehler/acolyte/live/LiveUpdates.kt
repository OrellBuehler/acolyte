package ch.orellbuehler.acolyte.live

import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiException
import ch.orellbuehler.acolyte.api.ApiJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.Volatile
import kotlin.math.min
import kotlin.random.Random

enum class LiveStatus {
    OFF,
    CONNECTING,
    LIVE,
    RECONNECTING,
}

data class LiveEvent(
    val topic: String,
    val data: JsonElement,
    val id: String?,
) {
    companion object {
        const val RESYNC = "resync"
    }
}

internal class LiveUpdates(
    private val client: ApiClient,
    scope: CoroutineScope,
    private val random: Random = Random.Default,
) {
    private val mutableStatus = MutableStateFlow(LiveStatus.OFF)

    @Volatile
    private var lastId: String? = null

    val status: StateFlow<LiveStatus> = mutableStatus.asStateFlow()

    val events: SharedFlow<LiveEvent> =
        channelFlow {
            var failures = 0
            try {
                while (true) {
                    mutableStatus.value = if (failures == 0) LiveStatus.CONNECTING else LiveStatus.RECONNECTING
                    val parser = SseParser()
                    val reopened = failures > 0
                    try {
                        client.live(
                            lastEventId = lastId,
                            onOpen = {
                                mutableStatus.value = LiveStatus.LIVE
                                failures = 0
                            },
                        ) { line ->
                            val message = parser.feed(line) ?: return@live
                            message.id?.let { lastId = it }
                            decode(message)?.let { send(it) }
                        }
                    } catch (e: ApiException) {
                        if (e.error.code == ApiErrorCode.UNAUTHORIZED || e.error.code == ApiErrorCode.FORBIDDEN) return@channelFlow
                    }
                    if (reopened && lastId == null) send(LiveEvent(LiveEvent.RESYNC, JsonNull, null))
                    failures++
                    delay(backoffMillis(failures))
                }
            } finally {
                mutableStatus.value = LiveStatus.OFF
            }
        }.shareIn(scope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS))

    private fun backoffMillis(failures: Int): Long {
        val base = min(MAX_BACKOFF_MILLIS, 1000L shl min(failures, 10))
        return (base * (0.8 + random.nextDouble() * 0.2)).toLong()
    }

    private fun decode(message: SseMessage): LiveEvent? {
        val envelope =
            try {
                ApiJson.lenient.parseToJsonElement(message.data) as? JsonObject
            } catch (e: IllegalArgumentException) {
                null
            } ?: return null
        val topic = envelope["topic"]?.jsonPrimitive?.contentOrNull ?: return null
        return LiveEvent(topic, envelope["data"] ?: JsonNull, message.id)
    }

    private companion object {
        const val MAX_BACKOFF_MILLIS = 10_000L
        const val STOP_AFTER_MILLIS = 5_000L
    }
}
