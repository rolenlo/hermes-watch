package com.hermeswatch.phone.datalayer

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Woken by Google Play services when the watch sends a MessageClient message
 * under `/hermeswatch/…` (see the manifest intent-filter). Hands off to the
 * process-wide [WatchRequestCoordinator] and returns immediately.
 *
 * Data Layer only delivers between apps with the **same applicationId and
 * signing key** (`com.hermeswatch` for both phone and watch).
 */
class HermesWearListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        WatchRequestCoordinator.onMessage(
            context = applicationContext,
            sourceNodeId = event.sourceNodeId,
            path = event.path,
            data = event.data,
        )
    }
}
