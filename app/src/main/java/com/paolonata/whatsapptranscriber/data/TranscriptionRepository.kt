package com.paolonata.whatsapptranscriber.data

import java.io.File
import kotlinx.coroutines.flow.Flow

class TranscriptionRepository(private val dao: TranscriptionDao) {
    fun observeAll(): Flow<List<Transcription>> = dao.observeAll()

    suspend fun insert(transcription: Transcription): Long = dao.insert(transcription)

    suspend fun delete(transcription: Transcription) {
        dao.delete(transcription)
        transcription.audioFilePath?.let { File(it).delete() }
    }

    suspend fun getById(id: Long): Transcription? = dao.getById(id)

    fun observeRecentSenders(): Flow<List<String>> = dao.observeRecentSenders()

    suspend fun updateEnhancedText(id: Long, text: String) = dao.updateEnhancedText(id, text)
}
