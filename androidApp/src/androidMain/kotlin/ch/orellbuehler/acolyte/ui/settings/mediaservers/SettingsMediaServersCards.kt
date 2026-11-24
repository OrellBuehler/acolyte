package ch.orellbuehler.acolyte.ui.settings.mediaservers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersState
import ch.orellbuehler.acolyte.feature.settings.mediaservers.SettingsMediaServersTrackRow
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

private const val MAX_LABEL = 64

@Composable
internal fun WatchHistoryCard(
    state: SettingsMediaServersState,
    onChange: (Boolean) -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_mediaservers_watch_title)) {
        SwitchRow(
            stringResource(R.string.settings_mediaservers_watch_read),
            state.watchOn,
            onChange,
            hint = stringResource(R.string.settings_mediaservers_watch_hint),
            enabled = !state.watchBusy,
        )
    }
}

@Composable
internal fun PlexLabelsCard(
    state: SettingsMediaServersState,
    viewModel: SettingsMediaServersViewModel,
) {
    val draft = state.labelDraft
    SettingsCard(stringResource(R.string.settings_mediaservers_labels_title)) {
        SwitchRow(
            stringResource(R.string.settings_mediaservers_labels_switch),
            draft.enabled,
            viewModel::setLabelsEnabled,
            hint = stringResource(R.string.settings_mediaservers_labels_hint),
        )
        if (draft.enabled) {
            MediaServerField(
                value = draft.label,
                revision = state.cardsRevision,
                onChange = viewModel::setLabelText,
                label = stringResource(R.string.settings_mediaservers_labels_text),
                maxLength = MAX_LABEL,
            )
            MediaServerCheckRow(draft.askers, viewModel::setLabelAskers, stringResource(R.string.settings_mediaservers_labels_askers))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Button(onClick = viewModel::saveLabels, enabled = state.labelsCanSave) {
                Text(stringResource(R.string.settings_mediaservers_save))
            }
            if (state.labels?.enabled == true) {
                OutlinedButton(onClick = viewModel::runLabels, enabled = state.labelsCanRun) {
                    Text(stringResource(R.string.settings_mediaservers_labels_existing))
                }
            }
        }
    }
}

@Composable
internal fun DefaultTracksCard(
    state: SettingsMediaServersState,
    viewModel: SettingsMediaServersViewModel,
) {
    SettingsCard(stringResource(R.string.settings_mediaservers_tracks_title)) {
        SwitchRow(
            stringResource(R.string.settings_mediaservers_tracks_switch),
            state.trackDraft.enabled,
            viewModel::setTracksEnabled,
            hint = stringResource(R.string.settings_mediaservers_tracks_hint),
        )
        if (state.trackDraft.enabled) {
            TrackList(stringResource(R.string.settings_mediaservers_tracks_audio), state.audioRows, state.canAddAudio, false, viewModel)
            TrackList(stringResource(R.string.settings_mediaservers_tracks_subs), state.subtitleRows, state.canAddSubtitle, true, viewModel)
            Hint(stringResource(R.string.settings_mediaservers_tracks_server_hint))
        }
        Button(onClick = viewModel::saveTracks, enabled = state.tracksCanSave) {
            Text(stringResource(R.string.settings_mediaservers_save))
        }
    }
}

@Composable
private fun TrackList(
    title: String,
    rows: List<SettingsMediaServersTrackRow>,
    canAdd: Boolean,
    subtitles: Boolean,
    viewModel: SettingsMediaServersViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        rows.forEach { row ->
            val name = mediaServerLanguageName(row.code, row.name)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${row.index + 1}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
                PickerField(
                    label = stringResource(R.string.settings_mediaservers_tracks_pick, row.index + 1),
                    options = row.options.map { it.code to mediaServerLanguageName(it.code, it.name) },
                    selected = row.code,
                    onSelect = { viewModel.setTrackLanguage(subtitles, row.index, it) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.moveTrackLanguage(subtitles, row.index, true) }, enabled = row.canUp) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.settings_mediaservers_tracks_up, name))
                }
                IconButton(onClick = { viewModel.moveTrackLanguage(subtitles, row.index, false) }, enabled = row.canDown) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.settings_mediaservers_tracks_down, name))
                }
                IconButton(onClick = { viewModel.removeTrackLanguage(subtitles, row.index) }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.settings_mediaservers_tracks_remove, name))
                }
            }
        }
        if (canAdd) {
            OutlinedButton(onClick = { viewModel.addTrackLanguage(subtitles) }) {
                Text(stringResource(R.string.settings_mediaservers_tracks_add))
            }
        }
    }
}
