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

    /** Thrown when [ConnectionHandle.cancel] stopped the request - never fall back on-device for this. */
    class CancelledException : IOException("Annullato dall'utente")

    /**
     * Lets whoever started a [transcribe] call interrupt it from another
     * thread: disconnecting the live HttpURLConnection is the only way to
     * unblock a thread that's stuck reading the server's response.
     */
    class ConnectionHandle {
        @Volatile var connection: HttpURLConnection? = null
        @Volatile var cancelled = false

        fun cancel() {
            cancelled = true
            connection?.disconnect()
        }
    }

    suspend fun transcribe(
        config: ServerConfig,
        audioFile: File,
        tier: ModelTier,
        jobId: String,
        handle: ConnectionHandle,
    ): String = withContext(Dispatchers.IO) {
        if (handle.cancelled) throw CancelledException()

        val connection = URL("${config.baseUrl}/transcribe").openConnection() as HttpURLConnection
        handle.connection = connection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer ${config.token}")
            connection.setRequestProperty("X-Model-Tier", if (tier == ModelTier.PRECISE) "precise" else "fast")
            connection.setRequestProperty("X-Job-Id", jobId)
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setFixedLengthStreamingMode(audioFile.length())

            connection.outputStream.use { output ->
                audioFile.inputStream().use { input -> input.copyTo(output) }
            }

            val responseCode = connection.responseCode
            val body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                ?: ""

            if (handle.cancelled) throw CancelledException()

            val json = runCatching { JSONObject(body) }.getOrNull()
            if (responseCode !in 200..299) {
                throw IOException(json?.optString("error") ?: "Il server ha risposto $responseCode")
            }
            val error = json?.optString("error")
            if (error == "cancelled") throw CancelledException()

            json?.optString("text")?.takeIf { it.isNotBlank() }
                ?: throw IOException("Risposta del server non valida")
        } catch (e: IOException) {
            if (handle.cancelled) throw CancelledException()
            throw e
        } finally {
            handle.connection = null
            connection.disconnect()
        }
    }

    /** Best-effort: tells the server to kill the process for [jobId]. Never throws. */
    suspend fun cancel(config: ServerConfig, jobId: String) = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${config.baseUrl}/cancel").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 3_000
                connection.readTimeout = 3_000
                connection.setRequestProperty("Authorization", "Bearer ${config.token}")
                connection.setRequestProperty("X-Job-Id", jobId)
                connection.outputStream.close()
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            // If this fails the server job may keep running until it finishes
            // naturally, but the app itself has already stopped waiting.
        }
    }
}
