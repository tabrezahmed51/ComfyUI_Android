package com.example.comfyui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.comfyui.ui.screens.*
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel

enum class ComfyScreen(val title: String, val icon: ImageVector) {
    STUDIO("Studio", Icons.Default.Dashboard),
    BLUEPRINTS("Blueprints", Icons.Default.AccountTree),
    QUEUE("Queue", Icons.Default.HourglassTop),
    GALLERY("Gallery", Icons.Default.PhotoLibrary),
    MODELS("Models", Icons.Default.Memory)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ComfyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ComfyUITheme {
                var currentScreen by remember { mutableStateOf(ComfyScreen.STUDIO) }
                val queue by viewModel.queue.collectAsState()
                val activeJob by viewModel.activeJob.collectAsState()

                // Support hardware and gesture back navigation to Studio
                BackHandler(enabled = currentScreen != ComfyScreen.STUDIO) {
                    currentScreen = ComfyScreen.STUDIO
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BackgroundDark,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("comfy_bottom_navigation")
                        ) {
                            ComfyScreen.values().forEach { screen ->
                                val isSelected = currentScreen == screen
                                val hasQueueBadge = screen == ComfyScreen.QUEUE && (activeJob != null || queue.isNotEmpty())

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (hasQueueBadge) {
                                                    Badge(containerColor = PrimaryNeon) {
                                                        Text("${if (activeJob != null) 1 else 0 + queue.size}")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title
                                            )
                                        }
                                    },
                                    label = { Text(screen.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryNeon,
                                        selectedTextColor = PrimaryNeon,
                                        unselectedIconColor = BorderDark,
                                        unselectedTextColor = BorderDark,
                                        indicatorColor = SurfaceVariantDark
                                    ),
                                    modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            ComfyScreen.STUDIO -> StudioScreen(viewModel = viewModel)
                            ComfyScreen.BLUEPRINTS -> BlueprintsScreen(
                                viewModel = viewModel,
                                onNavigateToStudio = { currentScreen = ComfyScreen.STUDIO }
                            )
                            ComfyScreen.QUEUE -> QueueScreen(viewModel = viewModel)
                            ComfyScreen.GALLERY -> GalleryScreen(
                                viewModel = viewModel,
                                onNavigateToStudio = { currentScreen = ComfyScreen.STUDIO }
                            )
                            ComfyScreen.MODELS -> ModelsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
