package com.example.comfyui.data.repository

import com.example.comfyui.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object WorkflowRepository {

    fun createStandardSDWorkflow(): Workflow {
        val nodes = listOf(
            Node(
                id = 1,
                type = "CheckpointLoaderSimple",
                title = "Load Checkpoint",
                category = NodeCategory.LOADER,
                positionX = 60f,
                positionY = 100f,
                outputs = listOf(
                    NodeSocket("MODEL", SocketType.MODEL, false),
                    NodeSocket("CLIP", SocketType.CLIP, false),
                    NodeSocket("VAE", SocketType.VAE, false)
                ),
                widgetValues = mapOf("ckpt_name" to "v1-5-pruned-emaonly.safetensors")
            ),
            Node(
                id = 2,
                type = "CLIPTextEncode",
                title = "CLIP Text Encode (Positive)",
                category = NodeCategory.CONDITIONING,
                positionX = 400f,
                positionY = 40f,
                inputs = listOf(NodeSocket("clip", SocketType.CLIP, true)),
                outputs = listOf(NodeSocket("CONDITIONING", SocketType.CONDITIONING, false)),
                widgetValues = mapOf("text" to "a majestic cyberpunk cityscape at twilight, glowing cyan neon signs, rain-slicked reflective streets, high detail, masterpiece, 8k")
            ),
            Node(
                id = 3,
                type = "CLIPTextEncode",
                title = "CLIP Text Encode (Negative)",
                category = NodeCategory.CONDITIONING,
                positionX = 400f,
                positionY = 320f,
                inputs = listOf(NodeSocket("clip", SocketType.CLIP, true)),
                outputs = listOf(NodeSocket("CONDITIONING", SocketType.CONDITIONING, false)),
                widgetValues = mapOf("text" to "blurry, low quality, deformed, duplicate, bad anatomy, watermark, text")
            ),
            Node(
                id = 4,
                type = "EmptyLatentImage",
                title = "Empty Latent Image",
                category = NodeCategory.LATENT,
                positionX = 400f,
                positionY = 580f,
                outputs = listOf(NodeSocket("LATENT", SocketType.LATENT, false)),
                widgetValues = mapOf(
                    "width" to "512",
                    "height" to "512",
                    "batch_size" to "1"
                )
            ),
            Node(
                id = 5,
                type = "KSampler",
                title = "KSampler",
                category = NodeCategory.SAMPLER,
                positionX = 820f,
                positionY = 160f,
                inputs = listOf(
                    NodeSocket("model", SocketType.MODEL, true),
                    NodeSocket("positive", SocketType.CONDITIONING, true),
                    NodeSocket("negative", SocketType.CONDITIONING, true),
                    NodeSocket("latent_image", SocketType.LATENT, true)
                ),
                outputs = listOf(NodeSocket("LATENT", SocketType.LATENT, false)),
                widgetValues = mapOf(
                    "seed" to "4820194827",
                    "control_after_generate" to "randomize",
                    "steps" to "20",
                    "cfg" to "7.0",
                    "sampler_name" to "euler",
                    "scheduler" to "normal",
                    "denoise" to "1.00"
                )
            ),
            Node(
                id = 6,
                type = "VAEDecode",
                title = "VAE Decode",
                category = NodeCategory.LATENT,
                positionX = 1180f,
                positionY = 200f,
                inputs = listOf(
                    NodeSocket("samples", SocketType.LATENT, true),
                    NodeSocket("vae", SocketType.VAE, true)
                ),
                outputs = listOf(NodeSocket("IMAGE", SocketType.IMAGE, false))
            ),
            Node(
                id = 7,
                type = "SaveImage",
                title = "Save Image",
                category = NodeCategory.IMAGE,
                positionX = 1460f,
                positionY = 180f,
                inputs = listOf(NodeSocket("images", SocketType.IMAGE, true)),
                widgetValues = mapOf("filename_prefix" to "ComfyUI")
            )
        )

        val links = listOf(
            ConnectionLink(1, 1, 0, 5, 0, SocketType.MODEL),
            ConnectionLink(2, 1, 1, 2, 0, SocketType.CLIP),
            ConnectionLink(3, 1, 1, 3, 0, SocketType.CLIP),
            ConnectionLink(4, 2, 0, 5, 1, SocketType.CONDITIONING),
            ConnectionLink(5, 3, 0, 5, 2, SocketType.CONDITIONING),
            ConnectionLink(6, 4, 0, 5, 3, SocketType.LATENT),
            ConnectionLink(7, 5, 0, 6, 0, SocketType.LATENT),
            ConnectionLink(8, 1, 2, 6, 1, SocketType.VAE),
            ConnectionLink(9, 6, 0, 7, 0, SocketType.IMAGE)
        )

        return Workflow(
            id = "sd15_default",
            name = "Text to Image (Standard SD 1.5)",
            description = "Standard 512x512 text to image diffusion pipeline with KSampler and VAE Decode",
            nodes = nodes,
            links = links
        )
    }

    fun createFluxDevWorkflow(): Workflow {
        val nodes = listOf(
            Node(
                id = 1,
                type = "UNETLoader",
                title = "Load Diffusion Model",
                category = NodeCategory.LOADER,
                positionX = 60f,
                positionY = 80f,
                outputs = listOf(NodeSocket("MODEL", SocketType.MODEL, false)),
                widgetValues = mapOf("unet_name" to "flux1-dev.safetensors", "weight_dtype" to "default")
            ),
            Node(
                id = 2,
                type = "DualCLIPLoader",
                title = "DualCLIPLoader",
                category = NodeCategory.LOADER,
                positionX = 60f,
                positionY = 320f,
                outputs = listOf(NodeSocket("CLIP", SocketType.CLIP, false)),
                widgetValues = mapOf("clip_name1" to "t5xxl_fp16.safetensors", "clip_name2" to "clip_l.safetensors", "type" to "flux")
            ),
            Node(
                id = 3,
                type = "VAELoader",
                title = "Load VAE",
                category = NodeCategory.LOADER,
                positionX = 60f,
                positionY = 560f,
                outputs = listOf(NodeSocket("VAE", SocketType.VAE, false)),
                widgetValues = mapOf("vae_name" to "ae.safetensors")
            ),
            Node(
                id = 4,
                type = "CLIPTextEncode",
                title = "CLIP Text Encode (Flux Prompt)",
                category = NodeCategory.CONDITIONING,
                positionX = 420f,
                positionY = 100f,
                inputs = listOf(NodeSocket("clip", SocketType.CLIP, true)),
                outputs = listOf(NodeSocket("CONDITIONING", SocketType.CONDITIONING, false)),
                widgetValues = mapOf("text" to "close up cinematic portrait of a robotic fox with crystalline fur and golden eyes, forest background, photorealistic, shallow depth of field, 35mm photograph")
            ),
            Node(
                id = 5,
                type = "FluxGuidance",
                title = "Flux Guidance",
                category = NodeCategory.CONDITIONING,
                positionX = 760f,
                positionY = 100f,
                inputs = listOf(NodeSocket("conditioning", SocketType.CONDITIONING, true)),
                outputs = listOf(NodeSocket("CONDITIONING", SocketType.CONDITIONING, false)),
                widgetValues = mapOf("guidance" to "3.5")
            ),
            Node(
                id = 6,
                type = "EmptyLatentImage",
                title = "Empty Latent (Flux 1024x1024)",
                category = NodeCategory.LATENT,
                positionX = 420f,
                positionY = 460f,
                outputs = listOf(NodeSocket("LATENT", SocketType.LATENT, false)),
                widgetValues = mapOf("width" to "1024", "height" to "1024", "batch_size" to "1")
            ),
            Node(
                id = 7,
                type = "KSampler",
                title = "KSampler (Flux Dev)",
                category = NodeCategory.SAMPLER,
                positionX = 1060f,
                positionY = 180f,
                inputs = listOf(
                    NodeSocket("model", SocketType.MODEL, true),
                    NodeSocket("positive", SocketType.CONDITIONING, true),
                    NodeSocket("negative", SocketType.CONDITIONING, true),
                    NodeSocket("latent_image", SocketType.LATENT, true)
                ),
                outputs = listOf(NodeSocket("LATENT", SocketType.LATENT, false)),
                widgetValues = mapOf(
                    "seed" to "9182374612",
                    "steps" to "24",
                    "cfg" to "1.0",
                    "sampler_name" to "euler",
                    "scheduler" to "simple",
                    "denoise" to "1.00"
                )
            ),
            Node(
                id = 8,
                type = "VAEDecode",
                title = "VAE Decode",
                category = NodeCategory.LATENT,
                positionX = 1420f,
                positionY = 240f,
                inputs = listOf(
                    NodeSocket("samples", SocketType.LATENT, true),
                    NodeSocket("vae", SocketType.VAE, true)
                ),
                outputs = listOf(NodeSocket("IMAGE", SocketType.IMAGE, false))
            ),
            Node(
                id = 9,
                type = "SaveImage",
                title = "Save Image",
                category = NodeCategory.IMAGE,
                positionX = 1700f,
                positionY = 220f,
                inputs = listOf(NodeSocket("images", SocketType.IMAGE, true)),
                widgetValues = mapOf("filename_prefix" to "Flux_Dev")
            )
        )

        val links = listOf(
            ConnectionLink(1, 1, 0, 7, 0, SocketType.MODEL),
            ConnectionLink(2, 2, 0, 4, 0, SocketType.CLIP),
            ConnectionLink(3, 4, 0, 5, 0, SocketType.CONDITIONING),
            ConnectionLink(4, 5, 0, 7, 1, SocketType.CONDITIONING),
            ConnectionLink(5, 6, 0, 7, 3, SocketType.LATENT),
            ConnectionLink(6, 7, 0, 8, 0, SocketType.LATENT),
            ConnectionLink(7, 3, 0, 8, 1, SocketType.VAE),
            ConnectionLink(8, 8, 0, 9, 0, SocketType.IMAGE)
        )

        return Workflow(
            id = "flux_dev",
            name = "Text to Image (Flux.1 Dev)",
            description = "State-of-the-art 12B parameter flow matching model with DualCLIP and Flux Guidance",
            nodes = nodes,
            links = links
        )
    }

    fun createTurboWorkflow(): Workflow {
        val nodes = listOf(
            Node(
                id = 1,
                type = "CheckpointLoaderSimple",
                title = "Load Turbo Checkpoint",
                category = NodeCategory.LOADER,
                positionX = 60f,
                positionY = 120f,
                outputs = listOf(
                    NodeSocket("MODEL", SocketType.MODEL, false),
                    NodeSocket("CLIP", SocketType.CLIP, false),
                    NodeSocket("VAE", SocketType.VAE, false)
                ),
                widgetValues = mapOf("ckpt_name" to "sd_xl_turbo_1.0_fp16.safetensors")
            ),
            Node(
                id = 2,
                type = "CLIPTextEncode",
                title = "Prompt",
                category = NodeCategory.CONDITIONING,
                positionX = 400f,
                positionY = 80f,
                inputs = listOf(NodeSocket("clip", SocketType.CLIP, true)),
                outputs = listOf(NodeSocket("CONDITIONING", SocketType.CONDITIONING, false)),
                widgetValues = mapOf("text" to "vibrant digital art of a futuristic hovercraft speeding through holographic canyon, volumetric lighting, unreal engine 5")
            ),
            Node(
                id = 3,
                type = "EmptyLatentImage",
                title = "Latent (512x512)",
                category = NodeCategory.LATENT,
                positionX = 400f,
                positionY = 400f,
                outputs = listOf(NodeSocket("LATENT", SocketType.LATENT, false)),
                widgetValues = mapOf("width" to "512", "height" to "512", "batch_size" to "1")
            ),
            Node(
                id = 4,
                type = "KSampler",
                title = "KSampler (1-4 Steps)",
                category = NodeCategory.SAMPLER,
                positionX = 800f,
                positionY = 160f,
                inputs = listOf(
                    NodeSocket("model", SocketType.MODEL, true),
                    NodeSocket("positive", SocketType.CONDITIONING, true),
                    NodeSocket("negative", SocketType.CONDITIONING, true),
                    NodeSocket("latent_image", SocketType.LATENT, true)
                ),
                outputs = listOf(NodeSocket("LATENT", SocketType.LATENT, false)),
                widgetValues = mapOf(
                    "seed" to "331827401",
                    "steps" to "4",
                    "cfg" to "1.0",
                    "sampler_name" to "euler_ancestral",
                    "scheduler" to "karras",
                    "denoise" to "1.00"
                )
            ),
            Node(
                id = 5,
                type = "VAEDecode",
                title = "VAE Decode",
                category = NodeCategory.LATENT,
                positionX = 1160f,
                positionY = 180f,
                inputs = listOf(
                    NodeSocket("samples", SocketType.LATENT, true),
                    NodeSocket("vae", SocketType.VAE, true)
                ),
                outputs = listOf(NodeSocket("IMAGE", SocketType.IMAGE, false))
            ),
            Node(
                id = 6,
                type = "SaveImage",
                title = "Save Image",
                category = NodeCategory.IMAGE,
                positionX = 1440f,
                positionY = 160f,
                inputs = listOf(NodeSocket("images", SocketType.IMAGE, true)),
                widgetValues = mapOf("filename_prefix" to "Turbo")
            )
        )

        val links = listOf(
            ConnectionLink(1, 1, 0, 4, 0, SocketType.MODEL),
            ConnectionLink(2, 1, 1, 2, 0, SocketType.CLIP),
            ConnectionLink(3, 2, 0, 4, 1, SocketType.CONDITIONING),
            ConnectionLink(4, 3, 0, 4, 3, SocketType.LATENT),
            ConnectionLink(5, 4, 0, 5, 0, SocketType.LATENT),
            ConnectionLink(6, 1, 2, 5, 1, SocketType.VAE),
            ConnectionLink(7, 5, 0, 6, 0, SocketType.IMAGE)
        )

        return Workflow(
            id = "turbo_fast",
            name = "Text to Image (Z-Image-Turbo)",
            description = "Ultra-fast adversarial diffusion model generating quality images in just 4 steps",
            nodes = nodes,
            links = links
        )
    }

    fun createUpscaleWorkflow(): Workflow {
        val nodes = listOf(
            Node(
                id = 1,
                type = "UpscaleModelLoader",
                title = "Load Upscale Model",
                category = NodeCategory.UPSCALE,
                positionX = 60f,
                positionY = 100f,
                outputs = listOf(NodeSocket("UPSCALE_MODEL", SocketType.MODEL, false)),
                widgetValues = mapOf("model_name" to "4x-UltraSharp.pth")
            ),
            Node(
                id = 2,
                type = "LoadImage",
                title = "Load Image",
                category = NodeCategory.LOADER,
                positionX = 60f,
                positionY = 340f,
                outputs = listOf(
                    NodeSocket("IMAGE", SocketType.IMAGE, false),
                    NodeSocket("MASK", SocketType.MASK, false)
                ),
                widgetValues = mapOf("image" to "input.png")
            ),
            Node(
                id = 3,
                type = "ImageUpscaleWithModel",
                title = "Upscale Image (with Model)",
                category = NodeCategory.UPSCALE,
                positionX = 420f,
                positionY = 180f,
                inputs = listOf(
                    NodeSocket("upscale_model", SocketType.MODEL, true),
                    NodeSocket("image", SocketType.IMAGE, true)
                ),
                outputs = listOf(NodeSocket("IMAGE", SocketType.IMAGE, false))
            ),
            Node(
                id = 4,
                type = "SaveImage",
                title = "Save 4x Image",
                category = NodeCategory.IMAGE,
                positionX = 780f,
                positionY = 180f,
                inputs = listOf(NodeSocket("images", SocketType.IMAGE, true)),
                widgetValues = mapOf("filename_prefix" to "Upscale_4x")
            )
        )

        val links = listOf(
            ConnectionLink(1, 1, 0, 3, 0, SocketType.MODEL),
            ConnectionLink(2, 2, 0, 3, 1, SocketType.IMAGE),
            ConnectionLink(3, 3, 0, 4, 0, SocketType.IMAGE)
        )

        return Workflow(
            id = "upscale_ultrasharp",
            name = "Image Upscale (UltraSharp 4x)",
            description = "Upscales an existing image by 4x using UltraSharp ESRGAN neural network weights",
            nodes = nodes,
            links = links
        )
    }

    fun createBgRemovalWorkflow(): Workflow {
        val nodes = listOf(
            Node(
                id = 1,
                type = "LoadImage",
                title = "Load Image",
                category = NodeCategory.LOADER,
                positionX = 60f,
                positionY = 120f,
                outputs = listOf(
                    NodeSocket("IMAGE", SocketType.IMAGE, false),
                    NodeSocket("MASK", SocketType.MASK, false)
                ),
                widgetValues = mapOf("image" to "portrait_input.png")
            ),
            Node(
                id = 2,
                type = "BiRefNet_RemoveBackground",
                title = "Remove Background (BiRefNet)",
                category = NodeCategory.POST_PROCESS,
                positionX = 420f,
                positionY = 120f,
                inputs = listOf(NodeSocket("image", SocketType.IMAGE, true)),
                outputs = listOf(
                    NodeSocket("IMAGE", SocketType.IMAGE, false),
                    NodeSocket("MASK", SocketType.MASK, false)
                ),
                widgetValues = mapOf("model" to "BiRefNet-general-epoch_244.pth", "threshold" to "0.5")
            ),
            Node(
                id = 3,
                type = "SaveImage",
                title = "Save Transparent Image",
                category = NodeCategory.IMAGE,
                positionX = 800f,
                positionY = 120f,
                inputs = listOf(NodeSocket("images", SocketType.IMAGE, true)),
                widgetValues = mapOf("filename_prefix" to "BiRefNet_Cutout")
            )
        )

        val links = listOf(
            ConnectionLink(1, 1, 0, 2, 0, SocketType.IMAGE),
            ConnectionLink(2, 2, 0, 3, 0, SocketType.IMAGE)
        )

        return Workflow(
            id = "birefnet_bg_remove",
            name = "Remove Background (BiRefNet)",
            description = "High-resolution bilateral reference network for crisp, pixel-perfect alpha background removal",
            nodes = nodes,
            links = links
        )
    }

    fun getAllBlueprints(): List<Blueprint> {
        return listOf(
            Blueprint(
                id = "sd15_default",
                title = "Text to Image (Standard SD 1.5)",
                category = "Generation",
                description = "Classic Stable Diffusion text-to-image workflow. Highly customizable, supports LoRA and ControlNet.",
                tags = listOf("SD 1.5", "Txt2Img", "Beginner"),
                nodeCount = 7,
                recommendedModel = "v1-5-pruned-emaonly.safetensors",
                workflowFactory = { createStandardSDWorkflow() }
            ),
            Blueprint(
                id = "flux_dev",
                title = "Text to Image (Flux.1 Dev)",
                category = "State of the Art",
                description = "Black Forest Labs Flux.1 12B parameter flow matching diffusion pipeline with Dual CLIP (T5XXL + CLIP-L).",
                tags = listOf("Flux.1", "12B", "DiT", "High-Res"),
                nodeCount = 9,
                recommendedModel = "flux1-dev.safetensors",
                workflowFactory = { createFluxDevWorkflow() }
            ),
            Blueprint(
                id = "turbo_fast",
                title = "Text to Image (Z-Image-Turbo)",
                category = "Real-Time",
                description = "Ultra-fast adversarial diffusion model designed to generate complete images in 1-4 inference steps.",
                tags = listOf("Turbo", "Fast", "4-Steps"),
                nodeCount = 6,
                recommendedModel = "sd_xl_turbo_1.0_fp16.safetensors",
                workflowFactory = { createTurboWorkflow() }
            ),
            Blueprint(
                id = "upscale_ultrasharp",
                title = "Image Upscale (UltraSharp 4x)",
                category = "Enhancement",
                description = "Deep learning super-resolution upscale workflow restoring textures and clarity by 400%.",
                tags = listOf("Upscale", "ESRGAN", "Super-Res"),
                nodeCount = 4,
                recommendedModel = "4x-UltraSharp.pth",
                workflowFactory = { createUpscaleWorkflow() }
            ),
            Blueprint(
                id = "birefnet_bg_remove",
                title = "Remove Background (BiRefNet)",
                category = "Segmentation",
                description = "Bilateral reference network model for instantaneous and pristine background cutout extraction.",
                tags = listOf("Cutout", "BiRefNet", "Alpha"),
                nodeCount = 3,
                recommendedModel = "BiRefNet-general.pth",
                workflowFactory = { createBgRemovalWorkflow() }
            )
        )
    }

    fun getAllModels(): List<ModelItem> {
        return listOf(
            ModelItem(
                id = "m1",
                name = "v1-5-pruned-emaonly",
                type = ModelType.CHECKPOINT,
                architecture = "SD 1.5",
                filename = "v1-5-pruned-emaonly.safetensors",
                size = "4.27 GB",
                precision = "fp16",
                description = "Standard Stable Diffusion base model"
            ),
            ModelItem(
                id = "m2",
                name = "flux1-dev",
                type = ModelType.CHECKPOINT,
                architecture = "Flux.1",
                filename = "flux1-dev.safetensors",
                size = "11.9 GB",
                precision = "bf16",
                description = "Flagship 12B rectified flow transformer"
            ),
            ModelItem(
                id = "m3",
                name = "sd_xl_base_1.0",
                type = ModelType.CHECKPOINT,
                architecture = "SDXL",
                filename = "sd_xl_base_1.0.safetensors",
                size = "6.94 GB",
                precision = "fp16",
                description = "High-definition 1024x1024 native architecture"
            ),
            ModelItem(
                id = "m4",
                name = "sd_xl_turbo_1.0",
                type = ModelType.CHECKPOINT,
                architecture = "SDXL Turbo",
                filename = "sd_xl_turbo_1.0_fp16.safetensors",
                size = "6.94 GB",
                precision = "fp16",
                description = "Single-step adversarial diffusion model"
            ),
            ModelItem(
                id = "m5",
                name = "4x-UltraSharp",
                type = ModelType.UPSCALE,
                architecture = "ESRGAN",
                filename = "4x-UltraSharp.pth",
                size = "67 MB",
                precision = "fp32",
                description = "Top rated super-resolution model"
            ),
            ModelItem(
                id = "m6",
                name = "BiRefNet-general",
                type = ModelType.CONTROLNET,
                architecture = "Bilateral",
                filename = "BiRefNet-general-epoch_244.pth",
                size = "315 MB",
                precision = "fp32",
                description = "High resolution object/subject segmentation"
            ),
            ModelItem(
                id = "m7",
                name = "CyberpunkStyle_v2",
                type = ModelType.LORA,
                architecture = "SDXL",
                filename = "cyberpunk_style_v2.safetensors",
                size = "144 MB",
                precision = "fp16",
                description = "Cyberpunk aesthetics and neon lighting LoRA"
            ),
            ModelItem(
                id = "m8",
                name = "ae_flux",
                type = ModelType.VAE,
                architecture = "Flux.1",
                filename = "ae.safetensors",
                size = "335 MB",
                precision = "bf16",
                description = "Flux 16-channel autoencoder"
            )
        )
    }

    fun exportToJson(workflow: Workflow): String {
        val root = JSONObject()
        val nodesArray = JSONArray()

        for (node in workflow.nodes) {
            val nodeObj = JSONObject()
            nodeObj.put("id", node.id)
            nodeObj.put("type", node.type)
            nodeObj.put("title", node.title)

            val posArray = JSONArray()
            posArray.put(node.positionX.toDouble())
            posArray.put(node.positionY.toDouble())
            nodeObj.put("pos", posArray)

            val widgetsObj = JSONObject()
            for ((key, value) in node.widgetValues) {
                widgetsObj.put(key, value)
            }
            nodeObj.put("widgets_values", widgetsObj)

            val inputsArray = JSONArray()
            for (input in node.inputs) {
                val inputObj = JSONObject()
                inputObj.put("name", input.name)
                inputObj.put("type", input.type.name)
                inputsArray.put(inputObj)
            }
            nodeObj.put("inputs", inputsArray)

            val outputsArray = JSONArray()
            for (output in node.outputs) {
                val outputObj = JSONObject()
                outputObj.put("name", output.name)
                outputObj.put("type", output.type.name)
                outputsArray.put(outputObj)
            }
            nodeObj.put("outputs", outputsArray)

            nodesArray.put(nodeObj)
        }
        root.put("nodes", nodesArray)

        val linksArray = JSONArray()
        for (link in workflow.links) {
            val linkArr = JSONArray()
            linkArr.put(link.id)
            linkArr.put(link.fromNodeId)
            linkArr.put(link.fromSocketIndex)
            linkArr.put(link.toNodeId)
            linkArr.put(link.toSocketIndex)
            linkArr.put(link.type.name)
            linksArray.put(linkArr)
        }
        root.put("links", linksArray)
        root.put("name", workflow.name)

        return root.toString(2)
    }

    fun exportToWorkflowApiJson(workflow: Workflow): String {
        val root = JSONObject()
        for (node in workflow.nodes) {
            val nodeObj = JSONObject()
            val inputsObj = JSONObject()

            // 1. Add scalar widget values
            for ((key, value) in node.widgetValues) {
                when {
                    value.toLongOrNull() != null -> inputsObj.put(key, value.toLong())
                    value.toDoubleOrNull() != null -> inputsObj.put(key, value.toDouble())
                    value.equals("true", ignoreCase = true) -> inputsObj.put(key, true)
                    value.equals("false", ignoreCase = true) -> inputsObj.put(key, false)
                    else -> inputsObj.put(key, value)
                }
            }

            // 2. Add connection links into inputs: ["fromNodeId", fromSocketIndex]
            for (link in workflow.links) {
                if (link.toNodeId == node.id) {
                    val socketName = node.inputs.getOrNull(link.toSocketIndex)?.name
                        ?: "input_${link.toSocketIndex}"
                    val wireRef = JSONArray()
                    wireRef.put(link.fromNodeId.toString())
                    wireRef.put(link.fromSocketIndex)
                    inputsObj.put(socketName, wireRef)
                }
            }

            nodeObj.put("inputs", inputsObj)
            nodeObj.put("class_type", node.type)

            val metaObj = JSONObject()
            metaObj.put("title", node.title)
            nodeObj.put("_meta", metaObj)

            root.put(node.id.toString(), nodeObj)
        }
        return root.toString(2)
    }

    fun importFromJson(jsonStr: String, fallbackName: String = "Imported Workflow"): Workflow? {
        return try {
            val root = JSONObject(jsonStr)

            // Case A: ComfyUI Web Graph Export format with "nodes" array
            if (root.has("nodes")) {
                val nodesArray = root.getJSONArray("nodes")
                val parsedNodes = mutableListOf<Node>()
                for (i in 0 until nodesArray.length()) {
                    val nObj = nodesArray.getJSONObject(i)
                    val id = nObj.optInt("id", i + 1)
                    val type = nObj.optString("type", "UnknownNode")
                    val title = nObj.optString("title", type)
                    val posArr = nObj.optJSONArray("pos")
                    val posX = posArr?.optDouble(0, 100.0)?.toFloat() ?: (100f + i * 40f)
                    val posY = posArr?.optDouble(1, 100.0)?.toFloat() ?: (100f + i * 40f)

                    val widgetsMap = mutableMapOf<String, String>()
                    if (nObj.has("widgets_values")) {
                        val wObj = nObj.optJSONObject("widgets_values")
                        if (wObj != null) {
                            val keys = wObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                widgetsMap[k] = wObj.optString(k, "")
                            }
                        } else {
                            val wArr = nObj.optJSONArray("widgets_values")
                            if (wArr != null) {
                                for (w in 0 until wArr.length()) {
                                    widgetsMap["param_$w"] = wArr.optString(w, "")
                                }
                            }
                        }
                    }

                    val cat = when {
                        type.contains("Loader", true) -> NodeCategory.LOADER
                        type.contains("CLIP", true) || type.contains("Text", true) -> NodeCategory.CONDITIONING
                        type.contains("Latent", true) || type.contains("VAE", true) -> NodeCategory.LATENT
                        type.contains("Sampler", true) -> NodeCategory.SAMPLER
                        type.contains("Upscale", true) -> NodeCategory.UPSCALE
                        type.contains("Save", true) || type.contains("Preview", true) -> NodeCategory.IMAGE
                        else -> NodeCategory.POST_PROCESS
                    }

                    parsedNodes.add(
                        Node(
                            id = id,
                            type = type,
                            title = title,
                            category = cat,
                            positionX = posX,
                            positionY = posY,
                            widgetValues = widgetsMap
                        )
                    )
                }

                val parsedLinks = mutableListOf<ConnectionLink>()
                if (root.has("links")) {
                    val linksArray = root.getJSONArray("links")
                    for (l in 0 until linksArray.length()) {
                        val linkItem = linksArray.getJSONArray(l)
                        val linkId = linkItem.optInt(0, l + 1)
                        val fromNode = linkItem.optInt(1, 0)
                        val fromSocket = linkItem.optInt(2, 0)
                        val toNode = linkItem.optInt(3, 0)
                        val toSocket = linkItem.optInt(4, 0)
                        val typeName = linkItem.optString(5, "IMAGE")
                        val socketType = runCatching { SocketType.valueOf(typeName) }.getOrDefault(SocketType.IMAGE)
                        parsedLinks.add(
                            ConnectionLink(
                                id = linkId,
                                fromNodeId = fromNode,
                                fromSocketIndex = fromSocket,
                                toNodeId = toNode,
                                toSocketIndex = toSocket,
                                type = socketType
                            )
                        )
                    }
                }

                val wfName = root.optString("name", fallbackName)
                Workflow(
                    id = "imported_${System.currentTimeMillis()}",
                    name = wfName,
                    nodes = parsedNodes,
                    links = parsedLinks
                )
            } else {
                // Case B: ComfyUI workflow_api.json prompt format (key-value map of node IDs)
                val keys = root.keys()
                val parsedNodes = mutableListOf<Node>()
                val parsedLinks = mutableListOf<ConnectionLink>()
                var linkIdCounter = 1
                var col = 0
                var row = 0

                while (keys.hasNext()) {
                    val key = keys.next()
                    val nodeObj = root.optJSONObject(key) ?: continue
                    val nodeId = key.toIntOrNull() ?: (parsedNodes.size + 1)
                    val classType = nodeObj.optString("class_type", "Node")
                    val metaObj = nodeObj.optJSONObject("_meta")
                    val title = metaObj?.optString("title", classType) ?: classType

                    val inputsObj = nodeObj.optJSONObject("inputs")
                    val widgetValues = mutableMapOf<String, String>()
                    val inputSockets = mutableListOf<NodeSocket>()

                    if (inputsObj != null) {
                        val inputKeys = inputsObj.keys()
                        var socketIdx = 0
                        while (inputKeys.hasNext()) {
                            val inKey = inputKeys.next()
                            val value = inputsObj.get(inKey)
                            if (value is JSONArray && value.length() >= 2) {
                                // This is a link reference: ["node_id", output_slot]
                                val fromId = value.getString(0).toIntOrNull() ?: 0
                                val fromSlot = value.getInt(1)
                                parsedLinks.add(
                                    ConnectionLink(
                                        id = linkIdCounter++,
                                        fromNodeId = fromId,
                                        fromSocketIndex = fromSlot,
                                        toNodeId = nodeId,
                                        toSocketIndex = socketIdx,
                                        type = SocketType.IMAGE
                                    )
                                )
                                inputSockets.add(NodeSocket(inKey, SocketType.IMAGE, true))
                                socketIdx++
                            } else {
                                widgetValues[inKey] = value.toString()
                            }
                        }
                    }

                    val cat = when {
                        classType.contains("Loader", true) -> NodeCategory.LOADER
                        classType.contains("CLIP", true) || classType.contains("Text", true) -> NodeCategory.CONDITIONING
                        classType.contains("Latent", true) || classType.contains("VAE", true) -> NodeCategory.LATENT
                        classType.contains("Sampler", true) -> NodeCategory.SAMPLER
                        classType.contains("Upscale", true) -> NodeCategory.UPSCALE
                        classType.contains("Save", true) || classType.contains("Preview", true) -> NodeCategory.IMAGE
                        else -> NodeCategory.POST_PROCESS
                    }

                    val posX = 80f + col * 340f
                    val posY = 80f + row * 260f
                    col = (col + 1) % 4
                    if (col == 0) row++

                    parsedNodes.add(
                        Node(
                            id = nodeId,
                            type = classType,
                            title = title,
                            category = cat,
                            positionX = posX,
                            positionY = posY,
                            inputs = inputSockets,
                            outputs = listOf(NodeSocket("OUTPUT", SocketType.IMAGE, false)),
                            widgetValues = widgetValues
                        )
                    )
                }

                Workflow(
                    id = "api_wf_${System.currentTimeMillis()}",
                    name = fallbackName,
                    description = "Imported from ComfyUI workflow_api.json",
                    nodes = parsedNodes,
                    links = parsedLinks
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkflowRepo", "Failed to parse workflow JSON", e)
            null
        }
    }
}
