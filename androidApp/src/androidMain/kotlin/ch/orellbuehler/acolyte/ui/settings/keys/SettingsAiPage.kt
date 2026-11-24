package ch.orellbuehler.acolyte.ui.settings.keys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsAiState
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsKeysAiKey
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.settings.SettingsReauthDialog
import ch.orellbuehler.acolyte.ui.settings.shareText
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun SettingsAiPage(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsAiViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val copy = rememberKeysCopy(snackbar)
    val context = LocalContext.current
    ScreenScaffold(title = stringResource(R.string.settings_page_ai), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(status = state.status, isEmpty = !state.available, onRefresh = viewModel::refresh, empty = { Unavailable() }) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = Spacing.XLarge),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    state.status.error?.let { error -> item(key = "error") { ErrorBanner(error) } }
                    item(key = "switch") { SwitchCard(state, viewModel) }
                    item(key = "connect") { ConnectCard(state, copy) }
                    if (state.canManageKeys) item(key = "keys") { LinkedCard(state, viewModel, copy) { shareText(context, it) } }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(Spacing.Small))
        }
    }
    KeysNoticeHost(state.notice, snackbar, viewModel::dismissNotice)
    SettingsReauthDialog(state.reauth, viewModel::confirmReauth, viewModel::cancelReauth)
}

@Composable
private fun Unavailable() {
    MessageView(
        icon = Icons.Rounded.AutoAwesome,
        title = stringResource(R.string.settings_keys_ai_unavailable_title),
        message = stringResource(R.string.settings_keys_ai_unavailable_text),
    )
}

@Composable
private fun SwitchCard(
    state: SettingsAiState,
    viewModel: SettingsAiViewModel,
) {
    SettingsCard(stringResource(R.string.settings_keys_ai_card_title), subtitle = stringResource(R.string.settings_keys_ai_intro)) {
        SwitchRow(
            stringResource(R.string.settings_keys_ai_switch),
            state.enabled,
            viewModel::setEnabled,
            hint = if (state.isOwner) null else stringResource(R.string.settings_keys_ai_owner_only),
            enabled = state.isOwner && !state.saving,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Hint("• " + stringResource(R.string.settings_keys_ai_fact1))
            Hint("• " + stringResource(R.string.settings_keys_ai_fact2))
            Hint("• " + stringResource(R.string.settings_keys_ai_fact3))
        }
    }
}

@Composable
private fun ConnectCard(
    state: SettingsAiState,
    copy: (String, String) -> Unit,
) {
    val copied = stringResource(R.string.settings_keys_copied_address)
    SettingsCard(stringResource(R.string.settings_keys_ai_connect_title)) {
        Text(stringResource(R.string.settings_keys_ai_address), style = MaterialTheme.typography.bodyMedium)
        CopyBlock(
            value = state.address,
            copyDescription = stringResource(R.string.settings_keys_ai_copy_address),
            onCopy = { copy(state.address, copied) },
        )
        Hint(stringResource(R.string.settings_keys_ai_error_expected))
        if (!state.enabled) {
            Text(
                stringResource(R.string.settings_keys_ai_turn_on_first),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.acolyte.waitText,
            )
        }
    }
}

@Composable
private fun LinkedCard(
    state: SettingsAiState,
    viewModel: SettingsAiViewModel,
    copy: (String, String) -> Unit,
    onShare: (String) -> Unit,
) {
    var revoking by rememberSaveable { mutableLongStateOf(0L) }
    var editing by rememberSaveable { mutableLongStateOf(0L) }
    SettingsCard(stringResource(R.string.settings_keys_ai_linked_title), subtitle = stringResource(R.string.settings_keys_ai_linked_note)) {
        val error = state.keysError
        if (error != null) {
            FailText(if (state.keysNeedPassword) stringResource(R.string.settings_keys_ai_sign_in) else errorText(error))
        } else {
            val copied = stringResource(R.string.settings_keys_copied_key)
            state.fresh?.let { fresh ->
                FreshKeyBox(
                    title = stringResource(R.string.settings_keys_ai_fresh_title),
                    text = stringResource(R.string.settings_keys_ai_fresh_text, fresh.name),
                    secret = fresh.key,
                    onCopy = { copy(fresh.key, copied) },
                    onShare = { onShare(fresh.key) },
                    onClose = viewModel::dismissFresh,
                )
            }
            if (state.keys.isEmpty()) {
                Hint(stringResource(R.string.settings_keys_ai_none))
            } else {
                Column {
                    state.keys.forEachIndexed { index, key ->
                        if (index > 0) HorizontalDivider()
                        AiKeyRow(key, state.canChange, state.revokingId == 0L, { editing = key.id }, { revoking = key.id })
                    }
                }
            }
            HorizontalDivider()
            ByHand(state, viewModel)
        }
    }
    state.keys.firstOrNull { it.id == revoking }?.let { key ->
        ConfirmDialog(
            title = stringResource(R.string.settings_keys_ai_revoke_title, key.label),
            text = stringResource(R.string.settings_keys_ai_revoke_text),
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
    state.keys.firstOrNull { it.id == editing }?.let { key ->
        ChangeSheet(key, state, viewModel) {
            editing = 0L
            viewModel.clearChangeError()
        }
    }
}

@Composable
private fun AiKeyRow(
    key: SettingsKeysAiKey,
    canChange: Boolean,
    enabled: Boolean,
    onChange: () -> Unit,
    onRevoke: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.Small), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Text(key.label, style = MaterialTheme.typography.bodyLarge)
        Text(
            key.hint,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Hint(stringResource(R.string.settings_keys_ai_can, canLine(key.groups)))
        Hint(keyUsageText(key.createdAt, key.lastUsedAt))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (canChange) TextButton(onClick = onChange) { Text(stringResource(R.string.settings_keys_ai_change)) }
            TextButton(onClick = onRevoke, enabled = enabled) {
                Text(stringResource(R.string.settings_keys_revoke), color = MaterialTheme.acolyte.failText)
            }
        }
    }
}

@Composable
private fun canLine(groups: List<String>): String =
    if (groups.isEmpty()) {
        stringResource(R.string.settings_keys_ai_can_nothing)
    } else {
        groups.map { groupShort(it) }.joinToString(" · ")
    }

@Composable
private fun ByHand(
    state: SettingsAiState,
    viewModel: SettingsAiViewModel,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var label by rememberSaveable { mutableStateOf("") }
    var ticked by remember { mutableStateOf(state.defaultGroups.toSet()) }
    LaunchedEffect(state.fresh?.id) {
        if (state.fresh != null) {
            label = ""
            ticked = state.defaultGroups.toSet()
        }
    }
    TextButton(onClick = { open = !open }) { Text(stringResource(R.string.settings_keys_ai_by_hand)) }
    if (open) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Text(stringResource(R.string.settings_keys_ai_groups_legend), style = MaterialTheme.typography.titleSmall)
            GroupsChecklist(
                groups = state.offeredGroups,
                selected = ticked,
                enabled = !state.making,
                onToggle = { group -> ticked = if (group in ticked) ticked - group else ticked + group },
            )
            Hint(stringResource(R.string.settings_keys_ai_groups_fixed))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(state.labelMax) },
                label = { Text(stringResource(R.string.settings_keys_key_name)) },
                placeholder = { Text(stringResource(R.string.settings_keys_ai_key_example)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.makeKey(label, state.offeredGroups.filter { it in ticked }) },
                enabled =
                    label.isNotBlank() && !state.making,
            ) {
                Text(stringResource(R.string.settings_keys_ai_make))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeSheet(
    key: SettingsKeysAiKey,
    state: SettingsAiState,
    viewModel: SettingsAiViewModel,
    onDismiss: () -> Unit,
) {
    var ticked by remember(key.id) { mutableStateOf(key.groups.filter { it in state.offeredGroups }.toSet()) }
    val opened = remember(key.id) { state.changedCount }
    LaunchedEffect(state.changedCount) { if (state.changedCount > opened) onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.settings_keys_ai_change_title, key.label), style = MaterialTheme.typography.titleLarge)
            Hint(stringResource(R.string.settings_keys_ai_change_text))
            if (state.changeNeedsSession) FailText(stringResource(R.string.settings_keys_ai_change_needs_session))
            GroupsChecklist(
                groups = state.offeredGroups,
                selected = ticked,
                enabled = !state.changing,
                onToggle = { group -> ticked = if (group in ticked) ticked - group else ticked + group },
            )
            state.changeError?.let { error ->
                FailText(
                    listOf(stringResource(R.string.settings_keys_ai_change_failed), errorText(error), state.changeDetail)
                        .filter {
                            it.isNotBlank()
                        }.joinToString("\n"),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_keys_keep)) }
                Button(
                    onClick = { viewModel.changeGroups(key.id, state.offeredGroups.filter { it in ticked }) },
                    enabled = !state.changing && !state.changeNeedsSession,
                ) { Text(stringResource(R.string.settings_keys_save)) }
            }
        }
    }
}
