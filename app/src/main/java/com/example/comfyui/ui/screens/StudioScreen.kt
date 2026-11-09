package com.example.comfyui.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comfyui.data.model.NodeCategory
import com.example.comfyui.ui.components.CanvasWireRenderer
import com.example.comfyui.ui.components.NodeView
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: ComfyViewModel,
    modifier: Modifier = Modifier
) {
    val workflow by viewModel.currentWorkflow.collectAsState()
    val selectedNodeId by viewModel.selectedNodeId.collectAsState()
    val scale by viewModel.canvasScale.collectAsState()
    val offsetX by viewModel.canvasOffsetX.collectAsState()
    val offsetY by viewModel.canvasOffsetY.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val queue by viewModel.queue.collectAsState()

    var showAddNodeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("studio_canvas_container")
    ) {
        // Pannable and zoomable background grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        viewModel.updateCanvasTransform(zoom, pan.x, pan.y)
                    }
                }
        ) {
            // Subtle Dot Grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawDotGrid(scale, offsetX, offsetY)
            }

            // Connection Wires Layer
            CanvasWireRenderer(
                nodes = workflow.nodes,
                links = workflow.links,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                isExecuting = activeJob != null
            )

            // Nodes Layer
            workflow.nodes.forEach { node ->
                val nodeX = (node.positionX * scale + offsetX).roundToInt()
                val nodeY = (node.positionY * scale + offsetY).roundToInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(nodeX, nodeY) }
                ) {
                    NodeView(
                        node = node,
                        isSelected = selectedNodeId == node.id,
                        isCurrentExecuting = activeJob?.currentNodeTitle?.contains(node.title, ignoreCase = true) == true,
                        onSelect = { viewModel.selectNode(node.id) },
                        onMove = { dx, dy -> viewModel.moveNode(node.id, dx / scale, dy / scale) },
                        onDelete = { viewModel.deleteNode(node.id) },
                        onToggleBypass = { viewModel.toggleBypassNode(node.id) },
                        onUpdateWidget = { k, v -> viewModel.updateWidgetValue(node.id, k, v) }
                    )
                }
            }
        }

        // Top Status Bar Overlay
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDark.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, BorderDark),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = workflow.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                        maxLines = 1
                    )
                    Text(
                        text = "${workflow.nodes.size} Nodes • ${workflow.links.size} Connections • Scale: ${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.resetCanvasTransform() },
                        modifier = Modifier.size(36.dp).testTag("reset_zoom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = "Reset Zoom",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = { showAddNodeDialog = true },
                        modifier = Modifier.height(36.dp).testTag("add_node_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Node", fontSize = 12.sp)
                    }
                }
            }
        }

        // Floating Action Queue Prompt Button
        FloatingActionButton(
            onClick = { viewModel.queuePrompt() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = if (activeJob != null) 100.dp else 24.dp)
                .testTag("queue_prompt_button"),
            containerColor = PrimaryNeon,
            contentColor = Color(0xFF00354E)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Queue Prompt",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (queue.isNotEmpty()) "Queue (${queue.size + 1})" else "Queue Prompt",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Active Execution Progress Card at bottom
        AnimatedVisibility(
            visible = activeJob != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            activeJob?.let { job ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, PrimaryNeon),
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.5.dp,
                                    color = PrimaryNeon
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = job.currentNodeTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                            }

                            IconButton(
                                onClick = { viewModel.cancelActiveJob() },
                                modifier = Modifier.size(28.dp).testTag("cancel_active_job")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Job",
                                    tint = AccentRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { job.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = PrimaryNeon,
                            trackColor = SurfaceVariantDark
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sampling ${job.currentStep}/${job.totalSteps} steps",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "${(job.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryNeon
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Node Dialog
    if (showAddNodeDialog) {
        AlertDialog(
            onDismissRequest = { showAddNodeDialog = false },
            title = { Text("Add ComfyUI Node") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("CLIPTextEncode", "CLIP Text Encode (Prompt)", NodeCategory.CONDITIONING),
                        Triple("KSampler", "KSampler (Diffusion Engine)", NodeCategory.SAMPLER),
                        Triple("EmptyLatentImage", "Empty Latent Image", NodeCategory.LATENT),
                        Triple("VAEDecode", "VAE Decode (Latent to Image)", NodeCategory.LATENT),
                        Triple("SaveImage", "Save Image", NodeCategory.IMAGE),
                        Triple("LoraLoader", "Load LoRA", NodeCategory.LOADER)
                    ).forEach { (type, name, cat) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.addNode(type, name, cat)
                                showAddNodeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceVariantDark
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text(
                                    cat.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cat.headerColor
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddNodeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun DrawScope.drawDotGrid(scale: Float, offsetX: Float, offsetY: Float) {
    val gridSize = 32f * scale
    if (gridSize < 10f) return

    val startX = (offsetX % gridSize)
    val startY = (offsetY % gridSize)

    var x = startX
    while (x < size.width) {
        var y = startY
        while (y < size.height) {
            drawCircle(
                color = Color(0xFF1E293B),
                radius = 1.2f,
                center = Offset(x, y)
            )
            y += gridSize
        }
        x += gridSize
    }
}
