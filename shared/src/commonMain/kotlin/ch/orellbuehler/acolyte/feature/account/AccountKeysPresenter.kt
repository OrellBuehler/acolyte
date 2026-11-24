package ch.orellbuehler.acolyte.feature.account

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.PeopleEndpoints
import ch.orellbuehler.acolyte.api.model.AccountAddressAnswer
import ch.orellbuehler.acolyte.api.model.AccountAddressTest
import ch.orellbuehler.acolyte.api.model.AccountImportBody
import ch.orellbuehler.acolyte.api.model.AccountImportPerson
import ch.orellbuehler.acolyte.api.model.AccountImportPick
import ch.orellbuehler.acolyte.api.model.AccountImportPreview
import ch.orellbuehler.acolyte.api.model.AccountKey
import ch.orellbuehler.acolyte.api.model.AccountKeyChange
import ch.orellbuehler.acolyte.api.model.AccountKeyMade
import ch.orellbuehler.acolyte.api.model.AccountKeyMake
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val ACCOUNT_AI_GROUPS: List<String> = listOf("see", "add", "control", "settings", "delete", "admin", "approve")

val ACCOUNT_AI_DEFAULT_GROUPS: List<String> = listOf("see", "add", "control", "settings")

data class AccountKeysState(
    val status: LoadStatus = LoadStatus(),
    val keys: List<AccountKey> = emptyList(),
    val made: AccountKeyMade? = null,
    val busy: Boolean = false,
    val error: ApiError? = null,
)

class AccountKeysPresenter internal constructor(
    private val api: Api,
    val reauth: ReauthGate,
    private val ai: Boolean,
) {
    private val mutableState = MutableStateFlow(AccountKeysState())
    private val lock = Mutex()

    val state: StateFlow<AccountKeysState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            when (val result = api.result(PeopleEndpoints.keys)) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            keys = result.value.keys.filter { key -> ("mcp" in key.scopes) == ai },
                        )
                    }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    fun dismissMade() {
        mutableState.update { it.copy(made = null) }
    }

    fun consume() {
        mutableState.update { it.copy(error = null) }
    }

    suspend fun make(
        label: String,
        groups: List<String> = ACCOUNT_AI_DEFAULT_GROUPS,
    ) {
        val trimmed = label.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LABEL || mutableState.value.busy) return
        val body =
            if (ai) {
                AccountKeyMake(trimmed, listOf("mcp"), ACCOUNT_AI_GROUPS.filter { it in groups })
            } else {
                AccountKeyMake(trimmed)
            }
        mutableState.update { it.copy(busy = true, error = null) }
        when (val result = reauth.guard { api.result(PeopleEndpoints.makeKey, body) }) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(busy = false, made = result.value) }
                refresh()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }

    suspend fun revoke(key: AccountKey) {
        change { reauth.guard { api.result(PeopleEndpoints.revokeKey, "id" to key.id) } }
    }

    suspend fun setGroups(
        key: AccountKey,
        groups: List<String>,
    ) {
        change {
            reauth.guard {
                api.result(
                    PeopleEndpoints.changeKey,
                    AccountKeyChange(ACCOUNT_AI_GROUPS.filter { it in groups }),
                    "id" to key.id,
                )
            }
        }
    }

    private suspend fun <T> change(block: suspend () -> ApiResult<T>) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        when (val result = block()) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(busy = false) }
                refresh()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }

    private companion object {
        const val MAX_LABEL = 64
    }
}

data class AccountImportState(
    val people: List<AccountImportPerson> = emptyList(),
    val picked: Set<String> = emptySet(),
    val kids: Set<String> = emptySet(),
    val previewed: Boolean = false,
    val busy: Boolean = false,
    val made: List<String>? = null,
    val skipped: Int = 0,
    val error: ApiError? = null,
) {
    val importable: List<AccountImportPerson> get() = people.filter { it.linkedTo.isEmpty() }
}

class AccountImportPresenter internal constructor(
    private val api: Api,
    val reauth: ReauthGate,
    private val kind: String,
) {
    private val mutableState = MutableStateFlow(AccountImportState())

    val state: StateFlow<AccountImportState> = mutableState.asStateFlow()

    suspend fun preview(
        adminName: String,
        adminPassword: String,
    ) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        val body = if (kind == "plex") AccountImportPreview() else AccountImportPreview(adminName.trim(), adminPassword)
        when (val result = api.result(PeopleEndpoints.importPreview, body, "kind" to kind)) {
            is ApiResult.Ok -> {
                val people = result.value.people
                mutableState.update {
                    it.copy(
                        busy = false,
                        previewed = true,
                        people = people,
                        picked = people.filter { p -> p.linkedTo.isEmpty() && !p.serverAdmin }.map { p -> p.id }.toSet(),
                        kids = people.filter { p -> p.kid }.map { p -> p.id }.toSet(),
                        made = null,
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }

    fun toggle(id: String) {
        mutableState.update { it.copy(picked = if (id in it.picked) it.picked - id else it.picked + id) }
    }

    fun toggleKid(id: String) {
        mutableState.update { it.copy(kids = if (id in it.kids) it.kids - id else it.kids + id) }
    }

    suspend fun import() {
        val current = mutableState.value
        if (current.busy || current.picked.isEmpty()) return
        val picks =
            current.people.filter { it.id in current.picked }.map { person ->
                val kid = person.id in current.kids
                AccountImportPick(person.id, kid, if (!kid && person.kid) true else null)
            }
        mutableState.update { it.copy(busy = true, error = null) }
        when (val result = reauth.guard { api.result(PeopleEndpoints.importPeople, AccountImportBody(picks), "kind" to kind) }) {
            is ApiResult.Ok -> mutableState.update { it.copy(busy = false, made = result.value.made, skipped = result.value.skipped) }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }

    suspend fun test(
        serverKind: String,
        url: String,
    ): AccountAddressAnswer? =
        when (val result = api.result(PeopleEndpoints.testAddress, AccountAddressTest(url.trim()), "kind" to serverKind)) {
            is ApiResult.Ok -> result.value
            is ApiResult.Failed -> AccountAddressAnswer(ok = false, message = result.error.message)
        }
}
