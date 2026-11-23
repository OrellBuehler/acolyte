package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.About
import ch.orellbuehler.acolyte.api.model.MyProfile
import ch.orellbuehler.acolyte.api.model.Parts

internal data object AppEndpoints : EndpointGroup() {
    val parts = get("/api/v1/parts", Parts.serializer(), Need.SIGNED_IN)
    val profile = get("/api/v1/my/profile", MyProfile.serializer(), Need.SIGNED_IN)
    val about = get("/api/v1/app/about", About.serializer(), Need.SETTINGS)
}
