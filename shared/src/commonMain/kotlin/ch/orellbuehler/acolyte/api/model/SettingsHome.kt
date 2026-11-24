package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SettingsStatusAnswer(
    val items: List<SettingsStatusItem> = emptyList(),
    val unset: Map<String, List<String>> = emptyMap(),
    val known: List<String> = emptyList(),
    @SerialName("metadata_up") val metadataUp: Boolean = true,
)

@Serializable
internal data class SettingsStatusItem(
    val group: String = "",
    val page: String = "",
    val text: String = "",
    val severity: String = "",
)
