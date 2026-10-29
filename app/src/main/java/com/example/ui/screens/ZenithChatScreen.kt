package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.components.AnimeBackgroundBox
import com.example.ui.components.ChatInputBar
import com.example.ui.components.ChatSessionsDrawer
import com.example.ui.components.CodeSandboxDialog
import com.example.ui.components.EnvSettingsSheet
import com.example.ui.components.FuturisticTopBar
import com.example.ui.components.MessageBubble
import com.example.ui.components.PollinationsGallerySheet
import com.example.ui.components.TermuxServerDialog
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ZenithChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val termuxStatus by viewModel.termuxStatus.collectAsState()
    val sandboxCode by viewModel.sandboxCode.collectAsState()
    val isArtStudioOpen by viewModel.isArtStudioOpen.collectAsState()
    val isTermuxCenterOpen by viewModel.isTermuxCenterOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()

    val drawerState = rememberDrawerState(
        initialValue = if (isDrawerOpen) DrawerValue.Open else DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Sync drawer state
    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen && isDrawerOpen) {
            viewModel.closeDrawer()
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
        viewModel.closeDrawer()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerContentColor = Color.White
            ) {
                ChatSessionsDrawer(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onSelectSession = { session ->
                        viewModel.selectSession(session)
                        scope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { sessionId ->
                        viewModel.deleteSession(sessionId)
                    },
                    onClearAllHistory = {
                        viewModel.clearAllHistory()
                        scope.launch { drawerState.close() }
                    },
                    onExportHistory = {
                        viewModel.exportCurrentChatAsMarkdown()
                    }
                )
            }
        }
    ) {
        AnimeBackgroundBox(
            customBgUri = settings.customBgUri,
            bgDarkness = settings.bgDarkness
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    FuturisticTopBar(
                        settings = settings,
                        termuxStatus = termuxStatus,
                        onProviderSelected = { viewModel.setProvider(it) },
                        onOpenTermuxCenter = { viewModel.openTermuxCenter() },
                        onOpenArtStudio = { viewModel.openArtStudio() },
                        onOpenSettings = { viewModel.openSettings() },
                        onToggleAmbientAudio = { viewModel.toggleAmbientAudio() },
                        onNewChat = { viewModel.createNewSession() },
                        onToggleDrawer = {
                            viewModel.toggleDrawer()
                        }
                    )
                },
                bottomBar = {
                    ChatInputBar(
                        input = inputText,
                        onInputChange = { viewModel.onInputChange(it) },
                        onSend = { viewModel.sendMessage() },
                        isGenerating = isGenerating,
                        isWebSearchEnabled = settings.isWebSearchEnabled,
                        onToggleWebSearch = { viewModel.toggleWebSearch() },
                        onSelectQuickPrompt = { prompt ->
                            viewModel.sendMessage(prompt)
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                onSpeak = { text -> viewModel.audioSynthesizer.speak(text) },
                                onRunSandbox = { code -> viewModel.openSandbox(code) },
                                onDispatchDiscord = { text -> viewModel.dispatchToDiscord(text) },
                                onDispatchTelegram = { text -> viewModel.dispatchToTelegram(text) },
                                onDeleteMessage = { messageId -> viewModel.deleteMessage(messageId) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Modals
    if (sandboxCode != null) {
        CodeSandboxDialog(
            initialCode = sandboxCode.orEmpty(),
            onDismiss = { viewModel.closeSandbox() }
        )
    }

    if (isArtStudioOpen) {
        PollinationsGallerySheet(
            onDismiss = { viewModel.closeArtStudio() },
            onSetAsWallpaper = { url ->
                viewModel.saveSettings(settings.copy(customBgUri = url))
            },
            onInsertToChat = { url ->
                viewModel.insertImageToChat(url)
            }
        )
    }

    if (isTermuxCenterOpen) {
        TermuxServerDialog(
            settings = settings,
            termuxStatus = termuxStatus,
            onRefreshStatus = { viewModel.checkTermuxStatus() },
            onSaveSettings = { updated -> viewModel.saveSettings(updated) },
            onDismiss = { viewModel.closeTermuxCenter() }
        )
    }

    if (isSettingsOpen) {
        EnvSettingsSheet(
            settings = settings,
            onSaveSettings = { updated -> viewModel.saveSettings(updated) },
            onDismiss = { viewModel.closeSettings() }
        )
    }
}
