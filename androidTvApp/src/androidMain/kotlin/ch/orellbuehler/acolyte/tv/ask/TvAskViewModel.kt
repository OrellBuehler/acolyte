package ch.orellbuehler.acolyte.tv.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.requests.AskSheetState
import ch.orellbuehler.acolyte.feature.requests.ShowMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface TvAskActions {
    fun select(key: String)

    fun toggleAlso(key: String)

    fun setFollow(on: Boolean)

    fun setMode(mode: ShowMode)

    fun toggleSeason(number: Int)

    fun submit()
}

class TvAskViewModel(
    sdk: AcolyteSdk,
    kind: String,
    tmdb: String,
    fourK: Boolean,
) : ViewModel(),
    TvAskActions {
    private val presenter = sdk.ask(kind, tmdb, fourK)

    val state: StateFlow<AskSheetState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun retry() {
        viewModelScope.launch { presenter.load() }
    }

    override fun select(key: String) = presenter.select(key)

    override fun toggleAlso(key: String) = presenter.toggleAlso(key)

    override fun setFollow(on: Boolean) = presenter.setFollow(on)

    override fun setMode(mode: ShowMode) = presenter.setMode(mode)

    override fun toggleSeason(number: Int) = presenter.toggleSeason(number)

    override fun submit() {
        viewModelScope.launch { presenter.submit() }
    }
}
