package com.hermeswatch.phone.relay

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/**
 * Map transport / HTTP failures to short Cantonese-friendly English messages
 * the companion UI can show.
 */
object RelayErrors {
    fun fromThrowable(t: Throwable): String {
        var cur: Throwable? = t
        while (cur != null) {
            when (cur) {
                is SocketTimeoutException ->
                    return "Timeout: relay did not respond in time. Is relay.js running? Is Tailscale up?"
                is ConnectException ->
                    return "Connection refused: cannot reach the relay host/port. Check base URL and that relay.js is listening."
                is UnknownHostException ->
                    return "Unknown host: DNS failed for that URL. Check Tailscale MagicDNS / hostname."
                is SSLHandshakeException, is CertificateException ->
                    return "TLS/certificate error: Tailscale HTTPS cert not trusted on this phone. Fix Tailscale or use a debug trust override."
                is SSLException ->
                    return "TLS error: ${cur.message ?: cur.javaClass.simpleName}"
                is IllegalArgumentException ->
                    return cur.message ?: "Invalid argument."
                is RelayHttpException ->
                    return cur.message ?: "HTTP ${cur.code}"
            }
            cur = cur.cause
        }
        return when (t) {
            is IOException -> "Network error: ${t.message ?: t.javaClass.simpleName}"
            else -> "Error: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    fun http(code: Int, bodySnippet: String?): String = when (code) {
        401 -> "401 Unauthorized: Bearer token missing or wrong (must match config.json token)."
        403 -> "403 Forbidden: relay rejected the request."
        404 -> "404 Not found: check base URL path (expect /health and /watch/ask on the relay root)."
        413 -> "413 Payload too large: recording/body too long for the relay."
        in 500..599 -> "Server error HTTP $code: ${bodySnippet?.take(200) ?: "relay failed"}"
        else -> "HTTP $code: ${bodySnippet?.take(200) ?: "unexpected response"}"
    }
}

class RelayHttpException(
    val code: Int,
    message: String,
) : IOException(message)
