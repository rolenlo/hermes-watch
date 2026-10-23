package com.hermeswatch.wear.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hermeswatch.wear.state.HapticKind

/** Short, distinct wrist taps so an answer is noticed without looking. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = run {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Vibrator::class.java)
        }
    }

    fun play(kind: HapticKind) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = when (kind) {
            HapticKind.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticKind.ANSWER -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticKind.CONFIRM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            HapticKind.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 40, 70, 40, 70, 40), -1)
        }
        runCatching { v.vibrate(effect) }
    }
}
