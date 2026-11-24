package ch.orellbuehler.acolyte.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.session.InviteLink
import ch.orellbuehler.acolyte.session.InviteTarget
import ch.orellbuehler.acolyte.session.SignInFlow
import ch.orellbuehler.acolyte.session.SignInState
import ch.orellbuehler.acolyte.session.SignInStep
import ch.orellbuehler.acolyte.session.SignInWay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectViewModel(
    private val sdk: AcolyteSdk,
) : ViewModel() {
    private val flow = MutableStateFlow(sdk.signIn())
    private var plexJob: Job? = null
    private val mutableInvite = MutableStateFlow<InviteTarget?>(null)
    private val mutablePicture = MutableStateFlow<ByteArray?>(null)

    val state: StateFlow<SignInState> =
        flow.flatMapLatest { it.state }.stateIn(viewModelScope, SharingStarted.Eagerly, flow.value.state.value)

    val invite: StateFlow<InviteTarget?> = mutableInvite.asStateFlow()
    val picture: StateFlow<ByteArray?> = mutablePicture.asStateFlow()

    fun restartIfDone() {
        if (flow.value.state.value.step != SignInStep.DONE) return
        flow.value.close()
        flow.value = sdk.signIn()
        mutablePicture.value = null
    }

    fun connect(address: String) = run { connect(address) }

    fun signIn(
        name: String,
        password: String,
        way: SignInWay,
    ) = run { signIn(name, password, way) }

    fun useApiKey(key: String) = run { useApiKey(key) }

    fun verify(code: String) = run { verify(code) }

    fun createOwner(
        name: String,
        displayName: String,
        password: String,
    ) {
        val photo = mutablePicture.value
        run { createOwner(name, displayName, password, photo) }
    }

    fun setPicture(bytes: ByteArray?) {
        mutablePicture.value = bytes
    }

    fun startPlex() {
        if (plexJob?.isActive == true) return
        val current = flow.value
        plexJob = viewModelScope.launch { current.signInWithPlex() }
    }

    fun cancelPlex() {
        plexJob?.cancel()
        plexJob = null
    }

    fun startReset() = flow.value.startReset()

    fun makeResetCode() = run { makeResetCode() }

    fun checkResetCode(code: String) = run { checkResetCode(code) }

    fun finishReset(
        code: String,
        name: String,
        password: String,
    ) = run { finishReset(code, name, password) }

    fun back() {
        if (plexJob?.isActive == true) {
            cancelPlex()
            return
        }
        flow.value.back()
    }

    fun clearError() = flow.value.clearError()

    fun openInvite(text: String): Boolean {
        val target = InviteLink.parse(text) ?: return false
        mutableInvite.value = target
        return true
    }

    fun closeInvite() {
        mutableInvite.value = null
    }

    private fun run(block: suspend SignInFlow.() -> Unit) {
        val current = flow.value
        viewModelScope.launch { current.block() }
    }

    override fun onCleared() {
        plexJob?.cancel()
        flow.value.close()
    }
}
