# Trascrivi WhatsApp

App Android nativa per trascrivere i vocali di WhatsApp in testo, **interamente sul telefono**: nessun audio e nessun testo trascritto viene mai inviato a internet.

## Come funziona

1. Apri un vocale su WhatsApp e tocca **Condividi**.
2. Scegli **Trascrivi WhatsApp** dall'elenco delle app.
3. Un dialog veloce chiede chi ha mandato il vocale (o si può saltare) - WhatsApp non passa questa informazione alle app per motivi di privacy, quindi va indicata a mano; i nomi già usati restano come scorciatoie a un tocco.
4. La trascrizione parte in un **servizio in background**: si può chiudere l'app, arriva una notifica quando il testo è pronto (o se qualcosa è andato storto). Motore [whisper.cpp](https://github.com/ggerganov/whisper.cpp), vendorizzato come submodule in `third_party/whisper.cpp`.
5. Il testo compare a schermo, con possibilità di ingrandire il carattere, farlo leggere ad alta voce (sintesi vocale), copiarlo o ricondividerlo. Se la trascrizione automatica ha qualche parola imprecisa, il pulsante **"Migliora precisione"** la rielabora in background con un modello più accurato (più lento, per questo non è quello usato di default).

Tutte le trascrizioni restano salvate nello storico dell'app (database locale), raggruppate per mittente, e si possono eliminare direttamente dall'elenco senza doverle aprire.

La lingua di trascrizione è fissata all'**italiano** (patchata nel bridge JNI di whisper.cpp, vedi `scripts/patch-whisper-language.sh`, dato che di default è forzata all'inglese). Per un'altra lingua principale, cambia il codice lingua in quello script.

## Privacy

- **Nessuna IA cloud**: la trascrizione avviene con whisper.cpp eseguito localmente sulla CPU del telefono (niente GPU/NPU su Android, quindi la velocità dipende molto dalla dimensione del modello).
- **Due modelli, scaricati solo quando servono** (file pubblici e statici da [huggingface.co/ggerganov/whisper.cpp](https://huggingface.co/ggerganov/whisper.cpp), nessun dato personale coinvolto):
  - `small` quantizzato (~190 MB) — usato automaticamente per ogni vocale condiviso, veloce.
  - `medium` quantizzato (~510 MB) — scaricato solo alla prima volta che si tocca "Migliora precisione".
- L'audio originale di ogni vocale viene salvato in locale (memoria privata dell'app) così "Migliora precisione" può rielaborarlo anche giorni dopo, senza bisogno di ricondividerlo da WhatsApp; viene eliminato insieme alla trascrizione.
- A parte i download dei modelli, l'app funziona **completamente offline**.

## Server di trascrizione locale (opzionale)

Il telefono (specialmente uno non recentissimo) resta comunque limitato: whisper.cpp su Android gira solo su CPU. Chi ha un **Raspberry Pi 4** (o superiore) con **Home Assistant OS** può installare l'add-on in `whisper-transcriber-addon/` per trascrivere su hardware più potente, restando comunque sulla propria rete di casa (nessun server di terzi coinvolto):

1. In Home Assistant: **Impostazioni → Add-on → Negozio add-on → ⋮ → Repository**, aggiungi `https://github.com/paolonata/WhatsApp-transcriber`.
2. Installa **Whisper Transcriber**, imposta un `api_token` a tua scelta nella configurazione, avvialo.
3. Nell'app, tocca l'icona ⚙️ in alto a destra nella schermata principale e inserisci host (es. `homeassistant.local`), porta (`8765`) e lo stesso token.

Quando sei sulla rete di casa, l'app usa il Pi (molto più veloce); fuori casa passa automaticamente al modello sul telefono, senza bisogno di fare nulla. Dettagli nel [README dell'add-on](whisper-transcriber-addon/README.md).

## Struttura del progetto

- `app/` — applicazione Android (Kotlin + Jetpack Compose)
- `third_party/whisper.cpp` — submodule git con il motore di trascrizione (modulo `:lib` incluso da `examples/whisper.android/lib`)
- `whisper-transcriber-addon/` — add-on Home Assistant opzionale per trascrivere su un Raspberry Pi invece che sul telefono
- `.github/workflows/android-release.yml` — compila, firma e pubblica l'APK come GitHub Release ad ogni push

## Scaricare e installare l'app

Ad ogni push su questo repository, GitHub Actions compila automaticamente l'APK e lo pubblica nella pagina **[Releases](../../releases)** del repository. Scarica l'ultimo `.apk` dal telefono e installalo (potrebbe essere necessario abilitare "Installa da fonti sconosciute" per il browser/file manager usato).

Le versioni successive, firmate con la stessa chiave, si installano automaticamente **come aggiornamento** sopra quella precedente — non serve disinstallare.

### Configurazione una tantum per le release firmate

Perché la firma sia sempre la stessa (requisito indispensabile per gli aggiornamenti in-place), il workflow legge una keystore da GitHub Actions secrets. Finché questi secret non sono configurati, l'APK viene comunque compilato e pubblicato come artifact di build, ma firmato con la chiave di debug (che può cambiare da un'esecuzione all'altra, quindi il primo aggiornamento *dopo* aver configurato i secret potrebbe richiedere una disinstallazione manuale una tantum).

Per abilitare le release firmate in modo stabile, in **Settings → Secrets and variables → Actions** del repository aggiungi questi secret (i valori sono stati generati per te e comunicati a parte, in chat, non salvati nel repository):

- `ANDROID_KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## Sviluppo locale

Richiede Android Studio (o Gradle + Android SDK/NDK `25.2.9519653` da linea di comando):

```bash
git submodule update --init --recursive
./scripts/patch-whisper-language.sh
./gradlew assembleDebug
```
