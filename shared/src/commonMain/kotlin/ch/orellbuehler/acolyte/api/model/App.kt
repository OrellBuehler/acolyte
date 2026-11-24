package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Parts(
    val parts: List<PartView> = emptyList(),
    @SerialName("safe_mode") val safeMode: Boolean = false,
    @SerialName("safe_screens") val safeScreens: List<String> = emptyList(),
    val stages: List<String> = emptyList(),
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
    val docker: Boolean = false,
    @SerialName("started_at") val startedAt: String = "",
)
