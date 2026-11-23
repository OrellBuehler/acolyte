package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.HealthChecks

internal data object HealthEndpoints : EndpointGroup() {
    val checks = get("/api/v1/health/checks", HealthChecks.serializer(), Need.MANAGE_LIBRARY)
    val runChecks = post("/api/v1/health/checks/run", HealthChecks.serializer(), Need.MANAGE_LIBRARY)
}
