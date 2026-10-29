package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProvider
import com.example.data.model.AppSettings
import com.example.data.remote.TermuxStatus
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FuturisticTopBar(
    settings: AppSettings,
    termuxStatus: TermuxStatus,
    onProviderSelected: (AIProvider) -> Unit,
    onOpenTermuxCenter: () -> Unit,
    onOpenArtStudio: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleAmbientAudio: () -> Unit,
    onNewChat: () -> Unit,
    onToggleDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var providerDropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GlassBackground)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Drawer menu & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "History", tint = NeonCyan)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ZENITH",
                                color = NeonCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = " AI",
                                color = NeonPink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Active provider badge button
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(settings.activeProvider.badgeColorHex).copy(alpha = 0.2f))
                                    .clickable { providerDropdownExpanded = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(settings.activeProvider.badgeColorHex))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = settings.activeProvider.displayName,
                                    color = Color(settings.activeProvider.badgeColorHex),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            DropdownMenu(
                                expanded = providerDropdownExpanded,
                                onDismissRequest = { providerDropdownExpanded = false }
                            ) {
                                AIProvider.entries.forEach { provider ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(provider.badgeColorHex))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(provider.displayName, color = Color.White)
                                            }
                                        },
                                        onClick = {
                                            onProviderSelected(provider)
                                            providerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Termux Status, Tor indicator, Audio toggle, Art, Settings, New Chat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Termux status chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (termuxStatus.isOnline) Color(0x3300E676) else Color(0x22FFFFFF))
                            .border(
                                1.dp,
                                if (termuxStatus.isOnline) NeonGreen else DarkSurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onOpenTermuxCenter() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (termuxStatus.isOnline) NeonGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "Termux",
                            tint = if (termuxStatus.isOnline) NeonGreen else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Tor indicator
                    if (settings.isTorEnabled) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Tor Active",
                            tint = NeonViolet,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(2.dp)
                        )
                    }

                    // Ambient Audio button
                    IconButton(onClick = onToggleAmbientAudio, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = if (settings.ambientAudioEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = "Toggle Ambient Music",
                            tint = if (settings.ambientAudioEnabled) NeonPink else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pollinations Art Studio button
                    IconButton(onClick = onOpenArtStudio, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Art Studio",
                            tint = NeonPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings (.env) button
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // New Chat
                    IconButton(onClick = onNewChat, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
