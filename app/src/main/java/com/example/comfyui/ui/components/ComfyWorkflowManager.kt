package com.example.comfyui.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.comfyui.data.model.Workflow
import com.example.comfyui.data.model.WorkflowSchemaValidator
import com.example.comfyui.data.model.WorkflowValidationResult
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import com.example.comfyui.ui.viewmodel.LocalWorkflowFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyWorkflowManager(
    viewModel: ComfyViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onWorkflowLoaded: (Workflow) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentWorkflow by viewModel.currentWorkflow.collectAsState()
    val localFiles by viewModel.localWorkflowFiles.collectAsState()

    var fileNameInput by remember { mutableStateOf("workflow_api.json") }
    var fileToDelete by remember { mutableStateOf<LocalWorkflowFile?>(null) }
    var fileToInspect by remember { mutableStateOf<LocalWorkflowFile?>(null) }
    var inspectedContent by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var validationWarningResult by remember { mutableStateOf<Pair<LocalWorkflowFile, WorkflowValidationResult>?>(null) }

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
                .testTag("comfy_workflow_manager_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = BackgroundDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Comfy Workflow Manager",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Text(
                            text = "Save, list & select local workflow_api.json files in app storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_workflow_manager")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderDark, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Save Current Canvas Workflow
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SAVE CANVAS TO LOCAL STORAGE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = PrimaryNeon
                                )
                            )

                            Text(
                                text = "${currentWorkflow.nodes.size} nodes • ${currentWorkflow.links.size} wires",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = fileNameInput,
                            onValueChange = { fileNameInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("workflow_filename_input"),
                            label = { Text("Workflow File Name") },
                            placeholder = { Text("workflow_api.json") },
                            trailingIcon = {
                                Text(
                                    text = ".json",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick preset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = { fileNameInput = "workflow_api.json" },
                                label = { Text("workflow_api.json", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp).testTag("preset_default_api")
                            )

                            SuggestionChip(
                                onClick = {
                                    val safeName = currentWorkflow.name.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
                                    fileNameInput = "${safeName}_api.json"
                                },
                                label = { Text("Name Based", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp).testTag("preset_name_api")
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    val success = viewModel.saveWorkflowToFile(fileNameInput, currentWorkflow)
                                    if (success) {
                                        statusMessage = "Saved '$fileNameInput' successfully to app storage"
                                        Toast.makeText(context, "Saved $fileNameInput to local storage", Toast.LENGTH_SHORT).show()
                                    } else {
                                        statusMessage = "Failed to save workflow file"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                modifier = Modifier.testTag("save_workflow_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = Color(0xFF00354E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save JSON", color = Color(0xFF00354E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (statusMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: List Local Workflow Files
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Local Workflow Files (${localFiles.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SurfaceVariantDark
                        ) {
                            Text(
                                text = "files/workflows/",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SecondaryOrange
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.loadLocalWorkflowFiles() },
                        modifier = Modifier.size(32.dp).testTag("refresh_workflow_files")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (localFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(SurfaceDark, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No local workflow files found in app storage", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(localFiles, key = { it.file.absolutePath }) { wfFile ->
                            WorkflowFileItemCard(
                                fileItem = wfFile,
                                onSelect = {
                                    val validation = viewModel.validateWorkflowFile(wfFile.file)
                                    if (!validation.isValid) {
                                        validationWarningResult = Pair(wfFile, validation)
                                    } else {
                                        val loaded = viewModel.loadWorkflowFromFile(wfFile.file)
                                        if (loaded != null) {
                                            Toast.makeText(context, "Loaded '${wfFile.name}' (Schema Verified)", Toast.LENGTH_SHORT).show()
                                            onWorkflowLoaded(loaded)
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Failed to parse ${wfFile.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onInspect = {
                                    fileToInspect = wfFile
                                    inspectedContent = runCatching { wfFile.file.readText() }.getOrDefault("{}")
                                },
                                onDelete = { fileToDelete = wfFile }
                            )
                        }
                    }
                }
            }
        }
    }

    // Inspect JSON Dialog
    fileToInspect?.let { item ->
        AlertDialog(
            onDismissRequest = { fileToInspect = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(inspectedContent))
                            Toast.makeText(context, "Copied JSON to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = inspectedContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = PrimaryNeon
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val loaded = viewModel.loadWorkflowFromFile(item.file)
                        if (loaded != null) {
                            fileToInspect = null
                            onWorkflowLoaded(loaded)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Load into Studio", color = Color(0xFF00354E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToInspect = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Local Workflow?") },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete '${item.name}' from app storage?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkflowFile(item.file)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Schema Validation Warning Dialog
    validationWarningResult?.let { (fileItem, validation) ->
        AlertDialog(
            onDismissRequest = { validationWarningResult = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = SecondaryOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "ComfyUI Schema Warning",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "The file '${fileItem.name}' is missing one or more required node types for a complete ComfyUI diffusion pipeline:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (validation.hasModelLoader) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (validation.hasModelLoader) AccentEmerald else AccentRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Model Loader (CheckpointLoader / UNET)", fontSize = 12.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (validation.hasSampler) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (validation.hasSampler) AccentEmerald else AccentRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Diffusion Sampler (KSampler)", fontSize = 12.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (validation.hasConditioning) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (validation.hasConditioning) AccentEmerald else AccentRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Conditioning (CLIP Text Encode)", fontSize = 12.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (validation.hasLatentOrOutput) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (validation.hasLatentOrOutput) AccentEmerald else AccentRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Latent / Image Output (Latent/Save)", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }

                    if (validation.detectedNodeTypes.isNotEmpty()) {
                        Text(
                            text = "Detected nodes (${validation.totalNodes}): " + validation.detectedNodeTypes.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val loaded = viewModel.loadWorkflowFromFile(fileItem.file)
                        if (loaded != null) {
                            Toast.makeText(context, "Loaded '${fileItem.name}' into Studio", Toast.LENGTH_SHORT).show()
                            onWorkflowLoaded(loaded)
                            validationWarningResult = null
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryOrange),
                    modifier = Modifier.testTag("load_anyway_button")
                ) {
                    Text("Load Anyway", color = Color(0xFF452B00), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { validationWarningResult = null },
                    modifier = Modifier.testTag("cancel_load_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkflowFileItemCard(
    fileItem: LocalWorkflowFile,
    onSelect: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDefaultApi = fileItem.name.equals("workflow_api.json", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isDefaultApi) PrimaryNeon.copy(alpha = 0.6f) else BorderDark,
                RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .testTag("workflow_item_${fileItem.name}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fileItem.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = Color.White
                    )

                    if (isDefaultApi) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryNeon.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "DEFAULT API",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${fileItem.nodeCount} Nodes",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SecondaryOrange
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )

                    Text(
                        text = formatWorkflowFileSize(fileItem.sizeBytes),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )

                    Text(
                        text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(fileItem.lastModified)),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onInspect,
                    modifier = Modifier.size(32.dp).testTag("inspect_${fileItem.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Inspect JSON",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    modifier = Modifier
                        .height(30.dp)
                        .padding(horizontal = 4.dp)
                        .testTag("load_workflow_${fileItem.name}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Select",
                        color = Color(0xFF00354E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_${fileItem.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AccentRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun formatWorkflowFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}
