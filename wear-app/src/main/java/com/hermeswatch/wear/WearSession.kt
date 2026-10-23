package com.hermeswatch.wear

import android.content.Context
import android.util.Log
import com.hermeswatch.shared.DataLayerPaths
import com.hermeswatch.shared.WireCodec
import com.hermeswatch.wear.data.Haptics
import com.hermeswatch.wear.data.PhoneLink
import com.hermeswatch.wear.state.WearEffect
import com.hermeswatch.wear.state.WearEvent
import com.hermeswatch.wear.state.WearReducer
import com.hermeswatch.wear.state.WearUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Process-wide owner of the watch state machine. Lives in [HermesWearApp] so a
 * reply delivered to [com.hermeswatch.wear.data.ResultListenerService] while the
 * activity is stopped still updates the UI state.
 *
 * Everything runs on the main thread: [dispatch] hops there, applies
 * [WearReducer.reduce], publishes the state, then executes effects.
 */
class WearSession(
    context: Context,
    private val reducer: WearReducer = WearReducer(newRequestId = { UUID.randomUUID().toString() }),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val phone = PhoneLink(context)
    private val haptics = Haptics(context)

    private val _state = MutableStateFlow<WearUiState>(WearUiState.Talk())
    val state: StateFlow<WearUiState> = _state.asStateFlow()

    private var timeout: Job? = null

    /** Safe from any thread. */
    fun dispatch(event: WearEvent) {
        scope.launch { apply(event) }
    }

    /**
     * Debug builds only (guarded by the caller): jump straight to a screen so UI can be
     * reviewed on a watch / emulator without a paired phone. See [DemoStates].
     */
    internal fun debugShow(state: WearUiState) {
        scope.launch {
            timeout?.cancel()
            _state.value = state
        }
    }

    private fun apply(event: WearEvent) {
        val t = reducer.reduce(_state.value, event)
        if (t.state != _state.value) Log.d(TAG, "${event::class.simpleName}: → ${t.state.screenKey}")
        _state.value = t.state
        t.effects.forEach(::run)
    }

    private fun run(effect: WearEffect) {
        when (effect) {
            is WearEffect.SendAsk -> send(effect.request.requestId, DataLayerPaths.MSG_ASK, WireCodec.encodeAsk(effect.request))
            is WearEffect.SendConfirm -> send(effect.request.requestId, DataLayerPaths.MSG_CONFIRM, WireCodec.encodeConfirm(effect.request))
            is WearEffect.ArmTimeout -> {
                timeout?.cancel()
                timeout = scope.launch {
                    delay(effect.delayMs)
                    apply(WearEvent.AttemptTimedOut(effect.requestId, effect.attempt))
                }
            }
            WearEffect.DisarmTimeout -> {
                timeout?.cancel()
                timeout = null
            }
            is WearEffect.Haptic -> haptics.play(effect.kind)
        }
    }

    private fun send(requestId: String, path: String, payload: ByteArray) {
        scope.launch {
            when (phone.send(path, payload)) {
                PhoneLink.SendResult.Ok -> Unit
                PhoneLink.SendResult.NoPhone -> apply(WearEvent.SendFailed(requestId, noPhone = true))
                is PhoneLink.SendResult.Failed -> apply(WearEvent.SendFailed(requestId, noPhone = false))
            }
        }
    }

    private companion object {
        const val TAG = "HermesWear"
    }
}
