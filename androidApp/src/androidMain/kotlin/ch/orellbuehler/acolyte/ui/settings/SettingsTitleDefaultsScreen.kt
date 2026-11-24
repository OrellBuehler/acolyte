package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.look.SettingsLookDefaultsScreen

@Composable
fun SettingsTitleDefaultsScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsLookDefaultsScreen(onBack, onOpen)
}
