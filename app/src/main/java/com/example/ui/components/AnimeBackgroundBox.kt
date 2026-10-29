package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.DarkBackground

@Composable
fun AnimeBackgroundBox(
    customBgUri: String?,
    bgDarkness: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val model = customBgUri?.ifBlank { null } ?: R.drawable.bg_anime_default

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        // Background Wallpaper
        AsyncImage(
            model = model,
            contentDescription = "Anime background wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient & Darkness Scrim for perfect readability
        val darknessColor = Color.Black.copy(alpha = bgDarkness.coerceIn(0.1f, 0.9f))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            darknessColor.copy(alpha = (bgDarkness + 0.2f).coerceAtMost(0.95f)),
                            darknessColor,
                            darknessColor.copy(alpha = (bgDarkness + 0.35f).coerceAtMost(0.98f))
                        )
                    )
                )
        )

        // Child composables
        content()
    }
}
