package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsDownloaderPath(
    val theirs: String = "",
    val ours: String = "",
    val link: Boolean = false,
)

@Serializable
data class SettingsDownloaderSeeding(
    val remove: Boolean = false,
    val ratio: Double? = null,
    val minutes: Int? = null,
    @SerialName("delete_files") val deleteFiles: Boolean = false,
)

@Serializable
data class SettingsDownloaderHealthView(
    val state: String = "",
    val label: String = "",
    val text: String = "",
    val cause: String = "",
)

@Serializable
data class SettingsDownloaderBody(
    val name: String = "",
    val kind: String? = null,
    val url: String? = null,
    val username: String? = null,
    val secret: String? = null,
    @SerialName("clear_secret") val clearSecret: Boolean? = null,
    val category: String? = null,
    @SerialName("category_movies") val categoryMovies: String? = null,
    @SerialName("category_shows") val categoryShows: String? = null,
    @SerialName("category_music") val categoryMusic: String? = null,
    val priority: Int? = null,
    @SerialName("path_map") val pathMap: List<SettingsDownloaderPath>? = null,
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean? = null,
    val writable: Boolean? = null,
    @SerialName("clear_finished") val clearFinished: Boolean? = null,
    val tags: List<String>? = null,
    @SerialName("after_seeding") val afterSeeding: SettingsDownloaderSeeding? = null,
    @SerialName("category_after_import") val categoryAfterImport: String? = null,
    @SerialName("own_only") val ownOnly: Boolean? = null,
)

@Serializable
data class SettingsDownloaderTestAnswer(
    val ok: Boolean = false,
    val ms: Long = 0,
    val text: String = "",
    val health: SettingsDownloaderHealthView = SettingsDownloaderHealthView(),
    val cause: String = "",
    val version: String = "",
    val category: String = "",
    @SerialName("can_make") val canMake: Boolean = false,
    @SerialName("make_off") val makeOff: Boolean = false,
)

@Serializable
data class SettingsDownloaderPicks(
    val movie: String = "",
    val show: String = "",
    val artist: String = "",
)

@Serializable
data class SettingsDownloaderCategories(
    val listed: Boolean = false,
    val categories: List<String> = emptyList(),
    val picks: SettingsDownloaderPicks = SettingsDownloaderPicks(),
)

@Serializable
data class SettingsDownloaderCategoryBody(
    val name: String = "",
)

@Serializable
data class SettingsDownloaderAttempt(
    val id: Long = 0,
    @SerialName("downloader_id") val downloaderId: Long = 0,
    @SerialName("release_id") val releaseId: Long = 0,
    @SerialName("slot_id") val slotId: Long = 0,
    val state: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class SettingsDownloaderAttempts(
    val attempts: List<SettingsDownloaderAttempt> = emptyList(),
)

@Serializable
data class SettingsKeyLookupBody(
    val kind: String = "sabnzbd",
    val url: String = "",
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean = false,
    val username: String = "",
    val password: String = "",
)

@Serializable
data class SettingsKeyLookupAnswer(
    val key: String = "",
    @SerialName("need_login") val needLogin: Boolean = false,
    val problem: String = "",
)
