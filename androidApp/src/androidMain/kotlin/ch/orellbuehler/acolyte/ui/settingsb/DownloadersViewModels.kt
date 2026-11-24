package ch.orellbuehler.acolyte.ui.settingsb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderKindField
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderNumber
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderRulesState
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderSwitch
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloadersState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DownloadersViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsDownloaders()

    val state: StateFlow<SettingsDownloadersState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    suspend fun watch() = presenter.watch()

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun startAdd() = presenter.startAdd()

    fun change(id: Long) {
        viewModelScope.launch { presenter.startChange(id) }
    }

    fun cancelForm() = presenter.cancelForm()

    fun pickKind(wire: String) = presenter.pickKind(wire)

    fun setName(value: String) = presenter.setName(value)

    fun setUrl(value: String) = presenter.setUrl(value)

    fun setUsername(value: String) = presenter.setUsername(value)

    fun setSecret(value: String) = presenter.setSecret(value)

    fun setLogin(on: Boolean) = presenter.setLogin(on)

    fun setCategory(value: String) = presenter.setCategory(value)

    fun setPriority(value: String) = presenter.setPriority(value)

    fun setKindCategory(
        field: SettingsDownloaderKindField,
        value: String,
    ) = presenter.setKindCategory(field, value)

    fun addTag(value: String) = presenter.addTag(value)

    fun removeTag(value: String) = presenter.removeTag(value)

    fun setCategoryAfterImport(value: String) = presenter.setCategoryAfterImport(value)

    fun setOwnOnly(on: Boolean) = presenter.setOwnOnly(on)

    fun addPathPair() = presenter.addPathPair()

    fun setPathTheirs(
        index: Int,
        value: String,
    ) = presenter.setPathTheirs(index, value)

    fun setPathOurs(
        index: Int,
        value: String,
    ) = presenter.setPathOurs(index, value)

    fun setPathLink(
        index: Int,
        on: Boolean,
    ) = presenter.setPathLink(index, on)

    fun removePathPair(index: Int) = presenter.removePathPair(index)

    fun setAllowLocalNetwork(on: Boolean) = presenter.setAllowLocalNetwork(on)

    fun setWritable(on: Boolean) = presenter.setWritable(on)

    fun setSeedRemove(on: Boolean) = presenter.setSeedRemove(on)

    fun setSeedRatio(value: String) = presenter.setSeedRatio(value)

    fun setSeedHours(value: String) = presenter.setSeedHours(value)

    fun setSeedDeleteFiles(on: Boolean) = presenter.setSeedDeleteFiles(on)

    fun setClearFinished(on: Boolean) = presenter.setClearFinished(on)

    fun setSabUser(value: String) = presenter.setSabUser(value)

    fun setSabPass(value: String) = presenter.setSabPass(value)

    fun fetchKey() {
        viewModelScope.launch { presenter.fetchKey() }
    }

    fun save() {
        viewModelScope.launch { presenter.save() }
    }

    fun test(id: Long) {
        viewModelScope.launch { presenter.test(id) }
    }

    fun testForm() {
        viewModelScope.launch { presenter.testForm() }
    }

    fun pause(id: Long) {
        viewModelScope.launch { presenter.pause(id) }
    }

    fun resume(id: Long) {
        viewModelScope.launch { presenter.resume(id) }
    }

    fun remove(id: Long) {
        viewModelScope.launch { presenter.remove(id) }
    }

    fun makeCategory(
        id: Long,
        name: String,
    ) {
        viewModelScope.launch { presenter.makeCategory(id, name) }
    }

    fun abandon(
        id: Long,
        attemptId: Long,
    ) {
        viewModelScope.launch { presenter.abandon(id, attemptId) }
    }
}

class DownloaderRulesViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsDownloaderRules()

    val state: StateFlow<SettingsDownloaderRulesState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setSwitch(
        flag: SettingsDownloaderSwitch,
        on: Boolean,
    ) = presenter.setSwitch(flag, on)

    fun setNumber(
        kind: SettingsDownloaderNumber,
        text: String,
    ) = presenter.setNumber(kind, text)

    fun setDeadRule(on: Boolean) = presenter.setDeadRule(on)

    fun discard() = presenter.discard()

    fun save() {
        viewModelScope.launch { presenter.save() }
    }
}
