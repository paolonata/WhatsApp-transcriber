package com.paolonata.whatsapptranscriber.audio

import java.util.Calendar

/**
 * WhatsApp names shared voice notes like "AUD-20250928-WA0007.opus" (or
 * "PTT-...") embedding the recording date. There is no way to read who sent
 * the file - Android's share sheet never exposes the sender's contact
 * identity to the receiving app - but the date is a nice, real signal we can
 * recover for free instead of just using "now" (when the file happened to be
 * opened in this app).
 */
object WhatsAppMetadata {

    private val FILENAME_DATE_REGEX = Regex("""(?:AUD|PTT)-(\d{4})(\d{2})(\d{2})-WA""")

    fun parseTimestampFromFileName(name: String?): Long? {
        if (name == null) return null
        val match = FILENAME_DATE_REGEX.find(name) ?: return null
        val (year, month, day) = match.destructured
        return try {
            val calendar = Calendar.getInstance().apply {
                clear()
                set(year.toInt(), month.toInt() - 1, day.toInt(), 12, 0, 0)
            }
            calendar.timeInMillis
        } catch (e: Exception) {
            null
        }
    }
}
