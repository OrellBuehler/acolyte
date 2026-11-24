package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsStatusAnswer

internal data object SettingsHomeEndpoints : EndpointGroup() {
    val status = get("/api/v1/settings/status", SettingsStatusAnswer.serializer(), Need.SETTINGS)
}
