package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.ApiKeyGroups
import ch.orellbuehler.acolyte.api.model.ApiKeyGroupsChange
import ch.orellbuehler.acolyte.api.model.ApiKeyList
import ch.orellbuehler.acolyte.api.model.ImportBody
import ch.orellbuehler.acolyte.api.model.ImportMade
import ch.orellbuehler.acolyte.api.model.ImportPreview
import ch.orellbuehler.acolyte.api.model.ImportPreviewBody
import ch.orellbuehler.acolyte.api.model.MadeApiKey
import ch.orellbuehler.acolyte.api.model.MediaServerProbe
import ch.orellbuehler.acolyte.api.model.MediaServerProbed
import ch.orellbuehler.acolyte.api.model.NewApiKey

internal data object AccountKeysEndpoints : EndpointGroup() {
    val keys = get("/api/v1/auth/keys", ApiKeyList.serializer(), Need.SETTINGS)
    val makeKey = post("/api/v1/auth/keys", NewApiKey.serializer(), MadeApiKey.serializer(), Need.SETTINGS)
    val changeKey = put("/api/v1/auth/keys/{id}", ApiKeyGroupsChange.serializer(), ApiKeyGroups.serializer(), Need.SETTINGS)
    val revokeKey = delete("/api/v1/auth/keys/{id}", NoContent, Need.SETTINGS)
    val previewImport = post("/api/v1/auth/import/{kind}/preview", ImportPreviewBody.serializer(), ImportPreview.serializer(), Need.OWNER)
    val importPeople = post("/api/v1/auth/import/{kind}", ImportBody.serializer(), ImportMade.serializer(), Need.OWNER)
    val probeMediaServer =
        post("/api/v1/auth/media-server/{kind}/test", MediaServerProbe.serializer(), MediaServerProbed.serializer(), Need.OWNER)
}
