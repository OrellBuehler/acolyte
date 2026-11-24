package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.server.SettingsBackupsContent

@Composable
fun SettingsBackupsScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsBackupsContent(onBack)
}
