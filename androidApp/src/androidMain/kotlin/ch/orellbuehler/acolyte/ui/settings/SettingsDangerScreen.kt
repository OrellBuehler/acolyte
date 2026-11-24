package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.R

@Composable
fun SettingsDangerScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    SettingsPagePlaceholder(R.string.settings_page_danger, onBack)
}
