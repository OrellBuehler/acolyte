package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsSignInChange
import ch.orellbuehler.acolyte.api.model.SettingsSignInPlexFinish
import ch.orellbuehler.acolyte.api.model.SettingsSignInPlexFinished
import ch.orellbuehler.acolyte.api.model.SettingsSignInPlexOwnerWire
import ch.orellbuehler.acolyte.api.model.SettingsSignInPlexStarted
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoSave
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoSaved
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoTest
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoTested
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoView
import ch.orellbuehler.acolyte.api.model.SettingsSignInWire

internal data object SettingsSignInEndpoints : EndpointGroup() {
    val settings = get("/api/v1/auth/settings", SettingsSignInWire.serializer(), Need.SETTINGS)
    val change = put("/api/v1/auth/settings", SettingsSignInChange.serializer(), SettingsSignInWire.serializer(), Need.OWNER)
    val plexStart = post("/api/v1/auth/plex/link/start", SettingsSignInPlexStarted.serializer(), Need.OWNER)
    val plexFinish =
        post("/api/v1/auth/plex/link/finish", SettingsSignInPlexFinish.serializer(), SettingsSignInPlexFinished.serializer(), Need.OWNER)
    val plexUnlink = delete("/api/v1/auth/plex/link", SettingsSignInPlexOwnerWire.serializer(), Need.OWNER)
    val sso = get("/api/v1/auth/sso", SettingsSignInSsoView.serializer(), Need.OWNER)
    val saveSso = put("/api/v1/auth/sso", SettingsSignInSsoSave.serializer(), SettingsSignInSsoSaved.serializer(), Need.OWNER)
    val testSso = post("/api/v1/auth/sso/test", SettingsSignInSsoTest.serializer(), SettingsSignInSsoTested.serializer(), Need.OWNER)
}
