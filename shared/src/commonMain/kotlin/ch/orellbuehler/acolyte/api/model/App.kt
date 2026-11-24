package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Parts(
    val parts: List<PartView> = emptyList(),
    @SerialName("safe_mode") val safeMode: Boolean = false,
)

@Serializable
data class PartView(
    val part: String = "",
    val name: String = "",
    val on: Boolean = false,
    val state: String = "",
    val reason: String = "",
)

@Serializable
data class MyProfile(
    @SerialName("display_name") val displayName: String = "",
    val username: String = "",
    val language: String = "",
)

@Serializable
data class About(
    val version: String = "",
    val go: String = "",
    val os: String = "",
    val docker: Boolean = false,
    val sqlite: String = "",
    val migration: Int = 0,
    @SerialName("config_dir") val configDir: String = "",
    @SerialName("start_dir") val startDir: String = "",
    @SerialName("uptime_s") val uptimeSeconds: Long = 0,
    @SerialName("started_at") val startedAt: String = "",
)
