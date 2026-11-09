package com.example.comfyui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comfyui.data.model.ModelItem
import com.example.comfyui.data.model.ModelType
import com.example.comfyui.data.repository.WorkflowRepository
import com.example.comfyui.ui.auth.GoogleSignInButton
import com.example.comfyui.ui.auth.UserAuthStatusCard
import com.example.comfyui.ui.auth.signOutUser
import com.example.comfyui.ui.theme.*
import com.example.comfyui.ui.viewmodel.ComfyViewModel
import com.example.comfyui.ui.viewmodel.ServerMode

@Composable
fun ModelsScreen(
    viewModel: ComfyViewModel,
    modifier: Modifier = Modifier
) {
    val models = remember { WorkflowRepository.getAllModels() }
    val serverMode by viewModel.serverMode.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val serverApiKey by viewModel.serverApiKey.collectAsState()
    val isConnected by viewModel.isServerConnected.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showServerSettings by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var selectedTypeFilter by remember { mutableStateOf<ModelType?>(null) }

    val filteredModels = remember(selectedTypeFilter) {
        if (selectedTypeFilter == null) models else models.filter { it.type == selectedTypeFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .testTag("models_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Models & Cloud",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Firebase sync, diffusion models & server bridge",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = { showServerSettings = !showServerSettings },
                modifier = Modifier.testTag("server_settings_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Server Settings",
                    tint = PrimaryNeon
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Firebase Cloud User Profile Card
        UserAuthStatusCard(
            user = currentUser,
            onSignInClicked = { showSignInDialog = true },
            onSignOutClicked = { signOutUser(context, scope) },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Server Connection Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (serverMode == ServerMode.MOBILE_EMULATION || isConnected) AccentEmerald else AccentRose,
                                    RoundedCornerShape(5.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (serverMode == ServerMode.MOBILE_EMULATION) "Standalone Mobile Engine" else "Remote Server ($serverUrl)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = {
                            if (serverMode == ServerMode.MOBILE_EMULATION) {
                                viewModel.setServerMode(ServerMode.REMOTE_COMFY_SERVER)
                                viewModel.checkServerConnection()
                            } else {
                                viewModel.setServerMode(ServerMode.MOBILE_EMULATION)
                            }
                        }
                    ) {
                        Text(
                            text = if (serverMode == ServerMode.MOBILE_EMULATION) "Switch to Remote" else "Switch to Mobile",
                            fontSize = 11.sp,
                            color = PrimaryNeon
                        )
                    }
                }

                if (serverMode == ServerMode.REMOTE_COMFY_SERVER) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { viewModel.setServerUrl("http://127.0.0.1:8188") },
                            label = { Text("Termux Local", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                        SuggestionChip(
                            onClick = { viewModel.setServerUrl("https://cloud.comfy.org") },
                            label = { Text("Comfy Cloud", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                        SuggestionChip(
                            onClick = { viewModel.setServerUrl("http://10.0.2.2:8188") },
                            label = { Text("Emulator", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                        SuggestionChip(
                            onClick = { viewModel.setServerUrl("http://192.168.1.100:8188") },
                            label = { Text("LAN IP", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { viewModel.setServerUrl(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input"),
                        label = { Text("Server URL (e.g. http://127.0.0.1:8188)") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        trailingIcon = {
                            IconButton(onClick = { viewModel.checkServerConnection() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Test Connection", tint = PrimaryNeon)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = serverApiKey,
                        onValueChange = { viewModel.setServerApiKey(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_api_key_input"),
                        label = { Text("COMFY_UI_SERVER_KEY / Auth Token") },
                        placeholder = { Text("Paste Termux generated key...") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Key Visibility",
                                    tint = PrimaryNeon
                                )
                            }
                        }
                    )

                    if (serverApiKey.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Key attached (Length: ${serverApiKey.length} chars)",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentEmerald
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Model Type Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedTypeFilter == null,
                onClick = { selectedTypeFilter = null },
                label = { Text("All", fontSize = 11.sp) }
            )
            ModelType.values().forEach { type ->
                FilterChip(
                    selected = selectedTypeFilter == type,
                    onClick = { selectedTypeFilter = type },
                    label = { Text(type.name, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Models List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredModels) { model ->
                ModelItemCard(model = model)
            }
        }
    }

    if (showSignInDialog) {
        AlertDialog(
            onDismissRequest = { showSignInDialog = false },
            title = { Text("Sign in to ComfyUI Mobile") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Connect your Google Account with Firebase to securely sync workflows, blueprints, and generated artworks across devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    GoogleSignInButton(
                        onAuthSuccess = {
                            showSignInDialog = false
                            authError = null
                        },
                        onAuthError = { err ->
                            authError = err
                        },
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
fun ModelItemCard(model: ModelItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceVariantDark
                    ) {
                        Text(
                            text = model.architecture,
                            style = MaterialTheme.typography.labelSmall.copy(color = PrimaryNeon, fontSize = 9.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${model.filename} • ${model.precision}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 11.sp),
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (model.description.isNotBlank()) {
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontSize = 11.sp),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SurfaceVariantDark.copy(alpha = 0.7f)
            ) {
                Text(
                    text = model.size,
                    style = MaterialTheme.typography.labelSmall.copy(color = SecondaryOrange, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
