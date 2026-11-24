package ch.orellbuehler.acolyte.feature.quality

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.model.PickRuleUse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class SettingsPickRuleUsePart(
    val key: String,
    val count: Int,
)

data class SettingsPickRuleUse(
    val versions: Int = 0,
    val libraryFolders: Int = 0,
    val outsideLists: Int = 0,
    val wishlist: Int = 0,
    val titleDefaults: Int = 0,
) {
    val total: Int get() = versions + libraryFolders + outsideLists + wishlist + titleDefaults

    val parts: List<SettingsPickRuleUsePart>
        get() =
            listOf(
                SettingsPickRuleUsePart("versions", versions),
                SettingsPickRuleUsePart("library_folders", libraryFolders),
                SettingsPickRuleUsePart("outside_lists", outsideLists),
                SettingsPickRuleUsePart("wishlist", wishlist),
                SettingsPickRuleUsePart("title_defaults", titleDefaults),
            ).filter { it.count > 0 }
}

internal fun PickRuleUse.toPublic(): SettingsPickRuleUse =
    SettingsPickRuleUse(versions, libraryFolders, outsideLists, wishlist, titleDefaults)

data class SettingsQualityNotice(
    val seq: Int,
    val kind: String,
    val name: String = "",
    val count: Int = 0,
)

internal class QualityNotices {
    private var seq = 0

    fun make(
        kind: String,
        name: String = "",
        count: Int = 0,
    ): SettingsQualityNotice = SettingsQualityNotice(++seq, kind, name, count)
}

private fun ApiError.bodyObject(): JsonObject? = runCatching { ApiJson.lenient.parseToJsonElement(body) as? JsonObject }.getOrNull()

internal fun ApiError.fieldName(): String = (bodyObject()?.get("field") as? JsonPrimitive)?.contentOrNull.orEmpty()

internal fun ApiError.serverText(): String =
    (bodyObject()?.get("error") as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: message

internal fun ApiError.inUse(): SettingsPickRuleUse? {
    val element = bodyObject()?.get("in_use") as? JsonObject ?: return null
    return runCatching { ApiJson.lenient.decodeFromJsonElement(PickRuleUse.serializer(), element).toPublic() }.getOrNull()
}
