package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.keys.SettingsApiPage

@Composable
fun SettingsApiScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsApiPage(onBack)
}
