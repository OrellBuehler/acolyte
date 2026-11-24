package ch.orellbuehler.acolyte.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.LoadingView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.ReauthSheet
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun AiConnectScreen(onBack: () -> Unit) {
    val sdk = LocalSdk.current
    val vm = viewModel { AiConnectViewModel(sdk) }
    val account = currentAccount()
    var link by rememberSaveable { mutableStateOf("") }
    val presenter = vm.presenter
    ScreenScaffold(title = stringResource(R.string.aiconnect_title), onBack = onBack) { padding ->
        FormColumn(padding) {
            if (account == null || !account.can(Need.SETTINGS)) {
                NoticeBanner(stringResource(R.string.aiconnect_staff_only))
                return@FormColumn
            }
            if (presenter == null) {
                Text(stringResource(R.string.aiconnect_intro), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text(stringResource(R.string.aiconnect_link)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(enabled = link.contains("?"), onClick = { vm.start(link) }) { Text(stringResource(R.string.aiconnect_continue)) }
            } else {
                AiConnectCard(vm, onBack)
            }
        }
    }
    presenter?.let { ReauthSheet(it.reauth) }
}

@Composable
private fun AiConnectCard(
    vm: AiConnectViewModel,
    onBack: () -> Unit,
) {
    val presenter = vm.presenter ?: return
    val state by presenter.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    val redirect = state.redirect
    LaunchedEffect(redirect) {
        if (redirect != null) runCatching { uri.openUri(redirect) }
    }
    val info = state.info
    when {
        !state.valid -> NoticeBanner(stringResource(R.string.aiconnect_invalid))
        state.status.phase == Phase.LOADING -> LoadingView()
        state.status.phase == Phase.FAILED -> {
            state.status.error?.let { ErrorBanner(it) }
            Text(stringResource(R.string.aiconnect_start_again), style = MaterialTheme.typography.bodyMedium)
        }
        redirect != null -> {
            Text(stringResource(R.string.aiconnect_connected, info.clientName), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.aiconnect_connected_hint, info.clientName), style = MaterialTheme.typography.bodyMedium)
        }
        else -> {
            Text(stringResource(R.string.aiconnect_wants, info.returnsTo), style = MaterialTheme.typography.titleMedium)
            val says = stringResource(R.string.aiconnect_says, info.clientName)
            Text(
                if (info.nameVerified) says else says + " · " + stringResource(R.string.aiconnect_not_verified),
                style = MaterialTheme.typography.bodyMedium,
            )
            listOf(R.string.aiconnect_can_1, R.string.aiconnect_can_2, R.string.aiconnect_can_3, R.string.aiconnect_history).forEach {
                Text("• " + stringResource(it), style = MaterialTheme.typography.bodyMedium)
            }
            if (info.local) Text(stringResource(R.string.aiconnect_local), style = MaterialTheme.typography.bodyMedium)
            if (!info.enabled) NoticeBanner(stringResource(R.string.aiconnect_off))
            state.error?.let { ErrorBanner(it) }
            Row(Modifier.padding(top = Spacing.Small), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Button(enabled = state.canAllow, onClick = { vm.allow() }) { Text(stringResource(R.string.aiconnect_allow)) }
                OutlinedButton(onClick = {
                    if (info.denyUrl.isNotEmpty()) runCatching { uri.openUri(info.denyUrl) }
                    onBack()
                }) { Text(stringResource(R.string.common_cancel)) }
            }
        }
    }
}
