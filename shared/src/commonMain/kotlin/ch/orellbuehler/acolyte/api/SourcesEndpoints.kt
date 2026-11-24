package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsBridge
import ch.orellbuehler.acolyte.api.model.SettingsBridgeBody
import ch.orellbuehler.acolyte.api.model.SettingsBridgeCheckAnswer
import ch.orellbuehler.acolyte.api.model.SettingsBridgeList
import ch.orellbuehler.acolyte.api.model.SettingsBridgeRemoved
import ch.orellbuehler.acolyte.api.model.SettingsBridgeTestAnswer
import ch.orellbuehler.acolyte.api.model.SettingsSourceCheckAnswer
import ch.orellbuehler.acolyte.api.model.SettingsSourceCheckBody
import ch.orellbuehler.acolyte.api.model.SettingsSourceInput
import ch.orellbuehler.acolyte.api.model.SettingsSourceStats
import ch.orellbuehler.acolyte.api.model.SettingsSourceTest
import ch.orellbuehler.acolyte.api.model.SourceRef

internal data object SourcesEndpoints : EndpointGroup() {
    val add = post("/api/v1/sources", SettingsSourceInput.serializer(), SourceRef.serializer(), Need.SETTINGS)
    val change = put("/api/v1/sources/{id}", SettingsSourceInput.serializer(), SourceRef.serializer(), Need.SETTINGS)
    val remove = delete("/api/v1/sources/{id}", NoContent, Need.SETTINGS)
    val test = post("/api/v1/sources/{id}/test", SettingsSourceTest.serializer(), Need.SETTINGS)
    val tryNow = post("/api/v1/sources/{id}/try", SettingsSourceTest.serializer(), Need.SETTINGS)
    val pause = post("/api/v1/sources/{id}/pause", SourceRef.serializer(), Need.SETTINGS)
    val resume = post("/api/v1/sources/{id}/resume", SourceRef.serializer(), Need.SETTINGS)
    val check = post("/api/v1/sources/check", SettingsSourceCheckBody.serializer(), SettingsSourceCheckAnswer.serializer(), Need.SETTINGS)
    val stats = get("/api/v1/sources/{id}/stats", SettingsSourceStats.serializer(), Need.MANAGE_LIBRARY, query = listOf("days"))

    val bridges = get("/api/v1/bridges", SettingsBridgeList.serializer(), Need.MANAGE_LIBRARY)
    val linkBridge = post("/api/v1/bridges", SettingsBridgeBody.serializer(), SettingsBridge.serializer(), Need.SETTINGS)
    val testBridge = post("/api/v1/bridges/test", SettingsBridgeBody.serializer(), SettingsBridgeTestAnswer.serializer(), Need.SETTINGS)
    val syncBridge = post("/api/v1/bridges/{id}/check", SettingsBridgeCheckAnswer.serializer(), Need.SETTINGS)
    val unlinkBridge = delete("/api/v1/bridges/{id}", SettingsBridgeRemoved.serializer(), Need.SETTINGS, query = listOf("keep_sources"))
}
