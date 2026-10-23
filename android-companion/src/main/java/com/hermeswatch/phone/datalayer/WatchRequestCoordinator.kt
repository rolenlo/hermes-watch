package com.hermeswatch.phone.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.hermeswatch.phone.relay.AskResponse
import com.hermeswatch.phone.relay.RelayClient
import com.hermeswatch.phone.relay.RelayErrors
import com.hermeswatch.phone.relay.ReplyMapper
import com.hermeswatch.phone.relay.RequestDeduper
import com.hermeswatch.phone.relay.WireErrors
import com.hermeswatch.phone.settings.RelaySettings
import com.hermeswatch.shared.DataLayerPaths
import com.hermeswatch.shared.ErrorCode
import com.hermeswatch.shared.ReplyKind
import com.hermeswatch.shared.WatchReply
import com.hermeswatch.shared.WireCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Watch ↔ relay bridge. Lives for the whole process (not tied to the listener
 * service, which Android may stop as soon as `onMessageReceived` returns).
 *
 * Flow per watch request:
 * 1. Dedupe on `requestId` ([RequestDeduper]): new → run; running → heartbeat;
 *    done → replay the cached final reply.
 * 2. Immediately ack with `kind=working`, then keep a heartbeat every
 *    [HEARTBEAT_MS] so the watch's 25 s retry timer never fires while we wait.
 * 3. Call relay.js with `rid = requestId` (relay dedupes too), poll
 *    `GET /watch/result?job=` while `kind=working`, up to [MAX_WORK_MS].
 * 4. Send the final [WatchReply] to the watch node that asked, on
 *    [DataLayerPaths.MSG_RESULT].
 */
object WatchRequestCoordinator {
    private const val TAG = "HermesBridge"
    const val HEARTBEAT_MS = 10_000L
    const val POLL_MS = 2_000L
    const val MAX_WORK_MS = 10 * 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deduper = RequestDeduper<WatchReply>()

    private val _lastEvent = MutableStateFlow("")
    /** One human-readable line for the companion UI ("最近手錶請求"). */
    val lastEvent: StateFlow<String> = _lastEvent.asStateFlow()

    fun onMessage(context: Context, sourceNodeId: String, path: String, data: ByteArray?) {
        val app = context.applicationContext
        when (path) {
            DataLayerPaths.MSG_ASK -> {
                val req = WireCodec.decodeAsk(data)
                if (req == null) {
                    rejectBadRequest(app, sourceNodeId, data)
                    return
                }
                note("ask ${req.requestId.take(8)}: ${req.text.take(40)}")
                handle(app, sourceNodeId, req.requestId) { it.ask(req.text, rid = req.requestId) }
            }
            DataLayerPaths.MSG_CONFIRM -> {
                val req = WireCodec.decodeConfirm(data)
                if (req == null) {
                    rejectBadRequest(app, sourceNodeId, data)
                    return
                }
                note("confirm ${req.requestId.take(8)}: id=${req.id} yes=${req.yes}")
                handle(app, sourceNodeId, req.requestId) { it.confirm(req.id, req.yes, rid = req.requestId) }
            }
            else -> Log.d(TAG, "ignored path $path")
        }
    }

    private fun handle(
        context: Context,
        nodeId: String,
        requestId: String,
        call: (RelayClient) -> AskResponse,
    ) {
        when (val d = deduper.onArrival(requestId)) {
            is RequestDeduper.Decision.Replay -> {
                note("replay ${requestId.take(8)} (${d.reply.kind})")
                scope.launch { send(context, nodeId, d.reply) }
            }
            is RequestDeduper.Decision.InFlight -> {
                scope.launch {
                    send(context, nodeId, d.lastHeartbeat ?: WatchReply(requestId, ReplyKind.WORKING))
                }
            }
            RequestDeduper.Decision.Start -> scope.launch { run(context, nodeId, requestId, call) }
        }
    }

    private suspend fun run(
        context: Context,
        nodeId: String,
        requestId: String,
        call: (RelayClient) -> AskResponse,
    ) {
        val ack = WatchReply(requestId, ReplyKind.WORKING)
        deduper.heartbeat(requestId, ack)
        send(context, nodeId, ack)

        val ticker = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_MS)
                send(context, nodeId, latestHeartbeat(requestId) ?: ack)
            }
        }

        val final: WatchReply = try {
            val config = RelaySettings(context).loadConfig()
            if (config.baseUrl.isBlank() || config.bearerToken.isBlank()) {
                ReplyMapper.error(requestId, ErrorCode.NOT_CONFIGURED, "Open the phone app and set relay URL + token.")
            } else {
                val client = RelayClient(config)
                val started = System.currentTimeMillis()
                var r = call(client)
                var job = r.job
                while (r.ok && r.kind == ReplyKind.WORKING) {
                    val j = job
                    if (j.isNullOrBlank()) {
                        r = r.copy(ok = false, kind = null, error = "Relay said working without a job id.")
                        break
                    }
                    deduper.heartbeat(requestId, ReplyMapper.toWatch(requestId, r))
                    if (System.currentTimeMillis() - started > MAX_WORK_MS) {
                        return finish(context, nodeId, requestId, ticker,
                            ReplyMapper.error(requestId, ErrorCode.TOO_SLOW, "Hermes still working (job=$j)."))
                    }
                    delay(POLL_MS)
                    r = client.result(j)
                    job = r.job ?: j
                }
                ReplyMapper.toWatch(requestId, r)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "request $requestId failed", t)
            ReplyMapper.error(requestId, WireErrors.codeFor(t), RelayErrors.fromThrowable(t))
        }
        finish(context, nodeId, requestId, ticker, final)
    }

    private suspend fun finish(
        context: Context,
        nodeId: String,
        requestId: String,
        ticker: kotlinx.coroutines.Job,
        final: WatchReply,
    ) {
        ticker.cancel()
        deduper.complete(requestId, final)
        note("done ${requestId.take(8)} → ${final.kind}${final.errorCode?.let { " ($it)" } ?: ""}")
        send(context, nodeId, final)
    }

    private fun latestHeartbeat(requestId: String): WatchReply? =
        deduper.latestHeartbeat(requestId)

    private fun rejectBadRequest(context: Context, nodeId: String, data: ByteArray?) {
        val rid = WireCodec.peekRequestId(data) ?: return
        note("bad request ${rid.take(8)}")
        scope.launch {
            send(context, nodeId, ReplyMapper.error(rid, ErrorCode.BAD_REQUEST, "Phone could not read the request."))
        }
    }

    private suspend fun send(context: Context, nodeId: String, reply: WatchReply) {
        try {
            Wearable.getMessageClient(context)
                .sendMessage(nodeId, DataLayerPaths.MSG_RESULT, WireCodec.encodeReply(reply))
                .await()
        } catch (t: Throwable) {
            // The watch retries after 25 s of silence and we replay from the cache.
            Log.w(TAG, "send ${reply.kind} to $nodeId failed: ${t.message}")
        }
    }

    private fun note(line: String) {
        Log.i(TAG, line)
        _lastEvent.value = line
    }
}
