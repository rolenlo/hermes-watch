package com.hermeswatch.phone.settings

import android.content.Context
import com.hermeswatch.phone.relay.RelayConfig

/**
 * Persists relay base URL + Bearer token in SharedPreferences.
 */
class RelaySettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var bearerToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    fun loadConfig(): RelayConfig = RelayConfig(
        baseUrl = baseUrl,
        bearerToken = bearerToken,
    )

    fun save(baseUrl: String, bearerToken: String) {
        this.baseUrl = baseUrl
        this.bearerToken = bearerToken
    }

    companion object {
        private const val PREFS = "hermes_relay"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "bearer_token"
    }
}
