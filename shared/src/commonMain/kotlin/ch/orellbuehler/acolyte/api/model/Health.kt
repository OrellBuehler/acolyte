package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthChecks(
    val checks: List<HealthCheck> = emptyList(),
)

@Serializable
data class HealthCheck(
    val id: Long = 0,
    val key: String = "",
    val kind: String = "",
    val subject: String = "",
    val state: String = "",
    val confirmed: String = "",
    val message: String = "",
    val since: String = "",
    @SerialName("checked_at") val checkedAt: String = "",
)
