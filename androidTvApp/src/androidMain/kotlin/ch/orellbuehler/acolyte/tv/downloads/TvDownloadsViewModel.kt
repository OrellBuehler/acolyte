package ch.orellbuehler.acolyte.tv.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import ch.orellbuehler.acolyte.feature.downloads.DownloadsState
import ch.orellbuehler.acolyte.feature.downloads.DownloadsSub
import ch.orellbuehler.acolyte.feature.downloads.DownloadsTab
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface TvDownloadsActions {
    fun refresh()

    fun setTab(tab: DownloadsTab)

    fun setSub(sub: DownloadsSub)

    fun showMore()

    fun openTitle(
        titleId: Long,
        open: (DownloadTitleLink) -> Unit,
    )
}

class TvDownloadsViewModel(
    sdk: AcolyteSdk,
) : ViewModel(),
    TvDownloadsActions {
    private val presenter = sdk.downloads()

    val state: StateFlow<DownloadsState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    override fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    override fun setTab(tab: DownloadsTab) = presenter.setTab(tab)

    override fun setSub(sub: DownloadsSub) = presenter.setSub(sub)

    override fun showMore() {
        viewModelScope.launch { presenter.showMore() }
    }

    override fun openTitle(
        titleId: Long,
        open: (DownloadTitleLink) -> Unit,
    ) {
        viewModelScope.launch { presenter.titleLink(titleId)?.let(open) }
    }

    suspend fun watch() = presenter.watch()
}
