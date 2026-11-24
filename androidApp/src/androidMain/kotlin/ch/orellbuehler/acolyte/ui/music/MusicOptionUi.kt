package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicOption
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.theme.Spacing

enum class MusicOptionGroup(
    val key: String,
) {
    CHOICE("choice"),
    NEW("new"),
    TYPE("type"),
    EXTRA("extra"),
    STATUS("status"),
    RULE("rule"),
}

private val labels: Map<String, Int> =
    mapOf(
        "choice_all" to R.string.music_option_choice_all,
        "choice_future" to R.string.music_option_choice_future,
        "choice_missing" to R.string.music_option_choice_missing,
        "choice_latest" to R.string.music_option_choice_latest,
        "choice_first" to R.string.music_option_choice_first,
        "choice_none" to R.string.music_option_choice_none,
        "new_all" to R.string.music_option_new_all,
        "new_new" to R.string.music_option_new_new,
        "new_none" to R.string.music_option_new_none,
        "type_album" to R.string.music_option_type_album,
        "type_ep" to R.string.music_option_type_ep,
        "type_single" to R.string.music_option_type_single,
        "type_broadcast" to R.string.music_option_type_broadcast,
        "type_other" to R.string.music_option_type_other,
        "extra_studio" to R.string.music_option_extra_studio,
        "extra_live" to R.string.music_option_extra_live,
        "extra_compilation" to R.string.music_option_extra_compilation,
        "extra_soundtrack" to R.string.music_option_extra_soundtrack,
        "extra_remix" to R.string.music_option_extra_remix,
        "extra_spokenword" to R.string.music_option_extra_spokenword,
        "extra_interview" to R.string.music_option_extra_interview,
        "extra_audiobook" to R.string.music_option_extra_audiobook,
        "extra_audio_drama" to R.string.music_option_extra_audio_drama,
        "extra_dj_mix" to R.string.music_option_extra_dj_mix,
        "extra_mixtape" to R.string.music_option_extra_mixtape,
        "extra_demo" to R.string.music_option_extra_demo,
        "extra_field_recording" to R.string.music_option_extra_field_recording,
        "status_official" to R.string.music_option_status_official,
        "status_promotion" to R.string.music_option_status_promotion,
        "status_bootleg" to R.string.music_option_status_bootleg,
        "status_pseudo_release" to R.string.music_option_status_pseudo_release,
        "rule_common" to R.string.music_option_rule_common,
        "rule_most_tracks" to R.string.music_option_rule_most_tracks,
        "rule_earliest" to R.string.music_option_rule_earliest,
    )

private val hints: Map<String, Int> =
    mapOf(
        "choice_all" to R.string.music_option_choice_all_hint,
        "choice_future" to R.string.music_option_choice_future_hint,
        "choice_missing" to R.string.music_option_choice_missing_hint,
        "choice_latest" to R.string.music_option_choice_latest_hint,
        "choice_first" to R.string.music_option_choice_first_hint,
        "choice_none" to R.string.music_option_choice_none_hint,
        "rule_common" to R.string.music_option_rule_common_hint,
        "rule_most_tracks" to R.string.music_option_rule_most_tracks_hint,
        "rule_earliest" to R.string.music_option_rule_earliest_hint,
    )

@Composable
fun musicOptionLabel(
    group: MusicOptionGroup,
    option: MusicOption,
): String = labels["${group.key}_${option.key}"]?.let { stringResource(it) } ?: option.id

@Composable
fun musicOptionHint(
    group: MusicOptionGroup,
    id: String,
    options: List<MusicOption>,
): String? {
    val key = options.firstOrNull { it.id == id }?.key ?: return null
    return hints["${group.key}_$key"]?.let { stringResource(it) }
}

@Composable
fun MusicOptionSelect(
    label: String,
    group: MusicOptionGroup,
    options: List<MusicOption>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
) {
    val items = options.map { it.id to musicOptionLabel(group, it) }
    val own = musicOptionHint(group, selected, options)
    PickerField(
        label = label,
        options = items,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        enabled = enabled,
        hint = listOfNotNull(own, hint).joinToString(" ").ifEmpty { null },
    )
}

@Composable
fun MusicOptionChecks(
    title: String,
    group: MusicOptionGroup,
    options: List<MusicOption>,
    selected: List<String>,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (hint != null) Hint(hint)
        options.forEach { option ->
            val on = option.id in selected
            Row(
                Modifier.fillMaxWidth().clickable(enabled = enabled) { onToggle(option.id, !on) }.padding(vertical = Spacing.XSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = on, onCheckedChange = { onToggle(option.id, it) }, enabled = enabled)
                Text(musicOptionLabel(group, option), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
