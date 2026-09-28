package com.paolonata.whatsapptranscriber

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService

class WhatsAppTranscriberApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "Trascrizione in corso",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Avanzamento della trascrizione di un vocale"
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                RESULT_CHANNEL_ID,
                "Trascrizioni pronte",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Avviso quando una trascrizione è pronta o non è riuscita"
            },
        )
    }

    companion object {
        const val PROGRESS_CHANNEL_ID = "transcription_progress"
        const val RESULT_CHANNEL_ID = "transcription_result"
    }
}
