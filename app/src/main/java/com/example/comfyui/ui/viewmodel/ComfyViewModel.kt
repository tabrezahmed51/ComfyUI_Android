package com.example.comfyui.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.comfyui.data.engine.WorkflowEngine
import com.example.comfyui.data.firebase.FirebaseRepository
import com.example.comfyui.data.firebase.FirestoreWorkflow
import com.example.comfyui.data.model.*
import com.example.comfyui.data.repository.WorkflowRepository
import com.example.comfyui.ui.theme.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.content.Context
import com.example.comfyui.data.network.ComfyRetrofitClient
import com.example.comfyui.data.network.model.DeviceInfo
import com.example.comfyui.data.network.model.SystemInfo
import com.example.comfyui.data.network.model.SystemStatsResponse
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

enum class ServerMode {
    MOBILE_EMULATION,
    REMOTE_COMFY_SERVER
}

data class ServerHealth(
    val isOnline: Boolean = false,
    val isChecking: Boolean = false,
    val statusCode: Int? = null,
    val statusMessage: String = "Not Checked",
    val latencyMs: Long? = null,
    val lastCheckedTime: Long = 0L,
    val systemInfo: SystemInfo? = null,
    val deviceName: String? = null,
    val vramStats: String? = null
)

data class LocalCacheImage(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isFromCacheDir: Boolean
)

data class LocalWorkflowFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val nodeCount: Int
)

class ComfyViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = WorkflowEngine(application.applicationContext)

    private val _currentWorkflow = MutableStateFlow(WorkflowRepository.createStandardSDWorkflow())
    val currentWorkflow: StateFlow<Workflow> = _currentWorkflow.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<Int?>(null)
    val selectedNodeId: StateFlow<Int?> = _selectedNodeId.asStateFlow()

    private val _canvasScale = MutableStateFlow(1.0f)
    val canvasScale: StateFlow<Float> = _canvasScale.asStateFlow()

    private val _canvasOffsetX = MutableStateFlow(0f)
    val canvasOffsetX: StateFlow<Float> = _canvasOffsetX.asStateFlow()

    private val _canvasOffsetY = MutableStateFlow(0f)
    val canvasOffsetY: StateFlow<Float> = _canvasOffsetY.asStateFlow()

    private val _activeJob = MutableStateFlow<GenerationJob?>(null)
    val activeJob: StateFlow<GenerationJob?> = _activeJob.asStateFlow()

    private val _queue = MutableStateFlow<List<GenerationJob>>(emptyList())
    val queue: StateFlow<List<GenerationJob>> = _queue.asStateFlow()

    private val _history = MutableStateFlow<List<GenerationJob>>(emptyList())
    val history: StateFlow<List<GenerationJob>> = _history.asStateFlow()

    private val prefs by lazy {
        getApplication<Application>().getSharedPreferences("comfy_settings", Context.MODE_PRIVATE)
    }

    private val _serverMode = MutableStateFlow(ServerMode.MOBILE_EMULATION)
    val serverMode: StateFlow<ServerMode> = _serverMode.asStateFlow()

    private val _serverUrl = MutableStateFlow(
        prefs.getString("server_url", null) ?: ComfyRetrofitClient.currentBaseUrl.trimEnd('/')
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _serverApiKey = MutableStateFlow(
        prefs.getString("server_api_key", null) ?: ComfyRetrofitClient.currentApiKey
    )
    val serverApiKey: StateFlow<String> = _serverApiKey.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _serverHealth = MutableStateFlow(ServerHealth())
    val serverHealth: StateFlow<ServerHealth> = _serverHealth.asStateFlow()

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(
        prefs.getBoolean("use_dynamic_color", true)
    )
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _useDynamicColor.value = enabled
        prefs.edit().putBoolean("use_dynamic_color", enabled).apply()
    }

    private val _localCacheImages = MutableStateFlow<List<LocalCacheImage>>(emptyList())
    val localCacheImages: StateFlow<List<LocalCacheImage>> = _localCacheImages.asStateFlow()

    private val _localWorkflowFiles = MutableStateFlow<List<LocalWorkflowFile>>(emptyList())
    val localWorkflowFiles: StateFlow<List<LocalWorkflowFile>> = _localWorkflowFiles.asStateFlow()

    private val firebaseRepo = FirebaseRepository.create(application.applicationContext)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _cloudWorkflows = MutableStateFlow<List<FirestoreWorkflow>>(emptyList())
    val cloudWorkflows: StateFlow<List<FirestoreWorkflow>> = _cloudWorkflows.asStateFlow()

    private var executionJob: Job? = null

    init {
        // Sync Retrofit client with initial/persisted settings
        ComfyRetrofitClient.updateBaseUrl(_serverUrl.value)
        ComfyRetrofitClient.updateApiKey(_serverApiKey.value)
        seedInitialHistory()
        seedInitialCacheAndWorkflows()
        refreshLocalCacheImages()
        loadLocalWorkflowFiles()
        checkServerHealth()
        try {
            Firebase.auth.addAuthStateListener { auth ->
                _currentUser.value = auth.currentUser
                auth.currentUser?.let { user ->
                    viewModelScope.launch {
                        try {
                            firebaseRepo.syncUserProfile(user)
                            listenToCloudData()
                        } catch (e: Exception) {
                            Log.e("ComfyVM", "Error syncing Firebase profile", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ComfyVM", "Firebase auth initialization error", e)
        }
    }

    private fun listenToCloudData() {
        if (_currentUser.value == null) return
        viewModelScope.launch {
            try {
                firebaseRepo.observeWorkflows().collect { wfList ->
                    _cloudWorkflows.value = wfList
                }
            } catch (e: Exception) {
                Log.e("ComfyVM", "Error observing cloud workflows", e)
            }
        }
    }

    private fun seedInitialHistory() {
        val initialJob1 = GenerationJob(
            id = "seed_1",
            workflowName = "Text to Image (Standard SD 1.5)",
            promptText = "a majestic cyberpunk cityscape at twilight, glowing cyan neon signs, rain-slicked reflective streets, high detail, masterpiece, 8k",
            negativePromptText = "blurry, low quality, deformed, duplicate, bad anatomy",
            seed = 4820194827,
            steps = 20,
            cfg = 7.0f,
            samplerName = "euler",
            modelName = "v1-5-pruned-emaonly.safetensors",
            width = 512,
            height = 512,
            status = JobStatus.COMPLETED,
            currentNodeTitle = "Completed",
            currentStep = 20,
            totalSteps = 20,
            progress = 1.0f,
            durationSeconds = 3.4f,
            createdAt = System.currentTimeMillis() - 120000,
            completedAt = System.currentTimeMillis() - 116600,
            workflowSnapshot = WorkflowRepository.createStandardSDWorkflow()
        )

        val initialJob2 = GenerationJob(
            id = "seed_2",
            workflowName = "Text to Image (Flux.1 Dev)",
            promptText = "close up cinematic portrait of a robotic fox with crystalline fur and golden eyes, forest background, photorealistic, shallow depth of field, 35mm photograph",
            negativePromptText = "low quality, distorted",
            seed = 9182374612,
            steps = 24,
            cfg = 1.0f,
            samplerName = "euler",
            modelName = "flux1-dev.safetensors",
            width = 1024,
            height = 1024,
            status = JobStatus.COMPLETED,
            currentNodeTitle = "Completed",
            currentStep = 24,
            totalSteps = 24,
            progress = 1.0f,
            durationSeconds = 6.2f,
            createdAt = System.currentTimeMillis() - 60000,
            completedAt = System.currentTimeMillis() - 53800,
            workflowSnapshot = WorkflowRepository.createFluxDevWorkflow()
        )

        _history.value = listOf(initialJob2, initialJob1)
    }

    fun selectNode(id: Int?) {
        _selectedNodeId.value = id
    }

    fun updateCanvasTransform(scale: Float, dx: Float, dy: Float) {
        _canvasScale.value = (_canvasScale.value * scale).coerceIn(0.4f, 2.5f)
        _canvasOffsetX.value += dx
        _canvasOffsetY.value += dy
    }

    fun resetCanvasTransform() {
        _canvasScale.value = 1.0f
        _canvasOffsetX.value = 0f
        _canvasOffsetY.value = 0f
    }

    fun loadBlueprint(blueprint: Blueprint) {
        _currentWorkflow.value = blueprint.workflowFactory()
        _selectedNodeId.value = null
        resetCanvasTransform()
    }

    fun setWorkflow(workflow: Workflow) {
        _currentWorkflow.value = workflow
        _selectedNodeId.value = null
        resetCanvasTransform()
    }

    fun centerCanvasOnNode(nodeId: Int) {
        val node = _currentWorkflow.value.nodes.find { it.id == nodeId } ?: return
        _selectedNodeId.value = nodeId
        _canvasScale.value = 1.0f
        _canvasOffsetX.value = -node.positionX + 100f
        _canvasOffsetY.value = -node.positionY + 200f
    }

    fun updateWidgetValue(nodeId: Int, key: String, value: String) {
        _currentWorkflow.update { current ->
            val updatedNodes = current.nodes.map { node ->
                if (node.id == nodeId) {
                    val updatedWidgets = node.widgetValues.toMutableMap()
                    updatedWidgets[key] = value
                    node.copy(widgetValues = updatedWidgets)
                } else {
                    node
                }
            }
            current.copy(nodes = updatedNodes, lastModified = System.currentTimeMillis())
        }
    }

    fun moveNode(nodeId: Int, dx: Float, dy: Float) {
        _currentWorkflow.update { current ->
            val updatedNodes = current.nodes.map { node ->
                if (node.id == nodeId) {
                    node.copy(
                        positionX = (node.positionX + dx).coerceAtLeast(0f),
                        positionY = (node.positionY + dy).coerceAtLeast(0f)
                    )
                } else {
                    node
                }
            }
            current.copy(nodes = updatedNodes)
        }
    }

    fun deleteNode(nodeId: Int) {
        _currentWorkflow.update { current ->
            val updatedNodes = current.nodes.filterNot { it.id == nodeId }
            val updatedLinks = current.links.filterNot { it.fromNodeId == nodeId || it.toNodeId == nodeId }
            current.copy(nodes = updatedNodes, links = updatedLinks, lastModified = System.currentTimeMillis())
        }
        if (_selectedNodeId.value == nodeId) {
            _selectedNodeId.value = null
        }
    }

    fun toggleBypassNode(nodeId: Int) {
        _currentWorkflow.update { current ->
            val updatedNodes = current.nodes.map { node ->
                if (node.id == nodeId) {
                    node.copy(isBypassed = !node.isBypassed)
                } else {
                    node
                }
            }
            current.copy(nodes = updatedNodes)
        }
    }

    fun addNode(type: String, title: String, category: NodeCategory) {
        val current = _currentWorkflow.value
        val nextId = (current.nodes.maxOfOrNull { it.id } ?: 0) + 1

        val (inputs, outputs, defaultWidgets) = when (type) {
            "CLIPTextEncode" -> Triple(
                listOf(NodeSocket("clip", SocketType.CLIP, true)),
                listOf(NodeSocket("CONDITIONING", SocketType.CONDITIONING, false)),
                mapOf("text" to "a majestic landscape with mountains and lake")
            )
            "EmptyLatentImage" -> Triple(
                emptyList(),
                listOf(NodeSocket("LATENT", SocketType.LATENT, false)),
                mapOf("width" to "512", "height" to "512", "batch_size" to "1")
            )
            "KSampler" -> Triple(
                listOf(
                    NodeSocket("model", SocketType.MODEL, true),
                    NodeSocket("positive", SocketType.CONDITIONING, true),
                    NodeSocket("negative", SocketType.CONDITIONING, true),
                    NodeSocket("latent_image", SocketType.LATENT, true)
                ),
                listOf(NodeSocket("LATENT", SocketType.LATENT, false)),
                mapOf(
                    "seed" to "${Random.nextLong(1000000000, 9999999999)}",
                    "steps" to "20",
                    "cfg" to "7.0",
                    "sampler_name" to "euler",
                    "scheduler" to "normal",
                    "denoise" to "1.00"
                )
            )
            "VAEDecode" -> Triple(
                listOf(
                    NodeSocket("samples", SocketType.LATENT, true),
                    NodeSocket("vae", SocketType.VAE, true)
                ),
                listOf(NodeSocket("IMAGE", SocketType.IMAGE, false)),
                emptyMap()
            )
            "SaveImage" -> Triple(
                listOf(NodeSocket("images", SocketType.IMAGE, true)),
                emptyList(),
                mapOf("filename_prefix" to "ComfyUI")
            )
            "LoraLoader" -> Triple(
                listOf(
                    NodeSocket("model", SocketType.MODEL, true),
                    NodeSocket("clip", SocketType.CLIP, true)
                ),
                listOf(
                    NodeSocket("MODEL", SocketType.MODEL, false),
                    NodeSocket("CLIP", SocketType.CLIP, false)
                ),
                mapOf(
                    "lora_name" to "cyberpunk_style_v2.safetensors",
                    "strength_model" to "1.0",
                    "strength_clip" to "1.0"
                )
            )
            else -> Triple(emptyList(), emptyList(), emptyMap())
        }

        val newNode = Node(
            id = nextId,
            type = type,
            title = title,
            category = category,
            positionX = -_canvasOffsetX.value + 120f,
            positionY = -_canvasOffsetY.value + 200f,
            inputs = inputs,
            outputs = outputs,
            widgetValues = defaultWidgets
        )

        _currentWorkflow.update {
            it.copy(nodes = it.nodes + newNode, lastModified = System.currentTimeMillis())
        }
        _selectedNodeId.value = nextId
    }

    fun queuePrompt() {
        val workflowToRun = _currentWorkflow.value

        // Check if seed should randomize
        val updatedNodes = workflowToRun.nodes.map { node ->
            if (node.type.contains("KSampler", ignoreCase = true) &&
                node.widgetValues["control_after_generate"] == "randomize"
            ) {
                val newSeed = Random.nextLong(100000000, 9999999999).toString()
                val widgets = node.widgetValues.toMutableMap()
                widgets["seed"] = newSeed
                node.copy(widgetValues = widgets)
            } else {
                node
            }
        }
        val finalWorkflow = workflowToRun.copy(nodes = updatedNodes)
        _currentWorkflow.value = finalWorkflow

        viewModelScope.launch {
            if (_activeJob.value == null) {
                runWorkflow(finalWorkflow)
            } else {
                // Add to queue
                val queuedJob = GenerationJob(
                    id = "queue_${System.currentTimeMillis()}",
                    workflowName = finalWorkflow.name,
                    promptText = finalWorkflow.nodes.find { it.type == "CLIPTextEncode" }?.widgetValues?.get("text") ?: "Generative Task",
                    negativePromptText = "",
                    seed = Random.nextLong(100000000, 9999999999),
                    steps = 20,
                    cfg = 7.0f,
                    samplerName = "euler",
                    modelName = "v1-5-pruned-emaonly.safetensors",
                    width = 512,
                    height = 512,
                    status = JobStatus.QUEUED,
                    workflowSnapshot = finalWorkflow
                )
                _queue.update { it + queuedJob }
            }
        }
    }

    private suspend fun runWorkflow(workflow: Workflow) {
        executionJob = viewModelScope.launch {
            try {
                val isRemote = _serverMode.value == ServerMode.REMOTE_COMFY_SERVER
                val completedJob = engine.executeWorkflow(workflow, isRemote = isRemote) { inProgressJob ->
                    _activeJob.value = inProgressJob
                }
                _history.update { listOf(completedJob) + it }
                if (_currentUser.value != null) {
                    try {
                        firebaseRepo.saveGalleryItem(completedJob)
                    } catch (e: Exception) {
                        Log.e("ComfyVM", "Failed to sync gallery item to Firebase", e)
                    }
                }
                _activeJob.value = null

                // Process next in queue if available
                val nextInQueue = _queue.value.firstOrNull()
                if (nextInQueue != null) {
                    _queue.update { it.drop(1) }
                    nextInQueue.workflowSnapshot?.let { runWorkflow(it) }
                }
            } catch (e: Exception) {
                _activeJob.value = null
            }
        }
    }

    fun saveCurrentWorkflowToCloud(onComplete: (Boolean) -> Unit = {}) {
        val user = _currentUser.value
        if (user == null) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            try {
                firebaseRepo.saveWorkflow(_currentWorkflow.value)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("ComfyVM", "Failed to save workflow to cloud", e)
                onComplete(false)
            }
        }
    }

    fun cancelActiveJob() {
        executionJob?.cancel()
        _activeJob.value = null
    }

    fun clearQueue() {
        _queue.value = emptyList()
    }

    fun removeFromQueue(jobId: String) {
        _queue.update { it.filterNot { job -> job.id == jobId } }
    }

    fun reRunJob(job: GenerationJob) {
        job.workflowSnapshot?.let { snapshot ->
            _currentWorkflow.value = snapshot
            queuePrompt()
        }
    }

    fun queueWorkflowFiles(files: List<File>) {
        viewModelScope.launch {
            for (file in files) {
                try {
                    val content = file.readText()
                    val workflow = WorkflowRepository.importFromJson(content, fallbackName = file.nameWithoutExtension)
                    if (workflow != null) {
                        val promptText = workflow.nodes.find { it.type == "CLIPTextEncode" }?.widgetValues?.get("text")
                            ?: "Batch: ${file.nameWithoutExtension}"
                        val queuedJob = GenerationJob(
                            id = "batch_${System.currentTimeMillis()}_${file.nameWithoutExtension}",
                            workflowName = workflow.name.ifBlank { file.nameWithoutExtension },
                            promptText = promptText,
                            negativePromptText = "",
                            seed = Random.nextLong(100000000, 9999999999),
                            steps = 20,
                            cfg = 7.0f,
                            samplerName = "euler",
                            modelName = "v1-5-pruned-emaonly.safetensors",
                            width = 512,
                            height = 512,
                            status = JobStatus.QUEUED,
                            workflowSnapshot = workflow
                        )
                        if (_activeJob.value == null && _queue.value.isEmpty()) {
                            runWorkflow(workflow)
                        } else {
                            _queue.update { it + queuedJob }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ComfyVM", "Error batch queueing file ${file.name}", e)
                }
            }
        }
    }

    fun zoomIn() {
        _canvasScale.value = (_canvasScale.value * 1.15f).coerceAtMost(2.5f)
    }

    fun zoomOut() {
        _canvasScale.value = (_canvasScale.value / 1.15f).coerceAtLeast(0.4f)
    }

    fun setServerMode(mode: ServerMode) {
        _serverMode.value = mode
    }

    fun setServerUrl(url: String) {
        val clean = url.trim()
        _serverUrl.value = clean
        ComfyRetrofitClient.updateBaseUrl(clean)
        prefs.edit().putString("server_url", clean).apply()
    }

    fun setServerApiKey(key: String) {
        val clean = key.trim()
        _serverApiKey.value = clean
        ComfyRetrofitClient.updateApiKey(clean)
        prefs.edit().putString("server_api_key", clean).apply()
    }

    fun updateServerSettings(url: String, key: String) {
        setServerUrl(url)
        setServerApiKey(key)
        checkServerHealth()
    }

    fun resetServerSettingsToDefault() {
        ComfyRetrofitClient.resetToEnvDefaults()
        val defaultUrl = ComfyRetrofitClient.currentBaseUrl.trimEnd('/')
        val defaultKey = ComfyRetrofitClient.currentApiKey
        _serverUrl.value = defaultUrl
        _serverApiKey.value = defaultKey
        prefs.edit().remove("server_url").remove("server_api_key").apply()
        checkServerHealth()
    }

    fun checkServerConnection() {
        checkServerHealth()
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _serverHealth.value = _serverHealth.value.copy(
                isChecking = true,
                statusMessage = "Connecting via Retrofit..."
            )

            if (_serverMode.value == ServerMode.MOBILE_EMULATION) {
                delay(250)
                val health = ServerHealth(
                    isOnline = true,
                    isChecking = false,
                    statusCode = 200,
                    statusMessage = "Mobile Standalone Engine Ready",
                    latencyMs = 12L,
                    lastCheckedTime = System.currentTimeMillis()
                )
                _serverHealth.value = health
                _isServerConnected.value = true
                return@launch
            }

            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                // Update Retrofit client with active URL and API key
                ComfyRetrofitClient.updateBaseUrl(_serverUrl.value)
                ComfyRetrofitClient.updateApiKey(_serverApiKey.value)

                try {
                    // Call ComfyUI /system_stats endpoint via Retrofit
                    val response = ComfyRetrofitClient.apiService.getSystemStats()
                    val latency = System.currentTimeMillis() - startTime
                    val code = response.code()

                    if (response.isSuccessful && response.body() != null) {
                        val stats = response.body()!!
                        val sys = stats.system
                        val dev = stats.devices?.firstOrNull()
                        val vramStr = if (dev?.vramTotal != null && dev.vramFree != null) {
                            val freeGb = String.format("%.1f", dev.vramFree.toDouble() / (1024 * 1024 * 1024))
                            val totalGb = String.format("%.1f", dev.vramTotal.toDouble() / (1024 * 1024 * 1024))
                            "$freeGb / $totalGb GB Free"
                        } else null

                        _serverHealth.value = ServerHealth(
                            isOnline = true,
                            isChecking = false,
                            statusCode = code,
                            statusMessage = "HTTP 200 (ComfyUI PyTorch ${sys?.pytorchVersion ?: "Ready"})",
                            latencyMs = latency,
                            lastCheckedTime = System.currentTimeMillis(),
                            systemInfo = sys,
                            deviceName = dev?.name ?: "ComfyUI Hardware",
                            vramStats = vramStr
                        )
                        _isServerConnected.value = true
                    } else {
                        // Even if 401/403/404, server is online and reached via Retrofit
                        val isAlive = code in 200..499
                        _serverHealth.value = ServerHealth(
                            isOnline = isAlive,
                            isChecking = false,
                            statusCode = code,
                            statusMessage = "HTTP $code (${response.message().ifBlank { "Responding" }})",
                            latencyMs = latency,
                            lastCheckedTime = System.currentTimeMillis()
                        )
                        _isServerConnected.value = isAlive
                    }
                } catch (e: Exception) {
                    val latency = System.currentTimeMillis() - startTime
                    val errorMsg = e.localizedMessage ?: "Connection Refused"
                    val cleanMsg = when {
                        errorMsg.contains("ECONNREFUSED", true) -> "Server Offline (ECONNREFUSED)"
                        errorMsg.contains("Failed to connect", true) -> "Failed to reach host"
                        errorMsg.contains("timed out", true) -> "Connection Timed Out"
                        else -> errorMsg
                    }
                    _serverHealth.value = ServerHealth(
                        isOnline = false,
                        isChecking = false,
                        statusCode = null,
                        statusMessage = cleanMsg,
                        latencyMs = latency,
                        lastCheckedTime = System.currentTimeMillis()
                    )
                    _isServerConnected.value = false
                }
            }
        }
    }

    private fun seedInitialCacheAndWorkflows() {
        try {
            val context = getApplication<Application>().applicationContext

            // 1. Seed initial cache image if cacheDir has no images
            val cacheFiles = context.cacheDir.listFiles { _, name ->
                name.endsWith(".jpg", true) || name.endsWith(".png", true)
            }
            if (cacheFiles.isNullOrEmpty()) {
                val seedCacheFile = File(context.cacheDir, "cached_cyberpunk_output.jpg")
                context.resources.openRawResource(com.example.comfyui.R.drawable.sample_cyberpunk).use { input ->
                    FileOutputStream(seedCacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // 2. Seed initial workflow_api.json if workflows directory is empty
            val workflowsDir = File(context.filesDir, "workflows")
            if (!workflowsDir.exists()) {
                workflowsDir.mkdirs()
            }
            val defaultApiFile = File(workflowsDir, "workflow_api.json")
            if (!defaultApiFile.exists()) {
                val sdWorkflow = WorkflowRepository.createStandardSDWorkflow()
                val apiJson = WorkflowRepository.exportToWorkflowApiJson(sdWorkflow)
                defaultApiFile.writeText(apiJson)
            }
            val fluxFile = File(workflowsDir, "flux1_dev_api.json")
            if (!fluxFile.exists()) {
                val fluxWorkflow = WorkflowRepository.createFluxDevWorkflow()
                val apiJson = WorkflowRepository.exportToWorkflowApiJson(fluxWorkflow)
                fluxFile.writeText(apiJson)
            }
        } catch (e: Exception) {
            Log.e("ComfyVM", "Error seeding initial cache and workflows", e)
        }
    }

    fun refreshLocalCacheImages() {
        val context = getApplication<Application>().applicationContext
        val list = mutableListOf<LocalCacheImage>()

        // Scan cacheDir
        context.cacheDir.listFiles { file ->
            val ext = file.extension.lowercase()
            ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp"
        }?.forEach { file ->
            list.add(
                LocalCacheImage(
                    file = file,
                    name = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    isFromCacheDir = true
                )
            )
        }

        // Scan filesDir for generated outputs
        context.filesDir.listFiles { file ->
            val ext = file.extension.lowercase()
            (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp") && file.name.startsWith("gen_")
        }?.forEach { file ->
            list.add(
                LocalCacheImage(
                    file = file,
                    name = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    isFromCacheDir = false
                )
            )
        }

        _localCacheImages.value = list.sortedByDescending { it.lastModified }
    }

    fun deleteLocalImage(file: File) {
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("ComfyVM", "Error deleting image file: ${file.name}", e)
        }
        refreshLocalCacheImages()
    }

    fun clearLocalCache() {
        val context = getApplication<Application>().applicationContext
        try {
            context.cacheDir.listFiles { file ->
                val ext = file.extension.lowercase()
                ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp"
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e("ComfyVM", "Error clearing cache directory", e)
        }
        refreshLocalCacheImages()
    }

    fun loadLocalWorkflowFiles() {
        val context = getApplication<Application>().applicationContext
        val workflowsDir = File(context.filesDir, "workflows")
        if (!workflowsDir.exists()) {
            workflowsDir.mkdirs()
        }

        val files = workflowsDir.listFiles { file ->
            file.extension.equals("json", ignoreCase = true)
        } ?: emptyArray()

        val list = files.map { file ->
            var nodeCount = 0
            try {
                val content = file.readText()
                val json = JSONObject(content)
                nodeCount = if (json.has("nodes")) {
                    json.getJSONArray("nodes").length()
                } else {
                    json.length()
                }
            } catch (_: Exception) {}

            LocalWorkflowFile(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                nodeCount = nodeCount
            )
        }.sortedByDescending { it.lastModified }

        _localWorkflowFiles.value = list
    }

    fun saveWorkflowToFile(fileName: String, workflow: Workflow): Boolean {
        return try {
            val context = getApplication<Application>().applicationContext
            val workflowsDir = File(context.filesDir, "workflows")
            if (!workflowsDir.exists()) {
                workflowsDir.mkdirs()
            }

            val sanitizedName = if (fileName.endsWith(".json", ignoreCase = true)) fileName else "$fileName.json"
            val targetFile = File(workflowsDir, sanitizedName)

            // Export in ComfyUI workflow_api.json format
            val jsonContent = WorkflowRepository.exportToWorkflowApiJson(workflow)
            targetFile.writeText(jsonContent)

            loadLocalWorkflowFiles()
            true
        } catch (e: Exception) {
            Log.e("ComfyVM", "Error saving workflow to file $fileName", e)
            false
        }
    }

    fun validateWorkflowFile(file: File): WorkflowValidationResult {
        return try {
            val content = file.readText()
            WorkflowSchemaValidator.validate(content)
        } catch (e: Exception) {
            WorkflowValidationResult(
                isValid = false,
                format = WorkflowFormat.UNKNOWN,
                totalNodes = 0,
                detectedNodeTypes = emptySet(),
                hasModelLoader = false,
                hasSampler = false,
                hasConditioning = false,
                hasLatentOrOutput = false,
                missingEssentialTypes = emptyList(),
                warnings = emptyList(),
                errorMessage = "Cannot read file: ${e.localizedMessage}"
            )
        }
    }

    fun loadWorkflowFromFile(file: File): Workflow? {
        return try {
            val content = file.readText()
            val parsed = WorkflowRepository.importFromJson(content, fallbackName = file.nameWithoutExtension)
            if (parsed != null) {
                _currentWorkflow.value = parsed
                _selectedNodeId.value = null
                resetCanvasTransform()
            }
            parsed
        } catch (e: Exception) {
            Log.e("ComfyVM", "Error reading workflow from file ${file.name}", e)
            null
        }
    }

    fun deleteWorkflowFile(file: File): Boolean {
        return try {
            val deleted = file.delete()
            loadLocalWorkflowFiles()
            deleted
        } catch (e: Exception) {
            Log.e("ComfyVM", "Error deleting workflow file ${file.name}", e)
            false
        }
    }

    fun restoreFromHistory(job: GenerationJob) {
        job.workflowSnapshot?.let {
            _currentWorkflow.value = it
            _selectedNodeId.value = null
            resetCanvasTransform()
        }
    }
}
