package com.example.data.model

enum class AIProvider(
    val displayName: String,
    val defaultModel: String,
    val badgeColorHex: Long
) {
    GEMINI("Gemini", "gemini-2.0-flash", 0xFF00E5FF),
    GROQ("Groq", "llama-3.3-70b-versatile", 0xFFFF9100),
    CEREBRAS("Cerebras", "llama-3.3-70b", 0xFFFF4081),
    MISTRAL("Mistral", "mistral-large-latest", 0xFFFF5722),
    OPENROUTER("OpenRouter", "deepseek/deepseek-r1:free", 0xFF7C4DFF),
    TERMUX_SERVER("Termux Node", "termux-agent", 0xFF00E676);

    companion object {
        fun fromString(name: String?): AIProvider {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: GEMINI
        }
    }
}
