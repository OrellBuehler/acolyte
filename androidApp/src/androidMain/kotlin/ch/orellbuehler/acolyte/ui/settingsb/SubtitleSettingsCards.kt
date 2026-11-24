package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.acquisition.SETTINGS_SUBTITLE_EVERY_LANGUAGE
import ch.orellbuehler.acolyte.feature.acquisition.SETTINGS_SUBTITLE_HEARING_TYPES
import ch.orellbuehler.acolyte.feature.acquisition.SETTINGS_SUBTITLE_MAX_LANGUAGES
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleFlag
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleNumber
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleState
import ch.orellbuehler.acolyte.feature.acquisition.SettingsSubtitleStep
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
internal fun SubtitleCheckRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Column(Modifier.weight(1f).padding(top = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Hint(hint)
        }
    }
}

@Composable
internal fun SubtitleTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
    placeholder: String? = null,
    hint: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = hint?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SubtitleNumberField(
    label: String,
    key: String,
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
    hint: String? = null,
) {
    val field = SettingsSubtitleNumber.of(key)
    val valid = state.numberValid(key)
    val supporting =
        if (!valid && field != null) stringResource(R.string.acq_sub_range, field.min, field.max) else hint
    SubtitleTextField(
        label = label,
        value = state.number(key),
        onChange = { viewModel.setNumber(key, it) },
        hint = supporting,
        isError = !valid,
        keyboardType = if (field?.whole == false) KeyboardType.Decimal else KeyboardType.Number,
    )
}

@Composable
internal fun SubtitleLanguagesCard(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var means by rememberSaveable { mutableStateOf(false) }
    val wants = state.draft.languages
    SettingsCard(stringResource(R.string.acq_sub_langs_title), subtitle = stringResource(R.string.acq_sub_langs_sub)) {
        if (wants.isEmpty()) Hint(stringResource(R.string.acq_sub_none_yet))
        wants.forEachIndexed { index, want ->
            SubtitleWantRow(state, viewModel, index, want.language, want.hearingImpaired, want.translateFrom, want.forced)
        }
        state.wishProblem?.let { FailText(subtitleProblemText(it)) }
        OutlinedButton(onClick = viewModel::addLanguage, enabled = state.canAddLanguage) {
            Text(stringResource(R.string.acq_sub_add_language))
        }
        if (wants.size >= SETTINGS_SUBTITLE_MAX_LANGUAGES) Hint(stringResource(R.string.acq_sub_problem_too_many))
        if (wants.isNotEmpty()) Hint(stringResource(R.string.acq_sub_hi_note))
        TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.acq_sub_advanced)) }
        if (advanced) {
            SubtitleNumberField(
                stringResource(R.string.acq_sub_pass_episode),
                SettingsSubtitleNumber.PASS_EPISODE.key,
                state,
                viewModel,
                stringResource(R.string.acq_sub_pass_hint),
            )
            SubtitleNumberField(
                stringResource(R.string.acq_sub_pass_movie),
                SettingsSubtitleNumber.PASS_MOVIE.key,
                state,
                viewModel,
                stringResource(R.string.acq_sub_pass_hint),
            )
            TextButton(onClick = { means = !means }) { Text(stringResource(R.string.acq_sub_pass_means)) }
            if (means) {
                Hint(stringResource(R.string.acq_sub_pass_text1))
                Hint(stringResource(R.string.acq_sub_pass_text2))
            }
        }
    }
}

@Composable
private fun SubtitleWantRow(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
    index: Int,
    language: String,
    type: String,
    translateFrom: String,
    forced: Boolean,
) {
    val pick = stringResource(R.string.acq_sub_lang_pick)
    val languageOptions =
        listOf("" to pick) + state.languages.map { it.code to it.name } +
            if (language.isNotEmpty() && state.languages.none { it.code == language }) listOf(language to language) else emptyList()
    val dontTranslate = stringResource(R.string.acq_sub_dont_translate)
    val fromOptions = listOf("" to dontTranslate) + state.languages.filter { it.code != language }.map { it.code to it.name }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    PickerField(
                        stringResource(R.string.acq_sub_lang_language),
                        languageOptions,
                        language,
                        { viewModel.setWantLanguage(index, it) },
                    )
                }
                IconButton(onClick = { viewModel.removeLanguage(index) }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.acq_sub_take_off, state.languageName(language)),
                    )
                }
            }
            PickerField(
                stringResource(R.string.acq_sub_lang_type),
                SETTINGS_SUBTITLE_HEARING_TYPES.map { it to subtitleTypeText(it) },
                type,
                { viewModel.setWantType(index, it) },
            )
            if (state.translatorReady) {
                PickerField(
                    stringResource(R.string.acq_sub_translate_from),
                    fromOptions,
                    translateFrom,
                    { viewModel.setWantTranslateFrom(index, it) },
                )
            }
            SubtitleCheckRow(stringResource(R.string.acq_sub_forced), forced, { viewModel.setWantForced(index, it) })
        }
    }
}

@Composable
internal fun SubtitleTranslatorCard(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
) {
    val form = state.translator
    val kinds =
        listOf(
            "" to stringResource(R.string.acq_sub_tr_off),
            "openai" to stringResource(R.string.acq_sub_tr_openai),
            "lingarr" to stringResource(R.string.acq_sub_tr_lingarr),
        )
    SettingsCard(stringResource(R.string.acq_sub_tr_title), subtitle = stringResource(R.string.acq_sub_tr_sub)) {
        PickerField(
            stringResource(R.string.acq_sub_tr_kind),
            kinds,
            form.kind,
            viewModel::setTranslatorKind,
            hint =
                when (form.kind) {
                    "openai" -> stringResource(R.string.acq_sub_tr_openai_hint)
                    "lingarr" -> stringResource(R.string.acq_sub_tr_lingarr_hint)
                    else -> null
                },
        )
        if (form.kind.isNotEmpty()) {
            SubtitleTextField(
                label = stringResource(R.string.acq_sub_tr_address),
                value = form.url,
                onChange = viewModel::setTranslatorUrl,
                placeholder = if (form.kind == "openai") "http://192.168.1.20:11434/v1" else "http://192.168.1.20:9876",
                keyboardType = KeyboardType.Uri,
            )
            if (form.kind == "openai") {
                SubtitleTextField(
                    label = stringResource(R.string.acq_sub_tr_model),
                    value = form.model,
                    onChange = viewModel::setTranslatorModel,
                )
                val models = state.modelSuggestions
                if (models.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        models.forEach { model ->
                            AssistChip(onClick = { viewModel.setTranslatorModel(model) }, label = { Text(model) })
                        }
                    }
                }
            }
            SubtitleTextField(
                label = stringResource(R.string.acq_sub_tr_key),
                value = form.key,
                onChange = viewModel::setTranslatorKey,
                secret = true,
                placeholder = if (form.hasKey) "••••••••" else stringResource(R.string.acq_sub_tr_key_empty),
                hint = stringResource(if (form.hasKey) R.string.acq_sub_tr_key_kept else R.string.acq_sub_tr_key_hint),
            )
            if (form.hasKey && form.key.isEmpty()) {
                SubtitleCheckRow(stringResource(R.string.acq_sub_tr_clear_key), form.clearKey, viewModel::setTranslatorClearKey)
            }
            SubtitleCheckRow(
                stringResource(R.string.acq_sub_tr_built_in),
                form.builtInAuto,
                viewModel::setTranslatorBuiltInAuto,
                hint = stringResource(R.string.acq_sub_tr_built_in_hint),
            )
        }
        val test = state.translatorTest
        if (test != null && !state.translatorDirty) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                StatusChip(
                    stringResource(if (test.ok) R.string.acq_sub_test_ok else R.string.acq_sub_test_failed),
                    if (test.ok) Tone.OK else Tone.FAIL,
                )
                if (test.message.isNotBlank()) {
                    Text(
                        test.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (test.ok) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        state.translatorError?.let { FailText(subtitleErrorText(it)) }
        if (form.kind.isNotEmpty() || state.translatorDirty) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Button(onClick = viewModel::saveTranslator, enabled = state.canSaveTranslator) {
                    Text(stringResource(R.string.acq_sub_save))
                }
                if (state.translatorSaved.kind.isNotEmpty()) {
                    OutlinedButton(onClick = viewModel::testTranslator, enabled = state.canTestTranslator) {
                        Text(stringResource(R.string.acq_sub_tr_test))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubtitleWhenCard(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_sub_when_title), subtitle = stringResource(R.string.acq_sub_when_sub)) {
        SwitchRow(
            stringResource(R.string.acq_sub_better),
            state.flag(SettingsSubtitleFlag.BETTER.key),
            { viewModel.setFlag(SettingsSubtitleFlag.BETTER.key, it) },
            hint = stringResource(R.string.acq_sub_better_hint),
        )
        TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.acq_sub_advanced)) }
        if (advanced) {
            SubtitleNumberField(stringResource(R.string.acq_sub_search_hours), SettingsSubtitleNumber.SEARCH_HOURS.key, state, viewModel)
            SubtitleNumberField(
                stringResource(R.string.acq_sub_batch),
                SettingsSubtitleNumber.BATCH.key,
                state,
                viewModel,
                stringResource(R.string.acq_sub_batch_hint),
            )
            SwitchRow(
                stringResource(R.string.acq_sub_slow_down),
                state.flag(SettingsSubtitleFlag.ADAPTIVE.key),
                { viewModel.setFlag(SettingsSubtitleFlag.ADAPTIVE.key, it) },
                hint = stringResource(R.string.acq_sub_slow_down_hint),
            )
            if (state.flag(SettingsSubtitleFlag.BETTER.key)) {
                SubtitleNumberField(stringResource(R.string.acq_sub_every_hours), SettingsSubtitleNumber.BETTER_HOURS.key, state, viewModel)
                SubtitleNumberField(stringResource(R.string.acq_sub_last_days), SettingsSubtitleNumber.BETTER_DAYS.key, state, viewModel)
            }
        }
    }
}

@Composable
internal fun SubtitleMoreOptions(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        TextButton(onClick = { open = !open }, modifier = Modifier.padding(horizontal = Spacing.Medium)) {
            Text(stringResource(R.string.acq_sub_more_options))
        }
        if (open) {
            SubtitleInsideCard(state, viewModel)
            SubtitleCleanupCard(state, viewModel)
            SubtitleSyncCard(state, viewModel)
        }
    }
}

@Composable
private fun SubtitleInsideCard(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val suffixes =
        listOf(
            "hi" to stringResource(R.string.acq_sub_suffix_hi),
            "cc" to stringResource(R.string.acq_sub_suffix_cc),
            "sdh" to stringResource(R.string.acq_sub_suffix_sdh),
        )
    SettingsCard(stringResource(R.string.acq_sub_inside_title)) {
        SwitchRow(
            stringResource(R.string.acq_sub_count_inside),
            state.flag(SettingsSubtitleFlag.USE_BUILT_IN.key),
            { viewModel.setFlag(SettingsSubtitleFlag.USE_BUILT_IN.key, it) },
            hint = stringResource(R.string.acq_sub_count_inside_hint),
        )
        TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.acq_sub_advanced)) }
        if (advanced) {
            SwitchRow(
                stringResource(R.string.acq_sub_no_picture),
                state.flag(SettingsSubtitleFlag.IGNORE_PICTURE_TRACKS.key),
                { viewModel.setFlag(SettingsSubtitleFlag.IGNORE_PICTURE_TRACKS.key, it) },
                hint = stringResource(R.string.acq_sub_no_picture_hint),
                enabled = state.flag(SettingsSubtitleFlag.USE_BUILT_IN.key),
            )
            PickerField(stringResource(R.string.acq_sub_hi_suffix), suffixes, state.draft.hiSuffix, viewModel::setHiSuffix)
        }
    }
}

@Composable
private fun SubtitleCleanupCard(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val every = stringResource(R.string.acq_sub_clean_every)
    val rows = state.cleanRows.map { code -> code to if (code == SETTINGS_SUBTITLE_EVERY_LANGUAGE) every else state.languageName(code) }
    val addable = state.cleanAddable.map { it.code to it.name }
    SettingsCard(stringResource(R.string.acq_sub_clean_title), subtitle = stringResource(R.string.acq_sub_clean_sub)) {
        PickerField(stringResource(R.string.acq_sub_clean_row), rows, state.cleanLanguage, viewModel::pickCleanLanguage)
        if (addable.isNotEmpty()) {
            val addLabel = stringResource(R.string.acq_sub_clean_add_lang)
            PickerField(addLabel, listOf("" to addLabel) + addable, "", { if (it.isNotEmpty()) viewModel.pickCleanLanguage(it) })
        }
        if (state.cleanLanguage != SETTINGS_SUBTITLE_EVERY_LANGUAGE) {
            TextButton(onClick = viewModel::removeCleanLanguage) { Text(stringResource(R.string.acq_sub_clean_remove_lang)) }
        }
        SwitchRow(
            stringResource(R.string.acq_sub_clean_ads),
            state.step(SettingsSubtitleStep.ADS.key),
            { viewModel.setStep(SettingsSubtitleStep.ADS.key, it) },
            hint = stringResource(R.string.acq_sub_clean_ads_hint),
        )
        SwitchRow(
            stringResource(R.string.acq_sub_clean_hi),
            state.step(SettingsSubtitleStep.HI.key),
            { viewModel.setStep(SettingsSubtitleStep.HI.key, it) },
            hint = stringResource(R.string.acq_sub_clean_hi_hint),
        )
        TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.acq_sub_advanced)) }
        if (advanced) {
            CleanStep(state, viewModel, SettingsSubtitleStep.TAGS, R.string.acq_sub_clean_tags, null)
            CleanStep(state, viewModel, SettingsSubtitleStep.EMOJI, R.string.acq_sub_clean_emoji, null)
            CleanStep(state, viewModel, SettingsSubtitleStep.OCR, R.string.acq_sub_clean_ocr, R.string.acq_sub_clean_ocr_hint)
            CleanStep(state, viewModel, SettingsSubtitleStep.CAPS, R.string.acq_sub_clean_caps, null)
            CleanStep(state, viewModel, SettingsSubtitleStep.SPACING, R.string.acq_sub_clean_spacing, null)
            CleanStep(state, viewModel, SettingsSubtitleStep.OVERLAP, R.string.acq_sub_clean_overlap, R.string.acq_sub_clean_overlap_hint)
        }
        HorizontalDivider()
        Hint(stringResource(R.string.acq_sub_clean_note))
        if (state.canClean) {
            OutlinedButton(onClick = viewModel::cleanAll, enabled = !state.cleaning) { Text(stringResource(R.string.acq_sub_clean_all)) }
        }
    }
}

@Composable
private fun CleanStep(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
    step: SettingsSubtitleStep,
    title: Int,
    hint: Int?,
) {
    SwitchRow(
        stringResource(title),
        state.step(step.key),
        { viewModel.setStep(step.key, it) },
        hint = hint?.let { stringResource(it) },
    )
}

@Composable
private fun SubtitleSyncCard(
    state: SettingsSubtitleState,
    viewModel: SubtitleSettingsViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.acq_sub_sync_title), subtitle = stringResource(R.string.acq_sub_sync_sub)) {
        Hint(stringResource(R.string.acq_sub_sync_note))
        TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.acq_sub_advanced)) }
        if (advanced) {
            SubtitleNumberField(stringResource(R.string.acq_sub_shift), SettingsSubtitleNumber.SYNC_STEP_MS.key, state, viewModel)
            SubtitleNumberField(stringResource(R.string.acq_sub_fps_from), SettingsSubtitleNumber.SYNC_FROM_FPS.key, state, viewModel)
            SubtitleNumberField(stringResource(R.string.acq_sub_fps_to), SettingsSubtitleNumber.SYNC_TO_FPS.key, state, viewModel)
        }
    }
}
