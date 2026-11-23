package ch.orellbuehler.acolyte.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun StatusChip(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    onArt: Boolean = false,
) {
    val colors = MaterialTheme.acolyte
    val background = if (onArt) colors.artScrim else colors.chip
    val content = if (onArt) colors.onArt else colors.text(tone) ?: MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .height(24.dp)
            .background(background, CircleShape)
            .padding(start = 8.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).background(colors.dot(tone), CircleShape))
        Text(
            text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
