package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsControlAnswer
import ch.orellbuehler.acolyte.api.model.SettingsControlConfirm
import ch.orellbuehler.acolyte.api.model.SettingsLinkTest
import ch.orellbuehler.acolyte.api.model.SettingsProxy
import ch.orellbuehler.acolyte.api.model.SettingsScheduleChange
import ch.orellbuehler.acolyte.api.model.SettingsScheduleRow
import ch.orellbuehler.acolyte.api.model.SettingsStorage
import ch.orellbuehler.acolyte.api.model.SettingsUrlBase
import ch.orellbuehler.acolyte.api.model.SettingsUrlBaseChange

internal data object SettingsControlsEndpoints : EndpointGroup() {
    val partOn = post("/api/v1/parts/{part}/on", SettingsControlAnswer.serializer(), Need.USERS)
    val partOff = post("/api/v1/parts/{part}/off", SettingsControlConfirm.serializer(), SettingsControlAnswer.serializer(), Need.USERS)
    val partRestart =
        post("/api/v1/parts/{part}/restart", SettingsControlConfirm.serializer(), SettingsControlAnswer.serializer(), Need.USERS)
    val stagePause = post("/api/v1/stages/{name}/pause", SettingsControlAnswer.serializer(), Need.USERS)
    val stageResume = post("/api/v1/stages/{name}/resume", SettingsControlAnswer.serializer(), Need.USERS)
    val stageRestart = post("/api/v1/stages/{name}/restart", SettingsControlAnswer.serializer(), Need.USERS)
    val linkRestart = post("/api/v1/links/{kind}/{id}/restart", SettingsLinkTest.serializer(), Need.USERS)
    val appRestart = post("/api/v1/app/restart", SettingsControlConfirm.serializer(), SettingsControlAnswer.serializer(), Need.USERS)
    val appSafeMode = post("/api/v1/app/safe-mode", SettingsControlConfirm.serializer(), SettingsControlAnswer.serializer(), Need.USERS)
    val appShutdown = post("/api/v1/app/shutdown", SettingsControlConfirm.serializer(), SettingsControlAnswer.serializer(), Need.USERS)
    val storage = get("/api/v1/app/storage", SettingsStorage.serializer(), Need.SETTINGS)
    val urlBase = get("/api/v1/settings/url-base", SettingsUrlBase.serializer(), Need.SETTINGS)
    val changeUrlBase =
        put("/api/v1/settings/url-base", SettingsUrlBaseChange.serializer(), SettingsUrlBase.serializer(), Need.SETTINGS)
    val proxy = get("/api/v1/settings/proxy", SettingsProxy.serializer(), Need.SETTINGS)
    val changeProxy = put("/api/v1/settings/proxy", SettingsProxy.serializer(), SettingsProxy.serializer(), Need.SETTINGS)
    val changeSchedule =
        put("/api/v1/schedules/{kind}", SettingsScheduleChange.serializer(), SettingsScheduleRow.serializer(), Need.SETTINGS)
}
