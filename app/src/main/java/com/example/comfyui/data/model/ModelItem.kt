package com.example.comfyui.data.model

enum class ModelType {
    CHECKPOINT,
    LORA,
    VAE,
    CONTROLNET,
    UPSCALE
}

data class ModelItem(
    val id: String,
    val name: String,
    val type: ModelType,
    val architecture: String, // "SD 1.5", "SDXL", "Flux.1", "Wan 2.1"
    val filename: String,
    val size: String,
    val precision: String, // "fp16", "bf16", "q8_0", "q4_k_s"
    val isInstalled: Boolean = true,
    val description: String = ""
)
