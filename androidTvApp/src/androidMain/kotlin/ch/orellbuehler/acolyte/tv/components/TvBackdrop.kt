package ch.orellbuehler.acolyte.tv.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.components.ServerImage

@Composable
fun TvBackdropLayer(
    source: String?,
    modifier: Modifier = Modifier,
    blurred: Boolean = false,
) {
    val background = TvColors.background
    Box(modifier.fillMaxSize().clipToBounds()) {
        Crossfade(targetState = source, animationSpec = tween(400), label = "backdrop") { current ->
            Box(Modifier.fillMaxSize()) {
                if (current != null) {
                    ServerImage(
                        source = current,
                        contentDescription = null,
                        size = if (blurred) ImageSize.BACKDROP else ImageSize.HERO,
                        modifier = if (blurred) Modifier.fillMaxSize().scale(1.15f).blur(24.dp) else Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (blurred) {
            Box(Modifier.fillMaxSize().background(background.copy(alpha = 0.68f)))
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to background.copy(alpha = 0.94f),
                        0.5f to background.copy(alpha = 0.55f),
                        1f to background.copy(alpha = 0.05f),
                    ),
                ),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.35f to background.copy(alpha = 0f),
                        1f to background.copy(alpha = 0.94f),
                    ),
                ),
            )
        }
    }
}

@Composable
fun TvBillboard(
    card: DiscoverCard,
    modifier: Modifier = Modifier,
    label: String? = null,
    pills: List<String> = emptyList(),
) {
    AnimatedContent(
        targetState = card,
        contentKey = { it.key },
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        modifier = modifier.fillMaxWidth(),
        label = "billboard",
    ) { shown ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (label != null) TvStatusChip(label, Tone.IDLE, onArt = true)
            if (pills.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pills.forEach { TvStatusChip(it, Tone.INFO, onArt = true) }
                }
            }
            val logo = shown.logo
            if (logo != null) {
                ServerImage(
                    source = logo,
                    contentDescription = shown.name,
                    size = ImageSize.POSTER_LARGE,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.size(TvMetrics.logoWidth, TvMetrics.logoHeight),
                )
            } else {
                Text(shown.name, style = TvType.title, color = TvColors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val meta = billboardMeta(shown)
            if (meta.isNotEmpty()) Text(meta, style = TvType.callout, color = TvColors.text, maxLines = 1)
            if (shown.overview.isNotEmpty()) {
                Text(
                    shown.overview,
                    style = TvType.footnote,
                    color = TvColors.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
                )
            }
        }
    }
}

@Composable
private fun billboardMeta(card: DiscoverCard): String {
    val parts = mutableListOf(tvKindText(card.kind))
    if (card.year > 0) parts += card.year.toString()
    parts += card.genres.take(3).map { tvGenreText(it) }
    return parts.joinToString(" · ")
}
