package ch.orellbuehler.acolyte.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.Locale

internal actual fun platformEngine(): HttpClientEngine = OkHttp.create()

internal actual fun deviceLanguage(): String = Locale.getDefault().toLanguageTag().lowercase()

internal actual val platformName: String = "Android"
