package ch.orellbuehler.acolyte.ui.settingsb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchChoice
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchState
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchSwitch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsSearchViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsSearch()

    val state: StateFlow<SettingsSearchState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setNumber(
        field: SettingsSearchNumber,
        text: String,
    ) = presenter.setNumber(field, text)

    fun setSwitch(
        field: SettingsSearchSwitch,
        on: Boolean,
    ) = presenter.setSwitch(field, on)

    fun setChoice(
        field: SettingsSearchChoice,
        value: String,
    ) = presenter.setChoice(field, value)

    fun discard() = presenter.discard()

    fun save() {
        viewModelScope.launch { presenter.save() }
    }

    fun checkFeeds() {
        viewModelScope.launch { presenter.checkFeeds() }
    }

    fun findBetter() {
        viewModelScope.launch { presenter.findBetter() }
    }
}
