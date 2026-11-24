package ch.orellbuehler.acolyte.ui.settingsb

import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleChoices
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QualityWordsTest {
    private fun string(name: String): Int = R.string::class.java.getField(name).getInt(null)

    private fun plural(name: String): Int = R.plurals::class.java.getField(name).getInt(null)

    private fun check(
        ids: List<String>,
        prefix: String,
        lookup: (String) -> Int?,
        suffix: String = "",
    ) {
        for (id in ids) {
            val res = lookup(id)
            assertNotNull(res, "$prefix$id")
            assertEquals(string("$prefix$id$suffix"), res, "$prefix$id$suffix")
        }
        assertNull(lookup("unknown_value"))
    }

    @Test
    fun everyStepResolves() {
        check(SettingsPickRuleChoices.videoLadder + SettingsPickRuleChoices.musicLadder, "acq_q_step_", ::qualityStepRes)
    }

    @Test
    fun everyTraitResolves() {
        val all = SettingsPickRuleChoices.videoTraits + SettingsPickRuleChoices.musicTraits + SettingsPickRuleChoices.safetyTraits
        check(all.distinct(), "acq_q_trait_", ::qualityTraitRes)
    }

    @Test
    fun everyLanguageResolves() {
        assertEquals(26, SettingsPickRuleChoices.languages.size)
        check(SettingsPickRuleChoices.languages, "acq_q_lang_", ::qualityLanguageRes)
    }

    @Test
    fun everyDeviceAndKindResolves() {
        check(SettingsPickRuleChoices.devices, "acq_q_dev_", ::qualityDeviceRes)
        check(SettingsPickRuleChoices.kinds, "acq_q_kind_", ::qualityKindRes)
    }

    @Test
    fun ordersPropersAndPrefersResolve() {
        check(SettingsPickRuleChoices.orders, "acq_q_order_", ::qualityOrderRes)
        check(SettingsPickRuleChoices.orders, "acq_q_order_", ::qualityOrderDescRes, "_desc")
        check(SettingsPickRuleChoices.properModes, "acq_q_proper_", ::qualityProperRes)
        check(SettingsPickRuleChoices.properModes, "acq_q_proper_", ::qualityProperDescRes, "_desc")
        check(SettingsPickRuleChoices.prefers, "acq_q_prefer_", ::qualityPreferRes)
    }

    @Test
    fun verdictsMatchesAndSpecKindsResolve() {
        check(
            listOf("would_choose", "would_hold", "refused", "passed_over", "turned_down", "judged"),
            "acq_q_verdict_",
            ::qualityVerdictRes,
        )
        check(listOf("name", "group", "size"), "acq_q_ot_match_", ::qualityMatchRes)
        check(listOf("name", "group", "size"), "acq_q_ot_hint_", ::qualityMatchHintRes)
        check(
            listOf("name", "group", "source", "resolution", "size", "edition", "language", "release_type", "year"),
            "acq_q_ot_kind_",
            ::qualitySpecKindRes,
        )
    }

    @Test
    fun problemCodesResolve() {
        val codes =
            listOf(
                "rules_name",
                "name_200",
                "name_60",
                "kinds",
                "kind_once",
                "music_alone",
                "steps_other",
                "step_once",
                "step_on",
                "good_enough",
                "trait_once",
                "own_removed",
                "not_trait",
                "points",
                "size_step",
                "size_neg",
                "size_order",
                "size_big",
                "size_preferred",
                "wait",
                "lang_unknown",
                "lang_once",
                "lang_wait",
                "days",
                "trait_name",
                "terms_min",
                "terms_50",
                "term_200",
                "size_range",
                "cannot_be_must",
            )
        check(codes, "acq_q_err_", ::qualityProblemRes)
    }

    @Test
    fun useKeysResolveToPlurals() {
        for (key in listOf("versions", "library_folders", "outside_lists", "wishlist", "title_defaults")) {
            assertEquals(plural("acq_q_use_$key"), qualityUsePluralRes(key), key)
        }
        assertNull(qualityUsePluralRes("other"))
    }

    @Test
    fun trashAppWords() {
        assertEquals(R.string.acq_q_e_trash_shows, qualityTrashAppRes("sonarr"))
        assertEquals(R.string.acq_q_e_trash_movies, qualityTrashAppRes("radarr"))
    }

    @Test
    fun joinsUseWords() {
        val and = "%1\$s and %2\$s"
        assertEquals("", qualityJoinWords(emptyList(), and))
        assertEquals("2 Versions", qualityJoinWords(listOf("2 Versions"), and))
        assertEquals("2 Versions and 1 Outside list", qualityJoinWords(listOf("2 Versions", "1 Outside list"), and))
        assertEquals("a, b and c", qualityJoinWords(listOf("a", "b", "c"), and))
    }

    @Test
    fun formatsServerTimes() {
        val text = qualityLocalTime("2026-03-05T14:30:00Z", ZoneId.of("UTC"), Locale.US)
        assertEquals(true, text.contains("2026") && text.contains("Mar"), text)
        assertEquals("not a time", qualityLocalTime("not a time"))
    }

    @Test
    fun problemFieldsFindTheirCard() {
        assertEquals("name", qualityCardFor("kinds"))
        assertEquals("steps", qualityCardFor("good_enough_at"))
        assertEquals("traits", qualityCardFor("proper_policy"))
        assertEquals("must", qualityCardFor("never"))
        assertEquals("size", qualityCardFor("size_limits"))
        assertEquals("waiting", qualityCardFor("wait_minutes_usenet"))
        assertEquals("language", qualityCardFor("wait_days"))
        assertEquals("", qualityCardFor(""))
    }
}
