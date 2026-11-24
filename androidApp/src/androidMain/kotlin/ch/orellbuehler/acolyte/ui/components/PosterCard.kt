package ch.orellbuehler.acolyte.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.feature.discover.CardMark
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.acolyte

val PosterWidth: Dp = 120.dp

@Composable
fun PosterCard(
    title: String,
    image: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Dp = PosterWidth,
    progress: Float? = null,
    badges: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    mark: CardMark? = null,
    markLabel: String = "",
    fresh: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .width(width)
            .clip(RoundedCornerShape(Radius.Poster))
            .let {
                if (onClick != null || onLongClick != null) it.combinedClickable(onLongClick = onLongClick) { onClick?.invoke() } else it
            },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(Radius.Poster)),
        ) {
            ServerImage(
                source = image,
                contentDescription = title,
                size = if (width > 160.dp) ImageSize.POSTER_LARGE else ImageSize.POSTER,
                placeholderName = title,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            )
            if (badges != null) {
                Row(Modifier.align(Alignment.TopStart).padding(8.dp)) { badges() }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.acolyte.artScrim,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
                )
            }
            if (mark != null) OwnedMark(mark, markLabel, Modifier.align(Alignment.TopEnd).padding(8.dp))
            if (fresh != null && progress == null) FreshTab(fresh, Modifier.align(Alignment.BottomCenter))
            if (onAction != null && actionLabel != null) {
                CardActionButton(actionLabel, onAction, Modifier.align(Alignment.BottomEnd).padding(6.dp))
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
