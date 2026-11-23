package ch.orellbuehler.acolyte.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.nav.destination

@Composable
fun PlaceholderScreen(
    section: Section,
    onBack: (() -> Unit)?,
) {
    val destination = section.destination
    ScreenScaffold(title = stringResource(destination.label), onBack = onBack) { padding ->
        EmptyView(
            title = stringResource(R.string.placeholder_title),
            message = stringResource(R.string.placeholder_message),
            icon = destination.icon,
            modifier = Modifier.padding(padding),
        )
    }
}
