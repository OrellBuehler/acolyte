package ch.orellbuehler.acolyte.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ch.orellbuehler.acolyte.feature.Tone

object Palette {
    val Ink = Color(0xFF05080D)
    val Night1 = Color(0xFF0B0F15)
    val Night2 = Color(0xFF11151C)
    val Night3 = Color(0xFF191E26)
    val Night4 = Color(0xFF232932)
    val NightLine = Color(0xFF1D2128)
    val NightLineStrong = Color(0xFF353B44)
    val TextDark = Color(0xFFF5F5F7)
    val MutedDark = Color(0xFFA1A1A6)
    val FaintDark = Color(0xFF7C7C80)

    val Paper = Color(0xFFFFFFFF)
    val Paper2 = Color(0xFFF7F7F8)
    val Paper3 = Color(0xFFF2F2F4)
    val Paper4 = Color(0xFFE9E9EC)
    val PaperLine = Color(0xFFE3E3E6)
    val PaperLineStrong = Color(0xFFD1D1D6)
    val TextLight = Color(0xFF1D1D1F)
    val MutedLight = Color(0xFF6E6E73)
    val FaintLight = Color(0xFF8E8E93)

    val Red = Color(0xFFE5352B)
    val RedText = Color(0xFFCF2C23)
    val RedBright = Color(0xFFFF453A)
    val RedFill = Color(0xFFD42D24)
    val IconRed = Color(0xFFBF281F)
    val Cream = Color(0xFFF3F0E9)
}

@Immutable
class AcolyteColors(
    val ok: Color,
    val okText: Color,
    val wait: Color,
    val waitText: Color,
    val fail: Color,
    val failText: Color,
    val info: Color,
    val infoText: Color,
    val idle: Color,
    val accentText: Color,
    val textFaint: Color,
    val line: Color,
    val chip: Color,
    val placeholder: Color,
) {
    val onArt: Color = Color.White
    val artScrim: Color = Color.Black.copy(alpha = 0.55f)

    val markHave: Color = Color(0xFF1F9D48)
    val markDownloading: Color = Color(0xFF4F46E5)
    val markAsked: Color = Color(0xFFF0A020)
    val markAskedGlyph: Color = Color(0xFF2A1A00)
    val markNo: Color = Color(0xFFD92D20)
    val markRing: Color = Color.Black.copy(alpha = 0.22f)
    val artScrimStrong: Color = Color.Black.copy(alpha = 0.86f)
    val artFill: Color = Color.White.copy(alpha = 0.16f)
    val tileHues: List<Color> =
        listOf(
            0xFFD0202A,
            0xFFB3123E,
            0xFF8E1F4F,
            0xFF6D2A8A,
            0xFFC2410C,
            0xFF9F1D35,
            0xFF3F6A8A,
            0xFFB91C1C,
            0xFF7A1D6B,
            0xFF2C7A7B,
            0xFFC0263F,
            0xFFA3402B,
        ).map { Color(it) }

    fun dot(tone: Tone): Color =
        when (tone) {
            Tone.OK -> ok
            Tone.WAIT -> wait
            Tone.FAIL -> fail
            Tone.INFO -> info
            Tone.IDLE -> idle
        }

    fun text(tone: Tone): Color? =
        when (tone) {
            Tone.OK -> okText
            Tone.FAIL -> failText
            else -> null
        }
}

private val DarkAcolyte =
    AcolyteColors(
        ok = Color(0xFF30D158),
        okText = Color(0xFF30D158),
        wait = Color(0xFFFFB340),
        waitText = Color(0xFFFFB340),
        fail = Color(0xFFFF6961),
        failText = Color(0xFFFF6961),
        info = Color(0xFF409CFF),
        infoText = Color(0xFF409CFF),
        idle = Palette.FaintDark,
        accentText = Palette.RedBright,
        textFaint = Palette.FaintDark,
        line = Palette.NightLine,
        chip = Palette.Night3,
        placeholder = Palette.Night3,
    )

private val LightAcolyte =
    AcolyteColors(
        ok = Color(0xFF248A3D),
        okText = Color(0xFF1A6E2E),
        wait = Color(0xFFB25000),
        waitText = Color(0xFF8F4000),
        fail = Color(0xFFD70015),
        failText = Color(0xFFB3000F),
        info = Color(0xFF0071E3),
        infoText = Color(0xFF0068D1),
        idle = Palette.FaintLight,
        accentText = Palette.RedText,
        textFaint = Palette.FaintLight,
        line = Palette.PaperLine,
        chip = Palette.Paper3,
        placeholder = Palette.Paper3,
    )

val DarkColors: ColorScheme =
    darkColorScheme(
        primary = Palette.Red,
        onPrimary = Color.White,
        primaryContainer = Color(0xFF3B1210),
        onPrimaryContainer = Color(0xFFFFDAD6),
        inversePrimary = Palette.RedText,
        secondary = Palette.MutedDark,
        onSecondary = Palette.Ink,
        secondaryContainer = Palette.Night4,
        onSecondaryContainer = Palette.TextDark,
        tertiary = Color(0xFF409CFF),
        onTertiary = Palette.Ink,
        tertiaryContainer = Color(0xFF0D2A4A),
        onTertiaryContainer = Color(0xFFD6E6FF),
        background = Palette.Ink,
        onBackground = Palette.TextDark,
        surface = Palette.Ink,
        onSurface = Palette.TextDark,
        surfaceVariant = Palette.Night3,
        onSurfaceVariant = Palette.MutedDark,
        surfaceTint = Color.Transparent,
        inverseSurface = Palette.TextDark,
        inverseOnSurface = Palette.Ink,
        error = Color(0xFFFF6961),
        onError = Color(0xFF3A0000),
        errorContainer = Color(0xFF4A1210),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Palette.NightLineStrong,
        outlineVariant = Palette.NightLine,
        scrim = Color.Black,
        surfaceBright = Palette.Night4,
        surfaceDim = Palette.Ink,
        surfaceContainerLowest = Color(0xFF030508),
        surfaceContainerLow = Palette.Night1,
        surfaceContainer = Palette.Night2,
        surfaceContainerHigh = Palette.Night3,
        surfaceContainerHighest = Palette.Night4,
    )

val LightColors: ColorScheme =
    lightColorScheme(
        primary = Palette.RedText,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE1DD),
        onPrimaryContainer = Color(0xFF410002),
        inversePrimary = Palette.RedBright,
        secondary = Palette.MutedLight,
        onSecondary = Color.White,
        secondaryContainer = Palette.Paper4,
        onSecondaryContainer = Palette.TextLight,
        tertiary = Color(0xFF0071E3),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD6E6FF),
        onTertiaryContainer = Color(0xFF001C3A),
        background = Palette.Paper,
        onBackground = Palette.TextLight,
        surface = Palette.Paper,
        onSurface = Palette.TextLight,
        surfaceVariant = Palette.Paper3,
        onSurfaceVariant = Palette.MutedLight,
        surfaceTint = Color.Transparent,
        inverseSurface = Palette.TextLight,
        inverseOnSurface = Palette.Paper,
        error = Color(0xFFD70015),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Palette.PaperLineStrong,
        outlineVariant = Palette.PaperLine,
        scrim = Color.Black,
        surfaceBright = Palette.Paper,
        surfaceDim = Palette.Paper4,
        surfaceContainerLowest = Palette.Paper,
        surfaceContainerLow = Palette.Paper2,
        surfaceContainer = Palette.Paper3,
        surfaceContainerHigh = Color(0xFFECECEF),
        surfaceContainerHighest = Palette.Paper4,
    )

val AcolyteShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

object Radius {
    val Button = 10.dp
    val Card = 12.dp
    val Poster = 12.dp
    val Panel = 16.dp
    val Sheet = 20.dp
}

object Spacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
    val Screen = 16.dp
}

private val Base = Typography()
private const val TABULAR = "tnum"

val AcolyteTypography =
    Typography(
        displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.022).em),
        headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.022).em),
        headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
        headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.018).em),
        titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.014).em),
        titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold),
    )

private val LocalAcolyteColors = staticCompositionLocalOf { DarkAcolyte }
private val LocalDarkTheme = staticCompositionLocalOf { true }

val MaterialTheme.acolyte: AcolyteColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAcolyteColors.current

val MaterialTheme.isDark: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalDarkTheme.current

@Composable
fun AcolyteTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAcolyteColors provides if (dark) DarkAcolyte else LightAcolyte,
        LocalDarkTheme provides dark,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AcolyteTypography,
            shapes = AcolyteShapes,
            content = content,
        )
    }
}
