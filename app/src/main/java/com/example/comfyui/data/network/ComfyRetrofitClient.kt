package com.example.comfyui.data.network

import android.util.Log
import com.example.comfyui.BuildConfig
import com.example.comfyui.data.network.api.ComfyApiService
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Singleton Retrofit provider handling network communication with the ComfyUI server instance.
 * Initialized from the .env configuration via BuildConfig, and supports dynamic runtime updates.
 */
object ComfyRetrofitClient {

    private const val TAG = "ComfyRetrofitClient"

    // Default base URL from .env via BuildConfig
    private val defaultBaseUrl: String = run {
        val envUrl = runCatching { BuildConfig.COMFYUI_SERVER_URL }.getOrNull()
        if (!envUrl.isNullOrBlank() && envUrl != "none") {
            formatBaseUrl(envUrl)
        } else {
            "http://127.0.0.1:8188/"
        }
    }

    private val defaultApiKey: String = run {
        val envKey = runCatching { BuildConfig.COMFY_UI_SERVER_KEY }.getOrNull()
        if (!envKey.isNullOrBlank() && envKey != "none") envKey else ""
    }

    private val currentBaseUrlRef = AtomicReference(defaultBaseUrl)
    private val currentApiKeyRef = AtomicReference(defaultApiKey)

    val currentBaseUrl: String
        get() = currentBaseUrlRef.get()

    val currentApiKey: String
        get() = currentApiKeyRef.get()

    fun updateBaseUrl(newUrl: String) {
        val formatted = formatBaseUrl(newUrl)
        currentBaseUrlRef.set(formatted)
        Log.d(TAG, "Base URL dynamically updated to: $formatted")
    }

    fun updateApiKey(newKey: String) {
        val trimmed = newKey.trim()
        val safeKey = if (trimmed == "none") "" else trimmed
        currentApiKeyRef.set(safeKey)
        Log.d(TAG, "API key dynamically updated (length=${safeKey.length})")
    }

    fun resetToEnvDefaults() {
        currentBaseUrlRef.set(defaultBaseUrl)
        currentApiKeyRef.set(defaultApiKey)
        Log.d(TAG, "Reset connection parameters to .env defaults")
    }

    private fun formatBaseUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        return clean
    }

    /**
     * Interceptor that dynamically alters the host, scheme, and port of requests
     * to match the currently configured COMFYUI_SERVER_URL, and injects auth headers.
     */
    private val dynamicEndpointInterceptor = Interceptor { chain ->
        var originalRequest = chain.request()
        val activeUrlStr = currentBaseUrlRef.get()
        val activeHttpUrl = activeUrlStr.toHttpUrlOrNull()

        val requestBuilder = originalRequest.newBuilder()

        // 1. Rewrite base host & scheme if available
        if (activeHttpUrl != null) {
            val newHttpUrl = originalRequest.url.newBuilder()
                .scheme(activeHttpUrl.scheme)
                .host(activeHttpUrl.host)
                .port(activeHttpUrl.port)
                .build()
            requestBuilder.url(newHttpUrl)
        }

        // 2. Attach auth headers if COMFY_UI_SERVER_KEY is present
        val activeKey = currentApiKeyRef.get()
        if (activeKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $activeKey")
            requestBuilder.header("X-API-Key", activeKey)
        }

        requestBuilder.header("User-Agent", "ComfyUI-Android-Client/1.0")

        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("ComfyHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(dynamicEndpointInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(defaultBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ComfyApiService by lazy {
        retrofit.create(ComfyApiService::class.java)
    }
}
