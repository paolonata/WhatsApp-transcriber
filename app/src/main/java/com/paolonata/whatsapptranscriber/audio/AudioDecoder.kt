package com.paolonata.whatsapptranscriber.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.IOException
import java.nio.ByteOrder

/**
 * Decodes an arbitrary compressed audio file (as shared by WhatsApp: usually an
 * Ogg/Opus voice note, sometimes AAC/M4A) into 16 kHz mono float32 PCM, the format
 * whisper.cpp expects. Uses Android's built-in MediaExtractor/MediaCodec, so no
 * extra native decoding library is needed and no data ever leaves the device.
 */
object AudioDecoder {

    private const val TARGET_SAMPLE_RATE = 16000

    fun decodeToPcm16k(context: Context, uri: Uri): FloatArray {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Impossibile aprire il file audio condiviso")

        pfd.use { descriptor ->
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(descriptor.fileDescriptor)

                var trackIndex = -1
                var inputFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        trackIndex = i
                        inputFormat = format
                        break
                    }
                }
                if (trackIndex < 0 || inputFormat == null) {
                    throw IOException("Nessuna traccia audio trovata nel file condiviso")
                }
                extractor.selectTrack(trackIndex)

                val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
                var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

                val codec = MediaCodec.createDecoderByType(mime)
                codec.configure(inputFormat, null, null, 0)
                codec.start()

                val pcmChunks = mutableListOf<ShortArray>()
                var totalSamples = 0
                val bufferInfo = MediaCodec.BufferInfo()
                var sawInputEos = false
                var sawOutputEos = false

                try {
                    while (!sawOutputEos) {
                        if (!sawInputEos) {
                            val inputIndex = codec.dequeueInputBuffer(10_000)
                            if (inputIndex >= 0) {
                                val inputBuffer = codec.getInputBuffer(inputIndex)!!
                                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                                if (sampleSize < 0) {
                                    codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    sawInputEos = true
                                } else {
                                    val presentationTimeUs = extractor.sampleTime
                                    codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)
                                    extractor.advance()
                                }
                            }
                        }

                        val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                        when {
                            outputIndex >= 0 -> {
                                if (bufferInfo.size > 0) {
                                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    val shortBuffer = outputBuffer.asShortBuffer()
                                    val chunk = ShortArray(shortBuffer.remaining())
                                    shortBuffer.get(chunk)
                                    pcmChunks.add(chunk)
                                    totalSamples += chunk.size
                                }
                                codec.releaseOutputBuffer(outputIndex, false)
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    sawOutputEos = true
                                }
                            }
                            outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                val outputFormat = codec.outputFormat
                                sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                        }
                    }
                } finally {
                    codec.stop()
                    codec.release()
                }

                val merged = ShortArray(totalSamples)
                var offset = 0
                for (chunk in pcmChunks) {
                    System.arraycopy(chunk, 0, merged, offset, chunk.size)
                    offset += chunk.size
                }

                val mono = if (channelCount > 1) downmixToMono(merged, channelCount) else merged
                val resampled = if (sampleRate != TARGET_SAMPLE_RATE) {
                    resampleLinear(mono, sampleRate, TARGET_SAMPLE_RATE)
                } else {
                    mono
                }

                return FloatArray(resampled.size) { i -> resampled[i] / 32768.0f }
            } finally {
                extractor.release()
            }
        }
    }

    private fun downmixToMono(data: ShortArray, channels: Int): ShortArray {
        val frameCount = data.size / channels
        val mono = ShortArray(frameCount)
        for (i in 0 until frameCount) {
            var sum = 0
            for (c in 0 until channels) sum += data[i * channels + c]
            mono[i] = (sum / channels).toShort()
        }
        return mono
    }

    private fun resampleLinear(data: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        if (fromRate == toRate || data.isEmpty()) return data
        val ratio = toRate.toDouble() / fromRate.toDouble()
        val newLength = (data.size * ratio).toInt().coerceAtLeast(1)
        val result = ShortArray(newLength)
        val lastIndex = data.size - 1
        for (i in 0 until newLength) {
            val srcPos = i / ratio
            val srcIndex = srcPos.toInt().coerceIn(0, lastIndex)
            val nextIndex = (srcIndex + 1).coerceIn(0, lastIndex)
            val frac = srcPos - srcIndex
            val s0 = data[srcIndex]
            val s1 = data[nextIndex]
            result[i] = (s0 + (s1 - s0) * frac).toInt().toShort()
        }
        return result
    }
}
