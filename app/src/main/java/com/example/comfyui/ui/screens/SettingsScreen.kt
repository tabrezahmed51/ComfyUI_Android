package com.example.comfyui.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comfyui.ui.auth.GoogleSignInButton
import com.example.comfyui.ui.auth.UserAuthStatusCard
import com.example.comfyui.ui.auth.signOutUser
import com.example.comfyui.ui.components.ServerStatusDashboard
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import com.example.comfyui.ui.viewmodel.ServerMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ComfyViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentUrl by viewModel.serverUrl.collectAsState()
    val currentApiKey by viewModel.serverApiKey.collectAsState()
    val serverMode by viewModel.serverMode.collectAsState()
    val serverHealth by viewModel.serverHealth.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    val hasUnsavedChanges = remember(urlInput, apiKeyInput, currentUrl, currentApiKey) {
        urlInput.trim() != currentUrl.trim() || apiKeyInput.trim() != currentApiKey.trim()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("settings_screen")
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Settings & Connection",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Configure dynamic server endpoints & API authentication",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            modifier = Modifier.border(1.dp, BorderDark)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Server Status Dashboard Card
            ServerStatusDashboard(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            // GLOBAL SYSTEM THEME (MATERIAL 3) CARD
            val themeMode by viewModel.themeMode.collectAsState()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .testTag("settings_theme_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GLOBAL SYSTEM THEME (M3)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PrimaryNeon
                        )
                    )
                    Text(
                        text = "Switch between Dark, Light, or follow System default with Material 3 dynamic color palettes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Mode Selection Chips: System, Dark, Light
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text("System", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f).testTag("theme_system_chip")
                        )

                        FilterChip(
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            label = { Text("Dark", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f).testTag("theme_dark_chip")
                        )

                        FilterChip(
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text("Light", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f).testTag("theme_light_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderDark)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Color Toggle (Android 12+)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dynamic Colors (Android 12+)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                            }
                            Text(
                                text = "Derives primary and secondary tonal palettes from system wallpaper",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                            )
                        }

                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00354E),
                                checkedTrackColor = PrimaryNeon
                            ),
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }
                }
            }

            // Connection Mode Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EXECUTION ENGINE MODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PrimaryNeon
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = serverMode == ServerMode.REMOTE_COMFY_SERVER,
                            onClick = {
                                viewModel.setServerMode(ServerMode.REMOTE_COMFY_SERVER)
                                viewModel.checkServerHealth()
                            },
                            label = { Text("Remote Server (Retrofit)") },
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f).testTag("mode_remote_chip")
                        )

                        FilterChip(
                            selected = serverMode == ServerMode.MOBILE_EMULATION,
                            onClick = {
                                viewModel.setServerMode(ServerMode.MOBILE_EMULATION)
                                viewModel.checkServerHealth()
                            },
                            label = { Text("Standalone Mobile") },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f).testTag("mode_standalone_chip")
                        )
                    }

                    Text(
                        text = if (serverMode == ServerMode.REMOTE_COMFY_SERVER) {
                            "Communicates via Retrofit with your running ComfyUI instance (Termux, Comfy Cloud, or LAN server)."
                        } else {
                            "Uses internal procedural neural synthesis on-device with zero server dependencies."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Dynamic Server URL & Key Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "DYNAMIC SERVER CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PrimaryNeon
                        )
                    )
                    Text(
                        text = "Updates COMFYUI_SERVER_URL & COMFY_UI_SERVER_KEY dynamically across the application and Retrofit client",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // COMFYUI_SERVER_URL Field
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            saveFeedback = null
                        },
                        label = { Text("COMFYUI_SERVER_URL") },
                        placeholder = { Text("http://127.0.0.1:8188") },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryNeon)
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_server_url_input")
                    )

                    // Quick URL Presets
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                urlInput = "http://127.0.0.1:8188"
                                saveFeedback = null
                            },
                            label = { Text("Termux Local", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp).testTag("preset_url_termux")
                        )
                        SuggestionChip(
                            onClick = {
                                urlInput = "https://cloud.comfy.org"
                                saveFeedback = null
                            },
                            label = { Text("Comfy Cloud", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp).testTag("preset_url_cloud")
                        )
                        SuggestionChip(
                            onClick = {
                                urlInput = "http://10.0.2.2:8188"
                                saveFeedback = null
                            },
                            label = { Text("Emulator", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp).testTag("preset_url_emulator")
                        )
                        SuggestionChip(
                            onClick = {
                                urlInput = "http://192.168.1.100:8188"
                                saveFeedback = null
                            },
                            label = { Text("LAN IP", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp).testTag("preset_url_lan")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // COMFY_UI_SERVER_KEY Field
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            saveFeedback = null
                        },
                        label = { Text("COMFY_UI_SERVER_KEY / Auth Token") },
                        placeholder = { Text("Enter or paste API token...") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = SecondaryOrange)
                        },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Key Visibility",
                                    tint = if (keyVisible) PrimaryNeon else Color.Gray
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_server_api_key_input")
                    )

                    Text(
                        text = "Passed dynamically in headers: Authorization: Bearer <key> and X-API-Key: <key>",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (saveFeedback != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceVariantDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = saveFeedback ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = AccentEmerald
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateServerSettings(urlInput, apiKeyInput)
                                saveFeedback = "Settings saved & synced with Retrofit client"
                                Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_save_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00354E), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save & Apply", color = Color(0xFF00354E), fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.updateServerSettings(urlInput, apiKeyInput)
                                viewModel.checkServerHealth()
                            },
                            modifier = Modifier.testTag("settings_test_connection_button")
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.resetServerSettingsToDefault()
                            urlInput = viewModel.serverUrl.value
                            apiKeyInput = viewModel.serverApiKey.value
                            saveFeedback = "Reset to .env default parameters"
                            Toast.makeText(context, "Reset to .env defaults", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reset_defaults_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset to .env Defaults", fontSize = 12.sp)
                    }
                }
            }

            // Cloud Profile & Firebase Integration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CLOUD ACCOUNTS & FIREBASE SYNC",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PrimaryNeon
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    UserAuthStatusCard(
                        user = currentUser,
                        onSignInClicked = { showSignInDialog = true },
                        onSignOutClicked = { signOutUser(context, scope) }
                    )
                }
            }

            // System & Architecture Specs Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "STACK & ARCHITECTURE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PrimaryNeon
                        )
                    )
                    SpecRow("Networking", "Retrofit 2.11.0 • OkHttp 4.12.0")
                    SpecRow("Image Engine", "Coil 2.7.0")
                    SpecRow("Database", "Firebase Firestore & Auth")
                    SpecRow("Platform", "Android SDK 36 (Java 21 / Kotlin 2.2)")
                    SpecRow("Active Endpoint", currentUrl)
                }
            }
        }
    }

    if (showSignInDialog) {
        AlertDialog(
            onDismissRequest = { showSignInDialog = false },
            title = { Text("Sign in to ComfyUI Mobile") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Authenticate with Google to back up your workflows and sync outputs to Cloud Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    GoogleSignInButton(
                        onAuthSuccess = {
                            showSignInDialog = false
                            authError = null
                        },
                        onAuthError = { authError = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (authError != null) {
                        Text(
                            text = authError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentRose,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSignInDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White
        )
    }
}
