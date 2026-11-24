package ch.orellbuehler.acolyte.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.account.ACCOUNT_LANGUAGES
import ch.orellbuehler.acolyte.feature.account.AccountProblem
import ch.orellbuehler.acolyte.feature.account.AccountSaved
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.util.Locale

fun languageName(code: String): String =
    if (code.isEmpty()) "" else Locale.forLanguageTag(code).let { it.getDisplayName(it).replaceFirstChar { c -> c.uppercase() } }

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val vm = viewModel { AccountProfileViewModel(sdk) }
    val state by vm.state.collectAsStateWithLifecycle()
    val account = currentAccount()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) scope.launch { loadSquareJpeg(context, uri)?.let { vm.upload(it) } }
        }
    val savedText =
        when (state.saved) {
            AccountSaved.NAME -> stringResource(R.string.myaccount_saved_name)
            AccountSaved.PICTURE -> stringResource(R.string.myaccount_saved_picture)
            AccountSaved.PICTURE_REMOVED -> stringResource(R.string.myaccount_saved_picture_removed)
            AccountSaved.LANGUAGE -> stringResource(R.string.myaccount_saved_language)
            else -> null
        }
    ToastOnce(savedText) { vm.consume() }

    ScreenScaffold(title = stringResource(R.string.myaccount_profile), onBack = onBack) { padding ->
        StatusContent(state.status, isEmpty = false, onRefresh = { vm.refresh() }, modifier = Modifier.padding(padding)) {
            FormColumn(PaddingValuesZero) {
                state.actionError?.let { ErrorBanner(it) }
                state.problem?.let {
                    val text =
                        when (it) {
                            AccountProblem.NAME_INVALID -> R.string.myaccount_name_invalid
                            AccountProblem.PICTURE_TOO_BIG -> R.string.myaccount_picture_big
                            AccountProblem.PICTURE_EMPTY -> R.string.myaccount_picture_empty
                        }
                    ErrorBannerText(stringResource(text))
                }
                FormCard(stringResource(R.string.myaccount_profile)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                        ServerImage(
                            source = account?.avatarPath,
                            contentDescription = null,
                            size = ImageSize.THUMB,
                            placeholderName = state.profile.displayName.ifEmpty { state.profile.username },
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                        )
                        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            OutlinedButton(
                                enabled = !state.busy,
                                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            ) {
                                Text(
                                    stringResource(
                                        if (account?.avatarPath !=
                                            null
                                        ) {
                                            R.string.myaccount_picture_change
                                        } else {
                                            R.string.myaccount_picture_add
                                        },
                                    ),
                                )
                            }
                            if (account?.avatarPath != null) {
                                TextButton(enabled = !state.busy, onClick = { vm.removePicture() }) {
                                    Text(stringResource(R.string.myaccount_picture_remove))
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.myaccount_picture_hint),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                    HorizontalDivider()
                    if (editing) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(64) },
                            label = { Text(stringResource(R.string.myaccount_display_name)) },
                            placeholder = { Text(state.profile.username) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            Button(enabled = !state.busy, onClick = {
                                vm.saveName(name)
                                editing = false
                            }) { Text(stringResource(R.string.myaccount_save)) }
                            TextButton(onClick = { editing = false }) { Text(stringResource(R.string.common_cancel)) }
                        }
                    } else {
                        LabeledRow(stringResource(R.string.myaccount_display_name)) {
                            Text(state.profile.displayName.ifEmpty { state.profile.username })
                            TextButton(onClick = {
                                name = state.profile.displayName
                                editing = true
                            }) { Text(stringResource(R.string.myaccount_change)) }
                        }
                    }
                    LabeledRow(stringResource(R.string.myaccount_username)) { Text(state.profile.username) }
                }
                FormCard(stringResource(R.string.myaccount_preferences)) {
                    LabeledRow(stringResource(R.string.myaccount_language), Modifier.clickable { picking = true }) {
                        Text(
                            if (state.profile.language.isEmpty()) {
                                stringResource(R.string.myaccount_language_auto)
                            } else {
                                languageName(state.profile.language)
                            },
                        )
                    }
                    Text(
                        stringResource(R.string.myaccount_language_hint),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.myaccount_language)) },
            text = {
                LazyColumn(Modifier.height(380.dp)) {
                    items(listOf("") + ACCOUNT_LANGUAGES) { code ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    picking = false
                                    vm.saveLanguage(code)
                                }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.profile.language == code, onClick = null)
                            Text(
                                if (code.isEmpty()) stringResource(R.string.myaccount_language_auto) else languageName(code),
                                modifier = Modifier.padding(start = Spacing.Medium),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
