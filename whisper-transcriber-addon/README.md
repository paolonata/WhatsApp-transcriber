# Whisper Transcriber (add-on per Home Assistant)

Server di trascrizione vocale offline per l'app Android **Trascrivi WhatsApp**: gira sul tuo Raspberry Pi, usa whisper.cpp in locale, nessun audio o testo esce mai dalla tua rete di casa.

## Installazione

1. In Home Assistant: **Impostazioni → Add-on → Negozio add-on → ⋮ (in alto a destra) → Repository**.
2. Aggiungi questo repository: `https://github.com/paolonata/WhatsApp-transcriber`
3. Cerca **Whisper Transcriber** nell'elenco e installalo.
4. Prima di avviarlo, vai nella scheda **Configurazione** e cambia `api_token` con una password a tua scelta (serve per evitare che altri dispositivi sulla tua rete possano usare il server).
5. Avvia l'add-on. Il primo avvio scarica i modelli (alcune centinaia di MB ciascuno): segui i log dalla scheda **Log**.
6. Nell'app Android, vai nelle impostazioni del server e inserisci:
   - **Host**: l'indirizzo del tuo Home Assistant (es. `homeassistant.local` o l'IP del Pi)
   - **Porta**: `8765`
   - **Token**: lo stesso che hai messo in `api_token`

## Come funziona

- L'app Android invia l'audio al Pi solo quando sei sulla stessa rete di casa (connessione diretta, nessun server esterno coinvolto).
- Se il Pi non è raggiungibile (es. sei fuori casa), l'app passa automaticamente alla trascrizione sul telefono stesso - niente si blocca.
- Due modelli configurabili: uno "veloce" (usato di default) e uno "preciso" (usato dal pulsante "Migliora precisione").

## Note

- `threads`: quanti core del Pi usare per la trascrizione (max 4 su un Pi 4). Più thread = più veloce ma meno margine per Home Assistant durante l'elaborazione.
- Build iniziale dell'immagine: compila whisper.cpp da sorgente, può richiedere diversi minuti sul Pi.
