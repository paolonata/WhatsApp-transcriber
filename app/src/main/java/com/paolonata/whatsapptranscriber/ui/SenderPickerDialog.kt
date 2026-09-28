package com.paolonata.whatsapptranscriber.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SenderPickerDialog(
    recentSenders: List<String>,
    onConfirm: (sender: String?) -> Unit,
) {
    var typedSender by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onConfirm(null) },
        title = { Text("Chi ha mandato questo vocale?", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                if (recentSenders.isNotEmpty()) {
                    Text("Scegli tra i recenti:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        recentSenders.forEach { sender ->
                            SuggestionChip(
                                onClick = { onConfirm(sender) },
                                label = { Text(sender, style = MaterialTheme.typography.bodyMedium) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("oppure scrivi un nome nuovo:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedTextField(
                    value = typedSender,
                    onValueChange = { typedSender = it },
                    singleLine = true,
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(typedSender.trim().ifBlank { null }) }) {
                Text("Trascrivi", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) {
                Text("Non lo so / salta", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
