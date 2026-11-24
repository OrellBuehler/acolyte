package ch.orellbuehler.acolyte.ui.settings.look

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.SettingsLookSource
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookKeyField
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookKeyResult
import ch.orellbuehler.acolyte.feature.settings.look.SettingsLookMetadataState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun SettingsLookMetadataScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { SettingsLookMetadataViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val modeSaved = stringResource(R.string.settings_look_metadata_mode_saved)
    val keysSaved = stringResource(R.string.settings_look_metadata_keys_saved)
    LaunchedEffect(state.modeSavedCount) { if (state.modeSavedCount > 0) snackbar.showSnackbar(modeSaved) }
    LaunchedEffect(state.keysSavedCount) { if (state.keysSavedCount > 0) snackbar.showSnackbar(keysSaved) }
    val editing = state.canEdit && state.dirty
    LookLeaveGuard(editing, onBack) { back ->
        ScreenScaffold(title = stringResource(R.string.settings_page_metadata), onBack = back) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                StatusContent(
                    status = state.status,
                    isEmpty = !state.allowed,
                    onRefresh = viewModel::refresh,
                    empty = {
                        EmptyView(
                            title = stringResource(R.string.settings_look_not_allowed),
                            message = stringResource(R.string.settings_look_not_allowed_text),
                            icon = Icons.Rounded.Lock,
                        )
                    },
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        item(key = "mode") { ModeCard(state, viewModel) }
                        if (state.tvdbClosed) item(key = "tvdb") { TvdbCard(viewModel) }
                        if (state.shownKeys.isNotEmpty()) item(key = "keys") { KeysCard(state, viewModel) }
                        if (state.offSources.isNotEmpty()) item(key = "off") { OffCard(state) }
                        if (state.fullMode) {
                            item(key = "server") { ServerCard(state, viewModel) }
                            item(key = "sent") { SentCard() }
                        }
                        item(key = "never") { NeverCard() }
                        if (state.hosts.isNotEmpty()) item(key = "hosts") { HostsCard(state) }
                    }
                }
                if (editing) {
                    LookSaveBar(
                        canSave = state.canSave,
                        saving = state.saving,
                        onDiscard = viewModel::discard,
                        onSave = viewModel::save,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        error = state.saveError,
                    )
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (editing) 96.dp else 0.dp))
            }
        }
    }
}

@Composable
private fun ModeCard(
    state: SettingsLookMetadataState,
    viewModel: SettingsLookMetadataViewModel,
) {
    SettingsCard(stringResource(R.string.settings_look_metadata_mode_title)) {
        if (state.dockerValue.isNotEmpty()) {
            FailText(stringResource(R.string.settings_look_metadata_docker_value, state.dockerValue))
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            ModeOption(
                stringResource(R.string.settings_look_metadata_mode_full),
                stringResource(R.string.settings_look_metadata_mode_full_about),
                state.mode == "full",
                state.canChooseMode,
            ) { viewModel.chooseMode("full") }
            ModeOption(
                stringResource(R.string.settings_look_metadata_mode_own),
                stringResource(R.string.settings_look_metadata_mode_own_about),
                state.mode == "own",
                state.canChooseMode,
            ) { viewModel.chooseMode("own") }
            ModeOption(
                stringResource(R.string.settings_look_metadata_mode_off),
                stringResource(R.string.settings_look_metadata_mode_off_about),
                state.mode == "off",
                state.canChooseMode,
            ) { viewModel.chooseMode("off") }
        }
        if (state.locked) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Icon(
                    Icons.Rounded.Key,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Hint(stringResource(R.string.settings_look_metadata_locked))
            }
        }
        if (state.ownMode) Hint(stringResource(R.string.settings_look_metadata_own_note))
        state.modeError?.let { LookErrorText(it) }
    }
}

@Composable
private fun ModeOption(
    name: String,
    about: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(Radius.Button),
        border = BorderStroke(1.dp, border),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Hint(about)
            }
        }
    }
}

@Composable
private fun TvdbCard(viewModel: SettingsLookMetadataViewModel) {
    SettingsCard(
        stringResource(R.string.settings_look_metadata_tvdb_title),
        subtitle = stringResource(R.string.settings_look_metadata_tvdb_desc),
    ) {
        OutlinedButton(onClick = viewModel::openTvdb) { Text(stringResource(R.string.settings_look_metadata_tvdb_use)) }
    }
}

@Composable
private fun KeysCard(
    state: SettingsLookMetadataState,
    viewModel: SettingsLookMetadataViewModel,
) {
    SettingsCard(
        stringResource(R.string.settings_look_metadata_keys_title),
        subtitle = if (state.ownMode) stringResource(R.string.settings_look_metadata_keys_desc) else null,
    ) {
        state.shownKeys.forEach { field -> KeyRow(field, state.ownMode, viewModel) }
        val kept = if (state.ownMode) R.string.settings_look_metadata_keys_kept_own else R.string.settings_look_metadata_keys_kept
        Hint(stringResource(kept))
    }
}

@Composable
private fun KeyRow(
    field: SettingsLookKeyField,
    ownMode: Boolean,
    viewModel: SettingsLookMetadataViewModel,
) {
    val uri = LocalUriHandler.current
    val status =
        when {
            field.fromDocker -> null
            field.removing -> stringResource(R.string.settings_look_metadata_key_removing)
            field.stored -> stringResource(R.string.settings_look_metadata_key_stored)
            else -> null
        }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        OutlinedTextField(
            value = field.shownText,
            onValueChange = { viewModel.setKey(field.name, it) },
            label = { Text(stringResource(keyLabel(field.name))) },
            supportingText = status?.let { { Text(it) } },
            enabled = !field.fromDocker,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Hint(stringResource(keyHint(field, ownMode)))
        if (!field.fromDocker) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                if (!field.isPin) {
                    OutlinedButton(onClick = { viewModel.testKey(field.name) }, enabled = !field.testing) {
                        val label = if (field.testing) R.string.settings_look_metadata_testing else R.string.settings_look_metadata_test
                        Text(stringResource(label))
                    }
                }
                if (field.removing) {
                    TextButton(onClick = { viewModel.restoreKey(field.name) }) {
                        Text(stringResource(R.string.settings_look_metadata_key_undo))
                    }
                } else if (field.saved) {
                    TextButton(onClick = { viewModel.clearKey(field.name) }) {
                        Text(stringResource(R.string.settings_look_metadata_key_remove))
                    }
                }
                if (field.link.isNotEmpty()) {
                    TextButton(onClick = { runCatching { uri.openUri(field.link) } }) {
                        Text(stringResource(R.string.settings_look_metadata_get_key))
                    }
                }
            }
        }
        field.result?.let { TestLine(it) }
    }
}

@Composable
private fun TestLine(result: SettingsLookKeyResult) {
    val error = result.error
    val text =
        when {
            error != null -> errorText(error)
            result.code == "works" -> stringResource(R.string.settings_look_metadata_test_works)
            result.code == "no_key" -> stringResource(R.string.settings_look_metadata_test_no_key)
            result.code == "refused" -> stringResource(R.string.settings_look_metadata_test_refused)
            result.code == "tvdb_refused" -> stringResource(R.string.settings_look_metadata_test_tvdb_refused)
            result.code == "tvdb_pin_refused" -> stringResource(R.string.settings_look_metadata_test_pin_refused)
            result.code == "unreachable" -> stringResource(R.string.settings_look_metadata_test_unreachable)
            else -> result.message
        }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (result.ok) MaterialTheme.acolyte.okText else MaterialTheme.acolyte.failText,
    )
}

@StringRes
private fun keyLabel(name: String): Int =
    when (name) {
        "tmdb_token" -> R.string.settings_look_metadata_key_tmdb_token
        "tvdb_key" -> R.string.settings_look_metadata_key_tvdb_key
        "tvdb_pin" -> R.string.settings_look_metadata_key_tvdb_pin
        "mdblist_key" -> R.string.settings_look_metadata_key_mdblist_key
        "fanart_key" -> R.string.settings_look_metadata_key_fanart_key
        else -> R.string.settings_look_metadata_key_omdb_key
    }

@StringRes
private fun keyHint(
    field: SettingsLookKeyField,
    ownMode: Boolean,
): Int =
    when (field.name) {
        "tmdb_token" ->
            if (field.fromDocker) R.string.settings_look_metadata_docker_key else R.string.settings_look_metadata_hint_tmdb_token
        "tvdb_key" -> if (ownMode) R.string.settings_look_metadata_hint_tvdb_key else R.string.settings_look_metadata_tvdb_desc
        "tvdb_pin" -> R.string.settings_look_metadata_hint_tvdb_pin
        "mdblist_key" -> R.string.settings_look_metadata_hint_mdblist_key
        "fanart_key" -> R.string.settings_look_metadata_hint_fanart_key
        else -> R.string.settings_look_metadata_hint_omdb_key
    }

@Composable
private fun OffCard(state: SettingsLookMetadataState) {
    SettingsCard(
        stringResource(R.string.settings_look_metadata_off_title),
        subtitle = stringResource(R.string.settings_look_metadata_off_desc),
    ) {
        state.offSources.forEach { source ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(sourceName(source), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                val needsKey = source.state == "needs_key"
                Hint(stringResource(if (needsKey) R.string.settings_look_metadata_needs_key else R.string.settings_look_metadata_state_off))
            }
        }
    }
}

@Composable
private fun sourceName(source: SettingsLookSource): String {
    val res =
        when (source.id) {
            "tmdb" -> R.string.settings_look_metadata_src_tmdb
            "tvmaze" -> R.string.settings_look_metadata_src_tvmaze
            "tvdb" -> R.string.settings_look_metadata_src_tvdb
            "music" -> R.string.settings_look_metadata_src_music
            "trailers" -> R.string.settings_look_metadata_src_trailers
            "ratings" -> R.string.settings_look_metadata_src_ratings
            "art" -> R.string.settings_look_metadata_src_art
            "omdb" -> R.string.settings_look_metadata_src_omdb
            "charts" -> R.string.settings_look_metadata_src_charts
            "streaming" -> R.string.settings_look_metadata_src_streaming
            "wiki" -> R.string.settings_look_metadata_src_wiki
            "anime" -> R.string.settings_look_metadata_src_anime
            else -> null
        }
    return res?.let { stringResource(it) } ?: source.name.ifEmpty { source.id }
}

@Composable
private fun ServerCard(
    state: SettingsLookMetadataState,
    viewModel: SettingsLookMetadataViewModel,
) {
    val health = state.health
    val up = health?.up != false
    SettingsCard(stringResource(R.string.settings_look_metadata_server)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Column(Modifier.weight(1f)) {
                val title =
                    when {
                        health == null && state.healthError != null -> R.string.settings_look_metadata_health_unknown
                        health == null -> R.string.settings_look_metadata_checking
                        up -> R.string.settings_look_metadata_connected
                        else -> R.string.settings_look_metadata_not_answering
                    }
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                if (health != null && !up) {
                    Hint(
                        if (health.fakeHost.isNotEmpty()) {
                            stringResource(R.string.settings_look_metadata_fake_ip, health.fakeHost)
                        } else {
                            stringResource(R.string.settings_look_metadata_down_text)
                        },
                    )
                }
            }
            if (health != null) {
                StatusChip(
                    stringResource(if (up) R.string.settings_look_metadata_answering else R.string.settings_look_metadata_down),
                    if (up) Tone.OK else Tone.FAIL,
                )
            }
        }
        state.healthError?.let { LookErrorText(it) }
        OutlinedButton(onClick = viewModel::checkAgain, enabled = !state.checking) {
            if (state.checking) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.settings_look_metadata_check_again))
            }
        }
    }
}

@Composable
private fun SentCard() {
    SettingsCard(
        stringResource(R.string.settings_look_metadata_sent_title),
        subtitle = stringResource(R.string.settings_look_metadata_sent_desc),
    ) {
        Item(R.string.settings_look_metadata_sent_token, R.string.settings_look_metadata_sent_token_text)
        Item(R.string.settings_look_metadata_sent_lookup, R.string.settings_look_metadata_sent_lookup_text)
        Item(R.string.settings_look_metadata_sent_display, R.string.settings_look_metadata_sent_display_text)
        Item(R.string.settings_look_metadata_sent_headers, R.string.settings_look_metadata_sent_headers_text)
        Item(R.string.settings_look_metadata_sent_country, R.string.settings_look_metadata_sent_country_text)
    }
}

@Composable
private fun Item(
    @StringRes lead: Int,
    @StringRes text: Int,
) {
    Column {
        Text(stringResource(lead), style = MaterialTheme.typography.titleSmall)
        Hint(stringResource(text))
    }
}

@Composable
private fun NeverCard() {
    SettingsCard(stringResource(R.string.settings_look_metadata_never_title)) {
        listOf(
            R.string.settings_look_metadata_never_1,
            R.string.settings_look_metadata_never_2,
            R.string.settings_look_metadata_never_3,
            R.string.settings_look_metadata_never_4,
        ).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun HostsCard(state: SettingsLookMetadataState) {
    SettingsCard(
        stringResource(R.string.settings_look_metadata_hosts_title),
        subtitle = stringResource(R.string.settings_look_metadata_hosts_desc),
    ) {
        state.hosts.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
