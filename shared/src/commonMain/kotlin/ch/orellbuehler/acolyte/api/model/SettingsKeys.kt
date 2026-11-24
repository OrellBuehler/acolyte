package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsKeysAi(
    val enabled: Boolean = false,
    val path: String = "",
    val versions: List<String> = emptyList(),
)

@Serializable
internal data class SettingsKeysAiChange(
    val enabled: Boolean,
)

@Serializable
data class SettingsKeysArrKey(
    val side: String = "",
    val hint: String = "",
    val owner: String = "",
    @SerialName("owner_active") val ownerActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)

@Serializable
data class SettingsKeysSide(
    val side: String = "",
    val path: String = "",
    val url: String = "",
    val key: SettingsKeysArrKey? = null,
)

@Serializable
data class SettingsKeysPushedFormat(
    val side: String = "",
    val name: String = "",
    @SerialName("trait_id") val traitId: Long = 0,
    val skipped: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class SettingsKeysPushedProfile(
    val side: String = "",
    val name: String = "",
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
    val notes: List<String> = emptyList(),
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class SettingsKeysPushed(
    val formats: List<SettingsKeysPushedFormat> = emptyList(),
    val profiles: List<SettingsKeysPushedProfile> = emptyList(),
)

@Serializable
data class SettingsKeysVersions(
    val sonarr: String = "",
    val radarr: String = "",
)

@Serializable
data class SettingsKeysOldApp(
    val enabled: Boolean = false,
    @SerialName("delete_files") val deleteFiles: Boolean = false,
    val sides: List<SettingsKeysSide> = emptyList(),
    val versions: SettingsKeysVersions = SettingsKeysVersions(),
    val pushed: SettingsKeysPushed = SettingsKeysPushed(),
)

@Serializable
internal data class SettingsKeysOldAppChange(
    val enabled: Boolean? = null,
    @SerialName("delete_files") val deleteFiles: Boolean? = null,
)

@Serializable
data class SettingsKeysArrKeyMade(
    val side: String = "",
    val key: String = "",
    val path: String = "",
    val note: String = "",
)
