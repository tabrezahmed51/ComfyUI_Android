package com.example.comfyui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.comfyui.data.model.Node
import com.example.comfyui.data.model.NodeSocket
import com.example.comfyui.ui.theme.*
import kotlin.random.Random

/**
 * Mobile-First 9:16 Optimized ComfyUI Node View
 *
 * Streamlined small card nodes (195dp width) tailored for vertical mobile handheld use:
 * - Ultra-compact layout designed for 9:16 touch displays.
 * - 'i' Info Bubble popover displaying rich node technical metadata without cluttering the canvas.
 * - Tiny dropdowns and stepper chips for parameters.
 * - Interactive Vertical Portrait (9:16) Card Preview Popup with watermark capsule overlay.
 * - Clear color-coded input/output sockets.
 */
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
    var showInfoBubble by remember { mutableStateOf(false) }
    var showPortraitPreview by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val borderColor = when {
        isCurrentExecuting -> PrimaryNeon
        isSelected -> AccentViolet
        node.isBypassed -> Color(0xFF475569)
        else -> BorderDark
    }

    val borderWidth = if (isCurrentExecuting || isSelected) 2.dp else 1.dp

    Card(
        modifier = modifier
            .width(195.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .pointerInput(node.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            }
            .testTag("node_${node.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (node.isBypassed) SurfaceDark.copy(alpha = 0.65f) else SurfaceDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 3.dp)
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(node.category.headerColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "#${node.id}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 'i' Info Bubble Icon
                    IconButton(
                        onClick = { showInfoBubble = true },
                        modifier = Modifier.size(20.dp).testTag("info_bubble_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Node Info",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Bypass Node Toggle
                    IconButton(
                        onClick = onToggleBypass,
                        modifier = Modifier.size(20.dp).testTag("bypass_node_${node.id}")
                    ) {
                        Icon(
                            imageVector = if (node.isBypassed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Bypass node",
                            tint = if (node.isBypassed) Color.White.copy(alpha = 0.4f) else Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Delete Node
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(20.dp).testTag("delete_node_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete node",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Sockets Row
            if (node.inputs.isNotEmpty() || node.outputs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Inputs Column (Left)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        node.inputs.forEach { input ->
                            SocketItem(socket = input, isInput = true)
                        }
                    }

                    // Outputs Column (Right)
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        node.outputs.forEach { output ->
                            SocketItem(socket = output, isInput = false)
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderDark.copy(alpha = 0.4f), thickness = 0.6.dp)

            // Widgets Area (Mobile-Optimized)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (node.type) {
                    "CLIPTextEncode" -> {
                        val currentText = node.widgetValues["text"] ?: ""
                        OutlinedTextField(
                            value = currentText,
                            onValueChange = { onUpdateWidget("text", it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp, max = 85.dp)
                                .testTag("prompt_input_${node.id}"),
                            placeholder = { Text("Prompt description...", fontSize = 10.sp, color = Color.Gray) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeon,
                                unfocusedBorderColor = BorderDark,
                                focusedContainerColor = SurfaceVariantDark,
                                unfocusedContainerColor = SurfaceVariantDark,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    "KSampler" -> {
                        val steps = node.widgetValues["steps"]?.toIntOrNull() ?: 20
                        val cfg = node.widgetValues["cfg"]?.toFloatOrNull() ?: 7.0f
                        val seed = node.widgetValues["seed"] ?: "4820194827"
                        val sampler = node.widgetValues["sampler_name"] ?: "euler"

                        // Stepper row for Steps and CFG
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Steps stepper
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Steps:", fontSize = 9.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("$steps", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(3.dp))
                                Surface(
                                    onClick = { if (steps > 1) onUpdateWidget("steps", (steps - 1).toString()) },
                                    shape = CircleShape,
                                    color = SurfaceVariantDark,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.LightGray)
                                    }
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                Surface(
                                    onClick = { onUpdateWidget("steps", (steps + 1).toString()) },
                                    shape = CircleShape,
                                    color = SurfaceVariantDark,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.LightGray)
                                    }
                                }
                            }

                            // CFG
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("CFG:", fontSize = 9.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(String.format("%.1f", cfg), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(3.dp))
                                Surface(
                                    onClick = {
                                        val newCfg = (cfg - 0.5f).coerceAtLeast(1.0f)
                                        onUpdateWidget("cfg", String.format("%.1f", newCfg))
                                    },
                                    shape = CircleShape,
                                    color = SurfaceVariantDark,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.LightGray)
                                    }
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                Surface(
                                    onClick = {
                                        val newCfg = (cfg + 0.5f).coerceAtMost(20.0f)
                                        onUpdateWidget("cfg", String.format("%.1f", newCfg))
                                    },
                                    shape = CircleShape,
                                    color = SurfaceVariantDark,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.LightGray)
                                    }
                                }
                            }
                        }

                        // Seed & Rand Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Seed: $seed",
                                fontSize = 9.sp,
                                color = Color.LightGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                onClick = {
                                    val randomSeed = Random.nextLong(100000000, 9999999999).toString()
                                    onUpdateWidget("seed", randomSeed)
                                },
                                shape = RoundedCornerShape(4.dp),
                                color = SurfaceVariantDark,
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.height(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(10.dp), tint = PrimaryNeon)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Rand", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }

                        // Tiny Sampler Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            listOf("euler", "dpmpp_2m", "ddim").forEach { sName ->
                                val isChosen = sampler.equals(sName, ignoreCase = true)
                                Surface(
                                    onClick = { onUpdateWidget("sampler_name", sName) },
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isChosen) PrimaryNeon else SurfaceVariantDark,
                                    border = BorderStroke(1.dp, if (isChosen) PrimaryNeon else BorderDark),
                                    modifier = Modifier.height(18.dp)
                                ) {
                                    Text(
                                        text = sName,
                                        fontSize = 8.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) Color(0xFF00354E) else Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    "EmptyLatentImage" -> {
                        val width = node.widgetValues["width"] ?: "512"
                        val height = node.widgetValues["height"] ?: "512"

                        Text("Size: ${width}x${height}", fontSize = 9.sp, color = Color.LightGray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            listOf(
                                "512²" to ("512" to "512"),
                                "768²" to ("768" to "768"),
                                "9:16" to ("576" to "1024") // 9:16 vertical portrait ratio
                            ).forEach { (label, pair) ->
                                val isSelectedSize = width == pair.first && height == pair.second
                                Surface(
                                    onClick = {
                                        onUpdateWidget("width", pair.first)
                                        onUpdateWidget("height", pair.second)
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelectedSize) PrimaryNeon else SurfaceVariantDark,
                                    border = BorderStroke(1.dp, if (isSelectedSize) PrimaryNeon else BorderDark),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontSize = 8.sp,
                                            fontWeight = if (isSelectedSize) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelectedSize) Color(0xFF00354E) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "SaveImage", "PreviewImage", "VAEDecode" -> {
                        // Portrait 9:16 preview card affordance
                        Surface(
                            onClick = { showPortraitPreview = true },
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceVariantDark,
                            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("node_${node.id}_portrait_preview_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PrimaryNeon.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AspectRatio,
                                            contentDescription = null,
                                            tint = PrimaryNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("9:16 Portrait Preview", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Tap to open full card", fontSize = 8.sp, color = Color.Gray)
                                    }
                                }
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    "CheckpointLoaderSimple" -> {
                        val ckpt = node.widgetValues["ckpt_name"] ?: "v1-5-pruned-emaonly.safetensors"
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SurfaceVariantDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = ckpt,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    else -> {
                        node.widgetValues.entries.take(2).forEach { (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$k:", fontSize = 8.sp, color = Color.Gray)
                                Text(v, fontSize = 8.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // 'i' INFO BUBBLE MODAL
    // -------------------------------------------------------------
    if (showInfoBubble) {
        Dialog(
            onDismissRequest = { showInfoBubble = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showInfoBubble = false }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.7f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clickable(enabled = false) {}
                        .testTag("node_${node.id}_info_bubble")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(node.category.headerColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = node.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                            IconButton(
                                onClick = { showInfoBubble = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Node Type: ${node.type} • ID #${node.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryNeon
                        )
                        Text(
                            text = "Category: ${node.category.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderDark)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Full Parameter Breakdown
                        Text("Active Parameters:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (node.widgetValues.isEmpty()) {
                            Text("No parameters configured.", fontSize = 10.sp, color = Color.Gray)
                        } else {
                            node.widgetValues.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(k, fontSize = 10.sp, color = Color.LightGray)
                                    Text(v, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sockets Architecture:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Inputs: " + (if (node.inputs.isEmpty()) "None" else node.inputs.joinToString { it.name }), fontSize = 9.sp, color = Color.Gray)
                        Text("Outputs: " + (if (node.outputs.isEmpty()) "None" else node.outputs.joinToString { it.name }), fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // VERTICAL PORTRAIT (9:16) PREVIEW CARD POPUP WITH WATERMARK
    // -------------------------------------------------------------
    if (showPortraitPreview) {
        Dialog(
            onDismissRequest = { showPortraitPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showPortraitPreview = false }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // 9:16 Aspect Ratio Portrait Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.8f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .testTag("vertical_portrait_preview_card")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background placeholder / generative image
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=720&q=80",
                            contentDescription = "9:16 Portrait Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Top Sleek Watermark Capsule Menu
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(PrimaryNeon, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ComfyUI • 9:16 Portrait Canvas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Close Button Top Right
                        IconButton(
                            onClick = { showPortraitPreview = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Bottom Watermark Stats Capsule Overlay
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = node.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantDark) {
                                        Text("Ratio 9:16", fontSize = 9.sp, color = PrimaryNeon, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantDark) {
                                        Text("576x1024 px", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantDark) {
                                        Text("Node #${node.id}", fontSize = 9.sp, color = SecondaryOrange, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
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
                    .size(6.dp)
                    .background(socket.type.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = socket.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.LightGray,
                maxLines = 1
            )
        } else {
            Text(
                text = socket.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.LightGray,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(socket.type.color, CircleShape)
            )
        }
    }
}
