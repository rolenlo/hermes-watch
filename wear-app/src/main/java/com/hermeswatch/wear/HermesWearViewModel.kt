package com.hermeswatch.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hermeswatch.wear.state.WearEvent
import com.hermeswatch.wear.state.WearUiState
import kotlinx.coroutines.flow.StateFlow

/** Thin UI-facing wrapper over the process-wide [WearSession]. */
class HermesWearViewModel(app: Application) : AndroidViewModel(app) {
    private val session = (app as HermesWearApp).session

    val state: StateFlow<WearUiState> = session.state

    fun onEvent(event: WearEvent) = session.dispatch(event)
}
