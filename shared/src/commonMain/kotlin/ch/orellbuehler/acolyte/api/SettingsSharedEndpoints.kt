package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsHealthStates
import ch.orellbuehler.acolyte.api.model.SettingsScheduleRun
import ch.orellbuehler.acolyte.api.model.SettingsSchedules
import ch.orellbuehler.acolyte.api.model.SettingsUiChange
import ch.orellbuehler.acolyte.api.model.SettingsUiSettings
import ch.orellbuehler.acolyte.api.model.SettingsUpdateInfo

internal data object SettingsSharedEndpoints : EndpointGroup() {
    val healthStates = get("/api/v1/health/states", SettingsHealthStates.serializer(), Need.MANAGE_LIBRARY)
    val schedules = get("/api/v1/schedules", SettingsSchedules.serializer(), Need.MANAGE_LIBRARY)
    val runSchedule = post("/api/v1/schedules/{kind}/run", SettingsScheduleRun.serializer(), Need.MANAGE_LIBRARY)
    val updateInfo = get("/api/v1/app/update", SettingsUpdateInfo.serializer(), Need.MANAGE_LIBRARY)
    val uiSettings = get("/api/v1/settings/ui", SettingsUiSettings.serializer(), Need.SIGNED_IN)
    val changeUiSettings = put("/api/v1/settings/ui", SettingsUiChange.serializer(), SettingsUiSettings.serializer(), Need.SETTINGS)
}
