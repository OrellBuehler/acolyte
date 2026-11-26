package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType

@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onAction: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(TvMetrics.cornerRadius)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .background(TvColors.surfaceRaised, shape)
                .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else TvColors.line, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        singleLine = true,
        textStyle = TvType.callout.copy(color = TvColors.text),
        cursorBrush = SolidColor(TvColors.accent),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onGo = { onAction?.invoke() },
                onDone = { onAction?.invoke() },
            ),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = TvType.callout, color = TvColors.textFaint, maxLines = 1)
                inner()
            }
        },
    )
}
