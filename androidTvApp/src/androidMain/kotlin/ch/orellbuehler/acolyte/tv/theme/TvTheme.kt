package ch.orellbuehler.acolyte.tv.theme

import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.ui.theme.AcolyteColors
import ch.orellbuehler.acolyte.ui.theme.AcolyteTheme
import ch.orellbuehler.acolyte.ui.theme.Palette
import ch.orellbuehler.acolyte.ui.theme.acolyte

object TvMetrics {
    val posterWidth: Dp = 128.dp
    val posterHeight: Dp = 192.dp
    val backdropWidth: Dp = 200.dp
    val backdropHeight: Dp = 112.dp
    val personSize: Dp = 84.dp
    val personPhoto: Dp = 120.dp
    val gutter: Dp = 16.dp
    val sectionGap: Dp = 28.dp
    val focusPad: Dp = 14.dp
    val textGap: Dp = 8.dp
    val readableWidth: Dp = 500.dp
    val cornerRadius: Dp = 10.dp
    val marginX: Dp = 48.dp
    val marginY: Dp = 27.dp
    val thumbHeight: Dp = 76.dp
    val thumbInset: Dp = 12.dp
    val thumbCorner: Dp = 6.dp
    val tabBarHeight: Dp = 48.dp
    val logoWidth: Dp = 260.dp
    val logoHeight: Dp = 76.dp
    val formWidth: Dp = 420.dp
    val modeTileWidth: Dp = 250.dp
    val modeTileHeight: Dp = 140.dp
    const val FOCUS_SCALE = 1.08f
    const val HERO_FRACTION = 0.62f
    const val TITLE_HERO_FRACTION = 0.8f
}

object TvType {
    val largeTitle = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val title3 = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
    val headline = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
    val callout = TextStyle(fontSize = 18.sp, lineHeight = 24.sp)
    val calloutBold = callout.copy(fontWeight = FontWeight.SemiBold)
    val footnote = TextStyle(fontSize = 16.sp, lineHeight = 22.sp)
    val footnoteBold = footnote.copy(fontWeight = FontWeight.SemiBold)
    val caption = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
}

object TvColors {
    val background: Color = Palette.Ink
    val surface: Color = Palette.Night2
    val surfaceRaised: Color = Palette.Night3
    val surfaceHigh: Color = Palette.Night4
    val text: Color = Palette.TextDark
    val textMuted: Color = Palette.MutedDark
    val textFaint: Color = Palette.FaintDark
    val accent: Color = Palette.Red
    val accentFill: Color = Palette.RedFill
    val accentText: Color = Palette.RedBright
    val onAccent: Color = Color.White
    val line: Color = Palette.NightLineStrong

    val status: AcolyteColors
        @Composable
        @ReadOnlyComposable
        get() = androidx.compose.material3.MaterialTheme.acolyte

    @Composable
    @ReadOnlyComposable
    fun dot(tone: Tone): Color = status.dot(tone)

    @Composable
    @ReadOnlyComposable
    fun textFor(tone: Tone): Color = status.text(tone) ?: text
}

private val TvTypography =
    Typography(
        displayLarge = TvType.largeTitle,
        displayMedium = TvType.largeTitle,
        displaySmall = TvType.title,
        headlineLarge = TvType.title,
        headlineMedium = TvType.title3,
        headlineSmall = TvType.headline,
        titleLarge = TvType.headline,
        titleMedium = TvType.calloutBold,
        titleSmall = TvType.footnoteBold,
        bodyLarge = TvType.callout,
        bodyMedium = TvType.footnote,
        bodySmall = TvType.caption,
        labelLarge = TvType.calloutBold,
        labelMedium = TvType.footnoteBold,
        labelSmall = TvType.caption,
    )

private val TvColorScheme =
    darkColorScheme(
        primary = TvColors.accent,
        onPrimary = TvColors.onAccent,
        primaryContainer = TvColors.accentFill,
        onPrimaryContainer = TvColors.onAccent,
        secondary = TvColors.textMuted,
        onSecondary = Palette.Ink,
        secondaryContainer = TvColors.surfaceHigh,
        onSecondaryContainer = TvColors.text,
        background = TvColors.background,
        onBackground = TvColors.text,
        surface = TvColors.surfaceRaised,
        onSurface = TvColors.text,
        surfaceVariant = TvColors.surfaceHigh,
        onSurfaceVariant = TvColors.textMuted,
        error = Color(0xFFFF6961),
        onError = Color(0xFF3A0000),
        border = Color.White,
        borderVariant = TvColors.line,
        scrim = Color.Black,
    )

// Compose's default pivots every focused item to 30 percent of the list, which scrolls a billboard away
// as soon as focus reaches the first row. Scroll only as far as needed to show the focused item.
private val TvMinimalScroll =
    object : BringIntoViewSpec {
        override fun calculateScrollDistance(
            offset: Float,
            size: Float,
            containerSize: Float,
        ): Float {
            val trailing = offset + size
            return when {
                offset >= 0f && trailing <= containerSize -> 0f
                offset < 0f && trailing > containerSize -> 0f
                kotlin.math.abs(offset) < kotlin.math.abs(trailing - containerSize) -> offset
                else -> trailing - containerSize
            }
        }
    }

@Composable
fun TvTheme(content: @Composable () -> Unit) {
    AcolyteTheme(dark = true) {
        MaterialTheme(colorScheme = TvColorScheme, typography = TvTypography) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides TvMinimalScroll, content = content)
        }
    }
}
