# Trascrivi WhatsApp

App Android nativa per trascrivere i vocali di WhatsApp in testo, **interamente sul telefono**: nessun audio e nessun testo trascritto viene mai inviato a internet.

## Come funziona

1. Apri un vocale su WhatsApp e tocca **Condividi**.
2. Scegli **Trascrivi WhatsApp** dall'elenco delle app.
3. L'app decodifica l'audio e lo trascrive localmente (motore [whisper.cpp](https://github.com/ggerganov/whisper.cpp), vendorizzato come submodule in `third_party/whisper.cpp`).
4. Il testo compare a schermo, con possibilità di ingrandire il carattere, farlo leggere ad alta voce (sintesi vocale), copiarlo o ricondividerlo.

Tutte le trascrizioni restano salvate nello storico dell'app (database locale), così si possono rileggere in qualsiasi momento.

La lingua di trascrizione è fissata all'**italiano** (patchata nel bridge JNI di whisper.cpp, vedi `scripts/patch-whisper-language.sh`, dato che di default è forzata all'inglese). Per un'altra lingua principale, cambia il codice lingua in quello script.

## Privacy

- **Nessuna IA cloud**: la trascrizione avviene con un modello whisper.cpp eseguito localmente sulla CPU del telefono.
- **Un'unica chiamata di rete**, la prima volta che si usa l'app: il download del modello di trascrizione (un file pubblico, statico, `large-v3-turbo` quantizzato, ~550 MB, da [huggingface.co/ggerganov/whisper.cpp](https://huggingface.co/ggerganov/whisper.cpp)). Non contiene né trasmette alcun dato personale.
- Dopo il primo avvio l'app funziona **completamente offline**.

## Struttura del progetto

- `app/` — applicazione Android (Kotlin + Jetpack Compose)
- `third_party/whisper.cpp` — submodule git con il motore di trascrizione (modulo `:lib` incluso da `examples/whisper.android/lib`)
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
