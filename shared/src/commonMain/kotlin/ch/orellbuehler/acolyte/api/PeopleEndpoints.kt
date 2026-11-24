package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AccountAddressAnswer
import ch.orellbuehler.acolyte.api.model.AccountAddressTest
import ch.orellbuehler.acolyte.api.model.AccountEnded
import ch.orellbuehler.acolyte.api.model.AccountImportBody
import ch.orellbuehler.acolyte.api.model.AccountImportPeople
import ch.orellbuehler.acolyte.api.model.AccountImportPreview
import ch.orellbuehler.acolyte.api.model.AccountImported
import ch.orellbuehler.acolyte.api.model.AccountKeyChange
import ch.orellbuehler.acolyte.api.model.AccountKeyChanged
import ch.orellbuehler.acolyte.api.model.AccountKeyMade
import ch.orellbuehler.acolyte.api.model.AccountKeyMake
import ch.orellbuehler.acolyte.api.model.AccountKeys
import ch.orellbuehler.acolyte.api.model.AccountMediaLink
import ch.orellbuehler.acolyte.api.model.AccountSessions
import ch.orellbuehler.acolyte.api.model.PeopleChange
import ch.orellbuehler.acolyte.api.model.PeopleChanged
import ch.orellbuehler.acolyte.api.model.PeopleInviteBody
import ch.orellbuehler.acolyte.api.model.PeopleInviteMade
import ch.orellbuehler.acolyte.api.model.PeopleInvites
import ch.orellbuehler.acolyte.api.model.PeopleMediaAccounts
import ch.orellbuehler.acolyte.api.model.PeopleMediaLinkBody
import ch.orellbuehler.acolyte.api.model.PeoplePassword
import ch.orellbuehler.acolyte.api.model.PeopleSsoUnlink

internal data object PeopleEndpoints : EndpointGroup() {
    val invites = get("/api/v1/users/invites", PeopleInvites.serializer(), Need.USERS)
    val makeInvite = post("/api/v1/users/invites", PeopleInviteBody.serializer(), PeopleInviteMade.serializer(), Need.USERS)
    val revokeInvite = delete("/api/v1/users/invites/{id}", NoContent, Need.USERS)
    val mediaAccounts = get("/api/v1/users/media-accounts", PeopleMediaAccounts.serializer(), Need.OWNER)
    val change = put("/api/v1/users/{id}", PeopleChange.serializer(), PeopleChanged.serializer(), Need.USERS)
    val remove = delete("/api/v1/users/{id}", NoContent, Need.USERS)
    val setPassword = post("/api/v1/users/{id}/password", PeoplePassword.serializer(), NoContent, Need.USERS)
    val resetTwoFactor = post("/api/v1/users/{id}/two-factor/reset", NoContent, Need.USERS)
    val sessions = get("/api/v1/users/{id}/sessions", AccountSessions.serializer(), Need.USERS)
    val endAll = post("/api/v1/users/{id}/sessions/end-all", AccountEnded.serializer(), Need.USERS)
    val endSession = delete("/api/v1/users/{id}/sessions/{sid}", NoContent, Need.USERS)
    val ssoUnlink = post("/api/v1/users/{id}/sso/unlink", PeopleSsoUnlink.serializer(), NoContent, Need.OWNER)
    val mediaLink = put("/api/v1/users/{id}/media-link", PeopleMediaLinkBody.serializer(), AccountMediaLink.serializer(), Need.OWNER)
    val mediaUnlink = delete("/api/v1/users/{id}/media-link/{kind}", NoContent, Need.OWNER)

    val keys = get("/api/v1/auth/keys", AccountKeys.serializer(), Need.SETTINGS)
    val makeKey = post("/api/v1/auth/keys", AccountKeyMake.serializer(), AccountKeyMade.serializer(), Need.SETTINGS)
    val changeKey = put("/api/v1/auth/keys/{id}", AccountKeyChange.serializer(), AccountKeyChanged.serializer(), Need.SETTINGS)
    val revokeKey = delete("/api/v1/auth/keys/{id}", NoContent, Need.SETTINGS)

    val testAddress =
        post("/api/v1/auth/media-server/{kind}/test", AccountAddressTest.serializer(), AccountAddressAnswer.serializer(), Need.OWNER)
    val importPreview =
        post("/api/v1/auth/import/{kind}/preview", AccountImportPreview.serializer(), AccountImportPeople.serializer(), Need.OWNER)
    val importPeople = post("/api/v1/auth/import/{kind}", AccountImportBody.serializer(), AccountImported.serializer(), Need.OWNER)
}
