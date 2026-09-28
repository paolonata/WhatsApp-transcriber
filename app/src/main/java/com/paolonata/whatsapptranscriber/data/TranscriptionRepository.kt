package com.paolonata.whatsapptranscriber.data

import kotlinx.coroutines.flow.Flow

class TranscriptionRepository(private val dao: TranscriptionDao) {
    fun observeAll(): Flow<List<Transcription>> = dao.observeAll()

    suspend fun insert(transcription: Transcription): Long = dao.insert(transcription)

    suspend fun delete(transcription: Transcription) = dao.delete(transcription)

    suspend fun getById(id: Long): Transcription? = dao.getById(id)

    fun observeRecentSenders(): Flow<List<String>> = dao.observeRecentSenders()
}
