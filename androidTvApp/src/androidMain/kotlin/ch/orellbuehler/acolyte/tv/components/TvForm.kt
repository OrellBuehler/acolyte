package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType

@Composable
fun TvModeTile(
    icon: ImageVector,
    title: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TvCardSurface(
        onClick = { if (enabled) onClick() },
        modifier =
            modifier
                .size(TvMetrics.modeTileWidth, TvMetrics.modeTileHeight)
                .alpha(if (enabled) 1f else 0.5f)
                .focusProperties { canFocus = enabled },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, contentDescription = null, tint = TvColors.text, modifier = Modifier.size(24.dp))
                if (selected) TvSelectedMark()
            }
            Text(
                title,
                style = TvType.calloutBold,
                color = if (enabled) TvColors.text else TvColors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(hint, style = TvType.caption, color = TvColors.textMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TvToggleRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    ListItem(
        selected = false,
        onClick = { onChange(!checked) },
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = ListItemDefaults.shape(RoundedCornerShape(TvMetrics.cornerRadius)),
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = TvColors.text,
                focusedContainerColor = TvColors.surfaceHigh,
                focusedContentColor = TvColors.text,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = TvColors.textFaint,
            ),
        headlineContent = { Text(title, style = TvType.callout) },
        supportingContent = hint?.let { text -> { Text(text, style = TvType.footnote, color = TvColors.textMuted) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors =
                    SwitchDefaults.colors(
                        checkedTrackColor = TvColors.accentFill,
                        checkedThumbColor = TvColors.onAccent,
                    ),
            )
        },
    )
}
