package com.example.comfyui.data.model

enum class JobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class GenerationJob(
    val id: String,
    val workflowName: String,
    val promptText: String,
    val negativePromptText: String,
    val seed: Long,
    val steps: Int,
    val cfg: Float,
    val samplerName: String,
    val modelName: String,
    val width: Int,
    val height: Int,
    val status: JobStatus = JobStatus.QUEUED,
    val currentNodeTitle: String = "Initializing",
    val currentStep: Int = 0,
    val totalSteps: Int = 20,
    val progress: Float = 0f,
    val outputImageUrl: String? = null,
    val durationSeconds: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val workflowSnapshot: Workflow? = null
)
