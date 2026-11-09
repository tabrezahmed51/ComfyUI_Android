package com.example.comfyui.data.model

data class Blueprint(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val tags: List<String>,
    val nodeCount: Int,
    val recommendedModel: String,
    val workflowFactory: () -> Workflow
)
