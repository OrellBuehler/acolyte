package ch.orellbuehler.acolyte.ui.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.OutsideList
import ch.orellbuehler.acolyte.api.model.TraktDeviceCode
import ch.orellbuehler.acolyte.feature.requests.ListKind
import ch.orellbuehler.acolyte.feature.requests.ListSeasons
import ch.orellbuehler.acolyte.feature.requests.OutsideListsState
import ch.orellbuehler.acolyte.feature.requests.OutsideNote
import ch.orellbuehler.acolyte.feature.requests.OutsideNoteKind
import ch.orellbuehler.acolyte.feature.requests.listLink
import ch.orellbuehler.acolyte.feature.requests.tmdb
import ch.orellbuehler.acolyte.feature.requests.verificationUrl
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.EmptyView
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun OutsideListsScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { OutsideListsViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    val snackbar = remember { SnackbarHostState() }
    var remove by rememberSaveable { mutableStateOf<Long?>(null) }
    val note = state.note
    val noteText = note?.let { outsideNoteText(it) }
    LaunchedEffect(note?.id) {
        if (noteText != null) {
            snackbar.showSnackbar(noteText)
            viewModel.dismissNote()
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.requests_outside_title),
        onBack = onBack,
        actions = {
            if (state.allowed && state.lists.isNotEmpty()) {
                IconButton(
                    onClick = viewModel::startNew,
                ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.requests_outside_add)) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                        if (state.lists.isEmpty()) {
                            item(key = "empty") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    EmptyView(
                                        title = stringResource(R.string.requests_outside_empty),
                                        message = stringResource(R.string.requests_outside_empty_text),
                                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    )
                                    Button(onClick = viewModel::startNew) { Text(stringResource(R.string.requests_outside_add)) }
                                }
                            }
                        }
                        items(state.lists, key = { it.id }) { list ->
                            ListRow(list, state, onSync = { viewModel.sync(list.id) }, onEdit = { viewModel.edit(list.id) }, onRemove = {
                                remove =
                                    list.id
                            })
                        }
                        item(key = "trakt") { TraktCard(state, viewModel) }
                        item(key = "mdblist") { MdblistCard(state, viewModel) }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (state.formOpen) ListForm(state, viewModel)
    remove?.let { id ->
        val list = state.lists.firstOrNull { it.id == id }
        ConfirmDialog(
            title = stringResource(R.string.requests_outside_remove_title, list?.name.orEmpty()),
            text =
                stringResource(
                    if (list?.titleKind ==
                        "show"
                    ) {
                        R.string.requests_outside_remove_shows
                    } else {
                        R.string.requests_outside_remove_movies
                    },
                ),
            confirm = stringResource(R.string.requests_remove),
            dismiss = stringResource(R.string.common_cancel),
            danger = true,
            onConfirm = {
                remove = null
                viewModel.remove(id)
            },
            onDismiss = { remove = null },
        )
    }
}

@Composable
private fun outsideNoteText(note: OutsideNote): String =
    when (note.kind) {
        OutsideNoteKind.SAVED -> stringResource(R.string.requests_outside_saved)
        OutsideNoteKind.SAVED_OFF -> stringResource(R.string.requests_outside_saved_off)
        OutsideNoteKind.READING -> stringResource(R.string.requests_outside_reading, note.name)
        OutsideNoteKind.READ_FAILED -> stringResource(R.string.requests_outside_read_failed)
        OutsideNoteKind.REMOVED -> stringResource(R.string.requests_outside_removed)
        OutsideNoteKind.REMOVE_FAILED -> stringResource(R.string.requests_outside_remove_failed)
        OutsideNoteKind.TRAKT_SAVED -> stringResource(R.string.requests_trakt_saved)
        OutsideNoteKind.TRAKT_KEY_SAVED -> stringResource(R.string.requests_trakt_key_saved)
        OutsideNoteKind.TRAKT_SIGNED_IN -> stringResource(R.string.requests_trakt_signed_in)
        OutsideNoteKind.TRAKT_DENIED -> stringResource(R.string.requests_trakt_denied)
        OutsideNoteKind.TRAKT_EXPIRED -> stringResource(R.string.requests_trakt_expired)
        OutsideNoteKind.TRAKT_SIGNED_OUT -> stringResource(R.string.requests_trakt_signed_out)
        OutsideNoteKind.MDBLIST_SAVED -> stringResource(R.string.requests_mdblist_saved)
        OutsideNoteKind.MDBLIST_REMOVED -> stringResource(R.string.requests_mdblist_removed)
    }

@Composable
fun listKindLabel(kind: ListKind): String =
    stringResource(
        when (kind) {
            ListKind.TMDB -> R.string.requests_outside_kind_tmdb
            ListKind.TRAKT -> R.string.requests_outside_kind_trakt
            ListKind.MDBLIST -> R.string.requests_outside_kind_mdblist
        },
    )

@Composable
fun everyText(hours: Int): String =
    when {
        hours == 24 -> stringResource(R.string.requests_outside_every_day)
        hours > 0 && hours % 24 == 0 -> stringResource(R.string.requests_outside_every_days, hours / 24)
        else -> stringResource(R.string.requests_outside_every_hours, hours)
    }

@Composable
private fun ListRow(
    list: OutsideList,
    state: OutsideListsState,
    onSync: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val uri = LocalUriHandler.current
    val busy = list.id in state.busy
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(list.name, style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                AssistChip(onClick = {}, label = { Text(listKindLabel(ListKind.of(list.kind))) })
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            stringResource(
                                if (list.titleKind ==
                                    "show"
                                ) {
                                    R.string.requests_kind_shows
                                } else {
                                    R.string.requests_kind_movies
                                },
                            ),
                        )
                    },
                )
                if (!list.enabled) AssistChip(onClick = {}, label = { Text(stringResource(R.string.requests_outside_off)) })
                if (list.lastError.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.requests_outside_problem)) },
                    )
                }
            }
            TextButton(onClick = { runCatching { uri.openUri(listLink(list)) } }) {
                Text("${list.ref} · ${everyText(list.syncEveryHours)}")
            }
            Hint(
                relativeTime(list.lastSyncedAt)?.let { stringResource(R.string.requests_outside_last_read, it, list.lastResult) }
                    ?: stringResource(R.string.requests_outside_not_read),
            )
            if (list.lastError.isNotBlank()) FailText(list.lastError)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedButton(
                    onClick = onSync,
                    enabled = list.enabled && !busy,
                ) { Text(stringResource(R.string.requests_outside_read_now)) }
                OutlinedButton(onClick = onEdit, enabled = !busy) { Text(stringResource(R.string.requests_outside_change)) }
                TextButton(onClick = onRemove, enabled = !busy) { Text(stringResource(R.string.requests_remove)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListForm(
    state: OutsideListsState,
    viewModel: OutsideListsViewModel,
) {
    val draft = state.draft
    ModalBottomSheet(onDismissRequest = viewModel::cancelEdit, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen).padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(
                if (state.editing ==
                    0L
                ) {
                    stringResource(R.string.requests_outside_add)
                } else {
                    stringResource(R.string.requests_outside_change_title, draft.name)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.requests_outside_name)) },
                placeholder = { Text(stringResource(R.string.requests_outside_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PickerField(
                stringResource(R.string.requests_outside_kind),
                ListKind.entries.map {
                    it to listKindLabel(it)
                },
                state.draftKind,
                viewModel::setKind,
            )
            Text(stringResource(R.string.requests_outside_adds), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !state.isShowList, onClick = {
                    viewModel.setTitleKind(false)
                }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text(stringResource(R.string.requests_kind_movies))
                }
                SegmentedButton(
                    selected = state.isShowList,
                    onClick = { viewModel.setTitleKind(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) {
                    Text(stringResource(R.string.requests_kind_shows))
                }
            }
            OutlinedTextField(
                value = draft.ref,
                onValueChange = viewModel::setRef,
                label = { Text(stringResource(R.string.requests_outside_ref)) },
                supportingText = {
                    Text(
                        stringResource(
                            when (state.draftKind) {
                                ListKind.TMDB -> R.string.requests_outside_ref_tmdb
                                ListKind.TRAKT -> R.string.requests_outside_ref_trakt
                                ListKind.MDBLIST -> R.string.requests_outside_ref_mdblist
                            },
                        ),
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.draftFolders.isEmpty()) {
                FailText(
                    stringResource(
                        if (state.isShowList) R.string.requests_outside_no_show_folder else R.string.requests_outside_no_movie_folder,
                    ),
                )
            } else {
                PickerField(
                    stringResource(R.string.requests_outside_folder),
                    state.draftFolders.map { it.id to if (it.label.isNotBlank()) "${it.label} (${it.path})" else it.path },
                    draft.libraryFolderId,
                    viewModel::setFolder,
                )
            }
            PickerField(
                stringResource(R.string.requests_quality),
                listOf(0L to stringResource(R.string.requests_outside_folder_own)) + state.draftRules.map { it.id to it.name },
                draft.pickRulesId,
                viewModel::setPickRules,
            )
            if (state.isShowList) {
                PickerField(
                    stringResource(R.string.requests_outside_seasons),
                    ListSeasons.entries.map { it to listSeasonsLabel(it) },
                    state.draftSeasons,
                    viewModel::setSeasons,
                    hint =
                        if (state.draftSeasons ==
                            ListSeasons.FIRST
                        ) {
                            stringResource(R.string.requests_outside_seasons_first_hint)
                        } else {
                            null
                        },
                )
                SwitchRow(
                    stringResource(R.string.requests_outside_new_seasons),
                    draft.newSeasons == "all",
                    viewModel::setNewSeasons,
                    enabled = state.draftSeasons != ListSeasons.FUTURE,
                    hint =
                        stringResource(
                            if (state.draftSeasons ==
                                ListSeasons.FUTURE
                            ) {
                                R.string.requests_outside_new_seasons_future
                            } else {
                                R.string.requests_outside_new_seasons_hint
                            },
                        ),
                )
            }
            OutlinedTextField(
                value = if (draft.syncEveryHours > 0) draft.syncEveryHours.toString() else "",
                onValueChange = viewModel::setEvery,
                label = { Text(stringResource(R.string.requests_outside_every)) },
                supportingText = { Text(stringResource(R.string.requests_outside_every_hint)) },
                isError = draft.syncEveryHours !in 6..168,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow(
                stringResource(R.string.requests_outside_keep),
                draft.keepUpToDate,
                viewModel::setKeepUpToDate,
                hint = stringResource(if (state.isShowList) R.string.requests_outside_keep_show else R.string.requests_outside_keep_movie),
            )
            SwitchRow(
                stringResource(R.string.requests_outside_search),
                draft.searchOnAdd,
                viewModel::setSearchOnAdd,
                hint =
                    stringResource(
                        if (state.isShowList) R.string.requests_outside_search_show else R.string.requests_outside_search_movie,
                    ),
            )
            SwitchRow(stringResource(R.string.requests_outside_enabled), draft.enabled, viewModel::setEnabled)
            if (state.needsTraktKey) FailText(stringResource(R.string.requests_outside_needs_trakt))
            state.formError?.let { FailText(askErrorText(it)) }
            SheetFooter(stringResource(R.string.requests_save), state.canSave, state.saving, viewModel::cancelEdit, viewModel::saveList)
        }
    }
}

@Composable
private fun listSeasonsLabel(seasons: ListSeasons): String =
    stringResource(
        when (seasons) {
            ListSeasons.ALL -> R.string.requests_outside_seasons_all
            ListSeasons.FUTURE -> R.string.requests_outside_seasons_future
            ListSeasons.LATEST -> R.string.requests_outside_seasons_latest
            ListSeasons.FIRST -> R.string.requests_outside_seasons_first
            ListSeasons.NONE -> R.string.requests_outside_seasons_none
        },
    )

@Composable
private fun KeyError(state: OutsideListsState) {
    val error = state.keyError ?: return
    FailText(
        if (error.code == ApiErrorCode.FORBIDDEN && error.message.contains("password")) {
            stringResource(R.string.requests_key_password)
        } else {
            askErrorText(error)
        },
    )
}

@Composable
private fun TraktCard(
    state: OutsideListsState,
    viewModel: OutsideListsViewModel,
) {
    var key by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    val trakt = state.trakt
    SettingsCard(
        stringResource(R.string.requests_trakt_title),
        subtitle = stringResource(if (state.traktKeySet) R.string.requests_key_saved else R.string.requests_key_not_set),
    ) {
        Hint(stringResource(R.string.requests_trakt_text))
        if (state.hasTraktList && !state.traktKeySet) FailText(stringResource(R.string.requests_trakt_lists_blocked))
        SecretField(key, {
            key = it
        }, stringResource(if (state.traktKeySet) R.string.requests_trakt_replace_key else R.string.requests_trakt_paste_key))
        if (state.owner) {
            SecretField(
                token,
                { token = it },
                stringResource(if (state.traktToken.isNotEmpty()) R.string.requests_trakt_replace_token else R.string.requests_trakt_token),
                hint = stringResource(R.string.requests_trakt_token_hint),
            )
            SecretField(secret, { secret = it }, stringResource(R.string.requests_trakt_secret))
        }
        KeyError(state)
        Button(
            onClick = {
                viewModel.saveTrakt(key, token, secret) {
                    key = ""
                    token = ""
                    secret = ""
                }
            },
            enabled = !state.keyBusy && (key.isNotBlank() || token.isNotBlank() || secret.isNotBlank()),
        ) { Text(stringResource(R.string.requests_save)) }
        if (state.traktKeySet && trakt != null) {
            Text(stringResource(R.string.requests_trakt_signin), style = MaterialTheme.typography.titleSmall)
            Hint(stringResource(R.string.requests_trakt_signin_text))
            Text(
                if (trakt.signedIn) {
                    stringResource(R.string.requests_trakt_signed_in_as, trakt.username)
                } else {
                    stringResource(R.string.requests_trakt_not_signed_in)
                },
            )
            if (trakt.problem.isNotBlank()) FailText(trakt.problem)
            if (state.owner) {
                val device = state.device
                when {
                    device != null -> DeviceCode(device, viewModel::cancelSignIn)
                    trakt.signedIn ->
                        OutlinedButton(onClick = viewModel::signOutTrakt, enabled = !state.keyBusy) {
                            Text(stringResource(R.string.requests_trakt_sign_out))
                        }
                    else -> {
                        Button(
                            onClick = viewModel::signInTrakt,
                            enabled = state.canSignIn,
                        ) { Text(stringResource(R.string.requests_trakt_sign_in)) }
                        if (!trakt.clientSecret) Hint(stringResource(R.string.requests_trakt_needs_secret))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCode(
    device: TraktDeviceCode,
    onCancel: () -> Unit,
) {
    val uri = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val url = verificationUrl(device)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Hint(stringResource(R.string.requests_trakt_device, url.removePrefix("https://"), device.userCode))
        Text(device.userCode, style = MaterialTheme.typography.headlineMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(device.userCode)) },
            ) { Text(stringResource(R.string.requests_copy)) }
            Button(onClick = { runCatching { uri.openUri(url) } }) { Text(stringResource(R.string.requests_trakt_open)) }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
        }
    }
}

@Composable
private fun MdblistCard(
    state: OutsideListsState,
    viewModel: OutsideListsViewModel,
) {
    var key by rememberSaveable { mutableStateOf("") }
    val saved = state.mdblistKey.isNotEmpty()
    SettingsCard(
        stringResource(R.string.requests_mdblist_title),
        subtitle = stringResource(if (saved) R.string.requests_key_saved else R.string.requests_key_not_set),
    ) {
        Hint(stringResource(R.string.requests_mdblist_text))
        SecretField(key, { key = it }, stringResource(if (saved) R.string.requests_mdblist_replace else R.string.requests_mdblist_paste))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Button(onClick = { viewModel.saveMdblist(key) { key = "" } }, enabled = !state.keyBusy && key.isNotBlank()) {
                Text(stringResource(R.string.requests_save))
            }
            if (saved) {
                TextButton(
                    onClick = { viewModel.saveMdblist("") {} },
                    enabled = !state.keyBusy,
                ) { Text(stringResource(R.string.requests_mdblist_remove)) }
            }
        }
    }
}

@Composable
private fun SecretField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    hint: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun NeverAddScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { NeverAddViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var show by rememberSaveable { mutableStateOf(false) }
    var tmdb by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val removedText = state.removed.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.requests_never_allowed_again, it) }
    LaunchedEffect(state.removed) { if (removedText != null) snackbar.showSnackbar(removedText) }
    ScreenScaffold(title = stringResource(R.string.requests_never_title), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.allowed) {
                NotAllowed()
            } else {
                StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        item(key = "form") {
                            SettingsCard(
                                stringResource(R.string.requests_never_add),
                                subtitle = stringResource(R.string.requests_never_sub),
                            ) {
                                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = !show,
                                        onClick = { show = false },
                                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                                    ) {
                                        Text(stringResource(R.string.requests_kind_movie))
                                    }
                                    SegmentedButton(
                                        selected = show,
                                        onClick = { show = true },
                                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                                    ) {
                                        Text(stringResource(R.string.requests_kind_show))
                                    }
                                }
                                OutlinedTextField(
                                    value = tmdb,
                                    onValueChange = { tmdb = it.take(30) },
                                    label = { Text(stringResource(R.string.requests_never_tmdb)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it.take(200) },
                                    label = { Text(stringResource(R.string.requests_outside_name)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it.take(200) },
                                    label = { Text(stringResource(R.string.requests_never_reason)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (state.missing) FailText(stringResource(R.string.requests_never_missing))
                                state.error?.let { FailText(askErrorText(it)) }
                                Button(
                                    onClick = {
                                        viewModel.add(show, tmdb, name, reason) {
                                            tmdb = ""
                                            name = ""
                                            reason = ""
                                        }
                                    },
                                    enabled = !state.busy,
                                ) { Text(stringResource(R.string.requests_never_add_button)) }
                            }
                        }
                        if (state.entries.isEmpty()) {
                            item(
                                key = "empty",
                            ) { EmptyView(title = stringResource(R.string.requests_never_empty), icon = Icons.Rounded.Block) }
                        }
                        items(state.entries, key = { it.id }) { entry ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                                    val kind = if (entry.kind == "show") stringResource(R.string.requests_kind_show) + " · " else ""
                                    Hint(listOf("${kind}tmdb:${entry.tmdb}", entry.reason).filter { it.isNotBlank() }.joinToString(" · "))
                                }
                                TextButton(onClick = { viewModel.remove(entry.id) }, enabled = entry.id !in state.removing) {
                                    Text(stringResource(R.string.requests_remove))
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
