package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.look.SettingsLookAppearanceScreen

@Composable
fun SettingsAppearanceScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsLookAppearanceScreen(onBack, onOpen)
}
