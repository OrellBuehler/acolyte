package ch.orellbuehler.acolyte.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.model.Decision
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.TorrentUpload
import ch.orellbuehler.acolyte.feature.downloads.DecideAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadDetailPresenter
import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import ch.orellbuehler.acolyte.feature.downloads.DownloadsState
import ch.orellbuehler.acolyte.feature.downloads.RemovePlan
import ch.orellbuehler.acolyte.feature.downloads.SpeedTarget
import ch.orellbuehler.acolyte.feature.downloads.UsenetPriority
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val sdk: AcolyteSdk,
    show: String,
) : ViewModel() {
    val presenter = sdk.downloads()

    val state: StateFlow<DownloadsState> = presenter.state

    init {
        if (show.isNotEmpty()) presenter.show(show)
        viewModelScope.launch { presenter.load() }
    }

    fun detail(row: DownloadRow): DownloadDetailPresenter = sdk.downloadDetail(row)

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun act(
        key: String,
        action: DownloadAction,
    ) {
        viewModelScope.launch { presenter.act(key, action) }
    }

    fun priority(
        key: String,
        priority: UsenetPriority,
    ) {
        viewModelScope.launch { presenter.setPriority(key, priority) }
    }

    fun bulk(action: DownloadAction) {
        viewModelScope.launch { presenter.bulk(action) }
    }

    fun startAll() {
        viewModelScope.launch { presenter.startAll() }
    }

    fun pauseAll() {
        viewModelScope.launch { presenter.pauseAll() }
    }

    fun showMore() {
        viewModelScope.launch { presenter.showMore() }
    }

    fun remove(
        plan: RemovePlan,
        deleteFiles: Boolean,
        turnDown: Boolean,
        searchAgain: Boolean,
        done: (List<String>) -> Unit,
    ) {
        viewModelScope.launch { done(presenter.remove(plan, deleteFiles, turnDown, searchAgain)) }
    }

    fun saveSpeeds(
        targets: List<SpeedTarget>,
        done: () -> Unit,
    ) {
        viewModelScope.launch {
            presenter.saveSpeedLimits(targets)
            done()
        }
    }

    fun slowMode(
        id: Long,
        on: Boolean,
    ) {
        viewModelScope.launch { presenter.setSlowMode(id, on) }
    }

    fun loadClients() {
        viewModelScope.launch { presenter.loadClients() }
    }

    fun addTorrents(
        downloaderId: Long,
        links: String,
        category: String,
        savePath: String,
        paused: Boolean,
        files: List<TorrentUpload>,
        done: (List<String>) -> Unit,
    ) {
        viewModelScope.launch { done(presenter.addTorrentFiles(downloaderId, links, files, category, savePath, paused)) }
    }

    fun shareLimits(
        keys: List<String>,
        ratio: Double,
        seeding: Long,
        idle: Long,
        action: String,
        done: (Boolean) -> Unit,
    ) {
        viewModelScope.launch { done(presenter.setShareLimits(keys, ratio, seeding, idle, action)) }
    }

    fun location(
        keys: List<String>,
        category: String,
        folder: String,
        done: (Boolean) -> Unit,
    ) {
        viewModelScope.launch { done(presenter.setLocation(keys, category, folder)) }
    }

    fun speeds(
        keys: List<String>,
        down: Double,
        up: Double,
        done: (Boolean) -> Unit,
    ) {
        viewModelScope.launch { done(presenter.setTorrentSpeeds(keys, down, up)) }
    }

    fun decide(
        decision: Decision,
        action: DecideAction,
        pack: Boolean,
    ) {
        viewModelScope.launch { presenter.decide(decision, action, pack) }
    }

    fun importAgain(decision: Decision) {
        viewModelScope.launch { presenter.importAgain(decision) }
    }

    fun setEdition(
        decision: Decision,
        edition: String,
        done: (Boolean) -> Unit,
    ) {
        viewModelScope.launch { done(presenter.setEdition(decision, edition)) }
    }

    fun pickMatch(
        id: Long,
        candidate: String?,
        notMedia: Boolean,
    ) {
        viewModelScope.launch { presenter.pickMatch(id, candidate, notMedia) }
    }

    fun runMatches() {
        viewModelScope.launch { presenter.runMatches() }
    }

    fun openTitle(
        titleId: Long,
        open: (DownloadTitleLink) -> Unit,
    ) {
        viewModelScope.launch { presenter.titleLink(titleId)?.let(open) }
    }

    suspend fun reauth(
        password: String,
        code: String,
    ): Boolean = sdk.session.reauth(password, code.trim().ifEmpty { null }) == null

    suspend fun watch() = presenter.watch()
}
