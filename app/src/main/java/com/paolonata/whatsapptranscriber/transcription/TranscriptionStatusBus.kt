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

    fun jobStarted() {
        _activeJobCount.value += 1
    }

    fun jobFinished() {
        _activeJobCount.value = (_activeJobCount.value - 1).coerceAtLeast(0)
    }

    fun enhanceStarted(transcriptionId: Long) {
        _activeEnhanceIds.value = _activeEnhanceIds.value + transcriptionId
    }

    fun enhanceFinished(transcriptionId: Long) {
        _activeEnhanceIds.value = _activeEnhanceIds.value - transcriptionId
    }
}
