package ch.orellbuehler.acolyte.ui.settings.signin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInState
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
internal fun PasswordsCard(
    state: SettingsSignInState,
    model: SignInModel,
) {
    SettingsCard(stringResource(R.string.settings_signin_passwords)) {
        SignInField(
            label = stringResource(R.string.settings_signin_shortest),
            value = state.minLengthText,
            onValueChange = model.presenter::setMinLength,
            enabled = state.isOwner,
            hint = stringResource(R.string.settings_signin_shortest_hint),
            keyboard = KeyboardType.Number,
        )
        if (state.shortPasswordWarning) InlineNotice(stringResource(R.string.settings_signin_short_warn))
        if (state.isOwner) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Button(onClick = { model.run { saveMinLength() } }, enabled = state.canSaveMinLength) {
                    Text(stringResource(R.string.settings_signin_save))
                }
                if (state.minLengthSaving) SmallSpinner()
            }
        } else {
            Hint(stringResource(R.string.settings_signin_owner_only))
        }
    }
}

@Composable
internal fun TwoFactorCard(
    state: SettingsSignInState,
    model: SignInModel,
) {
    SettingsCard(stringResource(R.string.settings_signin_two_factor)) {
        SwitchRow(
            title = stringResource(R.string.settings_signin_admins_must),
            checked = state.requireTwoFactor,
            onChange = { model.run { setRequireTwoFactor(it) } },
            enabled = state.isOwner && !state.requireTwoFactorSaving,
        )
        Hint(
            stringResource(R.string.settings_signin_admins_note) +
                if (state.isOwner) "" else " " + stringResource(R.string.settings_signin_only_owner_change),
        )
    }
}

@Composable
internal fun ForgotCard(
    state: SettingsSignInState,
    model: SignInModel,
) {
    SettingsCard(stringResource(R.string.settings_signin_forgot_title)) {
        SwitchRow(
            title = stringResource(R.string.settings_signin_forgot_on),
            checked = state.forgotLink,
            onChange = { model.run { setForgotLink(it) } },
            enabled = state.isOwner && !state.forgotLinkSaving,
        )
        Hint(
            stringResource(R.string.settings_signin_forgot_note) +
                if (state.isOwner) "" else " " + stringResource(R.string.settings_signin_only_owner_change),
        )
    }
}

@Composable
internal fun ApiKeysRow(onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen).clickable(onClick = onOpen),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_signin_api_keys)) },
            supportingContent = { Text(stringResource(R.string.settings_signin_api_keys_note)) },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        )
    }
}

@Composable
internal fun AdvancedHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.settings_signin_advanced),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onToggle) {
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
        }
    }
}

@Composable
internal fun HostsCard(
    state: SettingsSignInState,
    model: SignInModel,
) {
    val editable = state.isOwner && !state.apiKey
    SettingsCard(stringResource(R.string.settings_signin_hosts)) {
        Hint(stringResource(R.string.settings_signin_hosts_hint))
        SignInArea(
            label = stringResource(R.string.settings_signin_host_names),
            value = state.hosts.text,
            onValueChange = model.presenter::setHosts,
            enabled = editable,
        )
        if (state.apiKey) Hint(stringResource(R.string.settings_signin_hosts_needs_session))
        if (state.isOwner) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Button(onClick = { model.run { saveHosts() } }, enabled = editable && !state.hosts.saving) {
                    Text(stringResource(R.string.settings_signin_save))
                }
                if (state.hosts.saving) SmallSpinner()
            }
        } else {
            Hint(stringResource(R.string.settings_signin_owner_only))
        }
    }
}

@Composable
internal fun ProxiesInfoCard() {
    SettingsCard(stringResource(R.string.settings_signin_proxies)) {
        Hint(stringResource(R.string.settings_signin_proxies_info))
    }
}
