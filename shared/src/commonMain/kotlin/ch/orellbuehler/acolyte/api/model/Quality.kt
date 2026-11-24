package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class PickRuleUse(
    val versions: Int = 0,
    @SerialName("library_folders") val libraryFolders: Int = 0,
    @SerialName("outside_lists") val outsideLists: Int = 0,
    val wishlist: Int = 0,
    @SerialName("title_defaults") val titleDefaults: Int = 0,
)

@Serializable
internal data class PickRuleDetail(
    val id: Long = 0,
    val name: String = "",
    val kinds: List<String> = emptyList(),
    val preset: Boolean = false,
    val revision: Int = 0,
    val body: JsonObject = JsonObject(emptyMap()),
    @SerialName("in_use") val inUse: PickRuleUse = PickRuleUse(),
)

@Serializable
internal data class PickRuleInput(
    val name: String,
    val kinds: List<String>,
    val body: JsonObject,
)

@Serializable
internal data class OwnTraitSpec(
    val name: String = "",
    val kind: String = "",
    val value: String = "",
    val negate: Boolean = false,
    val required: Boolean = false,
)

@Serializable
internal data class OwnTrait(
    val id: Long = 0,
    val key: String = "",
    val name: String = "",
    val match: String = "name",
    val terms: List<String> = emptyList(),
    val source: String = "",
    val specs: List<OwnTraitSpec> = emptyList(),
    val app: String = "",
)

@Serializable
internal data class OwnTraitList(
    @SerialName("own_traits") val ownTraits: List<OwnTrait> = emptyList(),
)

@Serializable
internal data class OwnTraitInput(
    val name: String,
    val match: String,
    val terms: List<String>,
)

@Serializable
internal data class OwnTraitRemoved(
    @SerialName("pick_rules_changed") val pickRulesChanged: Int = 0,
)

@Serializable
internal data class TrashProfile(
    val id: String = "",
    val app: String = "",
    val name: String = "",
    val description: String = "",
    val url: String = "",
    @SerialName("min_points") val minPoints: Int = 0,
    @SerialName("good_enough_points") val goodEnoughPoints: Int = 0,
    @SerialName("upgrade_min_points") val upgradeMinPoints: Int = 0,
)

@Serializable
internal data class TrashProfiles(
    val profiles: List<TrashProfile> = emptyList(),
)

@Serializable
internal data class TrashApply(
    val app: String,
    @SerialName("profile_id") val profileId: String,
)

@Serializable
internal data class TrashScored(
    val key: String = "",
    val name: String = "",
    val points: Int = 0,
)

@Serializable
internal data class TrashSkipped(
    val name: String = "",
    val kinds: List<String> = emptyList(),
    val reason: String = "",
)

@Serializable
internal data class TrashApplied(
    @SerialName("pick_rules") val pickRules: PickRuleDetail = PickRuleDetail(),
    val added: Int = 0,
    val updated: Int = 0,
    val scored: List<TrashScored> = emptyList(),
    val skipped: List<TrashSkipped> = emptyList(),
)

@Serializable
internal data class NameCheckBody(
    val name: String,
    val size: Long = 0,
    @SerialName("pick_rules_id") val pickRulesId: Long = 0,
)

@Serializable
internal data class NameCheckText(
    val value: String = "",
)

@Serializable
internal data class NameCheckNumber(
    val value: Int = 0,
)

@Serializable
internal data class NameCheckShow(
    val seasons: List<Int> = emptyList(),
    val episodes: List<Int> = emptyList(),
    @SerialName("air_date") val airDate: String = "",
    val absolute: List<Int> = emptyList(),
    val special: Boolean = false,
)

@Serializable
internal data class NameCheckParsed(
    val title: NameCheckText? = null,
    val year: NameCheckNumber? = null,
    val group: NameCheckText? = null,
    val edition: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
    val show: NameCheckShow? = null,
)

@Serializable
internal data class NameCheckTitle(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    val year: Int = 0,
    val sure: Boolean = false,
    val detail: String = "",
)

@Serializable
internal data class NameCheckCopy(
    val version: String = "",
    @SerialName("pick_rules") val pickRules: String = "",
    val episodes: String = "",
    val album: String = "",
    val outcome: String = "",
    val passed: Boolean = false,
    val points: Int = 0,
    val step: String = "",
    @SerialName("good_enough") val goodEnough: Boolean = false,
    @SerialName("has_file") val hasFile: Boolean = false,
    val reasons: List<ReleaseReason> = emptyList(),
)

@Serializable
internal data class NameCheckAnswer(
    val name: String = "",
    val parsed: NameCheckParsed = NameCheckParsed(),
    val read: ReleaseRead? = null,
    val title: NameCheckTitle? = null,
    val copies: List<NameCheckCopy> = emptyList(),
    val note: String = "",
)
