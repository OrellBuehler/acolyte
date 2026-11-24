package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsMediaServersInput
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersItem
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersLabels
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersList
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersPlexList
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersStarted
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersTested
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersTracks
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersWatch

internal data object SettingsMediaServersEndpoints : EndpointGroup() {
    val list = get("/api/v1/media-servers", SettingsMediaServersList.serializer(), Need.SETTINGS)
    val add = post("/api/v1/media-servers", SettingsMediaServersInput.serializer(), SettingsMediaServersItem.serializer(), Need.SETTINGS)
    val change =
        put("/api/v1/media-servers/{id}", SettingsMediaServersInput.serializer(), SettingsMediaServersItem.serializer(), Need.SETTINGS)
    val remove = delete("/api/v1/media-servers/{id}", NoContent, Need.SETTINGS)
    val test = post("/api/v1/media-servers/{id}/test", SettingsMediaServersTested.serializer(), Need.SETTINGS)
    val plexServers = get("/api/v1/auth/plex/servers", SettingsMediaServersPlexList.serializer(), Need.OWNER)
    val watchHistory = get("/api/v1/settings/watch-history", SettingsMediaServersWatch.serializer(), Need.OWNER, Part.MEDIA)
    val saveWatchHistory =
        put(
            "/api/v1/settings/watch-history",
            SettingsMediaServersWatch.serializer(),
            SettingsMediaServersWatch.serializer(),
            Need.OWNER,
            Part.MEDIA,
        )
    val labels = get("/api/v1/settings/plex-labels", SettingsMediaServersLabels.serializer(), Need.OWNER, Part.MEDIA)
    val saveLabels =
        put(
            "/api/v1/settings/plex-labels",
            SettingsMediaServersLabels.serializer(),
            SettingsMediaServersLabels.serializer(),
            Need.OWNER,
            Part.MEDIA,
        )
    val runLabels = post("/api/v1/settings/plex-labels/run", SettingsMediaServersStarted.serializer(), Need.OWNER, Part.MEDIA)
    val tracks = get("/api/v1/settings/default-tracks", SettingsMediaServersTracks.serializer(), Need.OWNER, Part.MEDIA)
    val saveTracks =
        put(
            "/api/v1/settings/default-tracks",
            SettingsMediaServersTracks.serializer(),
            SettingsMediaServersTracks.serializer(),
            Need.OWNER,
            Part.MEDIA,
        )
}
