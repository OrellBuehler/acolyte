package ch.orellbuehler.acolyte.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SessionState
import ch.orellbuehler.acolyte.ui.connect.ConnectScreen
import ch.orellbuehler.acolyte.ui.shell.MainShell
import ch.orellbuehler.acolyte.ui.theme.AcolyteTheme
import ch.orellbuehler.acolyte.ui.theme.isDark
import ch.orellbuehler.acolyte.ui.wizard.FirstRunScreen

val LocalSdk = staticCompositionLocalOf<AcolyteSdk> { error("AcolyteSdk is not provided") }

@Composable
fun AcolyteApp(
    sdk: AcolyteSdk,
    session: SessionViewModel,
) {
    val state by session.state.collectAsStateWithLifecycle()
    AcolyteTheme {
        SystemBarsEffect()
        CompositionLocalProvider(LocalSdk provides sdk) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (val current = state) {
                    SessionState.Restoring -> Splash()
                    is SessionState.SignedOut -> ConnectScreen(expired = current.expired)
                    is SessionState.SignedIn ->
                        if (current.account.setupOpen && current.account.role == Role.OWNER) {
                            FirstRunScreen()
                        } else {
                            MainShell(current.account, session)
                        }
                }
            }
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.ic_brand_mark),
            contentDescription = null,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)),
        )
    }
}

@Composable
private fun SystemBarsEffect() {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val dark = MaterialTheme.isDark
    val scrim =
        MaterialTheme.colorScheme.surfaceContainer
            .copy(alpha = 0.9f)
            .toArgb()
    DisposableEffect(dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(scrim, scrim) { dark },
        )
        onDispose { }
    }
}
