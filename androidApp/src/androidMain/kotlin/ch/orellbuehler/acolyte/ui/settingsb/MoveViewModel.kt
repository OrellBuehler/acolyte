package ch.orellbuehler.acolyte.ui.settingsb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.feature.move.MoveItemsList
import ch.orellbuehler.acolyte.feature.move.MoveListKind
import ch.orellbuehler.acolyte.feature.move.MoveNamesChoice
import ch.orellbuehler.acolyte.feature.move.MovePersonChoice
import ch.orellbuehler.acolyte.feature.move.MovePickKind
import ch.orellbuehler.acolyte.feature.move.MoveReadForm
import ch.orellbuehler.acolyte.feature.move.SettingsMoveState
import ch.orellbuehler.acolyte.feature.move.SettingsMoveStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MoveViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.settingsMove()

    val reauth = presenter.reauth
    private var readJob: Job? = null

    val state: StateFlow<SettingsMoveState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    suspend fun watch() = presenter.watch()

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun enter(step: SettingsMoveStep) = presenter.enter(step)

    fun goBackToReads() = presenter.goBackToReads()

    fun togglePick(
        id: Long,
        on: Boolean,
    ) = presenter.togglePick(id, on)

    fun forget(id: Long) {
        viewModelScope.launch { presenter.forget(id) }
    }

    fun removeMove(id: Long) {
        viewModelScope.launch { presenter.removeMove(id) }
    }

    fun openMove(id: Long) {
        viewModelScope.launch { presenter.openMove(id) }
    }

    fun resetSheet() = presenter.resetSheet()

    fun clearFound() = presenter.clearFound()

    fun clearProbe() = presenter.clearProbe()

    fun probe(form: MoveReadForm) {
        viewModelScope.launch { presenter.probe(form) }
    }

    fun find(
        host: String,
        form: MoveReadForm,
    ) {
        viewModelScope.launch { presenter.find(host, form) }
    }

    fun addLinked(form: MoveReadForm) {
        viewModelScope.launch { presenter.addLinked(form) }
    }

    fun read(
        form: MoveReadForm,
        onDone: (Boolean) -> Unit,
    ) {
        readJob =
            viewModelScope.launch {
                onDone(presenter.read(form))
            }
    }

    fun stopRead() {
        readJob?.cancel()
    }

    fun pickFolder(
        oldPath: String,
        kind: MovePickKind,
        path: String,
    ) = presenter.pickFolder(oldPath, kind, path)

    fun typeOther(
        oldPath: String,
        text: String,
    ) = presenter.typeOther(oldPath, text)

    fun commitFolders() = presenter.commitFolders()

    fun addRule(from: String) = presenter.addRule(from)

    fun editRule(
        index: Int,
        from: String,
        to: String,
    ) = presenter.editRule(index, from, to)

    fun removeRule(index: Int) = presenter.removeRule(index)

    fun setGroup(
        id: String,
        on: Boolean,
    ) = presenter.setGroup(id, on)

    fun setPerson(
        key: String,
        choice: MovePersonChoice?,
    ) = presenter.setPerson(key, choice)

    fun setNames(choice: MoveNamesChoice) = presenter.setNames(choice)

    fun setNoFormats(noFormats: Boolean) = presenter.setNoFormats(noFormats)

    fun chooseOnce(
        key: String,
        choice: MovePersonChoice,
    ) = presenter.chooseOnce(key, choice)

    fun importNow() {
        viewModelScope.launch { presenter.importNow() }
    }

    fun closeDialog() = presenter.closeDialog()

    fun askUndo() = presenter.askUndo()

    fun start() {
        viewModelScope.launch { presenter.start() }
    }

    fun undo() {
        viewModelScope.launch { presenter.undo() }
    }

    fun done() {
        viewModelScope.launch { presenter.done() }
    }

    fun chooseAgain() = presenter.chooseAgain()

    fun report(onText: (String, String) -> Unit) {
        viewModelScope.launch {
            val text = presenter.reportText() ?: return@launch
            onText(presenter.reportName(), text)
        }
    }

    fun items(
        kind: MoveListKind,
        id: String,
    ): MoveItemsList = presenter.items(kind, id)

    fun dismissNotice(seq: Int) = presenter.dismissNotice(seq)

    fun dismissProblem() = presenter.dismissProblem()
}
