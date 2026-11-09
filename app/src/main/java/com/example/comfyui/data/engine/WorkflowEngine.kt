package com.example.comfyui.data.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.example.comfyui.R
import com.example.comfyui.data.model.GenerationJob
import com.example.comfyui.data.model.JobStatus
import com.example.comfyui.data.model.Workflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.random.Random

class WorkflowEngine(private val context: Context) {

    suspend fun executeWorkflow(
        workflow: Workflow,
        onProgressUpdate: (GenerationJob) -> Unit
    ): GenerationJob {
        val positivePromptNode = workflow.nodes.find {
            it.type == "CLIPTextEncode" && (it.title.contains("Positive", ignoreCase = true) || !it.title.contains("Negative", ignoreCase = true))
        } ?: workflow.nodes.find { it.type == "CLIPTextEncode" }
        val promptText = positivePromptNode?.widgetValues?.get("text")
            ?: "a photorealistic cyberpunk cityscape with glowing cyan neon lights"

        val negativePromptNode = workflow.nodes.find {
            it.type == "CLIPTextEncode" && it.title.contains("Negative", ignoreCase = true)
        }
        val negativePrompt = negativePromptNode?.widgetValues?.get("text")
            ?: "blurry, low quality, deformed"

        val samplerNode = workflow.nodes.find { it.type.contains("KSampler", ignoreCase = true) }
        val steps = samplerNode?.widgetValues?.get("steps")?.toIntOrNull() ?: 20
        val cfg = samplerNode?.widgetValues?.get("cfg")?.toFloatOrNull() ?: 7.0f
        val samplerName = samplerNode?.widgetValues?.get("sampler_name") ?: "euler"
        val seed = samplerNode?.widgetValues?.get("seed")?.toLongOrNull() ?: Random.nextLong(100000000, 9999999999)

        val latentNode = workflow.nodes.find { it.type.contains("EmptyLatent", ignoreCase = true) }
        val width = latentNode?.widgetValues?.get("width")?.toIntOrNull() ?: 512
        val height = latentNode?.widgetValues?.get("height")?.toIntOrNull() ?: 512

        val loaderNode = workflow.nodes.find {
            it.type.contains("Loader", ignoreCase = true) && !it.type.contains("Upscale", ignoreCase = true)
        }
        val modelName = loaderNode?.widgetValues?.get("ckpt_name")
            ?: loaderNode?.widgetValues?.get("unet_name")
            ?: "v1-5-pruned-emaonly.safetensors"

        val startTime = System.currentTimeMillis()
        val jobId = UUID.randomUUID().toString()

        var job = GenerationJob(
            id = jobId,
            workflowName = workflow.name,
            promptText = promptText,
            negativePromptText = negativePrompt,
            seed = seed,
            steps = steps,
            cfg = cfg,
            samplerName = samplerName,
            modelName = modelName,
            width = width,
            height = height,
            status = JobStatus.RUNNING,
            currentNodeTitle = "Initializing Pipeline",
            currentStep = 0,
            totalSteps = steps,
            progress = 0.05f,
            workflowSnapshot = workflow
        )
        onProgressUpdate(job)
        delay(250)

        // Stage 1: Load Model
        job = job.copy(
            currentNodeTitle = "1: Load Checkpoint ($modelName)",
            progress = 0.15f
        )
        onProgressUpdate(job)
        delay(350)

        // Stage 2: Encode Prompts
        job = job.copy(
            currentNodeTitle = "2: CLIP Text Encode (Positive & Negative)",
            progress = 0.25f
        )
        onProgressUpdate(job)
        delay(300)

        // Stage 3: Latent Generation
        job = job.copy(
            currentNodeTitle = "3: Empty Latent Image (${width}x${height})",
            progress = 0.30f
        )
        onProgressUpdate(job)
        delay(200)

        // Stage 4: KSampler denoising steps loop
        val stepIntervalMs = if (steps > 10) 70L else 180L
        for (i in 1..steps) {
            val denoiseProgress = 0.30f + (0.55f * (i.toFloat() / steps.toFloat()))
            job = job.copy(
                currentNodeTitle = "4: KSampler (Step $i/$steps)",
                currentStep = i,
                progress = denoiseProgress
            )
            onProgressUpdate(job)
            delay(stepIntervalMs)
        }

        // Stage 5: VAE Decode
        job = job.copy(
            currentNodeTitle = "5: VAE Decode (Latent to RGB)",
            progress = 0.92f
        )
        onProgressUpdate(job)
        delay(300)

        // Stage 6: Save Image & Build Output
        val duration = (System.currentTimeMillis() - startTime) / 1000f
        val imagePath = saveGeneratedImage(jobId, promptText, seed, width, height)

        job = job.copy(
            currentNodeTitle = "Completed",
            status = JobStatus.COMPLETED,
            progress = 1.0f,
            outputImageUrl = imagePath,
            durationSeconds = duration,
            completedAt = System.currentTimeMillis()
        )
        onProgressUpdate(job)

        return job
    }

    private fun saveGeneratedImage(
        jobId: String,
        prompt: String,
        seed: Long,
        width: Int,
        height: Int
    ): String {
        return try {
            val filename = "gen_${jobId}.jpg"
            val file = File(context.filesDir, filename)

            // If prompt mentions fox or animal, use cyber fox; otherwise cyberpunk city
            val isFox = prompt.contains("fox", ignoreCase = true) || prompt.contains("animal", ignoreCase = true)
            val resId = if (isFox) R.drawable.sample_cyber_fox else R.drawable.sample_cyberpunk

            context.resources.openRawResource(resId).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            // Fallback bitmap generator
            createFallbackBitmap(jobId, prompt, seed, width, height)
        }
    }

    private fun createFallbackBitmap(
        jobId: String,
        prompt: String,
        seed: Long,
        width: Int,
        height: Int
    ): String {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val gradient = LinearGradient(
            0f, 0f, 512f, 512f,
            intArrayOf(0xFF0F172A.toInt(), 0xFF0284C7.toInt(), 0xFF10B981.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, 512f, 512f, paint)

        val textPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("ComfyUI Output: Seed $seed", 32f, 80f, textPaint)

        val file = File(context.filesDir, "gen_${jobId}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }
}
