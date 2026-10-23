package com.hermeswatch.phone.relay

import com.hermeswatch.shared.ErrorCode
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException

/**
 * Map companion-side failures to the small set of [ErrorCode]s the watch knows.
 * The watch shows zh-HK copy for the code; [RelayErrors] detail goes in `text`.
 */
object WireErrors {
    fun codeFor(t: Throwable): String {
        var cur: Throwable? = t
        while (cur != null) {
            when (cur) {
                is RelayHttpException -> return when (cur.code) {
                    401, 403 -> ErrorCode.UNAUTHORIZED
                    else -> ErrorCode.RELAY
                }
                is SocketTimeoutException,
                is ConnectException,
                is UnknownHostException,
                is SSLException,
                is CertificateException,
                -> return ErrorCode.UNREACHABLE
                is IllegalArgumentException -> return ErrorCode.NOT_CONFIGURED
            }
            cur = cur.cause
        }
        return if (t is IOException) ErrorCode.UNREACHABLE else ErrorCode.RELAY
    }
}
