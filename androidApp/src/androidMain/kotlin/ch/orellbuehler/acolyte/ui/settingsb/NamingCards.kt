package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingChoice
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingState
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingStyle
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingSwitch
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

private val MOVIE_FIELDS = listOf(SettingsNamingText.FILE_PATTERN, SettingsNamingText.FOLDER_PATTERN)
private val SHOW_FIELDS =
    listOf(
        SettingsNamingText.SHOW_FOLDER_PATTERN,
        SettingsNamingText.SEASON_FOLDER_PATTERN,
        SettingsNamingText.EPISODE_PATTERN,
        SettingsNamingText.DAILY_PATTERN,
        SettingsNamingText.ANIME_PATTERN,
    )
private val MUSIC_FIELDS =
    listOf(
        SettingsNamingText.ARTIST_FOLDER_PATTERN,
        SettingsNamingText.ALBUM_FOLDER_PATTERN,
        SettingsNamingText.TRACK_PATTERN,
        SettingsNamingText.MULTI_DISC_TRACK_PATTERN,
        SettingsNamingText.DISC_FOLDER_PATTERN,
    )

@Composable
private fun AdvancedToggle(
    open: Boolean,
    onToggle: () -> Unit,
) {
    TextButton(onClick = onToggle) {
        Text(stringResource(R.string.acq_nm_advanced) + if (open) " −" else " +")
    }
}

@Composable
private fun WordsButton(
    editor: NamingEditor,
    card: List<SettingsNamingText>,
    fallback: SettingsNamingText,
) {
    TextButton(onClick = { editor.sheet = editor.target(card, fallback) }) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.acq_nm_words), modifier = Modifier.padding(start = Spacing.Small))
    }
}

@Composable
private fun ChoiceField(
    choice: SettingsNamingChoice,
    label: String,
    hint: String?,
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
    enabled: Boolean = true,
) {
    PickerField(
        label = label,
        options = choice.options.map { it to namingChoiceLabel(choice, it) },
        selected = state.choice(choice),
        onSelect = { vm.setChoice(choice, it) },
        enabled = enabled,
        hint = hint,
    )
}

@Composable
private fun FlagRow(
    field: SettingsNamingSwitch,
    title: String,
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
    hint: String? = null,
    enabled: Boolean = true,
) {
    SwitchRow(title, state.isOn(field), { vm.setSwitch(field, it) }, hint = hint, enabled = enabled)
}

@Composable
private fun Pattern(
    field: SettingsNamingText,
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
    editor: NamingEditor,
    hint: String? = null,
    enabled: Boolean = true,
) {
    NamingPatternField(field, state, editor, namingFieldLabel(field), hint, { vm.setText(field, it) }, enabled)
}

@Composable
fun NamingWhereCard(
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_nm_where)) {
        NamingNumberField(
            stringResource(R.string.acq_nm_holding_days),
            stringResource(R.string.acq_nm_holding_days_hint),
            SettingsNamingNumber.HOLDING_DAYS,
            state,
        ) { vm.setNumber(SettingsNamingNumber.HOLDING_DAYS, it) }
        FlagRow(
            SettingsNamingSwitch.UNKEEP_MOVIES,
            stringResource(R.string.acq_nm_unkeep_movies),
            state,
            vm,
            stringResource(R.string.acq_nm_unkeep_hint),
        )
        FlagRow(
            SettingsNamingSwitch.UNKEEP_EPISODES,
            stringResource(R.string.acq_nm_unkeep_episodes),
            state,
            vm,
            stringResource(R.string.acq_nm_unkeep_hint),
        )
        AdvancedToggle(advanced) { advanced = !advanced }
        if (advanced) {
            NamingNumberField(
                stringResource(R.string.acq_nm_scan_limit),
                stringResource(R.string.acq_nm_scan_limit_hint),
                SettingsNamingNumber.ADOPT_SCAN_LIMIT,
                state,
            ) { vm.setNumber(SettingsNamingNumber.ADOPT_SCAN_LIMIT, it) }
            NamingNumberField(
                stringResource(R.string.acq_nm_music_scan_limit),
                stringResource(R.string.acq_nm_music_scan_limit_hint),
                SettingsNamingNumber.MUSIC_ADOPT_SCAN_LIMIT,
                state,
            ) { vm.setNumber(SettingsNamingNumber.MUSIC_ADOPT_SCAN_LIMIT, it) }
            NamingNumberField(
                stringResource(R.string.acq_nm_min_free),
                stringResource(R.string.acq_nm_min_free_hint),
                SettingsNamingNumber.MIN_FREE_MB,
                state,
            ) { vm.setNumber(SettingsNamingNumber.MIN_FREE_MB, it) }
            FlagRow(
                SettingsNamingSwitch.SKIP_FREE_CHECK,
                stringResource(R.string.acq_nm_skip_check),
                state,
                vm,
                stringResource(R.string.acq_nm_skip_check_hint),
            )
        }
    }
}

@Composable
fun NamingMovieCard(
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
    editor: NamingEditor,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_nm_movie_names)) {
        Text(stringResource(R.string.acq_nm_server_style), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            listOf(
                SettingsNamingStyle.PLEX to R.string.acq_nm_style_plex,
                SettingsNamingStyle.JELLYFIN to R.string.acq_nm_style_jellyfin,
                SettingsNamingStyle.EMBY to R.string.acq_nm_style_emby,
            ).forEach { (style, label) ->
                OutlinedButton(onClick = { vm.applyStyle(style) }, modifier = Modifier.weight(1f)) { Text(stringResource(label)) }
            }
        }
        Hint(stringResource(R.string.acq_nm_server_style_hint))
        Pattern(SettingsNamingText.FILE_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_file_pattern_hint))
        Pattern(SettingsNamingText.FOLDER_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_folder_pattern_hint))
        WordsButton(editor, MOVIE_FIELDS, SettingsNamingText.FILE_PATTERN)
        NamingExampleBox(state, state.exampleFor(SettingsNamingText.FILE_PATTERN))
        FlagRow(
            SettingsNamingSwitch.GROUP_BY_GENRE,
            stringResource(R.string.acq_nm_group_genre),
            state,
            vm,
            stringResource(R.string.acq_nm_group_genre_hint) + " " + stringResource(R.string.acq_nm_group_flat_note),
        )
        FlagRow(
            SettingsNamingSwitch.GROUP_BY_LETTER,
            stringResource(R.string.acq_nm_group_letter),
            state,
            vm,
            stringResource(R.string.acq_nm_group_letter_hint),
        )
        PickerField(
            label = stringResource(R.string.acq_nm_title_language),
            options =
                listOf("" to stringResource(R.string.acq_nm_title_language_default)) + state.languages.map { it.code to it.name },
            selected = state.choice(SettingsNamingChoice.TITLE_LANGUAGE),
            onSelect = { vm.setChoice(SettingsNamingChoice.TITLE_LANGUAGE, it) },
            hint = stringResource(R.string.acq_nm_title_language_hint),
        )
        AdvancedToggle(advanced) { advanced = !advanced }
        if (advanced) {
            ChoiceField(
                SettingsNamingChoice.COLON,
                stringResource(R.string.acq_nm_colon),
                stringResource(R.string.acq_nm_colon_hint),
                state,
                vm,
            )
            ChoiceField(
                SettingsNamingChoice.ILLEGAL,
                stringResource(R.string.acq_nm_illegal),
                stringResource(R.string.acq_nm_illegal_hint),
                state,
                vm,
            )
        }
    }
}

@Composable
fun NamingShowCard(
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
    editor: NamingEditor,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val seasons = state.isOn(SettingsNamingSwitch.SEASON_FOLDERS)
    SettingsCard(stringResource(R.string.acq_nm_show_names)) {
        Pattern(SettingsNamingText.SHOW_FOLDER_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_show_folder_hint))
        FlagRow(
            SettingsNamingSwitch.SHOW_GROUP_BY_LETTER,
            stringResource(R.string.acq_nm_group_letter),
            state,
            vm,
            stringResource(R.string.acq_nm_show_group_letter_hint),
        )
        FlagRow(SettingsNamingSwitch.SEASON_FOLDERS, stringResource(R.string.acq_nm_season_folders), state, vm)
        Pattern(
            SettingsNamingText.SEASON_FOLDER_PATTERN,
            state,
            vm,
            editor,
            stringResource(R.string.acq_nm_season_pattern_hint),
            enabled = seasons,
        )
        Pattern(SettingsNamingText.EPISODE_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_episode_pattern_hint))
        WordsButton(editor, SHOW_FIELDS, SettingsNamingText.EPISODE_PATTERN)
        NamingExampleList(state, state.episodeRows.map { namingExampleLabel(it.key) to it.path })
        AdvancedToggle(advanced) { advanced = !advanced }
        if (advanced) {
            ChoiceField(
                SettingsNamingChoice.SHOW_COLON,
                stringResource(R.string.acq_nm_colon),
                stringResource(R.string.acq_nm_colon_hint),
                state,
                vm,
            )
            ChoiceField(
                SettingsNamingChoice.SHOW_ILLEGAL,
                stringResource(R.string.acq_nm_illegal),
                stringResource(R.string.acq_nm_illegal_hint),
                state,
                vm,
            )
            Pattern(SettingsNamingText.SPECIALS_FOLDER, state, vm, editor, stringResource(R.string.acq_nm_specials_hint), enabled = seasons)
            Pattern(SettingsNamingText.DAILY_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_daily_hint))
            Pattern(SettingsNamingText.ANIME_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_anime_hint))
            ChoiceField(
                SettingsNamingChoice.MULTI_EPISODE,
                stringResource(R.string.acq_nm_multi),
                stringResource(R.string.acq_nm_multi_hint),
                state,
                vm,
            )
            ChoiceField(
                SettingsNamingChoice.AIR_DATE,
                stringResource(R.string.acq_nm_air_day),
                stringResource(R.string.acq_nm_air_day_hint),
                state,
                vm,
            )
            ChoiceField(
                SettingsNamingChoice.ABSOLUTE_DIGITS,
                stringResource(R.string.acq_nm_digits),
                stringResource(R.string.acq_nm_digits_hint),
                state,
                vm,
            )
        }
    }
}

@Composable
fun NamingMusicCard(
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
    editor: NamingEditor,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val discs = state.isOn(SettingsNamingSwitch.DISC_FOLDERS)
    SettingsCard(stringResource(R.string.acq_nm_music_names)) {
        Pattern(SettingsNamingText.ARTIST_FOLDER_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_artist_folder_hint))
        Pattern(SettingsNamingText.ALBUM_FOLDER_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_album_folder_hint))
        Pattern(SettingsNamingText.TRACK_PATTERN, state, vm, editor, stringResource(R.string.acq_nm_track_pattern_hint))
        WordsButton(editor, MUSIC_FIELDS, SettingsNamingText.TRACK_PATTERN)
        NamingExampleList(state, state.musicRows.map { namingExampleLabel(it.key) to it.path })
        AdvancedToggle(advanced) { advanced = !advanced }
        if (advanced) {
            ChoiceField(
                SettingsNamingChoice.MUSIC_COLON,
                stringResource(R.string.acq_nm_colon),
                stringResource(R.string.acq_nm_colon_hint),
                state,
                vm,
            )
            ChoiceField(
                SettingsNamingChoice.MUSIC_ILLEGAL,
                stringResource(R.string.acq_nm_illegal),
                stringResource(R.string.acq_nm_illegal_hint),
                state,
                vm,
            )
            Pattern(
                SettingsNamingText.MULTI_DISC_TRACK_PATTERN,
                state,
                vm,
                editor,
                stringResource(R.string.acq_nm_multi_disc_hint),
                enabled = !discs,
            )
            FlagRow(
                SettingsNamingSwitch.DISC_FOLDERS,
                stringResource(R.string.acq_nm_disc_folders),
                state,
                vm,
                stringResource(R.string.acq_nm_disc_folders_hint),
            )
            Pattern(
                SettingsNamingText.DISC_FOLDER_PATTERN,
                state,
                vm,
                editor,
                stringResource(R.string.acq_nm_disc_pattern_hint),
                enabled = discs,
            )
            FlagRow(
                SettingsNamingSwitch.READ_TAGS,
                stringResource(R.string.acq_nm_read_tags),
                state,
                vm,
                stringResource(R.string.acq_nm_read_tags_hint),
            )
        }
    }
}

@Composable
fun NamingImportingCard(
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
) {
    SettingsCard(stringResource(R.string.acq_nm_importing), subtitle = stringResource(R.string.acq_nm_rename)) {
        FlagRow(SettingsNamingSwitch.RENAME_MOVIES, stringResource(R.string.acq_nm_rename_movies), state, vm)
        FlagRow(SettingsNamingSwitch.RENAME_SHOWS, stringResource(R.string.acq_nm_rename_shows), state, vm)
        FlagRow(SettingsNamingSwitch.RENAME_MUSIC, stringResource(R.string.acq_nm_rename_music), state, vm)
        Hint(stringResource(R.string.acq_nm_rename_hint))
    }
}

@Composable
fun NamingHoldingCard(
    state: SettingsNamingState,
    vm: SettingsNamingViewModel,
) {
    SettingsCard(stringResource(R.string.acq_nm_holding)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.acq_nm_clear_old), style = MaterialTheme.typography.bodyLarge)
                Hint(stringResource(R.string.acq_nm_clear_old_hint))
            }
            if (state.cleaning) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = vm::cleanHolding, enabled = state.canClean) { Text(stringResource(R.string.acq_nm_clear)) }
            }
        }
        state.cleanError?.let { FailText(stringResource(R.string.acq_nm_clean_failed) + ". " + settingsErrorText(it)) }
    }
}
