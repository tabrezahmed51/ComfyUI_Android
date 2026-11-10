package com.example.comfyui.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Models representing ComfyUI REST API request and response schemas.
 */

data class SystemStatsResponse(
    @SerializedName("system") val system: SystemInfo? = null,
    @SerializedName("devices") val devices: List<DeviceInfo>? = null
)

data class SystemInfo(
    @SerializedName("os") val os: String? = null,
    @SerializedName("python_version") val pythonVersion: String? = null,
    @SerializedName("pytorch_version") val pytorchVersion: String? = null,
    @SerializedName("embedded_python") val embeddedPython: Boolean? = null
)

data class DeviceInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("index") val index: Int? = null,
    @SerializedName("vram_total") val vramTotal: Long? = null,
    @SerializedName("vram_free") val vramFree: Long? = null,
    @SerializedName("torch_vram_total") val torchVramTotal: Long? = null,
    @SerializedName("torch_vram_free") val torchVramFree: Long? = null
)

data class QueueStatusResponse(
    @SerializedName("queue_running") val queueRunning: List<Any>? = null,
    @SerializedName("queue_pending") val queuePending: List<Any>? = null
)

data class PromptResponse(
    @SerializedName("prompt_id") val promptId: String? = null,
    @SerializedName("number") val number: Int? = null,
    @SerializedName("node_errors") val nodeErrors: Map<String, Any>? = null,
    @SerializedName("error") val error: Any? = null
)
