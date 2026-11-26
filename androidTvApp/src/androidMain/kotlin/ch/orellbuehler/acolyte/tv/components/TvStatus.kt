package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.components.errorText

@Composable
fun TvLoadingView(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.tv_common_loading)
    Box(modifier.fillMaxSize().semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TvColors.accent, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun TvMessage(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().padding(TvMetrics.marginX),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = TvColors.textMuted, modifier = Modifier.size(48.dp))
        Text(title, style = TvType.title3, color = TvColors.text, textAlign = TextAlign.Center)
        if (!message.isNullOrEmpty()) {
            Text(
                message,
                style = TvType.footnote,
                color = TvColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
            )
        }
        actions()
    }
}

@Composable
fun TvErrorView(
    error: ApiError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val focus = remember { FocusRequester() }
    val offline = error.code == ApiErrorCode.OFFLINE || error.code == ApiErrorCode.TIMEOUT
    TvMessage(
        icon = if (offline) Icons.Rounded.WifiOff else Icons.Rounded.Warning,
        title = stringResource(R.string.tv_error_title),
        message = errorText(error),
        modifier = modifier,
    ) {
        if (onRetry != null) {
            TvActionButton(stringResource(R.string.tv_common_retry), onRetry, Modifier.focusRequester(focus), prominent = true)
            LaunchedEffect(Unit) { focus.requestFocusSafely() }
        }
    }
}

@Composable
fun TvEmptyView(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.tv_empty_title),
    message: String? = null,
    icon: ImageVector = Icons.Rounded.Inbox,
    actions: @Composable () -> Unit = {},
) {
    TvMessage(icon, title, modifier, message, actions)
}

@Composable
fun TvStatusContent(
    status: LoadStatus,
    isEmpty: Boolean,
    reload: () -> Unit,
    modifier: Modifier = Modifier,
    empty: @Composable () -> Unit = { TvEmptyView() },
    content: @Composable () -> Unit,
) {
    when (status.phase) {
        Phase.LOADING -> TvLoadingView(modifier)
        Phase.FAILED -> status.error?.let { TvErrorView(it, modifier, onRetry = reload) } ?: TvLoadingView(modifier)
        Phase.READY ->
            if (isEmpty) {
                val focus = remember { FocusRequester() }
                Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f)) { empty() }
                    TvActionButton(
                        stringResource(R.string.tv_reload),
                        reload,
                        Modifier.padding(bottom = TvMetrics.marginY * 2).focusRequester(focus),
                    )
                    LaunchedEffect(Unit) { focus.requestFocusSafely() }
                }
            } else {
                content()
            }
    }
}

@Composable
fun TvNoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(TvMetrics.cornerRadius)
    Row(
        modifier
            .fillMaxWidth()
            .background(TvColors.surfaceRaised, shape)
            .border(1.dp, TvColors.status.fail.copy(alpha = 0.45f), shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Warning, contentDescription = null, tint = TvColors.status.failText, modifier = Modifier.size(20.dp))
        Text(text, style = TvType.footnote, color = TvColors.text, maxLines = 3)
    }
}

@Composable
fun TvNoteBanner(
    text: String,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    onUndo: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(TvColors.surfaceHigh, RoundedCornerShape(TvMetrics.cornerRadius))
            .padding(TvMetrics.thumbInset),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (failed) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (failed) TvColors.status.failText else TvColors.status.ok,
            modifier = Modifier.size(20.dp),
        )
        Text(text, style = TvType.footnote, color = TvColors.text, maxLines = 3, modifier = Modifier.weight(1f))
        if (onUndo != null) TvActionButton(stringResource(R.string.tv_requests_undo), onUndo)
    }
}

@Composable
fun TvOpenOnPhoneView(
    what: String,
    modifier: Modifier = Modifier,
) {
    TvMessage(
        icon = Icons.Rounded.PhoneAndroid,
        title = stringResource(R.string.tv_phone_title),
        message = stringResource(R.string.tv_phone_body, what),
        modifier = modifier,
    )
}

@Composable
fun TvPlaceholderView(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    TvMessage(
        icon = icon,
        title = title,
        message = stringResource(R.string.tv_placeholder_body, title),
        modifier = modifier,
    )
}
