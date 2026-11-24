package ch.orellbuehler.acolyte.ui.settings.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@Composable
fun ToneDot(
    tone: Tone,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    Box(modifier.size(size).background(MaterialTheme.acolyte.dot(tone), CircleShape))
}

@Composable
fun ToneBang(
    tone: Tone,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(22.dp).background(MaterialTheme.acolyte.dot(tone), CircleShape), contentAlignment = Alignment.Center) {
        Text("!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.background)
    }
}

@Composable
fun KeyValue(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
fun KeyValueText(
    label: String,
    value: String,
    mono: Boolean = false,
) {
    KeyValue(label) {
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else null,
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, enabled = !busy, modifier = modifier) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(Spacing.Small))
        }
        Text(text, maxLines = 1)
    }
}
