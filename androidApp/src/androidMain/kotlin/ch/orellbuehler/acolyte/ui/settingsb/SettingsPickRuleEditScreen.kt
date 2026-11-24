package ch.orellbuehler.acolyte.ui.settingsb

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleEditState
import ch.orellbuehler.acolyte.feature.quality.SettingsQualityNotice
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.nav.Routes
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.NotAllowed
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.launch

@Composable
fun SettingsPickRuleEditScreen(
    id: Long,
    from: Long,
    account: Account,
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "quality-edit-$id-$from") { QualityEditViewModel(sdk, id, from) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var leave by remember { mutableStateOf(false) }
    val notice = state.notice
    val noticeText = if (notice != null) editNoticeText(notice) else ""
    LaunchedEffect(notice?.seq) {
        if (notice != null) {
            snackbar.showSnackbar(noticeText)
            viewModel.presenter.dismissNotice(notice.seq)
        }
    }
    BackHandler(enabled = state.dirty) { leave = true }
    val cards =
        buildList {
            add("head")
            add("name")
            add("steps")
            add("traits")
            if (state.showTrash && (state.mayEdit || state.form.synced != null)) add("trash")
            add("must")
            add("size")
            add("waiting")
            if (!state.form.music) add("sources")
            add("language")
        }
    val save: () -> Unit = {
        scope.launch {
            viewModel.save()
            val index = cards.indexOf(qualityCardFor(viewModel.state.value.firstProblem))
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }
    ScreenScaffold(
        title =
            when {
                state.isNew -> stringResource(R.string.acq_q_l_add)
                state.savedName.isNotEmpty() -> state.savedName
                else -> stringResource(R.string.acq_page_quality)
            },
        onBack = { if (state.dirty) leave = true else onBack() },
        actions = {
            if (state.ready && !state.isNew && state.mayEdit) {
                TextButton(onClick = { onOpen(Routes.settingsPickRuleEdit(0, state.id)) }) {
                    Text(stringResource(R.string.acq_q_e_copy))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.allowed -> NotAllowed()
                state.gone ->
                    MessageView(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.acq_q_e_not_found),
                        message = stringResource(R.string.acq_q_e_not_found_text),
                        action = stringResource(R.string.acq_q_e_all),
                        onAction = onBack,
                    )
                state.needsPermission ->
                    MessageView(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.acq_q_e_needs_permission),
                        message = stringResource(R.string.acq_q_e_needs_permission_text),
                        action = stringResource(R.string.acq_q_e_all),
                        onAction = onBack,
                    )
                else ->
                    StatusContent(
                        status = state.status,
                        isEmpty = false,
                        onRefresh = { if (!state.dirty) viewModel.refresh() },
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                        ) {
                            items(cards, key = { it }) { card -> EditCard(card, state, viewModel, onOpen) }
                        }
                    }
            }
            if (state.showSaveBar) QualitySaveBar(state, viewModel, save, Modifier.align(Alignment.BottomCenter))
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (state.showSaveBar) 96.dp else 0.dp))
        }
    }
    if (leave) {
        AlertDialog(
            onDismissRequest = { leave = false },
            title = { Text(stringResource(R.string.acq_q_e_leave_title)) },
            text = { Text(stringResource(R.string.acq_q_e_leave_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        leave = false
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.acq_q_e_leave_discard), color = MaterialTheme.acolyte.failText)
                }
            },
            dismissButton = { TextButton(onClick = { leave = false }) { Text(stringResource(R.string.acq_q_e_leave_stay)) } },
        )
    }
}

@Composable
private fun EditCard(
    card: String,
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
    onOpen: (Any) -> Unit,
) {
    when (card) {
        "head" -> EditHead(state)
        "name" -> QualityNameCard(state, viewModel)
        "steps" -> QualityStepsCard(state, viewModel)
        "traits" -> QualityTraitsEditCard(state, viewModel, onOpen)
        "trash" -> QualityTrashCard(state, viewModel)
        "must" -> QualityMustNeverCard(state, viewModel)
        "size" -> QualitySizeCard(state, viewModel)
        "waiting" -> QualityWaitingCard(state, viewModel)
        "sources" -> QualitySourcesCard(state, viewModel)
        "language" -> QualityLanguageCard(state, viewModel)
    }
}

@Composable
private fun EditHead(state: SettingsPickRuleEditState) {
    val subtitle =
        when {
            !state.ready -> ""
            state.isNew -> stringResource(R.string.acq_q_e_subtitle)
            state.use.total > 0 -> stringResource(R.string.acq_q_e_used_line, qualityUsedBy(state.use))
            else -> stringResource(R.string.acq_q_not_used)
        }
    Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.mayEdit) Hint(stringResource(R.string.acq_q_l_read_only))
    }
}

@Composable
private fun QualitySaveBar(
    state: SettingsPickRuleEditState,
    viewModel: QualityEditViewModel,
    onSave: () -> Unit,
    modifier: Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.Medium).navigationBarsPadding()) {
            val error = state.formError
            val use = state.formUse
            if (use != null) {
                FailText(stringResource(R.string.acq_q_e_in_use_error))
                Hint(qualityUsedBy(use))
            } else if (error != null) {
                FailText(errorText(error))
                val serverCoded =
                    error.code == ApiErrorCode.BAD_REQUEST || error.code == ApiErrorCode.CONFLICT || error.code == ApiErrorCode.SERVER
                if (serverCoded && error.message.isNotBlank()) Hint(error.message)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = viewModel.presenter::discard, enabled = !state.saving) {
                    Text(stringResource(R.string.acq_q_e_discard))
                }
                Button(onClick = onSave, enabled = !state.saving) {
                    if (state.saving) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.acq_q_e_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun editNoticeText(notice: SettingsQualityNotice): String =
    when (notice.kind) {
        "made" -> stringResource(R.string.acq_q_e_made, notice.name)
        "saved" -> stringResource(R.string.acq_q_e_saved)
        "applied" -> {
            val name = notice.name.ifEmpty { stringResource(R.string.acq_q_e_trash_profile_fallback) }
            stringResource(R.string.acq_q_e_trash_applied, name) + " · " + stringResource(R.string.acq_q_e_trash_scored, notice.count)
        }
        else -> ""
    }
