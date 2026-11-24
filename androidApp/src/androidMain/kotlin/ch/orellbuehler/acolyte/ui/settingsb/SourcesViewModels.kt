package ch.orellbuehler.acolyte.ui.settingsb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.sources.SettingsBridgeDraft
import ch.orellbuehler.acolyte.feature.sources.SettingsBridgeState
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCheckState
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceForm
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceHealthState
import ch.orellbuehler.acolyte.feature.sources.SettingsSourcesState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SourcesViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsSources()

    val state: StateFlow<SettingsSourcesState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun reloadQuietly() {
        viewModelScope.launch { presenter.reloadQuietly() }
    }

    suspend fun watch() = presenter.watch()

    fun startAdd() = presenter.startAdd()

    fun startChange(id: Long) = presenter.startChange(id)

    fun cancelEdit() = presenter.cancelEdit()

    fun settled(form: SettingsSourceForm): SettingsSourceForm = presenter.settled(form)

    fun save(form: SettingsSourceForm) {
        viewModelScope.launch { presenter.save(form) }
    }

    fun test(id: Long) {
        viewModelScope.launch { presenter.test(id) }
    }

    fun tryNow(id: Long) {
        viewModelScope.launch { presenter.tryNow(id) }
    }

    fun setPaused(
        id: Long,
        paused: Boolean,
    ) {
        viewModelScope.launch { presenter.setPaused(id, paused) }
    }

    fun remove(id: Long) {
        viewModelScope.launch { presenter.remove(id) }
    }

    fun dismissNotice(seq: Int) = presenter.dismissNotice(seq)
}

class BridgeViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsBridges()

    val state: StateFlow<SettingsBridgeState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun startConnect(kind: String) = presenter.startConnect(kind)

    fun startChange(id: Long) = presenter.startChange(id)

    fun cancelForm() = presenter.cancelForm()

    fun test(draft: SettingsBridgeDraft) {
        viewModelScope.launch { presenter.test(draft) }
    }

    fun link(draft: SettingsBridgeDraft) {
        viewModelScope.launch { presenter.link(draft) }
    }

    fun confirmReauth(
        password: String,
        code: String,
    ) {
        viewModelScope.launch { presenter.confirmReauth(password, code) }
    }

    fun cancelReauth() = presenter.cancelReauth()

    fun sync(id: Long) {
        viewModelScope.launch { presenter.sync(id) }
    }

    fun unlink(
        id: Long,
        keepSources: Boolean,
        done: () -> Unit,
    ) {
        viewModelScope.launch { if (presenter.unlink(id, keepSources)) done() }
    }

    fun clearRemoveError() = presenter.clearRemoveError()

    fun dismissKeys() = presenter.dismissKeys()

    fun dismissNotice(seq: Int) = presenter.dismissNotice(seq)
}

class SourceCheckViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsSourceCheck()

    val state: StateFlow<SettingsSourceCheckState> = presenter.state

    fun run(
        kind: String,
        title: String,
    ) {
        viewModelScope.launch { presenter.run(kind, title) }
    }

    fun titleAfterKind(
        old: String,
        title: String,
        new: String,
    ): String = presenter.titleAfterKind(old, title, new)

    fun sample(kind: String): String = presenter.sample(kind)
}

class SourceHealthViewModel(
    sdk: AcolyteSdk,
    id: Long,
) : ViewModel() {
    private val presenter = sdk.settingsSourceHealth(id)

    val state: StateFlow<SettingsSourceHealthState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setDays(days: Int) {
        viewModelScope.launch { presenter.setDays(days) }
    }

    fun tryNow() {
        viewModelScope.launch { presenter.tryNow() }
    }

    suspend fun watch() = presenter.watch()
}
