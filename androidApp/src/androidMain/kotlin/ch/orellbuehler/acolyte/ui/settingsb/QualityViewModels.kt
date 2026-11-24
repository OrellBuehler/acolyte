package ch.orellbuehler.acolyte.ui.settingsb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleEditPresenter
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleEditState
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRulesPresenter
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRulesState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QualityListViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    val presenter: SettingsPickRulesPresenter = sdk.settingsPickRules()

    val state: StateFlow<SettingsPickRulesState> = presenter.state

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun confirmRemove() {
        viewModelScope.launch { presenter.confirmRemove() }
    }

    fun saveTrait() {
        viewModelScope.launch { presenter.saveTrait() }
    }

    fun confirmRemoveTrait() {
        viewModelScope.launch { presenter.confirmRemoveTrait() }
    }

    fun runCheck() {
        viewModelScope.launch { presenter.runCheck() }
    }
}

class QualityEditViewModel(
    sdk: AcolyteSdk,
    id: Long,
    from: Long,
) : ViewModel() {
    val presenter: SettingsPickRuleEditPresenter = sdk.settingsPickRuleEdit(id, from)

    val state: StateFlow<SettingsPickRuleEditState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    suspend fun save() = presenter.save()

    fun setTrashApp(app: String) {
        viewModelScope.launch { presenter.setTrashApp(app) }
    }

    fun loadTrash() {
        viewModelScope.launch { presenter.loadTrash() }
    }

    fun applyTrash() {
        viewModelScope.launch { presenter.applyTrash() }
    }

    fun syncAgain() {
        viewModelScope.launch { presenter.syncAgain() }
    }
}
