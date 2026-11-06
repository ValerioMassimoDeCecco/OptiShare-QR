package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DesignStyle(val label: String, val subtitle: String) {
    MATERIAL("Material You (M3)", "Superfici tonali coordinate, forme morbide bilanciate ed elevazione leggera"),
    LIQUID_GLASS("Liquid Glass", "Superfici traslucide opzionali con bordi luminosi personalizzabili")
}

enum class ThemeModeOption(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Chiaro"),
    DARK("Scuro"),
    AMOLED("AMOLED")
}

enum class AccentPalettePreset(
    val label: String,
    val primaryDark: Color,
    val secondaryDark: Color,
    val tertiaryDark: Color,
    val primaryLight: Color,
    val secondaryLight: Color,
    val tertiaryLight: Color
) {
    SIGNAL_COBALT(
        label = "Material Blue",
        primaryDark = SignalCobalt,
        secondaryDark = OpticCyan,
        tertiaryDark = VerifiedGreen,
        primaryLight = SignalCobaltLight,
        secondaryLight = OpticCyanLight,
        tertiaryLight = VerifiedGreenLight
    ),
    INSTRUMENT_ORANGE(
        label = "Terracotta",
        primaryDark = InstrumentOrange,
        secondaryDark = PhosphorAmber,
        tertiaryDark = VerifiedGreen,
        primaryLight = InstrumentOrangeLight,
        secondaryLight = PhosphorAmberLight,
        tertiaryLight = VerifiedGreenLight
    ),
    OPTICAL_CYAN(
        label = "Acqua Marina",
        primaryDark = OpticCyan,
        secondaryDark = SignalCobalt,
        tertiaryDark = VerifiedGreen,
        primaryLight = OpticCyanLight,
        secondaryLight = SignalCobaltLight,
        tertiaryLight = VerifiedGreenLight
    ),
    EMERALD_LAB(
        label = "Salvia Verde",
        primaryDark = VerifiedGreen,
        secondaryDark = OpticCyan,
        tertiaryDark = PhosphorAmber,
        primaryLight = VerifiedGreenLight,
        secondaryLight = OpticCyanLight,
        tertiaryLight = PhosphorAmberLight
    ),
    PHOSPHOR_AMBER(
        label = "Ambra Calda",
        primaryDark = PhosphorAmber,
        secondaryDark = InstrumentOrange,
        tertiaryDark = VerifiedGreen,
        primaryLight = PhosphorAmberLight,
        secondaryLight = InstrumentOrangeLight,
        tertiaryLight = VerifiedGreenLight
    ),
    MONOCHROME_ZINC(
        label = "Ardesia",
        primaryDark = Color(0xFFC4C6D0),
        secondaryDark = Color(0xFF9094A0),
        tertiaryDark = VerifiedGreen,
        primaryLight = Color(0xFF393C44),
        secondaryLight = Color(0xFF5C5F68),
        tertiaryLight = VerifiedGreenLight
    )
}

enum class AppFontChoice(val label: String) {
    PLUS_JAKARTA("Plus Jakarta Sans"),
    SPACE_GROTESK("Space Grotesk"),
    JETBRAINS_MONO("JetBrains Mono")
}

data class AppCustomizationState(
    val designStyle: DesignStyle = DesignStyle.MATERIAL,
    val themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    val palette: AccentPalettePreset = AccentPalettePreset.SIGNAL_COBALT,
    val useDynamicColor: Boolean = true,
    val fontChoice: AppFontChoice = AppFontChoice.PLUS_JAKARTA,
    val fontScale: Float = 1.0f,

    // Material Design 3 Customizations
    val materialCornerRadiusDp: Int = 16,
    val materialBorderWidthDp: Float = 1.0f,
    val materialSurfaceContrast: Float = 1.0f,
    val materialUseTonalElevation: Boolean = true,

    // Liquid Glass Customizations (Optional Secondary Mode)
    val glassOpacity: Float = 0.72f,
    val glassSpecularIntensity: Float = 0.35f,
    val glassRefractionGlow: Float = 0.15f,
    val glassCornerRadiusDp: Int = 16,
    val glassAmbientOrbsEnabled: Boolean = false,
    val glassAmbientOrbIntensity: Float = 0.25f,
    val glassInnerHighlightEnabled: Boolean = true
)

val LocalAppCustomization = compositionLocalOf { AppCustomizationState() }

class ThemeSettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("optishare_theme_m3_you_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadFromPrefs())
    val state: StateFlow<AppCustomizationState> = _state.asStateFlow()

    private fun loadFromPrefs(): AppCustomizationState {
        return AppCustomizationState(
            designStyle = runCatching {
                DesignStyle.valueOf(prefs.getString("designStyle", DesignStyle.MATERIAL.name)!!)
            }.getOrDefault(DesignStyle.MATERIAL),
            themeMode = runCatching {
                ThemeModeOption.valueOf(prefs.getString("themeMode", ThemeModeOption.SYSTEM.name)!!)
            }.getOrDefault(ThemeModeOption.SYSTEM),
            palette = runCatching {
                AccentPalettePreset.valueOf(prefs.getString("palette", AccentPalettePreset.SIGNAL_COBALT.name)!!)
            }.getOrDefault(AccentPalettePreset.SIGNAL_COBALT),
            useDynamicColor = prefs.getBoolean("useDynamicColor", true),
            fontChoice = runCatching {
                AppFontChoice.valueOf(prefs.getString("fontChoice", AppFontChoice.PLUS_JAKARTA.name)!!)
            }.getOrDefault(AppFontChoice.PLUS_JAKARTA),
            fontScale = prefs.getFloat("fontScale", 1.0f).coerceIn(0.85f, 1.20f),
            materialCornerRadiusDp = prefs.getInt("materialCornerRadiusDp", 16).coerceIn(0, 28),
            materialBorderWidthDp = prefs.getFloat("materialBorderWidthDp", 1.0f).coerceIn(0f, 2.5f),
            materialSurfaceContrast = prefs.getFloat("materialSurfaceContrast", 1.0f).coerceIn(0.3f, 1.0f),
            materialUseTonalElevation = prefs.getBoolean("materialUseTonalElevation", true),
            glassOpacity = prefs.getFloat("glassOpacity", 0.72f).coerceIn(0.15f, 0.92f),
            glassSpecularIntensity = prefs.getFloat("glassSpecularIntensity", 0.35f).coerceIn(0.05f, 1.0f),
            glassRefractionGlow = prefs.getFloat("glassRefractionGlow", 0.15f).coerceIn(0.0f, 1.0f),
            glassCornerRadiusDp = prefs.getInt("glassCornerRadiusDp", 16).coerceIn(4, 32),
            glassAmbientOrbsEnabled = prefs.getBoolean("glassAmbientOrbsEnabled", false),
            glassAmbientOrbIntensity = prefs.getFloat("glassAmbientOrbIntensity", 0.25f).coerceIn(0.05f, 1.0f),
            glassInnerHighlightEnabled = prefs.getBoolean("glassInnerHighlightEnabled", true)
        )
    }

    fun update(transform: (AppCustomizationState) -> AppCustomizationState) {
        _state.update { current ->
            val next = transform(current)
            saveToPrefs(next)
            next
        }
    }

    fun resetToDefaults() {
        update { AppCustomizationState() }
    }

    fun applyQuickPreset(presetName: String) {
        when (presetName) {
            "SWISS_INSTRUMENT" -> update {
                AppCustomizationState(
                    designStyle = DesignStyle.MATERIAL,
                    themeMode = ThemeModeOption.SYSTEM,
                    palette = AccentPalettePreset.SIGNAL_COBALT,
                    useDynamicColor = true,
                    fontChoice = AppFontChoice.PLUS_JAKARTA,
                    materialCornerRadiusDp = 16,
                    materialBorderWidthDp = 1.0f,
                    materialSurfaceContrast = 1.0f,
                    materialUseTonalElevation = true
                )
            }
            "PAPER_EDITORIAL" -> update {
                it.copy(
                    designStyle = DesignStyle.MATERIAL,
                    themeMode = ThemeModeOption.LIGHT,
                    palette = AccentPalettePreset.EMERALD_LAB,
                    useDynamicColor = false,
                    fontChoice = AppFontChoice.PLUS_JAKARTA,
                    materialCornerRadiusDp = 16,
                    materialBorderWidthDp = 1.0f,
                    materialSurfaceContrast = 1.0f,
                    materialUseTonalElevation = true
                )
            }
            "SMOKED_ACRYLIC_GLASS" -> update {
                it.copy(
                    designStyle = DesignStyle.LIQUID_GLASS,
                    themeMode = ThemeModeOption.DARK,
                    palette = AccentPalettePreset.SIGNAL_COBALT,
                    glassOpacity = 0.72f,
                    glassSpecularIntensity = 0.35f,
                    glassRefractionGlow = 0.15f,
                    glassCornerRadiusDp = 16,
                    glassAmbientOrbsEnabled = false
                )
            }
            "MONOCHROME_TERMINAL" -> update {
                it.copy(
                    designStyle = DesignStyle.MATERIAL,
                    themeMode = ThemeModeOption.DARK,
                    palette = AccentPalettePreset.MONOCHROME_ZINC,
                    useDynamicColor = false,
                    fontChoice = AppFontChoice.SPACE_GROTESK,
                    materialCornerRadiusDp = 12,
                    materialBorderWidthDp = 1.0f,
                    materialSurfaceContrast = 1.0f,
                    materialUseTonalElevation = true
                )
            }
        }
    }

    private fun saveToPrefs(s: AppCustomizationState) {
        prefs.edit()
            .putString("designStyle", s.designStyle.name)
            .putString("themeMode", s.themeMode.name)
            .putString("palette", s.palette.name)
            .putBoolean("useDynamicColor", s.useDynamicColor)
            .putString("fontChoice", s.fontChoice.name)
            .putFloat("fontScale", s.fontScale)
            .putInt("materialCornerRadiusDp", s.materialCornerRadiusDp)
            .putFloat("materialBorderWidthDp", s.materialBorderWidthDp)
            .putFloat("materialSurfaceContrast", s.materialSurfaceContrast)
            .putBoolean("materialUseTonalElevation", s.materialUseTonalElevation)
            .putFloat("glassOpacity", s.glassOpacity)
            .putFloat("glassSpecularIntensity", s.glassSpecularIntensity)
            .putFloat("glassRefractionGlow", s.glassRefractionGlow)
            .putInt("glassCornerRadiusDp", s.glassCornerRadiusDp)
            .putBoolean("glassAmbientOrbsEnabled", s.glassAmbientOrbsEnabled)
            .putFloat("glassAmbientOrbIntensity", s.glassAmbientOrbIntensity)
            .putBoolean("glassInnerHighlightEnabled", s.glassInnerHighlightEnabled)
            .apply()
    }
}
