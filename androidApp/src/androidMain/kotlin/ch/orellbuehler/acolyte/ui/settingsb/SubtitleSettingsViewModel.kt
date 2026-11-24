package ch.orellbuehler.acolyte.ui.settingsb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SubtitleSettingsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsSubtitles()

    val state: StateFlow<SettingsSubtitleState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    suspend fun watch() = presenter.watch()

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setNumber(
        key: String,
        text: String,
    ) = presenter.setNumber(key, text)

    fun setFlag(
        key: String,
        on: Boolean,
    ) = presenter.setFlag(key, on)

    fun setHiSuffix(suffix: String) = presenter.setHiSuffix(suffix)

    fun addLanguage() = presenter.addLanguage()

    fun removeLanguage(index: Int) = presenter.removeLanguage(index)

    fun setWantLanguage(
        index: Int,
        code: String,
    ) = presenter.setWantLanguage(index, code)

    fun setWantType(
        index: Int,
        type: String,
    ) = presenter.setWantType(index, type)

    fun setWantForced(
        index: Int,
        forced: Boolean,
    ) = presenter.setWantForced(index, forced)

    fun setWantTranslateFrom(
        index: Int,
        code: String,
    ) = presenter.setWantTranslateFrom(index, code)

    fun pickCleanLanguage(code: String) = presenter.pickCleanLanguage(code)

    fun setStep(
        key: String,
        on: Boolean,
    ) = presenter.setStep(key, on)

    fun removeCleanLanguage() = presenter.removeCleanLanguage()

    fun discard() = presenter.discard()

    fun save() {
        viewModelScope.launch { presenter.save() }
    }

    fun setTranslatorKind(kind: String) = presenter.setTranslatorKind(kind)

    fun setTranslatorUrl(url: String) = presenter.setTranslatorUrl(url)

    fun setTranslatorModel(model: String) = presenter.setTranslatorModel(model)

    fun setTranslatorKey(key: String) = presenter.setTranslatorKey(key)

    fun setTranslatorClearKey(clear: Boolean) = presenter.setTranslatorClearKey(clear)

    fun setTranslatorBuiltInAuto(on: Boolean) = presenter.setTranslatorBuiltInAuto(on)

    fun saveTranslator() {
        viewModelScope.launch { presenter.saveTranslator() }
    }

    fun testTranslator() {
        viewModelScope.launch { presenter.testTranslator() }
    }

    fun startAdd() = presenter.startAdd()

    fun startChange(id: Long) = presenter.startChange(id)

    fun pickKind(kind: String) = presenter.pickKind(kind)

    fun cancelForm() = presenter.cancelForm()

    fun setFormName(name: String) = presenter.setFormName(name)

    fun setFormUrl(url: String) = presenter.setFormUrl(url)

    fun setFormNumber(
        key: String,
        text: String,
    ) = presenter.setFormNumber(key, text)

    fun setFormAccount(
        field: String,
        text: String,
    ) = presenter.setFormAccount(field, text)

    fun setFormFlag(
        key: String,
        on: Boolean,
    ) = presenter.setFormFlag(key, on)

    fun submitForm() {
        viewModelScope.launch { presenter.submitForm() }
    }

    fun testSite(id: Long) {
        viewModelScope.launch { presenter.testSite(id) }
    }

    fun togglePaused(id: Long) {
        viewModelScope.launch { presenter.togglePaused(id) }
    }

    fun removeSite(id: Long) {
        viewModelScope.launch { presenter.removeSite(id) }
    }

    fun cleanAll() {
        viewModelScope.launch { presenter.cleanAll() }
    }
}
