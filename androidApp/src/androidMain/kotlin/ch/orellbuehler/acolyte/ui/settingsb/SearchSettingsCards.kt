package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchChoice
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchNumberError
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchState
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSearchSwitch
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
private fun SearchNumberField(
    field: SettingsSearchNumber,
    label: Int,
    hint: Int,
    state: SettingsSearchState,
    vm: SettingsSearchViewModel,
) {
    val error = state.numberError(field)
    OutlinedTextField(
        value = state.number(field),
        onValueChange = { vm.setNumber(field, it) },
        label = { Text(stringResource(label)) },
        supportingText = {
            Text(
                when (error) {
                    SettingsSearchNumberError.FEED -> stringResource(R.string.acq_se_err_feed)
                    SettingsSearchNumberError.RANGE -> stringResource(R.string.acq_se_err_range, field.min, field.max)
                    null -> stringResource(hint)
                },
            )
        },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = if (field.min < 0) KeyboardType.Text else KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SearchFlag(
    field: SettingsSearchSwitch,
    title: Int,
    hint: Int?,
    state: SettingsSearchState,
    vm: SettingsSearchViewModel,
) {
    SwitchRow(stringResource(title), state.isOn(field), { vm.setSwitch(field, it) }, hint = hint?.let { stringResource(it) })
}

@Composable
private fun AdvancedToggle(
    open: Boolean,
    onToggle: () -> Unit,
) {
    TextButton(onClick = onToggle) { Text(stringResource(R.string.acq_se_advanced) + if (open) " −" else " +") }
}

@Composable
fun SearchNightlyCard(
    state: SettingsSearchState,
    vm: SettingsSearchViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_se_nightly)) {
        SearchNumberField(SettingsSearchNumber.MISSING_COUNT, R.string.acq_se_count, R.string.acq_se_count_hint, state, vm)
        SearchFlag(SettingsSearchSwitch.MISSING_BETTER, R.string.acq_se_better_nightly, R.string.acq_se_better_nightly_hint, state, vm)
        SearchFlag(SettingsSearchSwitch.MISSING_SHOWS, R.string.acq_se_episodes, R.string.acq_se_episodes_hint, state, vm)
        AdvancedToggle(advanced) { advanced = !advanced }
        if (advanced) {
            SearchNumberField(SettingsSearchNumber.FEED_MINUTES, R.string.acq_se_feed_minutes, R.string.acq_se_feed_minutes_hint, state, vm)
        }
    }
}

@Composable
fun SearchEarlyCard(
    state: SettingsSearchState,
    vm: SettingsSearchViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_se_early), subtitle = stringResource(R.string.acq_se_early_desc)) {
        SearchFlag(SettingsSearchSwitch.REFUSE_EARLY, R.string.acq_se_refuse_early, R.string.acq_se_refuse_early_hint, state, vm)
        SearchFlag(SettingsSearchSwitch.REFUSE_TINY, R.string.acq_se_refuse_tiny, R.string.acq_se_refuse_tiny_hint, state, vm)
        SearchFlag(SettingsSearchSwitch.SIZE_FLOORS, R.string.acq_se_floors, R.string.acq_se_floors_hint, state, vm)
        SearchNumberField(SettingsSearchNumber.MIN_VIDEO_MB, R.string.acq_se_min_video, R.string.acq_se_min_video_hint, state, vm)
        SearchNumberField(SettingsSearchNumber.MIN_TRACK_KB, R.string.acq_se_min_track, R.string.acq_se_min_track_hint, state, vm)
        SearchNumberField(
            SettingsSearchNumber.EPISODE_MINUTES,
            R.string.acq_se_episode_minutes,
            R.string.acq_se_episode_minutes_hint,
            state,
            vm,
        )
        SearchFlag(SettingsSearchSwitch.HOLD_UNKNOWN, R.string.acq_se_hold, R.string.acq_se_hold_hint, state, vm)
        Hint(stringResource(R.string.acq_se_program_note))
        AdvancedToggle(advanced) { advanced = !advanced }
        if (advanced) {
            SearchNumberField(SettingsSearchNumber.GRACE_HOURS, R.string.acq_se_grace, R.string.acq_se_grace_hint, state, vm)
        }
    }
}

@Composable
fun SearchPacksCard(
    state: SettingsSearchState,
    vm: SettingsSearchViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_se_packs)) {
        PickerField(
            label = stringResource(R.string.acq_se_pack_upgrade),
            options = listOf("all" to stringResource(R.string.acq_se_pack_all), "any" to stringResource(R.string.acq_se_pack_any)),
            selected = state.choice(SettingsSearchChoice.PACK_UPGRADE),
            onSelect = { vm.setChoice(SettingsSearchChoice.PACK_UPGRADE, it) },
        )
        PickerField(
            label = stringResource(R.string.acq_se_pack_prefer),
            options =
                listOf("rank" to stringResource(R.string.acq_se_prefer_rank), "always" to stringResource(R.string.acq_se_prefer_always)),
            selected = state.choice(SettingsSearchChoice.PACK_PREFER),
            onSelect = { vm.setChoice(SettingsSearchChoice.PACK_PREFER, it) },
            hint = stringResource(R.string.acq_se_pack_prefer_hint),
        )
        AdvancedToggle(advanced) { advanced = !advanced }
        if (advanced) {
            SearchFlag(SettingsSearchSwitch.MULTI_SEASON_PACKS, R.string.acq_se_multi, R.string.acq_se_multi_hint, state, vm)
        }
    }
}

@Composable
fun SearchMusicCard(
    state: SettingsSearchState,
    vm: SettingsSearchViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_se_music)) {
        SearchFlag(SettingsSearchSwitch.MISSING_MUSIC, R.string.acq_se_albums, R.string.acq_se_albums_hint, state, vm)
        SearchNumberField(SettingsSearchNumber.TRACK_TOLERANCE, R.string.acq_se_tolerance, R.string.acq_se_tolerance_hint, state, vm)
        AdvancedToggle(advanced) { advanced = !advanced }
        if (advanced) {
            SearchFlag(SettingsSearchSwitch.DISCOGRAPHY_PACKS, R.string.acq_se_discographies, null, state, vm)
            SearchNumberField(SettingsSearchNumber.EXTRA_FILES, R.string.acq_se_extra_files, R.string.acq_se_extra_files_hint, state, vm)
            SearchNumberField(SettingsSearchNumber.SEARCH_CAP, R.string.acq_se_search_cap, R.string.acq_se_search_cap_hint, state, vm)
        }
    }
}

@Composable
fun SearchSaveBar(
    state: SettingsSearchState,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding()) {
            state.saveError?.let { FailText(settingsErrorText(it)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.acq_se_unsaved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDiscard, enabled = !state.saving) { Text(stringResource(R.string.acq_se_discard)) }
                Button(onClick = onSave, enabled = state.canSave) { Text(stringResource(R.string.acq_se_save)) }
            }
        }
    }
}
