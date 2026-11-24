package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SettingsControlConfirm(
    val confirm: String? = null,
)

@Serializable
internal data class SettingsControlAnswer(
    val confirm: String = "",
    @SerialName("expires_in_s") val expiresInSeconds: Int = 0,
    val text: String = "",
    @SerialName("still_running") val stillRunning: Int = 0,
)

@Serializable
internal data class SettingsLinkTest(
    val ok: Boolean = true,
    val text: String = "",
    val message: String = "",
    val error: String = "",
    val found: String = "",
)

@Serializable
internal data class SettingsPathInfo(
    val name: String = "",
    val path: String = "",
    val bytes: Long = 0,
    @SerialName("free_bytes") val freeBytes: Long = -1,
    val kind: String = "",
)

@Serializable
internal data class SettingsStorage(
    val app: List<SettingsPathInfo> = emptyList(),
    @SerialName("library_folders") val libraryFolders: List<SettingsPathInfo> = emptyList(),
    @SerialName("save_path") val savePath: String = "",
)

@Serializable
internal data class SettingsUrlBase(
    val saved: String = "",
    @SerialName("in_use") val inUse: String = "",
    @SerialName("restart_needed") val restartNeeded: Boolean = false,
)

@Serializable
internal data class SettingsUrlBaseChange(
    @SerialName("url_base") val urlBase: String = "",
)

@Serializable
internal data class SettingsProxy(
    val type: String = "",
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
    val bypass: String = "",
    @SerialName("bypass_local") val bypassLocal: Boolean = true,
)

@Serializable
internal data class SettingsScheduleChange(
    @SerialName("every_s") val everySeconds: Long? = null,
    val enabled: Boolean? = null,
)
