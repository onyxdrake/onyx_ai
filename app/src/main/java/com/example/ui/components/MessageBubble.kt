package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.ChatMessage
import com.example.ui.theme.AssistantBubbleBg
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    onSpeak: (String) -> Unit,
    onRunSandbox: (String) -> Unit,
    onDispatchDiscord: (String) -> Unit,
    onDispatchTelegram: (String) -> Unit,
    onDeleteMessage: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    // Check for code blocks
    val codeBlocks = extractCodeBlocks(message.content)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) UserBubbleBg else AssistantBubbleBg
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) NeonViolet.copy(alpha = 0.4f) else DarkSurfaceBorder
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header (Assistant model indicator & time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else (message.modelUsed.ifBlank { "Zenith AI" }),
                        color = if (isUser) NeonCyan else NeonPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = timeString,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Attached Image (Pollinations or URL)
                if (!message.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Image preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        loading = {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NeonCyan)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Text Content
                Text(
                    text = cleanContentForDisplay(message.content),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Render detected Code Blocks with HTML Sandbox Runner
                codeBlocks.forEach { block ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF090D14)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = block.language.uppercase().ifBlank { "CODE" },
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row {
                                    if (block.isHtmlOrWeb) {
                                        Button(
                                            onClick = { onRunSandbox(block.code) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Run Sandbox", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("code", block.code))
                                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = block.code,
                                color = Color(0xFF80D8FF),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }

                // Action Bar for assistant messages (TTS, Copy, Discord, Telegram)
                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSpeak(message.content) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("chat", message.content))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDispatchDiscord(message.content) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Send, contentDescription = "Discord", tint = Color(0xFF5865F2), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDispatchTelegram(message.content) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Send, contentDescription = "Telegram", tint = Color(0xFF0088CC), modifier = Modifier.size(16.dp))
                        }
                        if (onDeleteMessage != null) {
                            IconButton(onClick = { onDeleteMessage(message.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Delete Message", tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ParsedCodeBlock(
    val language: String,
    val code: String,
    val isHtmlOrWeb: Boolean
)

private fun extractCodeBlocks(content: String): List<ParsedCodeBlock> {
    val blocks = mutableListOf<ParsedCodeBlock>()
    val regex = Regex("```([a-zA-Z0-9]*)\\n([\\s\\S]*?)```")
    regex.findAll(content).forEach { match ->
        val lang = match.groupValues[1].lowercase()
        val code = match.groupValues[2].trim()
        val isHtmlOrWeb = lang in listOf("html", "htm", "javascript", "js", "css", "svg", "xml") ||
                code.contains("<html", ignoreCase = true) ||
                code.contains("<div", ignoreCase = true) ||
                code.contains("<canvas", ignoreCase = true)
        blocks.add(ParsedCodeBlock(language = lang, code = code, isHtmlOrWeb = isHtmlOrWeb))
    }
    return blocks
}

private fun cleanContentForDisplay(content: String): String {
    // If the message is purely a code block, keep a brief intro
    return if (content.contains("```")) {
        content.replace(Regex("```([a-zA-Z0-9]*)\\n([\\s\\S]*?)```"), "\n[Code Block below]\n").trim()
    } else {
        content
    }
}
