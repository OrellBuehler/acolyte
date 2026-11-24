package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.DownloaderInfo
import ch.orellbuehler.acolyte.api.model.DownloaderSettings
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderAttempts
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderBody
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderCategories
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderCategoryBody
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderTestAnswer
import ch.orellbuehler.acolyte.api.model.SettingsKeyLookupAnswer
import ch.orellbuehler.acolyte.api.model.SettingsKeyLookupBody
import kotlinx.serialization.builtins.nullable

internal data object DownloadersEndpoints : EndpointGroup() {
    private const val ONE = "/api/v1/downloaders/{id}"

    val add = post("/api/v1/downloaders", SettingsDownloaderBody.serializer(), DownloaderInfo.serializer(), Need.SETTINGS)
    val change = put(ONE, SettingsDownloaderBody.serializer(), DownloaderInfo.serializer(), Need.SETTINGS)
    val remove = delete(ONE, NoContent, Need.SETTINGS)
    val test = post("$ONE/test", SettingsDownloaderBody.serializer().nullable, SettingsDownloaderTestAnswer.serializer(), Need.SETTINGS)
    val pause = post("$ONE/pause", DownloaderInfo.serializer(), Need.SETTINGS)
    val resume = post("$ONE/resume", DownloaderInfo.serializer(), Need.SETTINGS)
    val categories = get("$ONE/categories", SettingsDownloaderCategories.serializer(), Need.SETTINGS)
    val makeCategory = post("$ONE/category", SettingsDownloaderCategoryBody.serializer(), NoContent, Need.SETTINGS)
    val attempts = get("$ONE/attempts", SettingsDownloaderAttempts.serializer(), Need.SETTINGS)
    val abandon = post("$ONE/attempts/{attemptID}/abandon", NoContent, Need.SETTINGS)
    val saveSettings = put("/api/v1/settings/downloaders", DownloaderSettings.serializer(), DownloaderSettings.serializer(), Need.SETTINGS)
    val keyLookup = post("/api/v1/migrate/key-lookup", SettingsKeyLookupBody.serializer(), SettingsKeyLookupAnswer.serializer(), Need.OWNER)
}
