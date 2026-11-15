package com.example.ui.screens

import android.os.Build
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.components.SegmentedSelectorBar
import com.example.ui.components.rememberOptiShareHaptics
import com.example.ui.theme.AccentPalettePreset
import com.example.ui.theme.AppCustomizationState
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.DesignStyle
import com.example.ui.theme.ThemeModeOption

@Composable
fun SettingsScreen(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit,
    onApplyPreset: (String) -> Unit = {},
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
                        TypographySimpleCard(
                            customization = customization,
                            onUpdateCustomization = onUpdateCustomization,
                            onRequestReset = { showResetConfirmation = true }
                        )
                    }
                }
            } else {
                DesignAndThemeCard(
                    customization = customization,
                    onUpdateCustomization = onUpdateCustomization
                )
                TypographySimpleCard(
                    customization = customization,
                    onUpdateCustomization = onUpdateCustomization,
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
                Text("Vuoi tornare ai colori, al tema e al testo predefiniti?")
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
                                    text = if (style == DesignStyle.LIQUID_GLASS) "Vetro" else "Classico",
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
                items = listOf(
                    ThemeModeOption.SYSTEM,
                    ThemeModeOption.LIGHT,
                    ThemeModeOption.DARK,
                    ThemeModeOption.AMOLED
                ),
                selectedItem = customization.themeMode,
                onItemSelected = { mode ->
                    onUpdateCustomization { it.copy(themeMode = mode) }
                },
                labelProvider = {
                    when (it) {
                        ThemeModeOption.SYSTEM -> "Sistema"
                        ThemeModeOption.LIGHT -> "Chiaro"
                        ThemeModeOption.DARK -> "Scuro"
                        ThemeModeOption.AMOLED -> "Nero"
                    }
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Usa i colori del telefono",
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

            if (!customization.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccentPalettePreset.entries.forEach { preset ->
                        val selected = customization.palette == preset
                        val shape = RoundedCornerShape(10.dp)
                        val friendlyColorName = when (preset) {
                            AccentPalettePreset.SIGNAL_COBALT -> "Blu"
                            AccentPalettePreset.INSTRUMENT_ORANGE -> "Arancione"
                            AccentPalettePreset.OPTICAL_CYAN -> "Azzurro"
                            AccentPalettePreset.EMERALD_LAB -> "Verde"
                            AccentPalettePreset.PHOSPHOR_AMBER -> "Ambra"
                            AccentPalettePreset.MONOCHROME_ZINC -> "Grigio"
                        }

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
                                    text = friendlyColorName,
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
private fun TypographySimpleCard(
    customization: AppCustomizationState,
    onUpdateCustomization: ((AppCustomizationState) -> AppCustomizationState) -> Unit,
    onRequestReset: () -> Unit
) {
    val selectedSizeTier = when {
        customization.fontScale < 0.95f -> 0
        customization.fontScale > 1.05f -> 2
        else -> 1
    }

    OptiShareCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Testo e lettura",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Stile scrittura",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SegmentedSelectorBar(
                items = AppFontChoice.entries,
                selectedItem = customization.fontChoice,
                onItemSelected = { choice ->
                    onUpdateCustomization { it.copy(fontChoice = choice) }
                },
                labelProvider = {
                    when (it) {
                        AppFontChoice.PLUS_JAKARTA -> "Classico"
                        AppFontChoice.SPACE_GROTESK -> "Moderno"
                        AppFontChoice.JETBRAINS_MONO -> "Squadrato"
                    }
                }
            )

            Text(
                text = "Dimensione testo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SegmentedSelectorBar(
                items = listOf(0, 1, 2),
                selectedItem = selectedSizeTier,
                onItemSelected = { tier ->
                    val nextScale = when (tier) {
                        0 -> 0.90f
                        2 -> 1.12f
                        else -> 1.0f
                    }
                    onUpdateCustomization { it.copy(fontScale = nextScale) }
                },
                labelProvider = { tier ->
                    when (tier) {
                        0 -> "Piccolo"
                        2 -> "Grande"
                        else -> "Normale"
                    }
                }
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
