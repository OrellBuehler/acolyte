package ch.orellbuehler.acolyte.tv.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.session.SessionState
import ch.orellbuehler.acolyte.tv.signin.TvSignInScreen
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvTheme
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.SessionViewModel
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
fun TvApp(
    sdk: AcolyteSdk,
    session: SessionViewModel,
) {
    val state by session.state.collectAsStateWithLifecycle()
    TvTheme {
        CompositionLocalProvider(LocalSdk provides sdk) {
            Box(Modifier.fillMaxSize().background(TvColors.background)) {
                when (val current = state) {
                    SessionState.Restoring -> TvSplash()
                    is SessionState.SignedOut -> TvSignInScreen(expired = current.expired)
                    is SessionState.SignedIn -> TvMain(current.account, onSignOut = session::signOut)
                }
            }
        }
    }
}

@Composable
fun TvBrandMark(
    modifier: Modifier = Modifier,
    size: Int = 80,
) {
    Image(
        painter = painterResource(CommonR.drawable.ic_brand_mark),
        contentDescription = null,
        modifier = modifier.size(size.dp).clip(RoundedCornerShape((size * 0.22f).dp)),
    )
}

@Composable
private fun TvSplash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { TvBrandMark(size = 80) }
}
