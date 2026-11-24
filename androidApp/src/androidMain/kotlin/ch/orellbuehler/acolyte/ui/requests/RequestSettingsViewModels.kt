package ch.orellbuehler.acolyte.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.requests.FolderField
import ch.orellbuehler.acolyte.feature.requests.LimitKind
import ch.orellbuehler.acolyte.feature.requests.LineKind
import ch.orellbuehler.acolyte.feature.requests.ListKind
import ch.orellbuehler.acolyte.feature.requests.ListSeasons
import ch.orellbuehler.acolyte.feature.requests.NeverAddState
import ch.orellbuehler.acolyte.feature.requests.OutsideListsState
import ch.orellbuehler.acolyte.feature.requests.RequestSettingsState
import ch.orellbuehler.acolyte.feature.requests.SettingsFlag
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RequestRulesViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.requestSettings()

    val state: StateFlow<RequestSettingsState> = presenter.state

    init {
        viewModelScope.launch {
            presenter.load()
            presenter.followCleanup()
        }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun setLimit(
        kind: LimitKind,
        text: String,
    ) = presenter.setLimit(kind, text)

    fun setFolder(
        field: FolderField,
        id: Long,
    ) = presenter.setFolder(field, id)

    fun setPickRules(
        field: FolderField,
        id: Long,
    ) = presenter.setPickRules(field, id)

    fun setFlag(
        flag: SettingsFlag,
        on: Boolean,
    ) = presenter.setFlag(flag, on)

    fun setLineName(
        kind: LineKind,
        index: Int,
        name: String,
    ) = presenter.setLineName(kind, index, name)

    fun setLineLabel(
        kind: LineKind,
        index: Int,
        label: String,
    ) = presenter.setLineLabel(kind, index, label)

    fun setLineShown(
        kind: LineKind,
        index: Int,
        shown: Boolean,
    ) = presenter.setLineShown(kind, index, shown)

    fun setLineRules(
        kind: LineKind,
        index: Int,
        id: Long,
    ) = presenter.setLineRules(kind, index, id)

    fun moveLine(
        kind: LineKind,
        index: Int,
        up: Boolean,
    ) = presenter.moveLine(kind, index, up)

    fun addLine(kind: LineKind) = presenter.addLine(kind)

    fun removeLine(
        kind: LineKind,
        index: Int,
    ) = presenter.removeLine(kind, index)

    fun setRegion(region: String) {
        viewModelScope.launch { presenter.setRegion(region) }
    }

    fun toggleService(id: Long) = presenter.toggleService(id)

    fun discard() = presenter.discard()

    fun save() {
        viewModelScope.launch { presenter.save() }
    }

    fun startCleanup() {
        viewModelScope.launch { presenter.startCleanup() }
    }
}

class OutsideListsViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.outsideLists()

    val state: StateFlow<OutsideListsState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun startNew() = presenter.startNew()

    fun edit(id: Long) = presenter.edit(id)

    fun cancelEdit() = presenter.cancelEdit()

    fun setName(name: String) = presenter.setName(name)

    fun setKind(kind: ListKind) = presenter.setKind(kind)

    fun setTitleKind(show: Boolean) = presenter.setTitleKind(show)

    fun setRef(ref: String) = presenter.setRef(ref)

    fun setFolder(id: Long) = presenter.setFolder(id)

    fun setPickRules(id: Long) = presenter.setPickRules(id)

    fun setSeasons(seasons: ListSeasons) = presenter.setSeasons(seasons)

    fun setNewSeasons(on: Boolean) = presenter.setNewSeasons(on)

    fun setEvery(text: String) = presenter.setEvery(text)

    fun setKeepUpToDate(on: Boolean) = presenter.setKeepUpToDate(on)

    fun setSearchOnAdd(on: Boolean) = presenter.setSearchOnAdd(on)

    fun setEnabled(on: Boolean) = presenter.setEnabled(on)

    fun saveList() {
        viewModelScope.launch { presenter.saveList() }
    }

    fun sync(id: Long) {
        viewModelScope.launch { presenter.sync(id) }
    }

    fun remove(id: Long) {
        viewModelScope.launch { presenter.remove(id) }
    }

    fun saveTrakt(
        key: String,
        token: String,
        secret: String,
        done: () -> Unit,
    ) {
        viewModelScope.launch { if (presenter.saveTrakt(key, token, secret)) done() }
    }

    fun signInTrakt() {
        viewModelScope.launch { presenter.signInTrakt() }
    }

    fun cancelSignIn() = presenter.cancelSignIn()

    fun signOutTrakt() {
        viewModelScope.launch { presenter.signOutTrakt() }
    }

    fun saveMdblist(
        key: String,
        done: () -> Unit,
    ) {
        viewModelScope.launch { if (presenter.saveMdblist(key)) done() }
    }

    fun dismissNote() = presenter.dismissNote()

    suspend fun watch() = presenter.watch()

    override fun onCleared() {
        presenter.cancelSignIn()
    }
}

class NeverAddViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.neverAdd()

    val state: StateFlow<NeverAddState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun add(
        show: Boolean,
        tmdb: String,
        name: String,
        reason: String,
        done: () -> Unit,
    ) {
        viewModelScope.launch { if (presenter.add(show, tmdb, name, reason)) done() }
    }

    fun remove(id: Long) {
        viewModelScope.launch { presenter.remove(id) }
    }
}
