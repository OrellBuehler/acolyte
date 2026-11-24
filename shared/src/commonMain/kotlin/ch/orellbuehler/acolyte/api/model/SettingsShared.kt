package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsHealthStates(
    @SerialName("safe_mode") val safeMode: Boolean = false,
    val states: List<SettingsStageState> = emptyList(),
)

@Serializable
data class SettingsStageState(
    val key: String = "",
    val kind: String = "",
    val name: String = "",
    val state: String = "",
    val reason: String = "",
    val since: String = "",
    val stuck: Boolean = false,
    val restarts: Int = 0,
    @SerialName("every_s") val everySeconds: Long = 0,
    @SerialName("last_pass") val lastPass: String = "",
    @SerialName("next_pass") val nextPass: String = "",
    val part: String = "",
)

@Serializable
data class SettingsSchedules(
    val schedules: List<SettingsScheduleRow> = emptyList(),
    val loops: List<SettingsStageState> = emptyList(),
    @SerialName("safe_mode") val safeMode: Boolean = false,
)

@Serializable
data class SettingsScheduleRow(
    val kind: String = "",
    val label: String = "",
    val about: String = "",
    @SerialName("every_s") val everySeconds: Long = 0,
    @SerialName("min_s") val minSeconds: Long = 0,
    @SerialName("max_s") val maxSeconds: Long = 0,
    @SerialName("step_s") val stepSeconds: Long = 0,
    val enabled: Boolean = false,
    @SerialName("last_run") val lastRun: String = "",
    @SerialName("next_run") val nextRun: String = "",
    val state: String = "",
    @SerialName("last_job") val lastJob: String = "",
    @SerialName("last_job_ended") val lastJobEnded: String = "",
    @SerialName("last_job_error") val lastJobError: String = "",
    @SerialName("last_job_note") val lastJobNote: String = "",
    @SerialName("last_job_s") val lastJobSeconds: Double = 0.0,
    @SerialName("last_checked") val lastChecked: String = "",
    @SerialName("last_skip") val lastSkip: String = "",
    val part: String = "",
    val settings: String = "",
)

@Serializable
data class SettingsScheduleRun(
    @SerialName("job_id") val jobId: Long = 0,
    val already: Boolean = false,
)

@Serializable
data class SettingsUpdateInfo(
    val on: Boolean = false,
    val feed: String = "",
    @SerialName("checked_at") val checkedAt: String = "",
    val latest: String = "",
    val newer: Boolean = false,
    val link: String = "",
    @SerialName("no_release") val noRelease: Boolean = false,
    val notes: String = "",
    val problem: String = "",
    val version: String = "",
)

@Serializable
data class SettingsUiSettings(
    @SerialName("autoplay_trailers") val autoplayTrailers: Boolean = true,
    @SerialName("signin_library_art") val signInLibraryArt: Boolean = false,
    @SerialName("artwork_cache_gb") val artworkCacheGb: Int = 5,
    @SerialName("history_routine_days") val historyRoutineDays: Int = 30,
    @SerialName("history_other_days") val historyOtherDays: Int = 0,
    @SerialName("count_install") val countInstall: Boolean = true,
)

@Serializable
internal data class SettingsUiChange(
    @SerialName("autoplay_trailers") val autoplayTrailers: Boolean? = null,
    @SerialName("signin_library_art") val signInLibraryArt: Boolean? = null,
    @SerialName("artwork_cache_gb") val artworkCacheGb: Int? = null,
    @SerialName("history_routine_days") val historyRoutineDays: Int? = null,
    @SerialName("history_other_days") val historyOtherDays: Int? = null,
    @SerialName("count_install") val countInstall: Boolean? = null,
)
