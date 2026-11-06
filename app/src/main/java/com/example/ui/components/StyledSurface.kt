package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout. defaultMinSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DesignStyle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppCustomization

/**
 * Clean Material You background container.
 * Uses pure coordinated Material 3 background surface without decorative gradients or heavy visual noise.
 */
@Composable
fun AdaptiveAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        content()
    }
}

/**
 * Material You (Material Design 3) Card surface.
 * Uses balanced rounded corners, subtle tonal elevation, and a clean outlineVariant border
 * to clearly separate elevation layers without heavy shadows or glassmorphism.
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

    val radiusDp = if (custom.designStyle == DesignStyle.LIQUID_GLASS) {
        custom.glassCornerRadiusDp.dp
    } else {
        custom.materialCornerRadiusDp.dp
    }
    val shape = RoundedCornerShape(radiusDp)

    val containerColor = when {
        custom.designStyle == DesignStyle.LIQUID_GLASS -> {
            colorScheme.surface.copy(alpha = custom.glassOpacity.coerceIn(0.65f, 0.96f))
        }
        isHighlighted -> {
            colorScheme.surface
        }
        else -> {
            colorScheme.surface.copy(alpha = custom.materialSurfaceContrast.coerceIn(0.75f, 1.0f))
        }
    }

    val borderColor = when {
        isHighlighted && accentTint != null -> accentTint.copy(alpha = 0.55f)
        isHighlighted -> colorScheme.primary.copy(alpha = 0.45f)
        else -> colorScheme.outlineVariant.copy(alpha = 0.65f)
    }

    val borderStroke = if (custom.materialBorderWidthDp > 0.05f || custom.designStyle == DesignStyle.LIQUID_GLASS) {
        BorderStroke(
            width = if (custom.designStyle == DesignStyle.LIQUID_GLASS) 1.dp else custom.materialBorderWidthDp.dp,
            color = borderColor
        )
    } else {
        null
    }

    val tonalElevation = when {
        custom.designStyle == DesignStyle.LIQUID_GLASS -> 1.dp
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = overline,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface
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
 * Material 3 Segmented Selector Bar with moderate rounded corners (12dp),
 * accessible touch height (40dp), and tonal secondaryContainer selection highlight.
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
    val outerShape = RoundedCornerShape(12.dp)
    val itemShape = RoundedCornerShape(9.dp)

    Surface(
        shape = outerShape,
        color = colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f)),
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
                Surface(
                    onClick = { onItemSelected(item) },
                    shape = itemShape,
                    color = if (isSelected) colorScheme.secondaryContainer else Color.Transparent,
                    contentColor = if (isSelected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelProvider(item),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
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
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.42f),
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
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Accessible Material 3 Action Button (48dp minimum touch height) with moderate 12dp corners
 * (avoiding excessive pill shapes) and clear primary / tonal states.
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
    val buttonShape = RoundedCornerShape(12.dp)

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = buttonShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = modifier.defaultMinSize(minHeight = 48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            shape = buttonShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = modifier.defaultMinSize(minHeight = 48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
