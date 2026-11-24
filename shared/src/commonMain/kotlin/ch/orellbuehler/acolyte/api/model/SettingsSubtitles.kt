package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsSubtitleWant(
    val language: String = "",
    @SerialName("hearing_impaired") val hearingImpaired: String = "normal",
    val forced: Boolean = false,
    @SerialName("translate_from") val translateFrom: String = "",
)

@Serializable
data class SettingsSubtitleTidy(
    val ads: Boolean = false,
    val hi: Boolean = false,
    val tags: Boolean = false,
    val emoji: Boolean = false,
    val ocr: Boolean = false,
    val caps: Boolean = false,
    val spacing: Boolean = false,
    val overlap: Boolean = false,
)

@Serializable
data class SettingsSubtitleConfig(
    val languages: List<SettingsSubtitleWant> = emptyList(),
    @SerialName("pass_mark_movie") val passMarkMovie: Int = 70,
    @SerialName("pass_mark_episode") val passMarkEpisode: Int = 90,
    @SerialName("use_built_in") val useBuiltIn: Boolean = true,
    @SerialName("ignore_picture_tracks") val ignorePictureTracks: Boolean = false,
    @SerialName("hi_suffix") val hiSuffix: String = "hi",
    @SerialName("search_hours") val searchHours: Int = 6,
    val better: Boolean = true,
    @SerialName("better_hours") val betterHours: Int = 12,
    @SerialName("better_days") val betterDays: Int = 7,
    val batch: Int = 100,
    val adaptive: Boolean = true,
    @SerialName("sync_from_fps") val syncFromFps: Double = 25.0,
    @SerialName("sync_to_fps") val syncToFps: Double = 23.976,
    @SerialName("sync_step_ms") val syncStepMs: Int = 500,
    val clean: Map<String, SettingsSubtitleTidy> = mapOf("*" to SettingsSubtitleTidy(ads = true)),
)

@Serializable
data class SettingsSubtitleLimits(
    @SerialName("gap_ms") val gapMs: Int = 1000,
    @SerialName("per_window") val perWindow: Int = 60,
    @SerialName("window_seconds") val windowSeconds: Int = 3600,
    @SerialName("downloads_per_day") val downloadsPerDay: Int = 0,
)

@Serializable
data class SettingsSubtitleSite(
    @SerialName("ID") val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    val url: String = "",
    val state: String = "enabled",
    @SerialName("pause_reason") val pauseReason: String? = null,
    @SerialName("paused_until") val pausedUntil: String? = null,
    val failures: Int = 0,
    val limits: SettingsSubtitleLimits = SettingsSubtitleLimits(),
    @SerialName("downloads_today") val downloadsToday: Int = 0,
    @SerialName("downloads_reset_at") val downloadsResetAt: String? = null,
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean = false,
    val priority: Int = 25,
    @SerialName("machine_made") val machineMade: Boolean = false,
    @SerialName("has_account") val hasAccount: Boolean = false,
)

@Serializable
data class SettingsSubtitleSiteList(
    val sites: List<SettingsSubtitleSite> = emptyList(),
)

@Serializable
data class SettingsSubtitleKind(
    val kind: String = "",
    val name: String = "",
    @SerialName("default_url") val defaultUrl: String = "",
    val movies: Boolean = false,
    val shows: Boolean = false,
    @SerialName("needs_account") val needsAccount: Boolean = false,
    val account: List<String> = emptyList(),
    val note: String = "",
    @SerialName("default_off") val defaultOff: Boolean = false,
    val later: Boolean = false,
)

@Serializable
data class SettingsSubtitleKindList(
    val kinds: List<SettingsSubtitleKind> = emptyList(),
)

@Serializable
internal data class SettingsSubtitleSiteInput(
    val name: String = "",
    val kind: String = "",
    val url: String = "",
    val enabled: Boolean? = null,
    val limits: SettingsSubtitleLimits? = null,
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean = false,
    val priority: Int = 25,
    @SerialName("machine_made") val machineMade: Boolean? = null,
    val username: String? = null,
    val password: String? = null,
    @SerialName("api_key") val apiKey: String? = null,
)

@Serializable
data class SettingsSubtitleSiteTest(
    val ok: Boolean = false,
    val message: String = "",
    val found: Int = 0,
)

@Serializable
internal data class SettingsSubtitleTranslatorInput(
    val kind: String = "",
    val url: String = "",
    val model: String = "",
    val key: String? = null,
    @SerialName("clear_key") val clearKey: Boolean? = null,
    @SerialName("built_in_auto") val builtInAuto: Boolean = false,
)

@Serializable
data class SettingsSubtitleTranslatorTest(
    val ok: Boolean = false,
    val message: String = "",
    val models: List<String> = emptyList(),
)

@Serializable
data class SettingsSubtitleCleanQueued(
    val queued: Boolean = false,
)
