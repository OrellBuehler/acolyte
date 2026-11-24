package ch.orellbuehler.acolyte.ui.settings.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsAiState
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsApiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsApiViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsApi()

    val state: StateFlow<SettingsApiState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun reloadSeerr() {
        viewModelScope.launch { presenter.reloadSeerr() }
    }

    fun makeKey(label: String) {
        viewModelScope.launch { presenter.makeKey(label) }
    }

    fun revokeKey(id: Long) {
        viewModelScope.launch { presenter.revokeKey(id) }
    }

    fun dismissFresh() = presenter.dismissFresh()

    fun setSeerrEnabled(on: Boolean) {
        viewModelScope.launch { presenter.setSeerrEnabled(on) }
    }

    fun setSeerrDeleteFiles(on: Boolean) {
        viewModelScope.launch { presenter.setSeerrDeleteFiles(on) }
    }

    fun makeSeerrKey(side: String) {
        viewModelScope.launch { presenter.makeSeerrKey(side) }
    }

    fun removeSeerrKey(side: String) {
        viewModelScope.launch { presenter.removeSeerrKey(side) }
    }

    fun dismissSeerrFresh() = presenter.dismissSeerrFresh()

    fun confirmReauth(
        password: String,
        code: String,
    ) {
        viewModelScope.launch { presenter.confirmReauth(password, code) }
    }

    fun cancelReauth() = presenter.cancelReauth()

    fun dismissNotice(seq: Int) = presenter.dismissNotice(seq)
}

class SettingsAiViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsAi()

    val state: StateFlow<SettingsAiState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setEnabled(on: Boolean) {
        viewModelScope.launch { presenter.setEnabled(on) }
    }

    fun makeKey(
        label: String,
        groups: List<String>,
    ) {
        viewModelScope.launch { presenter.makeKey(label, groups) }
    }

    fun changeGroups(
        id: Long,
        groups: List<String>,
    ) {
        viewModelScope.launch { presenter.changeGroups(id, groups) }
    }

    fun clearChangeError() = presenter.clearChangeError()

    fun revokeKey(id: Long) {
        viewModelScope.launch { presenter.revokeKey(id) }
    }

    fun dismissFresh() = presenter.dismissFresh()

    fun confirmReauth(
        password: String,
        code: String,
    ) {
        viewModelScope.launch { presenter.confirmReauth(password, code) }
    }

    fun cancelReauth() = presenter.cancelReauth()

    fun dismissNotice(seq: Int) = presenter.dismissNotice(seq)
}
