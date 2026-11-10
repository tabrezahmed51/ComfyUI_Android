package com.example.comfyui.network

import com.example.comfyui.data.network.model.DeviceInfo
import com.example.comfyui.data.network.model.PromptResponse
import com.example.comfyui.data.network.model.QueueStatusResponse
import com.example.comfyui.data.network.model.SystemStatsResponse
import com.example.comfyui.data.repository.WorkflowRepository
import com.google.gson.Gson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ComfyNetworkModelsTest {

    private val gson = Gson()

    @Test
    fun systemStatsResponse_handlesMultiGpuAndNullDevices() {
        // Multi GPU JSON
        val multiGpuJson = """
            {
              "system": { "os": "Windows-10", "python_version": "3.11.0" },
              "devices": [
                { "name": "GPU 0 - RTX 3080", "type": "cuda", "index": 0, "vram_total": 10737418240, "vram_free": 8589934592 },
                { "name": "GPU 1 - RTX 3060", "type": "cuda", "index": 1, "vram_total": 12884901888, "vram_free": 11811160064 }
              ]
            }
        """.trimIndent()

        val parsed = gson.fromJson(multiGpuJson, SystemStatsResponse::class.java)
        assertNotNull(parsed.devices)
        assertEquals(2, parsed.devices?.size)
        assertEquals("GPU 0 - RTX 3080", parsed.devices?.get(0)?.name)
        assertEquals("GPU 1 - RTX 3060", parsed.devices?.get(1)?.name)

        // Empty / Null devices JSON
        val noGpuJson = "{\"system\": {\"os\": \"macOS-14.0\"}}"
        val parsedNoGpu = gson.fromJson(noGpuJson, SystemStatsResponse::class.java)
        assertEquals("macOS-14.0", parsedNoGpu.system?.os)
        assertNull(parsedNoGpu.devices)
    }

    @Test
    fun promptResponse_handlesErrorsAndMissingFields() {
        val errorJson = """
            {
              "error": "Prompt syntax invalid",
              "node_errors": {
                "3": {
                  "errors": ["Missing input 'clip'"]
                }
              }
            }
        """.trimIndent()

        val parsed = gson.fromJson(errorJson, PromptResponse::class.java)
        assertNull(parsed.promptId)
        assertNull(parsed.number)
        assertEquals("Prompt syntax invalid", parsed.error)
        assertNotNull(parsed.nodeErrors)
        assertTrue(parsed.nodeErrors?.containsKey("3") == true)
    }

    @Test
    fun workflowRepository_exportToWorkflowApiJson_generatesValidComfyStructure() {
        val workflow = WorkflowRepository.createStandardSDWorkflow()
        val apiJsonString = WorkflowRepository.exportToWorkflowApiJson(workflow)

        assertNotNull(apiJsonString)
        assertTrue(apiJsonString.isNotEmpty())

        val jsonObject = JSONObject(apiJsonString)
        assertTrue(jsonObject.length() > 0)

        // Verify standard node IDs are present (1=CheckpointLoaderSimple, 2=CLIPTextEncode, 5=KSampler, 9=SaveImage)
        assertTrue(jsonObject.has("1") || jsonObject.has("2"))
    }
}
