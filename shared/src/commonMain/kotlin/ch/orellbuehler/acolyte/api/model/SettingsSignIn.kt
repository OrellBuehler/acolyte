package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SettingsSignInWayWire(
    val on: Boolean = false,
    val url: String = "",
    @SerialName("server_id") val serverId: String = "",
    val join: String = "nobody",
    val allow: String = "",
)

@Serializable
internal data class SettingsSignInPlexOwnerWire(
    val linked: Boolean = false,
    val name: String = "",
    @SerialName("can_import") val canImport: Boolean = false,
)

@Serializable
internal data class SettingsSignInWire(
    @SerialName("skip_login_local_network") val skipLoginLocalNetwork: Boolean = false,
    @SerialName("local_networks") val localNetworks: List<String> = emptyList(),
    @SerialName("local_person") val localPerson: Long = 0,
    @SerialName("local_skip_off") val localSkipOff: String = "",
    @SerialName("media_sign_in") val mediaSignIn: List<String> = emptyList(),
    @SerialName("home_ranges") val homeRanges: List<String> = emptyList(),
    @SerialName("cgnat_range") val cgnatRange: String = "",
    @SerialName("trusted_proxies") val trustedProxies: List<String> = emptyList(),
    @SerialName("trusted_proxies_fixed") val trustedProxiesFixed: List<String> = emptyList(),
    @SerialName("require_two_factor_admins") val requireTwoFactorAdmins: Boolean = false,
    @SerialName("owner_reset_link") val ownerResetLink: Boolean = true,
    @SerialName("min_password_length") val minPasswordLength: Int = 8,
    val plex: SettingsSignInWayWire = SettingsSignInWayWire(),
    val jellyfin: SettingsSignInWayWire = SettingsSignInWayWire(),
    val emby: SettingsSignInWayWire = SettingsSignInWayWire(),
    @SerialName("plex_owner") val plexOwner: SettingsSignInPlexOwnerWire = SettingsSignInPlexOwnerWire(),
    @SerialName("allowed_hosts") val allowedHosts: List<String> = emptyList(),
    @SerialName("seen_address") val seenAddress: String = "",
    @SerialName("gateway_networks") val gatewayNetworks: List<String> = emptyList(),
    @SerialName("proxy_untrusted") val proxyUntrusted: Boolean = false,
    @SerialName("seen_gateway") val seenGateway: Boolean = false,
)

@Serializable
internal data class SettingsSignInWayChange(
    val on: Boolean,
    val url: String? = null,
    @SerialName("server_id") val serverId: String? = null,
    val join: String,
    val allow: String,
)

@Serializable
internal data class SettingsSignInChange(
    @SerialName("skip_login_local_network") val skipLoginLocalNetwork: Boolean? = null,
    @SerialName("local_networks") val localNetworks: List<String>? = null,
    @SerialName("local_person") val localPerson: Long? = null,
    @SerialName("trusted_proxies") val trustedProxies: List<String>? = null,
    @SerialName("require_two_factor_admins") val requireTwoFactorAdmins: Boolean? = null,
    @SerialName("owner_reset_link") val ownerResetLink: Boolean? = null,
    @SerialName("min_password_length") val minPasswordLength: Int? = null,
    val plex: SettingsSignInWayChange? = null,
    val jellyfin: SettingsSignInWayChange? = null,
    val emby: SettingsSignInWayChange? = null,
    @SerialName("allowed_hosts") val allowedHosts: List<String>? = null,
    @SerialName("confirm_everyone") val confirmEveryone: Boolean? = null,
)

@Serializable
internal data class SettingsSignInPlexStarted(
    val id: Long = 0,
    @SerialName("auth_url") val authUrl: String = "",
)

@Serializable
internal data class SettingsSignInPlexFinish(
    val id: Long,
)

@Serializable
internal data class SettingsSignInPlexServerWire(
    val id: String = "",
    val name: String = "",
)

@Serializable
internal data class SettingsSignInPlexFinished(
    val waiting: Boolean = false,
    val name: String = "",
    val servers: List<SettingsSignInPlexServerWire> = emptyList(),
)

@Serializable
data class SettingsSignInSsoForward(
    val on: Boolean = false,
    val issuer: String = "",
    @SerialName("allow_join") val allowJoin: Boolean = false,
    val links: Int = 0,
)

@Serializable
data class SettingsSignInSsoView(
    val source: String = "settings",
    val on: Boolean = false,
    val running: Boolean = false,
    val issuer: String = "",
    @SerialName("client_id") val clientId: String = "",
    @SerialName("secret_set") val secretSet: Boolean = false,
    @SerialName("redirect_url") val redirectUrl: String = "",
    @SerialName("suggested_redirect_url") val suggestedRedirectUrl: String = "",
    @SerialName("allow_join") val allowJoin: Boolean = false,
    @SerialName("groups_claim") val groupsClaim: String = "",
    @SerialName("group_roles") val groupRoles: String = "",
    val links: Int = 0,
    @SerialName("local_network_allowed") val localNetworkAllowed: Boolean = false,
    val forward: SettingsSignInSsoForward = SettingsSignInSsoForward(),
)

@Serializable
internal data class SettingsSignInSsoSave(
    val password: String,
    val code: String? = null,
    val on: Boolean,
    val issuer: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String? = null,
    @SerialName("public_client") val publicClient: Boolean,
    @SerialName("redirect_url") val redirectUrl: String,
    @SerialName("allow_join") val allowJoin: Boolean,
    @SerialName("confirm_everyone") val confirmEveryone: Boolean,
    @SerialName("groups_claim") val groupsClaim: String,
    @SerialName("group_roles") val groupRoles: String,
)

@Serializable
internal data class SettingsSignInSsoSaved(
    val sso: SettingsSignInSsoView = SettingsSignInSsoView(),
    @SerialName("links_removed") val linksRemoved: Int = 0,
    @SerialName("sessions_ended") val sessionsEnded: Int = 0,
)

@Serializable
internal data class SettingsSignInSsoTest(
    val issuer: String,
)

@Serializable
internal data class SettingsSignInSsoTested(
    val ok: Boolean = false,
)
