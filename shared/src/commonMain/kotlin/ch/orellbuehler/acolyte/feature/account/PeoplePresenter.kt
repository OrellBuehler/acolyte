package ch.orellbuehler.acolyte.feature.account

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.PeopleEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.AccountSsoLink
import ch.orellbuehler.acolyte.api.model.PeopleChange
import ch.orellbuehler.acolyte.api.model.PeopleInvite
import ch.orellbuehler.acolyte.api.model.PeopleInviteBody
import ch.orellbuehler.acolyte.api.model.PeopleMediaLinkBody
import ch.orellbuehler.acolyte.api.model.PeopleMediaServer
import ch.orellbuehler.acolyte.api.model.PeoplePassword
import ch.orellbuehler.acolyte.api.model.PeopleSsoUnlink
import ch.orellbuehler.acolyte.api.model.TitleUser
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountRules
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

val PEOPLE_PERMS: List<String> = listOf("ask", "auto_approve", "auto_approve_four_k", "approve", "manage_library", "settings")

val PEOPLE_RATINGS: List<String> = listOf("G", "PG", "PG-13", "R")

enum class PeoplePreset(
    val perms: List<String>,
    val rating: String,
) {
    ADMIN(listOf("ask", "auto_approve", "approve", "manage_library", "settings"), ""),
    MANAGER(listOf("ask", "auto_approve", "approve", "manage_library"), ""),
    MEMBER(listOf("ask"), ""),
    KID(listOf("ask"), "PG"),
    CUSTOM(emptyList(), ""),
}

fun peoplePreset(
    perms: List<String>,
    maxRating: String,
): PeoplePreset =
    PeoplePreset.entries.firstOrNull { it != PeoplePreset.CUSTOM && it.perms.toSet() == perms.toSet() && it.rating == maxRating }
        ?: PeoplePreset.CUSTOM

fun peopleRatingForShows(rating: String): String =
    when (rating) {
        "G" -> "TV-G"
        "PG" -> "TV-PG"
        "PG-13" -> "TV-14"
        "R" -> "TV-MA"
        else -> ""
    }

enum class PeopleNotice {
    INVITE_REVOKED,
    SAVED,
    TURNED_OFF,
    TURNED_ON,
    PASSWORD_SET,
    TWO_FACTOR_RESET,
    DELETED,
    SSO_UNLINKED,
    MEDIA_LINKED,
    MEDIA_UNLINKED,
}

enum class PeopleProblem {
    INVALID_DAYS,
    INVALID_USES,
    PASSWORD_SHORT,
}

data class PeopleInviteDraft(
    val admin: Boolean = false,
    val perms: List<String> = PeoplePreset.MEMBER.perms,
    val rating: String = "",
    val note: String = "",
    val days: Int = 7,
    val maxUses: Int = 1,
)

data class PeopleFreshInvite(
    val url: String,
    val singleUse: Boolean,
)

data class PeopleSsoAsk(
    val userId: Long,
    val link: AccountSsoLink,
)

data class PeopleState(
    val status: LoadStatus = LoadStatus(),
    val users: List<TitleUser> = emptyList(),
    val invites: List<PeopleInvite> = emptyList(),
    val viewerId: Long = 0,
    val viewerOwner: Boolean = false,
    val viewerPerms: List<String> = emptyList(),
    val busy: Boolean = false,
    val fresh: PeopleFreshInvite? = null,
    val notice: PeopleNotice? = null,
    val noticeName: String = "",
    val problem: PeopleProblem? = null,
    val actionError: ApiError? = null,
    val noWayIn: PeopleSsoAsk? = null,
    val servers: List<PeopleMediaServer> = emptyList(),
    val minPasswordLength: Int = 8,
) {
    val grantable: List<String> get() = if (viewerOwner) PEOPLE_PERMS else PEOPLE_PERMS.filter { it in viewerPerms }

    val canMakeAdmin: Boolean get() = viewerOwner

    fun mayChange(user: TitleUser): Boolean = user.role != "owner" && user.id != viewerId && (viewerOwner || user.role == "member")

    fun maySeeDevices(user: TitleUser): Boolean = user.id != viewerId && (viewerOwner || user.role == "member")

    fun isKid(user: TitleUser): Boolean = user.maxRating.isNotEmpty()

    fun presetOf(user: TitleUser): PeoplePreset = peoplePreset(user.perms, user.maxRating)

    val openInvites: List<PeopleInvite> get() = invites.filter { it.isOpen }
}

enum class PeopleInviteState {
    OPEN,
    USED,
    ENDED,
}

val PeopleInvite.isOpen: Boolean get() = state == PeopleInviteState.OPEN

val PeopleInvite.state: PeopleInviteState
    get() {
        val full = maxUses > 0 && uses >= maxUses
        val expired =
            !neverExpires &&
                try {
                    Instant.parse(expiresAt) < Clock.System.now()
                } catch (e: IllegalArgumentException) {
                    false
                }
        return when {
            usedAt.isNotEmpty() && maxUses == 1 -> PeopleInviteState.USED
            full || expired -> PeopleInviteState.ENDED
            else -> PeopleInviteState.OPEN
        }
    }

class PeoplePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
) {
    private val mutableState = MutableStateFlow(PeopleState())
    private val lock = Mutex()

    val state: StateFlow<PeopleState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            var minimum: Int? = null
            val (users, invites) =
                coroutineScope {
                    val u = async { api.result(TitleSupportEndpoints.users) }
                    val i = async { api.result(PeopleEndpoints.invites) }
                    minimum = api.result(AuthEndpoints.state).valueOrNull()?.minPasswordLength
                    u.await() to i.await()
                }
            if (users is ApiResult.Failed) {
                mutableState.update { it.copy(status = it.status.failed(users.error)) }
                return
            }
            val me = account()
            mutableState.update {
                it.copy(
                    status = LoadStatus.ready,
                    users = (users as ApiResult.Ok).value.users,
                    invites = invites.valueOrNull()?.invites ?: it.invites,
                    viewerId = me?.userId ?: 0,
                    viewerOwner = me?.role == Role.OWNER,
                    viewerPerms = me?.permissions.orEmpty(),
                    minPasswordLength = minimum ?: it.minPasswordLength,
                )
            }
        }
    }

    fun consume() {
        mutableState.update { it.copy(notice = null, noticeName = "", problem = null, actionError = null) }
    }

    fun dismissInvite() {
        mutableState.update { it.copy(fresh = null) }
    }

    fun dismissNoWayIn() {
        mutableState.update { it.copy(noWayIn = null) }
    }

    suspend fun makeInvite(draft: PeopleInviteDraft) {
        val owner = mutableState.value.viewerOwner
        val admin = draft.admin && owner
        val days = if (admin) maxOf(draft.days, 1) else draft.days
        val uses = if (admin) 1 else draft.maxUses
        val unlimitedOk = owner && !admin
        if (days !in (if (unlimitedOk) 0 else 1)..30) return problem(PeopleProblem.INVALID_DAYS)
        if (uses !in (if (unlimitedOk) 0 else 1)..1000) return problem(PeopleProblem.INVALID_USES)
        val kid = !admin && draft.rating.isNotEmpty()
        val body =
            PeopleInviteBody(
                role = if (admin) "admin" else "member",
                perms = draft.perms,
                note = draft.note.trim().take(100),
                days = days,
                maxUses = uses,
                maxRating = if (kid) draft.rating else "",
                ratingCountry = "US",
            )
        if (!begin()) return
        when (val result = reauth.guard { api.result(PeopleEndpoints.makeInvite, body) }) {
            is ApiResult.Ok -> {
                val base = account()?.serverUrl.orEmpty()
                mutableState.update {
                    it.copy(busy = false, fresh = PeopleFreshInvite(base + result.value.path, result.value.maxUses == 1))
                }
                reload()
            }
            is ApiResult.Failed -> fail(result.error)
        }
    }

    suspend fun revokeInvite(invite: PeopleInvite) {
        act(PeopleNotice.INVITE_REVOKED, invite.note) { reauth.guard { api.result(PeopleEndpoints.revokeInvite, "id" to invite.id) } }
    }

    suspend fun save(
        user: TitleUser,
        admin: Boolean,
        perms: List<String>,
        rating: String,
    ) {
        val owner = mutableState.value.viewerOwner
        val body =
            PeopleChange(
                role = if (owner) (if (admin) "admin" else "member") else null,
                perms = perms,
                maxRating = if (admin) "" else rating,
                ratingCountry = "US",
            )
        act(PeopleNotice.SAVED, user.name) { reauth.guard { api.result(PeopleEndpoints.change, body, "id" to user.id) } }
    }

    suspend fun turnOff(user: TitleUser) {
        act(PeopleNotice.TURNED_OFF, user.name) {
            reauth.guard { api.result(PeopleEndpoints.change, PeopleChange(state = "disabled"), "id" to user.id) }
        }
    }

    suspend fun turnOn(user: TitleUser) {
        act(PeopleNotice.TURNED_ON, user.name) {
            reauth.guard { api.result(PeopleEndpoints.change, PeopleChange(state = "active"), "id" to user.id) }
        }
    }

    suspend fun setPassword(
        user: TitleUser,
        password: String,
    ) {
        if (!AccountRules.validPassword(password, mutableState.value.minPasswordLength)) return problem(PeopleProblem.PASSWORD_SHORT)
        act(PeopleNotice.PASSWORD_SET, user.name) {
            reauth.guard { api.result(PeopleEndpoints.setPassword, PeoplePassword(password), "id" to user.id) }
        }
    }

    suspend fun resetTwoFactor(user: TitleUser) {
        act(PeopleNotice.TWO_FACTOR_RESET, user.name) { reauth.guard { api.result(PeopleEndpoints.resetTwoFactor, "id" to user.id) } }
    }

    suspend fun delete(user: TitleUser) {
        act(PeopleNotice.DELETED, user.name) { reauth.guard { api.result(PeopleEndpoints.remove, "id" to user.id) } }
    }

    suspend fun unlinkSso(
        user: TitleUser,
        link: AccountSsoLink,
        confirmed: Boolean = false,
    ) {
        if (!begin()) return
        val body = PeopleSsoUnlink(link.kind, link.issuer, if (confirmed) true else null)
        when (val result = reauth.guard { api.result(PeopleEndpoints.ssoUnlink, body, "id" to user.id) }) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(busy = false, notice = PeopleNotice.SSO_UNLINKED, noticeName = user.name, noWayIn = null) }
                reload()
            }
            is ApiResult.Failed ->
                if (result.error.serverCode == "no_way_in") {
                    mutableState.update { it.copy(busy = false, noWayIn = PeopleSsoAsk(user.id, link)) }
                } else {
                    fail(result.error)
                }
        }
    }

    suspend fun confirmNoWayIn() {
        val ask = mutableState.value.noWayIn ?: return
        val user = mutableState.value.users.firstOrNull { it.id == ask.userId } ?: return
        unlinkSso(user, ask.link, confirmed = true)
    }

    suspend fun loadServers() {
        if (!mutableState.value.viewerOwner) return
        mutableState.update { it.copy(servers = emptyList()) }
        when (val result = api.result(PeopleEndpoints.mediaAccounts)) {
            is ApiResult.Ok -> mutableState.update { it.copy(servers = result.value.servers) }
            is ApiResult.Failed -> mutableState.update { it.copy(actionError = result.error) }
        }
    }

    suspend fun linkMedia(
        user: TitleUser,
        serverId: Long,
        accountId: String,
    ) {
        val body = PeopleMediaLinkBody(serverId, accountId)
        act(PeopleNotice.MEDIA_LINKED, user.name) { reauth.guard { api.result(PeopleEndpoints.mediaLink, body, "id" to user.id) } }
    }

    suspend fun unlinkMedia(
        user: TitleUser,
        kind: String,
    ) {
        act(PeopleNotice.MEDIA_UNLINKED, user.name) {
            reauth.guard { api.result(PeopleEndpoints.mediaUnlink, "id" to user.id, "kind" to kind) }
        }
    }

    private suspend fun reload() {
        lock.withLock {
            val users = api.result(TitleSupportEndpoints.users).valueOrNull()?.users
            val invites = api.result(PeopleEndpoints.invites).valueOrNull()?.invites
            mutableState.update { it.copy(users = users ?: it.users, invites = invites ?: it.invites) }
        }
    }

    private fun problem(problem: PeopleProblem) {
        mutableState.update { it.copy(problem = problem) }
    }

    private fun begin(): Boolean {
        if (mutableState.value.busy) return false
        mutableState.update { it.copy(busy = true, actionError = null, problem = null, notice = null) }
        return true
    }

    private fun fail(error: ApiError) {
        mutableState.update { it.copy(busy = false, actionError = error) }
    }

    private suspend fun <T> act(
        notice: PeopleNotice,
        name: String,
        block: suspend () -> ApiResult<T>,
    ) {
        if (!begin()) return
        when (val result = block()) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(busy = false, notice = notice, noticeName = name) }
                reload()
            }
            is ApiResult.Failed -> fail(result.error)
        }
    }
}
