package ch.orellbuehler.acolyte.feature.account

import ch.orellbuehler.acolyte.api.AccountEndpoints
import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.PeopleEndpoints
import ch.orellbuehler.acolyte.api.model.AccountSession
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AccountDevicesState(
    val status: LoadStatus = LoadStatus(),
    val sessions: List<AccountSession> = emptyList(),
    val busy: Boolean = false,
    val ended: Int? = null,
    val actionError: ApiError? = null,
) {
    val ordered: List<AccountSession>
        get() = sessions.sortedWith(compareByDescending<AccountSession> { it.current }.thenByDescending { it.lastUsedAt })

    val hasOthers: Boolean get() = sessions.any { !it.current }
}

class AccountDevicesPresenter internal constructor(
    private val api: Api,
    private val userId: Long?,
) {
    private val mutableState = MutableStateFlow(AccountDevicesState())
    private val lock = Mutex()

    val state: StateFlow<AccountDevicesState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            val result =
                if (userId == null) api.result(AccountEndpoints.sessions) else api.result(PeopleEndpoints.sessions, "id" to userId)
            when (result) {
                is ApiResult.Ok -> mutableState.update { it.copy(status = LoadStatus.ready, sessions = result.value.sessions) }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    fun consume() {
        mutableState.update { it.copy(ended = null, actionError = null) }
    }

    suspend fun end(session: AccountSession) {
        if (!begin()) return
        val result =
            if (userId == null) {
                api.result(AccountEndpoints.endSession, "sid" to session.id)
            } else {
                api.result(PeopleEndpoints.endSession, "id" to userId, "sid" to session.id)
            }
        when (result) {
            is ApiResult.Ok -> mutableState.update { it.copy(busy = false, sessions = it.sessions - session, ended = 1) }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, actionError = result.error) }
        }
    }

    suspend fun endAll() {
        if (!begin()) return
        val result = if (userId == null) api.result(AccountEndpoints.endOthers) else api.result(PeopleEndpoints.endAll, "id" to userId)
        when (result) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(busy = false, ended = result.value.ended) }
                refresh()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, actionError = result.error) }
        }
    }

    private fun begin(): Boolean {
        if (mutableState.value.busy) return false
        mutableState.update { it.copy(busy = true, actionError = null, ended = null) }
        return true
    }
}
