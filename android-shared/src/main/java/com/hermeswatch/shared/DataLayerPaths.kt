package com.hermeswatch.shared

/**
 * Wearable Data Layer / MessageClient path constants shared by wear-app and
 * android-companion. Keep these in sync with the phone ↔ watch protocol.
 *
 * Phase 2 wire format (UTF-8 JSON, see [WireCodec]):
 * - [MSG_ASK]      watch → phone  [AskRequest]      `{requestId, text}`
 * - [MSG_CONFIRM]  watch → phone  [ConfirmRequest]  `{requestId, id, yes}`
 * - [MSG_RESULT]   phone → watch  [WatchReply]      `{requestId, kind, text, heard, id?, approve?, errorCode?}`
 *
 * MVP audio (Phase 3): 16 kHz mono WAV/PCM (see [AudioFormat]).
 */
object DataLayerPaths {
    /** Common prefix — used by the manifest `<data android:pathPrefix>` filters. */
    const val PREFIX = "/hermeswatch"

    /** Watch → phone: ask (Phase 2: text JSON; Phase 3: WAV envelope). */
    const val MSG_ASK = "/hermeswatch/ask"

    /** Phone → watch: final answer / confirm card / working heartbeat / error. */
    const val MSG_RESULT = "/hermeswatch/result"

    /** Watch → phone: Approve / Deny / Yes / No for a confirm card. */
    const val MSG_CONFIRM = "/hermeswatch/confirm"

    /** Phone → watch: relay health / connection status (reserved). */
    const val MSG_HEALTH = "/hermeswatch/health"

    /** DataItem path prefix for chunked audio transfer (Phase 3). */
    const val DATA_AUDIO_PREFIX = "/hermeswatch/audio"
}

/**
 * Capability names declared in each app's `res/values/wear.xml`
 * (`android_wear_capabilities`). The watch looks up [PHONE_RELAY] with
 * `CapabilityClient` to find the phone node that runs the companion; the phone
 * replies straight to the message's `sourceNodeId`, so it does not need discovery.
 */
object Capabilities {
    const val PHONE_RELAY = "hermeswatch_phone_relay"
    const val WATCH_APP = "hermeswatch_watch_app"
}

/**
 * MVP recording format. Matches relay STT expectations for raw PCM / WAV
 * (upstream Zepp path uses Opus; Wear port uses WAV for simplicity).
 */
object AudioFormat {
    const val SAMPLE_RATE_HZ = 16_000
    const val CHANNEL_COUNT = 1
    const val BITS_PER_SAMPLE = 16
    const val MIME_WAV = "audio/wav"
}
