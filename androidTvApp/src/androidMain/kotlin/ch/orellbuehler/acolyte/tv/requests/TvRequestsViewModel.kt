package ch.orellbuehler.acolyte.tv.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.KindFilter
import ch.orellbuehler.acolyte.feature.requests.MemberTab
import ch.orellbuehler.acolyte.feature.requests.RequestsState
import ch.orellbuehler.acolyte.feature.requests.Stage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface TvRequestsActions {
    fun refresh()

    fun loadMore()

    fun setMemberTab(tab: MemberTab)

    fun setApproverTab(tab: ApproverTab)

    fun setKind(kind: KindFilter)

    fun setStage(stage: Stage?)

    fun clearFilters()

    fun approve(id: Long)

    fun decline(id: Long)

    fun approveAll()

    fun undo(keys: List<Long>)

    fun takeBack(id: Long)

    fun dismissNote()
}

class TvRequestsViewModel(
    sdk: AcolyteSdk,
) : ViewModel(),
    TvRequestsActions {
    private val presenter = sdk.requests()

    val state: StateFlow<RequestsState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    override fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    override fun loadMore() {
        viewModelScope.launch { presenter.loadMore() }
    }

    override fun setMemberTab(tab: MemberTab) = presenter.setMemberTab(tab)

    override fun setApproverTab(tab: ApproverTab) = presenter.setApproverTab(tab)

    override fun setKind(kind: KindFilter) = presenter.setKind(kind)

    override fun setStage(stage: Stage?) = presenter.setStage(stage)

    override fun clearFilters() = presenter.clearFilters()

    override fun approve(id: Long) = presenter.approve(id)

    override fun decline(id: Long) = presenter.decline(id, "")

    override fun approveAll() {
        viewModelScope.launch { presenter.approveAll() }
    }

    override fun undo(keys: List<Long>) = presenter.undo(keys)

    override fun takeBack(id: Long) {
        viewModelScope.launch { presenter.takeBack(id) }
    }

    override fun dismissNote() = presenter.dismissNote()

    fun flush() = presenter.flush()

    suspend fun watch() = presenter.watch()
}
