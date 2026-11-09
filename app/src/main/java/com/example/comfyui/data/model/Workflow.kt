package com.example.comfyui.data.model

data class Workflow(
    val id: String,
    val name: String,
    val description: String = "",
    val nodes: List<Node> = emptyList(),
    val links: List<ConnectionLink> = emptyList(),
    val lastModified: Long = System.currentTimeMillis()
)
