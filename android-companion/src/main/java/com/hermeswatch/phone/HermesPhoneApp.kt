package com.hermeswatch.phone

import android.app.Application

/**
 * Phone companion application.
 * Phase 0: placeholder — binds Wear Data Layer + [com.hermeswatch.phone.relay.RelayClient] in Phase 1.
 */
class HermesPhoneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO(Phase 1): start Wear message listener; load relayUrl + Bearer token from settings.
    }
}
