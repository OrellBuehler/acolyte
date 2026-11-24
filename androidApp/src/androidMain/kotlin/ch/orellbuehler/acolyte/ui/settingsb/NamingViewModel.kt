package ch.orellbuehler.acolyte.ui.settingsb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingChoice
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingState
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingStyle
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingSwitch
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingText
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsNamingViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsNaming()

    val state: StateFlow<SettingsNamingState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    suspend fun followExample() = presenter.followExample()

    fun setText(
        field: SettingsNamingText,
        text: String,
    ) = presenter.setText(field, text)

    fun insertWord(
        field: SettingsNamingText,
        word: String,
        start: Int,
        end: Int,
    ): Int = presenter.insertWord(field, word, start, end)

    fun setSwitch(
        field: SettingsNamingSwitch,
        on: Boolean,
    ) = presenter.setSwitch(field, on)

    fun setChoice(
        field: SettingsNamingChoice,
        value: String,
    ) = presenter.setChoice(field, value)

    fun setNumber(
        field: SettingsNamingNumber,
        text: String,
    ) = presenter.setNumber(field, text)

    fun applyStyle(style: SettingsNamingStyle) = presenter.applyStyle(style)

    fun discard() = presenter.discard()

    fun save() {
        viewModelScope.launch { presenter.save() }
    }

    fun cleanHolding() {
        viewModelScope.launch { presenter.cleanHolding() }
    }
}
