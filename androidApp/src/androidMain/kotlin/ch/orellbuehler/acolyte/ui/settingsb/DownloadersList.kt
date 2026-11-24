package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderAttempt
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.acquisition.downloaders.SettingsDownloaderRow
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.relativeTime
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun EmptyDownloaders(
    canEdit: Boolean,
    onAdd: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.XLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Icon(
            Icons.Rounded.Dns,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(stringResource(R.string.acq_dl_none), style = MaterialTheme.typography.titleMedium)
        Hint(stringResource(R.string.acq_dl_none_text))
        if (canEdit) Button(onClick = onAdd) { Text(stringResource(R.string.acq_dl_add)) }
    }
}

@Composable
fun DownloaderCard(
    row: SettingsDownloaderRow,
    canEdit: Boolean,
    viewModel: DownloadersViewModel,
) {
    var menu by remember { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf(false) }
    var abandoning by remember { mutableStateOf<SettingsDownloaderAttempt?>(null) }
    val uri = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                KindBadge(row)
                Column(Modifier.weight(1f)) {
                    Text(row.info.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(
                            R.string.acq_dl_sub,
                            row.appName,
                            stringResource(if (row.usenet) R.string.acq_dl_usenet else R.string.acq_dl_torrents),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (row.showHealthWord) StatusChip(downloaderHealthWord(row), row.tone)
            }
            Hint(stringResource(R.string.acq_dl_detail, row.host, row.info.priority, row.info.category))
            downloaderFlags(row)?.let { Hint(it) }
            if (row.info.activeDownloads > 0) {
                val checked =
                    row.info.checkedAt
                        .takeIf { it.isNotEmpty() }
                        ?.let { relativeTime(it) }
                Hint(
                    stringResource(R.string.acq_dl_active, row.info.activeDownloads) +
                        (checked?.let { " · " + stringResource(R.string.acq_dl_checked, it) } ?: ""),
                )
            }
            val test = row.test
            if (test != null) {
                StatusChip(
                    if (test.ok) {
                        test.tag.ifEmpty {
                            stringResource(
                                R.string.acq_dl_connected,
                            )
                        }
                    } else {
                        stringResource(R.string.acq_dl_not_connected)
                    },
                    if (test.ok) Tone.OK else Tone.FAIL,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                FilledTonalButton(onClick = { viewModel.test(row.id) }, enabled = !row.testing && !row.busy) {
                    if (row.testing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.acq_dl_test))
                    }
                }
                if (canEdit) {
                    Box {
                        IconButton(onClick = { menu = true }, enabled = !row.busy) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.acq_dl_more, row.info.name))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (row.paused) R.string.acq_dl_resume else R.string.acq_dl_pause)) },
                                onClick = {
                                    menu = false
                                    if (row.paused) viewModel.resume(row.id) else viewModel.pause(row.id)
                                },
                            )
                            if (row.canChange) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.acq_dl_change)) },
                                    onClick = {
                                        menu = false
                                        viewModel.change(row.id)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.acq_dl_remove), color = MaterialTheme.acolyte.failText) },
                                onClick = {
                                    menu = false
                                    removing = true
                                },
                            )
                        }
                    }
                }
            }
            if (row.showHealthText) {
                val text = row.info.health.text
                if (row.healthTextFails) FailText(text) else Hint(text)
            }
            if (test != null && !test.ok) {
                FailText(downloaderTestText(test))
                if (row.helpUrl.isNotEmpty()) {
                    TextButton(
                        onClick = { uri.openUri(row.helpUrl) },
                    ) { Text(stringResource(R.string.acq_dl_connect_help)) }
                }
                if (test.category.isNotEmpty()) {
                    if (test.canMake && canEdit) {
                        OutlinedButton(onClick = { viewModel.makeCategory(row.id, test.category) }, enabled = !row.busy) {
                            Text(stringResource(R.string.acq_dl_make_category, test.category))
                        }
                    } else if (test.makeOff) {
                        Hint(stringResource(R.string.acq_dl_make_off))
                    }
                }
            }
            if (row.pending.isNotEmpty()) {
                AttemptsPanel(row, canEdit) { abandoning = it }
            } else {
                row.attemptError?.let { FailText(stringResource(R.string.acq_dl_attempts_unchecked, downloaderErrorText(it))) }
            }
        }
    }
    if (removing) {
        ConfirmDialog(
            title = stringResource(R.string.acq_dl_confirm_remove_title, row.info.name),
            text = stringResource(R.string.acq_dl_confirm_remove_text),
            confirm = stringResource(R.string.acq_dl_remove),
            dismiss = stringResource(R.string.acq_dl_cancel),
            danger = true,
            onConfirm = {
                removing = false
                viewModel.remove(row.id)
            },
            onDismiss = { removing = false },
        )
    }
    abandoning?.let { attempt ->
        ConfirmDialog(
            title = stringResource(R.string.acq_dl_abandon_title),
            text = stringResource(R.string.acq_dl_abandon_text, attempt.releaseId, attempt.slotId, row.info.name, row.appName),
            confirm = stringResource(R.string.acq_dl_stop),
            dismiss = stringResource(R.string.acq_dl_cancel),
            danger = true,
            onConfirm = {
                abandoning = null
                viewModel.abandon(row.id, attempt.id)
            },
            onDismiss = { abandoning = null },
        )
    }
}

@Composable
private fun downloaderFlags(row: SettingsDownloaderRow): String? {
    val parts = mutableListOf<String>()
    if (row.info.tags.isNotEmpty()) parts += stringResource(R.string.acq_dl_tags_list, row.info.tags.joinToString(", "))
    if (row.info.allowLocalNetwork) parts += stringResource(R.string.acq_dl_local_reach)
    parts +=
        stringResource(
            when {
                row.locked -> R.string.acq_dl_read_only
                row.handsOff -> R.string.acq_dl_hands_off
                else -> R.string.acq_dl_can_change
            },
        )
    return parts.joinToString(" · ")
}

@Composable
private fun KindBadge(row: SettingsDownloaderRow) {
    Box(
        Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(row.kind?.badge ?: row.appName.take(2), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AttemptsPanel(
    row: SettingsDownloaderRow,
    canEdit: Boolean,
    onGiveUp: (SettingsDownloaderAttempt) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(
                stringResource(R.string.acq_dl_attempt_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                stringResource(R.string.acq_dl_attempt_text, row.appName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            row.pending.forEach { attempt ->
                val ago = relativeTime(attempt.createdAt).orEmpty()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        stringResource(R.string.acq_dl_attempt_line, attempt.releaseId, attempt.slotId, ago),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    if (canEdit) {
                        TextButton(onClick = { onGiveUp(attempt) }, enabled = row.abandoning == 0L) {
                            if (row.abandoning == attempt.id) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.acq_dl_give_up))
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.acq_dl_attempt_footer, row.appName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
