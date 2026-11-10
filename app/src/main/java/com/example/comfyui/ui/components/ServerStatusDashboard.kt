package com.example.comfyui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import com.example.comfyui.ui.viewmodel.ServerHealth
import com.example.comfyui.ui.viewmodel.ServerMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ServerStatusDashboard(
    viewModel: ComfyViewModel,
    modifier: Modifier = Modifier
) {
    val serverMode by viewModel.serverMode.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val serverApiKey by viewModel.serverApiKey.collectAsState()
    val serverHealth by viewModel.serverHealth.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "spin"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when {
                    serverMode == ServerMode.MOBILE_EMULATION -> AccentViolet.copy(alpha = 0.6f)
                    serverHealth.isOnline -> AccentEmerald
                    serverHealth.isChecking -> SecondaryOrange
                    else -> BorderDark
                },
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("server_status_dashboard"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Status indicator and refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing LED Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    serverMode == ServerMode.MOBILE_EMULATION -> AccentViolet
                                    serverHealth.isChecking -> SecondaryOrange.copy(alpha = pulseAlpha)
                                    serverHealth.isOnline -> AccentEmerald.copy(alpha = pulseAlpha)
                                    else -> AccentRose
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    serverMode == ServerMode.MOBILE_EMULATION -> "Standalone Mobile Engine"
                                    serverHealth.isChecking -> "Checking Connectivity..."
                                    serverHealth.isOnline -> "COMFYUI SERVER ONLINE"
                                    else -> "SERVER OFFLINE"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = when {
                                    serverMode == ServerMode.MOBILE_EMULATION -> AccentViolet
                                    serverHealth.isChecking -> SecondaryOrange
                                    serverHealth.isOnline -> AccentEmerald
                                    else -> AccentRose
                                }
                            )

                            if (serverHealth.latencyMs != null && serverMode == ServerMode.REMOTE_COMFY_SERVER) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SurfaceVariantDark
                                ) {
                                    Text(
                                        text = "${serverHealth.latencyMs}ms",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryNeon
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (serverMode == ServerMode.MOBILE_EMULATION) {
                                "Internal procedural neural synthesis active"
                            } else {
                                serverUrl
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Refresh Button
                IconButton(
                    onClick = { viewModel.checkServerHealth() },
                    enabled = !serverHealth.isChecking,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("refresh_server_status_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Connectivity",
                        tint = if (serverHealth.isChecking) SecondaryOrange else PrimaryNeon,
                        modifier = Modifier
                            .size(20.dp)
                            .then(
                                if (serverHealth.isChecking) Modifier.rotate(spinRotation) else Modifier
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RESPONSE STATUS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.Gray
                    )
                    Text(
                        text = serverHealth.statusMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = if (serverHealth.isOnline) AccentEmerald else Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LAST CHECKED",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.Gray
                    )
                    Text(
                        text = if (serverHealth.lastCheckedTime > 0) {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(serverHealth.lastCheckedTime))
                        } else {
                            "Never"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    )
                }
            }

            if (serverHealth.deviceName != null || serverHealth.vramStats != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = serverHealth.deviceName ?: "Hardware",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = PrimaryNeon)
                        )
                        if (serverHealth.vramStats != null) {
                            Text(
                                text = "VRAM: ${serverHealth.vramStats}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = SecondaryOrange)
                            )
                        }
                    }
                }
            }

            // Quick Preset Selection Chips
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        viewModel.setServerMode(ServerMode.REMOTE_COMFY_SERVER)
                        viewModel.setServerUrl("http://127.0.0.1:8188")
                        viewModel.checkServerHealth()
                    },
                    label = { Text("Termux Local", fontSize = 10.sp) },
                    modifier = Modifier.height(28.dp).testTag("preset_termux")
                )
                SuggestionChip(
                    onClick = {
                        viewModel.setServerMode(ServerMode.REMOTE_COMFY_SERVER)
                        viewModel.setServerUrl("https://cloud.comfy.org")
                        viewModel.checkServerHealth()
                    },
                    label = { Text("Comfy Cloud", fontSize = 10.sp) },
                    modifier = Modifier.height(28.dp).testTag("preset_cloud")
                )
                SuggestionChip(
                    onClick = {
                        viewModel.setServerMode(ServerMode.MOBILE_EMULATION)
                    },
                    label = { Text("Mobile Standalone", fontSize = 10.sp) },
                    modifier = Modifier.height(28.dp).testTag("preset_standalone")
                )
            }
        }
    }
}
