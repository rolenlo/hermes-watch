package com.hermeswatch.phone.relay

/**
 * Parsed JSON from relay.js — keep field names identical to the PC contract
 * (root README "Relay HTTP API").
 */
data class HealthResponse(
    val ok: Boolean,
    val stt: Boolean?,
    val hermes: Boolean?,
    val rawJson: String,
) {
    fun summary(): String = buildString {
        append("ok=").append(ok)
        if (stt != null) append("  stt=").append(stt)
        if (hermes != null) append("  hermes=").append(hermes)
        append('\n').append(rawJson)
    }
}

data class AskResponse(
    val ok: Boolean,
    val heard: String?,
    val kind: String?,
    val text: String?,
    val id: String?,
    val job: String?,
    val approve: Boolean?,
    val error: String?,
    val note: String?,
    val rawJson: String,
) {
    fun displayText(): String {
        if (!ok && !error.isNullOrBlank()) return "Error: $error"
        val kindLabel = kind ?: "?"
        val body = text?.takeIf { it.isNotBlank() }
            ?: note?.takeIf { it.isNotBlank() }
            ?: error?.takeIf { it.isNotBlank() }
            ?: "(no text)"
        return buildString {
            append("kind=").append(kindLabel)
            if (!heard.isNullOrBlank()) append("\nheard=").append(heard)
            if (!id.isNullOrBlank()) append("\nid=").append(id)
            if (!job.isNullOrBlank()) append("\njob=").append(job)
            if (approve == true) append("\napprove=true")
            append('\n').append(body)
        }
    }
}
