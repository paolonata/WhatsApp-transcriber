package com.paolonata.whatsapptranscriber.transcription

import com.whispercpp.whisper.WhisperContext
import java.io.File

/**
 * Thin wrapper around whisper.cpp's on-device WhisperContext (see :lib module,
 * vendored from https://github.com/ggerganov/whisper.cpp). All inference runs
 * locally on the phone's CPU; nothing is sent over the network here.
 */
class TranscriptionEngine private constructor(private val whisperContext: WhisperContext) {

    suspend fun transcribe(pcm16kMono: FloatArray): String {
        return whisperContext.transcribeData(pcm16kMono, printTimestamp = false).trim()
    }

    suspend fun release() {
        whisperContext.release()
    }

    companion object {
        fun create(modelFile: File): TranscriptionEngine {
            val context = WhisperContext.createContextFromFile(modelFile.absolutePath)
            return TranscriptionEngine(context)
        }
    }
}
