package com.hermeswatch.phone

import android.app.Application

/**
 * Phone companion application.
 * Phase 1: settings + OkHttp RelayClient from MainActivity.
 * Phase 2+: bind Wear Data Layer listener here.
 */
class HermesPhoneApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
