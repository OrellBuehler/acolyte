package ch.orellbuehler.acolyte.ui

import androidx.compose.runtime.staticCompositionLocalOf
import ch.orellbuehler.acolyte.AcolyteSdk

val LocalSdk = staticCompositionLocalOf<AcolyteSdk> { error("AcolyteSdk is not provided") }
