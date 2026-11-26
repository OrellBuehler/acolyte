package ch.orellbuehler.acolyte.ui.downloads

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.TorrentUpload
import ch.orellbuehler.acolyte.feature.downloads.DownloadAction
import ch.orellbuehler.acolyte.feature.downloads.DownloadRemovePlan
import ch.orellbuehler.acolyte.feature.downloads.DownloadRules
import ch.orellbuehler.acolyte.feature.downloads.DownloadsState
import ch.orellbuehler.acolyte.feature.downloads.RemoveDoneLine
import ch.orellbuehler.acolyte.feature.downloads.RemoveFilesLine
import ch.orellbuehler.acolyte.feature.downloads.SpeedTarget
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import ch.orellbuehler.acolyte.common.R as CommonR

@Composable
internal fun RemoveDialog(
    plan: DownloadRemovePlan,
    busy: Boolean,
    viewModel: DownloadsViewModel,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(plan) }
    var deleteFiles by remember { mutableStateOf(plan.deleteFiles) }
    var turnDown by remember { mutableStateOf(plan.turnDown) }
    var searchAgain by remember { mutableStateOf(plan.searchAgain) }
    var failed by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val client = current.downloader.ifEmpty { stringResource(R.string.downloads_remove_the_downloader) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (current.keys.size == 1) {
                    stringResource(R.string.downloads_remove_title_one)
                } else {
                    stringResource(R.string.downloads_remove_title_many, current.keys.size)
                },
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (current.single.isNotEmpty()) {
                    Text(current.single, style = MaterialTheme.typography.titleSmall)
                    if (current.releaseName.isNotEmpty() && current.releaseName != current.single) {
                        Text(current.releaseName, style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                } else {
                    current.names.forEach {
                        Text(
                            "• $it",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (current.more > 0) Text(stringResource(R.string.downloads_remove_and_more, current.more), color = muted)
                }
                current.doneLine?.let { line ->
                    val res =
                        when (line) {
                            RemoveDoneLine.DONE_SEEDING -> R.string.downloads_remove_done_seeding
                            RemoveDoneLine.DONE_IDLE -> R.string.downloads_remove_done_idle
                            RemoveDoneLine.PART_SEEDING -> R.string.downloads_remove_part_seeding
                            RemoveDoneLine.PART_IDLE -> R.string.downloads_remove_part_idle
                        }
                    Text(stringResource(res, client), style = MaterialTheme.typography.bodyMedium)
                }
                if (current.showDeleteFiles) {
                    CheckLine(stringResource(R.string.downloads_remove_delete_files), deleteFiles) { deleteFiles = it }
                    val clients = current.filesClients.ifEmpty { client }
                    val hint =
                        when {
                            current.keepsPath.isNotEmpty() -> stringResource(R.string.downloads_remove_keeps_path, current.keepsPath)
                            !deleteFiles -> stringResource(R.string.downloads_remove_files_off, clients)
                            current.filesLine == RemoveFilesLine.PATH ->
                                stringResource(
                                    R.string.downloads_remove_files_path,
                                    current.filesPath,
                                    clients,
                                )
                            current.filesLine == RemoveFilesLine.ONE -> stringResource(R.string.downloads_remove_files_one, clients)
                            else -> stringResource(R.string.downloads_remove_files_many, clients)
                        }
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = muted)
                    if (current.keepsSome) {
                        Text(
                            stringResource(R.string.downloads_remove_keeps_some),
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                        )
                    }
                }
                if (current.showTurnDown) {
                    CheckLine(stringResource(R.string.downloads_remove_turn_down), turnDown) { turnDown = it }
                    Text(
                        stringResource(R.string.downloads_remove_turn_down_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                    CheckLine(stringResource(R.string.downloads_remove_search_again), searchAgain && turnDown, enabled = turnDown) {
                        searchAgain =
                            it
                    }
                    Text(
                        stringResource(R.string.downloads_remove_search_again_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
                if (current.blocked > 0) {
                    Text(
                        if (current.keys.isEmpty()) {
                            stringResource(R.string.downloads_remove_blocked_one)
                        } else {
                            stringResource(R.string.downloads_remove_blocked_many, current.blocked)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.acolyte.waitText,
                    )
                }
                if (failed) Text(stringResource(R.string.downloads_remove_failed), color = MaterialTheme.acolyte.failText)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && current.keys.isNotEmpty(),
                onClick = {
                    viewModel.remove(current, deleteFiles && current.showDeleteFiles, turnDown, searchAgain) { left ->
                        if (left.isEmpty()) {
                            onDismiss()
                        } else {
                            failed = true
                            current = current.copy(keys = left, names = emptyList(), single = "", more = 0)
                        }
                    }
                },
            ) {
                val label =
                    if (deleteFiles && current.showDeleteFiles && current.keepsPath.isEmpty()) {
                        stringResource(R.string.downloads_remove_confirm_delete)
                    } else {
                        stringResource(R.string.downloads_remove_confirm)
                    }
                Text(label, color = MaterialTheme.acolyte.failText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
internal fun CheckLine(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeedSheet(
    viewModel: DownloadsViewModel,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.loadClients() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val targets = remember(state.clientViews) { viewModel.presenter.speedTargets() }
    val values = remember(targets) { mutableStateListOf(*targets.map { mb(it.downMb) to mb(it.upMb) }.toTypedArray()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.downloads_speed_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.downloads_speed_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (targets.isEmpty()) Text(stringResource(R.string.downloads_speed_none))
            targets.forEachIndexed { i, t ->
                if (i >= values.size) return@forEachIndexed
                Text(t.name, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    NumberField(stringResource(R.string.downloads_speed_download), values[i].first, Modifier.weight(1f)) {
                        values[i] =
                            it to values[i].second
                    }
                    if (t.torrent) {
                        NumberField(stringResource(R.string.downloads_speed_upload), values[i].second, Modifier.weight(1f)) {
                            values[i] =
                                values[i].first to it
                        }
                    }
                }
                val slow = t.slowMode
                if (slow != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.downloads_speed_slow_mode), modifier = Modifier.weight(1f))
                        Switch(checked = slow, onCheckedChange = { viewModel.slowMode(t.id, it) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button(
                    enabled = targets.isNotEmpty() && !state.batchBusy,
                    onClick = {
                        val changed =
                            targets.mapIndexed { i, t ->
                                SpeedTarget(
                                    t.id,
                                    t.name,
                                    t.torrent,
                                    values.getOrNull(i)?.first?.toDoubleOrNull() ?: t.downMb,
                                    values.getOrNull(i)?.second?.toDoubleOrNull() ?: t.upMb,
                                    t.slowMode,
                                )
                            }
                        viewModel.saveSpeeds(changed, onDismiss)
                    },
                ) { Text(stringResource(R.string.downloads_save)) }
            }
        }
    }
}

private fun mb(value: Double): String = if (value <= 0) "0" else DownloadRules.ratioText(value, 1).removeSuffix(".0")

@Composable
internal fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTorrentsSheet(
    state: DownloadsState,
    viewModel: DownloadsViewModel,
    onDismiss: () -> Unit,
) {
    val targets = state.addTargets
    var target by remember { mutableStateOf(targets.firstOrNull()) }
    var links by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(targets.firstOrNull()?.category.orEmpty()) }
    var savePath by remember { mutableStateOf("") }
    var paused by remember { mutableStateOf(false) }
    var bad by remember { mutableStateOf<List<String>>(emptyList()) }
    var files by remember { mutableStateOf<List<TorrentUpload>>(emptyList()) }
    var dropped by remember { mutableStateOf(0) }
    val resolver = LocalContext.current.contentResolver
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            val picked = uris.mapNotNull { uri -> readUpload(resolver, uri) }
            files = picked.filter { it.name.endsWith(".torrent", ignoreCase = true) }
            dropped = picked.size - files.size + (uris.size - picked.size)
            bad = emptyList()
        }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.downloads_add_title), style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = { picker.launch("*/*") }) { Text(stringResource(R.string.downloads_add_files)) }
                if (files.isNotEmpty()) {
                    Text(
                        stringResource(R.string.downloads_add_files_picked, files.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (dropped > 0) {
                Text(
                    stringResource(R.string.downloads_add_files_dropped, dropped),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.acolyte.waitText,
                )
            }
            OutlinedTextField(
                value = links,
                onValueChange = {
                    links = it
                    bad = emptyList()
                },
                label = { Text(stringResource(R.string.downloads_add_links)) },
                placeholder = { Text(stringResource(R.string.downloads_add_links_hint)) },
                minLines = 3,
                isError = bad.isNotEmpty(),
                supportingText =
                    if (bad.isNotEmpty()) {
                        {
                            Text(
                                bad.firstOrNull { it.isNotEmpty() }?.let { stringResource(R.string.downloads_add_not_link, it) }
                                    ?: stringResource(R.string.downloads_add_empty),
                            )
                        }
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth(),
            )
            if (targets.size > 1) {
                Picker(
                    label = stringResource(R.string.downloads_add_downloader),
                    value = target?.name.orEmpty(),
                    options = targets.map { it.name },
                ) { i ->
                    target = targets[i]
                    category = targets[i].category
                }
            }
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(R.string.downloads_add_category)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = savePath,
                onValueChange = { savePath = it },
                label = { Text(stringResource(R.string.downloads_add_save_in)) },
                placeholder = { Text(stringResource(R.string.downloads_add_save_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CheckLine(stringResource(R.string.downloads_add_paused), paused) { paused = it }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button(
                    enabled = target != null && !state.batchBusy,
                    onClick = {
                        val id = target?.id ?: return@Button
                        viewModel.addTorrents(id, links, category, savePath, paused, files) { left ->
                            if (left.isEmpty()) onDismiss() else bad = left
                        }
                    },
                ) { Text(stringResource(R.string.downloads_add_submit)) }
            }
        }
    }
}

private fun readUpload(
    resolver: ContentResolver,
    uri: Uri,
): TorrentUpload? {
    val name =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: return null
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.size > MAX_TORRENT_BYTES) return null
    return TorrentUpload(name, bytes)
}

private const val MAX_TORRENT_BYTES = 16 * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Picker(
    label: String,
    value: String,
    options: List<String>,
    onPick: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    open = false
                    onPick(i)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SteerSheet(
    mode: DownloadAction,
    keys: List<String>,
    viewModel: DownloadsViewModel,
    onDismiss: () -> Unit,
) {
    val start = remember(keys) { viewModel.presenter.steerStart(keys) }
    var ratioMode by remember { mutableStateOf(modeOf(start.ratio)) }
    var ratio by remember { mutableStateOf(if (start.ratio >= 0) DownloadRules.shortRatio(start.ratio) else "2") }
    var seedMode by remember { mutableStateOf(modeOf(start.seedingMinutes.toDouble())) }
    var seed by remember { mutableStateOf(if (start.seedingMinutes >= 0) start.seedingMinutes.toString() else "1440") }
    var idleMode by remember { mutableStateOf(modeOf(start.inactiveMinutes.toDouble())) }
    var idle by remember { mutableStateOf(if (start.inactiveMinutes >= 0) start.inactiveMinutes.toString() else "60") }
    var action by remember { mutableStateOf(start.action) }
    var category by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("") }
    var down by remember { mutableStateOf("0") }
    var up by remember { mutableStateOf("0") }
    var problem by remember { mutableStateOf("") }
    val bad = stringResource(R.string.downloads_limits_bad)
    val needFolder = stringResource(R.string.downloads_location_needed)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            val title =
                when (mode) {
                    DownloadAction.SEEDING_RULES -> R.string.downloads_seeding_title
                    DownloadAction.SET_LOCATION -> R.string.downloads_location_title
                    else -> R.string.downloads_speeds_title
                }
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(
                if (keys.size ==
                    1
                ) {
                    stringResource(R.string.downloads_steer_one)
                } else {
                    stringResource(R.string.downloads_steer_many, keys.size)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (mode) {
                DownloadAction.SEEDING_RULES -> {
                    Text(stringResource(R.string.downloads_seeding_hint), style = MaterialTheme.typography.bodyMedium)
                    LimitGroup(stringResource(R.string.downloads_seeding_ratio), ratioMode, ratio, { ratioMode = it }, { ratio = it })
                    LimitGroup(stringResource(R.string.downloads_seeding_time), seedMode, seed, { seedMode = it }, {
                        seed =
                            it.filter(Char::isDigit)
                    })
                    LimitGroup(stringResource(R.string.downloads_seeding_idle), idleMode, idle, { idleMode = it }, {
                        idle =
                            it.filter(Char::isDigit)
                    })
                    val actions =
                        listOfNotNull(
                            "" to R.string.downloads_then_default,
                            "stop" to CommonR.string.downloads_then_stop,
                            "remove" to CommonR.string.downloads_then_remove,
                            if (start.action == "remove_files") "remove_files" to CommonR.string.downloads_then_remove_files else null,
                            if (start.action == "super_seed") "super_seed" to CommonR.string.downloads_then_super else null,
                        )
                    val labels = actions.map { stringResource(it.second) }
                    Picker(
                        stringResource(R.string.downloads_seeding_then),
                        labels[actions.indexOfFirst { it.first == action }.coerceAtLeast(0)],
                        labels,
                    ) {
                        action = actions[it].first
                    }
                }
                DownloadAction.SET_LOCATION -> {
                    Text(stringResource(R.string.downloads_location_hint), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(category, {
                        category = it
                    }, label = {
                        Text(
                            stringResource(R.string.downloads_location_category),
                        )
                    }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        folder,
                        { folder = it },
                        label = { Text(stringResource(R.string.downloads_location_folder)) },
                        placeholder = { Text(stringResource(R.string.downloads_location_folder_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    Text(stringResource(R.string.downloads_speeds_hint), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        NumberField(stringResource(R.string.downloads_speed_download), down, Modifier.weight(1f)) { down = it }
                        NumberField(stringResource(R.string.downloads_speed_upload), up, Modifier.weight(1f)) { up = it }
                    }
                }
            }
            if (problem.isNotEmpty()) Text(problem, color = MaterialTheme.acolyte.failText)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = {
                    val done = { ok: Boolean -> if (ok) onDismiss() }
                    when (mode) {
                        DownloadAction.SEEDING_RULES -> {
                            val r = valueOf(ratioMode, ratio.toDoubleOrNull() ?: -3.0)
                            val s = valueOf(seedMode, seed.toDoubleOrNull() ?: -3.0).toLong()
                            val i = valueOf(idleMode, idle.toDoubleOrNull() ?: -3.0).toLong()
                            if (r < -2 || s < -2 || i < -2 || DownloadRules.limitsProblem(r, s, i)) {
                                problem = bad
                            } else {
                                viewModel.shareLimits(keys, r, s, i, action, done)
                            }
                        }
                        DownloadAction.SET_LOCATION ->
                            if (folder.isBlank()) problem = needFolder else viewModel.location(keys, category, folder, done)
                        else -> viewModel.speeds(keys, down.toDoubleOrNull() ?: 0.0, up.toDoubleOrNull() ?: 0.0, done)
                    }
                }) {
                    Text(
                        stringResource(
                            if (mode == DownloadAction.SET_LOCATION) R.string.downloads_location_move else R.string.downloads_save,
                        ),
                    )
                }
            }
        }
    }
}

private enum class LimitMode { GLOBAL, NONE, SET }

private fun modeOf(v: Double): LimitMode =
    if (v == -2.0) {
        LimitMode.GLOBAL
    } else if (v < 0) {
        LimitMode.NONE
    } else {
        LimitMode.SET
    }

private fun valueOf(
    mode: LimitMode,
    v: Double,
): Double =
    when (mode) {
        LimitMode.GLOBAL -> -2.0
        LimitMode.NONE -> -1.0
        LimitMode.SET -> v
    }

@Composable
private fun LimitGroup(
    title: String,
    mode: LimitMode,
    value: String,
    onMode: (LimitMode) -> Unit,
    onValue: (String) -> Unit,
) {
    ChipGroup(title) {
        FilterChip(
            mode == LimitMode.GLOBAL,
            { onMode(LimitMode.GLOBAL) },
            label = { Text(stringResource(R.string.downloads_limit_global)) },
        )
        FilterChip(mode == LimitMode.NONE, { onMode(LimitMode.NONE) }, label = { Text(stringResource(R.string.downloads_limit_none)) })
        FilterChip(mode == LimitMode.SET, { onMode(LimitMode.SET) }, label = { Text(stringResource(R.string.downloads_limit_set)) })
    }
    if (mode == LimitMode.SET) NumberField(title, value, Modifier.width(200.dp), onValue)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MatchesSheet(
    state: DownloadsState,
    viewModel: DownloadsViewModel,
    onDismiss: () -> Unit,
) {
    val matches = state.matches
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.downloads_matches_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.downloads_matches_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (matches.pending >
                0
            ) {
                Text(stringResource(R.string.downloads_matches_pending, matches.pending), style = MaterialTheme.typography.bodySmall)
            }
            if (matches.matches.isEmpty()) Text(stringResource(R.string.downloads_matches_all_done))
            matches.matches.forEach { m ->
                val busy = "match-${m.id}" in state.busy
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Text(m.name, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (m.candidates.isEmpty()) {
                            Text(
                                stringResource(R.string.downloads_matches_none_found),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        m.candidates.forEach { c ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                            ) {
                                ServerImage(
                                    source = c.poster.ifEmpty { null },
                                    contentDescription = null,
                                    placeholderName = c.name,
                                    modifier = Modifier.size(36.dp, 54.dp).clip(RoundedCornerShape(Radius.Poster / 2)),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(
                                            if (c.kind ==
                                                "show"
                                            ) {
                                                R.string.downloads_matches_show
                                            } else {
                                                R.string.downloads_matches_movie
                                            },
                                            c.year,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(enabled = !busy, onClick = { viewModel.pickMatch(m.id, c.id, false) }) {
                                    Text(stringResource(R.string.downloads_matches_open))
                                }
                            }
                        }
                        Row {
                            TextButton(enabled = !busy, onClick = { viewModel.pickMatch(m.id, null, true) }) {
                                Text(stringResource(R.string.downloads_matches_not_media))
                            }
                            TextButton(enabled = !busy, onClick = { viewModel.pickMatch(m.id, null, false) }) {
                                Text(stringResource(R.string.downloads_matches_skip))
                            }
                            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(
                    onClick = viewModel::runMatches,
                    enabled = !state.batchBusy,
                ) { Text(stringResource(R.string.downloads_matches_run)) }
                Button(onClick = onDismiss) { Text(stringResource(R.string.downloads_history_done)) }
            }
        }
    }
}
