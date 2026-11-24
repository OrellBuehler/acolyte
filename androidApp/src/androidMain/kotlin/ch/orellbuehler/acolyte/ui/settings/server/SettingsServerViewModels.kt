package ch.orellbuehler.acolyte.ui.settings.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertField
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerAlertsState
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupPreset
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupsState
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerLogsState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsAlertsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsAlerts()

    val state: StateFlow<SettingsServerAlertsState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun retryPack() {
        viewModelScope.launch { presenter.retryPack() }
    }

    fun startAdd() = presenter.startAdd()

    fun startChange(id: Long) = presenter.startChange(id)

    fun cancelForm() = presenter.cancelForm()

    fun setText(
        field: SettingsServerAlertField,
        value: String,
    ) = presenter.setText(field, value)

    fun setKind(kind: SettingsServerAlertKind) = presenter.setKind(kind)

    fun setEnabled(on: Boolean) = presenter.setEnabled(on)

    fun setAllowLocalNetwork(on: Boolean) = presenter.setAllowLocalNetwork(on)

    fun setClearToken(on: Boolean) = presenter.setClearToken(on)

    fun setMoment(
        id: String,
        on: Boolean,
    ) = presenter.setMoment(id, on)

    fun setGroup(
        key: String,
        on: Boolean,
    ) = presenter.setGroup(key, on)

    fun save() {
        viewModelScope.launch { presenter.save() }
    }

    fun test(id: Long) {
        viewModelScope.launch { presenter.test(id) }
    }

    fun remove(id: Long) {
        viewModelScope.launch { presenter.remove(id) }
    }

    fun setPack(style: String) {
        viewModelScope.launch { presenter.setPack(style) }
    }

    fun dismissNotice(seq: Int) = presenter.dismissNotice(seq)
}

class SettingsBackupsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsBackups()

    val state: StateFlow<SettingsServerBackupsState> = presenter.state
    val reauth = presenter.reauth

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun makeNow() {
        viewModelScope.launch { presenter.makeNow() }
    }

    fun setPreset(preset: SettingsServerBackupPreset) {
        viewModelScope.launch { presenter.setPreset(preset) }
    }

    fun download(name: String) {
        viewModelScope.launch { presenter.download(name) }
    }

    fun delete(name: String) {
        viewModelScope.launch { presenter.delete(name) }
    }

    fun downloadKeyKit() {
        viewModelScope.launch { presenter.downloadKeyKit() }
    }

    fun stagingPath(name: String): String = presenter.stagingPath(name)

    fun restore(
        path: String,
        name: String,
    ) {
        viewModelScope.launch { presenter.restore(path, name) }
    }

    fun fileDelivered() = presenter.fileDelivered()

    fun release() = presenter.release()

    fun dismissNotice(seq: Int) = presenter.dismissNotice(seq)

    override fun onCleared() {
        presenter.release()
    }
}

class SettingsLogsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsLogs()

    val state: StateFlow<SettingsServerLogsState> = presenter.state
    val reauth = presenter.reauth

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun applyFilter(
        level: String,
        query: String,
        from: String,
        to: String,
    ) {
        viewModelScope.launch { presenter.applyFilter(level, query, from, to) }
    }

    fun clear() {
        viewModelScope.launch { presenter.clear() }
    }

    fun older() {
        viewModelScope.launch { presenter.older() }
    }

    fun newer() {
        viewModelScope.launch { presenter.newer() }
    }

    fun download() {
        viewModelScope.launch { presenter.download() }
    }

    fun fileDelivered() = presenter.fileDelivered()

    fun dismissDownloadError() = presenter.dismissDownloadError()

    override fun onCleared() {
        presenter.release()
    }
}
