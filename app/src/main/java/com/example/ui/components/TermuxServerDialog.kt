package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppSettings
import com.example.data.remote.GmailHelper
import com.example.data.remote.IntegrationDispatchService
import com.example.data.remote.OtpResult
import com.example.data.remote.TermuxBridge
import com.example.data.remote.TermuxStatus
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun TermuxServerDialog(
    settings: AppSettings,
    termuxStatus: TermuxStatus,
    onRefreshStatus: () -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val termuxBridge = remember { TermuxBridge() }
    val gmailHelper = remember { GmailHelper() }
    val integrationDispatch = remember { IntegrationDispatchService() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var serverUrl by remember { mutableStateOf(settings.termuxServerUrl) }
    var isCheckingOtp by remember { mutableStateOf(false) }
    var otpResult by remember { mutableStateOf<OtpResult?>(null) }
    var testBotMessage by remember { mutableStateOf("Hello from Zenith AI Termux Client!") }
    var isSendingBot by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (termuxStatus.isOnline) NeonGreen else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Termux Server Center",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row {
                            IconButton(onClick = onRefreshStatus) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                            }
                        }
                    }

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (termuxStatus.isOnline) Color(0x2200E676) else Color(0x22FF1744))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (termuxStatus.isOnline) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (termuxStatus.isOnline) NeonGreen else Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = termuxStatus.message,
                                color = if (termuxStatus.isOnline) NeonGreen else Color(0xFFFF8A80),
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Server URL Config
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = {
                                serverUrl = it
                                onSaveSettings(settings.copy(termuxServerUrl = it))
                            },
                            label = { Text("Termux Server Address", color = TextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRefreshStatus,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ping", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Tab Navigation
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkSurfaceElevated,
                        contentColor = NeonGreen
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Setup Script", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Gmail OTP", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Bot Relay", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    // Tab Body
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Setup scripts tab
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "1-Click Server Scripts for Termux:",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            copyToClipboard(
                                                "Termux Python Script",
                                                termuxBridge.getPythonServerScript()
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy Python Server Code (server.py)")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            copyToClipboard(
                                                "Termux Node.js Script",
                                                termuxBridge.getNodeServerScript()
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy Node.js Server Code (server.js)")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            copyToClipboard(
                                                "Termux Setup Command",
                                                "pkg update -y && pkg install python -y && pip install flask requests && python server.py"
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy Install & Run Command", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "How it works:\n• When you launch your Termux server script, this app automatically detects it and enables local AI routing, Discord bot dispatch, Telegram bot relay, and Gmail OTP automation.",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            1 -> {
                                // Gmail OTP tab
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Gmail OTP Assistant",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Fetch 2FA / OTP codes automatically from your Gmail via Termux server bridge.",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Account: ${settings.gmailUser.ifBlank { "Not set (configure in Settings)" }}",
                                        color = NeonCyan,
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isCheckingOtp = true
                                                val res = gmailHelper.fetchLatestOtp(settings)
                                                isCheckingOtp = false
                                                if (res.isSuccess) {
                                                    otpResult = res.getOrNull()
                                                    Toast.makeText(context, "OTP retrieved!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        res.exceptionOrNull()?.message ?: "Failed to fetch OTP",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        },
                                        enabled = !isCheckingOtp,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (isCheckingOtp) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Fetch Latest OTP Code")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (otpResult != null) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text("Detected OTP:", color = TextSecondary, fontSize = 12.sp)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = otpResult?.code ?: "No code found",
                                                        color = NeonGreen,
                                                        fontSize = 28.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Button(
                                                        onClick = {
                                                            otpResult?.code?.let { copyToClipboard("OTP Code", it) }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text("Copy", color = Color.Black)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Subject: ${otpResult?.subject}", color = TextPrimary, fontSize = 12.sp)
                                                Text("From: ${otpResult?.sender}", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // Bot relay tab
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Discord & Telegram Bot Dispatch",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = testBotMessage,
                                        onValueChange = { testBotMessage = it },
                                        label = { Text("Message to dispatch", color = TextSecondary) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonViolet,
                                            unfocusedBorderColor = DarkSurfaceBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isSendingBot = true
                                                val res = integrationDispatch.sendToDiscord(testBotMessage, settings)
                                                isSendingBot = false
                                                Toast.makeText(
                                                    context,
                                                    res.getOrNull() ?: res.exceptionOrNull()?.message.orEmpty(),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        enabled = !isSendingBot,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Send to Discord Webhook")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isSendingBot = true
                                                val res = integrationDispatch.sendToTelegram(testBotMessage, settings)
                                                isSendingBot = false
                                                Toast.makeText(
                                                    context,
                                                    res.getOrNull() ?: res.exceptionOrNull()?.message.orEmpty(),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        enabled = !isSendingBot,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Send to Telegram Bot")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
