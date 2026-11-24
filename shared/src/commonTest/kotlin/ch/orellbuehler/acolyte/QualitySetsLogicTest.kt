package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.feature.quality.PickBody
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleChoices
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleForm
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleStepSize
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleTrait
import ch.orellbuehler.acolyte.feature.quality.addBlockedGroup
import ch.orellbuehler.acolyte.feature.quality.addTags
import ch.orellbuehler.acolyte.feature.quality.blankForm
import ch.orellbuehler.acolyte.feature.quality.bytesPerMinute
import ch.orellbuehler.acolyte.feature.quality.checkPickRuleBody
import ch.orellbuehler.acolyte.feature.quality.formatGb
import ch.orellbuehler.acolyte.feature.quality.gbPerHour
import ch.orellbuehler.acolyte.feature.quality.moveStep
import ch.orellbuehler.acolyte.feature.quality.settingsPickRuleCopyName
import ch.orellbuehler.acolyte.feature.quality.settingsPickRuleFieldKey
import ch.orellbuehler.acolyte.feature.quality.toBody
import ch.orellbuehler.acolyte.feature.quality.toForm
import ch.orellbuehler.acolyte.feature.quality.toggleKind
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal const val SERVER_BODY =
    """{"steps":[{"quality":"720p_web","allowed":true},{"quality":"1080p_web","allowed":true},{"quality":"1080p_bluray","allowed":false}],
        "good_enough_at":"1080p_web","traits":[{"trait":"hevc","points":10},{"trait":"own:3","points":-5}],"must_have":[],"never":["sample"],
        "min_points":0,"size_limits":{"*":{"min":8947848,"max":107374182,"per_minute":true},
        "1080p_web":{"min":1000,"max":2000,"preferred":1500,"per_minute":true},"720p_web":{"min":5,"max":6,"per_minute":false}},
        "protocol":{"prefer":"either","wait_minutes_torrent":0,"wait_minutes_usenet":0,"skip_wait_at_good_enough":false,"later":1},
        "language":{"wanted":["en"],"original_ok":true},"order":"quality_then_points","upgrade_min_points":10,
        "proper_policy":{"mode":"take_any"},
        "equal_steps":[["1080p_web","1080p_bluray"]],
        "synced":{"from":"trash","app":"radarr","profile_id":"p1","name":"HD","at":"2026-01-02T03:04:05Z"},"future_key":{"a":1}}"""

private fun parse(text: String): JsonObject = ApiJson.lenient.parseToJsonElement(text).jsonObject

private fun formOf(
    text: String = SERVER_BODY,
    kinds: List<String> = listOf("movie"),
): SettingsPickRuleForm {
    val json = parse(text)
    return PickBody.from(json).toForm("Mine", kinds, json)
}

class QualitySetsLogicTest {
    @Test
    fun blankBodyMatchesTheWebDefaults() {
        val json = PickBody.blank("movie").toJson()
        assertEquals(14, json["steps"]!!.jsonArray.size)
        val allowed = json["steps"]!!.jsonArray.filter { it.jsonObject["allowed"]!!.jsonPrimitive.content == "true" }
        assertEquals(
            listOf("720p_tv", "720p_web", "720p_bluray", "1080p_tv", "1080p_web", "1080p_bluray"),
            allowed.map { it.jsonObject["quality"]!!.jsonPrimitive.content },
        )
        assertEquals("1080p_web", json["good_enough_at"]!!.jsonPrimitive.content)
        assertEquals(listOf("sample", "executable", "password_protected", "disc_image"), json["never"]!!.jsonArray.map { it.jsonPrimitive.content })
        val star = json["size_limits"]!!.jsonObject["*"]!!.jsonObject
        assertEquals("8947848", star["min"]!!.jsonPrimitive.content)
        assertEquals("107374182", star["max"]!!.jsonPrimitive.content)
        assertEquals("10", json["upgrade_min_points"]!!.jsonPrimitive.content)
        assertEquals("take_any", json["proper_policy"]!!.jsonObject["mode"]!!.jsonPrimitive.content)
        assertEquals("true", json["language"]!!.jsonObject["original_ok"]!!.jsonPrimitive.content)
        val music = PickBody.blank("music").toJson()
        assertEquals(5, music["steps"]!!.jsonArray.size)
        assertEquals("lossless", music["good_enough_at"]!!.jsonPrimitive.content)
        assertTrue(music["size_limits"]!!.jsonObject.isEmpty())
    }

    @Test
    fun aLoadedBodyRoundTripsWithUnknownKeys() {
        val json = parse(SERVER_BODY)
        val back = formOf().toBody().toJson()
        assertEquals(json, back)
        assertEquals(json["equal_steps"], back["equal_steps"])
        assertEquals(json["synced"], back["synced"])
        assertEquals(json["future_key"], back["future_key"])
        assertEquals(json["protocol"], back["protocol"])
    }

    @Test
    fun untouchedSizesKeepTheirExactBytesAndEditsConvert() {
        var form = formOf()
        assertEquals("0.5", form.sizeMin)
        assertEquals("6", form.sizeMax)
        assertEquals(1, form.stepSizes.size)
        val star = form.toBody().sizeLimits.getValue("*")
        assertEquals(8947848L, star.min)
        assertEquals(107374182L, star.max)
        form = form.copy(sizeMin = "0.50", sizeMax = "6")
        assertEquals(8947848L, form.toBody().sizeLimits.getValue("*").min)
        form = form.copy(sizeMax = "8")
        assertEquals(bytesPerMinute(8.0), form.toBody().sizeLimits.getValue("*").max)
        form = form.copy(sizeMin = "", sizeMax = "")
        assertNull(form.toBody().sizeLimits["*"])
        form = form.copy(sizeMin = "1", sizeMax = "")
        val open = form.toBody().sizeLimits.getValue("*")
        assertEquals(1L shl 50, open.max)
        assertEquals(bytesPerMinute(1.0), open.min)
        assertTrue(open.perMinute)
    }

    @Test
    fun stepSizeRowsConvertAndKeepOthers() {
        var form = formOf()
        val row = form.stepSizes.single()
        assertEquals("1080p_web", row.step)
        assertEquals("0", row.min)
        form = form.copy(stepSizes = form.stepSizes.map { it.copy(preferred = "2", max = "3") })
        val sizes = form.toBody().sizeLimits
        assertEquals(bytesPerMinute(2.0), sizes.getValue("1080p_web").preferred)
        assertEquals(bytesPerMinute(3.0), sizes.getValue("1080p_web").max)
        assertTrue(sizes.containsKey("720p_web"))
        form = form.copy(stepSizes = emptyList())
        assertNull(form.toBody().sizeLimits["1080p_web"])
        assertTrue(form.toBody().sizeLimits.containsKey("720p_web"))
        form = form.copy(stepSizes = listOf(SettingsPickRuleStepSize("1080p_web", "1", "", "")))
        val added = form.toBody().sizeLimits.getValue("1080p_web")
        assertEquals(1L shl 50, added.max)
        assertEquals(0L, added.preferred)
    }

    @Test
    fun conversionsAndNumbers() {
        assertEquals(0.5, gbPerHour(8947848))
        assertEquals("0.5", formatGb(0.5))
        assertEquals("6", formatGb(6.0))
        assertEquals("2.25", formatGb(2.25))
        assertEquals("1.1", formatGb(1.1))
        assertEquals(107374182L, bytesPerMinute(6.0))
    }

    @Test
    fun tidyDropsEmptyOptionals() {
        val json = parse(SERVER_BODY)
        var form =
            formOf().copy(
                goodEnoughPoints = "",
                tags = emptyList(),
                blockedGroups = emptyList(),
                devices = emptyList(),
                languageWait = "0",
                properAge = "0",
                frenchVersions = listOf("VFF"),
            )
        var out = form.toBody().toJson()
        assertTrue("good_enough_points" !in out)
        assertTrue("tags" !in out)
        assertTrue("blocked_groups" !in out)
        assertTrue("devices" !in out)
        assertTrue("wait_days" !in out["language"]!!.jsonObject)
        assertTrue("french_versions" !in out["language"]!!.jsonObject)
        assertTrue("max_age_days" !in out["proper_policy"]!!.jsonObject)
        form =
            form.copy(
                goodEnoughPoints = "12",
                languages = listOf("en", "fr"),
                frenchVersions = listOf("VFF"),
                languageWait = "7",
                properAge = "30",
                tags = listOf("hd"),
                traits = listOf(SettingsPickRuleTrait("hevc", ""), SettingsPickRuleTrait("av1", "-")),
                upgradeMinPoints = "",
            )
        out = form.toBody().toJson()
        assertEquals("12", out["good_enough_points"]!!.jsonPrimitive.content)
        assertEquals(listOf("VFF"), out["language"]!!.jsonObject["french_versions"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("7", out["language"]!!.jsonObject["wait_days"]!!.jsonPrimitive.content)
        assertEquals("30", out["proper_policy"]!!.jsonObject["max_age_days"]!!.jsonPrimitive.content)
        assertEquals(listOf("0", "0"), out["traits"]!!.jsonArray.map { it.jsonObject["points"]!!.jsonPrimitive.content })
        assertEquals("0", out["upgrade_min_points"]!!.jsonPrimitive.content)
        assertEquals(json["equal_steps"], out["equal_steps"])
    }

    @Test
    fun stepOrderAndKinds() {
        val steps = formOf().steps
        val up = moveStep(steps, "720p_web", true)
        assertEquals(listOf("1080p_web", "720p_web", "1080p_bluray"), up.map { it.quality })
        assertEquals(steps, moveStep(steps, "1080p_bluray", true))
        assertEquals(steps, moveStep(steps, "720p_web", false))
        assertEquals(listOf("1080p_bluray", "1080p_web", "720p_web"), formOf().stepsBestFirst.map { it.quality })
        assertEquals(listOf("1080p_web", "720p_web"), formOf().allowedBestFirst.map { it.quality })
        assertEquals(listOf("music"), toggleKind(listOf("movie", "show"), "music", true))
        assertEquals(listOf("movie", "show"), toggleKind(listOf("music"), "show", true).let { toggleKind(it, "movie", true) })
        assertEquals(listOf("show"), toggleKind(listOf("movie", "show"), "movie", false))
        assertEquals(emptyList(), toggleKind(listOf("movie"), "movie", false))
    }

    @Test
    fun checksMirrorTheServerRules() {
        val ok = formOf().toBody()
        val own = setOf("own:3")
        assertTrue(checkPickRuleBody("Mine", listOf("movie"), ok, own).isEmpty())
        assertEquals("rules_name", checkPickRuleBody("  ", listOf("movie"), ok, own)["name"]!!.code)
        assertEquals("name_200", checkPickRuleBody("x".repeat(201), listOf("movie"), ok, own)["name"]!!.code)
        assertEquals("kinds", checkPickRuleBody("a", emptyList(), ok, own)["kinds"]!!.code)
        assertEquals("music_alone", checkPickRuleBody("a", listOf("music", "movie"), ok, own)["kinds"]!!.code)
        assertEquals("steps_other", checkPickRuleBody("a", listOf("music"), ok, own)["steps"]!!.code)
        assertEquals("own_removed", checkPickRuleBody("a", listOf("movie"), ok, emptySet())["traits"]!!.code)
        val none = ok.copy(steps = ok.steps.map { it.copy(allowed = false) })
        assertEquals("step_on", checkPickRuleBody("a", listOf("movie"), none, own)["steps"]!!.code)
        assertNull(checkPickRuleBody("a", listOf("movie"), none, own)["good_enough_at"])
        assertEquals("good_enough", checkPickRuleBody("a", listOf("movie"), ok.copy(goodEnoughAt = "1080p_bluray"), own)["good_enough_at"]!!.code)
        assertEquals("trait_once", checkPickRuleBody("a", listOf("movie"), ok.copy(mustHave = listOf("hevc", "hevc")), own)["must_have"]!!.code)
        val must = checkPickRuleBody("a", listOf("movie"), ok.copy(mustHave = listOf("sample")), own)["must_have"]!!
        assertEquals("cannot_be_must", must.code)
        assertEquals("sample", must.arg)
        assertEquals("not_trait", checkPickRuleBody("a", listOf("movie"), ok.copy(never = listOf("flac")), own)["never"]!!.code)
        assertEquals("points", checkPickRuleBody("a", listOf("movie"), ok.copy(minPoints = 2147483648), own)["min_points"]!!.code)
        assertEquals("wait", checkPickRuleBody("a", listOf("movie"), ok.copy(waitTorrent = 10081), own)["wait_minutes_torrent"]!!.code)
        assertEquals("lang_unknown", checkPickRuleBody("a", listOf("movie"), ok.copy(languages = listOf("bg")), own)["language"]!!.code)
        assertEquals("lang_once", checkPickRuleBody("a", listOf("movie"), ok.copy(languages = listOf("en", "en")), own)["language"]!!.code)
        assertEquals("lang_wait", checkPickRuleBody("a", listOf("movie"), ok.copy(languageWait = 91), own)["wait_days"]!!.code)
        assertEquals("days", checkPickRuleBody("a", listOf("movie"), ok.copy(properAge = 36501), own)["max_age_days"]!!.code)
        val over = formOf().copy(sizeMin = "9", sizeMax = "2").toBody()
        assertEquals("size_order", checkPickRuleBody("a", listOf("movie"), over, own)["size_limits"]!!.code)
        val step = formOf().copy(stepSizes = listOf(SettingsPickRuleStepSize("2160p_web", "1", "", "2"))).toBody()
        assertEquals("size_step", checkPickRuleBody("a", listOf("movie"), step, own)["size_limits"]!!.code)
        val pref = formOf().copy(stepSizes = listOf(SettingsPickRuleStepSize("1080p_web", "1", "5", "2"))).toBody()
        assertEquals("size_preferred", checkPickRuleBody("a", listOf("movie"), pref, own)["size_limits"]!!.code)
        assertEquals(26, SettingsPickRuleChoices.languages.size)
        assertTrue("bg" !in SettingsPickRuleChoices.languages)
    }

    @Test
    fun helpersTidyTextAndKeys() {
        assertEquals(listOf("a"), addBlockedGroup(listOf("a"), " A "))
        assertEquals(listOf("a", "B"), addBlockedGroup(listOf("a"), " B "))
        assertEquals(listOf("hd-tv", "x"), addTags(emptyList(), " HD  TV, x ,,hd-tv"))
        assertEquals(200, settingsPickRuleCopyName("x".repeat(300)).length)
        assertTrue(settingsPickRuleCopyName("Standard").endsWith("Standard copy"))
        assertEquals("steps", settingsPickRuleFieldKey("body.steps[2].quality"))
        assertEquals("prefer", settingsPickRuleFieldKey("body.protocol.prefer"))
        assertEquals("wait_days", settingsPickRuleFieldKey("body.language.wait_days"))
        assertEquals("language", settingsPickRuleFieldKey("body.language.wanted"))
        assertEquals("proper_policy", settingsPickRuleFieldKey("body.proper_policy.mode"))
        assertEquals("max_age_days", settingsPickRuleFieldKey("body.proper_policy.max_age_days"))
        assertEquals("name", settingsPickRuleFieldKey("name"))
        assertEquals("", settingsPickRuleFieldKey(""))
        val blank = blankForm("music")
        assertTrue(blank.music)
        assertEquals(listOf("music"), blank.kinds)
    }
}
