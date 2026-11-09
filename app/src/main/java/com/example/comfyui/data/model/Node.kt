package com.example.comfyui.data.model

import androidx.compose.ui.graphics.Color
import com.example.comfyui.ui.theme.*

enum class SocketType(val displayName: String, val color: Color) {
    MODEL("MODEL", SocketModelColor),
    CLIP("CLIP", SocketClipColor),
    CONDITIONING("CONDITIONING", SocketConditioningColor),
    LATENT("LATENT", SocketLatentColor),
    VAE("VAE", SocketVaeColor),
    IMAGE("IMAGE", SocketImageColor),
    MASK("MASK", SocketMaskColor),
    STRING("STRING", PrimaryNeon),
    INT("INT", SecondaryOrange),
    FLOAT("FLOAT", AccentRose)
}

enum class NodeCategory(val title: String, val headerColor: Color) {
    LOADER("Loaders", NodeHeaderLoader),
    CONDITIONING("Conditioning", NodeHeaderPrompt),
    LATENT("Latent & Math", NodeHeaderLatent),
    SAMPLER("Sampling", NodeHeaderSampler),
    IMAGE("Image & Output", NodeHeaderOutput),
    UPSCALE("Upscaling", NodeHeaderUpscale),
    CONTROL("Control & Mask", NodeHeaderControl),
    POST_PROCESS("Post Processing", NodeHeaderUpscale)
}

data class NodeSocket(
    val name: String,
    val type: SocketType,
    val isInput: Boolean
)

data class Node(
    val id: Int,
    val type: String,
    val title: String,
    val category: NodeCategory,
    val positionX: Float,
    val positionY: Float,
    val inputs: List<NodeSocket> = emptyList(),
    val outputs: List<NodeSocket> = emptyList(),
    val widgetValues: Map<String, String> = emptyMap(),
    val isBypassed: Boolean = false,
    val isMuted: Boolean = false,
    val isCollapsed: Boolean = false
)

data class ConnectionLink(
    val id: Int,
    val fromNodeId: Int,
    val fromSocketIndex: Int,
    val toNodeId: Int,
    val toSocketIndex: Int,
    val type: SocketType
)
