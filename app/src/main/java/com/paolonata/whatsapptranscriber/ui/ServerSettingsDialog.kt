package com.paolonata.whatsapptranscriber.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paolonata.whatsapptranscriber.server.ServerConfig
import com.paolonata.whatsapptranscriber.server.ServerSettings

@Composable
fun ServerSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { ServerSettings(context) }
    val initial = remember { settings.get() }

    var enabled by remember { mutableStateOf(initial.enabled) }
    var host by remember { mutableStateOf(initial.host) }
    var port by remember { mutableStateOf(initial.port.toString()) }
    var token by remember { mutableStateOf(initial.token) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server locale (opzionale)", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text(
                    "Se hai installato l'add-on su Home Assistant, inserisci qui i dati per usarlo " +
                        "quando sei sulla rete di casa - molto più veloce del telefono. Fuori casa " +
                        "l'app passa automaticamente al modello sul telefono.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Usa server locale", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    singleLine = true,
                    label = { Text("Host (es. homeassistant.local)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    label = { Text("Porta") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    singleLine = true,
                    label = { Text("Token") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.save(
                    ServerConfig(
                        enabled = enabled,
                        host = host,
                        port = port.toIntOrNull() ?: ServerSettings.DEFAULT_PORT,
                        token = token,
                    ),
                )
                onDismiss()
            }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
