package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppCustomizationState
import com.example.ui.theme.DesignStyle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppCustomization
import com.example.ui.theme.ThemeModeOption

/**
 * Runtime Liquid Glass visual & accessibility tokens resolved for the current device,
 * theme luminance, and system accessibility/power-save preferences.
 */
@Immutable
data class LiquidGlassTokens(
    val isDark: Boolean,
    val isLiquidGlassMode: Boolean,
    val reduceTransparency: Boolean,
    val reduceMotion: Boolean,
    val supportsRenderEffect: Boolean,
    val barSurfaceAlpha: Float,
    val cardSurfaceAlpha: Float,
    val specularRimAlpha: Float,
    val innerMeniscusAlpha: Float,
    val refractionTintAlpha: Float
)

@Composable
fun rememberLiquidGlassTokens(
    customization: AppCustomizationState = LocalAppCustomization.current
): LiquidGlassTokens {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme

    val isDark = when (customization.themeMode) {
        ThemeModeOption.DARK, ThemeModeOption.AMOLED -> true
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.SYSTEM -> systemDark || colorScheme.background.luminance() < 0.35f
    }
    val isAmoled = customization.themeMode == ThemeModeOption.AMOLED

    val (a11yReduceTransparency, a11yReduceMotion) = remember(context) {
        resolveSystemAccessibilityFlags(context)
    }

    val reduceTransparency = a11yReduceTransparency || isAmoled
    val isGlassMode = customization.designStyle == DesignStyle.LIQUID_GLASS
    val supportsRenderEffect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val barSurfaceAlpha = when {
        reduceTransparency -> 0.98f
        isGlassMode -> customization.glassOpacity.coerceIn(0.72f, 0.90f)
        isDark -> 0.84f
        else -> 0.88f
    }

    // Content cards prioritize legibility and WCAG contrast while keeping subtle depth
    val cardSurfaceAlpha = when {
        reduceTransparency -> 1.0f
        isGlassMode -> if (isDark) {
            customization.glassOpacity.coerceIn(0.78f, 0.94f)
        } else {
            customization.glassOpacity.coerceIn(0.85f, 0.96f)
        }
        else -> customization.materialSurfaceContrast.coerceIn(0.92f, 1.0f)
    }

    val specularRimAlpha = when {
        reduceTransparency -> 0.16f
        isGlassMode -> (customization.glassSpecularIntensity * (if (isDark) 0.55f else 0.45f)).coerceIn(0.12f, 0.60f)
        isDark -> 0.22f
        else -> 0.18f
    }

    val innerMeniscusAlpha = when {
        reduceTransparency || !customization.glassInnerHighlightEnabled -> 0f
        isGlassMode -> if (isDark) 0.24f else 0.42f
        isDark -> 0.12f
        else -> 0.22f
    }

    val refractionTintAlpha = when {
        reduceTransparency -> 0f
        isGlassMode -> (customization.glassRefractionGlow * 0.14f).coerceIn(0f, 0.16f)
        else -> 0.04f
    }

    return remember(
        isDark,
        isGlassMode,
        reduceTransparency,
        a11yReduceMotion,
        supportsRenderEffect,
        barSurfaceAlpha,
        cardSurfaceAlpha,
        specularRimAlpha,
        innerMeniscusAlpha,
        refractionTintAlpha
    ) {
        LiquidGlassTokens(
            isDark = isDark,
            isLiquidGlassMode = isGlassMode,
            reduceTransparency = reduceTransparency,
            reduceMotion = a11yReduceMotion,
            supportsRenderEffect = supportsRenderEffect,
            barSurfaceAlpha = barSurfaceAlpha,
            cardSurfaceAlpha = cardSurfaceAlpha,
            specularRimAlpha = specularRimAlpha,
            innerMeniscusAlpha = innerMeniscusAlpha,
            refractionTintAlpha = refractionTintAlpha
        )
    }
}

internal fun resolveSystemAccessibilityFlags(context: Context): Pair<Boolean, Boolean> {
    return runCatching {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val touchExploration = am?.isTouchExplorationEnabled == true
        val powerSave = pm?.isPowerSaveMode == true
        val animScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        val reduceMotion = animScale == 0f || powerSave
        val reduceTransparency = touchExploration || powerSave
        reduceTransparency to reduceMotion
    }.getOrDefault(false to false)
}

/**
 * Cached GPU-friendly modifier that renders a refined Liquid Glass specular rim,
 * subtle diagonal chromatic refraction sheen, and a 1dp top inner meniscus highlight
 * without per-frame object allocations.
 */
fun Modifier.liquidGlassRimAndHighlight(
    cornerRadius: Dp,
    accentColor: Color,
    isDark: Boolean,
    specularAlpha: Float,
    meniscusAlpha: Float,
    refractionAlpha: Float,
    borderWidth: Dp = 1.dp
): Modifier = this.drawWithCache {
    val radiusPx = cornerRadius.toPx()
    val strokePx = borderWidth.toPx()
    val meniscusPx = 1.dp.toPx()

    val rimHighlightColor = if (isDark) {
        Color.White.copy(alpha = specularAlpha)
    } else {
        Color.White.copy(alpha = (specularAlpha * 1.35f).coerceAtMost(0.75f))
    }
    val rimMidColor = accentColor.copy(alpha = (specularAlpha * 0.55f).coerceAtMost(0.35f))
    val rimShadowColor = if (isDark) {
        Color.White.copy(alpha = (specularAlpha * 0.18f).coerceAtLeast(0.05f))
    } else {
        accentColor.copy(alpha = (specularAlpha * 0.28f).coerceAtLeast(0.08f))
    }

    val rimBrush = Brush.linearGradient(
        colors = listOf(rimHighlightColor, rimMidColor, rimShadowColor),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )

    val sheenBrush = if (refractionAlpha > 0.005f) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (refractionAlpha * 0.85f).coerceAtMost(0.14f)),
                accentColor.copy(alpha = refractionAlpha),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.85f, size.height * 0.9f)
        )
    } else {
        null
    }

    val meniscusColor = Color.White.copy(alpha = meniscusAlpha)

    onDrawWithContent {
        if (sheenBrush != null) {
            drawRoundRect(
                brush = sheenBrush,
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        }
        drawContent()
        if (meniscusAlpha > 0.01f && size.width > radiusPx * 2f) {
            drawLine(
                color = meniscusColor,
                start = Offset(radiusPx * 0.85f, strokePx + meniscusPx * 0.5f),
                end = Offset(size.width - radiusPx * 0.85f, strokePx + meniscusPx * 0.5f),
                strokeWidth = meniscusPx
            )
        }
        if (strokePx > 0f && specularAlpha > 0.01f) {
            drawRoundRect(
                brush = rimBrush,
                topLeft = Offset(strokePx * 0.5f, strokePx * 0.5f),
                size = Size(
                    (size.width - strokePx).coerceAtLeast(0f),
                    (size.height - strokePx).coerceAtLeast(0f)
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx)
            )
        }
    }
}

/**
 * Adaptive app background container.
 * Maintains the clean Material 3 background tone while adding a subtle, calibrated
 * radial luminance atmosphere behind navigation and header zones when Liquid Glass
 * or ambient diffusion is active (automatically disabled in Reduced Transparency / AMOLED modes).
 */
@Composable
fun AdaptiveAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val custom = LocalAppCustomization.current
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens(custom)

    val showSubtleAtmosphere = !tokens.reduceTransparency &&
        (tokens.isLiquidGlassMode || custom.glassAmbientOrbsEnabled)
    val orbIntensity = if (custom.glassAmbientOrbsEnabled) {
        (custom.glassAmbientOrbIntensity * 0.14f).coerceIn(0.03f, 0.14f)
    } else {
        0.06f
    }

    val primaryGlow = colorScheme.primary.copy(alpha = orbIntensity)
    val secondaryGlow = colorScheme.secondary.copy(alpha = orbIntensity * 0.75f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .then(
                if (showSubtleAtmosphere) {
                    Modifier.drawBehind {
                        val topRadius = size.minDimension * 0.72f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryGlow, Color.Transparent),
                                center = Offset(size.width * 0.82f, size.height * 0.08f),
                                radius = topRadius
                            ),
                            radius = topRadius,
                            center = Offset(size.width * 0.82f, size.height * 0.08f)
                        )
                        val bottomRadius = size.minDimension * 0.68f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(secondaryGlow, Color.Transparent),
                                center = Offset(size.width * 0.18f, size.height * 0.92f),
                                radius = bottomRadius
                            ),
                            radius = bottomRadius,
                            center = Offset(size.width * 0.18f, size.height * 0.92f)
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * Reusable Liquid Glass Surface for floating navigation bars, top bars, and contextual chrome.
 * Combines high-contrast frosted translucency with a delicate specular rim and top meniscus.
 */
@Composable
fun LiquidGlassBarSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    accentTint: Color = MaterialTheme.colorScheme.primary,
    tonalElevation: Dp = 2.dp,
    shadowElevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens()
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val baseSurfaceColor = if (tokens.isDark) {
        colorScheme.surface.copy(alpha = tokens.barSurfaceAlpha)
    } else {
        colorScheme.surface.copy(alpha = tokens.barSurfaceAlpha)
    }

    Surface(
        shape = shape,
        color = baseSurfaceColor,
        contentColor = colorScheme.onSurface,
        tonalElevation = if (tokens.reduceTransparency) 3.dp else tonalElevation,
        shadowElevation = if (tokens.reduceTransparency) 2.dp else shadowElevation,
        modifier = modifier
            .clip(shape)
            .liquidGlassRimAndHighlight(
                cornerRadius = cornerRadius,
                accentColor = accentTint,
                isDark = tokens.isDark,
                specularAlpha = (tokens.specularRimAlpha * 1.15f).coerceAtMost(0.65f),
                meniscusAlpha = tokens.innerMeniscusAlpha,
                refractionAlpha = tokens.refractionTintAlpha
            )
    ) {
        Box(content = content)
    }
}

/**
 * Reusable Liquid Glass Floating Pill for camera HUD controls (Flash, Quick Zoom, Telemetry overlay)
 * and contextual toolbar chips. Supports normal, pressed, selected, and focused states.
 */
@Composable
fun LiquidGlassPill(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    isDarkHud: Boolean = false,
    cornerRadius: Dp = 10.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !tokens.reduceMotion) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "GlassPillScale"
    )

    val targetContainerColor = when {
        isDarkHud && selected -> accentColor.copy(alpha = if (tokens.reduceTransparency) 0.96f else 0.84f)
        isDarkHud -> Color(0xFF0B0E14).copy(alpha = if (tokens.reduceTransparency) 0.92f else 0.72f)
        selected -> colorScheme.secondaryContainer.copy(alpha = if (tokens.reduceTransparency) 1.0f else 0.88f)
        else -> colorScheme.surfaceVariant.copy(alpha = if (tokens.reduceTransparency) 0.95f else 0.65f)
    }

    val targetContentColor = when {
        isDarkHud -> Color.White
        selected -> colorScheme.onSecondaryContainer
        else -> colorScheme.onSurfaceVariant
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "GlassPillContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "GlassPillContent"
    )

    val effectiveSpecular = when {
        isFocused -> (tokens.specularRimAlpha * 1.6f).coerceAtMost(0.85f)
        selected -> (tokens.specularRimAlpha * 1.35f).coerceAtMost(0.70f)
        isDarkHud -> 0.34f
        else -> tokens.specularRimAlpha
    }

    val pillModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .liquidGlassRimAndHighlight(
            cornerRadius = cornerRadius,
            accentColor = if (selected || isFocused) accentColor else colorScheme.outlineVariant,
            isDark = isDarkHud || tokens.isDark,
            specularAlpha = effectiveSpecular,
            meniscusAlpha = if (selected || isDarkHud) tokens.innerMeniscusAlpha else tokens.innerMeniscusAlpha * 0.5f,
            refractionAlpha = if (selected) tokens.refractionTintAlpha else 0f
        )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
            modifier = pillModifier
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                content = content
            )
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            modifier = pillModifier
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                content = content
            )
        }
    }
}

/**
 * Primary Content Card surface (`OptiShareCard`).
 * Prioritizes high contrast and legibility for main content, telemetry, and QR stages,
 * while applying delicate Liquid Glass rim/meniscus refinement when appropriate.
 */
@Composable
fun OptiShareCard(
    modifier: Modifier = Modifier,
    accentTint: Color? = null,
    isHighlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val custom = LocalAppCustomization.current
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens(custom)

    val radiusDp = if (tokens.isLiquidGlassMode) {
        custom.glassCornerRadiusDp.dp
    } else {
        custom.materialCornerRadiusDp.dp
    }
    val shape = remember(radiusDp) { RoundedCornerShape(radiusDp) }

    val containerColor = when {
        tokens.isLiquidGlassMode -> {
            colorScheme.surface.copy(alpha = tokens.cardSurfaceAlpha)
        }
        isHighlighted -> {
            colorScheme.surface
        }
        else -> {
            colorScheme.surface.copy(alpha = tokens.cardSurfaceAlpha)
        }
    }

    val resolvedAccent = accentTint ?: colorScheme.primary

    val borderColor = when {
        isHighlighted && accentTint != null -> accentTint.copy(alpha = 0.55f)
        isHighlighted -> colorScheme.primary.copy(alpha = 0.45f)
        else -> colorScheme.outlineVariant.copy(alpha = 0.62f)
    }

    val useGlassRim = tokens.isLiquidGlassMode || isHighlighted
    val borderStroke = if (!useGlassRim && custom.materialBorderWidthDp > 0.05f) {
        BorderStroke(
            width = custom.materialBorderWidthDp.dp,
            color = borderColor
        )
    } else {
        null
    }

    val tonalElevation = when {
        tokens.isLiquidGlassMode -> 1.dp
        !custom.materialUseTonalElevation -> 0.dp
        isHighlighted -> 3.dp
        else -> 1.dp
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = tonalElevation
        ),
        border = borderStroke,
        modifier = modifier.then(
            if (useGlassRim) {
                Modifier.liquidGlassRimAndHighlight(
                    cornerRadius = radiusDp,
                    accentColor = if (isHighlighted) resolvedAccent else colorScheme.outlineVariant,
                    isDark = tokens.isDark,
                    specularAlpha = if (tokens.isLiquidGlassMode) {
                        tokens.specularRimAlpha
                    } else {
                        (tokens.specularRimAlpha * 0.75f).coerceAtLeast(0.14f)
                    },
                    meniscusAlpha = if (tokens.isLiquidGlassMode || isHighlighted) {
                        tokens.innerMeniscusAlpha
                    } else {
                        0f
                    },
                    refractionAlpha = if (tokens.isLiquidGlassMode && isHighlighted) {
                        tokens.refractionTintAlpha
                    } else {
                        0f
                    },
                    borderWidth = if (tokens.isLiquidGlassMode) 1.dp else custom.materialBorderWidthDp.coerceAtLeast(0.8f).dp
                )
            } else {
                Modifier
            }
        ),
        content = content
    )
}

/**
 * Clean Material 3 section header with a readable subtitle/overline and primary title.
 */
@Composable
fun SectionHeaderRow(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = if (trailingContent != null) 8.dp else 0.dp)
        ) {
            Text(
                text = overline,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingContent != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = trailingContent
            )
        }
    }
}

/**
 * Segmented Selector Bar refined with a recessed translucent glass track and
 * a floating specular-rimmed active capsule indicator.
 */
@Composable
fun <T> SegmentedSelectorBar(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens()
    val outerRadius = 13.dp
    val itemRadius = 10.dp
    val outerShape = remember { RoundedCornerShape(outerRadius) }
    val itemShape = remember { RoundedCornerShape(itemRadius) }

    val trackAlpha = when {
        tokens.reduceTransparency -> 0.85f
        tokens.isLiquidGlassMode -> 0.45f
        else -> 0.55f
    }

    Surface(
        shape = outerShape,
        color = colorScheme.surfaceVariant.copy(alpha = trackAlpha),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = if (tokens.isLiquidGlassMode) 0.45f else 0.6f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val itemScale by animateFloatAsState(
                    targetValue = if (isPressed && !tokens.reduceMotion) 0.97f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "SegmentedScale120Hz"
                )

                val bgColor by animateColorAsState(
                    targetValue = when {
                        !isSelected -> Color.Transparent
                        tokens.reduceTransparency -> colorScheme.secondaryContainer
                        tokens.isLiquidGlassMode -> colorScheme.secondaryContainer.copy(alpha = 0.84f)
                        else -> colorScheme.secondaryContainer.copy(alpha = 0.92f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "SegmentedBg120Hz"
                )
                val fgColor by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "SegmentedFg120Hz"
                )
                Surface(
                    onClick = { onItemSelected(item) },
                    shape = itemShape,
                    color = bgColor,
                    contentColor = fgColor,
                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                    shadowElevation = if (isSelected && !tokens.reduceTransparency) 1.dp else 0.dp,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        }
                        .then(
                            if (isSelected) {
                                Modifier.liquidGlassRimAndHighlight(
                                    cornerRadius = itemRadius,
                                    accentColor = colorScheme.primary,
                                    isDark = tokens.isDark,
                                    specularAlpha = (tokens.specularRimAlpha * 1.15f).coerceAtMost(0.55f),
                                    meniscusAlpha = tokens.innerMeniscusAlpha,
                                    refractionAlpha = tokens.refractionTintAlpha * 0.6f
                                )
                            } else {
                                Modifier
                            }
                        )
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelProvider(item),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tonal Material 3 Metric Cell for displaying structured transfer statistics.
 * Maintains strong contrast for monospace telemetry values.
 */
@Composable
fun TelemetryMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens()
    val shape = remember { RoundedCornerShape(10.dp) }

    Surface(
        shape = shape,
        color = colorScheme.surfaceVariant.copy(
            alpha = if (tokens.reduceTransparency) 0.75f else 0.44f
        ),
        border = if (tokens.isLiquidGlassMode) {
            BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.38f))
        } else {
            null
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Accessible Material 3 Action Button (48dp minimum touch height) with subtle press physics,
 * delicate top meniscus highlight on primary actions, and guaranteed single-line labels.
 */
@Composable
fun InstrumentActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonRadius = 12.dp
    val buttonShape: Shape = remember { RoundedCornerShape(buttonRadius) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !tokens.reduceMotion) 0.975f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ActionBtnScale120Hz"
    )

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = buttonShape,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            shape = buttonShape,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (tokens.isLiquidGlassMode && !tokens.reduceTransparency) {
                    colorScheme.secondaryContainer.copy(alpha = 0.82f)
                } else {
                    colorScheme.secondaryContainer
                },
                contentColor = colorScheme.onSecondaryContainer
            ),
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (tokens.isLiquidGlassMode && enabled) {
                        Modifier.liquidGlassRimAndHighlight(
                            cornerRadius = buttonRadius,
                            accentColor = colorScheme.primary,
                            isDark = tokens.isDark,
                            specularAlpha = tokens.specularRimAlpha * 0.85f,
                            meniscusAlpha = tokens.innerMeniscusAlpha * 0.7f,
                            refractionAlpha = 0f
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
