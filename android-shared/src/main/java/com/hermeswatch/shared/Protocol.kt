package com.hermeswatch.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reply kinds. The first three mirror relay.js (`POST /watch/ask` → `kind`);
 * [ERROR] is added by the companion when the relay could not be reached or
 * answered `ok:false`.
 */
object ReplyKind {
    const val ANSWER = "answer"
    const val CONFIRM = "confirm"
    const val WORKING = "working"
    const val ERROR = "error"
}

/** Machine-readable error codes in [WatchReply.errorCode]; the watch maps them to zh-HK copy. */
object ErrorCode {
    /** Companion has no relay URL / token saved. */
    const val NOT_CONFIGURED = "not_configured"
    /** Relay answered 401 — token mismatch. */
    const val UNAUTHORIZED = "unauthorized"
    /** Timeout / refused / DNS / TLS — PC or Tailscale not reachable from the phone. */
    const val UNREACHABLE = "unreachable"
    /** Relay reachable but returned `ok:false` or a 5xx. */
    const val RELAY = "relay"
    /** Hermes still working after the companion's overall cap. */
    const val TOO_SLOW = "too_slow"
    /** Watch sent something the companion could not parse. */
    const val BAD_REQUEST = "bad_request"
}

/** Watch → phone on [DataLayerPaths.MSG_ASK]. */
@Serializable
data class AskRequest(
    val requestId: String,
    val text: String,
)

/** Watch → phone on [DataLayerPaths.MSG_CONFIRM]. `id` is the relay's confirm-card id. */
@Serializable
data class ConfirmRequest(
    val requestId: String,
    val id: String,
    val yes: Boolean,
)

/**
 * Phone → watch on [DataLayerPaths.MSG_RESULT].
 *
 * `kind=working` is a heartbeat (Hermes still thinking); the watch resets its
 * 25 s retry timer on it. Every other kind is final for that [requestId].
 */
@Serializable
data class WatchReply(
    val requestId: String,
    val kind: String,
    val text: String? = null,
    val heard: String? = null,
    val id: String? = null,
    val approve: Boolean? = null,
    val errorCode: String? = null,
) {
    val isFinal: Boolean get() = kind != ReplyKind.WORKING
}

/**
 * UTF-8 JSON (de)serialization for MessageClient payloads. Decoding never
 * throws: a malformed or foreign payload yields `null` so a listener can drop it.
 */
object WireCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = false
    }

    fun encodeAsk(req: AskRequest): ByteArray = json.encodeToString(AskRequest.serializer(), req).toByteArray(Charsets.UTF_8)
    fun encodeConfirm(req: ConfirmRequest): ByteArray = json.encodeToString(ConfirmRequest.serializer(), req).toByteArray(Charsets.UTF_8)
    fun encodeReply(reply: WatchReply): ByteArray = json.encodeToString(WatchReply.serializer(), reply).toByteArray(Charsets.UTF_8)

    fun decodeAsk(bytes: ByteArray?): AskRequest? = decode(bytes) {
        json.decodeFromString(AskRequest.serializer(), it).takeIf { r -> r.requestId.isNotBlank() }
    }

    fun decodeConfirm(bytes: ByteArray?): ConfirmRequest? = decode(bytes) {
        json.decodeFromString(ConfirmRequest.serializer(), it).takeIf { r -> r.requestId.isNotBlank() && r.id.isNotBlank() }
    }

    fun decodeReply(bytes: ByteArray?): WatchReply? = decode(bytes) {
        json.decodeFromString(WatchReply.serializer(), it).takeIf { r -> r.requestId.isNotBlank() && r.kind.isNotBlank() }
    }

    /** Best-effort requestId from any payload (so a bad request can still be answered). */
    fun peekRequestId(bytes: ByteArray?): String? = decode(bytes) {
        val obj = json.parseToJsonElement(it) as? kotlinx.serialization.json.JsonObject
        (obj?.get("requestId") as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { p -> p.isString }?.content
    }

    private inline fun <T> decode(bytes: ByteArray?, block: (String) -> T?): T? {
        if (bytes == null || bytes.isEmpty()) return null
        return try {
            block(String(bytes, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }
}
