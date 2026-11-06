package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.OptiShareCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SegmentedSelectorBar
import com.example.ui.components.TelemetryMetricCell
import com.example.ui.theme.AccentPalettePreset
import com.example.ui.theme.AppCustomizationState
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.DesignStyle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ThemeModeOption
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit,
    onApplyPreset: (String) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier,
    activeRefreshRateHz: Int = 120
) {
    val colorScheme = MaterialTheme.colorScheme
    var showResetConfirmation by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val isWideLayout = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isWideLayout) 1140.dp else 640.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Interactive Preview Card
            LiveDesignBenchPreviewCard(
                customization = customization,
                activeRefreshRateHz = activeRefreshRateHz
            )

            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DesignEngineSelectorPanel(
                            customization = customization,
                            onUpdateCustomization = onUpdateCustomization,
                            onApplyPreset = onApplyPreset
                        )
                        SurfaceCalibrationPanel(
                            customization = customization,
                            onUpdateCustomization = onUpdateCustomization
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ColorAndLuminancePanel(
                            customization = customization,
                            onUpdateCustomization = onUpdateCustomization
                        )
                        TypographyCalibrationPanel(
                            customization = customization,
                            onUpdateCustomization = onUpdateCustomization
                        )
                        ResetDefaultsPanel(
                            onRequestReset = { showResetConfirmation = true }
                        )
                    }
                }
            } else {
                DesignEngineSelectorPanel(
                    customization = customization,
                    onUpdateCustomization = onUpdateCustomization,
                    onApplyPreset = onApplyPreset
                )
                SurfaceCalibrationPanel(
                    customization = customization,
                    onUpdateCustomization = onUpdateCustomization
                )
                ColorAndLuminancePanel(
                    customization = customization,
                    onUpdateCustomization = onUpdateCustomization
                )
                TypographyCalibrationPanel(
                    customization = customization,
                    onUpdateCustomization = onUpdateCustomization
                )
                ResetDefaultsPanel(
                    onRequestReset = { showResetConfirmation = true }
                )
            }

            Spacer(modifier = Modifier.height(if (isWideLayout) 16.dp else 104.dp))
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Ripristina Calibrazione Visiva") },
            text = {
                Text("Vuoi ripristinare tutti i parametri di stile, colori, superfici e tipografia ai valori di fabbrica Swiss Instrument?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetConfirmation = false
                    }
                ) {
                    Text("Ripristina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun LiveDesignBenchPreviewCard(
    customization: AppCustomizationState,
    activeRefreshRateHz: Int = 120
) {
    val colorScheme = MaterialTheme.colorScheme
    val isGlass = customization.designStyle == DesignStyle.LIQUID_GLASS

    OptiShareCard(
        isHighlighted = true,
        accentTint = colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_live_preview_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeaderRow(
                overline = if (isGlass) "Stile Liquid Glass" else "Stile Material You (M3)",
                title = "Anteprima Personalizzazione"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isGlass) Icons.Default.BlurOn else Icons.Default.Layers,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = customization.designStyle.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${customization.palette.label} • ${customization.themeMode.label} • ${customization.fontChoice.label}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetricCell(
                    label = "RAGGIO CURVATURA",
                    value = if (isGlass) "${customization.glassCornerRadiusDp} dp" else "${customization.materialCornerRadiusDp} dp",
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCell(
                    label = "FREQUENZA DISPLAY",
                    value = "${activeRefreshRateHz} Hz VSync",
                    valueColor = colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCell(
                    label = "SCALA TESTO",
                    value = "${(customization.fontScale * 100).roundToInt()}%",
                    valueColor = colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = colorScheme.tertiary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934c…",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = colorScheme.tertiary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesignEngineSelectorPanel(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit,
    onApplyPreset: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeaderRow(
                overline = "Tema e superfici",
                title = "Stile dell'Interfaccia"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DesignStyle.entries.forEach { style ->
                    val selected = customization.designStyle == style
                    val tag = if (style == DesignStyle.LIQUID_GLASS) {
                        "style_liquid_glass_button"
                    } else {
                        "style_material_button"
                    }
                    val shape = RoundedCornerShape(10.dp)

                    Surface(
                        shape = shape,
                        color = if (selected) {
                            colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.6f),
                                shape = shape
                            )
                            .clickable {
                                onUpdateCustomization { it.copy(designStyle = style) }
                            }
                            .testTag(tag)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (style == DesignStyle.LIQUID_GLASS) Icons.Default.BlurOn else Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) colorScheme.primary else colorScheme.onSurface
                            )
                            Text(
                                text = style.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

            Text(
                text = "PROFILI STUDIO PRE-CALIBRATI",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "SWISS_INSTRUMENT" to "Material You (Predefinito)",
                    "PAPER_EDITORIAL" to "Material Chiaro Salvia",
                    "SMOKED_ACRYLIC_GLASS" to "Liquid Glass Scuro",
                    "MONOCHROME_TERMINAL" to "Material Scuro Ardesia"
                ).forEach { (key, label) ->
                    FilledTonalButton(
                        onClick = { onApplyPreset(key) },
                        modifier = Modifier.testTag("preset_$key")
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fluidità Schermo 120Hz (High Refresh Rate)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Richiede la modalità hardware 120Hz / ProMotion e sincronizza il flusso QR al VSync (8.33 ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = customization.preferHighRefreshRate,
                    onCheckedChange = { enabled ->
                        onUpdateCustomization { it.copy(preferHighRefreshRate = enabled) }
                    },
                    modifier = Modifier.testTag("switch_high_refresh_rate_120hz")
                )
            }
        }
    }
}

@Composable
private fun SurfaceCalibrationPanel(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    AnimatedVisibility(visible = customization.designStyle == DesignStyle.LIQUID_GLASS) {
        OptiShareCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeaderRow(
                    overline = "ACRYLIC OPTICS",
                    title = "Calibrazione Superfici Liquid Glass"
                )

                SliderSettingRow(
                    title = "Opacità Acrilico (Frosted Alpha)",
                    valueText = "${(customization.glassOpacity * 100).roundToInt()}%",
                    value = customization.glassOpacity,
                    valueRange = 0.08f..0.75f,
                    onValueChange = { v -> onUpdateCustomization { it.copy(glassOpacity = v) } },
                    testTag = "slider_glass_opacity"
                )

                SliderSettingRow(
                    title = "Incisione Speculare Bordi (Rim Precision)",
                    valueText = "${(customization.glassSpecularIntensity * 100).roundToInt()}%",
                    value = customization.glassSpecularIntensity,
                    valueRange = 0.05f..1.0f,
                    onValueChange = { v -> onUpdateCustomization { it.copy(glassSpecularIntensity = v) } },
                    testTag = "slider_glass_specular"
                )

                SliderSettingRow(
                    title = "Diffusione Cromatica Perimetrale",
                    valueText = "${(customization.glassRefractionGlow * 100).roundToInt()}%",
                    value = customization.glassRefractionGlow,
                    valueRange = 0.0f..1.0f,
                    onValueChange = { v -> onUpdateCustomization { it.copy(glassRefractionGlow = v) } },
                    testTag = "slider_glass_refraction"
                )

                SliderSettingRow(
                    title = "Raggio Curvatura Lente",
                    valueText = "${customization.glassCornerRadiusDp} dp",
                    value = customization.glassCornerRadiusDp.toFloat(),
                    valueRange = 6f..32f,
                    onValueChange = { v -> onUpdateCustomization { it.copy(glassCornerRadiusDp = v.roundToInt()) } },
                    testTag = "slider_glass_radius"
                )

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Retroilluminazione Radiale Sottile", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Diffusione atmosferica calibrata sotto i pannelli traslucidi",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = customization.glassAmbientOrbsEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateCustomization { it.copy(glassAmbientOrbsEnabled = enabled) }
                        },
                        modifier = Modifier.testTag("switch_glass_orbs")
                    )
                }

                if (customization.glassAmbientOrbsEnabled) {
                    SliderSettingRow(
                        title = "Intensità Diffusione Radiale",
                        valueText = "${(customization.glassAmbientOrbIntensity * 100).roundToInt()}%",
                        value = customization.glassAmbientOrbIntensity,
                        valueRange = 0.1f..1.0f,
                        onValueChange = { v -> onUpdateCustomization { it.copy(glassAmbientOrbIntensity = v) } },
                        testTag = "slider_glass_orb_intensity"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Menisco Speculare Superiore", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Filo di luce superiore da 1dp lungo i contenitori",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = customization.glassInnerHighlightEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateCustomization { it.copy(glassInnerHighlightEnabled = enabled) }
                        }
                    )
                }
            }
        }
    }

    AnimatedVisibility(visible = customization.designStyle == DesignStyle.MATERIAL) {
        OptiShareCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeaderRow(
                    overline = "STRUCTURAL M3",
                    title = "Geometria & Bordi Material Precision"
                )

                SliderSettingRow(
                    title = "Raggio Angoli Componenti",
                    valueText = "${customization.materialCornerRadiusDp} dp",
                    value = customization.materialCornerRadiusDp.toFloat(),
                    valueRange = 0f..28f,
                    onValueChange = { v -> onUpdateCustomization { it.copy(materialCornerRadiusDp = v.roundToInt()) } },
                    testTag = "slider_material_radius"
                )

                SliderSettingRow(
                    title = "Spessore Bordo Tecnico (Hairline)",
                    valueText = String.format(Locale.US, "%.1f dp", customization.materialBorderWidthDp),
                    value = customization.materialBorderWidthDp,
                    valueRange = 0f..2.5f,
                    onValueChange = { v -> onUpdateCustomization { it.copy(materialBorderWidthDp = v) } },
                    testTag = "slider_material_border"
                )

                SliderSettingRow(
                    title = "Densità Superficie Pannelli",
                    valueText = "${(customization.materialSurfaceContrast * 100).roundToInt()}%",
                    value = customization.materialSurfaceContrast,
                    valueRange = 0.35f..1.0f,
                    onValueChange = { v -> onUpdateCustomization { it.copy(materialSurfaceContrast = v) } },
                    testTag = "slider_material_contrast"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Elevazione Tonale Material 3", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Applica gerarchia tonale alle superfici principali",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = customization.materialUseTonalElevation,
                        onCheckedChange = { enabled ->
                            onUpdateCustomization { it.copy(materialUseTonalElevation = enabled) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorAndLuminancePanel(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeaderRow(
                overline = "MODALITÀ: ${customization.themeMode.label.uppercase()}",
                title = "Luminanza & Sistema Cromatico"
            )

            SegmentedSelectorBar(
                items = ThemeModeOption.entries,
                selectedItem = customization.themeMode,
                onItemSelected = { mode ->
                    onUpdateCustomization { it.copy(themeMode = mode) }
                },
                labelProvider = { it.label }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Colori Dinamici di Sistema (Android 12+)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Sincronizza gli accenti con la palette del sistema operativo",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = customization.useDynamicColor,
                        onCheckedChange = { enabled ->
                            onUpdateCustomization { it.copy(useDynamicColor = enabled) }
                        },
                        modifier = Modifier.testTag("switch_dynamic_color")
                    )
                }
            }

            if (!customization.useDynamicColor) {
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f))

                Text(
                    text = "ACCENTO STRUMENTALE",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccentPalettePreset.entries.forEach { preset ->
                        val selected = customization.palette == preset
                        val shape = RoundedCornerShape(8.dp)

                        Surface(
                            shape = shape,
                            color = if (selected) {
                                preset.primaryDark.copy(alpha = 0.16f)
                            } else {
                                colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            modifier = Modifier
                                .clip(shape)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) preset.primaryDark else colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    shape = shape
                                )
                                .clickable {
                                    onUpdateCustomization { it.copy(palette = preset) }
                                }
                                .testTag("palette_${preset.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(preset.primaryDark)
                                )
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypographyCalibrationPanel(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit
) {
    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeaderRow(
                overline = "FONT OFL • ${customization.fontChoice.name}",
                title = "Tipografia & Scala Editoriale"
            )

            SegmentedSelectorBar(
                items = AppFontChoice.entries,
                selectedItem = customization.fontChoice,
                onItemSelected = { choice ->
                    onUpdateCustomization { it.copy(fontChoice = choice) }
                },
                labelProvider = { it.label }
            )

            SliderSettingRow(
                title = "Fattore di Scala Tipografica",
                valueText = "${(customization.fontScale * 100).roundToInt()}%",
                value = customization.fontScale,
                valueRange = 0.85f..1.20f,
                onValueChange = { v -> onUpdateCustomization { it.copy(fontScale = v) } },
                testTag = "slider_font_scale"
            )
        }
    }
}

@Composable
private fun ResetDefaultsPanel(
    onRequestReset: () -> Unit
) {
    OutlinedButton(
        onClick = onRequestReset,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_theme_defaults_button")
    ) {
        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Ripristina Calibrazione Predefinita")
    }
}

@Composable
private fun SliderSettingRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
