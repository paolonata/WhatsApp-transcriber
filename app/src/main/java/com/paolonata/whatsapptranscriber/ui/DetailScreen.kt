package com.paolonata.whatsapptranscriber.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.paolonata.whatsapptranscriber.data.Transcription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailScreen(
    transcription: Transcription,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var fontSize by remember { mutableFloatStateOf(20f) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    val tts = remember { TtsHolder(context) }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            transcription.sender?.takeIf { it.isNotBlank() } ?: "Sconosciuto",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            formatTimestamp(transcription.timestamp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionButton(
                    icon = if (isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    label = if (isSpeaking) "Ferma" else "Ascolta",
                    onClick = {
                        if (isSpeaking) {
                            tts.stop()
                            isSpeaking = false
                        } else {
                            tts.speak(transcription.text) { isSpeaking = false }
                            isSpeaking = true
                        }
                    },
                )
                ActionButton(
                    icon = Icons.Filled.ContentCopy,
                    label = "Copia",
                    onClick = { copyToClipboard(context, transcription.text) },
                )
                ActionButton(
                    icon = Icons.Filled.Share,
                    label = "Condividi",
                    onClick = { shareText(context, transcription.text) },
                )
                ActionButton(
                    icon = Icons.Filled.Delete,
                    label = "Elimina",
                    onClick = { showDeleteConfirm = true },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.TextFields, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                TextButton(onClick = { fontSize = (fontSize - 2f).coerceAtLeast(14f) }) { Text("A-", fontWeight = FontWeight.Bold) }
                TextButton(onClick = { fontSize = (fontSize + 2f).coerceAtMost(32f) }) { Text("A+", fontWeight = FontWeight.Bold) }
            }

            Text(
                text = transcription.text,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.5f).sp,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp),
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminare questa trascrizione?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService<ClipboardManager>()
    clipboard?.setPrimaryClip(ClipData.newPlainText("Trascrizione", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Condividi trascrizione"))
}

private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.ITALIAN)
    return format.format(Date(timestamp))
}

private class TtsHolder(context: Context) {
    private var tts: TextToSpeech? = null
    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ITALIAN
            }
        }
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onDoneCallback?.invoke()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onDoneCallback?.invoke()
            }
        })
    }

    fun speak(text: String, onDone: () -> Unit) {
        onDoneCallback = onDone
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "transcription")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
