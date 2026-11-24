package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderForm
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderIssue
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderKind
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderKindField
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderSeedingFields
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderSignIn
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloadersState
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.samePathPair
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SwitchRow
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun DownloaderForm(
    state: SettingsDownloadersState,
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.Screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            if (form.isNew) {
                Text(stringResource(R.string.acq_dl_which_app), style = MaterialTheme.typography.titleMedium)
                KindGroup(stringResource(R.string.acq_dl_torrents), SettingsDownloaderKind.torrents, form, viewModel)
                KindGroup(stringResource(R.string.acq_dl_usenet), SettingsDownloaderKind.usenetKinds, form, viewModel)
            }
            if (form.showFields) FormFields(state, form, viewModel)
            state.formError?.let { error -> FailText(downloaderErrorText(error)) }
        }
        FormBar(state, form, viewModel)
    }
}

@Composable
private fun KindGroup(
    title: String,
    kinds: List<SettingsDownloaderKind>,
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    kinds.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            pair.forEach { kind ->
                val picked = form.picked && form.kind == kind.wire
                Card(
                    onClick = { viewModel.pickKind(kind.wire) },
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (picked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                        Text(kind.badge, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(kind.appName, style = MaterialTheme.typography.titleMedium)
                        if (kind == SettingsDownloaderKind.QBITTORRENT || kind == SettingsDownloaderKind.SABNZBD) {
                            Hint(stringResource(R.string.acq_dl_most_used))
                        }
                    }
                }
            }
            if (pair.size == 1) Column(Modifier.weight(1f)) {}
        }
    }
}

@Composable
private fun Field(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
    numbers: Boolean = false,
    mono: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = (error ?: hint)?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    when {
                        password -> KeyboardType.Password
                        numbers -> KeyboardType.Decimal
                        else -> KeyboardType.Text
                    },
            ),
        textStyle =
            if (mono) {
                MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                )
            } else {
                MaterialTheme.typography.bodyLarge
            },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun issueText(
    form: SettingsDownloaderForm,
    vararg issues: SettingsDownloaderIssue,
): String? = issues.firstOrNull { form.problem(it) }?.let { downloaderIssueText(it) }

@Composable
private fun FormFields(
    state: SettingsDownloadersState,
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    val kind = form.kindInfo
    Field(form.name, stringResource(R.string.acq_dl_name), viewModel::setName, error = issueText(form, SettingsDownloaderIssue.NAME))
    Field(
        form.url,
        stringResource(R.string.acq_dl_address),
        viewModel::setUrl,
        hint = stringResource(R.string.acq_dl_address_hint),
        error = issueText(form, SettingsDownloaderIssue.URL),
        placeholder = kind?.placeholder,
        mono = true,
    )
    SignInFields(state, form, viewModel)
    when (kind) {
        SettingsDownloaderKind.DELUGE -> Hint(stringResource(R.string.acq_dl_deluge_note))
        SettingsDownloaderKind.RTORRENT -> Hint(stringResource(R.string.acq_dl_rtorrent_note))
        SettingsDownloaderKind.ARIA2 -> Hint(stringResource(R.string.acq_dl_aria2_note))
        else -> Unit
    }
    if (kind == SettingsDownloaderKind.QBITTORRENT) {
        var open by rememberSaveable { mutableStateOf(false) }
        Expander(stringResource(R.string.acq_dl_more_options), open) { open = !open }
        if (open) CategoryFields(state, form, viewModel)
    } else {
        CategoryFields(state, form, viewModel)
    }
    SwitchRow(
        stringResource(R.string.acq_dl_local),
        form.allowLocalNetwork,
        viewModel::setAllowLocalNetwork,
        hint = stringResource(R.string.acq_dl_local_hint),
    )
    SwitchRow(
        stringResource(R.string.acq_dl_writable),
        form.writable,
        viewModel::setWritable,
        hint = stringResource(if (form.writableLocked) R.string.acq_dl_writable_locked else R.string.acq_dl_writable_off),
        enabled = !form.writableLocked,
    )
    AfterFields(form, viewModel)
}

@Composable
private fun SignInFields(
    state: SettingsDownloadersState,
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    val kind = form.kindInfo ?: return
    val secretPlaceholder = if (form.hasSecret) "••••••••" else null
    val secretIssue = issueText(form, SettingsDownloaderIssue.SECRET)
    if (kind.signIn == SettingsDownloaderSignIn.QBITTORRENT) {
        Text(stringResource(R.string.acq_dl_sign_in_with), style = MaterialTheme.typography.labelLarge)
        SignInWay(form.login, viewModel::setLogin)
    }
    if (form.hasUsername) {
        Field(
            form.username,
            stringResource(R.string.acq_dl_username),
            viewModel::setUsername,
            hint =
                if (kind == SettingsDownloaderKind.QBITTORRENT) {
                    stringResource(R.string.acq_dl_qbit_user)
                } else {
                    stringResource(R.string.acq_dl_leave_empty, kind.appName)
                },
            error = issueText(form, SettingsDownloaderIssue.USERNAME),
        )
        Field(
            form.secret,
            stringResource(R.string.acq_dl_password),
            viewModel::setSecret,
            hint = stringResource(R.string.acq_dl_secret_kept),
            error = secretIssue,
            placeholder = secretPlaceholder,
            password = true,
        )
    } else {
        val key = kind == SettingsDownloaderKind.SABNZBD || kind == SettingsDownloaderKind.QBITTORRENT
        val label =
            when {
                key -> R.string.acq_dl_api_key
                kind == SettingsDownloaderKind.ARIA2 -> R.string.acq_dl_secret_token
                else -> R.string.acq_dl_password
            }
        val what =
            when (kind) {
                SettingsDownloaderKind.SABNZBD -> R.string.acq_dl_secret_sab
                SettingsDownloaderKind.QBITTORRENT -> R.string.acq_dl_secret_qbit
                SettingsDownloaderKind.ARIA2 -> R.string.acq_dl_secret_aria2
                else -> R.string.acq_dl_secret_deluge
            }
        Field(
            form.secret,
            stringResource(label),
            viewModel::setSecret,
            hint = stringResource(what) + " " + stringResource(R.string.acq_dl_secret_kept),
            error = secretIssue,
            placeholder = secretPlaceholder,
            password = true,
            mono = key,
        )
        if (kind == SettingsDownloaderKind.SABNZBD) KeyHelper(state, form, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInWay(
    login: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !login,
            onClick = { onChange(false) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
        ) { Text(stringResource(R.string.acq_dl_api_key), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        SegmentedButton(
            selected = login,
            onClick = { onChange(true) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
        ) { Text(stringResource(R.string.acq_dl_user_pass), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun KeyHelper(
    state: SettingsDownloadersState,
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    if (!form.canFetchKey) return
    val uri = LocalUriHandler.current
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
        if (state.canFetchKey) {
            OutlinedButton(onClick = viewModel::fetchKey, enabled = !state.key.fetching) {
                if (state.key.fetching) {
                    CircularProgressIndicator(
                        Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.acq_dl_fetch_key))
                }
            }
        }
        TextButton(onClick = { uri.openUri(form.keyPageUrl) }) { Text(stringResource(R.string.acq_dl_get_key)) }
    }
    if (state.key.loginShown) {
        Field(state.key.user, stringResource(R.string.acq_dl_sab_user), viewModel::setSabUser)
        Field(state.key.pass, stringResource(R.string.acq_dl_sab_pass), viewModel::setSabPass, password = true)
    }
    state.key.note?.let { Hint(downloaderKeyNoteText(it)) }
}

@Composable
private fun Expander(
    title: String,
    open: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun CategoryFields(
    state: SettingsDownloadersState,
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    val kind = form.kindInfo
    Field(
        form.category,
        stringResource(R.string.acq_dl_category),
        viewModel::setCategory,
        hint = downloaderCategoryHint(kind) + " " + stringResource(R.string.acq_dl_only_follows),
        error =
            issueText(
                form,
                SettingsDownloaderIssue.CATEGORY,
                SettingsDownloaderIssue.CATEGORY_LABEL,
                SettingsDownloaderIssue.CATEGORY_FOLDER,
            ),
    )
    Field(
        form.priority,
        stringResource(R.string.acq_dl_priority),
        viewModel::setPriority,
        hint = stringResource(R.string.acq_dl_priority_hint),
        error = issueText(form, SettingsDownloaderIssue.PRIORITY),
        numbers = true,
    )
    KindCategories(state, form, viewModel)
    TagsField(form, viewModel)
    if (kind == SettingsDownloaderKind.QBITTORRENT) {
        Field(
            form.categoryAfterImport,
            stringResource(R.string.acq_dl_category_after_import),
            viewModel::setCategoryAfterImport,
            hint = stringResource(R.string.acq_dl_category_after_import_hint),
            error = issueText(form, SettingsDownloaderIssue.AFTER_IMPORT, SettingsDownloaderIssue.AFTER_IMPORT_SAME),
        )
    }
    if (!form.usenet) {
        SwitchRow(
            stringResource(R.string.acq_dl_own_only),
            form.ownOnly,
            viewModel::setOwnOnly,
            hint = stringResource(R.string.acq_dl_own_only_hint),
        )
    }
    PathPairs(form, viewModel)
}

@Composable
private fun KindCategories(
    state: SettingsDownloadersState,
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    val fields =
        listOf(
            SettingsDownloaderKindField.MOVIES to R.string.acq_dl_category_movies,
            SettingsDownloaderKindField.SHOWS to R.string.acq_dl_category_shows,
            SettingsDownloaderKindField.MUSIC to R.string.acq_dl_category_music,
        )
    val kindIssue = issueText(form, SettingsDownloaderIssue.KIND_CATEGORY)
    when {
        form.isNew -> Hint(stringResource(R.string.acq_dl_categories_picked))
        state.categories.loading -> Unit
        state.categories.listed == true ->
            fields.forEach { (field, label) ->
                val current = form.kindCategory(field)
                val options =
                    listOf("" to stringResource(R.string.acq_dl_same_as_main, form.category)) +
                        state.categories.options(current).map { it to it }
                PickerField(
                    label = stringResource(label),
                    options = options,
                    selected = current,
                    onSelect = { viewModel.setKindCategory(field, it) },
                )
            }
        else ->
            fields.forEach { (field, label) ->
                Field(
                    form.kindCategory(field),
                    stringResource(label),
                    { viewModel.setKindCategory(field, it) },
                    hint = stringResource(R.string.acq_dl_category_kind_hint),
                    error = kindIssue,
                )
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsField(
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    Text(stringResource(R.string.acq_dl_tags), style = MaterialTheme.typography.labelLarge)
    form.tags.chunked(3).forEach { chunk ->
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            chunk.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { viewModel.removeTag(tag) },
                    label = { Text(tag) },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.acq_dl_tag_remove, tag),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Field(draft, stringResource(R.string.acq_dl_tag_add), {
            draft = it
        }, hint = stringResource(R.string.acq_dl_tags_hint), modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                viewModel.addTag(draft)
                draft = ""
            },
            enabled = draft.isNotBlank(),
        ) { Text(stringResource(R.string.acq_dl_tag_add_button)) }
    }
}

@Composable
private fun PathPairs(
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    Text(stringResource(R.string.acq_dl_folder_names), style = MaterialTheme.typography.titleSmall)
    Hint(stringResource(R.string.acq_dl_folder_names_hint))
    val pathIssue = issueText(form, SettingsDownloaderIssue.PATH)
    form.pathMap.forEachIndexed { index, pair ->
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Field(pair.theirs, stringResource(R.string.acq_dl_their_path), {
                    viewModel.setPathTheirs(index, it)
                }, placeholder = "/data/downloads", mono = true)
                Field(
                    pair.ours,
                    stringResource(R.string.acq_dl_our_path),
                    { viewModel.setPathOurs(index, it) },
                    placeholder = "/downloads",
                    mono = true,
                )
                SwitchRow(
                    stringResource(R.string.acq_dl_link_pair),
                    pair.link,
                    { viewModel.setPathLink(index, it) },
                    hint = stringResource(R.string.acq_dl_link_pair_hint),
                )
                if (samePathPair(pair)) Hint(stringResource(R.string.acq_dl_same_pair))
                TextButton(onClick = { viewModel.removePathPair(index) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.acq_dl_remove_pair), modifier = Modifier.padding(start = Spacing.Small))
                }
            }
        }
    }
    pathIssue?.let { FailText(it) }
    OutlinedButton(onClick = viewModel::addPathPair) { Text(stringResource(R.string.acq_dl_add_pair)) }
}

@Composable
private fun AfterFields(
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    val app = form.appName
    val fields = form.seedingFields
    HorizontalDivider()
    if (fields != SettingsDownloaderSeedingFields.NONE) {
        val goals = fields == SettingsDownloaderSeedingFields.GOALS
        Text(stringResource(R.string.acq_dl_after_seeding), style = MaterialTheme.typography.titleSmall)
        SwitchRow(
            stringResource(R.string.acq_dl_seed_remove),
            form.seedRemove,
            viewModel::setSeedRemove,
            hint =
                when {
                    !form.writable -> stringResource(R.string.acq_dl_seed_needs_writable)
                    goals -> stringResource(R.string.acq_dl_seed_remove_hint_goal, app)
                    else -> stringResource(R.string.acq_dl_seed_remove_hint, app)
                },
            enabled = form.writable,
        )
        if (form.seedRemove && form.writable) {
            if (goals) {
                Field(
                    form.seedRatio,
                    stringResource(R.string.acq_dl_seed_ratio),
                    viewModel::setSeedRatio,
                    hint = stringResource(R.string.acq_dl_seed_ratio_hint, app),
                    error = issueText(form, SettingsDownloaderIssue.RATIO),
                    numbers = true,
                )
                Field(
                    form.seedHours,
                    stringResource(R.string.acq_dl_seed_hours),
                    viewModel::setSeedHours,
                    hint = stringResource(R.string.acq_dl_seed_hours_hint),
                    error = issueText(form, SettingsDownloaderIssue.HOURS),
                    numbers = true,
                )
            }
            SwitchRow(
                stringResource(R.string.acq_dl_seed_delete),
                form.seedDeleteFiles,
                viewModel::setSeedDeleteFiles,
                hint = stringResource(R.string.acq_dl_seed_delete_hint),
            )
        }
    } else {
        Text(stringResource(R.string.acq_dl_after_import), style = MaterialTheme.typography.titleSmall)
        SwitchRow(
            stringResource(R.string.acq_dl_clear_finished),
            form.clearFinished,
            viewModel::setClearFinished,
            hint =
                if (form.writable) {
                    stringResource(
                        R.string.acq_dl_clear_finished_hint,
                        app,
                    )
                } else {
                    stringResource(R.string.acq_dl_seed_needs_writable)
                },
            enabled = form.writable,
        )
    }
}

@Composable
private fun FormBar(
    state: SettingsDownloadersState,
    form: SettingsDownloaderForm,
    viewModel: DownloadersViewModel,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            val result = state.formResult
            if (!form.isNew && result != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(
                        if (result.ok) {
                            result.tag.ifEmpty {
                                stringResource(
                                    R.string.acq_dl_connected,
                                )
                            }
                        } else {
                            stringResource(R.string.acq_dl_not_connected)
                        },
                        if (result.ok) Tone.OK else Tone.FAIL,
                    )
                }
                if (!result.ok) FailText(downloaderTestText(result))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End)) {
                TextButton(onClick = viewModel::cancelForm) { Text(stringResource(R.string.acq_dl_cancel)) }
                if (!form.isNew) {
                    OutlinedButton(onClick = viewModel::testForm, enabled = !state.formTesting) {
                        if (state.formTesting) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.acq_dl_test))
                        }
                    }
                }
                if (form.showFields) {
                    Button(onClick = viewModel::save, enabled = form.canSave && !state.saving) {
                        if (state.saving) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.acq_dl_save))
                        }
                    }
                }
            }
        }
    }
}
