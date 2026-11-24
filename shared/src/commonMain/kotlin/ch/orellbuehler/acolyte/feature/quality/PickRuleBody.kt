package ch.orellbuehler.acolyte.feature.quality

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

object SettingsPickRuleChoices {
    val videoLadder: List<String> =
        listOf(
            "cam",
            "telesync",
            "sd_tv",
            "sd_dvd",
            "720p_tv",
            "720p_web",
            "720p_bluray",
            "1080p_tv",
            "1080p_web",
            "1080p_bluray",
            "1080p_remux",
            "2160p_web",
            "2160p_bluray",
            "2160p_remux",
        )

    val musicLadder: List<String> = listOf("lossy_low", "lossy_mid", "lossy_high", "lossless", "lossless_hires")

    val videoTraits: List<String> =
        listOf(
            "hevc",
            "ddp_audio",
            "web_dl",
            "upscaled",
            "dubbed_only",
            "hardcoded_subs",
            "3d",
            "cam",
            "telesync",
            "remux",
            "av1",
            "dv",
            "hdr10",
            "hdr10plus",
            "atmos",
            "h264",
            "dts_audio",
            "truehd_audio",
            "dv_no_fallback",
        )

    val musicTraits: List<String> =
        listOf(
            "cd_source",
            "web_source",
            "vinyl_source",
            "log_cue",
            "hires",
            "mp3",
            "aac",
            "opus",
            "vorbis",
            "flac",
            "alac",
            "single_image",
            "lossy_transcode",
            "deluxe",
        )

    val safetyTraits: List<String> = listOf("sample", "executable", "password_protected", "disc_image")

    val languages: List<String> =
        listOf(
            "en",
            "fr",
            "de",
            "it",
            "es",
            "ru",
            "ja",
            "ko",
            "pl",
            "nl",
            "pt",
            "sv",
            "da",
            "no",
            "fi",
            "cs",
            "hu",
            "el",
            "he",
            "tr",
            "ar",
            "hi",
            "zh",
            "th",
            "vi",
            "uk",
        )

    val frenchVersions: List<String> = listOf("VFF", "VFQ", "VFI", "VF2")

    val devices: List<String> = listOf("apple_tv", "fire_tv", "roku", "chromecast", "web_browser")

    val kinds: List<String> = listOf("movie", "show", "music")

    val orders: List<String> = listOf("quality_then_points", "points_then_quality")

    val properModes: List<String> = listOf("take_any", "same_group_only", "prefer_only", "ignore")

    val prefers: List<String> = listOf("either", "torrent", "usenet")

    val trashApps: List<String> = listOf("sonarr", "radarr")

    fun ladderFor(kinds: List<String>): List<String> = if ("music" in kinds) musicLadder else videoLadder

    fun traitsFor(kinds: List<String>): List<String> = if ("music" in kinds) musicTraits else videoTraits
}

internal val EMPTY = JsonObject(emptyMap())

internal const val BYTES_PER_GB: Long = 1L shl 30
internal const val SIZE_OPEN: Long = 1L shl 50
internal const val MAX_POINTS: Long = 2147483647L
internal const val OWN_PREFIX = "own:"

internal data class StepEntry(
    val quality: String,
    val allowed: Boolean,
    val extra: JsonObject = EMPTY,
)

internal data class TraitEntry(
    val trait: String,
    val points: Long,
    val extra: JsonObject = EMPTY,
)

internal data class SizeEntry(
    val min: Long,
    val max: Long,
    val preferred: Long = 0,
    val perMinute: Boolean = true,
    val extra: JsonObject = EMPTY,
)

internal data class PickBody(
    val steps: List<StepEntry> = emptyList(),
    val goodEnoughAt: String = "",
    val traits: List<TraitEntry> = emptyList(),
    val mustHave: List<String> = emptyList(),
    val never: List<String> = emptyList(),
    val blockedGroups: List<String> = emptyList(),
    val minPoints: Long = 0,
    val goodEnoughPoints: Long? = null,
    val sizeLimits: Map<String, SizeEntry> = emptyMap(),
    val prefer: String = "either",
    val waitTorrent: Long = 0,
    val waitUsenet: Long = 0,
    val skipWait: Boolean = false,
    val protocolExtra: JsonObject = EMPTY,
    val languages: List<String> = emptyList(),
    val originalOk: Boolean = true,
    val languageWait: Long = 0,
    val frenchVersions: List<String> = emptyList(),
    val languageExtra: JsonObject = EMPTY,
    val order: String = "quality_then_points",
    val upgradeMinPoints: Long = 10,
    val replaceUnderMin: Boolean = false,
    val properMode: String = "take_any",
    val properAge: Long = 0,
    val properExtra: JsonObject = EMPTY,
    val tags: List<String> = emptyList(),
    val devices: List<String> = emptyList(),
    val extra: JsonObject = EMPTY,
) {
    val synced: JsonObject? get() = extra["synced"] as? JsonObject

    fun toJson(): JsonObject {
        val out = LinkedHashMap<String, JsonElement>()
        out.putAll(extra)
        out["steps"] = JsonArray(steps.map { it.json() })
        out["good_enough_at"] = JsonPrimitive(goodEnoughAt)
        out["traits"] = JsonArray(traits.map { it.json() })
        out["must_have"] = strings(mustHave)
        out["never"] = strings(never)
        if (blockedGroups.isNotEmpty()) out["blocked_groups"] = strings(blockedGroups) else out.remove("blocked_groups")
        out["min_points"] = JsonPrimitive(minPoints)
        if (goodEnoughPoints != null) out["good_enough_points"] = JsonPrimitive(goodEnoughPoints) else out.remove("good_enough_points")
        out["size_limits"] = JsonObject(sizeLimits.mapValues { it.value.json() })
        out["protocol"] =
            merged(
                protocolExtra,
                "prefer" to JsonPrimitive(prefer),
                "wait_minutes_torrent" to JsonPrimitive(waitTorrent),
                "wait_minutes_usenet" to JsonPrimitive(waitUsenet),
                "skip_wait_at_good_enough" to JsonPrimitive(skipWait),
            )
        val language = LinkedHashMap<String, JsonElement>(languageExtra)
        language["wanted"] = strings(languages)
        language["original_ok"] = JsonPrimitive(originalOk)
        if (languageWait != 0L) language["wait_days"] = JsonPrimitive(languageWait) else language.remove("wait_days")
        if (frenchVersions.isNotEmpty()) language["french_versions"] = strings(frenchVersions) else language.remove("french_versions")
        out["language"] = JsonObject(language)
        out["order"] = JsonPrimitive(order)
        out["upgrade_min_points"] = JsonPrimitive(upgradeMinPoints)
        if (replaceUnderMin) out["replace_under_min"] = JsonPrimitive(true) else out.remove("replace_under_min")
        val proper = LinkedHashMap<String, JsonElement>(properExtra)
        proper["mode"] = JsonPrimitive(properMode)
        if (properAge != 0L) proper["max_age_days"] = JsonPrimitive(properAge) else proper.remove("max_age_days")
        out["proper_policy"] = JsonObject(proper)
        if (tags.isNotEmpty()) out["tags"] = strings(tags) else out.remove("tags")
        if (devices.isNotEmpty()) out["devices"] = strings(devices) else out.remove("devices")
        return JsonObject(out)
    }

    companion object {
        private val KNOWN =
            setOf(
                "steps",
                "good_enough_at",
                "traits",
                "must_have",
                "never",
                "blocked_groups",
                "min_points",
                "good_enough_points",
                "size_limits",
                "protocol",
                "language",
                "order",
                "upgrade_min_points",
                "replace_under_min",
                "proper_policy",
                "tags",
                "devices",
            )

        fun from(json: JsonObject): PickBody {
            val protocol = json["protocol"] as? JsonObject ?: EMPTY
            val language = json["language"] as? JsonObject ?: EMPTY
            val proper = json["proper_policy"] as? JsonObject ?: EMPTY
            val limits = json["size_limits"] as? JsonObject ?: EMPTY
            return PickBody(
                steps =
                    objects(json["steps"]).map {
                        StepEntry(it.text("quality"), it.flag("allowed"), it.without("quality", "allowed"))
                    },
                goodEnoughAt = json.text("good_enough_at"),
                traits =
                    objects(json["traits"]).map {
                        TraitEntry(it.text("trait"), it.number("points"), it.without("trait", "points"))
                    },
                mustHave = json.list("must_have"),
                never = json.list("never"),
                blockedGroups = json.list("blocked_groups"),
                minPoints = json.number("min_points"),
                goodEnoughPoints = (json["good_enough_points"] as? JsonPrimitive)?.longOrNull,
                sizeLimits =
                    limits.entries
                        .mapNotNull { (step, value) ->
                            (value as? JsonObject)?.let {
                                step to
                                    SizeEntry(
                                        it.number("min"),
                                        it.number("max"),
                                        it.number("preferred"),
                                        it.flag("per_minute"),
                                        it.without("min", "max", "preferred", "per_minute"),
                                    )
                            }
                        }.toMap(),
                prefer = protocol.text("prefer").ifEmpty { "either" },
                waitTorrent = protocol.number("wait_minutes_torrent"),
                waitUsenet = protocol.number("wait_minutes_usenet"),
                skipWait = protocol.flag("skip_wait_at_good_enough"),
                protocolExtra = protocol.without("prefer", "wait_minutes_torrent", "wait_minutes_usenet", "skip_wait_at_good_enough"),
                languages = language.list("wanted"),
                originalOk = language.flag("original_ok"),
                languageWait = language.number("wait_days"),
                frenchVersions = language.list("french_versions"),
                languageExtra = language.without("wanted", "original_ok", "wait_days", "french_versions"),
                order = json.text("order").ifEmpty { "quality_then_points" },
                upgradeMinPoints = json.number("upgrade_min_points"),
                replaceUnderMin = json.flag("replace_under_min"),
                properMode = proper.text("mode").ifEmpty { "take_any" },
                properAge = proper.number("max_age_days"),
                properExtra = proper.without("mode", "max_age_days"),
                tags = json.list("tags"),
                devices = json.list("devices"),
                extra = JsonObject(json.filterKeys { it !in KNOWN }),
            )
        }

        fun blank(kind: String): PickBody {
            val music = kind == "music"
            val on =
                if (music) {
                    listOf(
                        "lossy_high",
                        "lossless",
                        "lossless_hires",
                    )
                } else {
                    listOf("720p_tv", "720p_web", "720p_bluray", "1080p_tv", "1080p_web", "1080p_bluray")
                }
            val ladder = if (music) SettingsPickRuleChoices.musicLadder else SettingsPickRuleChoices.videoLadder
            return PickBody(
                steps = ladder.map { StepEntry(it, it in on) },
                goodEnoughAt = if (music) "lossless" else "1080p_web",
                never = SettingsPickRuleChoices.safetyTraits,
                sizeLimits =
                    if (music) {
                        emptyMap()
                    } else {
                        mapOf("*" to SizeEntry(BYTES_PER_GB / 2 / 60, 6 * BYTES_PER_GB / 60, 0, true))
                    },
            )
        }
    }
}

private fun StepEntry.json(): JsonObject =
    JsonObject(
        LinkedHashMap<String, JsonElement>(extra).also {
            it["quality"] = JsonPrimitive(quality)
            it["allowed"] = JsonPrimitive(allowed)
        },
    )

private fun TraitEntry.json(): JsonObject =
    JsonObject(
        LinkedHashMap<String, JsonElement>(extra).also {
            it["trait"] = JsonPrimitive(trait)
            it["points"] = JsonPrimitive(points)
        },
    )

private fun SizeEntry.json(): JsonObject =
    JsonObject(
        LinkedHashMap<String, JsonElement>(extra).also {
            it["min"] = JsonPrimitive(min)
            it["max"] = JsonPrimitive(max)
            if (preferred != 0L) it["preferred"] = JsonPrimitive(preferred) else it.remove("preferred")
            it["per_minute"] = JsonPrimitive(perMinute)
        },
    )

private fun strings(values: List<String>): JsonArray = JsonArray(values.map { JsonPrimitive(it) })

private fun merged(
    base: JsonObject,
    vararg values: Pair<String, JsonElement>,
): JsonObject = JsonObject(LinkedHashMap<String, JsonElement>(base).also { map -> values.forEach { map[it.first] = it.second } })

private fun objects(element: JsonElement?): List<JsonObject> = (element as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

private fun JsonObject.text(key: String): String = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull.orEmpty()

private fun JsonObject.number(key: String): Long = (this[key] as? JsonPrimitive)?.longOrNull ?: 0

private fun JsonObject.flag(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: false

private fun JsonObject.list(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p !is JsonNull }?.contentOrNull }.orEmpty()

private fun JsonObject.without(vararg keys: String): JsonObject = JsonObject(filterKeys { it !in keys })
