package ch.orellbuehler.acolyte.ui.settings.keys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.ApiKeyInfo
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsApiState
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsSeerrCard
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsSeerrFresh
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsSeerrSide
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.settings.SettingsReauthDialog
import ch.orellbuehler.acolyte.ui.settings.shareText
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun SettingsApiPage(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsApiViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val copy = rememberKeysCopy(snackbar)
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    ScreenScaffold(title = stringResource(R.string.settings_page_api), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = Spacing.XLarge),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        state.status.error?.let { error -> item(key = "error") { ErrorBanner(error) } }
                        item(key = "keys") { ApiKeysCard(state, viewModel, copy) { shareText(context, it) } }
                        item(key = "start") { StartCard(state, copy) }
                        if (!state.seerrGone) item(key = "seerr") { SeerrCard(state, viewModel, copy) { shareText(context, it) } }
                        item(key = "reference") { ReferenceCard { runCatching { uri.openUri(state.openApiAddress) } } }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(Spacing.Small))
        }
    }
    KeysNoticeHost(state.notice, snackbar, viewModel::dismissNotice)
    SettingsReauthDialog(state.reauth, viewModel::confirmReauth, viewModel::cancelReauth)
}

@Composable
private fun ApiKeysCard(
    state: SettingsApiState,
    viewModel: SettingsApiViewModel,
    copy: (String, String) -> Unit,
    onShare: (String) -> Unit,
) {
    var label by rememberSaveable { mutableStateOf("") }
    var revoking by rememberSaveable { mutableLongStateOf(0L) }
    val copied = stringResource(R.string.settings_keys_copied_key)
    LaunchedEffect(state.fresh?.id) { if (state.fresh != null) label = "" }
    SettingsCard(stringResource(R.string.settings_keys_api_keys_title), subtitle = stringResource(R.string.settings_keys_api_keys_hint)) {
        val error = state.keysError
        if (error != null) {
            FailText(if (state.keysNeedPassword) stringResource(R.string.settings_keys_api_sign_in) else errorText(error))
        } else {
            state.fresh?.let { fresh ->
                FreshKeyBox(
                    title = fresh.name,
                    text = stringResource(R.string.settings_keys_api_fresh_text),
                    secret = fresh.key,
                    onCopy = { copy(fresh.key, copied) },
                    onShare = { onShare(fresh.key) },
                    onClose = viewModel::dismissFresh,
                )
            }
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(state.labelMax) },
                label = { Text(stringResource(R.string.settings_keys_key_name)) },
                placeholder = { Text(stringResource(R.string.settings_keys_api_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { viewModel.makeKey(label) }, enabled = label.isNotBlank() && !state.making) {
                Text(stringResource(R.string.settings_keys_api_make))
            }
            if (state.keys.isEmpty()) {
                Hint(stringResource(R.string.settings_keys_api_none))
            } else {
                Column {
                    state.keys.forEachIndexed { index, key ->
                        if (index > 0) HorizontalDivider()
                        ApiKeyRow(key, state.revokingId == 0L) { revoking = key.id }
                    }
                }
            }
        }
    }
    state.keys.firstOrNull { it.id == revoking }?.let { key ->
        ConfirmDialog(
            title = stringResource(R.string.settings_keys_api_revoke_title, key.label),
            text = stringResource(R.string.settings_keys_api_revoke_text),
            confirm = stringResource(R.string.settings_keys_revoke),
            dismiss = stringResource(R.string.settings_keys_keep),
            onConfirm = {
                revoking = 0L
                viewModel.revokeKey(key.id)
            },
            onDismiss = { revoking = 0L },
            danger = true,
        )
    }
}

@Composable
private fun ApiKeyRow(
    key: ApiKeyInfo,
    enabled: Boolean,
    onRevoke: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Column(Modifier.weight(1f)) {
            Text(key.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                key.hint,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Hint(keyUsageText(key.createdAt, key.lastUsedAt))
        }
        TextButton(onClick = onRevoke, enabled = enabled) {
            Text(stringResource(R.string.settings_keys_revoke), color = MaterialTheme.acolyte.failText)
        }
    }
}

@Composable
private fun StartCard(
    state: SettingsApiState,
    copy: (String, String) -> Unit,
) {
    val copied = stringResource(R.string.settings_keys_copied_address)
    SettingsCard(stringResource(R.string.settings_keys_api_start_title)) {
        Step(1, stringResource(R.string.settings_keys_api_step1))
        Step(2, stringResource(R.string.settings_keys_api_step2))
        Step(3, stringResource(R.string.settings_keys_api_step3))
        CopyBlock(
            value = state.baseAddress,
            copyDescription = stringResource(R.string.settings_keys_api_copy_base),
            onCopy = { copy(state.baseAddress, copied) },
        )
    }
}

@Composable
private fun Step(
    number: Int,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text("$number.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReferenceCard(onOpen: () -> Unit) {
    SettingsCard(stringResource(R.string.settings_keys_api_reference_title)) {
        Hint(stringResource(R.string.settings_keys_api_openapi_hint))
        OutlinedButton(onClick = onOpen) { Text(stringResource(R.string.settings_keys_api_openapi)) }
    }
}

@Composable
private fun SeerrCard(
    state: SettingsApiState,
    viewModel: SettingsApiViewModel,
    copy: (String, String) -> Unit,
    onShare: (String) -> Unit,
) {
    var replacing by rememberSaveable { mutableStateOf("") }
    var removing by rememberSaveable { mutableStateOf("") }
    SettingsCard(stringResource(R.string.settings_keys_seerr_title), subtitle = stringResource(R.string.settings_keys_seerr_description)) {
        val card = state.seerr
        state.seerrError?.let { error ->
            FailText(errorText(error))
            if (card == null) OutlinedButton(onClick = viewModel::reloadSeerr) { Text(stringResource(R.string.common_retry)) }
        }
        if (card != null) {
            SwitchRow(
                stringResource(R.string.settings_keys_seerr_answer),
                card.enabled,
                viewModel::setSeerrEnabled,
                enabled = !state.seerrSaving,
            )
            if (card.enabled) SeerrBody(card, state, viewModel, copy, onShare, { replacing = it }, { removing = it })
        }
    }
    state.seerr?.sides?.firstOrNull { it.side == replacing }?.let { side ->
        ConfirmDialog(
            title = stringResource(R.string.settings_keys_seerr_replace_title, appName(side.side)),
            text = stringResource(R.string.settings_keys_seerr_replace_text),
            confirm = stringResource(R.string.settings_keys_replace),
            dismiss = stringResource(R.string.common_cancel),
            onConfirm = {
                replacing = ""
                viewModel.makeSeerrKey(side.side)
            },
            onDismiss = { replacing = "" },
            danger = true,
        )
    }
    state.seerr?.sides?.firstOrNull { it.side == removing }?.let { side ->
        ConfirmDialog(
            title = stringResource(R.string.settings_keys_seerr_remove_title, appName(side.side)),
            text = stringResource(R.string.settings_keys_seerr_remove_text, sideWhatLower(side.side)),
            confirm = stringResource(R.string.settings_keys_remove),
            dismiss = stringResource(R.string.common_cancel),
            onConfirm = {
                removing = ""
                viewModel.removeSeerrKey(side.side)
            },
            onDismiss = { removing = "" },
            danger = true,
        )
    }
}

@Composable
private fun SeerrBody(
    card: SettingsSeerrCard,
    state: SettingsApiState,
    viewModel: SettingsApiViewModel,
    copy: (String, String) -> Unit,
    onShare: (String) -> Unit,
    onReplace: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    card.sides.forEach { side ->
        HorizontalDivider()
        SeerrSideBlock(
            side = side,
            fresh = state.seerrFresh?.takeIf { it.side == side.side },
            copy = copy,
            onShare = onShare,
            onMake = { if (side.hasKey) onReplace(side.side) else viewModel.makeSeerrKey(side.side) },
            onRemove = { onRemove(side.side) },
            onCloseFresh = viewModel::dismissSeerrFresh,
        )
    }
    if (card.sent.isNotEmpty()) {
        HorizontalDivider()
        Text(stringResource(R.string.settings_keys_seerr_sent_title), style = MaterialTheme.typography.titleSmall)
        Hint(stringResource(R.string.settings_keys_seerr_sent_hint))
        card.sent.forEach { row ->
            Column {
                Text(row.name, style = MaterialTheme.typography.bodyMedium)
                Hint(sentLine(row.kind, row.side, row.updatedAt))
            }
        }
    }
    HorizontalDivider()
    SwitchRow(
        stringResource(R.string.settings_keys_seerr_delete_files),
        card.deleteFiles,
        viewModel::setSeerrDeleteFiles,
        hint = stringResource(R.string.settings_keys_seerr_delete_files_hint),
        enabled = !state.seerrSaving,
    )
}

@Composable
private fun SeerrSideBlock(
    side: SettingsSeerrSide,
    fresh: SettingsSeerrFresh?,
    copy: (String, String) -> Unit,
    onShare: (String) -> Unit,
    onMake: () -> Unit,
    onRemove: () -> Unit,
    onCloseFresh: () -> Unit,
) {
    val app = appName(side.side)
    val what = stringResource(if (side.side == "sonarr") R.string.settings_keys_seerr_shows else R.string.settings_keys_seerr_movies)
    val copiedUrl = stringResource(R.string.settings_keys_copied_url_base)
    val copiedKey = stringResource(R.string.settings_keys_copied_key)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.settings_keys_seerr_add_as, what, app), style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hint(stringResource(R.string.settings_keys_seerr_url_base, side.path), Modifier.weight(1f))
            IconButton(onClick = { copy(side.path, copiedUrl) }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.settings_keys_seerr_copy_url, app))
            }
        }
        Hint(keyLine(side))
        if (side.hasKey && !side.ownerActive) FailText(stringResource(R.string.settings_keys_seerr_owner_off, side.owner))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (side.hasKey) {
                TextButton(onClick = onRemove, enabled = !side.busy) {
                    Text(stringResource(R.string.settings_keys_seerr_remove), color = MaterialTheme.acolyte.failText)
                }
            }
            Button(onClick = onMake, enabled = !side.busy) {
                Text(stringResource(if (side.hasKey) R.string.settings_keys_seerr_new else R.string.settings_keys_seerr_make))
            }
        }
        if (fresh != null) {
            FreshKeyBox(
                title = stringResource(R.string.settings_keys_seerr_fresh_title, app),
                text = stringResource(R.string.settings_keys_seerr_fresh_text),
                secret = fresh.key,
                onCopy = { copy(fresh.key, copiedKey) },
                onShare = { onShare(fresh.key) },
                onClose = onCloseFresh,
            )
        }
    }
}

private fun appName(side: String): String = if (side == "sonarr") "Sonarr" else "Radarr"

@Composable
private fun sideWhatLower(side: String): String =
    stringResource(if (side == "sonarr") R.string.settings_keys_seerr_shows_lower else R.string.settings_keys_seerr_movies_lower)

@Composable
private fun keyLine(side: SettingsSeerrSide): String {
    if (!side.hasKey) return stringResource(R.string.settings_keys_seerr_no_key)
    val used = side.lastUsedAt?.let { relativeTime(it) }
    return if (used != null) {
        stringResource(R.string.settings_keys_seerr_key_used, side.hint, side.owner, used)
    } else {
        stringResource(R.string.settings_keys_seerr_key_new, side.hint, side.owner)
    }
}

@Composable
private fun sentLine(
    kind: String,
    side: String,
    updatedAt: String,
): String {
    val kindText = stringResource(if (kind == "format") R.string.settings_keys_seerr_format else R.string.settings_keys_seerr_profile)
    val updated = relativeTime(updatedAt)?.let { stringResource(R.string.settings_keys_seerr_updated, it) }
    return listOfNotNull(kindText, appName(side), updated).joinToString(" · ")
}
