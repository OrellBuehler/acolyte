package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsServerAlert(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    val enabled: Boolean = false,
    val moments: List<String> = emptyList(),
    val url: String = "",
    @SerialName("smtp_host") val smtpHost: String = "",
    @SerialName("smtp_port") val smtpPort: Int = 0,
    @SerialName("smtp_security") val smtpSecurity: String = "",
    @SerialName("smtp_from") val smtpFrom: String = "",
    @SerialName("smtp_user") val smtpUser: String = "",
    @SerialName("smtp_pass") val smtpPass: String = "",
    @SerialName("staff_to") val staffTo: String = "",
    val secret: String = "",
    @SerialName("webhook_auth") val webhookAuth: String = "",
    @SerialName("bot_token") val botToken: String = "",
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("server_url") val serverUrl: String = "",
    val topic: String = "",
    @SerialName("user_key") val userKey: String = "",
    @SerialName("app_token") val appToken: String = "",
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean = false,
    @SerialName("last_sent_at") val lastSentAt: String = "",
    @SerialName("last_error") val lastError: String = "",
)

@Serializable
data class SettingsServerAlerts(
    val alerts: List<SettingsServerAlert> = emptyList(),
    val moments: List<String> = emptyList(),
)

@Serializable
data class SettingsServerAlertInput(
    val name: String = "",
    val kind: String? = null,
    val enabled: Boolean? = null,
    val moments: List<String>? = null,
    val url: String? = null,
    @SerialName("smtp_host") val smtpHost: String? = null,
    @SerialName("smtp_port") val smtpPort: Int? = null,
    @SerialName("smtp_security") val smtpSecurity: String? = null,
    @SerialName("smtp_from") val smtpFrom: String? = null,
    @SerialName("smtp_user") val smtpUser: String? = null,
    @SerialName("smtp_pass") val smtpPass: String? = null,
    @SerialName("staff_to") val staffTo: String? = null,
    val secret: String? = null,
    @SerialName("webhook_auth") val webhookAuth: String? = null,
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean? = null,
    @SerialName("bot_token") val botToken: String? = null,
    @SerialName("chat_id") val chatId: String? = null,
    @SerialName("server_url") val serverUrl: String? = null,
    val topic: String? = null,
    @SerialName("user_key") val userKey: String? = null,
    @SerialName("app_token") val appToken: String? = null,
    @SerialName("clear_app_token") val clearAppToken: Boolean? = null,
)

@Serializable
data class SettingsServerAlertTested(
    val sent: Boolean = false,
)

@Serializable
data class SettingsServerPackSettings(
    @SerialName("pack_alerts") val packAlerts: String = "",
    @SerialName("each_limit") val eachLimit: Int? = null,
)

@Serializable
data class SettingsServerBackup(
    val name: String = "",
    val kind: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val size: Long = 0,
    val detail: String = "",
    @SerialName("from_version") val fromVersion: String = "",
    @SerialName("to_version") val toVersion: String = "",
)

@Serializable
data class SettingsServerKeyKit(
    val needed: Boolean = false,
    val fingerprint: String = "",
    val level: String = "",
)

@Serializable
data class SettingsServerBackups(
    val backups: List<SettingsServerBackup> = emptyList(),
    val preset: String = "",
    @SerialName("restore_waiting") val restoreWaiting: Boolean = false,
    @SerialName("key_note") val keyNote: String = "",
    @SerialName("key_kit") val keyKit: SettingsServerKeyKit? = null,
)

@Serializable
data class SettingsServerPreset(
    val preset: String = "",
)

@Serializable
data class SettingsServerRestoreStaged(
    val staged: Boolean = false,
    val restarting: Boolean = false,
    val note: String = "",
)

@Serializable
data class SettingsServerLogLine(
    val at: String = "",
    val level: String = "",
    val text: String = "",
)

@Serializable
data class SettingsServerLogPage(
    val lines: List<SettingsServerLogLine> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 0,
)
