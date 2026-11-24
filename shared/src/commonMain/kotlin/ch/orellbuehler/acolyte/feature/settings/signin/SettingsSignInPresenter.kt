package ch.orellbuehler.acolyte.feature.settings.signin

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.PeopleEndpoints
import ch.orellbuehler.acolyte.api.SettingsSignInEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.AccountAddressAnswer
import ch.orellbuehler.acolyte.api.model.AccountAddressTest
import ch.orellbuehler.acolyte.api.model.AccountImportBody
import ch.orellbuehler.acolyte.api.model.AccountImportPick
import ch.orellbuehler.acolyte.api.model.AccountImportPreview
import ch.orellbuehler.acolyte.api.model.SettingsSignInChange
import ch.orellbuehler.acolyte.api.model.SettingsSignInPlexFinish
import ch.orellbuehler.acolyte.api.model.SettingsSignInPlexFinished
import ch.orellbuehler.acolyte.api.model.SettingsSignInPlexOwnerWire
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoSave
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoTest
import ch.orellbuehler.acolyte.api.model.SettingsSignInSsoView
import ch.orellbuehler.acolyte.api.model.SettingsSignInWayChange
import ch.orellbuehler.acolyte.api.model.SettingsSignInWayWire
import ch.orellbuehler.acolyte.api.model.SettingsSignInWire
import ch.orellbuehler.acolyte.api.model.TitleUsers
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.SettingsReauthGate
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.SignInMethod
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class Block {
    MIN_LENGTH,
    PLEX,
    JELLYFIN,
    EMBY,
    LOCAL,
    HOSTS,
}

private val SettingsSignInWayKind.block: Block
    get() =
        when (this) {
            SettingsSignInWayKind.PLEX -> Block.PLEX
            SettingsSignInWayKind.JELLYFIN -> Block.JELLYFIN
            SettingsSignInWayKind.EMBY -> Block.EMBY
        }

private fun SettingsSignInWire.way(kind: SettingsSignInWayKind): SettingsSignInWayWire =
    when (kind) {
        SettingsSignInWayKind.PLEX -> plex
        SettingsSignInWayKind.JELLYFIN -> jellyfin
        SettingsSignInWayKind.EMBY -> emby
    }

private fun joinOf(wire: String): SettingsSignInJoin =
    SettingsSignInJoin.entries.firstOrNull { it.wire == wire } ?: SettingsSignInJoin.NOBODY

private fun SettingsSignInWay.fresh(wire: SettingsSignInWayWire): SettingsSignInWay =
    copy(
        on = wire.on,
        url = wire.url,
        serverId = wire.serverId,
        join = joinOf(wire.join),
        allow = wire.allow,
        savedJoin = joinOf(wire.join),
        saving = false,
    )

private fun SettingsSignInWay.matches(wire: SettingsSignInWayWire): Boolean =
    on == wire.on && url == wire.url && serverId == wire.serverId && join == joinOf(wire.join) && allow == wire.allow

private fun roleOf(role: String): SettingsSignInRole =
    when (role) {
        "owner" -> SettingsSignInRole.OWNER
        "admin" -> SettingsSignInRole.ADMIN
        else -> SettingsSignInRole.MEMBER
    }

private fun skipOffOf(wire: String): SettingsSignInSkipOff =
    when (wire) {
        "kid" -> SettingsSignInSkipOff.KID
        "person_gone" -> SettingsSignInSkipOff.PERSON_GONE
        else -> SettingsSignInSkipOff.NONE
    }

private fun appOf(key: String): String = SettingsSignInWayKind.entries.firstOrNull { it.key == key }?.app ?: key

private fun linkFailureOf(error: ApiError): SettingsSignInLinkFailure =
    when (error.code) {
        ApiErrorCode.NOT_FOUND -> SettingsSignInLinkFailure.ENDED
        ApiErrorCode.CONFLICT -> SettingsSignInLinkFailure.TAKEN
        ApiErrorCode.RATE_LIMITED -> SettingsSignInLinkFailure.TOO_MANY
        ApiErrorCode.FORBIDDEN -> SettingsSignInLinkFailure.NEEDS_PASSWORD
        else -> SettingsSignInLinkFailure.OTHER
    }

private fun describe(
    kind: SettingsSignInWayKind,
    probed: AccountAddressAnswer,
): String {
    val app = probed.product.ifEmpty { kind.app }
    val version = if (probed.version.isEmpty()) "" else " ${probed.version}"
    val name = if (probed.name.isEmpty()) "" else " (${probed.name})"
    return app + version + name
}

class SettingsSignInPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    reauth: suspend (String, String?) -> ApiError?,
) {
    private val mutableState = MutableStateFlow(SettingsSignInState())
    private val lock = Mutex()
    private val gate = SettingsReauthGate(reauth) { r -> mutableState.update { it.copy(reauth = r) } }
    private var snapshot: SettingsSignInWire? = null
    private var seq = 0
    private var linkId = 0L
    private var linkRound = 0
    private var linkWaited = 0L
    private var linkParked = false

    val state: StateFlow<SettingsSignInState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        val me = account()
        mutableState.update {
            it.copy(
                status = it.status.started(),
                isOwner = me?.can(Need.OWNER) == true,
                apiKey = me?.method == SignInMethod.API_KEY,
            )
        }
        gate.guard(onFailure = { error -> mutableState.update { it.copy(status = it.status.failed(error)) } }) { fetch() }
    }

    private suspend fun fetch(): ApiError? =
        lock.withLock {
            val owner = mutableState.value.isOwner
            coroutineScope {
                val people = async { if (owner) api.result(TitleSupportEndpoints.users) else null }
                val sso = async { probeSso() }
                when (val result = api.result(SettingsSignInEndpoints.settings)) {
                    is ApiResult.Ok -> {
                        took(result.value)
                        mutableState.update { it.copy(status = LoadStatus.ready) }
                        applyPeople(people.await())
                        sso.await()
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }

    private fun applyPeople(result: ApiResult<TitleUsers>?) {
        when (result) {
            null -> Unit
            is ApiResult.Ok ->
                editLocal {
                    it.copy(
                        people =
                            result.value.users
                                .filter { user -> user.state == "active" }
                                .map { user ->
                                    SettingsSignInPerson(user.id, user.name, roleOf(user.role), user.maxRating, user.avatar.orEmpty())
                                },
                        peopleError = null,
                    )
                }
            is ApiResult.Failed -> editLocal { it.copy(peopleError = result.error) }
        }
    }

    suspend fun retryPeople() {
        editLocal { it.copy(peopleError = null) }
        applyPeople(api.result(TitleSupportEndpoints.users))
    }

    private fun took(
        wire: SettingsSignInWire,
        saved: Set<Block> = emptySet(),
    ) {
        val old = snapshot
        snapshot = wire
        mutableState.update { s ->
            val ways =
                s.ways.map { way ->
                    val replace = old == null || way.kind.block in saved || way.matches(old.way(way.kind))
                    if (replace) way.fresh(wire.way(way.kind)) else way.copy(savedJoin = joinOf(wire.way(way.kind).join), saving = false)
                }
            val local = s.local
            val localReplace =
                old == null ||
                    Block.LOCAL in saved ||
                    (
                        local.networks == old.localNetworks.joinToString("\n") &&
                            local.proxies == old.trustedProxies.joinToString("\n") &&
                            local.person == old.localPerson
                    )
            val minReplace = old == null || Block.MIN_LENGTH in saved || s.minLengthText == old.minPasswordLength.toString()
            val hostsReplace = old == null || Block.HOSTS in saved || s.hosts.text == old.allowedHosts.joinToString("\n")
            s.copy(
                minLengthText = if (minReplace) wire.minPasswordLength.toString() else s.minLengthText,
                minLengthSaving = false,
                requireTwoFactor = wire.requireTwoFactorAdmins,
                requireTwoFactorSaving = false,
                forgotLink = wire.ownerResetLink,
                forgotLinkSaving = false,
                ways = ways,
                plex =
                    s.plex.copy(
                        linked = wire.plexOwner.linked,
                        name = wire.plexOwner.name,
                        canImport = wire.plexOwner.canImport,
                        unlinking = false,
                    ),
                local =
                    local.copy(
                        skip = wire.skipLoginLocalNetwork,
                        skipSaving = false,
                        saving = false,
                        saveError = if (localReplace) null else local.saveError,
                        networks = if (localReplace) wire.localNetworks.joinToString("\n") else local.networks,
                        proxies = if (localReplace) wire.trustedProxies.joinToString("\n") else local.proxies,
                        person = if (localReplace) wire.localPerson else local.person,
                        savedNetworks = wire.localNetworks,
                        savedProxies = wire.trustedProxies,
                        savedPerson = wire.localPerson,
                        fixedProxies = wire.trustedProxiesFixed,
                        homeRanges = wire.homeRanges,
                        cgnatRange = wire.cgnatRange.ifEmpty { local.cgnatRange },
                        seenAddress = wire.seenAddress,
                        gatewayNetworks = wire.gatewayNetworks,
                        skipOff = skipOffOf(wire.localSkipOff),
                        proxyUntrusted = wire.proxyUntrusted,
                        seenGateway = wire.seenGateway,
                        mediaApps = wire.mediaSignIn.map { appOf(it) },
                    ),
                hosts =
                    s.hosts.copy(
                        text = if (hostsReplace) wire.allowedHosts.joinToString("\n") else s.hosts.text,
                        saved = wire.allowedHosts,
                        saving = false,
                    ),
            )
        }
    }

    private fun restore(s: SettingsSignInState): SettingsSignInState {
        val wire = snapshot
        return s.copy(
            minLengthSaving = false,
            requireTwoFactor = wire?.requireTwoFactorAdmins ?: s.requireTwoFactor,
            requireTwoFactorSaving = false,
            forgotLink = wire?.ownerResetLink ?: s.forgotLink,
            forgotLinkSaving = false,
            ways = s.ways.map { it.copy(saving = false) },
            plex = s.plex.copy(unlinking = false),
            local = s.local.copy(skip = wire?.skipLoginLocalNetwork ?: s.local.skip, skipSaving = false, saving = false),
            hosts = s.hosts.copy(saving = false),
            sso =
                s.sso.copy(
                    saving = false,
                    testing = false,
                    phase = if (s.sso.phase == SettingsSignInSsoPhase.LOADING) SettingsSignInSsoPhase.LOCKED else s.sso.phase,
                ),
            importSheet = s.importSheet?.copy(busy = false),
        )
    }

    private fun notice(
        kind: SettingsSignInNoticeKind,
        name: String = "",
        count: Int = 0,
        count2: Int = 0,
        error: ApiError? = null,
    ) = mutableState.update { it.copy(notice = SettingsSignInNotice(++seq, kind, name, count, count2, error)) }

    fun dismissNotice(seq: Int) = mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }

    private fun failed(
        error: ApiError,
        kind: SettingsSignInNoticeKind = SettingsSignInNoticeKind.FAILED,
    ) {
        mutableState.update { restore(it) }
        notice(kind, error = error)
    }

    private fun edit(transform: (SettingsSignInState) -> SettingsSignInState) = mutableState.update(transform)

    private fun editWay(
        kind: SettingsSignInWayKind,
        transform: (SettingsSignInWay) -> SettingsSignInWay,
    ) = edit { s -> s.copy(ways = s.ways.map { if (it.kind == kind) transform(it) else it }) }

    private fun editLocal(transform: (SettingsSignInLocal) -> SettingsSignInLocal) = edit { it.copy(local = transform(it.local)) }

    private fun editSso(transform: (SettingsSignInSso) -> SettingsSignInSso) = edit { it.copy(sso = transform(it.sso)) }

    private fun editPlex(transform: (SettingsSignInPlex) -> SettingsSignInPlex) = edit { it.copy(plex = transform(it.plex)) }

    private fun editImport(transform: (SettingsSignInImport) -> SettingsSignInImport) =
        edit { s -> s.importSheet?.let { s.copy(importSheet = transform(it)) } ?: s }

    private suspend fun write(
        change: SettingsSignInChange,
        saved: Set<Block> = emptySet(),
        after: suspend (SettingsSignInWire) -> Unit = {},
    ): ApiError? =
        lock.withLock {
            when (val result = api.result(SettingsSignInEndpoints.change, change)) {
                is ApiResult.Ok -> {
                    took(result.value, saved)
                    after(result.value)
                    null
                }
                is ApiResult.Failed -> result.error
            }
        }

    private val owner: Boolean get() = mutableState.value.isOwner

    suspend fun confirmReauth(
        password: String,
        code: String,
    ) {
        linkParked = false
        gate.confirm(password, code)
        val s = mutableState.value
        if (linkId != 0L && s.plex.phase == SettingsSignInLinkPhase.WAITING && !s.reauth.needed) followLink()
    }

    fun cancelReauth() {
        gate.cancel()
        mutableState.update { s ->
            val restored = restore(s)
            val loading = restored.status.phase == Phase.LOADING
            val status = if (loading) restored.status.failed(ApiError(ApiErrorCode.REAUTH, 403)) else restored.status
            val plex = if (linkParked) restored.plex.copy(phase = SettingsSignInLinkPhase.IDLE) else restored.plex
            restored.copy(status = status, plex = plex)
        }
        if (linkParked) {
            linkParked = false
            linkRound++
            linkId = 0
        }
    }

    fun setMinLength(text: String) = edit { it.copy(minLengthText = text.filter { c -> c.isDigit() }.take(2)) }

    suspend fun saveMinLength() {
        val value = mutableState.value.minLengthValue
        if (!mutableState.value.canSaveMinLength || value == null) return
        edit { it.copy(minLengthSaving = true) }
        gate.guard(onFailure = { failed(it) }) {
            write(SettingsSignInChange(minPasswordLength = value), setOf(Block.MIN_LENGTH)) {
                notice(SettingsSignInNoticeKind.MIN_SAVED, count = value)
            }
        }
    }

    suspend fun setRequireTwoFactor(on: Boolean) {
        val s = mutableState.value
        if (!s.isOwner || s.requireTwoFactorSaving) return
        edit { it.copy(requireTwoFactor = on, requireTwoFactorSaving = true) }
        gate.guard(onFailure = { failed(it) }) {
            write(SettingsSignInChange(requireTwoFactorAdmins = on)) {
                notice(if (on) SettingsSignInNoticeKind.TWO_FACTOR_REQUIRED else SettingsSignInNoticeKind.TWO_FACTOR_OPTIONAL)
            }
        }
    }

    suspend fun setForgotLink(on: Boolean) {
        val s = mutableState.value
        if (!s.isOwner || s.forgotLinkSaving) return
        edit { it.copy(forgotLink = on, forgotLinkSaving = true) }
        gate.guard(onFailure = { failed(it) }) {
            write(SettingsSignInChange(ownerResetLink = on)) {
                notice(if (on) SettingsSignInNoticeKind.FORGOT_SHOWN else SettingsSignInNoticeKind.FORGOT_HIDDEN)
            }
        }
    }

    fun setWayOn(
        kind: SettingsSignInWayKind,
        on: Boolean,
    ) = editWay(kind) { it.copy(on = on) }

    fun setWayUrl(
        kind: SettingsSignInWayKind,
        text: String,
    ) = editWay(kind) { it.copy(url = text.take(MAX_URL), test = null) }

    fun setWayServerId(
        kind: SettingsSignInWayKind,
        text: String,
    ) = editWay(kind) { it.copy(serverId = text.take(MAX_SERVER_ID)) }

    fun setWayJoin(
        kind: SettingsSignInWayKind,
        join: SettingsSignInJoin,
    ) = editWay(kind) { it.copy(join = join) }

    fun setWayAllow(
        kind: SettingsSignInWayKind,
        text: String,
    ) = editWay(kind) { it.copy(allow = text.take(MAX_ALLOW)) }

    suspend fun testWay(kind: SettingsSignInWayKind) {
        val way = mutableState.value.way(kind)
        if (kind == SettingsSignInWayKind.PLEX || way.url.isBlank() || way.testing) return
        runTest(kind)
    }

    private suspend fun runTest(kind: SettingsSignInWayKind) {
        editWay(kind) { it.copy(testing = true, test = null) }
        val url =
            mutableState.value
                .way(kind)
                .url
                .trim()
        val result =
            when (val probed = api.result(PeopleEndpoints.testAddress, AccountAddressTest(url), "kind" to kind.key)) {
                is ApiResult.Ok ->
                    if (probed.value.ok) {
                        SettingsSignInTest(true, describe(kind, probed.value))
                    } else {
                        SettingsSignInTest(false, probed.value.message)
                    }
                is ApiResult.Failed -> SettingsSignInTest(false, error = probed.error)
            }
        editWay(kind) { it.copy(testing = false, test = result) }
    }

    suspend fun saveWay(
        kind: SettingsSignInWayKind,
        confirmed: Boolean = false,
    ) {
        val s = mutableState.value
        val way = s.way(kind)
        if (!s.isOwner || way.saving) return
        if (way.join == SettingsSignInJoin.EVERYONE && way.savedJoin != SettingsSignInJoin.EVERYONE && !confirmed) {
            val who =
                when {
                    kind != SettingsSignInWayKind.PLEX -> SettingsSignInEveryoneWho.SERVER
                    way.serverId.isNotEmpty() -> SettingsSignInEveryoneWho.SHARED
                    else -> SettingsSignInEveryoneWho.EVERY_PLEX
                }
            edit { it.copy(everyoneAsk = SettingsSignInEveryoneAsk(kind, who, way.lastCount)) }
            return
        }
        edit { it.copy(everyoneAsk = null) }
        editWay(kind) { it.copy(saving = true) }
        val body = wayChange(kind, way, confirmed)
        gate.guard(onFailure = { failed(it) }) {
            write(body, setOf(kind.block)) {
                val saved = mutableState.value.way(kind)
                if (kind != SettingsSignInWayKind.PLEX && saved.url.isNotEmpty() && saved.test?.ok != true) {
                    runTest(kind)
                    if (mutableState.value
                            .way(kind)
                            .test
                            ?.ok != true
                    ) {
                        notice(SettingsSignInNoticeKind.WAY_UNREACHABLE, name = kind.app)
                        return@write
                    }
                }
                notice(SettingsSignInNoticeKind.WAY_SAVED, name = kind.app)
            }
        }
    }

    private fun wayChange(
        kind: SettingsSignInWayKind,
        way: SettingsSignInWay,
        confirmed: Boolean,
    ): SettingsSignInChange {
        val plex = kind == SettingsSignInWayKind.PLEX
        val change =
            SettingsSignInWayChange(
                on = way.on,
                url = if (plex) null else way.url,
                serverId = if (plex) way.serverId else null,
                join = way.join.wire,
                allow = way.allow,
            )
        val confirm = if (confirmed) true else null
        return when (kind) {
            SettingsSignInWayKind.PLEX -> SettingsSignInChange(plex = change, confirmEveryone = confirm)
            SettingsSignInWayKind.JELLYFIN -> SettingsSignInChange(jellyfin = change, confirmEveryone = confirm)
            SettingsSignInWayKind.EMBY -> SettingsSignInChange(emby = change, confirmEveryone = confirm)
        }
    }

    fun dismissEveryone() = edit { it.copy(everyoneAsk = null) }

    suspend fun linkPlex() {
        if (mutableState.value.plex.linking || !owner) return
        linkId = 0
        editPlex { it.copy(phase = SettingsSignInLinkPhase.STARTING, error = null, servers = emptyList()) }
        gate.guard(onFailure = { linkFailed(it) }) { startLink() }
        followLink()
    }

    private suspend fun startLink(): ApiError? =
        when (val result = api.result(SettingsSignInEndpoints.plexStart)) {
            is ApiResult.Ok -> {
                linkId = result.value.id
                linkRound++
                linkWaited = 0
                editPlex {
                    it.copy(
                        phase = SettingsSignInLinkPhase.WAITING,
                        authUrl = result.value.authUrl,
                        openCount = it.openCount + 1,
                        error = null,
                    )
                }
                null
            }
            is ApiResult.Failed -> parked(result.error)
        }

    private fun parked(error: ApiError): ApiError {
        if (error.code == ApiErrorCode.REAUTH) linkParked = true
        return error
    }

    private suspend fun followLink() {
        val id = linkId
        if (id == 0L || mutableState.value.reauth.needed) return
        val round = linkRound
        while (linkWaited < LINK_LIMIT_MILLIS) {
            delay(LINK_POLL_MILLIS)
            linkWaited += LINK_POLL_MILLIS
            if (round != linkRound) return
            val failure = checkLink(id, round)
            when {
                failure == null || round != linkRound -> Unit
                failure.code == ApiErrorCode.REAUTH -> {
                    gate.guard(onFailure = { error -> if (round == linkRound) linkFailed(error) }) { checkLink(id, round) }
                    if (!mutableState.value.reauth.needed) linkParked = false
                }
                else -> linkFailed(failure)
            }
            val s = mutableState.value
            if (round != linkRound || s.plex.phase != SettingsSignInLinkPhase.WAITING || s.reauth.needed) return
        }
        if (round == linkRound) {
            linkId = 0
            editPlex { it.copy(phase = SettingsSignInLinkPhase.TIMED_OUT) }
        }
    }

    private suspend fun checkLink(
        id: Long,
        round: Int,
    ): ApiError? =
        when (val result = api.result(SettingsSignInEndpoints.plexFinish, SettingsSignInPlexFinish(id))) {
            is ApiResult.Ok -> {
                if (round == linkRound && !result.value.waiting) linked(result.value)
                null
            }
            is ApiResult.Failed ->
                when (result.error.code) {
                    ApiErrorCode.RATE_LIMITED, ApiErrorCode.OFFLINE, ApiErrorCode.TIMEOUT -> null
                    else -> parked(result.error)
                }
        }

    private fun linked(done: SettingsSignInPlexFinished) {
        linkId = 0
        val servers = done.servers.map { SettingsSignInPlexServer(it.id, it.name) }
        edit { s ->
            s.copy(
                ways =
                    s.ways.map { way ->
                        if (way.kind == SettingsSignInWayKind.PLEX && way.serverId.isEmpty() && servers.size == 1) {
                            way.copy(serverId = servers[0].id)
                        } else {
                            way
                        }
                    },
                plex =
                    s.plex.copy(
                        linked = true,
                        name = done.name,
                        canImport = true,
                        phase = if (servers.isEmpty()) SettingsSignInLinkPhase.LINKED_NONE else SettingsSignInLinkPhase.LINKED_PICK,
                        servers = servers,
                        error = null,
                    ),
            )
        }
        snapshot = snapshot?.copy(plexOwner = SettingsSignInPlexOwnerWire(linked = true, name = done.name, canImport = true))
        notice(SettingsSignInNoticeKind.PLEX_LINKED, name = done.name)
    }

    private fun linkFailed(error: ApiError) {
        linkId = 0
        editPlex { it.copy(phase = SettingsSignInLinkPhase.FAILED, failure = linkFailureOf(error), error = error) }
    }

    fun cancelLink() {
        linkRound++
        linkId = 0
        editPlex { if (it.linking) it.copy(phase = SettingsSignInLinkPhase.IDLE) else it }
    }

    suspend fun unlinkPlex() {
        val s = mutableState.value
        if (!s.isOwner || s.plex.unlinking) return
        editPlex { it.copy(unlinking = true) }
        gate.guard(onFailure = { failed(it, SettingsSignInNoticeKind.UNLINK_FAILED) }) {
            lock.withLock {
                when (val result = api.result(SettingsSignInEndpoints.plexUnlink)) {
                    is ApiResult.Ok -> {
                        snapshot = snapshot?.copy(plexOwner = result.value)
                        editPlex {
                            SettingsSignInPlex(
                                linked = result.value.linked,
                                name = result.value.name,
                                canImport = result.value.canImport,
                                authUrl = it.authUrl,
                                openCount = it.openCount,
                            )
                        }
                        notice(SettingsSignInNoticeKind.PLEX_UNLINKED)
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    suspend fun openImport(kind: SettingsSignInWayKind) {
        val s = mutableState.value
        if (!s.isOwner || !s.canImport(kind)) return
        val plex = kind == SettingsSignInWayKind.PLEX
        edit {
            it.copy(
                importSheet =
                    SettingsSignInImport(kind, if (plex) SettingsSignInImportPhase.LOADING else SettingsSignInImportPhase.ASK),
            )
        }
        if (plex) previewImport("", "")
    }

    fun closeImport() = edit { it.copy(importSheet = null) }

    suspend fun previewImport(
        name: String,
        password: String,
    ) {
        val sheet = mutableState.value.importSheet ?: return
        val plex = sheet.kind == SettingsSignInWayKind.PLEX
        editImport { it.copy(phase = SettingsSignInImportPhase.LOADING, error = null) }
        val body = if (plex) AccountImportPreview() else AccountImportPreview(name = name.trim(), password = password)
        when (val result = api.result(PeopleEndpoints.importPreview, body, "kind" to sheet.kind.key)) {
            is ApiResult.Ok -> {
                val people =
                    result.value.people.map {
                        SettingsSignInCandidate(
                            id = it.id,
                            name = it.name,
                            email = it.email,
                            kid = it.kid,
                            serverAdmin = it.serverAdmin,
                            linkedTo = it.linkedTo,
                            picked = it.linkedTo.isEmpty(),
                            asKid = it.kid,
                        )
                    }
                editImport { it.copy(phase = SettingsSignInImportPhase.READY, people = people) }
                editWay(sheet.kind) { it.copy(lastCount = people.size) }
            }
            is ApiResult.Failed -> editImport { it.copy(phase = SettingsSignInImportPhase.ASK, error = result.error) }
        }
    }

    fun togglePerson(id: String) =
        editImport { sheet ->
            sheet.copy(people = sheet.people.map { if (it.id == id && !it.locked) it.copy(picked = !it.picked) else it })
        }

    fun toggleKid(id: String) =
        editImport { sheet ->
            sheet.copy(people = sheet.people.map { if (it.id == id && !it.locked && it.picked) it.copy(asKid = !it.asKid) else it })
        }

    fun dismissConfirmAdults() = editImport { it.copy(confirmAdults = emptyList()) }

    suspend fun runImport(confirmAdults: Boolean = false) {
        val sheet = mutableState.value.importSheet ?: return
        if (sheet.busy) return
        val picks = sheet.people.filter { !it.locked && it.picked }
        if (picks.isEmpty()) return
        val adults = picks.filter { it.kid && !it.asKid }
        if (adults.isNotEmpty() && !confirmAdults) {
            editImport { it.copy(confirmAdults = adults.map { person -> person.name }) }
            return
        }
        editImport { it.copy(busy = true, confirmAdults = emptyList(), error = null) }
        val body = AccountImportBody(picks.map { AccountImportPick(it.id, it.asKid, if (it.kid && !it.asKid) true else null) })
        gate.guard(onFailure = { error -> editImport { it.copy(busy = false, error = error) } }) {
            lock.withLock {
                when (val result = api.result(PeopleEndpoints.importPeople, body, "kind" to sheet.kind.key)) {
                    is ApiResult.Ok -> {
                        edit { it.copy(importSheet = null) }
                        val made = result.value.made
                        if (made.isEmpty()) {
                            notice(SettingsSignInNoticeKind.IMPORT_NONE)
                        } else {
                            notice(SettingsSignInNoticeKind.IMPORTED, name = made.joinToString(", "), count = made.size)
                        }
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    private suspend fun probeSso() {
        val s = mutableState.value
        when {
            !s.isOwner -> editSso { it.copy(phase = SettingsSignInSsoPhase.HIDDEN) }
            s.apiKey -> editSso { it.copy(phase = SettingsSignInSsoPhase.NEEDS_SESSION) }
            s.sso.phase == SettingsSignInSsoPhase.READY -> Unit
            else ->
                when (val result = api.result(SettingsSignInEndpoints.sso)) {
                    is ApiResult.Ok -> showSso(result.value)
                    is ApiResult.Failed ->
                        if (result.error.code == ApiErrorCode.REAUTH) {
                            editSso { it.copy(phase = SettingsSignInSsoPhase.LOCKED, loadError = null) }
                        } else {
                            editSso { it.copy(phase = SettingsSignInSsoPhase.FAILED, loadError = result.error) }
                        }
                }
        }
    }

    private suspend fun showSso(view: SettingsSignInSsoView) {
        val twoFactor = if (view.source == "settings") api.result(AuthEndpoints.me).valueOrNull()?.twoFactor else null
        editSso { filled(it, view).copy(phase = SettingsSignInSsoPhase.READY, loadError = null, twoFactor = twoFactor) }
    }

    private fun filled(
        sso: SettingsSignInSso,
        view: SettingsSignInSsoView,
    ): SettingsSignInSso =
        sso.copy(
            view = view,
            on = view.on,
            issuer = view.issuer,
            clientId = view.clientId,
            secret = "",
            publicClient = !view.secretSet && view.issuer.isNotEmpty(),
            allowJoin = view.allowJoin,
            groupsClaim = view.groupsClaim,
            groupRoles = view.groupRoles,
            confirmEveryone = false,
            saving = false,
            saveError = null,
            error = null,
        )

    suspend fun loadSso() {
        val s = mutableState.value
        if (!s.isOwner || s.apiKey || s.sso.phase == SettingsSignInSsoPhase.LOADING) return
        editSso { it.copy(phase = SettingsSignInSsoPhase.LOADING, loadError = null) }
        gate.guard(onFailure = { error -> editSso { it.copy(phase = SettingsSignInSsoPhase.FAILED, loadError = error) } }) {
            when (val result = api.result(SettingsSignInEndpoints.sso)) {
                is ApiResult.Ok -> {
                    showSso(result.value)
                    null
                }
                is ApiResult.Failed -> result.error
            }
        }
    }

    fun setSsoOn(on: Boolean) = editSso { it.copy(on = on) }

    fun setSsoIssuer(text: String) = editSso { it.copy(issuer = text.take(MAX_SSO_FIELD), test = null) }

    fun setSsoClientId(text: String) = editSso { it.copy(clientId = text.take(MAX_SSO_FIELD)) }

    fun setSsoSecret(text: String) = editSso { it.copy(secret = text.take(MAX_SECRET)) }

    fun setSsoPublicClient(on: Boolean) = editSso { it.copy(publicClient = on) }

    fun setSsoAllowJoin(on: Boolean) = editSso { it.copy(allowJoin = on) }

    fun setSsoGroupsClaim(text: String) = editSso { it.copy(groupsClaim = text.take(MAX_CLAIM)) }

    fun setSsoGroupRoles(text: String) = editSso { it.copy(groupRoles = text.take(MAX_ROLES)) }

    fun setSsoConfirmEveryone(on: Boolean) = editSso { it.copy(confirmEveryone = on) }

    suspend fun testSso() {
        val sso = mutableState.value.sso
        val issuer = sso.issuer.trim()
        if (issuer.isEmpty() || sso.testing || sso.phase != SettingsSignInSsoPhase.READY) return
        editSso { it.copy(testing = true, test = null) }
        gate.guard(onFailure = { error -> editSso { it.copy(testing = false, test = SettingsSignInTest(false, error = error)) } }) {
            when (val result = api.result(SettingsSignInEndpoints.testSso, SettingsSignInSsoTest(issuer))) {
                is ApiResult.Ok -> {
                    editSso { it.copy(testing = false, test = SettingsSignInTest(result.value.ok, issuer)) }
                    null
                }
                is ApiResult.Failed -> result.error
            }
        }
    }

    suspend fun saveSso(
        password: String,
        code: String,
    ) {
        val s = mutableState.value
        val sso = s.sso
        if (!s.isOwner || sso.environment || sso.saving || sso.phase != SettingsSignInSsoPhase.READY || password.isEmpty()) return
        if (sso.mustRetype) {
            editSso { it.copy(saveError = SettingsSignInSsoError.RETYPE, error = null) }
            return
        }
        if (sso.opening && !sso.confirmEveryone) {
            editSso { it.copy(saveError = SettingsSignInSsoError.CONFIRM_BOX, error = null) }
            return
        }
        editSso { it.copy(saving = true, saveError = null, error = null) }
        val body =
            SettingsSignInSsoSave(
                password = password,
                code = code.trim().ifEmpty { null },
                on = sso.on,
                issuer = sso.issuer.trim(),
                clientId = sso.clientId.trim(),
                clientSecret = sso.secret.ifEmpty { null },
                publicClient = sso.publicClient,
                redirectUrl = sso.callback,
                allowJoin = sso.allowJoin,
                confirmEveryone = sso.confirmEveryone,
                groupsClaim = sso.groupsClaim.trim(),
                groupRoles = sso.groupRoles.trim(),
            )
        gate.guard(onFailure = { error -> ssoSaveFailed(error) }) {
            lock.withLock {
                when (val result = api.result(SettingsSignInEndpoints.saveSso, body)) {
                    is ApiResult.Ok -> {
                        editSso { filled(it, result.value.sso) }
                        notice(SettingsSignInNoticeKind.SSO_SAVED, count = result.value.linksRemoved, count2 = result.value.sessionsEnded)
                        null
                    }
                    is ApiResult.Failed -> result.error
                }
            }
        }
    }

    private fun ssoSaveFailed(error: ApiError) {
        val kind =
            when {
                error.status == HTTP_FORBIDDEN && error.serverCode == WRONG_PROOF -> SettingsSignInSsoError.WRONG_PROOF
                error.serverCode == GROUP_ROLES -> SettingsSignInSsoError.GROUP_ROLES
                else -> SettingsSignInSsoError.OTHER
            }
        editSso { it.copy(saving = false, saveError = kind, error = error) }
    }

    suspend fun setSkip(on: Boolean) {
        val s = mutableState.value
        if (!s.isOwner || s.local.skipSaving) return
        editLocal { it.copy(skip = on, skipSaving = true) }
        val body =
            if (on) {
                SettingsSignInChange(
                    skipLoginLocalNetwork = true,
                    localNetworks = SettingsSignInLines.lines(s.local.networks),
                    localPerson = s.local.person,
                )
            } else {
                SettingsSignInChange(skipLoginLocalNetwork = false)
            }
        gate.guard(onFailure = { failed(it) }) {
            write(body) { notice(if (on) SettingsSignInNoticeKind.LOCAL_ON else SettingsSignInNoticeKind.LOCAL_OFF) }
        }
    }

    fun setLocalNetworks(text: String) = editLocal { it.copy(networks = text) }

    fun setLocalProxies(text: String) = editLocal { it.copy(proxies = text) }

    fun pickPerson(id: Long) = editLocal { it.copy(person = if (id == it.ownerId) 0 else id) }

    fun addHomeRanges() =
        editLocal { local ->
            val have = SettingsSignInLines.lines(local.networks)
            val add = local.homeRanges.filter { it !in have }
            if (add.isEmpty()) local else local.copy(networks = (have + add).joinToString("\n"))
        }

    fun setTailscale(on: Boolean) =
        editLocal { local ->
            val have = SettingsSignInLines.lines(local.networks).filter { it != local.cgnatRange }
            local.copy(networks = (if (on) have + local.cgnatRange else have).joinToString("\n"))
        }

    suspend fun saveLocal() {
        val s = mutableState.value
        val local = s.local
        if (!s.isOwner || !local.canSave) return
        editLocal { it.copy(saving = true, saveError = null) }
        val proxiesChanged = local.proxies.trim() != local.savedProxies.joinToString("\n").trim()
        val body =
            SettingsSignInChange(
                localNetworks = SettingsSignInLines.lines(local.networks),
                localPerson = local.person,
                trustedProxies = if (proxiesChanged && !s.apiKey) SettingsSignInLines.lines(local.proxies) else null,
            )
        gate.guard(onFailure = { error -> editLocal { it.copy(saving = false, saveError = error) } }) {
            write(body, setOf(Block.LOCAL)) { notice(SettingsSignInNoticeKind.LOCAL_SAVED) }
        }
    }

    fun setHosts(text: String) = edit { it.copy(hosts = it.hosts.copy(text = text)) }

    suspend fun saveHosts() {
        val s = mutableState.value
        if (!s.isOwner || s.apiKey || s.hosts.saving) return
        edit { it.copy(hosts = it.hosts.copy(saving = true)) }
        gate.guard(onFailure = { failed(it) }) {
            write(SettingsSignInChange(allowedHosts = SettingsSignInLines.lines(s.hosts.text)), setOf(Block.HOSTS)) {
                notice(SettingsSignInNoticeKind.HOSTS_SAVED)
            }
        }
    }

    private companion object {
        const val MAX_URL = 500
        const val MAX_SERVER_ID = 200
        const val MAX_ALLOW = 4000
        const val MAX_SSO_FIELD = 1000
        const val MAX_SECRET = 4096
        const val MAX_CLAIM = 100
        const val MAX_ROLES = 4000
        const val LINK_POLL_MILLIS = 2_000L
        const val LINK_LIMIT_MILLIS = 10 * 60 * 1000L
        const val HTTP_FORBIDDEN = 403
        const val WRONG_PROOF = "wrong_proof"
        const val GROUP_ROLES = "group_roles"
    }
}
