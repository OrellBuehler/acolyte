package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold

@Composable
fun SettingsNamingScreen(
    account: Account,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.acq_page_naming), onBack = onBack) { padding ->
        EmptyView(
            title = stringResource(R.string.placeholder_title),
            message = stringResource(R.string.placeholder_message),
            icon = Icons.Rounded.Settings,
            modifier = Modifier.padding(padding),
        )
    }
}
