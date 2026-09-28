package com.paolonata.whatsapptranscriber.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paolonata.whatsapptranscriber.audio.AudioDecoder
import com.paolonata.whatsapptranscriber.data.AppDatabase
import com.paolonata.whatsapptranscriber.data.Transcription
import com.paolonata.whatsapptranscriber.data.TranscriptionRepository
import com.paolonata.whatsapptranscriber.model.ModelManager
import com.paolonata.whatsapptranscriber.transcription.TranscriptionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TranscriptionRepository(AppDatabase.getInstance(application).transcriptionDao())
    private val modelManager = ModelManager(application)
    private var engine: TranscriptionEngine? = null

    val transcriptions: StateFlow<List<Transcription>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen

    fun showHome() {
        _screen.value = Screen.Home
    }

    fun openDetail(id: Long) {
        _screen.value = Screen.Detail(id)
    }

    fun deleteTranscription(transcription: Transcription) {
        viewModelScope.launch {
            repository.delete(transcription)
            _screen.value = Screen.Home
        }
    }

    fun handleSharedAudio(uri: Uri, displayName: String?) {
        if (_screen.value is Screen.Processing || _screen.value is Screen.Downloading) return

        viewModelScope.launch {
            try {
                if (!modelManager.isModelReady()) {
                    _screen.value = Screen.Downloading(0f)
                    modelManager.ensureModelDownloaded { progress -> _screen.value = Screen.Downloading(progress) }
                }

                _screen.value = Screen.Processing("Lettura del file audio...")
                val pcm = withContext(Dispatchers.IO) {
                    AudioDecoder.decodeToPcm16k(getApplication(), uri)
                }

                if (pcm.isEmpty()) {
                    _screen.value = Screen.Error("Non è stato possibile leggere l'audio condiviso.")
                    return@launch
                }

                _screen.value = Screen.Processing(
                    "Trascrizione in corso...\nPer audio lunghi può richiedere qualche minuto."
                )
                val transcriptionEngine = withContext(Dispatchers.IO) { getOrCreateEngine() }
                val text = withContext(Dispatchers.Default) { transcriptionEngine.transcribe(pcm) }

                if (text.isBlank()) {
                    _screen.value = Screen.Error("Non è stato riconosciuto alcun testo in questo audio.")
                    return@launch
                }

                val durationMs = (pcm.size / 16000.0 * 1000).toLong()
                val id = repository.insert(
                    Transcription(
                        timestamp = System.currentTimeMillis(),
                        text = text,
                        durationMs = durationMs,
                        sourceLabel = displayName,
                    )
                )
                _screen.value = Screen.Detail(id)
            } catch (e: Exception) {
                _screen.value = Screen.Error(e.message ?: "Si è verificato un errore imprevisto.")
            }
        }
    }

    private fun getOrCreateEngine(): TranscriptionEngine {
        return engine ?: TranscriptionEngine.create(modelManager.modelFile()).also { engine = it }
    }
}
