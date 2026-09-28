package com.paolonata.whatsapptranscriber.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paolonata.whatsapptranscriber.data.Transcription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val UNKNOWN_SENDER = "Sconosciuto"

private val AVATAR_COLORS = listOf(
    Color(0xFF075E54), Color(0xFF128C7E), Color(0xFF25D366),
    Color(0xFF34B7F1), Color(0xFF7B68EE), Color(0xFFE67E22),
    Color(0xFFE74C3C), Color(0xFF9B59B6),
)

@Composable
fun HomeScreen(
    transcriptions: List<Transcription>,
    activeJobCount: Int,
    onOpen: (Long) -> Unit,
    onDelete: (Transcription) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Transcription?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trascrizioni", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        if (transcriptions.isEmpty() && activeJobCount == 0) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            val groups = transcriptions
                .groupBy { it.sender?.trim().takeUnless { s -> s.isNullOrBlank() } ?: UNKNOWN_SENDER }
                .toList()
                .sortedByDescending { (_, entries) -> entries.maxOf { it.timestamp } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { InstructionsCard() }

                if (activeJobCount > 0) {
                    item { ActiveJobBanner(activeJobCount) }
                }

                groups.forEach { (sender, entries) ->
                    item(key = "header_$sender") { SenderHeader(sender, entries.size) }
                    items(entries, key = { it.id }) { item ->
                        TranscriptionRow(
                            item = item,
                            sender = sender,
                            onClick = { onOpen(item.id) },
                            onDeleteClick = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminare questa trascrizione?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(toDelete)
                    pendingDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun ActiveJobBanner(count: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                if (count == 1) "Trascrizione in corso..." else "$count trascrizioni in corso...",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SenderHeader(sender: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SenderAvatar(sender, size = 32.dp)
        Spacer(Modifier.width(10.dp))
        Text(sender, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            "($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SenderAvatar(sender: String, size: Dp) {
    val isUnknown = sender == UNKNOWN_SENDER
    val color = AVATAR_COLORS[Math.floorMod(sender.hashCode(), AVATAR_COLORS.size)]
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isUnknown) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        } else {
            Text(
                sender.first().uppercaseChar().toString(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Come si usa",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Apri il vocale su WhatsApp, tocca \"Condividi\" e scegli questa app. " +
                    "Il testo apparirà qui, senza bisogno di ascoltare l'audio.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                "Nessuna trascrizione ancora",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Condividi un vocale di WhatsApp con questa app per vederne qui il testo.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TranscriptionRow(
    item: Transcription,
    sender: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SenderAvatar(sender, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(formatTimestamp(item.timestamp), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.ITALIAN)
    return format.format(Date(timestamp))
}
