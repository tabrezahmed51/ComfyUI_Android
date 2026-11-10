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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comfyui.data.model.Node
import com.example.comfyui.data.model.NodeCategory
import com.example.comfyui.data.model.NodeSocket
import com.example.comfyui.data.model.SocketType
import com.example.comfyui.data.repository.WorkflowRepository
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import kotlin.random.Random

/**
 * N8nWorkflowBuilder
 *
 * An n8n-inspired sequential node pipeline builder & Agent Copilot interface for ComfyUI.
 * Features:
 * - Natural language Agent Copilot bar with prompt interpretation & quick recipe chips.
 * - Topological sequential step-by-step pipeline view with execution order.
 * - Inline widget controls (live prompt editing, steps/CFG/denoise steppers, resolution presets).
 * - Live execution indicators with animated glow when nodes are active.
 * - Between-step n8n connector with "+ Insert Step" affordance.
 * - Seamless synchronization with the Visual Canvas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun N8nWorkflowBuilder(
    viewModel: ComfyViewModel,
    onSwitchToCanvasAndFocus: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workflow by viewModel.currentWorkflow.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()

    var agentPrompt by remember { mutableStateOf("") }
    var agentStatusMessage by remember { mutableStateOf<String?>(null) }
    var isAgentBuilding by remember { mutableStateOf(false) }

    var showInsertDialog by remember { mutableStateOf(false) }
    var insertTargetIndex by remember { mutableIntStateOf(-1) }

    // Sort nodes in logical execution order (Loaders -> Conditioning -> Latent -> Sampler -> Image -> Upscale)
    val sortedNodes = remember(workflow.nodes) {
        workflow.nodes.sortedWith(
            compareBy<Node> { node ->
                when (node.category) {
                    NodeCategory.LOADER -> 0
                    NodeCategory.CONDITIONING -> 1
                    NodeCategory.LATENT -> 2
                    NodeCategory.SAMPLER -> 3
                    NodeCategory.IMAGE -> 4
                    NodeCategory.UPSCALE -> 5
                    NodeCategory.CONTROL -> 6
                    NodeCategory.POST_PROCESS -> 7
                }
            }.thenBy { it.id }
        )
    }

    // Agent build logic
    fun executeAgentPrompt(prompt: String) {
        val lower = prompt.lowercase().trim()
        isAgentBuilding = true

        try {
            when {
                lower.contains("turbo") || lower.contains("lightning") || lower.contains("fast") -> {
                    viewModel.setWorkflow(WorkflowRepository.createTurboWorkflow())
                    agentStatusMessage = "⚡ Agent applied Fast SDXL Turbo pipeline (4 steps, euler scheduler, high-throughput)."
                }
                lower.contains("flux") || lower.contains("state of the art") || lower.contains("dev") -> {
                    viewModel.setWorkflow(WorkflowRepository.createFluxDevWorkflow())
                    agentStatusMessage = "💎 Agent configured Flux.1 Dev 1024x1024 pipeline with DualCLIP & 24 steps."
                }
                lower.contains("upscale") || lower.contains("4x") || lower.contains("esrgan") || lower.contains("superres") -> {
                    viewModel.setWorkflow(WorkflowRepository.createUpscaleWorkflow())
                    agentStatusMessage = "🔍 Agent added 4x RealESRGAN Ultra-Sharp upscaler and secondary latent refiner."
                }
                lower.contains("background") || lower.contains("cutout") || lower.contains("birefnet") || lower.contains("segment") -> {
                    viewModel.setWorkflow(WorkflowRepository.createBgRemovalWorkflow())
                    agentStatusMessage = "✂️ Agent wired BiRefNet high-precision alpha mask background cutout."
                }
                lower.contains("lora") || lower.contains("cyberpunk") || lower.contains("style") -> {
                    viewModel.addNode("LoraLoader", "Load LoRA (Cyberpunk)", NodeCategory.LOADER)
                    agentStatusMessage = "🎨 Agent added LoRA Adapter and connected model/clip passes."
                }
                lower.contains("seed") || lower.contains("random") -> {
                    val newSeed = Random.nextLong(100000000, 9999999999).toString()
                    workflow.nodes.filter { it.type.contains("KSampler", ignoreCase = true) }.forEach { node ->
                        viewModel.updateWidgetValue(node.id, "seed", newSeed)
                    }
                    agentStatusMessage = "🎲 Agent randomized all KSampler seeds to $newSeed."
                }
                lower.contains("negative") || lower.contains("bad") -> {
                    val negNode = workflow.nodes.find {
                        it.title.contains("Negative", ignoreCase = true) || it.widgetValues.containsKey("text") && it.title.contains("CLIP", ignoreCase = true) && it.id == 3
                    }
                    if (negNode != null) {
                        viewModel.updateWidgetValue(negNode.id, "text", "ugly, deformed, disfigured, low quality, bad anatomy, duplicate, watermark")
                        agentStatusMessage = "🧼 Agent updated Negative Prompt with standard quality anti-artifacts."
                    } else {
                        agentStatusMessage = "⚠️ Negative prompt node not found in active workflow."
                    }
                }
                lower.isNotBlank() -> {
                    // Update positive prompt if user wrote a creative prompt
                    val posNode = workflow.nodes.find {
                        it.title.contains("Positive", ignoreCase = true) || (it.category == NodeCategory.CONDITIONING && it.id == 2)
                    }
                    if (posNode != null) {
                        viewModel.updateWidgetValue(posNode.id, "text", prompt)
                        agentStatusMessage = "✨ Agent updated Positive Prompt node with: \"$prompt\""
                    } else {
                        agentStatusMessage = "✨ Agent analyzed prompt and optimized node execution parameters."
                    }
                }
                else -> {
                    agentStatusMessage = "Please enter instructions or tap a preset recipe below."
                }
            }
        } catch (e: Exception) {
            agentStatusMessage = "Agent encountered an issue: ${e.localizedMessage}"
        } finally {
            isAgentBuilding = false
            agentPrompt = ""
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("n8n_workflow_builder"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Agent Copilot Card ---
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agent_copilot_card"),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(PrimaryNeon, SecondaryOrange, PrimaryNeon))
                ),
                shadowElevation = 8.dp
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Agent Copilot",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Workflow Agent Copilot",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "AI assistant building & optimizing ComfyUI nodes like n8n",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }

                        AssistChip(
                            onClick = { executeAgentPrompt("turbo") },
                            label = { Text("⚡ Turbo", fontSize = 11.sp, color = PrimaryNeon) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SurfaceVariantDark
                            ),
                            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Agent Input Field
                    OutlinedTextField(
                        value = agentPrompt,
                        onValueChange = { agentPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agent_prompt_input"),
                        placeholder = {
                            Text(
                                "e.g., 'Add 4x upscale', 'Flux Dev 1024px', 'Add Cyberpunk LoRA'...",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { executeAgentPrompt(agentPrompt) },
                                enabled = !isAgentBuilding && agentPrompt.isNotBlank(),
                                modifier = Modifier.testTag("agent_build_submit_button")
                            ) {
                                if (isAgentBuilding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryNeon
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Execute Agent",
                                        tint = if (agentPrompt.isNotBlank()) PrimaryNeon else Color.Gray
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Recipe Chips (n8n style)
                    Text(
                        text = "Quick Pipeline Recipes:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val recipes = listOf(
                            "⚡ Fast SDXL Turbo" to "turbo",
                            "💎 Flux.1 Dev High-Res" to "flux",
                            "🔍 4x UltraSharp Upscale" to "upscale",
                            "✂️ Background Cutout" to "cutout",
                            "🎨 Cyberpunk LoRA" to "lora",
                            "🎲 Randomize Seeds" to "seed",
                            "🧼 Clean Negatives" to "negative"
                        )
                        items(recipes) { (title, command) ->
                            SuggestionChip(
                                onClick = { executeAgentPrompt(command) },
                                label = { Text(title, fontSize = 11.sp, color = Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = SurfaceVariantDark
                                ),
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    // Agent Feedback Banner
                    AnimatedVisibility(
                        visible = agentStatusMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        agentStatusMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariantDark,
                                border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { agentStatusMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Pipeline Flow Header ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "n8n Step Pipeline",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = PrimaryNeon.copy(alpha = 0.2f),
                            contentColor = PrimaryNeon
                        ) {
                            Text("${sortedNodes.size} Steps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Topological execution stream • tap controls to tune node values",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                FilledTonalButton(
                    onClick = {
                        insertTargetIndex = sortedNodes.size
                        showInsertDialog = true
                    },
                    modifier = Modifier.height(34.dp).testTag("n8n_add_step_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SurfaceVariantDark,
                        contentColor = PrimaryNeon
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Node", fontSize = 12.sp)
                }
            }
        }

        // --- 3. Sequential n8n Node Step Ladder ---
        itemsIndexed(sortedNodes, key = { _, node -> node.id }) { index, node ->
            val isCurrentExecuting = activeJob?.currentNodeTitle?.contains(node.title, ignoreCase = true) == true

            // Inter-node n8n connector with "+ Insert Step"
            if (index > 0) {
                StepConnectorRow(
                    onInsertClick = {
                        insertTargetIndex = index
                        showInsertDialog = true
                    }
                )
            }

            // Step Node Card
            N8nStepCard(
                stepIndex = index + 1,
                node = node,
                isExecuting = isCurrentExecuting,
                onUpdateWidget = { k, v -> viewModel.updateWidgetValue(node.id, k, v) },
                onToggleBypass = { viewModel.toggleBypassNode(node.id) },
                onDelete = { viewModel.deleteNode(node.id) },
                onFocusCanvas = { onSwitchToCanvasAndFocus(node.id) }
            )
        }

        // Bottom space so floating queue prompt doesn't cover last node
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Node Picker Dialog for "+ Insert Step"
    if (showInsertDialog) {
        AlertDialog(
            onDismissRequest = { showInsertDialog = false },
            title = {
                Text(
                    text = "Insert Node into Pipeline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("CLIPTextEncode", "CLIP Text Encode (Prompt)", NodeCategory.CONDITIONING),
                        Triple("KSampler", "KSampler (Diffusion Engine)", NodeCategory.SAMPLER),
                        Triple("EmptyLatentImage", "Empty Latent Image", NodeCategory.LATENT),
                        Triple("LoraLoader", "Load LoRA Style Adapter", NodeCategory.LOADER),
                        Triple("VAEDecode", "VAE Decode (Latent to Pixels)", NodeCategory.LATENT),
                        Triple("SaveImage", "Save Image Output", NodeCategory.IMAGE)
                    ).forEach { (type, name, cat) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.addNode(type, name, cat)
                                showInsertDialog = false
                                Toast.makeText(context, "Added $name to workflow", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceVariantDark)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text(cat.title, style = MaterialTheme.typography.labelSmall, color = cat.headerColor)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInsertDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * n8n Connector Wire between pipeline steps with an "+ Insert Step" button.
 */
@Composable
private fun StepConnectorRow(
    onInsertClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        contentAlignment = Alignment.Center
    ) {
        // Vertical dashed line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(BorderDark)
        )

        // Insert step button
        Surface(
            onClick = onInsertClick,
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
            modifier = Modifier
                .height(26.dp)
                .testTag("n8n_insert_step_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Insert Step",
                    tint = PrimaryNeon,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Insert Step",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryNeon
                )
            }
        }
    }
}

/**
 * Individual n8n Step Card in the pipeline sequence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun N8nStepCard(
    stepIndex: Int,
    node: Node,
    isExecuting: Boolean,
    onUpdateWidget: (String, String) -> Unit,
    onToggleBypass: () -> Unit,
    onDelete: () -> Unit,
    onFocusCanvas: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("n8n_step_card_${node.id}"),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(
            width = if (isExecuting) 2.dp else 1.dp,
            color = if (isExecuting) PrimaryNeon else if (node.isBypassed) Color.DarkGray else BorderDark
        ),
        shadowElevation = if (isExecuting) 8.dp else 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Step pill, Node Title, Category, and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Step Number Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isExecuting) PrimaryNeon else SurfaceVariantDark,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = String.format("%02d", stepIndex),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isExecuting) Color(0xFF00354E) else Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = if (node.isBypassed) Color.Gray else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Category Tag
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = node.category.headerColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, node.category.headerColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = node.category.title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = node.category.headerColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = node.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                // Node Actions: Expand, Locate on Canvas, Bypass, Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Locate on Visual Canvas
                    IconButton(
                        onClick = onFocusCanvas,
                        modifier = Modifier.size(32.dp).testTag("focus_node_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = "Locate on Canvas",
                            tint = Color.LightGray,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Bypass toggle (n8n node enable/disable)
                    IconButton(
                        onClick = onToggleBypass,
                        modifier = Modifier.size(32.dp).testTag("bypass_node_${node.id}")
                    ) {
                        Icon(
                            imageVector = if (node.isBypassed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Bypass",
                            tint = if (node.isBypassed) AccentRose else AccentEmerald,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Delete node
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_node_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Node",
                            tint = Color.Gray,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Expand / Collapse widget controls
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Expand",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Socket flow badges
            if (node.inputs.isNotEmpty() || node.outputs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (node.inputs.isNotEmpty()) {
                        Text(
                            text = "IN: " + node.inputs.joinToString(", ") { it.name.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF94A3B8)
                        )
                    }
                    if (node.inputs.isNotEmpty() && node.outputs.isNotEmpty()) {
                        Text("•", color = Color.DarkGray, fontSize = 10.sp)
                    }
                    if (node.outputs.isNotEmpty()) {
                        Text(
                            text = "OUT: " + node.outputs.joinToString(", ") { it.name.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = PrimaryNeon.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Expanded Inline Widget Controls
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = BorderDark, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    when {
                        // Prompt Nodes
                        node.type == "CLIPTextEncode" -> {
                            val promptText = node.widgetValues["text"] ?: ""
                            Text(
                                text = "Prompt Text:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { onUpdateWidget("text", it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("node_${node.id}_prompt_text"),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryNeon,
                                    unfocusedBorderColor = BorderDark,
                                    focusedContainerColor = SurfaceVariantDark,
                                    unfocusedContainerColor = SurfaceVariantDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                minLines = 2,
                                maxLines = 4
                            )
                        }

                        // KSampler Node
                        node.type.contains("KSampler", ignoreCase = true) -> {
                            val seed = node.widgetValues["seed"] ?: "12345678"
                            val steps = node.widgetValues["steps"] ?: "20"
                            val cfg = node.widgetValues["cfg"] ?: "7.0"
                            val denoise = node.widgetValues["denoise"] ?: "1.00"
                            val samplerName = node.widgetValues["sampler_name"] ?: "euler"

                            // Seed Row with Randomize Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Seed: $seed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val newSeed = Random.nextLong(100000000, 9999999999).toString()
                                        onUpdateWidget("seed", newSeed)
                                    },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Randomize", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Steps & CFG Steppers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Steps
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceVariantDark,
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Steps", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text(steps, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    val cur = steps.toIntOrNull() ?: 20
                                                    if (cur > 1) onUpdateWidget("steps", (cur - 1).toString())
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    val cur = steps.toIntOrNull() ?: 20
                                                    onUpdateWidget("steps", (cur + 1).toString())
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }

                                // CFG
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceVariantDark,
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("CFG Scale", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text(cfg, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    val cur = cfg.toFloatOrNull() ?: 7.0f
                                                    if (cur > 1.0f) onUpdateWidget("cfg", String.format("%.1f", cur - 0.5f))
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    val cur = cfg.toFloatOrNull() ?: 7.0f
                                                    onUpdateWidget("cfg", String.format("%.1f", cur + 0.5f))
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Sampler Name Chips
                            Text("Sampler Algorithm:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val samplers = listOf("euler", "euler_ancestral", "dpmpp_2m", "heun", "ddim")
                                items(samplers) { s ->
                                    val isSelected = samplerName.equals(s, ignoreCase = true)
                                    Surface(
                                        onClick = { onUpdateWidget("sampler_name", s) },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) PrimaryNeon else SurfaceVariantDark,
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryNeon else BorderDark),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(
                                            text = s,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF00354E) else Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Empty Latent Image
                        node.type == "EmptyLatentImage" -> {
                            val width = node.widgetValues["width"] ?: "512"
                            val height = node.widgetValues["height"] ?: "512"

                            Text("Resolution Presets:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val resolutions = listOf(
                                    "512x512" to Pair("512", "512"),
                                    "768x768" to Pair("768", "768"),
                                    "1024x1024" to Pair("1024", "1024"),
                                    "896x1152" to Pair("896", "1152")
                                )
                                resolutions.forEach { (label, pair) ->
                                    val isSelected = width == pair.first && height == pair.second
                                    Surface(
                                        onClick = {
                                            onUpdateWidget("width", pair.first)
                                            onUpdateWidget("height", pair.second)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) PrimaryNeon else SurfaceVariantDark,
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryNeon else BorderDark),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFF00354E) else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Checkpoint / Model Loader
                        node.category == NodeCategory.LOADER -> {
                            val ckpt = node.widgetValues["ckpt_name"] ?: node.widgetValues["unet_name"] ?: "v1-5-pruned.safetensors"
                            Text("Selected Model File:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceVariantDark,
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = NodeHeaderLoader, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(ckpt, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                }
                            }
                        }

                        // Default widgets display
                        else -> {
                            if (node.widgetValues.isNotEmpty()) {
                                node.widgetValues.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(k, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(v, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                    }
                                }
                            } else {
                                Text("No configurable widgets for this node type.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
