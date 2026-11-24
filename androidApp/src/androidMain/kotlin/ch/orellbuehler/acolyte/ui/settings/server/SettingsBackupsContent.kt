package ch.orellbuehler.acolyte.ui.settings.server

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupPreset
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupRow
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupsNotice
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupsNoticeKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerBackupsState
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerFile
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerFileKind
import ch.orellbuehler.acolyte.feature.settings.server.SettingsServerRestoreStage
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.settings.SettingsReauthDialog
import ch.orellbuehler.acolyte.ui.settings.shareTransferredFile
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsBackupsContent(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel { SettingsBackupsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedPath by rememberSaveable { mutableStateOf("") }
    var pickedName by rememberSaveable { mutableStateOf("") }
    val pickFailed = stringResource(R.string.settings_server_backups_pick_failed)
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    val name = pickedFileName(context, uri)
                    val path = viewModel.stagingPath(name)
                    val copied = withContext(Dispatchers.IO) { copyPicked(context, uri, path) }
                    if (copied) {
                        pickedPath = path
                        pickedName = name
                    } else {
                        snackbar.showSnackbar(pickFailed)
                    }
                }
            }
        }
    val notice = state.notice
    val noticeText = notice?.let { backupsNoticeText(it) }
    LaunchedEffect(notice?.seq) {
        if (notice != null && noticeText != null) {
            snackbar.showSnackbar(noticeText)
            viewModel.dismissNotice(notice.seq)
        }
    }
    ScreenScaffold(title = stringResource(R.string.settings_page_backups), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(
                    status = state.status,
                    isEmpty = state.off,
                    onRefresh = viewModel::refresh,
                    empty = {
                        EmptyView(
                            title = stringResource(R.string.settings_server_backups_off),
                            message = stringResource(R.string.settings_server_backups_off_text),
                            icon = Icons.Rounded.Archive,
                        )
                    },
                ) {
                    BackupsList(
                        state,
                        viewModel,
                        onDelete = { deleting = it },
                        onPick = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                    )
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    deleting?.let { name ->
        ConfirmDialog(
            title = stringResource(R.string.settings_server_backups_remove_title, name),
            text = stringResource(R.string.settings_server_backups_remove_text),
            confirm = stringResource(R.string.settings_server_backups_remove),
            dismiss = stringResource(R.string.common_cancel),
            danger = true,
            onConfirm = {
                deleting = null
                viewModel.delete(name)
            },
            onDismiss = { deleting = null },
        )
    }
    if (pickedPath.isNotEmpty()) {
        ConfirmDialog(
            title = stringResource(R.string.settings_server_backups_restore_title, pickedName),
            text = stringResource(R.string.settings_server_backups_restore_text),
            confirm = stringResource(R.string.settings_server_backups_restore),
            dismiss = stringResource(R.string.common_cancel),
            danger = true,
            onConfirm = {
                val path = pickedPath
                val name = pickedName
                pickedPath = ""
                pickedName = ""
                viewModel.restore(path, name)
            },
            onDismiss = {
                File(pickedPath).delete()
                pickedPath = ""
                pickedName = ""
            },
        )
    }
    state.file?.let { file ->
        ShareWarning(
            file,
            onShare = {
                shareTransferredFile(context, file.toTransferred(), fileMime(file))
                viewModel.fileDelivered()
            },
            onCancel = viewModel::release,
        )
    }
    SettingsReauthDialog(state.reauth, onConfirm = viewModel::confirmReauth, onCancel = viewModel::cancelReauth)
}

private fun fileMime(file: SettingsServerFile): String =
    when (file.kind) {
        SettingsServerFileKind.BACKUP -> "application/zip"
        SettingsServerFileKind.KEY_KIT -> "application/octet-stream"
        SettingsServerFileKind.LOG -> "text/plain"
    }

@Composable
private fun ShareWarning(
    file: SettingsServerFile,
    onShare: () -> Unit,
    onCancel: () -> Unit,
) {
    val kit = file.kind == SettingsServerFileKind.KEY_KIT
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                stringResource(
                    if (kit) R.string.settings_server_backups_share_kit_title else R.string.settings_server_backups_share_backup_title,
                ),
            )
        },
        text = {
            Text(
                stringResource(
                    if (kit) R.string.settings_server_backups_share_kit_text else R.string.settings_server_backups_share_backup_text,
                ),
            )
        },
        confirmButton = { TextButton(onClick = onShare) { Text(stringResource(R.string.settings_server_backups_share)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) } },
    )
}

private fun pickedFileName(
    context: Context,
    uri: Uri,
): String {
    val shown =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    return shown?.takeIf { it.isNotBlank() } ?: "backup.zip"
}

private fun copyPicked(
    context: Context,
    uri: Uri,
    path: String,
): Boolean =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            File(path).outputStream().use { output -> input.copyTo(output) }
        } != null
    }.getOrDefault(false)

@Composable
private fun backupsNoticeText(notice: SettingsServerBackupsNotice): String =
    when (notice.kind) {
        SettingsServerBackupsNoticeKind.MADE -> stringResource(R.string.settings_server_backups_made)
        SettingsServerBackupsNoticeKind.MAKE_FAILED ->
            noticeWithError(
                stringResource(R.string.settings_server_backups_make_failed),
                notice.error,
            )
        SettingsServerBackupsNoticeKind.DELETED -> stringResource(R.string.settings_server_backups_deleted)
        SettingsServerBackupsNoticeKind.DELETE_FAILED ->
            noticeWithError(
                stringResource(R.string.settings_server_backups_delete_failed),
                notice.error,
            )
        SettingsServerBackupsNoticeKind.PLAN_SAVED ->
            stringResource(
                R.string.settings_server_backups_plan_saved,
                presetName(
                    SettingsServerBackupPreset.entries.firstOrNull {
                        it.wire ==
                            notice.name
                    },
                ),
            )
        SettingsServerBackupsNoticeKind.PLAN_FAILED ->
            noticeWithError(
                stringResource(R.string.settings_server_backups_plan_failed),
                notice.error,
            )
        SettingsServerBackupsNoticeKind.DOWNLOAD_FAILED ->
            noticeWithError(stringResource(R.string.settings_server_backups_download_failed), notice.error)
        SettingsServerBackupsNoticeKind.KIT_READY -> stringResource(R.string.settings_server_backups_kit_ready)
        SettingsServerBackupsNoticeKind.KIT_FAILED ->
            noticeWithError(
                stringResource(R.string.settings_server_backups_kit_failed),
                notice.error,
            )
        SettingsServerBackupsNoticeKind.RESTORE_STAGED -> stringResource(R.string.settings_server_backups_restore_staged)
        SettingsServerBackupsNoticeKind.RESTORE_FAILED ->
            noticeWithError(stringResource(R.string.settings_server_backups_restore_failed), notice.error)
        SettingsServerBackupsNoticeKind.RESTARTED -> stringResource(R.string.settings_server_backups_restarted)
        SettingsServerBackupsNoticeKind.RESTART_SLOW -> stringResource(R.string.settings_server_backups_restart_slow)
    }

@Composable
private fun presetName(preset: SettingsServerBackupPreset?): String =
    stringResource(
        when (preset) {
            SettingsServerBackupPreset.LIGHT -> R.string.settings_server_backups_light
            SettingsServerBackupPreset.CAREFUL -> R.string.settings_server_backups_careful
            SettingsServerBackupPreset.OFF -> R.string.settings_server_backups_off_plan
            else -> R.string.settings_server_backups_balanced
        },
    )

@Composable
private fun presetAbout(preset: SettingsServerBackupPreset): String =
    stringResource(
        when (preset) {
            SettingsServerBackupPreset.BALANCED -> R.string.settings_server_backups_balanced_about
            SettingsServerBackupPreset.LIGHT -> R.string.settings_server_backups_light_about
            SettingsServerBackupPreset.CAREFUL -> R.string.settings_server_backups_careful_about
            SettingsServerBackupPreset.OFF -> R.string.settings_server_backups_off_about
        },
    )

@Composable
private fun kindWords(kind: String): String =
    when (kind) {
        "scheduled" -> stringResource(R.string.settings_server_backups_kind_scheduled)
        "manual" -> stringResource(R.string.settings_server_backups_kind_manual)
        "update" -> stringResource(R.string.settings_server_backups_kind_update)
        "pre-update" -> stringResource(R.string.settings_server_backups_kind_pre_update)
        else -> kind
    }

@Composable
private fun BackupsList(
    state: SettingsServerBackupsState,
    viewModel: SettingsBackupsViewModel,
    onDelete: (String) -> Unit,
    onPick: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
        if (state.restoreWaiting) {
            item(key = "waiting") { InfoBanner(stringResource(R.string.settings_server_backups_restore_ready)) }
        }
        if (state.restoring) {
            item(key = "restoring") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    InfoBanner(
                        if (state.restore == SettingsServerRestoreStage.UPLOADING) {
                            stringResource(R.string.settings_server_backups_uploading, state.restoreName)
                        } else {
                            stringResource(R.string.settings_server_backups_restarting)
                        },
                    )
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen))
                }
            }
        }
        if (state.canChange) {
            item(key = "make") {
                Button(
                    onClick = viewModel::makeNow,
                    enabled = !state.making && !state.restoring,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
                ) {
                    if (state.making) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(stringResource(R.string.settings_server_backups_back_up_now), modifier = Modifier.padding(start = Spacing.Small))
                }
            }
        }
        item(key = "list") { SavedCard(state, viewModel, onDelete, onPick) }
        item(key = "often") { OftenCard(state, viewModel) }
        if (state.keyKit
                ?.fingerprint
                .orEmpty()
                .isNotEmpty()
        ) {
            item(key = "key") { KeyCard(state, viewModel) }
        }
    }
}

@Composable
private fun SavedCard(
    state: SettingsServerBackupsState,
    viewModel: SettingsBackupsViewModel,
    onDelete: (String) -> Unit,
    onPick: () -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_server_backups_saved)) {
        if (state.isOwner) {
            OutlinedButton(onClick = onPick, enabled = !state.restoring) {
                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.settings_server_backups_restore_file), modifier = Modifier.padding(start = Spacing.Small))
            }
        }
        if (state.backups.isEmpty()) {
            Hint(stringResource(R.string.settings_server_backups_none))
        } else {
            state.backups.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                BackupRow(row, state.isOwner, onDownload = { viewModel.download(row.name) }, onDelete = { onDelete(row.name) })
            }
        }
        if (state.showKeyNote) Hint(state.keyNote)
    }
}

@Composable
private fun BackupRow(
    row: SettingsServerBackupRow,
    owner: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                row.name,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Hint(
                stringResource(
                    R.string.settings_server_backups_row_line,
                    dateTimeText(context, row.createdAt),
                    sizeText(context, row.size),
                    row.detail.ifEmpty { kindWords(row.kind) },
                ),
            )
        }
        if (owner) {
            if (row.busy) {
                CircularProgressIndicator(Modifier.padding(Spacing.Small).size(20.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onDownload) { Text(stringResource(R.string.settings_server_backups_download)) }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.settings_server_backups_delete_name, row.name))
                }
            }
        }
    }
}

@Composable
private fun OftenCard(
    state: SettingsServerBackupsState,
    viewModel: SettingsBackupsViewModel,
) {
    SettingsCard(
        stringResource(R.string.settings_server_backups_often),
        subtitle = stringResource(R.string.settings_server_backups_often_desc),
    ) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            SettingsServerBackupPreset.entries.forEach { preset ->
                val selected = state.preset == preset
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            enabled = state.canChange && !state.presetSaving,
                            role = Role.RadioButton,
                            onClick = { viewModel.setPreset(preset) },
                        ),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    RadioButton(selected = selected, onClick = null, enabled = state.canChange && !state.presetSaving)
                    Column(Modifier.weight(1f).padding(top = Spacing.XSmall)) {
                        Text(presetName(preset), style = MaterialTheme.typography.bodyLarge)
                        Hint(presetAbout(preset))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyCard(
    state: SettingsServerBackupsState,
    viewModel: SettingsBackupsViewModel,
) {
    val kit = state.keyKit ?: return
    SettingsCard(
        stringResource(R.string.settings_server_backups_key),
        subtitle = stringResource(R.string.settings_server_backups_key_desc),
    ) {
        if (kit.needed) {
            val base =
                stringResource(if (kit.warn) R.string.settings_server_backups_no_kit else R.string.settings_server_backups_kit_recommended)
            val text = if (state.isOwner) base else base + " " + stringResource(R.string.settings_server_backups_owner_only_kit)
            InfoBanner(text, padded = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Hint(stringResource(R.string.settings_server_backups_fingerprint))
            Text(kit.fingerprint, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
        }
        if (state.canDownloadKit) {
            OutlinedButton(onClick = viewModel::downloadKeyKit, enabled = !state.keyKitBusy) {
                if (state.keyKitBusy) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(stringResource(R.string.settings_server_backups_download_kit), modifier = Modifier.padding(start = Spacing.Small))
            }
            Hint(stringResource(R.string.settings_server_backups_restore_key))
        } else if (state.isOwner && state.keyKitNeedsSession) {
            Hint(stringResource(R.string.settings_server_backups_kit_needs_session))
        }
    }
}
