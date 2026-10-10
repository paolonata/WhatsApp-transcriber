package com.paolonata.whatsapptranscriber.transcription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lets the foreground service tell the UI "something is transcribing right
 * now" (and which specific jobs, so the UI can offer to cancel them) without
 * either side needing to know about the other's lifecycle. Purely in-memory:
 * if the process dies, there's nothing to lose here since the service itself
 * would also be gone.
 */
object TranscriptionStatusBus {
    private val _activeJobIds = MutableStateFlow<Set<String>>(emptySet())
    val activeJobIds: StateFlow<Set<String>> = _activeJobIds

    // transcriptionId -> jobId, for the detail screen's "Migliora precisione"
    // row to know both whether it's running and which job to cancel.
    private val _enhanceJobIds = MutableStateFlow<Map<Long, String>>(emptyMap())
    val enhanceJobIds: StateFlow<Map<Long, String>> = _enhanceJobIds

    // What the currently running job is doing right now (e.g. "Scaricamento
    // del modello: 40%"), so the home screen can show it directly instead of
    // a generic "in progress" message the user has to go check a system
    // notification to make sense of.
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    fun jobStarted(jobId: String) {
        _activeJobIds.value = _activeJobIds.value + jobId
    }

    fun jobFinished(jobId: String) {
        _activeJobIds.value = _activeJobIds.value - jobId
        if (_activeJobIds.value.isEmpty()) {
            _statusMessage.value = null
        }
    }

    fun enhanceStarted(transcriptionId: Long, jobId: String) {
        _enhanceJobIds.value = _enhanceJobIds.value + (transcriptionId to jobId)
    }

    fun enhanceFinished(transcriptionId: Long) {
        _enhanceJobIds.value = _enhanceJobIds.value - transcriptionId
    }

    fun updateStatus(message: String) {
        _statusMessage.value = message
    }
}
