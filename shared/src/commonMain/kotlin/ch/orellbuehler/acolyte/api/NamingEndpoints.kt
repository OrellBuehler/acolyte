package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.FileSettings
import ch.orellbuehler.acolyte.api.model.SettingsNamingCleanAnswer
import ch.orellbuehler.acolyte.api.model.SettingsNamingExampleAnswer
import ch.orellbuehler.acolyte.api.model.SettingsNamingExampleBody
import ch.orellbuehler.acolyte.api.model.SettingsNamingRules
import ch.orellbuehler.acolyte.api.model.SettingsNamingView

internal data object NamingEndpoints : EndpointGroup() {
    val settings = get("/api/v1/settings/naming", SettingsNamingView.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val save = put("/api/v1/settings/naming", SettingsNamingRules.serializer(), SettingsNamingView.serializer(), Need.SETTINGS, Part.MEDIA)
    val example =
        post(
            "/api/v1/settings/naming/example",
            SettingsNamingExampleBody.serializer(),
            SettingsNamingExampleAnswer.serializer(),
            Need.MANAGE_LIBRARY,
            Part.MEDIA,
        )
    val saveFiles = put("/api/v1/settings/files", FileSettings.serializer(), FileSettings.serializer(), Need.SETTINGS, Part.MEDIA)
    val cleanHolding = post("/api/v1/holding/clean", SettingsNamingCleanAnswer.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
}
