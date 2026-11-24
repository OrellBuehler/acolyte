package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.server.SettingsAlertsContent

@Composable
fun SettingsAlertsScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsAlertsContent(onBack)
}
