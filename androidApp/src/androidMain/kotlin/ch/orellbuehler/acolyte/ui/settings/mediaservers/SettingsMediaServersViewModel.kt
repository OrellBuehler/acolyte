package ch.orellbuehler.acolyte.ui.settings.mediaservers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsMediaServersViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsMediaServers()

    val state: StateFlow<SettingsMediaServersState> = presenter.state
    val reauth = presenter.reauth

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun startAdd() = presenter.startAdd()

    fun startChange(id: Long) = presenter.startChange(id)

    fun cancelForm() = presenter.cancelForm()

    fun setName(text: String) = presenter.setName(text)

    fun setKind(wire: String) = presenter.setKind(wire)

    fun setUrl(text: String) = presenter.setUrl(text)

    fun setUsername(text: String) = presenter.setUsername(text)

    fun setToken(text: String) = presenter.setToken(text)

    fun setWebUrl(text: String) = presenter.setWebUrl(text)

    fun setPlainPassword(on: Boolean) = presenter.setPlainPassword(on)

    fun setOnImport(on: Boolean) = presenter.setOnImport(on)

    fun setOnRename(on: Boolean) = presenter.setOnRename(on)

    fun setOnDelete(on: Boolean) = presenter.setOnDelete(on)

    fun setPathFrom(text: String) = presenter.setPathFrom(text)

    fun setPathTo(text: String) = presenter.setPathTo(text)

    fun setAllowLocalNetwork(on: Boolean) = presenter.setAllowLocalNetwork(on)

    fun setEnabled(on: Boolean) = presenter.setEnabled(on)

    fun findPlexServers() {
        viewModelScope.launch { presenter.findPlexServers() }
    }

    fun pickPlex(index: Int) = presenter.pickPlex(index)

    fun pickPlexAddress(url: String) = presenter.pickPlexAddress(url)

    fun save() {
        viewModelScope.launch { presenter.save() }
    }

    fun test(id: Long) {
        viewModelScope.launch { presenter.test(id) }
    }

    fun remove(id: Long) {
        viewModelScope.launch { presenter.remove(id) }
    }

    fun setWatchHistory(on: Boolean) {
        viewModelScope.launch { presenter.setWatchHistory(on) }
    }

    fun setLabelsEnabled(on: Boolean) = presenter.setLabelsEnabled(on)

    fun setLabelText(text: String) = presenter.setLabelText(text)

    fun setLabelAskers(on: Boolean) = presenter.setLabelAskers(on)

    fun saveLabels() {
        viewModelScope.launch { presenter.saveLabels() }
    }

    fun runLabels() {
        viewModelScope.launch { presenter.runLabels() }
    }

    fun setTracksEnabled(on: Boolean) = presenter.setTracksEnabled(on)

    fun addTrackLanguage(subtitles: Boolean) = presenter.addTrackLanguage(subtitles)

    fun removeTrackLanguage(
        subtitles: Boolean,
        index: Int,
    ) = presenter.removeTrackLanguage(subtitles, index)

    fun moveTrackLanguage(
        subtitles: Boolean,
        index: Int,
        up: Boolean,
    ) = presenter.moveTrackLanguage(subtitles, index, up)

    fun setTrackLanguage(
        subtitles: Boolean,
        index: Int,
        code: String,
    ) = presenter.setTrackLanguage(subtitles, index, code)

    fun saveTracks() {
        viewModelScope.launch { presenter.saveTracks() }
    }

    fun dismissNotice(id: Long) = presenter.dismissNotice(id)
}
