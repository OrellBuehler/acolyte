package ch.orellbuehler.acolyte.ui.settings.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInJoin
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInLinkFailure
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInLinkPhase
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInPlex
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInState
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInWayKind
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
internal fun WayCard(
    state: SettingsSignInState,
    kind: SettingsSignInWayKind,
    model: SignInModel,
) {
    val way = state.way(kind)
    val owner = state.isOwner
    val plex = kind == SettingsSignInWayKind.PLEX
    SettingsCard(stringResource(R.string.settings_signin_sign_in_with, kind.app)) {
        Hint(
            if (plex) {
                stringResource(R.string.settings_signin_plex_intro)
            } else {
                stringResource(R.string.settings_signin_other_intro, kind.app)
            },
        )
        SwitchRow(
            title = stringResource(R.string.settings_signin_show_button, kind.app),
            checked = way.on,
            onChange = { model.presenter.setWayOn(kind, it) },
            enabled = owner,
        )
        if (plex) PlexLinkSection(state, model) else AddressSection(state, kind, model)
        PickerField(
            label = stringResource(R.string.settings_signin_who_join),
            options = joinOptions(kind),
            selected = way.join,
            onSelect = { model.presenter.setWayJoin(kind, it) },
            enabled = owner,
            hint = stringResource(if (plex) R.string.settings_signin_join_hint_plex else R.string.settings_signin_join_hint),
        )
        if (way.join == SettingsSignInJoin.LIST) {
            SignInArea(
                label = stringResource(R.string.settings_signin_names),
                value = way.allow,
                onValueChange = { model.presenter.setWayAllow(kind, it) },
                enabled = owner,
                mono = false,
            )
        }
        if (owner) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Button(onClick = { model.run { saveWay(kind) } }, enabled = !way.saving) {
                    Text(stringResource(R.string.settings_signin_save))
                }
                OutlinedButton(onClick = { model.run { openImport(kind) } }, enabled = state.canImport(kind)) {
                    Text(stringResource(R.string.settings_signin_import_members))
                }
                if (way.saving) SmallSpinner()
            }
            if (plex && !state.plex.canImport) Hint(stringResource(R.string.settings_signin_link_first))
        } else {
            Hint(stringResource(R.string.settings_signin_owner_only))
        }
    }
}

@Composable
private fun AddressSection(
    state: SettingsSignInState,
    kind: SettingsSignInWayKind,
    model: SignInModel,
) {
    val way = state.way(kind)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        SignInField(
            label = stringResource(R.string.settings_signin_server_address, kind.app),
            value = way.url,
            onValueChange = { model.presenter.setWayUrl(kind, it) },
            enabled = state.isOwner,
            placeholder = "http://10.0.0.5:8096",
            mono = true,
            keyboard = KeyboardType.Uri,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            OutlinedButton(
                onClick = { model.run { testWay(kind) } },
                enabled = state.isOwner && way.url.isNotBlank() && !way.testing,
            ) { Text(stringResource(R.string.settings_signin_test)) }
            if (way.testing) SmallSpinner()
        }
        val test = way.test
        if (test != null) {
            TestResult(test, okLabel = test.text)
        } else if (way.untested) {
            InlineNotice(stringResource(R.string.settings_signin_not_tested))
        }
    }
}

@Composable
private fun PlexLinkSection(
    state: SettingsSignInState,
    model: SignInModel,
) {
    val plex = state.plex
    val owner = state.isOwner
    var unlink by rememberSaveable { mutableStateOf(false) }
    var typeId by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        if (plex.linked) {
            Text(stringResource(R.string.settings_signin_linked_to, plex.name))
        } else {
            Hint(stringResource(R.string.settings_signin_link_note))
        }
        if (owner) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                if (plex.linked) {
                    OutlinedButton(onClick = { unlink = true }, enabled = !plex.unlinking && !plex.linking) {
                        Text(stringResource(R.string.settings_signin_unlink))
                    }
                }
                Button(onClick = { model.run { linkPlex() } }, enabled = !plex.linking && !plex.unlinking) {
                    Text(stringResource(if (plex.linked) R.string.settings_signin_link_again else R.string.settings_signin_link_mine))
                }
            }
        }
        PlexStatus(plex, model)
        val way = state.way(SettingsSignInWayKind.PLEX)
        if (plex.servers.isNotEmpty()) {
            val known = plex.servers.map { it.id to it.name }
            val options =
                buildList {
                    add("" to stringResource(R.string.settings_signin_any_server))
                    addAll(known)
                    if (way.serverId.isNotEmpty() && known.none { it.first == way.serverId }) add(way.serverId to way.serverId)
                }
            PickerField(
                label = stringResource(R.string.settings_signin_server_share),
                options = options,
                selected = way.serverId,
                onSelect = { model.presenter.setWayServerId(SettingsSignInWayKind.PLEX, it) },
                enabled = owner,
            )
        }
        TextButton(onClick = { typeId = !typeId }) { Text(stringResource(R.string.settings_signin_type_id)) }
        if (typeId || (plex.servers.isEmpty() && way.serverId.isNotEmpty())) {
            SignInField(
                label = stringResource(R.string.settings_signin_server_id),
                value = way.serverId,
                onValueChange = { model.presenter.setWayServerId(SettingsSignInWayKind.PLEX, it) },
                enabled = owner,
                hint = stringResource(R.string.settings_signin_server_id_hint),
                mono = true,
            )
        }
    }
    if (unlink) {
        ConfirmDialog(
            title = stringResource(R.string.settings_signin_unlink_title),
            text = stringResource(R.string.settings_signin_unlink_text),
            confirm = stringResource(R.string.settings_signin_unlink),
            dismiss = stringResource(R.string.settings_signin_cancel),
            onConfirm = {
                unlink = false
                model.run { unlinkPlex() }
            },
            onDismiss = { unlink = false },
            danger = true,
        )
    }
}

@Composable
private fun PlexStatus(
    plex: SettingsSignInPlex,
    model: SignInModel,
) {
    val uriHandler = LocalUriHandler.current
    when (plex.phase) {
        SettingsSignInLinkPhase.IDLE -> Unit
        SettingsSignInLinkPhase.STARTING ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SmallSpinner()
                Hint(stringResource(R.string.settings_signin_link_starting))
            }
        SettingsSignInLinkPhase.WAITING ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    SmallSpinner()
                    Hint(stringResource(R.string.settings_signin_link_confirm), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    TextButton(onClick = { runCatching { uriHandler.openUri(plex.authUrl) } }) {
                        Text(stringResource(R.string.settings_signin_link_open))
                    }
                    TextButton(onClick = model.presenter::cancelLink) { Text(stringResource(R.string.settings_signin_link_stop)) }
                }
            }
        SettingsSignInLinkPhase.LINKED_PICK -> Hint(stringResource(R.string.settings_signin_linked_pick))
        SettingsSignInLinkPhase.LINKED_NONE -> Hint(stringResource(R.string.settings_signin_linked_none))
        SettingsSignInLinkPhase.TIMED_OUT -> FailText(stringResource(R.string.settings_signin_link_timed_out))
        SettingsSignInLinkPhase.FAILED ->
            FailText(
                when (plex.failure) {
                    SettingsSignInLinkFailure.ENDED -> stringResource(R.string.settings_signin_link_ended)
                    SettingsSignInLinkFailure.TAKEN -> stringResource(R.string.settings_signin_link_taken)
                    SettingsSignInLinkFailure.TOO_MANY -> stringResource(R.string.settings_signin_link_too_many)
                    SettingsSignInLinkFailure.NEEDS_PASSWORD -> stringResource(R.string.settings_signin_sign_in_to_change)
                    SettingsSignInLinkFailure.OTHER -> plex.error?.let { failureText(it) }.orEmpty()
                },
            )
    }
}
