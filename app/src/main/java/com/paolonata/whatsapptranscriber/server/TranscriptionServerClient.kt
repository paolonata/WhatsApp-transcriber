package com.paolonata.whatsapptranscriber.server

import com.paolonata.whatsapptranscriber.model.ModelTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the optional home-network Whisper server (see
 * whisper-transcriber-addon/). A short connect timeout means "not on the
 * home network" fails fast instead of making the user wait before the
 * on-device fallback kicks in.
 */
object TranscriptionServerClient {
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 15 * 60 * 1000 // transcription itself can take a while

    suspend fun transcribe(config: ServerConfig, audioFile: File, tier: ModelTier): String =
        withContext(Dispatchers.IO) {
            val connection = URL("${config.baseUrl}/transcribe").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.setRequestProperty("Authorization", "Bearer ${config.token}")
                connection.setRequestProperty("X-Model-Tier", if (tier == ModelTier.PRECISE) "precise" else "fast")
                connection.setRequestProperty("Content-Type", "application/octet-stream")
                connection.setFixedLengthStreamingMode(audioFile.length())

                connection.outputStream.use { output ->
                    audioFile.inputStream().use { input -> input.copyTo(output) }
                }

                val responseCode = connection.responseCode
                val body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: ""

                val json = runCatching { JSONObject(body) }.getOrNull()
                if (responseCode !in 200..299) {
                    throw IOException(json?.optString("error") ?: "Il server ha risposto $responseCode")
                }
                json?.optString("text")?.takeIf { it.isNotBlank() }
                    ?: throw IOException("Risposta del server non valida")
            } finally {
                connection.disconnect()
            }
        }
}
