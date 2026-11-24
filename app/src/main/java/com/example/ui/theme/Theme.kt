package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeoCamoDarkColorScheme = darkColorScheme(
    primary = EmeraldSignal,
    onPrimary = Color(0xFF00241A),
    primaryContainer = TealSurfaceTint,
    onPrimaryContainer = Color(0xFF7AF5D2),
    secondary = EmeraldGlow,
    onSecondary = Color(0xFF00210E),
    secondaryContainer = Color(0xFF153A2B),
    onSecondaryContainer = Color(0xFF9DF7BA),
    tertiary = AlertAmber,
    onTertiary = Color(0xFF281800),
    background = RadarObsidian,
    onBackground = TextPrimaryDark,
    surface = RadarSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = RadarCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = RadarCardElevated,
    outline = RadarOutline,
    error = AlertRed,
    onError = Color.White
)

private val GeoCamoLightColorScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDF5E4),
    onPrimaryContainer = Color(0xFF00291E),
    secondary = Color(0xFF128C7E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1F4EA),
    onSecondaryContainer = Color(0xFF073B35),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCardVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = Color(0xFFE0ECE8),
    outline = LightOutline,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun GeoCamoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GeoCamoDarkColorScheme else GeoCamoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
