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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.comfyui.ui.screens.*
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel

enum class ComfyScreen(val title: String, val icon: ImageVector) {
    STUDIO("Studio", Icons.Default.Dashboard),
    BLUEPRINTS("Blueprints", Icons.Default.AccountTree),
    QUEUE("Queue", Icons.Default.HourglassTop),
    GALLERY("Gallery", Icons.Default.PhotoLibrary),
    MODELS("Models", Icons.Default.Memory),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ComfyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()

            ComfyUITheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
                var currentScreen by remember { mutableStateOf(ComfyScreen.STUDIO) }
                val queue by viewModel.queue.collectAsState()
                val activeJob by viewModel.activeJob.collectAsState()

                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current

                // Dismiss any floating action mode or keyboard before navigating
                val navigateTo: (ComfyScreen) -> Unit = { targetScreen ->
                    if (currentScreen != targetScreen) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        currentScreen = targetScreen
                    }
                }

                // Support hardware and gesture back navigation to Studio
                BackHandler(enabled = currentScreen != ComfyScreen.STUDIO) {
                    navigateTo(ComfyScreen.STUDIO)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("comfy_bottom_navigation")
                        ) {
                            ComfyScreen.values().forEach { screen ->
                                val isSelected = currentScreen == screen
                                val hasQueueBadge = screen == ComfyScreen.QUEUE && (activeJob != null || queue.isNotEmpty())

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { navigateTo(screen) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (hasQueueBadge) {
                                                    val totalActive = (if (activeJob != null) 1 else 0) + queue.size
                                                    Badge(containerColor = PrimaryNeon) {
                                                        Text("$totalActive", color = Color(0xFF00354E), fontWeight = FontWeight.Bold)
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
                            ComfyScreen.STUDIO -> StudioScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navigateTo(ComfyScreen.SETTINGS) }
                            )
                            ComfyScreen.BLUEPRINTS -> BlueprintsScreen(
                                viewModel = viewModel,
                                onNavigateToStudio = { navigateTo(ComfyScreen.STUDIO) }
                            )
                            ComfyScreen.QUEUE -> QueueScreen(viewModel = viewModel)
                            ComfyScreen.GALLERY -> GalleryScreen(
                                viewModel = viewModel,
                                onNavigateToStudio = { navigateTo(ComfyScreen.STUDIO) }
                            )
                            ComfyScreen.MODELS -> ModelsScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navigateTo(ComfyScreen.SETTINGS) }
                            )
                            ComfyScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navigateTo(ComfyScreen.STUDIO) }
                            )
                        }
                    }
                }
            }
        }
    }
}
