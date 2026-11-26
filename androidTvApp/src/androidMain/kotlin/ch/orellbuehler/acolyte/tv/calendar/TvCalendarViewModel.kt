package ch.orellbuehler.acolyte.tv.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.downloads.CalendarFollows
import ch.orellbuehler.acolyte.feature.downloads.CalendarGroup
import ch.orellbuehler.acolyte.feature.downloads.CalendarState
import ch.orellbuehler.acolyte.feature.downloads.CalendarType
import ch.orellbuehler.acolyte.feature.downloads.DownloadTitleLink
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface TvCalendarActions {
    fun refresh()

    fun previous()

    fun next()

    fun today()

    fun setType(type: CalendarType)

    fun setFollows(follows: CalendarFollows)

    fun link(group: CalendarGroup): DownloadTitleLink?
}

class TvCalendarViewModel(
    sdk: AcolyteSdk,
) : ViewModel(),
    TvCalendarActions {
    private val presenter = sdk.calendar()

    val state: StateFlow<CalendarState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    override fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    override fun previous() {
        viewModelScope.launch { presenter.previous() }
    }

    override fun next() {
        viewModelScope.launch { presenter.next() }
    }

    override fun today() {
        viewModelScope.launch { presenter.goToday() }
    }

    override fun setType(type: CalendarType) = presenter.setType(type)

    override fun setFollows(follows: CalendarFollows) = presenter.setFollows(follows)

    override fun link(group: CalendarGroup): DownloadTitleLink? = presenter.link(group)

    suspend fun watch() = presenter.watch()
}
