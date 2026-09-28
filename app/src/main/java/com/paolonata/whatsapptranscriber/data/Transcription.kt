package com.paolonata.whatsapptranscriber.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcriptions")
data class Transcription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val text: String,
    val durationMs: Long,
    val sourceLabel: String?,
    val sender: String? = null,
)
