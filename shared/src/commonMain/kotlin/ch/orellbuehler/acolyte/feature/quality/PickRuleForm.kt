package ch.orellbuehler.acolyte.feature.quality

import kotlinx.serialization.json.JsonObject
import kotlin.math.roundToLong

data class SettingsPickRuleStep(
    val quality: String,
    val allowed: Boolean,
)

data class SettingsPickRuleTrait(
    val trait: String,
    val points: String,
)

data class SettingsPickRuleStepSize(
    val step: String,
    val min: String,
    val preferred: String,
    val max: String,
)

data class SettingsPickRuleProblem(
    val field: String,
    val code: String,
    val arg: String = "",
)

data class SettingsPickRuleForm(
    val name: String = "",
    val kinds: List<String> = listOf("movie", "show"),
    val steps: List<SettingsPickRuleStep> = emptyList(),
    val goodEnoughAt: String = "",
    val goodEnoughPoints: String = "",
    val traits: List<SettingsPickRuleTrait> = emptyList(),
    val mustHave: List<String> = emptyList(),
    val never: List<String> = emptyList(),
    val blockedGroups: List<String> = emptyList(),
    val devices: List<String> = emptyList(),
    val order: String = "quality_then_points",
    val upgradeMinPoints: String = "10",
    val minPoints: String = "0",
    val properMode: String = "take_any",
    val properAge: String = "",
    val sizeMin: String = "",
    val sizeMax: String = "",
    val stepSizes: List<SettingsPickRuleStepSize> = emptyList(),
    val replaceUnderMin: Boolean = false,
    val prefer: String = "either",
    val waitTorrent: String = "0",
    val waitUsenet: String = "0",
    val skipWait: Boolean = false,
    val tags: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val frenchVersions: List<String> = emptyList(),
    val originalOk: Boolean = true,
    val languageWait: String = "",
    internal val base: JsonObject = PickBody().toJson(),
) {
    internal val loaded: PickBody get() = PickBody.from(base)

    val music: Boolean get() = "music" in kinds

    val stepsBestFirst: List<SettingsPickRuleStep> get() = steps.reversed()

    val allowedBestFirst: List<SettingsPickRuleStep> get() = steps.filter { it.allowed }.reversed()

    val synced: SettingsPickRuleSynced?
        get() =
            loaded.synced?.let {
                SettingsPickRuleSynced(it.str("name"), it.str("app"), it.str("profile_id"), it.str("at"), it.str("from"))
            }

    val sizeChoices: List<String> get() = allowedBestFirst.map { it.quality }.filter { q -> stepSizes.none { it.step == q } }

    val frenchWanted: Boolean get() = "fr" in languages

    val traitChoices: List<String> get() = SettingsPickRuleChoices.traitsFor(kinds)

    val neverChoices: List<String> get() = traitChoices + SettingsPickRuleChoices.safetyTraits
}

data class SettingsPickRuleSynced(
    val name: String,
    val app: String,
    val profileId: String,
    val at: String,
    val from: String,
)

private fun JsonObject.str(key: String): String =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

internal fun whole(text: String): Long = text.trim().toLongOrNull() ?: 0

internal fun gbOf(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

internal fun gbPerHour(bytesPerMinute: Long): Double = ((bytesPerMinute * 60.0 / BYTES_PER_GB) * 100).roundToLong() / 100.0

internal fun bytesPerMinute(gbPerHour: Double): Long = (gbPerHour * BYTES_PER_GB / 60).roundToLong()

internal fun formatGb(value: Double): String {
    val cents = (value * 100).roundToLong()
    val whole = cents / 100
    val fraction = kotlin.math.abs(cents % 100)
    return when {
        fraction == 0L -> "$whole"
        fraction % 10 == 0L -> "$whole.${fraction / 10}"
        else -> "$whole.${fraction.toString().padStart(2, '0')}"
    }
}

private fun sameGb(
    a: String,
    b: String,
): Boolean = gbOf(a) == gbOf(b)

internal fun starFields(limits: Map<String, SizeEntry>): Pair<String, String> {
    val l = limits["*"]
    if (l == null || !l.perMinute) return "" to ""
    return formatGb(gbPerHour(l.min)) to if (l.max == 0L || l.max >= SIZE_OPEN) "" else formatGb(gbPerHour(l.max))
}

internal fun stepSizeRows(
    limits: Map<String, SizeEntry>,
    ladder: List<String>,
): List<SettingsPickRuleStepSize> {
    fun place(step: String): Int = ladder.indexOf(step).let { if (it < 0) ladder.size else it }
    return limits.keys
        .filter { it != "*" && limits.getValue(it).perMinute }
        .sortedBy { place(it) }
        .map { step ->
            val l = limits.getValue(step)
            SettingsPickRuleStepSize(
                step,
                formatGb(gbPerHour(l.min)),
                if (l.preferred > 0) formatGb(gbPerHour(l.preferred)) else "",
                if (l.max == 0L || l.max >= SIZE_OPEN) "" else formatGb(gbPerHour(l.max)),
            )
        }
}

internal fun applySizes(
    form: SettingsPickRuleForm,
    start: PickBody,
): Map<String, SizeEntry> {
    val limits = LinkedHashMap(form.loaded.sizeLimits)
    val startStar = starFields(start.sizeLimits)
    if (!(sameGb(form.sizeMin, startStar.first) && sameGb(form.sizeMax, startStar.second))) {
        if (gbOf(form.sizeMin) == null && gbOf(form.sizeMax) == null) {
            limits.remove("*")
        } else {
            val extra = limits["*"]?.extra ?: EMPTY
            limits["*"] =
                SizeEntry(
                    gbOf(form.sizeMin)?.let { bytesPerMinute(it) } ?: 0,
                    gbOf(form.sizeMax)?.let { bytesPerMinute(it) } ?: SIZE_OPEN,
                    0,
                    true,
                    extra,
                )
        }
    }
    val startRows = stepSizeRows(start.sizeLimits, start.steps.map { it.quality })
    startRows.filter { s -> form.stepSizes.none { it.step == s.step } }.forEach { limits.remove(it.step) }
    for (row in form.stepSizes) {
        val was = startRows.firstOrNull { it.step == row.step }
        if (was != null && sameGb(was.min, row.min) && sameGb(was.preferred, row.preferred) && sameGb(was.max, row.max)) continue
        val extra = limits[row.step]?.extra ?: EMPTY
        val preferred = gbOf(row.preferred)?.takeIf { it > 0 }?.let { bytesPerMinute(it) } ?: 0
        limits[row.step] =
            SizeEntry(
                gbOf(row.min)?.let { bytesPerMinute(it) } ?: 0,
                gbOf(row.max)?.let { bytesPerMinute(it) } ?: SIZE_OPEN,
                preferred,
                true,
                extra,
            )
    }
    return limits
}

internal fun PickBody.toForm(
    name: String,
    kinds: List<String>,
    base: JsonObject = toJson(),
): SettingsPickRuleForm {
    val star = starFields(sizeLimits)
    return SettingsPickRuleForm(
        name = name,
        kinds = kinds,
        steps = steps.map { SettingsPickRuleStep(it.quality, it.allowed) },
        goodEnoughAt = goodEnoughAt,
        goodEnoughPoints = goodEnoughPoints?.toString().orEmpty(),
        traits = traits.map { SettingsPickRuleTrait(it.trait, it.points.toString()) },
        mustHave = mustHave,
        never = never,
        blockedGroups = blockedGroups,
        devices = devices,
        order = order,
        upgradeMinPoints = upgradeMinPoints.toString(),
        minPoints = minPoints.toString(),
        properMode = properMode,
        properAge = if (properAge == 0L) "" else properAge.toString(),
        sizeMin = star.first,
        sizeMax = star.second,
        stepSizes = stepSizeRows(sizeLimits, steps.map { it.quality }),
        replaceUnderMin = replaceUnderMin,
        prefer = prefer,
        waitTorrent = waitTorrent.toString(),
        waitUsenet = waitUsenet.toString(),
        skipWait = skipWait,
        tags = tags,
        languages = languages,
        frenchVersions = frenchVersions,
        originalOk = originalOk,
        languageWait = if (languageWait == 0L) "" else languageWait.toString(),
        base = base,
    )
}

internal fun SettingsPickRuleForm.toBody(): PickBody {
    val base = loaded
    return base.copy(
        steps = steps.map { s -> StepEntry(s.quality, s.allowed, base.steps.firstOrNull { it.quality == s.quality }?.extra ?: EMPTY) },
        goodEnoughAt = goodEnoughAt,
        goodEnoughPoints = goodEnoughPoints.trim().takeIf { it.isNotEmpty() }?.let { whole(it) },
        traits = traits.map { t -> TraitEntry(t.trait, whole(t.points), base.traits.firstOrNull { it.trait == t.trait }?.extra ?: EMPTY) },
        mustHave = mustHave,
        never = never,
        blockedGroups = blockedGroups,
        minPoints = whole(minPoints),
        sizeLimits = applySizes(this, base),
        prefer = prefer,
        waitTorrent = whole(waitTorrent),
        waitUsenet = whole(waitUsenet),
        skipWait = skipWait,
        languages = languages,
        originalOk = originalOk,
        languageWait = whole(languageWait),
        frenchVersions = if (frenchWanted) frenchVersions else emptyList(),
        order = order,
        upgradeMinPoints = whole(upgradeMinPoints),
        replaceUnderMin = replaceUnderMin,
        properMode = properMode,
        properAge = whole(properAge),
        tags = tags,
        devices = devices,
    )
}

internal fun blankForm(kind: String): SettingsPickRuleForm {
    val kinds = if (kind == "music") listOf("music") else listOf("movie", "show")
    return PickBody.blank(kind).toForm("", kinds)
}

internal fun toggleKind(
    kinds: List<String>,
    kind: String,
    on: Boolean,
): List<String> {
    if (!on) return kinds.filter { it != kind }
    if (kind == "music") return listOf("music")
    val next = kinds.filter { it != "music" }.toSet() + kind
    return SettingsPickRuleChoices.kinds.filter { it in next }
}

internal fun moveStep(
    steps: List<SettingsPickRuleStep>,
    quality: String,
    toBest: Boolean,
): List<SettingsPickRuleStep> {
    val i = steps.indexOfFirst { it.quality == quality }
    val j = if (toBest) i + 1 else i - 1
    if (i < 0 || j < 0 || j >= steps.size) return steps
    return steps.toMutableList().also {
        val a = it[i]
        it[i] = it[j]
        it[j] = a
    }
}

internal fun settingsPickRuleCopyName(name: String): String {
    val tail = " copy"
    return name.take(200 - tail.length) + tail
}

internal fun addBlockedGroup(
    list: List<String>,
    raw: String,
): List<String> {
    val g = raw.trim()
    if (g.isEmpty() || list.any { it.equals(g, ignoreCase = true) }) return list
    return list + g
}

private const val MAX_TAG = 40
private const val MAX_TAGS = 20

internal fun cleanTag(raw: String): String =
    raw
        .lowercase()
        .trim()
        .split(Regex("\\s+"))
        .joinToString("-")
        .trim('-', ',')
        .take(MAX_TAG)

internal fun addTags(
    list: List<String>,
    raw: String,
): List<String> {
    val out = list.toMutableList()
    for (part in raw.split(',')) {
        val t = cleanTag(part)
        if (t.isNotEmpty() && t !in out && out.size < MAX_TAGS) out += t
    }
    return out
}

private val NESTED =
    mapOf(
        "protocol.prefer" to "prefer",
        "protocol.wait_minutes_torrent" to "wait_minutes_torrent",
        "protocol.wait_minutes_usenet" to "wait_minutes_usenet",
        "protocol.skip_wait_at_good_enough" to "prefer",
        "proper_policy.max_age_days" to "max_age_days",
        "language.wait_days" to "wait_days",
        "language.french_versions" to "language",
    )

internal val SETTINGS_PICK_RULE_FIELDS: Set<String> =
    setOf(
        "name", "kinds", "steps", "good_enough_at", "good_enough_points", "traits", "must_have", "never", "size_limits", "prefer",
        "wait_minutes_torrent", "wait_days", "wait_minutes_usenet", "language", "min_points", "upgrade_min_points", "order",
        "proper_policy", "max_age_days",
    )

internal fun settingsPickRuleFieldKey(field: String): String {
    val parts =
        field
            .removePrefix("body.")
            .replace(Regex("\\[[^\\]]*]"), "")
            .split('.')
            .filter { it.isNotEmpty() }
    if (parts.isEmpty()) return ""
    return NESTED["${parts[0]}.${parts.getOrNull(1).orEmpty()}"] ?: parts[0]
}
