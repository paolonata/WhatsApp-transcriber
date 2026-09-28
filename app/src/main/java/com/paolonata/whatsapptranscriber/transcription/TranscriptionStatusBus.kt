package com.paolonata.whatsapptranscriber.transcription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lets the foreground service tell the UI "something is transcribing right
 * now" without either side needing to know about the other's lifecycle.
 * Purely in-memory: if the process dies, there's nothing to lose here since
 * the service itself would also be gone.
 */
object TranscriptionStatusBus {
    private val _activeJobCount = MutableStateFlow(0)
    val activeJobCount: StateFlow<Int> = _activeJobCount

    private val _activeEnhanceIds = MutableStateFlow<Set<Long>>(emptySet())
    val activeEnhanceIds: StateFlow<Set<Long>> = _activeEnhanceIds

    // What the currently running job is doing right now (e.g. "Scaricamento
    // del modello: 40%"), so the home screen can show it directly instead of
    // a generic "in progress" message the user has to go check a system
    // notification to make sense of.
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    fun jobStarted() {
        _activeJobCount.value += 1
    }

    fun jobFinished() {
        val remaining = (_activeJobCount.value - 1).coerceAtLeast(0)
        _activeJobCount.value = remaining
        if (remaining == 0) {
            _statusMessage.value = null
        }
    }

    fun updateStatus(message: String) {
        _statusMessage.value = message
    }

    fun enhanceStarted(transcriptionId: Long) {
        _activeEnhanceIds.value = _activeEnhanceIds.value + transcriptionId
    }

    fun enhanceFinished(transcriptionId: Long) {
        _activeEnhanceIds.value = _activeEnhanceIds.value - transcriptionId
    }
}
