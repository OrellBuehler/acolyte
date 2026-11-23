package ch.orellbuehler.acolyte.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.live.LiveStatus
import ch.orellbuehler.acolyte.session.SessionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionViewModel(
    private val sdk: AcolyteSdk,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    val state: StateFlow<SessionState> = sdk.session.state
    val live: StateFlow<LiveStatus> = sdk.session.liveStatus

    private var lastRefresh: Instant = clock.now()

    init {
        viewModelScope.launch { sdk.session.restore() }
    }

    fun refresh() {
        val now = clock.now()
        if (state.value !is SessionState.SignedIn || now - lastRefresh < REFRESH_EVERY) return
        lastRefresh = now
        viewModelScope.launch { sdk.session.refresh() }
    }

    fun signOut() {
        viewModelScope.launch { sdk.session.signOut() }
    }

    private companion object {
        val REFRESH_EVERY = 30.seconds
    }
}
