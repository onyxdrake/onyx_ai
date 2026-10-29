package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvSettingsSheet(
    settings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var geminiKey by remember { mutableStateOf(settings.geminiApiKey) }
    var groqKey by remember { mutableStateOf(settings.groqApiKey) }
    var cerebrasKey by remember { mutableStateOf(settings.cerebrasApiKey) }
    var mistralKey by remember { mutableStateOf(settings.mistralApiKey) }
    var openRouterKey by remember { mutableStateOf(settings.openRouterApiKey) }

    var termuxUrl by remember { mutableStateOf(settings.termuxServerUrl) }
    var isTorEnabled by remember { mutableStateOf(settings.isTorEnabled) }
    var torHost by remember { mutableStateOf(settings.torHost) }
    var torPortStr by remember { mutableStateOf(settings.torPort.toString()) }

    var discordWebhook by remember { mutableStateOf(settings.discordWebhookUrl) }
    var telegramToken by remember { mutableStateOf(settings.telegramBotToken) }
    var telegramChatId by remember { mutableStateOf(settings.telegramChatId) }
    var gmailUser by remember { mutableStateOf(settings.gmailUser) }
    var gmailAppPassword by remember { mutableStateOf(settings.gmailAppPassword) }

    var customBg by remember { mutableStateOf(settings.customBgUri.orEmpty()) }
    var bgDarkness by remember { mutableStateOf(settings.bgDarkness) }
    var ambientAudio by remember { mutableStateOf(settings.ambientAudioEnabled) }
    var chimeSound by remember { mutableStateOf(settings.chimeSoundEnabled) }
    var ttsVoice by remember { mutableStateOf(settings.ttsVoiceEnabled) }

    var rawEnvText by remember {
        mutableStateOf(
            """
            GEMINI_API_KEY=${settings.geminiApiKey}
            GROQ_API_KEY=${settings.groqApiKey}
            CEREBRAS_API_KEY=${settings.cerebrasApiKey}
            MISTRAL_API_KEY=${settings.mistralApiKey}
            OPENROUTER_API_KEY=${settings.openRouterApiKey}
            TERMUX_SERVER_URL=${settings.termuxServerUrl}
            TOR_PROXY_HOST=${settings.torHost}
            TOR_PROXY_PORT=${settings.torPort}
            DISCORD_WEBHOOK_URL=${settings.discordWebhookUrl}
            TELEGRAM_BOT_TOKEN=${settings.telegramBotToken}
            TELEGRAM_CHAT_ID=${settings.telegramChatId}
            GMAIL_USER=${settings.gmailUser}
            GMAIL_APP_PASSWORD=${settings.gmailAppPassword}
            """.trimIndent()
        )
    }

    fun applyAndSave() {
        val updated = settings.copy(
            geminiApiKey = geminiKey.trim(),
            groqApiKey = groqKey.trim(),
            cerebrasApiKey = cerebrasKey.trim(),
            mistralApiKey = mistralKey.trim(),
            openRouterApiKey = openRouterKey.trim(),
            termuxServerUrl = termuxUrl.trim(),
            isTorEnabled = isTorEnabled,
            torHost = torHost.trim(),
            torPort = torPortStr.toIntOrNull() ?: 9050,
            discordWebhookUrl = discordWebhook.trim(),
            telegramBotToken = telegramToken.trim(),
            telegramChatId = telegramChatId.trim(),
            gmailUser = gmailUser.trim(),
            gmailAppPassword = gmailAppPassword.trim(),
            customBgUri = customBg.trim().ifBlank { null },
            bgDarkness = bgDarkness,
            ambientAudioEnabled = ambientAudio,
            chimeSoundEnabled = chimeSound,
            ttsVoiceEnabled = ttsVoice
        )
        onSaveSettings(updated)
        Toast.makeText(context, "Settings & .env saved successfully!", Toast.LENGTH_SHORT).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zenith AI .env & System Config",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurfaceElevated,
                contentColor = NeonCyan
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("AI Keys", fontSize = 11.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Termux/Tor", fontSize = 11.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Integrations", fontSize = 11.sp) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Audio/Visual", fontSize = 11.sp) })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Raw .env", fontSize = 11.sp) })
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // AI Keys Tab
                    Text("AI Provider API Keys", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Configure your keys directly or via raw .env tab.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    KeyField("Gemini API Key", geminiKey) { geminiKey = it }
                    KeyField("Groq API Key (Llama 3.3)", groqKey) { groqKey = it }
                    KeyField("Cerebras API Key (Ultra-Fast)", cerebrasKey) { cerebrasKey = it }
                    KeyField("Mistral API Key", mistralKey) { mistralKey = it }
                    KeyField("OpenRouter API Key (DeepSeek/Claude)", openRouterKey) { openRouterKey = it }
                }

                1 -> {
                    // Termux & Tor Tab
                    Text("Termux Bridge & Tor Network Proxy", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = termuxUrl,
                        onValueChange = { termuxUrl = it },
                        label = { Text("Termux Server Endpoint", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tor / SOCKS5 Proxy Mode", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Routes AI network calls anonymously", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isTorEnabled,
                            onCheckedChange = { isTorEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonViolet
                            )
                        )
                    }

                    if (isTorEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = torHost,
                                onValueChange = { torHost = it },
                                label = { Text("Proxy Host", color = TextSecondary) },
                                modifier = Modifier.weight(2f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = torPortStr,
                                onValueChange = { torPortStr = it },
                                label = { Text("Port", color = TextSecondary) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                2 -> {
                    // Integrations Tab
                    Text("Discord, Telegram & Gmail Integrations", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    KeyField("Discord Webhook URL", discordWebhook, isSecret = false) { discordWebhook = it }
                    KeyField("Telegram Bot Token", telegramToken) { telegramToken = it }
                    KeyField("Telegram Chat ID", telegramChatId, isSecret = false) { telegramChatId = it }
                    KeyField("Gmail User (Email)", gmailUser, isSecret = false) { gmailUser = it }
                    KeyField("Gmail App Password (for OTP retrieval)", gmailAppPassword) { gmailAppPassword = it }
                }

                3 -> {
                    // Audio & Visual Tab
                    Text("Anime Wallpaper & Audio Tuning", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customBg,
                        onValueChange = { customBg = it },
                        label = { Text("Custom Background Image URL (Leave blank for default Anime)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Wallpaper Darkness Scrim: ${(bgDarkness * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = bgDarkness,
                        onValueChange = { bgDarkness = it },
                        valueRange = 0.1f..0.85f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ambient Sci-Fi Background Audio", color = TextPrimary)
                        Switch(checked = ambientAudio, onCheckedChange = { ambientAudio = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("5-Second Completion Chime Melody", color = TextPrimary)
                        Switch(checked = chimeSound, onCheckedChange = { chimeSound = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Text-To-Speech (AI Anime Voice)", color = TextPrimary)
                        Switch(checked = ttsVoice, onCheckedChange = { ttsVoice = it })
                    }
                }

                4 -> {
                    // Raw .env tab
                    Text("Raw .env Editor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Paste or edit your complete .env file contents below:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawEnvText,
                        onValueChange = { rawEnvText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NeonGreen
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF090D14),
                            unfocusedContainerColor = Color(0xFF090D14)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // Parse raw env
                            val lines = rawEnvText.lines()
                            for (line in lines) {
                                val trimmed = line.trim()
                                if (trimmed.startsWith("#") || !trimmed.contains("=")) continue
                                val parts = trimmed.split("=", limit = 2)
                                val key = parts[0].trim()
                                val value = parts[1].trim()
                                when (key) {
                                    "GEMINI_API_KEY" -> geminiKey = value
                                    "GROQ_API_KEY" -> groqKey = value
                                    "CEREBRAS_API_KEY" -> cerebrasKey = value
                                    "MISTRAL_API_KEY" -> mistralKey = value
                                    "OPENROUTER_API_KEY" -> openRouterKey = value
                                    "TERMUX_SERVER_URL" -> termuxUrl = value
                                    "TOR_PROXY_HOST" -> torHost = value
                                    "TOR_PROXY_PORT" -> torPortStr = value
                                    "DISCORD_WEBHOOK_URL" -> discordWebhook = value
                                    "TELEGRAM_BOT_TOKEN" -> telegramToken = value
                                    "TELEGRAM_CHAT_ID" -> telegramChatId = value
                                    "GMAIL_USER" -> gmailUser = value
                                    "GMAIL_APP_PASSWORD" -> gmailAppPassword = value
                                }
                            }
                            applyAndSave()
                            Toast.makeText(context, "Parsed & Imported from .env!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Parse & Apply .env Values", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    applyAndSave()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save All Configuration", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KeyField(
    label: String,
    value: String,
    isSecret: Boolean = true,
    onValueChange: (String) -> Unit
) {
    var isVisible by remember { mutableStateOf(!isSecret) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            if (isSecret) {
                IconButton(onClick = { isVisible = !isVisible }) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = TextSecondary
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}
