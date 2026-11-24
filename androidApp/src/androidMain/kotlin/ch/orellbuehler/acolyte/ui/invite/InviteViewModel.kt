package ch.orellbuehler.acolyte.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.session.InviteState
import ch.orellbuehler.acolyte.session.InviteTarget
import ch.orellbuehler.acolyte.session.SignInWay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InviteViewModel(
    sdk: AcolyteSdk,
    target: InviteTarget,
) : ViewModel() {
    private val flow = sdk.invite(target)
    private var plexJob: Job? = null

    val state: StateFlow<InviteState> = flow.state

    init {
        viewModelScope.launch { flow.load() }
    }

    fun retry() {
        viewModelScope.launch { flow.load() }
    }

    fun join(
        name: String,
        password: String,
    ) {
        viewModelScope.launch { flow.join(name, password) }
    }

    fun joinMediaServer(
        way: SignInWay,
        name: String,
        password: String,
    ) {
        viewModelScope.launch { flow.joinMediaServer(way, name, password) }
    }

    fun startPlex() {
        if (plexJob?.isActive == true) return
        plexJob = viewModelScope.launch { flow.joinPlex() }
    }

    fun cancelPlex() {
        plexJob?.cancel()
        plexJob = null
    }

    fun signOutAndJoin() {
        viewModelScope.launch { flow.signOutAndJoin() }
    }

    override fun onCleared() {
        plexJob?.cancel()
        flow.close()
    }
}
