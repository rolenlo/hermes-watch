package com.hermeswatch.phone.relay

/**
 * HTTP client for the PC-side [relay.js](../../../../../../relay.js) over Tailscale.
 *
 * Auth: every path except `GET /health` needs
 * `Authorization: Bearer <token>` matching `config.json` / `config.example.json`.
 *
 * MVP endpoints (see root README "Relay HTTP API"):
 * - `GET  /health` → `{ok, stt, hermes}` (no Bearer required)
 * - `POST /watch/ask` body `{audio_b64}` or `{text}` → answer | confirm | working
 * - `POST /watch/confirm` `{id, yes}`
 * - `GET  /watch/result?job=`
 *
 * Audio for Wear port: 16 kHz mono WAV/PCM, base64 in `audio_b64` (upstream Zepp uses Opus).
 *
 * Implementation: OkHttp — left as TODO for Phase 0 so the skeleton builds without a live relay.
 */
data class RelayConfig(
    val baseUrl: String,
    val bearerToken: String,
)

class RelayClient(
    private val config: RelayConfig,
    // private val http: OkHttpClient = OkHttpClient(),
) {
    /**
     * Documents the Phase 1 call without hitting the network.
     *
     * Intended OkHttp sketch:
     * ```
     * val req = Request.Builder()
     *     .url("${config.baseUrl}/health")
     *     .get()
     *     .build()
     * http.newCall(req).execute().use { resp -> ... }
     * ```
     */
    fun describeHealthCheck(): String {
        return buildString {
            appendLine("TODO Phase 1: GET ${config.baseUrl}/health")
            appendLine("(no Authorization header)")
            appendLine("Expect JSON: {\"ok\":true,\"stt\":...,\"hermes\":...}")
            if (config.bearerToken.isBlank()) {
                appendLine("Token empty — required for POST /watch/ask later.")
            } else {
                appendLine("Token set (${config.bearerToken.length} chars) — used as Bearer for /watch/*.")
            }
        }.trimEnd()
    }

    /**
     * Phase 1: POST /watch/ask with Bearer token.
     *
     * ```
     * val body = JSONObject()
     *     .put("audio_b64", Base64.encodeToString(wavBytes, Base64.NO_WRAP))
     *     // or .put("text", "...")
     *     .toString()
     * val req = Request.Builder()
     *     .url("${config.baseUrl}/watch/ask")
     *     .header("Authorization", "Bearer ${config.bearerToken}")
     *     .post(body.toRequestBody("application/json".toMediaType()))
     *     .build()
     * ```
     */
    @Suppress("unused", "UNUSED_PARAMETER")
    fun askWithWav(wavBytes: ByteArray): Nothing =
        throw NotImplementedError("Phase 1: POST /watch/ask with audio_b64 (16 kHz mono WAV)")

    @Suppress("unused", "UNUSED_PARAMETER")
    fun askWithText(text: String): Nothing =
        throw NotImplementedError("Phase 1: POST /watch/ask with {text}")
}
