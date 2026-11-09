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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

enum class ServerMode {
    MOBILE_EMULATION,
    REMOTE_COMFY_SERVER
}

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

    private val _serverMode = MutableStateFlow(ServerMode.MOBILE_EMULATION)
    val serverMode: StateFlow<ServerMode> = _serverMode.asStateFlow()

    private val _serverUrl = MutableStateFlow("http://127.0.0.1:8188")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _serverApiKey = MutableStateFlow(
        if (com.example.comfyui.BuildConfig.COMFY_UI_SERVER_KEY != "none") com.example.comfyui.BuildConfig.COMFY_UI_SERVER_KEY else ""
    )
    val serverApiKey: StateFlow<String> = _serverApiKey.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val firebaseRepo = FirebaseRepository.create(application.applicationContext)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _cloudWorkflows = MutableStateFlow<List<FirestoreWorkflow>>(emptyList())
    val cloudWorkflows: StateFlow<List<FirestoreWorkflow>> = _cloudWorkflows.asStateFlow()

    private var executionJob: Job? = null

    init {
        seedInitialHistory()
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
                val completedJob = engine.executeWorkflow(workflow) { inProgressJob ->
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

    fun setServerMode(mode: ServerMode) {
        _serverMode.value = mode
    }

    fun setServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun setServerApiKey(key: String) {
        _serverApiKey.value = key.trim()
    }

    fun checkServerConnection() {
        viewModelScope.launch {
            // Emulate or probe connection
            _isServerConnected.value = false
            kotlinx.coroutines.delay(400)
            _isServerConnected.value = true
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
