package com.hermeswatch.phone

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hermeswatch.phone.datalayer.WatchRequestCoordinator
import com.hermeswatch.phone.relay.RelayClient
import com.hermeswatch.phone.relay.RelayConfig
import com.hermeswatch.phone.relay.RelayErrors
import com.hermeswatch.phone.relay.RelayUrl
import com.hermeswatch.phone.settings.RelaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings + probes (GET /health, text POST /watch/ask) + the latest watch
 * request handled by [WatchRequestCoordinator] (Phase 2 Data Layer bridge).
 */
class MainActivity : AppCompatActivity() {
    private lateinit var settings: RelaySettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = RelaySettings(this)

        val urlField = findViewById<EditText>(R.id.relayUrl)
        val tokenField = findViewById<EditText>(R.id.relayToken)
        val askField = findViewById<EditText>(R.id.askText)
        val status = findViewById<TextView>(R.id.status)
        val check = findViewById<Button>(R.id.checkHealth)
        val askBtn = findViewById<Button>(R.id.textAsk)
        val saveBtn = findViewById<Button>(R.id.saveSettings)

        if (settings.baseUrl.isNotBlank()) {
            urlField.setText(settings.baseUrl)
        } else {
            urlField.setText(getString(R.string.relay_url_hint))
        }
        tokenField.setText(settings.bearerToken)
        status.text = getString(R.string.phase1_ready)

        val watchStatus = findViewById<TextView>(R.id.watchStatus)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WatchRequestCoordinator.lastEvent.collect { line ->
                    watchStatus.text = line.ifBlank { getString(R.string.watch_idle) }
                }
            }
        }

        fun persistFields(): String? {
            val normalized = RelayUrl.normalize(urlField.text.toString())
            if (normalized.isFailure) {
                status.text = RelayErrors.fromThrowable(normalized.exceptionOrNull()!!)
                return null
            }
            val base = normalized.getOrThrow()
            val token = tokenField.text.toString().trim()
            settings.save(base, token)
            urlField.setText(base)
            return base
        }

        saveBtn.setOnClickListener {
            if (persistFields() != null) {
                status.text = getString(R.string.settings_saved)
            }
        }

        check.setOnClickListener {
            val base = persistFields() ?: return@setOnClickListener
            val token = tokenField.text.toString().trim()
            status.text = getString(R.string.health_checking)
            setBusy(check, askBtn, saveBtn, busy = true)

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        RelayClient(
                            RelayConfig(base, token),
                            RelayClient.healthClient(),
                        ).health()
                    }
                }
                setBusy(check, askBtn, saveBtn, busy = false)
                status.text = result.fold(
                    onSuccess = { it.summary() },
                    onFailure = { RelayErrors.fromThrowable(it) },
                )
            }
        }

        askBtn.setOnClickListener {
            val base = persistFields() ?: return@setOnClickListener
            val token = tokenField.text.toString().trim()
            val text = askField.text.toString()
            if (text.isBlank()) {
                status.text = getString(R.string.ask_empty)
                return@setOnClickListener
            }
            if (token.isBlank()) {
                status.text = getString(R.string.token_required)
                return@setOnClickListener
            }
            status.text = getString(R.string.ask_sending)
            setBusy(check, askBtn, saveBtn, busy = true)

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        RelayClient(RelayConfig(base, token)).askWithText(text)
                    }
                }
                setBusy(check, askBtn, saveBtn, busy = false)
                status.text = result.fold(
                    onSuccess = { it.displayText() },
                    onFailure = { RelayErrors.fromThrowable(it) },
                )
            }
        }
    }

    private fun setBusy(vararg buttons: Button, busy: Boolean) {
        buttons.forEach { it.isEnabled = !busy }
    }
}
