package ch.orellbuehler.acolyte.tv.signin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.session.SignInState
import ch.orellbuehler.acolyte.session.SignInStep
import ch.orellbuehler.acolyte.session.SignInWay
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvTextField
import ch.orellbuehler.acolyte.tv.components.requestFocusSafely
import ch.orellbuehler.acolyte.tv.nav.TvBrandMark
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.signInErrorText

@Composable
fun TvSignInScreen(expired: Boolean) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { TvSignInViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.restartIfDone() }
    BackHandler(enabled = state.step != SignInStep.SERVER && state.step != SignInStep.DONE) { viewModel.back() }
    Box(Modifier.fillMaxSize().padding(horizontal = TvMetrics.marginX, vertical = TvMetrics.marginY), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = TvMetrics.formWidth).verticalScroll(rememberScrollState()).padding(vertical = TvMetrics.focusPad),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TvBrandMark(size = 88)
            when (state.step) {
                SignInStep.SERVER, SignInStep.DONE -> ServerStep(state, expired, viewModel)
                SignInStep.CREDENTIALS -> CredentialsStep(state, viewModel)
                SignInStep.TWO_FACTOR -> TwoFactorStep(state, viewModel)
                SignInStep.SETUP, SignInStep.RESET_CODE, SignInStep.RESET_PASSWORD -> UnsupportedStep(state, viewModel)
            }
            state.error?.let {
                Text(signInErrorText(it), style = TvType.footnote, color = TvColors.status.failText, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Heading(
    title: String,
    subtitle: String?,
) {
    Text(title, style = TvType.title, color = TvColors.text, textAlign = TextAlign.Center)
    if (!subtitle.isNullOrEmpty()) Text(subtitle, style = TvType.footnote, color = TvColors.textMuted, textAlign = TextAlign.Center)
}

@Composable
private fun FocusOnShow(requester: FocusRequester) {
    LaunchedEffect(Unit) { requester.requestFocusSafely() }
}

@Composable
private fun ServerStep(
    state: SignInState,
    expired: Boolean,
    viewModel: TvSignInViewModel,
) {
    var address by rememberSaveable { mutableStateOf(state.serverInput) }
    Heading(
        stringResource(R.string.tv_connect_title),
        stringResource(if (expired) R.string.tv_connect_expired else R.string.tv_connect_subtitle),
    )
    TvTextField(
        value = address,
        onValueChange = { address = it },
        hint = stringResource(R.string.tv_connect_address_hint),
        keyboardType = KeyboardType.Uri,
        imeAction = ImeAction.Go,
        onAction = { if (address.isNotBlank()) viewModel.connect(address) },
    )
    TvActionButton(
        title = stringResource(R.string.tv_connect_continue),
        onClick = { viewModel.connect(address) },
        prominent = true,
        enabled = !state.busy && address.isNotBlank(),
    )
}

@Composable
private fun CredentialsStep(
    state: SignInState,
    viewModel: TvSignInViewModel,
) {
    var name by rememberSaveable { mutableStateOf(state.userName) }
    var password by rememberSaveable { mutableStateOf("") }
    val ways = state.ways.filter { it != SignInWay.PLEX && it.inApp }
    val way = if (SignInWay.PASSWORD in ways) SignInWay.PASSWORD else ways.firstOrNull() ?: SignInWay.PASSWORD
    val submit = { if (name.isNotEmpty() && password.isNotEmpty()) viewModel.signIn(name, password, way) }
    Heading(stringResource(R.string.tv_signin_title), state.serverName)
    TvTextField(name, { name = it }, stringResource(R.string.tv_signin_name))
    TvTextField(
        value = password,
        onValueChange = { password = it },
        hint = stringResource(R.string.tv_signin_password),
        password = true,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Go,
        onAction = { submit() },
    )
    TvActionButton(
        title = stringResource(R.string.tv_signin_submit),
        onClick = { submit() },
        prominent = true,
        enabled = !state.busy && name.isNotEmpty() && password.isNotEmpty(),
    )
    TvActionButton(stringResource(R.string.tv_connect_change_server), viewModel::back, enabled = !state.busy)
}

@Composable
private fun TwoFactorStep(
    state: SignInState,
    viewModel: TvSignInViewModel,
) {
    var code by rememberSaveable { mutableStateOf("") }
    Heading(stringResource(R.string.tv_twofactor_title), stringResource(R.string.tv_twofactor_message))
    TvTextField(
        value = code,
        onValueChange = { code = it },
        hint = stringResource(R.string.tv_twofactor_code),
        imeAction = ImeAction.Done,
        onAction = { if (code.isNotEmpty()) viewModel.verify(code) },
    )
    TvActionButton(
        title = stringResource(R.string.tv_twofactor_submit),
        onClick = { viewModel.verify(code) },
        prominent = true,
        enabled = !state.busy && code.isNotEmpty(),
    )
    TvActionButton(stringResource(R.string.tv_common_back), viewModel::back, enabled = !state.busy)
}

@Composable
private fun UnsupportedStep(
    state: SignInState,
    viewModel: TvSignInViewModel,
) {
    val focus = remember { FocusRequester() }
    Heading(stringResource(R.string.tv_signin_title), stringResource(R.string.tv_signin_unsupported))
    TvActionButton(stringResource(R.string.tv_common_back), viewModel::back, Modifier.focusRequester(focus), enabled = !state.busy)
    FocusOnShow(focus)
}
