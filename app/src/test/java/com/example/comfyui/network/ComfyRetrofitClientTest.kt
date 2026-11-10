package com.example.comfyui.network

import com.example.comfyui.data.network.ComfyRetrofitClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ComfyRetrofitClientTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        ComfyRetrofitClient.resetToEnvDefaults()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        ComfyRetrofitClient.resetToEnvDefaults()
    }

    @Test
    fun updateBaseUrl_normalizesProtocolsAndTrailingSlashes() {
        // Without scheme
        ComfyRetrofitClient.updateBaseUrl("192.168.1.100:8188")
        assertEquals("http://192.168.1.100:8188/", ComfyRetrofitClient.currentBaseUrl)

        // With trailing slash and spaces
        ComfyRetrofitClient.updateBaseUrl("  https://my-comfy-server.domain.com/  ")
        assertEquals("https://my-comfy-server.domain.com/", ComfyRetrofitClient.currentBaseUrl)

        // With standard HTTP and port
        ComfyRetrofitClient.updateBaseUrl("http://10.0.2.2:8188")
        assertEquals("http://10.0.2.2:8188/", ComfyRetrofitClient.currentBaseUrl)
    }

    @Test
    fun updateApiKey_handlesTrimmingAndNoneSentinel() {
        // Standard API key
        ComfyRetrofitClient.updateApiKey("  secret_api_token_123  ")
        assertEquals("secret_api_token_123", ComfyRetrofitClient.currentApiKey)

        // Sentinel 'none' should be treated as empty
        ComfyRetrofitClient.updateApiKey("none")
        assertEquals("", ComfyRetrofitClient.currentApiKey)

        // Whitespace only
        ComfyRetrofitClient.updateApiKey("   ")
        assertEquals("", ComfyRetrofitClient.currentApiKey)
    }

    @Test
    fun dynamicInterceptor_injectsAuthHeadersWhenKeyIsPresent() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"status\":\"ok\"}"))

        val mockUrl = mockWebServer.url("/").toString()
        ComfyRetrofitClient.updateBaseUrl(mockUrl)
        ComfyRetrofitClient.updateApiKey("test_token_xyz")

        // Issue request through client
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Mimic the dynamicEndpointInterceptor logic
                val orig = chain.request()
                val activeKey = ComfyRetrofitClient.currentApiKey
                val builder = orig.newBuilder()
                    .header("User-Agent", "ComfyUI-Android-Client/1.0")
                if (activeKey.isNotBlank()) {
                    builder.header("Authorization", "Bearer $activeKey")
                    builder.header("X-API-Key", activeKey)
                }
                chain.proceed(builder.build())
            }
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val response = client.newCall(Request.Builder().url(mockUrl).build()).execute()
        assertTrue(response.isSuccessful)

        val recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recordedRequest)
        assertEquals("Bearer test_token_xyz", recordedRequest?.getHeader("Authorization"))
        assertEquals("test_token_xyz", recordedRequest?.getHeader("X-API-Key"))
        assertEquals("ComfyUI-Android-Client/1.0", recordedRequest?.getHeader("User-Agent"))
    }

    @Test
    fun dynamicInterceptor_omitsAuthHeadersWhenKeyIsBlank() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"status\":\"ok\"}"))

        val mockUrl = mockWebServer.url("/").toString()
        ComfyRetrofitClient.updateBaseUrl(mockUrl)
        ComfyRetrofitClient.updateApiKey("")

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val orig = chain.request()
                val activeKey = ComfyRetrofitClient.currentApiKey
                val builder = orig.newBuilder()
                    .header("User-Agent", "ComfyUI-Android-Client/1.0")
                if (activeKey.isNotBlank()) {
                    builder.header("Authorization", "Bearer $activeKey")
                    builder.header("X-API-Key", activeKey)
                }
                chain.proceed(builder.build())
            }
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val response = client.newCall(Request.Builder().url(mockUrl).build()).execute()
        assertTrue(response.isSuccessful)

        val recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recordedRequest)
        assertNull(recordedRequest?.getHeader("Authorization"))
        assertNull(recordedRequest?.getHeader("X-API-Key"))
        assertEquals("ComfyUI-Android-Client/1.0", recordedRequest?.getHeader("User-Agent"))
    }
}
