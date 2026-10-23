package com.hermeswatch.phone

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hermeswatch.phone.relay.RelayClient
import com.hermeswatch.phone.relay.RelayConfig

/**
 * Minimal settings surface: relay base URL + token, and a /health check stub.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urlField = findViewById<EditText>(R.id.relayUrl)
        val tokenField = findViewById<EditText>(R.id.relayToken)
        val status = findViewById<TextView>(R.id.status)
        val check = findViewById<Button>(R.id.checkHealth)

        urlField.setText(getString(R.string.relay_url_hint))
        status.text = getString(R.string.health_stub)

        check.setOnClickListener {
            val client = RelayClient(
                RelayConfig(
                    baseUrl = urlField.text.toString().trim().trimEnd('/'),
                    bearerToken = tokenField.text.toString().trim(),
                ),
            )
            // Phase 0: call is documented but not executed (no network in skeleton CI).
            status.text = client.describeHealthCheck()
        }
    }
}
