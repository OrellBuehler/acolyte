package ch.orellbuehler.acolyte.tv.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import ch.orellbuehler.acolyte.feature.downloads.BarTone
import ch.orellbuehler.acolyte.tv.theme.TvColors

@Composable
@ReadOnlyComposable
fun tvBarColor(tone: BarTone): Color {
    val colors = TvColors.status
    return when (tone) {
        BarTone.GO, BarTone.DONE -> colors.ok
        BarTone.QUEUED -> colors.info
        BarTone.HELD -> colors.wait
        BarTone.BAD -> TvColors.textFaint
    }
}
