package ch.orellbuehler.acolyte.feature.acquisition

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleConfig
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleKind
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleLimits
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSite
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSiteInput
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleTidy
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleTranslatorInput
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleWant
import ch.orellbuehler.acolyte.api.model.SubtitleLanguage
import ch.orellbuehler.acolyte.api.model.SubtitleTranslator
import ch.orellbuehler.acolyte.feature.Tone
import kotlin.math.floor

const val SETTINGS_SUBTITLE_MAX_LANGUAGES: Int = 10
const val SETTINGS_SUBTITLE_EVERY_LANGUAGE: String = "*"

val SETTINGS_SUBTITLE_HEARING_TYPES: List<String> = listOf("normal", "hi", "either")
val SETTINGS_SUBTITLE_HI_SUFFIXES: List<String> = listOf("hi", "cc", "sdh")
val SETTINGS_SUBTITLE_ACCOUNT_FIELDS: List<String> = listOf("username", "password", "api_key")

private val DEFAULT_WANT_LANGUAGES = listOf("en", "es", "fr", "de")

enum class SettingsSubtitleNumber(
    val key: String,
    val min: Int,
    val max: Int,
    val whole: Boolean,
) {
    PASS_EPISODE("pass_mark_episode", 0, 100, true),
    PASS_MOVIE("pass_mark_movie", 0, 100, true),
    SEARCH_HOURS("search_hours", 1, 168, true),
    BATCH("batch", 1, 1000, true),
    BETTER_HOURS("better_hours", 6, 168, true),
    BETTER_DAYS("better_days", 0, 30, true),
    SYNC_STEP_MS("sync_step_ms", 10, 60000, true),
    SYNC_FROM_FPS("sync_from_fps", 1, 120, false),
    SYNC_TO_FPS("sync_to_fps", 1, 120, false),
    ;

    fun parse(text: String): Double? = parseSettingsSubtitleNumber(text, min, max, whole)

    internal fun text(config: SettingsSubtitleConfig): String =
        when (this) {
            PASS_EPISODE -> config.passMarkEpisode.toString()
            PASS_MOVIE -> config.passMarkMovie.toString()
            SEARCH_HOURS -> config.searchHours.toString()
            BATCH -> config.batch.toString()
            BETTER_HOURS -> config.betterHours.toString()
            BETTER_DAYS -> config.betterDays.toString()
            SYNC_STEP_MS -> config.syncStepMs.toString()
            SYNC_FROM_FPS -> settingsSubtitleFps(config.syncFromFps)
            SYNC_TO_FPS -> settingsSubtitleFps(config.syncToFps)
        }

    internal fun apply(
        config: SettingsSubtitleConfig,
        value: Double,
    ): SettingsSubtitleConfig =
        when (this) {
            PASS_EPISODE -> config.copy(passMarkEpisode = value.toInt())
            PASS_MOVIE -> config.copy(passMarkMovie = value.toInt())
            SEARCH_HOURS -> config.copy(searchHours = value.toInt())
            BATCH -> config.copy(batch = value.toInt())
            BETTER_HOURS -> config.copy(betterHours = value.toInt())
            BETTER_DAYS -> config.copy(betterDays = value.toInt())
            SYNC_STEP_MS -> config.copy(syncStepMs = value.toInt())
            SYNC_FROM_FPS -> config.copy(syncFromFps = value)
            SYNC_TO_FPS -> config.copy(syncToFps = value)
        }

    companion object {
        fun of(key: String): SettingsSubtitleNumber? = entries.firstOrNull { it.key == key }
    }
}

fun parseSettingsSubtitleNumber(
    text: String,
    min: Int,
    max: Int,
    whole: Boolean,
): Double? {
    val trimmed = text.trim()
    val value = (if (whole) trimmed.toIntOrNull()?.toDouble() else trimmed.toDoubleOrNull()) ?: return null
    return value.takeIf { it >= min && it <= max }
}

fun settingsSubtitleFps(value: Double): String =
    if (value == floor(value) &&
        value < 1_000_000
    ) {
        value.toLong().toString()
    } else {
        value.toString()
    }

enum class SettingsSubtitleFlag(
    val key: String,
) {
    BETTER("better"),
    ADAPTIVE("adaptive"),
    USE_BUILT_IN("use_built_in"),
    IGNORE_PICTURE_TRACKS("ignore_picture_tracks"),
    ;

    internal fun get(config: SettingsSubtitleConfig): Boolean =
        when (this) {
            BETTER -> config.better
            ADAPTIVE -> config.adaptive
            USE_BUILT_IN -> config.useBuiltIn
            IGNORE_PICTURE_TRACKS -> config.ignorePictureTracks
        }

    internal fun set(
        config: SettingsSubtitleConfig,
        on: Boolean,
    ): SettingsSubtitleConfig =
        when (this) {
            BETTER -> config.copy(better = on)
            ADAPTIVE -> config.copy(adaptive = on)
            USE_BUILT_IN -> config.copy(useBuiltIn = on)
            IGNORE_PICTURE_TRACKS -> config.copy(ignorePictureTracks = on)
        }

    companion object {
        fun of(key: String): SettingsSubtitleFlag? = entries.firstOrNull { it.key == key }
    }
}

enum class SettingsSubtitleStep(
    val key: String,
) {
    ADS("ads"),
    HI("hi"),
    TAGS("tags"),
    EMOJI("emoji"),
    OCR("ocr"),
    CAPS("caps"),
    SPACING("spacing"),
    OVERLAP("overlap"),
    ;

    internal fun get(tidy: SettingsSubtitleTidy): Boolean =
        when (this) {
            ADS -> tidy.ads
            HI -> tidy.hi
            TAGS -> tidy.tags
            EMOJI -> tidy.emoji
            OCR -> tidy.ocr
            CAPS -> tidy.caps
            SPACING -> tidy.spacing
            OVERLAP -> tidy.overlap
        }

    internal fun set(
        tidy: SettingsSubtitleTidy,
        on: Boolean,
    ): SettingsSubtitleTidy =
        when (this) {
            ADS -> tidy.copy(ads = on)
            HI -> tidy.copy(hi = on)
            TAGS -> tidy.copy(tags = on)
            EMOJI -> tidy.copy(emoji = on)
            OCR -> tidy.copy(ocr = on)
            CAPS -> tidy.copy(caps = on)
            SPACING -> tidy.copy(spacing = on)
            OVERLAP -> tidy.copy(overlap = on)
        }

    companion object {
        fun of(key: String): SettingsSubtitleStep? = entries.firstOrNull { it.key == key }
    }
}

enum class SettingsSubtitleSiteNumber(
    val key: String,
    val min: Int,
    val max: Int,
) {
    PRIORITY("priority", 1, 100),
    GAP_MS("gap_ms", 0, 60000),
    PER_WINDOW("per_window", 1, 10000),
    WINDOW_SECONDS("window_seconds", 60, 86400),
    DOWNLOADS_PER_DAY("downloads_per_day", 0, 100000),
    ;

    fun parse(text: String): Int? = parseSettingsSubtitleNumber(text, min, max, true)?.toInt()

    companion object {
        fun of(key: String): SettingsSubtitleSiteNumber? = entries.firstOrNull { it.key == key }
    }
}

enum class SettingsSubtitleSiteFlag(
    val key: String,
) {
    ALLOW_LOCAL_NETWORK("allow_local_network"),
    MACHINE_MADE("machine_made"),
    ENABLED("enabled"),
    ;

    companion object {
        fun of(key: String): SettingsSubtitleSiteFlag? = entries.firstOrNull { it.key == key }
    }
}

enum class SettingsSubtitleWishProblem {
    TOO_MANY,
    PICK,
    TWICE,
}

fun settingsSubtitleWishProblem(wants: List<SettingsSubtitleWant>): SettingsSubtitleWishProblem? {
    if (wants.size > SETTINGS_SUBTITLE_MAX_LANGUAGES) return SettingsSubtitleWishProblem.TOO_MANY
    val seen = HashSet<String>()
    for (want in wants) {
        if (want.language.isEmpty()) return SettingsSubtitleWishProblem.PICK
        if (!seen.add("${want.language}|${want.forced}")) return SettingsSubtitleWishProblem.TWICE
    }
    return null
}

fun settingsSubtitleNewLanguage(
    wants: List<SettingsSubtitleWant>,
    languages: List<SubtitleLanguage>,
): String {
    val used = wants.map { it.language }.toSet()
    return (DEFAULT_WANT_LANGUAGES + languages.map { it.code }).firstOrNull { it !in used } ?: ""
}

fun SettingsSubtitleTidy.settingsSubtitleStep(key: String): Boolean = SettingsSubtitleStep.of(key)?.get(this) ?: false

fun settingsSubtitleCleanRows(clean: Map<String, SettingsSubtitleTidy>): List<String> =
    listOf(SETTINGS_SUBTITLE_EVERY_LANGUAGE) + clean.keys.filter { it != SETTINGS_SUBTITLE_EVERY_LANGUAGE }.sorted()

fun settingsSubtitleTestCount(text: String): String {
    val match = Regex("\\b(\\d+) (Releases?|subtitles?|indexers?|sites?)\\b", RegexOption.IGNORE_CASE).find(text) ?: return ""
    return "${match.groupValues[1]} ${match.groupValues[2]}"
}

enum class SettingsSubtitleCovers {
    BOTH,
    MOVIES,
    SHOWS,
}

fun SettingsSubtitleKind.covers(): SettingsSubtitleCovers =
    when {
        movies && shows -> SettingsSubtitleCovers.BOTH
        movies -> SettingsSubtitleCovers.MOVIES
        else -> SettingsSubtitleCovers.SHOWS
    }

enum class SettingsSubtitleSiteState(
    val tone: Tone,
) {
    NEEDS_ACCOUNT(Tone.FAIL),
    PAUSED(Tone.IDLE),
    RESTING(Tone.WAIT),
    ON(Tone.OK),
}

fun settingsSubtitleSiteState(
    site: SettingsSubtitleSite,
    kind: SettingsSubtitleKind?,
): SettingsSubtitleSiteState =
    when {
        kind?.needsAccount == true && !site.hasAccount -> SettingsSubtitleSiteState.NEEDS_ACCOUNT
        site.state == "disabled" -> SettingsSubtitleSiteState.PAUSED
        site.state == "paused" -> SettingsSubtitleSiteState.RESTING
        else -> SettingsSubtitleSiteState.ON
    }

data class SettingsSubtitleSiteRow(
    val id: Long = 0,
    val name: String = "",
    val url: String = "",
    val kindName: String = "",
    val offered: Boolean = false,
    val covers: SettingsSubtitleCovers = SettingsSubtitleCovers.BOTH,
    val priority: Int = 25,
    val needsAccount: Boolean = false,
    val hasAccount: Boolean = false,
    val allowLocalNetwork: Boolean = false,
    val downloadsToday: Int = 0,
    val downloadsMax: Int = 0,
    val state: SettingsSubtitleSiteState = SettingsSubtitleSiteState.ON,
    val turnedOff: Boolean = false,
    val pauseReason: String = "",
    val pausedUntil: String = "",
    val testing: Boolean = false,
    val testOk: Boolean? = null,
    val testMessage: String = "",
    val testExtra: String = "",
    val testError: ApiError? = null,
)

data class SettingsSubtitleTranslatorForm(
    val kind: String = "",
    val url: String = "",
    val model: String = "",
    val key: String = "",
    val clearKey: Boolean = false,
    val builtInAuto: Boolean = false,
    val hasKey: Boolean = false,
) {
    val valid: Boolean get() = kind.isEmpty() || (url.isNotBlank() && (kind != "openai" || model.isNotBlank()))
}

internal fun settingsSubtitleTranslatorForm(translator: SubtitleTranslator): SettingsSubtitleTranslatorForm =
    SettingsSubtitleTranslatorForm(
        kind = translator.kind,
        url = translator.url,
        model = translator.model,
        builtInAuto = translator.builtInAuto,
        hasKey = translator.hasKey,
    )

internal fun settingsSubtitleTranslatorBody(form: SettingsSubtitleTranslatorForm): SettingsSubtitleTranslatorInput {
    val key = form.key.trim()
    return SettingsSubtitleTranslatorInput(
        kind = form.kind,
        url = form.url.trim(),
        model = if (form.kind == "openai") form.model.trim() else "",
        key = key.ifEmpty { null },
        clearKey = if (key.isEmpty() && form.clearKey) true else null,
        builtInAuto = form.builtInAuto,
    )
}

data class SettingsSubtitleSiteForm(
    val isNew: Boolean = true,
    val id: Long = 0,
    val kind: String = "",
    val name: String = "",
    val url: String = "",
    val priority: String = "25",
    val gapMs: String = "1000",
    val perWindow: String = "60",
    val windowSeconds: String = "3600",
    val downloadsPerDay: String = "0",
    val username: String = "",
    val password: String = "",
    val apiKey: String = "",
    val allowLocalNetwork: Boolean = false,
    val machineMade: Boolean = false,
    val enabled: Boolean = true,
    val hasAccount: Boolean = false,
    val busy: Boolean = false,
    val missing: List<String> = emptyList(),
    val error: ApiError? = null,
) {
    fun number(key: String): String =
        when (key) {
            SettingsSubtitleSiteNumber.PRIORITY.key -> priority
            SettingsSubtitleSiteNumber.GAP_MS.key -> gapMs
            SettingsSubtitleSiteNumber.PER_WINDOW.key -> perWindow
            SettingsSubtitleSiteNumber.WINDOW_SECONDS.key -> windowSeconds
            SettingsSubtitleSiteNumber.DOWNLOADS_PER_DAY.key -> downloadsPerDay
            else -> ""
        }

    fun numberValid(key: String): Boolean = SettingsSubtitleSiteNumber.of(key)?.parse(number(key)) != null

    fun flag(key: String): Boolean =
        when (key) {
            SettingsSubtitleSiteFlag.ALLOW_LOCAL_NETWORK.key -> allowLocalNetwork
            SettingsSubtitleSiteFlag.MACHINE_MADE.key -> machineMade
            SettingsSubtitleSiteFlag.ENABLED.key -> enabled
            else -> false
        }

    fun account(field: String): String =
        when (field) {
            "username" -> username
            "password" -> password
            "api_key" -> apiKey
            else -> ""
        }

    val numbersValid: Boolean get() = SettingsSubtitleSiteNumber.entries.all { numberValid(it.key) }

    val canSubmit: Boolean get() = !busy && name.isNotBlank() && url.isNotBlank() && numbersValid
}

internal fun settingsSubtitleSiteForm(
    kind: SettingsSubtitleKind,
    site: SettingsSubtitleSite?,
): SettingsSubtitleSiteForm {
    val limits = site?.limits ?: SettingsSubtitleLimits()
    return SettingsSubtitleSiteForm(
        isNew = site == null,
        id = site?.id ?: 0,
        kind = kind.kind,
        name = site?.name ?: kind.name,
        url = site?.url ?: kind.defaultUrl.ifEmpty { "http://" },
        priority = (site?.priority ?: 25).toString(),
        gapMs = limits.gapMs.toString(),
        perWindow = limits.perWindow.toString(),
        windowSeconds = limits.windowSeconds.toString(),
        downloadsPerDay = limits.downloadsPerDay.toString(),
        allowLocalNetwork = site?.allowLocalNetwork ?: false,
        machineMade = site?.machineMade ?: false,
        enabled = if (site != null) site.state != "disabled" else !kind.defaultOff,
        hasAccount = site?.hasAccount ?: false,
    )
}

fun settingsSubtitleAccountMissing(
    kind: SettingsSubtitleKind,
    form: SettingsSubtitleSiteForm,
): List<String> {
    if (!kind.needsAccount) return emptyList()
    val typed = kind.account.filter { form.account(it).isNotEmpty() }
    if (form.hasAccount && typed.isEmpty()) return emptyList()
    return kind.account.filter { form.account(it).isEmpty() }
}

internal fun settingsSubtitleSiteBody(
    form: SettingsSubtitleSiteForm,
    enabled: Boolean = form.enabled,
): SettingsSubtitleSiteInput =
    SettingsSubtitleSiteInput(
        name = form.name.trim(),
        kind = form.kind,
        url = form.url.trim(),
        enabled = enabled,
        limits =
            SettingsSubtitleLimits(
                gapMs = SettingsSubtitleSiteNumber.GAP_MS.parse(form.gapMs) ?: 0,
                perWindow = SettingsSubtitleSiteNumber.PER_WINDOW.parse(form.perWindow) ?: 0,
                windowSeconds = SettingsSubtitleSiteNumber.WINDOW_SECONDS.parse(form.windowSeconds) ?: 0,
                downloadsPerDay = SettingsSubtitleSiteNumber.DOWNLOADS_PER_DAY.parse(form.downloadsPerDay) ?: 0,
            ),
        allowLocalNetwork = form.allowLocalNetwork,
        priority = SettingsSubtitleSiteNumber.PRIORITY.parse(form.priority) ?: 25,
        machineMade = form.machineMade,
        username = form.username.ifEmpty { null },
        password = form.password.ifEmpty { null },
        apiKey = form.apiKey.ifEmpty { null },
    )

enum class SettingsSubtitleNoteKind {
    SETTINGS_SAVED,
    TRANSLATOR_SAVED,
    SITE_SAVED,
    SITE_PAUSED,
    SITE_RESUMED,
    PAUSE_FAILED,
    RESUME_FAILED,
    REMOVE_FAILED,
    CLEAN_QUEUED,
    CLEAN_FAILED,
}

data class SettingsSubtitleNote(
    val id: Int,
    val kind: SettingsSubtitleNoteKind,
    val name: String = "",
    val error: ApiError? = null,
)
