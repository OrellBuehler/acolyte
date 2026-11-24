package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.server.SettingsLogsContent

@Composable
fun SettingsLogsScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsLogsContent(onBack)
}
