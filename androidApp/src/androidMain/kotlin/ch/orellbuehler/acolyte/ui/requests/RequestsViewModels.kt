package ch.orellbuehler.acolyte.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.AskMusicState
import ch.orellbuehler.acolyte.feature.requests.AskReviewPresenter
import ch.orellbuehler.acolyte.feature.requests.AskReviewState
import ch.orellbuehler.acolyte.feature.requests.AskSheetState
import ch.orellbuehler.acolyte.feature.requests.ClearKind
import ch.orellbuehler.acolyte.feature.requests.KindFilter
import ch.orellbuehler.acolyte.feature.requests.MemberTab
import ch.orellbuehler.acolyte.feature.requests.MusicMode
import ch.orellbuehler.acolyte.feature.requests.ReportFilter
import ch.orellbuehler.acolyte.feature.requests.ReportKind
import ch.orellbuehler.acolyte.feature.requests.ReportProblemState
import ch.orellbuehler.acolyte.feature.requests.ReportsState
import ch.orellbuehler.acolyte.feature.requests.RequestsState
import ch.orellbuehler.acolyte.feature.requests.ShowMode
import ch.orellbuehler.acolyte.feature.requests.Stage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RequestsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.requests()

    val state: StateFlow<RequestsState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun loadMore() {
        viewModelScope.launch { presenter.loadMore() }
    }

    fun setMemberTab(tab: MemberTab) = presenter.setMemberTab(tab)

    fun setApproverTab(tab: ApproverTab) = presenter.setApproverTab(tab)

    fun setKind(kind: KindFilter) = presenter.setKind(kind)

    fun setStage(stage: Stage?) = presenter.setStage(stage)

    fun clearFilters() = presenter.clearFilters()

    fun approve(
        id: Long,
        review: AskReviewPresenter? = null,
    ) = presenter.approve(id, review)

    fun decline(
        id: Long,
        reason: String,
    ) = presenter.decline(id, reason)

    fun approveAll() {
        viewModelScope.launch { presenter.approveAll() }
    }

    fun undo(keys: List<Long>) = presenter.undo(keys)

    fun remove(id: Long) {
        viewModelScope.launch { presenter.remove(id) }
    }

    fun clear(which: ClearKind) {
        viewModelScope.launch { presenter.clear(which) }
    }

    fun takeBack(id: Long) {
        viewModelScope.launch { presenter.takeBack(id) }
    }

    fun dismissNote() = presenter.dismissNote()

    suspend fun watch() = presenter.watch()

    override fun onCleared() {
        presenter.flush()
    }
}

class AskReviewViewModel(
    sdk: AcolyteSdk,
    askId: Long,
) : ViewModel() {
    val presenter = sdk.askReview(askId)

    val state: StateFlow<AskReviewState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun toggleSeason(number: Int) = presenter.toggleSeason(number)

    fun toggleEpisode(
        number: Int,
        episode: Int,
    ) = presenter.toggleEpisode(number, episode)

    fun expandSeason(number: Int) {
        viewModelScope.launch { presenter.expandSeason(number) }
    }

    fun toggleAlbum(mbid: String) = presenter.toggleAlbum(mbid)

    fun toggleSong(
        mbid: String,
        key: String,
    ) = presenter.toggleSong(mbid, key)

    fun expandAlbum(mbid: String) = presenter.expandAlbum(mbid)

    fun select(key: String) = presenter.select(key)
}

class AskViewModel(
    sdk: AcolyteSdk,
    kind: String,
    tmdb: String,
    fourK: Boolean,
) : ViewModel() {
    private val presenter = sdk.ask(kind, tmdb, fourK)

    val state: StateFlow<AskSheetState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun retry() {
        viewModelScope.launch { presenter.load() }
    }

    fun select(key: String) = presenter.select(key)

    fun toggleAlso(key: String) = presenter.toggleAlso(key)

    fun setFollow(on: Boolean) = presenter.setFollow(on)

    fun setMode(mode: ShowMode) = presenter.setMode(mode)

    fun toggleSeason(number: Int) = presenter.toggleSeason(number)

    fun toggleEpisode(
        number: Int,
        episode: Int,
    ) = presenter.toggleEpisode(number, episode)

    fun expandSeason(number: Int) {
        viewModelScope.launch { presenter.expandSeason(number) }
    }

    fun submit() {
        viewModelScope.launch { presenter.submit() }
    }
}

class AskMusicViewModel(
    sdk: AcolyteSdk,
    artist: String,
    name: String,
    album: String,
    edition: String,
) : ViewModel() {
    private val presenter = sdk.askMusic(artist, name, album, edition)

    val state: StateFlow<AskMusicState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun retry() {
        viewModelScope.launch { presenter.load() }
    }

    fun setMode(mode: MusicMode) = presenter.setMode(mode)

    fun setFollow(on: Boolean) = presenter.setFollow(on)

    fun toggle(mbid: String) = presenter.toggle(mbid)

    fun submit() {
        viewModelScope.launch { presenter.submit() }
    }
}

class ReportsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.reports()

    val state: StateFlow<ReportsState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setFilter(filter: ReportFilter) = presenter.setFilter(filter)

    fun fix(
        id: Long,
        reply: String,
        done: () -> Unit,
    ) {
        viewModelScope.launch { if (presenter.fix(id, reply) == null) done() }
    }

    fun reopen(
        id: Long,
        done: () -> Unit,
    ) {
        viewModelScope.launch { if (presenter.reopen(id) == null) done() }
    }

    fun betterCopy(id: Long) {
        viewModelScope.launch { presenter.betterCopy(id) }
    }

    fun dismissNote() = presenter.dismissNote()

    suspend fun watch() = presenter.watch()
}

class ReportProblemViewModel(
    sdk: AcolyteSdk,
    titleId: Long,
    season: Int,
    itemId: Long,
    albumId: Long,
    versionId: Long,
) : ViewModel() {
    private val presenter = sdk.reportProblem(titleId, season, itemId, albumId, versionId)

    val state: StateFlow<ReportProblemState> = presenter.state

    fun setKind(kind: ReportKind) = presenter.setKind(kind)

    fun setNote(note: String) = presenter.setNote(note)

    fun send() {
        viewModelScope.launch { presenter.send() }
    }
}
