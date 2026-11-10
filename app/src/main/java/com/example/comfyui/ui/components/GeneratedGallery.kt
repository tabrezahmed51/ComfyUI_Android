package com.example.comfyui.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data item representing an image file stored in the application's cache directory.
 */
data class CachedImageItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
)

/**
 * GeneratedGallery composable using Coil to display images stored in the app's cache directory.
 * Features:
 * - Coil AsyncImage rendering in responsive grid.
 * - Single and multi-selection mode.
 * - Delete selected image(s) with confirmation dialog.
 * - External image sharing via FileProvider share intent (single & multiple).
 * - Fullscreen preview dialog with file metadata.
 * - Refresh and cache clearing actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedGallery(
    viewModel: ComfyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cachedImages by remember { mutableStateOf<List<CachedImageItem>>(emptyList()) }
    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var previewImage by remember { mutableStateOf<CachedImageItem?>(null) }
    var fileToDelete by remember { mutableStateOf<CachedImageItem?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Helper to query images from context.cacheDir
    val loadCacheImages = {
        val list = mutableListOf<CachedImageItem>()
        context.cacheDir.listFiles { file ->
            val ext = file.extension.lowercase(Locale.ROOT)
            ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp"
        }?.forEach { file ->
            list.add(
                CachedImageItem(
                    file = file,
                    name = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )
            )
        }
        cachedImages = list.sortedByDescending { it.lastModified }
    }

    LaunchedEffect(Unit) {
        loadCacheImages()
    }

    val isSelectionMode = selectedFiles.isNotEmpty()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("generated_gallery")
        ) {
        // Toolbar with Counts & Share / Delete Actions
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSelectionMode) "${selectedFiles.size} Selected" else "Cache Gallery (${cachedImages.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelectionMode) PrimaryNeon else Color.White
                            )
                        )
                    }
                    Text(
                        text = "cacheDir: ${cachedImages.sumOf { it.sizeBytes } / 1024} KB used",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSelectionMode) {
                        // Save Selected to Public Downloads Action
                        IconButton(
                            onClick = {
                                val filesToSave = cachedImages.filter { it.file.absolutePath in selectedFiles }.map { it.file }
                                val count = saveMultipleImagesToDownloads(context, filesToSave)
                                if (count > 0) {
                                    Toast.makeText(context, "Saved $count image(s) to Downloads/ComfyUI", Toast.LENGTH_SHORT).show()
                                    selectedFiles = emptySet()
                                } else {
                                    Toast.makeText(context, "Failed to save to Downloads", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp).testTag("save_selected_to_downloads")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Save to Downloads", tint = PrimaryNeon, modifier = Modifier.size(20.dp))
                        }

                        // Share Selected Intent Action
                        IconButton(
                            onClick = {
                                val filesToShare = cachedImages.filter { it.file.absolutePath in selectedFiles }.map { it.file }
                                shareMultipleImages(context, filesToShare)
                            },
                            modifier = Modifier.size(36.dp).testTag("share_selected_images")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Selected", tint = PrimaryNeon, modifier = Modifier.size(20.dp))
                        }

                        // Delete Selected Action
                        IconButton(
                            onClick = { showBatchDeleteDialog = true },
                            modifier = Modifier.size(36.dp).testTag("delete_selected_images")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = AccentRose, modifier = Modifier.size(20.dp))
                        }

                        // Clear Selection
                        TextButton(
                            onClick = { selectedFiles = emptySet() }
                        ) {
                            Text("Cancel", fontSize = 11.sp, color = Color.LightGray)
                        }
                    } else {
                        // Refresh button
                        IconButton(
                            onClick = {
                                loadCacheImages()
                                viewModel.refreshLocalCacheImages()
                            },
                            modifier = Modifier.size(36.dp).testTag("refresh_generated_gallery")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryNeon, modifier = Modifier.size(20.dp))
                        }

                        if (cachedImages.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedFiles = cachedImages.map { it.file.absolutePath }.toSet()
                                }
                            ) {
                                Text("Select All", fontSize = 11.sp, color = PrimaryNeon)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (cachedImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "App cache directory is empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                    Text(
                        text = "Execute a generation in Studio to populate cached outputs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 145.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(cachedImages, key = { it.file.absolutePath }) { item ->
                    val isSelected = item.file.absolutePath in selectedFiles

                    GalleryItemCard(
                        item = item,
                        isSelected = isSelected,
                        onCardClick = {
                            if (isSelectionMode) {
                                selectedFiles = if (isSelected) {
                                    selectedFiles - item.file.absolutePath
                                } else {
                                    selectedFiles + item.file.absolutePath
                                }
                            } else {
                                previewImage = item
                            }
                        },
                        onCardLongClick = {
                            selectedFiles = if (isSelected) {
                                selectedFiles - item.file.absolutePath
                            } else {
                                selectedFiles + item.file.absolutePath
                            }
                        },
                        onShare = { shareSingleImage(context, item.file) },
                        onDelete = { fileToDelete = item },
                        onDownload = {
                            val success = saveImageToPublicDownloads(context, item.file)
                            if (success) {
                                Toast.makeText(context, "Saved to Downloads/ComfyUI", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to save to Downloads", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    } // End of Column

        // Fullscreen Smooth Shared Expansion Overlay
        AnimatedVisibility(
            visible = previewImage != null,
            enter = fadeIn(animationSpec = tween(220)) + scaleIn(
                initialScale = 0.65f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(
                targetScale = 0.65f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            previewImage?.let { item ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.90f))
                        .clickable { previewImage = null }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.7f)),
                        shadowElevation = 24.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.92f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = false) {}
                            .testTag("fullscreen_expansion_card")
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Watermark Capsule Menu & Close button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SurfaceVariantDark,
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).background(PrimaryNeon, CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ComfyUI • Expanded HD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                IconButton(
                                    onClick = { previewImage = null },
                                    modifier = Modifier.size(32.dp).testTag("close_expansion_viewer")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                                }
                            }

                            // Full Expansion Image with Smooth Fit
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = item.file,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            // Bottom Metadata & Actions Bar
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Size: ${formatGalleryFileSize(item.sizeBytes)}", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(item.lastModified)),
                                        fontSize = 11.sp,
                                        color = PrimaryNeon
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = BorderDark)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons: Download to Downloads, Share, Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val success = saveImageToPublicDownloads(context, item.file)
                                            if (success) {
                                                Toast.makeText(context, "Saved to Downloads/ComfyUI", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to save to Downloads", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryNeon,
                                            contentColor = Color(0xFF00354E)
                                        ),
                                        modifier = Modifier.weight(1f).height(38.dp).testTag("download_expanded_image")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    FilledTonalButton(
                                        onClick = { shareSingleImage(context, item.file) },
                                        modifier = Modifier.weight(1f).height(38.dp).testTag("share_expanded_image"),
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceVariantDark)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", color = Color.White, fontSize = 12.sp)
                                    }

                                    IconButton(
                                        onClick = {
                                            fileToDelete = item
                                            previewImage = null
                                        },
                                        modifier = Modifier.size(38.dp).testTag("delete_expanded_image")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AccentRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } // End of Box

    // Single Delete Dialog
    fileToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Cached Image?") },
            text = {
                Text(
                    text = "Are you sure you want to delete '${item.name}' from the app cache?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        item.file.delete()
                        selectedFiles = selectedFiles - item.file.absolutePath
                        fileToDelete = null
                        loadCacheImages()
                        viewModel.refreshLocalCacheImages()
                        Toast.makeText(context, "Deleted from cache", Toast.LENGTH_SHORT).show()
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

    // Batch Delete Dialog
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Delete ${selectedFiles.size} Images?") },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete all ${selectedFiles.size} selected images from the application cache?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedFiles.forEach { path ->
                            runCatching { File(path).delete() }
                        }
                        selectedFiles = emptySet()
                        showBatchDeleteDialog = false
                        loadCacheImages()
                        viewModel.refreshLocalCacheImages()
                        Toast.makeText(context, "Selected images deleted from cache", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text("Delete All Selected")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GalleryItemCard(
    item: CachedImageItem,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PrimaryNeon else BorderDark,
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCardClick() }
            .testTag("gallery_card_${item.name}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = item.file,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Selection Indicator Badge
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PrimaryNeon.copy(alpha = 0.25f))
                    )
                    Surface(
                        shape = CircleShape,
                        color = PrimaryNeon,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF00354E),
                            modifier = Modifier.padding(3.dp)
                        )
                    }
                }

                // File size badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = formatGalleryFileSize(item.sizeBytes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(28.dp).testTag("download_cached_${item.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save to Downloads",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(28.dp).testTag("share_cached_${item.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("delete_cached_${item.name}")
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
}

/**
 * Dispatches an Android ACTION_SEND share intent with content URI granted via FileProvider.
 */
fun shareSingleImage(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Generated Image")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Dispatches an Android ACTION_SEND_MULTIPLE share intent for batch sharing.
 */
fun shareMultipleImages(context: Context, files: List<File>) {
    try {
        val uris = ArrayList<Uri>()
        for (f in files) {
            uris.add(
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    f
                )
            )
        }
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share ${files.size} Images")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share images: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun formatGalleryFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}

/**
 * Saves an image file from the app's internal cache to the device's public Downloads directory.
 */
fun saveImageToPublicDownloads(context: Context, file: File): Boolean {
    return try {
        val mimeType = when (file.extension.lowercase(Locale.ROOT)) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ComfyUI")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
                true
            } else {
                false
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val comfyDir = File(downloadsDir, "ComfyUI").apply { mkdirs() }
            val targetFile = File(comfyDir, file.name)
            file.copyTo(targetFile, overwrite = true)
            true
        }
    } catch (e: Exception) {
        Log.e("GeneratedGallery", "Error saving to Downloads: ${e.localizedMessage}", e)
        false
    }
}

/**
 * Saves multiple image files from the cache to the public Downloads directory.
 */
fun saveMultipleImagesToDownloads(context: Context, files: List<File>): Int {
    var count = 0
    for (f in files) {
        if (saveImageToPublicDownloads(context, f)) {
            count++
        }
    }
    return count
}
