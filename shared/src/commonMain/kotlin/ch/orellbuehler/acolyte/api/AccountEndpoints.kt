package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AccountAiAllow
import ch.orellbuehler.acolyte.api.model.AccountAiAllowed
import ch.orellbuehler.acolyte.api.model.AccountAiInfo
import ch.orellbuehler.acolyte.api.model.AccountAlerts
import ch.orellbuehler.acolyte.api.model.AccountAvatar
import ch.orellbuehler.acolyte.api.model.AccountBackupCodes
import ch.orellbuehler.acolyte.api.model.AccountCode
import ch.orellbuehler.acolyte.api.model.AccountEmpty
import ch.orellbuehler.acolyte.api.model.AccountEnded
import ch.orellbuehler.acolyte.api.model.AccountFollows
import ch.orellbuehler.acolyte.api.model.AccountLanguageChange
import ch.orellbuehler.acolyte.api.model.AccountMediaLink
import ch.orellbuehler.acolyte.api.model.AccountMediaLinkBody
import ch.orellbuehler.acolyte.api.model.AccountMediaLinks
import ch.orellbuehler.acolyte.api.model.AccountNameChange
import ch.orellbuehler.acolyte.api.model.AccountPasswordChange
import ch.orellbuehler.acolyte.api.model.AccountPicture
import ch.orellbuehler.acolyte.api.model.AccountPlexFinish
import ch.orellbuehler.acolyte.api.model.AccountPlexLink
import ch.orellbuehler.acolyte.api.model.AccountPlexStart
import ch.orellbuehler.acolyte.api.model.AccountSessions
import ch.orellbuehler.acolyte.api.model.AccountSsoLinks
import ch.orellbuehler.acolyte.api.model.AccountTwoFactorSetup
import ch.orellbuehler.acolyte.api.model.MyProfile
import ch.orellbuehler.acolyte.api.model.PeopleSsoUnlink
import ch.orellbuehler.acolyte.api.model.TitleMetaFollow

internal data object AccountEndpoints : EndpointGroup() {
    val updateName = put("/api/v1/my/profile", AccountNameChange.serializer(), MyProfile.serializer(), Need.SIGNED_IN)
    val updateLanguage = put("/api/v1/my/language", AccountLanguageChange.serializer(), MyProfile.serializer(), Need.SIGNED_IN)
    val alerts = get("/api/v1/my/alerts", AccountAlerts.serializer(), Need.SIGNED_IN)
    val saveAlerts = put("/api/v1/my/alerts", AccountAlerts.serializer(), AccountAlerts.serializer(), Need.SIGNED_IN)
    val follows = get("/api/v1/my/follows", AccountFollows.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val unfollowAlbum = delete("/api/v1/metadata/album/{mbid}/follow", TitleMetaFollow.serializer(), Need.SIGNED_IN, Part.MUSIC)

    val avatarPut = put("/api/v1/account/avatar", AccountPicture.serializer(), AccountAvatar.serializer(), Need.SELF)
    val avatarDelete = delete("/api/v1/account/avatar", NoContent, Need.SELF)

    val password = post("/api/v1/auth/password", AccountPasswordChange.serializer(), NoContent, Need.SELF)
    val twoFactorSetup = post("/api/v1/auth/two-factor/setup", AccountTwoFactorSetup.serializer(), Need.SELF)
    val twoFactorEnable = post("/api/v1/auth/two-factor/enable", AccountCode.serializer(), AccountBackupCodes.serializer(), Need.SELF)
    val twoFactorDisable = post("/api/v1/auth/two-factor/disable", AccountCode.serializer(), NoContent, Need.SELF)
    val backupCodes = post("/api/v1/auth/two-factor/backup-codes", AccountCode.serializer(), AccountBackupCodes.serializer(), Need.SELF)

    val sessions = get("/api/v1/auth/sessions", AccountSessions.serializer(), Need.SELF)
    val endOthers = delete("/api/v1/auth/sessions", AccountEnded.serializer(), Need.SELF)
    val endSession = delete("/api/v1/auth/sessions/{sid}", NoContent, Need.SELF)

    val ssoLinks = get("/api/v1/auth/sso/links", AccountSsoLinks.serializer(), Need.SELF)
    val ssoUnlink = post("/api/v1/auth/sso/unlink", PeopleSsoUnlink.serializer(), NoContent, Need.SELF)

    val plexLink = get("/api/v1/auth/plex/my-link", AccountPlexLink.serializer(), Need.SELF)
    val plexLinkStart = post("/api/v1/auth/plex/my-link/start", AccountEmpty.serializer(), AccountPlexStart.serializer(), Need.SELF)
    val plexLinkFinish = post("/api/v1/auth/plex/my-link/finish", AccountPlexFinish.serializer(), AccountPlexLink.serializer(), Need.SELF)
    val plexUnlink = delete("/api/v1/auth/plex/my-link", NoContent, Need.SELF)

    val mediaLinks = get("/api/v1/auth/media-link", AccountMediaLinks.serializer(), Need.SELF)
    val mediaLink = post("/api/v1/auth/media-link/{server}", AccountMediaLinkBody.serializer(), AccountMediaLink.serializer(), Need.SELF)
    val mediaUnlink = delete("/api/v1/auth/media-link/{kind}", NoContent, Need.SELF)

    val aiInfo =
        get(
            "/api/v1/auth/ai-connect",
            AccountAiInfo.serializer(),
            Need.SIGNED_IN,
            query =
                listOf(
                    "response_type",
                    "client_id",
                    "redirect_uri",
                    "code_challenge",
                    "code_challenge_method",
                    "state",
                    "resource",
                    "scope",
                ),
        )
    val aiAllow = post("/api/v1/auth/ai-connect", AccountAiAllow.serializer(), AccountAiAllowed.serializer(), Need.SETTINGS)
}
