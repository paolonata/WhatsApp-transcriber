package com.paolonata.whatsapptranscriber.server

import android.content.Context

data class ServerConfig(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val token: String,
) {
    val baseUrl: String get() = "http://$host:$port"
    val isConfigured: Boolean get() = host.isNotBlank() && token.isNotBlank()
}

/**
 * Settings for an optional home-network transcription server (see
 * whisper-transcriber-addon/): when configured and reachable, the app sends
 * the audio there instead of running whisper.cpp on the phone - much faster
 * on real hardware, but only works on the home network, so there's always a
 * fallback to the on-device model.
 */
class ServerSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): ServerConfig = ServerConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        host = prefs.getString(KEY_HOST, "") ?: "",
        port = prefs.getInt(KEY_PORT, DEFAULT_PORT),
        token = prefs.getString(KEY_TOKEN, "") ?: "",
    )

    fun save(config: ServerConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_HOST, config.host.trim())
            .putInt(KEY_PORT, config.port)
            .putString(KEY_TOKEN, config.token.trim())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "server_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_TOKEN = "token"
        const val DEFAULT_PORT = 8765
    }
}
