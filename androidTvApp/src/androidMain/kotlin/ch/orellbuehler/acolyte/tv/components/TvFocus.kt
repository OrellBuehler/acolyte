package ch.orellbuehler.acolyte.tv.components

import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics

fun FocusRequester.requestFocusSafely() {
    runCatching { requestFocus() }
}

private class RectResponder(
    private val rect: (Rect) -> Rect,
) : BringIntoViewResponder {
    override fun calculateRectForParent(localRect: Rect): Rect = rect(localRect)

    override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
}

@Composable
fun Modifier.tvRowBounds(): Modifier {
    val margin = with(LocalDensity.current) { TvMetrics.marginY.toPx() }
    var height by remember { mutableFloatStateOf(0f) }
    val responder = remember(margin) { RectResponder { Rect(it.left, -margin, it.right, height + margin) } }
    return onSizeChanged { height = it.height.toFloat() }.bringIntoViewResponder(responder)
}

@Composable
fun Modifier.tvFocusSlack(): Modifier {
    val slack = with(LocalDensity.current) { TvMetrics.focusPad.toPx() }
    val responder = remember(slack) { RectResponder { it.inflate(slack) } }
    return bringIntoViewResponder(responder)
}

@Composable
fun Modifier.tvTopScrim(): Modifier {
    val height = with(LocalDensity.current) { TvMetrics.scrimHeight.toPx() }
    return drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(
                0f to TvColors.background.copy(alpha = SCRIM_ALPHA),
                1f to Color.Transparent,
                startY = 0f,
                endY = height,
            ),
            size = Size(size.width, height),
        )
    }
}

private const val SCRIM_ALPHA = 0.9f

@Stable
class TvRowFocus {
    var active by mutableStateOf<Any?>(null)
        private set

    fun update(
        row: Any,
        focused: Boolean,
    ) {
        if (focused) {
            active = row
        } else if (active === row) {
            active = null
        }
    }

    fun alpha(row: Any): Float = if (active == null || active === row) 1f else TvMetrics.DIM_ALPHA
}

val LocalTvRowFocus = staticCompositionLocalOf<TvRowFocus?> { null }
