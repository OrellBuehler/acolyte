package ch.orellbuehler.acolyte.tv.requests

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.RequestsState
import ch.orellbuehler.acolyte.feature.requests.isFailed
import ch.orellbuehler.acolyte.feature.requests.isWaiting
import ch.orellbuehler.acolyte.ui.requests.approverTabLabel
import ch.orellbuehler.acolyte.ui.requests.memberTabLabel

enum class TvAskAction {
    OPEN,
    APPROVE,
    DECLINE,
    TAKE_BACK,
    UNDO,
}

fun Ask.canOpen(): Boolean = kind != "artist" && tmdbId.isNotEmpty()

fun tvAskActions(
    ask: Ask,
    state: RequestsState,
): List<TvAskAction> {
    if (state.isPending(ask.id)) return listOf(TvAskAction.UNDO)
    val actions = mutableListOf<TvAskAction>()
    if (ask.canOpen()) actions += TvAskAction.OPEN
    if (state.approver) {
        if (ask.isWaiting) actions += TvAskAction.APPROVE
        if (ask.isWaiting || ask.isFailed) actions += TvAskAction.DECLINE
    } else if (ask.isWaiting && ask.mine) {
        actions += TvAskAction.TAKE_BACK
    }
    return actions
}

fun tvAskNeedsMenu(
    ask: Ask,
    state: RequestsState,
): Boolean =
    when {
        state.isPending(ask.id) -> true
        state.approver -> ask.isWaiting || ask.isFailed
        else -> ask.isWaiting && ask.mine
    }

@Composable
fun tvApproverTabTitle(
    tab: ApproverTab,
    state: RequestsState,
): String {
    val count = state.approverCounts[tab] ?: 0
    val flag = if (tab == ApproverTab.PROCESSING && state.failedCount > 0) " ⚠" else ""
    val label = approverTabLabel(tab)
    return if (count > 0) "$label $count$flag" else label
}

@Composable
fun tvMemberTabTitle(
    tab: ch.orellbuehler.acolyte.feature.requests.MemberTab,
    state: RequestsState,
): String {
    val count = state.memberCounts[tab] ?: 0
    val label = memberTabLabel(tab)
    return if (count > 0) "$label $count" else label
}
