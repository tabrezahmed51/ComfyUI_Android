package com.example.comfyui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comfyui.data.model.Node
import com.example.comfyui.data.model.NodeSocket
import com.example.comfyui.ui.theme.*
import kotlin.random.Random

@Composable
fun NodeView(
    node: Node,
    isSelected: Boolean,
    isCurrentExecuting: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onDelete: () -> Unit,
    onToggleBypass: () -> Unit,
    onUpdateWidget: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isCurrentExecuting -> PrimaryNeon
        isSelected -> AccentViolet
        node.isBypassed -> Color(0xFF64748B)
        else -> BorderDark
    }

    val borderWidth = if (isCurrentExecuting || isSelected) 2.dp else 1.dp

    Card(
        modifier = modifier
            .width(260.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .pointerInput(node.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            }
            .testTag("node_${node.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (node.isBypassed) SurfaceDark.copy(alpha = 0.6f) else SurfaceDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(node.category.headerColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "#${node.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBypass,
                        modifier = Modifier.size(24.dp).testTag("bypass_node_${node.id}")
                    ) {
                        Icon(
                            imageVector = if (node.isBypassed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Bypass node",
                            tint = if (node.isBypassed) Color.White.copy(alpha = 0.5f) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_node_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete node",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Sockets Row
            if (node.inputs.isNotEmpty() || node.outputs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Inputs Column (Left)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        node.inputs.forEach { input ->
                            SocketItem(socket = input, isInput = true)
                        }
                    }

                    // Outputs Column (Right)
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        node.outputs.forEach { output ->
                            SocketItem(socket = output, isInput = false)
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Widgets Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (node.type) {
                    "CLIPTextEncode" -> {
                        val currentText = node.widgetValues["text"] ?: ""
                        OutlinedTextField(
                            value = currentText,
                            onValueChange = { onUpdateWidget("text", it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 68.dp, max = 120.dp)
                                .testTag("prompt_input_${node.id}"),
                            placeholder = { Text("Enter prompt description...", fontSize = 11.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeon,
                                unfocusedBorderColor = BorderDark,
                                focusedContainerColor = SurfaceVariantDark,
                                unfocusedContainerColor = SurfaceVariantDark
                            )
                        )

                        // Quick style modifiers chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Cyberpunk", "Cinematic", "8K").forEach { tag ->
                                SuggestionChip(
                                    onClick = {
                                        val newText = if (currentText.isBlank()) tag else "$currentText, $tag"
                                        onUpdateWidget("text", newText)
                                    },
                                    label = { Text(tag, fontSize = 9.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }

                    "KSampler" -> {
                        val steps = node.widgetValues["steps"]?.toIntOrNull() ?: 20
                        val cfg = node.widgetValues["cfg"]?.toFloatOrNull() ?: 7.0f
                        val seed = node.widgetValues["seed"] ?: "4820194827"
                        val sampler = node.widgetValues["sampler_name"] ?: "euler"

                        // Steps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Steps: $steps", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = steps.toFloat(),
                                onValueChange = { onUpdateWidget("steps", it.toInt().toString()) },
                                valueRange = 4f..50f,
                                steps = 46,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                                    .height(24.dp)
                            )
                        }

                        // CFG Scale
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CFG: ${String.format("%.1f", cfg)}", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = cfg,
                                onValueChange = { onUpdateWidget("cfg", String.format("%.1f", it)) },
                                valueRange = 1f..15f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                                    .height(24.dp)
                            )
                        }

                        // Seed & Randomize
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Seed: $seed",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = {
                                    val randomSeed = Random.nextLong(100000000, 9999999999).toString()
                                    onUpdateWidget("seed", randomSeed)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("🎲 Rand", fontSize = 10.sp)
                            }
                        }

                        // Sampler Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("euler", "dpmpp_2m", "uni_pc").forEach { sName ->
                                FilterChip(
                                    selected = sampler == sName,
                                    onClick = { onUpdateWidget("sampler_name", sName) },
                                    label = { Text(sName, fontSize = 9.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }

                    "EmptyLatentImage" -> {
                        val width = node.widgetValues["width"] ?: "512"
                        val height = node.widgetValues["height"] ?: "512"

                        Text("Resolution: ${width}x${height}", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("512x512", "768x768", "1024x1024").forEach { res ->
                                val (w, h) = res.split("x")
                                FilterChip(
                                    selected = width == w && height == h,
                                    onClick = {
                                        onUpdateWidget("width", w)
                                        onUpdateWidget("height", h)
                                    },
                                    label = { Text(res, fontSize = 9.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }

                    "CheckpointLoaderSimple" -> {
                        val ckpt = node.widgetValues["ckpt_name"] ?: "v1-5-pruned-emaonly.safetensors"
                        Column {
                            Text("Model Checkpoint:", style = MaterialTheme.typography.labelSmall)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SurfaceVariantDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    text = ckpt,
                                    style = MaterialTheme.typography.labelSmall.copy(color = PrimaryNeon),
                                    modifier = Modifier.padding(6.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    else -> {
                        // Generic widget summary
                        node.widgetValues.forEach { (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$k:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(v, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocketItem(socket: NodeSocket, isInput: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isInput) Arrangement.Start else Arrangement.End,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        if (isInput) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(socket.type.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = socket.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color.LightGray,
                maxLines = 1
            )
        } else {
            Text(
                text = socket.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color.LightGray,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(socket.type.color, CircleShape)
            )
        }
    }
}
