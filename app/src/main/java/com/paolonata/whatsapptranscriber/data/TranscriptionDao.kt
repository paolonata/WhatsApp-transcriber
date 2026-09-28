package com.paolonata.whatsapptranscriber.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Insert
    suspend fun insert(transcription: Transcription): Long

    @Delete
    suspend fun delete(transcription: Transcription)

    @Query("SELECT * FROM transcriptions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transcription>>

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    suspend fun getById(id: Long): Transcription?

    @Query(
        "SELECT sender FROM transcriptions WHERE sender IS NOT NULL AND sender != '' " +
            "GROUP BY sender ORDER BY MAX(timestamp) DESC LIMIT 8",
    )
    fun observeRecentSenders(): Flow<List<String>>
}
