package ch.orellbuehler.acolyte.api

import kotlinx.serialization.json.Json

internal object ApiJson {
    val lenient: Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
            isLenient = true
            encodeDefaults = true
        }

    val strict: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
}
