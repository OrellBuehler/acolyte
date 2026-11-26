package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvType

data class TvChipInfo(
    val text: String,
    val tone: Tone,
)

data class TvMenuAction(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

@Composable
fun TvActionButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    prominent: Boolean = false,
    enabled: Boolean = true,
) {
    val colors =
        if (prominent) {
            ButtonDefaults.colors(
                containerColor = TvColors.accentFill,
                contentColor = TvColors.onAccent,
                focusedContainerColor = Color.White,
                focusedContentColor = TvColors.accentFill,
                disabledContainerColor = TvColors.surfaceRaised,
                disabledContentColor = TvColors.textFaint,
            )
        } else {
            ButtonDefaults.colors(
                containerColor = TvColors.surfaceHigh,
                contentColor = TvColors.text,
                focusedContainerColor = Color.White,
                focusedContentColor = TvColors.background,
                disabledContainerColor = TvColors.surfaceRaised,
                disabledContentColor = TvColors.textFaint,
            )
        }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonDefaults.shape(CircleShape),
        colors = colors,
        contentPadding = if (icon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
        }
        Text(title, style = TvType.footnoteBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TvChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = if (selected) ({ Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null,
        colors =
            FilterChipDefaults.colors(
                containerColor = TvColors.surfaceRaised,
                contentColor = TvColors.text,
                focusedContainerColor = Color.White,
                focusedContentColor = TvColors.background,
                selectedContainerColor = TvColors.accentFill,
                selectedContentColor = TvColors.onAccent,
                focusedSelectedContainerColor = Color.White,
                focusedSelectedContentColor = TvColors.accentFill,
                disabledContainerColor = TvColors.surfaceRaised,
                disabledContentColor = TvColors.textFaint,
            ),
        border =
            FilterChipDefaults.border(
                border = Border(BorderStroke(1.dp, TvColors.line)),
                focusedBorder = Border(BorderStroke(2.dp, Color.White)),
            ),
    ) {
        Text(title, style = TvType.footnoteBold, maxLines = 1)
    }
}

@Composable
fun TvStatusChip(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    onArt: Boolean = false,
) {
    val colors = TvColors.status
    val background = if (onArt) colors.artScrim else TvColors.surfaceRaised
    val content = if (onArt) colors.onArt else TvColors.textFor(tone)
    Row(
        modifier
            .height(26.dp)
            .background(background, CircleShape)
            .padding(start = 9.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(TvColors.dot(tone), CircleShape))
        Text(text, style = TvType.caption, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TvConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    cancel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        LaunchedEffect(Unit) { focus.requestFocusSafely() }
        Column(
            Modifier
                .width(420.dp)
                .background(TvColors.surfaceRaised, RoundedCornerShape(16.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = TvType.title3, color = TvColors.text)
            Text(message, style = TvType.footnote, color = TvColors.textMuted)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvActionButton(cancel, onDismiss, Modifier.focusRequester(focus))
                TvActionButton(confirm, onConfirm, prominent = true)
            }
        }
    }
}

@Composable
fun TvMenuDialog(
    actions: List<TvMenuAction>,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        LaunchedEffect(Unit) { focus.requestFocusSafely() }
        Column(
            Modifier
                .width(320.dp)
                .background(TvColors.surfaceRaised, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            actions.forEachIndexed { index, action ->
                TvActionButton(
                    title = action.title,
                    icon = action.icon,
                    modifier = if (index == 0) Modifier.focusRequester(focus) else Modifier,
                    onClick = {
                        onDismiss()
                        action.onClick()
                    },
                )
            }
        }
    }
}

@Composable
fun TvSelectedMark(modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = TvColors.accentText, modifier = modifier.size(22.dp))
}
