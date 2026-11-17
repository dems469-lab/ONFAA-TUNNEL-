package com.onfaa.tunnel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Design tokens that are not colours. Kept in one place for UI consistency. */
object OnfaaDimens {
    val screenHorizontal = 20.dp
    val screenTop = 12.dp
    val sectionGap = 20.dp
    val cardPadding = 16.dp
    val cardGap = 10.dp
    val controlHeight = 50.dp
    val topBarHeight = 56.dp
    val orbSize = 226.dp
    val iconButton = 44.dp
    val cardRadius = 20.dp
    val pillRadius = 99.dp
    val bottomBarHeight = 72.dp
}

private val OnfaaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

private val OnfaaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun OnfaaPalette.toColorScheme() = if (isDark) {
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = surfaceElevated,
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color(0xFF00121F),
        secondaryContainer = surfaceElevated,
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = Color(0xFF16001F),
        background = backgroundTop,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceElevated,
        onSurfaceVariant = textSecondary,
        outline = border,
        outlineVariant = border.copy(alpha = 0.6f),
        error = danger,
        onError = Color(0xFF200004),
        scrim = Color(0xFF000000)
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = surfaceElevated,
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = surfaceElevated,
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = Color.White,
        background = backgroundTop,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceElevated,
        onSurfaceVariant = textSecondary,
        outline = border,
        outlineVariant = border.copy(alpha = 0.6f),
        error = danger,
        onError = Color.White,
        scrim = Color(0x66000000)
    )
}

object OnfaaTheme {
    val colors: OnfaaPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalOnfaaPalette.current
}

@Composable
fun OnfaaTunnelTheme(
    settings: ThemeSettings,
    content: @Composable () -> Unit
) {
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = remember(settings.paletteId, dark) { paletteFor(settings.paletteId, dark) }
    CompositionLocalProvider(LocalOnfaaPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = OnfaaTypography,
            shapes = OnfaaShapes,
            content = content
        )
    }
}

/** Backwards-compatible entry point used by previews and simple hosts. */
@Composable
fun OnfaaTunnelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    OnfaaTunnelTheme(
        settings = ThemeSettings(
            paletteId = ThemePaletteId.ONFAA,
            mode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
        ),
        content = content
    )
}
