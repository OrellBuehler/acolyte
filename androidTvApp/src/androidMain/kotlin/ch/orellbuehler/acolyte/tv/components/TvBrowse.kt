package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.components.ServerImage

@Composable
fun TvBrowsePage(
    hero: DiscoverCard?,
    modifier: Modifier = Modifier,
    pills: List<String> = emptyList(),
    header: @Composable () -> Unit = {},
    content: LazyListScope.(onFocusCard: (DiscoverCard) -> Unit) -> Unit,
) {
    var ambient by remember { mutableStateOf<DiscoverCard?>(null) }
    LaunchedEffect(hero?.key) { ambient = null }
    val shown = ambient ?: hero
    Box(modifier.fillMaxSize()) {
        TvBackdropLayer(shown?.wideImage)
        LazyColumn(
            contentPadding = PaddingValues(bottom = TvMetrics.marginY),
            verticalArrangement = Arrangement.spacedBy(TvMetrics.sectionGap),
        ) {
            item(key = "hero") {
                Column(
                    Modifier.fillParentMaxHeight(TvMetrics.HERO_FRACTION).tvPageInset(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    header()
                    if (shown != null) {
                        TvBillboard(
                            shown,
                            pills =
                                if (shown.key ==
                                    hero?.key
                                ) {
                                    pills
                                } else {
                                    emptyList()
                                },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }
            content { ambient = it }
        }
    }
}

@Composable
fun TvAmbientPage(
    modifier: Modifier = Modifier,
    fallback: String? = null,
    content: LazyListScope.(onFocusImage: (String?) -> Unit) -> Unit,
) {
    var ambient by remember { mutableStateOf<String?>(null) }
    Box(modifier.fillMaxSize()) {
        TvBackdropLayer(ambient ?: fallback, blurred = true)
        LazyColumn(
            contentPadding = PaddingValues(top = TvMetrics.focusPad, bottom = TvMetrics.marginY),
            verticalArrangement = Arrangement.spacedBy(TvMetrics.sectionGap),
        ) {
            content { image -> if (image != null) ambient = image }
        }
    }
}

@Composable
fun TvBrowseTile(
    name: String,
    image: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hue: Int = 0,
    label: String? = null,
) {
    val colors = TvColors.status
    val tint = colors.tileHues[hue.coerceIn(0, colors.tileHues.lastIndex)]
    TvCardSurface(onClick = onClick, modifier = modifier.size(TvMetrics.backdropWidth, TvMetrics.backdropHeight)) {
        Box(Modifier.fillMaxSize().background(tint.copy(alpha = 0.5f)))
        ServerImage(source = image, contentDescription = null, size = ImageSize.BACKDROP, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to colors.artScrimStrong)))
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            if (label != null) Text(label, style = TvType.caption, color = colors.onArt.copy(alpha = 0.84f), maxLines = 1)
            Text(name, style = TvType.calloutBold, color = colors.onArt, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
