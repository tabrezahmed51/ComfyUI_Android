package com.example.comfyui.data.network.api

import com.example.comfyui.data.network.model.PromptResponse
import com.example.comfyui.data.network.model.QueueStatusResponse
import com.example.comfyui.data.network.model.SystemStatsResponse
import com.google.gson.JsonObject
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service definition for ComfyUI REST API endpoints.
 */
interface ComfyApiService {

    @GET("system_stats")
    suspend fun getSystemStats(): Response<SystemStatsResponse>

    @GET("prompt")
    suspend fun getQueue(): Response<QueueStatusResponse>

    @POST("prompt")
    suspend fun queuePrompt(
        @Body body: RequestBody
    ): Response<PromptResponse>

    @GET("history/{prompt_id}")
    suspend fun getHistory(
        @Path("prompt_id") promptId: String
    ): Response<JsonObject>

    @GET("view")
    @Streaming
    suspend fun viewImage(
        @Query("filename") filename: String,
        @Query("subfolder") subfolder: String = "",
        @Query("type") type: String = "output"
    ): Response<ResponseBody>

    @GET("object_info")
    suspend fun getObjectInfo(): Response<JsonObject>

    @GET
    suspend fun pingCustomUrl(
        @Url url: String
    ): Response<ResponseBody>
}
