package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsLookKeyAnswer
import ch.orellbuehler.acolyte.api.model.SettingsLookKeyTest
import ch.orellbuehler.acolyte.api.model.SettingsLookMetadata
import ch.orellbuehler.acolyte.api.model.SettingsLookMetadataChange
import ch.orellbuehler.acolyte.api.model.SettingsLookMetadataHealth
import ch.orellbuehler.acolyte.api.model.TitleAddDefaults
import ch.orellbuehler.acolyte.api.model.TitleGateDefaults
import ch.orellbuehler.acolyte.api.model.TitleShowDefaults

internal data object SettingsLookEndpoints : EndpointGroup() {
    val saveDefaults =
        put("/api/v1/settings/title-defaults", TitleAddDefaults.serializer(), TitleAddDefaults.serializer(), Need.SETTINGS, Part.MEDIA)
    val saveGate =
        put("/api/v1/settings/release-gate", TitleGateDefaults.serializer(), TitleGateDefaults.serializer(), Need.SETTINGS, Part.MOVIES)
    val saveShows = put("/api/v1/settings/shows", TitleShowDefaults.serializer(), TitleShowDefaults.serializer(), Need.SETTINGS, Part.SHOWS)
    val metadata = get("/api/v1/settings/metadata", SettingsLookMetadata.serializer(), Need.SETTINGS)
    val changeMetadata =
        put("/api/v1/settings/metadata", SettingsLookMetadataChange.serializer(), SettingsLookMetadata.serializer(), Need.SETTINGS)
    val testKey =
        post("/api/v1/settings/metadata/test", SettingsLookKeyTest.serializer(), SettingsLookKeyAnswer.serializer(), Need.SETTINGS)
    val metadataHealth = get("/api/v1/metadata/health", SettingsLookMetadataHealth.serializer(), Need.SIGNED_IN)
}
