package ch.orellbuehler.acolyte.ui.requests

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.QualityLine
import ch.orellbuehler.acolyte.feature.requests.FOUR_K_LINE
import ch.orellbuehler.acolyte.feature.requests.FolderField
import ch.orellbuehler.acolyte.feature.requests.LimitKind
import ch.orellbuehler.acolyte.feature.requests.LineKind
import ch.orellbuehler.acolyte.feature.requests.MAX_SERVICES
import ch.orellbuehler.acolyte.feature.requests.RequestSettingsState
import ch.orellbuehler.acolyte.feature.requests.STREAMING_REGIONS
import ch.orellbuehler.acolyte.feature.requests.SettingsFlag
import ch.orellbuehler.acolyte.feature.requests.parseLimit
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.theme.Spacing
import java.util.Locale

@Composable
fun RequestRulesScreen(
    onBack: () -> Unit,
    onNavigate: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { RequestRulesViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.requests_rules_saved)
    LaunchedEffect(state.savedCount) { if (state.savedCount > 0) snackbar.showSnackbar(savedText) }
    ScreenScaffold(title = stringResource(R.string.requests_rules_title), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    ) {
                        item(key = "limits") { LimitsCard(state, viewModel) }
                        item(key = "approved") { ApprovedCard(state, viewModel) }
                        item(key = "approvals") {
                            SettingsCard(stringResource(R.string.requests_rules_approvals)) {
                                SwitchRow(
                                    stringResource(R.string.requests_rules_owner_auto),
                                    state.flag(SettingsFlag.OWNER_AUTO_APPROVE),
                                    { viewModel.setFlag(SettingsFlag.OWNER_AUTO_APPROVE, it) },
                                    hint = stringResource(R.string.requests_rules_approvals_hint),
                                )
                            }
                        }
                        item(key = "fourk") { FourKCard(state, viewModel) }
                        if (state.lines(LineKind.MOVIE).isNotEmpty() || state.lines(LineKind.SHOW).isNotEmpty()) {
                            item(key = "lines") { LinesCard(state, viewModel) }
                        }
                        item(key = "plex") { PlexCard(state, viewModel) }
                        item(key = "streaming") { StreamingCard(state, viewModel, onNavigate) }
                    }
                }
                if (state.dirty) SaveBar(state, viewModel, Modifier.align(Alignment.BottomCenter))
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (state.dirty) 96.dp else 0.dp))
        }
    }
}

@Composable
fun NotAllowed() {
    MessageView(
        icon = Icons.Rounded.Lock,
        title = stringResource(R.string.requests_not_allowed),
        message = stringResource(R.string.requests_not_allowed_text),
    )
}

@Composable
private fun SaveBar(
    state: RequestSettingsState,
    viewModel: RequestRulesViewModel,
    modifier: Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding()) {
            state.saveError?.let { FailText(askErrorText(it)) }
            if (!state.limitsValid) FailText(stringResource(R.string.requests_rules_limit_invalid))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = viewModel::discard) { Text(stringResource(R.string.requests_discard)) }
                Button(onClick = viewModel::save, enabled = state.canSave) { Text(stringResource(R.string.requests_save)) }
            }
        }
    }
}

@Composable
private fun LimitsCard(
    state: RequestSettingsState,
    viewModel: RequestRulesViewModel,
) {
    SettingsCard(stringResource(R.string.requests_rules_limits), subtitle = stringResource(R.string.requests_rules_limits_sub)) {
        LimitKind.entries.forEach { kind ->
            OutlinedTextField(
                value = state.limit(kind),
                onValueChange = { viewModel.setLimit(kind, it) },
                label = {
                    Text(
                        stringResource(
                            when (kind) {
                                LimitKind.MOVIES -> R.string.requests_rules_movie_asks
                                LimitKind.SHOWS -> R.string.requests_rules_show_asks
                                LimitKind.MUSIC -> R.string.requests_rules_music_asks
                            },
                        ),
                    )
                },
                supportingText =
                    when (kind) {
                        LimitKind.SHOWS -> ({ Text(stringResource(R.string.requests_rules_show_asks_hint)) })
                        LimitKind.MUSIC -> ({ Text(stringResource(R.string.requests_rules_music_asks_hint)) })
                        LimitKind.MOVIES -> null
                    },
                isError = parseOk(state.limit(kind)).not(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Hint(stringResource(R.string.requests_rules_limits_hint))
    }
}

private fun parseOk(text: String): Boolean = parseLimit(text) != null

@Composable
private fun folderOptions(
    state: RequestSettingsState,
    field: FolderField,
): List<Pair<Long, String>> {
    val first =
        stringResource(
            when (field) {
                FolderField.MOVIE -> R.string.requests_rules_first_movie_folder
                FolderField.SHOW -> R.string.requests_rules_first_show_folder
                FolderField.MUSIC -> R.string.requests_rules_first_music_folder
                FolderField.MOVIE_FOUR_K, FolderField.SHOW_FOUR_K -> R.string.requests_rules_same_folder
            },
        )
    return listOf(0L to first) + state.foldersFor(field).map { it.id to if (it.label.isNotBlank()) "${it.label} (${it.path})" else it.path }
}

@Composable
private fun rulesOptions(
    state: RequestSettingsState,
    field: FolderField,
): List<Pair<Long, String>> {
    val none =
        stringResource(
            if (field == FolderField.MOVIE_FOUR_K ||
                field == FolderField.SHOW_FOUR_K
            ) {
                R.string.requests_rules_named_4k
            } else {
                R.string.requests_rules_folder_quality
            },
        )
    return listOf(0L to none) + state.rulesFor(field).map { it.id to it.name }
}

@Composable
private fun ApprovedCard(
    state: RequestSettingsState,
    viewModel: RequestRulesViewModel,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.requests_rules_approved)) {
        PickerField(
            stringResource(
                R.string.requests_rules_movie_folder,
            ),
            folderOptions(state, FolderField.MOVIE),
            state.folder(FolderField.MOVIE),
            {
                viewModel.setFolder(FolderField.MOVIE, it)
            },
        )
        PickerField(
            stringResource(
                R.string.requests_rules_show_folder,
            ),
            folderOptions(state, FolderField.SHOW),
            state.folder(FolderField.SHOW),
            {
                viewModel.setFolder(FolderField.SHOW, it)
            },
        )
        PickerField(
            stringResource(R.string.requests_rules_music_folder),
            folderOptions(state, FolderField.MUSIC),
            state.folder(FolderField.MUSIC),
            { viewModel.setFolder(FolderField.MUSIC, it) },
            hint = stringResource(R.string.requests_rules_music_folder_hint),
        )
        SwitchRow(
            stringResource(R.string.requests_rules_search_on_approve),
            state.flag(SettingsFlag.SEARCH_ON_APPROVE),
            { viewModel.setFlag(SettingsFlag.SEARCH_ON_APPROVE, it) },
            hint = stringResource(R.string.requests_rules_search_on_approve_hint),
        )
        TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.requests_rules_advanced)) }
        if (advanced) {
            listOf(
                FolderField.MOVIE to R.string.requests_rules_movie_quality,
                FolderField.SHOW to R.string.requests_rules_show_quality,
                FolderField.MUSIC to R.string.requests_rules_music_quality,
            ).forEach { (field, label) ->
                PickerField(
                    stringResource(label),
                    rulesOptions(state, field),
                    state.pickRules(field),
                    { viewModel.setPickRules(field, it) },
                )
            }
            Hint(stringResource(R.string.requests_rules_quality_hint))
        }
    }
}

@Composable
private fun FourKCard(
    state: RequestSettingsState,
    viewModel: RequestRulesViewModel,
) {
    SettingsCard(stringResource(R.string.requests_rules_four_k), subtitle = stringResource(R.string.requests_rules_four_k_sub)) {
        PickerField(
            stringResource(
                R.string.requests_rules_movie_folder,
            ),
            folderOptions(state, FolderField.MOVIE_FOUR_K),
            state.folder(FolderField.MOVIE_FOUR_K),
            {
                viewModel.setFolder(FolderField.MOVIE_FOUR_K, it)
            },
        )
        PickerField(
            stringResource(
                R.string.requests_rules_movie_quality,
            ),
            rulesOptions(state, FolderField.MOVIE_FOUR_K),
            state.pickRules(FolderField.MOVIE_FOUR_K),
            {
                viewModel.setPickRules(FolderField.MOVIE_FOUR_K, it)
            },
        )
        PickerField(
            stringResource(
                R.string.requests_rules_show_folder,
            ),
            folderOptions(state, FolderField.SHOW_FOUR_K),
            state.folder(FolderField.SHOW_FOUR_K),
            {
                viewModel.setFolder(FolderField.SHOW_FOUR_K, it)
            },
        )
        PickerField(
            stringResource(
                R.string.requests_rules_show_quality,
            ),
            rulesOptions(state, FolderField.SHOW_FOUR_K),
            state.pickRules(FolderField.SHOW_FOUR_K),
            {
                viewModel.setPickRules(FolderField.SHOW_FOUR_K, it)
            },
        )
    }
}

@Composable
private fun LinesCard(
    state: RequestSettingsState,
    viewModel: RequestRulesViewModel,
) {
    SettingsCard(stringResource(R.string.requests_rules_lines), subtitle = stringResource(R.string.requests_rules_lines_sub)) {
        LineKind.entries.forEach { kind ->
            Text(
                stringResource(if (kind == LineKind.MOVIE) R.string.requests_rules_movie_asks else R.string.requests_rules_show_asks),
                style = MaterialTheme.typography.titleSmall,
            )
            val lines = state.lines(kind)
            lines.forEachIndexed { index, line -> LineEditor(state, viewModel, kind, index, line, lines.size) }
            OutlinedButton(onClick = { viewModel.addLine(kind) }, enabled = state.canAddLine(kind)) {
                Text(stringResource(R.string.requests_rules_add_line))
            }
            HorizontalDivider()
        }
        SwitchRow(
            stringResource(R.string.requests_rules_pick_many),
            state.flag(SettingsFlag.PICK_MANY),
            { viewModel.setFlag(SettingsFlag.PICK_MANY, it) },
            hint = stringResource(R.string.requests_rules_pick_many_hint),
        )
    }
}

@Composable
private fun LineEditor(
    state: RequestSettingsState,
    viewModel: RequestRulesViewModel,
    kind: LineKind,
    index: Int,
    line: QualityLine,
    count: Int,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.XSmall), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        OutlinedTextField(
            value = line.label,
            onValueChange = { viewModel.setLineLabel(kind, index, it) },
            label = { Text(stringResource(R.string.requests_rules_line_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = line.name,
            onValueChange = { viewModel.setLineName(kind, index, it) },
            label = { Text(stringResource(R.string.requests_rules_line_name)) },
            placeholder = { Text(qualityName(line.id, "")) },
            isError = line.isOwn && line.name.isBlank(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (line.isOwn) {
            val field = if (kind == LineKind.MOVIE) FolderField.MOVIE else FolderField.SHOW
            PickerField(stringResource(R.string.requests_quality), state.rulesFor(field).map { it.id to it.name }, line.pickRulesId, {
                viewModel.setLineRules(kind, index, it)
            })
        }
        val rules =
            state.ruleName(line.pickRulesId).ifBlank {
                stringResource(if (line.id == FOUR_K_LINE) R.string.requests_rules_named_4k else R.string.requests_rules_folder_quality)
            }
        val copy = stringResource(if (line.extra) R.string.requests_rules_own_copy else R.string.requests_rules_main_copy)
        Hint(if (line.ready) "$rules · $copy" else "$rules · $copy. " + stringResource(R.string.requests_rules_not_ready))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = !line.hidden, onCheckedChange = { viewModel.setLineShown(kind, index, it) })
            Text(stringResource(R.string.requests_rules_line_shown), Modifier.weight(1f))
            IconButton(onClick = { viewModel.moveLine(kind, index, up = true) }, enabled = index > 0) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.requests_move_up))
            }
            IconButton(onClick = { viewModel.moveLine(kind, index, up = false) }, enabled = index < count - 1) {
                Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.requests_move_down))
            }
            if (line.isOwn) {
                IconButton(onClick = { viewModel.removeLine(kind, index) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.requests_remove))
                }
            }
        }
    }
}

@Composable
private fun PlexCard(
    state: RequestSettingsState,
    viewModel: RequestRulesViewModel,
) {
    SettingsCard(stringResource(R.string.requests_rules_plex), subtitle = stringResource(R.string.requests_rules_plex_sub)) {
        SwitchRow(
            stringResource(R.string.requests_rules_plex_on),
            state.flag(SettingsFlag.PLEX_WATCHLISTS),
            { viewModel.setFlag(SettingsFlag.PLEX_WATCHLISTS, it) },
            hint = stringResource(R.string.requests_rules_plex_on_hint),
        )
        SwitchRow(
            stringResource(R.string.requests_rules_plex_everyone),
            state.flag(SettingsFlag.PLEX_EVERYONE),
            { viewModel.setFlag(SettingsFlag.PLEX_EVERYONE, it) },
            hint = stringResource(R.string.requests_rules_plex_everyone_hint),
            enabled = state.flagEnabled(SettingsFlag.PLEX_EVERYONE),
        )
        SwitchRow(
            stringResource(R.string.requests_rules_plex_friends),
            state.flag(SettingsFlag.PLEX_FRIENDS),
            { viewModel.setFlag(SettingsFlag.PLEX_FRIENDS, it) },
            hint = stringResource(R.string.requests_rules_plex_friends_hint),
            enabled = state.flagEnabled(SettingsFlag.PLEX_FRIENDS),
        )
        val friends = state.plexFriends
        if (state.showFriends && friends != null) {
            HorizontalDivider()
            when {
                !friends.ownerLinked -> Hint(stringResource(R.string.requests_friends_link_first))
                friends.people.isEmpty() -> Hint(stringResource(R.string.requests_friends_none))
                else ->
                    friends.people.forEach { friend ->
                        Column {
                            Text(
                                if (friend.kid) "${friend.name} · ${stringResource(R.string.requests_kid)}" else friend.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val checked =
                                relativeTime(friend.checkedAt)?.let { stringResource(R.string.requests_friends_checked, it) }
                                    ?: stringResource(R.string.requests_friends_not_checked)
                            Hint(stringResource(R.string.requests_friends_line, friend.plexName, checked, friend.asked))
                            if (friend.error.isNotBlank()) FailText(friend.error)
                        }
                    }
            }
        }
    }
}

@Composable
private fun StreamingCard(
    state: RequestSettingsState,
    viewModel: RequestRulesViewModel,
    onNavigate: (Any) -> Unit,
) {
    val region = state.draft.streamingRegion
    SettingsCard(stringResource(R.string.requests_rules_streaming), subtitle = stringResource(R.string.requests_rules_streaming_sub)) {
        val regions =
            (
                listOf("" to stringResource(R.string.requests_rules_not_set)) +
                    STREAMING_REGIONS.map { it to regionName(it) }.sortedBy { it.second }
            ) +
                (if (region.isNotEmpty() && region !in STREAMING_REGIONS) listOf(region to region) else emptyList())
        PickerField(stringResource(R.string.requests_rules_country), regions, region, viewModel::setRegion)
        if (region.isEmpty()) {
            val mine = LocalConfiguration.current.locales[0].country
            if (mine in STREAMING_REGIONS) {
                TextButton(onClick = { viewModel.setRegion(mine) }) { Text(stringResource(R.string.requests_rules_use_my_country)) }
            }
        } else {
            when {
                state.servicesLoading -> Hint(stringResource(R.string.requests_rules_services_loading))
                state.servicesError != null -> FailText(errorText(state.servicesError!!))
                state.services.isEmpty() -> Hint(stringResource(R.string.requests_rules_services_none))
                else -> {
                    Hint(stringResource(R.string.requests_rules_services_picked, state.draft.streamingServices.size, MAX_SERVICES))
                    state.sortedServices.forEach { service ->
                        val on = service.id in state.draft.streamingServices
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.toggleService(service.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = on, onCheckedChange = { viewModel.toggleService(service.id) })
                            RequestPoster(service.logo, service.name, width = 28.dp)
                            Text(service.name, Modifier.padding(start = Spacing.Small))
                        }
                    }
                }
            }
        }
        SwitchRow(
            stringResource(R.string.requests_rules_skip),
            state.flag(SettingsFlag.SKIP_ON_SERVICES),
            { viewModel.setFlag(SettingsFlag.SKIP_ON_SERVICES, it) },
            hint = stringResource(R.string.requests_rules_skip_hint),
            enabled = state.flagEnabled(SettingsFlag.SKIP_ON_SERVICES),
        )
        HorizontalDivider()
        Text(stringResource(R.string.requests_cleanup_title), style = MaterialTheme.typography.titleSmall)
        Hint(stringResource(R.string.requests_cleanup_sub))
        val check = state.cleanup
        Button(onClick = viewModel::startCleanup, enabled = state.canCheck) { Text(stringResource(R.string.requests_cleanup_check)) }
        if (state.dirty) Hint(stringResource(R.string.requests_cleanup_save_first))
        state.cleanupError?.let { FailText(askErrorText(it)) }
        if (check != null) {
            if (check.running) {
                Hint(stringResource(R.string.requests_cleanup_progress, check.checked, check.total))
                LinearProgressIndicator(
                    progress = { if (check.total > 0) check.checked.toFloat() / check.total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (check.finishedAt.isNotBlank()) {
                val bytes = check.found.sumOf { it.bytes }
                Hint(
                    if (check.found.isEmpty()) {
                        stringResource(R.string.requests_cleanup_none, check.total)
                    } else {
                        stringResource(R.string.requests_cleanup_found, check.found.size, formatBytes(bytes))
                    } + if (check.failed > 0) " " + stringResource(R.string.requests_cleanup_failed, check.failed) else "",
                )
            }
            check.found.forEach { title ->
                Row(
                    Modifier.fillMaxWidth().clickable(
                        enabled = title.tmdbId.isNotEmpty(),
                    ) { onNavigate(Routes.title(title.kind, title.tmdbId)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = Spacing.XSmall)) {
                        Text(
                            if (title.year >
                                0
                            ) {
                                "${title.name} (${title.year})"
                            } else {
                                title.name
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Hint(stringResource(R.string.requests_cleanup_row, title.service, formatBytes(title.bytes)))
                    }
                    TextButton(onClick = { onNavigate(Routes.title(title.kind, title.tmdbId)) }, enabled = title.tmdbId.isNotEmpty()) {
                        Text(stringResource(R.string.requests_open_title))
                    }
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (value < 10 &&
        unit > 0
    ) {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
    } else {
        "${value.toLong()} ${units[unit]}"
    }
}
