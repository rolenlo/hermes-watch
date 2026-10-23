package com.hermeswatch.wear.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.app.RemoteInput
import androidx.wear.input.RemoteInputIntentHelper
import com.hermeswatch.wear.R

private const val KEY_QUESTION = "hermes_question"

/**
 * Phase 2 stand-in for the microphone: the system Wear input sheet
 * (RemoteInput). It offers **voice dictation**, keyboard / handwriting and the
 * preset chips below — so the user can already "talk" before Phase 3 records
 * WAV itself.
 */
class TextInputLauncher internal constructor(
    private val launcher: ManagedActivityResultLauncher<android.content.Intent, ActivityResult>,
    private val label: String,
    private val presets: Array<CharSequence>,
    private val onUnavailable: () -> Unit,
) {
    fun launch() {
        val remoteInput = RemoteInput.Builder(KEY_QUESTION)
            .setLabel(label)
            .setChoices(presets)
            .setAllowFreeFormInput(true)
            .build()
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
        try {
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            onUnavailable()
        }
    }
}

@Composable
fun rememberTextInputLauncher(
    onText: (String) -> Unit,
    onUnavailable: () -> Unit,
): TextInputLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val text = RemoteInput.getResultsFromIntent(data)?.getCharSequence(KEY_QUESTION)?.toString()
        if (!text.isNullOrBlank()) onText(text)
    }
    return remember(launcher) {
        TextInputLauncher(
            launcher = launcher,
            label = context.getString(R.string.input_label),
            presets = context.resources.getTextArray(R.array.preset_questions),
            onUnavailable = onUnavailable,
        )
    }
}
