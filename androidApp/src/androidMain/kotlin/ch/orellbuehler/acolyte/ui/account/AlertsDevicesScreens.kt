package ch.orellbuehler.acolyte.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.AccountSession
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.account.AccountSaved
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun AlertsScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val vm = viewModel { AccountProfileViewModel(sdk) }
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var discord by rememberSaveable { mutableStateOf("") }
    var episodes by rememberSaveable { mutableStateOf(true) }
    val alerts = state.alerts
    LaunchedEffect(alerts) {
        email = alerts.email
        discord = alerts.discordId
        episodes = alerts.newEpisodes ?: true
    }
    val dirty = email.trim() != alerts.email || discord.trim() != alerts.discordId || episodes != state.newEpisodes
    val savedText =
        when (state.saved) {
            AccountSaved.ALERTS -> stringResource(R.string.myaccount_alerts_saved)
            AccountSaved.UNFOLLOWED -> stringResource(R.string.myaccount_unfollowed)
            else -> null
        }
    ToastOnce(savedText) { vm.consume() }

    ScreenScaffold(title = stringResource(R.string.myaccount_alerts), onBack = onBack) { padding ->
        StatusContent(state.status, isEmpty = false, onRefresh = { vm.refresh() }, modifier = Modifier.padding(padding)) {
            FormColumn(PaddingValuesZero) {
                state.actionError?.let { ErrorBanner(it) }
                FormCard(stringResource(R.string.myaccount_alerts)) {
                    Text(stringResource(R.string.myaccount_alerts_hint), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.myaccount_email)) },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = discord,
                        onValueChange = { discord = it },
                        label = { Text(stringResource(R.string.myaccount_discord)) },
                        supportingText = { Text(stringResource(R.string.myaccount_discord_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LabeledRow(stringResource(R.string.myaccount_new_episodes)) {
                        Switch(checked = episodes, onCheckedChange = {
                            episodes =
                                it
                        })
                    }
                    Button(enabled = dirty && !state.busy, onClick = { vm.saveAlerts(email, discord, episodes) }) {
                        Text(stringResource(R.string.myaccount_save))
                    }
                }
                if (state.followsOn) {
                    FormCard(stringResource(R.string.myaccount_following)) {
                        if (state.follows.isEmpty()) {
                            Text(stringResource(R.string.myaccount_following_none), style = MaterialTheme.typography.bodyMedium)
                        }
                        state.follows.forEach { follow ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        follow.name.ifEmpty { follow.tmdbId.ifEmpty { follow.mbid } },
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    val kind =
                                        when (follow.kind) {
                                            "movie" -> R.string.myaccount_kind_movie
                                            "artist" -> R.string.myaccount_kind_artist
                                            "album" -> R.string.myaccount_kind_album
                                            else -> R.string.myaccount_kind_show
                                        }
                                    Text(
                                        listOfNotNull(
                                            stringResource(kind),
                                            follow.year.takeIf { it > 0 }?.toString(),
                                            follow.artist.ifEmpty { null },
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { vm.unfollow(follow) }) { Text(stringResource(R.string.myaccount_unfollow)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevicesScreen(
    userId: Long?,
    title: String,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val vm = viewModel(key = "devices-$userId") { AccountDevicesViewModel(sdk, userId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val ended = state.ended
    val text =
        when {
            ended == null -> null
            ended == 0 -> stringResource(R.string.myaccount_devices_none_ended)
            else -> pluralStringResource(R.plurals.myaccount_devices_ended, ended, ended)
        }
    ToastOnce(text) { vm.consume() }
    ScreenScaffold(title = title, onBack = onBack) { padding ->
        StatusContent(
            state.status,
            isEmpty = state.sessions.isEmpty(),
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding),
        ) {
            FormColumn(PaddingValuesZero) {
                state.actionError?.let { ErrorBanner(it) }
                FormCard(stringResource(R.string.myaccount_devices)) {
                    state.ordered.forEachIndexed { index, session ->
                        if (index > 0) HorizontalDivider()
                        DeviceRow(session, userId == null, enabled = !state.busy) { vm.end(session) }
                    }
                }
                if (userId != null || state.hasOthers) {
                    OutlinedButton(enabled = !state.busy, onClick = { vm.endAll() }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                if (userId ==
                                    null
                                ) {
                                    R.string.myaccount_devices_end_others
                                } else {
                                    R.string.myaccount_devices_end_all
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    session: AccountSession,
    mine: Boolean,
    enabled: Boolean,
    onEnd: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Column(Modifier.weight(1f)) {
            Text(session.device.ifEmpty { stringResource(R.string.myaccount_device_unknown) }, style = MaterialTheme.typography.bodyLarge)
            val used = relativeTime(session.lastUsedAt)
            Text(
                stringResource(R.string.myaccount_device_used, used ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (session.current && mine) {
            StatusChip(stringResource(R.string.myaccount_device_this), Tone.OK)
        } else {
            TextButton(enabled = enabled, onClick = onEnd) { Text(stringResource(R.string.myaccount_device_sign_out)) }
        }
    }
}
