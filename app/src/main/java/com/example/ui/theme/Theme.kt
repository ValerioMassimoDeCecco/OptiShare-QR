package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OpticalCyan,
    onPrimary = Color(0xFF00272E),
    primaryContainer = OpticalCyanContainer,
    onPrimaryContainer = OnOpticalCyanContainer,
    secondary = QuantumViolet,
    onSecondary = Color.White,
    secondaryContainer = QuantumVioletContainer,
    onSecondaryContainer = OnQuantumVioletContainer,
    tertiary = VerifiedEmerald,
    onTertiary = Color(0xFF002912),
    tertiaryContainer = VerifiedEmeraldContainer,
    onTertiaryContainer = OnVerifiedEmeraldContainer,
    error = SignalCoral,
    background = ObsidianBackground,
    onBackground = Color(0xFFEDF2FA),
    surface = SlateSurface,
    onSurface = Color(0xFFEDF2FA),
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = Color(0xFFA9B7CE),
    outline = Color(0xFF31415E)
)

private val LightColorScheme = lightColorScheme(
    primary = LightCyanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAEFFF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = LightVioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5DEFF),
    onSecondaryContainer = Color(0xFF180065),
    tertiary = LightEmeraldTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9DF7B5),
    onTertiaryContainer = Color(0xFF00210C),
    error = SignalCoral,
    background = LightBackground,
    onBackground = Color(0xFF101726),
    surface = LightSurface,
    onSurface = Color(0xFF101726),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF425066),
    outline = Color(0xFFC2CDDF)
)

@Composable
fun OptiShareTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
