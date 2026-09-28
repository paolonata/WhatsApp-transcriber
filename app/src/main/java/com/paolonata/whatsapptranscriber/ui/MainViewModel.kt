package com.paolonata.whatsapptranscriber.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paolonata.whatsapptranscriber.data.AppDatabase
import com.paolonata.whatsapptranscriber.data.Transcription
import com.paolonata.whatsapptranscriber.data.TranscriptionRepository
import com.paolonata.whatsapptranscriber.service.TranscriptionService
import com.paolonata.whatsapptranscriber.transcription.TranscriptionStatusBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingShare(val uri: Uri, val displayName: String?)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TranscriptionRepository(AppDatabase.getInstance(application).transcriptionDao())

    val transcriptions: StateFlow<List<Transcription>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSenders: StateFlow<List<String>> = repository.observeRecentSenders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeJobCount: StateFlow<Int> = TranscriptionStatusBus.activeJobCount

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen

    private val _pendingShare = MutableStateFlow<PendingShare?>(null)
    val pendingShare: StateFlow<PendingShare?> = _pendingShare

    fun showHome() {
        _screen.value = Screen.Home
    }

    fun openDetail(id: Long) {
        _screen.value = Screen.Detail(id)
    }

    fun deleteTranscription(transcription: Transcription) {
        viewModelScope.launch {
            repository.delete(transcription)
            if (_screen.value == Screen.Detail(transcription.id)) {
                _screen.value = Screen.Home
            }
        }
    }

    fun offerShare(uri: Uri, displayName: String?) {
        _pendingShare.value = PendingShare(uri, displayName)
    }

    fun confirmShare(sender: String?) {
        val share = _pendingShare.value ?: return
        _pendingShare.value = null
        TranscriptionService.start(getApplication(), share.uri, share.displayName, sender)
    }
}
