package ch.orellbuehler.acolyte.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.live.LiveStatus
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun AccountScreen(
    account: Account,
    onSignOut: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val live by LocalSdk.current.session.liveStatus
        .collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.section_account), onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.Screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                ServerImage(
                    source = account.avatarPath,
                    contentDescription = null,
                    size = ImageSize.THUMB,
                    placeholderName = account.displayName,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                )
                Column {
                    Text(account.displayName, style = MaterialTheme.typography.headlineSmall)
                    Text(account.userName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (account.mustSetUpTwoFactor) NoticeBanner(stringResource(R.string.account_two_factor_needed))
            if (account.safeMode) NoticeBanner(stringResource(R.string.account_safe_mode))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                InfoRow(stringResource(R.string.account_server), account.serverName)
                account.serverVersion?.let {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow(stringResource(R.string.account_version), it)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(stringResource(R.string.account_role), stringResource(account.role.label()))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(stringResource(R.string.account_method), stringResource(account.method.label()))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.account_live), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    StatusChip(stringResource(live.label()), live.tone())
                }
            }
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_sign_out))
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.account_sign_out_title)) },
            text = { Text(stringResource(R.string.account_sign_out_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onSignOut()
                }) { Text(stringResource(R.string.account_sign_out)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth().padding(Spacing.Large), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Role.label(): Int =
    when (this) {
        Role.OWNER -> R.string.role_owner
        Role.ADMIN -> R.string.role_admin
        Role.MEMBER -> R.string.role_member
        Role.UNKNOWN -> R.string.role_unknown
    }

private fun SignInMethod.label(): Int =
    when (this) {
        SignInMethod.PASSWORD -> R.string.method_password
        SignInMethod.MEDIA_SERVER -> R.string.method_media_server
        SignInMethod.API_KEY -> R.string.method_api_key
    }

private fun LiveStatus.label(): Int =
    when (this) {
        LiveStatus.OFF -> R.string.live_off
        LiveStatus.CONNECTING -> R.string.live_connecting
        LiveStatus.LIVE -> R.string.live_live
        LiveStatus.RECONNECTING -> R.string.live_reconnecting
    }

private fun LiveStatus.tone(): Tone =
    when (this) {
        LiveStatus.OFF -> Tone.IDLE
        LiveStatus.CONNECTING, LiveStatus.RECONNECTING -> Tone.WAIT
        LiveStatus.LIVE -> Tone.OK
    }
