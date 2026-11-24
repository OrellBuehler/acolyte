package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.HomeAnswer
import ch.orellbuehler.acolyte.api.model.HomeHealth

internal data object HomeEndpoints : EndpointGroup() {
    val home = get("/api/v1/home", HomeAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val health = get("/api/v1/health", HomeHealth.serializer(), Need.MANAGE_LIBRARY)
}
