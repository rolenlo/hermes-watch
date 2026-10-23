package com.hermeswatch.phone.relay

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RelayConfig(
    val baseUrl: String,
    val bearerToken: String,
)

/**
 * HTTP client for PC-side [relay.js] over Tailscale (or any reachable base URL).
 *
 * Auth: every path except `GET /health` needs `Authorization: Bearer <token>`.
 *
 * Contracts match root README "Relay HTTP API":
 * - `GET  /health` → `{ok, stt, hermes}`
 * - `POST /watch/ask` `{text}` or `{audio_b64}` → answer | confirm | working
 * - `GET  /watch/result?job=` when kind is `working`
 */
class RelayClient(
    private val config: RelayConfig,
    private val http: OkHttpClient = defaultClient(),
) {
    private val base: String = RelayUrl.normalize(config.baseUrl).getOrThrow()

    fun health(): HealthResponse {
        val req = Request.Builder()
            .url(RelayUrl.join(base, "/health"))
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RelayHttpException(resp.code, RelayErrors.http(resp.code, body))
            }
            val json = parseObject(body)
            return HealthResponse(
                ok = json.optBoolean("ok", false),
                stt = if (json.has("stt")) json.optBoolean("stt") else null,
                hermes = if (json.has("hermes")) json.optBoolean("hermes") else null,
                rawJson = body.ifBlank { "{}" },
            )
        }
    }

    /**
     * POST /watch/ask with `{text}`. If the relay returns `kind=working`, poll
     * GET /watch/result until an answer/confirm or [maxPollAttempts] is hit.
     */
    fun askWithText(
        text: String,
        pollWorking: Boolean = true,
        maxPollAttempts: Int = 30,
        pollDelayMs: Long = 2_000L,
    ): AskResponse {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Ask text is empty." }
        require(config.bearerToken.isNotBlank()) {
            "Bearer token is empty — required for POST /watch/ask (must match config.json token)."
        }

        val payload = JSONObject().put("text", trimmed).toString()
        var reply = postAsk(payload)
        if (pollWorking && reply.kind == "working" && !reply.job.isNullOrBlank()) {
            reply = pollResult(reply.job!!, maxPollAttempts, pollDelayMs, seed = reply)
        }
        return reply
    }

    /**
     * One-shot `POST /watch/ask {text, rid}` — no polling. [rid] is the watch's
     * requestId: relay.js dedupes on it, so a retried request gets the same reply
     * and Hermes / actions run once.
     */
    fun ask(text: String, rid: String? = null): AskResponse {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Ask text is empty." }
        requireToken()
        val payload = JSONObject().put("text", trimmed)
        if (!rid.isNullOrBlank()) payload.put("rid", rid)
        return postJson("/watch/ask", payload.toString())
    }

    /**
     * `POST /watch/confirm {id, yes, rid}` — Approve/Deny or Yes/No for a confirm card.
     * May come back `kind=working` (an approved build); poll [result] with its `job`.
     */
    fun confirm(id: String, yes: Boolean, rid: String? = null): AskResponse {
        require(id.isNotBlank()) { "Confirm id is empty." }
        requireToken()
        val payload = JSONObject().put("id", id).put("yes", yes)
        if (!rid.isNullOrBlank()) payload.put("rid", rid)
        return postJson("/watch/confirm", payload.toString())
    }

    private fun requireToken() {
        require(config.bearerToken.isNotBlank()) {
            "Bearer token is empty — required for /watch/* (must match config.json token)."
        }
    }

    /** Phase 3: POST /watch/ask with 16 kHz mono WAV as `audio_b64`. */
    fun askWithWav(
        wavBytes: ByteArray,
        pollWorking: Boolean = true,
        maxPollAttempts: Int = 30,
        pollDelayMs: Long = 2_000L,
    ): AskResponse {
        require(wavBytes.isNotEmpty()) { "WAV bytes are empty." }
        require(config.bearerToken.isNotBlank()) {
            "Bearer token is empty — required for POST /watch/ask."
        }
        val b64 = android.util.Base64.encodeToString(wavBytes, android.util.Base64.NO_WRAP)
        val payload = JSONObject().put("audio_b64", b64).toString()
        var reply = postAsk(payload)
        if (pollWorking && reply.kind == "working" && !reply.job.isNullOrBlank()) {
            reply = pollResult(reply.job!!, maxPollAttempts, pollDelayMs, seed = reply)
        }
        return reply
    }

    fun result(job: String): AskResponse {
        require(job.isNotBlank()) { "job is empty." }
        require(config.bearerToken.isNotBlank()) {
            "Bearer token is empty — required for GET /watch/result."
        }
        val url = RelayUrl.join(base, "/watch/result") + "?job=" +
            java.net.URLEncoder.encode(job, Charsets.UTF_8.name())
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.bearerToken}")
            .get()
            .build()
        return executeAsk(req)
    }

    private fun postAsk(jsonBody: String): AskResponse = postJson("/watch/ask", jsonBody)

    private fun postJson(path: String, jsonBody: String): AskResponse {
        val req = Request.Builder()
            .url(RelayUrl.join(base, path))
            .header("Authorization", "Bearer ${config.bearerToken}")
            .header("Content-Type", "application/json; charset=utf-8")
            .post(jsonBody.toRequestBody(JSON_MEDIA))
            .build()
        return executeAsk(req)
    }

    private fun executeAsk(req: Request): AskResponse {
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (resp.code == 401) {
                throw RelayHttpException(401, RelayErrors.http(401, body))
            }
            if (!resp.isSuccessful) {
                throw RelayHttpException(resp.code, RelayErrors.http(resp.code, body))
            }
            return parseAsk(body)
        }
    }

    private fun pollResult(
        job: String,
        maxAttempts: Int,
        delayMs: Long,
        seed: AskResponse,
    ): AskResponse {
        var last = seed
        repeat(maxAttempts) { attempt ->
            if (attempt > 0 || last.kind == "working") {
                Thread.sleep(delayMs)
            }
            last = result(job)
            if (last.kind != null && last.kind != "working") return last
        }
        return last.copy(
            error = last.error
                ?: "Still working after ${maxAttempts * delayMs / 1000}s (job=$job). Try again or poll /watch/result.",
        )
    }

    private fun parseAsk(body: String): AskResponse {
        val json = parseObject(body)
        return AskResponse(
            ok = json.optBoolean("ok", false),
            heard = json.optStringOrNull("heard"),
            kind = json.optStringOrNull("kind"),
            text = json.optStringOrNull("text"),
            id = json.optStringOrNull("id"),
            job = json.optStringOrNull("job"),
            approve = if (json.has("approve")) json.optBoolean("approve") else null,
            error = json.optStringOrNull("error"),
            note = json.optStringOrNull("note"),
            rawJson = body.ifBlank { "{}" },
        )
    }

    private fun parseObject(body: String): JSONObject {
        if (body.isBlank()) return JSONObject()
        return try {
            JSONObject(body)
        } catch (_: Exception) {
            throw RelayHttpException(200, "Relay returned non-JSON: ${body.take(200)}")
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "")
        return v.ifEmpty { null }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()

        /** Shorter timeouts for a quick /health probe. */
        fun healthClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
