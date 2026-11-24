package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.HomeAnswer
import ch.orellbuehler.acolyte.api.model.HomeDecisions
import ch.orellbuehler.acolyte.api.model.HomeDownloads
import ch.orellbuehler.acolyte.api.model.HomeHealth

internal data object HomeEndpoints : EndpointGroup() {
    val home = get("/api/v1/home", HomeAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val downloads = get("/api/v1/downloads/view", HomeDownloads.serializer(), Need.SIGNED_IN, query = listOf("limit"))
    val health = get("/api/v1/health", HomeHealth.serializer(), Need.MANAGE_LIBRARY)
    val decisions = get("/api/v1/needs-decision", HomeDecisions.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
}
