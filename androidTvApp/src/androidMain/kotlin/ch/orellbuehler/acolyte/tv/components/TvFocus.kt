package ch.orellbuehler.acolyte.tv.components

import androidx.compose.ui.focus.FocusRequester

fun FocusRequester.requestFocusSafely() {
    runCatching { requestFocus() }
}
