package com.hermeswatch.shared

/**
 * Wearable Data Layer / MessageClient path constants shared by wear-app and
 * android-companion. Keep these in sync with the phone ↔ watch protocol.
 *
 * MVP audio: 16 kHz mono WAV/PCM (see [AudioFormat]).
 */
object DataLayerPaths {
    /** Watch → phone: start a voice ask (payload = WAV bytes or request envelope). */
    const val MSG_ASK = "/hermeswatch/ask"

    /** Phone → watch: final answer / confirm card / working status. */
    const val MSG_RESULT = "/hermeswatch/result"

    /** Watch → phone: Approve / Deny / Yes / No for a confirm card. */
    const val MSG_CONFIRM = "/hermeswatch/confirm"

    /** Phone → watch: relay health / connection status. */
    const val MSG_HEALTH = "/hermeswatch/health"

    /** DataItem path prefix for chunked audio transfer (Phase 1+). */
    const val DATA_AUDIO_PREFIX = "/hermeswatch/audio"
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
