package com.example.comfyui.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.comfyui.data.model.GenerationJob
import com.example.comfyui.data.model.JobStatus
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import com.example.comfyui.ui.viewmodel.LocalWorkflowFile
import java.io.File

/**
 * BatchQueueManager
 *
 * Allows users to queue multiple workflow_api.json files for sequential processing
 * and monitor the live progress of each job in the queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchQueueManager(
    viewModel: ComfyViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localFiles by viewModel.localWorkflowFiles.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()

    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Batch Queue Monitor, 1: Add Workflows

    LaunchedEffect(Unit) {
        viewModel.loadLocalWorkflowFiles()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("batch_queue_manager_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = BackgroundDark,
            border = BorderStroke(1.dp, BorderDark),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PrimaryNeon.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Batch Queue Manager",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Sequential workflow processing pipeline",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_batch_queue_manager")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher between Active Queue and Add Workflows
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceVariantDark,
                    contentColor = PrimaryNeon,
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                ) {
                    val totalJobs = (if (activeJob != null) 1 else 0) + queue.size
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Live Queue ($totalJobs)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        modifier = Modifier.testTag("tab_live_batch_queue")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Add Workflows (${localFiles.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        modifier = Modifier.testTag("tab_add_batch_workflows")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // -------------------------------------------------------------
                // TAB 0: LIVE BATCH QUEUE MONITOR
                // -------------------------------------------------------------
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Active Execution Card
                        if (activeJob != null) {
                            activeJob?.let { job ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceDark,
                                    border = BorderStroke(1.dp, PrimaryNeon),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
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
                                                    text = "PROCESSING NOW",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryNeon
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.cancelActiveJob() },
                                                modifier = Modifier.size(24.dp).testTag("cancel_batch_active_job")
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = AccentRose, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(job.workflowName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                        Text(job.currentNodeTitle, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { job.progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = PrimaryNeon,
                                            trackColor = SurfaceVariantDark
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Step ${job.currentStep}/${job.totalSteps}", fontSize = 10.sp, color = Color.Gray)
                                            Text("${(job.progress * 100).toInt()}%", fontSize = 10.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Queued Jobs List Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Upcoming Jobs (${queue.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )

                            if (queue.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearQueue() },
                                    modifier = Modifier.testTag("clear_batch_queue_button")
                                ) {
                                    Icon(Icons.Default.ClearAll, contentDescription = null, tint = AccentRose, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All", color = AccentRose, fontSize = 11.sp)
                                }
                            }
                        }

                        if (queue.isEmpty() && activeJob == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Batch Queue is empty", fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Text("Switch to 'Add Workflows' tab to queue files", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(queue, key = { _, job -> job.id }) { index, job ->
                                    QueuedJobCard(
                                        jobIndex = index + 1,
                                        job = job,
                                        onRemove = { viewModel.removeFromQueue(job.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // TAB 1: ADD LOCAL WORKFLOW FILES TO BATCH
                // -------------------------------------------------------------
                if (selectedTab == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Select workflow_api.json files:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                            )

                            Row {
                                TextButton(
                                    onClick = {
                                        selectedFiles = localFiles.map { it.file.absolutePath }.toSet()
                                    }
                                ) {
                                    Text("Select All", fontSize = 11.sp, color = PrimaryNeon)
                                }
                                TextButton(
                                    onClick = { selectedFiles = emptySet() }
                                ) {
                                    Text("Clear", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (localFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No local workflow files in storage", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(localFiles, key = { it.file.absolutePath }) { fileItem ->
                                    val isSelected = selectedFiles.contains(fileItem.file.absolutePath)

                                    Surface(
                                        onClick = {
                                            selectedFiles = if (isSelected) {
                                                selectedFiles - fileItem.file.absolutePath
                                            } else {
                                                selectedFiles + fileItem.file.absolutePath
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) SurfaceVariantDark else SurfaceDark,
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryNeon else BorderDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = {
                                                        selectedFiles = if (it) {
                                                            selectedFiles + fileItem.file.absolutePath
                                                        } else {
                                                            selectedFiles - fileItem.file.absolutePath
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = PrimaryNeon,
                                                        checkmarkColor = Color(0xFF00354E)
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        text = fileItem.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "${fileItem.nodeCount} nodes • ${fileItem.sizeBytes} B",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SurfaceVariantDark
                                            ) {
                                                Text(
                                                    text = "JSON",
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = PrimaryNeon,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Queue Action Button
                        Button(
                            onClick = {
                                val filesToQueue = localFiles.filter { it.file.absolutePath in selectedFiles }.map { it.file }
                                if (filesToQueue.isNotEmpty()) {
                                    viewModel.queueWorkflowFiles(filesToQueue)
                                    Toast.makeText(context, "Queued ${filesToQueue.size} workflows for processing", Toast.LENGTH_SHORT).show()
                                    selectedFiles = emptySet()
                                    selectedTab = 0
                                } else {
                                    Toast.makeText(context, "Please select at least one workflow file", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = selectedFiles.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("enqueue_batch_workflows_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryNeon,
                                contentColor = Color(0xFF00354E)
                            )
                        ) {
                            Icon(Icons.Default.QueuePlayNext, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Queue ${selectedFiles.size} Workflows into Batch",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueuedJobCard(
    jobIndex: Int,
    job: GenerationJob,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$jobIndex",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = job.workflowName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = job.promptText,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp).testTag("remove_queued_${job.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from Queue",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
