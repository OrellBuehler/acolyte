package ch.orellbuehler.acolyte.tv.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.session.SignInFlow
import ch.orellbuehler.acolyte.session.SignInState
import ch.orellbuehler.acolyte.session.SignInStep
import ch.orellbuehler.acolyte.session.SignInWay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TvSignInViewModel(
    private val sdk: AcolyteSdk,
) : ViewModel() {
    private val flow = MutableStateFlow(sdk.signIn())

    val state: StateFlow<SignInState> =
        flow.flatMapLatest { it.state }.stateIn(viewModelScope, SharingStarted.Eagerly, flow.value.state.value)

    fun restartIfDone() {
        if (flow.value.state.value.step != SignInStep.DONE) return
        flow.value.close()
        flow.value = sdk.signIn()
    }

    fun connect(address: String) = run { connect(address) }

    fun signIn(
        name: String,
        password: String,
        way: SignInWay,
    ) = run { signIn(name, password, way) }

    fun verify(code: String) = run { verify(code) }

    fun back() = flow.value.back()

    private fun run(block: suspend SignInFlow.() -> Unit) {
        val current = flow.value
        viewModelScope.launch { current.block() }
    }

    override fun onCleared() {
        flow.value.close()
    }
}
