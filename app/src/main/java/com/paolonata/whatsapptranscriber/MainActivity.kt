package com.paolonata.whatsapptranscriber

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.paolonata.whatsapptranscriber.ui.DetailScreen
import com.paolonata.whatsapptranscriber.ui.ErrorScreen
import com.paolonata.whatsapptranscriber.ui.HomeScreen
import com.paolonata.whatsapptranscriber.ui.MainViewModel
import com.paolonata.whatsapptranscriber.ui.ModelDownloadScreen
import com.paolonata.whatsapptranscriber.ui.ProcessingScreen
import com.paolonata.whatsapptranscriber.ui.Screen
import com.paolonata.whatsapptranscriber.ui.theme.WhatsAppTranscriberTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        setContent {
            WhatsAppTranscriberTheme {
                AppRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> extractUri(intent)?.let { uri ->
                viewModel.handleSharedAudio(uri, queryDisplayName(uri))
            }
            Intent.ACTION_SEND_MULTIPLE -> extractUris(intent)?.firstOrNull()?.let { uri ->
                viewModel.handleSharedAudio(uri, queryDisplayName(uri))
            }
        }
    }

    private fun extractUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun extractUris(intent: Intent): List<Uri>? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
private fun AppRoot(viewModel: MainViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val transcriptions by viewModel.transcriptions.collectAsStateWithLifecycle()

    BackHandler(enabled = screen !is Screen.Home) {
        viewModel.showHome()
    }

    when (val current = screen) {
        is Screen.Home -> HomeScreen(transcriptions = transcriptions, onOpen = viewModel::openDetail)
        is Screen.Downloading -> ModelDownloadScreen(progress = current.progress)
        is Screen.Processing -> ProcessingScreen(message = current.message)
        is Screen.Error -> ErrorScreen(message = current.message, onDismiss = viewModel::showHome)
        is Screen.Detail -> {
            val item = transcriptions.find { it.id == current.id }
            if (item != null) {
                DetailScreen(
                    transcription = item,
                    onBack = viewModel::showHome,
                    onDelete = { viewModel.deleteTranscription(item) },
                )
            } else {
                ProcessingScreen(message = "Caricamento...")
            }
        }
    }
}
