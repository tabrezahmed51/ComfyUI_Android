package com.example.comfyui.data.model

import org.json.JSONObject

enum class WorkflowFormat(val displayName: String) {
    COMFY_GRAPH("ComfyUI Web Graph (nodes/links)"),
    COMFY_API_PROMPT("ComfyUI API Prompt (workflow_api.json)"),
    UNKNOWN("Unknown / Unrecognized Schema")
}

data class WorkflowValidationResult(
    val isValid: Boolean,
    val format: WorkflowFormat,
    val totalNodes: Int,
    val detectedNodeTypes: Set<String>,
    val hasModelLoader: Boolean,
    val hasSampler: Boolean,
    val hasConditioning: Boolean,
    val hasLatentOrOutput: Boolean,
    val missingEssentialTypes: List<String>,
    val warnings: List<String>,
    val errorMessage: String? = null
)

object WorkflowSchemaValidator {

    private val MODEL_LOADER_PATTERNS = listOf(
        "CheckpointLoader", "UNETLoader", "DiffusersLoader", "DualCLIPLoader",
        "VAELoader", "LoraLoader", "Loader"
    )

    private val SAMPLER_PATTERNS = listOf(
        "KSampler", "SamplerCustom", "KSamplerAdvanced", "KSamplerSelect", "Diffusion"
    )

    private val CONDITIONING_PATTERNS = listOf(
        "CLIPTextEncode", "DualCLIPLoader", "FluxGuidance", "Conditioning", "Prompt"
    )

    private val LATENT_OUTPUT_PATTERNS = listOf(
        "EmptyLatentImage", "VAEDecode", "VAEEncode", "SaveImage", "PreviewImage",
        "UltimateSDUpscale", "Latent", "Image"
    )

    /**
     * Validates an imported workflow_api.json or ComfyUI graph export string.
     * Verifies JSON syntax, recognizes node types, and checks for required ComfyUI
     * diffusion pipeline components (Model Loader, Sampler, Conditioning, Latent/Output).
     */
    fun validate(jsonStr: String): WorkflowValidationResult {
        if (jsonStr.isBlank()) {
            return WorkflowValidationResult(
                isValid = false,
                format = WorkflowFormat.UNKNOWN,
                totalNodes = 0,
                detectedNodeTypes = emptySet(),
                hasModelLoader = false,
                hasSampler = false,
                hasConditioning = false,
                hasLatentOrOutput = false,
                missingEssentialTypes = listOf("Model Loader", "Diffusion Sampler", "Conditioning", "Latent / Output"),
                warnings = emptyList(),
                errorMessage = "The workflow JSON file is empty."
            )
        }

        return try {
            val root = JSONObject(jsonStr)
            val detectedTypes = mutableSetOf<String>()
            val format: WorkflowFormat

            if (root.has("nodes")) {
                format = WorkflowFormat.COMFY_GRAPH
                val nodesArray = root.getJSONArray("nodes")
                for (i in 0 until nodesArray.length()) {
                    val nObj = nodesArray.getJSONObject(i)
                    val type = nObj.optString("type", "")
                    if (type.isNotBlank()) detectedTypes.add(type)
                }
            } else {
                format = WorkflowFormat.COMFY_API_PROMPT
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val nObj = root.optJSONObject(key)
                    if (nObj != null) {
                        val classType = nObj.optString("class_type", "")
                        if (classType.isNotBlank()) detectedTypes.add(classType)
                    }
                }
            }

            if (detectedTypes.isEmpty()) {
                return WorkflowValidationResult(
                    isValid = false,
                    format = format,
                    totalNodes = 0,
                    detectedNodeTypes = emptySet(),
                    hasModelLoader = false,
                    hasSampler = false,
                    hasConditioning = false,
                    hasLatentOrOutput = false,
                    missingEssentialTypes = listOf("No ComfyUI node definitions found"),
                    warnings = listOf("File does not contain valid ComfyUI node keys or array elements."),
                    errorMessage = "No recognizable ComfyUI node definitions found in file."
                )
            }

            val hasModelLoader = detectedTypes.any { type ->
                MODEL_LOADER_PATTERNS.any { pattern -> type.contains(pattern, ignoreCase = true) }
            }

            val hasSampler = detectedTypes.any { type ->
                SAMPLER_PATTERNS.any { pattern -> type.contains(pattern, ignoreCase = true) }
            }

            val hasConditioning = detectedTypes.any { type ->
                CONDITIONING_PATTERNS.any { pattern -> type.contains(pattern, ignoreCase = true) }
            }

            val hasLatentOrOutput = detectedTypes.any { type ->
                LATENT_OUTPUT_PATTERNS.any { pattern -> type.contains(pattern, ignoreCase = true) }
            }

            val missing = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            if (!hasModelLoader) {
                missing.add("Model Loader (CheckpointLoaderSimple / UNETLoader)")
                warnings.add("Missing Model Checkpoint or UNET Loader.")
            }
            if (!hasSampler) {
                missing.add("Diffusion Sampler (KSampler / KSamplerAdvanced)")
                warnings.add("Missing KSampler diffusion engine.")
            }
            if (!hasConditioning) {
                missing.add("Conditioning (CLIPTextEncode Prompt)")
                warnings.add("Missing text prompt conditioning node.")
            }
            if (!hasLatentOrOutput) {
                missing.add("Latent / Image Output (EmptyLatentImage / SaveImage)")
                warnings.add("Missing latent source or image output node.")
            }

            val isValid = missing.isEmpty()

            WorkflowValidationResult(
                isValid = isValid,
                format = format,
                totalNodes = detectedTypes.size,
                detectedNodeTypes = detectedTypes,
                hasModelLoader = hasModelLoader,
                hasSampler = hasSampler,
                hasConditioning = hasConditioning,
                hasLatentOrOutput = hasLatentOrOutput,
                missingEssentialTypes = missing,
                warnings = warnings,
                errorMessage = if (!isValid) "Missing ${missing.size} required node type(s) for a functional ComfyUI diffusion pipeline." else null
            )
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
                errorMessage = "Invalid JSON syntax: ${e.localizedMessage}"
            )
        }
    }
}
