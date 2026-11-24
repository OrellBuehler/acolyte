package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.SettingsBridge
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.sources.SettingsBridgeDraft
import ch.orellbuehler.acolyte.feature.sources.SettingsBridgeFormState
import ch.orellbuehler.acolyte.ui.components.ReauthSheet
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing

private fun bridgeName(kind: String): String = kind.replaceFirstChar { it.uppercase() }

@Composable
fun BridgeCard(model: BridgeViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    ReauthSheet(model.reauth)
    var unlinking by remember { mutableStateOf<SettingsBridge?>(null) }
    SettingsCard(
        title = stringResource(R.string.acq_src_bridge_title),
        subtitle = if (state.bridges.isEmpty()) stringResource(R.string.acq_src_bridge_description) else null,
    ) {
        val loadError = state.status.error
        if (state.status.phase == Phase.FAILED && loadError != null) {
            FailText(errorText(loadError))
            TextButton(onClick = model::refresh) { Text(stringResource(R.string.acq_src_retry)) }
        }
        state.bridges.forEach { bridge ->
            BridgeRow(
                bridge = bridge,
                canEdit = state.canEdit,
                syncing = state.syncing == bridge.id,
                onSync = { model.sync(bridge.id) },
                onChange = { model.startChange(bridge.id) },
                onUnlink = {
                    model.clearRemoveError()
                    unlinking = bridge
                },
            )
        }
        val form = state.form
        if (state.canEdit && form == null && state.status.phase == Phase.READY) {
            if (state.bridges.isEmpty()) {
                Button(onClick = { model.startConnect(SettingsBridgeDraft.KIND_JACKETT) }) {
                    Text(stringResource(R.string.acq_src_bridge_link))
                }
            } else {
                TextButton(onClick = { model.startConnect(SettingsBridgeDraft.KIND_JACKETT) }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = Spacing.Small))
                    Text(stringResource(R.string.acq_src_bridge_add_another))
                }
            }
        }
        if (form != null) BridgeForm(form, state.formToken, model)
    }
    unlinking?.let { bridge ->
        UnlinkSheet(
            bridge = bridge,
            removing = state.removing,
            error = state.removeError,
            onConfirm = { keep -> model.unlink(bridge.id, keep) { unlinking = null } },
            onClose = { unlinking = null },
        )
    }
    if (state.madeKeys.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = model::dismissKeys,
            title = { Text(stringResource(R.string.acq_src_bridge_made_keys)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(stringResource(R.string.acq_src_bridge_made_keys_text))
                    state.madeKeys.forEach { (side, key) ->
                        val label = if (side == "sonarr") R.string.acq_src_bridge_tv_key else R.string.acq_src_bridge_movie_key
                        Column {
                            Hint(stringResource(label))
                            SelectionContainer { Text(key, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = model::dismissKeys) { Text(stringResource(R.string.acq_src_close)) } },
        )
    }
}

@Composable
private fun BridgeRow(
    bridge: SettingsBridge,
    canEdit: Boolean,
    syncing: Boolean,
    onSync: () -> Unit,
    onChange: () -> Unit,
    onUnlink: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            Text(bridgeName(bridge.kind), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (bridge.lastError.isNotEmpty()) {
                FailText(stringResource(R.string.acq_src_bridge_not_connected))
            } else {
                Text(
                    pluralStringResource(R.plurals.acq_src_bridge_sources, bridge.sources, bridge.sources),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(bridge.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        relativeTime(bridge.lastCheck)?.let { Hint(stringResource(R.string.acq_src_bridge_checked, it)) }
        if (bridge.lastError.isNotEmpty()) FailText(bridge.lastError)
        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                TextButton(onClick = onSync, enabled = !syncing) { Text(stringResource(R.string.acq_src_bridge_sync)) }
                TextButton(onClick = onChange) { Text(stringResource(R.string.acq_src_bridge_change)) }
                TextButton(onClick = onUnlink) {
                    Text(stringResource(R.string.acq_src_bridge_unlink), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun BridgeForm(
    form: SettingsBridgeFormState,
    token: Int,
    model: BridgeViewModel,
) {
    var draft by remember(token) { mutableStateOf(form.initial) }
    var callbackOpen by remember(token) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text(stringResource(R.string.acq_src_bridge_app), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(SettingsBridgeDraft.KIND_PROWLARR, SettingsBridgeDraft.KIND_JACKETT).forEachIndexed { index, kind ->
                SegmentedButton(
                    selected = draft.kind == kind,
                    onClick = { draft = draft.copy(kind = kind) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) {
                    Text(bridgeName(kind))
                }
            }
        }
        if (form.kindError.isNotEmpty()) FailText(form.kindError)
        BridgeField(
            label = stringResource(R.string.acq_src_bridge_address),
            value = draft.url,
            onChange = { draft = draft.copy(url = it) },
            error = form.urlError,
            hint = if (draft.isProwlarr) null else stringResource(R.string.acq_src_bridge_base_path),
            placeholder = "http://192.168.1.10:" + draft.defaultPort,
            keyboard = KeyboardType.Uri,
        )
        BridgeField(
            label = stringResource(R.string.acq_src_bridge_key),
            value = draft.key,
            onChange = { draft = draft.copy(key = it) },
            error = form.keyError,
            hint =
                stringResource(
                    when {
                        form.initial.editingId != 0L -> R.string.acq_src_bridge_key_again
                        draft.isProwlarr -> R.string.acq_src_bridge_key_prowlarr
                        else -> R.string.acq_src_bridge_key_jackett
                    },
                ),
            password = true,
        )
        if (draft.isProwlarr) {
            if (draft.callbackUrl.isNotBlank() && !callbackOpen && form.callbackError.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Hint(stringResource(R.string.acq_src_bridge_reaches))
                        Text(draft.callbackUrl, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { callbackOpen = true }) { Text(stringResource(R.string.acq_src_bridge_change)) }
                }
            } else {
                BridgeField(
                    label = stringResource(R.string.acq_src_bridge_callback),
                    value = draft.callbackUrl,
                    onChange = { draft = draft.copy(callbackUrl = it) },
                    error = form.callbackError,
                    hint = stringResource(R.string.acq_src_bridge_callback_hint),
                    keyboard = KeyboardType.Uri,
                )
            }
            if (form.keysNeeded.isNotEmpty()) {
                CheckLine(stringResource(R.string.acq_src_bridge_replace_keys), draft.newKeys) { draft = draft.copy(newKeys = it) }
                if (!draft.newKeys) {
                    Hint(
                        stringResource(
                            if (form.keysNeeded.size == 1) R.string.acq_src_bridge_current_key else R.string.acq_src_bridge_current_keys,
                        ),
                    )
                    if (form.needsSonarrKey) {
                        BridgeField(
                            label = stringResource(R.string.acq_src_bridge_tv_key),
                            value = draft.sonarrKey,
                            onChange = { draft = draft.copy(sonarrKey = it) },
                            password = true,
                        )
                    }
                    if (form.needsRadarrKey) {
                        BridgeField(
                            label = stringResource(R.string.acq_src_bridge_movie_key),
                            value = draft.radarrKey,
                            onChange = { draft = draft.copy(radarrKey = it) },
                            password = true,
                        )
                    }
                }
            }
        }
        form.error?.let { FailText(sourceErrorText(it)) }
        BridgeResult(form, draft.kind)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { model.test(draft) }, enabled = draft.canSubmit && !form.busy) {
                Text(stringResource(if (form.testing) R.string.acq_src_testing else R.string.acq_src_bridge_test))
            }
            Button(onClick = { model.link(draft) }, enabled = draft.canSubmit && !form.busy) {
                Text(stringResource(if (draft.editingId != 0L) R.string.acq_src_save else R.string.acq_src_bridge_link))
            }
            TextButton(onClick = model::cancelForm) { Text(stringResource(R.string.acq_src_cancel)) }
        }
    }
}

@Composable
private fun BridgeResult(
    form: SettingsBridgeFormState,
    kind: String,
) {
    val result = form.result ?: return
    val name = bridgeName(kind)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        val head =
            if (result.version.isNotEmpty()) {
                name + " " + result.version
            } else {
                stringResource(R.string.acq_src_bridge_answered, name)
            }
        Text(head, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        if (result.siteCount > 0) {
            Hint(pluralStringResource(R.plurals.acq_src_bridge_indexers, result.siteCount, result.siteCount))
            Hint(result.sites.joinToString(", ") + if (result.siteCount > result.sites.size) "…" else "")
        }
    }
}

@Composable
private fun BridgeField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String = "",
    hint: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = (error.ifEmpty { hint })?.let { { Text(it) } },
        isError = error.isNotEmpty(),
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlinkSheet(
    bridge: SettingsBridge,
    removing: Boolean,
    error: ch.orellbuehler.acolyte.api.ApiError?,
    onConfirm: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var keep by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Large).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.acq_src_bridge_unlink_title, bridgeName(bridge.kind)), style = MaterialTheme.typography.titleLarge)
            Hint(bridge.url)
            if (bridge.kind == SettingsBridgeDraft.KIND_JACKETT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !keep, onClick = { keep = false })
                    Text(pluralStringResource(R.plurals.acq_src_bridge_remove_its, bridge.sources, bridge.sources))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = keep, onClick = { keep = true })
                    Text(stringResource(R.string.acq_src_bridge_keep_stop))
                }
            } else {
                Text(pluralStringResource(R.plurals.acq_src_bridge_removes_prowlarr, bridge.sources, bridge.sources))
                CheckLine(stringResource(R.string.acq_src_bridge_unlink_here), keep) { keep = it }
            }
            error?.let { FailText(sourceErrorText(it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onClose) { Text(stringResource(R.string.acq_src_cancel)) }
                Button(onClick = { onConfirm(keep) }, enabled = !removing) { Text(stringResource(R.string.acq_src_bridge_unlink)) }
            }
        }
    }
}
