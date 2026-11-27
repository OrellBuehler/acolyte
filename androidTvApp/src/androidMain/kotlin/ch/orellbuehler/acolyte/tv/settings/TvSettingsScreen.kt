package ch.orellbuehler.acolyte.tv.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvConfirmDialog
import ch.orellbuehler.acolyte.tv.nav.LocalTvSignOut
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType

@Composable
fun TvSettingsScreen(account: Account) {
    val signOut = LocalTvSignOut.current
    var confirm by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = TvMetrics.marginX, vertical = TvMetrics.sectionGap),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.widthIn(max = TvMetrics.readableWidth), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsRow(stringResource(R.string.tv_settings_server), account.serverName.ifEmpty { account.serverUrl })
            SettingsRow(stringResource(R.string.tv_settings_account), account.displayName.ifEmpty { account.userName })
        }
        TvActionButton(stringResource(R.string.tv_settings_sign_out), onClick = { confirm = true })
        Text(stringResource(R.string.tv_settings_privacy), style = TvType.footnote, color = TvColors.textMuted)
    }
    if (confirm) {
        TvConfirmDialog(
            title = stringResource(R.string.tv_account_sign_out_title),
            message = stringResource(R.string.tv_account_sign_out_message),
            confirm = stringResource(R.string.tv_account_sign_out),
            cancel = stringResource(R.string.tv_common_cancel),
            onConfirm = {
                confirm = false
                signOut()
            },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = TvType.callout, color = TvColors.textMuted)
        Text(value, style = TvType.callout, color = TvColors.text)
    }
}
