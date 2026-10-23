package com.hermeswatch.phone.datalayer

import com.hermeswatch.shared.DataLayerPaths

/**
 * Bridge between Wear MessageClient and [com.hermeswatch.phone.relay.RelayClient].
 * Phase 0 stub — paths only; real listener in Phase 1.
 */
object WearBridge {
    val askPath: String = DataLayerPaths.MSG_ASK
    val resultPath: String = DataLayerPaths.MSG_RESULT
    val confirmPath: String = DataLayerPaths.MSG_CONFIRM
    val healthPath: String = DataLayerPaths.MSG_HEALTH

    // TODO(Phase 1):
    // - Wearable.getMessageClient(context).addListener { ... }
    // - on MSG_ASK: RelayClient.askWithWav → send MSG_RESULT
    // - on MSG_CONFIRM: POST /watch/confirm → MSG_RESULT
}
