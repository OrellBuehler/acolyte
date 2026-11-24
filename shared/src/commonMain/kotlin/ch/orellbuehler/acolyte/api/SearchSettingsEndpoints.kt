package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.JobIds
import ch.orellbuehler.acolyte.api.model.SettingsSearchValues

internal data object SearchSettingsEndpoints : EndpointGroup() {
    val settings = get("/api/v1/settings/search", SettingsSearchValues.serializer(), Need.SETTINGS, Part.MEDIA)
    val save = put("/api/v1/settings/search", SettingsSearchValues.serializer(), SettingsSearchValues.serializer(), Need.SETTINGS, Part.MEDIA)
    val feedCheck = post("/api/v1/search/feed-check", JobIds.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val better = post("/api/v1/search/better", JobIds.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
}
