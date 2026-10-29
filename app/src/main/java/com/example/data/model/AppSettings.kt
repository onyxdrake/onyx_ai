package com.example.data.model

data class AppSettings(
    val geminiApiKey: String = "",
    val groqApiKey: String = "",
    val cerebrasApiKey: String = "",
    val mistralApiKey: String = "",
    val openRouterApiKey: String = "",
    val termuxServerUrl: String = "http://127.0.0.1:8080",
    val isTorEnabled: Boolean = false,
    val torHost: String = "127.0.0.1",
    val torPort: Int = 9050,
    val discordWebhookUrl: String = "",
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val gmailUser: String = "",
    val gmailAppPassword: String = "",
    val customBgUri: String? = null,
    val bgDarkness: Float = 0.45f,
    val ambientAudioEnabled: Boolean = false,
    val chimeSoundEnabled: Boolean = true,
    val ttsVoiceEnabled: Boolean = false,
    val activeProvider: AIProvider = AIProvider.GEMINI,
    val activeModel: String = AIProvider.GEMINI.defaultModel,
    val isWebSearchEnabled: Boolean = false
)
