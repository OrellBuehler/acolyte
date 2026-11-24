package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsKeysAi
import ch.orellbuehler.acolyte.api.model.SettingsKeysAiChange
import ch.orellbuehler.acolyte.api.model.SettingsKeysArrKeyMade
import ch.orellbuehler.acolyte.api.model.SettingsKeysOldApp
import ch.orellbuehler.acolyte.api.model.SettingsKeysOldAppChange

internal data object SettingsKeysEndpoints : EndpointGroup() {
    val ai = get("/api/v1/settings/ai", SettingsKeysAi.serializer(), Need.SIGNED_IN)
    val changeAi = put("/api/v1/settings/ai", SettingsKeysAiChange.serializer(), SettingsKeysAi.serializer(), Need.OWNER)
    val oldApp = get("/api/v1/settings/old-app-api", SettingsKeysOldApp.serializer(), Need.SETTINGS)
    val changeOldApp =
        put("/api/v1/settings/old-app-api", SettingsKeysOldAppChange.serializer(), SettingsKeysOldApp.serializer(), Need.SETTINGS)
    val makeArrKey = post("/api/v1/settings/old-app-api/keys/{side}", SettingsKeysArrKeyMade.serializer(), Need.SETTINGS)
    val removeArrKey = delete("/api/v1/settings/old-app-api/keys/{side}", NoContent, Need.SETTINGS)
}

internal object SettingsKeysAddresses {
    fun api(serverUrl: String): String = serverUrl.trimEnd('/') + "/api/v1"

    fun mcp(serverUrl: String): String = serverUrl.trimEnd('/') + "/api/v1/mcp"

    fun openApi(serverUrl: String): String = serverUrl.trimEnd('/') + "/api/v1/openapi.json"

    fun remoteAccess(serverUrl: String): String = serverUrl.trimEnd('/') + "/settings/remote-access"
}
