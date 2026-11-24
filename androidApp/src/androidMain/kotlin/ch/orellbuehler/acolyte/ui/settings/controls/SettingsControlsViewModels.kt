package ch.orellbuehler.acolyte.ui.settings.controls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsDangerState
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsKind
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsPartsState
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsSchedulesState
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsStageAction
import ch.orellbuehler.acolyte.feature.settings.controls.SettingsControlsThisAppState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsPartsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsControlsParts()

    val state: StateFlow<SettingsControlsPartsState> = presenter.state
    val reauth = presenter.reauth

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun turnOn(part: String) {
        viewModelScope.launch { presenter.turnOn(part) }
    }

    fun turnOff(part: String) {
        viewModelScope.launch { presenter.turnOff(part) }
    }

    fun restart(part: String) {
        viewModelScope.launch { presenter.restart(part) }
    }

    fun stage(
        stage: String,
        action: SettingsControlsStageAction,
    ) {
        viewModelScope.launch { presenter.stage(stage, action) }
    }

    fun reconnect(key: String) {
        viewModelScope.launch { presenter.reconnect(key) }
    }

    fun confirm() {
        viewModelScope.launch { presenter.confirm() }
    }

    fun cancel() = presenter.cancel()

    suspend fun watch() = presenter.watch()
}

class SettingsDangerViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsControlsDanger()

    val state: StateFlow<SettingsControlsDangerState> = presenter.state
    val reauth = presenter.reauth

    init {
        presenter.load()
    }

    fun request(kind: SettingsControlsKind) {
        viewModelScope.launch { presenter.request(kind) }
    }

    fun confirm() {
        viewModelScope.launch { presenter.confirm() }
    }

    fun cancel() = presenter.cancel()

    fun dismissStopped() = presenter.dismissStopped()

    suspend fun waitForServer() = presenter.waitForServer()
}

class SettingsThisAppViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsControlsThisApp()

    val state: StateFlow<SettingsControlsThisAppState> = presenter.state
    val reauth = presenter.reauth

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setHistoryRoutine(days: Int) {
        viewModelScope.launch { presenter.setHistoryRoutine(days) }
    }

    fun setHistoryOther(days: Int) {
        viewModelScope.launch { presenter.setHistoryOther(days) }
    }

    fun setCacheGb(gb: Int) {
        viewModelScope.launch { presenter.setCacheGb(gb) }
    }

    fun setCountInstall(on: Boolean) {
        viewModelScope.launch { presenter.setCountInstall(on) }
    }

    fun proveStorage() {
        viewModelScope.launch { presenter.proveStorage() }
    }

    fun setUrlBase(text: String) = presenter.setUrlBase(text)

    fun saveUrlBase() {
        viewModelScope.launch { presenter.saveUrlBase() }
    }

    fun setProxyType(type: String) = presenter.setProxyType(type)

    fun setProxyHost(text: String) = presenter.setProxyHost(text)

    fun setProxyPort(text: String) = presenter.setProxyPort(text)

    fun setProxyUsername(text: String) = presenter.setProxyUsername(text)

    fun setProxyPassword(text: String) = presenter.setProxyPassword(text)

    fun setProxyBypass(text: String) = presenter.setProxyBypass(text)

    fun setProxyBypassLocal(on: Boolean) = presenter.setProxyBypassLocal(on)

    fun saveProxy() {
        viewModelScope.launch { presenter.saveProxy() }
    }

    suspend fun watch() = presenter.watch()
}

class SettingsSchedulesViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsControlsSchedules()

    val state: StateFlow<SettingsControlsSchedulesState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun runNow(kind: String) {
        viewModelScope.launch { presenter.runNow(kind) }
    }

    fun edit(kind: String) = presenter.edit(kind)

    fun closeEditor() = presenter.closeEditor()

    fun setAmount(text: String) = presenter.setAmount(text)

    fun stepAmount(up: Boolean) = presenter.stepAmount(up)

    fun saveEditor() {
        viewModelScope.launch { presenter.saveEditor() }
    }

    fun setEnabled(
        kind: String,
        enabled: Boolean,
    ) {
        viewModelScope.launch { presenter.setEnabled(kind, enabled) }
    }

    suspend fun watch() = presenter.watch()
}
