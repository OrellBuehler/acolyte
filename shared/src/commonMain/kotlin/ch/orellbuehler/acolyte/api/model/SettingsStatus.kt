package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsStatusSyncTrouble(
    val id: Long = 0,
    val name: String = "",
    val health: String = "",
    val stale: Boolean = false,
    @SerialName("seen_at") val seenAt: String = "",
)

@Serializable
data class SettingsStatusDisk(
    val path: String = "",
    val kind: String = "",
    val label: String = "",
    @SerialName("free_bytes") val freeBytes: Long = -1,
    @SerialName("total_bytes") val totalBytes: Long = -1,
)

@Serializable
data class SettingsStatusDisks(
    val disks: List<SettingsStatusDisk> = emptyList(),
)

@Serializable
data class SettingsStatusActiveJob(
    val id: Long = 0,
    val kind: String = "",
    val label: String = "",
    val state: String = "",
    val progress: Double = 0.0,
    val note: String = "",
    @SerialName("queued_at") val queuedAt: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("by_person") val byPerson: Boolean = false,
)

@Serializable
data class SettingsStatusActiveJobs(
    val jobs: List<SettingsStatusActiveJob> = emptyList(),
)

@Serializable
data class SettingsStatusLaunch(
    val source: String = "",
    val rollback: String = "",
    @SerialName("rollback_at") val rollbackAt: String = "",
    val chosen: Boolean = false,
    val left: String = "",
    val to: String = "",
    @SerialName("exec_error") val execError: String = "",
    val off: Boolean = false,
)

@Serializable
data class SettingsStatusProgress(
    val phase: String = "idle",
    val version: String = "",
    val message: String = "",
    val at: String = "",
)

@Serializable
data class SettingsStatusInstall(
    val available: Boolean = false,
    val mode: String = "off",
    @SerialName("mode_locked") val modeLocked: Boolean = false,
    @SerialName("check_on") val checkOn: Boolean = false,
    val source: String = "image",
    val previous: String = "",
    val launch: SettingsStatusLaunch = SettingsStatusLaunch(),
    val progress: SettingsStatusProgress = SettingsStatusProgress(),
    val ready: String = "",
    @SerialName("one_way") val oneWay: Boolean = false,
    val running: String = "",
)

@Serializable
internal data class SettingsStatusModeChange(
    val mode: String? = null,
    @SerialName("check_on") val checkOn: Boolean? = null,
)

@Serializable
data class SettingsHealthChecksSettings(
    @SerialName("every_minutes") val everyMinutes: Int = 5,
    @SerialName("flap_guard") val flapGuard: Int = 2,
    @SerialName("low_space_mb") val lowSpaceMb: Int = 1024,
    @SerialName("update_check_url") val updateCheckUrl: String = "",
    @SerialName("update_check_local") val updateCheckLocal: Boolean = false,
    @SerialName("update_prerelease") val updatePrerelease: Boolean = false,
    @SerialName("update_check_every_days") val updateCheckEveryDays: Int = 1,
    @SerialName("update_check_default") val updateCheckDefault: String = "",
)

@Serializable
internal data class SettingsHealthChecksChange(
    @SerialName("every_minutes") val everyMinutes: Int,
    @SerialName("flap_guard") val flapGuard: Int,
    @SerialName("low_space_mb") val lowSpaceMb: Int,
    @SerialName("update_check_url") val updateCheckUrl: String,
    @SerialName("update_check_local") val updateCheckLocal: Boolean,
    @SerialName("update_prerelease") val updatePrerelease: Boolean,
    @SerialName("update_check_every_days") val updateCheckEveryDays: Int,
)

@Serializable
data class SettingsStatusAlert(
    val id: Long = 0,
    val name: String = "",
    val enabled: Boolean = false,
    @SerialName("last_sent_at") val lastSentAt: String = "",
    @SerialName("last_error") val lastError: String = "",
)

@Serializable
data class SettingsStatusAlerts(
    val alerts: List<SettingsStatusAlert> = emptyList(),
)

@Serializable
data class SettingsStatusTest(
    val ok: Boolean = false,
    val text: String = "",
)

@Serializable
data class SettingsStatusMetadata(
    val up: Boolean = true,
    val message: String = "",
)
