package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsServerAlert
import ch.orellbuehler.acolyte.api.model.SettingsServerAlertInput
import ch.orellbuehler.acolyte.api.model.SettingsServerAlertTested
import ch.orellbuehler.acolyte.api.model.SettingsServerAlerts
import ch.orellbuehler.acolyte.api.model.SettingsServerBackup
import ch.orellbuehler.acolyte.api.model.SettingsServerBackups
import ch.orellbuehler.acolyte.api.model.SettingsServerLogPage
import ch.orellbuehler.acolyte.api.model.SettingsServerPackSettings
import ch.orellbuehler.acolyte.api.model.SettingsServerPreset
import ch.orellbuehler.acolyte.api.model.SettingsServerRestoreStaged

internal data object SettingsServerEndpoints : EndpointGroup() {
    val alerts = get("/api/v1/alerts", SettingsServerAlerts.serializer(), Need.SETTINGS)
    val addAlert = post("/api/v1/alerts", SettingsServerAlertInput.serializer(), SettingsServerAlert.serializer(), Need.SETTINGS)
    val changeAlert =
        put("/api/v1/alerts/{id}", SettingsServerAlertInput.serializer(), SettingsServerAlert.serializer(), Need.SETTINGS)
    val removeAlert = delete("/api/v1/alerts/{id}", NoContent, Need.SETTINGS)
    val testAlert = post("/api/v1/alerts/{id}/test", SettingsServerAlertTested.serializer(), Need.SETTINGS)
    val packSettings = get("/api/v1/settings/alerts", SettingsServerPackSettings.serializer(), Need.SETTINGS)
    val savePackSettings =
        put("/api/v1/settings/alerts", SettingsServerPackSettings.serializer(), SettingsServerPackSettings.serializer(), Need.SETTINGS)

    val backups = get("/api/v1/backups", SettingsServerBackups.serializer(), Need.SETTINGS)
    val makeBackup = post("/api/v1/backups", SettingsServerBackup.serializer(), Need.SETTINGS)
    val savePreset =
        put("/api/v1/backups/settings", SettingsServerPreset.serializer(), SettingsServerPreset.serializer(), Need.SETTINGS)
    val downloadBackup = get("/api/v1/backups/{name}", NoContent, Need.OWNER)
    val deleteBackup = delete("/api/v1/backups/{name}", NoContent, Need.OWNER)
    val restoreBackup = post("/api/v1/backups/restore", SettingsServerRestoreStaged.serializer(), Need.OWNER)
    val keyKit = post("/api/v1/system/key-kit", NoContent, Need.OWNER)

    val logs =
        get(
            "/api/v1/logs",
            SettingsServerLogPage.serializer(),
            Need.USERS,
            query = listOf("level", "q", "from", "to", "offset", "limit"),
        )
    val downloadLogs = get("/api/v1/logs/download", NoContent, Need.USERS)
}
