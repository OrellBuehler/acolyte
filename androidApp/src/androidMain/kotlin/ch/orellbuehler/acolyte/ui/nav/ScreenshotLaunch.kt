package ch.orellbuehler.acolyte.ui.nav

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.BuildConfig
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Debug builds only: lets scripts/screenshots/android.sh sign in to the demo server and open a
 * screen from the launch intent (extras screenshot_server, screenshot_user, screenshot_password,
 * screenshot_open = a section name or title:<kind>:<tmdb>). Release builds ignore all of it.
 */
object ScreenshotLaunch {
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

    internal fun route(target: String): Any? {
        val parts = target.split(':')
        if (parts.size == 3 && parts[0] == "title") return Routes.title(parts[1], parts[2])
        return Section.entries
            .firstOrNull { it.name.equals(target, ignoreCase = true) }
            ?.destination
            ?.route
    }

    @Composable
    internal fun Navigate(nav: NavHostController) {
        if (!BuildConfig.DEBUG) return
        val target by pending.collectAsState()
        LaunchedEffect(target) {
            val current = target ?: return@LaunchedEffect
            pending.value = null
            route(current)?.let { nav.navigate(it) { launchSingleTop = true } }
        }
    }
}
