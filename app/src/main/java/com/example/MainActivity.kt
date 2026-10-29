package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.TransferRepository
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.ReceiveScanScreen
import com.example.ui.screens.SendQrScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.OptiShareTheme
import com.example.ui.theme.OpticalCyan
import com.example.util.configureAppCheck
import com.example.viewmodel.AiStudioViewModel
import com.example.viewmodel.TransferViewModel

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
    AI_STUDIO(
        titleRes = R.string.nav_ai_studio,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_ai_studio"
    ),
    VAULT(
        titleRes = R.string.nav_vault,
        selectedIcon = Icons.Filled.FolderSpecial,
        unselectedIcon = Icons.Outlined.FolderSpecial,
        testTag = "nav_tab_vault"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TransferRepository(applicationContext, database.transferDao())

        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }

            OptiShareTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OptiShareApp(
                        repository = repository,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = { isDarkTheme = !isDarkTheme }
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
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit
) {
    val context = LocalContext.current
    val transferViewModel: TransferViewModel = viewModel(
        factory = TransferViewModel.Factory(repository)
    )
    val aiStudioViewModel: AiStudioViewModel = viewModel(
        factory = AiStudioViewModel.Factory(repository)
    )

    val senderState by transferViewModel.senderState.collectAsStateWithLifecycle()
    val receiverState by transferViewModel.receiverState.collectAsStateWithLifecycle()
    val vaultRecords by transferViewModel.vaultRecords.collectAsStateWithLifecycle()
    val aiUiState by aiStudioViewModel.uiState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(OptiShareTab.SEND) }

    // BackHandler on secondary screens to return to primary SEND tab
    if (currentTab != OptiShareTab.SEND) {
        BackHandler {
            currentTab = OptiShareTab.SEND
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row {
                            Text(
                                text = "OptiShare ",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "QR",
                                style = MaterialTheme.typography.titleLarge,
                                color = OpticalCyan
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onToggleDarkTheme,
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Cambia tema chiaro/scuro"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar {
                        OptiShareTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.titleRes)
                                    )
                                },
                                label = { Text(stringResource(tab.titleRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail {
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
                                label = { Text(stringResource(tab.titleRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        OptiShareTab.SEND -> {
                            SendQrScreen(
                                senderState = senderState,
                                onPickFileUri = { uri ->
                                    transferViewModel.prepareStreamFromUri(context, uri)
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
                                onAnalyzeCompletedWithAi = { record ->
                                    currentTab = OptiShareTab.AI_STUDIO
                                    aiStudioViewModel.analyzeVaultRecord(record)
                                },
                                onOpenVault = {
                                    currentTab = OptiShareTab.VAULT
                                }
                            )
                        }

                        OptiShareTab.AI_STUDIO -> {
                            AiStudioScreen(
                                uiState = aiUiState,
                                recentRecords = vaultRecords,
                                onAnalyzeBitmap = { bmp, title ->
                                    aiStudioViewModel.analyzeCapturedImage(bmp, title)
                                },
                                onAnalyzeCustomText = { text, title ->
                                    aiStudioViewModel.analyzeCustomTextOrDocument(text, title)
                                },
                                onAnalyzeVaultRecord = { record ->
                                    aiStudioViewModel.analyzeVaultRecord(record)
                                },
                                onApplySmartRename = {
                                    aiStudioViewModel.applySmartRenameToSourceRecord()
                                },
                                onSendExtractedTextViaQr = { fileName, extractedContent ->
                                    transferViewModel.prepareStreamFromTextNote(fileName, extractedContent)
                                    currentTab = OptiShareTab.SEND
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
                                onAnalyzeWithAi = { record ->
                                    aiStudioViewModel.analyzeVaultRecord(record)
                                    currentTab = OptiShareTab.AI_STUDIO
                                },
                                onRenameRecord = { id, newName ->
                                    transferViewModel.renameVaultRecord(id, newName)
                                },
                                onDeleteRecord = { record ->
                                    transferViewModel.deleteVaultRecord(record)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
