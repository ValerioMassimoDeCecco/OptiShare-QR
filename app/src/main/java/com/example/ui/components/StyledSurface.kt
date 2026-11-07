package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
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
import androidx.compose.ui.platform.LocalView
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
 * Platform-native Android Haptic Feedback controller that respects system
 * touch vibration settings (`View.isHapticFeedbackEnabled`) and uses semantic
 * `HapticFeedbackConstants` across API 24..36 without external dependencies.
 */
@Immutable
class OptiShareHaptics(private val view: View?) {

    private fun canVibrate(): Boolean {
        val v = view ?: return false
        return v.isHapticFeedbackEnabled
    }

    /** Subtle tick for tab switches, segmented selectors, zoom pills, and frame stepping. */
    fun selectionTick() {
        val v = view ?: return
        if (!canVibrate()) return
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_TICK
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            v.performHapticFeedback(constant)
        }
    }

    /** Crisp feedback for activating or deactivating an important toggle or transport control. */
    fun toggle(enabled: Boolean = true) {
        val v = view ?: return
        if (!canVibrate()) return
        runCatching {
            val constant = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    if (enabled) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    HapticFeedbackConstants.CONFIRM
                }
                else -> {
                    HapticFeedbackConstants.VIRTUAL_KEY
                }
            }
            v.performHapticFeedback(constant)
        }
    }

    /** Confirmation pulse when an important operation completes (e.g., SHA-256 verified transfer). */
    fun confirmSuccess() {
        val v = view ?: return
        if (!canVibrate()) return
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
            v.performHapticFeedback(constant)
        }
    }

    /** Distinct alert feedback for significant errors (e.g., wrong AES-256 password or invalid NACK QR). */
    fun notifyError() {
        val v = view ?: return
        if (!canVibrate()) return
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            v.performHapticFeedback(constant)
        }
    }
}

@Composable
fun rememberOptiShareHaptics(): OptiShareHaptics {
    val view = LocalView.current
    return remember(view) { OptiShareHaptics(view) }
}

/**
 * Runtime Liquid Glass & Material You tokens resolved for the current device,
 * theme luminance, and system accessibility/power-save preferences.
 *
 * Design Rule:
 * - Floating chrome (TopBar, Bottom Navigation Bar, Segmented Bar, Camera HUD pills)
 *   uses controlled translucency (`barSurfaceAlpha`) and delicate specular rims.
 * - Primary content cards (`OptiShareCard`) remain solid/high-contrast (`cardSurfaceAlpha`)
 *   so text, SHA-256 digests, and QR codes are always read without distraction or stacked blur.
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
        isGlassMode -> customization.glassOpacity.coerceIn(0.76f, 0.92f)
        isDark -> 0.88f
        else -> 0.92f
    }

    // Content cards stay opaque or near-opaque to guarantee distraction-free reading
    val cardSurfaceAlpha = when {
        reduceTransparency -> 1.0f
        isGlassMode -> if (isDark) {
            customization.glassOpacity.coerceIn(0.90f, 0.98f)
        } else {
            customization.glassOpacity.coerceIn(0.94f, 0.99f)
        }
        else -> customization.materialSurfaceContrast.coerceIn(0.96f, 1.0f)
    }

    val specularRimAlpha = when {
        reduceTransparency -> 0.14f
        isGlassMode -> (customization.glassSpecularIntensity * (if (isDark) 0.50f else 0.40f)).coerceIn(0.14f, 0.52f)
        isDark -> 0.20f
        else -> 0.16f
    }

    val innerMeniscusAlpha = when {
        reduceTransparency || !customization.glassInnerHighlightEnabled -> 0f
        isGlassMode -> if (isDark) 0.22f else 0.36f
        isDark -> 0.10f
        else -> 0.18f
    }

    val refractionTintAlpha = when {
        reduceTransparency -> 0f
        isGlassMode -> (customization.glassRefractionGlow * 0.12f).coerceIn(0f, 0.12f)
        else -> 0.03f
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
 * Cached GPU-friendly modifier for floating bars, pills, and active indicators.
 * Renders a directional 1dp specular rim and top inner meniscus without per-frame allocations.
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
        Color.White.copy(alpha = (specularAlpha * 1.3f).coerceAtMost(0.70f))
    }
    val rimMidColor = accentColor.copy(alpha = (specularAlpha * 0.50f).coerceAtMost(0.32f))
    val rimShadowColor = if (isDark) {
        Color.White.copy(alpha = (specularAlpha * 0.16f).coerceAtLeast(0.05f))
    } else {
        accentColor.copy(alpha = (specularAlpha * 0.24f).coerceAtLeast(0.07f))
    }

    val rimBrush = Brush.linearGradient(
        colors = listOf(rimHighlightColor, rimMidColor, rimShadowColor),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )

    val sheenBrush = if (refractionAlpha > 0.005f) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (refractionAlpha * 0.8f).coerceAtMost(0.10f)),
                accentColor.copy(alpha = refractionAlpha),
                Color.Transparent
            ),
            start = Offset.Zero,
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
 * Adaptive Material You app background.
 * Keeps main screen background clean and high-contrast, adding only a subtle atmospheric
 * luminance behind the top/bottom bar zones when explicitly enabled in settings.
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
        tokens.isLiquidGlassMode &&
        custom.glassAmbientOrbsEnabled
    val orbIntensity = (custom.glassAmbientOrbIntensity * 0.10f).coerceIn(0.02f, 0.10f)
    val primaryGlow = colorScheme.primary.copy(alpha = orbIntensity)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .then(
                if (showSubtleAtmosphere) {
                    Modifier.drawBehind {
                        val radius = size.minDimension * 0.65f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryGlow, Color.Transparent),
                                center = Offset(size.width * 0.8f, size.height * 0.08f),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(size.width * 0.8f, size.height * 0.08f)
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
 * Reusable Liquid Glass Surface reserved for floating navigation bars and contextual chrome.
 */
@Composable
fun LiquidGlassBarSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    accentTint: Color = MaterialTheme.colorScheme.primary,
    tonalElevation: Dp = 3.dp,
    shadowElevation: Dp = 5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens()
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    Surface(
        shape = shape,
        color = colorScheme.surface.copy(alpha = tokens.barSurfaceAlpha),
        contentColor = colorScheme.onSurface,
        tonalElevation = if (tokens.reduceTransparency) 4.dp else tonalElevation,
        shadowElevation = if (tokens.reduceTransparency) 2.dp else shadowElevation,
        modifier = modifier
            .clip(shape)
            .liquidGlassRimAndHighlight(
                cornerRadius = cornerRadius,
                accentColor = accentTint,
                isDark = tokens.isDark,
                specularAlpha = tokens.specularRimAlpha,
                meniscusAlpha = tokens.innerMeniscusAlpha,
                refractionAlpha = tokens.refractionTintAlpha
            )
    ) {
        Box(content = content)
    }
}

/**
 * Reusable Liquid Glass Floating Pill for camera HUD overlays and toolbar status pills.
 * Supports normal, pressed, selected, disabled, and focused states with optional haptic feedback.
 */
@Composable
fun LiquidGlassPill(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    isDarkHud: Boolean = false,
    hapticOnClick: Boolean = true,
    cornerRadius: Dp = 10.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens()
    val haptics = rememberOptiShareHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !tokens.reduceMotion) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "GlassPillScale"
    )

    val targetContainerColor = when {
        !enabled -> colorScheme.surfaceVariant.copy(alpha = 0.38f)
        isDarkHud && selected -> accentColor.copy(alpha = if (tokens.reduceTransparency) 0.96f else 0.86f)
        isDarkHud -> Color(0xFF0B0E14).copy(alpha = if (tokens.reduceTransparency) 0.92f else 0.74f)
        selected -> colorScheme.secondaryContainer.copy(alpha = if (tokens.reduceTransparency) 1.0f else 0.90f)
        else -> colorScheme.surfaceVariant.copy(alpha = if (tokens.reduceTransparency) 0.95f else 0.70f)
    }

    val targetContentColor = when {
        !enabled -> colorScheme.onSurface.copy(alpha = 0.38f)
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
        !enabled -> 0.08f
        isFocused -> (tokens.specularRimAlpha * 1.6f).coerceAtMost(0.85f)
        selected -> (tokens.specularRimAlpha * 1.3f).coerceAtMost(0.65f)
        isDarkHud -> 0.32f
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
            meniscusAlpha = if (selected || isDarkHud) tokens.innerMeniscusAlpha else 0f,
            refractionAlpha = if (selected) tokens.refractionTintAlpha else 0f
        )

    if (onClick != null) {
        Surface(
            onClick = {
                if (hapticOnClick) haptics.selectionTick()
                onClick()
            },
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
 * Uses solid/high-contrast Material You tonal surfaces (`colorScheme.surface` + crisp border)
 * so primary content, QR codes, SHA-256 digests, and long texts remain 100% legible without
 * stacked transparency or distracting glass reflections.
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

    val containerColor = if (isHighlighted) {
        colorScheme.surface
    } else {
        colorScheme.surface.copy(alpha = tokens.cardSurfaceAlpha)
    }

    val borderColor = when {
        isHighlighted && accentTint != null -> accentTint.copy(alpha = 0.55f)
        isHighlighted -> colorScheme.primary.copy(alpha = 0.45f)
        else -> colorScheme.outlineVariant.copy(alpha = 0.65f)
    }

    val borderWidth = if (tokens.isLiquidGlassMode) {
        1.dp
    } else {
        custom.materialBorderWidthDp.dp
    }

    val borderStroke = if (borderWidth > 0.05.dp) {
        BorderStroke(width = borderWidth, color = borderColor)
    } else {
        null
    }

    val tonalElevation = when {
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
        modifier = modifier,
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
 * Material You + Liquid Glass Segmented Selector Bar:
 * Recessed translucent track with a specular-rimmed active indicator capsule,
 * full state support (normal, pressed, selected, focus), and native selection haptics.
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
    val haptics = rememberOptiShareHaptics()
    val outerRadius = 13.dp
    val itemRadius = 10.dp
    val outerShape = remember { RoundedCornerShape(outerRadius) }
    val itemShape = remember { RoundedCornerShape(itemRadius) }

    val trackAlpha = when {
        tokens.reduceTransparency -> 0.85f
        tokens.isLiquidGlassMode -> 0.48f
        else -> 0.56f
    }

    Surface(
        shape = outerShape,
        color = colorScheme.surfaceVariant.copy(alpha = trackAlpha),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.55f)
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
                val isFocused by interactionSource.collectIsFocusedAsState()

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
                        else -> colorScheme.secondaryContainer.copy(alpha = 0.90f)
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
                    onClick = {
                        if (!isSelected) {
                            haptics.selectionTick()
                            onItemSelected(item)
                        }
                    },
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
                            if (isSelected || isFocused) {
                                Modifier.liquidGlassRimAndHighlight(
                                    cornerRadius = itemRadius,
                                    accentColor = colorScheme.primary,
                                    isDark = tokens.isDark,
                                    specularAlpha = if (isFocused) {
                                        (tokens.specularRimAlpha * 1.4f).coerceAtMost(0.65f)
                                    } else {
                                        tokens.specularRimAlpha
                                    },
                                    meniscusAlpha = tokens.innerMeniscusAlpha,
                                    refractionAlpha = tokens.refractionTintAlpha * 0.5f
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
 */
@Composable
fun TelemetryMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = remember { RoundedCornerShape(10.dp) }

    Surface(
        shape = shape,
        color = colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
 * Accessible Material 3 Action Button (48dp minimum touch height) with subtle press physics
 * and optional haptic feedback for primary state transitions.
 */
@Composable
fun InstrumentActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    hapticOnClick: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val tokens = rememberLiquidGlassTokens()
    val haptics = rememberOptiShareHaptics()
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
            onClick = {
                if (hapticOnClick) haptics.toggle(true)
                onClick()
            },
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
            onClick = {
                if (hapticOnClick) haptics.selectionTick()
                onClick()
            },
            enabled = enabled,
            shape = buttonShape,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer
            ),
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
    }
}
