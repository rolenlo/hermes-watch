package com.hermeswatch.wear.data

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.hermeswatch.shared.DataLayerPaths
import com.hermeswatch.shared.WireCodec
import com.hermeswatch.wear.HermesWearApp
import com.hermeswatch.wear.state.WearEvent

/**
 * Receives phone → watch replies on [DataLayerPaths.MSG_RESULT]. Manifest-declared
 * (not a live listener) so replies still land while the screen is off or the
 * activity is stopped; the process-wide session updates state and buzzes.
 */
class ResultListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != DataLayerPaths.MSG_RESULT) return
        val reply = WireCodec.decodeReply(event.data) ?: return
        (application as HermesWearApp).session.dispatch(WearEvent.ReplyReceived(reply))
    }
}
