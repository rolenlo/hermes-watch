package com.hermeswatch.wear

import android.app.Application

/**
 * Wear OS application entry (Xiaomi Watch 2 Pro / Wear OS).
 * Phase 0: placeholder — Data Layer listeners land in Phase 1.
 */
class HermesWearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO(Phase 1): register Wearable ListenerService / MessageClient.
    }
}
