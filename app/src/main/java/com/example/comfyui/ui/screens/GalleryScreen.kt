package com.example.comfyui.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.comfyui.R
import com.example.comfyui.data.model.GenerationJob
import com.example.comfyui.data.repository.WorkflowRepository
import com.example.comfyui.ui.components.GeneratedGallery
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: ComfyViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history by viewModel.history.collectAsState()
    val localCacheImages by viewModel.localCacheImages.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var selectedJob by remember { mutableStateOf<GenerationJob?>(null) }
    var showJsonDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .testTag("gallery_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Asset Gallery",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = if (selectedTab == 0) "${history.size} Generated outputs with embedded workflow metadata" else "${localCacheImages.size} Cached & saved image files in app directory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Selector between Cloud History and Local Cache Gallery
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = PrimaryNeon,
            divider = { HorizontalDivider(color = BorderDark) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Generation History (${history.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_history")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Local Cache (${localCacheImages.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_local_cache")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 1) {
            // Local Cache Gallery using Coil with delete and share
            GeneratedGallery(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Cloud / History Gallery
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No generated images yet.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(history) { job ->
                        GalleryThumbnailCard(
                            job = job,
                            onClick = { selectedJob = job }
                        )
                    }
                }
            }
        }
    }

    // Full Detail Dialog
    selectedJob?.let { job ->
        AlertDialog(
            onDismissRequest = { selectedJob = null },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromHistory(job)
                        selectedJob = null
                        onNavigateToStudio()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    modifier = Modifier.testTag("restore_workflow_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00354E), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load in Studio", color = Color(0xFF00354E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showJsonDialog = true }) {
                        Text("View JSON")
                    }
                    TextButton(onClick = { selectedJob = null }) {
                        Text("Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Render image
                    RenderOutputImage(
                        job = job,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Text(
                        text = "Positive Prompt:",
                        style = MaterialTheme.typography.labelSmall.copy(color = SecondaryOrange)
                    )
                    Text(
                        text = job.promptText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )

                    if (job.negativePromptText.isNotBlank()) {
                        Text(
                            text = "Negative Prompt:",
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentRose)
                        )
                        Text(
                            text = job.negativePromptText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    HorizontalDivider(color = BorderDark)

                    // Generation Metadata Grid
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MetaRow("Model Checkpoint", job.modelName)
                        MetaRow("Seed", job.seed.toString())
                        MetaRow("Steps / CFG", "${job.steps} steps / ${job.cfg}")
                        MetaRow("Sampler", job.samplerName)
                        MetaRow("Resolution", "${job.width}x${job.height}")
                        if (job.durationSeconds > 0) {
                            MetaRow("Generation Time", "${String.format("%.2f", job.durationSeconds)}s")
                        }
                    }
                }
            }
        )
    }

    // JSON Dialog
    if (showJsonDialog && selectedJob != null) {
        val jsonStr = remember(selectedJob) {
            selectedJob?.workflowSnapshot?.let { WorkflowRepository.exportToJson(it) }
                ?: "{}"
        }
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("ComfyUI Workflow JSON") },
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
                        text = jsonStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = PrimaryNeon
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showJsonDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun GalleryThumbnailCard(
    job: GenerationJob,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("gallery_item_${job.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RenderOutputImage(
                job = job,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = job.promptText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${job.modelName.take(16)} • Seed: ${job.seed}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RenderOutputImage(job: GenerationJob, modifier: Modifier = Modifier) {
    val imgPath = job.outputImageUrl
    val isFox = job.promptText.contains("fox", ignoreCase = true) || job.promptText.contains("animal", ignoreCase = true)

    if (imgPath != null && File(imgPath).exists()) {
        AsyncImage(
            model = File(imgPath),
            contentDescription = job.promptText,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val res = if (isFox) R.drawable.sample_cyber_fox else R.drawable.sample_cyberpunk
        Image(
            painter = painterResource(id = res),
            contentDescription = job.promptText,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}
