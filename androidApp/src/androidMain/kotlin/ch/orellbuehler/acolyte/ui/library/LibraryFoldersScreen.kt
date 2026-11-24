package ch.orellbuehler.acolyte.ui.library

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.feature.library.FolderHealth
import ch.orellbuehler.acolyte.feature.library.FolderRow
import ch.orellbuehler.acolyte.feature.library.LibraryFoldersPresenter
import ch.orellbuehler.acolyte.feature.library.LibraryFoldersState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.launch

private typealias FoldersModel = PresenterViewModel<LibraryFoldersPresenter>

@Composable
fun LibraryFoldersScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val model: FoldersModel = viewModel(key = "library-folders") { PresenterViewModel(sdk.libraryFolders()) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableLongStateOf(0L) }

    ScreenScaffold(
        title = stringResource(R.string.library_folders_title),
        onBack = onBack,
        actions = {
            if (state.canEdit &&
                !state.isEmpty
            ) {
                TextButton(onClick = { adding = true }) { Text(stringResource(R.string.library_folders_add)) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            StatusContent(
                status = state.status,
                isEmpty = false,
                onRefresh = { model.run { refresh() } },
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Spacing.Screen, end = Spacing.Screen, bottom = Spacing.XLarge),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    if (state.isOwner) item { HandsOffCard(state, model) }
                    if (state.isEmpty) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                EmptyView(
                                    title = stringResource(R.string.library_folders_empty_title),
                                    message = stringResource(R.string.library_folders_empty_message),
                                    icon = Icons.Rounded.Folder,
                                    modifier = Modifier.size(width = 320.dp, height = 220.dp),
                                )
                                if (state.canEdit) {
                                    Button(
                                        onClick = { adding = true },
                                    ) { Text(stringResource(R.string.library_folders_add)) }
                                }
                            }
                        }
                    }
                    groups(state) { editing = it }
                    state.importFolders?.let { item { ImportFoldersCard(state, model) } }
                    state.sideFiles?.let { item { SideFilesCard(state, model) } }
                }
            }
            NoticeHost(state.notice) { model.presenter.dismissNotice(it) }
        }
    }
    if (adding) AddFolderSheet(state, model) { adding = false }
    state.groups.flatMap { it.folders }.firstOrNull { it.folder.id == editing }?.let { row ->
        FolderSheet(row, state, model) { editing = 0L }
    }
    if (state.needsPassword) PasswordDialog(state, model)
}

private fun LazyListScope.groups(
    state: LibraryFoldersState,
    onEdit: (Long) -> Unit,
) {
    state.groups.forEach { group ->
        item(key = "group-${group.kind}") {
            Text(
                "${folderKindLabel(group.kind)}  ${group.folders.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.Small),
            )
        }
        items(group.folders, key = { it.folder.id }) { row -> FolderCard(row, state.canEdit) { onEdit(row.folder.id) } }
    }
}

@Composable
private fun folderKindLabel(kind: String): String =
    stringResource(
        when (kind) {
            "show" -> R.string.library_folders_kind_show
            "music" -> R.string.library_folders_kind_music
            else -> R.string.library_folders_kind_movie
        },
    )

@Composable
private fun healthLabel(health: FolderHealth): String =
    stringResource(
        when (health) {
            FolderHealth.OK -> R.string.library_folders_health_ok
            FolderHealth.MISSING -> R.string.library_folders_health_missing
            FolderHealth.READ_ONLY -> R.string.library_folders_health_read_only
            FolderHealth.NO_HARDLINK -> R.string.library_folders_health_no_hardlink
            FolderHealth.COPIES_EXPECTED -> R.string.library_folders_health_copies
            FolderHealth.LOOK_ONLY -> R.string.library_folders_health_look_only
            FolderHealth.UNKNOWN -> R.string.library_folders_health_unknown
        },
    )

@Composable
private fun FolderCard(
    row: FolderRow,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    val folder = row.folder
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        folder.label.ifEmpty { folder.path },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MutedText(row.shortPath)
                }
                if (row.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            StatusChip(healthLabel(row.health), row.tone)
            if (row.health == FolderHealth.OK) MutedText(stringResource(R.string.library_folders_health_ok_hint))
            if (folder.healthReason.isNotEmpty()) MutedText(folder.healthReason)
            if (folder.freeSpace > 0) MutedText(stringResource(R.string.library_folders_free, LibraryFormat.bytes(folder.freeSpace)))
            val rules = if (row.rulesIsDefault) stringResource(R.string.library_folders_default) else row.rulesName
            MutedText(stringResource(R.string.library_folders_quality) + ": " + rules)
            if (folder.titleTags.isNotEmpty()) {
                MutedText(
                    stringResource(R.string.library_folders_tags) + ": " + folder.titleTags.joinToString(", "),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Text(
                        if (canEdit) {
                            stringResource(R.string.library_folders_edit, folder.label.ifEmpty { folder.path })
                        } else {
                            stringResource(R.string.library_open)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesPicker(
    rules: List<PickRule>,
    current: Long,
    onPick: (Long) -> Unit,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        MutedText(stringResource(R.string.library_folders_quality))
        Box {
            OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(
                    rules.firstOrNull { it.id == current }?.name ?: stringResource(R.string.library_folders_default),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Icon(Icons.Rounded.ExpandMore, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.library_folders_default)) }, onClick = {
                    open = false
                    onPick(0)
                })
                rules.forEach { rule ->
                    DropdownMenuItem(text = { Text(rule.name) }, onClick = {
                        open = false
                        onPick(rule.id)
                    })
                }
            }
        }
        MutedText(stringResource(R.string.library_folders_quality_hint))
    }
}

@Composable
private fun SwitchRow(
    title: String,
    hint: String?,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = hint?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFolderSheet(
    state: LibraryFoldersState,
    model: FoldersModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var kind by rememberSaveable { mutableStateOf("movie") }
    var path by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var rules by rememberSaveable { mutableLongStateOf(0L) }
    var tags by rememberSaveable { mutableStateOf("") }
    var onlyTagged by rememberSaveable { mutableStateOf(false) }
    var lookOnly by rememberSaveable { mutableStateOf(false) }
    var flat by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            item { Text(stringResource(R.string.library_folders_add), style = MaterialTheme.typography.titleLarge) }
            item {
                MutedText(stringResource(R.string.library_folders_holds))
                val kinds = listOf("movie", "show", "music")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    kinds.forEachIndexed { index, k ->
                        SegmentedButton(selected = kind == k, onClick = {
                            kind = k
                            rules = 0
                        }, shape = SegmentedButtonDefaults.itemShape(index, kinds.size)) { Text(folderKindLabel(k), maxLines = 1) }
                    }
                }
                MutedText(stringResource(R.string.library_folders_holds_hint))
            }
            item {
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text(stringResource(R.string.library_folders_path)) },
                    placeholder = { Text("/$kind" + if (kind == "music") "" else "s") },
                    supportingText = { Text(stringResource(R.string.library_folders_path_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(64) },
                    label = { Text(stringResource(R.string.library_folders_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.rules != null) item { RulesPicker(model.presenter.rulesFor(kind), rules, { rules = it }) }
            item {
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.library_folders_tags)) },
                    supportingText = { Text(stringResource(R.string.library_folders_tags_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (tags.isNotBlank()) {
                item {
                    SwitchRow(
                        stringResource(R.string.library_folders_only_tagged),
                        stringResource(R.string.library_folders_only_tagged_hint),
                        onlyTagged,
                        true,
                    ) { onlyTagged = it }
                }
            }
            item {
                SwitchRow(
                    stringResource(R.string.library_folders_look_only),
                    stringResource(R.string.library_folders_look_only_hint),
                    lookOnly,
                    true,
                ) {
                    lookOnly = it
                }
            }
            if (kind == "movie") {
                item {
                    SwitchRow(
                        stringResource(R.string.library_folders_flat),
                        stringResource(R.string.library_folders_flat_hint),
                        flat,
                        true,
                    ) {
                        flat =
                            it
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) }
                    Button(
                        onClick = {
                            scope.launch {
                                val done = model.presenter.add(kind, path, label, rules, tags.split(','), onlyTagged, lookOnly, flat)
                                if (done) onDismiss()
                            }
                        },
                        enabled = path.isNotBlank() && !state.adding,
                    ) { Text(stringResource(R.string.library_folders_add_button)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderSheet(
    row: FolderRow,
    state: LibraryFoldersState,
    model: FoldersModel,
    onDismiss: () -> Unit,
) {
    val folder = row.folder
    val edit = state.canEdit && !row.busy
    var rename by remember { mutableStateOf(false) }
    var move by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf(false) }
    var tags by rememberSaveable(folder.id) { mutableStateOf(folder.titleTags.joinToString(", ")) }
    var audio by rememberSaveable(folder.id) { mutableStateOf(folder.titleAudio.joinToString(", ")) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            item {
                Text(folder.label.ifEmpty { folder.path }, style = MaterialTheme.typography.titleLarge)
                Text(folder.path, style = MaterialTheme.typography.bodySmall)
                StatusChip(healthLabel(row.health), row.tone)
                if (folder.healthCheckedAt.isNotEmpty()) {
                    MutedText(
                        stringResource(R.string.library_folders_checked, LibraryFormat.ago(folder.healthCheckedAt)),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    OutlinedButton(
                        onClick = { model.run { check(folder.id) } },
                        enabled = !row.busy,
                    ) { Text(stringResource(R.string.library_folders_check)) }
                    if (state.canEdit && !folder.lookOnly && (folder.health == "no_hardlink")) {
                        OutlinedButton(onClick = { model.run { setCopiesExpected(folder.id, !folder.copiesExpected) } }, enabled = edit) {
                            val label =
                                if (folder.copiesExpected) R.string.library_folders_warn_again else R.string.library_folders_copies_expected
                            Text(stringResource(label))
                        }
                    }
                }
            }
            if (state.rules != null) {
                item {
                    RulesPicker(
                        model.presenter.rulesFor(folder.kind),
                        folder.defaultPickRulesId ?: 0,
                        { model.run { setRules(folder.id, it) } },
                        edit,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.library_folders_tags)) },
                    supportingText = { Text(stringResource(R.string.library_folders_tags_hint)) },
                    singleLine = true,
                    enabled = edit,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { model.run { setTags(folder.id, tags.split(','), folder.onlyTaggedSources) } },
                    enabled = edit && tags != folder.titleTags.joinToString(", "),
                ) { Text(stringResource(R.string.library_save)) }
            }
            if (folder.titleTags.isNotEmpty()) {
                item {
                    SwitchRow(
                        stringResource(R.string.library_folders_only_tagged),
                        stringResource(R.string.library_folders_only_tagged_hint),
                        folder.onlyTaggedSources,
                        edit,
                    ) { model.run { setTags(folder.id, folder.titleTags, it) } }
                }
            }
            if (folder.kind != "music") {
                item {
                    OutlinedTextField(
                        value = audio,
                        onValueChange = { audio = it },
                        label = { Text(stringResource(R.string.library_folders_audio)) },
                        supportingText = { Text(stringResource(R.string.library_folders_audio_hint)) },
                        singleLine = true,
                        enabled = edit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = { model.run { setAudio(folder.id, audio) } },
                        enabled = edit && audio != folder.titleAudio.joinToString(", "),
                    ) { Text(stringResource(R.string.library_save)) }
                }
            }
            item {
                SwitchRow(
                    stringResource(R.string.library_folders_look_only),
                    stringResource(R.string.library_folders_look_only_hint),
                    folder.lookOnly,
                    edit,
                ) {
                    model.run { setLookOnly(folder.id, it) }
                }
            }
            if (folder.kind == "movie") {
                item {
                    SwitchRow(
                        stringResource(R.string.library_folders_flat),
                        stringResource(R.string.library_folders_flat_hint),
                        folder.flat,
                        edit,
                    ) {
                        model.run { setFlat(folder.id, it) }
                    }
                }
            }
            if (state.canEdit) {
                item { HorizontalDivider() }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        TextButton(onClick = { rename = true }, enabled = edit) { Text(stringResource(R.string.library_folders_rename)) }
                        TextButton(onClick = { move = true }, enabled = edit) { Text(stringResource(R.string.library_folders_change_path)) }
                        TextButton(onClick = { remove = true }, enabled = edit) {
                            Text(stringResource(R.string.library_folders_remove), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    if (rename) {
        TextInputDialog(
            title = stringResource(R.string.library_folders_rename),
            label = stringResource(R.string.library_folders_name),
            confirm = stringResource(R.string.library_save),
            initial = folder.label,
            maxLength = 64,
            onConfirm = { model.run { rename(folder.id, it) } },
            onDismiss = { rename = false },
        )
    }
    if (move) {
        TextInputDialog(
            title = stringResource(R.string.library_folders_change_path),
            label = stringResource(R.string.library_folders_new_path),
            hint = stringResource(R.string.library_folders_change_path_hint),
            confirm = stringResource(R.string.library_folders_change_path),
            initial = folder.path,
            maxLength = 1024,
            onConfirm = { model.run { changePath(folder.id, it) } },
            onDismiss = { move = false },
        )
    }
    if (remove) {
        ConfirmDialog(
            title = stringResource(R.string.library_folders_remove_title, folder.path),
            text = stringResource(R.string.library_folders_remove_text),
            confirm = stringResource(R.string.library_folders_remove),
            danger = true,
            onConfirm = {
                onDismiss()
                model.run { remove(folder.id) }
            },
            onDismiss = { remove = false },
        )
    }
}

@Composable
private fun HandsOffCard(
    state: LibraryFoldersState,
    model: FoldersModel,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.library_folders_hands_off_title), style = MaterialTheme.typography.titleSmall)
            MutedText(stringResource(R.string.library_folders_hands_off_about))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.library_folders_hands_off_toggle))
                    MutedText(stringResource(R.string.library_folders_hands_off_hint))
                }
                Switch(checked = state.handsOff, onCheckedChange = { model.run { setHandsOff(it) } }, enabled = !state.handsOffBusy)
            }
        }
    }
}

@Composable
private fun ImportFoldersCard(
    state: LibraryFoldersState,
    model: FoldersModel,
) {
    val folders = state.importFolders ?: return
    var extra by rememberSaveable { mutableStateOf(folders.extraFolders.joinToString("\n")) }
    var watch by rememberSaveable { mutableStateOf(folders.watchFolders.joinToString("\n")) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.library_folders_import_title), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = extra,
                onValueChange = { extra = it },
                label = { Text(stringResource(R.string.library_folders_import_folders)) },
                supportingText = { Text(stringResource(R.string.library_folders_import_hint)) },
                minLines = 2,
                enabled = state.canSaveImportFolders,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = watch,
                onValueChange = { watch = it },
                label = { Text(stringResource(R.string.library_folders_watch_folders)) },
                supportingText = { Text(stringResource(R.string.library_folders_watch_hint)) },
                minLines = 2,
                enabled = state.canSaveImportFolders,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.canSaveImportFolders) {
                Button(onClick = { model.run { saveImportFolders(extra, watch) } }) { Text(stringResource(R.string.library_save)) }
            } else {
                MutedText(stringResource(R.string.library_folders_owner_admins))
            }
        }
    }
}

@Composable
private fun SideFilesCard(
    state: LibraryFoldersState,
    model: FoldersModel,
) {
    val side = state.sideFiles ?: return
    var on by rememberSaveable { mutableStateOf(side.on) }
    var types by rememberSaveable { mutableStateOf(side.types.joinToString(", ")) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.library_folders_side_title), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.library_folders_side_toggle))
                    MutedText(stringResource(R.string.library_folders_side_hint))
                }
                Switch(checked = on, onCheckedChange = { on = it }, enabled = state.canEdit)
            }
            OutlinedTextField(
                value = types,
                onValueChange = { types = it },
                label = { Text(stringResource(R.string.library_folders_side_types)) },
                supportingText = { Text(stringResource(R.string.library_folders_side_types_hint)) },
                singleLine = true,
                enabled = state.canEdit,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.canEdit) Button(onClick = { model.run { saveSideFiles(on, types) } }) { Text(stringResource(R.string.library_save)) }
        }
    }
}

@Composable
private fun PasswordDialog(
    state: LibraryFoldersState,
    model: FoldersModel,
) {
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { model.presenter.cancelPassword() },
        title = { Text(stringResource(R.string.library_folders_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(stringResource(R.string.library_folders_password_text))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.library_folders_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(32) },
                    label = { Text(stringResource(R.string.library_folders_code)) },
                    singleLine = true,
                )
                state.passwordError?.let { MutedText(libraryErrorText(it)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { model.run { confirmPassword(password, code) } },
                enabled = password.isNotEmpty() && !state.handsOffBusy,
            ) {
                Text(stringResource(R.string.library_folders_confirm))
            }
        },
        dismissButton = { TextButton(onClick = { model.presenter.cancelPassword() }) { Text(stringResource(R.string.library_cancel)) } },
    )
}
