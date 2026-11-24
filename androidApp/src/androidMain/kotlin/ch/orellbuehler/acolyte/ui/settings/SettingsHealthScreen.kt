package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.runtime.Composable
import ch.orellbuehler.acolyte.ui.settings.status.HealthChecksPage

@Composable
fun SettingsHealthScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    HealthChecksPage(onBack)
}
