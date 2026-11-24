package ch.orellbuehler.acolyte.ui.settings.look

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing

private val SERVER_LINE_STATUSES = setOf(400, 409, 422, 502, 503)

@Composable
fun LookErrorText(
    error: ApiError,
    modifier: Modifier = Modifier,
    prefixed: Boolean = false,
) {
    val line = errorText(error)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        FailText(if (prefixed) stringResource(R.string.settings_look_not_saved, line) else line)
        if (error.status in SERVER_LINE_STATUSES && error.code != ApiErrorCode.PART_OFF && error.message.isNotBlank()) {
            Hint(stringResource(R.string.settings_look_server_says, error.message))
        }
    }
}

@Composable
fun LookSaveBar(
    canSave: Boolean,
    saving: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    error: ApiError? = null,
    problem: String? = null,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            if (error != null) LookErrorText(error)
            if (problem != null) FailText(problem)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_look_unsaved),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onDiscard, enabled = !saving) { Text(stringResource(R.string.settings_look_discard)) }
                Button(onClick = onSave, enabled = canSave) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.settings_look_save))
                    }
                }
            }
        }
    }
}

@Composable
fun LookLeaveGuard(
    dirty: Boolean,
    onBack: () -> Unit,
    content: @Composable (back: () -> Unit) -> Unit,
) {
    var asking by remember { mutableStateOf(false) }
    BackHandler(enabled = dirty) { asking = true }
    content { if (dirty) asking = true else onBack() }
    if (asking) {
        ConfirmDialog(
            title = stringResource(R.string.settings_look_leave_title),
            text = stringResource(R.string.settings_look_leave_text),
            confirm = stringResource(R.string.settings_look_leave_confirm),
            dismiss = stringResource(R.string.settings_look_leave_stay),
            onConfirm = {
                asking = false
                onBack()
            },
            onDismiss = { asking = false },
            danger = true,
        )
    }
}
