package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsSourceUse(
    val feed: Boolean = false,
    @SerialName("auto_search") val autoSearch: Boolean = false,
    @SerialName("hand_search") val handSearch: Boolean = false,
)

@Serializable
data class SettingsSourceLimits(
    @SerialName("searches_per_window") val searchesPerWindow: Int = 0,
    @SerialName("grabs_per_window") val grabsPerWindow: Int = 0,
    @SerialName("window_seconds") val windowSeconds: Int = 0,
    @SerialName("gap_ms") val gapMs: Int = 0,
    @SerialName("timeout_ms") val timeoutMs: Int = 0,
)

@Serializable
data class SettingsSourceHealth(
    val state: String = "",
    val text: String = "",
    val until: String = "",
    val reason: String = "",
    val block: String = "",
    val cause: String = "",
)

@Serializable
data class SettingsSourceSummary(
    @SerialName("last_success") val lastSuccess: String? = null,
    @SerialName("last_failure") val lastFailure: String? = null,
    @SerialName("calls_24h") val calls24h: Int = 0,
    @SerialName("failures_24h") val failures24h: Int = 0,
    @SerialName("avg_ms_24h") val avgMs24h: Int = 0,
)

@Serializable
data class SourceRef(
    val id: Long = 0,
    val name: String = "",
    @SerialName("protocol") val protocolName: String = "",
    val format: String = "",
    val site: String = "",
    @SerialName("site_user") val siteUser: String = "",
    @SerialName("site_login") val siteLogin: String = "",
    @SerialName("site_agent") val siteAgent: String = "",
    val url: String = "",
    @SerialName("api_key") val apiKey: String = "",
    @SerialName("use_for") val useFor: SettingsSourceUse = SettingsSourceUse(),
    val kinds: List<String> = emptyList(),
    val categories: Map<String, List<Int>>? = null,
    @SerialName("min_seeders") val minSeeders: Int? = null,
    val limits: SettingsSourceLimits = SettingsSourceLimits(),
    val priority: Int = 0,
    val state: String = "",
    val health: SettingsSourceHealth = SettingsSourceHealth(),
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean = false,
    val proxy: String = "",
    @SerialName("proxy_password") val proxyPassword: String = "",
    @SerialName("proxy_allow_local_network") val proxyAllowLocalNetwork: Boolean = false,
    val summary: SettingsSourceSummary = SettingsSourceSummary(),
    val tags: List<String> = emptyList(),
    @SerialName("multi_languages") val multiLanguages: List<String> = emptyList(),
    @SerialName("no_language_languages") val noLanguageLanguages: List<String> = emptyList(),
    @SerialName("downloader_id") val downloaderId: Long = 0,
)

@Serializable
data class SourceList(
    val sources: List<SourceRef> = emptyList(),
)

@Serializable
data class SettingsSourceInput(
    val name: String = "",
    val format: String = "",
    val site: String? = null,
    @SerialName("site_user") val siteUser: String? = null,
    @SerialName("site_login") val siteLogin: String? = null,
    @SerialName("site_agent") val siteAgent: String? = null,
    @SerialName("protocol") val protocolName: String = "",
    val url: String = "",
    @SerialName("api_key") val apiKey: String = "",
    @SerialName("use_for") val useFor: SettingsSourceUse = SettingsSourceUse(),
    val kinds: List<String> = emptyList(),
    @SerialName("min_seeders") val minSeeders: Int = 0,
    val priority: Int = 25,
    val limits: SettingsSourceLimits = SettingsSourceLimits(),
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean = false,
    val proxy: String = "",
    @SerialName("proxy_password") val proxyPassword: String = "",
    @SerialName("proxy_allow_local_network") val proxyAllowLocalNetwork: Boolean = false,
    val tags: List<String> = emptyList(),
    @SerialName("multi_languages") val multiLanguages: List<String> = emptyList(),
    @SerialName("no_language_languages") val noLanguageLanguages: List<String> = emptyList(),
    @SerialName("downloader_id") val downloaderId: Long = 0,
)

@Serializable
data class SettingsSourceTest(
    val ok: Boolean = false,
    val ms: Int = 0,
    val text: String = "",
    val health: SettingsSourceHealth = SettingsSourceHealth(),
    val cause: String = "",
    val items: Int = 0,
)

@Serializable
data class SettingsSourceCheckBody(
    val query: String = "",
    val kind: String = "movie",
)

@Serializable
data class SettingsSourceCheckRow(
    val id: Long = 0,
    val name: String = "",
    val answered: Boolean = false,
    val paused: Boolean = false,
    val count: Int = 0,
    val ms: Int = 0,
    val error: String = "",
    val health: SettingsSourceHealth = SettingsSourceHealth(),
)

@Serializable
data class SettingsSourceCheckAnswer(
    val query: String = "",
    val kind: String = "",
    val sources: List<SettingsSourceCheckRow> = emptyList(),
)

@Serializable
data class SettingsSourceDay(
    val day: String = "",
    val calls: Int = 0,
    val failures: Int = 0,
    val grabs: Int = 0,
    @SerialName("avg_ms") val avgMs: Int = 0,
)

@Serializable
data class SettingsSourceCall(
    val at: String = "",
    val op: String = "",
    val ok: Boolean = false,
    val ms: Int = 0,
    @SerialName("error_kind") val errorKind: String = "",
)

@Serializable
data class SettingsSourceStats(
    val source: SourceRef = SourceRef(),
    val days: List<SettingsSourceDay> = emptyList(),
    val recent: List<SettingsSourceCall> = emptyList(),
)

@Serializable
data class SettingsBridge(
    val id: Long = 0,
    val kind: String = "",
    val url: String = "",
    @SerialName("callback_url") val callbackUrl: String = "",
    @SerialName("last_check") val lastCheck: String = "",
    @SerialName("last_error") val lastError: String = "",
    val sites: Int = 0,
    val sources: Int = 0,
    val local: Boolean = false,
    val keys: Map<String, String> = emptyMap(),
)

@Serializable
data class SettingsBridgeList(
    val bridges: List<SettingsBridge> = emptyList(),
    @SerialName("home_ip") val homeIp: String = "",
)

@Serializable
data class SettingsBridgeBody(
    val kind: String = "",
    val url: String = "",
    val key: String = "",
    @SerialName("callback_url") val callbackUrl: String = "",
    @SerialName("new_keys") val newKeys: Boolean = false,
    val keys: Map<String, String> = emptyMap(),
)

@Serializable
data class SettingsBridgeTestAnswer(
    val ok: Boolean = false,
    val text: String = "",
    val sites: List<String> = emptyList(),
    val keys: Map<String, String> = emptyMap(),
)

@Serializable
data class SettingsBridgeSyncResult(
    val sites: Int = 0,
    val added: Int = 0,
    val updated: Int = 0,
    val removed: Int = 0,
    val skipped: Int = 0,
)

@Serializable
data class SettingsBridgeCheckAnswer(
    val result: SettingsBridgeSyncResult? = null,
    val bridge: SettingsBridge? = null,
)

@Serializable
data class SettingsBridgeRemoved(
    val sources: Int = 0,
    val kept: Boolean = false,
)
