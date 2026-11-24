package ch.orellbuehler.acolyte.ui.settings.look

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookAppearanceState
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookDefaultsState
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookMetadataState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsLookAppearanceViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsLookAppearance()

    val state: StateFlow<SettingsLookAppearanceState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { presenter.setLanguage(code) }
    }

    fun setAutoplayTrailers(on: Boolean) {
        viewModelScope.launch { presenter.setAutoplayTrailers(on) }
    }

    fun setSignInArt(on: Boolean) {
        viewModelScope.launch { presenter.setSignInArt(on) }
    }
}

class SettingsLookDefaultsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsLookDefaults()

    val state: StateFlow<SettingsLookDefaultsState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setFolder(
        show: Boolean,
        id: Long,
    ) = presenter.setFolder(show, id)

    fun setRules(
        show: Boolean,
        id: Long,
    ) = presenter.setRules(show, id)

    fun setKeepUpToDate(
        show: Boolean,
        on: Boolean,
    ) = presenter.setKeepUpToDate(show, on)

    fun setSearchOnAdd(
        show: Boolean,
        on: Boolean,
    ) = presenter.setSearchOnAdd(show, on)

    fun setReleaseGate(value: String) = presenter.setReleaseGate(value)

    fun setGateDays(text: String) = presenter.setGateDays(text)

    fun setSeasonChoice(value: String) = presenter.setSeasonChoice(value)

    fun setNewSeasons(value: String) = presenter.setNewSeasons(value)

    fun setEpisodeOrder(value: String) = presenter.setEpisodeOrder(value)

    fun setDetailsSource(value: String) = presenter.setDetailsSource(value)

    fun discard() = presenter.discard()

    fun save() {
        viewModelScope.launch { presenter.save() }
    }
}

class SettingsLookMetadataViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsLookMetadata()

    val state: StateFlow<SettingsLookMetadataState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun chooseMode(mode: String) {
        viewModelScope.launch { presenter.chooseMode(mode) }
    }

    fun openTvdb() = presenter.openTvdb()

    fun setKey(
        name: String,
        text: String,
    ) = presenter.setKey(name, text)

    fun clearKey(name: String) = presenter.clearKey(name)

    fun restoreKey(name: String) = presenter.restoreKey(name)

    fun testKey(name: String) {
        viewModelScope.launch { presenter.testKey(name) }
    }

    fun checkAgain() {
        viewModelScope.launch { presenter.checkAgain() }
    }

    fun discard() = presenter.discard()

    fun save() {
        viewModelScope.launch { presenter.save() }
    }
}
