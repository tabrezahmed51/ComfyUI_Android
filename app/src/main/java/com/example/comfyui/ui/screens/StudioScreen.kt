package com.example.comfyui.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.comfyui.data.model.GenerationJob
import com.example.comfyui.data.model.JobStatus
import com.example.comfyui.data.model.NodeCategory
import com.example.comfyui.ui.components.BatchQueueManager
import com.example.comfyui.ui.components.CanvasWireRenderer
import com.example.comfyui.ui.components.ComfyWorkflowManager
import com.example.comfyui.ui.components.N8nWorkflowBuilder
import com.example.comfyui.ui.components.NodeView
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: ComfyViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val workflow by viewModel.currentWorkflow.collectAsState()
    val selectedNodeId by viewModel.selectedNodeId.collectAsState()
    val scale by viewModel.canvasScale.collectAsState()
    val offsetX by viewModel.canvasOffsetX.collectAsState()
    val offsetY by viewModel.canvasOffsetY.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val history by viewModel.history.collectAsState()

    var selectedStudioTab by remember { mutableIntStateOf(0) } // 0: Visual Canvas, 1: Agent & n8n Flow
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showWorkflowManager by remember { mutableStateOf(false) }
    var showHistoryLog by remember { mutableStateOf(false) }
    var showBatchQueue by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("studio_screen_container")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Slim and Sleek Bar Menu with Buttons
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .height(48.dp)
                    .testTag("studio_sleek_menu_bar"),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, BorderDark),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Sleek Mode Switcher Pills (Canvas vs Agent Flow)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceVariantDark,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            // Canvas Pill
                            Surface(
                                onClick = { selectedStudioTab = 0 },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedStudioTab == 0) PrimaryNeon else Color.Transparent,
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("tab_visual_canvas")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = null,
                                        tint = if (selectedStudioTab == 0) Color(0xFF00354E) else Color.LightGray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Canvas",
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedStudioTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedStudioTab == 0) Color(0xFF00354E) else Color.LightGray
                                    )
                                }
                            }

                            // Agent Flow Pill
                            Surface(
                                onClick = { selectedStudioTab = 1 },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedStudioTab == 1) PrimaryNeon else Color.Transparent,
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("tab_agent_n8n_flow")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (selectedStudioTab == 1) Color(0xFF00354E) else Color.LightGray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Agent",
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedStudioTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedStudioTab == 1) Color(0xFF00354E) else Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    // Center: Minimal workflow info badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = workflow.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 100.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "• ${workflow.nodes.size}N",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = PrimaryNeon
                        )
                    }

                    // Right: Action Buttons Group (Add Node, Workflows, Batch, History, Settings)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Add Node Button
                        FilledTonalButton(
                            onClick = { showAddNodeDialog = true },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("add_node_button"),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SurfaceVariantDark,
                                contentColor = PrimaryNeon
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Node", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Workflows Button
                        IconButton(
                            onClick = { showWorkflowManager = true },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("open_workflow_manager_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = "Workflows",
                                tint = Color.LightGray,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Batch Queue Manager Button
                        IconButton(
                            onClick = { showBatchQueue = true },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("open_batch_queue_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (queue.isNotEmpty()) {
                                        Badge(containerColor = PrimaryNeon) {
                                            Text("${queue.size}", color = Color(0xFF00354E), fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = "Batch Queue",
                                    tint = if (queue.isNotEmpty()) PrimaryNeon else Color.LightGray,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Studio History Log Button
                        IconButton(
                            onClick = { showHistoryLog = true },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("studio_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Execution History",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("studio_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Server Settings",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedStudioTab == 0) {
                    // TAB 0: Freeform Interactive Graph Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("studio_canvas_container")
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

                        // Floating Pinch Zoom +/- and Reset Controls
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 14.dp, bottom = if (activeJob != null) 100.dp else 24.dp)
                                .testTag("canvas_floating_zoom_controls"),
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceDark.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, BorderDark),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Zoom In (+)
                                IconButton(
                                    onClick = { viewModel.zoomIn() },
                                    modifier = Modifier.size(30.dp).testTag("zoom_in_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                                }

                                // Scale Badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SurfaceVariantDark,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "${(scale * 100).toInt()}%",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                // Zoom Out (-)
                                IconButton(
                                    onClick = { viewModel.zoomOut() },
                                    modifier = Modifier.size(30.dp).testTag("zoom_out_button")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                                }

                                // Reset Focus
                                IconButton(
                                    onClick = { viewModel.resetCanvasTransform() },
                                    modifier = Modifier.size(30.dp).testTag("reset_zoom_button")
                                ) {
                                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Reset Zoom", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: n8n Node Web UI & Agent Copilot View
                    N8nWorkflowBuilder(
                        viewModel = viewModel,
                        onSwitchToCanvasAndFocus = { nodeId ->
                            viewModel.centerCanvasOnNode(nodeId)
                            selectedStudioTab = 0
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Floating Action Queue Prompt Button (Shared across both tabs)
        FloatingActionButton(
            onClick = { viewModel.queuePrompt() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = if (activeJob != null) 100.dp else 24.dp)
                .testTag("queue_prompt_button"),
            containerColor = PrimaryNeon,
            contentColor = Color(0xFF00354E)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Queue Prompt",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (queue.isNotEmpty()) "Queue (${queue.size + 1})" else "Queue Prompt",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Active Execution Progress Card at bottom (Shared across both tabs)
        AnimatedVisibility(
            visible = activeJob != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp)
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
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryNeon
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = job.currentNodeTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                            }

                            IconButton(
                                onClick = { viewModel.cancelActiveJob() },
                                modifier = Modifier.size(26.dp).testTag("cancel_active_job")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Job",
                                    tint = AccentRose,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { job.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp),
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

    // -------------------------------------------------------------
    // STUDIO EXECUTION HISTORY MODAL WITH ONE-CLICK RE-RUN
    // -------------------------------------------------------------
    if (showHistoryLog) {
        Dialog(
            onDismissRequest = { showHistoryLog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .testTag("studio_history_dialog"),
                shape = RoundedCornerShape(20.dp),
                color = BackgroundDark,
                border = BorderStroke(1.dp, BorderDark),
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Studio History Log",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "${history.size} Executed runs • Tap Re-run to re-execute with one click",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(
                            onClick = { showHistoryLog = false },
                            modifier = Modifier.testTag("close_studio_history")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No executed workflows recorded yet", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(history, key = { it.id }) { job ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceDark,
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Header Row: Timestamp & Status
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(job.createdAt)),
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (job.status == JobStatus.COMPLETED) AccentEmerald.copy(alpha = 0.2f) else AccentRose.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, if (job.status == JobStatus.COMPLETED) AccentEmerald.copy(alpha = 0.6f) else AccentRose.copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = job.status.name,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (job.status == JobStatus.COMPLETED) AccentEmerald else AccentRose,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = job.workflowName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${job.promptText}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Parameters Capsules
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantDark) {
                                                Text("Seed: ${job.seed}", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                            Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantDark) {
                                                Text("Steps: ${job.steps}", fontSize = 9.sp, color = PrimaryNeon, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                            Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantDark) {
                                                Text("CFG: ${job.cfg}", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                            Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariantDark) {
                                                Text("${job.width}x${job.height}", fontSize = 9.sp, color = SecondaryOrange, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = BorderDark)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Actions: One-Click Re-run and Load to Canvas
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.restoreFromHistory(job)
                                                    showHistoryLog = false
                                                    Toast.makeText(context, "Loaded into Studio", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Inspect on Canvas", fontSize = 11.sp, color = Color.LightGray)
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Button(
                                                onClick = {
                                                    viewModel.reRunJob(job)
                                                    showHistoryLog = false
                                                    Toast.makeText(context, "Re-running: ${job.workflowName}", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PrimaryNeon,
                                                    contentColor = Color(0xFF00354E)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                modifier = Modifier.height(30.dp).testTag("rerun_task_${job.id}")
                                            ) {
                                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Re-run Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    if (showWorkflowManager) {
        ComfyWorkflowManager(
            viewModel = viewModel,
            onDismiss = { showWorkflowManager = false }
        )
    }

    if (showBatchQueue) {
        BatchQueueManager(
            viewModel = viewModel,
            onDismiss = { showBatchQueue = false }
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
