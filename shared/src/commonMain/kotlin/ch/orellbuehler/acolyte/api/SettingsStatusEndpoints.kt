package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsHealthChecksChange
import ch.orellbuehler.acolyte.api.model.SettingsHealthChecksSettings
import ch.orellbuehler.acolyte.api.model.SettingsStatusActiveJobs
import ch.orellbuehler.acolyte.api.model.SettingsStatusDisks
import ch.orellbuehler.acolyte.api.model.SettingsStatusInstall
import ch.orellbuehler.acolyte.api.model.SettingsStatusModeChange
import ch.orellbuehler.acolyte.api.model.SettingsStatusTest
import ch.orellbuehler.acolyte.api.model.SettingsUpdateInfo

internal data object SettingsStatusEndpoints : EndpointGroup() {
    val disks = get("/api/v1/health/disks", SettingsStatusDisks.serializer(), Need.MANAGE_LIBRARY)
    val activeJobs = get("/api/v1/jobs/active", SettingsStatusActiveJobs.serializer(), Need.MANAGE_LIBRARY)
    val checkUpdate = post("/api/v1/app/update/check", SettingsUpdateInfo.serializer(), Need.SETTINGS)
    val install = get("/api/v1/app/update/install", SettingsStatusInstall.serializer(), Need.SETTINGS)
    val installNow = post("/api/v1/app/update/install", SettingsStatusInstall.serializer(), Need.USERS)
    val changeMode = put("/api/v1/app/update/mode", SettingsStatusModeChange.serializer(), SettingsStatusInstall.serializer(), Need.USERS)
    val goBack = post("/api/v1/app/update/go-back", SettingsStatusInstall.serializer(), Need.USERS)
    val supportBundle = get("/api/v1/support/bundle", NoContent, Need.USERS)
    val healthSettings = get("/api/v1/settings/health", SettingsHealthChecksSettings.serializer(), Need.SETTINGS)
    val saveHealthSettings =
        put(
            "/api/v1/settings/health",
            SettingsHealthChecksChange.serializer(),
            SettingsHealthChecksSettings.serializer(),
            Need.SETTINGS,
        )
    val testSource = post("/api/v1/sources/{id}/test", SettingsStatusTest.serializer(), Need.SETTINGS)
    val testDownloader = post("/api/v1/downloaders/{id}/test", SettingsStatusTest.serializer(), Need.SETTINGS)
}
