package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.discover.CardMark
import ch.orellbuehler.acolyte.feature.discover.MarkState
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.components.ServerImage

@Composable
fun TvCardSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(TvMetrics.cornerRadius),
    content: @Composable BoxScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.tvFocusSlack(),
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = TvColors.surfaceRaised),
        scale = CardDefaults.scale(focusedScale = TvMetrics.FOCUS_SCALE),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape)),
    ) {
        Box(Modifier.fillMaxSize(), content = content)
    }
}

fun Modifier.tvOnFocus(onFocus: (() -> Unit)?): Modifier = if (onFocus == null) this else onFocusChanged { if (it.hasFocus) onFocus() }

@Composable
fun TvBadgeLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = TvType.caption,
        color = TvColors.onAccent,
        maxLines = 1,
        modifier = modifier.background(TvColors.accentFill, CircleShape).padding(horizontal = 9.dp, vertical = 2.dp),
    )
}

@Composable
fun TvFreshTab(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = TvType.caption,
        color = TvColors.onAccent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            modifier
                .background(TvColors.accentFill, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .padding(horizontal = 9.dp, vertical = 2.dp),
    )
}

@Composable
fun TvOwnedMark(
    mark: CardMark,
    modifier: Modifier = Modifier,
) {
    val colors = TvColors.status
    val fill =
        when (mark.state) {
            MarkState.AVAILABLE, MarkState.PARTIAL -> colors.markHave
            MarkState.DOWNLOADING -> colors.markDownloading
            MarkState.REQUESTED -> colors.markAsked
            MarkState.DECLINED -> colors.markNo
        }
    val icon =
        when (mark.state) {
            MarkState.AVAILABLE -> Icons.Rounded.Check
            MarkState.PARTIAL -> Icons.Rounded.Remove
            MarkState.DOWNLOADING -> Icons.Rounded.ArrowDownward
            MarkState.REQUESTED -> Icons.Rounded.Schedule
            MarkState.DECLINED -> Icons.Rounded.Close
        }
    Box(
        modifier.size(24.dp).background(fill, CircleShape).border(1.dp, colors.markRing, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (mark.state == MarkState.DOWNLOADING && mark.percent >= 0) {
            CircularProgressIndicator(
                progress = { mark.percent / 100f },
                color = colors.onArt,
                trackColor = Color.Transparent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        }
        Icon(
            icon,
            contentDescription = null,
            tint = if (mark.state == MarkState.REQUESTED) colors.markAskedGlyph else colors.onArt,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun BoxScope.TvCardOverlays(
    mark: CardMark?,
    markAlignment: Alignment,
    fresh: String?,
    progress: Float?,
    badge: String?,
) {
    if (mark != null) TvOwnedMark(mark, Modifier.align(markAlignment).padding(6.dp))
    if (badge != null) TvBadgeLabel(badge, Modifier.align(Alignment.TopStart).padding(6.dp))
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = TvColors.accent,
            trackColor = TvColors.status.artScrim,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
        )
    } else if (fresh != null) {
        TvFreshTab(fresh, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun TvCardText(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        Text(title, style = TvType.footnoteBold, color = TvColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrEmpty()) {
            Text(subtitle, style = TvType.caption, color = TvColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TvMenuHost(
    menu: List<TvMenuAction>,
    content: @Composable (onLongClick: (() -> Unit)?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    content(if (menu.isEmpty()) null else ({ open = true }))
    if (open) TvMenuDialog(menu) { open = false }
}

@Composable
fun TvPosterCard(
    title: String,
    image: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    mark: CardMark? = null,
    fresh: String? = null,
    progress: Float? = null,
    badge: String? = null,
    menu: List<TvMenuAction> = emptyList(),
    onFocus: (() -> Unit)? = null,
) {
    Column(modifier.width(TvMetrics.posterWidth), verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap)) {
        TvMenuHost(menu) { onLongClick ->
            TvCardSurface(
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = Modifier.size(TvMetrics.posterWidth, TvMetrics.posterHeight).tvOnFocus(onFocus),
            ) {
                ServerImage(
                    source = image,
                    contentDescription = title,
                    size = ImageSize.POSTER_LARGE,
                    placeholderName = title,
                    modifier = Modifier.fillMaxSize(),
                )
                TvCardOverlays(mark, Alignment.TopEnd, fresh, progress, badge)
            }
        }
        TvCardText(title, subtitle = subtitle)
    }
}

@Composable
fun TvBackdropCard(
    title: String,
    image: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detail: String? = null,
    chip: TvChipInfo? = null,
    logo: String? = null,
    mark: CardMark? = null,
    fresh: String? = null,
    progress: Float? = null,
    badge: String? = null,
    menu: List<TvMenuAction> = emptyList(),
    showsText: Boolean = true,
    onFocus: (() -> Unit)? = null,
) {
    Column(modifier.width(TvMetrics.backdropWidth), verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap)) {
        TvMenuHost(menu) { onLongClick ->
            TvCardSurface(
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = Modifier.size(TvMetrics.backdropWidth, TvMetrics.backdropHeight).tvOnFocus(onFocus),
            ) {
                ServerImage(
                    source = image,
                    contentDescription = title,
                    size = ImageSize.BACKDROP,
                    placeholderName = title,
                    modifier = Modifier.fillMaxSize(),
                )
                if (logo != null) {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, TvColors.status.artScrim))),
                    ) {
                        ServerImage(
                            source = logo,
                            contentDescription = null,
                            size = ImageSize.POSTER,
                            contentScale = ContentScale.Fit,
                            modifier =
                                Modifier
                                    .padding(8.dp)
                                    .padding(bottom = if (fresh != null && progress == null) 18.dp else 0.dp)
                                    .size(100.dp, 28.dp),
                        )
                    }
                }
                TvCardOverlays(mark, Alignment.BottomEnd, fresh, progress, badge)
            }
        }
        if (showsText) TvCardText(title, subtitle = subtitle)
        if (chip != null) TvStatusChip(chip.text, chip.tone)
        if (!detail.isNullOrEmpty()) {
            Text(
                detail,
                style = TvType.caption.copy(fontWeight = FontWeight.Normal),
                color = TvColors.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
fun TvPersonCard(
    name: String,
    photo: String?,
    modifier: Modifier = Modifier,
    role: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier.width(TvMetrics.personSize + 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TvMetrics.textGap),
    ) {
        TvCardSurface(
            onClick = { onClick?.invoke() },
            shape = CircleShape,
            modifier = Modifier.size(TvMetrics.personSize),
        ) {
            ServerImage(
                source = photo,
                contentDescription = name,
                size = ImageSize.THUMB,
                placeholderName = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
        Text(
            name,
            style = TvType.footnoteBold,
            color = TvColors.text,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        if (!role.isNullOrEmpty()) {
            Text(role, style = TvType.caption, color = TvColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TvThumbCard(
    image: String?,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    square: Boolean = false,
    onFocus: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val height = TvMetrics.thumbHeight
    TvCardSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = height + TvMetrics.thumbInset * 2).tvOnFocus(onFocus),
    ) {
        Row(Modifier.padding(TvMetrics.thumbInset), horizontalArrangement = Arrangement.spacedBy(TvMetrics.thumbInset)) {
            ServerImage(
                source = image,
                contentDescription = name,
                size = ImageSize.POSTER,
                placeholderName = name,
                modifier = Modifier.size(if (square) height else height * 2 / 3, height).clip(RoundedCornerShape(TvMetrics.thumbCorner)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                content()
            }
        }
    }
}
