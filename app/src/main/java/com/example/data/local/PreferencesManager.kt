package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.AIProvider
import com.example.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zenith_ai_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun getEnvDefault(key: String, fallback: String = ""): String {
        return try {
            val field = BuildConfig::class.java.getField(key)
            val value = field.get(null) as? String
            if (!value.isNullOrBlank() && value != "placeholder" && value != "MY_GEMINI_API_KEY") value else fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun loadSettings(): AppSettings {
        val defaultTermux = getEnvDefault("TERMUX_SERVER_URL", "http://127.0.0.1:8080")
        val defaultTorHost = getEnvDefault("TOR_PROXY_HOST", "127.0.0.1")
        val defaultTorPortStr = getEnvDefault("TOR_PROXY_PORT", "9050")
        val defaultTorPort = defaultTorPortStr.toIntOrNull() ?: 9050

        return AppSettings(
            geminiApiKey = prefs.getString("geminiApiKey", null)
                ?: getEnvDefault("GEMINI_API_KEY"),
            groqApiKey = prefs.getString("groqApiKey", null)
                ?: getEnvDefault("GROQ_API_KEY"),
            cerebrasApiKey = prefs.getString("cerebrasApiKey", null)
                ?: getEnvDefault("CEREBRAS_API_KEY"),
            mistralApiKey = prefs.getString("mistralApiKey", null)
                ?: getEnvDefault("MISTRAL_API_KEY"),
            openRouterApiKey = prefs.getString("openRouterApiKey", null)
                ?: getEnvDefault("OPENROUTER_API_KEY"),
            termuxServerUrl = prefs.getString("termuxServerUrl", null) ?: defaultTermux,
            isTorEnabled = prefs.getBoolean("isTorEnabled", false),
            torHost = prefs.getString("torHost", null) ?: defaultTorHost,
            torPort = prefs.getInt("torPort", defaultTorPort),
            discordWebhookUrl = prefs.getString("discordWebhookUrl", null)
                ?: getEnvDefault("DISCORD_WEBHOOK_URL"),
            telegramBotToken = prefs.getString("telegramBotToken", null)
                ?: getEnvDefault("TELEGRAM_BOT_TOKEN"),
            telegramChatId = prefs.getString("telegramChatId", null)
                ?: getEnvDefault("TELEGRAM_CHAT_ID"),
            gmailUser = prefs.getString("gmailUser", null)
                ?: getEnvDefault("GMAIL_USER"),
            gmailAppPassword = prefs.getString("gmailAppPassword", null)
                ?: getEnvDefault("GMAIL_APP_PASSWORD"),
            customBgUri = prefs.getString("customBgUri", null),
            bgDarkness = prefs.getFloat("bgDarkness", 0.45f),
            ambientAudioEnabled = prefs.getBoolean("ambientAudioEnabled", false),
            chimeSoundEnabled = prefs.getBoolean("chimeSoundEnabled", true),
            ttsVoiceEnabled = prefs.getBoolean("ttsVoiceEnabled", false),
            activeProvider = AIProvider.fromString(
                prefs.getString("activeProvider", AIProvider.GEMINI.name)
            ),
            activeModel = prefs.getString("activeModel", AIProvider.GEMINI.defaultModel)
                ?: AIProvider.GEMINI.defaultModel,
            isWebSearchEnabled = prefs.getBoolean("isWebSearchEnabled", false)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("geminiApiKey", settings.geminiApiKey)
            .putString("groqApiKey", settings.groqApiKey)
            .putString("cerebrasApiKey", settings.cerebrasApiKey)
            .putString("mistralApiKey", settings.mistralApiKey)
            .putString("openRouterApiKey", settings.openRouterApiKey)
            .putString("termuxServerUrl", settings.termuxServerUrl)
            .putBoolean("isTorEnabled", settings.isTorEnabled)
            .putString("torHost", settings.torHost)
            .putInt("torPort", settings.torPort)
            .putString("discordWebhookUrl", settings.discordWebhookUrl)
            .putString("telegramBotToken", settings.telegramBotToken)
            .putString("telegramChatId", settings.telegramChatId)
            .putString("gmailUser", settings.gmailUser)
            .putString("gmailAppPassword", settings.gmailAppPassword)
            .putString("customBgUri", settings.customBgUri)
            .putFloat("bgDarkness", settings.bgDarkness)
            .putBoolean("ambientAudioEnabled", settings.ambientAudioEnabled)
            .putBoolean("chimeSoundEnabled", settings.chimeSoundEnabled)
            .putBoolean("ttsVoiceEnabled", settings.ttsVoiceEnabled)
            .putString("activeProvider", settings.activeProvider.name)
            .putString("activeModel", settings.activeModel)
            .putBoolean("isWebSearchEnabled", settings.isWebSearchEnabled)
            .apply()

        _settingsFlow.value = settings
    }
}
