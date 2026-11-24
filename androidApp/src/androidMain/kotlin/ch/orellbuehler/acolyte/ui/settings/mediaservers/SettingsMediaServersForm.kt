package ch.orellbuehler.acolyte.ui.settings.mediaservers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersForm
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersState
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SheetFooter
import ch.orellbuehler.acolyte.ui.theme.Spacing

private const val MAX_NAME = 80

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaServerFormSheet(
    state: SettingsMediaServersState,
    viewModel: SettingsMediaServersViewModel,
    onOpenSignIn: () -> Unit,
) {
    val form = state.form ?: return
    val revision = state.formRevision
    ModalBottomSheet(onDismissRequest = viewModel::cancelForm, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight().imePadding()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                Text(
                    if (form.isNew) {
                        stringResource(R.string.settings_mediaservers_add)
                    } else {
                        stringResource(R.string.settings_mediaservers_change_title, form.originalName)
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                MediaServerField(
                    form.name,
                    revision,
                    viewModel::setName,
                    stringResource(R.string.settings_mediaservers_name),
                    maxLength = MAX_NAME,
                )
                if (form.isNew) {
                    PickerField(
                        label = stringResource(R.string.settings_mediaservers_kind),
                        options = state.kinds.map { it.wire to it.name },
                        selected = form.kind,
                        onSelect = viewModel::setKind,
                    )
                }
                if (state.showsPlexPicker) PlexPicker(state, viewModel, onOpenSignIn)
                MediaServerField(
                    form.url,
                    revision,
                    viewModel::setUrl,
                    stringResource(R.string.settings_mediaservers_address),
                    placeholder = form.urlExample,
                    mono = true,
                    keyboardType = KeyboardType.Uri,
                )
                if (form.takesUsername) {
                    MediaServerField(
                        form.username,
                        revision,
                        viewModel::setUsername,
                        stringResource(R.string.settings_mediaservers_user_name),
                        mono = true,
                    )
                }
                if (form.showsToken) SecretField(form, revision, viewModel)
                if (form.isSubsonic) {
                    MediaServerCheckRow(
                        form.plainPassword,
                        viewModel::setPlainPassword,
                        stringResource(R.string.settings_mediaservers_plain),
                        hint = stringResource(R.string.settings_mediaservers_plain_hint),
                    )
                }
                if (form.takesWebUrl) {
                    MediaServerField(
                        form.webUrl,
                        revision,
                        viewModel::setWebUrl,
                        stringResource(R.string.settings_mediaservers_web_url),
                        hint = stringResource(R.string.settings_mediaservers_web_url_hint),
                        placeholder = "https://jellyfin.example.com",
                        mono = true,
                        keyboardType = KeyboardType.Uri,
                    )
                }
                Text(stringResource(R.string.settings_mediaservers_after), style = MaterialTheme.typography.titleSmall)
                MediaServerCheckRow(form.onImport, viewModel::setOnImport, stringResource(R.string.settings_mediaservers_an_import))
                MediaServerCheckRow(form.onRename, viewModel::setOnRename, stringResource(R.string.settings_mediaservers_a_rename))
                MediaServerCheckRow(
                    form.onDelete,
                    viewModel::setOnDelete,
                    stringResource(R.string.settings_mediaservers_a_delete),
                    hint = stringResource(R.string.settings_mediaservers_a_delete_hint),
                )
                if (form.takesPathMap) {
                    MediaServerField(
                        form.pathFrom,
                        revision,
                        viewModel::setPathFrom,
                        stringResource(R.string.settings_mediaservers_path_from),
                        placeholder = "/data/movies",
                        mono = true,
                    )
                    MediaServerField(
                        form.pathTo,
                        revision,
                        viewModel::setPathTo,
                        stringResource(R.string.settings_mediaservers_path_to),
                        hint = stringResource(R.string.settings_mediaservers_path_hint),
                        placeholder = "/movies",
                        mono = true,
                    )
                }
                MediaServerCheckRow(
                    form.allowLocalNetwork,
                    viewModel::setAllowLocalNetwork,
                    stringResource(R.string.settings_mediaservers_local),
                )
                MediaServerCheckRow(form.enabled, viewModel::setEnabled, stringResource(R.string.settings_mediaservers_send))
                state.formError?.let { error ->
                    FailText(errorText(error))
                    if (state.formDetail.isNotEmpty()) FailText(state.formDetail)
                }
            }
            Column(Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Medium)) {
                SheetFooter(
                    primary = stringResource(R.string.settings_mediaservers_save),
                    enabled = state.canSave,
                    busy = state.saving,
                    onCancel = viewModel::cancelForm,
                    onPrimary = viewModel::save,
                )
            }
        }
    }
}

@Composable
private fun SecretField(
    form: SettingsMediaServersForm,
    revision: Int,
    viewModel: SettingsMediaServersViewModel,
) {
    val label =
        when {
            form.isPlex -> R.string.settings_mediaservers_plex_token
            form.takesUsername -> R.string.settings_mediaservers_password
            else -> R.string.settings_mediaservers_api_key
        }
    val hint =
        when {
            form.isPlex -> R.string.settings_mediaservers_plex_token_hint
            form.isSubsonic -> R.string.settings_mediaservers_subsonic_hint
            form.isKodi -> R.string.settings_mediaservers_kodi_hint
            else -> R.string.settings_mediaservers_api_key_hint
        }
    MediaServerField(
        form.token,
        revision,
        viewModel::setToken,
        stringResource(label),
        hint = stringResource(hint),
        placeholder = if (form.tokenStored) stringResource(R.string.settings_mediaservers_stored) else null,
        secret = true,
    )
}

@Composable
private fun PlexPicker(
    state: SettingsMediaServersState,
    viewModel: SettingsMediaServersViewModel,
    onOpenSignIn: () -> Unit,
) {
    val plex = state.plex
    val form = state.form ?: return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = viewModel::findPlexServers, enabled = !plex.finding) {
                if (plex.finding) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Spacing.Small))
                }
                Text(stringResource(R.string.settings_mediaservers_plex_find))
            }
            if (plex.searched) Hint(stringResource(R.string.settings_mediaservers_plex_found, plex.rows.size), Modifier.weight(1f))
        }
        if (plex.notLinked) {
            Hint(stringResource(R.string.settings_mediaservers_plex_not_linked))
            TextButton(onClick = onOpenSignIn) { Text(stringResource(R.string.settings_mediaservers_plex_open_sign_in)) }
        }
        plex.error?.let { error ->
            FailText(errorText(error))
            if (error.status > 0 && error.message.isNotBlank()) FailText(error.message)
        }
        if (plex.rows.isNotEmpty()) {
            val manual = stringResource(R.string.settings_mediaservers_plex_manual)
            val options =
                plex.rows.mapIndexed { index, row ->
                    val place =
                        stringResource(
                            when (row.place) {
                                "relay" -> R.string.settings_mediaservers_plex_relay
                                "local" -> R.string.settings_mediaservers_plex_local
                                else -> R.string.settings_mediaservers_plex_remote
                            },
                        )
                    index to "${row.name} · ${row.address} · $place"
                } + (-1 to manual)
            PickerField(
                label = stringResource(R.string.settings_mediaservers_plex_pick),
                options = options,
                selected = plex.picked,
                onSelect = viewModel::pickPlex,
            )
            if (plex.addresses.size > 1) {
                PickerField(
                    label = stringResource(R.string.settings_mediaservers_plex_address),
                    options = plex.addresses.map { it to it },
                    selected = form.url,
                    onSelect = viewModel::pickPlexAddress,
                )
            }
            if (form.usePlexSignIn) Hint(stringResource(R.string.settings_mediaservers_plex_uses_sign_in))
        }
    }
}
