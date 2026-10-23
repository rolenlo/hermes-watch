package com.hermeswatch.phone.relay

/**
 * Pure URL helpers — no Android / OkHttp dependency (unit-testable on JVM).
 */
object RelayUrl {
    private val ALLOWED = Regex("^https?://.+", RegexOption.IGNORE_CASE)

    /** Trim, strip trailing slashes, reject blank / non-http(s). */
    fun normalize(raw: String): Result<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Base URL is empty."))
        }
        if (!ALLOWED.matches(trimmed)) {
            return Result.failure(
                IllegalArgumentException(
                    "Base URL must start with http:// or https:// (e.g. https://pc.tailnet.ts.net:8453).",
                ),
            )
        }
        val noTrail = trimmed.trimEnd('/')
        // Reject bare scheme:// with nothing after
        val afterScheme = noTrail.substringAfter("://")
        if (afterScheme.isBlank() || afterScheme == "/") {
            return Result.failure(IllegalArgumentException("Base URL is missing a host."))
        }
        return Result.success(noTrail)
    }

    fun join(baseUrl: String, path: String): String {
        val base = baseUrl.trimEnd('/')
        val p = if (path.startsWith("/")) path else "/$path"
        return base + p
    }
}
