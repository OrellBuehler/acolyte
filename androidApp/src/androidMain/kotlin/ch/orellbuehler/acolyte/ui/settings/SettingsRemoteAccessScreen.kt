package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold

@Composable
fun SettingsRemoteAccessScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val uri = LocalUriHandler.current
    val url = remember(sdk) { sdk.settingsRemoteAccessUrl() }
    ScreenScaffold(title = stringResource(R.string.settings_page_remote_access), onBack = onBack) { padding ->
        MessageView(
            icon = Icons.Rounded.Public,
            title = stringResource(R.string.settings_keys_remote_title),
            message = stringResource(R.string.settings_keys_remote_text),
            action = if (url.isNotEmpty()) stringResource(R.string.settings_keys_remote_open) else null,
            onAction = { runCatching { uri.openUri(url) } },
            modifier = Modifier.padding(padding),
        )
    }
}
