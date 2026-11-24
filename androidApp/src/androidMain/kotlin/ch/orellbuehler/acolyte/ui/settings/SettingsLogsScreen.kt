package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.R

@Composable
fun SettingsLogsScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsPagePlaceholder(R.string.settings_page_logs, onBack)
}
