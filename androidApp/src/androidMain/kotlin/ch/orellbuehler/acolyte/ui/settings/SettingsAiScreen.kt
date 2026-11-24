package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.keys.SettingsAiPage

@Composable
fun SettingsAiScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsAiPage(onBack)
}
