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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.OptiShareCard
import com.example.ui.components.SegmentedSelectorBar
import com.example.ui.components.rememberOptiShareHaptics
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
                .widthIn(max = if (isWideLayout) 1040.dp else 580.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DesignAndThemeCard(
                            customization = customization,
                            onUpdateCustomization = onUpdateCustomization
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DisplayAndTypographyCard(
                            customization = customization,
                            activeRefreshRateHz = activeRefreshRateHz,
                            onUpdateCustomization = onUpdateCustomization
                        )
                        AdvancedAppearanceAccordion(
                            customization = customization,
                            onUpdateCustomization = onUpdateCustomization,
                            onApplyPreset = onApplyPreset,
                            onRequestReset = { showResetConfirmation = true }
                        )
                    }
                }
            } else {
                DesignAndThemeCard(
                    customization = customization,
                    onUpdateCustomization = onUpdateCustomization
                )
                DisplayAndTypographyCard(
                    customization = customization,
                    activeRefreshRateHz = activeRefreshRateHz,
                    onUpdateCustomization = onUpdateCustomization
                )
                AdvancedAppearanceAccordion(
                    customization = customization,
                    onUpdateCustomization = onUpdateCustomization,
                    onApplyPreset = onApplyPreset,
                    onRequestReset = { showResetConfirmation = true }
                )
            }

            Spacer(modifier = Modifier.height(if (isWideLayout) 16.dp else 104.dp))
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Ripristina aspetto predefinito") },
            text = {
                Text("Vuoi ripristinare stile, colori e tipografia ai valori predefiniti?")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesignAndThemeCard(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptics = rememberOptiShareHaptics()

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Aspetto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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
                    val shape = RoundedCornerShape(12.dp)

                    Surface(
                        shape = shape,
                        color = if (selected) {
                            colorScheme.secondaryContainer
                        } else {
                            colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.45f),
                                shape = shape
                            )
                            .clickable {
                                if (!selected) haptics.selectionTick()
                                onUpdateCustomization { it.copy(designStyle = style) }
                            }
                            .testTag(tag)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (style == DesignStyle.LIQUID_GLASS) Icons.Default.BlurOn else Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (style == DesignStyle.LIQUID_GLASS) "Liquid Glass" else "Material You",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurface
                                )
                            }
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

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
                    Text(
                        text = "Colori dinamici di sistema",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = customization.useDynamicColor,
                        onCheckedChange = { enabled ->
                            haptics.toggle(enabled)
                            onUpdateCustomization { it.copy(useDynamicColor = enabled) }
                        },
                        modifier = Modifier.testTag("switch_dynamic_color")
                    )
                }
            }

            if (!customization.useDynamicColor) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccentPalettePreset.entries.forEach { preset ->
                        val selected = customization.palette == preset
                        val shape = RoundedCornerShape(10.dp)

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
                                    color = if (selected) preset.primaryDark else colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    shape = shape
                                )
                                .clickable {
                                    haptics.selectionTick()
                                    onUpdateCustomization { it.copy(palette = preset) }
                                }
                                .testTag("palette_${preset.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
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
private fun DisplayAndTypographyCard(
    customization: AppCustomizationState,
    activeRefreshRateHz: Int,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptics = rememberOptiShareHaptics()

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Display e carattere",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            SegmentedSelectorBar(
                items = AppFontChoice.entries,
                selectedItem = customization.fontChoice,
                onItemSelected = { choice ->
                    onUpdateCustomization { it.copy(fontChoice = choice) }
                },
                labelProvider = { it.label }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fluidità 120Hz",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Attivo a ${activeRefreshRateHz} Hz",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = customization.preferHighRefreshRate,
                    onCheckedChange = { enabled ->
                        haptics.toggle(enabled)
                        onUpdateCustomization { it.copy(preferHighRefreshRate = enabled) }
                    },
                    modifier = Modifier.testTag("switch_high_refresh_rate_120hz")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedAppearanceAccordion(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit,
    onApplyPreset: (String) -> Unit,
    onRequestReset: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptics = rememberOptiShareHaptics()
    var expanded by rememberSaveable { mutableStateOf(false) }

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.selectionTick()
                        expanded = !expanded
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Regolazioni avanzate",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Comprimi regolazioni" else "Espandi regolazioni",
                    tint = colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = "Profili rapidi",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "SWISS_INSTRUMENT" to "Material You",
                            "PAPER_EDITORIAL" to "Chiaro Salvia",
                            "SMOKED_ACRYLIC_GLASS" to "Glass Scuro",
                            "MONOCHROME_TERMINAL" to "Ardesia"
                        ).forEach { (key, label) ->
                            FilledTonalButton(
                                onClick = {
                                    haptics.selectionTick()
                                    onApplyPreset(key)
                                },
                                modifier = Modifier.testTag("preset_$key")
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (customization.designStyle == DesignStyle.LIQUID_GLASS) {
                        SliderSettingRow(
                            title = "Traslucenza barre",
                            valueText = "${(customization.glassOpacity * 100).roundToInt()}%",
                            value = customization.glassOpacity,
                            valueRange = 0.68f..0.95f,
                            onValueChange = { v -> onUpdateCustomization { it.copy(glassOpacity = v) } },
                            testTag = "slider_glass_opacity"
                        )
                        SliderSettingRow(
                            title = "Riflesso bordi",
                            valueText = "${(customization.glassSpecularIntensity * 100).roundToInt()}%",
                            value = customization.glassSpecularIntensity,
                            valueRange = 0.05f..1.0f,
                            onValueChange = { v -> onUpdateCustomization { it.copy(glassSpecularIntensity = v) } },
                            testTag = "slider_glass_specular"
                        )
                        SliderSettingRow(
                            title = "Raggio angoli",
                            valueText = "${customization.glassCornerRadiusDp} dp",
                            value = customization.glassCornerRadiusDp.toFloat(),
                            valueRange = 8f..24f,
                            onValueChange = { v -> onUpdateCustomization { it.copy(glassCornerRadiusDp = v.roundToInt()) } },
                            testTag = "slider_glass_radius"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bagliore radiale sottile", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = customization.glassAmbientOrbsEnabled,
                                onCheckedChange = { enabled ->
                                    haptics.toggle(enabled)
                                    onUpdateCustomization { it.copy(glassAmbientOrbsEnabled = enabled) }
                                },
                                modifier = Modifier.testTag("switch_glass_orbs")
                            )
                        }
                    } else {
                        SliderSettingRow(
                            title = "Raggio angoli",
                            valueText = "${customization.materialCornerRadiusDp} dp",
                            value = customization.materialCornerRadiusDp.toFloat(),
                            valueRange = 4f..24f,
                            onValueChange = { v -> onUpdateCustomization { it.copy(materialCornerRadiusDp = v.roundToInt()) } },
                            testTag = "slider_material_radius"
                        )
                    }

                    SliderSettingRow(
                        title = "Scala testo",
                        valueText = "${(customization.fontScale * 100).roundToInt()}%",
                        value = customization.fontScale,
                        valueRange = 0.85f..1.20f,
                        onValueChange = { v -> onUpdateCustomization { it.copy(fontScale = v) } },
                        testTag = "slider_font_scale"
                    )

                    TextButton(
                        onClick = onRequestReset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_theme_defaults_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ripristina predefiniti")
                    }
                }
            }
        }
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false
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
