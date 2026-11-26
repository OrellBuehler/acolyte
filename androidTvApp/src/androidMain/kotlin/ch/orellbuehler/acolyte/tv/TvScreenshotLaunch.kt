package ch.orellbuehler.acolyte.tv

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.SessionState
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.nav.TvNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Debug builds only: lets scripts/screenshots/androidtv.sh sign in to the demo server and open a
 * screen from the launch intent (extras screenshot_server, screenshot_user, screenshot_password,
 * screenshot_open = a section name, title:<kind>:<tmdb>, person:<id> or genre:<type>:<id>).
 * Release builds ignore all of it.
 */
object TvScreenshotLaunch {
    private val pending = MutableStateFlow<String?>(null)

    fun start(
        sdk: AcolyteSdk,
        intent: Intent,
        scope: CoroutineScope,
    ) {
        if (!BuildConfig.DEBUG) return
        val server = intent.getStringExtra("screenshot_server") ?: return
        val user = intent.getStringExtra("screenshot_user").orEmpty()
        val password = intent.getStringExtra("screenshot_password").orEmpty()
        pending.value = intent.getStringExtra("screenshot_open")
        scope.launch {
            sdk.session.state.first { it !is SessionState.Restoring }
            val flow = sdk.signIn()
            flow.connect(server)
            flow.signIn(user, password)
        }
    }

    internal fun open(
        target: String,
        navigator: TvNavigator,
    ) {
        val parts = target.split(':')
        when {
            parts.size == 3 && parts[0] == "title" -> navigator.openTitle(parts[1], parts[2])
            parts.size == 2 && parts[0] == "person" -> navigator.openPerson(parts[1])
            parts.size == 3 && parts[0] == "genre" -> parts[2].toIntOrNull()?.let { navigator.openGenre(parts[1], it) }
            else ->
                Section.entries
                    .firstOrNull { it.name.equals(target, ignoreCase = true) }
                    ?.let { navigator.openSection(it) }
        }
    }

    @Composable
    internal fun Navigate() {
        if (!BuildConfig.DEBUG) return
        val navigator = LocalTvNavigator.current
        val target by pending.collectAsState()
        LaunchedEffect(target) {
            val current = target ?: return@LaunchedEffect
            pending.value = null
            open(current, navigator)
        }
    }
}
