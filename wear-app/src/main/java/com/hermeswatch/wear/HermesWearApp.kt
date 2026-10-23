package com.hermeswatch.wear

import android.app.Application

/**
 * Wear OS application entry (Xiaomi Watch 2 Pro / Wear OS).
 * Owns the single [WearSession] (state machine + Data Layer link).
 */
class HermesWearApp : Application() {
    val session: WearSession by lazy { WearSession(this) }
}
