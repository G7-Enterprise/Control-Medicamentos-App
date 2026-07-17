package com.carlos.controlmedicamentos.data.remote

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object OllamaMedicalAiClient {
    fun generateClinicalReport(config: MedicalAiConfig, prompt: String): String {
        val connection = (URL(config.endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        val payload = JSONObject()
            .put("model", config.modelName)
            .put("prompt", prompt)
            .put("stream", false)

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = BufferedReader(
            InputStreamReader(stream ?: throw IllegalStateException("Respuesta vacia del servidor"), Charsets.UTF_8)
        ).use { reader ->
            reader.readText()
        }

        if (responseCode !in 200..299) {
            throw IllegalStateException("Ollama devolvio HTTP $responseCode: $responseBody")
        }

        val json = JSONObject(responseBody)
        return json.optString("response").ifBlank {
            throw IllegalStateException("La respuesta de Ollama no incluyo el campo response")
        }
    }
}