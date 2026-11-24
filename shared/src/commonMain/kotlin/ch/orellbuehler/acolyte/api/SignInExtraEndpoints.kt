package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AccountEmpty
import ch.orellbuehler.acolyte.api.model.AccountFoundServers
import ch.orellbuehler.acolyte.api.model.AccountPlexFinish
import ch.orellbuehler.acolyte.api.model.AccountPlexStart
import ch.orellbuehler.acolyte.api.model.AccountResetAnswer
import ch.orellbuehler.acolyte.api.model.AccountResetCode
import ch.orellbuehler.acolyte.api.model.AccountResetFinish
import ch.orellbuehler.acolyte.api.model.AccountSetupDone
import ch.orellbuehler.acolyte.api.model.AccountSetupLink
import ch.orellbuehler.acolyte.api.model.AccountSetupLinked
import ch.orellbuehler.acolyte.api.model.AccountSetupOwner
import ch.orellbuehler.acolyte.api.model.InviteJoinBody
import ch.orellbuehler.acolyte.api.model.InviteLook
import ch.orellbuehler.acolyte.api.model.PasswordSignIn
import ch.orellbuehler.acolyte.api.model.SignInAnswer

internal data object SignInExtraEndpoints : EndpointGroup() {
    val plexStart = post("/api/v1/auth/plex/start", AccountPlexStart.serializer(), Need.PUBLIC)
    val plexFinish = post("/api/v1/auth/plex/finish", AccountPlexFinish.serializer(), SignInAnswer.serializer(), Need.PUBLIC)

    val resetCode = post("/api/v1/auth/owner-reset/code", AccountEmpty.serializer(), AccountResetAnswer.serializer(), Need.PUBLIC)
    val resetCheck = post("/api/v1/auth/owner-reset/check", AccountResetCode.serializer(), AccountResetAnswer.serializer(), Need.PUBLIC)
    val resetFinish =
        post("/api/v1/auth/owner-reset/finish", AccountResetFinish.serializer(), AccountResetAnswer.serializer(), Need.PUBLIC)

    val setup = post("/api/v1/auth/setup", AccountSetupOwner.serializer(), SignInAnswer.serializer(), Need.PUBLIC)
    val setupDiscover = get("/api/v1/auth/setup/discover", AccountFoundServers.serializer(), Need.PUBLIC)
    val setupDone = post("/api/v1/auth/setup/done", AccountSetupDone.serializer(), Need.OWNER, query = listOf("skipped"))
    val setupLink = post("/api/v1/auth/setup/link/{kind}", AccountSetupLink.serializer(), AccountSetupLinked.serializer(), Need.OWNER)

    val inviteLook = get("/api/v1/auth/invites/{token}", InviteLook.serializer(), Need.PUBLIC)
    val inviteJoin = post("/api/v1/auth/invites/{token}", InviteJoinBody.serializer(), SignInAnswer.serializer(), Need.PUBLIC)
    val invitePlexStart = post("/api/v1/auth/invites/{token}/plex/start", AccountPlexStart.serializer(), Need.PUBLIC)
    val invitePlexFinish =
        post("/api/v1/auth/invites/{token}/plex/finish", AccountPlexFinish.serializer(), SignInAnswer.serializer(), Need.PUBLIC)
    val inviteMediaServer =
        post("/api/v1/auth/invites/{token}/media-server/{kind}", PasswordSignIn.serializer(), SignInAnswer.serializer(), Need.PUBLIC)
}
