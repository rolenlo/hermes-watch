package com.hermeswatch.wear

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.hermeswatch.wear.ui.HermesWearRoot
import com.hermeswatch.wear.ui.theme.HermesTheme

/**
 * Single-activity host. All screens (Talk / Waiting / Answer / Confirm / Error)
 * are driven by one state machine — see [com.hermeswatch.wear.state.WearReducer].
 */
class MainActivity : ComponentActivity() {
    private val viewModel: HermesWearViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDemo(intent)
        setContent {
            HermesTheme {
                HermesWearRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDemo(intent)
    }

    /**
     * Debug-only UI review without a phone:
     * `adb shell am start -n com.hermeswatch/com.hermeswatch.wear.MainActivity --es demo answer`
     * (talk | waiting | answer | approve | yesno | error).
     */
    private fun handleDemo(intent: Intent?) {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val key = intent?.getStringExtra("demo") ?: return
        if (!debuggable) return
        DemoStates.forKey(key)?.let { (application as HermesWearApp).session.debugShow(it) }
    }
}
