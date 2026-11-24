package ch.orellbuehler.acolyte.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

internal actual fun platformEngine(): HttpClientEngine =
    Darwin.create {
        configureSession {
            setHTTPShouldSetCookies(false)
            setHTTPCookieStorage(null)
        }
    }

internal actual fun deviceLanguage(): String = (NSLocale.preferredLanguages.firstOrNull() as? String ?: "en").lowercase()
