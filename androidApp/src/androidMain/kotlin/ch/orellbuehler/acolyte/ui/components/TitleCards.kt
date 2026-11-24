package ch.orellbuehler.acolyte.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.orellbuehler.acolyte.feature.discover.CardMark
import ch.orellbuehler.acolyte.feature.discover.MarkState
import ch.orellbuehler.acolyte.feature.discover.PersonCard
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

val WideCardWidth: Dp = 264.dp

@Composable
fun OwnedMark(
    mark: CardMark,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.acolyte
    val fill =
        when (mark.state) {
            MarkState.AVAILABLE, MarkState.PARTIAL -> colors.markHave
            MarkState.DOWNLOADING -> colors.markDownloading
            MarkState.REQUESTED -> colors.markAsked
            MarkState.DECLINED -> colors.markNo
        }
    val glyph = if (mark.state == MarkState.REQUESTED) colors.markAskedGlyph else colors.onArt
    Box(
        modifier
            .size(24.dp)
            .background(fill, CircleShape)
            .border(1.dp, colors.markRing, CircleShape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (mark.state == MarkState.DOWNLOADING && mark.percent >= 0) {
            CircularProgressIndicator(
                progress = { mark.percent / 100f },
                color = colors.onArt,
                trackColor = colors.artFill,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        }
        Icon(
            when (mark.state) {
                MarkState.AVAILABLE -> Icons.Rounded.Check
                MarkState.PARTIAL -> Icons.Rounded.Remove
                MarkState.DOWNLOADING -> Icons.Rounded.ArrowDownward
                MarkState.REQUESTED -> Icons.Rounded.Schedule
                MarkState.DECLINED -> Icons.Rounded.Close
            },
            contentDescription = null,
            tint = glyph,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
fun FreshTab(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun CardActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.acolyte.artScrimStrong,
                contentColor = MaterialTheme.acolyte.onArt,
            ),
        modifier = modifier.size(36.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = label)
    }
}

@Composable
fun CardArt(
    image: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: ImageSize = ImageSize.BACKDROP,
    logo: String? = null,
    mark: CardMark? = null,
    markLabel: String = "",
    fresh: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.clip(RoundedCornerShape(Radius.Card)).background(MaterialTheme.acolyte.placeholder)) {
        ServerImage(source = image, contentDescription = null, size = size, placeholderName = name, modifier = Modifier.fillMaxSize())
        if (logo != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.acolyte.artScrim))),
            )
            ServerImage(
                source = logo,
                contentDescription = null,
                size = ImageSize.POSTER,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .fillMaxWidth(0.55f)
                        .height(36.dp),
            )
        }
        overlay()
        if (mark != null) OwnedMark(mark, markLabel, Modifier.align(Alignment.TopEnd).padding(8.dp))
        if (fresh != null) FreshTab(fresh, Modifier.align(Alignment.BottomCenter))
        if (onAction != null && actionLabel != null) {
            CardActionButton(actionLabel, onAction, Modifier.align(Alignment.BottomEnd).padding(6.dp))
        }
    }
}

@Composable
fun WideCard(
    title: String,
    image: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Dp = WideCardWidth,
    logo: String? = null,
    mark: CardMark? = null,
    markLabel: String = "",
    fresh: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .width(width)
            .clip(RoundedCornerShape(Radius.Card))
            .combinedClickable(enabled = onClick != null || onLongClick != null, onLongClick = onLongClick) { onClick?.invoke() },
    ) {
        CardArt(
            image = image,
            name = title,
            logo = logo,
            mark = mark,
            markLabel = markLabel,
            fresh = fresh,
            actionLabel = actionLabel,
            onAction = onAction,
            size = if (width > 320.dp) ImageSize.HERO else ImageSize.BACKDROP,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (!subtitle.isNullOrEmpty()) {
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

@Composable
fun RankedPoster(
    rank: Int,
    content: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            rank.toString(),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 64.sp, lineHeight = 64.sp),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp, bottom = 40.dp),
        )
        content()
    }
}

@Composable
fun PersonCircle(
    person: PersonCard,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .width(size + 16.dp)
            .clip(RoundedCornerShape(Radius.Card))
            .combinedClickable(enabled = onClick != null) { onClick?.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ServerImage(
            source = person.photo,
            contentDescription = null,
            size = ImageSize.THUMB,
            placeholderName = person.name,
            modifier = Modifier.size(size).clip(CircleShape),
        )
        Text(
            person.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.Small),
        )
        if (person.note.isNotEmpty()) {
            Text(
                person.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BrowseTileCard(
    name: String,
    image: String?,
    hue: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    plain: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.acolyte
    Box(
        modifier
            .clip(RoundedCornerShape(Radius.Card))
            .background(colors.placeholder)
            .combinedClickable(onClick = onClick),
    ) {
        ServerImage(source = image, contentDescription = null, size = ImageSize.BACKDROP, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (plain) {
                        Brush.verticalGradient(listOf(Color.Transparent, colors.artScrim))
                    } else {
                        Brush.verticalGradient(listOf(colors.tileHues[hue].copy(alpha = 0.55f), colors.tileHues[hue].copy(alpha = 0.92f)))
                    },
                ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(Spacing.Medium)) {
            if (label != null) {
                Text(label, color = colors.onArt.copy(alpha = 0.84f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Text(
                name,
                color = colors.onArt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CardShelf(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    seeAllLabel: String? = null,
    onSeeAll: (() -> Unit)? = null,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.Small)) {
        if (title.isNotEmpty()) SectionHeader(title, actionLabel = seeAllLabel, onAction = onSeeAll)
        if (!subtitle.isNullOrEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Screen).padding(bottom = Spacing.Small),
            )
        }
        header()
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.Screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            modifier = Modifier.heightIn(min = 48.dp),
            content = content,
        )
        footer()
    }
}
