package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SettingsMediaServersList(
    @SerialName("media_servers") val mediaServers: List<SettingsMediaServersItem> = emptyList(),
)

@Serializable
internal data class SettingsMediaServersItem(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    val url: String = "",
    val token: String = "",
    val enabled: Boolean = false,
    @SerialName("on_import") val onImport: Boolean = false,
    @SerialName("on_rename") val onRename: Boolean = false,
    @SerialName("on_delete") val onDelete: Boolean = false,
    @SerialName("path_from") val pathFrom: String = "",
    @SerialName("path_to") val pathTo: String = "",
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean = false,
    @SerialName("web_url") val webUrl: String = "",
    @SerialName("plain_password") val plainPassword: Boolean = false,
    @SerialName("last_ok_at") val lastOkAt: String = "",
    @SerialName("last_error") val lastError: String = "",
)

@Serializable
internal data class SettingsMediaServersInput(
    val name: String? = null,
    val kind: String? = null,
    val url: String? = null,
    val token: String? = null,
    val username: String? = null,
    val enabled: Boolean? = null,
    @SerialName("on_import") val onImport: Boolean? = null,
    @SerialName("on_rename") val onRename: Boolean? = null,
    @SerialName("on_delete") val onDelete: Boolean? = null,
    @SerialName("path_from") val pathFrom: String? = null,
    @SerialName("path_to") val pathTo: String? = null,
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean? = null,
    @SerialName("plain_password") val plainPassword: Boolean? = null,
    @SerialName("web_url") val webUrl: String? = null,
    @SerialName("use_plex_sign_in") val usePlexSignIn: Boolean? = null,
)

@Serializable
internal data class SettingsMediaServersTested(
    val ok: Boolean = false,
    val found: String = "",
)

@Serializable
internal data class SettingsMediaServersPlexList(
    val servers: List<SettingsMediaServersPlexItem> = emptyList(),
)

@Serializable
internal data class SettingsMediaServersPlexItem(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val port: Int = 0,
    val local: Boolean = false,
    val url: String = "",
    @SerialName("secure_url") val secureUrl: String = "",
    val relay: Boolean = false,
)

@Serializable
internal data class SettingsMediaServersWatch(
    val enabled: Boolean = false,
)

@Serializable
internal data class SettingsMediaServersStarted(
    val started: Boolean = false,
)

@Serializable
data class SettingsMediaServersLabels(
    val enabled: Boolean = false,
    val label: String = "holyarrs",
    val askers: Boolean = false,
)

@Serializable
data class SettingsMediaServersTracks(
    val enabled: Boolean = false,
    val audio: List<String> = emptyList(),
    val subtitles: List<String> = emptyList(),
)
