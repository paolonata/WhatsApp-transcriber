package com.paolonata.whatsapptranscriber.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.paolonata.whatsapptranscriber.MainActivity
import com.paolonata.whatsapptranscriber.R
import com.paolonata.whatsapptranscriber.WhatsAppTranscriberApp
import com.paolonata.whatsapptranscriber.audio.AudioDecoder
import com.paolonata.whatsapptranscriber.audio.WhatsAppMetadata
import com.paolonata.whatsapptranscriber.data.AppDatabase
import com.paolonata.whatsapptranscriber.data.Transcription
import com.paolonata.whatsapptranscriber.data.TranscriptionRepository
import com.paolonata.whatsapptranscriber.model.ModelManager
import com.paolonata.whatsapptranscriber.transcription.TranscriptionEngine
import com.paolonata.whatsapptranscriber.transcription.TranscriptionStatusBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Does all the heavy lifting (model download, decode, transcription) outside
 * the Activity lifecycle so it keeps running if the app is backgrounded or
 * closed, and tells the user it's done via a notification instead of a
 * screen they have to sit and wait on.
 */
class TranscriptionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeJobs = AtomicInteger(0)
    private val nextNotificationId = AtomicInteger(RESULT_NOTIFICATION_ID_BASE)

    private lateinit var repository: TranscriptionRepository
    private lateinit var modelManager: ModelManager
    private var engine: TranscriptionEngine? = null

    override fun onCreate() {
        super.onCreate()
        repository = TranscriptionRepository(AppDatabase.getInstance(applicationContext).transcriptionDao())
        modelManager = ModelManager(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.getParcelableExtraCompat(EXTRA_URI) ?: return START_NOT_STICKY
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME)
        val sender = intent.getStringExtra(EXTRA_SENDER)

        startForeground(FOREGROUND_NOTIFICATION_ID, buildOngoingNotification())
        activeJobs.incrementAndGet()
        TranscriptionStatusBus.jobStarted()

        serviceScope.launch {
            runCatching { process(uri, displayName, sender) }
                .onFailure { postFailureNotification(it.message ?: "Si è verificato un errore imprevisto.") }

            TranscriptionStatusBus.jobFinished()
            if (activeJobs.decrementAndGet() <= 0) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun process(uri: Uri, displayName: String?, sender: String?) {
        if (!modelManager.isModelReady()) {
            NotificationManagerCompat.from(this).safeNotify(
                FOREGROUND_NOTIFICATION_ID,
                buildOngoingNotification("Scaricamento del modello di trascrizione..."),
            )
            modelManager.ensureModelDownloaded { progress ->
                NotificationManagerCompat.from(this).safeNotify(
                    FOREGROUND_NOTIFICATION_ID,
                    buildOngoingNotification("Scaricamento del modello: ${(progress * 100).toInt()}%"),
                )
            }
        }

        NotificationManagerCompat.from(this).safeNotify(
            FOREGROUND_NOTIFICATION_ID,
            buildOngoingNotification("Lettura del file audio..."),
        )
        val pcm = withContext(Dispatchers.IO) { AudioDecoder.decodeToPcm16k(applicationContext, uri) }
        if (pcm.isEmpty()) {
            postFailureNotification("Non è stato possibile leggere l'audio condiviso.")
            return
        }

        NotificationManagerCompat.from(this).safeNotify(
            FOREGROUND_NOTIFICATION_ID,
            buildOngoingNotification("Trascrizione in corso...\nPer audio lunghi può richiedere qualche minuto."),
        )
        val transcriptionEngine = getOrCreateEngine()
        val text = transcriptionEngine.transcribe(pcm)
        if (text.isBlank()) {
            postFailureNotification("Non è stato riconosciuto alcun testo in questo audio.")
            return
        }

        val durationMs = (pcm.size / 16000.0 * 1000).toLong()
        val timestamp = WhatsAppMetadata.parseTimestampFromFileName(displayName) ?: System.currentTimeMillis()
        val id = repository.insert(
            Transcription(
                timestamp = timestamp,
                text = text,
                durationMs = durationMs,
                sourceLabel = displayName,
                sender = sender,
            ),
        )
        postSuccessNotification(id, sender)
    }

    private fun getOrCreateEngine(): TranscriptionEngine {
        return engine ?: TranscriptionEngine.create(modelManager.modelFile()).also { engine = it }
    }

    private fun buildOngoingNotification(message: String = "In preparazione..."): Notification {
        return NotificationCompat.Builder(this, WhatsAppTranscriberApp.PROGRESS_CHANNEL_ID)
            .setContentTitle("Trascrizione in corso")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun postSuccessNotification(transcriptionId: Long, sender: String?) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TRANSCRIPTION_ID, transcriptionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            transcriptionId.toInt(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = if (sender != null) "Trascrizione pronta - $sender" else "Trascrizione pronta"
        val notification = NotificationCompat.Builder(this, WhatsAppTranscriberApp.RESULT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Tocca per leggere il testo")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(this).safeNotify(nextNotificationId.incrementAndGet(), notification)
    }

    private fun postFailureNotification(message: String) {
        val notification = NotificationCompat.Builder(this, WhatsAppTranscriberApp.RESULT_CHANNEL_ID)
            .setContentTitle("Trascrizione non riuscita")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(this).safeNotify(nextNotificationId.incrementAndGet(), notification)
    }

    companion object {
        private const val EXTRA_URI = "extra_uri"
        private const val EXTRA_DISPLAY_NAME = "extra_display_name"
        private const val EXTRA_SENDER = "extra_sender"

        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val RESULT_NOTIFICATION_ID_BASE = 1000

        fun start(context: Context, uri: Uri, displayName: String?, sender: String?) {
            val intent = Intent(context, TranscriptionService::class.java).apply {
                putExtra(EXTRA_URI, uri)
                putExtra(EXTRA_DISPLAY_NAME, displayName)
                putExtra(EXTRA_SENDER, sender)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

private fun Intent.getParcelableExtraCompat(key: String): Uri? {
    return if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(key, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

private fun NotificationManagerCompat.safeNotify(id: Int, notification: Notification) {
    try {
        notify(id, notification)
    } catch (e: SecurityException) {
        // POST_NOTIFICATIONS not granted - the work still completes and shows
        // up in the app's history, it just won't surface as a notification.
    }
}
