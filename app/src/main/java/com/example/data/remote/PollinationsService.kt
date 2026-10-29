package com.example.data.remote

import java.net.URLEncoder

object PollinationsService {

    enum class ImageModel(val id: String, val label: String) {
        FLUX("flux", "Flux High-Definition"),
        TURBO("turbo", "Turbo Fast"),
        ANIME("flux-anime", "Anime Fantasy")
    }

    fun buildImageUrl(
        prompt: String,
        model: ImageModel = ImageModel.FLUX,
        width: Int = 1024,
        height: Int = 1024,
        seed: Long = System.currentTimeMillis() % 100000
    ): String {
        val safePrompt = prompt.trim().ifBlank { "beautiful anime cyberpunk scenery glowing neon" }
        val encodedPrompt = URLEncoder.encode(safePrompt, "UTF-8").replace("+", "%20")
        return "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=${model.id}&seed=$seed&nologo=true&enhance=true"
    }
}
