package com.hermeswatch.phone

import android.app.Application

/**
 * Phone companion application.
 * Phase 1: settings + OkHttp RelayClient from MainActivity.
 * Phase 2: watch messages arrive via [com.hermeswatch.phone.datalayer.HermesWearListenerService]
 * (manifest-declared, woken by Play services) — nothing to register here.
 */
class HermesPhoneApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
