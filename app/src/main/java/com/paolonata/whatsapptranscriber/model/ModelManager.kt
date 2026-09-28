package com.paolonata.whatsapptranscriber.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the offline whisper.cpp speech-recognition model, once, on first use.
 *
 * This is the ONLY network call the app ever makes: it fetches a public, static
 * model file (no personal data of any kind) from whisper.cpp's official hosting
 * on Hugging Face. Once downloaded, all transcription happens entirely on-device;
 * the shared audio and the resulting text are never uploaded anywhere.
 */
class ModelManager(private val context: Context) {

    companion object {
        // large-v3-turbo's audio encoder is identical (and just as costly) to the
        // full large-v3 model - only its decoder is lighter. whisper.cpp on
        // Android runs on CPU only (no GPU/NPU acceleration), so that encoder
        // cost dominates and made a 5-minute voice note take 30+ minutes to
        // transcribe. "medium" has a much shallower encoder (24 layers vs 32)
        // and is a far better fit for on-device CPU inference, while still
        // being noticeably more accurate on unclear speech than "small".
        private const val MODEL_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q5_0.bin"
        private const val MODEL_FILE_NAME = "ggml-medium-q5_0.bin"
    }

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    fun modelFile(): File = File(modelsDir, MODEL_FILE_NAME)

    fun isModelReady(): Boolean = modelFile().exists() && modelFile().length() > 0

    suspend fun ensureModelDownloaded(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        if (isModelReady()) return@withContext

        // Remove any model left over from a previous app version instead of
        // accumulating several hundred MB blobs across updates.
        modelsDir.listFiles()?.forEach { it.delete() }

        val tempFile = File(modelsDir, "$MODEL_FILE_NAME.part")
        val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode !in 200..299) {
                throw IOException("Download del modello fallito (codice ${connection.responseCode})")
            }

            val totalBytes = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var totalRead = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress((totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                }
            }

            if (!tempFile.renameTo(modelFile())) {
                throw IOException("Impossibile salvare il modello scaricato")
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }
}
