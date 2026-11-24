package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SettingsLookLanguageChange(
    val language: String = "",
)

@Serializable
data class SettingsLookMetadata(
    val mode: String = "",
    val locked: Boolean = false,
    @SerialName("locked_note") val lockedNote: String = "",
    @SerialName("docker_value") val dockerValue: String = "",
    val keys: Map<String, Boolean> = emptyMap(),
    @SerialName("tmdb_key_from_docker") val tmdbKeyFromDocker: Boolean = false,
    val sources: List<SettingsLookSource> = emptyList(),
    val hosts: List<String> = emptyList(),
    val note: String = "",
    @SerialName("own_keys_note") val ownKeysNote: String = "",
)

@Serializable
data class SettingsLookSource(
    val id: String = "",
    val name: String = "",
    val state: String = "",
    val key: String = "",
)

@Serializable
internal data class SettingsLookMetadataChange(
    val mode: String? = null,
    val keys: SettingsLookKeys? = null,
)

@Serializable
internal data class SettingsLookKeys(
    @SerialName("tmdb_token") val tmdbToken: String = "",
    @SerialName("tvdb_key") val tvdbKey: String = "",
    @SerialName("tvdb_pin") val tvdbPin: String = "",
    @SerialName("mdblist_key") val mdblistKey: String = "",
    @SerialName("fanart_key") val fanartKey: String = "",
    @SerialName("omdb_key") val omdbKey: String = "",
)

@Serializable
internal data class SettingsLookKeyTest(
    val key: String = "",
    val value: String = "",
    val pin: String = "",
)

@Serializable
data class SettingsLookKeyAnswer(
    val ok: Boolean = false,
    val code: String = "",
    val message: String = "",
)

@Serializable
data class SettingsLookMetadataHealth(
    val up: Boolean = true,
    val message: String = "",
    @SerialName("fakeIP") val fakeIp: String = "",
)
