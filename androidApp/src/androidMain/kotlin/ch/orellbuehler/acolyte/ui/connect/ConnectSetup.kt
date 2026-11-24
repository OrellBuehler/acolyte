package ch.orellbuehler.acolyte.ui.connect

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.session.AccountRules
import ch.orellbuehler.acolyte.session.SignInState
import ch.orellbuehler.acolyte.ui.account.followDisplayName
import ch.orellbuehler.acolyte.ui.account.loadSquareJpeg
import ch.orellbuehler.acolyte.ui.account.passwordMismatch
import ch.orellbuehler.acolyte.ui.account.passwordReady
import ch.orellbuehler.acolyte.ui.account.passwordTooShort
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.PasswordField
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
internal fun SetupStep(
    state: SignInState,
    viewModel: ConnectViewModel,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var displayEdited by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val shownName = followDisplayName(name, displayName, displayEdited)
    val ready =
        AccountRules.validName(name) &&
            AccountRules.validDisplayName(shownName) &&
            passwordReady(password, confirm, state.minPasswordLength)
    val submit = { if (ready && !state.busy) viewModel.createOwner(name, shownName, password) }

    Heading(stringResource(R.string.signin_setup_welcome_title), stringResource(R.string.signin_setup_welcome_lead))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        listOf(
            R.string.signin_setup_feature_find,
            R.string.signin_setup_feature_sort,
            R.string.signin_setup_feature_requests,
            R.string.signin_setup_feature_subtitles,
        ).forEach { feature ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(feature), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(state.serverName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false), maxLines = 1)
        TextButton(onClick = viewModel::back, enabled = !state.busy) { Text(stringResource(R.string.connect_change_server)) }
    }
    Text(stringResource(R.string.signin_setup_account_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Text(
        stringResource(R.string.signin_setup_account_lead),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    if (state.configUnmapped) NoticeBanner(stringResource(R.string.signin_setup_config_unmapped))
    state.hostRefused?.let { NoticeBanner(stringResource(R.string.signin_host_refused, it)) }
    PicturePicker(viewModel, enabled = !state.busy)
    OutlinedTextField(
        value = name,
        onValueChange = { name = AccountRules.cleanName(it) },
        label = { Text(stringResource(R.string.signin_setup_username)) },
        supportingText = { Text(stringResource(R.string.signin_setup_username_hint)) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = shownName,
        onValueChange = {
            displayName = it.take(AccountRules.MAX_DISPLAY_NAME)
            displayEdited = true
        },
        label = { Text(stringResource(R.string.signin_setup_display_name)) },
        supportingText = { Text(stringResource(R.string.signin_setup_display_name_hint)) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.signin_password),
        placeholder = stringResource(R.string.signin_setup_password_hint, state.minPasswordLength),
        error =
            if (passwordTooShort(
                    password,
                    state.minPasswordLength,
                )
            ) {
                stringResource(R.string.signin_setup_password_short, state.minPasswordLength)
            } else {
                null
            },
        enabled = !state.busy,
    )
    PasswordField(
        value = confirm,
        onValueChange = { confirm = it },
        label = stringResource(R.string.signin_setup_confirm),
        error = if (passwordMismatch(password, confirm)) stringResource(R.string.signin_setup_mismatch) else null,
        enabled = !state.busy,
        imeAction = ImeAction.Done,
        onDone = submit,
    )
    BusyButton(stringResource(R.string.signin_setup_create), state.busy, enabled = ready) { submit() }
}

@Composable
private fun PicturePicker(
    viewModel: ConnectViewModel,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picture by viewModel.picture.collectAsStateWithLifecycle()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) scope.launch { viewModel.setPicture(loadSquareJpeg(context, uri)) }
        }
    val bitmap = remember(picture) { picture?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val pick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(enabled = enabled, onClick = pick),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap,
                    contentDescription = stringResource(R.string.signin_setup_photo_change),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp),
                )
            } else {
                Icon(Icons.Rounded.AddAPhoto, contentDescription = stringResource(R.string.signin_setup_photo_add))
            }
        }
        if (bitmap == null) {
            TextButton(onClick = pick, enabled = enabled) { Text(stringResource(R.string.signin_setup_photo_add)) }
        } else {
            TextButton(
                onClick = { viewModel.setPicture(null) },
                enabled = enabled,
            ) { Text(stringResource(R.string.signin_setup_photo_remove)) }
        }
    }
}

@Composable
internal fun ResetCodeStep(
    state: SignInState,
    viewModel: ConnectViewModel,
    code: String,
    onCode: (String) -> Unit,
) {
    Heading(stringResource(R.string.signin_reset_title), stringResource(R.string.signin_reset_lead))
    BusyButton(stringResource(R.string.signin_reset_make_code), state.busy, enabled = true) { viewModel.makeResetCode() }
    if (state.resetCodeMade) {
        Text(
            stringResource(R.string.signin_reset_made),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    OutlinedTextField(
        value = code,
        onValueChange = { onCode(it.take(64).uppercase()) },
        label = { Text(stringResource(R.string.signin_reset_code)) },
        placeholder = { Text("XXXX-XXXX-XXXX-XXXX-XXXX-XXXX", maxLines = 1) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { if (code.isNotBlank()) viewModel.checkResetCode(code) }),
        modifier = Modifier.fillMaxWidth(),
    )
    BusyButton(stringResource(R.string.signin_reset_next), state.busy, enabled = code.isNotBlank()) { viewModel.checkResetCode(code) }
    Text(
        stringResource(R.string.signin_reset_where_log),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    TextButton(onClick = viewModel::back, enabled = !state.busy) { Text(stringResource(R.string.signin_reset_back)) }
}

@Composable
internal fun ResetPasswordStep(
    state: SignInState,
    viewModel: ConnectViewModel,
    code: String,
) {
    var name by rememberSaveable(state.resetName) { mutableStateOf(state.resetName) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val ready = AccountRules.validName(name) && passwordReady(password, confirm, state.minPasswordLength)
    val submit = { if (ready && !state.busy) viewModel.finishReset(code, name, password) }
    Heading(stringResource(R.string.signin_reset_new_title), stringResource(R.string.signin_reset_new_lead))
    OutlinedTextField(
        value = name,
        onValueChange = { name = AccountRules.cleanName(it) },
        label = { Text(stringResource(R.string.signin_reset_owner_name)) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.signin_reset_new_password),
        placeholder = stringResource(R.string.signin_setup_password_hint, state.minPasswordLength),
        error =
            if (passwordTooShort(
                    password,
                    state.minPasswordLength,
                )
            ) {
                stringResource(R.string.signin_setup_password_short, state.minPasswordLength)
            } else {
                null
            },
        enabled = !state.busy,
    )
    PasswordField(
        value = confirm,
        onValueChange = { confirm = it },
        label = stringResource(R.string.signin_setup_confirm),
        error = if (passwordMismatch(password, confirm)) stringResource(R.string.signin_setup_mismatch) else null,
        enabled = !state.busy,
        imeAction = ImeAction.Done,
        onDone = submit,
    )
    BusyButton(stringResource(R.string.signin_reset_finish), state.busy, enabled = ready) { submit() }
    TextButton(onClick = viewModel::back, enabled = !state.busy) { Text(stringResource(R.string.signin_reset_back)) }
}
