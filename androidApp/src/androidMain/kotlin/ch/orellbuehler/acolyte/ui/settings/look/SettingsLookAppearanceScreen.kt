package ch.orellbuehler.acolyte.ui.settings.look

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookAppearanceSaved
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookAppearanceState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.nav.DiscoverStatsRoute
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.text.Collator
import java.util.Locale

@Composable
fun SettingsLookAppearanceScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsLookAppearanceViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val language = stringResource(R.string.settings_look_appearance_language_saved)
    val autoplayOn = stringResource(R.string.settings_look_appearance_autoplay_on)
    val autoplayOff = stringResource(R.string.settings_look_appearance_autoplay_off)
    val artLibrary = stringResource(R.string.settings_look_appearance_art_library)
    val artTrending = stringResource(R.string.settings_look_appearance_art_trending)
    LaunchedEffect(state.savedCount) {
        val text =
            when (state.saved) {
                SettingsLookAppearanceSaved.LANGUAGE -> language
                SettingsLookAppearanceSaved.AUTOPLAY_ON -> autoplayOn
                SettingsLookAppearanceSaved.AUTOPLAY_OFF -> autoplayOff
                SettingsLookAppearanceSaved.ART_LIBRARY -> artLibrary
                SettingsLookAppearanceSaved.ART_TRENDING -> artTrending
                SettingsLookAppearanceSaved.NONE -> null
            }
        if (state.savedCount > 0 && text != null) snackbar.showSnackbar(text)
    }
    ScreenScaffold(title = stringResource(R.string.settings_page_appearance), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = Spacing.XLarge),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    item(key = "theme") {
                        SettingsCard(stringResource(R.string.settings_look_appearance_theme_title)) {
                            Hint(stringResource(R.string.settings_look_appearance_theme_text))
                        }
                    }
                    item(key = "language") { LanguageCard(state, viewModel::setLanguage) }
                    if (state.showEveryone) item(key = "everyone") { EveryoneCard(state, viewModel) }
                    if (state.canSeeStats) {
                        item(key = "stats") {
                            SettingsCard(stringResource(R.string.discover_stats_title)) {
                                Hint(stringResource(R.string.settings_look_appearance_stats_text))
                                OutlinedButton(onClick = { onOpen(DiscoverStatsRoute) }) {
                                    Text(stringResource(R.string.settings_look_appearance_stats_open))
                                }
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun EveryoneCard(
    state: SettingsLookAppearanceState,
    viewModel: SettingsLookAppearanceViewModel,
) {
    SettingsCard(
        stringResource(R.string.settings_look_appearance_everyone_title),
        subtitle = stringResource(R.string.settings_look_appearance_everyone_sub),
    ) {
        SwitchRow(
            stringResource(R.string.settings_look_appearance_autoplay),
            state.autoplayTrailers,
            viewModel::setAutoplayTrailers,
            hint = stringResource(R.string.settings_look_appearance_autoplay_hint),
            enabled = !state.everyoneSaving,
        )
        SwitchRow(
            stringResource(R.string.settings_look_appearance_art),
            state.signInArt,
            viewModel::setSignInArt,
            hint = stringResource(R.string.settings_look_appearance_art_hint),
            enabled = !state.everyoneSaving,
        )
        state.everyoneError?.let { LookErrorText(it, prefixed = true) }
    }
}

@Composable
private fun LanguageCard(
    state: SettingsLookAppearanceState,
    onPick: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val automatic = stringResource(R.string.settings_look_appearance_language_automatic)
    val current = if (state.language.isEmpty()) automatic else lookLanguageName(state.language)
    SettingsCard(
        stringResource(R.string.settings_look_appearance_language_title),
        subtitle = stringResource(R.string.settings_look_appearance_language_sub),
    ) {
        OutlinedButton(onClick = { open = true }, enabled = !state.languageSaving, modifier = Modifier.fillMaxWidth()) {
            Text(current, Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, contentDescription = null)
        }
        state.languageError?.let { LookErrorText(it, prefixed = true) }
    }
    if (open) {
        LanguageSheet(
            state = state,
            onPick = {
                open = false
                onPick(it)
            },
            onClose = { open = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(
    state: SettingsLookAppearanceState,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val automatic = stringResource(R.string.settings_look_appearance_language_automatic)
    val names =
        remember(state.mostUsedLanguages, state.otherLanguages) {
            (state.mostUsedLanguages + state.otherLanguages).associateWith { lookLanguageName(it) }
        }
    val others =
        remember(state.otherLanguages) {
            val collator = Collator.getInstance(Locale.getDefault())
            state.otherLanguages.sortedWith { a, b -> collator.compare(names.getValue(a), names.getValue(b)) }
        }
    val needle = query.trim().lowercase()
    val matches: (String) -> Boolean = { code ->
        needle.isEmpty() || code.contains(needle) || names.getValue(code).lowercase().contains(needle)
    }
    val showAutomatic = needle.isEmpty() || automatic.lowercase().contains(needle)
    val most = state.mostUsedLanguages.filter(matches)
    val more = others.filter(matches)
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(SHEET_HEIGHT)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.settings_look_appearance_language_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
            )
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                if (showAutomatic) {
                    item(key = "automatic") { LanguageRow(automatic, state.language.isEmpty()) { onPick("") } }
                }
                if (most.isNotEmpty()) {
                    item(key = "most") { SectionHeader(stringResource(R.string.settings_look_appearance_language_most_used)) }
                    items(most, key = { "most-$it" }) { code -> LanguageRow(names.getValue(code), code == state.language) { onPick(code) } }
                }
                if (more.isNotEmpty()) {
                    item(key = "more") { SectionHeader(stringResource(R.string.settings_look_appearance_language_more)) }
                    items(more, key = { "more-$it" }) { code -> LanguageRow(names.getValue(code), code == state.language) { onPick(code) } }
                }
                if (!showAutomatic && most.isEmpty() && more.isEmpty()) {
                    item(key = "none") {
                        Hint(stringResource(R.string.settings_look_appearance_language_none), Modifier.padding(Spacing.Screen))
                    }
                }
            }
        }
    }
}

private const val SHEET_HEIGHT = 0.85f

@Composable
private fun LanguageRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

internal fun lookLanguageName(code: String): String {
    val tag =
        when (code) {
            "zh-cn" -> "zh-Hans"
            "zh-tw" -> "zh-Hant"
            else -> code
        }
    val locale = Locale.forLanguageTag(tag)
    val name = locale.getDisplayName(locale)
    return if (name.isBlank() || name.equals(tag, ignoreCase = true)) code else name.replaceFirstChar { it.titlecase(locale) }
}
