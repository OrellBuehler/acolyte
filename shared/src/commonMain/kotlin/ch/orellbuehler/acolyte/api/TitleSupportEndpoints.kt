package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.EventsPage
import ch.orellbuehler.acolyte.api.model.HideBody
import ch.orellbuehler.acolyte.api.model.OwnedDownload
import ch.orellbuehler.acolyte.api.model.SubtitleTranslator
import ch.orellbuehler.acolyte.api.model.TitleAddDefaults
import ch.orellbuehler.acolyte.api.model.TitleGateDefaults
import ch.orellbuehler.acolyte.api.model.TitleHide
import ch.orellbuehler.acolyte.api.model.TitleShowDefaults
import ch.orellbuehler.acolyte.api.model.TitleUsers

internal data object TitleSupportEndpoints : EndpointGroup() {
    val events =
        get(
            "/api/v1/events",
            EventsPage.serializer(),
            Need.MANAGE_LIBRARY,
            Part.CORE,
            query = listOf("title", "episode", "kind", "from", "to", "source", "feed", "limit", "offset"),
        )
    val markFailed = post("/api/v1/downloads/{id}/mark-failed", OwnedDownload.serializer(), Need.MANAGE_LIBRARY, Part.CORE)
    val defaults = get("/api/v1/settings/title-defaults", TitleAddDefaults.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val gate = get("/api/v1/settings/release-gate", TitleGateDefaults.serializer(), Need.MANAGE_LIBRARY, Part.MOVIES)
    val showDefaults = get("/api/v1/settings/shows", TitleShowDefaults.serializer(), Need.MANAGE_LIBRARY, Part.SHOWS)
    val users = get("/api/v1/users", TitleUsers.serializer(), Need.USERS, Part.CORE)
    val hide = post("/api/v1/hidden", HideBody.serializer(), TitleHide.serializer(), Need.USERS, Part.MEDIA)
    val unhide = delete("/api/v1/hidden/{id}", NoContent, Need.USERS, Part.MEDIA)
    val translator = get("/api/v1/settings/subtitles/translator", SubtitleTranslator.serializer(), Need.SETTINGS, Part.SUBTITLES)
}
