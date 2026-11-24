package ch.orellbuehler.acolyte.ui.settings.keys

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsKeysNotice
import ch.orellbuehler.acolyte.feature.settings.keys.SettingsKeysNoticeKind
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun settingsKeysDate(iso: String): String =
    try {
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        iso.take(10)
    }

@Composable
fun keyUsageText(
    createdAt: String,
    lastUsedAt: String?,
): String {
    val made = settingsKeysDate(createdAt)
    val used = lastUsedAt?.let { relativeTime(it) }
    return if (used != null) {
        stringResource(R.string.settings_keys_made_used, made, used)
    } else {
        stringResource(R.string.settings_keys_made_never, made)
    }
}

@Composable
fun rememberKeysCopy(snackbar: SnackbarHostState): (String, String) -> Unit {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, snackbar, scope) {
        { text: String, done: String ->
            clipboard.setText(AnnotatedString(text))
            scope.launch { snackbar.showSnackbar(done) }
        }
    }
}

@Composable
fun FreshKeyBox(
    title: String,
    text: String,
    secret: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium)
            SelectionContainer {
                Text(secret, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onCopy) { Text(stringResource(R.string.settings_keys_copy)) }
                OutlinedButton(onClick = onShare) { Text(stringResource(R.string.settings_keys_share)) }
                TextButton(onClick = onClose) { Text(stringResource(R.string.settings_keys_close)) }
            }
        }
    }
}

@Composable
fun CopyBlock(
    value: String,
    copyDescription: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            SelectionContainer(Modifier.weight(1f).padding(vertical = Spacing.Small)) {
                Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            }
            IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, contentDescription = copyDescription) }
        }
    }
}

@Composable
fun GroupsChecklist(
    groups: List<String>,
    selected: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        groups.forEach { group ->
            val checked = group in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle(group) })
                    .padding(vertical = Spacing.XSmall),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                Column {
                    Text(groupLabel(group), style = MaterialTheme.typography.bodyLarge)
                    groupHint(group)?.let { Hint(it) }
                }
            }
        }
    }
}

@Composable
fun groupLabel(group: String): String =
    stringResource(
        when (group) {
            "see" -> R.string.settings_keys_group_see
            "add" -> R.string.settings_keys_group_add
            "control" -> R.string.settings_keys_group_control
            "settings" -> R.string.settings_keys_group_settings
            "delete" -> R.string.settings_keys_group_delete
            "admin" -> R.string.settings_keys_group_admin
            "approve" -> R.string.settings_keys_group_approve
            else -> R.string.settings_keys_short_plugins
        },
    )

@Composable
fun groupHint(group: String): String? =
    when (group) {
        "admin" -> stringResource(R.string.settings_keys_group_admin_hint)
        "approve" -> stringResource(R.string.settings_keys_group_approve_hint)
        else -> null
    }

@Composable
fun groupShort(group: String): String =
    stringResource(
        when (group) {
            "see" -> R.string.settings_keys_short_see
            "add" -> R.string.settings_keys_short_add
            "control" -> R.string.settings_keys_short_control
            "settings" -> R.string.settings_keys_short_settings
            "delete" -> R.string.settings_keys_short_delete
            "admin" -> R.string.settings_keys_short_admin
            "approve" -> R.string.settings_keys_short_approve
            else -> R.string.settings_keys_short_plugins
        },
    )

@Composable
fun settingsKeysNoticeText(notice: SettingsKeysNotice): String {
    val base =
        when (notice.kind) {
            SettingsKeysNoticeKind.KEY_REVOKED -> stringResource(R.string.settings_keys_notice_revoked, notice.name)
            SettingsKeysNoticeKind.KEY_REVOKE_FAILED -> stringResource(R.string.settings_keys_notice_revoke_failed)
            SettingsKeysNoticeKind.KEY_MAKE_FAILED, SettingsKeysNoticeKind.SEERR_KEY_MAKE_FAILED ->
                stringResource(R.string.settings_keys_notice_make_failed)
            SettingsKeysNoticeKind.SEERR_SAVE_FAILED, SettingsKeysNoticeKind.AI_SAVE_FAILED ->
                stringResource(R.string.settings_keys_notice_save_failed)
            SettingsKeysNoticeKind.SEERR_KEY_REMOVED -> stringResource(R.string.settings_keys_notice_seerr_removed)
            SettingsKeysNoticeKind.SEERR_KEY_REMOVE_FAILED -> stringResource(R.string.settings_keys_notice_seerr_remove_failed)
            SettingsKeysNoticeKind.AI_ON -> stringResource(R.string.settings_keys_notice_ai_on)
            SettingsKeysNoticeKind.AI_OFF -> stringResource(R.string.settings_keys_notice_ai_off)
            SettingsKeysNoticeKind.GROUPS_SAVED -> stringResource(R.string.settings_keys_notice_groups_saved, notice.name)
        }
    val error = notice.error ?: return base
    return listOf(base, errorText(error), notice.detail).filter { it.isNotBlank() }.joinToString("\n")
}

@Composable
fun KeysNoticeHost(
    notice: SettingsKeysNotice?,
    snackbar: SnackbarHostState,
    onDismiss: (Int) -> Unit,
) {
    val text = notice?.let { settingsKeysNoticeText(it) }
    LaunchedEffect(notice?.seq) {
        if (notice != null && text != null) {
            snackbar.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
            onDismiss(notice.seq)
        }
    }
}
