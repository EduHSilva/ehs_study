package com.studyflow.app.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskUpdateSerializationTest {

    // Mesma configuração do ApiClient: null comum é omitido, JsonNull é enviado.
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    @Test
    fun `null explicito desvincula projeto e etapa`() {
        val text = json.encodeToString(
            UpdateTaskRequest.serializer(),
            UpdateTaskRequest(title = "Tarefa", projectId = JsonNull, stageId = JsonNull),
        )

        assertTrue(text.contains("\"projectId\":null"))
        assertTrue(text.contains("\"stageId\":null"))
    }

    @Test
    fun `null comum e omitido do corpo`() {
        val text = json.encodeToString(
            UpdateTaskRequest.serializer(),
            UpdateTaskRequest(status = "DONE"),
        )

        assertFalse(text.contains("projectId"))
        assertFalse(text.contains("stageId"))
    }
}
