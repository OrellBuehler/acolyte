package ch.orellbuehler.acolyte.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInEveryoneAsk
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInEveryoneWho
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInNoticeKind
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInWayKind
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel
import ch.orellbuehler.acolyte.ui.nav.SettingsApiRoute
import ch.orellbuehler.acolyte.ui.requests.ConfirmDialog
import ch.orellbuehler.acolyte.ui.settings.signin.AdvancedHeader
import ch.orellbuehler.acolyte.ui.settings.signin.ApiKeysRow
import ch.orellbuehler.acolyte.ui.settings.signin.ForgotCard
import ch.orellbuehler.acolyte.ui.settings.signin.HostsCard
import ch.orellbuehler.acolyte.ui.settings.signin.ImportSheet
import ch.orellbuehler.acolyte.ui.settings.signin.InlineNotice
import ch.orellbuehler.acolyte.ui.settings.signin.LocalCard
import ch.orellbuehler.acolyte.ui.settings.signin.PasswordsCard
import ch.orellbuehler.acolyte.ui.settings.signin.ProxiesInfoCard
import ch.orellbuehler.acolyte.ui.settings.signin.SignInModel
import ch.orellbuehler.acolyte.ui.settings.signin.SsoCard
import ch.orellbuehler.acolyte.ui.settings.signin.TwoFactorCard
import ch.orellbuehler.acolyte.ui.settings.signin.WayCard
import ch.orellbuehler.acolyte.ui.settings.signin.noticeMessage
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SettingsSignInScreen(
    onBack: () -> Unit,
    onOpen: (Any) -> Unit,
) {
    val sdk = LocalSdk.current
    val model: SignInModel = viewModel(key = "settings-signin") { PresenterViewModel(sdk.settingsSignIn()) { load() } }
    val state by model.presenter.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    var opened by remember { mutableIntStateOf(state.plex.openCount) }
    LaunchedEffect(state.plex.openCount) {
        if (state.plex.openCount > opened) {
            opened = state.plex.openCount
            runCatching { uriHandler.openUri(state.plex.authUrl) }
        }
    }
    val notice = state.notice
    val message = notice?.let { noticeMessage(it) }
    LaunchedEffect(notice?.seq) {
        if (notice != null && message != null) {
            val long = notice.kind == SettingsSignInNoticeKind.WAY_UNREACHABLE
            snackbar.showSnackbar(message, duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short)
            model.presenter.dismissNotice(notice.seq)
        }
    }

    ScreenScaffold(title = stringResource(R.string.settings_page_sign_in), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            StatusContent(status = state.status, isEmpty = false, onRefresh = { model.run { refresh() } }) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.XLarge),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    state.status.error?.let { error -> item(key = "error") { ErrorBanner(error) } }
                    if (state.apiKey) {
                        item(key = "api-key") {
                            InlineNotice(
                                stringResource(R.string.settings_signin_api_key_banner),
                                modifier = Modifier.padding(horizontal = Spacing.Screen),
                                warn = false,
                            )
                        }
                    }
                    item(key = "passwords") { PasswordsCard(state, model) }
                    item(key = "two-factor") { TwoFactorCard(state, model) }
                    item(key = "forgot") { ForgotCard(state, model) }
                    SettingsSignInWayKind.entries.forEach { kind ->
                        item(key = "way-${kind.name}") { WayCard(state, kind, model) }
                    }
                    item(key = "sso") { SsoCard(state, model, snackbar) }
                    if (state.showLocal) item(key = "local") { LocalCard(state, model) }
                    item(key = "api-keys") { ApiKeysRow { onOpen(SettingsApiRoute) } }
                    item(key = "advanced") { AdvancedHeader(advanced) { advanced = !advanced } }
                    if (advanced) {
                        item(key = "hosts") { HostsCard(state, model) }
                        item(key = "proxies") { ProxiesInfoCard() }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }

    SettingsReauthDialog(
        state = state.reauth,
        onConfirm = { password, code -> model.run { confirmReauth(password, code) } },
        onCancel = model.presenter::cancelReauth,
    )
    state.everyoneAsk?.let { ask -> EveryoneDialog(ask, model) }
    state.importSheet?.let { sheet -> ImportSheet(sheet, model) }
}

@Composable
private fun EveryoneDialog(
    ask: SettingsSignInEveryoneAsk,
    model: SignInModel,
) {
    val who =
        when (ask.who) {
            SettingsSignInEveryoneWho.PICKED_SERVER -> stringResource(R.string.settings_signin_who_shared)
            SettingsSignInEveryoneWho.EVERY_PLEX -> stringResource(R.string.settings_signin_who_every)
            SettingsSignInEveryoneWho.SERVER -> stringResource(R.string.settings_signin_who_server, ask.kind.app)
        }
    val count =
        if (ask.count >= 0) {
            " " +
                stringResource(
                    R.string.settings_signin_last_list,
                    pluralStringResource(R.plurals.settings_signin_people, ask.count, ask.count),
                )
        } else {
            ""
        }
    ConfirmDialog(
        title = stringResource(R.string.settings_signin_everyone_title),
        text = stringResource(R.string.settings_signin_everyone_text, who, count),
        confirm = stringResource(R.string.settings_signin_everyone_confirm),
        dismiss = stringResource(R.string.settings_signin_cancel),
        onConfirm = { model.run { saveWay(ask.kind, confirmed = true) } },
        onDismiss = model.presenter::dismissEveryone,
    )
}
