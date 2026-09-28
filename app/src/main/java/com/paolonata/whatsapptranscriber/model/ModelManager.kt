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
 * whisper.cpp on Android runs CPU-only (no GPU/NPU acceleration), so a
 * bigger/more accurate model can easily turn a 5-minute voice note into a
 * 30+ minute wait. To keep the app usable by default while still allowing
 * better accuracy when it's actually wanted, every transcription uses [FAST]
 * automatically, and the user can ask to re-run a specific note through
 * [PRECISE] afterwards ("Migliora precisione") instead of always paying that
 * cost up front.
 */
enum class ModelTier(val fileName: String) {
    FAST("ggml-small-q5_1.bin"),
    PRECISE("ggml-medium-q5_0.bin"),
}

/**
 * Downloads whisper.cpp speech-recognition models, once per tier, on first use.
 *
 * Model downloads are the ONLY network calls the app ever makes: each fetches a
 * public, static model file (no personal data of any kind) from whisper.cpp's
 * official hosting on Hugging Face. Once downloaded, all transcription happens
 * entirely on-device; the shared audio and the resulting text are never
 * uploaded anywhere.
 */
class ModelManager(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    fun modelFile(tier: ModelTier): File = File(modelsDir, tier.fileName)

    fun isModelReady(tier: ModelTier): Boolean = modelFile(tier).exists() && modelFile(tier).length() > 0

    suspend fun ensureModelDownloaded(tier: ModelTier, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        if (isModelReady(tier)) return@withContext

        // Clear out anything left over from an older app version that isn't
        // one of the model files we currently use, instead of accumulating
        // several hundred MB of stale blobs across updates.
        val currentFileNames = ModelTier.entries.map { it.fileName }.toSet()
        modelsDir.listFiles()
            ?.filter { it.name !in currentFileNames && !it.name.endsWith(".part") }
            ?.forEach { it.delete() }

        val modelUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${tier.fileName}"
        val tempFile = File(modelsDir, "${tier.fileName}.part")
        val connection = URL(modelUrl).openConnection() as HttpURLConnection
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

            if (!tempFile.renameTo(modelFile(tier))) {
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
