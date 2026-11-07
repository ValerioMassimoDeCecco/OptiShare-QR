package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.TransferRepository
import com.example.ui.components.AdaptiveAppBackground
import com.example.ui.components.LiquidGlassBarSurface
import com.example.ui.components.liquidGlassRimAndHighlight
import com.example.ui.components.rememberLiquidGlassTokens
import com.example.ui.screens.ReceiveScanScreen
import com.example.ui.screens.SendQrScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.DesignStyle
import com.example.ui.theme.OptiShareTheme
import com.example.ui.theme.ThemeSettingsManager
import com.example.viewmodel.TransferViewModel
import kotlin.math.roundToInt

enum class OptiShareTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SEND(
        titleRes = R.string.nav_send,
        selectedIcon = Icons.Filled.QrCode2,
        unselectedIcon = Icons.Outlined.QrCode2,
        testTag = "nav_tab_send"
    ),
    RECEIVE(
        titleRes = R.string.nav_receive,
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
        testTag = "nav_tab_receive"
    ),
    VAULT(
        titleRes = R.string.nav_vault,
        selectedIcon = Icons.Filled.FolderSpecial,
        unselectedIcon = Icons.Outlined.FolderSpecial,
        testTag = "nav_tab_vault"
    ),
    SETTINGS(
        titleRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Palette,
        unselectedIcon = Icons.Outlined.Palette,
        testTag = "nav_tab_settings"
    )
}

/**
 * Negotiates the highest supported hardware refresh rate (e.g. 120Hz ProMotion / LTPO)
 * for the current display resolution when [preferHighRefreshRate] is enabled,
 * and returns the target refresh rate in Hz (defaulting to 120Hz when preferred on virtual displays).
 */
fun Activity.configureHighRefreshRateWindow(preferHighRefreshRate: Boolean): Int {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                windowManager.defaultDisplay
            } ?: return 120

            val activeMode = currentDisplay.mode
            val sameResolutionModes = currentDisplay.supportedModes.filter {
                it.physicalWidth == activeMode.physicalWidth &&
                    it.physicalHeight == activeMode.physicalHeight
            }.ifEmpty { currentDisplay.supportedModes.toList() }

            val targetMode = if (preferHighRefreshRate) {
                sameResolutionModes.maxByOrNull { it.refreshRate } ?: activeMode
            } else {
                sameResolutionModes.minByOrNull { it.refreshRate } ?: activeMode
            }

            val targetHz = if (preferHighRefreshRate) {
                maxOf(targetMode.refreshRate.roundToInt(), 120)
            } else {
                targetMode.refreshRate.roundToInt().coerceAtLeast(60)
            }

            window.attributes = window.attributes.apply {
                preferredDisplayModeId = targetMode.modeId
                preferredRefreshRate = if (preferHighRefreshRate) {
                    maxOf(targetMode.refreshRate, 120f)
                } else {
                    60f
                }
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            targetHz
        } else {
            if (preferHighRefreshRate) 120 else 60
        }
    }.getOrDefault(if (preferHighRefreshRate) 120 else 60)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        configureHighRefreshRateWindow(preferHighRefreshRate = true)

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TransferRepository(applicationContext, database.transferDao())
        val themeManager = ThemeSettingsManager(applicationContext)

        setContent {
            val customization by themeManager.state.collectAsStateWithLifecycle()
            var targetRefreshRateHz by remember { mutableIntStateOf(120) }

            LaunchedEffect(customization.preferHighRefreshRate) {
                targetRefreshRateHz = configureHighRefreshRateWindow(
                    preferHighRefreshRate = customization.preferHighRefreshRate
                )
            }

            OptiShareTheme(customization = customization) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OptiShareApp(
                        repository = repository,
                        themeManager = themeManager,
                        activeRefreshRateHz = targetRefreshRateHz
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptiShareApp(
    repository: TransferRepository,
    themeManager: ThemeSettingsManager,
    activeRefreshRateHz: Int = 120
) {
    val context = LocalContext.current
    val transferViewModel: TransferViewModel = viewModel(
        factory = TransferViewModel.Factory(repository)
    )

    val senderState by transferViewModel.senderState.collectAsStateWithLifecycle()
    val receiverState by transferViewModel.receiverState.collectAsStateWithLifecycle()
    val vaultRecords by transferViewModel.vaultRecords.collectAsStateWithLifecycle()
    val customization by themeManager.state.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(OptiShareTab.SEND) }

    if (currentTab != OptiShareTab.SEND) {
        BackHandler {
            currentTab = OptiShareTab.SEND
        }
    }

    val isLiquidGlass = customization.designStyle == DesignStyle.LIQUID_GLASS
    val colorScheme = MaterialTheme.colorScheme
    val glassTokens = rememberLiquidGlassTokens(customization)

    AdaptiveAppBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(11.dp),
                                        color = colorScheme.primaryContainer.copy(
                                            alpha = if (glassTokens.reduceTransparency) 1f else 0.88f
                                        ),
                                        contentColor = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.liquidGlassRimAndHighlight(
                                            cornerRadius = 11.dp,
                                            accentColor = colorScheme.primary,
                                            isDark = glassTokens.isDark,
                                            specularAlpha = glassTokens.specularRimAlpha,
                                            meniscusAlpha = glassTokens.innerMeniscusAlpha,
                                            refractionAlpha = 0f
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.QrCode2,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(7.dp)
                                                .size(20.dp)
                                        )
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                        modifier = Modifier.testTag("refresh_rate_badge")
                                    ) {
                                        Text(
                                            text = "OptiShare QR",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Offline · v1.0.3 · ${activeRefreshRateHz}Hz",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            },
                            actions = {
                                FilledTonalButton(
                                    onClick = {
                                        themeManager.update { current ->
                                            val nextStyle = if (current.designStyle == DesignStyle.MATERIAL) {
                                                DesignStyle.LIQUID_GLASS
                                            } else {
                                                DesignStyle.MATERIAL
                                            }
                                            current.copy(designStyle = nextStyle)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = colorScheme.secondaryContainer.copy(
                                            alpha = if (glassTokens.reduceTransparency) 1f else 0.82f
                                        ),
                                        contentColor = colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .defaultMinSize(minHeight = 40.dp)
                                        .liquidGlassRimAndHighlight(
                                            cornerRadius = 12.dp,
                                            accentColor = colorScheme.primary,
                                            isDark = glassTokens.isDark,
                                            specularAlpha = (glassTokens.specularRimAlpha * 1.15f).coerceAtMost(0.6f),
                                            meniscusAlpha = glassTokens.innerMeniscusAlpha,
                                            refractionAlpha = glassTokens.refractionTintAlpha * 0.5f
                                        )
                                        .testTag("quick_style_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isLiquidGlass) Icons.Default.BlurOn else Icons.Default.Layers,
                                        contentDescription = "Cambia stile Material o Liquid Glass",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isLiquidGlass) "Liquid Glass" else "Material 3",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.surface.copy(alpha = glassTokens.barSurfaceAlpha),
                                titleContentColor = colorScheme.onBackground
                            )
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = if (isLiquidGlass) {
                                colorScheme.primary.copy(alpha = 0.22f)
                            } else {
                                colorScheme.outlineVariant.copy(alpha = 0.55f)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (isExpandedScreen) {
                            Row(modifier = Modifier.fillMaxHeight()) {
                                NavigationRail(
                                    containerColor = colorScheme.surface.copy(alpha = glassTokens.barSurfaceAlpha),
                                    contentColor = colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Spacer(modifier = Modifier.weight(0.15f))
                                    OptiShareTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = stringResource(tab.titleRes)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(tab.titleRes),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = colorScheme.onSecondaryContainer,
                                                selectedTextColor = colorScheme.onSurface,
                                                indicatorColor = colorScheme.secondaryContainer.copy(
                                                    alpha = if (glassTokens.reduceTransparency) 1f else 0.86f
                                                ),
                                                unselectedIconColor = colorScheme.onSurfaceVariant,
                                                unselectedTextColor = colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .testTag(tab.testTag)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(0.85f))
                                }
                                VerticalDivider(
                                    thickness = 1.dp,
                                    color = if (isLiquidGlass) {
                                        colorScheme.primary.copy(alpha = 0.22f)
                                    } else {
                                        colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    }
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            // 120Hz-tuned low-latency Spring transition between main tabs
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    fadeIn(
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) togetherWith fadeOut(
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                },
                                label = "TabTransition120Hz"
                            ) { targetTab ->
                                when (targetTab) {
                                    OptiShareTab.SEND -> {
                                        SendQrScreen(
                                            senderState = senderState,
                                            onPickFileUri = { uri ->
                                                transferViewModel.prepareStreamFromUri(context, uri)
                                            },
                                            onPickMultipleFileUris = { uris ->
                                                transferViewModel.prepareStreamFromMultipleUris(context, uris)
                                            },
                                            onCapturePhotoBitmap = { bitmap ->
                                                transferViewModel.prepareStreamFromCapturedPhoto(bitmap)
                                            },
                                            onCreateTextNote = { title, content ->
                                                transferViewModel.prepareStreamFromTextNote(title, content)
                                            },
                                            onLoadSampleFile = {
                                                transferViewModel.loadSampleSpecDocument()
                                            },
                                            onTogglePlayPause = {
                                                transferViewModel.toggleSenderPlayPause()
                                            },
                                            onStepFrame = { delta ->
                                                transferViewModel.stepSenderFrame(delta)
                                            },
                                            onAdvanceAutoFrame = {
                                                transferViewModel.advanceToNextActiveFrame()
                                            },
                                            onFpsChange = { fps ->
                                                transferViewModel.updateSenderFps(fps)
                                            },
                                            onUpdateConfig = { chunkSize, ecc ->
                                                transferViewModel.updateStreamConfig(chunkSize, ecc)
                                            },
                                            onUpdateEncryptionPassword = { pass ->
                                                transferViewModel.updateSenderEncryptionPassword(pass)
                                            },
                                            onToggleAutoMaxBrightness = {
                                                transferViewModel.toggleAutoMaxBrightness()
                                            },
                                            onScanNackQr = { qrStr ->
                                                transferViewModel.applyScannedNackQr(qrStr)
                                            },
                                            onToggleMissingChunkFilter = { idx ->
                                                transferViewModel.toggleMissingChunkFilter(idx)
                                            },
                                            onClearMissingChunkFilter = {
                                                transferViewModel.clearMissingChunkFilter()
                                            },
                                            onNavigateToReceiveLoopback = {
                                                currentTab = OptiShareTab.RECEIVE
                                                transferViewModel.startOpticalLoopbackSimulation(simulateFrameDrops = true)
                                            }
                                        )
                                    }

                                    OptiShareTab.RECEIVE -> {
                                        ReceiveScanScreen(
                                            receiverState = receiverState,
                                            onQrScanned = { qrStr ->
                                                transferViewModel.onQrStringScanned(qrStr)
                                            },
                                            onResetSession = {
                                                transferViewModel.resetReceiverSession()
                                            },
                                            onStartLoopbackSimulation = {
                                                transferViewModel.startOpticalLoopbackSimulation(simulateFrameDrops = true)
                                            },
                                            onGenerateNackQr = {
                                                transferViewModel.generateReceiverNackQr()
                                            },
                                            onDismissNackQr = {
                                                transferViewModel.dismissReceiverNackQr()
                                            },
                                            onSubmitDecryptionPassword = { pass ->
                                                transferViewModel.submitReceiverDecryptionPassword(pass)
                                            },
                                            onRetransmitCompletedRecord = { record ->
                                                transferViewModel.loadVaultRecordIntoSender(record)
                                                currentTab = OptiShareTab.SEND
                                            },
                                            onOpenVault = {
                                                currentTab = OptiShareTab.VAULT
                                            }
                                        )
                                    }

                                    OptiShareTab.VAULT -> {
                                        VaultScreen(
                                            records = vaultRecords,
                                            onRetransmitViaQr = { record ->
                                                transferViewModel.loadVaultRecordIntoSender(record)
                                                currentTab = OptiShareTab.SEND
                                            },
                                            onRenameRecord = { id, newName ->
                                                transferViewModel.renameVaultRecord(id, newName)
                                            },
                                            onDeleteRecord = { record ->
                                                transferViewModel.deleteVaultRecord(record)
                                            }
                                        )
                                    }

                                    OptiShareTab.SETTINGS -> {
                                        SettingsScreen(
                                            customization = customization,
                                            activeRefreshRateHz = activeRefreshRateHz,
                                            onUpdateCustomization = { transform ->
                                                themeManager.update(transform)
                                            },
                                            onApplyPreset = { preset ->
                                                themeManager.applyQuickPreset(preset)
                                            },
                                            onResetDefaults = {
                                                themeManager.resetToDefaults()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Bottom Navigation Bar on compact/mobile screens
                    if (!isExpandedScreen) {
                        FloatingMaterialNavigationBar(
                            currentTab = currentTab,
                            onSelectTab = { currentTab = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Bottom Navigation Bar with iOS-inspired Liquid Glass refinement:
 * Detached from the screen edges with balanced 24dp corners, frosted translucency,
 * directional specular rim, top 1dp meniscus highlight, and 120Hz spring states
 * (normal, pressed, selected, and focused).
 */
@Composable
private fun FloatingMaterialNavigationBar(
    currentTab: OptiShareTab,
    onSelectTab: (OptiShareTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassTokens = rememberLiquidGlassTokens()
    val itemRadius = 16.dp
    val itemShape = remember { RoundedCornerShape(itemRadius) }

    LiquidGlassBarSurface(
        cornerRadius = 24.dp,
        accentTint = colorScheme.primary,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .testTag("floating_bottom_navigation_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OptiShareTab.entries.forEach { tab ->
                val selected = currentTab == tab
                val labelText = stringResource(tab.titleRes)
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val isFocused by interactionSource.collectIsFocusedAsState()

                val itemContainerColor by animateColorAsState(
                    targetValue = when {
                        !selected -> Color.Transparent
                        glassTokens.reduceTransparency -> colorScheme.secondaryContainer
                        else -> colorScheme.secondaryContainer.copy(alpha = 0.82f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "NavTabContainer120Hz"
                )
                val itemContentColor by animateColorAsState(
                    targetValue = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "NavTabContent120Hz"
                )
                val iconScale by animateFloatAsState(
                    targetValue = when {
                        isPressed && !glassTokens.reduceMotion -> 0.94f
                        selected && !glassTokens.reduceMotion -> 1.06f
                        else -> 1.0f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "NavTabScale120Hz"
                )

                Surface(
                    onClick = { onSelectTab(tab) },
                    shape = itemShape,
                    color = itemContainerColor,
                    contentColor = itemContentColor,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .then(
                            if (selected || isFocused) {
                                Modifier.liquidGlassRimAndHighlight(
                                    cornerRadius = itemRadius,
                                    accentColor = colorScheme.primary,
                                    isDark = glassTokens.isDark,
                                    specularAlpha = if (isFocused) {
                                        (glassTokens.specularRimAlpha * 1.5f).coerceAtMost(0.75f)
                                    } else {
                                        (glassTokens.specularRimAlpha * 1.15f).coerceAtMost(0.55f)
                                    },
                                    meniscusAlpha = glassTokens.innerMeniscusAlpha,
                                    refractionAlpha = glassTokens.refractionTintAlpha * 0.7f
                                )
                            } else {
                                Modifier
                            }
                        )
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        }
                        .testTag(tab.testTag)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = labelText,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
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
