package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.model.InboxMessage
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InboxState(
    val unread: Int = 0,
    val waiting: Int = 0,
    val reportsOpen: Int = 0,
    val approver: Boolean = false,
    val messages: List<InboxMessage> = emptyList(),
) {
    val requestsBadge: Int get() = if (approver) waiting else unread

    val hasAttention: Boolean get() = approver && waiting > 0

    internal val unreadAskIds: Set<Long>
        get() = messages.filter { it.readAt.isEmpty() && it.askId > 0 }.map { it.askId }.toSet()
}

class RequestsInbox internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(InboxState())

    val state: StateFlow<InboxState> = mutableState.asStateFlow()

    fun badge(section: Section): Int = if (section == Section.REQUESTS) mutableState.value.requestsBadge else 0

    suspend fun refresh() {
        val me = account()
        if (me == null || !me.shows(Section.REQUESTS)) {
            mutableState.value = InboxState()
            return
        }
        api.result(RequestsEndpoints.messages).onSuccess { answer ->
            mutableState.value =
                InboxState(
                    unread = answer.unread,
                    waiting = answer.waiting,
                    reportsOpen = answer.reportsOpen,
                    approver = me.can(Need.APPROVE),
                    messages = answer.messages,
                )
        }
    }

    suspend fun poll() {
        while (true) {
            refresh()
            delay(POLL_MILLIS)
        }
    }

    internal suspend fun markRead(): Set<Long> {
        val current = mutableState.value
        if (current.unread <= 0) return emptySet()
        val ids = current.unreadAskIds
        api.result(RequestsEndpoints.readMessages).onSuccess {
            mutableState.update { it.copy(unread = 0) }
        }
        return ids
    }

    private companion object {
        const val POLL_MILLIS = 60_000L
    }
}
