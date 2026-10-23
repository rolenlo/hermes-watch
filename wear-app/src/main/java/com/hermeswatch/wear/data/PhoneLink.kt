package com.hermeswatch.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.hermeswatch.shared.Capabilities
import kotlinx.coroutines.tasks.await

/**
 * Finds the phone running the companion and sends it MessageClient messages.
 *
 * Node discovery: `CapabilityClient` for [Capabilities.PHONE_RELAY] (declared in
 * the companion's `res/values/wear.xml`), falling back to any connected node —
 * capability info can lag right after install. Nearby (Bluetooth-direct) nodes
 * are preferred over cloud-routed ones.
 */
class PhoneLink(context: Context) {
    sealed interface SendResult {
        data object Ok : SendResult
        data object NoPhone : SendResult
        data class Failed(val error: Throwable) : SendResult
    }

    private val app = context.applicationContext
    private val capabilities by lazy { Wearable.getCapabilityClient(app) }
    private val nodes by lazy { Wearable.getNodeClient(app) }
    private val messages by lazy { Wearable.getMessageClient(app) }

    suspend fun send(path: String, payload: ByteArray): SendResult {
        val nodeId = findPhoneNodeId() ?: return SendResult.NoPhone
        return try {
            messages.sendMessage(nodeId, path, payload).await()
            SendResult.Ok
        } catch (t: Throwable) {
            Log.w(TAG, "sendMessage $path → $nodeId failed: ${t.message}")
            SendResult.Failed(t)
        }
    }

    private suspend fun findPhoneNodeId(): String? {
        val withCapability = runCatching {
            capabilities.getCapability(Capabilities.PHONE_RELAY, CapabilityClient.FILTER_REACHABLE).await().nodes
        }.getOrNull().orEmpty()
        pickBest(withCapability)?.let { return it.id }

        val connected = runCatching { nodes.connectedNodes.await() }.getOrNull().orEmpty()
        if (connected.isNotEmpty()) Log.i(TAG, "capability ${Capabilities.PHONE_RELAY} not visible; trying connected node")
        return pickBest(connected)?.id
    }

    private fun pickBest(nodes: Collection<Node>): Node? = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()

    private companion object {
        const val TAG = "HermesPhoneLink"
    }
}
