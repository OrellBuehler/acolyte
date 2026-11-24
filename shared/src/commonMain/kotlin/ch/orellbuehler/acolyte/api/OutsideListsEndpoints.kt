package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.JobQueued
import ch.orellbuehler.acolyte.api.model.KeyBody
import ch.orellbuehler.acolyte.api.model.MaskedKey
import ch.orellbuehler.acolyte.api.model.MaskedToken
import ch.orellbuehler.acolyte.api.model.NeverAddEntry
import ch.orellbuehler.acolyte.api.model.NeverAddList
import ch.orellbuehler.acolyte.api.model.NewNeverAdd
import ch.orellbuehler.acolyte.api.model.OutsideList
import ch.orellbuehler.acolyte.api.model.OutsideListDraft
import ch.orellbuehler.acolyte.api.model.OutsideLists
import ch.orellbuehler.acolyte.api.model.SecretBody
import ch.orellbuehler.acolyte.api.model.TokenBody
import ch.orellbuehler.acolyte.api.model.TraktCheck
import ch.orellbuehler.acolyte.api.model.TraktDeviceCode
import ch.orellbuehler.acolyte.api.model.TraktStatus

internal data object OutsideListsEndpoints : EndpointGroup() {
    val lists = get("/api/v1/outside-lists", OutsideLists.serializer(), Need.SETTINGS, Part.MEDIA)
    val add = post("/api/v1/outside-lists", OutsideListDraft.serializer(), OutsideList.serializer(), Need.SETTINGS, Part.MEDIA)
    val change = put("/api/v1/outside-lists/{id}", OutsideListDraft.serializer(), OutsideList.serializer(), Need.SETTINGS, Part.MEDIA)
    val remove = delete("/api/v1/outside-lists/{id}", NoContent, Need.SETTINGS, Part.MEDIA)
    val sync = post("/api/v1/outside-lists/{id}/sync", JobQueued.serializer(), Need.SETTINGS, Part.MEDIA)
    val traktKey = get("/api/v1/settings/trakt-key", MaskedKey.serializer(), Need.SETTINGS)
    val saveTraktKey = put("/api/v1/settings/trakt-key", KeyBody.serializer(), MaskedKey.serializer(), Need.SETTINGS)
    val traktToken = get("/api/v1/settings/trakt-token", MaskedToken.serializer(), Need.OWNER)
    val saveTraktToken = put("/api/v1/settings/trakt-token", TokenBody.serializer(), MaskedToken.serializer(), Need.OWNER)
    val saveTraktSecret = put("/api/v1/settings/trakt-secret", SecretBody.serializer(), TraktStatus.serializer(), Need.OWNER)
    val traktSignIn = get("/api/v1/settings/trakt-signin", TraktStatus.serializer(), Need.SETTINGS)
    val startTraktSignIn = post("/api/v1/settings/trakt-signin", TraktDeviceCode.serializer(), Need.OWNER)
    val checkTraktSignIn = post("/api/v1/settings/trakt-signin/check", TraktCheck.serializer(), Need.OWNER)
    val endTraktSignIn = delete("/api/v1/settings/trakt-signin", TraktStatus.serializer(), Need.OWNER)
    val mdblistKey = get("/api/v1/settings/mdblist-key", MaskedKey.serializer(), Need.SETTINGS)
    val saveMdblistKey = put("/api/v1/settings/mdblist-key", KeyBody.serializer(), MaskedKey.serializer(), Need.SETTINGS)
    val neverAdd = get("/api/v1/never-add", NeverAddList.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val addNeverAdd = post("/api/v1/never-add", NewNeverAdd.serializer(), NeverAddEntry.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val removeNeverAdd = delete("/api/v1/never-add/{id}", NoContent, Need.MANAGE_LIBRARY, Part.MEDIA)
}
