package com.paolonata.whatsapptranscriber

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paolonata.whatsapptranscriber.ui.DetailScreen
import com.paolonata.whatsapptranscriber.ui.HomeScreen
import com.paolonata.whatsapptranscriber.ui.MainViewModel
import com.paolonata.whatsapptranscriber.ui.Screen
import com.paolonata.whatsapptranscriber.ui.SenderPickerDialog
import com.paolonata.whatsapptranscriber.ui.theme.WhatsAppTranscriberTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* if denied, the app still works - just without result notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
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

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val openId = intent.getLongExtra(EXTRA_OPEN_TRANSCRIPTION_ID, -1L)
        if (openId >= 0) {
            viewModel.openDetail(openId)
            return
        }

        when (intent.action) {
            Intent.ACTION_SEND -> extractUri(intent)?.let { uri ->
                viewModel.offerShare(uri, queryDisplayName(uri))
            }
            Intent.ACTION_SEND_MULTIPLE -> extractUris(intent)?.firstOrNull()?.let { uri ->
                viewModel.offerShare(uri, queryDisplayName(uri))
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

    companion object {
        const val EXTRA_OPEN_TRANSCRIPTION_ID = "extra_open_transcription_id"
    }
}

@Composable
private fun AppRoot(viewModel: MainViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val transcriptions by viewModel.transcriptions.collectAsStateWithLifecycle()
    val recentSenders by viewModel.recentSenders.collectAsStateWithLifecycle()
    val activeJobCount by viewModel.activeJobCount.collectAsStateWithLifecycle()
    val pendingShare by viewModel.pendingShare.collectAsStateWithLifecycle()

    BackHandler(enabled = screen !is Screen.Home) {
        viewModel.showHome()
    }

    when (val current = screen) {
        is Screen.Home -> HomeScreen(
            transcriptions = transcriptions,
            activeJobCount = activeJobCount,
            onOpen = viewModel::openDetail,
            onDelete = viewModel::deleteTranscription,
        )
        is Screen.Detail -> {
            val item = transcriptions.find { it.id == current.id }
            if (item != null) {
                DetailScreen(
                    transcription = item,
                    onBack = viewModel::showHome,
                    onDelete = { viewModel.deleteTranscription(item) },
                )
            } else {
                HomeScreen(
                    transcriptions = transcriptions,
                    activeJobCount = activeJobCount,
                    onOpen = viewModel::openDetail,
                    onDelete = viewModel::deleteTranscription,
                )
            }
        }
    }

    if (pendingShare != null) {
        SenderPickerDialog(
            recentSenders = recentSenders,
            onConfirm = { sender -> viewModel.confirmShare(sender) },
        )
    }
}
