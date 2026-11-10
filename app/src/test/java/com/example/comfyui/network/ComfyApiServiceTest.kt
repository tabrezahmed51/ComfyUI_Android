package com.example.comfyui.network

import com.example.comfyui.data.network.api.ComfyApiService
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class ComfyApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ComfyApiService

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(ComfyApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getSystemStats_deserializesSystemInfoAndGpuStatsCorrectly() = runBlocking {
        val mockJson = """
            {
              "system": {
                "os": "Linux-6.5.0-generic",
                "python_version": "3.10.12",
                "pytorch_version": "2.2.0+cu121",
                "embedded_python": false
              },
              "devices": [
                {
                  "name": "NVIDIA GeForce RTX 4090",
                  "type": "cuda",
                  "index": 0,
                  "vram_total": 25757220864,
                  "vram_free": 18234857472,
                  "torch_vram_total": 25757220864,
                  "torch_vram_free": 18234857472
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val response = apiService.getSystemStats()
        assertTrue(response.isSuccessful)

        val stats = response.body()
        assertNotNull(stats)
        assertEquals("Linux-6.5.0-generic", stats?.system?.os)
        assertEquals("3.10.12", stats?.system?.pythonVersion)
        assertEquals("2.2.0+cu121", stats?.system?.pytorchVersion)
        assertEquals(false, stats?.system?.embeddedPython)

        val devices = stats?.devices
        assertNotNull(devices)
        assertEquals(1, devices?.size)
        val gpu = devices?.first()
        assertEquals("NVIDIA GeForce RTX 4090", gpu?.name)
        assertEquals("cuda", gpu?.type)
        assertEquals(0, gpu?.index)
        assertEquals(25757220864L, gpu?.vramTotal)
        assertEquals(18234857472L, gpu?.vramFree)
    }

    @Test
    fun getQueue_deserializesRunningAndPendingQueues() = runBlocking {
        val mockJson = """
            {
              "queue_running": [
                ["job_id_101", 1, {"3": {"inputs": {"seed": 12345}}}]
              ],
              "queue_pending": [
                ["job_id_102", 2, {}],
                ["job_id_103", 3, {}]
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val response = apiService.getQueue()
        assertTrue(response.isSuccessful)

        val queue = response.body()
        assertNotNull(queue)
        assertEquals(1, queue?.queueRunning?.size)
        assertEquals(2, queue?.queuePending?.size)
    }

    @Test
    fun queuePrompt_successReturnsPromptIdAndNumber() = runBlocking {
        val mockJson = """
            {
              "prompt_id": "9b83b37a-4ec8-490b-ba6c-2f95b95874de",
              "number": 42,
              "node_errors": {}
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val body = "{\"prompt\":{}}".toRequestBody("application/json".toMediaType())
        val response = apiService.queuePrompt(body)

        assertTrue(response.isSuccessful)
        val result = response.body()
        assertNotNull(result)
        assertEquals("9b83b37a-4ec8-490b-ba6c-2f95b95874de", result?.promptId)
        assertEquals(42, result?.number)
        assertTrue(result?.nodeErrors.isNullOrEmpty())
    }

    @Test
    fun getHistory_returnsJsonObjectWithOutputFilenames() = runBlocking {
        val mockJson = """
            {
              "job_42": {
                "prompt": [1, "uuid", {}, {}],
                "outputs": {
                  "9": {
                    "images": [
                      {
                        "filename": "ComfyUI_00042_.png",
                        "subfolder": "",
                        "type": "output"
                      }
                    ]
                  }
                },
                "status": {
                  "status_str": "success",
                  "completed": true
                }
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val response = apiService.getHistory("job_42")
        assertTrue(response.isSuccessful)

        val root = response.body()
        assertNotNull(root)
        assertTrue(root?.has("job_42") == true)
        val jobObj = root?.getAsJsonObject("job_42")
        assertTrue(jobObj?.has("outputs") == true)
    }
}
