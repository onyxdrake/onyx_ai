package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynthesizer
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.AIProvider
import com.example.data.model.AppSettings
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.remote.AIService
import com.example.data.remote.IntegrationDispatchService
import com.example.data.remote.TermuxBridge
import com.example.data.remote.TermuxStatus
import com.example.data.remote.WebSearchService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val prefsManager = PreferencesManager(application)
    val audioSynthesizer = AudioSynthesizer(application)

    private val aiService = AIService()
    private val termuxBridge = TermuxBridge()
    private val webSearchService = WebSearchService()
    private val integrationDispatch = IntegrationDispatchService()

    val settings: StateFlow<AppSettings> = prefsManager.settingsFlow

    val sessions: StateFlow<List<ChatSession>> = chatDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentSessionId = MutableStateFlow<String>("")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _termuxStatus = MutableStateFlow(
        TermuxStatus(isOnline = false, latencyMs = -1, message = "Checking Termux...")
    )
    val termuxStatus: StateFlow<TermuxStatus> = _termuxStatus.asStateFlow()

    private val _sandboxCode = MutableStateFlow<String?>(null)
    val sandboxCode: StateFlow<String?> = _sandboxCode.asStateFlow()

    private val _isArtStudioOpen = MutableStateFlow(false)
    val isArtStudioOpen: StateFlow<Boolean> = _isArtStudioOpen.asStateFlow()

    private val _isTermuxCenterOpen = MutableStateFlow(false)
    val isTermuxCenterOpen: StateFlow<Boolean> = _isTermuxCenterOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private var messageObserverJob: Job? = null

    init {
        checkTermuxStatus()
        viewModelScope.launch {
            // Initialize default session if empty
            val initialSessions = chatDao.getSessionById("default")
            if (initialSessions == null) {
                val newSession = ChatSession(
                    id = "default",
                    title = "Zenith AI Workspace",
                    provider = settings.value.activeProvider.name
                )
                chatDao.insertSession(newSession)
                _currentSessionId.value = "default"

                // Add welcome message
                chatDao.insertMessage(
                    ChatMessage(
                        sessionId = "default",
                        role = "assistant",
                        content = """
                        ✨ **Welcome to Zenith AI** ✨
                        
                        I am your high-performance AI companion, equipped with:
                        • **Multi-Model Intelligence**: Switch between Gemini, Groq (Llama 3.3), Cerebras (Ultra-Fast), Mistral, and OpenRouter.
                        • **Termux Server Bridge**: Seamless local node pairing with automated scripts.
                        • **Live HTML / JS Sandbox**: Run and test web code instantly with one tap!
                        • **Tor / SOCKS5 Integration**: Encrypted, anonymous network routing.
                        • **Live Web Search**: Augment prompts with real-time web results.
                        • **Pollinations Art Studio**: Generate anime & cyberpunk artwork on the fly.
                        • **Bot Relays & OTP Reader**: Discord, Telegram, and Gmail 2FA helper.
                        
                        How can I assist you today?
                        """.trimIndent(),
                        modelUsed = "Zenith AI Core"
                    )
                )
            } else {
                _currentSessionId.value = initialSessions.id
            }

            observeMessagesForSession(_currentSessionId.value)
        }
    }

    private fun observeMessagesForSession(sessionId: String) {
        messageObserverJob?.cancel()
        messageObserverJob = viewModelScope.launch {
            chatDao.getMessagesForSession(sessionId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun checkTermuxStatus() {
        viewModelScope.launch {
            val status = termuxBridge.checkServerStatus(settings.value)
            _termuxStatus.value = status
        }
    }

    fun sendMessage(overridePrompt: String? = null) {
        val prompt = (overridePrompt ?: _inputText.value).trim()
        if (prompt.isBlank() || _isGenerating.value) return

        val sessionId = _currentSessionId.value
        val currentSettings = settings.value
        _inputText.value = ""
        _isGenerating.value = true

        viewModelScope.launch {
            // 1. Insert user message
            val userMsg = ChatMessage(
                sessionId = sessionId,
                role = "user",
                content = prompt,
                modelUsed = "User"
            )
            chatDao.insertMessage(userMsg)

            // Update session timestamp & title if first message
            val session = chatDao.getSessionById(sessionId)
            if (session != null) {
                val newTitle = if (session.title == "Zenith AI Workspace" || session.title == "New Session") {
                    prompt.take(30)
                } else session.title
                chatDao.updateSession(session.copy(title = newTitle, lastUpdatedAt = System.currentTimeMillis()))
            }

            // 2. Web search if enabled
            var searchContext: String? = null
            if (currentSettings.isWebSearchEnabled) {
                val searchResult = webSearchService.searchWeb(prompt, currentSettings)
                if (searchResult.isSuccess) {
                    searchContext = searchResult.getOrNull()?.summary
                }
            }

            // 3. Query AI Service
            val history = _messages.value
            val result = aiService.sendMessage(
                history = history,
                userMessage = prompt,
                settings = currentSettings,
                searchContext = searchContext
            )

            _isGenerating.value = false

            if (result.isSuccess) {
                val reply = result.getOrNull().orEmpty()
                val assistantMsg = ChatMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = reply,
                    modelUsed = "${currentSettings.activeProvider.displayName} (${currentSettings.activeModel})"
                )
                chatDao.insertMessage(assistantMsg)

                // 4. Audio Feedback
                if (currentSettings.chimeSoundEnabled) {
                    audioSynthesizer.play5SecondChime()
                }
                if (currentSettings.ttsVoiceEnabled) {
                    audioSynthesizer.speak(reply)
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error occurred"
                val assistantMsg = ChatMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "⚠️ [Execution Error]: $errorMsg\n\nPlease check your API keys or Termux Server connection in Settings.",
                    modelUsed = "System"
                )
                chatDao.insertMessage(assistantMsg)
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val newSession = ChatSession(
                id = newId,
                title = "New Session",
                provider = settings.value.activeProvider.name
            )
            chatDao.insertSession(newSession)
            _currentSessionId.value = newId
            observeMessagesForSession(newId)
            _isDrawerOpen.value = false
        }
    }

    fun selectSession(session: ChatSession) {
        _currentSessionId.value = session.id
        observeMessagesForSession(session.id)
        _isDrawerOpen.value = false
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first())
                } else {
                    createNewSession()
                }
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            chatDao.deleteMessage(messageId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            chatDao.deleteAllMessages()
            chatDao.deleteAllSessions()
            createNewSession()
        }
    }

    fun exportCurrentChatAsMarkdown(): String {
        val currentMsgs = _messages.value
        val session = sessions.value.find { it.id == _currentSessionId.value }
        val title = session?.title ?: "Zenith AI Conversation"
        val sb = java.lang.StringBuilder()
        sb.append("# ").append(title).append("\n\n")
        sb.append("*Exported from Zenith AI · Model: ").append(session?.provider ?: "Multi-Model").append("*\n\n")
        sb.append("---\n\n")
        for (msg in currentMsgs) {
            val roleName = if (msg.role == "user") "User" else (msg.modelUsed.ifBlank { "Zenith AI" })
            val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
            sb.append("### **").append(roleName).append("** (").append(time).append(")\n\n")
            if (!msg.imageUrl.isNullOrBlank()) {
                sb.append("![Image](").append(msg.imageUrl).append(")\n\n")
            }
            sb.append(msg.content).append("\n\n")
            sb.append("---\n\n")
        }
        return sb.toString()
    }

    fun setProvider(provider: AIProvider) {
        val updated = settings.value.copy(
            activeProvider = provider,
            activeModel = provider.defaultModel
        )
        prefsManager.saveSettings(updated)
    }

    fun toggleWebSearch() {
        val updated = settings.value.copy(
            isWebSearchEnabled = !settings.value.isWebSearchEnabled
        )
        prefsManager.saveSettings(updated)
    }

    fun toggleAmbientAudio() {
        val target = !settings.value.ambientAudioEnabled
        val updated = settings.value.copy(ambientAudioEnabled = target)
        prefsManager.saveSettings(updated)
        audioSynthesizer.setAmbientAudio(target)
    }

    fun saveSettings(newSettings: AppSettings) {
        prefsManager.saveSettings(newSettings)
        audioSynthesizer.setAmbientAudio(newSettings.ambientAudioEnabled)
        checkTermuxStatus()
    }

    fun insertImageToChat(url: String) {
        viewModelScope.launch {
            val sessionId = _currentSessionId.value
            val msg = ChatMessage(
                sessionId = sessionId,
                role = "assistant",
                content = "Generated artwork via Pollinations AI:",
                modelUsed = "Pollinations AI",
                imageUrl = url
            )
            chatDao.insertMessage(msg)
        }
    }

    fun openSandbox(code: String) {
        _sandboxCode.value = code
    }

    fun closeSandbox() {
        _sandboxCode.value = null
    }

    fun openArtStudio() {
        _isArtStudioOpen.value = true
    }

    fun closeArtStudio() {
        _isArtStudioOpen.value = false
    }

    fun openTermuxCenter() {
        _isTermuxCenterOpen.value = true
    }

    fun closeTermuxCenter() {
        _isTermuxCenterOpen.value = false
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun toggleDrawer() {
        _isDrawerOpen.value = !_isDrawerOpen.value
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun dispatchToDiscord(message: String) {
        viewModelScope.launch {
            integrationDispatch.sendToDiscord(message, settings.value)
        }
    }

    fun dispatchToTelegram(message: String) {
        viewModelScope.launch {
            integrationDispatch.sendToTelegram(message, settings.value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioSynthesizer.release()
    }
}
