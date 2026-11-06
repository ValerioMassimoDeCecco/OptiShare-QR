package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun OptiShareTheme(
    customization: AppCustomizationState = AppCustomizationState(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (customization.themeMode) {
        ThemeModeOption.DARK, ThemeModeOption.AMOLED -> true
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.SYSTEM -> systemDark
    }
    val isAmoled = customization.themeMode == ThemeModeOption.AMOLED
    val palette = customization.palette

    val baseScheme = if (customization.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (isDark) {
        val primaryContainer = when (palette) {
            AccentPalettePreset.SIGNAL_COBALT -> CobaltContainerDark
            AccentPalettePreset.INSTRUMENT_ORANGE -> OrangeContainerDark
            AccentPalettePreset.OPTICAL_CYAN -> CyanContainerDark
            AccentPalettePreset.EMERALD_LAB -> GreenContainerDark
            AccentPalettePreset.PHOSPHOR_AMBER -> AmberContainerDark
            AccentPalettePreset.MONOCHROME_ZINC -> Color(0xFF343740)
        }
        val onPrimaryContainer = when (palette) {
            AccentPalettePreset.SIGNAL_COBALT -> OnCobaltContainerDark
            AccentPalettePreset.INSTRUMENT_ORANGE -> OnOrangeContainerDark
            AccentPalettePreset.OPTICAL_CYAN -> OnCyanContainerDark
            AccentPalettePreset.EMERALD_LAB -> OnGreenContainerDark
            AccentPalettePreset.PHOSPHOR_AMBER -> OnAmberContainerDark
            AccentPalettePreset.MONOCHROME_ZINC -> Color(0xFFE2E2E9)
        }
        darkColorScheme(
            primary = palette.primaryDark,
            onPrimary = Color(0xFF0A192F),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = palette.secondaryDark,
            onSecondary = Color(0xFF0A192F),
            secondaryContainer = primaryContainer.copy(alpha = 0.85f),
            onSecondaryContainer = onPrimaryContainer,
            tertiary = palette.tertiaryDark,
            onTertiary = Color(0xFF002116),
            tertiaryContainer = GreenContainerDark,
            onTertiaryContainer = OnGreenContainerDark,
            error = DangerCrimson,
            onError = Color(0xFF690005),
            errorContainer = ErrorContainerDark,
            onErrorContainer = OnErrorContainerDark,
            background = if (isAmoled) Color.Black else M3BackgroundDark,
            onBackground = M3OnSurfaceDark,
            surface = if (isAmoled) Color(0xFF0A0C10) else M3SurfaceDark,
            onSurface = M3OnSurfaceDark,
            surfaceVariant = if (isAmoled) Color(0xFF181B22) else M3SurfaceVariantDark,
            onSurfaceVariant = M3OnSurfaceVariantDark,
            outline = M3OutlineDark,
            outlineVariant = M3OutlineVariantDark
        )
    } else {
        val primaryContainer = when (palette) {
            AccentPalettePreset.SIGNAL_COBALT -> CobaltContainerLight
            AccentPalettePreset.INSTRUMENT_ORANGE -> OrangeContainerLight
            AccentPalettePreset.OPTICAL_CYAN -> CyanContainerLight
            AccentPalettePreset.EMERALD_LAB -> GreenContainerLight
            AccentPalettePreset.PHOSPHOR_AMBER -> AmberContainerLight
            AccentPalettePreset.MONOCHROME_ZINC -> Color(0xFFDADCE4)
        }
        val onPrimaryContainer = when (palette) {
            AccentPalettePreset.SIGNAL_COBALT -> OnCobaltContainerLight
            AccentPalettePreset.INSTRUMENT_ORANGE -> OnOrangeContainerLight
            AccentPalettePreset.OPTICAL_CYAN -> OnCyanContainerLight
            AccentPalettePreset.EMERALD_LAB -> OnGreenContainerLight
            AccentPalettePreset.PHOSPHOR_AMBER -> OnAmberContainerLight
            AccentPalettePreset.MONOCHROME_ZINC -> Color(0xFF191C20)
        }
        lightColorScheme(
            primary = palette.primaryLight,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = palette.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = primaryContainer.copy(alpha = 0.75f),
            onSecondaryContainer = onPrimaryContainer,
            tertiary = palette.tertiaryLight,
            onTertiary = Color.White,
            tertiaryContainer = GreenContainerLight,
            onTertiaryContainer = OnGreenContainerLight,
            error = DangerCrimsonLight,
            onError = Color.White,
            errorContainer = ErrorContainerLight,
            onErrorContainer = OnErrorContainerLight,
            background = M3BackgroundLight,
            onBackground = M3OnSurfaceLight,
            surface = Color.White,
            onSurface = M3OnSurfaceLight,
            surfaceVariant = M3SurfaceVariantLight,
            onSurfaceVariant = M3OnSurfaceVariantLight,
            outline = M3OutlineLight,
            outlineVariant = M3OutlineVariantLight
        )
    }

    val finalScheme = if (isAmoled && customization.useDynamicColor) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color(0xFF0A0C10)
        )
    } else {
        baseScheme
    }

    val activeRadius = if (customization.designStyle == DesignStyle.LIQUID_GLASS) {
        customization.glassCornerRadiusDp
    } else {
        customization.materialCornerRadiusDp
    }.coerceIn(4, 24)

    // Moderate Material 3 rounded shapes (avoiding making every element a pill)
    val customShapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(activeRadius.dp),
        large = RoundedCornerShape((activeRadius + 4).coerceAtMost(24).dp),
        extraLarge = RoundedCornerShape((activeRadius + 8).coerceAtMost(28).dp)
    )

    val customTypography = buildCustomTypography(
        fontChoice = customization.fontChoice,
        fontScale = customization.fontScale
    )

    CompositionLocalProvider(LocalAppCustomization provides customization) {
        MaterialTheme(
            colorScheme = finalScheme,
            typography = customTypography,
            shapes = customShapes,
            content = content
        )
    }
}
