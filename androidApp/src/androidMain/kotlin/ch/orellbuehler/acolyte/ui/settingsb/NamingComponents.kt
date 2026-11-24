package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingChoice
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingNumberError
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingState
import ch.orellbuehler.acolyte.feature.acquisition.SettingsNamingText
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun settingsErrorText(error: ApiError): String =
    if (error.message.isNotBlank() && error.code in SERVER_TEXT_CODES) error.message else errorText(error)

private val SERVER_TEXT_CODES = setOf(ApiErrorCode.BAD_REQUEST, ApiErrorCode.CONFLICT, ApiErrorCode.UNKNOWN)

@Composable
fun SettingsNotAllowed(
    title: String,
    text: String,
) {
    MessageView(icon = Icons.Rounded.Lock, title = title, message = text)
}

@Stable
class NamingEditor {
    var focused by mutableStateOf<SettingsNamingText?>(null)
    var last by mutableStateOf<SettingsNamingText?>(null)
    var cursor by mutableStateOf<Pair<SettingsNamingText, Int>?>(null)
    var sheet by mutableStateOf<SettingsNamingText?>(null)
    val selections = mutableMapOf<SettingsNamingText, TextRange>()

    fun target(
        card: List<SettingsNamingText>,
        fallback: SettingsNamingText,
    ): SettingsNamingText = last?.takeIf { it in card && it.insertable } ?: fallback
}

@Composable
fun namingFieldLabel(field: SettingsNamingText): String =
    stringResource(
        when (field) {
            SettingsNamingText.FILE_PATTERN -> R.string.acq_nm_file_pattern
            SettingsNamingText.FOLDER_PATTERN -> R.string.acq_nm_folder_pattern
            SettingsNamingText.SHOW_FOLDER_PATTERN -> R.string.acq_nm_show_folder
            SettingsNamingText.SEASON_FOLDER_PATTERN -> R.string.acq_nm_season_pattern
            SettingsNamingText.SPECIALS_FOLDER -> R.string.acq_nm_specials
            SettingsNamingText.EPISODE_PATTERN -> R.string.acq_nm_episode_pattern
            SettingsNamingText.DAILY_PATTERN -> R.string.acq_nm_daily
            SettingsNamingText.ANIME_PATTERN -> R.string.acq_nm_anime
            SettingsNamingText.ARTIST_FOLDER_PATTERN -> R.string.acq_nm_artist_folder
            SettingsNamingText.ALBUM_FOLDER_PATTERN -> R.string.acq_nm_album_folder
            SettingsNamingText.TRACK_PATTERN -> R.string.acq_nm_track_pattern
            SettingsNamingText.MULTI_DISC_TRACK_PATTERN -> R.string.acq_nm_multi_disc
            SettingsNamingText.DISC_FOLDER_PATTERN -> R.string.acq_nm_disc_pattern
        },
    )

@Composable
fun namingChoiceLabel(
    choice: SettingsNamingChoice,
    value: String,
): String =
    stringResource(
        when (choice) {
            SettingsNamingChoice.COLON, SettingsNamingChoice.SHOW_COLON, SettingsNamingChoice.MUSIC_COLON ->
                when (value) {
                    "dash" -> R.string.acq_nm_colon_dash
                    "space_dash" -> R.string.acq_nm_colon_space_dash
                    "space_dash_space" -> R.string.acq_nm_colon_space_dash_space
                    "remove" -> R.string.acq_nm_colon_remove
                    else -> R.string.acq_nm_colon_smart
                }
            SettingsNamingChoice.ILLEGAL, SettingsNamingChoice.SHOW_ILLEGAL, SettingsNamingChoice.MUSIC_ILLEGAL ->
                if (value == "remove") R.string.acq_nm_illegal_remove else R.string.acq_nm_illegal_replace
            SettingsNamingChoice.MULTI_EPISODE ->
                when (value) {
                    "short_range" -> R.string.acq_nm_multi_short_range
                    "each" -> R.string.acq_nm_multi_each
                    "each_dash" -> R.string.acq_nm_multi_each_dash
                    else -> R.string.acq_nm_multi_range
                }
            SettingsNamingChoice.AIR_DATE ->
                when (value) {
                    "dot" -> R.string.acq_nm_date_dot
                    "space" -> R.string.acq_nm_date_space
                    else -> R.string.acq_nm_date_dash
                }
            SettingsNamingChoice.ABSOLUTE_DIGITS ->
                when (value) {
                    "2" -> R.string.acq_nm_digits_2
                    "4" -> R.string.acq_nm_digits_4
                    else -> R.string.acq_nm_digits_3
                }
            SettingsNamingChoice.TITLE_LANGUAGE -> R.string.acq_nm_title_language_default
        },
    )

@Composable
fun namingExampleLabel(key: String): String =
    stringResource(
        when (key) {
            "show_folder" -> R.string.acq_nm_ex_show_folder
            "episode" -> R.string.acq_nm_ex_episode
            "multi" -> R.string.acq_nm_ex_multi
            "special" -> R.string.acq_nm_ex_special
            "daily" -> R.string.acq_nm_ex_daily
            "anime" -> R.string.acq_nm_ex_anime
            "artist_folder" -> R.string.acq_nm_ex_artist_folder
            "album_folder" -> R.string.acq_nm_ex_album_folder
            "track" -> R.string.acq_nm_ex_track
            "multi_disc" -> R.string.acq_nm_ex_multi_disc
            else -> R.string.acq_nm_ex_various
        },
    )

@Composable
fun namingNumberError(error: SettingsNamingNumberError): String =
    stringResource(
        when (error) {
            SettingsNamingNumberError.DAYS -> R.string.acq_nm_err_days
            SettingsNamingNumberError.DAYS_MAX -> R.string.acq_nm_err_days_max
            SettingsNamingNumberError.FREE -> R.string.acq_nm_err_free
            SettingsNamingNumberError.FREE_MAX -> R.string.acq_nm_err_free_max
            SettingsNamingNumberError.RANGE -> R.string.acq_nm_err_range
        },
    )

@Composable
fun NamingNumberField(
    label: String,
    hint: String,
    field: SettingsNamingNumber,
    state: SettingsNamingState,
    onChange: (String) -> Unit,
) {
    val error = state.numberError(field)
    OutlinedTextField(
        value = state.number(field),
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text(if (error != null) namingNumberError(error) else hint) },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun NamingPatternField(
    field: SettingsNamingText,
    state: SettingsNamingState,
    editor: NamingEditor,
    label: String,
    hint: String?,
    onChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    val value = state.text(field)
    val cursor = editor.cursor?.takeIf { it.first == field }?.second
    var field0 by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value, cursor) {
        if (field0.text != value) field0 = TextFieldValue(value, TextRange((cursor ?: value.length).coerceIn(0, value.length)))
    }
    OutlinedTextField(
        value = field0,
        onValueChange = { next ->
            field0 = next
            editor.selections[field] = next.selection
            if (next.text != value) onChange(next.text)
        },
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        enabled = enabled,
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        modifier =
            Modifier.fillMaxWidth().onFocusChanged {
                if (it.isFocused) {
                    editor.focused = field
                    editor.last = field
                } else if (editor.focused == field) {
                    editor.focused = null
                }
            },
    )
}

@Composable
fun NamingExampleBox(
    state: SettingsNamingState,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(
                    stringResource(R.string.acq_nm_example),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.exampleLoading) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
            }
            val error = state.exampleError
            if (error != null) {
                FailText(settingsErrorText(error))
            } else {
                Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            }
        }
    }
}

@Composable
fun NamingExampleList(
    state: SettingsNamingState,
    rows: List<Pair<String, String>>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(
            stringResource(R.string.acq_nm_examples),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.exampleError != null) {
            Hint(stringResource(R.string.acq_nm_see_error))
        } else {
            rows.forEach { (label, path) ->
                Column {
                    Hint(label)
                    Text(path, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamingWordsSheet(
    state: SettingsNamingState,
    editor: NamingEditor,
    onInsert: (SettingsNamingText, String, Int, Int) -> Int,
) {
    val target = editor.sheet ?: return
    var query by remember(target) { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = { editor.sheet = null }) {
        Column(
            Modifier.padding(horizontal = Spacing.Screen).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(stringResource(R.string.acq_nm_words), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.acq_nm_words_for, namingFieldLabel(target)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(state.text(target), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            val error = state.exampleError
            if (error != null) {
                FailText(settingsErrorText(error))
            } else {
                Hint(state.exampleFor(target))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.acq_nm_words_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val words = state.words(target, query)
            if (words.isEmpty()) {
                Hint(stringResource(R.string.acq_nm_words_none))
            } else {
                Hint(stringResource(R.string.acq_nm_words_hint))
            }
            LazyColumn(contentPadding = PaddingValues(bottom = Spacing.Large), modifier = Modifier.weight(1f, fill = false)) {
                items(words, key = { it.key }) { word ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val range = editor.selections[target] ?: TextRange(state.text(target).length)
                                val at = onInsert(target, word.key, range.min, range.max)
                                editor.selections[target] = TextRange(at)
                                editor.cursor = target to at
                            }.padding(vertical = Spacing.Small),
                    ) {
                        Text(
                            "{${word.key}}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Hint(word.about)
                    }
                    HorizontalDivider()
                }
            }
            TextButton(onClick = { editor.sheet = null }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.acq_nm_words_close))
            }
        }
    }
}

@Composable
fun NamingSaveBar(
    state: SettingsNamingState,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding()) {
            state.saveError?.let { FailText(settingsErrorText(it)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.acq_nm_unsaved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDiscard, enabled = !state.saving) { Text(stringResource(R.string.acq_nm_discard)) }
                Button(onClick = onSave, enabled = state.canSave) { Text(stringResource(R.string.acq_nm_save)) }
            }
        }
    }
}
