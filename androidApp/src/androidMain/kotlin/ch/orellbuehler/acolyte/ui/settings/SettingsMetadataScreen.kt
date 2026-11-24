package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.look.SettingsLookMetadataScreen

@Composable
fun SettingsMetadataScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsLookMetadataScreen(onBack, onOpen)
}
