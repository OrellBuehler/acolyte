package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicKindsPreset
import ch.orellbuehler.acolyte.feature.music.MusicLibraryArtistState
import ch.orellbuehler.acolyte.feature.music.MusicOption
import ch.orellbuehler.acolyte.feature.music.MusicOptions
import ch.orellbuehler.acolyte.feature.music.MusicQuickRule
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import ch.orellbuehler.acolyte.ui.title.SettingSwitch
import kotlinx.coroutines.launch

@Composable
fun MusicLibrarySettings(
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Medium)) {
        Text(stringResource(R.string.music_lib_settings), style = MaterialTheme.typography.titleLarge)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small),
        ) {
            Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                SettingSwitch(
                    stringResource(R.string.music_lib_keep_artist),
                    stringResource(R.string.music_lib_keep_artist_hint),
                    state.keepUpToDate,
                ) { on -> actions.act { setKeepArtist(on) } }
                QualityControl(state, actions)
                KindsControl(state, actions)
                if (state.folders.size > 1) FolderControl(state, actions)
            }
        }
        Column(Modifier.padding(top = Spacing.Small)) {
            SettingRow(
                stringResource(R.string.music_lib_rename),
                stringResource(R.string.music_lib_rename_hint),
                false,
            ) { actions.openSheet("rename") }
            HorizontalDivider()
            val ago = relativeTime(state.refreshedAt)
            SettingRow(
                stringResource(R.string.music_lib_refresh),
                if (ago != null) stringResource(R.string.music_lib_refresh_ago, ago) else stringResource(R.string.music_lib_refresh_hint),
                false,
            ) { actions.act { refreshDetails() } }
            HorizontalDivider()
            SettingRow(stringResource(R.string.music_lib_history), null, false) { actions.nav.openRoute(Routes.history(state.titleId)) }
            HorizontalDivider()
            SettingRow(stringResource(R.string.music_lib_delete), null, true) { actions.openSheet("remove") }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    hint: String?,
    destructive: Boolean,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.Medium)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) MaterialTheme.acolyte.failText else MaterialTheme.colorScheme.onSurface,
        )
        if (!hint.isNullOrEmpty()) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QualityControl(
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    if (state.quick.isEmpty() && state.rules.isEmpty()) return
    var menu by remember { mutableStateOf(false) }
    val count = state.quick.size + 1
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.music_lib_quality), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            state.quick.forEachIndexed { index, quick ->
                SegmentedButton(
                    selected = state.rulesId == quick.id,
                    onClick = { actions.act { setRules(quick.id) } },
                    shape = SegmentedButtonDefaults.itemShape(index, count),
                    label = {
                        Text(
                            stringResource(
                                when (quick.kind) {
                                    MusicQuickRule.ANY -> R.string.music_lib_quality_any
                                    MusicQuickRule.LOSSY -> R.string.music_lib_quality_lossy
                                    MusicQuickRule.LOSSLESS -> R.string.music_lib_quality_lossless
                                },
                            ),
                            maxLines = 1,
                        )
                    },
                )
            }
            SegmentedButton(
                selected = state.customQuality,
                onClick = { menu = true },
                shape = SegmentedButtonDefaults.itemShape(count - 1, count),
                label = {
                    Text(stringResource(R.string.music_lib_quality_custom), maxLines = 1)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        state.rules.forEach { rule ->
                            DropdownMenuItem(
                                text = { Text(rule.name) },
                                onClick = {
                                    menu = false
                                    actions.act { setRules(rule.id) }
                                },
                                trailingIcon = { if (rule.id == state.rulesId) Text("✓") },
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun KindsControl(
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    val presets = listOf(MusicKindsPreset.ALBUMS, MusicKindsPreset.ALBUMS_EPS, MusicKindsPreset.ALBUMS_EPS_SINGLES, MusicKindsPreset.CUSTOM)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(stringResource(R.string.music_lib_kinds), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            presets.forEachIndexed { index, preset ->
                SegmentedButton(
                    selected = state.preset == preset,
                    onClick = {
                        if (preset ==
                            MusicKindsPreset.CUSTOM
                        ) {
                            actions.openSheet("kinds")
                        } else {
                            actions.act { applyPreset(preset) }
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, presets.size),
                    label = {
                        Text(
                            stringResource(
                                when (preset) {
                                    MusicKindsPreset.ALBUMS -> R.string.music_lib_preset_albums
                                    MusicKindsPreset.ALBUMS_EPS -> R.string.music_lib_preset_eps
                                    MusicKindsPreset.ALBUMS_EPS_SINGLES -> R.string.music_lib_preset_singles
                                    MusicKindsPreset.CUSTOM -> R.string.music_lib_preset_custom
                                },
                            ),
                            maxLines = 1,
                        )
                    },
                )
            }
        }
        val kinds =
            stringResource(
                when (state.preset) {
                    MusicKindsPreset.ALBUMS -> R.string.music_lib_kinds_albums
                    MusicKindsPreset.ALBUMS_EPS -> R.string.music_lib_kinds_albums_eps
                    MusicKindsPreset.ALBUMS_EPS_SINGLES -> R.string.music_lib_kinds_all
                    MusicKindsPreset.CUSTOM -> R.string.music_lib_kinds_mix
                },
            )
        val fresh =
            stringResource(
                when (state.newAlbums) {
                    "new" -> R.string.music_lib_new_new
                    "none" -> R.string.music_lib_new_none
                    else -> R.string.music_lib_new_all
                },
            )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.music_lib_kinds_hint, kinds, fresh),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { actions.openSheet("kinds") }) { Text(stringResource(R.string.music_lib_choose_kinds)) }
        }
    }
}

@Composable
private fun FolderControl(
    state: MusicLibraryArtistState,
    actions: MusicLibraryActions,
) {
    var menu by remember { mutableStateOf(false) }
    val current = state.folders.firstOrNull { it.id == state.folderId }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Text(stringResource(R.string.music_lib_folder), style = MaterialTheme.typography.titleSmall)
        Box {
            TextButton(onClick = { menu = true }) { Text(current?.label ?: state.folderPath) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                state.folders.filter { it.id != state.folderId }.forEach { folder ->
                    DropdownMenuItem(
                        text = { Text(folder.label) },
                        onClick = {
                            menu = false
                            actions.askMove(folder.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun optionText(option: MusicOption): String =
    when (option.key) {
        "all" -> stringResource(R.string.music_lib_opt_all)
        "new" -> stringResource(R.string.music_lib_opt_new)
        "none" -> stringResource(R.string.music_lib_opt_none)
        "album" -> stringResource(R.string.music_lib_opt_album)
        "ep" -> stringResource(R.string.music_lib_opt_ep)
        "single" -> stringResource(R.string.music_lib_opt_single)
        "broadcast" -> stringResource(R.string.music_lib_opt_broadcast)
        "other" -> stringResource(R.string.music_lib_opt_other)
        "studio" -> stringResource(R.string.music_lib_opt_studio)
        "live" -> stringResource(R.string.music_lib_opt_live)
        "compilation" -> stringResource(R.string.music_lib_opt_compilation)
        "soundtrack" -> stringResource(R.string.music_lib_opt_soundtrack)
        "remix" -> stringResource(R.string.music_lib_opt_remix)
        "spokenword" -> stringResource(R.string.music_lib_opt_spokenword)
        "interview" -> stringResource(R.string.music_lib_opt_interview)
        "audiobook" -> stringResource(R.string.music_lib_opt_audiobook)
        "audio_drama" -> stringResource(R.string.music_lib_opt_audio_drama)
        "dj_mix" -> stringResource(R.string.music_lib_opt_dj_mix)
        "mixtape" -> stringResource(R.string.music_lib_opt_mixtape)
        "demo" -> stringResource(R.string.music_lib_opt_demo)
        "field_recording" -> stringResource(R.string.music_lib_opt_field_recording)
        "official" -> stringResource(R.string.music_lib_opt_official)
        "promotion" -> stringResource(R.string.music_lib_opt_promotion)
        "bootleg" -> stringResource(R.string.music_lib_opt_bootleg)
        "pseudo_release" -> stringResource(R.string.music_lib_opt_pseudo_release)
        else -> option.id
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MusicKindsSheet(
    state: MusicLibraryArtistState,
    onSave: suspend (String, List<String>, List<String>, List<String>) -> Boolean,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var newAlbums by remember { mutableStateOf(state.newAlbums) }
    var types by remember { mutableStateOf(state.albumTypes) }
    var secondary by remember { mutableStateOf(state.secondaryTypes) }
    var statuses by remember { mutableStateOf(state.statuses) }
    var more by rememberSaveable { mutableStateOf(false) }
    val canSave = types.isNotEmpty() && secondary.isNotEmpty() && statuses.isNotEmpty() && !state.working
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.Screen)
                .padding(bottom = Spacing.XLarge)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(stringResource(R.string.music_lib_kinds_title), style = MaterialTheme.typography.titleLarge)
            Text(state.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.music_lib_kinds_new_albums), style = MaterialTheme.typography.titleSmall)
            MusicOptions.newAlbums.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().clickable { newAlbums = option.id },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = newAlbums == option.id, onClick = { newAlbums = option.id })
                    Text(optionText(option), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(stringResource(R.string.music_lib_kinds_types), style = MaterialTheme.typography.titleSmall)
            OptionChecks(MusicOptions.albumTypes, types) { types = it }
            TextButton(onClick = { more = !more }) {
                Text(stringResource(if (more) R.string.music_lib_kinds_fewer else R.string.music_lib_kinds_more))
            }
            if (more) {
                Text(stringResource(R.string.music_lib_kinds_extra), style = MaterialTheme.typography.titleSmall)
                OptionChecks(MusicOptions.secondaryTypes, secondary) { secondary = it }
                Text(stringResource(R.string.music_lib_kinds_statuses), style = MaterialTheme.typography.titleSmall)
                OptionChecks(MusicOptions.statuses, statuses) { statuses = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), modifier = Modifier.padding(top = Spacing.Small)) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.common_cancel)) }
                Button(
                    onClick = { scope.launch { if (onSave(newAlbums, types, secondary, statuses)) onClose() } },
                    enabled = canSave,
                ) { Text(stringResource(R.string.music_lib_kinds_save)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChecks(
    options: List<MusicOption>,
    selected: List<String>,
    onChange: (List<String>) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        options.forEach { option ->
            val on = option.id in selected
            Row(
                Modifier.clickable { onChange(MusicOptions.toggle(selected, option.id, !on, options)) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = on, onCheckedChange = { onChange(MusicOptions.toggle(selected, option.id, it, options)) })
                Text(optionText(option), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 12.dp))
            }
        }
    }
}
