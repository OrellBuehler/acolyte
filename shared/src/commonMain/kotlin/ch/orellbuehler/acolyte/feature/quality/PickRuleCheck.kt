package ch.orellbuehler.acolyte.feature.quality

internal fun isOwnTrait(trait: String): Boolean = trait.startsWith(OWN_PREFIX)

private fun inPoints(n: Long): Boolean = n in -MAX_POINTS..MAX_POINTS

private fun inWait(n: Long): Boolean = n in 0..10080

private fun trimmedLength(text: String): Int = text.trim().codePointCount()

private fun String.codePointCount(): Int {
    var count = 0
    var i = 0
    while (i < length) {
        i += if (this[i].isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) 2 else 1
        count++
    }
    return count
}

internal fun checkPickRuleBody(
    name: String,
    kinds: List<String>,
    body: PickBody,
    ownKeys: Set<String>,
): Map<String, SettingsPickRuleProblem> {
    val err = LinkedHashMap<String, SettingsPickRuleProblem>()

    fun fail(
        field: String,
        code: String,
        arg: String = "",
    ) {
        if (field !in err) err[field] = SettingsPickRuleProblem(field, code, arg)
    }

    val n = name.trim()
    if (n.isEmpty()) {
        fail("name", "rules_name")
    } else if (trimmedLength(n) > 200) {
        fail("name", "name_200")
    }

    val choices = SettingsPickRuleChoices
    if (kinds.isEmpty() || kinds.size > 3) {
        fail("kinds", "kinds")
    } else if (kinds.toSet().size != kinds.size) {
        fail("kinds", "kind_once")
    } else if (kinds.any { it !in choices.kinds }) {
        fail("kinds", "kinds")
    } else if ("music" in kinds && kinds.size > 1) {
        fail("kinds", "music_alone")
    }

    val ladder = choices.ladderFor(kinds)
    val qualities = body.steps.map { it.quality }
    if (qualities.any { it !in ladder }) {
        fail("steps", "steps_other")
    } else if (qualities.toSet().size != qualities.size) {
        fail("steps", "step_once")
    } else if (body.steps.none { it.allowed }) {
        fail("steps", "step_on")
    }
    if ("steps" !in err && body.steps.none { it.allowed && it.quality == body.goodEnoughAt }) fail("good_enough_at", "good_enough")

    val own = choices.traitsFor(kinds).toSet() + ownKeys
    val names = body.traits.map { it.trait }
    if (names.toSet().size != names.size) {
        fail("traits", "trait_once")
    } else if (names.any { it !in own }) {
        val bad = names.first { it !in own }
        if (isOwnTrait(bad)) fail("traits", "own_removed") else fail("traits", "not_trait", bad)
    } else if (body.traits.any { !inPoints(it.points) }) {
        fail("traits", "points")
    }

    listProblem(body.mustHave, own, "cannot_be_must")?.let { fail("must_have", it.first, it.second) }
    listProblem(body.never, own + choices.safetyTraits, "cannot_be_never")?.let { fail("never", it.first, it.second) }

    if (!inPoints(body.minPoints)) fail("min_points", "points")
    if (!inPoints(body.upgradeMinPoints)) fail("upgrade_min_points", "points")
    body.goodEnoughPoints?.let { if (!inPoints(it)) fail("good_enough_points", "points") }

    for ((step, l) in body.sizeLimits) {
        when {
            step != "*" && step !in qualities -> fail("size_limits", "size_step")
            l.min < 0 || l.max < 0 -> fail("size_limits", "size_neg")
            l.max < l.min -> fail("size_limits", "size_order")
            l.max > SIZE_OPEN -> fail("size_limits", "size_big")
            l.preferred != 0L && (l.preferred < l.min || (l.max > 0 && l.preferred > l.max)) -> fail("size_limits", "size_preferred")
        }
        if ("size_limits" in err) break
    }

    if (!inWait(body.waitTorrent)) fail("wait_minutes_torrent", "wait")
    if (!inWait(body.waitUsenet)) fail("wait_minutes_usenet", "wait")

    val unknown = body.languages.firstOrNull { it !in choices.languages }
    if (unknown != null) {
        fail("language", "lang_unknown", unknown)
    } else if (body.languages.toSet().size != body.languages.size) {
        fail("language", "lang_once")
    }
    if (body.languageWait !in 0..90) fail("wait_days", "lang_wait")
    if (body.properAge !in 0..36500) fail("max_age_days", "days")
    return err
}

private fun listProblem(
    values: List<String>,
    allowed: Set<String>,
    cannot: String,
): Pair<String, String>? {
    if (values.toSet().size != values.size) return "trait_once" to ""
    val bad = values.firstOrNull { it !in allowed } ?: return null
    if (bad in SettingsPickRuleChoices.safetyTraits) return cannot to bad
    return if (isOwnTrait(bad)) "own_removed" to "" else "not_trait" to bad
}
