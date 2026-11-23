package ch.orellbuehler.acolyte.api

import io.ktor.client.engine.HttpClientEngine

internal expect fun platformEngine(): HttpClientEngine

internal expect fun deviceLanguage(): String

internal expect val platformName: String
