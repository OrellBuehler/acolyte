package ch.orellbuehler.acolyte.ui.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.music.MusicOptions
import ch.orellbuehler.acolyte.feature.music.MusicSettingsPresenter
import ch.orellbuehler.acolyte.feature.music.MusicSettingsState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun MusicSettingsScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val vm = viewModel(key = "music-settings") { PresenterViewModel(sdk.musicSettings()) { load() } }
    val state by vm.presenter.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val saved = stringResource(R.string.music_note_settings_saved)
    LaunchedEffect(state.justSaved) { if (state.justSaved) snackbar.showSnackbar(saved) }
    val bar = state.canChange && state.dirty
    ScreenScaffold(title = stringResource(R.string.music_settings_title), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(status = state.status, isEmpty = false, onRefresh = { vm.run { load() } }) {
                SettingsForm(state, vm.presenter)
            }
            if (bar) SettingsSaveBar(state, vm, Modifier.align(Alignment.BottomCenter))
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (bar) 96.dp else 0.dp))
        }
    }
}

@Composable
private fun SettingsForm(
    state: MusicSettingsState,
    presenter: MusicSettingsPresenter,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var brainz by rememberSaveable { mutableStateOf(false) }
    val enabled = state.canChange
    val form = state.form
    LazyColumn(contentPadding = PaddingValues(bottom = 160.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        if (!enabled) {
            item(key = "cant") { NoticeBanner(stringResource(R.string.music_settings_cant)) }
        }
        item(key = "artist") {
            SettingsCard(
                stringResource(R.string.music_settings_new_artist),
                subtitle = stringResource(R.string.music_settings_new_artist_hint),
            ) {
                MusicOptionSelect(
                    label = stringResource(R.string.music_settings_choice),
                    group = MusicOptionGroup.CHOICE,
                    options = MusicOptions.albumChoices,
                    selected = form.albumChoice,
                    onSelect = presenter::setAlbumChoice,
                    enabled = enabled,
                )
                MusicOptionSelect(
                    label = stringResource(R.string.music_settings_new),
                    group = MusicOptionGroup.NEW,
                    options = MusicOptions.newAlbums,
                    selected = form.newAlbums,
                    onSelect = presenter::setNewAlbums,
                    enabled = enabled,
                    hint = stringResource(R.string.music_settings_new_hint),
                )
                MusicOptionChecks(
                    title = stringResource(R.string.music_settings_kinds),
                    group = MusicOptionGroup.TYPE,
                    options = MusicOptions.albumTypes,
                    selected = form.albumTypes,
                    onToggle = presenter::toggleAlbumType,
                    hint = stringResource(R.string.music_settings_kinds_hint),
                    enabled = enabled,
                )
                AdvancedToggle(advanced) { advanced = !advanced }
                if (advanced) {
                    MusicOptionChecks(
                        title = stringResource(R.string.music_settings_extra),
                        group = MusicOptionGroup.EXTRA,
                        options = MusicOptions.secondaryTypes,
                        selected = form.secondaryTypes,
                        onToggle = presenter::toggleSecondary,
                        hint = stringResource(R.string.music_settings_extra_hint),
                        enabled = enabled,
                    )
                    MusicOptionChecks(
                        title = stringResource(R.string.music_settings_statuses),
                        group = MusicOptionGroup.STATUS,
                        options = MusicOptions.statuses,
                        selected = form.releaseStatuses,
                        onToggle = presenter::toggleStatus,
                        enabled = enabled,
                    )
                    MusicOptionSelect(
                        label = stringResource(R.string.music_settings_rule),
                        group = MusicOptionGroup.RULE,
                        options = MusicOptions.pressingRules,
                        selected = form.pressingRule,
                        onSelect = presenter::setPressingRule,
                        enabled = enabled,
                        hint = stringResource(R.string.music_settings_rule_hint),
                    )
                }
            }
        }
        item(key = "brainz") {
            SettingsCard(
                stringResource(R.string.music_settings_brainz),
                subtitle = stringResource(R.string.music_settings_brainz_hint),
            ) {
                AdvancedToggle(brainz) { brainz = !brainz }
                if (brainz) {
                    OutlinedTextField(
                        value = form.musicbrainzUrl,
                        onValueChange = presenter::setUrl,
                        label = { Text(stringResource(R.string.music_settings_url)) },
                        supportingText = { Text(stringResource(R.string.music_settings_url_hint)) },
                        singleLine = true,
                        enabled = enabled,
                        placeholder = { Text("https://musicbrainz.org") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.hours,
                        onValueChange = presenter::setHours,
                        label = { Text(stringResource(R.string.music_settings_hours)) },
                        isError = !state.hoursValid,
                        singleLine = true,
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedToggle(
    open: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(stringResource(R.string.music_settings_advanced))
        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun SettingsSaveBar(
    state: MusicSettingsState,
    vm: PresenterViewModel<MusicSettingsPresenter>,
    modifier: Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding()) {
            state.error?.let { FailText(errorText(it)) }
            if (state.emptyKinds) FailText(stringResource(R.string.music_settings_need_kinds))
            if (state.emptyExtra) FailText(stringResource(R.string.music_settings_need_extra))
            if (state.emptyStatuses) FailText(stringResource(R.string.music_settings_need_statuses))
            if (!state.hoursValid) FailText(stringResource(R.string.music_settings_hours_invalid))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { vm.presenter.discard() }) { Text(stringResource(R.string.requests_discard)) }
                Button(onClick = { vm.run { save() } }, enabled = state.canSave) { Text(stringResource(R.string.requests_save)) }
            }
        }
    }
}
