package ch.orellbuehler.acolyte.feature.settings.signin

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoView
import ch.orellbuehler.acolyte.feature.LoadStatus

private const val DEFAULT_CGNAT = "100.64.0.0/10"
private const val MIN_PASSWORD = 4
private const val MAX_PASSWORD = 64
private const val SHORT_PASSWORD = 8

enum class SettingsSignInWayKind(
    internal val key: String,
    val app: String,
) {
    PLEX("plex", "Plex"),
    JELLYFIN("jellyfin", "Jellyfin"),
    EMBY("emby", "Emby"),
}

enum class SettingsSignInJoin(
    internal val wire: String,
) {
    NOBODY("nobody"),
    LIST("list"),
    EVERYONE("everyone"),
}

data class SettingsSignInTest(
    val ok: Boolean,
    val text: String = "",
    val error: ApiError? = null,
)

data class SettingsSignInWay(
    val kind: SettingsSignInWayKind,
    val on: Boolean = false,
    val url: String = "",
    val serverId: String = "",
    val join: SettingsSignInJoin = SettingsSignInJoin.NOBODY,
    val allow: String = "",
    val savedJoin: SettingsSignInJoin = SettingsSignInJoin.NOBODY,
    val saving: Boolean = false,
    val testing: Boolean = false,
    val test: SettingsSignInTest? = null,
    val lastCount: Int = -1,
) {
    val untested: Boolean get() = kind != SettingsSignInWayKind.PLEX && url.isNotEmpty() && test == null
}

enum class SettingsSignInLinkPhase {
    IDLE,
    STARTING,
    WAITING,
    LINKED_PICK,
    LINKED_NONE,
    TIMED_OUT,
    FAILED,
}

enum class SettingsSignInLinkFailure {
    ENDED,
    TAKEN,
    TOO_MANY,
    NEEDS_PASSWORD,
    OTHER,
}

data class SettingsSignInPlexServer(
    val id: String,
    val name: String,
)

data class SettingsSignInPlex(
    val linked: Boolean = false,
    val name: String = "",
    val canImport: Boolean = false,
    val phase: SettingsSignInLinkPhase = SettingsSignInLinkPhase.IDLE,
    val failure: SettingsSignInLinkFailure = SettingsSignInLinkFailure.OTHER,
    val error: ApiError? = null,
    val authUrl: String = "",
    val openCount: Int = 0,
    val servers: List<SettingsSignInPlexServer> = emptyList(),
    val unlinking: Boolean = false,
) {
    val linking: Boolean get() = phase == SettingsSignInLinkPhase.STARTING || phase == SettingsSignInLinkPhase.WAITING
}

enum class SettingsSignInEveryoneWho {
    PICKED_SERVER,
    EVERY_PLEX,
    SERVER,
}

data class SettingsSignInEveryoneAsk(
    val kind: SettingsSignInWayKind,
    val who: SettingsSignInEveryoneWho,
    val count: Int,
)

enum class SettingsSignInImportPhase {
    ASK,
    LOADING,
    READY,
}

data class SettingsSignInCandidate(
    val id: String,
    val name: String,
    val email: String,
    val kid: Boolean,
    val serverAdmin: Boolean,
    val linkedTo: String,
    val picked: Boolean,
    val asKid: Boolean,
) {
    val locked: Boolean get() = linkedTo.isNotEmpty()
}

data class SettingsSignInImport(
    val kind: SettingsSignInWayKind,
    val phase: SettingsSignInImportPhase,
    val people: List<SettingsSignInCandidate> = emptyList(),
    val busy: Boolean = false,
    val error: ApiError? = null,
    val confirmAdults: List<String> = emptyList(),
) {
    val pickedCount: Int get() = people.count { !it.locked && it.picked }
}

enum class SettingsSignInSsoPhase {
    HIDDEN,
    NEEDS_SESSION,
    LOCKED,
    LOADING,
    READY,
    FAILED,
}

enum class SettingsSignInSsoError {
    RETYPE,
    CONFIRM_BOX,
    WRONG_PROOF,
    GROUP_ROLES,
    OTHER,
}

data class SettingsSignInSso(
    val phase: SettingsSignInSsoPhase = SettingsSignInSsoPhase.HIDDEN,
    val loadError: ApiError? = null,
    val view: SettingsSignInSsoView = SettingsSignInSsoView(),
    val twoFactor: Boolean? = null,
    val on: Boolean = false,
    val issuer: String = "",
    val clientId: String = "",
    val secret: String = "",
    val publicClient: Boolean = false,
    val allowJoin: Boolean = false,
    val groupsClaim: String = "",
    val groupRoles: String = "",
    val confirmEveryone: Boolean = false,
    val testing: Boolean = false,
    val test: SettingsSignInTest? = null,
    val saving: Boolean = false,
    val saveError: SettingsSignInSsoError? = null,
    val error: ApiError? = null,
) {
    val environment: Boolean get() = view.source == "environment"

    val callback: String get() = view.redirectUrl.ifEmpty { view.suggestedRedirectUrl }

    val moving: Boolean
        get() =
            view.issuer.isNotEmpty() &&
                (issuer.trim().trimEnd('/') != view.issuer.trim().trimEnd('/') || clientId.trim() != view.clientId)

    val mustRetype: Boolean get() = moving && view.secretSet && secret.isEmpty() && !publicClient

    val opening: Boolean get() = on && allowJoin && !(view.on && view.allowJoin && !moving)

    val secretKept: Boolean get() = view.secretSet && !moving

    val showCode: Boolean get() = twoFactor != false

    val needsCode: Boolean get() = twoFactor == true
}

enum class SettingsSignInRole {
    OWNER,
    ADMIN,
    MEMBER,
}

data class SettingsSignInPerson(
    val id: Long,
    val name: String,
    val role: SettingsSignInRole,
    val kidLimit: String,
    val avatar: String,
)

enum class SettingsSignInSkipOff {
    NONE,
    KID,
    PERSON_GONE,
}

enum class SettingsSignInSeen {
    NONE,
    OWN_GATEWAY,
    BRIDGE,
    COVERED,
    NOT_COVERED,
}

data class SettingsSignInLocal(
    val skip: Boolean = false,
    val skipSaving: Boolean = false,
    val networks: String = "",
    val proxies: String = "",
    val person: Long = 0,
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val people: List<SettingsSignInPerson>? = null,
    val peopleError: ApiError? = null,
    val savedNetworks: List<String> = emptyList(),
    val savedProxies: List<String> = emptyList(),
    val savedPerson: Long = 0,
    val fixedProxies: List<String> = emptyList(),
    val homeRanges: List<String> = emptyList(),
    val cgnatRange: String = DEFAULT_CGNAT,
    val seenAddress: String = "",
    val gatewayNetworks: List<String> = emptyList(),
    val skipOff: SettingsSignInSkipOff = SettingsSignInSkipOff.NONE,
    val proxyUntrusted: Boolean = false,
    val seenGateway: Boolean = false,
    val mediaApps: List<String> = emptyList(),
) {
    val netProblems: List<SettingsSignInLineProblem> get() = SettingsSignInLines.problems(networks, false, gatewayNetworks)

    val proxyProblems: List<SettingsSignInLineProblem> get() = SettingsSignInLines.problems(proxies, true)

    val problemCount: Int get() = netProblems.size + proxyProblems.size

    val dirty: Boolean
        get() =
            networks + "|" + proxies + "|" + person !=
                savedNetworks.joinToString("\n") + "|" + savedProxies.joinToString("\n") + "|" + savedPerson

    val canSave: Boolean get() = !saving && problemCount == 0 && dirty

    val ownerId: Long get() = people?.firstOrNull { it.role == SettingsSignInRole.OWNER }?.id ?: -1

    val pickedId: Long get() = if (person == 0L) ownerId else person

    val hasTailscale: Boolean get() = SettingsSignInLines.lines(networks).contains(cgnatRange)

    val seen: SettingsSignInSeen
        get() {
            if (seenAddress.isEmpty()) return SettingsSignInSeen.NONE
            if (seenGateway) return SettingsSignInSeen.OWN_GATEWAY
            if (SettingsSignInLines.gatewayLike(seenAddress)) return SettingsSignInSeen.BRIDGE
            val covered = SettingsSignInLines.covers(seenAddress, SettingsSignInLines.lines(networks))
            return if (covered == true) SettingsSignInSeen.COVERED else SettingsSignInSeen.NOT_COVERED
        }
}

data class SettingsSignInHosts(
    val text: String = "",
    val saved: List<String> = emptyList(),
    val saving: Boolean = false,
)

enum class SettingsSignInNoticeKind {
    MIN_SAVED,
    TWO_FACTOR_REQUIRED,
    TWO_FACTOR_OPTIONAL,
    FORGOT_SHOWN,
    FORGOT_HIDDEN,
    WAY_SAVED,
    WAY_UNREACHABLE,
    PLEX_LINKED,
    PLEX_UNLINKED,
    IMPORTED,
    IMPORT_NONE,
    SSO_SAVED,
    LOCAL_ON,
    LOCAL_OFF,
    LOCAL_SAVED,
    HOSTS_SAVED,
    UNLINK_FAILED,
    FAILED,
}

data class SettingsSignInNotice(
    val seq: Int,
    val kind: SettingsSignInNoticeKind,
    val name: String = "",
    val count: Int = 0,
    val count2: Int = 0,
    val error: ApiError? = null,
)

data class SettingsSignInState(
    val status: LoadStatus = LoadStatus(),
    val isOwner: Boolean = false,
    val apiKey: Boolean = false,
    val minLengthText: String = "8",
    val minLengthSaving: Boolean = false,
    val requireTwoFactor: Boolean = false,
    val requireTwoFactorSaving: Boolean = false,
    val forgotLink: Boolean = true,
    val forgotLinkSaving: Boolean = false,
    val ways: List<SettingsSignInWay> = SettingsSignInWayKind.entries.map { SettingsSignInWay(it) },
    val plex: SettingsSignInPlex = SettingsSignInPlex(),
    val everyoneAsk: SettingsSignInEveryoneAsk? = null,
    val importSheet: SettingsSignInImport? = null,
    val sso: SettingsSignInSso = SettingsSignInSso(),
    val local: SettingsSignInLocal = SettingsSignInLocal(),
    val hosts: SettingsSignInHosts = SettingsSignInHosts(),
    val notice: SettingsSignInNotice? = null,
) {
    val minLengthValue: Int? get() = minLengthText.toIntOrNull()?.takeIf { it in MIN_PASSWORD..MAX_PASSWORD }

    val shortPasswordWarning: Boolean get() = (minLengthText.toIntOrNull() ?: 0) < SHORT_PASSWORD

    val canSaveMinLength: Boolean get() = isOwner && minLengthValue != null && !minLengthSaving

    val showLocal: Boolean get() = local.mediaApps.isEmpty() || local.skip

    fun way(kind: SettingsSignInWayKind): SettingsSignInWay = ways.first { it.kind == kind }

    fun canImport(kind: SettingsSignInWayKind): Boolean =
        if (kind == SettingsSignInWayKind.PLEX) plex.canImport else way(kind).url.isNotEmpty()
}
