package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AskFolders
import ch.orellbuehler.acolyte.api.model.AskPickRulesList
import ch.orellbuehler.acolyte.api.model.AskSettings
import ch.orellbuehler.acolyte.api.model.AskWatchServices
import ch.orellbuehler.acolyte.api.model.CleanupCheck
import ch.orellbuehler.acolyte.api.model.PlexFriends

internal data object RequestSettingsEndpoints : EndpointGroup() {
    val settings = get("/api/v1/settings/asks", AskSettings.serializer(), Need.SETTINGS, Part.WISHLIST)
    val save = put("/api/v1/settings/asks", AskSettings.serializer(), AskSettings.serializer(), Need.SETTINGS, Part.WISHLIST)
    val plexFriends = get("/api/v1/settings/asks/plex-friends", PlexFriends.serializer(), Need.USERS, Part.WISHLIST)
    val cleanup = get("/api/v1/settings/asks/streaming-cleanup", CleanupCheck.serializer(), Need.SETTINGS, Part.WISHLIST)
    val startCleanup = post("/api/v1/settings/asks/streaming-cleanup", CleanupCheck.serializer(), Need.SETTINGS, Part.WISHLIST)
    val folders = get("/api/v1/library-folders", AskFolders.serializer(), Need.MANAGE_LIBRARY)
    val pickRules = get("/api/v1/pick-rules", AskPickRulesList.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val watchServices = get("/api/v1/watch-services", AskWatchServices.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("region"))
}
