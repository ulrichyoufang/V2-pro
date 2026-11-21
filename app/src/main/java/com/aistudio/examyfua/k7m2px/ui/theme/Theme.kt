package com.aistudio.examyfua.k7m2px.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class YfuaExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val cardBorder: Color,
    val headline: Color,
    val bodySlate: Color,
    val avatarTeal: Color,
    val isDark: Boolean
)

private val LightExtendedColors = YfuaExtendedColors(
    success = InstitutionalEmerald,
    onSuccess = PureWhite,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
    warning = AmberWarning,
    warningContainer = WarningContainerLight,
    cardBorder = BorderLight,
    headline = HeadlineLight,
    bodySlate = BodySlateLight,
    avatarTeal = DeepPetrolTeal,
    isDark = false
)

private val DarkExtendedColors = YfuaExtendedColors(
    success = DarkSuccess,
    onSuccess = DarkBackground,
    successContainer = DarkSuccessContainer,
    onSuccessContainer = DarkOnSuccessContainer,
    warning = AmberWarningDark,
    warningContainer = WarningContainerDark,
    cardBorder = DarkBorder,
    headline = HeadlineDark,
    bodySlate = BodySlateDark,
    avatarTeal = DeepPetrolTeal,
    isDark = true
)

val LocalYfuaColors = staticCompositionLocalOf { LightExtendedColors }

private val YfuaLightColorScheme = lightColorScheme(
    primary = DeepPetrolTeal,
    onPrimary = PureWhite,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = AcademicCobaltBlue,
    onSecondary = PureWhite,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = BerryMagenta,
    onTertiary = PureWhite,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = HeadlineLight,
    surface = SurfaceCardLight,
    onSurface = HeadlineLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = BodySlateLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = CoralRed,
    onError = PureWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = CoralRed
)

private val YfuaDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = HeadlineDark,
    surface = DarkCard,
    onSurface = HeadlineDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = BodySlateDark,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = CoralRedDark,
    onError = DarkBackground,
    errorContainer = ErrorContainerDark,
    onErrorContainer = CoralRedDark
)

private val YfuaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

object YfuaTheme {
    val colors: YfuaExtendedColors
        @Composable
        get() = LocalYfuaColors.current
}

@Composable
fun YfuaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) YfuaDarkColorScheme else YfuaLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalYfuaColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = YfuaTypography,
            shapes = YfuaShapes,
            content = content
        )
    }
}
